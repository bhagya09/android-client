package com.bsb.hike.photos;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.photos.views.DoodleEffectItemLinearLayout;
import com.bsb.hike.photos.views.FilterEffectItemLinearLayout;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

//public static int[] BasicMenuIcons={R.drawable.effects_effect,R.drawable.effects_color,R.drawable.effects_frame,R.drawable.effects_text,R.drawable.effects_options};

/**
 * Utility class for picture editing.
 * 
 * @author akhiltripathi
 * 
 */

public class HikePhotosUtils
{

	// enum for features provided in the photo editer view
	public class MenuType
	{
		public static final int EFFECTS_TYPE = 0;

		public static final int DOODLE_TYPE = 1;

		public static final int BORDER_TYPE = 2;

		public static final int TEXT_TYPE = 3;

		public static final int QUALITY_TYPE = 4;
	}
	
	// array cpntaining colors hex codes for colors provided in doodling
	public static int[] DoodleColors = { 0xffff6d00, 0xff1014e2, 0xff86d71d,

	0xff18e883, 0xfff31717, 0xfff7d514, 0xff7418f0,

	0xff16efc4, 0xffffffff, 0xff2ab0fc };

	/**
	 * 
	 * Util method which converts the dp value into float(pixel value) based on the given context resources
	 * 
	 * @return value in pixel
	 */
	public static int dpToPx(int dps)
	{
		final float scale = HikeMessengerApp.getInstance().getApplicationContext().getResources().getDisplayMetrics().density;
		int pixels = (int) (dps * scale + 0.5f);

		return pixels;
	}

	/**
	 * This method converts device specific pixels to density independent pixels.
	 * http://stackoverflow.com/questions/4605527/converting-pixels-to-dp
	 * 
	 * @param px A value in px (pixels) unit. Which we need to convert into db
	 * @return A float value to represent dp equivalent to px value
	 */
	public static float pxToDp(float px){
	    Resources resources = HikeMessengerApp.getInstance().getApplicationContext().getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float dp = px / (metrics.densityDpi / 160f);
	    return dp;
	}
	
