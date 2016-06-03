package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.R;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatHead.TabClickListener;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.utils.StickerManager;
import com.viewpagerindicator.IconPageIndicator;
import com.viewpagerindicator.IconPagerAdapter;

/**
 * Created by anubhavgupta on 05/11/15.
 */
public class StickerIconPageIndicator extends IconPageIndicator
{

	StickerOtherIconLoader stickerOtherIconLoader;

	static TabClickListener tabClickListener;

	String selectedCatId;

	ImageView selectedImageView;

	public StickerIconPageIndicator(Context context)
	{
		this(context, null);
		this.stickerOtherIconLoader = new StickerOtherIconLoader(context, true);
	}

	public StickerIconPageIndicator(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		this.stickerOtherIconLoader = new StickerOtherIconLoader(context, true);
	}

	public static void registerChatHeadTabClickListener(TabClickListener tabListener)
	{
		tabClickListener = tabListener;
	}

	public static void unRegisterChatHeadTabClickListener()
	{
		tabClickListener = null;
	}

	/*
	 * TODO : This function is called twice, it should be handled properly so that it should run just once. Also inorder to remove the red icon once stickers gets downloaded, we
	 * should handle it properly instead of calling "notifyDataSetChanged" this again and again.
	 */

	@Override
	public void notifyDataSetChanged()
	{
		mIconsLayout.removeAllViews();
		StickerIconPagerAdapter iconAdapter = (StickerIconPagerAdapter) mViewPager.getAdapter();

		LayoutInflater inflater = LayoutInflater.from(getContext());

		int count = iconAdapter.getCount();

		for (int i = 0; i < count; i++)
		{
			View stickerParent = inflater.inflate(R.layout.sticker_btn, mIconsLayout, false);
			ImageView icon = (ImageView) stickerParent.findViewById(R.id.category_btn);
			ImageView updateAvailable = (ImageView) stickerParent.findViewById(R.id.update_available);

			// We run this on background thread to make opening of palate fast
			StickerCategory stickerCategory = iconAdapter.getCategoryForIndex(i);
			loadImage(stickerCategory.getCategoryId(), false, icon, false);
			updateAvailable.setVisibility(iconAdapter.isUpdateAvailable(i) ? View.VISIBLE : View.GONE);
			if (stickerCategory.getState() == StickerCategory.DONE_SHOP_SETTINGS)
			{
				updateAvailable.setVisibility(View.VISIBLE);
				updateAvailable.setImageResource(R.drawable.ic_done_pallete_2);
			}

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

	public interface StickerIconPagerAdapter extends IconPagerAdapter
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
			if (tabClickListener != null)
			{
				tabClickListener.onTabClick(newSelected);
			}
			setCurrentItem(newSelected);
		}
	};

	@Override
	public void setCurrentItem(int item)
	{
		int previousSelectedIndex = mSelectedIndex;
		super.setCurrentItem(item);
		if (mViewPager == null)
		{
			throw new IllegalStateException("ViewPager has not been bound.");
		}
		StickerIconPagerAdapter iconAdapter = (StickerIconPagerAdapter) mViewPager.getAdapter();

		int count = iconAdapter.getCount();
		item = item < count ? item : count - 1;
		/*
		 * might be the case that list items decrease. and we call notifyDataSetChanged
		 */
		if (previousSelectedIndex < count)
		{
			// deSelecting old child
			selectChild(iconAdapter, previousSelectedIndex, false);
		}

		if (count <= 0)
		{
			HAManager.sendStickerCrashDevEvent("Inside method : setCurrentItem. Getting count as 0 from IconAdapter. Expect a crash soon!");
		}
		// selecting new child
		selectChild(iconAdapter, item, true);
	}

	private void selectChild(StickerIconPagerAdapter iconAdapter, int index, boolean isSelected)
	{
		View child = mIconsLayout.getChildAt(index);
		if (child == null)
		{
			String errorMsg = "Inside method : Select Child.  View is null. Index value specified : " + index;
			HAManager.sendStickerCrashDevEvent(errorMsg);
			return;
		}
		ImageView icon = (ImageView) child.findViewById(R.id.category_btn);
		child.setSelected(isSelected);
		// We run this on UI thread otherwise there is a visible lag
		loadImage(iconAdapter.getCategoryForIndex(index).getCategoryId(), isSelected, icon, true);
	}

	private void loadImage(String catId, boolean isSelected, ImageView imageView, boolean runOnUiThread)
	{
		int assetType = isSelected ? StickerManager.PALLATE_ICON_SELECTED_TYPE : StickerManager.PALLATE_ICON_TYPE;
		stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(catId, assetType), imageView, false, runOnUiThread);
		if (isSelected)
		{
			selectedCatId = catId;
			selectedImageView = imageView;
		}
	}

	public void unselectCurrent()
	{
		if (selectedCatId != null && selectedImageView != null)
		{
			loadImage(selectedCatId, false, selectedImageView, true);
		}

	}
}