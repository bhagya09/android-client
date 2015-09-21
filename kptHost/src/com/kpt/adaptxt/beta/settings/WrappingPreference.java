package com.kpt.adaptxt.beta.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class WrappingPreference extends Preference {

	public WrappingPreference(Context context) {
		super(context);
	}
	
	public WrappingPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public WrappingPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		
		final TextView title = (TextView) view.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
        }
	}
}
