package com.hike.transporter.models;

import java.io.File;
import java.net.URI;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.hike.transporter.utils.TConstants;

/**
 * 
 * @author himanshu/GauravK
 * 
 * 	This class is used by the user to send any consignment to the receiver.All the interaction of the user with the library 
 * 	will be taken place wrt to this only.
 *
 */
public class SenderConsignment extends Consignment
{

	public final String topic;

	public boolean persistance;

	public final boolean ackRequired;

	public int sendFileSize = -1;

	private int hashCode = -1;
	
	private boolean spaceCheckedOnClient;

	private SenderConsignment(InitBuilder builder)
	{
		super(builder);
		this.ackRequired = builder.ackRequired;
		this.persistance = builder.persistance;
		this.topic = builder.topic;
	}

	public void setSendFileSize(int sendFileSize)
	{
		this.sendFileSize = sendFileSize;
	}

	public int getSendFileSize()
	{
		return sendFileSize;
	}
	
	public boolean isSpaceCheckedOnClient()
	{
		return spaceCheckedOnClient;
	}
	
	public void setSpaceCheckedOnClient(boolean spaceCheckedOnClient)
	{
		this.spaceCheckedOnClient = spaceCheckedOnClient;
	}

	public int getTotalFileSize()
	{
		if (file != null)
		{
			return (int) file.length();
		}
		return -1;
	}

	public static abstract class InitBuilder<T extends InitBuilder> extends Consignment.InitBuilder<T>
	{
		private String topic;

		private boolean persistance = true;

		private boolean ackRequired = true;

		public InitBuilder(String message, String topic)
		{
			super(message);
			this.topic = topic;
		}

		public InitBuilder<T> persistance(boolean persistance)
		{
			this.persistance = persistance;
			return this;
		}

		public InitBuilder<T> ackRequired(boolean ackRequired)
		{
			this.ackRequired = ackRequired;
			return this;
		}

		@Override
		public SenderConsignment build()
		{
			// TODO Auto-generated method stub
			return new SenderConsignment(this);
		}

	}

	public static class Builder extends InitBuilder<Builder>
	{

		public Builder(String message, String topic)
		{
			super(message, topic);
		}

		@Override
		public Builder getSelfObject()
		{
			return this;
		}

	}

	public String getTopic()
	{
		return topic;
	}

	@Override
	public int hashCode()
	{
		if (hashCode == -1)
		{
			String s = new String(getMessage() + getTopic() + System.currentTimeMillis());
			hashCode = s.hashCode();
		}
		return super.hashCode();
	}

	public String serialize()
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(TConstants.TYPE, type);
			json.put(TConstants.DATA, message);
			if (file != null)
			{
				json.put(TConstants.FILESIZE, file.length());
			}
			if (awb != -1)
				json.put(TConstants.AWB, awb);
		}
		catch (JSONException e)
		{

			e.printStackTrace();
		}
		return json.toString();

	}

	public String toJSONString()
	{
		JSONObject object = new JSONObject();

		try
		{

			object.put(TConstants.MSG, message);
			if (file != null)
				object.put(TConstants.FILE, file.getAbsolutePath());
			object.put(TConstants.TOPIC, topic);
			object.put(TConstants.PERSISTANCE, false);
			object.put(TConstants.ACK, ackRequired);
			object.put(TConstants.TYPE, type);
			object.put(TConstants.AWB, awb);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return object.toString();
	}

	public static SenderConsignment createSenderConsignment(JSONObject object) throws JSONException
	{
		File file = TextUtils.isEmpty(object.optString(TConstants.FILE)) ? null : new File(object.optString(TConstants.FILE));
		SenderConsignment senderConsignment = new SenderConsignment.Builder(object.optString(TConstants.MSG), object.optString(TConstants.TOPIC))
				.ackRequired(object.optBoolean(TConstants.ACK)).tag(null).file(file).persistance(false).awb(object.optInt(TConstants.AWB)).build();
		return senderConsignment;
	}
}