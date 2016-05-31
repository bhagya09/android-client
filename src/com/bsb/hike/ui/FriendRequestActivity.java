package com.bsb.hike.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.ui.fragments.AddFriendsFragment;
import com.bsb.hike.ui.fragments.AddedMeFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

/**
 * Created by gauravmittal on 19/05/16.
 */
public class FriendRequestActivity extends HikeAppStateBaseFragmentActivity {

    public static final String ADD_FRIENDS = "add_friends";

    public static final String ADDED_ME = "added_me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);
        if (getIntent().hasExtra(ADD_FRIENDS)) {
            setupActionBar(R.string.add_friends);
            addAddFriendsFragment();
        } else {
            setupActionBar(R.string.added_me);
            addAddedMeFragment();
        }
    }

    private void setupActionBar(int titleResId) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);
        TextView title = (TextView) actionBarView.findViewById(R.id.title);
        if (titleResId > 0)
            title.setText(titleResId);
        else
            title.setText("");
        actionBar.setCustomView(actionBarView);
        Toolbar parent = (Toolbar) actionBarView.getParent();
        parent.setContentInsetsAbsolute(0, 0);
    }

    private void addAddFriendsFragment() {
        AddFriendsFragment addFriendsFragment = null;

        Fragment frag = getSupportFragmentManager().findFragmentByTag(ADD_FRIENDS);
        if (frag != null) {
            addFriendsFragment = (AddFriendsFragment) frag;
        }
        if (addFriendsFragment == null) {
            addFriendsFragment = new AddFriendsFragment();
        }
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_layout, addFriendsFragment, ADD_FRIENDS).commit();
    }

    private void addAddedMeFragment() {
        AddedMeFragment addedMeFragment = null;

        Fragment frag = getSupportFragmentManager().findFragmentByTag(ADD_FRIENDS);
        if (frag != null) {
            addedMeFragment = (AddedMeFragment) frag;
        }
        if (addedMeFragment == null) {
            addedMeFragment = new AddedMeFragment();
        }
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_layout, addedMeFragment, ADDED_ME).commit();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
