package com.bsb.hike.platform.content;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;

import android.os.AsyncTask;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.platform.content.PlatformContent.EventCode;

/**
 * Download and store template
 * 
 * @author Atul M
 * 
 */
class PlatformTemplateDownloadTask extends AsyncTask<Void, Void, Void>
{
	private PlatformContentRequest mRequest;

	private HttpURLConnection connection;

	/**
	 * Instantiates a new platform template download task.
	 * 
	 * @param content
	 *            the content
	 * @param listener
	 *            the listener
	 */
	public PlatformTemplateDownloadTask(PlatformContentRequest argRequest)
	{
		// Get ID from content and call http
		mRequest = argRequest;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Void doInBackground(Void... params)
	{

		// Create temp folder
		File tempFolder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME);

		tempFolder.mkdirs();

		File zipFile = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME, mRequest.getContentData().getId() + ".zip");

		// Start downloading ZIP to temporary folder
		InputStream input = getZipStream();

		if (input == null)
		{
			return null;
		}

		OutputStream output = null;

		boolean isDownloaded = false;

		try
		{
			output = new FileOutputStream(zipFile);

			byte data[] = new byte[4096];

			int count;

			while ((count = input.read(data)) != -1)
			{
				output.write(data, 0, count);
			}

			isDownloaded = true;
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			PlatformRequestManager.reportFailure(mRequest, EventCode.STORAGE_FULL);
			PlatformRequestManager.remove(mRequest);
		}
		catch (IllegalStateException ise)
		{
			ise.printStackTrace();
			PlatformRequestManager.reportFailure(mRequest, EventCode.UNKNOWN);
			PlatformRequestManager.remove(mRequest);
		}
		finally
		{
			try
			{
				if (output != null)
				{
					output.flush();
					output.close();
				}

				if (input != null)
				{
					input.close();
				}

				if (connection != null)
				{
					connection.disconnect();
				}
			}
			catch (IOException ignored)
			{
				// Do nothing
			}
		}

		if (isDownloaded)
		{
			// unzip
			unzipWebFile(zipFile.getAbsolutePath(), PlatformContentConstants.PLATFORM_CONTENT_DIR, new Observer()
			{
				@Override
				public void update(Observable observable, Object data)
				{
					// delete temp folder
					File tempFolder = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR + PlatformContentConstants.TEMP_DIR_NAME);
					PlatformContentUtils.deleteDirectory(tempFolder);
					PlatformRequestManager.setReadyState(mRequest);
				}
			});
		}
		else
		{
			// Could not download template files due to come reason. TODO We can implement retry here. Leaving for v1.
			PlatformRequestManager.remove(mRequest);
		}

		return null;
	}

	private InputStream getZipStream()
	{
		InputStream is = getStreamFromAssets();

		if (is == null)
		{
			is = getStreamFromWeb();
		}

		return is;
	}

	private InputStream getStreamFromWeb()
	{
		try
		{
			URL url = new URL(mRequest.getContentData().getLayout_url());
			connection = (HttpURLConnection) url.openConnection();
			connection.connect();

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
			{
				throw new IOException("Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
			}

			return connection.getInputStream();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			PlatformRequestManager.reportFailure(mRequest, EventCode.LOW_CONNECTIVITY);
			PlatformRequestManager.remove(mRequest);
		}

		return null;
	}

	private InputStream getStreamFromAssets()
	{
		// Check if the zip is present in hike app package
		try
		{
			InputStream assetFileInputStream = HikeMessengerApp.getInstance().getAssets().open("content/" + mRequest.getContentData().getId() + ".zip");
			if (assetFileInputStream.available() > 0)
			{
				return assetFileInputStream;
			}
			else
			{
				assetFileInputStream.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	private void unzipWebFile(String zipFilePath, String unzipLocation, Observer observer)
	{
		HikeUnzipTask unzipper = new HikeUnzipTask(zipFilePath, unzipLocation);
		unzipper.addObserver(observer);
		unzipper.unzip();
	}
}
