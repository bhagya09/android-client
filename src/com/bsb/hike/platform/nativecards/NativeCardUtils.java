package com.bsb.hike.platform.nativecards;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;

import com.bsb.hike.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by pushkargupta on 05/05/16.
 * Calss to have all utility methods related to cards , cards to be moved
 */
public class NativeCardUtils
{
	public static Uri getFileForView(View view, Context mContext)
	{
		if(view == null){
			return null;
		}
		FileOutputStream fos = null;
		File cardShareImageFile = null;
		cardShareImageFile = new File(mContext.getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
		try
		{
			fos = new FileOutputStream(cardShareImageFile);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		Bitmap b = Utils.viewToBitmap(view);
		b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
		try
		{
			fos.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
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

		return Uri.fromFile(cardShareImageFile);

	}
}
