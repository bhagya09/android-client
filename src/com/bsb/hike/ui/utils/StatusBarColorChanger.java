package com.bsb.hike.ui.utils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Utils;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

public class StatusBarColorChanger
{

	public static void changeStatusBarColorifnotInTranslucentState(Activity activity, String color)
	{
		if (Utils.isLollipopOrHigher() && activity != null)
		{
			Window window = activity.getWindow();
			if (window != null)
				switch (color)
				{
				case HikeConstants.STATUS_BAR_BLUE:
					window.setStatusBarColor(Color.parseColor("#1993CB"));
					break;
				case HikeConstants.STATUS_BAR_TRANSPARENT:
					window.setStatusBarColor(Color.BLACK);
					break;
				default:
					break;
				}

		}
	}

	public static void setStatusBarColor(Activity activity, String color)
	{
		if (activity != null)
			setStatusBarColor(activity.getWindow(), color);
	}

	public static void setStatusBarColor(Window window, String color)
	{
		if (Utils.isLollipopOrHigher() && window != null)
		{

			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			switch (color)
			{
			case HikeConstants.STATUS_BAR_BLUE:
				window.setStatusBarColor(Color.parseColor("#1993CB"));
				break;
			case HikeConstants.STATUS_BAR_TRANSPARENT:
				window.setStatusBarColor(Color.BLACK);
				break;
			default:
				break;
			}

		}

	}
}
