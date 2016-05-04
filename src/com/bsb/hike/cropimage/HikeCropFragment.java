package com.bsb.hike.cropimage;

import java.io.IOException;

import org.apache.http.util.TextUtils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.utils.IntentFactory;
import com.edmodo.cropper.CropImageView;
import com.edmodo.cropper.cropwindow.edge.Edge;
import com.hike.transporter.utils.Logger;

/**
 * Uses https://github.com/edmodo/cropper/wiki
 * 
 * @author Atul M
 */
public class HikeCropFragment extends Fragment implements View.OnClickListener
{
	private boolean fixedAspectRatio;

	private boolean editEnabled;

	private boolean isSrcEdited;

	private View btnEdit;

	private View btnCrop;

	private View btnRotate;

	private View cropPanel;

	private View cropCancel;

	private View cropAccept;

	private View containerCrop, containerEdit, containerRotate;

	private View cropDivider;

	public interface HikeCropListener
	{
		void onSuccess(Bitmap croppedBmp);

		void onFailed();

		void toggleDoneButtonVisibility(boolean show);
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

		btnEdit = mFragmentView.findViewById(R.id.ib_edit);
		btnEdit.setOnClickListener(this);

		btnRotate = mFragmentView.findViewById(R.id.rotateLeft);
		btnRotate.setOnClickListener(this);

		btnCrop = mFragmentView.findViewById(R.id.ib_crop);
		btnCrop.setOnClickListener(this);

		cropPanel = mFragmentView.findViewById(R.id.crop_actions_panel);

		cropAccept = mFragmentView.findViewById(R.id.accept);
		cropAccept.setOnClickListener(this);

		cropCancel = mFragmentView.findViewById(R.id.cancel);
		cropCancel.setOnClickListener(this);

		containerCrop = mFragmentView.findViewById(R.id.container_crop);
		containerEdit = mFragmentView.findViewById(R.id.container_edit);
		containerRotate = mFragmentView.findViewById(R.id.container_rotate);

		cropDivider = mFragmentView.findViewById(R.id.crop_panel_divider);

		if (!editEnabled)
		{
			containerEdit.setVisibility(View.GONE);
		}

		return mFragmentView;
	}

	public void setAspectRatioFixed(boolean fixed)
	{
		fixedAspectRatio = fixed;
	}

	public void setEditEnabled(boolean editEnabled)
	{
		this.editEnabled = editEnabled;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		mCropImageView.setFixedAspectRatio(fixedAspectRatio);

		if (!fixedAspectRatio)
		{
			mCropImageView.hideCropOverlay();
		}
		else
		{
			containerCrop.setVisibility(View.GONE);
			containerRotate.setVisibility(View.VISIBLE);
		}

		if (!editEnabled)
		{
			containerEdit.setVisibility(View.GONE);
		}

		loadBitmap();
	}

	public void loadBitmap()
	{
		BitmapFactory.Options options = new BitmapFactory.Options();

		options.inScaled = false;

		options.inDither = true;

		options.inPreferQualityOverSpeed = true;

		// Load bitmap
		Bitmap sourceBitmap = null;

		if(fixedAspectRatio)
		{
			sourceBitmap = HikeBitmapFactory.decodeSampledBitmapFromFile(mSourceImagePath, (HikeConstants.HikePhotos.MODIFIED_MAX_IMAGE_DIMEN),
					(HikeConstants.HikePhotos.MODIFIED_MAX_IMAGE_DIMEN), Config.ARGB_8888, options, true);
		}
		else
		{
			sourceBitmap = HikeBitmapFactory.decodeSampledBitmapFromFile(mSourceImagePath, (HikeConstants.HikePhotos.MAX_IMAGE_DIMEN),
					(HikeConstants.HikePhotos.MAX_IMAGE_DIMEN), Config.ARGB_8888, options, false);
			if(sourceBitmap == null)
			{
				sourceBitmap = HikeBitmapFactory.decodeSampledBitmapFromFile(mSourceImagePath, (HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX),
						(HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX), Config.ARGB_8888, options, false);
			}
		}

		if (sourceBitmap == null)
		{
			Logger.e("HikeImageCropFragment", "Source file bitmap == null");
			return;
		}

		try
		{
			int minSize = HikePhotosUtils.dpToPx(120);

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
	}

	public boolean isInCropMode()
	{
		return mCropImageView.isCropOverlayVisible();
	}

	public void setListener(HikeCropListener argListener)
	{
		mListener = argListener;
	}

	public void setSourceImagePath(String argSourceImagePath)
	{
		mSourceImagePath = argSourceImagePath;
	}

	public String getSourceImagePath()
	{
		return mSourceImagePath;
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

	@Override
	public void onClick(View view)
	{
		int id = view.getId();
		switch (id)
		{
		case R.id.ib_edit:
			Intent intent = IntentFactory.getPictureEditorActivityIntent(getActivity(), mSourceImagePath, false, isSrcEdited ? mSourceImagePath : null, false);
			startActivityForResult(intent, HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE);
			break;
		case R.id.ib_crop:
			setCropViewVisibility(true);
			break;
		case R.id.rotateLeft:
			mCropImageView.rotateImage(-90);
			break;
		case R.id.accept:
			crop();
			setCropViewVisibility(false);
			break;
		case R.id.cancel:
			int rotation = mCropImageView.getDegreesRotated();
			if(rotation != 0)
			{
				mCropImageView.rotateImage(-1*rotation);
			}
			setCropViewVisibility(false);
			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK)
		{
			switch (requestCode)
			{
			case HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE:
				String editedFilePath = data.getStringExtra(HikeConstants.Extras.IMAGE_PATH);
				setSourceImagePath(editedFilePath);
				loadBitmap();
				if (!editedFilePath.equals(mSourceImagePath))
				{
					mSourceImagePath = editedFilePath;
					isSrcEdited = true;
				}
				break;
			}
		}
	}

	private void recordOriginalXY()
	{
		if (!originalRecordered)
		{
			originalRecordered = true;
			btnXorig = btnEdit.getX();
		}
	}

	boolean originalRecordered = false;

	private float btnXorig = 0;

	private void setCropViewVisibility(boolean enableCrop) {
		recordOriginalXY();

		btnEdit.animate().setStartDelay(50).x(enableCrop ? btnXorig + 200f : btnXorig);
		btnCrop.animate().x(enableCrop ? btnXorig + 200f : btnXorig);

		if (enableCrop)
		{
			mCropImageView.showCropOverlay();
		}
		else
		{
			mCropImageView.hideCropOverlay();
		}

		cropPanel.setVisibility(enableCrop ? View.VISIBLE : View.INVISIBLE);
		cropDivider.setVisibility(enableCrop ? View.VISIBLE : View.INVISIBLE);
		containerRotate.setVisibility(enableCrop ? View.VISIBLE : View.GONE);

		mListener.toggleDoneButtonVisibility(!enableCrop);

		containerCrop.animate().alpha(enableCrop ? 0f : 1f);
		containerEdit.animate().setStartDelay(50).alpha(enableCrop?0f:1f);
	}

	public boolean onBackPressed()
	{
		if(cropPanel.getVisibility() == View.VISIBLE)
		{
			onClick(cropCancel);
			return true;
		}

		return false;
	}

	public Bitmap getImageBitmap()
	{
		return mCropImageView.getBitmap();
	}

}
