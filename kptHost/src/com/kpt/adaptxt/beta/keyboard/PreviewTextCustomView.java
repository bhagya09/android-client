package com.kpt.adaptxt.beta.keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.widget.TextView;

/**
 * Class to display a customized text view as key preview. <br>
 * 
 * <p>As per LatimIme design, instead of popup windows text views are created and are added to
 * a relative layout and are moved across the keys by using margins.<p>
 * 
 * <p>Now the problem is we are unable to set margins for any other layout except TextView
 * (currently unknown why is this), so using a custom TextView and implemented 
 * all the features. This same text view is used for both normal and custom themes<p>  
 * 
 * 
 * TO DRAW TEXT PLEASE DONT USE setText() USE drawText(), since setText() is a private method.
 * 
 *
 */
public class PreviewTextCustomView extends TextView {

	private Paint   mPaint;

	private int mWidth = 0;
	private int mHeight = 0;

	private final Resources mRes;

	private Drawable mBackgroundDrawable;
	private Drawable mImageDrawable;
	private CharSequence mText;

	private boolean mIsCustomTheme;
	private boolean mShouldDrawText = true;

	private int mPrimaryColor;
	private int mSecondaryColor;

	private boolean mShouldDisplayEllipse = false;
	
	private final String ELLIPSE = "...";
	
	public PreviewTextCustomView(Context context) {
		super(context);

		mRes = context.getResources();
		createPaint();
	}

	private void createPaint() {
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(0xFFFF0000);
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(mWidth, mHeight);
	}

	public void setLayoutParams(int w, int h) {
		mWidth = w;
		mHeight = h;
		requestLayout();
	}

	public void setBackgroundDrawable(final Drawable background) {
		mBackgroundDrawable = background;
	}

	public void setImageDrawable(final Drawable background) {
		mShouldDrawText = false;
		mImageDrawable = background;
		invalidate();
	}

	public void drawText(CharSequence text) {
		mShouldDrawText = true;
		mText = text;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if(mBackgroundDrawable != null) {
			mBackgroundDrawable.setBounds(0, 0, mWidth, mHeight);
			mBackgroundDrawable.draw(canvas);
		}
		
		//if this is a cutom theme preview
		if(mIsCustomTheme) {
			onDrawCustomThemeKeyPreview(canvas);
			return;
		}
		
		if(mShouldDrawText) {
			if(mText != null) {
				final Rect bounds = new Rect();
				mPaint.getTextBounds(mText.toString(), 0, mText.length(), bounds);
				final float measuredWidth = mPaint.measureText(mText.toString());
				//final float posX = mWidth/2 /*- bounds.width()/2*/;  
				final float posX = mWidth/2 - measuredWidth/2;
				final float posY = mHeight/2 + bounds.height()/2 + mPaddingTop;
				
				canvas.drawText(mText.toString(), posX, posY, mPaint);
				
			}
		} else {
			mImageDrawable.setBounds(0, 0, mWidth, mHeight);
			mImageDrawable.draw(canvas);
		}
	}

	private int mPaddingTop;
	@Override
	public void setPadding(int left, int top, int right, int bottom) {
		super.setPadding(left, top, right, bottom);
		
		mPaddingTop = top;
	}
	
	protected void onDrawCustomThemeKeyPreview(Canvas canvas) {
		if(mShouldDrawText) {
			if(mText != null) {
				//draw text
				final Rect bounds = new Rect();
				mPaint.getTextBounds(mText.toString(), 0, mText.length(), bounds);

				float posX = mWidth/2 - bounds.width()/2;  
				float posY = mHeight/2 + bounds.height()/2 + mPaddingTop;
				mPaint.setColor(mSecondaryColor);
				canvas.drawText(mText.toString(), posX, posY, mPaint);
				
				//Ellipse is drawn only if the current theme
				//is a custom theme with keyboard background changed and doesnt have 
				// popup characters
				if(mShouldDisplayEllipse) {
					//draw ellipse ie.. three dots
					final Rect bounds1 = new Rect();
					mPaint.getTextBounds(ELLIPSE.toString(), 0, ELLIPSE.length(), bounds1);

					posX = mWidth - bounds1.width() - 10;
					posY = mHeight - 10; 
					mPaint.setColor(mPrimaryColor);
					canvas.drawText(ELLIPSE, posX, posY, mPaint);
				}
			}
		} else {
			mImageDrawable.setBounds(0, 0, mWidth, mHeight);
			mImageDrawable.draw(canvas);
		}
	}
	
	public void setDrawCustomThemeKeyPreview(final boolean isCustomTheme) {
		mIsCustomTheme = isCustomTheme;
	}

	public void setThemePrimaryColor(final int primaryColor) {
		mPrimaryColor = primaryColor;
	}
	
	public void setThemeSecondaryColor(final int secondaryColor) {
		mSecondaryColor = secondaryColor;
	}
	
	public void setTextSize(int unit, float size) {
		setRawTextSize(applyDimension(unit, size, mRes.getDisplayMetrics()));
	}

	public void setShouldDisplayEllipse(final boolean shouldDisplay) {
		mShouldDisplayEllipse = shouldDisplay;
	}
	
	private void setRawTextSize(float size) {
		if(mPaint == null) {
			return;
		}
		
		if (size != mPaint.getTextSize()) {
			mPaint.setTextSize(size);
			invalidate();
		}
	}

	public void setTypeface(final Typeface tf) {
		if(mPaint == null) {
			return;
		}
		
		if (mPaint.getTypeface() != tf) {
			mPaint.setTypeface(tf);
			invalidate();
		}
	}
	
	public void setTextColor(int color) {
		if(mPaint == null) {
			return;
		}
		
		if(mPaint.getColor() != color) {
			mPaint.setColor(color);
			invalidate();
		}
	}
	
	public static final int COMPLEX_UNIT_PX = 0;
    public static final int COMPLEX_UNIT_DIP = 1;
    public static final int COMPLEX_UNIT_SP = 2;
    
	private float applyDimension(int unit, float value, DisplayMetrics metrics) {
		switch (unit) {
		case COMPLEX_UNIT_PX:
			return value;
		case COMPLEX_UNIT_DIP:
			return value * metrics.density;
		case COMPLEX_UNIT_SP:
			return value * metrics.scaledDensity;
		}
		return 0;
	}
}
