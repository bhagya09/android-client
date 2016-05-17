package com.bsb.hike.tasks;

import java.util.Calendar;
import java.util.Locale;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

/**
 * Created by akhiltripathi on 08/03/16.
 */
public class SendDailyAnalyticsTask implements Runnable
{
    private static final String TAG = SendDailyAnalyticsTask.class.getSimpleName();

    public SendDailyAnalyticsTask()
    {

    }

    @Override
    public void run()
    {
        Logger.d(TAG, "SendDailyAnalyticsTask started.");

        //Add module specific analytics code here
        StickerManager.getInstance().sendStickerDailyAnalytics();

        ChatHeadUtils.resetBdayHttpCallInfo();

        Logger.d(TAG, "SendDailyAnalyticsTask completed with result: ");

        resetAnalyticsSendAlarm();

    }

    private void resetAnalyticsSendAlarm()
    {
        long scheduleTime = Utils.getTimeInMillis(Calendar.getInstance(Locale.ENGLISH), HikeMessengerApp.DEFAULT_SEND_ANALYTICS_TIME_HOUR, 0, 0, 0);

        if (scheduleTime < System.currentTimeMillis())
        {
            scheduleTime += HikeConstants.ONE_DAY_MILLS; // Next day at given time
        }

        HikeAlarmManager.setAlarmwithIntentPersistance(HikeMessengerApp.getInstance(), scheduleTime, HikeAlarmManager.REQUESTCODE_LOG_HIKE_ANALYTICS, false, IntentFactory.getPersistantAlarmIntent(), true);
    }
}