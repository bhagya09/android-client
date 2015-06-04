package com.bsb.hike.offline;
import static com.bsb.hike.offline.OfflineConstants.IP_SERVER;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.utils.Logger;

import java.util.ArrayList;
import java.util.Map;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;


/**
 * 
 * @author himanshu, deepak malik
 * This class forms the base of Offline Messaging and deals with socket connection,text transfer and file transfer queue. 
 */

public class OfflineManager implements IWIfiReceiverCallback, PeerListListener
{
	private static OfflineManager _instance = null;

	private Map<Long, FileTransferModel> currentSendingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private Map<Long, FileTransferModel> currentReceivingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private BlockingQueue<JSONObject> textMessageQueue = null;

	private ArrayList<IOfflineCallbacks> listeners;

	private BlockingQueue<FileTransferModel> fileTransferQueue = null;

	private volatile boolean inFileTransferInProgress=false;

	private Context context;

	private String connectedDevice =null;

	private ConnectionManager connectionManager;

	private static final String TAG=OfflineManager.class.getName();

	private OfflineThreadManager threadManager;

	private boolean scanResultsAvailable =  false;

	private boolean isConnectedToHotspot= false;
	
	private int tryGetScanResults = 0;

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
			synchronized (OfflineManager.class) {
				if (_instance == null) {
					_instance = new OfflineManager();
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
			isConnectedToHotspot = connectionManager.connectToNetwork((String)msg.obj);
			if(!isConnectedToHotspot)
			{
				handler.sendMessageDelayed(msg, OfflineConstants.TRY_CONNECT_TO_HOTSPOT);
			}
			break;
		case OfflineConstants.HandlerConstants.REMOVE_CONNECT_MESSAGE:
			removeMessage(OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT);
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
		}
	};

