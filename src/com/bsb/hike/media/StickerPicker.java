package com.bsb.hike.media;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatHead.ChatHeadActivity;
import com.bsb.hike.chatHead.ChatHeadService;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.TabClickListener;
import com.bsb.hike.models.ProfileItem.ProfileContactItem.contactType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator;

public class StickerPicker implements OnClickListener, ShareablePopup, StickerPickerListener,TabClickListener
{
	private StickerPickerListener listener;

	private Activity mActivity;

	private KeyboardPopupLayout popUpLayout;

	private StickerAdapter stickerAdapter;

	private View viewToDisplay;

	private int mLayoutResId = -1;
	
	private int currentConfig = Configuration.ORIENTATION_PORTRAIT; 
	
	private StickerEmoticonIconPageIndicator mIconPageIndicator;
	
	private static final String TAG = "StickerPicker";
	
	private ViewPager mViewPager;
	
	private boolean refreshStickers = false;
	
	private View chatHeadstickerPickerView;
	
	Context mContext;

	/**
	 * Constructor
	 * 
	 * @param activity
	 * @param listener
	 */
	public StickerPicker(Activity activity, StickerPickerListener listener)
	{
		this.mActivity = activity;
		this.listener = listener;
	}

	/**
	 * Another constructor. The popup layout is passed to this, rather than the picker instantiating one of its own.
	 * 
	 * @param context
	 * @param listener
	 * @param popUpLayout
	 */
	public StickerPicker(int layoutResId, Activity activity, StickerPickerListener listener, KeyboardPopupLayout popUpLayout)
	{
		this(activity, listener);
		this.mLayoutResId = layoutResId;
		this.popUpLayout = popUpLayout;
	}

	/**
	 * The view to display is also passed to this constructor
	 * 
	 * @param view
	 * @param context
	 * @param listener
	 * @param popUpLayout
	 */
	public StickerPicker(View view, Activity activity, StickerPickerListener listener, KeyboardPopupLayout popUpLayout)
	{
		this(activity, listener);
		this.viewToDisplay = view;
		this.popUpLayout = popUpLayout;
		initViewComponents(viewToDisplay);
		Logger.d(TAG, "Sticker Picker instantiated with views");
	}

	/**
	 * Basic constructor. Constructs the popuplayout on its own.
	 * 
	 * @param context
	 * @param listener
	 * @param mainView
	 * @param firstTimeHeight
	 * @param eatTouchEventViewIds
	 */

	public StickerPicker(Activity activity, StickerPickerListener listener, View mainView, int firstTimeHeight, int[] eatTouchEventViewIds)
	{
		this(activity, listener);
		popUpLayout = new KeyboardPopupLayout(mainView, firstTimeHeight, activity.getApplicationContext(), eatTouchEventViewIds, null);
	}

	/**
	 * 
	 * @param context
	 * @param listener
	 * @param mainview
	 *            this is your activity Or fragment root view which gets resized when keyboard toggles
	 * @param firstTimeHeight
	 */
	public StickerPicker(Activity activity, StickerPickerListener listener, View mainView, int firstTimeHeight)
	{
		this(activity, listener, mainView, firstTimeHeight, null);
	}

	public void showStickerPicker(int screenOrietentation)
	{
		showStickerPicker(0, 0, screenOrietentation);
	}

	public void showStickerPicker(int xoffset, int yoffset, int screenOritentation)
	{
		/**
		 * Checking for configuration change
		 */
		if (orientationChanged(screenOritentation))
		{
			resetView();
			currentConfig = screenOritentation;
		}
		
		initView();
		handleStickerIntro(viewToDisplay);
		addAdaptersToViews();

		popUpLayout.showKeyboardPopup(viewToDisplay);
	}

	/**
	 * Used for instantiating the views
	 */
	private void initView()
	{
		if (viewToDisplay != null)
		{
			return;
		}

		/**
		 * Use default view. or the view passed in the constructor
		 */

		mLayoutResId = (mLayoutResId == -1) ? R.layout.sticker_layout : mLayoutResId;

		viewToDisplay = (ViewGroup) LayoutInflater.from(mActivity.getApplicationContext()).inflate(mLayoutResId, null);

		initViewComponents(viewToDisplay);
	}

