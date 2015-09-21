/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.kpt.adaptxt.beta;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.widget.RelativeLayout;

import com.kpt.adaptxt.beta.GesturePreviewTrail.Params;
import com.kpt.adaptxt.beta.keyboard.CollectionUtils;
import com.kpt.adaptxt.beta.keyboard.PointerTracker;
import com.kpt.adaptxt.beta.keyboard.StaticInnerHandlerWrapper;
import com.kpt.adaptxt.beta.util.KPTConstants;
import com.kpt.adaptxt.beta.view.KPTAdaptxtTheme;

public final class PreviewPlacerView extends RelativeLayout {
	// The height of extra area above the keyboard to draw gesture trails.
	// Proportional to the keyboard height.
	private static final float EXTRA_GESTURE_TRAIL_AREA_ABOVE_KEYBOARD_RATIO = 0f;

	private int mKeyboardViewOriginX;
	private int mKeyboardViewOriginY;

	private final SparseArray<GesturePreviewTrail> mGesturePreviewTrails =
			CollectionUtils.newSparseArray();
	private final Params mGesturePreviewTrailParams;
	private final Paint mGesturePaint;
	private boolean mDrawsGesturePreviewTrail;
	private int mOffscreenWidth;
	private int mOffscreenHeight;
	private int mOffscreenOffsetY;
	private Bitmap mOffscreenBuffer;
	private final Canvas mOffscreenCanvas = new Canvas();
	private final Rect mOffscreenDirtyRect = new Rect();
	private final Rect mGesturePreviewTrailBoundsRect = new Rect(); // per trail

	private static final char[] TEXT_HEIGHT_REFERENCE_CHAR = { 'M' };
	private boolean mDrawsGestureFloatingPreviewText;

	private final DrawingHandler mDrawingHandler;
	
	private final int mScreenHeight, mScreenWidth;
	private final boolean mIsSDKBelowHC; 
	
	private static final class DrawingHandler extends StaticInnerHandlerWrapper<PreviewPlacerView> {
		private static final int MSG_DISMISS_GESTURE_FLOATING_PREVIEW_TEXT = 0;
		private static final int MSG_UPDATE_GESTURE_PREVIEW_TRAIL = 1;

		private final Params mGesturePreviewTrailParams;
		private final int mGestureFloatingPreviewTextLingerTimeout;

		public DrawingHandler(final PreviewPlacerView outerInstance,
				final Params gesturePreviewTrailParams,
				final int getstureFloatinPreviewTextLinerTimeout) {
			super(outerInstance);
			mGesturePreviewTrailParams = gesturePreviewTrailParams;
			mGestureFloatingPreviewTextLingerTimeout = getstureFloatinPreviewTextLinerTimeout;
		}

		@Override
		public void handleMessage(final Message msg) {
			final PreviewPlacerView placerView = getOuterInstance();
			if (placerView == null) return;
			switch (msg.what) {
			case MSG_DISMISS_GESTURE_FLOATING_PREVIEW_TEXT:
				placerView.setGestureFloatingPreviewText(null);
				break;
			case MSG_UPDATE_GESTURE_PREVIEW_TRAIL:
				//mGesturePreviewTrailParams.mTrailLingerDuration = 0;
				placerView.invalidate();
			//	mGesturePreviewTrailParams.mTrailLingerDuration = 300;
				break;
			}
		}

		public void dismissGestureFloatingPreviewText() {
			removeMessages(MSG_DISMISS_GESTURE_FLOATING_PREVIEW_TEXT);
			sendMessageDelayed(obtainMessage(MSG_DISMISS_GESTURE_FLOATING_PREVIEW_TEXT),
					mGestureFloatingPreviewTextLingerTimeout);
		}

		public void postUpdateGestureTrailPreview() {
			removeMessages(MSG_UPDATE_GESTURE_PREVIEW_TRAIL);
			sendMessageDelayed(obtainMessage(MSG_UPDATE_GESTURE_PREVIEW_TRAIL),
					mGesturePreviewTrailParams.mUpdateInterval);
		}
	}

	public PreviewPlacerView(final Context context, final AttributeSet attrs) {
		this(context, attrs, R.attr.kpt_keyboardViewStyle);
	}

