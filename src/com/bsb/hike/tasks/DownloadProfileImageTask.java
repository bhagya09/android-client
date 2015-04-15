package com.bsb.hike.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;

public class DownloadProfileImageTask extends AsyncTask<Void, Void, Boolean>
{

	private Context context;

	private String id;

	private String urlString;

	private String fileName;

	private String filePath;

	private boolean isSslON;

	private String msisdn;

	private String name;

	private boolean statusImage;

	private boolean showToast;

	public DownloadProfileImageTask(Context context, String id, String fileName, boolean hasCustomIcon, boolean statusImage, String msisdn, String name, boolean showToast)
	{
		this(context, id, fileName, hasCustomIcon, statusImage, null, msisdn, name, showToast);
	}

	public DownloadProfileImageTask(Context context, String id, String fileName, boolean hasCustomIcon, boolean statusImage, String msisdn, String name, boolean showToast,
			String url)
	{
		this(context, id, fileName, hasCustomIcon, statusImage, url, msisdn, name, showToast);
	}

	private DownloadProfileImageTask(Context context, String id, String fileName, boolean hasCustomIcon, boolean statusImage, String url, String msisdn, String name,
			boolean showToast)
	{
		this.context = context;
		this.id = id;
		this.msisdn = msisdn;
		this.statusImage = statusImage;
		this.name = name;

		if (TextUtils.isEmpty(url))
		{
			if (statusImage)
			{
				this.urlString = AccountUtils.base + "/user/status/" + id + "?only_image=true";
			}
			else
			{
				boolean isGroupConversation = OneToNConversationUtils.isGroupConversation(id);

				if (hasCustomIcon)
				{
					this.urlString = AccountUtils.base + (isGroupConversation ? "/group/" + id + "/avatar" : "/account/avatar/" + id) + "?fullsize=1";
				}
				else
				{
					this.urlString = (AccountUtils.ssl ? AccountUtils.HTTPS_STRING : AccountUtils.HTTP_STRING) + AccountUtils.host + ":" + AccountUtils.port + "/static/avatars/"
							+ fileName;
				}
				this.isSslON = AccountUtils.ssl;
			}
		}
		else
		{
			this.urlString = url;
			this.isSslON = url.startsWith(AccountUtils.HTTPS_STRING);
		}

		this.filePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		this.fileName = filePath + "/" + Utils.getTempProfileImageFileName(id);

		this.showToast = showToast;
	}

	@Override
	protected Boolean doInBackground(Void... params)
	{
		FileOutputStream fos = null;
		InputStream is = null;
		try
		{
			File dir = new File(filePath);
			if (!dir.exists())
			{
				if (!dir.mkdirs())
				{
					return Boolean.FALSE;
				}
			}

			Logger.d(getClass().getSimpleName(), "Downloading profile image: " + urlString);
			URL url = new URL(urlString);

			URLConnection connection = url.openConnection();
			AccountUtils.addUserAgent(connection);
			connection.addRequestProperty("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid);

			if (isSslON)
			{
				((HttpsURLConnection) connection).setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
			}

			is = new BufferedInputStream(connection.getInputStream());

			fos = new FileOutputStream(fileName);

			byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
			int len = 0;

			while ((len = is.read(buffer)) != -1)
			{
				fos.write(buffer, 0, len);
			}

		}
		catch (MalformedURLException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid URL", e);
			return Boolean.FALSE;
		}
		catch (IOException e)
		{
			Logger.e(getClass().getSimpleName(), "Error while downloding file", e);
			return Boolean.FALSE;
		}
		finally
		{
			try
			{
				if (fos != null)
				{
					fos.flush();
					fos.getFD().sync();
					fos.close();
				}
				if (is != null)
				{
					is.close();
				}
			}
			catch (IOException e)
			{
				Logger.e(getClass().getSimpleName(), "Error while closing file", e);
				return Boolean.FALSE;
			}
		}
		return Boolean.TRUE;
	}

	@Override
	protected void onCancelled()
	{
		File file = new File(fileName);
		file.delete();
		Utils.removeTempProfileImage(id);
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		if (result == false)
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
		else
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

}
