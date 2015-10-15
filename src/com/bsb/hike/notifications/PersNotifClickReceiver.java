package com.bsb.hike.notifications;

import java.util.Calendar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PersNotifClickReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Long interval = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.PERSISTENT_NOTIF_ALARM, HikeConstants.PERS_NOTIF_ALARM_DEFAULT);
		Logger.d("UpdateTipPersistentNotif", "setting alarm for persistent notif");
		HikeNotification.getInstance().cancelPersistNotif();
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.IS_PERS_NOTIF_ALARM_SET, true);
		HikeAlarmManager.setAlarm(context, Calendar.getInstance().getTimeInMillis() + interval*1000, HikeAlarmManager.REQUESTCODE_UPDATE_PERSISTENT_NOTIF, false);
	}

}
