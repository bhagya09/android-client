package com.bsb.hike.tasks;

import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;

public class StatusUpdateTask implements IHikeHTTPTask
{
	private String status;

	private int moodId;

	private RequestToken token;

	public StatusUpdateTask(String status, int moodId)
	{
		this.status = status;
		this.moodId = moodId;
		token = HttpRequests.postStatusRequest(getPostData(), getRequestListener());
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
				Logger.d(getClass().getSimpleName(), " post status request succeeded : " + response);
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
				StatusMessage statusMessage = new StatusMessage(0, mappedId, msisdn, name, text, StatusMessageType.TEXT, time, moodId, timeOfDay);
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