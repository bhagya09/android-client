package com.bsb.hike.chatHead;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class StickerShareSettings extends HikeAppStateBaseFragmentActivity
{

	public static class ListViewItem
	{
		String appName;

		boolean appChoice;

		Drawable appIcon;

		String pkgName;

		CheckBox mCheckBox;
	}

	private static ArrayList<ListViewItem> mListViewItems;
	
	private static TextView click2Accessibility;

	private static SwitchCompat selectAllCheckbox;
	
	private static ChatHeadSettingsArrayAdapter listAdapter;
	
	private static final String TAG = "StickerShareSettings";  
	
	static boolean isSelectAllTouched = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_sticker_share);
		initComponents();
	}

	private void initComponents()
	{
		creatingArrayList();
		listAdapter = new ChatHeadSettingsArrayAdapter(this, R.layout.settings_sticker_share_item, mListViewItems);
		selectAllCheckbox = (SwitchCompat) findViewById(R.id.select_all_checkbox);
		click2Accessibility = (TextView) findViewById(R.id.show_accessibility);
		settingOnClickEvent();
		settingSelectAllText();
	
		ListView listView = (ListView) findViewById(R.id.list_items);
		listView.setAdapter(listAdapter);
		setupActionBar();
	}

	@Override
	protected void onResume()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SHOW_ACCESSIBILITY, false) && !ChatHeadUtils.isAccessibilityEnabled(this))
		{
			click2Accessibility.setVisibility(View.VISIBLE);
		}
		else
		{
			click2Accessibility.setVisibility(View.GONE);
		}
		super.onResume();
	}

	private void settingOnClickEvent()
	{
		LinearLayout layout = (LinearLayout) findViewById(R.id.main_select_layout);
		layout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onSelectAllCheckboxClick();
			}
		});
		click2Accessibility.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SHOW_ACCESSIBILITY, true))
				{
					Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
					startActivityForResult(intent, 0);
				}
			}
		});
		selectAllCheckbox.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				isSelectAllTouched = true;
				return false;
			}
		});
		selectAllCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if(isSelectAllTouched)
				{
					isSelectAllTouched = false;
					onSelectAllCheckboxClick();
				}
			}
		});
	}

	private void creatingArrayList()
	{
		mListViewItems = new ArrayList<ListViewItem>();
		try
		{
			JSONArray jsonObj = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, ""));

			for (int i = 0; i < jsonObj.length(); i++)
			{
				JSONObject obj = jsonObj.getJSONObject(i);

				if (Utils.isPackageInstalled(getApplicationContext(), obj.optString(HikeConstants.ChatHead.PACKAGE_NAME, "")))
				{
					ListViewItem listItem = new ListViewItem();
					listItem.appName = obj.optString(HikeConstants.ChatHead.APP_NAME, null);
					listItem.appChoice = obj.optBoolean(HikeConstants.ChatHead.APP_ENABLE, false);
					listItem.pkgName = obj.optString(HikeConstants.ChatHead.PACKAGE_NAME, null);
					try
					{
						listItem.appIcon = getPackageManager().getApplicationIcon(listItem.pkgName);
					}
					catch (NameNotFoundException e)
					{
						Logger.d(TAG, "appicon not found ");
					}
					if (listItem.appName!= null && listItem.pkgName!= null && listItem.appIcon!= null)
					{
						mListViewItems.add(listItem);
				    }
				}
			}
		}
		catch (JSONException e)
		{
			Logger.d(TAG, "json exception");
		}
	}

	private void onSelectAllCheckboxClick()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SHOW_ACCESSIBILITY, true) && !ChatHeadUtils.isAccessibilityEnabled(this))
		{
			HikeDialogFactory.showDialog(StickerShareSettings.this, HikeDialogFactory.ACCESSIBILITY_DIALOG, new HikeDialogListener()
			{
				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
					startActivityForResult(intent, 0);
				}
				@Override
				public void neutralClicked(HikeDialog hikeDialog)
				{}
				@Override
				public void negativeClicked(HikeDialog hikeDialog)
				{
					hikeDialog.dismiss();
				}
			}, 2);
		}
		else
		{
			enalbleSelectAll();
		}
	}
	
	public void enalbleSelectAll()
	{
		boolean allChecked = areAllItemsCheckedOrUnchecked(true);
		if (allChecked)
		{
			for (int j = 0; j < mListViewItems.size(); j++)
			{
				mListViewItems.get(j).appChoice = false;
			}
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.SELECT_ALL, AnalyticsConstants.ChatHeadEvents.APP_UNCHECKED);

		}
		else
		{
			for (int j = 0; j < mListViewItems.size(); j++)
			{
				mListViewItems.get(j).appChoice = true;
			}
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, false);

			HikeAlarmManager.cancelAlarm(this, HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE);
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.SELECT_ALL, AnalyticsConstants.ChatHeadEvents.APP_CHECKED);
		}

		settingSelectAllText();
	}

	private static void settingSelectAllText()
	{
		selectAllCheckbox.setChecked(areAllItemsCheckedOrUnchecked(true));
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.CHAT_HEAD_USR_CONTROL, !areAllItemsCheckedOrUnchecked(false));
		listAdapter.notifyDataSetChanged();
		savingUserPref();
	}

	static boolean areAllItemsCheckedOrUnchecked(boolean allItemsExpectedChecked)
	{
		// if all list items are expected checked, we return false if even one list item is unchecked, and vice versa
		for (int j = 0; j < mListViewItems.size(); j++)
		{
			boolean thisPackageIsChecked = mListViewItems.get(j).appChoice;

			if (allItemsExpectedChecked ^ thisPackageIsChecked)
			{
				return false;
			}
		}
		return true;
	}

	public static void onItemClickEvent(int tag)
	{

		if (mListViewItems.get(tag).appChoice)
		{
			mListViewItems.get(tag).appChoice = false;
			mListViewItems.get(tag).mCheckBox.setChecked(false);
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.APP_CLICK, mListViewItems.get(tag).appName,
					AnalyticsConstants.ChatHeadEvents.APP_UNCHECKED);
		}
		else
		{

			mListViewItems.get(tag).appChoice = true;
			mListViewItems.get(tag).mCheckBox.setChecked(true);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, false);
			HikeAlarmManager.cancelAlarm(HikeMessengerApp.getInstance(), HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE);
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.APP_CLICK, mListViewItems.get(tag).appName,
					AnalyticsConstants.ChatHeadEvents.APP_CHECKED);
		}

		settingSelectAllText();

	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();

		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.settings_share_stickers);
		actionBar.setCustomView(actionBarView);
	}

	private static void savingUserPref()
	{
		JSONArray jsonObj;
		try
		{
			jsonObj = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, ""));
			int j = 0;
			JSONObject obj;
			for (int i = 0; i < mListViewItems.size(); i++)
			{
				obj = jsonObj.getJSONObject(j);
				while (!mListViewItems.get(i).pkgName.equals(obj.optString(HikeConstants.ChatHead.PACKAGE_NAME, "")))
				{
					j++;
					obj = jsonObj.getJSONObject(j);
				}
				if (mListViewItems.get(i).pkgName.equals(obj.optString(HikeConstants.ChatHead.PACKAGE_NAME, "")))
				{
					obj.put(HikeConstants.ChatHead.APP_ENABLE, mListViewItems.get(i).appChoice);
					j++;
				}
			}
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.PACKAGE_LIST, jsonObj.toString());
		}
		catch (JSONException e1)
		{
			Logger.d(TAG, "json Exception");
		}

	}
	
	
	@Override
	protected void onStop()
	{
		ChatHeadUtils.startOrStopService(true);
		super.onStop();
	}
}
