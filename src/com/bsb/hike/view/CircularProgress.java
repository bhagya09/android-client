package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.bsb.hike.utils.Utils;

public class CircularProgress extends View
{
	private RectF progressContainer;

	private Paint outerBoundary;

	private Paint actualProgress;

	private int progressAngle;

	public CircularProgress(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		progressContainer = new RectF(3 * Utils.scaledDensityMultiplier, 3 * Utils.scaledDensityMultiplier, 18 * Utils.scaledDensityMultiplier, 18 * Utils.scaledDensityMultiplier);

		outerBoundary = new Paint();
		outerBoundary.setColor(0xFFAAAAAA);
		outerBoundary.setAntiAlias(true);
		outerBoundary.setStrokeWidth(1.5f * Utils.scaledDensityMultiplier);
		outerBoundary.setStyle(Paint.Style.STROKE);

		actualProgress = new Paint();
		actualProgress.setColor(0xFFAAAAAA);
		actualProgress.setAntiAlias(true);
	}

	public void setProgressColor(int color)
	{
		outerBoundary.setColor(color);
		actualProgress.setColor(color);

		invalidate();
	}

	public CircularProgress(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public CircularProgress(Context context)
	{
		this(context, null, 0);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		canvas.drawArc(progressContainer, 0, 360, true, outerBoundary);
		canvas.drawArc(progressContainer, 270, progressAngle, true, actualProgress);
	}

	public void setProgressAngle(int progress)
	{
		progressAngle = (int) ((360 * progress) / 100);
	}
}