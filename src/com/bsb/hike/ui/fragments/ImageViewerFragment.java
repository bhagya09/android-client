package com.bsb.hike.ui.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

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
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.ProfileImageDownloader;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.utils.Utils;


import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.utils.ProfileImageLoader;
import com.bsb.hike.utils.Utils;

public class ImageViewerFragment extends SherlockFragment implements OnClickListener, Listener
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

	/**
	 * Default constructor
	 */
	public ImageViewerFragment() 
	{
	}
	
	/**
	 * Used to check if the imageview is editable from the acitivty or not
	 * @param isEditable true if editable, false otherwise
	 */
	public ImageViewerFragment(boolean isEditable)
	{
		this.isViewEditable = isEditable;
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
			ContactManager.getInstance().hasIcon(key);
		}
		ProfileImageLoader profileImageLoader = new ProfileImageLoader(getActivity(), key, imageView, imageSize, isStatusImage);
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
				mDialog = ProgressDialog.show(getActivity(), null, getResources().getString(R.string.downloading_image));
				mDialog.setCancelable(true);
				return null;
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
}