	public static void manageBitmaps(Bitmap bitmap)
	{

		if (bitmap != null)
		{
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB && !bitmap.isRecycled())
			{
				bitmap.recycle();
			}
			bitmap = null;
		}
	}

	public static int getBitmapArea(Bitmap bitmap)
	{
		if (bitmap == null)
		{
			return 0;
		}

		if (bitmap.getWidth() < 0 || bitmap.getHeight() < 0)
		{
			return 0;
		}

		return bitmap.getWidth() * bitmap.getHeight();
	}

	public static Bitmap compressBitamp(Bitmap bitmap, int maxWidth, int maxHeight, boolean centerIN,Config configType)
	{
		if(bitmap == null)
		{
			return null;
		}
		
		Bitmap temp = bitmap;
		int width = 0, height = 0;
		float aspectRatio = bitmap.getWidth() * 1.0f / bitmap.getHeight();

		if (centerIN)
		{
			if (bitmap.getWidth() >= bitmap.getHeight())
			{

				width = maxWidth;
				height = (int) (maxWidth / aspectRatio);
			}
			else
			{
				height = maxHeight;
				width = (int) (maxHeight * aspectRatio);
			}
		}
		else
		{
			if (bitmap.getWidth() < bitmap.getHeight())
			{

				width = maxWidth;
				height = (int) (maxWidth / aspectRatio);
			}
			else
			{
				height = maxHeight;
				width = (int) (maxHeight * aspectRatio);
			}
		}

		bitmap = createBitmap(bitmap, 0, 0, width, height, true, true, false, true,configType);
		HikePhotosUtils.manageBitmaps(temp);
		if (!centerIN)
		{
			bitmap = createBitmap(bitmap, (width - maxWidth) / 2, (height - maxHeight) / 2, maxWidth, maxHeight, true, false, true, true,configType);
		}
		return bitmap;
	}

	/**
	 * Funtcion to create Bitmap. Handles out of Memory Exception
	 * 
	 * @author akhiltripathi
	 */

	public static Bitmap createBitmap(Bitmap source, int x, int y, int targetWidth, int targetHeight, boolean createMutableCopy, boolean scaledCopy, boolean crop, boolean retry,Config config)
	{
		Bitmap ret = null;
		
		try
		{
			if (source != null)
			{
				Config outConfig = (source.getConfig() == null) ? config : source.getConfig();
				
				if (scaledCopy && createMutableCopy)
				{
					ret = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
				}
				else if (crop)
				{
					ret = Bitmap.createBitmap(source, x, y, targetWidth, targetHeight);
				}
				else if (createMutableCopy)
				{
					ret = source.copy(outConfig, true);
				}
				else
				{
					ret = Bitmap.createBitmap(source.getWidth(), source.getHeight(), outConfig);
				}

			}
			else
			{
				ret = Bitmap.createBitmap(targetWidth, targetHeight, config);
			}

		}
		catch (OutOfMemoryError e)
		{
			if (retry)
			{
				System.gc();
				createBitmap(source, x, y, targetWidth, targetHeight, createMutableCopy, scaledCopy, crop, false,config);
			}
			else
			{
				ret = null;
			}
		}

		return ret;
	}

	/**
	 * Utility class for Filters
	 * 
	 */
	public static class FilterTools
	{

		private static FilterType selectedFilter;

		private static FilterEffectItemLinearLayout prevFilter;

		private static int selectedColor;

		private static DoodleEffectItemLinearLayout prevColor;

		public static void setCurrentDoodleItem(DoodleEffectItemLinearLayout item)
		{
			prevColor = item;
		}

		public static DoodleEffectItemLinearLayout getCurrentDoodleItem()
		{
			return prevColor;
		}

		public static int getSelectedColor()
		{
			return selectedColor;
		}

		public static void setSelectedColor(int color)
		{
			selectedColor = color;
		}

		public static void setCurrentFilterItem(FilterEffectItemLinearLayout item)
		{
			prevFilter = item;
		}

		public static FilterEffectItemLinearLayout getCurrentFilterItem()
		{
			return prevFilter;
		}

		public static FilterType getSelectedFilter()
		{
			return selectedFilter;
		}

		public static void setSelectedFilter(FilterType type)
		{
			selectedFilter = type;
		}

		public enum FilterType
		{
			BRIGHTNESS, CONTRAST, SATURATION, HUE, SEPIA, GRAYSCALE, POLAROID, FADED, BGR, INVERSION, X_PRO_2, RANGEELA, WILLOW, WALDEN, VALENCIA, TOASTER, SUTRO, SIERRA, RISE, NASHVILLE, MAYFAIR, LO_FI, KELVIN, INKWELL, HUDSON, HEFE, EARLYBIRD, BRANNAN, AMARO, E1977, SOLOMON, CLASSIC, RETRO, APOLLO, ORIGINAL, JALEBI, GHOSTLY, GULAAL, AUTO, JUNGLEE, CHILLUM, HDR, SOFTINK, SUNLITT, TIRANGAA
		}

		public static class FilterList
		{
			public List<String> names = new ArrayList<String>();

			public List<FilterType> filters = new ArrayList<FilterType>();

			private static FilterList effectfilters, qualityfilters;

			public void addFilter(final String name, final FilterType filter)
			{
				names.add(name);
				filters.add(filter);
			}

			/**
			 * @return returns list having complex filters obtained from applying sequence of quality filterson the image
			 */
			public static FilterList getHikeEffects()
			{
				if (effectfilters == null)
				{
					effectfilters = new FilterList();
					effectfilters.addFilter("ORIGINAL", FilterType.ORIGINAL);
					
					if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SPECIAL_DAY_TRIGGER, false))
					{
						effectfilters.addFilter("TIRANGAA", FilterType.TIRANGAA);
					}
					effectfilters.addFilter("MELLOW", FilterType.SOLOMON);
					effectfilters.addFilter("CHUSKI", FilterType.CLASSIC);
					effectfilters.addFilter("AZURE", FilterType.NASHVILLE);
					effectfilters.addFilter("JALEBI", FilterType.JALEBI);
					effectfilters.addFilter("GULAAL", FilterType.GULAAL);
					effectfilters.addFilter("X-PRO", FilterType.X_PRO_2);
					effectfilters.addFilter("HDR", FilterType.HDR);
					effectfilters.addFilter("APOLLO", FilterType.APOLLO);
					effectfilters.addFilter("RETRO", FilterType.RETRO);
					effectfilters.addFilter("PULSAR", FilterType.EARLYBIRD);
					effectfilters.addFilter("SUNLITT", FilterType.SUNLITT);
					effectfilters.addFilter("HAZEL", FilterType.BRANNAN);
					effectfilters.addFilter("LO-FI", FilterType.LO_FI);
					effectfilters.addFilter("INKED", FilterType.INKWELL);
					effectfilters.addFilter("MASHAAL", FilterType.KELVIN);
					effectfilters.addFilter("SHOLAY", FilterType.E1977);
					effectfilters.addFilter("JUNGLEE", FilterType.JUNGLEE);
					effectfilters.addFilter("POLAROID", FilterType.POLAROID);
					effectfilters.addFilter("SEPIA", FilterType.SEPIA);
					effectfilters.addFilter("GRAYSCALE", FilterType.GRAYSCALE);
				}
				
				return effectfilters;

			}

			/**
			 * @return Filters that help in enhancing the image quality
			 */
			public static FilterList getQualityFilters()
			{
				if (qualityfilters == null)
				{
					qualityfilters = new FilterList();

				}
				return qualityfilters;

			}
		}

	}

	/**
	 * Utility class for Borders/Frames
	 * 
	 */

	public static class BorderTools
	{

		public static Bitmap ApplyBorderToBitmap(Bitmap source, Drawable border)
		{
			Bitmap topImage = null;
			Bitmap b = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
			Canvas comboImage = new Canvas(b);

			int width = source.getWidth();
			int height = source.getHeight();

			border.setBounds(0, 0, height, width);
			Bitmap tpImg = ((BitmapDrawable) border).getBitmap();
			topImage = Bitmap.createScaledBitmap(tpImg, width, height, true);
			source = Bitmap.createScaledBitmap(source, (int) (width - width * 0.24), (int) (height - height * 0.32), true);

			comboImage.drawBitmap(source, 0.12f * width, 0.176f * height, null);

			comboImage.drawBitmap(topImage, 0f, 0f, null);

			return b;

		}

		public static class BorderList
		{
			public List<String> names = new LinkedList<String>();

			public List<Integer> borders = new LinkedList<Integer>();

			private static BorderList list;

			public void addBorder(final String name, final int id)
			{
				names.add(name);
				borders.add(new Integer(id));
			}

			public static BorderList getBorders()
			{
				if (list == null)

				{
					list = new BorderList();
					list.addBorder("Hearts", R.drawable.a);

				}
				return list;
			}
		}

	}
	
	public static int getServerConfigDimenForDP()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.DP_IMAGE_SIZE, HikeConstants.HikePhotos.MAX_IMAGE_DIMEN);
	}
	
	public static ColorMatrixColorFilter getGreenDownShiftFilter()
	{
		float[] colorTransform = {
	            1, 0, 0, 0, 0,
	            0, 1, 0, 0, -5f,
	            0, 0, 1, 0, 0,
	            0, 0, 0, 1, 0 
	            };

	    ColorMatrix colorMatrix = new ColorMatrix();
	    colorMatrix.setSaturation(0f); //Remove Colour 
	    colorMatrix.set(colorTransform); //Apply the Red

	    ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
	    return colorFilter;
	}

}
