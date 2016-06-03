package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;

import com.bsb.hike.ui.utils.RecyclingImageView;

/**
 * Created by varunarora on 24/05/16. This class is an ImageView implementation to expand the view dimensions equal to its parent used for Hike Daily backround image.
 */
public class HikeDailyCustomImageView extends RoundedCornerImageView
{
	public HikeDailyCustomImageView(Context context)
	{
		super(context);
	}

	public HikeDailyCustomImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public HikeDailyCustomImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
		this.setMeasuredDimension(parentWidth, parentHeight);
	}

}
