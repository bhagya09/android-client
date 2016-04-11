package com.bsb.hike.media;

import android.app.Activity;
import android.content.res.Configuration;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.bsb.hike.R;
import com.bsb.hike.adapters.EmoticonAdapter;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.EmoticonIconPageIndicator;

/**
 * Class for implementing emoticons anywhere in the application.
 * 
 * @author piyush
 * 
 */
public class EmoticonPicker implements ShareablePopup, EmoticonPickerListener, OnClickListener
{
	private Activity mActivity;

	private KeyboardPopupLayout mPopUpLayout;

	private View mViewToDisplay;

	private int mLayoutResId = -1;
	
	private int currentConfig = Configuration.ORIENTATION_PORTRAIT;

	private static final String TAG = "EmoticonPicker";
	
	private EditText mEditText;
	
	private EmoticonIconPageIndicator mIconPageIndicator;
	
	private EmoticonAdapter mEmoticonAdapter;

	private int currentItem;
	
	private static int[] statusUpdateDefaultEmojisList = new int[] { R.drawable.emo_recent, R.drawable.emo_tab_5_selector, R.drawable.emo_tab_6_selector,
			R.drawable.emo_tab_7_selector, R.drawable.emo_tab_8_selector, R.drawable.emo_tab_9_selector };

	private static int[] defaultEmojisList = new int[] { R.drawable.emo_recent, R.drawable.emo_tab_1_selector, R.drawable.emo_tab_2_selector, R.drawable.emo_tab_3_selector,
			R.drawable.emo_tab_4_selector, R.drawable.emo_tab_5_selector, R.drawable.emo_tab_6_selector, R.drawable.emo_tab_7_selector, R.drawable.emo_tab_8_selector,
			R.drawable.emo_tab_9_selector };
	
	private boolean useStatusUpdateList = false;
	
	public int getCurrentItem()
	{
		return currentItem;
	}

	public void setCurrentItem(int currentItem)
	{
		this.currentItem = currentItem;
	}

	/**
	 * Constructor
	 * 
	 * @param activity
	 * @param emoPickerListener
	 */

	public EmoticonPicker(Activity activity, EditText editText)
	{
		this.mActivity = activity;
		this.mEditText = editText;
		this.currentConfig = activity.getResources().getConfiguration().orientation;
	}

	/**
	 * Another constructor. The popup layout is passed to this, rather than the picker instantiating one of its own.
	 * 
	 * @param activity
	 * @param emoPickerListener
	 * @param popUpLayout
	 */

	public EmoticonPicker(int layoutResId, Activity activity, EditText editText, KeyboardPopupLayout popUpLayout)
	{
		this(activity, editText);
		this.mLayoutResId = layoutResId;
		this.mPopUpLayout = popUpLayout;
	}

	/**
	 * The view to display is also passed to this constructor along with Keyboard popup layout object
	 * 
	 * @param view
	 * @param activity
	 * @param emoPickerListener
	 * @param popUpLayout
	 */

	public EmoticonPicker(View view, Activity activity, EditText editText, KeyboardPopupLayout popUpLayout)
	{
		this(activity, editText);
		this.mPopUpLayout = popUpLayout;
		this.mViewToDisplay = view;
		initViewComponents(mViewToDisplay);
		Logger.d(TAG, "Emoticon Picker instantiated with views");
	}

	/**
	 * Basic constructor. Constructs the popuplayout on its own.
	 * 
	 * @param activiy
	 * @param emoPickerListener
	 * @param mainView
	 * @param firstTimeHeight
	 * @param eatOuterTouchIds
	 */
	public EmoticonPicker(Activity activity, EditText editText, View mainView, int firstTimeHeight, int[] eatOuterTouchIds)
	{
		this(activity, editText);
		mPopUpLayout = new KeyboardPopupLayout(mainView, firstTimeHeight, activity.getApplicationContext(), eatOuterTouchIds, null);
	}

	/**
	 * 
	 * @param activiy
	 * @param listener
	 * @param mainview
	 *            This is the activity or fragment's root view, which would get resized when keyboard is toggled
	 */

