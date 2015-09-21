package com.kpt.adaptxt.beta;

import com.kpt.adaptxt.beta.view.AdaptxtEditText;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtEditTextEventListner;
import com.kpt.adaptxt.beta.view.AdaptxtEditText.AdaptxtKeyboordVisibilityStatusListner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.inputmethodservice.Keyboard;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


/**
 * When an activity hosts a keyboardView, this class allows several EditText's to register for it.
 *
 */
public class CustomKeyboard {

	protected static final int MSG_GET_VIEW_HEIGHT = 0;
	/** A link to the KeyboardView that is used to render this CustomKeyboard. */
	private MainKeyboardView mKeyboardView;
	/** A link to the activity that hosts the {@link #mKeyboardView}. */
	private Activity  mHostActivity;
	//private ScrollView mScroll;
	private LinearLayout mParentKeybaordView;
	private AdaptxtIME mAdaptxtIME;
	private View mParentViewHolder;
	private AdaptxtEditText mEdittext;
	//private HashSet<Integer> registeredEdittextSet = new HashSet();
	
//	private boolean mIsEditTextRegistered = false;

	/**
	 * Create a custom keyboard, that uses the KeyboardView (with resource id <var>viewid</var>) of the <var>host</var> activity,
	 * and load the keyboard layout from xml file <var>layoutid</var> (see {@link Keyboard} for description).
	 * Note that the <var>host</var> activity must have a <var>KeyboardView</var> in its layout (typically aligned with the bottom of the activity).
	 * Note that the keyboard layout xml file may include key codes for navigation; see the constants in this class for their values.
	 * Note that to enable EditText's to use this custom keyboard, call the {@link #registerEditText(int)}.
	 *
	 * @param host The hosting activity.
	 * @param viewid The id of the KeyboardView.
	 */
	public CustomKeyboard(Activity host, View viewHolder) {

		/*
        this.mHostActivity = host;
        this.mKeyboardView = (MainKeyboardView)mHostActivity.findViewById(viewid);
		 */
		this.mHostActivity = host;
		mParentViewHolder = viewHolder;
		//mParentViewHolder.setVisibility(View.GONE);

		mAdaptxtIME = new AdaptxtIME(mHostActivity);
		mAdaptxtIME.setCustomHandler(this);

		//mAdaptxtIME.onCreate();
		mAdaptxtIME.onStartInput(null, false);

		// Hide the standard keyboard initially
		mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
	}

