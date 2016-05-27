package com.bsb.hike.chatthread;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;

public class ChatThreadActivity extends HikeAppStateBaseFragmentActivity
{

	private ChatThread chatThread;
	
	private long lastMessageTimeStamp;
	
	private static final String TAG = "ChatThreadActivity";

	public static final String CHAT_THREAD_SOURCE = "ct_source";

	public static final class ChatThreadOpenSources
	{
		public static final int UNKNOWN = 0;

		public static final int NOTIF = 1;

		public static final int CONV_FRAGMENT = 2;

		public static final int NEW_COMPOSE = 3;

		public static final int SHORTCUT = 4;

		public static final int FORWARD = 5;

		public static final int EMPTY_STATE_CONV_FRAGMENT = 6;

		public static final int FRIENDS_SCREEN = 7;

		public static final int UNSAVED_CONTACT_CLICK = 8;

		public static final int FILE_SHARING = 9;

		public static final int PROFILE_SCREEN = 10;

		public static final int OFFLINE = 11;

		public static final int STICKEY_CALLER = 12;

		public static final int VOIP = 13;

		public static final int LIKES_DIALOG = 14;

		public static final int TIMELINE = 15;

		public static final int NEW_GROUP = 16;

		public static final int MICRO_APP = 17;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Logger.i(TAG, "OnCreate");
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		if (Utils.isLollipopOrHigher() && getWindow() != null)
		{
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		}
		/**
		 * force the user into the reg-flow process if the token isn't set
		 */
		if (Utils.requireAuth(this))
		{
			/**
			 * To avoid super Not Called exception
			 */
			super.onCreate(savedInstanceState);
			return;
		}

		if (filter(getIntent()))
		{
			init(getIntent());
		}
		
		// Activity should be created first in order to access action bar from chatthread.oncreate
		super.onCreate(savedInstanceState);
		if (filter(getIntent()))
		{
			chatThread.onCreate(savedInstanceState);
			showProductPopup(ProductPopupsConstants.PopupTriggerPoints.CHAT_SCR.ordinal());
		}
		else
		{
			closeChatThread(null);
		}
	}

