package com.bsb.hike.media;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.bsb.hike.chatHead.ChatHeadConstants;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.chatHead.ChatHeadViewManager;
import com.bsb.hike.chatHead.TabClickListener;
import com.bsb.hike.chatthread.IShopIconClickedCallback;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.StickerIconPageIndicator;

public class StickerPicker implements OnClickListener, ShareablePopup, StickerPickerListener,TabClickListener
{
	private StickerPickerListener listener;

	private KeyboardPopupLayout popUpLayout;

	private StickerAdapter stickerAdapter;

	private View viewToDisplay;

	private int mLayoutResId = -1;
	
	private int currentConfig = Configuration.ORIENTATION_PORTRAIT; 
	
	private StickerIconPageIndicator mIconPageIndicator;

	private static final String TAG = "StickerPicker";
	
	private ViewPager mViewPager;
	
	private boolean refreshStickers = false;
	
	private View chatHeadstickerPickerView;
	
	private Context mContext;
	
	private TextView  chatHeadDisableButton, chatHeadgetMoreStickersButton, chatHeadDisableSideText, chatHeadTotalStickersText, chatHeadMainText;
	
	private LinearLayout chatHeadMainLayout, chatHeadInfoIconLayout, chatHeadDisableLayout;
	
	private ImageView chatHeadInfoIconButton;

	private ProgressBar chatHeadProgressBar;

	private boolean showLastCategory;

	private IShopIconClickedCallback shopIconClickedCallback;

	private StickerCategory quickSuggetionCategory;

	private boolean showQuickSuggestions;
	
	/**
	 * Constructor
	 * 
	 * @param activity
	 * @param listener
	 */
	public StickerPicker(Context context, StickerPickerListener listener)
	{
		this.mContext = context;
		this.listener = listener;
		this.currentConfig = context.getResources().getConfiguration().orientation;
	}

	/**
	 * Constructor
	 *
	 * @param activity
	 * @param listener
	 */
	public StickerPicker(Context context, StickerPickerListener listener, IShopIconClickedCallback shopIconClickedCallback)
	{
		this.mContext = context;
		this.listener = listener;
		this.currentConfig = context.getResources().getConfiguration().orientation;
		this.shopIconClickedCallback = shopIconClickedCallback;
	}

