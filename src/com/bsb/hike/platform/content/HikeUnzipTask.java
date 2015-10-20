package com.bsb.hike.platform.content;

import android.os.AsyncTask;

import com.bsb.hike.utils.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Observable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Unzips ZIP file. Contains both methods unzipAsync() and unzip() which runs in non-blocking and blocking mode respectively.
 * 
 * @author Atul M
 * 
 */
public class HikeUnzipTask extends Observable
{

	private static final String TAG = "HikeUnzipTask";

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

	public void unzipAsync()
	{
		Logger.d(TAG, "unzipping " + mFilePath + " to " + mDestinationPath);
		new UnZipTask().execute(mFilePath, mDestinationPath);
	}

	public void unzip()
	{
		Logger.d(TAG, "unzipping " + mFilePath + " to " + mDestinationPath);
		UnZipTask unzipTask = new UnZipTask();
		unzipTask.onPostExecute(unzipTask.doInBackground(mFilePath, mDestinationPath) );
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

			if (!archive.exists())
			{
				return false;
			}
			
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
				Logger.e(TAG, "Error while extracting file " + archive, e);
				return false;
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			setChanged();
			if (result)
			{
				Logger.d(TAG, "Unzip Complete");
				notifyObservers(true);
			}
			else
			{
				Logger.wtf(TAG, "Unzip failed");
				notifyObservers(false);
			}
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

			BufferedInputStream inputStream = null;
			BufferedOutputStream outputStream = null;

			try
			{
				Logger.v(TAG, "Extracting: " + entry);
				inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
				outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

				PlatformContentUtils.copyFile(inputStream, outputStream);
			}
			catch (FileNotFoundException fnfe)
			{
				fnfe.printStackTrace();
			}
			finally
			{
				if (outputStream != null)
				{
					outputStream.close();
				}
				if (inputStream != null)
				{
					inputStream.close();
				}
			}
		}

		private void createDir(File dir)
		{
			if (dir.exists())
			{
				return;
			}
			Logger.v(TAG, "Creating dir " + dir.getName());
			if (!dir.mkdirs())
			{
				throw new RuntimeException("Can not create dir " + dir);
			}
		}

	}
}
