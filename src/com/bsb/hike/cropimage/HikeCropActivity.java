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
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.cropimage.HikeCropFragment.HikeCropListener;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.timeline.TestBmp;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;

/**
 * The activity can crop specific region of interest from an image.
 * 
 * @author Atul M
 */
public class HikeCropActivity extends HikeAppStateBaseFragmentActivity
{
	private static final String TAG = "HikeCropActivity";

	public static final String SOURCE_IMAGE_PATH = "SrcIMGP";

	public static final String CROPPED_IMAGE_PATH = "CropIMGP";

	boolean mSaving; // Whether the "save" button is already clicked.

	private String mSrcImagePath, mCropImagePath;

	private HikeCropFragment mCropFragment;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);

		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.cropimageactivity);

		Intent intent = getIntent();

		Bundle extras = intent.getExtras();

		if (extras != null)
		{
			mSrcImagePath = extras.getString(SOURCE_IMAGE_PATH);
			mCropImagePath = extras.getString(CROPPED_IMAGE_PATH);
		}

		if (TextUtils.isEmpty(mSrcImagePath) || TextUtils.isEmpty(mCropImagePath))
		{
			Toast.makeText(this, getResources().getString(R.string.image_failed), Toast.LENGTH_LONG).show();
			Logger.e(TAG, "Unable to open bitmap");
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		mCropFragment = HikeCropFragment.getInstance(new HikeCropListener()
		{
			@Override
			public void onSuccess(Bitmap croppedBmp)
			{
				onCropped(croppedBmp);
			}

			@Override
			public void onFailed()
			{
				Logger.e(TAG, "Crop failed");
			}
		}, mSrcImagePath);

		setupActionBar();

		getSupportFragmentManager().beginTransaction().replace(R.id.container, mCropFragment).commit();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.photos_action_bar, null);
		actionBar.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.color.photos_action_bar_background, null));
		actionBarView.findViewById(R.id.back).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		((TextView) actionBarView.findViewById(R.id.done_text)).setText(R.string.done);

		actionBarView.findViewById(R.id.done_container).setVisibility(View.VISIBLE);

		actionBarView.findViewById(R.id.done_container).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mCropFragment.crop();
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
			BitmapUtils.saveBitmapToFile(new File(mCropImagePath), argBmp);
			Intent resultIntent = new Intent();
			resultIntent.putExtra(CROPPED_IMAGE_PATH, mCropImagePath);
			resultIntent.putExtra(SOURCE_IMAGE_PATH, mSrcImagePath);
			setResult(RESULT_OK, resultIntent);
			finish();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			setResult(RESULT_CANCELED);
			finish();
		}
	}
}