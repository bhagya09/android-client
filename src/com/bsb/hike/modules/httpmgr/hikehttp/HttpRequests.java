package com.bsb.hike.modules.httpmgr.hikehttp;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.authSDKBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.bulkLastSeenUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.deleteAccountBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.editProfileAvatarBase;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getAvatarBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getGroupBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getHikeJoinTimeBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getStaticAvatarBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getStatusBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.groupProfileBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.lastSeenUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.multiStickerDownloadUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.postAddressbookBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.postDeviceDetailsBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.postGreenBlueDetailsBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.preActivationBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.registerAccountBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.sendDeviceDetailBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.sendUserLogsInfoBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.setProfileUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.singleStickerDownloadBase;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.unlinkAccountBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.updateAddressbookBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.validateNumberBaseUrl;
import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_HIGH;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_LONG;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_SHORT;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bsb.hike.platform.PlatformUtils;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants;
import com.bsb.hike.modules.httpmgr.interceptor.GzipRequestInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.request.FileRequest;
import com.bsb.hike.modules.httpmgr.request.JSONArrayRequest;
import com.bsb.hike.modules.httpmgr.request.JSONObjectRequest;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.FileBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.retry.DefaultRetryPolicy;
import com.bsb.hike.modules.httpmgr.retry.IRetryPolicy;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;

public class HttpRequests
{
	public static RequestToken singleStickerDownloadRequest(String requestId, String stickerId, String categoryId, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(singleStickerDownloadBase() + "?catId=" + categoryId + "&stId=" + stickerId + "&resId=" + Utils.getResolutionId())
				.setId(requestId)
				.setRequestListener(requestListener)
				.setAnalyticsParam(HttpAnalyticsConstants.HTTP_SINGLE_STICKER_DOWNLOAD_ANALYTICS_PARAM)
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
				.setAnalyticsParam(HttpAnalyticsConstants.HTTP_MULTI_STICKER_DOWNLOAD_ANALYTICS_PARAM)
				.build();
		requestToken.getRequestInterceptors().addFirst("sticker", interceptor);
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
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
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken platformZipDownloadRequest(String filePath, String url, IRequestListener requestListener)
	{
		RequestToken requestToken = new FileRequest.Builder()
				.setUrl(url)
				.setFile(filePath)
				.setRequestListener(requestListener)
				.setRetryPolicy(new DefaultRetryPolicy(HikePlatformConstants.numberOfRetries, HikePlatformConstants.retryDelay, HikePlatformConstants.backOffMultiplier))
				.build();
		return requestToken;
	}

	public static RequestToken getPlatformUserIdForFullAddressBookFetchRequest(String url, IRequestListener requestListener, List<Header> headers)
	{
		RequestToken requestToken = new JSONArrayRequest.Builder()
				.setUrl(url)
				.setRetryPolicy(new DefaultRetryPolicy(HikePlatformConstants.numberOfRetries, HikePlatformConstants.retryDelay, HikePlatformConstants.backOffMultiplier))
				.setRequestListener(requestListener)
				.setHeaders(headers)
				.setRequestType(REQUEST_TYPE_LONG)
				.build();

		return requestToken;
	}

	public static RequestToken postPlatformUserIdForPartialAddressBookFetchRequest(String url,JSONObject json, IRequestListener requestListener, List<Header> headers)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONArrayRequest.Builder()
				.setUrl(url)
				.post(body)
				.setRetryPolicy(new DefaultRetryPolicy(HikePlatformConstants.numberOfRetries, HikePlatformConstants.retryDelay, HikePlatformConstants.backOffMultiplier))
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_LONG)
				.setHeaders(headers)
				.build();

