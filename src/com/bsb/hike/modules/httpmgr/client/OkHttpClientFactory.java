package com.bsb.hike.modules.httpmgr.client;

import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * Created by sidharth on 26/04/16.
 */
public class OkHttpClientFactory
{
    /**
     * Generates a new OkHttpClient with given clientOption parameters
     *
     * @param clientOptions
     * @return
     */
    OkHttpClient generateClient(
            com.bsb.hike.modules.httpmgr.client.ClientOptions clientOptions)
    {
        clientOptions = clientOptions != null ? clientOptions : com.bsb.hike.modules.httpmgr.client.ClientOptions
                .getDefaultClientOptions();
        OkHttpClient client = new OkHttpClient();
        return setClientParameters(client, clientOptions);
    }

    /**
     * Sets Client option parameters to given OkHttpClient
     *
     * @param client
     * @param clientOptions
     * @return
     */
    OkHttpClient setClientParameters(OkHttpClient client, com.bsb.hike.modules.httpmgr.client.ClientOptions clientOptions)
    {
        client.setConnectTimeout(clientOptions.getConnectTimeout(), TimeUnit.MILLISECONDS);
        client.setReadTimeout(clientOptions.getReadTimeout(), TimeUnit.MILLISECONDS);
        client.setWriteTimeout(clientOptions.getWriteTimeout(), TimeUnit.MILLISECONDS);
        client.setWriteTimeout(clientOptions.getWriteTimeout(), TimeUnit.MILLISECONDS);
        client.setSocketFactory(client.getSocketFactory());
        client.setSslSocketFactory(clientOptions.getSslSocketFactory());
        client.setHostnameVerifier(clientOptions.getHostnameVerifier());

        if (clientOptions.getProxy() != null)
        {
            client.setProxy(clientOptions.getProxy());
        }

        if (clientOptions.getProxySelector() != null)
        {
            client.setProxySelector(clientOptions.getProxySelector());
        }

        if (clientOptions.getProxySelector() != null)
        {
            client.setProxySelector(clientOptions.getProxySelector());
        }

        if (clientOptions.getCookieHandler() != null)
        {
            client.setCookieHandler(clientOptions.getCookieHandler());
        }

        if (clientOptions.getCache() != null)
        {
            client.setCache(clientOptions.getCache());
        }

        if (clientOptions.getCache() != null)
        {
            client.setCache(clientOptions.getCache());
        }

        if (clientOptions.getHostnameVerifier() != null)
        {
            client.setHostnameVerifier(clientOptions.getHostnameVerifier());
        }

        if (clientOptions.getCertificatePinner() != null)
        {
            client.setCertificatePinner(clientOptions.getCertificatePinner());
        }

        if (clientOptions.getAuthenticator() != null)
        {
            client.setAuthenticator(clientOptions.getAuthenticator());
        }

        if (clientOptions.getProtocols() != null)
        {
            client.setProtocols(clientOptions.getProtocols());
        }

        if (clientOptions.getConnectionSpecs() != null)
        {
            client.setConnectionSpecs(clientOptions.getConnectionSpecs());
        }

        if (clientOptions.getDispatcher() != null)
        {
            client.setDispatcher(clientOptions.getDispatcher());
        }

        if (clientOptions.getConnectionPool() != null)
        {
            client.setConnectionPool(clientOptions.getConnectionPool());
        }

        client.setFollowSslRedirects(clientOptions.getFollowSslRedirects());
        client.setFollowRedirects(clientOptions.getFollowRedirects());

        return client;
    }
}