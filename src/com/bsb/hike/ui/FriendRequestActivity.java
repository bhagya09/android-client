package com.bsb.hike.ui;

import com.bsb.hike.R;
import com.bsb.hike.ui.fragments.AddFriendsFragment;
import com.bsb.hike.ui.fragments.AddedMeFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Created by gauravmittal on 19/05/16.
 */
public class FriendRequestActivity extends HikeAppStateBaseFragmentActivity
{

	public static final String ADD_FRIENDS = "add_friends";

	public static final String ADDED_ME = "added_me";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_friend_request);
		if (getIntent().hasExtra(ADD_FRIENDS))
			addAddFriendsFragment();
		else
			addAddedMeFragment();
	}

	private void addAddFriendsFragment()
	{
		getSupportActionBar().setTitle(R.string.add_friends);

		AddFriendsFragment addFriendsFragment = null;

		Fragment frag = getSupportFragmentManager().findFragmentByTag(ADD_FRIENDS);
		if (frag != null)
		{
			addFriendsFragment = (AddFriendsFragment) frag;
		}
		if (addFriendsFragment == null)
		{
			addFriendsFragment = new AddFriendsFragment();
		}
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_layout, addFriendsFragment, ADD_FRIENDS).commitAllowingStateLoss();
	}

	private void addAddedMeFragment()
	{
		getSupportActionBar().setTitle(R.string.added_me);

		AddedMeFragment addedMeFragment = null;

		Fragment frag = getSupportFragmentManager().findFragmentByTag(ADD_FRIENDS);
		if (frag != null)
		{
			addedMeFragment = (AddedMeFragment) frag;
		}
		if (addedMeFragment == null)
		{
			addedMeFragment = new AddedMeFragment();
		}
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_layout, addedMeFragment, ADDED_ME).commitAllowingStateLoss();

	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

}
