package com.bsb.hike.chatHead;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

public class CallListener implements IRequestListener
{
	private String number;

	public void setNumber(String number)
	{
		this.number = number;
	}
	
	@Override
	public void onRequestSuccess(Response result)
	{
		HikeSharedPreferenceUtil.getInstance(HikeConstants.CALLER_SHARED_PREF).saveData(number, result.getBody().getContent().toString());
		StickyCaller.showCallerView(number, result.getBody().getContent().toString(), StickyCaller.SUCCESS);
	}

	@Override
	public void onRequestProgressUpdate(float progress)
	{
		StickyCaller.showCallerView(null, null, StickyCaller.LOADING);
	}

	@Override
	public void onRequestFailure(HttpException httpException)
	{
		StickyCaller.showCallerView(null, null, StickyCaller.FAILURE);
	}
}