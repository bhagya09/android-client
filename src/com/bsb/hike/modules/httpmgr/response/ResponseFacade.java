package com.bsb.hike.modules.httpmgr.response;

import java.util.List;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.interceptor.Pipeline;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;

/**
 * This object is passed in interceptor methods so that users can work on response objtained form the network
 * 
 * @author sidharth
 * 
 */
public class ResponseFacade implements IResponseFacade
{
	private Response response;

	public ResponseFacade(Response response)
	{
		this.response = response;
	}

	public Response getResponse()
	{
		return response;
	}

	@Override
	public String getUrl()
	{
		return response.getUrl();
	}

	@Override
	public int getStatusCode()
	{
		return response.getStatusCode();
	}

	@Override
	public String getReason()
	{
		return response.getReason();
	}

	@Override
	public List<Header> getHeaders()
	{
		return response.getHeaders();
	}

	@Override
	public ResponseBody<?> getBody()
	{
		return response.getBody();
	}

	@Override
	public Pipeline<IResponseInterceptor> getResponseInterceptors()
	{
		return response.getResponseInterceptors();
	}
}
