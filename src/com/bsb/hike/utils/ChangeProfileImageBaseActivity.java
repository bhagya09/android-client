package com.bsb.hike.utils;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.AnalyticsConstants.ProfileImageActions;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.CustomAlertDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.tasks.DownloadImageTask;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.fragments.ImageViewerFragment;
import com.bsb.hike.ui.fragments.ImageViewerFragment.DisplayPictureEditListener;
import com.bsb.hike.utils.Utils.ExternalStorageState;

public class ChangeProfileImageBaseActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener, FinishableEvent, DisplayPictureEditListener
{
	private HikeSharedPreferenceUtil prefs;

	private String mLocalMSISDN;

	private Dialog mDialog;

	public class ActivityState
	{
		public HikeHTTPTask task; /* the task to update the global profile */

		public DownloadImageTask downloadPicasaImageTask; /*
														 * the task to download the picasa image
														 */

		public HikeHTTPTask getHikeJoinTimeTask;

		public String destFilePath = null; /*
											 * the bitmap before the user saves it
											 */

		public int genderType;

		public boolean groupEditDialogShowing = false;

		public String edittedGroupName = null;
	}

	private ActivityState mActivityState;

	private String mRemoveImagePath;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		prefs = HikeSharedPreferenceUtil.getInstance();

		Object obj = getLastCustomNonConfigurationInstance();

