package com.bsb.hike.ui.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.imageHttp.HikeImageDownloader;
import com.bsb.hike.imageHttp.HikeImageWorker;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.HikeUiHandler;
import com.bsb.hike.utils.HikeUiHandler.IHandlerCallback;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.utils.Utils;

public class ImageViewerFragment extends Fragment implements OnClickListener, Listener, IHandlerCallback, HikeImageWorker.TaskCallbacks
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
	
	private HikeImageDownloader mImageWorkerFragment;
	
	private boolean hasCustomImage;
	
	private HikeUiHandler hikeUiHandler;
	
	private static final String TAG = "dp_download";
	
	private Runnable failedRunnable = new Runnable()
	{
		
		@Override
		public void run()
		{
			if(isAdded() && isVisible())
			{
				Logger.d(TAG, "inside ImageViewerFragment, onFailed Recv");
				dismissProgressDialog();
				Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	private Runnable cancelledRunnable = new Runnable()
	{
		
		@Override
		public void run()
		{
			if(isAdded() && isVisible())
			{
				Logger.d(TAG, "inside ImageViewerFragment, onCancelled Recv");
				dismissProgressDialog();
				Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	private Runnable successRunnable = new Runnable()
	{
		
		@Override
		public void run()
		{
			if(isAdded() && isVisible())
			{
				Logger.d(TAG, "inside ImageViewerFragment, onSucecess Recv");
				dismissProgressDialog();
				profileImageLoader.loadFromFile();
			}
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

		isViewEditable = getArguments().getBoolean(HikeConstants.CAN_EDIT_DP);

		imageSize = this.getActivity().getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);

		key = mappedId;

		if (!isStatusImage) {
			int idx = key.lastIndexOf(ProfileActivity.PROFILE_PIC_SUFFIX);

			if (idx > 0) {
				key = new String(key.substring(0, idx));
			}
		}

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
				beginImageDownload();
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		ContactInfo contactInfo = Utils.getUserContactInfo(true);
		if(isViewEditable
				// check if msisdn is not a group id and if it already has an icon (force check to avoid stale state)
				&& (!OneToNConversationUtils.isOneToNConversation(key) && ContactManager.getInstance().hasIcon(contactInfo.getMsisdn()))) {
			menu.clear();
			inflater.inflate(R.menu.edit_dp, menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch(item.getItemId())
		{
			case R.id.remove_photo:
				if(mProfilePhotoEditListener != null)
				{
					mProfilePhotoEditListener.onDisplayPictureChangeClicked(whichActivity);
				}
			break;
		}
		return true;
	}

	public void setDisplayPictureEditListener (DisplayPictureEditListener listener, int sourceActivity)
	{
		this.mProfilePhotoEditListener = listener;
		this.whichActivity = sourceActivity;
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
		public void onDisplayPictureChangeClicked(int fromWhichActivity);

		public void onDisplayPictureRemoveClicked(int fromWhichActivity);
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

	private void beginImageDownload()
	{
    	Logger.d(TAG, "starting new mImageLoaderFragment");
    	String fileName = Utils.getProfileImageFileName(key);
    	mImageWorkerFragment = HikeImageDownloader.newInstance(key, fileName, hasCustomImage, isStatusImage, null, null, null, true,false);
    	mImageWorkerFragment.setTaskCallbacks(this);
    	mImageWorkerFragment.startLoadingTask();
	}
	
	@Override
	public void handleUIMessage(Message msg)
	{
		
	}

	@Override
	public void onTaskAlreadyRunning() {
		
	}
}
