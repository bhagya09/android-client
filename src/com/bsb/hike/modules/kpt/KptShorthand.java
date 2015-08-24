/**
 * 
 */
package com.bsb.hike.modules.kpt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.bsb.hike.R;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.kpt.adaptxt.beta.AdaptxtSettings;
import com.kpt.adaptxt.beta.AdaptxtSettingsRegisterListener;
import com.kpt.adaptxt.beta.KPTAdaptxtAddonSettings;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author anubansal
 *
 */
public class KptShorthand extends HikeAppStateBaseFragmentActivity implements AdaptxtSettingsRegisterListener, ATRDeleteListener
{
	KPTAdaptxtAddonSettings kptSettings;

	private ProgressDialog mProgressDialog;

	boolean mCoreEngineStatus;

	ShorthandAdapter mAdapter;
	
	ArrayList<String> shortcutList;
	
	ArrayList<String> expansionList;
	
	ListView mListView;
	
	Button addBtn;
	
	EditText shortcutEt;
	
	EditText expansionEt;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.keyboard_shorthand);
		setupActionBar();
		
		kptSettings = new KPTAdaptxtAddonSettings(this, this);

		if(mCoreEngineStatus)
		{
			callAddonServices();
		}
		
		shortcutList = new ArrayList<String>();
		expansionList = new ArrayList<String>();
		HashMap<String, String> ATRMap = kptSettings.getATRList();
		Set<Entry<String, String>> entrySet  = ATRMap.entrySet();
		Iterator<Entry<String, String>> mapIterator = entrySet.iterator();
		while (mapIterator.hasNext())
		{
			Map.Entry<String, String> mapEntry = (Entry<String, String>) mapIterator.next();
			String key = (String) mapEntry.getKey();
			String value = (String) mapEntry.getValue();
			shortcutList.add(key);
			expansionList.add(value);
		}
		
		shortcutEt = (EditText) findViewById(R.id.shortcut_et);
		expansionEt = (EditText) findViewById(R.id.expansion_et);
		addBtn = (Button) findViewById(R.id.add_shorthand_btn);
		mAdapter = new ShorthandAdapter(this, shortcutList, expansionList, mListView, this);
		mListView = (ListView) findViewById(R.id.atr_list);
		mListView.setAdapter(mAdapter);
		
		addBtn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String shortcut = shortcutEt.getText().toString();
				String expansion = expansionEt.getText().toString();
				int atrStatus = kptSettings.addATRShortcut(shortcut, expansion);
				if (checkErrorStatus(atrStatus))
				{
					shortcutList.add(shortcut);
					expansionList.add(expansion);
					mAdapter.notifyDataSetChanged();
				}
			}
		});
		
	}
	
	private boolean checkErrorStatus(int atrStatus)
	{
		switch (atrStatus) {
		case AdaptxtSettings.KPT_SUCCESS:
			Logger.e("KPT", "-----------> ATR SHORTCUT ADDED SUCCESFULLY <------------");
			return true;
		case AdaptxtSettings.ATR_ERROR_EXPANSION_SHORT:
			Toast.makeText(this, "ATR EXPANSION is LESS than 3", Toast.LENGTH_LONG).show();
			Logger.e("KPT", "-----------> ATR EXPANSION is LESS than 3 <------------");
			return false;
		case AdaptxtSettings.ATR_ERROR_SHORTCUT_SHORT:
			Toast.makeText(this, "ATR SHORTCUT is LESS than 3", Toast.LENGTH_LONG).show();
			Logger.e("KPT", "-----------> ATR SHORTCUT is LESS than 3 <------------");
			return false;
		case AdaptxtSettings.ATR_ERROR_SHORTCUT_LONG:
			Toast.makeText(this, "ATR SHORTCUT is GREATER than 3", Toast.LENGTH_LONG).show();
			Logger.e("KPT", "-----------> ATR SHORTCUT is GREATER than 3 <------------");
			return false;
		case AdaptxtSettings.ATR_ERROR_NULL:
			Toast.makeText(this, "ATR OR EXPANSION is NULL", Toast.LENGTH_LONG).show();
			Logger.e("KPT", "-----------> ATR OR EXPANSION is NULL <------------");
			return false;
		}
		return false;
	}
	
	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setDisplayHomeAsUpEnabled(true);
		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);
		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.shorthand_title);
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.blue_hike));
		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
//		parent.setContentInsetsAbsolute(0,0);
		invalidateOptionsMenu();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.shorthand_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		super.onOptionsItemSelected(item);
		
		switch (item.getItemId())
		{
		case R.id.overflow_del_shortcut:
			kptSettings.deleteAllATR();
			shortcutList.clear();
			expansionList.clear();
			mAdapter.notifyDataSetChanged();
			break;
			
		default:
			break;
		}
		return true;
	}
	
	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		
		Intent shorthandIntent = new Intent(this, HikePreferences.class);
		setResult(RESULT_OK, shorthandIntent);
		finish();
	}
	
	@Override
	protected void onDestroy()
	{
		kptSettings.saveUserContext();
		kptSettings.destroySettings();
		Intent shorthandIntent = new Intent(this, HikePreferences.class);
		setResult(RESULT_OK, shorthandIntent);
		finish();
		super.onDestroy();
	}

	@Override
	public void coreEngineService()
	{
		if(mCoreEngineStatus)
		{
			callAddonServices();
		}		
	}

	@Override
	public void coreEngineStatus(boolean coreStatus)
	{
		if (coreStatus)
		{
			mCoreEngineStatus = coreStatus;
			Logger.e("KPT", "--------------> core engine connected -----> " + coreStatus);
		}
		else
		{
			mCoreEngineStatus = coreStatus;
			Logger.e("KPT", "--------------> core engine init start -----> " + coreStatus);
		}		
	}
	
	private void callAddonServices(){

		HashMap<String, String> atrMap = kptSettings.getATRList();


		for (HashMap.Entry<String, String> entry : atrMap.entrySet()){
			Logger.e("KPT", entry.getKey() + "/" + entry.getValue());
		}

		int atrStatus = kptSettings.addATRShortcut("HRU", "How are you?");

		// Intimate the user here for errors
		switch (atrStatus) {
		case AdaptxtSettings.KPT_SUCCESS:
			Logger.e("KPT", "-----------> ATR SHORTCUT ADDED SUCCESFULLY <------------");
			break;
		case AdaptxtSettings.ATR_ERROR_EXPANSION_SHORT:
			Logger.e("KPT", "-----------> ATR EXPANSION is LESS than 3 <------------");
			break;
		case AdaptxtSettings.ATR_ERROR_SHORTCUT_SHORT:
			Logger.e("KPT", "-----------> ATR SHORTCUT is LESS than 3 <------------");
			break;
		case AdaptxtSettings.ATR_ERROR_SHORTCUT_LONG:
			Logger.e("KPT", "-----------> ATR SHORTCUT is GREATER than 3 <------------");
			break;
		case AdaptxtSettings.ATR_ERROR_NULL:
			Logger.e("KPT", "-----------> ATR OR EXPANSION is NULL <------------");
			break;


		default:
			break;
		}
	}

	@Override
	public void atrDeleted(String shortcut)
	{
		kptSettings.deleteATR(shortcut);
	}

//	deleteatr (shortcut) 
//	deleteallatr 

}
