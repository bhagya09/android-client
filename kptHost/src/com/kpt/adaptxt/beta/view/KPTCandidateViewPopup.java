package com.kpt.adaptxt.beta.view;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import com.kpt.adaptxt.beta.AdaptxtIME;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.core.coreservice.KPTSuggestion;
import com.kpt.adaptxt.beta.util.KPTConstants;

/**
 * Candidate view pop up class to display the pop-up of the candidate selected
 * 
 * 
 */
public class KPTCandidateViewPopup extends View implements View.OnTouchListener{

    // List of the suggestions received from core
    private List<KPTSuggestion> mSuggestions;

    // Number of max suggestions
    private static final int MAX_SUGGESTIONS = 32;

    // integer array of max width of the suggestions
    private int[] mWordWidth = new int[MAX_SUGGESTIONS];

    // integer array of x co-ordinate of the suggestions
    private int[] mWordX = new int[MAX_SUGGESTIONS];

    // minimum touchable width
    private int mMinTouchableWidth;

    // parent width
    private int parentWidth;

    // int storing normal color
    private int mColorNormal;

    // Paint
    private Paint paint;

    // background of key
    private Drawable mKeybackground;
    
    // Divider between suggestions.
    private Drawable mDivider;

    // X-position
    private int mTouchX = KPTCandidateView.OUT_OF_BOUNDS;

    // Y-position
    private int mTouchY;

    // Index at which suggestion is selected
    private int mSelectedIndex;

    // Adaptxt IME service
    private AdaptxtIME mService;

    // Candidate view container
    private KPTCandidateViewContainer mCandidateViewContainer;

    // selected Suggestion
    private KPTSuggestion mSelectedSuggestion;

    // Popup window to show suggestion bar
    private PopupWindow mSuggestionBarPopup;

    // Candidateview reference
	private KPTCandidateView mCandidateView;

	private Rect mBgPadding;

	private int x;

	private int y;
	
	private Context mContext;
	
	private static final int MSG_DELETE_WORD = 1;
	private boolean mStopInsertSuggestion=false;
	
	private SharedPreferences pref;
	
	 private int startX = -1;
	 
	private KPTAdaptxtTheme mTheme;

	private boolean isBelowICS;
	
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
	private boolean mIsFrameWorkEditorSuggestion;
	
