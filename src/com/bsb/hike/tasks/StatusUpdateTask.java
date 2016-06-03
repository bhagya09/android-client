package com.bsb.hike.tasks;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HomeAnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class StatusUpdateTask implements IHikeHTTPTask
{
	private boolean compressionEnabled = true;

	private Bitmap bmp;

	private String status;

	private int moodId;

	private RequestToken token;

	private String imageFilePath;

	public static final int TASK_IDLE = 0;

	public static final int TASK_RUNNING = 1;

	public static final int TASK_SUCCESS = 2;

	public static final int TASK_FAILED = 3;

	private int taskStatus = TASK_IDLE;

	private StatusMessageType mSUType;

	private String mGenus = HomeAnalyticsConstants.SU_GENUS_OTHER;

	private String mSpecies = HomeAnalyticsConstants.SU_SPECIES_OTHER;

	public StatusUpdateTask(String status, int argMoodId, String imageFilePath)
	{
		this(status, argMoodId, imageFilePath, null);
	}

	public StatusUpdateTask(String status, int argMoodId, String imageFilePath, Bitmap bmp)
	{
		this(status, argMoodId, imageFilePath, bmp, true);
	}

	public StatusUpdateTask(String status, int argMoodId, String imageFilePath, Bitmap bmp, boolean enableCompression)
	{
		this.status = status;
		this.moodId = argMoodId;
		this.imageFilePath = imageFilePath;
		this.bmp = bmp;
		this.compressionEnabled = enableCompression;

		if(TextUtils.isEmpty(imageFilePath) && bmp == null)
		{
			// Text Update
			mSUType = StatusMessageType.TEXT;
		}
		else if(TextUtils.isEmpty(status))
		{
			// Photo update
			mSUType = StatusMessageType.IMAGE;
		}
		else
		{
			// Text + photo update
			mSUType = StatusMessageType.TEXT_IMAGE;
		}
	}

	@Override
	public void execute()
	{
		HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					// Compression
					if (compressionEnabled && !TextUtils.isEmpty(imageFilePath))
					{
						Bitmap sourceBitmap = null;
						if (bmp != null)
						{
							sourceBitmap = bmp;
						}
						else if (!TextUtils.isEmpty(imageFilePath) && new File(imageFilePath).exists())
						{
							if (bmp != null)
							{
								sourceBitmap = bmp;
							}
							else
							{
								sourceBitmap = HikeBitmapFactory.decodeSampledBitmapFromFile(imageFilePath, (HikeConstants.HikePhotos.MAX_IMAGE_DIMEN),
										(HikeConstants.HikePhotos.MAX_IMAGE_DIMEN), Bitmap.Config.ARGB_8888, HikePhotosUtils.getLoadingOptionsAdvanced(), false);
								sourceBitmap = Utils.getRotatedBitmap(imageFilePath, sourceBitmap);

								if (sourceBitmap == null)
								{
									sourceBitmap = HikeBitmapFactory.decodeSampledBitmapFromFile(imageFilePath, (HikeConstants.HikePhotos.MAX_IMAGE_DIMEN),
											(HikeConstants.HikePhotos.MAX_IMAGE_DIMEN), Bitmap.Config.RGB_565, HikePhotosUtils.getLoadingOptionsAdvanced(), false);
									sourceBitmap = Utils.getRotatedBitmap(imageFilePath, sourceBitmap);
								}
							}
						}

						if (sourceBitmap != null)
						{
							File tempFile = File.createTempFile(Long.toString(System.currentTimeMillis()), ".jpg", HikeMessengerApp.getInstance().getCacheDir());

							sourceBitmap = HikePhotosUtils.scaleAdvanced(sourceBitmap, HikeConstants.HikePhotos.MAX_IMAGE_DIMEN, HikeConstants.HikePhotos.MAX_IMAGE_DIMEN, false);
							if (sourceBitmap == null)
							{
								sourceBitmap = HikePhotosUtils.scaleAdvanced(sourceBitmap, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX,
										HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX, false);
							}

							if (sourceBitmap == null)
							{
								getRequestListener().onRequestFailure(null, null);
								return;
							}

							BitmapUtils.saveBitmapToFile(tempFile, sourceBitmap, Bitmap.CompressFormat.JPEG, 80);
							imageFilePath = tempFile.getAbsolutePath();
						}
						else
						{
							getRequestListener().onRequestFailure(null, null);
							return;
						}
					}
					token = HttpRequests.postStatusRequest(status, moodId, getRequestListener(), imageFilePath);

					if (token == null)
					{
						getRequestListener().onRequestFailure(null, null);
						return;
					}
					token.execute();
				}
				catch (IOException ioe)
				{
					Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.could_not_post_pic, Toast.LENGTH_SHORT).show();
					ioe.printStackTrace();
					getRequestListener().onRequestFailure(null, null);
				}
			}
		}, 0);
	}

	@Override
	public void cancel()
	{
		token.cancel();
	}

	@Override
	public Bundle getRequestBundle()
	{
		return null;
	}

	@Override
	public String getRequestId()
	{
		return null;
	}

    private IRequestListener requestListener = null;

	public IRequestListener getRequestListener()
	{
		if (requestListener == null)
		{
			requestListener = new IRequestListener()
			{
				@Override
				public void onRequestSuccess(Response result)
				{
					JSONObject response = (JSONObject) result.getBody().getContent();
					Logger.d(getClass().getSimpleName(), "post status request succeeded : " + response);
					SharedPreferences preferences = HikeMessengerApp.getInstance().getApplicationContext()
							.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE);
					JSONObject data = response.optJSONObject("data");
					String mappedId = data.optString(HikeConstants.STATUS_ID);
					String text = data.optString(HikeConstants.STATUS_MESSAGE);
					int moodId = data.optInt(HikeConstants.MOOD) - 1;

					int timeOfDay = data.optInt(HikeConstants.TIME_OF_DAY);
					String msisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");
					String name = preferences.getString(HikeMessengerApp.NAME_SETTING, "");
					long time = (long) System.currentTimeMillis() / 1000;

					final String iconBase64 = data.optString(HikeConstants.THUMBNAIL);

					final String fileKey = data.optString(HikeConstants.SU_IMAGE_KEY);

					StatusMessageType suType = getStatusMessageType(text, imageFilePath);

					if (suType == StatusMessageType.TEXT_IMAGE || suType == StatusMessageType.IMAGE)
					{
						// TODO Support all image formats (same code is present in ChangeProfileImageBaseActivity)
						String destFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + mappedId + ".jpg";

						Utils.copyFile(imageFilePath, destFilePath);

						if (compressionEnabled || Utils.isFileInSameDirectory(imageFilePath, destFilePath))
						{
							Utils.deleteFile(new File(imageFilePath));
						}

						HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
						{
							@Override
							public void run()
							{
								// Add to fileThumbnail table viz. fk - thumb
								HikeConversationsDatabase.getInstance().addFileThumbnail(fileKey, Base64.decode(iconBase64, Base64.DEFAULT));
							}
						}, 0);
					}

					StatusMessage statusMessage = new StatusMessage(0, mappedId, msisdn, name, text, suType, time, moodId, timeOfDay, fileKey);
					HikeConversationsDatabase.getInstance().addStatusMessage(statusMessage, true);
					int unseenUserStatusCount = preferences.getInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
					Editor editor = preferences.edit();
					editor.putString(HikeMessengerApp.LAST_STATUS, text);
					editor.putInt(HikeMessengerApp.LAST_MOOD, moodId);
					editor.putInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, ++unseenUserStatusCount);
					editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
					editor.commit();
					HikeMessengerApp.getPubSub().publish(HikePubSub.MY_STATUS_CHANGED, text);

					recordStatusUpdateSource();

					/*
					 * This would happen in the case where the user has added a self contact and received an mqtt message before saving this to the db.
					 */
					if (statusMessage.getId() != -1)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
						HikeMessengerApp.getPubSub().publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
					}
					HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_POST_REQUEST_DONE, true);
					taskStatus = TASK_SUCCESS;
				}

				private StatusMessageType getStatusMessageType(String text, String imageFilePath)
				{
					// TODO server sends string "null" incase text is not present

					if (text.equals("null"))
					{
						text = null;
					}
					if (TextUtils.isEmpty(text) && TextUtils.isEmpty(imageFilePath))
					{
						throw new IllegalArgumentException("Both text and image cannot be empty");
					}

					File imageFile = null;
					if (!TextUtils.isEmpty(imageFilePath))
					{
						imageFile = new File(imageFilePath);
						if (!imageFile.exists())
						{
							imageFile = null;
						}
					}

					if (TextUtils.isEmpty(text) && imageFile != null)
					{
						return StatusMessageType.IMAGE;
					}
					else if (!TextUtils.isEmpty(text) && imageFile == null)
					{
						return StatusMessageType.TEXT;
					}
					else
					{
						return StatusMessageType.TEXT_IMAGE;
					}

				}

				@Override
				public void onRequestProgressUpdate(float progress)
				{
				}

				@Override
                public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
				{
					if (httpException != null)
					{
						Logger.e(getClass().getSimpleName(), " post status request failed : " + httpException.getMessage());
					}
					if (compressionEnabled && !TextUtils.isEmpty(imageFilePath))
						Utils.deleteFile(new File(imageFilePath));
					HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_POST_REQUEST_DONE, false);
					taskStatus = TASK_FAILED;
				}
			};
		}

		return requestListener;
	}

	public int getTaskStatus()
	{
		if (token != null && token.isRequestRunning())
		{
			return TASK_RUNNING;
		}
		return taskStatus;
	}

	private void recordStatusUpdateSource()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, HomeAnalyticsConstants.TIMELINE_UK);
			json.put(AnalyticsConstants.V2.KINGDOM, HomeAnalyticsConstants.HOMESCREEN_KINGDOM);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, HomeAnalyticsConstants.ORDER_STATUS_UPDATE);
			json.put(AnalyticsConstants.V2.FAMILY, getAnalyticsFamilyName());
			json.put(AnalyticsConstants.V2.GENUS, mGenus);
			json.put(AnalyticsConstants.V2.SPECIES, mSpecies);
			HAManager.getInstance().recordV2(json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public void setGenus(@HomeAnalyticsConstants.StatusUpdateGenus String argGenus)
	{
		mGenus = argGenus;
	}

	public void setSpecies(@HomeAnalyticsConstants.StatusUpdateSpecies String argSpecies)
	{
		mSpecies = argSpecies;
	}

	private String getAnalyticsFamilyName()
	{
		switch (mSUType)
		{
		case IMAGE:
			return HomeAnalyticsConstants.SU_TYPE_IMAGE;

		case TEXT_IMAGE:
			return HomeAnalyticsConstants.SU_TYPE_TEXT_IMAGE;

		case TEXT:
			return HomeAnalyticsConstants.SU_TYPE_TEXT;

		default:
			return HomeAnalyticsConstants.SU_TYPE_OTHER;

		}
	}
}
