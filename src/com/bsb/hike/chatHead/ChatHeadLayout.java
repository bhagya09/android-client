package com.bsb.hike.chatHead;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.media.StickerPicker;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

public class ChatHeadLayout extends LinearLayout implements StickerPickerListener
{
	private StickerPicker picker;
	
	public void attachPicker()
	{
		picker = new StickerPicker(R.layout.chat_head_sticker_layout, HikeMessengerApp.getInstance(), this, null);
		picker.onCreatingChatHeadActivity(HikeMessengerApp.getInstance(), this);
		picker.setOnClick();
		this.setVisibility(View.VISIBLE);
	}
	
	public ChatHeadLayout(Context context)
	{
		super(context);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		// TODO Auto-generated method stub
		super.onLayout(changed, l, t, r, b);
	}

	@Override
	public void stickerSelected(Sticker sticker, String source)
	{
		if (ChatHeadUtils.shareCount < ChatHeadUtils.shareLimit)
		{HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.STICKER_SHARE, ChatHeadService.foregroundAppName, sticker.getCategoryId(),
					sticker.getStickerId(), source);
			ChatHeadUtils.shareCount++;
			ChatHeadUtils.totalShareCount++;
			String filePathBmp = sticker.getStickerPath(HikeMessengerApp.getInstance().getApplicationContext());
		    ChatHeadService.getInstance().resetPosition(ChatHeadConstants.SHARING_BEFORE_FINISHING_ANIMATION, filePathBmp);
			ChatHeadService.dismissed = 0;
		}
		else
		{
			picker.infoIconClick();
		} 

	}

}
