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

	public static final int SET_PRODUCT_POPUPCOUNT = 1001;

	@Override
	public void onEventReceived(String type, Object object)
	{

		if (HikePubSub.PRODUCT_POPUP_BADGE_COUNT_CHANGED.equalsIgnoreCase(type))
		{

			mHandler.sendEmptyMessageDelayed(SET_PRODUCT_POPUPCOUNT, 500);

		}

	}

	@Override
	protected void init()
	{
		mlistener = new String[] { HikePubSub.PRODUCT_POPUP_BADGE_COUNT_CHANGED };

	}

	Handler mHandler = new Handler()
	{

		@Override
		public void handleMessage(Message msg)
		{
			// TODO Auto-generated method stub
			if (msg.what == SET_PRODUCT_POPUPCOUNT)
			{
				setCount(ProductInfoManager.getInstance().getAllValidPopUp());
				HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);
			}
		}

	};

}
