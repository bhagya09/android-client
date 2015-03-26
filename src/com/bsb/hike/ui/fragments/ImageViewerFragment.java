package com.bsb.hike.ui.fragments;

import java.io.File;

import android.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
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
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.ProfileImageLoader;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.ui.SettingsActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ImageViewerFragment extends SherlockFragment implements LoaderCallbacks<Boolean>, OnClickListener, Listener
{

	ImageView imageView;

	private ProgressDialog mDialog;

	private String mappedId;

	private String key;

	private boolean isStatusImage;

	private String basePath;

	private boolean hasCustomImage;

	private String fileName;

	private String url;

	private IconLoader iconLoader;

	private int imageSize;
	
	public static final int FROM_PROFILE_ACTIVITY = 1;

	public static final int FROM_SETTINGS_ACTIVITY = 2;
	
	private int whichActivity;

	private String TAG = "ImageViewerFragment";
	
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
		iconLoader = new IconLoader(getActivity(), 180);
		imageSize = this.getActivity().getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);
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

		url = getArguments().getString(HikeConstants.Extras.URL);

		basePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;

		showImage();
	}

	private void showImage() 
	{
		hasCustomImage = true;
		
		key = mappedId;
		
		if (!isStatusImage)
		{
			int idx = key.lastIndexOf(ProfileActivity.PROFILE_PIC_SUFFIX);
			
			if (idx > 0)
			{
				key = new String(key.substring(0, idx));
			}
			hasCustomImage = ContactManager.getInstance().hasIcon(key);
		}

		if (hasCustomImage)
		{
			fileName = Utils.getProfileImageFileName(key);

			File file = new File(basePath, fileName);

			boolean downloadImage = false;
			if (file.exists())
			{
				BitmapDrawable drawable = HikeMessengerApp.getLruCache().get(mappedId);
				if (drawable == null)
				{
					Bitmap b = HikeBitmapFactory.scaleDownBitmap(basePath + "/" + fileName, imageSize, imageSize, Bitmap.Config.RGB_565,true,false);
					if (b != null)
					{
						drawable = HikeBitmapFactory.getBitmapDrawable(this.getActivity().getApplicationContext().getResources(), b);
						Logger.e(getClass().getSimpleName(), "Decode from file is returning null bitmap.");
						if (drawable != null)
						{
							HikeMessengerApp.getLruCache().putInCache(mappedId, drawable);
						}
					}
					else
					{
						Utils.removeLargerProfileImageForMsisdn(key);
						drawable = HikeMessengerApp.getLruCache().getIconFromCache(key);
						downloadImage = true;
					}
				}

				imageView.setImageDrawable(drawable);
			}
			else
			{
				File f = new File(basePath, Utils.getTempProfileImageFileName(key));

				if (f.exists())
				{
					long fileTS = f.lastModified();
					long oldTS = Utils.getOldTimestamp(5);
					
					if (fileTS < oldTS)
					{
						Logger.d(TAG, "Temp file is older than 5 minutes.Deleting temp file and downloading profile pic ");
						Utils.removeTempProfileImage(key);
						downloadImage = true;
					}
					
					BitmapDrawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(key);
					imageView.setImageDrawable(drawable);
				}
				else
				{
					downloadImage = true;
				}
			}

			if (downloadImage)
			{
				BitmapDrawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(key);
				imageView.setImageDrawable(drawable);

				// imageView.setImageDrawable(IconCacheManager.getInstance()
				// .getIconForMSISDN(mappedId));

				getLoaderManager().initLoader(0, null, this);

				mDialog = ProgressDialog.show(getActivity(), null, getResources().getString(R.string.downloading_image));
				mDialog.setCancelable(true);
			}
			imageView.setBackgroundResource(0);				
		}
		else
		{
			imageView.setImageDrawable(HikeBitmapFactory.getDefaultAvatar(getResources(), key, true));
		}
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
	public Loader<Boolean> onCreateLoader(int id, Bundle arguments)
	{
		return new ProfileImageLoader(getActivity(), key, fileName, hasCustomImage, isStatusImage, url);
	}

	@Override
	public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1)
	{
		dismissProgressDialog();

		if (!isAdded())
		{
			return;
		}

		File file = new File(basePath, fileName);

		BitmapDrawable drawable = null;
		if (file.exists())
		{
			drawable = HikeBitmapFactory.getBitmapDrawable(this.getActivity().getApplicationContext().getResources(),
					HikeBitmapFactory.scaleDownBitmap(basePath + "/" + fileName, imageSize, imageSize, Bitmap.Config.RGB_565,true,false));
			imageView.setImageDrawable(drawable);
		}

		Log.d(getClass().getSimpleName(), "Putting in cache mappedId : " + mappedId);
		/*
		 * Putting downloaded image bitmap in cache.
		 */
		if (drawable != null)
		{
			HikeMessengerApp.getLruCache().putInCache(mappedId, drawable);
		}

		if (isStatusImage)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, null);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_IMAGE_DOWNLOADED, null);
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
	public void onLoaderReset(Loader<Boolean> arg0)
	{
		dismissProgressDialog();
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
