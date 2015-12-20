package com.bsb.hike.chatHead;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

public class CallListener implements IRequestListener
{	
	@Override
	public void onRequestSuccess(Response result)
	{
		String resultContent = result.getBody().getContent() == null ? "{}" : result.getBody().getContent().toString();
 		CallerContentModel callerContentModel = ChatHeadUtils.getCallerContentModelObject(resultContent);
		if (callerContentModel != null && callerContentModel.getMsisdn() != null)
		{
			HikeSharedPreferenceUtil.getInstance(HikeConstants.CALLER_SHARED_PREF).saveData(callerContentModel.getMsisdn(), resultContent);
			StickyCaller.showCallerViewWithDelay(callerContentModel.getMsisdn(), resultContent, StickyCaller.SUCCESS,
					AnalyticsConstants.StickyCallerEvents.SERVER);
		}
		else
		{
			StickyCaller.showCallerViewWithDelay(null, resultContent, StickyCaller.FAILURE, AnalyticsConstants.StickyCallerEvents.SERVER);
		}
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