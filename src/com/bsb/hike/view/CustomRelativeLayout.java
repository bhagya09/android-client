package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * Created by anuj on 10/05/16.
 */
public class CustomRelativeLayout extends RelativeLayout {

    private GestureDetector gestureDetector;
    private IDragCallback dragListener;

    public CustomRelativeLayout(Context context) {
        super(context);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public interface IDragCallback {
        void onDrag();
    }

    public void setOnDragListener(IDragCallback listener) {
        this.dragListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return false;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            //that's when user starts dragging
            if(dragListener != null) {
                dragListener.onDrag();
            }
            return false;
        }


    }
}
