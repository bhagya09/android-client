package com.bsb.hike.offline;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.filetransfer.UploadFileTask;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.offline.OfflineConstants.HandlerConstants;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.ui.ComposeChatActivity.FileTransferData;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.hike.transporter.TException;
import com.hike.transporter.Transporter;
import com.hike.transporter.models.SenderConsignment;

/**
 * 
 * @author himanshu
 *
 *	This class acts as a abstraction for Offline Messaging.The User should use this class to interact with the
 *	Offline Manager
 */

public class OfflineController
{

	private static final String TAG = "OfflineController";

	private OfflineManager offlineManager;
	
	private HikeConverter  hikeConverter;

	public static volatile OfflineController _instance=null;
	
	private OfflineFileManager fileManager;
	
	private volatile OFFLINE_STATE offlineState;
	
	
	private Handler mHandler = new Handler(HikeHandlerUtil.getInstance().getLooper())
	{
		public void handleMessage(android.os.Message msg) {
			if(msg==null)
			{
				return;
			}
			handleMsgOnBackEndThread(msg);
			
		}
	};
	
	public static OfflineController getInstance()
	{
		if (_instance == null)
		{
			synchronized (OfflineController.class)
			{
				if (_instance == null)
				{
					_instance = new OfflineController();
				}
			}
		}
		return _instance;

	}
	
	private void handleMsgOnBackEndThread(Message msg) {
		// TODO Auto-generated method stub
		switch (msg.what) {
		case HandlerConstants.SAVE_MSG_DB:
			saveToDb((ConvMessage) msg.obj);
			break;
		case OfflineConstants.HandlerConstants.SHUTDOWN:
			shutdownProcess((OfflineException) msg.obj);
			break;
		default:
			break;
		}
	}

	private OfflineController()
	{
		fileManager = new OfflineFileManager();
		hikeConverter = new HikeConverter(fileManager);
		offlineManager = new OfflineManager(hikeConverter, hikeConverter);
		setOfflineState(OFFLINE_STATE.NOT_CONNECTED);
		
	}

	public void addListener(IOfflineCallbacks listener)
	{
		offlineManager.addListener(listener);
	}
	public void startScan()
	{
		offlineManager.startScan();
	}

	public void stopScan()
	{
		offlineManager.stopScan();
	}

	public void startWifi()
	{
		offlineManager.startWifi();
	}
	
	public void stopWifi()
	{
		offlineManager.stopWifi();
	}
	
	public String getConnectedDevice()
	{
		return offlineManager.getConnectedDevice();
	}

	/**
	 * Add to Database and send to the recipent.
	 * 
	 * @param convMessage
	 */
	public void sendMessage(ConvMessage convMessage)
	{
		Message msg = Message.obtain();
		msg.what = OfflineConstants.HandlerConstants.SAVE_MSG_DB;
		msg.obj = convMessage;
		mHandler.sendMessage(msg);
	}

