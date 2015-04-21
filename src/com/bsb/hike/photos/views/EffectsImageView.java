package com.bsb.hike.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.Toast;

import com.bsb.hike.R;
import com.bsb.hike.photos.HikeEffectsFactory;
import com.bsb.hike.photos.HikeEffectsFactory.OnFilterAppliedListener;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;
import com.bsb.hike.utils.IntentFactory;

/**
 * Custom View Class extends ImageView in android
 * 
 * An object of EffectsImageView represents a layer on which the effects filters will be applied
 * 
 * @author akhiltripathi
 *
 */
public class EffectsImageView extends ImageView
{

	private Bitmap originalImage, currentImage;

	private FilterType currentFilter;

	private boolean isScaled;

	public EffectsImageView(Context context)
	{
		super(context);
		currentFilter = FilterType.ORIGINAL;

	}

	public EffectsImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		currentFilter = FilterType.ORIGINAL;
	}

	public EffectsImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		currentFilter = FilterType.ORIGINAL;
	}

	public void getBitmapWithEffectsApplied(Bitmap bitmap, OnFilterAppliedListener listener)
	{
		if (!isScaled && currentFilter == FilterType.ORIGINAL)
		{
			listener.onFilterApplied(HikePhotosUtils.createBitmap(bitmap, 0, 0, 0, 0, true, false, false, true));
			// listener.onFilterApplied(bitmap.copy(bitmap.getConfig(), true));
		}
		else if (!isScaled)
		{
			listener.onFilterApplied(currentImage);
		}
		else
		{
			if (!HikeEffectsFactory.applyFilterToBitmap(bitmap, listener, currentFilter, true))
			{
				Toast.makeText(getContext(), getResources().getString(R.string.photos_oom_save), Toast.LENGTH_SHORT).show();
				IntentFactory.openHomeActivity(getContext(), true);

			}
		}

	}

	public void handleImage(Bitmap image, boolean hasBeenScaled)
	{
		isScaled = hasBeenScaled;
		originalImage = image;
		this.setImageBitmap(image);

	}

	public void changeDisplayImage(Bitmap image)
	{
		this.setImageBitmap(image);
		HikeEffectsFactory.clearCache();
		currentImage = image;
	}

	public void applyEffect(FilterType filter, float value, OnFilterAppliedListener listener)
	{
		currentFilter = filter;
		if (!HikeEffectsFactory.applyFilterToBitmap(originalImage, listener, filter, false))
		{
			Toast.makeText(getContext(),getResources().getString(R.string.photos_oom_load), Toast.LENGTH_SHORT).show();
			IntentFactory.openHomeActivity(getContext(), true);

		}
	}

	public FilterType getCurrentFilter()
	{
		return currentFilter;
	}

}
