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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.kpt.adaptxt.beta.keyboard.Key;
import com.kpt.adaptxt.beta.keyboard.PointerTracker;
import com.kpt.adaptxt.beta.keyboard.ProximityInfo;
import com.kpt.adaptxt.beta.util.KPTConstants;
/**
 * Loads an XML description of a keyboard and stores the attributes of the keys. A keyboard
 * consists of rows of keys.
 * <p>The layout file for a keyboard contains XML that looks like the following snippet:</p>
 * <pre>
 * &lt;Keyboard
 *         android:keyWidth="%10p"
 *         android:keyHeight="50px"
 *         android:horizontalGap="2px"
 *         android:verticalGap="2px" &gt;
 *     &lt;Row android:keyWidth="32px" &gt;
 *         &lt;Key android:keyLabel="A" /&gt;
 *         ...
 *     &lt;/Row&gt;
 *     ...
 * &lt;/Keyboard&gt;
 * </pre>
 * @attr ref android.R.styleable#Keyboard_keyWidth
 * @attr ref android.R.styleable#Keyboard_keyHeight
 * @attr ref android.R.styleable#Keyboard_horizontalGap
 * @attr ref android.R.styleable#Keyboard_verticalGap
 */
public class Keyboard {

    static final String TAG = "Keyboard";
    private static final String TAG_KEYBOARD = "Keyboard";
    private static final String TAG_ROW = "Row";
    private static final String TAG_KEY = "Key";

    public static final int EDGE_LEFT = 0x01;
    public static final int EDGE_RIGHT = 0x02;
    public static final int EDGE_TOP = 0x04;
    public static final int EDGE_BOTTOM = 0x08;
    
    private ProximityInfo mProximityInfo;
    
    protected boolean mIsSpaceHighlighted;

    /** Horizontal gap default for all rows */
    private int mDefaultHorizontalGap;
    
    /** Default key width */
    private int mDefaultWidth;

    /** Default key height */
    private int mDefaultHeight;

    /** Default gap between rows */
    private int mDefaultVerticalGap;

    /** Is the keyboard in the shifted state */
    private static boolean mShifted;
    
    /** Key instance for the shift key, if present */
    public Key mShiftKey;
    
    /** Key index for the shift key, if present */
    private int mShiftKeyIndex = -1;
    
    /** Key index for the atx key, if present */
    private int mAtxKeyIndex = -1;
    
    /** Total height of the keyboard, including the padding and keys */
    private int mTotalHeight;
    
    public static int mKeyCounter = 0;
    
    /** 
     * Total width of the keyboard, including left side gaps and keys, but not any gaps on the
     * right side.
     */
    private int mTotalWidth;
    
    /** List of keys in this keyboard */
    public List<Key> mKeys;
    
    /** List of rows in this keyboard */
	private List<Row> mRows;
	
    /** List of modifier keys such as Shift & Alt, if any */
    private List<Key> mModifierKeys;
    
    /** Width of the screen available to fit the keyboard */
    public int mDisplayWidth;

    /** Height of the screen */
    public int mDisplayHeight;

    /** Keyboard mode, or zero, if none.  */
    private int mKeyboardMode;

	public static final int KEYCODE_REPLACE = -22;
	
    private int mProximityThreshold;
    /** Number of key widths from current touch point to search for nearest keys. */
    private static float SEARCH_DISTANCE = 1.8f;
    
    private Key mSpaceKey;
    
    private int mSpaceKeyIndex = -1;
    /**
     * Number of rows in this keyboard.
     */
    int mNumberofRows;
    
    private int keyType;
    
    /**
     * Is keyboard displayed on popup window
     */
    public boolean isPopupKeyboard;
    
    private SharedPreferences mSharedPref;
    /**
     * x position of the primary text of the key
     */
    public float keyLabelXPos;
    /**
     * y position of the primary text of the key
     */
    public float keyLabelYPos;
    /**
     * x position of the secondary text of the key
     */
    public float keyLabel2XPos;
    /**
     * y position of the secondary text of the key
     */
    public float keyLabel2YPos;
    
    /**
     * text size of the primary key
     */
    public int keyLabelTextSize;
    
    /**
     * text size of the secondary key
     */
    public int keyLabel2TextSize;

    /**
     * indicates weather this language is RTL language or not
     */
    private boolean mIsRTL;
    /**
     * indicates weather this language supports font style feature in keyboard customization
     */
    private boolean mIsFontStyleSupported;
    /**
     * indicates weather this language supported font size feature in keyboard customization
     */
    private boolean mIsFontSizeSupported;
    
