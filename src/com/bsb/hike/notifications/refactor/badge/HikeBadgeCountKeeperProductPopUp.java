package com.bsb.hike.notifications.refactor.badge;

import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.Utils;

public class HikeBadgeCountKeeperProductPopUp extends HikeBadgeCountKeeper
{

	@Override
	public void onEventReceived(String type, Object object)
	{

		if (HikePubSub.PRODUCT_POPUP_BADGE_COUNT_CHANGED.equalsIgnoreCase(type))
		{

			setCount(ProductInfoManager.getInstance().getAllValidPopUp());
			HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);

		}

	}

	@Override
	protected void init()
	{
		mlistener = new String[] { HikePubSub.PRODUCT_POPUP_BADGE_COUNT_CHANGED };

	}

}
