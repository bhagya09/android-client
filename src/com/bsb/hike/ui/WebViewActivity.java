package com.bsb.hike.ui;


import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.WindowCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.ViewStub.OnInflateListener;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.NonMessagingBotConfiguration;
import com.bsb.hike.bots.NonMessagingBotMetadata;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.media.HikeActionBar;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.media.OverFlowMenuLayout.OverflowViewListener;
import com.bsb.hike.media.OverflowItemClickListener;
import com.bsb.hike.media.TagPicker.TagOnClickListener;
import com.bsb.hike.models.WhitelistDomain;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.bridge.NonMessagingJavaScriptBridge;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.platform.content.PlatformContentListener;
import com.bsb.hike.platform.content.PlatformContentModel;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.TagEditText.Tag;

public class WebViewActivity extends HikeAppStateBaseFragmentActivity implements OnInflateListener, OnClickListener, TagOnClickListener, OverflowItemClickListener,
		OnDismissListener, OverflowViewListener
{

	private static final String tag = "WebViewActivity";
	
	public static final int WEB_URL_MODE = 1; // DEFAULT MODE OF THIS ACTIVITY

	public static final int WEB_URL_WITH_BRIDGE_MODE = 2;

	public static final int MICRO_APP_MODE = 3;

	public static final String WEBVIEW_MODE = "webviewMode";

	private CustomWebView webView;
	
	private HikeActionBar mActionBar;

	BotInfo botInfo;
	
	NonMessagingBotConfiguration botConfig;

	NonMessagingBotMetadata botMetaData;
	
	String msisdn;

	int mode;
	
	private NonMessagingJavaScriptBridge mmBridge;
	
	private View actionBarView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setMode(getIntent().getIntExtra(WEBVIEW_MODE, WEB_URL_MODE));
		if (mode == MICRO_APP_MODE)
		{
			getWindow().requestFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
		}
		setContentView(R.layout.webview_activity);
		initView();	
		initActionBar();
		initAppsBasedOnMode();
	}

	private void initAppsBasedOnMode()
	{
		if (mode == MICRO_APP_MODE)
		{
			setMicroAppMode();
		}
		else
		{
			setWebURLMode(); // default mode we consider this activity is opened for
		}
	}

	private void initView()
	{
		webView = (CustomWebView) findViewById(R.id.t_and_c_page);
	}

	private void setMode(int mode)
	{
		this.mode = mode;
	}

	private void setMicroAppMode()
	{
		msisdn = getIntent().getStringExtra(HikeConstants.MSISDN);
		if (msisdn == null)
		{
			throw new IllegalArgumentException("Seems You forgot to send msisdn of Bot my dear");
		}
		findViewById(R.id.progress).setVisibility(View.GONE);;
		attachBridge();
		initBot();
		setupMicroAppActionBar();
		setupNavBar();
		setupTagPicker();
		loadMicroApp();
	}

	private void initBot()
	{
		botInfo = BotInfo.getBotInfoForBotMsisdn(msisdn);
		if (botInfo == null)
		{
			Logger.wtf(tag, "Botinfo does not exist in map");
			this.finish();
			return;
		}
		botConfig = new NonMessagingBotConfiguration(botInfo.getConfiguration());
		botMetaData = new NonMessagingBotMetadata(botInfo.getMetadata());
	}

	private void setWebURLMode()
	{
		String urlToLoad = getIntent().getStringExtra(HikeConstants.Extras.URL_TO_LOAD);
		String title = getIntent().getStringExtra(HikeConstants.Extras.TITLE);
		final boolean allowLoc = getIntent().getBooleanExtra(HikeConstants.Extras.WEBVIEW_ALLOW_LOCATION, false);


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
				if (allowLoc)
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

		 mmBridge =new NonMessagingJavaScriptBridge(this, webView, BotInfo.getBotInfoForBotMsisdn(msisdn));
		 webView.addJavascriptInterface(mmBridge, HikePlatformConstants.PLATFORM_BRIDGE_NAME);
	}

	private void initActionBar()
	{
		mActionBar = new HikeActionBar(this);
	}

	@Override
	protected void onDestroy()
	{
		
		if(webView!=null)
		{
			webView.onActivityDestroyed();
		}
		
		if (mActionBar != null)
		{
			mActionBar.releseResources();
			mActionBar = null;
		}
		super.onDestroy();
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
		if (mActionBar == null)
		{
			initActionBar();
		}
		
		if (mode == MICRO_APP_MODE && mActionBar != null)
		{
			mActionBar.onCreateOptionsMenu(menu, R.menu.simple_overflow_menu, getOverflowMenuItems(), this, this);
			mActionBar.setOverflowViewListener(this);
			return true;
		}
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.overflow_menu)
		{
			int width = getResources().getDimensionPixelSize(R.dimen.overflow_menu_width);
			int rightMargin = width + getResources().getDimensionPixelSize(R.dimen.overflow_menu_right_margin);
			mActionBar.showOverflowMenu(width, LayoutParams.WRAP_CONTENT, -rightMargin, -(int) (0.5 * Utils.scaledDensityMultiplier), findViewById(R.id.overflow_anchor));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setupActionBar(String titleString)
	{
		actionBarView = mActionBar.setCustomActionBarView(R.layout.chat_thread_action_bar);
		View backContainer = actionBarView.findViewById(R.id.back);
		TextView title = (TextView) actionBarView.findViewById(R.id.contact_name);
		title.setText(titleString);
		
		actionBarView.findViewById(R.id.contact_status).setVisibility(View.GONE);
		
		backContainer.setOnClickListener(this);
	
	}

	private void setAvatar()
	{
		ImageView avatar = (ImageView) actionBarView.findViewById(R.id.avatar);
		if (avatar == null)
		{
			return;
		}

		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn);
		if (drawable == null)
		{
			drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(msisdn, false);
		}
		avatar.setScaleType(ScaleType.FIT_CENTER);
		avatar.setImageDrawable(drawable);
	}

	private void setupMicroAppActionBar()
	{
		setupActionBar(botInfo.getConversationName());
		updateActionBarColor(R.drawable.bg_header_transparent);
		setAvatar();
	}

	private void loadMicroApp()
	{
		// fetch micro app card

		PlatformContent.getContent(botMetaData.toString(), new PlatformContentListener<PlatformContentModel>()

		{

			public void onEventOccured(int uniqueId,PlatformContent.EventCode event)
			{
				if (event == PlatformContent.EventCode.DOWNLOADING || event == PlatformContent.EventCode.LOADED || event == EventCode.ALREADY_DOWNLOADED)
				{
					//do nothing
					return;
				}
				else
				{
					//TODO Analytics
					Logger.wtf(tag, "microapp download packet failed.");
					Toast.makeText(getApplicationContext(), "Error occured while loading " + botInfo.getLabel(), Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onComplete(PlatformContentModel content)
			{
				webView.loadMicroAppData(content.getFormedData());
			}
		});
	}

	@Override
	public void onBackPressed()
	{
		if (mode == MICRO_APP_MODE)
		{
			mmBridge.onBackPressed();
			return;
		}
		
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
		switch (arg0.getId())
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

	private List<OverFlowMenuItem> getOverflowMenuItems()
	{
		List<OverFlowMenuItem> list = new ArrayList<>();
		if (botConfig != null)
		{
			List<OverFlowMenuItem> items = botConfig.getOverflowItems();
			if (items != null)
			{
				list.addAll(items);
			}
		}
		
		list.add(new OverFlowMenuItem(getString(botInfo.isMute() ? R.string.unmute : R.string.mute), 0, 0, R.string.mute));
		list.add(new OverFlowMenuItem(getString(botInfo.isBlocked() ? R.string.unblock_title : R.string.block_title), 0, 0, R.string.block_title));
		return list;
	}

	@Override
	public void itemClicked(OverFlowMenuItem parameter)
	{
		switch (parameter.id)
		{
		case R.string.mute:
			muteClicked();
			break;
		case R.string.block_title:
			blockClicked();
			break;
			
		default: 
			if (mmBridge != null)
			{
				mmBridge.onMenuItemClicked(parameter.id);
			}
		}
	}

	private void muteClicked()
	{
		botInfo.setMute(!botInfo.isMute());
		botConfig.setConfigDataRefreshed(true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_BOT, botInfo.getMsisdn());
	}

	private void blockClicked()
	{
		botInfo.setBlocked(!botInfo.isBlocked());
		botConfig.setConfigDataRefreshed(true);
		HikeMessengerApp.getPubSub().publish(botInfo.isBlocked() ? HikePubSub.BLOCK_USER : HikePubSub.UNBLOCK_USER, botInfo.getMsisdn());
	}

	@Override
	public void onDismiss()
	{

	}
	
	private void setupNavBar()
	{
		
	}
	
	private void setupTagPicker()
	{
		
	}

	@Override
	public void onPrepareOverflowOptionsMenu(List<OverFlowMenuItem> overflowItems)
	{
		if (overflowItems == null)
		{
			return;
		}
		
		/**
		 * Updating menu conditionally
		 */
		if (botConfig.isConfigDataRefreshed())
		{
			overflowItems.clear();
			overflowItems.addAll(getOverflowMenuItems());
			botConfig.setConfigDataRefreshed(false);
		}
	}

}