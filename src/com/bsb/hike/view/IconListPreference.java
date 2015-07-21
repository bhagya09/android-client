package com.bsb.hike.view;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;

public class IconListPreference extends ListPreference
{
	private int mTitleColor = -1;

	public IconListPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
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
		TextView titleTextView = (TextView) view.findViewById(android.R.id.title);
		if ((titleTextView != null) && (this.mTitleColor >= 0))
		{
			titleTextView.setTextColor(HikeMessengerApp.getInstance().getApplicationContext().getResources().getColor(this.mTitleColor));
			titleTextView.setVisibility(View.VISIBLE);
		}
	}
}
