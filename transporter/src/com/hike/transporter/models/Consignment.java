package com.hike.transporter.models;

import java.io.File;

/**
 * 
 * @author himanshu/GauravK
 * 
 * This is the base of consignment that is used to deal with the User
 *
 */
public class Consignment
{
	public String type;

	public final String message;

	protected Object tag;

	public File file;

	public long awb = -1;

	protected Consignment(InitBuilder builder)
	{
		this.message = builder.message;
		this.tag = builder.tag;
		this.file = builder.file;
		this.type = builder.type;
		this.awb = builder.awb;
	}

	public void setAwb(long awb)
	{
		this.awb = awb;
	}

	public long getAwb()
	{
		return awb;
	}

	public Object getTag()
	{
		return tag;
	}

	public void setTag(Object tag)
	{
		this.tag = tag;
	}

	public String getMessage()
	{
		return message;
	}

	public File getFile()
	{
		return file;
	}

	public static abstract class InitBuilder<T extends InitBuilder>
	{

		protected String message;

		protected Object tag;

		protected File file;

		protected String type;
		
		protected long awb=-1;

		public abstract T getSelfObject();

		public InitBuilder(String message)
		{
			this.message = message;
		}

		public T tag(Object tag)
		{
			this.tag = tag;
			return getSelfObject();
		}

		public T file(File file)
		{
			this.file = file;
			return getSelfObject();
		}

		public T type(String type)
		{
			this.type = type;
			return getSelfObject();
		}
		
		public T awb(long awb)
		{
			this.awb=awb;
			return getSelfObject();
		}

		public abstract Consignment build();
	}

	public static class Builder extends InitBuilder<Builder>
	{

		public Builder(String message)
		{
			super(message);
			// TODO Auto-generated constructor stub
		}

		@Override
		public Builder getSelfObject()
		{
			return this;
		}

		@Override
		public Consignment build()
		{
			return new Consignment(this);
		}

	}
	
}
