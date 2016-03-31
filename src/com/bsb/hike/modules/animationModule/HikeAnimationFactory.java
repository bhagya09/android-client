package com.bsb.hike.modules.animationModule;

import android.content.Context;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;

import com.bsb.hike.R;

public class HikeAnimationFactory
{	
	
	static int anim_repeat_count;
	public static Animation getPulsatingDotAnimation(int initialOffset)
	{
		AnimationSet animSet = new AnimationSet(true);
		float a = 1.75f;
		float pivotX = 0.5f;
		float pivotY = 0.5f;
		
		Animation anim0 = new ScaleAnimation(1, a, 1, a, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim0.setStartOffset(initialOffset);
		anim0.setDuration(2500);
		anim0.setRepeatCount(Animation.INFINITE);
		animSet.addAnimation(anim0);

		Animation fade = new AlphaAnimation(1, 0);
		fade.setInterpolator(new AccelerateInterpolator(2f));
		fade.setStartOffset(1500);
		fade.setDuration(1000);
		fade.setRepeatCount(Animation.INFINITE);
		animSet.addAnimation(fade);
		return animSet;
	}

	public static Animation getScaleFadeRingAnimation(int initialOffset)
	{
		AnimationSet animSet = new AnimationSet(true);
		float a = 1f;
		float pivotX = 0.5f;
		float pivotY = 0.5f;

		Animation anim0 = new ScaleAnimation(a, 0.50f, a,0.50f, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim0.setStartOffset(initialOffset);
		anim0.setDuration(1000);
		anim0.setRepeatCount(Animation.INFINITE);
		animSet.addAnimation(anim0);

		Animation fade = new AlphaAnimation(1, 0);
		fade.setInterpolator(new AccelerateInterpolator(2f));
		fade.setStartOffset(1500);
		fade.setDuration(500);
		fade.setRepeatCount(Animation.INFINITE);
		animSet.addAnimation(fade);

		return animSet;
	}

	public static Animation getScaleInAnimation(int initialOffset)
	{
		AnimationSet animSet = new AnimationSet(true);
		float a = 1f;
		float pivotX = 0.5f;
		float pivotY = 0.75f;

		Animation anim0 = new ScaleAnimation(a, 0.0f, a,0.0f, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim0.setStartOffset(initialOffset);
		anim0.setDuration(500);
		animSet.addAnimation(anim0);

		return anim0;
	}

	public static AnimationSet getHikeActionBarLogoAnimation(Context context)
	{
		anim_repeat_count = 2;
		final AnimationSet animSet = (AnimationSet) AnimationUtils.loadAnimation(context, R.anim.hike_logo_stealth_anim);

		animSet.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				anim_repeat_count--;
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				animation.reset();
				if(anim_repeat_count > 0)
				{
					animation.start();
				}
			}
		});
		return animSet;
	}
	
	public static AnimationSet getStickerShopIconAnimation(Context context)
	{
		final AnimationSet animSet = (AnimationSet) AnimationUtils.loadAnimation(context, R.anim.sticker_shop_icon_anim);

		animSet.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				animation.reset();
				animation.start();
			}
		});
		return animSet;
	}
}
