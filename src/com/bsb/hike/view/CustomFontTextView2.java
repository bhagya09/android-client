package com.bsb.hike.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

public class CustomFontTextView2 extends CustomFontTextView implements ViewTreeObserver.OnGlobalLayoutListener
{

    public CustomFontTextView2(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    public CustomFontTextView2(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

	public CustomFontTextView2(Context context)
	{
		super(context);
	}

	@Override
	protected void onDetachedFromWindow() {
		ViewTreeObserver vto = getViewTreeObserver();
		if(vto != null && HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.SHOW_HIGH_RES_IMAGE, false))
		{
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			    vto.removeGlobalOnLayoutListener(this);
			} else {
			    vto.removeOnGlobalLayoutListener(this);
			}
		}
		super.onDetachedFromWindow();
	}
	
}
