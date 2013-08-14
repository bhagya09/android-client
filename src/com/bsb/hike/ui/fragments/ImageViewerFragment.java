package com.bsb.hike.ui.fragments;

import java.io.File;

import android.app.ProgressDialog;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.ProfileImageLoader;
import com.bsb.hike.utils.Utils;

public class ImageViewerFragment extends SherlockFragment implements
		LoaderCallbacks<Boolean> {

	ImageView imageView;
	private ProgressDialog mDialog;
	private String mappedId;
	private boolean isStatusImage;
	private String basePath;
	private boolean hasCustomImage;
	private String fileName;
	private String url;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View parent = inflater.inflate(R.layout.image_viewer, null);
		imageView = (ImageView) parent.findViewById(R.id.image);

		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mappedId = getArguments().getString(HikeConstants.Extras.MAPPED_ID);
		isStatusImage = getArguments().getBoolean(
				HikeConstants.Extras.IS_STATUS_IMAGE);

		url = getArguments().getString(HikeConstants.Extras.URL);

		basePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT
				+ HikeConstants.PROFILE_ROOT;

		hasCustomImage = true;
		if (!isStatusImage) {
			hasCustomImage = HikeUserDatabase.getInstance().hasIcon(mappedId);
		}

		fileName = hasCustomImage ? Utils.getProfileImageFileName(mappedId)
				: Utils.getDefaultAvatarServerName(getActivity(), mappedId);

		File file = new File(basePath, fileName);

		if (file.exists()) {
			imageView.setImageDrawable(BitmapDrawable.createFromPath(basePath
					+ "/" + fileName));
		} else {
			imageView.setImageDrawable(IconCacheManager.getInstance()
					.getIconForMSISDN(mappedId));

			getLoaderManager().initLoader(0, null, this);

			mDialog = ProgressDialog.show(getActivity(), null, getResources()
					.getString(R.string.downloading_image));
			mDialog.setCancelable(true);
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		dismissProgressDialog();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
	}

	@Override
	public Loader<Boolean> onCreateLoader(int id, Bundle arguments) {
		return new ProfileImageLoader(getActivity(), mappedId, fileName,
				hasCustomImage, isStatusImage, url);
	}

	@Override
	public void onLoadFinished(Loader<Boolean> arg0, Boolean arg1) {
		dismissProgressDialog();

		if (!isAdded()) {
			return;
		}

		File file = new File(basePath, fileName);

		if (file.exists()) {
			imageView.setImageDrawable(BitmapDrawable.createFromPath(basePath
					+ "/" + fileName));
		}
	}

	@Override
	public void onLoaderReset(Loader<Boolean> arg0) {
		dismissProgressDialog();
	}

	private void dismissProgressDialog() {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
	}
}
