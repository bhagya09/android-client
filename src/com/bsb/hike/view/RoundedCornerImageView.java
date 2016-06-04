package com.bsb.hike.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.bsb.hike.R;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * This class is an ImageView implementation to round the top two corners of imageview.
 */
public class RoundedCornerImageView extends RecyclingImageView
{
	private Bitmap maskBitmap;

	private Paint paint, maskPaint;

	private float cornerRadius;

	public RoundedCornerImageView(Context context)
	{
		super(context);
		init(context);
	}

	public RoundedCornerImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}

	public RoundedCornerImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context)
	{
		cornerRadius = (int)context.getResources().getDimension(R.dimen.native_cardview_radius);
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
		maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		setWillNotDraw(false);
	}

	@Override
	public void draw(Canvas canvas)
	{
		Bitmap offscreenBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas offscreenCanvas = new Canvas(offscreenBitmap);

		super.draw(offscreenCanvas);

		if (maskBitmap == null)
		{
			maskBitmap = createMask(canvas.getWidth(), canvas.getHeight());
		}

		offscreenCanvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint);
		canvas.drawBitmap(offscreenBitmap, 0f, 0f, paint);
	}

	private Bitmap createMask(int width, int height)
	{
		Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
		Canvas canvas = new Canvas(mask);

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.WHITE);

		canvas.drawRect(0, 0, width, height, paint);

		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		canvas.drawRoundRect(new RectF(0, 0, width, height), cornerRadius, cornerRadius, paint);
		// To remove arc from bottom left corner
		canvas.drawRect(0, height / 2, width / 2, height, paint);
		// To remove arc from bottom right corner
		canvas.drawRect(width / 2, height / 2, width, height, paint);
		return mask;
	}
}
