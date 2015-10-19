package com.bsb.hike.ui;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.modules.kpt.DictionaryManager;
import com.bsb.hike.utils.ChangeProfileImageBaseActivity;
import com.kpt.adaptxt.beta.KPTAddonItem;

public class LanguageSettingsActivity extends ChangeProfileImageBaseActivity implements Listener, OnItemClickListener
{

	Context mContext;

	ArrayAdapter<KPTAddonItem> addonItemAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_language_settings);
		setupActionBar();
		mContext = this;
		setupLanguageList();
		addToPubSub();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText("languages");
		actionBar.setCustomView(actionBarView);
		Toolbar parent = (Toolbar) actionBarView.getParent();
		parent.setContentInsetsAbsolute(0, 0);
	}

	protected void addToPubSub()
	{
		String[] mPubSubListeners = new String[] { HikePubSub.KPT_LANGUAGES_UPDATED };
		HikeMessengerApp.getPubSub().addListeners(this, mPubSubListeners);
	}

	private void setupLanguageList()
	{
		ArrayList<KPTAddonItem> addonItems = DictionaryManager.getInstance(this).getLanguagesList();
		addonItemAdapter = new ArrayAdapter<KPTAddonItem>(this, R.layout.setting_item, R.id.item, addonItems)
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = getLayoutInflater().inflate(R.layout.setting_item, null);
				}
				KPTAddonItem item = getItem(position);
				TextView textLang = (TextView) convertView.findViewById(R.id.item);
				textLang.setText(item.getDisplayName() + " (" + DictionaryManager.getInstance(mContext).getDictionaryLanguageStatus(item).toString() + " )");

				return convertView;
			}

			@Override
			public int getItemViewType(int position)
			{
				return -1;
			}

			@Override
			public int getViewTypeCount()
			{
				return 1;
			}

		};

		ListView langList = (ListView) findViewById(R.id.lang_list);
		langList.setAdapter(addonItemAdapter);
		langList.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		KPTAddonItem item = addonItemAdapter.getItem(position);
		DictionaryManager.getInstance(mContext).downloadAndInstallLanguage(item);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		switch (type)
		{
		case HikePubSub.KPT_LANGUAGES_UPDATED:
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					addonItemAdapter.notifyDataSetChanged();
				}
			});
			break;
		}
	}

}
