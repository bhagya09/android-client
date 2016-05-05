package com.bsb.hike.modules.httpmgr.response;

import android.text.TextUtils;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.Pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates all of the information necessary to make an HTTP response , implements {@link IResponseFacade}
 * 
 * @author sidharth
 */
public class Response implements IResponseFacade
{
	private String url;

	private int statusCode;

	private String reason;

	private List<Header> headers;

	private ResponseBody<?> body;

	private Pipeline<IResponseInterceptor> responseInterceptors;

	private Response(Builder builder)
	{
		this.url = builder.url;
		this.statusCode = builder.statusCode;
		this.reason = builder.reason;
		this.headers = builder.headers;
		this.body = builder.body;
		this.responseInterceptors = builder.responseInterceptors;
	}

	private Response(Response res)
	{
		this.url = res.url;
		this.statusCode = res.statusCode;
		this.reason = res.reason;
		this.headers = res.headers;
		this.body = res.body;
		this.responseInterceptors = res.responseInterceptors;
	}

	/**
	 * Returns the url of the response
	 * 
	 * @return
	 */
	@Override
	public String getUrl()
	{
		return url;
	}

	/**
	 * Returns the status code of the response
	 * 
	 * @return
	 */
	@Override
	public int getStatusCode()
	{
		return statusCode;
	}

	/**
	 * Returns the reason of the response
	 * 
	 * @return
	 */
	@Override
	public String getReason()
	{
		return reason;
	}

	/**
	 * Returns the list of headers of this response
	 * 
	 * @return
	 */
	@Override
	public List<Header> getHeaders()
	{
		return headers;
	}

	/**
	 * Returns the body of the response
	 * 
	 * @return
	 */
	@Override
	public ResponseBody<?> getBody()
	{
		return body;
	}

	@Override
	public Pipeline<IResponseInterceptor> getResponseInterceptors()
	{
		return this.responseInterceptors;
	}

	public void finish()
	{
		this.url = null;
		this.reason = null;
		this.headers = null;
		this.body = null;
		this.responseInterceptors = null;
	}

	public void replaceOrAddHeader(String name, String value)
	{
		if (TextUtils.isEmpty(name) || TextUtils.isEmpty(value))
		{
			return;
		}

		headers = headers != null ? headers : new ArrayList<Header>(1);

		Header header = HttpUtils.getHeader(headers, value);

		if(header != null)
		{
			header.setValue(value);
		}
		else
		{
			header = new Header(name, value);
			this.headers.add(header);
		}
	}

	public static class Builder
	{
		private String url;

		private int statusCode = -1;

		private String reason;

		private List<Header> headers;

		private ResponseBody<?> body;

		private Pipeline<IResponseInterceptor> responseInterceptors;

		/**
		 * Sets the url of the response
		 * 
		 * @param url
		 * @return
		 */
		public Builder setUrl(String url)
		{
			this.url = url;
			return this;
		}

		/**
		 * Sets the status code of the response
		 * 
		 * @param statusCode
		 * @return
		 */
		public Builder setStatusCode(int statusCode)
		{
			this.statusCode = statusCode;
			return this;
		}

		/**
		 * Sets the reason of the response
		 * 
		 * @param reason
		 * @return
		 */
		public Builder setReason(String reason)
		{
			this.reason = reason;
			return this;
		}

		/**
		 * Sets the headers list of the response
		 * 
		 * @param headers
		 * @return
		 */
		public Builder setHeaders(List<Header> headers)
		{
			this.headers = headers;
			return this;
		}

		/**
		 * Sets the body of the response
		 * 
		 * @param body
		 * @return
		 */

		public Builder setBody(ResponseBody<?> body)
		{
			this.body = body;
			return this;
		}

		/**
		 * Returns the {@link Response} object built using this builder
		 * 
		 * @return
		 */
		public Response build()
		{
			ensureSaneDefaults();
			return new Response(this);
		}

		private void ensureSaneDefaults()
		{
			if (TextUtils.isEmpty(url))
			{
				throw new IllegalStateException("Url must not be null and its length must be greater than 0");
			}
			if (statusCode < 0)
			{
				throw new IllegalStateException("status code < 0 " + statusCode);
			}
			if (responseInterceptors == null)
			{
				responseInterceptors = new Pipeline<IResponseInterceptor>();
			}
		}
	}

	public Response clone()
	{
		return new Response(this);
	}
}
