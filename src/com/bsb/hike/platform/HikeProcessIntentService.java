package com.bsb.hike.platform;

import java.util.Set;

import android.app.IntentService;
import android.app.usage.UsageEvents;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.MqttConstants;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.AppState;
import com.bsb.hike.models.EventData;
import com.bsb.hike.models.LogAnalyticsEvent;
import com.bsb.hike.models.NormalEvent;
import com.bsb.hike.models.NotifData;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sagar on 30/10/15.
 */
public class HikeProcessIntentService extends IntentService
{
	public static final String TAG = "HikeProcessIntentService";

	public static final String SEND_NORMAL_EVENT_DATA = "sendNormalEventData";

	public static final String LOG_ANALYTICS_EVENT_DATA = "logAnalyticsEventData";

	public static final String SEND_APP_STATE = "sendAppState";

	public static final String EVENT_DELETE="eventDelete";

	public static final String NOTIF_DATA_DELETE = "notifDataDelete";

	public static final String NOTIF_DATA_PARTIAL_DELETE = "notifDataPartialDelete";

	public HikeProcessIntentService()
	{
		super("HikeProcessIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		if(intent == null)
			return;
		Bundle bundleData = intent.getExtras();
		if (bundleData != null)
		{
			Set<String> bundleKeys = bundleData.keySet();
			for (String bundleKey : bundleKeys)
			{
				switch (bundleKey)
				{
				case SEND_NORMAL_EVENT_DATA:
					if( bundleData.getParcelable(SEND_NORMAL_EVENT_DATA) instanceof NormalEvent )
					{
						NormalEvent normalEvent = bundleData.getParcelable(SEND_NORMAL_EVENT_DATA);
						handleSendNormalEvent(normalEvent);
					} else
					{
						Logger.e(TAG, "Data passed to SEND_NORMAL_EVENT_DATA is not instance of NormalEvent");
					}
					break;
				case LOG_ANALYTICS_EVENT_DATA:
					if( bundleData.getParcelable(LOG_ANALYTICS_EVENT_DATA) instanceof LogAnalyticsEvent )
					{
						LogAnalyticsEvent logAnalyticsEvent = bundleData.getParcelable(LOG_ANALYTICS_EVENT_DATA);
						handleLogAnalyticsEvent(logAnalyticsEvent);
					} else
					{
						Logger.e(TAG, "Data passed to LOG_ANALYTICS_EVENT_DATA is not instance of LogAnalyticsEvent");
					}
					break;
					case SEND_APP_STATE:
						if( bundleData.getParcelable(SEND_APP_STATE) instanceof AppState)
						{
							AppState appState = bundleData.getParcelable(SEND_APP_STATE);
							JSONObject jsonObject=null;
							try
							{
								jsonObject = new JSONObject(appState.json);
								sendAppState(jsonObject);
							}
							catch(JSONException e)
							{
								//TODO
							}
						} else
						{
							Logger.e(TAG, "Data passed is not parcelable");
						}
						break;
					case EVENT_DELETE:
						if( bundleData.getParcelable(EVENT_DELETE) instanceof EventData)
						{
							EventData eventData = bundleData.getParcelable(EVENT_DELETE);
							handleEventDelete(eventData);
						}
						else
						{
							Logger.e(TAG, "Data passed is not parcelable");
						}
						break;
					case NOTIF_DATA_DELETE:
						deleteNotifData(bundleData.getString(NOTIF_DATA_DELETE));
						break;
					case NOTIF_DATA_PARTIAL_DELETE:
						if( bundleData.getParcelable(NOTIF_DATA_PARTIAL_DELETE) instanceof NotifData)
						{
							NotifData notifData = bundleData.getParcelable(NOTIF_DATA_PARTIAL_DELETE);
							deletePartialNotifData(notifData);
						}
						else
						{
							Logger.e(TAG, "Data passed is not parcelable");
						}
						break;


				}

			}
		}

	}

	/**
	 * Handler for normal event received
	 * 
	 * @param normalEvent
	 */
	private void handleSendNormalEvent(NormalEvent normalEvent)
	{
		if (normalEvent != null)
		{
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(normalEvent.botMsisdn);
			PlatformHelper.sendNormalEvent(normalEvent.messageHash, normalEvent.eventData, normalEvent.namespace, botInfo);
		}
	}

	private void handleLogAnalyticsEvent(LogAnalyticsEvent logAnalyticsEvent)
	{
		if (logAnalyticsEvent != null)
		{
			PlatformHelper.logAnalytics(logAnalyticsEvent.isUI, logAnalyticsEvent.subType, logAnalyticsEvent.json, logAnalyticsEvent.botMsisdn, logAnalyticsEvent.botName);
		}
	}
	private void sendAppState(JSONObject jsonObject)
	{
		try
		{
			JSONObject data=jsonObject.getJSONObject(HikeConstants.DATA);
			String isForeGround=data.optString(HikeConstants.SUB_TYPE);
			if(Boolean.valueOf(isForeGround))
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.APP_FOREGROUNDED, null);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.IS_HIKE_APP_FOREGROUNDED, true);

			}
			else
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.APP_BACKGROUNDED, null);
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.IS_HIKE_APP_FOREGROUNDED, false);


			}
			HikeMqttManagerNew.getInstance().sendMessage(jsonObject, MqttConstants.MQTT_QOS_ZERO);
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void handleEventDelete(EventData eventData)
	{
		if(Boolean.valueOf(eventData.isEventId))
			PlatformHelper.deleteEvent(eventData.id);
		else
			PlatformHelper.deleteAllEventsForMessage(eventData.id);
	}

	public void deleteNotifData(String msisdn)
	{
		HikeConversationsDatabase.getInstance().deleteAllNotifDataForMicroApp(msisdn);
	}

	/**
	 * Call this function to delete partial notif data pertaining to a microApp. The key is the timestamp provided by Native
	 */
	public void deletePartialNotifData(NotifData notifData)
	{
		HikeConversationsDatabase.getInstance().deletePartialNotifData(notifData.key, notifData.msisdn);
	}
}
