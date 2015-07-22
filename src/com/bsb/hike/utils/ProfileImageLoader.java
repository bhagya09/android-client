package com.bsb.hike.utils;

import java.io.File;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.tasks.ProfileImageDownloader;
import com.bsb.hike.ui.ProfileActivity;

public class ProfileImageLoader implements LoaderCallbacks<Boolean>
{
	private Context context;

	private String msisdn;

	private WeakReference<ImageView> imageView;

	private int imageSize;

	private Drawable defaultDrawable;

	private LoaderListener loaderListener;

	String basePath;

	String mappedId;

	boolean hasCustomImage;
	
	boolean isStatusImage;

	public ProfileImageLoader(Context context, String msisdn, ImageView imageView, int imageSize, boolean isStatusImage) 
	{
		this.context = context;
		this.msisdn = msisdn;
		this.imageView = new WeakReference<ImageView>(imageView);
		this.imageSize = imageSize;
		this.defaultDrawable = HikeBitmapFactory.getDefaultAvatar(context.getResources(), msisdn, true);
		basePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		mappedId = msisdn + ProfileActivity.PROFILE_PIC_SUFFIX;
		this.isStatusImage = isStatusImage;
	}

	/*
	 * Set default avatar drawable if the user doesnt have a custom DP. 
	 *  @param drawable Drawable
	 */
	public void setDefaultDrawable(Drawable drawable)
	{
		defaultDrawable = drawable;
	}

	/*
	 * Listen for LoaderCallbacks from activity/fragment. 
	 *  @param loaderListener LoaderListener
	 */
	public void setLoaderListener(LoaderListener loaderListener)
	{
		this.loaderListener = loaderListener;
	}

	/*
	 * This method loads up the image from a file otherwise makes a network request on an AsyncTaskLaoder.
	 * 
	 *  @param loaderManager getLaoderManager() from Activity/Fragment
	 */
	public boolean loadProfileImage(LoaderManager loaderManager)
	{
		
		hasCustomImage = isStatusImage || ContactManager.getInstance().hasIcon(msisdn);
		Log.v("Sriram", "loadProfileImage:::::::::::::::hasCustomImage:::"+hasCustomImage);
		if (hasCustomImage)
		{
			String fileName = Utils.getProfileImageFileName(msisdn);
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
						drawable = HikeBitmapFactory.getBitmapDrawable(context.getResources(), b);
						if (drawable != null)
						{
							HikeMessengerApp.getLruCache().putInCache(mappedId, drawable);
						}
					}
					else
					{
						Logger.d(getClass().getSimpleName(), "Decode from file is returning null bitmap.");
						Utils.removeLargerProfileImageForMsisdn(msisdn);
						drawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn);
						downloadImage = true;
					}
				}
				Logger.d(getClass().getSimpleName(), "setting image from cache...downloadImage = " + downloadImage);
				Log.v("Sriram", "loadProfileImage:::::::::::::::setting image from cache.:::");
				setImageDrawable(drawable);
			}
			else
			{
				File f = new File(basePath, Utils.getTempProfileImageFileName(msisdn));

				if (f.exists())
				{
					long fileTS = f.lastModified();
					long oldTS = Utils.getOldTimestamp(5);
					
					if (fileTS < oldTS)
					{
						Logger.d(getClass().getSimpleName(), "Temp file is older than 5 minutes.Deleting temp file and downloading profile pic ");
						Utils.removeTempProfileImage(msisdn);
						downloadImage = true;
					}
					
					BitmapDrawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn);
					setImageDrawable(drawable);
					Log.v("Sriram", "loadProfileImage:::::::::::::::setting image from cache Tempo cache.:::");
				}
				else
				{
					downloadImage = true;
				}
			}

			if (downloadImage)
			{
				BitmapDrawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn);
				Log.v("Sriram", "loadProfileImage:::::::::::::::downloadImage.:::");
				setImageDrawable(drawable);

				loaderManager.initLoader(0, null, this);
			}
		}
		else
		{
			Log.v("Sriram", "loadProfileImage:::::else.:::");
			setImageDrawable(defaultDrawable);
		}
		return hasCustomImage;
	}

	private void loadFromFile() 
	{
		String fileName = Utils.getProfileImageFileName(msisdn);

		File file = new File(basePath, fileName);

		BitmapDrawable drawable = null;
		if (file.exists())
		{
			Log.v("Sriram", "loadProfileImage:::::loadFromFile.:::");
			Logger.d(getClass().getSimpleName(),"setting final downloaded image...");
			drawable = HikeBitmapFactory.getBitmapDrawable(context.getResources(),
					HikeBitmapFactory.scaleDownBitmap(basePath + "/" + fileName, imageSize, imageSize, Bitmap.Config.RGB_565,true,false));
			setImageDrawable(drawable);
		}

		Logger.d(getClass().getSimpleName(), "Putting in cache mappedId : " + mappedId);
		/*
		 * Putting downloaded image bitmap in cache.
		 */
		if (drawable != null)
		{
			HikeMessengerApp.getLruCache().putInCache(mappedId, drawable);
		}
	}

	@Override
	public Loader<Boolean> onCreateLoader(int arg0, Bundle arg1) 
	{
		Logger.d(getClass().getSimpleName(),"onCreateLoader...");
		if(loaderListener!=null)
		{
			Loader<Boolean> loader = loaderListener.onCreateLoader(arg0, arg1);
			if(loader!=null)
			{
				return loader;
			}
		}
		return new ProfileImageDownloader(context, msisdn, Utils.getProfileImageFileName(msisdn), hasCustomImage, isStatusImage, null);
	}

	@Override
	public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1) 
	{
		Logger.d(getClass().getSimpleName(),"onLoadFinished...");
		if(loaderListener!=null)
		{
			loaderListener.onLoadFinished(arg0, arg1);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_IMAGE_DOWNLOADED, null);
		loadFromFile();
	}

	@Override
	public void onLoaderReset(Loader<Boolean> arg0) 
	{
		Logger.d(getClass().getSimpleName(),"onLoaderReset...");
		if(loaderListener!=null)
		{
			loaderListener.onLoaderReset(arg0);
		}
	}

	private void setImageDrawable(Drawable drawable)
	{
		Log.v("Sriram", "Setting image bitmap:::::::::::::::::::");
		if((drawable != null) && drawable instanceof BitmapDrawable){
			if(((BitmapDrawable) drawable).getBitmap().isRecycled()){
				Log.v("Sriram", "Setting image bitmap:::recycled bitmap hence returning it::::::::::::::::");
				return;
			}
		}
		if(imageView.get() != null)
		{
			imageView.get().setImageDrawable(drawable);
		}
		else
		{
			Logger.e(getClass().getSimpleName(), "ImageView reference is null");
		}
	}

	public interface LoaderListener
	{
		public Loader<Boolean> onCreateLoader(int arg0, Bundle arg1);

		public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1);

		public void onLoaderReset(Loader<Boolean> arg0);
	}
}
