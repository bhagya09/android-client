package com.bsb.hike.cropimage;

import java.io.IOException;

import org.apache.http.util.TextUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.photos.HikePhotosUtils;
import com.edmodo.cropper.CropImageView;
import com.edmodo.cropper.cropwindow.edge.Edge;
import com.hike.transporter.utils.Logger;

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

	public HikeCropFragment()
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

		BitmapFactory.Options options = new BitmapFactory.Options();
		
		options.inScaled = false;
		
		options.inDither = true;
		
		options.inPreferQualityOverSpeed = true;
		
		// Load bitmap
		Bitmap sourceBitmap = HikeBitmapFactory.decodeSampledBitmapFromFile(mSourceImagePath, HikeConstants.HikePhotos.MAX_IMAGE_DIMEN * 2, HikeConstants.HikePhotos.MAX_IMAGE_DIMEN * 2, Config.ARGB_8888, options);

		if (sourceBitmap == null)
		{
			Logger.e("HikeImageCropFragment", "Source file bitmap == null");
			return;
		}

		try
		{
			int minSize = HikePhotosUtils.dpToPx(50);

			if (sourceBitmap.getWidth() < minSize || sourceBitmap.getHeight() < minSize)
			{
				minSize = sourceBitmap.getWidth() > sourceBitmap.getHeight() ? sourceBitmap.getHeight() : sourceBitmap.getWidth();
			}

			Edge.MIN_CROP_LENGTH_PX = minSize;
			
			mCropImageView.setImageBitmap(sourceBitmap, new ExifInterface(mSourceImagePath));
		}
		catch (IOException e)
		{
			e.printStackTrace();
			mListener.onFailed();
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
