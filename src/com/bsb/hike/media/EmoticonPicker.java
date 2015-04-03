package com.bsb.hike.media;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.bsb.hike.R;
import com.bsb.hike.adapters.EmoticonAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator;

/**
 * Class for implementing emoticons anywhere in the application.
 * 
 * @author piyush
 * 
 */
public class EmoticonPicker implements ShareablePopup, EmoticonPickerListener, OnClickListener
{
	private Context mContext;

	private KeyboardPopupLayout mPopUpLayout;

	private View mViewToDisplay;

	private int mLayoutResId = -1;
	
	private int currentConfig = Configuration.ORIENTATION_PORTRAIT;

	private static final String TAG = "EmoticonPicker";
	
	private EditText mEditText;

	/**
	 * Constructor
	 * 
	 * @param activity
	 * @param emoPickerListener
	 */

	public EmoticonPicker(Activity activity, EditText editText)
	{
		this.mContext = activity;
		this.mEditText = editText;
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

	public void showEmoticonPicker(int screenOrientation)
	{
		showEmoticonPicker(0, 0, screenOrientation);
	}

	public void showEmoticonPicker(int xoffset, int yoffset, int screenOritentation)
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

		mPopUpLayout.showKeyboardPopup(mViewToDisplay);
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
		 * Use default view. or the view passed in the constructor
		 */

		mLayoutResId = (mLayoutResId == -1) ? R.layout.emoticon_layout : mLayoutResId;

		mViewToDisplay = (ViewGroup) LayoutInflater.from(mContext).inflate(mLayoutResId, null);

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

		StickerEmoticonIconPageIndicator mIconPageIndicator = (StickerEmoticonIconPageIndicator) view.findViewById(R.id.emoticon_icon_indicator);
		
		View eraseKey = view.findViewById(R.id.erase_key_image);
		eraseKey.setOnClickListener(this);

		int[] tabDrawables = new int[] { R.drawable.emo_recent, R.drawable.emo_tab_1_selector, R.drawable.emo_tab_2_selector, R.drawable.emo_tab_3_selector,
				R.drawable.emo_tab_4_selector, R.drawable.emo_tab_5_selector, R.drawable.emo_tab_6_selector, R.drawable.emo_tab_7_selector, R.drawable.emo_tab_8_selector,
				R.drawable.emo_tab_9_selector };

		boolean isPortrait = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

		int emoticonsListSize = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;

		int recentEmoticonsSizeReq = isPortrait ? EmoticonAdapter.MAX_EMOTICONS_PER_ROW_PORTRAIT : EmoticonAdapter.MAX_EMOTICONS_PER_ROW_LANDSCAPE;

		int[] mRecentEmoticons = HikeConversationsDatabase.getInstance().fetchEmoticonsOfType(0, emoticonsListSize, recentEmoticonsSizeReq);

		/**
		 * If there aren't sufficient recent emoticons, we do not show the recent emoticons tab.
		 */
		int firstCategoryToShow = (mRecentEmoticons.length < recentEmoticonsSizeReq) ? 1 : 0;

		EmoticonAdapter mEmoticonAdapter = new EmoticonAdapter(mContext, this, isPortrait, tabDrawables);

		mPager.setVisibility(View.VISIBLE);

		mPager.setAdapter(mEmoticonAdapter);

		mIconPageIndicator.setViewPager(mPager);

		mPager.setCurrentItem(firstCategoryToShow, false);

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
			initView();
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
		this.mContext = null;
		this.mEditText = null;
	}
	
	public void updateETAndContext(EditText editText, Activity activity)
	{
		updateET(editText);
		this.mContext = activity;
	}
	
	public void updateET(EditText editText)
	{
		this.mEditText = editText;
	}

	@Override
	public void emoticonSelected(int emoticonIndex)
	{
		Logger.i(TAG, " This emoticon was selected : " + emoticonIndex);
		Utils.emoticonClicked(mContext.getApplicationContext(), emoticonIndex, mEditText);
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

}