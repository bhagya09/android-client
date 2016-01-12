package com.bsb.hike.chatHead;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.hike.transporter.utils.Logger;

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

	@Override
	public void onRequestFailure(HttpException httpException)
	{
		setAlarmSyncingBlockedListFromServerToClient();
		Logger.d("ICallerSignUpListener", "Setting Alarm");
	}

	@Override
	public void onRequestSuccess(Response result)
	{
		String resultContent = result.getBody().getContent() == null ? null : result.getBody().getContent().toString();
		boolean isStatusFail = true;
		if (resultContent != null)
		{
			try
			{
				JSONObject jsonObject = new JSONObject(resultContent);
				if (HikeConstants.OK.equals(jsonObject.get(HikeConstants.STATUS)))
				{
					if(jsonObject.has(StickyCaller.BLOCK_MSISDNS))
					{
						ArrayList<CallerContentModel> callerContentModelArray = new ArrayList();
						JSONArray callerContent = jsonObject.getJSONArray(StickyCaller.BLOCK_MSISDNS);
						for (int i = 0; i < callerContent.length(); i++)
						{
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
		HikeAlarmManager.cancelAlarm(HikeMessengerApp.getInstance().getApplicationContext(), HikeAlarmManager.REQUESTCODE_FETCH_BLOCK_LIST_CALLER);
		HikeAlarmManager.setAlarmPersistance(HikeMessengerApp.getInstance().getApplicationContext(), Calendar.getInstance().getTimeInMillis() + FIVE_MINS,
				HikeAlarmManager.REQUESTCODE_FETCH_BLOCK_LIST_CALLER, false, true);

	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{

	}
}
