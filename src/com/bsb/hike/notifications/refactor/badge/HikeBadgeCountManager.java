package com.bsb.hike.notifications.refactor.badge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import com.bsb.hike.badger.shortcutbadger.ShortcutBadger;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.hike.transporter.utils.Logger;

public class HikeBadgeCountManager implements Listener
{
	private static HikeBadgeCountManager mInstance;

	private ShortcutBadger badger;

	private final ArrayList<HikeBadgeCountKeeper> mBadgeCountKeeperList = new ArrayList<>();

	private String[] mlistener = new String[] { HikePubSub.BADGE_COUNT_CHANGED };

	public HikeBadgeCountManager()
	{
		badger = ShortcutBadger.with(HikeMessengerApp.getInstance().getApplicationContext());
		mBadgeCountKeeperList.add(new HikeBadgeCountKeeperMessages());
		mBadgeCountKeeperList.add(new HikeBadgeCountKeeperTimeline());
		mBadgeCountKeeperList.add(new HikeBadgeCountKeeperActivityUpdate());
		HikeMessengerApp.getPubSub().addListeners(this, mlistener);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.BADGE_COUNT_CHANGED.equals(type))
		{
			Logger.d("badger", "set badgeCount as " + getBadgeCount());
			badger.count(getBadgeCount());

		}

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