	public EmoticonPicker(Activity activiy, EditText editText, View mainView, int firstTimeHeight)
	{
		this(activiy, editText, mainView, firstTimeHeight, null);
	}

	public EmoticonPicker(int layoutResId, Activity context, EditText editText)
	{
		this(context, editText);
		this.mLayoutResId = layoutResId;
	}

	public boolean showEmoticonPicker(int screenOrientation)
	{
		return showEmoticonPicker(0, 0, screenOrientation);
	}

	public boolean showEmoticonPicker(int xoffset, int yoffset, int screenOritentation)
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

		return mPopUpLayout.showKeyboardPopup(mViewToDisplay);
	}

	/**
	 * Used for instantiating the views
	 */

	private void initView()
	{
		if (mViewToDisplay != null)
		{
			return;
		}

		/**
		 * Defensive null check
		 */
		if (mActivity == null)
		{
			String errorMsg = "Inside method : getView of EmoticonPicker. Context is null";
			HAManager.sendStickerEmoticonStrangeBehaviourReport(errorMsg);
			return;
		}
		
		/**
		 * Use default view. or the view passed in the constructor
		 */

		mLayoutResId = (mLayoutResId == -1) ? R.layout.emoticon_layout : mLayoutResId;

		mViewToDisplay = (ViewGroup) LayoutInflater.from(mActivity.getApplicationContext()).inflate(mLayoutResId, null);

		initViewComponents(mViewToDisplay);
	}

	/**
	 * Initialises the view components from a given view
	 * 
	 * @param view
	 */
	private void initViewComponents(View view)
	{
		ViewPager mPager = ((ViewPager) view.findViewById(R.id.emoticon_pager));

		if (null == mPager)
		{
			throw new IllegalArgumentException("View Pager was not found in the view passed.");
		}

		mIconPageIndicator = (EmoticonIconPageIndicator) view.findViewById(R.id.emoticon_icon_indicator);
		
		View eraseKey = view.findViewById(R.id.erase_key_image);
		eraseKey.setOnClickListener(this);
		
		int [] tabDrawables;

		if (useStatusUpdateList) //SU List

		{
			tabDrawables = statusUpdateDefaultEmojisList; 
		}
		
		else //Default list
		{
			tabDrawables = defaultEmojisList;
		}

		boolean isPortrait = mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

		//EmoticonConstants.EMOJI_RES_ID is a list of emojis used only in STATUS UPDATE SCREEN and default is used elsewhere
		int emoticonsListSize = useStatusUpdateList ? EmoticonConstants.EMOJI_RES_IDS.length : EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;

		int recentEmoticonsSizeReq = isPortrait ? EmoticonAdapter.MAX_EMOTICONS_PER_ROW_PORTRAIT : EmoticonAdapter.MAX_EMOTICONS_PER_ROW_LANDSCAPE;


		int[] mRecentEmoticons;
		
		/**
		 * For Status update screen, since normal hike emojis are not parsed in iOS, we use a truncated list for recents as well as overall emojis.
		 */
		if (useStatusUpdateList)
		{
			int startOffset = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;
			int endOffset = startOffset + emoticonsListSize;
			mRecentEmoticons = HikeConversationsDatabase.getInstance().fetchEmoticonsOfType(startOffset, endOffset, recentEmoticonsSizeReq);
		}

		else
		{
			mRecentEmoticons = HikeConversationsDatabase.getInstance().fetchEmoticonsOfType(0, emoticonsListSize, recentEmoticonsSizeReq);
		}

		/**
		 * If there aren't sufficient recent emoticons, we do not show the recent emoticons tab.
		 */
		if (currentItem == 0)
		{
			currentItem = (mRecentEmoticons.length < recentEmoticonsSizeReq) ? 1 : 0;
		}

		mIconPageIndicator.setOnPageChangeListener(onPageChangeListener);

		mEmoticonAdapter = new EmoticonAdapter(mActivity, this, isPortrait, tabDrawables, useStatusUpdateList);

		mPager.setVisibility(View.VISIBLE);

		mPager.setAdapter(mEmoticonAdapter);

		mIconPageIndicator.setViewPager(mPager);

		mPager.setCurrentItem(currentItem, false);
		
		mEmoticonAdapter.notifyDataSetChanged();

	}

	public void dismiss()
	{
		mPopUpLayout.dismiss();
	}

	public boolean isShowing()
	{
		return mPopUpLayout.isShowing();
	}

	public void updateDimension(int width, int height)
	{
		mPopUpLayout.updateDimension(width, height);
	}

	/**
	 * Interface method. Check {@link ShareablePopup}
	 */

	@Override
	public View getView(int screenOrientation)
	{
		if (orientationChanged(screenOrientation))
		{
			Logger.i(TAG, "Orientation Changed");
			resetView();
			currentConfig = screenOrientation;
		}
		
		if (mViewToDisplay == null)
		{
			/**
			 * Defensive null check
			 */
			if (mActivity == null)
			{
				String errorMsg = "Inside method : getView of EmoticonPicker. Context is null";
				HAManager.sendStickerEmoticonStrangeBehaviourReport(errorMsg);
				return null;
			}
			
			initView();
		}
		/*
		 * This is to update the recent emoticons palette, because of caching.
		 */
		else
		{
			ViewPager mPager = ((ViewPager) mViewToDisplay.findViewById(R.id.emoticon_pager));
			View view = mPager.getChildAt(0);
			if (view != null && view.getTag() != null && view.getTag() instanceof Integer)
				mEmoticonAdapter.refreshView(view, (Integer) view.getTag());
		}
		return mViewToDisplay;
	}

	/**
	 * Interface method. Check {@link ShareablePopup}
	 */

	@Override
	public int getViewId()
	{
		return mViewToDisplay.getId();
	}

	/**
	 * Utility method to free up resources
	 */
	public void releaseReources()
	{
		this.mActivity = null;
		this.mEditText = null;
	}
	
	public void updateETAndContext(EditText editText, Activity activity)
	{
		updateET(editText);
		this.mActivity = activity;
	}
	
	public void updateET(EditText editText)
	{
		this.mEditText = editText;
	}

	@Override
	public void emoticonSelected(int emoticonIndex)
	{
		Logger.i(TAG, " This emoticon was selected : " + emoticonIndex);
		Utils.emoticonClicked(mActivity.getApplicationContext(), emoticonIndex, mEditText);
	}
	
	public void eraseEmoticon()
	{
		if (mEditText != null)
		{
			mEditText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
		}
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.erase_key_image:
			eraseEmoticon();
			break;
		}
	}
	
	private void resetView()
	{
		mViewToDisplay = null;
	}
	
	private boolean orientationChanged(int deviceOrientation)
	{
		return currentConfig != deviceOrientation;
	}
	
	public void setOnDismissListener(PopupListener listener)
	{
		if (mPopUpLayout != null)
		{
			mPopUpLayout.setPopupDismissListener(listener);
		}
	}
	
	/**
	 * This function should be called when orientation of screen is changed, it will update its view based on orientation
	 * If picker is being shown, it will first dismiss current picker and then show it again using post on view
	 * 
	 * @param orientation
	 */
	public void onOrientationChange(int orientation)
	{
		showEmoticonPicker(orientation);
	}

	public void setDisableExtraPadding(boolean disabled)
	{
		if(mPopUpLayout != null)
		{
			mPopUpLayout.setPaddingDisabled(disabled);
		}
	}

	OnPageChangeListener onPageChangeListener = new OnPageChangeListener()
	{

		@Override
		public void onPageSelected(int pageNum)
		{
			currentItem = pageNum;
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
	 * Setter for {@link #useStatusUpdateList}
	 * @param emojiList
	 */
	public void useStatusUpdateEmojisList(boolean shouldUse)
	{
		this.useStatusUpdateList = shouldUse;
	}
	
}