package com.bsb.hike.platform.content;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bsb.hike.platform.PlatformContentUtils;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.utils.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class HikeWebClient extends WebViewClient
{

	@SuppressLint("NewApi")
	@Override
	public WebResourceResponse shouldInterceptRequest(WebView view, String url)
	{
		Logger.d("Call from webview", "" + url);
		if (url.startsWith("http") || (!url.startsWith(PlatformContentConstants.CONTENT_FONTPATH_BASE)))
		{
			return super.shouldInterceptRequest(view, url);
		}

		Uri myuri = Uri.parse(url.replace(PlatformContentConstants.CONTENT_FONTPATH_BASE, PlatformContentConstants.PLATFORM_CONTENT_DIR));

		String fileNameRequested = myuri.getLastPathSegment();
		String[] name = fileNameRequested.split("\\.");
		String prefix = name[0];
		String suffix = name[1];
		
		if (prefix.contains("Roboto") || prefix.contains("roboto"))
		{
			return super.shouldInterceptRequest(view, url.replace(PlatformContentConstants.CONTENT_FONTPATH_BASE, "file:///android_asset/"));
		}

		InputStream wrtInputStreamm = null;
		WebResourceResponse response = null;
		wrtInputStreamm = new FileInputStream(PlatformContentUtils.openFileParcel(myuri, "").getFileDescriptor());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			int statusCode = 200;
			String reasonPhase = "OK";
			Map<String, String> responseHeaders = new HashMap<String, String>();
			responseHeaders.put("Access-Control-Allow-Origin", "*");
			response = new WebResourceResponse(PlatformContentConstants.ASSETS_FONTS_DIR + suffix, "UTF-8", statusCode, reasonPhase, responseHeaders, wrtInputStreamm);
		}
		else
		{
			response = new WebResourceResponse(PlatformContentConstants.ASSETS_FONTS_DIR + suffix, "UTF-8", wrtInputStreamm);
		}

		return response;
	}
	
	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon)
	{
		((CustomWebView)view).isLoaded = false;
		super.onPageStarted(view, url, favicon);
	}
	
	@Override
	public void onPageFinished(WebView view, String url)
	{
		super.onPageFinished(view, url);
		((CustomWebView)view).isLoaded = true;
	}
}
