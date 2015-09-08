/**
 * Licensed for use by hike limited.
 */
package com.bsb.hike.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.ui.HikePreferences;

/**
 * A backport of {@link SwitchPreference} which works all the way down to API Level 7. <br>
 * It uses {@link SwitchCompat} instead of {@link Switch}
 * 
 * 
 * A {@link Preference} that provides a two-state toggleable option.
 * <p>
 * This preference will store a boolean into the SharedPreferences.
 * 
 * @attr ref R.styleable#SwitchPreference_summaryOff
 * @attr ref R.styleable#SwitchPreference_summaryOn
 * @attr ref R.styleable#SwitchPreference_switchTextOff
 * @attr ref R.styleable#SwitchPreference_switchTextOn
 * @attr ref R.styleable#SwitchPreference_disableDependentsState
 * 
 * @author piyush
 * 
 */
public class SwitchPreferenceCompat extends com.bsb.hike.view.TwoStatePreference
{

	// Switch text for on and off states
	private CharSequence mSwitchOn;

	private CharSequence mSwitchOff;
	
	private CharSequence disabledSubText;
	
	private final Listener mListener = new Listener();

	private class Listener implements CompoundButton.OnCheckedChangeListener
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			if (!callChangeListener(isChecked))
			{
				// Listener didn't like it, change it back.
				// CompoundButton will make sure we don't recurse.
				buttonView.setChecked(!isChecked);
				return;
			}

			SwitchPreferenceCompat.this.setChecked(isChecked);
		}
	}

	/**
	 * Construct a new SwitchPreference with the given style options.
	 * 
	 * @param context
	 *            The Context that will style this preference
	 * @param attrs
	 *            Style attributes that differ from the default
	 * @param defStyle
	 *            Theme attribute defining the default style options
	 */
	public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwitchPreference, defStyle, 0);
		setSummaryOn(a.getString(R.styleable.SwitchPreference_summaryOn));
		setSummaryOff(a.getString(R.styleable.SwitchPreference_summaryOff));
		setSwitchTextOn(a.getString(R.styleable.SwitchPreference_switchTextOn));
		setSwitchTextOff(a.getString(R.styleable.SwitchPreference_switchTextOff));
		setDisableDependentsState(a.getBoolean(R.styleable.SwitchPreference_disableDependentsState, false));
		a.recycle();
	}

	/**
	 * Construct a new SwitchPreference with default style options.
	 * 
	 * @param context
	 *            The Context that will style this preference
	 */
	public SwitchPreferenceCompat(Context context)
	{
		this(context, null);
	}

	/**
	 * Construct a new SwitchPreference with the given style options.
	 * 
	 * @param context
	 *            The Context that will style this preference
	 * @param attrs
	 *            Style attributes that differ from the default
	 */
	public SwitchPreferenceCompat(Context context, AttributeSet attrs)
	{
		this(context, attrs, R.attr.switchPreferenceStyle);
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);
		
		ViewCompat.setAlpha(view, isEnabled() ? HikePreferences.PREF_ENABLED_ALPHA : HikePreferences.PREF_DISABLED_ALPHA);
		
		TextView subText = (TextView) view.findViewById(android.R.id.summary);
		
		if (subText != null)
		{
			if (isEnabled())
			{
				subText.setText(getSummary());
			}

			else
			{
				subText.setText(TextUtils.isEmpty(getDisabledSummaryText()) ? getSummary() : getDisabledSummaryText());
			}
		}
		

		View checkableView = view.findViewById(R.id.switchWidget);
		if (checkableView != null && checkableView instanceof Checkable)
		{
			((Checkable) checkableView).setChecked(mChecked);

			sendAccessibilityEvent(checkableView);

			if (checkableView instanceof SwitchCompat)
			{
				final SwitchCompat switchView = (SwitchCompat) checkableView;
				switchView.setTextOn(mSwitchOn);
				switchView.setTextOff(mSwitchOff);
				switchView.setOnCheckedChangeListener(mListener);
			}
		}

		syncSummaryView(view);
	}

	/**
	 * Set the text displayed on the switch widget in the on state. This should be a very short string; one word if possible.
	 * 
	 * @param onText
	 *            Text to display in the on state
	 */
	public void setSwitchTextOn(CharSequence onText)
	{
		mSwitchOn = onText;
		notifyChanged();
	}

	/**
	 * Set the text displayed on the switch widget in the off state. This should be a very short string; one word if possible.
	 * 
	 * @param offText
	 *            Text to display in the off state
	 */
	public void setSwitchTextOff(CharSequence offText)
	{
		mSwitchOff = offText;
		notifyChanged();
	}

	/**
	 * Set the text displayed on the switch widget in the on state. This should be a very short string; one word if possible.
	 * 
	 * @param resId
	 *            The text as a string resource ID
	 */
	public void setSwitchTextOn(int resId)
	{
		setSwitchTextOn(getContext().getString(resId));
	}

	/**
	 * Set the text displayed on the switch widget in the off state. This should be a very short string; one word if possible.
	 * 
	 * @param resId
	 *            The text as a string resource ID
	 */
	public void setSwitchTextOff(int resId)
	{
		setSwitchTextOff(getContext().getString(resId));
	}

	/**
	 * @return The text that will be displayed on the switch widget in the on state
	 */
	public CharSequence getSwitchTextOn()
	{
		return mSwitchOn;
	}

	/**
	 * @return The text that will be displayed on the switch widget in the off state
	 */
	public CharSequence getSwitchTextOff()
	{
		return mSwitchOff;
	}
	
	private CharSequence getDisabledSummaryText()
	{
		return this.disabledSubText;
	}

	public void setDisabledSummaryText(String disabledText)
	{
		this.disabledSubText = disabledText;
	}
	

}
