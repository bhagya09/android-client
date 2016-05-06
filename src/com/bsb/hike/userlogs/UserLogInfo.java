package com.bsb.hike.userlogs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog;
import android.text.TextUtils;

import com.bsb.hike.GCMIntentService;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

public class UserLogInfo {

	public static final int START = 0;
	public static final int STOP = 2;
	public static final int OPERATE = 1;
	
	private static Map<String, Long> foregroundAppsStartTimeMap;

	private static final String HASH_SCHEME = "MD5";
	private static final String TAG = "UserLogInfo";
	public static final String USER_LOG_SHARED_PREFS = "user_log_info";
	
	public static final int CALL_ANALYTICS_FLAG = 1;
	public static final int APP_ANALYTICS_FLAG = 2;	
	public static final int LOCATION_ANALYTICS_FLAG = 4;
	public static final int ADVERTISIND_ID_FLAG = 8;
	public static final int FETCH_LOG_FLAG = 16;
	public static final int PHONE_SPEC = 32;
	public static final int DEVICE_DETAILS = 64;
	public static final int ACCOUNT_ANALYTICS_FLAG = 128;
	
	
	private static final long milliSecInDay = 1000 * 60 * 60 * 24;
	private static final int DAYS_TO_LOG = 30;
	private static final int MAX_CURSOR_LIMIT = 500;

	private static final String MISSED_CALL_COUNT = "m";
	private static final String RECEIVED_CALL_COUNT = "r";
	private static final String SENT_CALL_COUNT = "s";
	private static final String RECEIVED_CALL_DURATION = "rd";
	private static final String SENT_CALL_DURATION = "sd";
	private static final String PHONE_NUMBER = "ph";
	
	private static final String PACKAGE_NAME = "pn";
	private static final String APPLICATION_NAME = "an";
	private static final String INSTALL_TIME = "it";
	private static final String RUNNING_APPS = "ra";
	
	private static final String LATITUDE = "lat";
	private static final String LONGITUDE = "long";
	private static final String RADIUS = "rd";
	private static final String TIMESTAMP = "ts";
	
	private static final String SENT_SMS = "ss";
	private static final String RECEIVED_SMS = "rs";

	private static final String ACCOUNT_TYPE = "act";
	private static final String ACCOUNT_NAME = "acn";
	
	private static final String SESSION_COUNT = "sn";
	private static final String DURATION = "dr";
	private static long MIN_SESSION_RECORD_TIME = 2000;
	
	private final static byte RUNNING_PROCESS_BIT = 0;
	private final static byte FOREGROUND_TASK_BIT = 1;
	
	private static int flags;

	private static HikeHandlerUtil mHikeHandler = HikeHandlerUtil.getInstance();
	
	public static class SessionLogPojo{
		final String packageName;
		final String applicationName;
		final long duration;
		final int sessions;
		
		public SessionLogPojo(String packageName, String applicationName, long duration, int sessions) {
			this.packageName = packageName;
			this.applicationName = applicationName;
			this.sessions = sessions;
			this.duration = duration;
		}
		
		public JSONObject toJSON() throws JSONException{
			JSONObject jsonObj = new JSONObject();
			jsonObj.putOpt(PACKAGE_NAME, this.packageName);
			jsonObj.putOpt(APPLICATION_NAME,this.applicationName);
			jsonObj.putOpt(SESSION_COUNT, this.sessions);
			jsonObj.putOpt(DURATION, this.duration);
			return jsonObj;
		}
	}
	
	public static class LocLogPojo{
		final double latitude;
		final double longitude;
		final float radius;
		final long timeStamp;
		
		public LocLogPojo(double latitude, double longitude, float radius, long timeStamp){
			this.latitude = latitude;
			this.longitude = longitude;
			this.radius = radius;
			this.timeStamp = timeStamp;
		}
		
		public JSONObject toJSON() throws JSONException{
			JSONObject jsonObj = new JSONObject();
			jsonObj.putOpt(LATITUDE, this.latitude);
			jsonObj.putOpt(LONGITUDE,this.longitude);
			jsonObj.putOpt(RADIUS, this.radius);
			jsonObj.putOpt(TIMESTAMP, this.timeStamp);
			return jsonObj;
		}
	}

	public static class AppLogPojo {
		final String packageName;
		final String applicationName;
		final long installTime;
		final int running;

