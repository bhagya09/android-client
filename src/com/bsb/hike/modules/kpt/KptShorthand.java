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

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontEditText;
import com.kpt.adaptxt.beta.AdaptxtSettings;
import com.kpt.adaptxt.beta.AdaptxtSettingsRegisterListener;
import com.kpt.adaptxt.beta.CustomKeyboard;
import com.kpt.adaptxt.beta.KPTAdaptxtAddonSettings;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtEditTextEventListner;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtKeyboordVisibilityStatusListner;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author anubansal
 *
 */
public class KptShorthand extends HikeAppStateBaseFragmentActivity implements AdaptxtSettingsRegisterListener, ATRDeleteListener,
		AdaptxtEditTextEventListner, AdaptxtKeyboordVisibilityStatusListner, OnClickListener
{
	private CustomKeyboard mCustomKeyboard;
	
	private boolean systemKeyboard;
	
	KPTAdaptxtAddonSettings kptSettings;

	boolean mCoreEngineStatus;

	ShorthandAdapter mAdapter;
	
	ArrayList<String> shortcutList;
	
	ArrayList<String> expansionList;
	
	ListView mListView;
	
	Button addBtn;
	
	CustomFontEditText shortcutEt;
	
	CustomFontEditText expansionEt;
	
	
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
		
		shortcutEt = (CustomFontEditText) findViewById(R.id.shortcut_et);
		shortcutEt.setFocusable(true);
		expansionEt = (CustomFontEditText) findViewById(R.id.expansion_et);
		expansionEt.setFocusable(true);
		addBtn = (Button) findViewById(R.id.add_shorthand_btn);
		mAdapter = new ShorthandAdapter(this, shortcutList, expansionList, mListView, this);
		mListView = (ListView) findViewById(R.id.atr_list);
		mListView.setAdapter(mAdapter);
		
		addOnClickListeners();
		initCustomKeyboard();
		
	}
	
	private void addOnClickListeners()
	{
		findViewById(R.id.add_shorthand_btn).setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.add_shorthand_btn:
			String shortcut = shortcutEt.getText().toString();
			String expansion = expansionEt.getText().toString();
			int atrStatus = kptSettings.addATRShortcut(shortcut, expansion);
			if (checkErrorStatus(atrStatus))
			{
				shortcutList.add(shortcut);
				expansionList.add(expansion);
				mAdapter.notifyDataSetChanged();
				shortcutEt.setText("");
				mCustomKeyboard.updateCore();
				shortcutEt.requestFocus();
			}
			break;
		
		default:
			break;
		}
	}
	
	private void showKeyboard(CustomFontEditText editText)
	{
		if (systemKeyboard)
		{
			Utils.showSoftKeyboard(KptShorthand.this, editText);
		}
		else
		{
			mCustomKeyboard.showCustomKeyboard(editText, true);
		}
	}
	
	private void hideKeyboard(CustomFontEditText editText)
	{
		if (systemKeyboard)
		{
			Utils.hideSoftKeyboard(KptShorthand.this, editText);
		}
		else
		{
			mCustomKeyboard.showCustomKeyboard(editText, false);
		}
	}
	
	private void initCustomKeyboard()
	{
		View keyboardHolder = (LinearLayout) findViewById(R.id.keyboardView_holder);
		mCustomKeyboard = new CustomKeyboard(KptShorthand.this, keyboardHolder);
		systemKeyboard = HikeMessengerApp.isSystemKeyboard(KptShorthand.this);
		mCustomKeyboard.registerEditText(R.id.shortcut_et, KPTConstants.MULTILINE_LINE_EDITOR, this, this);
		mCustomKeyboard.registerEditText(R.id.expansion_et, KPTConstants.MULTILINE_LINE_EDITOR, this, this);
		mCustomKeyboard.init(shortcutEt);
		mCustomKeyboard.showCustomKeyboard(shortcutEt, true);
		
		if (systemKeyboard)
		{
			mCustomKeyboard.showCustomKeyboard(shortcutEt, false);
			mCustomKeyboard.swtichToDefaultKeyboard(shortcutEt);
			mCustomKeyboard.unregister(R.id.shortcut_et);
			mCustomKeyboard.showCustomKeyboard(expansionEt, false);
			mCustomKeyboard.swtichToDefaultKeyboard(expansionEt);
			mCustomKeyboard.unregister(R.id.expansion_et);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			Utils.showSoftKeyboard(shortcutEt, InputMethodManager.SHOW_FORCED);
		}
		
		shortcutEt.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				showKeyboard(shortcutEt);
			}
		});
		
		expansionEt.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				showKeyboard(expansionEt);
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
		if (mCustomKeyboard != null && mCustomKeyboard.isCustomKeyboardVisible())
		{
			mCustomKeyboard.showCustomKeyboard(shortcutEt, false);
			mCustomKeyboard.showCustomKeyboard(expansionEt, false);
			return;
		}
		super.onBackPressed();
		
		Intent shorthandIntent = new Intent(this, HikePreferences.class);
		setResult(RESULT_OK, shorthandIntent);
		finish();
	}
	
	@Override
	protected void onDestroy()
	{
		KptUtils.destroyKeyboardResources(mCustomKeyboard, R.id.shortcut_et, R.id.expansion_et);
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

	@Override
	public void analyticalData(String currentLanguage)
	{
		KptUtils.generateKeyboardAnalytics(currentLanguage);
	}

	@Override
	public void onInputViewCreated()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInputviewVisbility(boolean arg0, int arg1)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showGlobeKeyView()
	{
		KptUtils.onGlobeKeyPressed(KptShorthand.this, mCustomKeyboard);
	}

	@Override
	public void showQuickSettingView()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAdaptxtFocusChange(View arg0, boolean arg1)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAdaptxtTouch(View arg0, MotionEvent arg1)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAdaptxtclick(View arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReturnAction(int arg0)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void onPause()
	{
		KptUtils.pauseKeyboardResources(mCustomKeyboard, shortcutEt, expansionEt);
		super.onPause();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		mCustomKeyboard.onConfigurationChanged(newConfig);
		super.onConfigurationChanged(newConfig);
	}
	
//	deleteatr (shortcut) 
//	deleteallatr 

}
