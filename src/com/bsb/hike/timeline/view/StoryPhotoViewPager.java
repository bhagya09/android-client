package com.bsb.hike.timeline.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.Interpolator;

import com.bsb.hike.ui.utils.ScrollerCustomDuration;
import com.bsb.hike.utils.Logger;

import java.lang.reflect.Field;

/**
 * Created by atul on 07/06/16.
 */
public class StoryPhotoViewPager extends ViewPager {

    private GestureDetector mGestureDetector;

    public StoryPhotoViewPager(Context context) {
        super(context);
        initViewPager();
    }

    public StoryPhotoViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViewPager();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(ev);
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }

    public void setGestureDetector(GestureDetector argGestureDetector) {
        mGestureDetector = argGestureDetector;
    }

    private ScrollerCustomDuration mScroller = null;

    /**
     * Override the Scroller instance with our own class so we can change the
     * duration
     */
    private void initViewPager() {
        try {
            Field scroller = ViewPager.class.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            Field interpolator = ViewPager.class.getDeclaredField("sInterpolator");
            interpolator.setAccessible(true);

            mScroller = new ScrollerCustomDuration(getContext(),
                    (Interpolator) interpolator.get(null));
            scroller.set(this, mScroller);
        } catch (IllegalAccessException | NoSuchFieldException e) { //
            Logger.e(StoryPhotoViewPager.class.getSimpleName(), "Some exception occured while initializing", e);
            e.printStackTrace();
        }
    }

    /**
     * Set the factor by which the duration will change
     */
    public void setScrollDurationFactor(double scrollFactor) {
        mScroller.setScrollDurationFactor(scrollFactor);
    }
}
