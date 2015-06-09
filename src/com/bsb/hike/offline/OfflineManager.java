package com.bsb.hike.offline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONObject;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView.ScaleType;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter.FTViewHolder;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeOfflinePersistence;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.OfflineHikePacket;
import com.bsb.hike.offline.OfflineConstants.HandlerConstants;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;


/**
 * 
 * @author himanshu, deepak malik
 * This class forms the base of Offline Messaging and deals with socket connection,text transfer and file transfer queue. 
 */

public class OfflineManager implements IWIfiReceiverCallback , PeerListListener
{
	private static OfflineManager _instance = null;

	private Map<Long, FileTransferModel> currentSendingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private Map<Long, FileTransferModel> currentReceivingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private BlockingQueue<JSONObject> textMessageQueue = null;

	private ArrayList<IOfflineCallbacks> listeners;

	private BlockingQueue<FileTransferModel> fileTransferQueue = null;

	private volatile boolean inFileTransferInProgress=false;

	private Context context;

	private volatile String connectedDevice =null;

	private ConnectionManager connectionManager;

	private static final String TAG=OfflineManager.class.getName();

	private OfflineThreadManager threadManager ;

	private boolean scanResultsAvailable =  false;

	private boolean isConnectedToHotspot= false;
	
	private int tryGetScanResults = 0;
	
	OfflineBroadCastReceiver receiver;
	
	private OFFLINE_STATE offlineState;
	
	private boolean startedForChatThread = false;

	Handler handler =new Handler(HikeHandlerUtil.getInstance().getLooper())
	{
		public void handleMessage(android.os.Message msg) {
			if(msg==null)
			{
				return;
			}

			handleMsgOnBackEndThread(msg);
		}

	};

	private OfflineManager()
	{
		init();
	}

	public static  OfflineManager getInstance()
	{
		if (_instance == null)
		{
			synchronized (OfflineManager.class)
			{
				if (_instance == null)
				{
					_instance = new OfflineManager();
				
				}
			}
		}
		return _instance;
	}


	private void handleMsgOnBackEndThread(Message msg)
	{
		switch(msg.what)
		{
		case OfflineConstants.HandlerConstants.SAVE_MSG_DB:
			saveToDb((ConvMessage)msg.obj);
			break;
		case OfflineConstants.HandlerConstants.DISCONNECT_AFTER_TIMEOUT:
			disconnect((String)msg.obj);
			break;
		case OfflineConstants.HandlerConstants.CREATE_HOTSPOT:
			connectionManager.createHotspot((String)msg.obj);
			break;
		case OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT:
			startedForChatThread = true;
			startWifiScan();
			Logger.d(TAG, "Scanning for hotspots");
			checkAndRetryConnect((String) msg.obj);
			break;
		case OfflineConstants.HandlerConstants.RECONNECT_TO_HOTSPOT:
			String connectingToMsisdn = (String) msg.obj;
			Logger.d(TAG, "Rescanning for hotspot.");
			startWifiScan();
			Message checkAndRetry = Message.obtain();
			checkAndRetry.what = OfflineConstants.HandlerConstants.RECONNECT_TO_HOTSPOT;
			checkAndRetry.obj = connectingToMsisdn;
			handler.sendMessageDelayed(checkAndRetry, OfflineConstants.TRY_CONNECT_TO_HOTSPOT);
			break;
		case OfflineConstants.HandlerConstants.REMOVE_CONNECT_MESSAGE:
			removeMessage(OfflineConstants.HandlerConstants.RECONNECT_TO_HOTSPOT);
			setOfflineState(OFFLINE_STATE.NOT_CONNECTED);
			break;
		case OfflineConstants.HandlerConstants.START_SCAN:
			runNetworkScan((int)msg.obj);
			msg.obj=((int)msg.obj)+1;
			performWorkOnBackEndThread(msg);
			break;
		case OfflineConstants.HandlerConstants.SEND_GHOST_PACKET:
			String msisdn = (String) msg.obj;
			sendGhostPacket(msisdn);
			handler.sendMessageDelayed(msg, OfflineConstants.GHOST_PACKET_SEND_TIME);
			break;
		case OfflineConstants.HandlerConstants.SAVE_MSG_PERSISTANCE_DB:
			saveMessagetoPersistanceDb((FileTransferModel) (msg.obj));
			break;
		case OfflineConstants.HandlerConstants.SEND_PERSISTANCE_MSGS:
			sendPersistantMsgs();
			break;
		}
	};
	
