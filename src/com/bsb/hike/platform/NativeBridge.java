package com.bsb.hike.platform;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.AppState;
import com.bsb.hike.models.EventData;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.LogAnalyticsEvent;
import com.bsb.hike.models.NormalEvent;
import com.bsb.hike.models.NotifData;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.CustomAnnotation.DoNotObfuscate;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;
import com.hike.transporter.utils.Logger;
import android.widget.Toast;

@DoNotObfuscate
public class NativeBridge
{
	protected CocosGamingActivity activity;

	public BotInfo mBotInfo;

	public String msisdn;

	HikeHandlerUtil mThread;

	PlatformHelper helper;

	private String cardObj;

	private static final String TAG = "GameUtils";

	protected WeakReference<Activity> weakActivity;

	protected static final String SEND_SHARED_MESSAGE = "SEND_SHARED_MESSAGE";

	protected static final String ON_EVENT_RECEIVE = "ON_EVENT_RECEIVE";

	protected static final String SHARED_NOTIF_CACHE = "SHARED_NOTIF_CACHE";

	protected boolean openViaNotif;

	private static Handler mHandler;

	public NativeBridge(String msisdn, CocosGamingActivity activity)
	{
		this.activity = activity;
		this.msisdn = msisdn;
		weakActivity = new WeakReference<Activity>(activity);
		init();
	}

	public NativeBridge(String msisdn, CocosGamingActivity activity, String cardObj)
	{
		this(msisdn, activity);
		this.cardObj = cardObj;
	}

