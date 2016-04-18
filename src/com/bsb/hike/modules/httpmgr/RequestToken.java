package com.bsb.hike.modules.httpmgr;

import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.Pipeline;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;

import java.util.List;

/**
 * Provides a mechanism for executing or canceling a http request without giving access to the request class outside the http manager
 * 
 * @author sidharth
 * 
 */
public class RequestToken
{
	private Request<?> request;

	public RequestToken(Request<?> request)
	{
		this.request = request;
	}

	/**
	 * Passes the request to HttpManager for execution
	 */
	public void execute()
	{
		FileSavedState fss = request.getState();
		if (!isRequestRunning())
		{
			HttpManager.getInstance().addRequest(request);
			if (fss != null)
				fss.setFTState(FTState.INITIALIZED);
		}
		else
		{
			if (fss != null)
				fss.setFTState(FTState.IN_PROGRESS);
		}
	}

	public void pause()
	{
		request.getState().setFTState(FTState.PAUSED);
	}

	/**
	 * Passes the request to HttpManager for execution with {@link ClientOptions} passed as parameter
	 * 
	 * @param options
	 */
	public void execute(ClientOptions options)
	{
		FileSavedState fss = request.getState();
		if (!isRequestRunning())
		{
			if (fss != null)
				fss.setFTState(FTState.INITIALIZED);
			HttpManager.getInstance().addRequest(request, options);
		}
		else
		{
			if (fss != null)
				fss.setFTState(FTState.IN_PROGRESS);
		}
	}

	/**
	 * Cancels the request
	 */
	public void cancel()
	{
		FileSavedState fss = request.getState();
		if (fss != null)
			fss.setFTState(FTState.CANCELLED);
		HttpManager.getInstance().cancel(request);
	}

	public void addRequestListener(IRequestListener listener)
	{
		HttpManager.getInstance().addRequestListener(request, listener);
	}
	
	/**
	 * Removes particular listener from list of listeners for a request
	 * 
	 * @param listener
	 */
	public void removeListener(IRequestListener listener)
	{
		HttpManager.getInstance().removeListener(request, listener);
	}

	/**
	 * Removes list of listeners from list of request listeners for a request
	 * 
	 * @param listeners
	 */
	public void removeListeners(List<IRequestListener> listeners)
	{
		HttpManager.getInstance().removeListeners(request, listeners);
	}

	/**
	 * Determines whether a request is running or not
	 * 
	 * @return true if request is already running
	 */
	public boolean isRequestRunning()
	{
		return HttpManager.getInstance().isRequestRunning(request);
	}

	public Pipeline<IRequestInterceptor> getRequestInterceptors()
	{
		return request.getRequestInterceptors();
	}
	
	public Pipeline<IResponseInterceptor> getResponseInterceptors()
	{
		return request.getResponseInterceptors();
	}
	
	public IRequestBody getRequestBody()
	{
		return request.getBody();
	}

	public FileSavedState getState()
	{
		return request.getState();
	}

	public int getChunkSize()
	{
		return request.getChunkSize();
	}
}