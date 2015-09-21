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



import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.util.KPTConstants;

public class KPTCandidateViewContainer extends LinearLayout implements OnTouchListener{

	private static final int SPAC_KEY = 32;
	private static final int DEFAULT_CONTAINER_DISPLAY_WIDTH = 0; 
	private static final int MSG_REMOVE_PREVIEW_WITH_SPACE = 1;
	//private static final int MSG_REMOVE_PREVIEW_WITHOUT_SPACE = 2;
	private static final int RED_THEME = 3;
	private static final int GREEN_THEME = 4;
	private boolean mIsAcc = true;
	private boolean isAccDismissed;

	private View mSuggestStrip;
	
	
	/**
	 * this will be visible only if custom theme
	 */
	private TextView mPrivateKeyLTR;
	private TextView mPrivateKeyRTL;

	private TextView mSuggestionBarExpand;
	private ImageView mdividerxml=null;
	
	
	private boolean mIsRTL;
	
	//private TextView mAccDismissLTR;
	//private View mAccLayoutLTR;	
	//private TextView mDeleteIconLTR;
	//private ImageView mDeletewordDividerLTR;
	
	//private TextView mAccDismissRTL;
	//private View mAccLayoutRTL;	
	//private TextView mDeleteIconRTL;
	//private ImageView mDeletewordDividerRTL;
	
