package com.bsb.hike;

import static com.bsb.hike.MqttConstants.COLON;
import static com.bsb.hike.MqttConstants.CONNECTION_TIMEOUT_SECONDS;
import static com.bsb.hike.MqttConstants.SSL_PROTOCOL;
import static com.bsb.hike.MqttConstants.TCP_PROTOCOL;

import com.bsb.hike.utils.Logger;

/**
 * This class is to keep track of all properties related to a connect call try. on connect failure we need to try next connect by changing some of these properties.
 * 
 * @author pankaj
 * 
 */
public class HostInfo
{
	public enum ConnectExceptions
	{
		NO_EXCEPTION, DNS_EXCEPTION, SOCKET_TIME_OUT_EXCEPTION
	}

	private String protocol;

	private String host;

	private int port;

	private int connectTimeOut;

	private int connectRetryCount = 0;

	private ConnectExceptions exceptionOnConnect = ConnectExceptions.NO_EXCEPTION;

	public String getProtocol()
	{
		return protocol;
	}

	public void setProtocol(int portNum)
	{
		if(portNum == MqttConstants.PRODUCTION_BROKER_PORT_NUMBER_SSL)
		{
			this.protocol = SSL_PROTOCOL;
		}
		else
		{
			this.protocol = TCP_PROTOCOL;
		}
		
	}

	public String getHost()
	{
		return host;
	}

	public void setHost(String host)
	{
		this.host = host;
	}

	public int getPort()
	{
		return port;
	}

	public void setPort(int port)
	{
		this.port = port;
	}

	public int getConnectTimeOut()
	{
		return connectTimeOut;
	}

	private void setConnectTimeOut(int connectTimeOut)
	{
		this.connectTimeOut = connectTimeOut;
	}

	public void setConnectTimeOut(HostInfo previousHostInfo)
	{
		if (previousHostInfo != null)
		{
			if (previousHostInfo.getExceptionOnConnect() == ConnectExceptions.SOCKET_TIME_OUT_EXCEPTION)
			{
				incrementConnectRetryCount();
			}
		}

		int connectTimeOut = CONNECTION_TIMEOUT_SECONDS[Math.min(CONNECTION_TIMEOUT_SECONDS.length - 1, connectRetryCount)];
		setConnectTimeOut(connectTimeOut);
	}

	private void incrementConnectRetryCount()
	{
		connectRetryCount++;
		connectRetryCount = Math.min(CONNECTION_TIMEOUT_SECONDS.length - 1, connectRetryCount);
		Logger.e(this.getClass().getSimpleName(), "Increasing connect retry count , connectRetryCount : " + connectRetryCount);
	}

	public ConnectExceptions getExceptionOnConnect()
	{
		return exceptionOnConnect;
	}

	public void setExceptionOnConnect(ConnectExceptions exceptionOnConnect)
	{
		this.exceptionOnConnect = exceptionOnConnect;
	}

	public String getServerUri()
	{
		return protocol + host + COLON + port;
	}

}