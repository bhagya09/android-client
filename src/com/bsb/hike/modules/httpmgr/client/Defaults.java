package com.bsb.hike.modules.httpmgr.client;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.bsb.hike.utils.HikeSSLUtil;

/**
 * This class contains client options related constants
 * 
 * @author anubhav & sidharth
 * 
 */
final class Defaults
{
	/** The timeout in making the initial connection i.e. completing the TCP connection handshake */
	static final int CONNECT_TIMEOUT_MILLIS = 30 * 1000; // 30s

	/** Specifies the amount of time, in seconds, the HTTP transport channel waits for a read request to complete on a socket after the previous read request occurs. */
	static final int READ_TIMEOUT_MILLIS = 60 * 1000; // 60s

	/** Specifies the amount of time, in seconds, that the HTTP transport channel waits on a socket for each portion of response data to be transmitted. */
	static final int WRITE_TIMEOUT_MILLIS = 60 * 1000; // 60s

	/** Default socket factory for creating sockets */
	static final SocketFactory SOCKET_FACTORY = SocketFactory.getDefault();

	/** Default ssl socket factory for creating ssl sockets */
	static final SSLSocketFactory SSL_SOCKET_FACTORY = HikeSSLUtil.getSSLSocketFactory();

	private Defaults()
	{
		// No instances.
	}
}
