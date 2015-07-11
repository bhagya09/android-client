package com.bsb.hike.ui.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.utils.HikeUiHandler;
import com.bsb.hike.utils.HikeUiHandler.IHandlerCallback;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.utils.Utils;

public class ImageViewerFragment extends SherlockFragment implements OnClickListener, Listener, IHandlerCallback, HeadLessImageWorkerFragment.TaskCallbacks
{
	ImageView imageView;

	private ProgressDialog mDialog;

	private String mappedId;

	private String key;

	private boolean isStatusImage;

	private int imageSize;
	
	public static final int FROM_PROFILE_ACTIVITY = 1;

	public static final int FROM_SETTINGS_ACTIVITY = 2;
	
	private int whichActivity;

	private DisplayPictureEditListener mProfilePhotoEditListener;
	
	private String[] profilePicPubSubListeners = { HikePubSub.ICON_CHANGED};
	
	private boolean isViewEditable = false;
	
	private ProfileImageLoader profileImageLoader;
	
	private HeadLessImageWorkerFragment mImageWorkerFragment;
	
	private boolean hasCustomImage;
	
	private HikeUiHandler hikeUiHandler;
	
	private Runnable failedRunnable = new Runnable()
	{
		
		@Override
		public void run()
		{
			if(isAdded() && isVisible())
			{
				Logger.d("dp_download", "inside ImageViewerFragment, onFailed Recv");
				dismissProgressDialog();
				Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
			}
			removeHeadLessFragment();
		}
	};
	
	private Runnable cancelledRunnable = new Runnable()
	{
		
		@Override
		public void run()
		{
			if(isAdded() && isVisible())
			{
				Logger.d("dp_download", "inside ImageViewerFragment, onCancelled Recv");
				dismissProgressDialog();
				Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
			}
			removeHeadLessFragment();
		}
	};
	
	private Runnable successRunnable = new Runnable()
	{
		
		@Override
		public void run()
		{
			if(isAdded() && isVisible())
			{
				Logger.d("dp_download", "inside ImageViewerFragment, onSucecess Recv");
				dismissProgressDialog();
				profileImageLoader.loadFromFile();
			}
			removeHeadLessFragment();
		}
	};

