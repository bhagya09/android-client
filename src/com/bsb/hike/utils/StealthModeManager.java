package com.bsb.hike.utils;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.ui.utils.LockPattern;

/*
 * Reset mode is highly dependent on this singleTon, therefore it needs more asynchronous fault proofing.
 */
public class StealthModeManager
{
	
	enum States{
		SETUP_PENDING, ACTIVE, INACTIVE, FAKE_ACTIVE; 
	}
	private static int RESET_TOGGLE_TIME_MS =10 * 1000;

	private static final StealthModeManager stealthModeManager = new StealthModeManager();

	private HikeHandlerUtil handler;

	private Context context;

	private static Set<String> stealthMsisdn;
	
	private final String TAG =  "StealthModeManager";
	
	private int currentState;

	private StealthModeManager()
	{
		this.currentState = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
		this.handler = HikeHandlerUtil.getInstance();
		this.context = HikeMessengerApp.getInstance().getApplicationContext();
	}

	public static StealthModeManager getInstance()
	{
		return stealthModeManager;
	}
	
	public void initiate()
	{
		stealthMsisdn = new HashSet<String>();
		HikeConversationsDatabase.getInstance().addStealthMsisdnToMap();
	}
	
	public void addStealthMsisdnToMap(String msisdn)
	{
		stealthMsisdn.add(msisdn);
	}
	
	public void addNewStealthMsisdn(ConvInfo conv)
	{
		addStealthMsisdnToMap(conv.getMsisdn());
		HikeConversationsDatabase.getInstance().toggleStealth(conv.getMsisdn(), true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_CONVERSATION_MARKED, conv);
	}

	public void removeStealthMsisdn(ConvInfo conv)
	{
		removeStealthMsisdn(conv, true);
	}
	
	public boolean containsStealthMsisdn(String msisdn)
	{
		return stealthMsisdn.contains(msisdn);
	}
	
	public int getStealthMsisdnMapSize() {
		return stealthMsisdn.size();
	}

	public void removeStealthMsisdn(ConvInfo conv, boolean publishEvent)
	{
		stealthMsisdn.remove(conv.getMsisdn());
		if(publishEvent)
		{
			HikeConversationsDatabase.getInstance().toggleStealth(conv.getMsisdn(), false);
			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_CONVERSATION_UNMARKED, conv);
		}
	}

	public void clearStealthMsisdn()
	{
		stealthMsisdn.clear();
	}

	public boolean isStealthMsisdn(String msisdn)
	{
		return stealthMsisdn.contains(msisdn);
	}

	public void resetStealthToggle()
	{
		clearScheduledStealthToggleTimer();
		handler.postRunnableWithDelay(toggleReset, RESET_TOGGLE_TIME_MS);
	}

	public void clearScheduledStealthToggleTimer()
	{
		handler.removeRunnable(toggleReset);
	}

	private Runnable toggleReset = new Runnable()
	{

		@Override
		public void run()
		{
			activate(false);
			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, true);
			HikeMessengerApp.getPubSub().publish(HikePubSub.CLOSE_CURRENT_STEALTH_CHAT, true);
		}
	};
	
	public boolean isSetUp()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false);
	}
	
	public void setUp(boolean isSetUp)
	{
		 HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, isSetUp);
	}

	public boolean isActive()
	{
		return (currentState == HikeConstants.STEALTH_ON);
	}

	public void activate(boolean activate)
	{
		currentState = activate ? HikeConstants.STEALTH_ON : HikeConstants.STEALTH_OFF;
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE, currentState);
	}
	
	public void resetPreferences()
	{
		HikeSharedPreferenceUtil prefUtil = HikeSharedPreferenceUtil.getInstance();

		prefUtil.removeData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN);
		prefUtil.removeData(HikeMessengerApp.STEALTH_MODE);
		prefUtil.removeData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE);
		prefUtil.removeData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP);
		prefUtil.removeData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME);
		prefUtil.removeData(HikeMessengerApp.SHOWN_FIRST_UNMARK_STEALTH_TOAST);
	}

	public void toggleActionTriggered(Activity activity)
	{
		if(false);
	}
	
	public void toggleConversation(ConvInfo conv, Activity activity) 
 	{	
		HikeMessengerApp.getPubSub().publish(conv.isStealth()? HikePubSub.STEALTH_CONVERSATION_UNMARKED : HikePubSub.STEALTH_CONVERSATION_MARKED, conv);
	}
	
	public void usePinAsPassword(boolean usePin) 
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_PIN_AS_PASSWORD, usePin);
	}
	
	public boolean isPinAsPassword() 
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_PIN_AS_PASSWORD, false);
	}
}
