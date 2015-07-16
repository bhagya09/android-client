package com.bsb.hike.utils;

import com.bsb.hike.R;

public enum ChatTheme
{

	DEFAULT
	{
		@Override
		public int bgResId()
		{
			return R.color.chat_thread_default_bg;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_blue_selector;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_default_preview;
		}

		@Override
		public String bgId()
		{
			return "0";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_hike_sent;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_hike_receive;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_default_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.list_item_subtext;
		}
		
	},
	FRIENDS_FOREVER
	{

		@Override
		public String bgId()
		{
			return "28";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_foreverfriends_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_foreverfriends;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_chatty_bechy_techy;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	
	
	NIGHTFALL
	{

		@Override
		public String bgId()
		{
			return "22";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_nightfall_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_nightfall;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_night;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_night;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_dark;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_dark;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_night;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	
	OWL
	{

		@Override
		public String bgId()
		{
			return "23";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.bg_ct_owl_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_owl;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_owl;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_owl;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_owl;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	VALENTINES_2
	{

		@Override
		public String bgId()
		{
			return "20";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_valentine_2_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_valentines_2_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_smiley_geometric1_independence;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_valentines_2;
		}

		@Override
		public boolean isAnimated()
		{
			return true;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom_valentines_2;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_smiley_geometric1_independence;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	VALENTINES
	{

		@Override
		public String bgId()
		{
			return "18";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_valentine_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_valentines_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}
	},
	LOVEU
	{

		@Override
		public String bgId()
		{
			return "40";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_loveu_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_loveu;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}
	},
	LOVEFOREVER
	{

		@Override
		public String bgId()
		{
			return "31";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_loveforever_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_loveforever;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_2x;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_2x;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme_2x;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	HEART_LOVE
	{

		@Override
		public String bgId()
		{
			return "32";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_heartlove_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_heartlove;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_hikin_couple_mountain;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_hikin_couple_mountain;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_3x;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_3x;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_hikin_couple_mountain;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.action_bar_item_pressed;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	

	KISSES
	{

		@Override
		public String bgId()
		{
			return "14";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_kisses_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_kisses_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	LOVE
	{

		@Override
		public String bgId()
		{
			return "1";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_love_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_love_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_2x;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_2x;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme_2x;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	SAD
	{

		@Override
		public String bgId()
		{
			return "29";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_sad_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_sad;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_night;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_night;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_dark;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_dark;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_night;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	NEWDAISY
	{
		@Override
		public String bgId()
		{
			return "24";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_daisy_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_daisy;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_forest_study_sporty_fifa_nature;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_forest_study_sporty_fifa_nature;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_forest_study_sporty_fifa_nature;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	LIGHT_LANTERNS
	{

		@Override
		public String bgId()
		{
			return "21";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_lightlanterns_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_lightlanterns;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_night;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_night;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_dark;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_dark;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_night;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	FRIGHT
	{

		@Override
		public String bgId()
		{
			return "8";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_fright_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_fright;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	SMILEYS
	{

		@Override
		public String bgId()
		{
			return "7";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_smiley_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_smiley_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_smiley_geometric1_independence;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_smiley_geometric1_independence;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_smiley_geometric1_independence;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	HERO
	{

		@Override
		public String bgId()
		{
			return "36";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_hero_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_hero;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_smiley_geometric1_independence;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_smiley_geometric1_independence;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_smiley_geometric1_independence;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	INDIAN
	{

		@Override
		public String bgId()
		{
			return "39";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_indian_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_indian;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_forest_study_sporty_fifa_nature;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_forest_study_sporty_fifa_nature;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_forest_study_sporty_fifa_nature;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	ANGRY
	{

		@Override
		public String bgId()
		{
			return "241";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_angry_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_angry;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_night;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_night;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_dark;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_dark;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_night;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	JOY
	{

		@Override
		public String bgId()
		{
			return "38";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_joy_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_joy;
		}
		
		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_smiley_geometric1_independence;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_smiley_geometric1_independence;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_smiley_geometric1_independence;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	HAUNTED
	{

		@Override
		public String bgId()
		{
			return "230";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_haunted_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_haunted;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_smiley_geometric1_independence;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_smiley_geometric1_independence;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_smiley_geometric1_independence;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	STARRY
	{

		@Override
		public String bgId()
		{
			return "4";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_starry_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_starry_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_starry_space;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_starry_space;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_stary_Space;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	

		
	CHATTY
	{

		@Override
		public String bgId()
		{
			return "2";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_chatty_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_chatty_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_chatty_bechy_techy;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	
	STRIPES
	{

		@Override
		public String bgId()
		{
			return "17";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_stripes_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_stripes;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_chatty_bechy_techy;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	SWIVEL
	{

		@Override
		public String bgId()
		{
			return "3";
		}


		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_swivel_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_swivel;
		}


		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}


	},
	AQUA
	{

		@Override
		public String bgId()
		{
			return "15";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_aqua_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_aqua;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_2x;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_2x;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_chatty_bechy_techy;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	FOREST
	{

		@Override
		public String bgId()
		{
			return "11";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_forest_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_forest_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_forest_study_sporty_fifa_nature;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_forest_study_sporty_fifa_nature;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_forest_study_sporty_fifa_nature;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	TECHY
	{

		@Override
		public String bgId()
		{
			return "13";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_techy_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_techy_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_chatty_bechy_techy;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	CELEBRATION
	{

		@Override
		public String bgId()
		{
			return "9";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_celebration_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_celebration_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_celebration_space;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_celebration_space;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_celebration_space;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	
	HAPPY
	{

		@Override
		public String bgId()
		{
			return "10";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_happy_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_happy;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_smiley_geometric1_independence;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_smiley_geometric1_independence;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_smiley_geometric1_independence;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	BUSY
	{

		@Override
		public String bgId()
		{
			return "42";
		}


		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_busy_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_busy;
		}


		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}
	},
	PINK_CANDY
	{

		@Override
		public String bgId()
		{
			return "25";
		}


		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_pinkcandy_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_pinkcandy;
		}


		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_kisses_valentines_girly_ipl_blurredlight_love2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}


	},
	SEASHORE
	{

		@Override
		public String bgId()
		{
			return "26";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_seashore_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_seashore;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_mr_right_exam;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_mr_right_exam;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_mr_right_exam;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme_2x;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	};
	
	
	
	
	
	
	
	
	

	
	public abstract String bgId();

	public abstract int headerBgResId();

	public abstract int previewResId();

	public abstract int bgResId();

	public abstract int bubbleResId();

	public abstract boolean isTiled();

	public abstract int sentNudgeResId();

	public abstract boolean isAnimated();

	public abstract int receivedNudgeResId();

	public abstract int inLineUpdateBGResId();

	public abstract int systemMessageLayoutId();
	
	public abstract int bubbleColor();
	
	public abstract int smsToggleBgRes();
	
	public abstract int multiSelectBubbleColor();
	
	public abstract int offlineMsgTextColor();

	public static ChatTheme getThemeFromId(String bgId)
	{
		if (bgId == null)
		{
			throw new IllegalArgumentException();
		}
		for (ChatTheme chatTheme : values())
		{
			if (chatTheme.bgId().equals(bgId))
			{
				return chatTheme;
			}
		}
		throw new IllegalArgumentException();
	}

	public static ChatTheme[] FTUE_THEMES = { STARRY, AQUA, FOREST };
};