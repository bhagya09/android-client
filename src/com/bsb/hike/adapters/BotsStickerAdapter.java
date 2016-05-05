package com.bsb.hike.adapters;

/**
 * Created by konarkarora on 29/04/16.
 */

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerPageAdapterItem;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.List;

/**
 * The Bots type sticker adapter.
 */
public class BotsStickerAdapter extends BaseAdapter implements View.OnClickListener, HikePubSub.Listener
{

	/**
	 * The enum View type.
	 */
	public enum ViewType
	{
		/**
		 * Sticker view type.
		 */
		STICKER,
		/**
		 * Downloading view type.
		 */
		DOWNLOADING,
	}

	private int sizeEachImage;

	private Context mContext;

	private List<StickerPageAdapterItem> itemList;

	private StickerLoader stickerLoader;

	private StickerPickerListener mStickerPickerListener;

	private String stickerSize;

	private LayoutInflater inflater;

	private static final int REFRESH_ADAPTER = 1;

	private String[] pubSubListeners = { HikePubSub.STICKER_DOWNLOADED };

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
	 * @param stickerSize
	 *            the sticker size
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
		this.inflater = LayoutInflater.from(mContext);
		calculateSizeOfStickerImage(numItemsRow);
		registerListener();
	}

	private void registerListener()
	{
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
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
		ViewType viewType = ViewType.STICKER; // Default value.
		StickerPageAdapterItem item = getItem(position);
		int itemId = item.getType();
		switch (itemId)
		{
		case StickerPageAdapterItem.STICKER:
			viewType = ViewType.STICKER;
			break;
		case StickerPageAdapterItem.DOWNLOADING:
			viewType = ViewType.DOWNLOADING;
			break;
		}

		return viewType.ordinal();
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
			switch (viewType)
			{
			case STICKER:
				convertView = new RecyclingImageView(mContext);
				int padding = (int) (5 * Utils.scaledDensityMultiplier);
				convertView.setLayoutParams(ll);
				((ImageView) convertView).setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				(convertView).setPadding(padding, padding, padding, padding);
				break;
			case DOWNLOADING:
				convertView = inflater.inflate(R.layout.update_sticker_set, null);
				convertView.setLayoutParams(ll);
				viewHolder.text = (TextView) convertView.findViewById(R.id.new_number_stickers);
				viewHolder.image = (ImageView) convertView.findViewById(R.id.update_btn);
				viewHolder.progress = (ProgressBar) convertView.findViewById(R.id.download_progress);
				viewHolder.tickImage = (ImageView) convertView.findViewById(R.id.sticker_placeholder);
			}

			convertView.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}

		switch (viewType)
		{
		case STICKER:
			Sticker sticker = item.getSticker();
			switch (stickerSize)
			{
			case HikePlatformConstants.BotsStickerSize.MEDIUM:
			case HikePlatformConstants.BotsStickerSize.LARGE:
				stickerLoader.loadSticker(sticker, StickerConstants.StickerType.LARGE, ((ImageView) convertView), false);
				break;
			case HikePlatformConstants.BotsStickerSize.SMALL:
				stickerLoader.loadSticker(sticker, StickerConstants.StickerType.SMALL, ((ImageView) convertView), false);
				break;
			default:
				stickerLoader.loadSticker(sticker, StickerConstants.StickerType.LARGE, ((ImageView) convertView), false);
			}
			convertView.setOnClickListener(this);
			break;
		case DOWNLOADING:
			viewHolder.progress.setVisibility(View.VISIBLE);
			viewHolder.text.setVisibility(View.GONE);
			viewHolder.tickImage.setVisibility(View.GONE);
			break;
		}

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

	private void refreshStickers(Object data)
	{
		if (data instanceof Sticker)
		{
			Sticker sticker = (Sticker) data;
			StickerPageAdapterItem item = new StickerPageAdapterItem(StickerPageAdapterItem.DOWNLOADING, sticker);
			itemList.remove(item);
			itemList.add(new StickerPageAdapterItem(StickerPageAdapterItem.STICKER, sticker));
            notifyDataSetChanged();
		}
	}

	/**
	 * Unregister listeners.
	 */
	public void unregisterListeners()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		switch (type)
		{
		case HikePubSub.STICKER_DOWNLOADED:
			sendUIMessage(REFRESH_ADAPTER, object);
			break;
		}
	}

	private void handleUIMessage(android.os.Message msg)
	{
		switch (msg.what)
		{
		case REFRESH_ADAPTER:
			refreshStickers(msg.obj);
			break;
		default:
			Logger.d(getClass().getSimpleName(), "Did not find any matching event for msg.what : " + msg.what);
			break;
		}
	}

	private void sendUIMessage(int what, Object data)
	{
		Message message = Message.obtain();
		message.what = what;
		message.obj = data;
		uiHandler.sendMessage(message);
	}

	private Handler uiHandler = new Handler(Looper.getMainLooper())
	{
		public void handleMessage(android.os.Message msg)
		{
			/**
			 * Defensive check
			 */
			if (msg == null)
			{
				Logger.e(getClass().getSimpleName(), "Getting a null message in bots sticker Adapter");
				return;
			}
			handleUIMessage(msg);
		}

	};

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