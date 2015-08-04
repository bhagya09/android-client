/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.SocketFactory;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.bsb.hike.MqttConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

/**
 * A network module for connecting over TCP.
 */
public class TCPNetworkModule implements NetworkModule
{
	protected Socket socket;

	protected SocketFactory factory;

	protected String host;

	protected int port;

	private int conTimeout;

	final static String className = TCPNetworkModule.class.getName();

	private final String TAG = "TCPNETWORKMODULE";

	/**
	 * Constructs a new TCPNetworkModule using the specified host and port. The supplied SocketFactory is used to supply the network socket.
	 */
	public TCPNetworkModule(SocketFactory factory, String host, int port, String resourceContext)
	{
		this.factory = factory;
		this.host = host;
		this.port = port;

	}

	/**
	 * Starts the module, by creating a TCP socket to the server.
	 */
	public void start() throws IOException, MqttException
	{
		final String methodName = "start";
		try
		{
			Logger.d(TAG, "Trying to connect on host : "+host + " and port :"+port);
			// InetAddress localAddr = InetAddress.getLocalHost();
			// socket = factory.createSocket(host, port, localAddr, 0);
			// @TRACE 252=connect to host {0} port {1} timeout {2}

			long dnsStartTime = System.currentTimeMillis();
			SocketAddress sockaddr = new InetSocketAddress(host, port);
			long dnsEndTime = System.currentTimeMillis();
			Logger.d(TAG, "DNS resolved : timeTaken in dns call : "+(dnsEndTime - dnsStartTime));
			
			socket = factory.createSocket();
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(6 * 60 * 1000); // setting socket timeout to 6 mins
			
			long sTime = System.currentTimeMillis();
			socket.connect(sockaddr, conTimeout * 1000);
			long eTime = System.currentTimeMillis();
			
			HikeSharedPreferenceUtil.getInstance().saveData(MqttConstants.TIME_TAKEN_IN_LAST_SOCKET_CONNECT, (eTime - sTime));
			
			Logger.d(TAG, "Connected : saving TIME_TAKEN_IN_LAST_SOCKET_CONNECT : "+(eTime - sTime));

			// SetTcpNoDelay was originally set ot true disabling Nagle's algorithm.
			// This should not be required.
			// socket.setTcpNoDelay(true); // TCP_NODELAY on, which means we do not use Nagle's algorithm
		}
		catch (ConnectException ex)
		{
			// @TRACE 250=Failed to create TCP socket
			Logger.e(TAG, "failed to create TCP Socket", ex);
			throw new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR, ex);
		}
	}

	public InputStream getInputStream() throws IOException
	{
		return socket.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException
	{
		return socket.getOutputStream();
	}

	public Socket getSocket()
	{
		return socket;
	}

	/**
	 * Stops the module, by closing the TCP socket.
	 */
	public void stop() throws IOException
	{
		try
		{
			if (socket != null)
			{
				if (socket.getOutputStream() != null)
				{
					socket.shutdownOutput();
				}
				if (socket.getInputStream() != null)
				{
					socket.shutdownInput();
				}
				socket.close();
			}
		}
		catch (Exception e)
		{
			Logger.e(TAG, "exception while trying to stop network module", e);
			socket.close();
		}
	}

	/**
	 * Set the maximum time to wait for a socket to be established
	 * 
	 * @param timeout
	 */
	public void setConnectTimeout(int timeout)
	{
		this.conTimeout = timeout;
	}
}
