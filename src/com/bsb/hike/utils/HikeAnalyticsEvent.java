package com.bsb.hike.utils;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.ChatAnalyticConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HomeAnalyticsConstants;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.service.HikeMqttManagerNew;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.List;

public class HikeAnalyticsEvent
{
	
	private static String TAG = "HikeAnalyticsEvent";
	/*
	 * We send this event every time user mark some chats as stealth
	 */
	public static void sendStealthMsisdns(List<String> enabledMsisdn, List<String> disabledMsisdn)
	{
		// TODO use array instead of sets here.
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.STEALTH);

			JSONObject dataJson = new JSONObject();
			if (enabledMsisdn != null)
			{
				dataJson.put(HikeConstants.ENABLED_STEALTH, new JSONArray(enabledMsisdn));
			}
			if (disabledMsisdn != null)
			{
				dataJson.put(HikeConstants.DISABLED_STEALTH, new JSONArray(disabledMsisdn));
			}
			object.put(HikeConstants.DATA, dataJson);
	        HikeMqttManagerNew.getInstance().sendMessage(object, MqttConstants.MQTT_QOS_ONE);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "JSONException in stealth msisdn", e);
		}

	}

	/*
	 * We send this event every time when user resets stealth mode
	 */
	public static void sendStealthReset()
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.STEALTH);

			JSONObject dataJson = new JSONObject();
			dataJson.put(HikeConstants.RESET, true);
			object.put(HikeConstants.DATA, dataJson);
	        HikeMqttManagerNew.getInstance().sendMessage(object, MqttConstants.MQTT_QOS_ONE);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "JSONException in stealth reset event", e);
		}
	}

	/*
	 * We send this event every time when user enter stealth mode
	 */
	public static void sendStealthEnabled(boolean enabled)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.TOGGLE_STEALTH);

			JSONObject dataJson = new JSONObject();
			dataJson.put(HikeConstants.ENABLED, enabled);
			object.put(HikeConstants.DATA, dataJson);
			HikeMqttManagerNew.getInstance().sendMessage(object, MqttConstants.MQTT_QOS_ZERO);
		}
		catch (JSONException e)
		{
			Logger.e("HikeAnalyticsEvent", "Exception in sending analytics event", e);
		}
	}
	

	/*
	 * We send an event every time user exists the gallery selection activity
	 */
	public static void sendGallerySelectionEvent(int total, int successful, Context context)
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.TOTAL_SELECTIONS, total);
			metadata.put(HikeConstants.SUCCESSFUL_SELECTIONS, successful);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.EXIT_FROM_GALLERY, metadata, HikeConstants.LogEvent.GALLERY_SELECTION);			
		}
		catch (JSONException e)
		{
			Logger.e("HikeAnalyticsEvent", "Exception is sending analytics event for gallery selections", e);
		}
	}

	public static void cardErrorAnalytics(PlatformContent.EventCode reason, ConvMessage convMessage)
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(HikePlatformConstants.ERROR_CODE, reason.toString());
			json.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.BOT_ERROR);
			json.put(AnalyticsConstants.CONTENT_ID, convMessage.getContentId());
			HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.ERROR_EVENT, json);
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

	public static void analyticsForBots(String type, String subType, JSONObject json)
	{
		try
		{
			Logger.d("HikeAnalyticsEvent", json.toString());
			HAManager.getInstance().record(type, subType, HAManager.EventPriority.NORMAL, json, AnalyticsConstants.EVENT_TAG_BOTS);
		}
		catch (NullPointerException npe)
		{
			npe.printStackTrace();
		}
	}

    public static void analyticsForPlatform(String type, String subType, JSONObject json)
    {
        try
        {
            Logger.d("HikeAnalyticsEvent", json.toString());
            HAManager.getInstance().record(type, subType, HAManager.EventPriority.HIGH, json, AnalyticsConstants.EVENT_TAG_PLATFORM);
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
        }
    }
    
    public static void analyticsForPhotos(String type, String subType, JSONObject json)
    {
        try
        {
            Logger.d("HikeAnalyticsEvent", json.toString());
            HAManager.getInstance().record(type, subType, HAManager.EventPriority.HIGH, json, AnalyticsConstants.EVENT_TAG_PHOTOS);
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
        }
    }

	public static void analyticsForNonMessagingBots(String type, String subType, JSONObject json)
	{
		try
		{
			Logger.d("HikeAnalyticsEvent", json.toString());
			json.put(AnalyticsConstants.NETWORK_TYPE, Integer.toString(Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext())));
			json.put(AnalyticsConstants.APP_VERSION, AccountUtils.getAppVersion());
			HAManager.getInstance().record(type, subType, HAManager.EventPriority.HIGH, json, AnalyticsConstants.EVENT_TAG_PLATFORM);
		}
		catch (NullPointerException npe)
		{
			npe.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static void analyticsForUserProfileOpen(String userMsisdn, String source)
	{
		try
		{
			JSONObject analyticsObj = new JSONObject();
			analyticsObj.put(AnalyticsConstants.V2.UNIQUE_KEY, AnalyticsConstants.USER_TL_OPEN);
			analyticsObj.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_LOG_2);
			analyticsObj.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			analyticsObj.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			analyticsObj.put(AnalyticsConstants.V2.ORDER, AnalyticsConstants.USER_TL_OPEN);
			analyticsObj.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());
			analyticsObj.put(AnalyticsConstants.V2.GENUS, source);
			analyticsObj.put(AnalyticsConstants.V2.TO_USER, userMsisdn);

			HAManager.getInstance().recordV2(analyticsObj);
		}

		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static void recordAnalyticsForAddFriend(String userMsisdn, String source, boolean requestSent)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, AnalyticsConstants.ADD_FRIEND);
			json.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.ACT_LOG_2);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, AnalyticsConstants.ADD_FRIEND);
			json.put(AnalyticsConstants.V2.FAMILY, System.currentTimeMillis());
			json.put(AnalyticsConstants.V2.GENUS, requestSent ? "req_sent" : "req_acc");
			json.put(AnalyticsConstants.V2.SPECIES, source);
			json.put(AnalyticsConstants.V2.TO_USER, userMsisdn);

			HAManager.getInstance().recordV2(json);
		}

		catch (JSONException e)
		{
			e.toString();
		}
	}

	public static void platformAnalytics(String json,String uniqueKey,String kingdom) {
		if (TextUtils.isEmpty(uniqueKey) || TextUtils.isEmpty(kingdom)) {
			Logger.e(TAG, "Either unique key or kingdom is null");
		}
		try {
			JSONObject jsonObject = new JSONObject(json);

			jsonObject.put(AnalyticsConstants.V2.NETWORK, (Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext())));
			jsonObject.put(AnalyticsConstants.V2.KINGDOM, kingdom);
			jsonObject.put(AnalyticsConstants.V2.UNIQUE_KEY, uniqueKey);
			HAManager.getInstance().recordV2(jsonObject);
		} catch (JSONException e) {
			Logger.e(TAG, e.toString());
		}

	}
	public static void recordAnalyticsForGCPins(String uniqueKey_order, String genus, String source, String species)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, uniqueKey_order);
			json.put(AnalyticsConstants.V2.KINGDOM, ChatAnalyticConstants.ACT_CORE_LOGS);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, uniqueKey_order);

			if(!TextUtils.isEmpty(genus))
				json.put(AnalyticsConstants.V2.GENUS, genus);

			if(!TextUtils.isEmpty(source))
				json.put(AnalyticsConstants.V2.SOURCE, source);

			if(!TextUtils.isEmpty(species))
				json.put(AnalyticsConstants.V2.SPECIES, species);

			HAManager.getInstance().recordV2(json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static void recordAnalyticsForGCFlow(String uniqueKey_order, String form, int race, int breed, int val_int, ArrayList<String> toUser_msisdn)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, uniqueKey_order);
			json.put(AnalyticsConstants.V2.KINGDOM, ChatAnalyticConstants.ACT_CORE_LOGS);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, uniqueKey_order);
			if(!TextUtils.isEmpty(form))
				json.put(AnalyticsConstants.V2.FORM, form);
			if(race != -1)
				json.put(AnalyticsConstants.V2.RACE, race);
			if(breed != -1)
				json.put(AnalyticsConstants.V2.BREED, breed);
			if(val_int > 0)
				json.put(AnalyticsConstants.V2.VAL_INT, val_int);
			if(toUser_msisdn != null)
			{
				json.put(AnalyticsConstants.V2.TO_USER, toUser_msisdn.toString());
			}
			json.put(AnalyticsConstants.V2.NETWORK, Utils.getNetworkTypeAsString(HikeMessengerApp.getInstance().getApplicationContext()));

			HAManager.getInstance().recordV2(json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public static JSONObject getSettingsAnalyticsJSON()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, HomeAnalyticsConstants.SETTINGS_UK);
			json.put(AnalyticsConstants.V2.KINGDOM, HomeAnalyticsConstants.HOMESCREEN_KINGDOM);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, HomeAnalyticsConstants.SETTINGS_ORDER);
			return json;
		}
		catch (JSONException e)
		{
			e.toString();
			return null;
		}
	}
}
