package com.kpt.adaptxt.beta.settings;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class WrappingCheckBoxPreference extends CheckBoxPreference {

	public WrappingCheckBoxPreference(Context context) {
		super(context);
	}
	
	public WrappingCheckBoxPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public WrappingCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
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
