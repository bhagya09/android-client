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

import org.apache.http.util.TextUtils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.cropimage.HikeCropFragment.HikeCropListener;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.edmodo.cropper.CropImageView;

/**
 * The activity can crop specific region of interest from an image.
 * 
 * @author Atul M
 */
public class HikeCropActivity extends HikeAppStateBaseFragmentActivity
{
	private static final String TAG = "HikeCropActivity";

	public static final String SOURCE_IMAGE_PATH = "image-path";

	public static final String CROPPED_IMAGE_PATH = "CropIMGP";

	public static final String CROP_COMPRESSION = "CropCompres";

	public static final String ALLOW_EDITING = "AllowEdit";

	public static final String FIXED_ASPECT_RATIO = "FixAspRatio";

	public static final String ASPECT_RATIO_X = "aspectRatioX";

	public static final String ASPECT_RATIO_Y = "aspectRatioY";

	private final String INTERIM_IMG_PATH = "InterimImgPath";

	private String mSrcImagePath, mCropImagePath;

	private HikeCropFragment mCropFragment;

	private CropCompression mCropCompression;

	private boolean isSrcEdited = false;

	private View doneContainer;
	
	private boolean doneClicked;

	private boolean isInCropMode;

	private ImageView doneImageView;

	private TextView doneText;

	private String mInterimImagePath;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.cropimageactivity);

		Intent intent = getIntent();

		Bundle extras = intent.getExtras();

		if (extras != null)
		{
			mSrcImagePath = extras.getString(SOURCE_IMAGE_PATH);

			if (TextUtils.isEmpty(mSrcImagePath))
			{
				mSrcImagePath = intent.getStringExtra(HikeConstants.Extras.GALLERY_SELECTION_SINGLE);
			}

			if (TextUtils.isEmpty(mSrcImagePath))
			{
				onCropFailed();
				return;
			}

			mCropImagePath = extras.getString(CROPPED_IMAGE_PATH);

			Bundle intentExtraBundle = extras.getBundle(CROP_COMPRESSION);

			if (intentExtraBundle != null)
			{
				Parcelable parcelable = intentExtraBundle.getParcelable(CROP_COMPRESSION);

				if (parcelable != null)
				{
					mCropCompression = (CropCompression) parcelable;
				}
			}
		}

		if(savedInstanceState != null)
		{
			mInterimImagePath = savedInstanceState.getString(INTERIM_IMG_PATH,null);
			if(!android.text.TextUtils.isEmpty(mInterimImagePath))
			{
				mSrcImagePath = mInterimImagePath;
			}
		}

		if (TextUtils.isEmpty(mSrcImagePath) || TextUtils.isEmpty(mCropImagePath))
		{
			Toast.makeText(this, getResources().getString(R.string.image_failed), Toast.LENGTH_LONG).show();
			Logger.e(TAG, "Unable to open bitmap");
			onCropFailed();
			return;
		}

		StatusBarColorChanger.setStatusBarColor(getWindow(), Color.BLACK);

		mCropFragment = HikeCropFragment.getInstance(new HikeCropListener() {
			@Override
			public void onSuccess(Bitmap croppedBmp) {
				onCropped(croppedBmp);
			}

			@Override
			public void onFailed() {
				Logger.e(TAG, "Crop failed");
				onCropFailed();
			}

			@Override
			public void toggleDoneButtonVisibility(final boolean show) {
				if (doneContainer != null)
				{
					doneContainer.setVisibility(show?View.VISIBLE:View.GONE);
				}
			}
		}, mSrcImagePath);

		boolean allowEditing = false;

		if (extras != null)
		{
			mCropFragment.setAspectRatioFixed(extras.getBoolean(FIXED_ASPECT_RATIO, false));

			int aspectRatioX = extras.getInt(ASPECT_RATIO_X, CropImageView.DEFAULT_ASPECT_RATIO_X);
			int aspectRatioY = extras.getInt(ASPECT_RATIO_Y, CropImageView.DEFAULT_ASPECT_RATIO_Y);
			mCropFragment.setAspectWidthAndHeight(aspectRatioX, aspectRatioY);

			allowEditing = extras.getBoolean(ALLOW_EDITING, false);
		}

		mCropFragment.setEditEnabled(allowEditing);

		setupActionBar();

		getSupportFragmentManager().beginTransaction().replace(R.id.container, mCropFragment).commit();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.photos_action_bar, null);
		actionBar.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.color.crop_actionbar_bg, null));
		actionBarView.findViewById(R.id.back).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		((TextView) actionBarView.findViewById(R.id.done_text)).setText(R.string.done);

		doneContainer = actionBarView.findViewById(R.id.done_container);

		doneContainer.setVisibility(View.VISIBLE);

		doneImageView = (ImageView) doneContainer.findViewById(R.id.next_btn);

		doneText = (TextView) doneContainer.findViewById(R.id.done_text);

		doneContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				doneClicked = true;
				if (mCropFragment.isInCropMode())
				{
					mCropFragment.crop();
				}
				else
				{
					sendCropResult();
				}
			}
		});

		actionBar.setCustomView(actionBarView);
		Toolbar parent = (Toolbar) actionBarView.getParent();
		parent.setContentInsetsAbsolute(0, 0);

	}

	private void onCropped(Bitmap argBmp)
	{
		try
		{
			if (mCropCompression != null)
			{
				argBmp = HikePhotosUtils.scaleAdvanced(argBmp, mCropCompression.getWidth(), mCropCompression.getHeight(), true);
			}

			if (argBmp == null)
			{
				onCropFailed();
			}

			BitmapUtils.saveBitmapToFile(new File(mCropImagePath), argBmp, CompressFormat.JPEG, mCropCompression == null ? 85 : mCropCompression.getQuality());

			mInterimImagePath = mCropImagePath;

			if (doneClicked)
			{
				sendCropResult();
			}
			else
			{
				mCropFragment.setSourceImagePath(mCropImagePath);
				mCropFragment.loadBitmap();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			onCropFailed();
		}
	}

	private void sendCropResult()
	{
		File cropResult = new File(mCropImagePath);
		if(!cropResult.exists())
		{
			try
			{
				BitmapUtils.saveBitmapToFile(new File(mCropImagePath), mCropFragment.getImageBitmap(), CompressFormat.JPEG,
						mCropCompression == null ? 85 : mCropCompression.getQuality());
			}
			catch (IOException e)
			{
				e.printStackTrace();
				onCropFailed();
				return;
			}
		}

		Intent resultIntent = new Intent();
		resultIntent.putExtra(CROPPED_IMAGE_PATH, mCropImagePath);
		resultIntent.putExtra(SOURCE_IMAGE_PATH, mSrcImagePath);
		Bundle extras = getIntent().getExtras();
		if (extras != null)
		{
			resultIntent.putExtras(extras);
		}
		setResult(RESULT_OK, resultIntent);
		finish();
	}

	private void onCropFailed()
	{
		setResult(RESULT_CANCELED);
		finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(INTERIM_IMG_PATH , mInterimImagePath);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed() {
		boolean isEventConsumed = false;

		if(mCropFragment != null && mCropFragment.isVisible())
		{
			//This means it: (1) has been added, (2) has its view attached to the window, and (3) is not hidden
			isEventConsumed = mCropFragment.onBackPressed();
		}

		if(!isEventConsumed)
		{
			if(!TextUtils.isEmpty(mInterimImagePath))
			{
				Utils.deleteFile(new File(mInterimImagePath));
			}
			super.onBackPressed();
		}
	}
}
