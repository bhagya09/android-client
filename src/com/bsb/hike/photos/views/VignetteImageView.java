package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;

/**
 * Custom View Class extends ImageView in android
 * 
 * Used in applying vignette (radial color fill) over the image as a seperate layer.
 * 
 * @author akhiltripathi
 */

//NOt being used in Photos V2 due to change in implementation technique of vignette
public class VignetteImageView extends ImageView
{
	

	//private Bitmap vignetteBitmap;

	public void setFilter(FilterType Type)
	{
		//this.filter = Type;
	}

	public VignetteImageView(Context context)
	{
		super(context);
		//this.filter = FilterType.ORIGINAL;
		//radiusRatio = 1f;
	//	isFinal = false;
	}

	public VignetteImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		//this.filter = FilterType.ORIGINAL;
		//radiusRatio = 1f;
		//isFinal = false;
	}

	public VignetteImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		//this.filter = FilterType.ORIGINAL;
		//radiusRatio = 1f;
	//	isFinal = false;
	}

	

	

	/**
	 * 
	 * Draws a vignette on the layer of provided image size respective to the current set filter.
	 * 
	 * @author akhiltripathi
	 * 
	 * @param original
	 */
	public static Bitmap getVignetteforFilter(Bitmap bitmap,FilterType filter,boolean isFinal,boolean draw)
	{

		if (filter == null)
		{
			return null;
		}

		int colors[];

		float stops[], radiusRatio = 1f;
		
		switch (filter)
		{
		case X_PRO_2:
			// Vignette: Stop 1 = #000000 84%, Opacity = 0%; Stop 2 = #232443 120%, Opacity = 100%
			colors = new int[] { 0xFFFFFFFF,0x00FFFFFF, 0x00000000, 0xFF232443 };
			stops = new float[] { 0.0f, 1.10f/ 1.5f,1.12f / 1.5f, 1.0f };
			radiusRatio = 1.5f;
			// makeRadialGradient(1.5f, colors, stops);
			break;
		case E1977:
			colors = new int[] { 0xFFFFFFFF,0x00FFFFFF, 0x00000000, 0xFF232443 };
			stops = new float[] { 0.0f, 1.60f/ 1.85f,1.62f / 1.85f, 1.0f };
			radiusRatio = 1.85f;
			// makeRadialGradient(1.5f, colors, stops);
			break;
		case EARLYBIRD:
			colors = new int[] { 0xFFFFFFFF,0x00FFFFFF, 0x00000000, 0xFF000000 };
			stops = new float[] { 0.0f,1.5f/ 2.65f, 1.55f / 2.65f, 1.0f };
			radiusRatio = 2.65f;
			// makeRadialGradient(1.5f, colors, stops);
			break;
		case RETRO:
		case KELVIN:
			// Vignette: Stop 1 = #000000 74%, Opacity = 0%; Stop 2 = #000000 120%, Opacity = 100%
			colors = new int[] { 0xFFFFFFFF,0x00FFFFFF, 0x00000000, 0xFF000000 };
			stops = new float[] { 0.0f,1.15f/ 1.8f, 1.18f / 1.8f, 1.0f };
			radiusRatio = 1.8f;
			// makeRadialGradient(1.5f, colors, stops);
			break;
		case APOLLO:
			// Vignette Stop 1: #18363f, Position 72%, Opacity 0% Stop 2: #18363f, Position 120%, Opacity 100%
			colors = new int[] { 0xFFFFFFFF,0x00FFFFFF, 0x0018363F, 0xFF18363F };
			stops = new float[] { 0.0f,1.65f/ 2.6f, 1.68f / 2.6f, 1.0f };
			radiusRatio = 2.6f;
			// makeRadialGradient(1.6f, colors, stops);
			break;
		
		case JALEBI:
			// Vignette: Stop 1 = #000000 74%, Opacity = 0%; Stop 2 = #000000 120%, Opacity = 100%
			colors = new int[] { 0x00000000, 0x00000000, 0xFF000000 };
			stops = new float[] { 0.0f, 0.98f / 1.8f, 1.0f };
			radiusRatio = 1.8f;
			// makeRadialGradient(1.5f, colors, stops);
			break;
			
		default:
			colors = null;
			stops = null;
			break;
		}

		if (!isFinal && draw)
		{
			//this.invalidate();
		}
		else
		{
			if(colors!=null && stops!=null)
			{
				makeRadialGradient(radiusRatio, bitmap.getWidth(), bitmap.getHeight(), bitmap, colors, stops);
			}
			isFinal = false;
		}

		return bitmap;
	}

//	@Override
//	protected void onDraw(Canvas canvas)
//	{
//		if (colors != null && stops != null)
//		{
//			width = canvas.getWidth() / 2;
//			height = canvas.getHeight() / 2;
//			
//			float radius = radiusRatio * width ;
//
//			RadialGradient gradient = new RadialGradient(width, height, radius, colors, stops, android.graphics.Shader.TileMode.CLAMP);
//
//			Paint p = new Paint();
//			p.setDither(true);
//			p.setShader(gradient);
//
//			canvas.drawCircle(width, height, (radius), p);
//
//		}
//	}

	private static void makeRadialGradient(float radiusRatio,int width,int height,Bitmap bitmap,int colors[],float stops[])
	{

		float radius = radiusRatio * width / 2;

		RadialGradient gradient = new RadialGradient(width / 2, height / 2, radius, colors, stops, android.graphics.Shader.TileMode.CLAMP);

		Paint p = new Paint();
		p.setDither(true);
		p.setShader(gradient);

		Canvas c = new Canvas(bitmap);

		c.drawCircle(width / 2, height / 2, (radius), p);
	}

}
