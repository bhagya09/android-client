package com.bsb.hike.userlogs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.CallLog;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.maps.UiSettings;


public class UserLogInfo {
	
	public static final int START = 0;
	public static final int STOP = 2;
	public static final int OPERATE = 1;
	
	private static long sessionTimeMillisStart;
	private static String currentForegroundPackage;
	
	private static Set<String> oldForegroundPackages;
	private static Set<String> newForegroundPackages;

	private static final String HASH_SCHEME = "MD5";
	private static final String TAG = "UserLogInfo";
	
	public static final int CALL_ANALYTICS_FLAG = 1;
	public static final int APP_ANALYTICS_FLAG = 2;	
	public static final int LOCATION_ANALYTICS_FLAG = 4;
	public static final int ADVERTISIND_ID_FLAG = 8;
	
	
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
	
	private static final String LATITUDE = "lat";
	private static final String LONGITUDE = "long";
	private static final String RADIUS = "rd";
	private static final String TIMESTAMP = "ts";
	
	private static final String SENT_SMS = "ss";
	private static final String RECEIVED_SMS = "rs";
	
	private static int flags;
	
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

		public AppLogPojo(String packageName, String applicationName, long installTime) {
			this.packageName = packageName;
			this.applicationName = applicationName;
			this.installTime = installTime;
		}
		
		public JSONObject toJSON() throws JSONException{
			JSONObject jsonObj = new JSONObject();
			jsonObj.putOpt(PACKAGE_NAME, this.packageName);
			jsonObj.putOpt(APPLICATION_NAME,this.applicationName);
			jsonObj.putOpt(INSTALL_TIME, this.installTime);
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

	public static List<AppLogPojo> getAppLogs() {
		
		List<AppLogPojo> appLogList = new ArrayList<AppLogPojo>();
		Context ctx = HikeMessengerApp.getInstance().getApplicationContext();
		List<PackageInfo> packInfoList = ctx.getPackageManager().getInstalledPackages(0);
		
		for(PackageInfo pi : packInfoList){
			
			if (pi.versionName == null)
				continue;
			AppLogPojo appLog = new AppLogPojo(
					pi.packageName,
					pi.applicationInfo.loadLabel(ctx.getPackageManager()).toString(),
					new File(pi.applicationInfo.sourceDir).lastModified());
			appLogList.add(appLog);
			
		}
		return appLogList;

	}

	private static JSONArray getJSONAppArray(List<AppLogPojo> appLogList)
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
		}
		return jsonKey;
	}

	private static JSONObject getEncryptedJSON(JSONArray jsonLogArray, int flag) throws JSONException {
		
		HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
		String key = settings.getData(HikeMessengerApp.MSISDN_SETTING, null);
		//for the case when AI packet will not send us the backup Token
		String salt = settings.getData(HikeMessengerApp.BACKUP_TOKEN_SETTING, null);
		
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
	
	private static JSONArray collectLogs(int flag) throws JSONException{	
		switch(flag){
			case APP_ANALYTICS_FLAG : return getJSONAppArray(getAppLogs()); 
			case CALL_ANALYTICS_FLAG : return getJSONCallArray(getCallLogs());
			case LOCATION_ANALYTICS_FLAG : return getJSONLocArray(getLocLogs());
			case ADVERTISIND_ID_FLAG : return getAdvertisingId();
			default : return null;
		}
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
		Location bestLocation = null;
		Context ctx = HikeMessengerApp.getInstance().getApplicationContext();
		LocationManager locManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
		List<String> locProviders = locManager.getProviders(true);
		if (locProviders == null || locProviders.isEmpty())
			return null;
		for(String provider : locManager.getProviders(true)){
			Location location = locManager.getLastKnownLocation(provider);
			if(location == null)
				continue;
			if (bestLocation == null || 
					(location.hasAccuracy() && location.getAccuracy() < bestLocation.getAccuracy())){
				bestLocation = location;
			}
		}
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
		String key = settings.getData(HikeMessengerApp.MSISDN_SETTING, null);
		//for the case when AI packet will not send us the backup Token
		String salt = settings.getData(HikeMessengerApp.BACKUP_TOKEN_SETTING, null);
		// if salt or key is empty, we do not send anything
		if(TextUtils.isEmpty(salt) || TextUtils.isEmpty(key))
			return false;
		
		return true;
	}
	
	public static void sendLogs(int flags) throws JSONException
	{

		JSONArray jsonLogArray = collectLogs(flags);
		// if nothing is logged we do not send anything
		if (jsonLogArray != null)
		{
			JSONObject jsonLogObj = getEncryptedJSON(jsonLogArray, flags);

			if (jsonLogObj != null)
			{
				IRequestListener requestListener = new IRequestListener()
				{
					@Override
					public void onRequestSuccess(Response result)
					{
						JSONObject response = (JSONObject) result.getBody().getContent();
						Logger.d(TAG, response.toString());
					}

					@Override
					public void onRequestProgressUpdate(float progress)
					{
					}

					@Override
					public void onRequestFailure(HttpException httpException)
					{
						Logger.d(TAG, "failure");
					}
				};

				RequestToken token = HttpRequests.sendUserLogInfoRequest(getLogKey(flags), jsonLogObj, requestListener);
				token.execute();
			}
		}
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
		
		if(flags == 0) 
		{
			return;
		}
		
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

		boolean isForceUser = data.optBoolean(HikeConstants.FORCE_USER,false);
		boolean isDeviceRooted=Utils.isDeviceRooted();
		
		sendAnalytics(isDeviceRooted);
		if ((!isForceUser && isDeviceRooted) || !isKeysAvailable()) 
		{
			return;
		}
		HikeHandlerUtil.getInstance().postRunnableWithDelay(rn, 0);
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
		newForegroundPackages = new HashSet<String>(currentForegroundApps);
		long currentTime = System.currentTimeMillis();
		if(oldForegroundPackages == null)
		{
			oldForegroundPackages = new HashSet<String>(5);
		}
		
		if(nextStep  == START)
		{
			oldForegroundPackages = new HashSet<String>(currentForegroundApps);
			sessionTimeMillisStart = currentTime;
		}
		else if (nextStep == STOP)
		{
			oldForegroundPackages.clear();
			sessionTimeMillisStart = 0;
		}
		else if(nextStep == OPERATE)
		{
			
			newForegroundPackages.removeAll(oldForegroundPackages);
			if(!newForegroundPackages.isEmpty())
			{
				
				Logger.d(TAG,"added : " + newForegroundPackages.iterator().next());
				if(currentForegroundPackage != null)
				{
					recordASession(currentForegroundPackage, sessionTimeMillisStart);
				}
				
				currentForegroundPackage = newForegroundPackages.iterator().next();
				sessionTimeMillisStart = currentTime;
			}
			
			oldForegroundPackages.removeAll(currentForegroundApps);
			if(!oldForegroundPackages.isEmpty())
			{
				Logger.d(TAG,"removed : " + oldForegroundPackages.iterator().next());
				if(oldForegroundPackages.iterator().next().equals(currentForegroundPackage))
				{
					recordASession(currentForegroundPackage, sessionTimeMillisStart);
					// re initialize the below variables, no new package has come up.
					sessionTimeMillisStart = 0;
					currentForegroundPackage = null;
				}
			}
			
			oldForegroundPackages = new HashSet<String>(currentForegroundApps);
		}
	}
	
	private static void recordASession(String packageName, long sesstionTime)
	{
		long logtime = System.currentTimeMillis() - sesstionTime;
		String logPackage = packageName;
		Logger.d(TAG,"time : " + logtime + " of " + logPackage);
	}
	
}