	/**
	 * Register <var>EditText<var> with resource id <var>resid</var> (on the hosting activity) for using this custom keyboard.
	 *
	 * @param resid The resource id of the EditText that registers to the custom keyboard.
	 */
	public void registerEditText(int resid,int eType,AdaptxtEditTextEventListner listner,AdaptxtKeyboordVisibilityStatusListner listner2) {
		//mEditorType = eType;
		AdaptxtEditText	currentView = null;
		try{
			currentView= (AdaptxtEditText)mHostActivity.findViewById(resid);
		}catch(Exception e){
			Log.e("KPT", " Strange register edit text cant find edit text from resource id ");
			return;
		} 
		if(currentView != null){
			currentView.setEventListner(listner);
			currentView.setInputViewListner(listner2);
			currentView.setmEditorType(eType);
			if(AdaptxtIME.sTextSelectionMethod != null){
				currentView.setTextIsSelectable(true);
			}
			currentView.setRegisteredWithAdaptxt(1);
			currentView.setCutCopyPasteInterface(mAdaptxtIME.getCutCopyPasteInterface());
			//registeredEdittextSet.add(mEdittext.getId());
			currentView.setOnFocusChangeListener(new OnFocusChangeListener() {
				// NOTE By setting the on focus listener, we can show the custom keyboard when the edit box gets focus, but also hide it when the edit box loses focus
				@Override 
				public void onFocusChange(View v, boolean hasFocus) {
					processOnFocusChangeListner(v, hasFocus);
				}
			});

			currentView.setOnClickListener(new OnClickListener() {
				// NOTE By setting the on click listener, we can show the custom 
				//keyboard again, by tapping on an edit box that already had focus (but that had the keyboard hidden).
				@Override 
				public void onClick(View v) {
					processOnClickListner(v);
				}
			});

			currentView.setOnTouchListener(new OnTouchListener() {
				@Override 
				public boolean onTouch(View v, MotionEvent event) {
					return processOnTouchListener(v, event);
				}
			});
		}
		// Disable spell check (hex strings look like words to Android)
		//mEdittext.setInputType(mEdittext.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	}
	
	public void registerEditText(AdaptxtEditText editText,int etype,AdaptxtEditTextEventListner listner, AdaptxtKeyboordVisibilityStatusListner listner2){
	//	mEdittext = editText;
		editText.setmEditorType(etype);
		editText.setEventListner(listner);
		editText.setInputViewListner(listner2);
		if(AdaptxtIME.sTextSelectionMethod != null){
			editText.setTextIsSelectable(true);
		}
		
		//registeredEdittextSet.add(editText.getId());

		editText.setRegisteredWithAdaptxt(1);
		editText.setCutCopyPasteInterface(mAdaptxtIME.getCutCopyPasteInterface());
		editText.setOnFocusChangeListener(new OnFocusChangeListener() {
			// NOTE By setting the on focus listener, we can show the custom keyboard when the edit box gets focus, but also hide it when the edit box loses focus
			@Override 
			public void onFocusChange(View v, boolean hasFocus) {
				//Log.e("KPT", " setOnFocusChangeListener ----------->  ");
				processOnFocusChangeListner(v, hasFocus);
			}
		});

		editText.setOnClickListener(new OnClickListener() {
			 //NOTE By setting the on click listener, we can show the custom 
        	//keyboard again, by tapping on an edit box that already had focus (but that had the keyboard hidden).
			@Override 
			public void onClick(View v) {
				//Log.e("KPT", " setOnClickListener ----------->  ");
				processOnClickListner(v);
				
			}
		});

		editText.setOnTouchListener(new OnTouchListener() {
			@Override 
			public boolean onTouch(View v, MotionEvent event) {
				//Log.e("KPT", " setOnTouchListener ----------->  ");
				return processOnTouchListener(v, event);
			}
		});
	}

	public void init(final View v){
		final AdaptxtEditText currentEditText = (AdaptxtEditText) v;
		if (currentEditText.isRegisteredWithAdaptxt()) {
			((InputMethodManager)mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
			if(mParentKeybaordView == null){
				mParentKeybaordView = (LinearLayout) mAdaptxtIME.onCreateInputView(mParentViewHolder);
				mParentKeybaordView.setVisibility(View.GONE);
				mAdaptxtIME.onCreateCandidatesView();
				AdaptxtKeyboordVisibilityStatusListner listner =	((AdaptxtEditText) v).getInputViewListner();
				if(listner != null){
					listner.onInputViewCreated();
				}
			}
		}else{
			Log.e("KPT", " Editext is not registered ");
		}
	}

	/** Returns whether the CustomKeyboard is visible. */
	public boolean isCustomKeyboardVisible() {
		if(mParentKeybaordView !=null){
			return mParentKeybaordView.getVisibility() == View.VISIBLE;
		}else{
			return false;
		}
	}

	public void unregister(AdaptxtEditText et){
		if(et != null){
			et.setRegisteredWithAdaptxt(0);
			et.setOnFocusChangeListener(null);
			et.setOnClickListener(null);
			et.setOnLongClickListener(null);
			et.setEventListner(null);
			et.setInputViewListner(null);
		}
		//registeredEdittextSet.remove(et.getId());
		
	}

	public void unregister(int resid){
		AdaptxtEditText et = ((AdaptxtEditText) mHostActivity.findViewById(resid));
		if (null == et) {
			Log.e("KPT", "Starange we are not able to unregister view resid not found");
			return;
		}
		et.setRegisteredWithAdaptxt(0);
		et.setOnFocusChangeListener(null);
		et.setOnClickListener(null);
		et.setOnLongClickListener(null);
		//registeredEdittextSet.remove(et.getId());
	}
	
	
	private void processOnFocusChangeListner(final View v, final boolean hasFocus){
		 final AdaptxtEditText currentView = (AdaptxtEditText) v;
		if (currentView.isRegisteredWithAdaptxt()) {
			currentView.setCursorVisible(hasFocus);
			if(currentView.getEventListner() != null){
				currentView.getEventListner().onAdaptxtFocusChange(v,hasFocus);
			}

			//Log.e("KPT", " mEditText onFocusChange  view is null "+(v == null) + " hasfoucs "+hasFocus);
			if( hasFocus){
				//currentView.setRegisteredWithAdaptxt(1);
				((InputMethodManager)mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
				//showCustomKeyboard(v,true);
				if(mKeyboardView != null &&
						(mKeyboardView.getFocusedEditText() == null || mKeyboardView.getFocusedEditText().getId() != currentView.getId())){
				if(mKeyboardView!=null){
					mKeyboardView.setEditText(currentView);
				}
				
				/*if (AdaptxtIME.sTextSelectionMethod != null && !currentView.isTextSelectable()) {
					
					currentView.setTextIsSelectable(true,true);
				}*/
				
				mAdaptxtIME.onStartInput(currentView, false);
				mAdaptxtIME.onStartInputView(currentView, false);
				mAdaptxtIME.onStartCandidatesView(currentView, false);

				//This will return one-before cursor position. So calling here once 
				currentView.getSelectionStart(); 
				mAdaptxtIME.checkTextinEditor();
				mAdaptxtIME.updateCurPosition();
				}

			}else{
				//currentView.setRegisteredWithAdaptxt(0);
				//hideCustomKeyboard(currentView);
			}
		}else{
			Log.e("KPT", "----------------> Editext is not registered ");
			//swtichToDefaultKeyboard(mEdittext);
		}
		
	}
	
	
	private void processOnClickListner(final View v){
		  final AdaptxtEditText currentView = (AdaptxtEditText) v;
		if (currentView.isRegisteredWithAdaptxt()) {
			//Log.e("KPT", " mEditText onClick  view is null "+(v == null));
			((InputMethodManager)mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
			//showCustomKeyboard(v,true);
			
			if(currentView.getEventListner() != null){
				currentView.getEventListner().onAdaptxtclick(v);
			}else{
				Log.e("KPT", " processOnClickListner ----------->  ");
			}
			//mKeyboardView.setEditText(currentView);
			/*mAdaptxtIME.onStartInput(currentView, false);
			mAdaptxtIME.onStartInputView(currentView, false);
			mAdaptxtIME.onStartCandidatesView(currentView, false);*/

			//This will return one-before cursor position. So calling here once 
			//currentView.getSelectionStart(); ////
			
			if(mKeyboardView != null &&
					(mKeyboardView.getFocusedEditText() == null || mKeyboardView.getFocusedEditText().getId() != currentView.getId())){
				if(mKeyboardView!=null){
					mKeyboardView.setEditText(currentView);
				}
				
			
				mAdaptxtIME.onStartInput(currentView, false);
				mAdaptxtIME.onStartInputView(currentView, false);
				mAdaptxtIME.onStartCandidatesView(currentView, false);
			}
			
			//mAdaptxtIME.checkTextinEditor();
			//mAdaptxtIME.updateCurPosition();
			//mAdaptxtIME.setCursorPosition(mAdaptxtIME.getEditor().getSelectionStart());////
		}else{
			Log.e("KPT", " processOnClickListner -----------> Editext is not registered ");
			swtichToDefaultKeyboard(mEdittext);
		}
	}
	
	private boolean processOnTouchListener(final View v, final MotionEvent event){
		  final AdaptxtEditText currentView = (AdaptxtEditText) v;
		if (currentView.isRegisteredWithAdaptxt()) {
			
			//Log.e("KPT", " mEditText onTouch  view is null "+(v == null));
			((InputMethodManager)mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
			//EditText edittext = (EditText) v;
			//AdaptxtEditText currentView = (AdaptxtEditText) v;
			if(currentView.getEventListner() != null){
				currentView.getEventListner().onAdaptxtTouch(v,event);
			}
			int inType = currentView.getInputType();       // Backup the input type
			//edittext.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
			try{
				boolean parentTouch =	currentView.onTouchEvent(event);  ////             // Call native handler
				currentView.setInputType(inType);              // Restore input type
				//mClientInterface.onAdaptxtTouch();
				return parentTouch; // Consume touch event
			}catch(Exception e){
				return false;
			}
			
		}else{
			Log.e("KPT", " Editext is not registered ");
			return false;
		}
		
	}
	
	public AdaptxtEditText getFocusEditor(){
		return mEdittext;
	}
	public void syncText(){
		if (null != mAdaptxtIME) {
			mAdaptxtIME.checkTextinEditor();
		}
	}
	
	/** Make the CustomKeyboard visible, and hide the system keyboard for view v. 
	 * @throws InterruptedException */
	public void showCustomKeyboard( View v,boolean visiblity ){
	//	Log.e("ES", " showCustomKeyboard  called with "+visiblity);
		hideCustomKeyboard(null);
		mKeyboardView = mAdaptxtIME.getKeyboardView();
		if(visiblity){
			mEdittext = (AdaptxtEditText)v;
			if(	mAdaptxtIME.getEditor() == null || mAdaptxtIME.getEditor().getId() != mEdittext.getId()){
				if(mKeyboardView!=null){
					mKeyboardView.setEditText(mEdittext);
				}
				
			
				mAdaptxtIME.onStartInput(mEdittext, false);
				mAdaptxtIME.onStartInputView(mEdittext, false);
				mAdaptxtIME.onStartCandidatesView(mEdittext, false);
			}
			mParentKeybaordView.setVisibility(View.VISIBLE);
			if(mAdaptxtIME != null && mAdaptxtIME.mInputView != null && mAdaptxtIME.mInputView.getVisibility() != View.VISIBLE){
				mAdaptxtIME.mInputView.setVisibility(View.VISIBLE);
			}
			AdaptxtKeyboordVisibilityStatusListner listner =	((AdaptxtEditText) v).getInputViewListner();
			if(listner != null){
				//Log.e("KPT", " showCustomKeyboard " + mAdaptxtIME.getReaLHeight());
				if(mHandler.hasMessages(MSG_GET_VIEW_HEIGHT)){
					mHandler.removeMessages(MSG_GET_VIEW_HEIGHT);
				}
				mHandler.sendEmptyMessageDelayed(MSG_GET_VIEW_HEIGHT, 300);
				//listner.onInputviewVisbility(true, mAdaptxtIME.getReaLHeight());
			}
			mKeyboardView.setEnabled(true);	
			mAdaptxtIME.checkTextinEditor();
		}else{
			hideCustomKeyboard((AdaptxtEditText)v);
			closeAnyDialogIfShowing();
		}

	}


	/** Make the CustomKeyboard invisible. */
	public void hideCustomKeyboard(AdaptxtEditText v) {
	///	Log.e("ES","hideCustomKeyboard ");
		if(isCustomKeyboardVisible() && mKeyboardView !=null){
			if(mParentKeybaordView != null && mParentKeybaordView.getVisibility() == View.VISIBLE){
				mParentKeybaordView.setVisibility(View.GONE);
				if(mAdaptxtIME != null && mAdaptxtIME.mInputView != null && mAdaptxtIME.mInputView.getVisibility() == View.VISIBLE){
					mAdaptxtIME.mInputView.setVisibility(View.GONE);
				}
				if(v!= null){
					AdaptxtKeyboordVisibilityStatusListner listner =	 v.getInputViewListner();
					if(listner != null){
						listner.onInputviewVisbility(false, 0);
					}
				}
				mAdaptxtIME.onFinishInputView(true);

				mKeyboardView.setEnabled(false);
			}
		}
	}

	/*public void onDisplay(View currentview, LinearLayout currentKeyboard) {
		if (mEdittext.isRegisteredWithAdaptxt()) {
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
					RelativeLayout.LayoutParams.MATCH_PARENT);
			params.addRule(RelativeLayout.ABOVE,  currentKeyboard.getId());
		}
	}*/

	public void reloadKeybaord(){
		hideCustomKeyboard(null);
	}
	
	
	public void closeAnyDialogIfShowing(){
		mAdaptxtIME.closeAnyDialogIfShowing();
		hideCustomKeyboard(null);
	}

	public void destroyCustomKeyboard(){
		if (mAdaptxtIME!= null) {
			mAdaptxtIME.onFinishInput();
		
			/*if( registeredEdittextSet.size() >0){
				mAdaptxtIME.onDestroy();
			}*/
		}
		
	}

	/**
	 * return the height of the Keyboard + Suggestion view
	 * @return
	 */
	public int getKeyBoardAndCVHeight(){
		int height = 0;
		if (mAdaptxtIME != null ) {
			height = mAdaptxtIME.getApproxHeight();
	}
		return height;
	}

	public void swtichToDefaultKeyboard(final AdaptxtEditText editText){
		if (mParentViewHolder != null) {
			hideCustomKeyboard(editText);
			unregister(editText);
			InputMethodManager imm = (InputMethodManager) mHostActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(editText, InputMethodManager.RESULT_UNCHANGED_SHOWN);
			closeAnyDialogIfShowing();
			destroyCustomKeyboard();
		}
	}
	
	//Bug Fix: 19984
	public Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_GET_VIEW_HEIGHT:
					if(mEdittext != null){
						Log.e("KPT", " showCustomKeyboard " + mAdaptxtIME.getReaLHeight());
						//mEdittext.getInputViewListner().onInputviewVisbility(true, mAdaptxtIME.getReaLHeight());
						
						AdaptxtKeyboordVisibilityStatusListner listner = mEdittext.getInputViewListner();
						if(listner != null){
							listner.onInputviewVisbility(true, mAdaptxtIME.getReaLHeight());
						}
						
						if(mAdaptxtIME != null){//update the action bar as we are not getting the proper height prior to this call
							mAdaptxtIME.updateActionBarHeight();
						}
					}
					break;
			  default :
				  if(mEdittext != null)
					  showCustomKeyboard(mEdittext, true);
					  mAdaptxtIME.checkTextinEditor();
			}
			
		}
	};


	public void swtichToKPTKeyboard(final AdaptxtEditText editText,int etype,AdaptxtEditTextEventListner listner, AdaptxtKeyboordVisibilityStatusListner listner2){
		if (mParentViewHolder != null) {
			((InputMethodManager)mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
			registerEditText(editText, etype, listner, listner2);
			
			mHandler.sendEmptyMessageDelayed(0, 200);
		}
	}


	public void   onConfigurationChanged(Configuration newConfig){
		mAdaptxtIME.onConfigurationChanged(newConfig, mEdittext);
		/*if(newConfig.keyboardHidden == Configuration.KEYBOARDHIDDEN_NO){
			hideCustomKeyboard(mEdittext);
		}*/
	}

	public void updateCore(){
		if(mAdaptxtIME != null){
			mAdaptxtIME.clearCore();
		}
	}
	
	public void onResume(){
	
		reloadKeybaord();
		mAdaptxtIME.onStartInput(mEdittext, true);
		mAdaptxtIME.onStartInputView(mEdittext, true);
	}
	
	public GlobeKeyData getGlobeKeydata(){
		return mAdaptxtIME.getGlobeKeyData();
	}

	public boolean showGlobeKeyView(final AlertDialog.Builder builder){
		return mAdaptxtIME.showGlobeKeyView(builder);
	}
	
	public void processChangeLanguageForDialog(final int index){
		mAdaptxtIME.processChangeLanguageForDialog(index);
	}
	
	
	public void onPause(){
		mAdaptxtIME.onPause();
		hideCustomKeyboard(mEdittext);
	}	
}
