package com.bsb.hike.platform;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.ui.CustomTabActivityHelper;
import com.bsb.hike.utils.IntentFactory;

/**
 * Created by pushkargupta on 26/05/16.
 */
public class CustomTabFallBackImpl implements CustomTabActivityHelper.CustomTabFallback {
    Context mActivity;
    public CustomTabFallBackImpl(Context activity)
    {
        mActivity = activity;
    }

    @Override
    public void openUri(String url, String title) {
        Intent intent = IntentFactory.getWebViewActivityIntent(mActivity, url, title);
        mActivity.startActivity(intent);
        
    }
}
