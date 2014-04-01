package com.bsb.hike.view;

import com.bsb.hike.utils.Utils;

import android.text.Layout;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class CustomSendMessageTextView extends CustomFontTextView
{
	//private String TAG = "CustomSendMessageTextView";
	
	private static final int widthTime12Hour = 75;
	
	private static final int widthTime24Hour = 56;
	
	private static final int widthMargin = 100;
	
	private static final int heightTime = 14;

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

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		try
		{
			int lastLineWidth = 0;
			int linesMaxWidth = 0;
			int lines = 0;
			
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			Layout layout = getLayout();
			lines = layout.getLineCount();
			float lastLine = layout.getLineWidth(lines - 1);
			int viewHeight = 0;

			linesMaxWidth = lastLineWidth = (int) Math.ceil(lastLine);

			//Log.d(TAG, "lastLine: " + lastLine + ", density multiplier: " + Utils.densityMultiplier);

			for (int n = 0; n < lines; ++n)
			{
				float lineWidth;
				int lineHeight;
				try
				{
					lineWidth = layout.getLineWidth(n);
					lineHeight = (layout.getLineTop(n+1) - layout.getLineTop(n));
					viewHeight += lineHeight;
					//Log.d(TAG, "LINE NO. " + n + ", Width: " + lineWidth + ", Height: " + lineHeight + ", Height in dp: " + lineHeight/Utils.densityMultiplier);
				}
				catch (Exception e)
				{
					return;
				}

				linesMaxWidth = Math.max(linesMaxWidth, (int) Math.ceil(lineWidth));
			}
			
			int heightAddition = heightTime;
			int widthAddition;
			if (android.text.format.DateFormat.is24HourFormat(getContext()))
			{
				widthAddition = widthTime24Hour;
			}
			else
			{
				widthAddition = widthTime12Hour;
			}

			if (getContext().getResources().getDisplayMetrics().widthPixels - lastLineWidth > ((widthAddition + widthMargin) * Utils.densityMultiplier))
			{
				int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
				int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
				//Log.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				parentHeight = viewHeight;
				//Log.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				linesMaxWidth = Math.max(linesMaxWidth, (int) ((widthAddition * Utils.densityMultiplier) + lastLineWidth));
				this.setMeasuredDimension(linesMaxWidth, parentHeight);
			}
			else
			{
				int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
				int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
				//Log.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				parentHeight = (int) (viewHeight + (heightAddition * Utils.densityMultiplier));
				//Log.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				this.setMeasuredDimension(linesMaxWidth, parentHeight);
			}
		}
		catch (Exception e)
		{
			try
			{
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
			catch (Exception e2)
			{
				setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
			}
		}
	}
}
