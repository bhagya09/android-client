package com.bsb.hike.triggers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.ui.ProfilePicActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class InterceptUtils
{
    private static final String TAG = "interceptUtils";

    public static final String INTERCEPT_TYPE_SCREENSHOT = "intercept_scrnShot";

    public static final String INTERCEPT_TYPE_IMAGE = "intercept_image";

    public static final String INTERCEPT_TYPE_VIDEO = "intercept_video";

    private static InterceptFileObserver screenshotObserver;

    /**
     * Method to determine whether to show the intercept or not. Checks user's pref in settings and fg-bg as needed.
     * @param whichIntercept the type of intercept
     * @return
     */
    public static boolean shouldShowGivenIntercept(String whichIntercept)
    {
        HikeSharedPreferenceUtil hikeSharedPrefInstance = HikeSharedPreferenceUtil.getInstance();
        switch (whichIntercept)
        {
            case INTERCEPT_TYPE_SCREENSHOT:
                return (hikeSharedPrefInstance.getData(HikeConstants.INTERCEPTS.SHOW_SCREENSHOT_INTERCEPT, false)
                        && hikeSharedPrefInstance.getData(HikeConstants.INTERCEPTS.ENABLE_SCREENSHOT_INTERCEPT, false));

            case INTERCEPT_TYPE_IMAGE:
                return (hikeSharedPrefInstance.getData(HikeConstants.INTERCEPTS.SHOW_IMAGE_INTERCEPT, false)
                        && hikeSharedPrefInstance.getData(HikeConstants.INTERCEPTS.ENABLE_IMAGE_INTERCEPT, false)
                        && !hikeSharedPrefInstance.getData(HikeConstants.IS_HIKE_APP_FOREGROUNDED, false));

            case INTERCEPT_TYPE_VIDEO:
                return (hikeSharedPrefInstance.getData(HikeConstants.INTERCEPTS.SHOW_VIDEO_INTERCEPT, false)
                        && hikeSharedPrefInstance.getData(HikeConstants.INTERCEPTS.ENABLE_VIDEO_INTERCEPT, false)
                        && !hikeSharedPrefInstance.getData(HikeConstants.IS_HIKE_APP_FOREGROUNDED, false));

            default:
                Logger.d(HikeConstants.INTERCEPTS.INTERCEPT_LOG, "unknown intercept type. returning false.");
                return false;
        }
    }

    public static void registerOrUnregisterScreenshotObserver()
    {
        HikeHandlerUtil.getInstance().postRunnable(new Runnable()
        {
            @Override
            public void run()
            {
                if (shouldShowGivenIntercept(INTERCEPT_TYPE_SCREENSHOT))
                {
                    String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator + InterceptFileObserver.SCREENSHOT_DIR;
                    if (screenshotObserver == null)
                    {
                        screenshotObserver = new InterceptFileObserver(path, INTERCEPT_TYPE_SCREENSHOT);
                        screenshotObserver.startWatching();
                        Logger.d(TAG, "started watching screenshot directory:" + path);
                    }
                }
                else
                {
                    unregisterScreenshotObserver();
                }
            }
        });
    }

    public static void unregisterScreenshotObserver()
    {
        if (screenshotObserver != null)
        {
            screenshotObserver.stopWatching();
            screenshotObserver = null;
            Logger.d(TAG, "stopped watching screenshot directory");
        }
    }

    public static boolean doesInterceptItemExist(Context context, String path, boolean isImageOrVideo)
    {
        if(isImageOrVideo)
        {
            if(!TextUtils.isEmpty(path))
            {
                return true;
            }
        }
        else
        {
            File scrnFile = new File(path);

            if(scrnFile.exists())
            {
                return true;
            }
            else
            {
                Toast.makeText(context, R.string.unknown_file_error, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return false;
    }

    public static Intent getInterceptActionIntent(Context context, String broadcastAction, String fileType, String path, String eventKey, Uri interceptItem)
    {
        Intent actionIntent = null;
        switch (broadcastAction)
        {
            case HikeNotification.INTERCEPT_NON_DWLD_SHARE_INTENT:
                try
                {
                    actionIntent = IntentFactory.getShareIntent(context, interceptItem, fileType);
                }
                catch(NullPointerException npe)
                {
                    npe.printStackTrace();
                }
                
                HAManager.getInstance().interceptAnalyticsEvent(eventKey, AnalyticsConstants.InterceptEvents.INTERCEPT_SHARE_CLICKED, true);
                break;

            case HikeNotification.INTERCEPT_VIDEO_SHARE_INTENT:
                try
                {
                    actionIntent = IntentFactory.getShareIntent(context, interceptItem, fileType);
                }
                catch(NullPointerException npe)
                {
                    npe.printStackTrace();
                }

                recordInterceptEventV2(eventKey, AnalyticsConstants.InterceptEvents.INTERCEPT_VIDEO_CLICKED, AnalyticsConstants.CLICK_EVENT);
                break;

            case HikeNotification.INTERCEPT_SET_DP_INTENT:
                try
                {
                    actionIntent = IntentFactory.setDpIntent(context, interceptItem);
                }
                catch(NullPointerException npe)
                {
                    npe.printStackTrace();
                }

                HAManager.getInstance().interceptAnalyticsEvent(eventKey, AnalyticsConstants.InterceptEvents.INTERCEPT_SET_DP_CLICKED, true);
                break;

            case HikeNotification.INTERCEPT_PHOTO_EDIT_INTENT:
                try
                {
                    actionIntent = IntentFactory.getShareIntent(context, interceptItem, fileType);
                }
                catch(NullPointerException npe)
                {
                    npe.printStackTrace();
                }

                HAManager.getInstance().interceptAnalyticsEvent(eventKey, AnalyticsConstants.InterceptEvents.INTERCEPT_IMAGE_CLICKED, true);
                break;

        }
        return actionIntent;
    }

    public static void recordInterceptEventV2(String cls, String order, String genus)
    {
        JSONObject eventJSON = new JSONObject();
        try
        {
            eventJSON.put(AnalyticsConstants.V2.KINGDOM, AnalyticsConstants.InterceptEvents.ACT_INTERCEPT);
            eventJSON.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.InterceptEvents.INTERCEPTS);
            eventJSON.put(AnalyticsConstants.V2.CLASS, cls);
            eventJSON.put(AnalyticsConstants.V2.UNIQUE_KEY, order);
            eventJSON.put(AnalyticsConstants.V2.ORDER, order);
            eventJSON.put(AnalyticsConstants.V2.GENUS, genus);
            HAManager.getInstance().recordV2(eventJSON);
        }
        catch (JSONException jse)
        {
            Logger.d(TAG, "error in parsing event json");
            jse.printStackTrace();
        }
    }

}
