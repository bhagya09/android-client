package com.kpt.adaptxt.beta.glide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.settings.KPTAddATRShortcut;

public class KPTGlideCustomGesturesActivity extends Activity {

	private ListView mShortcutsListView;
	private AlertDialog mAlertDialog;

	private final int MAX_CUSTOM_GESTURES_COUNT = 255;
	public static final String PREF_LAST_SELECTED_CUSTOM_GESTURE_OPTION = "last_custom_gesture_type";

	public static final int SHORTCUT_TYPE_APPLICATION = 0;
	public static final int SHORTCUT_TYPE_WEBSITE = 1;
	//public static final int SHORTCUT_TYPE_CLIPBOARD = 2;

	public static final int CLIPBOARD_ACTION_CUT = 0;
	public static final int CLIPBOARD_ACTION_COPY = 1;
	public static final int CLIPBOARD_ACTION_PASTE = 2;
	public static final int CLIPBOARD_ACTION_SELECTALL = 3;
	/*public static final int CLIPBOARD_ACTION_SELECT = 4;*/
	public static final int CLIPBOARD_ACTION_HIDE_KEYBOARD = 4;
	public static final int CLIPBOARD_ACTION_BEGI_MSG = 5;
	public static final int CLIPBOARD_ACTION_END_MSG = 6;
	
	private int mCurrentSelectedShortcutType;

	private KPTCustomGesturePreferences mCustomGesturePreference;
	private ShortcutsListAdapter mShortcutsAdapter;

	private String mSelectedApplicationPackageName;
	private String mSelectedApplicationName;

	private String mShortCutToDelete;
	private ArrayList<AppInfo> mInstalledAppsInfo;
	
	private String mCurrentEditingKeycode;
	
	private InstalledAppsInfo mInstalledAppsTask;
	private ProgressDialog mAppsLoadingDialog;
	private ViewHolder mViewHolder;
	