	public KPTCandidateViewPopup(Context context, AttributeSet attr){
		super(context, attr);
		
		mContext = context;
		
		paint = new Paint();
        //paint.setTypeface(mTheme.getUserSelectedFontTypeface());
    	
       // paint.setColor(mColorNormal);
        paint.setAntiAlias(true);
        paint.setTextAlign(Align.CENTER);
        paint.setTextSize(20);
        paint.setStrokeWidth(0);
		pref = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		registerOnTouchEvent();
		
		isBelowICS = false;
	    if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
	       	isBelowICS = true;
	    }
	}
		
    /**
     * Constructor of candidate View pop up for initial initialization.
     * 
     * @param context
     *            - Context
     * @param candidateView  -CaAndidateView reference
     * @param suggestionBarPopup - PopUp window 
     */
	public KPTCandidateViewPopup(Context context, KPTCandidateView candidateView, PopupWindow suggestionBarPopup,AdaptxtIME service, 
			KPTAdaptxtTheme theme, int themeMode){
        super(context);
        mContext = context;
        mCandidateView = candidateView;
        mSuggestionBarPopup = suggestionBarPopup;
        mService = service;
        mTheme = theme;
        //mColorNormal = getResources().getColor(R.color.candidate_normal);
        paint = new Paint();
        //paint.setTypeface(mTheme.getUserSelectedFontTypeface());
    	if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM){
        	paint.setTypeface(mTheme.getUserSelectedFontTypeface());
        }else{
        	paint.setTypeface(Typeface.DEFAULT);
        }
       // paint.setColor(mColorNormal);
        paint.setAntiAlias(true);
        paint.setTextAlign(Align.CENTER);
        paint.setTextSize(20);
        paint.setStrokeWidth(0);
		pref = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		registerOnTouchEvent();
		
		isBelowICS = false;
	    if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
	       	isBelowICS = true;
	    }
	        
		//mThemeMode = Integer.parseInt(pref.getString(
     		//	KPTConstants.PREF_THEME_MODE, "1"));
    }

	public void setFrameWorkSuggestionState(boolean state){
		mIsFrameWorkEditorSuggestion = state; 
	}

    /*
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
    	
        if (canvas != null) {
            super.onDraw(canvas);
        }
        int count = 0;
        if (mSuggestions != null){
        	if(mSuggestions.size() > 15) {
        		count = 15;

        	}else{
        		count = mSuggestions.size();
        	}
        }

        int rowWidth = 0;
        int rowNum = -1;
        x = 0;
    	y = 0;
    	
        mMinTouchableWidth = (int) (getResources().getDisplayMetrics().density * 50);
//        canvas.translate(0, 10);
        int suggswidth = (getWidth()/3);
		if (mBgPadding == null) {
			mBgPadding = new Rect(0, 0, 0, 0);
			if (getBackground() != null) {
				getBackground().getPadding(mBgPadding);
			}
		}
        int popUpSuggStartPos = 3;
        if (!mCandidateView.mISCheckedSuggestion)
        	popUpSuggStartPos = 2;
        int counterValue = 0;
        
        if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) {
        	mColorNormal = mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION];
        } else {
        	mColorNormal = mTheme.getResources().getColor(mTheme.mCandidateFontColor);
        }
        
        int extraPixel = 0;
        if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) {
        	extraPixel = 1;
        }
       
        paint.setTypeface(mTheme.getUserSelectedFontTypeface());
		for (int suggPos = popUpSuggStartPos; suggPos < count; suggPos++) {
			CharSequence suggestion = mSuggestions.get(suggPos)
					.getsuggestionString();
			if (suggestion == null)
				continue;

			if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM && mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION_BG] != -1) {
				mKeybackground = new ColorDrawable(mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SUGGESTION_BG]);
			} else {
				mKeybackground = mTheme.getResources().getDrawable(mTheme.mCandidateSuggBG);
			}
			
			
			mDivider = mTheme.getResources().getDrawable(mTheme.mCandidateSuggDivider);
			mDivider.setVisible(true, true);

			paint.setColor(mColorNormal);

			boolean isAtrOn = pref.getBoolean(KPTConstants.PREF_ATR_FEATURE,
					true);

			if (isAtrOn
					&& (mSuggestions.get(suggPos).getsuggestionType() == KPTSuggestion.KPT_SUGGS_TYPE_ATR)) {
				
				int colorRecommended;
				if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) {
					colorRecommended = mTheme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR];
		        } else {
		        	colorRecommended = getResources().getColor(
								R.color.kpt_candidate_recommended_dk_fk);
		        }
				paint.setColor(colorRecommended);
			}
            
            int wordWidth;
            if (mWordWidth[suggPos] != 0) {
                wordWidth = mWordWidth[suggPos];
            } else {
                float textWidth = paint.measureText(suggestion, 0, suggestion.length());
                wordWidth = Math.max(mMinTouchableWidth, (int) textWidth + 10);
                mWordWidth[suggPos] = wordWidth;
            }
           //As per requirement there should be 3 suggestions for each row in pop-up window.
            //So, incrementing the row height for every 3 suggestions.
            if (counterValue % 3 == 0) {
            	rowNum++;
            	x = 0;
            	y = y +  getHeight()/4 + extraPixel;
            	counterValue = 0;
            }
            counterValue++;
            
            int pos = suggPos;
           if (mTouchX  >= x && mTouchX < x + suggswidth
					&& mTouchY != KPTCandidateView.OUT_OF_BOUNDS) {
        	   if (canvas != null && rowNum == mTouchY / (getHeight()/4)) { 
        		    mSelectedSuggestion = mSuggestions.get(pos);
				    mSelectedIndex = pos;
				    mKeybackground = getResources().getDrawable(R.drawable.kpt_list_selector_background_pressed);
				    //keybackground.setState(KEY_STATE_PRESSED_ON);
					//mCandidateView.showSuggPopUpPreview(mSelectedIndex, mSelectedSuggestion.getsuggestionString(), x, y);
        	   }
			}
            mWordX[suggPos] = rowWidth;
            if (canvas != null) {
            	
            	if(isBelowICS) {
					Rect bounds = mKeybackground.getBounds();
	            	bounds.left = x;
	            	bounds.top = y - getHeight()/4;
	            	bounds.right = x + getWidth()/3;
	            	bounds.bottom = y;
	            	
					canvas.save();
					canvas.clipRect(x, y - getHeight()/4, x + getWidth()/3, y);
					mKeybackground.setBounds(bounds);
					mKeybackground.draw(canvas);
					canvas.restore();
					
				} else {
					mKeybackground.setBounds(x, y - getHeight()/4,  x + getWidth()/3,y );
            		mKeybackground.draw(canvas);
				}
            	
            	String sug = mCandidateView.fitTextSize(suggestion.toString(), paint, suggswidth, false);
				if(sug != null) {
					suggestion = sug;
				}
				
            	canvas.drawText(suggestion, 0, suggestion.length(), x + suggswidth/2,  (y - (getHeight()/4)/3) , paint);

            	//Log.e("DrawTest", x+" "+y+" " + mDivider.getIntrinsicWidth()+" "+mDivider.getIntrinsicHeight());
            	//Log.e("DrawTest", "mDivider.isVisible()--"+mDivider.isVisible());
            	canvas.translate(x + suggswidth, 0);
            	 
            	mDivider.setBounds(0, y - getHeight()/4, mDivider.getIntrinsicWidth(), y);
            	mDivider.draw(canvas);
            	canvas.translate(mDivider.getIntrinsicWidth(), 0);
            	x += mDivider.getIntrinsicWidth();
            	
            	canvas.translate(-x - suggswidth, 0);
            }
			x += (getWidth()/ 3);
        }
    }

    /**
     * get the width of next suggestion to be displayed
     * 
     * @param suggestion
     * @return - int width of next suggestion
     */
    public int nextSuggestionWidth(String suggestion) {
        float textWidth = paint.measureText(suggestion, 0, suggestion.length());
        return Math.max(mMinTouchableWidth, (int) textWidth + 10);
    }

    /**
     * suggestions to be displayed
     * 
     * @param suggestions
     *            - List of suggestions received from core
     */
    public void setSuggestion(List<KPTSuggestion> suggestions) {
        mSuggestions = suggestions;
        invalidate();
    }

  /*  
     * 
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mTouchX = (int) event.getX();
        mTouchY = (int) event.getY();

     // disable Xi key, if enabled
		if(KPTAdaptxtIME.mXi_enable){
			mService.setXiKeyState(false);
		}
		
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
        	mStopInsertSuggestion = false;
        	mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DELETE_WORD), 500);
        case MotionEvent.ACTION_MOVE:
        	mHandler.removeMessages(MSG_DELETE_WORD);
        	mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DELETE_WORD), 500);
            invalidate();
            break;
        case MotionEvent.ACTION_UP:
//        	mCandidateView.showSuggPopUpPreview(mSelectedIndex,mSelectedSuggestion.getsuggestionString(),x,y);
            if (!mStopInsertSuggestion  && mSelectedSuggestion != null) {
            	insertPopupSuggestion();  
            }
            mHandler.removeMessages(MSG_DELETE_WORD);
            mSelectedIndex = -1;
            removeHighlight();
            mCandidateView.hidePreview();
            requestLayout();
            mCandidateView.dismissExpandedSuggWindow();
            break;
        }
        return true;
    }*/
    
    private void removeHighlight() {
		mTouchX = KPTCandidateView.OUT_OF_BOUNDS;
		invalidate();
	}


    /**
     * width of the keyboard
     * 
     * @param width
     */
    void setParentWidth(int width) {
        parentWidth = width;
    }

   /**
     * To dismiss popup
     */
    public void dismissPopUp() {
        mCandidateView.mCandidatePopup.setVisibility(View.GONE);
    }
    
    /**
     * Inser Suggestion into editor after selecting the suggestion
     */
    public void insertPopupSuggestion(){
    	
    	if(mSelectedSuggestion != null){
    		if (mCandidateView != null && !mCandidateView.mIsFrameWorkEditorSuggestion) {
    			mService.pickSuggestionManually(mSelectedIndex, mSelectedSuggestion);	
    		}else{
    			mService.pickFrameWorkSuggestion(mSelectedIndex, mSelectedSuggestion);
    		}
    		
    		mSelectedSuggestion = null;
    		dismissPopUp();
    		mCandidateViewContainer.changeExpandIcon(mTheme.getCurrentThemeType());
    	}
    }

    /**
     * To set Candidate container
     * 
     * @param container
     */
    public void setCandidateContainer(KPTCandidateViewContainer container) {
        mCandidateViewContainer = container;
    }
    
    /**
     * Handler to handle long press on the suggestion from candidatePopup window.
     */
    Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_DELETE_WORD:
				// Remove dialog is not displayed. we are inserting suggestion to editor
				if (!mIsFrameWorkEditorSuggestion) {
					mStopInsertSuggestion = true;
					removeHighlight();
					startX = -1;
					//dismissPopUp();
					mCandidateView.hidePreview();
					// currently stoping touch listner.
					KPTCandidateViewPopup.this.setOnTouchListener(null);
					mService.removeWordDialog(mSelectedSuggestion,mSelectedIndex,false);
				}else{
					insertPopupSuggestion();
				}
				break;
			}
		}
	};
	
	



	@Override
	public boolean onTouch(View v, MotionEvent event) {
		 mTouchX = (int) event.getX();
	        mTouchY = (int) event.getY();

	     // disable Xi key, if enabled
			/*if(KPTConstants.mXi_enable){
				mService.setXiKeyState(false);
			}
			*/
	        switch (event.getAction()) {
	        case MotionEvent.ACTION_DOWN:
	        	mStopInsertSuggestion = false;
	        	mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DELETE_WORD),  300);
	        case MotionEvent.ACTION_MOVE:
	        	// Fix for the Longpress lagging on suggestin bar for LG and carbon device(for these devices it is going to onmove continuously)
				
			      int temp = (int)event.getX();
	              if (temp <= startX-5 ||  temp >= startX+5) {
	                    startX = (int)event.getX();
	                    	mHandler.removeMessages(MSG_DELETE_WORD);
	                    	mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DELETE_WORD),  300);
	                   
	              }
	            invalidate();
	            break;
	        case MotionEvent.ACTION_UP:
