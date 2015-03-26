package com.bsb.hike.ui;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import com.bsb.hike.platform.CardComponent;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformMessageMetadata;

import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.productpopup.DialogPojo;
import com.bsb.hike.productpopup.HikeDialogFragment;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.productpopup.ProductPopupsConstants.PopupTriggerPoints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.EmoticonType;
import com.bsb.hike.HikeConstants.MESSAGE_TYPE;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.adapters.AccountAdapter;
import com.bsb.hike.adapters.EmoticonAdapter;
import com.bsb.hike.adapters.EmoticonPageAdapter.EmoticonClickListener;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.adapters.StickerAdapter;
import com.bsb.hike.adapters.UpdateAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeMqttPersistence;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.AccountData;
import com.bsb.hike.models.BroadcastConversation;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ContactInfoData.DataType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.Conversation.MetaData;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.OverFlowMenuItem;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.lastseenmgr.FetchLastSeenTask;
import com.bsb.hike.platform.CardComponent;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformMessageMetadata;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.tasks.EmailConversationsAsyncTask;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.ui.utils.HashSpanWatcher;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.ContactDialog;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.EmoticonTextWatcher;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.HikeTip;
import com.bsb.hike.utils.HikeTip.TipType;
import com.bsb.hike.utils.LastSeenScheduler;
import com.bsb.hike.utils.LastSeenScheduler.LastSeenFetchedCallback;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.SoundUtils;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.CustomFontEditText;
import com.bsb.hike.view.CustomFontEditText.BackKeyListener;
import com.bsb.hike.view.CustomLinearLayout;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator;
import com.bsb.hike.voip.VoIPUtils;
import com.googlecode.mp4parser.boxes.dece.ContentInformationBox.BrandEntry;

