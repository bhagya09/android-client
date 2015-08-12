package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.util.AttributeSet;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.photos.HikePhotosUtils;

/**
 * Custom View for the doodle thumbnails
 * 
 * @author akhiltripathi
 *
 */
public class DoodleEffectItemLinearLayout extends EffectItemLinearLayout
{
	private int brushWidth;

	private int brushColor;

	private int ringColor;

	public DoodleEffectItemLinearLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		// TODO Auto-generated constructor stub
		brushWidth = (int) getResources().getDimension(R.dimen.preview_brush_width);
		brushColor = HikeConstants.HikePhotos.DEFAULT_BRUSH_COLOR;
		ringColor = HikeConstants.HikePhotos.DEFAULT_RING_COLOR;
		setImage(getCircleIcon());
	}

	public void refresh()
	{

		setImage(getCircleIcon());
		invalidate();
	}

	public int getBrushColor()
	{
		return brushColor;
	}

	public void setBrushColor(int Color)
	{
		this.brushColor = Color;
	}

	public void setBrushColor(int Color, boolean setCurrentItem)
	{
		this.brushColor = Color;
		if (Color == HikePhotosUtils.FilterTools.getSelectedColor())
		{
			ringColor = HikeConstants.HikePhotos.SELECTED_RING_COLOR;
			if (setCurrentItem)
				HikePhotosUtils.FilterTools.setCurrentDoodleItem(this);
		}
		else
		{
			ringColor = HikeConstants.HikePhotos.DEFAULT_RING_COLOR;
		}
	}

	public void select()
	{
		HikePhotosUtils.FilterTools.setSelectedColor(this.brushColor);
		ringColor = HikeConstants.HikePhotos.SELECTED_RING_COLOR;
		HikePhotosUtils.FilterTools.setCurrentDoodleItem(this);
		refresh();
	}

	public void unSelect()
	{
		ringColor = HikeConstants.HikePhotos.DEFAULT_RING_COLOR;
		refresh();
	}

	public int getRingColor()
	{
		return ringColor;
	}

	public void setRingColor(int Color)
	{
		this.ringColor = Color;
	}

	private Bitmap getCircleIcon()
	{
		int diameter = brushWidth + HikePhotosUtils.dpToPx(4);
		Bitmap bitmap = HikePhotosUtils.createBitmap(null, 0, 0, diameter, diameter, false, false, false, true,Config.ARGB_8888);
		// Bitmap bitmap = Bitmap.createBitmap(diameter, diameter, Config.ARGB_8888);
		if (bitmap != null)
		{
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(ringColor);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawCircle(diameter / 2, diameter / 2, (diameter / 2), paint);
			paint.setColor(brushColor);
			canvas.drawCircle(diameter / 2, diameter / 2, (brushWidth / 2), paint);
			RadialGradient gradient = new RadialGradient(diameter / 2, diameter / 2, (brushWidth + 1) / 2, new int[] { 0x00000000, 0x00000000, 0x55000000 }, new float[] { 0,
					0.86f, 1 }, android.graphics.Shader.TileMode.CLAMP);
			paint = new Paint();
			paint.setDither(true);
			paint.setShader(gradient);
			int delta = ringColor == HikeConstants.HikePhotos.SELECTED_RING_COLOR ? 1 : 2;
			canvas.drawCircle((diameter) / 2, diameter / 2, (brushWidth + delta) / 2, paint);
		}

		return bitmap;
	}

	public void setBrushWidth(int brushWidth)
	{
		this.brushWidth = brushWidth;
	}

	public int getBrushWidth()
	{
		return brushWidth;
	}

}