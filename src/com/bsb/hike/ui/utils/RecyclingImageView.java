package com.bsb.hike.ui.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

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
		String str = "";
		try
		{
			str = getResources().getResourceEntryName(getId());
		}
		catch(Exception e)
		{
			
		}
		Log.d("RecyclingImageView","onDetachedFromWindow called for : " + str);
		// This has been detached from Window, so clear the drawable
		setImageDrawable(null);

		super.onDetachedFromWindow();
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

