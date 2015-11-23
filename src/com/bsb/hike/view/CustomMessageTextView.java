package com.bsb.hike.view;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public abstract class CustomMessageTextView extends CustomFontTextView
{

	public interface DimentionMatrixHolder
	{
		public ViewDimentions getDimentionMatrix();

		public void setDimentionMatrix(ViewDimentions vD);
	}

	public class ViewDimentions
	{
		public int width;

		public int height;

		public ViewDimentions()
		{
			width = 0;
			height = 0;
		}
	}

	private DimentionMatrixHolder mDimentionMatrixHolder;

	private String TAG = "CustomMessageTextView";

	private static final int WidthMargin = 2;

	private static final int HeightTime = 16;

	protected static final int MaxWidth = 265;

	protected Context context;

	public CustomMessageTextView(Context context)
	{
		super(context);
		this.context = context;
	}

	public CustomMessageTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
	}

	public CustomMessageTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		this.context = context;
	}

	public void setDimentionMatrixHolder(DimentionMatrixHolder matrixHolder)
	{
		this.mDimentionMatrixHolder = matrixHolder;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		try
		{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			ViewDimentions viewDimentions = mDimentionMatrixHolder.getDimentionMatrix();

			if ((viewDimentions != null) && (viewDimentions.height > 0 && viewDimentions.width > 0))
			{
				this.setMeasuredDimension(viewDimentions.width, viewDimentions.height);
			}
			else
			{
				Layout layout = getLayout();
				int lines = layout.getLineCount();
				float lastLine = layout.getLineWidth(lines - 1);
				int viewHeight = 0;

				int lastLineWidth = (int) Math.ceil(lastLine);
				int linesMaxWidth = lastLineWidth;

				for (int n = 0; n < lines; ++n)
				{
					float lineWidth = layout.getLineWidth(n);
					int lineHeight = (layout.getLineTop(n + 1) - layout.getLineTop(n));
					viewHeight += lineHeight;
					linesMaxWidth = Math.max(linesMaxWidth, (int) Math.ceil(lineWidth));
				}

				int maxTextWidth = getMaximumTextWidth();
				int heightAddition = getTimeStatusHeight();
				int widthAddition = getTimeStatusWidth();
				int layoutHeight = 0, layoutWidth = 0;

				if ((int) (((widthAddition + WidthMargin) * Utils.scaledDensityMultiplier) + lastLineWidth) < (int) (maxTextWidth * Utils.scaledDensityMultiplier))
				{
					layoutHeight = viewHeight;
					layoutWidth = Math.max(linesMaxWidth, (int) (((widthAddition + 0) * Utils.scaledDensityMultiplier) + lastLineWidth));
				}
				else
				{
					layoutHeight = (int) (viewHeight + (heightAddition * Utils.scaledDensityMultiplier));
					layoutWidth = linesMaxWidth;
				}

				/*In some copy pasted messages, the layoutWidth increases beyond the maximumWidth available and hence the text truncates.*/
				layoutWidth = Math.min(layoutWidth,  MeasureSpec.getSize(widthMeasureSpec));
				this.setMeasuredDimension(layoutWidth, layoutHeight);
				if (lines > 10)
				{
					viewDimentions = new ViewDimentions();
					viewDimentions.width = layoutWidth;
					viewDimentions.height = layoutHeight;
					mDimentionMatrixHolder.setDimentionMatrix(viewDimentions);
				}
			}
		}
		catch (Exception e)
		{
			Logger.d(TAG, "exception: " + e);
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

	private int getTimeStatusHeight()
	{
		return HeightTime;
	}

	private int getTimeStatusWidth()
	{
		if (android.text.format.DateFormat.is24HourFormat(getContext()))
		{
			return getTimeStatusWidth24Hour();
		}
		else
		{
			return getTimeStatusWidth12Hour();
		}
	}

	protected abstract int getMaximumTextWidth();

	protected abstract int getTimeStatusWidth24Hour();

	protected abstract int getTimeStatusWidth12Hour();
}
