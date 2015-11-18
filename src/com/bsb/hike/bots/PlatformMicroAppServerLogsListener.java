package com.bsb.hike.bots;

import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;

/**
 * Created by pushkargupta on 14/11/15.
 */
public class PlatformMicroAppServerLogsListener implements IRequestListener
{
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }


    @Override

    public void onRequestFailure(HttpException httpException)
    {
        Logger.e("JavascriptBridge", "Unable to log to server  " + httpException.getMessage());
    }

    @Override
    public void onRequestSuccess(Response result)
    {
        Logger.d("JavascriptBridge", "Succesfully logged to server" + result.getStatusCode());
    }

    @Override
    public void onRequestProgressUpdate(float progress)
    {

    }
}
