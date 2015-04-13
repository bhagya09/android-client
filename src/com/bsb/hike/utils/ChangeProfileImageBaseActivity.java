package com.bsb.hike.utils;

import java.io.File;
import java.net.URI;

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
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
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
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.tasks.DownloadImageTask;
import com.bsb.hike.tasks.DownloadImageTask.ImageDownloadResult;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
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

	@Override
	public void onClick(DialogInterface dialog, int item)
	{
		Intent intent = null;
		switch (item)
		{
		case HikeConstants.PROFILE_PICTURE_FROM_CAMERA:
			if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
			{
				Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
				return;
			}
			if (!Utils.hasEnoughFreeSpaceForProfilePic())
			{
				Toast.makeText(getApplicationContext(), R.string.not_enough_space_profile_pic, Toast.LENGTH_SHORT).show();
				return;
			}
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			File selectedFileIcon = Utils.getOutputMediaFile(HikeFileType.PROFILE, null, false); // create a file to save
			
			// the image
			if (selectedFileIcon != null)
			{
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(selectedFileIcon));

				/*
				 * Saving the file path. Will use this to get the file once the image has been captured.
				 */
				Editor editor = prefs.getPref().edit();
				editor.putString(HikeMessengerApp.FILE_PATH, selectedFileIcon.getAbsolutePath());
				editor.commit();

				startActivityForResult(intent, HikeConstants.CAMERA_RESULT);
			}
			else
			{
				Toast.makeText(this, getString(R.string.no_sd_card), Toast.LENGTH_LONG).show();
			}
			break;
		case HikeConstants.PROFILE_PICTURE_FROM_GALLERY:
			if (Utils.getExternalStorageState() == ExternalStorageState.NONE)
			{
				Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
				return;
			}
			if (!Utils.hasEnoughFreeSpaceForProfilePic())
			{
				Toast.makeText(getApplicationContext(), R.string.not_enough_space_profile_pic, Toast.LENGTH_SHORT).show();
				return;
			}
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, HikeConstants.GALLERY_RESULT);
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

			switch(mRemoveImagePath)
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
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "json exception");
		}
		final CustomAlertDialog deleteConfirmDialog = new CustomAlertDialog(this);
		deleteConfirmDialog.setHeader(R.string.remove_photo);
		deleteConfirmDialog.setBody(R.string.confirm_remove_photo);
		
		View.OnClickListener dialogOkClickListener = new View.OnClickListener()
		{
			JSONObject md = new JSONObject();
			
			@Override
			public void onClick(View v)
			{
				// if checkbox is selected, delete the profile status update from own and favorites timeline
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
		deleteConfirmDialog.setCheckBox(R.string.check_delete_from_timeline, false);
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

	/**
	 * Used to show a dialog to the user to modify his/her current display picture
	 * @param removeImagePath describes which activity wants to remove profile picture
	 * @param groupId if its a group conversation, groupId is non-null
	 */
	public void showProfileImageEditDialog(android.content.DialogInterface.OnClickListener listener, Context ctx, String msisdn, String removeImagePath)
	{
		mRemoveImagePath = removeImagePath;		
 		mLocalMSISDN = msisdn;
 		
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.profile_photo);

		final CharSequence[] items = ctx.getResources().getStringArray(R.array.profile_pic_dialog);

		// Show Remove Photo item only if user has a profile photo other than default
		ContactInfo contactInfo = Utils.getUserContactInfo(prefs.getPref());

		if (!Utils.isGroupConversation(mLocalMSISDN) && ContactManager.getInstance().hasIcon(contactInfo.getMsisdn()))
		{
			CharSequence[] moreItems = new CharSequence[items.length + 1]; // adding one item to the existing list

			for (int i = 0; i < items.length; i++)
				moreItems[i] = items[i];

			moreItems[moreItems.length - 1] = ctx.getResources().getString(R.string.remove_photo);
			builder.setItems(moreItems, listener);
		}
		else
		{
			builder.setItems(items, listener);
		}
		builder.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		String path = null;
		File selectedFileIcon = null;

		if (resultCode != RESULT_OK)
		{
			return;
		}

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
		final String destFilePath = directory + "/" + fileName;

		switch (requestCode)
		{
		case HikeConstants.CAMERA_RESULT:
			Logger.d("ProfileActivity", "The activity is " + this);
			String filePath = prefs.getData(HikeMessengerApp.FILE_PATH, "");
			selectedFileIcon = new File(filePath);

			/*
			 * Removing this key. We no longer need this.
			 */
			Editor editor = prefs.getPref().edit();
			editor.remove(HikeMessengerApp.FILE_PATH);
			editor.commit();

			if (!selectedFileIcon.exists())
			{
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			path = selectedFileIcon.getAbsolutePath();

			if (TextUtils.isEmpty(path))
			{
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			Utils.startCropActivity(this, path, destFilePath);
			break;

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
				String fileUriStart = "file://";
				String fileUriString = selectedFileUri.toString();
				if (fileUriString.startsWith(fileUriStart))
				{
					selectedFileIcon = new File(URI.create(Utils.replaceUrlSpaces(fileUriString)));
					/*
					 * Done to fix the issue in a few Sony devices.
					 */
					path = selectedFileIcon.getAbsolutePath();
				}
				else
				{
					path = Utils.getRealPathFromUri(selectedFileUri, this);
				}
			}
			if (TextUtils.isEmpty(path))
			{
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			if (!isPicasaImage)
			{
				Utils.startCropActivity(this, path, destFilePath);
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
							Utils.startCropActivity(getParent(), destFile.getAbsolutePath(), destFilePath);
						}
					}
				});
				Utils.executeBoolResultAsyncTask(mActivityState.downloadPicasaImageTask);
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

		case HikeConstants.CROP_RESULT:
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
			
			if(bytes == null)
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

					/*
					 * Making the profile pic change a status message.
					 */
					JSONObject data = response.optJSONObject("status");

					if (data == null)
					{
						return;
					}

					String mappedId = data.optString(HikeConstants.STATUS_ID);
					String msisdn = prefs.getData(HikeMessengerApp.MSISDN_SETTING, "");
					String name = prefs.getData(HikeMessengerApp.NAME_SETTING, "");
					long time = (long) System.currentTimeMillis() / 1000;

					// saving mapped status id for this dp change. delete update will clear this pref later
					// this pref's current value will decide whether to give option to user to delete dp post from favourites timelines or not
					Editor ed = prefs.getPref().edit();
					ed.putString(HikeMessengerApp.DP_CHANGE_STATUS_ID, mappedId);
					ed.commit();
					
					StatusMessage statusMessage = new StatusMessage(0, mappedId, msisdn, name, "", StatusMessageType.PROFILE_PIC, time, -1, 0);
					HikeConversationsDatabase.getInstance().addStatusMessage(statusMessage, true);

					ContactManager.getInstance().setIcon(statusMessage.getMappedId(), bytes, false);

					String srcFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + msisdn + ".jpg";

					String destFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + mappedId + ".jpg";

					/*
					 * Making a status update file so we don't need to download this file again.
					 */
					Utils.copyFile(srcFilePath, destFilePath, null);

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

			if(whichActivity == ImageViewerFragment.FROM_PROFILE_ACTIVITY)
			{
				imageRemovePath = ProfileImageActions.DP_EDIT_FROM_DISPLAY_IMAGE;
				md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_EDIT_FROM_DISPLAY_IMAGE);
			}
			else if(whichActivity == ImageViewerFragment.FROM_SETTINGS_ACTIVITY)
			{
				md.put(ProfileImageActions.DP_EDIT_PATH, ProfileImageActions.DP_EDIT_FROM_SETTINGS_PREVIEW_IMAGE);
				imageRemovePath = ProfileImageActions.DP_EDIT_FROM_SETTINGS_PREVIEW_IMAGE;		
			}
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, md);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "json exception");
		}
		String msisdn = prefs.getPref().getString(HikeMessengerApp.MSISDN_SETTING, null);
		showProfileImageEditDialog(ChangeProfileImageBaseActivity.this, ChangeProfileImageBaseActivity.this, msisdn, imageRemovePath);		
	}
}
