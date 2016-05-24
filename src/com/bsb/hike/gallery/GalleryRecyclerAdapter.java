package com.bsb.hike.gallery;

import java.util.List;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.smartImageLoader.GalleryImageLoader;
import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout.LayoutParams;

public class GalleryRecyclerAdapter extends Adapter<GalleryViewHolder> {

	private List<GalleryItem> galleryItemList;

	private LayoutInflater layoutInflater;

	private GalleryImageLoader galleryImageLoader;

	private boolean isListFlinging;

	private boolean isInsideAlbum;

	private int sizeOfImage;

	private List<GalleryItem> selectedGalleryItems;

	private int selectedItemPostion = -1;

	private boolean selectedScreen = false;

	public GalleryRecyclerAdapter(Context context, List<GalleryItem> galleryItems, boolean isInsideAlbum, int sizeOfImage, List<GalleryItem> selectedItems, 
			boolean selectedScreen)
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
	public int getItemCount() {
		return galleryItemList.size();
	}

	@Override
	public void onBindViewHolder(GalleryViewHolder holder, int position) 
	{
		GalleryItem galleryItem = getItem(position);
		if (!isInsideAlbum && galleryItem.getType()!=GalleryItem.CUSTOM)
		{
			holder.albumLayout.setVisibility(View.VISIBLE);
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
			holder.albumLayout.setVisibility(View.GONE);
			holder.galleryName.setVisibility(View.GONE);
			holder.galleryCount.setVisibility(View.GONE);
		}

		if (galleryItem != null)
		{
			if (galleryItem.getType() == GalleryItem.CUSTOM)
			{
				holder.galleryThumb.setScaleType(ScaleType.CENTER_INSIDE);
				holder.contentLayout.removeAllViews();
				holder.contentLayout.addView(LayoutInflater.from(HikeMessengerApp.getInstance().getApplicationContext()).inflate(
						Utils.getLayoutIdFromName(galleryItem.getLayoutIDName()), null));
				holder.contentLayout.setVisibility(View.VISIBLE);
			}
			else
			{
				holder.galleryThumb.setImageDrawable(null);
				galleryImageLoader.loadImage(GalleryImageLoader.GALLERY_KEY_PREFIX + galleryItem.getFilePath(), holder.galleryThumb, isListFlinging);
				holder.galleryThumb.setScaleType(ScaleType.CENTER_CROP);
				holder.contentLayout.setVisibility(View.GONE);
			}
		}
		else
		{
			holder.galleryThumb.setScaleType(ScaleType.CENTER_INSIDE);
			holder.galleryThumb.setImageResource(R.drawable.ic_add_more);
		}

		if ((selectedGalleryItems != null && selectedGalleryItems.contains(galleryItem)) || selectedItemPostion == position)
		{
			holder.selected.setSelected(true);
		}
		else
		{
			holder.selected.setSelected(false);
		}
	}

	@Override
	public GalleryViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View view = layoutInflater.inflate(R.layout.gallery_item, null);
		GalleryViewHolder holder = new GalleryViewHolder(view);

		holder.selected.setBackgroundResource(selectedScreen ? R.drawable.gallery_item_selected_selector : R.drawable.gallery_item_selector);
		LayoutParams layoutParams = new LayoutParams(sizeOfImage, sizeOfImage);
		holder.galleryThumb.setLayoutParams(layoutParams);

		holder.contentLayout.setLayoutParams(layoutParams);

		return holder;
	}

	public GalleryItem getItem(int position)
	{
		return galleryItemList.get(position);
	}

	public int addItem(GalleryItem galleryItem)
	{
		galleryItemList.add(galleryItem);
		return (galleryItemList.size() - 1);
	}

	public GalleryImageLoader getGalleryImageLoader()
	{
		return galleryImageLoader;
	}
}
