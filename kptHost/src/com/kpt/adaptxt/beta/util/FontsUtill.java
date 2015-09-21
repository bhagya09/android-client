package com.kpt.adaptxt.beta.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class FontsUtill {
	private static final int WIDTH_PX = 200;
	private static final int HEIGHT_PX = 80;
	private static Resources mResources;

	public static boolean isSupported(Context context, String text) {

		String t1 = text.substring(0,1);
		String t2 = text.substring(1,2);

		mResources = context.getResources();
		
		Bitmap orig = generateBitmap(t1);
		Bitmap bitmap = generateBitmap(t2);

		boolean res = !orig.sameAs(bitmap);

		orig.recycle();
		bitmap.recycle();
		return res;

	}
	
	private static Bitmap generateBitmap(String text){
		int w = WIDTH_PX, h = HEIGHT_PX;
		
		float scale = mResources.getDisplayMetrics().density;
		Bitmap.Config conf = Bitmap.Config.ARGB_8888;
		
		Bitmap orig = Bitmap.createBitmap(w, h, conf);

			Canvas canvas = new Canvas(orig);
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setColor(Color.rgb(0, 0, 0));
			paint.setTextSize((int) (14 * scale));

			// draw text to the Canvas center
			Rect bounds = new Rect();
			paint.getTextBounds(text, 0, text.length(), bounds);
			int x = (orig.getWidth() - bounds.width()) / 2;
			int y = (orig.getHeight() + bounds.height()) / 2;

			canvas.drawText(text, x, y, paint);
			
		return orig;	
	}
}