	private void init()
	{
		mThread = HikeHandlerUtil.getInstance();
		mThread.startHandlerThread();
		if (BotUtils.isBot(msisdn) == false)
		{
			Log.e(TAG, "Bot doesn't exist for this msisdn");
		}
		mBotInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);

	}

	/**
	 * Platform Version 7 Call this method to get cardObj
	 * 
	 *
	 * @return
	 */
	public String getCardObj()
	{
		Logger.d(TAG, "+getCardObj()");
		String cardObject;
		cardObject = (TextUtils.isEmpty(cardObj)) ? "{}" : cardObj;
		Logger.d(TAG, "-getCardObj() : " + cardObject);
		return cardObject;
	}

	/**
	 * Platform Version 7 Call this method to get Bot Helper data
	 * 
	 *
	 * @return
	 */
	public void getBotHelperData(final String functionId)
	{
		if (mThread == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				activity.platformCallback(functionId, mBotInfo.getHelperData());
			}
		});
	}

	/**
	 * Platform Version 7 Call this method to put data in cache. This will be a key-value pair. A game can have different key-value pairs in the native's cache.
	 *
	 * @param key
	 *            : key of the data to be saved. Game needs to make sure about the uniqueness of the key.
	 * @param value
	 *            : : the data that the game need to cache. lastGame is a reserved keyword now
	 */
	public void putInCache(final String key, final String value)
	{
		if (mThread == null)
			return;

		mThread.postRunnable(new Runnable() {

			@Override
			public void run() {

				helper.putInCache(key, value, mBotInfo.getNamespace());

			}
		});

	}

	/**
	 * Platform Version 7
	 *
	 * Call this function to get data from cache.
	 *
	 * @param id
	 *            : the id of the function that native will call to call the game .
	 * @param key
	 *            : key of the data demanded. Game needs to make sure about the uniqueness of the key.
	 */
	public void getFromCache(final String id, final String key)
	{
		if (mThread == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				final String cache = helper.getFromCache(key, mBotInfo.getNamespace());

				activity.runOnGLThread(new Runnable() {
					@Override
					public void run() {
						activity.platformCallback(id, cache);
					}
				});

			}
		});
	}

	/**
	 * Platform Version 7 Call this function to log analytics events.
	 *
	 * @param isUI
	 *            : whether the event is a UI event or not. This is a string. Send "true" or "false".
	 * @param subType
	 *            : the subtype of the event to be logged, eg. send "click", to determine whether it is a click event.
	 * @param json
	 *            : any extra info for logging events, including the event key that is pretty crucial for analytics.
	 */
	public void logAnalytics(final String isUI, final String subType, final String json)
	{
		if (mThread == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				LogAnalyticsEvent logAnalyticsEvent = new LogAnalyticsEvent(isUI, subType, json, mBotInfo.getMsisdn(), mBotInfo.getConversationName());
				Intent hikeProcessIntentService = new Intent(activity, HikeProcessIntentService.class);
				hikeProcessIntentService.putExtra(HikeProcessIntentService.LOG_ANALYTICS_EVENT_DATA, logAnalyticsEvent);
				activity.startService(hikeProcessIntentService);

			}
		});
	}

	/**
	 * Platform Version 7 Calling this function will initiate forward of the message to a friend or group.
	 *
	 * @param json
	 *            : the card object data for the forwarded card. This data will be the card object for the new forwarded card that'll be created. The platform version of the card
	 *            should be same as the bot, that is defined by the server. The app name and app package will also be added from the card object of the bot metadata.
	 * @param hikeMessage
	 *            : the hike message to be included in notif tupple and conversation tupple.
	 */
	public void forwardToChat(final String json, final String hikeMessage)
	{
		final Activity gameActivity = weakActivity.get();
		if (mThread == null || gameActivity == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				helper.forwardToChat(json, hikeMessage, mBotInfo, gameActivity);
			}
		});
	}

	/**
	 * Platform Version 7 Call this method to send a normal event.
	 *
	 * @param messageHash
	 *            : the message hash that determines the uniqueness of the card message, to which the data is being sent.
	 * @param eventData
	 *            : the stringified json data to be sent. It should contain the following things : "cd" : card data, "increase_unread" : true/false, "notification" : the string to
	 *            be notified to the user, "notification_sound" : true/ false, play sound or not.
	 */
	public void sendNormalEvent(final String messageHash, final String eventData)
	{
		if (mThread == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				NormalEvent normalEvent = new NormalEvent(messageHash, eventData, mBotInfo.getNamespace(), mBotInfo.getMsisdn());
				Intent hikeProcessIntentService = new Intent(activity, HikeProcessIntentService.class);
				hikeProcessIntentService.putExtra(HikeProcessIntentService.SEND_NORMAL_EVENT_DATA, normalEvent);
				activity.startService(hikeProcessIntentService);
			}
		});

	}

	/**
	 * Platform Version 7 Call this function to send a shared message to the contacts of the user. This function when forwards the data, returns with the contact details of the
	 * users it has sent the message to. It will call JavaScript function "onContactChooserResult(int resultCode,JsonArray array)" This JSOnArray contains list of JSONObject where
	 * each JSONObject reflects one user. As of now each JSON will have name and platform_id, e.g : [{'name':'Paul','platform_id':'dvgd78as'}] resultCode will be 0 for fail and 1
	 * for success NOTE : JSONArray could be null as well, a game has to take care of this instance
	 *
	 * @param cardObject
	 *            : the cardObject data to create a card
	 * @param hikeMessage
	 *            : the hike message to be included in notif tupple and conversation tupple.
	 * @param sharedData
	 *            : the stringified json data to be shared among different bots. A mandatory field "recipients" is a must. It specifies what all namespaces to share the data with.
	 */
	public void sendSharedMessage(final String cardObject, final String hikeMessage, final String sharedData)
	{
		if (mThread == null || weakActivity.get() == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				helper.sendSharedMessage(cardObject, hikeMessage, sharedData, mBotInfo, weakActivity.get());
			}
		});
	}

	/**
	 * Platform Version 7 Call this function to get all the event messages data. The data is a stringified list that contains: "name": name of the user interacting with. This gives
	 * name, and if the name isn't present , then the msisdn. "platformUid": the platform user id of the user interacting with. "eventId" : the event id of the event. "d" : the
	 * data that has been sent/received for the card message "et": the type of message. 0 if shared event, and 1 if normal event. "eventStatus" : the status of the event. 0 if
	 * sent, 1 if received.
	 *
	 * @param functionId
	 *            : function id to call back to the game.
	 * @param messageHash
	 *            : the hash of the corresponding message.
	 */
	public void getAllEventsForMessageHash(final String functionId, final String messageHash)
	{
		if (mThread == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				final String returnedData = helper.getAllEventsForMessageHash(messageHash, mBotInfo.getNamespace());
				activity.runOnGLThread(new Runnable() {
					@Override
					public void run() {
						activity.platformCallback(functionId, returnedData);
					}
				});
			}
		});
	}

	public void getAllEventsForMessageHashFromUser(final String functionId, final String messageHash, final String fromUserId)
	{
		if (mThread == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				final String returnedData = helper.getAllEventsForMessageHashFromUser(messageHash, mBotInfo.getNamespace(), fromUserId);
				activity.runOnGLThread(new Runnable() {
					@Override
					public void run() {
						activity.platformCallback(functionId, returnedData);
					}
				});
			}
		});
	}

	/**
	 * Platform Version 7 Call this function to get all the event messages data. The data is a stringified list that contains event id, message hash and the data.
	 * <p/>
	 * "name": name of the user interacting with. This gives name, and if the name isn't present , then the msisdn. "platformUid": the platform user id of the user interacting
	 * with. "eventId" : the event id of the event. "h" : the unique hash of the message. Helps in determining the uniqueness of a card. "d" : the data that has been sent/received
	 * for the card message "et": the type of message. 0 if shared event, and 1 if normal event. "eventStatus" : the status of the event. 0 if sent, 1 if received.
	 *
	 * @param functionId
	 *            : function id to call back to the game.
	 */
	public void getAllEventsData(final String functionId)
	{
		if (mThread == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				final String returnedData = helper.getAllEventsData(mBotInfo.getNamespace());
				activity.runOnGLThread(new Runnable()
				{
					@Override
					public void run()
					{
						activity.platformCallback(functionId, returnedData);
					}
				});
			}
		});
	}

	/**
	 * Platform Version 7 Call this function to get all the shared messages data. The data is a stringified list that contains event id, message hash and the data.
	 * <p/>
	 * "name": name of the user interacting with. This gives name, and if the name isn't present , then the msisdn. "platformUid": the platform user id of the user interacting
	 * with. "eventId" : the event id of the event. "h" : the unique hash of the message. Helps in determining the uniqueness of a card. "d" : the data that has been sent/received
	 * for the card message "eventStatus" : the status of the event. 0 if sent, 1 if received.
	 *
	 * @param functionId
	 *            : function id to call back to the game.
	 */
	public void getSharedEventsData(final String functionId)
	{
		if (mThread == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				final String returnedData = helper.getSharedEventsData(mBotInfo.getNamespace());
				activity.runOnGLThread(new Runnable()
				{
					@Override
					public void run()
					{
						activity.platformCallback(functionId, returnedData);
					}
				});
			}
		});
	}

	/**
	 * Platform Version 8 Call this fucntion to show a popup
	 *
	 * @param contentData
	 *            : The stringified JSONobject for the popup.
	 */
	public void showPopup(final String contentData)
	{
		Handler handler = new Handler(HikeMessengerApp.getInstance().getApplicationContext().getMainLooper());
		handler.post(new Runnable() {
			@Override
			public void run() {
				PlatformHelper.showPopup(contentData, weakActivity.get());
			}
		});
	}

	/**
	 * Platform Version 7 Call this function to get the bot version.
	 *
	 * @param id
	 *            : the id of the function that native will call to call the js .
	 */
	public void getBotVersion(final String id)
	{
		activity.runOnGLThread(new Runnable() {
			@Override
			public void run() {
				activity.platformCallback(id, String.valueOf(mBotInfo.getVersion()));
			}
		});
	}

	/**
	 * Platform Version 7 Call this function to get the system architecture.
	 *
	 * @param id
	 *            : the id of the function that native will call .
	 */
	public void getSystemArchitecture(final String id)
	{
		activity.runOnGLThread(new Runnable() {
			@Override
			public void run() {
				activity.platformCallback(id, System.getProperty("os.arch"));
			}
		});
	}

	/**
	 * Platform Version 7 Call this function to get the current platform version.
	 *
	 * @param id
	 *            : the id of the function .
	 */
	public void getCurrentPlatformVersion(final String id)
	{
		activity.runOnGLThread(new Runnable()
		{
			@Override
			public void run()
			{
				activity.platformCallback(id, String.valueOf(HikePlatformConstants.CURRENT_VERSION));
			}
		});
	}

	/**
	 * Platform Version 7 Call this function to delete event.
	 *
	 * @param eventId
	 */
	public void deleteEvent(final String eventId)
	{
		mThread.postRunnable(new Runnable() {

			@Override
			public void run() {
				EventData eventData = new EventData(true, eventId);
				Intent hikeProcessIntentService = new Intent(activity, HikeProcessIntentService.class);
				hikeProcessIntentService.putExtra(HikeProcessIntentService.EVENT_DELETE, eventData);
				activity.startService(hikeProcessIntentService);
			}
		});
	}

	/**
	 * Platform Version 7 Call this function to delete all the events, be it shared data or normal event pertaining to a single message.
	 *
	 * @param messageHash
	 */

	public void deleteAllEventsForMessage(final String messageHash)
	{
		mThread.postRunnable(new Runnable() {

			@Override
			public void run() {
				EventData eventData = new EventData(false, messageHash);
				Intent hikeProcessIntentService = new Intent(activity, HikeProcessIntentService.class);
				hikeProcessIntentService.putExtra(HikeProcessIntentService.EVENT_DELETE, eventData);
				activity.startService(hikeProcessIntentService);
			}
		});
	}

	/**
	 * Platform Version 7 Call this function to get the user details.
	 *
	 * @param id
	 */
	public void getUserDetails(final String id)
	{
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				String uid = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING, null);
				String name = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.NAME_SETTING, null);
				String anonName = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.ANONYMOUS_NAME_SETTING, "");
				String user_msisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, "");
				final JSONObject result = new JSONObject();
				try
				{
					result.put("uid", uid);
					result.put("name", name);
					result.put("anonName", anonName);
					result.put("msisdn", user_msisdn);
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				activity.runOnGLThread(new Runnable() {
					@Override
					public void run() {
						activity.platformCallback(id, result.toString());
					}
				});

			}
		});

	}

	/**
	 * Platform Version 7 Call this function to create a shortcut.
	 *
	 */
	public void addShortCut()
	{
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				if (weakActivity != null)
				{
					Utils.createShortcut(weakActivity.get(), mBotInfo, true);
				}
			}
		});
	}

	/**
	 * Platform Bridge Version 7 Call this method to post a status update to timeline.
	 *
	 * @param status
	 * @param moodId
	 *            : Pass -1 if no mood
	 * @param imageFilePath
	 *            : Path of the image on the client. Image should only be of jpeg format and compressed.
	 *
	 *            Status = null, moodId = -1 & imageFilePath = null should not hold together
	 *
	 *            0, happy 1, sad 2, in_love 3, surprised 4, confused 5, angry 6, sleepy 7, hungover 8, chilling 9, studying 10, busy 11, love 12, middle_finger 13, boozing 14,
	 *            movie 15, caffeinated 16, insomniac 17, driving 18, traffic 19, late 20, shopping 21, gaming 22, coding 23, television 33, music 34, partying_hard 35, singing 36,
	 *            eating 37, working_out 38, cooking 39, beauty_saloon 40, sick
	 *
	 */
	public void postStatusUpdate(final String status, final String moodId, final String imageFilePath)
	{
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				helper.postStatusUpdate(status, moodId, imageFilePath);
			}
		});
	}

	/**
	 * Platform Bridge Version 7 Call this method to post a status update without an image to timeline.
	 *
	 * @param status
	 * @param moodId
	 *            : Pass -1 if no mood
	 *
	 *            Both status = null and moodId = -1 should not hold together
	 *
	 *            0, happy 1, sad 2, in_love 3, surprised 4, confused 5, angry 6, sleepy 7, hungover 8, chilling 9, studying 10, busy 11, love 12, middle_finger 13, boozing 14,
	 *            movie 15, caffeinated 16, insomniac 17, driving 18, traffic 19, late 20, shopping 21, gaming 22, coding 23, television 33, music 34, partying_hard 35, singing 36,
	 *            eating 37, working_out 38, cooking 39, beauty_saloon 40, sick
	 *
	 */
	public void postStatusUpdate(final String status, final String moodId)
	{
		postStatusUpdate(status, moodId, null);
	}

	/**
	 *
	 * @param callID
	 * @param response
	 */
	public void platformCallback(final String callID, final String response)
	{
		activity.runOnGLThread(new Runnable() {
			@Override
			public void run() {
				activity.platformCallback(callID, response);
			}
		});
	}

	/**
	 * Returns status of connectivity, on the same thread. No need of handler/GlThread explicit invocation.
	 * 
	 * @return
	 */
	public boolean isNetworkConnected()
	{
		return Utils.getNetInfoFromConnectivityManager().second;
	}

	/**
	 * Handler for message event Received. Invokes platformCallback
	 * 
	 * @param eventData
	 */
	public void eventReceived(String eventData)
	{
		platformCallback(ON_EVENT_RECEIVE, eventData);
	}

	public void sendAppState(boolean isForeGround)
	{
		JSONObject object = new JSONObject();

		try
		{
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.APP_STATE);
			if (isForeGround)
			{
				object.put(HikeConstants.SUB_TYPE, HikeConstants.FOREGROUND);
			}
			else
			{
				object.put(HikeConstants.SUB_TYPE, HikeConstants.BACKGROUND);
			}
			JSONObject data = new JSONObject();
			data.put(HikeConstants.BULK_LAST_SEEN, false);
			object.put(HikeConstants.DATA, data);

		}
		catch (JSONException e)
		{
			com.bsb.hike.utils.Logger.w("AppState", "Invalid json", e);
		}
		AppState appState = new AppState(object.toString());
		Intent hikeProcessIntentService = new Intent(activity, HikeProcessIntentService.class);
		hikeProcessIntentService.putExtra(HikeProcessIntentService.SEND_APP_STATE, appState);
		activity.startService(hikeProcessIntentService);
	}

	/**
	 * Opens an activity in hike based on the data passed
	 *
	 * @param data
	 */
	public void openActivity(final String data)
	{

		if (mThread == null || weakActivity == null || weakActivity.get() == null)
		{
			return;
		}
		Log.d("cocos2d-x", data);
		mThread.postRunnable(new Runnable() {
			@Override
			public void run() {
				PlatformUtils.openActivity(weakActivity.get(), data);
			}
		});
	}

	/**
	 * show Toast msg
	 *
	 * @param data
	 *            : message to be displayed
	 */
	public void showToast(String data, String duration)
	{

		if (mThread == null || weakActivity == null || weakActivity.get() == null)
		{
			return;
		}

		final String message = data;
		final Application application = weakActivity.get().getApplication();
		final int length = duration.equals("long") ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;

		mThread.postRunnable(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(application, message, length).show();
			}
		});
	}

	/**
	 * Call this method to open the gallery view to select a file.
	 * 
	 * @param id
	 * @param displayCameraItem
	 *            : Whether or not to display the camera item in the gallery view.
	 */

	public void chooseFile(String id, String displayCameraItem)
	{
		PlatformHelper.chooseFile(id, displayCameraItem, weakActivity.get());
	}

	/**
	 * Call tis method to set alarm.
	 *
	 * @param jsonString
	 *            {"notification_sound":true,"increase_unread":true,"alarm_data":{},"notification":"test notif","rearrange_chat":true}
	 * @param timeInMills
	 * @param persistent
	 */
	public void setAlarm(final String jsonString, final float timeInMills, final boolean persistent)
	{
		try
		{
			if (weakActivity == null)
			{
				return;
			}
			final Activity mContext = weakActivity.get();
			if (TextUtils.isEmpty(msisdn) || mContext == null)
			{
				return;
			}

			final JSONObject json = new JSONObject(jsonString);
			JSONObject cacheJson = json;
			cacheJson.put("createdTime", System.currentTimeMillis());
			cacheJson.put("timeInMillis", timeInMills);

			String cacheJsonString = helper.getFromCache(SHARED_NOTIF_CACHE, mBotInfo.getNamespace());
			JSONArray cacheJsonArray = new JSONArray();
			if (cacheJsonString != null && cacheJsonString.length() > 0)
			{
				cacheJsonArray = new JSONArray(cacheJsonString);
			}
			cacheJsonArray.put(cacheJson);
			helper.putInCache(SHARED_NOTIF_CACHE, cacheJsonArray.toString(), mBotInfo.getNamespace());

			mThread.postRunnable(new Runnable()
			{
				@Override
				public void run()
				{

					NonMessagingBotAlarmManager.setAlarm(mContext, json, msisdn, (long) timeInMills, persistent);
				}
			});
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * call this function to get the notif data pertaining to the microApp. This function will be called from the GLThread, no need to explicitly return on GLThread.
	 */
	public String getNotifData()
	{
		if (msisdn == null)
			return "";
		BotInfo botinfo = HikeConversationsDatabase.getInstance().getBotInfoForMsisdn(msisdn);
		try
		{
			if (botinfo == null)
			{
				return "";
			}
			String value = botinfo.getNotifData();
			if (value != null && value.length() > 0)
			{
				JSONObject notifJson = new JSONObject(value);
				Iterator keysToCopyIterator = notifJson.keys();
				JSONArray notifArray = new JSONArray();
				while (keysToCopyIterator.hasNext())
				{
					String key = (String) keysToCopyIterator.next();
					notifArray.put(notifJson.get(key));
				}
				value = notifArray.toString();
			}
			else
			{
				value = "";
			}
			final String notifData = value;
			deleteAllNotifData();
			return notifData;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		deleteAllNotifData();
		return "";
	}

	/**
	 * call this function to get the notif data ,pertaining to the microApp, which opened the microapp on clicking the notification. This function will be called from the GLThread,
	 * no need to explicitly return on GLThread.
	 */
	public String getCurrentNotifData()
	{
		if (openViaNotif)
		{
			try
			{
				if (mBotInfo == null)
				{
					return "";
				}
				String value = mBotInfo.getNotifData();
				if (value != null && value.length() > 0)
				{
					JSONObject notifJson = new JSONObject(value);
					Iterator keysToCopyIterator = notifJson.keys();
					JSONArray notifArray = new JSONArray();
					while (keysToCopyIterator.hasNext())
					{
						String key = (String) keysToCopyIterator.next();
						notifArray.put(notifJson.get(key));
					}
					value = notifArray.getJSONObject(notifArray.length() - 1).toString();
				}
				else
				{
					value = "";
				}
				final String notifData = value;
				return notifData;
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		return "";
	}

	/**
	 * call this function to delete the entire notif data of the microApp.
	 */
	public void deleteAllNotifData()
	{
		if (msisdn == null)
		{
			return;
		}
		helper.putInCache(SHARED_NOTIF_CACHE, "", mBotInfo.getNamespace());
		Intent hikeProcessIntentService = new Intent(activity, HikeProcessIntentService.class);
		hikeProcessIntentService.putExtra(HikeProcessIntentService.NOTIF_DATA_DELETE, msisdn);
		activity.startService(hikeProcessIntentService);
	}

	/**
	 * Call this function to delete partial notif data pertaining to a microApp. The key is the timestamp provided by Native
	 * 
	 * @param key
	 *            : the key of the saved data. Will remain unique for a unique microApp.
	 */
	public void deletePartialNotifData(String key)
	{
		if (key == null || TextUtils.isEmpty(msisdn))
		{
			return;
		}
		NotifData notifData = new NotifData(key, msisdn);
		Intent hikeProcessIntentService = new Intent(activity, HikeProcessIntentService.class);
		hikeProcessIntentService.putExtra(HikeProcessIntentService.NOTIF_DATA_PARTIAL_DELETE, notifData);
		activity.startService(hikeProcessIntentService);
	}

	/**
	 * Call this function to cancel the alarm data associtated with a particular alarm data
	 * 
	 * @param alarmData
	 */
	public void cancelAlarm(String alarmData)
	{
		if (weakActivity == null || TextUtils.isEmpty(msisdn) || alarmData == null)
		{
			return;
		}
		HikeAlarmManager.cancelAlarm(weakActivity.get(), msisdn.hashCode() + alarmData.hashCode());
	}

	/**
	 * Platform Version 12
	 * Method to get list of children bots
	 */
	public String getChildrenBots(String id)
	{
		try {
			if (!TextUtils.isEmpty(id) && mBotInfo !=null)
			{
				String childrenBotInformation = PlatformHelper.getChildrenBots(mBotInfo.getMsisdn());
				return childrenBotInformation;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "[]";
	}

	/**
	 * Platform Version 12
	 * Method to get bot information as string
	 */
	public String getBotInfoAsString(String id)
	{
		try {
			if (!TextUtils.isEmpty(id) && mBotInfo !=null)
			{
				String botInformation = BotUtils.getBotInfoAsString(mBotInfo).toString();
				return botInformation;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "{}";
	}

}
