package com.bsb.hike;

import static com.bsb.hike.MqttConstants.*;

import java.util.List;
import java.util.Random;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

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
	
	private volatile int ipConnectCount = 0;
	
	List<String> serverURIs = null;

	private ConnectExceptions exceptionOnConnect = ConnectExceptions.NO_EXCEPTION;
	
	public HostInfo(HostInfo previousHostInfo, List<String> serverURIs)
	{
		boolean isSslOn = Utils.switchSSLOn(HikeMessengerApp.getInstance());
		boolean isProduction = Utils.isOnProduction();
		
		this.serverURIs = serverURIs;
		
		setIpConnectCount(previousHostInfo);
		this.setHost(previousHostInfo, isProduction);
		setPort(previousHostInfo, isProduction, isSslOn);

		//We need to do it when port is decided for the host
		setConnectTimeOut(previousHostInfo);
		setProtocol(this.getPort());
	}

	public int getIpConnectCount()
	{
		return ipConnectCount;
	}

	private void setIpConnectCount(HostInfo previousHostInfo)
	{
		if(previousHostInfo != null)
		{
			this.ipConnectCount = previousHostInfo.getIpConnectCount();
		}
	}

	public String getProtocol()
	{
		return protocol;
	}

	private void setProtocol(int portNum)
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

	private void setHost(String host)
	{
		this.host = host;
	}
	
	/*
	 * calculates next ip to hit connect call based on previous host information
	 */
	private void setHost(HostInfo previousHostInfo, boolean isProduction)
	{
		if(!isProduction)
		{
			setHost(STAGING_BROKER_HOST_NAME); //staging host
		}
		else if(previousHostInfo != null && previousHostInfo.getExceptionOnConnect() == ConnectExceptions.DNS_EXCEPTION)
		{
			setHost(getIp());  //connect using ip fallback
		}
		else
		{
			setHost(serverURIs.get(0));  //standerd Domain name => mqtt.im.hike.in/
		}
	}
	


	public int getPort()
	{
		return port;
	}

	private void setPort(int port)
	{
		this.port = port;
	}
	
	private void setPort(HostInfo previousHostInfo, boolean isProduction, boolean isSslOn)
	{
		if(!isProduction)
		{
			 setStagingPort(isSslOn);
		}
		else
		{
			setProductionPort(previousHostInfo, isSslOn);
		}
	}

	private void setStagingPort(boolean isSslOn)
	{
		setPort(isSslOn ? STAGING_BROKER_PORT_NUMBER_SSL : STAGING_BROKER_PORT_NUMBER); //staging ssl/non-ssl scenario
	}

	private void setProductionPort(HostInfo previousHostInfo, boolean isSslOn)
	{
		if(previousHostInfo != null && previousHostInfo.getExceptionOnConnect() == ConnectExceptions.SOCKET_TIME_OUT_EXCEPTION)
		{
			/*
			 * on production for some countries ssl port connection is not allowed at all in these
			 * Countries we cannot use 443 as port fallback.
			 */
			boolean sslPortAllowedAsFallback = Utils.isSSLAllowed();
			if(sslPortAllowedAsFallback)
			{
				setPort(FALLBACK_BROKER_PORT_NUMBER_SSL);
			}
			else
			{
				setPort(MqttConstants.FALLBACK_BROKER_PORT_5222);
			}
		}
		else
		{
			// Standard production 8080 and 443 port in case of non-ssl and ssl respectively.
			setPort(getStanderedProductionPort(isSslOn));
		}
	}
	
	private boolean shouldConnectToFallbackPort(HostInfo previousHostInfo, boolean isSslOn)
	{
		return previousHostInfo != null && previousHostInfo.getExceptionOnConnect() == ConnectExceptions.SOCKET_TIME_OUT_EXCEPTION && !isSslOn;
	}

	private int getStanderedProductionPort(boolean isSslOn)
	{
		return isSslOn ? MqttConstants.PRODUCTION_BROKER_PORT_NUMBER_SSL : MqttConstants.PRODUCTION_BROKER_PORT_NUMBER;
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
	
	private String getIp()
	{
		Random random = new Random();
		int index = random.nextInt(serverURIs.size() - 1) + 1;
		if (ipConnectCount == serverURIs.size())
		{
			ipConnectCount = 0;
			return serverURIs.get(0);
		}
		else
		{
			ipConnectCount++;
			return serverURIs.get(index);
		}
	}
	


}