package com.bsb.hike.modules.httpmgr.hikehttp;

import com.bsb.hike.modules.httpmgr.exception.HttpException;

public interface IHikeHttpTaskResult
{
	public void doOnSuccess(Object result);

	public void doOnFailure(HttpException exception);
}
