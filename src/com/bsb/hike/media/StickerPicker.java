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
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
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

public class StickerPicker implements OnClickListener, ShareablePopup, StickerPickerListener
{
	private StickerPickerListener listener;

	private Context mContext;

	private KeyboardPopupLayout popUpLayout;

	private StickerAdapter stickerAdapter;

	private View viewToDisplay;

	private int mLayoutResId = -1;
	
	private int currentConfig = Configuration.ORIENTATION_PORTRAIT; 
	
	private StickerEmoticonIconPageIndicator mIconPageIndicator;
	
	private static final String TAG = "StickerPicker";

	/**
	 * Constructor
	 * 
	 * @param activity
	 * @param listener
	 */
	public StickerPicker(Activity activity, StickerPickerListener listener)
	{
		this.mContext = activity;
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

		viewToDisplay = (ViewGroup) LayoutInflater.from(mContext).inflate(mLayoutResId, null);

		initViewComponents(viewToDisplay);
	}

	/**
	 * Initialises the view components from a given view
	 * 
	 * @param view
	 */
	private void initViewComponents(View view)
	{
		ViewPager mViewPager = ((ViewPager) view.findViewById(R.id.sticker_pager));

		if (null == mViewPager)
		{
			throw new IllegalArgumentException("View Pager was not found in the view passed.");
		}

		stickerAdapter = new StickerAdapter(mContext, this);

		mIconPageIndicator = (StickerEmoticonIconPageIndicator) view.findViewById(R.id.sticker_icon_indicator);

		View shopIcon = (view.findViewById(R.id.shop_icon));

		shopIcon.setOnClickListener(this);

		handleStickerIntro(view);		

		mViewPager.setVisibility(View.VISIBLE);

		mViewPager.setAdapter(stickerAdapter);

		mIconPageIndicator.setViewPager(mViewPager);

		mIconPageIndicator.setOnPageChangeListener(onPageChangeListener);
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
			Toast.makeText(mContext, R.string.no_external_storage, Toast.LENGTH_SHORT).show();
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
			initView();
		}
		return viewToDisplay;
	}

	public boolean isShowing()
	{
		return popUpLayout.isShowing();
	}

	@Override
	public void onClick(View arg0)
	{
		if (arg0.getId() == R.id.shop_icon)
		{
			// shop icon clicked
			shopIconClicked();
		}
	}

	private void shopIconClicked()
	{
		setStickerIntroPrefs();
		HAManager.getInstance().record(HikeConstants.LogEvent.STKR_SHOP_BTN_CLICKED, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT);
		
		Intent i = IntentFactory.getStickerShopIntent(mContext);
		mContext.startActivity(i);
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
	}
	
	public void updateListener(StickerPickerListener mListener, Context context)
	{
		this.listener = mListener;
		this.mContext = context;
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

			view.findViewById(R.id.shop_icon).setAnimation(HikeAnimationFactory.getStickerShopIconAnimation(mContext));
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

	public void resetToFirstPosition()
	{
		if (mIconPageIndicator != null)
		{
			mIconPageIndicator.setCurrentItem(0);
		}
	}
}