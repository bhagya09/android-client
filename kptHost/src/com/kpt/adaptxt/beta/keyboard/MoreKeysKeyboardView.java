/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kpt.adaptxt.beta.keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import com.kpt.adaptxt.beta.Keyboard;
import com.kpt.adaptxt.beta.KeyboardView;
import com.kpt.adaptxt.beta.MainKeyboardView.KeyTimerHandler;
import com.kpt.adaptxt.beta.R;
import com.kpt.adaptxt.beta.keyboard.PointerTracker.DrawingProxy;
import com.kpt.adaptxt.beta.keyboard.PointerTracker.KeyTimerController;

/**
 * A view that renders a virtual {@link MoreKeysKeyboard}. It handles rendering of keys and
 * detecting key presses and touch movements.
 */
public final class MoreKeysKeyboardView extends KeyboardView implements MoreKeysPanel {

	private final int[] mCoordinates = new int[2];

	private final KeyDetector mKeyDetector;

	private Controller mController;
	private KeyboardActionListener mListener;

	private int mOriginX;
	private int mOriginY;

	/**
	 * for normal keyboard origin is at the left most corner of the screen below the editor.
	 * 
	 * but for mini keyboard(which is also a keyboard view), the origin will be based on the
	 * longpressed key. so shift the origin to the top-left point of the view
	 */
	private boolean mIgnoreOrigin;

	/**
	 * is the mini keyboard being dismissed
	 */
	private boolean mIsDismissing;

	private SuddenJumpingTouchEventHandler mTouchScreenRegulator;

	
	private KeyTimerController mKeyTimerHandler = new KeyTimerController.Adapter();

	private final KeyboardActionListener mMoreKeysKeyboardListener = new KeyboardActionListener() {

		@Override
		public void swipeUp() {}
		@Override
		public void swipeRight() {}
		@Override
		public void swipeLeft() {}
		@Override
		public void swipeDown() {}
		@Override
		public void onHardKey(int primaryCode, int[] keyCodes) {}
		@Override
		public void onGlideCompleted(float[] xCoordinates, float[] yCoordinates) {}
		@Override
		public void launchSettings(Class settingsClass) {}

		@Override
		public void onText(CharSequence text) {
			mListener.onText(text);
		}

		@Override
		public void onRelease(int primaryCode) {
			mListener.onRelease(primaryCode);
		}

		@Override
		public void onPress(int primaryCode) {
			mListener.onPress(primaryCode);
		}

		@Override
		public void onKey(int primaryCode, int[] keyCodes, boolean isPopupChar) {
			mListener.onKey(primaryCode, keyCodes, true);
		}
	};

	public void setKeyTimerHandler(KeyTimerHandler keyTimerHandler){
		mKeyTimerHandler = keyTimerHandler;
	}

	public MoreKeysKeyboardView(Context context, AttributeSet attrs) {
		this(context, attrs, R.styleable.Kpt_KeyboardView_kpt_keyboardViewStyle);
	}

	public MoreKeysKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		final Resources res = context.getResources();
		mKeyDetector = new MoreKeysDetector(res.getDimension(R.dimen.kpt_more_keys_keyboard_slide_allowance));
		setKeyPreviewPopupEnabled(false, 0);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final Keyboard keyboard = getKeyboard();
		if (keyboard != null) {
			final int width = keyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
			final int height = keyboard.getHeight() + getPaddingTop() + getPaddingBottom();
			setMeasuredDimension(width, height);
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	@Override
	public void setKeyboard(Keyboard keyboard) {
		setPopupKeyboardStatus(true);
		super.setKeyboard(keyboard);
		int verticalCorrection = 0/*keyboard.getSingleKeyHeight()*/; 
		mKeyDetector.setKeyboard(keyboard, -getPaddingLeft(), -getPaddingTop()  + verticalCorrection);
	}

	@Override
	public KeyDetector getKeyDetector() {
		return mKeyDetector;
	}

	@Override
	public KeyboardActionListener getKeyboardActionListener() {
		return mMoreKeysKeyboardListener;
	}

	@Override
	public DrawingProxy getDrawingProxy() {
		return this;
	}

	@Override
	public KeyTimerController getTimerProxy() {
		return mKeyTimerHandler;
	}

	@Override
	public void setKeyPreviewPopupEnabled(boolean previewEnabled, int delay) {
		// More keys keyboard needs no pop-up key preview displayed, so we pass always false with a
		// delay of 0. The delay does not matter actually since the popup is not shown anyway.
		super.setKeyPreviewPopupEnabled(false, 0);
	}

	@Override
	public void showMoreKeysPanel(View parentView, Controller controller, int pointX, int pointY,
			PopupWindow window, KeyboardActionListener listener, boolean ignoreOrg, int mOriginX, int mOriginY) {

		mController = controller;
		mListener = listener;
		final View container = (View)getParent();

		window.setContentView(container);
		window.setWidth(container.getMeasuredWidth());
		window.setHeight(container.getMeasuredHeight());
		parentView.getLocationInWindow(mCoordinates);
		window.showAtLocation(parentView, Gravity.NO_GRAVITY, pointX, pointY);

		mIgnoreOrigin = !ignoreOrg;

		this.mOriginX = mOriginX;
		this.mOriginY = mOriginY;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (getKeyboard() == null  ) {
			return false;
		}
		if (mTouchScreenRegulator == null  ) {
			return false;
		}
		return mTouchScreenRegulator.onTouchEvent(event, false);
	}


	@Override
	public void onDetachedFromWindow() {
		// PointerTracker.resetMinikeyboardForAllPointers();  this method should be called only when KeyboardView is detached.
		// but when minikeyboard is getting detached even its parent ie..keyboardView's onDetachedFromWindow() is called
		// hence stopping super.onDetachedFromWindow()
		//super.onDetachedFromWindow();
	}

	@Override
	public boolean dismissMoreKeysPanel() {
		if (mIsDismissing || mController == null) return false;
		PointerTracker.mPhantonMiniKeyboardOnScreen = false;
		mIsDismissing = true;
		final boolean dismissed = mController.dismissMoreKeysPanel();
		mIsDismissing = false;
		return dismissed;
	}

	@Override
	public void resetGlideInputType() {
		if (mController != null) {
			mController.resetGlideInputType();
		}
	}

	@Override
	public float translateX(float x, boolean touchStartedFromMiniKeyboard) {
		if(touchStartedFromMiniKeyboard) {
			mIgnoreOrigin = true;
		}

		if(mIgnoreOrigin) {
			return x;
		} else {
			return x - mOriginX;
		}
	}

	@Override
	public float translateY(float y, boolean touchStartedFromMiniKeyboard) {
		return y - mOriginY;
	}

	@Override
	public boolean getKeyboardType() {
		return false;
	}

	public void setTouchScreenRegulator(SuddenJumpingTouchEventHandler touchScreenRegulator) {
		mTouchScreenRegulator = touchScreenRegulator;
	}

	public void setStartX(int mMiniKeyboardStartX) {
		mKeyDetector.setX(mMiniKeyboardStartX);
	}
}
