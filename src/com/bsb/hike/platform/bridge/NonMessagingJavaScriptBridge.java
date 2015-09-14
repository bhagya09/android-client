package com.bsb.hike.platform.bridge;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.bots.NonMessagingBotConfiguration;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.GpsLocation;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformHelper;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.WebViewActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * API bridge that connects the javascript to the non-messaging Native environment. Make the instance of this class and add it as the
 * JavaScript interface of the MicroApp WebView.
 * This class caters Platform Bridge version 0
 *
 * Platform Bridge Version Start = 1
 * Platform Bridge Version End = ~
 */
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
		PlatformHelper.logAnalytics(isUI, subType, json,mBotInfo);
	}

	@Override
	@JavascriptInterface
	public void onLoadFinished(String height)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
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
		PlatformHelper.forwardToChat(json, hikeMessage,mBotInfo,weakActivity.get());
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

			
			mWebView.loadUrl("javascript:init('"+getEncodedDataForJS(jsonObject.toString())+"')");
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
		mBotInfo.setMute(!mBotInfo.isMute());
		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_BOT, mBotInfo.getMsisdn());
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
		PlatformHelper.putInCache(key, value,mBotInfo.getNamespace());
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
		String value = PlatformHelper.getFromCache(key,mBotInfo.getNamespace());
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
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
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
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
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
				mCallback.openFullPageWithTitle(params[1], params[0]); // Url, Title
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
		if (TextUtils.isEmpty(title))
		{
			sendMessageToUiThread(OPEN_FULL_PAGE, url);
		}
		else
		{
			sendMessageToUiThread(OPEN_FULL_PAGE_WITH_TITLE, new String[] { title, url });
		}
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
	@JavascriptInterface
	public void chooseFile(final String id)
	{	
		Logger.d("FileUpload","input Id chooseFile is "+ id);

		
	
		if (null == mHandler)
		{
			Logger.e("FileUpload", "mHandler is null");
			return;
		}
		
		
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{	Context weakActivityRef=weakActivity.get();
				if (weakActivityRef != null)
				{	
					int galleryFlags =GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS|GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM;
					Intent galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(weakActivityRef, galleryFlags,null);
					galleryPickerIntent.putExtra(GalleryActivity.START_FOR_RESULT, true);
					galleryPickerIntent.putExtra(HikeConstants.CALLBACK_ID,id);
					((WebViewActivity) weakActivityRef). startActivityForResult(galleryPickerIntent, HikeConstants.PLATFORM_FILE_CHOOSE_REQUEST);
					}
			}
		});
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
		if (TextUtils.isEmpty(eventId))
		{
			Logger.e(TAG, "event can't be deleted as the event id is " + eventId);
			return;
		}
		HikeConversationsDatabase.getInstance().deleteEvent(eventId);
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
		if (TextUtils.isEmpty(messageHash))
		{
			Logger.e(TAG, "the events corresponding to the message hash can't be deleted as the message hash is " + messageHash);
			return;
		}
		HikeConversationsDatabase.getInstance().deleteAllEventsForMessage(messageHash);
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
		String eventData =PlatformHelper.getAllEventsForMessageHash(messageHash,mBotInfo.getNamespace());
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
	 *                       "notification_sound" : true/ false, play sound or not.
	 */
	@JavascriptInterface
	public void sendNormalEvent(String messageHash, String eventData)
	{
		try
		{
			JSONObject eventJson = new JSONObject(eventData);
			eventJson.put(HikePlatformConstants.PARENT_MSISDN, mBotInfo.getMsisdn());
			PlatformHelper.sendNormalEvent(messageHash, eventJson.toString(), mBotInfo.getNamespace());
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
		postStatusUpdate(status, moodId, null);
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
		int mood;
		
		try
		{
			mood = Integer.parseInt(moodId);
		}
		catch(NumberFormatException e)
		{
			Logger.e(tag, "moodId to postStatusUpdate should be a number.");
			mood = -1;
		}
		
		Utils.postStatusUpdate(status, mood, imageFilePath);
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
		if (!BotUtils.isBot(msisdn))
		{
			return;
		}
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
		if (!metadata.isSpecialBot())
		{
			Logger.e(TAG, "the bot is not a special bot and only special bot has the authority to call this function.");
			return;
		}
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
	 * Platform Version 6 <br>
	 * This function is used for providing an ability to add a shortcut for a given bot.
	 * 
	 * @param msisdn
	 *            - The msisdn of the bot whose shortcut is to be created
	 */
	@JavascriptInterface
	public void addShortCut()
	{
		if (weakActivity.get() != null)
		{
			Utils.createShortcut(weakActivity.get(), mBotInfo);
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
		if (!BotUtils.isBot(msisdn))
		{
			return;
		}
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
		if (!metadata.isSpecialBot())
		{
			Logger.e(TAG, "the bot is not a special bot and only special bot has the authority to call this function.");
			return;
		}
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
		if (!BotUtils.isBot(msisdn))
		{
			callbackToJS(id, "false");
			return;
		}
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
		if (!metadata.isSpecialBot())
		{
			Logger.e(TAG, "the bot is not a special bot and only special bot has the authority to call this function.");
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
	 * @param enable : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void enableBot(String msisdn, String enable)
	{
		if (!BotUtils.isBot(msisdn))
		{
			return;
		}
		BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
		if (!metadata.isSpecialBot())
		{
			Logger.e(TAG, "the bot is not a special bot and only special bot has the authority to call this function.");
			return;
		}

		boolean enableBot = Boolean.valueOf(enable);
		if (enableBot)
		{
			PlatformUtils.enableBot(botInfo, true);
		}
		else
		{
			BotUtils.deleteBotConversation(msisdn, false);
		}
	}
	/**
	 * Added in Platform Version:6
	 * 
	 *            Will call locationReceived function of JS . return a json {"gpsAvailable":true/false,"coords":{"longitude":,"latitude":}} MicroApp to Handle
	 *            timeout in case GPS tracking takes time.
	 * 
	 */

	@JavascriptInterface
	public void getLocation()
	{
		GpsLocation gps = GpsLocation.getInstance();
		gps.getLocation();

	}

	
	/**
	 * Added in Platform Version:6
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
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mWebView.loadUrl("javascript:locationReceived" + "('" + getEncodedDataForJS(latLong) + "')");
			}
		});
		
	}


}