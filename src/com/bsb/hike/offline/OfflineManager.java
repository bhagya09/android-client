package com.bsb.hike.offline;
import static com.bsb.hike.offline.OfflineConstants.IP_SERVER;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;


/**
 * 
 * @author himanshu
 * This class forms the base of Offline Messaging and deals with socket connection,text transfer and file transfer queue. 
 */

public class OfflineManager implements IWIfiReceiverCallback , PeerListListener
{
	public static final OfflineManager _instance = new OfflineManager();

	private ConcurrentHashMap<Long, FileTransferModel> currentSendingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private ConcurrentHashMap<Long, FileTransferModel> currentReceivingFiles = new ConcurrentHashMap<Long, FileTransferModel>();

	private final static Object currentReceivingFilesLock = new Object();

	private final static Object currentSendingFilesLock = new Object();
	
	private BlockingQueue<JSONObject> textMessageQueue = null;
	
	private ArrayList<IOfflineCallbacks> listeners;

	private BlockingQueue<FileTransferModel> fileTransferQueue = null;

	private volatile boolean inFileTransferInProgress=false;
	
	private Context context;
	
	private String connectedDevice =null;
	
	private ConnectionManager connectionManager;
	
	// 60 seconds waitng time after creating hotspot
	private long waitingTimeOut = 60000;
	private Runnable waitingTimerRunnable;
		
	private static final String TAG=OfflineManager.class.getName();
	
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

	public static OfflineManager getInstance()
	{
		return _instance;
	}
	
	
	private void handleMsgOnBackEndThread(Message msg)
	{
		switch(msg.what)
		{
		case OfflineConstants.HandlerConstants.SAVE_MSG_DB:
			saveToDb((ConvMessage)msg.obj);
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
		listeners = new ArrayList<IOfflineCallbacks>();
		initTimerRunnables();
		setDeviceNameAsMsisdn();
	}

	private void setDeviceNameAsMsisdn() {
		// To Do : Restore back to previous deviceName
		connectionManager.setDeviceNameAsMsisdn();
	}

	private void initTimerRunnables() {
		waitingTimerRunnable = new Runnable() {
			@Override
			public void run() {
				disconnect();
			}
		};
		
	}
	
	public void disconnect() {
	
	}

	private void closeConnection(String deviceName) 
	{
		
	}


	public synchronized void addToTextQueue(JSONObject message)
	{
		textMessageQueue.add(message);
	}
	
	public synchronized void addToFileQueue(FileTransferModel fileTransferObject)
	{
		fileTransferQueue.add(fileTransferObject);
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
			synchronized (currentSendingFilesLock) {
				FileTransferModel fileTransfer=currentSendingFiles.get(msgId);
				fileTransfer.getTransferProgress().setCurrentChunks(fileTransfer.getTransferProgress().getCurrentChunks() + 1);
			}
		}
		else
		{
			synchronized (currentReceivingFilesLock) {
				FileTransferModel fileTransfer=currentReceivingFiles.get(msgId);
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
				synchronized (currentSendingFilesLock) {
					currentSendingFiles.remove(msgID);
				}
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
		synchronized (currentSendingFilesLock)
		{
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
		synchronized (currentReceivingFilesLock)
		{
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
		if(currentReceivingFiles.contains(msgId))
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
		HashMap<String,ScanResult> results = connectionManager.getWifiNetworksForMyMsisdn();
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
		Pair<Boolean,String> offlineNetwork = connectionManager.isConnectedToHikeNetwork();
		if(offlineNetwork.first)
		{
			String ssid  =  offlineNetwork.second;
			onConnected(ssid);
		}
	}
	
	public void onConnected(String connectedMsisdn)
	{
		connectedDevice = connectedMsisdn;
		removeRunnable(waitingTimerRunnable);
		
		for(IOfflineCallbacks  offlineListener : listeners)
		{
			offlineListener.connectedToMsisdn(connectedDevice);
		}
		
	}
	
	public void removeRunnable(Runnable r)
	{
		if (r != null)
			HikeHandlerUtil.getInstance().removeRunnable(r);
	}
	

	@Override
	public void onDiscoveryStarted() {
	
	}

	@Override
	public void onDiscoveryStopped() {
		
	}
	
	public void createHotspot(String msisdn)
	{
		connectionManager.createHotspot(msisdn);
		waitForConnection();
	}
	
	private void waitForConnection() 
	{
		handler.postDelayed(waitingTimerRunnable,waitingTimeOut);
	}

	public void connectToHotspot(String msisdn)
	{
		connectionManager.connectToHotspot(msisdn);
	}

	public void connectAsPerMsisdn(String msisdn) 
	{
		connectionManager.connectAsPerMsisdn(msisdn);
	}

	public boolean isHotspotCreated() 
	{
		return connectionManager.isHotspotCreated();
	}
}