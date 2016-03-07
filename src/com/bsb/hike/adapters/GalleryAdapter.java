package com.bsb.hike.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.smartImageLoader.GalleryImageLoader;
import com.bsb.hike.utils.Utils;

public class GalleryAdapter extends BaseAdapter
{

	private List<GalleryItem> galleryItemList;

	private LayoutInflater layoutInflater;

	private GalleryImageLoader galleryImageLoader;

	private boolean isListFlinging;

	private boolean isInsideAlbum;

	private int sizeOfImage;

	private List<GalleryItem> selectedGalleryItems;

	private int selectedItemPostion = -1;

	private boolean selectedScreen = false;

	public GalleryAdapter(Context context, List<GalleryItem> galleryItems, boolean isInsideAlbum, int sizeOfImage, List<GalleryItem> selectedItems, boolean selectedScreen)
	{
		this.layoutInflater = LayoutInflater.from(context);
		this.galleryItemList = galleryItems;
		this.isInsideAlbum = isInsideAlbum;
		this.sizeOfImage = sizeOfImage;
		this.selectedGalleryItems = selectedItems;
		this.selectedScreen = selectedScreen;

		this.galleryImageLoader = new GalleryImageLoader(context , sizeOfImage);
		this.galleryImageLoader.setDontSetBackground(true);
		this.galleryImageLoader.setDefaultDrawableNull(false);
	}

	@Override
	public int getCount()
	{
		return galleryItemList.size();
	}

	@Override
	public GalleryItem getItem(int position)
	{
		return galleryItemList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	public void setSelectedItemPosition(int position)
	{
		this.selectedItemPostion = position;
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		GalleryItem galleryItem = getItem(position);
		ViewHolder holder;

		if (convertView == null)
		{
			convertView = layoutInflater.inflate(R.layout.gallery_item, null);
			holder = new ViewHolder();

			holder.galleryName = (TextView) convertView.findViewById(R.id.album_title);
			holder.galleryCount = (TextView) convertView.findViewById(R.id.album_count);
			holder.galleryThumb = (ImageView) convertView.findViewById(R.id.album_image);
			holder.contentLayout = (ViewGroup) convertView.findViewById(R.id.contentLayout);
			holder.selected = convertView.findViewById(R.id.selected);


			holder.selected.setBackgroundResource(selectedScreen ? R.drawable.gallery_item_selected_selector : R.drawable.gallery_item_selector);

			LayoutParams layoutParams = new LayoutParams(sizeOfImage, sizeOfImage);
			holder.galleryThumb.setLayoutParams(layoutParams);

			convertView.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
		}
		


		if (!isInsideAlbum && galleryItem.getType()!=GalleryItem.CUSTOM)
		{
			(convertView.findViewById(R.id.album_layout)).setVisibility(View.VISIBLE);
			holder.galleryName.setVisibility(View.VISIBLE);
			holder.galleryName.setText(galleryItem.getName());

			if(galleryItem.getBucketCount() > 0)
			{
				holder.galleryCount.setVisibility(View.VISIBLE);
				holder.galleryCount.setText(Integer.toString(galleryItem.getBucketCount()));
			}
			else
				holder.galleryCount.setVisibility(View.GONE);
		}
		else
		{
			(convertView.findViewById(R.id.album_layout)).setVisibility(View.GONE);
			holder.galleryName.setVisibility(View.GONE);
			holder.galleryCount.setVisibility(View.GONE);
		}
		if (galleryItem != null)
		{
			if (galleryItem.getType() == GalleryItem.CUSTOM)
			{
				holder.galleryThumb.setScaleType(ScaleType.CENTER_INSIDE);
				holder.contentLayout.addView(LayoutInflater.from(HikeMessengerApp.getInstance().getApplicationContext()).inflate(
						Utils.getLayoutIdFromName(galleryItem.getLayoutIDName()), null));
				holder.contentLayout.setVisibility(View.VISIBLE);
			}
			else
			{
				galleryImageLoader.loadImage(GalleryImageLoader.GALLERY_KEY_PREFIX + galleryItem.getFilePath(), holder.galleryThumb, isListFlinging);
				holder.galleryThumb.setScaleType(ScaleType.CENTER_CROP);
				holder.contentLayout.setVisibility(View.GONE);
			}
			holder.galleryThumb.setAlpha(1f);
		}
		else
		{
			holder.galleryThumb.setScaleType(ScaleType.CENTER_INSIDE);
			holder.galleryThumb.setImageResource(R.drawable.ic_add_more);
			holder.galleryThumb.setAlpha(0.8f);
		}

		if ((selectedGalleryItems != null && selectedGalleryItems.contains(galleryItem)) || selectedItemPostion == position)
		{
			holder.selected.setSelected(true);
		}
		else
		{
			holder.selected.setSelected(false);
		}

		return convertView;
	}

	private class ViewHolder
	{
		ImageView galleryThumb;

		TextView galleryName;

		View selected;

		TextView galleryCount;

		ViewGroup contentLayout;
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
	
	public GalleryImageLoader getGalleryImageLoader()
	{
		return galleryImageLoader;
	}

	public void updateGalleryItemList(List<GalleryItem> gItemList)
	{
		galleryItemList = gItemList;
	}
}
