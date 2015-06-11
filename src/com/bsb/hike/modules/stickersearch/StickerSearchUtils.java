package com.bsb.hike.modules.stickersearch;

import android.content.Context;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.StickerManager;

public class StickerSearchUtils
{
	//http://stackoverflow.com/questions/11601139/determining-which-word-is-clicked-in-an-android-textview
	public static int getOffsetForPosition(TextView textView, float x, float y) {
	    if (textView.getLayout() == null) {
	        return -1;
	    }
	    final int line = getLineAtCoordinate(textView, y);
	    final int offset = getOffsetAtCoordinate(textView, line, x);
	    return offset;
	}

	private static int getOffsetAtCoordinate(TextView textView2, int line, float x) {
	    x = convertToLocalHorizontalCoordinate(textView2, x);
	    return textView2.getLayout().getOffsetForHorizontal(line, x);
	}

	private static float convertToLocalHorizontalCoordinate(TextView textView2, float x) {
	    x -= textView2.getTotalPaddingLeft();
	    // Clamp the position to inside of the view.
	    x = Math.max(0.0f, x);
	    x = Math.min(textView2.getWidth() - textView2.getTotalPaddingRight() - 1, x);
	    x += textView2.getScrollX();
	    return x;
	}

	private static int getLineAtCoordinate(TextView textView2, float y) {
	    y -= textView2.getTotalPaddingTop();
	    // Clamp the position to inside of the view.
	    y = Math.max(0.0f, y);
	    y = Math.min(textView2.getHeight() - textView2.getTotalPaddingBottom() - 1, y);
	    y += textView2.getScrollY();
	    return textView2.getLayout().getLineForVertical((int) y);
	}
	
	public static  int getStickerSize()
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
}