		return requestToken;
	}

	public static RequestToken postPlatformUserIdFetchRequest(String url, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.post(null)
				.setRetryPolicy(new DefaultRetryPolicy(HikePlatformConstants.numberOfRetries, HikePlatformConstants.retryDelay, HikePlatformConstants.backOffMultiplier))
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.build();

		return requestToken;
	}

	public static RequestToken sendDeviceDetailsRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		
		String md5hash = HttpUtils.calculateMD5hash(body.getBytes());
		Header header = new Header(HttpHeaderConstants.CONTENT_MD5, md5hash);
		
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(sendDeviceDetailBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.addHeader(header)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}
	
	public static RequestToken sendPreActivationRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(preActivationBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken registerAccountRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(registerAccountBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.setAsynchronous(false)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken validateNumberRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(validateNumberBaseUrl() + "?digits=4")
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.setAsynchronous(false)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken setProfileRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(setProfileUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.setAsynchronous(false)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken postAddressBookRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(postAddressbookBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_LONG)
				.setRequestListener(requestListener)
				.post(body)
				.setAsynchronous(false)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken updateAddressBookRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		
		String md5hash = HttpUtils.calculateMD5hash(body.getBytes());
		Header header = new Header(HttpHeaderConstants.CONTENT_MD5, md5hash);
		
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(updateAddressbookBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.addHeader(header)
				.post(body)
				.setAsynchronous(false)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken microAppPostRequest(String url, JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);

		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.addHeader(PlatformUtils.getHeaders())
				.setRequestListener(requestListener)
				.post(body)
				.build();
		
		return requestToken;
	}

	public static RequestToken postDeviceDetailsRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(postDeviceDetailsBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}
	
	public static RequestToken postGreenBlueDetailsRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(postGreenBlueDetailsBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}
	
	public static RequestToken sendUserLogInfoRequest(String logKey, JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(sendUserLogsInfoBaseUrl() + logKey)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken deleteAccountRequest(IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(deleteAccountBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.delete()
				.build();
		return requestToken;
	}

	public static RequestToken unlinkAccountRequest(IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(unlinkAccountBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(null)
				.build();
		return requestToken;
	}
	
	public static RequestToken downloadImageTaskRequest(String id, String fileName, String filePath, boolean hasCustomIcon, boolean statusImage, String url, IRequestListener requestListener)
	{
		String urlString;
		
		if (TextUtils.isEmpty(url))
		{
			if (statusImage)
			{
				urlString = getStatusBaseUrl() + "/" + id + "?only_image=true";
			}
			else
			{
				boolean isGroupConversation = OneToNConversationUtils.isGroupConversation(id);

				if (hasCustomIcon)
				{
					urlString = (isGroupConversation ? getGroupBaseUrl() + id + "/avatar" : getAvatarBaseUrl() + "/" + id) + "?fullsize=1";
				}
				else
				{
					urlString = getStaticAvatarBaseUrl() + "/" + fileName;
				}
			}
		}
		else
		{
			urlString = url;
		}
		RequestToken requestToken = new FileRequest.Builder()
				.setUrl(urlString)
				.setFile(filePath)
				.setRequestListener(requestListener)
				.get()
				.build();
		return requestToken;		
	}
	
	public static RequestToken editProfileAvatarRequest(String filePath, IRequestListener requestListener)
	{
		File file = new File(filePath);
		FileBody body = new FileBody("application/json", file);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(editProfileAvatarBase())
				.setRequestType(Request.REQUEST_TYPE_LONG)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}
	
	public static RequestToken authSDKRequest(String urlParamString, IRequestListener requestListener)
	{
		List<Header> headerList = new ArrayList<Header>(1);
		headerList.add(new Header("Content-type", "text/plain"));
		if(authSDKBaseUrl().contains(HttpRequestConstants.BASE_SDK_STAGING))
		{
			headerList.add(new Header("Cookie", "uid=UZtZkaEMFSBRwmys;token=EeEKpHJzesU="));
		}
		else
		{
			headerList.add(new Header("Cookie", "uid=" + AccountUtils.mUid + ";token=" + AccountUtils.mToken));
		}
			
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(authSDKBaseUrl() + "authorize" + "?" + urlParamString)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.setHeaders(headerList)
				.build();
		return requestToken;
	}
	
	public static RequestToken editGroupProfileAvatarRequest(String filePath, IRequestListener requestListener, String groupId)
	{
		File file = new File(filePath);
		FileBody body = new FileBody("application/json", file);

		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(groupProfileBaseUrl() + groupId + "/avatar")
				.setRequestType(Request.REQUEST_TYPE_LONG)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}
	
	public static RequestToken getHikeJoinTimeRequest(String msisdn, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getHikeJoinTimeBaseUrl() + msisdn)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.build();
		return requestToken;
	}
}
