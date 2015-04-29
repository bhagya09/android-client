package com.bsb.hike.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.notifications.ToastListener;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class PreloadNotificationSchedular
{

	public static final String CURRENT_ALARM_ID = "current_alarm_id";

	public static final String NOTIFICATION_TIMELINE = "notification_timeline";

	public static final String INCENTIVE_ID = "incentive_id";

	public static final String NOTIFICATION_TITLE = "notification_title";

	public static final String NOTIFICATION_TEXT = "notification_text";

	private static final String TEMP_INCENTIVE_ID = "temp_incentive_id";
	
	private static final String TEXT="text";
	
	private static final String TITLE="title";
	
	private static final String NOTIFICATION_SCHEDULE="notification_schedule";
	
	private static final String TIMESTAMP="timestamp";
	
	private static final String TAG="ALARM MANAGER";
	
	private static final String DEFAULT_INCENTIVE="-1";

	/**
	 * 
	 * @param context
	 * 
	 * 
	 *            Responsible for scheduling the next alarm
	 */

	public static void scheduleNextAlarm(Context context)
	{

		HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();
		int id = mprefs.getData(CURRENT_ALARM_ID, 0);

		long time = getTime(mprefs.getData(NOTIFICATION_TIMELINE, null), id, context);
		Logger.d(TAG, time + "");

		if (time == 0)
		{
			HikeAlarmManager.cancelAlarm(context, HikeAlarmManager.REQUESTCODE_NOTIFICATION_PRELOAD);

		}
		else if (time > System.currentTimeMillis())
		{
			Logger.i(TAG, "Alarm Set Called");
			HikeAlarmManager.setAlarm(context, time, HikeAlarmManager.REQUESTCODE_NOTIFICATION_PRELOAD, true);
		}

		else
		/**
		 * 
		 * This case occurs if the device is shut down and then restarted after some time and our time for notification is gone And also if there is some problem with the time of
		 * the device.So checking for the next time of notification
		 * 
		 */
		if (time < System.currentTimeMillis())
		{
			mprefs.saveData(CURRENT_ALARM_ID, id + 1);
			scheduleNextAlarm(context);
		}

	}

	/**
	 * 
	 * @param json
	 * @param id
	 * @return Retrieving the time for alarm from the response.
	 */
	private static long getTime(String json, int id, Context context)
	{

		if (json == null)
			return 0;

		JSONArray arr = null;
		try
		{

			arr = new JSONObject(json).optJSONArray(NOTIFICATION_SCHEDULE);

			if (arr != null)
			{
				JSONObject obj = arr.optJSONObject(id);

				if (obj != null)
				{
					String time = obj.optString(TIMESTAMP);
					if (time != null)
					{
						HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();
						mprefs.saveData(NOTIFICATION_TEXT, obj.optString(TEXT));
						mprefs.saveData(NOTIFICATION_TITLE, obj.optString(TITLE));
						mprefs.saveData(TEMP_INCENTIVE_ID, obj.optString(INCENTIVE_ID));

						return Long.parseLong(time);
					}
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();

		}
		return 0;
	}

	/**
	 * 
	 * @param context
	 *            Responsible for showing the notification when the alarm is received
	 */
	public static void run(Context context)
	{
		HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();
		ToastListener mmListener = ToastListener.getInstance(context);

		String title = mprefs.getData(NOTIFICATION_TEXT, null);
		String text = mprefs.getData(NOTIFICATION_TITLE, null);
		
		/*
		 *Adding an check that weather the user has signed up or not.
		 * 
		 */
		if (title != null && text != null && mmListener != null&&!Utils.isUserSignedUp(context, false)&&SignupTask.signupTask==null)
		{

			mmListener.notifyUser(text, title);

			// This has been done so that the correct incentive id is sent to the server when the notification is shown.Previously notification was scheduled.This time the

			// notification is shown .So this is the correct incentive.

			mprefs.saveData(INCENTIVE_ID, mprefs.getData(TEMP_INCENTIVE_ID, DEFAULT_INCENTIVE));

		}

		mprefs.saveData(PreloadNotificationSchedular.CURRENT_ALARM_ID, mprefs.getData(PreloadNotificationSchedular.CURRENT_ALARM_ID, 0) + 1);
		scheduleNextAlarm(context);
	}
}