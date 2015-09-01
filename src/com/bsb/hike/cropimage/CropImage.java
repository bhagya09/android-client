/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bsb.hike.cropimage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * The activity can crop specific region of interest from an image.
 */
public class CropImage extends MonitoredActivity
{
	private static final String TAG = "CropImage";

	// These are various options can be specified in the intent.
	private Bitmap.CompressFormat mOutputFormat = Bitmap.CompressFormat.JPEG; // only
																				// used
																				// with
																				// mSaveUri

	private Uri mSaveUri = null;

	private boolean returnToFile;// check to prevent scaling when required

	private int mAspectX, mAspectY;

	private boolean mCircleCrop = false;

	private final Handler mHandler = new Handler();

	// These options specifiy the output image size and whether we should
	// scale the output to fit it (or just crop it).
	private int mOutputX, mOutputY;

	private int mQuality = HikeBitmapFactory.DEFAULT_BITMAP_COMPRESSION;
	
	private boolean mScale;

	private boolean mScaleUp = true;

	private boolean mDoFaceDetection = false;

	boolean mWaitingToPick; // Whether we are wait the user to pick a face.

	boolean mSaving; // Whether the "save" button is already clicked.

	private CropImageView mImageView;

	private ContentResolver mContentResolver;

	private Bitmap mBitmap;

	private final BitmapManager.ThreadSet mDecodingThreads = new BitmapManager.ThreadSet();

	HighlightView mCrop;

	private IImage mImage;

	private String mImagePath;

	private boolean mCircleHighlight;

