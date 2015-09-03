package com.bsb.hike.modules.httpmgr.request.facade;

import java.net.URL;
import java.util.List;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.Pipeline;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;

/**
 * This interface is implemented both by {@link RequestFacade} and {@link Request} class. This interface provides methods that both class must implement so that user can add some
 * request parameters in {@link IPreProcessListener} listener method call.
 * 
 * @author anubhavgupta
 * 
 */
public interface IRequestFacade
{
	public URL getUrl();

	public void setUrl(URL url);

	public String getMethod();

	public List<Header> getHeaders();

	public void addHeaders(List<Header> headers);

	public IRequestBody getBody();

	public void setBody(IRequestBody body);

	public Pipeline<IRequestInterceptor> getRequestInterceptors();
}
