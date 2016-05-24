package com.bsb.hike.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Data;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
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
import com.bsb.hike.analytics.ChatAnalyticConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.bots.BotInfo;
import com.bsb.hike.bots.BotUtils;
import com.bsb.hike.chatthread.ChatThreadActivity;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FTMessageBuilder;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.media.PickContactParser;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.GalleryItem;
import com.bsb.hike.models.HikeFeatureInfo;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.MultipleConvMessage;
import com.bsb.hike.models.PhonebookContact;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.offline.OfflineController;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.platform.ContentLove;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformMessageMetadata;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.tasks.ConvertToJsonArrayTask;
import com.bsb.hike.tasks.InitiateMultiFileTransferTask;
import com.bsb.hike.tasks.MultipleStatusUpdateTask;
import com.bsb.hike.tasks.StatusUpdateTask;
import com.bsb.hike.timeline.view.TimelineActivity;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.LastSeenScheduler;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.NUXManager;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.ParcelableSparseArray;
import com.bsb.hike.utils.ShareUtils;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.TagEditText;
import com.bsb.hike.view.TagEditText.Tag;
import com.bsb.hike.view.TagEditText.TagEditorListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComposeChatActivity extends HikeAppStateBaseFragmentActivity implements TagEditorListener, OnItemClickListener, HikePubSub.Listener, OnScrollListener,ConvertToJsonArrayTask.ConvertToJsonArrayCallback
{
	private static final String SELECT_ALL_MSISDN="all";
	
	private final String HORIZONTAL_FRIEND_FRAGMENT = "horizontalFriendFragment";

	private final String IMAGES_TO_SHARE = "imgToShare";

	private final String MSG_TO_SHARE = "msgToShare";
	
	private static int MIN_MEMBERS_GROUP_CHAT = 2;

	private static int MIN_MEMBERS_BROADCAST_LIST = 2;

	private static final int CREATE_GROUP_MODE = 1;

	private static final int START_CHAT_MODE = 2;
	
	private static final int MULTIPLE_FWD = 3;

    private static final int NUX_INCENTIVE_MODE = 6;
    
    private static final int CREATE_BROADCAST_MODE = 7;
    
    public static final int PICK_CONTACT_MODE = 8;

	public static final int PICK_CONTACT_AND_SEND_MODE = 9;
	
	public static final int HIKE_DIRECT_MODE = 10;

    public static final int PICK_CONTACT_SINGLE_MODE = 11;
    
    public static final int PAYMENT_MODE = 12;

	private View multiSelectActionBar, groupChatActionBar;

	private TagEditText tagEditText;

	private int composeMode;

	private ComposeChatAdapter adapter;

	int originalAdapterLength = 0;

	private TextView multiSelectTitle;

	private ListView listView;

	private TextView title;

	private boolean createGroup;

	private boolean addToConference;

	private boolean createBroadcast;
	
	private boolean isForwardingMessage;

	private boolean isSharingFile;

	private String existingGroupOrBroadcastId;

	private volatile InitiateMultiFileTransferTask fileTransferTask;
	private PreFileTransferAsycntask prefileTransferTask;

	private ProgressDialog progressDialog;
	
	protected static final int FILE_TRANSFER = 0;
	
	protected static final int CONTACT_TRANSFER = 1;
	

	private LastSeenScheduler lastSeenScheduler;

	private String[] hikePubSubListeners = { HikePubSub.MULTI_FILE_TASK_FINISHED, HikePubSub.APP_FOREGROUNDED, HikePubSub.LAST_SEEN_TIME_UPDATED,
			HikePubSub.LAST_SEEN_TIME_BULK_UPDATED, HikePubSub.CONTACT_SYNC_STARTED, HikePubSub.CONTACT_SYNCED, HikePubSub.BOT_CREATED };

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
	
	private String oneToNConvName = null;
	
	private String oneToNConvId = null;
	
	private static final int OPEN_CREATE_BROADCAST_ACTIVITY = 412;

	private int triggerPointForPopup=ProductPopupsConstants.PopupTriggerPoints.UNKNOWN.ordinal();

	private HorizontalFriendsFragment newFragment;
	
	int type = HikeConstants.Extras.NOT_SHAREABLE;
	
	private Menu mainMenu;
	
	private MenuItem searchMenuItem;

	private int gcSettings = -1;
	
	private boolean thumbnailsRequired= false;
	
	private boolean hasMicroappShowcaseIntent = false;

	private ArrayList<String> imagesToShare = new ArrayList<String>();

	private ArrayList<String> imageCaptions = new ArrayList<String>()
	{
		@Override
		public String get(int index) {

			if(isEmpty())
			{
				return null;
			}

			if(size() <= index)
			{
				return null;
			}

			return super.get(index);
		}
	};

	private boolean allImages;

	// null incase of multiple msg objects
	private String messageToShare;

    private boolean isContactChooserFilter;

	private String titleText;

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

		if (getIntent().hasExtra(HikeConstants.Extras.GROUP_CREATE_BUNDLE))
		{
			Bundle bundle = getIntent().getBundleExtra(HikeConstants.Extras.GROUP_CREATE_BUNDLE);
			createGroup = bundle.getBoolean(HikeConstants.Extras.CREATE_GROUP);
		}
		
		if (getIntent().hasExtra(HikeConstants.Extras.ADD_TO_CONFERENCE)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			addToConference = true;
		}

		isForwardingMessage = getIntent().getBooleanExtra(HikeConstants.Extras.FORWARD_MESSAGE, false);
		isSharingFile = getIntent().getType() != null;
		nuxIncentiveMode = getIntent().getBooleanExtra(HikeConstants.Extras.NUX_INCENTIVE_MODE, false);
		createBroadcast = getIntent().getBooleanExtra(HikeConstants.Extras.CREATE_BROADCAST, false);
		thumbnailsRequired = getIntent().getBooleanExtra(HikeConstants.Extras.THUMBNAILS_REQUIRED, false);
		hasMicroappShowcaseIntent = getIntent().getBooleanExtra(HikeConstants.Extras.IS_MICROAPP_SHOWCASE_INTENT, false);

		// Getting the group id. This will be a valid value if the intent
		// was passed to add group participants.
		if (getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT))
		{
			if(getIntent().hasExtra(HikeConstants.Extras.CREATE_GROUP_SETTINGS)){
				gcSettings = getIntent().getIntExtra(HikeConstants.Extras.CREATE_GROUP_SETTINGS, -1);
			}
			existingGroupOrBroadcastId = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);
		}
		else if (getIntent().hasExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST))
		{
			existingGroupOrBroadcastId = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST);
		}

		if (getIntent().hasExtra(HikeConstants.Extras.GROUP_CREATE_BUNDLE))
		{
			Bundle bundle = getIntent().getBundleExtra(HikeConstants.Extras.GROUP_CREATE_BUNDLE);
			oneToNConvName = bundle.getString(HikeConstants.Extras.ONETON_CONVERSATION_NAME);
			oneToNConvId = bundle.getString(HikeConstants.Extras.CONVERSATION_ID);
			gcSettings = bundle.getInt(HikeConstants.Extras.CREATE_GROUP_SETTINGS);
		}

        if (getIntent().hasExtra(HikeConstants.Extras.IS_CONTACT_CHOOSER_FILTER_INTENT))
        {
            isContactChooserFilter = getIntent().getBooleanExtra(HikeConstants.Extras.IS_CONTACT_CHOOSER_FILTER_INTENT,false);
            composeMode = PICK_CONTACT_SINGLE_MODE;
        }
        if (getIntent().hasExtra(HikeConstants.Extras.TITLE))
        {
            titleText = getIntent().getStringExtra(HikeConstants.Extras.TITLE);
        }

        if (savedInstanceState != null)
		{
			deviceDetailsSent = savedInstanceState.getBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT);
			imagesToShare = savedInstanceState.getStringArrayList(IMAGES_TO_SHARE);
			messageToShare = savedInstanceState.getString(MSG_TO_SHARE);

			if(!Utils.isEmpty(imagesToShare) || !TextUtils.isEmpty(messageToShare))
			{
				allImages = true;
			}
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
		
		//Make sure we are not launching share intent if our activity is restarted by OS
		if ((Intent.ACTION_SEND.equals(getIntent().getAction()) || Intent.ACTION_SENDTO.equals(getIntent().getAction())
				|| Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction())))
		{
			if (savedInstanceState == null && Intent.ACTION_SEND.equals(getIntent().getAction()) )
			{
				// If any app wants to share text descriptions (shopclues/youtube/etc) extract that message
				messageToShare = IntentFactory.getTextFromActionSendIntent(getIntent());

				// First check if an image is present in the intent, if yes, send to editor with any/all subtext as prefilled caption
				if(getIntent().getParcelableExtra(Intent.EXTRA_STREAM) != null)
				{
					String filePath = Utils.getAbsolutePathFromUri((Uri) getIntent().getParcelableExtra(Intent.EXTRA_STREAM), getApplicationContext(), true, false);

					if (filePath != null && new File(filePath).exists())
					{
						ArrayList<String> filePathArrayList = new ArrayList<String>();
						filePathArrayList.add(filePath);
						ArrayList<GalleryItem> selectedImages = GalleryItem.getGalleryItemsFromFilepaths(filePathArrayList);

						if (HikeFileType.IMAGE.equals(HikeFileType.fromString(getIntent().getType())))
						{
							selectedImages = new ArrayList<>();
							selectedImages.add(new GalleryItem(0, null, GalleryItem.CUSTOM_TILE_NAME, filePath, 0));
						}

						if (selectedImages != null && !selectedImages.isEmpty())
						{
							ParcelableSparseArray captionsSparse = new ParcelableSparseArray();

							if (!TextUtils.isEmpty(messageToShare))
							{
								captionsSparse.put(0,messageToShare);
							}

							Intent multiIntent = IntentFactory.getImageSelectionIntent(getApplicationContext(), selectedImages, true, false, captionsSparse);

							// Got images to share
							// Keep references to images (these will need to be shared via hike features (timeline,etc)
							for (GalleryItem item : selectedImages)
							{
								imagesToShare.add(item.getFilePath());
							}

							allImages = true;
							startActivityForResult(multiIntent, GallerySelectionViewer.MULTI_EDIT_REQUEST_CODE);
						}
						else
						{
							// If the MIME type of shared media is anything other than image, discard caption
							messageToShare = null;
						}
					}
				}
				// Image is not present. Is there a message to forward?
				else if(!TextUtils.isEmpty(messageToShare))
				{
					// Do nothing, adapter will show "Timeline" based on this same check on messageToShare
				}
			}
			else if(savedInstanceState == null && Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction()))
			{
				if (getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM) != null)
				{
					ArrayList<Uri> imageUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
					ArrayList<GalleryItem> selectedImages = GalleryItem.getGalleryItemsFromFilepaths(imageUris);
					if ((selectedImages != null)) {
						allImages = true;
						Intent multiIntent = IntentFactory.getImageSelectionIntent(getApplicationContext(), selectedImages, true);
						startActivityForResult(multiIntent, GallerySelectionViewer.MULTI_EDIT_REQUEST_CODE);
						// Got images to share
						// Keep references to images (these will need to be shared via hike features (timeline,etc)
						for (GalleryItem item : selectedImages)
						{
							imagesToShare.add(item.getFilePath());
						}
					}
				}
			}
			
			isForwardingMessage = true;
		}
		else if (isForwardingMessage)
		{
			if (getIntent().hasExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT))
			{
				allImages = false;
				ArrayList<String> imageFilePathArray = new ArrayList<String>();

				String jsonString = getIntent().getStringExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
				try
				{
					JSONArray multipleMsgFwdArray = new JSONArray(jsonString);

					for (int i = 0; i < multipleMsgFwdArray.length(); i++)
					{
						JSONObject msgExtrasJson = (JSONObject) multipleMsgFwdArray.get(i);

						if (msgExtrasJson.has(HikeConstants.MESSAGE))
						{
							if (multipleMsgFwdArray.length() == 1 || multipleMsgFwdArray.length() == 2)
							{
								messageToShare = msgExtrasJson.optString(HikeConstants.MESSAGE);
							}
						}

						if (msgExtrasJson.has(HikeConstants.Extras.FILE_PATH))
						{
							String filePath = msgExtrasJson.getString(HikeConstants.Extras.FILE_PATH);
							if (new File(filePath).exists())
							{
								// TODO check for recording
								if (HikeFileType.fromFilePath(filePath, false).compareTo(HikeFileType.IMAGE) != 0)
								{
									//Do nothing
								}
								else
								{
									imagesToShare.add(filePath);
									imageFilePathArray.add(filePath);
								}
							}
						}
					}

					if(imagesToShare.size() >= multipleMsgFwdArray.length())
					{
						allImages = true;
					}

					if(multipleMsgFwdArray.length() >= 2 && !allImages && !TextUtils.isEmpty(messageToShare))
					{
						messageToShare = null;
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				if(allImages)
				{
					ArrayList<GalleryItem> selectedImages = GalleryItem.getGalleryItemsFromFilepaths(imageFilePathArray);
					if((selectedImages!=null))
					{
						Intent multiIntent = IntentFactory.getImageSelectionIntent(getApplicationContext(),selectedImages,true);
						if(savedInstanceState == null)
						startActivityForResult(multiIntent, GallerySelectionViewer.MULTI_EDIT_REQUEST_CODE);
					}
				}
			}
		}

		if(nuxIncentiveMode)
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			getSupportActionBar().hide();
		}
		else
		{
			setActionBar();
		}

		init();
		
		mPubSub = HikeMessengerApp.getPubSub();
		mPubSub.addListeners(this, hikePubSubListeners);
	}

	boolean isOpened = false;

	private HikeDialog contactDialog;

	 public void setListnerToRootView(){
	    final View activityRootView = getWindow().getDecorView().findViewById(R.id.ll_compose); 
	    activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {

				int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
				if (heightDiff > 100) { // 99% of the time the height diff will be due to a keyboard.

					if (isOpened == false) {
						FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
						ft.hide(newFragment);
						ft.commit();
					}
					isOpened = true;
				} else if (isOpened == true) {
					isOpened = false;
					FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
					if (tagEditText.getText().toString().length() == 0) {
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
		super.onSaveInstanceState(outState);
		outState.putBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT, deviceDetailsSent);
		outState.putStringArrayList(HikeConstants.Extras.BROADCAST_RECIPIENTS, (ArrayList<String>) adapter.getAllSelectedContactsMsisdns());
		outState.putStringArrayList(IMAGES_TO_SHARE, imagesToShare);
		outState.putString(MSG_TO_SHARE, messageToShare);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		restoreItemsOnConfigChange(savedInstanceState.getStringArrayList(HikeConstants.Extras.BROADCAST_RECIPIENTS));
	}
	
	/**
	 * This method has been created to handle the activity restoration when 'Do not keep activity' flag is checked in Developer options.
	 * When this flag is checked, the activity is killed as soon as you leave it. 
	 * So, we're using this method to restore the saved state parameters.
	 * 
	 * @param savedInstanceState
	 */
	private void restoreItemsOnConfigChange(ArrayList<String> msisdns)
	{
		if (!(msisdns == null || msisdns.isEmpty()))
		{
			adapter.selectAllFromList(msisdns);
			adapter.notifyDataSetChanged();
			int selected = adapter.getCurrentSelection();
			// Using selectAllMode here, because it arises in a corner case of 'Do not keep activity' flag on, and user presses back from broadcast name screen.
//			TODO a new selectSomeMode to handle this case
			selectAllMode = true;
			tagEditText.toggleTag(new Tag(getString(selected == 1 ? R.string.selected_contacts_count_singular : R.string.selected_contacts_count_plural, selected), SELECT_ALL_MSISDN,
					SELECT_ALL_MSISDN));
			setupMultiSelectActionBar();
			invalidateOptionsMenu();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		mainMenu = menu;
		type = getIntent().getIntExtra(HikeConstants.Extras.SHARE_TYPE, HikeConstants.Extras.NOT_SHAREABLE);

		if (!showingMultiSelectActionBar)
		{
			getMenuInflater().inflate(R.menu.compose_chat_menu, menu);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		else
		{	
			getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		}
		
		if (composeMode == START_CHAT_MODE||composeMode==HIKE_DIRECT_MODE)
		{
			initSearchMenu(menu);
		}
		
		if (type != HikeConstants.Extras.NOT_SHAREABLE && Utils.isPackageInstalled(getApplicationContext(), HikeConstants.Extras.WHATSAPP_PACKAGE))
		{
			if (menu.hasVisibleItems())
			{
				menu.findItem(R.id.whatsapp_share).setVisible(true);
			}

		}

		return true;
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
		
		if (item.getItemId() == R.id.whatsapp_share)
		{     
			if (Utils.isPackageInstalled(getApplicationContext(), HikeConstants.Extras.WHATSAPP_PACKAGE))
			{
				String str = getIntent().getStringExtra(HikeConstants.Extras.SHARE_CONTENT);

				switch (type)
				{
				case HikeConstants.Extras.ShareTypes.STICKER_SHARE:
					HAManager.getInstance().shareWhatsappAnalytics(HikeConstants.Extras.STICKER_SHARE, getIntent().getStringExtra(StickerManager.CATEGORY_ID),
							getIntent().getStringExtra(StickerManager.STICKER_ID), str);
					break;

				case HikeConstants.Extras.ShareTypes.IMAGE_SHARE:
					HAManager.getInstance().shareWhatsappAnalytics(HikeConstants.Extras.IMAGE_SHARE);
					break;

				case HikeConstants.Extras.ShareTypes.TEXT_SHARE:
					HAManager.getInstance().shareWhatsappAnalytics(HikeConstants.Extras.TEXT_SHARE);
					break;

				}
				ShareUtils.shareContent(type, str, HikeConstants.Extras.WHATSAPP_PACKAGE, false);
				this.finish();
			}

			else
			{
				Toast.makeText(getApplicationContext(), getString(R.string.whatsapp_uninstalled), Toast.LENGTH_SHORT).show();
			}
		}
		
		if(item.getItemId()==android.R.id.home)
		{
			onBackPressed();
			return true;
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

		String sendingMsisdn = null;
		if(getIntent()!=null){
			sendingMsisdn = getIntent().getStringExtra(HikeConstants.Extras.PREV_MSISDN);
		}
		boolean showNujNotif = PreferenceManager.getDefaultSharedPreferences(ComposeChatActivity.this).getBoolean(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF, true);
		HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		boolean fetchRecentlyJoined = pref.getData(HikeConstants.SHOW_RECENTLY_JOINED_DOT, false) || pref.getData(HikeConstants.SHOW_RECENTLY_JOINED, false);
		fetchRecentlyJoined = fetchRecentlyJoined && !isForwardingMessage && showNujNotif;
		boolean showMicroappShowcase = BotUtils.isBotDiscoveryEnabled();
		boolean showBdaySection = Utils.isBDayInNewChatEnabled() && (pref.getStringSet(HikeConstants.BDAYS_LIST, new HashSet<String>()).size() > 0);
		
		switch (composeMode)
		{
		case HIKE_DIRECT_MODE:
		case CREATE_BROADCAST_MODE:
		case PICK_CONTACT_AND_SEND_MODE:
		case PICK_CONTACT_MODE:
			//We do not show sms contacts in broadcast mode
			adapter = new ComposeChatAdapter(this, listView, isForwardingMessage, (isForwardingMessage && !isSharingFile), fetchRecentlyJoined, existingGroupOrBroadcastId, sendingMsisdn, friendsListFetchedCallback, false, false,isContactChooserFilter,isShowTimeline(), false);
			break;
		case CREATE_GROUP_MODE:
			adapter = new ComposeChatAdapter(this, listView, isForwardingMessage, (isForwardingMessage || isSharingFile), fetchRecentlyJoined, existingGroupOrBroadcastId,
					sendingMsisdn, friendsListFetchedCallback, true, (showMicroappShowcase && hasMicroappShowcaseIntent), isContactChooserFilter, isShowTimeline(), false);
			break;
		case START_CHAT_MODE:
			adapter = new ComposeChatAdapter(this, listView, isForwardingMessage, (isForwardingMessage || isSharingFile), fetchRecentlyJoined, existingGroupOrBroadcastId,
					sendingMsisdn, friendsListFetchedCallback, true, (showMicroappShowcase && hasMicroappShowcaseIntent), isContactChooserFilter, isShowTimeline(),
					showBdaySection);
			break;

		default:
			adapter = new ComposeChatAdapter(this, listView, isForwardingMessage, (isForwardingMessage || isSharingFile), fetchRecentlyJoined, existingGroupOrBroadcastId,
					sendingMsisdn, friendsListFetchedCallback, true, (showMicroappShowcase && hasMicroappShowcaseIntent), isContactChooserFilter, isShowTimeline(), false);
			break;
		}

		View emptyView = findViewById(android.R.id.empty);
		adapter.setEmptyView(emptyView);
		adapter.setLoadingView(findViewById(R.id.spinner));

		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		listView.setOnScrollListener(this);

		originalAdapterLength = adapter.getCount();

		if (this.composeMode != START_CHAT_MODE && this.composeMode != HIKE_DIRECT_MODE)
		{
			initTagEditText();
			tagEditText.setVisibility(View.VISIBLE);
		}
		
		else
		{
			tagEditText = (TagEditText) findViewById(R.id.composeChatNewGroupTagET);
			tagEditText.setVisibility(View.GONE);
		}
		
		if (existingGroupOrBroadcastId != null)
		{
			MIN_MEMBERS_GROUP_CHAT = 1;
		}
		
		setModeAndUpdateAdapter(composeMode);
		
		adapter.setIsCreatingOrEditingGroup(this.composeMode == CREATE_GROUP_MODE || this.composeMode == CREATE_BROADCAST_MODE);

		adapter.executeFetchTask();
		
		pref.saveData(HikeConstants.SHOW_RECENTLY_JOINED_DOT, false);
		pref.saveData(HikeConstants.SHOW_RECENTLY_JOINED, false);
		pref.saveData(HikeConstants.NEW_CHAT_RED_DOT, false);
		
		if(triggerPointForPopup!=ProductPopupsConstants.PopupTriggerPoints.UNKNOWN.ordinal())
		{
			showProductPopup(triggerPointForPopup);
		}
	}

	private boolean isShowTimeline(){
		//This method gives the value whether to show timeline option in compose chat list,
		//if there is an explicit extra mentioned, then use that value.
		boolean showTimeline;
		if(getIntent()!= null && getIntent().hasExtra(HikeConstants.Extras.SHOW_TIMELINE)){
			showTimeline = getIntent().getBooleanExtra(HikeConstants.Extras.SHOW_TIMELINE, false);
		}else{
			showTimeline = allImages || !TextUtils.isEmpty(messageToShare);
		}
		return showTimeline;
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
		super.onPause();
		if(adapter != null)
		{
			adapter.getIconLoader().setExitTasksEarly(true);
		}
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		if(adapter != null)
		{
			adapter.getIconLoader().setExitTasksEarly(false);
			adapter.notifyDataSetChanged();
		}

		Logger.d(HikeConstants.COMPOSE_SCREEN_OPENING_BENCHMARK, "end=" + System.currentTimeMillis());
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
		
		if (adapter != null)
		{
			adapter.releaseResources();
		}

		HikeMessengerApp.getPubSub().removeListeners(this, hikePubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
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

		if (ContactManager.getInstance().isBlocked(contactInfo.getMsisdn()))
		{
			showToast(getString(R.string.block_overlay_message, contactInfo.getFirstName()));
			return;
		}

		if(isContactChooserFilter)
        {
            ArrayList<ContactInfo> contactInfos = new ArrayList<>(1);
            contactInfos.add(contactInfo);
            ConvertToJsonArrayTask convertToJsonArrayTask = new ConvertToJsonArrayTask(this,contactInfos,true);
			convertToJsonArrayTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return;
        }


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
			else if (this.composeMode == CREATE_GROUP_MODE && adapter.getSelectedContactCount()+1 >= HikeConstants.MAX_CONTACTS_IN_GROUP && !adapter.isContactAdded(contactInfo))
			{
				showToast(getString(R.string.maxContactInGroupErr, HikeConstants.MAX_CONTACTS_IN_GROUP));
				return;
			}else if (this.composeMode == CREATE_BROADCAST_MODE &&  adapter.getSelectedContactCount() >= HikeConstants.MAX_CONTACTS_IN_BROADCAST && !adapter.isContactAdded(contactInfo))
			{
				showToast(getString(R.string.maxContactInBroadcastErr, HikeConstants.MAX_CONTACTS_IN_BROADCAST));
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
				toggleTag(getString(selected==1 ? R.string.selected_contacts_count_singular : R.string.selected_contacts_count_plural,selected), SELECT_ALL_MSISDN, SELECT_ALL_MSISDN);
				}else{
					((CheckBox)findViewById(R.id.select_all_cb)).setChecked(false); // very rare case
				}
			}
			else
			{
				toggleTag(name, contactInfo.getMsisdn(), contactInfo);
			}
			break;
		case PICK_CONTACT_MODE:
		case PICK_CONTACT_AND_SEND_MODE:
			if (StealthModeManager.getInstance().isStealthMsisdn(contactInfo.getMsisdn()))
			{
				if (!StealthModeManager.getInstance().isActive())
				{
					return;
				}
			}

			if(selectAllMode)
			{
				onItemClickDuringSelectAllMode(contactInfo);
			}
			else
			{
				toggleTag(contactInfo.getNameOrMsisdn(), contactInfo.getMsisdn(), contactInfo);
			}
			break;
		default:
			Logger.i("composeactivity", contactInfo.getId() + " - id of clicked");
			if (FriendsAdapter.SECTION_ID.equals(contactInfo.getId()) || FriendsAdapter.EMPTY_ID.equals(contactInfo.getId()))
			{
				return;
			}

			/*
			 * This would be true if the user entered a stealth msisdn and tried starting a chat with him/her in non stealth mode.
			 */
			if (StealthModeManager.getInstance().isStealthMsisdn(contactInfo.getMsisdn()))
			{
				if (!StealthModeManager.getInstance().isActive())
				{
					return;
				}
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
					onItemClickDuringSelectAllMode(contactInfo);
				}
				else{
					name = viewtype == ViewType.NOT_FRIEND_SMS.ordinal() ? contactInfo.getName() + " (SMS) " : contactInfo.getName();
					if (!nuxIncentiveMode) {
						// change is to prevent the Tags from appearing in the search bar.
						toggleTag(name, contactInfo.getMsisdn(), contactInfo);
					}
					else {
						// newFragment.toggleViews(contactInfo);
						FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
						ft.show(newFragment);
						ft.commit();
						final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(tagEditText.getWindowToken(), 0);
						adapter.removeFilter();
						tagEditText.clear(false);
						if (!adapter.isContactAdded(contactInfo))
						{
							if (newFragment.addView(contactInfo))
								selectContact(contactInfo);

						}
						else
						{
							if (newFragment.removeView(contactInfo))
								deSelectContact(contactInfo);
						}
					}
				}
			}
			else
			{
				if(getIntent().hasExtra(HikeConstants.Extras.HIKE_DIRECT_MODE))
				{
					Intent in=IntentFactory.createChatThreadIntentFromContactInfo(this, contactInfo, false, false, ChatThreadActivity.ChatThreadOpenSources.NEW_COMPOSE);
					in.putExtra(HikeConstants.Extras.HIKE_DIRECT_MODE, true);
					startActivity(in);
				}
				else if (adapter.isBirthdayContact(contactInfo))
				{
					Intent in = IntentFactory.createChatThreadIntentFromContactInfo(this, contactInfo, false, false, ChatThreadActivity.ChatThreadOpenSources.NEW_COMPOSE);
					in.putExtra(HikeConstants.Extras.HIKE_BDAY_MODE, true);
					startActivity(in);
				}
				else
				{
					Utils.startChatThread(this, contactInfo, ChatThreadActivity.ChatThreadOpenSources.NEW_COMPOSE);
				}
				finish();
			}
			break;
		}
	}

	private void sendFriendRequest(ContactInfo info)
	{
		if (!OneToNConversationUtils.isOneToNConversation(info.getMsisdn()))
		{
			info.setFavoriteType(Utils.toggleFavorite(this, info, false, HikeConstants.AddFriendSources.FORWARD_SCREEN));
			if (info.isMyTwoWayFriend())
				Toast.makeText(this, R.string.friend_request_sent, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void onItemClickDuringSelectAllMode(ContactInfo contactInfo){

		tagEditText.clear(false);
		if (!adapter.isContactAdded(contactInfo))
		{
			selectContact(contactInfo);
		}
		else
		{
			deSelectContact(contactInfo);
		}
		int selected = adapter.getSelectedContactCount();
		if(selected>0){
		toggleTag(getString(selected == 1 ? R.string.selected_contacts_count_singular : R.string.selected_contacts_count_plural, selected), SELECT_ALL_MSISDN, SELECT_ALL_MSISDN);
		}else{
			((CheckBox)findViewById(R.id.select_all_cb)).setChecked(false); // very rare case
		}
	
	}

	@Override
	public void tagRemoved(Tag tag)
	{
		if(selectAllMode){
		((CheckBox) findViewById(R.id.select_all_cb)).setChecked(false);
		}else{
			if(tag.data instanceof ContactInfo){
				adapter.removeContact((ContactInfo) tag.data);
			}
		}
		if (adapter.getCurrentSelection() == 0)
		{
			setActionBar();
			invalidateOptionsMenu();
		}
		else
        {
			if (createGroup) {
				multiSelectTitle.setText(getString(R.string.group_selected,
						adapter.getCurrentSelection()));
			} else {

			multiSelectTitle.setText(createBroadcast ? getString(R.string.broadcast_selected, adapter.getCurrentSelection()) : 
				getString(R.string.gallery_num_selected, adapter.getCurrentSelection()));
					}
		 }
	}

	@Override
	public void tagAdded(Tag tag)
	{
		String dataString = null;
		if(tag.data instanceof ContactInfo){
			ContactInfo contactInfo = (ContactInfo) tag.data;
			selectContact(contactInfo);
		}else if(tag.data instanceof String)
		{
			dataString = (String) tag.data;
		}

		/* AND-2137:: [Optional]
		Adding this for optimization - as there is no change from 1st selection to 2nd selection in views,
		except multiSelectTitle which is updated below anyways */
		if (adapter.getCurrentSelection() == 1 || selectAllMode ) {
			setupMultiSelectActionBar();
			invalidateOptionsMenu();
		}
		if (createGroup) {
			multiSelectTitle.setText(getString(R.string.group_selected,
					adapter.getCurrentSelection()));
		} else {
	    	multiSelectTitle.setText(getString(R.string.gallery_num_selected, adapter.getCurrentSelection()));
			}
		}


	private void selectContact(ContactInfo contactInfo)
	{
		if (!contactInfo.isMyOneWayFriend() && Utils.isFavToFriendsMigrationAllowed() && composeMode == MULTIPLE_FWD) {
			sendFriendRequest(contactInfo);
		}
		adapter.addContact(contactInfo);
	}

	private void deSelectContact(ContactInfo contactInfo)
	{
		adapter.removeContact(contactInfo);

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
	}
	
	@Override
	public void tagClicked(Tag tag)
	{
		// TODO Auto-generated method stub
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
		}
		else if(nuxIncentiveMode)
		{
			mode = NUX_INCENTIVE_MODE;
		}
		else if (isForwardingMessage || isSharingFile)
		{
			mode = MULTIPLE_FWD;
		}
		else if(getIntent().hasExtra(HikeConstants.Extras.GROUP_CREATE_BUNDLE) || getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT))
		{
				mode=CREATE_GROUP_MODE;
		}
		else if (getIntent().hasExtra(HikeConstants.Extras.HIKE_DIRECT_MODE))
		{
			mode = HIKE_DIRECT_MODE;
		}
		else if (addToConference) {
			mode = CREATE_GROUP_MODE;
		}
		else
		{
				mode=START_CHAT_MODE;
		}
		setMode(mode);
	}
	
	private void setModeAndUpdateAdapter(int mode)
	{
		setMode(mode);
		switch (composeMode)
		{
		case CREATE_GROUP_MODE:
		case PICK_CONTACT_MODE:
        case PICK_CONTACT_SINGLE_MODE:
		case PICK_CONTACT_AND_SEND_MODE:
		case CREATE_BROADCAST_MODE:
			// createGroupHeader.setVisibility(View.GONE);
			adapter.showCheckBoxAgainstItems(true);
			tagEditText.clear(false);
			adapter.removeFilter();
			adapter.clearAllSelection(true);
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_group_mode);
			if (this.composeMode == CREATE_BROADCAST_MODE)
			{
				triggerPointForPopup = ProductPopupsConstants.PopupTriggerPoints.BROADCAST.ordinal();
			}
			break;
		case START_CHAT_MODE:
			// createGroupHeader.setVisibility(View.VISIBLE);
			tagEditText.clear(false);
			adapter.clearAllSelection(false);
			adapter.removeFilter();
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_chat_mode);
			triggerPointForPopup = ProductPopupsConstants.PopupTriggerPoints.COMPOSE_CHAT.ordinal();
			return;
		case MULTIPLE_FWD:
			// createGroupHeader.setVisibility(View.GONE);
			adapter.showCheckBoxAgainstItems(true);
			adapter.provideAddFriend(true);
			tagEditText.clear(false);
			adapter.removeFilter();
			adapter.clearAllSelection(true);
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_group_mode);
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
		case HIKE_DIRECT_MODE:
			tagEditText.clear(false);
			adapter.clearAllSelection(false);
			adapter.removeFilter();
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_chat_mode);
			break;
		case PAYMENT_MODE:
			tagEditText.clear(false);
			adapter.clearAllSelection(false);
			adapter.removeFilter();
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_payment_mode);
			break;
		}
		if(!nuxIncentiveMode) 
			setTitle();
	}
	
	private void setupForSelectAll(){
		
		if(!(this.composeMode == CREATE_BROADCAST_MODE || this.composeMode == MULTIPLE_FWD))
		{
			return;
		}
		
		if (adapter.getOnHikeContactsCount() == 0)
		{
			return;
		}
		
		View selectAllCont = findViewById(R.id.select_all_container);
		selectAllCont.setVisibility(View.VISIBLE);
		final TextView tv = (TextView) selectAllCont.findViewById(R.id.select_all_text);
		CheckBox cb = (CheckBox) selectAllCont.findViewById(R.id.select_all_cb);
		cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					// call adapter select all
					selectAllMode = true;
					tv.setText(getString(R.string.unselect_all_hike));
					if (composeMode == CREATE_BROADCAST_MODE) {
						if (adapter.getOnHikeContactsCount() > HikeConstants.MAX_CONTACTS_IN_BROADCAST) {
							showToast(getString(R.string.maxContactInBroadcastErr, HikeConstants.MAX_CONTACTS_IN_BROADCAST));
							tv.setText(getString(R.string.select_all_hike));
							buttonView.setChecked(false);
							return;
						}
					}
					adapter.selectAllContacts(true);
					tagEditText.clear(false);
					int selected = adapter.getCurrentSelection();
					toggleTag( getString(selected <=1 ? R.string.selected_contacts_count_singular : R.string.selected_contacts_count_plural,selected), SELECT_ALL_MSISDN, SELECT_ALL_MSISDN);

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

	private void startCreateBroadcastActivity()
	{
		Intent broadcastIntent = IntentFactory.createNewBroadcastActivityIntent(ComposeChatActivity.this);
		startActivityForResult(broadcastIntent, OPEN_CREATE_BROADCAST_ACTIVITY);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK)
		{
			switch (requestCode)
			{
			case OPEN_CREATE_BROADCAST_ACTIVITY:
				Bundle broadcastBundle = data.getBundleExtra(HikeConstants.Extras.BROADCAST_CREATE_BUNDLE);
				createBroadcast = broadcastBundle.getBoolean(HikeConstants.Extras.CREATE_BROADCAST, true);
				oneToNConvName = broadcastBundle.getString(HikeConstants.Extras.ONETON_CONVERSATION_NAME);
				oneToNConvId = broadcastBundle.getString(HikeConstants.Extras.CONVERSATION_ID);
				OneToNConversationUtils.createGroupOrBroadcast(this, adapter.getAllSelectedContacts(), oneToNConvName, oneToNConvId, -1);
				break;

			case HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE:
				String sharedFilepath = data.getStringExtra(HikeConstants.Extras.IMAGE_PATH);
				if (sharedFilepath != null && (new File(sharedFilepath)).exists())
				{
					Intent intent = getIntent();
					intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(sharedFilepath)));
					setIntent(intent);
				}
				else
				{
					onError();
				}
				break;

			case GallerySelectionViewer.MULTI_EDIT_REQUEST_CODE:
				ArrayList<Uri> imageUris = data.getParcelableArrayListExtra(HikeConstants.IMAGE_PATHS);
				ArrayList<String> editedPaths = data.getStringArrayListExtra(HikeConstants.EDITED_IMAGE_PATHS);
				if (imageUris != null && !imageUris.isEmpty())
				{
					if (imageUris.size() > 1)
					{
						Intent intent = getIntent();
						intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
						setIntent(intent);
					}
					else
					{
						Intent intent = getIntent();
						intent.setAction(Intent.ACTION_SEND);
						intent.putExtra(Intent.EXTRA_STREAM, imageUris.get(0));
						setIntent(intent);
					}

					imagesToShare.clear();
					for (Uri uri : imageUris)
					{
						imagesToShare.add(uri.getPath());
					}

					if(data.getStringArrayListExtra(HikeConstants.CAPTION) != null)
					{
						imageCaptions.clear();
						imageCaptions.addAll(data.getStringArrayListExtra(HikeConstants.CAPTION));
					}

					Intent intent = getIntent();
					if (intent.hasExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT))
					{
						String jsonString = intent.getStringExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
						try
						{
							JSONArray multipleMsgFwdArray = new JSONArray(jsonString);
							JSONArray newMultipleMsgFwdArray = new JSONArray();
							int msgCount = multipleMsgFwdArray.length();
							int captionCounter = 0;

							for (int i = 0; i < msgCount; i++)
							{
								JSONObject msgExtrasJson = (JSONObject) multipleMsgFwdArray.get(i);
								if (msgExtrasJson.has(HikeConstants.Extras.FILE_PATH))
								{
									String filePath = msgExtrasJson.getString(HikeConstants.Extras.FILE_PATH);
									if (!imagesToShare.contains(filePath) && !Utils.isEmpty(editedPaths))
									{
										msgExtrasJson.remove(HikeConstants.Extras.FILE_PATH);
										msgExtrasJson.put(HikeConstants.Extras.FILE_PATH, editedPaths.remove(0));
										msgExtrasJson.remove(HikeConstants.Extras.FILE_KEY);
									}

									if (!imageCaptions.isEmpty() && imageCaptions.size() - 1 >= captionCounter)
									{
										msgExtrasJson.put(HikeConstants.CAPTION, imageCaptions.get(captionCounter));
									}
									captionCounter++;
									newMultipleMsgFwdArray.put(msgExtrasJson);
								}
							}
							intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, newMultipleMsgFwdArray.toString());
							setIntent(intent);
						}
						catch (JSONException je)
						{
							je.printStackTrace();
							onError();
						}
					}
				}
				else
				{
					onError();
				}
				break;
			}

		}
		else
		{
			switch (requestCode)
			{
			case HikeConstants.ResultCodes.PHOTOS_REQUEST_CODE:
			case GallerySelectionViewer.MULTI_EDIT_REQUEST_CODE:
				ComposeChatActivity.this.finish();
				break;
			}

		}
	}
	
	private void onError()
	{
		Toast.makeText(getApplicationContext(), R.string.unable_to_open, Toast.LENGTH_LONG).show();
		ComposeChatActivity.this.finish();
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


		title = (TextView) groupChatActionBar.findViewById(R.id.title);
		groupChatActionBar.findViewById(R.id.seprator).setVisibility(View.GONE);
	
		if(!nuxIncentiveMode)
			setTitle();
		
		if(HikeMessengerApp.syncingContacts)
		{
			// For showing progress bar when activity is closed and opened again
			showProgressBarContactsSync(View.VISIBLE);
		}

		actionBar.setCustomView(groupChatActionBar);
		Toolbar parent=(Toolbar)groupChatActionBar.getParent();
		parent.setContentInsetsAbsolute(0,0);

		showingMultiSelectActionBar = false;
	}

	private void setTitle()
	{
		if (composeMode == HIKE_DIRECT_MODE)
		{
			title.setText(R.string.scan_free_hike);
		}
		else if (composeMode == PICK_CONTACT_MODE || composeMode == PICK_CONTACT_AND_SEND_MODE)
		{
			title.setText(R.string.choose_contact);
		}
		else if (addToConference) 
		{
			title.setText(R.string.add_members_to_conference);
		}
		else if (createGroup)
		{
			title.setText(R.string.add_members);
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
        else if (this.composeMode == PICK_CONTACT_SINGLE_MODE)
        {
            title.setText(R.string.contacts);
        }else if (titleText!=null)
        {
            title.setText(titleText);
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
		if (createGroup) {
			multiSelectTitle.setText(getString(R.string.group_selected,
					adapter.getCurrentSelection()));
		} else {
		multiSelectTitle.setText(createBroadcast ? getString(R.string.broadcast_selected, adapter.getCurrentSelection()) : 
			getString(R.string.gallery_num_selected, adapter.getCurrentSelection()));
		}
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
					HikeHandlerUtil.getInstance().postRunnable(new Runnable() {
						@Override
						public void run()
						{
							sendAnalyticsMultiSelSend();
						}
					});

					ArrayList<ContactInfo> recepientList = adapter.getAllSelectedContacts();

					if(recepientList.size() == 1 && (imagesToShare.size() == 1 || !TextUtils.isEmpty(messageToShare)))
					{
						//No need to show confirmation since we are opening statusupdate activity
						if(recepientList.get(0) instanceof HikeFeatureInfo)
						{
							if (composeMode == PICK_CONTACT_AND_SEND_MODE)
							{
								onSendContactAndPick(recepientList);
							}
							else
							{
								forwardMultipleMessages(recepientList);
							}
							return;
						}
					}

					forwardConfirmation(recepientList);
				}
				else if (addToConference) {
					sendAddToConferenceResult();
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
					startCreateBroadcastActivity();
				}
				else if (getIntent().hasExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST))
				{
					OneToNConversationUtils.createGroupOrBroadcast(ComposeChatActivity.this, adapter.getAllSelectedContacts(), oneToNConvName, oneToNConvId, -1);
				}
				else if (composeMode == CREATE_GROUP_MODE)
				{
					OneToNConversationUtils.createGroupOrBroadcast(ComposeChatActivity.this, adapter.getAllSelectedContacts(), oneToNConvName, oneToNConvId, gcSettings);

					/*
					 *	oneToNConvId is null when we're adding members to an existing group chat. We need to send this data only for group "creation" flow.
					 */
					if (!TextUtils.isEmpty(oneToNConvId))
					{
						ArrayList<String> selectedContacts = (ArrayList<String>) adapter.getAllSelectedContactsMsisdns();
						HikeAnalyticsEvent.recordAnalyticsForGCFlow(ChatAnalyticConstants.GCEvents.GC_CLICK_CREATE_GROUP, oneToNConvName, ContactManager.getInstance().hasIcon(oneToNConvId) ? 1: 0, gcSettings, selectedContacts.size(), selectedContacts);
					}
				}
				else if(composeMode == PICK_CONTACT_MODE)
				{
					onDoneClickPickContact();
				}
				else if (composeMode == PICK_CONTACT_AND_SEND_MODE)
				{
					forwardConfirmation(adapter.getAllSelectedContacts());
				}
			}
		});

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				getIntent().removeExtra(HikeConstants.Extras.SELECT_ALL_INITIALLY);
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
		Toolbar parent=(Toolbar)multiSelectActionBar.getParent();
		parent.setContentInsetsAbsolute(0,0);
		//Begin : AND-2137
		if(!showingMultiSelectActionBar) {
			Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
			slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
			slideIn.setDuration(200);
			closeBtn.startAnimation(slideIn);
			sendBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));
		}
		//End : AND-2137
		showingMultiSelectActionBar = true;
	}
	
	/**
	 * ComposeChatActivity was called to add people to an ongoing VoIP call. 
	 * Return the list of people selected from the contact picker. 
	 */
	private void sendAddToConferenceResult() {
		int selected = adapter.getCurrentSelection();
		if (selected < 1)
		{
			Toast.makeText(this,R.string.pick_contact_zero,Toast.LENGTH_SHORT).show();
			return;
		}
		
		Intent intent = getIntent();
		ArrayList<String> contacts = new ArrayList<>();
		
		// We only want the msisdn's of the selected contacts
		for (ContactInfo contact : adapter.getAllSelectedContacts()) {
			contacts.add(contact.getMsisdn());
		}

		intent.putStringArrayListExtra(HikeConstants.HIKE_CONTACT_PICKER_RESULT_FOR_CONFERENCE, contacts);
		setResult(RESULT_OK, intent);
		this.finish();
	}

	private void onDoneClickPickContact()
	{
		int selected = adapter.getCurrentSelection();
		if (selected < 1)
		{
			Toast.makeText(this,R.string.pick_contact_zero,Toast.LENGTH_SHORT).show();
			return;
		}
		
		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setCancelable(false);
		dialog.setTitle(getResources().getString(R.string.please_wait));
		dialog.setMessage(getResources().getString(R.string.loading_data));
		ConvertToJsonArrayTask task = new ConvertToJsonArrayTask(this,adapter.getAllSelectedContacts(), dialog, thumbnailsRequired);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	private void forwardConfirmation(final ArrayList<ContactInfo> arrayList)
	{
		HikeDialogFactory.showDialog(this, HikeDialogFactory.FORWARD_CONFIRMATION_DIALOG, new HikeDialogListener()
		{
			
			@Override
			public void positiveClicked(HikeDialog hikeDialog)
			{
				hikeDialog.dismiss();
				if (composeMode == PICK_CONTACT_AND_SEND_MODE)
				{
					onSendContactAndPick(arrayList);
				}
				else
				{
					forwardMultipleMessages(arrayList);
				}
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
		if (isSharingFile)
		{
			try
			{
				JSONObject metadata = new JSONObject();
				if (selectAllMode)
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SELECT_ALL_SHARE);
				}
				else
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.CONFIRM_SHARE);
				}
				metadata.put(AnalyticsConstants.SELECTED_USER_COUNT_SHARE, arrayList.size());
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch (JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			Intent intent = null;
			if (arrayList.size() == 1)
			{
				ContactInfo contactInfo = arrayList.get(0);
				if(contactInfo instanceof HikeFeatureInfo)
				{
					HikeFeatureInfo hikeFeatureInfo = (HikeFeatureInfo)contactInfo;
					if(hikeFeatureInfo.getPhoneNum() == ComposeChatAdapter.HIKE_FEATURES_TIMELINE_ID)
					{
						if(imagesToShare.isEmpty())
						{
							intent = IntentFactory.getPostStatusUpdateIntent(this, messageToShare, null, true);
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							finish();
							return;
						}
						else if(imagesToShare.size() == 1)
						{
							intent = IntentFactory.getPostStatusUpdateIntent(this, imageCaptions.isEmpty()?null:imageCaptions.get(0), imagesToShare.get(0),true);
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							finish();
							return;
						}
						else
						{
							postImagesToShareOnTimeline(true);
							return;
						}
					}
				}
				else
				{
					intent = IntentFactory.createChatThreadIntentFromContactInfo(this, arrayList.get(0), true, false, ChatThreadActivity.ChatThreadOpenSources.FORWARD);
				}
			}
			else
			{
				// Scan through selected contacts. See if there are images to be posted on Timeline.
				HikeFeatureInfo hikefeatureInfo = getTimelineHikeFeatureInfoFromArray(arrayList);
				if(hikefeatureInfo !=null)
				{
					arrayList.remove(hikefeatureInfo);
					postImagesToShareOnTimeline(false);
				}

				if(arrayList.size() == 1)
				{
					forwardMultipleMessages(arrayList);
					return;
				}

				intent = Utils.getHomeActivityIntent(this);
			}
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			arrayList = forwardMessageAsPerType(presentIntent, intent, arrayList);

			/*
			 * If the intent action is ACTION_SEND_MULTIPLE then we don't need to start the activity here since we start an async task for initiating the file upload and an
			 * activity is started when that async task finishes execution.
			 * 
			 * If size of arraylist is zero, it means the only contact was an offline contact and has been removed from the arrayList, thus size is zero.
			 */
			if ((!Intent.ACTION_SEND_MULTIPLE.equals(presentIntent.getAction()) && arrayList.size() <= 1) || (arrayList.size() == 0))
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
				if (selectAllMode)
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SELECT_ALL_HIKE_CONTACTS);
				}
				else
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.CONFIRM_FORWARD);
				}
				metadata.put(AnalyticsConstants.SELECTED_USER_COUNT_FWD, arrayList.size());

				try
				{
					//Sending File Transfer analytics for bots.
					if (BotUtils.isBot(presentIntent.getStringExtra(HikeConstants.Extras.PREV_MSISDN)))
					{
						JSONArray array = new JSONArray(presentIntent.getStringExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT));
						JSONObject msgObject;
						for (int i = 0; i < array.length(); i++)
						{
							msgObject = array.getJSONObject(i);
							{
								if (msgObject.has(HikeConstants.Extras.FILE_KEY))
								{
									String fileKey = msgObject.getString(HikeConstants.Extras.FILE_KEY);
									JSONObject json = new JSONObject();
									json.putOpt(AnalyticsConstants.EVENT_KEY, AnalyticsConstants.MICRO_APP_EVENT);
									json.putOpt(AnalyticsConstants.EVENT, AnalyticsConstants.BOT_CONTENT_FORWARDED);
									json.putOpt(AnalyticsConstants.LOG_FIELD_4, fileKey);
									json.putOpt(AnalyticsConstants.LOG_FIELD_1, msgObject.optString(HikeConstants.Extras.FILE_TYPE));
									json.putOpt(AnalyticsConstants.BOT_MSISDN, presentIntent.getStringExtra(HikeConstants.Extras.PREV_MSISDN));
									HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
								}
							}
						}
					}

				}
				catch (Exception e)
				{
					Logger.e("ComposeChatActivity", "Bot Content Error");
				}
