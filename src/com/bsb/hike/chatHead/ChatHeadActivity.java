package com.bsb.hike.chatHead;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.media.StickerPicker;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatHead.ChatHeadService;
import com.bsb.hike.ui.HikeBaseActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public class ChatHeadActivity extends HikeBaseActivity implements StickerPickerListener, IFinishActivityListener
{
	public static int shareCount, totalShareCount, noOfDays, shareLimit, maxDismissLimit;
	
	private static final String SERVICE_START_DATE= "strtDate";
	
	public static final int DISMISS_CONST= 5;
	
	private StickerPicker picker;
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		this.finish();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	protected void onStop()
	{
		if (ChatHeadService.flagActivityRunning)
		{
			ChatHeadService.getInstance().resetPosition(ChatHeadUtils.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION);
		}
		saveUpdatedSharedPref();
		ChatHeadService.flagActivityRunning = false;
		picker.stoppingChatHeadActivity();
		ChatHeadService.unregisterReceiver(this);
		super.onStop();
	}
		
	private void saveUpdatedSharedPref()
	{
		HikeSharedPreferenceUtil.getInstance().saveData(ChatHeadUtils.DAILY_STICKER_SHARE_COUNT, shareCount);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT, totalShareCount);
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		this.overridePendingTransition(0, 0);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		ChatHeadService.flagActivityRunning = true;
		ChatHeadService.registerReceiver(this);
		setContentView(R.layout.chat_head);
		ChatHeadUtils.settingDailySharedPref();
		initVariables();
		getWindow().getDecorView().setBackgroundColor(R.color.transparent);
		picker = new StickerPicker(R.layout.chat_head_sticker_layout, this, this, null);
		LinearLayout layout = (LinearLayout) findViewById(R.id.sticker_pallete_other_app);
		picker.onCreatingChatHeadActivity(this, layout);
		picker.setOnClick();
	}

	public void closeActivity(View v)
	{
		ChatHeadService.getInstance().resetPosition(ChatHeadUtils.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION);
	}

	private void initVariables()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(SERVICE_START_DATE, -1L) == -1L)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(SERVICE_START_DATE, Utils.gettingMidnightTimeinMilliseconds());
		}
		shareCount = HikeSharedPreferenceUtil.getInstance().getData(ChatHeadUtils.DAILY_STICKER_SHARE_COUNT, 0);
		noOfDays = (int) ((Utils.gettingMidnightTimeinMilliseconds() - (HikeSharedPreferenceUtil.getInstance().getData(SERVICE_START_DATE,
				Utils.gettingMidnightTimeinMilliseconds()))) / (24 * ChatHeadUtils.HOUR_TO_MILLISEC_CONST)) + 1;
		if (noOfDays < 1)
		{
			noOfDays = 1;
		}
		totalShareCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT, 0);
		shareLimit = (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.STICKERS_PER_DAY, HikeConstants.ChatHead.DEFAULT_NO_STICKERS_PER_DAY) + HikeSharedPreferenceUtil
				.getInstance().getData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, 0));
		maxDismissLimit = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DISMISS_COUNT, DISMISS_CONST);
	}

	@Override
	public void stickerSelected(Sticker sticker, String source)
	{
		if (shareCount < shareLimit)
		{   HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.STICKER_SHARE, ChatHeadService.foregroundAppName, sticker.getCategoryId(),
					sticker.getStickerId(), source);
			shareCount++;
			totalShareCount++;
			String filePathBmp = sticker.getStickerPath(this);
		    ChatHeadService.getInstance().resetPosition(ChatHeadUtils.SHARING_BEFORE_FINISHING_ANIMATION, filePathBmp);
			ChatHeadService.dismissed = 0;
		}
		else
		{
			picker.infoIconClick();
		} 

	}

	@Override
	public void finishActivity()
	{
		finish();
		overridePendingTransition(0, 0);
	}

}