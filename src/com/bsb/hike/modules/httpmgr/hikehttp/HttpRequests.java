package com.bsb.hike.modules.httpmgr.hikehttp;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.HttpUtils;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.analytics.HttpAnalyticsConstants;
import com.bsb.hike.modules.httpmgr.interceptor.GzipRequestInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;
import com.bsb.hike.modules.httpmgr.request.BitmapRequest;
import com.bsb.hike.modules.httpmgr.request.ByteArrayRequest;
import com.bsb.hike.modules.httpmgr.request.FileDownloadRequest;
import com.bsb.hike.modules.httpmgr.request.FileRequest;
import com.bsb.hike.modules.httpmgr.request.FileRequestPersistent;
import com.bsb.hike.modules.httpmgr.request.FileUploadRequest;
import com.bsb.hike.modules.httpmgr.request.IGetChunkSize;
import com.bsb.hike.modules.httpmgr.request.JSONArrayRequest;
import com.bsb.hike.modules.httpmgr.request.JSONObjectRequest;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.StringRequest;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.ByteArrayBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.FileBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.MultipartRequestBody;
import com.bsb.hike.modules.httpmgr.retry.BasicRetryPolicy;
import com.bsb.hike.modules.stickersearch.StickerLanguagesManager;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.userlogs.PhoneSpecUtils;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.authSDKBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.bulkLastSeenUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.deleteAccountBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.editProfileAvatarBase;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getActionsUpdateUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getAvatarBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getBaseCodeGCAcceptUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getBotdiscoveryTableUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getDeleteAvatarBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getDeleteStatusBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getFastFileUploadBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getForcedStickersUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getGroupBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getGroupBaseUrlForLinkSharing;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getHikeJoinTimeBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getPostImageSUUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getStaticAvatarBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getStatusBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getStickerTagsUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getUploadContactOrLocationBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getUploadFileBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getValidateFileKeyBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.groupProfileBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.httpNetworkTestUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.languageListUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.lastSeenUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.multiStickerDownloadUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.multiStickerImageDownloadUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.postAddressbookBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.postDeviceDetailsBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.postGreenBlueDetailsBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.preActivationBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.registerAccountBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.registerViewActionUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.sendDeviceDetailBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.sendUserLogsInfoBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.setProfileUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.signUpPinCallBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.singleStickerDownloadBase;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.singleStickerImageDownloadBase;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.singleStickerTagsUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerCategoryFetchPrefOrderUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerPalleteImageDownloadUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerPreviewImageDownloadUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerShopDownloadUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerShopFetchCategoryTagsUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerShopFetchCategoryUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerSignupUpgradeUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.stickerCategoryDetailsUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.unlinkAccountBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.updateAddressbookBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.updateLoveLinkUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.updateUnLoveLinkUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.validateNumberBaseUrl;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.getHistoricalStatusUpdatesUrl;
import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_HIGH;
import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_LOW;
import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_NORMAL;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_LONG;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_SHORT;


