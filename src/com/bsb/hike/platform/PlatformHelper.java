package com.bsb.hike.platform;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.bridge.JavascriptBridge;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

public class PlatformHelper
{

	public static Handler mHandler;

	public static final String TAG = "PlatformHelper";

	// Call this method to put data in cache. This will be a key-value pair.
	public static void putInCache(String key, String value, String namespace)
	{
		HikeContentDatabase.getInstance().putInContentCache(key, namespace, value);
	}

	// Call this function to get data from cache corresponding to a key
	public static String getFromCache(String key, String namespace)
	{
		return HikeContentDatabase.getInstance().getFromContentCache(key, namespace);

	}

	// Function to log Analytics
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

	// Function to forward to chat
	public static void forwardToChat(String json, String hikeMessage, BotInfo mBotInfo, Activity activity)
	{
		Logger.i(TAG, "Received this json in forward to chat : " + json + "\n Received this hm : " + hikeMessage);

		if (TextUtils.isEmpty(json) || TextUtils.isEmpty(hikeMessage))
		{
			Logger.e(TAG, "Received a null or empty json/hikeMessage in forward to chat");
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
			webMetadata.put(HikePlatformConstants.FORWARD_CARD_OBJECT, metadata.getFwdCardObj());
			ConvMessage message = PlatformUtils.getConvMessageFromJSON(webMetadata, hikeMessage, mBotInfo.getMsisdn());
			message.setNameSpace(mBotInfo.getNamespace());
			if (message != null)
			{
				startComPoseChatActivity(message, activity);
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void sendNormalEvent(String messageHash, String eventData, String namespace)
	{
		PlatformUtils.sendPlatformMessageEvent(eventData, messageHash, namespace);
	}

	public static void sendSharedMessage(String cardObject, String hikeMessage, String sharedData, BotInfo mBotInfo, final Activity activity, int hashcode)
	{
		if (TextUtils.isEmpty(cardObject) || TextUtils.isEmpty(hikeMessage))
		{
			Logger.e(TAG, "Received a null or empty json/hikeMessage in forward to chat");
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
			webMetadata.put(HikePlatformConstants.FORWARD_CARD_OBJECT, metadata.getFwdCardObj());
			ConvMessage message = PlatformUtils.getConvMessageFromJSON(webMetadata, hikeMessage, mBotInfo.getMsisdn());

			message.setParticipantInfoState(ConvMessage.ParticipantInfoState.NO_INFO);
			JSONObject sharedDataJson = new JSONObject(sharedData);
			sharedDataJson.put(HikePlatformConstants.EVENT_TYPE, HikePlatformConstants.SHARED_EVENT);
			message.setPlatformData(sharedDataJson);
			message.setNameSpace(mBotInfo.getNamespace());
			pickContactAndSend(message, activity, hashcode);

		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void sendSharedMessage(String cardObject, String hikeMessage, String sharedData, BotInfo mBotInfo, final Activity activity)
	{
		sendSharedMessage(cardObject, hikeMessage, sharedData, mBotInfo, activity, -1);
	}

	public static String getAllEventsForMessageHash(String messageHash, String namespace)
	{
		if (TextUtils.isEmpty(messageHash))
		{
			Logger.e(TAG, "can't return all events as the message hash is " + messageHash);
			return null;
		}
		String eventData = HikeConversationsDatabase.getInstance().getEventsForMessageHash(messageHash, namespace);
		return eventData;
	}

	public static String getAllEventsData(String namespace)
	{
		String messageData = HikeConversationsDatabase.getInstance().getMessageEventsForMicroapps(namespace, true);
		return messageData;
	}

	public static String getSharedEventsData(String namespace)
	{
		String messageData = HikeConversationsDatabase.getInstance().getMessageEventsForMicroapps(namespace, false);
		return messageData;
	}

	public static void pickContactAndSend(ConvMessage message, final Activity activity, int hashcode)
	{
		if (activity != null)
		{
			final Intent intent = IntentFactory.getForwardIntentForConvMessage(activity, message, PlatformContent.getForwardCardData(message.webMetadata.JSONtoString()), false);
			intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, ComposeChatActivity.PICK_CONTACT_AND_SEND_MODE);
			if (hashcode < 0)
				intent.putExtra(JavascriptBridge.tag, hashcode);
			intent.putExtra(HikePlatformConstants.REQUEST_CODE, JavascriptBridge.PICK_CONTACT_AND_SEND_REQUEST);
			intent.putExtra(HikeConstants.Extras.THUMBNAILS_REQUIRED, true);
			activity.startActivityForResult(intent, HikeConstants.PLATFORM_REQUEST);

		}
	}

	public static void startComPoseChatActivity(final ConvMessage message, final Activity mContext)
	{
		PlatformHelper.mHandler = new Handler(HikeMessengerApp.getInstance().getMainLooper());
		if (null == mHandler)
		{
			Log.e(TAG, "handler is null");
			return;
		}

		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (mContext != null)
				{
					final Intent intent = IntentFactory.getForwardIntentForConvMessage(mContext, message, PlatformContent.getForwardCardData(message.webMetadata.JSONtoString()),
							true);
					mContext.startActivity(intent);
				}
				else
				{
					Log.d(TAG, "context is null");
				}
			}
		});
	}

	public static void deleteEvent(String eventId)
	{
		if (TextUtils.isEmpty(eventId))
		{
			Logger.e(TAG, "event can't be deleted as the event id is " + eventId);
			return;
		}
		HikeConversationsDatabase.getInstance().deleteEvent(eventId);
	}

	public static void deleteAllEventsForMessage(String messageHash)
	{
		if (TextUtils.isEmpty(messageHash))
		{
			Logger.e(TAG, "the events corresponding to the message hash can't be deleted as the message hash is " + messageHash);
			return;
		}
		HikeConversationsDatabase.getInstance().deleteAllEventsForMessage(messageHash);
	}

	public static void postStatusUpdate(String status, String moodId, String imageFilePath)
	{
		int mood;

		try
		{
			mood = Integer.parseInt(moodId);
		}
		catch (NumberFormatException e)
		{
			Logger.e(TAG, "moodId to postStatusUpdate should be a number.");
			mood = -1;
		}

		Utils.postStatusUpdate(status, mood, imageFilePath);

	}

	public static void showPopup(String contentData, Activity activity)
	{
		final HikeAppStateBaseFragmentActivity hikeBaseActivity;
		if (TextUtils.isEmpty(contentData) || activity == null)
		{
			Logger.e(TAG, "Either activity or contentData to showPopup is null. Returning.");
			return;
		}
		if (activity instanceof HikeAppStateBaseFragmentActivity)
		{
			hikeBaseActivity = (HikeAppStateBaseFragmentActivity) activity;
		}
		else
		{
			Logger.e(TAG, "Activity passed to showPopup is not subclass of HikeAppStateBaseFragmentActivity. Returning.");
			return;
		}
		try
		{
			JSONObject data = new JSONObject(contentData);
			if (!checkContentData(data))
			{
				return;
			}
			final ProductContentModel mmModel = ProductContentModel.makeProductContentModel(data);
			ProductInfoManager.getInstance().parseAndShowPopup(mmModel, new IActivityPopup()
			{
				@Override
				public void onSuccess(ProductContentModel productContentModel)
				{
					hikeBaseActivity.showPopupDialog(mmModel);
				}

				@Override
				public void onFailure()
				{
					Logger.e(TAG, "Failure occured when opening popup.");
				}
			});

		} catch (JSONException e)
		{
			Logger.e(TAG, "JSONException in showPopup : " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static boolean checkContentData(JSONObject data) throws JSONException
	{
		if (data.has(HikePlatformConstants.CARD_OBJECT))
		{
			JSONObject cardObj = data.optJSONObject(HikePlatformConstants.CARD_OBJECT);
			if (cardObj == null)
			{
				Logger.e(TAG, "cardObj is null in contentData. Returning.");
				return false;
			}
			if (!cardObj.has(HikePlatformConstants.LAYOUT_DATA))
			{
				cardObj.put(HikePlatformConstants.LAYOUT_DATA, new JSONObject());
			}
			else
			{
				if (!(cardObj.get(HikePlatformConstants.LAYOUT_DATA) instanceof JSONObject))
				{
					cardObj.put(HikePlatformConstants.LAYOUT_DATA, new JSONObject());
				}
			}
		}
		else
		{
			Logger.e(TAG, "cardObj not present in contentData. Returning.");
			return false;
		}
		return true;
	}
}