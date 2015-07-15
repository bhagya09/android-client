package com.bsb.hike.chatHead;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.widget.HeterogeneousExpandableList;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ChatHeadUtils
{
	public static int shareCount, totalShareCount, noOfDays, shareLimit, maxDismissLimit;
	
	public static final String SERVICE_START_DATE= "strtDate";
	
    private static final String SERVICE_LAST_USED = "lastUsed";
    
    private static final String TAG = "ChatHeadUtils";

	private static Set<String> foregroundedPackages;

	public static boolean areWhitelistedPackagesSharable(Context context)
	{
		try
		{
			JSONArray whitelistedPackages = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, ""));
			List<String> packgesWithImageShareAbility = Utils.getPackagesMatchingIntent(Intent.ACTION_SEND, null, "image/jpeg");
			for (int i = 0; i < whitelistedPackages.length(); i++)
			{
				if(packgesWithImageShareAbility.contains(whitelistedPackages.getJSONObject(i).optString(HikeConstants.ChatHead.PACKAGE_NAME)))
				{
					return true;
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public static void initVariables()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(SERVICE_START_DATE, -1L) == -1L)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(SERVICE_START_DATE, Utils.gettingMidnightTimeinMilliseconds());
		}
		noOfDays = (int) ((Utils.gettingMidnightTimeinMilliseconds() - (HikeSharedPreferenceUtil.getInstance().getData(SERVICE_START_DATE,
				Utils.gettingMidnightTimeinMilliseconds()))) / (24 * ChatHeadConstants.HOUR_TO_MILLISEC_CONST)) + 1;
		if (noOfDays < 1)
		{
			noOfDays = 1;
		}
		totalShareCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT, 0);
		shareLimit = (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.STICKERS_PER_DAY, HikeConstants.ChatHead.DEFAULT_NO_STICKERS_PER_DAY) + HikeSharedPreferenceUtil
				.getInstance().getData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, 0));
		maxDismissLimit = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DISMISS_COUNT, ChatHeadConstants.DISMISS_CONST);
		shareCount = HikeSharedPreferenceUtil.getInstance().getData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, 0);
		if (shareCount > shareLimit)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, shareLimit);
			shareCount = shareLimit;
		}
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
		
		if(Utils.isLollipopOrHigher())
		{
			Field field = null;
			try
			{
				field = RunningAppProcessInfo.class.getDeclaredField("processState");
			} catch (NoSuchFieldException e)
			{
				Logger.d(ChatHeadUtils.class.getSimpleName(),  e.toString());
			}
			List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager.getRunningAppProcesses();
			for (ActivityManager.RunningAppProcessInfo processInfo : processInfos)
			{
				if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && processInfo.importanceReasonCode == 0)
				{
					Integer state = null;
					try
					{
						state = field.getInt(processInfo);
					}
					catch (IllegalAccessException e)
					{
						Logger.d(ChatHeadUtils.class.getSimpleName(),  e.toString());
					}
					catch (IllegalArgumentException e)
					{
						Logger.d(ChatHeadUtils.class.getSimpleName(),  e.toString());
					}
					if (state != null && state == 2) 
					{
						foregroundedPackages.add(processInfo.processName);
					}
				}
			}
		}
		else
		{
			try
			{
				List<RunningTaskInfo>  runningTasks = mActivityManager.getRunningTasks(1);
				if(runningTasks != null && !runningTasks.isEmpty())
				{
					foregroundedPackages.add(runningTasks.get(0).topActivity.getPackageName());
				}
			}
			catch (SecurityException se)
			{
				Logger.d(TAG, "SecurityException in fetching Tasks");
			}
			catch (Exception e)
			{
				Logger.d(TAG,"Exception in fetching tasks");
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
		boolean isSnoozed = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SNOOZE, false);
		boolean sessionLogEnabled = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SESSION_LOG_TRACKING, false);
		if (!Utils.isMyServiceRunning(ChatHeadService.class, HikeMessengerApp.getInstance()) && (!isSnoozed || sessionLogEnabled))
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
		boolean sessionLogEnabled = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SESSION_LOG_TRACKING, false);
		boolean chatHeadEnabledAndValid = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_SERVICE, false)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_USR_CONTROL, false)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, null) != null;
		if (chatHeadEnabledAndValid || sessionLogEnabled)
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
		if(!chatHeadEnabledAndValid)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, false);
			HikeAlarmManager.cancelAlarm(HikeMessengerApp.getInstance(), HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE);
			if(!sessionLogEnabled)
			{
				stopService();
			}
		}
	}

	public static void onClickSetAlarm(Context context, int time)
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, true);
		HikeAlarmManager.setAlarm(context, Calendar.getInstance().getTimeInMillis() + time, HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE, false);
		ChatHeadService.getInstance().resetPosition(ChatHeadConstants.STOPPING_SERVICE_ANIMATION, null);
	}
	
	public static void setAllApps(JSONArray pkgList, boolean toSet)
	{
		try
		{
			for (int j = 0; j < pkgList.length(); j++)
			{
				pkgList.getJSONObject(j).put(HikeConstants.ChatHead.APP_ENABLE, toSet);
			}
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.PACKAGE_LIST, pkgList.toString());
			ChatHeadUtils.startOrStopService(true);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}
}
