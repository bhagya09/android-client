package com.bsb.hike;

import static com.bsb.hike.MqttConstants.*;

import java.util.List;
import java.util.Random;

import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
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
		NO_EXCEPTION, DNS_EXCEPTION, SOCKET_TIME_OUT_EXCEPTION, OTHER
	}

	private String protocol;

	private String host;

	private int port;

	private int connectTimeOut;

	private int connectRetryCount = 0;
	
	List<String> serverURIs = null;

	private ConnectExceptions exceptionOnConnect = ConnectExceptions.NO_EXCEPTION;
	
	public HostInfo(HostInfo previousHostInfo, List<String> serverURIs)
	{
		boolean isSslOn = Utils.switchSSLOn(HikeMessengerApp.getInstance());
		boolean isProduction = Utils.isOnProduction();
		
		this.serverURIs = serverURIs;
		
		setConnectRetryCount(previousHostInfo);
		this.setHost(previousHostInfo, isProduction);
		setPort(previousHostInfo, isProduction, isSslOn);

		//We need to do it when port is decided for the host
		setConnectTimeOut(previousHostInfo, this.getPort(), isSslOn);
		setProtocol(this.getPort());
	}

	private int getConnectRetryCount()
	{
		return connectRetryCount;
	}
	
	private void setConnectRetryCount(HostInfo previousHostInfo)
	{
		if(previousHostInfo != null)
		{
			this.connectRetryCount = previousHostInfo.getConnectRetryCount();
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
			setStagingHost();
		}
		else if(previousHostInfo != null)
		{
			if(previousHostInfo.getExceptionOnConnect() == ConnectExceptions.DNS_EXCEPTION)
			{
				setHost(getIp());  //connect using ip fallback
			}
			else
			{
				setHost(previousHostInfo.getHost());  //otherwise we should try on host as last try
			}
		}
		else
		{
			setHost(serverURIs.get(0));  //standerd Domain name => mqtt.im.hike.in/
		}
	}
	
	private void setStagingHost()
	{
		int whichServer = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PRODUCTION_HOST_TOGGLE, AccountUtils._STAGING_HOST);
		if (whichServer == AccountUtils._CUSTOM_HOST)
		{
			setHost( HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.CUSTOM_MQTT_HOST, STAGING_BROKER_HOST_NAME) );
		}
		else
		{
			setHost(STAGING_BROKER_HOST_NAME); //staging host
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
		int whichServer = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.PRODUCTION_HOST_TOGGLE, AccountUtils._STAGING_HOST);
		if (whichServer == AccountUtils._CUSTOM_HOST)
		{
			setPort( HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.CUSTOM_MQTT_PORT, STAGING_BROKER_PORT_NUMBER) );
		}
		else
		{
			setPort(isSslOn ? STAGING_BROKER_PORT_NUMBER_SSL : STAGING_BROKER_PORT_NUMBER); //staging ssl/non-ssl scenario
		}
	}

	private void setProductionPort(HostInfo previousHostInfo, boolean isSslOn)
	{
		if(previousHostInfo != null)
		{
			if(isSslOn)
			{
				//if ssl is on(plus on wifi) than we always try on 443
				setPort(getStanderedProductionPort(isSslOn));
			}
			// ssl off and we got an socket timeout than we go to fallback ports
			else if (previousHostInfo.getExceptionOnConnect() == ConnectExceptions.SOCKET_TIME_OUT_EXCEPTION )
			{
				setNextFallBackPort(previousHostInfo);
			}
			else if (previousHostInfo.getExceptionOnConnect() == ConnectExceptions.OTHER )
			{
				setNextFallBackPort(previousHostInfo);
			}
			else
			{
				//otherwise we should try on same port as last try
				setPort(previousHostInfo.getPort());
			}
				
		}
		else
		{
			// Standard production 8080 and 443 port in case of non-ssl and ssl respectively.
			setPort(getStanderedProductionPort(isSslOn));
		}
	}
	
	private void setNextFallBackPort(HostInfo previousHostInfo)
	{
		/*
		 * on production for some countries ssl port connection is not allowed at all in these Countries we cannot use 443 as port fallback.
		 * Logic is 
		 * 1. 5222 -- fallback to --> 8080
		 * 2. 8080 -- fallback to --> 443 if ssl is allowed
		 * 3. again fallback to 5222 -- if tried above
		 */
		boolean sslPortAllowedAsFallback = Utils.isSSLAllowed();
		if(previousHostInfo.getPort() == PRODUCTION_MQTT_CONNECT_PORTS[0])
		{
			setPort(PRODUCTION_MQTT_CONNECT_PORTS[1]);
		}
		else if(previousHostInfo.getPort() == PRODUCTION_MQTT_CONNECT_PORTS[1]  && sslPortAllowedAsFallback)
		{
			setPort(MqttConstants.FALLBACK_BROKER_PORT_NUMBER_SSL);
		}
		else 
		{
			setPort(PRODUCTION_MQTT_CONNECT_PORTS[0]);
		}	
	}
	
	private int getStanderedProductionPort(boolean isSslOn)
	{
		return isSslOn ? MqttConstants.PRODUCTION_BROKER_PORT_NUMBER_SSL : HikeSharedPreferenceUtil.getInstance().getData(MqttConstants.LAST_MQTT_CONNECT_PORT, PRODUCTION_MQTT_CONNECT_PORTS[0]);
	}

	public int getConnectTimeOut()
	{
		return connectTimeOut;
	}

	private void setConnectTimeOut(int connectTimeOut)
	{
		this.connectTimeOut = connectTimeOut;
	}

	public void setConnectTimeOut(HostInfo previousHostInfo, int currentPortNum, boolean isSslOn)
	{
		/*
		 * if it is not null ==> last connect try was a failure ==> we just need to increment time
		 * in case of socketTimeOutException.  
		 */
		if (previousHostInfo != null)
		{
			/*
			 * We would need to increment connect timeout if
			 * 1. Last connect call resulted into socket timeout
			 */
			if (previousHostInfo.getExceptionOnConnect() == ConnectExceptions.SOCKET_TIME_OUT_EXCEPTION)
			{
				incrementConnectRetryCount();
			}
		}
		else
		{
			// if it is null ==> this is a fresh try ==> we need to start from last successful connect
			// try time
			long timeTakenInLastSuccessfullConnect = HikeSharedPreferenceUtil.getInstance().getData(MqttConstants.TIME_TAKEN_IN_LAST_SOCKET_CONNECT, (long) (CONNECTION_TIMEOUT_SECONDS[0]));
			
			/*
			 * current logic is to increment connect timeout every time by 4
			 * and timeTakenInLastSuccessfullConnect is in milliseconds So
			 * devide it by 4000. 
			 */
			int index = (int) (timeTakenInLastSuccessfullConnect/4000) ;
			
			index = index -1;
			
			if(index < 0)
			{
				index = 0;
			}
			else if (index >= CONNECTION_TIMEOUT_SECONDS.length)
			{
				index = CONNECTION_TIMEOUT_SECONDS.length - 1;
			}
			
			connectRetryCount = index;
		}

		int connectTimeOut = CONNECTION_TIMEOUT_SECONDS[Math.min(CONNECTION_TIMEOUT_SECONDS.length - 1, connectRetryCount)];
		setConnectTimeOut(connectTimeOut);
		
		Logger.e(this.getClass().getSimpleName(), "connectTimeOut " + connectTimeOut);
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
		return serverURIs.get(index);
	}
	
	public String toString()
	{
		return " Protocol : "+protocol + " Host : " + host + " Port : "+ port + " connectTimeOut : " + connectTimeOut 
				+ " connectRetryCount : "+ connectRetryCount  + " exceptionOnConnect : "+exceptionOnConnect;
	}

}