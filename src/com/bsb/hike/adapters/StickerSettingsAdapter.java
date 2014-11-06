package com.bsb.hike.adapters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.DragSortListView.DragSortListView;
import com.bsb.hike.DragSortListView.DragSortListView.DragSortListener;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.smartImageLoader.StickerPreviewLoader;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;

public class StickerSettingsAdapter extends BaseAdapter implements DragSortListener, OnClickListener
{
	/**
	 * Index is ListView position, value is ArrayList position ( which is to be interpreted as stickerCategoryIndex - 1 )
	 */
	private int[] mListMapping;

	private List<StickerCategory> stickerCategories;

	private Context mContext;

	private LayoutInflater mInflater;

	private boolean isListFlinging;

	private Set<StickerCategory> stickerSet = new HashSet<StickerCategory>();  //Stores the categories which have been reordered
	
	private int lastVisibleIndex = 0;   //gives the index of last visible category in the stickerCategoriesList
	
	private StickerPreviewLoader stickerPreviewLoader;

	public StickerSettingsAdapter(Context context, List<StickerCategory> stickerCategories)
	{
		this.mContext = context;
		this.stickerCategories = stickerCategories;
		this.mInflater = LayoutInflater.from(mContext);
		mListMapping = new int[stickerCategories.size()];
		this.stickerPreviewLoader = new StickerPreviewLoader(context, false);
		initialiseMapping(mListMapping, stickerCategories);
		
	}

	/**
	 * Initialising the initial array mapping as well as we set the category index of those categories for which the indexes are != (position in arraylist + 1), i.e. the categories are randomly ordered. 
	 * This is a one time overhead to ensure that next time user comes on this screen, we are able to show visible and invisible categories based on their appropriate order without any extra overhead.
	 * @param mListMapping
	 * @param stickerCategoryList
	 */
	private void initialiseMapping(int[] mListMapping, List<StickerCategory> stickerCategoryList)
	{
		for(int i=0; i< stickerCategoryList.size(); i++)
		{
			StickerCategory category = stickerCategoryList.get(i);
			mListMapping[i] = i+1;
			if(category.getCategoryIndex() != (mListMapping[i]))
			{
				category.setCategoryIndex(mListMapping[i]);
				stickerSet.add(category);
			}
			if(category.isVisible())
			{
				lastVisibleIndex = i;
			}
		}
	}

	@Override
	public int getCount()
	{
		if (stickerCategories != null)
		{
			return stickerCategories.size();
		}
		return 0;
	}

	@Override
	public StickerCategory getItem(int position)
	{
		return stickerCategories.get(mListMapping[position] - 1);
	}

	@Override
	public long getItemId(int position)
	{
		return mListMapping[position] - 1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		final StickerCategory category = getItem(position);
		ViewHolder viewHolder;
		
		if(convertView == null)
		{
			convertView = mInflater.inflate(R.layout.sticker_settings_list_item, null);
			viewHolder = new ViewHolder();
			viewHolder.categoryName = (TextView) convertView.findViewById(R.id.category_name);
			viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.category_checkbox);
			viewHolder.categoryPreviewImage = (ImageView) convertView.findViewById(R.id.category_icon);
			viewHolder.categorySize = (TextView) convertView.findViewById(R.id.category_size);
			viewHolder.updateAvailable = (TextView) convertView.findViewById(R.id.update_available);
			viewHolder.downloadProgress = convertView.findViewById(R.id.download_progress);
			viewHolder.checkBox.setOnClickListener(this);
			convertView.setTag(viewHolder);
			
		}
		
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		viewHolder.downloadProgress.setVisibility(View.GONE); //This is being done to clear the spinner animation.
		viewHolder.downloadProgress.clearAnimation();
		
		if(category.getTotalStickers() > 0)
		{
			viewHolder.categorySize.setVisibility(View.VISIBLE);
			viewHolder.categorySize.setText(mContext.getString(R.string.n_stickers, category.getTotalStickers()));
		}
		else
		{
			viewHolder.categorySize.setVisibility(View.GONE);
		}
		
		if(category.getState() == StickerCategory.UPDATE)
		{
			if(!category.isVisible())
			{
				viewHolder.updateAvailable.setTextColor(mContext.getResources().getColor(R.color.shop_update_invisible_color));
			}
			
			viewHolder.updateAvailable.setVisibility(View.VISIBLE);
			checkAndDisableCheckBox(category.getCategoryId(), viewHolder.checkBox);
		}
		
		else if(category.getState() == StickerCategory.DOWNLOADING)
		{
			viewHolder.updateAvailable.setText(R.string.downloading_sticker);
			viewHolder.updateAvailable.setVisibility(View.VISIBLE);
			viewHolder.downloadProgress.setVisibility(View.VISIBLE);
			viewHolder.checkBox.setVisibility(View.GONE);
			viewHolder.downloadProgress.setAnimation(AnimationUtils.loadAnimation(mContext, R.anim.rotate));
		}
		else
		{ 
			viewHolder.updateAvailable.setVisibility(View.GONE);
			viewHolder.downloadProgress.setVisibility(View.GONE);
			viewHolder.downloadProgress.clearAnimation();
			checkAndDisableCheckBox(category.getCategoryId(), viewHolder.checkBox);
		}
		
