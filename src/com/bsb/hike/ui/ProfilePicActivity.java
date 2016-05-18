package com.bsb.hike.ui;

import java.io.File;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.WindowManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.cropimage.CropCompression;
import com.bsb.hike.cropimage.HikeCropActivity;
import com.bsb.hike.ui.fragments.ProfilePicFragment;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Utils;

public class ProfilePicActivity extends HikeAppStateBaseFragmentActivity
{
	private String filename;

	private String mGenus;

	private String mSpecies;

	@Override
	public void onCreate(Bundle savedState)
	{
		overridePendingTransition(R.anim.fade_in_animation, R.anim.fade_out_animation);

		super.onCreate(savedState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.profilepicactivity);

		// Get filename from intent data
		Intent intent = getIntent();
		filename = intent.getStringExtra(HikeMessengerApp.FILE_PATH);

		if (filename == null)
		{
			filename = intent.getStringExtra(HikeConstants.Extras.GALLERY_SELECTION_SINGLE);
		}

		if (filename == null)
		{
			ProfilePicActivity.this.finish();
			return;
		}

		mGenus = intent.getStringExtra(HikeConstants.Extras.GENUS);

		mSpecies = intent.getStringExtra(HikeConstants.Extras.SPECIES);

		getSupportActionBar().hide();

		StatusBarColorChanger.setStatusBarColor(getWindow(), Color.BLACK);

		if(savedState == null)
		{
			CropCompression compression = new CropCompression().maxWidth(640).maxHeight(640).quality(80);
			startActivityForResult(IntentFactory.getCropActivityIntent(this, filename, getTempProfilePicFile(new File(filename).getName()), compression, true, true), HikeConstants.CROP_RESULT);
		}

	}

	protected void onSaveInstanceState(Bundle outState)
	{
		// first saving my state, so the bundle wont be empty.
		// http://code.google.com/p/android/issues/detail?id=19917
		outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
		super.onSaveInstanceState(outState);
	}

	public static String getTempProfilePicFile(String mOriginalName)
	{
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		/*
		 * Making sure the directory exists before setting a profile image
		 */
		File dir = new File(directory);
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		// Creating No Media file in Hike Profile Images Folder if not already there
		// Todo prevent deleting of .nomedia on app start
		Utils.makeNoMediaFile(dir, true);

		String fileName = Utils.getTempProfileImageFileName(mOriginalName, true);
		String destFilePath = directory + File.separator + fileName;
		return destFilePath;
	}

	private void deleteTempProfilePicIfExists()
	{
		// The user returned from crop...deleting temporary profile image if created
		String fileName = getTempProfilePicFile(filename);

		if (fileName == null)
		{
			return;
		}

		File temp = new File(fileName);
		if (temp.exists())
		{
			temp.delete();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == HikeConstants.CROP_RESULT)
		{
			switch (resultCode)
			{
			case RESULT_OK:
				String croppedImagePath = data.getStringExtra(HikeCropActivity.CROPPED_IMAGE_PATH);
				String sourceImagePath = data.getStringExtra(HikeCropActivity.SOURCE_IMAGE_PATH);
				uploadProfilePic(croppedImagePath, sourceImagePath);
				break;
			case RESULT_CANCELED:
				// The user returned from crop...deleting temporary profile image if created
				deleteTempProfilePicIfExists();
				ProfilePicActivity.this.finish();
				break;
			}
		}
	}

	private void uploadProfilePic(final String croppedImageFile, final String originalImageFile)
	{
		ProfilePicFragment profilePicFragment = new ProfilePicFragment();
		Bundle b = new Bundle();
		b.putString(HikeConstants.HikePhotos.FILENAME, croppedImageFile);
		b.putString(HikeConstants.HikePhotos.ORIG_FILE, croppedImageFile);

		if (!TextUtils.isEmpty(mGenus))
		{
			b.putString(HikeConstants.Extras.GENUS, mGenus);
		}

		if (!TextUtils.isEmpty(mSpecies))
		{
			b.putString(HikeConstants.Extras.SPECIES, mSpecies);
		}

		profilePicFragment.setArguments(b);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, profilePicFragment).commit();
	}

}
