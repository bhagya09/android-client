package com.bsb.hike.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
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

import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.smartImageLoader.SharedFileImageLoader;
import com.bsb.hike.ui.fragments.PhotoViewerFragment;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.view.TouchImageView;



public class SharedMediaAdapter extends PagerAdapter
{
	private  ArrayList<HikeSharedFile> sharedMediaItemList;

	private static final String TAG = "SharedMediaAdapter";
    
    private final FragmentManager mFragmentManager;
    
    private FragmentTransaction mCurTransaction = null;

    private ArrayList<SharedMediaCustomFragment.SavedState> mSavedState = new ArrayList<SharedMediaCustomFragment.SavedState>();
    
    private ArrayList<SharedMediaCustomFragment> mFragments = new ArrayList<SharedMediaCustomFragment>();
    
    private SharedMediaCustomFragment firstFragment;
    
    private SharedMediaCustomFragment mCurrentPrimaryItem = null;

    public SharedMediaAdapter(FragmentManager fm,Context context, int size_image, ArrayList<HikeSharedFile> sharedMediaItemList, String msisdn, ViewPager viewPager, PhotoViewerFragment photoViewerFragment)
	{
		this(fm);
		this.sharedMediaItemList = sharedMediaItemList;
	}
    
    public SharedMediaAdapter(FragmentManager fm) {
        mFragmentManager = fm;
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

	
	public SharedMediaCustomFragment getItem (int position)
	{
		return SharedMediaCustomFragment.newInstance(position,sharedMediaItemList.get(position));
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
    	
    	if (mFragments.size() > position) {
    		SharedMediaCustomFragment f = mFragments.get(position);
            if (f != null && f.getPathTag().equals(sharedMediaItemList.get(position).getExactFilePath()))
            {
            	Logger.i(TAG,"Match : "+f.getPathTag());
                return f;
            }
        }
    	
    	SharedMediaCustomFragment fragment = null;
    	
    	if (firstFragment != null && firstFragment.getPathTag().equals(sharedMediaItemList.get(position).getExactFilePath())) 
    	{
    		fragment = firstFragment;
    		Logger.d(TAG,"First Fragment found)");
    		if(fragment.isAdded())
    		{
    			Logger.d(TAG,"First fragment already added");
    			return fragment;
    		}
    		
        }
        

    	if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        if(fragment == null)
        {
        	fragment = getItem(position);
        }
        
        Logger.v(TAG, "Adding item #" + position + ": f=" + fragment);
        if (mSavedState.size() > position) {
        	SharedMediaCustomFragment.SavedState fss = mSavedState.get(position);
            if (fss != null) {
                fragment.setInitialSavedState(fss);
            }
        }
        while (mFragments.size() <= position) {
            mFragments.add(null);
        }
        fragment.setMenuVisibility(false);
        fragment.setUserVisibleHint(false);
        
        if(firstFragment == null)
        {
        	firstFragment = fragment;
        }
        
        mFragments.set(position,fragment);
        mCurTransaction.add(container.getId(), fragment);
        Logger.i(TAG,"Finishing : " +position);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
    	Logger.i(TAG,"Destroy Item called : "+position);
    	SharedMediaCustomFragment fragment = (SharedMediaCustomFragment)object;

    	
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        Logger.v(TAG, "Removing item #" + position + ": f=" + object
                + " v=" + ((SharedMediaCustomFragment)object).getView());
        while (mSavedState.size() <= position) {
            mSavedState.add(null);
        }
        mSavedState.set(position, mFragmentManager.saveFragmentInstanceState(fragment));
        mFragments.set(position, null);

        mCurTransaction.remove(fragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
    	SharedMediaCustomFragment fragment = (SharedMediaCustomFragment)object;
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
        return ((SharedMediaCustomFragment)object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        Bundle state = null;
        if (mSavedState.size() > 0) {
            state = new Bundle();
            SharedMediaCustomFragment.SavedState[] fss = new SharedMediaCustomFragment.SavedState[mSavedState.size()];
            mSavedState.toArray(fss);
            state.putParcelableArray("states", fss);
        }
        for (int i=0; i<mFragments.size(); i++) {
        	SharedMediaCustomFragment f = mFragments.get(i);
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
                    SharedMediaCustomFragment f = (SharedMediaCustomFragment) mFragmentManager.getFragment(bundle, key);
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
    
	public static class SharedMediaCustomFragment extends Fragment
	{

		private String pathTag;
		
		private int pos;
		
		private OnClickListener mListener;
		
		private HikeSharedFile mFile;
		
		private SharedFileImageLoader mLoader;
		
		public static String SHARED_FILE_NAME = "smfile";
		
		public static String SHARED_VIEW_INDEX = "smindex";
		
		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static SharedMediaCustomFragment newInstance(int position,HikeSharedFile file) {

			SharedMediaCustomFragment fragment = new SharedMediaCustomFragment();
			
			Bundle data = new Bundle();
			data.putParcelable(SHARED_FILE_NAME, file);
			data.putInt(SHARED_VIEW_INDEX, position);
			
			fragment.setArguments(data);
			fragment.setPathTag(file.getExactFilePath());
			return fragment;
		}

		public SharedMediaCustomFragment() {
		}
		
		
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			Bundle data = getArguments();
			
			this.mFile = data.getParcelable(SHARED_FILE_NAME);
			this.pos = data.getInt(SHARED_VIEW_INDEX);
			this.setPathTag(mFile.getExactFilePath());
			this.mListener = (PhotoViewerFragment)getParentFragment();
			
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
			
			this.mLoader = ((PhotoViewerFragment)getParentFragment()).getImageLoader();
			
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

			if (mFile.exactFilePathFileExists() && mLoader!=null)
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

		public int getPosition() {
			return pos;
		}

		public void setPosition(int pos) {
			this.pos = pos;
		}
		
	}


}