package com.bsb.hike.tasks;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.PictureEditer;

public class GalleryItemLoaderTask implements Runnable{
	
	public static interface GalleryItemLoaderImp
	{
		public void onGalleryItemLoaded(GalleryItem galleryItem);
	}

	private GalleryItemLoaderImp listener;
	private Thread mThread;
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
	
	public GalleryItemLoaderTask(GalleryItemLoaderImp listener, boolean isInsideAlbum, boolean enableCameraPick) {
		this.listener = listener;
		mThread = new Thread(this);
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
	public void startQuery(Uri uri, String [] projection, String selection, String [] args, String sortBy, boolean editEnabled, List<String> editedImages)
	{
		this.uri = uri;
		this.projection = projection;
		this.selection = selection;
		this.args = args;
		this.sortBy = sortBy;
		this.editEnabled = editEnabled;
		this.editedImages = editedImages;
		
		if(!mRunning && mThread != null)
		{
			mThread.start();
			mRunning = true;
		}
	}

	public void cancelTask()
	{
		mRunning = false;
	}

	@Override
	public void run() {
		
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
				listener.onGalleryItemLoaded(cameraItem);
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
							GalleryItem allImgItem = new GalleryItem(cursor.getLong(idIdx), null, GalleryActivity.ALL_IMAGES_BUCKET_NAME, cursor.getString(dataIdx), cursor.getCount());
							listener.onGalleryItemLoaded(allImgItem);
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
							if(TextUtils.isEmpty(filePath) || isImageEdited(filePath))
							{
								continue;
							}

							if (!isInsideAlbum)
							{
								count = getGalleryItemCount(new File(filePath).getParent());
							}
							GalleryItem galleryItem = new GalleryItem(cursor.getLong(idIdx), cursor.getString(bucketIdIdx), cursor.getString(nameIdx), cursor.getString(dataIdx), count);
							if(!isInsideAlbum)
							{
								if(galleryItem.getName().startsWith(CAMERA_IMAGES))
									listener.onGalleryItemLoaded(galleryItem);
								else
									addItemInPreferredOrder(galleryItem, pendingItemList);
							}
							else
							{
								listener.onGalleryItemLoaded(galleryItem);
							}
						}while (mRunning && cursor.moveToNext());
					}
					if(pendingItemList != null && pendingItemList.size() > 0)
					{
						for (GalleryItem galleryItem : pendingItemList) {
							listener.onGalleryItemLoaded(galleryItem);
						}
					}
					mRunning = false;
				}
				finally
				{
					cursor.close();
				}
			}
		}
	}

	private void addItemInPreferredOrder(GalleryItem galleryItem, List<GalleryItem> pendingItemList)
	{
		String bucketName = galleryItem.getName();
		if(!isInsideAlbum && bucketName.startsWith(HIKE_IMAGES))
		{
			pendingItemList.add(0, galleryItem);
		}
		else
			pendingItemList.add(galleryItem);
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

		if(editedImages.contains(filePath))
		{
			return true;
		}

		return false;
	}
}
