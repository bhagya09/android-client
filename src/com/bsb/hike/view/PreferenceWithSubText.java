/**
 * 
 */
package com.bsb.hike.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.ui.HikePreferences;

/**
 * Custom preference class which allows us to create a subtext to the right of the title in the preferences screen
 * 
 * @author piyush
 * 
 */
public class PreferenceWithSubText extends Preference
{

	private String subText;

	/**
	 * @param context
	 */
	public PreferenceWithSubText(Context context)
	{
		super(context);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public PreferenceWithSubText(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context, attrs);
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	public PreferenceWithSubText(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 * @param defStyleRes
	 */
	public PreferenceWithSubText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs)
	{

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MyPref);
		String subText = ta.getString(R.styleable.MyPref_s_text);
		ta.recycle();

		if (!TextUtils.isEmpty(subText))
		{
			this.subText = subText;
		}
	}
	
	public void setSubText(String subText)
	{
		this.subText = subText;
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);

		ViewCompat.setAlpha(view, isEnabled() ? HikePreferences.PREF_ENABLED_ALPHA : HikePreferences.PREF_DISABLED_ALPHA);

		final TextView subTextView = (TextView) view.findViewById(R.id.sub_text);
		
		if (subTextView == null) //Getting an NPE here in playStore. Defensive check.
		{
			return;
		}
		
		if (TextUtils.isEmpty(this.subText))
		{
			subTextView.setVisibility(View.GONE);
		}
		else
		{
			subTextView.setText(this.subText);
			subTextView.setVisibility(View.VISIBLE);
		}
	}

}
