package com.bsb.hike.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.bsb.hike.R;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class DownloadAndInstallUpdateAsyncTask extends AsyncTask<Void, Integer, Boolean>
{
	private String filePath;

	private Notification notification;

	private NotificationManager notificationManager;

	private int notificationId = 119;

	private Context context;

	private String downloadUrl;

	public DownloadAndInstallUpdateAsyncTask(Context context, String downloadUrl)
	{
		this.context = context;
		this.downloadUrl = downloadUrl;
	}

	@Override
	protected void onPreExecute()
	{
		// Show progress notification
		Intent intent = new Intent(context, HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		notification = new Notification(R.drawable.ic_contact_logo, context.getString(R.string.downloading), System.currentTimeMillis());
		notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
		notification.contentIntent = pendingIntent;
		notification.contentView = new RemoteViews(context.getPackageName(), R.layout.download_notification);
		notification.contentView.setProgressBar(R.id.download_progress, 100, 0, false);
		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationId, notification);
	}

	@Override
	protected Boolean doInBackground(Void... arg0)
	{
		FileOutputStream fos = null;
		InputStream is = null;
		try
		{
			URL url = new URL(downloadUrl);
			URLConnection urlConnection = url.openConnection();
			AccountUtils.setNoTransform(urlConnection);

			int length = urlConnection.getContentLength();

			is = new BufferedInputStream(urlConnection.getInputStream());

			filePath = Environment.getExternalStorageDirectory() + "/hike/";
			File file = new File(filePath);
			file.mkdirs();

			File myApk = new File(file, "hike.apk");
			fos = new FileOutputStream(myApk);

			byte[] buffer = new byte[4096];
			int len = 0;
			int totProg = 0;
			int progress;

			while ((len = is.read(buffer)) != -1)
			{
				totProg += len;
				fos.write(buffer, 0, len);
				progress = (int) ((totProg * 100 / length));
				if (progress % 10 == 0)
				{
					publishProgress(progress);
				}
			}
			fos.flush();
			fos.getFD().sync();

			return true;
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			Utils.closeStreams(fos, is);
		}
		return false;
	}

	@Override
	protected void onProgressUpdate(Integer... values)
	{
		Logger.d(getClass().getSimpleName(), "Progress: " + values[0]);
		notification.contentView.setProgressBar(R.id.download_progress, 100, values[0], false);
		notificationManager.notify(notificationId, notification);
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		HikeNotification.getInstance().cancelNotification(notificationId);
		if (result)
		{
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(new File(filePath + "hike.apk")), "application/vnd.android.package-archive");
			context.startActivity(intent);
		}
		else
		{
			Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
		}
	}
}