	/**
	 * Default constructor
	 */
	public ImageViewerFragment() 
	{
	}
		
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		HikeMessengerApp.getPubSub().addListeners(this, profilePicPubSubListeners);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.image_viewer, null);
		imageView = (ImageView) parent.findViewById(R.id.image);
		imageView.setOnClickListener(this);
		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mappedId = getArguments().getString(HikeConstants.Extras.MAPPED_ID);

		isStatusImage = getArguments().getBoolean(HikeConstants.Extras.IS_STATUS_IMAGE);

		imageSize = this.getActivity().getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);
		
		hikeUiHandler = new HikeUiHandler(this);
						
		showImage();
	}

	private void showImage() 
	{
		key = mappedId;
		
		if (!isStatusImage)
		{
			int idx = key.lastIndexOf(ProfileActivity.PROFILE_PIC_SUFFIX);
			
			if (idx > 0)
			{
				key = new String(key.substring(0, idx));
			}
		}
		
		hasCustomImage = isStatusImage || ContactManager.getInstance().hasIcon(key);
		
		profileImageLoader = new ProfileImageLoader(getActivity(), key, imageView, imageSize, isStatusImage, true);
		profileImageLoader.setLoaderListener(new ProfileImageLoader.LoaderListener() {

			@Override
			public void onLoaderReset(Loader<Boolean> arg0) {
				dismissProgressDialog();
			}

			@Override
			public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1) {
				dismissProgressDialog();
				if (isStatusImage)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, null);
				}
			}

			@Override
			public Loader<Boolean> onCreateLoader(int arg0, Bundle arg1) {
				showProgressDialog();
				return null;
			}

			@Override
			public void startDownloading()
			{
				showProgressDialog();
				loadHeadLessImageDownloadingFragment();
			}
		});
		profileImageLoader.loadProfileImage(getLoaderManager());
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		dismissProgressDialog();
		HikeMessengerApp.getPubSub().removeListeners(this, profilePicPubSubListeners);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		menu.clear();			

		if(isViewEditable)
		{
			inflater.inflate(R.menu.edit_dp, menu);			
		}	
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch(item.getItemId())
		{
			case R.id.edit_dp:
				if(mProfilePhotoEditListener != null)
				{
					mProfilePhotoEditListener.onDisplayPictureEditClicked(whichActivity);
				}
			break;
		}
		return true;
	}

	@Override
	public void onAttach(Activity activity) 
	{
		super.onAttach(activity);
		
		if(activity instanceof SettingsActivity)
		{
			whichActivity = FROM_SETTINGS_ACTIVITY;
		}
		else if(activity instanceof ProfileActivity)
		{
			whichActivity = FROM_PROFILE_ACTIVITY;
		}					

		isViewEditable = getArguments().getBoolean(HikeConstants.CAN_EDIT_DP);

		if(isViewEditable)
		{
			// activity should implement DisplayPictureEditListener interface
			try 
			{
	            mProfilePhotoEditListener = (DisplayPictureEditListener) activity;            
	        }
			catch (ClassCastException e) 
			{
	            throw new ClassCastException(activity.toString() + " must implement DisplayPictureEditListener");
	        }
		}
	}

	private void dismissProgressDialog()
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
	}

	private void showProgressDialog()
	{
		mDialog = ProgressDialog.show(getActivity(), null, getResources().getString(R.string.downloading_image));
		mDialog.setCancelable(true);
	}
	
	@Override
	public void onClick(View v)
	{
		/*
		 * This object can become null, if the method is called when the fragment is not attached with the activity. In that case we do nothing and return.
		 */
		if (getActivity() == null)
		{
			return;
		}
		getActivity().onBackPressed();
	}
	
	public interface DisplayPictureEditListener
	{
		public void onDisplayPictureEditClicked(int fromWhichActivity);
	}

	@Override
	public void onEventReceived(String type, Object object) 
	{
		/**
		 * If fragment is not added, returning it
		 */
		if (!isAdded())
		{
			return;
		}
		
		if (HikePubSub.ICON_CHANGED.equals(type))
		{
			ContactInfo contactInfo = Utils.getUserContactInfo(getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0));

			if (contactInfo.getMsisdn().equals((String) object))
			{
				getActivity().runOnUiThread(new Runnable()
				{
					
					@Override
					public void run() 
					{
						showImage();						
					}
				});
			}
		}		
	}	
	
	public void onProgressUpdate(float percent)
	{
		
	}

	public void onCancelled()
	{
		hikeUiHandler.post(cancelledRunnable);
	}

	public void onSuccess(Response result)
	{
		hikeUiHandler.post(successRunnable);
	}

	public void onFailed()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_IMAGE_NOT_DOWNLOADED, key);
		hikeUiHandler.post(failedRunnable);
	}

	/*//TODO API Duplicated, As currently used in loader as well will be removed
	//When loader will be removed from Voip as well
	private void loadFromFile() 
	{
		Logger.d("dp_download", "inside ImageViewerFragment, loadFromFile Recv, "+ Thread.currentThread().getName());
		Logger.e("dp_test", "============ loadFromFile ============");
		String fileName = Utils.getProfileImageFileName(key);

		String basePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		
		File file = new File(basePath, fileName);

		BitmapDrawable drawable = null;
		if (file.exists() && isAdded())
		{
			Logger.d(getClass().getSimpleName(),"setting final downloaded image...");
			drawable = HikeBitmapFactory.getBitmapDrawable(getActivity().getResources(),
					HikeBitmapFactory.scaleDownBitmap(basePath + "/" + fileName, imageSize, imageSize, Bitmap.Config.RGB_565,true,false));
			imageView.setImageDrawable(drawable);
			
			Logger.d(getClass().getSimpleName(), "Putting in cache mappedId : " + mappedId);
			
			 * Putting downloaded image bitmap in cache.
			 
			if (drawable != null)
			{
				HikeMessengerApp.getLruCache().putInCache(mappedId, drawable);
			}
		}
	}*/
	
	private void loadHeadLessImageDownloadingFragment()
	{
		Logger.d("dp_download", "isnide API loadHeadLessImageDownloadingFragment");
		FragmentManager fm = getFragmentManager();
		mImageWorkerFragment = (HeadLessImageDownloaderFragment) fm.findFragmentByTag(HikeConstants.TAG_HEADLESS_IMAGE_DOWNLOAD_FRAGMENT);

	    // If the Fragment is non-null, then it is currently being
	    // retained across a configuration change.
	    if (mImageWorkerFragment == null) 
	    {
	    	Logger.d("dp_download", "starting new mImageLoaderFragment");
	    	String fileName = Utils.getProfileImageFileName(key);
	    	mImageWorkerFragment = HeadLessImageDownloaderFragment.newInstance(key, fileName, hasCustomImage, isStatusImage, null, null, null, true);
	    	mImageWorkerFragment.setTaskCallbacks(this);
	        fm.beginTransaction().add(mImageWorkerFragment, HikeConstants.TAG_HEADLESS_IMAGE_DOWNLOAD_FRAGMENT).commit();
	    }
	    else
	    {
	    	Logger.d("dp_download", "As mImageLoaderFragment already there, so not starting new one");
	    }

	}
	
	private void removeHeadLessFragment()
	{
		Logger.d("dp_download", "inside ImageViewerFragment, removing UILessFragment");
		if(getFragmentManager().findFragmentByTag(HikeConstants.TAG_HEADLESS_IMAGE_DOWNLOAD_FRAGMENT) != null)
		{
			mImageWorkerFragment = (HeadLessImageDownloaderFragment)getFragmentManager().findFragmentByTag(HikeConstants.TAG_HEADLESS_IMAGE_DOWNLOAD_FRAGMENT);
			getFragmentManager().beginTransaction().remove(mImageWorkerFragment).commit();
		}
	}

	@Override
	public void handleUIMessage(Message msg)
	{
		
	}
}
