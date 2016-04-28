package com.bsb.hike.media;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.bsb.hike.R;

/**
 * Created by nidhi on 09/03/16.
 */
public class RedDot extends View {

    private Drawable dotDrawable;
    private float alpha;
    private long lastUpdateTime;
    private boolean isIncr;

    public RedDot(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
    }

    public RedDot(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public RedDot(Context context) {
        super(context);
        initialize(context);
    }

    private void initialize(Context context) {
        dotDrawable = context.getResources().getDrawable(R.drawable.ic_red_dot_walkie_talkie);
    }

    public void resetAlpha() {
        alpha = 1.0f;
        lastUpdateTime = System.currentTimeMillis();
        isIncr = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        dotDrawable.setBounds(0, 0, DrawUtils.dp(12), DrawUtils.dp(12));
        dotDrawable.setAlpha((int) (255 * alpha));
        long dt = (System.currentTimeMillis() - lastUpdateTime);
        if (!isIncr) {
            alpha -= dt / 900.0f;
            if (alpha <= 0) {
                alpha = 0;
                isIncr = true;
            }
        } else {
            alpha += dt / 100.0f;
            if (alpha >= 1) {
                alpha = 1;
                isIncr = false;
            }
        }
        lastUpdateTime = System.currentTimeMillis();
        dotDrawable.draw(canvas);
        invalidate();
    }
}
