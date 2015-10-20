package com.bsb.hike.timeline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Environment;

public class TestBmp
{

	public static String compressImage(final String filePath, final String destFilePath, final float maxWidth, final float maxHeight)
	{

		Bitmap scaledBitmap = null;

		BitmapFactory.Options options = new BitmapFactory.Options();

		options.inJustDecodeBounds = true;

		Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

		int actualHeight = options.outHeight;
		int actualWidth = options.outWidth;

		float imgRatio = actualWidth / actualHeight;
		float maxRatio = maxWidth / maxHeight;

		// width and height values are set maintaining the aspect ratio of the image
		if (actualHeight > maxHeight || actualWidth > maxWidth)
		{
			if (imgRatio < maxRatio)
			{
				imgRatio = maxHeight / actualHeight;
				actualWidth = (int) (imgRatio * actualWidth);
				actualHeight = (int) maxHeight;
			}
			else if (imgRatio > maxRatio)
			{
				imgRatio = maxWidth / actualWidth;
				actualHeight = (int) (imgRatio * actualHeight);
				actualWidth = (int) maxWidth;
			}
			else
			{
				actualHeight = (int) maxHeight;
				actualWidth = (int) maxWidth;

			}
		}

		// setting inSampleSize value allows to load a scaled down version of the original image
		options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

		// inJustDecodeBounds set to false to load the actual bitmap
		options.inJustDecodeBounds = false;
		
		options.inScaled = false;
		
		options.inDither = true;
		
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		
		options.inPreferQualityOverSpeed = true;

//		try
//		{
//			// load the bitmap from its path
			bmp = BitmapFactory.decodeFile(filePath, options);
//			BitmapUtils.saveBitmapToFile(new File(getFilename()), bmp, CompressFormat.PNG,100);
//			BitmapUtils.saveBitmapToFile(new File(getFilename()), bmp, CompressFormat.JPEG,80);
//		}
//		catch (OutOfMemoryError | IOException exception)
//		{
//			exception.printStackTrace();
//
//		}
		try
		{
			scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.RGB_565);
		}
		catch (OutOfMemoryError exception)
		{
			exception.printStackTrace();
		}

		float ratioX = actualWidth / (float) options.outWidth;
		float ratioY = actualHeight / (float) options.outHeight;
		float middleX = actualWidth / 2.0f;
		float middleY = actualHeight / 2.0f;

		Matrix scaleMatrix = new Matrix();
		scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

		Canvas canvas = new Canvas(scaledBitmap);
		canvas.setMatrix(scaleMatrix);
		Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
		paint.setDither(true);
		canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, paint);

		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(destFilePath);

			// write the compressed bitmap at the destination specified by filename.
			scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);

		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		return destFilePath;
	}

	public static String getFilename()
	{
		File file = new File(Environment.getExternalStorageDirectory().getPath(), "MyFolder/Images");
		if (!file.exists())
		{
			file.mkdirs();
		}
		String uriSting = (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
		return uriSting;
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth)
		{
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		final float totalPixels = width * height;
		final float totalReqPixelsCap = reqWidth * reqHeight * 2;
		while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap)
		{
			inSampleSize++;
		}

		return inSampleSize;
	}
}
