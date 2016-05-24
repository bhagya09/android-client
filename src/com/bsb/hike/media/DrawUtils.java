package com.bsb.hike.media;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.bsb.hike.HikeMessengerApp;

/**
 * Created by nidhi on 09/03/16.
 */
public class DrawUtils {

    public static float density = 1;
    public static Point displaySize = new Point();
    public static DisplayMetrics displayMetrics = new DisplayMetrics();
    public static boolean usingHardwareInput;

    public static void checkDisplaySize() {
        try {
            Configuration configuration = HikeMessengerApp.getInstance().getApplicationContext().getResources().getConfiguration();
            usingHardwareInput = configuration.keyboard != Configuration.KEYBOARD_NOKEYS && configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
            WindowManager manager = (WindowManager) HikeMessengerApp.getInstance().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                Display display = manager.getDefaultDisplay();
                if (display != null) {
                    display.getMetrics(displayMetrics);
                    display.getSize(displaySize);
                    Log.e("tmessages", "display size = " + displaySize.x + " " + displaySize.y + " " + displayMetrics.xdpi + "x" + displayMetrics.ydpi);
                }
            }
        } catch (Exception e) {
            Log.e("tmessages", e.getMessage());
        }
    }


    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    static {
        density = HikeMessengerApp.getInstance().getApplicationContext().getResources().getDisplayMetrics().density;
        checkDisplaySize();
    }

}
