package com.bsb.hike.ui.utils;

import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * Created by atul on 07/06/16.
 */
public class CrossfadePageTransformer implements ViewPager.PageTransformer {
    @Override
    public void transformPage(View view, float position) {
        // Ensures the views overlap each other.
        view.setTranslationX(view.getWidth() * -position);

        // Alpha property is based on the view position.
        if (position <= -1.0F || position >= 1.0F) {
            view.setAlpha(0.0F);
        } else if (position == 0.0F) {
            view.setAlpha(1.0F);
        } else { // position is between -1.0F & 0.0F OR 0.0F & 1.0F
            view.setAlpha(1.0F - Math.abs(position));
        }
    }
}
