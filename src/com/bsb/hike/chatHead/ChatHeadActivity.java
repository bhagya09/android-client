package com.bsb.hike.chatHead;

import java.io.File;
import java.util.Calendar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.media.StickerPicker;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatHead.ChatHeadService;
import com.bsb.hike.ui.HikeBaseActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ChatHeadActivity extends HikeBaseActivity implements StickerPickerListener, OnClickListener, TabClickListener,FinishActivityListener
{
	private View stickerPickerView;

	private LinearLayout layout;

	private int shareCount, totalShareCount, noOfDays, shareLimit, maxDismissLimit;

	private StickerPicker picker;

	private StickerEmoticonIconPageIndicator mIconPageIndicator;

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
			ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHead.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION);
		}
		saveUpdatedSharedPref();
		ChatHeadService.flagActivityRunning = false;
		StickerEmoticonIconPageIndicator.unRegisterChatHeadTabClickListener();
		ChatHeadService.unregisterReceiver(this);
		super.onStop();
	}

	private void saveUpdatedSharedPref()
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.DAILY_STICKER_SHARE_COUNT, shareCount);
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
		stickerPickerView = picker.getView(getResources().getConfiguration().orientation);
		layout = (LinearLayout) findViewById(R.id.sticker_pallete_other_app);
		View infoIcon = (stickerPickerView.findViewById(R.id.info_icon));
		if (infoIcon != null)
		{
			infoIcon.setOnClickListener(this);
		}
		layout.addView(stickerPickerView);
		if (ChatHeadService.dismissed > maxDismissLimit || shareCount >= shareLimit)
		{
			infoIconClick();

		}
		setOnClick();
	}

	public void closeActivity(View v)
	{
		ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHead.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION);
	}
	
	private void initVariables()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SERVICE_START_DATE, -1L) == -1L)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SERVICE_START_DATE, Utils.gettingMidnightTimeinMilliseconds());
		}
		shareCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DAILY_STICKER_SHARE_COUNT, 0);
		noOfDays = (int) ((Utils.gettingMidnightTimeinMilliseconds() - (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SERVICE_START_DATE,
				Utils.gettingMidnightTimeinMilliseconds()))) / (24 * 60 * 60 * 1000)) + 1 ;
		if (noOfDays<1)
		{
			noOfDays = 1;
		}
		totalShareCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT, 0);
		shareLimit = (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.STICKERS_PER_DAY, HikeConstants.ChatHead.DEFAULT_NO_STICKERS_PER_DAY) + HikeSharedPreferenceUtil
				.getInstance().getData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, 0));
		maxDismissLimit = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DISMISS_COUNT, HikeConstants.ChatHead.DISMISS_CONST);
	}

	@Override
	public void stickerSelected(Sticker sticker, String source)
	{
		if (shareCount < shareLimit)
		{
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.STICKER_SHARE, ChatHeadService.foregroundAppName, sticker.getCategoryId(),
					sticker.getStickerId(), source);
			shareCount++;
			totalShareCount++;
			String filePath = sticker.getStickerPath(this);
			File stickerFile = new File(filePath, sticker.getStickerId());
			String filePathBmp = stickerFile.getAbsolutePath();
			ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHead.SHARING_BEFORE_FINISHING_ANIMATION, filePathBmp);
			ChatHeadService.dismissed = 0;
		}
		else
		{
			infoIconClick();
		}

	}

	@Override
	public void finishActivity()
	{
		finish();
		overridePendingTransition(0, 0);
	}
	
	private void infoIconClick()
	{
		LinearLayout disableLayout, infoIconLayout;
		TextView sideText;
		ViewPager viewPager = (ViewPager) (stickerPickerView.findViewById(R.id.sticker_pager));
		viewPager.setVisibility(View.GONE);
		ImageView imageView = (ImageView) (stickerPickerView.findViewById(R.id.info_icon));
		imageView.setSelected(true);
		infoIconLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.info_icon_layout));
		infoIconLayout.setVisibility(View.VISIBLE);
		mIconPageIndicator = (StickerEmoticonIconPageIndicator) stickerPickerView.findViewById(R.id.sticker_icon_indicator);
		mIconPageIndicator.unselectCurrent();
		disableLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.disable_layout));
		disableLayout.setVisibility(View.GONE);
		if (ChatHeadService.dismissed > maxDismissLimit)
		{
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.INFOICON_WITHOUT_CLICK, ChatHeadService.foregroundAppName, AnalyticsConstants.ChatHeadEvents.DISMISS_LIMIT);
			TextView tv = (TextView) (infoIconLayout.findViewById(R.id.disable));
			tv.setTextColor(getResources().getColor(R.color.external_pallete_text_highlight_color));
			ChatHeadService.dismissed = 0;

		}
		else if (shareCount >= shareLimit)
		{
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.INFOICON_WITHOUT_CLICK, ChatHeadService.foregroundAppName, AnalyticsConstants.ChatHeadEvents.SHARE_LIMIT);
			TextView tv = (TextView) (infoIconLayout.findViewById(R.id.get_more_stickers));
			tv.setTextColor(getResources().getColor(R.color.external_pallete_text_highlight_color));
		}
		initLayoutComponentsView();
		sideText = (TextView) (stickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.side_text));
		sideText.setText(getString(R.string.total_sticker_sent_start) + " " + totalShareCount + " " + getString(R.string.total_sticker_sent_middle) + " " + noOfDays + " "
				+ getString(R.string.total_sticker_sent_end));
	}

	private void onDisableClick()
	{
		LinearLayout mainLayout, disableLayout;
		TextView sideText;
		mainLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.main_layout));
		mainLayout.setVisibility(View.GONE);
		disableLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.disable_layout));
		disableLayout.setVisibility(View.VISIBLE);
		sideText = (TextView) (stickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.side_text));
		sideText.setText(getString(R.string.disable_from_hike_settings));
	}

	private void onBackMainLayoutClick()
	{
		LinearLayout mainLayout, disableLayout;
		TextView sideText;
		mainLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.main_layout));
		mainLayout.setVisibility(View.VISIBLE);
		disableLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.disable_layout));
		disableLayout.setVisibility(View.GONE);
		sideText = (TextView) (stickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.side_text));
		sideText.setText(getString(R.string.total_sticker_sent_start) + " " + totalShareCount + " " + getString(R.string.total_sticker_sent_middle) + " " + noOfDays + " "
				+ getString(R.string.total_sticker_sent_end));

	}

	private void onClickSetAlarm(int time)
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SNOOZE, true);
		HikeAlarmManager.setAlarm(getApplicationContext(), Calendar.getInstance().getTimeInMillis() + time, HikeAlarmManager.REQUESTCODE_START_STICKER_SHARE_SERVICE, false);
		ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHead.STOPPING_SERVICE_ANIMATION);
	}

	@Override
	public void onClick(View v)
	{

		switch (v.getId())
		{
		case R.id.info_icon:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.INFOICON_CLICK, ChatHeadService.foregroundAppName);
			infoIconClick();
			break;
		case R.id.disable:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.MAIN_LAYOUT_CLICKS, ChatHeadService.foregroundAppName, AnalyticsConstants.ChatHeadEvents.DISABLE_SETTING);
			onDisableClick();
			break;
		case R.id.get_more_stickers:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.MAIN_LAYOUT_CLICKS, ChatHeadService.foregroundAppName, AnalyticsConstants.ChatHeadEvents.MORE_STICKERS);
			ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHead.GET_MORE_STICKERS_ANIMATION);
			break;
		case R.id.open_hike:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.MAIN_LAYOUT_CLICKS, ChatHeadService.foregroundAppName, AnalyticsConstants.ChatHeadEvents.OPEN_HIKE);
			ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHead.OPEN_HIKE_ANIMATION);
			break;
		case R.id.one_hour:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.SNOOZE_TIME, ChatHeadService.foregroundAppName, AnalyticsConstants.ChatHeadEvents.ONE_HOUR);
			onClickSetAlarm(1 * 60 * 60 * 1000);
			break;
		case R.id.eight_hours:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.SNOOZE_TIME, ChatHeadService.foregroundAppName, AnalyticsConstants.ChatHeadEvents.EIGHT_HOURS);
			onClickSetAlarm(8 * 60 * 60 * 1000);
			break;
		case R.id.one_day:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.SNOOZE_TIME, ChatHeadService.foregroundAppName, AnalyticsConstants.ChatHeadEvents.ONE_DAY);
			onClickSetAlarm(24 * 60 * 60 * 1000);
			break;
		case R.id.back_main_layout:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.BACK, ChatHeadService.foregroundAppName);
			onBackMainLayoutClick();
			break;
		case R.id.shop_icon_external:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.STICKER_SHOP, ChatHeadService.foregroundAppName);
			ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHead.STICKER_SHOP_ANIMATION);
			break;
		}

	}

	private void setOnClick()
	{
		LinearLayout layout = (LinearLayout) (stickerPickerView.findViewById(R.id.info_icon_layout));
		layout.findViewById(R.id.get_more_stickers).setOnClickListener(this);
		layout.findViewById(R.id.disable).setOnClickListener(this);
		layout.findViewById(R.id.open_hike).setOnClickListener(this);
		layout.findViewById(R.id.back_main_layout).setOnClickListener(this);
		layout.findViewById(R.id.one_day).setOnClickListener(this);
		layout.findViewById(R.id.one_hour).setOnClickListener(this);
		layout.findViewById(R.id.eight_hours).setOnClickListener(this);

		StickerEmoticonIconPageIndicator.registerChatHeadTabClickListener(this);

		View shopIcon = (stickerPickerView.findViewById(R.id.shop_icon_external));
		shopIcon.setOnClickListener(this);
	}

	private void initLayoutComponentsView()
	{
		LinearLayout layout = (LinearLayout) (stickerPickerView.findViewById(R.id.main_layout));
		layout.setVisibility(View.VISIBLE);
		layout.findViewById(R.id.disable).setOnClickListener(this);
		TextView tv = (TextView) (stickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.main_text));
		tv.setText(shareCount + "/" + shareLimit + " " + getString(R.string.stickers_sent_today));
		ProgressBar progressBar = (ProgressBar) (stickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.progress_bar));
		int progress;
		if (shareLimit != 0)
		{
			progress = (int) ((shareCount * 100) / shareLimit);
		}
		else
		{
			progress = 0;
		}
		progressBar.setProgress(progress);
	}

	@Override
	public void onTabClick()
	{
		ViewPager viewPager = (ViewPager) (stickerPickerView.findViewById(R.id.sticker_pager));
		viewPager.setVisibility(View.VISIBLE);
		ImageView imageView = (ImageView) (stickerPickerView.findViewById(R.id.info_icon));
		imageView.setSelected(false);
		LinearLayout layout = (LinearLayout) (stickerPickerView.findViewById(R.id.info_icon_layout));
		layout.setVisibility(View.GONE);
	}

}