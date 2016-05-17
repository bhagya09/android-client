package com.bsb.hike.filetransfer;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class UploadContactOrLocationTask extends FileTransferBase
{
	private static final String TAG = "UploadContactOrLocationTask";

	private static final String STATIC_MAP_UNFORMATTED_URL = "http://maps.googleapis.com/maps/api/staticmap?center=%1$f,%2$f&zoom=%3$d&size=%4$dx%4$d&markers=size:mid|color:red|%1$f,%2$f&sensor=false";

	private double latitude;

	private double longitude;

	private int zoomLevel;

	private String address;

	private boolean uploadingContact;

	protected long maxSize = 100; // just to avoid divide by zero operation exception

	protected UploadContactOrLocationTask(Context ctx, ConvMessage convMessage, boolean uploadingContact)
	{
		super(ctx, null, -1, null);
        this.msgId = convMessage.getMsgID();
        this.userContext = convMessage;
		this.uploadingContact = uploadingContact;
	}

	public void execute()
	{
		try
		{
			// If we don't have a file key, that means we haven't uploaded the
			// file to the server yet

			if (TextUtils.isEmpty(fileKey))
			{
				requestToken = HttpRequests.uploadContactOrLocation(uploadingContact ? HikeConstants.CONTACT_FILE_NAME : HikeConstants.LOCATION_FILE_NAME,
						((ConvMessage) userContext).getMetadata().getJSON(), uploadingContact ? HikeConstants.CONTACT_CONTENT_TYPE : HikeConstants.LOCATION_CONTENT_TYPE,
						getUploadContactorLocationRequestListener(), getUploadContactOrLocationInterceptor());
				requestToken.execute();
			}
			else
			{
				send(true);
			}
		}
		catch (Exception ex)
		{
			Logger.e(TAG, "exception occurred ", ex);
			doOnFailure();
		}
	}

	private IRequestInterceptor getUploadContactOrLocationInterceptor()
	{
		return new IRequestInterceptor() {
			@Override
			public void intercept(Chain chain) throws Exception
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
					chain.proceed();
				}
				catch (Exception ex)
				{
					Logger.e(TAG, "exception occurred ", ex);
					doOnFailure();
				}
			}
		};
	}

	private IRequestListener getUploadContactorLocationRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();

				if (!"ok".equals(response.optString("stat")))
				{
					onRequestFailure(null);
					return;
				}

				JSONObject fileJSON = response.optJSONObject("data");
				fileKey = fileJSON.optString(HikeConstants.FILE_KEY);
				fileSize = fileJSON.optInt(HikeConstants.FILE_SIZE);
				send(false);
				FileTransferManager.getInstance(context).removeTask(msgId);
                HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
				FileTransferManager.getInstance(context).logTaskCompletedAnalytics(msgId, userContext, false);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				if (httpException.getErrorCode() == HttpException.REASON_CODE_CANCELLATION || httpException.getErrorCode() == HttpException.REASON_CODE_REQUEST_PAUSED)
				{
					FileTransferManager.getInstance(context).removeTask(msgId);
				}
				else
				{
					doOnFailure();
				}
			}
		};
	}

	private void doOnFailure() {
		FileTransferManager.getInstance(context).removeTask(msgId);
		if (userContext != null)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		}
		showToast(HikeConstants.FTResult.UPLOAD_FAILED);
	}

	private void send(boolean fileWasAlreadyUploaded)
	{
		try
		{
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

			if (convMessageObject.isBroadcastConversation())
			{
				List<PairModified<GroupParticipant, String>> participantList = ContactManager.getInstance().getGroupParticipants(convMessageObject.getMsisdn(), false, false);
				for (PairModified<GroupParticipant, String> grpParticipant : participantList)
				{
					String msisdn = grpParticipant.getFirst().getContactInfo().getMsisdn();
					convMessageObject.addToSentToMsisdnsList(msisdn);
				}
				OneToNConversationUtils.addBroadcastRecipientConversations(convMessageObject);
			}
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessageObject);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Exception while sending localtion ot contact ", e);
			doOnFailure();
		}
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

	private JSONObject getFileTransferMetadataForLocation(double latitude, double longitude, int zoomLevel, String address, String thumbnailString) throws JSONException
	{
		JSONArray files = new JSONArray();
		files.put(new HikeFile(latitude, longitude, zoomLevel, address, thumbnailString, null, true).serialize());
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);
		return metadata;
	}

	private void showToast(final HikeConstants.FTResult result)
	{
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				switch (result)
				{
					case UPLOAD_FAILED:
						Toast.makeText(context, R.string.upload_failed, Toast.LENGTH_SHORT).show();
						break;
				}
			}
		});
	}
}
