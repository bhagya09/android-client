package com.bsb.hike.triggers;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;

/**
 * This broadcast receivers responds to actions broadcast by system when a
 * picture is clicked or video is recorded through phone's native camera app
 */
public class ImageAndVideoInterceptReceiver extends BroadcastReceiver
{

	private final String TAG = "ImageAndVideoInterception";
	/**
	 * This is an undocumented broadcast action when a new picture is
	 * taken by the camera and its entry has been added to the media store.
	 * {@link android.content.Intent#getData} is URI of the picture.
	 */
	private final String ACTION_NEW_PICTURE = "com.android.camera.NEW_PICTURE";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Logger.d(TAG, "received intercept intent");

        Uri currUri = intent.getData();

        if(!currUri.equals(Uri.parse(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.INTERCEPTS.PREV_MEDIA_URI, "null"))))
        {
            switch (intent.getAction())
            {
                case Camera.ACTION_NEW_VIDEO:
                    if (InterceptUtils.shouldShowGivenIntercept(InterceptUtils.INTERCEPT_TYPE_VIDEO))
                    {
                        Logger.d(TAG, "processing action:" + intent.getAction());
                        Logger.d(TAG, "processing video intercept");
                        InterceptUtils.recordInterceptEventV2(AnalyticsConstants.InterceptEvents.INTERCEPT_VIDEO, AnalyticsConstants.InterceptEvents.VIDEO_CAPTURE, AnalyticsConstants.InterceptEvents.CAPTURE);
                        HikeNotification.getInstance().notifyIntercept(currUri, null, null, InterceptUtils.INTERCEPT_TYPE_VIDEO);
                    }
                    break;

                /* Different devices broadcast either one or both actions on picture click.
                 * In case of both, it is handled by checking uri of previous and current intercept items */
                case ACTION_NEW_PICTURE:
                case Camera.ACTION_NEW_PICTURE:
                    if (InterceptUtils.shouldShowGivenIntercept(InterceptUtils.INTERCEPT_TYPE_IMAGE))
                    {
                        Logger.d(TAG, "processing action:" + intent.getAction());
                        Logger.d(TAG, "processing image intercept");
                        InterceptUtils.recordInterceptEventV2(AnalyticsConstants.InterceptEvents.INTERCEPT_IMAGE, AnalyticsConstants.InterceptEvents.IMAGE_CAPTURE, AnalyticsConstants.InterceptEvents.CAPTURE);
                        HikeNotification.getInstance().notifyIntercept(currUri, null, null, InterceptUtils.INTERCEPT_TYPE_IMAGE);
                    }
                    break;
            }
        }

        HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.INTERCEPTS.PREV_MEDIA_URI, currUri.toString());

	}
}
