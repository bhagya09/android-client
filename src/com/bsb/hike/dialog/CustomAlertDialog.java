package com.bsb.hike.dialog;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsb.hike.R;

/**
 * CustomAlertDialog is a class that extends {@link HikeDialog}. The purpose of this class is
 * keep the alert dialog implementation to as close as possible to the native AlertDialog
 * and preserve our custom settings(UI) like fonts, etc... 
 * @author gauravmittal
 */
public class CustomAlertDialog extends HikeDialog
{

	Context mContext;

	View titleTemplate;

	TextView title;

	TextView message;

	CheckBox checkBox;

	View buttonPanel;

	Button buttonPositive;

	Button buttonNegative;

	Button buttonNeutral;
	
	ProgressBar mProgressIndeterminate;
	
	private static final int DEFAULT_LAYOUT_RESID = R.layout.custom_dialog;
	
	private int layoutResId;

	public CustomAlertDialog(Context context, int dialogId)
	{
		this(context, dialogId, DEFAULT_LAYOUT_RESID);
	}
	
	public CustomAlertDialog(Context context, int dialogId, int layoutResId)
	{
		super(context, dialogId);
		this.mContext = context;
		this.layoutResId = layoutResId;
		initViews();
	}

	private void initViews()
	{
		this.setContentView(layoutResId);
		this.setCancelable(true);

		titleTemplate = (View) this.findViewById(R.id.title_template);
		title = (TextView) this.findViewById(R.id.title);
		message = (TextView) this.findViewById(R.id.message);
		checkBox = (CheckBox) this.findViewById(R.id.checkbox);
		buttonPanel = (View) this.findViewById(R.id.button_panel);
		buttonPositive = (Button) this.findViewById(R.id.btn_positive);
		buttonNegative = (Button) this.findViewById(R.id.btn_negative);
		buttonNeutral = (Button) this.findViewById(R.id.btn_neutral);
		mProgressIndeterminate = (ProgressBar) this.findViewById(R.id.loading_progress);
	}

	public void setTitle(int resId)
	{
		setTitle(mContext.getString(resId));
	}

	@Override
	public void setTitle(CharSequence titleText)
	{
		title.setText(titleText);
		titleTemplate.setVisibility(View.VISIBLE);
	}

	public void setMessage(int resId)
	{
		setMessage(mContext.getString(resId));
	}

	public void setMessage(CharSequence messageText)
	{
		message.setText(messageText);
	}

	public void setCheckBox(int textResId, OnCheckedChangeListener listener, boolean isChecked)
	{
		setCheckBox(mContext.getString(textResId), listener, isChecked);
	}

	public void setCheckBox(CharSequence checkBoxText, OnCheckedChangeListener listener, boolean isChecked)
	{
		checkBox.setText(checkBoxText);
		checkBox.setOnCheckedChangeListener(listener);
		checkBox.setChecked(isChecked);
		checkBox.setVisibility(View.VISIBLE);
	}

	public boolean isChecked()
	{
		return checkBox.isChecked();
	}

	private void setHikeDialogButtonClick(Button buttonView, final HikeDialogListener l, final int whichButton)
	{
		buttonView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				switch (whichButton)
				{
				case HikeDialogListener.BUTTON_POSITIVE:
					l.positiveClicked(CustomAlertDialog.this);
					break;
				case HikeDialogListener.BUTTON_NEGATIVE:
					l.negativeClicked(CustomAlertDialog.this);
					break;
				case HikeDialogListener.BUTTON_NEUTRAL:
					l.neutralClicked(CustomAlertDialog.this);
					break;
				}
			}
		});
	}

	public void setPositiveButton(int textResId, HikeDialogListener l)
	{
		setPositiveButton(mContext.getString(textResId), l);
	}

	public void setPositiveButton(CharSequence text, HikeDialogListener l)
	{
		buttonPositive.setText(text);
		buttonPositive.setVisibility(View.VISIBLE);
		if (l != null)
			setHikeDialogButtonClick(buttonPositive, l, HikeDialogListener.BUTTON_POSITIVE);
		buttonPanel.setVisibility(View.VISIBLE);
	}

	public void setNegativeButton(int textResId, HikeDialogListener l)
	{
		setNegativeButton(mContext.getString(textResId), l);
	}

	public void setNegativeButton(CharSequence text, HikeDialogListener l)
	{
		buttonNegative.setText(text);
		buttonNegative.setVisibility(View.VISIBLE);
		if (l != null)
			setHikeDialogButtonClick(buttonNegative, l, HikeDialogListener.BUTTON_NEGATIVE);
		buttonPanel.setVisibility(View.VISIBLE);
	}

	public void setNeutralButton(int textResId, HikeDialogListener l)
	{
		setNeutralButton(mContext.getString(textResId), l);
	}

	public void setNeutralButton(CharSequence text, HikeDialogListener l)
	{
		buttonNeutral.setText(text);
		buttonNeutral.setVisibility(View.VISIBLE);
		if (l != null)
			setHikeDialogButtonClick(buttonNeutral, l, HikeDialogListener.BUTTON_NEUTRAL);
		buttonPanel.setVisibility(View.VISIBLE);
	}
}
