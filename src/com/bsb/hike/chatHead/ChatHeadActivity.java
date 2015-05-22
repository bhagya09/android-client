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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

	static String date = "0", month = "0";

	static int shareCount = 0;

	static int totalShareCount = 0;

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
		Logger.d("ashish", "ondestroy");
		super.onDestroy();
		flagActivity = false;
		ChatHeadService.flagActivityRunning = false;
	}

	@Override
	public void onBackPressed()
	{
		ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHeadService.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION);
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
		Logger.d("ashish", "oncreate");
		if (!flagActivity)
		{
			startActivity(new Intent(this, HomeActivity.class));
			this.finish();
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_head);
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
		instance = this;

		Calendar c = Calendar.getInstance();
		String time = c.getTime().toString();
		if (!month.equals(time.split(" ")[1]) || !date.equals(time.split(" ")[2]))

		{
			month = time.split(" ")[1];
			date = time.split(" ")[2];
			shareCount = 0;
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHeadService.EXTRA_STICKERS_PER_DAY, 0);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHeadService.TOTAL_STICKER_SHARE_COUNT,
					HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHeadService.TOTAL_STICKER_SHARE_COUNT, 0));
		}
		setOnClick();
	}

	@Override
	public void stickerSelected(Sticker sticker, String source)
	{
		if (shareCount < (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHeadService.STICKERS_PER_DAY, HikeConstants.ChatHeadService.DEFAULT_NO_STICKERS_PER_DAY) + HikeSharedPreferenceUtil
				.getInstance().getData(HikeConstants.ChatHeadService.EXTRA_STICKERS_PER_DAY, 0)))
		{
			shareCount++;
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ChatHeadService.TOTAL_STICKER_SHARE_COUNT,
					HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHeadService.TOTAL_STICKER_SHARE_COUNT, 0) + 1);
			String filePath = StickerManager.getInstance().getStickerDirectoryForCategoryId(sticker.getCategoryId()) + HikeConstants.LARGE_STICKER_ROOT;
			File stickerFile = new File(filePath, sticker.getStickerId());
			String filePathBmp = stickerFile.getAbsolutePath();
			ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHeadService.SHARING_BEFORE_FINISHING_ANIMATION, filePathBmp);
		}
		else
		{
			this.finish();
		}

	}

	public static ChatHeadActivity getInstance()
	{
		return instance;
	}

	public void closeActivity(View v)
	{
		ChatHeadService.getInstance().resetPosition(HikeConstants.ChatHeadService.FINISHING_CHAT_HEAD_ACTIVITY_ANIMATION);
	}

	@Override
	public void onClick(View v)
	{
		LinearLayout mainLayout, disableLayout, infoIconLayout;
		TextView sideText;
		AlarmManager alarmManager = (AlarmManager) (getApplicationContext().getSystemService(getApplicationContext().ALARM_SERVICE));
		Intent mIntent = new Intent(getApplicationContext(), ChatHeadServiceManager.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mIntent, PendingIntent.FLAG_ONE_SHOT);

		switch (v.getId())
		{
		case R.id.info_icon:
			ViewPager viewPager = (ViewPager) (stickerPickerView.findViewById(R.id.sticker_pager));
			viewPager.setVisibility(View.GONE);
			ImageView imageView = (ImageView) (stickerPickerView.findViewById(R.id.info_icon));
			imageView.setImageResource(R.drawable.infoicon_active);
			infoIconLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.info_icon_layout));
			infoIconLayout.setVisibility(View.VISIBLE);
			disableLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.disable_layout));
			disableLayout.setVisibility(View.GONE);
			initLayoutComponentsView();
			sideText = (TextView) (stickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.side_text));
			sideText.setText("You sent 34 stickers in the last 8 days");
			break;
		case R.id.disable:
			mainLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.main_layout));
			mainLayout.setVisibility(View.GONE);
			disableLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.disable_layout));
			disableLayout.setVisibility(View.VISIBLE);
			sideText = (TextView) (stickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.side_text));
			sideText.setText("Disable permanently from hike settings");
			break;
		case R.id.get_more_stickers:
			Logger.d("ashish", "getmorestickers");
			break;
		case R.id.open_hike:
			ChatHeadService.getInstance().setChatHeadInvisible();
			startActivity(new Intent(getApplicationContext(), HomeActivity.class));
			break;
		case R.id.one_hour:
			mIntent.putExtra(HikeConstants.ChatHeadService.INTENT_EXTRA, 1 * 60 * 60 * 1000);
			alarmManager.set(AlarmManager.RTC, 1, pendingIntent);
			break;
		case R.id.eight_hours:
			mIntent.putExtra(HikeConstants.ChatHeadService.INTENT_EXTRA, 8 * 60 * 60 * 1000);
			alarmManager.set(AlarmManager.RTC, 1, pendingIntent);
			break;
		case R.id.one_day:
			mIntent.putExtra(HikeConstants.ChatHeadService.INTENT_EXTRA, 24 * 60 * 60 * 1000);
			alarmManager.set(AlarmManager.RTC, 1, pendingIntent);
			break;
		case R.id.back_main_layout:
			mainLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.main_layout));
			mainLayout.setVisibility(View.VISIBLE);
			disableLayout = (LinearLayout) (stickerPickerView.findViewById(R.id.disable_layout));
			disableLayout.setVisibility(View.GONE);
			sideText = (TextView) (stickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.side_text));
			sideText.setText("You sent ");
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
		int totalShareLimit = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHeadService.STICKERS_PER_DAY,
				HikeConstants.ChatHeadService.DEFAULT_NO_STICKERS_PER_DAY)
				+ HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHeadService.EXTRA_STICKERS_PER_DAY, 0);
		TextView tv = (TextView) (stickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.main_text));
		tv.setText(shareCount + "/" + totalShareLimit + " stickers sent today");
		ProgressBar progressBar = (ProgressBar) (stickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.progress_bar));
		int progress = (int) ((shareCount * 100) / totalShareLimit);
		progressBar.setProgress(progress);
	}

}