    private boolean mIsGlideSupportedForThisKeyboard;
    /**
     * Container for keys in the keyboard. All keys in a row are at the same Y-coordinate. 
     * Some of the key size defaults can be overridden per row from what the {@link Keyboard}
     * defines. 
     * @attr ref android.R.styleable#Keyboard_keyWidth
     * @attr ref android.R.styleable#Keyboard_keyHeight
     * @attr ref android.R.styleable#Keyboard_horizontalGap
     * @attr ref android.R.styleable#Keyboard_verticalGap
     * @attr ref android.R.styleable#Keyboard_Row_rowEdgeFlags
     * @attr ref android.R.styleable#Keyboard_Row_keyboardMode
     */
    public static class Row {
        /** Default width of a key in this row. */
        public int defaultWidth;
        /** Default height of a key in this row. */
        public int defaultHeight;
        /** Default horizontal gap between keys in this row. */
        public int defaultHorizontalGap;
        /** Vertical gap following this row. */
        public int verticalGap;
        /**
         * Edge flags for this row of keys. Possible values that can be assigned are
         * {@link Keyboard#EDGE_TOP EDGE_TOP} and {@link Keyboard#EDGE_BOTTOM EDGE_BOTTOM}  
         */
        public int rowEdgeFlags;
        
        /** The keyboard mode for this row */
        public int mode;
        
        /** number of keys in a row*/
        public int numberOfKeysInRow=0;

        public Keyboard parent;

        /**
         * x position of the primary text of the key
         */
        public float keyLabelXPos;
        /**
         * y position of the primary text of the key
         */
        public float keyLabelYPos;
        /**
         * x position of the secondary text of the key
         */
        public float keyLabel2XPos;
        /**
         * y position of the secondary text of the key
         */
        public float keyLabel2YPos;
        
        /**
         * text size of the primary key
         */
        public int keyLabelTextSize;
        
        /**
         * text size of the secondary key
         */
        public int keyLabel2TextSize;
        
        public int keyType;
		private boolean mIsNumberRow;
        
        public Row(Keyboard parent) {
            this.parent = parent;
        }
        
