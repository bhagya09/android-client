package com.bsb.hike.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.ui.HikePreferences;

public class IconPreference extends Preference
{

	private Drawable mIcon;
	
	private String disabledSummary =  "";

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
		
		ViewCompat.setAlpha(view, isEnabled() ? HikePreferences.PREF_ENABLED_ALPHA : HikePreferences.PREF_DISABLED_ALPHA);
		
		TextView subText = (TextView) view.findViewById(android.R.id.summary);
		
		if (isEnabled())
		{
			subText.setText(getSummary());
		}
		
		else
		{
			subText.setText(TextUtils.isEmpty(getDisabledSummaryText()) ? getSummary() : getDisabledSummaryText());
		}
		
		final ImageView imageView = (ImageView) view.findViewById(R.id.icon);
		if ((imageView != null) && (this.mIcon != null))
		{
			imageView.setImageDrawable(this.mIcon);
			imageView.setVisibility(View.VISIBLE);
		}
		else if(imageView != null)
		{
			imageView.setVisibility(View.GONE);
		}
	}
	
	private CharSequence getDisabledSummaryText()
	{
		return this.disabledSummary;
	}

	public void setDisabledSummaryText(String disabledText)
	{
		this.disabledSummary = disabledText;
	}
	
	

}
