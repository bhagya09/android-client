package com.bsb.hike.platform.bridge;

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
	 * this function will update the helper data. It will replace the key if it is present in the helper data and will add it if it is not present in the helper data.
	 * 
	 * @param json
	 */
	@JavascriptInterface
	public void updateHelperData(String json)
	{
		Logger.i(tag, "update metadata called " + json + " , MicroApp msisdn : " + mBotInfo.getMsisdn());
		String originalmetadata = HikeConversationsDatabase.getInstance().getMetadataOfBot(mBotInfo.getMsisdn());
		originalmetadata = PlatformUtils.updateHelperData(json, originalmetadata);
		if (originalmetadata != null)
		{
			HikeConversationsDatabase.getInstance().updateMetadataOfBot(mBotInfo.getMsisdn(), originalmetadata);
		}

	}

	/**
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
	 * calling this method will forcefully mute the full screen bot. The user won't receive any more notifications after calling this.
	 */
	@JavascriptInterface
	public void mute()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_BOT, mBotInfo.getMsisdn());
	}

	/**
	 * calling this method will forcefully block the full screen bot. The user won't see any messages in the chat thread after calling this.
	 * 
	 * @param : true to block the microapp false to unblock it.
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
	 * call this function to delete the entire notif data of the microApp.
	 */
	@JavascriptInterface
	public void deleteAllNotifData()
	{
		HikeConversationsDatabase.getInstance().deleteAllNotifDataForMicroApp(mBotInfo.getMsisdn());
	}

	/**
	 * Call this function to delete partial notif data pertaining to a microApp. The key is the timestamp provided by Native
	 * @param key
	 */
	@JavascriptInterface
	public void deletePartialNotifData(String key)
	{
		HikeConversationsDatabase.getInstance().deletePartialNotifData(key, mBotInfo.getMsisdn());
	}

	/**
	 * call this function to send notif data to js. Will be primarily used when the bot is in foreground and notif is received.
	 * @param notifData : notif data to be sent to the js.
	 */
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
}
