package com.bsb.hike.modules.httpmgr.hikehttp;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.bulkLastSeenUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getStatusBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.lastSeenUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.multiStickerDownloadUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.singleStickerDownloadBase;
import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_HIGH;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_LONG;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_SHORT;

import org.json.JSONObject;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.request.FileRequest;
import com.bsb.hike.modules.httpmgr.interceptor.GzipRequestInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.request.JSONObjectRequest;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.retry.DefaultRetryPolicy;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.retry.IRetryPolicy;
import com.bsb.hike.utils.Utils;
public class HttpRequests
{
	public static RequestToken singleStickerDownloadRequest(String requestId, String stickerId, String categoryId, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(singleStickerDownloadBase() + "?catId=" + categoryId + "&stId=" + stickerId + "&resId=" + Utils.getResolutionId())
				.setId(requestId)
				.setRequestListener(requestListener)
				.build();
		return requestToken;
	}
	
	public static RequestToken multiStickerDownloadRequest(String requestId, IRequestInterceptor interceptor, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(multiStickerDownloadUrl())
				.setId(requestId)
				.post(null) // will set it in interceptor method using request facade
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_LONG)
				.setPriority(PRIORITY_HIGH)
				.build();
		requestToken.getRequestInterceptors().addFirst("sticker", interceptor);
		requestToken.getRequestInterceptors().addAfter("sticker", "gzip", new GzipRequestInterceptor());
		return requestToken;
	}
	
	public static RequestToken LastSeenRequest(String msisdn, IRequestListener requestListener, IRetryPolicy retryPolicy)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(lastSeenUrl() + "/" + msisdn)
				.setRetryPolicy(retryPolicy)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.setPriority(PRIORITY_HIGH)
				.build();
		return requestToken;
	}

	public static RequestToken BulkLastSeenRequest(IRequestListener requestListener, IRetryPolicy retryPolicy)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(bulkLastSeenUrl())
				.setRetryPolicy(retryPolicy)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.build();
		return requestToken;
	}
	
	public static RequestToken postStatusRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getStatusBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(body)
				.build();
		requestToken.getRequestInterceptors().addFirst("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken platformZipDownloadRequest(String filePath, String url, IRequestListener requestListener)
	{

		RequestToken requestToken = new FileRequest.Builder()
				.setUrl(url)
				.setFile(filePath)
				.setRequestListener(requestListener)
				.setRetryPolicy(new DefaultRetryPolicy(3, 1000, 2.0f))
				.build();
		return requestToken;
	}
}
