package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;

/**
 * @author akhiltripathi
 *
 *         Custom View Class extends ImageView in android
 * 
 *         Used in applying vignette (radial color fill) over the image as a seperate layer.
 *
 */

class VignetteImageView extends ImageView
{
	int width;

	Bitmap vignetteBitmap;
	
	FilterType filter;
	
	public void setFilter(FilterType Type)
	{
		this.filter = Type;
	}

	public VignetteImageView(Context context)
	{
		super(context);
	}

	public VignetteImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public VignetteImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	public void setVignetteforFilter(Bitmap original)
	{
		width = original.getWidth();

		int colors[];
		float stops[];
		vignetteBitmap = Bitmap.createBitmap(width, width, Config.ARGB_8888);
		switch (filter)
		{
		case X_PRO_2:
			//Vignette: Stop 1 = #000000 84%, Opacity = 0%; Stop 2 = #232443 120%, Opacity = 100%
			colors = new int[]{0x00000000,0xAA000000,0xFF232443};
			stops = new float[]{0.0f,0.84f/1.2f,1.0f};
			makeRadialGradient(1.2f, colors, stops);
			break;
		case RETRO:
		case KELVIN:
		case EARLYBIRD:
			//Vignette: Stop 1 = #000000 74%, Opacity = 0%; Stop 2 = #000000 120%, Opacity = 100%
			colors = new int[]{0x00000000,0xFF000000};
			stops = new float[]{0.0f,1.0f};
			makeRadialGradient(1.2f, colors, stops);
			break;
		case APOLLO:
			//Vignette Stop 1: #18363f, Position 72%, Opacity 0% Stop 2: #18363f, Position 120%, Opacity 100%
			colors = new int[]{0x00000000,0x1118363F,0xFF18363F};
			stops = new float[]{0.0f,0.72f/1.2f,1.0f};
			makeRadialGradient(1.2f, colors, stops);
			break;
				
		}
		this.setImageBitmap(vignetteBitmap);
	}

	private void makeRadialGradient(float radiusRatio, int[] colors, float[] stops)
	{

		float radius = radiusRatio * width;

		RadialGradient gradient = new RadialGradient(width / 2, width / 2, radius, colors, stops, android.graphics.Shader.TileMode.CLAMP);

		Paint p = new Paint();
		p.setDither(true);
		p.setShader(gradient);

		Canvas c = new Canvas(vignetteBitmap);

		c.drawCircle(width / 2, width / 2, (float) (radius), p);
	}

}
