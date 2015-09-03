package com.bsb.hike.modules.httpmgr.client;

import java.net.CookieHandler;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.HttpManager;
import com.bsb.hike.modules.httpmgr.hikehttp.hostnameverifier.HikeHostNameVerifier;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.CertificatePinner;
import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.Util;

/**
 * This class represents the client options used for executing a request, if caller does not specify client options then default client options are used for executing a request
 * {@see Defaults}
 * 
 * @author anubhav & sidharth
 * 
 */
public class ClientOptions
{
	final int connectTimeout;

	final int readTimeout;

	final int writeTimeout;

	final Proxy proxy;

	final ProxySelector proxySelector;

	final CookieHandler cookieHandler;

	final SocketFactory socketFactory;

	final SSLSocketFactory sslSocketFactory;

	final Cache cache;

	final Authenticator authenticator;

	final CertificatePinner certificatePinner;

	final HostnameVerifier hostnameVerifier;

	final List<ConnectionSpec> connectionSpecs;

	final List<Protocol> protocols;

	final Dispatcher dispatcher;

	final ConnectionPool connectionPool;

	boolean followSslRedirects = true;

	boolean followRedirects = true;

	public ClientOptions(Builder builder)
	{
		this.connectTimeout = builder.connectTimeout;
		this.readTimeout = builder.readTimeout;
		this.writeTimeout = builder.writeTimeout;
		this.proxy = builder.proxy;
		this.proxySelector = builder.proxySelector;
		this.cookieHandler = builder.cookieHandler;
		this.socketFactory = builder.socketFactory;
		this.sslSocketFactory = builder.sslSocketFactory;
		this.cache = builder.cache;
		this.hostnameVerifier = builder.hostnameVerifier;
		this.certificatePinner = builder.certificatePinner;
		this.authenticator = builder.authenticator;
		this.protocols = builder.protocols;
		this.connectionSpecs = builder.connectionSpecs;
		this.dispatcher = builder.dispatcher;
		this.connectionPool = builder.connectionPool;
		this.followSslRedirects = builder.followSslRedirects;
		this.followRedirects = builder.followRedirects;
	}

	/** connect timeout (in milliseconds). */
	public final int getConnectTimeout()
	{
		return connectTimeout;
	}

	/** read timeout (in milliseconds). */
	public final int getReadTimeout()
	{
		return readTimeout;
	}

	/** write timeout (in milliseconds). */
	public final int getWriteTimeout()
	{
		return writeTimeout;
	}

	public final Proxy getProxy()
	{
		return proxy;
	}

	public final ProxySelector getProxySelector()
	{
		return proxySelector;
	}

	public final CookieHandler getCookieHandler()
	{
		return cookieHandler;
	}

	public final Cache getCache()
	{
		return cache;
	}

	public final SocketFactory getSocketFactory()
	{
		return socketFactory;
	}

	public final SSLSocketFactory getSslSocketFactory()
	{
		return sslSocketFactory;
	}

	public final HostnameVerifier getHostnameVerifier()
	{
		return hostnameVerifier;
	}

	public final CertificatePinner getCertificatePinner()
	{
		return certificatePinner;
	}

	public final Authenticator getAuthenticator()
	{
		return authenticator;
	}

	public final List<Protocol> getProtocols()
	{
		return protocols;
	}

	public final List<ConnectionSpec> getConnectionSpecs()
	{
		return connectionSpecs;
	}

	public final Dispatcher getDispatcher()
	{
		return dispatcher;
	}

	public final ConnectionPool getConnectionPool()
	{
		return connectionPool;
	}

	public final boolean getFollowSslRedirects()
	{
		return followSslRedirects;
	}

	public final boolean getFollowRedirects()
	{
		return followRedirects;
	}

	public static class Builder
	{
		private int connectTimeout = -1;

		private int readTimeout = -1;

		private int writeTimeout = -1;

		private Proxy proxy;

		private ProxySelector proxySelector;

		private CookieHandler cookieHandler;

		private Cache cache;

		private SocketFactory socketFactory;

		private SSLSocketFactory sslSocketFactory;

