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
        Intent actionIntent = null;
        boolean doesInterceptItemExist = false;

        String action = intent.getAction();
        String type = intent.getStringExtra(HikeConstants.INTERCEPTS.INTENT_EXTRA_TYPE);
        Uri interceptItem = intent.getParcelableExtra(HikeConstants.INTERCEPTS.INTENT_EXTRA_URI);

        //determine whether intercept file exists or not
        switch(type)
        {
            case InterceptManager.INTERCEPT_TYPE_SCREENSHOT:
                type = HikeNotification.IMAGE;
                eventKey = AnalyticsConstants.InterceptEvents.INTERCEPT_SCREENSHOT;
                path = interceptItem.getPath();
                File scrnFile = new File(path);

                if(scrnFile.exists())
                {
                    doesInterceptItemExist = true;
                }
                else
                {
                    Toast.makeText(context, R.string.unknown_file_error, Toast.LENGTH_SHORT).show();
                }

                break;

            case InterceptManager.INTERCEPT_TYPE_IMAGE:
            case InterceptManager.INTERCEPT_TYPE_VIDEO:
                if(type.equals(InterceptManager.INTERCEPT_TYPE_IMAGE))
                {
                    type = HikeNotification.IMAGE;
                    eventKey = AnalyticsConstants.InterceptEvents.INTERCEPT_IMAGE;
                }
                else if(type.equals(InterceptManager.INTERCEPT_TYPE_VIDEO))
                {
                    type = HikeNotification.VIDEO;
                    eventKey = AnalyticsConstants.InterceptEvents.INTERCEPT_VIDEO;
                }

                path = Utils.getAbsolutePathFromUri(interceptItem, context, false);
                Logger.d(HikeConstants.INTERCEPTS.INTERCEPT_LOG, "intercept path:"+path);

                if(!TextUtils.isEmpty(path))
                {
                    doesInterceptItemExist = true;
                }

                break;

        }

        if(doesInterceptItemExist)
        {
            switch (action)
            {
                case HikeNotification.INTERCEPT_NON_DWLD_SHARE_INTENT:
                    actionIntent = IntentFactory.getShareIntent(context,interceptItem, type);
                    HAManager.getInstance().interceptAnalyticsUIEvent(eventKey, AnalyticsConstants.InterceptEvents.INTERCEPT_SHARE_CLICKED);
                    break;

                case HikeNotification.INTERCEPT_SET_DP_INTENT:
                    actionIntent = IntentFactory.setDpIntent(context, interceptItem);
                    HAManager.getInstance().interceptAnalyticsUIEvent(eventKey, AnalyticsConstants.InterceptEvents.INTERCEPT_SET_DP_CLICKED);
                    break;

                case HikeNotification.INTERCEPT_PHOTO_EDIT_INTENT:
                    actionIntent = IntentFactory.getPictureEditorActivityIntent(context, path, true, null, false);
                    HAManager.getInstance().interceptAnalyticsUIEvent(eventKey, AnalyticsConstants.InterceptEvents.INTERCEPT_IMAGE_CLICKED);
                    break;

            }

            Logger.d(HikeConstants.INTERCEPTS.INTERCEPT_LOG, "processing intercept click action");
            IntentFactory.openInterceptActionActivity(context, actionIntent);
        }
        else
        {
            HAManager.getInstance().interceptAnalyticsUIEvent(eventKey, AnalyticsConstants.InterceptEvents.INTERCEPT_CLICK_FOR_DELETED_FILE);
        }

        //to clear the notification as it has been clicked.
        HikeNotification.getInstance().cancelNotification(HikeNotification.NOTIF_INTERCEPT_NON_DOWNLOAD);

        /* since clicking the notification is sending a broadcast instead of launching activity directly
         * the notification tray may not close automatically. so it must be closed programatically */
        Intent closeNotifTray = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeNotifTray);

    }
}
