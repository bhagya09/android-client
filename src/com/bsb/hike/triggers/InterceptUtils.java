package com.bsb.hike.triggers;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import java.io.File;

public class InterceptUtils
{
    private static final String TAG = "interceptManager";

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

}
