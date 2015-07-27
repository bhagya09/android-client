package com.bsb.hike.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import com.bsb.hike.utils.Logger;
import com.bsb.hike.view.TouchImageView;



public class SharedMediaAdapter extends PagerAdapter implements OnClickListener, SuccessfulImageLoadingListener
{
	private  SharedFileImageLoader sharedMediaLoader;

	private  ArrayList<HikeSharedFile> sharedMediaItemList;

	private Context context;

	private PhotoViewerFragment photoViewerFragment;

	private Handler mHandler;
	
	private static final String TAG = "SharedMediaAdapter";
    
	private boolean initialised;

    private final FragmentManager mFragmentManager;
    
    private FragmentTransaction mCurTransaction = null;

    private ArrayList<ImageViewerFragment.SavedState> mSavedState = new ArrayList<ImageViewerFragment.SavedState>();
    
    private ArrayList<ImageViewerFragment> mFragments = new ArrayList<ImageViewerFragment>();
    
    private ImageViewerFragment initFragment;
    
    private ImageViewerFragment mCurrentPrimaryItem = null;

    public SharedMediaAdapter(FragmentManager fm,Context context, int size_image, ArrayList<HikeSharedFile> sharedMediaItemList, String msisdn, ViewPager viewPager, PhotoViewerFragment photoViewerFragment)
	{
		this(fm);
		this.context = context;
		sharedMediaLoader = new SharedFileImageLoader(context, size_image,false);
		sharedMediaLoader.setDefaultDrawable(context.getResources().getDrawable(R.drawable.ic_file_thumbnail_missing));
		sharedMediaLoader.setCachingEnabled(false);
		sharedMediaLoader.setSuccessfulImageLoadingListener(this);
		this.sharedMediaItemList = sharedMediaItemList;
		this.photoViewerFragment = photoViewerFragment;
		this.mHandler = new Handler(HikeMessengerApp.getInstance().getMainLooper());
	}
    
    public SharedMediaAdapter(FragmentManager fm) {
        mFragmentManager = fm;
    }

    public void clearInitialFragment()
    {
    	initFragment = null;
    	initialised = true;
    }
    
    /**
     * Return the Fragment associated with a specified position.
     */
    @Override
	public int getCount()
	{
		return sharedMediaItemList.size();
	}

	@Override
	public int getItemPosition(Object object)
	{
		return POSITION_NONE;
	}

	
	public ImageViewerFragment getItem (int position)
	{
		return ImageViewerFragment.newInstance(position,sharedMediaLoader,sharedMediaItemList.get(position),this);
	}
	

