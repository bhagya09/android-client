package com.bsb.hike.platform;

import java.lang.ref.WeakReference;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.models.HikeHandlerUtil;
import com.hike.transporter.utils.Logger;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class NativeBridge
{
	private CocosGamingActivity activity;

	public BotInfo mBotInfo;

	public String msisdn;

	HikeHandlerUtil mThread;

	PlatformHelper helper;

	private String cardObj;

	public static final String TAG = "GameUtils";

	protected WeakReference<Activity> weakActivity;

	protected static final String SEND_SHARED_MESSAGE = "SEND_SHARED_MESSAGE";

	protected static final String ON_EVENT_RECEIVE = "ON_EVENT_RECEIVE";
	
	public static Handler mHandler;

	public NativeBridge(String msisdn, CocosGamingActivity activity)
	{
		this.activity=activity;
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
	 * @return
	 */
	public void getCardObj(final String functionId)
	{
		if (mThread == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				String cardObject = cardObj;
				if (cardObject == null)
				{
					cardObject = "{}";
				}
				activity.PlatformCallback(functionId, cardObject);
			}
		});
	}
	
	/**
	 * Platform Version 7 Call this method to get cardObj
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
				activity.PlatformCallback(functionId, mBotInfo.getHelperData());
			}
		});
	}

	/**
	 * Platform Version 7 Call this method to put data in cache. This will be a key-value pair. A game can have different key-value pairs in the native's cache.
	 * 
	 * @param key:
	 *            key of the data to be saved. Game needs to make sure about the uniqueness of the key.
	 * @param value:
	 *            : the data that the game need to cache.
	 */
	public void putInCache(final String key, final String value)
	{
		if (mThread == null)
			return;

		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{

				helper.putInCache(key, value, mBotInfo.getNamespace());

			}
		});

	}

	/**
	 * Platform Version 7
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

				activity.runOnGLThread(new Runnable()
				{
					@Override
					public void run()
					{
						activity.PlatformCallback(id, cache);
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
				helper.logAnalytics(isUI, subType, json, mBotInfo);
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
		if (mThread == null || weakActivity.get() == null)
			return;
		mThread.postRunnable(new Runnable()
		{

			@Override
			public void run()
			{
				helper.forwardToChat(json, hikeMessage, mBotInfo, weakActivity.get());
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
				helper.sendNormalEvent(messageHash, eventData, mBotInfo.getNamespace());
			}
		});
		
		
	}

	/**
	 * Platform Version 7 Call this function to send a shared message to the contacts of the user. This function when forwards the data, returns with the contact details of the
	 * users it has sent the message to. It will call JavaScript function "onContactChooserResult(int resultCode,JsonArray array)" This JSOnArray contains list of JSONObject where
	 * each JSONObject reflects one user. As of now each JSON will have name and platform_id, e.g : [{'name':'Paul','platform_id':'dvgd78as'}] resultCode will be 0 for fail and 1
	 * for success NOTE : JSONArray could be null as well, a game has to take care of this instance
	 *
	 * @param cardObject:
	 *            the cardObject data to create a card
	 * @param hikeMessage
	 *            : the hike message to be included in notif tupple and conversation tupple.
	 * @param sharedData:
	 *            the stringified json data to be shared among different bots. A mandatory field "recipients" is a must. It specifies what all namespaces to share the data with.
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
	 * @param functionId:
	 *            function id to call back to the game.
	 * @param messageHash:
	 *            the hash of the corresponding message.
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
				activity.runOnGLThread(new Runnable()
				{
					@Override
					public void run()
					{
						activity.PlatformCallback(functionId, returnedData);
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
	 * @param functionId:
	 *            function id to call back to the game.
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
						activity.PlatformCallback(functionId, returnedData);
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
	 * @param functionId:
	 *            function id to call back to the game.
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
						activity.PlatformCallback(functionId, returnedData);
					}
				});
			}
		});
	}

	/**
	 * Platform Version 7 Call this function to get the bot version.
	 * 
	 * @param id:
	 *            the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void getBotVersion(final String id)
	{
		activity.runOnGLThread(new Runnable()
		{
			@Override
			public void run()
			{
				activity.PlatformCallback(id, String.valueOf(mBotInfo.getVersion()));
			}
		});
	}

	/**
	 * Platform Version 7 Call this function to get the system architecture.
	 * 
	 * @param id:
	 *            the id of the function that native will call to call the js .
	 */
	public void getSystemArchitecture(final String id)
	{
		activity.runOnGLThread(new Runnable()
		{
			@Override
			public void run()
			{
				activity.PlatformCallback(id, System.getProperty("os.arch"));
			}
		});
	}
	/**
	 * Platform Version 7 Call this function to get the current platform version.
	 * 
	 * @param id:
	 *            the id of the function that native will call to call the js .
	 */
	public void getCurrentPlatformVersion(final String id)
	{
		activity.runOnGLThread(new Runnable()
		{
			@Override
			public void run()
			{
				activity.PlatformCallback(id, String.valueOf(HikePlatformConstants.CURRENT_VERSION));
			}
		});
	}
	
	

}