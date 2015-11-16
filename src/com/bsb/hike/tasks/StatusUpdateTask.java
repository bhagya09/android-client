package com.bsb.hike.tasks;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Base64;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class StatusUpdateTask implements IHikeHTTPTask
{
	private String status;

	private int moodId;

	private RequestToken token;

	private String imageFilePath;
	
	private String language;

	public StatusUpdateTask(String status, int argMoodId, String imageFilePath, String language) throws IOException
	{
		this.status = status;
		this.moodId = argMoodId;
		this.imageFilePath = imageFilePath;
		this.language = language;
		token = HttpRequests.postStatusRequest(status, argMoodId, getRequestListener(), imageFilePath, language);
	}

	@Override
	public void execute()
	{
		token.execute();
	}

	@Override
	public void cancel()
	{
		token.cancel();
	}

	public JSONObject getPostData()
	{
		JSONObject data = new JSONObject();
		try
		{
			data.put(HikeConstants.STATUS_MESSAGE_2, status);
			if (moodId != -1)
			{
				data.put(HikeConstants.MOOD, moodId + 1);
				data.put(HikeConstants.TIME_OF_DAY, getTimeOfDay());
			}
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
		}
		return data;
	}

	public IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();
				Logger.d(getClass().getSimpleName(), "post status request succeeded : " + response);
				SharedPreferences preferences = HikeMessengerApp.getInstance().getApplicationContext()
						.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);
				JSONObject data = response.optJSONObject("data");
				String mappedId = data.optString(HikeConstants.STATUS_ID);
				String text = data.optString(HikeConstants.STATUS_MESSAGE);
				int moodId = data.optInt(HikeConstants.MOOD) - 1;
				
				int timeOfDay = data.optInt(HikeConstants.TIME_OF_DAY);
				String msisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");
				String name = preferences.getString(HikeMessengerApp.NAME_SETTING, "");
				long time = (long) System.currentTimeMillis() / 1000;
				
				final String iconBase64 = data.optString(HikeConstants.THUMBNAIL);
				
				final String fileKey = data.optString(HikeConstants.SU_IMAGE_KEY);
				
				StatusMessageType suType = getStatusMessageType(text, imageFilePath);
				
				if(suType == StatusMessageType.TEXT_IMAGE || suType == StatusMessageType.IMAGE)
				{
					// TODO Support all image formats (same code is present in ChangeProfileImageBaseActivity)
					String destFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + mappedId + ".jpg";

					Utils.copyFile(imageFilePath, destFilePath);
					
					HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
					{
						@Override
						public void run()
						{
							// Add to fileThumbnail table viz. fk - thumb
							HikeConversationsDatabase.getInstance().addFileThumbnail(fileKey, Base64.decode(iconBase64, Base64.DEFAULT));
						}
					}, 0);
				}
				
				StatusMessage statusMessage = new StatusMessage(0, mappedId, msisdn, name, text, suType, time, moodId, timeOfDay,fileKey);
				HikeConversationsDatabase.getInstance().addStatusMessage(statusMessage, true);
				int unseenUserStatusCount = preferences.getInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
				Editor editor = preferences.edit();
				editor.putString(HikeMessengerApp.LAST_STATUS, text);
				editor.putInt(HikeMessengerApp.LAST_MOOD, moodId);
				editor.putInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, ++unseenUserStatusCount);
				editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
				editor.commit();
				HikeMessengerApp.getPubSub().publish(HikePubSub.MY_STATUS_CHANGED, text);
				/*
				 * This would happen in the case where the user has added a self contact and received an mqtt message before saving this to the db.
				 */
				if (statusMessage.getId() != -1)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
					HikeMessengerApp.getPubSub().publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
				}
				HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_POST_REQUEST_DONE, true);
			}

			private StatusMessageType getStatusMessageType(String text, String imageFilePath)
			{
				// TODO server sends string "null" incase text is not present

				if (text.equals("null"))
				{
					text = null;
				}
				if (TextUtils.isEmpty(text) && TextUtils.isEmpty(imageFilePath))
				{
					throw new IllegalArgumentException("Both text and image cannot be empty");
				}

				File imageFile = null;
				if (!TextUtils.isEmpty(imageFilePath))
				{
					imageFile = new File(imageFilePath);
					if (!imageFile.exists())
					{
						imageFile = null;
					}
				}

				if (TextUtils.isEmpty(text) && imageFile != null)
				{
					return StatusMessageType.IMAGE;
				}
				else if (!TextUtils.isEmpty(text) && imageFile == null)
				{
					return StatusMessageType.TEXT;
				}
				else
				{
					return StatusMessageType.TEXT_IMAGE;
				}

			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.e(getClass().getSimpleName(), " post status request failed : " + httpException.getMessage());
				HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_POST_REQUEST_DONE, false);
			}
		};
	}
	
	private int getTimeOfDay()
	{
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		if (hour >= 4 && hour < 12)
		{
			return 1;
		}
		else if (hour >= 12 && hour < 20)
		{
			return 2;
		}
		return 3;
	}
}