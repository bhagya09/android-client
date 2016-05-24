package com.bsb.hike.chatHead;

import android.support.annotation.Nullable;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.squareup.okhttp.internal.Util;

public class CallListener implements IRequestListener
{
	final private static String TAG = "CALL_LISTENER";

	@Override
	public void onRequestSuccess(Response result)
	{
		String resultContent = result.getBody().getContent() == null ? null : result.getBody().getContent().toString();
		CallerContentModel callerContentModel = ChatHeadUtils.getCallerContentModelObject(resultContent);
		Logger.d(TAG, resultContent);
		if (callerContentModel != null && callerContentModel.getMsisdn() != null)
		{
			ContactManager.getInstance().insertIntoCallerTable(callerContentModel, true, true);
			if (callerContentModel.isBlock())
			{
				Utils.killCall();
			}
			else
			{
				StickyCaller.showCallerViewWithDelay(callerContentModel.getMsisdn(), callerContentModel, StickyCaller.SUCCESS, AnalyticsConstants.StickyCallerEvents.SERVER);
			}
		}
		else
		{
			HAManager.getInstance().stickyCallerAnalyticsNonUIEvent(StickyCaller.getCallEventFromCallType(StickyCaller.CALL_TYPE), AnalyticsConstants.StickyCallerEvents.UNKNOWN,
					null, AnalyticsConstants.StickyCallerEvents.FAIL, AnalyticsConstants.StickyCallerEvents.SERVER);
		}
	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{
	
	}

	@Override
	public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
	{
		HAManager.getInstance().stickyCallerAnalyticsNonUIEvent(StickyCaller.getCallEventFromCallType(StickyCaller.CALL_TYPE), AnalyticsConstants.StickyCallerEvents.UNKNOWN, null,
				AnalyticsConstants.StickyCallerEvents.FAIL, AnalyticsConstants.StickyCallerEvents.SERVER);

		if (httpException.getErrorCode() == HttpException.REASON_CODE_NO_NETWORK)
		{
			StickyCaller.showCallerViewWithDelay(null, null, StickyCaller.FAILURE, AnalyticsConstants.StickyCallerEvents.SERVER);
		}
	}
}
