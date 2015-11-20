package com.bsb.hike.filetransfer;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.http.CustomByteArrayEntity;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.ProgressListener;
import com.bsb.hike.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

public class UploadContactOrLocationTask extends FileTransferBase
{
	private static final String STATIC_MAP_UNFORMATTED_URL = "http://maps.googleapis.com/maps/api/staticmap?center=%1$f,%2$f&zoom=%3$d&size=%4$dx%4$d&markers=size:mid|color:red|%1$f,%2$f&sensor=false";

	private double latitude;

	private double longitude;

	private int zoomLevel;

	private String address;

	private boolean uploadingContact;

	protected long maxSize = 100; // just to avoid divide by zero operation exception

	private FutureTask<FTResult> futureTask;

	protected UploadContactOrLocationTask(Handler handler, ConcurrentHashMap<Long, FutureTask<FTResult>> fileTaskMap, Context ctx, ConvMessage convMessage, boolean uploadingContact,
			String token, String uId)
	{
		super(handler, fileTaskMap, ctx, null, -1, null, token, uId);
		this.userContext = convMessage;
		this.uploadingContact = uploadingContact;
		_state = FTState.INITIALIZED;
	}

	protected void setFutureTask(FutureTask<FTResult> fuTask)
	{
		futureTask = fuTask;
		fileTaskMap.put(((ConvMessage) userContext).getMsgID(), futureTask);
	}

	@Override
	public FTResult call()
	{
		try
		{
			if (!uploadingContact)
			{
				HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
				latitude = hikeFile.getLatitude();
				longitude = hikeFile.getLongitude();
				zoomLevel = hikeFile.getZoomLevel();
				address = hikeFile.getAddress();

				if (address == null)
					address = Utils.getAddressFromGeoPoint(new LatLng(latitude, longitude), context);

				if (TextUtils.isEmpty(hikeFile.getThumbnailString()))
				{
					fetchThumbnailAndUpdateConvMessage(latitude, longitude, zoomLevel, address, (ConvMessage) userContext);
				}
			}

			if (!fileTaskMap.containsKey(((ConvMessage) userContext).getMsgID()))
				fileTaskMap.put(((ConvMessage) userContext).getMsgID(), futureTask);

			boolean fileWasAlreadyUploaded = true;
			// If we don't have a file key, that means we haven't uploaded the
			// file to the server yet
			if (TextUtils.isEmpty(fileKey))
			{
				fileWasAlreadyUploaded = false;

				JSONObject response = executeFileTransferRequest(null, uploadingContact ? HikeConstants.CONTACT_FILE_NAME : HikeConstants.LOCATION_FILE_NAME,
						((ConvMessage) userContext).getMetadata().getJSON(), uploadingContact ? HikeConstants.CONTACT_CONTENT_TYPE : HikeConstants.LOCATION_CONTENT_TYPE);

				JSONObject fileJSON = response.getJSONObject("data");
				fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
				fileSize = fileJSON.optInt(HikeConstants.FILE_SIZE);
			}

			JSONObject metadata = new JSONObject();
			JSONArray filesArray = new JSONArray();

			HikeFile hikeFile = ((ConvMessage) userContext).getMetadata().getHikeFiles().get(0);
			hikeFile.setFileKey(fileKey);
			hikeFile.setFileSize(fileSize);
			hikeFile.setFileTypeString(uploadingContact ? HikeConstants.CONTACT_CONTENT_TYPE : HikeConstants.LOCATION_CONTENT_TYPE);

			filesArray.put(hikeFile.serialize());
			Logger.d(getClass().getSimpleName(), "JSON FINAL: " + hikeFile.serialize());
			metadata.put(HikeConstants.FILES, filesArray);

			ConvMessage convMessageObject = (ConvMessage) userContext;
			convMessageObject.setMetadata(metadata);

			// If the file was just uploaded to the servers, we want to publish
			// this event
			if (!fileWasAlreadyUploaded)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPLOAD_FINISHED, convMessageObject);
			}
			
