package com.bsb.hike.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.DragSortListView.DragSortListView;
import com.bsb.hike.DragSortListView.DragSortListView.DragSortListener;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.dialog.CustomAlertDialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerSettingsTask;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.tasks.DeleteStickerPackAsyncTask;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	
	private StickerOtherIconLoader stickerOtherIconLoader;
	
	private StickerCategory draggedCategory = null;

	private StickerSettingsTask stickerSettingsTask;

	private HikeDialog deleteDialog;

	private ItemButtonClickListener clickListener;

	private final int FULLY_DOWNLOADED = 0;

	private final int UPDATE_AVAILABLE = 2;

	private final int RETRY = 3;

	public StickerSettingsAdapter(Context context, List<StickerCategory> stickerCategories, StickerSettingsTask stickerSettingsTask, ItemButtonClickListener clickListener)
	{
		this.mContext = context;
		this.stickerCategories = stickerCategories;
		this.mInflater = LayoutInflater.from(mContext);
		mListMapping = new int[stickerCategories.size()];
		this.stickerOtherIconLoader = new StickerOtherIconLoader(context, true);
		this.stickerSettingsTask = stickerSettingsTask;
		this.clickListener = clickListener;
		initialiseMapping(mListMapping, stickerCategories);
		
	}

	public interface ItemButtonClickListener
	{
		void onDownloadClicked(StickerCategory stickerCategory);

		void onDelete(StickerCategory stickerCategory);
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

	//Mwthod to check if all the packs in update list have got updated
	private boolean areAllUpdated()
	{
		int count = 0;

		for (StickerCategory category: stickerCategories)
		{
			if ((category.getState() == StickerCategory.DONE) || (category.getState() == StickerCategory.DONE_SHOP_SETTINGS))
			{
				count++;
			}
		}

		return count==stickerCategories.size();
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
			viewHolder.categoryPreviewImage = (ImageView) convertView.findViewById(R.id.category_icon);
			viewHolder.categorySize = (TextView) convertView.findViewById(R.id.category_size);
			viewHolder.updateAvailable = (TextView) convertView.findViewById(R.id.update_available);
			viewHolder.downloadProgress = (ProgressBar) convertView.findViewById(R.id.download_progress);
			viewHolder.deleteButton = (ImageButton) convertView.findViewById(R.id.delete_button);
			viewHolder.deleteButton.setOnClickListener(this);
			viewHolder.updateButton = (ImageView) convertView.findViewById(R.id.update_button);
			viewHolder.updateButton.setOnClickListener(this);
			viewHolder.reorderIcon = (ImageView) convertView.findViewById(R.id.reorder_icon);
			viewHolder.updateStickersCount = (TextView) convertView.findViewById(R.id.update_stickers_count);
			viewHolder.checkBox = (ImageButton) convertView.findViewById(R.id.category_checkbox);
			viewHolder.checkBox.setOnClickListener(this);
			convertView.setTag(viewHolder);
			
		}
		
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}

		//doing task specific things
		switch(stickerSettingsTask)
		{
			case STICKER_DELETE_TASK:
				viewHolder.deleteButton.setVisibility(View.VISIBLE);
				break;

			case STICKER_REORDER_TASK:
				viewHolder.reorderIcon.setVisibility(View.VISIBLE);
				break;

			case STICKER_UPDATE_TASK:
				viewHolder.updateButton.setVisibility(View.VISIBLE);
				viewHolder.updateButton.setImageLevel(UPDATE_AVAILABLE);
				viewHolder.updateStickersCount.setVisibility(View.VISIBLE);
				viewHolder.updateStickersCount.setText(mContext.getString(R.string.n_more_stickers, category.getMoreStickerCount()));
				break;
			case STICKER_HIDE_TASK:
				viewHolder.checkBox.setVisibility(View.VISIBLE);
				break;
		}

		viewHolder.downloadProgress.setVisibility(View.GONE); //This is being done to clear the spinner animation.
		viewHolder.downloadProgress.clearAnimation();
		
		if(category.getTotalStickers() > 0 && (stickerSettingsTask != StickerSettingsTask.STICKER_UPDATE_TASK))		//showing no. of more stickers rather than total stickers in case of update packs
		{
			viewHolder.categorySize.setVisibility(View.VISIBLE);
			viewHolder.categorySize.setText(category.getTotalStickers() == 1 ? mContext.getString(R.string.singular_stickers, category.getTotalStickers()) : mContext.getString(R.string.n_stickers, category.getTotalStickers()));
		}
		else
		{
			viewHolder.categorySize.setVisibility(View.GONE);
		}
		
		int state = category.getState();
		switch(state)
		{
			case StickerCategory.UPDATE:
				viewHolder.updateAvailable.setTextColor(category.isVisible() ? mContext.getResources().getColor(R.color.sticker_settings_update_color) : mContext.getResources().getColor(R.color.shop_update_invisible_color));
				viewHolder.updateAvailable.setVisibility(View.GONE);
				viewHolder.updateAvailable.setText(mContext.getResources().getString(R.string.update_sticker));
				viewHolder.downloadProgress.setVisibility(View.GONE);

				break;
			case StickerCategory.DOWNLOADING:
				if((stickerSettingsTask == StickerSettingsTask.STICKER_DELETE_TASK) || (stickerSettingsTask == StickerSettingsTask.STICKER_UPDATE_TASK))
				{
					viewHolder.updateAvailable.setTextColor(category.isVisible() ? mContext.getResources().getColor(R.color.sticker_settings_update_color) : mContext.getResources().getColor(R.color.shop_update_invisible_color));
					viewHolder.updateAvailable.setText(R.string.downloading_stk);
					viewHolder.updateAvailable.setVisibility(View.VISIBLE);
					viewHolder.deleteButton.setVisibility(View.GONE);
					viewHolder.downloadProgress.setVisibility(View.VISIBLE);
					viewHolder.updateButton.setVisibility(View.GONE);
				}
				break;
			case StickerCategory.DONE_SHOP_SETTINGS:  //To be treated as same
			case StickerCategory.DONE:
				showUIForState(state, viewHolder, category.getCategoryId(), category.isVisible());
				viewHolder.updateButton.setImageLevel(FULLY_DOWNLOADED);
				break;
			case StickerCategory.RETRY:
				showUIForState(state, viewHolder, category.getCategoryId(), category.isVisible());
				viewHolder.updateButton.setImageLevel(RETRY);

				break;

			default:
				viewHolder.updateAvailable.setVisibility(View.GONE);
				viewHolder.downloadProgress.setVisibility(View.GONE);
		}


			
		viewHolder.checkBox.setTag(category);
		viewHolder.deleteButton.setTag(category);
		viewHolder.updateButton.setTag(category);
		viewHolder.categoryName.setText(category.getCategoryName());
		viewHolder.checkBox.setSelected(category.isVisible());
		stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(category.getCategoryId(), StickerManager.PREVIEW_IMAGE_SHOP_TYPE), viewHolder.categoryPreviewImage, isListFlinging);
		stickerOtherIconLoader.setImageSize(StickerManager.PREVIEW_IMAGE_SIZE, StickerManager.PREVIEW_IMAGE_SIZE);
		return convertView;
	}

	/**
	 * Handles the display of UI for a Settings page list item based on the categoryState 
	 * @param state
	 * @param viewHolder
	 * @param categoryId
	 */
	private void showUIForState(int state, ViewHolder viewHolder, String categoryId, boolean isVisible)
	{
		viewHolder.updateAvailable.setVisibility((state == StickerCategory.DONE || state == StickerCategory.DONE_SHOP_SETTINGS ||
				stickerSettingsTask != StickerSettingsTask.STICKER_UPDATE_TASK) ? View.GONE : View.VISIBLE);
		viewHolder.updateAvailable.setText(state == StickerCategory.DONE ? R.string.see_them : R.string.RETRY);
		viewHolder.updateAvailable.setTextColor(isVisible ? mContext.getResources().getColor(R.color.sticker_settings_update_color) : mContext.getResources().getColor(R.color.shop_update_invisible_color));
		viewHolder.downloadProgress.setVisibility(View.GONE);
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
		draggedCategory = category;
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
		
		ImageButton deleteButton;

		ImageView categoryPreviewImage;

		TextView updateAvailable;
		
		TextView categorySize;
		
		ProgressBar downloadProgress;

		ImageView updateButton;

		ImageView reorderIcon;

		TextView updateStickersCount;

		ImageButton checkBox;
	}

	public void onStickerPackDelete(StickerCategory category)
	{
		if (deleteDialog != null  && deleteDialog.isShowing())
		{
			deleteDialog.dismiss();
		}
		stickerCategories.remove(category);								//removing sticker pack from sticker categories list
		stickerSet.remove(category);									//removing sticker pack from sticker set
		mListMapping = null;
		mListMapping = new int[stickerCategories.size()];
		initialiseMapping(mListMapping, stickerCategories);				//reinitialising mapping
		this.notifyDataSetChanged();
		sendDeleteClicked(category);
		StickerManager.getInstance().sendPackDeleteAnalytics(HikeConstants.LogEvent.PACK_DELETE_SUCCESS, category.getCategoryId());
	}

	public void onStickerPackHide(View v, StickerCategory category)
	{
		boolean visibility = !category.isVisible();
		Toast.makeText(mContext, visibility ? mContext.getResources().getString(R.string.pack_visible) : mContext.getResources().getString(R.string.pack_hidden), Toast.LENGTH_SHORT).show();;
		ImageButton checkBox = (ImageButton) v;
		category.setVisible(visibility);
		StickerManager.getInstance().sendPackHideAnalytics(category.getCategoryId(), visibility);
		checkBox.setSelected(visibility);
		stickerSet.add(category);
		int categoryIdx = stickerCategories.indexOf(category);
		updateLastVisibleIndex(categoryIdx, category);
		StickerManager.getInstance().checkAndSendAnalytics(visibility);
	}

	@Override
	public void onClick(View v)
	{
			final StickerCategory category = (StickerCategory) v.getTag();

			switch(v.getId())
			{
				case R.id.delete_button:
					StickerManager.getInstance().sendPackDeleteAnalytics(HikeConstants.LogEvent.PACK_DELETE_CLICKED, category.getCategoryId());
					final DeleteStickerPackAsyncTask deletePackTask = new DeleteStickerPackAsyncTask(category);
					deleteDialog = HikeDialogFactory.showDialog(mContext, HikeDialogFactory.DELETE_STICKER_PACK_DIALOG,
							new HikeDialogListener() {

								@Override
								public void positiveClicked(HikeDialog hikeDialog)
								{
									deletePackTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
									StickerManager.getInstance().sendPackDeleteAnalytics(HikeConstants.LogEvent.DELETE_POSITIVE_CLICKED, category.getCategoryId());
									//Displaying delete progress bar and deleting message in delete dialog box
									CustomAlertDialog deleteDialog = (CustomAlertDialog)hikeDialog;
									View buttonPanel = deleteDialog.findViewById(R.id.button_panel);
									buttonPanel.setVisibility(View.GONE);
									ProgressBar deletingProgress = (ProgressBar) deleteDialog.findViewById(R.id.loading_progress);
									deletingProgress.setVisibility(View.VISIBLE);
									deleteDialog.setMessage(R.string.deleting_pack);
								}

								@Override
								public void neutralClicked(HikeDialog hikeDialog)
								{

								}

								@Override
								public void negativeClicked(HikeDialog hikeDialog)
								{
									hikeDialog.dismiss();
									StickerManager.getInstance().sendPackDeleteAnalytics(HikeConstants.LogEvent.DELETE_NEGATIVE_CLICKED, category.getCategoryId());
								}

							}
							,category.getCategoryName());
					break;

				case R.id.update_button:
					StickerManager.getInstance().initialiseDownloadStickerPackTask(category, StickerManager.getInstance().getPackDownloadBodyJson(DownloadSource.SETTINGS));
					StickerManager.getInstance().sendPackUpdateAnalytics(HikeConstants.LogEvent.STICKER_PACK_UPDATE, category.getCategoryId());
					sendDownloadClicked(category);
					this.notifyDataSetChanged();
					break;

				case R.id.category_checkbox:
					onStickerPackHide(v, category);
					break;

				default:
					break;
			}
	}

	private void sendDownloadClicked(StickerCategory stickerCategory)
	{
		if(clickListener != null)
		{
			clickListener.onDownloadClicked(stickerCategory);
		}
	}

	private void sendDeleteClicked(StickerCategory stickerCategory)
	{
		if(clickListener != null)
		{
			clickListener.onDelete(stickerCategory);
		}
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
	
	public StickerOtherIconLoader getStickerPreviewLoader()
	{
		return stickerOtherIconLoader;
	}

	public int getLastVisibleIndex()
	{
		return lastVisibleIndex;
	}
	
	public StickerCategory getDraggedCategory()
	{
		return draggedCategory;
	}
}
