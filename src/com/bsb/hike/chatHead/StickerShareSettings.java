package com.bsb.hike.chatHead;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class StickerShareSettings extends HikeAppStateBaseFragmentActivity
{

	private class ListViewItem
	{
		String appName;

		boolean appChoice;

		Drawable appIcon;

		String pkgName;

		CheckBox mCheckBox;
	}

	private ArrayList<ListViewItem> mListViewItems;

	private ArrayAdapter<ListViewItem> listAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings_sticker_share);

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
					listItem.appName = obj.optString(HikeConstants.ChatHead.APP_NAME, "");
					listItem.appChoice = obj.optBoolean(HikeConstants.ChatHead.APP_ENABLE, false);
					listItem.pkgName = obj.optString(HikeConstants.ChatHead.PACKAGE_NAME, "");
					try
					{
						listItem.appIcon = getPackageManager().getApplicationIcon(listItem.pkgName);
					}
					catch (NameNotFoundException e)
					{
						e.printStackTrace();
					}
					mListViewItems.add(listItem);
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		listAdapter = new ArrayAdapter<ListViewItem>(this, R.layout.settings_sticker_share_item, mListViewItems)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				ListViewItem listItem = new ListViewItem();
				listItem = mListViewItems.get(position);
				if (convertView == null)
				{
					LayoutInflater inflater = getLayoutInflater();
					convertView = inflater.inflate(R.layout.settings_sticker_share_item, null);
				}
				ImageView imgView = (ImageView) convertView.findViewById(R.id.app_icon);
				TextView txtView = (TextView) convertView.findViewById(R.id.app_name);
				mListViewItems.get(position).mCheckBox = (CheckBox) convertView.findViewById(R.id.checkbox_item);
				imgView.setBackground(listItem.appIcon);
				txtView.setText(listItem.appName);
				if (listItem.appChoice)
				{
					mListViewItems.get(position).mCheckBox.setChecked(true);
				}
				else
				{
					mListViewItems.get(position).mCheckBox.setChecked(false);

				}
				convertView.setTag(position);
				mListViewItems.get(position).mCheckBox.setTag(position);
				mListViewItems.get(position).mCheckBox.setOnClickListener(new View.OnClickListener()
				{

					@Override
					public void onClick(View v)
					{
						onItemClickEvent((int) v.getTag());
					}
				});

				convertView.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						onItemClickEvent((int) v.getTag());
					}
				});

				return convertView;
			}

		};

		LinearLayout layout = (LinearLayout) findViewById(R.id.main_select_layout);
		layout.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onSelectAllCheckboxClick();
			}
		});
		CheckBox checkBox = (CheckBox) findViewById(R.id.select_all_checkbox);
		checkBox.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onSelectAllCheckboxClick();
			}
		});
		settingSelectAllText();
		ListView listView = (ListView) findViewById(R.id.list_items);
		listView.setAdapter(listAdapter);
		setupActionBar();
	}

	private void onSelectAllCheckboxClick()
	{
		boolean allChecked = checkAllChecked();
		if (allChecked)
		{
			for (int j = 0; j < mListViewItems.size(); j++)
			{
				mListViewItems.get(j).appChoice = false;
			}
			HAManager.getInstance().chatHeadshareAnalytics(HikeConstants.ChatHead.SELECT_ALL, HikeConstants.ChatHead.APP_UNCHECKED);

		}
		else
		{
			for (int j = 0; j < mListViewItems.size(); j++)
			{
				mListViewItems.get(j).appChoice = true;
			}
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, false);

			HikeAlarmManager.cancelAlarm(this, HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE);
			HAManager.getInstance().chatHeadshareAnalytics(HikeConstants.ChatHead.SELECT_ALL, HikeConstants.ChatHead.APP_CHECKED);
		}

		settingSelectAllText();

	}

	private void settingSelectAllText()
	{
		boolean allChecked, allUnchecked;

		allChecked = checkAllChecked();
		allUnchecked = checkAllUnchecked();
		if (allChecked)
		{
			TextView txtView = (TextView) findViewById(R.id.main_text_select_all);
			txtView.setText(getString(R.string.settings_deselect_all));
			TextView tv = (TextView) findViewById(R.id.side_text_select_all);
			tv.setText(getString(R.string.sticker_widget_hide));
			CheckBox selectAllCheckbox = (CheckBox) findViewById(R.id.select_all_checkbox);
			selectAllCheckbox.setChecked(true);
		}
		else
		{
			TextView txtView = (TextView) findViewById(R.id.main_text_select_all);
			txtView.setText(getString(R.string.settings_select_all));

			TextView tv = (TextView) findViewById(R.id.side_text_select_all);
			tv.setText(getString(R.string.sticker_widget_show));

			CheckBox selectAllCheckbox = (CheckBox) findViewById(R.id.select_all_checkbox);
			selectAllCheckbox.setChecked(false);

		}
		if (allUnchecked)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.CHAT_HEAD_USR_CONTROL, false);
		}
		else
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.CHAT_HEAD_USR_CONTROL, true);
		}

		listAdapter.notifyDataSetChanged();
		savingUserPref();
		ChatHeadUtils.serviceDecision(this, true);
	}

	boolean checkAllUnchecked()
	{
		for (int j = 0; j < mListViewItems.size(); j++)
		{
			boolean pkgEnbl = false;
			pkgEnbl = (boolean) mListViewItems.get(j).appChoice;

			if (pkgEnbl)
			{
				return false;
			}
		}
		return true;
	}

	boolean checkAllChecked()
	{
		for (int j = 0; j < mListViewItems.size(); j++)
		{
			boolean pkgEnbl = true;

			pkgEnbl = (boolean) mListViewItems.get(j).appChoice;

			if (!pkgEnbl)
			{
				return false;
			}
		}
		return true;
	}

	private void onItemClickEvent(int tag)
	{

		if (mListViewItems.get(tag).appChoice)
		{
			mListViewItems.get(tag).appChoice = false;
			mListViewItems.get(tag).mCheckBox.setChecked(false);
			HAManager.getInstance().chatHeadshareAnalytics(HikeConstants.ChatHead.APP_CLICK, mListViewItems.get(tag).appName, HikeConstants.ChatHead.APP_UNCHECKED);
		}
		else
		{

			mListViewItems.get(tag).appChoice = true;
			mListViewItems.get(tag).mCheckBox.setChecked(true);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, false);
			HikeAlarmManager.cancelAlarm(this, HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE);
			HAManager.getInstance().chatHeadshareAnalytics(HikeConstants.ChatHead.APP_CLICK, mListViewItems.get(tag).appName, HikeConstants.ChatHead.APP_CHECKED);
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

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.settings_share_stickers);
		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	private void savingUserPref()
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
			e1.printStackTrace();
		}

	}

}
