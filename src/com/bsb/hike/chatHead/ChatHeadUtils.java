package com.bsb.hike.chatHead;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
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
import com.bsb.hike.userlogs.PhoneSpecUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ChatHeadUtils
{
    private static final String SERVICE_LAST_USED = "lastUsed";
    
    private static final String TAG = "ChatHeadUtils";

	public static final int GET_TOP_MOST_SINGLE_PROCESS = 0; 

	public static final int GET_FOREGROUND_PROCESSES = 1; 

	public static final int GET_ALL_RUNNING_PROCESSES = 2; 

	
	/**
	 * returns the package names of the running processes can be single, all or in tasks packages as per argument
	 */
	public static Set<String> getRunningAppPackage(int type)
	{
		Context context = HikeMessengerApp.getInstance();
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
		Set<String> packageName = new HashSet<String>();
		
		//called if all the packages whose processes are running is needed
		if (type == GET_ALL_RUNNING_PROCESSES)
		{
			Iterator runningAppProcessInfo = processInfos.iterator();
			while (runningAppProcessInfo.hasNext())
			{
				ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (runningAppProcessInfo.next());
				packageName.add(PhoneSpecUtils.getPackageFromProcess(info.processName));
			}
			return packageName;
		}

		//called if all the packages whose task is running is needed
		return getRunningTaskPackage(context, activityManager, processInfos, packageName, type);
	}

	public static Set<String> getRunningTaskPackage(Context context, ActivityManager activityManager, List<RunningAppProcessInfo> processInfos, Set<String> packageName, int type)
	{
		if (Utils.isLollipopOrHigher())
		{
			if (type == GET_TOP_MOST_SINGLE_PROCESS)
			{
				Field field = null;
				try
				{
					field = RunningAppProcessInfo.class.getDeclaredField("processState");
				}
				catch (NoSuchFieldException e)
				{
					Logger.d(ChatHeadUtils.class.getSimpleName(), e.toString());
				}
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
							Logger.d(ChatHeadUtils.class.getSimpleName(), e.toString());
						}
						catch (IllegalArgumentException e)
						{
							Logger.d(ChatHeadUtils.class.getSimpleName(), e.toString());
						}
						if (state != null && state == 2)
						{
							packageName.add(PhoneSpecUtils.getPackageFromProcess(processInfo.processName));
						}
					}
				}

				return packageName;
			}
			else 
			{
				for (ActivityManager.RunningAppProcessInfo processInfo : processInfos)
				{
					if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
							|| processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE)
					{
						packageName.add(PhoneSpecUtils.getPackageFromProcess(processInfo.processName));
					}
				}
				return packageName;
			}
		}
		else
		{
			try
			{
				List<RunningTaskInfo> runningTasks = activityManager.getRunningTasks((type == GET_TOP_MOST_SINGLE_PROCESS)? 1 : Integer.MAX_VALUE);
				for (int i = 0; i < runningTasks.size(); i++)
				{
					packageName.add(runningTasks.get(i).topActivity.getPackageName());
				}
			}
			catch (SecurityException se)
			{
				Logger.d(TAG, "SecurityException in fetching Tasks");
			}
			catch (Exception e)
			{
				Logger.d(TAG, "Exception in fetching tasks");
			}
			return packageName;
		}
	}
	
	
	
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

}
