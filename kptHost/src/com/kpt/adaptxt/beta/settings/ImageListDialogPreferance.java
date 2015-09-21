package com.kpt.adaptxt.beta.settings;

import java.util.ArrayList;

import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
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

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.database.KPTAdaptxtDBHandler;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class ImageListDialogPreferance extends DialogPreference {
	
	private Drawable[] mThemeImages;
	private String[] mThemeNames;
	private Context mContext;
	private KPTAdaptxtDBHandler mKptDB;

	public ImageListDialogPreferance(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;
		mKptDB = new KPTAdaptxtDBHandler(context);
		setPositiveButtonText("");
		
	}
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		int currentThemeId;
		
		final String[] defaultThemes = mContext.getResources().getStringArray(R.array.kpt_theme_options_entries);
		
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		currentThemeId = Integer.parseInt(sp.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
		
		String themeName = getSelectedThemeName(currentThemeId);
		
		ListView dialoglayout = (ListView) view.findViewById(R.id.theme_list);
		dialoglayout.setCacheColorHint(Color.TRANSPARENT);
		mThemeImages = getDrawables();
		mThemeNames = getThemeNames(KPTConstants.THEME_ENABLE);
		
		final ThemeDialogAdapter listAdapter = new ThemeDialogAdapter(mContext,
				R.layout.kpt_theme_list_item, mThemeNames, 
				mThemeImages, themeName, defaultThemes);
		dialoglayout.setAdapter(listAdapter);
		
		dialoglayout.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int pos,
					long arg3) {
				
				
				if(pos > 2){
					CheckedTextView chk = (CheckedTextView) view.findViewById(R.id.radioButton);
					String selectedThemeName = chk.getText().toString();
					
					int rowId = getThemeId(selectedThemeName);
					
					SharedPreferences.Editor spEdit = sp.edit();
					spEdit.putString(KPTConstants.PREF_THEME_MODE, rowId+"");
					spEdit.commit();
					
					ImageListDialogPreferance.this.getDialog().dismiss();
					ImageListDialogPreferance.this.setSummary(selectedThemeName);
				}else{
					SharedPreferences.Editor spEdit = sp.edit();
					spEdit.putString(KPTConstants.PREF_THEME_MODE, pos+"");
					spEdit.commit();
					
					ImageListDialogPreferance.this.getDialog().dismiss();
					ImageListDialogPreferance.this.setSummary(defaultThemes[pos]);
				}
			}
		});
	}
	
	@Override
	protected void onClick() {
		super.onClick();
		
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		
		super.onClick(dialog, which);
	}
	
	private String getSelectedThemeName(int currentThemeId) {
		String themeName = null;
		try{
			themeName = mKptDB.getCurThemeName(currentThemeId);
			
		}catch (SQLiteException e) {
			e.printStackTrace();
		}
		return themeName;
	}
	
	
	private String[] getThemeNames(int themeEnabled){
		String[] namesArray = null;
		try{
			ArrayList<String> namesList = mKptDB.getThemeNames(themeEnabled);
			
			namesArray = new String[namesList.size()];
			for(int i = 0; i < namesList.size(); i++){
				namesArray[i] = namesList.get(i);
			}
		}catch (SQLException e) {
			e.printStackTrace();
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return namesArray;
	}
	
	private Drawable[] getDrawables(){
		return mKptDB.getAllThemeDrawable();
		
	}
	
	private int getThemeId(String themeName){
		return mKptDB.getThemeId(themeName);
		
	}
	
	
	/**
	 * The ImageArrayAdapter is the array adapter used for displaying an additional
	 * image to a list preference item.
	 * @author Casper Wakkers
	 */
	public class ThemeDialogAdapter extends BaseAdapter {

		private Drawable[] icons;
		private CharSequence[] titles;
		private String themeName;
		private String[] defaultNames;
		private LayoutInflater mLayoutInflator;
		private int currentThemeId;

		public ThemeDialogAdapter(Context context, int textViewResourceId,
				CharSequence[] text,Drawable[] ids, String name, String[] defaultThemes) {
			themeName = name;
			icons = ids;
			titles = text;
			defaultNames = defaultThemes;
			mLayoutInflator = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
			
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
			currentThemeId = Integer.parseInt(sp.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
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
		public View getView(int position, View convertView, ViewGroup parent) {

			View row = mLayoutInflator.inflate(R.layout.kpt_theme_list_item, parent, false);

			ImageView imageView = (ImageView) row.findViewById(R.id.theme_select_image);
			CheckedTextView checkedTextView = (CheckedTextView) row.findViewById(R.id.radioButton);

			imageView.setBackgroundDrawable(icons[position]);
			if(position > 2){
				checkedTextView.setText(titles[position]);
				
				if (checkedTextView.getText().toString().equals(themeName)) {
					checkedTextView.setChecked(true);
				}
			}else{
				checkedTextView.setText(defaultNames[position]);
				
				if(currentThemeId == position){
					checkedTextView.setChecked(true);
				}
			}
			
			

			return row;
		}
	}


	public void setDBObject(KPTAdaptxtDBHandler kptDB) {
		mKptDB = kptDB;
	}
	
}