	public PreviewPlacerView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context);
		setWillNotDraw(false);

		final Resources res = context.getResources();
		final int gestureFloatingPreviewTextSize = res.getDimensionPixelSize(R.dimen.kpt_gesture_floating_preview_text_size);

		final int gestureFloatingPreviewTextLingerTimeout = res.getInteger(R.integer.kpt_config_key_preview_linger_timeout);

		mScreenHeight = res.getDisplayMetrics().heightPixels;
		mScreenWidth = res.getDisplayMetrics().widthPixels;
		 
		mIsSDKBelowHC = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? true : false;
		
		mGesturePreviewTrailParams = new Params(res);
		mDrawingHandler = new DrawingHandler(this, mGesturePreviewTrailParams,
				gestureFloatingPreviewTextLingerTimeout);

		final Paint gesturePaint = new Paint();
		gesturePaint.setAntiAlias(true);
		if(!mIsSDKBelowHC) {
			gesturePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
		}
		mGesturePaint = gesturePaint;

		final Paint textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Align.CENTER);
		textPaint.setTextSize(gestureFloatingPreviewTextSize);
		final Rect textRect = new Rect();
		textPaint.getTextBounds(TEXT_HEIGHT_REFERENCE_CHAR, 0, 1, textRect);

		if(!mIsSDKBelowHC) {
			final Paint layerPaint = new Paint();
			layerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
			setLayerType(LAYER_TYPE_HARDWARE, layerPaint);
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		if(mIsSDKBelowHC) {
			setMeasuredDimension(mScreenWidth, mScreenHeight);
		}
	}
	
	public void setKeyboardViewGeometry(final int x, final int y, final int w, final int h) {
		mKeyboardViewOriginX = x;
		mKeyboardViewOriginY = y;
		mOffscreenOffsetY = (int)(h * EXTRA_GESTURE_TRAIL_AREA_ABOVE_KEYBOARD_RATIO);
		mOffscreenWidth = w;
		mOffscreenHeight = mOffscreenOffsetY + h;
	}

	public void setGesturePreviewMode(final boolean drawsGesturePreviewTrail,
			final boolean drawsGestureFloatingPreviewText) {
		mDrawsGesturePreviewTrail = drawsGesturePreviewTrail;
		mDrawsGestureFloatingPreviewText = drawsGestureFloatingPreviewText;
	}

	public void invalidatePointer(final PointerTracker tracker, final boolean isOldestTracker) {
		final boolean needsToUpdateLastPointer =
				isOldestTracker && mDrawsGestureFloatingPreviewText;
		if (mDrawsGesturePreviewTrail) {
			GesturePreviewTrail trail;
			synchronized (mGesturePreviewTrails) {
				trail = mGesturePreviewTrails.get(tracker.mPointerId);
				if (trail == null) {
					trail = new GesturePreviewTrail();
					mGesturePreviewTrails.put(tracker.mPointerId, trail);
				}
			}
			trail.addStroke(tracker.getGestureStrokeWithPreviewPoints(), tracker.getDownTime());
		}

		// TODO: Should narrow the invalidate region.
		if (mDrawsGesturePreviewTrail || needsToUpdateLastPointer) {
			invalidate();
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		freeOffscreenBuffer();
	}

	private void freeOffscreenBuffer() {
		if (mOffscreenBuffer != null) {
			mOffscreenBuffer.recycle();
			mOffscreenBuffer = null;
		}
	}

	private void mayAllocateOffscreenBuffer() {
		if (mOffscreenBuffer != null && mOffscreenBuffer.getWidth() == mOffscreenWidth
				&& mOffscreenBuffer.getHeight() == mOffscreenHeight && !mIsSDKBelowHC) {
			return;
		}
		freeOffscreenBuffer();
		mOffscreenBuffer = Bitmap.createBitmap(
				mOffscreenWidth, mOffscreenHeight, Bitmap.Config.ARGB_8888);
		mOffscreenCanvas.setBitmap(mOffscreenBuffer);
	}

	@Override
	public void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		if (mDrawsGesturePreviewTrail) {
			mayAllocateOffscreenBuffer();
			// Draw gesture trails to offscreen buffer.
			final boolean needsUpdatingGesturePreviewTrail = drawGestureTrails(
					mOffscreenCanvas, mGesturePaint, mOffscreenDirtyRect);
			// Transfer offscreen buffer to screen.
			if (!mOffscreenDirtyRect.isEmpty()) {
				final int offsetY = mKeyboardViewOriginY - mOffscreenOffsetY;
				canvas.translate(mKeyboardViewOriginX, offsetY);
				canvas.drawBitmap(mOffscreenBuffer, mOffscreenDirtyRect, mOffscreenDirtyRect,
						mGesturePaint);
				canvas.translate(-mKeyboardViewOriginX, -offsetY);
				// Note: Defer clearing the dirty rectangle here because we will get cleared
				// rectangle on the canvas.
			}
			if (needsUpdatingGesturePreviewTrail) {
				mDrawingHandler.postUpdateGestureTrailPreview();
			}
		}
		/*if (mDrawsGestureFloatingPreviewText) {
            canvas.translate(mKeyboardViewOriginX, mKeyboardViewOriginY);
            drawGestureFloatingPreviewText(canvas, mGestureFloatingPreviewText);
            canvas.translate(-mKeyboardViewOriginX, -mKeyboardViewOriginY);
        }*/
	}

	private boolean drawGestureTrails(final Canvas offscreenCanvas, final Paint paint,
			final Rect dirtyRect) {
		// Clear previous dirty rectangle.
		if (!dirtyRect.isEmpty()) {
			paint.setColor(Color.TRANSPARENT);
			paint.setStyle(Paint.Style.FILL);
			offscreenCanvas.drawRect(dirtyRect, paint);
		}
		dirtyRect.setEmpty();

		// Draw gesture trails to offscreen buffer.
		offscreenCanvas.translate(0, mOffscreenOffsetY);
		boolean needsUpdatingGesturePreviewTrail = false;
		synchronized (mGesturePreviewTrails) {
			// Trails count == fingers count that have ever been active.
			final int trailsCount = mGesturePreviewTrails.size();
			for (int index = 0; index < trailsCount; index++) {
				final GesturePreviewTrail trail = mGesturePreviewTrails.valueAt(index);
				needsUpdatingGesturePreviewTrail |=
						trail.drawGestureTrail(offscreenCanvas, paint,
								mGesturePreviewTrailBoundsRect, mGesturePreviewTrailParams);
				// {@link #mGesturePreviewTrailBoundsRect} has bounding box of the trail.
				dirtyRect.union(mGesturePreviewTrailBoundsRect);
			}
		}
		offscreenCanvas.translate(0, -mOffscreenOffsetY);

		// Clip dirty rectangle with offscreen buffer width/height.
		dirtyRect.offset(0, mOffscreenOffsetY);
		clipRect(dirtyRect, 0, 0, mOffscreenWidth, mOffscreenHeight);
		return needsUpdatingGesturePreviewTrail;
	}

	private static void clipRect(final Rect out, final int left, final int top, final int right,
			final int bottom) {
		out.set(Math.max(out.left, left), Math.max(out.top, top), Math.min(out.right, right),
				Math.min(out.bottom, bottom));
	}

	public void setGestureFloatingPreviewText(final String gestureFloatingPreviewText) {
		if (!mDrawsGestureFloatingPreviewText) return;
		invalidate();
	}

	public void dismissGestureFloatingPreviewText() {
		mDrawingHandler.dismissGestureFloatingPreviewText();
	}
	
	/**
	 * update the trace path color based on the current theme
	 * @param theme
	 */
	public void updateTracePathColor(final KPTAdaptxtTheme theme) {
		
		mGesturePreviewTrailParams.mTrailColor = getResources().getColor(R.color.kpt_secondary_key_color_dk_fk);
		//theme.getResources().getColor(theme.mSecondaryTextColor);
		
		/*SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(getContext());
		int glideTraceColor = spref.getInt(KPTConstants.PREF_CUST_GLIDE_TRACE_PATH, -1);
		if(glideTraceColor != -1) {
			mGesturePreviewTrailParams.mTrailColor = glideTraceColor;
		} else {
			//user has not yet changed glide trace color in customization, so load secondary color
			try {
				if(theme.getCurrentThemeType() == KPTConstants.THEME_CUSTOM) { 
					mGesturePreviewTrailParams.mTrailColor = theme.mCustomThemeColors[KPTAdaptxtTheme.COLOR_SECONDARY_CHAR];
				} else {
					mGesturePreviewTrailParams.mTrailColor = theme.getResources().getColor(theme.mSecondaryTextColor);	
				}
			} catch(Exception e) {
				e.printStackTrace();
				//check for resource not found exception
				mGesturePreviewTrailParams.mTrailColor = Color.parseColor("#2aa7e8");
			}
		}*/
	}
}
