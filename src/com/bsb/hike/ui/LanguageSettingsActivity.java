package com.bsb.hike.ui;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.DictionaryLanguageAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.modules.kpt.KptKeyboardManager;
import com.bsb.hike.utils.ChangeProfileImageBaseActivity;
import com.bsb.hike.utils.Logger;
import com.kpt.adaptxt.beta.KPTAddonItem;

public class LanguageSettingsActivity extends ChangeProfileImageBaseActivity implements Listener, OnItemClickListener, KptKeyboardManager.KptLanguageInstallListener
{

	Context mContext;

	DictionaryLanguageAdapter addonItemAdapter;

	ArrayList<KPTAddonItem> addonItems;

	private String[] mPubSubListeners = new String[] { HikePubSub.KPT_LANGUAGES_UPDATED };
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_language_settings);
		setupActionBar();
		mContext = this;
		setupLanguageList();
		addToPubSub();
		KptKeyboardManager.getInstance().setInstallListener(this);
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.language);
		actionBar.setCustomView(actionBarView);
		Toolbar parent = (Toolbar) actionBarView.getParent();
		parent.setContentInsetsAbsolute(0, 0);
	}

	protected void addToPubSub()
	{
		HikeMessengerApp.getPubSub().addListeners(this, mPubSubListeners);
	}

	private void setupLanguageList()
	{
		addonItems = new ArrayList<KPTAddonItem>();
		addonItemAdapter = new DictionaryLanguageAdapter(this, R.layout.kpt_dictionary_language_list_item, addonItems);
		ListView langList = (ListView) findViewById(R.id.lang_list);
		langList.setAdapter(addonItemAdapter);
		langList.setOnItemClickListener(this);
		refreshLanguageList();
	}

	private void refreshLanguageList()
	{
		addonItems.clear();
		addonItems.addAll(KptKeyboardManager.getInstance().getSupportedLanguagesList());
		addonItems.addAll(KptKeyboardManager.getInstance().getUnsupportedLanguagesList());
		addonItemAdapter.notifyDataSetChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		KPTAddonItem item = addonItemAdapter.getItem(position);
		KptKeyboardManager.LanguageDictionarySatus status = KptKeyboardManager.getInstance().getDictionaryLanguageStatus(item);
		if (status == KptKeyboardManager.LanguageDictionarySatus.UNINSTALLED)
		{
			KptKeyboardManager.getInstance().downloadAndInstallLanguage(item, HikeConstants.KEYBOARD_LANG_DWNLD_SETTINGS);
			
		}
		else if (status == KptKeyboardManager.LanguageDictionarySatus.INSTALLED_LOADED)
		{
			KptKeyboardManager.getInstance().unloadInstalledLanguage(item);
		}
		else if (status == KptKeyboardManager.LanguageDictionarySatus.INSTALLED_UNLOADED)
		{
			KptKeyboardManager.getInstance().loadInstalledLanguage(item);
		}
		else if (status == KptKeyboardManager.LanguageDictionarySatus.UNSUPPORTED)
		{
			Toast.makeText(mContext, R.string.unsupported_language_toast_msg, Toast.LENGTH_SHORT).show();
		}
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
					refreshLanguageList();
				}
			});
			break;
		}
	}

	@Override
	public void onError(KPTAddonItem item, final String message)
	{
		this.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void onSuccess(KPTAddonItem item) {

	}

	@Override
	protected void onDestroy()
	{
		KptKeyboardManager.getInstance().setInstallListener(null);
		// Mempry leak pub sub no destroyed
		removePubSubs();
		super.onDestroy();
	}

	public void removePubSubs()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, mPubSubListeners);
	}
}
