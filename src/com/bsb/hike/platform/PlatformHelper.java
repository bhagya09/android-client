package com.bsb.hike.platform;

import java.lang.ref.WeakReference;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;

public class PlatformHelper {
	public BotInfo mBotInfo;
	protected Handler mHandler;
	protected WeakReference<Activity> weakActivity;
	public static final String tag = "PlatformHelper";
	Activity activity;
	public PlatformHelper(BotInfo mBotInfo,Activity activty)
	{
		this.mBotInfo=mBotInfo;
		this.activity=activity;
		weakActivity = new WeakReference<Activity>(activity);
	}
	
	public void putInCache(String key, String value)
	{
		HikeContentDatabase.getInstance().putInContentCache(key, mBotInfo.getNamespace(), value);
	}
	public String getFromCache(String id, String key)
	{
		return HikeContentDatabase.getInstance().getFromContentCache(key, mBotInfo.getNamespace());

	}
	public void logAnalytics(String isUI, String subType, String json)
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
	
	public void forwardToChat(String json, String hikeMessage)
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
				startComPoseChatActivity(message);
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void startComPoseChatActivity(final ConvMessage message)
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
