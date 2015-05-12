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
	FRIENDS
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
			return R.drawable.ic_ct_friends_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_friends;
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
	
	BEACH_2
	{

		@Override
		public String bgId()
		{
			return "26";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_beach2_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_beach_2;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_beach_2;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_beach_2;
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
			return R.layout.participant_info_custom_3x;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_beach_2;
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

	NIGHT
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
			return R.drawable.bg_ct_night_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_night;
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
	LOVE_2
	{

		@Override
		public String bgId()
		{
			return "40";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_love_2_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_love_2;
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
	HIKIN_COUPLE
	{

		@Override
		public String bgId()
		{
			return "32";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_hikin_couple_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_hikin_couple;
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

	},
	SWIVEL
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
			return R.drawable.bg_header_transparent;
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
	MR_RIGHT
	{

		@Override
		public String bgId()
		{
			return "31";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_mr_right_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_mr_right;
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
			return R.color.light_black_transparent;
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
	RAIN2
	{

		@Override
		public String bgId()
		{
			return "29";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_rain2_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_rain_2;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_rain2;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_rain2;
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
			return R.color.bubble_beach_2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme_2x;
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
	SPRING
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
			return R.drawable.ic_ct_spring_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_spring;
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
	
	NIGHT_PATTERN
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
			return R.drawable.ic_ct_night_pattern_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_night_pattern;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_night;
		}

		@Override
		public boolean isTiled()
		{
			return true;
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
	CREEPY
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
			return R.drawable.ic_ct_creepy_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_creepy_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_creepy;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_creepy;
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
			return R.color.bubble_creepy;
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
	IPL
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
			return R.drawable.ic_ct_ipl_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_ipl;
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
	INDEPENDENCE
	{

		@Override
		public String bgId()
		{
			return "39";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_3x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_independence_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_independence;
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
			return R.color.bubble_smiley_geometric1_independence;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme_3x;
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
	NATURE
	{

		@Override
		public String bgId()
		{
			return "41";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_nature_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_nature;
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
	BLURRED_LIGHT
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
			return R.drawable.ic_ct_blurred_light_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_blurred_light;
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
	MUSIC
	{

		@Override
		public String bgId()
		{
			return "30";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_music_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_music;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_music;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_white;
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
			return R.color.bubble_music;
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
	GIRLY
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
			return R.drawable.ic_ct_girly_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_girly_tile;
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
	
	FLORAL
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
			return R.drawable.ic_ct_floral_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_floral_tile;
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
	STUDY
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
			return R.drawable.ic_ct_study_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_study_tile;
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
	GEOMETRIC_1
	{

		@Override
		public String bgId()
		{
			return "37";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_geometric_1_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_geometric_1;
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
	BEACH
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
			return R.drawable.ic_ct_beach_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_beach_tile;
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
	STRIPES
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

	DIWALI
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
			return R.drawable.ic_ct_diwali_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_diwali;
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
			return R.drawable.ic_nudge_sent_custom_diwali;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom_diwali;
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

	

	

	
	
	
	
	
	
	
	
	

	
	

	
	
	
	
	
	
	
	

	MOUNTAINS
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
			return R.drawable.ic_ct_mountain_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_mountain;
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

	public static ChatTheme[] FTUE_THEMES = { STARRY, BEACH, FOREST };
};