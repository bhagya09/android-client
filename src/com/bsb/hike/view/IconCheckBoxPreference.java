package com.bsb.hike.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.R;
import com.bsb.hike.ui.HikePreferences;

public class IconCheckBoxPreference extends CheckBoxPreference
{
	private Drawable mIcon;

	private ImageView imageView;

	public IconCheckBoxPreference(final Context context, final AttributeSet attrs, final int defStyle)
	{
		super(context, attrs, defStyle);
		setIcon(context, attrs);
	}

	private void setIcon(Context context, AttributeSet attrs)
	{
		String iconName = attrs.getAttributeValue(null, "icon");
		if (!TextUtils.isEmpty(iconName))
		{
			iconName = iconName.split("/")[1];
			int id = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
			this.mIcon = context.getResources().getDrawable(id);
		}
	}

	public IconCheckBoxPreference(Context context)
	{
		super(context);
	}

	public IconCheckBoxPreference(final Context context, final AttributeSet attrs)
	{
		super(context, attrs);
		setIcon(context, attrs);
	}

	protected void onBindView(final View view)
	{
		super.onBindView(view);
		
		view.setAlpha(isEnabled() ? HikePreferences.PREF_ENABLED_ALPHA : HikePreferences.PREF_DISABLED_ALPHA);
		
		imageView = (ImageView) view.findViewById(R.id.icon);
		
		if ((imageView != null) && (this.mIcon != null))
		{
			imageView.setImageDrawable(this.mIcon);
			imageView.setVisibility(View.VISIBLE);
			imageView.setSelected(isChecked());
		}
		else if(imageView != null)
		{
			imageView.setVisibility(View.GONE);
		}
	}

	@Override
	protected void notifyChanged()
	{
		if (imageView != null)
		{
			imageView.setSelected(isChecked());
		}
		super.notifyChanged();
	}

}