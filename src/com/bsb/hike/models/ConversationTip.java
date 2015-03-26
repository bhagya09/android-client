package com.bsb.hike.models;

public class ConversationTip extends Conversation
{
	public static final int GROUP_CHAT_TIP = 1;
	
	public static final int STEALTH_FTUE_TIP = 2;

	public static final int RESET_STEALTH_TIP = 3;

	public static final int WELCOME_HIKE_TIP = 4;

	public static final int STEALTH_INFO_TIP = 5;

	public static final int STEALTH_UNREAD_TIP = 6;

	public static final int ATOMIC_PROFILE_PIC_TIP = 7;

	public static final int ATOMIC_FAVOURTITES_TIP = 8;

	public static final int ATOMIC_INVITE_TIP = 9;

	public static final int ATOMIC_STATUS_TIP = 10;

	public static final int ATOMIC_INFO_TIP = 11;
	
	public static final int ATOMIC_HTTP_TIP = 12;
	
	public static final int ATOMIC_APP_GENERIC_TIP = 13;

	private int tipType;

	public ConversationTip(int tipType)
	{
//		passing 0 as timestamp because the tip is displayed on top irrespective, so this won't affect
		super(null, 0);
		this.tipType = tipType;
	}

	public int getTipType()
	{
		return tipType;
	}

	public void setTipType(int tipType)
	{
		this.tipType = tipType;
	}

	public boolean isGroupChatTip()
	{
		return tipType == GROUP_CHAT_TIP;
	}

	public boolean isStealthFtueTip()
	{
		return tipType == STEALTH_FTUE_TIP;
	}

	public boolean isResetStealthTip()
	{
		return tipType == RESET_STEALTH_TIP;
	}
	
	public boolean isWelcomeHikeTip()
	{
		return tipType == WELCOME_HIKE_TIP;
	}
	
	public boolean isStealthInfoTip()
	{
		return tipType == STEALTH_INFO_TIP;
	}

	public boolean isStealthUnreadTip()
	{
		return tipType == STEALTH_UNREAD_TIP;
	}
}
