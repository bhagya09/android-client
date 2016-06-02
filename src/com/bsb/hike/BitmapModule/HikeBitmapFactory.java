package com.bsb.hike.BitmapModule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
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
import android.media.ExifInterface;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.contactmgr.GroupDetails;
import com.bsb.hike.photos.HikePhotosListener;
import com.bsb.hike.smartcache.HikeLruCache;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.TextDrawable;

public class HikeBitmapFactory
{
	private static final String TAG = "HikeBitmapFactory";

	private static final int MEMORY_MULTIPLIIER = 8;
		
	private static final int RGB_565_BYTE_SIZE = 16;
		
	private static final int THRESHHOLD_LIMIT = 0; 
		
	public enum BitmapResolutionState {
		
		INIT_STATE(1), STATE_2(2), STATE_3(3), STATE_EXIT(4);
		
        private int value;

        BitmapResolutionState(int value) {
                this.value = value;
        }
        
        public int getValue()
		{
			return value;
		}
	};   

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
		Bitmap cacheBmp = Bitmap.createBitmap(textView.getDrawingCache());
		Bitmap viewBmp = null;
		if (cacheBmp != null)
		{
			viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
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

	public static Bitmap drawableToBitmap(Drawable drawable)
	{
		if (drawable instanceof BitmapDrawable)
		{
			return ((BitmapDrawable) drawable).getBitmap();
		}

		Bitmap bitmap;
		int width = Math.max(drawable.getIntrinsicWidth(), 2);
		int height = Math.max(drawable.getIntrinsicHeight(), 2);
		try
		{
			bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			bitmap = null;
		}

		return bitmap;
	}

	public static String getBase64ForDrawable(int drawableId, Context context)
	{

		BitmapDrawable drawable = (BitmapDrawable) context.getResources().getDrawable(drawableId);
		return Utils.drawableToString(drawable);
	}

	public static void correctBitmapRotation(final String srcFilePath, final HikePhotosListener mListener)
	{
		HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
		{
			@Override
			public void run()
			{
				Bitmap bmp = HikeBitmapFactory.decodeSampledBitmapFromFile(srcFilePath, HikeConstants.HikePhotos.MAX_IMAGE_DIMEN, HikeConstants.HikePhotos.MAX_IMAGE_DIMEN, Config.ARGB_8888);
				
				if(bmp==null)
				{
					bmp = HikeBitmapFactory.decodeSampledBitmapFromFile(srcFilePath, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX, HikeConstants.MAX_DIMENSION_MEDIUM_FULL_SIZE_PX, Config.ARGB_8888);
				}
				
				if(bmp==null)
				{
					mListener.onFailure();
					return;
				}
				try
				{
					ExifInterface ei = new ExifInterface(srcFilePath);
					int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
					Logger.d(TAG, "Orientation: " + orientation);
					switch (orientation)
					{
					case ExifInterface.ORIENTATION_ROTATE_90:
						bmp = HikeBitmapFactory.rotateBitmap(bmp, 90);
						break;
					case ExifInterface.ORIENTATION_ROTATE_180:
						bmp = HikeBitmapFactory.rotateBitmap(bmp, 180);
						break;
					case ExifInterface.ORIENTATION_ROTATE_270:
						bmp = HikeBitmapFactory.rotateBitmap(bmp, 270);
						break;
					}
				}
				catch (IOException ioe)
				{
					ioe.printStackTrace();
					mListener.onFailure();
				}
				mListener.onComplete(bmp);
			}
		}, 0);
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

		int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
		int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
		src = createScaledBitmap(src, width, height, Bitmap.Config.RGB_565, false, true, true);
		return src;
	}

	public static BitmapDrawable getBitmapDrawable(Resources mResources, final Bitmap bitmap)
	{
		if (bitmap == null)
			return null;

		return new BitmapDrawable(mResources, bitmap);
	}

	public static BitmapDrawable getBitmapDrawable(final Bitmap bitmap)
	{
		if (bitmap == null)
			return null;

		return new BitmapDrawable(bitmap);
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
		return decodeSampledBitmapFromFile(filename, reqWidth, reqHeight, con, null, false);
	}

