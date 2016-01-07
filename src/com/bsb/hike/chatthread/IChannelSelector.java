package com.bsb.hike.chatthread;

import java.util.ArrayList;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.PopupWindow.OnDismissListener;

import com.bsb.hike.media.AttachmentPicker;
import com.bsb.hike.media.OverflowItemClickListener;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;

public interface IChannelSelector {

	void sendMessage(ConvMessage convMessage);

	void sendAudioRecording(Context applicationContext, String filePath,
			long duration, String msisdn, boolean onHike);

	void sendAudio(Context applicationContext, String msisdn, String filePath,
			boolean onHike);

	void sendVideo(Context applicationContext, String msisdn, String filePath,
			boolean onHike);

	void startMessageRelLogging(ConvMessage convMessage, String text);

	void onShareFile(Context applicationContext, String msisdn, Intent intent,
			boolean onHike);

	void sendFile(Context applicationContext, String msisdn, String filePath,
			String fileKey, HikeFileType hikeFileType, String fileType,
			boolean isRecording, long recordingDuration, boolean isForwardingFile,
			boolean onHike, int attachmentType);

	void sendFile(Context applicationContext, String msisdn, String filePath,
				  String fileKey, HikeFileType hikeFileType, String fileType,
				  boolean isRecording, long recordingDuration, boolean isForwardingFile,
				  boolean onHike, int attachmentType, String caption);

	void sendPicasaUriFile(Context applicationContext, Uri parse,
			HikeFileType hikeFileType, String msisdn, boolean onHike);

	void initialiaseLocationTransfer(Context applicationContext, String msisdn,
			double latitude, double longitude, int zoomLevel, boolean isOnHike,
			boolean newConvIfnotExist);

	void initialiseContactTransfer(Context applicationContext, String msisdn,
			JSONObject contactJson, boolean onHike);

	void initiateFileTransferFromIntentData(Context applicationContext,
			String msisdn, String fileType, String filePath, boolean isOnHike,
			int otherAttachement);

	void uploadFile(Context applicationContext, String msisdn, Uri uri,
			HikeFileType image, boolean isOnHike);

	void uploadFile(Context applicationContext, String msisdn,
			String imagePath, HikeFileType imageType, boolean isOnHike,
			int cameraAttachement);

	void uploadFile(Context applicationContext, String msisdn,
					String imagePath, HikeFileType imageType, boolean isOnHike,
					int cameraAttachement, String caption);

	void sendApps(Context applicationContext,String filePath, String mime, String apkLabel, String msisdn, boolean isOnHike);

	void modifyAttachmentPicker(ChatThreadActivity activity, AttachmentPicker attachmentPicker,
			boolean addContact);

	void postMR(JSONObject object);

}
