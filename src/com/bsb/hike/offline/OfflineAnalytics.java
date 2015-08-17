package com.bsb.hike.offline;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.offline.OfflineConstants.AnalyticsConstants;
//import com.bsb.hike.offline.OfflineConstants.AnalyticsEvents;

/**
 * 
 * @author himanshu This class is used to record Analytics related to Offline Messaging
 */
public class OfflineAnalytics
{

	public static void pushNotificationClicked(int argument)
	{
		JSONObject object = new JSONObject();

		try
		{
			object.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.EVENT_TYPE_OFFLINE);
			object.put(HikeConstants.EVENT_KEY, AnalyticsConstants.EVENT_KEY_PUSH);
			object.put(HikeConstants.TAG, argument);

			recordAnalytics(object);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

	}
	
	public static void noInternetTipClicked()
	{
		JSONObject object = new JSONObject();

		try
		{
			object.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.EVENT_TYPE_OFFLINE);
			object.put(HikeConstants.EVENT_KEY, AnalyticsConstants.EVENT_KEY_PUSH);
			object.put(HikeConstants.TAG, AnalyticsConstants.TAG_NO_INTERNET_CLICKED);
			recordAnalytics(object);
			
		}
		catch (JSONException e)
		{

		}
	}

	public static void recordTimeForConnection(long time)
	{
		JSONObject object = new JSONObject();

		try
		{
			object.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.EVENT_TYPE_OFFLINE);
			object.put(HikeConstants.EVENT_KEY, AnalyticsConstants.EVENT_KEY_PUSH);
			object.put(AnalyticsConstants.EVENY_KEY_CONN_TIME, time);
			recordAnalytics(object);
		}
		catch (JSONException e)
		{
		
		}
	}

	public static void recordDisconnectionAnalytics(int reasonCode)
	{
		JSONObject object = new JSONObject();

		try
		{
			object.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.EVENT_TYPE_OFFLINE);
			object.put(HikeConstants.EVENT_KEY, AnalyticsConstants.EVENY_KEY_DISCONN_REA);
			object.put(AnalyticsConstants.TIP_KEY, reasonCode);
			recordAnalytics(object);
		}
		catch (JSONException e)
		{

		}
	}
	
	private static void recordAnalytics(JSONObject metaData)
	{
		if (metaData == null)
		{
			return;
		}
		HAManager.getInstance().record(HikeConstants.UI_EVENT, HikeConstants.LogEvent.CLICK, EventPriority.HIGH, metaData);
	}
}
