package com.bsb.hike.platform.bridge;

import java.util.ArrayList;

import com.bsb.hike.db.HikeContentDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;
import android.webkit.JavascriptInterface;
import android.widget.BaseAdapter;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformAlarmManager;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.platform.WebViewCardRenderer.WebViewHolder;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * API bridge that connects the javascript to the Native environment. Make the instance of this class and add it as the JavaScript interface of the Card WebView.
 */

public class MessagingBotJavaScriptBridge extends JavascriptBridge
{

	public static final String tag = "platformbridge";
	private static final int UPDATE_MESSAGE = 1;
	
	
	protected SparseArray<WebMetadata> metadataMap = new SparseArray<>();
	
	public static interface WebviewEventsListener{
		public void loadFinished(ConvMessage message);
		
		public void notifyDataSetChanged(); 
	}
	
	
	ConvMessage message;

	JSONObject profilingTime;
	BaseAdapter adapter;
	WebviewEventsListener listener;

	public MessagingBotJavaScriptBridge(Activity activity,CustomWebView mWebView)
	{
		super(activity,mWebView);
	}

	public MessagingBotJavaScriptBridge(Activity activity,CustomWebView webView, ConvMessage convMessage, BaseAdapter adapter)
	{
		super(activity,webView);
		this.message = convMessage;
		this.adapter = adapter;
	}


	/**
	 * Call this function to log analytics events.
	 *
	 * @param isUI    : whether the event is a UI event or not. This is a string. Send "true" or "false".
	 * @param subType : the subtype of the event to be logged, eg. send "click", to determine whether it is a click event.
	 * @param json    : any extra info for logging events, including the event key that is pretty crucial for analytics.
	 */
	@JavascriptInterface
	public void logAnalytics(String isUI, String subType, String json)
	{

		try
		{
			String msisdn = message.getMsisdn();
			JSONObject jsonObject = new JSONObject(json);
			jsonObject.put(AnalyticsConstants.CHAT_MSISDN, msisdn);
			jsonObject.put(AnalyticsConstants.ORIGIN, Utils.conversationType(msisdn));
			jsonObject.put(HikePlatformConstants.CARD_TYPE, message.webMetadata.getAppName());
			jsonObject.put(AnalyticsConstants.CONTENT_ID, message.getContentId());
			if (Boolean.valueOf(isUI))
			{
				HikeAnalyticsEvent.analyticsForCards(AnalyticsConstants.MICROAPP_UI_EVENT, subType, jsonObject);
			}
			else
			{
				HikeAnalyticsEvent.analyticsForCards(AnalyticsConstants.MICROAPP_NON_UI_EVENT, subType, jsonObject);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}
	}

	

	

	
	
	
	protected void updateMetadata(WebMetadata metadata, String notifyScreen)
	{
		if (metadata!=null && notifyScreen != null && Boolean.valueOf(notifyScreen))
		{
			if (null == mHandler)
			{
				return;
			}
			mHandler.post(new Runnable()
			{

				@Override
				public void run()
				{
					if(listener!=null)
					{
						listener.notifyDataSetChanged();
					}
				}
			});
		}
	}

	

	/**
	 * calling this method will forcefully mute the chat thread. The user won't receive any more
	 * notifications after calling this.
	 */
	@JavascriptInterface
	public void muteChatThread()
	{

		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_BOT, message.getMsisdn());

	}