	private boolean filter(Intent intent)
	{
		String msisdn = intent.getStringExtra(HikeConstants.Extras.MSISDN);
		
		/**
		 * Possibly Chat Thread is being invoked from outside the application
		 */
		
		if (TextUtils.isEmpty(msisdn))
		{
			msisdn = ChatThreadUtils.getMsisdnFromSendToIntent(intent);
			if (TextUtils.isEmpty(msisdn))
			{
				return false;
			}
			Logger.d(TAG, "Got msisdn from outside chat thread. msisdn is : " + msisdn);
			intent.putExtra(HikeConstants.Extras.WHICH_CHAT_THREAD, HikeConstants.Extras.ONE_TO_ONE_CHAT_THREAD);
			intent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		}
		
		if (StealthModeManager.getInstance().isStealthMsisdn(msisdn) && !StealthModeManager.getInstance().isActive())
		{
			/**
			 * If Birthday feature is enabled, then for case
			 * Hidden mode was off, Notification was generated for Birthday for Hidden contact
			 * Then Hidden mode is on and chat is hidden and then on tapping notification
			 * Bounce Hike logo + close 1-1 Chat + open Home Screen
			 */
			if (Utils.isBDayInNewChatEnabled() && intent.hasExtra(HikeConstants.Extras.BIRTHDAY_NOTIF))
			{
				if (PreferenceManager.getDefaultSharedPreferences(ChatThreadActivity.this).getBoolean(HikeConstants.STEALTH_INDICATOR_ENABLED, false))
				{
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STEALTH_INDICATOR_SHOW_REPEATED, true);
					HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_INDICATOR, null);
					return false;
				}
			}
			return false;
		}
		
		/**
		 * Possibly opening a deleted bot ?
		 */
		if (HikeConstants.Extras.BOT_CHAT_THREAD.equals(intent.getStringExtra(HikeConstants.Extras.WHICH_CHAT_THREAD)))
		{
			if (null == BotUtils.getBotInfoForBotMsisdn(msisdn))
			{
				return false;
			}
		}
		return true;
	}
	
	public void closeChatThread(String msisdn)
	{
		Intent homeintent = IntentFactory.getHomeActivityIntent(this);
		if(msisdn != null)
		{
			homeintent.putExtra(HikeConstants.STEALTH_MSISDN, msisdn);
		}
		this.startActivity(homeintent);
		this.finish();
	}

	private void init(Intent intent)
	{
		String whichChatThread = intent.getStringExtra(HikeConstants.Extras.WHICH_CHAT_THREAD);
		lastMessageTimeStamp = intent.getLongExtra(HikeConstants.Extras.LAST_MESSAGE_TIMESTAMP, 0);
		
		if (HikeConstants.Extras.ONE_TO_ONE_CHAT_THREAD.equals(whichChatThread))
		{
			chatThread = new OneToOneChatThread(this, intent.getStringExtra(HikeConstants.Extras.MSISDN));
		}
		else if (HikeConstants.Extras.GROUP_CHAT_THREAD.equals(whichChatThread))
		{
			chatThread = new GroupChatThread(this, intent.getStringExtra(HikeConstants.Extras.MSISDN),  intent.getBooleanExtra(HikeConstants.Extras.NEW_GROUP, false));
		}
		else if (HikeConstants.Extras.BROADCAST_CHAT_THREAD.equals(whichChatThread))
		{
			chatThread = new BroadcastChatThread(this, intent.getStringExtra(HikeConstants.Extras.MSISDN));
		}
		
		else if(HikeConstants.Extras.BOT_CHAT_THREAD.equals(whichChatThread))
		{
			chatThread = new BotChatThread(this, intent.getStringExtra(HikeConstants.Extras.MSISDN));
		}
		
		else
		{
			throw new IllegalArgumentException("Which chat thread I am !!! Did you pass proper arguments?");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{	
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
			chatThread.tryToDestroyChatThread();
			init(intent);
			setIntent(intent);
			chatThread.onNewIntent();
		}
		else
		{
			setIntent(intent);
			if(chatThread != null)
			{
				chatThread.dismissResidualAcitonMode();
				chatThread.takeActionBasedOnIntent();
				/**
				 * Scrolling to bottom in case same chat is opened from onNewIntent
				 */
				chatThread.scrollToEnd();
			}
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
		try
		{
			// In some phones keyboard is not closing on press of Action bar back button
			// try catch just for safe side
			Utils.hideSoftKeyboard(getApplicationContext(), getWindow().getDecorView());
		}
		catch (Exception e)
		{
		}
		IntentFactory.openHomeActivity(ChatThreadActivity.this, true);
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
			chatThread.tryToDestroyChatThread();
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
		if (chatThread != null)
		{
			chatThread.onAttachFragment(fragment);
		}
		
		else
		{
			Logger.wtf(TAG, "Chat Thread obj is null! We are attaching a ghost fragment!!");
		}
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
	
	public long getLastMessageTimeStamp()
	{
		return this.lastMessageTimeStamp;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		if (chatThread != null)
		{
			chatThread.onSaveInstanceState(outState);
		}
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		if(chatThread != null) 
		{
			chatThread.onRestoreInstanceState(savedInstanceState);
		}
		super.onRestoreInstanceState(savedInstanceState);
		
	}
	@Override
	protected void setStatusBarColor(Window window, String color) {
		// TODO Auto-generated method stub
		//Nothing to be done with status bar
		return;
	}
	
	public boolean isWalkieTalkieShowing(){
		if(chatThread != null) {
			return chatThread.isWalkieTalkieShowing();
		}
		return false;
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		Logger.i(TAG, "onPostResume");
		if (chatThread != null) {
			chatThread.onPostResume();
		}
	}

	public void recordMediaShareEvent(String uniqueKey_order, String genus, String family){
		if(chatThread != null){
			chatThread.recordMediaShareAnalyticEvent(uniqueKey_order, genus, family);
		}
	}

	protected void recordActivityEndTime()
	{
		super.recordActivityEndTime();
	}
}