	private SharedPreferences pref;
	private KPTCandidateView mCandidates;
	private KPTAdaptxtTheme mTheme;
	private int mSuggestionMode;
	private boolean hideAccButton = false;
	
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REMOVE_PREVIEW_WITH_SPACE:
				//mCandidates.SetPreviewTextVisibility(GONE);
				mCandidates.getService().dismissCandidate(SPAC_KEY);
				break;
			/*case MSG_REMOVE_PREVIEW_WITHOUT_SPACE: //Bug 6305
				mCandidates.SetPreviewTextVisibility(GONE);
				//mCandidates.getService().handleUserInput(SPAC_KEY);
				break;*/
			}
		}
	};

	public KPTCandidateViewContainer(Context screen, AttributeSet attrs) {
		super(screen, attrs);
	}

	public void initViews() {
		if (mTheme == null) {
			return;
		}
		if (mCandidates == null) {
			mSuggestStrip=findViewById(R.id.suggest_strip);
			pref=PreferenceManager.getDefaultSharedPreferences(getContext());
			
			mCandidates = (KPTCandidateView) findViewById(R.id.candidates);
			mSuggestionBarExpand = (TextView) findViewById(R.id.candidate_expand_suggestions);
			/*mAccDismissLTR = (TextView) findViewById(R.id.acc_head_ltr);
			mDeleteIconLTR = (TextView) findViewById(R.id.delete_icon_ltr);
			mAccDismissRTL = (TextView) findViewById(R.id.acc_head_rtl);
			mDeleteIconRTL = (TextView) findViewById(R.id.delete_icon_rtl);
			*/
			//this will be displayed only if custom theme and private is enabled
			mPrivateKeyLTR = (TextView) findViewById(R.id.private_custom_theme_ltr);
			mPrivateKeyLTR.setTypeface(mTheme.getCustomFontTypeface());
			mPrivateKeyLTR.setText(getContext().getString(R.string.kpt_icontext_private_mode));
			mPrivateKeyLTR.setVisibility(View.GONE);
			mPrivateKeyRTL = (TextView) findViewById(R.id.private_custom_theme_rtl);
			mPrivateKeyRTL.setTypeface(mTheme.getCustomFontTypeface());
			mPrivateKeyRTL.setText(getContext().getString(R.string.kpt_icontext_private_mode));
			mPrivateKeyRTL.setVisibility(View.GONE);
			
		/*	mDeletewordDividerLTR = (ImageView) findViewById(R.id.delete_word_divider_ltr);
			mAccLayoutLTR = findViewById(R.id.acc_parent_ltr);
			mDeletewordDividerRTL= (ImageView) findViewById(R.id.delete_word_divider_rtl);
			mAccLayoutRTL = findViewById(R.id.acc_parent_rtl);*/
			mdividerxml = (ImageView) findViewById(R.id.xmldivider);

			if (!pref.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false)) {
				mSuggestStrip.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggBG));
			}
			
		/*	mDeletewordDividerLTR.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggDivider));
			//mDeleteIconLTR.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateDeleteWord));
			mDeletewordDividerRTL.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggDivider));*/
			//mDeleteIconRTL.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateDeleteWord));
					
		}/*else if (!pref.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false)) {
			mSuggestStrip.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggBG));
		}*/else if (mCandidates != null && !mCandidates.getRTL()) {
			mSuggestStrip.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggBG));
		} else {
			mSuggestStrip.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggBG));
		}

		mdividerxml.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggDivider));

		//mAccDismissLTR.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mAccHead));
		//mDeletewordDividerLTR.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggDivider));
		//mAccDismissRTL.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mAccHead));
		//mDeletewordDividerRTL.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggDivider));

		mSuggestionBarExpand.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCandidates.isShowingSuggPopUp()) {
					mCandidates.dismissExpandedSuggWindow();
					//mSuggestionBarExpand.setBackgroundResource(R.drawable.kpt_all_sug_open);
				} else {
					mCandidates.suggestionBarPopUp();
					//mSuggestionBarExpand.setBackgroundResource(R.drawable.kpt_all_sug_close);
				}
				changeExpandIcon(mTheme.getCurrentThemeType());
			}
		});

		//mAccLayoutLTR = findViewById(R.id.acc_parent_ltr);
		//mAccLayoutRTL = findViewById(R.id.acc_parent_rtl);

		//Bug fix 6348, changing the button click listener to touch listener
		//mAccDismissLTR.setOnTouchListener(new AccDismissTouchListner());
		//mAccDismissRTL.setOnTouchListener(new AccDismissTouchListner());

		//changing the button click listener to touch listener for delete icon in suggestion bar.
		//mDeleteIconLTR.setOnTouchListener(new DeleteIconTouchListner());
		//mDeleteIconRTL.setOnTouchListener(new DeleteIconTouchListner());
		
		hideAccButton = pref.getBoolean(KPTConstants.PREF_HIDE_ACC_KEY, false);
		
		updatePrivateKey(mTheme.getCurrentThemeType());
		
		//Bug Fix : 19960
		if(mCandidates.isShowingSuggPopUp()){
			mCandidates.dismissExpandedSuggWindow();
		}
	}

	@Override
	public void requestLayout() {
		
		if (mCandidates != null) {
			int availableWidth = mCandidates.getWidth();
			if(availableWidth == 0)
			{
				/* TP item 6312: at this point the Layout pass was not yet completed by the framework
				 * (just after orientation was changed)as a result of which the CandidateViewContainer is unable to get the width
				 * of candidateView. So we notify the candidateView about the orientation change
				 * to take care of initiating the requestLayout() call during the layout pass.
				 * */
				mCandidates.notifyConfigurationChange(true);
			}
		}
		super.requestLayout();
	}

	//  RTL support for candidate bar:Auto correction box should not overlap with the suggesstions.

	@Override
	public void onLayout (boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		// now the width properties required for RTL candidate scrolling should be appropriate
		/*if(mCandidates != null && mCandidates.getRTL() && mCandidates.getRTLScroll()){
			//mCandidates.checkRTLscrolling();

		}*/
	}

	public void updateView() {
		if (mCandidates == null) {
			return;
		}
		
		/*if (KPTConstants.mIsNavigationBackwar || mCandidates.mService.isUnkownEditor()) {
			mDeleteIconLTR.setVisibility(GONE);
			mDeletewordDividerLTR.setVisibility(GONE);
			mDeleteIconRTL.setVisibility(GONE);
			mDeletewordDividerRTL.setVisibility(GONE);
		} else if(mSuggestionMode == KPTCandidateView.SUGGESTIONS_DEFAULT){
			if(mIsRTL){
				mDeleteIconRTL.setVisibility(VISIBLE);
				mDeletewordDividerRTL.setVisibility(VISIBLE);	
				mDeleteIconLTR.setVisibility(GONE);
				mDeletewordDividerLTR.setVisibility(GONE);
			}else {
				mDeleteIconLTR.setVisibility(VISIBLE);
				mDeletewordDividerLTR.setVisibility(VISIBLE);
				mDeleteIconRTL.setVisibility(GONE);
				mDeletewordDividerRTL.setVisibility(GONE);
			}
		}
		if (mAccLayoutRTL != null && mAccLayoutLTR != null) {
			
			boolean isXiEnable = KPTConstants.mXi_enable;
			// Don't show the Acc dismiss when the Xi key is highlighted.
			boolean shouoldShowAccDismiss = mCandidates.isAcc() && !isXiEnable;
			isAccDismissed = shouoldShowAccDismiss;//mCandidates.isAcc();
			
			//System.out.println("GONE ACC LAYOUT ------------> "+shouoldShowAccDismiss);
			//System.out.println("GONE hideAccButton ---------> "+hideAccButton);
			
			if(mIsRTL){
				mAccLayoutRTL.setVisibility((shouoldShowAccDismiss && !hideAccButton) ? VISIBLE : GONE);
				mAccLayoutLTR.setVisibility(GONE);
			}else{
				mAccLayoutLTR.setVisibility((shouoldShowAccDismiss && !hideAccButton) ? VISIBLE : GONE);
				mAccLayoutRTL.setVisibility(GONE);
			}
		}*/
		
		// calculate the width that can be used to preview selected candidate.
		if (mCandidates.getAccString() != "") // bug fix for 6167
		{
			mCandidates.setAccWidth((int)getResources().getDimension(R.dimen.kpt_draw_space_key_arrows));
			
			// --karthik
			/*if(mIsRTL){
				
			}else{
				//mCandidates.setAccWidth(mAccDismissLTR.getWidth());
				mCandidates.setAccWidth((int)getResources().getDimension(R.dimen.draw_space_key_arrows));
				
			}*/
		} else
			mCandidates.setAccWidth(DEFAULT_CONTAINER_DISPLAY_WIDTH);

		//mDeleteIcon.setBackgroundDrawable(getResources().getDrawable(mTheme.mCandidateDeleteWord));

	}

	public boolean onTouch(View v, MotionEvent event) {
		/*
		 * pref=PreferenceManager.getDefaultSharedPreferences(getContext());
		 * mThemeMode = Integer.parseInt(pref.getString(
		 * KPTConstants.PREF_THEME_MODE, "0")); //TODO need to check the
		 * necessary of this function.. switch (mThemeMode) { case
		 * KPTAdaptxtIME.DARK_THEME: if(!
		 * pref.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false))
		 * mSuggestStrip
		 * .setBackgroundDrawable(getResources().getDrawable(R.drawable
		 * .keyboard_suggest_strip)); else
		 * mSuggestStrip.setBackgroundDrawable(getResources
		 * ().getDrawable(R.drawable.keyboard_suggest_strip_stoplearning));
		 * break; case KPTAdaptxtIME.BRIGHT_THEME: if(!
		 * pref.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false))
		 * mSuggestStrip
		 * .setBackgroundDrawable(getResources().getDrawable(R.drawable
		 * .keyboard_suggest_strip_val)); else
		 * mSuggestStrip.setBackgroundDrawable
		 * (getResources().getDrawable(R.drawable
		 * .keyboard_suggest_strip_stoplearning_val)); break; }
		 */
		return false;

	}
	
	
	
	public void resetAccDismiss() {/*
		if(mIsRTL){
			mAccDismissRTL.setVisibility(VISIBLE);
		}else {
			mAccDismissLTR.setVisibility(VISIBLE);
		}
	*/}

	/**
	 * returns true if acc suggestion is being shown
	 * 
	 * @return
	 */
	public boolean getAccDsmissState() { 
		// TODO Auto-generated method stub
		//return mAccDismiss.getVisibility()== VISIBLE  && mIsAcc;
		return isAccDismissed /* && mIsAcc*/;
	}

	public void setAccDismissState(boolean accState){
		isAccDismissed = accState;
	}

	public boolean isAccApplicableOnSpace() {
		if(mCandidates != null /*&& (mAccDismissLTR != null||mAccDismissRTL !=null)*/)  {
			//return (mAccDismiss.getVisibility()== VISIBLE) && mCandidates.isAcc() && mIsAcc;
			return (isAccDismissed) && mCandidates.isAcc() && mIsAcc;
		} else {
			return false;
		}
	}

	public void setAcc(boolean isAcc) {
		mIsAcc  = isAcc;
	}


	public int getAccDismissWidth()
	{
		/*if(mIsRTL){
			
			if (mAccDismissRTL != null) {
				return mAccDismissRTL.getWidth();
			}
		}else{
			if (mAccDismissLTR != null) {
				return mAccDismissLTR.getWidth();
			}

		}*/
		return 0;
	}
	
	public int getDeleteIconWidth()
	{
		
		int delIconWidth = (int) getResources().getDimension(R.dimen.kpt_draw_space_key_arrows);
		return delIconWidth;
		
		/*if(mIsRTL){
			
			if (mDeleteIconRTL != null) {
				return mDeleteIconRTL.getWidth();
			}
		}else{
			if (mDeleteIconLTR != null) {
				int delIconWidth = (int) getResources().getDimension(R.dimen.draw_space_key_arrows);
				//return mDeleteIconLTR.getWidth();
				return delIconWidth;
			}
			
		}
		return 0;*/
	}
	
	/*public boolean isDeleteIconVisible(){
		if(mIsRTL){
		return mDeleteIconRTL.isShown();
		}else{
			return mDeleteIconLTR.isShown();	
		}
		
	}*/
	
	
	
	public int getSuggestionBarExpandWidth(){
		if (mSuggestionBarExpand != null) {
			return mSuggestionBarExpand.getWidth();
		}
		return 0;
	}
	
	/**
     * Change the expand icon based on the current position either already showing or not.
     */
    public void changeExpandIcon(int themeMode) {
    	if (mTheme == null) {
			return;
		}
    	if (mSuggestionBarExpand == null) {
			return;
		}
    	//int themeMode = Integer.parseInt(pref.getString(KPTConstants.PREF_THEME_MODE, "1"));
    	mSuggestionBarExpand.setTypeface(Typeface.DEFAULT);
    	
    	 if (mCandidates.isShowingSuggPopUp()) {
    		 /*if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) {
    			 mSuggestionBarExpand.setTypeface(mTheme.getCustomFontTypeface());
    			 mSuggestionBarExpand.setBackgroundDrawable(null);
    			 mSuggestionBarExpand.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION]);
    			 mSuggestionBarExpand.setText(getResources().getString(R.string.kpt_icontext_suggestion_expand));
    		 } else {*/
    			 mSuggestionBarExpand.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mSuggestionbarClose));
    			 mSuggestionBarExpand.setText(null);
    		// }
    	 } else {
    		 /*if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) {
    			 mSuggestionBarExpand.setTypeface(mTheme.getCustomFontTypeface());
    			 mSuggestionBarExpand.setBackgroundDrawable(null);
    			 mSuggestionBarExpand.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION]);
    			 mSuggestionBarExpand.setText(getResources().getString(R.string.kpt_icontext_suggestion_shrink));
    		 } else {*/
    			 mSuggestionBarExpand.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mSuggestionbarOpen));
    			 mSuggestionBarExpand.setText(null);
    		// }
    	 }
        mSuggestionBarExpand.setClickable(true);
    }

    /**
     * Disable the expand icon
     */
    public void setExpandIconDisable(int themeMode) {
    	if (mTheme == null) {
			return;
		}
    	if (mSuggestionBarExpand == null) {
			return;
		}
    	/*if(themeMode == KPTConstants.THEME_CUSTOM) {
    		mSuggestionBarExpand.setTypeface(mTheme.getCustomFontTypeface());
    		mSuggestionBarExpand.setBackgroundDrawable(null);
    		mSuggestionBarExpand.setText(getResources().getString(R.string.kpt_icontext_suggestion_shrink));
    	} else {*/
    		mSuggestionBarExpand.setTypeface(Typeface.DEFAULT);
    		//mSuggestionBarExpand.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION]);
    		mSuggestionBarExpand.setText(null);
    		mSuggestionBarExpand.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mSuggestionbarOpen));
    	//}

    	mSuggestionBarExpand.setClickable(false);
    }
    
    public void updateCandidateBar() {
    	if (mTheme == null) {
    		return;
    	}
    	if (mCandidates == null) {
    		return;
    	}
    	float FONT_DIPS = getResources().getDimension(R.dimen.kpt_sugg_icons_text_size);
    	if (pref == null) {
    		pref = PreferenceManager.getDefaultSharedPreferences(getContext());
    	}
    	int themeType = mTheme.getCurrentThemeType();
    	mSuggestStrip.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggBG));
    	if (!pref.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false)) {
    		mSuggestStrip.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggBG));
    	} else if (mCandidates != null && !mCandidates.getRTL()) {
    		mSuggestStrip.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggBG));
    	} else {
    		mSuggestStrip.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggBG));
    	}

    	//mDeletewordDividerLTR.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggDivider));
    	//mDeletewordDividerRTL.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggDivider));
    	mdividerxml.setBackgroundDrawable(mTheme.getResources().getDrawable(mTheme.mCandidateSuggDivider));

    	updatePrivateKey(themeType);
    	changeExpandIcon(themeType);

    	/*if(themeType == KPTConstants.THEME_CUSTOM) {
    		mAccDismissLTR.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);
    		mDeleteIconLTR.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);
    		mAccDismissRTL.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);
    		mDeleteIconRTL.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR]);
    	}else{*/
    		/*mAccDismissLTR.setTextColor(mTheme.getResources().getColor(mTheme.mSecondaryTextColor));
    		mDeleteIconLTR.setTextColor(mTheme.getResources().getColor(mTheme.mSecondaryTextColor));
    		mAccDismissRTL.setTextColor(mTheme.getResources().getColor(mTheme.mSecondaryTextColor));
    		mDeleteIconRTL.setTextColor(mTheme.getResources().getColor(mTheme.mSecondaryTextColor));*/
    	//}
    	
    	//hideAccButton = pref.getBoolean(KPTConstants.PREF_HIDE_ACC_KEY, false);
		//System.out.println("HIDE ACC 1 ------------> "+hideAccButton);

		/*if(!hideAccButton){
			mAccDismissLTR.setTypeface(mTheme.getCustomFontTypeface());
			mAccDismissLTR.setBackgroundDrawable(null);
			mAccDismissLTR.setText(getContext().getResources().getString(R.string.kpt_icontext_acc_dismiss));
			mAccDismissLTR.setTextSize(FONT_DIPS);
			
			mAccDismissRTL.setTypeface(mTheme.getCustomFontTypeface());
			mAccDismissRTL.setBackgroundDrawable(null);
			mAccDismissRTL.setText(getContext().getResources().getString(R.string.kpt_icontext_acc_dismiss));
			mAccDismissRTL.setTextSize(FONT_DIPS);
		}
    	
    	mDeleteIconLTR.setTypeface(mTheme.getCustomFontTypeface());
    	mDeleteIconLTR.setBackgroundDrawable(null);
    	mDeleteIconLTR.setText(getContext().getResources().getString(R.string.kpt_icontext_word_delete));
    	mDeleteIconLTR.setTextSize(FONT_DIPS);


    	mDeleteIconRTL.setTypeface(mTheme.getCustomFontTypeface());
    	mDeleteIconRTL.setBackgroundDrawable(null);
    	mDeleteIconRTL.setText(getContext().getResources().getString(R.string.kpt_icontext_word_delete));
    	mDeleteIconRTL.setTextSize(FONT_DIPS);*/

    	/*if(themeType == KPTConstants.THEME_CUSTOM
    			&& mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION_BG] != -1) {
    		mSuggestStrip.setBackgroundColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION_BG]);
    		if(mCandidates.mCandidatePopup != null) {
    			mCandidates.mCandidatePopup.setBackgroundColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_KEYBOARD_BG]);
    		}
    	}*/
    }

    private void updatePrivateKey(int themeType) {
    	if (mTheme == null) {
    		return;
    	}
    	/*if(themeType == KPTConstants.THEME_CUSTOM
    			&& mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION_BG] != -1) {
    		if(pref.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false)) {

    			if(mIsRTL){
    				mPrivateKeyRTL.setVisibility(View.VISIBLE);
    				mPrivateKeyRTL.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION]);
    				mPrivateKeyLTR.setVisibility(View.GONE);
    			}else{
    				mPrivateKeyLTR.setVisibility(View.VISIBLE);
    				mPrivateKeyLTR.setTextColor(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION]);
    				mPrivateKeyRTL.setVisibility(View.GONE);
    			}
    		} else {
    			mPrivateKeyLTR.setVisibility(View.GONE);
    			mPrivateKeyRTL.setVisibility(View.GONE);
    		}
    	} else {*/
    		if(pref.getBoolean(KPTConstants.PREF_PRIVATE_MODE, false)) {
    			if(mIsRTL){
    				mPrivateKeyRTL.setVisibility(View.VISIBLE);
    				mPrivateKeyRTL.setTextColor(mTheme.getResources().getColor(mTheme.mCandidatePrivateColor));
    				mPrivateKeyLTR.setVisibility(View.GONE);
    			}else{
    				mPrivateKeyLTR.setVisibility(View.VISIBLE);
    				mPrivateKeyLTR.setTextColor(mTheme.getResources().getColor(mTheme.mCandidatePrivateColor));
    				mPrivateKeyRTL.setVisibility(View.GONE);
    			}
    		}else {
    			mPrivateKeyLTR.setVisibility(View.GONE);
    			mPrivateKeyRTL.setVisibility(View.GONE);
    		}
    	//}
    }
	
	/**
	 * Change the visibility of the Expand icon based on the suggestion mode.
	 * @param suggestionMode - The mode of the suggestion like default,password,expiry,..
	 */
	public void updateVisibilityOfExpandIcon(int suggestionMode) {
		mSuggestionBarExpand.setVisibility(suggestionMode == KPTCandidateView.SUGGESTIONS_DEFAULT 
				? View.VISIBLE : View.GONE);
		 mSuggestionMode = suggestionMode;
	}
	
	public void setTheme(KPTAdaptxtTheme theme) {
		mTheme = theme;
	}

	public void setRTL(boolean isRTL) {
		// TODO Auto-generated method stub
		mIsRTL = isRTL;
	}
	
	public class AccDismissTouchListner implements View.OnTouchListener{
		//boolean mRtl = false;
		
		/*AccDismissTouchListner(boolean isRTL){
			mRtl = isRTL;
			
		}*/

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (mCandidates == null) {
				return true;
			}
			if(KPTConstants.mXi_enable){
				mCandidates.mService.setXiKeyState(false);
			}
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				//  RTL support for candidate bar:Auto correction box should not overlap with the suggesstions.
				if(mCandidates.getRTL()){
					int[] offsetInWindow = new int[2];
					//mAccDismissRTL.getLocationInWindow(offsetInWindow);
					//mCandidates.showPreview(0, "",KPTCandidateView.CANDIDATE_TYPE_ACC_DISSMISS,offsetInWindow[0],offsetInWindow[1] - mAccDismissRTL.getHeight());
				}
				else{
					//mCandidates.showPreview(0, "",KPTCandidateView.CANDIDATE_TYPE_ACC_DISSMISS,0,0);
				}
				break;
			}   
			case MotionEvent.ACTION_MOVE: {/*
				if(mIsRTL){
					if(! (event.getX() >mAccDismissRTL.getLeft() && event.getX() < mAccDismissRTL.getRight()
							&& event.getY()>mAccDismissRTL.getTop() && event.getY() < mAccDismissRTL.getBottom())) {
						if(mHandler.hasMessages(MSG_REMOVE_PREVIEW_WITHOUT_SPACE)) {
							mHandler.removeMessages(MSG_REMOVE_PREVIEW_WITHOUT_SPACE);
						}
						mHandler.sendEmptyMessageDelayed(MSG_REMOVE_PREVIEW_WITHOUT_SPACE, 0);
					}
					
				}else {
					if(! (event.getX() >mAccDismissLTR.getLeft() && event.getX() < mAccDismissLTR.getRight()
							&& event.getY()>mAccDismissLTR.getTop() && event.getY() < mAccDismissLTR.getBottom())) {
						if(mHandler.hasMessages(MSG_REMOVE_PREVIEW_WITHOUT_SPACE)) {
							mHandler.removeMessages(MSG_REMOVE_PREVIEW_WITHOUT_SPACE);
						}
						mHandler.sendEmptyMessageDelayed(MSG_REMOVE_PREVIEW_WITHOUT_SPACE, 0);
					}
					
				}
				
				break;
			*/}
			case MotionEvent.ACTION_UP: {
				mCandidates.hidePreview(); // Fix for TP item 6878,6924
				if(mIsRTL){
					/*if(event.getX() >mAccDismissRTL.getLeft() && event.getX() < mAccDismissRTL.getRight()
							&& event.getY()>mAccDismissRTL.getTop() && event.getY() < mAccDismissRTL.getBottom()) {
						isAccDismissed = false;
						mHandler.sendEmptyMessageDelayed(MSG_REMOVE_PREVIEW_WITH_SPACE, 0);
					} else {
						if(mHandler.hasMessages(MSG_REMOVE_PREVIEW_WITHOUT_SPACE)) {
							mHandler.removeMessages(MSG_REMOVE_PREVIEW_WITHOUT_SPACE);
						}
						mHandler.sendEmptyMessageDelayed(MSG_REMOVE_PREVIEW_WITHOUT_SPACE, 0);
					}*/
					
				}else{
				/*if(event.getX() >mAccDismissLTR.getLeft() && event.getX() < mAccDismissLTR.getRight()
						&& event.getY()>mAccDismissLTR.getTop() && event.getY() < mAccDismissLTR.getBottom()) {
					isAccDismissed = false;
					mHandler.sendEmptyMessageDelayed(MSG_REMOVE_PREVIEW_WITH_SPACE, 0);
				} else {
					if(mHandler.hasMessages(MSG_REMOVE_PREVIEW_WITHOUT_SPACE)) {
						mHandler.removeMessages(MSG_REMOVE_PREVIEW_WITHOUT_SPACE);
					}
					mHandler.sendEmptyMessageDelayed(MSG_REMOVE_PREVIEW_WITHOUT_SPACE, 0);
				}*/
				}
				mCandidates.dismissExpandedSuggWindow();
				break;
			}
			}
			return true;
		}
	}
	
	public class DeleteIconTouchListner implements View.OnTouchListener{
		@Override
		public boolean onTouch(View arg0, MotionEvent arg1) {

			switch (arg1.getAction()) {
			case MotionEvent.ACTION_DOWN: {

				KPTConstants.mIsNavigationBackwar = true;
				//  RTL support for candidate bar:Auto correction box should not overlap with the suggesstions.
				if (mCandidates == null){
					break;
				}
				if(mIsRTL){
					int[] offsetInWindow = new int[2];
					//mDeleteIconRTL.getLocationInWindow(offsetInWindow);
					//mCandidates.showPreview(0, "",KPTCandidateView.CANDIDATE_TYPE_DELETE_WORD,offsetInWindow[0],offsetInWindow[1] - mDeleteIconRTL.getHeight());
				}
				else{
					//mCandidates.showPreview(0, "",KPTCandidateView.CANDIDATE_TYPE_DELETE_WORD,0,0);
				}
				
				//Fix for TP Item-13001
					mCandidates.mService.playKeyClick(0);
					//mCandidates.mService.vibrate(0);
				break;
			}   
			case MotionEvent.ACTION_MOVE: 
				break;
			case MotionEvent.ACTION_UP: {
				if (mCandidates == null){
					break;
				}
				mCandidates.hidePreview(); // Fix for TP item 6878,6924
				mCandidates.mService.deleteWord();
				mCandidates.dismissExpandedSuggWindow();
			}
			break;
			}
			return true;
		}
	}
	
	public boolean isShown(){
		
		if(View.VISIBLE == this.getVisibility()){
			return true;
		}else{
			
			return false;
		}
		
	}
}
