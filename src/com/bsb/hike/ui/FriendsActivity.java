package com.bsb.hike.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.ui.fragments.AddFriendsFragment;
import com.bsb.hike.ui.fragments.AddFriendsViaABFragment;
import com.bsb.hike.ui.fragments.AddedMeFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentFactory;

/**
 * Created by gauravmittal on 19/05/16.
 */
public class FriendsActivity extends HikeAppStateBaseFragmentActivity {

    public static final String ADD_FRIENDS = "add_friends";

    public static final String ADD_FRIENDS_ADDRESSBOOK = "add_friends_ab";

    public static final String ADDED_ME = "added_me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);
        setupFragment(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setupFragment(intent);
    }

    private void setupFragment(Intent intent) {
        if (intent.hasExtra(ADD_FRIENDS)) {
            setupActionBar(R.string.add_friends);
            addAddFriendsFragment();
        } else if (intent.hasExtra(ADD_FRIENDS_ADDRESSBOOK)) {
            setupActionBar(R.string.address_book);
            addAddFriendsViaABFragment();
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
            if (!addFriendsFragment.isAdded())
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_layout, addFriendsFragment, ADD_FRIENDS).commit();
        }
        if (addFriendsFragment == null) {
            addFriendsFragment = new AddFriendsFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_layout, addFriendsFragment, ADD_FRIENDS).commit();
        }
    }

    private void addAddFriendsViaABFragment() {
        AddFriendsViaABFragment addFriendsViaABFragment = null;

        Fragment frag = getSupportFragmentManager().findFragmentByTag(ADD_FRIENDS_ADDRESSBOOK);
        if (frag != null) {
            addFriendsViaABFragment = (AddFriendsViaABFragment) frag;
            if (!addFriendsViaABFragment.isAdded())
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_layout, addFriendsViaABFragment, ADD_FRIENDS_ADDRESSBOOK).commit();
        }
        if (addFriendsViaABFragment == null) {
            addFriendsViaABFragment = new AddFriendsViaABFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_layout, addFriendsViaABFragment, ADD_FRIENDS_ADDRESSBOOK).commit();
        }
    }

    private void addAddedMeFragment() {
        AddedMeFragment addedMeFragment = null;

        Fragment frag = getSupportFragmentManager().findFragmentByTag(ADDED_ME);
        if (frag != null) {
            addedMeFragment = (AddedMeFragment) frag;
            if (!addedMeFragment.isAdded())
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_layout, addedMeFragment, ADDED_ME).commit();
        }
        if (addedMeFragment == null) {
            addedMeFragment = new AddedMeFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_layout, addedMeFragment, ADDED_ME).commit();
        }

    }

    @Override
    public void onBackPressed() {
        if (isTaskRoot())
            startActivity(IntentFactory.getHomeActivityIntent(this));
        super.onBackPressed();
    }
}
