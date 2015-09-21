/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.kpt.adaptxt.beta.keyboard;

import java.util.Arrays;
import java.util.StringTokenizer;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.kpt.adaptxt.beta.KPTKeyboardSwitcher;
import com.kpt.adaptxt.beta.Keyboard;
import com.kpt.adaptxt.beta.Keyboard.Row;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.util.KPTConstants;

/**
 * Class for describing the position and characteristics of a single key in the keyboard.
 */
public class Key implements Comparable<Key> {

	public static final int KEY_TYPE_NORMAL = 0;
	/**
	 * this is used for Glide functionality. If the key is this type then it is considered for glide
	 * ie.. this key properties are sent to core
	 */
	public static final int KEY_TYPE_FUNCTIONAL_SEND_CORE = 1;
	/**
	 * this is used for glide functionality. If the key is this type then this key attributes 
	 * are not sent to core
	 */
	public static final int KEY_TYPE_FUNCTIONAL_DONT_SEND_CORE = 2;
	
	/** 
	 * All the key codes (unicode or custom code) that this key could generate, zero'th 
	 * being the most important.
	 */
	public int[] codes;

	/**
	 * This is blank key dont draw key background and text for this key.We need a blank space in minikeyboard
	 */
	public boolean isBlankKey= false;
	/** Label to display */
	public CharSequence label;

	public CharSequence label2;

	/** Icon to display instead of a label. Icon takes precedence over a label */
	public Drawable icon;
	/** Preview version of the icon, for the preview popup */
	public Drawable iconPreview;
	/** Width of the key, not including the gap */
	public int width;
	/** Height of the key, not including the gap */
	public int height;
	/** The horizontal gap before this key */
	public int gap;
	/** Whether this key is sticky, i.e., a toggle key */
	public boolean sticky;
	/** X coordinate of the key in the keyboard layout */
	public int x;
	/** Y coordinate of the key in the keyboard layout */
	public int y;
	/** Hit bounding box of the key */
	public final Rect mHitBox = new Rect();

	/** The current pressed state of this key */
	//public boolean pressed;
	/** If this is a sticky key, is it on? */
	public boolean on;
	/** Text to output when pressed. This can be multiple characters, like ".com" */
	public CharSequence text;
	/** Popup characters */
	public CharSequence popupCharacters;

	/** 
	 * Flags that specify the anchoring to edges of the keyboard for detecting touch events
	 * that are just out of the boundary of the key. This is a bit mask of 
	 * {@link Keyboard#EDGE_LEFT}, {@link Keyboard#EDGE_RIGHT}, {@link Keyboard#EDGE_TOP} and
	 * {@link Keyboard#EDGE_BOTTOM}.
	 */
	public int edgeFlags;
	/** Whether this is a modifier key, such as Shift or Alt */
	public boolean modifier;
	/** The keyboard that this key belongs to */
	private Keyboard keyboard;
	/** 
	 * If this key pops up a mini keyboard, this is the resource id for the XML layout for that
	 * keyboard.
	 */
	public int popupResId;
	
	/**
	 *  Whether this key repeats itself when held down 
	 */
	public boolean repeatable;

	/**
	 * The row number in which the character is present.
	 * Index starts from zero.
	 */
	public int rowNumber;

	/**
	 * Index of key in a row.
	 * Starts from zero
	 */
	public int keyIndexInRow;

	/**
     * what is the special operation of this key when combined with adaptxt keys.
     */
    public int specialOperation;
    
    /**
     *  this indicates the final position of this key in the keyboard
     */
    public int actualKeyPosition;
    
    /**
     * indicated whether the key uses external font.
     * if true, then a separate font(adp.ttf in assets) is used to draw the text
     */
    public boolean useExternalFont;
    /**
     * the external font text that should be drawn on the key. 
     * first check if useExternalFont is set true for this key
     */
    public CharSequence iconText;
    
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
     * what type of key is this ?
     */
    public int type;
    
    /**
    * number of consonant in hindhi alphabet.
    * this numbering start with "क" as 0 and increments one by one.
    */
    public int syllableIndex;
    
    /**
     * unicode of the vowel in the conjunction character of hindhi
     * Suppose for the character की as "ी" is the vowel conjuncted in the character
     * we will get the unicode for "ी".
     */
     public int vowelUnicode;
     
     /**
      * String all the conjucted characters of hindhi with that particalar vowel.
      */
      public String[] vowelBase;

	private final static int[] KEY_STATE_NORMAL_ON = { 
		android.R.attr.state_checkable, 
		android.R.attr.state_checked
	};

	private final static int[] KEY_STATE_PRESSED_ON = { 
		android.R.attr.state_pressed, 
		android.R.attr.state_checkable, 
		android.R.attr.state_checked 
	};

	private final static int[] KEY_STATE_NORMAL_OFF = { 
		android.R.attr.state_checkable 
	};

