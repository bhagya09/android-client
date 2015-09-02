package com.bsb.hike.platform;

import java.lang.ref.WeakReference;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.bridge.JavascriptBridge;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

public class PlatformHelper
{

	protected static Handler mHandler;

	public static final String tag = "PlatformHelper";
	public PlatformHelper()
	{
		this.mHandler = new Handler(HikeMessengerApp.getInstance().getMainLooper())
		{
			public void handleMessage(Message msg)
			{
				handleUiMessage(msg);
			};
		};
	}
	protected void handleUiMessage(Message msg)
	{

	}

	public static void putInCache(String key, String value, BotInfo mBotInfo)
	{
		HikeContentDatabase.getInstance().putInContentCache(key, mBotInfo.getNamespace(), value);
	}

	public static String getFromCache(String key, BotInfo mBotInfo)
	{
		return HikeContentDatabase.getInstance().getFromContentCache(key, mBotInfo.getNamespace());

	}

	public static void logAnalytics(String isUI, String subType, String json, BotInfo mBotInfo)
	{

		try
		{
			JSONObject jsonObject = new JSONObject(json);
			jsonObject.put(AnalyticsConstants.BOT_MSISDN, mBotInfo.getMsisdn());
			jsonObject.put(AnalyticsConstants.BOT_NAME, mBotInfo.getConversationName());
			if (Boolean.valueOf(isUI))
			{
				HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.MICROAPP_UI_EVENT, subType, jsonObject);
			}
			else
			{
				HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.MICROAPP_NON_UI_EVENT, subType, jsonObject);
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

	public static void forwardToChat(String json, String hikeMessage, BotInfo mBotInfo, final WeakReference<Activity> weakActivity)
	{
		Logger.i(tag, "Received this json in forward to chat : " + json + "\n Received this hm : " + hikeMessage);

		if (TextUtils.isEmpty(json) || TextUtils.isEmpty(hikeMessage))
		{
			Logger.e(tag, "Received a null or empty json/hikeMessage in forward to chat");
			return;
		}

		try
		{
			NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
			JSONObject cardObj = new JSONObject(json);

			/**
			 * Blindly inserting the appName in the cardObj JSON.
			 */
			cardObj.put(HikePlatformConstants.APP_NAME, metadata.getAppName());
			cardObj.put(HikePlatformConstants.APP_PACKAGE, metadata.getAppPackage());

			JSONObject webMetadata = new JSONObject();
			webMetadata.put(HikePlatformConstants.TARGET_PLATFORM, metadata.getTargetPlatform());
			webMetadata.put(HikePlatformConstants.CARD_OBJECT, cardObj);
			ConvMessage message = PlatformUtils.getConvMessageFromJSON(webMetadata, hikeMessage, mBotInfo.getMsisdn());
			message.setNameSpace(mBotInfo.getNamespace());
			if (message != null)
			{
				startComPoseChatActivity(message, weakActivity);
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void sendNormalEvent(String messageHash, String eventData, BotInfo mBotInfo)
	{
		PlatformUtils.sendPlatformMessageEvent(eventData, messageHash, mBotInfo.getNamespace());
	}

	public static void sendSharedMessage(String cardObject, String hikeMessage, String sharedData, BotInfo mBotInfo, final WeakReference<Activity> weakActivity)
	{
		if (TextUtils.isEmpty(cardObject) || TextUtils.isEmpty(hikeMessage))
		{
			Logger.e(tag, "Received a null or empty json/hikeMessage in forward to chat");
			return;
		}

		try
		{
			NonMessagingBotMetadata metadata = new NonMessagingBotMetadata(mBotInfo.getMetadata());
			JSONObject cardObj = new JSONObject(cardObject);

			/**
			 * Blindly inserting the appName in the cardObject JSON.
			 */
			cardObj.put(HikePlatformConstants.APP_NAME, metadata.getAppName());
			cardObj.put(HikePlatformConstants.APP_PACKAGE, metadata.getAppPackage());

			JSONObject webMetadata = new JSONObject();
			webMetadata.put(HikePlatformConstants.TARGET_PLATFORM, metadata.getTargetPlatform());
			webMetadata.put(HikePlatformConstants.CARD_OBJECT, cardObj);
			ConvMessage message = PlatformUtils.getConvMessageFromJSON(webMetadata, hikeMessage, mBotInfo.getMsisdn());

			message.setParticipantInfoState(ConvMessage.ParticipantInfoState.NO_INFO);
			JSONObject sharedDataJson = new JSONObject(sharedData);
			sharedDataJson.put(HikePlatformConstants.EVENT_TYPE, HikePlatformConstants.SHARED_EVENT);
			message.setPlatformData(sharedDataJson);
			message.setNameSpace(mBotInfo.getNamespace());
			pickContactAndSend(message, weakActivity);

		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String getAllEventsForMessageHash(String messageHash, BotInfo mBotInfo)
	{
		if (TextUtils.isEmpty(messageHash))
		{
			Logger.e(tag, "can't return all events as the message hash is " + messageHash);
			return null;
		}
		String eventData = HikeConversationsDatabase.getInstance().getEventsForMessageHash(messageHash, mBotInfo.getNamespace());
		return eventData;
	}

	public static String getAllEventsData(BotInfo mBotInfo)
	{
		String messageData = HikeConversationsDatabase.getInstance().getMessageEventsForMicroapps(mBotInfo.getNamespace(), true);
		return messageData;
	}

	public static String getSharedEventsData(BotInfo mBotInfo)
	{
		String messageData = HikeConversationsDatabase.getInstance().getMessageEventsForMicroapps(mBotInfo.getNamespace(), false);
		return messageData;
	}

	protected static void pickContactAndSend(ConvMessage message, final WeakReference<Activity> weakActivity)
	{
		final String REQUEST_CODE = "request_code";

		final int PICK_CONTACT_REQUEST = 1;

		final int PICK_CONTACT_AND_SEND_REQUEST = 2;
		Activity activity = weakActivity.get();
		if (activity != null)
		{
			final Intent intent = IntentFactory.getForwardIntentForConvMessage(activity, message, PlatformContent.getForwardCardData(message.webMetadata.JSONtoString()));
			intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, ComposeChatActivity.PICK_CONTACT_AND_SEND_MODE);
			intent.putExtra(tag, activity.hashCode());
			intent.putExtra(REQUEST_CODE, PICK_CONTACT_AND_SEND_REQUEST);
			activity.startActivityForResult(intent, HikeConstants.PLATFORM_REQUEST);
		}
	}

	protected static void startComPoseChatActivity(final ConvMessage message, final WeakReference<Activity> weakActivity)
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
				Activity mContext = weakActivity.get();
				if (mContext != null)
				{
					final Intent intent = IntentFactory.getForwardIntentForConvMessage(mContext, message, PlatformContent.getForwardCardData(message.webMetadata.JSONtoString()));
					mContext.startActivity(intent);
				}
			}
		});
	}

}
