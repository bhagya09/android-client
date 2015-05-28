package com.bsb.hike.utils;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.utils.LockPattern;

/*
 * Reset mode is highly dependent on this singleTon, therefore it needs more asynchronous fault proofing.
 */
public class StealthModeManager
{
	
	public static final String DEFAULT_RESET_TOGGLE_TIME = "0";
	
	private static final String NEVER_RESET_TOGGLE_TIME = "-1";

	private static final StealthModeManager stealthModeManager = new StealthModeManager();

	private HikeHandlerUtil handler;

	private static Set<String> stealthMsisdn;
	
	private int currentState;

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
		setTipVisibility(false, ConversationTip.STEALTH_FTUE_TIP);
		ftuePending(false);
		activate(false);
	}

	public int getStealthMsisdnMapSize() {
		return stealthMsisdn.size();
	}

	public void markStealthMsisdn(String msisdn, boolean markStealth, boolean publish)
	{
		if(markStealth)
		{
			stealthMsisdn.add(msisdn);
		}
		else
		{
			stealthMsisdn.remove(msisdn);
		}
		if(publish)
		{
			HikeMessengerApp.getPubSub().publish(markStealth? HikePubSub.STEALTH_DATABASE_MARKED : HikePubSub.STEALTH_DATABASE_UNMARKED, msisdn);
			// TODO analytics, marking/unmarking chat as stealth
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

	private void resetStealthToggleTimer()
	{
		clearScheduledStealthToggleTimer();
		String stealthTimeOut = PreferenceManager.getDefaultSharedPreferences(HikeMessengerApp.getInstance().getApplicationContext()).getString(HikeConstants.CHANGE_STEALTH_TIMEOUT, DEFAULT_RESET_TOGGLE_TIME);

		if(!stealthTimeOut.equals(NEVER_RESET_TOGGLE_TIME))
		{
			
			handler.postRunnableWithDelay(toggleReset, Long.parseLong(stealthTimeOut) * 1000);
		}
	}

	private void clearScheduledStealthToggleTimer()
	{
		stealthFakeOn();
		handler.removeRunnable(toggleReset);
	}

	private Runnable toggleReset = new Runnable()
	{

		@Override
		public void run()
		{
			if(isActive() || isStealthFakeOn())
			{
				activate(false);
			}
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
		 if(!isSetUp)
		 {
			 activate(false);
		 }
		 // TODO analytics setup / unsetup
	}
	
	private void stealthFakeOn()
	{
		if(isActive())
		{
			currentState = HikeConstants.STEALTH_ON_FAKE;
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE, currentState);
			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, null);
		}
	}
	
	private boolean isStealthFakeOn()
	{
		//specific use case where we want to activate hidden mode with out confirming password/pin
		return (currentState == HikeConstants.STEALTH_ON_FAKE);
	}

	public boolean isActive()
	{
		//this means stealth is neither fake on nor off
		return (currentState == HikeConstants.STEALTH_ON);
	}

	public void activate(boolean activate)
	{
		currentState = activate ? HikeConstants.STEALTH_ON : HikeConstants.STEALTH_OFF;
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE, currentState);
		HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, null);
		// TODO analytics activates/deactivated
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
	
	public void ftuePending(boolean pending)
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_MODE_FTUE_DONE, !pending);
	}
	
	public boolean isFtueDone()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE_FTUE_DONE, true);
	}
	
	public void showLockPattern(String msisdn, Activity activity)
	{
		Bundle stealthBundle = new Bundle();
		stealthBundle.putString(HikeConstants.STEALTH_MSISDN, msisdn);

		if (!isSetUp())
		{
			ftuePending(true);
			LockPattern.createNewPattern(activity, false, HikeConstants.ResultCodes.CREATE_LOCK_PATTERN_HIDE_CHAT, stealthBundle);
		} 
		else if (!isActive())
		{
			if(isFtueDone())
			{
				LockPattern.confirmPattern(activity, false, HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN_HIDE_CHAT, stealthBundle);
			}
			else
			{
				setTipVisibility(true, ConversationTip.STEALTH_REVEAL_TIP);
				markStealthMsisdn(msisdn, true, true);
			}
		}

	}

	public void toggleActionTriggered(Activity activity)
	{

		if (isTipPersisted(ConversationTip.STEALTH_UNREAD_TIP))
		{
			setTipVisibility(false, ConversationTip.STEALTH_UNREAD_TIP);
		}

		if (!isSetUp())
		{
			if (!isTipPersisted(ConversationTip.STEALTH_INFO_TIP))
			{
				setTipVisibility(true, ConversationTip.STEALTH_INFO_TIP);
				return;
			}
			else
			{
				setTipVisibility(false, ConversationTip.STEALTH_INFO_TIP);
			}
			ftuePending(true);
			LockPattern.createNewPattern(activity, false, HikeConstants.ResultCodes.CREATE_LOCK_PATTERN);
		}
		else
		{

			if (isTipPersisted(ConversationTip.STEALTH_FTUE_TIP) && !isFtueDone())
			{
				return;
			}

			if (!isActive())
			{
				//if FTUE is not setup, show the HIDE TIP after removing REVEAL TIP
				if(!isFtueDone())
				{
					setTipVisibility(true, ConversationTip.STEALTH_HIDE_TIP);
					activate(true);
					ftuePending(false);
				}
				else
				{
					if(!isStealthFakeOn())
					{
						LockPattern.confirmPattern(activity, false, HikeConstants.ResultCodes.CONFIRM_LOCK_PATTERN);
					}
					else
					{
						activate(true);
					}
				}
			}
			else
			{
				setTipVisibility(false,ConversationTip.STEALTH_HIDE_TIP);
				activate(false);

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
		//TODO anayltics if Hi button was pressed or not
	}

	public boolean isTipPersisted(int tipType)
	{
		HikeSharedPreferenceUtil mPrefs = HikeSharedPreferenceUtil.getInstance();
		switch (tipType)
		{
		case ConversationTip.STEALTH_FTUE_TIP:
			return mPrefs.getData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP, false);
		case ConversationTip.STEALTH_INFO_TIP:
			return mPrefs.getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false);
		case ConversationTip.STEALTH_UNREAD_TIP:
			return mPrefs.getData(HikeMessengerApp.SHOW_STEALTH_UNREAD_TIP, false);
		default: 
			break;
		}
		return false;
	}
	
	public void closingConversationScreen(int tipType)
	{
		if (tipType == ConversationTip.STEALTH_REVEAL_TIP || tipType == ConversationTip.STEALTH_HIDE_TIP)
		{
			if(tipType == ConversationTip.STEALTH_REVEAL_TIP)
			{
				// not sure if it is needed
				activate(false);
			}
			ftuePending(false);
			setTipVisibility(false, tipType);
		}
	}
	
	public void setTipVisibility(boolean makeTipVisible, int tipType)
	{
		HikeSharedPreferenceUtil mPrefs = HikeSharedPreferenceUtil.getInstance();
		switch (tipType)
		{
		case ConversationTip.STEALTH_FTUE_TIP:
			mPrefs.saveData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP, makeTipVisible && isSetUp());
			break;
		case ConversationTip.STEALTH_INFO_TIP:
			mPrefs.saveData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, makeTipVisible && !isSetUp());
			break;
		default: 
			break;
		}
		HikeMessengerApp.getPubSub().publish(makeTipVisible ? HikePubSub.SHOW_TIP : HikePubSub.REMOVE_TIP, tipType);
		// TODO analytics tip close-show
	}
	
	public void toggleConversation(String msisdn, boolean markStealth, Activity activity) 
 	{

		setTipVisibility(false, ConversationTip.STEALTH_INFO_TIP);
		setTipVisibility(false, ConversationTip.STEALTH_FTUE_TIP);

		if(isActive())
		{
			markStealthMsisdn(msisdn, markStealth, true);
		}
		else
		{
			if(activity instanceof HomeActivity)
			{
				showLockPattern(msisdn, activity);
			}
		}
		//TODO analytics activity defines MH 1/0, MV 1/0, HC 1/0,[1/0 - inside/outside]
	}

	public void usePinAsPassword(boolean usePin) 
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STEALTH_PIN_AS_PASSWORD, usePin);
	}
	
	public boolean isPinAsPassword() 
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_PIN_AS_PASSWORD, false);
	}

	public void appStateChange(boolean resetStealth, boolean appBackgrounded)
	{

		if (appBackgrounded)
		{
			if(!isFtueDone() || isTipPersisted(ConversationTip.STEALTH_FTUE_TIP))
			{
				activate(false);
				setTipVisibility(false, ConversationTip.STEALTH_FTUE_TIP);
				ftuePending(false);
			}
			if (!isSetUp())
			{
				// if stealth setup is not done and user has marked some chats as stealth unmark all of them
				// this should ideally only happen when user upgrades from HM1 to HM2 while his hidden is not setup
				for (String msisdn : stealthMsisdn)
				{
					markStealthMsisdn(msisdn, false, true);
				}
				clearStealthMsisdn();
			}
		}
		
		if (resetStealth)
		{
			if (appBackgrounded)
			{
				resetStealthToggleTimer();
			}
			else
			{
				clearScheduledStealthToggleTimer();
			}
		}
	}
}
