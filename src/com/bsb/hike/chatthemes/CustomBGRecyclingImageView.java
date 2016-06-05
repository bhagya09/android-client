package com.bsb.hike.chatthemes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;

import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.ui.utils.RecyclingImageView;

/**
 * Created by sriram on 04/04/16.
 */

public class CustomBGRecyclingImageView extends RecyclingImageView {
    private Paint mPaint;
    private boolean mShowOverlay = false;
    private Context context;

    public CustomBGRecyclingImageView(Context context) {
        super(context);
        this.context = context;
        initialize();
    }

    public CustomBGRecyclingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initialize();
    }

    private void initialize(){
        mPaint = new Paint();
    }

    public void setOverLay(boolean status){
        mShowOverlay = status;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mShowOverlay) {
            mPaint.setColor(Color.argb(77, 0, 0, 0));
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawPaint(mPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec) - ChatThreadUtils.getStatusBarHeight(context);
        this.setMeasuredDimension(parentWidth, parentHeight);
    }
}
