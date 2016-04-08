package com.bsb.hike.modules.stickersearch;

import android.content.Context;
import android.util.Pair;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.provider.db.HikeStickerSearchBaseConstants;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StickerSearchUtils
{
	public static final String TAG = StickerSearchUtils.class.getSimpleName();

	// http://stackoverflow.com/questions/11601139/determining-which-word-is-clicked-in-an-android-textview
	public static int getOffsetForPosition(TextView textView, float x, float y)
	{
		if (textView.getLayout() == null)
		{
			return -1;
		}
		final int line = getLineAtCoordinate(textView, y);
		final int offset = getOffsetAtCoordinate(textView, line, x);
		return offset;
	}

	private static int getOffsetAtCoordinate(TextView textView2, int line, float x)
	{
		x = convertToLocalHorizontalCoordinate(textView2, x);
		return textView2.getLayout().getOffsetForHorizontal(line, x);
	}

	private static float convertToLocalHorizontalCoordinate(TextView textView2, float x)
	{
		x -= textView2.getTotalPaddingLeft();
		// Clamp the position to inside of the view.
		x = Math.max(0.0f, x);
		x = Math.min(textView2.getWidth() - textView2.getTotalPaddingRight() - 1, x);
		x += textView2.getScrollX();
		return x;
	}

	private static int getLineAtCoordinate(TextView textView2, float y)
	{
		y -= textView2.getTotalPaddingTop();
		// Clamp the position to inside of the view.
		y = Math.max(0.0f, y);
		y = Math.min(textView2.getHeight() - textView2.getTotalPaddingBottom() - 1, y);
		y += textView2.getScrollY();
		return textView2.getLayout().getLineForVertical((int) y);
	}

	public static int getStickerSize()
	{
		Context context = HikeMessengerApp.getInstance();
		int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

		int numItemsRow = StickerManager.getInstance().getNumColumnsForStickerGrid(context);

		int stickerPadding = (int) 2 * context.getResources().getDimensionPixelSize(R.dimen.sticker_padding);
		int horizontalSpacing = (int) (numItemsRow - 1) * context.getResources().getDimensionPixelSize(R.dimen.sticker_grid_horizontal_padding);

		int remainingSpace = (screenWidth - horizontalSpacing - stickerPadding) - (numItemsRow * StickerManager.SIZE_IMAGE);

		int sizeEachImage = StickerManager.SIZE_IMAGE + ((int) (remainingSpace / numItemsRow));

		return sizeEachImage;
	}

	/**
	 * 
	 * @param stickerList
	 * @return a pair of boolean and sticker list where boolean represents whether all stickers in original list are available or not. if boolean is true it return list containing
	 *         allowed stickers only and original sticker list if boolean is false
	 */
	public static Pair<Boolean, List<Sticker>> shouldShowStickerFtue(List<Sticker> stickerList, int undownloadedStickersToDisplay)
	{

		boolean result = false;

		if ((undownloadedStickersToDisplay == 0))
		{
			result = stickerList.get(0).isStickerAvailable();
		}
		else
		{
			for (int i = 0; i < stickerList.size(); i++)
			{
				Sticker sticker = stickerList.get(i);
				result = result || sticker.isStickerAvailable();
			}
		}

		if (result)
		{
			return new Pair<Boolean, List<Sticker>>(result, getAllowedStickerList(stickerList, undownloadedStickersToDisplay));
		}
		else
		{
			return new Pair<Boolean, List<Sticker>>(result, stickerList);
		}

	}

	private static List<Sticker> getAllowedStickerList(List<Sticker> stickerList, int allowedUndownloadedLimit)
	{
		int length = stickerList.size(), count = 0;

		List<Sticker> resultList = new ArrayList<Sticker>(length);
		List<Sticker> undownloadedList = new ArrayList<Sticker>();

		for (int i = 0; i < length; i++)
		{
			Sticker sticker = stickerList.get(i);
			if (sticker.isStickerAvailable())
			{
				resultList.add(sticker);
			}
			else if (count < allowedUndownloadedLimit)
			{
				if (sticker.isMiniStickerAvailable())
				{
					resultList.add(sticker);
				}
				else
				{
					undownloadedList.add(sticker);
				}
				count++;
			}
			else
			{
				Logger.i(TAG, "Undownloaded sticker found but not shown : " + sticker.getCategoryId() + " : " + sticker.getStickerId());
			}
		}

		if (undownloadedList.size() > 0)
		{
			resultList.addAll(undownloadedList);
		}

		return resultList;
	}
	/***
	 * @return current keyboard language in ISO 639-2/T format
	 */
	public static String getCurrentLanguageISOCode()
	{

		try
		{
			InputMethodSubtype inputMethodSubtype = ((InputMethodManager) HikeMessengerApp.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE))
					.getCurrentInputMethodSubtype();

			Locale currentLocale = new Locale(inputMethodSubtype.getLocale());

			Logger.d(TAG, "Current language is " + currentLocale.toString());

			return StickerSearchUtils.getISOCodeFromLocale(currentLocale);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in getting current language: ", e);
		}

		return StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE;
	}

	public static int getTagCacheLimit(int tagType)
	{
		HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance();

		switch (tagType)
		{
		case StickerSearchConstants.STATE_FORCED_TAGS_DOWNLOAD:
			return prefs.getData(HikeStickerSearchBaseConstants.KEY_PREF_UNDOWNLOADED_CACHE_LIMIT, StickerSearchConstants.DEFAULT_STICKER_CACHE_LIMIT);
		}

		return StickerSearchConstants.DEFAULT_STICKER_CACHE_LIMIT;
	}

	public static int getUndownloadedTagsStickersCount()
	{
		HikeSharedPreferenceUtil prefs = HikeSharedPreferenceUtil.getInstance();

		return prefs.getData(HikeStickerSearchBaseConstants.KEY_PREF_UNDOWNLOADED_TAG_COUNT, 0);
	}

	public static boolean tagCacheLimitReached(int tagType)
	{
		int cacheLimit = getTagCacheLimit(tagType);

        if (cacheLimit == StickerSearchConstants.DEFAULT_STICKER_CACHE_LIMIT)
		{
			return false;
		}

		return getUndownloadedTagsStickersCount() - cacheLimit > 0;
	}

	public static String getISOCodeFromLocale(Locale locale)
	{
		try
		{
			return locale.getISO3Language();
		}
		catch(Exception e)
		{
			Logger.e(TAG, "exception in get language iso 3 code : ", e);
		}
		return StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE_ISO_CODE;
	}

    /**
     *
     * @return server controlled limit if not connected via Hike Direct and user on wifi
     */
    public static int getUndownloadedStickerToDisplayCount()
    {
        if((Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext()) == 1) && !OfflineController.getInstance().isConnected())
        {
            return HikeSharedPreferenceUtil.getInstance().getData(HikeStickerSearchBaseConstants.KEY_PREF_UNDOWNLOADED_VISIBLE_IN_RECO_COUNT, 0);
        }

        return  0;
    }

}