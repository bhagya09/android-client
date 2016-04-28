package com.bsb.hike.media;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.View;
import android.widget.TextView;

/**
 * Created by nidhi on 23/03/16.
 */
public class HikeTipVisibilityAnimator {

    private ObjectAnimator mTipVisiblityAnimation;
    private View inflatedView;
    int mTipViewResID;
    public static final long TIP_ANIMATION_LENGTH_SHORT = 2000;
    public static final long TIP_ANIMATION_LENGTH_LONG = 3500;

    public HikeTipVisibilityAnimator(final int stringResId, View parentView, final Activity activity, int tipViewResID){
        initializeInfoTip(stringResId, parentView, activity, tipViewResID, TIP_ANIMATION_LENGTH_SHORT);
    }

    public HikeTipVisibilityAnimator(final int stringResId, View parentView, final Activity activity, int tipViewResID, long duration){
        initializeInfoTip(stringResId, parentView, activity, tipViewResID, duration);
    }

    private void initializeInfoTip(final int stringResId, View parentView, final Activity activity, int tipViewResID, long duration) {
        inflatedView = parentView;
        mTipViewResID = tipViewResID;
        final TextView tip = (TextView) inflatedView.findViewById(tipViewResID);
        mTipVisiblityAnimation = ObjectAnimator.ofFloat(tip, "alpha", 0.9f, 1.0f);
        mTipVisiblityAnimation.setRepeatCount(1);
        mTipVisiblityAnimation.setRepeatMode(ValueAnimator.REVERSE);
        mTipVisiblityAnimation.setDuration(100);
        mTipVisiblityAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (activity == null || activity.isFinishing()) {
                    return;
                }
                tip.setText(stringResId);
                tip.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                dismissInfoTipIfShowing();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                mIsAnimShown = true;
                animation.setDuration(1000);
            }
        });
    }

    private boolean mIsAnimShown = false;

    public boolean isTipShownForMinDuration() {
        return mIsAnimShown;
    }

    public void startInfoTipAnim() {
        mIsAnimShown = false;
        mTipVisiblityAnimation.setDuration(100);
        mTipVisiblityAnimation.start();
    }

    public boolean isShowingInfoTip() {
        if (mTipVisiblityAnimation != null) return mTipVisiblityAnimation.isStarted();

        TextView tip = (TextView) inflatedView.findViewById(mTipViewResID);
        if (tip != null && tip.getVisibility() == View.VISIBLE)
            return true;

        if (mTipVisiblityAnimation != null && mTipVisiblityAnimation.isStarted())
            return true;

        return false;
    }

    public void dismissInfoTipIfShowing() {
        if (mTipVisiblityAnimation != null && mTipVisiblityAnimation.isStarted()) {
            final TextView tip = (TextView) inflatedView.findViewById(mTipViewResID);
            tip.setVisibility(View.GONE);
            mTipVisiblityAnimation.cancel();
        }
    }

}