	/**
	 * calling this method will forcefully block the chat thread. The user won't see any messages in the
	 * chat thread after calling this.
	 */
	@JavascriptInterface
	public void blockChatThread()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, message.getMsisdn());
	}

	@JavascriptInterface
	public void share()
	{
		share(null, null);
	}

	
	
	public void setListener(WebviewEventsListener listener)
	{
		this.listener = listener;
	}
	
	public void setData()
	{
		mWebView.loadUrl("javascript:setData('" + message.getMsisdn() + "','" + message.webMetadata.getHelperData().toString() + "','" + message.isSent() + "','" +
				HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING,null) + "','" + AccountUtils.getAppVersion() + "')");
	}

	public void init()
	{
		JSONObject jsonObject = new JSONObject();
		try
		{
			jsonObject.put(HikeConstants.MSISDN, message.getMsisdn());
			jsonObject.put(HikePlatformConstants.HELPER_DATA, message.webMetadata.getHelperData());
			jsonObject.put(HikePlatformConstants.IS_SENT, message.isSent());
			jsonObject.put(HikePlatformConstants.PLATFORM_USER_ID,HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PLATFORM_UID_SETTING,null) );
			jsonObject.put(HikePlatformConstants.APP_VERSION, AccountUtils.getAppVersion());

			jsonObject.put(HikePlatformConstants.PROFILING_TIME, profilingTime);
			mWebView.loadUrl("javascript:init('" + jsonObject.toString() + "')");
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public void alarmPlayed(String alarmData)
	{
		mWebView.loadUrl("javascript:alarmPlayed(" + "'" + alarmData + "')");
	}

	public void updateProfilingTime(JSONObject profilingTime)
	{
		this.profilingTime = profilingTime;
	}

	/**
	 * Update this conv message on java bridge thread
	 * @param message
	 */
	public void updateConvMessage(ConvMessage message)
	{
			this.message = message;
			WebMetadata metadata = metadataMap.get((int)message.getMsgID());
			if( metadata !=null )
			{
				this.message.webMetadata = metadata;
				metadataMap.remove((int) message.getMsgID());
			}
	}

	
	/**
	 * Calling this function will initiate forward of the message to a friend or group.
	 *
	 * @param json : if the data has changed , then send the updated fields and it will update the metadata.
	 *             If the key is already present, it will be replaced else it will be added to the existent metadata.
	 *             If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already present
	 *             and will add the key-value pair if the key is not present in the existent metadata.
	 */
	@JavascriptInterface
	public void forwardToChat(String json)
	{
		try
		{
			Logger.i(tag, "forward to chat called " + json + " , message id=" + message.getMsgID());

			if (!TextUtils.isEmpty(json))
			{
				String updatedJSON = HikeConversationsDatabase.getInstance().updateJSONMetadata((int) (message.getMsgID()), json);
				if (!TextUtils.isEmpty(updatedJSON))
				{
					message.webMetadata = new WebMetadata(updatedJSON);
				}
			}

			startComPoseChatActivity(message);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * call this function to get the data from the native memory
	 * @param id: the id of the function that native will call to call the js .
	 * @param key: key of the data to be saved. Microapp needs to make sure about the uniqueness of the key.
	 */
	@JavascriptInterface
	public void getFromCache(String id, String key)
	{
		String value = HikeContentDatabase.getInstance().getFromContentCache(key, message.getNameSpace());
		callbackToJS(id, value);
	}
	
	/**
	 * Call this function to get the bulk large data from the native memory
	 * @param id : the id of the function that native will call to call the js .
	 */
	@JavascriptInterface
	public void getLargeDataFromCache(String id)
	{
		String value = HikeContentDatabase.getInstance().getFromContentCache(message.getNameSpace(), message.getNameSpace());
		callbackToJS(id, value);
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
		HikeContentDatabase.getInstance().putInContentCache(key, message.getNameSpace(), value);
	}
	
	/**
	 * Call this method to put bulk large data in cache. Earlier large data will be replaced by this new data and there will
	 * be only one entry per microapp.
	 * @param value: the data that the app need to cache.
	 */
	@JavascriptInterface
	public void putLargeDataInCache(String value)
	{
		HikeContentDatabase.getInstance().putInContentCache(message.getNameSpace(), message.getNameSpace(), value);
	}
	
	/**
	 * This function is called whenever the onLoadFinished of the html is called. This function calling is MUST.
	 * This function is also used for analytics purpose.
	 *
	 * @param height : The height of the loaded content
	 */
	@JavascriptInterface
	public void onLoadFinished(String height)
	{
		super.onLoadFinished(height);
		if(message.webMetadata.getPlatformJSCompatibleVersion() >= HikePlatformConstants.VERSION_2)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Logger.i(tag, "inside run onloadfinished "+listener);
					init();
				}
			});
		}
		try
		{
			int requiredHeightinDP = Integer.parseInt(height);
			int requiredHeightInPX = (int) (requiredHeightinDP * Utils.densityMultiplier);
			if (requiredHeightInPX != mWebView.getHeight())
			{
				Logger.i(tag, "onloadfinished called with height=" + requiredHeightInPX + " current height is " + mWebView.getHeight() + " : updated in DB as well");
				// lets save in DB, so that from next time onwards we will have less flickering
				message.webMetadata.setCardHeight(requiredHeightinDP);
				HikeConversationsDatabase.getInstance().updateMetadataOfMessage(message.getMsgID(), message.webMetadata.JSONtoString());
				resizeWebview(height);
			}
			else
			{
				Logger.i(tag, "onloadfinished called with height=" + requiredHeightInPX + " current height is " + mWebView.getHeight());
			}

		}
		catch (NumberFormatException ne)
		{
			ne.printStackTrace();
		}

	}
}