		if (obj instanceof ActivityState)
		{
			mActivityState = (ActivityState) obj;

			if (mActivityState.task != null)
			{
				/* we're currently executing a task, so show the progress dialog */
				mActivityState.task.setActivity(this);
				mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));
			}
		}
		else
		{
			mActivityState = new ActivityState();
		}
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		Logger.d("ChangeProfileImageBaseActivity", "onRetainNonConfigurationinstance");
		return mActivityState;
	}

	public void selectNewProfilePicture(Context context, boolean isPersonal)
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

		boolean editPic = Utils.isPhotosEditEnabled();
		
		Intent galleryPickerIntent = null; 
		
		int galleryFlags = GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS|GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM;

		if (editPic)
		{
			galleryFlags = galleryFlags | GalleryActivity.GALLERY_EDIT_SELECTED_IMAGE|GalleryActivity.GALLERY_COMPRESS_EDITED_IMAGE;
			if (!isPersonal)
			{
				galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(ChangeProfileImageBaseActivity.this,galleryFlags,getNewProfileImagePath());
				startActivityForResult(galleryPickerIntent, HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE);
			}
			else
			{
				galleryFlags = galleryFlags | GalleryActivity.GALLERY_FOR_PROFILE_PIC_UPDATE;
				galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(ChangeProfileImageBaseActivity.this,galleryFlags,null);
				startActivity(galleryPickerIntent);
			}
		}
		else
		{
			galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(ChangeProfileImageBaseActivity.this, galleryFlags,getNewProfileImagePath());
			galleryPickerIntent.putExtra(GalleryActivity.START_FOR_RESULT, true);
			startActivityForResult(galleryPickerIntent, HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE);
		}

	}
	
	protected String getNewProfileImagePath()
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

		String fileName = Utils.getTempProfileImageFileName(mLocalMSISDN);
		String destFilePath = directory + File.separator + fileName;
		return destFilePath;

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK)
		{
			return;
		}
		
		switch (requestCode)
		{

		case HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE:
			mActivityState.destFilePath = data.getStringExtra(MediaStore.EXTRA_OUTPUT);

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
		switch (item)
		{
		case HikeConstants.NEW_PROFILE_PICTURE:
			selectNewProfilePicture(ChangeProfileImageBaseActivity.this, !OneToNConversationUtils.isOneToNConversation(mLocalMSISDN));
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

		final CustomAlertDialog deleteConfirmDialog = new CustomAlertDialog(this, HikeDialogFactory.REMOVE_DP_CONFIRM_DIALOG);
		deleteConfirmDialog.setHeader(R.string.remove_photo);
		deleteConfirmDialog.setBody(R.string.confirm_remove_photo);
		deleteConfirmDialog.setCheckBox(R.string.check_delete_from_timeline, false);

		// if checkbox is selected, delete the profile status update from own and favorites timeline
		String dpStatusId = prefs.getPref().getString(HikeMessengerApp.DP_CHANGE_STATUS_ID, "");

		if (dpStatusId.isEmpty())
		{
			deleteConfirmDialog.setCheckboxVisibility(View.GONE);
		}

		View.OnClickListener dialogOkClickListener = new View.OnClickListener()
		{
			JSONObject md = new JSONObject();

			@Override
			public void onClick(View v)
			{
				// if checkbox is selected, delete the profile status update from own and favorites timelines
				if (deleteConfirmDialog.isChecked())
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
				deleteConfirmDialog.dismiss();
			}
		};
		deleteConfirmDialog.setOkButton(R.string.yes, dialogOkClickListener);
		deleteConfirmDialog.setCancelButton(R.string.no);
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
		String statusId = (id == null) ? "" : (HikeConstants.HTTP_STATUS_ID + id);

		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/account/avatar" + statusId, RequestType.DELETE_DP, new HikeHttpCallback()
		{
			@Override
			public void onSuccess(JSONObject response)
			{
				Logger.d("ProfileActivity", "delete dp request succeeded!");

				// clear the profile thumbnail from lru cache and db
				HikeMessengerApp.getLruCache().deleteIconForMSISDN(mLocalMSISDN);

				if (id != null)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_STATUS, id);
					ContactInfo contactInfo = Utils.getUserContactInfo(prefs.getPref());
					StatusMessageType[] smType = { StatusMessageType.PROFILE_PIC };
					StatusMessage lastsm = HikeConversationsDatabase.getInstance().getLastStatusMessage(smType, contactInfo);

					if (lastsm != null && id.equals(lastsm.getMappedId()))
					{
						displayPictureRemoved(id);
					}
				}
				clearDpUpdatePref();
				HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, mLocalMSISDN);
			}

			@Override
			public void onFailure()
			{
				Logger.d("ProfileActivity", "delete dp request failed!");
			}
		});
		mActivityState.task = new HikeHTTPTask(this, R.string.remove_dp_error);
		Utils.executeHttpTask(mActivityState.task, hikeHttpRequest);
		mDialog = ProgressDialog.show(this, null, getString(R.string.removing_dp));
	}

	public void beginProfilePicChange(android.content.DialogInterface.OnClickListener listener, Context context, String removeImagePath)
	{
		ContactInfo contactInfo = Utils.getUserContactInfo(prefs.getPref());
		if (!OneToNConversationUtils.isOneToNConversation(mLocalMSISDN) && ContactManager.getInstance().hasIcon(contactInfo.getMsisdn()))
		{
			// case when we need to show dialog for change dp and remove dp 
			showProfileImageEditDialog(this, context, removeImagePath);
		}
		else
		{
			// directly open gallery to allow user to select new image
			selectNewProfilePicture(context, !OneToNConversationUtils.isOneToNConversation(mLocalMSISDN));
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
		builder.setTitle(R.string.profile_photo);

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
	 * 
	 * @param httpApi
	 *            TODO
	 */
	public void uploadProfilePicture(String httpApi)
	{
		if (mActivityState.destFilePath != null)
		{
			final byte[] bytes = scaleDownBitmap();

			if (bytes == null)
				return;

			HikeHttpRequest request = new HikeHttpRequest(httpApi, RequestType.PROFILE_PIC, new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{
					Logger.d("ProfileActivity", "resetting image");
					failureWhileSettingProfilePic();
				}

				public void onSuccess(JSONObject response)
				{
					mActivityState.destFilePath = null;
					ContactManager.getInstance().setIcon(mLocalMSISDN, bytes, false);
					Utils.renameTempProfileImage(mLocalMSISDN);
					StatusMessage statusMessage = Utils.createTimelinePostForDPChange(response);

					if (statusMessage == null)
					{
						return;
					}
					ContactManager.getInstance().setIcon(statusMessage.getMappedId(), bytes, false);

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
					HikeMessengerApp.getLruCache().clearIconForMSISDN(mLocalMSISDN);
					HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, mLocalMSISDN);

					profilePictureUploaded();
				}
			});
			request.setFilePath(mActivityState.destFilePath);
			mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));
			mActivityState.task = new HikeHTTPTask(this, R.string.update_profile_failed);
			Utils.executeHttpTask(mActivityState.task, request);
		}
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
		Utils.removeTempProfileImage(mLocalMSISDN);
		mActivityState.destFilePath = null;
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
	public void onFinish(boolean success)
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
		mActivityState.task = null;
	}

	@Override
	public void onDisplayPictureEditClicked(int whichActivity)
	{
		String imageRemovePath = null;
		JSONObject md = new JSONObject();

		try
		{
			md.put(HikeConstants.EVENT_KEY, ProfileImageActions.DP_EDIT_EVENT);

			if (whichActivity == ImageViewerFragment.FROM_PROFILE_ACTIVITY)
			{
				imageRemovePath = ProfileImageActions.DP_EDIT_FROM_DISPLAY_IMAGE;
				md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_EDIT_FROM_DISPLAY_IMAGE);
			}
			else if (whichActivity == ImageViewerFragment.FROM_SETTINGS_ACTIVITY)
			{
				md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_EDIT_FROM_SETTINGS_PREVIEW_IMAGE);
				imageRemovePath = ProfileImageActions.DP_EDIT_FROM_SETTINGS_PREVIEW_IMAGE;
			}
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, md);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "json exception");
		}
		showProfileImageEditDialog(ChangeProfileImageBaseActivity.this, ChangeProfileImageBaseActivity.this, imageRemovePath);
	}

	/**
	 * Used to clear the pref used to save status id of the dp change status update
	 * 
	 * @param statusId
	 *            of the status update
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
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
		super.onDestroy();
	}
}
