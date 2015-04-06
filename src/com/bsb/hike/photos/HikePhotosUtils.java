package com.bsb.hike.photos;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterType;
import com.bsb.hike.photos.views.DoodleEffectItemLinearLayout;
import com.bsb.hike.photos.views.FilterEffectItemLinearLayout;

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
	 * @param context
	 *            : Context of the application dps : Value in DP
	 * 
	 * @return value in pixel
	 */
	public static int dpToPx(Context context, int dps)
	{
		final float scale = context.getResources().getDisplayMetrics().density;
		int pixels = (int) (dps * scale + 0.5f);

		return pixels;
	}
	
	
	/**
	 * Funtcion to create Bitmap. Handles out of Memory Exception
	 * 
	 * @author akhiltripathi
	 */

	public static Bitmap createBitmap(Bitmap source, int x, int y, int targetWidth, int targetHeight, boolean createMutableCopy, boolean scaledCopy, boolean crop, boolean retry)
	{
		Bitmap ret = null;

		try
		{
			if (source != null)
			{
				if (scaledCopy && createMutableCopy)
				{
					ret = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
				}
				else if (crop )
				{
					ret = Bitmap.createBitmap(source, x, y, targetWidth, targetHeight);
				}
				else if (createMutableCopy)
				{
					ret = source.copy(source.getConfig(), true);
				}
				else
				{
					ret = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
				}

			}
			else
			{
				ret = Bitmap.createBitmap(targetWidth, targetHeight, Config.ARGB_8888);
			}

		}
		catch (OutOfMemoryError e)
		{
			if (retry)
			{
				System.gc();
				createBitmap(source, x, y, targetWidth, targetHeight, createMutableCopy, scaledCopy, crop, false);
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
			BRIGHTNESS, CONTRAST, SATURATION, HUE, SEPIA, GRAYSCALE, POLAROID, FADED, BGR, INVERSION, X_PRO_2, WILLOW, WALDEN, VALENCIA, TOASTER, SUTRO, SIERRA, RISE, NASHVILLE, MAYFAIR, LO_FI, KELVIN, INKWELL, HUDSON, HEFE, EARLYBIRD, BRANNAN, AMARO, E1977, SOLOMON, CLASSIC, RETRO, APOLLO, ORIGINAL
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
					effectfilters.addFilter("KALA PILA", FilterType.SOLOMON);
					effectfilters.addFilter("CHUSKI", FilterType.CLASSIC);
					effectfilters.addFilter("JUGAAD", FilterType.NASHVILLE);
					effectfilters.addFilter("JALEBI", FilterType.KELVIN);
					effectfilters.addFilter("X-PRO", FilterType.X_PRO_2);
					effectfilters.addFilter("RETRO", FilterType.RETRO);
					effectfilters.addFilter("APOLLO", FilterType.APOLLO);
					effectfilters.addFilter("EARLYBIRD", FilterType.EARLYBIRD);
					effectfilters.addFilter("SHOLAY", FilterType.E1977);
					effectfilters.addFilter("BRANNAN", FilterType.BRANNAN);
					effectfilters.addFilter("LO-FI", FilterType.LO_FI);
					effectfilters.addFilter("INKWELL", FilterType.INKWELL);
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

}