		public AppLogPojo(String packageName, String applicationName, long installTime, int running) {
			this.packageName = packageName;
			this.applicationName = applicationName;
			this.installTime = installTime;
			this.running = running;
		}
		
		public JSONObject toJSON() throws JSONException{
			JSONObject jsonObj = new JSONObject();
			jsonObj.putOpt(PACKAGE_NAME, this.packageName);
			jsonObj.putOpt(APPLICATION_NAME,this.applicationName);
			jsonObj.putOpt(INSTALL_TIME, this.installTime);
			jsonObj.putOpt(RUNNING_APPS, this.running);
			return jsonObj;
		}
		
	}
	
	public static class CallLogPojo {
		final String phoneNumber;
		int missedCallCount;
		int receivedCallCount;
		int sentCallCount;
		int sentCallDuration;
		int receivedCallDuration;
		int sentSmsCount;
		int receivedSmsCount;

		public CallLogPojo(String phoneNumber, int missedCallCount, int receivedCallCount, int sentCallCount, 
				int sentCallDuration, int receivedCallDuration, int sentSmsCount, int receivedSmsCount) {
			this.phoneNumber = phoneNumber;
			this.missedCallCount = missedCallCount;
			this.receivedCallCount = receivedCallCount;
			this.sentCallCount = sentCallCount;
			this.sentCallDuration = sentCallDuration;
			this.receivedCallDuration = receivedCallDuration;
			this.sentSmsCount = sentSmsCount;
			this.receivedSmsCount = receivedSmsCount;
		}
	}

	public static class AccountLogPojo
	{
		private String accountName;
		private String accountType;

		public AccountLogPojo(String accountName, String accountType)
		{
			this.accountName = accountName;
			this.accountType = accountType;
		}

		public JSONObject toJSON() throws JSONException
		{
			JSONObject jsonObj = new JSONObject();
			jsonObj.putOpt(ACCOUNT_NAME, this.accountName);
			jsonObj.putOpt(ACCOUNT_TYPE,this.accountType);
			return jsonObj;
		}
	}

	public static List<AccountLogPojo> getAccountLogs()
	{

		List<AccountLogPojo> accountLogPojos = new ArrayList<AccountLogPojo>();

		Account[] accountList = AccountManager.get(HikeMessengerApp.getInstance().getApplicationContext()).getAccounts();
		if(accountList != null && accountList.length > 0)
		{
			for (Account account : accountList)
			{
				accountLogPojos.add(new AccountLogPojo(account.name, account.type));
				Logger.d(TAG, account.name + " : " + account.type + " : " + account.toString());

			}
		}
		return accountLogPojos;
		
	}

	public static JSONArray getJSONAccountArray(List<AccountLogPojo> accountLogList)
			throws JSONException
	{
		JSONArray jsonArray = new JSONArray();
		for (AccountLogPojo accountLog : accountLogList)
		{
			jsonArray.put(accountLog.toJSON());
		}
		return jsonArray;

	}


