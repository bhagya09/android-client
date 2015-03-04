package com.bsb.hike.utils;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.service.HikeMqttManagerNew;

public class HikeAnalyticsEvent
{
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
	        HikeMqttManagerNew.getInstance().sendMessage(object, HikeMqttManagerNew.MQTT_QOS_ONE);
		}
		catch (JSONException e)
		{
			Logger.e("HikeAnalyticsEvent", "Exception in sending analytics event", e);
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
	        HikeMqttManagerNew.getInstance().sendMessage(object, HikeMqttManagerNew.MQTT_QOS_ONE);
		}
		catch (JSONException e)
		{
			Logger.e("HikeAnalyticsEvent", "Exception in sending analytics event", e);
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
			HikeMqttManagerNew.getInstance().sendMessage(object, HikeMqttManagerNew.MQTT_QOS_ZERO);
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

    public static void analyticsForCards(String type, String subType, JSONObject json)
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

}
