package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.R;
import com.bsb.hike.adapters.EmoticonAdapter;
import com.bsb.hike.analytics.HAManager;
import com.viewpagerindicator.IconPageIndicator;

/**
 * Created by anubhavgupta on 06/11/15.
 */
public class EmoticonIconPageIndicator extends IconPageIndicator
{

	public EmoticonIconPageIndicator(Context context)
	{
		this(context, null);
	}

	public EmoticonIconPageIndicator(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	public void notifyDataSetChanged()
	{
		mIconsLayout.removeAllViews();
		EmoticonAdapter iconAdapter = (EmoticonAdapter) mViewPager.getAdapter();

		LayoutInflater inflater = LayoutInflater.from(getContext());

		int count = iconAdapter.getCount();

		for (int i = 0; i < count; i++)
		{
			View stickerParent = inflater.inflate(R.layout.sticker_btn, mIconsLayout, false);
			ImageView icon = (ImageView) stickerParent.findViewById(R.id.category_btn);
			icon.setImageResource(iconAdapter.getIconResId(i));

			stickerParent.setTag(i);
			stickerParent.setOnClickListener(mTabClickListener);

			mIconsLayout.addView(stickerParent);
		}
		if (mSelectedIndex > count)
		{
			mSelectedIndex = count - 1;
		}

		if (mSelectedIndex < 0)
		{
			HAManager.sendStickerCrashDevEvent("Current Selected index inside : notifyDataSetChanged is : " + mSelectedIndex);
		}
		setCurrentItem(mSelectedIndex);
		requestLayout();
	}

	private final OnClickListener mTabClickListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			Integer currentIndex = (Integer) view.getTag();
			final int newSelected = currentIndex;
			setCurrentItem(newSelected);
		}
	};
}