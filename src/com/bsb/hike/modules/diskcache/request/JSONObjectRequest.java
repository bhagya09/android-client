package com.bsb.hike.modules.diskcache.request;

import org.json.JSONObject;

/**
 * Created by anubhavgupta on 07/12/15.
 */
public class JSONObjectRequest extends CacheRequest{

    private JSONObject dataObject;

    private JSONObjectRequest(Init<?> builder)
    {
        super(builder);
        this.dataObject = builder.dataObject;
    }

    protected static abstract class Init<S extends Init<S>> extends CacheRequest.Init<S>
    {
        private JSONObject dataObject;

        public S setString(JSONObject dataObject)
        {
            this.dataObject = dataObject;
            return self();
        }

        public JSONObjectRequest build()
        {
            return new JSONObjectRequest(this);
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
    public byte[] getData()
    {
        return dataObject.toString().getBytes();
    }
}
