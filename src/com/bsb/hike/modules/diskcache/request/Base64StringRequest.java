package com.bsb.hike.modules.diskcache.request;

import android.util.Base64;

public class Base64StringRequest extends CacheRequest
{
	private String dataString;
	
	private Base64StringRequest(Init<?> builder)
	{
		super(builder);
		this.dataString = builder.dataString;
	}
	
	protected static abstract class Init<S extends Init<S>> extends CacheRequest.Init<S>
	{
		private String dataString;

		public S setString(String dataString)
		{
			this.dataString = dataString;
			return self();
		}

		public Base64StringRequest build()
		{
			return new Base64StringRequest(this);
		}
	}
	
	public static class Builder extends Init<Builder>
	{
		@Override
		protected Builder self()
		{
			return this;
		}
	}

	@Override
	public byte[] getData()
	{
		return Base64.decode(dataString, Base64.DEFAULT);
	}

}
