package com.bsb.hike.platform.content;

import android.os.AsyncTask;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.platform.PlatformContentRequest;

import java.io.*;

/**
 * Created by shobhit on 04/03/15.
 */
public class AssetsZipMoveTask extends AsyncTask<Void, Void, Boolean>
{
	File zipFilePath;

	PlatformContentRequest mRequest;

	AssetZipMovedCallbackCallback movedCallback;

	boolean isTemplatingEnabled;

	public static interface AssetZipMovedCallbackCallback
	{
		public void assetZipMoved(boolean hasMoved);
	}

	public AssetsZipMoveTask(File tempFilePath, PlatformContentRequest request, AssetZipMovedCallbackCallback callback, boolean isTemplatingEnabled)
	{
		zipFilePath = tempFilePath;
		mRequest = request;
		movedCallback = callback;
		this.isTemplatingEnabled = isTemplatingEnabled;
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		if (result.booleanValue())
		{
			movedCallback.assetZipMoved(true);
		}
		else
		{
			movedCallback.assetZipMoved(false);
		}
	}

	@Override
	protected Boolean doInBackground(Void... unused)
	{
		try
		{
			InputStream assetFileInputStream = HikeMessengerApp.getInstance().getAssets().open("content/" + mRequest.getContentData().getId() + ".zip");
			if (assetFileInputStream.available() > 0)
			{
				OutputStream output = null;
				try
				{
					output = new FileOutputStream(zipFilePath);

					byte data[] = new byte[4096];

					int count;

					while ((count = assetFileInputStream.read(data)) != -1)
					{
						output.write(data, 0, count);
					}

					return true;
				}
				catch (IOException ioe)
				{
					ioe.printStackTrace();
					PlatformRequestManager.failure(mRequest, PlatformContent.EventCode.STORAGE_FULL, isTemplatingEnabled);
				}
				catch (IllegalStateException ise)
				{
					ise.printStackTrace();
					PlatformRequestManager.failure(mRequest, PlatformContent.EventCode.UNKNOWN, isTemplatingEnabled);
				}
				finally
				{
					if (output != null)
					{
						output.flush();
						output.close();
					}

					if (assetFileInputStream != null)
					{
						assetFileInputStream.close();
					}
				}
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

		return false;
	}
}
