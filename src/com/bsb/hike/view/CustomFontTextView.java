package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class CustomFontTextView extends TextView {

	private String fontName;

	private void setFont(AttributeSet attrs)
	{
		fontName = attrs.getAttributeValue(null, "font");
	}

	public CustomFontTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setFont(attrs);
	}

	public CustomFontTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFont(attrs);
	}

	public CustomFontTextView(Context context) {
		super(context);
	}

	@Override
	public void setTypeface(Typeface tf, int style) {
		if (!isInEditMode()) {
			if (CustomTypeFace.customTypeFace == null) {
				CustomTypeFace.customTypeFace = new CustomTypeFace(getContext(), fontName);
			}

			if (style == Typeface.BOLD) {
				super.setTypeface(CustomTypeFace.customTypeFace.bold);
			} else if (style == Typeface.ITALIC) {
				super.setTypeface(CustomTypeFace.customTypeFace.thin);
			} else {
				super.setTypeface(CustomTypeFace.customTypeFace.normal);
			}
		}
	}
}
