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
	 * call this function to delete the message. The message will get deleted instantaneously
	 */
	@JavascriptInterface
	public void deleteMessage()
	{
		MessagingBotBridgeHelper.deleteMessage(message.getMsgID(), message.getMsisdn(), adapter);
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

	/**
	 * Call this function to set the alarm at certain time that is defined by the second parameter.
	 * The first param is a json that contains
	 * 1.alarm_data: the data that the javascript receives when the alarm is played.
	 * 2.delete_card: if present and true, used to delete the message on alarm getting played
	 * 3.conv_msisdn: this field is must Send the msisdn.
	 * 4.inc_unread: if inc_unread is present and true, we will increase red unread counter in Conversation screen.
	 * 5.notification: contains message  if you want to show notification at some particular time
	 * 6.notification_sound: true if we you want to play sound
	 * sample json  :  {alarm_data:{}, conv_msisdn:'', ;delete_card' : 'true' , 'inc_unread' :'true ' , 'notification': 'message', 'notification_sound':'true'}
	 *
	 * @param json
	 * @param timeInMills
	 */
	@JavascriptInterface
	public void setAlarm(String json, String timeInMills)
	{
		Logger.i(tag, "set alarm called " + json + " , mId " + message.getMsgID() + " , time " + timeInMills);
			if(weakActivity.get()!=null){
				MessagingBotBridgeHelper.setAlarm(json, timeInMills, weakActivity.get(), (int)message.getMsgID());
			}
	}

	/**
	 * this function will update the helper data. It will replace the key if it is present in the helper data and will add it if it is
	 * not present in the helper data.
	 *
	 * @param json
	 */
	@JavascriptInterface
	public void updateHelperData(String json)
	{
		Logger.i(tag, "update metadata called " + json + " , message id=" + message.getMsgID());
		WebMetadata metadata = MessagingBotBridgeHelper.updateHelperData(message.getMsgID(), json);
		if(metadata!=null)
		{
			message.webMetadata = metadata;
		}
	}


	/**
	 * calling this function will delete the alarm associated with this javascript.
	 */
	@JavascriptInterface
	public void deleteAlarm()
	{
		MessagingBotBridgeHelper.deleteAlarm((int)message.getMsgID());
	}

	/**
	 * Calling this function will update the metadata. If the key is already present, it will be replaced else it will be added to the existent metadata.
	 * If the json has JSONObject as key, there would be another round of iteration, and will replace the key-value pair if the key is already present
	 * and will add the key-value pair if the key is not present in the existent metadata.
	 *
	 * @param json
	 * @param notifyScreen : if true, the adapter will be notified of the change, else there will be only db update.
	 */
	@JavascriptInterface
	public void updateMetadata(String json, String notifyScreen)
	{
		Logger.i(tag, "update metadata called " + json + " , message id=" + message.getMsgID() + " notifyScren is " + notifyScreen);
		updateMetadata(MessagingBotBridgeHelper.updateMetadata((int)message.getMsgID(), json), notifyScreen);
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
					Object obj = mWebView.getTag();
					if (obj instanceof WebViewHolder)
					{
						Logger.i(tag, "updated metadata and calling notifydataset of " + adapter.getClass().getName() + " and thread= " + Thread.currentThread().getName());
						WebViewHolder holder = (WebViewHolder) obj;
						holder.id = -1; // will make sure new metadata is inflated in webview
						adapter.notifyDataSetChanged();
					}
					else
					{
						Logger.e(tag, "Expected Tag of Webview was WebViewHolder and received " + obj.getClass().getCanonicalName());
					}

				}
			});
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

	/**
	 * This function is called whenever the onLoadFinished of the html is called. This function calling is MUST.
	 * This function is also used for analytics purpose.
	 *
	 * @param height : The height of the loaded content
	 */
	@JavascriptInterface
	public void onLoadFinished(String height)
	{
		if(message.webMetadata.getPlatformJSCompatibleVersion() >= HikePlatformConstants.VERSION_2)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					init();
					setData();
					if (listener != null)
					{
						listener.loadFinished(message);
					}
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
		if(javaBridgeHandler!=null)
		{
			javaBridgeHandler.removeMessages(UPDATE_MESSAGE);
			Message msg = Message.obtain();
			msg.what = UPDATE_MESSAGE;
			msg.obj = message;
			javaBridgeHandler.sendMessage(msg);
		}else{
			this.message = message;
			Logger.e(tag, "javabridge handler is null while updating conv message");
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
		HikeContentDatabase.getInstance().putInContentCache(message.getNameSpace(), message.getNameSpace(), value);
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

	@Override
	protected void handleJavaBridgeMessage(Message msg)
	{
		switch(msg.what)
		{
		case UPDATE_MESSAGE:
			message = (ConvMessage) msg.obj;
			break;
		}
	}
}
