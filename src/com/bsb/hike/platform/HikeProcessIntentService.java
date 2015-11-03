package com.bsb.hike.platform;

import java.util.Set;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.bsb.hike.models.LogAnalyticsEvent;
import com.bsb.hike.models.NormalEvent;

/**
 * Created by sagar on 30/10/15.
 */
public class HikeProcessIntentService extends IntentService
{

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
					NormalEvent normalEvent = bundleData.getParcelable(SEND_NORMAL_EVENT_DATA);
					handleSendNormalEvent(normalEvent);
					break;
				case LOG_ANALYTICS_EVENT_DATA:
					LogAnalyticsEvent logAnalyticsEvent = bundleData.getParcelable(LOG_ANALYTICS_EVENT_DATA);
					handleLogAnalyticsEvent(logAnalyticsEvent);
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