	/**
	 * Another constructor. The popup layout is passed to this, rather than the picker instantiating one of its own.
	 * 
	 * @param context
	 * @param listener
	 * @param popUpLayout
	 */
	public StickerPicker(int layoutResId, Context context, StickerPickerListener listener, KeyboardPopupLayout popUpLayout)
	{
		this(context, listener);
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
	public StickerPicker(View view, Context context, StickerPickerListener listener, KeyboardPopupLayout popUpLayout)
	{
		this(context, listener);
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

	public StickerPicker(Context context, StickerPickerListener listener, View mainView, int firstTimeHeight, int[] eatTouchEventViewIds)
	{
		this(context, listener);
		popUpLayout = new KeyboardPopupLayout(mainView, firstTimeHeight, context.getApplicationContext(), eatTouchEventViewIds, null);
	}

	/**
	 * 
	 * @param context
	 * @param listener
	 * @param mainview
	 *            this is your activity Or fragment root view which gets resized when keyboard toggles
	 * @param firstTimeHeight
	 */
	public StickerPicker(Context context, StickerPickerListener listener, View mainView, int firstTimeHeight)
	{
		this(context, listener, mainView, firstTimeHeight, null);
	}

	public void showStickerPicker(int screenOrietentation)
	{
		showStickerPicker(0, 0, screenOrietentation);
	}

	public void setShowLastCategory(boolean showLastCategory)
	{
		this.showLastCategory = showLastCategory;
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

		viewToDisplay = (ViewGroup) LayoutInflater.from(mContext.getApplicationContext()).inflate(mLayoutResId, null);

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

		initStickerAdapter();

		mIconPageIndicator = (StickerIconPageIndicator) view.findViewById(R.id.sticker_icon_indicator);
		
		View shopIcon = (view.findViewById(R.id.shop_icon));
		if (shopIcon != null)
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
			Toast.makeText(mContext.getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
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
			if (mContext == null)
			{
				String errorMsg = "Inside method : getView of StickerPicker. Context is null";
				HAManager.sendStickerEmoticonStrangeBehaviourReport(errorMsg);
				return null;
			}
				
			initView();
		}
		
		if (mLayoutResId != R.layout.chat_head_sticker_layout)
		{
			handleStickerIntro(viewToDisplay);
		}
		addAdaptersToViews();
		
		return viewToDisplay;
	}
	
	private void addAdaptersToViews()
	{
		addOrRemoveQuickSuggestionCategory();

		mViewPager.setAdapter(stickerAdapter);

		mIconPageIndicator.setViewPager(mViewPager);

		mIconPageIndicator.setOnPageChangeListener(onPageChangeListener);

		mIconPageIndicator.setCurrentItem(0);

		if(showLastCategory)
		{
			mIconPageIndicator.setCurrentItem(stickerAdapter.getCount());
			setShowLastCategory(false);
		}
		else
		{
			mIconPageIndicator.setCurrentItem(0);
		}
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
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.INFOICON_CLICK, ChatHeadViewManager.foregroundAppName);
			infoIconClick();
			break;
		case R.id.disable:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.MAIN_LAYOUT_CLICKS, ChatHeadViewManager.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.DISABLE_SETTING);
			onDisableClick();
			break;
		case R.id.get_more_stickers:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.MAIN_LAYOUT_CLICKS, ChatHeadViewManager.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.MORE_STICKERS);
			ChatHeadViewManager.getInstance(mContext).resetPosition(ChatHeadConstants.GET_MORE_STICKERS_ANIMATION, null);
			break;
		case R.id.open_hike:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.MAIN_LAYOUT_CLICKS, ChatHeadViewManager.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.OPEN_HIKE);
			ChatHeadViewManager.getInstance(mContext).resetPosition(ChatHeadConstants.OPEN_HIKE_ANIMATION, null);
			break;
		case R.id.one_hour:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.SNOOZE_TIME, ChatHeadViewManager.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.ONE_HOUR);
			ChatHeadUtils.onClickSetAlarm(mContext, 1 * ChatHeadConstants.HOUR_TO_MILLISEC_CONST);
			break;
		case R.id.eight_hours:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.SNOOZE_TIME, ChatHeadViewManager.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.EIGHT_HOURS);
			ChatHeadUtils.onClickSetAlarm(mContext, 8 * ChatHeadConstants.HOUR_TO_MILLISEC_CONST);
			break;
		case R.id.one_day:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.SNOOZE_TIME, ChatHeadViewManager.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.ONE_DAY);
			ChatHeadUtils.onClickSetAlarm(mContext, 24 * ChatHeadConstants.HOUR_TO_MILLISEC_CONST);
			break;
		case R.id.back_main_layout:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.BACK, ChatHeadViewManager.foregroundAppName);
			onBackMainLayoutClick();
			break;
		case R.id.shop_icon_external:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.STICKER_SHOP, ChatHeadViewManager.foregroundAppName);
			ChatHeadViewManager.getInstance(mContext).resetPosition(ChatHeadConstants.STICKER_SHOP_ANIMATION, null);
			break;
		case R.id.disable_side_text:
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.DISABLE_TEXT, ChatHeadViewManager.foregroundAppName);
			ChatHeadViewManager.getInstance(mContext).resetPosition(ChatHeadConstants.OPEN_SETTINGS_ANIMATION, null);
			break;
		}
	}

	private void shopIconClicked()
	{
		setStickerIntroPrefs();
		if (shopIconClickedCallback!= null)
		{
			shopIconClickedCallback.shopClicked();
		}
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
		this.mContext = null;
		this.listener = null;
		if (stickerAdapter != null)
		{
			stickerAdapter.unregisterListeners();
		}

	}
	
	public void updateListener(StickerPickerListener mListener, Context context)
	{
		this.listener = mListener;
		this.mContext = context;
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
	{   if (listener != null)
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
		if (HikeSharedPreferenceUtil.getInstance().getData(StickerManager.SHOW_STICKER_SHOP_BADGE, false))
		{
			// show sticker shop badge on shop icon
			view.findViewById(R.id.shop_icon_badge).setVisibility(View.VISIBLE);
		}
		else
		{
			view.findViewById(R.id.shop_icon_badge).setVisibility(View.GONE);
		}
		
		
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_SHOP_ICON_BLUE, false))  //The shop icon would be blue unless the user clicks on it once
		{
			View animatedBackground = view.findViewById(R.id.animated_backgroud);
			
			animatedBackground.setVisibility(View.VISIBLE);
			Animation anim = AnimationUtils.loadAnimation(mContext, R.anim.scale_out_from_mid);
			animatedBackground.startAnimation(anim);

			view.findViewById(R.id.shop_icon_image).setAnimation(HikeAnimationFactory.getStickerShopIconAnimation(mContext));
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
		mViewPager.setVisibility(View.GONE);
		chatHeadInfoIconButton.setSelected(true);
		chatHeadInfoIconLayout.setVisibility(View.VISIBLE);
		mIconPageIndicator.unselectCurrent();
		chatHeadDisableLayout.setVisibility(View.GONE);
		if (ChatHeadViewManager.dismissed > HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DISMISS_COUNT, ChatHeadConstants.DISMISS_CONST))
		{
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.INFOICON_WITHOUT_CLICK, ChatHeadViewManager.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.DISMISS_LIMIT);
			chatHeadDisableButton.setTextColor(mContext.getResources().getColor(R.color.external_pallete_text_highlight_color));
			ChatHeadViewManager.dismissed = 0;

		}
		else if (HikeSharedPreferenceUtil.getInstance().getData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, 0) >= ChatHeadUtils.shareLimit)
		{
			HAManager.getInstance().chatHeadshareAnalytics(AnalyticsConstants.ChatHeadEvents.INFOICON_WITHOUT_CLICK, ChatHeadViewManager.foregroundAppName,
					AnalyticsConstants.ChatHeadEvents.SHARE_LIMIT);
			chatHeadgetMoreStickersButton.setTextColor(mContext.getResources().getColor(R.color.external_pallete_text_highlight_color));
		}
		initLayoutComponentsView();
		chatHeadDisableSideText.setVisibility(View.GONE);
		chatHeadTotalStickersText.setVisibility(View.VISIBLE);
		chatHeadTotalStickersText.setText(String.format(mContext.getString(R.string.total_sticker_sent), HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT, 0), ChatHeadUtils.noOfDays));
		// we are doing this because we need to consume this touch event here and don't want to pass further
	    chatHeadInfoIconLayout.setOnTouchListener(new View.OnTouchListener()
		{	
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				Logger.d(TAG, "accepting user touch");
				return true;
			}
		});
	    
	}

	private void initLayoutComponentsView()
	{
		chatHeadMainLayout.setVisibility(View.VISIBLE);
		chatHeadMainText.setText(String.format(mContext.getString(R.string.stickers_sent_today), HikeSharedPreferenceUtil.getInstance().getData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, 0), ChatHeadUtils.shareLimit));
		int progress;
		if (ChatHeadUtils.shareLimit != 0)
		{
			progress = (int) ((HikeSharedPreferenceUtil.getInstance().getData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, 0) * 100) / ChatHeadUtils.shareLimit);
		}
		else
		{
			progress = 0;
		}
		chatHeadProgressBar.setProgress(progress);
	}

	private void onDisableClick()
	{
		chatHeadMainLayout.setVisibility(View.GONE);
		chatHeadDisableLayout.setVisibility(View.VISIBLE);
		chatHeadTotalStickersText.setVisibility(View.GONE);         
		chatHeadDisableSideText.setVisibility(View.VISIBLE);
	}

	private void onBackMainLayoutClick()
	{
		chatHeadMainLayout.setVisibility(View.VISIBLE);
		chatHeadDisableLayout.setVisibility(View.GONE);
		chatHeadDisableSideText.setVisibility(View.GONE);
		chatHeadTotalStickersText.setVisibility(View.VISIBLE);
		chatHeadTotalStickersText.setText(String.format(mContext.getString(R.string.total_sticker_sent), HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.TOTAL_STICKER_SHARE_COUNT, 0), ChatHeadUtils.noOfDays));
	}

	public void setOnClick()
	{
		chatHeadInfoIconButton.setOnClickListener(this);
		chatHeadgetMoreStickersButton.setOnClickListener(this);
		chatHeadDisableButton.setOnClickListener(this);
		chatHeadDisableSideText.setOnClickListener(this);
		chatHeadstickerPickerView.findViewById(R.id.open_hike).setOnClickListener(this);
		chatHeadstickerPickerView.findViewById(R.id.back_main_layout).setOnClickListener(this);
		chatHeadstickerPickerView.findViewById(R.id.one_day).setOnClickListener(this);
		chatHeadstickerPickerView.findViewById(R.id.one_hour).setOnClickListener(this);
		chatHeadstickerPickerView.findViewById(R.id.eight_hours).setOnClickListener(this);
		chatHeadstickerPickerView.findViewById(R.id.shop_icon_external).setOnClickListener(this);
		
	}

	public void onCreatingChatHeadActivity(Context context, LinearLayout layout)
	{
		mContext = context;
		chatHeadstickerPickerView = getView(context.getResources().getConfiguration().orientation);
		findindViewById();
		layout.addView(chatHeadstickerPickerView);
		if (ChatHeadViewManager.dismissed > HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.DISMISS_COUNT, ChatHeadConstants.DISMISS_CONST) || HikeSharedPreferenceUtil.getInstance().getData(ChatHeadConstants.DAILY_STICKER_SHARE_COUNT, 0) >= ChatHeadUtils.shareLimit)
		{
			infoIconClick();
		}
		setOnClick();
		StickerIconPageIndicator.registerChatHeadTabClickListener(this);
	}
	
	public void createExternalStickerPicker(LinearLayout layout)
	{
//		chatHeadstickerPickerView = getView(context.getResources().getConfiguration().orientation);
//		findindViewById();
//		layout.addView(chatHeadstickerPickerView);
//		if (ChatHeadViewManager.dismissed > ChatHeadUtils.maxDismissLimit || ChatHeadUtils.shareCount >= ChatHeadUtils.shareLimit)
//		{
//			infoIconClick();
//		}
//		setOnClick();
//		StickerEmoticonIconPageIndicator.registerChatHeadTabClickListener(this);	
	}

	private void findindViewById()
	{
		chatHeadDisableButton = (TextView)chatHeadstickerPickerView.findViewById(R.id.disable);
		chatHeadgetMoreStickersButton = (TextView)chatHeadstickerPickerView.findViewById(R.id.get_more_stickers);
		chatHeadInfoIconButton = (ImageView) chatHeadstickerPickerView.findViewById(R.id.info_icon);
		chatHeadInfoIconLayout = (LinearLayout)chatHeadstickerPickerView.findViewById(R.id.info_icon_layout);
		mViewPager = (ViewPager) chatHeadstickerPickerView.findViewById(R.id.sticker_pager);
		chatHeadMainLayout = (LinearLayout)chatHeadstickerPickerView.findViewById(R.id.main_layout);
		chatHeadDisableLayout = (LinearLayout)chatHeadstickerPickerView.findViewById(R.id.disable_layout);
		chatHeadDisableSideText = (TextView)chatHeadstickerPickerView.findViewById(R.id.disable_side_text);
		chatHeadTotalStickersText = (TextView)chatHeadstickerPickerView.findViewById(R.id.sticker_sent_side_text);
		chatHeadMainText  = (TextView)chatHeadstickerPickerView.findViewById(R.id.main_text);
		chatHeadProgressBar = (ProgressBar)chatHeadstickerPickerView.findViewById(R.id.progress_bar);
		mIconPageIndicator = (StickerIconPageIndicator) chatHeadstickerPickerView.findViewById(R.id.sticker_icon_indicator);
		
	}

	@Override
	public void onTabClick()
	{
		mViewPager.setVisibility(View.VISIBLE);
		chatHeadInfoIconButton.setSelected(false);
		chatHeadInfoIconLayout.setVisibility(View.GONE);
	}

	public void stoppingChatHeadActivity()
	{
		StickerIconPageIndicator.unRegisterChatHeadTabClickListener();
		releaseResources();
	}

	public void resetPostionAfterSharing(String filePathBmp)
	{
		ChatHeadViewManager.getInstance(HikeMessengerApp.getInstance()).resetPosition(ChatHeadConstants.SHARING_BEFORE_FINISHING_ANIMATION, filePathBmp);
	}

	private void initStickerAdapter()
	{
		stickerAdapter = stickerAdapter == null ? new StickerAdapter(mContext, this) : stickerAdapter;
	}

	private void addOrRemoveQuickSuggestionCategory()
	{
		if(showQuickSuggestions && quickSuggetionCategory != null)
		{
			stickerAdapter.addQuickSuggestionCategory(quickSuggetionCategory);
			showQuickSuggestions = false;
			refreshStickers = true;
		}
		else
		{
			refreshStickers = stickerAdapter.removeQuickSuggestionCategory();
		}
	}

	public void showQuickSuggestionCategory(StickerCategory quickSuggestionCategory)
	{
		this.showQuickSuggestions = true;
		this.quickSuggetionCategory = quickSuggestionCategory;
	}
}