	/**
	 * Initialises the view components from a given view
	 * 
	 * @param view
	 */
	private void initViewComponents(View view)
	{
		mViewPager = ((ViewPager) view.findViewById(R.id.sticker_pager));

		if (null == mViewPager)
		{
			throw new IllegalArgumentException("View Pager was not found in the view passed.");
		}

		stickerAdapter = new StickerAdapter(mActivity, this);

		mIconPageIndicator = (StickerEmoticonIconPageIndicator) view.findViewById(R.id.sticker_icon_indicator);

		
		View shopIcon = (view.findViewById(R.id.shop_icon));
		
		if(shopIcon != null)
		{
			shopIcon.setOnClickListener(this);
		}

		mViewPager.setVisibility(View.VISIBLE);
	}

	/**
	 * Interface mehtod. Check {@link ShareablePopup}
	 */

	@Override
	public View getView(int screenOritentation)
	{
		/**
		 * Exit condition : If there is no external storage, we return null here. 
		 * Null check is handled where we call getView().
		 */
		if ((Utils.getExternalStorageState() == ExternalStorageState.NONE))
		{
			Toast.makeText(mActivity.getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
			return null;
		}
		
		if (orientationChanged(screenOritentation))
		{
			Logger.i(TAG, "Orientation Changed");
			resetView();
			currentConfig = screenOritentation;
		}
		
		if (viewToDisplay == null)
		{
			/**
			 * Defensive null check
			 */
			if (mActivity == null)
			{
				String errorMsg = "Inside method : getView of StickerPicker. Context is null";
				HAManager.sendStickerEmoticonStrangeBehaviourReport(errorMsg);
				return null;
			}
				
			initView();
		}
		
		handleStickerIntro(viewToDisplay);
		
		addAdaptersToViews();
		
		return viewToDisplay;
	}
	
	private void addAdaptersToViews()
	{
		mViewPager.setAdapter(stickerAdapter);

		mIconPageIndicator.setViewPager(mViewPager);

		mIconPageIndicator.setOnPageChangeListener(onPageChangeListener);

		mIconPageIndicator.setCurrentItem(0);
		
		if (refreshStickers)
		{
			mIconPageIndicator.notifyDataSetChanged();
			refreshStickers = false;
		}
	}

	public boolean isShowing()
	{
		return popUpLayout.isShowing();
	}

	@Override
	public void onClick(View arg0)
	{
		switch (arg0.getId())
		{
		case R.id.shop_icon:
			// shop icon clicked
			shopIconClicked();
			break;
		case R.id.info_icon:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.INFOICON_CLICK, ChatHeadService.foregroundAppName);
			infoIconClick();
			break;
		case R.id.disable:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.MAIN_LAYOUT_CLICKS, ChatHeadService.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.DISABLE_SETTING);
			onDisableClick();
			break;
		case R.id.get_more_stickers:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.MAIN_LAYOUT_CLICKS, ChatHeadService.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.MORE_STICKERS);
			ChatHeadService.getInstance().resetPosition(ChatHeadUtils.GET_MORE_STICKERS_ANIMATION);
			break;
		case R.id.open_hike:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.MAIN_LAYOUT_CLICKS, ChatHeadService.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.OPEN_HIKE);
			ChatHeadService.getInstance().resetPosition(ChatHeadUtils.OPEN_HIKE_ANIMATION);
			break;
		case R.id.one_hour:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.SNOOZE_TIME, ChatHeadService.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.ONE_HOUR);
			ChatHeadUtils.onClickSetAlarm(mContext, 1 * 60 * 60 * 1000);
			break;
		case R.id.eight_hours:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.SNOOZE_TIME, ChatHeadService.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.EIGHT_HOURS);
			ChatHeadUtils.onClickSetAlarm(mContext, 8 * 60 * 60 * 1000);
			break;
		case R.id.one_day:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.SNOOZE_TIME, ChatHeadService.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.ONE_DAY);
			ChatHeadUtils.onClickSetAlarm(mContext, 24 * 60 * 60 * 1000);
			break;
		case R.id.back_main_layout:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.BACK, ChatHeadService.foregroundAppName);
			onBackMainLayoutClick();
			break;
		case R.id.shop_icon_external:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.STICKER_SHOP, ChatHeadService.foregroundAppName);
			ChatHeadService.getInstance().resetPosition(ChatHeadUtils.STICKER_SHOP_ANIMATION);
			break;
		}
	}

	private void shopIconClicked()
	{
		setStickerIntroPrefs();
		HAManager.getInstance().record(HikeConstants.LogEvent.STKR_SHOP_BTN_CLICKED, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT);
		Intent i = IntentFactory.getStickerShopIntent(mActivity);
		mActivity.startActivity(i);
	}

	public void updateDimension(int width, int height)
	{
		popUpLayout.updateDimension(width, height);
	}

	public void dismiss()
	{
		popUpLayout.dismiss();
	}

	OnPageChangeListener onPageChangeListener = new OnPageChangeListener()
	{

		@Override
		public void onPageSelected(int pageNum)
		{
			StickerCategory category = stickerAdapter.getCategoryForIndex(pageNum);
			/**
			 * If the category has been downloaded/updated from the sticker pallete/shop/settings page and the user has now seen it's done state, so we reset it.
			 */
			if (category.getState() == StickerCategory.DONE || category.getState() == StickerCategory.DONE_SHOP_SETTINGS)
			{
				category.setState(StickerCategory.NONE);
			}
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

	/**
	 * Interface method. Check {@link ShareablePopup}
	 */

	@Override
	public int getViewId()
	{
		return viewToDisplay.getId();
	}

	/**
	 * Utility method to free up resources
	 */
	public void releaseResources()
	{
		this.mActivity = null;
		this.listener = null;
		if (stickerAdapter != null)
		{
			stickerAdapter.unregisterListeners();
		}

	}
	
	public void updateListener(StickerPickerListener mListener, Activity activity)
	{
		this.listener = mListener;
		this.mActivity = activity;
		if (stickerAdapter != null)
		{
			stickerAdapter.registerListener();
		}
	}
	
	private void updateStickerAdapter()
	{
		if (stickerAdapter != null)
		{
			stickerAdapter.instantiateStickerList();
			stickerAdapter.notifyDataSetChanged();
		}
	}
	
	public void setExitTasksEarly(boolean flag)
	{
		if (stickerAdapter != null)
		{
			stickerAdapter.getStickerLoader().setExitTasksEarly(flag);
			stickerAdapter.getStickerOtherIconLoader().setExitTasksEarly(flag);
			if (!flag)
			{
				stickerAdapter.notifyDataSetChanged();
			}
		}
	}
	
	private void updateIconPageIndicator()
	{
		if (mIconPageIndicator != null)
		{
			mIconPageIndicator.notifyDataSetChanged();
		}
	}
	
	public void notifyDataSetChanged()
	{
		updateIconPageIndicator();
		updateStickerAdapter();
	}

	@Override
	public void stickerSelected(Sticker sticker, String source)
	{
		if (listener != null)
		{
			listener.stickerSelected(sticker, source);
		}
	}
	
	/**
	 * This method is used to handle any sort of animations on sticker shop icon or showing the red badges or in future any FTUE related changes to Sticker shop Icon
	 * @param view
	 */
	private void handleStickerIntro(View view)
	{
		View shopIconBadge = view.findViewById(R.id.shop_icon_badge);
		
		if(shopIconBadge != null)
		{
			// show sticker shop badge on shop icon
			shopIconBadge.setVisibility(HikeSharedPreferenceUtil.getInstance().getData(StickerManager.SHOW_STICKER_SHOP_BADGE, false)
					? View.VISIBLE : View.GONE);
		}
		
		View shopIconImage = view.findViewById(R.id.shop_icon_image);
		//The shop icon would be blue unless the user clicks on it once
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_SHOP_ICON_BLUE, false) && shopIconImage != null)
		{
			View animatedBackground = view.findViewById(R.id.animated_backgroud);
			
			if(animatedBackground != null)
			{
				animatedBackground.setVisibility(View.VISIBLE);
				Animation anim = AnimationUtils.loadAnimation(mActivity, R.anim.scale_out_from_mid);
				animatedBackground.startAnimation(anim);
			}

			shopIconImage.setAnimation(HikeAnimationFactory.getStickerShopIconAnimation(mActivity));
		}
	}
	
	/**
	 * Used to set preferences related to Sticker Views.
	 */
	
	private void setStickerIntroPrefs()
	{
		if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_SHOP_ICON_BLUE, false)) // The shop icon would be blue unless the
																																			// user clicks
		// on it once
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_SHOP_ICON_BLUE, true);

			View animatedBackground = viewToDisplay.findViewById(R.id.animated_backgroud);
			animatedBackground.setVisibility(View.GONE);
			animatedBackground.clearAnimation();
			viewToDisplay.findViewById(R.id.shop_icon).clearAnimation();

		}

		if (HikeSharedPreferenceUtil.getInstance().getData(StickerManager.SHOW_STICKER_SHOP_BADGE, false)) // The shop icon would be blue unless the
																																			// user clicks
		// on it once
		{
			HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.SHOW_STICKER_SHOP_BADGE, false);
			viewToDisplay.findViewById(R.id.shop_icon_badge).setVisibility(View.GONE);
		}
	}
	
	private void resetView()
	{
		viewToDisplay = null;
		stickerAdapter = null;
	}
	
	private boolean orientationChanged(int deviceOrientation)
	{
		return currentConfig != deviceOrientation;
	}

	/**
	 * Used for indicating to the sticker picker to refresh its underlying dataset
	 * 
	 * @param refreshStickers
	 *            the refreshStickers to set
	 */
	public void setRefreshStickers(boolean refreshStickers)
	{
		this.refreshStickers = refreshStickers;
	}
	
	public void infoIconClick()
	{
		LinearLayout disableLayout, infoIconLayout;
		TextView sideText;
		ViewPager viewPager = (ViewPager) (chatHeadstickerPickerView.findViewById(R.id.sticker_pager));
		viewPager.setVisibility(View.GONE);
		ImageView imageView = (ImageView) (chatHeadstickerPickerView.findViewById(R.id.info_icon));
		imageView.setSelected(true);
		infoIconLayout = (LinearLayout) (chatHeadstickerPickerView.findViewById(R.id.info_icon_layout));
		infoIconLayout.setVisibility(View.VISIBLE);
		mIconPageIndicator = (StickerEmoticonIconPageIndicator) chatHeadstickerPickerView.findViewById(R.id.sticker_icon_indicator);
		mIconPageIndicator.unselectCurrent();
		disableLayout = (LinearLayout) (chatHeadstickerPickerView.findViewById(R.id.disable_layout));
		disableLayout.setVisibility(View.GONE);
		if (ChatHeadService.dismissed > ChatHeadActivity.maxDismissLimit)
		{
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.INFOICON_WITHOUT_CLICK, ChatHeadService.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.DISMISS_LIMIT);
			TextView tv = (TextView) (infoIconLayout.findViewById(R.id.disable));
			tv.setTextColor(mContext.getResources().getColor(R.color.external_pallete_text_highlight_color));
			ChatHeadService.dismissed = 0;

		}
		else if (ChatHeadActivity.shareCount >= ChatHeadActivity.shareLimit)
		{
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.INFOICON_WITHOUT_CLICK, ChatHeadService.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.SHARE_LIMIT);
			TextView tv = (TextView) (infoIconLayout.findViewById(R.id.get_more_stickers));
			tv.setTextColor(mContext.getResources().getColor(R.color.external_pallete_text_highlight_color));
		}
		initLayoutComponentsView();
		sideText = (TextView) (chatHeadstickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.side_text));
		sideText.setText(mContext.getString(R.string.total_sticker_sent_start) + " " + ChatHeadActivity.totalShareCount + " "
				+ mContext.getString(R.string.total_sticker_sent_middle) + " " + ChatHeadActivity.noOfDays + " " + mContext.getString(R.string.total_sticker_sent_end));
	}

	private void initLayoutComponentsView()
	{
		LinearLayout layout = (LinearLayout) (chatHeadstickerPickerView.findViewById(R.id.main_layout));
		layout.setVisibility(View.VISIBLE);
		layout.findViewById(R.id.disable).setOnClickListener(this);
		TextView tv = (TextView) (chatHeadstickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.main_text));
		tv.setText(ChatHeadActivity.shareCount + "/" + ChatHeadActivity.shareLimit + " " + mContext.getString(R.string.stickers_sent_today));
		ProgressBar progressBar = (ProgressBar) (chatHeadstickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.progress_bar));
		int progress;
		if (ChatHeadActivity.shareLimit != 0)
		{
			progress = (int) ((ChatHeadActivity.shareCount * 100) / ChatHeadActivity.shareLimit);
		}
		else
		{
			progress = 0;
		}
		progressBar.setProgress(progress);
	}

	private void onDisableClick()
	{
		LinearLayout mainLayout, disableLayout;
		TextView sideText;
		mainLayout = (LinearLayout) (chatHeadstickerPickerView.findViewById(R.id.main_layout));
		mainLayout.setVisibility(View.GONE);
		disableLayout = (LinearLayout) (chatHeadstickerPickerView.findViewById(R.id.disable_layout));
		disableLayout.setVisibility(View.VISIBLE);
		sideText = (TextView) (chatHeadstickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.side_text));
		sideText.setText(mContext.getString(R.string.disable_from_hike_settings));
	}

	private void onBackMainLayoutClick()
	{
		LinearLayout mainLayout, disableLayout;
		TextView sideText;
		mainLayout = (LinearLayout) (chatHeadstickerPickerView.findViewById(R.id.main_layout));
		mainLayout.setVisibility(View.VISIBLE);
		disableLayout = (LinearLayout) (chatHeadstickerPickerView.findViewById(R.id.disable_layout));
		disableLayout.setVisibility(View.GONE);
		sideText = (TextView) (chatHeadstickerPickerView.findViewById(R.id.info_icon_layout).findViewById(R.id.side_text));
		sideText.setText(mContext.getString(R.string.total_sticker_sent_start) + " " + ChatHeadActivity.totalShareCount + " "
				+ mContext.getString(R.string.total_sticker_sent_middle) + " " + ChatHeadActivity.noOfDays + " " + mContext.getString(R.string.total_sticker_sent_end));

	}

	public void setOnClick()
	{
		LinearLayout layout = (LinearLayout) (chatHeadstickerPickerView.findViewById(R.id.info_icon_layout));
		layout.findViewById(R.id.get_more_stickers).setOnClickListener(this);
		layout.findViewById(R.id.disable).setOnClickListener(this);
		layout.findViewById(R.id.open_hike).setOnClickListener(this);
		layout.findViewById(R.id.back_main_layout).setOnClickListener(this);
		layout.findViewById(R.id.one_day).setOnClickListener(this);
		layout.findViewById(R.id.one_hour).setOnClickListener(this);
		layout.findViewById(R.id.eight_hours).setOnClickListener(this);
		View shopIcon = (chatHeadstickerPickerView.findViewById(R.id.shop_icon_external));
		shopIcon.setOnClickListener(this);
	}

	public void onCreatingChatHeadActivity(Context context, LinearLayout layout)
	{
		mContext = context;
		chatHeadstickerPickerView = getView(context.getResources().getConfiguration().orientation);
		View infoIcon = (chatHeadstickerPickerView.findViewById(R.id.info_icon));
		if (infoIcon != null)
		{
			infoIcon.setOnClickListener(this);
		}
		layout.addView(chatHeadstickerPickerView);
		if (ChatHeadService.dismissed > ChatHeadActivity.maxDismissLimit || ChatHeadActivity.shareCount >= ChatHeadActivity.shareLimit)
		{
			infoIconClick();
		}
		setOnClick();
		StickerEmoticonIconPageIndicator.registerChatHeadTabClickListener(this);
	}

	@Override
	public void onTabClick()
	{
		ViewPager viewPager = (ViewPager) (chatHeadstickerPickerView.findViewById(R.id.sticker_pager));
		viewPager.setVisibility(View.VISIBLE);
		ImageView imageView = (ImageView) (chatHeadstickerPickerView.findViewById(R.id.info_icon));
		imageView.setSelected(false);
		LinearLayout layout = (LinearLayout) (chatHeadstickerPickerView.findViewById(R.id.info_icon_layout));
		layout.setVisibility(View.GONE);
	}

	public void stoppingChatHeadActivity()
	{
		StickerEmoticonIconPageIndicator.unRegisterChatHeadTabClickListener();
	}

}
