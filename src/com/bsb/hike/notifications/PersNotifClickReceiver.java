package com.bsb.hike.notifications;

import java.util.Calendar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class PersNotifClickReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

        switch (intent.getAction())
        {
            case HikeNotification.NOTIF_URL_INTENT:
                Intent closeNofifTray = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(closeNofifTray);
                HAManager.getInstance().updateTipAndNotifAnalyticEvent(AnalyticsConstants.UPDATE_PERSISTENT_NOTIF,
                        AnalyticsConstants.PERSISTENT_NOTIF_CLICKED, AnalyticsConstants.CLICK_EVENT);
                Uri url = Uri.parse(intent.getStringExtra(HikeConstants.PERSISTENT_NOTIF_URL));
                Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "processing update click. opening url:"+url);
                Intent openUrl = new Intent(Intent.ACTION_VIEW, url);
                openUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(openUrl);
                break;
            case HikeNotification.NOTIF_ALARM_INTENT:
                Long interval = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.PERSISTENT_NOTIF_ALARM, HikeConstants.PERS_NOTIF_ALARM_DEFAULT);
                Logger.d(HikeConstants.UPDATE_TIP_AND_PERS_NOTIF_LOG, "setting alarm for persistent notif for interval:"+interval);
                HikeNotification.getInstance().cancelPersistNotif();
                HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.IS_PERS_NOTIF_ALARM_SET, true);
                HikeAlarmManager.setAlarmPersistance(context, Calendar.getInstance().getTimeInMillis() + interval*1000, HikeAlarmManager.REQUESTCODE_UPDATE_PERSISTENT_NOTIF, false, true);
                break;
        }

	}

}
