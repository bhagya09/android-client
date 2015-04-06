package com.bsb.hike.ui;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.adapters.ProfileAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.ImageViewerInfo;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.models.ProfileItem.ProfileStatusItem;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.Conversation.BroadcastConversation;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.GroupConversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.DownloadImageTask;
import com.bsb.hike.tasks.DownloadImageTask.ImageDownloadResult;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.ui.fragments.PhotoViewerFragment;
import com.bsb.hike.utils.ChangeProfileImageBaseActivity;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontEditText;
import com.bsb.hike.voip.VoIPUtils;

public class ProfileActivity extends ChangeProfileImageBaseActivity implements FinishableEvent, Listener, OnLongClickListener, OnItemLongClickListener, OnScrollListener,
		View.OnClickListener
{
	private TextView mName;
	
	private EditText mNameEdit;

	private View currentSelection;

	private Dialog mDialog;

	private String mLocalMSISDN = null;

	private ActivityState mActivityState; /* config state of this activity */

	private String nameTxt;

	private boolean isBackPressed = false;

	private EditText mEmailEdit;

	private String emailTxt;

	private Map<String, PairModified<GroupParticipant, String>> participantMap;

	private ProfileType profileType;

	private String httpRequestURL;

	private String groupOwner;

	private int lastSavedGender;

	private SharedPreferences preferences;

	private String[] groupInfoPubSubListeners = { HikePubSub.ICON_CHANGED, HikePubSub.GROUP_NAME_CHANGED, HikePubSub.GROUP_END, HikePubSub.PARTICIPANT_JOINED_ONETONCONV,
			HikePubSub.PARTICIPANT_LEFT_ONETONCONV, HikePubSub.USER_JOINED, HikePubSub.USER_LEFT, HikePubSub.LARGER_IMAGE_DOWNLOADED, HikePubSub.PROFILE_IMAGE_DOWNLOADED,
			HikePubSub.ClOSE_PHOTO_VIEWER_FRAGMENT, HikePubSub.DELETE_MESSAGE, HikePubSub.CONTACT_ADDED, HikePubSub.UNREAD_PIN_COUNT_RESET, HikePubSub.MESSAGE_RECEIVED, HikePubSub.BULK_MESSAGE_RECEIVED };

	private String[] contactInfoPubSubListeners = { HikePubSub.ICON_CHANGED, HikePubSub.CONTACT_ADDED, HikePubSub.USER_JOINED, HikePubSub.USER_LEFT,
			HikePubSub.STATUS_MESSAGE_RECEIVED, HikePubSub.FAVORITE_TOGGLED, HikePubSub.FRIEND_REQUEST_ACCEPTED, HikePubSub.REJECT_FRIEND_REQUEST,
			HikePubSub.HIKE_JOIN_TIME_OBTAINED, HikePubSub.LARGER_IMAGE_DOWNLOADED, HikePubSub.PROFILE_IMAGE_DOWNLOADED,
			HikePubSub.ClOSE_PHOTO_VIEWER_FRAGMENT, HikePubSub.CONTACT_DELETED, HikePubSub.DELETE_MESSAGE };

	private String[] profilePubSubListeners = { HikePubSub.USER_JOIN_TIME_OBTAINED, HikePubSub.LARGER_IMAGE_DOWNLOADED, HikePubSub.STATUS_MESSAGE_RECEIVED,
			HikePubSub.ICON_CHANGED, HikePubSub.PROFILE_IMAGE_DOWNLOADED };

	private String[] profilEditPubSubListeners = { HikePubSub.PROFILE_UPDATE_FINISH };

	private OneToNConversation oneToNConversation;
	
	private ImageButton topBarBtn;

	private ContactInfo contactInfo;

	private boolean isBlocked;

	private Dialog groupEditDialog;

	private boolean showingRequestItem = false;
	
	private Boolean showingGroupEdit = false;
	
	public static final String ORIENTATION_FLAG = "of";
	
	private ProfileItem.ProfileSharedMedia sharedMediaItem;
	
	private ProfileItem.ProfileSharedContent sharedContentItem;

	public static final String PROFILE_PIC_SUFFIX = "profilePic";

	private static enum ProfileType
	{
		USER_PROFILE, // The user profile screen
		USER_PROFILE_EDIT, // The user profile edit screen
		GROUP_INFO, // The group info screen
		BROADCAST_INFO,
		CONTACT_INFO, // Contact info screen
		CONTACT_INFO_TIMELINE //Contact's Timeline screen
	};

	private class ActivityState
	{
		public HikeHTTPTask task; /* the task to update the global profile */

		public DownloadImageTask downloadPicasaImageTask; /*
														 * the task to download the picasa image
														 */

		public HikeHTTPTask getHikeJoinTimeTask;

		public String destFilePath = null; /*
											 * the bitmap before the user saves it
											 */

		public int genderType;

		public boolean groupEditDialogShowing = false;

		public String edittedGroupName = null;
	}

	public File selectedFileIcon;

	private ListView profileContent;

	private ProfileAdapter profileAdapter;

	private List<ProfileItem> profileItems;

	private boolean isGroupOwner;

	private Menu mMenu;
	
	private int sharedMediaCount = 0;
	
	private int sharedPinCount = 0;
	
	private int sharedFileCount = 0;
	
	private int unreadPinCount = 0;
	
	private int currUnreadCount = 0;
	
	private static final int MULTIPLIER = 3;  //multiplication factor for 3X loading media items initially
	
	private int maxMediaToShow = 0;

	private View headerView;
	
	public SmileyParser smileyParser;
	
	int triggerPointPopup=ProductPopupsConstants.PopupTriggerPoints.UNKNOWN.ordinal();
	
	private static final String TAG = "Profile_Activity";
	/* store the task so we can keep keep the progress dialog going */
	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		Logger.d("ProfileActivity", "onRetainNonConfigurationinstance");
		return mActivityState;
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		if (profileAdapter != null)
		{
			profileAdapter.getTimelineImageLoader().setExitTasksEarly(true);
			profileAdapter.getIconImageLoader().setExitTasksEarly(true);
			profileAdapter.getProfilePicImageLoader().setExitTasksEarly(true);
			
			if(profileAdapter.getSharedFileImageLoader()!=null)
			{
				profileAdapter.getSharedFileImageLoader().setExitTasksEarly(true);
			}
		}
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
		if (groupEditDialog != null)
		{
			if (mNameEdit != null)
			{
				mActivityState.edittedGroupName = mNameEdit.getText().toString();
			}
			groupEditDialog.dismiss();
			groupEditDialog = null;
		}
		if ((mActivityState != null) && (mActivityState.task != null))
		{
			mActivityState.task.setActivity(null);
		}
		if ((mActivityState != null) && (mActivityState.getHikeJoinTimeTask != null))
		{
			mActivityState.getHikeJoinTimeTask.cancel(true);
		}
		if (profileType == ProfileType.GROUP_INFO || profileType == ProfileType.BROADCAST_INFO)
		{
			HikeMessengerApp.getPubSub().removeListeners(this, groupInfoPubSubListeners);
		}
		else if (profileType == ProfileType.CONTACT_INFO || profileType == ProfileType.CONTACT_INFO_TIMELINE)
		{
			HikeMessengerApp.getPubSub().removeListeners(this, contactInfoPubSubListeners);
		}
		else if (profileType == ProfileType.USER_PROFILE)
		{
			HikeMessengerApp.getPubSub().removeListeners(this, profilePubSubListeners);
		}
		else if (profileType == ProfileType.USER_PROFILE_EDIT)
		{
			HikeMessengerApp.getPubSub().removeListeners(this, profilEditPubSubListeners);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_ACTION_BAR_OVERLAY);

		if (Utils.requireAuth(this))
		{
			return;
		}

		preferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		smileyParser = SmileyParser.getInstance();
		Object o = getLastCustomNonConfigurationInstance();
		if (o instanceof ActivityState)
		{
			mActivityState = (ActivityState) o;
			if (mActivityState.task != null)
			{
				/* we're currently executing a task, so show the progress dialog */
				mActivityState.task.setActivity(this);
				mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));
			}
		}
		else
		{
			mActivityState = new ActivityState();
		}

		if (getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT) || getIntent().hasExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST))
		{
			setContentView(R.layout.profile);
			this.profileType = getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT) ? ProfileType.GROUP_INFO : ProfileType.BROADCAST_INFO;
			setupGroupAndBroadcastProfileScreen();
			HikeMessengerApp.getPubSub().addListeners(this, groupInfoPubSubListeners);
		}
		else if (getIntent().hasExtra(HikeConstants.Extras.CONTACT_INFO))
		{
			setContentView(R.layout.profile);
			this.profileType = ProfileType.CONTACT_INFO;
			setupContactProfileScreen();
			HikeMessengerApp.getPubSub().addListeners(this, contactInfoPubSubListeners);
		}
		else if(getIntent().hasExtra(HikeConstants.Extras.CONTACT_INFO_TIMELINE))
		{
			setContentView(R.layout.profile);
			View parent = findViewById(R.id.parent_layout);
			parent.setBackgroundColor(getResources().getColor(R.color.standerd_background)); 
			this.profileType = ProfileType.CONTACT_INFO_TIMELINE;
			setupContactTimelineScreen();
			HikeMessengerApp.getPubSub().addListeners(this, contactInfoPubSubListeners);
		}
		else
		{
			httpRequestURL = "/account";
			fetchPersistentData();

			if(Intent.ACTION_ATTACH_DATA.equals(getIntent().getAction()))
			{
				setProfileImage(HikeConstants.GALLERY_RESULT, RESULT_OK, getIntent());				
			}
			if (getIntent().getBooleanExtra(HikeConstants.Extras.EDIT_PROFILE, false))
			{
				// set pubsub listeners
				setContentView(R.layout.profile_edit);
				this.profileType = ProfileType.USER_PROFILE_EDIT;
				setupEditScreen();
				HikeMessengerApp.getPubSub().addListeners(this, profilEditPubSubListeners);
				triggerPointPopup=ProductPopupsConstants.PopupTriggerPoints.EDIT_PROFILE.ordinal();
			}
			else
			{
				setContentView(R.layout.profile);
				View parent = findViewById(R.id.parent_layout);
				parent.setBackgroundColor(getResources().getColor(R.color.standerd_background)); //Changing background color form white for self profile
				this.profileType = ProfileType.USER_PROFILE;
				setupProfileScreen(savedInstanceState);
				HikeMessengerApp.getPubSub().addListeners(this, profilePubSubListeners);
				triggerPointPopup=ProductPopupsConstants.PopupTriggerPoints.PROFILE_PHOTO.ordinal();
			}
		}
		if (mActivityState.groupEditDialogShowing)
		{
			onEditGroupNameClick(null);
		}
		setupActionBar();
		if (getIntent().getBooleanExtra(ProductPopupsConstants.SHOW_CAMERA, false))
		{
			onHeaderButtonClicked(null);
		}
		
		if (triggerPointPopup != ProductPopupsConstants.PopupTriggerPoints.UNKNOWN.ordinal())
		{
			showProductPopup(triggerPointPopup);
		}
		
	}

	private void setGroupNameFields(View parent)
	{
		showingGroupEdit = true;
		ViewGroup parentView = (ViewGroup) parent.getParent();
		mName = (TextView) parentView.findViewById(R.id.name);
		mName.setVisibility(View.GONE);
		mNameEdit = (CustomFontEditText) parentView.findViewById(R.id.name_edit);
		mNameEdit.setVisibility(View.VISIBLE);
		mNameEdit.requestFocus();
		mNameEdit.setText(oneToNConversation.getLabel());
		mNameEdit.setSelection(mNameEdit.getText().toString().length());
		Utils.showSoftKeyboard(getApplicationContext(), mNameEdit);
		setupGroupNameEditActionBar();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);
		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		switch (profileType)
		{
		case CONTACT_INFO_TIMELINE:
			/*Falling onto contact info intentionally*/
		case CONTACT_INFO:
			title.setText(R.string.profile_title);
			break;
		case USER_PROFILE:
			title.setText(R.string.me);
			break;
		case GROUP_INFO:
			title.setText(R.string.group_info);
			break;
		case BROADCAST_INFO:
			title.setText(R.string.broadcast_info);
			break;
		case USER_PROFILE_EDIT:
			title.setText(R.string.edit_profile);
			break;
		}

		backContainer.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});
		
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_header));
		actionBar.setCustomView(actionBarView);
		invalidateOptionsMenu();
	}
	
	private void setupGroupNameEditActionBar()
	{	
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		View editGroupNameView = LayoutInflater.from(ProfileActivity.this).inflate(R.layout.chat_theme_action_bar, null);
		View okBtn = editGroupNameView.findViewById(R.id.done_container);
		ViewGroup closeContainer = (ViewGroup) editGroupNameView.findViewById(R.id.close_container);
		TextView multiSelectTitle = (TextView) editGroupNameView.findViewById(R.id.title);
		if (this.profileType == ProfileType.GROUP_INFO)
		{
			multiSelectTitle.setText(R.string.edit_group_name);  //Add String to strings.xml
		}
		else
		{
			multiSelectTitle.setText(R.string.edit_broadcast_name);  //Add String to strings.xml
		}
		okBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String groupName = mNameEdit.getText().toString();
				if (TextUtils.isEmpty(groupName.trim()))
				{
					showNameCanNotBeEmptyToast();
					return;
				}
				saveChanges();
				Utils.hideSoftKeyboard(ProfileActivity.this, mNameEdit);
				showingGroupEdit = false;
				mName.setText(groupName);
				mName.setVisibility(View.VISIBLE);
				mNameEdit.setVisibility(View.GONE);
				setupActionBar();
			}
		});

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				closeGroupNameEdit();
			}
		});
		actionBar.setCustomView(editGroupNameView);
		invalidateOptionsMenu();
	}

	public void closeGroupNameEdit()
	{
		if(showingGroupEdit)
		{
			showingGroupEdit = false;
			mActivityState.edittedGroupName = null;
			Utils.hideSoftKeyboard(ProfileActivity.this, mNameEdit);
			mName.setText(oneToNConversation.getLabel());
			mName.setVisibility(View.VISIBLE);
			mNameEdit.setVisibility(View.GONE);
			setupActionBar();
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		if (profileAdapter != null)
		{
			profileAdapter.getTimelineImageLoader().setExitTasksEarly(false);
			profileAdapter.getIconImageLoader().setExitTasksEarly(false);
			profileAdapter.getProfilePicImageLoader().setExitTasksEarly(false);
						
			if(profileAdapter.getSharedFileImageLoader()!=null)
			{
				profileAdapter.getSharedFileImageLoader().setExitTasksEarly(false);
			}
			profileAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		switch (profileType)
		{
		case CONTACT_INFO_TIMELINE:
			/*Falling onto contact info intentionally*/
		case CONTACT_INFO:
			if(HikeMessengerApp.hikeBotNamesMap.containsKey(contactInfo.getMsisdn()))
			{
				return false;  /*No need to show menu for HikeBots.*/
			}
			else
			{
				getSupportMenuInflater().inflate(R.menu.contact_profile_menu, menu);
				mMenu = menu;
				return true;
			}
		case GROUP_INFO:
			if (!showingGroupEdit)
			{
				getSupportMenuInflater().inflate(R.menu.group_profile_menu, menu);
			}
			mMenu = menu;
			return true;
		case BROADCAST_INFO:
			if (!showingGroupEdit)
			{
				getSupportMenuInflater().inflate(R.menu.broadcast_profile_menu, menu);
			}
			mMenu = menu;
			return true;
		case USER_PROFILE:
			getSupportMenuInflater().inflate(R.menu.my_profile_menu, menu);
			mMenu = menu;
			return true;
		}
		mMenu = menu;
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		switch (profileType)
		{
		case CONTACT_INFO_TIMELINE:
			/*Falling onto contact info intentionally*/
		case CONTACT_INFO:
			MenuItem friendItem = menu.findItem(R.id.unfriend);
			MenuItem overflow = menu.findItem(R.id.overflow_menu);

			if (friendItem != null)
			{
					if (contactInfo.getFavoriteType() != FavoriteType.NOT_FRIEND && contactInfo.getFavoriteType() != FavoriteType.REQUEST_RECEIVED && contactInfo.getFavoriteType() != FavoriteType.REQUEST_RECEIVED_REJECTED )
					{
						friendItem.setVisible(true);
						friendItem.setTitle(R.string.remove_from_favorites);
					}
					else
					{
						friendItem.setVisible(false);
					}
			}

			if(overflow!=null && !overflow.getSubMenu().hasVisibleItems())
			{
				overflow.setVisible(false);
			}
			return true;
		case GROUP_INFO:
			MenuItem muteItem = menu.findItem(R.id.mute_group);
			if (muteItem != null)
			{
				muteItem.setTitle(oneToNConversation.isMuted() ? R.string.unmute_group : R.string.mute_group);
			}
			return true;
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.unfriend:
			FavoriteType fav = Utils.checkAndUnfriendContact(contactInfo);
			contactInfo.setFavoriteType(fav);
			invalidateOptionsMenu();
			break;
		case R.id.edit_group_picture:
			onHeaderButtonClicked(null);
			break;
		case R.id.delete_broadcast:
		case R.id.leave_group:
			onProfileLargeBtnClick(null);
			break;
		case R.id.mute_group:
			onProfileSmallRightBtnClick(null);
			break;
		case R.id.new_update:
			onProfileLargeBtnClick(null);
			break;
		case R.id.edit:
			onEditProfileClicked(null);
			break;
		case R.id.add_recipients:
		case R.id.add_people:
			openAddToGroup();
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	private void setupContactProfileScreen()
	{
		this.mLocalMSISDN = getIntent().getStringExtra(HikeConstants.Extras.CONTACT_INFO);
		contactInfo = HikeMessengerApp.getContactManager().getContact(mLocalMSISDN, true, true);
		sharedMediaCount = HikeConversationsDatabase.getInstance().getSharedMediaCount(mLocalMSISDN, true);
		sharedPinCount = 0;  //Add a query here to get shared groups count. sharedPincount is to be treated as shared group count here.
		unreadPinCount = 0;
		sharedFileCount =  HikeConversationsDatabase.getInstance().getSharedMediaCount(mLocalMSISDN, false);
		if (!contactInfo.isOnhike())
		{
			contactInfo.setOnhike(getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, false));
		}

		initializeListviewAndAdapter();

		/*
		 * if the hike join time for a known hike contact is 0, we request the server for the hike join time.
		 */
		if (contactInfo.isOnhike() && contactInfo.getHikeJoinTime() == 0)
		{
			getHikeJoinedTimeFromServer();
		}
	}
	
	private void setupContactTimelineScreen()
	{
		this.mLocalMSISDN = getIntent().getStringExtra(HikeConstants.Extras.CONTACT_INFO_TIMELINE);
		contactInfo = HikeMessengerApp.getContactManager().getContact(mLocalMSISDN, true, true);
		if(!contactInfo.isOnhike())
		{
			contactInfo.setOnhike(getIntent().getBooleanExtra(HikeConstants.Extras.ON_HIKE, false));
		}
		
		initializeListviewAndAdapter();
		
		if(contactInfo.isOnhike() && contactInfo.getHikeJoinTime() == 0)
		{
			getHikeJoinedTimeFromServer();
		}
	}
	
	private void getHikeJoinedTimeFromServer()
	{
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(HikeConstants.REQUEST_BASE_URLS.HTTP_REQUEST_PROFILE_BASE_URL + mLocalMSISDN, RequestType.HIKE_JOIN_TIME, new HikeHttpCallback()
		{
			@Override
			public void onSuccess(JSONObject response)
			{
				Logger.d(getClass().getSimpleName(), "Response: " + response.toString());
				try
				{
					JSONObject profile = response.getJSONObject(HikeConstants.PROFILE);
					long hikeJoinTime = profile.optLong(HikeConstants.JOIN_TIME, 0);
					hikeJoinTime = Utils.applyServerTimeOffset(ProfileActivity.this, hikeJoinTime);
					HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_JOIN_TIME_OBTAINED, new Pair<String, Long>(mLocalMSISDN, hikeJoinTime));
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		});
		mActivityState.getHikeJoinTimeTask = new HikeHTTPTask(null, -1);
		Utils.executeHttpTask(mActivityState.getHikeJoinTimeTask, hikeHttpRequest);
	}
	
	private void updateProfileHeaderView()
	{
		addProfileHeaderView(true, false);
	}
	
	private void updateProfileImageInHeaderView()
	{
		addProfileHeaderView(true, true);
	}
	
	private void addProfileHeaderView()
	{
		addProfileHeaderView(false, false);
	}

	private void addProfileHeaderView(boolean isUpdate, boolean profileImageUpdated)
	{
		TextView text;
		TextView subText;
		ImageView profileImage;
		View parentView;
		TextView extraInfo;
		ImageView smallIcon;
		EditText groupNameEditText;
		ImageView smallIconFrame;
		ImageView statusMood;
		TextView dualText;
		String msisdn;
		String name;
		boolean headerViewInitialized = false;
		switch (profileType)
		{
		case CONTACT_INFO:
			if(headerView == null)
			{
				headerViewInitialized = true;
				headerView = getLayoutInflater().inflate(R.layout.profile_header_other, null);
			}
			text = (TextView) headerView.findViewById(R.id.name);
			subText = (TextView) headerView.findViewById(R.id.subtext);
			profileImage = (ImageView) headerView.findViewById(R.id.profile_image);
			parentView = headerView.findViewById(R.id.profile_header);
			extraInfo = (TextView) headerView.findViewById(R.id.add_fav_tv);
			smallIcon = (ImageView) headerView.findViewById(R.id.add_fav_star);
			statusMood = (ImageView) headerView.findViewById(R.id.status_mood);
			smallIconFrame = (ImageView) headerView.findViewById(R.id.add_fav_star_2);
			dualText = (TextView) headerView.findViewById(R.id.add_fav_tv_2);

			String infoSubText = getString(Utils.isLastSeenSetToFavorite() ? R.string.both_ls_status_update : R.string.status_updates_proper_casing);
			((TextView) headerView.findViewById(R.id.update_text)).setText(getString(R.string.add_fav_msg, infoSubText));
			msisdn = contactInfo.getMsisdn();
			name = TextUtils.isEmpty(contactInfo.getName()) ? contactInfo.getMsisdn() : contactInfo.getName();
			text.setText(name);
			LinearLayout fav_layout = (LinearLayout) parentView.findViewById(R.id.add_fav_view);
			LinearLayout req_layout = (LinearLayout) parentView.findViewById(R.id.remove_fav);
			RelativeLayout dual_layout = (RelativeLayout) parentView.findViewById(R.id.add_fav_view_2);
			fav_layout.setVisibility(View.GONE);
			req_layout.setVisibility(View.GONE);
			dual_layout.setVisibility(View.GONE);
			statusMood.setVisibility(View.GONE);
			fav_layout.setTag(null);  //Resetting the tag, incase we need to add to favorites again.
			if(!HikeMessengerApp.hikeBotNamesMap.containsKey(contactInfo.getMsisdn()))  //The HikeBot's numbers wont be shown
			{
			if (showContactsUpdates(contactInfo)) // Favourite case
			
			{
				addContactStatusInHeaderView(text, subText, statusMood);
				// Request_Received --->> Show add/not now screen.
				if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED)
				{	
					// Show add/not now screen.
					req_layout.setVisibility(View.VISIBLE);
				}
				
				else if(contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED && !contactInfo.isUnknownContact())
				{	
					fav_layout.setVisibility(View.VISIBLE);  //Simply show add to fav view if contact is unsaved
					extraInfo.setTextColor(getResources().getColor(R.color.add_fav));
					extraInfo.setText(getResources().getString(R.string.add_fav));
					smallIcon.setImageResource(R.drawable.ic_add_friend);
				}
				
				if (contactInfo.isUnknownContact())
				{		
						if(contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED)
						{
							dual_layout.setVisibility(View.VISIBLE);
						}
						else
						{
							fav_layout.setVisibility(View.VISIBLE);
							fav_layout.setTag(getResources().getString(R.string.tap_save_contact));
							extraInfo.setTextColor(getResources().getColor(R.color.blue_hike));
							extraInfo.setText(getResources().getString(R.string.tap_save_contact));
							smallIcon.setImageResource(R.drawable.ic_invite_to_hike);
						}
				}
				
			}
			else if (contactInfo.isOnhike()) 
				{
					setStatusText(getJoinedHikeStatus(contactInfo), subText, text);
					if ((contactInfo.getFavoriteType() == FavoriteType.NOT_FRIEND  || contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED))
					{
						if (contactInfo.isUnknownContact())
						{
							// Show dual layout
							dual_layout.setVisibility(View.VISIBLE);
						}
						else
						{
							dual_layout.setVisibility(View.GONE);
							fav_layout.setVisibility(View.VISIBLE);
							extraInfo.setTextColor(getResources().getColor(R.color.add_fav));
							extraInfo.setText(getResources().getString(R.string.add_fav));
							smallIcon.setImageResource(R.drawable.ic_add_friend);
						}
					}
					else if (contactInfo.getFavoriteType() == FavoriteType.REQUEST_SENT || contactInfo.getFavoriteType() == FavoriteType.REQUEST_SENT_REJECTED)
					{
						if (contactInfo.isUnknownContact()) // Tap to save
						{
							fav_layout.setVisibility(View.VISIBLE);
							fav_layout.setTag(getResources().getString(R.string.tap_save_contact));
							extraInfo.setTextColor(getResources().getColor(R.color.blue_hike));
							extraInfo.setText(getResources().getString(R.string.tap_save_contact));
							smallIcon.setImageResource(R.drawable.ic_invite_to_hike);
						}
						else
						{
							fav_layout.setTag(null);
							fav_layout.setVisibility(View.GONE);
						}
					}
			}

			else if (!contactInfo.isOnhike())
			{  	subText.setText(getResources().getString(R.string.on_sms));
				// UNKNOWN and on SMS
				if(contactInfo.isUnknownContact())
				{
					dual_layout.setVisibility(View.VISIBLE);
					dualText.setTextColor(getResources().getColor(R.color.blue_hike));
					dualText.setText(getResources().getString(R.string.ftue_add_prompt_invite_title));
					smallIconFrame.setImageResource(R.drawable.ic_invite_to_hike_small);
				}
				else
				{	dual_layout.setVisibility(View.GONE);
					fav_layout.setVisibility(View.VISIBLE);
					extraInfo.setTextColor(getResources().getColor(R.color.blue_hike));
					extraInfo.setText(getResources().getString(R.string.ftue_add_prompt_invite_title));
					smallIcon.setImageResource(R.drawable.ic_invite_to_hike);
				}
			 }
			}
			else  //Hike Bot. Don't show the status subtext bar. No need to take the user to Bot's timeline as well
			{
				subText.setVisibility(View.GONE);
				headerView.findViewById(R.id.divider_view).setVisibility(View.GONE);
				headerView.findViewById(R.id.profile_head).setEnabled(false);
			}
			
			break;
			
		case BROADCAST_INFO:
		case GROUP_INFO:
			if(headerView == null)
			{
				headerViewInitialized = true;
				headerView = getLayoutInflater().inflate(R.layout.profile_header_group, null);
			}
			groupNameEditText = (EditText) headerView.findViewById(R.id.name_edit);
			text = (TextView) headerView.findViewById(R.id.name);
			profileImage = (ImageView) headerView.findViewById(R.id.group_profile_image);
			smallIconFrame = (ImageView) headerView.findViewById(R.id.change_profile);
			groupNameEditText.setText(oneToNConversation.getLabel());
			msisdn = oneToNConversation.getMsisdn();
			name = oneToNConversation.getLabel();
			text.setText(name);
			break;
			
		default:
			return;
		}
		
		if(!isUpdate)
		{
			ImageViewerInfo imageViewerInfo = new ImageViewerInfo(msisdn + PROFILE_PIC_SUFFIX, null, false, !ContactManager.getInstance().hasIcon(msisdn));
			profileImage.setTag(imageViewerInfo);
		}
		if(headerViewInitialized || profileImageUpdated )
		{
			int mBigImageSize = getResources().getDimensionPixelSize(R.dimen.avatar_profile_size);
			(new IconLoader(this, mBigImageSize)).loadImage(msisdn, profileImage, false, false, true);
		}

		if(headerViewInitialized)
		{
			profileContent.addHeaderView(headerView);
		}
	}
	
	private void addContactStatusInHeaderView(TextView name, TextView subText, ImageView statusMood)
	{
		StatusMessageType[] statusMessagesTypesToFetch = {StatusMessageType.TEXT};
		StatusMessage status = HikeConversationsDatabase.getInstance().getLastStatusMessage(statusMessagesTypesToFetch, contactInfo);
		if(status != null)
		{
			if (status.hasMood())  //Adding mood image for status
			{
				statusMood.setVisibility(View.VISIBLE);
				statusMood.setImageResource(EmoticonConstants.moodMapping.get(status.getMoodId()));
			}
			else
			{
				statusMood.setVisibility(View.GONE);
			}
			subText.setText(smileyParser.addSmileySpans(status.getText(), true));
			return;
		}
		
		status = getJoinedHikeStatus(contactInfo);
		setStatusText(status, subText, name);
	}
	
	private void setStatusText(StatusMessage status,final TextView subText, TextView name)
	{
		if (status.getTimeStamp() == 0)
			subText.setVisibility(View.GONE);
		else
		{
			subText.setText(status.getText() + " " + status.getTimestampFormatted(true, ProfileActivity.this));
			subText.setVisibility(View.INVISIBLE);
			Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_up_hike_joined);
			name.startAnimation(animation);
			animation.setAnimationListener(new AnimationListener()
			{
				@Override
				public void onAnimationStart(Animation animation)
				{
				}

				@Override
				public void onAnimationRepeat(Animation animation)
				{
				}

				@Override
				public void onAnimationEnd(Animation animation)
				{
					subText.setVisibility(View.VISIBLE);
				}
			});
		}
	}

	private void setupContactProfileList()
	{
		profileItems.clear();
		if(!HikeMessengerApp.hikeBotNamesMap.containsKey(contactInfo.getMsisdn()))  //The HikeBot's numbers wont be shown
		profileItems.add(new ProfileItem.ProfilePhoneNumberItem(ProfileItem.PHONE_NUMBER, getResources().getString(R.string.phone_pa)));
		if(contactInfo.isOnhike())
		{	shouldAddSharedMedia();
			profileItems.add(new ProfileItem.ProfileSharedContent(ProfileItem.SHARED_CONTENT, getResources().getString(R.string.shared_cont_pa), sharedFileCount, sharedPinCount, unreadPinCount, null));
		}
	}
	
	private void setupContactTimelineList()
	{
		profileItems.clear();
		profileItems.add(new ProfileItem.ProfileStatusItem(ProfileItem.HEADER_ID));
		if(showContactsUpdates(contactInfo))
		{
			addStatusMessagesAsMyProfileItems(HikeConversationsDatabase.getInstance().getStatusMessages(false, HikeConstants.MAX_STATUSES_TO_LOAD_INITIALLY, -1, mLocalMSISDN));
		}
		
		if(contactInfo.isOnhike() && contactInfo.getHikeJoinTime() > 0)
		{
			profileItems.add(new ProfileItem.ProfileStatusItem(getJoinedHikeStatus(contactInfo)));
		}
	}

	private void shouldAddSharedMedia()
	{
		// TODO Auto-generated method stub
		
		sharedMediaItem = new ProfileItem.ProfileSharedMedia(ProfileItem.SHARED_MEDIA, sharedMediaCount, maxMediaToShow);
		if(sharedMediaCount>0)
		{	
			addSharedMedia();
		}
		profileItems.add(sharedMediaItem);
	}

	private void addStatusMessagesAsMyProfileItems(List<StatusMessage> statusMessages)
	{
		for (StatusMessage statusMessage : statusMessages)
		{
			profileItems.add(new ProfileItem.ProfileStatusItem(statusMessage));
		}
	}

	private boolean showContactsUpdates(ContactInfo contactInfo)
	{
		return (contactInfo.getFavoriteType() != FavoriteType.NOT_FRIEND) && (contactInfo.getFavoriteType() != FavoriteType.REQUEST_SENT)
				&& (contactInfo.getFavoriteType() != FavoriteType.REQUEST_SENT_REJECTED) && (contactInfo.isOnhike());
	}

	private void setupGroupAndBroadcastProfileScreen()
	{
		HikeConversationsDatabase hCDB = HikeConversationsDatabase.getInstance();

		if (this.profileType == ProfileType.BROADCAST_INFO)
		{
			this.mLocalMSISDN = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST);
			oneToNConversation = (BroadcastConversation) hCDB.getConversation(mLocalMSISDN, 0, true);
			sharedMediaCount = hCDB.getSharedMediaCount(mLocalMSISDN,true);
			sharedPinCount = 0;
		}
		else if (this.profileType == ProfileType.GROUP_INFO)
		{
			this.mLocalMSISDN = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);
			oneToNConversation = (GroupConversation) hCDB.getConversation(mLocalMSISDN, 0, true);
			sharedMediaCount = hCDB.getSharedMediaCount(mLocalMSISDN,true);
			sharedPinCount = hCDB.getPinCount(mLocalMSISDN);
		}

		try 
		{
			if (!(oneToNConversation instanceof BroadcastConversation))
			{
				unreadPinCount = oneToNConversation.getMetadata().getUnreadPinCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN);			
			}
		}
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
		sharedFileCount = hCDB.getSharedMediaCount(mLocalMSISDN, false);
		participantMap = oneToNConversation.getConversationParticipantList();

		httpRequestURL = "/group/" + oneToNConversation.getMsisdn();

		initializeListviewAndAdapter();
		
		if(unreadPinCount > 0)
		{
			currUnreadCount = unreadPinCount;
			
			sharedContentItem.setPinAnimation(true);
		}

		profileContent.setDivider(null);

		nameTxt = oneToNConversation.getLabel();
	}

	private void initializeListviewAndAdapter()
	{
		profileContent = (ListView) findViewById(R.id.profile_content);
		headerView = null;
		int sizeOfImage = calculateDimens();
		switch (profileType)
		{
		case CONTACT_INFO:
			profileItems = new ArrayList<ProfileItem>();
			setupContactProfileList();
			profileAdapter = new ProfileAdapter(this, profileItems, null, contactInfo, false, ContactManager.getInstance().isBlocked(mLocalMSISDN), sizeOfImage);
			addProfileHeaderView();
			break;
		case BROADCAST_INFO:
		case GROUP_INFO:
			profileItems = new ArrayList<ProfileItem>();
			setupGroupProfileList();
			profileAdapter = new ProfileAdapter(this, profileItems, oneToNConversation, null, false, false, sizeOfImage);
			addProfileHeaderView();
			break;
		case USER_PROFILE:
			profileAdapter = new ProfileAdapter(this, profileItems, null, contactInfo, true);
			profileContent.setOnItemLongClickListener(this);
			profileContent.setOnScrollListener(this);
			break;
			
		case CONTACT_INFO_TIMELINE:
			profileItems = new ArrayList<ProfileItem>();
			setupContactTimelineList();
			profileAdapter = new ProfileAdapter(this, profileItems, null, contactInfo, false, ContactManager.getInstance().isBlocked(mLocalMSISDN));
			profileContent.setOnScrollListener(this);
			break;
		default:
			break;
		}
		profileContent.setAdapter(profileAdapter);
	}

	private int calculateDimens()
	{
		// TODO Auto-generated method stub
		int sizeOfImage = getResources().getDimensionPixelSize(R.dimen.profile_shared_media_item_size);
		int screenWidth = getResources().getDisplayMetrics().widthPixels - getResources().getDimensionPixelSize(R.dimen.sm_leftmargin) - getResources().getDimensionPixelSize(R.dimen.sm_rightmargin);
		int numColumns = screenWidth/sizeOfImage;
		int remainder = screenWidth - (numColumns * getResources().getDimensionPixelSize(R.dimen.thumbnail_margin_right)) - numColumns * sizeOfImage;
		maxMediaToShow = numColumns;
		return sizeOfImage + (remainder/numColumns);
	}

	private void setupGroupProfileList()
	{
		GroupParticipant userInfo = new GroupParticipant(Utils.getUserContactInfo(preferences, true));

		profileItems.clear();
		shouldAddSharedMedia();
		sharedContentItem = new ProfileItem.ProfileSharedContent(ProfileItem.SHARED_CONTENT,getResources().getString(R.string.shared_cont_pa), sharedFileCount, sharedPinCount, unreadPinCount, null);
		profileItems.add(sharedContentItem);
		
		List<PairModified<GroupParticipant, String>> participants = new ArrayList<PairModified<GroupParticipant, String>>();

		for (Entry<String, PairModified<GroupParticipant, String>> mapEntry : participantMap.entrySet())
		{
			participants.add(mapEntry.getValue());
		}
		
		if (this.profileType == ProfileType.GROUP_INFO)
		{
			if (!participantMap.containsKey(userInfo.getContactInfo().getMsisdn()))
			{
				participants.add(new PairModified<GroupParticipant, String>(userInfo, null));
			}
		}

		profileItems.add(new ProfileItem.ProfileGroupItem(ProfileItem.MEMBERS, participants.size()));		//Adding group member count
		Collections.sort(participants, GroupParticipant.lastSeenTimeComparator);

		for (int i = 0; i < participants.size(); i++)
		{
			profileItems.add(new ProfileItem.ProfileGroupItem(ProfileItem.GROUP_MEMBER, participants.get(i)));
		}
		isGroupOwner = userInfo.getContactInfo().getMsisdn().equals(oneToNConversation.getConversationOwner());
		//Add -> Add member tab
		profileItems.add(new ProfileItem.ProfileGroupItem(ProfileItem.ADD_MEMBERS, null));
		
	}

	private void addSharedMedia()
	{
		// TODO Auto-generated method stub
		
		HikeConversationsDatabase hCDB = HikeConversationsDatabase.getInstance();
		if(sharedMediaCount < maxMediaToShow )
			sharedMediaItem.addSharedMediaFiles((List<HikeSharedFile>) hCDB.getSharedMedia(mLocalMSISDN, sharedMediaCount, -1, true));

		else
			sharedMediaItem.addSharedMediaFiles((List<HikeSharedFile>) hCDB.getSharedMedia(mLocalMSISDN, maxMediaToShow * MULTIPLIER , -1, true));
	}

	private void setupEditScreen()
	{
		ViewGroup name = (ViewGroup) findViewById(R.id.name);
		ViewGroup phone = (ViewGroup) findViewById(R.id.phone);
		ViewGroup email = (ViewGroup) findViewById(R.id.email);
		ViewGroup gender = (ViewGroup) findViewById(R.id.gender);
		ViewGroup picture = (ViewGroup) findViewById(R.id.photo);

		mNameEdit = (EditText) name.findViewById(R.id.name_input);
		mEmailEdit = (EditText) email.findViewById(R.id.email_input);

		((TextView) name.findViewById(R.id.name_edit_field)).setText(R.string.name);
		((TextView) phone.findViewById(R.id.phone_edit_field)).setText(R.string.phone_num);
		((TextView) email.findViewById(R.id.email_edit_field)).setText(R.string.email);
		((TextView) gender.findViewById(R.id.gender_edit_field)).setText(R.string.gender);
		((TextView) picture.findViewById(R.id.photo_edit_field)).setText(R.string.edit_picture);

		picture.setBackgroundResource(R.drawable.profile_bottom_item_selector);
		picture.setFocusable(true);

		((EditText) phone.findViewById(R.id.phone_input)).setText(mLocalMSISDN);
		((EditText) phone.findViewById(R.id.phone_input)).setEnabled(false);

		// Make sure that the name text does not exceed the permitted length
		int maxLength = getResources().getInteger(R.integer.max_length_name);
		if (nameTxt.length() > maxLength)
		{
			nameTxt = new String(nameTxt.substring(0, maxLength));
		}

		mNameEdit.setText(nameTxt);
		mEmailEdit.setText(emailTxt);

		mNameEdit.setSelection(nameTxt.length());
		mEmailEdit.setSelection(emailTxt.length());

		onEmoticonClick(mActivityState.genderType == 0 ? null : mActivityState.genderType == 1 ? gender.findViewById(R.id.guy) : gender.findViewById(R.id.girl));

		// Hide the cursor initially
		Utils.hideCursor(mNameEdit, getResources());
	}

	private void setupProfileScreen(Bundle savedInstanceState)
	{
		contactInfo = Utils.getUserContactInfo(preferences);

		profileItems = new ArrayList<ProfileItem>();

		// Adding an item for the header
		profileItems.add(new ProfileItem.ProfileStatusItem(ProfileItem.HEADER_ID));

		addStatusMessagesAsMyProfileItems(HikeConversationsDatabase.getInstance().getStatusMessages(false, HikeConstants.MAX_STATUSES_TO_LOAD_INITIALLY, -1, mLocalMSISDN));

		if (contactInfo.isOnhike() && contactInfo.getHikeJoinTime() > 0)
		{
			profileItems.add(new ProfileItem.ProfileStatusItem(getJoinedHikeStatus(contactInfo)));
		}

		initializeListviewAndAdapter();
		if (contactInfo.isOnhike() && contactInfo.getHikeJoinTime() == 0)
		{
			HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/account/profile/" + mLocalMSISDN, RequestType.HIKE_JOIN_TIME, new HikeHttpCallback()
			{
				@Override
				public void onSuccess(JSONObject response)
				{
					Logger.d(getClass().getSimpleName(), "Response: " + response.toString());
					try
					{
						JSONObject profile = response.getJSONObject(HikeConstants.PROFILE);
						long hikeJoinTime = profile.optLong(HikeConstants.JOIN_TIME, 0);

						Editor editor = preferences.edit();
						editor.putLong(HikeMessengerApp.USER_JOIN_TIME, hikeJoinTime);
						editor.commit();

						HikeMessengerApp.getPubSub().publish(HikePubSub.USER_JOIN_TIME_OBTAINED, new Pair<String, Long>(mLocalMSISDN, hikeJoinTime));
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
				}
			});
			mActivityState.getHikeJoinTimeTask = new HikeHTTPTask(null, -1);
			Utils.executeHttpTask(mActivityState.getHikeJoinTimeTask, hikeHttpRequest);
		}
	}

	boolean reachedEnd;

	boolean loadingMoreMessages;

	private int previousFirstVisibleItem;

	private int velocity;

	private long previousEventTime;

	@Override
	public void onScroll(AbsListView view, final int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (previousFirstVisibleItem != firstVisibleItem)
		{
			long currTime = System.currentTimeMillis();
			long timeToScrollOneElement = currTime - previousEventTime;
			velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

			previousFirstVisibleItem = firstVisibleItem;
			previousEventTime = currTime;
		}

		if ((this.profileType == ProfileType.USER_PROFILE || profileType == ProfileType.CONTACT_INFO_TIMELINE) &&  !reachedEnd && !loadingMoreMessages && !profileItems.isEmpty()
				&& (firstVisibleItem + visibleItemCount) >= (profileItems.size() - HikeConstants.MIN_INDEX_TO_LOAD_MORE_MESSAGES))
		{
			Logger.d(getClass().getSimpleName(), "Loading more items");
			loadingMoreMessages = true;

			AsyncTask<Void, Void, List<StatusMessage>> asyncTask = new AsyncTask<Void, Void, List<StatusMessage>>()
			{
				private boolean isLastMessageJoinedHike = false;

				@Override
				protected List<StatusMessage> doInBackground(Void... params)
				{
					StatusMessage statusMessage = ((ProfileStatusItem) profileItems.get(profileItems.size() - 1)).getStatusMessage();
					
					if (statusMessage != null && statusMessage.getId() == HikeConstants.JOINED_HIKE_STATUS_ID)
					{	
						try
							{
								statusMessage = ((ProfileStatusItem) profileItems.get(profileItems.size() - 2)).getStatusMessage();
								isLastMessageJoinedHike = true;
							}
					
						catch(ClassCastException e)
							{	
								e.printStackTrace();
							}
					}

					if (statusMessage == null)
					{
						return new ArrayList<StatusMessage>();
					}
					List<StatusMessage> olderMessages = HikeConversationsDatabase.getInstance().getStatusMessages(true, HikeConstants.MAX_OLDER_STATUSES_TO_LOAD_EACH_TIME,
							(int) statusMessage.getId(), mLocalMSISDN);
					if (!olderMessages.isEmpty() && isLastMessageJoinedHike)
					{
						olderMessages.add(getJoinedHikeStatus(contactInfo));
					}
					return olderMessages;
				}

				@Override
				protected void onPostExecute(List<StatusMessage> olderMessages)
				{
					if (!olderMessages.isEmpty())
					{
						int scrollOffset = profileContent.getChildAt(0).getTop();

						if (isLastMessageJoinedHike)
						{
							profileItems.remove(profileItems.size() - 1);
						}
						
						addStatusMessagesAsMyProfileItems(olderMessages);
						profileAdapter.notifyDataSetChanged();
						profileContent.setSelectionFromTop(firstVisibleItem, scrollOffset);
					}
					else
					{
						reachedEnd = true;
					}

					loadingMoreMessages = false;
				}

			};
			if (Utils.isHoneycombOrHigher())
			{
				asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			else
			{
				asyncTask.execute();
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		if (profileAdapter != null)
		{
			Logger.d(getClass().getSimpleName(), "CentralTimeline Adapter Scrolled State: " + scrollState);
			profileAdapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_TIMELINE_IMAGES && scrollState == OnScrollListener.SCROLL_STATE_FLING);
		}
		/*
		 * // Pause fetcher to ensure smoother scrolling when flinging if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) { // Before Honeycomb pause image loading
		 * on scroll to help with performance if (!Utils.hasHoneycomb()) { if (profileAdapter != null) { profileAdapter.getTimelineImageLoader().setPauseWork(true);
		 * profileAdapter.getIconImageLoader().setPauseWork(true); } } } else { if (profileAdapter != null) { profileAdapter.getTimelineImageLoader().setPauseWork(false);
		 * profileAdapter.getIconImageLoader().setPauseWork(false); } }
		 */
	}

	private void fetchPersistentData()
	{
		nameTxt = preferences.getString(HikeMessengerApp.NAME, "Set a name!");
		mLocalMSISDN = preferences.getString(HikeMessengerApp.MSISDN_SETTING, null);
		emailTxt = preferences.getString(HikeConstants.Extras.EMAIL, "");
		lastSavedGender = preferences.getInt(HikeConstants.Extras.GENDER, 0);
		mActivityState.genderType = mActivityState.genderType == 0 ? lastSavedGender : mActivityState.genderType;
	}

	public void onBackPressed()
	{
		if(showingGroupEdit)
		{
			closeGroupNameEdit();
			return;
		}
		if(removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG))
		{
			if(mNameEdit!=null && mName!=null)
				{
					mNameEdit.setVisibility(View.GONE);
					mName.setVisibility(View.VISIBLE);
				}
			return;
		}
		
		if (this.profileType == ProfileType.USER_PROFILE_EDIT)
		{
			isBackPressed = true;
			saveChanges();
		}
		else
		{
			super.onBackPressed();
		}
	}

	public void saveChanges()
	{
		ArrayList<HikeHttpRequest> requests = new ArrayList<HikeHttpRequest>();

		if ((this.profileType == ProfileType.USER_PROFILE_EDIT) && !TextUtils.isEmpty(mEmailEdit.getText()))
		{
			if (!Utils.isValidEmail(mEmailEdit.getText()))
			{
				Toast.makeText(this, getResources().getString(R.string.invalid_email), Toast.LENGTH_LONG).show();
				return;
			}
		}

		if (mNameEdit != null)
		{
			final String newName = mNameEdit.getText().toString().trim();
			if (!TextUtils.isEmpty(newName) && !nameTxt.equals(newName))
			{
				/* user edited the text, so update the profile */
				HikeHttpRequest request = new HikeHttpRequest(httpRequestURL + "/name", RequestType.OTHER, new HikeHttpRequest.HikeHttpCallback()
				{
					public void onFailure()
					{
						if (isBackPressed)
						{
							HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
						}
					}

					public void onSuccess(JSONObject response)
					{
						if (ProfileActivity.this.profileType != ProfileType.GROUP_INFO && ProfileActivity.this.profileType != ProfileType.BROADCAST_INFO)
						{
							/*
							 * if the request was successful, update the shared preferences and the UI
							 */
							String name = newName;
							Editor editor = preferences.edit();
							editor.putString(HikeMessengerApp.NAME_SETTING, name);
							editor.commit();
							HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_NAME_CHANGED, null);
						}
						if (isBackPressed)
						{
							HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
						}
					}
				});

				JSONObject json = new JSONObject();
				try
				{
					json.put("name", newName);
					request.setJSONData(json);
				}
				catch (JSONException e)
				{
					Logger.e("ProfileActivity", "Could not set name", e);
				}
				requests.add(request);
			}
		}
		
		if (mActivityState.destFilePath != null)
		{
			/* the server only needs a smaller version */
			final Bitmap smallerBitmap = HikeBitmapFactory.scaleDownBitmap(mActivityState.destFilePath, HikeConstants.PROFILE_IMAGE_DIMENSIONS,
					HikeConstants.PROFILE_IMAGE_DIMENSIONS, Bitmap.Config.RGB_565, true, false);

			if (smallerBitmap == null)
			{
				failureWhileSettingProfilePic();
				return;
			}

			final byte[] bytes = BitmapUtils.bitmapToBytes(smallerBitmap, Bitmap.CompressFormat.JPEG, 100);

			if (profileAdapter != null)
			{
				profileAdapter.setProfilePreview(smallerBitmap);
			}

			HikeHttpRequest request = new HikeHttpRequest(httpRequestURL + "/avatar", RequestType.PROFILE_PIC, new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{
					Logger.d("ProfileActivity", "resetting image");
					failureWhileSettingProfilePic();
				}

				public void onSuccess(JSONObject response)
				{
					mActivityState.destFilePath = null;
					ContactManager.getInstance().setIcon(mLocalMSISDN, bytes, false);

					Utils.renameTempProfileImage(mLocalMSISDN);

					if (profileAdapter != null)
					{
						profileAdapter.setProfilePreview(null);
					}

					if (profileType == ProfileType.USER_PROFILE || profileType == ProfileType.USER_PROFILE_EDIT)
					{

						// HikeMessengerApp.getLruCache().clearIconForMSISDN(mLocalMSISDN);

						/*
						 * Making the profile pic change a status message.
						 */
						JSONObject data = response.optJSONObject("status");

						if (data == null)
						{
							return;
						}

						String mappedId = data.optString(HikeConstants.STATUS_ID);
						String msisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");
						String name = preferences.getString(HikeMessengerApp.NAME_SETTING, "");
						long time = (long) System.currentTimeMillis() / 1000;

						StatusMessage statusMessage = new StatusMessage(0, mappedId, msisdn, name, "", StatusMessageType.PROFILE_PIC, time, -1, 0);
						HikeConversationsDatabase.getInstance().addStatusMessage(statusMessage, true);

						ContactManager.getInstance().setIcon(statusMessage.getMappedId(), bytes, false);

						String srcFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + msisdn + ".jpg";

						String destFilePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT + "/" + mappedId + ".jpg";

						/*
						 * Making a status update file so we don't need to download this file again.
						 */
						Utils.copyFile(srcFilePath, destFilePath, null);

						int unseenUserStatusCount = preferences.getInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0);
						Editor editor = preferences.edit();
						editor.putInt(HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, ++unseenUserStatusCount);
						editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
						editor.commit();
						/*
						 * This would happen in the case where the user has added a self contact and received an mqtt message before saving this to the db.
						 */

						if (statusMessage.getId() != -1)
						{
							HikeMessengerApp.getPubSub().publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
							HikeMessengerApp.getPubSub().publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
						}
					}

					HikeMessengerApp.getLruCache().clearIconForMSISDN(mLocalMSISDN);
					HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, mLocalMSISDN);

					if (isBackPressed)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
					}
				}
			});

			request.setFilePath(mActivityState.destFilePath);
			requests.add(request);
		}

		if ((this.profileType == ProfileType.USER_PROFILE_EDIT) && ((!emailTxt.equals(mEmailEdit.getText().toString())) || ((mActivityState.genderType != lastSavedGender))))
		{
			HikeHttpRequest request = new HikeHttpRequest(httpRequestURL + "/profile", RequestType.OTHER, new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{
					if (isBackPressed)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
					}
				}

				public void onSuccess(JSONObject response)
				{
					Editor editor = preferences.edit();
					if (Utils.isValidEmail(mEmailEdit.getText()))
					{
						editor.putString(HikeConstants.Extras.EMAIL, mEmailEdit.getText().toString());
					}
					editor.putInt(HikeConstants.Extras.GENDER, currentSelection != null ? (currentSelection.getId() == R.id.guy ? 1 : 2) : 0);
					editor.commit();
					if (isBackPressed)
					{
						// finishEditing();
						HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
					}
				}
			});
			JSONObject obj = new JSONObject();
			try
			{
				Logger.d(getClass().getSimpleName(), "Profile details Email: " + mEmailEdit.getText() + " Gender: " + mActivityState.genderType);
				if (!emailTxt.equals(mEmailEdit.getText().toString()))
				{
					obj.put(HikeConstants.EMAIL, mEmailEdit.getText());
				}
				if (mActivityState.genderType != lastSavedGender)
				{
					obj.put(HikeConstants.GENDER, mActivityState.genderType == 1 ? "m" : mActivityState.genderType == 2 ? "f" : "");
				}
				Logger.d(getClass().getSimpleName(), "JSON to be sent is: " + obj.toString());
				request.setJSONData(obj);
			}
			catch (JSONException e)
			{
				Logger.e("ProfileActivity", "Could not set email or gender", e);
			}
			requests.add(request);
		}

		if (!requests.isEmpty())
		{
			mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));
			mActivityState.task = new HikeHTTPTask(this, R.string.update_profile_failed);
			HikeHttpRequest[] r = new HikeHttpRequest[requests.size()];
			requests.toArray(r);
			Utils.executeHttpTask(mActivityState.task, r);
		}
		else if (isBackPressed)
		{
			finishEditing();
		}
	}

	private void failureWhileSettingProfilePic()
	{
		Utils.removeTempProfileImage(mLocalMSISDN);
		mActivityState.destFilePath = null;
		if (profileAdapter != null)
		{
			/*
			 * Reload the older image
			 */
			profileAdapter.setProfilePreview(null);
			profileAdapter.notifyDataSetChanged();
		}
		if (isBackPressed)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_UPDATE_FINISH, null);
		}
	}

	private void finishEditing()
	{
		if (this.profileType != ProfileType.GROUP_INFO && this.profileType != ProfileType.USER_PROFILE)
		{
			Intent i = new Intent(this, ProfileActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
		}
		else if (this.profileType == ProfileType.USER_PROFILE)
		{
			super.onBackPressed();
			return;
		}
		super.onBackPressed();
	}

	protected String getLargerIconId()
	{
		return mLocalMSISDN + "::large";
	}

	@Override
	public void onFinish(boolean success)
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}

		mActivityState.task = null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		setProfileImage(requestCode, resultCode, data);
	}

	protected void setProfileImage(int requestCode, int resultCode, Intent data)
	{
		String path = null;
		if (resultCode != RESULT_OK)
		{
			return;
		}

		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		/*
		 * Making sure the directory exists before setting a profile image
		 */
		File dir = new File(directory);
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		String fileName = Utils.getTempProfileImageFileName(mLocalMSISDN);
		final String destFilePath = directory + "/" + fileName;

		switch (requestCode)
		{
			case HikeConstants.CAMERA_RESULT:
				Logger.d("ProfileActivity", "The activity is " + this);
				String filePath = preferences.getString(HikeMessengerApp.FILE_PATH, "");
				selectedFileIcon = new File(filePath);

				/*
				 * Removing this key. We no longer need this.
				 */
				Editor editor = preferences.edit();
				editor.remove(HikeMessengerApp.FILE_PATH);
				editor.commit();
				if (!selectedFileIcon.exists())
				{
					Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
					return;
				}
				path = selectedFileIcon.getAbsolutePath();
				if (TextUtils.isEmpty(path))
				{
					Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
					return;
				}
				Utils.startCropActivity(this, path, destFilePath);
				break;

			case HikeConstants.GALLERY_RESULT:
				Logger.d("ProfileActivity", "The activity is " + this);
				boolean isPicasaImage = false;
				Uri selectedFileUri = null;
				if (data == null || data.getData() == null)
				{
					Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
					return;
				}
				selectedFileUri = data.getData();
				if (Utils.isPicasaUri(selectedFileUri.toString()))
				{
					isPicasaImage = true;
					path = Utils.getOutputMediaFile(HikeFileType.PROFILE, null, false).getAbsolutePath();
				}
				else
				{
					String fileUriStart = "file://";
					String fileUriString = selectedFileUri.toString();
					if (fileUriString.startsWith(fileUriStart))
					{
						selectedFileIcon = new File(URI.create(Utils.replaceUrlSpaces(fileUriString)));
						/*
						 * Done to fix the issue in a few Sony devices.
						 */
						path = selectedFileIcon.getAbsolutePath();
					}
					else
					{
						path = Utils.getRealPathFromUri(selectedFileUri, this);
					}
				}
				if (TextUtils.isEmpty(path))
				{
					Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
					return;
				}
				if (!isPicasaImage)
				{
					Utils.startCropActivity(this, path, destFilePath);
				}
				else
				{
					final File destFile = new File(path);
					mActivityState.downloadPicasaImageTask = new DownloadImageTask(getApplicationContext(), destFile, selectedFileUri, new ImageDownloadResult()
					{

						@Override
						public void downloadFinished(boolean result)
						{
							if (mDialog != null)
							{
								mDialog.dismiss();
								mDialog = null;
							}
							mActivityState.downloadPicasaImageTask = null;
							if (!result)
							{
								Toast.makeText(getApplicationContext(), R.string.error_download, Toast.LENGTH_SHORT).show();
							}
							else
							{
								Utils.startCropActivity(ProfileActivity.this, destFile.getAbsolutePath(), destFilePath);
							}
						}
					});
					Utils.executeBoolResultAsyncTask(mActivityState.downloadPicasaImageTask);
					mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.downloading_image));
				}
				try
				{
					JSONObject metadata = new JSONObject();
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SET_PROFILE_PIC_GALLERY);
					HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
				}
				catch(JSONException e)
				{
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
				}
				break;

			case HikeConstants.CROP_RESULT:
				mActivityState.destFilePath = data.getStringExtra(MediaStore.EXTRA_OUTPUT);
				if (mActivityState.destFilePath == null)
				{
					Toast.makeText(getApplicationContext(), R.string.error_setting_profile, Toast.LENGTH_SHORT).show();
					return;
				}
				if ((this.profileType == ProfileType.USER_PROFILE) || (this.profileType == ProfileType.GROUP_INFO))
				{
					Utils.compressAndCopyImage(mActivityState.destFilePath, mActivityState.destFilePath, ProfileActivity.this, ImageQuality.QUALITY_MEDIUM);
					saveChanges();
				}
				break;
		}
	}

	public void onEmoticonClick(View v)
	{
		if (v != null)
		{
			if (currentSelection != null)
			{
				currentSelection.setSelected(false);
			}
			v.setSelected(currentSelection != v);
			currentSelection = v == currentSelection ? null : v;
			if (currentSelection != null)
			{
				mActivityState.genderType = currentSelection.getId() == R.id.guy ? 1 : 2;
				return;
			}
			mActivityState.genderType = 0;
		}
	}

	public void onViewImageClicked(View v)
	{
		if(Utils.isBot(mLocalMSISDN))
		{
			return;
		}
		
		ImageViewerInfo imageViewerInfo = (ImageViewerInfo) v.getTag();

		String mappedId = imageViewerInfo.mappedId;
		String url = imageViewerInfo.url;

		Bundle arguments = new Bundle();
		arguments.putString(HikeConstants.Extras.MAPPED_ID, mappedId);
		arguments.putString(HikeConstants.Extras.URL, url);
		arguments.putBoolean(HikeConstants.Extras.IS_STATUS_IMAGE, imageViewerInfo.isStatusMessage);

		HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_IMAGE, arguments);
	}

	public void onYesBtnClick(View v)
	{
		respondToFriendRequest(contactInfo, true);
	}

	public void onNoBtnClick(View v)
	{
		respondToFriendRequest(contactInfo, false);
	}

	private void respondToFriendRequest(ContactInfo contactInfo, boolean accepted)
	{
		FavoriteType favoriteType = accepted ? FavoriteType.FRIEND : FavoriteType.REQUEST_RECEIVED_REJECTED;
		contactInfo.setFavoriteType(favoriteType);
		Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, ContactInfo.FavoriteType>(contactInfo, favoriteType);
		HikeMessengerApp.getPubSub().publish(accepted ? HikePubSub.FAVORITE_TOGGLED : HikePubSub.REJECT_FRIEND_REQUEST, favoriteToggle);
		int count = preferences.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0);
		if(count > 0)
		{
			Utils.incrementOrDecrementFriendRequestCount(preferences, -1);
		}
	}

	public void onTextButtonClick(View v)
	{
		if(v.getTag()!=null &&      
				((String) v.getTag()).equals(getResources().getString(R.string.tap_save_contact))) //Only in this case, the the view will have a tag else tag will be null
		{
			onAddToContactClicked(v);
			return;
		}
		
		if (contactInfo.isOnhike())
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.ADD_TO_FAVOURITE);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			Utils.addFavorite(this, contactInfo, false);
		}
		else
		{
			inviteToHike(contactInfo);
		}
	}

	public void onHeaderButtonClicked(View v)
	{
		if (profileType == ProfileType.USER_PROFILE || profileType == ProfileType.GROUP_INFO)
		{
			/*
			 * The wants to change their profile picture. Open a dialog to allow them pick Camera or Gallery
			 */
			final CharSequence[] items = getResources().getStringArray(R.array.profile_pic_dialog);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.choose_picture);
			builder.setItems(items, this);
			mDialog = builder.show();
		}
		else if (profileType == ProfileType.CONTACT_INFO_TIMELINE)
		{
			openChatThread(contactInfo);
		}
	}

	public void onEditProfileClicked(View v)
	{
		Utils.logEvent(ProfileActivity.this, HikeConstants.LogEvent.EDIT_PROFILE);
		Intent i = new Intent(ProfileActivity.this, ProfileActivity.class);
		i.putExtra(HikeConstants.Extras.EDIT_PROFILE, true);
		startActivity(i);
		finish();
	}

	public void onProfileLargeBtnClick(View v)
	{
		AlertDialog.Builder builder;
		AlertDialog alertDialog;
		switch (profileType)
		{
		case BROADCAST_INFO:
		case GROUP_INFO:
			final boolean isBroadcast = profileType == ProfileType.BROADCAST_INFO;
			
			builder = new AlertDialog.Builder(this);
			builder.setMessage(isBroadcast ? R.string.delete_broadcast_confirm : R.string.leave_group_confirm);
			builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					HikePubSub hikePubSub = HikeMessengerApp.getPubSub();
					HikeMqttManagerNew.getInstance().sendMessage(oneToNConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE), HikeMqttManagerNew.MQTT_QOS_ONE);
					hikePubSub.publish(HikePubSub.GROUP_LEFT, oneToNConversation.getMsisdn());
					Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
					intent.putExtra(HikeConstants.Extras.GROUP_LEFT, mLocalMSISDN);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
				}
			});
			builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
				}
			});
			builder.setCancelable(true);
			alertDialog = builder.create();
			alertDialog.show();
			break;
		case CONTACT_INFO:
			openChatThread(contactInfo);
			break;
		case USER_PROFILE:
			startActivity(new Intent(this, StatusUpdate.class));
			break;
		}
	}

	private void openChatThread(ContactInfo contactInfo)
	{
		Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(this, contactInfo, true);
		//Add anything else which is need to the intent
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if (getIntent().getBooleanExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, false))
		{
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
		}
		startActivity(intent);
	}

	public void onProfileSmallLeftBtnClick(View v)
	{
		Utils.logEvent(ProfileActivity.this, HikeConstants.LogEvent.ADD_PARTICIPANT);
		Intent intent = IntentFactory.createChatThreadIntentFromMsisdn(ProfileActivity.this, mLocalMSISDN, false);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

	}

	public void onProfileSmallRightBtnClick(View v)
	{
		oneToNConversation.setIsMute(!oneToNConversation.isMuted());

		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED, new Pair<String, Boolean>(oneToNConversation.getMsisdn(), oneToNConversation.isMuted()));
		invalidateOptionsMenu();
	}

	public void onProfileBtn1Click(View v)
	{
		if (profileAdapter.isContactBlocked())
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, mLocalMSISDN);
			profileAdapter.setIsContactBlocked(false);
			profileAdapter.notifyDataSetChanged();
		}
		else
		{
			if (contactInfo.isOnhike())
			{
				contactInfo.setFavoriteType(FavoriteType.REQUEST_SENT);

				Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(contactInfo, contactInfo.getFavoriteType());
				HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED, favoriteToggle);
			}
			else
			{
				inviteToHike(contactInfo);
			}
		}
	}

	public void onProfileBtn2Click(View v)
	{
		contactInfo.setFavoriteType(FavoriteType.REQUEST_SENT);

		Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(contactInfo, contactInfo.getFavoriteType());
		HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED, favoriteToggle);
	}

	public void onEditGroupNameClick(View v)
	{
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(mNameEdit, InputMethodManager.SHOW_IMPLICIT);

		mActivityState.groupEditDialogShowing = true;

		groupEditDialog = new Dialog(this, R.style.Theme_CustomDialog_Keyboard);
		groupEditDialog.setContentView(R.layout.group_name_change_dialog);

		TextView header = (TextView) groupEditDialog.findViewById(R.id.header);

		if (this.profileType == ProfileType.BROADCAST_INFO)
		{
			header.setText(R.string.edit_broadcast_name);
		}
		else
		{
			header.setText(R.string.edit_group_name);
		}
		mNameEdit = (EditText) groupEditDialog.findViewById(R.id.group_name_edit);
		mNameEdit.setText(TextUtils.isEmpty(mActivityState.edittedGroupName) ? oneToNConversation.getLabel() : mActivityState.edittedGroupName);
		mNameEdit.setSelection(mNameEdit.length());

		Button okBtn = (Button) groupEditDialog.findViewById(R.id.btn_ok);
		Button cancelBtn = (Button) groupEditDialog.findViewById(R.id.btn_cancel);

		cancelBtn.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				groupEditDialog.dismiss();
				mActivityState.groupEditDialogShowing = false;
				mActivityState.edittedGroupName = null;
			}
		});

		okBtn.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				String groupName = mNameEdit.getText().toString();
				if (TextUtils.isEmpty(groupName.trim()))
				{
					showNameCanNotBeEmptyToast();
				}
				Utils.hideSoftKeyboard(ProfileActivity.this, mNameEdit);
				saveChanges();
				mActivityState.groupEditDialogShowing = false;
				groupEditDialog.cancel();
				groupEditDialog.dismiss();
			}
		});

		groupEditDialog.show();
	}
	
	private void showNameCanNotBeEmptyToast()
	{
		int toastStringResId = R.string.enter_valid_group_name; 
		if(profileType == ProfileType.BROADCAST_INFO)
		{
			toastStringResId = R.string.enter_valid_broadcast_name;	
		}
		Toast toast = Toast.makeText(ProfileActivity.this, toastStringResId, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
		return;
	}

	public void onGroupNameEditClick(View v)
	{
		View parent = (View) v.getParent();
		setGroupNameFields(parent);
	}
	
	public void onBlockGroupOwnerClicked(View v)
	{
		Button blockBtn = (Button) v;
		HikeMessengerApp.getPubSub().publish(isBlocked ? HikePubSub.UNBLOCK_USER : HikePubSub.BLOCK_USER, this.groupOwner);
		isBlocked = !isBlocked;
		blockBtn.setText(!isBlocked ? R.string.block_owner : R.string.unblock_owner);
	}

	public void onAddToContactClicked(View v)
	{
		if (profileType != ProfileType.CONTACT_INFO && profileType != ProfileType.CONTACT_INFO_TIMELINE)
		{
			return;
		}
		if (!contactInfo.getMsisdn().equals(contactInfo.getId()))
		{
			return;
		}
		Utils.logEvent(this, HikeConstants.LogEvent.MENU_ADD_TO_CONTACTS);
		Utils.addToContacts(this, mLocalMSISDN);
	}

	public void onInviteToHikeClicked(View v)
	{
		inviteToHike(contactInfo);
	}

	private void inviteToHike(ContactInfo contactInfo)
	{
		Utils.sendInviteUtil(contactInfo, this, HikeConstants.SINGLE_INVITE_SMS_ALERT_CHECKED, getString(R.string.native_header), getString(R.string.native_info));

	}

	public void onBlockUserClicked(View v)
	{
		Button blockBtn = (Button) v;
		HikeMessengerApp.getPubSub().publish(isBlocked ? HikePubSub.UNBLOCK_USER : HikePubSub.BLOCK_USER, this.mLocalMSISDN);
		isBlocked = !isBlocked;
		blockBtn.setText(!isBlocked ? R.string.block_user : R.string.unblock_user);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onEventReceived(final String type, Object object)
	{
		super.onEventReceived(type, object);

		if (mLocalMSISDN == null)
		{
			Logger.w(getClass().getSimpleName(), "The msisdn is null, we are doing something wrong.." + object);
			return;
		}
		if (HikePubSub.GROUP_NAME_CHANGED.equals(type))
		{
			if (mLocalMSISDN.equals((String) object))
			{
				nameTxt = ContactManager.getInstance().getName(mLocalMSISDN);
				oneToNConversation.setConversationName(nameTxt);
				
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						updateProfileHeaderView();
						//profileAdapter.updateGroupConversation(groupConversation);
					}
				});
			}
		}
		else if (HikePubSub.ICON_CHANGED.equals(type))
		{
			if (mLocalMSISDN.equals((String) object))
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if(profileType == ProfileType.CONTACT_INFO || profileType == ProfileType.GROUP_INFO)
						{
							updateProfileImageInHeaderView();
						}
						else
						{
							profileAdapter.updateHasCustomPhoto();
							profileAdapter.notifyDataSetChanged();
						}
					}
				});
			}
		}

		if (HikePubSub.PARTICIPANT_LEFT_ONETONCONV.equals(type))
		{
			if (mLocalMSISDN.equals(((JSONObject) object).optString(HikeConstants.TO)))
			{
				String msisdn = ((JSONObject) object).optString(HikeConstants.DATA);
				this.participantMap.remove(msisdn);

				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						setupGroupProfileList();
						updateProfileHeaderView();
						profileAdapter.notifyDataSetChanged();
					}
				});
			}
		}
		else if (HikePubSub.PARTICIPANT_JOINED_ONETONCONV.equals(type))
		{
			if (mLocalMSISDN.equals(((JSONObject) object).optString(HikeConstants.TO)))
			{
				final JSONObject obj = (JSONObject) object;
				final JSONArray participants = obj.optJSONArray(HikeConstants.DATA);
				List<String> msisdns = new ArrayList<String>();
				
				for (int i = 0; i < participants.length(); i++)
				{
					String msisdn = participants.optJSONObject(i).optString(HikeConstants.MSISDN);
					String contactName = participants.optJSONObject(i).optString(HikeConstants.NAME);
					boolean onHike = participants.optJSONObject(i).optBoolean(HikeConstants.ON_HIKE);
					boolean onDnd = participants.optJSONObject(i).optBoolean(HikeConstants.DND);
					GroupParticipant groupParticipant = new GroupParticipant(new ContactInfo(msisdn, msisdn, contactName, msisdn, onHike), false, onDnd);
					participantMap.put(msisdn, new PairModified<GroupParticipant, String>(groupParticipant, contactName));
					msisdns.add(msisdn);
				}

				if (msisdns.size() > 0)
				{
					List<ContactInfo> contacts = HikeMessengerApp.getContactManager().getContact(msisdns, true, true);
					for (ContactInfo contactInfo : contacts)
					{
						GroupParticipant grpParticipant = participantMap.get(contactInfo.getMsisdn()).getFirst();
						ContactInfo con = grpParticipant.getContactInfo();
						contactInfo.setOnhike(con.isOnhike());
						grpParticipant.setContactInfo(contactInfo);
					}
				}
				
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						setupGroupProfileList();
						updateProfileHeaderView();
						profileAdapter.notifyDataSetChanged();
					}
				});
			}
		}
		else if (HikePubSub.GROUP_END.equals(type))
		{
			mLocalMSISDN.equals(((JSONObject) object).optString(HikeConstants.TO));
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						ProfileActivity.this.finish();
					}
				});
			}
		}
		else if (HikePubSub.CONTACT_ADDED.equals(type) || HikePubSub.CONTACT_DELETED.equals(type))
		{
			final ContactInfo contact = (ContactInfo) object;
			if (contact == null)
			{
				return;
			}
			if(profileType == ProfileType.GROUP_INFO)
			{
				if(participantMap.containsKey(contact.getMsisdn()))
				{
					PairModified<GroupParticipant, String> groupParticipantPair = participantMap.get(contact.getMsisdn());
					groupParticipantPair.getFirst().setContactInfo(contact);
				}
				else
				{
					return;
				}
			}

			else if (profileType == ProfileType.CONTACT_INFO || profileType == ProfileType.CONTACT_INFO_TIMELINE)
			{
				if (!this.mLocalMSISDN.equals(contact.getMsisdn()))
				{
					return;
				}
				this.contactInfo = contact;
			}
			
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if(profileType == ProfileType.CONTACT_INFO)
					{
						updateProfileImageInHeaderView();
					}
					else if(profileType == ProfileType.GROUP_INFO || profileType == ProfileType.BROADCAST_INFO)
					{
						profileAdapter.updateGroupConversation(oneToNConversation);
					}
					else if(profileType == ProfileType.CONTACT_INFO_TIMELINE)
					{
						//setupContactTimelineList() ?
						profileAdapter.updateContactInfo(contactInfo);  
					}
				}
			});
		}
		else if (HikePubSub.USER_JOINED.equals(type) || HikePubSub.USER_LEFT.equals(type))
		{
			String msisdn = (String) object;
			if (!mLocalMSISDN.equals(msisdn) && profileType != ProfileType.GROUP_INFO)
			{
				return;
			}
			else if (profileType == ProfileType.GROUP_INFO || profileType == ProfileType.BROADCAST_INFO)
			{
				PairModified<GroupParticipant, String> groupParticipantPair = oneToNConversation.getConversationParticipant(msisdn);
				GroupParticipant groupParticipant = null;
				if (groupParticipant == null)
				{
					return;
				}
				groupParticipant.getContactInfo().setOnhike(HikePubSub.USER_JOINED.equals(type));
			}

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (profileType == ProfileType.GROUP_INFO || profileType == ProfileType.BROADCAST_INFO)
					{
						setupGroupProfileList();
					}
					else   
					{
						setupContactProfileList();
					}
					updateProfileHeaderView();
					profileAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.STATUS_MESSAGE_RECEIVED.equals(type))
		{
			final StatusMessage statusMessage = (StatusMessage) object;
			if (!mLocalMSISDN.equals(statusMessage.getMsisdn()) || statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST_ACCEPTED
					|| statusMessage.getStatusMessageType() == StatusMessageType.USER_ACCEPTED_FRIEND_REQUEST)
			{
				return;
			}
			
			if((profileType == ProfileType.CONTACT_INFO || profileType == ProfileType.CONTACT_INFO_TIMELINE) && !showContactsUpdates(contactInfo))
				return;
			
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if(profileType == ProfileType.CONTACT_INFO)
 					{	
						updateProfileHeaderView();
 					}
					else if (profileType == ProfileType.USER_PROFILE || profileType == ProfileType.CONTACT_INFO_TIMELINE)
					{
						profileItems.add(1, new ProfileItem.ProfileStatusItem(statusMessage));
						profileAdapter.notifyDataSetChanged();
					}
				}
			});
		}
		else if (HikePubSub.FAVORITE_TOGGLED.equals(type) || HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type) || HikePubSub.REJECT_FRIEND_REQUEST.equals(type))
		{
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			ContactInfo contactInfo = favoriteToggle.first;
			FavoriteType favoriteType = favoriteToggle.second;

			if (!mLocalMSISDN.equals(contactInfo.getMsisdn()))
			{
				return;
			}
			if(favoriteType == FavoriteType.REQUEST_SENT_REJECTED)
			{
				return;
			}
			this.contactInfo.setFavoriteType(favoriteType);
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{	invalidateOptionsMenu();
					if(profileType == ProfileType.CONTACT_INFO)
					{
						updateProfileHeaderView();
					}
					else if(profileType == ProfileType.CONTACT_INFO_TIMELINE)
					{
						setupContactTimelineList();
						profileAdapter.notifyDataSetChanged();
					}
				}
			});
		}
		else if (HikePubSub.HIKE_JOIN_TIME_OBTAINED.equals(type) || HikePubSub.USER_JOIN_TIME_OBTAINED.equals(type))
		{
			Pair<String, Long> msisdnHikeJoinTimePair = (Pair<String, Long>) object;

			String msisdn = msisdnHikeJoinTimePair.first;
			long hikeJoinTime = msisdnHikeJoinTimePair.second;

			if (!msisdn.equals(mLocalMSISDN))
			{
				return;
			}

			contactInfo.setHikeJoinTime(hikeJoinTime);
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if(profileType == ProfileType.CONTACT_INFO)
					{
						updateProfileHeaderView();
					}
					else if(profileType == ProfileType.CONTACT_INFO_TIMELINE)
					{
						profileItems.add(new ProfileItem.ProfileStatusItem(getJoinedHikeStatus(contactInfo)));
						profileAdapter.notifyDataSetChanged();
					}
				}
			});
		}
		else if (HikePubSub.LARGER_IMAGE_DOWNLOADED.equals(type))
		{
			// TODO: find a more specific way to trigger this.
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					profileAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.PROFILE_IMAGE_DOWNLOADED.equals(type))
		{
			if (mLocalMSISDN.equals((String) object))
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						profileAdapter.notifyDataSetChanged();
					}
				});
			}
		}
		else if (HikePubSub.PROFILE_UPDATE_FINISH.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					finishEditing();
				}
			});
		}
		else if (HikePubSub.DELETE_MESSAGE.equals(type))
		{
			Pair<ArrayList<Long>, Bundle> deleteMessage = (Pair<ArrayList<Long>, Bundle>) object;
			Bundle bundle = deleteMessage.second;
			String msisdn = bundle.getString(HikeConstants.Extras.MSISDN);
			/*
			 * if message type is not set return;
			 */
			if(!bundle.containsKey(HikeConstants.Extras.DELETED_MESSAGE_TYPE))
			{
				return;
			}
			final int deletedMessageType = bundle.getInt(HikeConstants.Extras.DELETED_MESSAGE_TYPE);
			
			final ArrayList<Long> msgIds = deleteMessage.first;
			
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if(deletedMessageType == HikeConstants.SHARED_MEDIA_TYPE)
					{
						Iterator<HikeSharedFile> it = sharedMediaItem.getSharedFilesList().iterator();
						while (it.hasNext())
						{
							HikeSharedFile file = it.next();
							if (msgIds.contains(file.getMsgId()))
							{
								it.remove();
							}
						}
						sharedMediaCount -= msgIds.size();
						sharedMediaItem.setSharedMediaCount(sharedMediaCount);
						if (sharedMediaCount == 0)
						{
							sharedMediaItem.clearMediaList();
						}

						if (sharedMediaItem.getSharedFilesList() != null && sharedMediaItem.getSharedFilesList().size() < maxMediaToShow
								&& sharedMediaCount != sharedMediaItem.getSharedFilesList().size()) // If somehow all the elements which were laoded initially are deleted, we need
																									// to
																									// fetch more stuff from db.
						{
							addSharedMedia();
						}
					}
					else if(HikeConstants.SHARED_PIN_TYPE == deletedMessageType)
					{
						sharedPinCount -= msgIds.size();
						sharedContentItem.setSharedPinsCount(sharedPinCount);
					}

					profileAdapter.notifyDataSetChanged();
				}
			});
		}
		else if(HikePubSub.UNREAD_PIN_COUNT_RESET.equals(type))
		{
			if(oneToNConversation.getMsisdn().equals(((Conversation)object).getMsisdn()))
			{
				sharedContentItem.setUnreadPinCount(0);	
				
				currUnreadCount = 0;
				
				runOnUiThread(new Runnable() 
				{					
					@Override
					public void run() 
					{
						profileAdapter.notifyDataSetChanged();	
					}
				});
			}
		}
		else if(HikePubSub.MESSAGE_RECEIVED.equals(type))
		{
			if (oneToNConversation != null)
			{
				if(oneToNConversation.getMsisdn().equals(((ConvMessage)object).getMsisdn()))
				{							
					if(((ConvMessage)object).getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
					{
						sharedContentItem.setUnreadPinCount(++currUnreadCount);
		
						sharedContentItem.setSharedPinsCount(sharedContentItem.getSharedPinsCount() + 1);
						
						sharedPinCount += 1;
						
						if(sharedContentItem.getPinAnimation() == false)
						{
							sharedContentItem.setPinAnimation(true);
						}
						
						runOnUiThread(new Runnable() 
						{					
							@Override
							public void run() 
							{
								profileAdapter.notifyDataSetChanged();
							}
						});
					}
				}
			}
		}
		else if(HikePubSub.BULK_MESSAGE_RECEIVED.equals(type))
		{			
			boolean isUnreadCountChanged = false;
			
			HashMap<String, LinkedList<ConvMessage>> messageListMap = (HashMap<String, LinkedList<ConvMessage>>) object;
			final LinkedList<ConvMessage> messageList = messageListMap.get(mLocalMSISDN);

			if(messageList != null)
			{										
				for (final ConvMessage message : messageList)
				{
					if(message.getMsisdn().equals(oneToNConversation.getMsisdn()))
					{
						if(message.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
						{
							currUnreadCount++;
							isUnreadCountChanged = true;
						}
					}
				}					
			}
			
			if(isUnreadCountChanged)
			{
				sharedContentItem.setUnreadPinCount(currUnreadCount);
				
				sharedContentItem.setSharedPinsCount(sharedContentItem.getSharedPinsCount() + 1);
				
				if(sharedContentItem.getPinAnimation() == false)
				{
					sharedContentItem.setPinAnimation(true);
				}
				
				runOnUiThread(new Runnable()
				{					
					@Override
					public void run() 
					{
						profileAdapter.notifyDataSetChanged();
					}
				});
			}
		}
		else if (HikePubSub.ClOSE_PHOTO_VIEWER_FRAGMENT.equals(type))
		{

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG);
				}
			});
		}
	}

	private StatusMessage getJoinedHikeStatus(ContactInfo contactInfo)
	{
		return new StatusMessage(HikeConstants.JOINED_HIKE_STATUS_ID, null, contactInfo.getMsisdn(), contactInfo.getName(),
				getString(R.string.joined_hike_update), StatusMessageType.JOINED_HIKE, contactInfo.getHikeJoinTime());
	}

	@Override
	public boolean onLongClick(View view)
	{
		if (profileType == ProfileType.USER_PROFILE)
		{
			StatusMessage statusMessage = (StatusMessage) view.getTag();
			return statusMessageContextMenu(statusMessage);
		}
		else if (profileType == ProfileType.GROUP_INFO || profileType == ProfileType.BROADCAST_INFO)
		{
			boolean isBroadcast  = profileType == ProfileType.BROADCAST_INFO;
			GroupParticipant groupParticipant = (GroupParticipant) view.getTag();

			ArrayList<String> optionsList = new ArrayList<String>();

			ContactInfo tempContactInfo = null;

			if (groupParticipant == null)
			{
				return false;
			}

			String myMsisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");

			tempContactInfo = groupParticipant.getContactInfo();
			if (myMsisdn.equals(tempContactInfo.getMsisdn()))
			{
				return false;
			}

			final ContactInfo contactInfo = tempContactInfo;

			if (tempContactInfo.isUnknownContact())
			{
				optionsList.add(getString(R.string.add_to_contacts));
			}
			optionsList.add(getString(R.string.send_message));
			if(Utils.isVoipActivated(this) && (tempContactInfo!=null && tempContactInfo.isOnhike()) && !HikeMessengerApp.hikeBotNamesMap.containsKey(tempContactInfo.getMsisdn()))
			{
				optionsList.add(getString(R.string.make_call));
			}
			if (!tempContactInfo.isOnhike())
			{
				optionsList.add(getString(R.string.invite_to_hike));
			}
			if (isGroupOwner)
			{
				if (isBroadcast)
				{
					if(oneToNConversation.getParticipantListSize() > 1)
					{
						optionsList.add(getString(R.string.remove_from_broadcast));
					}
				}
				else
				{
					optionsList.add(getString(R.string.remove_from_group));
				}
			}

			final String[] options = new String[optionsList.size()];
			optionsList.toArray(options);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this, R.layout.alert_item, R.id.item, options);

			builder.setAdapter(dialogAdapter, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					String option = options[which];
					if (getString(R.string.send_message).equals(option))
					{
						openChatThread(contactInfo);
					}
					else if (getString(R.string.make_call).equals(option))
					{
						Utils.onCallClicked(getApplicationContext(), contactInfo.getMsisdn(), VoIPUtils.CallSource.PROFILE_ACTIVITY);
					}
					else if (getString(R.string.invite_to_hike).equals(option))
					{
						inviteToHike(contactInfo);
					}
					else if (getString(R.string.add_to_contacts).equals(option))
					{
						Utils.addToContacts(ProfileActivity.this, contactInfo.getMsisdn());
					}
					else if (getString(R.string.remove_from_group).equals(option))
					{
						removeFromGroup(contactInfo);
					}
					else if (getString(R.string.remove_from_broadcast).equals(option))
					{
						removeFromGroup(contactInfo);
					}
				}
			});

			AlertDialog alertDialog = builder.show();
			alertDialog.getListView().setDivider(getResources().getDrawable(R.drawable.ic_thread_divider_profile));
			return true;
		}
		return false;
	}

	private void removeFromGroup(final ContactInfo contactInfo)
	{
		HikeDialogFactory.showDialog(this, HikeDialogFactory.DELETE_FROM_GROUP, new HikeDialogListener()
		{	
			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				JSONObject object = new JSONObject();
				try
				{
					object.put(HikeConstants.TO, oneToNConversation.getMsisdn());
					object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.GROUP_CHAT_KICK);

					JSONObject data = new JSONObject();

					JSONArray msisdns = new JSONArray();
					msisdns.put(contactInfo.getMsisdn());

					data.put(HikeConstants.MSISDNS, msisdns);
					data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()));

					object.put(HikeConstants.DATA, data);
				}
				catch (JSONException e)
				{
					Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
				}
				
				HikeMqttManagerNew.getInstance().sendMessage(object, HikeMqttManagerNew.MQTT_QOS_ONE);
				hikeDialog.dismiss();
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
		}, contactInfo.getFirstName());	
		
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
	{

		StatusMessage tempStatusMessage = null;

		ProfileItem profileItem = profileAdapter.getItem(position);
		tempStatusMessage = ((ProfileStatusItem) profileItem).getStatusMessage();

		if (tempStatusMessage == null
				|| (tempStatusMessage.getStatusMessageType() != StatusMessageType.TEXT && tempStatusMessage.getStatusMessageType() != StatusMessageType.PROFILE_PIC))
		{
			return false;
		}

		return statusMessageContextMenu(tempStatusMessage);
	}

	private boolean statusMessageContextMenu(final StatusMessage statusMessage)
	{
		ArrayList<String> optionsList = new ArrayList<String>();
		optionsList.add(getString(R.string.delete_status));

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this, R.layout.alert_item, R.id.item, options);

		builder.setAdapter(dialogAdapter, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				showDeleteStatusConfirmationDialog(statusMessage.getMappedId());
			}
		});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(getResources().getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
	}

	private void showDeleteStatusConfirmationDialog(final String statusId)
	{
		HikeDialogFactory.showDialog(this, HikeDialogFactory.DELETE_STATUS_DIALOG, new HikeDialogListener()
		{
			
			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				deleteStatus(statusId);
				hikeDialog.dismiss();
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
		});
	}

	private void deleteStatus(final String statusId)
	{
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/user/status/" + statusId, RequestType.DELETE_STATUS, new HikeHttpCallback()
		{

			@Override
			public void onSuccess(JSONObject response)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_STATUS, statusId);
				for (int i = 0; i < profileItems.size(); i++)
				{
					ProfileItem profileItem = profileAdapter.getItem(i);
					StatusMessage message = ((ProfileStatusItem) profileItem).getStatusMessage();

					if (message == null)
					{
						continue;
					}

					if (statusId.equals(message.getMappedId()))
					{
						profileItems.remove(i);
						break;
					}
				}
				profileAdapter.notifyDataSetChanged();
			}

		});
		mActivityState.task = new HikeHTTPTask(this, R.string.delete_status_error);
		Utils.executeHttpTask(mActivityState.task, hikeHttpRequest);
		mDialog = ProgressDialog.show(this, null, getString(R.string.deleting_status));
	}

	public void onAddGroupMemberClicked(View v)
	{
		openAddToGroup();
	}
	
	@Override
	public void onClick(View v)
	{
		if(showingGroupEdit)
		{
			return;
		}
		
		//switch (profileType)
		if(v.getTag() instanceof HikeSharedFile)
		{	HikeSharedFile hikeFile = (HikeSharedFile) v.getTag();
			Bundle arguments = new Bundle();
			ArrayList<HikeSharedFile> hsf = new ArrayList<HikeSharedFile>();
			hsf.add(hikeFile);
			arguments.putParcelableArrayList(HikeConstants.Extras.SHARED_FILE_ITEMS, hsf);
			arguments.putInt(HikeConstants.MEDIA_POSITION, hsf.size()-1);
			arguments.putBoolean(HikeConstants.FROM_CHAT_THREAD, true);
			arguments.putString(HikeConstants.Extras.MSISDN, mLocalMSISDN);
			
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.MEDIA_THUMBNAIL_VIA_PROFILE);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			if(this.profileType == ProfileType.GROUP_INFO || this.profileType == ProfileType.BROADCAST_INFO)
				PhotoViewerFragment.openPhoto(R.id.parent_layout, ProfileActivity.this, hsf, true, oneToNConversation);
			else
				PhotoViewerFragment.openPhoto(R.id.parent_layout, ProfileActivity.this, hsf, true, 0, hsf.get(0).getMsisdn(), contactInfo.getFirstNameAndSurname());
			
			return;
		}
		else if(v.getTag() instanceof String)  //Open entire gallery intent
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.OPEN_GALLERY_VIA_PROFILE);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			if(this.profileType == ProfileType.GROUP_INFO || this.profileType == ProfileType.BROADCAST_INFO)
				startActivity(HikeSharedFilesActivity.getHikeSharedFilesActivityIntent(ProfileActivity.this, oneToNConversation));
			else
				startActivity(HikeSharedFilesActivity.getHikeSharedFilesActivityIntent(ProfileActivity.this, contactInfo.getNameOrMsisdn(), contactInfo.getMsisdn()));
			return;
		}
		
		//Group Participant was clicked
		
		GroupParticipant groupParticipant = (GroupParticipant) v.getTag();
		
		if (groupParticipant == null)
		{
			openAddToGroup();  //Add to member bottom
		}
		else if(groupParticipant!=null)
		{	
			ContactInfo contactInfo = groupParticipant.getContactInfo();

			if (HikeMessengerApp.isStealthMsisdn(contactInfo.getMsisdn()))
			{
				int stealthMode = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
				if (stealthMode != HikeConstants.STEALTH_ON)
				{
					return;
				}
			}

			String myMsisdn = preferences.getString(HikeMessengerApp.MSISDN_SETTING, "");

			Intent intent = new Intent(this, ProfileActivity.class);

			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			if (myMsisdn.equals(contactInfo.getMsisdn()))
			{
				startActivity(intent);
				return;
			}

			intent.setClass(this, ProfileActivity.class);
			intent.putExtra(HikeConstants.Extras.CONTACT_INFO, contactInfo.getMsisdn());
			intent.putExtra(HikeConstants.Extras.ON_HIKE, contactInfo.isOnhike());
			startActivity(intent);
		}
	}

	private void openAddToGroup()
	{
		// TODO Auto-generated method stub
		Intent intent = new Intent(ProfileActivity.this, ComposeChatActivity.class);
		if (this.profileType == ProfileType.GROUP_INFO)
		{
			intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
			intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, mLocalMSISDN);
		}
		else if (this.profileType == ProfileType.BROADCAST_INFO)
		{
			intent.putExtra(HikeConstants.Extras.BROADCAST_LIST, true);
			intent.putExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST, mLocalMSISDN);
			intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, HikeConstants.Extras.CREATE_BROADCAST_MODE);
		}
		startActivity(intent);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (Build.VERSION.SDK_INT <= 10 || (Build.VERSION.SDK_INT >= 14 && ViewConfiguration.get(this).hasPermanentMenuKey()))
		{
			if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU)
			{
				/*
				 * For some reason the activity randomly catches this event in the background and we get an NPE when that happens with mMenu. Adding an NPE guard for that.
				 * if media viewer is open don't do anything
				 */
				if (mMenu == null  || isFragmentAdded(HikeConstants.IMAGE_FRAGMENT_TAG))
				{
					return super.onKeyUp(keyCode, event);
				}
				mMenu.performIdentifierAction(R.id.overflow_menu, 0);
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	
	public void openPinHistory(View v)
	{
		if(showingGroupEdit)
		{
			return;
		}
		
		if(oneToNConversation!=null)
		{
			if (sharedPinCount == 0)
			{
				Toast.makeText(ProfileActivity.this, getResources().getString(R.string.pinHistoryTutorialText), Toast.LENGTH_SHORT).show();
			}
			else
			{
				Intent intent = new Intent();
				intent.setClass(ProfileActivity.this, PinHistoryActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.putExtra(HikeConstants.TEXT_PINS, mLocalMSISDN);
				startActivity(intent);
				return;
			}
		}
	}
	
	public void onSharedFilesClick(View v)
	{
		if(showingGroupEdit)
		{
			return;
		}
		if (sharedFileCount == 0)
		{
			Toast.makeText(ProfileActivity.this, R.string.no_file_profile, Toast.LENGTH_SHORT).show();
		}
		else
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SHARED_FILES_VIA_PROFILE);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			Intent intent = new Intent(this, SharedOtherFilesActivity.class);
			intent.putExtra(HikeConstants.Extras.MSISDN, mLocalMSISDN);
			startActivity(intent);
		}
	}
	
	public void messageBtnClicked(View v)
	{
		openChatThread(contactInfo);
	}
	
	public void callBtnClicked(View v)
	{
		Utils.onCallClicked(this, mLocalMSISDN, VoIPUtils.CallSource.PROFILE_ACTIVITY);
	}
	
	@Override
	public boolean removeFragment(String tag)
	{
		// TODO Auto-generated method stub
		boolean isRemoved = super.removeFragment(tag);
		if(isRemoved)
		{
			getSupportActionBar().show();
			setupActionBar();
		}
		return isRemoved;
	}
	
	public void openTimeline(View v)
	{
		Intent intent = new Intent();
		intent.setClass(ProfileActivity.this, ProfileActivity.class);
		intent.putExtra(HikeConstants.Extras.CONTACT_INFO_TIMELINE, mLocalMSISDN);
		intent.putExtra(HikeConstants.Extras.ON_HIKE, contactInfo.isOnhike());
		startActivity(intent);
	}
}
