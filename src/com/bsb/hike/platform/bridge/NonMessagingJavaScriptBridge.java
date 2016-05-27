package com.bsb.hike.platform.bridge;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotConfiguration;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.FileRequestPersistent;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.GpsLocation;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.NonMessagingBotAlarmManager;
import com.bsb.hike.platform.PlatformHelper;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.auth.AuthListener;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.platform.content.PlatformZipDownloader;
import com.bsb.hike.tasks.SendLogsTask;
import com.bsb.hike.utils.CustomAnnotation.DoNotObfuscate;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

/**
 * API bridge that connects the javascript to the non-messaging Native environment. Make the instance of this class and add it as the
 * JavaScript interface of the MicroApp WebView.
 * This class caters Platform Bridge version 0
 *
 * Platform Bridge Version Start = 1
 * Platform Bridge Version End = ~
 */
@DoNotObfuscate
public class NonMessagingJavaScriptBridge extends JavascriptBridge
{
	private static final int OPEN_FULL_PAGE_WITH_TITLE = 111;
	
	private static final int SHOW_OVERFLOW_MENU = 112;
	
	private static final int OPEN_FULL_PAGE = 114;

	private static final int CHANGE_ACTION_BAR_TITLE = 115;
	
	private static final int CHANGE_STATUS_BAR_COLOR = 116;
	
	private static final int CHANGE_ACTION_BAR_COLOR = 117;
	
	private BotInfo mBotInfo;
	
	private static final String TAG  = "NonMessagingJavaScriptBridge";
	
	private IBridgeCallback mCallback;

	private String extraData; // Any extra miscellaneous data received in the intent.
	
	public NonMessagingJavaScriptBridge(Activity activity, CustomWebView mWebView, BotInfo botInfo, IBridgeCallback callback)
	{
		super(activity, mWebView);
		this.mBotInfo = botInfo;
		this.mCallback = callback;
	}

	/**
	 * Platform Bridge Version 0.
	 * Call this function to log analytics events.
	 *
	 * @param isUI    : whether the event is a UI event or not. This is a string. Send "true" or "false".
	 * @param subType : the subtype of the event to be logged, eg. send "click", to determine whether it is a click event.
	 * @param json    : any extra info for logging events, including the event key that is pretty crucial for analytics.
	 */
	@JavascriptInterface
	public void logAnalytics(String isUI, String subType, String json)
	{
		PlatformHelper.logAnalytics(isUI, subType, json, mBotInfo);
	}

