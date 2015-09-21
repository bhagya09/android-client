package com.kpt.adaptxt.beta.view;

import com.kpt.adaptxt.beta.ImeCutCopyPasteInterface;

import android.app.AlertDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class AdaptxtEditText extends EditText{

	private int isRegisteredWithAdaptxt = 0;
	private AdaptxtEditTextEventListner mClientInterface;
	public int mEditorType;
	private AdaptxtKeyboordVisibilityStatusListner mVisiblityInterface;
	
	private int mSelectionStart = -1;
	private int mSelectionEnd = -1;
	
	private int mSelectionStartNew = -1;
	private int mSelectionEndNew = -1;
	
	private boolean mUserSelectedText = false;
	private ImeCutCopyPasteInterface mCutCopyPasteInterface;
	private boolean mSetByAdaptxtIME;
	private boolean mCursorSetByAdaptxt;
	public static final String XML_NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android";
	private int mMaxLength;

	public AdaptxtEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		 mMaxLength = attrs.getAttributeIntValue(XML_NAMESPACE_ANDROID, "maxLength", -1);
	}



	public AdaptxtEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		 mMaxLength = attrs.getAttributeIntValue(XML_NAMESPACE_ANDROID, "maxLength", -1);
	}



	public int getmMaxLength() {
		return mMaxLength;
	}



	public void setmMaxLength(int mMaxLength) {
		this.mMaxLength = mMaxLength;
	}



	public AdaptxtEditText(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	


     @Override
    protected void onTextChanged(CharSequence text, int start,
    		int lengthBefore, int lengthAfter) {
    	super.onTextChanged(text, start, lengthBefore, lengthAfter);
    	if(mCutCopyPasteInterface!=null && !mSetByAdaptxtIME){
    		mCutCopyPasteInterface.processImeTextChange();
    	}
    	mSetByAdaptxtIME = false;
    	
    }
     
     
	/**
	 * This method is called before keyboard appears when text is selected.
	 * So just hide the keyboard
	 * @return
	 */
	@Override
	public boolean onCheckIsTextEditor() {
		//Log.e("NIRAJ", "onCheckIsTextEditor"+  isRegisteredWithAdaptxt);
		hideKeyboard();

		return super.onCheckIsTextEditor();
	}
	
	 public void setSelection(int index, boolean fromAdaptxt) {
		 mCursorSetByAdaptxt = fromAdaptxt;
		 setSelection(index);
		 
	 };
	
	

	/**
	 * This methdod is called when text selection is changed, so hide keyboard to prevent it to appear
	 * @param selStart
	 * @param selEnd
	 */
	@Override
	protected void onSelectionChanged(int selStart, int selEnd) {
		//Log.e("KPT", "onSelectionChanged"+  selStart + " Selend + "+selEnd);
		super.onSelectionChanged(selStart, selEnd);
		
		if (!mUserSelectedText) {
			mSelectionStart = selStart;
			mSelectionEnd = selEnd;
		}else{
			mSelectionStartNew = selStart;
			mSelectionEndNew = selEnd;
		}
		
		if(selStart != selEnd){
			mUserSelectedText = true;
		}
		if (null != mCutCopyPasteInterface && !mCursorSetByAdaptxt) {
			mCutCopyPasteInterface.onSelectionChanged(selStart, selEnd);
		}
		mCursorSetByAdaptxt = false;
		

		hideKeyboard();

	}
	
	public int getSelectionStartPosition() {
		return mSelectionStart;
	}
	
	public int getSelectionEndPosition() {
		return mSelectionEnd;
	}
	
	
	@Override
	public boolean hasSelection() {
		// TODO Auto-generated method stub;
		boolean status = super.hasSelection();
		
		mUserSelectedText = status;
		
		//Log.e("KPT", " has Selection "+mUserSelectedText);
		return status;
	}
	
	public boolean getUserSelectedPSomeText(){
		return mUserSelectedText;
	}
	
	public void setUserSelectedPSomeText(boolean isSelectedSomeText){
		mUserSelectedText = isSelectedSomeText;
	}
 
	
	@Override
	public boolean onTextContextMenuItem(int id) {
		// Do your thing:
	    boolean consumed = super.onTextContextMenuItem(id);
	    // React:
	    if(consumed){
	    	switch (id){
	    	case android.R.id.selectAll:
	    		processSelectAll();
	    		break;
	    	case android.R.id.cut:
	    		processFrameworkCut();
	    		//onTextCut();
	    		break;
	    	case android.R.id.paste:
	    		processFrameworkPaste();
	    		break;
	    	case android.R.id.copy:
	    		processFrameworkCopy();
	    	}
	    }
	    return consumed;
	}
	
	
	private void processSelectAll() {
		if (null != mCutCopyPasteInterface) {
			mCutCopyPasteInterface.processImeSelectAll();
			//perform copy/cut after performingselect all, the flag is restting. So, commenting
//			mUserSelectedText = false;
		}else{
			Log.e("KPT", "Strange mCutCopyPasteInterface is null at cut ");
		}
	}



	public void processFrameworkCut(){

		if (mUserSelectedText) {
			//Log.e("KPT", "processFrameworkCut"+  mSelectionStart + " Selend + "+mSelectionEnd);
			if (null != mCutCopyPasteInterface) {
				mCutCopyPasteInterface.processImeCut();
				mUserSelectedText = false;
			}else{
				Log.e("KPT", "Strange mCutCopyPasteInterface is null at cut ");
			}
		}
	}
	
	
	public void processFrameworkPaste(){

		if (null != mCutCopyPasteInterface) {
			mCutCopyPasteInterface.processImePaste();
			mUserSelectedText = false;
		}else{
			Log.e("KPT", "Strange mCutCopyPasteInterface is null at paste ");
		}
	}
	
	
	public void processFrameworkCopy(){
		if (mUserSelectedText) {
			//Log.e("KPT", "processFrameworkCopy"+  mSelectionStart + " Selend + "+mSelectionStart);
			
			if (null != mCutCopyPasteInterface) {
				mCutCopyPasteInterface.processImeCopy();
				mUserSelectedText = false;
			}else{
				Log.e("KPT", "Strange mCutCopyPasteInterface is null at copy ");
			}
		}
	}
	
	private void hideKeyboard(){
		//Log.e("NIRAJ", "hideKeyboard"+  isRegisteredWithAdaptxt);
		if(isRegisteredWithAdaptxt()){
			InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getWindowToken(), 0);
		}
	}
	
	public boolean isRegisteredWithAdaptxt() {
		return isRegisteredWithAdaptxt == 1;
	}

	public void setCutCopyPasteInterface(final ImeCutCopyPasteInterface cutCopyPasteInterface){
		 mCutCopyPasteInterface = cutCopyPasteInterface;
	}
	

	public void setRegisteredWithAdaptxt(int isRegisteredWithAdaptxt) {
		//Log.e("NIRAJ", getId()+"setRegisteredWithAdaptxt "+  isRegisteredWithAdaptxt);
		this.isRegisteredWithAdaptxt = isRegisteredWithAdaptxt;
	}
	
	public int getmEditorType() {
		return mEditorType;
	}



	public void setmEditorType(int mEditorType) {
		this.mEditorType = mEditorType;
	}
	
	
	public void setEventListner(final AdaptxtEditTextEventListner clientInterface) {
		mClientInterface = clientInterface;
	}
	
	public AdaptxtEditTextEventListner getEventListner() {
		return mClientInterface ;
	}
	
	
	public void setInputViewListner(final AdaptxtKeyboordVisibilityStatusListner clientInterface) {
		mVisiblityInterface = clientInterface;
	}
	
	public AdaptxtKeyboordVisibilityStatusListner getInputViewListner() {
		return mVisiblityInterface ;
	}
	
	
	public interface AdaptxtEditTextEventListner{
		public void onAdaptxtTouch(View v, MotionEvent event);
		
		public void onAdaptxtFocusChange(View v, boolean hasFocus);
		
	
		
		public void onAdaptxtclick(View v);
		
		
		public void onReturnAction(int type);
	}
	
	public interface AdaptxtKeyboordVisibilityStatusListner{
		public void onInputViewCreated();
		public void onInputviewVisbility(boolean visible, int height);
		public void showGlobeKeyView();
		public void showQuickSettingView();
		public void analyticalData(final String languageName);
	}
	
	public void setText(CharSequence text,boolean changed){
		mSetByAdaptxtIME = changed;
		mCursorSetByAdaptxt = changed;
		setText(text);
	}
	



	public void setSelection(int cursorPosition, int cursorPosition2, boolean b) {
		// TODO Auto-generated method stub
		mCursorSetByAdaptxt = b;
		setSelection(cursorPosition, cursorPosition2);
		
	}
	
	
	
	

}