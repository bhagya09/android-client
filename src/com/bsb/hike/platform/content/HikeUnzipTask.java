package com.bsb.hike.platform.content;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Observable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.os.AsyncTask;
import android.util.Log;

public class HikeUnzipTask extends Observable
{

	private static final String TAG = "UnZip";

	private String mFilePath, mDestinationPath;

	public HikeUnzipTask(String filePath, String destinationPath)
	{
		mFilePath = filePath;
		mDestinationPath = destinationPath;
	}

	public String getFilePath()
	{
		return mFilePath;
	}

	public String getDestinationPath()
	{
		return mDestinationPath;
	}

	public void unzip()
	{
		Log.d(TAG, "unzipping " + mFilePath + " to " + mDestinationPath);
		new UnZipTask().execute(mFilePath, mDestinationPath);
	}

	private class UnZipTask extends AsyncTask<String, Void, Boolean>
	{

		@SuppressWarnings("rawtypes")
		@Override
		protected Boolean doInBackground(String... params)
		{
			String filePath = params[0];
			String destinationPath = params[1];

			File archive = new File(filePath);
			try
			{
				ZipFile zipfile = new ZipFile(archive);
				for (Enumeration e = zipfile.entries(); e.hasMoreElements();)
				{
					ZipEntry entry = (ZipEntry) e.nextElement();
					unzipEntry(zipfile, entry, destinationPath);
				}
			}
			catch (Exception e)
			{
				Log.e(TAG, "Error while extracting file " + archive, e);
				return false;
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			System.out.println("UNZIP COMPLETE");
			setChanged();
			notifyObservers();
		}

		private void unzipEntry(ZipFile zipfile, ZipEntry entry, String outputDir) throws IOException
		{

			if (entry.isDirectory())
			{
				createDir(new File(outputDir, entry.getName()));
				return;
			}

			File outputFile = new File(outputDir, entry.getName());
			if (!outputFile.getParentFile().exists())
			{
				createDir(outputFile.getParentFile());
			}

			Log.v(TAG, "Extracting: " + entry);
			BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
			BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

			try
			{
				PlatformContentUtils.copyFile(inputStream, outputStream);
			}
			finally
			{
				outputStream.close();
				inputStream.close();
			}
		}

		private void createDir(File dir)
		{
			if (dir.exists())
			{
				return;
			}
			Log.v(TAG, "Creating dir " + dir.getName());
			if (!dir.mkdirs())
			{
				throw new RuntimeException("Can not create dir " + dir);
			}
		}
	}
}
