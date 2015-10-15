package com.bsb.hike.modules.httpmgr.hikehttp;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.authSDKBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.bulkLastSeenUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.deleteAccountBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.editProfileAvatarBase;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getActionsUpdateUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getAvatarBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getDeleteAvatarBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getDeleteStatusBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getGroupBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getHikeJoinTimeBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getPostImageSUUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getStaticAvatarBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getStatusBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getStickerTagsUrl;
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
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.signUpPinCallBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.singleStickerDownloadBase;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerPalleteImageDownloadUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerPreviewImageDownloadUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerShopDownloadUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerSignupUpgradeUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.unlinkAccountBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.updateAddressbookBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.updateLoveLinkUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.updateUnLoveLinkUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.validateNumberBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getBotdiscoveryTableUrl;
import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_HIGH;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_LONG;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_SHORT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants;
import com.bsb.hike.modules.httpmgr.interceptor.GzipRequestInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;
import com.bsb.hike.modules.httpmgr.request.ByteArrayRequest;
import com.bsb.hike.modules.httpmgr.request.FileRequest;
import com.bsb.hike.modules.httpmgr.request.JSONArrayRequest;
import com.bsb.hike.modules.httpmgr.request.JSONObjectRequest;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.StringRequest;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.FileBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.MultipartRequestBody;
import com.bsb.hike.modules.httpmgr.retry.BasicRetryPolicy;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;
import com.hike.transporter.utils.Logger;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.RequestBody;

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
	
	public static RequestToken StickerPalleteImageDownloadRequest(String requestId, String categoryId, IRequestInterceptor interceptor, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(stickerPalleteImageDownloadUrl() + "?catId=" + categoryId + "&resId=" + Utils.getResolutionId())
				.setId(requestId)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_LONG)
				.setPriority(10) // Setting priority between sticker shop task and enable_disable icon task
				.build();
		requestToken.getRequestInterceptors().addLast("sticker", interceptor);
		return requestToken;
	}
	
	public static RequestToken StickerPreviewImageDownloadRequest(String requestId, String categoryId, IRequestInterceptor interceptor, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(stickerPreviewImageDownloadUrl() + "?catId=" + categoryId + "&resId=" + Utils.getResolutionId())
				.setId(requestId)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.build();
		requestToken.getRequestInterceptors().addLast("sticker", interceptor);
		return requestToken;
	}
	
	public static RequestToken StickerShopDownloadRequest(String requestId, int offset, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(stickerShopDownloadUrl() + "?offset=" + offset + "&resId=" + Utils.getResolutionId())
				.setId(requestId)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.setPriority(PRIORITY_HIGH)
				.build();
		return requestToken;
	}
	
	public static RequestToken StickerSignupUpgradeRequest(String requestId, JSONObject json, IRequestListener requestListener)
	{
		IRequestBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(stickerSignupUpgradeUrl())
				.setId(requestId)
				.post(body)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.build();
		requestToken.getRequestInterceptors().addFirst("gzip", new GzipRequestInterceptor());
		return requestToken;
	}
	
	public static RequestToken LastSeenRequest(String msisdn, IRequestListener requestListener, BasicRetryPolicy retryPolicy)
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

	public static RequestToken BulkLastSeenRequest(IRequestListener requestListener, BasicRetryPolicy retryPolicy)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(bulkLastSeenUrl())
				.setRetryPolicy(retryPolicy)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.build();
		return requestToken;
	}
	
	public static RequestToken postStatusRequest(String argStatusMessage, int argMood, IRequestListener requestListener, String imageFilePath) throws IOException
	{
		final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

		final MediaType MEDIA_TYPE_TEXTPLAIN = MediaType.parse("text/plain; charset=UTF-8");

		boolean isAnyHeaderPresent = false;
		
		RequestToken requestToken = null;
		
		MultipartBuilder multipartBuilder = new MultipartBuilder()
				.type(MultipartBuilder.FORM);
		
		if(!TextUtils.isEmpty(argStatusMessage))
		{
			multipartBuilder.addPart(Headers.of("Content-Disposition", "form-data; name=\"status-message\""), RequestBody.create(MEDIA_TYPE_TEXTPLAIN, argStatusMessage));
			isAnyHeaderPresent = true;
		}
		
		if(argMood != -1)
		{
			multipartBuilder.addPart(Headers.of("Content-Disposition", "form-data; name=\"mood\""), RequestBody.create(MEDIA_TYPE_TEXTPLAIN, String.valueOf(argMood+1)));
			isAnyHeaderPresent = true;
		}

		if(!TextUtils.isEmpty(imageFilePath))
		{
			File imageFile = new File(imageFilePath);
			if (imageFile.exists())
			{
				multipartBuilder.addPart(Headers.of("Content-Disposition", "form-data; name=\"file\";filename=\"" + imageFile.getName() + "\""),
						RequestBody.create(MEDIA_TYPE_PNG, new File(imageFilePath)));
				isAnyHeaderPresent = true;
			}
		}
		
		if(isAnyHeaderPresent)
		{
			final RequestBody requestBody = multipartBuilder.build();

			MultipartRequestBody body = new MultipartRequestBody(requestBody);

			requestToken = new JSONObjectRequest.Builder().setUrl(getPostImageSUUrl()).setRequestListener(requestListener).post(body).build();
			
			requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		}

		return requestToken;
	}

	public static RequestToken postAdminRequest(String grpId, JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getGroupBaseUrl()+grpId+"/admin")
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(body)
				.build();
		return requestToken;
	}
	
	public static RequestToken postChangeAddMemSettingRequest(String grpId, JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getGroupBaseUrl()+grpId+"/setting")
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.put(body)
				.build();
		return requestToken;
	}

	public static RequestToken platformZipDownloadRequest(String filePath, String url, IRequestListener requestListener)
	{
		RequestToken requestToken = new FileRequest.Builder()
				.setUrl(url)
				.setFile(filePath)
				.setRequestListener(requestListener)
				.setRetryPolicy(new BasicRetryPolicy(HikePlatformConstants.NUMBER_OF_RETRIES, HikePlatformConstants.RETRY_DELAY, HikePlatformConstants.BACK_OFF_MULTIPLIER))
				.build();
		return requestToken;
	}

	public static RequestToken getPlatformUserIdForFullAddressBookFetchRequest(String url, IRequestListener requestListener, List<Header> headers)
	{
		RequestToken requestToken = new JSONArrayRequest.Builder()
				.setUrl(url)
				.setRetryPolicy(new BasicRetryPolicy(HikePlatformConstants.NUMBER_OF_RETRIES, HikePlatformConstants.RETRY_DELAY, HikePlatformConstants.BACK_OFF_MULTIPLIER))
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
				.setRetryPolicy(new BasicRetryPolicy(HikePlatformConstants.NUMBER_OF_RETRIES, HikePlatformConstants.RETRY_DELAY, HikePlatformConstants.BACK_OFF_MULTIPLIER))
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_LONG)
				.setHeaders(headers)
				.build();

		return requestToken;
	}
	
	public static RequestToken postNumberAndGetCallerDetails(String url,JSONObject json, IRequestListener requestListener, int delay, int multiplier)
	{		
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.post(body)
				.setRetryPolicy(new BasicRetryPolicy(HikePlatformConstants.NUMBER_OF_RETRIES, delay, multiplier))
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.setResponseOnUIThread(true)
				.build();

		return requestToken;
	}

	public static RequestToken postPlatformUserIdFetchRequest(String url, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.post(null)
				.setRetryPolicy(new BasicRetryPolicy(HikePlatformConstants.NUMBER_OF_RETRIES, HikePlatformConstants.RETRY_DELAY, HikePlatformConstants.BACK_OFF_MULTIPLIER))
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

	public static RequestToken postAddressBookRequest(JSONObject json, IRequestListener requestListener, IResponseInterceptor responseInterceptor)
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
		requestToken.getResponseInterceptors().addFirst("abProcessing", responseInterceptor);
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
	
	public static RequestToken tagsForCategoriesRequest(String requestId, JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		
		RequestToken requestToken = new JSONObjectRequest.Builder()
					.setId(requestId)
					.setUrl(getStickerTagsUrl())
					.setRequestListener(requestListener)
					.setRequestType(REQUEST_TYPE_LONG)
					.post(body)
					.setPriority(PRIORITY_HIGH)
					.build();
		
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}
	
	public static RequestToken defaultTagsRequest(String requestId, boolean isSignUp, long lastSuccessfulTagDownloadTime, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
					.setId(requestId)
					.setUrl(getStickerTagsUrl() + "?signup_stickers=" + isSignUp + "&timestamp=" + lastSuccessfulTagDownloadTime)
					.setRequestListener(requestListener)
					.setRequestType(REQUEST_TYPE_SHORT)
					.setPriority(PRIORITY_HIGH)
					.build();
		
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}
	
	public static RequestToken productPopupRequest(String url, IRequestListener requestListener, String requestType)
	{
		ByteArrayRequest.Builder builder = new ByteArrayRequest.Builder().
				setUrl(url).
				setRequestType(Request.REQUEST_TYPE_SHORT).
				setRetryPolicy(new BasicRetryPolicy(ProductPopupsConstants.numberOfRetries, ProductPopupsConstants.retryDelay, ProductPopupsConstants.backOffMultiplier)).
				setRequestListener(requestListener);

		if (requestType.equals(HikeConstants.POST))
		{
			builder = builder.post(null);
		}
		
		return builder.build();
	}

	public static RequestToken microAppPostRequest(String url, JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);

		RequestToken requestToken = new StringRequest.Builder()
				.setUrl(url)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.addHeader(PlatformUtils.getHeaders())
				.setRequestListener(requestListener)
				.post(body)
				.build();
		
		return requestToken;
	}

	public static RequestToken microAppGetRequest(String url, IRequestListener requestListener)
	{

		RequestToken requestToken = new StringRequest.Builder()
				.setUrl(url)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.addHeader(PlatformUtils.getHeaders())
				.setRequestListener(requestListener)
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
				.post(null)
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
		return downloadImageTaskRequest(id,fileName,filePath,hasCustomIcon,statusImage,url,false,requestListener);
	}
	
	public static RequestToken downloadImageTaskRequest(String id, String fileName, String filePath, boolean hasCustomIcon, boolean statusImage, String url,boolean forceCreateNewToken, IRequestListener requestListener)
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
		
		FileRequest.Builder builder = new FileRequest.Builder()
				.setUrl(urlString)
				.setFile(filePath)
				.setRequestListener(requestListener)
				.get();
		
		if(forceCreateNewToken)
		{
			//this should be done when new image needs to be downloaded irrespectve of a previous download is running on the same URL
			builder.setId(id+"_"+System.currentTimeMillis());
		}
			
		RequestToken requestToken = builder.build();
		
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
	
	public static RequestToken createLoveLink(JSONObject json, IRequestListener requestListener,String id)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(updateLoveLinkUrl())
				.setRequestType(REQUEST_TYPE_SHORT)
				.setId(id)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(body)
				.build();
		return requestToken;
	}
	
	public static RequestToken removeLoveLink(JSONObject json, IRequestListener requestListener,String id)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(updateUnLoveLinkUrl())
				.setRequestType(REQUEST_TYPE_SHORT)
				.setId(id)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(body)
				.build();
		return requestToken;
	}
	public static RequestToken signUpPinCallRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);

		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(signUpPinCallBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(body)
				.build();
		requestToken.getRequestInterceptors().addFirst("gzip", new GzipRequestInterceptor());
		return requestToken;
	}
	
	public static RequestToken getJSONfromUrl(String url, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(null)
				.setAsynchronous(false)
				.build();
		return requestToken;
	}
	
	public static RequestToken getActionUpdates(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getActionsUpdateUrl())
				.setRequestListener(requestListener)
				.post(body)
				.build();
		return requestToken;
	}

	public static RequestToken deleteStatusRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);

		RequestToken requestToken = new ByteArrayRequest.Builder()
				.setUrl(getDeleteStatusBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(body)
				.build();
		return requestToken;
	}

	public static RequestToken deleteAvatarRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = null;
		if (json != null)
		{
			body = new JsonBody(json);
		}

		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getDeleteAvatarBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.setResponseOnUIThread(true)
				.build();
		return requestToken;
	}
	
	public static RequestToken getAvatarForBots(String msisdn, IRequestListener listener)
	{

		String botAvatarUrl = getAvatarBaseUrl() + "/" + msisdn;
		Logger.v("BotUtils", botAvatarUrl );  

		RequestToken requestToken = new ByteArrayRequest.Builder().setUrl(botAvatarUrl).setRequestType(Request.REQUEST_TYPE_SHORT).setRequestListener(listener).get().build();

		return requestToken;
	}
	
	public static RequestToken BotDiscoveryTableDownloadRequest(String requestId, int offset, IRequestListener requestListener, JSONObject json)
	{
		JsonBody body = null;
		
		if (json != null)
		{
			body = new JsonBody(json);
		}
		
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getBotdiscoveryTableUrl() +  "?offset=" + offset)
				.setId(requestId)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.setPriority(PRIORITY_HIGH)
				.addHeader(PlatformUtils.getHeaders())
				.post(body)
				.build();
		
		return requestToken;
	}

}
