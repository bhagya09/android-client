package com.bsb.hike.chatHead;

import android.support.annotation.Nullable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by ashishagarwal on 04/01/16.
 */
public class ICallerSignUpRequestListener implements IRequestListener {

	private final long FIVE_MINS =  5 * 60 * 1000l;

	private final String TAG = "ICallerSignUpListener";

	@Override
	public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
	{
		ChatHeadUtils.blockedCallerFromServerToClientFetched = false;
		setAlarmSyncingBlockedListFromServerToClient();
	}

	@Override
	public void onRequestSuccess(Response result)
	{
		ChatHeadUtils.blockedCallerFromServerToClientFetched = false;
		String resultContent = result.getBody().getContent() == null ? null : result.getBody().getContent().toString();
		boolean isStatusFail = true;
		if (resultContent != null)
		{
			try
			{
				Logger.d(TAG, resultContent);
				JSONObject jsonObject = new JSONObject(resultContent);
				if (HikeConstants.OK.equals(jsonObject.get(HikeConstants.STATUS)))
				{
					if(jsonObject.has(StickyCaller.BLOCK_MSISDNS))
					{
						ArrayList<CallerContentModel> callerContentModelArray = new ArrayList();
						JSONArray callerContent = jsonObject.getJSONArray(StickyCaller.BLOCK_MSISDNS);
						for (int i = 0; i < callerContent.length(); i++)
						{
							Logger.d("ICallerSignUpRequestListener", callerContent.get(i).toString());
							CallerContentModel callerContentModel = ChatHeadUtils.getCallerContentModelObject(callerContent.get(i).toString());
							callerContentModelArray.add(callerContentModel);
						}
						ContactManager.getInstance().insertAllBlockedContactsIntoCallerTable(callerContentModelArray);
						HikeAlarmManager.cancelAlarm(HikeMessengerApp.getInstance().getApplicationContext(), HikeAlarmManager.REQUESTCODE_FETCH_BLOCK_LIST_CALLER);
						isStatusFail = false;
					}
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		if (isStatusFail)
		{
			setAlarmSyncingBlockedListFromServerToClient();
		}
	}

	private void setAlarmSyncingBlockedListFromServerToClient()
	{
		Logger.d(TAG, "Cancelling old Alarm if any and Setting new Alarm");
		HikeAlarmManager.cancelAlarm(HikeMessengerApp.getInstance().getApplicationContext(), HikeAlarmManager.REQUESTCODE_FETCH_BLOCK_LIST_CALLER);
		HikeAlarmManager.setAlarmPersistance(HikeMessengerApp.getInstance().getApplicationContext(), Calendar.getInstance().getTimeInMillis() + FIVE_MINS,
				HikeAlarmManager.REQUESTCODE_FETCH_BLOCK_LIST_CALLER, false, true);

	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{

	}
}