	@Override
	public void onCreate(Bundle icicle)
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);
		
		super.onCreate(icicle);
		
		mContentResolver = getContentResolver();

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.cropimage);

		/*
		 * Added to fix a Android issue for devices that support hardware acceleration. http://android-developers.blogspot.in/2011/03/android-30 -hardware-acceleration.html
		 */
		mImageView = (CropImageView) findViewById(R.id.image);
		try
		{
			Method method = mImageView.getClass().getMethod("setLayerType", Integer.TYPE, Paint.class);
			method.invoke(mImageView, 1, null);
		}
		catch (IllegalArgumentException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception during reflection", e);
		}
		catch (IllegalAccessException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception during reflection", e);
		}
		catch (InvocationTargetException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception during reflection", e);
		}
		catch (SecurityException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception during reflection", e);
		}
		catch (NoSuchMethodException e)
		{
			Logger.e(getClass().getSimpleName(), "Exception during reflection", e);
		}

		showStorageToast(this);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		
		if (extras != null)
		{
			boolean circleHighlight = extras.getBoolean(HikeConstants.Extras.CIRCLE_HIGHLIGHT);
			
			if (extras.getString(HikeConstants.Extras.CIRCLE_CROP) != null || circleHighlight)
			{
				mCircleCrop = true;
				if (circleHighlight)
				{
					mCircleHighlight = true;
				}
				mAspectX = 1;
				mAspectY = 1;
			}

			if (extras.containsKey(HikeConstants.Extras.RETURN_CROP_RESULT_TO_FILE))
			{
				returnToFile = extras.getBoolean(HikeConstants.Extras.RETURN_CROP_RESULT_TO_FILE);
			}

			mImagePath = extras.getString(HikeConstants.Extras.IMAGE_PATH);
			
			if (mImagePath == null)
			{
//				// Check if intent is from GalleryActivity
//				ArrayList<GalleryItem> galleryList = intent.getParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS);
//				if (galleryList != null && !galleryList.isEmpty())
//				{
//					mImagePath = galleryList.get(0).getFilePath();
//				}
				mImagePath = intent.getStringExtra(HikeConstants.Extras.GALLERY_SELECTION_SINGLE);
			}
			
			mSaveUri = extras.containsKey(MediaStore.EXTRA_OUTPUT) ? getImageUri(extras.getString(MediaStore.EXTRA_OUTPUT)) : null;

			mBitmap = getBitmap(mImagePath);
			String imageOrientation = Utils.getImageOrientation(mImagePath);
			mBitmap = HikeBitmapFactory.rotateBitmap(mBitmap, Utils.getRotatedAngle(imageOrientation));

			mAspectX = extras.getInt(HikeConstants.Extras.ASPECT_X);
			mAspectY = extras.getInt(HikeConstants.Extras.ASPECT_Y);
			mOutputX = extras.getInt(HikeConstants.Extras.OUTPUT_X);
			mOutputY = extras.getInt(HikeConstants.Extras.OUTPUT_Y);
			mScale = extras.getBoolean(HikeConstants.Extras.SCALE, true);
			mScaleUp = extras.getBoolean(HikeConstants.Extras.SCALE_UP, true);
			mQuality = extras.getInt(HikeConstants.Extras.JPEG_COMPRESSION_QUALITY, HikeBitmapFactory.DEFAULT_BITMAP_COMPRESSION);
		}

		if (mBitmap == null)
		{
			onError();
			return;
		}

		// Make UI fullscreen.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		findViewById(R.id.rotateLeft).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				mBitmap = Util.rotateImage(mBitmap, -90);
				RotateBitmap rotateBitmap = new RotateBitmap(mBitmap);
				mImageView.setImageRotateBitmapResetBase(rotateBitmap, true);
				mRunFaceDetection.run();
			}
		});

		setupActionBar();
		
		startFaceDetection();
	}
	
	private void onError()
	{
		Toast toast = Toast.makeText(this, getResources().getString(R.string.image_failed), Toast.LENGTH_LONG);
		toast.show();
		Logger.d(TAG, "Unable to open bitmap");
		finish();
	}
	
	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.photos_action_bar, null);
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.photos_action_bar_background));
		actionBarView.findViewById(R.id.back).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		((TextView) actionBarView.findViewById(R.id.done_text)).setText(R.string.done);
		
		actionBarView.findViewById(R.id.done_container).setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				onSaveClicked();
			}
		});

		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
		
	}
	
	@Override
	public void onBackPressed()
	{
		setResult(RESULT_CANCELED);
		super.onBackPressed();
	}

	private Uri getImageUri(String path)
	{
		return Uri.fromFile(new File(path));
	}

	private Bitmap getBitmap(String path)
	{
		/*
		 * resize the image while opening it. http://stackoverflow.com/questions/ 477572/android-strange-out-of-memory
		 * -issue-while-loading-an-image-to-a-bitmap-object/823966#823966
		 */
		BitmapFactory.Options options = new BitmapFactory.Options();

		/* query the filesize of the bitmap */
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		final int maxSize = 1024;
		int scale = 1;
		/* determine the correct scale (must be a power of 2) */
		if (options.outHeight > maxSize || options.outWidth > maxSize)
		{
			scale = Math.max(options.outHeight, options.outWidth) / maxSize;
		}

		options = new BitmapFactory.Options();
		options.inSampleSize = scale;
		return HikeBitmapFactory.decodeFile(path, options);
	}

	private void startFaceDetection()
	{
		if (isFinishing())
		{
			return;
		}

		mImageView.setImageBitmapResetBase(mBitmap, true);

		Util.startBackgroundJob(this, null, "Please wait\u2026", new Runnable()
		{
			public void run()
			{
				final CountDownLatch latch = new CountDownLatch(1);
				final Bitmap b = (mImage != null) ? mImage.fullSizeBitmap(IImage.UNCONSTRAINED, 1024 * 1024) : mBitmap;
				mHandler.post(new Runnable()
				{
					public void run()
					{
						if (b != mBitmap && b != null)
						{
							mImageView.setImageBitmapResetBase(b, true);
							mBitmap = b;
						}
						if (mImageView.getScale() == 1F)
						{
							mImageView.center(true, true);
						}
						latch.countDown();
					}
				});
				try
				{
					latch.await();
				}
				catch (InterruptedException e)
				{
					throw new RuntimeException(e);
				}
				mRunFaceDetection.run();
			}
		}, mHandler);
	}

	private void onSaveClicked()
	{
		// TODO this code needs to change to use the decode/crop/encode single
		// step api so that we don't require that the whole (possibly large)
		// bitmap doesn't have to be read into memory
		if (mSaving)
			return;

		if (mCrop == null)
		{
			return;
		}

		mSaving = true;

		Rect r = mCrop.getCropRect();

		int width = r.width();
		int height = r.height();

		// If we are circle cropping, we want alpha channel, which is the
		// third param here.
		Bitmap croppedImage = returnToFile ? HikeBitmapFactory.createBitmap(width, height, Bitmap.Config.ARGB_8888) : HikeBitmapFactory.createBitmap(width, height,
				mCircleCrop ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);

		if (croppedImage == null)
		{
			onError();
			return;
		}
		else
		{
			Canvas canvas = new Canvas(croppedImage);
			Rect dstRect = new Rect(0, 0, width, height);
			canvas.drawBitmap(mBitmap, r, dstRect, null);
		}

		if (mCircleCrop && !mCircleHighlight)
		{
			// OK, so what's all this about?
			// Bitmaps are inherently rectangular but we want to return
			// something that's basically a circle. So we fill in the
			// area around the circle with alpha. Note the all important
			// PortDuff.Mode.CLEAR.
			Canvas c = new Canvas(croppedImage);
			Path p = new Path();
			p.addCircle(width / 2F, height / 2F, width / 2F, Path.Direction.CW);
			c.clipPath(p, Region.Op.DIFFERENCE);
			c.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
		}

		/* If the output is required to a specific size then scale or fill */
		if (mOutputX != 0 && mOutputY != 0)
		{
			if (mScale)
			{
				/*
				 * This throws an NPE for low end phones when their memory runs low.
				 */
				try
				{
					/* Scale the image to the required dimensions */
					croppedImage = Util.transform(new Matrix(), croppedImage, mOutputX, mOutputY, mScaleUp);
				}
				catch (NullPointerException e)
				{
					croppedImage = null;
				}
			}
			else
			{

				/*
				 * Don't scale the image crop it to the size requested. Create an new image with the cropped image in the center and the extra space filled.
				 */

				// Don't scale the image but instead fill it so it's the
				// required dimension
				Bitmap b = HikeBitmapFactory.createBitmap(mOutputX, mOutputY, Bitmap.Config.RGB_565);
				if (b == null)
				{
					onError();
					return;
				}
				Canvas canvas = new Canvas(b);

				Rect srcRect = mCrop.getCropRect();
				Rect dstRect = new Rect(0, 0, mOutputX, mOutputY);

				int dx = (srcRect.width() - dstRect.width()) / 2;
				int dy = (srcRect.height() - dstRect.height()) / 2;

				/* If the srcRect is too big, use the center part of it. */
				srcRect.inset(Math.max(0, dx), Math.max(0, dy));

				/* If the dstRect is too big, use the center part of it. */
				dstRect.inset(Math.max(0, -dx), Math.max(0, -dy));

				/* Draw the cropped bitmap in the center */
				canvas.drawBitmap(mBitmap, srcRect, dstRect, null);

				/* Set the cropped bitmap as the new bitmap */
				croppedImage = b;
			}
		}

		// Return the cropped image directly or save it to the specified URI.
		Bundle myExtras = getIntent().getExtras();
		if (!returnToFile && myExtras != null && (myExtras.getParcelable(HikeConstants.Extras.DATA) != null || myExtras.getBoolean(HikeConstants.Extras.RETURN_DATA)))
		{
			Bundle extras = new Bundle();
			extras.putParcelable(HikeConstants.Extras.DATA, croppedImage);
			setResult(RESULT_OK, (new Intent()).setAction("inline-data").putExtras(extras));
			finish();
		}
		else
		{
			final Bitmap b = croppedImage;
			Util.startBackgroundJob(this, null, getString(R.string.cropping_image), new Runnable()
			{
				public void run()
				{
					saveOutput(b);
				}
			}, mHandler);
		}
	}

	private void saveOutput(Bitmap croppedImage)
	{
		if (mSaveUri != null)
		{
			if (croppedImage != null)
			{
				OutputStream outputStream = null;
				try
				{
					outputStream = mContentResolver.openOutputStream(mSaveUri);
					if (outputStream != null)
					{
						Logger.d(TAG, "quality factor : " + mQuality);
						croppedImage.compress(mOutputFormat, mQuality, outputStream);
					}
				}
				catch (IOException ex)
				{
					// TODO: report error to caller
					Logger.e(TAG, "Cannot open file: " + mSaveUri, ex);
				}
				finally
				{
					Util.closeSilently(outputStream);
				}
			}
			Bundle extras = new Bundle();
			extras.putString(MediaStore.EXTRA_OUTPUT, croppedImage == null ? null : mSaveUri.getPath());
			extras.putString(HikeConstants.HikePhotos.ORIG_FILE, mImagePath);
			setResult(RESULT_OK, new Intent(mSaveUri.toString()).putExtras(extras));
		}
		else
		{
			Bundle extras = new Bundle();
			extras.putParcelable(HikeConstants.Extras.BITMAP, croppedImage);
			extras.putString(HikeConstants.HikePhotos.ORIG_FILE, mImagePath);
			setResult(RESULT_OK, new Intent().putExtras(extras));
		}
		finish();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		BitmapManager.instance().cancelThreadDecoding(mDecodingThreads);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		this.mBitmap = null;
		// Good time for GC
		System.gc();
	}

	Runnable mRunFaceDetection = new Runnable()
	{
		float mScale = 1F;

		Matrix mImageMatrix;

		FaceDetector.Face[] mFaces = new FaceDetector.Face[3];

		int mNumFaces;

		// For each face, we create a HightlightView for it.
		private void handleFace(FaceDetector.Face f)
		{
			PointF midPoint = new PointF();

			int r = ((int) (f.eyesDistance() * mScale)) * 2;
			f.getMidPoint(midPoint);
			midPoint.x *= mScale;
			midPoint.y *= mScale;

			int midX = (int) midPoint.x;
			int midY = (int) midPoint.y;

			HighlightView hv = new HighlightView(mImageView);

			int width = mBitmap.getWidth();
			int height = mBitmap.getHeight();

			Rect imageRect = new Rect(0, 0, width, height);

			RectF faceRect = new RectF(midX, midY, midX, midY);
			faceRect.inset(-r, -r);
			if (faceRect.left < 0)
			{
				faceRect.inset(-faceRect.left, -faceRect.left);
			}

			if (faceRect.top < 0)
			{
				faceRect.inset(-faceRect.top, -faceRect.top);
			}

			if (faceRect.right > imageRect.right)
			{
				faceRect.inset(faceRect.right - imageRect.right, faceRect.right - imageRect.right);
			}

			if (faceRect.bottom > imageRect.bottom)
			{
				faceRect.inset(faceRect.bottom - imageRect.bottom, faceRect.bottom - imageRect.bottom);
			}

			hv.setup(mImageMatrix, imageRect, faceRect, mCircleCrop, mAspectX != 0 && mAspectY != 0);

			mImageView.add(hv);
		}

		// Create a default HightlightView if we found no face in the picture.
		private void makeDefault()
		{
			HighlightView hv = new HighlightView(mImageView);

			int width = mBitmap.getWidth();
			int height = mBitmap.getHeight();

			Rect imageRect = new Rect(0, 0, width, height);

			// REMOVED: make the default size about 4/5 of the width or height//* 4 / 5;
			int cropWidth = Math.min(width, height);
			
			int cropHeight = cropWidth;

			if (mAspectX != 0 && mAspectY != 0)
			{
				if (mAspectX > mAspectY)
				{
					cropHeight = cropWidth * mAspectY / mAspectX;
				}
				else
				{
					cropWidth = cropHeight * mAspectX / mAspectY;
				}
			}

			int x = (width - cropWidth) / 2;
			int y = (height - cropHeight) / 2;

			RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
			hv.setup(mImageMatrix, imageRect, cropRect, mCircleCrop, mAspectX != 0 && mAspectY != 0);

			mImageView.mHighlightViews.clear(); // Thong added for rotate

			mImageView.add(hv);
		}

		// Scale the image down for faster face detection.
		private Bitmap prepareBitmap()
		{
			if (mBitmap == null)
			{
				return null;
			}

			// 256 pixels wide is enough.
			if (mBitmap.getWidth() > 256)
			{
				mScale = 256.0F / mBitmap.getWidth();
			}
			Matrix matrix = new Matrix();
			matrix.setScale(mScale, mScale);
			Bitmap faceBitmap = HikeBitmapFactory.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
			return faceBitmap;
		}

		public void run()
		{
			mImageMatrix = mImageView.getImageMatrix();
			Bitmap faceBitmap = prepareBitmap();

			mScale = 1.0F / mScale;
			
			if(faceBitmap == null)
			{
				return ;
			}
			
			if (mDoFaceDetection)
			{
				FaceDetector detector = new FaceDetector(faceBitmap.getWidth(), faceBitmap.getHeight(), mFaces.length);
				mNumFaces = detector.findFaces(faceBitmap, mFaces);
			}

			mHandler.post(new Runnable()
			{
				public void run()
				{
					mWaitingToPick = mNumFaces > 1;
					if (mNumFaces > 0)
					{
						for (int i = 0; i < mNumFaces; i++)
						{
							handleFace(mFaces[i]);
						}
					}
					else
					{
						makeDefault();
					}
					mImageView.invalidate();
					if (mImageView.mHighlightViews.size() == 1)
					{
						mCrop = mImageView.mHighlightViews.get(0);
						mCrop.setFocus(true);
					}

					if (mNumFaces > 1)
					{
						Toast t = Toast.makeText(CropImage.this, "Multi face crop help", Toast.LENGTH_SHORT);
						t.show();
					}
				}
			});
		}
	};

	public static final int NO_STORAGE_ERROR = -1;

	public static final int CANNOT_STAT_ERROR = -2;

	public static void showStorageToast(Activity activity)
	{
		showStorageToast(activity, calculatePicturesRemaining());
	}

	public static void showStorageToast(Activity activity, int remaining)
	{
		String noStorageText = null;

		if (remaining == NO_STORAGE_ERROR)
		{
			String state = Environment.getExternalStorageState();
			if (state == Environment.MEDIA_CHECKING)
			{
				noStorageText = "Preparing card";
			}
			else
			{
				noStorageText = "No storage card";
			}
		}
		else if (remaining < 1)
		{
			noStorageText = "Not enough space";
		}

		if (noStorageText != null)
		{
			Toast.makeText(activity, noStorageText, 5000).show();
		}
	}

	public static int calculatePicturesRemaining()
	{
		try
		{
			/*
			 * if (!ImageManager.hasStorage()) { return NO_STORAGE_ERROR; } else {
			 */
			String storageDirectory = Environment.getExternalStorageDirectory().toString();
			StatFs stat = new StatFs(storageDirectory);
			float remaining = ((float) stat.getAvailableBlocks() * (float) stat.getBlockSize()) / 400000F;
			return (int) remaining;
			// }
		}
		catch (Exception ex)
		{
			// if we can't stat the filesystem then we don't know how many
			// pictures are remaining. it might be zero but just leave it
			// blank since we really don't know.
			return CANNOT_STAT_ERROR;
		}
	}

}

