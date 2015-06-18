package com.bsb.hike.ui.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.bsb.hike.BitmapModule.RecyclingBitmapDrawable;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author GK
 * Wrapper class for ImageView.
 * Use this class instead of ImageView for calling recycle() at appropriate times. 
 */
/**
 * Sub-class of ImageView which automatically notifies the drawable when it is being displayed.
 */
public class RecyclingImageView extends ImageView
{

	public RecyclingImageView(Context context)
	{
		super(context);
	}

	public RecyclingImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public RecyclingImageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	/**
	 * @see android.widget.ImageView#onDetachedFromWindow()
	 */
	@Override
	protected void onDetachedFromWindow()
	{
		// This has been detached from Window, so clear the drawable
		setImageDrawable(null);

		super.onDetachedFromWindow();
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		if (!Utils.isHoneycombOrHigher()) // <=2.3
		{
			if (getDrawable() instanceof RecyclingBitmapDrawable)
			{
				if (!((RecyclingBitmapDrawable) getDrawable()).isBitmapValid()) // If bitmap is not valid, dont try to draw on canvas
				{
					setImageDrawable(null);
				}
			}
		}
		super.onDraw(canvas);
	}

	/**
	 * @see android.widget.ImageView#setImageResource(int)
	 */
	@Override
	public void setImageResource(int resId)
	{
		// get previous drawable
		final Drawable previousDrawable = getDrawable();

		super.setImageResource(resId);

		// Notify previous drawable so it is no longer displayed
		notifyDrawable(previousDrawable, false);
	}
	
	/**
	 * @see android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)
	 */
	@Override
	public void setImageDrawable(Drawable drawable)
	{
		// Keep hold of previous Drawable
		final Drawable previousDrawable = getDrawable();

		// Call super to set new Drawable
		super.setImageDrawable(drawable);

		// Notify new Drawable that it is being displayed
		notifyDrawable(drawable, true);

		// Notify old Drawable so it is no longer being displayed
		notifyDrawable(previousDrawable, false);
	}

	/**
	 * Notifies the drawable that it's displayed state has changed.
	 * 
	 * @param drawable
	 * @param isDisplayed
	 */
	private static void notifyDrawable(Drawable drawable, final boolean isDisplayed)
	{
		if (drawable instanceof RecyclingBitmapDrawable)
		{
			// The drawable is a CountingBitmapDrawable, so notify it
			((RecyclingBitmapDrawable) drawable).setIsDisplayed(isDisplayed);
		}
		else if (drawable instanceof LayerDrawable)
		{
			// The drawable is a LayerDrawable, so recurse on each layer
			LayerDrawable layerDrawable = (LayerDrawable) drawable;
			for (int i = 0, z = layerDrawable.getNumberOfLayers(); i < z; i++)
			{
				notifyDrawable(layerDrawable.getDrawable(i), isDisplayed);
			}
		}
	}
}