    @Override
    public void startUpdate(ViewGroup container) {
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        // If we already have this item instantiated, there is nothing
        // to do.  This can happen when we are restoring the entire pager
        // from its saved state, where the fragment manager has already
        // taken care of restoring the fragments we previously had instantiated.
    	Logger.i(TAG,"Institiating : " +position +"/"+ sharedMediaItemList.size());
    	Logger.i(TAG,"Fragments : " +position +"/"+ mFragments.size());
    	
    	if (initFragment != null && initFragment.getPathTag().equals(sharedMediaItemList.get(position).getExactFilePath())) 
    	{
    		Logger.i(TAG,"Match : "+initFragment.getPathTag());
    		return initFragment;
        }
        
    	if (mFragments.size() > position) {
            Fragment f = mFragments.get(position);
            if (f != null) {
                return f;
            }
        }
    	
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        ImageViewerFragment fragment = getItem(position);
        Logger.v(TAG, "Adding item #" + position + ": f=" + fragment);
        if (mSavedState.size() > position) {
        	ImageViewerFragment.SavedState fss = mSavedState.get(position);
            if (fss != null) {
                fragment.setInitialSavedState(fss);
            }
        }
        while (mFragments.size() <= position) {
            mFragments.add(null);
        }
        fragment.setMenuVisibility(false);
        fragment.setUserVisibleHint(false);
        
        if(initFragment == null && !initialised)
        {
        	initFragment = fragment;
        }
        
        mFragments.set(position,fragment);
        mCurTransaction.add(container.getId(), fragment);
        Logger.i(TAG,"Finishing : " +position);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
    	Logger.i(TAG,"Destroy Item called : "+position);
    	ImageViewerFragment fragment = (ImageViewerFragment)object;

    	if( !initialised && initFragment != null && fragment != null && fragment.getPathTag().equals(initFragment.getPathTag()) )
    	{
    		return;
    	}
    	
    	if(position >= mFragments.size())
    	{
    		return;
    	}
    	
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        Logger.v(TAG, "Removing item #" + position + ": f=" + object
                + " v=" + ((ImageViewerFragment)object).getView());
        while (mSavedState.size() <= position) {
            mSavedState.add(null);
        }
        mSavedState.set(position, mFragmentManager.saveFragmentInstanceState(fragment));
        mFragments.set(position, null);

        mCurTransaction.remove(fragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
    	ImageViewerFragment fragment = (ImageViewerFragment)object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((ImageViewerFragment)object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        Bundle state = null;
        if (mSavedState.size() > 0) {
            state = new Bundle();
            ImageViewerFragment.SavedState[] fss = new ImageViewerFragment.SavedState[mSavedState.size()];
            mSavedState.toArray(fss);
            state.putParcelableArray("states", fss);
        }
        for (int i=0; i<mFragments.size(); i++) {
        	ImageViewerFragment f = mFragments.get(i);
            if (f != null && f.isAdded()) {
                if (state == null) {
                    state = new Bundle();
                }
                String key = "f" + i;
                mFragmentManager.putFragment(state, key, f);
            }
        }
        return state;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
    	Logger.i(TAG,"Restore State called : clearing fragment");
        if (state != null) {
            Bundle bundle = (Bundle)state;
            bundle.setClassLoader(loader);
            Parcelable[] fss = bundle.getParcelableArray("states");
            mSavedState.clear();
            mFragments.clear();
            if (fss != null) {
                for (int i=0; i<fss.length; i++) {
                    mSavedState.add((Fragment.SavedState)fss[i]);
                }
            }
            Iterable<String> keys = bundle.keySet();
            for (String key: keys) {
                if (key.startsWith("f")) {
                    int index = Integer.parseInt(key.substring(1));
                    ImageViewerFragment f = (ImageViewerFragment) mFragmentManager.getFragment(bundle, key);
                    if (f != null) {
                        while (mFragments.size() <= index) {
                            mFragments.add(null);
                        }
                        f.setMenuVisibility(false);
                        mFragments.set(index, f);
                    } else {
                        Logger.w(TAG, "Bad fragment at key " + key);
                    }
                }
            }
        }
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
	
	public static class ImageViewerFragment extends Fragment {

		private String pathTag;
		
		private OnClickListener mListener;
		
		private HikeSharedFile mFile;
		
		private SharedFileImageLoader mLoader;
		
		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static ImageViewerFragment newInstance(int position,SharedFileImageLoader loader,HikeSharedFile file,OnClickListener listener) {

			ImageViewerFragment fragment = new ImageViewerFragment();
			fragment.setPathTag(file.getExactFilePath());
			fragment.mListener = listener;
			fragment.mLoader = loader;
			fragment.mFile = file;
			return fragment;
		}

		public ImageViewerFragment() {
		}
		

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
			
			View argView = inflater.inflate(R.layout.gallery_layout_item, container, false);
			
			TouchImageView galleryImageView = (TouchImageView) argView.findViewById(R.id.album_image);
			ImageView videPlayButton = (ImageView) argView.findViewById(R.id.play_media);
			ProgressBar progressBar = (ProgressBar) argView.findViewById(R.id.progress_bar);
			galleryImageView.setZoom(1.0f);
			galleryImageView.setScaleType(ScaleType.FIT_CENTER);
			
			progressBar.setVisibility(View.VISIBLE);
			videPlayButton.setVisibility(View.VISIBLE);
			galleryImageView.setVisibility(View.VISIBLE);
			argView.findViewById(R.id.file_missing_layout).setVisibility(View.GONE);

			if (mFile.getHikeFileType() == HikeFileType.VIDEO)
			{
				progressBar.setVisibility(View.GONE);
				videPlayButton.setVisibility(View.VISIBLE);
			}
			else
			{
				videPlayButton.setVisibility(View.GONE);
			}

			if (mFile.exactFilePathFileExists())
			{
				mLoader.loadImage(mFile.getImageLoaderKey(true), galleryImageView);
			}
			else
			{
				progressBar.setVisibility(View.GONE);
				videPlayButton.setVisibility(View.GONE);
				galleryImageView.setVisibility(View.GONE);
				argView.findViewById(R.id.file_missing_layout).setVisibility(View.VISIBLE);
			}

			galleryImageView.setTag(mFile);
			galleryImageView.setOnClickListener(mListener);
			return argView;

		}

		public String getPathTag() {
			return pathTag;
		}

		public void setPathTag(String pathTag) {
			Logger.i(TAG,"Setting Tag : " +pathTag);
			this.pathTag = pathTag;
		}
	}


}