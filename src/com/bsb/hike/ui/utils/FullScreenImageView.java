package com.bsb.hike.ui.utils;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Adjusts height according to given width.
 * 
 * @author Atul M
 */
public class FullScreenImageView extends RecyclingImageView
{

	public FullScreenImageView(Context context)
	{
		super(context);
	}

	public FullScreenImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public FullScreenImageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		try
		{
			int width = MeasureSpec.getSize(widthMeasureSpec);
			int height = width * getDrawable().getIntrinsicHeight() / getDrawable().getIntrinsicWidth();
			setMeasuredDimension(width, height);
		}
		catch (NullPointerException npe)
		{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}
}
