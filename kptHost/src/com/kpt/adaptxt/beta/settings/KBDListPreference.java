/**
 * Copyright CMW Mobile.com, 2010. 
 */
package com.kpt.adaptxt.beta.settings;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.util.KPTConstants;


/**
 * The KBDListPreference class 
 * 
 * @author Nikhil Joshi
 */

public class KBDListPreference extends ListPreference {
	private int[] resourceIds = null;
	
	public KBDListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	protected void onPrepareDialogBuilder(Builder builder) {
		int index = findIndexOfValue(getSharedPreferences().getString(
			getKey(), KPTConstants.KEYBOARD_TYPE_QWERTY));

		ListAdapter listAdapter = new KBDArrayAdapter(getContext(),
			R.layout.kpt_kbd_list_item, getEntries(), resourceIds, index);

		builder.setAdapter(listAdapter, this);
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
	
	public class KBDArrayAdapter extends ArrayAdapter<CharSequence> {
		private int index = 0;
		int color;

		public KBDArrayAdapter(Context context, int textViewResourceId,
				CharSequence[] objects, int[] ids, int i) {
			super(context, textViewResourceId, objects);
			index = i;
			resourceIds = ids;
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
				color = getContext().getResources().getColor(R.color.kpt_balck_color_text);
			}else{
				color = getContext().getResources().getColor(R.color.kpt_white_color_text);
			}
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
			View row = inflater.inflate(R.layout.kpt_kbd_list_item, parent, false);
			

			CheckedTextView checkedTextView = (CheckedTextView)row.findViewById(
				R.id.radioButton);

			checkedTextView.setText(getItem(position));
			checkedTextView.setTextColor(color);

			if (position == index) {
				checkedTextView.setChecked(true);
			}

			return row;
		}
	}

	
	
}