//	        	mCandidateView.showSuggPopUpPreview(mSelectedIndex,mSelectedSuggestion.getsuggestionString(),x,y);
	            if (!mStopInsertSuggestion  && mSelectedSuggestion != null) {
	            	insertPopupSuggestion();  
	            }
	            mHandler.removeMessages(MSG_DELETE_WORD);
	            mSelectedIndex = -1;
	            removeHighlight();
	            startX = -1;
	            mCandidateView.hidePreview();
	            requestLayout();
	            mCandidateView.dismissExpandedSuggWindow();
	            break;
	        }
	        return true;
	}
	
	/*
	 * Method used to register touch listener to the candidateviewpopup
	 */
	public void registerOnTouchEvent(){ 
		KPTCandidateViewPopup.this.setOnTouchListener(KPTCandidateViewPopup.this);
	}
	
	public void updateFont() {
		paint.setTypeface(mTheme.getUserSelectedFontTypeface());	
	}

	public void setCandidateView(KPTCandidateView kptCandidateView) {
        mCandidateView = kptCandidateView;
	}

	public void setService(AdaptxtIME service) {
        mService = service;
	}

	public void setTheme(KPTAdaptxtTheme theme) {
        mTheme = theme;
        
        /*if(mTheme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM){
        	paint.setTypeface(mTheme.getUserSelectedFontTypeface());
        }else{*/
        	paint.setTypeface(Typeface.DEFAULT);
        //}
	}

}