	private final static int[] KEY_STATE_PRESSED_OFF = { 
		android.R.attr.state_pressed, 
		android.R.attr.state_checkable 
	};

	private final static int[] KEY_STATE_NORMAL = {
	};

	private final static int[] KEY_STATE_PRESSED = {
		android.R.attr.state_pressed
	};

	private int mHashCode;

	/** The current pressed state of this key */
	public boolean mPressed;
	/** Key is enabled and responds on press */
	private boolean mEnabled = true;

	private boolean mShiftLockEnabled = false;



	/** Create an empty key with no attributes. */
	public Key(Row parent, int rowNumber, int keyIndexInRow ) {
		keyboard = parent.parent;
		this.rowNumber = rowNumber;
		this.keyIndexInRow = keyIndexInRow;
		mHashCode = computeHashCode(this);
	}


	/** Create a key with the given top-left coordinate and extract its attributes from
	 * the XML parser.
	 * @param res resources associated with the caller's context
	 * @param parent the row that this key belongs to. The row must already be attached to
	 * a {@link Keyboard}.
	 * @param x the x coordinate of the top-left
	 * @param y the y coordinate of the top-left
	 * @param parser the XML parser containing the attributes for this key
	 */
	public Key(Resources res, Row parent, int x, int y, XmlResourceParser parser, int rowNumber, int keyIndexInRow) {
		this(parent, rowNumber, keyIndexInRow);

		this.x = x;
		this.y = y;

		TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), 
				R.styleable.Kpt_Keyboard);

		width = Keyboard.getDimensionOrFraction(a, 
				R.styleable.Kpt_Keyboard_kpt_keyWidth,
				keyboard.mDisplayWidth, parent.defaultWidth);
		height = Keyboard.getDimensionOrFraction(a, 
				R.styleable.Kpt_Keyboard_kpt_keyHeight,
				keyboard.mDisplayHeight, parent.defaultHeight);
		gap = Keyboard.getDimensionOrFraction(a, 
				R.styleable.Kpt_Keyboard_kpt_horizontalGap,
				keyboard.mDisplayWidth, parent.defaultHorizontalGap);
		
		keyLabelXPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabelXPos, parent.keyLabelXPos);
        keyLabelYPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabelYPos, parent.keyLabelYPos);
        keyLabel2XPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabel2XPos, parent.keyLabel2XPos);
        keyLabel2YPos = a.getFloat(R.styleable.Kpt_Keyboard_kpt_keyLabel2YPos, parent.keyLabel2YPos);

        keyLabelTextSize = a.getDimensionPixelSize(R.styleable.Kpt_Keyboard_kpt_keyLabelTextSize, 
        								parent.keyLabelTextSize);
        keyLabel2TextSize = a.getDimensionPixelSize(R.styleable.Kpt_Keyboard_kpt_keyLabel2TextSize, 
        								parent.keyLabel2TextSize);   
        
        type = a.getInt(R.styleable.Kpt_Keyboard_kpt_keyType, parent.keyType);
        
		a.recycle();
		a = res.obtainAttributes(Xml.asAttributeSet(parser),
				R.styleable.Kpt_Keyboard_Key);
		this.x += gap;
		TypedValue codesValue = new TypedValue();
		a.getValue(R.styleable.Kpt_Keyboard_Key_kpt_codes, 
				codesValue);
		if (codesValue.type == TypedValue.TYPE_INT_DEC 
				|| codesValue.type == TypedValue.TYPE_INT_HEX) {
			codes = new int[] { codesValue.data };
		} else if (codesValue.type == TypedValue.TYPE_STRING) {
			codes = parseCSV(codesValue.string.toString());
		}
		try{
        	syllableIndex = a.getInt(R.styleable.Kpt_Keyboard_Key_kpt_syllable_index,-1);
            vowelUnicode = a.getInt(R.styleable.Kpt_Keyboard_Key_kpt_vowel_unicode,-1);
            vowelBase = a.getText(R.styleable.Kpt_Keyboard_Key_kpt_vowel_base).toString().split(",");
        }catch(Exception e){
        	//Log.e("KPT", "Exception "+e.toString());
        	syllableIndex = -1;
        	vowelUnicode = -1;
        	vowelBase = null;
        }
        
		specialOperation = a.getInt(R.styleable.Kpt_Keyboard_Key_kpt_keySpecialOper, 0);

		iconPreview = a.getDrawable(R.styleable.Kpt_Keyboard_Key_kpt_iconPreview);
		if (iconPreview != null) {
			iconPreview.setBounds(0, 0, iconPreview.getIntrinsicWidth(), 
					iconPreview.getIntrinsicHeight());
		}
		popupCharacters = a.getText(
				R.styleable.Kpt_Keyboard_Key_kpt_popupCharacters);
		popupResId = a.getResourceId(
				R.styleable.Kpt_Keyboard_Key_kpt_popupKeyboard, 0);
		repeatable = a.getBoolean(
				R.styleable.Kpt_Keyboard_Key_kpt_isRepeatable, false);
		modifier = a.getBoolean(
				R.styleable.Kpt_Keyboard_Key_kpt_isModifier, false);
		sticky = a.getBoolean(
				R.styleable.Kpt_Keyboard_Key_kpt_isSticky, false);
		edgeFlags = a.getInt(R.styleable.Kpt_Keyboard_Key_kpt_keyEdgeFlags, 0);
		edgeFlags |= parent.rowEdgeFlags;

		icon = a.getDrawable(
				R.styleable.Kpt_Keyboard_Key_kpt_keyIcon);
		if (icon != null) {
			icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
		}
		label = a.getText(R.styleable.Kpt_Keyboard_Key_kpt_keyLabel);

		if(KPTKeyboardSwitcher.mIsSymbolschanging && label != null && label.length() > 0
				&& codes!=null && codes[0] != KPTConstants.KEYCODE_SPACE) {
			String[] pageSymbols = KPTKeyboardSwitcher.mPageSymbols;
			if(pageSymbols != null && pageSymbols.length > Keyboard.mKeyCounter) {
				String[] symNCode= pageSymbols[Keyboard.mKeyCounter].split(" ");
				label = symNCode[0];
				int[] temp = new int[1];
				if(!label.equals("-1")) {
					temp[0] = Integer.parseInt(symNCode[1]);
				} else {
					label = " ";
					temp[0] = 0;
				}
				codes = temp;
			}
			Keyboard.mKeyCounter ++;
		}

		useExternalFont = a.getBoolean(
                R.styleable.Kpt_Keyboard_Key_kpt_useExternalFont, false);
        iconText = a.getText(
                R.styleable.Kpt_Keyboard_Key_kpt_iconText);
        
		/*** For Secondary key text ***/
		TypedArray a2 = res.obtainAttributes(Xml.asAttributeSet(parser), 
				R.styleable.Kpt_Atx_Keyboard_Key);
		label2 = a2.getText(R.styleable.Kpt_Atx_Keyboard_Key_kpt_keyLabel2);
		/*******************************/

		text = a.getText(R.styleable.Kpt_Keyboard_Key_kpt_keyOutputText);

		if (codes == null && !TextUtils.isEmpty(label)) {
			codes = new int[] { label.charAt(0) };
		}
		mHitBox.set(x, y, x + width + 1, y + height);
		a.recycle();
		mHashCode = computeHashCode(this);
	}

	public void enableShiftLock() {
		mShiftLockEnabled  = true;
	}

	public boolean getEnableShiftLock() {
		return mShiftLockEnabled; 
	}

	private int[] parseCSV(final String value) {
		int count = 0;
		int lastIndex = 0;
		if (value.length() > 0) {
			count++;
			while ((lastIndex = value.indexOf(",", lastIndex + 1)) > 0) {
				count++;
			}
		}
		int[] values = new int[count];
		count = 0;
		StringTokenizer st = new StringTokenizer(value, ",");
		while (st.hasMoreTokens()) {
			try {
				values[count++] = Integer.parseInt(st.nextToken());
			} catch (NumberFormatException nfe) {
				//KPTLog.e(TAG, "Error parsing keycodes " + value);
			}
		}
		return values;
	}

	/**
	 * Returns the drawable state for the key, based on the current state and type of the key.
	 * @param mKeyboardSwitcher 
	 * @return the drawable state of the key.
	 * @see android.graphics.drawable.StateListDrawable#setState(int[])
	 */
	public int[] getCurrentDrawableState(boolean isCurrentLayoutQwerty) {
        int[] states = KEY_STATE_NORMAL;
        
        if (on) {
            if (mPressed) {
                states = KEY_STATE_PRESSED_ON;
            } else {
                states = KEY_STATE_NORMAL_ON;
            }
        } else {
            if (sticky) {
                if (mPressed) {
                    states = KEY_STATE_PRESSED_OFF;
                } else {
                    states = KEY_STATE_NORMAL_OFF;
                }
            } else {
                if (mPressed) {
                    states = KEY_STATE_PRESSED;
                }
            }
        }
        
        // For Delete and ALT key alone use a different key background
        // as Del and Alt have brown background.
        // Hence a workaround. We use Android's state_active for Del and Alt alone. 
        
        // Fix for TP-8329.AS  Mic key has to pick the  normal key.
        
        if(codes[0] == KPTConstants.KEYCODE_DELETE || 
        		codes[0] == KPTConstants.KEYCODE_ALT ||
        		codes[0] == KPTConstants.KEYCODE_ENTER || 
        		codes[0] == KPTConstants.KEYCODE_MODE_CHANGE ||
        		codes[0] == KPTConstants.KEYCODE_JUMP_TO_TERTIARY ||
        		codes[0] == KPTConstants.KEYCODE_XI || 
        		codes[0] == KPTConstants.KEYCODE_SHIFT || 
        		codes[0] == KPTConstants.KEYCODE_PHONE_MODE_SYM ||
        		codes[0] == KPTConstants.KEYCODE_PHONE_LANGUAGE_AND_SHARE ||
        		codes[0] == KPTConstants.KEYCODE_ATX ||
        		codes[0] == KPTConstants.KEYCODE_SHIFT ||
        		codes[0] == KPTConstants.KEYCODE_THAI_SHIFT || //TP 13379
        		codes[0] == KPTConstants.KEYCODE_LOOKUP	) {
        	int[] finalStates = new int[states.length + 1];
            System.arraycopy(states, 0, finalStates, 0, states.length);
            finalStates[states.length] = android.R.attr.state_active;
            states = finalStates;
        }
        return states;
    }

	private static int computeHashCode(final Key key) {
		return Arrays.hashCode(new Object[] {
				key.x,
				key.y,
				key.width,
				key.height,
				/*key.codes[0],
				key.label,
				key.label2,
				key.popupResId,*/
		});
	}

	private boolean equalsInternal(final Key o) {
		if (this == o) return true;
		return o.x == this.x
				&& o.y == this.y
				&& o.width == this.width
				&& o.height == this.height;
		/*&& o.codes[0] == this.codes[0]
						&& TextUtils.equals(o.label, this.label)
						&& TextUtils.equals(o.label2, this.label2)
						&& o.popupResId == this.popupResId;*/

	}

	@Override
	public int compareTo(Key o) {
		if (equalsInternal(o)) return 0;
		if (mHashCode > o.mHashCode) return 1;
		return -1;
	}

	@Override
	public int hashCode() {
		return mHashCode;
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof Key && equalsInternal((Key)o);
	}

	public final boolean isShift() {
		return (codes[0] == KPTConstants.KEYCODE_SHIFT || codes[0] == KPTConstants.KEYCODE_SHIFTINDIC);
	}

	public final boolean isModifierKey() {
		return isShift();
	}

	public final boolean isRepeatable() {
		return repeatable;
	}

	/**
	 * Informs the key that it has been pressed, in case it needs to change its appearance or
	 * state.
	 * @see #onReleased()
	 */
	public void onPressed() {
		mPressed = true;
	}

	/**
	 * Informs the key that it has been released, in case it needs to change its appearance or
	 * state.
	 * @see #onPressed()
	 */
	public void onReleased() {
		mPressed = false;
		if (sticky) {
			on = !on;
		}
	}

	public final boolean isEnabled() {
		return mEnabled;
	}

	public void setEnabled(final boolean enabled) {
		mEnabled = enabled;
	}

	/**
	 * Detects if a point falls on this key.
	 * @param x the x-coordinate of the point
	 * @param y the y-coordinate of the point
	 * @return whether or not the point falls on the key. If the key is attached to an edge, it
	 * will assume that all points between the key and the edge are considered to be on the key.
	 * @see #markAsLeftEdge(KeyboardParams) etc.
	 */
	public boolean isOnKey(final int x, final int y) {
		
		 boolean leftEdge = (edgeFlags & Keyboard.EDGE_LEFT) > 0;
         boolean rightEdge = (edgeFlags & Keyboard.EDGE_RIGHT) > 0;
         boolean topEdge = (edgeFlags & Keyboard.EDGE_TOP) > 0;
         boolean bottomEdge = (edgeFlags & Keyboard.EDGE_BOTTOM) > 0;
         if ((x >= this.x || (leftEdge && x <= this.x + this.width)) 
                 && (x < this.x + this.width || (rightEdge && x >= this.x)) 
                 && (y >= this.y || (topEdge && y <= this.y + this.height))
                 && (y < this.y + this.height || (bottomEdge && y >= this.y))) {
             return true;
         } else {
             return false;
         }
         
		//return mHitBox.contains(x, y);
	}

	/**
	 * Returns the square of the distance to the nearest edge of the key and the given point.
	 * @param x the x-coordinate of the point
	 * @param y the y-coordinate of the point
	 * @return the square of the distance of the point from the nearest edge of the key
	 */
	public int squaredDistanceToEdge(final int x, final int y) {
		final int left = this.x;
		final int right = left + width;
		final int top = this.y;
		final int bottom = top + height;
		final int edgeX = x < left ? left : (x > right ? right : x);
		final int edgeY = y < top ? top : (y > bottom ? bottom : y);
		final int dx = x - edgeX;
		final int dy = y - edgeY;
		return dx * dx + dy * dy;
	}
}
