package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Utils;

public class CustomFontTextView extends TextView 
{
	private String fontName;
	
	private CustomTypeFace customTypeFace;
	
    private void setFont(AttributeSet attrs)
    {
    	fontName = attrs.getAttributeValue(HikeConstants.NAMESPACE, HikeConstants.FONT);
    	if (Utils.isLollipopOrHigher())
    		setTypeface(getTypeface(), getTypeface().getStyle());
    }

    public CustomFontTextView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        setFont(attrs);
    }

    public CustomFontTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setFont(attrs);
    }

	public CustomFontTextView(Context context)
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


}
