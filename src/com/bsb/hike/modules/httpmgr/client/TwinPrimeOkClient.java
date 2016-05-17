package com.bsb.hike.modules.httpmgr.client;

import com.squareup.okhttp.OkHttpClient;
import com.twinprime.TwinPrimeSDK.TPOkHttpClient;

/**
 * Created by himanshu on 28/03/16.
 */
public class TwinPrimeOkClient extends OkClient {

    public TwinPrimeOkClient(ClientOptions options) {
        super(options);
    }

    protected OkHttpClient generateClient(ClientOptions clientOptions)
    {
        clientOptions = clientOptions != null ? clientOptions : ClientOptions.getDefaultClientOptions();
        OkHttpClient client = new TPOkHttpClient();
        addLogging(client);
        return new OkHttpClientFactory().setClientParameters(client, clientOptions);
    }


}