public class ChatThread extends HikeAppStateBaseFragmentActivity implements HikePubSub.Listener, TextWatcher, OnEditorActionListener, OnSoftKeyboardListener, View.OnKeyListener,
		FinishableEvent, OnTouchListener, OnScrollListener, OnItemLongClickListener, BackKeyListener, EmoticonClickListener
{
	private static final String HASH_PIN = "#pin";

	private boolean screenOffEvent;

	private boolean activityVisible = true;

	private enum DialogShowing
	{
		SMS_SYNC_CONFIRMATION_DIALOG, SMS_SYNCING_DIALOG
	}

	private Bundle savedInstanceState;

	private HikePubSub mPubSub;

	private HikeConversationsDatabase mConversationDb;

	private String mContactName;

	private String mContactNumber;

	private MessagesAdapter mAdapter;

	private Conversation mConversation;

	private FavoriteType favoriteType;

	private ComposeViewWatcher mComposeViewWatcher;

	/* View element */
	private ImageButton mSendBtn;

	private int mCredits;

	private TextView mMetadataNumChars;

	private View mBottomView;

	private CustomFontEditText mComposeView;

	private ListView mConversationsView;

	int mMaxSmsLength = 140;

	private String mLabel;

	/* notifies that the adapter has been updated */
	public Runnable mUpdateAdapter;

	private TextView mLastSeenView;

	private boolean mUserIsBlocked;

	private View mOverlayLayout;

	private ArrayList<ConvMessage> messages;

	private static HashMap<Long, ConvMessage> messageMap;

	private CustomLinearLayout chatLayout;

	private Handler mHandler;

	private boolean blockOverlay;

	private Configuration config;

	private SharedPreferences prefs;

	private Animation slideUp;

	private Animation slideDown;

	private TextView smsCount;

	private boolean animatedOnce = false;

	private boolean isOverlayShowing = false, isKeyboardOpen;

	private ViewPager emoticonViewPager;

	private ViewGroup emoticonLayout;

	private GroupParticipant myInfo;

	private static File selectedFile;

	private Dialog filePickerDialog;

	private static MediaRecorder recorder;

	private static MediaPlayer player;

	private Handler recordingHandler;

	private UpdateRecordingDuration updateRecordingDuration;

	private Dialog recordingDialog;

	private RecorderState recorderState;

	private boolean showKeyboard = false;

	private boolean isOnline = false;

	private View hikeToOfflineTipview;
	
	private TextView topUnreadPinsIndicator;
	
	private ViewStub pulsatingDot;
	
	private View pulsatingDotInflated;

	private int HIKE_TO_OFFLINE_TIP_STATE_1 = 1;

	private int HIKE_TO_OFFLINE_TIP_STATE_2 = 2;

	private int HIKE_TO_OFFLINE_TIP_STATE_3 = 3;

	private int currentCreditsForToast = 0;

	/*
	 * We should run client timer before showing hikeOffline tip only if user is entering chat thread and reciever's online state changes while user is on chatthread
	 */
	public boolean shouldRunTimerForHikeOfflineTip = true;

	private ContactInfo contactInfo;

	private StickerEmoticonIconPageIndicator iconPageIndicator;

	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED, HikePubSub.TYPING_CONVERSATION, HikePubSub.END_TYPING_CONVERSATION, HikePubSub.SMS_CREDIT_CHANGED,
			HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.MESSAGE_DELIVERED, HikePubSub.SERVER_RECEIVED_MSG,HikePubSub.SERVER_RECEIVED_MULTI_MSG, HikePubSub.MESSAGE_FAILED, HikePubSub.ICON_CHANGED,
			HikePubSub.USER_JOINED, HikePubSub.USER_LEFT, HikePubSub.GROUP_NAME_CHANGED, HikePubSub.GROUP_END, HikePubSub.CONTACT_ADDED, HikePubSub.UPLOAD_FINISHED,
			HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, HikePubSub.FILE_MESSAGE_CREATED, HikePubSub.MUTE_CONVERSATION_TOGGLED, HikePubSub.BLOCK_USER, HikePubSub.UNBLOCK_USER,
			HikePubSub.DELETE_MESSAGE, HikePubSub.GROUP_REVIVED, HikePubSub.CHANGED_MESSAGE_TYPE, HikePubSub.SHOW_SMS_SYNC_DIALOG, HikePubSub.SMS_SYNC_COMPLETE,
			HikePubSub.SMS_SYNC_FAIL, HikePubSub.SMS_SYNC_START, HikePubSub.STICKER_DOWNLOADED, HikePubSub.LAST_SEEN_TIME_UPDATED, HikePubSub.SEND_SMS_PREF_TOGGLED,
			HikePubSub.PARTICIPANT_JOINED_GROUP, HikePubSub.PARTICIPANT_LEFT_GROUP, HikePubSub.STICKER_CATEGORY_DOWNLOADED, HikePubSub.STICKER_CATEGORY_DOWNLOAD_FAILED,
			HikePubSub.LAST_SEEN_TIME_UPDATED, HikePubSub.SEND_SMS_PREF_TOGGLED, HikePubSub.PARTICIPANT_JOINED_GROUP, HikePubSub.PARTICIPANT_LEFT_GROUP,
			HikePubSub.CHAT_BACKGROUND_CHANGED, HikePubSub.UPDATE_NETWORK_STATE, HikePubSub.CLOSE_CURRENT_STEALTH_CHAT, HikePubSub.APP_FOREGROUNDED, HikePubSub.BULK_MESSAGE_RECEIVED, 
			HikePubSub.GROUP_MESSAGE_DELIVERED_READ, HikePubSub.BULK_MESSAGE_DELIVERED_READ, HikePubSub.UPDATE_PIN_METADATA,HikePubSub.ClOSE_PHOTO_VIEWER_FRAGMENT,HikePubSub.CONV_META_DATA_UPDATED, 
			HikePubSub.LATEST_PIN_DELETED, HikePubSub.CONTACT_DELETED, HikePubSub.STICKER_CATEGORY_MAP_UPDATED, HikePubSub.STICKER_FTUE_TIP,HikePubSub.MULTI_MESSAGE_DB_INSERTED, HikePubSub.MUTE_BOT  };

	private EmoticonType emoticonType;

	private PagerAdapter emoticonsAdapter;

	private StickerAdapter stickerAdapter;

	private boolean wasOrientationChanged = false;

	private GestureDetector gestureDetector;

	private boolean loadingMoreMessages;

	private boolean reachedEnd;

	private ContactDialog contactDialog;

	private DialogShowing dialogShowing;

	private Dialog smsDialog;

	private Dialog nativeSmsDialog;

	private long recordStartTime;

	private long recordedTime = -1;

	private static boolean recording = false;

	/*
	 * Made this public so that its accessible to this activity's adapter
	 */
	public View tipView;

	private TextView mLabelView;

	private ImageView avatar;

	private PopupWindow attachmentWindow;

	private Menu mMenu;

	private ChatTheme selectedTheme;

	private ChatTheme temporaryTheme;

	private boolean showingChatThemePicker;

	private boolean showingImpMessagePinCreate;

	private ImageView backgroundImage;

	private int selectedNonTextMsgs = 0;

	private int selectedNonForwadableMsgs = 0, shareableMessagesCount;

	private int selectedCancelableMsgs = 0;

	private boolean isActionModeOn = false;

	private boolean isHikeToOfflineMode = false;

	private TextView mActionModeTitle;

	private View unreadMessageIndicator;

	private int unreadMessageCount = 0;

	private View bottomFastScrollIndicator;

	private View upFastScrollIndicator;

	private LastSeenScheduler lastSeenScheduler;

	int currentFirstVisibleItem = Integer.MAX_VALUE;

	private HashMap<Integer, Boolean> mOptionsList = new HashMap<Integer, Boolean>();

	private MessageReceiver mMessageReceiver;

	private ChatThreadReceiver chatThreadReceiver;

	private ScreenOffReceiver screenOffBR;

	private HashSpanWatcher hashWatcher;
	
	private RequestToken lastSeenRequestToken;

	@Override
	protected void onPause()
	{
		if (mAdapter != null)
		{
			// mAdapter.getStickerLoader().setPauseWork(false);
			// mAdapter.getStickerLoader().setExitTasksEarly(true);
			mAdapter.getIconImageLoader().setExitTasksEarly(true);
			mAdapter.getHighQualityThumbLoader().setExitTasksEarly(true);
			
		}
		if(stickerAdapter != null)
		{
			stickerAdapter.getStickerLoader().setExitTasksEarly(true);
			stickerAdapter.getStickerOtherIconLoader().setExitTasksEarly(true);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
		activityVisible = false;
		
		//Logging ChatThread Screen closing for bot case
		if(HikeMessengerApp.getInstance().isHikeBotNumber(mContactNumber))
		{
			HAManager.getInstance().endChatSession(mContactNumber);
		}
		super.onPause();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		setMessagesRead();
	}

	@Override
	protected void onResume()
	{
		activityVisible = true;
		super.onResume();
		if (mAdapter != null)
		{
			// mAdapter.getStickerLoader().setExitTasksEarly(false);
			mAdapter.getIconImageLoader().setExitTasksEarly(false);
			mAdapter.getHighQualityThumbLoader().setExitTasksEarly(false);
			mAdapter.notifyDataSetChanged();
		}
		if(stickerAdapter != null)
		{
			stickerAdapter.getStickerLoader().setExitTasksEarly(false);
			stickerAdapter.getStickerOtherIconLoader().setExitTasksEarly(false);
			stickerAdapter.notifyDataSetChanged();
		}
		/* mark any messages unread as read */
		setMessagesRead();

		/* clear any pending notifications */
		/* clear any toast notifications */
		if (mConversation != null)
		{
			NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			mgr.cancel((int) mConversation.getMsisdn().hashCode());
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.init();

			/* check if the send button should be enabled */
			mComposeViewWatcher.setBtnEnabled();
			mComposeView.requestFocus();
		}
		if(isShowingPin())
		{
			   decrementUnreadPInCount();
		}
		updateOverflowMenuUnreadCount();
		
		//Logging ChatThread Screen opening for bot case
		if(HikeMessengerApp.getInstance().isHikeBotNumber(mContactNumber))
		{
			HAManager.getInstance().startChatSession(mContactNumber);
		}
	}

	private void showPopUpIfRequired()
	{
		if (savedInstanceState != null)
		{
			if (savedInstanceState.getBoolean(HikeConstants.Extras.FILE_TRANSFER_DIALOG_SHOWING))
			{
				showFilePicker(Utils.getExternalStorageState());
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.RECORDER_DIALOG_SHOWING))
			{
				recordStartTime = savedInstanceState.getLong(HikeConstants.Extras.RECORDER_START_TIME);
				recordedTime = savedInstanceState.getLong(HikeConstants.Extras.RECORDED_TIME);

				showRecordingDialog();
			}

			int dialogShowingOrdinal = savedInstanceState.getInt(HikeConstants.Extras.DIALOG_SHOWING, -1);
			if (dialogShowingOrdinal != -1)
			{
				dialogShowing = DialogShowing.values()[dialogShowingOrdinal];
				smsDialog = Utils.showSMSSyncDialog(this, dialogShowing == DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG);
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.CHAT_THEME_WINDOW_OPEN, false))
			{
				final ChatTheme chatTheme = ChatTheme.values()[savedInstanceState.getInt(HikeConstants.Extras.SELECTED_THEME, 0)];
				/*
				 * We need to queue this piece of code after the lifecycle methods are done. Else the popup window throws an exception.
				 */
				chatLayout.post(new Runnable()
				{

					@Override
					public void run()
					{
						setupThemePicker(chatTheme);
						Logger.d("ChatThread", "Calling setchattheme from showPopupIfRequired");
						setChatTheme(chatTheme);
					}
				});
			}
			isActionModeOn = savedInstanceState.getBoolean(HikeConstants.Extras.IS_ACTION_MODE_ON, false);
			if (isActionModeOn)
			{
				long[] selectedPositions = savedInstanceState.getLongArray(HikeConstants.Extras.SELECTED_POSITIONS);
				mAdapter.setPositionsSelected(selectedPositions);
				selectedNonForwadableMsgs = savedInstanceState.getInt(HikeConstants.Extras.SELECTED_NON_FORWARDABLE_MSGS);
				selectedNonTextMsgs = savedInstanceState.getInt(HikeConstants.Extras.SELECTED_NON_TEXT_MSGS);
				selectedCancelableMsgs = savedInstanceState.getInt(HikeConstants.Extras.SELECTED_CANCELABLE_MSGS);
				shareableMessagesCount = savedInstanceState.getInt(HikeConstants.Extras.SELECTED_SHARABLE_MSGS_COUNT);

				setupActionModeActionBar();
				invalidateOptionsMenu();
			}
		}
	}

	private void unregisterReceivers()
	{
		if (mMessageReceiver != null)
		{
			LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		}
		if (chatThreadReceiver != null)
		{
			LocalBroadcastManager.getInstance(this).unregisterReceiver(chatThreadReceiver);
		}
		if (screenOffBR != null)
		{
			unregisterReceiver(screenOffBR);
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		possibleKeyboardHeight = 0;
		unregisterReceivers();

		if (prefs != null && !prefs.getBoolean(HikeMessengerApp.SHOWN_SDR_INTRO_TIP, false) && mAdapter != null && mAdapter.shownSdrToolTip())
		{
			Editor editor = prefs.edit();
			editor.putBoolean(HikeMessengerApp.SHOWN_SDR_INTRO_TIP, true);
			editor.commit();
		}
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		if (stickerAdapter != null)
		{	
			stickerAdapter.unregisterListeners();
		}

		if (mComposeViewWatcher != null)
		{
			// If we didn't send an end typing. We should send one before
			// exiting
			if (!mComposeViewWatcher.wasEndTypingSent())
			{
				mComposeViewWatcher.sendEndTyping();
			}
			mComposeViewWatcher.uninit();
			mComposeViewWatcher = null;
		}
		if (contactDialog != null)
		{
			contactDialog.dismiss();
			contactDialog = null;
		}
		if (smsDialog != null)
		{
			smsDialog.cancel();
			smsDialog = null;
		}
		if (nativeSmsDialog != null)
		{
			nativeSmsDialog.cancel();
			nativeSmsDialog = null;
		}
		if (mAdapter != null)
		{
			mAdapter.onDestroy();
			mAdapter.resetPlayerIfRunning();
		}
		if (attachmentWindow != null && attachmentWindow.getContentView() == emoticonLayout)
		{
			Utils.hideSoftKeyboard(this, mComposeView);
		}
		if (attachmentWindow != null && attachmentWindow.isShowing())
		{
			attachmentWindow.dismiss();
			attachmentWindow = null;
		}
 
		if (Utils.isOkHttp())
		{
			cancelFetchLastseenTask();
		}
		else
		{
			resetLastSeenScheduler();
		}
		
		StickerManager.getInstance().saveCustomCategories();
		if (messageMap != null)
		{
			messageMap.clear();
			messageMap = null;
		}

	}

	private void resetLastSeenScheduler()
	{
		if (lastSeenScheduler != null)
		{
			lastSeenScheduler.stop(false);
			lastSeenScheduler = null;
		}
	}

	private void cancelFetchLastseenTask()
	{
		if (lastSeenRequestToken != null)
		{
			lastSeenRequestToken.cancel();
		}
	}
	
	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		return getIntent();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		/*
		 * Making the action bar transparent for custom theming.
		 */
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
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

		setContentView(R.layout.chatthread);
		this.savedInstanceState = savedInstanceState;
		findViewById(R.id.chatThreadParentLayout).getViewTreeObserver().addOnGlobalLayoutListener(getGlobalLayoutListener());
		// we are getting the intent which has started our activity here.
		// we fetch the boolean extra to check if the keyboard has to be
		// expanded.

		Intent fromIntent = getIntent();
		if (fromIntent.getBooleanExtra(HikeConstants.Extras.SHOW_KEYBOARD, false))
			showKeyboard = true;

		mHandler = new Handler();

		prefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

		wasOrientationChanged = savedInstanceState != null;
		isOverlayShowing = savedInstanceState == null ? false : savedInstanceState.getBoolean(HikeConstants.Extras.OVERLAY_SHOWING);

		config = getResources().getConfiguration();
		StickerManager.getInstance().checkAndDownLoadStickerData();

		/* bind views to variables */

		chatLayout = (CustomLinearLayout) findViewById(R.id.chat_layout);
		backgroundImage = (ImageView) findViewById(R.id.background);
		mBottomView = findViewById(R.id.bottom_panel);
		mConversationsView = (ListView) findViewById(R.id.conversations_list);
		mComposeView = (CustomFontEditText) findViewById(R.id.msg_compose);
		mComposeView.setBackKeyListener(this);
		mSendBtn = (ImageButton) findViewById(R.id.send_message);
		mMetadataNumChars = (TextView) findViewById(R.id.sms_chat_metadata_num_chars);
		mOverlayLayout = findViewById(R.id.overlay_layout);
		unreadMessageIndicator = findViewById(R.id.new_message_indicator);
		unreadMessageIndicator.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mConversationsView.setSelection(messages.size() - unreadMessageCount - 1);
				hideUnreadCountIndicator();
			}
		});

		bottomFastScrollIndicator = findViewById(R.id.scroll_bottom_indicator);
		bottomFastScrollIndicator.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mConversationsView.setSelection(messages.size() - 1);
				hideFastScrollIndicator();
			}
		});

		upFastScrollIndicator = findViewById(R.id.scroll_top_indicator);
		((ImageView) upFastScrollIndicator.findViewById(R.id.indicator_img)).setVisibility(View.GONE);
		((ImageView) upFastScrollIndicator.findViewById(R.id.up_indicator_img)).setVisibility(View.VISIBLE);
		upFastScrollIndicator.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mConversationsView.setSelection(0);
				hideUpFastScrollIndicator();
			}
		});

		/*
		 * ensure that when the softkeyboard Done button is pressed (different than the sen button we have), we send the message.
		 */
		mComposeView.setOnEditorActionListener(this);

		/*
		 * Fix for android bug, where the focus is removed from the edittext when you have a layout with tabs (Emoticon layout) for hard keyboard devices
		 * http://code.google.com/p/android/issues/detail?id=2516
		 */
		if (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS)
		{
			mComposeView.setOnTouchListener(new OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					mComposeView.requestFocusFromTouch();
					return event == null;
				}
			});
		}

		/* ensure that when we hit Alt+Enter, we insert a newline */
		mComposeView.setOnKeyListener(this);

		mConversationDb = HikeConversationsDatabase.getInstance();

		chatLayout.setOnSoftKeyboardListener(this);
		mPubSub = HikeMessengerApp.getPubSub();
		/* register listeners */
		mPubSub.addListeners(this, pubSubListeners);

		if (prefs.contains(HikeMessengerApp.TEMP_NUM))
		{
			mContactName = prefs.getString(HikeMessengerApp.TEMP_NAME, null);
			mContactNumber = prefs.getString(HikeMessengerApp.TEMP_NUM, null);
			clearTempData();
			setIntentFromField();
			onNewIntent(getIntent());
		}
		else
		{
			Object o = getLastCustomNonConfigurationInstance();
			Intent intent = (o instanceof Intent) ? (Intent) o : getIntent();
			onNewIntent(intent);
		}

		if (savedInstanceState != null)
		{
			mHandler.post(new Runnable()
			{

				@Override
				public void run()
				{
					showPopUpIfRequired();
				}
			});
		}

		mHandler.post(new Runnable()
		{

			@Override
			public void run()
			{
				android.view.Window window = getWindow();
				if (window != null)
				{
					window.setBackgroundDrawableResource(R.color.black);
				}

			}
		});
		/* registering localbroadcast manager */
		mMessageReceiver = new MessageReceiver();
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED));

		chatThreadReceiver = new ChatThreadReceiver();
		IntentFilter intentFilter = new IntentFilter(StickerManager.STICKERS_UPDATED);
		intentFilter.addAction(StickerManager.MORE_STICKERS_DOWNLOADED);
		intentFilter.addAction(StickerManager.STICKERS_DOWNLOADED);
		LocalBroadcastManager.getInstance(this).registerReceiver(chatThreadReceiver, intentFilter);

		screenOffBR = new ScreenOffReceiver();
		registerReceiver(screenOffBR, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		final int whichPinEditShowing = savedInstanceState!=null ? savedInstanceState.getInt(HikeConstants.Extras.PIN_TYPE_SHOWING) : 0;
		if(whichPinEditShowing!=0){
			mHandler.post(new Runnable()
			{

				@Override
				public void run()
				{
					setupPinImpMessage(whichPinEditShowing);
				}
			});
			
		}
		Logger.i("chatthread", "on create end");
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.CHAT_SCR.ordinal());

	}
	
	private void startPulsatingDotAnimation()
	{
		new Handler().postDelayed(new Runnable()
		{

			@Override
			public void run()
			{
				ImageView ringView1 = (ImageView) findViewById(R.id.ring1);
				ringView1.startAnimation(HikeAnimationFactory.getPulsatingDotAnimation(0));
			}
		}, 0);

		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				ImageView ringView2 = (ImageView) findViewById(R.id.ring2);
				ringView2.startAnimation(HikeAnimationFactory.getPulsatingDotAnimation(0));
			}
		}, 1500);
	}
	
	private boolean showImpMessageIfRequired()
	{
		if (mConversation instanceof GroupConversation && mConversation.getMetaData() != null && mConversation.getMetaData().isShowLastPin(HikeConstants.MESSAGE_TYPE.TEXT_PIN))
		{
			ConvMessage impMessage = mConversationDb.getLastPinForConversation(mConversation);
			if (impMessage != null)
			{
				showImpMessage(impMessage, -1);
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param impMessage
	 *            -- ConvMessage to stick to top
	 * @param animationId
	 *            -- play animation on message , id must be anim resource id, -1 of no
	 */
	private void showImpMessage(ConvMessage impMessage, int animationId)
	{
		if (!prefs.getBoolean(HikeMessengerApp.SHOWN_PIN_TIP, false))
		{
		
		Editor editor = prefs.edit();
		editor.putBoolean(HikeMessengerApp.SHOWN_PIN_TIP, true);
		editor.commit();
		}
		
		if (tipView != null)
		{
			tipView.setVisibility(View.GONE);
		}
		if (impMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			tipView = LayoutInflater.from(this).inflate(R.layout.imp_message_text_pin, null);
		}

		if (tipView == null)
		{
			Logger.e("chatthread", "got imp message but type is unnknown , type " + impMessage.getMessageType());
			return;
		}
		TextView text = (TextView) tipView.findViewById(R.id.text);
		if (impMessage.getMetadata() != null && impMessage.getMetadata().isGhostMessage())
		{
			tipView.findViewById(R.id.main_content).setBackgroundResource(R.drawable.pin_bg_black);
			text.setTextColor(getResources().getColor(R.color.gray));
		}
		String name="";
			if(impMessage.isSent()){
				name="You: ";
			}else{
				if(mConversation instanceof GroupConversation){
				name = ((GroupConversation) mConversation).getGroupParticipantFirstNameAndSurname(impMessage.getGroupParticipantMsisdn()) + ": ";
				}
			}
		
		ForegroundColorSpan fSpan = new ForegroundColorSpan(getResources().getColor(R.color.pin_name_color));
		String str = name+impMessage.getMessage();
		SpannableString spanStr = new SpannableString(str);
		spanStr.setSpan(fSpan, 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		spanStr.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.pin_text_color)), name.length(), str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		CharSequence markedUp = spanStr;
		SmileyParser smileyParser = SmileyParser.getInstance();
		markedUp = smileyParser.addSmileySpans(markedUp, false);
		text.setText(markedUp);
		Linkify.addLinks(text, Linkify.ALL);
		Linkify.addLinks(text, Utils.shortCodeRegex, "tel:");
		text.setMovementMethod(new LinkMovementMethod()
		{
			@Override
			public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event)
			{
				// TODO Auto-generated method stub
				boolean result = super.onTouchEvent(widget, buffer, event);
				if (!result)
				{
					showPinHistory(false);
				}
				return result;
			}
		});
		// text.setText(spanStr);

		View cross = tipView.findViewById(R.id.cross);
		cross.setTag(impMessage);
		cross.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				hidePin();
				
			}
		});

		tipView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showPinHistory(false);
			}
		});
		LinearLayout ll = ((LinearLayout) findViewById(R.id.impMessageContainer));
		if (ll.getChildCount() > 0)
		{
			ll.removeAllViews();
		}
		ll.addView(tipView, 0);
		// to hide pin , if pin create view is visible
		if (findViewById(R.id.impMessageCreateView).getVisibility() == View.VISIBLE)
		{
			tipView.setVisibility(View.GONE);
		}
		if (animationId != -1 && !isShowingPin())
		{
			tipView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), animationId));
		}
		tipView.setTag(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
		//decrement the unread count if message pinned
		   
		     decrementUnreadPInCount();
	}
	
	
     public void decrementUnreadPInCount()
	{
		MetaData metadata = mConversation.getMetaData();
		if(!metadata.isPinDisplayed(HikeConstants.MESSAGE_TYPE.TEXT_PIN) && activityVisible)
		{
		try
		{
			metadata.setPinDisplayed(HikeConstants.MESSAGE_TYPE.TEXT_PIN, true);
			metadata.decrementUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mPubSub.publish(HikePubSub.UPDATE_PIN_METADATA,mConversation);
		}
	}
	private void hidePin(){
		hidePinFromUI(true);
		MetaData metadata = mConversation.getMetaData();
		try
		{
			metadata.setShowLastPin(HikeConstants.MESSAGE_TYPE.TEXT_PIN, false);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mPubSub.publish(HikePubSub.UPDATE_PIN_METADATA,mConversation);
	}
	
	private void playUpDownAnimation(final View view){
		if(view==null)
		{
			return;
		}
		Animation an = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.down_up_up_part);
		an.setAnimationListener(new AnimationListener()
		{
			
			@Override
			public void onAnimationStart(Animation animation)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				view.setVisibility(View.GONE);
				if(view==tipView){
					tipView = null;
				}
			}
		});
		view.startAnimation(an);
	}

	private void hidePinFromUI(boolean playAnim)
	{
		if (!isShowingPin())
		{
			return;
		}
		if (playAnim)
		{
			playUpDownAnimation(tipView);
		}
		else
		{
			tipView.setVisibility(View.GONE);
			tipView = null;
		}
	}

	private void showTipIfRequired()
	{
		if (isHikeOfflineTipShowing() || (tipView != null && tipView.getVisibility() == View.VISIBLE))
		{
			return;
		}
		HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		String key = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_CHAT, "");
		if (key.equals(HikeMessengerApp.ATOMIC_POP_UP_ATTACHMENT))
		{
			// show attachment
			View v = LayoutInflater.from(this).inflate(R.layout.tip_right_arrow, null);
			((ImageView) (v.findViewById(R.id.arrow_pointer))).setImageResource(R.drawable.ftue_up_arrow);
			setAtomicTipContent(v, pref);
			((LinearLayout) findViewById(R.id.tipContainerTop)).addView(v, 0);

		}
		else if (key.equals(HikeMessengerApp.ATOMIC_POP_UP_STICKER))
		{
			Logger.i("chatthread", "sticker tip");
			LinearLayout ll = ((LinearLayout) findViewById(R.id.tipContainerBottom));
			View v = LayoutInflater.from(this).inflate(R.layout.tip_left_arrow, null);
			((ImageView) (v.findViewById(R.id.arrow_pointer))).setImageResource(R.drawable.ftue_down_arrow);
			setAtomicTipContent(v, pref);
			ll.addView(v, 0);

		}
		else if (key.equals(HikeMessengerApp.ATOMIC_POP_UP_THEME) && !(mConversation instanceof GroupConversation))
		{
			Logger.i("chatthread", "theme tip");
			LinearLayout ll = ((LinearLayout) findViewById(R.id.tipContainerTop));
			View v = LayoutInflater.from(this).inflate(R.layout.tip_middle_arrow, null);
			((ImageView) (v.findViewById(R.id.arrow_pointer))).setImageResource(R.drawable.ftue_up_arrow);
			setAtomicTipContent(v, pref);
			ll.addView(v, 0);
		}
	}

	private void setAtomicTipContent(View view, final HikeSharedPreferenceUtil pref)
	{
		tipView = view;
		((TextView) view.findViewById(R.id.tip_header)).setText(pref.getData(HikeMessengerApp.ATOMIC_POP_UP_HEADER_CHAT, ""));
		((TextView) view.findViewById(R.id.tip_msg)).setText(pref.getData(HikeMessengerApp.ATOMIC_POP_UP_MESSAGE_CHAT, ""));
		view.findViewById(R.id.close_tip).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				tipView.setVisibility(View.GONE);
				pref.saveData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_CHAT, "");
			}
		});
	}

	private void clearTempData()
	{
		Editor editor = prefs.edit();
		editor.remove(HikeMessengerApp.TEMP_NAME);
		editor.remove(HikeMessengerApp.TEMP_NUM);
		editor.commit();
	}

	@Override
	public void onBackPressed()
	{
		if(removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG, true))
		{
			return;
		}
		if (findViewById(R.id.impMessageCreateView).getVisibility() == View.VISIBLE)
		{
			dismissPinCreateView(R.anim.down_up_up_part);
			return;
		}
		if (isActionModeOn)
		{
			destroyActionMode();
			return;
		}

		if (isHikeToOfflineMode)
		{
			destroyHikeToOfflineMode();
			return;
		}

		if (attachmentWindow != null && attachmentWindow.isShowing())
		{
			((View)findViewById(R.id.tb_layout)).findViewById(R.id.emo_btn).setSelected(false);
			findViewById(R.id.sticker_btn).setSelected(false);
			dismissPopupWindow();
			attachmentWindow = null;
			return;
		}

		if (isHikeOfflineTipShowing())
		{
			hideHikeToOfflineTip();
			return;
		}

		selectedFile = null;

		Intent intent = new Intent(this, HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

		super.onBackPressed();
	}

	private void saveDraft()
	{
		/*
		 * If the user had typed something, we save it as a draft and will show it in the text box when he comes back to this conversation.
		 */
		if (mComposeView != null && mComposeView.getVisibility() == View.VISIBLE)
		{
			Editor editor = getSharedPreferences(HikeConstants.DRAFT_SETTING, MODE_PRIVATE).edit();
			if (mComposeView.length() != 0)
			{
				editor.putString(mContactNumber, mComposeView.getText().toString());
			}
			else
			{
				editor.remove(mContactNumber);
			}
			editor.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (showingChatThemePicker || showingImpMessagePinCreate)
		{
			return false;
		}
		getSupportMenuInflater().inflate(isActionModeOn ? R.menu.multi_select_chat_menu : R.menu.chat_thread_menu, menu);
		mMenu = menu;
		
		if(!isActionModeOn)
		{
			topUnreadPinsIndicator = (TextView) menu.findItem(R.id.overflow_menu).getActionView().findViewById(R.id.top_bar_indicator);

			mMenu.findItem(R.id.overflow_menu).getActionView().setOnClickListener(new OnClickListener() 
			{			
				@Override
				public void onClick(View v) 
				{
					showOverFlowMenu();
				}
			});
		}
		// for group chat we show pin and one:one, we show theme
		if (Utils.isBroadcastConversation(mContactNumber))
		{
			menu.getItem(0).setVisible(false);
			menu.getItem(1).setVisible(false);
		}
		else if ( Utils.isGroupConversation(mContactNumber))
		{
			onCreatePinMenu( menu);
		}
		else
		{
			onCreateThemeMenu( menu);
		}
		return true;
	}

	private void onCreateThemeMenu(Menu menu)
	{
		if(!Utils.isVoipActivated(this) || (mConversation!=null && !mConversation.isOnhike()) || HikeMessengerApp.hikeBotNamesMap.containsKey(mContactNumber))
		{
			menu.getItem(0).setVisible(false);
		}
		menu.getItem(1).setVisible(false);
		if(tipView!=null && tipView.getVisibility()== View.VISIBLE && tipView.getTag() instanceof TipType && (TipType)tipView.getTag()==TipType.PIN)
		{
			tipView.setVisibility(View.GONE);
			tipView = null;
		}
	}

	private void onCreatePinMenu(Menu menu)
	{
		menu.getItem(0).setVisible(false);
		menu.getItem(1).setVisible(true);
		if (tipView == null)
		{
			if (mConversation!=null &&((GroupConversation) mConversation).getIsGroupAlive() && (!prefs.getBoolean(HikeMessengerApp.SHOWN_PIN_TIP, false)))
			{

				showPinFtueTip();
			}
		}
		updateOverflowMenuUnreadCount();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if (mConversation == null || showingChatThemePicker || showingImpMessagePinCreate)
		{
			return super.onPrepareOptionsMenu(menu);
		}
		if (isActionModeOn)
		{
			return onPrepareActionMode(menu);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (mConversation == null)
		{
			Logger.w("ChatThread", "OptionItem menu selected when conversation was null");
			return false;
		}

		if (mConversation instanceof GroupConversation)
		{
			if (!isActionModeOn && !((GroupConversation) mConversation).getIsGroupAlive())
			{
				return false;
			}
		}
		if (mUserIsBlocked)
		{
			return false;
		}
		if (!mConversation.isOnhike() && mCredits <= 0 && !isActionModeOn)
		{
			boolean nativeSmsPref = Utils.getSendSmsPref(this);
			if (!nativeSmsPref)
			{
				return false;
			}
		}

		if (isActionModeOn)
		{
			return onActionModeItemClicked(item);
		}

		if(isFragmentAdded(HikeConstants.IMAGE_FRAGMENT_TAG))
		{
			return false;
		}
		
		switch (item.getItemId())
		{
		case R.id.voip_call:
			Utils.onCallClicked(this, mContactNumber, VoIPUtils.CallSource.CHAT_THREAD);
			break;
		case R.id.attachment:
			showProductPopup(ProductPopupsConstants.PopupTriggerPoints.ATCH_SCR.ordinal());
			// hide pop up if any
			return attachmentClicked();
		case R.id.overflow_menu:
			Utils.hideSoftKeyboard(getApplicationContext(), mComposeView);
			showOverFlowMenu();
			break;
		case R.id.pin_imp:
			setupPinImpMessage(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
			break;
		}

		return true;
	}

	private boolean attachmentClicked()
	{
		resetAtomicPopUpKey(HikeMessengerApp.ATOMIC_POP_UP_ATTACHMENT);
		if (FileTransferManager.getInstance(this).remainingTransfers() == 0)
		{
			Toast.makeText(this, getString(R.string.max_num_files_reached, FileTransferManager.getInstance(this).getTaskLimit()), Toast.LENGTH_SHORT).show();
			return false;
		}
		showFilePicker(Utils.getExternalStorageState());
		return true;
	}

	private void resetAtomicPopUpKey(String requiredkey)
	{
		HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance();
		String key = pref.getData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_CHAT, "");
		if (key.equals(requiredkey))
		{
			pref.saveData(HikeMessengerApp.ATOMIC_POP_UP_TYPE_CHAT, "");
			if (tipView != null)
			{
				tipView.setVisibility(View.GONE);
				tipView = null;
			}
		}
	}

	private void setupThemePicker(ChatTheme preSelectedTheme)
	{
		showThemePicker(preSelectedTheme);
		setupChatThemeActionBar();
		showingChatThemePicker = true;
		invalidateOptionsMenu();
		resetAtomicPopUpKey(HikeMessengerApp.ATOMIC_POP_UP_THEME);
	}

	private void dismissPopupWindow()
	{
		if (attachmentWindow != null)
		{
			if (showingImpMessagePinCreate)
			{
				View v = attachmentWindow.getContentView();
				v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out_animation));
			}
			attachmentWindow.dismiss();
		}
	}

	private void showOverFlowMenu()
	{
		if(this.getCurrentFocus() != null && this.getCurrentFocus() instanceof EditText)
		{
			Utils.hideSoftKeyboard(ChatThread.this, this.getCurrentFocus());
		}  //Hiding the soft keyboard when the keyboard was visible and overflow menu was pressed.

		ArrayList<OverFlowMenuItem> optionsList = new ArrayList<OverFlowMenuItem>();

		optionsList.add(new OverFlowMenuItem(getString((mConversation instanceof BroadcastConversation) ? R.string.broadcast_profile : 
			((mConversation instanceof GroupConversation) ? R.string.group_profile : R.string.view_profile)), 0));
		
		if (!(mConversation instanceof GroupConversation))
		{
			optionsList.add(new OverFlowMenuItem(getString(R.string.chat_theme), 1));
			
			if (!mConversation.getMsisdn().equals(HikeConstants.FTUE_HIKE_DAILY) && !mConversation.getMsisdn().equals(HikeConstants.FTUE_TEAMHIKE_MSISDN))
			{
				if (mUserIsBlocked)
				{
					optionsList.add(new OverFlowMenuItem(getString(R.string.unblock_title), 6));
				}
				else
				{
					optionsList.add(new OverFlowMenuItem(getString(R.string.block_title), 6));
				}
			}
		}

		if (mConversation instanceof GroupConversation)
		{
			if (!(mConversation instanceof BroadcastConversation))
			{
				boolean isMuted = ((GroupConversation) mConversation).isMuted();
				optionsList.add(new OverFlowMenuItem(getString(isMuted ? R.string.unmute_group : R.string.mute_group), 2));
			}
		}

		if (mConversation.isBotConv()
				&& (!mConversation.getMsisdn().equals(HikeConstants.FTUE_HIKE_DAILY) && !mConversation.getMsisdn().equals(HikeConstants.FTUE_TEAMHIKE_MSISDN)))
		{
			boolean isMuted = mConversation.isMutedBotConv(false);

			optionsList.add(new OverFlowMenuItem(getString(isMuted ? R.string.unmute : R.string.mute), 2));
		}
		
		optionsList.add(new OverFlowMenuItem(getString(R.string.clear_chat), 5));
		if(messages.size() > 0)
		{
			optionsList.add(new OverFlowMenuItem(getString(R.string.email_chat), 3));
		}

		if (mConversation instanceof GroupConversation)
		{
			if (!(mConversation instanceof BroadcastConversation))
			{
				optionsList.add(new OverFlowMenuItem(getString(R.string.chat_theme_small), 4));
			}
		}

		if (mConversation instanceof BroadcastConversation)
		{
			optionsList.add(new OverFlowMenuItem(getString(R.string.add_shortcut), 8));
		}
		
		if (!(mConversation instanceof GroupConversation) && contactInfo.isOnhike() && (!mConversation.isBotConv()))
		{
			if (contactInfo.getFavoriteType() == FavoriteType.NOT_FRIEND || contactInfo.getFavoriteType() == FavoriteType.REQUEST_SENT_REJECTED
					|| contactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED)
			{
				optionsList.add(new OverFlowMenuItem(getString(R.string.add_as_favorite_menu), 7));
			}
		}

		dismissPopupWindow();

		attachmentWindow = new PopupWindow(this);

		View parentView = getLayoutInflater().inflate(R.layout.overflow_menu, chatLayout, false);

		attachmentWindow.setContentView(parentView);

		ListView overFlowListView = (ListView) parentView.findViewById(R.id.overflow_menu_list);
		overFlowListView.setAdapter(new ArrayAdapter<OverFlowMenuItem>(this, R.layout.over_flow_menu_item, R.id.item_title, optionsList)
		{

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

				TextView freeSmsCount = (TextView) convertView.findViewById(R.id.free_sms_count);
				freeSmsCount.setVisibility(View.GONE);

				TextView newGamesIndicator = (TextView) convertView.findViewById(R.id.new_games_indicator);
				newGamesIndicator.setVisibility(View.GONE);

				return convertView;
			}
		});

		OnItemClickListener onItemClickListener = new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				Logger.d(getClass().getSimpleName(), "Onclick: " + position);

				dismissPopupWindow();
				OverFlowMenuItem item = (OverFlowMenuItem) adapterView.getItemAtPosition(position);

				switch (item.getKey())
				{
				case 0:
					openProfileScreen();
                    if (mConversation.isBotConv())
                    {
                        analyticsForBots(HikePlatformConstants.BOT_VIEW_PROFILE, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
                    }
					break;
				case 1:
					setupThemePicker(null);
					break;
				case 2:
					if (mConversation.isBotConv())
					{
                        if(mConversation.isMutedBotConv(false))
                        {
                            mConversation.setBotConvMute(false);
                            analyticsForBots(HikePlatformConstants.BOT_UNMUTE_CHAT, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
                        }
                        else
                        {
                            mConversation.setBotConvMute(true);
                            analyticsForBots(HikePlatformConstants.BOT_MUTE_CHAT, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
                        }

						
						HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED,
								new Pair<String, Boolean>(mConversation.getMsisdn(), mConversation.isMutedBotConv(false)));

					}
					else
					{
						GroupConversation groupConversation = (GroupConversation) mConversation;

						groupConversation.setIsMuted(!groupConversation.isMuted());

						HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED,
								new Pair<String, Boolean>(groupConversation.getMsisdn(), groupConversation.isMuted()));
					}
					break;
				case 3:
					EmailConversationsAsyncTask emailTask = new EmailConversationsAsyncTask(ChatThread.this, null);
                    if (mConversation.isBotConv())
                    {
                        analyticsForBots(HikePlatformConstants.BOT_EMAIL_CONVERSATION, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
                    }
					Utils.executeConvAsyncTask(emailTask, mConversation);
					break;
				case 5:
					clearConversation();
                    if (mConversation.isBotConv())
                    {
                        analyticsForBots(HikePlatformConstants.BOT_CLEAR_CONVERSATION, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
                    }
					break;
				case 6:
					if(mUserIsBlocked)
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, mContactNumber);
                        analyticsForBots(HikePlatformConstants.BOT_UNBLOCK_CHAT, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
						unblockUser();
					}
					else
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, mContactNumber);
                        analyticsForBots(HikePlatformConstants.BOT_BLOCK_CHAT, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
					}
					break;
				case 7:
					FavoriteType favoriteType = FavoriteType.REQUEST_SENT;
					contactInfo.setFavoriteType(favoriteType);
					Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(contactInfo, favoriteType);
					HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED, favoriteToggle);
					break;
				case 4:
					setupThemePicker(null);
					break;
				case 8:
					Utils.createShortcut(ChatThread.this, mConversation);
					break;
				}

			}
		};

		setupPopupWindow(optionsList, onItemClickListener);
	}

    public void analyticsForBots(String key, String origin, String subType, JSONObject json)
    {
        if (json == null || json.length() == 0)
        {
            json = new JSONObject();
        }
        try
        {
            json.put(AnalyticsConstants.EVENT_KEY, key);
            json.put(AnalyticsConstants.ORIGIN, origin);
            json.put(AnalyticsConstants.CHAT_MSISDN, mContactNumber);
            HikeAnalyticsEvent.analyticsForBots(AnalyticsConstants.UI_EVENT, subType, json);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }


	private void clearConversation()
	{
		final CustomAlertDialog clearConfirmDialog = new CustomAlertDialog(ChatThread.this);
		clearConfirmDialog.setHeader(R.string.clear_conversation);
		clearConfirmDialog.setBody(R.string.confirm_clear_conversation);
		View.OnClickListener dialogOkClickListener = new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				mPubSub.publish(HikePubSub.CLEAR_CONVERSATION, mContactNumber);
				messages.clear();
				if (messageMap != null)
				{
					messageMap.clear();
				}
				mAdapter.notifyDataSetChanged();
				clearConfirmDialog.dismiss();
				hidePinFromUI(true);
				hideHikeToOfflineTip();

				Utils.resetPinUnreadCount(mConversation);
				updateOverflowMenuUnreadCount();
				if(mConversation instanceof GroupConversation){
				mPubSub.publish(HikePubSub.UPDATE_PIN_METADATA, mConversation);
				}
			}
		};

		clearConfirmDialog.setOkButton(R.string.ok, dialogOkClickListener);
		clearConfirmDialog.setCancelButton(R.string.cancel);
		clearConfirmDialog.show();
	}

	private void blockUser()
	{
		Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.MENU_BLOCK);
		mUserIsBlocked = true;
		showOverlay(true);
	}

	public void blockUser(View v)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, mContactNumber);
	}

	public void addToContacts(View v)
	{
		Utils.addToContacts(ChatThread.this, contactInfo.getMsisdn());
	}

	private void unblockUser()
	{
		mUserIsBlocked = false;
		mComposeView.setEnabled(true);
		hideOverlay();
	}

	public void onOverlayButtonClick(View v)
	{
		/* user clicked the unblock button in the chat-screen */
		if (v.getId() != R.id.overlay_layout && blockOverlay)
		{

			mPubSub.publish(HikePubSub.UNBLOCK_USER, getMsisdnMainUser());
            analyticsForBots(HikePlatformConstants.BOT_UNBLOCK_CHAT, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, null);
			unblockUser();
		}
		else if (v.getId() != R.id.overlay_layout)
		{
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.INVITE_OVERLAY_BUTTON);
			inviteUser();
			hideOverlay();
		}

		if (!blockOverlay)
		{
			hideOverlay();
			mConversationDb.setOverlay(true, mConversation.getMsisdn());
		}
	}

	private void hideOverlay()
	{
		if (mOverlayLayout.getVisibility() == View.VISIBLE && hasWindowFocus())
		{
			Animation fadeOut = AnimationUtils.loadAnimation(ChatThread.this, android.R.anim.fade_out);
			mOverlayLayout.setAnimation(fadeOut);
		}
		mOverlayLayout.setVisibility(View.INVISIBLE);
		if (mConversation instanceof GroupConversation)
		{
			mComposeView.setEnabled(true);
		}
		isOverlayShowing = false;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		return showMessageContextMenu(mAdapter.getItem(position - mConversationsView.getHeaderViewsCount()));
	}

	public boolean showMessageContextMenu(ConvMessage message)
	{
		if(showingImpMessagePinCreate)
		{
			return true;
		}
		dismissPopupWindow();
		if (message == null || message.getParticipantInfoState() != ParticipantInfoState.NO_INFO || message.getTypingNotification() != null || message.isBlockAddHeader())
		{
			return false;
		}
		if (message.getMessageType() == MESSAGE_TYPE.FORWARD_WEB_CONTENT || message.getMessageType() == MESSAGE_TYPE.WEB_CONTENT)
		{
			if (message.webMetadata.isLongPressDisabled())
			{
				return false;
			}
		}
		mAdapter.toggleSelection(message);
		boolean isMsgSelected = mAdapter.isSelected(message);

		boolean hasCheckedItems = mAdapter.getSelectedCount() > 0;

		if (hasCheckedItems && !isActionModeOn)
		{
			// there are some selected items, start the actionMode
			setupActionModeActionBar();
		}
		else if (!hasCheckedItems && isActionModeOn)
		{
			// there no selected items, finish the actionMode
			destroyActionMode();
			return true;
		}

		if (isActionModeOn)
		{
			setActionModeTitle(mAdapter.getSelectedCount());
		}

		if (message.isFileTransferMessage())
		{
			// File transfer message is a non text message
			selectedNonTextMsg(isMsgSelected);

			HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
			File file = hikeFile.getFile();
			FileSavedState fss;
			if (message.isSent())
			{
				fss = FileTransferManager.getInstance(getApplicationContext()).getUploadFileState(message.getMsgID(), file);
			}
			else
			{
				fss = FileTransferManager.getInstance(getApplicationContext()).getDownloadFileState(message.getMsgID(), file);
			}
			if ((message.isSent() && TextUtils.isEmpty(hikeFile.getFileKey())) || (!message.isSent() && !hikeFile.wasFileDownloaded()))
			{
				/*
				 * This message is not downloaded or uplpaded yet. this can't be forwarded
				 */
				if (message.isSent())
				{
					selectedNonForwadableMsg(isMsgSelected);
				}
				if ((fss.getFTState() == FTState.IN_PROGRESS || fss.getFTState() == FTState.PAUSED ))
				{
					/*
					 * File Transfer is in progress. this can be canceled.
					 */
					selectedCancelableMsg(isMsgSelected);
				}
			}
			else
			{
				HikeFileType ftype = hikeFile.getHikeFileType();
				// we donot support location and contact sharing
				if (ftype != HikeFileType.LOCATION && ftype != HikeFileType.CONTACT)
				{
					if (isMsgSelected)
					{
						shareableMessagesCount++;
					}
					else
					{
						shareableMessagesCount--;
					}
				}
			}
		}
		else if (message.getMetadata() != null && message.getMetadata().isPokeMessage())
		{
			// Poke message can only be deleted
			selectedNonTextMsg(isMsgSelected);
		}
		else if (message.isStickerMessage())
		{
			// Sticker message is a non text message.
			selectedNonTextMsg(isMsgSelected);
		}
        else if (message.getMessageType() == MESSAGE_TYPE.CONTENT )
        {
            // Content card is a non text message.
            selectedNonTextMsg(isMsgSelected);
        }if (message.getMessageType() == MESSAGE_TYPE.FORWARD_WEB_CONTENT || message.getMessageType() == MESSAGE_TYPE.WEB_CONTENT)
		{
			selectedNonTextMsg(isMsgSelected);
		}

		invalidateOptionsMenu();
		return true;
	}

	private void sendMessage(ConvMessage convMessage)
	{
		sendMessage(convMessage,false);
	}
	
	private void sendMessage(ConvMessage convMessage,boolean playPinAnim)
	{
		setSentTo(convMessage);
		addMessage(convMessage,playPinAnim);

		if(mConversation instanceof BroadcastConversation)
		{
			convMessage.setMessageOriginType(OriginType.BROADCAST);
		}
		mPubSub.publish(HikePubSub.MESSAGE_SENT, convMessage);
		if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			JSONObject metadata = new JSONObject();

			try
			{
				if (convMessage.getHashMessage()==HikeConstants.HASH_MESSAGE_TYPE.DEFAULT_MESSAGE)
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.PIN_POSTED_VIA_ICON);
				}
				else
				{
					metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.PIN_POSTED_VIA_HASH_PIN);
				}
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
		}
	}

	public void setSentTo(ConvMessage convMessage)
	{
		ArrayList<String> sentToList = new ArrayList<String>();
		if (mConversation instanceof GroupConversation)
		{
			sentToList.addAll(((GroupConversation) mConversation).getGroupParticipantList().keySet());
		}
		else
		{
			sentToList.add(mConversation.getMsisdn());
		}
		convMessage.setSentToMsisdnsList(sentToList);
	}
	
	public void onSendClick(View v)
	{
		if (!mConversation.isOnhike() && mCredits <= 0)
		{
			boolean nativeSmsPref = Utils.getSendSmsPref(this);
			if (!nativeSmsPref)
			{
				return;
			}
		}
		if (TextUtils.isEmpty(mComposeView.getText()))
		{
			if (mComposeView.getId() == R.id.msg_compose)
			{
				recordingDialogClicked();
			}
			return;
		}

		String message = mComposeView.getText().toString();


		ConvMessage convMessage = Utils.makeConvMessage(mContactNumber, message, isConversationOnHike());
		if (showingImpMessagePinCreate)
		{
			convMessage.setMessageType((Integer) v.getTag());
			Object metaData = v.getTag(R.id.message_info);
			if (metaData != null && metaData instanceof JSONObject)
			{
				try
				{
					JSONObject metaD = (JSONObject) metaData;
					convMessage.setMetadata(metaD);
					if(metaD.has(HikeConstants.PIN_MESSAGE))
					{
						int value = metaD.getInt(HikeConstants.PIN_MESSAGE);
						if(value==1)
						{
							convMessage.setHashMessage(HikeConstants.HASH_MESSAGE_TYPE.DEFAULT_MESSAGE);
						}
					}
					
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else
		{
			if (mConversation instanceof GroupConversation && !(mConversation instanceof BroadcastConversation))
			{
				if (!checkMessageTypeFromHash(convMessage)){
					return;
				}
			}
		}
		mComposeView.setText("");
		sendMessage(convMessage,!showingImpMessagePinCreate);

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.onMessageSent();
		}
	}

	private void recordingDialogClicked()
	{
		if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
		{
			Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
			return;
		}
		if (FileTransferManager.getInstance(this).remainingTransfers() == 0)
		{
			Toast.makeText(this, getString(R.string.max_num_files_reached, FileTransferManager.getInstance(this).getTaskLimit()), Toast.LENGTH_SHORT).show();
			return;
		}

		showRecordingDialog();
	}

	/*
	 * return true if all validation passes and it modifies message properly
	 */
	private boolean checkMessageTypeFromHash(ConvMessage convMessage)
	{
		Pattern p = Pattern.compile("(?i)" + HASH_PIN + ".*",Pattern.DOTALL);
		if (p.matcher(convMessage.getMessage()).matches())
		{
			convMessage.setMessage(convMessage.getMessage().substring(HASH_PIN.length()).trim());
			if (TextUtils.isEmpty(convMessage.getMessage()))
			{
				Toast.makeText(getApplicationContext(), "Text Can't be empty!", Toast.LENGTH_SHORT).show();
				return false;
			}
			convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
			JSONObject jsonObject = new JSONObject();
			try
			{
				jsonObject.put(HikeConstants.PIN_MESSAGE, 1);
				convMessage.setMetadata(jsonObject);
				convMessage.setHashMessage(HikeConstants.HASH_MESSAGE_TYPE.HASH_PIN_MESSAGE);
				return true;
			}
			catch (JSONException je)
			{
				Toast.makeText(getApplicationContext(), R.string.some_error, Toast.LENGTH_SHORT).show();
				je.printStackTrace();
			}
		}
		return true;
	}

	/*
	 * this function is called externally when our Activity is on top and the user selects an Intent for this same Activity
	 * 
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent)
	{
		Logger.d(getClass().getSimpleName(), "Intent: " + intent.toString());
		mComposeView.setText("");
		String prevContactNumber = null;

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.uninit();
			mComposeViewWatcher = null;
		}

		/* setIntent so getIntent returns the right values */
		setIntent(intent);

		Uri dataURI = intent.getData();

		if (mAdapter != null)
		{
			mAdapter.onDestroy();
			messages.clear();
			mAdapter.notifyDataSetChanged();
		}

		mConversation = null;

		if (intent.getBooleanExtra(HikeConstants.Extras.SHOW_KEYBOARD, false))
		{
			showKeyboard = true;
		}

		if(intent.getBooleanExtra(HikeConstants.Extras.SHOW_RECORDING_DIALOG, false))
		{
			recordingDialogClicked();
		}

		if ((dataURI != null) && ("smsto".equals(dataURI.getScheme()) || "sms".equals(dataURI.getScheme())))
		{
			// Intent received externally
			String phoneNumber = dataURI.getSchemeSpecificPart();
			
			/*
			 *  If phone number is empty or null finish activity and return 
			 */
			if(TextUtils.isEmpty(phoneNumber))
			{
				finish();
				return ;
			}
			// We were getting msisdns with spaces in them. Replacing all spaces
			// so that lookup is correct
			phoneNumber = phoneNumber.replaceAll(" ", "");
			/*
			 * Replacing all '-' that we get in the number
			 */
			phoneNumber = phoneNumber.replaceAll("-", "");
			Logger.d(getClass().getSimpleName(), "SMS To: " + phoneNumber);
			ContactInfo contactInfo = ContactManager.getInstance().getContactInfoFromPhoneNo(phoneNumber);
			/*
			 * phone lookup fails for a *lot* of people. If that happens, fall back to using their msisdn
			 */
			if (contactInfo != null)
			{
				mContactName = contactInfo.getName();
				prevContactNumber = mContactNumber;
				mContactNumber = contactInfo.getMsisdn();
				setIntentFromField();
			}
			else
			{
				prevContactNumber = mContactNumber;
				mContactName = mContactNumber = Utils.normalizeNumber(phoneNumber, prefs.getString(HikeMessengerApp.COUNTRY_CODE, HikeConstants.INDIA_COUNTRY_CODE));
			}

			if (!createConversation())
			{
				return;
			}
		}
		else if (intent.hasExtra(HikeConstants.Extras.MSISDN) && !intent.hasExtra(HikeConstants.Extras.GROUP_CHAT))
		{

			prevContactNumber = mContactNumber;
			// selected chat from conversation list
			mContactNumber = intent.getStringExtra(HikeConstants.Extras.MSISDN);
			mContactName = intent.getStringExtra(HikeConstants.Extras.NAME);

			if (!createConversation())
			{
				return;
			}
			if (intent.getBooleanExtra(HikeConstants.Extras.INVITE, false))
			{
				intent.removeExtra(HikeConstants.Extras.INVITE);
				inviteUser();
			}

			if (intent.hasExtra(HikeConstants.Extras.MSG))
			{
				String msg = intent.getStringExtra(HikeConstants.Extras.MSG);
				mComposeView.setText(msg);
				mComposeView.setSelection(mComposeView.length());
				SmileyParser.getInstance().addSmileyToEditable(mComposeView.getText(), false);
			}
			else if (intent.hasExtra(HikeConstants.Extras.CONTACT_ID))
			{
				String contactId = intent.getStringExtra(HikeConstants.Extras.CONTACT_ID);
				if (TextUtils.isEmpty(contactId))
				{
					Toast.makeText(getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
				}
				else
				{
					getContactData(contactId);
				}
			}
			else if (intent.hasExtra(HikeConstants.Extras.FILE_PATH))
			{

				String fileKey = null;
				if (intent.hasExtra(HikeConstants.Extras.FILE_KEY))
				{
					fileKey = intent.getStringExtra(HikeConstants.Extras.FILE_KEY);
				}
				String filePath = intent.getStringExtra(HikeConstants.Extras.FILE_PATH);
				String fileType = intent.getStringExtra(HikeConstants.Extras.FILE_TYPE);

				boolean isRecording = false;
				long recordingDuration = -1;
				if (intent.hasExtra(HikeConstants.Extras.RECORDING_TIME))
				{
					recordingDuration = intent.getLongExtra(HikeConstants.Extras.RECORDING_TIME, -1);
					isRecording = true;
					fileType = HikeConstants.VOICE_MESSAGE_CONTENT_TYPE;
				}

				if (filePath == null)
				{
					Toast.makeText(getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
				}
				else
				{
					int attachmentType = FTAnalyticEvents.OTHER_ATTACHEMENT;
					/*
					 * Added to know the attachment type when selected from file. 
					 */
					if(intent.hasExtra(FTAnalyticEvents.FT_ATTACHEMENT_TYPE))
						attachmentType = FTAnalyticEvents.FILE_ATTACHEMENT;
					initiateFileTransferFromIntentData(fileType, filePath, fileKey, isRecording, recordingDuration, attachmentType);
				}

				// Making sure the file does not get forwarded again on
				// orientation change.
				intent.removeExtra(HikeConstants.Extras.FILE_PATH);
			}

			else if (intent.hasExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT))
			{
				String jsonString = intent.getStringExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
				try
				{
					JSONArray multipleMsgFwdArray = new JSONArray(jsonString);
					int msgCount = multipleMsgFwdArray.length();
					for (int i = 0; i < msgCount; i++)
					{
						JSONObject msgExtrasJson = (JSONObject) multipleMsgFwdArray.get(i);
						if (msgExtrasJson.has(HikeConstants.Extras.MSG))
						{
							String msg = msgExtrasJson.getString(HikeConstants.Extras.MSG);
							ConvMessage convMessage = Utils.makeConvMessage(mContactNumber, msg, isConversationOnHike());
							sendMessage(convMessage);
						}else if(msgExtrasJson.has(HikeConstants.Extras.POKE)){
							// as we will be changing msisdn and hike status while inserting in DB
							ConvMessage convMessage = Utils.makeConvMessage(mContactNumber, getString(R.string.poke_msg), isConversationOnHike());
							JSONObject metadata = new JSONObject();
							try
							{
								metadata.put(HikeConstants.POKE, true);
								convMessage.setMetadata(metadata);
								sendMessage(convMessage);
							}
							catch (JSONException e)
							{
								Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
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

							if (Utils.isPicasaUri(filePath))
							{
								FileTransferManager.getInstance(getApplicationContext()).uploadFile(Uri.parse(filePath), hikeFileType, mContactNumber, mConversation.isOnhike());
							}
							else
							{
								initialiseFileTransfer(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, true, FTAnalyticEvents.OTHER_ATTACHEMENT);
							}
						}
						else if (msgExtrasJson.has(HikeConstants.Extras.LATITUDE) && msgExtrasJson.has(HikeConstants.Extras.LONGITUDE)
								&& msgExtrasJson.has(HikeConstants.Extras.ZOOM_LEVEL))
						{
							String fileKey = null;
							double latitude = msgExtrasJson.getDouble(HikeConstants.Extras.LATITUDE);
							double longitude = msgExtrasJson.getDouble(HikeConstants.Extras.LONGITUDE);
							int zoomLevel = msgExtrasJson.getInt(HikeConstants.Extras.ZOOM_LEVEL);
							initialiseLocationTransfer(latitude, longitude, zoomLevel);
						}
						else if (msgExtrasJson.has(HikeConstants.Extras.CONTACT_METADATA))
						{
							try
							{
								JSONObject contactJson = new JSONObject(msgExtrasJson.getString(HikeConstants.Extras.CONTACT_METADATA));
								initialiseContactTransfer(contactJson);
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
							sendSticker(sticker, categoryId, StickerManager.FROM_FORWARD);
							boolean isDis = sticker.isDisabled(sticker, this.getApplicationContext());
							// add this sticker to recents if this sticker is not disabled
							if (!isDis)
								StickerManager.getInstance().addRecentSticker(sticker);
							/*
							 * Making sure the sticker is not forwarded again on orientation change
							 */
							intent.removeExtra(StickerManager.FWD_CATEGORY_ID);
						}
                        else if(msgExtrasJson.optInt(MESSAGE_TYPE.MESSAGE_TYPE) == MESSAGE_TYPE.CONTENT){
                            // as we will be changing msisdn and hike status while inserting in DB
                            ConvMessage convMessage = Utils.makeConvMessage(mContactNumber, isConversationOnHike());
                            convMessage.setMessageType(MESSAGE_TYPE.CONTENT);
                            convMessage.platformMessageMetadata = new PlatformMessageMetadata(msgExtrasJson.optString(HikeConstants.METADATA), getApplicationContext());
                            convMessage.platformMessageMetadata.addThumbnailsToMetadata();
                            convMessage.setMessage(convMessage.platformMessageMetadata.notifText);

                            sendMessage(convMessage);

                        }
						else if(msgExtrasJson.optInt(MESSAGE_TYPE.MESSAGE_TYPE) == MESSAGE_TYPE.WEB_CONTENT || msgExtrasJson.optInt(MESSAGE_TYPE.MESSAGE_TYPE) == MESSAGE_TYPE.FORWARD_WEB_CONTENT){
							// as we will be changing msisdn and hike status while inserting in DB
							ConvMessage convMessage = Utils.makeConvMessage(mContactNumber,msgExtrasJson.getString(HikeConstants.HIKE_MESSAGE), isConversationOnHike());
							convMessage.setMessageType(MESSAGE_TYPE.FORWARD_WEB_CONTENT);
							convMessage.webMetadata = new WebMetadata(msgExtrasJson.optString(HikeConstants.METADATA));
							JSONObject json = new JSONObject();
							try
							{
								json.put(HikePlatformConstants.CARD_TYPE, convMessage.webMetadata.getAppName());
								json.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.CARD_FORWARD);
								json.put(AnalyticsConstants.TO, mContactNumber);
								json.put(AnalyticsConstants.CONTENT_ID, convMessage.getContentId());
								HikeAnalyticsEvent.analyticsForCards(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
							}
							catch (JSONException e)
							{
								e.printStackTrace();
							}
							catch (NullPointerException e)
							{
								e.printStackTrace();
							}
							sendMessage(convMessage);

						}

						/*
						 * Since the message was not forwarded, we check if we have any drafts saved for this conversation, if we do we enter it in the compose box.
						 */
					}
					if (isActionModeOn)
					{
						destroyActionMode();
					}
				}
				catch (JSONException e)
				{
					Logger.e(getClass().getSimpleName(), "Invalid JSON Array", e);
				}
				intent.removeExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
			}
			else if (intent.hasExtra(HikeConstants.Extras.FILE_PATHS))
			{
				ArrayList<String> filePaths = intent.getStringArrayListExtra(HikeConstants.Extras.FILE_PATHS);
				String fileType = intent.getStringExtra(HikeConstants.Extras.FILE_TYPE);
				for (String filePath : filePaths)
				{
					initiateFileTransferFromIntentData(fileType, filePath);
				}
				intent.removeExtra(HikeConstants.Extras.FILE_PATHS);
			}
			/*
			 * Since the message was not forwarded, we check if we have any drafts saved for this conversation, if we do we enter it in the compose box.
			 */
			else
			{
				String message = getSharedPreferences(HikeConstants.DRAFT_SETTING, MODE_PRIVATE).getString(mContactNumber, "");
				mComposeView.setText(message);
				mComposeView.setSelection(mComposeView.length());
				SmileyParser.getInstance().addSmileyToEditable(mComposeView.getText(), false);
			}
			intent.removeExtra(HikeConstants.Extras.FORWARD_MESSAGE);
		}
		/*
		 * close context menu(if open) if the previous MSISDN is different from the current one)
		 */
		if (prevContactNumber != null && !prevContactNumber.equalsIgnoreCase(mContactNumber))
		{
			Logger.w("ChatThread", "DIFFERENT MSISDN CLOSING CONTEXT MENU!!");
			closeContextMenu();
		}
		if(showingImpMessagePinCreate){
			dismissPinCreateView(-1);
		}
		if(isShowingPin()){
			hidePinFromUI(false);
		}
		invalidateOptionsMenu();
		// give priority to imp message , say pin message
		if (!showImpMessageIfRequired())
		{
			showTipIfRequired();
		}
	}

	private void initiateFileTransferFromIntentData(String fileType, String filePath)
	{
		initiateFileTransferFromIntentData(fileType, filePath, null, false, -1, FTAnalyticEvents.OTHER_ATTACHEMENT);
	}

	private void initiateFileTransferFromIntentData(String fileType, String filePath, String fileKey, boolean isRecording, long recordingDuration, int attachementType)
	{
		HikeFileType hikeFileType = HikeFileType.fromString(fileType, isRecording);

		Logger.d(getClass().getSimpleName(), "Forwarding file- Type:" + fileType + " Path: " + filePath);

		if (Utils.isPicasaUri(filePath))
		{
			FileTransferManager.getInstance(getApplicationContext()).uploadFile(Uri.parse(filePath), hikeFileType, mContactNumber, mConversation.isOnhike());
		}
		else
		{
			initialiseFileTransfer(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, true, attachementType);
		}

	}

	private void inviteUser()
	{
		if (mConversation == null)
		{
			return;
		}

		if (!mConversation.isOnhike())
		{
			Utils.sendInviteUtil(new ContactInfo(mContactNumber, mContactNumber, mContactName, mContactNumber), this, HikeConstants.SINGLE_INVITE_SMS_ALERT_CHECKED,
					getString(R.string.native_header), getString(R.string.native_info));
		}
		else if (mConversation instanceof GroupConversation)
		{
			startActivity(new Intent(ChatThread.this, HikeListActivity.class));
		}
		else
		{
			Toast toast = Toast.makeText(this, R.string.already_hike_user, Toast.LENGTH_LONG);
			toast.show();
		}
	}

	/*
	 * sets the intent for this screen based on the fields we've assigned. useful if the user has entered information or we've determined information that indicates the type of
	 * data on this screen.
	 */
	private void setIntentFromField()
	{
		Intent intent = new Intent();
		if (mContactName != null)
		{
			intent.putExtra(HikeConstants.Extras.NAME, mContactName);
		}

		if (!TextUtils.isEmpty(mContactNumber))
		{
			intent.putExtra(HikeConstants.Extras.MSISDN, mContactNumber);
		}

		setIntent(intent);
	}

	/**
	 * Renders the chats for a given user
	 * @return
	 * true if the conversation was created. False otherwise.
	 */
	private boolean createConversation()
	{
		/*
		 * Fix for forward crash : Happens due to the action mode is remain enabled on switching the orientation before forwarding.
		 */
		invalidateOptionsMenu();
		/*
		 * If we are in a stealth conversation when the stealth mode is off, we should exit the conversation.
		 */
		if (HikeMessengerApp.isStealthMsisdn(mContactNumber))
		{
			if (HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF) != HikeConstants.STEALTH_ON)
			{
				Intent intent = new Intent(this, HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);

				finish();
			}
		}
		
		/*
		 * To handle the case when photo viewer is opened from chat thread and user forwards/share some item. In this case we should close the photo viewer.
		 */
		if(savedInstanceState == null)
		{
			if(removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG))
			{
				getSupportActionBar().show();
			}
		}

		// This prevent the activity from simply finishing and opens up the last
		// screen.
		getIntent().removeExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);

		mComposeView.setFocusable(true);
		mComposeView.setVisibility(View.VISIBLE);
		mComposeView.requestFocus();

		/*
		 * strictly speaking we shouldn't be reading from the db in the UI Thread
		 */
		int toLoad = 0;
		if(savedInstanceState != null && savedInstanceState.containsKey(HikeConstants.Extras.TOTAL_MSGS_CURRENTLY_LOADED))
		{
			toLoad = savedInstanceState.getInt(HikeConstants.Extras.TOTAL_MSGS_CURRENTLY_LOADED);
			
		}
		else
		{
			toLoad = HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY;
		}
		mConversation = mConversationDb.getConversation(mContactNumber, toLoad,Utils.isGroupConversation(mContactNumber));
		
		if (mConversation == null)
		{
			if (Utils.isBroadcastConversation(mContactNumber))
			{
				/* the user must have deleted the chat. */
				Toast toast = Toast.makeText(this, R.string.invalid_broadcast_list, Toast.LENGTH_LONG);
				toast.show();
				onBackPressed();
				return false;
			}
			else if (Utils.isGroupConversation(mContactNumber))
			{
				/* the user must have deleted the chat. */
				Toast toast = Toast.makeText(this, R.string.invalid_group_chat, Toast.LENGTH_LONG);
				toast.show();
				onBackPressed();
				return false;
			}

			ContactInfo contactInfo = HikeMessengerApp.getContactManager().getContact(mContactNumber, true, true);
			mConversation = new Conversation(mContactNumber, (contactInfo != null) ? contactInfo.getName() : null, contactInfo.isOnhike());
			mConversation.setMessages(HikeConversationsDatabase.getInstance().getConversationThread(mContactNumber, toLoad, mConversation, -1));
		}
		/*
		 * Setting a flag which tells us whether the group contains sms users or not.
		 * Set participant ready by list
		 */
		if ((mConversation instanceof GroupConversation))
		{
			if(!(mConversation instanceof BroadcastConversation))
			{
				hashWatcher = new HashSpanWatcher(mComposeView, HASH_PIN, getResources().getColor(R.color.sticky_yellow));
			}
			
			boolean hasSmsUser = false;
			for (Entry<String, PairModified<GroupParticipant,String>> entry : ((GroupConversation) mConversation).getGroupParticipantList().entrySet())
			{
				GroupParticipant groupParticipant = entry.getValue().getFirst();
				if (!groupParticipant.getContactInfo().isOnhike())
				{
					hasSmsUser = true;
					break;
				}
			}
			((GroupConversation) mConversation).setHasSmsUser(hasSmsUser);
			
			Pair<String,Long> pair = HikeConversationsDatabase.getInstance().getReadByValueForGroup(mConversation.getMsisdn());
			if (pair != null)
			{
				String readBy = pair.first;
				long msgId = pair.second;
				((GroupConversation)mConversation).setupReadByList(readBy, msgId);
			}
			
		}

		mLabel = mConversation.getLabel();
		if (!(mConversation instanceof GroupConversation))
		{
			// To Show Full Name in Actionbar in One-to-One Conv
			mLabel = Utils.getFirstNameAndSurname(mLabel);
		}

		if (showKeyboard && !wasOrientationChanged)
		{
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		}
		else if (!showKeyboard)
		{
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		}

		mHandler.post(new Runnable()
		{

			@Override
			public void run()
			{
				setupActionBar(true);
			}
		});

		gestureDetector = new GestureDetector(this, simpleOnGestureListener);
		boolean addBlockHeader = false;
		if (!(mConversation instanceof GroupConversation))
		{
			contactInfo = HikeMessengerApp.getContactManager().getContact(mContactNumber, true,true);

			favoriteType = contactInfo.getFavoriteType();

			if (mConversation.isOnhike())
			{
				addBlockHeader = true;
			}
			else
			{
				HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/account/profile/" + mContactNumber, RequestType.HIKE_JOIN_TIME, new HikeHttpCallback()
				{
					@Override
					public void onSuccess(JSONObject response)
					{
						Logger.d(getClass().getSimpleName(), "Response: " + response.toString());
						try
						{
							JSONObject profile = response.getJSONObject(HikeConstants.PROFILE);
							long hikeJoinTime = profile.optLong(HikeConstants.JOIN_TIME, 0);
							if (hikeJoinTime > 0)
							{
								addUnkownContactBlockHeader();
								hikeJoinTime = Utils.applyServerTimeOffset(ChatThread.this, hikeJoinTime);

								HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_JOIN_TIME_OBTAINED, new Pair<String, Long>(mContactNumber, hikeJoinTime));
								ContactManager.getInstance().updateHikeStatus(ChatThread.this, mContactNumber, true);
								mConversationDb.updateOnHikeStatus(mContactNumber, true);
								showCallIcon();
								HikeMessengerApp.getPubSub().publish(HikePubSub.USER_JOINED, mContactNumber);
							}
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
					}
				});
				HikeHTTPTask getHikeJoinTimeTask = new HikeHTTPTask(null, -1);
				Utils.executeHttpTask(getHikeJoinTimeTask, hikeHttpRequest);
			}

			if (shouldShowLastSeen())
			{
				/*
				 * Marking the isOnline flag based on the contact's last value.
				 */
				if (contactInfo.getOffline() == 0)
				{
					isOnline = true;
				}

				/*
				 * Making sure nothing is already scheduled wrt last seen.
				 */	
				if (Utils.isOkHttp())
				{
					cancelFetchLastseenTask();
					FetchLastSeenTask fetchLastseenTask = new FetchLastSeenTask(contactInfo.getMsisdn());
					lastSeenRequestToken = fetchLastseenTask.start();
				}
				else
				{
					resetLastSeenScheduler();
					lastSeenScheduler = LastSeenScheduler.getInstance(this);
					lastSeenScheduler.start(contactInfo.getMsisdn(), lastSeenFetchedCallback);
				}
				
				
				HAManager.getInstance().recordLastSeenEvent(ChatThread.class.getName(), "createConversation", null, mContactNumber);
			}
		}

		mUserIsBlocked = ContactManager.getInstance().isBlocked(getMsisdnMainUser());
		if (mUserIsBlocked)
		{
			showOverlay(true);
		}
		else
		{
			hideOverlay();
		}

		/*
		 * make a copy of the message list since it's used internally by the adapter
		 */
		messages = new ArrayList<ConvMessage>(mConversation.getMessages());
		if (messageMap != null)
		{
			messageMap.clear();
		}
		messageMap = new HashMap<Long, ConvMessage>();
		addtoMessageMap(0, messages.size());

		if (mConversation instanceof GroupConversation && mConversation.getUnreadCount() > 0 && messages.size() > 0)
		{
			ConvMessage message = messages.get(messages.size() - 1);
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD && (message.getTypingNotification() == null))
			{
				long timeStamp = messages.get(messages.size() - mConversation.getUnreadCount()).getTimestamp();
				long msgId = messages.get(messages.size() - mConversation.getUnreadCount()).getMsgID();
				if ((messages.size() - mConversation.getUnreadCount()) > 0)
				{
					messages.add((messages.size() - mConversation.getUnreadCount()), new ConvMessage(mConversation.getUnreadCount(), timeStamp, msgId));
				}
				else
				{
					messages.add(0, new ConvMessage(mConversation.getUnreadCount(), timeStamp, msgId));
				}
			}
		}

		shouldShowStickerFtueTip();
		
		mAdapter = new MessagesAdapter(this, messages, mConversation, this);

		shouldRunTimerForHikeOfflineTip = true;
		if (isHikeOfflineTipShowing())
		{
			/*
			 * We need to close the tip without any animation if opening from
			 */
			hideHikeToOfflineTip(false, false, true, false);
		}
		if (!(mConversation instanceof GroupConversation) && mConversation.isOnhike())
		{
			mAdapter.addAllUndeliverdMessages(messages);
		}

		// add block view
		if (addBlockHeader)
		{
			addUnkownContactBlockHeader();
		}
		mConversationsView.setAdapter(mAdapter);
		mConversationsView.setOnItemLongClickListener(this);
		mConversationsView.setOnTouchListener(this);

		/*
		 * Added a hacky fix to ensure that we don't load more messages the first time onScroll is called.
		 */
		loadingMoreMessages = true;
		mConversationsView.setOnScrollListener(this);
		loadingMoreMessages = false;

		if (getIntent().getBooleanExtra(HikeConstants.Extras.FROM_CHAT_THEME_FTUE, false))
		{
			setupChatThemeFTUE();
		}
		else
		{
			selectedTheme = mConversationDb.getChatThemeForMsisdn(mContactNumber);
			Logger.d("ChatThread", "Calling setchattheme from createConversation");
			setChatTheme(selectedTheme);
		}

		if (mConversation.getUnreadCount() > 0 && !messages.isEmpty())
		{
			ConvMessage message = messages.get(messages.size() - 1);
			if (message.getTypingNotification() != null)
			{
				message = messages.get(messages.size() - 2);
			}
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD)
			{
				mConversationsView.setSelection(messages.size() - mConversation.getUnreadCount() - 1);
			}
			else if (!wasOrientationChanged)
			{
				mConversationsView.setSelection(messages.size() - 1);
			}
		}
		// Scroll to the bottom if we just opened a new conversation
		else if (!wasOrientationChanged)
		{
			mConversationsView.setSelection(messages.size() - 1);
		}

		/* add a text changed listener */
		mComposeView.addTextChangedListener(this);

		/* get the number of credits and also listen for changes */
		mCredits = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0);

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.uninit();
		}

		updateUIForHikeStatus();

		mComposeViewWatcher = new ComposeViewWatcher(mConversation, mComposeView, mSendBtn, mCredits, this);

		/*
		 * create an object that we can notify when the contents of the thread are updated
		 */
		mUpdateAdapter = new UpdateAdapter(mAdapter);

		if (checkNetworkError())
		{
			showNetworkError(true);
		}
		else
		{
			showNetworkError(false);
		}

		if (mConversation instanceof GroupConversation)
		{
			myInfo = new GroupParticipant(Utils.getUserContactInfo(prefs));
			if (!checkNetworkError())
			{
				toggleConversationMuteViewVisibility(((GroupConversation) mConversation).isMuted());
			}
			else
			{
				toggleConversationMuteViewVisibility(false);
			}

		}
		else if (mConversation.isBotConv())
		{
			if (!checkNetworkError())
			{
				toggleConversationMuteViewVisibility(mConversation.isMutedBotConv(false));
			}
			else
			{
				toggleConversationMuteViewVisibility(false);
			}
		}
		else
		{
			toggleConversationMuteViewVisibility(false);
		}

		if ((mConversation instanceof GroupConversation) && !((GroupConversation) mConversation).getIsGroupAlive())
		{
			toggleGroupLife(false);
		}
		/*
		 * Check whether we have an existing typing notification for this conversation
		 */
		if (HikeMessengerApp.getTypingNotificationSet().containsKey(mContactNumber))
		{
			runOnUiThread(new SetTypingText(true, HikeMessengerApp.getTypingNotificationSet().get(mContactNumber)));
		}

		/*
		 * Resetting the Orientation Change flag to be used again
		 */
		wasOrientationChanged = false;

		/*
		 * Fix for action mode remaining open after multi-forward. Also, destroying action mode in the end so that above objects get initialized.
		 */
		if(isActionModeOn)
		{
			destroyActionMode();
		}
		return true;
	}

	private void shouldShowStickerFtueTip()
	{
		/*
		 * Only show these tips in a live group conversation or other conversations and is the conversation is not a hike bot conversation.
		 */
		boolean isNuxBot = mContactNumber.equals(HikeConstants.NUX_BOT);
		
		if (isNuxBot || !HikeMessengerApp.hikeBotNamesMap.containsKey(mContactNumber))
		{
			if (!(mConversation instanceof GroupConversation) || ((GroupConversation) mConversation).getIsGroupAlive())
			{
				if (!prefs.getBoolean(HikeMessengerApp.SHOWN_EMOTICON_TIP, isNuxBot))
				{
					showStickerFtueTip();
				}
			}
		}
	}
	
	private void showStickerFtueTip()
	{
		// if some other tip is visible , make its visibility gone, giving more priority to sticker tip 
		if(tipView!=null){
			tipView.setVisibility(View.GONE);
		}
		
		if (pulsatingDotInflated == null)
		{
			pulsatingDot = (ViewStub) findViewById(R.id.pulsatingDotViewStub);
			pulsatingDot.setOnInflateListener(new ViewStub.OnInflateListener()
			{
				@Override
				public void onInflate(ViewStub stub, View inflated)
				{
					pulsatingDotInflated = inflated;
				}
			});
			try
			{
				pulsatingDot.inflate();
			}
			catch (Exception e)
			{

			}
		}
		else
		{
			pulsatingDotInflated.setVisibility(View.VISIBLE);
		}
		startPulsatingDotAnimation();
	}
	
	private void showPinFtueTip()
	{
		tipView = findViewById(R.id.pin_tip);
		tipView.setVisibility(View.VISIBLE);
		tipView.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1)
			{
				// disabling on touch gesture for sticker ftue tip
				// so that we do not send an unnecessary nudge on a
				// double tap on tipview.
				return true;
			}
		});
		tipView.setTag(TipType.PIN);

		ImageView closeIcon = (ImageView) tipView.findViewById(R.id.close_tip);
		closeIcon.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				HikeTip.closeTip(TipType.PIN, tipView, prefs);
				
			}
		});
	}

	private void addtoMessageMap(int from, int to)
	{
		for (int i = to - 1; i >= from; i--)
		{
			ConvMessage message = messages.get(i);
			ConvMessage msg = checkNUpdateFTMsg(message);
			if (msg != null)
			{
				message = msg;
				messages.set(i, message);
			}
			addtoMessageMap(message);
		}
	}

	private ConvMessage checkNUpdateFTMsg(ConvMessage message)
	{
		if (message.isSent() && message.isFileTransferMessage())
		{
			ConvMessage msg = FileTransferManager.getInstance(getApplicationContext()).getMessage(message.getMsgID());
			return msg;
		}
		return null;
	}

	public static void addtoMessageMap(ConvMessage msg)
	{
		State msgState = msg.getState();

		if (msg.getMsgID() <= 0)
		{
			return;
		}
		if (msg.isSent())
		{
			if (messageMap == null)
			{
				messageMap = new HashMap<Long, ConvMessage>();
			}

			if (msg.isFileTransferMessage())
			{
				if (TextUtils.isEmpty(msg.getMetadata().getHikeFiles().get(0).getFileKey()))
				{
					messageMap.put(msg.getMsgID(), msg);
					return;
				}
			}
			if (msg.isSMS())
			{
				if (msgState == State.SENT_UNCONFIRMED || msgState == State.SENT_FAILED)
				{
					messageMap.put(msg.getMsgID(), msg);
				}
			}
			else
			{
				if (msgState != State.SENT_DELIVERED_READ)
				{
					messageMap.put(msg.getMsgID(), msg);
				}
			}
		}
	}

	private void removeFromMessageMap(ConvMessage msg)
	{
		if (messageMap == null)
			return;

		if (msg.isGroupChat())
		{
			return;
		}

		if (msg.isFileTransferMessage())
		{
			if (!TextUtils.isEmpty(msg.getMetadata().getHikeFiles().get(0).getFileKey()))
			{
				messageMap.remove(msg.getMsgID());
			}
		}
		else
		{
			messageMap.remove(msg.getMsgID());
		}
	}

	private void addUnkownContactBlockHeader()
	{
		if (contactInfo != null && contactInfo.isUnknownContact() && !Utils.isBot(mConversation.getMsisdn()))
		{
			if (messages != null && messages.size() > 0)
			{
				ConvMessage cm = messages.get(0);
				if (cm.isBlockAddHeader())
				{
					return;
				}
				cm = new ConvMessage(0, 0l, 0l);
				cm.setBlockAddHeader(true);
				messages.add(0, cm);
				if (mAdapter != null)
				{
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	private void showCallIcon()
	{
		if(mMenu!=null && !HikeMessengerApp.hikeBotNamesMap.containsKey(mContactNumber) && Utils.isVoipActivated(this))
		{
			mMenu.findItem(R.id.voip_call).setVisible(true);
		}
	}

	public void updateViewWindowForReadBy()
	{
		if (mConversationsView.getLastVisiblePosition() >= (messages.size() - 2))
		{
			mConversationsView.post(new Runnable()
			{

				@Override
				public void run()
				{
					mConversationsView.smoothScrollToPosition(messages.size() - 1);
				}
			});
		}
	}

	private void setupSMSToggleButton()
	{
		TextView smsToggleSubtext = (TextView) findViewById(R.id.sms_toggle_subtext);
		CheckBox smsToggle = (CheckBox) findViewById(R.id.checkbox);
		TextView hikeSmsText = (TextView) findViewById(R.id.hike_text);
		TextView regularSmsText = (TextView) findViewById(R.id.sms_text);

		if (selectedTheme == ChatTheme.DEFAULT)
		{
			hikeSmsText.setTextColor(this.getResources().getColor(R.color.sms_choice_unselected));
			regularSmsText.setTextColor(this.getResources().getColor(R.color.sms_choice_unselected));
			smsToggleSubtext.setTextColor(this.getResources().getColor(R.color.sms_choice_unselected));
			smsToggle.setButtonDrawable(R.drawable.sms_checkbox);
			findViewById(R.id.sms_toggle_button).setBackgroundResource(R.drawable.bg_sms_toggle);
		}
		else
		{
			hikeSmsText.setTextColor(this.getResources().getColor(R.color.white));
			regularSmsText.setTextColor(this.getResources().getColor(R.color.white));
			smsToggleSubtext.setTextColor(this.getResources().getColor(R.color.white));
			smsToggle.setButtonDrawable(R.drawable.sms_checkbox_custom_theme);
			findViewById(R.id.sms_toggle_button).setBackgroundResource(selectedTheme.smsToggleBgRes());
		}

		boolean smsToggleOn = Utils.getSendSmsPref(this);
		smsToggle.setChecked(smsToggleOn);
		mAdapter.initializeSmsToggleTexts(hikeSmsText, regularSmsText, smsToggleSubtext);
		mAdapter.setSmsToggleSubtext(smsToggleOn);

		smsToggleSubtext.setVisibility(View.VISIBLE);
		smsToggle.setVisibility(View.VISIBLE);
		hikeSmsText.setVisibility(View.VISIBLE);
		regularSmsText.setVisibility(View.VISIBLE);

		smsToggle.setOnCheckedChangeListener(mAdapter);
	}

	private void setupChatThemeFTUE()
	{
		Random random = new Random();
		selectedTheme = ChatTheme.FTUE_THEMES[random.nextInt(ChatTheme.FTUE_THEMES.length)];

		final View whiteOverlay = findViewById(R.id.white_overlay);
		AnimationSet animationSet = new AnimationSet(false);

		AlphaAnimation alphaIn = new AlphaAnimation(0, 1);
		alphaIn.setStartOffset(500);
		alphaIn.setDuration(500);

		AlphaAnimation alphaOut = new AlphaAnimation(1, 0);
		alphaOut.setStartOffset(1000);
		alphaOut.setDuration(500);

		animationSet.addAnimation(alphaIn);
		animationSet.addAnimation(alphaOut);

		whiteOverlay.setVisibility(View.VISIBLE);
		whiteOverlay.startAnimation(animationSet);

		animationSet.setAnimationListener(new AnimationListener()
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
				whiteOverlay.setVisibility(View.GONE);
			}
		});

		mHandler.postDelayed(new Runnable()
		{

			@Override
			public void run()
			{
				setChatTheme(selectedTheme);
				sendChatThemeMessage();
				/*
				 * Only remove the extra once we've actually set the theme
				 */
				getIntent().removeExtra(HikeConstants.Extras.FROM_CHAT_THEME_FTUE);
			}
		}, 1200);
	}

	private void setupActionBar(boolean initialising)
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.chat_thread_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);
		View contactInfoContainer = actionBarView.findViewById(R.id.contact_info);

		String lastSeenString = null;
		if (!initialising)
		{
			lastSeenString = mLastSeenView.getText().toString();
		}

		avatar = (ImageView) actionBarView.findViewById(R.id.avatar);
		mLabelView = (TextView) actionBarView.findViewById(R.id.contact_name);
		mLastSeenView = (TextView) actionBarView.findViewById(R.id.contact_status);

		if (initialising)
		{
			mLastSeenView.setVisibility(View.GONE);
			mLastSeenView.setSelected(true);

			if (mConversation instanceof GroupConversation)
			{
				updateActivePeopleNumberView(0);
			}
			else
			{
				if(mConversation != null)
				{
					setLastSeenTextBasedOnHikeValue(mConversation.isOnhike());
				}
			}
		}
		else
		{
			setLastSeenText(lastSeenString);
		}

		setAvatar();
		// avatar.setImageDrawable(IconCacheManager.getInstance()
		// .getIconForMSISDN(mContactNumber, true));
		setLabel(mLabel);

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Utils.hideSoftKeyboard(ChatThread.this, mComposeView);

				Intent intent = new Intent(ChatThread.this, HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);

				finish();
			}
		});

		contactInfoContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				openProfileScreen();
                if (mConversation.isBotConv())
                {
                    analyticsForBots(HikePlatformConstants.BOT_VIEW_PROFILE, HikePlatformConstants.ACTION_BAR, AnalyticsConstants.CLICK_EVENT, null);
                }
			}
		});

		actionBar.setBackgroundDrawable(getResources().getDrawable(selectedTheme.headerBgResId()));
		actionBar.setCustomView(actionBarView);
	}

	private void setAvatar()
	{
		if (avatar == null)
		{
			return;
		}

		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(mContactNumber);
		if (drawable == null)
		{
			drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(mContactNumber, false);
		}
		avatar.setScaleType(ScaleType.FIT_CENTER);
		avatar.setImageDrawable(drawable);
	}

	private void setLabel(String label)
	{
		if (mLabelView == null)
		{
			return;
		}
		mLabelView.setText(label);
	}

	private void setLastSeenText(String text)
	{
		if (mLastSeenView == null)
		{
			return;
		}
		mLastSeenView.setText(text);

		if (TextUtils.isEmpty(text))
		{
			mLastSeenView.setVisibility(View.GONE);
			return;
		}

		if (mLastSeenView.getVisibility() == View.GONE)
		{
			if (mLabelView == null)
			{
				mLastSeenView.setVisibility(View.VISIBLE);
				return;
			}

			if (mConversation.isOnhike() && !(mConversation instanceof GroupConversation))
			{
				mLastSeenView.setVisibility(View.INVISIBLE);
				Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_up_last_seen);
				mLabelView.startAnimation(animation);

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
						mLastSeenView.setVisibility(View.VISIBLE);
					}
				});
			}
			else
			{
				mLastSeenView.setVisibility(View.VISIBLE);
			}
		}
		HAManager.getInstance().recordLastSeenEvent(ChatThread.class.getName(), "setLastSeenText", "Updated UI for LastSeen", mContactNumber);
	}

	private void hideLastSeenText()
	{
		if (mLastSeenView == null)
		{
			return;
		}
		mLastSeenView.setVisibility(View.GONE);
	}

	private void setLastSeenTextBasedOnHikeValue(boolean onHike)
	{
		if (onHike || Utils.isBot(mContactNumber))
		{
			hideLastSeenText();
		}
		else
		{
			setLastSeenText(getString(R.string.on_sms));
		}
	}

	private void updateActivePeopleNumberView(int addPeopleCount)
	{
		int numActivePeople = ((GroupConversation) mConversation).getGroupMemberAliveCount() + addPeopleCount;
		((GroupConversation) mConversation).setGroupMemberAliveCount(numActivePeople);

		if (numActivePeople > 0)
		{
			/*
			 * Adding 1 to count the user.
			 */
			if (mConversation instanceof BroadcastConversation)
			{
				setLastSeenText(getString(R.string.num_people, numActivePeople));
			}
			else
			{
				setLastSeenText(getString(R.string.num_people, (numActivePeople + 1)));
			}
		}
	}

	private void openProfileScreen()
	{
		if (mUserIsBlocked)
		{
			return;
		}
		if (mConversation instanceof BroadcastConversation)
		{
			if (!((BroadcastConversation) mConversation).getIsGroupAlive())
			{
				return;
			}

			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.GROUP_INFO_TOP_BUTTON);
			Intent intent = new Intent();
			intent.setClass(ChatThread.this, ProfileActivity.class);
			intent.putExtra(HikeConstants.Extras.BROADCAST_LIST, true);
			intent.putExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST, mConversation.getMsisdn());
			startActivity(intent);
		}
		else if (mConversation instanceof GroupConversation)
		{
			if (!((GroupConversation) mConversation).getIsGroupAlive())
			{
				return;
			}

			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.GROUP_INFO_TOP_BUTTON);
			Intent intent = new Intent();
			intent.setClass(ChatThread.this, ProfileActivity.class);
			intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
			intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, mConversation.getMsisdn());
			startActivity(intent);
		}
		else
		{
			String userMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, "");

			Intent intent = new Intent();
			intent.setClass(ChatThread.this, ProfileActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			if (!userMsisdn.equals(mContactNumber))
			{
				intent.putExtra(HikeConstants.Extras.CONTACT_INFO, mContactNumber);
				intent.putExtra(HikeConstants.Extras.ON_HIKE, mConversation.isOnhike());
			}
			startActivity(intent);
		}
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();
		saveDraft();
	}

	public boolean shouldShowLastSeen()
	{
		if ((favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_RECEIVED || favoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED)
				&& mConversation.isOnhike())
		{
			return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HikeConstants.LAST_SEEN_PREF, true);
		}
		return false;
	}

	/*
	 * Update the UI to show SMS Credits/etc if the conversation is on hike
	 */
	private void updateUIForHikeStatus()
	{
		if (mConversation.isOnhike() || (mConversation instanceof GroupConversation))
		{
			removeSMSToggle();

			if ((mConversation instanceof GroupConversation) && ((GroupConversation) mConversation).hasSmsUser())
			{
				if (mCredits == 0)
				{
					zeroCredits();
				}
				else
				{
					nonZeroCredits();
				}
			}
			else
			{
				nonZeroCredits();
			}
		}
		else
		{
			updateChatMetadata();
		}
	}

	private void removeSMSToggle()
	{
		findViewById(R.id.sms_toggle_button).setVisibility(View.GONE);
	}

	/* returns TRUE iff the last message was received and unread */
	private boolean isLastMsgReceivedAndUnread()
	{
		if (mAdapter == null || mConversation == null)
		{
			return false;
		}

		ConvMessage lastMsg = null;
		for (int i = messages.size() - 1; i >= 0; i--)
		{
			ConvMessage msg = messages.get(i);
			if (msg.getTypingNotification() != null)
			{
				continue;
			}
			lastMsg = msg;
			break;
		}
		if (lastMsg == null)
		{
			return false;
		}

		return lastMsg.getState() == ConvMessage.State.RECEIVED_UNREAD || lastMsg.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE;
	}

	/*
	 * marks messages read
	 */
	private void setMessagesRead()
	{
		if (!hasWindowFocus())
		{
			return;
		}

		if (isLastMsgReceivedAndUnread())
		{
			if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
			{
				setSMSReadInNative();
			}

			JSONArray ids = mConversationDb.updateStatusAndSendDeliveryReport(mConversation.getMsisdn());
			mPubSub.publish(HikePubSub.MSG_READ, mConversation.getMsisdn());

			Logger.d("UnreadBug", "Unread count event triggered");
			/*
			 * If there are msgs which are RECEIVED UNREAD then only broadcast a msg that these are read avoid sending read notifications for group chats
			 */
			if (ids != null)
			{
				// int lastReadIndex = messages.size() - ids.length();
				// // Scroll to the last unread message
				// if (lastReadIndex == 0)
				// {
				// mConversationsView.setSelection(lastReadIndex);
				// }
				// else
				// {
				// mConversationsView.setSelection(lastReadIndex - 1);
				// }

				JSONObject object = new JSONObject();
				try
				{
					object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_READ);
					object.put(HikeConstants.TO, mConversation.getMsisdn());
					object.put(HikeConstants.DATA, ids);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

		        HikeMqttManagerNew.getInstance().sendMessage(object, HikeMqttManagerNew.MQTT_QOS_ONE);
			}
		}
	}

	private void setSMSReadInNative()
	{
		new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				Logger.d(getClass().getSimpleName(), "Marking message as read: " + mContactNumber);

				ContentValues contentValues = new ContentValues();
				contentValues.put(HikeConstants.SMSNative.READ, 1);
				try
				{
					int smsUptaed = getContentResolver().update(HikeConstants.SMSNative.INBOX_CONTENT_URI, contentValues, HikeConstants.SMSNative.NUMBER + "=?",
							new String[] { mContactNumber });
				}
				catch (Exception iae)
				{
					// this case should not happen usually , but id no message database resolver is present , say rooted phones , app will crash
					iae.printStackTrace();
				}
			}
		}).start();
	}

	private class SetTypingText implements Runnable
	{
		TypingNotification typingNotification;

		public SetTypingText(boolean direction, TypingNotification typingNotification)
		{
			this.direction = direction;
			this.typingNotification = typingNotification;
		}

		boolean direction;

		@Override
		public void run()
		{
			if (direction)
			{
				if (messages.isEmpty() || messages.get(messages.size() - 1).getTypingNotification() == null)
				{
					addMessage(new ConvMessage(typingNotification));
					Logger.d(getClass().getSimpleName(), "calling chatThread.addMessage() Line no. : 2129");
				}
				else if (messages.get(messages.size() - 1).getTypingNotification() != null)
				{
					ConvMessage convMessage = messages.get(messages.size() - 1);
					convMessage.setTypingNotification(typingNotification);

					mAdapter.notifyDataSetChanged();
				}
			}
			else
			{
				if (!messages.isEmpty() && messages.get(messages.size() - 1).getTypingNotification() != null)
				{
					/*
					 * We only remove the typing notification if the conversation in a one to one conversation or it no one is typing in the group.
					 */
					if (!(mConversation instanceof GroupConversation))
					{
						messages.remove(messages.size() - 1);
					}
					else
					{
						GroupTypingNotification groupTypingNotification = (GroupTypingNotification) messages.get(messages.size() - 1).getTypingNotification();
						if (groupTypingNotification.getGroupParticipantList().isEmpty())
						{
							messages.remove(messages.size() - 1);
						}
					}
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	@Override
	public void onEventReceived(final String type, final Object object)
	{
		if (mContactNumber == null || mConversation == null)
		{
			Logger.w("ChatThread", "received message when contactNumber is null type=" + type + " object=" + object);
			return;
		}

		if (HikePubSub.MESSAGE_RECEIVED.equals(type))
		{
			final ConvMessage message = (ConvMessage) object;
			String msisdn = message.getMsisdn();
			if (msisdn == null)
			{
				Logger.wtf("ChatThread", "Message with missing msisdn:" + message.toString());
			}
			if (msisdn.equals(mContactNumber))
			{
				if (hasWindowFocus())
				{
					message.setState(ConvMessage.State.RECEIVED_READ);
					mConversationDb.updateMsgStatus(message.getMsgID(), ConvMessage.State.RECEIVED_READ.ordinal(), mConversation.getMsisdn());
					if (message.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
					{
						HikeMqttManagerNew.getInstance().sendMessage(message.serializeDeliveryReportRead(), HikeMqttManagerNew.MQTT_QOS_ONE);
					}
					// return to
					// sender

					mPubSub.publish(HikePubSub.MSG_READ, mConversation.getMsisdn());
				}

				if (message.getParticipantInfoState() != ParticipantInfoState.NO_INFO && mConversation instanceof GroupConversation)
				{
					ContactManager conMgr = ContactManager.getInstance();
					((GroupConversation) mConversation).setGroupParticipantList(conMgr.getGroupParticipants(mConversation.getMsisdn(), false, false));
				}

				final String label = message.getParticipantInfoState() != ParticipantInfoState.NO_INFO ? mConversation.getLabel() : null;
				if (activityVisible && SoundUtils.isTickSoundEnabled(getApplicationContext()))
				{
					SoundUtils.playSoundFromRaw(getApplicationContext(), R.raw.received_message);
				}
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if (label != null)
						{
							setLabel(label);
						}

						addMessage(message);
						Logger.d(getClass().getSimpleName(), "calling chatThread.addMessage() Line no. : 2219");
					}
				});

			}
		}
		else if (HikePubSub.END_TYPING_CONVERSATION.equals(type))
		{
			TypingNotification typingNotification = (TypingNotification) object;
			if (typingNotification == null)
			{
				return;
			}
			if (mContactNumber.equals(typingNotification.getId()))
			{
				runOnUiThread(new SetTypingText(false, typingNotification));
			}
		}
		else if (HikePubSub.TYPING_CONVERSATION.equals(type))
		{
			TypingNotification typingNotification = (TypingNotification) object;
			if (typingNotification == null)
			{
				return;
			}
			if (mContactNumber.equals(typingNotification.getId()))
			{

				runOnUiThread(new SetTypingText(true, typingNotification));

				if (shouldShowLastSeen() && contactInfo.getOffline() != -1)
				{
					/*
					 * Publishing an online event for this number.
					 */
					contactInfo.setOffline(0);
					HikeMessengerApp.getPubSub().publish(HikePubSub.LAST_SEEN_TIME_UPDATED, contactInfo);
				}
			}
		}
		// We only consider this case if there is a valid conversation in the
		// Chat Thread
		else if (mConversation != null && HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			mCredits = ((Integer) object).intValue();
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					updateUIForHikeStatus();
					if (!animatedOnce)
					{
						animatedOnce = prefs.getBoolean(HikeConstants.Extras.ANIMATED_ONCE, false);
						if (!animatedOnce)
						{
							Editor editor = prefs.edit();
							editor.putBoolean(HikeConstants.Extras.ANIMATED_ONCE, true);
							editor.commit();
						}
					}

					if ((mCredits % HikeConstants.SHOW_CREDITS_AFTER_NUM == 0 || !animatedOnce) && !mConversation.isOnhike())
					{
						animatedOnce = true;
						showSMSCounter();
					}
				}
			});
		}
		else if (HikePubSub.MESSAGE_DELIVERED.equals(type))
		{
			Pair<String, Long> pair = (Pair<String, Long>) object;
			// If the msisdn don't match we simply return
			if (!mConversation.getMsisdn().equals(pair.first))
			{
				return;
			}
			long msgID = pair.second;
			// TODO we could keep a map of msgId -> conversation objects
			// somewhere to make this faster
			ConvMessage msg = findMessageById(msgID);
			if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED.ordinal()))
			{
				msg.setState(ConvMessage.State.SENT_DELIVERED);
				if (!(mConversation instanceof GroupConversation) && mConversation.isOnhike())
				{
					if(mAdapter != null)
					{
						mAdapter.removeFromUndeliverdMessage(msg, true);
					}
				}
				runOnUiThread(mUpdateAdapter);
			}
		}
		else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type))
		{
			Pair<String, long[]> pair = (Pair<String, long[]>) object;
			// If the msisdn don't match we simply return
			if (!mConversation.getMsisdn().equals(pair.first))
			{
				return;
			}
			long[] ids = pair.second;
			// TODO we could keep a map of msgId -> conversation objects
			// somewhere to make this faster
			for (int i = 0; i < ids.length; i++)
			{
				ConvMessage msg = findMessageById(ids[i]);
				if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED_READ.ordinal()))
				{
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
					removeFromMessageMap(msg);
				}
			}
			/*
			 * Right now our logic is to force MR for all the unread messages that is why we need to remove all message from undelivered set
			 * 
			 * if in future we move to MR less than msgId we should modify this logic also
			 */
			if (!(mConversation instanceof GroupConversation) && mConversation.isOnhike())
			{
				if(mAdapter != null)
				{
					mAdapter.removeAllFromUndeliverdMessage();
				}
			}
			runOnUiThread(mUpdateAdapter);
		}
		else if (HikePubSub.MESSAGE_FAILED.equals(type))
		{
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_FAILED);
				runOnUiThread(mUpdateAdapter);
			}
		}
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type))
		{
			long msgId = ((Long) object).longValue();
			setStateAndUpdateView(msgId, true);
		}
		else if (HikePubSub.SERVER_RECEIVED_MULTI_MSG.equals(type))
		{
			Pair<Long, Integer> p  = (Pair<Long, Integer>) object;
			long baseId = p.first;
			int count = p.second;
			for(long msgId=baseId; msgId<(baseId+count) ; msgId++)
			{
				if (msgId == baseId)
				{
					setStateAndUpdateView(msgId, true);
				}
				else
				{
					setStateAndUpdateView(msgId, false);
				}
			}
			if (mAdapter == null)
			{
				return;
			}
			runOnUiThread(mUpdateAdapter);
		}
		else if (HikePubSub.ICON_CHANGED.equals(type))
		{
			String msisdn = (String) object;
			if (mContactNumber.equals(msisdn))
			{
				/* update the image drawable */
				runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						setAvatar();
						// avatar.setImageDrawable(IconCacheManager.getInstance()
						// .getIconForMSISDN(mContactNumber, true));
					}
				});
			}
		}
		else if ((HikePubSub.USER_LEFT.equals(type)) || (HikePubSub.USER_JOINED.equals(type)))
		{
			/* only update the UI if the message is for this conversation */
			String msisdn = (String) object;
			if (!mContactNumber.equals(msisdn))
			{
				return;
			}

			mConversation.setOnhike(HikePubSub.USER_JOINED.equals(type));
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					setLastSeenTextBasedOnHikeValue(mConversation.isOnhike());

					updateUIForHikeStatus();
					mUpdateAdapter.run();
				}
			});
		}
		else if (HikePubSub.GROUP_NAME_CHANGED.equals(type))
		{
			String groupId = (String) object;
			if (mContactNumber.equals(groupId))
			{
				final String groupName = ContactManager.getInstance().getName(mContactNumber);
				mConversation.setContactName(groupName);

				runOnUiThread(new Runnable()
				{
					public void run()
					{
						setLabel(groupName);
					}
				});
			}
		}
		else if (HikePubSub.GROUP_END.equals(type))
		{
			String groupId = ((JSONObject) object).optString(HikeConstants.TO);
			if (mContactNumber.equals(groupId))
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						toggleGroupLife(false);
					}
				});
			}
		}
		else if (HikePubSub.CONTACT_ADDED.equals(type) || HikePubSub.CONTACT_DELETED.equals(type))
		{
			ContactInfo contactInfo = (ContactInfo) object;
			if (contactInfo == null)
			{
				return;
			}

			if (this.mContactNumber.equals(contactInfo.getMsisdn()))
			{
				if(HikePubSub.CONTACT_DELETED.equals(type))
					this.mContactName = contactInfo.getMsisdn();
				else
					this.mContactName = contactInfo.getName();
				mConversation.setContactName(this.mContactName);
				this.mLabel = mContactName;
				if (!(mConversation instanceof GroupConversation))
				{
					this.mLabel = Utils.getFirstName(mLabel);
				}
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						setLabel(mLabel);
						if(HikePubSub.CONTACT_DELETED.equals(type))
							setAvatar();

						// remove block header if present
						if (messages != null && messages.size() > 0)
						{
							ConvMessage cm = messages.get(0);
							if (cm.isBlockAddHeader())
							{
								messages.remove(0);
								mAdapter.notifyDataSetChanged();
							}
						}
					}
				});
			}
		}
		else if (HikePubSub.UPLOAD_FINISHED.equals(type))
		{
			runOnUiThread(mUpdateAdapter);
		}
		else if (HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED.equals(type))
		{
			runOnUiThread(mUpdateAdapter);
		}
		else if (HikePubSub.FILE_MESSAGE_CREATED.equals(type))
		{
			final ConvMessage convMessage = (ConvMessage) object;
			selectedFile = null;
			/*
			 * Making sure this convmessage belongs to the conversation.
			 */
			if (!convMessage.getMsisdn().equals(mContactNumber))
			{
				return;
			}

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					addMessage(convMessage);
					Logger.d(getClass().getSimpleName(), "calling chatThread.addMessage() Line no. : 2429");
				}
			});
		}
		else if (HikePubSub.MUTE_CONVERSATION_TOGGLED.equals(type))
		{
			Pair<String, Boolean> groupMute = (Pair<String, Boolean>) object;
			if (!groupMute.first.equals(this.mContactNumber))
			{
				return;
			}
			final Boolean isMuted = groupMute.second;

			if (!Utils.isBot(groupMute.first))
			{
				((GroupConversation) mConversation).setIsMuted(isMuted);
			}

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (!checkNetworkError())
					{
						toggleConversationMuteViewVisibility(isMuted);
					}
					invalidateOptionsMenu();
				}
			});
		}
		else if (HikePubSub.MUTE_BOT.equals(type))
		{
			mConversation.setBotConvMute(true);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED,
					new Pair<String, Boolean>(mConversation.getMsisdn(), mConversation.isMutedBotConv(false)));
		}
		else if (HikePubSub.UPDATE_NETWORK_STATE.equals(type))
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (checkNetworkError())
					{
						showNetworkError(true);
						if (mConversation instanceof GroupConversation || mConversation.isBotConv())
						{
							toggleConversationMuteViewVisibility(false);
						}
					}
					else
					{
						showNetworkError(false);
						if (mConversation instanceof GroupConversation)
						{
							toggleConversationMuteViewVisibility(((GroupConversation) mConversation).isMuted());
						}
						else if (mConversation.isBotConv())
						{
							toggleConversationMuteViewVisibility(mConversation.isMutedBotConv(false));
						}
					}
				}
			});
		}
		else if (HikePubSub.BLOCK_USER.equals(type) || HikePubSub.UNBLOCK_USER.equalsIgnoreCase(type))
		{
			String msisdn = (String) object;
			final boolean blocked = HikePubSub.BLOCK_USER.equals(type);

			if (!msisdn.equals(getMsisdnMainUser()))
			{
				return;
			}

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (blocked)
					{
						blockUser();
					}
					else
					{
						unblockUser();
					}
				}
			});
		}
		else if (HikePubSub.DELETE_MESSAGE.equals(type))
		{
			Pair<ArrayList<Long>, Bundle> deleteMessage = (Pair<ArrayList<Long>, Bundle>) object;
			Bundle bundle = deleteMessage.second;
			String msisdn = bundle.getString(HikeConstants.Extras.MSISDN);
			if(!msisdn.equals(mContactNumber))
			{
				return;
			}
			
			final ArrayList<Long> msgIds = deleteMessage.first;
			final boolean deleteMediaFromPhone = bundle.getBoolean(HikeConstants.Extras.DELETE_MEDIA_FROM_PHONE);
			
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if(mAdapter == null || msgIds.isEmpty())
					{
						return;
					}
					deleteMessages(msgIds, deleteMediaFromPhone);
				}
			});
		}
		else if(HikePubSub.LATEST_PIN_DELETED.equals(type))
		{
			long msgId = (Long)object;
			
			try 
			{
				long pinIdFromMetadata = mConversation.getMetaData().getLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
				
				if(msgId==pinIdFromMetadata)
				{
					runOnUiThread(new Runnable() 
					{				
						@Override
						public void run() 
						{
							hidePinFromUI(true);
						}
					});
				}
			}
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
			
		}
		else if (HikePubSub.GROUP_REVIVED.equals(type))
		{
			String groupId = (String) object;
			if (mContactNumber.equals(groupId))
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						toggleGroupLife(true);
						((GroupConversation) mConversation).setGroupMemberAliveCount(0);
					}
				});
			}
		}
		else if (HikePubSub.CHANGED_MESSAGE_TYPE.equals(type))
		{
			updateAdapter();
		}
		else if (HikePubSub.SHOW_SMS_SYNC_DIALOG.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					smsDialog = Utils.showSMSSyncDialog(ChatThread.this, true);
					dialogShowing = DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG;
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
					if (smsDialog != null)
					{
						smsDialog.dismiss();
					}
					dialogShowing = null;
				}
			});
		}
		else if (HikePubSub.SMS_SYNC_START.equals(type))
		{
			dialogShowing = DialogShowing.SMS_SYNCING_DIALOG;
		}
		else if (HikePubSub.STICKER_DOWNLOADED.equals(type))
		{
			updateAdapter();
		}
		else if (HikePubSub.LAST_SEEN_TIME_UPDATED.equals(type))
		{
			ContactInfo newContactInfo = (ContactInfo) object;

			HAManager.getInstance().recordLastSeenEvent(ChatThread.class.getName(), "recv pubsub LAST_SEEN_TIME_UPDATED", "going update UI", newContactInfo.getMsisdn());
			updateLastSeen(newContactInfo.getMsisdn(), newContactInfo.getOffline(), newContactInfo.getLastSeenTime());
		}
		else if (HikePubSub.SEND_SMS_PREF_TOGGLED.equals(type))
		{
			if (mConversation == null)
			{
				return;
			}
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					updateUIForHikeStatus();
				}
			});
		}
		else if (HikePubSub.PARTICIPANT_LEFT_GROUP.equals(type))
		{
			if (mConversation == null)
			{
				return;
			}
			if (mConversation instanceof GroupConversation)
			{
				if (mConversation.getMsisdn().equals(((JSONObject) object).optString(HikeConstants.TO)))
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							// decrementing one user
							updateActivePeopleNumberView(-1);
						}
					});
				}
			}
		}
		else if (HikePubSub.PARTICIPANT_JOINED_GROUP.equals(type))
		{
			if (mConversation == null)
			{
				return;
			}
			if (mConversation.getMsisdn().equals(((JSONObject) object).optString(HikeConstants.TO)))
			{
				JSONObject obj = (JSONObject) object;
				JSONArray participants = obj.optJSONArray(HikeConstants.DATA);
				final int addPeopleCount = participants.length();
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						// increment by number of newly added participants
						updateActivePeopleNumberView(addPeopleCount);
					}
				});
			}
		}
		else if (HikePubSub.CHAT_BACKGROUND_CHANGED.equals(type))
		{
			if (mConversation == null)
			{
				return;
			}
			Pair<String, ChatTheme> pair = (Pair<String, ChatTheme>) object;
			if (!mConversation.getMsisdn().equals(pair.first))
			{
				return;
			}

			selectedTheme = pair.second;
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					Logger.d("ChatThread", "Calling setchattheme from onEventRecieved");
					setChatTheme(selectedTheme);
				}
			});
		}
		else if (HikePubSub.CLOSE_CURRENT_STEALTH_CHAT.equals(type))
		{
			if (mConversation == null || !mConversation.isStealth())
			{
				return;
			}

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					saveDraft();
					finish();
				}
			});
		}
		
		else if (HikePubSub.APP_FOREGROUNDED.equals(type))
		{

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (contactInfo == null)
					{
						return;
					}

					if (!shouldShowLastSeen())
					{
						return;
					}

					if (Utils.isOkHttp())
					{
						cancelFetchLastseenTask();
						FetchLastSeenTask fetchLastseenTask = new FetchLastSeenTask(contactInfo.getMsisdn());
						lastSeenRequestToken = fetchLastseenTask.start();
					}
					else
					{
						if (lastSeenScheduler == null)
						{
							lastSeenScheduler = LastSeenScheduler.getInstance(ChatThread.this);
						}
						else
						{
							lastSeenScheduler.stop(false);
						}
						lastSeenScheduler.start(contactInfo.getMsisdn(), lastSeenFetchedCallback);
					}
					HAManager.getInstance().recordLastSeenEvent(ChatThread.class.getName(), "onEventRecv", "recv pubsub APP_FOREGROUNDED", mContactNumber);
				}
			});
		}
		/*
		 * Receives conversation group-id, the message id for the message read packet, and the participant msisdn.
		 */
		else if (HikePubSub.GROUP_MESSAGE_DELIVERED_READ.equals(type))
		{
			Pair<String, Pair<Long,String>> pair = (Pair<String, Pair<Long, String>>) object;
			// If the msisdn don't match we simply return
			if (!mConversation.getMsisdn().equals(pair.first) || messages == null || messages.isEmpty())
			{
				return;
			}
			Long mrMsgId = pair.second.first;
			for (int i = messages.size() - 1 ; i>=0; i--)
			{
				ConvMessage msg = messages.get(i);
				if (msg != null && msg.isSent())
				{
					long id = msg.getMsgID();
					if (id > mrMsgId)
					{
						continue;
					}
					if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED_READ.ordinal()))
					{
						msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
						removeFromMessageMap(msg);
					}
					else
					{
						break;
					}
				}
			}
			String participant = pair.second.second;
			// TODO we could keep a map of msgId -> conversation objects
			// somewhere to make this faster
			((GroupConversation)mConversation).updateReadByList(participant,mrMsgId);
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (messages.isEmpty())
					{
						return;
					}

					mAdapter.notifyDataSetChanged();
					/*
					 * We are doing this based on the assumption that if the last message is sent, we probably received an mr for that message.
					 * Now we'll scroll to the bottom as long as the bottom message is visible.
					 */
					if (messages.get(messages.size() - 1).isSent())
					{
						updateViewWindowForReadBy();
					}
				}
			});
		}
		/*
		 * The list of messages is processed.
		 * The messages are added and the UI is updated at once.
		 */
		else if (HikePubSub.BULK_MESSAGE_RECEIVED.equals(type))
		{
			HashMap<String, LinkedList<ConvMessage>> messageListMap = (HashMap<String, LinkedList<ConvMessage>>) object;
			final LinkedList<ConvMessage> messageList = messageListMap.get(mContactNumber);
			String label = null;
			if(messageList != null)
			{
				ConvMessage pin = null;				
				JSONArray ids = new JSONArray();
				for (ConvMessage message : messageList)
				{
					if(message.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
					{
						pin = message;
					}
					if (hasWindowFocus())
					{
						message.setState(ConvMessage.State.RECEIVED_READ);
						if (message.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
						{
							ids.put(String.valueOf(message.getMappedMsgID()));
						}
						
					}
					
					if (message.getParticipantInfoState() != ParticipantInfoState.NO_INFO && mConversation instanceof GroupConversation)
					{
						ContactManager conMgr = ContactManager.getInstance();
						((GroupConversation) mConversation).setGroupParticipantList(conMgr.getGroupParticipants(mConversation.getMsisdn(), false, false));
					}

					label = message.getParticipantInfoState() != ParticipantInfoState.NO_INFO ? mConversation.getLabel() : null;
					if (activityVisible && SoundUtils.isTickSoundEnabled(getApplicationContext()))
					{
						SoundUtils.playSoundFromRaw(getApplicationContext(), R.raw.received_message);
					}
				}
				final String convLabel = label;
				final ConvMessage pinMsg = pin;

				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if (convLabel != null)
						{
							setLabel(convLabel);
						}
						addBulkMessages(messageList);
						Logger.d(getClass().getSimpleName(), "calling chatThread.addMessage() Line no. : 2219");
												
						if(pinMsg!= null)
						{
							showImpMessage(pinMsg, -1);
						}
					}
				});
				
				
				if (ids != null && ids.length() > 0)
				{
					JSONObject jsonObject = new JSONObject();
					try
					{
						jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_READ);
						jsonObject.put(HikeConstants.TO, mConversation.getMsisdn());
						jsonObject.put(HikeConstants.DATA, ids);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
					// TODO make the calls here.
					HikeMqttManagerNew.getInstance().sendMessage(jsonObject, HikeMqttManagerNew.MQTT_QOS_ONE);
					mPubSub.publish(HikePubSub.MSG_READ, mConversation.getMsisdn());
				}
			}
		}
		/*
		 * The list of msisdns and their maximum ids for DR and MR packets is received.
		 * The messages are updated in the chat thread.
		 */
		else if (HikePubSub.BULK_MESSAGE_DELIVERED_READ.equals(type))
		{
			if(messages == null || messages.isEmpty())
			{
				return;
			}
			Map<String, PairModified<PairModified<Long, Set<String>>, Long>> messageStatusMap = (Map<String, PairModified<PairModified<Long, Set<String>>, Long>>) object;
			PairModified<PairModified<Long, Set<String>>, Long> pair = messageStatusMap.get(mConversation.getMsisdn());
			if (pair != null)
			{
				long mrMsgId = (long) pair.getFirst().getFirst();
				long drMsgId = (long) pair.getSecond();
				if (mrMsgId > drMsgId)
				{
					drMsgId = mrMsgId;
				}

				if (mConversation instanceof GroupConversation)
				{
					for ( String msisdn : pair.getFirst().getSecond())
					{
						((GroupConversation)mConversation).updateReadByList(msisdn, mrMsgId);
					}
				}
				for (int i = messages.size() - 1 ; i>=0; i--)
				{
					ConvMessage msg = messages.get(i);
					if (msg != null && msg.isSent())
					{
						long id = msg.getMsgID();
						if (id <= mrMsgId)
						{
							if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED_READ.ordinal()))
							{
								msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
								removeFromMessageMap(msg);
							}
							else
							{
								break;
							}
						}
						else if (id <= drMsgId)
						{
							if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED.ordinal()))
							{
								msg.setState(ConvMessage.State.SENT_DELIVERED);
							}
						}
					}
				}
				runOnUiThread(mUpdateAdapter);
			}
		}else if(HikePubSub.CONV_META_DATA_UPDATED.equals(type))
		{			
			if(mConversation.getMsisdn().equals(((MetaData)object).getGroupId()))
			{
				mConversation.setMetaData((MetaData) object);
			}
		}
		else if (HikePubSub.ClOSE_PHOTO_VIEWER_FRAGMENT.equals(type))
		{

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG, true);
				}
			});
		}

		else if (HikePubSub.STICKER_CATEGORY_MAP_UPDATED.equals(type))
		{
			if(stickerAdapter == null)
			{
				return;
			}
				runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						stickerAdapter.instantiateStickerList();
						stickerAdapter.notifyDataSetChanged();
					}
				});
		}
		else if (HikePubSub.STICKER_FTUE_TIP.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					shouldShowStickerFtueTip();
				}
			});
		}
        else if (HikePubSub.MULTI_MESSAGE_DB_INSERTED.equals(type)) {
            List<Pair<ContactInfo, ConvMessage>> pairList = (List<Pair<ContactInfo, ConvMessage>>) object;
            for (final Pair<ContactInfo, ConvMessage> pair : pairList) {
                ContactInfo conInfo = pair.first;
                String msisdn = conInfo.getMsisdn();

                if (msisdn.equals(mContactNumber)) {

                    if (activityVisible && SoundUtils.isTickSoundEnabled(getApplicationContext())) 
                    {
                    	SoundUtils.playSoundFromRaw(getApplicationContext(), R.raw.message_sent);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addMessage(pair.second);
                        }
                    });
                    break;
                }
            }
        }
	}

	private void setStateAndUpdateView(long msgId, boolean updateView)
	{
		/*
		 * This would happen in the case if the events calling this method are called before the conversation is setup.
		 */
		if (mConversation == null || mAdapter == null)
		{
			return;
		}
		ConvMessage msg = findMessageById(msgId);
		
		/*
		 * This is a hackish check. For some cases we were getting convMsg in
		 * another user's messageMap. which should not happen ideally. that was
		 * leading to showing hikeOfflineTip in wrong ChatThread.
		 */
		if(msg == null || TextUtils.isEmpty(msg.getMsisdn())  || !msg.getMsisdn().equals(mContactNumber))
		{
			Logger.i("ChatThread", "We are getting a wrong msisdn convMessage object in " + mContactNumber + " ChatThread");
			return;
		}	
		
		if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_CONFIRMED.ordinal()))
		{
			if (activityVisible && (!msg.isTickSoundPlayed()) && SoundUtils.isTickSoundEnabled(getApplicationContext()))
			{
				SoundUtils.playSoundFromRaw(getApplicationContext(), R.raw.message_sent);
			}
			msg.setTickSoundPlayed(true);
			msg.setState(ConvMessage.State.SENT_CONFIRMED);
			if (!(mConversation instanceof GroupConversation) && mConversation.isOnhike())
			{
				if (!msg.isSMS() && !msg.isBroadcastMessage())
				{
					if(mAdapter != null)
					{
						mAdapter.addToUndeliverdMessage(msg);
					}
				}
			}
			if(updateView)
			{
				runOnUiThread(mUpdateAdapter);
			}
		}
	}

	public boolean isContactOnline()
	{
		return isOnline;
	}

	private void updateAdapter()
	{
		runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				mUpdateAdapter.run();
			}
		});
	}

	private ConvMessage findMessageById(long msgID)
	{
		if (messageMap == null)
			return null;

		return messageMap.get(msgID);
	}

	public String getContactNumber()
	{
		return mContactNumber;
	}

	@Override
	public void afterTextChanged(Editable editable)
	{
		if (mConversation == null)
		{
			return;
		}

		/* only update the chat metadata if this is an SMS chat */
		if (!mConversation.isOnhike())
		{
			updateChatMetadata();
		}
		if (hashWatcher != null)
		{
			hashWatcher.afterTextChanged(editable);
		}
	}

	/* must be called on the UI Thread */
	private void updateChatMetadata()
	{
		mMetadataNumChars.setVisibility(View.VISIBLE);

		boolean nativeSmsPref = Utils.getSendSmsPref(this);

		if (mCredits <= 0 && !nativeSmsPref)
		{
			zeroCredits();
		}
		else
		{
			nonZeroCredits();

			if (mComposeView.getLineCount() > 2)
			{
				mMetadataNumChars.setVisibility(View.VISIBLE);
				int length = mComposeView.getText().length();
				// set the max sms length to a length appropriate to the number
				// of characters we have
				int charNum = length % 140;
				int numSms = ((int) (length / 140)) + 1;
				String charNumString = Integer.toString(charNum);
				SpannableString ss = new SpannableString(charNumString + "/#" + Integer.toString(numSms));
				ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.send_green)), 0, charNumString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				mMetadataNumChars.setText(ss);
			}
			else
			{
				mMetadataNumChars.setVisibility(View.INVISIBLE);
			}
		}
	}

	public void onTitleIconClick(View v)
	{
		if (v.getId() == R.id.info_layout || v.getId() == R.id.group_info_layout)
		{
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.I_BUTTON);
			showOverlay(false);
		}
	}

	private void zeroCredits()
	{
		Logger.d(getClass().getSimpleName(), "Zero credits");
		mSendBtn.setEnabled(false);

		if (!TextUtils.isEmpty(mComposeView.getText()))
		{
			mComposeView.setText("");
		}
		if (!(mConversation instanceof GroupConversation))
		{
			mComposeView.setHint("0 Free SMS left...");
			mComposeView.setEnabled(false);
			findViewById(R.id.info_layout).setVisibility(View.VISIBLE);
			findViewById(R.id.emo_btn).setVisibility(View.GONE);
		}
		else
		{
			findViewById(R.id.emo_btn).setVisibility(View.VISIBLE);
		}

		boolean show = mConversationDb.wasOverlayDismissed(mConversation.getMsisdn());
		if (!show)
		{
			showOverlay(false);
		}
		if (!(mConversation instanceof GroupConversation))
		{
			findViewById(R.id.emo_btn).setEnabled(false);
			findViewById(R.id.sticker_btn).setEnabled(false);
		}
		if (tipView != null && tipView.getVisibility() == View.VISIBLE)
		{
			Object tag = tipView.getTag();
			
			if (tag instanceof TipType && ((TipType)tag == TipType.EMOTICON))
			{
				HikeTip.closeTip(TipType.EMOTICON, tipView, prefs);
				tipView = null;
			}
		}
	}

	private void nonZeroCredits()
	{
		Logger.d(getClass().getSimpleName(), "Non Zero credits");
		if (!mComposeView.isEnabled())
		{
			if (!TextUtils.isEmpty(mComposeView.getText()))
			{
				mComposeView.setText("");
			}
			mComposeView.setEnabled(true);
		}
		findViewById((mConversation instanceof GroupConversation) ? R.id.group_info_layout : R.id.info_layout).setVisibility(View.GONE);
		findViewById(R.id.emo_btn).setVisibility(View.VISIBLE);

		findViewById(R.id.emo_btn).setEnabled(true);
		findViewById(R.id.sticker_btn).setEnabled(true);

		if (!blockOverlay)
		{
			hideOverlay();
		}
	}

	private void showSMSCounter()
	{
		slideUp = AnimationUtils.loadAnimation(ChatThread.this, R.anim.slide_up_noalpha);
		slideUp.setDuration(2000);

		slideDown = AnimationUtils.loadAnimation(ChatThread.this, R.anim.slide_down_noalpha);
		slideDown.setDuration(2000);
		slideDown.setStartOffset(2000);

		if (smsCount == null)
		{
			smsCount = (TextView) findViewById(R.id.sms_counter);
		}
		smsCount.setBackgroundColor(getResources().getColor(mAdapter.isDefaultTheme() ? R.color.updates_text : R.color.chat_thread_indicator_bg_custom_theme));
		smsCount.setAnimation(slideUp);
		smsCount.setVisibility(View.VISIBLE);
		smsCount.setText(mCredits + " " + getString(R.string.sms_left));

		slideUp.setAnimationListener(new AnimationListener()
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
				smsCount.setAnimation(slideDown);
				smsCount.setVisibility(View.INVISIBLE);
			}
		});
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int before, int count)
	{
		if(hashWatcher!=null){
			hashWatcher.onTextChanged(s, start, before, count);
		}
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	}

	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent keyEvent)
	{
		if (mConversation == null)
		{
			return false;
		}

		if ((view == mComposeView)
				&& ((actionId == EditorInfo.IME_ACTION_SEND) || ((keyEvent != null) && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)
						&& (keyEvent.getAction() != KeyEvent.ACTION_UP) && (config.keyboard != Configuration.KEYBOARD_NOKEYS))))
		{
			boolean ret = mSendBtn.performClick();
			Utils.hideSoftKeyboard(this, mComposeView);
			return ret;
		}
		return false;
	}

	private void addMessage(ConvMessage convMessage)
	{
		addMessage(convMessage, false);
	}
	
	private void addMessage(ConvMessage convMessage, boolean playPinAnim)
	{
		if (messages != null && mAdapter != null)
		{
			TypingNotification typingNotification = null;
			/*
			 * If we were showing the typing bubble, we remove it from the add the new message and add the typing bubble back again
			 */
			if (!messages.isEmpty() && messages.get(messages.size() - 1).getTypingNotification() != null)
			{
				typingNotification = messages.get(messages.size() - 1).getTypingNotification();
				messages.remove(messages.size() - 1);
			}
			if (mConversation instanceof GroupConversation && convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
			{
				showImpMessage(convMessage, playPinAnim ? R.anim.up_down_fade_in : -1);
			}
			mAdapter.addMessage(convMessage);

			if (mConversation instanceof GroupConversation)
			{
				if (convMessage.isSent())
				{
					((GroupConversation) mConversation).setupReadByList(null, convMessage.getMsgID());
				}
			}
			addtoMessageMap(messages.size() - 1 ,messages.size());

			// Reset this boolean to load more messages when the user scrolls to
			// the top
			reachedEnd = false;

			/*
			 * We add the typing notification back if the message was sent by the user or someone in the group is still typing.
			 */
			if (convMessage.getTypingNotification() == null && typingNotification != null)
			{
				if (convMessage.isSent())
				{
					mAdapter.addMessage(new ConvMessage(typingNotification));
				}
				else if (mConversation instanceof GroupConversation)
				{
					if (!((GroupTypingNotification) typingNotification).getGroupParticipantList().isEmpty())
					{
						Logger.d("TypingNotification", "Size in chat thread: " + ((GroupTypingNotification) typingNotification).getGroupParticipantList().size());
						mAdapter.addMessage(new ConvMessage(typingNotification));
					}
				}
			}

			mAdapter.notifyDataSetChanged();

			/*
			 * Don't scroll to bottom if the user is at older messages. It's possible that the user might be reading them.
			 */
			if (((convMessage != null && !convMessage.isSent()) || convMessage == null) && mConversationsView.getLastVisiblePosition() < messages.size() - 4)
			{
				if (convMessage.getTypingNotification() == null
						&& (convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO || convMessage.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE))
				{
					showUnreadCountIndicator();
				}
				return;
			}
			else
			{
				mConversationsView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
			}
			/*
			 * Resetting the transcript mode once the list has scrolled to the bottom.
			 */
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mConversationsView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
				}
			});
		}
	}
	
	/**
	 * Adds a complete list of messages at the end of the messages list and updates the UI at once
	 * 
	 * @param messageList
	 * 			The list of messages to be added.
	 */
	private void addBulkMessages(LinkedList<ConvMessage> messageList)
	{
		if (messages != null && mAdapter != null)
		{
			TypingNotification typingNotification = null;
			/*
			 * If we were showing the typing bubble, we remove it from the add the new message and add the typing bubble back again
			 */
			if (!messages.isEmpty() && messages.get(messages.size() - 1).getTypingNotification() != null)
			{
				typingNotification = messages.get(messages.size() - 1).getTypingNotification();
				messages.remove(messages.size() - 1);
			}
			mAdapter.addMessages(messageList, messages.size());

			// Reset this boolean to load more messages when the user scrolls to
			// the top
			reachedEnd = false;

			ConvMessage convMessage = messageList.get(messageList.size() - 1);
			/*
			 * We add the typing notification back if the message was sent by the user or someone in the group is still typing.
			 */
			if (typingNotification != null)
			{
				if (convMessage.isSent())
				{
					mAdapter.addMessage(new ConvMessage(typingNotification));
				}
				else if (mConversation instanceof GroupConversation)
				{
					if (!((GroupTypingNotification) typingNotification).getGroupParticipantList().isEmpty())
					{
						Logger.d("TypingNotification", "Size in chat thread: " + ((GroupTypingNotification) typingNotification).getGroupParticipantList().size());
						mAdapter.addMessage(new ConvMessage(typingNotification));
					}
				}
			}
			mAdapter.notifyDataSetChanged();

			/*
			 * Don't scroll to bottom if the user is at older messages. It's possible that the user might be reading them.
			 */
			if (((convMessage != null && !convMessage.isSent()) || convMessage == null) && mConversationsView.getLastVisiblePosition() < messages.size() - 4)
			{
				if (convMessage.getTypingNotification() == null
						&& (convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO || convMessage.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE))
				{
					showUnreadCountIndicator(messageList.size());
				}
				return;
			}
			else
			{
				mConversationsView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
			}
			/*
			 * Resetting the transcript mode once the list has scrolled to the bottom.
			 */
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mConversationsView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
				}
			});
		}
	}

	private void showUnreadCountIndicator()
	{
		unreadMessageCount++;
		// fast scroll indicator and unread message should not show
		// simultaneously.
		bottomFastScrollIndicator.setVisibility(View.GONE);
		unreadMessageIndicator.setVisibility(View.VISIBLE);
		TextView indicatorText = (TextView) findViewById(R.id.indicator_text);
		indicatorText.setVisibility(View.VISIBLE);
		if (unreadMessageCount == 1)
		{
			indicatorText.setText(getResources().getString(R.string.one_new_message));
		}
		else
		{
			indicatorText.setText(getResources().getString(R.string.num_new_messages, unreadMessageCount));
		}
	}
	
	private void showUnreadCountIndicator(int unreadCount)
	{
		unreadMessageCount += unreadCount;
		// fast scroll indicator and unread message should not show
		// simultaneously.
		bottomFastScrollIndicator.setVisibility(View.GONE);
		unreadMessageIndicator.setVisibility(View.VISIBLE);
		TextView indicatorText = (TextView) findViewById(R.id.indicator_text);
		indicatorText.setVisibility(View.VISIBLE);
		if (unreadMessageCount == 1)
		{
			indicatorText.setText(getResources().getString(R.string.one_new_message));
		}
		else
		{
			indicatorText.setText(getResources().getString(R.string.num_new_messages, unreadMessageCount));
		}
	}

	private void hideUnreadCountIndicator()
	{
		unreadMessageCount = 0;
		unreadMessageIndicator.setVisibility(View.GONE);
	}

	private void showFastScrollIndicator()
	{
		if (unreadMessageIndicator.getVisibility() == View.GONE)
		{
			bottomFastScrollIndicator.setVisibility(View.VISIBLE);
		}
	}

	private void hideFastScrollIndicator()
	{
		if (bottomFastScrollIndicator != null)
		{
			bottomFastScrollIndicator.setVisibility(View.GONE);
		}
	}

	private void hideUpFastScrollIndicator()
	{
		if (upFastScrollIndicator != null)
		{
			upFastScrollIndicator.setVisibility(View.GONE);
		}
	}

	private void deleteMessagesFromDb(ArrayList<Long> msgIds, boolean deleteMediaFromPhone)
	{
		//TODO if last message is typing notification we will get wrong result here
		boolean isLastMessage = (msgIds.contains(messages.get(messages.size() - 1).getMsgID()));
		Bundle bundle = new Bundle();
		bundle.putBoolean(HikeConstants.Extras.IS_LAST_MESSAGE, isLastMessage);
		bundle.putString(HikeConstants.Extras.MSISDN, mContactNumber);
		bundle.putBoolean(HikeConstants.Extras.DELETE_MEDIA_FROM_PHONE, deleteMediaFromPhone);
		mPubSub.publish(HikePubSub.DELETE_MESSAGE, new Pair<ArrayList<Long>, Bundle>(msgIds, bundle));
	}
	
	private void deleteMessages(ArrayList<Long> msgIds, boolean deleteMediaFromPhone)
	{
		for (long msgId : msgIds)
		{
			for (ConvMessage convMessage : messages)
			{
				if (convMessage.getMsgID() == msgId)
				{
					deleteMessage(convMessage, deleteMediaFromPhone);
					break;
				}
			}
		}
		mAdapter.notifyDataSetChanged();
	}
	
	/*
	 * 1. remove message from chat thread and db
	 * 2. remove message from offline messages set of messagesAdapter
	 * 3. if ongoing file transfer message than cancel the task
	 */
	private void deleteMessage(ConvMessage convMessage, boolean deleteMediaFromPhone)
	{
		mAdapter.removeMessage(convMessage);
		if (!convMessage.isSMS() && convMessage.getState() == State.SENT_CONFIRMED)
		{
			mAdapter.removeFromUndeliverdMessage(convMessage);
			if (mAdapter.isSelected(convMessage))
			{
				mAdapter.toggleSelection(convMessage);
			}
		}

		if (convMessage.isFileTransferMessage())
		{
			// @GM cancelTask has been changed
			HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
            String key = hikeFile.getFileKey();
			File file = hikeFile.getFile();
			if(deleteMediaFromPhone && hikeFile != null)
			{
				hikeFile.delete(getApplicationContext());
			}
            HikeConversationsDatabase.getInstance().reduceRefCount(key);
			FileTransferManager.getInstance(getApplicationContext()).cancelTask(convMessage.getMsgID(), file, convMessage.isSent(), hikeFile.getFileSize());
			mAdapter.notifyDataSetChanged();
		}

        if (convMessage.getMessageType() == MESSAGE_TYPE.CONTENT){
            int numberOfMediaComponents = convMessage.platformMessageMetadata.mediaComponents.size();
            for (int i = 0; i < numberOfMediaComponents; i++){
                CardComponent.MediaComponent mediaComponent = convMessage.platformMessageMetadata.mediaComponents.get(i);
                HikeConversationsDatabase.getInstance().reduceRefCount(mediaComponent.getKey());
            }
        }

        if (convMessage.getMessageType() == MESSAGE_TYPE.FORWARD_WEB_CONTENT || convMessage.getMessageType() == MESSAGE_TYPE.WEB_CONTENT)
        {
			String origin = Utils.conversationType(mContactNumber);
			JSONObject json = new JSONObject();
			try
			{
				json.put(HikePlatformConstants.CARD_TYPE, convMessage.webMetadata.getAppName());
				json.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.CARD_DELETE);
				json.put(AnalyticsConstants.ORIGIN, origin);
				json.put(AnalyticsConstants.CHAT_MSISDN, mContactNumber);
				json.put(AnalyticsConstants.CONTENT_ID, convMessage.getContentId());
				HikeAnalyticsEvent.analyticsForCards(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
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


	}

	@Override
	public void onShown()
	{

		if (messages != null)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mConversationsView.setSelection(messages.size() - 1);
				}
			});
		}
		Logger.d(getClass().getSimpleName(), "Keyboard shown");
		// if (emoticonLayout != null && emoticonLayout.getVisibility() == View.VISIBLE)
		// {
		// onEmoticonBtnClicked(null);
		// }
	}

	@Override
	public void onHidden()
	{

	}

	public void onInviteButtonClick(View v)
	{
		inviteUser();
	}

	private void showOverlay(boolean blockOverlay)
	{
		this.blockOverlay = blockOverlay;

		Utils.hideSoftKeyboard(this, mComposeView);

		if (mOverlayLayout.getVisibility() != View.VISIBLE && !isOverlayShowing && hasWindowFocus())
		{
			Animation fadeIn = AnimationUtils.loadAnimation(ChatThread.this, android.R.anim.fade_in);
			mOverlayLayout.setAnimation(fadeIn);
		}
		mOverlayLayout.setVisibility(View.VISIBLE);
		// To prevent the views in the background from being clickable
		mOverlayLayout.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
			}
		});

		TextView message = (TextView) mOverlayLayout.findViewById(R.id.overlay_message);
		Button overlayBtn = (Button) mOverlayLayout.findViewById(R.id.overlay_button);
		ImageView overlayImg = (ImageView) mOverlayLayout.findViewById(R.id.overlay_image);

		mComposeView.setEnabled(false);
		String label = mConversation instanceof GroupConversation ? ((GroupConversation) mConversation).getGroupParticipantFirstNameAndSurname(getMsisdnMainUser()) : mLabel;
		String formatString;
		if (blockOverlay)
		{
			overlayImg.setImageResource(R.drawable.ic_no);
			formatString = getResources().getString(R.string.block_overlay_message);
			overlayBtn.setText(R.string.unblock_title);
		}
		else
		{
			mConversationDb.setOverlay(false, mConversation.getMsisdn());
			formatString = getResources().getString(mConversation instanceof GroupConversation ? R.string.no_credits_gc : R.string.no_credits);
			overlayImg.setImageResource(R.drawable.ic_no_credits);
			overlayBtn.setText(mConversation instanceof GroupConversation ? R.string.invite_to_hike : R.string.invite_now);
			mOverlayLayout.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.INVITE_OVERLAY_DISMISS);
					onOverlayButtonClick(mOverlayLayout);
				}
			});
		}
		/* bold the blocked users name */
		String formatted = String.format(formatString, label);
		SpannableString str = new SpannableString(formatted);
		if (!(mConversation instanceof GroupConversation) || blockOverlay)
		{
			int start = formatString.indexOf("%1$s");
			str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, start + label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		message.setText(str);
	}

	private void toggleConversationMuteViewVisibility(boolean isMuted)
	{
		findViewById(R.id.conversation_mute).setVisibility(isMuted ? View.VISIBLE : View.GONE);
	}

	private void setMuteViewBackground()
	{
		findViewById(R.id.conversation_mute).setBackgroundColor(
				getResources().getColor(mAdapter.isDefaultTheme() ? R.color.updates_text : R.color.chat_thread_indicator_bg_custom_theme));
	}

	private boolean checkNetworkError()
	{
		return HikeMessengerApp.networkError;
	}

	private void showNetworkError(boolean isNetError)
	{
		findViewById(R.id.network_error_chat).setVisibility(isNetError ? View.VISIBLE : View.GONE);
	}

	private void showThemePicker(ChatTheme preSelectedTheme)
	{

		dismissPopupWindow();

		if (attachmentWindow != null && attachmentWindow.getContentView() == emoticonLayout)
		{
			onEmoticonBtnClicked(null, 0, true);
		}
		Utils.hideSoftKeyboard(this, mComposeView);

		attachmentWindow = new PopupWindow(this);

		View parentView = getLayoutInflater().inflate(R.layout.chat_backgrounds, chatLayout, false);

		attachmentWindow.setContentView(parentView);

		GridView attachmentsGridView = (GridView) parentView.findViewById(R.id.attachment_grid);

		TextView chatThemeTip = (TextView) parentView.findViewById(R.id.chat_theme_tip);

		chatThemeTip.setText(mConversation instanceof GroupConversation ? R.string.chat_theme_tip_group : R.string.chat_theme_tip);
		chatThemeTip.setVisibility(mConversation.isOnhike() ? View.VISIBLE : View.GONE);

		temporaryTheme = preSelectedTheme == null ? selectedTheme : preSelectedTheme;

		attachmentsGridView.setNumColumns(getNumColumnsChatThemes());

		final ArrayAdapter<ChatTheme> gridAdapter = new ArrayAdapter<ChatTheme>(this, -1, ChatTheme.values())
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = LayoutInflater.from(ChatThread.this).inflate(R.layout.chat_bg_item, parent, false);
				}
				ChatTheme chatTheme = getItem(position);

				ImageView theme = (ImageView) convertView.findViewById(R.id.theme);
				ImageView animatedThemeIndicator = (ImageView) convertView.findViewById(R.id.animated_theme_indicator);

				animatedThemeIndicator.setVisibility(chatTheme.isAnimated() ? View.VISIBLE : View.GONE);
				theme.setBackgroundResource(chatTheme.previewResId());
				theme.setEnabled(temporaryTheme == chatTheme);

				return convertView;
			}
		};

		attachmentsGridView.setAdapter(gridAdapter);

		int selection = temporaryTheme.ordinal();
		attachmentsGridView.setSelection(selection);

		attachmentsGridView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				temporaryTheme = ChatTheme.values()[position];
				gridAdapter.notifyDataSetChanged();
				Logger.d("ChatThread", "Calling setchattheme from showThemePicker onItemClick");
				setChatTheme(temporaryTheme);
			}
		});

		attachmentWindow.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss()
			{
				temporaryTheme = null;

				Logger.d("ChatThread", "Calling setchattheme from showThemePicker onDismissListener");
				setChatTheme(selectedTheme);

				setupActionBar(false);
				showingChatThemePicker = false;
				invalidateOptionsMenu();

				attachmentWindow = null;
			}
		});

		attachmentsGridView.requestFocus();
		attachmentWindow.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
		attachmentWindow.setOutsideTouchable(false);
		attachmentWindow.setFocusable(true);
		attachmentWindow.setWidth(LayoutParams.MATCH_PARENT);
		attachmentWindow.setHeight(LayoutParams.WRAP_CONTENT);

		try
		{
			attachmentWindow.showAsDropDown(findViewById(R.id.cb_anchor));
		}
		catch (BadTokenException e)
		{
			Logger.e(getClass().getSimpleName(), "Excepetion in ChatThread ChatTheme picker", e);
		}

		FrameLayout viewParent = (FrameLayout) parentView.getParent();
		WindowManager.LayoutParams lp = (WindowManager.LayoutParams) viewParent.getLayoutParams();
		lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

		WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		windowManager.updateViewLayout(viewParent, lp);

		attachmentWindow.setTouchInterceptor(new OnTouchListener()
		{

			@Override
			public boolean onTouch(View view, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_OUTSIDE)
				{
					return true;
				}
				return false;
			}
		});
	}

	private int getNumColumnsChatThemes()
	{
		int width = getResources().getDisplayMetrics().widthPixels;

		int chatThemePaletteMargin = 2 * getResources().getDimensionPixelSize(R.dimen.chat_theme_palette_margin);

		int chatThemeGridMargin = 2 * getResources().getDimensionPixelSize(R.dimen.chat_theme_grid_margin);

		int chatThemeGridWidth = width - chatThemeGridMargin - chatThemePaletteMargin;

		int chatThemeItemWidth = getResources().getDimensionPixelSize(R.dimen.chat_bg_item_width);

		return (int) (chatThemeGridWidth / chatThemeItemWidth);
	}

	private void setChatTheme(ChatTheme chatTheme)
	{
		System.gc();
		if (chatTheme != ChatTheme.DEFAULT)
		{
			backgroundImage.setScaleType(chatTheme.isTiled() ? ScaleType.FIT_XY : ScaleType.CENTER_CROP);
			backgroundImage.setImageDrawable(Utils.getChatTheme(chatTheme, this));
		}
		else
		{
			backgroundImage.setImageResource(chatTheme.bgResId());
		}

		mAdapter.setChatTheme(chatTheme);

		if (!mConversation.isOnhike() && !Utils.isContactInternational(mContactNumber))
		{
			if (!Utils.isKitkatOrHigher())
			{
				/*
				 * Add another item which translates to the SMS toggle option.
				 */
				setupSMSToggleButton();
				findViewById(R.id.sms_toggle_button).setVisibility(View.VISIBLE);
			}
			else
			{
				findViewById(R.id.sms_toggle_button).setVisibility(View.GONE);
			}
		}
		else
		{
			findViewById(R.id.sms_toggle_button).setVisibility(View.GONE);
		}
		setMuteViewBackground();

		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(getResources().getDrawable(chatTheme.headerBgResId()));
		/*
		 *  Workaround to set actionbar background drawable multiple times. Refer SO.
		 */
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
	}

	private void sendChatThemeMessage()
	{
		if(selectedTheme == null)
		{
			Logger.d("ChatThread","selectedTheme is null in sendChatThemeMessage Method");
			return;
		}

		long timestamp = System.currentTimeMillis() / 1000;
		mConversationDb.setChatBackground(mContactNumber, selectedTheme.bgId(), timestamp);

		JSONObject jsonObject = new JSONObject();
		JSONObject data = new JSONObject();

		try
		{
			data.put(HikeConstants.MESSAGE_ID, Long.toString(timestamp));
			data.put(HikeConstants.BG_ID, selectedTheme.bgId());

			jsonObject.put(HikeConstants.DATA, data);
			jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.CHAT_BACKGROUD);
			jsonObject.put(HikeConstants.TO, mConversation.getMsisdn());
			jsonObject.put(HikeConstants.FROM, prefs.getString(HikeMessengerApp.MSISDN_SETTING, ""));

			ConvMessage convMessage = new ConvMessage(jsonObject, mConversation, this, true);
			sendMessage(convMessage);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void setupChatThemeActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.chat_theme_action_bar, null);

		View saveThemeBtn = actionBarView.findViewById(R.id.done_container);
		View closeBtn = actionBarView.findViewById(R.id.close_action_mode);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		ViewGroup closeContainer = (ViewGroup) actionBarView.findViewById(R.id.close_container);

		title.setText(R.string.chat_theme);

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dismissPopupWindow();
			}
		});

		saveThemeBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{

				/*
				 * If the user has selected the same theme, we shouldn't do anything
				 */
				if (temporaryTheme != selectedTheme)
				{
					selectedTheme = temporaryTheme;
					sendChatThemeMessage();

                    if (mConversation.isBotConv())
                    {
                        JSONObject json = new JSONObject();
                        try {
                            json.put(HikeConstants.BG_ID, selectedTheme.bgId());
                            analyticsForBots(HikePlatformConstants.BOT_CHAT_THEME_PICKER, HikePlatformConstants.OVERFLOW_MENU, AnalyticsConstants.CLICK_EVENT, json);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
				}
				dismissPopupWindow();

			}
		});

		actionBar.setCustomView(actionBarView);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);
		saveThemeBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));
	}

	private void setupPinImpMessage(int pinType)
	{
		switch (pinType)
		{
		case HikeConstants.MESSAGE_TYPE.TEXT_PIN:
			setupPinImpMessageTextBased();
			break;
		}
	}

	private void setupPinImpMessageTextBased()
	{
		setupPinImpMessageActionBar();
		showingImpMessagePinCreate = true;
		invalidateOptionsMenu();
		dismissPopupWindow();
		final View content = findViewById(R.id.impMessageCreateView);
		content.setVisibility(View.VISIBLE);
		if(isKeyboardOpen){
			mBottomView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.up_down_lower_part));
		}
		mBottomView.setVisibility(View.GONE);
		content.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.up_down_fade_in));
		mComposeView.requestFocus();
		Utils.showSoftKeyboard(getApplicationContext(), mComposeView);
		mComposeView = (CustomFontEditText) content.findViewById(R.id.messageedittext);
		mComposeView.addTextChangedListener(new EmoticonTextWatcher());
		mComposeView.requestFocus();
		content.findViewById(R.id.emo_btn).setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onEmoticonBtnClicked(v);

			}
		});
		if (tipView != null && tipView.getVisibility() == View.VISIBLE && tipView.getTag() instanceof TipType && (TipType) tipView.getTag() == TipType.PIN)
		{
			tipView.setVisibility(View.GONE);
			HikeTip.closeTip(TipType.PIN, tipView, prefs);
			tipView = null;
		}
		
	}

	private void dismissPinCreateView(int animId)
	{
		if (tipView != null)
		{
			tipView.setVisibility(View.VISIBLE);
		}
		// Utils.hideSoftKeyboard(getApplicationContext(), mComposeView);
		showingImpMessagePinCreate = false;
		setupActionBar(false);
		invalidateOptionsMenu();
		mComposeView = (CustomFontEditText) findViewById(R.id.msg_compose);
		// ChatThread.this.chatLayout.requestFocus();
		mComposeView.requestFocus();
		dismissPopupWindow();
		mBottomView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.down_up_lower_part));
		mBottomView.setVisibility(View.VISIBLE);
		final View v = findViewById(R.id.impMessageCreateView);
		if(animId!=-1){
			Animation an = AnimationUtils.loadAnimation(getApplicationContext(), animId);
			playUpDownAnimation(v);
		}else{
		v.setVisibility(View.GONE);
		}
	}

	private void setupPinImpMessageActionBar()
	{

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.chat_theme_action_bar, null);

		View saveBtn = actionBarView.findViewById(R.id.done_container);
		View closeBtn = actionBarView.findViewById(R.id.close_action_mode);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		TextView saveText = (TextView) actionBarView.findViewById(R.id.save);
		ViewGroup closeContainer = (ViewGroup) actionBarView.findViewById(R.id.close_container);

		title.setText(R.string.create_pin);
		saveText.setText(R.string.pin);
	
		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dismissPinCreateView(R.anim.down_up_up_part);
			}
		});

		saveBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				v.setTag(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
				JSONObject jsonObject = new JSONObject();
				try
				{
					jsonObject.put(HikeConstants.PIN_MESSAGE, 1);
					v.setTag(R.id.message_info, jsonObject);
				}
				catch (JSONException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (!TextUtils.isEmpty(mComposeView.getText().toString().trim()))
				{
					onSendClick(v);
					dismissPinCreateView(-1);
				}
				else
				{
					Toast.makeText(getApplicationContext(), R.string.text_empty_error, Toast.LENGTH_SHORT).show();
				}
			}
		});

		actionBar.setCustomView(actionBarView);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);
		saveBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));

	}

	private void showFilePicker(final ExternalStorageState externalStorageState)
	{
		final boolean canShareLocation = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION);

		final boolean canShareContacts = mConversation.isOnhike();

		final ArrayList<OverFlowMenuItem> optionsList = new ArrayList<OverFlowMenuItem>();

		optionsList.add(new OverFlowMenuItem(getString(R.string.camera_upper_case), 0, R.drawable.ic_attach_camera));
		optionsList.add(new OverFlowMenuItem(getString(R.string.photo), 1, R.drawable.ic_attach_pic));
		optionsList.add(new OverFlowMenuItem(getString(R.string.audio), 3, R.drawable.ic_attach_music));
		optionsList.add(new OverFlowMenuItem(getString(R.string.video), 2, R.drawable.ic_attach_video));
		optionsList.add(new OverFlowMenuItem(getString(R.string.file), 6, R.drawable.ic_attach_file));
		if (canShareContacts)
		{
			optionsList.add(new OverFlowMenuItem(getString(R.string.contact), 5, R.drawable.ic_attach_contact));
		}
		if (canShareLocation)
		{
			optionsList.add(new OverFlowMenuItem(getString(R.string.location_option), 4, R.drawable.ic_attach_location));
		}
		dismissPopupWindow();

		Utils.hideSoftKeyboard(this, mComposeView);

		attachmentWindow = new PopupWindow(this);

		View parentView = getLayoutInflater().inflate(R.layout.attachments, chatLayout, false);

		attachmentWindow.setContentView(parentView);

		GridView attachmentsGridView = (GridView) parentView.findViewById(R.id.attachment_grid);
		attachmentsGridView.setAdapter(new ArrayAdapter<OverFlowMenuItem>(this, R.layout.attachment_item, R.id.text, optionsList)
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = getLayoutInflater().inflate(R.layout.attachment_item, parent, false);
				}
				OverFlowMenuItem menuItem = getItem(position);

				ImageView attachmentImageView = (ImageView) convertView.findViewById(R.id.attachment_icon);
				TextView attachmentTextView = (TextView) convertView.findViewById(R.id.text);

				attachmentImageView.setImageResource(menuItem.getIconRes());
				attachmentTextView.setText(menuItem.getName());

				return convertView;
			}
		});

		attachmentsGridView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				dismissPopupWindow();

				OverFlowMenuItem item = optionsList.get(position);
				int itemKey = item.getKey();

				int requestCode;
				Intent pickIntent = new Intent();
				Intent newMediaFileIntent = null;
				/*
				 * If we're not doing a location/contact transfer, we need an external storage
				 */
				if (itemKey != 4 && itemKey != 5)
				{
					if (externalStorageState == ExternalStorageState.NONE)
					{
						Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
						return;
					}
				}

				switch (itemKey)
				{
				case 0:
					requestCode = HikeConstants.IMAGE_CAPTURE_CODE;
					pickIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					File selectedDir = new File(Utils.getFileParent(HikeFileType.IMAGE, false));
					if (!selectedDir.exists())
					{
						if (!selectedDir.mkdirs())
						{
							Logger.d(getClass().getSimpleName(), "failed to create directory");
							return;
						}
					}
					String fileName = HikeConstants.CAM_IMG_PREFIX + Utils.getOriginalFile(HikeFileType.IMAGE, null);
					selectedFile = new File(selectedDir.getPath() + File.separator + fileName); 

					pickIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(selectedFile));
					/*
					 * For images, save the file path as a preferences since in some devices the reference to the file becomes null.
					 */
					Editor editor = prefs.edit();
					editor.putString(HikeMessengerApp.FILE_PATH, selectedFile.getAbsolutePath());
					editor.commit();
					break;

				case 2:
					requestCode = HikeConstants.VIDEO_TRANSFER_CODE;
					pickIntent.setType("video/*");
					newMediaFileIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
					newMediaFileIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (0.9 * HikeConstants.MAX_FILE_SIZE));
					break;

				case 3:
					requestCode = HikeConstants.AUDIO_TRANSFER_CODE;
					showAudioDialog();
					return;

				case 4:
					requestCode = HikeConstants.SHARE_LOCATION_CODE;
					startActivityForResult(new Intent(ChatThread.this, ShareLocation.class), requestCode);
					return;

				case 5:
					requestCode = HikeConstants.SHARE_CONTACT_CODE;
					pickIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
					startActivityForResult(pickIntent, requestCode);
					return;

				case 6:
					requestCode = HikeConstants.SHARE_FILE_CODE;
					Intent intent = new Intent(ChatThread.this, FileSelectActivity.class);
					intent.putExtra(HikeConstants.Extras.MSISDN, mContactNumber);
					intent.putExtra(HikeConstants.Extras.ON_HIKE, mConversation.isOnhike());
					startActivity(intent);
					return;

				case 1:
				default:
					requestCode = HikeConstants.IMAGE_TRANSFER_CODE;
					Intent imageIntent = new Intent(ChatThread.this, GalleryActivity.class);
					imageIntent.putExtra(HikeConstants.Extras.MSISDN, mContactNumber);
					imageIntent.putExtra(HikeConstants.Extras.ON_HIKE, mConversation.isOnhike());
					startActivity(imageIntent);
					return;
				}

				Intent chooserIntent;
				if (requestCode != HikeConstants.IMAGE_CAPTURE_CODE)
				{
					pickIntent.setAction(Intent.ACTION_PICK);

					chooserIntent = Intent.createChooser(pickIntent, "");
				}
				else
				{
					chooserIntent = pickIntent;
				}

				if (externalStorageState == ExternalStorageState.WRITEABLE)
				{
					/*
					 * Cannot send a file for new videos because of an android issue http://stackoverflow.com/questions/10494839 /verifiyandsetparameter
					 * -error-when-trying-to-record-video
					 */
					if (requestCode == HikeConstants.IMAGE_CAPTURE_CODE)
					{
						if (selectedFile == null)
						{
							Logger.w(getClass().getSimpleName(), "Unable to create file to store media.");
							Toast.makeText(ChatThread.this, ChatThread.this.getResources().getString(R.string.no_external_storage), Toast.LENGTH_LONG).show();
							return;
						}
					}
					if (newMediaFileIntent != null)
					{
						chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { newMediaFileIntent });
					}
				}
				Editor editor = prefs.edit();
				editor.putString(HikeMessengerApp.TEMP_NUM, mContactNumber);
				editor.putString(HikeMessengerApp.TEMP_NAME, mContactName);
				editor.commit();
				startActivityForResult(chooserIntent, requestCode);
			}
		});

		attachmentWindow.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss()
			{
				dismissPopupWindow();
			}
		});

		attachmentWindow.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
		attachmentWindow.setOutsideTouchable(true);
		attachmentWindow.setFocusable(true);
		attachmentWindow.setWidth((int) (Utils.scaledDensityMultiplier * 270));
		attachmentWindow.setHeight(LayoutParams.WRAP_CONTENT);
		attachmentWindow.showAsDropDown(findViewById(R.id.attachment_anchor), -(int) (276 * Utils.scaledDensityMultiplier), -(int) (0.5 * Utils.scaledDensityMultiplier));
	}

	private class AudioActivityInfo
	{
		CharSequence label;

		Drawable icon;

		String packageName;

		String activityName;

		public AudioActivityInfo(CharSequence label, Drawable icon, String packageName, String activityName)
		{
			this.label = label;
			this.icon = icon;
			this.packageName = packageName;
			this.activityName = activityName;
		}
	}

	private void showAudioDialog()
	{
		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("audio/*");
		List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
		final List<AudioActivityInfo> audioActivityList = new ArrayList<AudioActivityInfo>();
		int maxSize = Math.min(list.size(), 2);
		for (int i = 0; i < maxSize; i++)
		{
			ActivityInfo activityInfo = list.get(i).activityInfo;
			audioActivityList.add(new AudioActivityInfo(getPackageManager().getApplicationLabel(activityInfo.applicationInfo), getPackageManager().getApplicationIcon(
					activityInfo.applicationInfo), activityInfo.packageName, activityInfo.name));
		}
		Builder builder = new Builder(this);

		ListAdapter dialogAdapter = new ArrayAdapter<AudioActivityInfo>(this, android.R.layout.select_dialog_item, android.R.id.text1, audioActivityList)
		{

			public View getView(int position, View convertView, ViewGroup parent)
			{
				AudioActivityInfo audioActivityInfo = getItem(position);
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);
				tv.setText(audioActivityInfo.label);
				tv.setCompoundDrawablesWithIntrinsicBounds(audioActivityInfo.icon, null, null, null);
				tv.setCompoundDrawablePadding((int) (15 * Utils.scaledDensityMultiplier));
				return v;
			}
		};
		builder.setAdapter(dialogAdapter, new DialogInterface.OnClickListener()
		{

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				AudioActivityInfo audioActivityInfo = audioActivityList.get(which);
				intent.setClassName(audioActivityInfo.packageName, audioActivityInfo.activityName);
				startActivityForResult(intent, HikeConstants.AUDIO_TRANSFER_CODE);
			}
		});

		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	private enum RecorderState
	{
		IDLE, RECORDING, RECORDED, PLAYING
	}

	/**
	 * Method for displaying the record audio dialog.
	 * 
	 * @param startTime
	 *            If the recording was already ongoing when this method is called, this parameter denotes the time the recording was started
	 */
	private void showRecordingDialog()
	{
		if (attachmentWindow != null && attachmentWindow.getContentView() == emoticonLayout)
		{
			dismissPopupWindow();
		}
		recordingDialog = new Dialog(ChatThread.this, R.style.Theme_CustomDialog);

		recordingDialog.setContentView(R.layout.record_audio_dialog);

		final TextView recordInfo = (TextView) recordingDialog.findViewById(R.id.record_info);
		final ImageView recordImage = (ImageView) recordingDialog.findViewById(R.id.record_img);
		final Button cancelBtn = (Button) recordingDialog.findViewById(R.id.cancel_btn);
		final Button sendBtn = (Button) recordingDialog.findViewById(R.id.send_btn);
		final ImageButton recordBtn = (ImageButton) recordingDialog.findViewById(R.id.btn_record);

		recordBtn.setEnabled(true);

		recordBtn.setImageResource(R.drawable.ic_record_selector);
		sendBtn.setEnabled(false);

		recordingHandler = new Handler();

		recorderState = RecorderState.IDLE;
		// Recording already onGoing
		if (recorder != null)
		{
			initialiseRecorder(recordBtn, recordInfo, recordImage, cancelBtn, sendBtn);
			setupRecordingView(recordInfo, recordImage, recordStartTime);
		}
		// Player is playing the recording
		else if (player != null && selectedFile != null)
		{
			try
			{
				initialisePlayer(recordBtn, recordInfo, recordImage, sendBtn);
			}
			catch (IOException e)
			{
				Logger.e(getClass().getSimpleName(), "Error while playing the recording", e);
				Toast.makeText(getApplicationContext(), R.string.error_play_recording, Toast.LENGTH_SHORT).show();
				setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, 0);
				stopPlayer();
			}
			setUpPlayingRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordStartTime);
		}
		// Recording has been stopped and we have a valid file to be sent
		else if (recorder == null && selectedFile != null)
		{
			setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordedTime);
		}

		recordBtn.setOnTouchListener(new OnTouchListener()
		{

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				int action = event.getAction();
				if (recorderState == RecorderState.RECORDED || recorderState == RecorderState.PLAYING)
				{
					return false;
				}
				switch (action)
				{
				case MotionEvent.ACTION_DOWN:
					if (recording)
					{
						return false;
					}
					recordBtn.setPressed(true);
					// New recording
					if (recorder == null)
					{
						initialiseRecorder(recordBtn, recordInfo, recordImage, cancelBtn, sendBtn);
					}
					try
					{
						recorder.prepare();
						recorder.start();
						recordStartTime = System.currentTimeMillis();
						setupRecordingView(recordInfo, recordImage, recordStartTime);
					}
					catch (IOException e)
					{
						stopRecorder();
						recordingError(true);
						Logger.e(getClass().getSimpleName(), "Failed to start recording", e);
					}
					catch (IllegalStateException e)
					{
						stopRecorder();
						recordingError(true);
						recordingDialog.dismiss();
						Logger.e(getClass().getSimpleName(), "Failed to start recording", e);
					}
					recording = true;

					Utils.blockOrientationChange(ChatThread.this);
					getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					return true;
				case MotionEvent.ACTION_UP:
					if (!recording)
					{
						return false;
					}
					mHandler.postDelayed(new Runnable()
					{

						@Override
						public void run()
						{
							recordBtn.setPressed(false);
							recording = false;
							stopRecorder();
							recordedTime = (System.currentTimeMillis() - recordStartTime) / 1000;
							setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordedTime);
							getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
							Utils.unblockOrientationChange(ChatThread.this);
						}
					}, 1);

					return true;
				}
				return false;
			}
		});
		// recordBtn.setOnClickListener(new OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// switch (recorderState) {
		// case RECORDED:
		// try {
		// initialisePlayer(recordBtn, recordImage, sendBtn);
		// player.prepare();
		// player.start();
		// } catch (IOException e) {
		// Logger.e(getClass().getSimpleName(),
		// "Error while playing the recording", e);
		// Toast.makeText(getApplicationContext(),
		// R.string.error_play_recording,
		// Toast.LENGTH_SHORT).show();
		// setUpPreviewRecordingLayout(recordBtn, recordImage,
		// sendBtn);
		// stopPlayer();
		// }
		// setUpPlayingRecordingLayout(recordBtn, recordInfo,
		// recordImage, sendBtn, System.currentTimeMillis());
		// break;
		// case PLAYING:
		// stopPlayer();
		// setUpPreviewRecordingLayout(recordBtn, recordImage, sendBtn);
		// break;
		// }
		// }
		// });

		cancelBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				recordingDialog.cancel();
			}
		});

		sendBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				recordingDialog.dismiss();
				if (selectedFile == null)
				{
					return;
				}
				initialiseFileTransfer(selectedFile.getPath(), null, HikeFileType.AUDIO_RECORDING, HikeConstants.VOICE_MESSAGE_CONTENT_TYPE, true, recordedTime, false, FTAnalyticEvents.AUDIO_ATTACHEMENT);
			}
		});

		recordingDialog.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				stopRecorder();
				stopPlayer();
				recordingError(false);
			}
		});

		recordingDialog.show();
	}

	private void initialiseRecorder(final ImageButton recordBtn, final TextView recordInfo, final ImageView recordImage, final Button cancelBtn, final Button sendBtn)
	{
		if (recorder == null)
		{
			recorder = new MediaRecorder();
			recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			recorder.setMaxDuration(HikeConstants.MAX_DURATION_RECORDING_SEC * 1000);
			recorder.setMaxFileSize(HikeConstants.MAX_FILE_SIZE);
			selectedFile = Utils.getOutputMediaFile(HikeFileType.AUDIO_RECORDING, null, true);
			recorder.setOutputFile(selectedFile.getPath());
		}
		recorder.setOnErrorListener(new OnErrorListener()
		{
			@Override
			public void onError(MediaRecorder mr, int what, int extra)
			{
				stopRecorder();
				recordingError(true);
			}
		});
		recorder.setOnInfoListener(new OnInfoListener()
		{
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra)
			{
				stopRecorder();
				if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
				{
					recordedTime = (System.currentTimeMillis() - recordStartTime) / 1000;
					setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordedTime);
				}
				else
				{
					recordingError(true);
				}
			}
		});
	}

	private void initialisePlayer(final ImageButton recordBtn, final TextView recordInfo, final ImageView recordImage, final Button sendBtn) throws IOException
	{
		if (player == null)
		{
			player = new MediaPlayer();
			player.setDataSource(selectedFile.getPath());
		}

		player.setOnCompletionListener(new OnCompletionListener()
		{
			@Override
			public void onCompletion(MediaPlayer mp)
			{
				setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordedTime);
				stopPlayer();
			}
		});
	}

	private void stopPlayer()
	{
		if (updateRecordingDuration != null)
		{
			updateRecordingDuration.stopUpdating();
			updateRecordingDuration = null;
		}
		if (player != null)
		{
			player.stop();
			player.reset();
			player.release();
			player = null;
		}
	}

	private void setupRecordingView(TextView recordInfo, ImageView recordImage, long startTime)
	{
		recorderState = RecorderState.RECORDING;

		updateRecordingDuration = new UpdateRecordingDuration(recordInfo, recordImage, startTime, R.drawable.ic_recording);
		recordingHandler.post(updateRecordingDuration);
	}

	private void setUpPreviewRecordingLayout(ImageButton recordBtn, TextView recordText, ImageView recordImage, Button sendBtn, long duration)
	{
		recorderState = RecorderState.RECORDED;

		recordBtn.setEnabled(false);
		recordBtn.setImageResource(R.drawable.ic_big_tick);
		recordImage.setImageResource(R.drawable.ic_recorded);
		Utils.setupFormattedTime(recordText, duration);

		sendBtn.setEnabled(true);
	}

	private void setUpPlayingRecordingLayout(ImageButton recordBtn, TextView recordInfo, ImageView recordImage, Button sendBtn, long startTime)
	{
		recorderState = RecorderState.PLAYING;

		sendBtn.setEnabled(true);

		updateRecordingDuration = new UpdateRecordingDuration(recordInfo, recordImage, startTime, 0);
		recordingHandler.post(updateRecordingDuration);
	}

	private class UpdateRecordingDuration implements Runnable
	{
		private long startTime;

		private TextView durationText;

		private ImageView recordImage;

		private boolean keepUpdating = true;

		private boolean imageSet = false;

		private int imageRes;

		public UpdateRecordingDuration(TextView durationText, ImageView iv, long startTime, int imageRes)
		{
			this.durationText = durationText;
			this.recordImage = iv;
			this.startTime = startTime;
			this.imageRes = imageRes;
		}

		public void stopUpdating()
		{
			keepUpdating = false;
		}

		public long getStartTime()
		{
			return startTime;
		}

		@Override
		public void run()
		{
			long timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
			Utils.setupFormattedTime(durationText, timeElapsed);
			if (!imageSet)
			{
				recordImage.setImageResource(imageRes);
				imageSet = true;
			}
			if (keepUpdating)
			{
				recordingHandler.postDelayed(updateRecordingDuration, 500);
			}
		}
	};

	private void stopRecorder()
	{
		if (updateRecordingDuration != null)
		{
			updateRecordingDuration.stopUpdating();
			updateRecordingDuration = null;
		}
		if (recorder != null)
		{
			/*
			 * Catching RuntimeException here to prevent the app from crashing when the the media recorder is immediately stopped after starting.
			 */
			try
			{
				recorder.stop();
			}
			catch (RuntimeException e)
			{
			}
			recorder.reset();
			recorder.release();
			recorder = null;
		}
		recording = false;
	}

	private void recordingError(boolean showError)
	{
		recorderState = RecorderState.IDLE;

		if (showError)
		{
			Toast.makeText(getApplicationContext(), R.string.error_recording, Toast.LENGTH_SHORT).show();
		}
		if (selectedFile == null)
		{
			return;
		}
		if (selectedFile.exists())
		{
			selectedFile.delete();
			selectedFile = null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == HikeConstants.IMAGE_CAPTURE_CODE || requestCode == HikeConstants.IMAGE_TRANSFER_CODE || requestCode == HikeConstants.VIDEO_TRANSFER_CODE || requestCode == HikeConstants.AUDIO_TRANSFER_CODE)
				&& resultCode == RESULT_OK)
		{
			if (requestCode == HikeConstants.IMAGE_CAPTURE_CODE)
			{
				selectedFile = new File(prefs.getString(HikeMessengerApp.FILE_PATH, ""));

				Editor editor = prefs.edit();
				editor.remove(HikeMessengerApp.FILE_PATH);
				editor.commit();
			}
			if (data == null && (selectedFile == null || !selectedFile.exists()))
			{
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				clearTempData();
				return;
			}

			final HikeFileType hikeFileType = (requestCode == HikeConstants.IMAGE_TRANSFER_CODE || requestCode == HikeConstants.IMAGE_CAPTURE_CODE) ? HikeFileType.IMAGE
					: requestCode == HikeConstants.VIDEO_TRANSFER_CODE ? HikeFileType.VIDEO : HikeFileType.AUDIO;

			String filePath = null;
			if (selectedFile != null)
			{
				filePath = selectedFile.getAbsolutePath();
			}
			else if (data == null || data.getData() == null)
			{
				/*
				 * This else condition was added because of a bug in android 4.3 with recording videos. https://code.google.com/p/android/issues/detail?id=57996
				 */
				Toast.makeText(this, R.string.error_capture_video, Toast.LENGTH_SHORT).show();
				clearTempData();
				return;
			}
			else
			{
				Uri selectedFileUri = Utils.makePicasaUri(data.getData());

				if (Utils.isPicasaUri(selectedFileUri.toString()))
				{
					// Picasa image
					FileTransferManager.getInstance(getApplicationContext()).uploadFile(selectedFileUri, hikeFileType, mContactNumber, mConversation.isOnhike());
					clearTempData();
					return;
				}
				// Added this to avoid twice upload on capturing image from camera. 
				//Issue is happening on device Samsung Ace.GT-S5830i due to "onActivityResult" called twice for image capture.
				else if(requestCode == HikeConstants.IMAGE_CAPTURE_CODE)
				{
					if(selectedFile == null || !selectedFile.exists())
						return;
				}
				else
				{
					String fileUriStart = "file://";
					String fileUriString = selectedFileUri.toString();
					if (fileUriString.startsWith(fileUriStart))
					{
						selectedFile = new File(URI.create(Utils.replaceUrlSpaces(fileUriString)));
						/*
						 * Done to fix the issue in a few Sony devices.
						 */
						filePath = selectedFile.getAbsolutePath();
					}
					else
					{
						filePath = Utils.getRealPathFromUri(selectedFileUri, this);
					}
					Logger.d(getClass().getSimpleName(), "File path: " + filePath);
				}
			}

			int attachementType = FTAnalyticEvents.OTHER_ATTACHEMENT;
			switch (requestCode)
			{
				case HikeConstants.IMAGE_CAPTURE_CODE:
					attachementType = FTAnalyticEvents.CAMERA_ATTACHEMENT;
					break;
				case HikeConstants.IMAGE_TRANSFER_CODE:
					attachementType = FTAnalyticEvents.GALLERY_ATTACHEMENT;
					break;
				case HikeConstants.VIDEO_TRANSFER_CODE:
					attachementType = FTAnalyticEvents.VIDEO_ATTACHEMENT;
					break;
				case HikeConstants.AUDIO_TRANSFER_CODE:
					attachementType = FTAnalyticEvents.AUDIO_ATTACHEMENT;
					break;
				default:
					break;
			}
			if (selectedFile != null && requestCode == HikeConstants.IMAGE_CAPTURE_CODE)
			{
				final String fPath = filePath;
				final int atType = attachementType;
				getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + new File(filePath))));
				HikeDialog.showDialog(ChatThread.this, HikeDialog.SHARE_IMAGE_QUALITY_DIALOG, new HikeDialog.HikeDialogListener()
				{
					@Override
					public void onSucess(Dialog dialog)
					{
						initialiseFileTransfer(fPath, null, hikeFileType, null, false, -1, false, atType);
						dialog.dismiss();
					}

					@Override
					public void negativeClicked(Dialog dialog)
					{

					}

					@Override
					public void positiveClicked(Dialog dialog)
					{

					}

					@Override
					public void neutralClicked(Dialog dialog)
					{

					}
				}, (Object[]) new Long[] { (long) 1, selectedFile.length() });
			}else
				initialiseFileTransfer(filePath, null, hikeFileType, null, false, -1, false, attachementType);
		}
		else if (requestCode == HikeConstants.SHARE_LOCATION_CODE && resultCode == RESULT_OK)
		{
			double latitude = data.getDoubleExtra(HikeConstants.Extras.LATITUDE, 0);
			double longitude = data.getDoubleExtra(HikeConstants.Extras.LONGITUDE, 0);
			int zoomLevel = data.getIntExtra(HikeConstants.Extras.ZOOM_LEVEL, 0);

			Logger.d(getClass().getSimpleName(), "Share Location Lat: " + latitude + " long:" + longitude + " zoom: " + zoomLevel);
			initialiseLocationTransfer(latitude, longitude, zoomLevel);
		}
		else if (requestCode == HikeConstants.SHARE_CONTACT_CODE && resultCode == RESULT_OK)
		{
			String contactId = data.getData().getLastPathSegment();
			getContactData(contactId);
		}
		else if (resultCode == RESULT_CANCELED)
		{
			clearTempData();
			Logger.d(getClass().getSimpleName(), "File transfer Cancelled");
			selectedFile = null;
		}
	}

	private void getContactData(String contactId)
	{
		StringBuilder mimeTypes = new StringBuilder("(");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Phone.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Email.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(StructuredPostal.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Event.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(StructuredName.CONTENT_ITEM_TYPE) + ")");

		String selection = Data.CONTACT_ID + " =? AND " + Data.MIMETYPE + " IN " + mimeTypes.toString();

		String[] projection = new String[] { Data.DATA1, Data.DATA2, Data.DATA3, Data.MIMETYPE, Data.DISPLAY_NAME };

		Cursor c = getContentResolver().query(Data.CONTENT_URI, projection, selection, new String[] { contactId }, null);

		int data1Idx = c.getColumnIndex(Data.DATA1);
		int data2Idx = c.getColumnIndex(Data.DATA2);
		int data3Idx = c.getColumnIndex(Data.DATA3);
		int mimeTypeIdx = c.getColumnIndex(Data.MIMETYPE);
		int nameIdx = c.getColumnIndex(Data.DISPLAY_NAME);

		JSONObject contactJson = new JSONObject();

		JSONArray phoneNumbersJson = null;
		JSONArray emailsJson = null;
		JSONArray addressesJson = null;
		JSONArray eventsJson = null;

		List<ContactInfoData> items = new ArrayList<ContactInfoData>();
		String name = null;
		try
		{
			while (c.moveToNext())
			{
				String mimeType = c.getString(mimeTypeIdx);

				if (!contactJson.has(HikeConstants.NAME))
				{
					String dispName = c.getString(nameIdx);
					contactJson.put(HikeConstants.NAME, dispName);
					name = dispName;
				}

				if (Phone.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (phoneNumbersJson == null)
					{
						phoneNumbersJson = new JSONArray();
						contactJson.put(HikeConstants.PHONE_NUMBERS, phoneNumbersJson);
					}

					String type = Phone.getTypeLabel(getResources(), c.getInt(data2Idx), c.getString(data3Idx)).toString();
					String msisdn = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, msisdn);
					phoneNumbersJson.put(data);

					items.add(new ContactInfoData(DataType.PHONE_NUMBER, msisdn, type));
				}
				else if (Email.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (emailsJson == null)
					{
						emailsJson = new JSONArray();
						contactJson.put(HikeConstants.EMAILS, emailsJson);
					}

					String type = Email.getTypeLabel(getResources(), c.getInt(data2Idx), c.getString(data3Idx)).toString();
					String email = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, email);
					emailsJson.put(data);

					items.add(new ContactInfoData(DataType.EMAIL, email, type));
				}
				else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (addressesJson == null)
					{
						addressesJson = new JSONArray();
						contactJson.put(HikeConstants.ADDRESSES, addressesJson);
					}

					String type = StructuredPostal.getTypeLabel(getResources(), c.getInt(data2Idx), c.getString(data3Idx)).toString();
					String address = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, address);
					addressesJson.put(data);

					items.add(new ContactInfoData(DataType.ADDRESS, address, type));
				}
				else if (Event.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (eventsJson == null)
					{
						eventsJson = new JSONArray();
						contactJson.put(HikeConstants.EVENTS, eventsJson);
					}

					String event;
					int eventType = c.getInt(data2Idx);
					if (eventType == Event.TYPE_ANNIVERSARY)
					{
						event = getString(R.string.anniversary);
					}
					else if (eventType == Event.TYPE_OTHER)
					{
						event = getString(R.string.other);
					}
					else if (eventType == Event.TYPE_BIRTHDAY)
					{
						event = getString(R.string.birthday);
					}
					else
					{
						event = c.getString(data3Idx);
					}
					String type = event.toString();
					String eventDate = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, eventDate);
					eventsJson.put(data);

					items.add(new ContactInfoData(DataType.EVENT, eventDate, type));
				}
			}
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
		}

		Logger.d(getClass().getSimpleName(), "Data of contact is : " + contactJson.toString());
		clearTempData();
		showContactDetails(items, name, contactJson, false);
	}

	public void showContactDetails(final List<ContactInfoData> items, final String name, final JSONObject contactInfo, final boolean saveContact)
	{
		contactDialog = new ContactDialog(this, R.style.Theme_CustomDialog);
		contactDialog.setContentView(R.layout.contact_share_info);

		ViewGroup parent = (ViewGroup) contactDialog.findViewById(R.id.parent);
		TextView contactName = (TextView) contactDialog.findViewById(R.id.contact_name);
		ListView contactDetails = (ListView) contactDialog.findViewById(R.id.contact_details);
		Button yesBtn = (Button) contactDialog.findViewById(R.id.btn_ok);
		Button noBtn = (Button) contactDialog.findViewById(R.id.btn_cancel);
		View accountContainer = contactDialog.findViewById(R.id.account_container);
		final Spinner accounts = (Spinner) contactDialog.findViewById(R.id.account_spinner);
		final TextView accountInfo = (TextView) contactDialog.findViewById(R.id.account_info);

		int screenHeight = getResources().getDisplayMetrics().heightPixels;
		int dialogWidth = (int) getResources().getDimension(R.dimen.contact_info_width);
		int dialogHeight = (int) (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? ((3 * screenHeight) / 4)
				: FrameLayout.LayoutParams.MATCH_PARENT);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dialogWidth, dialogHeight);
		lp.topMargin = (int) (5 * Utils.scaledDensityMultiplier);
		lp.bottomMargin = (int) (5 * Utils.scaledDensityMultiplier);

		parent.setLayoutParams(lp);

		contactDialog.setViewReferences(parent, accounts);

		yesBtn.setText(saveContact ? R.string.save : R.string.send);

		if (saveContact)
		{
			accountContainer.setVisibility(View.VISIBLE);
			accounts.setAdapter(new AccountAdapter(getApplicationContext(), getAccountList()));
			if (accounts.getSelectedItem() != null)
			{
				accountInfo.setText(((AccountData) accounts.getSelectedItem()).getName());
			}
			else
			{
				accountInfo.setText(R.string.device);
			}
		}
		else
		{
			accountContainer.setVisibility(View.GONE);
		}

		accountContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				accounts.performClick();
			}
		});

		accounts.setOnItemSelectedListener(new OnItemSelectedListener()
		{

			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
			{
				accountInfo.setText(((AccountData) accounts.getSelectedItem()).getName());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
			}

		});

		contactName.setText(name);
		contactDetails.setAdapter(new ArrayAdapter<ContactInfoData>(getApplicationContext(), R.layout.contact_share_item, R.id.info_value, items)
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				View v = super.getView(position, convertView, parent);
				ContactInfoData contactInfoData = getItem(position);

				TextView header = (TextView) v.findViewById(R.id.info_head);
				header.setText(contactInfoData.getDataSubType());

				TextView details = (TextView) v.findViewById(R.id.info_value);
				details.setText(contactInfoData.getData());
				return v;
			}

		});
		yesBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (saveContact)
				{
					if (accounts.getSelectedItem() != null)
					{
						saveContact(items, accounts, name);
					}
					else
					{
						Utils.addToContacts(items, name, ChatThread.this);
					}
				}
				else
				{
					initialiseContactTransfer(contactInfo);
				}
				contactDialog.dismiss();
			}
		});
		noBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				contactDialog.dismiss();
			}
		});
		contactDialog.show();
	}

	private void initialiseFileTransfer(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
			boolean isForwardingFile, int attachementType)
	{
		clearTempData();
		if (filePath == null)
		{
			Toast.makeText(getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
			return;
		}
		File file = new File(filePath);
		Logger.d(getClass().getSimpleName(), "File size: " + file.length() + " File name: " + file.getName());

		if (HikeConstants.MAX_FILE_SIZE != -1 && HikeConstants.MAX_FILE_SIZE < file.length())
		{
			Toast.makeText(getApplicationContext(), R.string.max_file_size, Toast.LENGTH_SHORT).show();
			return;
		}
		FileTransferManager.getInstance(getApplicationContext()).uploadFile(mContactNumber, file, fileKey, fileType, hikeFileType, isRecording, isForwardingFile,
				mConversation.isOnhike(), recordingDuration, attachementType);
	}

	private void initialiseLocationTransfer(double latitude, double longitude, int zoomLevel)
	{
		clearTempData();
		FileTransferManager.getInstance(getApplicationContext()).uploadLocation(mContactNumber, latitude, longitude, zoomLevel, mConversation.isOnhike());
	}

	private void initialiseContactTransfer(JSONObject contactJson)
	{
		FileTransferManager.getInstance(getApplicationContext()).uploadContact(mContactNumber, contactJson, mConversation.isOnhike());
	}

	private void saveContact(List<ContactInfoData> items, Spinner accountSpinner, String name)
	{

		AccountData accountData = (AccountData) accountSpinner.getSelectedItem();

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		int rawContactInsertIndex = ops.size();

		ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withValue(RawContacts.ACCOUNT_TYPE, accountData.getType())
				.withValue(RawContacts.ACCOUNT_NAME, accountData.getName()).build());

		for (ContactInfoData contactInfoData : items)
		{
			switch (contactInfoData.getDataType())
			{
			case ADDRESS:
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
						.withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE).withValue(StructuredPostal.DATA, contactInfoData.getData())
						.withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_CUSTOM).withValue(StructuredPostal.LABEL, contactInfoData.getDataSubType()).build());
				break;
			case EMAIL:
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE).withValue(Email.DATA, contactInfoData.getData()).withValue(Email.TYPE, Email.TYPE_CUSTOM)
						.withValue(Email.LABEL, contactInfoData.getDataSubType()).build());
				break;
			case EVENT:
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE).withValue(Event.DATA, contactInfoData.getData()).withValue(Event.TYPE, Event.TYPE_CUSTOM)
						.withValue(Event.LABEL, contactInfoData.getDataSubType()).build());
				break;
			case PHONE_NUMBER:
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE).withValue(Phone.NUMBER, contactInfoData.getData()).withValue(Phone.TYPE, Phone.TYPE_CUSTOM)
						.withValue(Phone.LABEL, contactInfoData.getDataSubType()).build());
				break;
			}
		}
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
				.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE).withValue(StructuredName.DISPLAY_NAME, name).build());
		boolean contactSaveSuccessful;
		try
		{
			getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			contactSaveSuccessful = true;
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			contactSaveSuccessful = false;
		}
		catch (OperationApplicationException e)
		{
			e.printStackTrace();
			contactSaveSuccessful = false;
		}
		Toast.makeText(getApplicationContext(), contactSaveSuccessful ? R.string.contact_saved : R.string.contact_not_saved, Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onKey(View view, int keyCode, KeyEvent event)
	{
		if ((event.getAction() == KeyEvent.ACTION_UP) && (keyCode == KeyEvent.KEYCODE_ENTER) && event.isAltPressed())
		{
			mComposeView.append("\n");
			/*
			 * micromax phones appear to fire this event twice. Doing this seems to fix the problem.
			 */
			KeyEvent.changeAction(event, KeyEvent.ACTION_DOWN);
			return true;
		}
		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		// For preventing the tool tip from animating again if its already
		// showing
		outState.putBoolean(HikeConstants.Extras.OVERLAY_SHOWING, mOverlayLayout.getVisibility() == View.VISIBLE);
		if (isEmoticonPalleteVisible())
		{
			dismissPopupWindow();
		}
		outState.putBoolean(HikeConstants.Extras.FILE_TRANSFER_DIALOG_SHOWING, filePickerDialog != null && filePickerDialog.isShowing());
		outState.putBoolean(HikeConstants.Extras.RECORDER_DIALOG_SHOWING, recordingDialog != null && recordingDialog.isShowing());
		outState.putLong(HikeConstants.Extras.RECORDER_START_TIME, updateRecordingDuration != null ? updateRecordingDuration.getStartTime() : 0);
		outState.putLong(HikeConstants.Extras.RECORDED_TIME, recordedTime);
		outState.putInt(HikeConstants.Extras.DIALOG_SHOWING, dialogShowing != null ? dialogShowing.ordinal() : -1);
		if (isActionModeOn)
		{
			outState.putBoolean(HikeConstants.Extras.IS_ACTION_MODE_ON, true);
			outState.putLongArray(HikeConstants.Extras.SELECTED_POSITIONS, mAdapter.getSelectedMsgIdsLongArray());
			outState.putInt(HikeConstants.Extras.SELECTED_NON_FORWARDABLE_MSGS, selectedNonForwadableMsgs);
			outState.putInt(HikeConstants.Extras.SELECTED_NON_TEXT_MSGS, selectedNonTextMsgs);
			outState.putInt(HikeConstants.Extras.SELECTED_CANCELABLE_MSGS, selectedCancelableMsgs);
			outState.putInt(HikeConstants.Extras.SELECTED_SHARABLE_MSGS_COUNT, shareableMessagesCount);
			outState.putInt(HikeConstants.Extras.TOTAL_MSGS_CURRENTLY_LOADED, mAdapter.getCount());
		}
		if (attachmentWindow != null && attachmentWindow.isShowing() && temporaryTheme != null)
		{
			outState.putBoolean(HikeConstants.Extras.CHAT_THEME_WINDOW_OPEN, true);
			outState.putInt(HikeConstants.Extras.SELECTED_THEME, temporaryTheme.ordinal());
		}
		if (showingImpMessagePinCreate)
		{
			outState.putInt(HikeConstants.Extras.PIN_TYPE_SHOWING, HikeConstants.MESSAGE_TYPE.TEXT_PIN);
		}
		super.onSaveInstanceState(outState);
	}

	public void onStickerBtnClicked(View v)
	{
		if((Utils.getExternalStorageState() == ExternalStorageState.NONE))
		{
			Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
			return;
		}
		onEmoticonBtnClicked(v, 0, false);
		if (!prefs.getBoolean(HikeMessengerApp.SHOWN_EMOTICON_TIP, false))
		{
			/*
			 * Added this code to prevent the sticker ftue tip from showing up if the user has already used stickers.
			 */
			if (null != pulsatingDotInflated)
			{
				pulsatingDotInflated.setVisibility(View.GONE);
			}
			Editor editor = prefs.edit();
			editor.putBoolean(HikeMessengerApp.SHOWN_EMOTICON_TIP, true);
			editor.commit();
		}
		if (!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STICKED_BTN_CLICKED_FIRST_TIME, false))
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.STICKED_BTN_CLICKED_FIRST_TIME, true);
			
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_BTN_CLICKED);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
		}
		
		showProductPopup(ProductPopupsConstants.PopupTriggerPoints.STKBUT_BUT.ordinal());
	}

	public void onEmoticonBtnClicked(View v)
	{
		onEmoticonBtnClicked(v, 0, false);
	}

	public void onEmoticonBtnClicked(final View v, int whichSubcategory, boolean backPressed)
	{
		// it is possible that window token is null when activity is rotated, will occur rarely
		View anchor = findViewById(R.id.chatThreadParentLayout);
		if (anchor.getWindowToken() != null)
		{
			if (showingChatThemePicker)
			{
				return;
			}
			// boolean controls whether we switch from sticker to emo or vice versa
			emoticonLayout = emoticonLayout == null ? (ViewGroup) LayoutInflater.from(getApplicationContext()).inflate(R.layout.emoticon_layout, null) : emoticonLayout;

			emoticonViewPager = emoticonViewPager == null ? (ViewPager) emoticonLayout.findViewById(R.id.emoticon_pager) : emoticonViewPager;

			View eraseKey = emoticonLayout.findViewById(R.id.erase_key);
			View shopButton = emoticonLayout.findViewById(R.id.shop_button);

			if (v != null)
			{
				int[] tabDrawables = null;

				if (v.getId() == R.id.sticker_btn)
				{
					if (emoticonType == EmoticonType.STICKERS)
					{
						v.setSelected(false);
						// view not changed , exit with dismiss dialog
						dismissPopupWindow();
						return;
					}

					eraseKey.setVisibility(View.GONE);
					shopButton.setVisibility(View.VISIBLE);

					v.setSelected(true);
					((View)findViewById(R.id.tb_layout)).findViewById(R.id.emo_btn).setSelected(false);
					resetAtomicPopUpKey(HikeMessengerApp.ATOMIC_POP_UP_STICKER);
					if (tipView != null)
					{
						Object tag = tipView.getTag();
						
						if (tag instanceof TipType && ((TipType)tag == TipType.EMOTICON))
						{
							HikeTip.closeTip(TipType.EMOTICON, tipView, prefs);
							
							try
							{
								JSONObject metadata = new JSONObject();
								metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STICKER_FTUE_BTN_CLICK);
								HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
							}
							catch(JSONException e)
							{
								Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
							}

							tipView = null;
						}
					}
					if (emoticonType != EmoticonType.STICKERS)
					{
						emoticonType = EmoticonType.STICKERS;
					}
					final ImageView shopIcon = (ImageView) emoticonLayout.findViewById(R.id.shop_icon_image);
					shopIcon.setImageResource(R.drawable.ic_sticker_shop);

					if(HikeSharedPreferenceUtil.getInstance().getData(StickerManager.SHOW_STICKER_SHOP_BADGE, false))
					{
						emoticonLayout.findViewById(R.id.sticker_shop_badge).setVisibility(View.VISIBLE);
					}
					else
					{
						emoticonLayout.findViewById(R.id.sticker_shop_badge).setVisibility(View.GONE);
					}

					final View animatedBackground = emoticonLayout.findViewById(R.id.animated_backgroud);
					if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_SHOP_ICON_BLUE, false))  //The shop icon would be blue unless the user clicks on it once
					{
						animatedBackground.setVisibility(View.VISIBLE);
						Animation anim = AnimationUtils.loadAnimation(this, R.anim.scale_out_from_mid);
						animatedBackground.startAnimation(anim);

						shopIcon.setAnimation(HikeAnimationFactory.getStickerShopIconAnimation(this));
					}
					
					shopButton.setOnClickListener(new View.OnClickListener()
					{
						
						@Override
						public void onClick(View v)
						{
							if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.SHOWN_SHOP_ICON_BLUE, false))  //The shop icon would be blue unless the user clicks on it once
							{
								HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOWN_SHOP_ICON_BLUE, true);
								animatedBackground.setVisibility(View.GONE);
								animatedBackground.clearAnimation();
								shopIcon.clearAnimation();
								
								try
								{
									JSONObject metadata = new JSONObject();
									metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.STKR_SHOP_BTN_CLICKED);
									HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
								}
								catch(JSONException e)
								{
									Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
								}
							}
							if(HikeSharedPreferenceUtil.getInstance().getData(StickerManager.SHOW_STICKER_SHOP_BADGE, false))  //The shop icon would be blue unless the user clicks on it once
							{
								HikeSharedPreferenceUtil.getInstance().saveData(StickerManager.SHOW_STICKER_SHOP_BADGE, false);
							}
							Intent i = new Intent(ChatThread.this, StickerShopActivity.class);
							startActivity(i);
						}
					});
					
				}
				else
				{
					if (emoticonType == EmoticonType.EMOTICON)
					{
						v.setSelected(false);
						dismissPopupWindow();
						return;
					}

					eraseKey.setVisibility(View.VISIBLE);
					shopButton.setVisibility(View.GONE);

					v.setSelected(true);
					findViewById(R.id.sticker_btn).setSelected(false);
					int offset = 0;
					int emoticonsListSize = 0;
					tabDrawables = new int[] { R.drawable.emo_recent, R.drawable.emo_tab_1_selector, R.drawable.emo_tab_2_selector, R.drawable.emo_tab_3_selector,
							R.drawable.emo_tab_4_selector, R.drawable.emo_tab_5_selector, R.drawable.emo_tab_6_selector, R.drawable.emo_tab_7_selector,
							R.drawable.emo_tab_8_selector, R.drawable.emo_tab_9_selector };
					if (emoticonType != EmoticonType.EMOTICON)
					{
						emoticonType = EmoticonType.EMOTICON;
					}
					emoticonsListSize = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;

					/*
					 * Checking whether we have a few emoticons in the recents category. If not we show the next tab emoticons.
					 */
					if (whichSubcategory == 0)
					{
						int startOffset = offset;
						int endOffset = startOffset + emoticonsListSize;
						int recentEmoticonsSizeReq = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? EmoticonAdapter.MAX_EMOTICONS_PER_ROW_PORTRAIT
								: EmoticonAdapter.MAX_EMOTICONS_PER_ROW_LANDSCAPE;
						int[] recentEmoticons = HikeConversationsDatabase.getInstance().fetchEmoticonsOfType(startOffset, endOffset, recentEmoticonsSizeReq);
						if (recentEmoticons.length < recentEmoticonsSizeReq)
						{
							whichSubcategory++;
						}
					}
					eraseKey.setOnClickListener(new OnClickListener()
					{

						@Override
						public void onClick(View view)
						{
							mComposeView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
						}
					});
				}
				setupEmoticonLayout(emoticonType, whichSubcategory, tabDrawables);
				if (attachmentWindow != null && attachmentWindow.getContentView() == emoticonLayout)
				{
					return;
				}

				// emotype is same , so same view clicked

				int selection = mConversationsView.getLastVisiblePosition();
				attachmentWindow = new PopupWindow(ChatThread.this);
				updateEmoticonPallateHeight();
				attachmentWindow.setContentView(emoticonLayout);
				Log.i("keyboard", "showing at emoticon anchor");
				attachmentWindow.setWidth(android.view.ViewGroup.LayoutParams.MATCH_PARENT);

				attachmentWindow.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
				attachmentWindow.setOutsideTouchable(true);
				attachmentWindow.setFocusable(false);
				attachmentWindow.setOnDismissListener(new OnDismissListener()
				{

					@Override
					public void onDismiss()
					{
						/*
						 * Hiding the black filler palette.
						 */
						findViewById(R.id.sticker_palette_filler).setVisibility(View.GONE);
						resizeMainheight(0, false);
						emoticonType = null;
						attachmentWindow = null;
						v.setSelected(false);
					}
				});
				// attachmentWindow.showAsDropDown(anchor, 0, 0);

				anchor.setVisibility(View.VISIBLE);

				attachmentWindow.showAtLocation(anchor, Gravity.BOTTOM, 0, 0);
				attachmentWindow.setTouchInterceptor(new OnTouchListener()
				{

					@Override
					public boolean onTouch(View view, MotionEvent event)
					{

						// Logger.i("emoticon", "outside click " + event.getX() + ", " + event.getY() + " sticker- " + xy[0] + " , " + xy[1] + ",width- " + st.getWidth()
						// + " and emo == " + xye[0] + " , " + xye[1] + " ,width- " + emo.getWidth());
						if (event.getAction() == MotionEvent.ACTION_OUTSIDE)
						{
							int eventX = (int) event.getX();
							boolean stickerTouch = eatOuterTouchEmoticonPallete(eventX, R.id.sticker_btn);
							if (stickerTouch)
								return true;
							boolean emoTouch = eatOuterTouchEmoticonPallete(eventX, R.id.emo_btn);
							if (emoTouch)
								return true;
							boolean recordingTouch = eatOuterTouchEmoticonPallete(eventX, R.id.send_message);
							if (recordingTouch)
								return true;

							return false;
						}

						return false;
					}
				});

				/*
				 * Setting the current selection to the last message.
				 */
				mConversationsView.post(new Runnable()
				{

					@Override
					public void run()
					{
						mConversationsView.setSelection(messages.size() - 1);
					}
				});
			}

		}
		else
		{
			Logger.d("chatthread", "window token is null -- trying to show emoticon pallette");
			attachmentWindow = null;
			emoticonType = null;
		}

	}

	private boolean eatOuterTouchEmoticonPallete(int eventX, int viewId)
	{
		View st = null;
		if (viewId == R.id.emo_btn)
		{
			st = ((View) findViewById(R.id.tb_layout)).findViewById(R.id.emo_btn);
		}
		else
		{
			st = findViewById(viewId);
		}
		int[] xy = new int[2];
		st.getLocationInWindow(xy);
		// touch over sticker
		if ((eventX >= xy[0] && eventX <= (xy[0] + st.getWidth())))
		{
			return true;
		}
		return false;
	}

	OnPageChangeListener onPageChangeListener = new OnPageChangeListener()
	{

		@Override
		public void onPageSelected(int pageNum)
		{
			Logger.d("ViewPager", "Page number: " + pageNum);
			if (emoticonType == EmoticonType.STICKERS)
			{
				StickerCategory category = stickerAdapter.getCategoryForIndex(pageNum);
				if(category.getState() == StickerCategory.DONE || category.getState() == StickerCategory.DONE_SHOP_SETTINGS)
				{
					category.setState(StickerCategory.NONE);
				}
			}
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2)
		{
		}

		@Override
		public void onPageScrollStateChanged(int arg0)
		{
		}
	};

	public int getCurrentPage()
	{
		if (emoticonViewPager == null || emoticonType != EmoticonType.STICKERS)
		{
			return -1;
		}
		return emoticonViewPager.getCurrentItem();
	}

	public EmoticonType getCurrentEmoticonType()
	{
		return emoticonType;
	}

	private void setupEmoticonLayout(EmoticonType emoticonType, int pageNum, int[] categoryResIds)
	{
		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		if (emoticonType != EmoticonType.STICKERS)
		{
			if (emoticonsAdapter == null)
			{
				emoticonsAdapter = new EmoticonAdapter(this, this, isPortrait, categoryResIds);
			}
			emoticonViewPager.setAdapter(emoticonsAdapter);
		}
		else
		{
			if (stickerAdapter == null)
			{
				stickerAdapter = new StickerAdapter(this);
			}
			emoticonViewPager.setAdapter(stickerAdapter);
		}

		int actualPageNum = pageNum;
		emoticonViewPager.setCurrentItem(actualPageNum, false);
		emoticonViewPager.invalidate();

		iconPageIndicator = (StickerEmoticonIconPageIndicator) emoticonLayout.findViewById(R.id.icon_indicator);
		iconPageIndicator.setViewPager(emoticonViewPager);
		iconPageIndicator.setOnPageChangeListener(onPageChangeListener);
		iconPageIndicator.notifyDataSetChanged();
		iconPageIndicator.setCurrentItem(actualPageNum);

		onPageChangeListener.onPageSelected(pageNum);
	}

	private void toggleGroupLife(boolean alive)
	{
		((GroupConversation) mConversation).setGroupAlive(alive);
		this.mSendBtn.setEnabled(false);
		this.mComposeView.setVisibility(alive ? View.VISIBLE : View.INVISIBLE);
		findViewById(R.id.emo_btn).setEnabled(alive ? true : false);
		findViewById(R.id.sticker_btn).setEnabled(alive ? true : false);
		if (emoticonLayout != null && emoticonLayout.getVisibility() == View.VISIBLE)
		{
			onEmoticonBtnClicked(null);
		}
	}

	private String getMsisdnMainUser()
	{
		return mConversation instanceof GroupConversation ? ((GroupConversation) mConversation).getGroupOwner() : mContactNumber;
	}

	@Override
	public void onFinish(boolean success)
	{
	}

	public void sendPoke()
	{
		ConvMessage convMessage = Utils.makeConvMessage(mContactNumber, getString(R.string.poke_msg), isConversationOnHike());

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
		sendMessage(convMessage);

		boolean vibrate = false;
		if (mConversation != null)
		{
			if (mConversation instanceof GroupConversation)
			{
				if (!((GroupConversation) mConversation).isMuted())
				{
					vibrate = true;
				}
			}
			else
			{
				vibrate = true;
			}
		}

		if (vibrate)
		{
			Utils.vibrateNudgeReceived(this);
		}
	}

	public void sendSticker(Sticker sticker, String source)
	{
		sendSticker(sticker, null, source);
	}

	public void sendSticker(Sticker sticker, String categoryIdIfUnknown, String source)
	{
		ConvMessage convMessage = Utils.makeConvMessage(mContactNumber, "Sticker", isConversationOnHike());

		JSONObject metadata = new JSONObject();
		try
		{
			String categoryId;
			categoryId = sticker.getCategoryId();
			
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
		sendMessage(convMessage);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		return gestureDetector.onTouchEvent(event);
	}

	SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener()
	{

		@Override
		public boolean onDoubleTap(MotionEvent e)
		{
			Logger.i("chatthread", "double tap");
			if (isActionModeOn || isHikeToOfflineMode)
			{
				return false;
			}
			if (mConversation instanceof BroadcastConversation)
			{
				return false;
			}
			if (mConversation instanceof GroupConversation)
			{
				if (!((GroupConversation) mConversation).getIsGroupAlive())
				{
					return false;
				}
			}
			if (!mConversation.isOnhike() && mCredits <= 0)
			{
				boolean nativeSmsPref = Utils.getSendSmsPref(ChatThread.this);
				if (!nativeSmsPref)
				{
					return false;
				}
			}
			sendPoke();
			return true;
		}

	};

	@Override
	public void onScroll(AbsListView view, final int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (!reachedEnd && !loadingMoreMessages && messages != null && !messages.isEmpty() && firstVisibleItem <= HikeConstants.MIN_INDEX_TO_LOAD_MORE_MESSAGES)
		{
			final int startIndex = messages.get(0).isBlockAddHeader() ? 1 : 0;
			
			/*
			 * This should only happen in the case where the user starts a new chat and gets a typing notification.
			 */
			/* messageid -1:
			 * Algo is message id can not be -1 here, -1 means message has been added in UI and not been inserted in DB which is being done on pubsub thread. It will happen for new
			 * added messages. Once message is succesfully inserted in DB, messageID will be updated and will be reflected here.
			 * Bug was : There is data race between  this async task and pubsub, it was happening that message id is -1 when async task is just started, so async task fetches data from DB and results in duplicate sent messages
			 */
			if (messages.size() <= startIndex || messages.get(startIndex) == null || messages.get(startIndex).getMsgID()==-1)
			{
				return;
			}
			final long firstMessageId = messages.get(startIndex).getMsgID();
			loadingMoreMessages = true;

			final String msisdn = mContactNumber;

			

			final Conversation conversation = mConversation;
			
			AsyncTask<Void, Void, List<ConvMessage>> asyncTask = new AsyncTask<Void, Void, List<ConvMessage>>()
			{

				@Override
				protected List<ConvMessage> doInBackground(Void... params)
				{
					return mConversationDb
							.getConversationThread(msisdn, HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME, conversation, firstMessageId);
				}

				@Override
				protected void onPostExecute(List<ConvMessage> result)
				{
					/*
					 * Making sure that we are still in the same conversation.
					 */
					if (!msisdn.equals(mContactNumber))
					{
						return;
					}
					if (!result.isEmpty())
					{
						int scrollOffset = 0;

						if (mConversationsView.getChildAt(0) != null)
						{
							scrollOffset = mConversationsView.getChildAt(0).getTop();
						}

						mAdapter.addMessages(result, startIndex);
						addtoMessageMap(startIndex, startIndex + result.size());
						mAdapter.notifyDataSetChanged();
						mConversationsView.setSelectionFromTop(firstVisibleItem + result.size(), scrollOffset);
					}
					else
					{
						/*
						 * This signifies that we've reached the end. No need to query the db anymore unless we add a new message.
						 */
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

		if (unreadMessageIndicator.getVisibility() == View.VISIBLE && mConversationsView.getLastVisiblePosition() > messages.size() - unreadMessageCount - 2)
		{
			hideUnreadCountIndicator();
		}

		if (view.getLastVisiblePosition() < messages.size() - HikeConstants.MAX_FAST_SCROLL_VISIBLE_POSITION)
		{
			if (currentFirstVisibleItem < firstVisibleItem)
			{
				showFastScrollIndicator();
				hideUpFastScrollIndicator();
			}
			else if (currentFirstVisibleItem > firstVisibleItem)
			{
				hideFastScrollIndicator();
				/*
				 * if user is viewing message less than certain position in chatthread we should not show topfast scroll.
				 */
				if (firstVisibleItem > HikeConstants.MAX_FAST_SCROLL_VISIBLE_POSITION)
				{
					upFastScrollIndicator.setVisibility(View.VISIBLE);
				}
				else
				{
					hideUpFastScrollIndicator();
				}
			}
		}
		else
		{
			hideFastScrollIndicator();
			hideUpFastScrollIndicator();
		}
		currentFirstVisibleItem = firstVisibleItem;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		Logger.d("scroll", "Message Adapter Scrolled State: " + scrollState);
		if (bottomFastScrollIndicator.getVisibility() == View.VISIBLE)
		{
			if (view.getLastVisiblePosition() >= messages.size() - HikeConstants.MAX_FAST_SCROLL_VISIBLE_POSITION)
			{
				hideFastScrollIndicator();
			}
			else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE)
			{
				mHandler.postDelayed(new Runnable()
				{

					@Override
					public void run()
					{
						hideFastScrollIndicator();
					}
				}, 2000);
			}
		}
		if (upFastScrollIndicator.getVisibility() == View.VISIBLE)
		{
			if (view.getFirstVisiblePosition() <= HikeConstants.MAX_FAST_SCROLL_VISIBLE_POSITION)
			{
				hideUpFastScrollIndicator();
			}
			else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE)
			{
				mHandler.postDelayed(new Runnable()
				{

					@Override
					public void run()
					{
						hideUpFastScrollIndicator();
					}
				}, 2000);
			}
		}
		
		if(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE){
			mAdapter.setIsListFlinging(false);
		}else{
			mAdapter.setIsListFlinging(true);
		}
	}

	// We depend on listflinging state to download hd thumbnail of image
	// call this function when listview stops flinging, so we iterate through
	// the visible items and call getview just to make sure imageloader loads thumbnail properly
	public void notifyFileThumbnailDataSetChanged()
	{
		int start = mConversationsView.getFirstVisiblePosition();
		int last = mConversationsView.getLastVisiblePosition();
		for (int i = start, j = last; i <= j; i++)
		{
			Object object = mConversationsView.getItemAtPosition(i);
			if (object instanceof ConvMessage)
			{
				ConvMessage convMessage = (ConvMessage) object;
				if (convMessage.isFileTransferMessage())
				{
					View view = mConversationsView.getChildAt(i - start);
					// this method call will take care of thumbnail loading when lv stops flinging.
					mAdapter.getView(i, view, mConversationsView);
					break;
				}
			}
		}
	}

	private List<AccountData> getAccountList()
	{
		Account[] a = AccountManager.get(this).getAccounts();
		// Clear out any old data to prevent duplicates
		List<AccountData> accounts = new ArrayList<AccountData>();

		// Get account data from system
		AuthenticatorDescription[] accountTypes = AccountManager.get(this).getAuthenticatorTypes();

		// Populate tables
		for (int i = 0; i < a.length; i++)
		{
			// The user may have multiple accounts with the same name, so we
			// need to construct a
			// meaningful display name for each.
			String type = a[i].type;
			/*
			 * Only showing the user's google accounts
			 */
			if (!"com.google".equals(type))
			{
				continue;
			}
			String systemAccountType = type;
			AuthenticatorDescription ad = getAuthenticatorDescription(systemAccountType, accountTypes);
			AccountData data = new AccountData(a[i].name, ad, this);
			accounts.add(data);
		}

		return accounts;
	}

	/**
	 * Obtain the AuthenticatorDescription for a given account type.
	 * 
	 * @param type
	 *            The account type to locate.
	 * @param dictionary
	 *            An array of AuthenticatorDescriptions, as returned by AccountManager.
	 * @return The description for the specified account type.
	 */
	private AuthenticatorDescription getAuthenticatorDescription(String type, AuthenticatorDescription[] dictionary)
	{
		for (int i = 0; i < dictionary.length; i++)
		{
			if (dictionary[i].type.equals(type))
			{
				return dictionary[i];
			}
		}
		// No match found
		throw new RuntimeException("Unable to find matching authenticator");
	}

	@Override
	public void startActivity(Intent intent)
	{
		try
		{
			/* Workaround for an HTC issue */
			if (intent.getComponent() != null && ".HtcLinkifyDispatcherActivity".equals(intent.getComponent().getShortClassName()))
				intent.setComponent(null);
			super.startActivity(intent);
		}
		catch (ActivityNotFoundException e)
		{
			super.startActivity(Intent.createChooser(intent, null));
		}
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
				if (mMenu == null || isFragmentAdded(HikeConstants.IMAGE_FRAGMENT_TAG))
				{
					return super.onKeyUp(keyCode, event);
				}
				mMenu.performIdentifierAction(R.id.overflow_menu, 0);
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	private boolean isConversationOnHike()
	{
		return mConversation != null && mConversation.isOnhike();
	}

	private class MessageReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (mAdapter != null && intent.getAction().equals(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED))
			{
				// runOnUiThread(mUpdateAdapter);
				// @GM
				mAdapter.notifyDataSetChanged();
			}
			// else if(intent.getAction().equals(HikePubSub.RESUME_BUTTON_UPDATED))
			// {
			// //Logger.d(getClass().getSimpleName(),"making button visible...1");
			// long msgId = intent.getLongExtra("msgId", -1);
			// if(msgId<0)
			// return;
			// //Logger.d(getClass().getSimpleName(),"making button visible...2");
			// ConvMessage message = null;
			// for(int i=(messages.size()-1); i>=0; i--)
			// {
			// ConvMessage m = messages.get(i);
			// //Logger.d(getClass().getSimpleName(), "comparing  : " +m.getMsgID() +" == " + msgId);
			// if(m != null && m.getMsgID() == msgId)
			// {
			// message = m;
			// break;
			// }
			// }
			// if (message == null)
			// return;
			// //Logger.d(getClass().getSimpleName(),"making button visible...3");
			//
			// if(!message.getResumeButtonVisibility())
			// {
			// Logger.d(getClass().getSimpleName(),"making button visible...DONE");
			// message.setResumeButtonVisibility(true);
			// mAdapter.notifyDataSetChanged();
			// }
			// }
		}
	}

	private class ChatThreadReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(StickerManager.STICKERS_UPDATED) || intent.getAction().equals(StickerManager.MORE_STICKERS_DOWNLOADED) || intent.getAction().equals(StickerManager.STICKERS_DOWNLOADED))
			{
				runOnUiThread(new Runnable()
				{
					public void run()
					{
						/**
						 * We were getting an NPE here.
						 */
						if(iconPageIndicator == null)
						{
							return;
						}
						
						iconPageIndicator.notifyDataSetChanged();
					}
				});
			}
		}
	}

	private void setActionModeOn(boolean isOn)
	{
		isActionModeOn = isOn;
		mAdapter.setActionMode(isOn);
		mAdapter.notifyDataSetChanged();
	}

	public void selectedNonTextMsg(boolean isMsgSelected)
	{
		if (isMsgSelected)
		{
			selectedNonTextMsgs++;
		}
		else
		{
			selectedNonTextMsgs--;
		}
	}

	public void selectedNonForwadableMsg(boolean isMsgSelected)
	{
		if (isMsgSelected)
		{
			selectedNonForwadableMsgs++;
		}
		else
		{
			selectedNonForwadableMsgs--;
		}
	}

	public void selectedCancelableMsg(boolean isMsgSelected)
	{
		if (isMsgSelected)
		{
			selectedCancelableMsgs++;
		}
		else
		{
			selectedCancelableMsgs--;
		}
	}

	private void setupActionModeActionBar()
	{
		if (attachmentWindow != null && attachmentWindow.isShowing())
		{
			dismissPopupWindow();
			attachmentWindow = null;
		}

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.action_mode_action_bar, null);

		View closeBtn = actionBarView.findViewById(R.id.close_action_mode);
		mActionModeTitle = (TextView) actionBarView.findViewById(R.id.title);
		ViewGroup closeContainer = (ViewGroup) actionBarView.findViewById(R.id.close_container);

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				destroyActionMode();
			}
		});
		actionBar.setCustomView(actionBarView);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);

		initializeActionMode();
	}

	private void setActionModeTitle(int count)
	{
		if (mActionModeTitle != null)
		{
			mActionModeTitle.setText(getString(R.string.selected_count, count));
		}
	}

	public boolean onPrepareActionMode(Menu menu)
	{
		if (selectedNonTextMsgs == 0)
		{
			mOptionsList.put(R.id.copy_msgs, true);
		}
		else
		{
			mOptionsList.put(R.id.copy_msgs, false);
		}
		mOptionsList.put(R.id.share_msgs, shareableMessagesCount == 1 && mAdapter.getSelectedCount() == 1);
		if (selectedNonForwadableMsgs > 0)
		{
			mOptionsList.put(R.id.forward_msgs, false);
		}
		else
		{
			mOptionsList.put(R.id.forward_msgs, true);
		}
		if (selectedCancelableMsgs == 1 && mAdapter.getSelectedCount() == 1)
		{
			mOptionsList.put(R.id.action_mode_overflow_menu, true);
		}
		else
		{
			mOptionsList.put(R.id.action_mode_overflow_menu, false);
		}

		for (int i = 0; i < menu.size(); i++)
		{
			MenuItem item = menu.getItem(i);
			if (mOptionsList.get(item.getItemId()) != null && mOptionsList.get(item.getItemId()))
			{
				item.setVisible(true);
			}
			else
			{
				item.setVisible(false);
			}
		}
		return true;
	}

	public boolean initializeActionMode()
	{
		setActionModeOn(true);
		mOptionsList.put(R.id.delete_msgs, true);
		mOptionsList.put(R.id.forward_msgs, true);
		mOptionsList.put(R.id.copy_msgs, true);
		mOptionsList.put(R.id.share_msgs, true);
		mOptionsList.put(R.id.action_mode_overflow_menu, false);
		if (mAdapter.getSelectedCount() > 0)
		{
			setActionModeTitle(mAdapter.getSelectedCount());
		}

		/*
		 * if chat bg ftue tip or last seen tip is visible we should hide them in action mode
		 */
		if (tipView != null && tipView.getVisibility() == View.VISIBLE)
		{
			Object tag = tipView.getTag();
			
			if (tag instanceof TipType && ((TipType)tag == TipType.LAST_SEEN))
			{
				tipView.setVisibility(View.INVISIBLE);
			}
		}
		if (isHikeOfflineTipShowing())
		{
			setEnableHikeOfflineNextButton(false);
		}
		return true;
	}

	private void destroyActionMode()
	{
		shareableMessagesCount = 0;
		selectedNonTextMsgs = 0;
		selectedNonForwadableMsgs = 0;
		selectedCancelableMsgs = 0;
		setActionModeOn(false);
		mAdapter.removeSelection();
		mOptionsList.clear();
		setupActionBar(false);

		/*
		 * if we have hidden tips while initializing action mode we should unhide them
		 */
		if (tipView != null && tipView.getVisibility() == View.INVISIBLE)
		{
			tipView.setVisibility(View.VISIBLE);
		}
		if (isHikeOfflineTipShowing())
		{
			setEnableHikeOfflineNextButton(true);
		}
		invalidateOptionsMenu();
	}

	public boolean onActionModeItemClicked(MenuItem item)
	{
		final HashMap<Long, ConvMessage> selectedMessagesMap = mAdapter.getSelectedMessagesMap();
		ArrayList<Long> selectedMsgIds;
		switch (item.getItemId())
		{
		case R.id.delete_msgs:
			final ArrayList<Long> selectedMsgIdsToDelete = new ArrayList<Long>(mAdapter.getSelectedMessageIds());
			final CustomAlertDialog deleteConfirmDialog = new CustomAlertDialog(ChatThread.this);
			if (mAdapter.getSelectedCount() == 1)
			{
				deleteConfirmDialog.setHeader(R.string.confirm_delete_msg_header);
				deleteConfirmDialog.setBody(R.string.confirm_delete_msg);
			}
			else
			{
				deleteConfirmDialog.setHeader(R.string.confirm_delete_msgs_header);
				deleteConfirmDialog.setBody(getString(R.string.confirm_delete_msgs, mAdapter.getSelectedCount()));
			}
			View.OnClickListener dialogOkClickListener = new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					deleteMessagesFromDb(selectedMsgIdsToDelete, deleteConfirmDialog.isChecked());
					destroyActionMode();
					deleteConfirmDialog.dismiss();
				}
			};

			if(mAdapter.containsMediaMessage(selectedMsgIdsToDelete))
			{
				deleteConfirmDialog.setCheckBox(R.string.delete_media_from_sdcard, true);
			}
			deleteConfirmDialog.setOkButton(R.string.delete, dialogOkClickListener);
			deleteConfirmDialog.setCancelButton(R.string.cancel);
			deleteConfirmDialog.show();
			return true;
		case R.id.forward_msgs:
			selectedMsgIds = new ArrayList<Long>(mAdapter.getSelectedMessageIds());
			Collections.sort(selectedMsgIds);
			
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FORWARD_MSG);
                metadata.put(HikeConstants.MSISDN, mContactNumber);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			Intent intent = new Intent(ChatThread.this, ComposeChatActivity.class);
			String msg;
			intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
			JSONArray multipleMsgArray = new JSONArray();
			try
			{
				for (int i = 0; i < selectedMsgIds.size(); i++)
				{
					ConvMessage message = selectedMessagesMap.get(selectedMsgIds.get(i));
					JSONObject multiMsgFwdObject = new JSONObject();
					multiMsgFwdObject.put(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE, message.getMessageType());
					if (message.isFileTransferMessage())
					{
						HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
						Utils.handleFileForwardObject(multiMsgFwdObject, hikeFile);
					}
					else if (message.isStickerMessage())
					{
						Sticker sticker = message.getMetadata().getSticker();
						/*
						 * If the category is an unknown one, we have the id saved in the json.
						 */
						String categoryId = sticker.getCategoryId();
						multiMsgFwdObject.putOpt(StickerManager.FWD_CATEGORY_ID, categoryId);
						multiMsgFwdObject.putOpt(StickerManager.FWD_STICKER_ID, sticker.getStickerId());
					}else if(message.getMetadata()!=null && message.getMetadata().isPokeMessage()){
						multiMsgFwdObject.put(HikeConstants.Extras.POKE, true);
					}else if(message.getMessageType()==MESSAGE_TYPE.CONTENT){
						multiMsgFwdObject.put(MESSAGE_TYPE.MESSAGE_TYPE, MESSAGE_TYPE.CONTENT);
						if(message.platformMessageMetadata!=null){
						multiMsgFwdObject.put(HikeConstants.METADATA, message.platformMessageMetadata.JSONtoString());
						if(message.contentLove!=null){
							multiMsgFwdObject.put(HikeConstants.ConvMessagePacketKeys.LOVE_ID, message.contentLove.loveId);
						}
						}
					}  else if(message.getMessageType()==MESSAGE_TYPE.WEB_CONTENT || message.getMessageType() == MESSAGE_TYPE.FORWARD_WEB_CONTENT){
						multiMsgFwdObject.put(MESSAGE_TYPE.MESSAGE_TYPE, MESSAGE_TYPE.FORWARD_WEB_CONTENT);
						multiMsgFwdObject.put(HikeConstants.HIKE_MESSAGE, message.getMessage());
						if(message.webMetadata !=null){
							multiMsgFwdObject.put(HikeConstants.METADATA, PlatformContent.getForwardCardData(message.webMetadata.JSONtoString()));

						}
					}
					else
					{
						msg = message.getMessage();
						multiMsgFwdObject.putOpt(HikeConstants.Extras.MSG, msg);
					}
					multipleMsgArray.put(multiMsgFwdObject);
				}
			}
			catch (JSONException e)
			{
				Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
			}
			
			intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());
			intent.putExtra(HikeConstants.Extras.PREV_MSISDN, mContactNumber);
			intent.putExtra(HikeConstants.Extras.PREV_NAME, mContactName);
			startActivity(intent);
			return true;
		case R.id.copy_msgs:
			selectedMsgIds = new ArrayList<Long>(mAdapter.getSelectedMessageIds());
			Collections.sort(selectedMsgIds);
			StringBuilder msgStr = new StringBuilder();
			int size = selectedMsgIds.size();
			
			for (int i = 0; i < size; i++)
			{
				msgStr.append(selectedMessagesMap.get(selectedMsgIds.get(i)).getMessage());
				msgStr.append("\n");				
			}
			Utils.setClipboardText(msgStr.toString(), getApplicationContext());
			Toast.makeText(ChatThread.this, R.string.copied, Toast.LENGTH_SHORT).show();
			destroyActionMode();
			return true;
		case R.id.action_mode_overflow_menu:
			for (ConvMessage convMessage : selectedMessagesMap.values())
			{
				showActionModeOverflow(convMessage);
			}
			return true;
		case R.id.share_msgs:
			selectedMsgIds = new ArrayList<Long>(mAdapter.getSelectedMessageIds());
			if (selectedMsgIds.size() == 1)
			{
				ConvMessage message = selectedMessagesMap.get(selectedMsgIds.get(0));
				HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
				hikeFile.shareFile(ChatThread.this);
				destroyActionMode();
			}
			else
			{
				Toast.makeText(ChatThread.this, R.string.some_error, Toast.LENGTH_SHORT).show();
			}
			return true;
		default:
			destroyActionMode();
			return false;
		}
	}

	private void showActionModeOverflow(final ConvMessage message)
	{
		ArrayList<OverFlowMenuItem> optionsList = new ArrayList<OverFlowMenuItem>();

		optionsList.add(new OverFlowMenuItem(getString(message.isSent() ? R.string.cancel_upload : R.string.cancel_download), 0));

		OnItemClickListener onItemClickListener = new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				dismissPopupWindow();
				OverFlowMenuItem item = (OverFlowMenuItem) adapterView.getItemAtPosition(position);

				switch (item.getKey())
				{
				case 0:
					HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
					File file = hikeFile.getFile();
					FileTransferManager.getInstance(getApplicationContext()).cancelTask(message.getMsgID(), file, message.isSent(), hikeFile.getFileSize());
					mAdapter.notifyDataSetChanged();
					destroyActionMode();
					break;
				}
			}
		};

		setupPopupWindow(optionsList, onItemClickListener);
	}

	private void setupPopupWindow(ArrayList<OverFlowMenuItem> optionsList, OnItemClickListener onItemClickListener)
	{

		dismissPopupWindow();

		View parentView = getLayoutInflater().inflate(R.layout.overflow_menu, chatLayout, false);

		ListView overFlowListView = (ListView) parentView.findViewById(R.id.overflow_menu_list);
		overFlowListView.setAdapter(new ArrayAdapter<OverFlowMenuItem>(this, R.layout.over_flow_menu_item, R.id.item_title, optionsList)
		{

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

				TextView freeSmsCount = (TextView) convertView.findViewById(R.id.free_sms_count);
				freeSmsCount.setVisibility(View.GONE);

				TextView pin_unread = (TextView) convertView.findViewById(R.id.new_games_indicator);

				if (item.getKey() == 0)
				{
					if(mConversation instanceof GroupConversation && ((GroupConversation)mConversation).getIsGroupAlive())
					{
						pin_unread.setVisibility(View.VISIBLE);
						int pin_unread_count = 0;
						try
						{
							// -1 because most recent pin will be at stick at top
							pin_unread_count = mConversation.getMetaData().getUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
						if (pin_unread_count > 0)
						{
							if (pin_unread_count >= HikeConstants.MAX_PIN_CONTENT_LINES_IN_HISTORY)
							{
								pin_unread.setText(R.string.max_pin_unread_counter);
							}
							else
							{
								pin_unread.setText(Integer.toString(pin_unread_count));
							}
						}
						else
							pin_unread.setVisibility(View.GONE);
					}
				}
				else
				{
					pin_unread.setVisibility(View.GONE);
				}

				return convertView;
			}
		});

		overFlowListView.setOnItemClickListener(onItemClickListener);

		attachmentWindow = new PopupWindow(this);

		attachmentWindow.setContentView(parentView);

		attachmentWindow.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss()
			{
				attachmentWindow = null;
			}
		});

		attachmentWindow.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
		attachmentWindow.setOutsideTouchable(true);
		attachmentWindow.setFocusable(true);
		attachmentWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.overflow_menu_width));
		attachmentWindow.setHeight(LayoutParams.WRAP_CONTENT);
		/*
		 * In some devices Activity crashes and a BadTokenException is thrown by showAsDropDown method. Still need to find out exact repro of the bug.
		 */
		try
		{
			int rightMargin = getResources().getDimensionPixelSize(R.dimen.overflow_menu_width) + getResources().getDimensionPixelSize(R.dimen.overflow_menu_right_margin);
			attachmentWindow.showAsDropDown(findViewById(R.id.attachment_anchor), -rightMargin, -(int) (0.5 * Utils.scaledDensityMultiplier));

		}
		catch (BadTokenException e)
		{
			Logger.e(getClass().getSimpleName(), "Excepetion in Action Mode Overflow popup", e);
		}
		attachmentWindow.getContentView().setFocusableInTouchMode(true);
		attachmentWindow.getContentView().setOnKeyListener(new View.OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				return onKeyUp(keyCode, event);
			}
		});
	}

	@Override
	protected void onPostResume()
	{
		// TODO Auto-generated method stub
		super.onPostResume();
		mConversationsView.postDelayed(new Runnable()
		{

			@Override
			public void run()
			{
				// TODO Auto-generated method stub
				Logger.i("chatadapter", mAdapter.getCount() + " --total items");
			}
		}, 2000);
	}

	private static int possibleKeyboardHeight;

	private static int possibleKeyboardHeightLand;

	private ViewTreeObserver.OnGlobalLayoutListener getGlobalLayoutListener()
	{
		return new ViewTreeObserver.OnGlobalLayoutListener()
		{

			@Override
			public void onGlobalLayout()
			{
				Log.i("chatthread", "global layout listener");
				View root = findViewById(R.id.chatThreadParentLayout);
				Log.i("chatthread", "global layout listener rootHeight " + root.getRootView().getHeight() + " new height " + root.getHeight());
				Rect r = new Rect();
				root.getWindowVisibleDisplayFrame(r);
				// this is height of view which is visible on screen
				int rootViewHeight = root.getRootView().getHeight();
				int temp = rootViewHeight - r.bottom;
				Logger.i("chatthread", "keyboard  height " + temp);
				boolean islandScape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
				if (temp > 0)
				{
					if (islandScape)
					{
						possibleKeyboardHeightLand = temp;
					}
					else
					{
						possibleKeyboardHeight = temp;
					}
					isKeyboardOpen = true;
					if (isEmoticonPalleteVisible())
					{
						resizeMainheight(0, false);
						attachmentWindow.update(-1, temp);
					}
				}
				else
				{
					// when we change orientation , from portrait to landscape and keyboard is open , it is possible that screen does adjust its size more than once until it
					// stabilize
					if (islandScape)
						possibleKeyboardHeightLand = 0;
					isKeyboardOpen = false;
				}
			}
		};
	}

	@Override
	protected void onRestart()
	{
		super.onRestart();
		/*
		 * This method will be called either user is returning after pressing home or screen lock ,
		 * 
		 * IF USER came after pressing home, then soft keyboard respects softinputstate , i.e : if keyboard was visible or but softinputmode visible is set , then soft keyboard
		 * will become visible
		 * 
		 * But if it is called after screen lock , then soft input keyboard maintains its state , it doesnot change, if it was visible earlier, it will be visible this time as well
		 * , so we simply return as it does not effect our sticker pallete -- gauravKhanna
		 */

		if (screenOffEvent)
		{
			screenOffEvent = false;
			return;
		}
		Logger.i("chatthread", "on restart");

		int softInput = getWindow().getAttributes().softInputMode;
		if (softInput == WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE || softInput == WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
		{
			// keyboard will come for sure
			if (isEmoticonPalleteVisible())
			{
				resizeMainheight(0, false);
			}
			return;
		}
		// mean last time it was above keyboard, so no guarantee of keyboard, simply discard it
		if (isEmoticonPalleteVisible() && findViewById(R.id.chat_layout).getPaddingBottom() == 0)
		{
			dismissPopupWindow();

		}

	}

	private boolean isEmoticonPalleteVisible()
	{
		return attachmentWindow != null && emoticonLayout != null && attachmentWindow.getContentView() == emoticonLayout;
	}

	private boolean resizeMainheight(int emoticonPalHeight, boolean respectKeyboardVisiblity)
	{
		Logger.i("chatthread", "trying to resize main height with bottom padding");
		if (respectKeyboardVisiblity && isKeyboardOpen)
		{
			return false;
		}
		View root = findViewById(R.id.chat_layout);
		if (root.getPaddingBottom() != emoticonPalHeight)
		{
			Logger.i("chatthread", "resize main height with bottom padding " + emoticonPalHeight);
			root.setPadding(0, 0, 0, emoticonPalHeight);
			showPaletteFillerView(emoticonPalHeight);
			return true;
		}
		return false;
	}

	public int getStatusBarHeight()
	{
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0)
		{
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	@Override
	public void onBackKeyPressedET(CustomFontEditText editText)
	{
		// on back press - if keyboard was open , now keyboard gone , try to hide emoticons
		// if keyboard ws not open , onbackpress of activity will get call back, dismiss popup there
		// if we dismiss here in second case as well, then onbackpress of acitivty will be called and it will finish activity
		if (isKeyboardOpen && attachmentWindow != null && attachmentWindow.getContentView() == emoticonLayout)
		{
			dismissPopupWindow();
		}

	}

	private void updateEmoticonPallateHeight()
	{
		ViewGroup.LayoutParams lp = emoticonLayout.getLayoutParams();
		if (lp == null)
		{
			lp = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0);
		}
		boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		if (isLandscape)
		{
			Logger.i("chatthread", "landscape mode is on");
			if (possibleKeyboardHeightLand != 0)
			{
				Logger.i("chatthread", "landscape mode is on landkeyboardheight " + possibleKeyboardHeightLand);
				lp.height = possibleKeyboardHeightLand;
			}
			else
			{
				View root = findViewById(R.id.chat_layout);

				int statusBarHeight = getStatusBarHeight();
				int maxHeight = root.getRootView().getHeight();
				// giving half height of screen in landscape mode
				Logger.i("chatthread", "landscape mode is on setting half of screen " + maxHeight);
				lp.height = (maxHeight) / 2;
			}
		}
		else
		{
			if (possibleKeyboardHeight != 0)
			{
				lp.height = possibleKeyboardHeight;
			}
			else
			{
				lp.height = (int) (getResources().getDimension(R.dimen.emoticon_pallete));
			}
		}

		Log.i("keyboard", "height " + lp.height);
		emoticonLayout.setLayoutParams(lp);
		attachmentWindow.setHeight(lp.height);
		resizeMainheight(lp.height, true);
	}

	/**
	 * This method shows a empty black filler view that has the same height as the emoticon/sticker palette to improve the palette popup transition.
	 * 
	 * @param height
	 */
	private void showPaletteFillerView(int height)
	{
		View fillerView = findViewById(R.id.sticker_palette_filler);

		android.widget.RelativeLayout.LayoutParams lp = (android.widget.RelativeLayout.LayoutParams) fillerView.getLayoutParams();
		lp.height = height;

		fillerView.setVisibility(View.VISIBLE);
	}

	private class ScreenOffReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Logger.d("chatthread", "on receive called screenoff");
			screenOffEvent = true;
		}
	}

	LastSeenFetchedCallback lastSeenFetchedCallback = new LastSeenFetchedCallback()
	{

		@Override
		public void lastSeenFetched(String msisdn, int offline, long lastSeenTime)
		{
			HAManager.getInstance().recordLastSeenEvent(ChatThread.class.getName(), "lastSeenFetched", "going to update UI", mContactNumber);
			updateLastSeen(msisdn, offline, lastSeenTime);
		}
	};

	private void updateLastSeen(String msisdn, int offline, long lastSeenTime)
	{
		if (!mContactNumber.equals(msisdn) || (mConversation instanceof GroupConversation) || !shouldShowLastSeen())
		{
			return;
		}
		
		/*
		 * Fix for case where server and client values are out of sync
		 */
		if(offline == 1 && lastSeenTime <= 0)
		{
			return;
		}

		/*
		 * Updating the class's contactinfo object
		 */
		contactInfo.setOffline(offline);
		contactInfo.setLastSeenTime(lastSeenTime);

		final String lastSeenString = Utils.getLastSeenTimeAsString(this, lastSeenTime, offline, false, true);

		isOnline = contactInfo.getOffline() == 0;

		if (isHikeOfflineTipShowing() && isOnline)
		{
			// if hike offline tip is showing and server sends that user has
			// come online, we donot update last seen field if all pending
			// messages gets delivered than we would update this field
			return;
		}

		runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				if (isOnline)
				{
					shouldRunTimerForHikeOfflineTip = true;
				}

				if (lastSeenString == null)
				{
					HAManager.getInstance().recordLastSeenEvent(ChatThread.class.getName(), "updateLastSeen", 
							"lastSeen null so setLastSeenTextBasedOnHikeValue", mContactNumber);
					setLastSeenTextBasedOnHikeValue(mConversation.isOnhike());
				}
				else
				{
					setLastSeenText(lastSeenString);
				}

			}
		});
	}

	private void updateLastSeen()
	{
		updateLastSeen(contactInfo.getMsisdn(), contactInfo.getOffline(), contactInfo.getLastSeenTime());
	}

	public void showHikeToOfflineTip()
	{
		if (!mConversation.isOnhike() || mConversation instanceof GroupConversation || isHikeOfflineTipShowing())
		{
			return;
		}

		if (!HikeMqttPersistence.getInstance().isMessageSent(mAdapter.getFirstUnsentMessageId()))
		{
			return;
		}

		if (isOnline && mLastSeenView != null)
		{
			mLastSeenView.setVisibility(View.GONE);
		}

		if (hikeToOfflineTipview == null)
		{
			hikeToOfflineTipview = LayoutInflater.from(this).inflate(R.layout.hike_to_offline_tip, null);
		}

		hikeToOfflineTipview.clearAnimation();
		setupHikeToOfflineTipViews();

		LinearLayout tipContainer = (LinearLayout) findViewById(R.id.tipContainerBottom);
		if (tipContainer.getChildCount() > 0)
		{
			tipContainer.removeAllViews();
		}
		if (tipView != null && tipView.getVisibility() == View.VISIBLE)
		{
			tipView.setVisibility(View.GONE);
		}

		tipContainer.addView(hikeToOfflineTipview);
		hikeToOfflineTipview.setVisibility(View.VISIBLE);

		scrollListViewOnShowingOfflineTip();
		shouldRunTimerForHikeOfflineTip = false;
	}

	private void scrollListViewOnShowingOfflineTip()
	{
		if (mConversationsView.getLastVisiblePosition() > messages.size() - 3)
		{
			mConversationsView.post(new Runnable()
			{

				@Override
				public void run()
				{
					mConversationsView.smoothScrollToPosition(messages.size() - 1);
				}
			});
		}
	}

	public void setupHikeToOfflineTipViews()
	{
		setupHikeToOfflineTipViews(false);
	}

	public void setupHikeToOfflineTipViews(boolean messagesSent)
	{
		setupHikeToOfflineTipViews(messagesSent, false);
	}

	public void setupHikeToOfflineTipViews(boolean messagesSent, boolean isNativeSms)
	{
		if (isHikeToOfflineMode)
		{
			((TextView) hikeToOfflineTipview.findViewById(R.id.tip_header)).setText(getResources().getString(R.string.selected_count, mAdapter.getSelectedCount()));
			((TextView) hikeToOfflineTipview.findViewById(R.id.tip_msg)).setText(getResources().getString(R.string.hike_offline_mode_msg, mAdapter.getSelectedFreeSmsCount()));
			((TextView) hikeToOfflineTipview.findViewById(R.id.send_button_text)).setText(R.string.send_uppercase);
			hikeToOfflineTipview.findViewById(R.id.send_button).setVisibility(View.VISIBLE);

			hikeToOfflineTipview.findViewById(R.id.send_button).setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mAdapter.hikeOfflineSendClick();
					
					try
					{
						JSONObject metadata = new JSONObject();
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SECOND_OFFLINE_TIP_CLICKED);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					}
					catch(JSONException e)
					{
						Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
					}
				}
			});

			hikeToOfflineTipview.findViewById(R.id.close_tip).setVisibility(View.GONE);

			hikeToOfflineTipview.setTag(HIKE_TO_OFFLINE_TIP_STATE_2);
		}
		else
		{
			/*
			 * Only when user has selected native sms as Always we show "send paid sms" in all other cases we show heading as "send free sms"
			 */
			if (PreferenceManager.getDefaultSharedPreferences(ChatThread.this).getBoolean(HikeConstants.SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF, false)
					&& PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_PREF, false))
			{
				((TextView) hikeToOfflineTipview.findViewById(R.id.tip_header)).setText(R.string.send_paid_sms);
			}
			else
			{
				((TextView) hikeToOfflineTipview.findViewById(R.id.tip_header)).setText(R.string.send_free_sms);
			}
			((TextView) hikeToOfflineTipview.findViewById(R.id.tip_msg)).setText(getResources().getString(R.string.reciever_is_offline, mLabel));
			((TextView) hikeToOfflineTipview.findViewById(R.id.send_button_text)).setText(R.string.next_uppercase);
			hikeToOfflineTipview.findViewById(R.id.send_button).setVisibility(View.VISIBLE);
			OnClickListener onNextClickListener = new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (isActionModeOn)
					{
						return;
					}
					initialiseHikeToOfflineMode();
					setupHikeToOfflineTipViews();
					
					try
					{
						JSONObject metadata = new JSONObject();
						metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FIRST_OFFLINE_TIP_CLICKED);
						HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
					}
					catch(JSONException e)
					{
						
					}
				}
			};

			hikeToOfflineTipview.findViewById(R.id.send_button).setOnClickListener(onNextClickListener);
			hikeToOfflineTipview.findViewById(R.id.close_tip).setVisibility(View.GONE);

			hikeToOfflineTipview.setTag(HIKE_TO_OFFLINE_TIP_STATE_1);
		}
	}

	private void initialiseHikeToOfflineMode()
	{
		scrollListViewOnShowingOfflineTip();
		hikeToOfflineTipview.findViewById(R.id.send_button).setVisibility(View.VISIBLE);
		hikeToOfflineTipview.findViewById(R.id.close_tip).setVisibility(View.GONE);

		LinkedHashMap<Long, ConvMessage> unsentMessages = mAdapter.getAllUnsentMessages(false);
		for (Long msgid : unsentMessages.keySet())
		{
			ConvMessage convMsg = unsentMessages.get(msgid);
			if (convMsg.getState() == State.SENT_CONFIRMED)
			{
				mAdapter.selectView(convMsg, true);
			}
		}
		sethikeToOfflineMode(true);
	}

	public void hideHikeToOfflineTip(final boolean messagesSent, final boolean isNativeSms, boolean hideWithoutAnimation, boolean calledFromMsgDelivered)
	{
		if (hikeToOfflineTipview == null)
		{
			return;
		}
		/*
		 * If tip is already hiding we don't need to hide it anymore
		 */
		else if (((Integer) hikeToOfflineTipview.getTag()) == HIKE_TO_OFFLINE_TIP_STATE_3)
		{
			return;
		}

		AnimationListener animationListener = new AnimationListener()
		{
			@Override
			public void onAnimationStart(Animation animation)
			{
				hikeToOfflineTipview.setTag(HIKE_TO_OFFLINE_TIP_STATE_3);
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				if (hikeToOfflineTipview == null)
				{
					return;
				}
				hikeToOfflineTipview.setVisibility(View.GONE);
				if (isHikeToOfflineMode)
				{
					destroyHikeToOfflineMode();
				}
				((LinearLayout) findViewById(R.id.tipContainerBottom)).removeView(hikeToOfflineTipview);

				/*
				 * When messages are successfully sent we need to show a toast
				 */
				if (messagesSent)
				{
					Toast toast;
					if (!isNativeSms)
					{
						toast = Toast.makeText(ChatThread.this, getString(R.string.hike_offline_messages_sent_msg, currentCreditsForToast - mAdapter.getSelectedFreeSmsCount()),
								Toast.LENGTH_SHORT);
					}
					else
					{
						toast = Toast.makeText(ChatThread.this, getString(R.string.regular_sms_sent_confirmation), Toast.LENGTH_SHORT);
					}
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
			}
		};

		if (hikeToOfflineTipview.getAnimation() == null)
		{
			setHikeOfflineTipHideAnimation(hikeToOfflineTipview, animationListener, hideWithoutAnimation);
			
			if(calledFromMsgDelivered)
			{
				/*
				 * we need to update last seen value coz we might
				 * have updated contact's last seen value in between
				 * when hike offline tip was showing
				 */
				updateLastSeen();
			}
		}
	}

	public void hideHikeToOfflineTip()
	{
		hideHikeToOfflineTip(false, false, false, false);
	}

	public void hideHikeToOfflineTip(final boolean messagesSent, final boolean isNativeSms)
	{
		hideHikeToOfflineTip(messagesSent, isNativeSms, false, false);
	}

	public void sethikeToOfflineMode(boolean isOn)
	{
		isHikeToOfflineMode = isOn;
		mAdapter.sethikeToOfflineMode(isOn);
		mAdapter.notifyDataSetChanged();
	}

	private void destroyHikeToOfflineMode()
	{
		sethikeToOfflineMode(false);
		setupHikeToOfflineTipViews();
		mAdapter.removeSelection();
	}

	/*
	 * Method called when user selects one of the chat bubbles while inside hike to offline mode.
	 */
	public boolean clickedHikeToOfflineMessage(ConvMessage message)
	{
		dismissPopupWindow();
		if (message == null || message.getParticipantInfoState() != ParticipantInfoState.NO_INFO || message.getTypingNotification() != null || message.isBlockAddHeader())
		{
			return false;
		}

		if (message.getState() != State.SENT_CONFIRMED || message.isSMS())
		{
			return false;
		}
		mAdapter.toggleSelection(message);
		boolean isMsgSelected = mAdapter.isSelected(message);

		boolean hasCheckedItems = mAdapter.getSelectedCount() > 0;

		if (hasCheckedItems && !isHikeToOfflineMode)
		{
			// there are some selected items, start the actionMode
			setupActionModeActionBar();
		}
		else if (!hasCheckedItems && isHikeToOfflineMode)
		{
			// there no selected items, finish the actionMode
			destroyHikeToOfflineMode();
			return true;
		}

		if (isHikeToOfflineMode)
		{
			setupHikeToOfflineTipViews();
		}

		return true;
	}

	public void messagesSentCloseHikeToOfflineMode(boolean isNativeSms)
	{
		currentCreditsForToast = mCredits;
		destroyHikeToOfflineMode();
		hideHikeToOfflineTip(true, isNativeSms);
	}

	private void setHikeOfflineTipHideAnimation(View v, AnimationListener animationListener, boolean hideWithoutAnimation)
	{
		slideDown = AnimationUtils.loadAnimation(ChatThread.this, R.anim.slide_down_noalpha);
		slideDown.setDuration(hideWithoutAnimation ? 0 : 400);

		slideDown.setAnimationListener(animationListener);

		v.startAnimation(slideDown);

	}

	public boolean isHikeOfflineTipShowing()
	{
		if (hikeToOfflineTipview != null)
		{
			/*
			 * if hike offline tip is in last state this means it is going to hide;
			 */
			if (((Integer) hikeToOfflineTipview.getTag()) == HIKE_TO_OFFLINE_TIP_STATE_3)
			{
				return false;
			}
			return hikeToOfflineTipview.getVisibility() == View.VISIBLE;
		}
		return false;
	}

	public int getCurrentSmsBalance()
	{
		return mCredits;
	}

	private void setEnableHikeOfflineNextButton(boolean enabled)
	{
		hikeToOfflineTipview.findViewById(R.id.send_button).setEnabled(enabled);
		hikeToOfflineTipview.findViewById(R.id.send_button_text).setEnabled(enabled);
		hikeToOfflineTipview.findViewById(R.id.send_button_tick).setEnabled(enabled);
	}

	@Override
	public void onEmoticonClicked(int emoticonIndex)
	{
		Utils.emoticonClicked(getApplicationContext(), emoticonIndex, mComposeView);

	}

	private void showPinHistory(boolean viaMenu)
	{
		Intent intent = new Intent();
		intent.setClass(ChatThread.this, PinHistoryActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(HikeConstants.TEXT_PINS, mContactNumber);
		startActivity(intent);
		Utils.resetPinUnreadCount(mConversation);
		
		JSONObject metadata = new JSONObject();
		
		try
		{
			if(viaMenu)
			{
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.PIN_HISTORY_VIA_MENU);
			}else
			{
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.PIN_HISTORY_VIA_PIN_CLICK);
			}
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}		
	}
	
	private void updateOverflowMenuUnreadCount()
	{
		int count = -1;
		
		try 
		{
			if(mConversation != null && mConversation.getMetaData() != null)
			{
				count = mConversation.getMetaData().getUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
			}
		}
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
				
		if (topUnreadPinsIndicator != null)
		{
			final int uCount = count;
			
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					updateOverflowUnreadCount(uCount, 1000);
				}
			});
		}
	}
	
	private void updateOverflowUnreadCount(final int count, int delayTime)
	{
		if (count < 1)
		{
			topUnreadPinsIndicator.setVisibility(View.GONE);
		}
		else
		{
			mHandler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					int newCount = -1;
					
					try 
					{
						newCount = mConversation.getMetaData().getUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
					}
					catch (JSONException e) 
					{
						e.printStackTrace();
					}
					
					if (topUnreadPinsIndicator != null)
					{						
						if (newCount < 1)
						{
							topUnreadPinsIndicator.setVisibility(View.GONE);
						}
						else if (newCount > 9)
						{
							topUnreadPinsIndicator.setVisibility(View.VISIBLE);
							topUnreadPinsIndicator.setText("9+");
							topUnreadPinsIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
						}
						else if (newCount > 0)
						{
							topUnreadPinsIndicator.setVisibility(View.VISIBLE);
							topUnreadPinsIndicator.setText(String.valueOf(count));
							topUnreadPinsIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
						}
					}
				}
			}, delayTime);
		}
	}
	
	private boolean isShowingPin(){
		return tipView!=null && tipView.getTag() instanceof Integer && ((Integer)tipView.getTag() == HikeConstants.MESSAGE_TYPE.TEXT_PIN);
	}
	
	public boolean removeFragment(String tag, boolean updateActionBar)
	{
		boolean isRemoved = super.removeFragment(tag);
		if (isRemoved)
		{
			getSupportActionBar().show();
			if(updateActionBar)
			{
				setupActionBar(false);
			}
		}
		return isRemoved;
	}

	public void hideKeyBoardIfVisible()
	{
		if(isKeyboardOpen)
		{
			Utils.hideSoftKeyboard(ChatThread.this, mComposeView);
		}
	}
}
