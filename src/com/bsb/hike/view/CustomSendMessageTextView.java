package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;

public class CustomSendMessageTextView extends CustomMessageTextView
{

	private String TAG = "CustomSendMessageTextView";

	private static final int widthTime12HourDefault = 75;

	private static final int widthTime24HourDefault = 57;

	private static final int widthTime12HourBroadcast = 90;

	private static final int widthTime24HourBroadcast = 72;

	private int widthTime12Hour = widthTime12HourDefault;

	private int widthTime24Hour = widthTime24HourDefault;

	public CustomSendMessageTextView(Context context)
	{
		super(context);
	}

	public CustomSendMessageTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public CustomSendMessageTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public void setDefaultLength()
	{
		widthTime12Hour = widthTime12HourDefault;
		widthTime24Hour = widthTime24HourDefault;
	}

	public void setBroadcastLength()
	{
		widthTime12Hour = widthTime12HourBroadcast;
		widthTime24Hour = widthTime24HourBroadcast;
	}

	@Override
	protected int getMaximumTextWidth()
	{
		return MaxWidth;
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
