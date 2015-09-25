package com.bsb.hike.gallery;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.PictureEditer;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class GalleryItemLoaderTask extends AsyncTask<Void, Void, Void>{
	
	public static interface GalleryItemLoaderImp
	{
		public void onGalleryItemLoaded(GalleryItem galleryItem);
		public void onNoGalleryItemFound();
	}

	private GalleryItemLoaderImp listener;
	private boolean mRunning;
	private boolean isInsideAlbum;

	private Uri uri;
	private String [] projection;
	private String selection;
	private String [] args;
	private String sortBy;
	private boolean editEnabled;
	private List<String> editedImages;
	private boolean enableCameraPick;

	private final String TYPE_JPG = ".jpg";

	private final String TYPE_JPEG = ".jpeg";

	private final String TYPE_PNG = ".png";

	private final String TYPE_GIF = ".gif";

	private final String TYPE_BMP = ".bmp";

	private final String CAMERA_IMAGES = "Camera";

	private final String HIKE_IMAGES = "hike";

	private final String TAG = "GalleryItemLoaderTask";
	
	public GalleryItemLoaderTask(GalleryItemLoaderImp listener, boolean isInsideAlbum, boolean enableCameraPick) {
		this.listener = listener;
		this.isInsideAlbum = isInsideAlbum;
		this.enableCameraPick = enableCameraPick;
	}

	/**
	 * Query to fetch the details of files
	 * @param uri
	 * @param projection
	 * @param selection
	 * @param args
	 * @param sortBy
	 * @param editEnabled
	 * @param editedImages
	 */
	public void buildQuery(Uri uri, String [] projection, String selection, String [] args, String sortBy, boolean editEnabled, List<String> editedImages)
	{
		this.uri = uri;
		this.projection = projection;
		this.selection = selection;
		this.args = args;
		this.sortBy = sortBy;
		this.editEnabled = editEnabled;
		this.editedImages = editedImages;
		mRunning = true;
	}

	public void cancelTask()
	{
		mRunning = false;
	}

	public boolean isRunning()
	{
		return mRunning;
	}

	@Override
	protected Void doInBackground(Void... params) {
		while (mRunning) {
			Context mContext = HikeMessengerApp.getInstance().getApplicationContext();
			Cursor cursor = null;
			List<GalleryItem> pendingItemList = new ArrayList<GalleryItem>();

			/*
			 * Add "pick from camera" button/bucket
			 */
			if (enableCameraPick)
			{
				GalleryItem cameraItem = new GalleryItem(GalleryItem.CAMERA_TILE_ID, GalleryActivity.NEW_PHOTO, GalleryActivity.CAMERA_TILE, 0);
				onItemLoaded(cameraItem);
			}
			/*
			 * Creating All images bucket where we will show all images present in the device.
			 */
			if (!isInsideAlbum)
			{
				String[] proj = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA };
				cursor = mContext.getContentResolver().query(uri, proj, null, null, sortBy);
				if (cursor != null)
				{
					try
					{
						int idIdx = cursor.getColumnIndex(MediaStore.Images.Media._ID);
						int dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
						if (cursor.moveToFirst())
						{
							String filePath = cursor.getString(dataIdx);
							if(!TextUtils.isEmpty(filePath))
							{
								GalleryItem allImgItem = new GalleryItem(cursor.getLong(idIdx), null, GalleryActivity.ALL_IMAGES_BUCKET_NAME, filePath, cursor.getCount());
								onItemLoaded(allImgItem);
							}
						}
					}
					finally
					{
						cursor.close();
					}
				}
			}

			cursor = mContext.getContentResolver().query(uri, projection, selection, args, sortBy);

			if (cursor != null)
			{
				try
				{
					int idIdx = cursor.getColumnIndex(MediaStore.Images.Media._ID);
					int bucketIdIdx = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
					int nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
					int dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

					if (cursor.moveToFirst())
					{
						do
						{
							int count = 0;
							String filePath = cursor.getString(dataIdx);
							String fileName = cursor.getString(nameIdx);
							String bucketId = cursor.getString(bucketIdIdx);
							if(TextUtils.isEmpty(filePath) || TextUtils.isEmpty(fileName) || TextUtils.isEmpty(bucketId) || isImageEdited(filePath))
							{
								continue;
							}

							if (!isInsideAlbum)
							{
								count = getGalleryItemCount(new File(filePath).getParent());
							}
							GalleryItem galleryItem = new GalleryItem(cursor.getLong(idIdx), bucketId, fileName, filePath, count);
							if(!isInsideAlbum)
							{
								if(galleryItem.getName().startsWith(CAMERA_IMAGES))
								{
									onItemLoaded(galleryItem);
								}
								else
								{
									addItemInPreferredOrder(galleryItem, pendingItemList);
								}
							}
							else
							{
								onItemLoaded(galleryItem);
							}
						}while (mRunning && cursor.moveToNext());
					}
					else
					{
						if(listener != null)
						{
							listener.onNoGalleryItemFound();
						}
					}
					mRunning = false;
					if(!Utils.isEmpty(pendingItemList))
					{
						for (GalleryItem galleryItem : pendingItemList) {
							listener.onGalleryItemLoaded(galleryItem);
						}
					}
				}
				finally
				{
					cursor.close();
				}
			}
			else
			{
				mRunning = false;
				if(listener != null)
				{
					listener.onNoGalleryItemFound();
				}
			}
		}
		return null;
	}

	private void addItemInPreferredOrder(GalleryItem galleryItem, List<GalleryItem> pendingItemList)
	{
		String bucketName = galleryItem.getName();
		if(!isInsideAlbum && bucketName.startsWith(HIKE_IMAGES))
		{
			pendingItemList.add(0, galleryItem);
		}
		else
		{
			pendingItemList.add(galleryItem);
		}
	}

	/**
	 * returns count of files in given directory
	 * @param dirPath
	 */
	private int getGalleryItemCount(String dirPath)
	{
		int number = 0;
		try
		{
			File dir = new File(dirPath);
			FilenameFilter imgFilenameFilter = new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String filename) {
					return isImage(filename);
				}
			};
			number = dir.listFiles(imgFilenameFilter).length;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Logger.e(TAG, "Exception while getting item count" , e);
		}
		return number;
	}

	/**
	 * returns true if the file is an image
	 * @param fileName
	 */
	private boolean isImage(String fileName)
	{
		boolean isImg = false;
		fileName = fileName.toLowerCase();
		if (fileName.endsWith(TYPE_JPG) || fileName.endsWith(TYPE_JPEG) || fileName.endsWith(TYPE_PNG)
				|| fileName.endsWith(TYPE_GIF) || fileName.endsWith(TYPE_BMP))
		{
			isImg = true;
		}
		return isImg;
	}

	/**
	 * returns true if the filePath was edited in the current MultiEdit Flow
	 * @param filePath
	 */
	private boolean isImageEdited(String filePath)
	{
		if (!editEnabled || editedImages == null || !filePath.contains(PictureEditer.getEditImageSaveDirectory(false)))
		{
			return false;
		}

		return editedImages.contains(filePath);
	}

	private void onItemLoaded(GalleryItem item)
	{
		if(this.listener != null)
		{
			listener.onGalleryItemLoaded(item);
		}
	}
}
