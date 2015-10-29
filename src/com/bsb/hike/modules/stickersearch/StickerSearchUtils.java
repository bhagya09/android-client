package com.bsb.hike.modules.stickersearch;

import android.content.Context;
import android.util.Pair;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

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
	 * @return a pair of boolean and sticker list where boolean represents whether first sticker in original list is available or not. if boolean is true it return list containing
	 *         available stickers only and original sticker list if boolean is false
	 */
	public static Pair<Boolean, List<Sticker>> shouldShowStickerFtue(List<Sticker> stickerList)
	{
		Sticker sticker = stickerList.get(0);
		if (!sticker.getStickerCurrentAvailability())
		{
			return new Pair<Boolean, List<Sticker>>(false, stickerList);
		}

		return new Pair<Boolean, List<Sticker>>(true, getAvailableStickerList(stickerList));
	}

	private static List<Sticker> getAvailableStickerList(List<Sticker> stickerList)
	{
		int length = stickerList.size();

		List<Sticker> resultList = new ArrayList<Sticker>(length);

		for (int i = 0; i < length; i++)
		{
			Sticker sticker = stickerList.get(i);
			if (sticker.isStickerAvailable())
			{
				resultList.add(sticker);
			}
		}

		return resultList;
	}

    /***
     * @return current keyboard language in ISO 639-2/T format
     */
    public static String getCurrentLanguage() {
        try {

            InputMethodManager inputMethodManager = (InputMethodManager) HikeMessengerApp.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);

            InputMethodSubtype inputMethodSubtype = inputMethodManager.getCurrentInputMethodSubtype();

            Locale currentLocale = new Locale(inputMethodSubtype.getLocale());

            Logger.d(TAG, currentLocale.toString());

            return currentLocale.getISO3Language();

        } catch (Exception e) {
            Logger.e(TAG, "exception in getting current language : ", e);
        }

        return StickerSearchConstants.DEFAULT_KEYBOARD_LANGUAGE;
    }

}