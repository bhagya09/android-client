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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.userlogs.PhoneSpecUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ChatHeadUtils
{
	public static int noOfDays, shareLimit;
	
	public static final String SERVICE_START_DATE= "strtDate";
	
    private static final String SERVICE_LAST_USED = "lastUsed";
    
    private static final String TAG = "ChatHeadUtils";

	public static final int GET_TOP_MOST_SINGLE_PROCESS = 0; 

	public static final int GET_FOREGROUND_PROCESSES = 1; 

	public static final int GET_ALL_RUNNING_PROCESSES = 2; 
	
	private static IncomingCallReceiver incomingCallReceiver;
	
	private static OutgoingCallReceiver outgoingCallReceiver;
	
		
	// replica of hidden constant ActivityManager.PROCESS_STATE_TOP 
	public static final int PROCESS_STATE_TOP =2;

	/**
	 * returns the package names of the running processes can be single, all or in tasks packages as per argument
	 */
	public static Set<String> getRunningAppPackage(int type)
	{
		Context context = HikeMessengerApp.getInstance().getApplicationContext();
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
		Set<String> packageName = new HashSet<String>();
		
		//called if all the packages whose processes are running is needed
		if (type == GET_ALL_RUNNING_PROCESSES)
		{
			if(processInfos != null && !processInfos.isEmpty())
			{
				Iterator<RunningAppProcessInfo> runningAppProcessInfo = processInfos.iterator();
				while (runningAppProcessInfo.hasNext())
				{
					ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (runningAppProcessInfo.next());
					packageName.add(PhoneSpecUtils.getPackageFromProcess(info.processName));
				}
			}
		}
		else
		{
			// called if all the packages whose task is running is needed
			getRunningTaskPackage(context, activityManager, processInfos, packageName, type);
		}
		return packageName;
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
		shareLimit = (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.STICKERS_PER_DAY, HikeConstants.ChatHead.DEFAULT_NO_STICKERS_PER_DAY) + HikeSharedPreferenceUtil
				.getInstance().getData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, 0));
		if (HikeSharedPreferenceUtil.getInstance().getData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, 0) > shareLimit)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, shareLimit);
			HikeSharedPreferenceUtil.getInstance().saveData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, shareLimit);
		}
	}

	public static void getRunningTaskPackage(Context context, ActivityManager activityManager, List<RunningAppProcessInfo> processInfos, Set<String> packageName, int type)
	{
		if (Utils.isLollipopOrHigher())
		{
			if(processInfos == null)
			{
				return;
			}
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
						// its a hidden api and no value is defined 
						if (state != null && state == PROCESS_STATE_TOP)
						{
							packageName.add(PhoneSpecUtils.getPackageFromProcess(processInfo.processName));
						}
					}
				}
			}
			else if(type == GET_FOREGROUND_PROCESSES) 
			{
				for (ActivityManager.RunningAppProcessInfo processInfo : processInfos)
				{
					if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
							|| processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE)
					{
						packageName.add(PhoneSpecUtils.getPackageFromProcess(processInfo.processName));
					}
				}
			}
		}
		// Lower than LOLLIPOP or processInfos null
		else
		{
			try
			{
				List<RunningTaskInfo> runningTasks = activityManager.getRunningTasks((type == GET_TOP_MOST_SINGLE_PROCESS)? 1 : Integer.MAX_VALUE);
				if(runningTasks != null && !runningTasks.isEmpty())
				{
					for (int i = 0; i < runningTasks.size(); i++)
					{
						packageName.add(runningTasks.get(i).topActivity.getPackageName());
					}
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
	
	public static boolean isAccessibilityEnabled(Context ctx)
	{
		int accessibilityEnabled = 0;
		final String LIGHTFLOW_ACCESSIBILITY_SERVICE = "com.bsb.hike/com.bsb.hike.service.HikeAccessibilityService";
		boolean accessibilityFound = false;
		try
		{
			accessibilityEnabled = Settings.Secure.getInt(ctx.getContentResolver(), android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
			Logger.d(TAG, "ACCESSIBILITY: " + accessibilityEnabled);
		}
		catch (SettingNotFoundException e)
		{
			Logger.d(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
		}

		TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

		if (accessibilityEnabled == 1)
		{
			Logger.d(TAG, "***ACCESSIBILIY IS ENABLED***: ");

			String settingValue = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
			Logger.d(TAG, "Setting: " + settingValue);
			if (settingValue != null)
			{
				TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
				splitter.setString(settingValue);
				while (splitter.hasNext())
				{
					String accessabilityService = splitter.next();
					Logger.d(TAG, "Setting: " + accessabilityService);
					if (accessabilityService.equalsIgnoreCase(LIGHTFLOW_ACCESSIBILITY_SERVICE))
					{
						Logger.d(TAG, "We've found the correct setting - accessibility is switched on!");
						return true;
					}
				}
			}

			Logger.d(TAG, "***END***");
		}
		else
		{
			Logger.d(TAG, "***ACCESSIBILIY IS DISABLED***");
		}
		return accessibilityFound;
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

	public static boolean shouldRunChatHeadServiceForStickey()
	{
		boolean enabledForUser = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_SERVICE, false);
		boolean permittedToRun = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.CHAT_HEAD_USR_CONTROL, false);
		boolean packageListNonEmpty = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, null) != null;
		return enabledForUser && permittedToRun && packageListNonEmpty;
	}
	
	public static boolean shouldShowAccessibility()
	{
		boolean showAccessibility = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SHOW_ACCESSIBILITY, true);
		if(!showAccessibility)
		{
			return false;
		}
		return  !isAccessibilityEnabled(HikeMessengerApp.getInstance().getApplicationContext());
	}
	
	public static boolean canAccessibilityBeUsed(boolean serviceDecision)
	{
		boolean forceAccessibility = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.FORCE_ACCESSIBILITY, true);
		if(!forceAccessibility)
		{
			return false;
		}
		boolean accessibilityDisabled = !isAccessibilityEnabled(HikeMessengerApp.getInstance().getApplicationContext());
		if(!serviceDecision)
		{
			return accessibilityDisabled;
		}
		boolean wantToUseAccessibility = !HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DONT_USE_ACCESSIBILITY, true);
		//dontUseAccessibility is an internal flag, to prevent user from using accessibility service for stickey,
		//even if accessibility is enabled by forceAccessibility flag On
		return  wantToUseAccessibility || accessibilityDisabled;
	}
	
	public static void startOrStopService(boolean jsonChanged)
	{
		boolean sessionLogEnabled = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SESSION_LOG_TRACKING, false);
		boolean startChatHead = shouldRunChatHeadServiceForStickey() && !canAccessibilityBeUsed(true);
		
		if (sessionLogEnabled || startChatHead)
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
			stopService();
		}
		
		if(!startChatHead)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, false);
			HikeAlarmManager.cancelAlarm(HikeMessengerApp.getInstance(), HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE); 
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
	
	public static void setShareEnableForAllApps(boolean enable)
	{
		JSONArray jsonArray;
		try
		{
			jsonArray = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, ""));
		
		for (int i = 0; i < jsonArray.length(); i++)
		{
			JSONObject obj = jsonArray.getJSONObject(i);
			{
				obj.put(HikeConstants.ChatHead.APP_ENABLE, enable);
			}
		}
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.PACKAGE_LIST, jsonArray.toString());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static boolean checkDeviceFunctionality()
	{
		if (Utils.isIceCreamOrHigher() && !Utils.isLollipopMR1OrHigher())
		{
			return true;
		}
		else if(Utils.isLollipopMR1OrHigher())
		{
			Set<String> currentPoll = ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_ALL_RUNNING_PROCESSES);
			return currentPoll != null && !currentPoll.isEmpty() && !(currentPoll.size() == 1 && currentPoll.contains(HikeMessengerApp.getInstance().getPackageName()));
		}
		else
		{
			return false; 
		}
	}
	
	public static String getNameFromNumber(Context context, String number)
	{
		// / number is the phone number
		Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		String[] mPhoneNumberProjection = { PhoneLookup._ID, PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME };
		Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null);
		try
		{
			if (cur.moveToFirst())
			{
				if (cur.getString(cur.getColumnIndex(PhoneLookup.DISPLAY_NAME)) != null)
				{
					return cur.getString(cur.getColumnIndex(PhoneLookup.DISPLAY_NAME));
				}
			}
		}
		finally
		{
			if (cur != null)
				cur.close();
		}
		return null;
	}
	
	public static void postNumberRequest(Context context, String searchNumber)
	{
		final String number = Utils.normalizeNumber(searchNumber,
				HikeMessengerApp.getInstance().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0)
						.getString(HikeMessengerApp.COUNTRY_CODE, HikeConstants.INDIA_COUNTRY_CODE));
		StickyCaller.callCurrentNumber = number;
		String contactName = getNameFromNumber(context, number);
		
		if (contactName != null)
		{
			StickyCaller.showCallerView(number, contactName, StickyCaller.ALREADY_SAVED, AnalyticsConstants.StickyCallerEvents.ALREADY_SAVED);
		}
		else if (HikeSharedPreferenceUtil.getInstance(HikeConstants.CALLER_SHARED_PREF).getData(number, null) != null)
		{
			StickyCaller.showCallerView(number, HikeSharedPreferenceUtil.getInstance(HikeConstants.CALLER_SHARED_PREF).getData(number, null), StickyCaller.SUCCESS, AnalyticsConstants.StickyCallerEvents.CACHE);
		}
		else
		{
			JSONObject json = new JSONObject();
			try
			{
				json.put(HikeConstants.MSISDN, number);
			}
			catch (JSONException e)
			{
	           Logger.d(TAG, "jsonException");
			}
			CallListener callListener = new CallListener();
			RequestToken requestToken = HttpRequests.postNumberAndGetCallerDetails("http://52.76.46.27:5000/hikeCaller", json, callListener, 2000, 1);
			StickyCaller.showCallerView(null, null, StickyCaller.LOADING, null);
			requestToken.execute();
		}
	}	
	
	public static void registerCallReceiver()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_STICKY_CALLER, false)
				&& HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.ACTIVATE_STICKY_CALLER, false))
		{
			Context context = HikeMessengerApp.getInstance();
			if (incomingCallReceiver == null)
			{
				incomingCallReceiver = new IncomingCallReceiver();
				TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
				telephonyManager.listen(incomingCallReceiver, PhoneStateListener.LISTEN_CALL_STATE);
			}
			if (outgoingCallReceiver == null)
			{
				outgoingCallReceiver = new OutgoingCallReceiver();
				IntentFilter intentFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
				context.registerReceiver(outgoingCallReceiver, intentFilter);
			}
		}
	}
	
	public static void unregisterCallReceiver()
	{
		Context context = HikeMessengerApp.getInstance();
		if (incomingCallReceiver != null)
		{
			TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			telephonyManager.listen(incomingCallReceiver, PhoneStateListener.LISTEN_NONE);
			incomingCallReceiver = null;
		}

		if (outgoingCallReceiver != null)
		{
			context.unregisterReceiver(outgoingCallReceiver);
			outgoingCallReceiver = null;
		}

	}

}
