package com.bsb.hike.chatHead;

import android.transition.ChangeTransform;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;

public class UpdateCallListener implements IRequestListener
{
	final private static String TAG = "UPDATE_CALL_LISTENER";

	@Override
	public void onRequestSuccess(Response result)
	{
		String resultContent = result.getBody().getContent() == null ? "{}" : result.getBody().getContent().toString();
		CallerContentModel callerContentModel = ChatHeadUtils.getCallerContentModelObject(resultContent);
		Logger.d(TAG, resultContent);
		if (callerContentModel != null && callerContentModel.getMsisdn() != null)
		{
			String contactName = ChatHeadUtils.getNameFromNumber(HikeMessengerApp.getInstance().getApplicationContext(), callerContentModel.getMsisdn());
			if (contactName == null)
			{
				ContactManager.getInstance().updateCallerTable(callerContentModel);
			}
           else
			{
				callerContentModel.setFullName(contactName);
				ContactManager.getInstance().updateCallerTable(callerContentModel);
			}
			StickyCaller.updateLayoutData(callerContentModel);
		}
	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{

	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{

	}
}