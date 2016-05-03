package com.bsb.hike.modules.diskcache.request;


public class ByteArrayRequest extends CacheRequest
{
	private byte[] data;
	
	protected ByteArrayRequest(Init<?> builder)
	{
		super(builder);
		this.data = builder.data;
	}

	protected static abstract class Init<S extends Init<S>> extends CacheRequest.Init<S>
	{
		private byte[] data;

		public S setData(byte[] data)
		{
			this.data = data;
			return self();
		}

		public ByteArrayRequest build()
		{
			return new ByteArrayRequest(this);
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
		return data;
	}
}
