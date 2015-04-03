package com.bsb.hike.BitmapModule;

import java.io.InputStream;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.View.MeasureSpec;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.smartcache.HikeLruCache;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class HikeBitmapFactory
{
	private static final String TAG = "HikeBitmapFactory";

	public static Bitmap getCircularBitmap(Bitmap bitmap)
	{
		if (bitmap == null)
		{
			return null;
		}

		Bitmap output = createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);

		if (output == null)
		{
			return null;
		}

		Canvas canvas = new Canvas(output);
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}


	public static Bitmap getBitMapFromTV(View textView)
	{
		// capture bitmapt of genreated textviewl
		int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		textView.measure(spec, spec);
		textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
		Bitmap b = createBitmap(textView.getWidth(), textView.getHeight(), Bitmap.Config.ARGB_8888);
		if (b == null)
		{
			return null;
		}
		Canvas canvas = new Canvas(b);
		canvas.translate(-textView.getScrollX(), -textView.getScrollY());
		textView.draw(canvas);
		textView.setDrawingCacheEnabled(true);
		Bitmap cacheBmp = textView.getDrawingCache();
		Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
		if (cacheBmp != null)
		{
			cacheBmp.recycle();
		}
		textView.destroyDrawingCache(); // destory drawable
		return viewBmp;

	}

	public static BitmapDrawable stringToDrawable(String encodedString)
	{
		if (TextUtils.isEmpty(encodedString))
		{
			return null;
		}
		byte[] thumbnailBytes = Base64.decode(encodedString, Base64.DEFAULT);
		return getBitmapDrawable(decodeBitmapFromByteArray(thumbnailBytes, Config.RGB_565));
	}

	public static Bitmap makeSquareThumbnail(Bitmap thumbnail)
	{
		int dimensionLimit = thumbnail.getWidth() < thumbnail.getHeight() ? thumbnail.getWidth() : thumbnail.getHeight();

		int startX = thumbnail.getWidth() > dimensionLimit ? (int) ((thumbnail.getWidth() - dimensionLimit) / 2) : 0;
		int startY = thumbnail.getHeight() > dimensionLimit ? (int) ((thumbnail.getHeight() - dimensionLimit) / 2) : 0;

		Logger.d("Utils", "StartX: " + startX + " StartY: " + startY + " WIDTH: " + thumbnail.getWidth() + " Height: " + thumbnail.getHeight());
		Logger.d("Utils", "dimensionLimit : " + dimensionLimit);

		Bitmap squareThumbnail = createBitmap(thumbnail, startX, startY, startX + dimensionLimit, startY + dimensionLimit);

		if (squareThumbnail != thumbnail)
		{
			thumbnail.recycle();
		}
		thumbnail = null;
		return squareThumbnail;
	}

	public static Bitmap stringToBitmap(String thumbnailString)
	{
		byte[] encodeByte = Base64.decode(thumbnailString, Base64.DEFAULT);
		return decodeByteArray(encodeByte, 0, encodeByte.length);
	}

	public static Bitmap drawableToBitmap(Drawable drawable, Bitmap.Config config)
	{
		if (drawable == null)
		{
			return null;
		}

		if (drawable instanceof BitmapDrawable)
		{
			return ((BitmapDrawable) drawable).getBitmap();
		}
		/*
		 * http://developer.android.com/reference/android/graphics/Bitmap.Config. html
		 */
		Bitmap bitmap = createBitmap((int) (48 * Utils.scaledDensityMultiplier), (int) (48 * Utils.scaledDensityMultiplier), config);

		if (bitmap == null)
		{
			return null;
		}
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}
	
	public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap;
        int width = Math.max(drawable.getIntrinsicWidth(), 2);
        int height = Math.max(drawable.getIntrinsicHeight(), 2);
        try {
            bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        } catch (Exception e) {
            e.printStackTrace();
            bitmap = null;
        }

        return bitmap;
    }

	public static Bitmap rotateBitmap(Bitmap b, int degrees)
	{
		if (degrees != 0 && b != null)
		{
			Matrix m = new Matrix();
			m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
			try
			{
				Bitmap b2 = createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
				if (b2 != null)
				{
					if (b != b2)
					{
						b.recycle();
						b = b2;
					}
				}
			}
			catch (OutOfMemoryError e)
			{
				Logger.e("Utils", "Out of memory", e);
			}
		}
		return b;
	}

	public static Bitmap returnScaledBitmap(Bitmap src, Context context)
	{
		Resources res = context.getResources();
		if (Utils.isHoneycombOrHigher())
		{
			int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
			int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
			src = createScaledBitmap(src, width, height, Bitmap.Config.RGB_565, false, true, true);
			return src;
		}
		else
			return src;
	}

	public static BitmapDrawable getBitmapDrawable(Resources mResources, final Bitmap bitmap)
	{
		if (bitmap == null)
			return null;

		if (Utils.isHoneycombOrHigher())
		{
			// Running on Honeycomb or newer, so wrap in a standard BitmapDrawable
			return new BitmapDrawable(mResources, bitmap);
		}
		else
		{
			// Running on Gingerbread or older, so wrap in a RecyclingBitmapDrawable
			// which will recycle automagically
			return new RecyclingBitmapDrawable(mResources, bitmap);
		}
	}

	public static BitmapDrawable getBitmapDrawable(final Bitmap bitmap)
	{
		if (bitmap == null)
			return null;

		if (Utils.isHoneycombOrHigher())
		{
			// Running on Honeycomb or newer, so wrap in a standard BitmapDrawable
			return new BitmapDrawable(bitmap);
		}
		else
		{
			// Running on Gingerbread or older, so wrap in a RecyclingBitmapDrawable
			// which will recycle automagically
			return new RecyclingBitmapDrawable(bitmap);
		}
	}

	/**
	 * Decode and sample down a bitmap from resources to the requested width and height.
	 * 
	 * @param res
	 *            The resources object containing the image data
	 * @param resId
	 *            The resource id of the image data
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @param cache
	 *            The ImageCache used to find candidate bitmaps for use with inBitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight)
	{
		return decodeSampledBitmapFromResource(res, resId, reqWidth, reqHeight, Bitmap.Config.ARGB_8888);
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight, Bitmap.Config con)
	{
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		decodeResource(res, resId, options);

		options.inPreferredConfig = con;

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// If we're running on Honeycomb or newer, try to use inBitmap
		// if (Utils.hasHoneycomb())
		// {
		// addInBitmapOptions(options, cache);
		// }

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap result = null;
		try
		{
			result = decodeResource(res, resId, options);
		}
		catch (IllegalArgumentException e)
		{
			result = decodeResource(res, resId);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decoding Bitmap from resources: ", e);
		}
		return result;
	}

	/**
	 * Decode and sample down a bitmap from a file to the requested width and height.
	 * 
	 * @param filename
	 *            The full path of the file to decode
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @param cache
	 *            The ImageCache used to find candidate bitmaps for use with inBitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight)
	{
		return decodeSampledBitmapFromFile(filename, reqWidth, reqHeight, Bitmap.Config.ARGB_8888);
	}

	public static Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight, Bitmap.Config con)
	{
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		decodeFile(filename, options);

		options.inPreferredConfig = con;

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// If we're running on Honeycomb or newer, try to use inBitmap
		// if (Utils.hasHoneycomb())
		// {
		// addInBitmapOptions(options, cache);
		// }

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap result = null;
		try
		{
			result = decodeFile(filename, options);
		}
		catch (IllegalArgumentException e)
		{
			result = decodeFile(filename, options);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decoding Bitmap from file: ", e);
		}
		return result;
	}

	/**
	 * Decode and sample down a bitmap from a file to the requested width and height.
	 * 
	 * @param filename
	 *            The full path of the file to decode
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @param cache
	 *            The ImageCache used to find candidate bitmaps for use with inBitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromByteArray(byte[] bytearray, int reqWidth, int reqHeight)
	{
		return decodeSampledBitmapFromByteArray(bytearray, reqWidth, reqHeight, Bitmap.Config.ARGB_8888);
	}

	public static Bitmap decodeSampledBitmapFromByteArray(byte[] bytearray, int reqWidth, int reqHeight, Bitmap.Config con)
	{
		if (bytearray == null)
			return null;
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		decodeByteArray(bytearray, 0, bytearray.length, options);

		options.inPreferredConfig = con;

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// If we're running on Honeycomb or newer, try to use inBitmap
		// if (Utils.hasHoneycomb())
		// {
		// addInBitmapOptions(options, cache);
		// }

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap result = null;
		try
		{
			result = decodeByteArray(bytearray, 0, bytearray.length, options);
		}
		catch (IllegalArgumentException e)
		{
			result = decodeByteArray(bytearray, 0, bytearray.length);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decoding Bitmap from ByteArray: ", e);
		}
		return result;
	}

	/**
	 * This method decodes a bitmap from byte array with particular configuration config passed as a parameter. Bitmap will not be sampled , only configuration will be config. To
	 * sample down bitmap use decodeSampledBitmapFromByteArray
	 * 
	 * @param bytearray
	 * @param con
	 * @return
	 */
	public static Bitmap decodeBitmapFromByteArray(byte[] bytearray, Config config)
	{
		if (bytearray == null)
			return null;

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = config;

		Bitmap result = null;
		try
		{
			result = decodeByteArray(bytearray, 0, bytearray.length, options);
		}
		catch (IllegalArgumentException e)
		{
			result = decodeByteArray(bytearray, 0, bytearray.length);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decoding Bitmap from ByteArray: ", e);
		}
		return result;
	}

	/**
	 * This method uses the configuration given by config to decode a bitmap from file.
	 * @param filename
	 * @param con
	 * @return
	 */
	public static Bitmap decodeBitmapFromFile(String filename, Bitmap.Config config)
	{
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = config;

		Bitmap result = null;
		try
		{
			result = decodeFile(filename, options);
		}
		catch (IllegalArgumentException e)
		{
			result = decodeFile(filename);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decoding Bitmap from file: ", e);
		}
		return result;
	}

	/**
	 * This method uses the configuration given by config to decode a bitmap from resource.
	 * 
	 * @param filename
	 * @param con
	 * @return
	 */
	public static Bitmap decodeBitmapFromResource(Resources res, int resId, Bitmap.Config config)
	{
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = config;

		Bitmap result = null;
		try
		{
			result = decodeResource(res, resId, options);
		}
		catch (IllegalArgumentException e)
		{
			result = decodeResource(res, resId);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decoding Bitmap from file: ", e);
		}
		return result;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static void addInBitmapOptions(BitmapFactory.Options options)
	{
		// inBitmap only works with mutable bitmaps so force the decoder to
		// return mutable bitmaps.
		options.inMutable = true;
		HikeLruCache cache = HikeMessengerApp.getLruCache();
		if (cache != null)
		{
			// Try and find a bitmap to use for inBitmap
			Bitmap inBitmap = cache.getBitmapFromReusableSet(options);

			if (inBitmap != null)
			{
				Logger.d(TAG, "Found a bitmap in reusable set.");
				options.inBitmap = inBitmap;
			}
		}
	}

	/**
	 * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding bitmaps using the decode* methods from {@link BitmapFactory}. This implementation
	 * calculates the closest inSampleSize that is a power of 2 and will result in the final decoded bitmap having a width and height equal to or larger than the requested width
	 * and height.
	 * 
	 * @param options
	 *            An options object with out* params already populated (run through a decode* method with inJustDecodeBounds==true
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @return The value to be used for inSampleSize
	 */
	private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth)
		{

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth)
			{
				inSampleSize *= 2;
			}

		}
		return inSampleSize;
	}

	/**
	 * Decode and sample down a bitmap from resources to the requested inSampleSize.
	 * 
	 * @param res
	 *            The resources object containing the image data
	 * @param resId
	 *            The resource id of the image data
	 * @param inSampleSize
	 *            The value to be used for inSampleSize
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int inSampleSize)
	{

		final BitmapFactory.Options options = new BitmapFactory.Options();

		options.inSampleSize = inSampleSize;

		options.inJustDecodeBounds = false;
		Bitmap result = null;
		try
		{
			result = decodeResource(res, resId, options);
		}
		catch (IllegalArgumentException e)
		{
			result = decodeResource(res, resId);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decoding Bitmap from resources: ", e);
		}
		return result;
	}

	public static Bitmap createBitmap(int width, int height, Config con)
	{
		Bitmap b = null;
		try
		{
			b = Bitmap.createBitmap(width, height, con);
			Logger.wtf(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = Bitmap.createBitmap(width, height, con);
				Logger.wtf(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));
			}
			catch (OutOfMemoryError ex)
			{
				Logger.wtf(TAG, "Out of Memory even after System.gc");
			}
			catch (Exception exc)
			{
				Logger.e(TAG, " Exception in createBitmap : ", exc);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in createBitmap : ", e);
		}
		return b;
	}

	private static Bitmap createBitmap(Bitmap thumbnail, int startX, int startY, int i, int j)
	{
		Bitmap b = null;
		try
		{
			b = Bitmap.createBitmap(thumbnail, startX, startY, i, j);
			Logger.wtf(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));

		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = Bitmap.createBitmap(thumbnail, startX, startY, i, j);
				Logger.wtf(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));
			}
			catch (OutOfMemoryError ex)
			{
				Logger.wtf(TAG, "Out of Memory even after System.gc");
			}
			catch (Exception exc)
			{
				Logger.e(TAG, "Exception in createBitmap : ", exc);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in createBitmap : ", e);
		}
		return b;
	}

	public static Bitmap createBitmap(Bitmap bm, int i, int j, int width, int height, Matrix m, boolean c)
	{
		Bitmap b = null;
		try
		{
			b = Bitmap.createBitmap(bm, i, j, width, height, m, c);
			Logger.wtf(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = Bitmap.createBitmap(bm, i, j, width, height, m, c);
				Logger.wtf(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));
			}
			catch (OutOfMemoryError ex)
			{
				Logger.wtf(TAG, "Out of Memory even after System.gc");
			}
			catch (Exception exc)
			{
				Logger.e(TAG, "Exception in createBitmap : ", exc);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in createBitmap : ", e);
		}
		return b;
	}

	public static Bitmap decodeFile(String path)
	{
		Bitmap b = null;
		try
		{
			b = BitmapFactory.decodeFile(path);
			Logger.wtf(TAG, "Bitmap size in decodeFile : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeFile(path);
				Logger.wtf(TAG, "Bitmap size in decodeFile : " + BitmapUtils.getBitmapSize(b));
			}
			catch (OutOfMemoryError ex)
			{
				Logger.wtf(TAG, "Out of Memory even after System.gc");
			}
			catch (Exception exc)
			{
				Logger.e(TAG, "Exception in decodeFile : ", exc);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decodeFile : ", e);
		}
		return b;
	}

	public static Bitmap decodeFile(String path, BitmapFactory.Options opt)
	{
		Bitmap b = null;
		try
		{
			b = BitmapFactory.decodeFile(path, opt);
			Logger.wtf(TAG, "Bitmap size in decodeFile : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeFile(path, opt);
				Logger.wtf(TAG, "Bitmap size in decodeFile : " + BitmapUtils.getBitmapSize(b));
			}
			catch (OutOfMemoryError ex)
			{
				Logger.wtf(TAG, "Out of Memory even after System.gc");
			}
			catch (Exception exc)
			{
				Logger.e(TAG, "Exception in decodeFile : ", exc);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decodeFile : ", e);
		}
		return b;
	}

	public static Bitmap decodeStream(InputStream is)
	{
		Bitmap b = null;
		try
		{
			b = BitmapFactory.decodeStream(is);
			Logger.wtf(TAG, "Bitmap size in decodeStream : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeStream(is);
				Logger.wtf(TAG, "Bitmap size in decodeStream : " + BitmapUtils.getBitmapSize(b));
			}
			catch (OutOfMemoryError ex)
			{
				Logger.wtf(TAG, "Out of Memory even after System.gc");
			}
			catch (Exception exc)
			{
				Logger.e(TAG, "Exception in decodeStream : ", exc);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decodeStream : ", e);
		}
		return b;
	}

	public static Bitmap decodeResource(Resources res, int id)
	{
		Bitmap b = null;
		try
		{
			b = BitmapFactory.decodeResource(res, id);
			Logger.wtf(TAG, "Bitmap size in decodeResource : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeResource(res, id);
				Logger.wtf(TAG, "Bitmap size in decodeResource : " + BitmapUtils.getBitmapSize(b));
			}
			catch (OutOfMemoryError ex)
			{
				Logger.wtf(TAG, "Out of Memory even after System.gc");
			}
			catch (Exception exc)
			{
				Logger.e(TAG, "Exception in decodeResource : ", exc);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decodeResource : ", e);
		}
		return b;
	}

	public static Bitmap decodeResource(Resources res, int id, BitmapFactory.Options opt)
	{
		Bitmap b = null;
		try
		{
			b = BitmapFactory.decodeResource(res, id, opt);
			Logger.wtf(TAG, "Bitmap size in decodeResource : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeResource(res, id, opt);
				Logger.wtf(TAG, "Bitmap size in decodeResource : " + BitmapUtils.getBitmapSize(b));
			}
			catch (OutOfMemoryError ex)
			{
				Logger.wtf(TAG, "Out of Memory even after System.gc");
			}
			catch (Exception exc)
			{
				Logger.e(TAG, "Exception in decodeResource : ", exc);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decodeResource : ", e);
		}
		return b;
	}

	public static Bitmap decodeByteArray(byte[] data, int offset, int length)
	{
		Bitmap b = null;
		try
		{
			b = BitmapFactory.decodeByteArray(data, offset, length);
			Logger.wtf(TAG, "Bitmap size in decodeByteArray : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeByteArray(data, offset, length);
				Logger.wtf(TAG, "Bitmap size in decodeByteArray : " + BitmapUtils.getBitmapSize(b));
			}
			catch (OutOfMemoryError ex)
			{
				Logger.wtf(TAG, "Out of Memory even after System.gc");
			}
			catch (Exception exc)
			{
				Logger.e(TAG, "Exception in decodeByteArray : ", exc);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decodeByteArray : ", e);
		}
		return b;
	}

	public static Bitmap decodeByteArray(byte[] data, int offset, int length, BitmapFactory.Options opt)
	{
		Bitmap b = null;
		try
		{
			b = BitmapFactory.decodeByteArray(data, offset, length, opt);
			Logger.wtf(TAG, "Bitmap size in decodeByteArray : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeByteArray(data, offset, length, opt);
				Logger.wtf(TAG, "Bitmap size in decodeByteArray : " + BitmapUtils.getBitmapSize(b));
			}
			catch (OutOfMemoryError ex)
			{
				Logger.wtf(TAG, "Out of Memory even after System.gc called");
			}
			catch (Exception exc)
			{
				Logger.e(TAG, "Exception in decodeByteArray : ", exc);
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decodeByteArray : ", e);
		}
		return b;
	}

	/**
	 * returns scaled down bitmap if finResMoreThanReq is set to true than return bitmap resolution will be atleast reqHeight and reqWidth and if set to false will be at most
	 * reqWidth and reqHeight
	 * 
	 * @param filename
	 * @param reqWidth
	 * @param reqHeight
	 * @param finResMore
	 * @return
	 */
	public static Bitmap scaleDownBitmap(String filename, int reqWidth, int reqHeight, boolean finResMoreThanReq, boolean scaleUp)
	{
		return scaleDownBitmap(filename, reqWidth, reqHeight, Bitmap.Config.ARGB_8888, finResMoreThanReq, scaleUp);
	}

	public static Bitmap scaleDownBitmap(String filename, int reqWidth, int reqHeight, Bitmap.Config config, boolean finResMoreThanReq, boolean scaleUp)
	{
		Bitmap unscaledBitmap = decodeSampledBitmapFromFile(filename, reqWidth, reqHeight, config);

		if (unscaledBitmap == null)
		{
			return null;
		}

		Bitmap small = createScaledBitmap(unscaledBitmap, reqWidth, reqHeight, config, true, finResMoreThanReq, scaleUp);

		if (unscaledBitmap != small)
		{
			unscaledBitmap.recycle();
		}

		return small;

	}

	public static Bitmap createScaledBitmap(Bitmap unscaledBitmap, int reqWidth, int reqHeight, Bitmap.Config config, boolean filter, boolean finResMore, boolean scaleUp)
	{
		if (unscaledBitmap == null)
		{
			return null;
		}

		if (scaleUp || reqHeight < unscaledBitmap.getHeight() && reqWidth < unscaledBitmap.getWidth())
		{
			Rect srcRect = new Rect(0, 0, unscaledBitmap.getWidth(), unscaledBitmap.getHeight());

			Rect reqRect = calculateReqRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(), reqWidth, reqHeight, finResMore);

			Bitmap scaledBitmap = createBitmap(reqRect.width(), reqRect.height(), config);

			if (scaledBitmap == null)
			{
				return null;
			}

			Canvas canvas = new Canvas(scaledBitmap);
			Paint p = new Paint();
			p.setFilterBitmap(filter);
			canvas.drawBitmap(unscaledBitmap, srcRect, reqRect, p);
			return scaledBitmap;
		}
		else
		{
			return unscaledBitmap;
		}
	}

	private static Rect calculateReqRect(int srcWidth, int srcHeight, int reqWidth, int reqHeight, boolean finResMore)
	{
		final float srcAspect = (float) srcWidth / (float) srcHeight;
		final float dstAspect = (float) reqWidth / (float) reqHeight;

		if (finResMore)
		{
			if (srcAspect > dstAspect)
			{
				return new Rect(0, 0, (int) (reqHeight * srcAspect), reqHeight);
			}
			else
			{
				return new Rect(0, 0, reqWidth, (int) (reqWidth / srcAspect));
			}
		}
		else
		{
			if (srcAspect > dstAspect)
			{
				return new Rect(0, 0, reqWidth, (int) (reqWidth / srcAspect));
			}
			else
			{
				return new Rect(0, 0, (int) (reqHeight * srcAspect), reqHeight);
			}
		}
	}
	
	public static Bitmap decodeSmallStickerFromObject(Object bitmapSourceObject, int reqWidth, int reqHeight, Bitmap.Config con)
	{
		if (bitmapSourceObject == null)
			return null;
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		decodeObject(bitmapSourceObject, options);

		options.inPreferredConfig = con;
		/*
		 * this is an hit and trial approx factor for our stickers. 
		 */
		options.inSampleSize = calculateApproxSampleSize(options, reqWidth, reqHeight, 0.2);
		options.inJustDecodeBounds = false;
		Bitmap result = decodeObject(bitmapSourceObject, options);
		if (options.inSampleSize < 2)
		{
			/*
			 * if we calculated our sample size to be greater then 1 than all fine. if some how it is not the case than we need to create a scaled Bitmap which fits exactly into our
			 * required window.
			 */
			result = createScaledBitmap(result, reqWidth, reqHeight, Bitmap.Config.ARGB_8888, true, true, false);
		}
		
		return result;
	}
	
	private static Bitmap decodeObject(Object object, BitmapFactory.Options options)
	{
		Bitmap bitmap = null;
		if(object instanceof byte[])
		{
			byte[] byteArray = (byte[]) object; 
			bitmap = decodeByteArray(byteArray, 0, byteArray.length, options);
		}
		else if (object instanceof String)
		{
			String filePath = (String) object;
			bitmap = decodeFile(filePath, options);
		}
		return bitmap;
	}
	
	/*
	 * calculate a near about sample size base on give error parameter
	 */
	private static int calculateApproxSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight, double error)
	{
		double oneMinusError = 1-error;
		reqHeight = (int)(reqHeight*oneMinusError);
		reqWidth = (int)(reqWidth*oneMinusError);
		
		return calculateInSampleSize(options, reqWidth, reqHeight);
	}
	public static BitmapDrawable getDefaultAvatar(Resources res, String msisdn, boolean hiRes)
	{
	
		int index = BitmapUtils.iconHash(msisdn) % (HikeConstants.DEFAULT_AVATAR_KEYS.length);

		int defaultAvatarResId = HikeConstants.DEFAULT_AVATARS[index]; 
		
		Drawable layers[] = new Drawable[2];
		layers[0] = res.getDrawable(defaultAvatarResId);
		layers[1] = res.getDrawable(getDefaultAvatarIconResId(msisdn, hiRes));
		
		LayerDrawable ld = new LayerDrawable(layers);
		ld.setId(0, 0);
		ld.setId(1, 1);
		ld.setDrawableByLayerId(0, layers[0]);
		ld.setDrawableByLayerId(1, layers[1]);
		
		Bitmap bmp = drawableToBitmap(ld);
		
		BitmapDrawable bd = getBitmapDrawable(res, bmp);
		return bd;
	}
	
	private static int getDefaultAvatarIconResId( String msisdn, boolean hiRes)
	{
		if (Utils.isBroadcastConversation(msisdn))
		{
			return hiRes ? R.drawable.ic_default_avatar_broadcast_hires : R.drawable.ic_default_avatar_broadcast;
		}
		else if (Utils.isGroupConversation(msisdn))
		{
			return hiRes ? R.drawable.ic_default_avatar_group_hires : R.drawable.ic_default_avatar_group;
		}
		else
		{
			return hiRes ? R.drawable.ic_default_avatar_hires : R.drawable.ic_default_avatar;
		}
	}

}
