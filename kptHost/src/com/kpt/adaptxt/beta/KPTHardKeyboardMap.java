/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


package com.kpt.adaptxt.beta;

import java.util.HashMap;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.Xml;

/**
 * Class to handle the Hard keyboard keymapping for a given script.
 * @author smadabusi
 *
 */
public class KPTHardKeyboardMap {
	
	/**
	 * App component context
	 */
	private Context mContext;
	
    // Keyboard XML Tags
    private static final String TAG_KEYMAP = "Keymap";
    private static final String TAG_KEY = "Key";
   
	private static final String LOG_TAG = "HardKeyboardMap";
    
    /**
     * Current language scripts's HKB keymap
     */
    private HashMap<Integer, String> mKeymap;
    
    /**
     * Current language locale for which keymap hash table is created and stored.
     */
    private String mCurrentLanguageLocale;
    
    /**
     * Creates a HardKeyboardMap from the HKB xml key layout file. 
     * @param context the application or service context
     */
    public KPTHardKeyboardMap(Context context) {
    	mKeymap = new HashMap<Integer, String>();
    	mContext = context;
    }
    
    /**
     * Sets the current locale and loads corresponding HKB keymap resources.
     * @param currScript
     */
    public void setCurrentLocale(String currLocale) {
    	// Don't load keymap if it already exists.
    	if(currLocale != null &&
    			!(mCurrentLanguageLocale != null && currLocale.compareToIgnoreCase(mCurrentLanguageLocale) == 0 )) {
    		
    		mCurrentLanguageLocale = currLocale;
    		loadKeymap(mContext, mContext.getResources().getXml(R.xml.hardkeyboard), currLocale);
    	}
    }
    
    /**
     * Loads the keymap corresponding to specific SIP locale.
     * @param context
     * @param parser
     * @param script
     */
    private void loadKeymap(Context context, XmlResourceParser parser, String locale) {
    	mKeymap.clear();
    	Resources res = context.getResources();
    	boolean isCurrentKeymap = false; 
    	try {
            int event;
            while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    String tag = parser.getName();
                    if (TAG_KEYMAP.equals(tag)) {
                        
                        TypedArray keymapArray = res.obtainAttributes(Xml.asAttributeSet(parser),
                                R.styleable.Kpt_HardKeyboardMap_Keymap);
                    	
                        String localeStr = keymapArray.getText(
                                R.styleable.Kpt_HardKeyboardMap_Keymap_kpt_locale).toString();
                        
                        if(locale.compareToIgnoreCase(localeStr) == 0) {
                        	isCurrentKeymap = true;
                        }
                        keymapArray.recycle();
                        
                   } else if (TAG_KEY.equals(tag)) {
                        if(isCurrentKeymap) {
                        	// Read and add to the  Keymap hash
                        	TypedArray keyArray = res.obtainAttributes(Xml.asAttributeSet(parser),
                                    R.styleable.Kpt_HardKeyboardMap_Key);
                        	
                        	int keyCode = keyArray.getInteger(R.styleable.Kpt_HardKeyboardMap_Key_kpt_code, 0);
                        	String accentedCharString = keyArray.getString(R.styleable.Kpt_HardKeyboardMap_Key_kpt_values);
                        	mKeymap.put(keyCode, accentedCharString);
                        	keyArray.recycle();
                        }
                    } else {
                        // Do Nothing
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                	String tag = parser.getName();
                    if (TAG_KEYMAP.equals(tag) && isCurrentKeymap == true) {
                    	// Break out of the loop once the corresponding script is read.
                    	break;
                    }
                }
            }
        } catch (Exception e) {
            //KPTLog.e(LOG_TAG, "Parse error:" + e);
            e.printStackTrace();
        }
    }
    
    /**
     * Gets the accented character corresponding to the given unicode key code value.
     * @param keyCode
     * @return
     */
    public  String getAccentedCharString(int keyCode) {
    	if(mKeymap == null) {
    		return null;
    	} else {
    		return mKeymap.get(keyCode);
    	}
    }

}
