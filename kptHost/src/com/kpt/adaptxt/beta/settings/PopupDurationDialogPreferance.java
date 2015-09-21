package com.kpt.adaptxt.beta.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class PopupDurationDialogPreferance extends DialogPreference {

	private Button mCancel,mOk,mDefault;
	private SeekBar mSeek;
	private TextView mPopupMeasure;
	private int mProgress;

	public PopupDurationDialogPreferance(Context context, AttributeSet attrs) {
		super(context, attrs);			
		setPersistent(false);		
		setDialogLayoutResource(R.layout.kpt_popup_delay);		
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		builder.setTitle(R.string.kpt_UI_STRING_MENU_ITEM_5_300);
		builder.setPositiveButton(null, null);
		builder.setNegativeButton(null, null);
		super.onPrepareDialogBuilder(builder);  
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		
		final TextView title = (TextView) view.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
        }
	}
	
	@Override
	public void onBindDialogView(View view){
		mOk = (Button)view.findViewById(R.id.popup_Ok);
		mDefault = (Button)view.findViewById(R.id.popup_default); 
		mCancel = (Button)view.findViewById(R.id.popup_cancel);
		mSeek = (SeekBar)view.findViewById(R.id.popup_seekBar);
		mPopupMeasure = (TextView)view.findViewById(R.id.popup_value);		
		int mPopupVariation = getSharedPreferences().getInt(KPTConstants.PREF_KEY_POPUP,KPTConstants.DEFAULT_POPUP);
		mProgress = mPopupVariation;
		int progressD = mPopupVariation -200;	
		mPopupMeasure.setText(String.valueOf(mPopupVariation+"ms"));
		mSeek.setMax(200);
		mSeek.setPressed(true);
		mSeek.setProgress(progressD);		 
		mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ 

			@Override 
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) { 							
				mProgress = progress + 200;
				mPopupMeasure.setText(String.valueOf(mProgress+"ms"));							  				
			} 

			@Override 
			public void onStartTrackingTouch(SeekBar seekBar) { 
				// TODO Auto-generated method stub 	
			} 

			@Override 
			public void onStopTrackingTouch(SeekBar seekBar) { 
				// TODO Auto-generated method stub 
			} 
		}); 

		mDefault.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mProgress=KPTConstants.DEFAULT_POPUP;				
				SharedPreferences.Editor prefsEditor = getSharedPreferences().edit();
				prefsEditor.putInt(KPTConstants.PREF_KEY_POPUP,mProgress);
				prefsEditor.commit();
				getDialog().dismiss();
			}
		});

		mOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor prefsEditor = getSharedPreferences().edit();
				prefsEditor.putInt(KPTConstants.PREF_KEY_POPUP,mProgress);
				prefsEditor.commit();
				getDialog().dismiss();
			}
		});

		mCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getDialog().dismiss();
			}
		});
		super.onBindDialogView(view);
	}
}


