package com.bsb.hike.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.view.View;

public class ShareBitmapTask extends AsyncTask<Void, Void, Void>
{
	View share;

	Context mContext;

	File shareFile;

	public ShareBitmapTask(View view, Context context, File shareFile)
	{
		share = view;
		mContext = context;
		this.shareFile = shareFile;
	}

	private static void closeFos(FileOutputStream fos)
	{
		if (fos != null)
		{
			try
			{
				fos.close();
			}
			catch (IOException e)
			{
				// Do nothing
				e.printStackTrace();
			}
		}

	}

	@Override
	protected Void doInBackground(Void... params)
	{
		FileOutputStream fos = null;

		try
		{

			fos = new FileOutputStream(shareFile);
			Bitmap shB = Utils.undrawnViewToBitmap(share);
			shB.compress(CompressFormat.JPEG, 100, fos);
			fos.flush();

		}
		catch (Exception e)
		{
			e.printStackTrace();

		}
		finally
		{
			closeFos(fos);
		}
		return null;

	}
}
