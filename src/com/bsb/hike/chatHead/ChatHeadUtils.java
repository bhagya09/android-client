package com.bsb.hike.chatHead;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.userlogs.PhoneSpecUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPUtils.CallSource;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
	
	private static ClipboardListener clipboardListener;

	public static final int NO_OF_HTTP_CALL_RETRY = 3;

	public static final int HTTP_CALL_RETRY_DELAY = 2000;
	
	public static final int HTTP_CALL_RETRY_MULTIPLIER = 1;
		
	// replica of hidden constant ActivityManager.PROCESS_STATE_TOP 
	public static final int PROCESS_STATE_TOP =2;

	private static final int MIN_SMS_MSISDN_LENGTH = 10;

	private static final int MAX_SMS_MSISDN_LENGTH = 13;

	private static ChatHeadViewManager viewManager;
	
	private static final int CHAT_HEAD_DISMISS_COUNT = 3;
	
	private static final int CHAT_HEAD_STICKERS_PER_DAY = 5;
	
	private static final int CHAT_HEAD_EXTRA_STICKERS_PER_DAY = 0;
	
	private static final boolean CHAT_HEAD_ENABLE_DEFAULT = true;
	
	private static final boolean CHAT_HEAD_USR_CONTROL_DEFAULT = true;

	private static final String NAME_JSON_FIELD = "n";

	private static final String GENDER_JSON_FIELD = "g";

	private static final String PARAMS = "params";

	private static final long ONE_MONTH = 30 * 24 * 60 * 60 * 1000l;

	private static final long ONE_DAY = 24 * 60 * 60 * 1000l;

	public static String msisdn;

	public static final int VALUE_FALSE = 0;

	public static final int VALUE_TRUE = 1;

	public static boolean blockedCallerFromServerToClientFetched = false;

	public static boolean syncedCallerBlockedFromClientToServer = false;

	private static final String CHAT_HEAD_SHARABLE_PACKAGES = "["
			+ "{\"a\":\"Whatsapp\",\"p\":\"com.whatsapp\"},"
			+ "{\"a\":\"Viber\",\"p\":\"com.viber.voip\"},"
			+ "{\"a\":\"Messenger\",\"p\":\"com.facebook.orca\"},"
			+ "{\"a\":\"Line\",\"p\":\"jp.naver.line.android\"},"
			+ "{\"a\":\"Wechat\",\"p\":\"com.tencent.mm\"},"
			+ "{\"a\":\" Telegram\",\"p\":\"org.telegram.messenger\"}"
			+ "]";

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

	
	public static String getdateFromSystemTime()
	{
	    SimpleDateFormat formatter = new SimpleDateFormat(" 'on' MMM dd 'at' hh:mm aaa");
	    Date resultdate = new Date(System.currentTimeMillis());
	    return formatter.format(resultdate).replace("am", "AM").replace("pm", "PM");
	}

	public static String getValidNumber(String number)
	{
		String regex = "^(\\s*\\+?(\\d{1,3}\\s?\\-?){3,6}\\s*)$";

		String validNumber = "";

		if (number == null || !number.matches(regex))
		{
			return null;
		}

		for (int var = 0; var < number.length(); var++)
		{
			if (Character.isDigit(number.charAt(var)) || (number.charAt(var) == '+'))
			{
				validNumber = validNumber + number.charAt(var);
			}
		}
		return validNumber;
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
	
	public static CallerContentModel getCallerContentModelObject(String result)
	{
		if (result != null)
		{
			try
			{
				JsonParser parser = new JsonParser();
				JsonObject callerDetails = (JsonObject) parser.parse(result);
				CallerContentModel callerContentModel = new Gson().fromJson(callerDetails, CallerContentModel.class);
				return callerContentModel;
			}
			catch (Exception e)
			{
				Logger.d(TAG, "Exception" + e);
				JSONObject metadata = new JSONObject();
				try
				{
					metadata.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.StickyCallerEvents.STICKY_CALLER);
					metadata.put(HikeConstants.EVENT_KEY, AnalyticsConstants.StickyCallerEvents.WRONG_JSON);
					metadata.put(AnalyticsConstants.StickyCallerEvents.WRONG_JSON, result);
					HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ERROR_EVENT, EventPriority.HIGH, metadata);
				}
				catch (JSONException e1)
				{
					Logger.d(TAG, "Failure while making anaytics JSON for wrong JSON syntax");
				}
			}
		}
		return null;
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
		if (!isSnoozed || sessionLogEnabled)
		{
			HikeMessengerApp.getInstance().startService(new Intent(HikeMessengerApp.getInstance(), ChatHeadService.class));
		}
	}

	public static void stopService()
	{
		HikeMessengerApp.getInstance().stopService(new Intent(HikeMessengerApp.getInstance(), ChatHeadService.class));
	}

	private static void restartService()
	{
		stopService();
		startService();
	}

	public static boolean shouldRunChatHeadServiceForStickey()
	{
		boolean enabledForUser = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.ENABLE, false);
		boolean permittedToRun = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.USER_CONTROL, false);
		boolean packageListNonEmpty = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, null) != null;
		boolean notSnoozed = !HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SNOOZE, false);
		return enabledForUser && permittedToRun && packageListNonEmpty && notSnoozed;
	}
	
	public static boolean shouldShowAccessibility()
	{
		boolean showAccessibility = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SHOW_ACCESSIBILITY, !willPollingWork());
		if(!showAccessibility)
		{
			return false;
		}
		return  !isAccessibilityEnabled(HikeMessengerApp.getInstance().getApplicationContext());
	}
	
	public static boolean useOfAccessibilittyPermitted()
	{
		return !HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DONT_USE_ACCESSIBILITY, willPollingWork());
	}
	
	public static boolean isAccessibilityForcedUponUser()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.FORCE_ACCESSIBILITY, !willPollingWork());
	}
	
	public static boolean accessibilityMustBeActivated(boolean isAccessibilityActive)
	{
		return isAccessibilityForcedUponUser() && !isAccessibilityActive; 
	}
	
	public static void startOrStopService(final boolean jsonChanged)
	{
		Context context  = HikeMessengerApp.getInstance().getApplicationContext();
		final boolean sessionLogEnabled = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SESSION_LOG_TRACKING, false);
		final boolean serverEndAccessibilityPermitted = useOfAccessibilittyPermitted();
		final boolean canAccessibilityBeUsed = isAccessibilityForcedUponUser() && ( serverEndAccessibilityPermitted || !isAccessibilityEnabled(context));
		final boolean startChatHead = shouldRunChatHeadServiceForStickey() && !canAccessibilityBeUsed;
		
		Handler uiHandler = new Handler(Looper.getMainLooper());
		
		if(viewManager == null)
		{
			viewManager = ChatHeadViewManager.getInstance(context);
		}
		
		if(jsonChanged || serverEndAccessibilityPermitted)
		{
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					viewManager.onDestroy();
				}
			});
		}
		
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				if (willPollingWork() && (sessionLogEnabled || startChatHead)) {
					if (jsonChanged) {
						restartService();
					} else {
						startService();
					}
				} else {
					stopService();
				}
			}
		});

		if (serverEndAccessibilityPermitted)
		{
			uiHandler.post(new Runnable()
			{
				
				@Override
				public void run()
				{
					viewManager.onCreate();
				}
			});
			
		}

	}

	public static void onClickSetAlarm(Context context, int time)
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, true);
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DONT_USE_ACCESSIBILITY, willPollingWork()))
		{
			ChatHeadViewManager.getInstance(context).onDestroy();
		}
		HikeAlarmManager.setAlarm(context, Calendar.getInstance().getTimeInMillis() + time, HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE, false);
		ChatHeadViewManager.getInstance(context).resetPosition(ChatHeadConstants.STOPPING_SERVICE_ANIMATION, null);
	}
	
	public static void setAllApps(JSONArray pkgList, boolean valueToSet, boolean userDefinedValuePreferred) throws JSONException
	{
		JSONArray newPkgList = new JSONArray();
		JSONArray storedPkgList = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, "[]"));
		boolean storedPkgFound;
		for (int j = 0; j < pkgList.length(); j++)
		{
			storedPkgFound = false;
			JSONObject newPkg = pkgList.getJSONObject(j);
			for (int i = 0; i< storedPkgList.length(); i++)
			{
				JSONObject storedPkg = storedPkgList.getJSONObject(i);
				if(storedPkg.getString(HikeConstants.ChatHead.PACKAGE_NAME).equals(newPkg.getString(HikeConstants.ChatHead.PACKAGE_NAME)))
				{
					storedPkgFound = true;
					newPkg.put(HikeConstants.ChatHead.APP_ENABLE, storedPkg.getBoolean(HikeConstants.ChatHead.APP_ENABLE));
				}
			}
			if(!storedPkgFound || !userDefinedValuePreferred)
			{
				newPkg.put(HikeConstants.ChatHead.APP_ENABLE, valueToSet);
			}
			newPkgList.put(newPkg);
		}
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.PACKAGE_LIST, newPkgList.toString());

	}
	
	public static void activateChatHead(JSONObject data) throws JSONException
	{
		JSONObject stkrWdgtJson;
		HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
		
		if(data == null || !data.has(HikeConstants.ChatHead.STICKER_WIDGET))
		{
			stkrWdgtJson = new JSONObject().put(HikeConstants.ChatHead.USER_CONTROL, CHAT_HEAD_USR_CONTROL_DEFAULT);
		}
		else
		{
			stkrWdgtJson = data.getJSONObject(HikeConstants.ChatHead.STICKER_WIDGET);
		}

		boolean chatHeadEnabled = settings.getData(HikeConstants.ChatHead.ENABLE, CHAT_HEAD_ENABLE_DEFAULT);
		if(stkrWdgtJson.has(HikeConstants.ChatHead.ENABLE) || !settings.contains(HikeConstants.ChatHead.ENABLE))
		{
			chatHeadEnabled = stkrWdgtJson.optBoolean(HikeConstants.ChatHead.ENABLE, chatHeadEnabled);
			settings.saveData(HikeConstants.ChatHead.ENABLE, chatHeadEnabled);
		}
		
		boolean userEnabled = settings.getData(HikeConstants.ChatHead.USER_CONTROL, CHAT_HEAD_USR_CONTROL_DEFAULT);
		if (stkrWdgtJson.has(HikeConstants.ChatHead.USER_CONTROL) && !settings.contains(HikeConstants.ChatHead.USER_CONTROL))
		{
			userEnabled = stkrWdgtJson.optBoolean(HikeConstants.ChatHead.USER_CONTROL, userEnabled);
			settings.saveData(HikeConstants.ChatHead.USER_CONTROL, userEnabled);
		}

		if(chatHeadEnabled)
		{
			boolean forceAccessibility = stkrWdgtJson.optBoolean(HikeConstants.ChatHead.FORCE_ACCESSIBILITY, !ChatHeadUtils.willPollingWork());
			settings.saveData(HikeConstants.ChatHead.FORCE_ACCESSIBILITY, forceAccessibility);

			boolean showAccessibility = stkrWdgtJson.optBoolean(HikeConstants.ChatHead.SHOW_ACCESSIBILITY, !ChatHeadUtils.willPollingWork());
			settings.saveData(HikeConstants.ChatHead.SHOW_ACCESSIBILITY, showAccessibility);

			boolean dontUseAccessibility = stkrWdgtJson.optBoolean(HikeConstants.ChatHead.DONT_USE_ACCESSIBILITY, ChatHeadUtils.willPollingWork());
			settings.saveData(HikeConstants.ChatHead.DONT_USE_ACCESSIBILITY, dontUseAccessibility);
		}

		JSONArray sharablePackageList;
		if (stkrWdgtJson.has(HikeConstants.ChatHead.PACKAGE_LIST))
		{ 
			sharablePackageList = stkrWdgtJson.optJSONArray(HikeConstants.ChatHead.PACKAGE_LIST);
		}
		else
		{
			sharablePackageList = new JSONArray(settings.getData(HikeConstants.ChatHead.PACKAGE_LIST, CHAT_HEAD_SHARABLE_PACKAGES));
		}
		if(stkrWdgtJson.has(HikeConstants.ChatHead.PACKAGE_LIST) || !settings.contains(HikeConstants.ChatHead.PACKAGE_LIST))
		{
			ChatHeadUtils.setAllApps(sharablePackageList, userEnabled, true);
		}

		if (stkrWdgtJson.has(HikeConstants.ChatHead.STICKERS_PER_DAY) || !settings.contains(HikeConstants.ChatHead.STICKERS_PER_DAY))
		{
		    int stickersPerDay = stkrWdgtJson.optInt(HikeConstants.ChatHead.STICKERS_PER_DAY, CHAT_HEAD_STICKERS_PER_DAY);
			settings.saveData(HikeConstants.ChatHead.STICKERS_PER_DAY, stickersPerDay);
		}
		if (stkrWdgtJson.has(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY) || !settings.contains(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY))
		{
			int extraStickersPerDay = stkrWdgtJson.optInt(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, CHAT_HEAD_EXTRA_STICKERS_PER_DAY);
			ChatHeadUtils.settingDailySharedPref();
			settings.saveData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, extraStickersPerDay);
		}
		
		if (stkrWdgtJson.has(HikeConstants.ChatHead.DISMISS_COUNT) || !settings.contains(HikeConstants.ChatHead.DISMISS_COUNT))
		{	
			int dismissCount = stkrWdgtJson.optInt(HikeConstants.ChatHead.DISMISS_COUNT, CHAT_HEAD_DISMISS_COUNT);
			settings.saveData(HikeConstants.ChatHead.DISMISS_COUNT, dismissCount);
		}
		ChatHeadUtils.startOrStopService(true);
	
	}

	public static boolean willPollingWork()
	{
		Set<String> currentPoll = ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_ALL_RUNNING_PROCESSES);
		return currentPoll != null && !currentPoll.isEmpty() && !(currentPoll.size() == 1 && currentPoll.contains(HikeMessengerApp.getInstance().getPackageName()));
	}

	public static boolean checkDeviceFunctionality()
	{
		return Utils.isIceCreamOrHigher();
	}

	public static String getNameFromNumber(Context context, String number)
	{
		if (number != null)
		{
			Uri lookupUriName = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
			String[] mPhoneNumberProjection = { PhoneLookup.DISPLAY_NAME };
			Cursor cur = null;
			String name = null;
			String address = null;
			try
			{
				cur = context.getContentResolver().query(lookupUriName, mPhoneNumberProjection, null, null, null);
				if (cur.moveToFirst())
				{
					if (cur.getString(cur.getColumnIndex(PhoneLookup.DISPLAY_NAME)) != null)
					{
						name = cur.getString(cur.getColumnIndex(PhoneLookup.DISPLAY_NAME));
					}
				}
			}
			catch (Exception e)
			{
				Logger.d("Caller", "getNameException");
			}
			finally
			{
				if (cur != null)
					cur.close();
			}
			if (name != null)
			{
				return name;
			}
		}
		return null;
	}


	private static boolean isNameValid(CallerContentModel callerContentModel)
	{
		if (callerContentModel!= null && !TextUtils.isEmpty(callerContentModel.getMsisdn()) && !TextUtils.isEmpty(callerContentModel.getFullName()))
		{
			return !(callerContentModel.getMsisdn().replaceAll("\\s+","").equalsIgnoreCase(callerContentModel.getFullName().replaceAll("\\s+","")));
		}
		return false;
	}

	private static void callerServerCall(String number, boolean isUpdate, CallerContentModel callerContentModel)
	{
		JSONObject json = new JSONObject();
		IRequestListener iRequestListener;

		if (callerContentModel != null)
		{
			if (isUpdate)
			{

				if (System.currentTimeMillis() - callerContentModel.getUpdationTime() < ONE_DAY)
				{
					return;
				}

				if (((System.currentTimeMillis() - callerContentModel.getCreationTime() < ONE_MONTH) && isNameValid(callerContentModel))
						|| (ChatHeadUtils.getNameFromNumber(HikeMessengerApp.getInstance().getApplicationContext(), callerContentModel.getMsisdn()) != null))
				{
					try
					{
						JSONObject params = new JSONObject();
						params.put(NAME_JSON_FIELD, 0);
						params.put(GENDER_JSON_FIELD, 0);
						json.put(PARAMS, params);
					}
					catch (JSONException e)
					{
						Logger.d("StickyCaller", "JSONException server Request");
					}
				}
			}
		}
		try
		{
			json.put(HikeConstants.MSISDN, number);
		}
		catch (JSONException e)
		{
			Logger.d(TAG, "jsonException");
		}
		if (isUpdate)
		{
			iRequestListener = new UpdateCallListener();
		}
		else
		{
			iRequestListener = new CallListener();
		}
		RequestToken requestToken = HttpRequests.postCallerMsisdn(HttpRequestConstants.getHikeCallerUrl(), json, iRequestListener, StickyCaller.THREE_RETRIES, HTTP_CALL_RETRY_DELAY,
				HTTP_CALL_RETRY_MULTIPLIER, true);
		requestToken.execute();
	}

	public static void postNumberRequest(Context context, String searchNumber)
	{
		String number = null;
		if (!TextUtils.isEmpty(searchNumber))
		{
			 number = getValidNumber(Utils.normalizeNumber(searchNumber, HikeMessengerApp.getInstance().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0)
					.getString(HikeMessengerApp.COUNTRY_CODE, HikeConstants.INDIA_COUNTRY_CODE)));

		if (!TextUtils.isEmpty(number))
		{
			//removing caller view as old caller view must be removed when new caller card request is initiated
			StickyCaller.removeCallerView();
			msisdn = number;
			CallerContentModel callerContentModel = ContactManager.getInstance().getCallerContentModelFromMsisdn(number);
			if (callerContentModel != null && callerContentModel.isBlock())
			{
				if (StickyCaller.CALL_TYPE == StickyCaller.INCOMING || StickyCaller.CALL_TYPE == StickyCaller.MISSED)
				{
					Utils.killCall();
					StickyCaller.CALL_TYPE = StickyCaller.NONE;
					return;
				}
			}
			String contactName = getNameFromNumber(context, number);
			if (contactName != null)
			{
				if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.ENABLE_KNOWN_NUMBER_CARD_PREF, true))
				{
					if (callerContentModel != null)
					{
						callerContentModel.setFullName(contactName);
					}
					else
					{
						callerContentModel = new CallerContentModel();
						callerContentModel.setFullName(contactName);
						callerContentModel.setMsisdn(number);
						callerContentModel.setIsOnHike(Utils.isOnHike(number));
						ContactManager.getInstance().insertIntoCallerTable(callerContentModel, false, false);
					}
					callerServerCall(number, true, callerContentModel);
					StickyCaller.showCallerViewWithDelay(number, callerContentModel, StickyCaller.ALREADY_SAVED, AnalyticsConstants.StickyCallerEvents.ALREADY_SAVED);
				}
			}
			else
			{
				if (callerContentModel != null)
				{
					callerServerCall(number, true, callerContentModel);
					StickyCaller.showCallerViewWithDelay(number, callerContentModel, StickyCaller.SUCCESS, AnalyticsConstants.StickyCallerEvents.CACHE);
				}
				else
				{
					callerServerCall(number, false, callerContentModel);
				}
			}
		}
		}
	}
	
	public static void registerCallReceiver()
	{
		final Context context = HikeMessengerApp.getInstance().getApplicationContext();
		if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_STICKY_CALLER, false)
				&& PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.ACTIVATE_STICKY_CALLER_PREF, false))
		{
			registerOrUnregisterClipboardListener(context);

			Handler uiHandler = new Handler(HikeMessengerApp.getInstance().getApplicationContext().getMainLooper());
			if (uiHandler != null)
			{
				uiHandler.post(new Runnable()
				{
					// putting code inside runnable to make it run on UI thread.
					@Override
					public void run()
					{
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
				});
			}
		}
	}


	public static void registerOrUnregisterClipboardListener(final Context context)
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.ENABLE_CLIPBOARD_CARD, true))
		{
			Handler uiHandler = new Handler(HikeMessengerApp.getInstance().getApplicationContext().getMainLooper());
			if (uiHandler != null)
			{
				uiHandler.post(new Runnable()
				{
					// putting code inside runnable to make it run on UI thread.
					@Override
					public void run()
					{

						if (clipboardListener == null)
						{
							clipboardListener = new ClipboardListener();
							ClipboardManager clipBoard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
							clipBoard.addPrimaryClipChangedListener(clipboardListener);
						}
					}
				});
			}
		}
		else
		{
			unregisterClipboardListener(context);
		}

	}

	public static void unregisterClipboardListener(final Context context)
	{
		Handler uiHandler = new Handler(HikeMessengerApp.getInstance().getApplicationContext().getMainLooper());
		if (uiHandler != null)
		{
			uiHandler.post(new Runnable()
			{
				// putting code inside runnable to make it run on UI thread.
				@Override
				public void run()
				{
					if (clipboardListener != null)
					{
						ClipboardManager clipBoard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
						clipBoard.removePrimaryClipChangedListener(clipboardListener);
						clipboardListener = null;
					}
				}
			});
		}
	}

	public static void unregisterCallReceiver()
	{
		final Context context = HikeMessengerApp.getInstance();

		unregisterClipboardListener(context);
		
		Handler uiHandler = new Handler(HikeMessengerApp.getInstance().getApplicationContext().getMainLooper());
		if (uiHandler != null)
		{
			uiHandler.post(new Runnable()
			{
				// putting code inside runnable to make it run on UI thread.
				@Override
				public void run()
				{

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
					StickyCaller.removeCallerView();
				}
			});
		}
	}

	public static void onCallClickedFromCallerCard(Context context, String callCurrentNumber, CallSource hikeStickyCaller)
	{
		CallerContentModel callerContentModel = ContactManager.getInstance().getCallerContentModelFromMsisdn(callCurrentNumber);
		String callerName = callCurrentNumber;
		if (callerContentModel != null && callerContentModel.getFullName() != null)
		{
			callerName = callerContentModel.getFullName();
		}
		if (callerContentModel != null && callerContentModel.getIsOnHike())
		{
			Utils.onCallClicked(context, callCurrentNumber, hikeStickyCaller);
		}
		else
		{
			Toast.makeText(context, String.format(context.getString(R.string.caller_invited_to_join), callerName), Toast.LENGTH_SHORT).show();
			Utils.sendInvite(callCurrentNumber, context);
		}
	}
	
	public static void insertHomeActivitBeforeStarting(Intent openingIntent)
	{
		Context context = HikeMessengerApp.getInstance().getApplicationContext();
		// Any activity which is being opened from the Sticker Chat Head will open Homeactivity on BackPress
		// this is being done to prevent loss of BG packet sent by the app to server when we exit from the activity
		// its also a product call to take user inside hike after exploring stickers deeply
		// This code may be removed in case some better strategy replaces the FSM to handle FG-BG-lastseen use cases
		TaskStackBuilder.create(context)
			.addNextIntent(IntentFactory.getHomeActivityIntentAsLauncher(context))
			.addNextIntent(openingIntent).startActivities();
	}

	public static void showCallerCard(String number)
	{
		Context context = HikeMessengerApp.getInstance().getApplicationContext();
		if (HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_STICKY_CALLER, false)
				&& PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.ACTIVATE_STICKY_CALLER_PREF, false)
				&& HikeSharedPreferenceUtil.getInstance().getData(StickyCaller.SHOW_SMS_CARD_PREF, false)
				&& PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.SMS_CARD_ENABLE_PREF, false)
				&& number != null && number.length() >= MIN_SMS_MSISDN_LENGTH && number.length() <= MAX_SMS_MSISDN_LENGTH)
		{
			StickyCaller.CALL_TYPE = StickyCaller.SMS;
			postNumberRequest(context, number);
		}
	}

	public static void syncFromClientToServer()
	{
		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				Logger.d(TAG, "syncedCallerBlockedFromClientToServer "+ syncedCallerBlockedFromClientToServer);
				if (!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CALLER_BLOKED_LIST_SYNCHED, false) && !syncedCallerBlockedFromClientToServer)
				{
					JSONArray blockedArray = new JSONArray();
					JSONArray unBlockedArray = new JSONArray();

					Cursor cursor = ContactManager.getInstance().getAllUnsyncedContactCursor();
					if (cursor != null)
					{
						if (cursor.moveToFirst())
						{
							while (!cursor.isAfterLast())
							{
								if (cursor.getInt(cursor.getColumnIndex(DBConstants.HIKE_USER.IS_BLOCK)) == ChatHeadUtils.VALUE_TRUE)
								{
									blockedArray.put(cursor.getString(cursor.getColumnIndex(DBConstants.MSISDN)));
								}
								else
								{
									unBlockedArray.put(cursor.getString(cursor.getColumnIndex(DBConstants.MSISDN)));
								}
								cursor.moveToNext();
							}
						}
						else
						{
							HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.CALLER_BLOKED_LIST_SYNCHED, true);
							return;
						}
					}
					try
					{
						JSONObject callerSyncJson = new JSONObject();
						if (blockedArray.length() != 0)
						{
							callerSyncJson.put(StickyCaller.BLOCKED_LIST, blockedArray);
						}
						if (unBlockedArray.length() != 0)
						{
							callerSyncJson.put(StickyCaller.UNBLOCKED_LIST, unBlockedArray);
						}
						if (blockedArray.length() != 0 || unBlockedArray.length() != 0)
						{
							IBlockRequestListener blockListener = new IBlockRequestListener(callerSyncJson);
							RequestToken requestToken = HttpRequests.postCallerMsisdn(HttpRequestConstants.getHikeCallerBlockUrl(), callerSyncJson, blockListener, StickyCaller.ONE_RETRY,
									ChatHeadUtils.HTTP_CALL_RETRY_DELAY, ChatHeadUtils.HTTP_CALL_RETRY_MULTIPLIER, false);
							if (!requestToken.isRequestRunning())
							{
								requestToken.execute();
							}
						}
						Logger.d("callerSyncJson", callerSyncJson.toString());
					}
					catch (JSONException e)
					{
						Logger.d(TAG, "not able to create sync json");
					}
					syncedCallerBlockedFromClientToServer = true;
				}
			}
		});

	}

	public static void syncAllCallerBlockedContacts()
	{
		HikeHandlerUtil.getInstance().postRunnable(new Runnable()
		{
			@Override
			public void run()
			{
				Logger.d(TAG, "blockedCallerFromServerToClientFetched "+ blockedCallerFromServerToClientFetched);
				if (!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.CALLER_BLOKED_LIST_SYNCHED_SIGNUP, false) && !blockedCallerFromServerToClientFetched)
				{
					ICallerSignUpRequestListener callerSignUpListener = new ICallerSignUpRequestListener();
					RequestToken requestToken = HttpRequests.getBlockedCallerList(HttpRequestConstants.getBlockedCallerListUrl(), callerSignUpListener, StickyCaller.ONE_RETRY,
							HikePlatformConstants.RETRY_DELAY, HikePlatformConstants.BACK_OFF_MULTIPLIER);
					requestToken.execute();
					blockedCallerFromServerToClientFetched = true;
				}
			}
		});
	}

	public static void makeHttpCallToSpamUnspamChatUser(final Context context, final String msisdn, final int markSpam)
	{
		JSONObject spamUserJSONObject = new JSONObject();
		try
		{
			spamUserJSONObject.put(HikeConstants.MSISDN, msisdn);
			spamUserJSONObject.put(HikeConstants.SPAM, markSpam);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		IRequestListener spamUserRequestListener = new IRequestListener() {

			@Override
			public void onRequestFailure(HttpException httpException) {
				Toast.makeText(context, R.string.spam_call_fail, Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onRequestSuccess(Response result)
			{
				ContactManager.getInstance().toggleChatSpam(msisdn, markSpam);
			}

			@Override
			public void onRequestProgressUpdate(float progress) {

			}
		};

		RequestToken token = HttpRequests.toggleChatSpamUser(spamUserJSONObject, spamUserRequestListener, StickyCaller.ONE_RETRY,
				HikePlatformConstants.RETRY_DELAY, HikePlatformConstants.BACK_OFF_MULTIPLIER);
		if(token != null && !token.isRequestRunning())
		{
			token.execute();
		}
		else
		{
			Logger.d(TAG, "As mark as spam task already running, so not starting new one");
		}
	}

	public static CallerContentModel getCallerContentModelFormIntent(Intent intent)
	{
		CallerContentModel callerContentModel = null;
		if(intent != null)
		{
			callerContentModel = intent.getParcelableExtra(HikeConstants.Extras.CALLER_CONTENT_MODEL);
		}
		return callerContentModel;
	}

	public static CallerContentModel getUpdatedCallerContentModelFromResponse(CallerContentModel callerContentModel, JSONObject result, String msisdn)
	{
		try
		{
			if(result != null)
			{
				if(callerContentModel == null)
				{
					callerContentModel = new CallerContentModel();
					callerContentModel.setMsisdn(msisdn);
				}

				callerContentModel.setFullName(result.optString(HikeConstants.NAME));
				callerContentModel.setLocation(result.optString(HikeConstants.LOCATION));
				CallerMetadata md = new CallerMetadata(null);
				md.setIsUserSpammedByYou(result.optBoolean(HikeConstants.IS_USER_CHAT_SPAMMED_BY_YOU) == true ? 1 : 0);
				md.setChatSpamCount(result.optInt(HikeConstants.CHAT_SPAM_COUNT));
				callerContentModel.setCallerMetadata(md);
			}

		}
		catch (JSONException ex)
		{
			ex.printStackTrace();
		}
		return callerContentModel;
	}

	/**
	 * Checks String is NOt valid
	 * 1. Empty/Null
	 * 2. contains only Number : + is optional, contains only number and digint count is (10, 12)
	 *
	 * @param fullName
	 * @return
	 */
	public static boolean isFullNameValid(String fullName)
	{
		boolean valid = true;

		if(TextUtils.isEmpty(fullName) || fullName.trim().matches(HikeConstants.VALID_MSISDN_REGEX))
		{
			valid = false;
		}
		return valid;
	}
}
