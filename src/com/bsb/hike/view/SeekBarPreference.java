/**
 * 
 */
package com.bsb.hike.view;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * @author anubansal
 *
 */
public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
	private static final String androidns="http://schemas.android.com/apk/res/android";
	
	private SeekBar mSeekBar;
	
	private TextView mValueText;
	
	private Context mContext;
	
	private String mSuffix;
	
	private int mDefault, mMax, mValue = 0, mMin = 57;
	
	/**
	 * @param context
	 * @param attrs
	 */
	public SeekBarPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
		
		mSuffix = attrs.getAttributeValue(androidns, "text");
		mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
		mMax = attrs.getAttributeIntValue(androidns, "max", 100);
	}
	
	@Override
	protected View onCreateDialogView()
	{
		LinearLayout.LayoutParams params;
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6, 6, 6, 6);
		
		mValueText = new TextView(mContext);
		mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
		mValueText.setTextSize(20);
		params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(mValueText, params);
		
		mSeekBar = new SeekBar(mContext);
		mSeekBar.setOnSeekBarChangeListener(this);
		layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		
		if (shouldPersist())
			mValue = getPersistedInt(mDefault);
		
		mSeekBar.setMax(mMax);
		mSeekBar.setProgress(mValue);
		return layout;
	}

	@Override
	protected void onBindDialogView(View view)
	{
		super.onBindDialogView(view);
		mSeekBar.setMax(mMax);
		mSeekBar.setProgress(mValue);
	}
	
	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
	{
		super.onSetInitialValue(restorePersistedValue, defaultValue);
		if (restorePersistedValue)
		{
			mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
		}
		else
		{
			mValue = (Integer)defaultValue;
		}
	}
	
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	{
		String t = String.valueOf(progress + mMin);
		mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
		if (shouldPersist())
		{
			persistInt(progress);
		}
		callChangeListener(new Integer(progress));
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{
		
	}
	
	public void setMax(int max)
	{
		mMax = max;
	}
	
	public int getMax()
	{
		return mMax;
	}

	public void setProgress(int progress)
	{
		mValue = progress;
		if (mSeekBar != null)
		{
			mSeekBar.setProgress(progress);
		}
	}
	
	public int getProgress()
	{
		return mValue;
	}
}
