package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.PendingIntent;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.MailTo;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.ViewStub.OnInflateListener;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;
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
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.media.HikeActionBar;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.media.OverFlowMenuLayout.OverflowViewListener;
import com.bsb.hike.media.OverflowItemClickListener;
import com.bsb.hike.media.TagPicker.TagOnClickListener;
import com.bsb.hike.models.MessageEvent;
import com.bsb.hike.models.WhitelistDomain;
import com.bsb.hike.platform.ContentModules.PlatformContentModel;
import com.bsb.hike.platform.CustomWebView;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformContentListener;
import com.bsb.hike.platform.PlatformUtils;
import com.bsb.hike.platform.bridge.IBridgeCallback;
import com.bsb.hike.platform.bridge.NonMessagingJavaScriptBridge;
import com.bsb.hike.platform.content.HikeWebClient;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContent.EventCode;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.TagEditText.Tag;
import com.google.gson.JsonObject;

public class WebViewActivity extends HikeAppStateBaseFragmentActivity implements OnInflateListener, TagOnClickListener, OverflowItemClickListener,
		OnDismissListener, OverflowViewListener, HikePubSub.Listener, IBridgeCallback, OnClickListener, CustomTabActivityHelper.CustomTabFallback
{
	
	private static final String tag = "WebViewActivity";
	
	public static final int WEB_URL_MODE = 1; // DEFAULT MODE OF THIS ACTIVITY

	public static final int SERVER_CONTROLLED_WEB_URL_MODE = 2;

	public static final int MICRO_APP_MODE = 3;

	public static final int WEB_URL_BOT_MODE = 4;
	
	public static final String FULL_SCREEN_AB_COLOR = "abColor";
	
	public static final String FULL_SCREEN_SB_COLOR = "sbColor";
	
	public static final String JS_TO_INJECT = "jsToInject";

	public static final String WEBVIEW_MODE = "webviewMode";

	public static final String INTERCEPT_URLS = "icpt_url";
	
	public static final String BACK_TO_ACTIVITY = "backToActivity";

	public static final String URL_PARAMETER_STRING = "url_params";

	private CustomWebView webView;
	
	private  ProgressBar bar;
	
	private HikeActionBar mActionBar;

	BotInfo botInfo;
	
	NonMessagingBotConfiguration botConfig;

	NonMessagingBotMetadata botMetaData;
	
	public  String msisdn = "";

	int mode;
	
	private NonMessagingJavaScriptBridge mmBridge;
	
	private View actionBarView;
	
	private Menu mMenu;

	private String[] pubsub = new String[]{HikePubSub.NOTIF_DATA_RECEIVED, HikePubSub.LOCATION_AVAILABLE,  HikePubSub.MESSAGE_EVENT_RECEIVED, HikePubSub.DOWNLOAD_PROGRESS};

	private boolean allowLoc;
	
	private View inflatedErrorView;
	
	private boolean webViewLoadFailed = false;

	private String microappData;

	private boolean isShortcut = false;


	private HashMap<String, Integer> interceptUrlMap;

	private final String CALLING_MSISDN = "calling_msisdn";

	// The msisdn of the WebViewActivity microapp that called this WebViewActivity (if such a case exists, null otherwise)
	String callingMsisdn;

	// Miscellaneous data received in the intent.
	private String extraData = "";

	private String urlParams;

	private long time;

	private CustomTabActivityHelper mCustomTabActivityHelper;

	private String isBackToActivity;
	public static final String KEY_CUSTOM_TABS_MENU_TITLE = "android.support.customtabs.customaction.MENU_ITEM_TITLE";
	public static final String EXTRA_CUSTOM_TABS_MENU_ITEMS = "android.support.customtabs.extra.MENU_ITEMS";
	public static final String KEY_CUSTOM_TABS_PENDING_INTENT = "android.support.customtabs.customaction.PENDING_INTENT";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		/**
		 * force the user into the reg-flow process if the token isn't set
		 */
		if (getIntent().getIntExtra(WEBVIEW_MODE, WEB_URL_MODE) == MICRO_APP_MODE && Utils.requireAuth(this))
		{
			super.onCreate(savedInstanceState);
			return;
		}


		time=System.currentTimeMillis();

		allowLoc = getIntent().getBooleanExtra(HikeConstants.Extras.WEBVIEW_ALLOW_LOCATION, false);

		microappData = getIntent().getStringExtra(HikePlatformConstants.MICROAPP_DATA);

		isShortcut = getIntent().getBooleanExtra(HikePlatformConstants.IS_SHORTCUT, false);
		
		setMode(getIntent().getIntExtra(WEBVIEW_MODE, WEB_URL_MODE));
		
		isBackToActivity =getIntent().getStringExtra( BACK_TO_ACTIVITY);

		initInterceptUrls(getIntent().getStringExtra(INTERCEPT_URLS));

		callingMsisdn = getIntent().getStringExtra(CALLING_MSISDN);

		extraData = getIntent().getStringExtra(HikePlatformConstants.EXTRA_DATA);

		urlParams = getIntent().getStringExtra(URL_PARAMETER_STRING);

		if (mode == MICRO_APP_MODE || mode == WEB_URL_BOT_MODE)
		{
			if(HikeSharedPreferenceUtil.getInstance().getData(HikePlatformConstants.CUSTOM_TABS, true) && Utils.isJellybeanOrHigher())
			{
				setupCustomTabHelper();
			}
			initMsisdn();
			JSONObject json = new JSONObject();
			try
			{
				json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_EVENT);
				json.putOpt(AnalyticsConstants.EVENT,AnalyticsConstants.MICRO_APP_OPENED);
				json.putOpt(AnalyticsConstants.BOT_MSISDN,msisdn);
			} catch (JSONException e)
			{
				e.printStackTrace();
			}
			HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT,AnalyticsConstants.MICRO_APP_OPENED,json);
			if (filterNonMessagingBot(msisdn))
			{
				initBot();
				setMicroAppTheme();
			}
			
			else
			{
				super.onCreate(savedInstanceState);
				closeWebViewActivity();
				return;
			}
		}
		
		super.onCreate(savedInstanceState);
		checkForWebViewPackageInstalled();
		setContentView(R.layout.webview_activity);
		initView();	
		initActionBar();
		initAppsBasedOnMode();
		HikeMessengerApp.getPubSub().addListeners(this, pubsub);
		alignAnchorForOverflowMenu();
		checkAndRecordBotOpen();
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

		else if (mode == WEB_URL_BOT_MODE)
		{
			setWebUrlBotMode();
		}
		
		else if (mode == SERVER_CONTROLLED_WEB_URL_MODE)
		{
			setServerControlledWebUrlMode();
		}
		
		else
		{
			setWebURLMode(); // default mode we consider this activity is opened for
		}
	}

	private void setWebUrlBotMode()
	{
		findViewById(R.id.progress).setVisibility(View.VISIBLE);
		attachBridge();
		setupMicroAppActionBar();
		handleURLBotMode();
		checkAndBlockOrientation();
	}

	private void handleURLBotMode()
	{
		webView.loadUrl(botMetaData.getUrl());
		webView.setWebViewClient(new HikeWebViewClient());
		webView.setWebChromeClient(new HikeWebChromeClient(allowLoc));
	}

	private void setServerControlledWebUrlMode()
	{
		String url = getIntent().getStringExtra(HikeConstants.Extras.URL_TO_LOAD);
		String title = getIntent().getStringExtra(HikeConstants.Extras.TITLE);
		int color = getIntent().getIntExtra(FULL_SCREEN_AB_COLOR, R.color.blue_hike);
		int sbColor = getIntent().getIntExtra(FULL_SCREEN_SB_COLOR, Color.parseColor(StatusBarColorChanger.DEFAULT_STATUS_BAR_COLOR));
		
		sbColor = (sbColor == -1) ? Color.parseColor(StatusBarColorChanger.DEFAULT_STATUS_BAR_COLOR) : sbColor;
		
		final String js = getIntent().getStringExtra(JS_TO_INJECT);
		setupWebURLWithBridgeActionBar(title, color, sbColor);
		
		
		WebViewClient mClient = new HikeWebViewClient()
		{

			@Override
			public void onPageFinished(WebView view, String url)
			{
				if (view != null && !TextUtils.isEmpty(js))
				{
					Logger.i(tag, "loading js injection");
					view.loadUrl("javascript:" + js);
				}
				super.onPageFinished(view, url);
			}
		};

		webView.setWebChromeClient(new HikeWebChromeClient(allowLoc));
		webView.setWebViewClient(mClient);
		webView.loadUrl(url);
	}

	private void initView()
	{
		webView = (CustomWebView) findViewById(R.id.t_and_c_page);
		bar = (ProgressBar) findViewById(R.id.progress);

		if (mode == MICRO_APP_MODE || mode == WEB_URL_BOT_MODE)
		{
			View view = findViewById(R.id.overflow_anchor);
			LayoutParams layoutParams = view.getLayoutParams();
			if (layoutParams == null)
			{
				layoutParams = new LayoutParams((int) getResources().getDimension(R.dimen.one_dp), 0);
			}
			layoutParams.height = 0;
			if (botConfig.shouldOverlayActionBar())
			{
				//To remove the gap since action bar should overlay the view now 
				RelativeLayout rl=(RelativeLayout)findViewById(R.id.webview_layout);
				FrameLayout.LayoutParams fp=(FrameLayout.LayoutParams)rl.getLayoutParams();
				fp.setMargins(0, 0, 0, 0);
				rl.setLayoutParams(fp);
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
		sendUrlParamsInExtraData(urlParams);
		deliverExtraDataToMicroapp(extraData);
		loadMicroApp();
		checkAndBlockOrientation();
		resetNotificationCounter();
		webView.setWebViewClient(new HikeWebViewClient());
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

		WebViewClient client = new HikeWebViewClient()
		{

			@Override
			public WebResourceResponse shouldInterceptRequest(WebView view, String url)
			{
				// TODO Auto-generated method stub
				return super.shouldInterceptRequest(view, url);
			}
		};
		
		webView.getSettings().setGeolocationEnabled(allowLoc);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(client);
		webView.setWebChromeClient(new HikeWebChromeClient(allowLoc));
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
		if(mCustomTabActivityHelper != null && Utils.isJellybeanOrHigher()) {
			mCustomTabActivityHelper.unbindCustomTabsService(this);
		}

        if(!TextUtils.isEmpty(msisdn))
            HAManager.getInstance().recordIndividualChatSession(msisdn);

        if(webView!=null)
		{
			webView.stopLoading();
			webView.onActivityDestroyed();
            webView.clearWebViewCache(true);

			if (mode == SERVER_CONTROLLED_WEB_URL_MODE || mode == WEB_URL_MODE)
			{
				webView.removeWebViewReferencesFromWebKit();
			}
		}
		
		if (mActionBar != null)
		{
			mActionBar.releseResources();
			mActionBar = null;
		}

		mmBridge = null;
		webView = null;
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
		intent.putExtra(Intent.EXTRA_CC, new String[] {cc});
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
		
		if ((mode == MICRO_APP_MODE || mode == WEB_URL_BOT_MODE) && mActionBar != null)
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
		
		else if (item.getItemId() == android.R.id.home)
		{
			if (mode == MICRO_APP_MODE || mode == WEB_URL_BOT_MODE)
			{
				if (botInfo.getIsUpPressAllowed())
				{
					mmBridge.onUpPressed();
					return true;
				}
				else if (isShortcut)
				{
					Intent intent = IntentFactory.getHomeActivityIntent(this);
					startActivity(intent);
				}
			}
			if(isBackToActivity != null){
				
				handleBackPress();
				return true;
			}
			this.finish();
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
		if (mode == MICRO_APP_MODE || mode == WEB_URL_BOT_MODE)
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

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(titleString);
	}

	private void inflateMicroAppActionBar(String titleString)
	{
		actionBarView = mActionBar.setCustomActionBarView(R.layout.chat_thread_action_bar);
		TextView title = (TextView) actionBarView.findViewById(R.id.contact_name);
		title.setText(titleString);
		
		actionBarView.findViewById(R.id.contactinfocontainer).setClickable(false);

		actionBarView.findViewById(R.id.contact_status).setVisibility(View.GONE);

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
			drawable = HikeBitmapFactory.getDefaultTextAvatar(msisdn);
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
		if (!botConfig.shouldShowTransparentActionBar() && color == R.color.transparent)
		{
			color = R.color.blue_hike;
		}

		updateActionBarColor(color !=-1 ? new ColorDrawable(color) : getResources().getDrawable(R.drawable.repeating_action_bar_bg));
		setMicroAppStatusBarColor();
		
		setAvatar();
		
	}
	
	private void setupWebURLWithBridgeActionBar(String title, int color, int statusBarColor)
	{
		setupActionBar(title);
		updateActionBarColor(color != -1 ? new ColorDrawable(color) : getResources().getDrawable(R.color.blue_hike));
		
		StatusBarColorChanger.setStatusBarColor(getWindow(), statusBarColor);
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
			public void onComplete(final PlatformContentModel content)
			{
				if(null != webView && null != content)
				{
					JSONObject json = new JSONObject();
					try
					{
						json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_EVENT);
						json.putOpt(AnalyticsConstants.EVENT,AnalyticsConstants.MICRO_APP_LOADED);
						json.putOpt(AnalyticsConstants.LOG_FIELD_6,(System.currentTimeMillis()-time));
						json.putOpt(AnalyticsConstants.BOT_MSISDN,msisdn);
					} catch (JSONException e)
					{
						e.printStackTrace();
					}

					HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.MICRO_APP_LOADED,json);
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            webView.loadMicroAppData(content.getFormedData());
                        }
                    });
				}
			}
		});
	}

	@Override
	public void onBackPressed()
	{
		if (mode == MICRO_APP_MODE || mode == WEB_URL_BOT_MODE)
		{
			if (botConfig != null && botInfo.getIsBackPressAllowed())
			{
				mmBridge.onBackPressed();
				return;
			}
			else if (isShortcut)
			{
				Intent intent = IntentFactory.getHomeActivityIntent(this);
				startActivity(intent);
			}
		}

		if(isBackToActivity != null){
			
			handleBackPress();
			return;
		}
		
		if ((mode == WEB_URL_MODE || mode == SERVER_CONTROLLED_WEB_URL_MODE) && webView.canGoBack())
		{
			webView.goBack();
		}
		else
		{
			super.onBackPressed();
		}
	}

	private void handleBackPress()
	{
		try
		{
			JSONObject json = new JSONObject(isBackToActivity);
			if(json.has(HikePlatformConstants.BACK_PROPERTY)){
				JSONObject back = json.getJSONObject(HikePlatformConstants.BACK_PROPERTY);
				if(back!=null && back.has(HikePlatformConstants.BACK_ENABLE)){
					String back_enable = back.getString(HikePlatformConstants.BACK_ENABLE);
					if(back_enable.equalsIgnoreCase("true")&&back.has(HikePlatformConstants.BACK_CONFIRMATION_TEXT)){
						String confirmationText = back.getString(HikePlatformConstants.BACK_CONFIRMATION_TEXT);
						String tiltle = "";
				    	if(back.has(HikePlatformConstants.BACK_CONFIRMATION_TITLE)){
				    		tiltle =  back.getString(HikePlatformConstants.BACK_CONFIRMATION_TITLE);
				    	}
						if(confirmationText!=null){
							HikeDialogListener nativeDialogListener = new HikeDialogListener()
							{

								@Override
								public void negativeClicked(HikeDialog hikeDialog)
								{
									hikeDialog.dismiss();
			            		}

								@Override
								public void positiveClicked(HikeDialog hikeDialog)
								{
									hikeDialog.dismiss();
									activityFinish();
									return;
								}

							

								@Override
								public void neutralClicked(HikeDialog hikeDialog)
								{
									// TODO Auto-generated method stub

								}

							};

							try
							{
								HikeDialogFactory.showDialog(WebViewActivity.this, HikeDialogFactory.MICROAPP_DIALOG, nativeDialogListener, tiltle, confirmationText, "OK", "Cancel");
							}
							catch (Exception e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
						
					}
				}
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void activityFinish()
	{
		this.finish();
		
	}
	@Override
	public void onEventReceived(String type, Object object)
	{

		if (type.equals(HikePubSub.NOTIF_DATA_RECEIVED))
		{
			if (object instanceof BotInfo)
			{
				BotInfo botInfo = (BotInfo) object;
				if (botInfo == null)
				{
					return;
				}

				if (botInfo.getMsisdn().equals(msisdn))
				{
					String notifData = botInfo.getNotifData();
					if (null != mmBridge && !TextUtils.isEmpty(botInfo.getNotifData()))
					{
						mmBridge.notifDataReceived(notifData);
					}
				}
			}
		}

		else if (type.equals(HikePubSub.MESSAGE_EVENT_RECEIVED))
		{
			if (mode != MICRO_APP_MODE && mode != WEB_URL_BOT_MODE) //We need it only Micro App mode as of now.
			{
				return;
			}

			if (object instanceof MessageEvent)
			{
				MessageEvent messageEvent = (MessageEvent) object;
				String parent_msisdn = messageEvent.getParent_msisdn();
				if (!TextUtils.isEmpty(parent_msisdn) && messageEvent.getParent_msisdn().equals(msisdn))
				{
					try
					{
						JSONObject jsonObject = PlatformUtils.getPlatformContactInfo(msisdn);
						jsonObject.put(HikePlatformConstants.EVENT_DATA, messageEvent.getEventMetadata());
						jsonObject.put(HikePlatformConstants.EVENT_ID, messageEvent.getEventId());
						jsonObject.put(HikePlatformConstants.EVENT_STATUS, messageEvent.getEventStatus());

						jsonObject.put(HikePlatformConstants.EVENT_TYPE, messageEvent.getEventType());
						if (null != mmBridge)
						{
							mmBridge.eventReceived(jsonObject.toString());
						}

					}
					catch (JSONException e)
					{
						Logger.e(tag, "JSON Exception in message event received");
					}
				}
			}
		}
		else if (type.equals(HikePubSub.LOCATION_AVAILABLE))
		{
			if (mode != MICRO_APP_MODE && mode != WEB_URL_BOT_MODE) //We need it only Micro App mode as of now.
			{
				return;
			}

			LocationManager locationManager = (LocationManager) object;
			Location location = null;
			if (locationManager != null)
			{

				location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

			}
			String latLong = PlatformUtils.getLatLongFromLocation(locationManager, location);
			if (null != mmBridge)
			{
				mmBridge.locationReceived(latLong);
			}
		}
		else if (type.equals(HikePubSub.DOWNLOAD_PROGRESS))
		{
			if (mode != MICRO_APP_MODE && mode != WEB_URL_BOT_MODE) //We need it only Micro App mode as of now.
			{
				return;
			}

			if (object instanceof Pair<?,?>)
			{
				if (null != mmBridge && null != msisdn && BotUtils.isSpecialBot(botInfo) && (msisdn.equals(botInfo.getMsisdn())|| msisdn.equals(botMetaData.getParentMsisdn())))
				{
					Pair<String, String> callback = (Pair<String, String>) object;
					mmBridge.downloadStatus(callback.first, callback.second);
				}

			}
		}

	}

	@Override
	public void onInflate(ViewStub arg0, View arg1)
	{

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
					parameter.drawableId = parameter.drawableId == R.drawable.control_check_on ? R.drawable.control_check_off : R.drawable.control_check_on;
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(data == null)
		{
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK)
		{
			if(requestCode == HikeConstants.PLATFORM_REQUEST || requestCode == HikeConstants.PLATFORM_FILE_CHOOSE_REQUEST || requestCode == HikeConstants.PLATFORM_MSISDN_FILTER_DISPLAY_REQUEST)
			{
				mmBridge.onActivityResult(requestCode,resultCode, data);
			}
		}
	}
	
	public void openFullPage(String url)
	{
		openFullPage(url, null);
	}

	public void openFullPage(String url, String interceptUrlJson)
	{
		startWebViewWithBridge(url, "", interceptUrlJson, null);
	}
	
	@Override
	public void openFullPageWithTitle(String url, String title)
	{
		startWebViewWithBridge(url, title);
	}

	@Override
	public void openFullPageWithTitle(String url, String title, String interceptUrlJson)
	{

		startWebViewWithBridge(url, title, interceptUrlJson, null);
	}
	@Override
	public void openFullPageWithTitle(String url, String title, String interceptUrlJson, String backToActivity)
	{

		startWebViewWithBridge(url, title, interceptUrlJson, backToActivity);
	}
	
	private void startWebViewWithBridge(String url, String title)
	{
		if(HikeSharedPreferenceUtil.getInstance().getData(HikePlatformConstants.CUSTOM_TABS, true) && Utils.isJellybeanOrHigher())
		{
			//TODO: Analytics impl
			openCustomTab(url, title);
		}
		else
		{
			startWebViewWithBridge(url, title, null, null);
		}
	}

	private void startWebViewWithBridge(String url, String title, String interceptUrlJson, String backToActivity)
	{

		if (TextUtils.isEmpty(title))
		{
			title = botConfig.getFullScreenTitle();
		}
		Intent intent = IntentFactory.getWebViewActivityIntent(getApplicationContext(), url, title);
		intent.putExtra(WEBVIEW_MODE, SERVER_CONTROLLED_WEB_URL_MODE);
		int color = botConfig.getFullScreenActionBarColor();
		intent.putExtra(FULL_SCREEN_AB_COLOR, color == -1 ? botConfig.getActionBarColor() : color);
		int sb_color = botConfig.getSecondaryStatusBarColor();
		intent.putExtra(FULL_SCREEN_SB_COLOR, sb_color == -1 ? botConfig.getStatusBarColor() : sb_color);
		if (!TextUtils.isEmpty(msisdn))
		{
			intent.putExtra(CALLING_MSISDN, msisdn);
		}

		if (!TextUtils.isEmpty(interceptUrlJson))
		{
			intent.putExtra(INTERCEPT_URLS, interceptUrlJson);
		}
		if (!TextUtils.isEmpty(backToActivity))
		{
			intent.putExtra(BACK_TO_ACTIVITY, backToActivity);
		}

		if (botConfig.isJSInjectorEnabled())
		{
			intent.putExtra(JS_TO_INJECT, botConfig.getJSToInject());
		}

		startActivity(intent);
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

        /*
		Logging MicroApp Screen closing for bot case
		Added SERVER_CONTROLLED_WEB_URL_MODE and callingMsisdn case here for counting user full story session time under the same micro app
		*/
		if (mode == MICRO_APP_MODE || mode == WEB_URL_BOT_MODE || mode == SERVER_CONTROLLED_WEB_URL_MODE)
		{
            if(!TextUtils.isEmpty(msisdn))
                HAManager.getInstance().endChatSession(msisdn);
            else if(!TextUtils.isEmpty(callingMsisdn))
                HAManager.getInstance().endChatSession(callingMsisdn);
        }
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
		webView.onPaused();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

        /*
		Logging MicroApp Screen opening for bot case
		Added SERVER_CONTROLLED_WEB_URL_MODE and callingMsisdn case here for counting user full story session time under the same micro app
		*/
        if (mode == MICRO_APP_MODE || mode == WEB_URL_BOT_MODE || mode == SERVER_CONTROLLED_WEB_URL_MODE)
		{
			if(!TextUtils.isEmpty(msisdn))
                HAManager.getInstance().startChatSession(msisdn);
            else if(!TextUtils.isEmpty(callingMsisdn))
                HAManager.getInstance().startChatSession(callingMsisdn);
		}
		/**
		 * Used to clear notif tray if this is opened from notification
		 */
		HikeMessengerApp.getPubSub().publish(HikePubSub.CANCEL_ALL_NOTIFICATIONS, null);
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);
		webView.onResumed();
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

	@Override
	public void changeActionBarTitle(String title)
	{
		TextView actionBarTitle = (TextView) actionBarView.findViewById(R.id.contact_name);
		actionBarTitle.setText(title);
	}

	@Override
	public void openUri(String url, String title) {
		startWebViewWithBridge(url, title, null, null);
	}


	private class HikeWebChromeClient extends WebChromeClient
	{
		boolean allowLocation;

		public HikeWebChromeClient(boolean allowLocation)
		{
			this.allowLocation = allowLocation;
		}

		@Override
		public void onProgressChanged(WebView view, int newProgress)
		{
			super.onProgressChanged(view, newProgress);
			bar.setProgress(newProgress);
		}

		@Override
		public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback)
		{
			if (allowLocation)
			{
				callback.invoke(origin, true, false);
			}
			else
			{
				super.onGeolocationPermissionsShowPrompt(origin, callback);
			}
		}
	}

	private class HikeWebViewClient extends HikeWebClient
	{
		@Override
		public void onPageFinished(WebView view, String url)
		{
			super.onPageFinished(view, url);
			if (mode != MICRO_APP_MODE)
			{
				bar.setVisibility(View.INVISIBLE);
				showErrorViewIfLoadError(view);
			}
			if (!TextUtils.isEmpty(microappData) && null != mmBridge)
			{
				mmBridge.sendMicroappIntentData(microappData);
			}
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			Logger.i(tag, "url about to load " + url);
			if (TextUtils.isEmpty(url))
			{
				return false;
			}
			interceptUrlIfRequired(url);
			if (url.startsWith("mailto:"))
			{
				try
				{
					MailTo mt = MailTo.parse(url);
					Intent i = newEmailIntent(WebViewActivity.this, mt.getTo(), mt.getSubject(), mt.getBody(), mt.getCc());
					startActivity(i);
				}
				catch (ParseException e)
				{
					e.printStackTrace();
				}

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
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				}
				catch (ActivityNotFoundException e)
				{
					Logger.w(getClass().getSimpleName(), e);
					view.loadUrl(url);
				}
			}
			else if (url.startsWith("tel:"))
			{
				startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
			}
			else
			{
				if (mode == WEB_URL_MODE)
				{
					handleURLLoadInWebView(view, url);
				}
				else
				{
					view.loadUrl(url);
				}
			}
			return true;

		}
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			if (mode != MICRO_APP_MODE)
			{
				bar.setProgress(0);
				bar.setVisibility(View.VISIBLE);
			}
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
		{
			// TODO Auto-generated method stub
			super.onReceivedError(view, errorCode, description, failingUrl);
			
			setupAndShowErrorView(view);
		}
	}

	@Override
	public void changeStatusBarColor(String color)
	{
		if (!Utils.isLollipopOrHigher())
		{
			return;
		}
		
		try
		{
			int sbColor = Color.parseColor(color);
			StatusBarColorChanger.setStatusBarColor(getWindow(), sbColor);
		}

		catch (IllegalArgumentException e)
		{
			Logger.e(tag, "Seems like you passed the wrong color");
		}
	}
	
	/**
	 * If the microapp has overlay action bar then we set the top margin as 4dp, else we set it as -52dp
	 * This is done in order to show the menu over the action bar based on material guidelines
	 */
	private void alignAnchorForOverflowMenu()
	{
		View anchor = findViewById(R.id.overflow_anchor);

		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) anchor.getLayoutParams();

		if (params == null)
		{
			params = new ViewGroup.MarginLayoutParams(getResources().getDimensionPixelSize(R.dimen.one_dp), 0);
		}

		if ((mode == MICRO_APP_MODE || mode == WEB_URL_BOT_MODE) && botConfig != null)
		{
			if (botConfig.shouldOverlayStatusBar())
				params.topMargin = ChatThreadUtils.getStatusBarHeight(getApplicationContext()) + getResources().getDimensionPixelSize(R.dimen.overflow_menu_top_margin_overlay);
			
			else if (botConfig.shouldOverlayActionBar())
				params.topMargin = getResources().getDimensionPixelSize(R.dimen.overflow_menu_top_margin_overlay);
			
			else
				params.topMargin = getResources().getDimensionPixelSize(R.dimen.overflow_menu_top_margin_non_overlay);
		}

		else
		{
			params.topMargin = getResources().getDimensionPixelSize(R.dimen.overflow_menu_top_margin_non_overlay);
		}

		anchor.setLayoutParams(params);
	}
	
	/**
	 * Should overlay status bar flag is given highest priority. If that is true, we do not respect the other flags like ShouldOverlayActionBar and DisableActionBarShadow
	 */
	private void setMicroAppTheme()
	{

		if (botConfig.shouldOverlayStatusBar())
		{
			setTheme(R.style.WebView_Theme_TranslucentStatusBar);
		}

		else
		{
			if (botConfig.shouldOverlayActionBar())
			{
				if (botConfig.disableActionBarShadow())
				{
					setTheme(R.style.WebView_Theme_ActionBar_Overlay_NoShadow);
				}

				else
				{
					setTheme(R.style.WebView_Theme_ActionBar_Overlay);
				}
			}

			else if (botConfig.disableActionBarShadow())
			{
				setTheme(R.style.WebView_Theme_NoShadow);
			}
		}
	}
	
	/**
	 * Utility method to set the status bar color of the micro-app
	 */
	private void setMicroAppStatusBarColor()
	{
		//We have translucent status bar by default in this case
		if (botConfig.shouldOverlayStatusBar())
		{
			return;
		}
		
		int sbColor = botConfig.getStatusBarColor();
		sbColor = (sbColor == -1 ) ? Color.parseColor(StatusBarColorChanger.DEFAULT_STATUS_BAR_COLOR) : sbColor;
		StatusBarColorChanger.setStatusBarColor(getWindow(), sbColor);
	}


	/**
	 * To prevent package name not found exception we check whether webview package is installed or not in Android L+.
	 * Check this for more info : 
	 * 
	 * https://code.google.com/p/chromium/issues/detail?id=506369
	 * 
	 */
	private void checkForWebViewPackageInstalled()
	{
		if (Utils.isLollipopOrHigher())
		{
			if (!Utils.appInstalledOrNot(getApplicationContext(), "com.google.android.webview"))
			{
				PlatformUtils.sendPlatformCrashAnalytics("PackageManager.NameNotFoundException", msisdn);
			}
		}
	}

	private void setupAndShowErrorView(final WebView view)
	{
		webViewLoadFailed = true;

		ViewStub stub = (ViewStub) findViewById(R.id.http_error_viewstub);
		if (stub == null)
		{
			inflatedErrorView = findViewById(R.id.http_error_viewstub_inflated);
			inflatedErrorView.setVisibility(View.VISIBLE);
		}
		else
		{
			inflatedErrorView = stub.inflate();
		}

		view.setVisibility(View.GONE);
		final Button retryButton = (Button) inflatedErrorView.findViewById(R.id.retry_button);
		
		retryButton.setOnClickListener(this);
	}

	private void showErrorViewIfLoadError(WebView view)
	{
		// webView loaded
		if (!webViewLoadFailed)
		{
			if (inflatedErrorView != null)
			{
				inflatedErrorView.setVisibility(View.GONE);
				view.setVisibility(View.VISIBLE);
			}
		}
		// webView load failed
		else
		{
			inflatedErrorView.findViewById(R.id.http_error_ll).setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void changeActionBarColor(String color)
	{
		try
		{
			int abColor = Color.parseColor(color);
			updateActionBarColor(new ColorDrawable(abColor));
		}

		catch (IllegalArgumentException e)
		{
			Logger.e(tag, "Seems like you passed the wrong color");
		}
	}

	@Override
	public void onClick(View v)
	{
		switch(v.getId())
		{
			case R.id.retry_button:
				webViewLoadFailed = false;
				initAppsBasedOnMode();
				inflatedErrorView.findViewById(R.id.http_error_ll).setVisibility(View.GONE);
				break;
		}
	}
	
	/**
	 * Used to record analytics for bot opens via push notifications
	 */
	private void checkAndRecordBotOpen()
	{
		if (getIntent() != null && getIntent().hasExtra(AnalyticsConstants.BOT_NOTIF_TRACKER))
		{
			PlatformUtils.recordBotOpenSource(msisdn, getIntent().getStringExtra(AnalyticsConstants.BOT_NOTIF_TRACKER));
		}else if (getIntent() != null && getIntent().hasExtra(AnalyticsConstants.BOT_VIA_MENU))
		{
			PlatformUtils.recordBotOpenSource(msisdn, getIntent().getStringExtra(AnalyticsConstants.BOT_VIA_MENU));
		}
	}

	public void setInterceptUrlMap(HashMap<String, Integer> interceptUrlMap)
	{
		this.interceptUrlMap = interceptUrlMap;
	}

	/**
	 * Initializes the intercept URL map from JSONString.
	 * @param urlJson
	 */
	public void initInterceptUrls(String urlJson)
	{
		if (TextUtils.isEmpty(urlJson))
		{
			Logger.e(tag, "Intercept URL json is empty. Returning.");
			return;
		}
		HashMap<String, Integer> urlToTypeMap = new HashMap<>();

		try
		{
			JSONObject urls = new JSONObject(urlJson);
			JSONArray array = urls.getJSONArray(INTERCEPT_URLS);

			for (int i = 0; i < array.length(); i++)
			{
				JSONObject tuple = (JSONObject) array.get(i);
				urlToTypeMap.put(tuple.getString(HikePlatformConstants.URL), tuple.getInt(HikePlatformConstants.TYPE));
			}
		}
		catch (JSONException e)
		{
			Logger.e(tag, "JSONException in initInterceptUrls. "+e.getMessage());
			e.printStackTrace();
		}

		setInterceptUrlMap(urlToTypeMap);
	}

	/**
	 * Checks and intercepts the interceptUrl param if it exists in the interceptUrl map.
	 * @param interceptUrl
	 */
	private void interceptUrlIfRequired(String interceptUrl)
	{
		if (this.interceptUrlMap == null || this.interceptUrlMap.isEmpty())
		{
			Logger.i(tag, "URL " + interceptUrl + " not intercepted.");
			return;
		}

		for (String url : this.interceptUrlMap.keySet())
		{
			if (interceptUrl.contains(url))
			{
				int type = this.interceptUrlMap.get(url);
				interceptUrl(interceptUrl, type);
				return;
			}
		}
	}

	/**
	 * This method contains the intercept logic for each type.
	 * @param urlToIntercept
	 * @param type
	 */
	private void interceptUrl(String urlToIntercept, int type)
	{
		if (TextUtils.isEmpty(urlToIntercept))
		{
			Logger.e(tag, "URL passed to interceptUrl is empty or null. Returning.");
			return;
		}
		switch(type)
		{
			case HikePlatformConstants.UrlInterceptTypes.INTERCEPT_AND_CLOSE_WEBVIEW:

				int index = urlToIntercept.indexOf('?');
				String params = index < 0 ? "" : urlToIntercept.substring(index);
				if (TextUtils.isEmpty(callingMsisdn))
				{
					Logger.e(tag, "callingMsisdn, the msisdn to open after URL intercept is missing. Returning.");
					return;
				}
				Intent intent = IntentFactory.getNonMessagingBotIntent(callingMsisdn, getApplicationContext());
				intent.putExtra(URL_PARAMETER_STRING, params);
				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				webView.stopLoading();

				this.finish();
				startActivity(intent);

				break;

		}
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		if (mode == MICRO_APP_MODE && getIntent().getStringExtra(HikeConstants.MSISDN).equals(intent.getStringExtra(HikeConstants.MSISDN)))
		{
			deliverUrlParamsToMicroapp(intent.getStringExtra(URL_PARAMETER_STRING));
		}
	}

	/**
	 * This method passes along the intercepted URL's parameters to the microapp.
	 * @param urlParams
	 */
	private void deliverUrlParamsToMicroapp(String urlParams)
	{
		if (mmBridge != null && urlParams != null)
		{
			mmBridge.urlIntercepted(urlParams);
		}
	}

	// Method to pass extra miscellaneous data from the intent to the microapp.
	private void deliverExtraDataToMicroapp(String data)
	{
		if (mmBridge != null)
		{
			mmBridge.setExtraData(data);
		}
	}

	// Any URL parameters received in the intent will be passed to the microapp in it's init method as part of extraData.
	private void sendUrlParamsInExtraData(String params)
	{
		if (TextUtils.isEmpty(params))
		{
			Logger.e(tag, "No params to send in extra data to Microapp.");
			return;
		}
		try
		{
			if (TextUtils.isEmpty(extraData))
			{
				JSONObject data = new JSONObject();
				data.put(URL_PARAMETER_STRING, params);
				extraData = data.toString();
			}
			else
			{
				JSONObject extraDataJson = new JSONObject(extraData);
				extraDataJson.put(URL_PARAMETER_STRING, params);
				extraData = extraDataJson.toString();
			}
		}
		catch (JSONException e)
		{
			Logger.e(tag, "JSONException in sendUrlParamsInExtraData + "+e.getMessage());
			e.printStackTrace();
		}

	}

	private void openCustomTab(String url, String title)
	{
		CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
		intentBuilder.enableUrlBarHiding();
		int titleColor = getResources().getColor(R.color.credits_blue);
		intentBuilder.setToolbarColor(titleColor);
		intentBuilder.setShowTitle(true);
		Bitmap bm = HikeBitmapFactory.drawableToBitmap(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back));
		intentBuilder.setCloseButtonIcon(bm);

		//set overflow menu
		PendingIntent sharePendingIntent = PendingIntent.getActivity(this, HikePlatformConstants.CHROME_TABS_PENDING_INTENT_SHARE, IntentFactory.getShareIntentForPlainText(url), PendingIntent.FLAG_UPDATE_CURRENT);
		intentBuilder.addMenuItem(getResources().getString(R.string.share), sharePendingIntent);

		PendingIntent forwardPendingIntent = PendingIntent.getActivity(this, HikePlatformConstants.CHROME_TABS_PENDING_INTENT_FORWARD, IntentFactory.getForwardIntentForPlainText(this, url,AnalyticsConstants.CHROME_CUSTOM_TABS), PendingIntent.FLAG_UPDATE_CURRENT);
		intentBuilder.addMenuItem(getResources().getString(R.string.forward), forwardPendingIntent);

		CustomTabsIntent intent = intentBuilder.build();
		CustomTabActivityHelper.openCustomTab(this, intent, url, this, title);
	}

	private void setupCustomTabHelper(){
		mCustomTabActivityHelper = CustomTabActivityHelper.getInstance();
		mCustomTabActivityHelper.bindCustomTabsService(this);
	}

	@Override
	protected void onStart() {
		super.onStart();


	}

	@Override
	protected void onStop() {
		super.onStop();
	}
}