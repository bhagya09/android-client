package com.bsb.hike.chatthemes;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.chatThemeAssetsDownloadBase;
import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_HIGH;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_SHORT;

import org.json.JSONObject;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.JSONObjectRequest;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.utils.Utils;

/**
 * Created by sriram on 24/02/16.
 */
public class ChatThemeHttpRequestHelper extends HttpRequests
{
	public static RequestToken downloadChatThemeAssets(String requestId, JSONObject data, IRequestListener requestListener)
	{
		IRequestBody body = new JsonBody(data);
		RequestToken requestToken = new JSONObjectRequest.Builder().setUrl(chatThemeAssetsDownloadBase() + "?resId=" + Utils.getResolutionId()).setId(requestId).post(body)
				.setRequestListener(requestListener).setRequestType(REQUEST_TYPE_SHORT).setPriority(PRIORITY_HIGH).build();
		return requestToken;
	}
}
