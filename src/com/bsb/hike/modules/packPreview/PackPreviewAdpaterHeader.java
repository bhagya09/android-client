package com.bsb.hike.modules.packPreview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.bsb.hike.R;

/**
 * Created by anubhavgupta on 29/01/16.
 */
public class PackPreviewAdpaterHeader extends FrameLayout {


    public PackPreviewAdpaterHeader(Context context) {
        this(context, null);
    }

    public PackPreviewAdpaterHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PackPreviewAdpaterHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PackPreviewAdpaterHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init()
    {
        View tapTextHeader = LayoutInflater.from(getContext()).inflate(R.layout.tap_text_header, this, false);
    }

}
