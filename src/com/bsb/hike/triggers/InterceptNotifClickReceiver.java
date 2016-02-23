package com.bsb.hike.triggers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.io.File;

/**
 * This is a broadcast receiver for actions which are broadcast when intercept notifications are clicked.
 */
public class InterceptNotifClickReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Logger.d(HikeConstants.INTERCEPTS.INTERCEPT_LOG, "received intercept click intent");

        String path = null;
        String eventKey = null;
        Intent actionIntent;
        boolean doesInterceptItemExist = false;

        String action = intent.getAction();
        String type = intent.getStringExtra(HikeConstants.INTERCEPTS.INTENT_EXTRA_TYPE);
        Uri interceptItem = intent.getParcelableExtra(HikeConstants.INTERCEPTS.INTENT_EXTRA_URI);

        //determine whether intercept file exists or not
        switch(type)
        {
            case InterceptUtils.INTERCEPT_TYPE_SCREENSHOT:
                type = HikeNotification.IMAGE;
                eventKey = AnalyticsConstants.InterceptEvents.INTERCEPT_SCREENSHOT;
                path = interceptItem.getPath();
                doesInterceptItemExist = InterceptUtils.doesInterceptItemExist(context, path, false);
                break;

            case InterceptUtils.INTERCEPT_TYPE_IMAGE:
                type = HikeNotification.IMAGE;
                eventKey = AnalyticsConstants.InterceptEvents.INTERCEPT_IMAGE;
                path = Utils.getAbsolutePathFromUri(interceptItem, context, false);
                doesInterceptItemExist = InterceptUtils.doesInterceptItemExist(context, path, true);
                break;

            case InterceptUtils.INTERCEPT_TYPE_VIDEO:
                type = HikeNotification.VIDEO;
                eventKey = AnalyticsConstants.InterceptEvents.INTERCEPT_VIDEO;
                path = Utils.getAbsolutePathFromUri(interceptItem, context, false);
                doesInterceptItemExist = InterceptUtils.doesInterceptItemExist(context, path, true);
                break;
        }

        Logger.d(HikeConstants.INTERCEPTS.INTERCEPT_LOG, "intercept path:" + path + "\t exists:" + doesInterceptItemExist);

        if(doesInterceptItemExist)
        {
            actionIntent = InterceptUtils.getInterceptActionIntent(context, action, type, path, eventKey, interceptItem);
            Logger.d(HikeConstants.INTERCEPTS.INTERCEPT_LOG, "processing intercept click action");
            IntentFactory.openInterceptActionActivity(context, actionIntent);
        }
        else
        {
            HAManager.getInstance().interceptAnalyticsEvent(eventKey, AnalyticsConstants.InterceptEvents.INTERCEPT_CLICK_FOR_DELETED_FILE, true);
        }

        //to clear the notification as it has been clicked.
        HikeNotification.getInstance().cancelNotification(HikeNotification.NOTIF_INTERCEPT_NON_DOWNLOAD);

        /* since clicking the notification is sending a broadcast instead of launching activity directly
         * the notification tray may not close automatically. so it must be closed programatically */
        Intent closeNotifTray = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeNotifTray);

    }
}
