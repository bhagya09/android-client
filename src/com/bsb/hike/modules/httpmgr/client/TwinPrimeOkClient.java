package com.bsb.hike.modules.httpmgr.client;

import com.squareup.okhttp.OkHttpClient;

/**
 * Created by himanshu on 28/03/16.
 */
public class TwinPrimeOkClient extends OkClient {

    protected OkHttpClient generateClient(ClientOptions clientOptions)
    {
        clientOptions = clientOptions != null ? clientOptions : ClientOptions.getDefaultClientOptions();
        OkHttpClient client = new OkHttpClient();
        addLogging(client);
        return setClientParameters(client, clientOptions);
    }
}
