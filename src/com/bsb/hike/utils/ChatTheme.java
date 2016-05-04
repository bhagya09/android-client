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
			return R.drawable.bg_header_transparent;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.blue_hike_status_bar_m;
		}

		@Override
		public int systemMessageTextViewLayoutId()
		{
			return R.layout.system_message_default_theme;
		}
	},
    
	COFFEEBEAN
	{

		@Override
		public String bgId()
		{
			return "42";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_coffeebean_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_coffeebean;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_default;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.nudge_sent;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.nudge_received;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.coffeebean_theme_status_bar_color;
		}

		@Override
		public int systemMessageTextViewLayoutId()
		{
			return R.layout.system_message_light;
		}

		@Override
		public int systemMessageBackgroundId()
		{
			return R.drawable.bg_system_message_light;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.independence_theme_status_bar_color;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.love_2_theme_status_bar_color;
		}

	},

	TRACK
	{

		@Override
		public String bgId()
		{
			return "41";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_track_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_track;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_default;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.nudge_sent;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.nudge_received;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.track_theme_status_bar_color;
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
		public int bubbleColor()
		{
			return R.color.bubble_rain2; //AND-2793
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.rain2_theme_status_bar_color;
		}

	},

	SLEEPINGDOG
	{

		@Override
		public String bgId()
		{
			return "36";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_sleepingdog_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_sleepingdog;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_default;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.nudge_sent;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.nudge_received;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.sleepingdog_theme_status_bar_color;
		}

	},
	PEACOCKGLORY
	{

		@Override
		public String bgId()
		{
			return "37";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_peacockglory_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_peacockglory;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_default;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.nudge_sent;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.nudge_received;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.peacockglory_theme_status_bar_color;
		}

		@Override
		public int systemMessageTextViewLayoutId()
		{
			return R.layout.system_message_light;
		}

		@Override
		public int systemMessageBackgroundId()
		{
			return R.drawable.bg_system_message_light;
		}
	},

	
	NOWHERE
	{

		@Override
		public String bgId()
		{
			return "38";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_nowhere_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_nowhere;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_default;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.nudge_sent;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.nudge_received;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.nowhere_theme_status_bar_color;
		}

		@Override
		public int systemMessageTextViewLayoutId()
		{
			return R.layout.system_message_light;
		}

		@Override
		public int systemMessageBackgroundId()
		{
			return R.drawable.bg_system_message_light;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.valentines_2_theme_status_bar_color;
		}

		@Override
		public int systemMessageTextViewLayoutId()
		{
			return R.layout.system_message_light;
		}

		@Override
		public int systemMessageBackgroundId()
		{
			return R.drawable.bg_system_message_light;
		}

		@Override
		public int getAnimationId()
		{
			return R.anim.valetines_nudge_anim;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.friends_theme_status_bar_color;
		}

		@Override
		public int systemMessageTextViewLayoutId()
		{
			return R.layout.system_message_light;
		}

		@Override
		public int systemMessageBackgroundId()
		{
			return R.drawable.bg_system_message_light;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.beach_2_theme_status_bar_color;
		}

		@Override
		public int systemMessageTextViewLayoutId()
		{
			return R.layout.system_message_light;
		}

		@Override
		public int systemMessageBackgroundId()
		{
			return R.drawable.bg_system_message_light;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.night_theme_status_bar_color;
		}

		@Override
		public int systemMessageTextViewLayoutId()
		{
			return R.layout.system_message_light;
		}

		@Override
		public int systemMessageBackgroundId()
		{
			return R.drawable.bg_system_message_light;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.spring_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.night_pattern_theme_status_bar_color;
		}

		@Override
		public int systemMessageTextViewLayoutId()
		{
			return R.layout.system_message_light;
		}

		@Override
		public int systemMessageBackgroundId()
		{
			return R.drawable.bg_system_message_light;
		}
	},
	GUITAR
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
			return R.drawable.ic_ct_guitar_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_guitar;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_default;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.nudge_sent;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.nudge_received;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.guitar_theme_status_bar_color;
		}

		@Override
		public int systemMessageTextViewLayoutId()
		{
			return R.layout.system_message_light;
		}

		@Override
		public int systemMessageBackgroundId()
		{
			return R.drawable.bg_system_message_light;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.starry_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.owl_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.beach_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.forest_theme_status_bar_color;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.hikin_couple_theme_status_bar_color;
		}

	},

	SWIVEL
	{

		@Override
		public String bgId()
		{
			return "25";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
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
			return R.drawable.ic_bubble_default;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.nudge_sent;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.nudge_received;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_3x;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.swivel_theme_status_bar_color;
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
			return R.drawable.bg_status_chat_thread_custom_theme;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.love_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.girly_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.mr_right_theme_status_bar_color;
		}

	},

	SMILEY
	{

		@Override
		public String bgId()
		{
			return "7";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.smiley_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.chatty_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.creepy_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.valentines_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.kisses_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.study_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.techy_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.celebration_theme_status_bar_color;
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
			return R.drawable.bg_header_transparent_2x;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.floral_theme_status_bar_color;
		}

	},

	PURPLE_FLOWER
	{

		@Override
		public String bgId()
		{
			return "44";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_purpleflower_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_purpleflower;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_blue;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_purpleflower;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_purpleflower;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.purpleflower_theme_status_bar_color;
		}

	},

	CHRISTMAS {
		@Override
		public String bgId()
		{
			return "43";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_christmas_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_christmas;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_blue;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_christmas;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_christmas;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.christmas_theme_status_bar_color;
		}

	},

	VALENTINES_2016 {
		@Override
		public String bgId()
		{
			return "45";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_valentine_2016_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_valentine;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_blue;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_valentines_2016;
		}

		@Override
		public boolean isAnimated()
		{
			return true;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_valentines_2016;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
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

		@Override
		public int statusBarColor()
		{
			// TODO Auto-generated method stub
			return R.color.valentines_2016_theme_status_bar_color;
		}

		@Override
		public int getAnimationId()
		{
			return R.anim.valentines_2016_nudge_anim;
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

	public int systemMessageTextViewLayoutId()
	{
		return R.layout.system_message_dark;
	}

	public int systemMessageBackgroundId()
	{
		return R.drawable.bg_system_message_dark;
	}

	public int getAnimationId()
	{
		return -1;
	}
	
	public abstract int bubbleColor();
	
	public abstract int smsToggleBgRes();
	
	public abstract int multiSelectBubbleColor();
	public abstract int statusBarColor();
	
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
	
	/*This stores the order of themes in which they will they will be displayed. Whenever another theme is added update this as well.
	 The current order followed is [0,45,44,41,42,36,37,38,30,25,28,21,23,20,40,32,22,29,26,8,1,39,43,24,14,31,18,17,10,2,4,9,13,15,11,7,3]
	*/
	public static ChatTheme[] THEME_PICKER = { DEFAULT, VALENTINES_2016, PURPLE_FLOWER, TRACK, COFFEEBEAN, SLEEPINGDOG, PEACOCKGLORY, NOWHERE, GUITAR,
			SWIVEL, FRIENDS, NIGHT_PATTERN, OWL, VALENTINES_2, LOVE_2, HIKIN_COUPLE, NIGHT, RAIN2, BEACH_2, CREEPY, LOVE, INDEPENDENCE, CHRISTMAS,
			SPRING, KISSES, MR_RIGHT, VALENTINES, STUDY, FLORAL, CHATTY, STARRY, CELEBRATION, TECHY, BEACH, FOREST, SMILEY, GIRLY };
	
	/*
	 * This method returns the position of given theme in THEME_PICKER array
	 */
	public static int getPositionForTheme(ChatTheme theme)
	{
		int position = 0;
		for (int i = 0; i < THEME_PICKER.length; i++)
			if (theme == THEME_PICKER[i])
				position = i;

		return position;
	}
};