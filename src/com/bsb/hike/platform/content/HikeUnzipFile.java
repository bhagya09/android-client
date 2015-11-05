package com.bsb.hike.platform.content;

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
 * Unzips ZIP file.
 * 
 * @author Atul M
 * 
 */
public class HikeUnzipFile extends Observable
{

	private static final String TAG = "HikeUnzipFile";

	private String mFilePath, mDestinationPath,mAppName;

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
            ZipFile zipfile = new ZipFile(filePath);
			for (Enumeration e = zipfile.entries(); e.hasMoreElements();)
			{
				ZipEntry entry = (ZipEntry) e.nextElement();
				unzipEachEntry(zipfile, entry, destinationPath);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Error while extracting file " + archive, e);
			return false;
		}



		return true;
	}


	/*
	 * This method called from the above method does the actual unzip of each file and directorie present within the zip file and copy them to the destination
	 */
	private void unzipEachEntry(ZipFile zipfile, ZipEntry entry, String outputDir) throws IOException
	{
        // Code logic to get the exact unzip directory name that needs to be generated
        String name =entry.getName();
        name = name.substring(name.indexOf("/")+1,name.length());

        if (entry.isDirectory())
		{
				new File(outputDir, name).mkdirs();
				return;
		}

		File outputFile = new File(outputDir,name);

		BufferedInputStream inputStream = null;
		BufferedOutputStream outputStream = null;

		try
		{
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
