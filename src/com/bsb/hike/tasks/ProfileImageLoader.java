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
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ProfileImageLoader extends AsyncTaskLoader<Boolean>
{

	private String urlString;

	private boolean isSslON;

	private String filePath;

	private String fileName;

	private String key;

	private boolean isStatusImage;

	public ProfileImageLoader(Context context, String id, String fileName, boolean hasCustomIcon, boolean statusImage)
	{
		this(context, id, fileName, hasCustomIcon, statusImage, null);
	}

	public ProfileImageLoader(Context context, String id, String fileName, boolean hasCustomIcon, boolean statusImage, String url)
	{
		super(context);

		this.isStatusImage = statusImage;
		this.key = id;

		if (TextUtils.isEmpty(url))
		{
			if (statusImage)
			{
				this.urlString = AccountUtils.base + "/user/status/" + id + "?only_image=true";
			}
			else
			{
				boolean isGroupConversation = Utils.isGroupConversation(id);

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
		this.fileName = filePath + "/" + Utils.getTempProfileImageFileName(key);
	}

	@Override
	protected void onStartLoading()
	{
		forceLoad();
	}

	@Override
	public Boolean loadInBackground()
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
			Utils.removeTempProfileImage(key);
			return Boolean.FALSE;
		}
		catch (IOException e)
		{
			Logger.e(getClass().getSimpleName(), "Error while downloding file", e);
			Utils.removeTempProfileImage(key);
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
				Utils.removeTempProfileImage(key);
				return Boolean.FALSE;
			}
		}

		Utils.renameTempProfileImage(key);

		String keypp = key;

		if (!isStatusImage)
			keypp = key + ProfileActivity.PROFILE_PIC_SUFFIX;

		HikeMessengerApp.getLruCache().remove(keypp);
		HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_IMAGE_DOWNLOADED, key);
		return Boolean.TRUE;
	}

	@Override
	public void onCanceled(Boolean data)
	{
		super.onCanceled(data);
		Utils.removeTempProfileImage(key);
	}

	@Override
	protected void onStopLoading()
	{
		cancelLoad();
		Utils.removeTempProfileImage(key);
	}

}