		viewHolder.checkBox.setTag(category);
		viewHolder.categoryName.setText(category.getCategoryName());
		viewHolder.checkBox.setChecked(category.isVisible());
		stickerPreviewLoader.loadImage(category.getCategoryId() + HikeConstants.DELIMETER + StickerManager.PREVIEW_IMAGE, viewHolder.categoryPreviewImage, isListFlinging);
		
		return convertView;
	}

	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;

		isListFlinging = b;

		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}

	/**
	 * On drop, this updates the mapping between ArrayList positions and ListView positions. The ArrayList is unchanged.
	 * 
	 * @see DragSortListView.DropListener#drop(int, int)
	 */
	@Override
	public void drop(int from, int to)
	{
		StickerCategory category = getItem(from);
		if ((from == to) || (!category.isVisible())) // Dropping at the same position. No need to perform Drop.
		{
			return;
		}

		if (from > lastVisibleIndex)
		{
			if(to > lastVisibleIndex+1)
			{
			   return;
			}
			else
			{
				lastVisibleIndex++;
			}
		}

		int cursorFrom = mListMapping[from];
		if (from > to)
		{
			for (int i = from; i > to; --i)
			{
				mListMapping[i] = mListMapping[i - 1];
			}
		}

		else
		{
			for (int i = from; i < to; ++i)
			{
				mListMapping[i] = mListMapping[i + 1];
			}
		}

		mListMapping[to] = cursorFrom;

		notifyDataSetChanged();

		if (from > to)
		{
			for (int i = from; i >= to; --i)
			{
				addToStickerSet(i);
			}
		}
		else
		{
			for (int i = from; i <= to; ++i)
			{
				addToStickerSet(i);
			}
		}
	}
	
	/**
	 * Adds to Categories to stickerSet and also changes it's categoryIndex
	 * @param categoryPos
	 */
	public void addToStickerSet(int categoryPos)
	{
		StickerCategory category = getItem(categoryPos);
		int oldCategoryIndex = mListMapping[categoryPos];
		int newCategoryIndex = categoryPos + 1;  // new stickerCategoryIndex is categoryPos + 1
		if(oldCategoryIndex != newCategoryIndex)
		{
			category.setCategoryIndex(newCategoryIndex); 
			stickerSet.add(category);
		}
	}

	@Override
	public void drag(int from, int to)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(int which)
	{
		// TODO Auto-generated method stub

	}

	public void persistChanges()
	{
		if(stickerSet.size() > 0)
		{
			StickerManager.getInstance().saveVisibilityAndIndex(stickerSet);
		}
	}

	private class ViewHolder
	{
		TextView categoryName;
		
		CheckBox checkBox;
		
		ImageView categoryPreviewImage;

		TextView updateAvailable;
		
		TextView categorySize;
		
		View downloadProgress;
	}
	
	@Override
	public void onClick(View v)
	{
		StickerCategory category = (StickerCategory) v.getTag();
		boolean visibility = !category.isVisible(); 
		CheckBox checkBox = (CheckBox) v;
		category.setVisible(visibility);
		checkBox.setChecked(visibility);
		stickerSet.add(category);
		int categoryIdx = stickerCategories.indexOf(category);
		updateLastVisibleIndex(categoryIdx, category);
	}

	/**
	 * Updates the lastVisible Category Index in the list based on the category whose visibility has just been toggled.
	 * @param categoryIdx
	 * @param category
	 */
	private void updateLastVisibleIndex(int categoryIdx, StickerCategory category)
	{
		if(categoryIdx == lastVisibleIndex && (!category.isVisible()))
		{
			lastVisibleIndex --;
		}
		else if((categoryIdx == lastVisibleIndex + 1) && (category.isVisible()))
		{
			lastVisibleIndex ++;
		}
	}

	public Set<StickerCategory> getStickerSet()
	{
		return stickerSet;
	}
	
	public StickerPreviewLoader getStickerPreviewLoader()
	{
		return stickerPreviewLoader;
	}
	/**
	 * Hides/makes the checkbox visible conditionally
	 * @param categoryId
	 * @param cb
	 */
	private void checkAndDisableCheckBox(String categoryId, CheckBox cb)
	{
		if(categoryId.equals(StickerManager.HUMANOID) || categoryId.equals(StickerManager.EXPRESSIONS))
		{
			cb.setVisibility(View.GONE);
		}
		else
		{
			cb.setVisibility(View.VISIBLE);
		}
	}
	
	public int getLastVisibleIndex()
	{
		return lastVisibleIndex;
	}

}
