/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.kpt.adaptxt.beta.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.kpt.adaptxt.beta.AdaptxtIME;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.core.coreservice.KPTSuggestion;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class KPTCandidateView extends View implements View.OnTouchListener{

	public static final int OUT_OF_BOUNDS = -1;
	public static final int SUGGESTIONS_DEFAULT = 0;
	public static final int SUGGESTIONS_MAINTENANCE = 1;
	public static final int SUGGESTIONS_PASSWORD = 2;
	public static final int SUGGESTIONS_NO_DICTIONARY_INSTALLED = 3;
	public static final int SUGGESTIONS_BASE_EXPIRED = 4;
	public static final int SUGGESTIONS_NO_GLIDE_SUGGESTIONS = 5;
	public static final int SUGGESTIONS_BASE_UPGRADE = 6;
	public static final int SUGGESTIONS_ADDON_UPGRADING = 7;
	public static final int SUGGESTIONS_BLANK_SUGGESTIONS = 8;

	public static final int CANDIDATE_TYPE_DEFAULT = 1;
	public static final int CANDIDATE_TYPE_ACC_DISSMISS = 2;
	public static final int CANDIDATE_TYPE_ACC = 3;
	public static final int ACC_RTL_TOLERANCE = 1;
	public static final int CANDIDATE_TYPE_DELETE_WORD = 4;

	private static String[] mSuggModString;
	private static final List<KPTSuggestion> EMPTY_LIST = new ArrayList<KPTSuggestion>();

	public AdaptxtIME mService;
	private List<KPTSuggestion> mSuggestions = EMPTY_LIST;
	private KPTSuggestion mSelectedSuggestion;
	private int mSelectedIndex;
	private int mTouchX = OUT_OF_BOUNDS;
	private Drawable mSelectionHighlight;
	private int mSuggestionMode;
	private Rect mBgPadding;

	//private TextView mPreviewText;
	//private PopupWindow mPreviewPopup;
	private float mCandiatePreviewTextSize;
	//private int mCurrentWordIndex;
	//private int mCurrentPopUPWordIndex;
	private Drawable mDivider;

	private static final int MAX_SUGGESTIONS = 32;
	private static final int SCROLL_PIXELS = 20;

	//private static final int MSG_REMOVE_PREVIEW = 1;
	//private static final int MSG_REMOVE_THROUGH_PREVIEW = 2;
	private static final int MSG_DELETE_WORD = 3;
	private boolean mStopInsertSuggestion=false;

	private int[] mWordX = new int[MAX_SUGGESTIONS];
	private float MIN_FONT_DIPS = 14.0f;
	private static final int X_GAP = 10;
	private static final int MIN_DISMISS_DIPS = 50;

	private int mColorNormal;
	private int mColorRecommended;
	private int mColorATR;
	private Paint mPaint;
	private Paint mModePaint;
	private int mDescent;
	private boolean mScrolled;
	private boolean mShowingAddToDictionary;
	public int mEndOfCV;
	public int mTargetScrollX;
	private boolean mIsRTLScroll;
	private int mTotalWidth;
	//public int mExtraWidth;
	// popup window for displaying expanded suggestions
	//public PopupWindow mSuggestionBarPopup;

	//Flag to indicate whether ACC or RC is shown
	private boolean mIsAcc;

	//String contains either AC or RC
	private CharSequence mAccString;

	//Setting in UI whether AC is on or not
	private boolean mAutoCorrection;



	//Indicator flag set by KPTAdaptxtIME for orientation changes
	private boolean mConfigurationChanged;

	//Indicator whether to support RightToLeft(RTL) suggestion bar display
	private boolean mRTL;

	//This hold the current density
	private final float scale = getResources().getDisplayMetrics().density;


	//To know the current theme
	private SharedPreferences pref;
	private int mThemeMode;
	private int mThemeType = KPTConstants.THEME_INTERNAL;



	private Context mContext;
	public KPTCandidateViewPopup mCandidatePopup;
	private int suggswidth;
	private int suggCount = 3;
	public boolean mISCheckedSuggestion;
	private int startX = -1;

	private KPTAdaptxtTheme mTheme;

	private boolean mOrientationLandscape;

	private Resources mResource; 

	//This contains the calculated width of ACC head & tail and left button arrow 
	private int mAccWidth;
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			/*case MSG_REMOVE_PREVIEW:
				mPreviewText.setVisibility(GONE);
				break;
			case MSG_REMOVE_THROUGH_PREVIEW:
				mPreviewText.setVisibility(GONE);
				if (mTouchX != OUT_OF_BOUNDS) {
					removeHighlight();
				}
				break;*/
			case MSG_DELETE_WORD:
				// Remove dialog is not displayed. we are inserting suggestion to editor
				if (!mIsFrameWorkEditorSuggestion) {
					mStopInsertSuggestion = true;
					removeHighlight();
					//SetPreviewTextVisibility(GONE);
					dismissExpandedSuggWindow();
					startX = -1;

					// currently stoping touch listner.
					KPTCandidateView.this.setOnTouchListener(null);
					mService.removeWordDialog(mSelectedSuggestion,mSelectedIndex,true);
					if (!mService.isSuggestionDelDialogShown()) 	{
						KPTCandidateView.this.registerOnTouchEvent();
					}
					invalidate();
				}else{
					insertSuggestion();
				}
				break;
			}
		}
	};
	private boolean mDeleteAction = false;
	private boolean mIsRTL = false;

	/**
	 * Flag states wheather suggestion list is from KPTCoreEngine or the List of the suggestion was given
	 * by the IMEFramework means given by editor for example to field in message application when u type some char
	 * frame will give drop down list of contact. this suggestion are given by frame work to IME's onDisplayCompletions()</br>
	 * NOTE: this suggestion are displayed ONLY when we have Fullscreen Mode and Landscape</br>
	 * 
	 * FALSE means KPTSuggestion</br>
	 * TRUE means Framework suggestion.
	 *  
	 */
	public boolean mIsFrameWorkEditorSuggestion;
	private int mDividerConstant = 100;


	/**
	 * Construct a CandidateView for showing suggested words for completion.
	 * 
	 * @param context
	 * @param attrs
	 */
	public KPTCandidateView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		//mTheme = KPTAdaptxtIME.mTheme;

		//mResource = mTheme.getThemeContext().getResources();
		//mThemeType = mTheme.getCurrentThemeType();

		Resources res = context.getResources();
		LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mCandiatePreviewTextSize = getResources().getDimension(R.dimen.kpt_cv_preview_textsize);

		 // shared preference to know the current theme
	    pref=PreferenceManager.getDefaultSharedPreferences(context);
	   	//mThemeMode = Integer.parseInt(pref.getString(KPTConstants.PREF_THEME_MODE, "1"));
	    //mKptDB = new KPTDataBaseImplemetation(context);
	   	//mKptThemeItem = mKptDB.getThemeValues(mThemeMode);
	   	//mThemeType = mKptThemeItem.getThemeType();
	   	
		mSuggModString = context.getResources().getStringArray(
				R.array.kpt_suggestion_mode_strings);
		mSelectionHighlight = context.getResources().getDrawable(
				R.drawable.kpt_list_selector_background_pressed);
		/*mPreviewPopup = new PopupWindow(context);*/
