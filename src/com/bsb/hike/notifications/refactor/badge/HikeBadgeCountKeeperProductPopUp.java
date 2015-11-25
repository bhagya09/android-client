package com.bsb.hike.notifications.refactor.badge;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.productpopup.ProductInfoManager;

public class HikeBadgeCountKeeperProductPopUp extends HikeBadgeCountKeeper
{
	public static final String BADGE_COUNT_PRODUCT_POPUP = "badgecountproductpopup";

	@Override
	public void onEventReceived(String type, Object object)
	{

		if (HikePubSub.PRODUCT_POPUP_BADGE_COUNT_CHANGED.equalsIgnoreCase(type))
		{

			setCount(ProductInfoManager.getInstance().getAllValidPopUp());

		}
		else
			super.onEventReceived(type, object);
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_CHANGED, null);

	}

	@Override
	protected void init()
	{
		mlistener = new String[] { HikePubSub.PRODUCT_POPUP_BADGE_COUNT_CHANGED };
		defaultCount=ProductInfoManager.getInstance().getAllValidPopUp();

	}

	@Override
	public String getSharedPreferenceTag()
	{
		// TODO Auto-generated method stub
		return BADGE_COUNT_PRODUCT_POPUP;
	}

}
