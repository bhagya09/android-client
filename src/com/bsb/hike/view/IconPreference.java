package com.bsb.hike.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.R;

public class IconPreference extends Preference
{

	private Drawable mIcon;

	public IconPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setIcon(context, attrs);
	}

	private void setIcon(Context context, AttributeSet attrs)
	{
		String iconName = attrs.getAttributeValue(null, "icon");
		if(!TextUtils.isEmpty(iconName))
		{
			iconName = iconName.split("/")[1];
			int id = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());

			this.mIcon = context.getResources().getDrawable(id);	
		}
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);
		final ImageView imageView = (ImageView) view.findViewById(R.id.icon);
		if ((imageView != null) && (this.mIcon != null))
		{
			imageView.setImageDrawable(this.mIcon);
			imageView.setVisibility(View.VISIBLE);
		}
		else 
		{
			imageView.setVisibility(View.GONE);
		}
	}

}