	private void saveToDb(ConvMessage convMessage)
	{
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.OFFLINE_MESSAGE_SENT, convMessage);
		SenderConsignment msgConsignment = hikeConverter.getMessageConsignment(convMessage, true);
		offlineManager.sendConsignment(msgConsignment);
	}
	
	public void sendMR(JSONObject object)
	{
		SenderConsignment mrConsignement=hikeConverter.getMRConsignement(object);
		offlineManager.sendConsignment(mrConsignement);
	}
	public void sendAudioFile(String filePath, long duration, String msisdn)
	{
		SenderConsignment audioConsignment = hikeConverter.getFileConsignment(filePath, null, HikeFileType.AUDIO_RECORDING, HikeConstants.VOICE_MESSAGE_CONTENT_TYPE, true, duration,
				FTAnalyticEvents.AUDIO_ATTACHEMENT, msisdn, null);
		offlineManager.sendConsignment(audioConsignment);
	}
	
	// currently using for sharing files...
	public void sendFile(ArrayList<FileTransferData> fileTransferList, String msisdn) 
	{
		for (FileTransferData fileData : fileTransferList)
		{
			String apkLabel = null;
			if (fileData.hikeFileType == HikeFileType.APK)
			{
				apkLabel = fileData.file.getName();
			}
			
			SenderConsignment fileConsignment = hikeConverter.getFileConsignment(fileData.filePath, fileData.fileKey, fileData.hikeFileType, fileData.fileType, fileData.isRecording,  
															fileData.recordingDuration, FTAnalyticEvents.OTHER_ATTACHEMENT, msisdn, apkLabel);
			offlineManager.sendConsignment(fileConsignment);
		}
	}

	public void sendFile(Intent intent ,JSONObject msgExtrasJson, String msisdn)
	{
		String fileKey = null;
		try
		{
			if (msgExtrasJson.has(HikeConstants.Extras.FILE_KEY))
			{
				fileKey = msgExtrasJson.getString(HikeConstants.Extras.FILE_KEY);
			}
			String filePath = msgExtrasJson.getString(HikeConstants.Extras.FILE_PATH);
			String fileType = msgExtrasJson.getString(HikeConstants.Extras.FILE_TYPE);

			boolean isRecording = false;
			long recordingDuration = -1;
			if (msgExtrasJson.has(HikeConstants.Extras.RECORDING_TIME))
			{
				recordingDuration = msgExtrasJson.getLong(HikeConstants.Extras.RECORDING_TIME);
				isRecording = true;
				fileType = HikeConstants.VOICE_MESSAGE_CONTENT_TYPE;
			}

			int attachmentType = FTAnalyticEvents.OTHER_ATTACHEMENT;
			/*
			 * Added to know the attachment type when selected from file.
			 */
			if (intent.hasExtra(FTAnalyticEvents.FT_ATTACHEMENT_TYPE))
			{
				attachmentType = FTAnalyticEvents.FILE_ATTACHEMENT;

			}

			HikeFileType hikeFileType = HikeFileType.fromString(fileType, isRecording);
			if (filePath == null)
			{
				Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
			}
			else
			{
				SenderConsignment fileConsignment = hikeConverter.getFileConsignment(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType,
						msisdn, null);
				offlineManager.sendConsignment(fileConsignment);
			}
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Incorrect JSON");
		}
	}
	
	public void sendFile(Intent intent, String msisdn)
	{
		
		if(intent.hasExtra(HikeConstants.Extras.FILE_PATHS))
		{
			ArrayList<String> filePaths = intent.getStringArrayListExtra(HikeConstants.Extras.FILE_PATHS);
			String fileType = intent.getStringExtra(HikeConstants.Extras.FILE_TYPE);
			for (String filePath : filePaths)
			{
				HikeFileType hikeFileType = HikeFileType.fromString(fileType,false);

				if (filePath == null)
				{
					Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
				}
				else
				{
					SenderConsignment fileConsignment = hikeConverter.getFileConsignment(filePath, null, hikeFileType, fileType, false, -1,FTAnalyticEvents.OTHER_ATTACHEMENT, msisdn, null);
					offlineManager.sendConsignment(fileConsignment);
				}

			}
		}
		else
		{
			String filePath = intent.getStringExtra(HikeConstants.Extras.FILE_PATH);
			String fileType = intent.getStringExtra(HikeConstants.Extras.FILE_TYPE);
			String fileKey = null;

			if (intent.hasExtra(HikeConstants.Extras.FILE_KEY))
			{
				fileKey = intent.getStringExtra(HikeConstants.Extras.FILE_KEY);
			}

			int attachmentType = FTAnalyticEvents.FILE_ATTACHEMENT;

			boolean isRecording = false;
			long recordingDuration = -1;

			if (intent.hasExtra(HikeConstants.Extras.RECORDING_TIME))
			{
				recordingDuration = intent.getLongExtra(HikeConstants.Extras.RECORDING_TIME, -1);
				isRecording = true;
				fileType = HikeConstants.VOICE_MESSAGE_CONTENT_TYPE;
			}
			HikeFileType hikeFileType = HikeFileType.fromString(fileType, isRecording);

			if (filePath == null)
			{
				Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
			}
			else
			{
				SenderConsignment fileConsignment = hikeConverter.getFileConsignment(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType, msisdn, null);
				offlineManager.sendConsignment(fileConsignment);
			}

		}
		
		
	}

	public void sendApps(String filePath, String mime, String apkLabel, String msisdn)
	{
		SenderConsignment appConsignment = hikeConverter.getFileConsignment(filePath, null, HikeFileType.APK, mime, false, (long) -1, FTAnalyticEvents.APK_ATTACHMENT, msisdn, apkLabel);
		offlineManager.sendConsignment(appConsignment);
	}

	public boolean isConnected()
	{
		return false;
	}

	public void shutDown()
	{
		SenderConsignment disconnectConsignment = hikeConverter.getDisconnectConsignment(getConnectedDevice());
		Logger.d(TAG, "Going to send disconnect packet");
		offlineManager.sendConsignment(disconnectConsignment);
		offlineManager.disconnectAfterTimeout();
	}

	public void sendAudio(String filePath, String msisdn)
	{
		SenderConsignment audioConsignment = hikeConverter.getFileConsignment(filePath, null, HikeFileType.AUDIO, null, false, -1, FTAnalyticEvents.AUDIO_ATTACHEMENT, msisdn, null);
		offlineManager.sendConsignment(audioConsignment);
	}

	public void sendVideo(String filePath, String msisdn)
	{
		SenderConsignment videoConsignment = hikeConverter.getFileConsignment(filePath, null, HikeFileType.VIDEO, null, false, -1, FTAnalyticEvents.VIDEO_ATTACHEMENT, msisdn, null);
		offlineManager.sendConsignment(videoConsignment);
	}

	public void sendImage(String imagePath, String msisdn,int attachementType)
	{
		SenderConsignment imageConsignment = hikeConverter.getFileConsignment(imagePath, null, HikeFileType.IMAGE, null, false, -1, FTAnalyticEvents.CAMERA_ATTACHEMENT, msisdn,null);
		offlineManager.sendConsignment(imageConsignment);
    }
	
	public void createHotspot(String msisdn)
	{
		offlineManager.createHotspot(msisdn);
	}
	
	public void connectToHotspot(String msisdn)
	{
		offlineManager.connectToHotspot(msisdn);	
	}
	
	public void connectAsPerMsisdn(String msisdn)
	{
		offlineManager.connectAsPerMsisdn(msisdn);
	}
	
	public OFFLINE_STATE getOfflineState()
	{
		return this.offlineState;
	}
	
	public void setOfflineState(OFFLINE_STATE offlineState)
	{
		this.offlineState = offlineState;
		Logger.d("OfflineManager", "Offline state is " + offlineState);
	}

	public void removeListener(IOfflineCallbacks listener)
	{
		offlineManager.removeListener(listener);
	}

	public void sendMultiMessages(ArrayList<ConvMessage> offlineMessageList,String msisdn) {
		for(ConvMessage convMessage: offlineMessageList)
		{
			convMessage.setMsisdn(msisdn);
			sendMessage(convMessage);
		}
	}

	public void sendfile(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration, int attachmentType,
			String msisdn, String apkLabel)
	{
		SenderConsignment fileConsignment = hikeConverter.getFileConsignment(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType, msisdn,
				apkLabel);
		offlineManager.sendConsignment(fileConsignment);
	}
	
	public void onDisconnect(TException e)
	{
		shutdownProcess(e);
	}
	
	public void shutdown(TException exception)
	{
		Logger.d(TAG, "ShutDown called Due to reason " + exception.getReasonCode());
		Message msg=Message.obtain();
		msg.what=HandlerConstants.SHUTDOWN;
		msg.obj=exception;
		mHandler.sendMessage(msg);
	}
	
	public FileSavedState getFileState(ConvMessage convMessage, File file) 
	{
		return convMessage.isSent() ? fileManager.getUploadFileState(convMessage, file) : fileManager.getDownloadFileState(convMessage, file);
	}

	public void handleRetryButton(ConvMessage convMessage) 
	{
		
		if (OfflineController.getInstance().getOfflineState() != OFFLINE_STATE.CONNECTED) 
		{
			HikeMessengerApp.getInstance().showToast("You are not connected..!! Kindly connect.");
			return;
		}
		HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
		Logger.d(TAG, "Hike File type is: "
				+ hikeFile.getHikeFileType().ordinal());

		File selected = hikeFile.getFile();
		File sourceFile=new File (hikeFile.getSourceFilePath());
		if (selected.exists()) 
		{
			//TODO: Check if true is really required here
			SenderConsignment fileConsignment = hikeConverter.getFileConsignment(convMessage, false);
			offlineManager.sendConsignment(fileConsignment);
		}
		else if(sourceFile.exists())
		{
			
			try {
				(convMessage.serialize().getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA).getJSONArray(HikeConstants.FILES))
				.getJSONObject(0).putOpt(HikeConstants.FILE_PATH, hikeFile.getSourceFilePath());
//				File x=HikeMessengerApp.getInstance().getApplicationContext().getExternalFilesDir("hiketmp");
//				File bin = new File(x,selected.getName() + ".bin." + convMessage.getMsgID());
//				if(bin!=null&&bin.exists())
//				{
//					bin.delete();
//				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			SenderConsignment fileConsignment = hikeConverter.getFileConsignment(convMessage, true);
			offlineManager.sendConsignment(fileConsignment);
		}
		else 
		{
			
			HikeMessengerApp.getInstance().showToast("File not found.!!");
		}
	}
	
	protected boolean isHotspotCreated()
	{
		return offlineManager.isHotspotCreated();
	}
	
	public long getTransferProgress(long msgId, boolean isSent, long fileSize)
	{
		long num = fileManager.getTransferProgress(msgId, isSent);
		// not present in files map
		if (num == -1)
		{
			return num;
		}
		long progress = (((long) num * OfflineConstants.CHUNK_SIZE * 100) / fileSize);
		Logger.d(TAG, "CurrentSizeReceived: " + num + " FileSize: "+ fileSize + 
				" Progress -> " + progress + "  msgId  --->" + msgId);
		return progress;
	}
	
	public synchronized void shutdownProcess(TException exception)
	{
		if (getOfflineState() != OFFLINE_STATE.DISCONNECTED)
		{
			// this function uses offline state == connected.
			// so changing OfflineState after calling this.
			setOfflineState(OFFLINE_STATE.DISCONNECTED);
			Transporter.getInstance().shutDown();
			fileManager.shutDown();
			sendDisconnectToListeners();

			
			Logger.d(TAG, "going to disconnect");
			HikeMessengerApp.getInstance().showToast("Disconnected Reason " + exception.getReasonCode());

			hikeConverter.releaseResources();

			offlineManager.releaseResources();
			// if a sending file didn't go change from spinner to retry button
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		}
	}
	
	private void sendDisconnectToListeners()
	{
		sendDisconnectInlineMsg(getConnectedDevice());

		offlineManager.updateListeners(getConnectedDevice(), getConnectingDevice());
	}
	
	public void deleteRemainingFiles(ArrayList<Long> msgId,String msisdn)
	{
		fileManager.deleteFiles(msgId, msisdn);
	}
	
	public void sendDisconnectInlineMsg(String msisdn)
	{
		if(TextUtils.isEmpty(msisdn))
			return ;
		final ConvMessage convMessage = OfflineUtils.createOfflineInlineConvMessage(msisdn, HikeMessengerApp.getInstance().getApplicationContext().getString(R.string.connection_deestablished),
				OfflineConstants.OFFLINE_MESSAGE_DISCONNECTED_TYPE);
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
	}

	public void onConnect() 
	{
		Logger.d(TAG, "In onConnect");
		
		offlineManager.setConnectingDeviceAsConnected();
		Logger.d(TAG,"Connected Device is "+ offlineManager.getConnectedDevice());
		HikeSharedPreferenceUtil.getInstance().saveData(OfflineConstants.OFFLINE_MSISDN, offlineManager.getConnectedDevice());
		offlineManager.removeMessage(OfflineConstants.HandlerConstants.REMOVE_CONNECT_MESSAGE);
		offlineManager.removeMessage(OfflineConstants.HandlerConstants.CONNECT_TO_HOTSPOT);
		offlineManager.removeMessage(OfflineConstants.HandlerConstants.RECONNECT_TO_HOTSPOT);
		OfflineController.getInstance().setOfflineState(OFFLINE_STATE.CONNECTED);
		final ConvMessage convMessage = OfflineUtils.createOfflineInlineConvMessage(offlineManager.getConnectedDevice(), 
						HikeMessengerApp.getInstance().getApplicationContext().getString(R.string.connection_established), OfflineConstants.OFFLINE_MESSAGE_CONNECTED_TYPE);
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage, true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		offlineManager.sendConnectedCallback();
		offlineManager.sendInfoPacket();
		
	}

	public SenderConsignment getSenderConsignment(ConvMessage convMessage, boolean persistence)
	{
		SenderConsignment senderConsignment = null;
		if (convMessage.isFileTransferMessage())
		{
			senderConsignment = hikeConverter.getFileConsignment(convMessage, persistence);
		}
		else
		{
			senderConsignment = hikeConverter.getMessageConsignment(convMessage, persistence);
		}
			
		return senderConsignment;
	}

	public String getConnectingDevice() 
	{
		return offlineManager.getConnectingDevice();
	}	

}
