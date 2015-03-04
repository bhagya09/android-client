package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.R;
import com.bsb.hike.adapters.EmoticonAdapter;
import com.bsb.hike.adapters.StickerAdapter;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.utils.StickerManager;
import com.viewpagerindicator.IconPageIndicator;
import com.viewpagerindicator.IconPagerAdapter;

public class StickerEmoticonIconPageIndicator extends IconPageIndicator
{

	StickerOtherIconLoader stickerOtherIconLoader;

	public StickerEmoticonIconPageIndicator(Context context)
	{
		this(context, null);
		this.stickerOtherIconLoader = new StickerOtherIconLoader(context, true);
	}

	public StickerEmoticonIconPageIndicator(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		this.stickerOtherIconLoader = new StickerOtherIconLoader(context, true);
	}

	/*
	 * TODO : This function is called twice, it should be handled properly so that it should run just once. Also inorder to remove the red icon once stickers gets downloaded, we
	 * should handle it properly instead of calling "notifyDataSetChanged" this again and again.
	 */

	@Override
	public void notifyDataSetChanged()
	{
		mIconsLayout.removeAllViews();
		StickerEmoticonIconPagerAdapter iconAdapter = (StickerEmoticonIconPagerAdapter) mViewPager.getAdapter();

		LayoutInflater inflater = LayoutInflater.from(getContext());

		int count = iconAdapter.getCount();

		for (int i = 0; i < count; i++)
		{
			View stickerParent = inflater.inflate(R.layout.sticker_btn, mIconsLayout, false);
			ImageView icon = (ImageView) stickerParent.findViewById(R.id.category_btn);
			ImageView updateAvailable = (ImageView) stickerParent.findViewById(R.id.update_available);
			if (iconAdapter instanceof EmoticonAdapter)
			{
				icon.setImageResource(iconAdapter.getIconResId(i));
			}
			else if(iconAdapter instanceof StickerAdapter)
			{
				//We run this on background thread to make opening of pallate fast
				StickerCategory stickerCategory = iconAdapter.getCategoryForIndex(i);
				loadImage(stickerCategory.getCategoryId(), false, icon, false);
				updateAvailable.setVisibility(iconAdapter.isUpdateAvailable(i) ? View.VISIBLE : View.GONE);
				if(stickerCategory.getState() == StickerCategory.DONE_SHOP_SETTINGS)
				{
					updateAvailable.setVisibility(View.VISIBLE);
					updateAvailable.setImageResource(R.drawable.ic_done_pallete_2);
				}
			}

			stickerParent.setTag(i);
			stickerParent.setOnClickListener(mTabClickListener);

			mIconsLayout.addView(stickerParent);
		}
		if (mSelectedIndex > count)
		{
			mSelectedIndex = count - 1;
		}
		setCurrentItem(mSelectedIndex);
		requestLayout();
	}

	public interface StickerEmoticonIconPagerAdapter extends IconPagerAdapter
	{
		boolean isUpdateAvailable(int index);
		StickerCategory getCategoryForIndex(int index);
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
	
	@Override
    public void setCurrentItem(int item) {
		int previousSelectedIndex = mSelectedIndex;
		super.setCurrentItem(item);
        if (mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        StickerEmoticonIconPagerAdapter iconAdapter = (StickerEmoticonIconPagerAdapter) mViewPager.getAdapter();;
        if(!(iconAdapter instanceof StickerAdapter))
        {
        	//If it is not stickerAdapter we don't need to do anything
        	return;
        }
        int count = iconAdapter.getCount();
        item = item < count ? item : count - 1;
        /*
         * might be the case that list items decrease. and we call notifyDataSetChanged
         */
        if(previousSelectedIndex < count)
		{
        	//deSelecting old child
			selectChild(iconAdapter, previousSelectedIndex, false);
		}
        //selecting new child
        selectChild(iconAdapter, item, true);
    }
	
	private void selectChild(StickerEmoticonIconPagerAdapter iconAdapter, int index, boolean isSelected)
	{
		View child = mIconsLayout.getChildAt(index);
		ImageView icon = (ImageView) child.findViewById(R.id.category_btn);
		child.setSelected(isSelected);
		//We run this on UI thread otherwise there is a visible lag
		loadImage(iconAdapter.getCategoryForIndex(index).getCategoryId(), isSelected, icon, true);
	}

	private void loadImage(String catId, boolean isSelected,  ImageView imageView, boolean runOnUiThread )
	{
		int assetType = isSelected ? StickerManager.PALLATE_ICON_SELECTED_TYPE : StickerManager.PALLATE_ICON_TYPE;
        stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(catId, assetType), imageView, false, runOnUiThread);
	}

}