/*		mPreviewText = (TextView) inflate.inflate(R.layout.candidate_preview,
				null);*/
	/*	mPreviewPopup.setBackgroundDrawable(null);*/

/*
		mPreviewPopup.setContentView(mPreviewText);
		mPreviewPopup.setWindowLayoutMode(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);*/

		mPaint = new Paint();
		//mPaint.setTypeface(mTheme.getUserSelectedFontTypeface());
		mPaint.setColor(mColorNormal);
		mPaint.setAntiAlias(true);
	
		mPaint.setTextSize(mCandiatePreviewTextSize);
		mPaint.setStrokeWidth(0);

		mModePaint = new Paint();
		//mModePaint.setTypeface(mTheme.getUserSelectedFontTypeface());
		mModePaint.setColor(mColorNormal);
		mModePaint.setAntiAlias(true);
		mModePaint.setTextSize(MIN_FONT_DIPS * scale + 0.5f); //Setting the text which is independent of density
		mModePaint.setStrokeWidth(0);

		mPaint.setTextAlign(Align.CENTER);
		mDescent = (int) mPaint.descent();

		mIsRTLScroll = false;
//		setHorizontalFadingEdgeEnabled(false);//Bug TP: 10288
		setWillNotDraw(false);
		setHorizontalScrollBarEnabled(false);
		setVerticalScrollBarEnabled(false);
		notifyConfigurationChange(false);
		scrollTo(0, getScrollY());
		registerOnTouchEvent();
		if(getResources().getDisplayMetrics().density >  3/*DisplayMetrics.DENSITY_XXHIGH*/){
			mDividerConstant  = 300;
		}
	}

	/**
	 * create the suggestion bar popup window. displayed when the down arrow is
	 * pressed in the suggestion bar(this works only in low tier)
	 */
	public void suggestionBarPopUp() {
		if (mTheme == null || mCandidatePopup == null) {
			return;
		}
		
		mCandidatePopup.setVisibility(View.VISIBLE);
		
		invalidate();

		//mSuggestionBarPopup = new PopupWindow(mContext);
		int width = 0;
		int height = 0;
		if (mService.mInputView.isShown()) {
			width = mService.mInputView.getWidth();
			height = mService.mInputView.getHeight(); ;
		} else {
			width = getResources().getDisplayMetrics().widthPixels;

			height = mService.mCandidateViewContainer.getHeight() * 5;

		}
		//mSuggestionBarPopup.setWidth(width + 1);// Added 1px of width for moving
		// the image right side edge to
		// disappear.
		//mSuggestionBarPopup.setHeight(height + 2);// Added 2px of height for
		// moving the image bottom
		// side edge to disappear.

		//mCandidatePopup = new KPTCandidateViewPopup(mContext, this, mSuggestionBarPopup, mService, mTheme, mThemeMode);
		mCandidatePopup.setCandidateView(this);
		mCandidatePopup.setService(mService);
		mCandidatePopup.setTheme(mTheme);
		
		mCandidatePopup.setFrameWorkSuggestionState(mIsFrameWorkEditorSuggestion);
		mCandidatePopup.setLayoutParams(new FrameLayout.LayoutParams(width, height));
		mCandidatePopup.setParentWidth(width);
		mCandidatePopup.setCandidateContainer((KPTCandidateViewContainer) this.getParent());
		mCandidatePopup.setSuggestion(mSuggestions);


		/*if(mThemeMode == KPTConstants.THEME_CUSTOM 
				&& mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION_BG] != -1) {
			Drawable background = new ColorDrawable(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION_BG]);
			mCandidatePopup.setBackgroundDrawable(background);
		}else{*/
			mCandidatePopup.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggBG));
		//}

		/*if(((Object) mService).isFullscreenMode()) {
			height = height + mService.mCandidateViewContainer.getHeight() * 2;
		} else {
			//height = height + mService.mCandidateViewContainer.getHeight();
		}*/


		//mSuggestionBarPopup.setContentView(mCandidatePopup);
	/*	checkOrientation();
		if (Build.VERSION.SDK_INT > 18 && !mOrientationLandscape) {
			View decorView = mService.getWindow().getWindow().getDecorView(); 
			if (decorView !=null) {
				Log.e("BHA", "mService.mCandidateViewContainer.getHeight() ------ "+mService.mCandidateViewContainer.getHeight()+ ":height :"+height);
				mSuggestionBarPopup.showAtLocation(decorView, Gravity.TOP, 0, 0);
			}
		}else {*/
		
			/*if (mService.mInputView.getWindowToken() != null) {
				mSuggestionBarPopup.showAtLocation(mService.mInputView, Gravity.NO_GRAVITY, 0, height);
			}else { 
				View decorView = mService.getWindow().getWindow().getDecorView(); 
				if (decorView !=null) {
					mSuggestionBarPopup.showAtLocation(decorView, Gravity.TOP, 0, mService.mCandidateViewContainer.getHeight());
				}
		//	}
			
		}*/
		
		//commenting to fix 11495. Not sure who made it false
		//mService.mComposingMode= false;
	}

	public void setFrameWorkSuggestionState(boolean state){
		mIsFrameWorkEditorSuggestion = state; 
	}

	/**
	 * Dismiss the expanded suggestion window
	 */
	public void dismissExpandedSuggWindow() {
		if (mCandidatePopup != null && mCandidatePopup.getVisibility() == View.VISIBLE) {
			mCandidatePopup.setVisibility(View.GONE);
		}
	}

	public boolean isShowingSuggPopUp() {
		return (mCandidatePopup != null && mCandidatePopup.isShown());
	}
	/**
	 * Insert the suggestion in editor
	 */
	public void insertSuggestionforCandidatePopup(){
		if(mCandidatePopup!=null)
			mCandidatePopup.insertPopupSuggestion();
	}

	/**
	 * A connection back to the service to communicate with the text field
	 * 
	 * @param listener
	 */
	public void setService(AdaptxtIME listener) {
		mService = listener;
	}

	/**
	 * Gets the currently set IME service reference
	 * 
	 * @return IME service reference
	 */
	public AdaptxtIME getService() {
		return mService;
	}

	@Override
	public int computeHorizontalScrollRange() {
		return mTotalWidth;
	}

	public void RTLDraw(Canvas canvas)
	{
		/* intializing theme modes for bright and dark themes */
		//mThemeMode = Integer.parseInt(pref.getString(
		//KPTConstants.PREF_THEME_MODE, "1"));
		Resources res = getResources();

		mDivider = mTheme.getResources().getDrawable(mTheme.mCandidateSuggDivider);

		final int height = getHeight();
		if (mBgPadding == null) {
			mBgPadding = new Rect(0, 0, 0, 0);
			if (getBackground() != null) {
				getBackground().getPadding(mBgPadding);
			}

		}

		int count = mSuggestions.size(); // this variable for the no of suggestions
		final Rect bgPadding = mBgPadding; // this variable for the padding
		final Paint paint = mPaint;
		final int touchX = mTouchX;// this varaible for the touch area calculations
		final int scrollX = getScrollX(); // this variable for the scrolling of x in candidate bar
		final boolean scrolled = mScrolled; 
		final int y = (int) (height + mPaint.getTextSize() - mDescent) / 2;
		mIsAcc = false; 
		mAccString = "";

		// every suggestion width is 3rd part of total candidate bar width

		if (!KPTConstants.mIsNavigationBackwar){
			suggCount = 2;
			suggswidth = getWidth()/2;
		} else {
			suggCount = 3;
			suggswidth = (getWidth() / 3);
		}
		//x is the variable to know where we have to draw text on candidate bar
		// we are drawing suggestions from the end... and decreasing suggestion width each time 
		int x = getWidth()+suggswidth/2;


		// To disable the expand icon if the count is less than the specified range.
		int disableChevronCount = 4;
		if (!mISCheckedSuggestion)
			disableChevronCount = 3;
		if (mSuggestions.size() < disableChevronCount) {
			((KPTCandidateViewContainer) this.getParent()).setExpandIconDisable(mThemeType);
		} else {
			((KPTCandidateViewContainer) this.getParent()).changeExpandIcon(mThemeType);
		}

		// Suggestions are less than the minimum of 3, re initialize the
		// suggestions value.
		if (count < 3) {
			suggCount = count;
		}

		// to wrap the width of the suggestion based on the number of
		// suggestions.
		if (count == 1) {
			suggswidth = getWidth();

		} else if (count == 2) {
			suggswidth = getWidth() / 2;
		}
		int devider_position =0;
		int prev_devider = 0;
		//Render text on CV 
		if (canvas != null && mSuggestionMode != SUGGESTIONS_DEFAULT) {
			String[] mSuggModString = getResources().getStringArray(
					R.array.kpt_suggestion_mode_strings);
			
			if (mSuggModString.length <= mSuggestionMode) {
				return;
			}
			int maintanceWidth = (int) mModePaint.measureText(
					mSuggModString[mSuggestionMode], 0,
					mSuggModString[mSuggestionMode].length());
			x = getWidth()/2 - (maintanceWidth/2);
			mModePaint.setColor(mColorNormal);
			canvas.drawText(mSuggModString[mSuggestionMode], 0,
					mSuggModString[mSuggestionMode].length(), x, y, mModePaint);

			requestLayout();
			return;
		}
		//Returning when there are no suggestions 
		//bug fix 6057, no need to render candidate view when there are no suggestions
		if(mSuggestionMode != SUGGESTIONS_DEFAULT)
			return;


		//canvas.drawCircle(x, y, 10, mPaint);

		for (int i = 0; i < suggCount; i++) {

			if (i >= mSuggestions.size()) {
				return;
			}
			CharSequence suggestion = mSuggestions.get(i)
					.getsuggestionString();
			if (suggestion == null)
				continue;
			updateTextColor();
			if (mAutoCorrection
					&& (mSuggestions.get(i).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION || mSuggestions
					.get(i).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_REVERT_CORRECTION)) {
				if(mSuggestions
						.get(i).getsuggestionType() != KPTSuggestion.KPT_SUGGS_TYPE_REVERT_CORRECTION) mIsAcc = true;
				mAccString = suggestion;
				paint.setColor(mColorRecommended);
				//paint.setTypeface(Typeface.DEFAULT_BOLD);
			} else {
				paint.setColor(mColorNormal);
				//paint.setTypeface(Typeface.DEFAULT);
			}
			// this variable to know how much we have to move in preview it
			// align correctly
			//mExtraWidth = suggswidth / 2;
			boolean isAtrOn = pref.getBoolean(KPTConstants.PREF_ATR_FEATURE,
					true);
			if (isAtrOn
					&& (mSuggestions.get(i).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_ATR)) {
				paint.setColor(mColorRecommended);
			}

			if (canvas != null) {
				String sug = fitTextSize(suggestion.toString(), paint,
						suggswidth, true);
				if (sug != null) {
					suggestion = sug;
				}

				x = x - suggswidth;

				// Draw a divider unless it's after the hint
				if (!(mShowingAddToDictionary && i == 1)
						&& (suggCount != 1 && i != suggCount - 1)) {
					x -= mDivider.getIntrinsicWidth();
					mDivider.setBounds(x - suggswidth / 2, y - 100,
							x + mDivider.getIntrinsicWidth() - suggswidth
							/ 2, y + mDivider.getIntrinsicHeight());
					mDivider.draw(canvas);
					prev_devider = devider_position;
					devider_position = x - suggswidth / 2;
					x += mDivider.getIntrinsicWidth();

				}


				if (!(KPTConstants.mIsNavigationBackwar) && !(mShowingAddToDictionary && i == 1)
						&& (suggCount != 1)) {

					x -= mDivider.getIntrinsicWidth();
					mDivider.setBounds(x - suggswidth / 2, y - 100,
							x + mDivider.getIntrinsicWidth() - suggswidth
							/ 2, y + mDivider.getIntrinsicHeight());
					mDivider.draw(canvas);
					prev_devider = devider_position;
					devider_position = x - suggswidth / 2;
					x += mDivider.getIntrinsicWidth();

				}


				// coX and coY variables are to know the bounds of each
				// suggestion box
				// these will be used for drawing deviders, boundaries for
				// touch area and highlighting area when we touch
				int coX = 0;
				int coY = 0;
				if (i == 0) {
					coX = devider_position;
					coY = getWidth();
					mWordX[i] = devider_position - suggswidth / 2;
				}
				if (i == 1) {
					coX = devider_position;
					coY = prev_devider;
					mWordX[i] = devider_position - suggswidth / 2;
				}
				if ((i == suggCount-1) && (i!= 0)) {
					coX = -suggswidth / 2;
					coY = devider_position;
					mWordX[i] = coX;
				}
				// setting bounds for highlighting area and draw
				if (touchX + scrollX >= coX && touchX + scrollX < coY
						&& !scrolled && touchX != OUT_OF_BOUNDS) {
					if (canvas != null && !mShowingAddToDictionary) {
						if (count != 1) {

							mSelectionHighlight.setBounds(coX,
									bgPadding.top, coY, height);
							mSelectionHighlight.draw(canvas);
						/*	showPreview(i, null, CANDIDATE_TYPE_DEFAULT, 0,
									0);*/
						} else {
							mSelectionHighlight.setBounds(-suggswidth / 2,
									bgPadding.top, getWidth(), height);
							mSelectionHighlight.draw(canvas);
						/*	showPreview(i, null, CANDIDATE_TYPE_DEFAULT, 0,
									0);*/
						}
					}
					//showPreview(i, null, CANDIDATE_TYPE_DEFAULT, 0, 0);
					mSelectedSuggestion = mSuggestions.get(i);
					mSelectedIndex = i;
				}
				if (count == 1) {
					canvas.drawText(suggestion, 0, suggestion.length(),
							getWidth()/2, y, paint);
				} else {
					canvas.drawText(suggestion, 0, suggestion.length(), x,
							y, paint);
				}
				x -= mDivider.getIntrinsicWidth();
			}
		}
		mTotalWidth = getWidth();
	}




	/**
	 * If the canvas is null, then only touch calculations are performed to pick
	 * the target candidate.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		if (canvas != null) {
			super.onDraw(canvas);
		}
		if (mTheme == null){
			return;
		}
		mTotalWidth = 0;
		if (mSuggestions == null){
			return;
		}
		
		if(mRTL)
		{
			RTLDraw(canvas);
			return;
		}


		mDivider = mTheme.getResources().getDrawable(mTheme.mCandidateSuggDivider);

		final int height = (int)((KPTCandidateViewContainer)this.getParent()).getHeight();

		if (mBgPadding == null) {
			mBgPadding = new Rect(0, 0, 0, 0);
			if (getBackground() != null) {
				getBackground().getPadding(mBgPadding);
			}
			/*mDivider.setBounds(0, 0, mDivider.getIntrinsicWidth(), mDivider
					.getIntrinsicHeight());*/

			/*		float dip = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getResources()
					.getDimension(R.dimen.candidate_strip_height), getResources()
					.getDisplayMetrics());
			mDivider.setBounds(0, 0, mDivider.getIntrinsicWidth(), (int) dip
					+ mDivider.getIntrinsicHeight());*/
		}

		int count = mSuggestions.size();
		final Rect bgPadding = mBgPadding;
		final Paint paint = mPaint;
		final int touchX = mTouchX;
		final int scrollX = getScrollX();
		final boolean scrolled = mScrolled;
		final int y = (int) (height + mPaint.getTextSize() - mDescent) / 2;
		mIsAcc = false; //karthik -- bug fix for 6201
		mAccString = "";

		int x = 0;
		suggswidth = getWidth() / 3;


		//It called when we are invalidating from the ellipse case(From fitTextSize()) of suggestion. 
		if (!mISCheckedSuggestion) {
			if (count > 2)
				count = 2;
			suggswidth = getWidth()/2;
		}

		// To disable the expand icon if the count is less than the specified range.
		int disableChevronCount = 4;
		if (!mISCheckedSuggestion)
			disableChevronCount = 3;
		if (mSuggestions.size() < disableChevronCount) {
			((KPTCandidateViewContainer) this.getParent()).setExpandIconDisable(mThemeType);
		} else {
			((KPTCandidateViewContainer) this.getParent()).changeExpandIcon(mThemeType);
		}

		// Suggestions are less than the minimum of 3, re initialize the
		// suggestions value.
		if (count < 3) {
			suggCount = count;
		}

		// to wrap the width of the suggestion based on the number of
		// suggestions.
		if (count == 1) {
			suggswidth = getWidth();
		} else if (count == 2) {
			suggswidth = getWidth() / 2;
		}
		//Render text on CV 
		if (canvas != null && mSuggestionMode != SUGGESTIONS_DEFAULT) {
			String[] mSuggModString = getResources().getStringArray(
					R.array.kpt_suggestion_mode_strings);
			
			if (mSuggModString.length <= mSuggestionMode) {
				return;
			}

			int maintanceWidth = (int) mModePaint.measureText(
					mSuggModString[mSuggestionMode], 0,
					mSuggModString[mSuggestionMode].length());
			x = x + (getWidth() - maintanceWidth)
					- (getWidth() - maintanceWidth) / 2;
			mModePaint.setColor(mColorNormal);
			if (!mIsFrameWorkEditorSuggestion) {
				canvas.drawText(mSuggModString[mSuggestionMode], 0,
						mSuggModString[mSuggestionMode].length(), x, y, mModePaint);
			}
			requestLayout();
			return;
		}
		
		//Returning when there are no suggestions 
		//bug fix 6057, no need to render candidate view when there are no suggestions
		if(mSuggestionMode != SUGGESTIONS_DEFAULT)
			return;
		if(count > 2) {
			if(!(KPTConstants.mIsNavigationBackwar)){
				suggCount =2 ;
				suggswidth = getWidth() / 2;
			}
			else{
				suggCount = 3;
			}
		}

		for (int i = 0; i < suggCount; i++) {

			if (i >= mSuggestions.size()) {
				return;
			}
			CharSequence suggestion = mSuggestions.get(i).getsuggestionString();
			if (suggestion == null)
				continue;
			updateTextColor();
			if (mAutoCorrection
					&& (mSuggestions.get(i).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION || mSuggestions
					.get(i).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_REVERT_CORRECTION)) {
				if(mSuggestions
						.get(i).getsuggestionType() != KPTSuggestion.KPT_SUGGS_TYPE_REVERT_CORRECTION) mIsAcc = true;
				mAccString = suggestion;
				// only auto correction is present
				//Commenting the below code for TP #15668. Empty space next to AC being the only suggestion 
				/*if (count == 1) {
					suggswidth -= ((KPTCandidateViewContainer) this.getParent())
							.getAccDismissWidth();
				}*/

				paint.setColor(mColorRecommended);
				//paint.setTypeface(Typeface.DEFAULT_BOLD);
			} else {
				paint.setColor(mColorNormal);
				//paint.setTypeface(Typeface.DEFAULT);
			}

			//mExtraWidth=0;
			boolean isAtrOn = pref.getBoolean(KPTConstants.PREF_ATR_FEATURE,
					true);
			if (isAtrOn
					&& (mSuggestions.get(i).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_ATR)) {
				//tp  10131
				paint.setColor(mColorRecommended);
			}

			mWordX[i] = x;

			if (touchX + scrollX >= x && touchX + scrollX < x + suggswidth && !scrolled
					&& touchX != OUT_OF_BOUNDS) {
				if (canvas != null && !mShowingAddToDictionary) {
					canvas.translate(x, 0);
					mSelectionHighlight.setBounds(0, bgPadding.top, suggswidth,
							height);
					mSelectionHighlight.draw(canvas);
					canvas.translate(-x, 0);
					//showPreview(i, null,CANDIDATE_TYPE_DEFAULT,0,0);
				}
				mSelectedSuggestion = mSuggestions.get(i);
				mSelectedIndex = i;
			}

			if (canvas != null) {

				String sug = fitTextSize(suggestion.toString(), paint, suggswidth,true);
				if (sug != null) {
					suggestion = sug;
				}

				canvas.drawText(suggestion, 0, suggestion.length(), x+ suggswidth/2, y, paint);
				canvas.translate(x + suggswidth, 0);
				 
				// Draw a divider unless it's after the hint
				if (!(mShowingAddToDictionary && i == 1) && (suggCount != 1 && i != suggCount - 1)) {
					mDivider.setBounds(0, y  - mDividerConstant, mDivider.getIntrinsicWidth(), mDivider.getIntrinsicHeight());
					/*if(Configuration.ORIENTATION_LANDSCAPE == getResources().getConfiguration().orientation ){
						Log.e("VMC", "---------------> LANDSCAPE MODE 20:20<------------------");
						mDivider.setBounds(0, (y  - mDividerConstant) + 20 , mDivider.getIntrinsicWidth(), mDivider.getIntrinsicHeight() - 20);
					}else{
						mDivider.setBounds(0, (y  - mDividerConstant) + 50 , mDivider.getIntrinsicWidth(), mDivider.getIntrinsicHeight() - 50);
					}*/
					
					mDivider.draw(canvas);
					canvas.translate(mDivider.getIntrinsicWidth(), 0);
					x += mDivider.getIntrinsicWidth();
				}
				canvas.translate(-x - suggswidth, 0);
			}
			x += suggswidth;
		}
		mTotalWidth = x;
		//}		
		/*// change the scrollX according RTL status
		if(mRTL && mTargetScrollX > mEndOfCV)
		{
			mTargetScrollX = mEndOfCV;
		}
		if (mTargetScrollX != scrollX) {
			scrollToTarget();
		}*/

	}





	/**
	 * Checks the suggestions are fitted to the provided width.
	 *  
	 * @return true if all the suggestions are fitted else returns false.
	 */
	private boolean CheckSuggestionWidths(){
		Paint paint = new Paint();
		paint.setTextSize(mCandiatePreviewTextSize - 6); // The 6 indicates the total pixels to reduce the text size.
		//Removing the expand Icon width and vertical divider width from total width of the device.
		//We are not taking the getWidth of the Candidate because we are calling this before drawing the suggestions. 
		//So,initially and while changing the Orientation it returns Zero.
		int width = getResources().getDisplayMetrics().widthPixels
				- ((KPTCandidateViewContainer) this.getParent()).getSuggestionBarExpandWidth() - 2;
		width = width /3; // Dividing the width to each suggestion.
		for (int i = 0; i < suggCount && mSuggestions.size() > i; i++) {
			String sequence = mSuggestions.get(i).getsuggestionString().toString();
			if (mAutoCorrection
					&& (mSuggestions.get(i).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_AUTO_CORRECTION || mSuggestions
					.get(i).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_REVERT_CORRECTION)) {
				// removing the acc dismiss icon width from each suggestion[i.e  dividing the width of acc dismiss to 3 parts(suggestions)]
				width = width
						- (((KPTCandidateViewContainer) this.getParent()).getAccDismissWidth())/3;
			}
			if (doesItFit(sequence, paint, width)) {
				mISCheckedSuggestion = true;
			}else{
				mISCheckedSuggestion = false;
				break; // If any one of the suggestion is not fitted to the specified width then stop the loop.
			}
		}
		return false;
	}

	/**
	 * Checking the suggestion string and applying the appropriate text size and
	 * ellipse.
	 * 
	 * @param suggestion
	 *            - The suggestion to string to be drawn.
	 * @param localPaint
	 *            - Paint reference
	 * @param width
	 *            - Width of the suggestion
	 * @param isFromMainCV
	 *            - whether it called from CandidateView or CandidateViewPopup
	 * 
	 * @return the string which fits in the specified width of the suggestion.
	 */
	public String fitTextSize(String suggestion, Paint localPaint, int width, boolean isFromMainCV) {
		localPaint.setTextSize(mCandiatePreviewTextSize);
		if (doesItFit(suggestion, localPaint, width)) {
			return null;
		} else {
			localPaint.setTextSize(localPaint.getTextSize() - 2);
			//localPaint.setTypeface(Typeface.DEFAULT);
			if (doesItFit(suggestion, localPaint, width)) {
				return null;
			} else {
				localPaint.setTextSize(localPaint.getTextSize() - 1);
				if (doesItFit(suggestion, localPaint, width)) {
					return null;
				} else {
					localPaint.setTextSize(localPaint.getTextSize() - 1);
					if (doesItFit(suggestion, localPaint, width)) {
						return null;
					} else {
						localPaint.setTextSize(localPaint.getTextSize() - 1);
						if (doesItFit(suggestion, localPaint, width)) {
							return null;
						} else {
							localPaint.setTextSize(localPaint.getTextSize() - 1);
							if (doesItFit(suggestion, localPaint, width)) {
								return null;
							} else {
								for (int i = 0; i < suggestion.length(); i++) {
									/*if (isFromMainCV && !mISCheckedSuggestion) {
										invalidate();
									}*/
									if (doesItFit("..." + suggestion.substring(i), localPaint, width)) 
										if(!mRTL)
											return "..." + suggestion.substring(i);
										else
											return suggestion.substring(i)+ "..." ;
								}
								return null;
							}
						}
					}
				}

			}
		}
	}

	/**
	 * Checking the suggestion string with the available width space.
	 * 
	 * @param suggestion
	 *            - The suggestion to string to be drawn.
	 * @param paint
	 *            - Paint reference
	 * @param width
	 *            = Width of the suggestion
	 * @return true if the suggestion fits in the specified width.
	 */
	private boolean doesItFit(String suggestion, Paint paint, int width) {
		// Removing the 6 pixels from the width because the text is overlapping on the divider of the suggestion.
		// And leaving some reasonable gap between and after the suggestion.
		return ((width - 6) > paint.measureText(suggestion, 0, suggestion.length()));
	}

	public void setAutoCorrection(boolean autoCorrectOn) {
		mAutoCorrection = autoCorrectOn;
	}


	public void setSuggestions(List<KPTSuggestion> suggestions,
			boolean completions, boolean typedWordValid, int suggestionMode) {
		clear();
		//mThemeMode = Integer.parseInt(pref.getString(KPTConstants.PREF_THEME_MODE, "1"));
		Resources res = getResources();

		suggCount = 3;
		if (suggestions != null) {
			mSuggestions = new ArrayList<KPTSuggestion>(suggestions);
			CheckSuggestionWidths();
			/*if(mRTL) 
			{
			// RTL support for candidate bar: reversing the suggestions order to support RTL display
				Collections.reverse(mSuggestions);
			}*/
		}
		
		/*if(mCandidatePopup!= null){
			setTheme(mTheme);
		}*/
		
		//RTL support for candidate bar: need not set the scroll to '0' when it is a RTL, as the scroll position will change for sure
		updateTextColor();
		updateFontStyle();

		mSuggestionMode = suggestionMode;
		if(mRTL  && mSuggestionMode == SUGGESTIONS_DEFAULT)
		{
			mIsRTLScroll = true;
		}
		else
		{
			scrollTo(0, getScrollY());
			mTargetScrollX = 0;
		}
		KPTCandidateViewContainer viewContainer = ((KPTCandidateViewContainer)this.getParent());
		viewContainer.updateVisibilityOfExpandIcon(suggestionMode);
		// Compute the total width
		onDraw(null);

		invalidate();
		viewContainer.updateView();
		viewContainer.requestLayout();
		invalidate();
	}

	public void checkRTLscrolling(){
		KPTCandidateViewContainer viewContainer = ((KPTCandidateViewContainer)this.getParent());
		// set the scroll properly according to RTL status
		mEndOfCV = mTotalWidth - getResources().getDisplayMetrics().widthPixels;
		if(isAcc()) {
			mEndOfCV = mEndOfCV + viewContainer.getAccDismissWidth() + ACC_RTL_TOLERANCE;
		}
		scrollTo(mEndOfCV, getScrollY());
		mTargetScrollX = mEndOfCV;
		mIsRTLScroll = false;
	}


	public List<KPTSuggestion> getSuggestions(){
		if(mSuggestions != null){
			return mSuggestions;
		}
		return null;
	}

	public void clear() {
		mSuggestions = EMPTY_LIST;
		mTouchX = OUT_OF_BOUNDS;
		mSelectedSuggestion = null;
		mSelectedIndex = -1;
		mShowingAddToDictionary = false;
		invalidate();
		Arrays.fill(mWordX, 0);
		/*if (mPreviewPopup.isShowing()) {
			mPreviewPopup.dismiss();
		}*/
	}

	public void insertSuggestion(){
		if(mSelectedSuggestion != null) {
			if (mShowingAddToDictionary) {
				// longPressFirstWord();
				clear();
			} else if (mSuggestionMode != SUGGESTIONS_DEFAULT) {
				// Do nothing
			} else {
				/*
				 * if (!mShowingCompletions) {
				 * KPTTextEntryState.acceptedSuggestion
				 * (mSuggestions.get(0).getsuggestionString(),
				 * mSelectedSuggestion.getsuggestionString()); }
				 */
				if (!mIsFrameWorkEditorSuggestion) {

					/**
					 * There is no difference between ACC and Normal Suggestion,
					 * Core will handle ACC suggestion and normal suggestion by itself.
					 * So it is not required to insert ACC seperately with SPACE.
					 */

					mService.pickSuggestionManually(mSelectedIndex,
							mSelectedSuggestion);


				}else{
					// Frame work suggestion
					mService.pickFrameWorkSuggestion(mSelectedIndex,mSelectedSuggestion);
				}
				mSelectedSuggestion = null;
				mSuggestions = null; // Kashyap: Bug Fix 5756
			}
		} else if (mSuggestionMode == SUGGESTIONS_NO_DICTIONARY_INSTALLED) {
			/*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mService);
			
			if (!prefs.getBoolean(KPTConstants.PREFERENCE_EULA_ACCEPTED, false)) {
				KPTAdaptxtIMESettings.showEULA(mContext, false);
			}else if (!prefs.getBoolean(KPTConstants.PREFERENCE_SETUP_COMPLETED, false)) {
				KPTAdaptxtIMESettings.showWizard(mContext);
			}else{
				Intent intent = new Intent(mService, KPTAddonManager.class);
				intent.putExtra(KPTAddonManager.SHOW_TAB_KEY, KPTAddonManager.DOWNLOAD_TAB);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				// fix for TP item 6816
				mService.handleClose(false);
				mService.startActivity(intent);*/
		} else if (mSuggestionMode == SUGGESTIONS_BASE_EXPIRED) {
			/*Intent intent = new Intent(mService, KPTAdaptxtIMESettings.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mService.handleClose(false);
			mService.startActivity(intent);*/
			
			/*Intent intent = new Intent();
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			try {
				intent.setData(Uri.parse("market://details?id="+KPTConstants.APP_PACKAGE_NAME_PRO));
				mService.startActivity(intent);
			    
			} catch (android.content.ActivityNotFoundException anfe) {
				intent.setData(Uri.parse("http://play.google.com/store/apps/details?id="+KPTConstants.APP_PACKAGE_NAME_PRO));
				mService.startActivity(intent);
			}*/

		}else if (mSuggestionMode == SUGGESTIONS_BASE_UPGRADE) {
			//mService.showUpgradeDialog();
		}else if (mSuggestionMode == SUGGESTIONS_ADDON_UPGRADING) {
			//Toast.makeText(mContext, "UPGRADING ADDONS", Toast.LENGTH_SHORT).show();
		}/*else if(mSuggestionMode == SUGGESTIONS_BLANK_SUGGESTIONS){
			
		}*/
	}

	/**
	 * For flick through from keyboard, call this method with the x coordinate
	 * of the flick gesture.
	 * 
	 * @param x
	 */
	public void takeSuggestionAt(float x) {
		mTouchX = (int) x;
		// To detect candidate
		onDraw(null);
		if (mSelectedSuggestion != null) {
			mService
			.pickSuggestionManually(mSelectedIndex, mSelectedSuggestion);
		}
		invalidate();
	/*	mHandler.sendMessageDelayed(mHandler
				.obtainMessage(MSG_REMOVE_THROUGH_PREVIEW), 200);*/
	}


	public void hidePreview() {
		//mCurrentWordIndex = OUT_OF_BOUNDS;
		/*if (mPreviewPopup.isShowing()) {
			mHandler.sendMessageDelayed(mHandler
					.obtainMessage(MSG_REMOVE_PREVIEW), 60);
		}*/
	}

	/**
	 * It displays the preview of the suggestions which are on overlay of SIP.
	 * 
	 * @param wordIndex
	 *            The index of the suggestion.
	 * @param word
	 *            The suggestion string
	 * @param x
	 *            the X co- ordinate to show the visual feedback
	 * @param y
	 *            the Y co- ordinate to show the visual feedback
	 */
	/*public void showSuggPopUpPreview(int wordIndex, String word, int x, int y) {
		int oldWordIndex = mCurrentPopUPWordIndex;
		mCurrentPopUPWordIndex = wordIndex;
		if (oldWordIndex != mCurrentPopUPWordIndex) {
			if (wordIndex == OUT_OF_BOUNDS) {
				hidePreview();
			} else {
				//TP #15674. Issue with visual feedback when suggestion is selected from extended sbar
				mPreviewText.setTextColor(Color.BLACK);
				mPreviewText.setBackgroundDrawable(getResources().getDrawable(
						R.drawable.candidate_feedback_background));
				mPreviewText.setText(word);
				mPreviewText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
						MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

				final int popupWidth;
				int wordWidth = 0;
				int popupHeight = 0;
				int suggswidth = getWidth() / 3;

				if (word != "") {
					wordWidth = (int) (mPaint.measureText(word, 0, word.length()) + X_GAP * 2);

					popupWidth = wordWidth + mPreviewText.getPaddingLeft()
							+ mPreviewText.getPaddingRight();

					popupHeight = mPreviewText.getMeasuredHeight();
				} else {
					// The w & h is changed based on density
					popupWidth = ((int) (MIN_DISMISS_DIPS * scale));
					popupHeight = ((int) (MIN_DISMISS_DIPS * scale));
				}
				int popupPreviewX;
				int popupPreviewY;

				popupPreviewX = x - mPreviewText.getPaddingLeft() + suggswidth / 2;
				popupPreviewY = y - popupHeight + getHeight() / 4;

				mHandler.removeMessages(MSG_REMOVE_PREVIEW);
				int[] offsetInWindow = new int[2];
				getLocationInWindow(offsetInWindow);

				if (mPreviewPopup.isShowing()) {
					mPreviewPopup.update(popupPreviewX, popupPreviewY - 10
							+ offsetInWindow[1],
							popupWidth, popupHeight);
				} else {
					mPreviewPopup.setWidth(popupWidth);
					mPreviewPopup.setHeight(popupHeight);

					if (mService.mInputView.isShown() && mService.mInputView.getWindowToken() != null) {
						mPreviewPopup.showAtLocation(this, Gravity.NO_GRAVITY,
								popupPreviewX + mAccWidth, popupPreviewY - 10
								+ offsetInWindow[1]);
					}
					else{
						mPreviewPopup.showAtLocation(this, Gravity.NO_GRAVITY,
								0,0);
					}
				}
				mPreviewText.setVisibility(VISIBLE);
			}
		}
	}*/

	/*protected void showPreview(int wordIndex, String altText,int candidate_type,int absX,int absY) {
		if (mTheme == null) {
			return;
		}
		int oldWordIndex = mCurrentWordIndex;
		mCurrentWordIndex = wordIndex;
		CharSequence word ="";
		// shared preference to know the current theme
		if (pref == null){
			pref=PreferenceManager.getDefaultSharedPreferences(getContext());
		}
		//mThemeMode = Integer.parseInt(pref.getString(
		//KPTConstants.PREF_THEME_MODE, "1"));

		mPreviewText.setTypeface(mTheme.getUserSelectedFontTypeface());
		mPreviewText.setTextColor(Color.BLACK);

		// If index changed or changing text
		if (oldWordIndex != mCurrentWordIndex || altText != null) {
			if (wordIndex == OUT_OF_BOUNDS) {
				hidePreview();
			} else {
				word ="";
				switch(candidate_type){
				case CANDIDATE_TYPE_DEFAULT:
					mPreviewText.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateFeedback));


					word = altText != null ? altText : mSuggestions.get(
							wordIndex).getsuggestionString();
					mPreviewText.setText(word);
					break;

					//Preview on clicking dismiss button
				case CANDIDATE_TYPE_ACC_DISSMISS:
					word = "";
					mAccWidth=0;
					mPreviewText.setTypeface(mTheme.getCustomFontTypeface());
					mPreviewText.setText(mContext.getResources().getString(R.string.kpt_icontext_acc_dismiss));
					if(mThemeType == KPTConstants.THEME_CUSTOM && mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG] != -1) {
						mPreviewText.setBackgroundDrawable(new ColorDrawable(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG]));
						//Changed from PRIMARY_COLOR to SECONDARY
						mPreviewText.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]); 
					}else {
						//mPreviewText.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyBackground));
						//mPreviewText.setText(mResource.getString(R.string.kpt_icontext_acc_dismiss));
						//mPreviewText.setTextColor(mTheme.getResources().getColor(mTheme.mPreviewTextColor));

						mPreviewText.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mAccDismissFeedback));
						mPreviewText.setText(word);
					}
					break;
				case CANDIDATE_TYPE_DELETE_WORD:
					mPreviewText.setTypeface(mTheme.getCustomFontTypeface());
					mPreviewText.setText(mContext.getResources().getString(R.string.kpt_icontext_word_delete));
					if(mThemeType == KPTConstants.THEME_CUSTOM && mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG] != -1) {
						mPreviewText.setBackgroundDrawable(new ColorDrawable(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEY_BG]));
						mPreviewText.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_PRIMARY_CHAR]);
					} else {
						//mPreviewText.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mKeyBackground));
						//mPreviewText.setText(mResource.getString(R.string.kpt_icontext_word_delete));
						//mPreviewText.setTextColor(mTheme.getResources().getColor(mTheme.mPreviewTextColor));

						mPreviewText.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateDeleteWordFeedback));
						mPreviewText.setText(word);
					}
					word ="";
					mAccWidth = 0;
					break;
				case CANDIDATE_TYPE_ACC:
					mPreviewText.setBackgroundResource(R.drawable.candidate_feedback_background);
					word = mAccString;
					mAccWidth = 0;
					mPreviewText.setText(word);
					break;
				}

				mPreviewText.measure(MeasureSpec.makeMeasureSpec(0,
						MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(
								0, MeasureSpec.UNSPECIFIED));

				final int popupWidth ;
				int wordWidth = 0;
				int popupHeight = 0;

				//Bug 6311, 6320
				if(word != "") {
					wordWidth = (int) (mPaint.measureText(word, 0, word
							.length()) + X_GAP * 2);

					popupWidth = wordWidth
							+ mPreviewText.getPaddingLeft()
							+ mPreviewText.getPaddingRight();

					popupHeight = mPreviewText.getMeasuredHeight();
				}
				else {
					//The w & h is changed based on density
					popupWidth = ((int) (MIN_DISMISS_DIPS * scale));
					if(mPreviewText.getMeasuredHeight() != 0) {
						popupHeight = mPreviewText.getMeasuredHeight();
					} else {
						popupHeight = ((int) (MIN_DISMISS_DIPS * scale));
					}
				}

				int popupPreviewX;
				int popupPreviewY;
				//  RTL support for candidate bar:Auto correction box should not overlap with the suggesstions.
				if(mRTL && (candidate_type == CANDIDATE_TYPE_ACC_DISSMISS || candidate_type == CANDIDATE_TYPE_ACC || candidate_type == CANDIDATE_TYPE_DELETE_WORD) ){

					popupPreviewX = absX;
					//popupPreviewY = absY-200;	
					popupPreviewY = -popupHeight;	
				}else{
					popupPreviewX = mWordX[wordIndex] + +((getWidth() / 3) / 4);
					popupPreviewY = -popupHeight;
				}

				mHandler.removeMessages(MSG_REMOVE_PREVIEW);
				int deleteIconWidth = ((KPTCandidateViewContainer) this.getParent()).getDeleteIconWidth();
				int[] offsetInWindow = new int[2];
				getLocationInWindow(offsetInWindow);
				int expandWidth = ((KPTCandidateViewContainer) this.getParent()).getSuggestionBarExpandWidth();
				if (mPreviewPopup.isShowing()) {
					if (mAccString != null && mIsAcc) {
						// for acc dismiss icon width
						int accWidth = ((KPTCandidateViewContainer) this.getParent()).getAccDismissWidth();

						popupPreviewX = popupPreviewX + accWidth;
					}
					if(((KPTCandidateViewContainer) this.getParent()).isDeleteIconVisible() && candidate_type != CANDIDATE_TYPE_DELETE_WORD){

						popupPreviewX = popupPreviewX + deleteIconWidth;
					}
					if(mRTL && (((KPTCandidateViewContainer) this.getParent()).isDeleteIconVisible()) && candidate_type != CANDIDATE_TYPE_DELETE_WORD){
						popupPreviewX = popupPreviewX - deleteIconWidth;
					}
					mPreviewPopup.update(popupPreviewX, popupPreviewY + offsetInWindow[1],
							popupWidth, popupHeight);
				} else {
					mPreviewPopup.setWidth(popupWidth);
					mPreviewPopup.setHeight(popupHeight);

					if(!mRTL){
						mExtraWidth=0;
					}
					else {
						mExtraWidth=mExtraWidth+((KPTCandidateViewContainer) this.getParent()).getSuggestionBarExpandWidth();
					}

					if(((KPTCandidateViewContainer) this.getParent()).isDeleteIconVisible() && candidate_type != CANDIDATE_TYPE_DELETE_WORD){

						popupPreviewX = popupPreviewX + deleteIconWidth;
					}
					if(mRTL && (((KPTCandidateViewContainer) this.getParent()).isDeleteIconVisible()) && candidate_type != CANDIDATE_TYPE_DELETE_WORD){
						popupPreviewX = popupPreviewX - deleteIconWidth;
					}

					mPreviewPopup.showAtLocation(this, Gravity.NO_GRAVITY, popupPreviewX
							+ mAccWidth+mExtraWidth , popupPreviewY // // bug fix for 6167
							// --karthik
							+ offsetInWindow[1]);
				}
				mPreviewText.setVisibility(VISIBLE);
			}
		}
	}*/

	private void removeHighlight() {
		mTouchX = OUT_OF_BOUNDS;
		invalidate();
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		hidePreview();
	}

	public void setIsAcc(boolean isAcc) {
		mIsAcc = isAcc;
	}

	public boolean isAcc() {
		return mIsAcc;
	}

	public void setAccString(String mAccString) {
		this.mAccString = mAccString;
	}

	public CharSequence getAccString() {
		return mAccString;
	}

	public void setAccWidth(int accWidth) {

		mAccWidth = accWidth;
	}

	/*public void SetPreviewTextVisibility(int gone) {
		mPreviewText.setVisibility(GONE);
		if (mPreviewPopup.isShowing()) {
			mPreviewPopup.dismiss();
		}
	}*/

	// to set RTL display status
	public void setRTL(boolean aRTL){
		mRTL = aRTL;
	}

	// to get RTL display status
	public boolean getRTL(){
		return mRTL;
	}

	public boolean getRTLScroll(){
		return mIsRTLScroll;
	}


	/* For TP item 6312: to identify the actual  width of the view during layout pass */
	@Override
	public void onLayout (boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		if(mConfigurationChanged && mService != null && getWidth() != 0)
		{
			// TP item 6312: to handle candidateView for device configuration change scenario
			mConfigurationChanged = false;
			mService.forceRequestLayout();
		}
	}

	// Used by KPTAdaptxtIME for notifying the orientation change
	public void notifyConfigurationChange(boolean status)
	{
		mConfigurationChanged = status;
	}

	private void updateTextColor() {
		if (mTheme == null) {
			return;
		}
		// TODO Auto-generated method stub
		/*mThemeType = mTheme.getCurrentThemeType();
		if(mTheme.isCustomizationInProgress || mThemeType == KPTConstants.THEME_CUSTOM) {
			mColorNormal = mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION];
			mColorRecommended = mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR];
		} else {*/
			//Resources res = getResources();

			mColorNormal = mTheme.getResources().getColor(mTheme.mCandidateFontColor);
			mColorRecommended = mTheme.getResources().getColor(mTheme.mCandidateFontColorRecommended);
		//}

		mModePaint.setColor(mColorNormal);
		//invalidate();
	}


	/**
	 * 
	 * @param isPvtModeOn
	 */
	public void setPvtModeOnOff(boolean isPvtModeOn) {
		if(mService!=null){
			mService.setPrivateMode(isPvtModeOn);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int max = Math.max(getMeasuredHeight(), ((KPTCandidateViewContainer)this.getParent()).getHeight());
		setMeasuredDimension(getMeasuredWidth(), max);
	}


	protected void checkOrientation()
	{
		switch(getResources().getConfiguration().orientation)
		{
		case  Configuration.ORIENTATION_LANDSCAPE:
			mOrientationLandscape = true;
			break;
		case Configuration.ORIENTATION_PORTRAIT:
			mOrientationLandscape = false;
			break;
		case Configuration.ORIENTATION_SQUARE:
			// dont know what to do
			break;
		case Configuration.ORIENTATION_UNDEFINED:
			// dont know what to do
			break;
		}
	}


	public void loadView(Context context, int themeMode){
		if (mTheme == null) {
			return;
		}
		mThemeType = themeMode;
		LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	

		mSuggModString = context.getResources().getStringArray(
				R.array.kpt_suggestion_mode_strings);
		mSelectionHighlight = getResources().getDrawable(R.drawable.kpt_list_selector_background_pressed);


		//mPreviewPopup = new PopupWindow(context);

		//mPreviewText = (TextView) inflate.inflate(R.layout.candidate_preview,null);
		//mPreviewText.setTextColor(getResources().getColor(mTheme.mCandidateFontColor));

		mColorNormal = mTheme.getResources().getColor(mTheme.mCandidateFontColor);
		mColorRecommended = mTheme.getResources().getColor(mTheme.mCandidateFontColorRecommended);

		//mPreviewPopup.setContentView(mPreviewText);
		//mPreviewPopup.setBackgroundDrawable(null);


		mPaint = new Paint();
		mPaint.setColor(mColorNormal);
		mPaint.setAntiAlias(true);
		//mPaint.setTextSize(mPreviewText.getTextSize());


		MIN_FONT_DIPS = getResources().getDimension(R.dimen.kpt_keytextsize);

		/*if(mCandidatePopup!= null){
			setTheme(mTheme);
		}*/

		mPaint.setTextSize(MIN_FONT_DIPS );//* scale + 0.5f);
		mPaint.setStrokeWidth(0);
		mPaint.setTextAlign(Align.CENTER);


		updateFontStyle();
//		setHorizontalFadingEdgeEnabled(true);
		setWillNotDraw(false);
		setHorizontalScrollBarEnabled(false);
		setVerticalScrollBarEnabled(false);
		notifyConfigurationChange(false);

		// get this from viewManger Class in future
		// for now put true

		checkOrientation();

	}

	public void setATRSuggestion() {
		String str = "+"
				+ mSuggestions.get(mSuggestions.size() - 1)
				.getsuggestionString();
		mSuggestions.get(mSuggestions.size() - 1).setsuggestionString(str);
	}

	@Override
	public boolean onTouch(View v, MotionEvent me) {

		// disable Xi key, if enabled
		/*if(KPTConstants.mXi_enable){
			mService.setXiKeyState(false);
		}*/

		mService.dismissPopupKeyboard();
		int action = me.getAction();
		int x = (int) me.getX();
		mTouchX = x;

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mScrolled = false;
			mStopInsertSuggestion = false;
			dismissExpandedSuggWindow();
			/*if(!PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(KPTSetupWizard.PREFERENCE_EULA_ACCEPTED, false)) {
				mService.requestHideSelf(0);
				KPTAdaptxtIMESettings.showEULA(getContext(), true);
			} else{*/
			if(mSuggestionMode == SUGGESTIONS_DEFAULT) {
				mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DELETE_WORD),300);
				invalidate();
			}
			//}
			break;
		case MotionEvent.ACTION_MOVE:
			/*
			 * if (y <= 0) { // Fling up!? if (mSelectedString != null) { if
			 * (!mShowingCompletions) {
			 * KPTTextEntryState.acceptedSuggestion(mSuggestions.get(0),
			 * mSelectedString); }
			 * mService.pickSuggestionManually(mSelectedIndex, mSelectedString);
			 * mSelectedString = null; mSelectedIndex = -1; } }
			 */

			// Fix for the Longpress lagging on suggestin bar for LG and carbon device(for these devices it is going to onmove continuously)


			int temp = (int)me.getX();
			if (temp <= startX-5 ||  temp >= startX+5) {
				startX = (int)me.getX();
				if(mSuggestionMode == SUGGESTIONS_DEFAULT) {
					mHandler.removeMessages(MSG_DELETE_WORD);
					mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DELETE_WORD),  300);
				}
			}

			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			if (!mStopInsertSuggestion && !mScrolled) {
				insertSuggestion();
			}
			mHandler.removeMessages(MSG_DELETE_WORD);
			mSelectedIndex = -1;
			removeHighlight();
			hidePreview();
			startX = -1;
			requestLayout();
			break;
		}
		return true;
	}
	/*
	 * Method used to register the touch listener for candidateview.
	 */

	public void registerOnTouchEvent(){ 
		KPTCandidateView.this.setOnTouchListener(KPTCandidateView.this);
	}

	public void setTheme(KPTAdaptxtTheme theme) {
		mTheme = theme;

		mResource = mTheme.getThemeContext().getResources();
		mThemeType = mTheme.getCurrentThemeType();
	}

	public void updateFontStyle() {
		if (mTheme == null) {
			return;
		}
		mPaint.setTypeface(mTheme.getUserSelectedFontTypeface());	
		mModePaint.setTypeface(mTheme.getUserSelectedFontTypeface());
		if(mCandidatePopup != null) {
			mCandidatePopup.setTheme(mTheme);
			
			mCandidatePopup.updateFont();
		}
		invalidate();
	}

	public void setExpandView(KPTCandidateViewPopup mExpandSuggView) {
		this.mCandidatePopup = mExpandSuggView;
	}
	
	@Override
	public boolean isShown() {
		if(View.VISIBLE == getVisibility()){
			return true;
		}else{
			return false;
		}
	}
}
