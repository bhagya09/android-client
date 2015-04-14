package com.bsb.hike.ui;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Data;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.MESSAGE_TYPE;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ComposeChatAdapter;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.adapters.FriendsAdapter.FriendsListFetchedCallback;
import com.bsb.hike.adapters.FriendsAdapter.ViewType;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.media.PickContactParser;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MultipleConvMessage;
import com.bsb.hike.models.PhonebookContact;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.ContentLove;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformMessageMetadata;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.tasks.InitiateMultiFileTransferTask;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.LastSeenScheduler;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.TagEditText;
import com.bsb.hike.view.TagEditText.TagEditorListener;

public class ComposeChatActivity extends HikeAppStateBaseFragmentActivity implements TagEditorListener, OnItemClickListener, HikePubSub.Listener, OnScrollListener
{
	private static final String SELECT_ALL_MSISDN="all";
	
	private final String HORIZONTAL_FRIEND_FRAGMENT = "horizontalFriendFragment";
	
	private static int MIN_MEMBERS_GROUP_CHAT = 2;

	private static int MIN_MEMBERS_BROADCAST_LIST = 2;

	private static final int CREATE_GROUP_MODE = 1;

	private static final int START_CHAT_MODE = 2;
	
	private static final int MULTIPLE_FWD = 3;

    private static final int NUX_INCENTIVE_MODE = 6;
    
    private static final int CREATE_BROADCAST_MODE = 7;

	private View multiSelectActionBar, groupChatActionBar;

	private TagEditText tagEditText;

	private int composeMode;

	private ComposeChatAdapter adapter;

	int originalAdapterLength = 0;

	private TextView multiSelectTitle;

	private ListView listView;

	private TextView title;

	private boolean createGroup;

	private boolean createBroadcast;
	
	private boolean isForwardingMessage;

	private boolean isSharingFile;

	private String existingGroupOrBroadcastId;

	private volatile InitiateMultiFileTransferTask fileTransferTask;
	private PreFileTransferAsycntask prefileTransferTask;

	private ProgressDialog progressDialog;

	private LastSeenScheduler lastSeenScheduler;

	private String[] hikePubSubListeners = { HikePubSub.MULTI_FILE_TASK_FINISHED, HikePubSub.APP_FOREGROUNDED, HikePubSub.LAST_SEEN_TIME_UPDATED,
			HikePubSub.LAST_SEEN_TIME_BULK_UPDATED, HikePubSub.CONTACT_SYNC_STARTED, HikePubSub.CONTACT_SYNCED };

	private int previousFirstVisibleItem;

	private int velocity;

	private long previousEventTime;
	
	private HikePubSub mPubSub;

	private boolean showingMultiSelectActionBar = false;
	
	private List<ContactInfo> recentContacts;
	
	private boolean selectAllMode;
	
	private ViewStub composeCard;
	
	private View composeCardInflated;
	
	private boolean deviceDetailsSent;

	private boolean nuxIncentiveMode;
	
	private String groupName = null;

	private int triggerPointForPopup=ProductPopupsConstants.PopupTriggerPoints.UNKNOWN.ordinal();

	 private HorizontalFriendsFragment newFragment;
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		/* force the user into the reg-flow process if the token isn't set */
		if (Utils.requireAuth(this))
		{
			return;
		}

		// TODO this is being called everytime this activity is created. Way too
		// often
		HikeMessengerApp app = (HikeMessengerApp) getApplicationContext();
		app.connectToService();

		createGroup = getIntent().getBooleanExtra(HikeConstants.Extras.CREATE_GROUP, false);
		isForwardingMessage = getIntent().getBooleanExtra(HikeConstants.Extras.FORWARD_MESSAGE, false);
		isSharingFile = getIntent().getType() != null;
		nuxIncentiveMode = getIntent().getBooleanExtra(HikeConstants.Extras.NUX_INCENTIVE_MODE, false);
		createBroadcast = getIntent().getBooleanExtra(HikeConstants.Extras.CREATE_BROADCAST, false);

