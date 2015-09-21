package com.kpt.adaptxt.beta.settings;

import android.app.AlertDialog.Builder;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class HideXiAccDialogPreference extends DialogPreference {

	private Context mContext;
	String[] listEntries;
	private boolean hideXiFlag , hideAccFlag = false;
	private SharedPreferences preferences;
	private ListView dialoglayout;
	//Drawable[] drawablesList;

	public HideXiAccDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		preferences = PreferenceManager.getDefaultSharedPreferences(context);

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
	protected void onPrepareDialogBuilder(Builder builder) {
		super.onPrepareDialogBuilder(builder);

		listEntries = mContext.getResources().getStringArray(R.array.kpt_hide_xi_acc_options);
		dialoglayout = (ListView) View.inflate(getContext(), R.layout.kpt_themes_layout, null);
		dialoglayout.setCacheColorHint(Color.TRANSPARENT);

		hideAccFlag = preferences.getBoolean(KPTConstants.PREF_HIDE_ACC_KEY, false);
		hideXiFlag = preferences.getBoolean(KPTConstants.PREF_HIDE_XI_KEY, false);

		final ThemeDialogAdapter listAdapter = new ThemeDialogAdapter(mContext,
				R.layout.kpt_hide_xi_list_item, listEntries, 
				null);

		builder.setPositiveButton(mContext.getResources().getString(R.string.kpt_alert_dialog_ok), new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				Editor prefEditor = preferences.edit();

				prefEditor.putBoolean(KPTConstants.PREF_HIDE_ACC_KEY, false);
				prefEditor.putBoolean(KPTConstants.PREF_HIDE_XI_KEY, false);
				prefEditor.commit();
			}
		});

		dialoglayout.setOnItemClickListener(new OnItemClickListener() {

			private ViewHolder holder;

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {


				holder = (ViewHolder) arg1.getTag();
				//holder.textView.toggle();

				if(pos == 0){
					if(holder.textView.isChecked()) {
						hideXiFlag = false;
						holder.textView.setCheckMarkDrawable(android.R.drawable.checkbox_off_background);
						holder.textView.setChecked(false);
					} else {
						hideXiFlag = true;
						holder.textView.setCheckMarkDrawable(android.R.drawable.checkbox_on_background);
						holder.textView.setChecked(true);
					}
				}else{
					if(holder.textView.isChecked()) {
						hideAccFlag = false;
						holder.textView.setCheckMarkDrawable(android.R.drawable.checkbox_off_background);
						holder.textView.setChecked(false);
					} else {
						hideAccFlag = true;
						holder.textView.setCheckMarkDrawable(android.R.drawable.checkbox_on_background);
						holder.textView.setChecked(true);
					}
				}
			}

		});

		dialoglayout.setAdapter(listAdapter);
		builder.setView(dialoglayout);
	}

	public class ThemeDialogAdapter extends BaseAdapter {

		//private Drawable[] icons;
		private CharSequence[] titles;
		private LayoutInflater mLayoutInflator;

		public ThemeDialogAdapter(Context context, int textViewResourceId,
				CharSequence[] text,Drawable[] ids) {
			//icons = ids;
			titles = text;
			mLayoutInflator = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return titles.length;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			if (convertView == null) {
				convertView = mLayoutInflator.inflate(R.layout.kpt_hide_xi_list_item, null);

				holder = new ViewHolder();
				holder.imageView = (ImageView)convertView.findViewById(R.id.theme_select_image);
				holder.textView = (CheckedTextView) convertView.findViewById(R.id.radioButton);
				//holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkButton);

				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			//holder.textView.setCheckMarkDrawable(android.R.attr.listChoiceIndicatorMultiple);
			holder.textView.setText(titles[position]);

			if(position == 0){
				holder.imageView.setBackgroundResource(R.drawable.kpt_image_hide_undo);
				if(hideXiFlag){
					holder.textView.setCheckMarkDrawable(android.R.drawable.checkbox_on_background);
					holder.textView.setChecked(true);
				}else{
					holder.textView.setCheckMarkDrawable(android.R.drawable.checkbox_off_background);
					holder.textView.setChecked(false);
				}
			}else{
				holder.imageView.setBackgroundResource(R.drawable.kpt_image_hide_dismiss);
				if(hideAccFlag){
					holder.textView.setCheckMarkDrawable(android.R.drawable.checkbox_on_background);
					holder.textView.setChecked(true);
				}else{
					holder.textView.setCheckMarkDrawable(android.R.drawable.checkbox_off_background);
					holder.textView.setChecked(false);
				}
			}


			return convertView;
		}
	}

	static class ViewHolder{
		CheckedTextView textView;
		//CheckBox checkBox;
		ImageView imageView;
	}
	
	/*@Override
	public void onClick(DialogInterface dialog, int which) {
		super.onClick(dialog, which);
		
		if(which == DialogInterface.BUTTON_POSITIVE){
			Log.e("VMC", "CLKICKED OK BUTTON");
			
			Editor prefEditor = preferences.edit();

			prefEditor.putBoolean(KPTConstants.PREF_HIDE_ACC_KEY, hideAccFlag);
			prefEditor.putBoolean(KPTConstants.PREF_HIDE_XI_KEY, hideXiFlag);
			prefEditor.commit();
			
		}else if(which == DialogInterface.BUTTON_NEGATIVE){
			Log.e("VMC", "CLKICKED CANCEL BUTTON");
		}
	}*/

}