// {"t":"le_android","d":{"et":"nonUiEvent","st":"repl","ep":"HIGH","cts":1456826454120,"tag":"plf","md":{"ek":"micro_app","event":"chromeCustomTabs","fld4":"forward","fld6":1,"sid":1456826429219
				if(!TextUtils.isEmpty(presentIntent.getStringExtra(AnalyticsConstants.ANALYTICS_EXTRA)))
				{
					JSONObject json = new JSONObject();
					try
					{
						json.putOpt(AnalyticsConstants.EVENT_KEY,AnalyticsConstants.MICRO_APP_EVENT);
						json.putOpt(AnalyticsConstants.EVENT,presentIntent.getStringExtra(AnalyticsConstants.ANALYTICS_EXTRA));
						json.putOpt(AnalyticsConstants.LOG_FIELD_4,AnalyticsConstants.FORWARD);
						json.putOpt(AnalyticsConstants.LOG_FIELD_6,arrayList.size());
					} catch (JSONException e)
					{
						e.printStackTrace();
					}

					HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.NON_UI_EVENT, AnalyticsConstants.MICRO_APP_REPLACED, json);
				}
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch (JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			// forwarding it is
			Intent intent = null;
			if (arrayList.size() == 1)
			{
				ContactInfo contactInfo = arrayList.get(0);
				if(contactInfo instanceof HikeFeatureInfo)
				{
					HikeFeatureInfo hikeFeatureInfo = (HikeFeatureInfo)contactInfo;
					if(hikeFeatureInfo.getPhoneNum() == ComposeChatAdapter.HIKE_FEATURES_TIMELINE_ID)
					{

						if (imagesToShare.size() == 1)
						{
							intent = IntentFactory.getPostStatusUpdateIntent(this, imageCaptions.get(0), imagesToShare.get(0), true);
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							finish();
							return;
						}
						else if(!TextUtils.isEmpty(messageToShare))
						{
							intent = IntentFactory.getPostStatusUpdateIntent(this, messageToShare,null,true);
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							finish();
							return;
						}
						else
						{
							postImagesToShareOnTimeline(true);
							return;
						}
					}
				}
				else
				{
					// forwarding to 1 is special case , we want to create conversation if does not exist and land to recipient
					intent = IntentFactory.createChatThreadIntentFromMsisdn(this, arrayList.get(0).getMsisdn(), false, false, ChatThreadActivity.ChatThreadOpenSources.FORWARD);
					intent.putExtras(presentIntent);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
				}
			}
			else
			{

				// Scan through selected contacts. See if there are images to be posted on Timeline.
				HikeFeatureInfo hikefeatureInfo = getTimelineHikeFeatureInfoFromArray(arrayList);
				if(hikefeatureInfo !=null)
				{
					arrayList.remove(hikefeatureInfo);
					postImagesToShareOnTimeline(false);
				}

				if(arrayList.size() == 1)
				{
					forwardMultipleMessages(arrayList);
					return;
				}

				// multi forward to multi people
				if (presentIntent.hasExtra(HikeConstants.Extras.PREV_MSISDN))
				{
					// open chat thread from where we initiated
					String id = presentIntent.getStringExtra(HikeConstants.Extras.PREV_MSISDN);
					if (BotUtils.isBot(id))
					{
						BotInfo botInfo = BotUtils.getBotInfoForBotMsisdn(id);
						if (botInfo.isNonMessagingBot())
						{
							intent = IntentFactory.getNonMessagingBotIntent(botInfo.getMsisdn(), this);
						}
						else
						{
							intent = IntentFactory.createChatThreadIntentFromMsisdn(this, id, false, false, ChatThreadActivity.ChatThreadOpenSources.FORWARD);
						}
					}
					else
					{
						intent = IntentFactory.createChatThreadIntentFromMsisdn(this, id, false, false, ChatThreadActivity.ChatThreadOpenSources.FORWARD);
					}

				}
				else
				{
					// home activity
					intent = Utils.getHomeActivityIntent(this);
				}


				if (intent != null)
				{
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					forwardMessageAsPerType(presentIntent, intent, arrayList);
				}
			}
		}
	}

	public static HikeFeatureInfo getTimelineHikeFeatureInfoFromArray(ArrayList<ContactInfo> argContactInfoList)
	{
		for (ContactInfo contactInfo : argContactInfoList)
		{
			if (contactInfo instanceof HikeFeatureInfo)
			{
				HikeFeatureInfo hikeFeatureInfo = (HikeFeatureInfo) contactInfo;
				if (hikeFeatureInfo.getPhoneNum() == ComposeChatAdapter.HIKE_FEATURES_TIMELINE_ID)
				{
					return hikeFeatureInfo;
				}
			}
		}
		return null;
	}

	private void postImagesToShareOnTimeline(final boolean foreground)
	{
		final ArrayList<StatusUpdateTask> statusUpdateTasks = new ArrayList<StatusUpdateTask>();

		if(Utils.isEmpty(imagesToShare) && !TextUtils.isEmpty(messageToShare))
		{
			statusUpdateTasks.add(new StatusUpdateTask(messageToShare, -1, null));
		}
		else
		{
			for (int i = 0; i < imagesToShare.size(); i++)
			{
				statusUpdateTasks.add(new StatusUpdateTask(imageCaptions.get(i), -1, imagesToShare.get(i)));
			}
		}
		if (!statusUpdateTasks.isEmpty())
		{
			HikeHandlerUtil.getInstance().postRunnable(new Runnable()
			{
				@Override
				public void run()
				{
					new MultipleStatusUpdateTask(statusUpdateTasks, new MultipleStatusUpdateTask.MultiSUTaskListener()
					{
						@Override
						public void onSuccess()
						{
							if (foreground)
							{
								ComposeChatActivity.this.runOnUiThread(new Runnable()
								{
									@Override
									public void run()
									{
										Intent intent = new Intent(ComposeChatActivity.this, TimelineActivity.class);
										intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
										intent.putExtra(TimelineActivity.TIMELINE_SOURCE, TimelineActivity.TimelineOpenSources.COMPOSE_CHAT);
										startActivity(intent);
										finish();
										return;
									}
								});

							}
						}

						@Override
						public void onFailed()
						{
							ComposeChatActivity.this.runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									Toast.makeText(HikeMessengerApp.getInstance(), R.string.multiple_su_post_failed, Toast.LENGTH_SHORT).show();
								}
							});
						}

						@Override
						public void onTimeout()
						{
							if (foreground)
							{
								ComposeChatActivity.this.runOnUiThread(new Runnable()
								{
									@Override
									public void run()
									{
										Toast.makeText(HikeMessengerApp.getInstance(), R.string.timeline_post_timeout, Toast.LENGTH_SHORT).show();
										String id = ComposeChatActivity.this.getIntent().getStringExtra(HikeConstants.Extras.PREV_MSISDN);
										Intent intent = null;
										if (!TextUtils.isEmpty(id))
										{
											intent = IntentFactory.createChatThreadIntentFromMsisdn(ComposeChatActivity.this, id, false, false, ChatThreadActivity.ChatThreadOpenSources.FORWARD);
										}
										else
										{
											intent = IntentFactory.getHomeActivityIntent(ComposeChatActivity.this);
										}
										intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
										startActivity(intent);
										finish();
										return;
									}
								});

							}
						}
					}, foreground, ComposeChatActivity.this).execute();
				}
			});

		}
	}


	private ArrayList<ContactInfo> forwardMessageAsPerType(Intent presentIntent, final Intent intent, ArrayList<ContactInfo> arrayList)
	{
		// update contact info sequence as per conversation ordering
		arrayList = updateContactInfoOrdering(arrayList);
		String type = presentIntent.getType();
		
		// check if this arrayList contains any Offline contact
		ContactInfo offlineContact = null;
		for (ContactInfo contactInfo : arrayList)
		{
			if (!TextUtils.isEmpty(OfflineUtils.getConnectedMsisdn())  && OfflineUtils.getConnectedMsisdn().equals(contactInfo.getMsisdn()))
			{
				offlineContact = contactInfo;
			}
		}
		
		// removing the contact from the list.
		OfflineController controller = null;
		if (offlineContact != null)
		{
			controller = OfflineController.getInstance();
			arrayList.remove(offlineContact);
		}

		if (Intent.ACTION_SEND_MULTIPLE.equals(presentIntent.getAction()))
		{
			if (type != null)
			{
				ArrayList<Uri> imageUris = presentIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
				ArrayList<FileTransferData> fileTransferList = new ArrayList<ComposeChatActivity.FileTransferData>();
				ArrayList<FileTransferData> offlineFileTransferList = new ArrayList<ComposeChatActivity.FileTransferData>();

				if (imageUris != null)
				{
					boolean showMaxFileToast = false;

					ArrayList<Pair<String, String>> fileDetails = new ArrayList<Pair<String, String>>(imageUris.size());
					for (int i = 0 ; i < imageUris.size() ; i++)
					{
						Uri fileUri = imageUris.get(i);
						Logger.d(getClass().getSimpleName(), "File path uri: " + fileUri.toString());

						String filePath = Utils.getAbsolutePathFromUri(fileUri, this,true);

						// Defensive fix for play store crash. java.lang.NullPointerException in java.io.File.fixSlashes.
						if(filePath == null)
						{
							Logger.e(getClass().getSimpleName(), "filePath was null. Defensive check for play store crash was hit");
							FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_7_1, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "Compose - 1forwardMessageAsPerType - file path is null.");
							continue;
						}
						
						File file = new File(filePath);
						

						String fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(filePath));
						if (fileType == null)
							fileType = presentIntent.getType();
						HikeFileType hikeFileType = HikeFileType.fromString(fileType, false);
						
						
						if (file.length() > HikeConstants.MAX_FILE_SIZE)
						{
							showMaxFileToast = true;
							if (offlineContact != null)
							{
								FileTransferData fileData = initialiseFileTransfer(filePath, null, hikeFileType, fileType, false, -1, true, arrayList,imageCaptions.get(i));
								offlineFileTransferList.add(fileData);
							}
							continue;
								
						}
						FileTransferData fileData = initialiseFileTransfer(filePath, null, hikeFileType, fileType, false, -1, true, arrayList,imageCaptions.get(i));
						if(fileData!=null){
							fileDetails.add(new Pair<String, String>(filePath, fileType));
							fileTransferList.add(fileData);
						}
					}

					
					if (showMaxFileToast && !arrayList.isEmpty() && !fileTransferList.isEmpty())
					{
						FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_1_1, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "Compose - 1forwardMessageAsPerType - Max limit is reached.");
						Toast.makeText(ComposeChatActivity.this, R.string.max_file_size, Toast.LENGTH_SHORT).show();
					}

					if (offlineContact != null)
					{
						offlineFileTransferList.addAll(fileTransferList);
						controller.sendFile(offlineFileTransferList, offlineContact.getMsisdn());
					}
					
						if (arrayList.size() >= 1)
						{
							ContactInfo contactInfo = arrayList.get(0);
							String msisdn = OneToNConversationUtils.isGroupConversation(contactInfo.getMsisdn()) ? contactInfo.getId() : contactInfo.getMsisdn();
							boolean onHike = contactInfo.isOnhike();

							if (fileDetails.isEmpty())
							{
								return arrayList;
							}

							if (arrayList.size() == 1)
							{
								fileTransferTask = new InitiateMultiFileTransferTask(getApplicationContext(), fileTransferList, msisdn, onHike, FTAnalyticEvents.OTHER_ATTACHEMENT,
										intent);
								fileTransferTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

								progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.multi_file_creation));

							}
							else if (!fileTransferList.isEmpty())
							{
								prefileTransferTask = new PreFileTransferAsycntask(fileTransferList, intent, null, false, FILE_TRANSFER);
								prefileTransferTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
							}
						}
				
					return arrayList;
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
			ArrayList<FileTransferData> offlinefileTransferList = new ArrayList<ComposeChatActivity.FileTransferData>();
			
			ArrayList<ConvMessage> multipleMessageList = new ArrayList<ConvMessage>();
			ArrayList<ConvMessage> offlineMessageList = new ArrayList<ConvMessage>();
			
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
						if(offlineContact!=null)
						{
							ConvMessage offlineConvMessage = new ConvMessage(convMessage);
							offlineConvMessage.setMessageOriginType(OriginType.OFFLINE);
							offlineMessageList.add(offlineConvMessage);
						}
					}else if(msgExtrasJson.has(HikeConstants.Extras.POKE)){
						// as we will be changing msisdn and hike status while inserting in DB
						ConvMessage convMessage = Utils.makeConvMessage(null, getString(R.string.poke_msg_english_only), true);
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
						if(offlineContact!=null)
						{
							ConvMessage offlineConvMessage = new ConvMessage(convMessage);
							offlineConvMessage.setMessageOriginType(OriginType.OFFLINE);
							offlineMessageList.add(offlineConvMessage);
						}
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
						Logger.d("ComposeChatActivity", "CompChAct : isCloudUri" + Utils.isPicasaUri(filePath));
						FileTransferData fileData = initialiseFileTransfer(filePath, fileKey, hikeFileType, fileType,
								isRecording, recordingDuration, true, arrayList,imageCaptions.get(i));
						if (fileData != null && fileData.file != null)
						{
							if ((HikeConstants.MAX_FILE_SIZE > fileData.file.length()))
							{
								fileTransferList.add(fileData);
								offlinefileTransferList.add(fileData);
							} else {
								if (offlineContact != null)
								{
									offlinefileTransferList.add(fileData);
								}
								FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_1_4, 0, FTAnalyticEvents.UPLOAD_FILE_TASK,
												"init", "Compose - InitialiseFileTransfer - Max size reached.");
								Toast.makeText(getApplicationContext(), R.string.max_file_size, Toast.LENGTH_SHORT).show();
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
						// To Do for offline
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.CONTACT_METADATA))
					{
						try
						{
							JSONObject contactJson = new JSONObject(msgExtrasJson.getString(HikeConstants.Extras.CONTACT_METADATA));
							initialiseContactTransfer(contactJson,arrayList);
							if(offlineContact!=null)
							{
								  ConvMessage offlineConvMessage = OfflineUtils.createOfflineContactConvMessage(offlineContact.getMsisdn(),contactJson,offlineContact.isOnhike());
								  offlineConvMessage.setMessageOriginType(OriginType.OFFLINE);
								  offlineMessageList.add(offlineConvMessage);
							}
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
						boolean isDis = sticker.isDisabled();
						// add this sticker to recents if this sticker is not disabled
						if (!isDis)
							StickerManager.getInstance().addRecentStickerToPallete(sticker);
						/*
						 * Making sure the sticker is not forwarded again on orientation change
						 */
						presentIntent.removeExtra(StickerManager.FWD_CATEGORY_ID);
						if(offlineContact!=null)
						{
							ArrayList<ContactInfo> offlineList = new ArrayList<>();
							offlineList.add(offlineContact);
							ConvMessage offlineConvMessage = sendSticker(sticker, categoryId,offlineList, StickerManager.FROM_FORWARD);
							offlineConvMessage.setMessageOriginType(OriginType.OFFLINE);
							offlineMessageList.add(offlineConvMessage);
						}
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
                        if(offlineContact!=null)
						{
                        	ConvMessage offlineConvMessage =  new ConvMessage(convMessage);
                        	offlineConvMessage.setMessageOriginType(OriginType.OFFLINE);
                        	offlineMessageList.add(offlineConvMessage);
						}
						multipleMessageList.add(convMessage);
					} else if(msgExtrasJson.optInt(MESSAGE_TYPE.MESSAGE_TYPE) == MESSAGE_TYPE.WEB_CONTENT || msgExtrasJson.optInt(MESSAGE_TYPE.MESSAGE_TYPE) == MESSAGE_TYPE.FORWARD_WEB_CONTENT){

						ConvMessage convMessage = getConvMessageForForwardedWebContent(msgExtrasJson);
						try
						{

							platformCards.append( TextUtils.isEmpty(platformCards) ? convMessage.webMetadata.getAppName() : "," + convMessage.webMetadata.getAppName());
						}
						catch (NullPointerException e)
						{
							e.printStackTrace();
						}

						convMessage.setMessage(msgExtrasJson.getString(HikeConstants.HIKE_MESSAGE));
						if(offlineContact!=null)
						{
							ConvMessage offlineConvMessage =  new ConvMessage(convMessage);
							offlineConvMessage.setMessageOriginType(OriginType.OFFLINE);
                        	offlineMessageList.add(offlineConvMessage);
						}
						
						multipleMessageList.add(convMessage);
					}
					/*
					 * Since the message was not forwarded, we check if we have any drafts saved for this conversation, if we do we enter it in the compose box.
					 */
				}
				platformAnalyticsJson.put(HikePlatformConstants.CARD_TYPE, platformCards);
				if(!fileTransferList.isEmpty()){
					prefileTransferTask = new PreFileTransferAsycntask(fileTransferList,intent,null, false,FILE_TRANSFER);
					prefileTransferTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}else{
					// if file trasfer started then it will show toast
					Toast.makeText(getApplicationContext(), getString(R.string.messages_sent_succees), Toast.LENGTH_LONG).show();
				}
				// Send Files and Messages Offline
				if(offlineContact!=null && !offlineMessageList.isEmpty())
				{
					controller.sendMultiMessages(offlineMessageList,offlineContact.getMsisdn());
				}
				if(offlineContact!=null  && !offlinefileTransferList.isEmpty())
				{
					controller.sendFile(offlinefileTransferList, offlineContact.getMsisdn());
				}
				
				if(multipleMessageList.size() ==0 || arrayList.size()==0){
					if(fileTransferList.isEmpty()){
						// if it is >0 then onpost execute of PreFileTransferAsycntask will start intent
						startActivity(intent);
						finish();
					}
					return arrayList;
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
			if(fileUri == null)
			{
				Toast.makeText(getApplicationContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
				return arrayList;
			}
			if (type.startsWith(HikeConstants.SHARE_CONTACT_CONTENT_TYPE))
			{
				if(offlineContact!=null)
				{
					arrayList.add(offlineContact);
				}
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
					if (TextUtils.isEmpty(contactId))
					{
						FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_2_1, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "Compose - forwardMessageAsPerType - contact id is null.");
						Toast.makeText(getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
						return arrayList;
					}
					PhonebookContact contact = PickContactParser.getContactData(contactId, this);
					final ArrayList<ContactInfo> finalArrayList = arrayList;
					final ContactInfo finalOfflineContact = offlineContact;
					if (contact != null)
					{
						contactDialog = HikeDialogFactory.showDialog(this, HikeDialogFactory.CONTACT_SEND_DIALOG, new HikeDialogListener()
						{
							
							@Override
							public void positiveClicked(HikeDialog hikeDialog)
							{
								if(finalOfflineContact !=null) {
									ConvMessage offlineConvMessage = OfflineUtils.createOfflineContactConvMessage(finalOfflineContact.getMsisdn(), ((PhonebookContact) hikeDialog.data).jsonData, true);
									OfflineController.getInstance().sendMessage(offlineConvMessage);
									finalArrayList.remove(finalOfflineContact);
								}
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
						}, contact, getString(R.string.send_uppercase), false);
					}

				}
			}
			else
			{
				Logger.d(getClass().getSimpleName(), "File path uri: " + fileUri.toString());
				ArrayList<FileTransferData> fileTransferList = new ArrayList<ComposeChatActivity.FileTransferData>();
				ArrayList<FileTransferData> offlineFileTransferList=new ArrayList<ComposeChatActivity.FileTransferData>();
				fileUri = Utils.makePicasaUriIfRequired(fileUri);
				
				String filePath = Utils.getAbsolutePathFromUri(fileUri, this,true);
				
				if (TextUtils.isEmpty(filePath))
				{
					FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_2_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "Compose - forwardMessageAsPerType - file path is null.");
					Toast.makeText(getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
					return arrayList;
				}
	
				File file = new File(filePath);
	
				type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(filePath));
				if (type == null)
					type = presentIntent.getType();
	
				if (arrayList.size() > 0)
				{
					intent.putExtra(HikeConstants.Extras.FILE_PATH, filePath);
					intent.putExtra(HikeConstants.Extras.FILE_TYPE, type);
					intent.putExtra(HikeConstants.CAPTION,imageCaptions.get(0));
				}
				
				HikeFileType hikeFileType = HikeFileType.fromString(
						type, false);
				
				if (file.length() > HikeConstants.MAX_FILE_SIZE)  
				{
					//Not showing toast if sharing to offline contact only
					if (offlineContact != null && arrayList.size()==0)
					{
						offlineFileTransferList.add(initialiseFileTransfer(filePath, null, hikeFileType, type, false, -1, true, arrayList));
					}
					else
					{
						//CE-815: max size toast appears even before compressing a 100MB+ video
						if(!ChatThreadUtils.isMaxSizeUploadableFile(hikeFileType, ComposeChatActivity.this)) {
							FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_1_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init",
									"Compose - forwardMessageAsPerType - Max size reached.");
							Toast.makeText(ComposeChatActivity.this, R.string.max_file_size, Toast.LENGTH_SHORT).show();
						}
					}
							
				}
				else
				{
					FileTransferData fileData = initialiseFileTransfer(filePath, null, hikeFileType, type, false, -1, true, arrayList,imageCaptions.get(0));
					if (fileData != null)
					{
						fileTransferList.add(fileData);

					}
				}
				if (offlineContact != null)
				{
					if(fileTransferList.size()>0)
					{
						offlineFileTransferList.addAll(fileTransferList);
					}
					controller.sendFile(offlineFileTransferList, offlineContact.getMsisdn());
					
				}
				
				// If the arrayList has 2 person 1 online and 1 offline contact then we need to initiate the preFileTransferTask
				if (!fileTransferList.isEmpty() && ((offlineContact != null && arrayList.size() == 1) || (arrayList.size() > 1)))
				{
					prefileTransferTask = new PreFileTransferAsycntask(fileTransferList, intent, null, false, FILE_TRANSFER);
					prefileTransferTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
				
			}
		}
		else if (presentIntent.hasExtra(Intent.EXTRA_TEXT) || presentIntent.hasExtra(HikeConstants.Extras.MSG))
		{
			String msg = IntentFactory.getTextFromActionSendIntent(presentIntent);

			if (msg == null)
				Toast.makeText(getApplicationContext(), R.string.text_empty_error, Toast.LENGTH_SHORT).show();
			else
			{
				if (offlineContact != null)
				{
					ConvMessage convMessage = Utils.makeConvMessage(offlineContact.getMsisdn(), msg, offlineContact.isOnhike());
					controller.sendMessage(convMessage);
				}

				if (arrayList.size() == 1)
				{
					ContactInfo contact = (ContactInfo) arrayList.get(0);
					if (contact != null)
					{
						ConvMessage convMessage = Utils.makeConvMessage(contact.getMsisdn(), msg, contact.isOnhike());
						sendMessage(convMessage);
					}
				}
				else
				{
					ArrayList<ConvMessage> multipleMessageList = new ArrayList<ConvMessage>();
					ConvMessage convMessage = Utils.makeConvMessage(null, msg, true);
					multipleMessageList.add(convMessage);
					sendMultiMessages(multipleMessageList, arrayList, null, false);
					startActivity(intent);
					finish();

				}

			}
		}
		return arrayList;
	}
	private OnQueryTextListener onQueryTextListener = new OnQueryTextListener()
	{
		@Override
		public boolean onQueryTextSubmit(String query)
		{
			Utils.hideSoftKeyboard(getApplicationContext(), searchMenuItem.getActionView());
			return false;
		}
		
		@Override
		public boolean onQueryTextChange(String newText)
		{
			if (newText != null)
				newText = newText.trim();
			adapter.onQueryChanged(newText);

			return true;
		}
		
		
	};
	private void onSendContactAndPick(ArrayList<ContactInfo> arrayList)
	{
		Intent presentIntent = getIntent();
		try
		{

			ArrayList<ConvMessage> multipleMessageList = new ArrayList<ConvMessage>();
			String jsonString = presentIntent.getStringExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);

			JSONArray multipleMsgFwdArray = new JSONArray(jsonString);
			int msgCount = multipleMsgFwdArray.length();
			if (arrayList.size() == 1)
			{

				for (int i = 0; i < msgCount; i++)
				{
					JSONObject msgExtrasJson = (JSONObject) multipleMsgFwdArray.get(i);
					ConvMessage convMessage = getConvMessageForForwardedWebContent(msgExtrasJson);
					convMessage.setMsisdn(arrayList.get(0).getMsisdn());
					sendMessage(convMessage);
				}
			}
			else
			{

				for (int i = 0; i < msgCount; i++)
				{
					JSONObject msgExtrasJson = (JSONObject) multipleMsgFwdArray.get(i);
					ConvMessage convMessage = getConvMessageForForwardedWebContent(msgExtrasJson);
					multipleMessageList.add(convMessage);
				}
				sendMultiMessages(multipleMessageList, arrayList, null, false);
			}
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid JSON Array", e);
		}
		presentIntent.removeExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
		onDoneClickPickContact();
	}

	private ConvMessage getConvMessageForForwardedWebContent(JSONObject msgExtrasJson) throws JSONException
	{
		//Web content message
		String metadata = msgExtrasJson.optString(HikeConstants.METADATA);
		ConvMessage convMessage = new ConvMessage();
		convMessage.setIsSent(true);
		convMessage.setParticipantInfoState(ConvMessage.ParticipantInfoState.NO_INFO);
		convMessage.setMessageType(MESSAGE_TYPE.FORWARD_WEB_CONTENT);
		convMessage.webMetadata = new WebMetadata(PlatformContent.getForwardCardData(metadata));
		convMessage.setPlatformData(msgExtrasJson.optJSONObject(HikeConstants.PLATFORM_PACKET));
		convMessage.setMessage(msgExtrasJson.getString(HikeConstants.HIKE_MESSAGE));
		if (msgExtrasJson.has(HikePlatformConstants.NAMESPACE))
		{
			convMessage.setNameSpace(msgExtrasJson.getString(HikePlatformConstants.NAMESPACE));
		}
		return convMessage;
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
			HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, platformAnalyticsJson);
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
		mPubSub.publish(HikePubSub.UPDATE_THREAD, convMessage);
		
	}
	

	@Override
	public void onEventReceived(String type, Object object)
	{
		super.onEventReceived(type, object);

		if (HikePubSub.MULTI_FILE_TASK_FINISHED.equals(type))
		{
			final Intent intent = (Intent) object;

			fileTransferTask = null;

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
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
			Pair<Boolean, Byte> ret = (Pair<Boolean, Byte>) object;
			final byte contactSyncResult = ret.second;
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					// Dont repopulate list if no sync changes
					if (contactSyncResult == ContactManager.SYNC_CONTACTS_CHANGED)
						adapter.executeFetchTask();
					showProgressBarContactsSync(View.GONE);
				}

			});
		}

		else if (HikePubSub.BOT_CREATED.equals(type))
		{
            if (object instanceof Pair)
            {
                BotInfo botInfo = (BotInfo)(((Pair) object).first);
                Boolean isBotCreationSuccess = (Boolean) (((Pair) object).second);
				if (adapter != null && isBotCreationSuccess)
				{
					adapter.onBotCreated(botInfo);
				}
			}
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
			if (existingGroupOrBroadcastId != null || createGroup || createBroadcast || addToConference)
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
		}else if(composeMode == PICK_CONTACT_MODE || composeMode == PICK_CONTACT_AND_SEND_MODE)
		{
			setResult(RESULT_CANCELED,getIntent());
			this.finish();
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
				else
				{
					setupForSelectAll();
					if(getIntent().getBooleanExtra(HikeConstants.Extras.SELECT_ALL_INITIALLY, false))
					{
						View selectAllCont = findViewById(R.id.select_all_container);
						CheckBox cb = (CheckBox) selectAllCont.findViewById(R.id.select_all_cb);
						cb.setChecked(true);
						selectAllMode=true;
						getIntent().putExtra(HikeConstants.Extras.SELECT_ALL_INITIALLY, false);
					}
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
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		adapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES_SMALL && scrollState == OnScrollListener.SCROLL_STATE_FLING);
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
		return initialiseFileTransfer(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, isForwardingFile, arrayList,null);
	}

	private FileTransferData initialiseFileTransfer(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
			boolean isForwardingFile, ArrayList<ContactInfo> arrayList, String caption)
	{
		clearTempData();
		if (filePath == null)
		{
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_2_4, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "Compose - InitialiseFileTransfer - File path is null.");
			return null;
		}
		File file = new File(filePath);
		Logger.d(getClass().getSimpleName(), "File size: " + file.length() + " File name: " + file.getName());

