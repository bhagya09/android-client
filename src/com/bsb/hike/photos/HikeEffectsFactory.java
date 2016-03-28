package com.bsb.hike.photos;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.Bitmap.Config;
import android.graphics.ColorMatrixColorFilter;
import android.os.Handler;
import android.os.Looper;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RSRuntimeException;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;
import com.bsb.hike.photos.views.VignetteUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;

/**
 * 
 * Factory model class. Effect Filter being applied using RenderScript class in android.
 * 
 * To apply the effect blending , color matrix and curve fitting techniques of Image Processing have been implemented.
 * 
 * Vignette is not added onto the image in this class.
 * 
 * @see http://developer.android.com/guide/topics/renderscript/compute.html
 * 
 * @see http://developer.android.com/reference/android/graphics/ColorMatrix.html
 * 
 * 
 */

public final class HikeEffectsFactory
{

	private static HikeEffectsFactory instance;// singleton instance

	private Bitmap mBitmapIn, currentOut, mBitmapOut2, mBitmapOut1, vignetteBitmap, finalBitmap;

	private RenderScript mRS;

	private Allocation mInAllocation, mOutAllocations, mBlendAllocation;

	private ScriptC_HikePhotosEffects mScript;

	private ScriptIntrinsicBlur mScriptBlur;

