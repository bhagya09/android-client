package com.bsb.hike.modules.httpmgr.requeststate;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A POJO for http request state
 */
public class HttpRequestState
{
	private String requestId;

	private String metadata;

	private JSONObject mdJSON;

	public HttpRequestState(String requestId)
	{
		this.requestId = requestId;
	}

	public String getRequestId()
	{
		return requestId;
	}

	public void setRequestId(String requestId)
	{
		this.requestId = requestId;
	}

	public String getMetadataString()
	{
		return metadata;
	}

	public JSONObject getMetadata()
	{
		return mdJSON;
	}

	public void setMetadata(String metadata) throws JSONException
	{
		this.metadata = metadata;
		this.mdJSON = new JSONObject(metadata);
	}

	public void setMetadata(JSONObject json)
	{
		this.mdJSON = json;
		this.metadata = json.toString();
	}
}
