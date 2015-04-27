package com.bsb.hike.ui.utils;

import android.content.Context;
import android.util.AttributeSet;

public class SquareRecyclingImageView extends RecyclingImageView
{

	public SquareRecyclingImageView(Context context)
	{
		super(context);
	}

	public SquareRecyclingImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public SquareRecyclingImageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}
}
