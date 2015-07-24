package com.bsb.hike.view;

import com.bsb.hike.ui.HikePreferences;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;

public class IconCheckBoxPreference extends CheckBoxPreference
{
	public IconCheckBoxPreference(final Context context, final AttributeSet attrs, final int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public IconCheckBoxPreference(Context context)
	{
		super(context);
	}

	public IconCheckBoxPreference(final Context context, final AttributeSet attrs)
	{
		super(context, attrs);
	}

	protected void onBindView(final View view)
	{
		view.setAlpha(isEnabled() ? HikePreferences.PREF_ENABLED_ALPHA : HikePreferences.PREF_DISABLED_ALPHA);
		
		super.onBindView(view);
	}

	@Override
	protected void notifyChanged()
	{
		super.notifyChanged();
	}

}