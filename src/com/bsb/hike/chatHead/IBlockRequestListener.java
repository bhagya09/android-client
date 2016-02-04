package com.bsb.hike.chatHead;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created by ashishagarwal on 30/12/15.
 */
public class IBlockRequestListener implements IRequestListener
{

	private final long  SIX_HRS = 6 * 60 * 60 * 1000l;

	private JSONObject callerSyncJSON;

	public IBlockRequestListener(JSONObject callerSyncJSON)
	{
		this.callerSyncJSON = callerSyncJSON;
	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{
		
		Logger.d("IBlockRequestListener", "block list update failure");
		setAlarmUpdateBlockedClientToServer();
	}

	@Override
	public void onRequestSuccess(Response result)
	{
		ChatHeadUtils.syncingFromClientToServer = false;
		String resultContent = result.getBody().getContent() == null ? null : result.getBody().getContent().toString();
		boolean requestSuccess = false;
		if (resultContent != null)
		{
			Logger.d("IBlockRequestListener", resultContent);
			try
			{
				JSONObject jsonObject = new JSONObject(resultContent);
				if (HikeConstants.OK.equals(jsonObject.get(HikeConstants.STATUS)) && callerSyncJSON != null)
				{
					ContactManager.getInstance().updateCallerSyncStatus(callerSyncJSON);
					requestSuccess = true;
				}
			}
			catch (JSONException e)
			{
				Logger.d("BlockRequestListener", "JSONException");
			}
		}
		if (requestSuccess)
		{
			HikeAlarmManager.cancelAlarm(HikeMessengerApp.getInstance().getApplicationContext(), HikeAlarmManager.REQUESTCODE_BLOCKED_CALLER_FROM_CLIENT_TO_SERVER);
			ChatHeadUtils.syncFromClientToServer();
		}
		else
		{
			setAlarmUpdateBlockedClientToServer();
		}
	}

	private void setAlarmUpdateBlockedClientToServer()
	{
		ChatHeadUtils.syncingFromClientToServer = false;
		HikeAlarmManager.cancelAlarm(HikeMessengerApp.getInstance().getApplicationContext(), HikeAlarmManager.REQUESTCODE_BLOCKED_CALLER_FROM_CLIENT_TO_SERVER);
		HikeAlarmManager.setAlarmPersistance(HikeMessengerApp.getInstance().getApplicationContext(), Calendar.getInstance().getTimeInMillis() + SIX_HRS,
				HikeAlarmManager.REQUESTCODE_BLOCKED_CALLER_FROM_CLIENT_TO_SERVER, false, true);
		Logger.d("IBlockRequestListener", "Cancelling old Alarm if any and Setting new Alarm");

	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{

	}
}
