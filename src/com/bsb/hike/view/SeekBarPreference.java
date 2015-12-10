package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.bsb.hike.R;

public final class SeekBarPreference extends DialogPreference implements
		OnSeekBarChangeListener {

	// Namespaces to read attributes
	private static final String PREFERENCE_NS = "http://schemas.android.com/apk/res/com.mnm.seekbarpreference";
	private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

	// Attribute names
	private static final String ATTR_DEFAULT_VALUE = "defaultValue";
	private static final String ATTR_MIN_VALUE = "minValue";
	private static final String ATTR_MAX_VALUE = "maxValue";

	// Default values for defaults
	private static final int DEFAULT_CURRENT_VALUE = 50;
	private static final int DEFAULT_MIN_VALUE = 0;
	private static final int DEFAULT_MAX_VALUE = 100;

	// Real defaults
	private final int mDefaultValue;
	private final int mMaxValue;
	private  int mMinValue = 0;

	// Current value
	private int mCurrentValue;

	// View elements
	private SeekBar mSeekBar;
	private TextView mValueText;
	private Context mContext;

	private CustomTypeFace prefFont;

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;

		mDefaultValue = attrs.getAttributeIntValue(ANDROID_NS, "defaultValue", 0);
		mMaxValue = attrs.getAttributeIntValue(ANDROID_NS, "max", 100);

		prefFont = CustomTypeFace.getTypeFace("roboto");
		
	}

	@Override
	protected View onCreateDialogView()

	{
		// Get current value from settings
		if (shouldPersist())
			mCurrentValue = getPersistedInt(mDefaultValue);

		// Inflate layout
		LinearLayout.LayoutParams params;

		LinearLayout layout = new LinearLayout(mContext);

		layout.setOrientation(LinearLayout.VERTICAL);

		layout.setPadding(6, 6, 6, 6);

		mValueText = new TextView(mContext);

		mValueText.setGravity(Gravity.CENTER_HORIZONTAL);

		mValueText.setTextSize(20);
		mValueText.setText(Integer.toString(mCurrentValue));
		mValueText.setTypeface(prefFont.normal);

		params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);

		layout.addView(mValueText, params);

		mSeekBar = new SeekBar(mContext);

		

		layout.addView(mSeekBar, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		
		 // Setup SeekBar
		mSeekBar.setMax(mMaxValue);
		mSeekBar.setProgress(mCurrentValue - mMinValue);
		mSeekBar.setOnSeekBarChangeListener(this);

		return layout;

	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		TextView titleView = (TextView) view.findViewById(android.R.id.title);
		titleView.setTextAppearance(mContext, R.style.SettingsHeaderItemTextView);
		titleView.setTypeface(prefFont.normal);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(10, 0, 0, 0); // llp.setMargins(left, top, right, bottom);
		titleView.setLayoutParams(params);

		TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
		summaryView.setTextAppearance(mContext, R.style.ListItemSubHeaderTextView);
		summaryView.setTypeface(prefFont.normal);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		// Return if change was cancelled
		if (!positiveResult) {
			return;
		}

		// Persist current value if needed
		if (shouldPersist()) {
			persistInt(mCurrentValue);
		}

		// Notify activity about changes (to update preference summary line)
		notifyChanged();
		callChangeListener(mCurrentValue);
	
	}

	@Override
	public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
		// Update current value
		mCurrentValue = value + mMinValue;
		// Update label with current value
		mValueText.setText(Integer.toString(value + mMinValue));
	}

	public void onStartTrackingTouch(SeekBar seek) {
		// Not used
	}

	public void onStopTrackingTouch(SeekBar seek) {
		// Not used
	}

	public void setMinimun(int min) {
		mMinValue = min;
	}
	
	public int getCurrentValue()
	{
		return getPersistedInt(mDefaultValue);
	}
	
	public void setCurrentValue(int value)
	{
		this.mCurrentValue = value;
	}
}