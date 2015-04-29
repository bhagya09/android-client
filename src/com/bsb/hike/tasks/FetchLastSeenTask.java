package com.bsb.hike.tasks;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class FetchLastSeenTask extends FetchLastSeenBase
{
	public static interface FetchLastSeenCallback
	{
		public void lastSeenFetched(String msisdn, int offline, long lastSeenTime);

		public void lastSeenNotFetched();
	}

	String msisdn;

	Context context;

	FetchLastSeenCallback fetchLastSeenCallback;

	long lastSeenValue;

	int offline;

	public FetchLastSeenTask(Context context, String msisdn, FetchLastSeenCallback fetchLastSeenCallback)
	{
		/*
		 * We only need the application context in this case
		 */
		this.context = context.getApplicationContext();

		this.msisdn = msisdn;

		this.fetchLastSeenCallback = fetchLastSeenCallback;
	}

	@Override
	protected Boolean doInBackground(Void... params)
	{
		/*
		 * Adding this check to ensure we don't make a request for empty/null msisdn.
		 * TODO figure out why this is happening
		 */
		if (TextUtils.isEmpty(msisdn))
		{
			Logger.w("LastSeenTask", "msisdn is null!");
			return false;
		}
		HAManager.getInstance().recordLastSeenEvent(FetchLastSeenTask.class.getName(), "doInBackground", "Sending req", msisdn);
		try
		{
			JSONObject response = sendRequest(AccountUtils.base + "/user/lastseen/" + msisdn);
			return saveResult(response);
		}
		catch (IOException e)
		{
			Logger.w(getClass().getSimpleName(), e);
			return false;
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), e);
			return false;
		}

	}

	@Override
	public boolean saveResult(JSONObject response) throws JSONException
	{
		if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)))
		{
			return false;
		}
		JSONObject data = response.getJSONObject(HikeConstants.DATA);
		long result = data.getLong(HikeConstants.LAST_SEEN);

		/*
		 * Update current last seen value.
		 */
		long currentLastSeenValue = result;
		/*
		 * We only apply the offset if the value is greater than 0 since 0 and -1 are reserved.
		 */
		if (currentLastSeenValue > 0)
		{
			offline = 1;
			lastSeenValue = Utils.applyServerTimeOffset(context, currentLastSeenValue);
		}
		else
		{
			offline = (int) currentLastSeenValue;
			lastSeenValue = System.currentTimeMillis() / 1000;
		}

		ContactManager.getInstance().updateLastSeenTime(msisdn, lastSeenValue);
		ContactManager.getInstance().updateIsOffline(msisdn, offline);
		
		HAManager.getInstance().recordLastSeenEvent(FetchLastSeenTask.class.getName(), "saveResult", "Updated CM", msisdn);
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		HAManager.getInstance().recordLastSeenEvent(FetchLastSeenTask.class.getName(), "onPostExecute", "reseult recv "+ result, msisdn);
		if (!result)
		{
			fetchLastSeenCallback.lastSeenNotFetched();
		}
		else
		{
			fetchLastSeenCallback.lastSeenFetched(msisdn, offline, lastSeenValue);
		}
	}

}