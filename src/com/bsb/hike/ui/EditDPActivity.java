package com.bsb.hike.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.ui.fragments.ImageViewerFragment;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

/**
 * Created by gauravmittal on 29/05/16.
 */
public class EditDPActivity extends HikeAppStateBaseFragmentActivity {

    ContactInfo myInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_dp_activity);
        myInfo = Utils.getUserContactInfo(false);
        setupActionBar();
        setupMyPhotoFragment();
    }

    private void setupMyPhotoFragment() {
        Bundle arguments = new Bundle();
        arguments.putString(HikeConstants.Extras.MAPPED_ID, myInfo.getMsisdn() + ProfileActivity.PROFILE_PIC_SUFFIX);
        arguments.putBoolean(HikeConstants.Extras.IS_STATUS_IMAGE, false);
        arguments.putBoolean(HikeConstants.CAN_EDIT_DP, false);
        ImageViewerFragment imageViewerFragment = new ImageViewerFragment();
        imageViewerFragment.setArguments(arguments);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.my_photo_fragment, imageViewerFragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void setupActionBar() {
        StatusBarColorChanger.setStatusBarColor(this, HikeConstants.STATUS_BAR_TRANSPARENT);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayHomeAsUpEnabled(true);
        View actionBarView = getLayoutInflater().inflate(R.layout.compose_action_bar, null);
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_header_photo_viewer));

        actionBarView.findViewById(R.id.title).setVisibility(View.GONE);
        actionBarView.findViewById(R.id.subtext).setVisibility(View.GONE);
        actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

        actionBar.setCustomView(actionBarView);
        Toolbar parent = (Toolbar) actionBarView.getParent();
        parent.setContentInsetsAbsolute(0, 0);
    }
}