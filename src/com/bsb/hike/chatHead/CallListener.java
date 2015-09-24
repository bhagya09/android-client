package com.bsb.hike.chatHead;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

public class CallListener implements IRequestListener
{	
	@Override
	public void onRequestSuccess(Response result)
	{
		HikeSharedPreferenceUtil.getInstance(HikeConstants.CALLER_SHARED_PREF).saveData(StickyCaller.callCurrentNumber, result.getBody().getContent().toString());
		StickyCaller.showCallerViewWithDelay(StickyCaller.callCurrentNumber, result.getBody().getContent().toString(), StickyCaller.SUCCESS, AnalyticsConstants.StickyCallerEvents.SERVER);
	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{
	
	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{
		StickyCaller.showCallerViewWithDelay(null, Integer.toString(httpException.getErrorCode()), StickyCaller.FAILURE, AnalyticsConstants.StickyCallerEvents.SERVER);
	}
}