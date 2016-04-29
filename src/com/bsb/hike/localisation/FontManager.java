package com.bsb.hike.localisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.bsb.hike.HikeMessengerApp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class FontManager {

	private final Context context;
	
	private static volatile FontManager _instance = null;
	
	private ArrayList<String> mSupportedLanguageList;
	
	private ArrayList<String> mUnsupportedLanguageList;
	
	private FontManager(Context ctx)
	{
		context = ctx;
		mSupportedLanguageList = new ArrayList<String>();
		mUnsupportedLanguageList = new ArrayList<String>();
		mLanguageFonts = new HashMap<String, String>();
		{
			  mLanguageFonts.put("en-US", "ab");
		      mLanguageFonts.put("hi-IN", "अआ");
		      mLanguageFonts.put("bn-IN", "অআ");
		      mLanguageFonts.put("gu-IN", "અઆ");
		      mLanguageFonts.put("mr-IN", "अआ");
		      mLanguageFonts.put("ta-IN", "அஆ");
		      mLanguageFonts.put("te-IN", "అఆ");
		      mLanguageFonts.put("kn-IN", "ಅಆ");
		      mLanguageFonts.put("ml-IN", "അആ");
		}
		
		Iterator<Entry<String, String>> languageIterator = mLanguageFonts.entrySet().iterator();
	    while (languageIterator.hasNext()) 
	    {
	        Map.Entry<String, String> pair = (Entry<String, String>)languageIterator.next();
	        String font = pair.getValue().toString();
	        
	        if(isSupported(font))
	        	mSupportedLanguageList.add(pair.getKey());
	        else
	        	mUnsupportedLanguageList.add(pair.getKey());
	        
	        languageIterator.remove(); // avoids a ConcurrentModificationException
	    }
	}
	
	public static FontManager getInstance()
	{
		if(_instance == null)
		{
			synchronized (FontManager.class) {
				if(_instance == null) {
					_instance = new FontManager(HikeMessengerApp.getInstance().getApplicationContext());
				}
			}
		}
		return _instance;
	}
	
	private static HashMap<String, String> mLanguageFonts;
		
	private boolean isSupported(String text) 
	{
    	
        String char1 = text.substring(0, 1);
        text = text.substring(1, 2);
        Bitmap bitmap1 = generateBitmap(context, char1);
        Bitmap bitmap2 = generateBitmap(context, text);
        boolean res = !bitmap1.sameAs(bitmap2);
        bitmap1.recycle();
        bitmap2.recycle();
        return res;
	}

	private static Bitmap generateBitmap(Context context, String text) {
		Resources mResources = context.getResources();
	    float var1 = mResources.getDisplayMetrics().density;
	    Config conf = Config.ARGB_8888;
	    Bitmap bitmap = Bitmap.createBitmap(200, 80, conf);
	    Canvas canvas = new Canvas(bitmap);
	    Paint paint;
	    (paint = new Paint(1)).setColor(Color.rgb(0, 0, 0));
	    paint.setTextSize((float)((int)(14.0F * var1)));
	    Rect bounds = new Rect();
	    paint.getTextBounds(text, 0, text.length(), bounds);
	    int x = (bitmap.getWidth() - bounds.width()) / 2;
	    int y = (bitmap.getHeight() + bounds.height()) / 2;
	    canvas.drawText(text, (float)x, (float)y, paint);
	    return bitmap;
	}
	
	public ArrayList<String> getSupportedLanguageList()
	{
	    return mSupportedLanguageList;
	}
	
	public ArrayList<String> getUnsupportedLanguageList()
	{
	    return mUnsupportedLanguageList;
	}
}
