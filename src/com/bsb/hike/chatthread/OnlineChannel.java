package com.bsb.hike.chatthread;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.MsgRelLogManager;
import com.bsb.hike.analytics.AnalyticsConstants.MessageType;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.media.AttachmentPicker;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.service.HikeMqttManagerNew;

public class OnlineChannel implements IChannelSelector{

	@Override
	public void sendMessage(ConvMessage convMessage)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);
	}

	@Override
	public void sendAudioRecording(Context applicationContext, String filePath,
			long duration, String msisdn, boolean isOnHike)
	{
		ChatThreadUtils.initialiseFileTransfer(applicationContext, msisdn, filePath, null, HikeFileType.AUDIO_RECORDING, HikeConstants.VOICE_MESSAGE_CONTENT_TYPE,
				true, duration, false, isOnHike, FTAnalyticEvents.AUDIO_ATTACHEMENT);
	}

	@Override
	public void sendAudio(Context applicationContext, String msisdn,
			String filePath, boolean onHike) 
	{	
		ChatThreadUtils.uploadFile(applicationContext, msisdn, filePath, HikeFileType.AUDIO, onHike, FTAnalyticEvents.AUDIO_ATTACHEMENT);
	}

	@Override
	public void sendVideo(Context applicationContext, String msisdn,
			String filePath, boolean onHike)
	{
		ChatThreadUtils.uploadFile(applicationContext, msisdn, filePath, HikeFileType.VIDEO,onHike, FTAnalyticEvents.VIDEO_ATTACHEMENT);
	}

	@Override
	public void startMessageRelLogging(ConvMessage convMessage, String text) {
		MsgRelLogManager.startMessageRelLogging(convMessage, MessageType.TEXT);
	}

	@Override
	public void onShareFile(Context applicationContext, String msisdn,
			Intent intent, boolean onHike) {
		ChatThreadUtils.onShareFile(applicationContext, msisdn, intent, onHike);
	}

	@Override
	public void sendFile(Context applicationContext, String msisdn,
			String filePath, String fileKey, HikeFileType hikeFileType,
			String fileType, boolean isRecording, long recordingDuration,
			boolean isForwardingFile, boolean isOnHike, int attachmentType) {
		
		ChatThreadUtils.initialiseFileTransfer(applicationContext, msisdn, filePath, fileKey, hikeFileType, fileType, isRecording,
				recordingDuration, true, isOnHike, attachmentType);
		
	}

	@Override
	public void sendFile(Context applicationContext, String msisdn,
						 String filePath, String fileKey, HikeFileType hikeFileType,
						 String fileType, boolean isRecording, long recordingDuration,
						 boolean isForwardingFile, boolean isOnHike, int attachmentType, String caption) {

		ChatThreadUtils.initialiseFileTransfer(applicationContext, msisdn, filePath, fileKey, hikeFileType, fileType, isRecording,
				recordingDuration, true,isOnHike, attachmentType, caption);

	}

	@Override
	public void sendPicasaUriFile(Context applicationContext, Uri uri,
			HikeFileType hikeFileType, String msisdn, boolean isOnHike) {
	}

	@Override
	public void initialiaseLocationTransfer(Context applicationContext,
			String msisdn, double latitude, double longitude, int zoomLevel,
			boolean isOnHike, boolean newConvIfnotExist) {
		ChatThreadUtils.initialiseLocationTransfer(applicationContext, msisdn, latitude, longitude, zoomLevel,isOnHike,true);
	}

	@Override
	public void initialiseContactTransfer(Context applicationContext,
			String msisdn, JSONObject contactJson, boolean isOnHike) {
		ChatThreadUtils.initialiseContactTransfer(applicationContext, msisdn, contactJson, isOnHike);
	}

	@Override
	public void initiateFileTransferFromIntentData(Context applicationContext,
			String msisdn, String fileType, String filePath, boolean isOnHike,
			int otherAttachement) {
		ChatThreadUtils.initiateFileTransferFromIntentData(applicationContext, msisdn, fileType, filePath, isOnHike, FTAnalyticEvents.OTHER_ATTACHEMENT);
	}

	@Override
	public void uploadFile(Context applicationContext, String msisdn, Uri uri,
			HikeFileType image, boolean isOnHike) {
	}

	@Override
	public void uploadFile(Context applicationContext, String msisdn,
			String imagePath, HikeFileType imageType, boolean isOnHike,
			int cameraAttachement) {
		uploadFile(applicationContext, msisdn, imagePath, imageType, isOnHike, cameraAttachement,null);
	}

	@Override
	public void uploadFile(Context applicationContext, String msisdn,
						   String imagePath, HikeFileType imageType, boolean isOnHike,
						   int cameraAttachement, String caption) {
		ChatThreadUtils.uploadFile(applicationContext, msisdn, imagePath, HikeFileType.IMAGE,isOnHike, FTAnalyticEvents.CAMERA_ATTACHEMENT,caption);
	}


	@Override
	public void modifyAttachmentPicker(ChatThreadActivity activity,
			AttachmentPicker attachmentPicker, boolean addContact) {
		if (addContact)
		{
			attachmentPicker.appendItem(new OverFlowMenuItem(activity.getString(R.string.contact_msg_sent), 0, R.drawable.ic_attach_contact, AttachmentPicker.CONTACT));
		}
	}

	@Override
	public void postMR(JSONObject object) {
		HikeMqttManagerNew.getInstance().sendMessage(object, MqttConstants.MQTT_QOS_ONE);	
	}

	@Override
	public void sendApps(Context applicationContext, String filePath, String mime, String apkLabel, String msisdn, boolean isOnHike)
	{
		ChatThreadUtils.initialiseFileTransfer(applicationContext, msisdn, filePath, null, 
				HikeFileType.OTHER,mime,false, -1, false,isOnHike,FTAnalyticEvents.OTHER_ATTACHEMENT);
	}

}
