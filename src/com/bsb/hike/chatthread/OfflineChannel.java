package com.bsb.hike.chatthread;

import java.util.ArrayList;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.models.ConvMessage;
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
		offlineController.sendfile(filePath , fileKey, hikeFileType, fileType, isRecording, recordingDuration, attachmentType, msisdn, null);
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
		HikeFileType hikeFileType = HikeFileType.fromString(fileType,false);	
		offlineController.sendfile(filePath, null, hikeFileType, fileType,false,-1, FTAnalyticEvents.OTHER_ATTACHEMENT, msisdn, null);
	}

	@Override
	public void uploadFile(Context applicationContext, String msisdn, Uri uri,
			HikeFileType image, boolean onHike) {
		offlineController.sendImage(Utils.getRealPathFromUri(uri,applicationContext), msisdn);
	}

	@Override
	public void uploadFile(Context applicationContext, String msisdn,
			String imagePath, HikeFileType imageType, boolean isOnHike,
			int cameraAttachement) {
		offlineController.sendImage(imagePath, msisdn);
	}

	@Override
	public void sendApps(String filePath, String mime, String apkLabel,
			String msisdn) {
		
		offlineController.sendApps(filePath, mime, apkLabel, msisdn);
	}

}
