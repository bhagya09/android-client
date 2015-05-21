package com.bsb.hike.platform.bridge;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.NonMessagingBotConfiguration;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPUtils;

import java.util.Iterator;

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
	
	private BotInfo mBotInfo;
	
	private static final String TAG  = "NonMessagingJavaScriptBridge";
	
	public NonMessagingJavaScriptBridge(Activity activity, CustomWebView mWebView, BotInfo botInfo)
	{
		super(activity, mWebView);
		this.mBotInfo = botInfo;
	}

	@Override
	@JavascriptInterface
	public void logAnalytics(String isUI, String subType, String json)
	{
		//TODO log analytics
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
			JSONObject oldHelperDataJson = new JSONObject(oldHelper);
			Iterator<String> i = oldHelperDataJson.keys();
			while (i.hasNext())
			{
				String key = i.next();
				oldHelperDataJson.put(key, oldHelperDataJson.get(key));
			}

			mBotInfo.setHelperData(oldHelperDataJson.toString());
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
	 *            : if the data has changed , then send the updated fields and it will update the metadata. If the key is already present, it will be replaced else it will be added
	 *            to the existent metadata. If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already
	 *            present and will add the key-value pair if the key is not present in the existent metadata.
	 */
	@JavascriptInterface
	public void forwardToChat(String json, String hikeMessage)
	{
		Logger.i(TAG, "Received this json in forward to chat : " + json + "\n Received this hm : " + hikeMessage);
		
		if (TextUtils.isEmpty(json) || TextUtils.isEmpty(hikeMessage))
		{
			Logger.e(TAG, "Received a null or empty json/hikeMessage in forward to chat");
			return;
		}
		
		try
		{
			BotInfo botInfo = BotInfo.getBotInfoForBotMsisdn(mBotInfo.getMsisdn());
			NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(botInfo.getMetadata());
			JSONObject cardObj = new JSONObject(json);

			/**
			 * Blindly inserting the appName in the cardObj JSON.
			 */
			cardObj.put(HikePlatformConstants.APP_NAME, metadata.getAppName());
			cardObj.put(HikePlatformConstants.APP_PACKAGE, metadata.getAppPackage());
			metadata.setCardObj(cardObj);
			
			ConvMessage message = PlatformUtils.getConvMessageFromJSON(metadata.getJson(), hikeMessage);
			
			if (message != null)
			{
				startComPoseChatActivity(message);
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void init()
	{
		JSONObject jsonObject = new JSONObject();
		try
		{
			NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
			getInitJson(jsonObject, mBotInfo.getMsisdn());
			jsonObject.put(HikePlatformConstants.HELPER_DATA, metadata.getHelperData());
			jsonObject.put(HikePlatformConstants.NOTIF_DATA, mBotInfo.getNotifData());
			jsonObject.put(HikePlatformConstants.BLOCK, Boolean.toString(mBotInfo.isBlocked()));
			jsonObject.put(HikePlatformConstants.MUTE, Boolean.toString(mBotInfo.isMute()));
			jsonObject.put(HikePlatformConstants.NETWORK_TYPE, Integer.toString(VoIPUtils.getConnectionClass(HikeMessengerApp.getInstance().getApplicationContext()).ordinal()));
			

			mWebView.loadUrl("javascript:init('" + jsonObject.toString() + "')");
		}
		catch (JSONException e)
		{
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
			HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, mBotInfo.getMsisdn());
		}
		
		else
		{
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
		HikeContentDatabase.getInstance().putInContentCache(key, mBotInfo.getNamespace(), value);
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
	 	String value = HikeContentDatabase.getInstance().getFromContentCache(key, mBotInfo.getNamespace());
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
	 * @param key
	 */
	@JavascriptInterface
	public void deletePartialNotifData(String key)
	{
		HikeConversationsDatabase.getInstance().deletePartialNotifData(key, mBotInfo.getMsisdn());
	}


	public void notifDataReceived(final String notifData)
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
				mWebView.loadUrl("javascript:notifDataReceived" + "('" + notifData + "')");
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

	/**
	 *  Platform Bridge Version 1
	 * call this function for any post call. The call is gonna be fire and forget. MicroApp will not receive any response as this
	 * request is a fire and forget request.
	 * @param url: the url that will be called.
	 * @param params: the push params to be included in the body.
	 */
	@JavascriptInterface
	public void fireAndForgetPostRequest(String url, String params)
	{
		try
		{
			RequestToken token = HttpRequests.microAppPostRequest(url, new JSONObject(params));
			if (!token.isRequestRunning())
			{
				token.execute();
			}
		}
		catch (JSONException e)
		{
			Logger.e(tag, "error in JSON");
			e.printStackTrace();
		}
	}


}
