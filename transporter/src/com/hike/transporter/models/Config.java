package com.hike.transporter.models;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.hike.transporter.DefaultRetryPolicy;

/**
 * 
 * @author himanshu/GauravK
 * 
 * This is the configuration that is set by the client and the server for proper functioning of the library.
 *
 */
public class Config
{

	private static final int DEFAULT_CONNECT_TIMEOUT = 5 * 1000; // seconds

	private static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 20 * 1000; // seconds

	private static final int DEFAULT_KEEP_ALIVE_SCREEN_OFF_TIMEOUT = 60 * 1000; // seconds

	private List<Topic> topics;

	private String ip;

	private int port;

	private int connectTimeout = DEFAULT_CONNECT_TIMEOUT, keepAlive = DEFAULT_KEEP_ALIVE_TIMEOUT, keepAliveScreenOff = DEFAULT_KEEP_ALIVE_SCREEN_OFF_TIMEOUT; // in seconds

	private boolean tcpNoDelay; // true- nagles algo off, false - nagles algo on

	private String ackTopic;

	private String namespace;

	private boolean sendOldPersistedPackages = true;

	private DefaultRetryPolicy defaultRetryPolicy = new DefaultRetryPolicy();

	private Config(List<Topic> list, String ip, int port,String ackTopic)
	{
		this.topics = list;
		this.ip = ip;
		this.port = port;
		this.ackTopic=ackTopic;
	}

	public static class ConfigBuilder
	{
		private Config clientConfig;

		public ConfigBuilder(List<Topic> list, String ip, int port,String ackTopic)
		{
			clientConfig = new Config(list, ip, port,ackTopic);
		}

		public ConfigBuilder connectTimeout(int connectTimeout)
		{
			clientConfig.connectTimeout = connectTimeout;
			return this;
		}

		public ConfigBuilder keepAliveTimeout(int keepAlive)
		{
			clientConfig.keepAlive = keepAlive;
			return this;
		}

		public ConfigBuilder keepAliveTimeoutScreenOff(int keepAlive)
		{
			clientConfig.keepAliveScreenOff = keepAlive;
			return this;
		}

		public ConfigBuilder tcpNoDelay(boolean tcpNoDelay)
		{
			clientConfig.tcpNoDelay = tcpNoDelay;
			return this;
		}

		public ConfigBuilder nameSpace(String namespace)
		{
			clientConfig.namespace = namespace;
			return this;
		}

		public ConfigBuilder sendoldPersistedMessages(boolean sendoldPersistedMessages)
		{
			clientConfig.sendOldPersistedPackages = sendoldPersistedMessages;
			return this;
		}

		public ConfigBuilder setRetryPolicy(DefaultRetryPolicy retryPolicy)
		{
			clientConfig.defaultRetryPolicy = retryPolicy;
			return this;
		}

		public Config build()
		{
			if (TextUtils.isEmpty(clientConfig.ip) || clientConfig.port == 0 || clientConfig.topics == null || clientConfig.topics.size() == 0 || !isAckTopicPresent())
			{
				throw new IllegalArgumentException(" OOPS ...Parameters are wrong.KIndly check again");
			}
			return clientConfig;
		}
		
		private boolean isAckTopicPresent()
		{
			for (Topic t : clientConfig.topics)
			{
				if (t.getName().equals(clientConfig.ackTopic))
				{
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * @return the topics
	 */
	public List<Topic> getTopics()
	{
		return topics;
	}

	/**
	 * @return the ip
	 */
	public String getIp()
	{
		return ip;
	}

	/**
	 * @return the port
	 */
	public int getPort()
	{
		return port;
	}

	/**
	 * @return the connectTimeout
	 */
	public int getConnectTimeout()
	{
		return connectTimeout;
	}

	/**
	 * @return the keepAlive
	 */
	public int getKeepAlive()
	{
		return keepAlive;
	}

	/**
	 * @return the keepAliveScreenOff
	 */
	public int getKeepAliveScreenOff()
	{
		return keepAliveScreenOff;
	}

	/**
	 * @return the tcpNoDelay
	 */
	public boolean isTcpNoDelay()
	{
		return tcpNoDelay;
	}

	/**
	 * @return the ackTopic
	 */
	public String getAckTopic()
	{
		return ackTopic;
	}

	/**
	 * @return the namespace
	 */
	public String getNamespace()
	{
		return namespace;
	}

	/**
	 * @return the sendOldPersistedPackages
	 */
	public boolean isSendOldPersistedPackages()
	{
		return sendOldPersistedPackages;
	}

	public DefaultRetryPolicy getDefaultRetryPolicy()
	{
		return defaultRetryPolicy;
	}
	public String serialize()
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put("connectTimeout", connectTimeout);
			json.put("keepAlive", keepAlive);
			json.put("keepAliveScreenOff", keepAliveScreenOff);
			json.put("ackTopic", ackTopic);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return json.toString();
	}

	public void deserialize(String jsonString)
	{
		try
		{
			JSONObject json = new JSONObject(jsonString);
			this.connectTimeout = json.getInt("connectTimeout");
			this.keepAlive = json.getInt("keepAlive");
			this.keepAliveScreenOff = json.getInt("keepAliveScreenOff");
			this.ackTopic = json.getString("ackTopic");
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