	@Override
	@JavascriptInterface
	public void onLoadFinished(String height)
	{
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				init();
			}
		});

	}

	/**
	 * Platform Bridge Version 1
	 * this function will update the helper data. It will replace the key if it is present in the helper data and will add it if it is not present in the helper data.
	 *
	 * @param json
	 */
	@JavascriptInterface
	public void updateHelperData(String json)
	{
		if (TextUtils.isEmpty(json))
		{
			Logger.e(tag, "json to update helper data is empty. Returning.");
			return;
		}

		Logger.i(tag, "update helperData called " + json + " , MicroApp msisdn : " + mBotInfo.getMsisdn());
		String oldHelper = mBotInfo.getHelperData();

		try
		{
			JSONObject oldHelperDataJson = TextUtils.isEmpty(oldHelper) ? new JSONObject() : new JSONObject(oldHelper);
			JSONObject helperDataDiff = new JSONObject(json);
			JSONObject newHelperData = PlatformUtils.mergeJSONObjects(oldHelperDataJson, helperDataDiff);

			mBotInfo.setHelperData(newHelperData.toString());
			HikeConversationsDatabase.getInstance().updateHelperDataForNonMessagingBot(mBotInfo.getMsisdn(), mBotInfo.getHelperData());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * Platform Bridge Version 1
	 * Calling this function will initiate forward of the message to a friend or group.
	 * 
	 * @param json
	 *            : the card object data for the forwarded card. This data will be the card object for the new forwarded card
	 *            that'll be created. The platform version of the card should be same as the bot, that is defined by the server. The
	 *            app name and app package will also be added from the card object of the bot metadata.
	 *@param hikeMessage : the hike message to be included in notif tupple and conversation tupple.
	 */
	@JavascriptInterface
	public void forwardToChat(String json, String hikeMessage)
	{
		PlatformHelper.forwardToChat(json, hikeMessage, mBotInfo, weakActivity.get());
	}

	/**
	 * Data is encoded with URL Encoded Scheme. Decode it before using.
	 * The json contains:
	 * hd: helper data
	 * target_platform: the platform version that this bot and associated microapp targets
	 * notifData: notif data
	 * block: whether the bot is blocked
	 * mute: whether the bot is muted
	 * networkType:
	 *	 <li>-1 in case of no network</li>
	 * 	 <li>0 in case of unknown network</li>
	 *	 <li>1 in case of wifi</li>
	 *	 <li>2 in case of 2g</li>
	 *	 <li>3 in case of 3g</li>
	 *	 <li>4 in case of 4g</li>
	 */
	public void init()
	{
		JSONObject jsonObject = new JSONObject();
		try
		{
			NonMessagingBotMetadata botMetadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
			getInitJson(jsonObject, mBotInfo.getMsisdn());
			jsonObject.put(HikePlatformConstants.TARGET_PLATFORM, botMetadata.getTargetPlatform());
			jsonObject.put(HikePlatformConstants.HELPER_DATA, mBotInfo.getHelperData());
			jsonObject.put(HikePlatformConstants.NOTIF_DATA, mBotInfo.getNotifDataJSON());
			jsonObject.put(HikePlatformConstants.BLOCK, Boolean.toString(mBotInfo.isBlocked()));
			jsonObject.put(HikePlatformConstants.MUTE, Boolean.toString(mBotInfo.isMute()));
			jsonObject.put(HikePlatformConstants.NETWORK_TYPE, Integer.toString(Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext())));
			jsonObject.put(HikePlatformConstants.BOT_VERSION, mBotInfo.getVersion());
            jsonObject.put(HikePlatformConstants.MAPP_VERSION_CODE, mBotInfo.getMAppVersionCode());
			jsonObject.put(HikePlatformConstants.ASSOCIATE_MAPP,botMetadata.getAsocmapp());
			jsonObject.put(HikeMessengerApp.PRODUCTION,HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PRODUCTION,true));

			if (!TextUtils.isEmpty(extraData))
			{
				jsonObject.put(HikePlatformConstants.EXTRA_DATA, extraData);
			}

			PlatformUtils.addLocaleToInitJSON(jsonObject);

			mWebView.loadUrl("javascript:init('" + getEncodedDataForJS(jsonObject.toString()) + "')");
		}
		catch (JSONException e)
		{
			Logger.e(tag, "JSON exception in init at NonMessagingJavascriptBridge");
			e.printStackTrace();
		}
	}


	/**
	 * Platform Bridge Version 1
	 * calling this method will forcefully mute the full screen bot. The user won't receive any more notifications after calling this.
	 */
	@JavascriptInterface
	public void muteChatThread()
	{
		mBotInfo.setIsMute(!mBotInfo.isMute());
		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED, mBotInfo.getMute());
	}

	/**
	 * Platform Bridge Version 1
	 * calling this method will forcefully block the full screen bot. The user won't see any messages in the chat thread after calling this.
	 * 
	 * @param isBlocked : true to block the microapp false to unblock it.
	 */
	@JavascriptInterface
	public void blockChatThread(String isBlocked)
	{
		if (Boolean.valueOf(isBlocked))
		{
			mBotInfo.setBlocked(true);
			HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, mBotInfo.getMsisdn());
		}
		
		else
		{
			mBotInfo.setBlocked(false);
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, mBotInfo.getMsisdn());
		}
	}

	public void onMenuItemClicked(int id )
	{
		mWebView.loadUrl("javascript:platformSdk.onMenuItemClicked('" + id + "')");
	}

	/**
	 * Platform Bridge Version 1
	 * Call this function to update the overflow menu items.
	 * @param id
	 * @param newMenuJSON
	 */
	@JavascriptInterface
	public void updateOverflowMenu(String id, String newMenuJSON)
	{
		NonMessagingBotConfiguration botConfig = new NonMessagingBotConfiguration(mBotInfo.getConfiguration(), mBotInfo.getConfigData());
		if (botConfig.getConfigData() != null)
		{
			try
			{
				botConfig.updateOverFlowMenu(Integer.parseInt(id), new JSONObject(newMenuJSON));
				mBotInfo.setConfigData(botConfig.getConfigData().toString());
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void onBackPressed()
	{
		mWebView.loadUrl("javascript:platformSdk.events.publish('onBackPressed')");
	}

	public void onUpPressed()
	{
		mWebView.loadUrl("javascript:platformSdk.events.publish('onUpPressed')");
	}
	/**
	 * Platform Bridge Version 1
	 * Utility method to remove a menu from the list of menu options for a bot
	 * 
	 * @param id
	 */
	@JavascriptInterface
	public void removeMenu(String id)
	{
		NonMessagingBotConfiguration botConfig = new NonMessagingBotConfiguration(mBotInfo.getConfiguration(), mBotInfo.getConfigData());
		if (botConfig.getConfigData() != null)
		{
			botConfig.removeOverflowMenu(Integer.parseInt(id));
			mBotInfo.setConfigData(botConfig.getConfigData().toString());
			HikeConversationsDatabase.getInstance().updateConfigData(mBotInfo.getMsisdn(), botConfig.getConfigData().toString());
		}
	}

	/**
	 * Platform Bridge Version 1
	 * Utility method to fetch the overflowMenu from the MicroApp. This replaces the existing menu in the config data of the app
	 * 
	 * MenuString should be in the following form : <br>
	 * [ {“title”: “xyz”, “id” : <unique integer>, "en" : "true"}, {“title”:abc,”id”:<unique integer>, "en":"false" }]
	 * 
	 * @param newMenuString
	 */
	@JavascriptInterface
	public void replaceOverflowMenu(String newMenuString)
	{
		NonMessagingBotConfiguration botConfig = new NonMessagingBotConfiguration(mBotInfo.getConfiguration(), mBotInfo.getConfigData());
		if(botConfig.getConfigData() != null)
		{
			botConfig.replaceOverflowMenu(newMenuString);
			mBotInfo.setConfigData(botConfig.getConfigData().toString());
			HikeConversationsDatabase.getInstance().updateConfigData(mBotInfo.getMsisdn(), botConfig.getConfigData().toString());
			
			sendMessageToUiThread(SHOW_OVERFLOW_MENU, null);
		}
	}

	/**
	 * Platform Bridge Version 1
	 * Call this method to put bulk large data in cache. Earlier large data will be replaced by this new data and there will
	 * be only one entry per microapp.
	 * @param value: the data that the app need to cache.
	 */
	@JavascriptInterface
	public void putLargeDataInCache(String value)
	{
		HikeContentDatabase.getInstance().putInContentCache(mBotInfo.getNamespace(), mBotInfo.getNamespace(), value);
	}

	/**
	 * Platform Bridge Version 1
	 * Call this method to put data in cache. This will be a key-value pair. A microapp can have different key-value pairs
	 * in the native's cache.
	 * @param key: key of the data to be saved. Microapp needs to make sure about the uniqueness of the key.
	 * @param value: : the data that the app need to cache.
	 */
	@JavascriptInterface
	public void putInCache(String key, String value)
	{
		PlatformHelper.putInCache(key, value, mBotInfo.getNamespace());
	}

	/**
	 * Platform Bridge Version 1
	 * Call this function to get the bulk large data from the native memory
	 * @param id : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void getLargeDataFromCache(String id)
	{
		String value = HikeContentDatabase.getInstance().getFromContentCache(mBotInfo.getNamespace(), mBotInfo.getNamespace());
		callbackToJS(id, value);
	}

	/**
	 * Platform Bridge Version 1
	 * call this function to get the data from the native memory
	 * @param id: the id of the function that native will call to call the js .
	 * @param key: key of the data demanded. Microapp needs to make sure about the uniqueness of the key.
	 */
	@JavascriptInterface
	public void getFromCache(String id, String key)
	{
		String value = PlatformHelper.getFromCache(key, mBotInfo.getNamespace());
		callbackToJS(id, value);
	}

	/**
	 * Platform Bridge Version 1
	 * call this function to get the notif data pertaining to the microApp.
	 * @param id: the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void getNotifData(String id)
	{
		String value = mBotInfo.getNotifData();
		callbackToJS(id, value);
	}

	/**
	 * Platform Bridge Version 1
	 * call this function to delete the entire notif data of the microApp.
	 */
	@JavascriptInterface
	public void deleteAllNotifData()
	{
		HikeConversationsDatabase.getInstance().deleteAllNotifDataForMicroApp(mBotInfo.getMsisdn());
	}

	/**
	 * Platform Bridge Version 1
	 * Call this function to delete partial notif data pertaining to a microApp. The key is the timestamp provided by Native
	 * @param key: the key of the saved data. Will remain unique for a unique microApp.
	 */
	@JavascriptInterface
	public void deletePartialNotifData(String key)
	{
		HikeConversationsDatabase.getInstance().deletePartialNotifData(key, mBotInfo.getMsisdn());
	}


	public void notifDataReceived(final String notifData)
	{
		if (mHandler == null || TextUtils.isEmpty(notifData))
		{
			return;
		}
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mWebView.loadUrl("javascript:notifDataReceived" + "('" + getEncodedDataForJS(notifData) + "')");
			}
		});
	}

	public void eventReceived(final String event)
	{
		if (mHandler == null || TextUtils.isEmpty(event))
		{
			return;
		}
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mWebView.loadUrl("javascript:eventReceived" + "('" + getEncodedDataForJS(event) + "')");
			}
		});
	}

	/**
	 * Platform Bridge Version 1
	 * Utility method to indicate change in orientation of the device.<br>
	 * 1 : Indicates PORTRAIT <br>
	 * 2 : Indicates LANDSCAPE
	 * 
	 * @param orientation
	 */
	public void orientationChanged(int orientation)
	{
		mWebView.loadUrl("javascript:orientationChanged('" + Integer.toString(orientation) + "')");
	}
	
	/**
	 * Platform Bridge Version 1
	 * Utility method to call finish of the current activity
	 */
	@JavascriptInterface
	public void finish()
	{
		Activity activity = weakActivity.get();
		if (activity != null)
		{
			activity.finish();
		}
	}

	/**
	 * Platform Bridge Version 1
	 * Call this function to allow the back Press. The android back button will be given to the microapp.
	 * @param allowBack
	 */
	@JavascriptInterface
	public void allowBackPress(String allowBack)
	{
		mBotInfo.setIsBackPressAllowed(Boolean.valueOf(allowBack));
	}
	
	@Override
	protected void handleUiMessage(Message msg)
	{
		switch (msg.what)
		{
		case SHOW_OVERFLOW_MENU:
			if (mCallback != null)
			{
				mCallback.overflowMenuUpdated();
			}
			break;

		case OPEN_FULL_PAGE_WITH_TITLE:
			if (mCallback != null)
			{
				String[] params = (String[]) msg.obj;
				// checking for interceptUrl JSON String
				if (params[3] != null)
				{
					mCallback.openFullPageWithTitle(params[1], params[0], params[2], params[3]); // Url, title, interceptUrlJson,back
				}
				else if (params[2] != null)
				{
					mCallback.openFullPageWithTitle(params[1], params[0], params[2]); // Url, title, interceptUrlJson
				}
				else
				{
					mCallback.openFullPageWithTitle(params[1], params[0]); // Url, Title
				}
			}
			break;
		case CHANGE_ACTION_BAR_TITLE:
			if (mCallback != null)
			{
				String title = (String) msg.obj;
				mCallback.changeActionBarTitle(title);
			}
			break;
			
		case CHANGE_STATUS_BAR_COLOR:
			if (mCallback != null)
			{
				String sbColor = (String) msg.obj;
				mCallback.changeStatusBarColor(sbColor);
			}
			
			break;
			
		case CHANGE_ACTION_BAR_COLOR :
			if (mCallback != null)
			{
				String abColor = (String) msg.obj;
				mCallback.changeActionBarColor(abColor);
			}
			
			break;
			
		default:
			super.handleUiMessage(msg);
		}
	}

	/**
	 * Platform bridge Version 2
	 * Call this function to open a full page webView within hike. Calling this function will create full page with action bar
	 * color specified by server, and js injected to remove unwanted features from the full page.
	 * @param title
	 *            : the title on the action bar.
	 * @param url
	 *            : the url that will be loaded.
	 */
	@JavascriptInterface
	@Override
	public void openFullPage(final String title, final String url)
	{
		openFullPage(title, url, null, "false");
	}
	
	/**
	 * Platform bridge Version 1
	 * Call this function to open a full page webView within hike.
	 * 
	 * @param url
	 *            : the url that will be loaded.
	 */
	@JavascriptInterface
	public void openFullPage(final String url)
	{
		sendMessageToUiThread(OPEN_FULL_PAGE, url);
	}

	/**
	 * Platform Version 2
	 * called by the special packet sent in the bot to delete the conversation of the particular bot
	 */
	@JavascriptInterface
	public void deleteBotConversation()
	{
		Logger.i(tag, "delete bot conversation and removing from conversation fragment");
		final Activity context = weakActivity.get();
		if (context != null)
		{
			ConversationsAdapter.removeBotMsisdn = mBotInfo.getMsisdn();
			final Intent intent = Utils.getHomeActivityIntent(context);
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					context.finish();
					context.startActivity(intent);
				}
			});
		}
	}
	/**
	 * Platform Bridge Version 3
	 * call this function to delete the entire caching related to the namespace of the bot.
	 */
	@JavascriptInterface
	public void deleteAllCacheData()
	{
		HikeContentDatabase.getInstance().deleteAllMicroAppCacheData(mBotInfo.getNamespace());
	}

	/**
	 * Platform Bridge Version 3
	 * Call this function to delete partial cached data pertaining to the namespace of the bot, The key is  provided by Javascript
	 * @param key: the key of the saved data. Will remain unique for a unique microApp.
	 */
	@JavascriptInterface
	public void deletePartialCacheData(String key)
	{
		HikeContentDatabase.getInstance().deletePartialMicroAppCacheData(key, mBotInfo.getNamespace());
	}

	@JavascriptInterface
	@Override
	public void onResize(String height)
	{
		//do nothing
	}

	/**
	 * Platform Version 3
	 * Call this method to show the gallery view and select a file.
	 * This method will not show the camera item in the gallery view.
	 * @param id
	 */
	@JavascriptInterface
	public void chooseFile(String id)
	{	
		chooseFile(id, "false");
	}
	/**
	 * Platform Version 3
	 * call this method to change the title of the action bar for the bot.
	 * @param title : the title on the action bar.
	 */
	@JavascriptInterface
	public void changeBotTitle(final String title)
	{
		if (!TextUtils.isEmpty(title))
		{
			sendMessageToUiThread(CHANGE_ACTION_BAR_TITLE, title);
		}
	}

	/**
	 * Platform Version 3
	 * call this method to reset the title of the action bar for the bot to the original title sent by server.
	 */
	@JavascriptInterface
	public void resetBotTitle()
	{
		if (!TextUtils.isEmpty(mBotInfo.getConversationName()))
		{
			sendMessageToUiThread(CHANGE_ACTION_BAR_TITLE, mBotInfo.getConversationName());
		}
	}
	

	/**
	 * Platform bridge Version 4
	 * 
	 * Call this method to change the status bar color at runtime. <br>
	 * this method will work only on Android L and above (i.e. API Level 21 + ) <br>
	 * However calling this on lower devices will not crash the app <br>
	 * 
	 * @param sbColor : The hex code of the color. eg : "#ef48f1"
	 * Be careful about the '#' in the hex code
	 */
	@JavascriptInterface
	public void setStatusBarColor(String sbColor)
	{
		if (!TextUtils.isEmpty(sbColor))
		{
			sendMessageToUiThread(CHANGE_STATUS_BAR_COLOR, sbColor);
		}
	}

	/**
	 * Platform Bridge Version 5
	 * Call this function to allow the up Press. The android up button will be given to the microapp.
	 * @param allowUp
	 */
	@JavascriptInterface
	public void allowUpPress(String allowUp)
	{
		mBotInfo.setIsUpPressAllowed(Boolean.valueOf(allowUp));
	}
	
	/**
	 * Platform Bridge Version 5
	 * Call this function to change action bar color at runtime. <br>
	 * This method will work regardless of the Android Version. <br> 
	 * Call it prudently, since it can alter the beauty of the micro app
	 *
	 * 
	 * @param abColor
	 */
	@JavascriptInterface
	public void setActionBarColor(String abColor)
	{
		if (!TextUtils.isEmpty(abColor))
		{
			sendMessageToUiThread(CHANGE_ACTION_BAR_COLOR, abColor);
		}
	}

	/**
	 * Platform Version 6
	 * Call this function to delete an event from the list of events that are shared with the microapp.
	 *
	 * @param eventId: the event that will be deleted from the shared messages table.
	 */
	@JavascriptInterface
	public void deleteEvent(String eventId)
	{
		PlatformHelper.deleteEvent(eventId);
	}

	/**
	 * Platform Version 6
	 * Call this function to delete all the events, be it shared data or normal event pertaining to a single message.
	 *
	 * @param messageHash
	 */
	@JavascriptInterface
	public void deleteAllEventsForMessage(String messageHash)
	{
		PlatformHelper.deleteAllEventsForMessage(messageHash);
	}

	/**
	 * Platform version 6
	 * Call this function to delete all the events for a particular microapp, be it shared data or normal event.
	 */
	@JavascriptInterface
	public void deleteAllEventsForMicroapp()
	{
		HikeConversationsDatabase.getInstance().deleteAllEventsForNamespace(mBotInfo.getNamespace());
	}

	/**
	 * Platform Version 6
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * Call this function to delete all the events for a particular microapp, be it shared data or normal event.
	 *
	 * @param namespace: the namespace whose shared events are being asked
	 */
	@JavascriptInterface
	public void deleteAllEventsForMicroapp(String namespace)
	{
		if (TextUtils.isEmpty(namespace))
		{
			Logger.e(TAG, "the events corresponding to the namespace can't be deleted as the namespace is " + namespace);
			return;
		}
		NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
		if (!metadata.isSpecialBot())
		{
			Logger.e(TAG, "the bot is not a special bot and only special bot has the authority to call this function.");
			return;
		}
		HikeConversationsDatabase.getInstance().deleteAllEventsForNamespace(namespace);
	}

	/**
	 * Platform Version 6
	 * Call this function to get all the shared messages data. The data is a stringified list that contains event id, message hash and the data.
	 * <p/>
	 * "name": name of the user interacting with. This gives name, and if the name isn't present , then the msisdn.
	 * "platformUid": the platform user id of the user interacting with.
	 * "eventId" : the event id of the event.
	 * "h" : the unique hash of the message. Helps in determining the uniqueness of a card.
	 * "d" : the data that has been sent/received for the card message
	 * "eventStatus" : the status of the event. 0 if sent, 1 if received.
	 *
	 * @param functionId: function id to call back to the js.
	 */
	@JavascriptInterface
	public void getSharedEventsData(String functionId)
	{
		String messageData = PlatformHelper.getSharedEventsData(mBotInfo.getNamespace());
		callbackToJS(functionId, messageData);
	}

	/**
	 * Platform Version 6
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * Call this function to get all the shared events data. The data is a stringified list that contains :
	 * "name": name of the user interacting with. This gives name, and if the name isn't present , then the msisdn.
	 * "platformUid": the platform user id of the user interacting with.
	 * "eventId" : the event id of the event.
	 * "h" : the unique hash of the message. Helps in determining the uniqueness of a card.
	 * "d" : the data that has been sent/received for the card message
	 * "eventStatus" : the status of the event. 0 if sent, 1 if received.
	 *
	 * @param functionId: function id to call back to the js.
	 * @param namespace   : the namespace whose shared events are being asked
	 */
	@JavascriptInterface
	public void getSharedEventsData(String functionId, String namespace)
	{
		if (TextUtils.isEmpty(namespace))
		{
			Logger.e(TAG, "can't return shared events as the namespace is " + namespace);
			return;
		}
		NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
		if (!metadata.isSpecialBot())
		{
			Logger.e(TAG, "the bot is not a special bot and only special bot has the authority to call this function.");
			return;
		}
		String messageData = HikeConversationsDatabase.getInstance().getMessageEventsForMicroapps(namespace, false);
		callbackToJS(functionId, messageData);
	}

	/**
	 * Platform Version 6
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * Call this function to get all the event messages data. The data is a stringified list that contains:
	 * "name": name of the user interacting with. This gives name, and if the name isn't present , then the msisdn.
	 * "platformUid": the platform user id of the user interacting with.
	 * "eventId" : the event id of the event.
	 * "h" : the unique hash of the message. Helps in determining the uniqueness of a card.
	 * "d" : the data that has been sent/received for the card message
	 * "et": the type of message. 0 if shared event, and 1 if normal event.
	 * "eventStatus" : the status of the event. 0 if sent, 1 if received.
	 *
	 * @param functionId: function id to call back to the js.
	 * @param namespace   : the namespace whose shared events are being asked
	 */
	@JavascriptInterface
	public void getAllEventsData(String functionId, String namespace)
	{
		if (TextUtils.isEmpty(namespace))
		{
			Logger.e(TAG, "can't return all events as the namespace is " + namespace);
			return;
		}
		NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
		if (!metadata.isSpecialBot())
		{
			Logger.e(TAG, "the bot is not a special bot and only special bot has the authority to call this function.");
			return;
		}
		String messageData = HikeConversationsDatabase.getInstance().getMessageEventsForMicroapps(namespace, true);
		callbackToJS(functionId, messageData);
	}

	/**
	 * Platform Version 6
	 * Call this function to get all the event messages data. The data is a stringified list that contains event id, message hash and the data.
	 * <p/>
	 * "name": name of the user interacting with. This gives name, and if the name isn't present , then the msisdn.
	 * "platformUid": the platform user id of the user interacting with.
	 * "eventId" : the event id of the event.
	 * "h" : the unique hash of the message. Helps in determining the uniqueness of a card.
	 * "d" : the data that has been sent/received for the card message
	 * "et": the type of message. 0 if shared event, and 1 if normal event.
	 * "eventStatus" : the status of the event. 0 if sent, 1 if received.
	 *
	 * @param functionId: function id to call back to the js.
	 */
	@JavascriptInterface
	public void getAllEventsData(String functionId)
	{
		String messageData = PlatformHelper.getAllEventsData(mBotInfo.getNamespace());
		callbackToJS(functionId, messageData);
	}

	/**
	 * Platform Version 6
	 * Call this function to get all the event messages data. The data is a stringified list that contains:
	 * "name": name of the user interacting with. This gives name, and if the name isn't present , then the msisdn.
	 * "platformUid": the platform user id of the user interacting with.
	 * "eventId" : the event id of the event.
	 * "d" : the data that has been sent/received for the card message
	 * "et": the type of message. 0 if shared event, and 1 if normal event.
	 * "eventStatus" : the status of the event. 0 if sent, 1 if received.
	 *
	 * @param functionId:  function id to call back to the js.
	 * @param messageHash: the hash of the corresponding message.
	 */
	@JavascriptInterface
	public void getAllEventsForMessageHash(String functionId, String messageHash)
	{
		String eventData =PlatformHelper.getAllEventsForMessageHash(messageHash, mBotInfo.getNamespace());
		callbackToJS(functionId, eventData);
	}

	@JavascriptInterface
	public void getAllEventsForMessageHashFromUser(String functionId, String messageHash, String fromUserId)
	{
		String eventData =PlatformHelper.getAllEventsForMessageHashFromUser(messageHash, mBotInfo.getNamespace(), fromUserId);
		callbackToJS(functionId, eventData);
	}

	/**
	 * Platform Version 6
	 * Call this function to send a shared message to the contacts of the user. This function when forwards the data, returns with the contact details of
	 * the users it has sent the message to.
	 * It will call JavaScript function "onContactChooserResult(int resultCode,JsonArray array)" This JSOnArray contains list of JSONObject where each JSONObject reflects one user. As of now
	 * each JSON will have name and platform_id, e.g : [{'name':'Paul','platform_id':'dvgd78as'}] resultCode will be 0 for fail and 1 for success NOTE : JSONArray could be null as
	 * well, a micro app has to take care of this instance
	 *
	 * @param cardObject: the cardObject data to create a card
	 * @param hikeMessage : the hike message to be included in notif tupple and conversation tupple.
	 * @param sharedData: the stringified json data to be shared among different bots. A mandatory field "recipients" is a must. It specifies what all namespaces
	 *                    to share the data with.
	 */
	@JavascriptInterface
	public void sendSharedMessage(String cardObject, String hikeMessage, String sharedData)
	{
		sendSharedMessage(cardObject, hikeMessage, sharedData, mBotInfo);
	}

	/**
	 * Platform version 6
	 * Call this method to send a normal event.
	 *
	 * @param messageHash : the message hash that determines the uniqueness of the card message, to which the data is being sent.
	 * @param eventData   : the stringified json data to be sent. It should contain the following things :
	 *                       "cd" : card data, "increase_unread" : true/false, "notification" : the string to be notified to the user,
	 *                       "notification_sound" : true/ false, play sound or not, "rearrange_chat":true/false.
	 */
	@JavascriptInterface
	public void sendNormalEvent(String messageHash, String eventData)
	{
		try
		{
			JSONObject eventJson = new JSONObject(eventData);
			eventJson.put(HikePlatformConstants.PARENT_MSISDN, mBotInfo.getMsisdn());
			PlatformHelper.sendNormalEvent(messageHash, eventJson.toString(), mBotInfo.getNamespace(), mBotInfo);
		}
		catch (JSONException e)
		{	
			e.printStackTrace();
		}

	}
	
	/**
	* Platform Bridge Version 6
	* Call this method to post a status update without an image to timeline.
	*
	* @param status
	* @param moodId : Pass -1 if no mood
	*
	* Both status = null and moodId = -1 should not hold together
	*
	* 0, happy
	* 1, sad
	* 2, in_love
	* 3, surprised
	* 4, confused
	* 5, angry
	* 6, sleepy
	* 7, hungover
	* 8, chilling
	* 9, studying
	* 10, busy
	* 11, love
	* 12, middle_finger
	* 13, boozing
	* 14, movie
	* 15, caffeinated
	* 16, insomniac
	* 17, driving
	* 18, traffic
	* 19, late
	* 20, shopping
	* 21, gaming
	* 22, coding
	* 23, television
	* 33, music
	* 34, partying_hard
	* 35, singing
	* 36, eating
	* 37, working_out
	* 38, cooking
	* 39, beauty_saloon
	* 40, sick
	*
	*/
	@JavascriptInterface
	public void postStatusUpdate(String status, String moodId)
	{
		PlatformHelper.postStatusUpdate(status, moodId, null);
	}
	
	/**
	 * Platform Bridge Version 6
	 * Call this method to post a status update to timeline.
	 * 
	 * @param status
	 * @param moodId : Pass -1 if no mood
	 * @param imageFilePath : Path of the image on the client. Image should only be of jpeg format and compressed.
	 * 
	 * Status = null, moodId = -1 & imageFilePath = null should not hold together
	 * 
	 * 0, happy
	 * 1, sad
	 * 2, in_love
	 * 3, surprised
	 * 4, confused
	 * 5, angry
	 * 6, sleepy
	 * 7, hungover
	 * 8, chilling
	 * 9, studying
	 * 10, busy
	 * 11, love
	 * 12, middle_finger
	 * 13, boozing
	 * 14, movie
	 * 15, caffeinated
	 * 16, insomniac
	 * 17, driving
	 * 18, traffic
	 * 19, late
	 * 20, shopping
	 * 21, gaming
	 * 22, coding
	 * 23, television
	 * 33, music
	 * 34, partying_hard
	 * 35, singing
	 * 36, eating
	 * 37, working_out
	 * 38, cooking
	 * 39, beauty_saloon
	 * 40, sick
	 * 
	 */
	@JavascriptInterface
	public void postStatusUpdate(String status, String moodId, String imageFilePath)
	{
		PlatformHelper.postStatusUpdate(status,moodId,imageFilePath);
	}

	/**
	 * Platform Version 6
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * calling this method will forcefully block the full screen bot. The user won't see any messages in the bot after calling this.
	 *
	 * @param block : true to block the microapp false to unblock it.
	 * @param msisdn : the msisdn of the bot to be blocked/unblocked
	 */
	@JavascriptInterface
	public void blockBot(String block, String msisdn)
	{
		if (!BotUtils.isSpecialBot(mBotInfo) || !BotUtils.isBot(msisdn))
		{
			return;
		}
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		if (Boolean.valueOf(block))
		{
			botInfo.setBlocked(true);
			HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, msisdn);
		}

		else
		{
			botInfo.setBlocked(false);
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, msisdn);
		}
	}
	
	/**
	 * Platform Version 7 <br>
	 * This function is used for providing an ability to add a shortcut for a given bot.
	 */
	@JavascriptInterface
	public void addShortCut()
	{
		if (weakActivity.get() != null)
		{
			Utils.createShortcut(weakActivity.get(), mBotInfo, true);
		}
	}

	/**
	 * Platform Version 6
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * Call this method to know whether the bot pertaining to the msisdn is blocked or not.
	 * @param msisdn : the msisdn of the bot.
	 * @param id : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void isBotBlocked(String id, String msisdn)
	{
		if (!BotUtils.isSpecialBot(mBotInfo) || !BotUtils.isBot(msisdn))
		{
			return;
		}
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		callbackToJS(id, String.valueOf(botInfo.isBlocked()));
	}

	/**
	 * Platform Version 6
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * Call this method to know whether the bot pertaining to the msisdn is enabled or not.
	 * @param msisdn : the msisdn of the bot.
	 * @param id : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void isBotEnabled(String id, String msisdn)
	{
		if (!BotUtils.isSpecialBot(mBotInfo) || !BotUtils.isBot(msisdn))
		{
			return;
		}
		String value = String.valueOf(HikeConversationsDatabase.getInstance().isConversationExist(msisdn));
		callbackToJS(id, value);
	}

	/**
	 * Platform Version 6
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * Call this method to enable/disable bot. Enable means to show the bot in the conv list and disable is vice versa.
	 * @param msisdn :the msisdn of the bot.
	 * @param enable : send true to enable the bot in Conversation Fragment and false to disable.
	 */
	@JavascriptInterface
	public void enableBot(String msisdn, String enable)
	{
		enableBot(msisdn, enable, Boolean.toString(false));
	}
	/**
	 * Added in Platform Version:7
	 * 
	 *            Will call locationReceived function of JS . return a json {"gpsAvailable":true/false,"coords":{"longitude":,"latitude":}} MicroApp to Handle
	 *            timeout in case GPS tracking takes time.
	 * 
	 */

	@JavascriptInterface
	public void getLocation()
	{
		final GpsLocation gps = GpsLocation.getInstance();
		gps.getLocation(new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				HikeMessengerApp.getPubSub().publish(HikePubSub.LOCATION_AVAILABLE, gps.getLocationManager());
				gps.removeUpdates(this);
			}

			@Override
			public void onProviderDisabled(String provider) {
			}

			@Override
			public void onProviderEnabled(String provider) {
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
		});

	}

	/**
	 * Added in Platform Version:7
	 * 
	 * @param id
	 *            : : the id of the function that native will call to call the js . Get last store location,in case GPS is on,but unable to get location. return a json
	 *            {"gpsAvailable":true/false,"coords":{"longitude":,"latitude":}}
	 */
	@JavascriptInterface
	public void getLastStoredLocation(final String id)
	{
		LocationManager locationManager;
		Location location;
		Activity mContext = weakActivity.get();
		if (mContext != null)
		{
			locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
			location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			String latLong = PlatformUtils.getLatLongFromLocation(locationManager, location);
			callbackToJS(id, latLong);
		}

	}

	public void locationReceived(final String latLong)
	{
		if (mHandler == null)
		{
			return;
		}
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mWebView.loadUrl("javascript:locationReceived" + "('" + getEncodedDataForJS(latLong) + "')");
			}
		});
		
	}


	/**
	 * Platform Version 7
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * Call this method to mute/unmute bot.
	 * @param msisdn :the msisdn of the bot.
	 * @param mute : send true to mute the bot in Conversation Fragment and false to unmute.
	 */
	@JavascriptInterface
	public void muteBot(String msisdn, String mute)
	{

		if (!BotUtils.isSpecialBot(mBotInfo) || !BotUtils.isBot(msisdn))
		{
			return;
		}

		Boolean muteBot = Boolean.valueOf(mute);
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		botInfo.setIsMute(muteBot);
		HikeConversationsDatabase.getInstance().toggleMuteBot(msisdn, muteBot);
	}

	/**
	 * Platform Version 7
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * Call this method to know whether the bot pertaining to the msisdn is muted or not.
	 * @param msisdn : the msisdn of the bot.
	 * @param id : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void isBotMute(String id, String msisdn)
	{
		if (!BotUtils.isSpecialBot(mBotInfo) || !BotUtils.isBot(msisdn))
		{
			return;
		}

		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		callbackToJS(id, String.valueOf(botInfo.isMute()));
	}

	/**
	 * Platform Version 7
	 * Call this function to get the bot version.
	 * @param id: the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void getBotVersion(String id)
	{
		callbackToJS(id, String.valueOf(mBotInfo.getVersion()));
	}

	/**
	 * Platform Version 7
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * Call this function to get the bot version.
	 * @param id: the id of the function that native will call to call the js .
	 * returns -1 if bot not exists
	 */
	@JavascriptInterface
	public void getBotVersion(String id, String msisdn)
	{
        BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);

        if (botInfo == null || !BotUtils.isSpecialBot(mBotInfo) || !BotUtils.isBot(msisdn))
		{
			callbackToJS(id,"-1");
			return;
		}

		callbackToJS(id, String.valueOf(botInfo.getVersion()));
	}

	public void downloadStatus(final String id, final String progress)
	{
		if (mHandler == null)
		{
			return;
		}
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mWebView.loadUrl("javascript:downloadStatus" + "('" + id + "','" + progress + "')");
			}
		});

	}
	
	/**
	 * Platform Version 9
	 * This function is made for a bot to know whether its directory exists.
	 * @param id: the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void isMicroappExist(String id)
	{
        // Check for is Micro App exists in all of the directories path that are being used after the versioning release
        String microAppUnzipDirectoryPath = PlatformUtils.getMicroAppContentRootFolder();
		NonMessagingBotMetadata nonMessagingBotMetadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());

        File fileInMappsDirectory = new File(microAppUnzipDirectoryPath + PlatformContentConstants.HIKE_MAPPS + nonMessagingBotMetadata.getAppName());
        File fileInGamesDirectory = new File(microAppUnzipDirectoryPath + PlatformContentConstants.HIKE_GAMES + nonMessagingBotMetadata.getAppName());
        File fileInHikeWebMicroAppsDirectory = new File(microAppUnzipDirectoryPath + PlatformContentConstants.HIKE_WEB_MICRO_APPS + nonMessagingBotMetadata.getAppName());
        File fileInHikePopupsDirectory = new File(microAppUnzipDirectoryPath + PlatformContentConstants.HIKE_ONE_TIME_POPUPS + nonMessagingBotMetadata.getAppName());
        File fileInOldContentDirectory = new File(PlatformContentConstants.PLATFORM_CONTENT_OLD_DIR + nonMessagingBotMetadata.getAppName());

        if (fileInMappsDirectory.exists())
            callbackToJS(id, "true");
        else if(fileInGamesDirectory.exists())
            callbackToJS(id, "true");
        else if(fileInHikeWebMicroAppsDirectory.exists())
            callbackToJS(id, "true");
        else if(fileInHikePopupsDirectory.exists())
            callbackToJS(id, "true");
        else if(fileInOldContentDirectory.exists())
            callbackToJS(id, "true");
        else
            callbackToJS(id, "false");
	}

	/**
	 * Platform Version 10
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * Call this method to cancel the request that the Bot has initiated to do some http /https call.
	 * @param functionId : the id of the function that native will call to call the js .
	 * @param appName: the app name of the call that needs to be cancelled.
	 */
	@JavascriptInterface
	public void cancelRequest(String functionId, String appName)
	{
		if (!BotUtils.isSpecialBot(mBotInfo))
		{
			callbackToJS(functionId, "false");
			return;
		}

		PairModified<RequestToken, Integer> tokenCountPair = PlatformZipDownloader.getCurrentDownloadingRequests().get(appName);

		if (null != tokenCountPair && null != tokenCountPair.getFirst())
		{
			callbackToJS(functionId, "true");
			tokenCountPair.getFirst().cancel();
		}
		else
		{
			callbackToJS(functionId, "false");
		}
	}
	/**
	 * Platform Version 9
	 * Call this method to remove resume for an app
	 */
	@JavascriptInterface
	public void removeStateFile(String app)
	{
		if (!BotUtils.isSpecialBot(mBotInfo))
			return;
		File file = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + app+FileRequestPersistent.STATE_FILE_EXT);
		if (file.exists())
		{
			file.delete();
		}
	}

	/**
	 * Platform Version 9
	 * Call this method to delete a bot and remove its files
	 * Can only be called by special bots
	 * @param msisdn
	 */
	@JavascriptInterface
	public void deleteAndRemoveBot(String msisdn)
	{
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		if (!BotUtils.isSpecialBot(mBotInfo) || botInfo == null)
			return;
		NonMessagingBotMetadata nonMessagingBotMetadata = new NonMessagingBotMetadata(botInfo.getMetadata());

        // Json to remove micro app code from old micro app content path and from new structured versioning path
        JSONObject json = new JSONObject();
        try
        {
            // Generating app Names json array
            JSONArray appNameArray = new JSONArray();
            appNameArray.put(nonMessagingBotMetadata.getAppName());

            // Generating msisdn json array
            JSONArray msisdnArray = new JSONArray();
            msisdnArray.put(msisdn);

            json.put(HikePlatformConstants.APP_NAME, appNameArray);
            json.put(HikePlatformConstants.MSISDN, msisdnArray);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        BotUtils.removeMicroAppByAppName(json);
        BotUtils.removeMicroAppFromVersioningPathByMsisdn(json);

        // code to delete bot from conversations
        BotUtils.deleteBot(msisdn);
	}
	/**
	 * Platform Version 9
	 * Call this method to know if download request is currently running
	 * Can only be called by special bots
	 * @param appName
	 * @param functionId
	 * return true/false
	 */
	@JavascriptInterface
	public void isRequestRunning(String functionId,String appName)
	{
		if (!BotUtils.isSpecialBot(mBotInfo))
		{
			callbackToJS(functionId, "false");
			return;
		}

		PairModified<RequestToken, Integer> tokenCountPair = PlatformZipDownloader.getCurrentDownloadingRequests().get(appName);

		if (null != tokenCountPair && null != tokenCountPair.getFirst() && tokenCountPair.getFirst().isRequestRunning())
		{
			callbackToJS(functionId, "true");
		}
		else
		{
			callbackToJS(functionId, "false");
		}
	}

	/**
	 * Platform Version 9
	 * Call this method to open the gallery view to select a file.
	 * @param id
	 * @param displayCameraItem : Whether or not to display the camera item in the gallery view.
	 */
	@JavascriptInterface
	public void chooseFile(final String id, final String displayCameraItem)
	{
		Logger.d("FileUpload", "input Id chooseFile is " + id);

		if (null == mHandler)
		{
			Logger.e("FileUpload", "mHandler is null");
			return;
		}

		PlatformHelper.chooseFile(id, displayCameraItem, weakActivity.get());
	}

	/**
	 * Platform bridge Version 8
	 * Call this function to open a full page webView within hike. Calling this function will create full page with action bar
	 * color specified by server, js injected to remove unwanted features from the full page, and URLs defined by the interceptUrlJson
	 * will be intercepted when they start loading.
	 * @param title
	 *            : the title on the action bar.
	 * @param url
	 *            : the url that will be loaded.
	 * @param interceptUrlJson
	 * 			  : the JSON String that contains the interception URL and type.
	 * 			    If a loading url contains the String value of the "url" field, it will be intercepted.
	 * 			    eg - {"icpt_url":[{"url":"ndtv","type":1},{"url":"techinsider.com","type":1}]}
	 * 			    URL http://www.ndtv.com/news?txId=1234&authId=12345&key1=val1&key2=val2
	 * 			    will be intercepted and parameter String ?txId=1234&authId=12345&key1=val1&key2=val2 will be returned to the microapp
	 * 			    in the urlIntercepted method.
	 *
	 * 			    Type 1 : Closes the current WebView and opens the microapp that invoked it, with the URL parameters from the
	 * 			    		 intercepted URL.
	 * @param backToActivity TODO
	 */
	@JavascriptInterface
	
	public void openFullPage(String title, String url, String interceptUrlJson)
	{
		openFullPage( title,  url,  interceptUrlJson,"false");
	}
	/**
	 * Platform bridge Version 11
	 * Call this function to open a full page webView within hike. Calling this function will create full page with action bar
	 * color specified by server, js injected to remove unwanted features from the full page, and URLs defined by the interceptUrlJson
	 * will be intercepted when they start loading.
	 * @param title
	 *            : the title on the action bar.
	 * @param url
	 *            : the url that will be loaded.
	 * @param interceptUrlJson
	 * 			  : the JSON String that contains the interception URL and type.
	 * 			    If a loading url contains the String value of the "url" field, it will be intercepted.
	 * 			    eg - {"icpt_url":[{"url":"ndtv","type":1},{"url":"techinsider.com","type":1}]}
	 * 			    URL http://www.ndtv.com/news?txId=1234&authId=12345&key1=val1&key2=val2
	 * 			    will be intercepted and parameter String ?txId=1234&authId=12345&key1=val1&key2=val2 will be returned to the microapp
	 * 			    in the urlIntercepted method.
	 *
	 * 			    Type 1 : Closes the current WebView and opens the microapp that invoked it, with the URL parameters from the
	 * 			    		 intercepted URL.
	 * @param backToActivity : "true"/"false"-- Depends whether on back press, activity wants to kill itself or not
	 */
	@JavascriptInterface
	
	public void openFullPage(String title, String url, String interceptUrlJson, String backToActivity)
	{
		if (TextUtils.isEmpty(title))
		{
			sendMessageToUiThread(OPEN_FULL_PAGE, url);
		}
		else if (TextUtils.isEmpty(interceptUrlJson))
		{
			sendMessageToUiThread(OPEN_FULL_PAGE_WITH_TITLE, new String[] { title, url, null, null });
		}
		else
		{
			sendMessageToUiThread(OPEN_FULL_PAGE_WITH_TITLE, new String[] { title, url, interceptUrlJson,backToActivity });
		}
	}

	public void urlIntercepted(String urlParams)
	{
		mWebView.loadUrl("javascript:urlIntercepted('" + urlParams + "')");
	}

	public void setExtraData(String data)
	{
		this.extraData = data;
	}

	/**
	 * Platform Version 9
	 * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
	 * Call this method to enable/disable bot. Enable means to show the bot in the conv list and disable is vice versa.
	 * @param msisdn :the msisdn of the bot.
	 * @param enable : send true to enable the bot in Conversation Fragment and false to disable.
	 * @param increaseUnread
	 */
	@JavascriptInterface
	public void enableBot(String msisdn, String enable, String increaseUnread)
	{

		if (!BotUtils.isSpecialBot(mBotInfo) || !BotUtils.isBot(msisdn))
		{
			return;
		}

		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);

		boolean enableBot = Boolean.valueOf(enable);
		boolean increaseUnreadCount = Boolean.valueOf(increaseUnread);
		if (enableBot)
		{
				PlatformUtils.enableBot(botInfo, true, increaseUnreadCount);
		}
		else
		{
			BotUtils.deleteBotConversation(msisdn, false);
		}
	}
	/**
	 * Platform Version 10
	 *This function allows for a bot to send logs after it has been enabled
	 */
	@JavascriptInterface
	public void sendLogs()
	{
		Activity mContext = weakActivity.get();
		if(mContext==null)
		{
			return;
		}
		SendLogsTask logsTask = new SendLogsTask(mContext);
		logsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Platform Version 11 This function is made to know, if any game is running and accordingly display the running status on games channel Call this method to get the current
	 * game name running in hike. Gameid is empty, if no game is running
	 * 
	 * @param id
	 *            : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void getRunningGame(String id)
	{
		Activity context = weakActivity.get();
		if (context != null)
		{
			String gameId = PlatformUtils.getRunningGame(context);
			callbackToJS(id, gameId);
		}
	}

    /**
     * Platform Version 11
     * Call this function to get the bot mAppVersionCode.
     * @param id: the id of the function that native will call to call the js .
     */
    @JavascriptInterface
    public void getMicroAppVersionCode(String id)
    {
        callbackToJS(id, String.valueOf(mBotInfo.getMAppVersionCode()));
    }

    /**
     * Platform Version 11
     * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
     * Call this function to get the mAppVersionCode for asked msisdn.
     * @param id: the id of the function that native will call to call the js .
     * @param msisdn: the msisdn of the bot for which micro app version code is required.
     * returns -1 if bot not exists
     */
    @JavascriptInterface
    public void getMicroAppVersionCode(String id, String msisdn)
    {
        BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);

        if (botInfo == null || !BotUtils.isSpecialBot(mBotInfo) || !BotUtils.isBot(msisdn) )
        {
            callbackToJS(id,"-1");
            return;
        }

        callbackToJS(id, String.valueOf(botInfo.getMAppVersionCode()));

    }

    /**
     * Platform Version 11
     * This function is made for the special Shared bot that has the information about some other bots as well, and acts as a channel for them.
     * Call this function to get the mAppVersionCode for asked appName.
     * @param id: the id of the function that native will call to call the js .
     * @param appName: the appName of the sdk that you require version code for.
     * returns -1 if caller of the method is not a special bot
     */
    @JavascriptInterface
    public void getSDKVersionCode(String id, String appName)
    {
        if (!BotUtils.isSpecialBot(mBotInfo))
        {
            callbackToJS(id,"-1");
            return;
        }

        int gameEngineMappVersionCode = 0;

        if(HikeMessengerApp.hikeMappInfo.containsKey(appName))
            gameEngineMappVersionCode = HikeMessengerApp.hikeMappInfo.get(appName);

        callbackToJS(id, String.valueOf(gameEngineMappVersionCode));
    }


	private class PlatformPostListener implements IRequestListener
	{
		String id;
		String urlKey;
		JSONObject data;
		int current_count;
		private int tokenLife;
		public PlatformPostListener(String id, String urlKey, final JSONObject data, final int count, int tokenLife)
		{
			this.id =id;
			this.urlKey =urlKey;
			this.data=data;
			this.current_count = count;
			this.tokenLife = tokenLife;
		}
		@Override
		public void onRequestFailure(HttpException httpException) {
			Logger.e("NonMessagingJavascriptBridge", "Error while parsing success request: "+httpException.getErrorCode()+" : "+httpException.getMessage());
			if (httpException.getErrorCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
			{
				AuthListener authListener = new AuthListener()
				{
					@Override
					public void onTokenResponse(String authToken)
					{
						Logger.d("NonMessagingJavascriptBridge", "Again trying infra url");
						doInfraPost(id, urlKey, data, --current_count);
					}

					@Override
					public void onTokenErrorResponse(String error)
					{

					}
				};
				PlatformUtils.requestAuthToken(mBotInfo, authListener, tokenLife);
			}
		}

		@Override
		public void onRequestSuccess(Response result) {
			Logger.d("JavascriptBridge", "microapp request success with code " + result.getStatusCode());
			JSONObject success = new JSONObject();
			try
			{
				success.put(HikePlatformConstants.STATUS, HikePlatformConstants.SUCCESS);
				success.put(HikePlatformConstants.STATUS_CODE, result.getStatusCode());
				success.put(HikePlatformConstants.RESPONSE, result.getBody().getContent());
			}
			catch (JSONException e)
			{
				Logger.e("JavascriptBridge", "Error while parsing success request");
				e.printStackTrace();
			}
			callbackToJS(id, String.valueOf(success));
		}

		@Override
		public void onRequestProgressUpdate(float progress) {

		}

	}

	/**
	 * Platform Version 11
	 */
	@JavascriptInterface
	public void doInfraPostinit(final String id, String urlKey, String data)
	{
		try
		{
			JSONObject jsonData = new JSONObject(data);
			doInfraPost(id, urlKey, jsonData, MAX_COUNT);
		}
		catch (JSONException ex)
		{

		}
	}

	public void doInfraPost(final String id, final String urlKey, final JSONObject data, final int count)
	{
		if (count <= 0)
		{
			return;
		}
		Cursor cursor = HikeConversationsDatabase.getInstance().getURL(urlKey);
		if(cursor == null){
			callbackToJS(id, "Invalid Key");
			return;
		}
		final String url  = Utils.decrypt(cursor.getString(cursor.getColumnIndex(DBConstants.URL)));
		if (TextUtils.isEmpty(url))
		{
			callbackToJS(id, "Invalid Key");
			return;
		}
		int tokenLife = cursor.getInt(cursor.getColumnIndex(DBConstants.LIFE));
		if(tokenLife == DBConstants.LONG_LIVED) {
			final String oAuth = HikeContentDatabase.getInstance().getTokenForMicroapp(mBotInfo.getMsisdn());
			if (TextUtils.isEmpty(oAuth)) {
				Logger.d("NonMessagingJavascriptBridge", "Fetching auth token as its not saved earlier");
				fetchToken(id, urlKey, data, count, url,tokenLife);
			} else {
				makePlatformPostRequest(id, url, data, oAuth, urlKey, count,tokenLife);
			}
		}else{
			Logger.d("NonMessagingJavascriptBridge", "Fetching auth token as its short lived");
			fetchToken(id, urlKey, data, count, url,tokenLife);
		}
	}
    private void fetchToken(final String id, final String urlKey, final JSONObject data, final int count, final String url, final int tokenLife){
		AuthListener authListener = new AuthListener() {
			@Override
			public void onTokenResponse(String authToken) {
				makePlatformPostRequest(id, url, data, authToken, urlKey, count,tokenLife);
			}

			@Override
			public void onTokenErrorResponse(String error) {

			}
		};
		PlatformUtils.requestAuthToken(mBotInfo, authListener,tokenLife);
	}
	private void makePlatformPostRequest(String id, String url, JSONObject json, String oAuth, String urlKey, int count, int tokenLife)
	{
		RequestToken token = HttpRequests.platformPostRequest(url, json, PlatformUtils.getHeaderForOauth(oAuth), new PlatformPostListener(id, urlKey, json, count,tokenLife));

		if (!token.isRequestRunning())
		{
			token.execute();
		}
	}
	/**
	 * Call tis method to set alarm.
	 * @param json {"notification_sound":true,"increase_unread":true,"alarm_data":{},"notification":"test notif","rearrange_chat":true}
	 * @param timeInMills
	 * @param persistent
	 */
	@JavascriptInterface
	public void setAlarm(String inputJson, String timeInMills,String persistent)
	{
		JSONObject json = null;
		try
		{
			json = new JSONObject(inputJson);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		String msisdn = mBotInfo.getMsisdn();
		Activity mContext = weakActivity.get();
		if(TextUtils.isEmpty(msisdn) || mContext == null)
		{
			return;
		}
		NonMessagingBotAlarmManager.setAlarm(mContext, json, msisdn, Long.valueOf(timeInMills).longValue(), Boolean.valueOf(persistent));
	}

	/**
	 * Platform Version 11
	 * Call this function to cancel the alarm data associtated with a particular alarm data
	 * @param alarmData
	 */
	@JavascriptInterface
	public void cancelAlarm(String alarmData)
	{
		if(mBotInfo ==  null || weakActivity == null || TextUtils.isEmpty(mBotInfo.getMsisdn()))
		{
			return;
		}
		HikeAlarmManager.cancelAlarm(weakActivity.get(), (mBotInfo.getMsisdn().hashCode() + alarmData.hashCode()));
	}
	/**
	 * Platform Version 11
	 * Method to update last message
	 */
	@JavascriptInterface
	public void updateLastMessage(String message)
	{
		if (!TextUtils.isEmpty(message) && mBotInfo !=null)
		{
			HikeConversationsDatabase.getInstance().updateLastMessageForNonMessagingBot(mBotInfo.getMsisdn(), message);
			HikeConversationsDatabase.getInstance().updateLastMessageStateAndCount(mBotInfo.getMsisdn(), ConvMessage.State.RECEIVED_READ.ordinal());
			// Saving lastConvMessage in memory as well to refresh the UI
			mBotInfo.setLastConversationMsg(Utils.makeConvMessage(mBotInfo.getMsisdn(), message, true, ConvMessage.State.RECEIVED_READ));
		}
	}
	/**
	 * Platform Version 12
	 * Method to log analytics according to new taxonomy
	 * unique key and kingdom are compulsory
	 * @param json in the new taxonomy
	 */
	@JavascriptInterface
	public void logAnalyticsV2(String json) {
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(json);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		PlatformHelper.logAnaLyticsV2(json, mBotInfo.getConversationName(), mBotInfo.getMsisdn(), jsonObject.optString(AnalyticsConstants.V2.UNIQUE_KEY), jsonObject.optString(AnalyticsConstants.V2.KINGDOM), mBotInfo.getMAppVersionCode());
	}
	/**
	 * Platform Version 12
	 * Method to get list of children bots
	 */
	@JavascriptInterface
	public void getChildrenBots(String id)
	{
		try {
			if (!TextUtils.isEmpty(id) && mBotInfo !=null)
            {
                String childrenBotInformation = PlatformHelper.getChildrenBots(mBotInfo.getMsisdn());
				callbackToJS(id, childrenBotInformation);
				return;
            }
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		callbackToJS(id, "[]");
	}

	/**
	 * Platform Version 12
	 * Method to get bot information as string
	 */
	@JavascriptInterface
	public void getBotInfoAsString(String id)
	{
		try {
			if (!TextUtils.isEmpty(id) && mBotInfo !=null)
			{
				String childrenBotInformation = BotUtils.getBotInfoAsString(mBotInfo).toString();
				callbackToJS(id, childrenBotInformation);
				return;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		callbackToJS(id, "{}");
	}
}
