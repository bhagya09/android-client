package com.bsb.hike.view;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class CustomReceiveMessageTextView extends CustomFontTextView
{
	private String TAG = "CustomReceiveMessageTextView";
	
	private static final int maxWidth = 265;
	
	private static final int widthTime12Hour = 51;
	
	private static final int widthTime24Hour = 33;
	
	private static final int widthMargin = 2;

	private static final int heightTime = 16;

	private static final int minOutMargin = 88;
	
	private Context context;

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
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		try
		{
			int lastLineWidth = 0;
			int linesMaxWidth = 0;
			int lines = 0;
			
			// issue : https://code.google.com/p/android/issues/detail?id=35466
			try
			{
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
			catch (IndexOutOfBoundsException e)
			{
				// Fallback to plain text
				setText(getText().toString());
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
			Layout layout = getLayout();
			lines = layout.getLineCount();
			float lastLine = layout.getLineWidth(lines - 1);
			int viewHeight = 0;

			linesMaxWidth = lastLineWidth = (int) Math.ceil(lastLine);

			//Logger.d(TAG, "lastLine: " + lastLine + ", density multiplier: " + Utils.densityMultiplier);

			for (int n = 0; n < lines; ++n)
			{
				float lineWidth;
				int lineHeight;
				try
				{
					lineWidth = layout.getLineWidth(n);
					lineHeight = (layout.getLineTop(n+1) - layout.getLineTop(n));
					viewHeight += lineHeight;
					//Logger.d(TAG, "LINE NO. " + n + ", Width: " + lineWidth + ", Height: " + lineHeight + ", Height in dp: " + lineHeight/Utils.densityMultiplier);
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
			DisplayMetrics displaymetrics = context.getResources().getDisplayMetrics();
			int height = displaymetrics.heightPixels;
			int width = displaymetrics.widthPixels;
			int widthInDP = (int)(width / Utils.scaledDensityMultiplier);
			int max_width = Math.min((widthInDP - minOutMargin),maxWidth);
			if(linesMaxWidth>= (int) ((widthAddition * Utils.scaledDensityMultiplier) + lastLineWidth))
			{
				int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
				int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
				//Logger.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				parentHeight = viewHeight;
				//Logger.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				this.setMeasuredDimension(linesMaxWidth, parentHeight);
			}
			else if((int) (((widthAddition + widthMargin) * Utils.scaledDensityMultiplier) + lastLineWidth) < (int)(max_width * Utils.scaledDensityMultiplier))
			{
				int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
				int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
				//Logger.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				parentHeight = viewHeight;
				//Logger.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				linesMaxWidth = Math.max(linesMaxWidth, (int) ((widthAddition * Utils.scaledDensityMultiplier) + lastLineWidth));
				this.setMeasuredDimension(linesMaxWidth, parentHeight);
			}
			else
			{
				int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
				int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
				//Logger.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				parentHeight = (int) (viewHeight + (heightAddition * Utils.scaledDensityMultiplier));
				//Logger.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				this.setMeasuredDimension(linesMaxWidth, parentHeight);
			}
		}
		catch (Exception e)
		{
			Logger.d(TAG,"exception: " + e.getStackTrace());
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

	@Override
	public void setGravity(int gravity)
	{
		try
		{
			super.setGravity(gravity);
		}
		catch (IndexOutOfBoundsException e)
		{
			setText(getText().toString());
			super.setGravity(gravity);
		}
	}

	@Override
	public void setText(CharSequence text, BufferType type)
	{
		try
		{
			super.setText(text, type);
		}
		catch (IndexOutOfBoundsException e)
		{
			setText(text.toString());
		}
	}
}
