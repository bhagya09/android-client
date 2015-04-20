package com.bsb.hike.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

public class IconListPreference extends ListPreference
{
	private Drawable mIcon;
	private int mTitleColor = -1;

	public IconListPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setIcon(context, attrs);
	}

	private void setIcon(Context context, AttributeSet attrs)
	{
		String iconName = attrs.getAttributeValue(null, "icon");
		iconName = iconName.split("/")[1];
		int id = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());

		this.mIcon = context.getResources().getDrawable(id);
	}

	public void setTitleColor(int color)
	{
		this.mTitleColor = color;
		notifyChanged();
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
		final TextView titleTextView = (TextView) view.findViewById(android.R.id.title);
		if ((titleTextView != null) && (this.mTitleColor >= 0))
		{
			titleTextView.setTextColor(HikeMessengerApp.getInstance().getApplicationContext().getResources().getColor(this.mTitleColor));
			titleTextView.setVisibility(View.VISIBLE);
		}
	}
}