	/**
	 * 
	 * Decode and sample down a bitmap from a file to the requested width and height.
	 * 
	 * @param filename
	 *            The full path of the file to decode
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @param con
	 *            Bitmap factory configurations
	 * @param argOptions
	 *            Bitmap factory options
	 * @param fitEqualOrSmall
	 *            If true, the returned bitmap will be equal or smaller than required width/height
	 * 
	 * @return
	 */
	public static Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight, Bitmap.Config con, BitmapFactory.Options argOptions, boolean fitEqualOrSmall)
	{
		// First decode with inJustDecodeBounds=true to check dimensions
		BitmapFactory.Options options = null;

		if (argOptions == null)
		{
			options = new BitmapFactory.Options();
		}
		else
		{
			options = argOptions;
		}
		options.inJustDecodeBounds = true;

		decodeFile(filename, options);

		options.inPreferredConfig = con;

		// Calculate inSampleSize
		if (fitEqualOrSmall)
		{
			options.inSampleSize = calculateSmallerInSampleSize(options, reqWidth, reqHeight);
		}
		else
		{
			options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		}

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
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
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
			while (((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth)
					|| (height/inSampleSize) > 4096 || (width/inSampleSize) > 4096) //OpenGL Surface limitation
			{
				inSampleSize *= 2;
			}

		}
		return inSampleSize;
	}
	
	/**
	 * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding bitmaps using the decode* methods from {@link BitmapFactory}. This implementation
	 * calculates the closest inSampleSize that is a power of 2 and will result in the final decoded bitmap having a width and height equal to or smaller than the requested width
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
	public static int calculateSmallerInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth)
		{
			inSampleSize *= 2;
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
			Logger.d(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = Bitmap.createBitmap(width, height, con);
				Logger.d(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));
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

	public static Bitmap createBitmap(Bitmap thumbnail, int startX, int startY, int i, int j)
	{
		Bitmap b = null;
		try
		{
			b = Bitmap.createBitmap(thumbnail, startX, startY, i, j);
			Logger.d(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));

		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = Bitmap.createBitmap(thumbnail, startX, startY, i, j);
				Logger.d(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));
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
			Logger.d(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = Bitmap.createBitmap(bm, i, j, width, height, m, c);
				Logger.d(TAG, "Bitmap size in createBitmap : " + BitmapUtils.getBitmapSize(b));
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
			Logger.d(TAG, "Bitmap size in decodeFile : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeFile(path);
				Logger.d(TAG, "Bitmap size in decodeFile : " + BitmapUtils.getBitmapSize(b));
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
			Logger.d(TAG, "Bitmap size in decodeFile : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeFile(path, opt);
				Logger.d(TAG, "Bitmap size in decodeFile : " + BitmapUtils.getBitmapSize(b));
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
			Logger.d(TAG, "Bitmap size in decodeStream : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeStream(is);
				Logger.d(TAG, "Bitmap size in decodeStream : " + BitmapUtils.getBitmapSize(b));
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
			Logger.d(TAG, "Bitmap size in decodeResource : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeResource(res, id);
				Logger.d(TAG, "Bitmap size in decodeResource : " + BitmapUtils.getBitmapSize(b));
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
			Logger.d(TAG, "Bitmap size in decodeResource : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeResource(res, id, opt);
				Logger.d(TAG, "Bitmap size in decodeResource : " + BitmapUtils.getBitmapSize(b));
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
			Logger.d(TAG, "Bitmap size in decodeByteArray : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeByteArray(data, offset, length);
				Logger.d(TAG, "Bitmap size in decodeByteArray : " + BitmapUtils.getBitmapSize(b));
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
			Logger.d(TAG, "Bitmap size in decodeByteArray : " + BitmapUtils.getBitmapSize(b));
		}
		catch (OutOfMemoryError e)
		{
			Logger.wtf(TAG, "Out of Memory");

			System.gc();

			try
			{
				b = BitmapFactory.decodeByteArray(data, offset, length, opt);
				Logger.d(TAG, "Bitmap size in decodeByteArray : " + BitmapUtils.getBitmapSize(b));
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

	public static Drawable getDefaultTextAvatar(String text)
	{
		return getDefaultTextAvatar(text, -1);
	}

	public static Drawable getDefaultTextAvatar(String text, int fontSize)
	{
		return getDefaultTextAvatar(text,fontSize,-1);
	}

	public static Drawable getDefaultTextAvatar(String text, int fontSize, int argBgColor)
	{
		return getDefaultTextAvatar(text, fontSize, argBgColor,false);
	}

	public static Drawable getDefaultTextAvatar(String text, int fontSize, int argBgColor, boolean isFirstName)
	{
		if (TextUtils.isEmpty(text))
		{
			return getRandomHashTextDrawable();
		}

		String initials = null;

		if (isFirstName)
		{
			initials = Utils.getInitialsFromContactName(text);
		}
		else
		{
			initials = getNameInitialsForDefaultAv(text);
		}

		int bgColor = argBgColor;

		if (bgColor == -1)
		{
			TypedArray bgColorArray = Utils.getDefaultAvatarBG();

			int index = BitmapUtils.iconHash(text) % (bgColorArray.length());

			bgColor = bgColorArray.getColor(index, 0);
		}

		TextDrawable drawable = null;
		if (fontSize != -1)
		{
			drawable =  TextDrawable.builder().beginConfig().fontSize(fontSize).endConfig().buildRound(initials, bgColor);
		}
		else
		{
			drawable = TextDrawable.builder().buildRound(initials, bgColor);
		}

		//https://github.com/facebook/fresco/issues/501
		drawable.setPadding(new Rect());

		return drawable;
	}
	
	public static TextDrawable getRandomHashTextDrawable()
	{
		return getRandomHashTextDrawable(-1);
	}

	public static TextDrawable getRandomHashTextDrawable(int argBgColor)
	{
		int bgColor = argBgColor;

		if (argBgColor == -1)
		{
			TypedArray bgColorArray = Utils.getDefaultAvatarBG();

			bgColor = bgColorArray.getColor(new Random().nextInt(bgColorArray.length()), 0);
		}

		return TextDrawable.builder().buildRound("#", bgColor);
	}

	public static Drawable getRectTextAvatar(String text)
	{
		TypedArray bgColorArray = Utils.getDefaultAvatarBG();

		if (TextUtils.isEmpty(text))
		{
			return getRandomHashTextDrawable();
		}

		String initials = getNameInitialsForDefaultAv(text);

		int index = BitmapUtils.iconHash(text) % (bgColorArray.length());

		int bgColor = bgColorArray.getColor(index, 0);

		return TextDrawable.builder().buildRect(initials, bgColor);
	}

	public static String getNameInitialsForDefaultAv(String msisdn)
	{
		if (TextUtils.isEmpty(msisdn.trim()))
		{
			return "#";
		}

		String contactName = msisdn;

		if (OneToNConversationUtils.isOneToNConversation(msisdn) && ContactManager.getInstance().getGroupDetails(msisdn) != null)
		{
			GroupDetails groupDetails = ContactManager.getInstance().getGroupDetails(msisdn);

			contactName = groupDetails.getGroupName();
		}
		else if(BotUtils.isBot(msisdn))
		{
			BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
			contactName = botInfo.getConversationName();
		}
		else
		{
			ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, true, true, false);

			contactName = contactInfo.getName();

			if (ContactManager.getInstance().getSelfMsisdn().equals(msisdn))
			{
				contactName = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.NAME_SETTING, msisdn);
			}

			if (contactName == null)
			{
				contactName = msisdn;
			}
		}

		return Utils.getInitialsFromContactName(contactName);
	}

	private static int getDefaultAvatarIconResId( String msisdn, boolean hiRes)
	{
		if (OneToNConversationUtils.isBroadcastConversation(msisdn))
		{
			return hiRes ? R.drawable.ic_default_avatar_broadcast_hires : R.drawable.ic_default_avatar_broadcast;
		}
		else if (OneToNConversationUtils.isGroupConversation(msisdn))
		{
			return hiRes ? R.drawable.ic_default_avatar_group_hires : R.drawable.ic_default_avatar_group;
		}
		else
		{
			return hiRes ? R.drawable.ic_default_avatar_hires : R.drawable.ic_default_avatar;
		}
	}
	
	public static BitmapFactory.Options getImageOriginalSizeBitmap(String filename)
	{
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		decodeFile(filename, options);

		options.inJustDecodeBounds = false;
		return options;
	}

	/**
	 * Algorithm Description:-
	 * 
	 * Let X = Max available RAM for Hike
	 * 
	 * Check1: if RAM is available to load original image but not greater than screen size X 2.....
	 * 
	 * X (Max available RAM for Hike) > 8 X 16(for RGB_565_BYTE_SIZE) X Minimum(Original Image size, Screen Width X 2) =========>if yes go for this for loading image
	 * 
	 * ========>If No then
	 * 
	 * Check 2: A) Calculate inSampleSize with original image width and height B) Max Dimens of image i.e Max of (width, height) C) Found insampledBitmapArea Now Check if RAM is
	 * available to load with size Max of insampledBitmapArea and screen width X 1.5
	 * 
	 * X (Max available RAM for Hike) > 8 X 16(for RGB_565_BYTE_SIZE) X Maximum(insampledBitmapArea, Screen Width X 1.5) =========>if yes go for this for loading image
	 * 
	 * =========> If No then
	 * 
	 * fallback to our current condition ====> RGB and Screen Width
	 * 
	 * @param context
	 * @param filename
	 * @param ImageSize
	 * @param state
	 *            :
	 * @return
	 */
	public static Bitmap getBestResolutionBitmap(Context context, String filename, Dimension defaultSize, BitmapResolutionState state)
	{
		MemmoryScreenShot screenShot = new MemmoryScreenShot(filename, defaultSize);
		Logger.d("image_config", screenShot.toString());

		/**
		 * If by any reasons 
		 * options.outWidth = -1
		 * options.outHeight = -1
		 * 
		 * go to 3rd case
		 */
		if(screenShot.options.outWidth == -1 || screenShot.options.outHeight == -1)
		{
			state = BitmapResolutionState.STATE_3;
		}
		
		Dimension bestDimen = getBestDimensions(state, screenShot);

		Bitmap thumbnail = null;
		Logger.d("image_config", "Going to try load image with case:- " + bestDimen.toString());
		thumbnail = HikeBitmapFactory.scaleDownBitmap(filename, bestDimen.getWidth(), bestDimen.getHeight(), Bitmap.Config.RGB_565, true, false);
		if (thumbnail == null)
		{
			Logger.d("image_config", "degrading loading image qulity from case " + bestDimen.getState() + " to next case");
			bestDimen.nextState();
			if (bestDimen.getState() != BitmapResolutionState.STATE_EXIT)
			{
				showSCToastForImageDegrade(context.getResources().getString(R.string.show_degraded_image_quality));
				return getBestResolutionBitmap(context, filename, defaultSize, bestDimen.getState());
			}
			else
			{
				Logger.d("image_config", "Showing thumbnail for last fallback ");
				showSCToastForImageDegrade(context.getResources().getString(R.string.toast_for_showing_thumbnail));
				// TODO Show Thumbnail
			}
		}
		Logger.d("image_config", "Successfully Loaded Image");
		return thumbnail;
	}

	private static Dimension getBestDimensions(BitmapResolutionState state, MemmoryScreenShot screenShot)
	{
		Dimension imageDimen = null;

		if (state == BitmapResolutionState.INIT_STATE)
		{
			// Going for 1st case
			imageDimen = getDimensionsAsPerCase1(screenShot);

			if (imageDimen != null)
			{
				// Best match found, returning
				Logger.d("image_config", "API getBestDimensions, best case " + state + ", \n size is " + imageDimen.toString());
				imageDimen.setState(state);
				return imageDimen;
			}

			//First case not successful, Moving on to 2nd case
			state = BitmapResolutionState.STATE_2;
		}

		if (state == BitmapResolutionState.STATE_2)
		{
			// Going for 2nd case
			imageDimen = getDimensionsAsPerCase2(screenShot);

			if (imageDimen != null)
			{
				// Best match found, returning
				Logger.d("image_config", "API getBestDimensions, best case " + state + ", \n size is " + imageDimen.toString());
				imageDimen.setState(state);
				return imageDimen;
			}

			//Second case not successful, Moving on to 3rd case
			state = BitmapResolutionState.STATE_3;
		}

		// Going for 3rd case
		if (state == BitmapResolutionState.STATE_3)
		{
			Logger.d("image_config", "API getBestDimensions, best case " + state + ", \n size is " + screenShot.getDefaultDimen().toString());
			imageDimen = screenShot.getDefaultDimen();
			imageDimen.setState(state);
			return imageDimen;
		}

		Logger.d("image_config", "False State...Some problem is there");
		return null;

	}

	/**
	 * Show Toast only if it is set by server via packet.
	 * 
	 * @param context
	 * @param textToShow
	 */
	private static void showSCToastForImageDegrade(final String textToShow)
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_TOAST_FOR_DEGRADING_QUALITY, false) == true)
		{
			HikeMessengerApp.getInstance().appStateHandler.post(new Runnable()
			{

				@Override
				public void run()
				{
					Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), textToShow, Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	// Case2: checking with RGB+ Max 1240/Screen*1.5
	private static Dimension getDimensionsAsPerCase2(MemmoryScreenShot screenShot)
	{
		int height;

		int width;

		int max;

		int inSampleSize = calculateInSampleSize(screenShot.getOptions(), screenShot.getOptions().outWidth, screenShot.getOptions().outHeight);

		int maxDimen = screenShot.getOptions().outWidth < screenShot.getOptions().outHeight ? screenShot.getOptions().outHeight : screenShot.getOptions().outWidth;

		int h = 0, w = 0;

		if ((int) (maxDimen / (2 ^ inSampleSize)) > HikeConstants.SMO_MAX_DIMENSION_MEDIUM_FULL_SIZE_PX + THRESHHOLD_LIMIT)
		{
			float aspectRatio = screenShot.getOptions().outWidth * 1.0f / (screenShot.getOptions().outHeight);

			if (screenShot.getOptions().outWidth >= screenShot.getOptions().outHeight)
			{

				w = HikeConstants.SMO_MAX_DIMENSION_MEDIUM_FULL_SIZE_PX;
				h = (int) (HikeConstants.SMO_MAX_DIMENSION_MEDIUM_FULL_SIZE_PX / aspectRatio);
			}
			else
			{
				h = HikeConstants.SMO_MAX_DIMENSION_MEDIUM_FULL_SIZE_PX;
				w = (int) (HikeConstants.SMO_MAX_DIMENSION_MEDIUM_FULL_SIZE_PX * aspectRatio);
			}
		}
		else
		{
			h = screenShot.getOptions().outHeight / (2 ^ inSampleSize);
			w = screenShot.getOptions().outWidth / (2 ^ inSampleSize);
		}

		int insampledBitmapArea = h * w;

		if (insampledBitmapArea > screenShot.getDeviceScreenArea() * 1.5)
		{
			height = h;
			width = w;
			max = insampledBitmapArea;
		}
		else
		{
			Pair<Integer, Integer> dimension = getDimensionsAsPerAspectRatio(screenShot);
			width = dimension.first;
			height = dimension.second;
			max = (int) (width * height * 1.5);
		}

		if (screenShot.getAvailableRAM() > (MEMORY_MULTIPLIIER * (RGB_565_BYTE_SIZE * max)))
		{
			return new Dimension(width, height);
		}
		return null;
	}

	// Case1: checking with RGB+ original/Screen*2
	private static Dimension getDimensionsAsPerCase1(MemmoryScreenShot screenShot)
	{
		int height;

		int width;

		int min;

		if (screenShot.getImageOriginalArea() > screenShot.getDeviceScreenArea() * 2)
		{
			Pair<Integer, Integer> dimensions = getDimensionsAsPerAspectRatio(screenShot);
			width = dimensions.first;
			height = dimensions.second;
			min = width * height * 2;
		}
		else
		{
			height = screenShot.getOptions().outHeight;
			width = screenShot.getOptions().outWidth;
			min = screenShot.getImageOriginalArea();
		}

		if (screenShot.getAvailableRAM() > (MEMORY_MULTIPLIIER * (RGB_565_BYTE_SIZE * min)))
		{
			return new Dimension(height, width);
		}
		return null;
	}

	public static class MemmoryScreenShot
	{
		private double availableRAM;

		private BitmapFactory.Options options;

		private int imageOriginalArea;

		private int deviceScreenArea;

		private Dimension defaultDimen;

		public double getAvailableRAM()
		{
			return availableRAM;
		}

		public MemmoryScreenShot(String filename, Dimension imageSize)
		{
			availableRAM = Utils.getTotalRAMForHike();
			options = getImageOriginalSizeBitmap(filename);
			Logger.d("image_config", "Image original dimens are :- " + options.outWidth + ", " + options.outHeight);
			imageOriginalArea = options.outHeight * options.outWidth;
			deviceScreenArea = (Utils.getDeviceScreenArea());
			defaultDimen = imageSize;
		}

		public BitmapFactory.Options getOptions()
		{
			return options;
		}

		public int getImageOriginalArea()
		{
			return imageOriginalArea;
		}

		public void setImageOriginalArea(int imageOriginalArea)
		{
			this.imageOriginalArea = imageOriginalArea;
		}

		public int getDeviceScreenArea()
		{
			return deviceScreenArea;
		}

		public void setDeviceScreenArea(int deviceScreenArea)
		{
			this.deviceScreenArea = deviceScreenArea;
		}

		public Dimension getDefaultDimen()
		{
			return defaultDimen;
		}

		public void setDefaultDimen(Dimension screenDimen)
		{
			this.defaultDimen = screenDimen;
		}

		@Override
		public String toString()
		{
			return "MemmoryScreenShot  \n [availableRAM=" + availableRAM + ", \n imageOriginalArea=" + imageOriginalArea + ", \n deviceScreenArea=" + deviceScreenArea + "]";
		}

	}

	/**
	 * 
	 */
	public static class Dimension
	{

		int width;

		int height;

		BitmapResolutionState algoState;

		public Dimension(int w, int h)
		{
			width = w;
			height = h;
		}

		public void nextState()
		{
			switch (this.algoState)
			{
			case INIT_STATE:
				this.algoState = BitmapResolutionState.STATE_2;
				break;

			case STATE_2:
				this.algoState = BitmapResolutionState.STATE_3;
				break;

			case STATE_3:
				this.algoState = BitmapResolutionState.STATE_EXIT;
				break;

			default:
				this.algoState = BitmapResolutionState.STATE_EXIT;
				break;
			}
		}

		public int getWidth()
		{
			return width;
		}

		public int getHeight()
		{
			return height;
		}

		public BitmapResolutionState getState()
		{
			return algoState;
		}

		public void setState(BitmapResolutionState algoState)
		{
			this.algoState = algoState;
		}

		@Override
		public String toString()
		{
			return "AlgoBestDimensionResult [width=" + width + ", height=" + height + ", algoState=" + algoState + "]";
		}

	}

	private static Pair<Integer, Integer> getDimensionsAsPerAspectRatio(MemmoryScreenShot screenShot)
	{
		int deviceHeight = HikeMessengerApp.getInstance().getApplicationContext().getResources().getDisplayMetrics().heightPixels;
		int deviceWidth = HikeMessengerApp.getInstance().getApplicationContext().getResources().getDisplayMetrics().widthPixels;

		int imgWidth = screenShot.getOptions().outWidth;
		int imgHeight = screenShot.getOptions().outHeight;
		float imageAspectRatio = imgWidth * 1.0f / imgHeight;

		if (imgWidth > imgHeight)
		{
			if (imgWidth > deviceWidth)
			{
				imgWidth = deviceWidth;
				imgHeight = (int) (imgWidth / imageAspectRatio);
			}
		}
		else
		{
			if (imgHeight > deviceHeight)
			{
				imgHeight = deviceHeight;
				imgWidth = (int) (imgHeight * imageAspectRatio);
			}
		}

		return new Pair<>(imgWidth, imgHeight);
	}

}
