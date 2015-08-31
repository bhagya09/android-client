package com.bsb.hike.platform;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.HikeAnalyticsEvent;

import android.app.Activity;

public class PlatformHelper {
	public BotInfo mBotInfo;
	public static final String tag = "GameUtils";
	Activity activity;
	public PlatformHelper(BotInfo mBotInfo,Activity activty)
	{
		this.mBotInfo=mBotInfo;
		this.activity=activity;
	}
	public void callbackToGame(final String id, final String value)
	{
		//Call call-back function of Games Activity.
	}
	public void putInCache(String key, String value)
	{
		HikeContentDatabase.getInstance().putInContentCache(key, mBotInfo.getNamespace(), value);
	}
	public void getFromCache(String id, String key)
	{
		String value = HikeContentDatabase.getInstance().getFromContentCache(key, mBotInfo.getNamespace());
		callbackToGame(id, value);
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
	


}
