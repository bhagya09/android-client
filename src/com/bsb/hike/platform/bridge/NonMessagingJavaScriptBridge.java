package com.bsb.hike.platform.bridge;

import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeContentDatabase;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

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
			JSONObject cardObj = new JSONObject(json);
			/**
			 * Blindly inserting the appName in the cardObj JSON.
			 */
			cardObj.put(HikePlatformConstants.APP_NAME, BotInfo.getBotInfoForBotMsisdn(mBotInfo.getMsisdn()).getMicroAppName());
			
			ConvMessage message = getConvMessageFromJSON(cardObj, hikeMessage);
			
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
			jsonObject.put(HikeConstants.MSISDN, mBotInfo.getMsisdn());
			jsonObject.put(HikePlatformConstants.HELPER_DATA, metadata.getHelperData());
			jsonObject.put(HikePlatformConstants.PLATFORM_USER_ID,HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING,null) );
			jsonObject.put(HikeConstants.APP_VERSION, AccountUtils.getAppVersion());

			mWebView.loadUrl("javascript:init('" + jsonObject.toString() + "')");
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private ConvMessage getConvMessageFromJSON(JSONObject cardObj, String text)
	{
		try
		{
			ConvMessage convMessage = new ConvMessage();
			convMessage.setMetadata(cardObj);
			convMessage.setMessage(text);
			convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT);
			return convMessage;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return null;
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
	 */
	@JavascriptInterface
	public void block()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, mBotInfo.getMsisdn());
	}

	public void onBackPressed()
	{
		mWebView.loadUrl("javascript:onBackPressed()");
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
	 * @param functionName : the function name that native will call to give the cache back to js.
	 */
	@JavascriptInterface
	public void getLargeDataFromCache(String functionName)
	{
		String value = HikeContentDatabase.getInstance().getFromContentCache(mBotInfo.getNamespace(), mBotInfo.getNamespace());
		callbackToJS(functionName, value);
	}

	/**
	 * call this function to get the data from the native memory
	 * @param functionName
	 * @param key
	 */
	@JavascriptInterface
	public void getFromCache(String functionName, String key)
	{
	 	String value = HikeContentDatabase.getInstance().getFromContentCache(key, mBotInfo.getNamespace());
		callbackToJS(functionName, value);
	}



}
