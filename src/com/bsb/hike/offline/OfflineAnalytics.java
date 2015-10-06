package com.bsb.hike.offline;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.offline.OfflineConstants.AnalyticsConstants;
import com.bsb.hike.offline.OfflineConstants.DisconnectFragmentType;
import com.bsb.hike.utils.Logger;

//import com.bsb.hike.offline.OfflineConstants.AnalyticsEvents;

/**
 * 
 * @author himanshu This class is used to record Analytics related to Offline Messaging
 */
public class OfflineAnalytics
{

	private static final String TAG = OfflineAnalytics.class.getName();

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
			object.put(HikeConstants.TAG,AnalyticsConstants.EVENY_KEY_CONN_TIME);
			object.put("tym",time);
			recordAnalytics(object);
		}
		catch (JSONException e)
		{

		}
	}

	public static void recordDisconnectionAnalytics(int reasonCode, long connectionId)
	{
		JSONObject object = new JSONObject();

		try
		{
			object.put("connId", connectionId);
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

	public static void retryButtonClicked()
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.EVENT_TYPE_OFFLINE);
			object.put(HikeConstants.EVENT_KEY, AnalyticsConstants.EVENT_KEY_PUSH);
			object.put(HikeConstants.TAG, AnalyticsConstants.RETRY_BUTTON_CLICKED);
			recordAnalytics(object);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Exception while recording offline retry button Analytics");
		}
	}

	public static void closeAnimationCrossClicked()
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.EVENT_TYPE_OFFLINE);
			object.put(HikeConstants.EVENT_KEY, AnalyticsConstants.EVENT_KEY_CANCEL);
			recordAnalytics(object);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Exception while recording cross click offline Analytics");
		}
	}

	// type 0 is popup-1(Connecting case)
	// type 1 is popup-2(Connected case)
	public static void disconnectPopupClicked(DisconnectFragmentType type, int itemClicked)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.EVENT_TYPE_OFFLINE);
			switch (type)
			{
			case CONNECTING:
				object.put(HikeConstants.EVENT_KEY, AnalyticsConstants.EVENT_KEY_CONNECTING_POP_UP_CLICK);
				break;
			case CONNECTED:
				object.put(HikeConstants.EVENT_KEY, AnalyticsConstants.EVENT_KEY_CONNECTED_POP_UP_CLICK);
				break;
			case REQUESTING:
				break;
			default:
				break;
			}
			object.put(HikeConstants.TAG, itemClicked);
			recordAnalytics(object);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Exception while recording offline disconnection popup analytics");
		}

	}

	public static void recordSessionAnalytics(JSONObject md) throws JSONException
	{
		md.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.EVENT_TYPE_OFFLINE);
		md.put(HikeConstants.EVENT_KEY, "msg");

		HAManager.getInstance().record(HikeConstants.UI_EVENT, HikeConstants.LogEvent.CLICK, EventPriority.HIGH, md);

	}

	public static void offlineOverflowIndicatorClicked()
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.EVENT_TYPE, AnalyticsConstants.EVENT_TYPE_OFFLINE);
			object.put(HikeConstants.EVENT_KEY, AnalyticsConstants.EVENT_KEY_PUSH);
			object.put(HikeConstants.TAG, AnalyticsConstants.OFFLINE_RED_DOT_CLICKED);
			recordAnalytics(object);
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Exception in logging offline overflow indicator click");
		}
		
	}

}
