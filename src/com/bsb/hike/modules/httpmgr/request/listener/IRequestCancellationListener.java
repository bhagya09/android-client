package com.bsb.hike.modules.httpmgr.request.listener;

import com.bsb.hike.modules.httpmgr.exception.HttpException;

/**
 * Interface used to deal with request cancellation
 * 
 * @author sidharth
 * 
 */
public interface IRequestCancellationListener
{
	/**
	 * {@link HttpException#REASON_CODE_CANCELLATION} exception is thrown
	 */
	public void onCancel();
}