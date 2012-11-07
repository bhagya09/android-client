package com.bsb.hike.tasks;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.FileTransferTaskBase;
import com.bsb.hike.utils.Utils;
import com.google.android.maps.GeoPoint;

public class UploadLocationTask extends FileTransferTaskBase
{
	private static final String STATIC_MAP_UNFORMATTED_URL = 
			"http://maps.googleapis.com/maps/api/staticmap?center=%1$f,%2$f&zoom=%3$d&size=%4$dx%4$d&markers=size:mid|color:red|%1$f,%2$f&sensor=false"; 

	private String msisdn;
	private double latitude;
	private double longitude;
	private int zoomLevel;
	private String address;
	private ConvMessage convMessage;
	private String fileKey;
	private Context context;
	
	public UploadLocationTask(String msisdn, double latitude, double longitude, int zoomLevel, String fileKey, Context context) 
	{
		this.latitude = latitude;
		this.longitude = longitude;
		this.zoomLevel = zoomLevel;
		this.fileKey = fileKey;
		this.msisdn = msisdn;
		this.context = context;
	}

	public UploadLocationTask(ConvMessage convMessage, Context context)
	{
		this.convMessage = convMessage;
		this.context = context;
	}

	@Override
	protected FTResult doInBackground(Void... params) 
	{
		this.cancelTask = new AtomicBoolean(false);
		try
		{
			if(convMessage == null)
			{
				JSONObject metadata = getFileTransferMetadata(latitude, longitude, zoomLevel, null, null);

				convMessage = createConvMessage(msisdn, metadata);

				if(TextUtils.isEmpty(fileKey))
				{
					// Called so that the UI in the Conversation lists screen is updated
					HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);
				}

				address = Utils.getAddressFromGeoPoint(new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6)), context);

				fetchThumbnailAndUpdateConvMessage(latitude, longitude, zoomLevel, address, convMessage);
			}
			else
			{
				HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
				latitude = hikeFile.getLatitude();
				longitude = hikeFile.getLongitude();
				address = hikeFile.getAddress();

				if(TextUtils.isEmpty(hikeFile.getThumbnailString()))
				{
					fetchThumbnailAndUpdateConvMessage(latitude, longitude, zoomLevel, address, convMessage);
				}
			}
			boolean fileWasAlreadyUploaded = true;
			// If we don't have a file key, that means we haven't uploaded the file to the server yet
			if(TextUtils.isEmpty(fileKey))
			{
				fileWasAlreadyUploaded = false;

				JSONObject response = AccountUtils.executeFileTransferRequest(
						null, HikeConstants.LOCATION_FILE_NAME, convMessage.getMetadata().getJSON(), this, cancelTask, HikeConstants.LOCATION_CONTENT_TYPE);
				
				JSONObject fileJSON = response.getJSONObject("data");
				fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
			}

			JSONObject metadata = new JSONObject();
			JSONArray filesArray = new JSONArray();

			HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			hikeFile.setFileKey(fileKey);
			hikeFile.setFileTypeString(HikeConstants.LOCATION_CONTENT_TYPE);

			filesArray.put(hikeFile.serialize());
			metadata.put(HikeConstants.FILES, filesArray);

			convMessage.setMetadata(metadata);

			// If the file was just uploaded to the servers, we want to publish this event
			if(!fileWasAlreadyUploaded)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, convMessage);
			}

			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);
		}
		catch (Exception e)
		{
			Log.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.UPLOAD_FAILED;
		}
		return FTResult.SUCCESS;
	}

	private JSONObject getFileTransferMetadata(double latitude, double longitude, int zoomLevel, String address, String thumbnailString) throws JSONException
	{
		JSONArray files = new JSONArray();
		files.put(new HikeFile(latitude, longitude, zoomLevel, address, thumbnailString, null).serialize());
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);

		return metadata;
	}

	private ConvMessage createConvMessage(String msisdn, JSONObject metadata) throws JSONException
	{
		long time = System.currentTimeMillis()/1000;
		ConvMessage convMessage = new ConvMessage(HikeConstants.LOCATION_FILE_NAME, msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setMetadata(metadata);
		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage);

		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_MESSAGE_CREATED, convMessage);

		ChatThread.fileTransferTaskMap.put(convMessage.getMsgID(), this);
		return convMessage;
	}

	private void fetchThumbnailAndUpdateConvMessage(double latitude, double longitude, int zoomLevel, String address, ConvMessage convMessage) throws Exception
	{
		String staticMapUrl = String.format(STATIC_MAP_UNFORMATTED_URL, latitude, longitude, zoomLevel, HikeConstants.MAX_DIMENSION_THUMBNAIL_PX);
		Log.d(getClass().getSimpleName(), "Static map url: " + staticMapUrl);

		Bitmap thumbnail =  BitmapFactory.decodeStream((InputStream) new URL(staticMapUrl).getContent());
		String thumbnailString = Base64.encodeToString(Utils.bitmapToBytes(thumbnail, Bitmap.CompressFormat.JPEG), Base64.DEFAULT);

		JSONObject metadata = getFileTransferMetadata(latitude, longitude, zoomLevel, address, thumbnailString);

		convMessage.setMetadata(metadata);
		HikeConversationsDatabase.getInstance().updateMessageMetadata(convMessage.getMsgID(), convMessage.getMetadata());
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	@Override
	protected void onPostExecute(FTResult result) {
		if(convMessage != null)
		{
			ChatThread.fileTransferTaskMap.remove(convMessage.getMsgID());
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		}

		if(result != FTResult.SUCCESS)
		{
			int errorStringId = 0;
			if(result == FTResult.UPLOAD_FAILED)
			{
				errorStringId = R.string.upload_failed;
			}
			Toast.makeText(context, errorStringId, Toast.LENGTH_SHORT).show();
		}
	}

	
}
