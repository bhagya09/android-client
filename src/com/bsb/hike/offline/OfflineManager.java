package com.bsb.hike.offline;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
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
import android.util.Pair;

import com.bsb.hike.HikeConstants;
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


/**
 * 
 * @author himanshu
 * This class forms the base of Offline Messaging and deals with socket connection,text transfer and file transfer queue. 
 */

public class OfflineManager implements IWIfiReceiverCallback , PeerListListener
{
	private static OfflineManager _instance = null;

	private Map<Long, FileTransferModel> currentSendingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private Map<Long, FileTransferModel> currentReceivingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private final static Object currentReceivingFilesLock = new Object();

	private final static Object currentSendingFilesLock = new Object();

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
	
	OfflineBroadCastReceiver receiver;

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
			isConnectedToHotspot= connectionManager.connectToNetwork((String)msg.obj);
			Logger.d("OfflineManager","isConnectedToHotspot "+isConnectedToHotspot);
			if(!isConnectedToHotspot)
			{
				Message retryMsg = Message.obtain();
				retryMsg.what = OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT;
				retryMsg.obj = (String)msg.obj;
				handler.sendMessageDelayed(retryMsg,OfflineConstants.TRY_CONNECT_TO_HOTSPOT);
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
		}
	};

	private void saveToDb(ConvMessage convMessage)
	{
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage,true);
		addToTextQueue(convMessage.serialize());
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
		receiver=new OfflineBroadCastReceiver(this);
	}

	private void setDeviceNameAsMsisdn() {
		// TODO : Restore back to previous deviceName
		connectionManager.setDeviceNameAsMsisdn();
	}
	public void disconnect(String msisdn) {
		connectionManager.disconnect(msisdn);
	}

	public synchronized void addToTextQueue(JSONObject message)
	{
		try
		{
			textMessageQueue.put(message);
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
			fileTransferQueue.put(fileTransferObject);
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

	public boolean sendOfflineFile(FileTransferModel fileTransferModel,OutputStream outputStream)
	{
		inFileTransferInProgress = true;
		boolean isSent = true;
		String fileUri =null;
		InputStream inputStream = null;
		JSONObject  jsonFile =  null;
		try
		{
			JSONArray jsonFiles = fileTransferModel.getPacket().getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES);
			jsonFile = (JSONObject) jsonFiles.get(0);
			fileUri = jsonFile.getString(HikeConstants.FILE_PATH);

			String metaString = fileTransferModel.getPacket().toString();
			Logger.d(TAG, metaString);
			byte[] metaDataBytes = metaString.getBytes("UTF-8");
			int length = metaDataBytes.length;
			Logger.d(TAG, "Sizeof metaString: " + length);
			byte[] intToBArray = OfflineUtils.intToByteArray(length);
			outputStream.flush();
			outputStream.write(intToBArray, 0, intToBArray.length);
			ByteArrayInputStream byteArrayInputStream =  new ByteArrayInputStream(metaDataBytes);
			boolean isMetaDataSent = copyFile(byteArrayInputStream, outputStream, metaDataBytes.length);
			Logger.d(TAG, "FileMetaDataSent:" + isMetaDataSent);
			byteArrayInputStream.close();
			JSONObject metadata;
			int fileSize  = 0;
			metadata = fileTransferModel.getPacket().getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
			JSONObject fileJSON = (JSONObject) metadata.getJSONArray(HikeConstants.FILES).get(0);
			fileSize = fileJSON.getInt(HikeConstants.FILE_SIZE);
			long msgID;
			msgID = fileTransferModel.getPacket().getJSONObject(HikeConstants.DATA).getLong(HikeConstants.MESSAGE_ID);
			fileTransferModel.getTransferProgress().setCurrentChunks(OfflineUtils.getTotalChunks(fileSize));

			//TODO:We can listen to PubSub ...Why to do this ...????
			//showUploadTransferNotification(msgID,fileSize);


			isSent = copyFile(inputStream, outputStream, msgID, true, true,fileSize);
			currentSendingFiles.remove(msgID);
			String msisdn = fileTransferModel.getPacket().getString(HikeConstants.TO);
			HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, null);
			int rowsUpdated = OfflineUtils.updateDB(msgID, ConvMessage.State.SENT_DELIVERED, msisdn);
			if (rowsUpdated == 0)
			{
				Logger.d(getClass().getSimpleName(), "No rows updated");
			}
			Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgID);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_DELIVERED, pair);
		}
		catch(JSONException e)
		{
			e.printStackTrace();
			return false;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
		inFileTransferInProgress=false;
		return isSent;


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
	 * TODO:Removing the try catch for the app to crash .So that we cab debug ki what was the issue.
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

	public 	void setIsOfflineFileTransferInProgress(boolean val)
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

		scanResultsAvailable =true;
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
		Logger.d("OfflineManager","checkConnectedNetwork");
		if(offlineNetworkMsisdn!=null)
		{
			onConnected(offlineNetworkMsisdn);
		}
	}

	public void onConnected(String connectedMsisdn)
	{
		Logger.d("OfflineManager","onConnected() called");
		connectedDevice = connectedMsisdn;
		removeMessage(OfflineConstants.HandlerConstants.DISCONNECT_AFTER_TIMEOUT);
		removeMessage(OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT);
		threadManager.startReceivingThread();
		threadManager.startSendingThread();
		for(IOfflineCallbacks  offlineListener : listeners)
		{
			offlineListener.connectedToMsisdn(connectedDevice);
		}

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
		threadManager.startReceivingThread();
		//waitForConnection(msisdn);
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
		threadManager.startReceivingThread();
		threadManager.startSendingThread();
		connectionManager.connectToHotspot(msisdn);
	}

	public void connectAsPerMsisdn(final String msisdn) 
	{
		String myMsisdn = OfflineUtils.getMyMsisdn();
		if(myMsisdn.compareTo(msisdn)>0)
		{
			Logger.d(TAG,"Will create Hotspot");
			createHotspot(msisdn);
		}
		else
		{
			Logger.d(TAG,"Will connect to  Hotspot");
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