	private void saveToDb(ConvMessage convMessage)
	{
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage,true);
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
		threadManager = OfflineThreadManager.getInstance();
		listeners = new ArrayList<IOfflineCallbacks>();
		setDeviceNameAsMsisdn();
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
		textMessageQueue.add(message);
	}

	public synchronized void addToFileQueue(FileTransferModel fileTransferObject)
	{
		fileTransferQueue.add(fileTransferObject);
		addToCurrentSendingFile(fileTransferObject.getMessageId(), fileTransferObject);
	}

	@Override
	public void onRequestPeers() {
		connectionManager.requestPeers(this);
	}

	public boolean copyFile(InputStream inputStream, OutputStream outputStream,long fileSize)
	{
		return copyFile(inputStream, outputStream,-1,false,false,fileSize);
	}

	public boolean copyFile(InputStream inputStream, OutputStream out, long msgId, boolean showProgress, boolean isSent,long fileSize) 
	{
		byte buf[] = new byte[OfflineConstants.CHUNK_SIZE];
		int len;
		boolean isCopied = false;
		try {
			while (fileSize>=OfflineConstants.CHUNK_SIZE) {
				len = inputStream.read(buf);
				out.write(buf, 0, len);
				fileSize -= len;	
				if (showProgress)
				{

					showSpinnerProgress(isSent,msgId);
				}
			}
			while(fileSize > 0) 
			{
				buf = new byte[(int)fileSize];
				len = inputStream.read(buf);
				fileSize -= len;
				out.write(buf, 0, len);
				if (showProgress)
				{
					showSpinnerProgress(isSent,msgId);
				}
			}
			isCopied = true;
		} catch (IOException e) {
			Logger.e("Spinner", "Exception in copyFile: ", e);
			isCopied = false;
		}
		return isCopied;
	}

	private void showSpinnerProgress(boolean isSent,long msgId)
	{
		if(isSent)
		{
			FileTransferModel fileTransfer=currentSendingFiles.get(msgId);
			fileTransfer.getTransferProgress().setCurrentChunks(fileTransfer.getTransferProgress().getCurrentChunks() + 1);
		}
		else
		{
			FileTransferModel fileTransfer=currentReceivingFiles.get(msgId);

			fileTransfer.getTransferProgress().setCurrentChunks(fileTransfer.getTransferProgress().getCurrentChunks() + 1);
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
			host = IP_SERVER;
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
			Logger.d(TAG, "Current Msg Id -> " + msgId);
			fss = new FileSavedState(FTState.IN_PROGRESS, (int) file.length(), currentSendingFiles.get(msgId).getTransferProgress().getCurrentChunks() * 1024);
		}
		else
		{
			Logger.d(TAG, "Completed Msg Id -> " + msgId);
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
			Logger.d(TAG, "Current Msg Id -> " + msgId);
			fss = new FileSavedState(FTState.IN_PROGRESS, (int) file.length(), currentReceivingFiles.get(msgId).getTransferProgress().getCurrentChunks() * 1024);

		}
		else
		{
			Logger.d(TAG, "Completed Msg Id -> " + msgId);
			fss = new FileSavedState(FTState.COMPLETED, hikeFile.getFileKey());
		}
		return fss;
	}

	public void setInOfflineFileTransferInProgress(boolean val)
	{
		inFileTransferInProgress = val;
	}

	public String getConnectedDevice()
	{
		return connectedDevice;
	}

	public void addToCurrentReceivingFile(long msgId,FileTransferModel fileTransferModel)
	{
		currentReceivingFiles.put(msgId,fileTransferModel);
	}

	public void removeFromCurrentReceivingFile(long msgId)
	{
		if(((ConcurrentHashMap<Long, FileTransferModel>)currentReceivingFiles).contains(msgId))
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
		if(((ConcurrentHashMap<Long, FileTransferModel>)currentSendingFiles).contains(msgId))
		{
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
		addToFileQueue(new FileTransferModel(new TransferProgress(), convMessage.serialize()));
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
	public void onScanResultAvailable() {

		scanResultsAvailable = true;
		Map<String,ScanResult> results = connectionManager.getWifiNetworksForMyMsisdn();
		
		//pass results to listeners
		for(IOfflineCallbacks  offlineListener : listeners)
		{
			offlineListener.wifiScanResults(results);
		}
	}

	public void addListener(IOfflineCallbacks listener) {
		if(listener==null)
			return;
		listeners.add(listener);
	}

	@Override
	public void checkConnectedNetwork() {
		String offlineNetworkMsisdn = connectionManager.getConnectedHikeNetworkMsisdn();
		if(offlineNetworkMsisdn!=null)
		{
			onConnected(offlineNetworkMsisdn);
		}
	}

	public void onConnected(String connectedMsisdn)
	{
		connectedDevice = connectedMsisdn;
		removeMessage(OfflineConstants.HandlerConstants.DISCONNECT_AFTER_TIMEOUT);

		for(IOfflineCallbacks offlineListener : listeners)
		{
			offlineListener.connectedToMsisdn(connectedDevice);
		}

		// send ghost packet and post disconnect for timeout
		startSendingGhostPackets(connectedMsisdn);
		postDisconnectForGhostPackets(connectedMsisdn);
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
		threadManager.startReceivingThreads();
		Message msg = Message.obtain();
		msg.what = OfflineConstants.HandlerConstants.CREATE_HOTSPOT;
		msg.obj = msisdn;
		performWorkOnBackEndThread(msg);
		waitForConnection(msisdn);
	}

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

	public void connectAsPerMsisdn(final String msisdn) 
	{
		String myMsisdn = OfflineUtils.getMyMsisdn();
		if(myMsisdn.compareTo(msisdn)>0)
		{
			createHotspot(msisdn);
		}
		else
		{
			threadManager.startReceivingThreads();
			threadManager.startSendingThreads();
			Message msg = Message.obtain();
			msg.what = OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT;
			msg.obj = msisdn;
			performWorkOnBackEndThread(msg);
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
}

