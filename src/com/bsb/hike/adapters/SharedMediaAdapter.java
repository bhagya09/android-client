package com.bsb.hike.adapters;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
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
import com.bsb.hike.media.SharedMediaCursorIterator;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.smartImageLoader.ImageWorker.SuccessfulImageLoadingListener;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;
import com.bsb.hike.ui.fragments.PhotoViewerFragment;
import com.bsb.hike.view.TouchImageView;

public class SharedMediaAdapter extends CursorPagerAdapter implements OnClickListener, SuccessfulImageLoadingListener
{
	private LayoutInflater layoutInflater;

	private SharedFileImageLoader sharedMediaLoader;

	private Context context;

	private PhotoViewerFragment photoViewerFragment;

	private SharedMediaCursorIterator sharedMediaCursorIter;

	private Handler mHandler;
	
	public SharedMediaAdapter(Context context, int size_image, Cursor sharedMediaCursor, String msisdn, ViewPager viewPager, PhotoViewerFragment photoViewerFragment, SharedMediaCursorIterator smIter)
	{
		super(context, sharedMediaCursor, true);
		this.context = context;
		this.layoutInflater = LayoutInflater.from(this.context);
		this.sharedMediaLoader = new SharedFileImageLoader(context, size_image);
		this.sharedMediaCursorIter = smIter;
		sharedMediaLoader.setDefaultDrawable(context.getResources().getDrawable(R.drawable.ic_file_thumbnail_missing));
		sharedMediaLoader.setImageToBeCached(false);
		sharedMediaLoader.setSuccessfulImageLoadingListener(this);
		this.photoViewerFragment = photoViewerFragment;
		this.mHandler = new Handler(HikeMessengerApp.getInstance().getMainLooper());
	}

	@Override
	public boolean isViewFromObject(View view, Object object)
	{
		return view == object;
	}

	@Override
	public int getItemPosition(Object object)
	{
		return POSITION_NONE;
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

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		return layoutInflater.inflate(R.layout.gallery_layout_item, parent, false);
	}

	@Override
	public void bindView(View page, Context context, Cursor cursor)
	{
		final HikeSharedFile currentSharedMediaItem = sharedMediaCursorIter.getFromCursor(cursor);

		TouchImageView galleryImageView = (TouchImageView) page.findViewById(R.id.album_image);
		ImageView videPlayButton = (ImageView)  page.findViewById(R.id.play_media);
		ProgressBar progressBar = (ProgressBar)  page.findViewById(R.id.progress_bar);
		galleryImageView.setZoom(1.0f);
		galleryImageView.setScaleType(ScaleType.FIT_CENTER);
		
		if (currentSharedMediaItem.getHikeFileType() == HikeFileType.VIDEO)
		{
			progressBar.setVisibility(View.GONE);
			videPlayButton.setVisibility(View.VISIBLE);
		}
		else
		{
			videPlayButton.setVisibility(View.GONE);
		}

		if (currentSharedMediaItem.exactFilePathFileExists())
		{
			sharedMediaLoader.loadImage(currentSharedMediaItem.getImageLoaderKey(true), galleryImageView);
		}
		else
		{
			progressBar.setVisibility(View.GONE);
			videPlayButton.setVisibility(View.GONE);
			galleryImageView.setVisibility(View.GONE);
			page.findViewById(R.id.file_missing_layout).setVisibility(View.VISIBLE);
		}

		galleryImageView.setTag(currentSharedMediaItem);
		galleryImageView.setOnClickListener(this);
		
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
}