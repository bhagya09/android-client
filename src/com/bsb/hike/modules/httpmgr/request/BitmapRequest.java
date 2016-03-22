package com.bsb.hike.modules.httpmgr.request;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.bsb.hike.modules.httpmgr.RequestToken;

import java.io.IOException;
import java.io.InputStream;

/**
 * BitmapRequest is used to return response in form of Bitmap to the request listener. InputStream to Bitmap is done in {@link Request#parseResponse(InputStream)}
 */
public class BitmapRequest extends Request<Bitmap>
{
    protected BitmapRequest(Init<?> init)
    {
        super(init);
    }

    protected static abstract class Init<S extends Init<S>> extends Request.Init<S>
    {
        public RequestToken build()
        {
            BitmapRequest request = new BitmapRequest(this);
            RequestToken token = new RequestToken(request);
            return token;
        }
    }

    public static class Builder extends Init<Builder>
    {
        @Override
        protected Builder self()
        {
            return this;
        }
    }

    @Override
    public Bitmap parseResponse(InputStream is, int contentLength) throws IOException
    {
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        return bitmap;
    }
}
