package com.bsb.hike.notifications.refactor.badge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import android.content.Context;

import com.bsb.hike.badger.shortcutbadger.ShortcutBadger;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;

public class HikeBadgeCountManager implements Listener
{

	private ShortcutBadger badger;

	private final ArrayList<HikeBadgeCountKeeper> mBadgeCountKeeperList = new ArrayList<>();

	private String[] mlistener = new String[] { HikePubSub.BADGE_COUNT_CHANGED, HikePubSub.BADGE_COUNT_RESET };

	private static final String BADGE_COUNT_ENABLED = "badgecountenabled";

	private static Context mContext;

	public HikeBadgeCountManager()
	{
		mContext = HikeMessengerApp.getInstance().getApplicationContext();
		badger = ShortcutBadger.with(mContext);
		mBadgeCountKeeperList.add(new HikeBadgeCountKeeperMessages());
		mBadgeCountKeeperList.add(new HikeBadgeCountKeeperTimeline());
		mBadgeCountKeeperList.add(new HikeBadgeCountKeeperActivityUpdate());
		mBadgeCountKeeperList.add(new HikeBadgeCountKeeperUserJoin());
		mBadgeCountKeeperList.add(new HikeBadgeCountKeeperProductPopUp());
		HikeMessengerApp.getPubSub().addListeners(this, mlistener);
		init();
	}

	private void init()
	{
		if (!Utils.isUserSignedUp(mContext, false))
		{
			badger.count(0);
			return;
		}
		updateBadgeCount();

	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.BADGE_COUNT_CHANGED.equals(type))
		{

			updateBadgeCount();

		}
		else if (HikePubSub.BADGE_COUNT_RESET.equals(type) || HikePubSub.ACCOUNT_RESET_OR_DELETE.equals(type))
		{
			badger.count(0);
		}

	}

	private void updateBadgeCount()
	{
		// To check if badge counter is disabled or not
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.BADGE_COUNT_ENABLED, true))
		{
			int count = getBadgeCount();
			Logger.d("badger", "set badgeCount as " + count);
			badger.count(count);

		}
		else
			badger.count(0);
	}

	private int getBadgeCount()
	{
		int count = 0;
		for (HikeBadgeCountKeeper item : mBadgeCountKeeperList)
		{
			if (item.getBadgeCountPriority() > 0)
			{
				Logger.d("badger", "count is " + item.getCount());
				count += item.getCount();
			}
		}
		return count;
	}

}
