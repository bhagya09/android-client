package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;

import com.bsb.hike.utils.Utils;

public class CustomReceiveMessageTextView extends CustomMessageTextView
{
	private String TAG = "CustomReceiveMessageTextView";

	private static final int widthTime12Hour = 51;

	private static final int widthTime24Hour = 33;

	private static final int minOutMargin = 88;

	public CustomReceiveMessageTextView(Context context)
	{
		super(context);
		this.context = context;
	}

	public CustomReceiveMessageTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
	}

	public CustomReceiveMessageTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		this.context = context;
	}

	@Override
	protected int getMaximumTextWidth()
	{
		int diaplyWidthInDP = (int) (Utils.displayWidthPixels / Utils.scaledDensityMultiplier);
		return Math.min((diaplyWidthInDP - minOutMargin), MaxWidth);
	}

	@Override
	protected int getTimeStatusWidth24Hour()
	{
		return widthTime24Hour;
	}

	@Override
	protected int getTimeStatusWidth12Hour()
	{
		return widthTime12Hour;
	}
}
