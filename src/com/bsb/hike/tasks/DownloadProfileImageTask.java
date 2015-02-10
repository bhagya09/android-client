package com.bsb.hike.tasks;

import java.io.File;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;

public class DownloadProfileImageTask
{
	private Context context;

	private String id;

	private String fileName;

	private String filePath;

	private String msisdn;

	private String name;

	private boolean statusImage;

	private boolean showToast;

	private boolean hasCustomIcon;

	private String urlString;

	public DownloadProfileImageTask(Context context, String id, String fileName, boolean hasCustomIcon, boolean statusImage, String msisdn, String name, boolean showToast)
	{
		this(context, id, fileName, hasCustomIcon, statusImage, msisdn, name, showToast, null);
	}

	public DownloadProfileImageTask(Context context, String id, String fileName, boolean hasCustomIcon, boolean statusImage, String msisdn, String name,
			boolean showToast, String url)
	{
		this.context = context;
		this.id = id;
		this.msisdn = msisdn;
		this.statusImage = statusImage;
		this.name = name;
		this.filePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		this.fileName = filePath + "/" + Utils.getTempProfileImageFileName(id);
		this.showToast = showToast;
		this.hasCustomIcon = hasCustomIcon;
		this.urlString = url;
	}

	public void execute()
	{
		File dir = new File(filePath);
		if (!dir.exists())
		{
			if (!dir.mkdirs())
			{
				doOnFailure();
				return;
			}
		}

		RequestToken token;
		if (TextUtils.isEmpty(urlString))
		{
			if (statusImage)
			{
				token = HttpRequests.downloadStatusImageRequest(id, fileName, requestListener);
			}
			else
			{
				boolean isGroupConversation = OneToNConversationUtils.isGroupConversation(msisdn);
				token = HttpRequests.downloadProfileImageRequest(id, fileName, hasCustomIcon, isGroupConversation, requestListener);
			}
		}
		else
		{
			token = HttpRequests.downloadProtipRequest(urlString, filePath, requestListener);
		}
		token.execute();
	}

	private IRequestListener requestListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			doOnSuccess();
		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
		}

		@Override
		public void onRequestFailure(HttpException httpException)
		{
			if (httpException.getErrorCode() == HttpException.REASON_CODE_CANCELLATION)
			{
				doOnCancelled();
			}
			else
			{
				doOnFailure();
			}
		}
	};

	public void cancel()
	{
		doOnCancelled();
	}

	private void doOnCancelled()
	{
		File file = new File(fileName);
		file.delete();
		Utils.removeTempProfileImage(id);
	}

	private void doOnFailure()
	{
		Utils.removeTempProfileImage(id);
		if (showToast)
		{
			Toast.makeText(context, R.string.error_download, Toast.LENGTH_SHORT).show();
		}
		File file = new File(fileName);
		file.delete();
		HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_IMAGE_NOT_DOWNLOADED, id);
	}

	private void doOnSuccess()
	{
		/*
		 * Removing the smaller icon in cache.
		 */
		Utils.renameTempProfileImage(id);

		String idpp = id;

		if (!statusImage)
		{
			idpp = id + ProfileActivity.PROFILE_PIC_SUFFIX;
		}

		HikeMessengerApp.getLruCache().remove(idpp);

		if (statusImage)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, null);
		}
		else
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_IMAGE_DOWNLOADED, id);
		}

		if (this.name == null)
			this.name = this.msisdn; // show the msisdn if its an unsaved contact
		if (statusImage && !TextUtils.isEmpty(this.fileName) && !TextUtils.isEmpty(this.msisdn))
		{
			String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
			this.fileName = directory + "/" + Utils.getProfileImageFileName(msisdn);

			Bundle bundle = new Bundle();
			bundle.putString(HikeConstants.Extras.IMAGE_PATH, this.fileName);
			bundle.putString(HikeConstants.Extras.MSISDN, this.msisdn);
			bundle.putString(HikeConstants.Extras.NAME, this.name);
			HikeMessengerApp.getPubSub().publish(HikePubSub.PUSH_AVATAR_DOWNLOADED, bundle);
		}
	}

}