			if(convMessageObject.isBroadcastConversation())
			{
				List<PairModified<GroupParticipant, String>> participantList= ContactManager.getInstance().getGroupParticipants(convMessageObject.getMsisdn(), false, false);
				for (PairModified<GroupParticipant, String> grpParticipant : participantList)
				{
					String msisdn = grpParticipant.getFirst().getContactInfo().getMsisdn();
					convMessageObject.addToSentToMsisdnsList(msisdn);
				}
				OneToNConversationUtils.addBroadcastRecipientConversations(convMessageObject);
			}

			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessageObject);
			
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Exception", e);
			return FTResult.UPLOAD_FAILED;
		}
		return FTResult.SUCCESS;
	}

	private JSONObject executeFileTransferRequest(String filePath, String fileName, JSONObject request, String fileType) throws Exception
	{


		HttpContext httpContext = new BasicHttpContext();

		HttpPut httpPut = new HttpPut(AccountUtils.fileTransferBase + "/user/ft");

		AccountUtils.addToken(httpPut);
		httpPut.addHeader("Connection", "Keep-Alive");
		httpPut.addHeader("Content-Name", fileName);
		Logger.d("Upload", "Content type: " + fileType);
		httpPut.addHeader("Content-Type", TextUtils.isEmpty(fileType) ? "" : fileType);
		httpPut.addHeader("X-Thumbnail-Required", "0");
		AccountUtils.setNoTransform(httpPut);
		final AbstractHttpEntity entity;
		entity = new CustomByteArrayEntity(request.toString().getBytes(), new ProgressListener()
		{
			@Override
			public void transferred(long num)
			{
				incrementBytesTransferred(num);
				progressPercentage = (int) ((num / (float) maxSize) * 100);
			}
		});
		maxSize = entity.getContentLength();

		httpPut.setEntity(entity);
		HttpClient httpClient = AccountUtils.getClient(httpPut);
		if(httpClient==null){
			throw new NetworkErrorException("Unable to perform request");
		}
		HttpResponse response = httpClient.execute(httpPut, httpContext);
		String serverResponse = EntityUtils.toString(response.getEntity());

		JSONObject responseJSON = new JSONObject(serverResponse);
		if ((responseJSON == null) || (!"ok".equals(responseJSON.optString("stat"))))
		{
			throw new NetworkErrorException("Unable to perform request");
		}
		return responseJSON;
	}

	private JSONObject getFileTransferMetadataForLocation(double latitude, double longitude, int zoomLevel, String address, String thumbnailString) throws JSONException
	{
		JSONArray files = new JSONArray();
		files.put(new HikeFile(latitude, longitude, zoomLevel, address, thumbnailString, null, true).serialize());
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);

		return metadata;
	}

	private void fetchThumbnailAndUpdateConvMessage(double latitude, double longitude, int zoomLevel, String address, ConvMessage convMessage) throws Exception
	{
		String staticMapUrl = String.format(Locale.US, STATIC_MAP_UNFORMATTED_URL, latitude, longitude, zoomLevel, HikeConstants.MAX_DIMENSION_LOCATION_THUMBNAIL_PX);
		Logger.d(getClass().getSimpleName(), "Static map url: " + staticMapUrl);

		Bitmap thumbnail = HikeBitmapFactory.decodeStream((InputStream) new URL(staticMapUrl).getContent());
		String thumbnailString = Base64.encodeToString(BitmapUtils.bitmapToBytes(thumbnail, Bitmap.CompressFormat.JPEG), Base64.DEFAULT);
		if (thumbnail != null)
		{
			thumbnail.recycle();
		}
		JSONObject metadata = getFileTransferMetadataForLocation(latitude, longitude, zoomLevel, address, thumbnailString);

		convMessage.setMetadata(metadata);
		HikeConversationsDatabase.getInstance().updateMessageMetadata(convMessage.getMsgID(), convMessage.getMetadata());
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	protected void postExecute(FTResult result)
	{
		fileTaskMap.remove(((ConvMessage) userContext).getMsgID());
		Logger.d(getClass().getSimpleName(), "error display: removing" + ((ConvMessage) userContext).getMsgID());
		if (userContext != null)
		{
			// HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		}

		if (result != FTResult.SUCCESS)
		{
			if (result == FTResult.UPLOAD_FAILED)
			{
				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						Toast.makeText(context, R.string.upload_failed, Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}

}
