package com.bsb.hike.ui.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * Created by piyush on 23/08/14
 * This class adds stack like animations on flipping through pages in a viewpager
 */
public class DepthPageTransformer implements ViewPager.PageTransformer
{
	private static final float MIN_SCALE = 0.75f;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void transformPage(View view, float position)
	{
		int pageWidth = view.getWidth();

		if (position <= 0)
		{ // [-1,0]
			// Use the default slide transition when moving to the left page
			view.setAlpha(1);
			view.setTranslationX(0);
			view.setScaleX(1);
			view.setScaleY(1);

		}
		else if (position <= 1)
		{ // (0,1]
			// Fade the page out.
			view.setAlpha(1 - position);

			// Counteract the default slide transition
			view.setTranslationX(pageWidth * -position);

			// Scale the page down (between MIN_SCALE and 1)
			float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
			view.setScaleX(scaleFactor);
			view.setScaleY(scaleFactor);

		}
	}
}