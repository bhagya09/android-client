package com.bsb.hike.chatHead;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.media.StickerPicker;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class ChatHeadLayout implements StickerPickerListener
{
	private static StickerPicker picker;
	private static final ChatHeadLayout chatHeadLayout = new ChatHeadLayout();
	private static View overlayScreenViewGroup;
	
	private ChatHeadLayout(){}
	
	public static ChatHeadLayout getLayout()
	{
		return chatHeadLayout;
	}

	public static View getOverlayView()
	{
		return overlayScreenViewGroup;
	}
	
	public static View attachPicker(Context context)
	{	
		ChatHeadUtils.settingDailySharedPref();
		ChatHeadUtils.initVariables();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		overlayScreenViewGroup = (RelativeLayout)inflater.inflate(R.layout.chat_head, null);
	    //overlayScreenViewGroup.setOnClickListener(chatHeadLayout);
		LinearLayout chatty = (LinearLayout) overlayScreenViewGroup.findViewById(R.id.sticker_pallete_other_app);
		picker = new StickerPicker(R.layout.chat_head_sticker_layout, context, chatHeadLayout, null);
		picker.onCreatingChatHeadActivity(context, chatty);
		picker.setOnClick();
		overlayScreenViewGroup.setFocusableInTouchMode(true);
		return overlayScreenViewGroup;
	}
	
	public static View detachPicker(Context context)
	{
		picker.stoppingChatHeadActivity();
		return overlayScreenViewGroup;
	
	}

	@Override
	public void stickerSelected(Sticker sticker, String source)
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, 0) < ChatHeadUtils.shareLimit)
		{
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.STICKER_SHARE, ChatHeadViewManager.foregroundAppName, sticker.getCategoryId(),
					sticker.getStickerId(), source);
			HikeSharedPreferenceUtil.getInstance().saveData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, HikeSharedPreferenceUtil.getInstance().getData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, 0)+1);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT, (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT,0)+1));
		    ChatHeadViewManager.dismissed = 0;
			String filePathBmp = sticker.getLargeStickerPath();
		    picker.resetPostionAfterSharing(filePathBmp);
		
		}
		else
		{
			picker.infoIconClick();
		} 

	}
}
