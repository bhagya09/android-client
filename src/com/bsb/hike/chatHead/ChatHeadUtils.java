package com.bsb.hike.chatHead;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ChatHeadUtils
{
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

	public static boolean isPackageForeground(Context context, String packageName)
	{
		ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager.getRunningAppProcesses();
		for (ActivityManager.RunningAppProcessInfo processInfo : processInfos)
		{
			if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && processInfo.importanceReasonCode == 0)
			{
				if (processInfo.processName.equals(packageName))
				{
					ChatHeadService.foregroundApp = packageName;
					return true;
				}
			}
		}

		return false;
	}

	public static void settingDailySharedPref()
	{   if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SERVICE_LAST_USED, -1L) == -1L)
	 	{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SERVICE_LAST_USED, Utils.gettingMidnightTimeinMilliseconds());
	 	}
		if ((int) ((Utils.gettingMidnightTimeinMilliseconds() - (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SERVICE_LAST_USED,
				Utils.gettingMidnightTimeinMilliseconds()))) / (24 * 60 * 60 * 1000)) > 0)
		{    
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SERVICE_LAST_USED, Utils.gettingMidnightTimeinMilliseconds());
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.DAILY_STICKER_SHARE_COUNT, 0);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, 0);
		}
	}

	private static void startService(Context context)
	{
		if (!Utils.isMyServiceRunning(ChatHeadService.class, context) && !ChatHeadService.snooze)
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

	public static void serviceDecision(Context context, boolean jsonChanged)
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
			ChatHeadService.snooze = false;
			HikeAlarmManager.cancelAlarm(context, HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE);
			stopService(context);
		}
	}

}
