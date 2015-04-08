package com.bsb.hike.platform.bridge;

import java.util.ArrayList;

import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformAlarmManager;
import com.bsb.hike.platform.WebMetadata;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.BaseAdapter;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.WebViewCardRenderer.WebViewHolder;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * API bridge that connects the javascript to the Native environment. Make the instance of this class and add it as the JavaScript interface of the Card WebView.
 */

public class PlatformJavaScriptBridge extends JavascriptBridge
{

	private static final String tag = "platformbridge";

	ConvMessage message;

	BaseAdapter adapter;

	public PlatformJavaScriptBridge(Activity activity,WebView mWebView)
	{
		super(activity,mWebView);
	}

	public PlatformJavaScriptBridge(Activity activity,WebView webView, ConvMessage convMessage, BaseAdapter adapter)
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
		ArrayList<Long> msgIds = new ArrayList<Long>(1);
		msgIds.add(message.getMsgID());
		Bundle bundle = new Bundle();
		if (adapter.getCount() <= 1)
		{
			bundle.putBoolean(HikeConstants.Extras.IS_LAST_MESSAGE, true);
		}
		else
		{
			bundle.putBoolean(HikeConstants.Extras.IS_LAST_MESSAGE, false);
		}

		bundle.putString(HikeConstants.Extras.MSISDN, message.getMsisdn());
		bundle.putBoolean(HikeConstants.Extras.DELETE_MEDIA_FROM_PHONE, false);
		HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_MESSAGE, new Pair<ArrayList<Long>, Bundle>(msgIds, bundle));
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
		try
		{
			Logger.i(tag, "set alarm called " + json + " , mId " + message.getMsgID() + " , time " + timeInMills);
			if(weakActivity.get()!=null)
			{
				PlatformAlarmManager.setAlarm(weakActivity.get().getApplicationContext(), new JSONObject(json), (int) message.getMsgID(), Long.valueOf(timeInMills));
			}
		}
		catch(NumberFormatException ne)
		{
			ne.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
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
		try
		{
			Logger.i(tag, "update metadata called " + json + " , message id=" + message.getMsgID());
			String updatedJSON = HikeConversationsDatabase.getInstance().updateHelperData((message.getMsgID()), json);
			if (updatedJSON != null)
			{
				message.webMetadata = new WebMetadata(updatedJSON);
			}

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * This function will replace the entire metadata of the message.
	 *
	 * @param metadata
	 */
	@JavascriptInterface
	public void replaceMetadata(String metadata)
	{
		try
		{
			message.webMetadata = new WebMetadata(new JSONObject(metadata));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * calling this function will delete the alarm associated with this javascript.
	 */
	@JavascriptInterface
	public void deleteAlarm()
	{
		HikeConversationsDatabase.getInstance().deleteAppAlarm((int) (message.getMsgID()));
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
		try
		{
			Logger.i(tag, "update metadata called " + json + " , message id=" + message.getMsgID() + " notifyScren is " + notifyScreen);
			String updatedJSON = HikeConversationsDatabase.getInstance().updateJSONMetadata((int) (message.getMsgID()), json);

			if (updatedJSON != null)
			{
				message.webMetadata = new WebMetadata(updatedJSON); // the new metadata to inflate in webview
				if (notifyScreen != null && Boolean.valueOf(notifyScreen))
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
		}
		catch (JSONException e)
		{
			e.printStackTrace();
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
			

			if (null == mHandler)
			{
				return;
			}

			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Activity mContext = weakActivity.get();
					if(mContext!=null)
					{
						final Intent intent = IntentManager.getForwardIntentForConvMessage(mContext, message,
							PlatformContent.getForwardCardData(message.webMetadata.JSONtoString()));
						mContext.startActivity(intent);
					}
				}
			});

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

		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_BOT,
				null);

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

	public void setData()
	{
		mWebView.loadUrl("javascript:setData('" + message.getMsisdn() + "','" + message.webMetadata.getHelperData().toString() + "','" + message.isSent() + "')");
	}

	public void alarmPlayed(String alarmData)
	{
		mWebView.loadUrl("javascript:alarmPlayed(" + "'" + alarmData + "')");
	}

	public void updateConvMessage(ConvMessage message)
	{
		this.message = message;
	}

}
