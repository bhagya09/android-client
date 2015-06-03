package com.bsb.hike.offline;

import android.content.Intent;
import android.os.Message;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;

/**
 * 
 * @author himanshu
 *
 *	This class acts as a abstraction for Offline Messaging.The User should use this class to interact with the
 *	Offline Manager
 */

public class OfflineController
{

	private IOfflineCallbacks offlineListener = null;

	OfflineManager offlineManager;

	public OfflineController(IOfflineCallbacks listener)
	{
		this.offlineListener = listener;
		offlineManager = OfflineManager.getInstance();
	}

	public void startScanningWifiDirect()
	{

	}

	public void stopScanningWifiDirect()
	{

	}

	public String getConnectedDevice()
	{
		return null;
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
		offlineManager.addToTextQueue(convMessage.serialize());
	}

	public void sendAudioFile(String filePath, long duration, String msisdn)
	{
		offlineManager.initialiseOfflineFileTransfer(filePath, null, HikeFileType.AUDIO_RECORDING, HikeConstants.VOICE_MESSAGE_CONTENT_TYPE, true, duration,
				FTAnalyticEvents.AUDIO_ATTACHEMENT, msisdn, null);
	}

	public void sendFile(Intent intent, String msisdn)
	{
		String fileKey = null;

		if (intent.hasExtra(HikeConstants.Extras.FILE_KEY))
		{
			fileKey = intent.getStringExtra(HikeConstants.Extras.FILE_KEY);
		}
		String filePath = intent.getStringExtra(HikeConstants.Extras.FILE_PATH);
		String fileType = intent.getStringExtra(HikeConstants.Extras.FILE_TYPE);

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
			offlineManager.initialiseOfflineFileTransfer(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType, msisdn, null);
		}
	}

	public void sendApps(String filePath, String mime, String apkLabel, String msisdn)
	{
		offlineManager.initialiseOfflineFileTransfer(filePath, null, HikeFileType.APK, mime, false, (long) -1, FTAnalyticEvents.APK_ATTACHMENT, msisdn, apkLabel);
	}

	public boolean isConnected()
	{
		return false;
	}

	public void shutDown()
	{
		offlineManager.shutDown();
	}

	public void sendAudio(String filePath, String msisdn)
	{
		offlineManager.initialiseOfflineFileTransfer(filePath, null, HikeFileType.AUDIO, null, false, -1, FTAnalyticEvents.AUDIO_ATTACHEMENT, msisdn, null);
	}

	public void sendVideo(String filePath, String msisdn)
	{
		offlineManager.initialiseOfflineFileTransfer(filePath, null, HikeFileType.VIDEO, null, false, -1, FTAnalyticEvents.VIDEO_ATTACHEMENT, msisdn, null);
	}

	public void sendImage(String imagePath, String msisdn)
	{
		offlineManager.initialiseOfflineFileTransfer(imagePath, null, HikeFileType.IMAGE, null, false, -1, FTAnalyticEvents.CAMERA_ATTACHEMENT, msisdn,null);
	}
	
	public void createHotspot(String msisdn)
	{
	}
	
	public void connectToHotspot(String msisdn)
	{
	}
	
	public void connectAsPerMsisdn(String msisdn)
	{
		
	}

}
