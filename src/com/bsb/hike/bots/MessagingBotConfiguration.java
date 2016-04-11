package com.bsb.hike.bots;


/**
 * Created by shobhit on 22/04/15.
 */
public class MessagingBotConfiguration extends BotConfiguration
{
	private boolean isReceiveEnabled;

	public MessagingBotConfiguration(int config, boolean isReceiveEnabled)
	{
		super(config);
		this.isReceiveEnabled = isReceiveEnabled;
	}

	//Bit position for config: starting from the most significant bit : call, attachment, sticker, smiley, walkie_talkie, text input, nudge,
	// view profile everywhere, 3dot menu, view profile in 3 dot menu, chat theme in 3dot menu, block in 3dot menu, clear chat in 3dot menu,
	// email chat in 3dot menu, mute in 3dot menu, long tap, view profile in long tap,add conversation shortcut in long tap,
	// chat in long tap, clear conv in long tap, email conv in long tap.

	static class ConversationScreen
	{
		public static byte EMAIL_CONV = 0;

		public static byte CLEAR_CONV = 1;

		public static byte DELETE_CHAT = 2;

		public static byte ADD_CONVERSATION_SHORTCUT = 3;

		public static byte VIEW_PROFILE = 4;


	}
	private static byte LONG_TAP = 5;

	static class OverflowMenu
	{
		public static byte MUTE = 6;

		public static byte EMAIL_CHAT = 7;

		public static byte CLEAR_CHAT = 8;

		public static byte BLOCK = 9;

		public static byte CHAT_THEME = 10;

		public static byte VIEW_PROFILE = 11;

		public static byte SEARCH = 23;

		public static byte HELP = 26;
	}



	private static byte OVERFLOW_MENU = 12;

	private static byte VIEW_PROFILE = 13;

	private static byte ADD_BLOCK_STRIP = 14;

	private static byte NUDGE = 15;

	private static byte TEXT_INPUT = 16;

	private static byte AUDIO_RECORD = 17;

	private static byte EMOTICON_PICKER = 18;

	private static byte STICKER_PICKER = 19;

	private static byte INPUT_BOX = 20;

	private static byte ATTACHMENT_PICKER = 21;

	private static byte CALL = 22;
	
	public static final byte SLIDE_IN = 23;
	
	public static final byte READ_SLIDE_OUT = 24;

	public static final byte SHOW_KPT_EXIT_UI = 25;
	
	public boolean isLongTapEnabled()
	{
		return isBitSet(LONG_TAP);
	}

	public boolean isEmailConvInConversationScreenEnabled()
	{
		return  isBitSet(ConversationScreen.EMAIL_CONV);
	}

	public boolean isClearConvInConversationScreenEnabled()
	{
		return  isBitSet(ConversationScreen.CLEAR_CONV);
	}

	public boolean isDeleteChatInConversationScreenEnabled()
	{
		return  isBitSet(ConversationScreen.DELETE_CHAT);
	}

	public boolean isAddConvShortcutInConversationScreenEnabled()
	{
		return isBitSet(ConversationScreen.ADD_CONVERSATION_SHORTCUT);
	}

	public boolean isViewProfileInConversationScreenEnabled()
	{
		return isBitSet(ConversationScreen.VIEW_PROFILE);
	}

	public boolean isOverflowMenuEnabled()
	{
		return isBitSet(OVERFLOW_MENU);
	}

	public boolean isMuteInOverflowMenuEnabled()
	{
		return isBitSet(OverflowMenu.MUTE);
	}

	public boolean isEmailChatInOverflowMenuEnabled()
	{
		return isBitSet(OverflowMenu.EMAIL_CHAT);
	}
	
	public boolean isClearChatInOverflowMenuEnabled()
	{
		return isBitSet(OverflowMenu.CLEAR_CHAT);
	}

	public boolean isBlockInOverflowMenuEnabled()
	{
		return isBitSet(OverflowMenu.BLOCK);
	}

	public boolean isSearchInOverflowMenuEnabled()
	{
		return isBitSet(OverflowMenu.SEARCH);
	}

	public boolean isChatThemeInOverflowMenuEnabled()
	{
		return isBitSet(OverflowMenu.CHAT_THEME);
	}

	public boolean isViewProfileInOverflowMenuEnabled()
	{
		return isBitSet(OverflowMenu.VIEW_PROFILE);
	}

	public boolean isViewProfileEnabled()
	{
		return isBitSet(VIEW_PROFILE);
	}

	public boolean isAddBlockStripEnabled()
	{
		return isBitSet(ADD_BLOCK_STRIP);
	}

	public boolean isNudgeEnabled()
	{
		return isReceiveEnabled && isBitSet(NUDGE);
	}

	public boolean isTextInputEnabled()
	{
		return isReceiveEnabled &&  isBitSet(TEXT_INPUT);
	}

	public boolean isAudioRecordingEnabled()
	{
		return isReceiveEnabled &&  isBitSet(AUDIO_RECORD);
	}

	public boolean isEmoticonPickerEnabled()
	{
		return isReceiveEnabled &&  isBitSet(EMOTICON_PICKER);
	}

	public boolean isStickerPickerEnabled()
	{
		return isReceiveEnabled &&  isBitSet(STICKER_PICKER);
	}

	public boolean isInputEnabled()
	{
		return  isReceiveEnabled && isBitSet(INPUT_BOX);
	}

	public boolean isAttachmentPickerEnabled()
	{
		return isReceiveEnabled && isBitSet(ATTACHMENT_PICKER);
	}

	public boolean isCallEnabled()
	{
		return isReceiveEnabled && isBitSet(CALL);
	}
	
	public boolean  isSlideInEnabled()
	{
		return isBitSet(SLIDE_IN);
	}
	
	public void setBit(byte bit,boolean toSet)
	{
		if (toSet)
		{
			setConfig(getConfig() | (1 << bit));
		}
		else
		{
			setConfig(getConfig() & ~(1 << bit));
		}
	}
	
	public boolean isReadSlideOutEnabled()
	{
		return isBitSet(READ_SLIDE_OUT);
	}

	public boolean isKptExitUIEnabled() {

		return isBitSet(SHOW_KPT_EXIT_UI);
	}

	public boolean isHelpInOverflowMenuEnabled()
	{
		return isBitSet(OverflowMenu.HELP);
	}
	
}
