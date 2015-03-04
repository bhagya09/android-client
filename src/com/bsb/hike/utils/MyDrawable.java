package com.bsb.hike.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;

import com.bsb.hike.R;

public class MyDrawable extends ShapeDrawable
{
	private String text;

	private Paint paint;

	private Context context;

	private boolean onHike;

	public MyDrawable(String text, Context context, boolean onHike)
	{
		this.text = text;
		this.context = context;
		this.onHike = onHike;

		paint = new Paint();
		paint.setColor(context.getResources().getColor(onHike ? R.color.contact_blue : R.color.contact_green));
		paint.setTextSize(13.5f * Utils.scaledDensityMultiplier);
		paint.setAntiAlias(true);
		paint.setDither(true);

		getPaint().set(paint);
	}

	@Override
	public void draw(Canvas canvas)
	{
		Drawable d = context.getResources().getDrawable(onHike ? R.drawable.hike_contact_bg : R.drawable.sms_contact_bg);

		int left = (int) (0 * Utils.scaledDensityMultiplier);
		int top = (int) (1.5 * Utils.scaledDensityMultiplier);
		int right = (int) (paint.measureText(text) + ((int) (14 * Utils.scaledDensityMultiplier)));
		int bottom = (int) (25 * Utils.scaledDensityMultiplier);

		d.setBounds(left, top, right, bottom);
		d.draw(canvas);

		canvas.drawText(text, 7 * Utils.scaledDensityMultiplier, 18 * Utils.scaledDensityMultiplier, paint);
	}
}