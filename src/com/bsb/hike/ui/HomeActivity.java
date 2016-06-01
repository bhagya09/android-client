package com.bsb.hike.ui;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager.BadTokenException;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.AppConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.analytics.HomeAnalyticsConstants;
import com.bsb.hike.backup.AccountBackupRestore;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.db.AccountRestoreAsyncTask;
import com.bsb.hike.dialog.CustomAlertDialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.filetransfer.FTApkManager;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.models.FtueContactsData;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.offline.OfflineConstants.OFFLINE_STATE;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.snowfall.SnowFallView;
import com.bsb.hike.tasks.DownloadAndInstallUpdateAsyncTask;
import com.bsb.hike.tasks.SendLogsTask;
import com.bsb.hike.timeline.view.StatusUpdate;
import com.bsb.hike.timeline.view.TimelineActivity;
import com.bsb.hike.ui.fragments.ConversationFragment;
import com.bsb.hike.ui.utils.LockPattern;
import com.bsb.hike.utils.FestivePopup;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.HikeTip;
import com.bsb.hike.utils.HikeTip.TipType;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends HikeAppStateBaseFragmentActivity implements Listener, HikeDialogListener,
		AccountRestoreAsyncTask.IRestoreCallback
{

	public static FtueContactsData ftueContactsData = new FtueContactsData();

	private OverflowAdapter overflowAdapter;
	
	private boolean extrasClearedOut = false;

	private enum DialogShowing
	{
		SMS_CLIENT, SMS_SYNC_CONFIRMATION, SMS_SYNCING, UPGRADE_POPUP, FREE_INVITE_POPUP, FESTIVE_POPUP, VOIP_FTUE_POPUP
	}

	private DialogShowing dialogShowing;

	private boolean deviceDetailsSent;

	private View parentLayout;
	
	private TextView networkErrorPopUp;

	private Dialog dialog;

	private SharedPreferences accountPrefs;

	private Dialog progDialog, dbCorruptDialog, restoreProgDialog;

	private CustomAlertDialog updateAlert;

	private Button updateAlertOkBtn;

	private static int updateType;

	private boolean showingBlockingDialog = false; // This variable is used to prevent the normal setup of the HomeActivity. This is used when we need to show app upgrading dialog or db corrup dialog

	private PopupWindow overFlowWindow;

	private TextView newConversationIndicator;
	
	private TextView topBarIndicator;

	private View ftueAddFriendWindow;

	private boolean isAddFriendFtueShowing = false;

	private int hikeContactsCount = -1;

	private int friendsListCount = -1;

	private int recommendedCount = -1;

	private FetchContactsTask fetchContactsTask;

	private ConversationFragment mainFragment;

	private static String MAIN_FRAGMENT_TAG = "mainFragTag";

	private SnowFallView snowFallView;
	
	private int searchOptionID;
	
	private final long STEALTH_INDICATOR_DURATION = 3000;

	private String[] homePubSubListeners = { HikePubSub.UNSEEN_STATUS_COUNT_CHANGED, HikePubSub.SMS_SYNC_COMPLETE, HikePubSub.SMS_SYNC_FAIL, HikePubSub.FAVORITE_TOGGLED,
			HikePubSub.USER_JOINED, HikePubSub.USER_LEFT, HikePubSub.FRIEND_REQUEST_ACCEPTED, HikePubSub.REJECT_FRIEND_REQUEST, HikePubSub.UPDATE_OF_MENU_NOTIFICATION,
			HikePubSub.SERVICE_STARTED, HikePubSub.UPDATE_PUSH, HikePubSub.REFRESH_FAVORITES, HikePubSub.UPDATE_NETWORK_STATE, HikePubSub.CONTACT_SYNCED, HikePubSub.FAVORITE_COUNT_CHANGED,
			HikePubSub.STEALTH_UNREAD_TIP_CLICKED,HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, HikePubSub.STEALTH_INDICATOR, HikePubSub.USER_JOINED_NOTIFICATION, HikePubSub.UPDATE_OF_PHOTOS_ICON,
			HikePubSub.SHOW_NEW_CHAT_RED_DOT, HikePubSub.PRODUCT_POPUP_RECEIVE_COMPLETE, HikePubSub.OPEN_COMPOSE_CHAT_SCREEN, HikePubSub.STEALTH_MODE_TOGGLED, HikePubSub.BOT_CREATED};

	private String[] progressPubSubListeners = { HikePubSub.FINISHED_UPGRADE_INTENT_SERVICE };

	private MenuItem searchMenuItem;

	private boolean showingSearchModeActionBar = false;
	
	private static final String TAG = "HomeActivity";
	
	// Declare all Handler Msg Id here
	
	protected static final int FESTIVE_POPUP = -101;

	protected static final int SHOW_OVERFLOW_INDICATOR = -102;

	protected static final int SHOW_RECENTLY_JOINED_INDICATOR = -103;
	
	protected static final int SHOW_TIMELINE_UPDATES_INDICATOR = -104;

	protected static final int SHOW_NEW_CHAT_RED_DOT = -105;
	
	private View hiButton;

	private TextView timelineUpdatesIndicator;

	private long time;

	private AccountRestoreAsyncTask restoreAsyncTask;

	private boolean wasFragmentRemoved = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		time = System.currentTimeMillis();
		Logger.d(TAG,"onCreate");
		super.onCreate(savedInstanceState);
		
		if (!isTaskRoot())
		{
		    final Intent intent = getIntent();
		    if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(intent.getAction())) {
		        Logger.d(TAG, "Main Activity is not the root.  Finishing Main Activity instead of launching.");
		        finish();
		        return;       
		    }
		}

		if (savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.CLEARED_OUT, false)) 
		{

			Logger.d(TAG, " making extra TRUE");
			//this means that singleTop activity has been re-spawned after being destroyed 
			extrasClearedOut = true;
		}
		
		if(extrasClearedOut)
		{
			Logger.d(TAG, "clearing all data");
			//removing unwanted EXTRA becoz every time a singleTop activity is re-spawned, 
			//android system uses the old intent to fire it, and it will contain unwanted extras.
			getIntent().removeExtra(HikeConstants.STEALTH_MSISDN);
			
			//setting actions and data "null" for case of onCreate called second time 
			//example: in case of Don't Keep Activities
			//Means getIntent's Actions and Data can be used first time only
			getIntent().setAction(null);
			getIntent().setData(null);
		}

		if (Utils.requireAuth(this))
		{
			Logger.wtf(TAG, "user is not authenticated. Finishing activity");
			return;
		}

		if (HomeFtueActivity.isFtueToBeShown())
		{
			IntentFactory.freshLaunchHomeFtueActivity(HomeActivity.this);
			this.finish();
			return;
		}
				
		if (NUXManager.getInstance().showNuxScreen())
		{
			NUXManager.getInstance().startNUX(this);
			Logger.wtf(TAG, "Nux is not shown. So finishing activity");
			return;

		}

		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		HikeMessengerApp app = (HikeMessengerApp) getApplication();
		app.connectToService();

		setupActionBar();

		// Checking whether the state of the avatar and conv DB Upgrade settings
		// is 1
		// If it's 1, it means we need to show a progress dialog and then wait
		// for the
		// pub sub thread event to cancel the dialog once the upgrade is done.
		HikeMessengerApp.getPubSub().addListeners(this, progressPubSubListeners);
		
		if ((HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UPGRADING, false)))
		{
			progDialog = HikeDialogFactory.showDialog(HomeActivity.this, HikeDialogFactory.HIKE_UPGRADE_DIALOG, null);
			showingBlockingDialog = true;
		}

		if (!showingBlockingDialog && Utils.isDBCorrupt()) //If we were not showing Upgrading Dialog before
		{
			showCorruptDBRestoreDialog();
		}

		if (!showingBlockingDialog)
		{
			if (Utils.isVoipActivated(HomeActivity.this) && HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_VOIP_FTUE_POPUP, false))
			{
				dialogShowing = DialogShowing.VOIP_FTUE_POPUP;
			}
			initialiseHomeScreen(savedInstanceState);
		}
		Logger.d(getClass().getSimpleName(),"onCreate "+this.getClass().getSimpleName());

		if (!Utils.isDBCorrupt()) //Avoid making a call to show popup if Db is corrupt
		{
			showProductPopup(ProductPopupsConstants.PopupTriggerPoints.HOME_SCREEN.ordinal());
		}
		
		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STEALTH_INDICATOR_SHOW_REPEATED, false)
				|| HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STEALTH_INDICATOR_SHOW_ONCE, false))
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_INDICATOR, null);
		}
		FTApkManager.removeApkIfNeeded();
		moveToComposeChatScreen();

    }
	
	@Override
	public void handleUIMessage(Message msg)
	{
		switch (msg.what)
		{
		case FESTIVE_POPUP:
			startFestivePopup(msg.arg1);
			break;
		case SHOW_OVERFLOW_INDICATOR:
			showOverFlowIndicator(msg.arg1);
			break;
		case SHOW_RECENTLY_JOINED_INDICATOR:
			showRecentlyJoinedDot();
			break;
		case SHOW_TIMELINE_UPDATES_INDICATOR:
			showTimelineUpdatesIndicator();
			break;
		case SHOW_NEW_CHAT_RED_DOT:
			showNewChatRedDot();
		default:
			super.handleUIMessage(msg);
			break;
		}

	}
	
	private void startFestivePopup(int type)
	{
		snowFallView = FestivePopup.startAndSetSnowFallView(HomeActivity.this, type, false);
	}
	
	private void showRecentlyJoinedDot()
	{
		// Defensive check for case where newConversationIndicator was coming as null. Possible due to the various if..else conditions for newConversationIndicator initialisation.
		if(newConversationIndicator == null)
			return;
		
		boolean showNujNotif = PreferenceManager.getDefaultSharedPreferences(HomeActivity.this).getBoolean(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF, true);
		if (showNujNotif && accountPrefs.getBoolean(HikeConstants.SHOW_RECENTLY_JOINED_DOT, false))
		{
			newConversationIndicator.setText("1");
			newConversationIndicator.setVisibility(View.VISIBLE);
			newConversationIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
		}
		else
		{
			newConversationIndicator.setVisibility(View.GONE);
		}
	}
	
	private void showTimelineUpdatesIndicator()
	{
		// Defensive check for case where newConversationIndicator was coming as null. Possible due to the various if..else conditions for newConversationIndicator initialisation.
		if (timelineUpdatesIndicator == null)
		{
			return;
		}

		int count = 0;
		count = Utils.getNotificationCount(accountPrefs, true);
		if (count > 9)
		{
			timelineUpdatesIndicator.setVisibility(View.VISIBLE);
			timelineUpdatesIndicator.setText("9+");
			timelineUpdatesIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
		}
		else if (count > 0)
		{
			timelineUpdatesIndicator.setVisibility(View.VISIBLE);
			timelineUpdatesIndicator.setText(String.valueOf(count));
			timelineUpdatesIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
		}
		else
		{
			timelineUpdatesIndicator.setVisibility(View.GONE);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_TIMELINE_UPDATE_CHANGED, null);

	}
	
	private void showOverFlowIndicator(int count)
	{
		if (topBarIndicator != null)
		{
			/*
			 * Fetching the count again since it could have changed after the delay. 
			 */
			int newCount = getHomeOverflowCount(accountPrefs, false, false);
			if (newCount < 1)
			{
				topBarIndicator.setVisibility(View.GONE);
			}
			else if (newCount > 9)
			{
				topBarIndicator.setVisibility(View.VISIBLE);
				topBarIndicator.setText("9+");
				topBarIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
			}
			else if (newCount > 0)
			{
				topBarIndicator.setVisibility(View.VISIBLE);
				topBarIndicator.setText(String.valueOf(count));
				topBarIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
			}
		}
	}

	private void showNewChatRedDot()
	{
		if (newConversationIndicator != null && HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.NEW_CHAT_RED_DOT, false))
		{
			newConversationIndicator.setText("1");
			newConversationIndicator.setVisibility(View.VISIBLE);
			newConversationIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
		}
	}

	private void setupActionBar()
	{
		showingSearchModeActionBar = false;
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowCustomEnabled(false);
		actionBar.setDisplayUseLogoEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setTitle("");
		actionBar.setHomeAsUpIndicator(R.drawable.home_screen_top_bar_logo);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
		setupHikeButton();
	}

	private void setupHikeButton()
	{
		CharSequence homeButtonDescription = "hikeIcon";
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeActionContentDescription(homeButtonDescription);
		final ArrayList<View> outViews = new ArrayList<View>();
		if (getWindow().getDecorView() != null)
		{
			getWindow().getDecorView().findViewsWithText(outViews, homeButtonDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
			if (outViews.size() > 0)
				hiButton = outViews.get(0);
		}
	}

	private void flashStealthIndicatorView()
	{
		final View stealthIndicatorView;
		if(findViewById(R.id.stealth_indicator_inflated) == null)
		{
			stealthIndicatorView = ((ViewStub) findViewById(R.id.stealth_indicator_stub)).inflate();
		}
		else
		{
			stealthIndicatorView = findViewById(R.id.stealth_indicator_inflated);
		}
		stealthIndicatorView.setVisibility(View.VISIBLE);
		
		HikeTip.showTip(HomeActivity.this, TipType.STEALTH_INDICATOR, stealthIndicatorView);
		stealthIndicatorView.postDelayed(new Runnable() {
			@Override
			public void run() {
				stealthIndicatorView.setVisibility(View.GONE);
			}
		}, STEALTH_INDICATOR_DURATION);
	}

	private void initialiseHomeScreen(Bundle savedInstanceState)
	{
		if (showingBlockingDialog) //If showing any blocking dialog, then return from here.
		{
			return;
		}

		setContentView(R.layout.home);

		parentLayout = findViewById(R.id.parent_layout);

		networkErrorPopUp = (TextView) findViewById(R.id.network_error);

		if (savedInstanceState != null)
		{
			deviceDetailsSent = savedInstanceState.getBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT);
			int dialogShowingOrdinal = savedInstanceState.getInt(HikeConstants.Extras.DIALOG_SHOWING, -1);
			if (dialogShowingOrdinal != -1)
			{
				dialogShowing = DialogShowing.values()[dialogShowingOrdinal];
			}
		}
		else
		{
			// check the preferences and show update
			updateType = accountPrefs.getInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.NO_UPDATE);
			showUpdatePopup(updateType);
		}

		setupMainFragment(savedInstanceState);
		initialiseTabs();

		setupFestivePopup();

		if (savedInstanceState == null && dialogShowing == null)
		{
			
				/*
				 * Only show app rater if the tutorial is not being shown an the app was just launched i.e not an orientation change
				 */
		}
		else if (dialogShowing != null)
		{
			showAppropriateDialog();
		}

		HikeMessengerApp.getPubSub().addListeners(this, homePubSubListeners);

		GetFTUEContactsTask getFTUEContactsTask = new GetFTUEContactsTask();
		getFTUEContactsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void setupFestivePopup()
	{
		final int festivePopupType = accountPrefs.getInt(HikeConstants.SHOW_FESTIVE_POPUP, -1);
		if (festivePopupType == FestivePopup.HOLI_POPUP)
		{
			if(FestivePopup.isPastFestiveDate(festivePopupType))
			{
				HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.SHOW_FESTIVE_POPUP);
			}
			else if(dialogShowing == null)
			{
				ViewStub festiveView = (ViewStub) findViewById(R.id.festive_view_stub);
				festiveView.setOnInflateListener(new ViewStub.OnInflateListener()
				{
					@Override
					public void onInflate(ViewStub stub, View inflated)
					{
						startFestiveView(festivePopupType);
					}
				});
				festiveView.inflate();
			}
		}

	}
	
	private void startFestiveView(final int type)
	{
		Utils.blockOrientationChange(HomeActivity.this);
		dialogShowing = DialogShowing.FESTIVE_POPUP;
		getSupportActionBar().hide();

		if(snowFallView == null)
		{
			Message msg = Message.obtain();
			msg.arg1 = type;
			msg.what = FESTIVE_POPUP;
			uiHandler.sendMessageDelayed(msg, 300);
			
		}
	}

	private void setupMainFragment(Bundle savedInstanceState)
	{
		Fragment frag = getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG);
		if (frag != null)
		{
			mainFragment = (ConversationFragment) frag;
		}

		if (mainFragment == null)
		{
			mainFragment = new ConversationFragment();
			
			getSupportFragmentManager().beginTransaction().add(R.id.home_screen, mainFragment, MAIN_FRAGMENT_TAG).commitAllowingStateLoss();

			wasFragmentRemoved = false;
		}

		if (wasFragmentRemoved && (mainFragment != null))
		{
			getSupportFragmentManager().beginTransaction().add(R.id.home_screen, mainFragment, MAIN_FRAGMENT_TAG).commitAllowingStateLoss();
			wasFragmentRemoved = false;
		}
	}

	public void onFestiveModeBgClick(View v)
	{
		return;
	}

	public void showActionBarAfterFestivePopup()
	{
		dialogShowing = null;
		// Bringing back action bar & unblocking orientation
		getSupportActionBar().show();
		Utils.unblockOrientationChange(this);
	}

	@Override
	protected void onDestroy()
	{
		Logger.d(TAG, "onDestroy");
		if (progDialog != null)
		{
			progDialog.dismiss();
			progDialog = null;
		}
		if (overFlowWindow != null && overFlowWindow.isShowing())
			overFlowWindow.dismiss();
		HikeMessengerApp.getPubSub().removeListeners(this, homePubSubListeners);
		HikeMessengerApp.getPubSub().removeListeners(this, progressPubSubListeners);
		if (searchMenuItem != null && MenuItemCompat.getActionView(searchMenuItem) != null)
		{
			SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
			searchView.setOnQueryTextListener(null);
			searchView.clearFocus();
		}
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STEALTH_INDICATOR_ANIM_ON_RESUME, HikeConstants.STEALTH_INDICATOR_RESUME_RESET);

		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		Logger.d(getClass().getSimpleName(), "onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent);
		if (Utils.requireAuth(this))
		{
			return;
		}

		if (NUXManager.getInstance().showNuxScreen())
		{
			NUXManager.getInstance().startNUX(this);
			return;
		}
		
		if (mainFragment != null)
		{
			mainFragment.onNewintent(intent);
		}
		if(showingSearchModeActionBar)
		{
			MenuItemCompat.getActionView(searchMenuItem).clearFocus();
			MenuItemCompat.collapseActionView(searchMenuItem);
		}
			
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.HOME_SCREEN.ordinal());
	}

	private void showSmsOrFreeInvitePopup()
	{
		if (dialogShowing == null)
		{
			if (!accountPrefs.getBoolean(HikeMessengerApp.SHOWN_SMS_CLIENT_POPUP, true))
			{
				showSMSClientDialog();
			}
			else if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_FREE_INVITE_POPUP, false))
			{
				showFreeInviteDialog();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		/**
		 * This is a strange bug in Android 5.1. If we call finish to an activity from onCreate, ideally onCreateOptions menu should not have been called. But in Droid 5.1 this is
		 * being called. This check is defensive in nature
		 */
		if (isFinishing())
		{
			Logger.wtf(TAG, "Activity is finishing yet onCreateOptionsMenu is being called");
			return false;
		}
		
		if (showingBlockingDialog)
		{
			return false;
		}
		else
		{
			return setupMenuOptions(menu);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		return super.onPrepareOptionsMenu(menu);
	}
	
	private boolean setupMenuOptions(final Menu menu)
	{
		try
		{
			getMenuInflater().inflate(R.menu.chats_menu, menu);
			topBarIndicator = (TextView) (MenuItemCompat.getActionView(menu.findItem(R.id.overflow_menu)).findViewById(R.id.top_bar_indicator_text));
			updateOverFlowMenuNotification();
			MenuItemCompat.getActionView(menu.findItem(R.id.overflow_menu)).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					recordOverFlowMenuClick();
					showOverFlowMenu();
					topBarIndicator.setVisibility(View.GONE);
					Editor editor = accountPrefs.edit();
					editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, true);
					editor.commit();
				}
			});
			searchMenuItem = menu.findItem(R.id.search);
			SearchView searchView=(SearchView) MenuItemCompat.getActionView(searchMenuItem);
			searchView.setOnQueryTextListener(onQueryTextListener);
			searchView.setQueryHint(getString(R.string.search));
			searchView.clearFocus();
			searchOptionID = searchMenuItem.getItemId();
			MenuItemCompat.setShowAsAction(MenuItemCompat.setActionView(searchMenuItem, searchView), MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
			MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener()
			{
				@Override
				public boolean onMenuItemActionExpand(MenuItem item)
				{
					if(mainFragment!=null)
			        {
						mainFragment.setupSearch();
			        }
					toggleMenuItems(menu, false);
					showProductPopup(ProductPopupsConstants.PopupTriggerPoints.SEARCH.ordinal());
					showingSearchModeActionBar = true;
					if (hiButton != null)
					{
						hiButton.clearAnimation();
					}

					return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item)
			{
				if(mainFragment!=null)
		        {
					mainFragment.removeSearch();
		        }
				toggleMenuItems(menu, true);
				setupActionBar();
				return true;
			}
			});
		
			newConversationIndicator = (TextView) MenuItemCompat.getActionView(menu.findItem(R.id.new_conversation)).findViewById(R.id.top_bar_indicator_text);
			MenuItemCompat.getActionView(menu.findItem(R.id.new_conversation)).findViewById(R.id.overflow_icon_image).setContentDescription("Start a new chat");
			((ImageView) MenuItemCompat.getActionView(menu.findItem(R.id.new_conversation)).findViewById(R.id.overflow_icon_image))
					.setImageResource(R.drawable.ic_new_conversation);

			View timelineActionView = (View) MenuItemCompat.getActionView(menu.findItem(R.id.timeline));
			((ImageView) timelineActionView.findViewById(R.id.overflow_icon_image)).setImageResource(R.drawable.ic_timeline);
			timelineActionView.findViewById(R.id.overflow_icon_image).setContentDescription("Timeline");
			timelineUpdatesIndicator = (TextView) timelineActionView.findViewById(R.id.top_bar_indicator_text);
			showTimelineUpdatesDot(1000);
			timelineActionView.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					try
					{
						JSONObject md = new JSONObject();
						md.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SHOW_TIMELINE_TOP_BAR);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, md);
					}
					catch (JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}

					JSONObject metadataSU = new JSONObject();
					try
					{
						metadataSU.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.TIMELINE_OPEN);
						if (Utils.getNotificationCount(accountPrefs, false) > 0)
						{
							metadataSU.put(AnalyticsConstants.EVENT_SOURCE, HikeConstants.LogEvent.TIMELINE_WITH_RED_DOT);
						}

						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadataSU);
					}
					catch (JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}

					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_TIMELINE_RED_DOT, false);
					Intent intent = new Intent(HomeActivity.this, TimelineActivity.class);
					intent.putExtra(TimelineActivity.TIMELINE_SOURCE, TimelineActivity.TimelineOpenSources.HOME_ACTIVITY);
					startActivity(intent);
				}
			});

			showRecentlyJoinedDot(1000);
			sendUIMessage(SHOW_NEW_CHAT_RED_DOT, 1000);

			MenuItemCompat.getActionView(menu.findItem(R.id.new_conversation)).setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Logger.d(HikeConstants.COMPOSE_SCREEN_OPENING_BENCHMARK, "start=" + System.currentTimeMillis());
					try
					{
						JSONObject metadata = new JSONObject();
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.NEW_CHAT_FROM_TOP_BAR);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					}
					catch (JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}

					Intent intent = IntentFactory.getComposeChatIntentWithBotDiscovery(HomeActivity.this);

					newConversationIndicator.setVisibility(View.GONE);
					HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_USER_JOINED, new Integer(0));
					startActivity(intent);
				}
			});

			return true;
		}
		catch (NullPointerException e)
		{
			Logger.e("NulllpointerException :setupMenuOptions", e.toString());
			return false;
		}
	}

	private void recordSearchOptionClick()
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.HOME_SEARCH);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	private void toggleMenuItems(Menu menu, boolean value)
	{
		menu.findItem(R.id.overflow_menu).setVisible(value);
		menu.findItem(R.id.new_conversation).setVisible(value);
		menu.findItem(R.id.timeline).setVisible(value);
	}

	private OnQueryTextListener onQueryTextListener = new OnQueryTextListener()
	{
		@Override
		public boolean onQueryTextSubmit(String query)
		{
			Utils.hideSoftKeyboard(getApplicationContext(), searchMenuItem.getActionView());
			return true;
		}

		@Override
		public boolean onQueryTextChange(String newText)
		{
			if(mainFragment!=null)
	        {
				mainFragment.onSearchQueryChanged(newText.toString());
	        }
			return true;
		}
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			hikeLogoClicked();
			break;
		}

		if (item.getItemId() == searchOptionID)
		{
			recordSearchOptionClick();
		}

		return true;
	}

	private void showSMSClientDialog()
	{
		dialogShowing = DialogShowing.SMS_CLIENT;

		dialog = new Dialog(this, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.sms_with_hike_popup);
		dialog.setCancelable(false);

		Button okBtn = (Button) dialog.findViewById(R.id.btn_ok);
		Button cancelBtn = (Button) dialog.findViewById(R.id.btn_cancel);

		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Utils.setReceiveSmsSetting(getApplicationContext(), true);

				Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
				editor.putBoolean(HikeConstants.SEND_SMS_PREF, true);
				editor.commit();

				dialogShowing = null;
				dialog.dismiss();
				if (!accountPrefs.getBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false))
				{
					showSMSSyncDialog();
				}
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Utils.setReceiveSmsSetting(getApplicationContext(), false);
				dialogShowing = null;
				dialog.dismiss();
			}
		});

		dialog.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss(DialogInterface dialog)
			{
				Editor editor = accountPrefs.edit();
				editor.putBoolean(HikeMessengerApp.SHOWN_SMS_CLIENT_POPUP, true);
				editor.commit();
			}
		});
		dialog.show();
	}

	private void showSMSSyncDialog()
	{
		if (dialogShowing == null)
		{
			dialogShowing = DialogShowing.SMS_SYNC_CONFIRMATION;
		}

		dialog = HikeDialogFactory.showDialog(this, HikeDialogFactory.SMS_SYNC_DIALOG, dialogShowing == DialogShowing.SMS_SYNC_CONFIRMATION);
	}

	private void showFreeInviteDialog()
	{
		/*
		 * We don't send free invites for non indian users.
		 */
		if (!HikeMessengerApp.isIndianUser())
		{
			return;
		}

		dialogShowing = DialogShowing.FREE_INVITE_POPUP;

		dialog = new Dialog(this, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.free_invite_popup);
		dialog.setCancelable(false);

		TextView header = (TextView) dialog.findViewById(R.id.header);
		TextView body = (TextView) dialog.findViewById(R.id.body);
		ImageView image = (ImageView) dialog.findViewById(R.id.image);

		String headerText = accountPrefs.getString(HikeMessengerApp.FREE_INVITE_POPUP_HEADER, "");
		String bodyText = accountPrefs.getString(HikeMessengerApp.FREE_INVITE_POPUP_BODY, "");

		if (TextUtils.isEmpty(headerText))
		{
			headerText = getString(R.string.free_invite_header);
		}

		if (TextUtils.isEmpty(bodyText))
		{
			bodyText = getString(R.string.free_invite_body);
		}

		header.setText(headerText);
		body.setText(bodyText);

		Button okBtn = (Button) dialog.findViewById(R.id.btn_ok);
		Button cancelBtn = (Button) dialog.findViewById(R.id.btn_cancel);

		final boolean showingRewardsPopup = !accountPrefs.getBoolean(HikeMessengerApp.FREE_INVITE_POPUP_DEFAULT_IMAGE, true);

		if (image != null)
		{
			image.setImageResource(!showingRewardsPopup ? R.drawable.ic_free_sms_default : R.drawable.ftue_card_invite_img_small);
		}

		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dialog.dismiss();

				Intent intent = new Intent(HomeActivity.this, HikeListActivity.class);
				startActivity(intent);

				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, showingRewardsPopup ? HikeConstants.LogEvent.INVITE_FRIENDS_FROM_POPUP_REWARDS : HikeConstants.LogEvent.INVITE_FRIENDS_FROM_POPUP_FREE_SMS);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dialog.dismiss();
			}
		});

		dialog.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss(DialogInterface dialog)
			{
				Editor editor = accountPrefs.edit();
				editor.putBoolean(HikeMessengerApp.SHOW_FREE_INVITE_POPUP, false);
				editor.commit();

				dialogShowing = null;
			}
		});

		dialog.show();
	}

	private class FetchContactsTask extends AsyncTask<Void, Void, Void>
	{
		List<ContactInfo> hikeContacts = new ArrayList<ContactInfo>();

		List<ContactInfo> friendsList = new ArrayList<ContactInfo>();

		List<ContactInfo> recommendedContacts = new ArrayList<ContactInfo>();

		@Override
		protected Void doInBackground(Void... arg0)
		{
			Utils.getRecommendedAndHikeContacts(HomeActivity.this, recommendedContacts, hikeContacts, friendsList);
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			hikeContactsCount = hikeContacts.size();
			recommendedCount = recommendedContacts.size();
			friendsListCount = friendsList.size();
			super.onPostExecute(result);
		}
	}

	@Override
	protected void onResume() {
		Logger.d(TAG,"onResume");
		super.onResume();

		checkNShowNetworkError();

		showSmsOrFreeInvitePopup();
	
		HikeMessengerApp.getPubSub().publish(HikePubSub.CANCEL_ALL_NOTIFICATIONS, null);
		
		if(getIntent() != null)
		{
			acceptGroupMembershipConfirmation(getIntent());
		}

		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STEALTH_INDICATOR_ANIM_ON_RESUME, HikeConstants.STEALTH_INDICATOR_RESUME_RESET) == HikeConstants.STEALTH_INDICATOR_RESUME_ACTIVE)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.STEALTH_INDICATOR_ANIM_ON_RESUME, HikeConstants.STEALTH_INDICATOR_RESUME_EXPIRED);
			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_INDICATOR, null);
		}

		checkAndShowCorruptDbDialog();
		Logger.d(HikeConstants.APP_OPENING_BENCHMARK, "Time taken between onCreate and onResume of HomeActivity = " + (System.currentTimeMillis() - time));
		recordActivityEndTime();
	}

	
	private void acceptGroupMembershipConfirmation(Intent intent)
	{
		String action = intent.getAction();
		String linkUrl = intent.getDataString();
		int flags = intent.getFlags();
		
		if ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) 
		{
		    // The activity was launched from history
			return;
		}
		
		if (TextUtils.isEmpty(action) || TextUtils.isEmpty(linkUrl))
		{
			//finish();
			return;
		}
		
		if (linkUrl.contains(HttpRequestConstants.BASE_LINK_SHARING_URL))
		{
			//linkurl is http://hike.in/refid:gc:code
			String codeArray[] = linkUrl.split("/");
			if(codeArray.length < 4 || !linkUrl.contains(":gc:"))
			{
				Logger.d("link_share_error", "The linkurl is wrong, either no :gc: present or split in '/' is < 4 " + linkUrl);
				return;
			}
			
			String code = codeArray[3];
			RequestToken requestToken = HttpRequests.acceptGroupMembershipConfirmationRequest(code, new IRequestListener()
			{
				
				@Override
				public void onRequestSuccess(Response result)
				{
				}
				
				@Override
				public void onRequestProgressUpdate(float progress)
				{
				}
				
				@Override
				public void onRequestFailure(HttpException httpException)
				{
					String errorText = "";

					Logger.d("link_share_error", "The error code received is " + httpException.getErrorCode());
					
					switch (httpException.getErrorCode())
					{

					// 406: “The person who invited you has deleted their account”
					case HttpURLConnection.HTTP_NOT_ACCEPTABLE:
						errorText = getString(R.string.link_share_error_invitee_account_deleted);
						break;

					// 400: “You’re already in the group” 
					case HttpURLConnection.HTTP_BAD_REQUEST:
						errorText = getString(R.string.link_share_error_already_group_member);
						break;

					// 16: “This link is invalid”
					// 401: “This link is invalid”
					case HttpURLConnection.HTTP_UNAUTHORIZED:
					case HttpException.REASON_CODE_UNKNOWN_HOST_EXCEPTION:
						errorText = getString(R.string.link_share_error_invalid_link);
						break;
						
					// 410: “This group has been deleted”
					case HttpURLConnection.HTTP_GONE:
						errorText = getString(R.string.link_share_error_group_deleted);
						break;

					// 412: “The person who invited you is not in the group anymore”
					case HttpURLConnection.HTTP_PRECON_FAILED:
						errorText = getString(R.string.link_share_error_person_not_in_group);
						break;

					// 1:- NO Internet connectivity
					case HttpException.REASON_CODE_NO_NETWORK:
						errorText = getString(R.string.link_share_network_error);
						break;

					default:
						errorText = getString(R.string.link_share_error_default);
						break;
					}

					// Show Toast
					Toast.makeText(HomeActivity.this, errorText, Toast.LENGTH_SHORT).show();
				}
			});
			requestToken.execute();
		}

	
	}

	@Override
	public void onBackPressed()
	{
		// The following change checks if search mode is still there, and takes action accordingly
		if (searchMenuItem != null && searchMenuItem.isActionViewExpanded())
		{
			searchMenuItem.collapseActionView();
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onPause()
	{
		Logger.d(TAG,"onPause");
		String data = getIntent().getDataString();
		boolean isDataSet = TextUtils.isEmpty(data)  ? false : data.contains(HttpRequestConstants.BASE_LINK_SHARING_URL);
		if(getIntent().hasExtra(HikeConstants.STEALTH_MSISDN) || isDataSet)
		{
			//after showing the LockPatternActivity in onResume of ConvFrag the extra is no longer needed, so clearing it out.
			extrasClearedOut = true;
			getIntent().setAction(null);
			getIntent().setData(null);
			getIntent().removeExtra(HikeConstants.STEALTH_MSISDN);
		}
		super.onPause();
	}

	@Override
	protected void onStart()
	{
		Logger.d(getClass().getSimpleName(), "onStart");
		super.onStart();
		long t1, t2;
		t1 = System.currentTimeMillis();
		Utils.clearJar(this);
		t2 = System.currentTimeMillis();
		Logger.d("clearJar", "time : " + (t2 - t1));
	}

	
	@Override
	protected void onStop()
	{
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		Logger.d(TAG,"onsavedInstance");
		outState.putBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT, deviceDetailsSent);
		if (dialog != null && dialog.isShowing())
		{
			outState.putInt(HikeConstants.Extras.DIALOG_SHOWING, dialogShowing != null ? dialogShowing.ordinal() : -1);
		}
		outState.putBoolean(HikeConstants.Extras.IS_HOME_POPUP_SHOWING, overFlowWindow != null && overFlowWindow.isShowing());
		outState.putInt(HikeConstants.Extras.FRIENDS_LIST_COUNT, friendsListCount);
		outState.putInt(HikeConstants.Extras.HIKE_CONTACTS_COUNT, hikeContactsCount);
		outState.putInt(HikeConstants.Extras.RECOMMENDED_CONTACTS_COUNT, recommendedCount);
		//saving the extrasClearedOut value to be used in onCreate, in case the activity is destroyed and re-spawned using old Intent

		Logger.d(TAG," setting value  of EXTRTA  " + extrasClearedOut);
		outState.putBoolean(HikeConstants.Extras.CLEARED_OUT, extrasClearedOut);
		super.onSaveInstanceState(outState);
	}

	private void sendDeviceDetails()
	{
		Utils.sendDeviceDetails(HomeActivity.this, false, false);
		deviceDetailsSent = true;
	}

	private void updateApp(int updateType)
	{
		if (TextUtils.isEmpty(this.accountPrefs.getString(HikeConstants.Extras.UPDATE_URL, "")))
		{
			Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
			marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			try
			{
				startActivity(marketIntent);
			}
			catch (ActivityNotFoundException e)
			{
				Logger.e(HomeActivity.class.getSimpleName(), "Unable to open market");
			}
			if (updateType == HikeConstants.NORMAL_UPDATE)
			{
				updateAlert.dismiss();
			}
		}
		else
		{
			// In app update!

			updateAlertOkBtn.setText(R.string.downloading_string);
			updateAlertOkBtn.setEnabled(false);

			DownloadAndInstallUpdateAsyncTask downloadAndInstallUpdateAsyncTask = new DownloadAndInstallUpdateAsyncTask(this, accountPrefs.getString(
					HikeConstants.Extras.UPDATE_URL, ""));
			downloadAndInstallUpdateAsyncTask.execute();
		}
	}

	@SuppressLint("NewApi")
	private void initialiseTabs()
	{
		invalidateOptionsMenu();
	}

	private boolean showUpdateIcon;

	private void showUpdatePopup(final int updateType)
	{
		//TODO wrong check in place can cause other features to fail
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.AutoApkDownload.UPDATE_FROM_DOWNLOADED_APK,false))
		{
			return;
		}
		if (updateType == HikeConstants.NO_UPDATE)
		{
			return;
		}

		if (!Utils.isUpdateRequired(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.Extras.LATEST_VERSION, ""), this.getApplicationContext()))
		{
			return;
		}

		if (updateType == HikeConstants.NORMAL_UPDATE)
		{
			// Here we check if the user cancelled the update popup for this
			// version earlier
			String updateToIgnore = accountPrefs.getString(HikeConstants.Extras.UPDATE_TO_IGNORE, "");
			if (!TextUtils.isEmpty(updateToIgnore) && updateToIgnore.equals(accountPrefs.getString(HikeConstants.Extras.LATEST_VERSION, "")))
			{
				return;
			}
		}

		// If we are already showing an update we don't need to do anything else
		if (updateAlert != null && updateAlert.isShowing())
		{
			return;
		}
		dialogShowing = DialogShowing.UPGRADE_POPUP;
		
		updateAlert = new CustomAlertDialog(this, -1);
		HikeDialogListener dialogListener = new HikeDialogListener()
		{
			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				updateApp(updateType);
			}
			
			@Override
			public void neutralClicked(HikeDialog hikeDialog)
			{
			}
			
			@Override
			public void negativeClicked(HikeDialog hikeDialog)
			{
				hikeDialog.cancel();
				dialogShowing = null;
			}
		};

		updateAlert.setTitle(updateType == HikeConstants.CRITICAL_UPDATE ? R.string.critical_update_head : R.string.normal_update_head);
		updateAlert.setMessage(accountPrefs.getString(HikeConstants.Extras.UPDATE_MESSAGE, ""));

		updateAlert.setPositiveButton(R.string.UPDATE_APP, dialogListener);
		if (updateType != HikeConstants.CRITICAL_UPDATE)
		{
			updateAlert.setNegativeButton(R.string.CANCEL, dialogListener);
		}

		updateAlert.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				if (updateType == HikeConstants.CRITICAL_UPDATE)
				{
					finish();
				}
				else
				{
					Editor editor = accountPrefs.edit();
					editor.putString(HikeConstants.Extras.UPDATE_TO_IGNORE, accountPrefs.getString(HikeConstants.Extras.LATEST_VERSION, ""));
					editor.commit();
				}
			}
		});

		updateAlertOkBtn = (Button) updateAlert.findViewById(R.id.btn_positive);
		updateAlert.show();
	}

	/**
	 * This method returns sum of timeline status count and hike extras + rewards
	 * 
	 * @param accountPrefs
	 * @param countUsersStatus
	 *            Whether to include user status count in the total
	 * @param defaultValue
	 *            default value for hike extras and rewards if key is not present in shared preferences
	 * @return
	 */
	private int getHomeOverflowCount(SharedPreferences accountPrefs, boolean countUsersStatus, boolean defaultValue)
	{
		return Utils.updateHomeOverflowToggleCount(accountPrefs, defaultValue);
	}
	
	@Override
	public void onEventReceived(String type, Object object)
	{
		super.onEventReceived(type, object);
		if (HikePubSub.UNSEEN_STATUS_COUNT_CHANGED.equals(type) || HikePubSub.STEALTH_MODE_TOGGLED.equals(type) || HikePubSub.FAVORITE_COUNT_CHANGED.equals(type))
		{
			runOnUiThread( new Runnable()
			{
				@Override
				public void run()
				{
					showTimelineUpdatesDot(1000);
				}
			});
		}
		else if (type.equals(HikePubSub.FINISHED_UPGRADE_INTENT_SERVICE))
		{
			Logger.d("Migration", "FINISHED_UPGRADE_INTENT_SERVICE received in home activity ");

			runOnUiThread(new Runnable()
			{
				@SuppressLint("NewApi")
				@Override
				public void run()
				{
					HikeMessengerApp.getPubSub().removeListeners(HomeActivity.this, progressPubSubListeners);

					showingBlockingDialog = false;
					if (progDialog != null)
					{
						progDialog.dismiss();
						progDialog = null;
					}

					if (restoreProgDialog != null)
					{
						restoreProgDialog.dismiss();
						restoreProgDialog = null;
					}

					Logger.d("Migration", "dismissed diaglogues in home activity");

					if (Utils.isDBCorrupt())
					{
						showCorruptDBRestoreDialog();
					}

					invalidateOptionsMenu();
					initialiseHomeScreen(null);
				}
			});
		}
		else if (HikePubSub.SMS_SYNC_COMPLETE.equals(type) || HikePubSub.SMS_SYNC_FAIL.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (dialog != null)
					{
						dialog.dismiss();
					}
					dialogShowing = null;
				}
			});
		}
		else if (HikePubSub.FAVORITE_TOGGLED.equals(type) || HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type) || HikePubSub.REJECT_FRIEND_REQUEST.equals(type))
		{
			Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			if (ftueContactsData.isEmpty())
			{
				return;
			}
			ContactInfo favoriteToggleContact = favoriteToggle.first;

			for (ContactInfo contactInfo : ftueContactsData.getCompleteList())
			{
				if (contactInfo.getMsisdn().equals(favoriteToggleContact.getMsisdn()))
				{
					contactInfo.setFavoriteType(favoriteToggle.second);
					HikeMessengerApp.getPubSub().publish(HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, null);
					break;
				}
			}
		}
		else if (HikePubSub.USER_JOINED.equals(type) || HikePubSub.USER_LEFT.equals(type))
		{
			if (ftueContactsData.isEmpty())
			{
				return;
			}
			
			String msisdn = (String) object;
			
			for (ContactInfo contactInfo : ftueContactsData.getCompleteList())
			{
				if (contactInfo.getMsisdn().equals(msisdn))
				{
					contactInfo.setOnhike(HikePubSub.USER_JOINED.equals(type));
					HikeMessengerApp.getPubSub().publish(HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, null);
					break;
				}
			}
		}
		else if (HikePubSub.USER_JOINED_NOTIFICATION.equals(type))
		{
			showRecentlyJoinedDot(1000);
		}
		else if (HikePubSub.UPDATE_OF_MENU_NOTIFICATION.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					updateOverFlowMenuNotification();
					if (null != overflowAdapter)
					{
						overflowAdapter.notifyDataSetChanged();
					}
				}
			});
		}
		else if (HikePubSub.SERVICE_STARTED.equals(type))
		{
			boolean justSignedUp = accountPrefs.getBoolean(HikeMessengerApp.JUST_SIGNED_UP, false);
			if (justSignedUp)
			{

				Editor editor = accountPrefs.edit();
				editor.remove(HikeMessengerApp.JUST_SIGNED_UP);
				editor.commit();

				if (!deviceDetailsSent)
				{
					sendDeviceDetails();
					if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) > -1)
					{
						try
						{
							JSONObject metadata = new JSONObject();
							
							if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) == HikeConstants.WelcomeTutorial.STICKER_VIEWED.ordinal())
							{
								metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FTUE_TUTORIAL_STICKER_VIEWED);
							}
							else if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) == HikeConstants.WelcomeTutorial.CHAT_BG_VIEWED.ordinal())
							{
								metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FTUE_TUTORIAL_CBG_VIEWED);
							}
							HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
	
							editor = accountPrefs.edit();
							editor.remove(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED);
							editor.commit();
						}
						catch(JSONException e)
						{
							Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
						}
					}						
				}
			}
		}
		else if (HikePubSub.UPDATE_PUSH.equals(type))
		{
			final int updateType = ((Integer) object).intValue();
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					showUpdatePopup(updateType);
				}
			});
		}
		else if (HikePubSub.REFRESH_FAVORITES.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					GetFTUEContactsTask ftueContactsTask = new GetFTUEContactsTask();
					ftueContactsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
			});
		}
		else if (HikePubSub.UPDATE_NETWORK_STATE.equals(type))
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					animateNShowNetworkError();
				}
			});
		}
		else if (HikePubSub.CONTACT_SYNCED.equals(type))
		{
			Pair<Boolean, Byte> ret = (Pair<Boolean, Byte>) object;
			final boolean manualSync = ret.first;
			final byte contactSyncResult = ret.second;
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (manualSync)
					{
						if (contactSyncResult == ContactManager.SYNC_CONTACTS_NO_CONTACTS_FOUND_IN_ANDROID_ADDRESSBOOK)
						{
							Toast.makeText(getApplicationContext(), R.string.contacts_sync_no_contacts_found, Toast.LENGTH_SHORT).show();
						}
						else if (contactSyncResult == ContactManager.SYNC_CONTACTS_ERROR)
						{
							Toast.makeText(getApplicationContext(), R.string.contacts_sync_error, Toast.LENGTH_SHORT).show();
						}
						else
						{
							Toast.makeText(getApplicationContext(), R.string.contacts_synced, Toast.LENGTH_SHORT).show();
						}
					}
				}
			});
		}
		else if (HikePubSub.STEALTH_UNREAD_TIP_CLICKED.equals(type))
		{
			runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					hikeLogoClicked();
				}
			});
		}
		else if(HikePubSub.FAVORITE_COUNT_CHANGED.equals(type))
		{
			runOnUiThread( new Runnable()
			{
				@Override
				public void run()
				{
					updateHomeOverflowToggleCount(getHomeOverflowCount(accountPrefs, false, false), 0);
					if (null != overflowAdapter)
					{
						overflowAdapter.notifyDataSetChanged();
					}
				}
			});
		}
		else if(HikePubSub.OPEN_COMPOSE_CHAT_SCREEN.equals(type))
		{
			if(isActivityVisible())
			{
				moveToComposeChatScreen();
			}
		}
		else if(HikePubSub.STEALTH_INDICATOR.equals(type))
		{
			HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.STEALTH_INDICATOR_SHOW_ONCE);
			
			if(StealthModeManager.getInstance().isActive())
			{
				return;
			}
			runOnUiThread( new Runnable()
			{
				@Override
				public void run()
				{
					/**
					 * If we are showing search mode action bar, we should not show tip/anim
					 */
					if (showingSearchModeActionBar)
					{
						return;
					}
					
					if(hiButton != null)
					{
						hiButton.startAnimation(HikeAnimationFactory.getHikeActionBarLogoAnimation(HomeActivity.this));
					}
					else
					{
						flashStealthIndicatorView();
					}
				}
			});
		}
		else if (HikePubSub.UPDATE_OF_PHOTOS_ICON.equals(type))
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					invalidateOptionsMenu();
				}
			});
		}
		else if (HikePubSub.SHOW_NEW_CHAT_RED_DOT.equals(type))
		{
			sendUIMessage(SHOW_NEW_CHAT_RED_DOT, 1000);
		}
		else if (HikePubSub.PRODUCT_POPUP_RECEIVE_COMPLETE.equals(type))
		{
			if (isActivityVisible())
			{
				showProductPopup(ProductPopupsConstants.PopupTriggerPoints.HOME_SCREEN.ordinal());
			}
		}else if (HikePubSub.BOT_CREATED.equals(type))
		{
			if(object == null || ! (object instanceof Pair) || !(Boolean)((Pair) object).second)
			{
				return;
			}
			if(((Pair) object).first instanceof BotInfo)
			{
			 final BotInfo info = ((BotInfo) ((Pair) object).first);
			if(info == null)
			{
				return;
			}
			 if(!info.isConvPresent()&&info.getTriggerPointFormenu()==BotInfo.TriggerEntryPoint.ENTRY_AT_HOME_MENU)
			 {
				 runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							ArrayList<OverFlowMenuItem> optionsList = new ArrayList<OverFlowMenuItem>();
							addBotItem(optionsList,info);
						}
					});
			 }
		   }
		}
	}

	private void moveToComposeChatScreen()
	{
		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.OPEN_COMPOSE_CHAT_ONE_TIME_TRIGGER, false))
		{
			HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.OPEN_COMPOSE_CHAT_ONE_TIME_TRIGGER);
			Intent intent = IntentFactory.getComposeChatIntentWithBotDiscovery(HomeActivity.this);
			startActivity(intent);
		}
	}

	private void updateHomeOverflowToggleCount(final int count, int delayTime)
	{
		if (accountPrefs.getBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false) || count < 1 || (null != overFlowWindow && overFlowWindow.isShowing()))
		{
			if(topBarIndicator!=null)
				topBarIndicator.setVisibility(View.GONE);
		}
		else
		{
			Message msg = Message.obtain();
			msg.what = SHOW_OVERFLOW_INDICATOR;
			msg.arg1 = count;
			uiHandler.sendMessageDelayed(msg, delayTime);
		}

	}

	private class GetFTUEContactsTask extends AsyncTask<Void, Void, FtueContactsData>
	{

		@Override
		protected FtueContactsData doInBackground(Void... params)
		{
			FtueContactsData ftueContactsDataResult = ContactManager.getInstance().getFTUEContacts(accountPrefs);
			/*
			 * This msisdn type will be the identifier for ftue contacts in the friends tab.
			 */
			ftueContactsDataResult.setCompleteList();
			for (ContactInfo contactInfo : ftueContactsDataResult.getCompleteList())
			{
				contactInfo.setMsisdnType(HikeConstants.FTUE_MSISDN_TYPE);
			}

			return ftueContactsDataResult;
		}

		@Override
		protected void onPostExecute(FtueContactsData result)
		{
			ftueContactsData = result;
			Logger.d("GetFTUEContactsTask","ftueContactsData = "+ ftueContactsData.toString());
			HikeMessengerApp.getPubSub().publish(HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, null);
		}
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (Build.VERSION.SDK_INT <= 10 || (Build.VERSION.SDK_INT >= 14 && ViewConfiguration.get(this).hasPermanentMenuKey()))
		{
			if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU)
			{
				if ((ftueAddFriendWindow != null && ftueAddFriendWindow.getVisibility() == View.VISIBLE) || dialogShowing!=null)
				{
					return true;
				}
				if (overFlowWindow == null || !overFlowWindow.isShowing())
				{
					showOverFlowMenu();
				}
				else
				{
					overFlowWindow.dismiss();
				}
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	private void checkNShowNetworkError()
	{
		if (networkErrorPopUp == null)
			return;
		Logger.d(getClass().getSimpleName(), "visiblity for: " + HikeMessengerApp.networkError);
		// networkErrorPopUp.clearAnimation();
		if (HikeMessengerApp.networkError && OfflineController.getInstance().getOfflineState() != OFFLINE_STATE.CONNECTED)
		{
			networkErrorPopUp.setText(R.string.no_internet_connection);
			networkErrorPopUp.setBackgroundColor(getResources().getColor(R.color.red_no_network));
			networkErrorPopUp.setVisibility(View.VISIBLE);
		}
		else
		{
			networkErrorPopUp.setVisibility(View.GONE);
		}
	}

	private void animateNShowNetworkError()
	{
		if (networkErrorPopUp == null)
			return;
		Logger.d(getClass().getSimpleName(), "animation for: " + HikeMessengerApp.networkError);
		if (HikeMessengerApp.networkError && OfflineController.getInstance().getOfflineState() != OFFLINE_STATE.CONNECTED)
		{
			Animation alphaIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up_noalpha);
			alphaIn.setDuration(400);
			networkErrorPopUp.setText(R.string.no_internet_connection);
			networkErrorPopUp.setBackgroundColor(getResources().getColor(R.color.red_no_network));
			networkErrorPopUp.setAnimation(alphaIn);
			networkErrorPopUp.setVisibility(View.VISIBLE);
			alphaIn.start();
		}
		else if (networkErrorPopUp.getVisibility() == View.VISIBLE)
		{
			Animation alphaIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down_noalpha);
			alphaIn.setStartOffset(1000);
			alphaIn.setDuration(400);
			networkErrorPopUp.setText(R.string.connected);
			networkErrorPopUp.setBackgroundColor(getResources().getColor(R.color.green_connected));
			networkErrorPopUp.setVisibility(View.GONE);
			networkErrorPopUp.setAnimation(alphaIn);
			alphaIn.start();
		}
		checkNShowNetworkError();
	}

	public class OverflowAdapter extends ArrayAdapter<OverFlowMenuItem>
	{
		private String msisdn;

		public OverflowAdapter(Context context, int resource, int textViewResourceId, List<OverFlowMenuItem> objects, String msisdn)
		{
			super(context, resource, textViewResourceId, objects);
			this.msisdn = msisdn;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
			{
				convertView = getLayoutInflater().inflate(R.layout.over_flow_menu_item, parent, false);
			}

			OverFlowMenuItem item = getItem(position);

			TextView itemTextView = (TextView) convertView.findViewById(R.id.item_title);
			itemTextView.setText(item.text);

			TextView newGamesIndicator = (TextView) convertView.findViewById(R.id.new_games_indicator);
			newGamesIndicator.setText("1");

			/*
			 * Rewards & Games indicator bubble are by default shown even if the keys are not stored in shared pref.
			 */
			boolean isGamesClicked = accountPrefs.getBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, false);
			boolean isRewardsClicked = accountPrefs.getBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, false);
			boolean showTimelineRedDot = accountPrefs.getBoolean(HikeConstants.SHOW_TIMELINE_RED_DOT, false);
			boolean showNUJRedDot = accountPrefs.getBoolean(HikeConstants.SHOW_RECENTLY_JOINED_DOT, false);

			int count = 0;
			if (item.id == R.string.timeline)
			{
				count = Utils.getNotificationCount(accountPrefs, false);
				if (count > 9)
					newGamesIndicator.setText("9+");
				else if (count > 0)
					newGamesIndicator.setText(String.valueOf(count));
			}
			
			if ((item.id == R.string.hike_extras && !isGamesClicked) || (item.id == R.string.rewards && !isRewardsClicked) || (item.id == R.string.timeline && (count > 0 || showTimelineRedDot)))
			{
				newGamesIndicator.setVisibility(View.VISIBLE);
			}
			
			else
			{
				newGamesIndicator.setVisibility(View.GONE);
			}

			return convertView;
		}
	}

	public void showOverFlowMenu()
	{

		if (overFlowWindow != null && overFlowWindow.isShowing())
			return;

		ArrayList<OverFlowMenuItem> optionsList = new ArrayList<OverFlowMenuItem>();

		final String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
		/*
		 * removing out new chat option for now
		 */
		optionsList.add(new OverFlowMenuItem(getString(R.string.new_broadcast), 0, 0, R.string.new_broadcast));

		optionsList.add(new OverFlowMenuItem(getString(R.string.new_group), 0, 0, R.string.new_group));


		if (OfflineController.getInstance().getConfigurationParamerters().shouldShowHikeDirectOption())
		{
			optionsList.add(new OverFlowMenuItem(getString(R.string.scan_free_hike), 0, 0, R.string.scan_free_hike));
		}
		else
		{
			if (Utils.isPhotosEditEnabled())
			{
				optionsList.add(new OverFlowMenuItem(getString(R.string.home_overflow_new_photo), 0, 0, R.string.home_overflow_new_photo));
			}
		}
		
		
		optionsList.add(new OverFlowMenuItem(getString(R.string.invite_friends), 0, 0, R.string.invite_friends));
	
		if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_GAMES, false) && !TextUtils.isEmpty((HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.REWARDS_TOKEN, ""))))
		{
			String hikeExtrasName = accountPrefs.getString(HikeConstants.HIKE_EXTRAS_NAME, getApplicationContext().getString(R.string.hike_extras));
					                       
			if(!TextUtils.isEmpty(hikeExtrasName))
			{
				optionsList.add(new OverFlowMenuItem(hikeExtrasName, 0, 0, R.string.hike_extras));
			}
		}
		
		if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_REWARDS, false) && !TextUtils.isEmpty(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.REWARDS_TOKEN, "")))
		{
			String rewards_name = accountPrefs.getString(HikeConstants.REWARDS_NAME, getApplicationContext().getString(R.string.rewards));
												
			if(!TextUtils.isEmpty(rewards_name))
			{
				optionsList.add(new OverFlowMenuItem(rewards_name, 0, 0, R.string.rewards));
			}
		}

		optionsList.add(new OverFlowMenuItem(getString(R.string.settings), 0, 0, R.string.settings));

		optionsList.add(new OverFlowMenuItem(getString(R.string.status), 0, 0, R.string.status));

		addEmailLogItem(optionsList);
		
		addBotItems(optionsList);

		overFlowWindow = new PopupWindow(this);

		FrameLayout homeScreen = (FrameLayout) findViewById(R.id.home_screen);

		View parentView = getLayoutInflater().inflate(R.layout.overflow_menu, homeScreen, false);

		overFlowWindow.setContentView(parentView);

		ListView overFlowListView = (ListView) parentView.findViewById(R.id.overflow_menu_list);
		overflowAdapter = new OverflowAdapter(this, R.layout.over_flow_menu_item, R.id.item_title, optionsList, msisdn);
		overFlowListView.setAdapter(overflowAdapter);

		overFlowListView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				Logger.d(getClass().getSimpleName(), "Onclick: " + position);

				overFlowWindow.dismiss();
				OverFlowMenuItem item = (OverFlowMenuItem) adapterView.getItemAtPosition(position);
				Intent intent = null;
				Editor editor = accountPrefs.edit();

				switch (item.id)
				{
				case R.string.scan_free_hike:
					intent = IntentFactory.getComposeChatActivityIntent(HomeActivity.this);
					intent.putExtra(HikeConstants.Extras.HIKE_DIRECT_MODE, true);
					OfflineUtils.recordHikeDirectOverFlowClicked();
					break;
				case R.string.invite_friends:
					recordInviteFriendsClick();
					intent = new Intent(HomeActivity.this, TellAFriend.class);
					break;
					
				case R.string.hike_extras:
					recordRewardsClick();
					editor.putBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, true);
					editor.commit();
					updateOverFlowMenuNotification();
					intent = IntentFactory.getGamingIntent(HomeActivity.this);
					break;
					
				case R.string.rewards:
					editor.putBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, true);
					editor.commit();
					updateOverFlowMenuNotification();
					intent = IntentFactory.getRewardsIntent(HomeActivity.this);
					break;
					
				case R.string.settings:
					HAManager.logClickEvent(HikeConstants.LogEvent.SETTING_CLICKED);
					intent = new Intent(HomeActivity.this, SettingsActivity.class);
					break;
				case R.string.new_group:
					recordNewGroupClick();
					intent = new Intent(HomeActivity.this, CreateNewGroupOrBroadcastActivity.class);
					break;
				
                case R.string.wallet_menu:
					
					intent = IntentFactory.getNonMessagingBotIntent(HikeConstants.MicroApp_Msisdn.HIKE_WALLET ,getApplicationContext());
					intent.putExtra(AnalyticsConstants.BOT_VIA_MENU, AnalyticsConstants.BOT_VIA_HOME_MENU);
					break;
					
                case R.string.recharge_menu:
					
					intent = IntentFactory.getNonMessagingBotIntent(HikeConstants.MicroApp_Msisdn.HIKE_RECHARGE ,getApplicationContext());
					intent.putExtra(AnalyticsConstants.BOT_VIA_MENU, AnalyticsConstants.BOT_VIA_HOME_MENU);
					break;
					
				case R.string.timeline:
					try
					{
						JSONObject md = new JSONObject();
						md.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SHOW_TIMELINE_TOP_BAR);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, md);
					}
					catch(JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}
					
					JSONObject metadataSU = new JSONObject();
					try
					{
						metadataSU.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.TIMELINE_OPEN);
						if (Utils.getNotificationCount(accountPrefs, false) > 0)
						{
							metadataSU.put(AnalyticsConstants.EVENT_SOURCE, HikeConstants.LogEvent.TIMELINE_WITH_RED_DOT);
						}

						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, HAManager.EventPriority.HIGH, metadataSU);
					}
					catch (JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}

					editor.putBoolean(HikeConstants.SHOW_TIMELINE_RED_DOT, false);
					editor.commit();
					intent = new Intent(HomeActivity.this, TimelineActivity.class);
					break;
					
				case R.string.status:
					try
					{
						JSONObject metadata = new JSONObject();
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STATUS_UPDATE_FROM_OVERFLOW);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					}
					catch(JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}

					intent = new Intent(HomeActivity.this, StatusUpdate.class);
					Utils.setSpecies(HomeAnalyticsConstants.SU_SPECIES_OVERFLOW, intent);
					break;
					
				case R.string.send_logs:
					SendLogsTask logsTask = new SendLogsTask(HomeActivity.this);
					logsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					break;
					
				case R.string.new_broadcast:
					sendBroadCastAnalytics();
					IntentFactory.createBroadcastIntent(HomeActivity.this);
					break;
				case R.string.home_overflow_new_photo:
					// Open gallery
					int galleryFlags = GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS | GalleryActivity.GALLERY_EDIT_SELECTED_IMAGE
							| GalleryActivity.GALLERY_COMPRESS_EDITED_IMAGE | GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM;
					Intent galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(HomeActivity.this, galleryFlags, null);

					startActivity(galleryPickerIntent);

					sendAnalyticsTakePicture();
					break;
					
				}

				if (intent != null)
				{
					startActivity(intent);
				}
			}
		});

		overFlowWindow.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
		overFlowWindow.setOutsideTouchable(true);
		overFlowWindow.setFocusable(true);
		overFlowWindow.setWidth(Utils.getOverflowMenuWidth(getApplicationContext()));
		overFlowWindow.setHeight(LayoutParams.WRAP_CONTENT);
		
		int width = getResources().getDimensionPixelSize(R.dimen.overflow_menu_width);
		final int rightMargin = width + getResources().getDimensionPixelSize(R.dimen.overflow_menu_right_margin);

		final View anchor = findViewById(R.id.overflow_anchor);
		anchor.post(new Runnable()
		{

			@Override
			public void run()
			{
				try
				{
					overFlowWindow.showAsDropDown(anchor, -rightMargin, 0);
				}

				catch (BadTokenException e)
				{
					Logger.wtf(TAG, " Getting badToken exception in showAsDropDown method");
				}
			}

		});

		overFlowWindow.getContentView().setFocusableInTouchMode(true);
		overFlowWindow.getContentView().setOnKeyListener(new View.OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				return onKeyUp(keyCode, event);
			}
		});
	}

	private void addBotItem(List<OverFlowMenuItem> overFlowMenuItems, BotInfo info)
	{
		if (info.getMsisdn().equalsIgnoreCase(HikeConstants.MicroApp_Msisdn.HIKE_WALLET))
		{
			overFlowMenuItems.add(new OverFlowMenuItem(getString(R.string.wallet_menu), 0, 0, R.string.wallet_menu));
		}else if (info.getMsisdn().equalsIgnoreCase(HikeConstants.MicroApp_Msisdn.HIKE_RECHARGE))
		{
			overFlowMenuItems.add(new OverFlowMenuItem(getString(R.string.recharge_menu), 0, 0, R.string.recharge_menu));
		}

	}
	
	private void addBotItems(List<OverFlowMenuItem> overFlowMenuItems)
	{

		BotUtils.addAllMicroAppMenu(overFlowMenuItems, BotInfo.TriggerEntryPoint.ENTRY_AT_HOME_MENU, getApplicationContext());
	}
	
	private void addEmailLogItem(List<OverFlowMenuItem> overFlowMenuItems)
	{
		if (AppConfig.SHOW_SEND_LOGS_OPTION)
		{
			overFlowMenuItems.add(new OverFlowMenuItem(getString(R.string.send_logs), 0, 0, R.string.send_logs));
		}
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		Logger.d(TAG, "onRestoredInstanceState");
		final boolean overflowState = savedInstanceState.getBoolean(HikeConstants.Extras.IS_HOME_POPUP_SHOWING);
		if (overflowState)
		{
			showOverFlowMenu();
		}
		super.onRestoreInstanceState(savedInstanceState);
	}

	public void updateOverFlowMenuNotification()
	{
		final int count = getHomeOverflowCount(accountPrefs, false, false);
		if (topBarIndicator != null)
		{
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					updateHomeOverflowToggleCount(count, 1000);
				}
			});

		}
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		return fetchContactsTask;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		// handle dialogs here
		if(progDialog != null && progDialog.isShowing())
		{
			progDialog.dismiss();
			progDialog = HikeDialogFactory.showDialog(HomeActivity.this, HikeDialogFactory.HIKE_UPGRADE_DIALOG, null);
			showingBlockingDialog = true;
			
		}
		if (dialogShowing != null)
		{
			showAppropriateDialog();
		}
		
		if (overFlowWindow != null && overFlowWindow.isShowing())
		{
			overFlowWindow.dismiss();
			showOverFlowMenu();
		}
	}

	private void showAppropriateDialog()
	{
		if (dialog != null)
		{
			if (dialog.isShowing())
			{
				dialog.dismiss();
			}
			else
			{
				return;
			}
		}
		switch (dialogShowing)
		{
		case SMS_CLIENT:
			showSMSClientDialog();
			break;

		case SMS_SYNC_CONFIRMATION:
		case SMS_SYNCING:
			showSMSSyncDialog();
			break;
		case UPGRADE_POPUP:
			showUpdatePopup(updateType);
			break;
		case FREE_INVITE_POPUP:
			showFreeInviteDialog();
			break;
		case VOIP_FTUE_POPUP:
			dialogShowing = DialogShowing.VOIP_FTUE_POPUP;
			dialog = HikeDialogFactory.showDialog(this, HikeDialogFactory.VOIP_INTRO_DIALOG, getHomeActivityDialogListener(), null);
		}
	}

	private HikeDialogListener getHomeActivityDialogListener()
	{
		return new HikeDialogListener()
		{

			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{

			}

			@Override
			public void neutralClicked(HikeDialog hikeDialog)
			{
				dialogShowing = null;
				hikeDialog.dismiss();
				HomeActivity.this.dialog = null;
			}

			@Override
			public void negativeClicked(HikeDialog hikeDialog)
			{
				hikeDialog.dismiss();
			}

		};
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		LockPattern.onLockActivityResult(this, requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	public void showRecentlyJoinedDot(int delayTime)
	{
		Message msg = Message.obtain();
		msg.what = SHOW_RECENTLY_JOINED_INDICATOR;
		uiHandler.sendMessageDelayed(msg, delayTime);
	}
	
	public void showTimelineUpdatesDot(int delayTime)
	{
		Message msg = Message.obtain();
		msg.what = SHOW_TIMELINE_UPDATES_INDICATOR;
		uiHandler.sendMessageDelayed(msg, delayTime);
	}

	public void sendUIMessage(int what, long delayTime)
	{
		Message msg = Message.obtain();
		msg.what = what;
		uiHandler.sendMessageDelayed(msg, delayTime);
	}

	public void hikeLogoClicked()
	{
		recordHikeLogoClicked();
		if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_WELCOME_HIKE_TIP, false))
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_TIP, ConversationTip.WELCOME_HIKE_TIP);
		}
		StealthModeManager.getInstance().toggleActionTriggered(this);
	}

	private void sendAnalyticsTakePicture()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.PHOTOS_FLOW_OPTION_CLICK);
			HikeAnalyticsEvent.analyticsForPhotos(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void sendBroadCastAnalytics()
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.NEW_BROADCAST_VIA_OVERFLOW);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	private void showCorruptDBRestoreDialog()
	{
		dbCorruptDialog = HikeDialogFactory.showDialog(HomeActivity.this, HikeDialogFactory.DB_CORRUPT_RESTORE_DIALOG, this);
		showingBlockingDialog = true;
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.BLOCK_NOTIFICATIONS, true); // Block any possible notifs as well
		Utils.disconnectFromMQTT();

		//Cancel the notif alarm if any
		HikeAlarmManager.cancelAlarm(getApplicationContext(),HikeAlarmManager.REQUESTCODE_SHOW_CORRUPT_DB_NOTIF);
	}

	@Override
	public void negativeClicked(HikeDialog hikeDialog)
	{
		switch (hikeDialog.getId())
		{
		case HikeDialogFactory.DB_CORRUPT_RESTORE_DIALOG:
			onCorruptDialogSkipRestoreClicked(hikeDialog);
			break;
		}

	}

	@Override
	public void positiveClicked(HikeDialog hikeDialog)
	{
		switch (hikeDialog.getId())
		{
		case HikeDialogFactory.DB_CORRUPT_RESTORE_DIALOG:
			onCorruptDialogRestoreClicked(hikeDialog);
		}
	}

	@Override
	public void neutralClicked(HikeDialog hikeDialog)
	{

	}

	private void onCorruptDialogSkipRestoreClicked(HikeDialog hikeDialog)
	{
		hikeDialog.dismiss();
		dbCorruptDialog = null;
		showingBlockingDialog = false;
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.DB_CORRUPT, false);
		HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.BLOCK_NOTIFICATIONS, false); // UnBlock any possible notifs as well

		// Connect to service again
		HikeMessengerApp app = (HikeMessengerApp) getApplication();

        //reset dticker tables to default sticker set
        StickerManager.getInstance().resetStickerTablesToDefault();

		app.connectToService();

		// Set up the home screen
		invalidateOptionsMenu();
		initialiseHomeScreen(null);
	}

	private void onCorruptDialogRestoreClicked(HikeDialog hikeDialog)
	{
		// dismiss the previous dialog and show the infinite spinner dialog
		hikeDialog.dismiss();
		dbCorruptDialog = null;

		startRestoreProcess();
	}

	private void startRestoreProcess()
	{
		restoreAsyncTask = new AccountRestoreAsyncTask(new WeakReference<AccountRestoreAsyncTask.IRestoreCallback>(this));
		restoreAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void preRestoreSetup()
	{
		showRestoreInProcessDialog();
	}

	@Override
	public void postRestoreFinished(@AccountBackupRestore.RestoreErrorStates Integer restoreResult)
	{
		if (dbCorruptDialog != null)
		{
			dbCorruptDialog.dismiss();
			dbCorruptDialog = null;
		}

		showingBlockingDialog = false;

		if (restoreResult == AccountBackupRestore.STATE_RESTORE_SUCCESS)
		{
			Toast.makeText(HomeActivity.this, getString(R.string.restore_success), Toast.LENGTH_LONG).show();
		}

		else
		{
			if (restoreProgDialog != null) //Dismiss the rotator!
			{
				restoreProgDialog.dismiss();
				restoreProgDialog = null;
			}
			checkAndShowCorruptDbDialog(); // Take the user to the same damn dialog until "Skip Restore" is pressed.
			Toast.makeText(HomeActivity.this, getString(R.string.restore_failure) , Toast.LENGTH_LONG).show();
			return;
		}

		// Connect to service again
		HikeMessengerApp app = (HikeMessengerApp) getApplication();
		app.connectToService();

		// Set up the home screen. First check if we are running upgrade intent service or not.

		if (!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.UPGRADING, false))
		{
			if (restoreProgDialog != null)
			{
				restoreProgDialog.dismiss();
				restoreProgDialog = null;
			}

			invalidateOptionsMenu();
			initialiseHomeScreen(null);
		}
	}

	private void showRestoreInProcessDialog()
	{
		restoreProgDialog = ProgressDialog.show(HomeActivity.this,"", getString(R.string.restore_progress_body), true, false);
		showingBlockingDialog = true;
	}

	private void checkAndShowCorruptDbDialog()
	{
		if (showingBlockingDialog)
		{
			return; //Already showing something so return
		}

		if (Utils.isDBCorrupt()) //Conversation fragment could have been added previously. Remove it and show the corrupt dialog
		{
			if (isFragmentAdded(MAIN_FRAGMENT_TAG))
			{
				removeFragment(MAIN_FRAGMENT_TAG);
				wasFragmentRemoved = true;
			}

			Logger.d(TAG, "Removed ConvFragment and showing the restore chats dialog now");

			showCorruptDBRestoreDialog();
		}
	}

	private void recordOverFlowMenuClick()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, HomeAnalyticsConstants.HOME_OVERFLOW_MENU);
			json.put(AnalyticsConstants.V2.KINGDOM, HomeAnalyticsConstants.HOMESCREEN_KINGDOM);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, HomeAnalyticsConstants.HOME_OVERFLOW_MENU);

			HAManager.getInstance().recordV2(json);
		}

		catch (JSONException e)
		{
			e.toString();
		}
	}

	private void recordNewGroupClick()
	{
		recordOverflowItemclick("grp");
	}

	private void recordInviteFriendsClick()
	{
		recordOverflowItemclick("invt_frnds");
	}

	private void recordRewardsClick()
	{
		recordOverflowItemclick("rwds");
	}

	private void recordOverflowItemclick(String whichItem)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, HomeAnalyticsConstants.HOME_OVERFLOW_MENU_ITEM);
			json.put(AnalyticsConstants.V2.KINGDOM, HomeAnalyticsConstants.HOMESCREEN_KINGDOM);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, whichItem);

			HAManager.getInstance().recordV2(json);
		}

		catch (JSONException e)
		{
			e.toString();
		}
	}

	private void recordHikeLogoClicked()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, HomeAnalyticsConstants.HIDDEN_UK);
			json.put(AnalyticsConstants.V2.KINGDOM, HomeAnalyticsConstants.HOMESCREEN_KINGDOM);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, HomeAnalyticsConstants.HIDDEN_UK);

			HAManager.getInstance().recordV2(json);
		}

		catch (JSONException e)
		{
			e.toString();
		}
	}

}
