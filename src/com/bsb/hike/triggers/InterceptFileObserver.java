package com.bsb.hike.triggers;

import java.io.File;

import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.utils.Logger;
import android.os.FileObserver;
import android.text.TextUtils;

public class InterceptFileObserver extends FileObserver
{
	public String absolutePath;

    private final String TAG = "InterceptFileObserver";

    public static final String SCREENSHOT_DIR = "Screenshots";

	String interceptType;

	private static int lastEvent, secondLastEvent, thirdLastEvent;

	public InterceptFileObserver(String path, String interceptType)
	{
		super(path, FileObserver.ALL_EVENTS);
		absolutePath = path;
		this.interceptType = interceptType;
	}

    /**
     * Method to check whether a sequence of events indicating file creation has occured
     * @param event the current event which has triggered the file observer
     * @return true or false
     */
    private boolean checkValidEvents(int event)
    {
        if(event == FileObserver.CLOSE_WRITE && lastEvent == FileObserver.MODIFY && secondLastEvent == FileObserver.MODIFY)
        {
          return true;
        }
        return false;
    }

	@Override
	public void onEvent(int event, String path)
	{
		if (!TextUtils.isEmpty(path))
		{
			File file = new File(absolutePath + File.separator + path);
			Logger.d(TAG, event + "" +"\tpath:"+path);

            if (file.exists() && checkValidEvents(event))
            {
                Logger.d(TAG, "event:" + event + " lastevent:" + lastEvent + " secondLastEvent:" + secondLastEvent
                        + " thirdLastEvent:" + thirdLastEvent + " path:" + absolutePath + File.separator + path);
                InterceptUtils.recordInterceptEventV2(AnalyticsConstants.InterceptEvents.INTERCEPT_SCREENSHOT, AnalyticsConstants.InterceptEvents.SCREENSHOT_CAPTURE, AnalyticsConstants.InterceptEvents.CAPTURE);
                HikeNotification.getInstance().notifyIntercept(null, absolutePath + File.separator, path, interceptType);
            }
            thirdLastEvent = secondLastEvent;
            secondLastEvent = lastEvent;
            lastEvent = event;
		}
	}
}
