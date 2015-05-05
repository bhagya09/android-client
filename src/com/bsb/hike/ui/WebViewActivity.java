package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.view.ViewStub.OnInflateListener;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.chatthread.HikeActionBar;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.media.TagPicker;
import com.bsb.hike.media.TagPicker.TagOnClickListener;
import com.bsb.hike.models.WhitelistDomain;
import com.bsb.hike.models.Conversation.BotInfo;
import com.bsb.hike.models.Conversation.BotInfo.NonMessagingBotConfig;
import com.bsb.hike.models.Conversation.NonMessagingBotMetaData;
import com.bsb.hike.models.FullScreenJavascriptBridge;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.view.TagEditText.Tag;

public class WebViewActivity extends HikeAppStateBaseFragmentActivity implements OnInflateListener, OnClickListener, TagOnClickListener
{

	public static final int WEB_URL_MODE = 1; // DEFAULT MODE OF THIS ACTIVITY
	
	public static final int WEB_URL_WITH_BRIDGE_MODE = 2;
	
	public static final int MICRO_APP_MODE = 3;
	
	public static final String WEBVIEW_MODE = "webviewMode";
	
	private WebView webView;
	
	HikeActionBar actionBar;
	
	BotInfo botInfo;
	NonMessagingBotConfig botConfig;
	NonMessagingBotMetaData botMetaData;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview_activity);
		init();
		setMode(getIntent().getIntExtra(WEBVIEW_MODE, -1));
	}	
	
	private void init()
	{
		actionBar = new HikeActionBar(this);
	}

	
	private void setMode(int mode)
	{
		if(mode == MICRO_APP_MODE)
		{
			setMicroAppMode();
		}
		else 
		{
			setWebURLMode();
		}
	}
	
	private void setMicroAppMode()
	{
		String msisdn = getIntent().getStringExtra(HikeConstants.MSISDN);
		if(msisdn==null){
			throw new IllegalArgumentException("Seems You forgot to send msisdn of Bot my dear");
		}
		initBot();
		setupMicroAppActionBar();
		
		
	}
	
	private void initBot()
	{
		botInfo = null; //fetch
		botConfig = new NonMessagingBotConfig(botInfo.configurtion);
		setupActionBar(botInfo.getConversationName());
		botMetaData = new NonMessagingBotMetaData(botInfo.metadata);	
	}
	
	
	
	private void setWebURLMode()
	{
		String urlToLoad = getIntent().getStringExtra(HikeConstants.Extras.URL_TO_LOAD);
		String title = getIntent().getStringExtra(HikeConstants.Extras.TITLE);
		final boolean allowLoc = getIntent().getBooleanExtra(HikeConstants.Extras.WEBVIEW_ALLOW_LOCATION, false);

		webView = (WebView) findViewById(R.id.t_and_c_page);
		final ProgressBar bar = (ProgressBar) findViewById(R.id.progress);

		WebViewClient client = new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				bar.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon)
			{
				bar.setProgress(0);
				bar.setVisibility(View.VISIBLE);
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public WebResourceResponse shouldInterceptRequest(WebView view, String url)
			{
				// TODO Auto-generated method stub
				return super.shouldInterceptRequest(view, url);
			}
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				if (url == null)
				{
					return false;
				}
				if (url.startsWith("mailto:"))
				{
					MailTo mt = MailTo.parse(url);
					Intent i = newEmailIntent(WebViewActivity.this, mt.getTo(), mt.getSubject(), mt.getBody(), mt.getCc());
					startActivity(i);
					view.reload();
				}
				else if (url.toLowerCase().endsWith("hike.in/rewards/invite"))
				{
					Intent i = new Intent(WebViewActivity.this, HikeListActivity.class);
					startActivity(i);
				}
				else if (url.startsWith("market://") || url.contains("play.google.com/store/apps/details?id"))
				{
					try
					{
						view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					}
					catch (ActivityNotFoundException e)
					{
						Logger.w(getClass().getSimpleName(), e);
						view.loadUrl(url);
					}
				}
				else
				{
					handleURLLoadInWebView(view, url);
				}
				return true;
			}
		};
		
		webView.getSettings().setGeolocationEnabled(allowLoc);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(client);
		webView.setWebChromeClient(new WebChromeClient()
		{
			@Override
			public void onProgressChanged(WebView view, int newProgress)
			{
				super.onProgressChanged(view, newProgress);
				bar.setProgress(newProgress);
			}
			
			@Override
	        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) 
			{
				if(allowLoc)
					callback.invoke(origin, true, false);
				else
					super.onGeolocationPermissionsShowPrompt(origin, callback);
	        }
		});
		handleURLLoadInWebView(webView, urlToLoad);
		setupActionBar(title);
		attachBridge();
	}
	
	private void attachBridge()
	{
		FullScreenJavascriptBridge mmBridge=new FullScreenJavascriptBridge(webView, this);
		webView.addJavascriptInterface(mmBridge, HikePlatformConstants.PLATFORM_BRIDGE_NAME);
	}
	
	/**
	 * 
	 * @param view
	 * @param url
	 * @return true if it has been loaded in webveiw OR false if you need to return in browser
	 */
	private boolean handleURLLoadInWebView(WebView view,String url){
		// check if feature is enabled
		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ENABLED_WHITELISTED_FEATURE, true))
		{
			WhitelistDomain domain = HikeContentDatabase.getInstance().getWhitelistedDomain(url);
			if (domain == null)
			{
				// this is case when URL is not whitelisted
				Logger.wtf("whitelist", "BLACKListed URL found " + url);
				JSONObject json  = new JSONObject();
				try
				{
					json.put(AnalyticsConstants.EVENT_KEY, HikeConstants.BLACKLIST_DOMAIN_ANALYTICS);
					json.put(HikeConstants.URL, url);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.VIEW_EVENT, json);
					startActivity(IntentFactory.getBrowserIntent(url));
					return false;
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				Toast.makeText(getApplicationContext(), R.string.some_error, Toast.LENGTH_SHORT).show();
			}
			else
			{
				if (domain.isOpenInHikeAllowed())
				{
					view.loadUrl(url); // open in webview
				}
				else
				{
					// open in browser
					startActivity(IntentFactory.getBrowserIntent(url));
					this.finish();
					return false;
				}
			}
		}
		else
		{
			view.loadUrl(url); // feature is disabled, load URL without any check
		}
		return true;
	}
	public Intent newEmailIntent(Context context, String address, String subject, String body, String cc)
	{
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { address });
		intent.putExtra(Intent.EXTRA_TEXT, body);
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_CC, cc);
		intent.setType("message/rfc822");
		return intent;
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		
		return super.onCreateOptionsMenu(menu);
	}
	
	private void setupActionBar(String titleString)
	{
		View  actionBarView = actionBar.setCustomActionBarView(R.layout.compose_action_bar);
		View backContainer = actionBarView.findViewById(R.id.back);
		backContainer.setOnClickListener(this);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(titleString);
	}
	
	private void setupMicroAppActionBar()
	{
		View  actionBarView = actionBar.setCustomActionBarView(R.layout.compose_action_bar);
		View backContainer = actionBarView.findViewById(R.id.back);
		backContainer.setOnClickListener(this);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(botInfo.getConversationName());
	}

	@Override
	public void onBackPressed()
	{
		if (webView.canGoBack())
		{
			webView.goBack();
		}
		else
		{
			super.onBackPressed();
		}
	}

	@Override
	public void onInflate(ViewStub arg0, View arg1)
	{
		
	}
	

	@Override
	public void onClick(View arg0)
	{
		switch(arg0.getId())
		{
		case R.id.back:
			finish();
			break;
		}
		
	}

	@Override
	public void onTagClicked(Tag tag)
	{
	
	}
	@Override
	protected void onDestroy()
	{
		
		if(webView!=null)
		{
			webView.removeJavascriptInterface(HikePlatformConstants.PLATFORM_BRIDGE_NAME);
		}
		super.onDestroy();
	}
}