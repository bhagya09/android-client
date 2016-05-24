package com.bsb.hike.ui.utils;

import android.app.Activity;
import android.graphics.Color;
import android.view.Window;
import android.view.WindowManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Utils;

public class StatusBarColorChanger
{

	public static final String DEFAULT_STATUS_BAR_COLOR = "black";
	
	public static void setStatusBarColor(Activity activity, String color)
	{
		if (activity != null)
			setStatusBarColor(activity.getWindow(), color);
	}
	/**
	 * @param activity  
	 * @param resIdcolor  unresolved color id  which will be resolved e.g R.color.mycolor
	 */
	public static void setStatusBarColor(Activity activity, int resIdcolor)
	{
		if (activity != null)
			setStatusBarColor(activity.getWindow(), activity.getResources().getColor(resIdcolor));
	}

	public static void setStatusBarColor(Window window, String color)
	{
		if (Utils.isLollipopOrHigher() && window != null)
		{

			
			switch (color)
			{
			case HikeConstants.STATUS_BAR_BLUE:
				setStatusBarColor(window,Color.parseColor("#1993CB"));
				break;
			case HikeConstants.STATUS_BAR_TRANSPARENT:
				setStatusBarColor(window,Color.BLACK);
				break;
			case HikeConstants.STATUS_BAR_TIMELINE:
				setStatusBarColor(window,0xE6000000);
				break;
			default:
				break;
			}
			
		}

	}

	public static void setStatusBarColor(Window window, int color)
	{
		if (Utils.isLollipopOrHigher() && window != null)
		{
		    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(color);
		}

	}
}
