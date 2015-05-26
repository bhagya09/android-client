package com.bsb.hike.chatHead;

import java.io.File;
import java.util.Calendar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.media.StickerPicker;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.chatHead.ChatHeadService;
import com.bsb.hike.chatHead.ChatHeadServiceManager;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ChatHeadActivity extends Activity implements StickerPickerListener, OnClickListener
{

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		this.finish();
	}

	public static boolean flagActivity = false;

	View stickerPickerView;

	LinearLayout layout;

	static ChatHeadActivity instance;

	int shareCount, totalShareCount, noOfDays, shareLimit, maxDismissLimit;

	StickerPicker picker;

	StickerEmoticonIconPageIndicator mIconPageIndicator;

	@Override
	protected void onPause()
	{
		super.onDestroy();
		ChatHeadService.flagActivityRunning = false;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		saveUpdtdShrdPrefVar();
		flagActivity = false;
		ChatHeadService.flagActivityRunning = false;
	}

	private void saveUpdtdShrdPrefVar()
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.DAILY_STICKER_SHARE_COUNT, shareCount);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT, totalShareCount);

	}

	@Override
	public void onBackPressed()
	{
		ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHead.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		if (!flagActivity)
		{
			startActivity(new Intent(this, HomeActivity.class));
			this.finish();
		}
		flagActivity = false;
	}

	OnPageChangeListener onPageChangeListener = new OnPageChangeListener()
	{
		@Override
		public void onPageSelected(int pageNum)
		{
			ViewPager viewPager = (ViewPager) (stickerPickerView.findViewById(R.id.sticker_pager));
			viewPager.setVisibility(View.VISIBLE);
			ImageView imageView = (ImageView) (stickerPickerView.findViewById(R.id.info_icon));
			imageView.setImageResource(R.drawable.infoicon);
			LinearLayout layout = (LinearLayout) (stickerPickerView.findViewById(R.id.info_icon_layout));
			layout.setVisibility(View.GONE);
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2)
		{
		}

		@Override
		public void onPageScrollStateChanged(int arg0)
		{
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		if (!flagActivity)
		{
			startActivity(new Intent(this, HomeActivity.class));
			this.finish();
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_head);
		settingShrdPrefVarFstTym();
		settingDlyChgShrdPrefVar();
		getWindow().getDecorView().setBackgroundColor(R.color.transparent);
		picker = new StickerPicker(R.layout.chat_head_sticker_layout, this, this, null);
		stickerPickerView = picker.getView(getResources().getConfiguration().orientation);
		layout = (LinearLayout) findViewById(R.id.sticker_pallete_other_app);
		View infoIcon = (stickerPickerView.findViewById(R.id.info_icon));
		if (infoIcon != null)
		{
			infoIcon.setOnClickListener(this);
		}
		mIconPageIndicator = (StickerEmoticonIconPageIndicator) stickerPickerView.findViewById(R.id.sticker_icon_indicator);
		mIconPageIndicator.setOnPageChangeListener(onPageChangeListener);
		layout.addView(stickerPickerView);
		if (ChatHeadService.dismissed > maxDismissLimit || shareCount >= shareLimit)
		{
			infoIconClick();
		}
		instance = this;
		setOnClick();
	}

	private void settingDlyChgShrdPrefVar()
	{
		if ((int) ((gettingMidnightTimeinMilliseconds() - (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SERVICE_LAST_USED,
				gettingMidnightTimeinMilliseconds()))) / (24 * 60 * 60 * 1000)) > 0)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SERVICE_LAST_USED, gettingMidnightTimeinMilliseconds());
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.DAILY_STICKER_SHARE_COUNT, 0);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, 0);
		}

		shareCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DAILY_STICKER_SHARE_COUNT, 0);
		noOfDays = (int) ((gettingMidnightTimeinMilliseconds() - (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SERVICE_START_DATE,
				gettingMidnightTimeinMilliseconds()))) / (24 * 60 * 60 * 1000));
		totalShareCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT, 0);
		shareLimit = (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.STICKERS_PER_DAY, HikeConstants.ChatHead.DEFAULT_NO_STICKERS_PER_DAY) + HikeSharedPreferenceUtil
				.getInstance().getData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, 0));
		maxDismissLimit = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DISMISS_COUNT, HikeConstants.ChatHead.DISMISS_CONST);
	}

	private void settingShrdPrefVarFstTym()
	{
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SERVICE_START_DATE, -1L) == -1L)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SERVICE_START_DATE, gettingMidnightTimeinMilliseconds());
		}
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DAILY_STICKER_SHARE_COUNT, -1) == -1)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.DAILY_STICKER_SHARE_COUNT, 0);
		}
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT, -1) == -1)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT, 0);
		}
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, -1) == -1)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.EXTRA_STICKERS_PER_DAY, 0);
		}
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.SERVICE_LAST_USED, -1L) == -1L)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHead.SERVICE_LAST_USED, gettingMidnightTimeinMilliseconds());
		}
	}

	private long gettingMidnightTimeinMilliseconds()
	{
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTimeInMillis();
	}

	@Override
	public void stickerSelected(Sticker sticker, String source)
	{
		if (shareCount < shareLimit)
		{
			shareCount++;
			totalShareCount++;
			String filePath = StickerManager.getInstance().getStickerDirectoryForCategoryId(sticker.getCategoryId()) + HikeConstants.LARGE_STICKER_ROOT;
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

	public static ChatHeadActivity getInstance()
	{
		return instance;
	}

	public void closeActivity(View v)
	{
		ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHead.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION);
	}

	public void infoIconClick()
	{
		LinearLayout disableLayout, infoIconLayout;
		TextView sideText;
		ViewPager viewPager = (ViewPager) (stickerPickerView.findViewById(R.id.sticker_pager));
		viewPager.setVisibility(View.GONE);
		ImageView imageView = (ImageView) (stickerPickerView.findViewById(R.id.info_icon));
		imageView.setImageResource(R.drawable.infoicon_active);
		infoIconLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.info_icon_layout));
		infoIconLayout.setVisibility(View.VISIBLE);
		disableLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.disable_layout));
		disableLayout.setVisibility(View.GONE);
		if (ChatHeadService.dismissed > maxDismissLimit)
		{
			TextView tv = (TextView) (infoIconLayout.findViewById(R.id.disable));
			tv.setTextColor(getResources().getColor(R.color.external_pallete_text_highlight_color));
			ChatHeadService.dismissed = 0;
		}
		else if (shareCount >= shareLimit)
		{
			TextView tv = (TextView) (infoIconLayout.findViewById(R.id.get_more_stickers));
			tv.setTextColor(getResources().getColor(R.color.external_pallete_text_highlight_color));
		}
		initLayoutComponentsView();
		sideText = (TextView) (stickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.side_text));
		sideText.setText(getString(R.string.total_sticker_sent_start) + " " + totalShareCount + " " + getString(R.string.total_sticker_sent_middle) + " " + noOfDays + " "
				+ getString(R.string.total_sticker_sent_end));
	}

	public void onDisableClick()
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
		ChatHeadServiceManager.snooze = true;
		ChatHeadServiceManager.stopService();
		AlarmManager alarmManager = (AlarmManager) (getApplicationContext().getSystemService(getApplicationContext().ALARM_SERVICE));
		Intent mIntent = new Intent(getApplicationContext(), ChatHeadServiceManager.class);
		mIntent.putExtra(HikeConstants.ChatHead.INTENT_EXTRA, HikeConstants.ChatHead.ALARM_BROADCAST);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mIntent, PendingIntent.FLAG_ONE_SHOT);
		alarmManager.set(AlarmManager.RTC, Calendar.getInstance().getTimeInMillis() + time, pendingIntent);
	}

	@Override
	public void onClick(View v)
	{

		switch (v.getId())
		{
		case R.id.info_icon:
			infoIconClick();
			break;
		case R.id.disable:
			onDisableClick();
			break;
		case R.id.get_more_stickers:
			Logger.d("ashish", "getmorestickers");
			break;
		case R.id.open_hike:
			this.finish();
			startActivity(new Intent(getApplicationContext(), HomeActivity.class));
			ChatHeadService.getInstance().setChatHeadInvisible();
			break;
		case R.id.one_hour:
			onClickSetAlarm(1 * 60 * 60 * 1000);
			break;
		case R.id.eight_hours:
			onClickSetAlarm(8 * 60 * 60 * 1000);
			break;
		case R.id.one_day:
			onClickSetAlarm(24 * 60 * 60 * 1000);
			break;
		case R.id.back_main_layout:
			onBackMainLayoutClick();
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

}