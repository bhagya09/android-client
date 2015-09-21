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

public class SoundVolumeDialogPreferance extends DialogPreference{	
	private Button mCancel,mOk,mDefault;
	private SeekBar mSeek;
	private TextView mSoundMeasure;   
	public float mProgress_set;

	public SoundVolumeDialogPreferance(Context context, AttributeSet attrs) {
		super(context, attrs);		
		setPersistent(false);		
		setDialogLayoutResource(R.layout.kpt_sound_delay);
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		builder.setTitle(R.string.kpt_UI_STRING_MENU_ITEM_3_300);
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
		mOk = (Button)view.findViewById(R.id.sound_Ok);
		mDefault = (Button)view.findViewById(R.id.sound_default); 
		mCancel =  (Button)view.findViewById(R.id.sound_cancel);
		mSeek = (SeekBar)view.findViewById(R.id.popup_seekBar);
		mSoundMeasure=  (TextView)view.findViewById(R.id.sound_value);
		float soundVariation = getSharedPreferences().getFloat(KPTConstants.PREF_KEY_SOUND,KPTConstants.DEFAULT_VOLUME);
		mProgress_set = soundVariation;
		float soundChange =soundVariation-1.0f;
		mSoundMeasure.setText(String.valueOf((int)soundVariation));
		mSeek.setPressed(true);
		mSeek.setProgress((int)soundChange);
		mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ 

			@Override 
			public void onProgressChanged(SeekBar seekBar, int progress, 
					boolean fromUser) { 
				// TODO Auto-generated method stub 
				mProgress_set = progress+1.0f;				
				mSoundMeasure.setText(String.valueOf((int)mProgress_set));							
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
				mProgress_set=KPTConstants.DEFAULT_VOLUME;
				SharedPreferences.Editor prefsEditor = getSharedPreferences().edit();
				prefsEditor.putFloat(KPTConstants.PREF_KEY_SOUND,mProgress_set);
				prefsEditor.commit();
				getDialog().dismiss();
			}
		});

		mOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {			
				SharedPreferences.Editor prefsEditor = getSharedPreferences().edit();
				prefsEditor.putFloat(KPTConstants.PREF_KEY_SOUND,mProgress_set);
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


