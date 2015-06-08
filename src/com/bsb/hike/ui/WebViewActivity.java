package com.bsb.hike.ui;


import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.WindowCompat;
import android.text.TextUtils;
import android.view.KeyEvent;
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
import com.bsb.hike.bots.BotUtils;
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
import com.bsb.hike.platform.bridge.IBridgeCallback;
import com.bsb.hike.platform.bridge.NonMessagingJavaScriptBridge;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.platform.content.PlatformContentListener;
import com.bsb.hike.platform.content.PlatformContentModel;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.TagEditText.Tag;

public class WebViewActivity extends HikeAppStateBaseFragmentActivity implements OnInflateListener, OnClickListener, TagOnClickListener, OverflowItemClickListener,
		OnDismissListener, OverflowViewListener, HikePubSub.Listener, IBridgeCallback
{
	
	private static final String tag = "WebViewActivity";
	
	public static final int WEB_URL_MODE = 1; // DEFAULT MODE OF THIS ACTIVITY

	public static final int WEB_URL_WITH_BRIDGE_MODE = 2;

	public static final int MICRO_APP_MODE = 3;

	public static final String WEBVIEW_MODE = "webviewMode";

	private CustomWebView webView,secondaryWebView;
	
	private  ProgressBar bar;
	
	private HikeActionBar mActionBar;

	BotInfo botInfo;
	
	NonMessagingBotConfiguration botConfig;

	NonMessagingBotMetadata botMetaData;
	
	String msisdn;

	int mode;
	
	private NonMessagingJavaScriptBridge mmBridge;
	
	private View actionBarView;
	
	private Menu mMenu;
	
	private String[] pubsub = new String[]{HikePubSub.NOTIF_DATA_RECEIVED}; 
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		/**
		 * force the user into the reg-flow process if the token isn't set
		 */
		if (Utils.requireAuth(this))
		{
			return;
		}
		
		setMode(getIntent().getIntExtra(WEBVIEW_MODE, WEB_URL_MODE));

		if (mode == MICRO_APP_MODE)
		{
			initMsisdn();
			if (filterNonMessagingBot(msisdn))
			{
				initBot();
				if (botConfig.shouldOverlayActionBar())
				{
					getWindow().requestFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
				}
			}
			
			else
			{
				closeWebViewActivity();
				return;
			}
		}
		setContentView(R.layout.webview_activity);
		initView();	
		initActionBar();
		initAppsBasedOnMode();
		HikeMessengerApp.getPubSub().addListeners(this, pubsub);

	}

	private void closeWebViewActivity()
	{
		Intent homeintent = IntentFactory.getHomeActivityIntent(this);
		this.startActivity(homeintent);
		this.finish();
	}

	/**
	 * Basic filtering on msisdn. eg : Stealth chat check
	 * 
	 * @param msisdn
	 * @return
	 */
	private boolean filterNonMessagingBot(String msisdn)
	{
		if (msisdn == null)
		{
			throw new IllegalArgumentException("Seems You forgot to send msisdn of Bot my dear");
		}

		/**
		 * Bot marked as stealth.
		 */
		if (StealthModeManager.getInstance().isStealthMsisdn(msisdn) && !StealthModeManager.getInstance().isActive())
		{
			return false;
		}

		/**
		 * BotInfo no longer exists in the map. Possibly opening a deleted bot ?
		 */
		if (BotUtils.getBotInfoForBotMsisdn(msisdn) == null)
		{
			return false;
		}

		return true;
	}

	private void resetNotificationCounter()
	{
		Utils.resetUnreadCounterForConversation(botInfo);
	}

	/**
	 * Utility method to block orientation for a given bot
	 */
	private void checkAndBlockOrientation()
	{
		if (botConfig != null)
		{
			int orientation = botConfig.getOritentationForBot();
			if (orientation == Configuration.ORIENTATION_LANDSCAPE || orientation == Configuration.ORIENTATION_PORTRAIT)
			{
				changeCurrentOrientation(orientation);
				if (mmBridge != null)
				{
					mmBridge.orientationChanged(orientation);
				}
			}
		}
	}

	/**
	 * Utility method to change the screen orientation of the device
	 */
	private void changeCurrentOrientation(int orientation)
	{
		if (getResources().getConfiguration().orientation != orientation)
		{
			if (orientation == Configuration.ORIENTATION_LANDSCAPE)
			{
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			}
			else
			{
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
		}
		
		else
		{
			Utils.blockOrientationChange(this);
		}
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
		bar = (ProgressBar) findViewById(R.id.progress);

		if (mode == MICRO_APP_MODE)
		{
			View view = findViewById(R.id.overflow_anchor);
			LayoutParams layoutParams = view.getLayoutParams();
			if (layoutParams == null)
			{
				layoutParams = new LayoutParams((int) getResources().getDimension(R.dimen.one_dp), 0);
			}

			if (botConfig.shouldOverlayActionBar())
			{
				layoutParams.height = (int) getResources().getDimension(R.dimen.st__action_bar_default_height);
			}

			else
			{
				layoutParams.height = 0;
			}

			view.setLayoutParams(layoutParams);
		}
	}

	private void setMode(int mode)
	{
		this.mode = mode;
	}

	private void setMicroAppMode()
	{
		findViewById(R.id.progress).setVisibility(View.GONE);
		attachBridge();
		setupMicroAppActionBar();
		setupNavBar();
		setupTagPicker();
		loadMicroApp();
		checkAndBlockOrientation();
		resetNotificationCounter();
	}

	private void initMsisdn()
	{
		msisdn = getIntent().getStringExtra(HikeConstants.MSISDN);
	}

	private void initBot()
	{
		botInfo = BotUtils.getBotInfoForBotMsisdn(msisdn);
		botConfig = null == botInfo.getConfigData() ?  new NonMessagingBotConfiguration(botInfo.getConfiguration()) : new NonMessagingBotConfiguration(botInfo.getConfiguration(), botInfo.getConfigData());
		botMetaData = new NonMessagingBotMetadata(botInfo.getMetadata());
	}

	private void setWebURLMode()
	{
		String urlToLoad = getIntent().getStringExtra(HikeConstants.Extras.URL_TO_LOAD);
		String title = getIntent().getStringExtra(HikeConstants.Extras.TITLE);
		final boolean allowLoc = getIntent().getBooleanExtra(HikeConstants.Extras.WEBVIEW_ALLOW_LOCATION, false);


		

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
		if(handleURLLoadInWebView(webView, urlToLoad)){
			setupActionBar(title);
		}else {
			WebViewActivity.this.finish(); // first time if loaded in browser, then finish the activity
		}
	}
	
	private void attachBridge()
	{
		 mmBridge =new NonMessagingJavaScriptBridge(this, webView, BotUtils.getBotInfoForBotMsisdn(msisdn), this);
		 webView.addJavascriptInterface(mmBridge, HikePlatformConstants.PLATFORM_BRIDGE_NAME);
	}

	private void initActionBar()
	{
		mActionBar = new HikeActionBar(this);
	}

	@Override
	protected void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubsub);
		if(webView!=null)
		{
			webView.onActivityDestroyed();
		}
		
		secondaryWebView  =(CustomWebView) findViewById(R.id.secondaryWebView);
		if(secondaryWebView!=null)
		{
			secondaryWebView.onActivityDestroyed();
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
			List<OverFlowMenuItem> menuItemsList = getOverflowMenuItems();
			mActionBar.onCreateOptionsMenu(menu, R.menu.simple_overflow_menu, menuItemsList, this, this);
			mActionBar.setOverflowViewListener(this);
			mActionBar.setShouldAvoidDismissOnClick(true);
			
			if (menuItemsList == null || menuItemsList.isEmpty())
			{
				menu.findItem(R.id.overflow_menu).setVisible(false);
			}
			
			else
			{
				menu.findItem(R.id.overflow_menu).setVisible(true);
			}
			
			this.mMenu = menu;
			
			return true;
		}
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.overflow_menu)
		{
			showOverflowMenu();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showOverflowMenu()
	{
		overflowMenuClickedAnalytics();
		int width = getResources().getDimensionPixelSize(R.dimen.overflow_menu_width);
		int rightMargin = width + getResources().getDimensionPixelSize(R.dimen.overflow_menu_right_margin);
		mActionBar.showOverflowMenu(width, LayoutParams.WRAP_CONTENT, -rightMargin, -(int) (0.5 * Utils.scaledDensityMultiplier), findViewById(R.id.overflow_anchor));
	}

	private void overflowMenuClickedAnalytics()
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.OVERFLOW_MENU_CLICKED);
			json.put(AnalyticsConstants.BOT_NAME, botInfo.getConversationName());
			json.put(AnalyticsConstants.BOT_MSISDN, msisdn);
			HikeAnalyticsEvent.analyticsForNonMessagingBots(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}
	}

	private void setupActionBar(String titleString)
	{
		if (mode == MICRO_APP_MODE)
		{
			inflateMicroAppActionBar(titleString);
		}
		
		else
		{
			inflateWebModeActionBar(titleString);
		}
	
	}

	private void inflateWebModeActionBar(String titleString)
	{
		actionBarView = mActionBar.setCustomActionBarView(R.layout.compose_action_bar);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(titleString);
		backContainer.setOnClickListener(this);
	}

	private void inflateMicroAppActionBar(String titleString)
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
		int color = botConfig.getActionBarColor();
		/**
		 * If we don't have actionBar overlay, then we shouldn't show transparent color
		 */
		if (!botConfig.shouldOverlayActionBar() && color == R.color.transparent)
		{
			color = R.color.blue_hike;
		}
		
		updateActionBarColor(color !=-1 ? new ColorDrawable(color) : getResources().getDrawable(R.drawable.repeating_action_bar_bg));
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
				if(null != content)
				{
					webView.loadMicroAppData(content.getFormedData());
				}
			}
		});
	}

	@Override
	public void onBackPressed()
	{
		if(secondaryWebView!=null)
		{
			secondaryWebView.stopLoading();
			if(secondaryWebView.canGoBack()) // 1 is for about:blank
			{
				Logger.i(tag, "taking secondary webview back");
				secondaryWebView.goBack();
			}else{
				hideSecondaryWebView();
			}
			return;
		}
		if (mode == MICRO_APP_MODE)
		{
			if (botConfig != null && botInfo.getIsBackPressAllowed())
			{
				mmBridge.onBackPressed();
				return;
			}
			
		}
		if (mode == WEB_URL_MODE && webView.canGoBack())
		{
			webView.goBack();
		}
		else
		{
			super.onBackPressed();
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		final String notifData = (String ) object;
		if (type.equals(HikePubSub.NOTIF_DATA_RECEIVED))
		{
			if (null != mmBridge && !TextUtils.isEmpty(notifData))
			{
				mmBridge.notifDataReceived(notifData);
			}
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
			if(secondaryWebView!=null)
			{
				hideSecondaryWebView();
			}else{
			finish();
			}
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
		if (botConfig != null && botConfig.shouldShowOverflowMenu())
		{
			List<OverFlowMenuItem> items = botConfig.getOverflowItems();
			if (items != null)
			{
				list.addAll(items);
			}
		}
		return list;
	}

	@Override
	public void itemClicked(OverFlowMenuItem parameter)
	{
		switch (parameter.id)
		{
		default: 
			if (mmBridge != null)
			{
				mmBridge.onMenuItemClicked(parameter.id);
				
				if (parameter.drawableId != 0)
				{
					parameter.drawableId = parameter.drawableId == R.drawable.tick ? R.drawable.untick : R.drawable.tick;
					mActionBar.refreshOverflowMenuItem(parameter);
				}
				
				else
				{
					mActionBar.dismissOverflowMenu();
				}
			}
		}
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
		if (botInfo.isConfigDataRefreshed())
		{
			botConfig.setConfigData(botInfo.getConfigData());
			overflowItems.clear();
			overflowItems.addAll(getOverflowMenuItems());
			botInfo.setConfigDataRefreshed(false);
		}
	}
	
	private void initSecondaryWebview()
	{
		if(secondaryWebView == null)
		{
			secondaryWebView = (CustomWebView) findViewById(R.id.secondaryWebView);
			secondaryWebView.getSettings().setJavaScriptEnabled(true);
		}
	}

	@Override
	public void openFullPage(String url)
	{
		initSecondaryWebview();
		secondaryWebView.setVisibility(View.VISIBLE);
		mMenu.findItem(R.id.overflow_menu).setVisible(false);
		secondaryWebView.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon)
			{
				bar.setVisibility(View.VISIBLE);
				super.onPageStarted(view, url, favicon);
			}
			

			@Override
			public void onPageFinished(WebView view, String url)
			{
				Logger.i(tag, "onpage finished secondary " + url);
				bar.setVisibility(View.GONE);
				super.onPageFinished(view, url);
				if("about:blank".equals(url) && secondaryWebView!=null) {
					secondaryWebView.clearHistory();
					secondaryWebView.setVisibility(View.GONE);
					mMenu.findItem(R.id.overflow_menu).setVisible(true);
					secondaryWebView = null;
				}else {
					if(secondaryWebView!=null && botConfig.isJSInjectorEnabled()) {
						String js = botConfig.getJSToInject();
						if(js!=null) {
							Logger.i(tag, "loading js injection");
							secondaryWebView.loadUrl("javascript:"+js);
						}
					}
				}
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				Logger.i(tag, "url about to load in secondary " + url);
				if (url == null)
				{
					return false;
				}
				view.loadUrl(url);
				return true;
			}
		});
		Logger.i(tag, "url about to load first time in secondary " + url);
		secondaryWebView.loadUrl(url);
		
	}
	
	private void hideSecondaryWebView()
	{
		secondaryWebView.loadUrl("about:blank");
//		secondaryWebView.setVisibility(View.GONE);
		
	}

	/**
	 * This method is called on the UI thread. 
	 * It is basically intended to show the overflow menu icon
	 */
	@Override
	public void overflowMenuUpdated()
	{
		botConfig.setConfigData(botInfo.getConfigData());
		botInfo.setConfigDataRefreshed(false);
		supportInvalidateOptionsMenu();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		//Logging MicroApp Screen closing for bot case
		if (mode == MICRO_APP_MODE)
		{
			HAManager.getInstance().endChatSession(msisdn);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		//Logging MicroApp Screen opening for bot case
		if (mode == MICRO_APP_MODE)
		{
			HAManager.getInstance().startChatSession(msisdn);
		}
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_MENU)
		{
			/**
			 * We show the overflow menu only if the menu is not null and the overflow_menu icon is visible
			 */
			if (mMenu != null && mMenu.findItem(R.id.overflow_menu) != null)
			{
				if (mMenu.findItem(R.id.overflow_menu).isVisible())
				{
					showOverflowMenu();
				}
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
	
}