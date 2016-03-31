package com.bsb.hike.chatthread;

import java.util.ArrayList;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;
import android.widget.PopupWindow.OnDismissListener;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.media.AttachmentPicker;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.media.OverflowItemClickListener;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.offline.IOfflineCallbacks;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.utils.Utils;

public class OfflineChannel implements IChannelSelector{

	private OfflineController offlineController;
	
	public OfflineChannel(OfflineController offlineController) 
	{
		this.offlineController = offlineController;
	}

	@Override
	public void sendMessage(ConvMessage convMessage) 
	{
		convMessage.setMessageOriginType(OriginType.OFFLINE);
		offlineController.sendMessage(convMessage);
	}

	@Override
	public void sendAudioRecording(Context applicationContext, String filePath,
			long duration, String msisdn, boolean onHike) 
	{
		offlineController.sendAudioFile(filePath, duration, msisdn);
	}

	@Override
	public void sendAudio(Context applicationContext, String msisdn,
			String filePath, boolean onHike) 
	{
		offlineController.sendAudio(filePath, msisdn);
	}

	@Override
	public void sendVideo(Context applicationContext, String msisdn,
			String filePath, boolean onHike) {
		offlineController.sendVideo(filePath, msisdn);
	}

	@Override
	public void startMessageRelLogging(ConvMessage convMessage, String text) {
		//Todo Add code for logging 
	}

	@Override
	public void onShareFile(Context applicationContext, String msisdn,
			Intent intent, boolean onHike) {
		offlineController.sendFile(intent, msisdn);
	}

	@Override
	public void sendFile(Context applicationContext, String msisdn,
			String filePath, String fileKey, HikeFileType hikeFileType,
			String fileType, boolean isRecording, long recordingDuration,
			boolean isForwardingFile, boolean isOnHike, int attachmentType) {
		//To do - Check  for  apk label  
		offlineController.sendfile(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType, msisdn, null,null);
	}

	@Override
	public void sendFile(Context applicationContext, String msisdn,
						 String filePath, String fileKey, HikeFileType hikeFileType,
						 String fileType, boolean isRecording, long recordingDuration,
						 boolean isForwardingFile, boolean isOnHike, int attachmentType, String caption) {
		//To do - Check  for  apk label
		offlineController.sendfile(filePath , fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType, msisdn, null,caption);
	}

	@Override
	public void sendPicasaUriFile(Context applicationContext, Uri parse,
			HikeFileType hikeFileType, String msisdn, boolean onHike) {
		//To do - Deal with picasa in Offline 
	}

	@Override
	public void initialiaseLocationTransfer(Context applicationContext,
			String msisdn, double latitude, double longitude, int zoomLevel,
			boolean isOnHike, boolean newConvIfnotExist) {
		// To do - Deal with location transfer in future
	}

	@Override
	public void initialiseContactTransfer(Context applicationContext,
			String msisdn, JSONObject contactJson, boolean isOnHike) {
		
		ConvMessage offlineConvMessage = OfflineUtils.createOfflineContactConvMessage(msisdn,contactJson,isOnHike);
		sendMessage(offlineConvMessage);
	}

	@Override
	public void initiateFileTransferFromIntentData(Context applicationContext,
			String msisdn, String fileType, String filePath, boolean isOnHike,
			int otherAttachement) {
		//To do - Not dealing with picasa uri
		HikeFileType hikeFileType = HikeFileType.fromString(fileType, false);
		offlineController.sendfile(filePath, null, hikeFileType, fileType, false, -1, FTAnalyticEvents.OTHER_ATTACHEMENT, msisdn, null,null);
	}

	@Override
	public void uploadFile(Context applicationContext, String msisdn, Uri uri,
			HikeFileType image, boolean onHike) {
		offlineController.sendImage(Utils.getRealPathFromUri(uri, applicationContext), msisdn, FTAnalyticEvents.OTHER_ATTACHEMENT,null);
	}

	@Override
	public void uploadFile(Context applicationContext, String msisdn,
			String imagePath, HikeFileType imageType, boolean isOnHike,
			int cameraAttachement) {
		offlineController.sendImage(imagePath, msisdn,cameraAttachement,null);
	}

	@Override
	public void uploadFile(Context applicationContext, String msisdn,
						   String imagePath, HikeFileType imageType, boolean isOnHike,
						   int cameraAttachement,String caption) {
		offlineController.sendImage(imagePath, msisdn,cameraAttachement,caption);
	}

	@Override
	public void modifyAttachmentPicker(ChatThreadActivity activity,
			AttachmentPicker attachmentPicker, boolean addContact) {
		if (addContact)
		{
			attachmentPicker.appendItem(new OverFlowMenuItem(activity.getResources().getString(R.string.contact_msg_sent), 0, R.drawable.ic_attach_contact, AttachmentPicker.CONTACT));
		}
		attachmentPicker.removeItem(AttachmentPicker.LOCATION);
		attachmentPicker.appendItem(new OverFlowMenuItem(activity.getResources().getString(R.string.apps), 0, R.drawable.ic_attach_apk, AttachmentPicker.APPS));
		//Changing text for file info in offline mode
		((TextView)attachmentPicker.getView().findViewById(R.id.group_info)).setText(activity.getResources().getString(R.string.file_attachment_info_offline));
	}

	@Override
	public void postMR(JSONObject object) {
		
		OfflineController.getInstance().sendMR(object);
	}

	@Override
	public void sendApps(Context applicationContext, String filePath, String mime, String apkLabel, String msisdn, boolean isOnHike)
	{
		offlineController.sendApps(filePath, mime, apkLabel, msisdn);
	}


}
