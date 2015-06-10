package com.bsb.hike.adapters;

import java.util.HashSet;
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

import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;

public class HikeSharedFileAdapter extends BaseAdapter
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
	public int getCount()
	{
		return sharedFilesList.size();
	}

	@Override
	public HikeSharedFile getItem(int position)
	{
		return sharedFilesList.get(position);
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
		HikeSharedFile galleryItem = getItem(position);
		ViewHolder holder;

		if (convertView == null)
		{
			convertView = layoutInflater.inflate(R.layout.gallery_item, null);
			holder = new ViewHolder();

			holder.galleryName = (TextView) convertView.findViewById(R.id.album_title);
			holder.galleryThumb = (ImageView) convertView.findViewById(R.id.album_image);
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

		holder.galleryName.setVisibility(View.GONE);
		if (galleryItem != null)
		{	View time_view = convertView.findViewById(R.id.vid_time_layout);
			holder.galleryThumb.setImageDrawable(null);
			if(galleryItem.exactFilePathFileExists())
			{
				thumbnailLoader.loadImage(galleryItem.getImageLoaderKey(false), holder.galleryThumb, isListFlinging);
				holder.galleryThumb.setScaleType(ScaleType.CENTER_CROP);
				
				if (galleryItem.getHikeFileType() == HikeFileType.VIDEO)
				{
					time_view.setVisibility(View.VISIBLE);
				}
				else
				{
					time_view.setVisibility(View.GONE);
				}
			}
			else
			{
				time_view.setVisibility(View.GONE);
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

		return convertView;
	}

	private class ViewHolder
	{
		ImageView galleryThumb;

		TextView galleryName;

		View selected;
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
	
	public SharedFileImageLoader getSharedFileImageLoader()
	{
		return thumbnailLoader;
	}
}
