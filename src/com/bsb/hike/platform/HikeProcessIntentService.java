package com.bsb.hike.platform;

import java.util.Set;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.bsb.hike.models.LogAnalyticsEvent;
import com.bsb.hike.models.NormalEvent;
import com.bsb.hike.utils.Logger;

/**
 * Created by sagar on 30/10/15.
 */
public class HikeProcessIntentService extends IntentService
{
	public static final String TAG = "HikeProcessIntentService";

	public static final String SEND_NORMAL_EVENT_DATA = "sendNormalEventData";

	public static final String LOG_ANALYTICS_EVENT_DATA = "logAnalyticsEventData";

	public HikeProcessIntentService()
	{
		super("HikeProcessIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
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
			PlatformHelper.sendNormalEvent(normalEvent.messageHash, normalEvent.eventData, normalEvent.namespace);
		}
	}

	private void handleLogAnalyticsEvent(LogAnalyticsEvent logAnalyticsEvent)
	{
		if (logAnalyticsEvent != null)
		{
			PlatformHelper.logAnalytics(logAnalyticsEvent.isUI, logAnalyticsEvent.subType, logAnalyticsEvent.json, logAnalyticsEvent.botMsisdn, logAnalyticsEvent.botName);
		}
	}
}
