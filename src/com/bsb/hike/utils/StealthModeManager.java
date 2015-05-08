package com.bsb.hike.utils;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.Conversation.ConvInfo;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.utils.LockPattern;

/*
 * Reset mode is highly dependent on this singleTon, therefore it needs more asynchronous fault proofing.
 */
public class StealthModeManager
{
	
	private static final String DEFAULT_RESET_TOGGLE_TIME = "0";
	
	private static final String NEVER_RESET_TOGGLE_TIME = "-1";

	private static final StealthModeManager stealthModeManager = new StealthModeManager();

	private HikeHandlerUtil handler;

	private static Set<String> stealthMsisdn;
	
	private final String TAG =  "StealthModeManager";
	
	private int currentState;
	
	private static boolean inFakeHiddenMode = false;

	private StealthModeManager()
	{
		this.currentState = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
		this.handler = HikeHandlerUtil.getInstance();
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
		String stealthTimeOut = PreferenceManager.getDefaultSharedPreferences(HikeMessengerApp.getInstance().getApplicationContext()).getString(HikeConstants.CHANGE_STEALTH_TIMEOUT, DEFAULT_RESET_TOGGLE_TIME);
		inFakeHiddenMode = true;
		if(!stealthTimeOut.equals(NEVER_RESET_TOGGLE_TIME))
		{
			
			handler.postRunnableWithDelay(toggleReset, Long.parseLong(stealthTimeOut) * 1000);
		}
	}

	public void clearScheduledStealthToggleTimer()
	{
		inFakeHiddenMode = false;
		stealthFakeOn();
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
			inFakeHiddenMode = false;
		}
	};
	
	public boolean isSetUp()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false);
	}
	
	public void setUp(boolean isSetUp)
	{
		 HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, isSetUp);
		 if(!isSetUp)
		 {
			 activate(false);
		 }
	}
	
	private void stealthFakeOn()
	{
		if(isActive())
		{
			currentState = HikeConstants.STEALTH_ON_FAKE;
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE, currentState);
		}
	}
	
	public boolean isStealthFakeOn()
	{
		return (currentState == HikeConstants.STEALTH_ON_FAKE);
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
	
	public void settingupTriggered(Activity activity, boolean toggleVisibility)
	{
		//HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, !isActive() && toggleVisibility);
		
		if (!StealthModeManager.getInstance().isSetUp())
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE_FTUE_DONE, false);
			LockPattern.createNewPattern(activity, false, HikeConstants.ResultCodes.CREATE_LOCK_PATTERN_HIDE_CHAT);
		} 
		else if (!StealthModeManager.getInstance().isActive())
		{
			if(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE_FTUE_DONE, true))
			{
				LockPattern.confirmPattern(activity, false, HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN_HIDE_CHAT);
			}
			else
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_STEALTH_REVEAL_TIP, null);

				HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_FTUE_STEALTH_CONV, false);
			}
		}
			
	
	}

	public void toggleActionTriggered(Activity activity)
	{
		if (!StealthModeManager.getInstance().isSetUp())
		{
			if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false))
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_STEALTH_FTUE_CONV_TIP, ConversationTip.STEALTH_INFO_TIP);
				return;
			}
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE_FTUE_DONE, false);
			LockPattern.createNewPattern(activity, false, HikeConstants.ResultCodes.CREATE_LOCK_PATTERN);
		}
		else
		{
			if (!StealthModeManager.getInstance().isActive())
			{
				//if FTUE is not setup, show the HIDE TIP after removing REVEAL TIP
				if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE_FTUE_DONE, true))
				{
					//TODO need to find a way to know if stealth mSisdn hidden is just on
					if(true)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_STEALTH_HIDE_TIP, true);
						StealthModeManager.getInstance().activate(true);
						HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, true);
					}
					HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE_FTUE_DONE, true);
				}
				else
				{
					if(!isStealthFakeOn())
					{
						LockPattern.confirmPattern(activity, false, HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN);
					}
					else
					{
						StealthModeManager.getInstance().activate(true);
						HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, true);
					}
						
				}
			}
			else
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_STEALTH_HIDE_TIP, null);
				StealthModeManager.getInstance().activate(false);
			
					HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, true);
				
				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.EXIT_STEALTH_MODE);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
			}
		}
	}
	
	public void toggleConversation(ConvInfo conv, Activity activity) 
 	{	
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false))
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_STEALTH_INFO_TIP, null);
		}
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