//		if (HikeConstants.MAX_FILE_SIZE != -1 && HikeConstants.MAX_FILE_SIZE < file.length())
//		{
//			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_1_4, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "Compose - InitialiseFileTransfer - Max size reached.");
//			Toast.makeText(getApplicationContext(), R.string.max_file_size, Toast.LENGTH_SHORT).show();
//			return null;
//		}
		return new FileTransferData(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, isForwardingFile, arrayList, file, caption);
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
		boolean newConvIfnotExist = false;
		if(arrayList.size()==1){
			newConvIfnotExist = true;
		}
		for(ContactInfo contactInfo:arrayList)
		{
			FTMessageBuilder.Builder msgBuilder = new FTMessageBuilder.Builder()
			.setMsisdn(contactInfo.getMsisdn())
			.setLatitude(latitude)
			.setLongitude(longitude)
			.setZoomLevel(zoomLevel)
			.setRecipientOnHike(((ContactInfo)arrayList.get(0)).isOnhike())
			.setNewConvIfnotExist(newConvIfnotExist)
			.setHikeFileType(HikeFileType.LOCATION);
			msgBuilder.build();
		}
	}
	private void initialiseContactTransfer(JSONObject contactJson, ArrayList<ContactInfo>arrayList )
	{
		boolean newConvIfnotExist = false;
		if(arrayList.size()==1){
			newConvIfnotExist = true;
		}
		
		prefileTransferTask = new PreFileTransferAsycntask(arrayList,null,
				contactJson, newConvIfnotExist,CONTACT_TRANSFER);
		prefileTransferTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

	public static class FileTransferData {
        public String filePath, fileKey, fileType;
        public HikeFileType hikeFileType;
        public boolean isRecording, isForwardingFile;
        public long recordingDuration;
        public ArrayList<ContactInfo> arrayList;
        public File file;
        public String caption;

        public FileTransferData(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
                                boolean isForwardingFile, ArrayList<ContactInfo> arrayList, File file) {
            this(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, isForwardingFile, arrayList, file, null);
        }

        public FileTransferData(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
                                boolean isForwardingFile, ArrayList<ContactInfo> arrayList, File file, String caption) {
            this.filePath = filePath;
            this.fileKey = fileKey;
            this.hikeFileType = hikeFileType;
            this.fileType = fileType;
            this.isRecording = isRecording;
            this.recordingDuration = recordingDuration;
            this.arrayList = arrayList;
            this.file = file;
            this.caption = caption;
        }
    }

    @Override
    public void onCallBack(JSONArray array) {
        Intent intent = getIntent();
        intent.putExtra(HikeConstants.HIKE_CONTACT_PICKER_RESULT, array == null ? "" : array.toString());
        intent.putExtra(HikeConstants.Extras.FUNCTION_ID,getIntent().getStringExtra(HikeConstants.Extras.FUNCTION_ID));
        setResult(RESULT_OK, intent);
        ComposeChatActivity.this.finish();
    }


	private class PreFileTransferAsycntask extends AsyncTask<Void, Void, Void>{
		Object arrayList;
		Intent intent;
		private JSONObject contactJson;
		private boolean newConvIfnotExist;
		private int fileType;
		
		PreFileTransferAsycntask(Object arrayList,Intent intent,JSONObject contactJson, boolean newConvIfnotExist,int fileType){
			this.arrayList = arrayList;
			this.intent = intent;
			this.contactJson = contactJson;
			this.newConvIfnotExist = newConvIfnotExist;
			this.fileType = fileType;
					
		}


	
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			String message = getResources().getString(R.string.multi_file_creation);
			if(fileType == CONTACT_TRANSFER){
				message =getResources().getString(R.string.multi_contact_creation);
			}
			progressDialog = ProgressDialog.show(ComposeChatActivity.this, null, message);
		}
		@Override
		protected Void doInBackground(Void... params)
		{
			if(arrayList!=null){
				if(fileType == FILE_TRANSFER)
				{
					ArrayList<FileTransferData> files = (ArrayList<FileTransferData>)arrayList;
			        for(FileTransferData file:files)
			        {
			        	Logger.d("ComposeChatActivity", "PreFTAsyncTask : isCloudMedia = " + Utils.isPicasaUri(file.filePath));
						FTMessageBuilder.Builder mBuilder = new FTMessageBuilder.Builder()
								.setContactList(file.arrayList)
								.setSourceFile(file.file)
								.setFileKey(file.fileKey)
								.setFileType(file.fileType)
								.setHikeFileType(file.hikeFileType)
								.setRec(file.isRecording)
								.setForwardMsg(file.isForwardingFile)
								.setRecipientOnHike(((ContactInfo) file.arrayList.get(0)).isOnhike())
								.setRecordingDuration(file.recordingDuration)
								.setAttachement(FTAnalyticEvents.OTHER_ATTACHEMENT)
								.setCaption(file.caption);
						mBuilder.build();
			        }
				}else if(fileType == CONTACT_TRANSFER)
				{
					ArrayList<ContactInfo> contactList = (ArrayList<ContactInfo>)arrayList;	
       			    for(ContactInfo contactInfo:contactList)
       			    {
						FTMessageBuilder.Builder msgBuilder = new FTMessageBuilder.Builder()
								.setMsisdn(contactInfo.getMsisdn())
								.setContactJson(contactJson)
								.setRecipientOnHike(contactInfo.isOnhike())
								.setNewConvIfnotExist(newConvIfnotExist)
								.setHikeFileType(HikeFileType.CONTACT);
						msgBuilder.build();
       			    }
				}
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
			if (intent != null) {
				startActivity(intent);
				finish();
			}
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
	private void toggleTag(String text, String uniqueness,Object data)
	{
		Tag tag = new Tag(text,uniqueness,data);
		tagEditText.toggleTag(tag);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU) 
		{
			if (mainMenu != null)
			{
				mainMenu.performIdentifierAction(R.id.overflow_menu, 0);
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	

	private void initSearchMenu(Menu menu)
	{
		searchMenuItem = menu.findItem(R.id.search);
		if (searchMenuItem != null)
		{
			searchMenuItem.setVisible(true);

			SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
			searchView.setOnQueryTextListener(onQueryTextListener);
			searchView.setQueryHint(getString(R.string.search));

			MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener()
			{

				@Override
				public boolean onMenuItemActionCollapse(MenuItem arg0)
				{
					if (adapter != null)
					{
						adapter.setSearchModeOn(false);
						adapter.refreshBots();
						adapter.removeFilter();
					}

					return true;
				}

				@Override
				public boolean onMenuItemActionExpand(MenuItem arg0)
				{
					if (adapter != null)
					{
						adapter.setSearchModeOn(true);
					}
					return true;
				}
				
			});

		}
	}

	private void sendAnalyticsMultiSelSend()
	{
		//Execute on worker thread
		ArrayList<ContactInfo> recepientList = adapter.getAllSelectedContacts();

		int timeline_sel = 0;
		int contact_sel = 0;

		boolean isShareSend = isSharingFile;

		for(ContactInfo cInfo : recepientList)
		{
			if(cInfo instanceof HikeFeatureInfo)
			{
				timeline_sel++;
				break;
			}
		}

		contact_sel = Math.max(0,recepientList.size() - timeline_sel);

		if(recepientList.size() == 1 && (imagesToShare.size() == 1 || !TextUtils.isEmpty(messageToShare)))
		{
			//No need to show confirmation since we are opening statusupdate activity
			if(recepientList.get(0) instanceof HikeFeatureInfo)
			{
				return;
			}
		}

		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.EVENT_KEY, HikeConstants.LogEvent.MULSEL_SEND);
			json.put(HikeConstants.LogEvent.MULSEL_TIMELINE_SEL,timeline_sel);
			json.put(HikeConstants.LogEvent.MULSEL_CONTACT_SEL,contact_sel);
			json.put(HikeConstants.LogEvent.MULSEL_IS_SHARE,isShareSend);
			HikeAnalyticsEvent.analyticsForPhotos(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
}
