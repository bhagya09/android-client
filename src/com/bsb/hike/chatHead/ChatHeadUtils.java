package com.bsb.hike.chatHead;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.widget.HeterogeneousExpandableList;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class ChatHeadUtils
{
    private static final String SERVICE_LAST_USED = "lastUsed";

	private static Set<String> foregroundedPackages;

	public static boolean isSharingPackageInstalled(Context context)
	{
		try
		{
			JSONArray packageJSONArray = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, ""));

			for (int i = 0; i < packageJSONArray.length(); i++)
			{
				JSONObject obj = packageJSONArray.getJSONObject(i);
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
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(SERVICE_LAST_USED, -1L) == -1L)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(SERVICE_LAST_USED, Utils.gettingMidnightTimeinMilliseconds());
		}
		if ((int) ((Utils.gettingMidnightTimeinMilliseconds() - (HikeSharedPreferenceUtil.getInstance().getData(SERVICE_LAST_USED, Utils.gettingMidnightTimeinMilliseconds()))) / (24 * ChatHeadConstants.HOUR_TO_MILLISEC_CONST)) > 0)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(SERVICE_LAST_USED, Utils.gettingMidnightTimeinMilliseconds());
			HikeSharedPreferenceUtil.getInstance().saveData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, 0);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, 0);
		}
	}

	private static void startService()
	{
		if (!Utils.isMyServiceRunning(ChatHeadService.class, HikeMessengerApp.getInstance()) && !HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SNOOZE, false))
		{
			HikeMessengerApp.getInstance().startService(new Intent(HikeMessengerApp.getInstance(), ChatHeadService.class));
		}
	}

	public static void stopService()
	{
		if (Utils.isMyServiceRunning(ChatHeadService.class, HikeMessengerApp.getInstance()))
		{
			HikeMessengerApp.getInstance().stopService(new Intent(HikeMessengerApp.getInstance(), ChatHeadService.class));
		}
	}

	private static void restartService()
	{
		stopService();
		startService();
	}

	public static void startOrStopService(boolean jsonChanged)
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_SERVICE, false)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_USR_CONTROL, false)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, null) != null)
		{
			if (jsonChanged)
			{
				restartService();
			}
			else
			{
				startService();
			}
		}
		else
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, false);
			HikeAlarmManager.cancelAlarm(HikeMessengerApp.getInstance(), HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE);
			stopService();
		}
	}

	public static void onClickSetAlarm(Context context, int time)
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, true);
		HikeAlarmManager.setAlarm(context, Calendar.getInstance().getTimeInMillis() + time, HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE, false);
		ChatHeadService.getInstance().resetPosition(ChatHeadConstants.STOPPING_SERVICE_ANIMATION);
	}

}
