package com.bsb.hike.modules.httpmgr.client;

import com.bsb.hike.StethoInterceptor;
import com.squareup.okhttp.OkHttpClient;

/**
 * Created by sidharth on 09/05/16.
 */
public class TwinPrimeOkClientCustomDev extends TwinPrimeOkClient
{
    public TwinPrimeOkClientCustomDev(ClientOptions options)
    {
        super(options);
    }

    /**
     * Generates a new OkHttpClient with given clientOption parameters
     *
     * @param clientOptions
     * @return
     */
    protected OkHttpClient generateClient(com.bsb.hike.modules.httpmgr.client.ClientOptions clientOptions)
    {
        OkHttpClient client = super.generateClient(clientOptions);
        client.networkInterceptors().add(new StethoInterceptor());
        return client;
    }
}
