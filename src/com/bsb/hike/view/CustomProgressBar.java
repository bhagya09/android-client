package com.bsb.hike.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

import com.bsb.hike.utils.Utils;

public class CustomProgressBar extends ProgressBar
{
	private int start = 0;

	private int target = 0;

	private int duration = 0;

	private ObjectAnimator animation;

	private boolean nonFirstProgress = false;

	private long msgId = -1;

	public CustomProgressBar(Context context)
	{
		super(context);
		// TODO Auto-generated constructor stub
	}

	public CustomProgressBar(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public CustomProgressBar(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	@Override
	public synchronized void setProgress(int progress)
	{
		// TODO Auto-generated method stub
		progress = filterPercentValue(progress);
		super.setProgress(progress);
	}

	public synchronized void setAnimatedProgress(int start, int target, int duration, long id)
	{
		start = filterPercentValue(start);
		target = filterPercentValue(target);

		if ((this.target == target) && (this.start == start) && (this.duration == duration))
			return;

		setStart(start);
		setTarget(target);
		setDuration(duration);

		if ((!nonFirstProgress) || (this.msgId != id))
		{
			nonFirstProgress = true;
			this.msgId = id;
			this.setProgress(this.target);
			return;
		}

		if (this.target <= this.start)
		{
			this.stopAnimation();
			this.setProgress(this.target);
			return;
		}

		if (this.target >= 99)
		{
			this.stopAnimation();
			this.setProgress(this.start);
			return;
		}

		if (this.duration <= 0)
		{
			this.stopAnimation();
			this.setProgress(this.target);
			return;
		}

		if (animation == null)
		{
			animation = ObjectAnimator.ofInt(this, "progress", this.start, this.target);
			animation.setDuration(this.duration); // 0.5 second
			animation.setInterpolator(new LinearInterpolator());
			if (android.os.Build.VERSION.SDK_INT >= 18)
				animation.setAutoCancel(true);
			animation.start();
		}
		else
		{
			animation.setIntValues(this.start, this.target);
			// animation.setIntValues(this.target);
			animation.setDuration(this.duration);
			animation.start();
		}

		return;
	}

	public void stopAnimation()
	{
		if (animation != null)
		{
			animation.cancel();
		}
	}

	protected void setStart(int value)
	{
		this.start = value;
	}

	protected void setTarget(int value)
	{
		this.target = value;
	}

	protected void setDuration(int value)
	{
		this.duration = value;
	}

	protected int filterPercentValue(int value)
	{
		if (value < 0)
			return 0;
		else if (value > 100)
			return 100;
		else
			return value;
	}
}
