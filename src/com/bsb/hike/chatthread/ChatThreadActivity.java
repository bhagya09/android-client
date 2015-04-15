package com.bsb.hike.chatthread;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;

public class ChatThreadActivity extends HikeAppStateBaseFragmentActivity
{

	private ChatThread chatThread;
	
	private static final String TAG = "ChatThreadActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Logger.i(TAG, "OnCreate");
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);

		if (filter(getIntent()))
		{
			init(getIntent());
			chatThread.onCreate();
			showProductPopup(ProductPopupsConstants.PopupTriggerPoints.CHAT_SCR.ordinal());
		}
		else
		{
			closeChatThread();
		}
	}

	private boolean filter(Intent intent)
	{
		String msisdn = getIntent().getStringExtra(HikeConstants.Extras.MSISDN);
		if (HikeMessengerApp.isStealthMsisdn(msisdn)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF) != HikeConstants.STEALTH_ON)
		{
			return false;
		}
		return true;
	}
	
	private void closeChatThread()
	{
		Intent homeintent = IntentFactory.getHomeActivityIntent(this);
		this.startActivity(homeintent);
		this.finish();
	}

	private void init(Intent intent)
	{
		String whichChatThread = intent.getStringExtra(HikeConstants.Extras.WHICH_CHAT_THREAD);
		if (HikeConstants.Extras.ONE_TO_ONE_CHAT_THREAD.equals(whichChatThread))
		{
			chatThread = new OneToOneChatThread(this, intent.getStringExtra(HikeConstants.Extras.MSISDN));
		}
		else if (HikeConstants.Extras.GROUP_CHAT_THREAD.equals(whichChatThread))
		{
			chatThread = new GroupChatThread(this, intent.getStringExtra(HikeConstants.Extras.MSISDN));
		}
		else if (HikeConstants.Extras.BROADCAST_CHAT_THREAD.equals(whichChatThread))
		{
			chatThread = new BroadcastChatThread(this, intent.getStringExtra(HikeConstants.Extras.MSISDN));
		}
		else
		{
			throw new IllegalArgumentException("Which chat thread I am !!! Did you pass proper arguments?");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		Logger.i(TAG, "OnCreate Options Menu Called");
		return chatThread.onCreateOptionsMenu(menu) ? true : super.onCreateOptionsMenu(menu);

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{

		return chatThread.onPrepareOptionsMenu(menu) ? true : super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		return chatThread.onOptionsItemSelected(item) ? true : super.onOptionsItemSelected(item);
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		Logger.i(TAG, "OnNew Intent called");
		super.onNewIntent(intent);
		if(processNewIntent(intent))
		{
			chatThread.onPreNewIntent();
			init(intent);
			setIntent(intent);
			chatThread.onNewIntent();
		}
		else
		{
			setIntent(intent);
			chatThread.takeActionBasedOnIntent();
		}
	}
	
	private boolean processNewIntent(Intent intent)
	{
		/**
		 * Exit condition #1. We are in the same Chat Thread.
		 */
		String oldMsisdn = getIntent().getStringExtra(HikeConstants.Extras.MSISDN);
		String newMsisdn = intent.getStringExtra(HikeConstants.Extras.MSISDN);
		
		if(oldMsisdn.equals(newMsisdn))
		{
			return false;
		}
		
		/**
		 * Exit condition #2. We are in stealth chat without being in stealth mode
		 */
		return filter(intent);
				
	}

	@Override
	public void onBackPressed()
	{
		if (chatThread.onBackPressed())
		{
			return;
		}
		
		backPressed();
	}
	
	public void backPressed()
	{
		Intent intent = IntentFactory.getHomeActivityIntent(this);
		startActivity(intent);
		super.onBackPressed();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		chatThread.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onDestroy()
	{
		Logger.i(TAG, "OnDestroy");
		/**
		 * It could be possible that we enter a stealth chat and we intend to close it from the filter() method. Hence the null check
		 */
		if (chatThread != null)
		{
			chatThread.onDestroy();
		}
		super.onDestroy();
	}
	
	@Override
	protected void onPause()
	{
		Logger.i(TAG, "OnPause");
		chatThread.onPause();
		super.onPause();
	}
	
	@Override
	protected void onResume()
	{
		Logger.i(TAG, "OnResume");
		super.onResume();
		chatThread.onResume();
	}
	
	@Override
	protected void onRestart()
	{
		Logger.i(TAG, "OnRestart");
		chatThread.onRestart();
		super.onRestart();
	}
	
	@Override
	protected void onStop()
	{
		Logger.i(TAG, "OnStop");
		chatThread.onStop();
		super.onStop();
	}
	
	@Override
	protected void onStart()
	{
		Logger.i(TAG, "On Start");
		chatThread.onStart();
		super.onStart();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		Logger.i(TAG, "OnConfigchanged");
		chatThread.onConfigurationChanged(newConfig);
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onAttachFragment(android.support.v4.app.Fragment fragment)
	{
		Logger.i(TAG, "onAttachFragment");
		chatThread.onAttachFragment();
		super.onAttachFragment(fragment);
	}
	
	public String getContactNumber()
	{
		return chatThread.getContactNumber();
	}
	
	@Override
	public void showProductPopup(int which)
	{
		super.showProductPopup(which);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (Build.VERSION.SDK_INT <= 10 || (Build.VERSION.SDK_INT >= 14 && ViewConfiguration.get(this).hasPermanentMenuKey()))
		{
			if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU)
			{
				/*
				 * For some reason the activity randomly catches this event in the background and we get an NPE when that happens with mMenu. Adding an NPE guard for that.
				 * if media viewer is open don't do anything
				 */
				if (isFragmentAdded(HikeConstants.IMAGE_FRAGMENT_TAG))
				{
					return super.onKeyUp(keyCode, event);
				}
				
				chatThread.onMenuKeyPressed();
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	
}
