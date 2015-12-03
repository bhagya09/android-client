package com.bsb.hike.ui.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class FitXImageView extends RecyclingImageView
{

	public FitXImageView(Context context)
	{
		super(context);
	}

	public FitXImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public FitXImageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		Drawable d = getDrawable();
		if (d != null && d.getIntrinsicHeight()!=0 && d.getIntrinsicWidth()!=0)
		{
			int w = MeasureSpec.getSize(widthMeasureSpec);
			int h = w * d.getIntrinsicHeight() / d.getIntrinsicWidth();
			setMeasuredDimension(w, h);
		}
		else
		{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}
}