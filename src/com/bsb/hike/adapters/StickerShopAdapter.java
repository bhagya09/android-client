package com.bsb.hike.adapters;

import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerPalleteImageDownloadTask;
import com.bsb.hike.smartImageLoader.StickerOtherIconLoader;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerShopAdapter extends CursorAdapter
{
	private LayoutInflater layoutInflater;

	private StickerOtherIconLoader stickerOtherIconLoader;
	
	private boolean isListFlinging;

	private int idColoumn;

	private int categoryNameColoumn;

	private int totalStickersCountColoumn;

	private int categorySizeColoumn;

	private Map<String, StickerCategory> stickerCategoriesMap;
	
	private final int FULLY_DOWNLOADED = 0;
	
	private final int NOT_DOWNLOADED = 1;
	
	private final int UPDATE_AVAILABLE = 2;
	
	private final int RETRY = 3;

	class ViewHolder
	{
		TextView categoryName;

		TextView totalStickers;

		TextView stickersPackDetails;
		
		TextView categoryPrice;

		ImageView downloadState;
		
		ImageView categoryPreviewIcon;

		ProgressBar downloadProgress;
	}

	public StickerShopAdapter(Context context, Cursor cursor, Map<String, StickerCategory> stickerCategoriesMap)
	{
		super(context, cursor, false);
		this.layoutInflater = LayoutInflater.from(context);
		this.stickerOtherIconLoader = new StickerOtherIconLoader(context, true);
		this.idColoumn = cursor.getColumnIndex(DBConstants._ID);
		this.categoryNameColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_NAME);
		this.totalStickersCountColoumn = cursor.getColumnIndex(DBConstants.TOTAL_NUMBER);
		this.categorySizeColoumn = cursor.getColumnIndex(DBConstants.CATEGORY_SIZE);
		this.stickerCategoriesMap = stickerCategoriesMap;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		View v = layoutInflater.inflate(R.layout.sticker_shop_list_item, parent, false);
		ViewHolder viewholder = new ViewHolder();
		viewholder.categoryName = (TextView) v.findViewById(R.id.category_name);
		viewholder.stickersPackDetails = (TextView) v.findViewById(R.id.pack_details);
		viewholder.downloadState = (ImageView) v.findViewById(R.id.category_download_btn);
		viewholder.categoryPreviewIcon = (ImageView) v.findViewById(R.id.category_icon);
		viewholder.downloadProgress = (ProgressBar) v.findViewById(R.id.download_progress_bar);
		viewholder.categoryPrice = (TextView) v.findViewById(R.id.category_price);
		viewholder.downloadState.setOnClickListener(mDownloadButtonClickListener);
		v.setTag(viewholder);
		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor)
	{
		ViewHolder viewholder = (ViewHolder) view.getTag();
		String categoryId = cursor.getString(idColoumn);
		String displayCategoryName = context.getResources().getString(R.string.pack_rank, cursor.getPosition() + 1);
		String categoryName = cursor.getString(categoryNameColoumn);
		displayCategoryName += " " + categoryName;
		int totalStickerCount = cursor.getInt(totalStickersCountColoumn);
		int categorySizeInBytes = cursor.getInt(categorySizeColoumn);
		viewholder.categoryName.setText(displayCategoryName);
		stickerOtherIconLoader.loadImage(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(categoryId, StickerManager.PREVIEW_IMAGE_TYPE), viewholder.categoryPreviewIcon);
		stickerOtherIconLoader.setImageSize(StickerManager.PREVIEW_IMAGE_SIZE, StickerManager.PREVIEW_IMAGE_SIZE);
		viewholder.downloadProgress.setVisibility(View.GONE);
		if (totalStickerCount > 0)
		{
			String detailsStirng = totalStickerCount == 1 ? context.getResources().getString(R.string.singular_stickers, totalStickerCount)  : context.getResources().getString(R.string.n_stickers, totalStickerCount);
			if (categorySizeInBytes > 0)
			{
				detailsStirng += ", " + Utils.getSizeForDisplay(categorySizeInBytes);
			}
			viewholder.stickersPackDetails.setVisibility(View.VISIBLE);
			viewholder.stickersPackDetails.setText(detailsStirng);
		}
		else
		{
			viewholder.stickersPackDetails.setVisibility(View.GONE);
		}

		StickerCategory category;
		if (stickerCategoriesMap.containsKey(categoryId))
		{
			category = stickerCategoriesMap.get(categoryId);
		}
		else
		{
			category = new StickerCategory(categoryId, categoryName, totalStickerCount, categorySizeInBytes);
			stickerCategoriesMap.put(categoryId, category);
		}
		viewholder.downloadState.setVisibility(View.VISIBLE);
		
		if(category.isVisible())
		{
			switch (category.getState())
			{
			case StickerCategory.NONE:
			case StickerCategory.DONE_SHOP_SETTINGS:
			case StickerCategory.DONE:
				if (category.getDownloadedStickersCount() == 0)
				{
					viewholder.downloadState.setImageLevel(NOT_DOWNLOADED);
					viewholder.categoryPrice.setVisibility(View.VISIBLE);
					viewholder.categoryPrice.setText(context.getResources().getString(R.string.sticker_pack_free));
					viewholder.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));
				}
				else
				{
					viewholder.downloadState.setImageLevel(FULLY_DOWNLOADED);
					viewholder.categoryPrice.setVisibility(View.GONE);
				}
				break;
			case StickerCategory.UPDATE:
				viewholder.downloadState.setImageLevel(UPDATE_AVAILABLE);
				viewholder.categoryPrice.setVisibility(View.VISIBLE);
				viewholder.categoryPrice.setText(context.getResources().getString(R.string.update_sticker));
				viewholder.categoryPrice.setTextColor(context.getResources().getColor(R.color.sticker_settings_update_color));
				break;
			case StickerCategory.RETRY:
				viewholder.downloadState.setImageLevel(RETRY);
				viewholder.categoryPrice.setVisibility(View.VISIBLE);
				viewholder.categoryPrice.setText(context.getResources().getString(R.string.RETRY));
				viewholder.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));
				break;
			case StickerCategory.DOWNLOADING:
				viewholder.downloadState.setVisibility(View.GONE);
				viewholder.downloadProgress.setVisibility(View.VISIBLE);
				viewholder.categoryPrice.setVisibility(View.VISIBLE);
				viewholder.categoryPrice.setText(context.getResources().getString(R.string.downloading_stk));
				viewholder.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));
				
				break;
			}
		}
		else
		{
			viewholder.downloadState.setImageLevel(NOT_DOWNLOADED);
			viewholder.categoryPrice.setVisibility(View.VISIBLE);
			viewholder.categoryPrice.setText(context.getResources().getString(R.string.sticker_pack_free));
			viewholder.categoryPrice.setTextColor(context.getResources().getColor(R.color.tab_pressed));
		}
		viewholder.downloadState.setTag(category);
	}
	

	public StickerOtherIconLoader getStickerPreviewLoader()
	{
		return stickerOtherIconLoader;
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

	private OnClickListener mDownloadButtonClickListener = new OnClickListener()
	{
		
		@Override
		public void onClick(View view)
		{
			ImageView downloadButton = (ImageView) view;
			StickerCategory category = (StickerCategory) view.getTag();
			switch (downloadButton.getDrawable().getLevel())
			{
			case NOT_DOWNLOADED:
				StickerPalleteImageDownloadTask stickerPalleteImageDownloadTask = new StickerPalleteImageDownloadTask(category.getCategoryId());
				stickerPalleteImageDownloadTask.execute();
				StickerManager.getInstance().initialiseDownloadStickerTask(category, DownloadSource.SHOP, DownloadType.NEW_CATEGORY, mContext);
				break;
			case UPDATE_AVAILABLE:
			case RETRY:
				StickerManager.getInstance().initialiseDownloadStickerTask(category, DownloadSource.SHOP, mContext);
				break;

			default:
				break;
			}
		}
	};

}
