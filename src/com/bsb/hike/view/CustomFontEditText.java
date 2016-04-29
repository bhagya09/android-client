package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Utils;

public class CustomFontEditText extends EditText
{
	private String fontName;
	
	private CustomTypeFace customTypeFace;
	
	private BackKeyListener listener;

	private void setFont(AttributeSet attrs)
	{
		fontName = attrs.getAttributeValue(HikeConstants.NAMESPACE, HikeConstants.FONT);
		if (Utils.isLollipopOrHigher())
    		setTypeface(getTypeface(), getTypeface().getStyle());
	}

	public CustomFontEditText(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setFont(attrs);
	}

	public CustomFontEditText(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setFont(attrs);
	}

	public CustomFontEditText(Context context)
	{
		super(context);
	}

	@Override
	public void setTypeface(Typeface tf, int style)
	{
		if (!isInEditMode())
		{
			/*
			 * If we are dealing with LDPI phones, we use the default font, They have a rendering issue with the font that we're using
			 */
			if (Utils.scaledDensityMultiplier <= 0.75f)
			{
				if (style == Typeface.ITALIC || style == Typeface.BOLD_ITALIC)
				{
					style = Typeface.NORMAL;
				}
				super.setTypeface(tf, style);
				return;
			}

			if (fontName == null)
			{
				fontName = "roboto";
			}
			customTypeFace = CustomTypeFace.getTypeFace(fontName);
			if (customTypeFace == null)
			{
				customTypeFace = new CustomTypeFace(getContext(), fontName);
				CustomTypeFace.customTypeFaceList.add(customTypeFace);
			}

			if (style == Typeface.BOLD)
			{
				super.setTypeface(customTypeFace.bold);
			}
			else if (style == Typeface.ITALIC)
			{
				super.setTypeface(customTypeFace.thin);
			}
			else if (style == Typeface.BOLD_ITALIC)
			{
				super.setTypeface(customTypeFace.medium);
			}
			else
			{
				super.setTypeface(customTypeFace.normal);
			}
		}
	}

	public void setBackKeyListener(BackKeyListener listener)
	{
		this.listener = listener;
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event)
	{
		boolean result = false;
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			result = super.onKeyPreIme(keyCode, event);

			// User has pressed Back key Hide your view as you do it in your activity
			if (listener != null)
			{
				listener.onBackKeyPressedET(this);
			}
		}
		return result;
	}

	public static interface BackKeyListener
	{
		public void onBackKeyPressedET(CustomFontEditText editText);
	}
}