	private boolean loadRenderScript(Bitmap image, boolean isThumbnail, boolean isFinal)
	{
		// Initialize RS
		
		// Load script

		boolean ret = true;

		try
		{
			if (mRS == null)
			{
				mRS = RenderScript.create(HikeMessengerApp.getInstance().getApplicationContext());
				mScript = new ScriptC_HikePhotosEffects(mRS);
				mScriptBlur = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fallback(e);
			finish();
			return false;
		}

		if(mRS == null || image == null)
		{
			finish();
			return false;
		}
		
		// Allocate buffer
		mBitmapIn = image;
		mInAllocation = Allocation.createFromBitmap(mRS, mBitmapIn);
		if (isFinal)
		{
				finalBitmap = HikePhotosUtils.createBitmap(mBitmapIn, 0, 0, 0, 0, false, false, false, true,Config.ARGB_8888);
				vignetteBitmap = HikePhotosUtils.createBitmap(mBitmapIn, 0, 0, 0, 0, false, false, false, true,Config.ARGB_8888);
				currentOut = finalBitmap;
		}
		else if (!isThumbnail)
		{
			if (currentOut == null || (finalBitmap == null && (currentOut.getHeight() != mBitmapIn.getHeight() || currentOut.getWidth() != mBitmapIn.getWidth())))
			{
				mBitmapOut1 = HikePhotosUtils.createBitmap(mBitmapIn, 0, 0, 0, 0, false, false, false, true,Config.ARGB_8888);
				mBitmapOut2 = HikePhotosUtils.createBitmap(mBitmapIn, 0, 0, 0, 0, false, false, false, true,Config.ARGB_8888);
				vignetteBitmap = HikePhotosUtils.createBitmap(mBitmapIn, 0, 0, 0, 0, false, false, false, true,Config.ARGB_8888);
				currentOut = mBitmapOut1;
			}
			else if (currentOut != null && ((currentOut.getHeight() == mBitmapIn.getHeight() && currentOut.getWidth() == mBitmapIn.getWidth()) || finalBitmap != null))
			{
				if (currentOut == mBitmapOut1)
				{
					currentOut = mBitmapOut2;
				}
				else
				{
					currentOut = mBitmapOut1;
				}
			}
		}

		if (!isThumbnail && (currentOut == null || vignetteBitmap == null || mBitmapIn == null))
		{
			ret = false;
		}

		return ret;

	}

	private void fallback(Exception rre)
	{
		// Disable photos
		HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.ACCOUNT_SETTINGS).saveData(HikeConstants.Extras.ENABLE_PHOTOS, false);

		// Send logs to analytics
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.EVENT_KEY, "RSRuntimeException");
			json.put(AnalyticsConstants.LOG_KEY, rre.getStackTrace().toString());
			HAManager.getInstance().record(AnalyticsConstants.DEV_EVENT, AnalyticsConstants.PHOTOS_ERROR_EVENT, HAManager.EventPriority.HIGH, json,
					AnalyticsConstants.EVENT_TAG_PHOTOS);
		}
		catch (NullPointerException npe)
		{
			npe.printStackTrace();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Method To Clear HikeEffectFactory's singleton object and attributes associated with it. Recycles all bitmaps. Should be called only when no further effects are to be
	 * applied.
	 */

	public static void finish()
	{
		Log.e("com.bsb.hike", "collecting garbage");
		if (instance != null)
		{
			instance.mBlendAllocation = null;
			instance.mInAllocation = null;
			instance.mOutAllocations = null;
			if (instance.finalBitmap != null)
			{
				HikePhotosUtils.manageBitmaps(instance.finalBitmap);
			}
			if (instance.mBitmapIn != null)
			{
				HikePhotosUtils.manageBitmaps(instance.mBitmapIn);
			}
			if (instance.mBitmapOut1 != null)
			{
				HikePhotosUtils.manageBitmaps(instance.mBitmapOut1);
			}
			if (instance.mBitmapOut2 != null)
			{
				HikePhotosUtils.manageBitmaps(instance.mBitmapOut2);
			}
			if (instance.vignetteBitmap != null)
			{
				HikePhotosUtils.manageBitmaps(instance.vignetteBitmap);
			}
			
			instance = null;
		}
	}

	/**
	 * Method initiates an async task to apply filter to the provided thumbnail (obtained by scaling the image to be handled). Run on a background since loading preview can take
	 * some time in case of complex filters or large filter count. Till then the original image is displayed.
	 */
	public static void loadPreviewThumbnail(Bitmap scaledOriginal, FilterType type, OnFilterAppliedListener listener)
	{
		if (instance == null)
			instance = new HikeEffectsFactory();

		if (!instance.loadRenderScript(scaledOriginal, true, false))
		{
			Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(),
					HikeMessengerApp.getInstance().getApplicationContext().getResources().getString(R.string.photos_oom_load), Toast.LENGTH_SHORT).show();

			Intent homeActivityIntent = IntentFactory.getHomeActivityIntent(HikeMessengerApp.getInstance().getApplicationContext());
			homeActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			HikeMessengerApp.getInstance().getApplicationContext().startActivity(homeActivityIntent);
			
			return;
		}
		instance.beginEffectAsyncTask(listener, type, true);

	}


	public static ColorMatrixColorFilter getGreenDownShiftFilter()
	{
		float[] colorTransform = {
				1, 0, 0, 0, 0,
				0, 1, 0, 0, -3f,
				0, 0, 1, 0, 0,
				0, 0, 0, 1, 0
		};

		ColorMatrix colorMatrix = new ColorMatrix();
		colorMatrix.setSaturation(0f); //Remove Colour
		colorMatrix.set(colorTransform);

		ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
		return colorFilter;
	}

	/**
	 * @author akhiltripathi
	 * 
	 * 
	 * @param type
	 *            : FilterType enum for the type of effect for which the colormatrix is applied
	 * 
	 *            isPreMatrix : boolean value which defines wether the matrix will be applied to the bitmap before or after Blending / Curve Fitting
	 * 
	 *            value : Float defining the percentage of filter to be applied
	 * 
	 * @return Method returns the color matrix to be applied for a particular filter
	 * 
	 */
	private ColorMatrix getColorMatrixforFilter(FilterType type, boolean isPreMatrix, float value)
	{
		if (type == null)
		{
			return null;
		}
		ColorMatrix filterColorMatrix = null;
		switch (type)
		{

		case SEPIA:
			filterColorMatrix = getSepiaColorMatrix(value);
			break;
		case GRAYSCALE:
			filterColorMatrix = getBlackAndWhiteColorMatrix();
			break;
		case POLAROID:
			filterColorMatrix = getPolaroidColorMatrix();
			break;
		case FADED:
			filterColorMatrix = getFadedColorMatrix();
			break;
		case BGR:
			filterColorMatrix = getBGRColorMatrix();
			break;
		case X_PRO_2:
			if (isPreMatrix)
			{
				return null;
			}
			else
			{
				filterColorMatrix = getContrastColorMatrix(30f);
			}
			break;
		case APOLLO:
			if (isPreMatrix)
			{
				filterColorMatrix = getSaturationColorMatrix(0.5f);
			}
			else
			{
				filterColorMatrix = getBrightnessColorMatrix(1.4f);
				filterColorMatrix.setConcat(getContrastColorMatrix(-20f), filterColorMatrix);
			}
			break;
		case BRANNAN:
			if (isPreMatrix)
			{
				filterColorMatrix = getSaturationColorMatrix(0.6f);
			}
			break;
		case NASHVILLE:
			if (!isPreMatrix)
			{
				filterColorMatrix = getBrightnessColorMatrix(1.3f);
				filterColorMatrix.setConcat(getContrastColorMatrix(15f), filterColorMatrix);
			}
			break;
		case EARLYBIRD:
			if (isPreMatrix)
			{
				filterColorMatrix = getSaturationColorMatrix(0.68f);
				filterColorMatrix.setConcat(getBrightnessColorMatrix(1.15f), filterColorMatrix);
				filterColorMatrix.setConcat(getSaturationColorMatrix(1.2f), filterColorMatrix);
			}
			break;
		case INKWELL:
			if (isPreMatrix)
			{
				filterColorMatrix = getSaturationColorMatrix(0);
			}
			else
			{
				filterColorMatrix = getBrightnessColorMatrix(0.9f);
				filterColorMatrix.setConcat(getContrastColorMatrix(35f), filterColorMatrix);
			}
			break;
		case LO_FI:
			if (isPreMatrix)
			{
				filterColorMatrix = getBrightnessColorMatrix(1.35f);
				filterColorMatrix.setConcat(getContrastColorMatrix(30f), filterColorMatrix);
			}
			break;
		case RETRO:
			if (isPreMatrix)
			{
				filterColorMatrix = getSepiaColorMatrix(50f);
			}
			break;
		case GHOSTLY:
			if (isPreMatrix)
			{
				filterColorMatrix = getSaturationColorMatrix(0f);
			}
			else
			{
				filterColorMatrix = getBrightnessColorMatrix(1.6f);
				filterColorMatrix.setConcat(getContrastColorMatrix(40f), filterColorMatrix);
			}
			break;
		case GULAAL:
			if (isPreMatrix)
			{
				filterColorMatrix = getBrightnessColorMatrix(1.3f);
				filterColorMatrix.setConcat(getContrastColorMatrix(100f), filterColorMatrix);
			}
			break;

		case HDR:
			if (isPreMatrix)
			{
				filterColorMatrix = getSaturationColorMatrix(0f);
				filterColorMatrix.setConcat(getInvertColorsColorMatrix(), filterColorMatrix);
			}
			else
			{
				filterColorMatrix = getInvertColorsColorMatrix();
			}
			break;
		default:
			filterColorMatrix = null;

		}
		return filterColorMatrix;
	}

	/**
	 * Method used in saving the final filter onto a bitmap.
	 * 
	 * @param bitmap
	 *            : The bitmap to which the filters have to be applied matrix : The Colormatrix object for the filter type to be applied.
	 * @return Bitmap with the given matrix filter applied to the given bitmap
	 * 
	 */

	public static boolean applyFilterToBitmap(Bitmap bitmap, OnFilterAppliedListener listener, FilterType type, boolean isFinal)
	{
		if (instance == null)
			instance = new HikeEffectsFactory();

		if (!instance.loadRenderScript(bitmap, false, isFinal))
		{
			return false;
		}

		instance.vignetteBitmap = VignetteUtils.getVignetteforFilter(instance.vignetteBitmap, type, true, false);

		instance.beginEffectAsyncTask(listener, type, false);

		return true;

	}

	/**
	 * Pushes the Image Processing on the looper thread.
	 * 
	 * Uses HikeHandlerUtil.java class for thread queuing.
	 * 
	 * Ensures all actions occur in a serialized manner
	 * 
	 * @param OnFilterAppliedListener
	 *            : The listener to be called once the filter is applied.
	 * 
	 *            type : FilterType enum for thr respective effect.
	 * 
	 *            blur : boolean wether to blur the output image or not. It should be true only in case of thumbnails.
	 * 
	 */

	private void beginEffectAsyncTask(OnFilterAppliedListener listener, FilterType type, boolean blur)
	{

		HikeHandlerUtil.getInstance().postRunnableWithDelay(new ApplyFilterTask(type, listener, blur), 0);

	}

	private ColorMatrix getSaturationColorMatrix(float value)
	{
		ColorMatrix ret = new ColorMatrix();
		ret.setSaturation(value);
		return ret;
	}

	private ColorMatrix getInvertColorsColorMatrix()
	{
		float[] array = new float[] { -1, 0, 0, 0, 255, 0, -1, 0, 0, 255, 0, 0, -1, 0, 255, 0, 0, 0, 1, 0 };
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix getBlackAndWhiteColorMatrix()
	{

		ColorMatrix matrixA = new ColorMatrix();
		matrixA.setSaturation(0);

		return matrixA;
	}

	private ColorMatrix getContrastColorMatrix(float value)
	{
		float scale = 1 + value / 100;
		float translate = (-.5f * scale + .5f) * 255.f;
		float[] array = new float[] { scale, 0, 0, 0, translate, 0, scale, 0, 0, translate, 0, 0, scale, 0, translate, 0, 0, 0, 1, 0 };
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix getBrightnessColorMatrix(float value)
	{
		value = (value - 1) * 100;
		float[] array = new float[] { 1, 0, 0, 0, value, 0, 1, 0, 0, value, 0, 0, 1, 0, value, 0, 0, 0, 1, 0 };
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix getBGRColorMatrix()
	{
		float[] array = new float[] { 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0 };
		ColorMatrix matrix = new ColorMatrix(array);

		return matrix;
	}

	private ColorMatrix getFadedColorMatrix()
	{

		ColorMatrix matrixA = new ColorMatrix(new float[] { .66f, .33f, .33f, 0, 0, // red
				.33f, .66f, .33f, 0, 0, // green
				.33f, .33f, .66f, 0, 0, // blue
				0, 0, 0, 1, 0 // alpha
				});

		return matrixA;
	}

	private ColorMatrix getPolaroidColorMatrix()
	{

		final ColorMatrix matrixA = new ColorMatrix(new float[] { 1.438f, -0.062f, -0.062f, 0f, 0, -0.122f, 1.378f, -0.122f, 0f, 0, -0.016f, -0.016f, 1.483f, 0f, 0, -0.03f, 0.05f,
				-0.02f, 0f, 1 });

		return matrixA;
	}

	private ColorMatrix getSepiaColorMatrix(float value)
	{
		value = value / 100;
		ColorMatrix sepiaMatrix = new ColorMatrix();
		float[] sepMat = { 1 - (1 - 0.3930000066757202f) * value, 0.7689999938011169f * value, 0.1889999955892563f * value, 0, 0, 0.3490000069141388f * value,
				1 - (1 - 0.6859999895095825f) * value, 0.1679999977350235f * value, 0, 0, 0.2720000147819519f * value, 0.5339999794960022f * value,
				1 - (1 - 0.1309999972581863f) * value, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1 };
		sepiaMatrix.set(sepMat);
		return sepiaMatrix;
	}

	// UI thread Handler object to make changes to the UI from a seperate thread
	private static Handler uiHandler = new Handler(Looper.getMainLooper());

	/**
	 * 
	 * Class implements Runnable Interface
	 * 
	 * Applies specific Effect to mBitmapIn object on a new thread
	 * 
	 * @author akhiltripathi
	 * 
	 * 
	 */
	public class ApplyFilterTask implements Runnable
	{

		private FilterType effect;

		private OnFilterAppliedListener readyListener;

		private Bitmap inBitmapOut;

		private boolean blurImage, error;

		public ApplyFilterTask(FilterType effectType, OnFilterAppliedListener listener, boolean isThumbnail)
		{
			// TODO Auto-generated constructor stub
			effect = effectType;
			readyListener = listener;
			blurImage = isThumbnail;
			if (blurImage)
			{
				inBitmapOut = HikePhotosUtils.createBitmap(mBitmapIn, 0, 0, 0, 0, false, false, false, true,Config.RGB_565);
				if (inBitmapOut != null)
				{
					mOutAllocations = Allocation.createFromBitmap(mRS, inBitmapOut);
				}
				else
				{
					error = true;
				}
			}
			else
			{
				mOutAllocations = Allocation.createFromBitmap(mRS, currentOut);
				mBlendAllocation = Allocation.createFromBitmap(mRS, vignetteBitmap);
			}

		}

		@Override
		public void run()
		{
			if (error)
			{
				return;
			}
			float[] preMatrix = getPreScriptEffects();
			if (preMatrix != null)
			{
				mScript.set_preMatrix(preMatrix);
			}

			float[] postMatrix = getPostScriptEffects();
			if (postMatrix != null)
			{
				mScript.set_postMatrix(postMatrix);
			}

			try
			{
				applyEffect(effect);
			}
			catch (RSRuntimeException e)
			{
				e.printStackTrace();
				error = true;
				Logger.e("RS Exception", "occured while applying : " + effect.toString());
			}
			catch (Exception e)
			{
				e.printStackTrace();
				error = true;
				Logger.e("Editor Exception", "occured while applying : " + effect.toString());
			}
			if (!error)
			{
				uiHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						readyListener.onFilterApplied(blurImage ? inBitmapOut : currentOut);

					}
				});
			}
		}

		private void applyEffect(FilterType effect)
		{
			int[] ro, ri, go, gi, bo, bi, ci, co;
			Splines red, green, blue, composite;
			Bitmap temp = null;
			
			if(mInAllocation == null || mOutAllocations == null)
			{
				error = true;
				return;
			}
			
			if (!blurImage)
			{
				mScript.set_input1(mBlendAllocation);
				mScript.set_isThumbnail(0);
				mScript.set_imageHeight(currentOut.getHeight());
				mScript.set_imageWidth(currentOut.getWidth());
			}
			else
			{
				mScript.set_isThumbnail(1);
				mScript.set_imageHeight(inBitmapOut.getHeight());
				mScript.set_imageWidth(inBitmapOut.getWidth());
			}
			
			switch (effect)
			{
			case CLASSIC:
				ri = new int[] { 0, 26, 51, 77, 102, 128, 154, 179, 204, 230, 255 };
				ro = new int[] { 37, 38, 45, 81, 148, 181, 196, 203, 207, 210, 214 };
				gi = new int[] { 0, 26, 51, 77, 102, 128, 153, 179, 204, 230, 255 };
				go = new int[] { 48, 50, 60, 95, 144, 174, 188, 201, 207, 213, 215 };
				bi = new int[] { 0, 26, 51, 77, 102, 128, 153, 179, 204, 230, 255 };
				bo = new int[] { 58, 61, 65, 72, 88, 108, 124, 135, 142, 153, 165 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_classic(mInAllocation, mOutAllocations);
				break;
			case E1977:

				ri = new int[] { 0, 22, 43, 63, 75, 96, 119, 147, 164, 191, 209, 235, 255 };
				ro = new int[] { 75, 81, 82, 113, 128, 151, 168, 201, 220, 222, 219, 225, 231 };
				gi = new int[] { 0, 16, 30, 43, 76, 89, 111, 130, 155, 176, 188, 216, 237, 255 };
				go = new int[] { 54, 59, 57, 58, 87, 104, 124, 144, 169, 190, 202, 222, 240, 244 };
				bi = new int[] { 0, 23, 40, 67, 90, 108, 134, 153, 175, 195, 211, 234, 255 };
				bo = new int[] { 65, 61, 65, 84, 118, 134, 167, 188, 209, 214, 210, 213, 209 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);

				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_1977(mInAllocation, mOutAllocations);

				break;
			case SOLOMON:

				mScript.set_r(new int[] { 0x33, 0xCD, 0 });
				mScript.set_g(new int[] { 0x27, 0x98, 0 });
				mScript.set_b(new int[] { 0xCD, 0x83, 0 });
				mScript.forEach_filter_solomon(mInAllocation, mOutAllocations);
				break;
			case KELVIN:
				ri = new int[] { 0, 27, 39, 69, 86, 109, 133, 151, 171, 193, 210, 235, 255 };
				ro = new int[] { 0, 37, 54, 111, 147, 185, 207, 220, 230, 234, 242, 247, 247 };
				gi = new int[] { 0, 22, 48, 68, 85, 107, 132, 151, 163, 192, 211, 235, 255 };
				go = new int[] { 0, 23, 51, 70, 92, 121, 154, 178, 192, 225, 245, 253, 255 };
				bi = new int[] { 0, 21, 42, 64, 88, 109, 127, 146, 166, 185, 218, 230, 255 };
				bo = new int[] { 0, 0, 0, 0, 7, 46, 94, 141, 186, 213, 242, 250, 255 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_kelvin(mInAllocation, mOutAllocations);
				break;
			case JALEBI:
				ri = new int[] { 0, 149, 255 };
				ro = new int[] { 64, 229, 255 };
				gi = new int[] { 0, 159, 255 };
				go = new int[] { 38, 181, 255 };
				bi = new int[] { 0, 255 };
				bo = new int[] { 62, 189 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.set_r(new int[] { 0xFF, 0, 0 });
				mScript.set_g(new int[] { 0xB0, 0, 0 });
				mScript.set_b(new int[] { 0x7C, 0, 0 });
				mScript.forEach_filter_jalebi(mInAllocation, mOutAllocations);
				break;
			case RETRO:
				ci = new int[] { 0, 91, 182, 255 };
				co = new int[] { 0, 85, 199, 255 };
				bi = new int[] { 0, 91, 255 };
				bo = new int[] { 0, 136, 255 };
				composite = new Splines(ci, co);
				blue = new Splines(bi, bo);
				mScript.set_compositeSpline(composite.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());

				mScript.set_r(new int[] { 0xF3, 0, 0 });
				mScript.set_g(new int[] { 0xE4, 0, 0 });
				mScript.set_b(new int[] { 0x8E, 0, 0 });
				mScript.forEach_filter_retro(mInAllocation, mOutAllocations);
				break;
			case X_PRO_2:

				ri = new int[] { 0, 23, 44, 63, 83, 106, 128, 149, 170, 187, 214, 235, 255 };
				ro = new int[] { 0, 13, 28, 48, 71, 102, 131, 160, 185, 209, 231, 246, 255 };
				gi = new int[] { 0, 20, 42, 62, 87, 103, 128, 146, 167, 190, 211, 234, 255 };
				go = new int[] { 0, 10, 25, 45, 75, 96, 130, 154, 183, 209, 231, 244, 255 };
				bi = new int[] { 0, 22, 41, 64, 82, 105, 128, 150, 170, 191, 212, 234, 255 };
				bo = new int[] { 31, 47, 62, 79, 95, 111, 129, 145, 161, 180, 196, 212, 255 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_xpro(mInAllocation, mOutAllocations);
				break;
			case APOLLO:
				ri = new int[] { 30, 120, 222, 255 };
				ro = new int[] { 20, 137, 221, 221 };
				gi = new int[] { 0, 117, 255 };
				go = new int[] { 0, 141, 255 };
				bi = new int[] { 0, 255 };
				bo = new int[] { 0, 255 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_apollo(mInAllocation, mOutAllocations);
				break;
			case BRANNAN:
				ri = new int[] { 0, 16, 40, 64, 82, 102, 127, 146, 159, 174, 202, 234, 255 };
				ro = new int[] { 37, 39, 54, 71, 92, 120, 167, 198, 220, 235, 246, 252, 255 };
				gi = new int[] { 0, 28, 43, 65, 92, 114, 130, 153, 175, 197, 208, 232, 255 };
				go = new int[] { 0, 15, 27, 50, 99, 147, 175, 202, 223, 237, 239, 250, 255 };
				bi = new int[] { 0, 20, 41, 64, 88, 117, 134, 155, 170, 188, 206, 227, 255 };
				bo = new int[] { 35, 41, 52, 64, 96, 138, 159, 178, 186, 203, 216, 232, 240 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_brannan(mInAllocation, mOutAllocations);
				break;
			case EARLYBIRD:
				ri = new int[] { 0, 25, 44, 67, 85, 104, 125, 148, 174, 190, 213, 234, 255 };
				ro = new int[] { 26, 60, 83, 113, 135, 157, 183, 198, 212, 223, 232, 244, 255 };
				gi = new int[] { 0, 25, 41, 60, 88, 104, 134, 153, 169, 196, 216, 235, 255 };
				go = new int[] { 0, 30, 53, 75, 113, 134, 173, 187, 196, 211, 219, 235, 240 };
				bi = new int[] { 0, 23, 44, 65, 93, 111, 121, 150, 165, 195, 212, 232, 255 };
				bo = new int[] { 17, 38, 58, 76, 104, 121, 131, 153, 171, 186, 196, 203, 211 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_earlyBird(mInAllocation, mOutAllocations);
				break;
			case INKWELL:
				ci = new int[] { 0, 16, 82, 151, 255 };
				co = new int[] { 0, 0, 88, 184, 224 };
				ri = new int[] { 0, 101, 129, 255 };
				ro = new int[] { 15, 92, 129, 255 };
				gi = new int[] { 0, 85, 128, 255 };
				go = new int[] { 15, 65, 128, 255 };
				bi = new int[] { 0, 59, 158, 255 };
				bo = new int[] { 10, 70, 170, 245 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				composite = new Splines(ci, co);
				mScript.set_compositeSpline(composite.getInterpolationMatrix());
				mScript.forEach_filter_inkwell(mInAllocation, mOutAllocations);
				break;
			case LO_FI:
				ci = new int[] { 0, 90, 170, 255 };
				co = new int[] { 0, 47, 171, 255 };
				composite = new Splines(ci, co);
				mScript.set_compositeSpline(composite.getInterpolationMatrix());
				mScript.forEach_filter_lomofi(mInAllocation, mOutAllocations);
				break;
			case NASHVILLE:
				ri = new int[] { 0, 20, 34, 45, 54, 65, 84, 98, 115, 132, 147, 172, 190, 214, 231, 255 };
				ro = new int[] { 0, 8, 14, 19, 20, 24, 86, 115, 141, 162, 183, 206, 223, 240, 246, 255 };
				gi = new int[] { 0, 22, 50, 63, 87, 100, 133, 150, 170, 192, 210, 234, 255 };
				go = new int[] { 0, 7, 64, 81, 105, 120, 152, 169, 183, 207, 212, 224, 226 };
				bi = new int[] { 0, 22, 42, 64, 84, 106, 131, 151, 168, 190, 212, 234, 250, 255 };
				bo = new int[] { 67, 74, 90, 102, 117, 126, 142, 154, 165, 181, 188, 201, 204, 204 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_nashville(mInAllocation, mOutAllocations);
				break;
			case POLAROID:
				ri = new int[] { 0, 23, 44, 64, 86, 106, 125, 149, 171, 192, 209, 237, 255 };
				ro = new int[] { 36, 51, 68, 90, 116, 143, 166, 187, 202, 215, 223, 244, 255 };
				gi = new int[] { 0, 24, 45, 68, 87, 103, 114, 141, 165, 192, 206, 232, 255 };
				go = new int[] { 0, 29, 59, 90, 117, 138, 153, 183, 200, 215, 225, 239, 255 };
				bi = new int[] { 0, 25, 36, 60, 90, 106, 126, 145, 171, 191, 211, 237, 255 };
				bo = new int[] { 0, 43, 59, 101, 149, 168, 195, 208, 219, 226, 237, 247, 247 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_nashville(mInAllocation, mOutAllocations);
				break;
			case ORIGINAL:
				mScript.forEach_filter_original(mInAllocation, mOutAllocations);
				break;
			case AUTO:
				mScript.forEach_filter_auto(mInAllocation, mOutAllocations);
				break;
			case GULAAL:
				ci = new int[] { 0, 102, 255 };
				co = new int[] { 0, 138, 255 };
				composite = new Splines(ci, co);
				mScript.set_compositeSpline(composite.getInterpolationMatrix());
				mScript.set_r(new int[] { 0x02, 0, 0 });
				mScript.set_g(new int[] { 0x04, 0, 0 });
				mScript.set_b(new int[] { 0x6B, 0, 0 });
				mScript.forEach_filter_gulaal(mInAllocation, mOutAllocations);
				break;
			case JUNGLEE:
				ri = new int[] { 0, 66, 178, 255 };
				ro = new int[] { 0, 0, 210, 255 };
				gi = new int[] { 0, 46, 255 };
				go = new int[] { 0, 0, 255 };
				bi = new int[] { 0, 50, 223, 255 };
				bo = new int[] { 0, 78, 182, 255 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_junglee(mInAllocation, mOutAllocations);
				break;
			case GHOSTLY:
				bi = new int[] { 0, 74, 128, 182, 255 };
				bo = new int[] { 0, 89, 128, 165, 255 };
				blue = new Splines(bi, bo);
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_ghostly(mInAllocation, mOutAllocations);
				break;
			case BGR:
				bi = new int[] { 0, 70, 128, 182, 255 };
				bo = new int[] { 0, 91, 128, 165, 255 };
				blue = new Splines(bi, bo);
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_bgr(mInAllocation, mOutAllocations);
				break;
			case CHILLUM:
				ri = new int[] { 0, 15, 32, 65, 83, 109, 127, 147, 170, 195, 215, 234, 255 };
				ro = new int[] { 26, 43, 64, 114, 148, 177, 193, 202, 208, 218, 229, 241, 251 };
				gi = new int[] { 0, 26, 49, 72, 89, 115, 147, 160, 177, 189, 215, 234, 255 };
				go = new int[] { 26, 26, 72, 123, 147, 188, 205, 210, 222, 224, 235, 246, 255 };
				bi = new int[] { 0, 1, 30, 57, 74, 87, 108, 130, 152, 172, 187, 215, 239, 255 };
				bo = new int[] { 29, 29, 72, 124, 147, 162, 175, 184, 189, 195, 203, 216, 237, 247 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_chillum(mInAllocation, mOutAllocations);
				break;
			case HDR:
				temp = HikePhotosUtils.createBitmap(mBitmapIn, 0, 0, 0, 0, true, false, false, true,Config.ARGB_8888);
				if (temp != null)
				{
					mBlendAllocation = Allocation.createFromBitmap(mRS, temp);
					mScript.set_input2(mBlendAllocation);
					mScript.forEach_filter_HDR_init(mInAllocation, mBlendAllocation);
					mScriptBlur.setRadius(25f);
					mScriptBlur.setInput(mBlendAllocation);
					mScriptBlur.forEach(mOutAllocations);
					mScriptBlur.setInput(mInAllocation);
					mScriptBlur.forEach(mBlendAllocation);
					mScript.set_input1(mBlendAllocation);
					mScript.set_input2(mOutAllocations);
					mScript.forEach_filter_HDR_post(mInAllocation, mOutAllocations);
				}
				else
				{
					error = true;
				}
				break;
			case SUNLITT:
				ri = new int[] { 0, 114, 255 };
				ro = new int[] { 0, 147, 255 };
				gi = new int[] { 0, 78, 178, 255 };
				go = new int[] { 0, 48, 207, 255 };
				bi = new int[] { 0, 145, 255 };
				bo = new int[] { 0, 110, 255 };
				red = new Splines(ri, ro);
				green = new Splines(gi, go);
				blue = new Splines(bi, bo);
				ci = new int[] { 0, 53, 180, 255 };
				co = new int[] { 0, 55, 197, 255 };
				composite = new Splines(ci, co);
				mScript.set_compositeSpline(composite.getInterpolationMatrix());
				mScript.set_rSpline(red.getInterpolationMatrix());
				mScript.set_gSpline(green.getInterpolationMatrix());
				mScript.set_bSpline(blue.getInterpolationMatrix());
				mScript.forEach_filter_sunlitt(mInAllocation, mOutAllocations);
				break;
		
			case TIRANGAA:
				mScript.set_r(new int[] { 0xFF, 0xFF, 0x14 });
				mScript.set_g(new int[] { 0x78, 0xFF, 0xC6 });
				mScript.set_b(new int[] { 0x00, 0xFF, 0x3F });
				mScript.forEach_filter_tirangaa(mInAllocation, mOutAllocations);
				break;
				
			default:
				mScript.forEach_filter_colorMatrix(mInAllocation, mOutAllocations);
				break;

			}

			if (!error)
			{
				mOutAllocations.copyTo(blurImage ? inBitmapOut : currentOut);
				HikePhotosUtils.manageBitmaps(temp);
			}

		}

		float[] getPreScriptEffects()
		{
			ColorMatrix matrix = getColorMatrixforFilter(this.effect, true, HikeConstants.HikePhotos.DEFAULT_FILTER_APPLY_PERCENTAGE);
			if (matrix != null)
				return matrix.getArray();
			else
				return null;
		}

		float[] getPostScriptEffects()
		{
			ColorMatrix matrix = getColorMatrixforFilter(this.effect, false, HikeConstants.HikePhotos.DEFAULT_FILTER_APPLY_PERCENTAGE);
			if (matrix != null)
				return matrix.getArray();
			else
				return null;

		}

	}

	public interface OnFilterAppliedListener
	{
		void onFilterApplied(Bitmap preview);
	}

}

/**
 * 
 * Class implements Curve Fitting Image Processing Technique
 * 
 * Only Cubic Spline Curves Implemented
 * 
 * @author akhiltripathi
 * 
 */
class Splines
{
	private int maxLength;

	private double slopes[];

	private int interpolationOutput[];

	private double inputArray[];

	private double outputArray[];

	public Splines(int inputs[], int outputs[])
	{
		// TODO Auto-generated constructor stub
		this.maxLength = 256;
		this.inputArray = new double[inputs.length];
		this.outputArray = new double[inputs.length];
		this.interpolationOutput = new int[maxLength];
		this.slopes = new double[inputs.length];

		for (int i = 0; i < inputs.length; i++)
			this.inputArray[i] = inputs[i] + 0.0;
		for (int i = 0; i < outputs.length; i++)
			this.outputArray[i] = outputs[i] + 0.0;
		init();
	}

	public static double splineEval(double x, double x0, double x1, double y0, double y1, double s0, double s1)
	{
		double h = x1 - x0;
		double t = (x - x0) / h;
		double u = 1 - t;

		return u * u * (y0 * (2 * t + 1) + s0 * h * t) + t * t * (y1 * (3 - 2 * t) - s1 * h * u);
	}

	public static void computeSplineSlopes(int n, double x[], double y[], double s[])
	{

		double h[] = new double[n];
		double hinv[] = new double[n];
		double g[] = new double[n];
		double a[] = new double[n + 1];
		double b[] = new double[n + 1];
		double fac;

		for (int i = 0; i < n; i++)
		{
			h[i] = x[i + 1] - x[i];
			hinv[i] = 1.0 / h[i];
			g[i] = 3 * (y[i + 1] - y[i]) * hinv[i] * hinv[i];
		}
		a[0] = 2 * hinv[0];
		b[0] = g[0];
		for (int i = 1; i <= n; i++)
		{
			fac = hinv[i - 1] / a[i - 1];
			a[i] = (2 - fac) * hinv[i - 1];
			b[i] = g[i - 1] - fac * b[i - 1];
			if (i < n)
			{
				a[i] += 2 * hinv[i];
				b[i] += g[i];
			}
		}
		s[n] = b[n] / a[n];
		for (int i = n - 1; i >= 0; i--)
			s[i] = (b[i] - hinv[i] * s[i + 1]) / a[i];
	}

	public int[] getInterpolationMatrix()
	{
		return this.interpolationOutput;
	}

	private void init()
	{
		int ix = 0, iy = 0;
		computeSplineSlopes(inputArray.length - 1, inputArray, outputArray, slopes);
		int seg;
		for (seg = 0; seg < inputArray.length - 1; seg++)
		{
			int nDivs = (int) (inputArray[seg + 1] - inputArray[seg]);
			for (int i = 0; i < nDivs; i++)
			{
				double rx = inputArray[seg] + i;
				double ry = splineEval(rx, inputArray[seg], inputArray[seg + 1], outputArray[seg], outputArray[seg + 1], slopes[seg], slopes[seg + 1]);
				iy = (int) (ry);
				ix = (int) (rx);
				if (iy > 255)
					iy = 255;
				if (iy < 0)
					iy = 0;
				interpolationOutput[ix] = iy;
			}
		}
		interpolationOutput[255] = (int) this.outputArray[this.outputArray.length - 1];

	}

}