		// Getting the group id. This will be a valid value if the intent
		// was passed to add group participants.
		if (getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT))
		{
			existingGroupOrBroadcastId = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);
		}
		else if (getIntent().hasExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST))
		{
			existingGroupOrBroadcastId = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST);
		}

		if (getIntent().hasExtra(HikeConstants.Extras.GROUP_NAME))
		{
			groupName = getIntent().getStringExtra(HikeConstants.Extras.GROUP_NAME);
		}
		
		if (savedInstanceState != null)
		{
			deviceDetailsSent = savedInstanceState.getBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT);
		}
		
		if (!shouldInitiateFileTransfer())
		{
			Toast.makeText(this, getString(R.string.max_num_files_reached, FileTransferManager.getInstance(this).getTaskLimit()), Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		setContentView(R.layout.compose_chat);

		if (nuxIncentiveMode)
		{ 
			FragmentManager fm = getSupportFragmentManager();
			newFragment = (HorizontalFriendsFragment) fm.findFragmentByTag(HORIZONTAL_FRIEND_FRAGMENT);
			FragmentTransaction ft = fm.beginTransaction();
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
			
			if(newFragment == null) 
			{
				newFragment = new HorizontalFriendsFragment();
				ft.add(R.id.horizontal_friends_placeholder, newFragment, HORIZONTAL_FRIEND_FRAGMENT).commit();
			} 
			else 
			{
				ft.attach(newFragment).commit();
			}
			setListnerToRootView();
		} 
		Object object = getLastCustomNonConfigurationInstance();

		if (object instanceof InitiateMultiFileTransferTask)
		{
			fileTransferTask = (InitiateMultiFileTransferTask) object;
			progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.multi_file_creation));
		}
		else if ( object instanceof PreFileTransferAsycntask){
			prefileTransferTask = (PreFileTransferAsycntask) object;
			progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.multi_file_creation));
		}

		if (Intent.ACTION_SEND.equals(getIntent().getAction()) || Intent.ACTION_SENDTO.equals(getIntent().getAction())
				|| Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction()))
		{
			isForwardingMessage = true;
		}

		if(nuxIncentiveMode){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			getSupportActionBar().hide();
		}
		else{
			setActionBar();
		}

		init();
		if (getIntent().hasExtra(HikeConstants.Extras.BROADCAST_RECIPIENTS))
		{
			ArrayList<String> initiallySelectedMsisidns = getIntent().getStringArrayListExtra(HikeConstants.Extras.BROADCAST_RECIPIENTS);
			adapter.selectAllFromList(initiallySelectedMsisidns);
			adapter.notifyDataSetChanged();
			if (!(initiallySelectedMsisidns == null || initiallySelectedMsisidns.isEmpty()))
			{
				tagEditText.clear(false);
				int selected = adapter.getCurrentSelection();
				tagEditText.toggleTag(getString(selected==1 ? R.string.selected_contacts_count_singular : R.string.selected_contacts_count_plural,selected), SELECT_ALL_MSISDN, SELECT_ALL_MSISDN);
				setupMultiSelectActionBar();
				invalidateOptionsMenu();
			}
		}
		mPubSub = HikeMessengerApp.getPubSub();
		mPubSub.addListeners(this, hikePubSubListeners);
	}

	boolean isOpened = false;

	 public void setListnerToRootView(){
	    final View activityRootView = getWindow().getDecorView().findViewById(R.id.ll_compose); 
	    activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
	        @Override
	        public void onGlobalLayout() {

	            int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
	            if (heightDiff > 100 ) { // 99% of the time the height diff will be due to a keyboard.

	                if(isOpened == false){
						FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
						ft.hide(newFragment);
						ft.commit();
	                }
	                isOpened = true;
	            }else if(isOpened == true){
	                isOpened = false;
					FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
					if (tagEditText.getText().toString().length() == 0)
					{
						ft.show(newFragment);
						ft.commit();
					}
				}
			}
	    });
	}
	 
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT, deviceDetailsSent);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (!showingMultiSelectActionBar)
			getSupportMenuInflater().inflate(R.menu.compose_chat_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId() == R.id.refresh_contacts)
		{
			if(HikeMessengerApp.syncingContacts)
				return super.onOptionsItemSelected(item);
			if(!Utils.isUserOnline(this))
			{
				Utils.showNetworkUnavailableDialog(this);
				return super.onOptionsItemSelected(item);
			}
			Intent contactSyncIntent = new Intent(HikeService.MQTT_CONTACT_SYNC_ACTION);
			contactSyncIntent.putExtra(HikeConstants.Extras.MANUAL_SYNC, true);
			sendBroadcast(contactSyncIntent);
			
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.COMPOSE_REFRESH_CONTACTS);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
		}
		return super.onOptionsItemSelected(item);
	}
	
	private boolean shouldInitiateFileTransfer()
	{
		if (isSharingFile)
		{
			if (Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction()))
			{
				ArrayList<Uri> imageUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
				if (imageUris.size() > FileTransferManager.getInstance(this).remainingTransfers())
				{
					return false;
				}
			}
			else if (getIntent().hasExtra(Intent.EXTRA_STREAM))
			{
				if (FileTransferManager.getInstance(this).remainingTransfers() == 0)
				{
					return false;
				}
			}
		}
		else if (isForwardingMessage)
		{
			if (getIntent().hasExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT))
			{
				String jsonString = getIntent().getStringExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
				try
				{
					JSONArray multipleMsgFwdArray = new JSONArray(jsonString);

					int fileCount = 0;

					for (int i = 0; i < multipleMsgFwdArray.length(); i++)
					{
						JSONObject msgExtrasJson = (JSONObject) multipleMsgFwdArray.get(i);

						if (msgExtrasJson.has(HikeConstants.Extras.FILE_PATH))
						{
							fileCount++;
						}
					}

					if (fileCount > FileTransferManager.getInstance(this).remainingTransfers())
					{
						return false;
					}
				}
				catch (JSONException e)
				{
				}
			}
		}
		return true;
	}

	private void init()
	{
		setMode();
		listView = (ListView) findViewById(R.id.list);
		
		boolean showNujNotif = PreferenceManager.getDefaultSharedPreferences(ComposeChatActivity.this).getBoolean(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF, true);
		HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		boolean fetchRecentlyJoined = pref.getData(HikeConstants.SHOW_RECENTLY_JOINED_DOT, false) || pref.getData(HikeConstants.SHOW_RECENTLY_JOINED, false);
		fetchRecentlyJoined = fetchRecentlyJoined && !isForwardingMessage && showNujNotif;
		
		switch (composeMode)
		{
		case CREATE_BROADCAST_MODE:
			//We do not show sms contacts in broadcast mode
			adapter = new ComposeChatAdapter(this, listView, isForwardingMessage, (isForwardingMessage || isSharingFile), fetchRecentlyJoined, existingGroupOrBroadcastId, null, friendsListFetchedCallback, false);
			break;

		default:
			adapter = new ComposeChatAdapter(this, listView, isForwardingMessage, (isForwardingMessage || isSharingFile), fetchRecentlyJoined, existingGroupOrBroadcastId, null, friendsListFetchedCallback, !(isForwardingMessage||isSharingFile));
			break;
		}

		View emptyView = findViewById(android.R.id.empty);
		adapter.setEmptyView(emptyView);
		adapter.setLoadingView(findViewById(R.id.spinner));

		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		listView.setOnScrollListener(this);

		originalAdapterLength = adapter.getCount();

		initTagEditText();

		if (existingGroupOrBroadcastId != null)
		{
			MIN_MEMBERS_GROUP_CHAT = 1;
		}
		
		setModeAndUpdateAdapter(composeMode);
		
		adapter.setIsCreatingOrEditingGroup(this.composeMode == CREATE_GROUP_MODE || this.composeMode == CREATE_BROADCAST_MODE);

		adapter.executeFetchTask();
		
		pref.saveData(HikeConstants.SHOW_RECENTLY_JOINED_DOT, false);
		pref.saveData(HikeConstants.SHOW_RECENTLY_JOINED, false);
		
		if(triggerPointForPopup!=ProductPopupsConstants.PopupTriggerPoints.UNKNOWN.ordinal())
		{
			showProductPopup(triggerPointForPopup);
		}

		
	}

	private void initTagEditText()
	{
		tagEditText = (TagEditText) findViewById(R.id.composeChatNewGroupTagET);
		tagEditText.setListener(this);
		tagEditText.setMinCharChangeThreshold(1);
		// need to confirm with rishabh --gauravKhanna
		tagEditText.setMinCharChangeThresholdForTag(8);
		tagEditText.setSeparator(TagEditText.SEPARATOR_SPACE);
	}
	
	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if(adapter != null)
		{
			adapter.getIconLoader().setExitTasksEarly(true);
		}
	}
	
	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		if(adapter != null)
		{
			adapter.getIconLoader().setExitTasksEarly(false);
			adapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onDestroy()
	{
		if (progressDialog != null)
		{
			progressDialog.dismiss();
			progressDialog = null;
		}

		if (lastSeenScheduler != null)
		{
			lastSeenScheduler.stop(true);
			lastSeenScheduler = null;
		}

		HikeMessengerApp.getPubSub().removeListeners(this, hikePubSubListeners);
		super.onDestroy();
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		if (fileTransferTask != null)
		{
			return fileTransferTask;
		}
		else if(prefileTransferTask!=null){
			return prefileTransferTask;
		}
		else
		{
			return null;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		final ContactInfo contactInfo = adapter.getItem(arg2);

		// jugaad , coz of pinned listview , discussed with team
		if (ComposeChatAdapter.EXTRA_ID.equals(contactInfo.getId()))
		{
			Intent intent = new Intent(this, CreateNewGroupOrBroadcastActivity.class);
			startActivity(intent);
			return;
		}
		int viewtype;
		String name;
		switch (composeMode) {
		case CREATE_GROUP_MODE:
		case CREATE_BROADCAST_MODE:
			if (adapter.isContactPresentInExistingParticipants(contactInfo))
			{
				// basically it will work when you add participants to existing group via typing numbers
				showToast(getString(this.composeMode == CREATE_BROADCAST_MODE ? R.string.added_in_broadcast : R.string.added_in_group));
				return;
			}
			else if (adapter.getSelectedContactCount() >= HikeConstants.MAX_CONTACTS_IN_GROUP && !adapter.isContactAdded(contactInfo))
			{
				showToast(getString(this.composeMode == CREATE_BROADCAST_MODE ? R.string.maxContactInBroadcastErr : R.string.maxContactInGroupErr, HikeConstants.MAX_CONTACTS_IN_GROUP));
				return;
			}
			// for SMS users, append SMS text with name
			viewtype = adapter.getItemViewType(arg2);
			if (contactInfo.getName() == null)
			{
				contactInfo.setName(contactInfo.getMsisdn());
			}
			name = viewtype == ViewType.NOT_FRIEND_SMS.ordinal() ? contactInfo.getName() + " (SMS) " : contactInfo.getName();
			if(selectAllMode)
			{
				tagEditText.clear(false);
				if(adapter.isContactAdded(contactInfo)){
					adapter.removeContact(contactInfo);

				}else{
					adapter.addContact(contactInfo);

				}
				int selected = adapter.getCurrentSelection();
				if(selected>0){
				tagEditText.toggleTag(getString(selected==1 ? R.string.selected_contacts_count_singular : R.string.selected_contacts_count_plural,selected), SELECT_ALL_MSISDN, SELECT_ALL_MSISDN);
				}else{
					((CheckBox)findViewById(R.id.select_all_cb)).setChecked(false); // very rare case
				}
			}
			else
			{
				tagEditText.toggleTag(name, contactInfo.getMsisdn(), contactInfo);
			}
			break;
			
		default:
			Logger.i("composeactivity", contactInfo.getId() + " - id of clicked");
			if (FriendsAdapter.SECTION_ID.equals(contactInfo.getId()) || FriendsAdapter.EMPTY_ID.equals(contactInfo.getId()))
			{
				return;
			}

			if (isForwardingMessage)
			{
				
				// for SMS users, append SMS text with name

				viewtype = adapter.getItemViewType(arg2);
				if (contactInfo.getName() == null)
				{
					contactInfo.setName(contactInfo.getMsisdn());
				}
				
				if(selectAllMode){
					tagEditText.clear(false);
					if(adapter.isContactAdded(contactInfo)){
						adapter.removeContact(contactInfo);

					}else{
						adapter.addContact(contactInfo);

					}
					int selected = adapter.getSelectedContactCount();
					if(selected>0){
					tagEditText.toggleTag(getString(selected==1 ? R.string.selected_contacts_count_singular : R.string.selected_contacts_count_plural,selected), SELECT_ALL_MSISDN, SELECT_ALL_MSISDN);
					}else{
						((CheckBox)findViewById(R.id.select_all_cb)).setChecked(false); // very rare case
					}
				}
				else{
					name = viewtype == ViewType.NOT_FRIEND_SMS.ordinal() ? contactInfo.getName() + " (SMS) " : contactInfo.getName();
					if (!nuxIncentiveMode)
						// change is to prevent the Tags from appearing in the search bar.
						tagEditText.toggleTag(name, contactInfo.getMsisdn(),contactInfo);
					else {
						// newFragment.toggleViews(contactInfo);
						FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
						ft.show(newFragment);
						ft.commit();
						final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(tagEditText.getWindowToken(), 0);
						adapter.removeFilter();
						tagEditText.clear(false);
						if (adapter.isContactAdded(contactInfo))
						{
							if (newFragment.removeView(contactInfo))
								adapter.removeContact(contactInfo);

						}
						else
						{
							if (newFragment.addView(contactInfo))
								adapter.addContact(contactInfo);

						}
					}
				}
			}
			else
			{
				/*
				 * This would be true if the user entered a stealth msisdn and tried starting a chat with him/her in non stealth mode.
				 */
				if (HikeMessengerApp.isStealthMsisdn(contactInfo.getMsisdn()))
				{
					int stealthMode = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
					if (stealthMode != HikeConstants.STEALTH_ON)
					{
						return;
					}
				}

				Utils.startChatThread(this, contactInfo);
				finish();
			}
			break;
		}
	}

	@Override
	public void tagRemoved(Object data, String uniqueNess)
	{
		if(selectAllMode){
		((CheckBox) findViewById(R.id.select_all_cb)).setChecked(false);;
		}else{
			if(data instanceof ContactInfo){
				adapter.removeContact((ContactInfo) data);
			}
		}
		if (adapter.getCurrentSelection() == 0)
		{
			setActionBar();
			invalidateOptionsMenu();
		}
		else
		{
			multiSelectTitle.setText(createBroadcast ? getString(R.string.broadcast_selected, adapter.getCurrentSelection()) : 
				getString(R.string.gallery_num_selected, adapter.getCurrentSelection()));	
		}
	}

	@Override
	public void tagAdded(Object data, String uniqueNess)
	{
		String dataString = null;
		if(data instanceof ContactInfo){
		adapter.addContact((ContactInfo) data);
		}else if(data instanceof String)
		{
			dataString = (String) data;
		}

		setupMultiSelectActionBar();
		invalidateOptionsMenu();
		
		multiSelectTitle.setText(createBroadcast ? getString(R.string.broadcast_selected, adapter.getCurrentSelection()) : 
			getString(R.string.gallery_num_selected, adapter.getCurrentSelection()));
	}

	@Override
	public void characterAddedAfterSeparator(String characters)
	{
		adapter.onQueryChanged(characters);
	}

	@Override
	public void charResetAfterSeperator()
	{
		adapter.removeFilter();
		
//		We do not add 'Select all' button at the bottom for Group Chats
		if (!(this.composeMode == CREATE_GROUP_MODE))
		{
			setupForSelectAll();
		}
	}

	private void setMode(int mode)
	{
		this.composeMode = mode;
	}
	
	private void setMode()
	{
		int mode = START_CHAT_MODE; 
		if(getIntent().hasExtra(HikeConstants.Extras.COMPOSE_MODE))
		{
			mode = getIntent().getIntExtra(HikeConstants.Extras.COMPOSE_MODE, START_CHAT_MODE);
			triggerPointForPopup=ProductPopupsConstants.PopupTriggerPoints.BROADCAST.ordinal();
			
		}
		else if(nuxIncentiveMode)
		{
			mode = NUX_INCENTIVE_MODE;
		}
		else if (isForwardingMessage || isSharingFile)
		{
			mode = MULTIPLE_FWD;
		}
		else if(getIntent().hasExtra(HikeConstants.Extras.GROUP_BROADCAST_ID) || getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT))
		{
				mode=CREATE_GROUP_MODE;
		}
		else
		{
				mode=START_CHAT_MODE;
				triggerPointForPopup=ProductPopupsConstants.PopupTriggerPoints.COMPOSE_CHAT.ordinal();
		}
		setMode(mode);
	}
	
	private void setModeAndUpdateAdapter(int mode)
	{
		setMode(mode);
		switch (composeMode)
		{
		case CREATE_GROUP_MODE:
		case CREATE_BROADCAST_MODE:
			// createGroupHeader.setVisibility(View.GONE);
			adapter.showCheckBoxAgainstItems(true);
			tagEditText.clear(false);
			adapter.removeFilter();
			adapter.clearAllSelection(true);
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_group_mode);
			if (this.composeMode == CREATE_BROADCAST_MODE)
			{
				setupForSelectAll();	
			}
			break;
		case START_CHAT_MODE:
			// createGroupHeader.setVisibility(View.VISIBLE);
			tagEditText.clear(false);
			adapter.clearAllSelection(false);
			adapter.removeFilter();
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_chat_mode);
			return;
		case MULTIPLE_FWD:
			// createGroupHeader.setVisibility(View.GONE);
			adapter.showCheckBoxAgainstItems(true);
			tagEditText.clear(false);
			adapter.removeFilter();
			adapter.clearAllSelection(true);
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_group_mode);
			// select all bottom text
			setupForSelectAll();
			break;
		case NUX_INCENTIVE_MODE:
			adapter.showCheckBoxAgainstItems(true);
			tagEditText.clear(false);
			adapter.removeFilter();
			adapter.clearAllSelection(true);
			adapter.setNuxStateActive(true);
			NUXManager nm  = NUXManager.getInstance();
			adapter.preSelectContacts(nm.getLockedContacts(), nm.getUnlockedContacts());
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_group_mode);
			tagEditText.setHint(R.string.search);
			break;
		}
		if(!nuxIncentiveMode) 
			setTitle();
	}
	
	private void setupForSelectAll(){
		View selectAllCont = findViewById(R.id.select_all_container);
		selectAllCont.setVisibility(View.VISIBLE);
		final TextView tv = (TextView) selectAllCont.findViewById(R.id.select_all_text);
		tv.setText(getString(R.string.select_all_hike));
		CheckBox cb = (CheckBox) selectAllCont.findViewById(R.id.select_all_cb);
		cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					// call adapter select all
					selectAllMode = true;
					tv.setText(getString(R.string.unselect_all_hike));
//					adapter.clearAllSelection(true);
					if (composeMode == CREATE_BROADCAST_MODE)
					{
						if (adapter.getOnHikeContactsCount() > HikeConstants.MAX_CONTACTS_IN_BROADCAST)
						{
							showToast(getString(R.string.maxContactInBroadcastErr, HikeConstants.MAX_CONTACTS_IN_BROADCAST));
							tv.setText(getString(R.string.select_all_hike));
							buttonView.setChecked(false);
							return;
						}
					}
					adapter.selectAllContacts(true);
					tagEditText.clear(false);
					int selected = adapter.getCurrentSelection();
					tagEditText.toggleTag( getString(selected <=1 ? R.string.selected_contacts_count_singular : R.string.selected_contacts_count_plural,selected), SELECT_ALL_MSISDN, SELECT_ALL_MSISDN);
					
					try
					{
						JSONObject metadata = new JSONObject();
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SELECT_ALL_HIKE_CONTACTS);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					}
					catch(JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}
				}else{
					// call adapter unselect all
					selectAllMode = false;
					tv.setText(getString(R.string.select_all_hike));
					adapter.selectAllContacts(false);
					tagEditText.clear(true);
					setActionBar();
					invalidateOptionsMenu();
					
				}
				
			}
		});
		
	}

	private void createBroadcast(List<String> selectedContactList)
	{
		IntentFactory.createNewBroadcastActivityIntent(ComposeChatActivity.this, selectedContactList);
		finish();
	}
	
	private void setActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		if (groupChatActionBar == null)
		{
			groupChatActionBar = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);
		}

		if (actionBar.getCustomView() == groupChatActionBar)
		{
			return;
		}

		View backContainer = groupChatActionBar.findViewById(R.id.back);

		title = (TextView) groupChatActionBar.findViewById(R.id.title);
		groupChatActionBar.findViewById(R.id.seprator).setVisibility(View.GONE);
		
		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{

				onBackPressed();
			}
		});
		
		if(!nuxIncentiveMode)
			setTitle();
		
		if(HikeMessengerApp.syncingContacts)
		{
			// For showing progress bar when activity is closed and opened again
			showProgressBarContactsSync(View.VISIBLE);
		}

		actionBar.setCustomView(groupChatActionBar);

		showingMultiSelectActionBar = false;
	}

	private void setTitle()
	{
		if (createGroup)
		{
			title.setText(R.string.new_group);
		}
		else if (createBroadcast)
		{
			title.setText(R.string.new_broadcast);
		}
		else if (isSharingFile)
		{
			title.setText(R.string.share_file);
		}
		else if (isForwardingMessage)
		{
			title.setText(R.string.forward);
		}
		else if (!TextUtils.isEmpty(existingGroupOrBroadcastId) && this.composeMode == CREATE_GROUP_MODE)
		{
			title.setText(R.string.add_group);
		}
		else if (!TextUtils.isEmpty(existingGroupOrBroadcastId) && this.composeMode == CREATE_BROADCAST_MODE)
		{
			title.setText(R.string.add_broadcast);
		}
		else
		{
			title.setText(R.string.new_chat);
		}
	}

	private void setupMultiSelectActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		if (multiSelectActionBar == null)
		{
			multiSelectActionBar = LayoutInflater.from(this).inflate(R.layout.chat_theme_action_bar, null);
		}
		View sendBtn = multiSelectActionBar.findViewById(R.id.done_container);
		TextView save = (TextView) multiSelectActionBar.findViewById(R.id.save);
		if (createBroadcast)
		{
			save.setText(R.string.next_signup);
		}
		View closeBtn = multiSelectActionBar.findViewById(R.id.close_action_mode);
		ViewGroup closeContainer = (ViewGroup) multiSelectActionBar.findViewById(R.id.close_container);

		multiSelectTitle = (TextView) multiSelectActionBar.findViewById(R.id.title);
		
		multiSelectTitle.setText(createBroadcast ? getString(R.string.broadcast_selected, adapter.getCurrentSelection()) : 
			getString(R.string.gallery_num_selected, adapter.getCurrentSelection()));
		
		if (isForwardingMessage)
		{
			TextView send = (TextView) multiSelectActionBar.findViewById(R.id.save);
			send.setText(R.string.send);
		}

		sendBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (isForwardingMessage||isSharingFile)
				{
					forwardConfirmation(adapter.getAllSelectedContacts());
				}
				else if (createBroadcast)
				{
					int selected = adapter.getCurrentSelection();
					if (selected < MIN_MEMBERS_BROADCAST_LIST)
					{
						Toast.makeText(getApplicationContext(), getString(R.string.minContactInBroadcastErr, MIN_MEMBERS_BROADCAST_LIST), Toast.LENGTH_SHORT).show();;
						return;
					}
					sendBroadCastAnalytics();
					createBroadcast(adapter.getAllSelectedContactsMsisdns());
				}
				else if (getIntent().hasExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST))
				{
					OneToNConversationUtils.createGroupOrBroadcast(ComposeChatActivity.this, adapter.getAllSelectedContacts(), groupName);
				}
				else if (composeMode == CREATE_GROUP_MODE)
				{
					int selected = adapter.getCurrentSelection();
					if (selected < MIN_MEMBERS_GROUP_CHAT)
					{
						Toast.makeText(getApplicationContext(), getString(R.string.minContactInGroupErr, MIN_MEMBERS_GROUP_CHAT), Toast.LENGTH_SHORT).show();
						return;
					}
					OneToNConversationUtils.createGroupOrBroadcast(ComposeChatActivity.this, adapter.getAllSelectedContacts(), groupName);
				}
			}
		});

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				setModeAndUpdateAdapter(composeMode);
				if(selectAllMode)
				{
					View selectAllCont = findViewById(R.id.select_all_container);
					CheckBox cb = (CheckBox) selectAllCont.findViewById(R.id.select_all_cb);
					cb.setChecked(false);
				}
				setActionBar();
				invalidateOptionsMenu();
			}
		});

		if(HikeMessengerApp.syncingContacts)
		{
			showProgressBarContactsSync(View.VISIBLE);
		}
		actionBar.setCustomView(multiSelectActionBar);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);
		sendBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));

		showingMultiSelectActionBar = true;
	}

	private void forwardConfirmation(final ArrayList<ContactInfo> arrayList)
	{
		HikeDialogFactory.showDialog(this, HikeDialogFactory.FORWARD_CONFIRMATION_DIALOG, new HikeDialogListener()
		{
			
			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				hikeDialog.dismiss();
				forwardMultipleMessages(arrayList);
			}
			
			@Override
			public void neutralClicked(HikeDialog hikeDialog)
			{
			}
			
			@Override
			public void negativeClicked(HikeDialog hikeDialog)
			{
				hikeDialog.dismiss();
			}
		}, isSharingFile, arrayList);
	}
	
	private void forwardMultipleMessages(ArrayList<ContactInfo> arrayList)
	{
		Intent presentIntent = getIntent();
		if(isSharingFile){
	        
			Intent intent = null;
			if (arrayList.size() == 1) {
				intent = IntentFactory.createChatThreadIntentFromContactInfo(this, arrayList.get(0), true);
			} else {
				intent = Utils.getHomeActivityIntent(this);
			}
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			forwardMessageAsPerType(presentIntent, intent, arrayList);

	        /*
	         * If the intent action is ACTION_SEND_MULTIPLE then we don't need to start the activity here
	         * since we start an async task for initiating the file upload and an activity is started when
	         * that async task finishes execution.
	         */
		 	if (!Intent.ACTION_SEND_MULTIPLE.equals(presentIntent.getAction())&&arrayList.size()<=1)
	        {
	        	startActivity(intent);
	        	finish();
	        }
		}
		else
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.CONFIRM_FORWARD);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			// forwarding it is
			Intent intent = null;
			if(arrayList.size()==1)
			{
				// forwarding to 1 is special case , we want to create conversation if does not exist and land to recipient
				intent = IntentFactory.createChatThreadIntentFromMsisdn(this, arrayList.get(0).getMsisdn(), false);
				intent.putExtras(presentIntent);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
			}
			else
			{
				// multi forward to multi people
				if(presentIntent.hasExtra(HikeConstants.Extras.PREV_MSISDN)){
					// open chat thread from where we initiated
					String id = presentIntent.getStringExtra(HikeConstants.Extras.PREV_MSISDN);
					intent = IntentFactory.createChatThreadIntentFromMsisdn(this, id, false);
				}else{
					//home activity
					intent = Utils.getHomeActivityIntent(this);
				}
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				forwardMessageAsPerType(presentIntent, intent,arrayList);
			}
		}
	}

	private void forwardMessageAsPerType(Intent presentIntent, final Intent intent, ArrayList<ContactInfo> arrayList)
	{
		// update contact info sequence as per conversation ordering
		arrayList = updateContactInfoOrdering(arrayList);
		String type = presentIntent.getType();

		if (Intent.ACTION_SEND_MULTIPLE.equals(presentIntent.getAction()))
		{
			if (type != null)
			{
				ArrayList<Uri> imageUris = presentIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
				ArrayList<FileTransferData> fileTransferList = new ArrayList<ComposeChatActivity.FileTransferData>();
				
				if (imageUris != null)
				{
					boolean showMaxFileToast = false;

					ArrayList<Pair<String, String>> fileDetails = new ArrayList<Pair<String, String>>(imageUris.size());
					for (Uri fileUri : imageUris)
					{
						Logger.d(getClass().getSimpleName(), "File path uri: " + fileUri.toString());
						String fileUriStart = "file:";
						String fileUriString = fileUri.toString();

						String filePath;
						if (fileUriString.startsWith(fileUriStart))
						{
							File selectedFile = new File(URI.create(Utils.replaceUrlSpaces(fileUriString)));
							/*
							 * Done to fix the issue in a few Sony devices.
							 */
							filePath = selectedFile.getAbsolutePath();
						}
						else
						{
							filePath = Utils.getRealPathFromUri(fileUri, this);
						}

						File file = new File(filePath);
						if (file.length() > HikeConstants.MAX_FILE_SIZE)
						{
							showMaxFileToast = true;
							continue;
						}

						String fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(filePath));
						HikeFileType hikeFileType = HikeFileType.fromString(fileType, false);

						fileDetails.add(new Pair<String, String>(filePath, fileType));
						FileTransferData fileData = initialiseFileTransfer(filePath, null, hikeFileType, fileType, false, -1, true, arrayList);
						if(fileData!=null){
							fileTransferList.add(fileData);
						}
					}

					if (showMaxFileToast)
					{
						Toast.makeText(ComposeChatActivity.this, R.string.max_file_size, Toast.LENGTH_SHORT).show();
					}

					ContactInfo contactInfo = arrayList.get(0);
					String msisdn = OneToNConversationUtils.isGroupConversation(contactInfo.getMsisdn()) ? contactInfo.getId() : contactInfo.getMsisdn();
					boolean onHike = contactInfo.isOnhike();

					if (fileDetails.isEmpty())
					{
						return;
					}
					if(arrayList.size()==1){
						fileTransferTask = new InitiateMultiFileTransferTask(getApplicationContext(), fileDetails, msisdn, onHike, FTAnalyticEvents.OTHER_ATTACHEMENT);
						Utils.executeAsyncTask(fileTransferTask);
	
     					progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.multi_file_creation));

					}else if(!fileTransferList.isEmpty()){
						prefileTransferTask = new PreFileTransferAsycntask(fileTransferList,intent);
						Utils.executeAsyncTask(prefileTransferTask);
					}
					
					return;
				}
			}
		}
		else if (presentIntent.hasExtra(HikeConstants.Extras.FILE_KEY) )
		{
			intent.putExtras(presentIntent);
		}else if (presentIntent.hasExtra(StickerManager.FWD_CATEGORY_ID))
		{
			intent.putExtras(presentIntent);
		}else if ( presentIntent.hasExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT))
		{
			ArrayList<FileTransferData> fileTransferList = new ArrayList<ComposeChatActivity.FileTransferData>();
			ArrayList<ConvMessage> multipleMessageList = new ArrayList<ConvMessage>();
			String jsonString = presentIntent.getStringExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
			try
			{
				JSONArray multipleMsgFwdArray = new JSONArray(jsonString);
				JSONObject platformAnalyticsJson = new JSONObject();
				StringBuilder platformCards = new StringBuilder();
				int msgCount = multipleMsgFwdArray.length();
				for (int i = 0; i < msgCount; i++)
				{
					JSONObject msgExtrasJson = (JSONObject) multipleMsgFwdArray.get(i);
					if (msgExtrasJson.has(HikeConstants.Extras.MSG))
					{
						String msg = msgExtrasJson.getString(HikeConstants.Extras.MSG);
						// as we will be changing msisdn and hike status while inserting in DB
						ConvMessage convMessage = Utils.makeConvMessage(null, msg, true);
						//sendMessage(convMessage);
						multipleMessageList.add(convMessage);
					}else if(msgExtrasJson.has(HikeConstants.Extras.POKE)){
						// as we will be changing msisdn and hike status while inserting in DB
						ConvMessage convMessage = Utils.makeConvMessage(null, getString(R.string.poke_msg), true);
						JSONObject metadata = new JSONObject();
						try
						{
							metadata.put(HikeConstants.POKE, true);
							convMessage.setMetadata(metadata);
						}
						catch (JSONException e)
						{
							Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
						}
						multipleMessageList.add(convMessage);
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.FILE_PATH))
					{
						String fileKey = null;
						if (msgExtrasJson.has(HikeConstants.Extras.FILE_KEY))
						{
							fileKey = msgExtrasJson.getString(HikeConstants.Extras.FILE_KEY);
						}
						else
						{
						}
						String filePath = msgExtrasJson.getString(HikeConstants.Extras.FILE_PATH);
						String fileType = msgExtrasJson.getString(HikeConstants.Extras.FILE_TYPE);

						boolean isRecording = false;
						long recordingDuration = -1;
						if (msgExtrasJson.has(HikeConstants.Extras.RECORDING_TIME))
						{
							recordingDuration = msgExtrasJson.getLong(HikeConstants.Extras.RECORDING_TIME);
							isRecording = true;
							fileType = HikeConstants.VOICE_MESSAGE_CONTENT_TYPE;
						}

						HikeFileType hikeFileType = HikeFileType.fromString(fileType, isRecording);

						if (Utils.isPicasaUri(filePath))
						{
							FileTransferManager.getInstance(getApplicationContext()).uploadFile(Uri.parse(filePath), hikeFileType, ((ContactInfo)arrayList.get(0)).getMsisdn(), ((ContactInfo)arrayList.get(0)).isOnhike());
						}
						else
						{
							FileTransferData fileData = initialiseFileTransfer(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, true, arrayList);
							if(fileData!=null){
								fileTransferList.add(fileData);
							}
						}
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.LATITUDE) && msgExtrasJson.has(HikeConstants.Extras.LONGITUDE)
							&& msgExtrasJson.has(HikeConstants.Extras.ZOOM_LEVEL))
					{
						String fileKey = null;
						double latitude = msgExtrasJson.getDouble(HikeConstants.Extras.LATITUDE);
						double longitude = msgExtrasJson.getDouble(HikeConstants.Extras.LONGITUDE);
						int zoomLevel = msgExtrasJson.getInt(HikeConstants.Extras.ZOOM_LEVEL);
						initialiseLocationTransfer(latitude, longitude, zoomLevel,arrayList);
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.CONTACT_METADATA))
					{
						try
						{
							JSONObject contactJson = new JSONObject(msgExtrasJson.getString(HikeConstants.Extras.CONTACT_METADATA));
							initialiseContactTransfer(contactJson,arrayList);
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
					}
					else if (msgExtrasJson.has(StickerManager.FWD_CATEGORY_ID))
					{
						String categoryId = msgExtrasJson.getString(StickerManager.FWD_CATEGORY_ID);
						String stickerId = msgExtrasJson.getString(StickerManager.FWD_STICKER_ID);
						Sticker sticker = new Sticker(categoryId, stickerId);
						multipleMessageList.add(sendSticker(sticker, categoryId, arrayList, StickerManager.FROM_FORWARD));
						boolean isDis = sticker.isDisabled(sticker, this.getApplicationContext());
						// add this sticker to recents if this sticker is not disabled
						if (!isDis)
							StickerManager.getInstance().addRecentSticker(sticker);
						/*
						 * Making sure the sticker is not forwarded again on orientation change
						 */
						presentIntent.removeExtra(StickerManager.FWD_CATEGORY_ID);
					}else if(msgExtrasJson.optInt(MESSAGE_TYPE.MESSAGE_TYPE) == MESSAGE_TYPE.CONTENT){
						// CONTENT Message
						String metadata = msgExtrasJson.optString(HikeConstants.METADATA);
						int loveId = msgExtrasJson.optInt(HikeConstants.ConvMessagePacketKeys.LOVE_ID);
						loveId = loveId==0 ? -1 : loveId;
						ConvMessage convMessage = new ConvMessage();
						convMessage.contentLove = new ContentLove();
						convMessage.contentLove.loveId = loveId;
                        convMessage.setMessageType(MESSAGE_TYPE.CONTENT);
						convMessage.platformMessageMetadata = new PlatformMessageMetadata(metadata, getApplicationContext());
                        convMessage.setIsSent(true);
                        convMessage.setMessage(convMessage.platformMessageMetadata.notifText);
						multipleMessageList.add(convMessage);
					} else if(msgExtrasJson.optInt(MESSAGE_TYPE.MESSAGE_TYPE) == MESSAGE_TYPE.WEB_CONTENT || msgExtrasJson.optInt(MESSAGE_TYPE.MESSAGE_TYPE) == MESSAGE_TYPE.FORWARD_WEB_CONTENT){
						//Web content message
						String metadata = msgExtrasJson.optString(HikeConstants.METADATA);

						ConvMessage convMessage = new ConvMessage();
						convMessage.setIsSent(true);
						convMessage.setMessageType(MESSAGE_TYPE.FORWARD_WEB_CONTENT);
						convMessage.webMetadata =  new WebMetadata(PlatformContent.getForwardCardData(metadata));

						try
						{
							platformCards.append( TextUtils.isEmpty(platformCards) ? convMessage.webMetadata.getAppName() : "," + convMessage.webMetadata.getAppName());
						}
						catch (NullPointerException e)
						{
							e.printStackTrace();
						}
						convMessage.setMessage(msgExtrasJson.getString(HikeConstants.HIKE_MESSAGE));
						multipleMessageList.add(convMessage);
					}
					/*
					 * Since the message was not forwarded, we check if we have any drafts saved for this conversation, if we do we enter it in the compose box.
					 */
				}
				platformAnalyticsJson.put(HikePlatformConstants.CARD_TYPE, platformCards);
				if(!fileTransferList.isEmpty()){
					prefileTransferTask = new PreFileTransferAsycntask(fileTransferList,intent);
					Utils.executeAsyncTask(prefileTransferTask);
				}else{
					// if file trasfer started then it will show toast
					Toast.makeText(getApplicationContext(), getString(R.string.messages_sent_succees), Toast.LENGTH_LONG).show();
				}
				if(multipleMessageList.size() ==0 || arrayList.size()==0){
					if(fileTransferList.isEmpty()){
						// if it is >0 then onpost execute of PreFileTransferAsycntask will start intent
						startActivity(intent);
						finish();
					}
					return;
				}else if(isSharingFile){
					ConvMessage convMessage = multipleMessageList.get(0);
					convMessage.setMsisdn(arrayList.get(0).getMsisdn());
					intent.putExtra(HikeConstants.Extras.MSISDN, convMessage.getMsisdn());
					sendMessage(convMessage);
				}else{
					sendMultiMessages(multipleMessageList,arrayList,platformAnalyticsJson,false);
					if(fileTransferList.isEmpty()){
						// if it is >0 then onpost execute of PreFileTransferAsycntask will start intent
						startActivity(intent);
						finish();
					}
				}
				
			}
			catch (JSONException e)
			{
				Logger.e(getClass().getSimpleName(), "Invalid JSON Array", e);
			}
			presentIntent.removeExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
		}
		else if (type != null && presentIntent.hasExtra(Intent.EXTRA_STREAM))
		{
			Uri fileUri = presentIntent.getParcelableExtra(Intent.EXTRA_STREAM);
			if (type.startsWith(HikeConstants.SHARE_CONTACT_CONTENT_TYPE))
			{
				String lookupKey = fileUri.getLastPathSegment();

        		String[] projection = new String[] { Data.CONTACT_ID };
        		String selection = Data.LOOKUP_KEY + " =?";
        		String[] selectionArgs = new String[] { lookupKey };

        		Cursor c = getContentResolver().query(Data.CONTENT_URI, projection, selection, selectionArgs, null);

        		int contactIdIdx = c.getColumnIndex(Data.CONTACT_ID);
        		String contactId = null;
        		while(c.moveToNext())
        		{
        			contactId = c.getString(contactIdIdx);
        			if(!TextUtils.isEmpty(contactId))
        				break;
        		}
				if (arrayList.size() == 1) {
					intent.putExtra(HikeConstants.Extras.CONTACT_ID, contactId);
					intent.putExtra(HikeConstants.Extras.FILE_TYPE, type);
				} else {
					PhonebookContact contact = PickContactParser.getContactData(contactId, this);
					final ArrayList<ContactInfo> finalArrayList = arrayList;
					if (contact != null)
					{
						HikeDialogFactory.showDialog(this, HikeDialogFactory.CONTACT_SEND_DIALOG, new HikeDialogListener()
						{
							
							@Override
							public void positiveClicked(HikeDialog hikeDialog)
							{
								initialiseContactTransfer(((PhonebookContact) hikeDialog.data).jsonData,finalArrayList);
								hikeDialog.dismiss();
								startActivity(intent);
					      		finish();
							}
							
							@Override
							public void neutralClicked(HikeDialog hikeDialog)
							{
							}
							
							@Override
							public void negativeClicked(HikeDialog hikeDialog)
							{
								hikeDialog.dismiss();
								startActivity(intent);
					      		finish();
							}
						}, contact, getString(R.string.send), false);
					}

				}
			}
			else
			{
				Logger.d(getClass().getSimpleName(), "File path uri: " + fileUri.toString());
				ArrayList<FileTransferData> fileTransferList = new ArrayList<ComposeChatActivity.FileTransferData>();
				fileUri = Utils.makePicasaUri(fileUri);
				String fileUriStart = "file:";
				String fileUriString = fileUri.toString();
				String filePath;
				if (Utils.isPicasaUri(fileUriString))
				{
					filePath = fileUriString;
				}
				else if (fileUriString.startsWith(fileUriStart))
				{
					File selectedFile = new File(URI.create(Utils.replaceUrlSpaces(fileUriString)));
					/*
					 * Done to fix the issue in a few Sony devices.
					 */
					filePath = selectedFile.getAbsolutePath();
				}
				else
				{
					filePath = Utils.getRealPathFromUri(fileUri, this);
				}
	
				if (TextUtils.isEmpty(filePath))
				{
					Toast.makeText(getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
					return;
				}
	
				File file = new File(filePath);
				if (file.length() > HikeConstants.MAX_FILE_SIZE)
				{
					Toast.makeText(ComposeChatActivity.this, R.string.max_file_size, Toast.LENGTH_SHORT).show();
					return;
				}
	
				type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(filePath));
				if (type == null)
					type = presentIntent.getType();
	
				intent.putExtra(HikeConstants.Extras.FILE_PATH, filePath);
				intent.putExtra(HikeConstants.Extras.FILE_TYPE, type);
				if (arrayList.size() > 1) {
					
					HikeFileType hikeFileType = HikeFileType.fromString(
							type, false);
					FileTransferData fileData = initialiseFileTransfer(
							filePath, null, hikeFileType, type, false, -1,
							true, arrayList);
					if (fileData != null) {
						fileTransferList.add(fileData);

					}
					if (!fileTransferList.isEmpty()) {
						prefileTransferTask = new PreFileTransferAsycntask(
								fileTransferList, intent);
						Utils.executeAsyncTask(prefileTransferTask);
					}
				}
			}
		}
		else if (presentIntent.hasExtra(Intent.EXTRA_TEXT) || presentIntent.hasExtra(HikeConstants.Extras.MSG))
		{
			String msg = presentIntent.getStringExtra(presentIntent.hasExtra(HikeConstants.Extras.MSG) ? HikeConstants.Extras.MSG : Intent.EXTRA_TEXT);
			Logger.d(getClass().getSimpleName(), "Contained a message: " + msg);
			if(msg == null){
				Bundle extraText = presentIntent.getExtras();
				if(extraText.get(Intent.EXTRA_TEXT) != null)
					msg = extraText.get(Intent.EXTRA_TEXT).toString();
			}
			if(msg == null)
				Toast.makeText(getApplicationContext(), R.string.text_empty_error, Toast.LENGTH_SHORT).show();
			else
			{
				if(arrayList.size()==1){
					ContactInfo contact = (ContactInfo) arrayList.get(0);
					if(contact != null)
					{
						ConvMessage convMessage = Utils.makeConvMessage(contact.getMsisdn(), msg, contact.isOnhike());
						sendMessage(convMessage);
					}
				}else{
					ArrayList<ConvMessage> multipleMessageList = new ArrayList<ConvMessage>();
					ConvMessage convMessage = Utils.makeConvMessage(null, msg, true);
					multipleMessageList.add(convMessage);
					sendMultiMessages(multipleMessageList, arrayList, null, false);
					startActivity(intent);
					finish();
					
				}
				
			}
		}
	}
	
	private ArrayList<ContactInfo> updateContactInfoOrdering(ArrayList<ContactInfo> arrayList){
		Set<ContactInfo> set = new HashSet<ContactInfo>(arrayList);
		ArrayList<ContactInfo> toReturn = new ArrayList<ContactInfo>();
		List<ContactInfo> conversations = getRecentContacts();
		int total = conversations.size();
		// we want to maintain ordering, conversations on home screen must appear in same order they were before multi forward
		// we are adding from last to first , so that when db entry is made timestamp for last is less than first
		for(int i=0;i<total;i++){
			ContactInfo contactInfo = conversations.get(i);
			if(set.contains(contactInfo)){
				toReturn.add(contactInfo);
				set.remove(contactInfo);
			}
		}
		toReturn.addAll(set);
		return toReturn;
	}

	private void sendMultiMessages(ArrayList<ConvMessage> multipleMessageList, ArrayList<ContactInfo> arrayList, JSONObject platformAnalyticsJson, boolean createChatThread)
	{
		if(platformAnalyticsJson!=null)
		{
		try
		{
			StringBuilder contactList = new StringBuilder();
			for (ContactInfo contactInfo : arrayList)
			{
				contactList.append(TextUtils.isEmpty(contactList) ? contactInfo.getMsisdn() : "," + contactInfo.getMsisdn());
			}
			platformAnalyticsJson.put(AnalyticsConstants.TO, contactList);
			platformAnalyticsJson.put(HikeConstants.EVENT_KEY, HikePlatformConstants.CARD_FORWARD);
			HikeAnalyticsEvent.analyticsForCards(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, platformAnalyticsJson);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		}
		MultipleConvMessage multiMessages = new MultipleConvMessage(multipleMessageList, arrayList, System.currentTimeMillis() / 1000, createChatThread, null);
		mPubSub.publish(HikePubSub.MULTI_MESSAGE_SENT, multiMessages);
	}

	private void sendMessage(ConvMessage convMessage)
	{
		
		mPubSub.publish(HikePubSub.MESSAGE_SENT, convMessage);
		
	}
	

	@Override
	public void onEventReceived(String type, Object object)
	{
		super.onEventReceived(type, object);

		if (HikePubSub.MULTI_FILE_TASK_FINISHED.equals(type))
		{
			final String msisdn = fileTransferTask.getMsisdn();

			fileTransferTask = null;

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					Intent intent = IntentFactory.createChatThreadIntentFromMsisdn(ComposeChatActivity.this, msisdn, false); 
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();

					if (progressDialog != null)
					{
						progressDialog.dismiss();
						progressDialog = null;
					}
				}
			});
		}
		else if (HikePubSub.APP_FOREGROUNDED.equals(type))
		{

			if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HikeConstants.LAST_SEEN_PREF, true))
			{
				return;
			}

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (lastSeenScheduler == null)
					{
						lastSeenScheduler = LastSeenScheduler.getInstance(ComposeChatActivity.this);
					}
					else
					{
						lastSeenScheduler.stop(true);
					}
					lastSeenScheduler.start(true);
				}
			});
		}
		else if (HikePubSub.LAST_SEEN_TIME_BULK_UPDATED.equals(type))
		{
			List<ContactInfo> friendsList = adapter.getFriendsList();

			Utils.updateLastSeenTimeInBulk(friendsList);

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					adapter.makeCompleteList(false);
				}
			});

		}
		else if (HikePubSub.LAST_SEEN_TIME_UPDATED.equals(type))
		{
			final ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo.getFavoriteType() != FavoriteType.FRIEND)
			{
				return;
			}

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					adapter.addToGroup(contactInfo, FriendsAdapter.FRIEND_INDEX);
				}

			});
		}
		else if(HikePubSub.CONTACT_SYNC_STARTED.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					// For showing auto/manual sync progress bar when already on the activity
					showProgressBarContactsSync(View.VISIBLE);
				}

			});
		}
		else if (HikePubSub.CONTACT_SYNCED.equals(type))
		{
			Boolean[] ret = (Boolean[]) object;
			final boolean contactsChanged = ret[1];
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					// Dont repopulate list if no sync changes
					if(contactsChanged)
						adapter.executeFetchTask();
					showProgressBarContactsSync(View.GONE);
				}

			});
		}
	}

	private void showProgressBarContactsSync(int value)
	{
		ProgressBar progress_bar = null;
		if(groupChatActionBar!=null)
		{
			progress_bar = (ProgressBar)groupChatActionBar.findViewById(R.id.loading_progress);
			progress_bar.setVisibility(value);
		}
		if(multiSelectActionBar!=null)
		{
			progress_bar = (ProgressBar)multiSelectActionBar.findViewById(R.id.loading_progress);
			progress_bar.setVisibility(value);
		}
	}
	@Override
	public void onBackPressed()
	{
		if (composeMode == CREATE_GROUP_MODE || composeMode == CREATE_BROADCAST_MODE)
		{
			if (existingGroupOrBroadcastId != null || createGroup || createBroadcast)
			{
				ComposeChatActivity.this.finish();
//				Hiding keyboard on pressing back on "Add members to broadcast list" compose chat
				Utils.hideSoftKeyboard(ComposeChatActivity.this, tagEditText);
				return;
			}
			setModeAndUpdateAdapter(START_CHAT_MODE);
			return;
		}
		else if (composeMode == MULTIPLE_FWD)
		{
			ComposeChatActivity.this.finish();
			return;
		}
		super.onBackPressed();
	}

	private void showToast(String message)
	{
		Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
	}

	FriendsListFetchedCallback friendsListFetchedCallback = new FriendsListFetchedCallback()
	{

		@Override
		public void listFetched()
		{
			if(getIntent().getBooleanExtra(HikeConstants.Extras.SELECT_ALL_INITIALLY, false))
			{
				View selectAllCont = findViewById(R.id.select_all_container);
				CheckBox cb = (CheckBox) selectAllCont.findViewById(R.id.select_all_cb);
				cb.setChecked(true);
			}

			if (PreferenceManager.getDefaultSharedPreferences(ComposeChatActivity.this).getBoolean(HikeConstants.LAST_SEEN_PREF, true))
			{
				lastSeenScheduler = LastSeenScheduler.getInstance(ComposeChatActivity.this);
				lastSeenScheduler.start(true);
			}
		}

		@Override
		public void completeListFetched() {
			if (adapter != null)
			{
				if (adapter.getCompleteList().size() <= 0)
				{
					View selectAllCont = findViewById(R.id.select_all_container);
					selectAllCont.setVisibility(View.GONE);
				}
			}
			
		}
	};

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (previousFirstVisibleItem != firstVisibleItem)
		{
			long currTime = System.currentTimeMillis();
			long timeToScrollOneElement = currTime - previousEventTime;
			velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

			previousFirstVisibleItem = firstVisibleItem;
			previousEventTime = currTime;
		}

		if (adapter == null)
		{
			return;
		}

		adapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES_SMALL);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
	}
	public ConvMessage sendSticker(Sticker sticker, String categoryIdIfUnknown, ArrayList<ContactInfo> arrayList, String source)
	{
		ConvMessage convMessage = Utils.makeConvMessage(((ContactInfo) arrayList.get(0)).getMsisdn(), "Sticker", ((ContactInfo) arrayList.get(0)).isOnhike());
	
		JSONObject metadata = new JSONObject();
		try
		{
			String categoryId = sticker.getCategoryId();
			metadata.put(StickerManager.CATEGORY_ID, categoryId);

			metadata.put(StickerManager.STICKER_ID, sticker.getStickerId());
			
			if(!source.equalsIgnoreCase(StickerManager.FROM_OTHER))
			{
				metadata.put(StickerManager.SEND_SOURCE, source);
			}
			convMessage.setMetadata(metadata);
			Logger.d(getClass().getSimpleName(), "metadata: " + metadata.toString());
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		return convMessage;
		//sendMessage(convMessage);
	}
	private FileTransferData initialiseFileTransfer(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
			boolean isForwardingFile, ArrayList<ContactInfo> arrayList)
	{
		clearTempData();
		if (filePath == null)
		{
			Toast.makeText(getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
			return null;
		}
		File file = new File(filePath);
		Logger.d(getClass().getSimpleName(), "File size: " + file.length() + " File name: " + file.getName());

		if (HikeConstants.MAX_FILE_SIZE != -1 && HikeConstants.MAX_FILE_SIZE < file.length())
		{
			Toast.makeText(getApplicationContext(), R.string.max_file_size, Toast.LENGTH_SHORT).show();
			return null;
		}
		return new FileTransferData(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, isForwardingFile, arrayList, file);
	}
	private void clearTempData()
	{
		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
		editor.remove(HikeMessengerApp.TEMP_NAME);
		editor.remove(HikeMessengerApp.TEMP_NUM);
		editor.commit();
	}
	private void initialiseLocationTransfer(double latitude, double longitude, int zoomLevel, ArrayList<ContactInfo> arrayList)
	{
		clearTempData();
		for(ContactInfo contactInfo:arrayList){
		FileTransferManager.getInstance(getApplicationContext()).uploadLocation(contactInfo.getMsisdn(), latitude, longitude, zoomLevel, ((ContactInfo)arrayList.get(0)).isOnhike());
		}
	}
	private void initialiseContactTransfer(JSONObject contactJson, ArrayList<ContactInfo> arrayList)
	{
		for(ContactInfo contactInfo:arrayList){
		FileTransferManager.getInstance(getApplicationContext()).uploadContact(contactInfo.getMsisdn(), contactJson, (((ContactInfo)arrayList.get(0)).isOnhike()));
		}
	}

	
	List<ContactInfo> getRecentContacts()
	{
		if(recentContacts == null)
		{
			recentContacts = ContactManager.getInstance().getAllConversationContactsSorted(false, true);
			Collections.reverse(recentContacts);
		}
		return recentContacts;
	}
	
	private static class FileTransferData{
		String filePath,fileKey,fileType;
		HikeFileType hikeFileType;
		boolean isRecording,isForwardingFile;
		long recordingDuration;
		ArrayList<ContactInfo> arrayList;
		File file;
		public FileTransferData(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
				boolean isForwardingFile, ArrayList<ContactInfo> arrayList,File file){
				this.filePath = filePath;
				this.fileKey = fileKey;
				this.hikeFileType = hikeFileType;
				this.fileType = fileType;
				this.isRecording = isRecording;
				this.recordingDuration = recordingDuration;
				this.arrayList = arrayList;
				this.file = file;
			}
	}
	private class PreFileTransferAsycntask extends AsyncTask<Void, Void, Void>{
		
		ArrayList<FileTransferData> files;
		Intent intent;
		PreFileTransferAsycntask(ArrayList<FileTransferData> files,Intent intent){
			this.files = files;
			this.intent = intent;
		}
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			progressDialog = ProgressDialog.show(ComposeChatActivity.this, null, getResources().getString(R.string.multi_file_creation));
		}
		@Override
		protected Void doInBackground(Void... params) {
			for(FileTransferData file:files){
			FileTransferManager.getInstance(getApplicationContext()).uploadFile(file.arrayList, file.file, file.fileKey, file.fileType, file.hikeFileType, file.isRecording, file.isForwardingFile,
					((ContactInfo)file.arrayList.get(0)).isOnhike(), file.recordingDuration,  FTAnalyticEvents.OTHER_ATTACHEMENT);
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			Toast.makeText(getApplicationContext(), getString(R.string.messages_sent_succees), Toast.LENGTH_LONG).show();
			super.onPostExecute(result);
			if(progressDialog!=null){
			progressDialog.dismiss();
			progressDialog = null;
			}
			startActivity(intent);
			finish();
			prefileTransferTask=null;
		}
		
	}
	
	private void sendDetailsAfterSignup(boolean sendBot)
    {
      SharedPreferences accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
      boolean justSignedUp = accountPrefs.getBoolean(HikeMessengerApp.JUST_SIGNED_UP, false);
      Logger.d("nux","send details after signup");
      if (justSignedUp)
      {
              Logger.d("nux","sendbot ="+sendBot);
              Editor editor = accountPrefs.edit();
              editor.remove(HikeMessengerApp.JUST_SIGNED_UP);
              editor.commit();

              if (!deviceDetailsSent)
              {
                      // Request for sending Bot after user skips or sends a sticker from ftue screen
                      Utils.sendDetailsAfterSignup(this, false, sendBot);
                      deviceDetailsSent = true;
              }
      }
   }
	
	private void sendBroadCastAnalytics()
	{
		if(selectAllMode)
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.BROADCAST_SELECT_ALL_NEXT);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

		}
		else
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.BROADCAST_NEXT_MULTI_CONTACT);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
		}
	}
}
