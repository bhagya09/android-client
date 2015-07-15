package com.bsb.hike.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.smartImageLoader.ImageWorker.SuccessfulImageLoadingListener;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;
import com.bsb.hike.ui.fragments.PhotoViewerFragment;
import com.bsb.hike.view.TouchImageView;

public class SharedMediaAdapter extends PagerAdapter implements OnClickListener, SuccessfulImageLoadingListener
{
	private LayoutInflater layoutInflater;

	private SharedFileImageLoader sharedMediaLoader;

	private ArrayList<HikeSharedFile> sharedMediaItems;

	private Context context;

	private PhotoViewerFragment photoViewerFragment;

	private Handler mHandler;
	
	public SharedMediaAdapter(Context context, int size_image, ArrayList<HikeSharedFile> sharedMediaItems, String msisdn, ViewPager viewPager, PhotoViewerFragment photoViewerFragment)
	{
		this.context = context;
		this.layoutInflater = LayoutInflater.from(this.context);
		this.sharedMediaLoader = new SharedFileImageLoader(context, size_image);
		sharedMediaLoader.setDefaultDrawable(context.getResources().getDrawable(R.drawable.ic_file_thumbnail_missing));
		sharedMediaLoader.setImageToBeCached(false);
		sharedMediaLoader.setSuccessfulImageLoadingListener(this);
		this.sharedMediaItems = sharedMediaItems;
		this.photoViewerFragment = photoViewerFragment;
		this.mHandler = new Handler(HikeMessengerApp.getInstance().getMainLooper());
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
		bindView(page, position);
		((ViewPager) container).addView(page);
		return page;
	}

	public View bindView(View argView, int position)
	{
		final HikeSharedFile sharedMediaItem = sharedMediaItems.get(position);

		TouchImageView galleryImageView = (TouchImageView) argView.findViewById(R.id.album_image);
		ImageView videPlayButton = (ImageView) argView.findViewById(R.id.play_media);
		ProgressBar progressBar = (ProgressBar) argView.findViewById(R.id.progress_bar);
		galleryImageView.setZoom(1.0f);
		galleryImageView.setScaleType(ScaleType.FIT_CENTER);
		
		progressBar.setVisibility(View.VISIBLE);
		videPlayButton.setVisibility(View.VISIBLE);
		galleryImageView.setVisibility(View.VISIBLE);
		argView.findViewById(R.id.file_missing_layout).setVisibility(View.GONE);

		if (sharedMediaItem.getHikeFileType() == HikeFileType.VIDEO)
		{
			progressBar.setVisibility(View.GONE);
			videPlayButton.setVisibility(View.VISIBLE);
		}
		else
		{
			videPlayButton.setVisibility(View.GONE);
		}

		if (sharedMediaItem.exactFilePathFileExists())
		{
			sharedMediaLoader.loadImage(sharedMediaItem.getImageLoaderKey(true), galleryImageView);
		}
		else
		{
			progressBar.setVisibility(View.GONE);
			videPlayButton.setVisibility(View.GONE);
			galleryImageView.setVisibility(View.GONE);
			argView.findViewById(R.id.file_missing_layout).setVisibility(View.VISIBLE);
		}

		galleryImageView.setTag(sharedMediaItem);
		galleryImageView.setOnClickListener(SharedMediaAdapter.this);
		return argView;
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
	
	/**
	 * This is done via post runnable so that this "removing loader"
	 * gets queued into loop and is performed after image is shown in view pager
	 * 
	 * Note:- Doing directly without post via Runnable, first loader is removed and
	 * then image is shown to user, so there is no loader seen or there is black screen shown
	 */
	@Override
	public void onSuccessfulImageLoaded(final ImageView imageView)
	{
		if(photoViewerFragment.isAdded())
		{
			RemoveLoaderRunnable removeLoaderRunnable = new RemoveLoaderRunnable(imageView);
			mHandler.post(removeLoaderRunnable);
		}
		
	}
	
	public static class RemoveLoaderRunnable implements Runnable
	{
		private ImageView imageView;
		
		public RemoveLoaderRunnable(ImageView imageView)
		{
			this.imageView = imageView;
		}
		
		@Override
		public void run()
		{
			if(imageView == null)
			{
				return;
			}
			
			View parent = imageView.getRootView();
			
			if(parent != null && parent.findViewById(R.id.progress_bar) != null)
			{
				parent.findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);
			}
		}
		
	}

	// To remove any Callbacks in Handler
	public void onDestroy()
	{
		if(mHandler!=null)
		{
			mHandler.removeCallbacksAndMessages(null);
		}
	}
	
	public SharedFileImageLoader getSharedFileImageLoader()
	{
		return sharedMediaLoader;
	}

}