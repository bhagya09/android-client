package com.bsb.hike.photos.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;

/**
 * Custom View Class extends ImageView in android
 * 
 * Used in applying vignette (radial color fill) over the image as a seperate layer.
 * 
 * @author akhiltripathi
 */

// NOt being used in Photos V2 due to change in implementation technique of vignette
public class VignetteUtils 
{

	// private Bitmap vignetteBitmap;

	
	/**
	 * 
	 * Draws a vignette on the layer of provided image size respective to the current set filter.
	 * 
	 * @author akhiltripathi
	 * 
	 * @param original
	 */
	public static Bitmap getVignetteforFilter(Bitmap bitmap, FilterType filter, boolean isFinal, boolean draw)
	{

		if (filter == null)
		{
			return null;
		}
		
		bitmap.eraseColor(0x00000000);

		int colors[], cX = bitmap.getWidth() / 2, cY = bitmap.getHeight() / 2, sX = 0, sY = 0, eX = bitmap.getWidth(), eY = bitmap.getHeight();

		boolean radialGradient = true, isLinearDiagonal = false;

		float stops[], radius = 1f;

		radius = bitmap.getWidth()>bitmap.getHeight()?bitmap.getHeight():bitmap.getWidth();
		
		switch (filter)
		{
		case X_PRO_2:
			// Vignette: Stop 1 = #000000 84%, Opacity = 0%; Stop 2 = #232443 120%, Opacity = 100%
			colors = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x00000000, 0xFF232443 };
			stops = new float[] { 0.0f, 1.45f / 1.8f, 1.47f / 1.8f, 1.0f };
			radius = 1.8f * radius / 2;
			break;
		case E1977:
			colors = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x00000000, 0xFF232443 };
			stops = new float[] { 0.0f, 1.60f / 1.85f, 1.62f / 1.85f, 1.0f };
			radius = 1.85f * radius / 2;
			break;
		case EARLYBIRD:
			colors = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x004B2B1E, 0xFF4B2B1E };
			stops = new float[] { 0.0f, 1.67f / 2.65f, 1.7f / 2.65f, 1.0f };
			radius = 2.65f * radius / 2;
			break;
		case GHOSTLY:
		case BGR:
			colors = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x00000000, 0xFF000000 };
			stops = new float[] { 0.0f, 1.67f / 2.65f, 1.7f / 2.65f, 1.0f };
			radius = 2.65f * radius / 2;
			break;
		case BRANNAN:
			colors = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x00A9AA98, 0xFFA9AA98 };
			stops = new float[] { 0.0f, 1.67f / 2.65f, 1.7f / 2.65f, 1.0f };
			radius = 2.65f * radius / 2;
			break;
		case RETRO:
		case KELVIN:
			// Vignette: Stop 1 = #000000 74%, Opacity = 0%; Stop 2 = #000000 120%, Opacity = 100%
			colors = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x00000000, 0xFF000000 };
			stops = new float[] { 0.0f, 1.45f / 1.8f, 1.48f / 1.8f, 1.0f };
			radius = 1.8f * radius / 2;
			break;
		case APOLLO:
			// Vignette Stop 1: #18363f, Position 72%, Opacity 0% Stop 2: #18363f, Position 120%, Opacity 100%
			colors = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x0018363F, 0xFF18363F };
			stops = new float[] { 0.0f, 1.65f / 2.6f, 1.68f / 2.6f, 1.0f };
			radius = 2.6f * radius / 2;
			break;
		case CHILLUM:
			colors = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x00000000, 0xFF000000 };
			stops = new float[] { 0.0f, 1.85f / 2.65f, 1.90f / 2.65f, 1.0f };
			radius = 2.65f * radius / 2;
			break;
		case JALEBI:
			// Vignette: Stop 1 = #000000 74%, Opacity = 0%; Stop 2 = #000000 120%, Opacity = 100%
			colors = new int[] { 0x00000000, 0x00000000, 0xFF000000 };
			stops = new float[] { 0.0f, 0.98f / 1.8f, 1.0f };
			radius = 1.8f * radius / 2;
			break;
		case LO_FI:
			// Vignette Stop 1: #18363f, Position 72%, Opacity 0% Stop 2: #18363f, Position 120%, Opacity 100%
			colors = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x00525252, 0xFF525252 };
			stops = new float[] { 0.0f, 1.65f / 2.6f, 1.68f / 2.6f, 1.0f };
			radius = 2.6f * radius / 2;
			break;
		case GULAAL:
			// Gradient: Linear - Start From Right Side #ff0000 (opacity: 86% to 0%) (Scale: 150%)
			colors = new int[] { 0x00FF0000, 0x88FF0000 };
			stops = new float[] { 0.0f, 1.0f };
			radius = 1f * radius;
			radialGradient = false;
			break;

		case SUNLITT:
			isLinearDiagonal = false;
			colors = new int[] { 0xFF290A59, 0xFFFF7C00 };
			stops = new float[] { 0.0f, 1.0f };
			radius = 1.76f * radius;
			makeLinearGradient(radius, bitmap.getWidth(), bitmap.getHeight(), bitmap, colors, stops, sX, sY, eX, eY, isLinearDiagonal);
			radialGradient = false;
			isLinearDiagonal = true;
			colors = new int[] { 0x00FF0000,0x55FF0000,0x55FF0000, 0x00FF0000 };
			stops = new float[] { 0.0f,0.15f, 0.42f, 1.0f };
			radius = bitmap.getWidth()>bitmap.getHeight()?bitmap.getHeight():bitmap.getWidth();
			radius = 1f * bitmap.getWidth();
			sX =eX;
			eX=0;
			break;
		default:
			colors = null;
			stops = null;
			break;
		}

		if (!isFinal && draw)
		{
			// this.invalidate();
		}
		else
		{
			if (colors != null && stops != null)
			{
				if (radialGradient)
				{
					makeRadialGradient(radius, bitmap.getWidth(), bitmap.getHeight(), bitmap, colors, stops, cX, cY);
				}
				else
				{
					makeLinearGradient(radius, bitmap.getWidth(), bitmap.getHeight(), bitmap, colors, stops, sX, sY, eX, eY, isLinearDiagonal);
				}

			}
			isFinal = false;
		}

		return bitmap;
	}

	private static void makeRadialGradient(float radius, int width, int height, Bitmap bitmap, int colors[], float stops[], float centerX, float centerY)
	{

		RadialGradient gradient = new RadialGradient(centerX, centerY, radius, colors, stops, TileMode.CLAMP);

		Paint p = new Paint();
		p.setDither(true);
		p.setShader(gradient);

		Canvas c = new Canvas(bitmap);

		c.drawCircle(centerX, centerY, (radius), p);
	}

	private static void makeLinearGradient(float radius, int width, int height, Bitmap bitmap, int colors[], float stops[], float startX, float startY, float endX, float endY,
			boolean isDiagonal)
	{
		float gradientEnd = isDiagonal ? endY : startY;
		LinearGradient shader = new LinearGradient(startX, startY, endX, gradientEnd, colors, stops, TileMode.CLAMP);
		Paint paint = new Paint();
		paint.setDither(true);
		paint.setShader(shader);
		Canvas c = new Canvas(bitmap);
		c.drawRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), paint);

	}

}