		private HostnameVerifier hostnameVerifier;

		private CertificatePinner certificatePinner;

		private Authenticator authenticator;

		private ConnectionPool connectionPool;

		private boolean followSslRedirects;

		private boolean followRedirects;

		private Dispatcher dispatcher;

		private List<Protocol> protocols;

		private List<ConnectionSpec> connectionSpecs;

		/**
		 * Sets the default connect timeout for new connections. A value of 0 means no timeout.
		 * 
		 * @see URLConnection#setConnectTimeout(int)
		 */
		public final Builder setConnectTimeout(long timeout, TimeUnit unit)
		{
			if (timeout < 0)
				throw new IllegalArgumentException("timeout < 0");
			if (unit == null)
				throw new IllegalArgumentException("unit == null");
			long millis = unit.toMillis(timeout);
			if (millis > Integer.MAX_VALUE)
				throw new IllegalArgumentException("Timeout too large.");
			connectTimeout = (int) millis;

			return this;
		}

		/**
		 * Sets the default read timeout for new connections. A value of 0 means no timeout.
		 * 
		 * @see URLConnection#setReadTimeout(int)
		 */
		public final Builder setReadTimeout(long timeout, TimeUnit unit)
		{
			if (timeout < 0)
				throw new IllegalArgumentException("timeout < 0");
			if (unit == null)
				throw new IllegalArgumentException("unit == null");
			long millis = unit.toMillis(timeout);
			if (millis > Integer.MAX_VALUE)
				throw new IllegalArgumentException("Timeout too large.");
			readTimeout = (int) millis;

			return this;
		}

		/**
		 * Sets the default write timeout for new connections. A value of 0 means no timeout.
		 */
		public final Builder setWriteTimeout(long timeout, TimeUnit unit)
		{
			if (timeout < 0)
				throw new IllegalArgumentException("timeout < 0");
			if (unit == null)
				throw new IllegalArgumentException("unit == null");
			long millis = unit.toMillis(timeout);
			if (millis > Integer.MAX_VALUE)
				throw new IllegalArgumentException("Timeout too large.");
			writeTimeout = (int) millis;

			return this;
		}

		/**
		 * Sets the HTTP proxy that will be used by connections created by this client. This takes precedence over {@link #setProxySelector}, which is only honored when this proxy
		 * is null (which it is by default). To disable proxy use completely, call {@code setProxy(Proxy.NO_PROXY)}.
		 */
		public final Builder setProxy(Proxy proxy)
		{
			this.proxy = proxy;
			return this;
		}

		/**
		 * Sets the proxy selection policy to be used if no {@link #setProxy proxy} is specified explicitly. The proxy selector may return multiple proxies; in that case they will
		 * be tried in sequence until a successful connection is established.
		 * 
		 * <p>
		 * If unset, the {@link ProxySelector#getDefault() system-wide default} proxy selector will be used.
		 */
		public final Builder setProxySelector(ProxySelector proxySelector)
		{
			this.proxySelector = proxySelector;
			return this;
		}

		/**
		 * Sets the cookie handler to be used to read outgoing cookies and write incoming cookies.
		 * 
		 * <p>
		 * If unset, the {@link CookieHandler#getDefault() system-wide default} cookie handler will be used.
		 */
		public final Builder setCookieHandler(CookieHandler cookieHandler)
		{
			this.cookieHandler = cookieHandler;
			return this;
		}

		public final Builder setCache(Cache cache)
		{
			this.cache = cache;
			return this;
		}

		/**
		 * Sets the socket factory used to create connections.
		 * 
		 * <p>
		 * If unset, the {@link SocketFactory#getDefault() system-wide default} socket factory will be used.
		 */
		public final Builder setSocketFactory(SocketFactory socketFactory)
		{
			this.socketFactory = socketFactory;
			return this;
		}

		/**
		 * Sets the socket factory used to secure HTTPS connections.
		 * 
		 * <p>
		 * If unset, a lazily created SSL socket factory will be used.
		 */
		public final Builder setSslSocketFactory(SSLSocketFactory sslSocketFactory)
		{
			this.sslSocketFactory = sslSocketFactory;
			return this;
		}

