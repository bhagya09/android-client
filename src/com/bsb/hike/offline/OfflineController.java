package com.bsb.hike.offline;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Message;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.ui.ComposeChatActivity.FileTransferData;
import com.hike.transporter.models.SenderConsignment;
import com.bsb.hike.utils.Logger;

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

	private IOfflineCallbacks offlineListener = null;

	private OfflineManager offlineManager;
	
	private HikeConverter  converter;

	public static volatile OfflineController _instance=null;
	
	private OfflineFileManager fileManager;
	
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

	public OfflineController()
	{
		fileManager = new OfflineFileManager();
		converter = new HikeConverter();
		offlineManager = new OfflineManager();
		
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
		offlineManager.performWorkOnBackEndThread(msg);
		//offlineManager.addToTextQueue(convMessage.serialize());
	}

	public void sendAudioFile(String filePath, long duration, String msisdn)
	{
		converter.sendFile(filePath, null, HikeFileType.AUDIO_RECORDING, HikeConstants.VOICE_MESSAGE_CONTENT_TYPE, true, duration,
				FTAnalyticEvents.AUDIO_ATTACHEMENT, msisdn, null);
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
			
			converter.sendFile(fileData.filePath, fileData.fileKey, fileData.hikeFileType, fileData.fileType, fileData.isRecording,  
															fileData.recordingDuration, FTAnalyticEvents.OTHER_ATTACHEMENT, msisdn, apkLabel);
		}
	}

	public void sendFile(Intent intent ,JSONObject msgExtrasJson, String msisdn)
	{
		String fileKey = null;
		try{
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
			converter.sendFile(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType, msisdn, null);
		}
	 }catch(JSONException e){
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
					converter.sendFile(filePath, null, hikeFileType, fileType, false, -1,FTAnalyticEvents.OTHER_ATTACHEMENT, msisdn, null);
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
				converter.sendFile(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType, msisdn, null);
			}

		}
		
		
	}

	public void sendApps(String filePath, String mime, String apkLabel, String msisdn)
	{
		converter.sendFile(filePath, null, HikeFileType.APK, mime, false, (long) -1, FTAnalyticEvents.APK_ATTACHMENT, msisdn, apkLabel);
	}

	public boolean isConnected()
	{
		return false;
	}

	public void shutDown()
	{
		converter.sendDisconnectMessage();		
		offlineManager.disconnectAfterTimeout();
	}

	public void sendAudio(String filePath, String msisdn)
	{
		converter.sendFile(filePath, null, HikeFileType.AUDIO, null, false, -1, FTAnalyticEvents.AUDIO_ATTACHEMENT, msisdn, null);
	}

	public void sendVideo(String filePath, String msisdn)
	{
		converter.sendFile(filePath, null, HikeFileType.VIDEO, null, false, -1, FTAnalyticEvents.VIDEO_ATTACHEMENT, msisdn, null);
	}

	public void sendImage(String imagePath, String msisdn)
	{
		converter.sendFile(imagePath, null, HikeFileType.IMAGE, null, false, -1, FTAnalyticEvents.CAMERA_ATTACHEMENT, msisdn,null);
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
		return offlineManager.getOfflineState();
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
}
