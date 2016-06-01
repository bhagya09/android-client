package com.bsb.hike.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.AnalyticsConstants.ProfileImageActions;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HomeAnalyticsConstants;
import com.bsb.hike.cropimage.CropCompression;
import com.bsb.hike.cropimage.HikeCropActivity;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.CustomAlertDialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.imageHttp.HikeImageUploader;
import com.bsb.hike.imageHttp.HikeImageWorker;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.tasks.DownloadImageTask;
import com.bsb.hike.tasks.DownloadImageTask.ImageDownloadResult;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.ProfilePicActivity;
import com.bsb.hike.ui.fragments.ImageViewerFragment;
import com.bsb.hike.ui.fragments.ImageViewerFragment.DisplayPictureEditListener;
import com.bsb.hike.ui.fragments.ShareLinkFragment;
import com.bsb.hike.ui.fragments.ShareLinkFragment.ShareLinkFragmentListener;
import com.bsb.hike.utils.Utils.ExternalStorageState;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public abstract class ChangeProfileImageBaseActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener, DisplayPictureEditListener, HikeImageWorker.TaskCallbacks, ShareLinkFragmentListener
{
	private HikeSharedPreferenceUtil prefs;

	private String mLocalMSISDN;
	private Dialog mDialog;
	
	private static final String TAG = "dp_upload";

	public class ChangeProfileImageActivityState
	{
		public String deleteAvatarStatusId;

		/*
		 * the bitmap before the user saves it
		 */
		public String destFilePath = null;

		public RequestToken deleteAvatarToken;

		/*
		 * the task to download the picasa image
		 */
		public DownloadImageTask downloadPicasaImageTask;

		public HikeImageUploader mImageWorkerFragment;
		
		public ShareLinkFragment shareLinkFragment;

		public String genus;
	}

	protected ChangeProfileImageActivityState mActivityState;

	private String mRemoveImagePath;

	private IRequestListener deleteAvatarRequestListener;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		prefs = HikeSharedPreferenceUtil.getInstance();

		Object obj = getLastCustomNonConfigurationInstance();

		if (obj instanceof ChangeProfileImageActivityState)
		{
			mActivityState = (ChangeProfileImageActivityState) obj;
			
			if (mActivityState.deleteAvatarToken != null)
			{
				/* we're currently executing a task, so show the progress dialog */
				if (mActivityState.deleteAvatarToken.isRequestRunning())
				{
					mActivityState.deleteAvatarToken.addRequestListener(getDeleteAvatarRequestListener());
				}
				mDialog = ProgressDialog.show(this, null, getString(R.string.removing_dp));
			}
			if(mActivityState.mImageWorkerFragment != null &&  (mActivityState.mImageWorkerFragment.isTaskRunning()))
			{
				mActivityState.mImageWorkerFragment.setTaskCallbacks(this);
				mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));
			}
		}
		else
		{
			mActivityState = new ChangeProfileImageActivityState();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		Logger.d(TAG, "onRetainNonConfigurationinstance");
		return mActivityState;
	}

	public void selectNewProfilePicture(Context context, boolean isPersonal, boolean useTimestamp)
	{
		if (Utils.getExternalStorageState() == ExternalStorageState.NONE || Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
		{
			Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
			return;
		}
		if (!Utils.hasEnoughFreeSpaceForProfilePic())
		{
			Toast.makeText(getApplicationContext(), R.string.not_enough_space_profile_pic, Toast.LENGTH_SHORT).show();
			return;
		}

		int galleryFlags = GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS | GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM;

		if(isPersonal)
		{
			Intent galleryPickerIntent = IntentFactory.getProfilePicUpdateIntent(ChangeProfileImageBaseActivity.this, galleryFlags);
			Utils.setSpecies(getSourceSpecies(), galleryPickerIntent);
			startActivity(galleryPickerIntent);
		}
		else
		{
			CropCompression compression = new CropCompression().maxWidth(640).maxHeight(640).quality(80);
			Intent imageChooserIntent = IntentFactory.getImageChooserIntent(ChangeProfileImageBaseActivity.this, galleryFlags, getNewProfileImagePath(true),compression, true);
			startActivityForResult(imageChooserIntent, HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE);
		}
	}

	protected String getNewProfileImagePath(boolean useTimestamp)
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

		String fileName = Utils.getTempProfileImageFileName(mLocalMSISDN, useTimestamp);
		String destFilePath = directory + File.separator + fileName;
		return destFilePath;

	}

	@Override
	protected void  onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK)
		{
			return;
		}
		
		String path = null;
		File selectedFileIcon = null;

		mActivityState.genus = data.getStringExtra(HikeConstants.Extras.GENUS);

		switch (requestCode)
		{
		case HikeConstants.GALLERY_RESULT:
			Logger.d("ProfileActivity", "The activity is " + this);
			boolean isPicasaImage = false;
			Uri selectedFileUri = null;
			if (data == null || data.getData() == null)
			{
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			selectedFileUri = data.getData();

			if (Utils.isPicasaUri(selectedFileUri.toString()))
			{
				isPicasaImage = true;
				path = Utils.getOutputMediaFile(HikeFileType.PROFILE, null, false).getAbsolutePath();
			}
			else
			{
				path = Utils.getAbsolutePathFromUri(selectedFileUri, this,false);
			}
			if (TextUtils.isEmpty(path))
			{
				if (isPicasaImage)
					Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			if (!isPicasaImage)
			{
					Intent profilePicIntent = new Intent(ChangeProfileImageBaseActivity.this, ProfilePicActivity.class);
					profilePicIntent.putExtra(HikeMessengerApp.FILE_PATH, path);
					Utils.setGenus(mActivityState.genus, profilePicIntent);
					Utils.setSpecies(getSourceSpecies(), profilePicIntent);
					startActivity(profilePicIntent);
					finish();
			}
			else
			{
				final File destFile = new File(path);

				mActivityState.downloadPicasaImageTask = new DownloadImageTask(getApplicationContext(), destFile, selectedFileUri, new ImageDownloadResult()
				{
					@Override
					public void downloadFinished(boolean result)
					{
						if (mDialog != null)
						{
							mDialog.dismiss();
							mDialog = null;
						}
						mActivityState.downloadPicasaImageTask = null;

						if (!result)
						{
							Toast.makeText(getApplicationContext(), R.string.error_download, Toast.LENGTH_SHORT).show();
						}
						else
						{
							Intent profilePicIntent = new Intent(ChangeProfileImageBaseActivity.this, ProfilePicActivity.class);
							profilePicIntent.putExtra(HikeMessengerApp.FILE_PATH, destFile.getAbsolutePath());
							Utils.setGenus(mActivityState.genus, profilePicIntent);
							Utils.setSpecies(getSourceSpecies(), profilePicIntent);
							startActivity(profilePicIntent);
							finish();
						}
					}
				});
				mActivityState.downloadPicasaImageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.downloading_image));
			}
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SET_PROFILE_PIC_GALLERY);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch (JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
			break;


		case HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE:
		case HikeConstants.CROP_RESULT:
			mActivityState.destFilePath = data.getStringExtra(HikeCropActivity.CROPPED_IMAGE_PATH);

			if (mActivityState.destFilePath == null)
			{
				Toast.makeText(getApplicationContext(), R.string.error_setting_profile, Toast.LENGTH_SHORT).show();
				return;
			}
			
			profileImageCropped();
			break;
		
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int item)
	{
		if(dialog!=null)
		{
			dialog.dismiss();
		}

		switch (item)
		{
		case HikeConstants.NEW_PROFILE_PICTURE:
			selectNewProfilePicture(ChangeProfileImageBaseActivity.this, !OneToNConversationUtils.isOneToNConversation(mLocalMSISDN), true);
			break;

		case HikeConstants.REMOVE_PROFILE_PICTURE:
		{
			showRemovePhotoConfirmDialog();
		}
			break;
		}
		mRemoveImagePath = null;
	}

	/**
	 * Used to display a confirmation dialog asking user if he wants to delete DP from his favorites timeline as well
	 */
	private void showRemovePhotoConfirmDialog()
	{
		
		if(TextUtils.isEmpty(mRemoveImagePath))
		{
			return;
		}
		
		JSONObject md = new JSONObject();

		try
		{
			md.put(HikeConstants.EVENT_KEY, ProfileImageActions.DP_REMOVE_EVENT);

			switch (mRemoveImagePath)
			{
			case ProfileImageActions.DP_EDIT_FROM_DISPLAY_IMAGE:
			{
				md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_EDIT_FROM_DISPLAY_IMAGE);
			}
				break;

			case ProfileImageActions.DP_EDIT_FROM_PROFILE_OVERFLOW_MENU:
			{
				md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_EDIT_FROM_PROFILE_OVERFLOW_MENU);
			}
				break;

			case ProfileImageActions.DP_EDIT_FROM_SETTINGS_PREVIEW_IMAGE:
			{
				md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_EDIT_FROM_SETTINGS_PREVIEW_IMAGE);
			}
				break;

			case ProfileImageActions.DP_EDIT_FROM_PROFILE_SCREEN:
			{
				md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_EDIT_FROM_PROFILE_SCREEN);
			}
				break;
			}
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, md);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "json exception");
		}

		CustomAlertDialog deleteConfirmDialog = new CustomAlertDialog(this, HikeDialogFactory.REMOVE_DP_CONFIRM_DIALOG);
		deleteConfirmDialog.setTitle(R.string.remove_photo);
		deleteConfirmDialog.setMessage(R.string.confirm_remove_photo);

		// if checkbox is selected, delete the profile status update from own and favorites timeline
		String dpStatusId = prefs.getPref().getString(HikeMessengerApp.DP_CHANGE_STATUS_ID, "");

		if (!dpStatusId.isEmpty())
		{
			deleteConfirmDialog.setCheckBox(R.string.check_delete_from_timeline, null, false);
		}

		HikeDialogListener dialogListener = new HikeDialogListener()
		{
			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				JSONObject md = new JSONObject();
				CustomAlertDialog deleteDialog = (CustomAlertDialog) hikeDialog;
				if (deleteDialog.isChecked())
				{
					ContactInfo contactInfo = Utils.getUserContactInfo(prefs.getPref());
					StatusMessageType[] smType = { StatusMessageType.PROFILE_PIC };
					StatusMessage lastsm = HikeConversationsDatabase.getInstance().getLastStatusMessage(smType, contactInfo);

					/* To handle the case when a user has set dp during signup. He will not have any dp status update at this point. */
					if (lastsm == null)
					{
						deleteDisplayPicture(null);
					}
					else
					{
						deleteDisplayPicture(lastsm.getMappedId());
					}
					try
					{
						md.put(AnalyticsConstants.EVENT_KEY, ProfileImageActions.DP_REMOVE_CONFIRM_YES);
						md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_REMOVE_FROM_FAVOURITES_CHECKED);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.UI_EVENT, md);
					}
					catch (JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "json error");
					}
				}
				else
				{
					deleteDisplayPicture(null);

					try
					{
						md.put(AnalyticsConstants.EVENT_KEY, ProfileImageActions.DP_REMOVE_CONFIRM_YES);
						md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_REMOVE_FROM_FAVOURITES_UNCHECKED);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.UI_EVENT, md);
					}
					catch (JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "json error");
					}
				}
				deleteDialog.dismiss();
			}
			
			@Override
			public void neutralClicked(HikeDialog hikeDialog)
			{
				
			}
			
			@Override
			public void negativeClicked(HikeDialog hikeDialog)
			{
				hikeDialog.dismiss();
			}
		};
		deleteConfirmDialog.setPositiveButton(R.string.YES, dialogListener);
		deleteConfirmDialog.setNegativeButton(R.string.NO,dialogListener);
		deleteConfirmDialog.show();
	}

	/**
	 * Used to submit a request to the server to delete the display picture
	 * 
	 * @param id
	 *            statusId of the status message about the display picture status update from the timeline as well While calling this method pass statusid to delete related post,
	 *            pass null to delete just the profile picture
	 */
	public void deleteDisplayPicture(final String id)
	{
		mActivityState.deleteAvatarStatusId = id;
		JSONObject json = null;
		if (mActivityState.deleteAvatarStatusId != null)
		{
			try
			{
				json = new JSONObject();
				json.put(HikeConstants.STATUS_ID, mActivityState.deleteAvatarStatusId);
			}
			catch (JSONException e)
			{
				Logger.e(TAG, "exception while deleting status : " + e);
			}
		}
		mActivityState.deleteAvatarToken = HttpRequests.deleteAvatarRequest(json, getDeleteAvatarRequestListener());
		mActivityState.deleteAvatarToken.execute();
		mDialog = ProgressDialog.show(this, null, getString(R.string.removing_dp));
	}

	private IRequestListener getDeleteAvatarRequestListener()
	{
		deleteAvatarRequestListener = new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				Logger.d("ProfileActivity", "delete dp request succeeded!");

				// clear the profile thumbnail from lru cache and db
				HikeMessengerApp.getLruCache().deleteIconForMSISDN(mLocalMSISDN);

				if (mActivityState.deleteAvatarStatusId != null)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_STATUS, mActivityState.deleteAvatarStatusId);
					ContactInfo contactInfo = Utils.getUserContactInfo(prefs.getPref());
					StatusMessageType[] smType = { StatusMessageType.PROFILE_PIC };
					StatusMessage lastsm = HikeConversationsDatabase.getInstance().getLastStatusMessage(smType, contactInfo);

					if (lastsm != null && mActivityState.deleteAvatarStatusId.equals(lastsm.getMappedId()))
					{
						displayPictureRemoved(mActivityState.deleteAvatarStatusId);
					}
				}
				clearDpUpdatePref();
				HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, mLocalMSISDN);
				dismissDialog();
				mActivityState.deleteAvatarToken = null;
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{

			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.d("ProfileActivity", "delete dp request failed!");
				dismissDialog();
				mActivityState.deleteAvatarToken = null;
			}
		};
		return deleteAvatarRequestListener;
	}
	
	public void beginProfilePicChange(android.content.DialogInterface.OnClickListener listener, Context context, String removeImagePath, boolean useTimestamp)
	{
		ContactInfo contactInfo = Utils.getUserContactInfo(prefs.getPref());

		// check if msisdn is not a group id and if it already has an icon (force check to avoid stale state)
		if (!OneToNConversationUtils.isOneToNConversation(mLocalMSISDN) && ContactManager.getInstance().hasIcon(contactInfo.getMsisdn()))
		{
			// case when we need to show dialog for change dp and remove dp 
			showProfileImageEditDialog(this, context, removeImagePath);
		}
		else
		{
			// directly open gallery to allow user to select new image
			selectNewProfilePicture(context, !OneToNConversationUtils.isOneToNConversation(mLocalMSISDN), useTimestamp);
		}
	}

	/**
	 * Used to show a dialog to the user to modify his/her current display picture
	 * 
	 * @param removeImagePath
	 *            describes which activity wants to remove profile picture
	 */
	public void showProfileImageEditDialog(android.content.DialogInterface.OnClickListener listener, Context ctx, String removeImagePath)
	{
		mRemoveImagePath = removeImagePath;

		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

		// We have a single element array so that if new options are added to the dialog we just have to add strings to this array.
		final CharSequence[] items = ctx.getResources().getStringArray(R.array.profile_pic_dialog);

		// Show Remove Photo item only if user has a profile photo other than default

		CharSequence[] moreItems = new CharSequence[items.length + 1]; // adding one item to the existing list

		for (int i = 0; i < items.length; i++)
			moreItems[i] = items[i];

		moreItems[moreItems.length - 1] = ctx.getResources().getString(R.string.remove_photo);
		builder.setItems(moreItems, listener);
		builder.show();
	}

	/**
	 * Used as a callback for image crop action
	 * 
	 * @return path of the image file
	 */
	public String profileImageCropped()
	{
		return mActivityState.destFilePath;
	}

	/**
	 * Used to scale down the dp bitmap to send to server
	 * 
	 * @return bitmap byte array
	 */
	private byte[] scaleDownBitmap()
	{
		byte[] bytes = null;

		/* the server only needs a smaller version */
		final Bitmap smallerBitmap = HikeBitmapFactory.scaleDownBitmap(mActivityState.destFilePath, HikeConstants.PROFILE_IMAGE_DIMENSIONS, HikeConstants.PROFILE_IMAGE_DIMENSIONS,
				Bitmap.Config.RGB_565, true, false);

		if (smallerBitmap == null)
		{
			failureWhileSettingProfilePic();
			return bytes;
		}
		bytes = BitmapUtils.bitmapToBytes(smallerBitmap, Bitmap.CompressFormat.JPEG, 100);

		return bytes;
	}

	/**
	 * Used to upload profile picture to the server, compose related timeline post
	 * @param msisdn 
	 */
	public void uploadProfilePicture(String msisdn)
	{
		if (mActivityState.destFilePath != null)
		{
			final byte[] bytes = scaleDownBitmap();

			if (bytes == null)
				return;

		    // If the Fragment is non-null, then it is currently being
		    // retained across a configuration change.
	    	Logger.d(TAG, "starting new mImageLoaderFragment");
	    	mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));
	    	mActivityState.mImageWorkerFragment = HikeImageUploader.newInstance(bytes, mActivityState.destFilePath, msisdn, true, true);
	    	mActivityState.mImageWorkerFragment.setTaskCallbacks(this);
	    	mActivityState.mImageWorkerFragment.startUpLoadingTask();
		}
	}
	
	@Override
	public void onSuccess(Response result)
	{
		Logger.d(TAG, "inside onSuccess of request");
		dismissDialog();
		
		mActivityState.destFilePath = null;
		
		JSONObject response = (JSONObject) result.getBody().getContent();
		StatusMessage statusMessage = Utils.createTimelinePostForDPChange(response);
		
		if (statusMessage != null)
		{
			mActivityState.mImageWorkerFragment.doContactManagerIconChange(statusMessage.getMappedId(), scaleDownBitmap(), true);
			
			int unseenUserStatusCount = prefs.getData(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
			Editor editor = prefs.getPref().edit();
			editor.putInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, ++unseenUserStatusCount);
			editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
			editor.commit();

			/*
			 * This would happen in the case where the user has added a self contact and received an mqtt message before saving this to the db.
			 */
			if (statusMessage.getId() != -1)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
				HikeMessengerApp.getPubSub().publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
			}
		}
		
		HikeMessengerApp.getLruCache().clearIconForMSISDN(mLocalMSISDN);
		HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, mLocalMSISDN);

		profilePictureUploaded();

	}

	/**
	 * Used as a callback for successful picture upload to server
	 */
	public void profilePictureUploaded()
	{
		return;
	}

	/**
	 * Used as a failure callback while upload picture to the server
	 */
	private void failureWhileSettingProfilePic()
	{
		dismissDialog();
		mActivityState.destFilePath = null;
	}

	private void dismissDialog()
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
	}

	/**
	 * Used as a callback on successful picture removal
	 * 
	 * @param id
	 *            of the status message
	 */
	public void displayPictureRemoved(final String id)
	{
		return;
	}

	@Override
	public void onDisplayPictureChangeClicked(int whichActivity)
	{
		sendDPEditAnalytics(whichActivity);
		// directly open gallery to allow user to select new image
		selectNewProfilePicture(this, !OneToNConversationUtils.isOneToNConversation(mLocalMSISDN), true);
	}

	@Override
	public void onDisplayPictureRemoveClicked(int whichActivity) {
		if (whichActivity == ImageViewerFragment.FROM_PROFILE_ACTIVITY) {
			mRemoveImagePath = ProfileImageActions.DP_EDIT_FROM_DISPLAY_IMAGE;
		} else if (whichActivity == ImageViewerFragment.FROM_SETTINGS_ACTIVITY) {
			mRemoveImagePath = ProfileImageActions.DP_EDIT_FROM_SETTINGS_PREVIEW_IMAGE;
		} else if (whichActivity == ImageViewerFragment.FROM_EDIT_DP_ACTIVITY) {
			mRemoveImagePath = ProfileImageActions.DP_EDIT_FROM_EDIT_DP_SCREEN;
		}
		sendDPEditAnalytics(whichActivity);
		showRemovePhotoConfirmDialog();
	}

	private void sendDPEditAnalytics(int whichActivity) {
		JSONObject md = new JSONObject();

		try {
			md.put(HikeConstants.EVENT_KEY, ProfileImageActions.DP_EDIT_EVENT);

			if (whichActivity == ImageViewerFragment.FROM_PROFILE_ACTIVITY) {
				md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_EDIT_FROM_DISPLAY_IMAGE);
			} else if (whichActivity == ImageViewerFragment.FROM_SETTINGS_ACTIVITY) {
				md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_EDIT_FROM_SETTINGS_PREVIEW_IMAGE);
			}
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, md);
		} catch (JSONException e) {
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "json exception");
		}

	}

	/**
	 * Used to clear the pref used to save status id of the dp change status update
	 */
	public void clearDpUpdatePref()
	{
		Editor ed = prefs.getPref().edit();
		ed.putString(HikeMessengerApp.DP_CHANGE_STATUS_ID, "");
		ed.commit();
	}

	/**
	 * Sets the local msisdn for the profile
	 * 
	 * @param msisdn
	 */
	protected void setLocalMsisdn(String msisdn)
	{
		this.mLocalMSISDN = msisdn;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		dismissDialog();
		if (mActivityState != null && mActivityState.deleteAvatarToken != null)
		{
			mActivityState.deleteAvatarToken.removeListener(deleteAvatarRequestListener);
		}
	}
	
	public void onProgressUpdate(float percent)
	{
		
	}

	@Override
	public void onCancelled()
	{
		onFailed();
	}
	
	@Override
	public void onFailed()
	{
		Logger.d(TAG, "req failed");
		failureWhileSettingProfilePic();
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), getString(R.string.update_profile_failed), Toast.LENGTH_LONG).show();
				dismissDialog();
			}
		});
		
	}

	@Override
	public void onTaskAlreadyRunning() {
	runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), getString(R.string.task_already_running), Toast.LENGTH_LONG).show();
					dismissDialog();
				}
			});
	}
	
	protected void showLinkShareView(String grpId, String grpName, int grpSettings, boolean isNewGroup)
	{
		android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(ShareLinkFragment.SHARE_LINK_FRAGMENT_TAG);
		if (prev != null)
		{
			ft.remove(prev);
			ft.commitAllowingStateLoss();
			fm.executePendingTransactions();
			ft = fm.beginTransaction();
		}
		
		// Create and show the dialog.
		mActivityState.shareLinkFragment = ShareLinkFragment.newInstance(grpId, grpName, grpSettings, isNewGroup, false);
		mActivityState.shareLinkFragment.show(ft, ShareLinkFragment.SHARE_LINK_FRAGMENT_TAG);
		
		fm.executePendingTransactions();
	}

	@Override
	public void addMembersViaHike()
	{
		// TODO Auto-generated method stub
		
	}

	protected abstract @HomeAnalyticsConstants.ProfilePicUpdateSpecies String getSourceSpecies();
}
