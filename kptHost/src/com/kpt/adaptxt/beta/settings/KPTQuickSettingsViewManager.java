package com.kpt.adaptxt.beta.settings;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kpt.adaptxt.beta.AdaptxtIME;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.database.KPTAdaptxtDBHandler;
import com.kpt.adaptxt.beta.database.KPTThemeItem;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class KPTQuickSettingsViewManager implements OnClickListener {

	private SharedPreferences mSharedPreferences;
	private Context mContext;


	/** 
	 * singleton object 
	 */
	private static KPTQuickSettingsViewManager mKptAdaptxtViewManager = null;

	/**
	 * This layout holds the input view and extra options for split layout
	 * (ie.. handwriting or navigation keys). 
	 */
	/**
	 * Qucik settings dialog
	 */
	private QuickSettingsDialog quickSettingsDialog;
	private AdaptxtIME mKptAdaptxtIME;
	private AlertDialog mOptionsDialog;
	private ThemeDialog mThemeDialog;
	Resources mResources;
	private ColorStateList moldColors;
	private KPTAdaptxtDBHandler kptDB;
	

	public  KPTQuickSettingsViewManager(Context context){
		mContext = context;
		mKptAdaptxtViewManager = this;
		mResources = context.getResources();
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

		kptDB = new KPTAdaptxtDBHandler(context);
	}

	public static KPTQuickSettingsViewManager getInstance() {
		return mKptAdaptxtViewManager;
	}

	private class QuickSettingsDialog extends Dialog{
		private SharedPreferences sharedPreferences;
		

		public QuickSettingsDialog(Context context) {
			super(context);
		}

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			this.requestWindowFeature(Window.FEATURE_NO_TITLE);

			WindowManager.LayoutParams lp = this.getWindow().getAttributes();
			lp.token = mKptAdaptxtIME.mInputView.getWindowToken();
			lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
			lp.dimAmount = 0.5f;
			lp.y = lp.y;

			this.getWindow().setAttributes(lp);
			this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);


			this.setContentView(R.layout.kpt_quicksettings);
			this.setCanceledOnTouchOutside(true);

			sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
			
			
			
			TextView tvPvtMode = (TextView) this.findViewById(R.id.quick_sett_prvtmode);
			moldColors =  tvPvtMode.getTextColors(); //save original colors

			TextView themesView = (TextView) this.findViewById(R.id.quick_sett_layouts);
			TextView settingsView = (TextView)this.findViewById(R.id.quick_sett_settings);
			//TextView customizeView = (TextView) this.findViewById(R.id.quick_sett_customize);
			TextView tvATROptions = (TextView) this.findViewById(R.id.quick_sett_atr_dialog);
			TextView tvRemoveAccents = (TextView) this.findViewById(R.id.quick_sett_rmvaccents);
			
			
			//listeners for dialog options
			themesView.setOnClickListener(KPTQuickSettingsViewManager.this);
			settingsView.setOnClickListener(KPTQuickSettingsViewManager.this);
			//customizeView.setOnClickListener(KPTQuickSettingsViewManager.this);
			tvPvtMode.setOnClickListener(KPTQuickSettingsViewManager.this);
			tvATROptions.setOnClickListener(KPTQuickSettingsViewManager.this);
			tvRemoveAccents.setOnClickListener(KPTQuickSettingsViewManager.this);

			//disable themeboard in landscape mode or not purchased the feature
			//disable other options if trial expired
			/*if (mResources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ) {
				customizeView.setCompoundDrawablesWithIntrinsicBounds(null, null, null,mResources.getDrawable(R.drawable.keyboard_customization_disable));
				customizeView.setTextColor(Color.GRAY);
			} else if(licenceExpired) {
				themesView.setTextColor(Color.GRAY);
				customizeView.setTextColor(Color.GRAY);
				
				themesView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, 
						mResources.getDrawable(R.drawable.keyboard_customization_disable));
				customizeView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, 
						mResources.getDrawable(R.drawable.keyboard_customization_disable));
				
				themesView.setClickable(false);
				customizeView.setClickable(false);
				tvPvtMode.setClickable(false);
				tvATROptions.setClickable(false);
				tvRemoveAccents.setClickable(false);
			}else{
				customizeView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, 
						mResources.getDrawable(R.drawable.keyboard_customization));
				//customizeView.setTextColor(Color.WHITE);
			}
*/
			setDynamicDrawables();


		}

		@Override
		public void onWindowFocusChanged(boolean hasFocus) {
			// Once focus is lost due to orientation change or application switch
			// dismiss the dialog.
			if(!hasFocus) {
				this.dismiss();
			}
		}

		/**
		 * Used to change the drawables dynamically in the quicksettings view
		 * @param kptAdaptxtIME
		 */

		private void setDynamicDrawables()
		{
			TextView tvPrivateMode;
			//TextView tvAutoCorrection;
			TextView tvATROptions;
			LinearLayout parent;
			TextView tvRemoveAccents;
			

			tvPrivateMode = (TextView) this.findViewById(R.id.quick_sett_prvtmode);
			//tvAutoCorrection = (TextView) quickSettingsDialog.findViewById(R.id.quick_sett_autocorrection);
			tvATROptions = (TextView) this.findViewById(R.id.quick_sett_atr_dialog);
			parent = (LinearLayout) this.findViewById(R.id.llParent);
			
			
			tvRemoveAccents = (TextView) findViewById(R.id.quick_sett_rmvaccents);
			
			/*if( mKptAdaptxtIME != null ){
				Drawable drawable = mContext.getResources().getDrawable(R.drawable.remove_accents_icon_disable);
				tvRemoveAccents.setTextColor(mContext.getResources().getColor(R.color.kpt_atr_option));
				tvRemoveAccents.setCompoundDrawablesWithIntrinsicBounds(null, null, null, drawable);
				//tvRemoveAccents.setClickable(false);*/			
			
			if( mKptAdaptxtIME != null ){
				Drawable drawable = mContext.getResources().getDrawable(R.drawable.kpt_remove_accents_icon);
				tvRemoveAccents.setCompoundDrawablesWithIntrinsicBounds(null, null, null, drawable);
				tvRemoveAccents.setTextColor(moldColors);
				tvRemoveAccents.setClickable(true);
			}

			int totalWidth = mContext.getResources().getDisplayMetrics().widthPixels;
			int currWidth = (totalWidth * 75)/100;

			parent.setLayoutParams( new FrameLayout.LayoutParams(currWidth,
					LayoutParams.FILL_PARENT));
			
				if(!sharedPreferences.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false))	{
					Drawable drawable = mContext.getResources().getDrawable(R.drawable.kpt_privatemode_icon);
					tvPrivateMode.setCompoundDrawablesWithIntrinsicBounds(null, null, null, drawable);
					tvPrivateMode.setTextColor(Color.GRAY);
					tvPrivateMode.setText(mContext.getResources().getString(R.string.kpt_UI_STRING_QUICKSETTINGS_ITEMS_2_II_7003));
					tvPrivateMode.setTextColor(moldColors);
				}
				else{
					Drawable drawable = mContext.getResources().getDrawable(R.drawable.kpt_privatemode_icon_on);
					tvPrivateMode.setCompoundDrawablesWithIntrinsicBounds(null, null, null, drawable);
					tvPrivateMode.setTextColor(Color.GREEN);
					tvPrivateMode.setText(mContext.getResources().getString(R.string.kpt_UI_STRING_QUICKSETTINGS_ITEMS_2_I_7003));
				}
			
			/*if(!mSharedPreferences.getBoolean(KPTConstants.PREF_AUTOCORRECTION, false))	{
				Drawable drawable = mContext.getResources().getDrawable(R.drawable.autocorrection_icon_off);
				tvAutoCorrection.setCompoundDrawablesWithIntrinsicBounds(null, null, null, drawable);
				tvAutoCorrection.setTextColor(Color.GRAY);
				tvAutoCorrection.setText(mContext.getResources().getString(R.string.kpt_UI_STRING_COMMON_5_OFF));
				tvAutoCorrection.setTextColor(moldColors);
			}
			else{
				Drawable drawable = mContext.getResources().getDrawable(R.drawable.autocorrection_icon_on);
				tvAutoCorrection.setCompoundDrawablesWithIntrinsicBounds(null, null, null, drawable);
				tvAutoCorrection.setTextColor(Color.GREEN);
				tvAutoCorrection.setText(mContext.getResources().getString(R.string.kpt_UI_STRING_COMMON_5_ON));
			}*/

			if ( !(mSharedPreferences.getBoolean(KPTConstants.PREF_ATR_FEATURE,
					true))
					|| mSharedPreferences.getBoolean(
							KPTConstants.PREF_CORE_MAINTENANCE_MODE, false)) {
				Drawable drawable = mContext.getResources().getDrawable(R.drawable.kpt_atr_options_disable);
				tvATROptions.setCompoundDrawablesWithIntrinsicBounds(null, null, null, drawable);
				tvATROptions.setTextColor(mContext.getResources().getColor(R.color.kpt_atr_option));
			}
			else{
				Drawable drawable = mContext.getResources().getDrawable(R.drawable.kpt_atr_options_enable);
				tvATROptions.setCompoundDrawablesWithIntrinsicBounds(null, null, null, drawable);
				tvATROptions.setTextColor(moldColors);
			}


		}
	}


	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.quick_sett_layouts) {
			quickSettingsDialog.dismiss();
			showThemeChangeMenu();
		} else if (id == R.id.quick_sett_prvtmode) {
			quickSettingsDialog.dismiss();
			mKptAdaptxtIME.showDialogForPrivateMode();
		} else if (id == R.id.quick_sett_rmvaccents) {
			/*if (mKptAdaptxtIME != null ){
				Toast.makeText(mContext, R.string.kpt_UI_STRING_TOAST_MESSAGE_1_2001, Toast.LENGTH_LONG).show();
				return;
			}*/
			String str;
			if (mKptAdaptxtIME.getExtractedTextLength() > 0) {
				str = mContext.getResources().getString(R.string.kpt_UI_STRING_TOAST_2_7003);
				mKptAdaptxtIME.trimAccents();
			} else {
				str = mContext.getResources().getString(R.string.kpt_UI_STRING_TOAST_1_7003);
			}

			Toast.makeText(mContext, str, Toast.LENGTH_LONG).show();
			quickSettingsDialog.dismiss();
		} else if (id == R.id.quick_sett_settings) {
			quickSettingsDialog.dismiss();

			launchAdaptxtSettings();
		} else if (id == R.id.quick_sett_atr_dialog) {
			if (mSharedPreferences.getBoolean(KPTConstants.PREF_ATR_FEATURE, true)
					&& (!(mSharedPreferences.getBoolean(KPTConstants.PREF_CORE_MAINTENANCE_MODE, false))))
			{
				mKptAdaptxtIME.launchATRListView();
			}
			quickSettingsDialog.dismiss();
		} /*else if (id == R.id.quick_sett_customize) {
			if (mResources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				Toast.makeText(
						mContext,
						mContext.getResources().getString(
								R.string.kpt_UI_STRING_TOAST_5_110),
								Toast.LENGTH_LONG)
								.show();
				return;
			}
			quickSettingsDialog.dismiss();
			displayCustomizationDialog();
		}*/
	}



	/**
	 * launch adaptxt main settings
	 */
	public void launchAdaptxtSettings() {
		//mKptAdaptxtIME.launchSettings();
		//CustomKeyboard.hideKeyboard();
		
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP); // should use this flag for
		// starting an activity from
		// Service
		intent.setClass(mContext, KPTAdaptxtIMESettings.class);
		mContext.startActivity(intent);
	}

	/**
	 * Dismiss the quick setting View
	 */
	public void dissMissQuickSettingDialog()
	{
		if(quickSettingsDialog!=null && quickSettingsDialog.isShowing())
			quickSettingsDialog.dismiss();
		if(mOptionsDialog != null && mOptionsDialog.isShowing())
			mOptionsDialog.dismiss();
	}

	/*public void dismissCustomizationDialog() {
		if(mCustomizeDialog != null && mCustomizeDialog.isShowing()) {
			mCustomizeDialog.dismissExitDialog();
			mCustomizeDialog.dismiss();
		}
	}*/



	public void setKPTAdaptxtIME(AdaptxtIME kptAdaptxtIME) {
		// TODO Auto-generated method stub
		mKptAdaptxtIME = kptAdaptxtIME;
	}


	public void displayQuickSettingsDialog() {
		quickSettingsDialog = new QuickSettingsDialog(mContext);
		quickSettingsDialog.show();
	}

	/**
	 * The ImageArrayAdapter is the array adapter used for displaying an additional
	 * image to a list preference item.
	 * @author Casper Wakkers
	 */
	public class QuicksettingsOptionAdapter extends BaseAdapter {

		private Drawable[] icons;
		private String themeName;
		private LayoutInflater mLayoutInflator;
		private int defaultThemesCount;
		private int selectedDefaultTheme;
		
		private ArrayList<String> themesList;
		
		public QuicksettingsOptionAdapter(Context context) {
			
			mLayoutInflator = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
			
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
			selectedDefaultTheme = Integer.parseInt(sp.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
			themeName = getSelectedThemeName(selectedDefaultTheme);
			
			themesList =  kptDB.getAllThemeNames();//getAllInstalledThemeNames();
			for(int i=0;i<3;i++){
				themesList.remove(0);
			}
			String[] internalThemes = mResources.getStringArray(R.array.kpt_theme_options_entries);
			defaultThemesCount = internalThemes.length;
			for (int i = 0; i < internalThemes.length; i++) {
				themesList.add(i, internalThemes[i]);
			}
			icons = getDrawables();
		}
		
		/**
		 * get all themes installed in Default, external and custom themes order
		 * 
		 * Fix to TP 15494 case 1
		 */
		public ArrayList<String> getAllInstalledThemeNames() {
			
			ArrayList<String> allInstalledThemes = new ArrayList<String>();
			
			//first add all default themes based on current language
			String[] internalThemes = mResources.getStringArray(R.array.kpt_theme_options_entries);
			defaultThemesCount = internalThemes.length;
			for (int i = 0; i < internalThemes.length; i++) {
				allInstalledThemes.add(internalThemes[i]);
			}
			
			//add all external themes
			ArrayList<KPTThemeItem> externalThemelist = kptDB.getAllExternalThemes();
			for (int i = 0; i < externalThemelist.size(); i++) {
				allInstalledThemes.add(externalThemelist.get(i).themeName);
			}

			//add all custom themes
			ArrayList<String> customtheme = kptDB.getAllCustomThemeNames();
			allInstalledThemes.addAll(customtheme);
			
			/*for (int i = 0; i < allInstalledThemes.size(); i++) {
				Log.e("kpt","all theme -->"+allInstalledThemes.get(i));
			}*/
			
			return allInstalledThemes;
		}
		
		@Override
		public int getCount() {
			return themesList.size();
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
			checkedTextView.setChecked(false);
			
			checkedTextView.setText(themesList.get(position));
			
			if(position > defaultThemesCount) {
				if(themesList.get(position).equalsIgnoreCase(themeName)){
					checkedTextView.setChecked(true);
				}
			} else {
				if (selectedDefaultTheme == position) {
					checkedTextView.setChecked(true);
				}
			}
			
			/*if(position > 2){
				checkedTextView.setText(titles[position]);
				
				if (checkedTextView.getText().toString().equals(themeName)) {
					checkedTextView.setChecked(true);
				}
			}else{
				checkedTextView.setText(defaultNames[position]);
				
				if (currentThemeId == position) {
					checkedTextView.setChecked(true);
				}
			}*/
			return row;
		}
	}

	private int[] resourceIds = null;
	String[] imageNames = null;
	public void prepareResourceIds(boolean customThemeAdded) {

		imageNames = mResources.getStringArray(R.array.kpt_theme_image_entries);

		if(customThemeAdded){
			resourceIds = new int[imageNames.length + 1];
		} else {
			resourceIds = new int[imageNames.length];
		}

		for (int i=0;i<imageNames.length;i++) {
			String imageName = imageNames[i].substring(
					imageNames[i].indexOf('/') + 1,
					imageNames[i].lastIndexOf('.'));
			resourceIds[i] = mResources.getIdentifier(imageName,null, mContext.getPackageName());
		}
	}

	private void saveTheme(int pos) {
		SharedPreferences.Editor spEdit = mSharedPreferences.edit();
		spEdit.putString(KPTConstants.PREF_THEME_MODE, pos+"");
		spEdit.commit();


		if(mKptAdaptxtIME != null){
			mKptAdaptxtIME.updateTheme();
			mKptAdaptxtIME.mInputView.changeKeyboardBG();
			//mKptAdaptxtIME.mTheme.loadTheme(pos);
			//mKptAdaptxtIME.mInputView.loadtheme();
			mKptAdaptxtIME.mCandidateViewContainer.updateCandidateBar();
			//mKptAdaptxtIME.updateSpaceKeyHighlight();
			mKptAdaptxtIME.mInputView.invalidateAllKeys();
		}
	}
	Drawable[] mThemeImages;
	String[] mThemeNames;


	public void showThemeChangeMenu() {
		mThemeDialog = new ThemeDialog(mContext,android.R.style.Theme_DeviceDefault_Dialog);
		mThemeDialog.show();
	}

	private int getThemeId(String themeName){
		return kptDB.getThemeId(themeName);

	}

	private String[] getThemeNames(int themeEnabled){
		ArrayList<String> namesList = kptDB.getThemeNames(themeEnabled);

		String[] namesArray = new String[namesList.size()];
		for(int i = 0; i < namesList.size(); i++){
			namesArray[i] = namesList.get(i);
		}

		return namesArray;
	}

	private String[] getThemeIDs(){
		String [] str_themeID = new String [kptDB.getNumberOfThemes()];
		int[] theme_ID = kptDB.getAllThemeIDs();
		for (int i = 0; i < str_themeID.length; i++) {
			str_themeID[i] = Integer.valueOf(theme_ID[i]).toString(); 
		}
		return str_themeID; 
	}

	private Drawable[] getDrawables(){
		return kptDB.getAllThemeDrawable();

	}
	
	private String getSelectedThemeName(int currentThemeId) {
		String themeName = null;
		try{
			themeName = kptDB.getCurThemeName(currentThemeId);
			
		}catch (SQLiteException e) {
			e.printStackTrace();
		}
		return themeName;
	}

	public void setDbObject(KPTAdaptxtDBHandler kptDBObj) {
		this.kptDB = kptDBObj;
	}
	
	/**
	 * Fix for 11491
	 */
	class ThemeDialog extends Dialog {

		public ThemeDialog(Context context, int themeCustomdialog) {
			super(context,themeCustomdialog);
			mContext = context;
			
		}
		
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			if( mKptAdaptxtIME.mInputView.getWindowToken() == null){
				return; 
			}
			

			final int currentThemeId;
	

			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
			currentThemeId = Integer.parseInt(sp.getString(KPTConstants.PREF_THEME_MODE, KPTConstants.DEFAULT_THEME+""));
			//String themeName = getSelectedThemeName(currentThemeId);

			requestWindowFeature(Window.FEATURE_LEFT_ICON);
			this.setTitle(mResources.getString(R.string.kpt_theme_title));
			setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.kpt_adaptxt_launcher_icon);
			
			mThemeImages = getDrawables();
			mThemeNames = getThemeNames(KPTConstants.THEME_ENABLE);

			ListView dialoglayout = (ListView) View.inflate(mContext, R.layout.kpt_themes_layout , null );
			/*final QuicksettingsOptionAdapter listAdapter = new QuicksettingsOptionAdapter(mContext,
					R.layout.theme_list_item, mThemeNames, 
					mThemeImages, themeName, defaultThemes);*/
			
			final QuicksettingsOptionAdapter listAdapter = new QuicksettingsOptionAdapter(mContext);
			dialoglayout.setAdapter(listAdapter);
			
			this.setContentView(dialoglayout);

			dialoglayout.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View view, int pos,
						long arg3) {
					dismiss();
					if(pos > 4){
						CheckedTextView chk = (CheckedTextView) view.findViewById(R.id.radioButton);
						String selectedThemeName = chk.getText().toString();

						int rowId = getThemeId(selectedThemeName);
						
						if(currentThemeId != rowId){
							saveTheme(rowId);
						}
					}else{
						if(pos != currentThemeId){
							saveTheme(pos);
						}
					}
					
					listAdapter.notifyDataSetChanged();
				}
			});
			
			Window window = this.getWindow();
			WindowManager.LayoutParams lp = window.getAttributes();
			lp.token = mKptAdaptxtIME.mInputView.getWindowToken();
			lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
			lp.flags = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
			
		    
			window.setAttributes(lp);
			window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
			
			this.setCanceledOnTouchOutside(true);
		}
		
		@Override
		public void onWindowFocusChanged(boolean hasFocus) {
			// Once focus is lost due to orientation change or application switch
			// dismiss the dialog.
			if(!hasFocus) {
				this.dismiss();
			}
		}
	}
}