        public Row(Resources res, Keyboard parent, XmlResourceParser parser) {
            this.parent = parent;
            TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), 
                    R.styleable.Kpt_Keyboard);
            defaultWidth = getDimensionOrFraction(a, 
                    R.styleable.Kpt_Keyboard_kpt_keyWidth, 
                    parent.mDisplayWidth, parent.mDefaultWidth);
            defaultHeight = getDimensionOrFraction(a, 
                    R.styleable.Kpt_Keyboard_kpt_keyHeight, 
                    parent.mDisplayHeight, parent.mDefaultHeight);
            defaultHorizontalGap = getDimensionOrFraction(a,
                    R.styleable.Kpt_Keyboard_kpt_horizontalGap, 
                    parent.mDisplayWidth, parent.mDefaultHorizontalGap);
            verticalGap = getDimensionOrFraction(a, 
                    R.styleable.Kpt_Keyboard_kpt_verticalGap, 
                    parent.mDisplayHeight, parent.mDefaultVerticalGap);
            a.recycle();
            a = res.obtainAttributes(Xml.asAttributeSet(parser),
                    R.styleable.Kpt_Keyboard_Row);
            rowEdgeFlags = a.getInt(R.styleable.Kpt_Keyboard_Row_kpt_rowEdgeFlags, 0);
            mode = a.getResourceId(R.styleable.Kpt_Keyboard_Row_kpt_keyboardMode,
                    0);
            mIsNumberRow = a.getBoolean(R.styleable.Kpt_Keyboard_Row_kpt_isNumberRow, false);
            keyLabelXPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabelXPos, parent.keyLabelXPos);
            keyLabelYPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabelYPos, parent.keyLabelYPos);
            keyLabel2XPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabel2XPos, parent.keyLabel2XPos);
            keyLabel2YPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabel2YPos, parent.keyLabel2YPos);
            
            keyLabelTextSize = a.getDimensionPixelSize(R.styleable.Kpt_Keyboard_kpt_keyLabelTextSize, 
					parent.keyLabelTextSize);
            keyLabel2TextSize = a.getDimensionPixelSize(R.styleable.Kpt_Keyboard_kpt_keyLabel2TextSize, 
            		parent.keyLabel2TextSize);
            
            keyType = a.getInt(R.styleable.Kpt_Keyboard_kpt_keyType, parent.keyType);
        }
    }
    
    public int getSingleKeyWidth(){
    	if (mKeys != null && mKeys.size()>0) {
			Key key = mKeys.get(0);
			if (key != null) {
				return key.width;
			}
		}
    	return -1;
    }
    
    public int getSingleKeyHeight(){
    	if (mKeys != null && mKeys.size() > 0) {
			Key key = mKeys.get(0);
			if (key != null) {
				return key.height;
			}
		}
    	return -1;
    }
    
    public Keyboard() {
    	//this is a empty constructor
    }
    
    /**
     * Creates a keyboard from the given xml key layout file.
     * @param context the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     */
    public Keyboard(Context context, int xmlLayoutResId, boolean isPopupKeyboard, int closeButtonWidth, boolean fromActivity) {
        this(context, xmlLayoutResId, 0, isPopupKeyboard, closeButtonWidth, fromActivity);
    }
    
    /**
     * Creates a keyboard from the given xml key layout file. Weeds out rows
     * that have a keyboard mode defined but don't match the specified mode. 
     * @param context the application or service context
     * @param xmlLayoutResId the resource file that contains the keyboard layout and keys.
     * @param modeId keyboard mode identifier
     * @param fromActivity this is called to diaply keyboard from a activity, display only qwerrty mode in this case
     */
    public Keyboard(Context context, int xmlLayoutResId, int modeId,  boolean isPopupKeyboard, int closeButtonWidth, boolean fromActivity) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mDisplayWidth = dm.widthPixels;
        mDisplayHeight = dm.heightPixels;
        //Log.v(TAG, "keyboard's display metrics:" + dm);

        mDefaultHorizontalGap = 0;
        mDefaultWidth = mDisplayWidth / 10;
        mDefaultVerticalGap = 0;
        mDefaultHeight = mDefaultWidth;
        mKeys = new ArrayList<Key>();
        mRows = new ArrayList<Row>();
        mModifierKeys = new ArrayList<Key>();
        mKeyboardMode = modeId;
        this.isPopupKeyboard = isPopupKeyboard;
        
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String keyboardLayout = mSharedPref.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
				KPTConstants.KEYBOARD_TYPE_QWERTY);
		/*if(context.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE){
			keyboardLayout = mSharedPref.getString(KPTConstants.PREF_LAND_KEYBOARD_TYPE,
					KPTConstants.KEYBOARD_TYPE_QWERTY);	
		}else{
			keyboardLayout = mSharedPref.getString(KPTConstants.PREF_PORT_KEYBOARD_TYPE,
					KPTConstants.KEYBOARD_TYPE_QWERTY);	
		}*/
		
		//if the call isfrom activity display on qwerty keypad, even if 12 key is enabled
		//this change is done for keyboard customization
		boolean isKeyboardQwerty = fromActivity || keyboardLayout.equals(KPTConstants.KEYBOARD_TYPE_QWERTY);
		try{
			loadKeyboard(context, context.getResources().getXml(xmlLayoutResId), isKeyboardQwerty);
		}catch (Exception e) {
			e.printStackTrace();
		}
		        
        final int grid_width = context.getResources().getInteger(R.integer.kpt_config_keyboard_grid_width);
        final int grid_height = context.getResources().getInteger(R.integer.kpt_config_keyboard_grid_height);
        
        if(isPopupKeyboard || mProximityInfo == null) {
        	mProximityInfo = new ProximityInfo(grid_width, grid_height, mTotalWidth, mTotalHeight,
        			getSingleKeyHeight(), getSingleKeyHeight(), mKeys.toArray(new Key[mKeys.size()]));
        }
    }
    
    /**
     * if the keys are swapped, update the nearest key logic based on the new keys
     */
    public void updateNearaestKeys(final Key[] keys) {
    	if(mProximityInfo != null) {
    		mProximityInfo.updateComputeNearestNeighbors(keys);
    	}
    }
    
    /**
     * <p>Creates a blank keyboard from the given resource file and populates it with the specified
     * characters in left-to-right, top-to-bottom fashion, using the specified number of columns.
     * </p>
     * <p>If the specified number of columns is -1, then the keyboard will fit as many keys as
     * possible in each row.</p>
     * @param context the application or service context
     * @param layoutTemplateResId the layout template file, containing no keys.
     * @param characters the list of characters to display on the keyboard. One key will be created
     * for each character.
     * @param columns the number of columns of keys to display. If this number is greater than the 
     * number of keys that can fit in a row, it will be ignored. If this number is -1, the 
     * keyboard will fit as many keys as possible in each row.
     */
    public Keyboard(Context context, int layoutTemplateResId, 
            CharSequence characters, int columns, int horizontalPadding,  boolean isPopupKeyboard, int closeButtonWidth, boolean fromActivity) {
        this(context, layoutTemplateResId, isPopupKeyboard, closeButtonWidth, fromActivity);
        int x = 0;
        int y = 0;
        int column = 0;
        mTotalWidth = 0;
        int numberofRows = 1;
        int tempHeight = 0;
        
        Row row = new Row(this);
        row.defaultHeight = mDefaultHeight;
        row.defaultWidth = mDefaultWidth;
        row.defaultHorizontalGap = mDefaultHorizontalGap;
        row.verticalGap = mDefaultVerticalGap;
        row.rowEdgeFlags = EDGE_TOP | EDGE_BOTTOM;
        final int maxColumns = columns == -1 ? Integer.MAX_VALUE: columns;
        Row parentRow = null;
        mKeys = new ArrayList<Key>();
		if(mSharedPref == null){
        	mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		}
        tempHeight = mDefaultHeight;
        
        int customHeight;
        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        	customHeight = mSharedPref.getInt(KPTConstants.PREF_CUSTOM_BASE_VAL, 
        			(int) context.getResources().getDimension(R.dimen.kpt_key_height_dk_fk));
        } else {
        	customHeight = mSharedPref.getInt(KPTConstants.PREF_CUSTOM_BASE_VAL_LANDSCAPE, 
        			(int) context.getResources().getDimension(R.dimen.kpt_key_height_land));
        }
        if(mDefaultHeight != customHeight){
    		tempHeight = customHeight;	
    	}
        
        for (int i = 0,rIndex = 0,rKeyIndex = 0; characters != null && i < characters.length(); i++,rKeyIndex++) {
            char c = characters.charAt(i);
            if(parentRow == null){
            	parentRow = row;
            }
            // While determining mTotalWidth for keyboard the close button width must be taken into account
        	// to properly wrap the keys and make sure teh close button is visible.
            if (column >= maxColumns 
                   /* || (x + mDefaultWidth + horizontalPadding) > (mDisplayWidth - mCloseButtonWidth) */ /*||  (i!=0 && i%5==0 && isPopupKeyboard)*/) {
                x = 0;
                y += mDefaultVerticalGap + tempHeight;
                column = 0;
                // TP 6646
                // update the forthcoming accentuated chars in new row so that
                // the arrow key navigation behaves properly in multiple rows scenario
                numberofRows++;
                rIndex++;
                rKeyIndex = 0;
                // creating new row parent for the up-coming keys of new row
                Row nRow = new Row(this);
                nRow.defaultHeight = tempHeight;
                nRow.defaultWidth = mDefaultWidth;
                nRow.defaultHorizontalGap = mDefaultHorizontalGap;
                nRow.verticalGap = mDefaultVerticalGap;
                nRow.rowEdgeFlags = EDGE_TOP | EDGE_BOTTOM;
                parentRow = nRow;
            }
            final Key key = new Key(parentRow, rIndex, rKeyIndex);
            key.x = x;
            key.y = y;
            key.width = mDefaultWidth;
            if(AdaptxtIME.mShowNumbers && numberofRows==1){
            	key.height = mDefaultHeight;
            }
            else{
            	key.height = tempHeight;
            }
           	
            key.gap = mDefaultHorizontalGap;
            key.label = String.valueOf(c);
            key.codes = new int[] { c };
            column++;
            x += key.width + key.gap;
            mKeys.add(key);
            if (x > mTotalWidth) {
                mTotalWidth = x;
            }
        }
        mNumberofRows = numberofRows;
        mTotalHeight = y + tempHeight;
        
    	setCustomKeyHeight(customHeight, -1);
    }
    
    public List<Key> getKeys() {
        return mKeys;
    }
    
    public List<Key> getModifierKeys() {
        return mModifierKeys;
    }
    
    protected int getHorizontalGap() {
        return mDefaultHorizontalGap;
    }
    
    protected void setHorizontalGap(int gap) {
        mDefaultHorizontalGap = gap;
    }

    protected int getVerticalGap() {
        return mDefaultVerticalGap;
    }

    protected void setVerticalGap(int gap) {
        mDefaultVerticalGap = gap;
    }

    protected int getKeyHeight() {
        return mDefaultHeight;
    }

    protected void setKeyHeight(int height) {
        mDefaultHeight = height;
    }

    protected int getKeyWidth() {
        return mDefaultWidth;
    }
    
    protected void setKeyWidth(int width) {
        mDefaultWidth = width;
    }

    public  int getAtxKeyIndex() {
    	return mAtxKeyIndex;
    }
    
    /**
     * Returns the total height of the keyboard
     * @return the total height of the keyboard
     */
    public int getHeight() {
        return mTotalHeight;
    }
    
    public void setHeight(int height) {
    	mTotalHeight = height;
    }
    
    public int getMinWidth() {
        return mTotalWidth;
    }

    public boolean setShifted(boolean shiftState) {
        if (mShiftKey != null) {
            mShiftKey.on = shiftState;
        }
        if (mShifted != shiftState) {
            mShifted = shiftState;
            return true;
        }
        return false;
    }

    public boolean isShifted() {
        return mShifted;
    }

    public int getShiftKeyIndex() {
        return mShiftKeyIndex;
    }
    
    public boolean isFontStyleApplicable() {
    	return mIsFontStyleSupported;
    }
    
    public boolean isFontSizeApplicable() {
    	return mIsFontSizeSupported;
    }
    
    public boolean isGlideSupportedForCurrentKeyboard() {
    	return mIsGlideSupportedForThisKeyboard;
    }
    
    public boolean isRTLLanguage() {
    	return mIsRTL;
    }
    
    /**
     * @return returns the ATX key.
     */
    public Key getAtxKey() { //Bug 7284
    	if (mAtxKeyIndex != -1 && getKeys().size() > mAtxKeyIndex) {
    		return getKeys().get(mAtxKeyIndex);
    	}
    	return null;
    }

    public Key getSpaceKey() {
    	return mSpaceKey;
    }
    
    /**
     * return the space key index in the keyboard layout
     * @return
     */
    public int getSpacekeyIndex() {
    	//this is just a safety check, if in any case index is -1
    	if(mSpaceKeyIndex == -1 && mKeys != null) {
    		for (int i = 0; i < mKeys.size(); i++) {
				if(mKeys.get(i).codes.length > 0 && mKeys.get(i).codes[0] == KPTConstants.KEYCODE_SPACE) {
					return i;
				}
			}
    	}
    	return mSpaceKeyIndex;
    }

    public boolean isSpaceKey(int index) {
    	if(index != -1 && index > 0 && mKeys.size() > index) {
    		Key key = mKeys.get(index);
            return key != null && key.codes[0] == KPTConstants.KEYCODE_SPACE;
    	}
    	return false;
    }
    
    protected Row createRowFromXml(Resources res, XmlResourceParser parser) {
        return new Row(res, this, parser);
    }
    
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, 
            XmlResourceParser parser, int rowNumber, int keyIndexInRow) {
        return new Key(res, parent, x, y, parser, rowNumber, keyIndexInRow);
    }
    public  void loadKeyboard(Context context, XmlResourceParser parser, boolean isFirstPage , int KeysPerPage,int total_keys) {
        boolean inKey = false;
        //boolean leftMostKey = false;
        int x = 0;
        int y = 0;
        Key key = null;
        Resources res = context.getResources();
        Row currentRow =  createRowFromXml(res, parser);
        int keyIndexinRow = 0;
        int counter = 0;
        mKeys = new ArrayList<Key>();
        mRows = new ArrayList<Row>();
        
        
        List<Key> tempKeys = new ArrayList<Key>();
        int tempHeight = mDefaultHeight;
        if(mSharedPref == null) {
        	mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        }

        int customHeight;
        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        	customHeight = mSharedPref.getInt(KPTConstants.PREF_CUSTOM_BASE_VAL, 
        						(int) context.getResources().getDimension(R.dimen.kpt_key_height_dk_fk));
        } else {
        	customHeight = mSharedPref.getInt(KPTConstants.PREF_CUSTOM_BASE_VAL_LANDSCAPE, 
        						(int) context.getResources().getDimension(R.dimen.kpt_key_height_land));
        }
        if(mDefaultHeight != customHeight){
    		tempHeight = customHeight;	
    	}
        
        String leftChevronIconText = res.getString(R.string.kpt_icontext_space_chevron_left);
		String rightChevronIconText =res.getString(R.string.kpt_icontext_space_chevron_right);
		if( (( MainKeyboardView.isRightHalf  && isFirstPage ) || ( !MainKeyboardView.isRightHalf  && !isFirstPage  )) && mNumberofRows>1 )
		{
			key = createKeyFromXml(res, currentRow, x, y, parser, 0, keyIndexinRow);
            //key.x = x;
            //key.y = y;
            key.width = mDefaultWidth;
           	//key.height = tempHeight;
            key.gap = mDefaultHorizontalGap;
            key.label = leftChevronIconText;
            key.codes = new int[] { ( (int)leftChevronIconText.charAt(0)  )};
            x += key.width  + key.gap;
            mKeys.add(key);
            if (x > mTotalWidth) {
                mTotalWidth = x;
            }
		}
		if(total_keys >  (2 * KeysPerPage)) total_keys = (2 * KeysPerPage) ;
        try {
            int event;
            while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    String tag = parser.getName();
                    if (TAG_KEY.equals(tag)) {
                        if ( (counter < KeysPerPage &&  isFirstPage ) || (counter >= (total_keys -KeysPerPage) && counter < total_keys &&  !isFirstPage) )
                        {
                        	inKey = true;
                             
                        	 key = createKeyFromXml(res, currentRow, x, y, parser, 0, keyIndexinRow);
                              if(!MainKeyboardView.isRightHalf) {
                            	  
                            	  mKeys.add(key);
                              }
                              else {
                            	  tempKeys.add(key);
                              }
                              tempHeight = key.height;
                        }
                        
                        counter++ ;
                    }  else if (TAG_KEYBOARD.equals(tag)) {
                        parseKeyboardAttributes(res, parser);
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false;
                        if(!MainKeyboardView.isRightHalf) {
	                      keyIndexinRow++;
	                        x += key.gap + key.width;
	                        if (x > mTotalWidth) {
	                            mTotalWidth = x;
	                        }
                        }
                    } 
                }
            }
            
            setCustomKeyHeight(tempHeight, -1);
            mKeyCounter = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(MainKeyboardView.isRightHalf)
        {
        	
        	for (int i = tempKeys.size() - 1; i >= 0; i--) {
				mKeys.add(tempKeys.get(i));
				keyIndexinRow++;
				mKeys.get(mKeys.size()-1).x = x;
				mKeys.get(mKeys.size()-1).y = y;
                x += tempKeys.get(i).gap + tempKeys.get(i).width;
                
                if (x > mTotalWidth) {
                    mTotalWidth = x;
                }
			}
        }
    	if( (( MainKeyboardView.isRightHalf  && !isFirstPage ) || ( !MainKeyboardView.isRightHalf  && isFirstPage  )) && mNumberofRows>1 )
		{
			key = new Key(currentRow, 0, 0);
            key.x = x;
            key.y = y;
            key.width = mDefaultWidth;
           	key.height = tempHeight;
            key.gap = mDefaultHorizontalGap;
            key.label = rightChevronIconText;
            key.codes = new int[] { ( (int)rightChevronIconText.charAt(0)  )};
            x += key.width  + key.gap;
            mKeys.add(key);
            if (x > mTotalWidth) {
                mTotalWidth = x;
            }
		}
    	mNumberofRows = 1;
    	 y = currentRow.verticalGap;
         y = currentRow.defaultHeight;
         mRows.add(currentRow);
         
     	setCustomKeyHeight(customHeight, -1);
    }
    
    
    
    private void loadHindhKeyboard(Context context, XmlResourceParser parser, boolean isKeyboardQwerty) {
    	
    	
        boolean inKey = false;
        boolean inRow = false;
        //boolean leftMostKey = false;
        int row = 0;
        int x = 0;
        int y = 0;
        Key key = null;
        Row currentRow = null;
        Resources res = context.getResources();
        boolean skipRow = false;
        int keyIndexinRow = 0;
        int counter = 0;
        try {
            int event;
            while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    String tag = parser.getName();
                    if (TAG_ROW.equals(tag)) {
                    	inRow = true;
                    	x = 0;
                    	currentRow = createRowFromXml(res, parser);
                    	// skipRow = currentRow.mode != 0 && currentRow.mode != mKeyboardMode;
                    	if(!isKeyboardQwerty) {
                    		skipRow = currentRow.mode != 0 && 
                    				!KPTKeyboardSwitcher.phoneKeypadModes.contains(currentRow.mode);
                    	} else {
                    		skipRow = currentRow.mode != 0 && currentRow.mode != mKeyboardMode;
                    	}
                    	
                    	//Bug fix for #14627. (Commenting row!=0)
                    	if ((skipRow /*&& row !=0*/)  || (!AdaptxtIME.mShowNumbers && currentRow.mIsNumberRow) ) {
                    		skipToEndOfRow(parser);
                    		inRow = false;
                    	}

                    	keyIndexinRow = 0;
                    } else if (TAG_KEY.equals(tag)) {
                    	
                        inKey = true;
                        key = createKeyFromXml(res, currentRow, x, y, parser, row, keyIndexinRow);
                        key.actualKeyPosition = counter ++;
                        try{
                        	
                            if(key.rowNumber == 0 || (key.rowNumber == 1 &&  key.vowelUnicode !=-1) ) {
                            	
                            	 if(key.vowelBase!=null && key.vowelBase.length !=0 && key.vowelUnicode!=-1){
                 					
                                 	key.label = key.vowelBase[PointerTracker.mCurrentSyllableIndex+1];
                                 	if(PointerTracker.mCurrentSyllableIndex!=-1){
                                 		key.codes[0] = key.vowelUnicode;
                                 	}else{
                                 		key.codes[0] =(int)key.label.charAt(0);
                                 	}
                                 	
                            	 }
                            	 
                            }
                          AdaptxtIME.mHindhiSyllableTable.put(key.label.toString(), key.syllableIndex);
                            
                        }catch(Exception e){
                        	
                        }
                        
                        
                       
                        mKeys.add(key);
                        currentRow.numberOfKeysInRow++;
                        if (key.codes[0] == KPTConstants.KEYCODE_SHIFT ||key.codes[0] == KPTConstants.KEYCODE_SHIFTINDIC ) {
                            mShiftKey = key;
                            mShiftKeyIndex = mKeys.size()-1;
                            mModifierKeys.add(key);
                        } /*else if (key.codes[0] == KEYCODE_ALT) { //Bug fix  6205 -karthik
                            mModifierKeys.add(key);
                        }*/ else if(key.codes[0] == KPTConstants.KEYCODE_ATX) {
							mAtxKeyIndex = mKeys.size() - 1;
						} else if(key.codes[0] == KPTConstants.KEYCODE_SPACE) {
							mSpaceKey = key;
							mSpaceKeyIndex = mKeys.size() - 1;
						}
                    } else if (TAG_KEYBOARD.equals(tag)) {
                        parseKeyboardAttributes(res, parser);
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false;
                        keyIndexinRow++;
                        x += key.gap + key.width;
                        if (x > mTotalWidth) {
                            mTotalWidth = x;
                        }
                    } else if (inRow) {
                        inRow = false;
                        y += currentRow.verticalGap;
                        y += currentRow.defaultHeight;
                        mRows.add(currentRow);
                        row++;
                    } else {
                        // TODO: error or extend?
                    }
                }
            }
            mKeyCounter = 0;
        } catch (Exception e) {
            //KPTLog.e(TAG, "Parse error:" + e);
          //  e.printStackTrace();
        }
        
        if(row == 0) {
        	// Atleast there has to be default one row, even in case of popup keyboard
        	row = 1;
        }
        mNumberofRows = row;
        mTotalHeight = y - mDefaultVerticalGap;
        
        // Other than popup keyboard, for rest all evenly arrange the leading and trailing gaps.
        if(!this.isPopupKeyboard) {
        	autoArrangeLeadingAndTrailingGaps();
        }
        
    }

    private void loadKeyboard(Context context, XmlResourceParser parser, boolean isKeyboardQwerty) {
    	if(AdaptxtIME.mIsHindhi){
    		loadHindhKeyboard(context,parser,isKeyboardQwerty);
    		return;
    	}

        boolean inKey = false;
        boolean inRow = false;
        //boolean leftMostKey = false;
        int row = 0;
        int x = 0;
        int y = 0;
        Key key = null;
        Row currentRow = null;
        Resources res = context.getResources();
        boolean skipRow = false;
        int keyIndexinRow = 0;
        int counter = 0;
        try {
            int event;
            while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    String tag = parser.getName();
                    if (TAG_ROW.equals(tag)) {
                    	inRow = true;
                    	x = 0;
                    	currentRow = createRowFromXml(res, parser);
                    	// skipRow = currentRow.mode != 0 && currentRow.mode != mKeyboardMode;
                    	if(!isKeyboardQwerty) {
                    		skipRow = currentRow.mode != 0 && 
                    				!KPTKeyboardSwitcher.phoneKeypadModes.contains(currentRow.mode);
                    	} else {
                    		skipRow = currentRow.mode != 0 && currentRow.mode != mKeyboardMode;
                    	}
                    	
                    	//Bug fix for #14627. (Commenting row!=0)
                    	if ((skipRow /*&& row !=0*/)  || (!AdaptxtIME.mShowNumbers && currentRow.mIsNumberRow) ) {
                    		skipToEndOfRow(parser);
                    		inRow = false;
                    	}

                    	keyIndexinRow = 0;
                    } else if (TAG_KEY.equals(tag)) {
                        inKey = true;
                        key = createKeyFromXml(res, currentRow, x, y, parser, row, keyIndexinRow);
                        key.actualKeyPosition = counter ++;
                        mKeys.add(key);
                        currentRow.numberOfKeysInRow++;
                        if (key.codes[0] == KPTConstants.KEYCODE_SHIFT ||key.codes[0] == KPTConstants.KEYCODE_SHIFTINDIC ) {
                            mShiftKey = key;
                            mShiftKeyIndex = mKeys.size()-1;
                            mModifierKeys.add(key);
                        } /*else if (key.codes[0] == KEYCODE_ALT) { //Bug fix  6205 -karthik
                            mModifierKeys.add(key);
                        }*/ else if(key.codes[0] == KPTConstants.KEYCODE_ATX) {
							mAtxKeyIndex = mKeys.size() - 1;
						} else if(key.codes[0] == KPTConstants.KEYCODE_SPACE) {
							mSpaceKey = key;
							mSpaceKeyIndex = mKeys.size() - 1;
						}
                    } else if (TAG_KEYBOARD.equals(tag)) {
                        parseKeyboardAttributes(res, parser);
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false;
                        keyIndexinRow++;
                        x += key.gap + key.width;
                        if (x > mTotalWidth) {
                            mTotalWidth = x;
                        }
                    } else if (inRow) {
                        inRow = false;
                        y += currentRow.verticalGap;
                        y += currentRow.defaultHeight;
                        mRows.add(currentRow);
                        row++;
                    } else {
                        // TODO: error or extend?
                    }
                }
            }
            mKeyCounter = 0;
        } catch (Exception e) {
            //KPTLog.e(TAG, "Parse error:" + e);
            e.printStackTrace();
        }
        
        if(row == 0) {
        	// Atleast there has to be default one row, even in case of popup keyboard
        	row = 1;
        }
        mNumberofRows = row;
        mTotalHeight = y - mDefaultVerticalGap;
        
        // Other than popup keyboard, for rest all evenly arrange the leading and trailing gaps.
        if(!this.isPopupKeyboard) {
        	autoArrangeLeadingAndTrailingGaps();
        }
        
    }

    /**
     * update the mkeys with the latest height, also the total keyboard height
     * 
     * @param defHeight
     * @param keyHeightPerc
     */
    public void setCustomKeyHeight(int defHeight, int keyHeightPerc) {
    	//int y = 0;
		final int keyCount = mKeys.size();
		
		for (int i = 0; i < keyCount; i++) {
			if(AdaptxtIME.mShowNumbers){
				if(mKeys.get(i).rowNumber == 0){
					mKeys.get(i).height = mKeys.get(0).height;
					mKeys.get(i).y = 0 ; 
				}else{
					mKeys.get(i).height = defHeight;
					mKeys.get(i).y = mKeys.get(0).height + (defHeight * (mKeys.get(i).rowNumber-1)); 
				}
			}
			else{
				mKeys.get(i).height = defHeight;
				mKeys.get(i).y = defHeight * mKeys.get(i).rowNumber; 
			}
			
		}
		
		if(mKeys != null && mKeys.size()>0){
			//mTotalHeight = mKeys.get(0).height * mNumberofRows + mDefaultVerticalGap;/*y - mDefaultVerticalGap*/;
			mTotalHeight = mKeys.get(0).height + (defHeight * (mNumberofRows-1)) + mDefaultVerticalGap;
		}
		
        final Key[] keys = mKeys.toArray(new Key[mKeys.size()]);
		updateNearaestKeys(keys);
    }
    
    public int getNumberOfRows() {
    	return mNumberofRows;
    }
    
    private void skipToEndOfRow(XmlResourceParser parser) 
            throws XmlPullParserException, IOException {
        int event;
        while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
            if (event == XmlResourceParser.END_TAG 
                    && parser.getName().equals(TAG_ROW)) {
                break;
            }
        }
    }
    
    /**
     * Evenly Splits the leading and trailing gaps in each row of the keyboard
     */
    private void autoArrangeLeadingAndTrailingGaps() {
    	int[] trailingGaps = new int[mNumberofRows];
    	
    	// Calculate th etrailing space for all the keys
    	for(Key currKey : mKeys) {
    		if( (currKey.edgeFlags & EDGE_RIGHT) > 0) {
    			trailingGaps[currKey.rowNumber] = mDisplayWidth - (currKey.x + currKey.width);
    		}
    	}
    	
    	int horizontalOffset = 0;
    	// Adjust the horizontal offset for all the keys
    	for(Key currKey : mKeys) {
    		if( (currKey.edgeFlags & EDGE_LEFT) > 0) {
    			// Leading key in the row
    			horizontalOffset = (currKey.x -  trailingGaps[currKey.rowNumber]) / 2;
    		} 
    		currKey.x -= horizontalOffset;
    	}
    	
    }
    
    private void parseKeyboardAttributes(Resources res, XmlResourceParser parser) {
        TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), 
                R.styleable.Kpt_Keyboard);

        mDefaultWidth = getDimensionOrFraction(a,
                R.styleable.Kpt_Keyboard_kpt_keyWidth,
                mDisplayWidth, mDisplayWidth / 10);
        mDefaultHeight = getDimensionOrFraction(a,
                R.styleable.Kpt_Keyboard_kpt_keyHeight,
                mDisplayHeight, 50);
        mDefaultHorizontalGap = getDimensionOrFraction(a,
                R.styleable.Kpt_Keyboard_kpt_horizontalGap,
                mDisplayWidth, 0);
        mDefaultVerticalGap = getDimensionOrFraction(a,
                R.styleable.Kpt_Keyboard_kpt_verticalGap,
                mDisplayHeight, 0);
        
        keyLabelXPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabelXPos, 0f);
        keyLabelYPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabelYPos, 0f);
        keyLabel2XPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabel2XPos, 0f);
        keyLabel2YPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabel2YPos, 0f);
        
        keyLabelTextSize = a.getDimensionPixelSize(R.styleable.Kpt_Keyboard_kpt_keyLabelTextSize, 
				res.getDimensionPixelSize(R.dimen.kpt_keyLabelTextSize));
        keyLabel2TextSize = a.getDimensionPixelSize(R.styleable.Kpt_Keyboard_kpt_keyLabel2TextSize, 
				res.getDimensionPixelSize(R.dimen.kpt_keyLabel2TextSize)); 
        
        mIsRTL = a.getBoolean(R.styleable.Kpt_Keyboard_kpt_isRTL, false);
        mIsFontStyleSupported = a.getBoolean(R.styleable.Kpt_Keyboard_kpt_fontStyleApplicable, false);
        mIsFontSizeSupported = a.getBoolean(R.styleable.Kpt_Keyboard_kpt_fontSizeApplicable, false);
        
        keyType = a.getInt(R.styleable.Kpt_Keyboard_kpt_keyType, 
        						res.getInteger(R.integer.kpt_key_type_normal));
        
        mIsGlideSupportedForThisKeyboard = a.getBoolean(R.styleable.Kpt_Keyboard_kpt_isGlideSupported, false);
        
        mProximityThreshold = (int) (mDefaultWidth * SEARCH_DISTANCE);
        mProximityThreshold = mProximityThreshold * mProximityThreshold; // Square it for comparison
        a.recycle();
    }
    
    public static int getDimensionOrFraction(TypedArray a, int index, int base, int defValue) {
        TypedValue value = a.peekValue(index);
        if (value == null) {
        	return defValue;
        }
        if (value.type == TypedValue.TYPE_DIMENSION) {
            return a.getDimensionPixelOffset(index, defValue);
        } else if (value.type == TypedValue.TYPE_FRACTION) {
            // Round it to avoid values like 47.9999 from getting truncated
            return Math.round(a.getFraction(index, base, base, defValue));
        }
        return defValue;
    }
    
    public boolean isSpaceHighlighted(){
    	return mIsSpaceHighlighted;
    }
    
    public Row getRowDetails(int index) {
    	if(mRows!=null && index>=0 && index< mRows.size())
    	{
    		return mRows.get(index);
    	}
    	return null;
    }
    
    public float getThreshhold() {
		return (int) (mDefaultWidth * SEARCH_DISTANCE);
	}
    
    /**
     * Returns the array of the keys that are closest to the given point.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return the array of the nearest keys to the given point. If the given
     * point is out of range, then an array of size zero is returned.
     */
    public Key[] getNearestKeys(final int x, final int y) {
        // Avoid dead pixels at edges of the keyboard
        final int adjustedX = Math.max(0, Math.min(x, mTotalWidth - 1));
        final int adjustedY = Math.max(0, Math.min(y, mTotalHeight - 1));
        return mProximityInfo.getNearestKeys(adjustedX, adjustedY);
    }
    
    public String [] getIgonoredLeftKey(Key key, final int mStartX,final int mStartY ){
		return mProximityInfo.getIgonoredLeftKey(key, mStartX, mStartY);
	}
	
	public String [] getIgonoredRightKey(Key key, final int mStartX,final int mStartY){
		return mProximityInfo.getIgonoredRightKey(key, mStartX, mStartY);
	}
	
	public String [] getAllKey(Key key, final int mStartX,final int mStartY){
		return mProximityInfo.getAllKey(key, mStartX, mStartY);
	}
}