	private void checkAndRetryConnect(String msisdn)
	{
		Message checkAndRetry = Message.obtain();
		checkAndRetry.what = OfflineConstants.HandlerConstants.RECONNECT_TO_HOTSPOT;
		checkAndRetry.obj = msisdn;
		handler.sendMessageDelayed(checkAndRetry, OfflineConstants.TRY_CONNECT_TO_HOTSPOT);
	}

	
	private void saveMessagetoPersistanceDb(FileTransferModel fileTransferModel)
	{
		//Add the Msg here to Persistance Db.
		HikeOfflinePersistence.getInstance().addMessage(fileTransferModel.getPacket());
		addToFileQueue(fileTransferModel);
		
	}
	
	private void saveToDb(ConvMessage convMessage)
	{
		long startTime=System.currentTimeMillis();
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage,true);
		// Save Msg here in Persistance DB.
		HikeOfflinePersistence.getInstance().addMessage(convMessage.serialize());
		addToTextQueue(convMessage.serialize());
		long endTime = System.currentTimeMillis();
		
		Logger.d(TAG, "Time in DB entry: " + (endTime-startTime));
	}

	public void performWorkOnBackEndThread(Message msg)
	{
		handler.sendMessage(msg);
	}

	/**
	 * Initialize all your functions here
	 */
	private void init()
	{
		textMessageQueue=new LinkedBlockingQueue<>();
		fileTransferQueue=new LinkedBlockingQueue<>();
		context=HikeMessengerApp.getInstance().getApplicationContext();
		connectionManager  = ConnectionManager.getInstance();
		threadManager=OfflineThreadManager.getInstance();
		listeners = new ArrayList<IOfflineCallbacks>();
		setDeviceNameAsMsisdn();
		receiver=new OfflineBroadCastReceiver(this);
		setOfflineState(OFFLINE_STATE.NOT_CONNECTED);
		Logger.d(TAG,"Contructor called");
	}

	private void setDeviceNameAsMsisdn() {
		// TODO : Restore back to previous deviceName
		connectionManager.setDeviceNameAsMsisdn();
	}
	public void disconnect(String msisdn) {

		// Since disconnect is called, stop sending ghost packets
		removeMessage(OfflineConstants.HandlerConstants.SEND_GHOST_PACKET);
		connectionManager.disconnect(msisdn);
	}

	public synchronized void addToTextQueue(JSONObject message)
	{
		
		try
		{
			if(OfflineUtils.isConnectedToSameMsisdn(message,getConnectedDevice()))
			{
				textMessageQueue.put(message);
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	public synchronized void addToFileQueue(FileTransferModel fileTransferObject)
	{

		try
		{
			if (OfflineUtils.isConnectedToSameMsisdn(fileTransferObject.getPacket(), getConnectedDevice()))
			{
				addToCurrentSendingFile(fileTransferObject.getMessageId(), fileTransferObject);
				fileTransferQueue.put(fileTransferObject);
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onRequestPeers() {
		connectionManager.requestPeers(this);
	}

	public  boolean copyFile(InputStream inputStream, OutputStream outputStream,long fileSize)
	{
		return copyFile(inputStream, outputStream,-1,false,false,fileSize);
	}

	public boolean copyFile(InputStream inputStream, OutputStream out, long msgId, boolean showProgress, boolean isSent,long fileSize) 
	{
		byte buf[] = new byte[OfflineConstants.CHUNK_SIZE];
		int len = 0;
		boolean isCopied = false;

		try
		{

			long prev = 0;
			while (fileSize >= OfflineConstants.CHUNK_SIZE)
			{
				int readLen = 0;
				readLen = inputStream.read(buf, 0, OfflineConstants.CHUNK_SIZE);
				out.write(buf, 0, readLen);
				len += readLen;
				fileSize -= readLen;
				if (showProgress && ((len / OfflineConstants.CHUNK_SIZE) != prev))
				{
					prev = len / OfflineConstants.CHUNK_SIZE;
					Logger.d(TAG, "Chunk read " + prev + "");
					showSpinnerProgress(isSent, msgId);
				}
			}

			while (fileSize > 0)
			{
				buf = new byte[(int) fileSize];
				len = inputStream.read(buf);
				fileSize -= len;
				out.write(buf, 0, len);

			}
			isCopied = true;
		}
		catch (IOException e)
		{
			Logger.e("Spinner", "Exception in copyFile: ", e);
			isCopied = false;
		}
		return isCopied;
	}

	public void showSpinnerProgress(boolean isSent,long msgId)
	{
		if (isSent)
		{
			FileTransferModel fileTransfer = currentSendingFiles.get(msgId);
			if (fileTransfer != null)
			{
				fileTransfer.getTransferProgress().setCurrentChunks(fileTransfer.getTransferProgress().getCurrentChunks() + 1);
			}
		}
		else
		{
			FileTransferModel fileTransfer = currentReceivingFiles.get(msgId);

			if (fileTransfer != null)
			{
				Logger.d(TAG, "Received side Current chunk is " + fileTransfer.getTransferProgress().getCurrentChunks());
				fileTransfer.getTransferProgress().setCurrentChunks(fileTransfer.getTransferProgress().getCurrentChunks() + 1);
			}
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	public BlockingQueue<JSONObject> getTextQueue()
	{
		return textMessageQueue;
	}

	public BlockingQueue<FileTransferModel> getFileTransferQueue()
	{
		return fileTransferQueue;
	}

	public String getHostAddress()
	{
		String host = null;
		if (isHotspotCreated())
		{
			host = OfflineUtils.getIPFromMac(null);
		}
		else
		{
			host = OfflineConstants.IP_SERVER;
		}
		return host;
	}

	public FileSavedState getFileState(ConvMessage convMessage, File file)
	{
		return convMessage.isSent() ? getUploadFileState(convMessage,file):getDownloadFileState(convMessage,file); 
	}

	private FileSavedState getUploadFileState(ConvMessage convMessage, File file)
	{
		long msgId = convMessage.getMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		if (currentSendingFiles.containsKey(msgId))
		{
			Logger.d("Spinner", "Current Msg Id -> " + msgId);
			fss = new FileSavedState(FTState.IN_PROGRESS, (int) file.length(), currentSendingFiles.get(msgId).getTransferProgress().getCurrentChunks() * 1024);
		}
		else
		{
			Logger.d("Spinner", "Completed Msg Id -> " + msgId);
			fss = new FileSavedState(FTState.COMPLETED, hikeFile.getFileKey());
		}
		return fss;
	}

	/**
	 * 
	 * @param convMessage
	 * @param file
	 * @return
	 * TODO:Removing the try catch for the app to crash. So that we can debug, what the issue was.
	 */
	private FileSavedState getDownloadFileState(ConvMessage convMessage, File file)
	{
		long msgId = convMessage.getMappedMsgID();
		FileSavedState fss = null;
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		
		if (currentReceivingFiles.containsKey(msgId))
		{
			Logger.d("Spinner", "Current Msg Id -> " + msgId);
			fss = new FileSavedState(FTState.IN_PROGRESS, (int) file.length(), currentReceivingFiles.get(msgId).getTransferProgress().getCurrentChunks() * 1024);

		}
		else
		{
			Logger.d("Spinner", "Completed Msg Id -> " + msgId);
			fss = new FileSavedState(FTState.COMPLETED, hikeFile.getFileKey());
		}
		return fss;
	}

	public 	void setInOfflineFileTransferInProgress(boolean val)
	{
		this.inFileTransferInProgress = val;
	}

	public String getConnectedDevice()
	{
		return connectedDevice;
	}

	public void addToCurrentReceivingFile(long msgId,FileTransferModel fileTransferModel)
	{
		Logger.d(TAG,"addToCurrentReceivingFile msg id is " +msgId);
		currentReceivingFiles.put(msgId,fileTransferModel);
	}

	public void removeFromCurrentReceivingFile(long msgId)
	{
		if(currentReceivingFiles.containsKey(msgId))
		{
			currentReceivingFiles.remove(msgId);
		}
	}
	
	public void addToCurrentSendingFile(long msgId, FileTransferModel fileTransferModel)
	{
		currentSendingFiles.put(msgId, fileTransferModel);
	}
	
	public void removeFromCurrentSendingFile(long msgId)
	{
		if(currentSendingFiles.containsKey(msgId))
		{
			Logger.d(TAG,"Removing message from removeFromCurrentSendingFile "  + "..."+ msgId);
			currentSendingFiles.remove(msgId);
		}
	}

	public void initialiseOfflineFileTransfer(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
			int attachmentType, String msisdn,String apkLabel)
	{
		int type = hikeFileType.ordinal();
		File file = new File(filePath);
		String fileName = file.getName();
		if (type == HikeFileType.APK.ordinal())
			fileName = apkLabel + ".apk";
		ConvMessage convMessage = FileTransferManager.getInstance(context).uploadOfflineFile(msisdn, file, fileKey, fileType, hikeFileType, isRecording,
				recordingDuration, attachmentType, fileName);
		FileTransferModel fileTransferModel=new FileTransferModel(new TransferProgress(0,OfflineUtils.getTotalChunks((int)file.length())), convMessage.serialize());
		Logger.d(TAG,"Total Chunk is "+fileTransferModel.getTransferProgress().getTotalChunks() + "...Current chunk is "+fileTransferModel.getTransferProgress().getCurrentChunks());
		
		Message msg=Message.obtain();
		msg.what=HandlerConstants.SAVE_MSG_PERSISTANCE_DB;
		msg.obj=fileTransferModel;
		performWorkOnBackEndThread(msg);
		
	}

	@Override
	public void onHotSpotConnected() {

	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		//pass results to listeners
		for(IOfflineCallbacks  offlineListener : listeners)
		{
			offlineListener.wifiP2PScanResults(peers);
		}

	}

	@Override
	public void onScanResultAvailable() 
	{
		scanResultsAvailable =true;
		Map<String,ScanResult> results = connectionManager.getWifiNetworksForMyMsisdn();
		
		if (startedForChatThread)
		{
			String ssid = OfflineUtils.getSsidForMsisdn(OfflineUtils.getMyMsisdn(), connectinMsisdn);
			if (results.containsKey(ssid))
			{
				connectionManager.connectToHotspot(connectinMsisdn);
				startedForChatThread = false;
				// since we already have the result no need to scan again
				removeMessage(OfflineConstants.HandlerConstants.RECONNECT_TO_HOTSPOT);
			}
		}
		else
		{
			//pass results to listeners
			for(IOfflineCallbacks offlineListener : listeners)
			{
				offlineListener.wifiScanResults(results);
			}
		}
		
	}

	public void addListener(IOfflineCallbacks listener) {
		if(listener==null)
			return;

		listeners.add(listener);
		
		if(listeners.size()==1)
		{
			IntentFilter intentFilter = new IntentFilter();
			addIntentFilters(intentFilter);
			context.registerReceiver(receiver,intentFilter);
		}
	}

	private void addIntentFilters(IntentFilter intentFilter)
	{
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
	        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
	        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
	        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);		
	}

	@Override
	public void checkConnectedNetwork() {
		String offlineNetworkMsisdn = connectionManager.getConnectedHikeNetworkMsisdn();
		Logger.d(TAG, "CheckConnectedNetwork");
		
		if (offlineNetworkMsisdn != null && connectedDevice == null && offlineState == OFFLINE_STATE.CONNECTING)
		{
			threadManager.startReceivingThreads();
			threadManager.startSendingThreads();
			
			// send ping packet to hotspot
			
			onConnected(offlineNetworkMsisdn);
			sendPingPacket();
			
		}
	}
	
	private void sendPingPacket()
	{
		JSONObject pingPacket = OfflineUtils.createPingPacket();
		addToTextQueue(pingPacket);
	}

	public void onConnected(String connectedMsisdn)
	{	
		if(connectedDevice==null)
		{
			Logger.d("OfflineManager","connected Device is "+connectedMsisdn);
			connectedDevice = connectedMsisdn;
			removeMessage(OfflineConstants.HandlerConstants.DISCONNECT_AFTER_TIMEOUT);
			removeMessage(OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT);
			offlineState=OFFLINE_STATE.CONNECTED;
			final ConvMessage convMessage=OfflineUtils.createOfflineInlineConvMessage("o:"+connectedDevice,context.getString(R.string.connection_established),OfflineConstants.OFFLINE_MESSAGE_CONNECTED_TYPE);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);
			for(IOfflineCallbacks  offlineListener : listeners)
			{
				offlineListener.connectedToMsisdn(connectedDevice);
			}
			Message msg=Message.obtain();
			msg.what=HandlerConstants.SEND_PERSISTANCE_MSGS;
			performWorkOnBackEndThread(msg);

			// send ghost packet and post disconnect for timeout
			//startSendingGhostPackets(connectedMsisdn);
			//postDisconnectForGhostPackets(connectedMsisdn);
		}
	}
	
	private void sendPersistantMsgs()
	{
		List<OfflineHikePacket> packets = HikeOfflinePersistence.getInstance().getAllSentMessages("o:" + getConnectedDevice());
		Logger.d(TAG, "List of offline msg foir the user are " + packets.toString());
	}

	private void startSendingGhostPackets(String msisdn)
	{
		Message msg = Message.obtain();
		msg.what = OfflineConstants.HandlerConstants.SEND_GHOST_PACKET;
		msg.obj = msisdn;
		performWorkOnBackEndThread(msg);
	}
	
	private void sendGhostPacket(String msisdn)
	{
		JSONObject ghost = OfflineUtils.createGhostPacket(msisdn);
		addToTextQueue(ghost);
	}
	
	private void postDisconnectForGhostPackets(String msisdn)
	{
		Message msg = Message.obtain();
		msg.what = OfflineConstants.HandlerConstants.DISCONNECT_AFTER_TIMEOUT;
		msg.obj = msisdn;
		handler.sendMessageDelayed(msg,OfflineConstants.GHOST_PACKET_DISCONNECT_TIMEOUT);
	}
	
	public void restartGhostTimeout()
	{
		removeMessage(OfflineConstants.HandlerConstants.DISCONNECT_AFTER_TIMEOUT);
		postDisconnectForGhostPackets(connectedDevice);
	}

	public void removeMessage(int msg)
	{
		handler.removeMessages(msg);
	}

	@Override
	public void onDiscoveryStarted() {

	}

	@Override
	public void onDiscoveryStopped() {

	}

	public void startDiscovery()
	{
		connectionManager.startDiscovery();
	}

	public void stopDiscovery()
	{
		connectionManager.stopDiscovery();
	}

	public void createHotspot(final String msisdn)
	{
		Message msg = Message.obtain();
		msg.what = OfflineConstants.HandlerConstants.CREATE_HOTSPOT;
		msg.obj = msisdn;
		performWorkOnBackEndThread(msg);
		threadManager.startReceivingThreads();
		// waitForConnection(msisdn);
	}

	// Call removed from createHotspot method
	private void waitForConnection(String msisdn) 
	{
		Message msg = Message.obtain();
		msg.what = OfflineConstants.HandlerConstants.DISCONNECT_AFTER_TIMEOUT;
		msg.obj = msisdn;
		handler.sendMessageDelayed(msg,OfflineConstants.WAITING_TIMEOUT);
	}

	public void connectToHotspot(String msisdn)
	{
		threadManager.startReceivingThreads();
		threadManager.startSendingThreads();
		connectionManager.connectToHotspot(msisdn);
	}
	
	String connectinMsisdn  = "";
	public void connectAsPerMsisdn(final String msisdn) 
	{
		if(offlineState==OFFLINE_STATE.CONNECTING)
		{
			HikeMessengerApp.getInstance().showToast("We are already connecting");
			return;
		}
		setOfflineState(OFFLINE_STATE.CONNECTING);
		String myMsisdn = OfflineUtils.getMyMsisdn();
		if(myMsisdn.compareTo(msisdn)>0)
		{
			Logger.d(TAG,"Will create Hotspot");
			createHotspot(msisdn);
		}
		else
		{
			Logger.d(TAG,"Will connect to  Hotspot");
			connectinMsisdn = msisdn;
			
			Message msg = Message.obtain();
			msg.what = OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT;
			msg.obj = msisdn;
			performWorkOnBackEndThread(msg);
			
			
			// removing the CONNECT_TO_HOTSPOT message from handler after timeout
			Message endTries = Message.obtain();
			endTries.what = OfflineConstants.HandlerConstants.REMOVE_CONNECT_MESSAGE; 
			endTries.obj  = msisdn;
			handler.sendMessageDelayed(endTries, OfflineConstants.TIME_TO_CONNECT);
			
		}
	}

	public boolean isHotspotCreated() 
	{
		return connectionManager.isHotspotCreated();
	}

	public void startWifiScan() {
		connectionManager.startWifiScan();
	}

	public void startWifi() {
		connectionManager.startWifi();
	}

	public void stopWifi() {
		connectionManager.stopWifi();
	}


	public void startScan() {
		Message startScan   =  Message.obtain();
		startScan.what = OfflineConstants.HandlerConstants.START_SCAN;
		startScan.obj = tryGetScanResults;
		performWorkOnBackEndThread(startScan);
	}

	private void runNetworkScan(int attemptNumber)
	{

		if (attemptNumber < OfflineConstants.MAXTRIES_FOR_SCAN_RESULTS || (scanResultsAvailable == true))
		{
			connectionManager.startDiscovery();
			try 
			{
				Thread.sleep(8000);
			}
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
			connectionManager.stopDiscovery();
			try 
			{
				Thread.sleep(2000);
			}
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			} 

			connectionManager.startWifiScan();
		}
		else
		{
			connectionManager.stopWifi();

			while(connectionManager.isWifiEnabled())
			{
				try 
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
				Logger.d(TAG, "Waiting for wifi to stop");
			}
			connectionManager.startWifi();
			Logger.d(TAG, "Called start wifi");
			while(!connectionManager.isWifiEnabled())
			{
				try 
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
				Logger.d(TAG, "Waiting for wifi to start");
			}
			Logger.d(TAG, "Wifi is on, now scanning for any available wifi hotspots");
			connectionManager.startWifiScan();
		}
	}
	
	public void stopScan() {
		removeMessage(OfflineConstants.HandlerConstants.START_SCAN);
	}
	
	public void setOfflineState(OFFLINE_STATE offlineState)
	{
		this.offlineState=offlineState;
		Logger.d("OfflineManager","Offline state is "+offlineState);
	}
	
	public OFFLINE_STATE getOfflineState()
	{
		return offlineState;
	}

	public void removeListener(IOfflineCallbacks listener)
	{
		if (listeners.contains(listener))
		{
			listeners.remove(listener);
		}
	}
	
	public void setupFileState(FTViewHolder holder, FileSavedState fss, long msgId, HikeFile hikeFile, boolean isSent, boolean ext)
	{
		int playImage = -1;
		if (!ext)
		{
			playImage = R.drawable.ic_videoicon;
		}
		holder.ftAction.setVisibility(View.GONE);
		holder.circularProgressBg.setVisibility(View.GONE);
		holder.initializing.setVisibility(View.GONE);
		holder.circularProgress.setVisibility(View.GONE);
		switch (fss.getFTState())
		{
		case IN_PROGRESS:
			holder.circularProgressBg.setVisibility(View.VISIBLE);
			holder.circularProgress.setVisibility(View.VISIBLE);
			Logger.d(TAG, "IN_PROGRESS");
			showTransferProgress(holder, fss, msgId, hikeFile, isSent);
			
			break;
		case COMPLETED:
			holder.circularProgressBg.setVisibility(View.GONE);
			holder.circularProgress.resetProgress();
			Logger.d(TAG, "COMPLETED");
			holder.circularProgress.setVisibility(View.GONE);
			if (hikeFile.getHikeFileType() == HikeFileType.VIDEO && !ext)
			{
				holder.ftAction.setImageResource(playImage);
				holder.ftAction.setVisibility(View.VISIBLE);
				holder.circularProgressBg.setVisibility(View.VISIBLE);
			}
			break;
		default:
			break;
		}
		holder.ftAction.setScaleType(ScaleType.CENTER);
	}
	
	private void showTransferProgress(FTViewHolder holder, FileSavedState fss, long msgId, HikeFile hikeFile, boolean isSent)
	{
		Logger.d(TAG,"in showTransferProgress");
		int num = 0;
		if(isSent)
		{
			if (!currentSendingFiles.containsKey(msgId))
				return;
			else
				num = currentSendingFiles.get(msgId).getTransferProgress().getCurrentChunks();
		}else
		{
			Logger.d(TAG,"showTransferProgress trying to get msg id is " +msgId);
			if (!currentReceivingFiles.containsKey(msgId))
				return;
			else
				num = currentReceivingFiles.get(msgId).getTransferProgress().getCurrentChunks();
		}
	
		long progress = (((long)num*OfflineConstants.CHUNK_SIZE*100)/hikeFile.getFileSize());
		Logger.d(TAG, "CurrentSizeReceived: " + num + " FileSize: " + hikeFile.getFileSize() + 
				" Progress -> " +  progress +  " FtState -> " + fss.getFTState().name());
		if (fss.getFTState() == FTState.IN_PROGRESS && fss.getTransferredSize() == 0)
		{
			float animatedProgress = 5 * 0.01f;
			if (fss.getTotalSize() > 0 && OfflineConstants.CHUNK_SIZE > 0)
			{
				animatedProgress = (float) OfflineConstants.CHUNK_SIZE;
				animatedProgress = animatedProgress / fss.getTotalSize();
			}
			if (holder.circularProgress.getRelatedMsgId() == -1 || holder.circularProgress.getCurrentProgress() > animatedProgress
					|| holder.circularProgress.getCurrentProgress() == 1.0f)
			{
				holder.circularProgress.resetProgress();
				Logger.d("Spinner", "Current Progress reset");
			}
			if (Utils.isHoneycombOrHigher())
			{
				holder.circularProgress.stopAnimation();
				holder.circularProgress.setAnimatedProgress(0, (int) (animatedProgress * 100), 6 * 1000);
			}
			else
			{
				holder.circularProgress.setProgress(animatedProgress);
			}
			holder.circularProgress.setRelatedMsgId(msgId);
			holder.circularProgress.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
		}
		else if (fss.getFTState() == FTState.IN_PROGRESS)
		{
			if (progress < 100)
				holder.circularProgress.setProgress(progress * 0.01f);
			if (Utils.isHoneycombOrHigher())
				holder.circularProgress.stopAnimation();
			
			Logger.d("Spinner", "" + "holder.circularProgress=" + holder.circularProgress.getCurrentProgress()*100
					+ " Progress=" + progress);
			
			float animatedProgress = 5 * 0.01f;
			if (fss.getTotalSize() > 0)
			{
				animatedProgress = (float) OfflineConstants.CHUNK_SIZE;
				animatedProgress = animatedProgress / fss.getTotalSize();
			}
			if (Utils.isHoneycombOrHigher())
			{
				if (holder.circularProgress.getCurrentProgress() < (0.95f) && progress == 100)
				{
					holder.circularProgress.setAnimatedProgress((int) (holder.circularProgress.getCurrentProgress() * 100), (int)progress, 300);
				}
				else
					holder.circularProgress.setAnimatedProgress((int)progress, (int)progress + (int) (animatedProgress * 100), 6 * 1000);
			}
			
			holder.circularProgress.setVisibility(View.VISIBLE);
			holder.circularProgressBg.setVisibility(View.VISIBLE);
		}
	}

}