package com.bsb.hike.tasks;

import java.io.File;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class DownloadImageTask extends AsyncTask<Void, Void, Boolean>
{

	public static interface ImageDownloadResult
	{
		public void downloadFinished(boolean result);
	}

	private File destFile;

	private Uri imageUri;

	private Context context;

	private ImageDownloadResult imageDownloadResult;

	public DownloadImageTask(Context context, File destFile, Uri picasaUri, ImageDownloadResult imageDownloadResult)
	{
		this.destFile = destFile;
		this.imageUri = picasaUri;
		this.context = context;
		this.imageDownloadResult = imageDownloadResult;
	}

	@Override
	protected Boolean doInBackground(Void... params)
	{
		try
		{
			Utils.downloadAndSaveFile(context.getContentResolver(), destFile, imageUri);
			return Boolean.TRUE;
		}
		catch (Exception e)
		{
			Logger.e(getClass().getSimpleName(), "Error while fetching image", e);
			return Boolean.FALSE;
		}
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		imageDownloadResult.downloadFinished(result);
	}
}