package com.bsb.hike.modules.httpmgr.request.listener;

import android.support.annotation.Nullable;

import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.response.Response;

/**
 * Interface used to deal with request result. Two cases : request failed or succeed. Implement this interface to retrieve request result or to manage error
 * 
 * @author sidharth
 */
public interface IRequestListener
{
	/**
	 * In case of request failure caller will receive a {@link HttpException} according to which caller can take further action
	 *
	 * @param errorResponse
	 * @param httpException
	 */
	void onRequestFailure(@Nullable Response errorResponse, HttpException httpException);

	/**
	 * In case of successful request {@link Response} object will be returned to the caller
	 * 
	 * @param result
	 */
	void onRequestSuccess(Response result);

	/**
	 * Sends the request progress in percentage
	 * 
	 * @param progress
	 */
	void onRequestProgressUpdate(float progress);
}