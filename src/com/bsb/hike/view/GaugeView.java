package com.bsb.hike.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.bsb.hike.utils.Utils;

public class GaugeView extends ImageView
{
	private Paint gaugeBackgroundPaint;

	private Paint maxCreditsPaint;

	private int maxCreditsAngle;

	private static final int START_ANGLE = 200;

	private static final int END_ANGLE = 154;

	private static final int MAX_CREDITS_TO_SHOW = 600;

	private static final int MAX_ANGLE = 141;

	private static final int OFFSET = 7;

	private RectF gaugeArcArea;

	public GaugeView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		DisplayMetrics metrics = new DisplayMetrics();
		((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);

		gaugeBackgroundPaint = new Paint();
		gaugeBackgroundPaint.setColor(0xffb2d989);
		gaugeBackgroundPaint.setStyle(Paint.Style.STROKE);
		gaugeBackgroundPaint.setStrokeWidth(56f * Utils.scaledDensityMultiplier);
		gaugeBackgroundPaint.setAntiAlias(true);

		maxCreditsPaint = new Paint();
		maxCreditsPaint.setColor(0xff90c86b);
		maxCreditsPaint.setStyle(Paint.Style.STROKE);
		maxCreditsPaint.setStrokeWidth(56f * Utils.scaledDensityMultiplier);
		maxCreditsPaint.setAntiAlias(true);

		gaugeArcArea = new RectF((int) (24 * Utils.scaledDensityMultiplier), (int) (29 * Utils.scaledDensityMultiplier), (int) (214 * Utils.scaledDensityMultiplier),
				(int) (224 * Utils.scaledDensityMultiplier));

	}

	public GaugeView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public void setMaxCreditsAngle(int credits)
	{
		this.maxCreditsAngle = creditsToAngle(credits);
		invalidate();
	}

	public GaugeView(Context context)
	{
		this(context, null, 0);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		canvas.drawArc(gaugeArcArea, START_ANGLE - OFFSET, END_ANGLE, false, gaugeBackgroundPaint);
		// Only show the arc if we have the credits per month is larger than
		// zero
		if (maxCreditsAngle > 0)
		{
			canvas.drawArc(gaugeArcArea, START_ANGLE - OFFSET, maxCreditsAngle + OFFSET, false, maxCreditsPaint);
		}
		super.onDraw(canvas);
	}

	private int creditsToAngle(int credits)
	{
		if (credits == MAX_CREDITS_TO_SHOW)
		{
			return MAX_ANGLE;
		}
		else if (credits > MAX_CREDITS_TO_SHOW)
		{
			return END_ANGLE;
		}
		float ratio = (credits * 100) / MAX_CREDITS_TO_SHOW;
		int angle = (int) (ratio * MAX_ANGLE) / 100;
		return angle;
	}
}