package com.bsb.hike.chatHead;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class ChatHeadUtils
{
	public static final int CREATING_CHAT_HEAD_ACTIVITY_ANIMATION = 0;

	public static final int FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION = 1;

	public static final int SHARING_BEFORE_FINISHING_ANIMATION = 2;

	public static final int STOPPING_SERVICE_ANIMATION = 3;
	
	public static final int GET_MORE_STICKERS_ANIMATION = 4;

	public static final int OPEN_HIKE_ANIMATION = 5;

	public static final int STICKER_SHOP_ANIMATION = 6;

	public static final int REMAINING_ANIMATION = 7;
	
	public static final String DAILY_STICKER_SHARE_COUNT = "shr_cnt_dly";
	
	private static final String SERVICE_LAST_USED= "lastUsed";

	private static Set<String> foregroundedPackages;
	
	public static boolean isSharingPackageInstalled(Context context)
	{
		JSONArray jsonObj;
		try
		{
			jsonObj = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, ""));

			for (int i = 0; i < jsonObj.length(); i++)
			{
				JSONObject obj = jsonObj.getJSONObject(i);
				{
					if (Utils.isPackageInstalled(context, obj.optString(HikeConstants.ChatHead.PACKAGE_NAME, "")))
					{
						return true;
					}
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public static Set<String> getForegroundedPackages()
	{
		if (foregroundedPackages == null)
		{
			foregroundedPackages = new HashSet<String>(5);
		}
		else
		{
			foregroundedPackages.clear();
		}
		
		ActivityManager mActivityManager = (ActivityManager) HikeMessengerApp.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager.getRunningAppProcesses();
		for (ActivityManager.RunningAppProcessInfo processInfo : processInfos)
		{
			if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && processInfo.importanceReasonCode == 0)
			{
				foregroundedPackages.add(processInfo.processName);
			}
		}

		return foregroundedPackages;
	}

	public static void settingDailySharedPref()
	{   if (HikeSharedPreferenceUtil.getInstance().getData(SERVICE_LAST_USED, -1L) == -1L)
	 	{
			HikeSharedPreferenceUtil.getInstance().saveData(SERVICE_LAST_USED, Utils.gettingMidnightTimeinMilliseconds());
	 	}
		if ((int) ((Utils.gettingMidnightTimeinMilliseconds() - (HikeSharedPreferenceUtil.getInstance().getData(SERVICE_LAST_USED,
				Utils.gettingMidnightTimeinMilliseconds()))) / (24 * 60 * 60 * 1000)) > 0)
		{    
			HikeSharedPreferenceUtil.getInstance().saveData(SERVICE_LAST_USED, Utils.gettingMidnightTimeinMilliseconds());
			HikeSharedPreferenceUtil.getInstance().saveData(DAILY_STICKER_SHARE_COUNT, 0);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, 0);
		}
	}

	private static void startService(Context context)
	{
		if (!Utils.isMyServiceRunning(ChatHeadService.class, context) && !HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SNOOZE, false))
		{
			context.startService(new Intent(context, ChatHeadService.class));
		}
	}

	public static void stopService(Context context)
	{
		if (Utils.isMyServiceRunning(ChatHeadService.class, context))
		{
			context.stopService(new Intent(context, ChatHeadService.class));
		}
	}

	private static void restartService(Context context)
	{
		stopService(context);
		startService(context);
	}

	public static void startOrStopService(Context context, boolean jsonChanged)
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_SERVICE, false)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_USR_CONTROL, false)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, null) != null)
		{
			if (jsonChanged)
			{
				restartService(context);
			}
			else
			{
				startService(context);
			}
		}
		else
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, false);
			HikeAlarmManager.cancelAlarm(context, HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE);
			stopService(context);
		}
	}
	
	public static void onClickSetAlarm(Context context, int time)
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, true);
		HikeAlarmManager.setAlarm(context, Calendar.getInstance().getTimeInMillis() + time, HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE, false);
		ChatHeadService.getInstance().resetPosition(ChatHeadUtils.STOPPING_SERVICE_ANIMATION);
	}


}