class CropImageView extends ImageViewTouchBase
{
	ArrayList<HighlightView> mHighlightViews = new ArrayList<HighlightView>();

	HighlightView mMotionHighlightView = null;

	float mLastX, mLastY;

	int mMotionEdge;

	private Context mContext;

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		if (mBitmapDisplayed.getBitmap() != null)
		{
			for (HighlightView hv : mHighlightViews)
			{
				hv.mMatrix.set(getImageMatrix());
				hv.invalidate();
				if (hv.mIsFocused)
				{
					centerBasedOnHighlightView(hv);
				}
			}
		}
	}

	public CropImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;
	}

	@Override
	protected void zoomTo(float scale, float centerX, float centerY)
	{
		super.zoomTo(scale, centerX, centerY);
		for (HighlightView hv : mHighlightViews)
		{
			hv.mMatrix.set(getImageMatrix());
			hv.invalidate();
		}
	}

	@Override
	protected void zoomIn()
	{
		super.zoomIn();
		for (HighlightView hv : mHighlightViews)
		{
			hv.mMatrix.set(getImageMatrix());
			hv.invalidate();
		}
	}

	@Override
	protected void zoomOut()
	{
		super.zoomOut();
		for (HighlightView hv : mHighlightViews)
		{
			hv.mMatrix.set(getImageMatrix());
			hv.invalidate();
		}
	}

	@Override
	protected void postTranslate(float deltaX, float deltaY)
	{
		super.postTranslate(deltaX, deltaY);
		for (int i = 0; i < mHighlightViews.size(); i++)
		{
			HighlightView hv = mHighlightViews.get(i);
			hv.mMatrix.postTranslate(deltaX, deltaY);
			hv.invalidate();
		}
	}

	// According to the event's position, change the focus to the first
	// hitting cropping rectangle.
	private void recomputeFocus(MotionEvent event)
	{
		for (int i = 0; i < mHighlightViews.size(); i++)
		{
			HighlightView hv = mHighlightViews.get(i);
			hv.setFocus(false);
			hv.invalidate();
		}

		for (int i = 0; i < mHighlightViews.size(); i++)
		{
			HighlightView hv = mHighlightViews.get(i);
			int edge = hv.getHit(event.getX(), event.getY());
			if (edge != HighlightView.GROW_NONE)
			{
				if (!hv.hasFocus())
				{
					hv.setFocus(true);
					hv.invalidate();
				}
				break;
			}
		}
		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		CropImage cropImage = (CropImage) mContext;
		if (cropImage.mSaving)
		{
			return false;
		}

		switch (event.getAction())
		{
		case MotionEvent.ACTION_DOWN:
			if (cropImage.mWaitingToPick)
			{
				recomputeFocus(event);
			}
			else
			{
				for (int i = 0; i < mHighlightViews.size(); i++)
				{
					HighlightView hv = mHighlightViews.get(i);
					int edge = hv.getHit(event.getX(), event.getY());
					if (edge != HighlightView.GROW_NONE)
					{
						mMotionEdge = edge;
						mMotionHighlightView = hv;
						mLastX = event.getX();
						mLastY = event.getY();
						mMotionHighlightView.setMode((edge == HighlightView.MOVE) ? HighlightView.ModifyMode.Move : HighlightView.ModifyMode.Grow);
						break;
					}
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			if (cropImage.mWaitingToPick)
			{
				for (int i = 0; i < mHighlightViews.size(); i++)
				{
					HighlightView hv = mHighlightViews.get(i);
					if (hv.hasFocus())
					{
						cropImage.mCrop = hv;
						for (int j = 0; j < mHighlightViews.size(); j++)
						{
							if (j == i)
							{
								continue;
							}
							mHighlightViews.get(j).setHidden(true);
						}
						centerBasedOnHighlightView(hv);
						((CropImage) mContext).mWaitingToPick = false;
						return true;
					}
				}
			}
			else if (mMotionHighlightView != null)
			{
				centerBasedOnHighlightView(mMotionHighlightView);
				mMotionHighlightView.setMode(HighlightView.ModifyMode.None);
			}
			mMotionHighlightView = null;
			break;
		case MotionEvent.ACTION_MOVE:
			if (cropImage.mWaitingToPick)
			{
				recomputeFocus(event);
			}
			else if (mMotionHighlightView != null)
			{
				mMotionHighlightView.handleMotion(mMotionEdge, event.getX() - mLastX, event.getY() - mLastY);
				mLastX = event.getX();
				mLastY = event.getY();

				if (true)
				{
					// This section of code is optional. It has some user
					// benefit in that moving the crop rectangle against
					// the edge of the screen causes scrolling but it means
					// that the crop rectangle is no longer fixed under
					// the user's finger.
					ensureVisible(mMotionHighlightView);
				}
			}
			break;
		}

		switch (event.getAction())
		{
		case MotionEvent.ACTION_UP:
			center(true, true);
			break;
		case MotionEvent.ACTION_MOVE:
			// if we're not zoomed then there's no point in even allowing
			// the user to move the image around. This call to center puts
			// it back to the normalized location (with false meaning don't
			// animate).
			if (getScale() == 1F)
			{
				center(true, true);
			}
			break;
		}

		return true;
	}

	// Pan the displayed image to make sure the cropping rectangle is visible.
	private void ensureVisible(HighlightView hv)
	{
		Rect r = hv.mDrawRect;

		int panDeltaX1 = Math.max(0, mLeft - r.left);
		int panDeltaX2 = Math.min(0, mRight - r.right);

		int panDeltaY1 = Math.max(0, mTop - r.top);
		int panDeltaY2 = Math.min(0, mBottom - r.bottom);

		int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
		int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

		if (panDeltaX != 0 || panDeltaY != 0)
		{
			panBy(panDeltaX, panDeltaY);
		}
	}

	// If the cropping rectangle's size changed significantly, change the
	// view's center and scale according to the cropping rectangle.
	private void centerBasedOnHighlightView(HighlightView hv)
	{
		Rect drawRect = hv.mDrawRect;

		float width = drawRect.width();
		float height = drawRect.height();

		float thisWidth = getWidth();
		float thisHeight = getHeight();

		float z1 = thisWidth / width * .6F;
		float z2 = thisHeight / height * .6F;

		float zoom = Math.min(z1, z2);
		zoom = zoom * this.getScale();
		zoom = Math.max(1F, zoom);
		if ((Math.abs(zoom - getScale()) / zoom) > .1)
		{
			float[] coordinates = new float[] { hv.mCropRect.centerX(), hv.mCropRect.centerY() };
			getImageMatrix().mapPoints(coordinates);
			zoomTo(zoom, coordinates[0], coordinates[1], 300F);
		}

		ensureVisible(hv);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		for (int i = 0; i < mHighlightViews.size(); i++)
		{
			mHighlightViews.get(i).draw(canvas);
		}
	}

	public void add(HighlightView hv)
	{
		mHighlightViews.add(hv);
		invalidate();
	}
}