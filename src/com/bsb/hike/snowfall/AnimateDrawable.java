package com.bsb.hike.snowfall;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

public class AnimateDrawable extends ProxyDrawable
{

	private Animation mAnimation;

	private Transformation mTransformation = new Transformation();

	public AnimateDrawable(Drawable targetSmall, Drawable targetLarge, Drawable targetTrans, Animation animation)
	{
		super(targetSmall, targetLarge, targetTrans);
		mAnimation = animation;
	}

	public Animation getAnimation()
	{
		return mAnimation;
	}

	public void setAnimation(Animation anim)
	{
		mAnimation = anim;
	}

	public boolean hasStarted()
	{
		return mAnimation != null && mAnimation.hasStarted();
	}

	public boolean hasEnded()
	{
		return mAnimation == null || mAnimation.hasEnded();
	}

	@Override
	public void draw(Canvas canvas)
	{
		Drawable dr = getProxy();
		if (dr != null)
		{
			dr.setBounds(0, 0, this.size, this.size);
			int sc = canvas.save();
			Animation anim = mAnimation;
			if (anim != null)
			{
				anim.getTransformation(AnimationUtils.currentAnimationTimeMillis() + 5000, mTransformation);
				canvas.concat(mTransformation.getMatrix());
			}
			dr.draw(canvas);
			canvas.restoreToCount(sc);
		}
	}
}