		/**
		 * Sets the verifier used to confirm that response certificates apply to requested hostnames for HTTPS connections.
		 * 
		 * <p>
		 * If unset, a default hostname verifier will be used.
		 */
		public final Builder setHostnameVerifier(HostnameVerifier hostnameVerifier)
		{
			this.hostnameVerifier = hostnameVerifier;
			return this;
		}

		/**
		 * <p>
		 * <b>Only for OkHttp Client </b>
		 * </p>
		 * Sets the certificate pinner that constrains which certificates are trusted. By default HTTPS connections rely on only the {@link #setSslSocketFactory SSL socket factory}
		 * to establish trust. Pinning certificates avoids the need to trust certificate authorities.
		 */
		public final Builder setCertificatePinner(CertificatePinner certificatePinner)
		{
			this.certificatePinner = certificatePinner;
			return this;
		}

		/**
		 * <p>
		 * <b>Only for OkHttp Client </b>
		 * </p>
		 * Sets the authenticator used to respond to challenges from the remote web server or proxy server.
		 * 
		 * <p>
		 * If unset, the {@link java.net.Authenticator#setDefault system-wide default} authenticator will be used.
		 */
		public final Builder setAuthenticator(Authenticator authenticator)
		{
			this.authenticator = authenticator;
			return this;
		}

		/**
		 * <p>
		 * <b>Only for OkHttp Client </b>
		 * </p>
		 * Sets the connection pool used to recycle HTTP and HTTPS connections.
		 * 
		 * <p>
		 * If unset, the {@link ConnectionPool#getDefault() system-wide default} connection pool will be used.
		 */
		public final Builder setConnectionPool(ConnectionPool connectionPool)
		{
			this.connectionPool = connectionPool;
			return this;
		}

		/**
		 * <p>
		 * <b>Only for OkHttp Client </b>
		 * </p>
		 * Configure this client to follow redirects from HTTPS to HTTP and from HTTP to HTTPS.
		 * 
		 * <p>
		 * If unset, protocol redirects will be followed. This is different than the built-in {@code HttpURLConnection}'s default.
		 */
		public final Builder setFollowSslRedirects(boolean followProtocolRedirects)
		{
			this.followSslRedirects = followProtocolRedirects;
			return this;
		}

		/**
		 * <p>
		 * <b>Only for OkHttp Client </b>
		 * </p>
		 * Configure this client to follow redirects. If unset, redirects be followed.
		 */
		public final void setFollowRedirects(boolean followRedirects)
		{
			this.followRedirects = followRedirects;
		}

		/**
		 * <p>
		 * <b>Only for OkHttp Client </b>
		 * </p>
		 * Sets the dispatcher used to set policy and execute asynchronous requests. Must not be null.
		 */
		public final Builder setDispatcher(Dispatcher dispatcher)
		{
			if (dispatcher == null)
				throw new IllegalArgumentException("dispatcher == null");
			this.dispatcher = dispatcher;
			return this;
		}

