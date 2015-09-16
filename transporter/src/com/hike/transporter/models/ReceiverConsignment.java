package com.hike.transporter.models;


/**
 * 
 * @author himanshu/GauravK
 * 
 * This class is returned to the user when any message arrives.It has all the info such as File,chunk read,message details
 * etc.
 *
 */
public class ReceiverConsignment extends Consignment
{

	private int totalFileSize, readFileSize; // bytes

	
	public void setReadFileSize(int FileRead)
	{
		this.readFileSize=FileRead;
	}
	
	public int getTotalFileSize()
	{
		return totalFileSize;
	}

	public int getFileRead()
	{
		return readFileSize;
	}
	protected ReceiverConsignment(InitBuilder builder)
	{
		super(builder);
		totalFileSize=builder.totalSize;
	
	}

	public static abstract class InitBuilder<T extends InitBuilder<T>> extends Consignment.InitBuilder<T>
	{

		private int totalSize;

		public InitBuilder(String message)
		{
			super(message);
		}

		public abstract T getSelfObject();

		public T totalFileSize(int totalFileSize)
		{
			this.totalSize = totalFileSize;
			return getSelfObject();
		}

		@Override
		public ReceiverConsignment build()
		{
			return new ReceiverConsignment(this);
		}

	}

	public static class Builder extends InitBuilder<Builder>
	{
		public Builder(String message)
		{
			super(message);
		}

		@Override
		public Builder getSelfObject()
		{
			return this;
		}
	}
}
