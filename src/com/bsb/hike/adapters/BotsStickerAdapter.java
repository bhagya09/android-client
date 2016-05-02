package com.bsb.hike.adapters;

/**
 * Created by konarkarora on 29/04/16.
 */

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerPageAdapterItem;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.List;

/**
 * The Bots type sticker adapter.
 */
public class BotsStickerAdapter extends BaseAdapter implements View.OnClickListener
{

	/**
	 * The enum View type.
	 */
	public enum ViewType
	{
		STICKER, UPDATE, DOWNLOADING, RETRY, DONE, PLACE_HOLDER
	}

	private int sizeEachImage;

	private Context mContext;

	private List<StickerPageAdapterItem> itemList;

	private StickerLoader stickerLoader;

	private boolean isListFlinging;

	private StickerPickerListener mStickerPickerListener;

	private String stickerSize;

	/**
	 * The Abs list view.
	 */
	AbsListView absListView;

	/**
	 * Instantiates a new Bots sticker adapter.
	 *
	 * @param context
	 *            the context
	 * @param itemList
	 *            the item list
	 * @param worker
	 *            the worker
	 * @param absListView
	 *            the abs list view
	 * @param listener
	 *            the listener
	 * @param numItemsRow
	 *            the num items row
	 */
	public BotsStickerAdapter(Context context, List<StickerPageAdapterItem> itemList, StickerLoader worker, AbsListView absListView, StickerPickerListener listener,
			String stickerSize, int numItemsRow)
	{
		this.mContext = context;
		this.itemList = itemList;
		this.mStickerPickerListener = listener;
		this.stickerLoader = worker;
		this.absListView = absListView;
		this.stickerSize = stickerSize;
		calculateSizeOfStickerImage(numItemsRow);
	}

	/**
	 * Calculate size of sticker image.
	 *
	 * @param numItemsPerRow
	 *            the num items per row
	 */
	public void calculateSizeOfStickerImage(int numItemsPerRow)
	{
		int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;

		int stickerPadding = 2 * mContext.getResources().getDimensionPixelSize(R.dimen.sticker_padding);
		int horizontalSpacing = (numItemsPerRow - 1) * mContext.getResources().getDimensionPixelSize(R.dimen.sticker_grid_horizontal_padding);

		int actualSpace = (screenWidth - horizontalSpacing - stickerPadding);

		this.sizeEachImage = actualSpace / numItemsPerRow;
	}

	@Override
	public int getViewTypeCount()
	{
		return ViewType.values().length;
	}

	@Override
	public int getCount()
	{
		return itemList.size();
	}

	@Override
	public StickerPageAdapterItem getItem(int position)
	{
		return itemList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public int getItemViewType(int position)
	{
		return ViewType.STICKER.ordinal();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewType viewType = ViewType.values()[getItemViewType(position)];
		StickerPageAdapterItem item = getItem(position);
		ViewHolder viewHolder;
		AbsListView.LayoutParams ll = new AbsListView.LayoutParams(sizeEachImage, sizeEachImage);

		if (convertView == null)
		{
			viewHolder = new ViewHolder();
			convertView = new RecyclingImageView(mContext);
			int padding = (int) (5 * Utils.scaledDensityMultiplier);
			convertView.setLayoutParams(ll);
			((ImageView) convertView).setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			(convertView).setPadding(padding, padding, padding, padding);
			convertView.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}

		Sticker sticker = item.getSticker();
        if (!sticker.isStickerAvailable())
        {
            StickerManager.getInstance().initiateSingleStickerDownloadTask(sticker.getStickerId(), sticker.getCategoryId(), null);
            notifyDataSetChanged();
        }

		switch (stickerSize)
		{
		case HikePlatformConstants.BotsStickerSize.MEDIUM:
		case HikePlatformConstants.BotsStickerSize.LARGE:
			stickerLoader.loadSticker(sticker, StickerConstants.StickerType.LARGE, ((ImageView) convertView), isListFlinging);
			break;
		case HikePlatformConstants.BotsStickerSize.SMALL:
            stickerLoader.loadSticker(sticker, StickerConstants.StickerType.SMALL, ((ImageView) convertView), isListFlinging);
			break;
		default:
			stickerLoader.loadSticker(sticker, StickerConstants.StickerType.LARGE, ((ImageView) convertView), isListFlinging);
		}

		convertView.setOnClickListener(this);

		viewHolder.position = position;
		return convertView;
	}

	@Override
	public void onClick(View v)
	{
		ViewHolder viewHolder = (ViewHolder) v.getTag();
		int position = viewHolder.position;
		StickerPageAdapterItem item = getItem(position);
		switch (item.getType())
		{
		case StickerPageAdapterItem.STICKER:
			Sticker sticker = item.getSticker();
			String source = StickerManager.FROM_OTHER;
			mStickerPickerListener.stickerSelected(sticker, source);
			break;
		default:
			break;
		}

	}

	/**
	 * Sets is list flinging.
	 *
	 * @param b
	 *            the b
	 */
	public void setIsListFlinging(boolean b)
	{
		boolean notify = b != isListFlinging;

		isListFlinging = b;

		if (notify && !isListFlinging)
		{
			notifyDataSetChanged();
		}
	}

	private class ViewHolder
	{
		/**
		 * The Image.
		 */
		ImageView image;

		/**
		 * The Text.
		 */
		TextView text;

		/**
		 * The Progress.
		 */
		ProgressBar progress;

		/**
		 * The Position.
		 */
		int position;

		/**
		 * The Tick image.
		 */
		ImageView tickImage;
	}

}