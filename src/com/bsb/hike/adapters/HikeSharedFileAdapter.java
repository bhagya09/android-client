package com.bsb.hike.adapters;

import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;

public class HikeSharedFileAdapter extends RecyclerView.Adapter<HikeSharedFileAdapter.SharedMediaViewHolder>
{

	private List<HikeSharedFile> sharedFilesList;

	private LayoutInflater layoutInflater;

	private SharedFileImageLoader thumbnailLoader;

	private boolean isListFlinging;

	private int sizeOfImage;

	private HashSet<Long> selectedItems;

	private int selectedItemPostion = -1;

	private boolean selectedScreen = false;

	public static String IMAGE_TAG = "image";
	
	private Context context;
	
	public HikeSharedFileAdapter(Context context, List<HikeSharedFile> sharedFilesList, int sizeOfImage, HashSet<Long> selectedItems, boolean selectedScreen)
	{
		this.context = context;
		this.layoutInflater = LayoutInflater.from(context);
		this.sharedFilesList = sharedFilesList;
		this.sizeOfImage = sizeOfImage;
		this.selectedItems = selectedItems;
		this.selectedScreen = selectedScreen;

		this.thumbnailLoader = new SharedFileImageLoader(context, sizeOfImage);
		this.thumbnailLoader.setDontSetBackground(true);
		thumbnailLoader.setDefaultDrawable(context.getResources().getDrawable(R.drawable.ic_file_thumbnail_missing));
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public int getItemCount() {
		return sharedFilesList.size();
	}

	public HikeSharedFile getItem(int position)
	{
		return sharedFilesList.get(position);
	}

	@Override
	public void onBindViewHolder(SharedMediaViewHolder holder, int position)
	{
		HikeSharedFile galleryItem = getItem(position);
		holder.galleryName.setVisibility(View.GONE);
		if (galleryItem != null)
		{
			holder.galleryThumb.setImageDrawable(null);
			if(galleryItem.exactFilePathFileExists())
			{
				thumbnailLoader.loadImage(galleryItem.getImageLoaderKey(false), holder.galleryThumb, isListFlinging);
				holder.galleryThumb.setScaleType(ScaleType.CENTER_CROP);
				
				if (galleryItem.getHikeFileType() == HikeFileType.VIDEO)
				{
					holder.time_view.setVisibility(View.VISIBLE);
				}
				else
				{
					holder.time_view.setVisibility(View.GONE);
				}
			}
			else
			{
				holder.time_view.setVisibility(View.GONE);
				holder.galleryThumb.setScaleType(ScaleType.CENTER_INSIDE);
				holder.galleryThumb.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_missing));
			}
		}
		else
		{
			holder.galleryThumb.setScaleType(ScaleType.CENTER_INSIDE);
			holder.galleryThumb.setImageResource(R.drawable.ic_add_more);
		}

		if ((selectedItems != null && selectedItems.contains(galleryItem.getMsgId())) || selectedItemPostion == position)
		{
			holder.selected.setSelected(true);
		}
		else
		{
			holder.selected.setSelected(false);
		}
	}

	@Override
	public SharedMediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View view = layoutInflater.inflate(R.layout.gallery_item, parent, false);
		SharedMediaViewHolder holder = new SharedMediaViewHolder(view);

		holder.selected.setBackgroundResource(selectedScreen ? R.drawable.gallery_item_selected_selector : R.drawable.gallery_item_selector);
		LayoutParams layoutParams = new LayoutParams(sizeOfImage, sizeOfImage);
		holder.galleryThumb.setLayoutParams(layoutParams);
		return holder;
	}

	public SharedFileImageLoader getSharedFileImageLoader()
	{
		return thumbnailLoader;
	}

	public int removeSharedFile(HikeSharedFile sharedFile)
	{
		int position = sharedFilesList.indexOf(sharedFile);
		sharedFilesList.remove(sharedFile);
		return position;
	}

	class SharedMediaViewHolder extends RecyclerView.ViewHolder
	{
		public ImageView galleryThumb;
		public TextView galleryName;
		public View selected;
		public View time_view;

		public SharedMediaViewHolder(View view) {
			super(view);
			galleryName = (TextView) view.findViewById(R.id.album_title);
			galleryThumb = (ImageView) view.findViewById(R.id.album_image);
			selected = view.findViewById(R.id.selected);
			time_view = view.findViewById(R.id.vid_time_layout);
		}
	}
}
