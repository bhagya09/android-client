package com.bsb.hike.platform.content;

import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.utils.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Observable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Unzips ZIP file.
 * 
 * @author Atul M
 * 
 */
public class HikeUnzipFile extends Observable
{

	private static final String TAG = "HikeUnzipFile";

	private String mFilePath, mDestinationPath,mAppName;

	public HikeUnzipFile(String filePath, String destinationPath)
	{
		mFilePath = filePath;
		mDestinationPath = destinationPath;
	}

    public HikeUnzipFile(String filePath, String destinationPath,String appName)
    {
        mFilePath = filePath;
        mDestinationPath = destinationPath;
        mAppName = appName;
    }

	public String getFilePath()
	{
		return mFilePath;
	}

	/*
	 * Method called to unzip from PlatformZipDownloader Class
	 */
	public void unzip()
	{
		Logger.d(TAG, "unzipping " + mFilePath + " to " + mDestinationPath);
		notifyOnUnzipProcessCompletion(unZipFromSourceToDestination(mFilePath, mDestinationPath));
	}

	/*
	 * Method to unzip zip file from given source to its destination
	 */
	private boolean unZipFromSourceToDestination(String... params)
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
				unzipEachEntry(zipfile, entry, destinationPath);
			}
		}
		catch (Exception e)
		{
            // Adding analytics call here on any exception and failure in unzip code base
            PlatformUtils.microAppWriteFailedAnalytics(mAppName,e.toString());
            return false;
		}

		return true;
	}

	/*
	 * This method called from the above method does the actual unzip of each file and directorie present within the zip file and copy them to the destination
	 */
	private void unzipEachEntry(ZipFile zipfile, ZipEntry entry, String outputDir) throws Exception
	{
		if (entry.isDirectory())
		{
				new File(outputDir, entry.getName()).mkdirs();
				return;
		}

		File outputFile = new File(outputDir, entry.getName());
		if (!outputFile.getParentFile().exists())
		{
			outputFile.getParentFile().mkdirs();
		}

		BufferedInputStream inputStream = null;
		BufferedOutputStream outputStream = null;

        // Any exception got while deflating micro app would be considered as micro app unzip failure
		try
		{
			Logger.v(TAG, "Extracting: " + entry);
			inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
			outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
			PlatformContentUtils.copyFile(inputStream, outputStream);
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

	/*
	 * Method to notify the observers on completion of the unzip process
	 */
	private void notifyOnUnzipProcessCompletion(Boolean result)
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

}
