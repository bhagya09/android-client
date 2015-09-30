package com.bsb.hike.cropimage;

import java.io.IOException;

import org.apache.http.util.TextUtils;

import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.edmodo.cropper.CropImageView;
import com.hike.transporter.utils.Logger;

import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Uses https://github.com/edmodo/cropper/wiki
 * 
 * @author Atul M
 */
public class HikeCropFragment extends Fragment
{
	public interface HikeCropListener
	{
		void onSuccess(Bitmap croppedBmp);

		void onFailed();
	}

	private View mFragmentView;

	private CropImageView mCropImageView;

	private HikeCropListener mListener;

	private String mSourceImagePath;

	public static HikeCropFragment getInstance(HikeCropListener listener, String sourceImagePath)
	{
		if (listener == null)
		{
			throw new IllegalArgumentException("listener cannot be null");
		}

		if (TextUtils.isEmpty(sourceImagePath))
		{
			throw new IllegalArgumentException("filename cannot be null");
		}

		HikeCropFragment newFragment = new HikeCropFragment();
		newFragment.setListener(listener);
		newFragment.setSourceImagePath(sourceImagePath);
		return newFragment;
	}

	private HikeCropFragment()
	{
		// use newInstance()
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mFragmentView = inflater.inflate(R.layout.cropimagefrag, null, false);

		mCropImageView = (CropImageView) mFragmentView.findViewById(R.id.cropimageview);

		return mFragmentView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		// Load bitmap
		Bitmap sourceBitmap = HikeBitmapFactory.decodeFile(mSourceImagePath);

		if (sourceBitmap == null)
		{
			Logger.e("HikeImageCropFragment", "Source file bitmap == null");
			return;
		}

		try
		{
			mCropImageView.setImageBitmap(sourceBitmap, new ExifInterface(mSourceImagePath));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		// Rotate button
		mFragmentView.findViewById(R.id.rotateLeft).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				mCropImageView.rotateImage(90);
			}
		});
	}

	public void setListener(HikeCropListener argListener)
	{
		mListener = argListener;
	}

	public void setSourceImagePath(String argSourceImagePath)
	{
		mSourceImagePath = argSourceImagePath;
	}

	public void crop()
	{
		Bitmap croppedImage = mCropImageView.getCroppedImage();
		
		if (croppedImage == null)
		{
			mListener.onFailed();
		}
		else
		{
			mListener.onSuccess(croppedImage);
		}
	}
}
