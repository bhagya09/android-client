package com.bsb.hike.ui;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Color;
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
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.FtueContactsData;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.OverFlowMenuItem;
import com.bsb.hike.models.Conversation.ConversationTip;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.snowfall.SnowFallView;
import com.bsb.hike.tasks.DownloadAndInstallUpdateAsyncTask;
import com.bsb.hike.tasks.SendLogsTask;
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
import com.bsb.hike.utils.Utils;

public class HomeActivity extends HikeAppStateBaseFragmentActivity implements Listener
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

	private Dialog progDialog;

	private Dialog updateAlert;

	private Button updateAlertOkBtn;

	private static int updateType;

	private boolean showingProgress = false;

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

	private String[] homePubSubListeners = { HikePubSub.INCREMENTED_UNSEEN_STATUS_COUNT, HikePubSub.SMS_SYNC_COMPLETE, HikePubSub.SMS_SYNC_FAIL, HikePubSub.FAVORITE_TOGGLED,
			HikePubSub.USER_JOINED, HikePubSub.USER_LEFT, HikePubSub.FRIEND_REQUEST_ACCEPTED, HikePubSub.REJECT_FRIEND_REQUEST, HikePubSub.UPDATE_OF_MENU_NOTIFICATION,
			HikePubSub.SERVICE_STARTED, HikePubSub.UPDATE_PUSH, HikePubSub.REFRESH_FAVORITES, HikePubSub.UPDATE_NETWORK_STATE, HikePubSub.CONTACT_SYNCED, HikePubSub.FAVORITE_COUNT_CHANGED,
			HikePubSub.STEALTH_UNREAD_TIP_CLICKED,HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, HikePubSub.STEALTH_INDICATOR, HikePubSub.USER_JOINED_NOTIFICATION, HikePubSub.UPDATE_OF_PHOTOS_ICON  };

	private String[] progressPubSubListeners = { HikePubSub.FINISHED_UPGRADE_INTENT_SERVICE };

	private static MenuItem searchMenuItem;

	private boolean showingSearchModeActionBar = false;
	
	private static final String TAG = "HomeActivity";
	
	// Declare all Handler Msg Id here
	
	protected static final int FESTIVE_POPUP = -101;

	protected static final int SHOW_OVERFLOW_INDICATOR = -102;

	protected static final int SHOW_RECENTLY_JOINED_INDICATOR = -103;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Logger.d(TAG,"onCreate");
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.CLEARED_OUT, false)) 
		{
			//this means that singleTop activity has been re-spawned after being destroyed 
			extrasClearedOut = true;
		}
		
		if(extrasClearedOut)
		{
			//removing unwanted EXTRA becoz every time a singleTop activity is re-spawned, 
			//android system uses the old intent to fire it, and it will contain unwanted extras.
			getIntent().removeExtra(HikeConstants.STEALTH_MSISDN);
		}

		if (Utils.requireAuth(this))
		{
			Logger.wtf(TAG, "user is not authenticated. Finishing activity");
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

		//Make sure we are not launching share intent if our activity is restarted by OS
		if(isShareIntent(getIntent()) && savedInstanceState == null)
		{
			handleFileShareIntent(getIntent());
		}
		
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
			showingProgress = true;
			
		}

		if (!showingProgress)
		{
			if (Utils.isVoipActivated(HomeActivity.this) && HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_VOIP_FTUE_POPUP, false))
			{
				dialogShowing = DialogShowing.VOIP_FTUE_POPUP;
			}
			initialiseHomeScreen(savedInstanceState);
		}
		Logger.d(getClass().getSimpleName(),"onCreate "+this.getClass().getSimpleName());
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.HOME_SCREEN.ordinal());
	
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
		default:
			super.handleUIMessage(msg);
			break;
		}

	}
	
	private boolean isShareIntent(Intent intent)
	{
		if (Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) 
		{
			return true;
		} 
		
		return false;
	}
	
	private void handleFileShareIntent(Intent intent)
	{

		//If launching from android history, do not process 
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0)
		{
			return;
		}
		
		if (Intent.ACTION_SEND.equals(intent.getAction()) ) 
		{
			if(HikeFileType.fromString(intent.getType()).compareTo(HikeFileType.IMAGE)==0 && Utils.isPhotosEditEnabled()) 
			{ 
				String fileName = Utils.getAbsolutePathFromUri((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM), getApplicationContext(),true);
				startActivity(IntentFactory.getPictureEditorActivityIntent(getApplicationContext(), fileName, true, null, false));
			}
			else
			{
				transferShareIntent(intent);
			}
		} 
		
		else if(Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
		{
			transferShareIntent(intent);
			
			//Commenting out Multi-share:images edit code. This feature will be enabled in next release.
			
			/*
			ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			ArrayList<GalleryItem> selectedImages = GalleryItem.getGalleryItemsFromFilepaths(imageUris);
			if((selectedImages!=null) && Utils.isPhotosEditEnabled()) 
			{
				Intent multiIntent = new Intent(getApplicationContext(),GallerySelectionViewer.class);
				multiIntent.putParcelableArrayListExtra(HikeConstants.Extras.GALLERY_SELECTIONS, selectedImages);
				multiIntent.putExtra(GallerySelectionViewer.FROM_DEVICE_GALLERY_SHARE, true);
				startActivity(multiIntent);
			}
			else
			{
				handleShareIntent(intent);
			}
			*/
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

	private void setupSearchActionBar()
	{
		showingSearchModeActionBar = true;

	}

	private void initialiseHomeScreen(Bundle savedInstanceState)
	{

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

		HikeMessengerApp.getPubSub().addListeners(this, homePubSubListeners);

		GetFTUEContactsTask getFTUEContactsTask = new GetFTUEContactsTask();
		Utils.executeContactInfoListResultTask(getFTUEContactsTask);

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
		findViewById(R.id.action_bar_img).setVisibility(View.VISIBLE);
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
			getSupportFragmentManager().beginTransaction().add(R.id.home_screen, mainFragment, MAIN_FRAGMENT_TAG).commit();
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
		findViewById(R.id.action_bar_img).setVisibility(View.GONE);
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
		}
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
		
		if(isShareIntent(intent))
		{
			handleFileShareIntent(intent);
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
		
		if (showingProgress)
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
		//Adding defensive null pointer check (bug#44531)May be due to sherlock code, nullpointerexception occured.
		try{
			getMenuInflater().inflate(R.menu.chats_menu, menu);
			topBarIndicator = (TextView) (MenuItemCompat.getActionView(menu.findItem(R.id.overflow_menu)).findViewById(R.id.top_bar_indicator_text));
		updateOverFlowMenuNotification();
		MenuItemCompat.getActionView(menu.findItem(R.id.overflow_menu)).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showOverFlowMenu();
				topBarIndicator.setVisibility(View.GONE);
				Editor editor = accountPrefs.edit();
				editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, true);
				editor.commit();
			}
		});
		searchMenuItem = menu.findItem(R.id.search);
		SearchView searchView=(SearchView) MenuItemCompat.getActionView(searchMenuItem);
		//http://wtanaka.com/node/8049
		/*searchView.setIconifiedByDefault(false);
		searchView.setIconified(false)*/;
		searchView.setOnQueryTextListener(onQueryTextListener);
		searchView.clearFocus();
		searchOptionID = searchMenuItem.getItemId();
		SearchView.SearchAutoComplete theTextArea = (SearchView.SearchAutoComplete)searchView.findViewById(R.id.search_src_text);
		theTextArea.setTextColor(Color.WHITE);
		//To show the cursor color in searchview in  versions higher than gingerbread
		//http://stackoverflow.com/questions/18705185/changing-the-cursor-color-in-searchview-without-actionbarsherlock
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD) {
			 try {
			        Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
			        mCursorDrawableRes.setAccessible(true);
			        mCursorDrawableRes.set(theTextArea,0); 
			    } catch (Exception e) {
			    	e.printStackTrace();
			    }
		}
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
				setupSearchActionBar();
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
         ((ImageView)    MenuItemCompat.getActionView(menu.findItem(R.id.new_conversation)).findViewById(R.id.overflow_icon_image)).setImageResource(R.drawable.ic_new_conversation);

	
			if (Utils.isPhotosEditEnabled())
			{
				
				View takePhotoActionView = (View)MenuItemCompat.getActionView(menu.findItem(R.id.take_pic));
				Logger.d("ph_en", "inside API setupMenuOptions, enabling photo inside actionbar");
				((ImageView) takePhotoActionView.findViewById(R.id.overflow_icon_image)).setImageResource(R.drawable.btn_cam_nav);
				takePhotoActionView.findViewById(R.id.overflow_icon_image).setContentDescription("New photo");
				takePhotoActionView.setOnClickListener(new OnClickListener()
				{

					@Override
					public void onClick(View v)
					{
						// Open gallery
						int galleryFlags = GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS | GalleryActivity.GALLERY_EDIT_SELECTED_IMAGE
								| GalleryActivity.GALLERY_COMPRESS_EDITED_IMAGE | GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM;
						Intent galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(HomeActivity.this, galleryFlags, null);

						startActivity(galleryPickerIntent);

						sendAnalyticsTakePicture();
					}
				});
			}
			else
			{
				Logger.d("ph_en", "inside setupMenuOptions, disabling photo inside actionbar");
				menu.removeItem(R.id.take_pic);
			}


		showRecentlyJoinedDot(1000);

		MenuItemCompat.getActionView(menu.findItem(R.id.new_conversation)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
				{
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

					Intent intent = new Intent(HomeActivity.this, ComposeChatActivity.class);
					intent.putExtra(HikeConstants.Extras.EDIT, true);

					newConversationIndicator.setVisibility(View.GONE);
					startActivity(intent);
				}
		});
		
		return true;
		}catch(NullPointerException e){
			Logger.e("NulllpointerException :setupMenuOptions" ,e.toString());
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

	public static void setSearchOptionAccess(boolean setVisible)
	{
		if (searchMenuItem != null)
		{
			searchMenuItem.setEnabled(setVisible);
			searchMenuItem.setVisible(setVisible);
		}
	}

	private void toggleMenuItems(Menu menu, boolean value)
	{
		menu.findItem(R.id.overflow_menu).setVisible(value);
		menu.findItem(R.id.new_conversation).setVisible(value);

		MenuItem takePicItem = menu.findItem(R.id.take_pic);
		if (takePicItem != null) // This will happen if photos edit option is disabled
		{
			takePicItem.setVisible(value);
		}
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
		Intent intent = null;

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

		if (intent != null)
		{
			startActivity(intent);
			return true;
		}
		else
		{
			return super.onOptionsItemSelected(item);
		}
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
			image.setImageResource(!showingRewardsPopup ? R.drawable.ic_free_sms_default : R.drawable.ic_free_sms_rewards);
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
	protected void onResume()
	{
		Logger.d(TAG,"onResume");
		super.onResume();

		if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STEALTH_INDICATOR_ENABLED, false))
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_INDICATOR, null);
		}
		checkNShowNetworkError();
		HikeMessengerApp.getPubSub().publish(HikePubSub.CANCEL_ALL_NOTIFICATIONS, null);
	}

	@Override
	protected void onPause()
	{
		if(getIntent().hasExtra(HikeConstants.STEALTH_MSISDN))
		{
			//after showing the LockPatternActivity in onResume of ConvFrag the extra is no longer needed, so clearing it out.
			extrasClearedOut = true;
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
		outState.putBoolean(HikeConstants.Extras.CLEARED_OUT, extrasClearedOut);
		super.onSaveInstanceState(outState);
	}

	private void sendDeviceDetails()
	{
		Utils.recordDeviceDetails(HomeActivity.this);
		Utils.requestAccountInfo(false, false);
		Utils.sendLocaleToServer(HomeActivity.this);
		deviceDetailsSent = true;
	}

	private void transferShareIntent(Intent shareIntent)
	{
		if(shareIntent == null)
		{
			return;
		}
		
		Intent intent = new Intent(HomeActivity.this,ComposeChatActivity.class);
		intent.putExtras(shareIntent.getExtras());
		intent.setAction(shareIntent.getAction());
		intent.setType(shareIntent.getType());
		startActivity(intent);
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
		if (updateType == HikeConstants.NO_UPDATE)
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
		updateAlert = new Dialog(HomeActivity.this, R.style.Theme_CustomDialog);
		updateAlert.setContentView(R.layout.operator_alert_popup);

		updateAlert.findViewById(R.id.body_checkbox).setVisibility(View.GONE);
		TextView updateText = ((TextView) updateAlert.findViewById(R.id.body_text));
		TextView updateTitle = (TextView) updateAlert.findViewById(R.id.header);

		updateText.setText(accountPrefs.getString(HikeConstants.Extras.UPDATE_MESSAGE, ""));

		updateTitle.setText(updateType == HikeConstants.CRITICAL_UPDATE ? R.string.critical_update_head : R.string.normal_update_head);

		Button cancelBtn = null;
		updateAlertOkBtn = (Button) updateAlert.findViewById(R.id.btn_ok);
		if (updateType == HikeConstants.CRITICAL_UPDATE)
		{
			((Button) updateAlert.findViewById(R.id.btn_cancel)).setVisibility(View.GONE);

			updateAlertOkBtn.setVisibility(View.VISIBLE);
		}
		else
		{
			cancelBtn = (Button) updateAlert.findViewById(R.id.btn_cancel);
			cancelBtn.setText(R.string.cancel);
		}
		updateAlertOkBtn.setText(R.string.update_app);

		updateAlert.setCancelable(true);

		updateAlertOkBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				updateApp(updateType);
			}
		});

		if (cancelBtn != null)
		{
			cancelBtn.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					updateAlert.cancel();
					dialogShowing = null;
				}
			});
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
		int timelineCount = Utils.getNotificationCount(accountPrefs, countUsersStatus);
		if (timelineCount == 0 && accountPrefs.getBoolean(HikeConstants.SHOW_TIMELINE_RED_DOT, true))
		{
			timelineCount = 1;
		}
		return timelineCount + Utils.updateHomeOverflowToggleCount(accountPrefs, defaultValue);
	}
	
	@Override
	public void onEventReceived(String type, Object object)
	{
		super.onEventReceived(type, object);
		if (HikePubSub.INCREMENTED_UNSEEN_STATUS_COUNT.equals(type))
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
		else if (type.equals(HikePubSub.FINISHED_UPGRADE_INTENT_SERVICE))
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					runOnUiThread(new Runnable()
					{
						@SuppressLint("NewApi")
						@Override
						public void run()
						{
							HikeMessengerApp.getPubSub().removeListeners(HomeActivity.this, progressPubSubListeners);

							showingProgress = false;
							if (progDialog != null)
							{
								progDialog.dismiss();
								progDialog = null;
							}
							invalidateOptionsMenu();
							initialiseHomeScreen(null);
						}
					});
				}
			}).start();
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
					if (accountPrefs.getBoolean(HikeMessengerApp.FB_SIGNUP, false))
					{
						try
						{
							JSONObject metadata = new JSONObject();
							metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FB_CLICK);
							HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
						}
						catch(JSONException e)
						{
							Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
						}
					}
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
					Utils.executeContactInfoListResultTask(ftueContactsTask);
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
			Boolean[] ret = (Boolean[]) object;
			final boolean manualSync = ret[0];
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if(manualSync)
						Toast.makeText(getApplicationContext(), R.string.contacts_synced, Toast.LENGTH_SHORT).show();
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
		else if(HikePubSub.STEALTH_INDICATOR.equals(type))
		{
			HikeSharedPreferenceUtil.getInstance().removeData(HikeConstants.STEALTH_INDICATOR_ENABLED);
			
			if(StealthModeManager.getInstance().isActive())
			{
				return;
			}
			runOnUiThread( new Runnable()
			{
				@Override
				public void run()
				{
					View hiButton = findViewById(android.R.id.home);
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
	}

	private void updateHomeOverflowToggleCount(final int count, int delayTime)
	{
		if (accountPrefs.getBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false) || count < 1 || (null != overFlowWindow && overFlowWindow.isShowing()))
		{
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
		if (HikeMessengerApp.networkError)
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
		if (HikeMessengerApp.networkError)
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
			itemTextView.setText(item.getName());

			int currentCredits = accountPrefs.getInt(HikeMessengerApp.SMS_SETTING, 0);

			TextView freeSmsCount = (TextView) convertView.findViewById(R.id.free_sms_count);
			freeSmsCount.setText(Integer.toString(currentCredits));
			if (item.getKey() == 1)
			{
				freeSmsCount.setVisibility(View.VISIBLE);
			}
			else
			{
				freeSmsCount.setVisibility(View.GONE);
			}

			TextView newGamesIndicator = (TextView) convertView.findViewById(R.id.new_games_indicator);
			newGamesIndicator.setText("1");

			/*
			 * Rewards & Games indicator bubble are by default shown even if the keys are not stored in shared pref.
			 */
			boolean isGamesClicked = accountPrefs.getBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, false);
			boolean isRewardsClicked = accountPrefs.getBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, false);
			boolean showTimelineRedDot = accountPrefs.getBoolean(HikeConstants.SHOW_TIMELINE_RED_DOT, true);
			boolean showBroadcastRedDot = accountPrefs.getBoolean(HikeConstants.SHOW_NEW_BROADCAST_RED_DOT, true);
			boolean showNUJRedDot = accountPrefs.getBoolean(HikeConstants.SHOW_RECENTLY_JOINED_DOT, false);

			int count = 0;
			if (item.getKey() == 7)
			{
				count = Utils.getNotificationCount(accountPrefs, false);
				if (count > 9)
					newGamesIndicator.setText("9+");
				else if (count > 0)
					newGamesIndicator.setText(String.valueOf(count));
			}
			if ((item.getKey() == 3 && !isGamesClicked) || (item.getKey() == 4 && !isRewardsClicked) || (item.getKey() == 7 && (count > 0 || showTimelineRedDot))
					|| (item.getKey() == 10 && showBroadcastRedDot)|| (item.getKey() == 11 && showNUJRedDot))
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

		ArrayList<OverFlowMenuItem> optionsList = new ArrayList<OverFlowMenuItem>();

		final String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
		/*
		 * removing out new chat option for now
		 */
		optionsList.add(new OverFlowMenuItem(getString(R.string.new_broadcast), 10));

		optionsList.add(new OverFlowMenuItem(getString(R.string.new_group), 6));

		optionsList.add(new OverFlowMenuItem(getString(R.string.timeline), 7));

		optionsList.add(new OverFlowMenuItem(getString(R.string.invite_friends), 2));

		if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_GAMES, false))
		{
			String hikeExtrasName = accountPrefs.getString(HikeConstants.HIKE_EXTRAS_NAME, getApplicationContext().getString(R.string.hike_extras));
					                       
			if(!TextUtils.isEmpty(hikeExtrasName))
			{
				optionsList.add(new OverFlowMenuItem(hikeExtrasName, 3));
			}
		}
		
		if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_REWARDS, false))
		{
			String rewards_name = accountPrefs.getString(HikeConstants.REWARDS_NAME, getApplicationContext().getString(R.string.rewards));
												
			if(!TextUtils.isEmpty(rewards_name))
			{
				optionsList.add(new OverFlowMenuItem(rewards_name, 4));
			}
		}

		optionsList.add(new OverFlowMenuItem(getString(R.string.settings), 5));

		optionsList.add(new OverFlowMenuItem(getString(R.string.status), 8));

		addEmailLogItem(optionsList);

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

				switch (item.getKey())
				{
				case HikeConstants.HOME_ACTIVITY_OVERFLOW.CREDITS:
					intent = new Intent(HomeActivity.this, CreditsActivity.class);
					break;
				case HikeConstants.HOME_ACTIVITY_OVERFLOW.INVITE_FRIENDS:
					intent = new Intent(HomeActivity.this, TellAFriend.class);
					break;
				case HikeConstants.HOME_ACTIVITY_OVERFLOW.HIKE_EXTRAS:
					editor.putBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, true);
					editor.commit();
					updateOverFlowMenuNotification();
					intent = IntentFactory.getGamingIntent(HomeActivity.this);
					break;
				case HikeConstants.HOME_ACTIVITY_OVERFLOW.REWARDS:
					editor.putBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, true);
					editor.commit();
					updateOverFlowMenuNotification();
					intent = IntentFactory.getRewardsIntent(HomeActivity.this);
					break;
				case HikeConstants.HOME_ACTIVITY_OVERFLOW.SETTINGS:
					HAManager.logClickEvent(HikeConstants.LogEvent.SETTING_CLICKED);
					intent = new Intent(HomeActivity.this, SettingsActivity.class);
					break;
				case HikeConstants.HOME_ACTIVITY_OVERFLOW.NEW_GROUP:
					intent = new Intent(HomeActivity.this, CreateNewGroupOrBroadcastActivity.class);
					break;
				case HikeConstants.HOME_ACTIVITY_OVERFLOW.TIMELINE:
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

					editor.putBoolean(HikeConstants.SHOW_TIMELINE_RED_DOT, false);
					editor.commit();
					intent = new Intent(HomeActivity.this, TimelineActivity.class);
					break;
				case HikeConstants.HOME_ACTIVITY_OVERFLOW.STATUS:
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
					break;
				case HikeConstants.HOME_ACTIVITY_OVERFLOW.LOGS:
					SendLogsTask logsTask = new SendLogsTask(HomeActivity.this);
					Utils.executeAsyncTask(logsTask);
					break;
				case HikeConstants.HOME_ACTIVITY_OVERFLOW.NEW_BROADCAST:
					
					sendBroadCastAnalytics();
					HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.SHOW_NEW_BROADCAST_RED_DOT, false);
					if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOW_BROADCAST_FTUE_SCREEN, true))
					{
						IntentFactory.createBroadcastFtue(HomeActivity.this);
					}
					else
					{
						IntentFactory.createBroadcastDefault(HomeActivity.this);
					}
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
		overFlowWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.overflow_menu_width));
		overFlowWindow.setHeight(LayoutParams.WRAP_CONTENT);
		/*
		 * In some devices Activity crashes and a BadTokenException is thrown by showAsDropDown method. Still need to find out exact repro of the bug.
		 */
		try
		{
			int rightMargin = getResources().getDimensionPixelSize(R.dimen.overflow_menu_right_margin);
			overFlowWindow.showAsDropDown(findViewById(R.id.overflow_anchor), -rightMargin, 0);
		}
		catch (BadTokenException e)
		{
			Logger.e(getClass().getSimpleName(), "Excepetion in HomeActivity Overflow popup", e);
		}
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

	private void addEmailLogItem(List<OverFlowMenuItem> overFlowMenuItems)
	{
		if (AppConfig.SHOW_SEND_LOGS_OPTION)
		{
			overFlowMenuItems.add(new OverFlowMenuItem("Send logs", 9));
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
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
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
			showingProgress = true;
			
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

	public void hikeLogoClicked()
	{
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
	
}