	public static List<AppLogPojo> getAppLogs() {
		
		List<AppLogPojo> appLogList = new ArrayList<AppLogPojo>();
		Context ctx = HikeMessengerApp.getInstance().getApplicationContext();
		List<PackageInfo> packInfoList = ctx.getPackageManager().getInstalledPackages(0);
		Set<String> runningPackageNames = ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_ALL_RUNNING_PROCESSES);
		Set<String> currentRunningtasks = ChatHeadUtils.getRunningAppPackage(ChatHeadUtils.GET_FOREGROUND_PROCESSES);
        for(PackageInfo pi : packInfoList){
        	int appStatus = 0;
			if (pi.versionName == null)
				continue;
			if (runningPackageNames.contains(pi.packageName))
			{
				appStatus = PhoneSpecUtils.getNumberAfterSettingBit(appStatus, RUNNING_PROCESS_BIT, true);
				runningPackageNames.remove(pi.packageName);
			}
			if (currentRunningtasks.contains(pi.packageName))
			{
				appStatus = PhoneSpecUtils.getNumberAfterSettingBit(appStatus, FOREGROUND_TASK_BIT, true);
				currentRunningtasks.remove(pi.packageName);
			  }
			AppLogPojo appLog = new AppLogPojo(
					pi.packageName,
					pi.applicationInfo.loadLabel(ctx.getPackageManager()).toString(),
					new File(pi.applicationInfo.sourceDir).lastModified(), appStatus);
			appLogList.add(appLog);
		}
        return appLogList;

	}

	public static JSONArray getJSONAppArray(List<AppLogPojo> appLogList)
			throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (AppLogPojo appLog : appLogList) {
			jsonArray.put(appLog.toJSON());
		}
		return jsonArray;

	}
	
	private static String getLogKey(int flag) {
		String jsonKey = null;
		switch (flag) {
			case (APP_ANALYTICS_FLAG): jsonKey = HikeConstants.APP_LOG_ANALYTICS; break;
			case (ADVERTISIND_ID_FLAG): jsonKey = HikeConstants.ADVERTSING_ID_ANALYTICS; break;
			case (CALL_ANALYTICS_FLAG): jsonKey = HikeConstants.CALL_LOG_ANALYTICS; break;
			case (LOCATION_ANALYTICS_FLAG): jsonKey = HikeConstants.LOCATION_LOG_ANALYTICS; break;
			case (FETCH_LOG_FLAG): jsonKey = HikeConstants.SESSION_LOG_TRACKING; break;
			case (PHONE_SPEC): jsonKey = HikeConstants.PHONE_SPEC; break;
			case (DEVICE_DETAILS): jsonKey = HikeConstants.DEVICE_DETAILS; break;
			case (ACCOUNT_ANALYTICS_FLAG): jsonKey = HikeConstants.ACCOUNT_LOG_ANALYTICS; break;
			
		}
		return jsonKey;
	}

	private static JSONObject getEncryptedJSON(JSONArray jsonLogArray, int flag) throws JSONException {
		
		HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
		String key = settings.getData("pa_uid", null);

		//for the case when AI packet will not send us the backup Token
		String salt = settings.getData("pa_encryption_key", null);
		
		AESEncryption aesObj = new AESEncryption(key + salt, HASH_SCHEME);
		JSONObject jsonLogObj = new JSONObject();
		jsonLogObj.putOpt(getLogKey(flag), aesObj.encrypt(jsonLogArray.toString()));
		Logger.d(TAG, "sending analytics : " + jsonLogObj.toString());
		
		return jsonLogObj;

	}

	private static JSONArray getAdvertisingId() throws JSONException{

		try {
			Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(HikeMessengerApp.getInstance().getApplicationContext());
			return new JSONArray().put(new JSONObject().putOpt(HikeConstants.ADVERTSING_ID_ANALYTICS, adInfo.getId()));
		} catch (IOException e) {
			Logger.d(TAG, "IOException" + e.toString());
		} catch (GooglePlayServicesRepairableException e) {
			Logger.d(TAG, "play service repairable exception" + e.toString());
		} catch (GooglePlayServicesNotAvailableException e) {
			Logger.d(TAG, "play services not found Exception" + e.toString());
		}
		return null;
	}
	
	private static JSONArray getDeviceDetails() throws JSONException
	{
		Context  context = HikeMessengerApp.getInstance().getApplicationContext();
		JSONObject deviceDetails = Utils.getPostDeviceDetails(context);
		deviceDetails.put(GCMIntentService.DEV_TOKEN, GCMRegistrar.getRegistrationId(context));
        deviceDetails.put(HikeConstants.LogEvent.DEVICE_ID, Utils.getDeviceId(context));
        deviceDetails.put(HikeConstants.LogEvent.DPI, Utils.densityDpi);
        deviceDetails.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
        Logger.d("Device Details", deviceDetails.toString());
        return new JSONArray().put(deviceDetails);
	}
	
	
	private static JSONArray collectLogs(int flag) throws JSONException{	
		switch(flag){
			case APP_ANALYTICS_FLAG : return getJSONAppArray(getAppLogs()); 
			case CALL_ANALYTICS_FLAG : return getJSONCallArray(getCallLogs());
			case LOCATION_ANALYTICS_FLAG : return getJSONLocArray(getLocLogs());
			case ADVERTISIND_ID_FLAG : return getAdvertisingId();
			case FETCH_LOG_FLAG : return getJSONLogArray(getLogsFor(HikeConstants.SESSION_LOG_TRACKING));
			case PHONE_SPEC:  return PhoneSpecUtils.getPhoneSpec();
			case DEVICE_DETAILS:  return getDeviceDetails();
			case ACCOUNT_ANALYTICS_FLAG : return getJSONAccountArray(getAccountLogs());
			default : return null;
		}
	}
	
	private static JSONArray getJSONLogArray(List<SessionLogPojo> sessionLogPojo) throws JSONException 
	{
		if (sessionLogPojo == null || sessionLogPojo.isEmpty())
			return null;
		JSONArray sessionJsonArray = new JSONArray();
		for (SessionLogPojo sessionLog : sessionLogPojo)
		{
			sessionJsonArray.put(sessionLog.toJSON());
		}
		Logger.d(TAG, sessionJsonArray.toString());
		return sessionJsonArray;
	}
	
	private static List<SessionLogPojo> getLogsFor(String logType)
	{
		List<SessionLogPojo> sessionLogs = new ArrayList<SessionLogPojo>();
		PackageManager pm = HikeMessengerApp.getInstance().getPackageManager();
		if(logType == HikeConstants.SESSION_LOG_TRACKING)
		{
			HikeSharedPreferenceUtil userPrefs = HikeSharedPreferenceUtil.getInstance(USER_LOG_SHARED_PREFS);
			for (Map.Entry<String, ?> entry : userPrefs.getPref().getAll().entrySet())
			{
				try
				{
					String[] sessionInfo = entry.getValue().toString().split(":");
					ApplicationInfo ai = pm.getApplicationInfo(entry.getKey(), PackageManager.GET_UNINSTALLED_PACKAGES);
					String applicationName = ai.loadLabel(HikeMessengerApp.getInstance().getPackageManager()).toString();
					sessionLogs.add(new SessionLogPojo(entry.getKey(), applicationName, Long.parseLong(sessionInfo[0]), Integer.parseInt(sessionInfo[1])));
					
				} 
				catch (NameNotFoundException e)
				{
					Logger.d(TAG, "Application uninstalled or not found : " + entry.getKey());
				}
				catch (Exception e)
				{
					Logger.d(TAG, "Exception : " + e);
				}
			}
			userPrefs.deleteAllData();
		}
		return sessionLogs;
	}
	
	private static JSONArray getJSONLocArray(List<LocLogPojo> locLogList) throws JSONException{
		//not sending anything if there is no Log available
		if(locLogList == null)
			return null;
		JSONArray locJsonArray = new JSONArray();
		for(LocLogPojo locLog : locLogList){
			locJsonArray.put(locLog.toJSON());
		}
		Logger.d(TAG, locJsonArray.toString());
		return locJsonArray;
	}
	
	private static List<LocLogPojo> getLocLogs(){
		Location bestLocation = Utils.getPassiveLocation();
		if(bestLocation == null)
			return null;
		LocLogPojo locLog = new LocLogPojo(bestLocation.getLatitude(), bestLocation.getLongitude(), 
				bestLocation.getAccuracy(), bestLocation.getTime());
		List<LocLogPojo> locLogList = new ArrayList<LocLogPojo>(1);
		locLogList.add(locLog);
		return locLogList;
	}

	private static boolean isKeysAvailable()
	{
		HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
		String key = settings.getData("pa_uid", null);
		//for the case when AI packet will not send us the backup Token
		String salt = settings.getData("pa_token", null);
		// if salt or key is empty, we do not send anything
		if(TextUtils.isEmpty(salt) || TextUtils.isEmpty(key))
			return false;
		
		return true;
	}
	
	public static void sendLogs(final int flags) throws JSONException
	{

		final JSONArray jsonLogArray = collectLogs(flags);
		// if nothing is logged we do not send anything
		if (jsonLogArray != null)
		{
			final JSONObject jsonLogObj = getEncryptedJSON(jsonLogArray, flags);

			if (jsonLogObj != null)
			{

				scheduleNextSendToServerAction(HikeMessengerApp.LAST_BACK_OFF_TIME_USER_LOGS, new Runnable() {
					@Override
					public void run() {

						IRequestListener requestListener = getRequestListener(getLogKey(flags));
						HikeSharedPreferenceUtil.getInstance().saveData(getLogKey(flags), jsonLogObj.toString());

						RequestToken token = HttpRequests.sendUserLogInfoRequest(getLogKey(flags), jsonLogObj, requestListener);
						token.execute();
					}
				});


			}
		}
	}


	private static IRequestListener getRequestListener(final String logType)
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();
				Logger.d(TAG, response.toString());

				HikeSharedPreferenceUtil.getInstance().removeData(logType);

				HikeSharedPreferenceUtil.getInstance().removeData(HikeMessengerApp.LAST_BACK_OFF_TIME_USER_LOGS);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(final HttpException httpException)
			{

				scheduleNextSendToServerAction(HikeMessengerApp.LAST_BACK_OFF_TIME_USER_LOGS, new Runnable() {
					@Override
					public void run() {

							IRequestListener requestListener = getRequestListener(logType);
							String encryptedJsonString = HikeSharedPreferenceUtil.getInstance().getData(logType, "");

							if (!TextUtils.isEmpty(encryptedJsonString)) {

								try {
									JSONObject jsonLogObject = null;
									jsonLogObject = new JSONObject(encryptedJsonString);

									RequestToken token = HttpRequests.sendUserLogInfoRequest(logType, jsonLogObject, requestListener);
									token.execute();
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}



					}
				});
				Logger.d(TAG, "failure : " + logType);
			}
		};
	};

	private static void scheduleNextSendToServerAction(String lastBackOffTimePref, Runnable postRunnableReference)
	{

		HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance();
		Logger.d(TAG, "Scheduling next " + lastBackOffTimePref + " send");

		int lastBackOffTime = mprefs.getData(lastBackOffTimePref, 0);

		lastBackOffTime = lastBackOffTime == 0 ? 2 : (lastBackOffTime * 2);
		lastBackOffTime = Math.min(20, lastBackOffTime);

		Logger.d(TAG, "Scheduling the next disconnect");

		mHikeHandler.removeRunnable(postRunnableReference);
		mHikeHandler.postRunnableWithDelay(postRunnableReference, lastBackOffTime * 1000);
		mprefs.saveData(lastBackOffTimePref, lastBackOffTime);
	}

	public static void requestUserLogs(final int flags) throws JSONException
	{


		Runnable rn  = new Runnable()
		{
			@Override
			public void run()
			{
				for(int counter = 0; counter<Integer.SIZE;counter ++)
				{
					try {
						sendLogs((1 << counter) & flags);
					} catch (JSONException e) {
						Logger.d(TAG, "JSON exception in making Logs" + e);
					}

				}

			}
		};

		HikeHandlerUtil.getInstance().postRunnableWithDelay(rn, 0);

	}

	public static void requestUserLogs(JSONObject data) throws JSONException {
		
		flags = 0;
		
		if(data.optBoolean(HikeConstants.CALL_LOG_ANALYTICS))
		{
			flags |= CALL_ANALYTICS_FLAG; 
		}
		if(data.optBoolean(HikeConstants.LOCATION_LOG_ANALYTICS))
		{
			flags |= LOCATION_ANALYTICS_FLAG;
		}
		if(data.optBoolean(HikeConstants.APP_LOG_ANALYTICS))
		{
			flags |= UserLogInfo.APP_ANALYTICS_FLAG;
		}
		if(data.optBoolean(HikeConstants.ADVERTSING_ID_ANALYTICS))
		{
			flags |= UserLogInfo.ADVERTISIND_ID_FLAG;
		}
		if(data.optBoolean(HikeConstants.FETCH_LOG_ANALYTICS))
		{
			//TODO possibly turn this into "gl":true to "gl":"stl"
			flags |= UserLogInfo.FETCH_LOG_FLAG;
		}
		if(data.optBoolean(HikeConstants.PHONE_SPEC))
		{
			flags |= UserLogInfo.PHONE_SPEC;
		}
		if(data.optBoolean(HikeConstants.DEVICE_DETAILS))
		{
			flags |= UserLogInfo.DEVICE_DETAILS;
		}
		if(data.optBoolean(HikeConstants.ACCOUNT_LOG_ANALYTICS))
		{
			flags |= UserLogInfo.ACCOUNT_ANALYTICS_FLAG;
		}
		
		if(flags == 0) 
		{
			return;
		}

		boolean isForceUser = data.optBoolean(HikeConstants.FORCE_USER,false);
		boolean isDeviceRooted=Utils.isDeviceRooted();

		sendAnalytics(isDeviceRooted);
		if ((!isForceUser && isDeviceRooted) || !isKeysAvailable())
		{
			return;
		}

		requestUserLogs(flags);
	}
	
	private static void sendAnalytics(boolean isDeviceRooted)
	{
		JSONObject metaData=new JSONObject();
		try
		{
			metaData.put(HikeConstants.IS_ROOT, String.valueOf(isDeviceRooted));
			HAManager.getInstance().record(AnalyticsConstants.NON_UI_EVENT, HikeConstants.LogEvent.DEVICE_ROOT, EventPriority.HIGH, metaData);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		
	}

	public static List<CallLogPojo> getCallLogs() {
		
		//Map is being used to store and retrieve values multiple times
		Map<String, CallLogPojo> callLogMap = new HashMap<String, CallLogPojo>();
		Context ctx = HikeMessengerApp.getInstance().getApplicationContext();
		
		 Uri smsUri = Uri.parse("content://sms");
         Cursor smsCur = ctx.getContentResolver().query(smsUri, null, null, null, null);
         
         if (smsCur != null && smsCur.moveToFirst()) { 
        	try { 
				do {
					String smsNumber = smsCur.getString(smsCur.getColumnIndexOrThrow("address"));
					String smsDate = smsCur.getString(smsCur.getColumnIndexOrThrow("date"));
					int smsType = smsCur.getInt(smsCur.getColumnIndexOrThrow("type"));
					
					if (Long.parseLong(smsDate) > (System.currentTimeMillis() - (milliSecInDay * DAYS_TO_LOG)) 
							&& smsNumber != null && (smsType == 1 || smsType == 2)) 
					{
	
						CallLogPojo callLog = null;
						
						if(callLogMap.containsKey(smsNumber)){
							callLog = callLogMap.get(smsNumber);
						} else {
							callLog = new CallLogPojo(smsNumber,0,0,0,0,0,0,0);
						}
		
						switch (smsType) {
						case 1 : //inbox
							callLog.receivedSmsCount++;
							break;
						case 2 : //sent
							callLog.sentSmsCount++;
							break;
						}
						callLogMap.put(smsNumber, callLog);
					}
				} while (smsCur.moveToNext());
        	} catch (Exception e) {
				Logger.d(TAG, e.toString());
			} finally {
				smsCur.close();
			}
 		}
		
		String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
		String[] projection = new String[] { CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.DURATION };
		String selection = CallLog.Calls.DATE + " > ?";
		String[] selectors = new String[] { String.valueOf(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 30)) };
		Uri callUri = CallLog.Calls.CONTENT_URI;
		Uri callUriLimited = callUri.buildUpon()
				.appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, String.valueOf(MAX_CURSOR_LIMIT))
				.build();
		
		ContentResolver cr = ctx.getContentResolver();
		Cursor cur = cr.query(callUriLimited, projection, null, null, strOrder);
		
		if (cur != null) { 
			try {
				while (cur.moveToNext()) {
	
					String callNumber = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.NUMBER));
					String callDate = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.DATE));				
					int duration = cur.getInt(cur.getColumnIndex(android.provider.CallLog.Calls.DURATION));
	
					if (Long.parseLong(callDate) > (System.currentTimeMillis() - (milliSecInDay * DAYS_TO_LOG))) {

						CallLogPojo callLog = null;
						
						if(callLogMap.containsKey(callNumber)){
							callLog = callLogMap.get(callNumber);
						} else {
							callLog = new CallLogPojo(callNumber,0,0,0,0,0,0,0);
						}
	
						switch (cur.getInt(cur.getColumnIndex(android.provider.CallLog.Calls.TYPE))) {
						case CallLog.Calls.MISSED_TYPE : 
							callLog.missedCallCount++;
							break;
						case CallLog.Calls.OUTGOING_TYPE :
							callLog.sentCallCount++;
							callLog.sentCallDuration += duration;
							break;
						case CallLog.Calls.INCOMING_TYPE : 
							callLog.receivedCallCount++;
							callLog.receivedCallDuration += duration;
							break;
	
						}
						callLogMap.put(callNumber, callLog);
					}
	
				}
			} catch (Exception e) {
				Logger.d(TAG, e.toString());
			} finally {
				cur.close();
			}
		}
		
		List<CallLogPojo> callLogList = new ArrayList<CallLogPojo>(callLogMap.size());
		for (Entry<String, CallLogPojo> entry : callLogMap.entrySet()) {
	        callLogList.add(entry.getValue());
	    }
		return callLogList;
		
	}
	
	private static JSONArray getJSONCallArray(List<CallLogPojo> callLogList) throws JSONException {
		JSONArray callJsonArray = new JSONArray();
		for(CallLogPojo callLog : callLogList){
			JSONObject jsonObj = new JSONObject();
			jsonObj.putOpt(PHONE_NUMBER, callLog.phoneNumber);
			jsonObj.putOpt(MISSED_CALL_COUNT, callLog.missedCallCount);
			jsonObj.putOpt(SENT_CALL_COUNT, callLog.sentCallCount);
			jsonObj.putOpt(RECEIVED_CALL_COUNT, callLog.receivedCallCount);
			jsonObj.putOpt(SENT_CALL_DURATION, callLog.sentCallDuration);
			jsonObj.putOpt(RECEIVED_CALL_DURATION, callLog.receivedCallDuration);
			jsonObj.putOpt(SENT_SMS, callLog.sentSmsCount);
			jsonObj.putOpt(RECEIVED_SMS, callLog.receivedSmsCount);
			callJsonArray.put(jsonObj);
		}
		Logger.d(TAG, callJsonArray.toString());
		return callJsonArray;
	}

	public static void recordSessionInfo(Set<String> currentForegroundApps, int nextStep)
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SESSION_LOG_TRACKING, false))
		{
			return;
		}
		KeyguardManager kgMgr = (KeyguardManager) HikeMessengerApp.getInstance().getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
		boolean lockScreenShowing = kgMgr.inKeyguardRestrictedInputMode();
		long currentTime = System.currentTimeMillis();
		if(foregroundAppsStartTimeMap == null)
		{
			foregroundAppsStartTimeMap = new HashMap<String, Long>(5);
		}
		
		Set<String> savedForegroundApps = new HashSet<String>(foregroundAppsStartTimeMap.keySet());
		
		//this if logic can also be called when the activity has already started
		if(nextStep  == START || foregroundAppsStartTimeMap.isEmpty())
		{
			foregroundAppsStartTimeMap.clear();
			for(String packageName : currentForegroundApps)
			{
				if(!lockScreenShowing)
				{
					foregroundAppsStartTimeMap.put(packageName, currentTime);
				}
			}
		}
		else if (nextStep == STOP)
		{
			for(String app : foregroundAppsStartTimeMap.keySet())
			{
				recordASession(app, foregroundAppsStartTimeMap.get(app));
			}
			foregroundAppsStartTimeMap.clear();
		}
		else if(nextStep == OPERATE && !currentForegroundApps.isEmpty())
		{
			savedForegroundApps.addAll(currentForegroundApps);
			for(String app : savedForegroundApps)
			{
				if(currentForegroundApps.contains(app) && !foregroundAppsStartTimeMap.containsKey(app) && !lockScreenShowing)
				{
					// foregrounded app here
					foregroundAppsStartTimeMap.put(app, System.currentTimeMillis());
				}
				else if(!currentForegroundApps.contains(app) && foregroundAppsStartTimeMap.containsKey(app))
				{
					// backgrounded apps here
					recordASession(app, foregroundAppsStartTimeMap.get(app));
					foregroundAppsStartTimeMap.remove(app);
				}
			}
		}
	}
	
	private static void recordASession(String packageName, long sesstionTime)
	{
		long sessionTime = System.currentTimeMillis() - sesstionTime;
		
		if (sessionTime > MIN_SESSION_RECORD_TIME)
		{
			HikeSharedPreferenceUtil userPrefs = HikeSharedPreferenceUtil.getInstance(USER_LOG_SHARED_PREFS);
			String[] loggedParams = userPrefs.getData(packageName, "0:0").split(":");
			for (String s: loggedParams) {
			    //Do your stuff here
			   Logger.d(TAG, s);
			}
			long duration = Long.parseLong(loggedParams[0]) + sessionTime;
			int sessions = Integer.parseInt(loggedParams[1]) + 1;
			userPrefs.saveData(packageName, duration + ":" + sessions);
			Logger.d(TAG, "time : " + sessionTime + " of " + packageName);
		}	
	}
	
}