	private boolean mIsInEditCustomGesture = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Spinner customGestureOptionsSpinner;
		final ImageView addCustomGesture;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){

			RelativeLayout headerView = (RelativeLayout) View.inflate(KPTGlideCustomGesturesActivity.this, 
					R.layout.kpt_custom_gestures_title, null);
			LinearLayout bodyView = (LinearLayout) View.inflate(KPTGlideCustomGesturesActivity.this, 
					R.layout.kpt_custom_gestures_body, null);

			ActionBar actionBar = getActionBar();
			if(actionBar != null) {
				actionBar.setCustomView(headerView);
				actionBar.setDisplayShowTitleEnabled(false);
				actionBar.setDisplayShowCustomEnabled(true);
				actionBar.setDisplayUseLogoEnabled(false);
				actionBar.setDisplayShowHomeEnabled(false);

				setContentView(bodyView);

				customGestureOptionsSpinner = (Spinner) headerView.findViewById(R.id.custom_gesture_types_spinner);
				addCustomGesture = (ImageView) headerView.findViewById(R.id.custom_gesture_create_new);
			} else {
				setContentView(R.layout.kpt_custom_gesture_below_ics);

				customGestureOptionsSpinner = (Spinner) findViewById(R.id.custom_gesture_types_spinner);
				addCustomGesture = (ImageView) findViewById(R.id.custom_gesture_create_new);
			}
		} else {
			setContentView(R.layout.kpt_custom_gesture_below_ics);

			customGestureOptionsSpinner = (Spinner) findViewById(R.id.custom_gesture_types_spinner);
			addCustomGesture = (ImageView) findViewById(R.id.custom_gesture_create_new);
		}
		
		
		String[] spinnerEntries = getResources().getStringArray(R.array.kpt_custom_gestures_options_items);
		String[] adjustEntries = new String[(spinnerEntries.length - 1)];
		
		for (int i = 0; i < adjustEntries.length; i++) {
			adjustEntries[i] = spinnerEntries[i];
		}
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
			    this, android.R.layout.simple_spinner_item, adjustEntries);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		customGestureOptionsSpinner.setAdapter(adapter);
		
		mInstalledAppsTask = new InstalledAppsInfo();
		mInstalledAppsTask.execute();
		
		mShortcutsListView = (ListView) findViewById(R.id.list_items);

		mCustomGesturePreference = KPTCustomGesturePreferences.getCustomGesturePreferences();
		mCustomGesturePreference.initializePreferences(KPTGlideCustomGesturesActivity.this);
		mCustomGesturePreference.loadDefaultPreferences();

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(KPTGlideCustomGesturesActivity.this);
		mCurrentSelectedShortcutType = pref.getInt(
				PREF_LAST_SELECTED_CUSTOM_GESTURE_OPTION, SHORTCUT_TYPE_APPLICATION);

		addCustomGesture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mViewHolder = null;
				mIsInEditCustomGesture = false;
				//displayAddEditCustomGetureDialog(false);
				
				if(mCurrentSelectedShortcutType == SHORTCUT_TYPE_APPLICATION && 
						mInstalledAppsTask != null && mInstalledAppsTask.getStatus() == Status.RUNNING) {
					mAppsLoadingDialog = ProgressDialog.show(KPTGlideCustomGesturesActivity.this, null, 
							getString(R.string.kpt_loading_data));
				} else {
					displayAddEditCustomGetureDialog(false);
				}
			}
		});

		mShortcutsAdapter = new ShortcutsListAdapter(KPTGlideCustomGesturesActivity.this); 
		mShortcutsListView.setAdapter(mShortcutsAdapter);
		mShortcutsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				mIsInEditCustomGesture = true;
				mViewHolder = ((ViewHolder)arg1.getTag());
				if(mCurrentSelectedShortcutType == SHORTCUT_TYPE_APPLICATION && 
						mInstalledAppsTask != null && mInstalledAppsTask.getStatus() == Status.RUNNING) {
					mAppsLoadingDialog = ProgressDialog.show(KPTGlideCustomGesturesActivity.this, null, 
							getString(R.string.kpt_loading_data));
				} else {
					displayAddEditCustomGetureDialog(true);
				}
			}
		});

		customGestureOptionsSpinner.setSelection(mCurrentSelectedShortcutType);
		customGestureOptionsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String selectedItem = customGestureOptionsSpinner.getSelectedItem().toString();
				String[] items = getResources().getStringArray(R.array.kpt_custom_gestures_options_items);
				List<String> list = Arrays.asList(items);
				mCurrentSelectedShortcutType = list.indexOf(selectedItem);
				mShortcutsAdapter.invalidate();

				pref.edit().putInt(PREF_LAST_SELECTED_CUSTOM_GESTURE_OPTION, mCurrentSelectedShortcutType).commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) { }
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.kpt_UI_STRING_MENUKEY_ITEMS_1_7003);
		menu.add(0, 1, 1, R.string.kpt_UI_STRING_MENUKEY_ITEMS_2_7003);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == 0) {
			mViewHolder = null;
			mIsInEditCustomGesture = false;
			if(mCurrentSelectedShortcutType == SHORTCUT_TYPE_APPLICATION && 
					mInstalledAppsTask != null && mInstalledAppsTask.getStatus() == Status.RUNNING) {
				mAppsLoadingDialog = ProgressDialog.show(KPTGlideCustomGesturesActivity.this, null, 
						getString(R.string.kpt_loading_data));
			} else {
				displayAddEditCustomGetureDialog(false);
			}
		} else {
			displayDeleteDialog(true);
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void displayAddEditCustomGetureDialog(final boolean editingShortcut){

		ViewHolder viewHolder = mViewHolder;
		if(mCustomGesturePreference.getTotalCreatedCustomGestures() >= MAX_CUSTOM_GESTURES_COUNT) {
			Toast.makeText(KPTGlideCustomGesturesActivity.this, getString(R.string.kpt_custom_gestures_max_limit_reached), Toast.LENGTH_LONG).show();
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(KPTGlideCustomGesturesActivity.this);
		builder.setTitle(getString(R.string.kpt_UI_STRING_SETTINGS_ITEM_2_220));
		LinearLayout dialogBody = (LinearLayout) View.inflate(KPTGlideCustomGesturesActivity.this, 
				R.layout.kpt_add_edit_custom_gesture_dialog_layout, null);

		final EditText unicodeEditText = (EditText) dialogBody.findViewById(R.id.custom_gesture_shortcut_char);
		final EditText urlEditText = (EditText) dialogBody.findViewById(R.id.custom_gesture_websites_clipboard_edittext);
		final TextView alreadyUsedTV = (TextView) dialogBody.findViewById(R.id.shortcut_already_exits_warning)	;
		final Spinner appsSpinner = (Spinner) dialogBody.findViewById(R.id.custom_gesture_applications_spinner);
		final TextView type = (TextView) dialogBody.findViewById(R.id.custom_gesture_shortcut_type);
		
		if(!editingShortcut) {
			mCurrentEditingKeycode = null;
			unicodeEditText.setEnabled(true);
			unicodeEditText.requestFocus();
		} else {
			//unicodeEditText.setEnabled(false);
			//unicodeEditText.setFocusable(false);
			mCurrentEditingKeycode = viewHolder.mDeleteButton.getContentDescription().toString();
			unicodeEditText.setText(viewHolder.mSubTitleview.getContentDescription().toString());
			
			switch (mCurrentSelectedShortcutType) {
			case SHORTCUT_TYPE_WEBSITE:
				urlEditText.setText(viewHolder.mTitleView.getText());
				break;
			}
		}
		
		InputFilter filter = new InputFilter() {
			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (!Character.isLetter(source.charAt(i))) {
						KPTAddATRShortcut.DISABLE_DOUBLE_SPACE_TAB = true;
						return "";
					}
				}
				return null;
			}
		};

		InputFilter[] FilterArray = new InputFilter[2];
		FilterArray[0] = new InputFilter.LengthFilter(1);
		FilterArray[1] = filter;
		unicodeEditText.setFilters(FilterArray);
		
		unicodeEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			@Override
			public void afterTextChanged(Editable s) {
				/**
				 * ok button should be enabled only when some text is entered in unicde and url edit texts
				 */
				Editable urlText = urlEditText.getText();
				if(s.length() == 0) {
					alreadyUsedTV.setVisibility(View.GONE);
					mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
				} else {
					/*if(editingShortcut) {
						mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
					} else {*/
						boolean result = mCustomGesturePreference.checkIfThisUnicodeIsAlreadyUsed(
								String.valueOf((int)unicodeEditText.getText().charAt(0)));
						if(result) {
							alreadyUsedTV.setVisibility(View.VISIBLE);
							mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
						} else {
							alreadyUsedTV.setVisibility(View.GONE);
							mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
						}
					//}	
				}
				
				if((mCurrentSelectedShortcutType == SHORTCUT_TYPE_WEBSITE
						&& urlText != null && urlText.toString().length() == 0)) {
					mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
				}
			}
		});

		urlEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			@Override
			public void afterTextChanged(Editable s) {
				
				boolean unicodeAlreadyUsed = false;
				Editable unicodeText = unicodeEditText.getText();
				if(unicodeText != null && unicodeText.toString().length() > 0) {
					unicodeAlreadyUsed = mCustomGesturePreference.checkIfThisUnicodeIsAlreadyUsed(
							String.valueOf((int)unicodeText.toString().charAt(0)));
				}
				
				if(s.length() == 0 || (unicodeText != null && unicodeText.toString().length() == 0)
						|| unicodeAlreadyUsed) {
					mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
				} else {
					mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
				}
			}
		});

		appsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				if(mCurrentSelectedShortcutType == SHORTCUT_TYPE_APPLICATION) {
					TextView tv = ((Holder)arg1.getTag()).appName;
					mSelectedApplicationName = tv.getText().toString();
					mSelectedApplicationPackageName = tv.getContentDescription().toString();
				}
				
				if(editingShortcut) {
					mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) { }
		});

		switch (mCurrentSelectedShortcutType) {
		case SHORTCUT_TYPE_APPLICATION:
			appsSpinner.setVisibility(View.VISIBLE);
			urlEditText.setVisibility(View.GONE);
			type.setText(R.string.kpt_dialog_gesture_type_app);

			appsSpinner.setAdapter(new ApplicationsSpinnerAdapter(KPTGlideCustomGesturesActivity.this));
			if(editingShortcut) {
				String appName = (String) viewHolder.mTitleView.getText();
				int index = -1;
				if(mInstalledAppsInfo != null) {
					int size = mInstalledAppsInfo.size();
					for (int i = 0; i < size; i++) {
						if(mInstalledAppsInfo.get(i).applicationName.equalsIgnoreCase(appName)) {
							index = i;
							break;
						}
					}
				}
				appsSpinner.setSelection(index);
			}
			break;
		case SHORTCUT_TYPE_WEBSITE:
			appsSpinner.setVisibility(View.GONE);
			urlEditText.setVisibility(View.VISIBLE);
			type.setText(R.string.kpt_dialog_gesture_type_website);
			break;
		}

		builder.setView(dialogBody);

		builder.setPositiveButton(R.string.kpt_alert_dialog_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String unicode = String.valueOf((int)unicodeEditText.getText().charAt(0));
				switch (mCurrentSelectedShortcutType) {
				case SHORTCUT_TYPE_APPLICATION:
					if(editingShortcut && mCurrentEditingKeycode != null) {
						mCustomGesturePreference.deleteApplicationPreference(mCurrentEditingKeycode);
					}
					mCustomGesturePreference.saveApplicationsPreference(unicode, 
							mSelectedApplicationName, mSelectedApplicationPackageName, editingShortcut);
					break;
				case SHORTCUT_TYPE_WEBSITE:
					if(editingShortcut && mCurrentEditingKeycode != null) {
						mCustomGesturePreference.deleteWebsitePreference(mCurrentEditingKeycode);
					}
					mCustomGesturePreference.saveWebsitePreference(unicode, urlEditText.getText().toString(), editingShortcut);
					break;
				}

				mShortcutsAdapter.invalidate();
				
				Toast.makeText(KPTGlideCustomGesturesActivity.this, getString(R.string.kpt_UI_STRING_TOAST_1_220), Toast.LENGTH_LONG).show();
			}
		});
		builder.setNegativeButton(R.string.kpt_cancel, null);

		mAlertDialog = builder.create();
		mAlertDialog.show();

		mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
	}

	private void displayDeleteDialog(final boolean isDeleteAllDialog) {
		AlertDialog.Builder builder = new AlertDialog.Builder(KPTGlideCustomGesturesActivity.this);
		if(isDeleteAllDialog) {
			builder.setTitle(getString(R.string.kpt_UI_STRING_DIALOG_TITLE_2_220));
			builder.setMessage(getString(R.string.kpt_UI_STRING_DIALOG_MESSAGE_2_220));
		} else {
			builder.setTitle(getString(R.string.kpt_UI_STRING_DIALOG_TITLE_1_220));
			builder.setMessage(getString(R.string.kpt_UI_STRING_DIALOG_MESSAGE_1_220));
		}
		
		builder.setPositiveButton(R.string.kpt_alert_dialog_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (mCurrentSelectedShortcutType) {
				case SHORTCUT_TYPE_APPLICATION:
					if(isDeleteAllDialog) {
						mCustomGesturePreference.deletePreferences(SHORTCUT_TYPE_APPLICATION);
					} else {
						mCustomGesturePreference.deleteApplicationPreference(mShortCutToDelete);
					}
					break;
				case SHORTCUT_TYPE_WEBSITE:
					if(isDeleteAllDialog) {
						mCustomGesturePreference.deletePreferences(SHORTCUT_TYPE_WEBSITE);
					} else {
						mCustomGesturePreference.deleteWebsitePreference(mShortCutToDelete);
					}
					break;
				}
				mShortcutsAdapter.invalidate();
			}
		});
		builder.setNegativeButton(R.string.kpt_cancel, null);
		
		builder.create().show();
	}
	
	/*private ArrayList<AppInfo> getInstalledAppsWithIcons() {
		ArrayList<AppInfo> res = new ArrayList<AppInfo>();
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> packs = getPackageManager().queryIntentActivities( mainIntent, 0);
		for(ResolveInfo rInfo : packs) {
			ActivityInfo aInfo = rInfo.activityInfo;
			AppInfo newInfo = new AppInfo();
			newInfo.applicationName = aInfo.applicationInfo.loadLabel(getPackageManager()).toString();
			newInfo.packageName = aInfo.packageName;
			newInfo.appIcon = rInfo.loadIcon(getPackageManager());
			res.add(newInfo);
		}
		return res;
	}*/

	class AppInfo {
		String applicationName;
		String packageName;
		Drawable appIcon;
	}

	private class ApplicationsSpinnerAdapter extends BaseAdapter {

		private Context mContext;
		public ApplicationsSpinnerAdapter(Context con) {
			mContext = con;
		}

		@Override
		public int getCount() {
			if(mInstalledAppsInfo != null) {
				return mInstalledAppsInfo.size();
			} else {
				return 0;
			}
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
			
			Holder holder;
			if(convertView == null) {
				holder = new Holder();

				convertView = View.inflate(mContext, R.layout.kpt_applications_icons_list, null);
				holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
				holder.appName = (TextView) convertView.findViewById(R.id.app_name);

				convertView.setTag(holder);
			} else {
				holder = (Holder) convertView.getTag();
			}	
			
			holder.appIcon.setImageDrawable(mInstalledAppsInfo.get(position).appIcon);
			holder.appName.setText(mInstalledAppsInfo.get(position).applicationName);
			holder.appName.setContentDescription(mInstalledAppsInfo.get(position).packageName);
			
			return convertView;
		}
	}

	private class ShortcutsListAdapter extends BaseAdapter {

		private Context mContext;
		private List<String[]> dataToDisplay = new ArrayList<String[]>();

		public ShortcutsListAdapter(Context context) {
			mContext = context;
			getData();
		}

		@SuppressWarnings("unchecked")
		private void getData() {
			Map<String, String> prefs = null;
			dataToDisplay.clear();

			switch (mCurrentSelectedShortcutType) {
			case SHORTCUT_TYPE_APPLICATION:
				prefs = (Map<String, String>) mCustomGesturePreference.getOnlyApplicationsPreferences();
				break;
			case SHORTCUT_TYPE_WEBSITE:
				prefs = (Map<String, String>) mCustomGesturePreference.getOnlyWebsitesPreferences();
				break;
			}

			if(prefs != null) {
				Iterator<String> keysIterator = prefs.keySet().iterator();
				String[] arr;
				while (keysIterator.hasNext()) {
					arr = new String[2];

					String key = keysIterator.next();
					String value = prefs.get(key);
					if(mCurrentSelectedShortcutType == SHORTCUT_TYPE_APPLICATION) {
						value = value.split(KPTCustomGesturePreferences.DELIMITER)[0];
					} 

					arr[0] = key;
					arr[1] = value;

					dataToDisplay.add(arr);
				}
			}
		}

		@Override
		public int getCount() {
			return dataToDisplay.size();
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
			ViewHolder viewHolder;
			if(convertView == null) {
				viewHolder = new ViewHolder();

				convertView = View.inflate(mContext, R.layout.kpt_shortcuts_display_list_item, null);
				viewHolder.mDeleteButton = (ImageView) convertView.findViewById(R.id.shortcut_delete);
				viewHolder.mTitleView = (TextView) convertView.findViewById(R.id.shortcut_title);
				viewHolder.mSubTitleview = (TextView) convertView.findViewById(R.id.shortcut_sub_text);

				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}

			viewHolder.mTitleView.setText(dataToDisplay.get(position)[1]);
			viewHolder.mSubTitleview.setText(getString(R.string.kpt_adaptxt_icon_to) + " " + (char)Integer.parseInt(dataToDisplay.get(position)[0]));
			viewHolder.mSubTitleview.setContentDescription(""+(char)Integer.parseInt(dataToDisplay.get(position)[0]));
			viewHolder.mDeleteButton.setContentDescription(dataToDisplay.get(position)[0]);

			viewHolder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mShortCutToDelete = v.getContentDescription().toString();
					displayDeleteDialog(false);
				}
			});
			return convertView;
		}

		private void invalidate() {
			getData();
			notifyDataSetChanged();
		}
	}

	class ViewHolder {
		ImageView mDeleteButton;
		TextView mTitleView;
		TextView mSubTitleview;
	}
	
	class Holder {
		ImageView appIcon;
		TextView appName;
	}
	
	class InstalledAppsInfo extends AsyncTask<Void, Void, Void> {
		
		private ArrayList<AppInfo> res;
		@Override
		protected void onPreExecute() {
			res = new ArrayList<AppInfo>();
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			
			/*try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
			
			final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			List<ResolveInfo> packs = getPackageManager().queryIntentActivities( mainIntent, 0);
			for(ResolveInfo rInfo : packs) {
				ActivityInfo aInfo = rInfo.activityInfo;
				AppInfo newInfo = new AppInfo();
				newInfo.applicationName = aInfo.applicationInfo.loadLabel(getPackageManager()).toString();
				newInfo.packageName = aInfo.packageName;
				newInfo.appIcon = rInfo.loadIcon(getPackageManager());
				res.add(newInfo);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			if(res != null && res.size() > 0) {
				mInstalledAppsInfo = new ArrayList<AppInfo>();
				mInstalledAppsInfo = res;
				if(mAppsLoadingDialog != null && mAppsLoadingDialog.isShowing()) {
					mAppsLoadingDialog.dismiss();
					displayAddEditCustomGetureDialog(mIsInEditCustomGesture);
				}
			}
		}
	}
}