		/**
		 * <p>
		 * <b>Only for OkHttp Client </b>
		 * </p>
		 * Configure the protocols used by this client to communicate with remote servers. By default this client will prefer the most efficient transport available, falling back
		 * to more ubiquitous protocols. Applications should only call this method to avoid specific compatibility problems, such as web servers that behave incorrectly when SPDY
		 * is enabled.
		 * 
		 * <p>
		 * The following protocols are currently supported:
		 * <ul>
		 * <li><a href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">http/1.1</a>
		 * <li><a href="http://www.chromium.org/spdy/spdy-protocol/spdy-protocol-draft3-1">spdy/3.1</a>
		 * <li><a href="http://tools.ietf.org/html/draft-ietf-httpbis-http2-16">h2-16</a>
		 * </ul>
		 * 
		 * <p>
		 * <strong>This is an evolving set.</strong> Future releases may drop support for transitional protocols (like h2-16), in favor of their successors (h2). The http/1.1
		 * transport will never be dropped.
		 * 
		 * <p>
		 * If multiple protocols are specified, <a href="https://technotes.googlecode.com/git/nextprotoneg.html">NPN</a> or <a
		 * href="http://tools.ietf.org/html/draft-ietf-tls-applayerprotoneg">ALPN</a> will be used to negotiate a transport.
		 * 
		 * <p>
		 * {@link Protocol#HTTP_1_0} is not supported in this set. Requests are initiated with {@code HTTP/1.1} only. If the server responds with {@code HTTP/1.0}, that will be
		 * exposed by {@link Response#protocol()}.
		 * 
		 * @param protocols
		 *            the protocols to use, in order of preference. The list must contain {@link Protocol#HTTP_1_1}. It must not contain null or {@link Protocol#HTTP_1_0}.
		 */
		public final Builder setProtocols(List<Protocol> protocols)
		{
			protocols = Util.immutableList(protocols);
			if (!protocols.contains(Protocol.HTTP_1_1))
			{
				throw new IllegalArgumentException("protocols doesn't contain http/1.1: " + protocols);
			}
			if (protocols.contains(Protocol.HTTP_1_0))
			{
				throw new IllegalArgumentException("protocols must not contain http/1.0: " + protocols);
			}
			if (protocols.contains(null))
			{
				throw new IllegalArgumentException("protocols must not contain null");
			}
			this.protocols = Util.immutableList(protocols);
			return this;
		}

		/**
		 * <p>
		 * <b>Only for OkHttp Client </b>
		 * </p>
		 * 
		 * @param connectionSpecs
		 * @return
		 */
		public final Builder setConnectionSpecs(List<ConnectionSpec> connectionSpecs)
		{
			this.connectionSpecs = Util.immutableList(connectionSpecs);
			return this;
		}

		/** Create the {@link ClientOptions} instances. */
		public ClientOptions build()
		{
			ensureSaneDefaults();
			return new ClientOptions(this);
		}

		private void ensureSaneDefaults()
		{
			if (connectTimeout < 0)
			{
				connectTimeout = Defaults.CONNECT_TIMEOUT_MILLIS;
			}

			if (readTimeout < 0)
			{
				connectTimeout = Defaults.READ_TIMEOUT_MILLIS;
			}

			if (writeTimeout < 0)
			{
				writeTimeout = Defaults.WRITE_TIMEOUT_MILLIS;
			}

			if (socketFactory == null)
			{
				socketFactory = Defaults.SOCKET_FACTORY;
			}

			if (sslSocketFactory == null)
			{
				sslSocketFactory = Defaults.SSL_SOCKET_FACTORY;
			}

		}
	}

	/**
	 * Returns clientoption with default values set
	 * 
	 * @return
	 */
	static ClientOptions getDefaultClientOptions()
	{
		int connectTimeout = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.OKHTTP_CONNECT_TIMEOUT, Defaults.CONNECT_TIMEOUT_MILLIS);
		int readTimeout = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.OKHTTP_READ_TIMEOUT, Defaults.READ_TIMEOUT_MILLIS);
		int writeTimeout = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.OKHTTP_WRITE_TIMEOUT, Defaults.WRITE_TIMEOUT_MILLIS);

		HikeHostNameVerifier hostVerifier = new HikeHostNameVerifier();
		hostVerifier.setProdHostIps(HttpManager.getProductionHostUris());
		hostVerifier.setPlatformHostIps(HttpManager.getPlatformProductionHostUris());
		LogFull.d("Connect Timeout : " + connectTimeout + "\n Read timeout : " + readTimeout + "\n Write timeout : " + writeTimeout);

		ClientOptions defaultClientOptions = new ClientOptions.Builder()
				.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
				.setReadTimeout(readTimeout, TimeUnit.MILLISECONDS)
				.setWriteTimeout(writeTimeout, TimeUnit.MILLISECONDS)
				.setSocketFactory(Defaults.SOCKET_FACTORY)
				.setSslSocketFactory(Defaults.SSL_SOCKET_FACTORY)
				.setHostnameVerifier(hostVerifier)
				.build();
		return defaultClientOptions;
	}
}
