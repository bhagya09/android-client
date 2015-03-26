package com.bsb.hike.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;

import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;
import com.bsb.hike.ui.fragments.PhotoViewerFragment;
import com.bsb.hike.view.TouchImageView;

public class SharedMediaAdapter extends PagerAdapter implements OnClickListener
{
	private LayoutInflater layoutInflater;

	private SharedFileImageLoader sharedMediaLoader;

	private ArrayList<HikeSharedFile> sharedMediaItems;

	private Context context;

	private PhotoViewerFragment photoViewerFragment;
	
	public SharedMediaAdapter(Context context, int size_image, ArrayList<HikeSharedFile> sharedMediaItems, String msisdn, ViewPager viewPager, PhotoViewerFragment photoViewerFragment)
	{
		this.context = context;
		this.layoutInflater = LayoutInflater.from(this.context);
		this.sharedMediaLoader = new SharedFileImageLoader(context, size_image);
		sharedMediaLoader.setDefaultDrawable(context.getResources().getDrawable(R.drawable.ic_file_thumbnail_missing));
		this.sharedMediaItems = sharedMediaItems;
		this.photoViewerFragment = photoViewerFragment;
	}

	@Override
	public int getCount()
	{
		// TODO Auto-generated method stub
		return sharedMediaItems.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object)
	{
		// TODO Auto-generated method stub
		return view == object;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object)
	{
		
		((ViewPager) container).removeView((View) object);
	}

	@Override
	public int getItemPosition(Object object)
	{
		return POSITION_NONE;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		View page = layoutInflater.inflate(R.layout.gallery_layout_item, container, false);
		final HikeSharedFile sharedMediaItem = sharedMediaItems.get(position);

		TouchImageView galleryImageView = (TouchImageView) page.findViewById(R.id.album_image);
		ImageView videPlayButton = (ImageView)  page.findViewById(R.id.play_media);
		ProgressBar progressBar = (ProgressBar)  page.findViewById(R.id.progress_bar);
		galleryImageView.setZoom(1.0f);
		galleryImageView.setScaleType(ScaleType.FIT_CENTER);
		
		if (sharedMediaItem.getHikeFileType() == HikeFileType.VIDEO)
		{
			progressBar.setVisibility(View.GONE);
			videPlayButton.setVisibility(View.VISIBLE);
		}
		else
		{
			videPlayButton.setVisibility(View.GONE);
		}

		if(sharedMediaItem.exactFilePathFileExists())
		{
			sharedMediaLoader.loadImage(sharedMediaItem.getImageLoaderKey(true), galleryImageView);
		}
		else
		{
			progressBar.setVisibility(View.GONE);
			videPlayButton.setVisibility(View.GONE);
			galleryImageView.setVisibility(View.GONE);
			page.findViewById(R.id.file_missing_layout).setVisibility(View.VISIBLE);
		}
		
		galleryImageView.setTag(sharedMediaItem);
		galleryImageView.setOnClickListener(this);
		((ViewPager) container).addView(page);
		return page;
	}

	@Override
	public void onClick(View v)
	{
		HikeSharedFile sharedMediaItem = (HikeSharedFile) v.getTag();
		switch (sharedMediaItem.getHikeFileType())
		{
		case IMAGE:
			photoViewerFragment.toggleViewsVisibility();
			break;
		case VIDEO:
			HikeSharedFile.openFile(sharedMediaItem, context);
			break;
		default:
			break;
		}
	}
	
	public SharedFileImageLoader getSharedFileImageLoader()
	{
		return sharedMediaLoader;
	}
}