public class HttpRequests
{
	public static RequestToken singleStickerDownloadRequest(String requestId, String stickerId, String categoryId, IRequestListener requestListener, String keyboardList)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(singleStickerDownloadBase() + "?catId=" + categoryId + "&stId=" + stickerId + "&resId=" + Utils.getResolutionId() + "&kbd=" + keyboardList)
				.setId(requestId)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.setPriority(PRIORITY_HIGH)
				.setAnalyticsParam(HttpAnalyticsConstants.HTTP_SINGLE_STICKER_DOWNLOAD_ANALYTICS_PARAM)
				.build();
		return requestToken;
	}

	public static RequestToken singleStickerImageDownloadRequest(String requestId, String stickerId, String categoryId, boolean miniStk, IRequestListener requestListener)
	{
		miniStk = miniStk & StickerManager.getInstance().isMiniStickersEnabled();
		String url = singleStickerImageDownloadBase() + "?catId=" + categoryId + "&stId=" + stickerId + "&resId=" + Utils.getResolutionId() + "&mini_stk=" + miniStk;
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.setId(requestId)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.setPriority(PRIORITY_HIGH)
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

	public static RequestToken multiStickerImageDownloadRequest(String requestId, IRequestInterceptor interceptor, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(multiStickerImageDownloadUrl())
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
		List<String> unsupportedLanguages = StickerLanguagesManager.getInstance().getUnsupportedLanguagesCollection();

		String url = stickerShopDownloadUrl() + "?offset=" + offset + "&resId=" + Utils.getResolutionId() + "&lang=" + StickerSearchUtils.getISOCodeFromLocale(Utils.getCurrentLanguageLocale());
		url = Utils.isEmpty(unsupportedLanguages) ? url : (url + "&unknown_langs=" + StickerLanguagesManager.getInstance().listToString(unsupportedLanguages));

		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.setId(requestId)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.setPriority(PRIORITY_HIGH)
				.build();
		return requestToken;
	}

	public static RequestToken fetchCategoryData(String requestId, JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(stickerShopFetchCategoryUrl())
				.setId(requestId)
				.post(body)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_LONG)
				.setPriority(PRIORITY_LOW)
				.build();
		return requestToken;
	}

    public static RequestToken fetchCategoryTagData(String requestId, JSONObject json, IRequestListener requestListener)
    {
        JsonBody body = new JsonBody(json);
        RequestToken requestToken = new JSONObjectRequest.Builder()
                .setUrl(stickerShopFetchCategoryTagsUrl())
                .setId(requestId)
                .post(body)
                .setRequestListener(requestListener)
                .setRequestType(REQUEST_TYPE_LONG)
                .setPriority(PRIORITY_LOW)
                .build();
        return requestToken;
    }

	public static RequestToken getPrefOrderForCategories(String requestId, IRequestListener requestListener, int catSize, int offset)
	{
		String url = stickerCategoryFetchPrefOrderUrl() + "?N=" + catSize + "&offset=" + offset;
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.setId(requestId)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_LONG)
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

	public static RequestToken stickerCategoryDetailsDownloadRequest(String requestId, String categoryId, IRequestListener requestListener)
	{
		List<String> unsupportedLanguages = StickerLanguagesManager.getInstance().getUnsupportedLanguagesCollection();
		String url = stickerCategoryDetailsUrl() + "?catId=" + categoryId + "&resId=" + Utils.getResolutionId() + "&lang=" + StickerSearchUtils.getCurrentLanguageISOCode();
		url = Utils.isEmpty(unsupportedLanguages) ? url : (url + "&unknown_langs=" + StickerLanguagesManager.getInstance().listToString(unsupportedLanguages));
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.setId(requestId)
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

		boolean isUploadingImage = false;
		
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
				isUploadingImage = true;
			}
		}

		//TODO isAnyHeaderPresent is useless. Remove this.

			if(isAnyHeaderPresent)
		{
			final RequestBody requestBody = multipartBuilder.build();

			MultipartRequestBody body = new MultipartRequestBody(requestBody);

			requestToken = new JSONObjectRequest.Builder().setUrl(getPostImageSUUrl()).setRequestListener(requestListener).post(body).build();

			// GZIP only for text updates
			if(!isUploadingImage)
			{
				requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
			}
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
				.setHeaders(PlatformUtils.getHeaders())
				.build();
		return requestToken;
	}

	public static RequestToken getPlatformFetchRequest(String url, IRequestListener requestListener, List<Header> headers)
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

	public static RequestToken getBlockedCallerList(String url, IRequestListener requestListener, int noOfRetry, int retryDelay, float backOffMultiplier)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.setRetryPolicy(new BasicRetryPolicy(noOfRetry, retryDelay, backOffMultiplier))
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
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

	public static RequestToken postCallerMsisdn(String url, JSONObject json, IRequestListener requestListener, int noOfRetry, int delay, float multiplier, boolean responseOnUiThread)
	{		
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.post(body)
				.setRetryPolicy(new BasicRetryPolicy(noOfRetry, delay, multiplier))
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.setResponseOnUIThread(responseOnUiThread)
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

	public static RequestToken postAnonymousNameFetchRequest(String url, IRequestListener requestListener, JSONObject json, List<Header> headers)
	{

		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.post(new JsonBody(json))
				.setRetryPolicy(new BasicRetryPolicy(HikePlatformConstants.NUMBER_OF_RETRIES, HikePlatformConstants.RETRY_DELAY, HikePlatformConstants.BACK_OFF_MULTIPLIER))
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.setHeaders(headers)
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

	public static RequestToken registerAccountRequest(JSONObject json, IRequestListener requestListener, BasicRetryPolicy retryPolicy)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(registerAccountBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setRetryPolicy(retryPolicy)
				.post(body)
				.setAsynchronous(false)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken validateNumberRequest(JSONObject json, IRequestListener requestListener, BasicRetryPolicy retryPolicy)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(validateNumberBaseUrl() + "?digits=4")
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setRetryPolicy(retryPolicy)
				.post(body)
				.setAsynchronous(false)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken setProfileRequest(JSONObject json, IRequestListener requestListener, BasicRetryPolicy retryPolicy)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(setProfileUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setRetryPolicy(retryPolicy)
				.post(body)
				.setAsynchronous(false)
				.build();
		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken postAddressBookRequest(JSONObject json, IRequestListener requestListener, IResponseInterceptor responseInterceptor, BasicRetryPolicy retryPolicy)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(postAddressbookBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_LONG)
				.setRequestListener(requestListener)
				.setRetryPolicy(retryPolicy)
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

	public static RequestToken tagsForSingleStickerRequest(String requestId, String stickerId, String categoryId, String keyboardList,  IRequestListener requestListener)
	{

		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setId(requestId)
				.setUrl((singleStickerTagsUrl() + "?catId=" + categoryId + "&stId=" + stickerId + "&resId=" + Utils.getResolutionId() + "&kbd=" + keyboardList))
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.setPriority(PRIORITY_HIGH)
				.build();
		return requestToken;
	}

	public static RequestToken tagsForMultiStickerRequest(String requestId, JSONObject json, IRequestListener requestListener)
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

	public static RequestToken defaultTagsRequest(String requestId, boolean isSignUp, long lastSuccessfulTagDownloadTime, IRequestListener requestListener, String languages)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setId(requestId)
				.setUrl(getStickerTagsUrl() + "?signup_stickers=" + isSignUp + "&timestamp=" + lastSuccessfulTagDownloadTime + "&kbd=" + languages)
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
		if(json==null)
		{
			RequestToken requestToken = new StringRequest.Builder()
					.setUrl(url)
					.setRequestType(Request.REQUEST_TYPE_SHORT)
					.addHeader(PlatformUtils.getHeaders())
					.setRequestListener(requestListener)
					.build();

			return requestToken;
		}
		else
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


	}

    public static RequestToken forwardCardsMISPostRequest(String url, JSONObject json, IRequestListener requestListener, String id)
    {
        if(json==null)
        {
            return null;
        }
        else
        {
            JsonBody body = new JsonBody(json);
            RequestToken requestToken = new StringRequest.Builder()
                    .setUrl(url)
                    .setRequestType(Request.REQUEST_TYPE_SHORT)
                    .addHeader(PlatformUtils.getHeaders())
                    .setRequestListener(requestListener)
                    .setRetryPolicy(new BasicRetryPolicy(HikePlatformConstants.NUMBER_OF_RETRIES, HikePlatformConstants.RETRY_DELAY, HikePlatformConstants.BACK_OFF_MULTIPLIER))
                    .post(body)
                    .setId(id)
                    .build();

            return requestToken;
        }


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

	public static RequestToken downloadBitmapTaskRequest(String urlString, IRequestListener listener)
	{
		BitmapRequest.Builder builder= new BitmapRequest.Builder()
				.setUrl(urlString).setRequestListener(listener).get();
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
				.setRetryPolicy(new BasicRetryPolicy(2,BasicRetryPolicy.DEFAULT_RETRY_DELAY,BasicRetryPolicy.DEFAULT_BACKOFF_MULTIPLIER))
				.post(body)
				.build();
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

	public static RequestToken sendViewsLink(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(registerViewActionUrl())
				.setRequestType(REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(false)
				.setRetryPolicy(new BasicRetryPolicy(2,BasicRetryPolicy.DEFAULT_RETRY_DELAY,BasicRetryPolicy.DEFAULT_BACKOFF_MULTIPLIER))
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

	/**
	 * @param json
	 * @param requestListener
	 * @param noOfRetries
	 * @param delayMultiplier
	 * @return
	 */
	public static RequestToken getShareLinkURLRequest(JSONObject json, IRequestListener requestListener, int noOfRetries, int delayMultiplier)
	{
		JsonBody body = null;
		if (json != null)
		{
			body = new JsonBody(json);
		}

		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getGroupBaseUrlForLinkSharing())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRetryPolicy(new BasicRetryPolicy(noOfRetries, HikePlatformConstants.RETRY_DELAY, delayMultiplier))
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(body)
				.build();
		return requestToken;
	}

	/**
	 *
	 * @param groupCode
	 * @param requestListener
	 * @return
	 */
	public static RequestToken acceptGroupMembershipConfirmationRequest(String groupCode, IRequestListener requestListener)
	{
		String url = getBaseCodeGCAcceptUrl() + groupCode;

		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(url)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(null)
				.build();
		return requestToken;
	}
	
	public static RequestToken getLatestApkInfo(IRequestListener requestListener)
	{
		String url = HttpRequestConstants.latestApkInfoUrl();

		JSONObject js = new JSONObject();
		try {
			js.put("version_name", Utils.getAppVersionName());
			js.put("version_code", Utils.getAppVersionCode());
			js.put("sim_mcc_mnc", PhoneSpecUtils
					.getSimDetails(HikeMessengerApp.getInstance().getApplicationContext())
					.get(PhoneSpecUtils.MCC_MNC));
			js.put("nwk_mcc_mnc", PhoneSpecUtils
					.getNetworkDetails(HikeMessengerApp.getInstance().getApplicationContext())
					.get(PhoneSpecUtils.MCC_MNC));
		}
		catch (JSONException je )
		{
			Logger.d("AUTOAPK", "exception in handling json : " + je);
		}
		Logger.d("AUTOAPK", "json params in request : " + js.toString());
		RequestToken requestToken = new JSONObjectRequest.Builder()
		.setUrl(url).setRequestType(Request.REQUEST_TYPE_SHORT)
		.setRequestListener(requestListener)
		.setResponseOnUIThread(true)
		.post(new JsonBody(js))
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


	public static RequestToken platformZipDownloadRequestWithResume(String filePath, String stateFilePath, String url, IRequestListener requestListener, long startOffset,float progressDone)
	{
		List<Header> headers = new ArrayList<Header>(1);
		headers.add(new Header(HttpHeaderConstants.RANGE, "bytes=" + startOffset + "-"));
		RequestToken requestToken = new FileRequestPersistent.Builder()
				.setUrl(url)
				.setFile(filePath)
				.setStateFilePath(stateFilePath)
				.setRequestListener(requestListener)
				.setHeaders(headers)
				.addHeader(PlatformUtils.getHeaders())
				.setCurrentPointer(startOffset)
				.setInitialProgress(progressDone)
				.setRetryPolicy(new BasicRetryPolicy(HikePlatformConstants.NUMBER_OF_RETRIES, HikePlatformConstants.RETRY_DELAY, HikePlatformConstants.BACK_OFF_MULTIPLIER))
				.build();
		return requestToken;
	}

	public static RequestToken uploadFileRequest(byte[] fileBytes, String boundry, IRequestListener requestListener, List<Header> headers, String url)
	{
		ByteArrayBody body = new ByteArrayBody("multipart/form-data; boundary=" + boundry, fileBytes);

		RequestToken requestToken = new ByteArrayRequest.Builder()
				.setUrl(url)
				.setRequestType(Request.REQUEST_TYPE_LONG)
				.setRequestListener(requestListener)
				.addHeader(headers)
				.post(body)
				.setAsynchronous(false)
				.setPriority(PRIORITY_HIGH)
				.setRetryPolicy(new BasicRetryPolicy(0, 1, 1))
				.build();
		return requestToken;
	}

	/*
     * this request is just for checking that internet is working but mqtt is unable to connect.
     * we will send an async http call to server
     */
    public static RequestToken httpNetworkTestRequest(int errorCode, int port, int networkType, int exceptionCount)
    {
        int isForeground = -1;
        if(HikeMessengerApp.getInstance() != null)
        {
            isForeground = Utils.isAppForeground(HikeMessengerApp.getInstance())? 1 : 0;
        }

        String url = httpNetworkTestUrl() + "/" + errorCode+ "?port="+port +"&net="+networkType+"&fg="+isForeground+"&ec="+exceptionCount;
        RequestToken requestToken = new JSONObjectRequest.Builder()
                .setUrl(url)
                .setRequestType(REQUEST_TYPE_SHORT)
                .setAsynchronous(true)
				.setPriority(PRIORITY_LOW)
                .setRetryPolicy(new BasicRetryPolicy(0, 1, 1))
                .build();
        Logger.e("HikeHttpRequests", "Making http call to " + url);
        return requestToken;
    }

	public static RequestToken getLanguageListOrderHTTP(IRequestListener requestListener)
	{

		RequestToken requestToken = new JSONObjectRequest.Builder().setUrl(languageListUrl()).setRequestType(Request.REQUEST_TYPE_SHORT).setRequestListener(requestListener)
				.setResponseOnUIThread(true).build();
		return requestToken;

	}

	public static RequestToken downloadFile(String destFilePath, String url, long msgId, IRequestListener requestListener, IGetChunkSize chunkSizePolicy, String fileTypeString)
	{
		RequestToken token = new FileDownloadRequest.Builder()
				.setUrl(url)
				.setRequestListener(requestListener)
				.addHeader(new Header("Accept-Encoding", "musixmatch"))
				.setFile(destFilePath)
				.setFileTypeString(fileTypeString)
				.setChunkSizePolicy(chunkSizePolicy)
				.setId(String.valueOf(msgId))
				.setRetryPolicy(new BasicRetryPolicy(FileTransferManager.MAX_RETRY_COUNT, FileTransferManager.RETRY_DELAY, FileTransferManager.RETRY_BACKOFF_MULTIPLIER))
				.build();
		return token;
	}
	
	public static RequestToken uploadFile(String filePath, long msgId, String videoCompressionReqd, String fileType, IRequestListener requestListener, IRequestInterceptor interceptor, IGetChunkSize chunkSizePolicy)
	{
		RequestToken requestToken = new FileUploadRequest.Builder()
				.setUrl(getUploadFileBaseUrl())
				.setId(String.valueOf(msgId))
				.setRequestType(Request.REQUEST_TYPE_LONG)
				.setFileType(fileType)
				.addHeader(new Header("X-Compression-Required", videoCompressionReqd))
				.setChunkSizePolicy(chunkSizePolicy)
				.setRequestListener(requestListener)
				.setFile(filePath)
				.build();

		requestToken.getRequestInterceptors().addFirst("uploadFileInterceptor", interceptor);
		return requestToken;
	}

	public static RequestToken getAnalyticsUploadRequestToken(IRequestListener requestListener, IRequestInterceptor requestInterceptor, String requestId, int retryCount, int delayBeforeRetry)
	{
        RequestToken requestToken = new JSONObjectRequest.Builder()
                .setUrl(HttpRequestConstants.getAnalyticsUrl())
                .setRequestType(Request.REQUEST_TYPE_LONG)
                .setAsynchronous(true)
                .setId(requestId)
                .setRequestListener(requestListener)
                .setRetryPolicy(new BasicRetryPolicy(retryCount, delayBeforeRetry, 1))
                .post(null)
                .build();
        requestToken.getRequestInterceptors().addFirst("analytics", requestInterceptor);
        return requestToken;
    }

	public static RequestToken getForcedDownloadListRequest(String requestId, IRequestListener requestListener, JSONObject json)
	{
		JsonBody body = new JsonBody(json);

		RequestToken requestToken = new JSONObjectRequest.Builder()
                .setId(requestId)
                .setUrl(getForcedStickersUrl())
                .setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_LONG)
                .post(body)
                .setPriority(PRIORITY_HIGH)
                .build();

		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;

	}
	public static RequestToken platformPostRequest(String url, JSONObject json,List<Header> headers, IRequestListener requestListener)
	{
		if(json==null)
		{
			RequestToken requestToken = new StringRequest.Builder()
					.setUrl(url)
					.setRequestType(Request.REQUEST_TYPE_SHORT)
					.addHeader(headers)
					.setRequestListener(requestListener)
					.build();

			return requestToken;
		}
		else
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


	}
	
	public static RequestToken validateFileKey(String fileKey, IRequestListener requestListener)
	{
		RequestToken requestToken = new ByteArrayRequest.Builder()
				.setUrl(getValidateFileKeyBaseUrl() + fileKey)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.head()
				.setRetryPolicy(new BasicRetryPolicy(FileTransferManager.MAX_RETRY_COUNT, 0, 1))
				.build();
		return requestToken;
	}

	public static RequestToken verifyMd5(long msgId, IRequestListener requestListener, IRequestInterceptor initFileUploadInterceptor)
	{
		RequestToken requestToken = new ByteArrayRequest.Builder()
				.setUrl(getFastFileUploadBaseUrl())  // url is changed in interceptor processing as we calculate md5 of file in interceptor
				.setId(String.valueOf(msgId))
				.setRequestType(REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.head()
				.setRetryPolicy(new BasicRetryPolicy(FileTransferManager.MAX_RETRY_COUNT, 0, 1))
				.build();
		requestToken.getRequestInterceptors().addFirst("initFileUpload", initFileUploadInterceptor);
		return requestToken;
	}

	public static RequestToken uploadContactOrLocation(String fileName, JSONObject json, String fileType, IRequestListener requestListener, IRequestInterceptor interceptor)
	{
		List<Header> headers = new ArrayList<>();
		headers.add(new Header("Content-Name", fileName));
		headers.add(new Header("Content-Type", TextUtils.isEmpty(fileType) ? "" : fileType));
		headers.add(new Header("X-Thumbnail-Required", "0"));
		
		JsonBody body = new JsonBody(json);
		
 		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getUploadContactOrLocationBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.addHeader(headers)
				.put(body)
				.build();
		requestToken.getRequestInterceptors().addFirst("uploadContactOrLocationPreProcess", interceptor);
		return requestToken;
	}

	public static RequestToken uploadChunk(URL url, byte[] fileBytes, String BOUNDARY, List<Header> headers, String id, IRequestListener listener)
	{
		ByteArrayBody body = new ByteArrayBody("multipart/form-data; boundary=" + BOUNDARY, fileBytes);
		RequestToken requestToken = new ByteArrayRequest.Builder()
				.setUrl(url)
				.post(body)
				.setId(id)
				.setHeaders(headers)
				.setRequestListener(listener)
				.setRetryPolicy(new BasicRetryPolicy(FileTransferManager.MAX_RETRY_COUNT, FileTransferManager.RETRY_DELAY, FileTransferManager.RETRY_BACKOFF_MULTIPLIER))
				.setAsynchronous(false)
				.build();
		return requestToken;
	}

	public static RequestToken getBytesFromServer(URL url, String sessionId, IRequestListener listener)
	{
		RequestToken requestToken = new ByteArrayRequest.Builder()
				.setUrl(url)
				.addHeader(new Header("X-SESSION-ID", sessionId))
				.setAsynchronous(false)
				.setRetryPolicy(new BasicRetryPolicy(1, FileTransferManager.RETRY_DELAY, 1))
				.setRequestListener(listener)
				.build();
		return requestToken;
	}

	public static RequestToken getHistoricSUToken(String userMsisdn, IRequestListener listener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getHistoricalStatusUpdatesUrl() + userMsisdn)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(listener)
				.build();
		requestToken.execute();
		return requestToken;
	}

	public static RequestToken atomicTipRequestGet(String url, IRequestListener listener)
	{
		JSONObjectRequest.Builder builder = new JSONObjectRequest.Builder()
				.setUrl(url)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(listener)
				.setRetryPolicy(new BasicRetryPolicy(ProductPopupsConstants.numberOfRetries, ProductPopupsConstants.retryDelay, ProductPopupsConstants.backOffMultiplier));
		return builder.build();
	}

	public static RequestToken atomicTipRequestPost(String url, JSONObject payload, IRequestListener listener, boolean addHeader) {
		JsonBody jsonBody = new JsonBody(payload);
		JSONObjectRequest.Builder builder = new JSONObjectRequest.Builder()
				.setUrl(url)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(listener)
				.post(jsonBody)
				.setRetryPolicy(new BasicRetryPolicy(ProductPopupsConstants.numberOfRetries, ProductPopupsConstants.retryDelay, ProductPopupsConstants.backOffMultiplier));
		if (addHeader) {
			builder.addHeader(PlatformUtils.getHeaders());
		}
		return builder.build();
	}

	public static RequestToken uploadUserSettings(IRequestListener requestListener, int retryCount, int delayBeforeRetry,@NonNull JSONObject payloadJSON)
	{

		JSONObject settingsJSON = new JSONObject();
		try
		{
			settingsJSON.put(HikeConstants.BackupRestore.KEY_SETTINGS, payloadJSON);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}
		JsonBody jsonBody = new JsonBody(settingsJSON);

		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(HttpRequestConstants.getSettingsUploadUrl())
				.setRequestListener(requestListener)
				.setId(Utils.StringToMD5(settingsJSON.toString()))
				.setRetryPolicy(new BasicRetryPolicy(retryCount, delayBeforeRetry, 1))
				.post(jsonBody)
				.build();
//		requestToken.getRequestInterceptors().addLast("gzip", new GzipRequestInterceptor());
		return requestToken;
	}

	public static RequestToken downloadUserSettings(IRequestListener requestListener,
												  int retryCount, int delayBeforeRetry)
	{
		//Since user settings will always be common. Define constant local var as id
		String downloadSettingsID = "dnwloadSettings";

		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(HttpRequestConstants.getSettingsDownloadUrl())
				.setRequestListener(requestListener)
				.setId(downloadSettingsID)
				.setRetryPolicy(new BasicRetryPolicy(retryCount, delayBeforeRetry, 1))
				.build();
		return requestToken;
	}

}
