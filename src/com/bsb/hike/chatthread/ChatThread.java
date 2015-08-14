package com.bsb.hike.chatthread;

import static com.bsb.hike.HikeConstants.IntentAction.ACTION_KEYBOARD_CLOSED;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.MESSAGE_TYPE;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.AnalyticsConstants.MsgRelEventType;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.MsgRelLogManager;
import com.bsb.hike.chatthread.HikeActionMode.ActionModeListener;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.CustomAlertDialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.media.AttachmentPicker;
import com.bsb.hike.media.AudioRecordView;
import com.bsb.hike.media.AudioRecordView.AudioRecordListener;
import com.bsb.hike.media.EmoticonPicker;
import com.bsb.hike.media.HikeActionBar;
import com.bsb.hike.media.ImageParser;
import com.bsb.hike.media.ImageParser.ImageParserListener;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.media.OverFlowMenuLayout.OverflowViewListener;
import com.bsb.hike.media.OverflowItemClickListener;
import com.bsb.hike.media.PickContactParser;
import com.bsb.hike.media.PickFileParser;
import com.bsb.hike.media.PickFileParser.PickFileListener;
import com.bsb.hike.media.PopupListener;
import com.bsb.hike.media.ShareablePopupLayout;
import com.bsb.hike.media.StickerPicker;
import com.bsb.hike.media.StickerPickerListener;
import com.bsb.hike.media.ThemePicker;
import com.bsb.hike.media.ThemePicker.ThemePickerListener;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MovingList;
import com.bsb.hike.models.MovingList.OnItemsFinishedListener;
import com.bsb.hike.models.PhonebookContact;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.models.Unique;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.modules.stickersearch.StickerSearchManager;
import com.bsb.hike.modules.stickersearch.listeners.IStickerPickerRecommendationListener;
import com.bsb.hike.modules.stickersearch.provider.StickerSearchHostManager;
import com.bsb.hike.modules.stickersearch.ui.StickerTagWatcher;
import com.bsb.hike.platform.CardComponent;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.platform.PlatformMessageMetadata;
import com.bsb.hike.platform.WebMetadata;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.tasks.EmailConversationsAsyncTask;
import com.bsb.hike.ui.ComposeViewWatcher;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.utils.LockPattern;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.SearchManager;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.SoundUtils;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.CustomFontEditText;
import com.bsb.hike.view.CustomFontEditText.BackKeyListener;
import com.bsb.hike.view.CustomLinearLayout;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;

/**
 * 
 * @generated
 */

public abstract class ChatThread extends SimpleOnGestureListener implements OverflowItemClickListener, View.OnClickListener, ThemePickerListener, ImageParserListener,
		PickFileListener, StickerPickerListener, AudioRecordListener, LoaderCallbacks<Object>, OnItemLongClickListener, OnTouchListener, OnScrollListener,
		Listener, ActionModeListener, HikeDialogListener, TextWatcher, OnDismissListener, OnEditorActionListener, OnKeyListener, PopupListener, BackKeyListener,
		OverflowViewListener, OnSoftKeyboardListener, IStickerPickerRecommendationListener
{
	private static final String TAG = ChatThread.class.getSimpleName();

	protected static final int FETCH_CONV = 1;

	protected static final int LOAD_MORE_MESSAGES = 2;

	protected static final int SHOW_TOAST = 3;

	protected static final int MESSAGE_RECEIVED = 4;

	protected static final int END_TYPING_CONVERSATION = 5;

	protected static final int TYPING_CONVERSATION = 6;

	protected static final int NOTIFY_DATASET_CHANGED = 7;

	protected static final int UPDATE_AVATAR = 8;

	protected static final int FILE_MESSAGE_CREATED = 9;

	protected static final int DELETE_MESSAGE = 10;

	protected static final int CHAT_THEME = 11;

	protected static final int CLOSE_CURRENT_STEALTH_CHAT = 12;

	/**
	 * Skipping the number '13' intentionally. #triskaidekaphobia
	 */
	protected static final int CLOSE_PHOTO_VIEWER_FRAGMENT = 14;

	protected static final int UPDATE_NETWORK_STATE = 16;

	protected static final int HIDE_DOWN_FAST_SCROLL_INDICATOR = 17;

	protected static final int HIDE_UP_FAST_SCROLL_INDICATOR = 18;

	protected static final int SET_LABEL = 19;
	
	protected static final int DISABLE_TRANSCRIPT_MODE = 20;

	protected static final int STICKER_CATEGORY_MAP_UPDATED = 21;

	protected static final int MULTI_SELECT_ACTION_MODE = 22;

	protected static final int SCROLL_TO_END = 23;
	
	protected static final int STICKER_FTUE_TIP = 24;

    protected static final int MULTI_MSG_DB_INSERTED = 25;

	protected static final int SEARCH_ACTION_MODE = 24;

	protected static final int SEARCH_NEXT = 25;

	protected static final int SEARCH_PREVIOUS = 26;
	
	protected static final int SEARCH_LOOP = 27;

    protected static final int SET_WINDOW_BG = 28;
    
    protected static final int SEARCH_RESULT = 29;
 
    protected static final int BLOCK_UNBLOCK_USER = 30;

    protected static final int ACTION_MODE_CONFIG_CHANGE = 32;
    
    private static final int MUTE_CONVERSATION_TOGGLED = 33;
    
    private static final int SHARING_FUNCTIONALITY = 34;
    
	protected static final int UPDATE_STEALTH_BADGE = 35;
	
	protected static final int INITIALIZE_MORE_MESSAGES = 36;
	
	protected static final int UPDATE_MESSAGE_LIST = 37;
    
    private int NUDGE_TOAST_OCCURENCE = 2;
    	
    private int currentNudgeCount = 0;
    
    protected ChatThreadActivity activity;
    
	protected ThemePicker themePicker;

	protected AttachmentPicker attachmentPicker;

	protected HikeSharedPreferenceUtil sharedPreference;

	protected ChatTheme currentTheme;

	protected String msisdn;

	protected StickerPicker mStickerPicker;

	protected EmoticonPicker mEmoticonPicker;

	protected ShareablePopupLayout mShareablePopupLayout;

	protected AudioRecordView audioRecordView;

	protected Conversation mConversation;

	protected HikeConversationsDatabase mConversationDb;

	protected MessagesAdapter mAdapter;

	protected MovingList<ConvMessage> messages;

	protected static HashMap<Long, ConvMessage> mMessageMap;

	protected boolean isActivityVisible = true;

	protected boolean reachedEnd = false;
	
	private volatile boolean _doubleTapPref = false;


	private int currentFirstVisibleItem = Integer.MAX_VALUE;

	protected boolean loadingMoreMessages;

	private String[] mPubSubListeners;

	protected ListView mConversationsView;

	protected ComposeViewWatcher mComposeViewWatcher;

	private int unreadMessageCount = 0;

	protected CustomFontEditText mComposeView;

	private GestureDetector mGestureDetector;

	protected View mActionBarView;

	protected HikeActionMode mActionMode;

	protected int selectedNonTextMsgs;

	protected static SearchManager messageSearchManager;

	private String searchText;

	protected int selectedNonForwadableMsgs;

	protected int shareableMessagesCount;

	protected int selectedCancelableMsgs;

	protected ChatThreadTips mTips;
	
	private ProgressDialog searchDialog;

	private static final String NEW_LINE_DELIMETER = "\n";
	
	private boolean ctSearchIndicatorShown, consumedForwardedData;
	
	protected HikeDialog dialog;
	
	private StickerTagWatcher stickerTagWatcher;
	
	protected int mCurrentActionMode;

	private boolean shouldKeyboardPopupShow;
	
	private class ChatThreadBroadcasts extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			switch (intent.getAction())
			{
			case StickerManager.STICKERS_UPDATED:
			case StickerManager.MORE_STICKERS_DOWNLOADED:
			case StickerManager.STICKERS_DOWNLOADED:
				if (mStickerPicker != null)
				{
					mStickerPicker.notifyDataSetChanged();
					mStickerPicker.setRefreshStickers(true);
				}
				break;
			case ACTION_KEYBOARD_CLOSED:
				// In keyboard21 when we click on sticker icon , if keyboard is open at that time it is first closed and then pallte comes.
				// So adding check so that recommendation popup is not closed when shareablepop is showing
				
				if(mShareablePopupLayout != null && !mShareablePopupLayout.isShowing())
				{
					dismissStickerRecommendationPopup();
				}
				break;
			}
			
		}
	}

	private ChatThreadBroadcasts mBroadCastReceiver;


	protected Handler uiHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			/**
			 * Defensive check
			 */
			if (msg == null)
			{
				Logger.e(TAG, "Getting a null message in chat thread");
				return;
			}
			handleUIMessage(msg);
		}

	};

	/**
	 * This method is called from the UI Handler's handleMessage. All the tasks performed by this are supposed to run on the UI Thread only.
	 * 
	 * This is also overriden by {@link OneToOneChatThread} and {@link OneToNChatThread}
	 * 
	 * @param msg
	 */
	protected void handleUIMessage(android.os.Message msg)
	{
		switch (msg.what)
		{
		case UPDATE_AVATAR:
			setAvatar();
			break;
		case UPDATE_STEALTH_BADGE:
			setAvatarStealthBadge();
			break;
		case SET_WINDOW_BG:
			setWindowBackGround();
			break;
		case SHOW_TOAST:
			showToast(msg.arg1);
			break;
		case MESSAGE_RECEIVED:
			addMessage((ConvMessage) msg.obj);
			break;
		case NOTIFY_DATASET_CHANGED:
			Logger.i(TAG, "notifying data set changed on UI Handler");
			Logger.i("gaurav", "notifying data set changed on UI Handler");
			mAdapter.notifyDataSetChanged();
			break;
		case END_TYPING_CONVERSATION:
			setTypingText(false, (TypingNotification) msg.obj);
			break;
		case TYPING_CONVERSATION:
			setTypingText(true, (TypingNotification) msg.obj);
			break;
		case FILE_MESSAGE_CREATED:
        case MULTI_MSG_DB_INSERTED:
			addMessage((ConvMessage) msg.obj);
			break;
		case DELETE_MESSAGE:
			deleteMessages((Pair<Boolean, ArrayList<Long>>) msg.obj);
			break;
		case CHAT_THEME:
			changeChatTheme((ChatTheme) msg.obj);
			break;
		case CLOSE_CURRENT_STEALTH_CHAT:
			closeStealthChat();
			break;
		case CLOSE_PHOTO_VIEWER_FRAGMENT:
			removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG, true);
			break;
		case UPDATE_NETWORK_STATE:
			updateNetworkState();
			break;
		case HIDE_DOWN_FAST_SCROLL_INDICATOR:
			hideView(R.id.scroll_bottom_indicator);
			break;
		case HIDE_UP_FAST_SCROLL_INDICATOR:
			hideView(R.id.scroll_top_indicator);
			break;
		case SET_LABEL:
			setLabel((String) msg.obj);
			break;
		case STICKER_CATEGORY_MAP_UPDATED:
			if (mStickerPicker != null)
			{
				mStickerPicker.notifyDataSetChanged();
				mStickerPicker.setRefreshStickers(true);
			}
			break;
		case SCROLL_TO_END:
			mConversationsView.setSelection(messages.size() - 1);
			break;
		case STICKER_FTUE_TIP:
			mTips.showStickerFtueTip();
			break;
		case DISABLE_TRANSCRIPT_MODE:
			mConversationsView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);		
			break;
		case SHARING_FUNCTIONALITY:
			if (mActionMode!= null && mActionMode.whichActionModeIsOn() == MULTI_SELECT_ACTION_MODE)
			{
				mActionMode.finish();
			}
			 break;
		case BLOCK_UNBLOCK_USER:
			blockUnBlockUser((boolean) msg.obj);
			break;
		case ACTION_MODE_CONFIG_CHANGE:
			handleActionModeOrientationChange(mActionMode.whichActionModeIsOn());
			break;
		case MUTE_CONVERSATION_TOGGLED:
			muteConvToggledUIChange((boolean) msg.obj);
			break;
		case UPDATE_MESSAGE_LIST:
			Pair<MovingList<ConvMessage>, Integer> pair = (Pair<MovingList<ConvMessage>, Integer>)(msg.obj);
			updateMessageList(pair.first,pair.second);
			break;
		case SEARCH_RESULT:
			updateUIforSearchResult((int) msg.obj);
			break;
		default:
			Logger.d(TAG, "Did not find any matching event for msg.what : " + msg.what);
			break;
		}
	}
	

	/**
	 * This method handles the UI part of Mute group conversation It is to be strictly called from the UI Thread
	 * 
	 * @param isMuted
	 */
	private void muteConvToggledUIChange(boolean isMuted)
	{
		if (!ChatThreadUtils.checkNetworkError())
		{
			toggleConversationMuteViewVisibility(isMuted);
		}
	}
	
	protected void toggleConversationMuteViewVisibility(boolean isMuted)
	{
		activity.findViewById(R.id.conversation_mute).setVisibility(isMuted ? View.VISIBLE : View.GONE);
	}

	protected void addMessage(ConvMessage convMessage)
	{

		addtoMessageMap(messages.size() - 1, messages.size());

		mAdapter.notifyDataSetChanged();

		// Reset this boolean to load more messages when the user scrolls to
		// the top
		reachedEnd = false;

		/*
		 * Don't scroll to bottom if the user is at older messages. It's possible that the user might be reading them.
		 */

		tryScrollingToBottom(convMessage, 0);

	}

	public ChatThread(ChatThreadActivity activity, String msisdn)
	{
		this.activity = activity;
		this.msisdn = msisdn;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public MessageSenderLayout messageSenderLayout;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public HikeActionBar mActionBar;

	protected Bundle savedState;
	
	public void onCreate(Bundle savedState)
	{
		Logger.i(TAG, "onCreate(" + savedState + ")");

		this.savedState = savedState;
		init();
		setContentView();
		fetchConversation(false);
		uiHandler.sendEmptyMessage(SET_WINDOW_BG);
		StickerManager.getInstance().checkAndDownLoadStickerData();
	}
	
	/**
	 * Setting window background as black to avoid jarring effect when keyboard or sticker or emoticon pallete is opened.
	 * Source : http://android-developers.blogspot.ca/2009/03/window-backgrounds-ui-speed.html
	 * This is purely for user perception 
	 */
	private void setWindowBackGround()
	{
		android.view.Window window = activity.getWindow();
		if (window != null)
		{
			Logger.d(TAG, "Setting window's background");
			window.setBackgroundDrawableResource(R.color.black);
		}
	}
	
	public void onNewIntent()
	{
		init();
		setContentView();
		fetchConversationOnNewIntent(false);
	}

	protected void init()
	{
		mActionBar = new HikeActionBar(activity);
		mConversationDb = HikeConversationsDatabase.getInstance();
		sharedPreference = HikeSharedPreferenceUtil.getInstance();
		ctSearchIndicatorShown = sharedPreference.getData(HikeMessengerApp.CT_SEARCH_INDICATOR_SHOWN, false);
		shouldKeyboardPopupShow=HikeMessengerApp.keyboardApproach(activity);
	}

	/**
	 * This function must be called after setting content view
	 */
	protected void initView()
	{
		audioRecordView = new AudioRecordView(activity, this);
		mComposeView = (CustomFontEditText) activity.findViewById(R.id.msg_compose);
		mComposeView.setOnClickListener(this);
		initShareablePopup();

		initActionMode();

		addOnClickListeners();

		showNetworkError(ChatThreadUtils.checkNetworkError());
		defineEnterAction();
		
		setupStickerSearch();
	}

	private void defineEnterAction() {
		
		if (mComposeView != null) {
			//if send on enter setting is unchecked then send button will send the cursor to the next line.
			if (!PreferenceManager.getDefaultSharedPreferences(
					activity.getApplicationContext()).getBoolean(
					HikeConstants.SEND_ENTER_PREF, false))
			{
				mComposeView.setInputType(mComposeView.getInputType()
						| InputType.TYPE_TEXT_FLAG_MULTI_LINE);
			}
			else if ((mComposeView.getInputType() & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == InputType.TYPE_TEXT_FLAG_MULTI_LINE)
			{
				mComposeView.setInputType(mComposeView.getInputType()
						^ InputType.TYPE_TEXT_FLAG_MULTI_LINE);
			}
			//Its a workaround to set the multiline editfield when android:imeOptions="actionSend".
    		mComposeView.setHorizontallyScrolling(false);
			mComposeView.setMaxLines(4);

		}
	}

	/**
	 * Instantiate the mShareable popupLayout
	 */
	private void initShareablePopup()
	{
		if (mShareablePopupLayout == null)
		{
			int[] mEatOuterTouchIds =null;
			if (shouldKeyboardPopupShow)
			{
				 mEatOuterTouchIds = new int[] { R.id.sticker_btn, R.id.emoticon_btn, R.id.send_message , R.id.msg_compose, R.id.sticker_recommendation_parent};	
			}else{
				 mEatOuterTouchIds = new int[] { R.id.sticker_btn, R.id.emoticon_btn, R.id.send_message, R.id.sticker_recommendation_parent};
			}

			initStickerPicker();
			initEmoticonPicker();

			mShareablePopupLayout = new ShareablePopupLayout(activity.getApplicationContext(), activity.findViewById(R.id.chatThreadParentLayout),
					(int) (activity.getResources().getDimension(R.dimen.emoticon_pallete)), mEatOuterTouchIds, this, this);
		}

		else
		{
			updateSharedPopups();
		}

	}

	/**
	 * Used for instantiating the ActionMode.
	 * 
	 * This should be called only once in a chatThread
	 */

	private void initActionMode()
	{
		mActionMode = new HikeActionMode(activity, this);
	}

	/**
	 * Updates the mainView for KeyBoard popup as well as updates the Picker Listeners for Emoticon and Stickers
	 */
	private void updateSharedPopups()
	{
		mShareablePopupLayout.updateListenerAndView(this, activity.findViewById(R.id.chatThreadParentLayout));
		if (mStickerPicker != null)
		{
			mStickerPicker.updateListener(this, activity);
		}
		if (mEmoticonPicker != null)
		{
			mEmoticonPicker.updateETAndContext(mComposeView, activity);
		}
	}

	protected void addOnClickListeners()
	{
		activity.findViewById(R.id.sticker_btn).setOnClickListener(this);
		activity.findViewById(R.id.emoticon_btn).setOnClickListener(this);
		activity.findViewById(R.id.send_message).setOnClickListener(this);
		activity.findViewById(R.id.new_message_indicator).setOnClickListener(this);
		activity.findViewById(R.id.scroll_bottom_indicator).setOnClickListener(this);
		activity.findViewById(R.id.scroll_top_indicator).setOnClickListener(this);
	}

	private void initStickerPicker()
	{
		
		mStickerPicker = mStickerPicker != null ? mStickerPicker : (new StickerPicker(activity, this));
	}

	private void initEmoticonPicker()
	{
		mEmoticonPicker = mEmoticonPicker != null ? mEmoticonPicker : (new EmoticonPicker(activity, mComposeView));
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (mConversation != null)
		{
			// overflow is common between all, one to one and group
			menu.findItem(R.id.overflow_menu).getActionView().setOnClickListener(this);
			mActionBar.setOverflowViewListener(this);
			return true;
		}
		return false;
	}

	public boolean onPrepareOptionsMenu(Menu menu)
	{
		return false;
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.attachment:
			showAttchmentPicker();
			activity.showProductPopup(ProductPopupsConstants.PopupTriggerPoints.ATCH_SCR.ordinal());
			return true;
		}
		return false;
	}

	
	@Override
	public void onPrepareOverflowOptionsMenu(List<OverFlowMenuItem> overflowItems)
	{
		if (overflowItems == null)
		{
			return;
		}

		boolean isMessageListEmpty = isMessageListEmpty();
		for (OverFlowMenuItem overFlowMenuItem : overflowItems)
		{
			switch (overFlowMenuItem.id)
			{
			case R.string.search:
				overFlowMenuItem.enabled = !isMessageListEmpty && !mConversation.isBlocked();
				if (!sharedPreference.getData(HikeMessengerApp.CT_SEARCH_CLICKED, false) && overFlowMenuItem.enabled)
				{
					overFlowMenuItem.drawableId = R.drawable.ic_top_bar_indicator_search;
				}
				else
				{
					overFlowMenuItem.drawableId = 0;
				}
				break;

			case R.string.clear_chat:
			case R.string.email_chat:
				overFlowMenuItem.enabled = !isMessageListEmpty;
				break;
			case R.string.hide_chat:
				overFlowMenuItem.text = getString(StealthModeManager.getInstance().isActive() ? 
						(mConversation.isStealth() ? R.string.mark_visible : R.string.mark_hidden)
						: R.string.hide_chat);
				break;
			}
		}
	}
	
	protected boolean isMessageListEmpty()
	{
		boolean isMessageListEmpty = messages.isEmpty();
		if (messages.size() == 1)
		{
			ConvMessage firstMessage = messages.get(0);
			if (firstMessage.getTypingNotification() != null || firstMessage.isBlockAddHeader())
			{
				isMessageListEmpty = true;
			}
		}
		return isMessageListEmpty;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		LockPattern.onLockActivityResult(activity, requestCode, resultCode, data);
		Logger.i(TAG, "on activity result " + requestCode + " result " + resultCode);
		if (resultCode == Activity.RESULT_CANCELED)
		{
			return;
		}
		switch (requestCode)
		{
		case AttachmentPicker.CAMERA:
			if(!Utils.isPhotosEditEnabled())
			{
				ImageParser.parseResult(activity, resultCode, data, this);
			}
			else
			{
				String filename = Utils.getCameraResultFile();
				if(filename!=null)
				{
					activity.startActivityForResult(IntentFactory.getPictureEditorActivityIntent(activity, filename, false, filename, false), AttachmentPicker.EDITOR);
				}
				else
				{
					imageParseFailed();
				}
			}
			break;
		case AttachmentPicker.AUDIO:
		case AttachmentPicker.VIDEO:
			PickFileParser.onAudioOrVideoResult(requestCode, resultCode, data, this, activity);
			break;
		case AttachmentPicker.LOCATOIN:
			onShareLocation(data);
			break;
		case AttachmentPicker.FILE:
			/**
			 * data == null indicates that we did not select any file to send.
			 */
			if (data != null)
			{
				ChatThreadUtils.onShareFile(activity.getApplicationContext(), msisdn, data, mConversation.isOnHike());
			}
			break;
		case AttachmentPicker.CONTACT:
			onShareContact(resultCode, data);
			break;
		case AttachmentPicker.GALLERY:
		case AttachmentPicker.EDITOR:
			if(resultCode == Activity.RESULT_OK)
			{
				ImageParser.parseResult(activity, resultCode, data, this);
			}
			else if (resultCode == GalleryActivity.GALLERY_ACTIVITY_RESULT_CODE)
			{
				// This would be executed if photos is not enabled on the device
				mConversationsView.requestFocusFromTouch();
				mConversationsView.setSelection(messages.size() - 1);
			}
			break;
		case HikeConstants.PLATFORM_REQUEST:
		case HikeConstants.PLATFORM_FILE_CHOOSE_REQUEST:
			mAdapter.onActivityResult(requestCode, resultCode, data);

		}
	}

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		switch (item.id)
		{
		case R.string.clear_chat:
			showClearConversationDialog();
			break;
		case R.string.email_chat:
			emailChat();
			break;
		case AttachmentPicker.GALLERY:
			startHikeGallery(mConversation.isOnHike());
			break;
		case R.string.search:
			recordSearchOptionClick();
			setupSearchMode(null);
			break;
		case R.string.hide_chat:
			StealthModeManager.getInstance().toggleConversation(msisdn, !mConversation.isStealth(), activity);
			//exiting chat thread 
			if(!StealthModeManager.getInstance().isActive())
			{
				activity.closeChatThread(msisdn);
			}

			break;
		default:
			break;
		}
		recordOverflowItemClicked(item);
	}
	
	private void automateMessages(final int count)
	{
		AsyncTask<Void, Void, Void> automateMessages = new AsyncTask<Void, Void, Void>()
		{

			@Override
			protected Void doInBackground(Void... params)
			{
				for(int i=0; i<count; i++)
				{
					ConvMessage convMessage = Utils.makeConvMessage(msisdn, "Message No. " + i, mConversation.isOnHike());
					HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);
					try
					{							
						Thread.sleep(20);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
				return null;
			}
			
		};
		Utils.executeAsyncTask(automateMessages);
	}
	
	private void recordOverflowItemClicked(OverFlowMenuItem item)
	{
		String ITEM = "item";
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.CHAT_OVRFLW_ITEM).put(ITEM, item.text);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	protected String getString(int stringId)
	{
		return activity.getString(stringId);
	}

	protected Resources getResources()
	{
		return activity.getResources();
	}

	public void setContentView()
	{
		activity.setContentView(getContentView());
		initView();
	}

	protected OverFlowMenuItem[] getOverFlowMenuItems()
	{
		return new OverFlowMenuItem[] {
				new OverFlowMenuItem(getString(R.string.hide_chat), 0, 0, R.string.hide_chat),
				new OverFlowMenuItem(getString(R.string.clear_chat), 0, 0, true, R.string.clear_chat),
				new OverFlowMenuItem(getString(R.string.email_chat), 0, 0, true, R.string.email_chat)};
	}

	protected void showOverflowMenu()
	{
		if (mActionMode != null && mActionMode.isActionModeOn())
		{
			return;
		}

		/**
		 * Hiding any open tip
		 */
		mTips.hideTip();

		// Remove the indicator if any on the overflow menu.
		mActionBar.updateOverflowMenuIndicatorImage(0);

		/**
		 * Hiding the softkeyboard if we are in landscape mode
		 */
		if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			Utils.hideSoftKeyboard(activity.getApplicationContext(), mComposeView);
		}

		int width = getResources().getDimensionPixelSize(R.dimen.overflow_menu_width);
		int rightMargin = width + getResources().getDimensionPixelSize(R.dimen.overflow_menu_right_margin);
		mActionBar.showOverflowMenu(width, LayoutParams.WRAP_CONTENT, -rightMargin, -(int) (0.5 * Utils.scaledDensityMultiplier), activity.findViewById(R.id.attachment_anchor));
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.overflowmenu:
			showOverflowMenu();
			break;
		case R.id.sticker_btn:
			if (mShareablePopupLayout.isBusyInOperations())
			{//  previous task is running don't accept this event
				return;
			}
			setEmoticonButtonSelected(false);
			setStickerButtonSelected(true);
			stickerClicked();
			break;
		case R.id.emoticon_btn:
			if (mShareablePopupLayout.isBusyInOperations())
			{// previous task is running don't accept this event
				return;
			}
			setStickerButtonSelected(false);
			setEmoticonButtonSelected(true);
			emoticonClicked();
			break;
		case R.id.send_message:
			sendButtonClicked();
			break;
		case R.id.new_message_indicator:
			unreadCounterClicked();
			break;
		case R.id.scroll_bottom_indicator:
			bottomScrollIndicatorClicked();
			break;
		case R.id.back:
			actionBarBackPressed();
			break;
		case R.id.contact_info:
			openProfileScreen();
			break;
		case R.id.overlay_layout:
			/**
			 * Do nothing. We simply eat this event to avoid chat thread window from catching this
			 */
			break;
		case R.id.overlay_button:
			onOverlayLayoutClicked((int) v.getTag());
			break;
		case R.id.scroll_top_indicator:
			mConversationsView.setSelection(0);
			hideView(R.id.scroll_top_indicator);
			break;
		case R.id.selected_state_overlay:
			onBlueOverLayClick((ConvMessage) v.getTag(), v);
			break;
		case R.id.next:
			Utils.hideSoftKeyboard(activity.getApplicationContext(), mComposeView);
			searchMessage(true,false);
			break;
		case R.id.previous:
			Utils.hideSoftKeyboard(activity.getApplicationContext(), mComposeView);
			searchMessage(false,false);
			break;
		case R.id.search_clear_btn:
			mComposeView.setText("");
			break;
		case R.id.msg_compose:
			if (mShareablePopupLayout.isShowing())
			{
				mShareablePopupLayout.dismiss();
			}
		default:
			Logger.e(TAG, "onClick Registered but not added in onClick : " + v.toString());
			break;
		}

	}

	protected void sendButtonClicked()
	{
		if (TextUtils.isEmpty(mComposeView.getText()))
		{
			audioRecordClicked();
		}
		else
		{
			sendMessageForStickerRecommendLearning();
			sendMessage();
			dismissStickerRecommendationPopup();
			dismissStickerRecommendTip();
		}
	}

	/**
	 * This function gets text from compose edit text and makes a message and uses {@link ChatThread#sendMessage(ConvMessage)} to send message
	 * 
	 * In addition, it calls {@link ComposeViewWatcher#onMessageSent()}
	 */
	protected void sendMessage()
	{
		ConvMessage convMessage = createConvMessageFromCompose();
		sendMessage(convMessage);
	}

	protected ConvMessage createConvMessageFromCompose()
	{
		String message = mComposeView.getText().toString();
		if (TextUtils.isEmpty(message))
		{
			showToast(R.string.text_empty_error);
			return null; // Do not create message
		}
		ConvMessage convMessage = Utils.makeConvMessage(msisdn, message, mConversation.isOnHike());
		// TODO : PinShowing related code -gaurav
		mComposeView.setText("");
		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.onMessageSent();
		}
		return convMessage;
	}

	/**
	 * This function adds convmessage to list using
	 * 
	 * It publishes a pubsub with {@link HikePubSub#MESSAGE_SENT}
	 */
	protected void sendMessage(ConvMessage convMessage)
	{
		if (convMessage != null)
		{
			addMessage(convMessage);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);
		}
	}
	
	private void sendMessageForStickerRecommendLearning()
	{
		String message = mComposeView.getText().toString();
		if (TextUtils.isEmpty(message))
		{
			return;
		}
		StickerSearchManager.getInstance().sentMessage(message, null, null, null);
		
		if(stickerTagWatcher!= null)
		{
			stickerTagWatcher.sendIgnoreAnalytics();
		}
	}
	
	protected void audioRecordClicked()
	{
		showAudioRecordView();
	}

	protected void showAudioRecordView()
	{
		audioRecordView.show();
	}

	protected void stickerClicked()
	{
		Long time = System.currentTimeMillis();
		initStickerPicker();
		
		closeStickerTip();
		StickerManager.getInstance().sendStickerButtonClickAnalytics();
		
		if (mShareablePopupLayout.togglePopup(mStickerPicker, activity.getResources().getConfiguration().orientation))
		{
			activity.showProductPopup(ProductPopupsConstants.PopupTriggerPoints.STKBUT_BUT.ordinal());
		}
		
		else 
		{
			if (!retryToInflateStickers())
			{
				setStickerButtonSelected(false);
				Toast.makeText(activity.getApplicationContext(), R.string.some_error, Toast.LENGTH_SHORT).show();
			}
		}
		Logger.v(TAG, "Time taken to open sticker pallete : " + (System.currentTimeMillis() - time));
	}
	
	protected void closeStickerTip()
	{
		if (mTips.isGivenTipShowing(ChatThreadTips.STICKER_TIP) || (!mTips.seenTip(ChatThreadTips.STICKER_TIP)))
		{
			mTips.setTipSeen(ChatThreadTips.STICKER_TIP);
		}
	}
	
	public void showStickerRecommendTip()
	{
		mTips.showStickerRecommendFtueTip();
	}
	
	public void dismissStickerRecommendTip()
	{
		if (mTips.isGivenTipShowing(ChatThreadTips.STICKER_RECOMMEND_TIP))
		{
			mTips.hideTip(ChatThreadTips.STICKER_RECOMMEND_TIP);
		}
	}
	
	public void setStickerRecommendFtueTipSeen()
	{
		if (mTips!=null&&mTips.isGivenTipVisible(ChatThreadTips.STICKER_RECOMMEND_TIP))
		{
			Logger.d(TAG, "set sticker recommend tip seen : " + true);
			mTips.setTipSeen(ChatThreadTips.STICKER_RECOMMEND_TIP);
		}
	}

	private void setStickerButtonSelected(boolean selected)
	{
		activity.findViewById(R.id.sticker_btn).setSelected(selected);
	}
	
	private void setEmoticonButtonSelected(boolean selected)
	{
		activity.findViewById(R.id.emoticon_btn).setSelected(selected);
	}

	protected void emoticonClicked()
	{
		Long time = System.currentTimeMillis();
		initEmoticonPicker();
		
		if (!mShareablePopupLayout.togglePopup(mEmoticonPicker, activity.getResources().getConfiguration().orientation))
		{
			if (!retryToInflateEmoticons())
			{
				setEmoticonButtonSelected(false);
				Toast.makeText(activity.getApplicationContext(), R.string.some_error, Toast.LENGTH_SHORT).show();
			}
		}
		
		Logger.v(TAG, "Time taken to open emoticon pallete : " + (System.currentTimeMillis() - time));
	}

	/**
	 * Got a failure while opening emoticon pallete possibly due to null context, mainView is null or mainView.getWindowToken() is null (very rare scenarios though)
	 * @return
	 */
	private boolean retryToInflateEmoticons()
	{
		String errorMsg = "Inside method : retry to inflate emoticons. Houston!, something's not right here";
		HAManager.sendStickerEmoticonStrangeBehaviourReport(errorMsg);
		initShareablePopup();
		return mShareablePopupLayout.togglePopup(mEmoticonPicker, activity.getResources().getConfiguration().orientation);
	}
	
	private boolean retryToInflateStickers()
	{
		String errorMsg = "Inside method : retry to inflate stickers. Houston!, something's not right here";
		HAManager.sendStickerEmoticonStrangeBehaviourReport(errorMsg);
		initShareablePopup();
		return mShareablePopupLayout.togglePopup(mStickerPicker, activity.getResources().getConfiguration().orientation);
	}

	protected void setUpThemePicker()
	{
		/**
		 * We can now dismiss the chatTheme tip if it is there or we can hide any other visible tip
		 */
		if (!wasTipSetSeen(ChatThreadTips.ATOMIC_CHAT_THEME_TIP))
		{
			mTips.hideTip();
		}

		if (themePicker == null)
		{
			themePicker = new ThemePicker(activity, this, currentTheme);
		}
	}

	protected void showAttchmentPicker()
	{
		/**
		 * We can now dismiss the Attachment tip if it is there or we hide any other visible tip
		 */
		if (!wasTipSetSeen(ChatThreadTips.ATOMIC_ATTACHMENT_TIP))
		{
			mTips.hideTip();
		}

		initAttachmentPicker(mConversation.isOnHike());
		int width = (int) (Utils.scaledDensityMultiplier * 270);
		int xOffset = -(int) (276 * Utils.scaledDensityMultiplier);
		int yOffset = -(int) (0.5 * Utils.scaledDensityMultiplier);
		attachmentPicker.show(width, LayoutParams.WRAP_CONTENT, xOffset, yOffset, activity.findViewById(R.id.attachment_anchor), PopupWindow.INPUT_METHOD_NOT_NEEDED);
	}

	/**
	 * Subclasses can override and create as per their use
	 */
	protected void initAttachmentPicker(boolean addContact)
	{
		if (attachmentPicker == null)
		{
			attachmentPicker = new AttachmentPicker(msisdn, this, this, activity, true);
			if (addContact)
			{
				attachmentPicker.appendItem(new OverFlowMenuItem(getString(R.string.contact), 0, R.drawable.ic_attach_contact, AttachmentPicker.CONTACT));
			}
		}
	}

	@Override
	public void themeClicked(ChatTheme theme)
	{
		Logger.i(TAG, "theme clicked " + theme);

		updateUIAsPerTheme(theme);
	}

	@Override
	public void themeSelected(ChatTheme chatTheme)
	{
		Logger.i(TAG, "theme selected " + chatTheme);

		/**
		 * Need to update the UI here as well as theme selected  and current theme could be different
		 */
		
		
		/**
		 * Save current theme and send chat theme message
		 */
		if (currentTheme != chatTheme)
		{
			updateUIAsPerTheme(chatTheme);
			currentTheme = chatTheme;
			sendChatThemeMessage();
		}
	}

	protected void updateUIAsPerTheme(ChatTheme theme)
	{
		if (mAdapter.getChatTheme() == theme && theme == ChatTheme.DEFAULT)
		{
			activity.updateActionBarColor(theme.headerBgResId());
		}
		
		else if (mAdapter.getChatTheme() != theme)
		{
			Logger.i(TAG, "update ui for theme " + theme);

			setConversationTheme(theme);
		}
	}

	protected void setBackground(ChatTheme theme)
	{
		ImageView backgroundImage = (ImageView) activity.findViewById(R.id.background);
		if (theme == ChatTheme.DEFAULT)
		{
			backgroundImage.setImageResource(theme.bgResId());
		}
		else
		{
			backgroundImage.setScaleType(theme.isTiled() ? ScaleType.FIT_XY : ScaleType.MATRIX);
			Drawable drawable = Utils.getChatTheme(theme, activity);
			if(!theme.isTiled())
			{
				ChatThreadUtils.applyMatrixTransformationToImageView(drawable, backgroundImage);
			}
			
			backgroundImage.setImageDrawable(drawable);
		}
	}

	@Override
	public void themeCancelled()
	{
		Logger.i(TAG, "theme cancelled, resetting the default theme if needed.");
		if (currentTheme != mAdapter.getChatTheme())
		{
			setConversationTheme(currentTheme);
		}
	}

	public boolean onBackPressed()
	{
		mShareablePopupLayout.onBackPressed();
		if (removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG, true))
		{
			return true;
		}
		
		if(dismissStickerRecommendationPopup())
		{
			return true;
		}
		
		if (mShareablePopupLayout.isShowing())
		{
			mShareablePopupLayout.dismiss();
			return true;
		}

		if (themePicker != null && themePicker.isShowing())
		{
			return themePicker.onBackPressed();
		}

		if (mActionMode.isActionModeOn())
		{
			mActionMode.finish();
			return true;
		}

		return false;
	}
	
	private void actionBarBackPressed()
	{
		if (mShareablePopupLayout.isShowing())
		{
			mShareablePopupLayout.dismiss();
			return;
		}

		if (removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG, true))
		{
			return;
		}

		activity.backPressed();
	}

	private void removeBroadcastReceiver()
	{
		if (mBroadCastReceiver != null)
		{
			LocalBroadcastManager.getInstance(activity.getBaseContext()).unregisterReceiver(mBroadCastReceiver);
		}
	}

	private void removePubSubListeners()
	{
		Logger.d(TAG, "removing pubSub listeners");
		if (mPubSubListeners != null)
		{
			HikeMessengerApp.getPubSub().removeListeners(this, mPubSubListeners);
		}
	}

	
	/**
	 * If photos enable it will launch a series of activities to return the final edited image.
	 * Delegate activity handles the launching of the required activities in series and handling their respective results
	 * @param onHike
	 */
	private void startHikeGallery(boolean onHike)
	{
		boolean editPic = Utils.isPhotosEditEnabled();
		int galleryFlags = GalleryActivity.GALLERY_ALLOW_MULTISELECT|GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS;
		if(editPic)
		{
			galleryFlags = galleryFlags|GalleryActivity.GALLERY_EDIT_SELECTED_IMAGE;
		}
		Intent imageIntent = IntentFactory.getHikeGalleryPickerIntent(activity.getApplicationContext(),galleryFlags,null);
		imageIntent.putExtra(GalleryActivity.START_FOR_RESULT, true);
		imageIntent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		imageIntent.putExtra(HikeConstants.Extras.ON_HIKE, onHike);
		if(editPic)
		{
			activity.startActivityForResult(imageIntent,AttachmentPicker.GALLERY);
		}
		else
		{
			activity.startActivity(imageIntent);
		}
	}
	
	private void recordSearchOptionClick()
	{
		String CHAT = "chat";
		int chat = 0;
		switch (ChatThreadUtils.getChatThreadType(msisdn))
		{
		case HikeConstants.Extras.ONE_TO_ONE_CHAT_THREAD:
			chat = 1;
			break;
		case HikeConstants.Extras.GROUP_CHAT:
			chat = 2;
			break;
		case HikeConstants.Extras.BROADCAST_CHAT_THREAD:
			chat = 3;
			break;
		}
		try
		{
			JSONObject metadata = new JSONObject();
			metadata
			.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.CHAT_SEARCH)
			.put(CHAT,chat);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}
	
	private void recordSearchInputWithResult(int id, String searchText, int result)
	{
		String PREV = "Prev";
		String NEXT = "Next";
		String LOOP = "Loop";
		String TEXT = "txt";
		String RESULT = "rslt";

		String eventKey;
		if (id == SEARCH_PREVIOUS)
			eventKey = HikeConstants.LogEvent.CHAT_SEARCH + PREV;
		else if (id == SEARCH_NEXT)
			eventKey = HikeConstants.LogEvent.CHAT_SEARCH + NEXT;
		else
			eventKey = HikeConstants.LogEvent.CHAT_SEARCH + LOOP;
		try
		{
			JSONObject metadata = new JSONObject();
			metadata
			.put(HikeConstants.EVENT_KEY, eventKey)
			.put(TEXT, searchText)
			.put(RESULT, result);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}

	private void setupStickerSearch()
	{
		if(!(sharedPreference.getData(HikeConstants.STICKER_RECOMMENDATION_ENABLED, false) && sharedPreference.getData(HikeConstants.STICKER_RECOMMEND_PREF, true)) || (Utils.getExternalStorageState() == ExternalStorageState.NONE))
		{
			return;
		}	
		
		stickerTagWatcher = (stickerTagWatcher != null) ? (stickerTagWatcher) : (new StickerTagWatcher(activity, this, mComposeView, getResources().getColor(R.color.sticker_recommend_highlight_text)));
		StickerSearchHostManager.getInstance().loadChatProfile(msisdn, !ChatThreadUtils.getChatThreadType(msisdn).equals(HikeConstants.Extras.ONE_TO_ONE_CHAT_THREAD), activity.getLastMessageTimeStamp());
		mComposeView.addTextChangedListener(stickerTagWatcher);
	}
	
	public boolean dismissStickerRecommendationPopup()
	{
		if(stickerTagWatcher == null)
		{
			return false;
		}

		if(stickerTagWatcher.isStickerRecommnedPoupShowing())
		{
			stickerTagWatcher.dismissStickerSearchPopup();;
			return true;
		}
		return false;
	}
	
	protected void setupSearchMode(String text)
	{
		searchText = text;
		if (!sharedPreference.getData(HikeMessengerApp.CT_SEARCH_CLICKED, false))
		{
			sharedPreference.saveData(HikeMessengerApp.CT_SEARCH_CLICKED, true);
		}

		mActionMode.showActionMode(SEARCH_ACTION_MODE, R.layout.search_action_bar);
		setUpSearchViews();

		// Creating new instance every time.
		// No need to modify existing instance. It might still be in the process of exiting.
		messageSearchManager = new SearchManager();
		/**
		 * Hiding any open tip
		 */
		mTips.hideTip();
		if (searchText != null)
		{
			mComposeView.setText(searchText);
			mComposeView.setSelection(searchText.length());
		}
		Utils.blockOrientationChange(activity);
	}
	
	private void setUpSearchViews()
	{
		int id = mComposeView.getId();
		mComposeView = (CustomFontEditText) activity.findViewById(R.id.search_text);
		mComposeView.setTag(id);
		
		mComposeView.requestFocus();
		Utils.showSoftKeyboard(activity.getApplicationContext(), mComposeView);
		mComposeView.addTextChangedListener(searchTextWatcher);
		mComposeView.setOnEditorActionListener(this);
		activity.findViewById(R.id.next).setOnClickListener(this);
		activity.findViewById(R.id.previous).setOnClickListener(this);
		activity.findViewById(R.id.search_clear_btn).setOnClickListener(this);
		
		View mBottomView = activity.findViewById(R.id.bottom_panel);
		if (mShareablePopupLayout.isKeyboardOpen())
		{ 
			// ifkeyboard is not open, then keyboard will come which will make so much animation on screen
			mBottomView.startAnimation(AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.up_down_lower_part));
		}
		else
		{
			Utils.toggleSoftKeyboard(activity.getApplicationContext());
		}
		if (mShareablePopupLayout.isShowing())
		{
			mShareablePopupLayout.dismiss();
		}
		mBottomView.setVisibility(View.GONE);
	}

	TextWatcher searchTextWatcher = new TextWatcher()
	{
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{
		}
		
		@Override
		public void afterTextChanged(Editable s)
		{
			View clearSearchText = activity.findViewById(R.id.search_clear_btn);
			if (clearSearchText != null)
			{
				if (TextUtils.isEmpty(s.toString()))
				{
					activity.findViewById(R.id.search_clear_btn).setVisibility(View.GONE);
				}
				else
				{
					activity.findViewById(R.id.search_clear_btn).setVisibility(View.VISIBLE);
				}
			}
			searchText = s.toString().toLowerCase();
			mAdapter.setSearchText(searchText);
			mAdapter.notifyDataSetChanged();
		}
	};

	private void searchMessage(boolean searchNext, boolean loop)
	{
		if (!TextUtils.isEmpty(searchText) &&
				// For some devices like micromax A120, one can get multiple calls from one user-input.
				// Check on the dialog is optimal here as it directly reflects the user intentions.
				(searchDialog == null || !searchDialog.isShowing()))
		{
			searchDialog = ProgressDialog.show(activity, null, getString(R.string.searching));
			// updating the dataset in case any new messages were received
			if (loop)
			{
				activity.getSupportLoaderManager().restartLoader(SEARCH_LOOP, null, this);
			}
			else if (searchNext)
			{
				activity.getSupportLoaderManager().restartLoader(SEARCH_NEXT, null, this);
			}
			else
			{
				activity.getSupportLoaderManager().restartLoader(SEARCH_PREVIOUS, null, this);
			}
		}
	}

	private void updateUIforSearchResult(final int position)
	{
		Logger.d("gaurav","updateUIforSearchResult: " + position);
		searchDialog.dismiss();
		if (messageSearchManager != null && messageSearchManager.isActive())
		{
			if (position >= 0)
			{
				scrollToPosition(position, (int) (40 * Utils.densityMultiplier));
			}
			else
			{
				showToast(R.string.no_results);
			}
		}
		setMessagesRead();
		loadingMoreMessages = false;
	}

	protected void destroySearchMode()
	{
		if (mCurrentActionMode != SEARCH_ACTION_MODE)
		{
			mComposeView = (CustomFontEditText) activity.findViewById(R.id.msg_compose);
			mComposeView.requestFocus();
			mComposeView.removeTextChangedListener(searchTextWatcher);
			if (mEmoticonPicker != null)
			{
				mEmoticonPicker.updateET(mComposeView);
			}
			
			View mBottomView = activity.findViewById(R.id.bottom_panel);
			int lastVisiblePosition = -1;
			int count = -1;
			if (mConversationsView != null)
			{
				lastVisiblePosition = mConversationsView.getLastVisiblePosition();
				count = mConversationsView.getCount();
			}
			mBottomView.setVisibility(View.VISIBLE);
			if (lastVisiblePosition == count - 1)
				uiHandler.sendEmptyMessage(SCROLL_TO_END);
			setupDefaultActionBar(false);
			messageSearchManager.deactivate();
			mAdapter.setSearchText(null);
			searchText = null;
		}
		Utils.unblockOrientationChange(activity);
	}

	protected void showToast(int messageId)
	{
		Toast.makeText(activity, getString(messageId), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void imageParsed(Uri uri)
	{
		ChatThreadUtils.uploadFile(activity.getApplicationContext(), msisdn, uri, HikeFileType.IMAGE, mConversation.isOnHike());
	}

	@Override
	public void imageParsed(String imagePath)
	{
		ChatThreadUtils.uploadFile(activity.getApplicationContext(), msisdn, imagePath, HikeFileType.IMAGE, mConversation.isOnHike(), FTAnalyticEvents.CAMERA_ATTACHEMENT);
	}

	@Override
	public void imageParseFailed()
	{
		FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_3, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "Image Parsing failed. 'Error capturing image'");
		showToast(R.string.error_capture);
		ChatThreadUtils.clearTempData(activity.getApplicationContext());
	}

	@Override
	public void pickFileSuccess(int requestCode, String filePath)
	{
		switch (requestCode)
		{
		case AttachmentPicker.AUDIO:
			ChatThreadUtils.uploadFile(activity.getApplicationContext(), msisdn, filePath, HikeFileType.AUDIO, mConversation.isOnHike(), FTAnalyticEvents.AUDIO_ATTACHEMENT);
			break;
		case AttachmentPicker.VIDEO:
			ChatThreadUtils.uploadFile(activity.getApplicationContext(), msisdn, filePath, HikeFileType.VIDEO, mConversation.isOnHike(), FTAnalyticEvents.VIDEO_ATTACHEMENT);
			break;
		}

	}

	@Override
	public void pickFileFailed(int requestCode)
	{
		switch (requestCode)
		{
		case AttachmentPicker.AUDIO:
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_4_2, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "Audio could not be recorded.");
			showToast(R.string.error_recording);
			break;
		case AttachmentPicker.VIDEO:
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_6, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "Error capturing the video");
			showToast(R.string.error_capture_video);
			break;
		}

	}

	protected void onShareLocation(Intent data)
	{
		if (data == null)
		{
			showToast(R.string.error_pick_location);
			FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_5, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "Error while picking location");
		}
		else
		{
			double latitude = data.getDoubleExtra(HikeConstants.Extras.LATITUDE, 0);
			double longitude = data.getDoubleExtra(HikeConstants.Extras.LONGITUDE, 0);
			int zoomLevel = data.getIntExtra(HikeConstants.Extras.ZOOM_LEVEL, 0);
			ChatThreadUtils.initialiseLocationTransfer(activity.getApplicationContext(), msisdn, latitude, longitude, zoomLevel, mConversation.isOnHike(),true);
		}
	}

	protected void onShareContact(int resultCode, Intent data)
	{
		PhonebookContact contact = PickContactParser.onContactResult(resultCode, data, activity.getApplicationContext());
		if (contact != null)
		{
			this.dialog = HikeDialogFactory.showDialog(activity, HikeDialogFactory.CONTACT_SEND_DIALOG, this, contact, getString(R.string.send), false);
		}
	}

	private void onForwardContact(String contactId)
	{
		PhonebookContact contact = PickContactParser.getContactData(contactId, activity);
		if (contact != null)
		{
			this.dialog = HikeDialogFactory.showDialog(activity, HikeDialogFactory.CONTACT_SEND_DIALOG, this, contact, getString(R.string.send), false);
		}
	}

	@Override
	public void negativeClicked(HikeDialog dialog)
	{
		switch (dialog.getId())
		{
		case HikeDialogFactory.DELETE_MESSAGES_DIALOG:
		case HikeDialogFactory.CONTACT_SEND_DIALOG:
		case HikeDialogFactory.CLEAR_CONVERSATION_DIALOG:
			dialog.dismiss();
			this.dialog = null;
			break;
			
		}
	}

	@Override
	public void positiveClicked(HikeDialog dialog)
	{
		switch (dialog.getId())
		{
		case HikeDialogFactory.CONTACT_SEND_DIALOG:
			ChatThreadUtils.initialiseContactTransfer(activity.getApplicationContext(), msisdn, ((PhonebookContact) dialog.data).jsonData, mConversation.isOnHike());
			dialog.dismiss();

			break;

		case HikeDialogFactory.CLEAR_CONVERSATION_DIALOG:
			clearConversation();
			dialog.dismiss();
			break;

		case HikeDialogFactory.DELETE_MESSAGES_DIALOG:
			ArrayList<Long> selectedMsgIdsToDelete = new ArrayList<Long>(mAdapter.getSelectedMessageIds());
			// TODO if last message is typing notification we will get wrong result here
			ChatThreadUtils.deleteMessagesFromDb(selectedMsgIdsToDelete, ((CustomAlertDialog) dialog).isChecked(), messages.get(messages.size() - 1).getMsgID(), msisdn);
			dialog.dismiss();
			mActionMode.finish();
			break;

		}

	}

	@Override
	public void neutralClicked(HikeDialog dialog)
	{

	}

	protected void setConversationTheme(ChatTheme theme)
	{
		System.gc();
		// messages theme changed, call adapter
		mAdapter.setChatTheme(theme);
		// action bar
		activity.updateActionBarColor(theme.headerBgResId());
		// background image
		setBackground(theme);
	}

	@Override
	public void stickerSelected(Sticker sticker, String sourceOfSticker)
	{
		Logger.i(TAG, "sticker clicked " + sticker.getStickerId() + sticker.getCategoryId() + sourceOfSticker);
		StickerSearchManager.getInstance().sentMessage(null, sticker, null, mComposeView.getText().toString());
		sendSticker(sticker, sourceOfSticker);
	}
	
	@Override
	public void stickerSelectedRecommedationPopup(Sticker sticker, String sourceOfSticker, boolean clearText)
	{
		Logger.i(TAG, "sticker clicked " + sticker.getStickerId() + sticker.getCategoryId() + sourceOfSticker);
		if(clearText)
		{
			StickerSearchManager.getInstance().sentMessage(mComposeView.getText().toString(), sticker, null, null);
		}
		else
		{
			StickerSearchManager.getInstance().sentMessage(null, sticker, null, mComposeView.getText().toString());
		}
		sendSticker(sticker, sourceOfSticker);
	}

	@Override
	public void audioRecordSuccess(String filePath, long duration)
	{
		Logger.i(TAG, "Audio Recorded " + filePath + "--" + duration);
		ChatThreadUtils.initialiseFileTransfer(activity.getApplicationContext(), msisdn, filePath, null, HikeFileType.AUDIO_RECORDING, HikeConstants.VOICE_MESSAGE_CONTENT_TYPE,
				true, duration, false, mConversation.isOnHike(), FTAnalyticEvents.AUDIO_ATTACHEMENT);
	}

	@Override
	public void audioRecordCancelled()
	{
		Logger.i(TAG, "Audio Recorded failed");
	}

	/**
	 * This method calls {@link #fetchConversation(String)} in UI or non UI thread, depending upon async variable For non UI, it starts asyncloader, see {@link ConversationLoader}
	 * 
	 * @param async
	 */
	protected final void fetchConversation(boolean async)
	{
		Logger.i(TAG, "fetch conversation called , isAsync " + async);
		if (async)
		{
			activity.getSupportLoaderManager().initLoader(FETCH_CONV, null, this);
		}
		else
		{
			setupConversation(fetchConversation());
		}
	}
	
	protected final void fetchConversationOnNewIntent(boolean async)
	{
		Logger.i(TAG, "Fetch Conversation called : isAync ? " + async);

		if (async)
		{
			activity.getSupportLoaderManager().restartLoader(FETCH_CONV, null, this);
		}

		else
		{
			setupConversation(fetchConversation());
		}
	}

	/**
	 * This method calls {@link #fetchConversation(String)} in UI or non UI thread, depending upon async variable For non UI, it starts asyncloader, see {@link ConversationLoader}
	 * 
	 * @param async
	 */
	protected final void loadMessage(boolean async)
	{
		Logger.i(TAG, "Load Messages called from onScroll  : Async Call ? " + async);

		if (async)
		{
			/**
			 * Calling restart loader here since if we use initLoader, for subsequent calls, loaderManager would deliver the same result instead of calling load in background
			 * again.
			 */
			activity.getSupportLoaderManager().restartLoader(LOAD_MORE_MESSAGES, null, this);
		}

		else
		{
			loadMoreMessages();
		}
	}
	
	protected final void initializeMessages(boolean async, boolean initializeMessagesAboveCurrentPosition)
	{
		Logger.i("gaurav", "initialize messages called from onScroll  : Async Call ? " + async);
		int startIndex = -1, endIndex = -1;

		if (initializeMessagesAboveCurrentPosition)
		{
			int current = mConversationsView.getFirstVisiblePosition();
			while (current > 0)
			{
				current--;
				if (messages.getRaw(current) == null)
				{
					endIndex = current;
					break;
				}
			}
			if (endIndex < 0)
				return;
			startIndex = Math.max(0, endIndex - HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME);
		}
		else
		{
			int current = mConversationsView.getLastVisiblePosition();
			while (current < messages.size()-1)
			{
				current++;
				if (messages.getRaw(current) == null)
				{
					startIndex = current;
					break;
				}
			}
			if (startIndex < 0)
				return;
			endIndex = Math.min(startIndex + HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME, messages.size() - 1);
		}
		if (async)
		{
			/**
			 * Calling restart loader here since if we use initLoader, for subsequent calls, loaderManager would deliver the same result instead of calling load in background
			 * again.
			 */
			Bundle arguments = new Bundle();
			arguments.putInt(MessageInitializer.START_INDX, startIndex);
			arguments.putInt(MessageInitializer.END_INDX, endIndex);
			activity.getSupportLoaderManager().restartLoader(INITIALIZE_MORE_MESSAGES, arguments, this);
		}
		else
		{
			getMessagesFromDB(messages, startIndex, endIndex);
		}
	}
	
	private void getMessagesFromDB(MovingList<ConvMessage> movingList, int startIndex, int endIndex)
	{
		Logger.d("gaurav","loading more items: " + startIndex + " to " + endIndex);
		Logger.d("gaurav","loading more msgs: " + movingList.getUniqueId(startIndex) + " to " + movingList.getUniqueId(endIndex));
		List<ConvMessage> msgList = loadMoreMessages(-1, movingList.getUniqueId(endIndex) + 1, movingList.getUniqueId(startIndex) - 1);
		for (int i = 0; i < msgList.size(); i++)
		{
			movingList.set(startIndex + i,msgList.get(i));
		}
		uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
		loadingMoreMessages = false;
	}

	/**
	 * This method is either called in either UI thread or non UI, check {@link #fetchConversation(boolean, String)}
	 * 
	 */
	protected abstract Conversation fetchConversation();

	/**
	 * This method is called in NON UI thread when list view scrolls
	 * 
	 * @return List of ConvMessages
	 */
	protected List<ConvMessage> loadMoreMessages()
	{
		return loadMoreMessages(HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME);
	}
	protected List<ConvMessage> loadMoreMessages(int messageCountToLoad)
	{
		int startIndex = getMessagesStartIndex();

		long firstMsgId = messages.get(startIndex).getMsgID();
		Logger.i(TAG, "inside background thread: loading more messages " + firstMsgId);
		
		return loadMoreMessages(messageCountToLoad, firstMsgId, -1);
	}
	
	protected List<ConvMessage> loadMoreMessages(int messageCountToLoad, long maxId, long minId)
	{
		return mConversationDb.getConversationThread(msisdn, messageCountToLoad, mConversation, maxId, minId);
	}

	protected abstract int getContentView();

	/**
	 * This method returns the main msisdn in the present chatThread. It can have different implementations in OneToOne, Group and a Bot Chat
	 * 
	 * @return
	 */
	protected abstract String getMsisdnMainUser();

	/**
	 * This function is called in UI thread when conversation is fetched from DB
	 */
	protected void fetchConversationFinished(Conversation conversation)
	{
		// this function should be called only once per conversation
		Logger.i(TAG, "conversation fetch success");
		mConversation = conversation;
		/*
		 * make a copy of the message list since it's used internally by the adapter
		 * 
		 * Adapter has to show UI elements like tips, day/date of messages, unknown contact headers etc.
		 */
		messages = new MovingList<ConvMessage>(mConversation.getMessagesList(),mOnItemsFinishedListener);
		messages.setLoadBufferSize(HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME);

		mMessageMap = new HashMap<Long, ConvMessage>();
		addtoMessageMap(0, messages.size());

		initListViewAndAdapter(); // init adapter, listView and add clicks etc
		currentTheme = mConversation.getChatTheme();
		updateUIAsPerTheme(currentTheme);// it has to be done after setting adapter
		setupDefaultActionBar(true); // Setup the action bar
		initMessageSenderLayout();

		setMessagesRead(); // Setting messages as read if there are any unread ones

		setEditTextListeners();
		
		activity.supportInvalidateOptionsMenu(); // Calling the onCreate menu here
		// Register broadcasts
		mBroadCastReceiver = new ChatThreadBroadcasts();

		IntentFilter intentFilter = new IntentFilter(StickerManager.STICKERS_UPDATED);
		intentFilter.addAction(StickerManager.MORE_STICKERS_DOWNLOADED);
		intentFilter.addAction(StickerManager.STICKERS_DOWNLOADED);
		intentFilter.addAction(ACTION_KEYBOARD_CLOSED);

		LocalBroadcastManager.getInstance(activity.getBaseContext()).registerReceiver(mBroadCastReceiver, intentFilter);
		
		/**
		 * Adding PubSub, here since all the heavy work related to fetching of messages and setting up UI has been done already.
		 */
		addToPubSub();

		checkAndAddTypingNotifications();
		
		takeActionBasedOnIntent();
		
		/**
		 * Showing the keyboard in case of empty conversation
		 */
		if (shouldShowKeyboard())
		{
			activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
	}
	
	private OnItemsFinishedListener mOnItemsFinishedListener  = new OnItemsFinishedListener()
	{
		@Override
		public void getMoreItems(MovingList<? extends Unique> movingList, int startIndex, int endIndex)
		{
			loadingMoreMessages = true;
			Logger.d("gaurav","mOnItemsFinishedListener.getMoreItems");
			getMessagesFromDB((MovingList<ConvMessage>)movingList, startIndex, endIndex);
		}
	};
	
	protected boolean shouldShowKeyboard()
	{
		return mConversation.getMessagesList().isEmpty() && !mConversation.isBlocked();
	}

	/**
	 * Checks if there is any typing notification present for the given msisdn, if present, it adds it to the ConvMessages
	 */
	protected void checkAndAddTypingNotifications()
	{
		if (HikeMessengerApp.getTypingNotificationSet().containsKey(msisdn))
		{
			setTypingText(true, HikeMessengerApp.getTypingNotificationSet().get(msisdn));
		}
	}
	
	protected void setEditTextListeners()
	{
		mComposeView.addTextChangedListener(this);
		mComposeView.setBackKeyListener(this);

		/**
		 * ensure that when the softkeyboard Done button is pressed (different than the send button we have), we send the message.
		 */
		mComposeView.setOnEditorActionListener(this);

		mComposeView.setOnTouchListener(this);

		mComposeView.setOnKeyListener(this);
	}

	/*
	 * This Function initializes all components which are required to send message, in case you do not want to send message OR want to provide your own functionality, override this
	 */
	protected void initMessageSenderLayout()
	{
		initComposeViewWatcher();
		initGestureDetector();
		((CustomLinearLayout) activity.findViewById(R.id.chat_layout)).setOnSoftKeyboardListener(this);
	}

	protected void initComposeViewWatcher()
	{
		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.releaseResources();
		}
		/* get the number of credits and also listen for changes */
		int mCredits = activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0);

		mComposeViewWatcher = new ComposeViewWatcher(mConversation, mComposeView, (ImageButton) activity.findViewById(R.id.send_message), mCredits,
				activity.getApplicationContext());

		mComposeViewWatcher.init();

		/* check if the send button should be enabled */
		mComposeViewWatcher.setBtnEnabled();
		mComposeView.requestFocus();

	}

	/**
	 * This function is used to initialize the double tap to nudge
	 */
	private void initGestureDetector()
	{
		_doubleTapPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean(HikeConstants.DOUBLE_TAP_PREF, true);
		mGestureDetector = new GestureDetector(activity.getApplicationContext(), this);
	}

	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
		Logger.d(TAG, "Double Tap motion");
		if(mActionMode.isActionModeOn())
		{
			return false;
		}
		if (!_doubleTapPref)
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.UNCHECKED_NUDGE);
                HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException ex)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}
			currentNudgeCount++;
			if(currentNudgeCount>NUDGE_TOAST_OCCURENCE)
			{
				Toast.makeText(activity.getApplicationContext(), R.string.nudge_toast, Toast.LENGTH_SHORT).show();
				currentNudgeCount=0;
			}
			return false;
		}
			sendPoke();
			return true;
	}

	protected void sendPoke()
	{
		ConvMessage convMessage = Utils.makeConvMessage(msisdn, getString(R.string.poke_msg), mConversation.isOnHike());
		ChatThreadUtils.setPokeMetadata(convMessage);
		sendMessage(convMessage);
	}

	private void initListViewAndAdapter()
	{
		mConversationsView = (ListView) activity.findViewById(R.id.conversations_list);
		mAdapter = new MessagesAdapter(activity, messages, mConversation, this, mConversationsView, activity);
		mConversationsView.setAdapter(mAdapter);
		if (mConversation.getUnreadCount() > 0 && !messages.isEmpty())
		{
			ConvMessage message = messages.get(messages.size() - 1);
			if (message.getTypingNotification() != null)
			{
				message = messages.get(messages.size() - 2);
			}
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD)
			{
				// message size could be less than that of the unread count coz microapps have the authority
				//to increase the unread count of the bot they are in. So a safety check to prevent exception.
				int index = (messages.size() - mConversation.getUnreadCount() - 1) ;
				index = index >= 0 ? index : 0;
				mConversationsView.setSelection(index);
			}
			else
			{
				mConversationsView.setSelection(messages.size());
			}
		}
		else
		{
			mConversationsView.setSelection(messages.size());
		}
		mConversationsView.setOnItemLongClickListener(this);
		mConversationsView.setOnTouchListener(this);

		/**
		 * Hacky fix to ensure onScroll is not called for the first time
		 */
		loadingMoreMessages = true;
		mConversationsView.setOnScrollListener(this);
		loadingMoreMessages = false;

	}

	protected void takeActionBasedOnIntent()
	{
		Logger.i(TAG, "take action based on intent");
		if(savedState!=null && savedState.getBoolean(HikeConstants.CONSUMED_FORWARDED_DATA)) {
			Logger.i(TAG, "consumed forwarded data");
			return;
		}
		Intent intent = activity.getIntent();

		/**
		 * 1. Has an existing message in intent
		 */
		if (intent.hasExtra(HikeConstants.Extras.MSG))
		{
			String msg = intent.getStringExtra(HikeConstants.Extras.MSG);
			mComposeView.setText(msg);
			mComposeView.setSelection(mComposeView.length());
			SmileyParser.getInstance().addSmileyToEditable(mComposeView.getText(), false);
		}

		/**
		 * 2. Has a contactId, i.e. we are trying to share a contact from external sources
		 */
		else if (intent.hasExtra(HikeConstants.Extras.CONTACT_ID))
		{
			String contactId = intent.getStringExtra(HikeConstants.Extras.CONTACT_ID);
			if (TextUtils.isEmpty(contactId))
			{
				Toast.makeText(activity.getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
				FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_2_6, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "TakeActionBasedOnIntent - Contact Id is empty");
			}
			else
			{
				onForwardContact(contactId);
			}
		}

		/**
		 * 3. Trying to forward a file
		 */
		else if (intent.hasExtra(HikeConstants.Extras.FILE_PATH))
		{
			ChatThreadUtils.onShareFile(activity.getApplicationContext(), msisdn, intent, mConversation.isOnHike());
			// Making sure the file does not get forwarded again on
			// orientation change.
			intent.removeExtra(HikeConstants.Extras.FILE_PATH);
		}

		/**
		 * 4. Multi Forward :
		 */

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
						ConvMessage convMessage = Utils.makeConvMessage(msisdn, msg, mConversation.isOnHike());
						sendMessage(convMessage);
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.POKE))
					{
						sendPoke();
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
						
						int attachmentType = FTAnalyticEvents.OTHER_ATTACHEMENT;
						/*
						 * Added to know the attachment type when selected from file.
						 */
						if (intent.hasExtra(FTAnalyticEvents.FT_ATTACHEMENT_TYPE))
						{
							attachmentType = FTAnalyticEvents.FILE_ATTACHEMENT;

						}

						HikeFileType hikeFileType = HikeFileType.fromString(fileType, isRecording);

						if (Utils.isPicasaUri(filePath))
						{
							FileTransferManager.getInstance(activity.getApplicationContext()).uploadFile(Uri.parse(filePath), hikeFileType, msisdn, mConversation.isOnHike());
						}
						else
						{
							ChatThreadUtils.initialiseFileTransfer(activity.getApplicationContext(), msisdn, filePath, fileKey, hikeFileType, fileType, isRecording,
									recordingDuration, true, mConversation.isOnHike(), attachmentType);
						}
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.LATITUDE) && msgExtrasJson.has(HikeConstants.Extras.LONGITUDE)
							&& msgExtrasJson.has(HikeConstants.Extras.ZOOM_LEVEL))
					{
						double latitude = msgExtrasJson.getDouble(HikeConstants.Extras.LATITUDE);
						double longitude = msgExtrasJson.getDouble(HikeConstants.Extras.LONGITUDE);
						int zoomLevel = msgExtrasJson.getInt(HikeConstants.Extras.ZOOM_LEVEL);
						ChatThreadUtils.initialiseLocationTransfer(activity.getApplicationContext(), msisdn, latitude, longitude, zoomLevel, mConversation.isOnHike(),true);
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.CONTACT_METADATA))
					{
						try
						{
							JSONObject contactJson = new JSONObject(msgExtrasJson.getString(HikeConstants.Extras.CONTACT_METADATA));
							ChatThreadUtils.initialiseContactTransfer(activity.getApplicationContext(), msisdn, contactJson, mConversation.isOnHike());
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
						sendSticker(sticker, StickerManager.FROM_FORWARD);
						boolean isDis = sticker.isDisabled(sticker, activity.getApplicationContext());
						// add this sticker to recents if this sticker is not disabled
						if (!isDis)
						{
							StickerManager.getInstance().addRecentSticker(sticker);
						}
						/*
						 * Making sure the sticker is not forwarded again on orientation change
						 */
						intent.removeExtra(StickerManager.FWD_CATEGORY_ID);
					}

                    else if(msgExtrasJson.optInt(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE) == HikeConstants.MESSAGE_TYPE.CONTENT){
                        // as we will be changing msisdn and hike status while inserting in DB
                        ConvMessage convMessage = Utils.makeConvMessage(msisdn, mConversation.isOnHike());
                        convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.CONTENT);
                        convMessage.platformMessageMetadata = new PlatformMessageMetadata(msgExtrasJson.optString(HikeConstants.METADATA), activity.getApplicationContext());
                        convMessage.platformMessageMetadata.addThumbnailsToMetadata();
                        convMessage.setMessage(convMessage.platformMessageMetadata.notifText);

                        sendMessage(convMessage);

                    }

					else if(msgExtrasJson.optInt(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE) == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || msgExtrasJson.optInt(
							HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE) == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT){
						// as we will be changing msisdn and hike status while inserting in DB
						ConvMessage convMessage = Utils.makeConvMessage(msisdn,msgExtrasJson.getString(HikeConstants.HIKE_MESSAGE), mConversation.isOnHike());
						convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT);
						convMessage.webMetadata = new WebMetadata(msgExtrasJson.optString(HikeConstants.METADATA));
						JSONObject json = new JSONObject();
						try
						{
							json.put(HikePlatformConstants.CARD_TYPE, convMessage.webMetadata.getAppName());
							json.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.CARD_FORWARD);
							json.put(AnalyticsConstants.TO, msisdn);
							HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
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


				}
				
				if (mActionMode != null && mActionMode.isActionModeOn())
				{
					mActionMode.finish();
				}
			}
			catch (JSONException e)
			{
				Logger.e(TAG, "Invalid JSON Array", e);
			}
			intent.removeExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
		}

		/**
		 * 5. Multiple files
		 */
		else if (intent.hasExtra(HikeConstants.Extras.FILE_PATHS))
		{
			ArrayList<String> filePaths = intent.getStringArrayListExtra(HikeConstants.Extras.FILE_PATHS);
			String fileType = intent.getStringExtra(HikeConstants.Extras.FILE_TYPE);
			for (String filePath : filePaths)
			{
				ChatThreadUtils.initiateFileTransferFromIntentData(activity.getApplicationContext(), msisdn, fileType, filePath, mConversation.isOnHike(), FTAnalyticEvents.OTHER_ATTACHEMENT);
			}
			intent.removeExtra(HikeConstants.Extras.FILE_PATHS);
		}

		/**
		 * 6. Show audio recording dialog
		 */
		else if(intent.hasExtra(HikeConstants.Extras.SHOW_RECORDING_DIALOG))
		{
			showAudioRecordView();
		}
		
		/**
		 * 7. Since the message was not forwarded, we check if we have any drafts saved for this conversation, if we do we enter it in the compose box.
		 */
		else
		{
			String message = activity.getApplicationContext().getSharedPreferences(HikeConstants.DRAFT_SETTING, Context.MODE_PRIVATE).getString(msisdn, "");
			mComposeView.setText(message);
			mComposeView.setSelection(mComposeView.length());
			SmileyParser.getInstance().addSmileyToEditable(mComposeView.getText(), false);
		}
		consumedForwardedData = true;
	}

	/*
	 * This function is called in UI thread when conversation fetch is failed from DB, By default we finish activity, override in case you want to do something else
	 */
	protected void fetchConversationFailed()
	{
		Logger.e(TAG, "conversation fetch failed");
		activity.finish();
	}

	/**
	 * This function is called in UI thread when message loading is finished
	 */
	protected void loadMessagesFinished(List<ConvMessage> list)
	{
		if (list == null)
		{
			Logger.e(TAG, "load message failed");
		}
		else
		{
			if (!list.isEmpty())
			{
				Logger.i(TAG, "Adding 'n' new messages in the list : " + list.size());
				addAndSetMessages(list);
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
	}

	private void addAndSetMessages(List<ConvMessage> list)
	{
		int firstVisibleItem = mConversationsView.getFirstVisiblePosition();
		int scrollOffset = 0;
		if (mConversationsView.getChildAt(0) != null)
		{
			scrollOffset = mConversationsView.getChildAt(0).getTop();
		}

		addMoreMessages(list);

		mConversationsView.setSelectionFromTop(firstVisibleItem + list.size(), scrollOffset);
	}

	private void updateMessageList(MovingList<ConvMessage> newList, int oldIndex)
	{
		Logger.d("gaurav","updating MessageList");
		Logger.d("gaurav","new size:" + newList.size());
		Logger.d("gaurav","old size:" + messages.size());
		Logger.d("gaurav","oldIndex : "  + oldIndex);
		int firstMessageFromBottom = messages.size() - mConversationsView.getFirstVisiblePosition();
		int scrollOffset = 0;
		if (mConversationsView.getChildAt(0) != null)
		{
			scrollOffset = mConversationsView.getChildAt(0).getTop();
		}
		if (messages.size() - HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY < oldIndex)
		{
			int from = oldIndex - 1;
			int to = Math.max(messages.size() - HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, 0);
			int position = newList.size()-1;
			while (from >= to)
			{
				newList.set(position, messages.getRaw(from), messages.getUniqueId(from));
				position--;
				from--;
				if (position < 0 || from < 0)
					break;
			}
		}
		for(int i = oldIndex; i<messages.size(); i++)
		{
			newList.add(newList.size(), messages.getRaw(i), messages.getUniqueId(i));
		}
		messages = newList;
		mAdapter.updateMessageList(messages);
		Logger.d("gaurav","final size:" + messages.size());
		Logger.d("gaurav","setting selection back");
		mConversationsView.setSelectionFromTop(newList.size() - firstMessageFromBottom, scrollOffset);
		Logger.d("gaurav","update message list complete");
	}

	private void addMoreMessages(List<ConvMessage> list)
	{
		int startIndex = getMessagesStartIndex();
		mAdapter.addMessages(list, startIndex);
		addtoMessageMap(startIndex, startIndex + list.size());
		mAdapter.notifyDataSetChanged();
	}
	
	private int getMessagesStartIndex()
	{
		if (!messages.isEmpty() && messages.get(0).isBlockAddHeader())
		{
			return 1;
		}
		return 0;
	}

	@Override
	public Loader<Object> onCreateLoader(int arg0, Bundle arg1)
	{
		Logger.d(TAG, "on create loader is called " + arg0);
		if (arg0 == FETCH_CONV)
		{
			return new ConversationLoader(activity.getApplicationContext(), FETCH_CONV, this);
		}
		else if (arg0 == LOAD_MORE_MESSAGES)
		{
			return new ConversationLoader(activity.getApplicationContext(), LOAD_MORE_MESSAGES, this);
		}
		else if (arg0 == INITIALIZE_MORE_MESSAGES)
		{
			return new MessageInitializer(activity.getApplicationContext(), this, arg1);
		}
		else if (arg0 == SEARCH_NEXT || arg0 == SEARCH_PREVIOUS || arg0 == SEARCH_LOOP)
		{
			return new MessageFinder(activity.getApplicationContext(), arg0, this);
		}
		else
		{
			throw new IllegalArgumentException("On create loader is called with wrong loader id");
		}
	}

	@Override
	public void onLoadFinished(Loader<Object> arg0, Object arg1)
	{
		Logger.d(TAG, "onLoadFinished");
		if (arg0 instanceof ConversationLoader)
		{
			ConversationLoader loader = (ConversationLoader) arg0;
			if (loader.loaderId == FETCH_CONV)
			{
				setupConversation((Conversation) arg1);
			}
			else if (loader.loaderId == LOAD_MORE_MESSAGES)
			{
				loadMessagesFinished((List<ConvMessage>) arg1);
			}
		}
		else if (arg0 instanceof MessageFinder)
		{
			MessageFinder loader = (MessageFinder) arg0;
			int id = loader.loaderId;
			if (id == SEARCH_LOOP || id == SEARCH_NEXT || id == SEARCH_PREVIOUS)
			{
				sendUIMessage(SEARCH_RESULT, 320, (int) arg1);
				recordSearchInputWithResult(id, searchText, (int) arg1);
			}
		}
		else if (arg0 instanceof MessageInitializer)
		{
		}
		else
		{
			throw new IllegalStateException("Expected data is either Conversation OR List<ConvMessages> , please check " + arg0.getClass().getCanonicalName());
		}
	}
	
	/**
	 * This method is called after we've made db query. This checks whether we should show the conversation fetched or if it is null, we clear can take appropriate action 
	 * @param conv
	 */
	protected void setupConversation(Conversation conv)
	{
		if (conv == null)
		{
			fetchConversationFailed();
		}
		else
		{
			fetchConversationFinished(conv);
		}
	}

	@Override
	public void onLoaderReset(Loader<Object> arg0)
	{
		
	}

	private static class ConversationLoader extends AsyncTaskLoader<Object>
	{
		int loaderId;

		private Conversation conversation;

		private List<ConvMessage> list;

		WeakReference<ChatThread> chatThread;

		public ConversationLoader(Context context, int loaderId, ChatThread chatThread)
		{
			super(context);
			Logger.i(TAG, "conversation loader object " + loaderId);
			this.loaderId = loaderId;
			this.chatThread = new WeakReference<ChatThread>(chatThread);
		}

		@Override
		public Object loadInBackground()
		{
			Logger.i(TAG, "load in background of conversation loader");

			if (chatThread.get() != null)
			{
				return loaderId == FETCH_CONV ? (conversation = chatThread.get().fetchConversation()) : (loaderId == LOAD_MORE_MESSAGES ? list = chatThread.get()
						.loadMoreMessages() : null);
			}
			return null;
		}

		/**
		 * This has to be done due to some bug in compat library -- http://stackoverflow.com/questions/10524667/android-asynctaskloader-doesnt-start-loadinbackground
		 */
		protected void onStartLoading()
		{
			Logger.i(TAG, "conversation loader onStartLoading");
			if (loaderId == FETCH_CONV && conversation != null)
			{
				deliverResult(conversation);
			}
			else if (loaderId == LOAD_MORE_MESSAGES && list != null)
			{
				deliverResult(list);
			}
			else
			{
				forceLoad();
			}
		}

	}

	private static class MessageInitializer extends AsyncTaskLoader<Object>
	{
		static final String START_INDX = "startIndex";
		static final String END_INDX = "endIndex";
		private List<ConvMessage> list;

		WeakReference<ChatThread> chatThread;

		int startIndex;

		int endIndex;
		
		boolean taskComplete = false;

		public MessageInitializer(Context context, ChatThread chatThread, Bundle bundle)
		{
			super(context);
			Logger.i("gaurav", "MessageInitializer loader object");
			this.chatThread = new WeakReference<ChatThread>(chatThread);
			this.startIndex = bundle.getInt(START_INDX);
			this.endIndex = bundle.getInt(END_INDX);
		}

		@Override
		public Object loadInBackground()
		{
			Logger.i(TAG, "load in background of conversation loader");

			if (chatThread.get() != null)
			{
				chatThread.get().getMessagesFromDB(chatThread.get().messages, startIndex, endIndex);
			}
			taskComplete = true;
			return null;
		}

		/**
		 * This has to be done due to some bug in compat library -- http://stackoverflow.com/questions/10524667/android-asynctaskloader-doesnt-start-loadinbackground
		 */
		protected void onStartLoading()
		{
			Logger.i(TAG, "conversation loader onStartLoading");
			if (taskComplete)
			{
				deliverResult(null);
			}
			else
			{
				forceLoad();
			}
		}

	}

	private static class MessageFinder extends AsyncTaskLoader<Object>
	{
		static final int MaxMsgLoadCount = 3200;

		int loaderId;
	
		int position = -2;

		int loadMessageCount = 50;
		
		boolean taskComplete = false;

		WeakReference<ChatThread> chatThread;

		public MessageFinder(Context context, int loaderId, ChatThread chatThread)
		{
			super(context);
			Logger.i(TAG, "message finder object " + loaderId);
			this.loaderId = loaderId;
			this.chatThread = new WeakReference<ChatThread>(chatThread);
		}

		@Override
		public Object loadInBackground()
		{
			Logger.i("gaurav", "search in background: " + loaderId);

			if (chatThread.get() != null && !chatThread.get().isMessageListEmpty())
			{
				chatThread.get().loadingMoreMessages = true;
				int msgSize = chatThread.get().messages.size();
				int firstVisisbleItem = chatThread.get().mConversationsView.getFirstVisiblePosition();
				Logger.d("gaurav","firstVisisbleItem : "  + firstVisisbleItem);
				if (firstVisisbleItem < msgSize-1)
					firstVisisbleItem++;
				ArrayList<ConvMessage> msgList;
				if (loaderId == SEARCH_PREVIOUS || loaderId == SEARCH_LOOP)
				{
					long maxId = chatThread.get().messages.getUniqueId(firstVisisbleItem);
					long minId = -1;
					ArrayList<Long> ids = new ArrayList<Long>();
					//position = messageSearchManager.searchFirstItem(chatThread.get().messages, firstVisisbleItem - 1, 0, chatThread.get().searchText);
					while (position < 0 && messageSearchManager.isActive())
					{
						Logger.d("gaurav", "loadmoremessages for search: " + loadMessageCount + " " + maxId + " " + minId);
						msgList = new ArrayList<>(chatThread.get().loadMoreMessages(loadMessageCount, maxId, minId));
						if (msgList == null || msgList.isEmpty() || !messageSearchManager.isActive())
						{
							break;
						}
						position = messageSearchManager.searchFirstItem(msgList, msgList.size(), 0, chatThread.get().searchText);
						ids.addAll(0, MovingList.getIds(msgList));
						if (position >= 0)
						{
							Logger.d("gaurav","found at pos: "+ position + ", id:" + msgList.get(position).getMsgID());
							int start = Math.max(position - HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME, 0);
							int end = Math.min(position + HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME, msgList.size()-1);
							ArrayList<ConvMessage> toBeAddedList = new ArrayList<ConvMessage>(ids.size());
							Utils.preFillArrayList(toBeAddedList, ids.size());
							for (int i = start; i <= end; i++)
							{
								toBeAddedList.set(i, msgList.get(i));
							}
							MovingList<ConvMessage> movingList = new MovingList<ConvMessage>(toBeAddedList, ids, chatThread.get().mOnItemsFinishedListener);
							movingList.setLoadBufferSize(HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME);
							chatThread.get().sendUIMessage(chatThread.get().UPDATE_MESSAGE_LIST,new Pair<>(movingList, firstVisisbleItem));
						}
						else
						{
							//No need to load more than 3200 messaging in one go.
							if (loadMessageCount < MaxMsgLoadCount)
							{
								loadMessageCount *= 2;
							}
							maxId = msgList.get(0).getMsgID();
						}
					}
					if (loaderId == SEARCH_LOOP && position < 0)
					{
						Logger.d("gaurav","shifting to next");
						loaderId = SEARCH_NEXT;
					}
				}
				if (loaderId == SEARCH_NEXT)
				{
					int count = firstVisisbleItem;
					long minId = chatThread.get().messages.getUniqueId(firstVisisbleItem);
					int maxIdPosition = Math.min(count + loadMessageCount, msgSize - 1);
					long maxId = chatThread.get().messages.getUniqueId(maxIdPosition);
					while (position < 0 && messageSearchManager.isActive())
					{
						Logger.d("gaurav", "loadmoremessages for search: " + loadMessageCount + " " + maxId + " " + minId);
						msgList = new ArrayList<>(chatThread.get().loadMoreMessages(loadMessageCount, maxId + 1, minId));
						if (msgList == null || msgList.isEmpty() || !messageSearchManager.isActive())
						{
							break;
						}
						position = messageSearchManager.searchFirstItem(msgList, 0, msgList.size(), chatThread.get().searchText);
						if (position >= 0)
						{
							Logger.d("gaurav","found at pos: "+ position + ", id:" + msgList.get(position).getMsgID());
							count += (position + 1);
							position = count;
						}
						else
						{
							count += msgList.size(); 
							//No need to load more than 3200 messaging in one go.
							if (loadMessageCount < MaxMsgLoadCount)
							{
								loadMessageCount *= 2;
							}
							minId = msgList.get(msgList.size() - 1).getMsgID();
							maxIdPosition = Math.min(count + loadMessageCount, msgSize - 1);
							maxId = chatThread.get().messages.getUniqueId(maxIdPosition);
						}
					}
				}
				msgList = null;
			}
			taskComplete = true;
			Logger.d("gaurav","found at position: " + position);
			return position;
		}

		/**
		 * This has to be done due to some bug in compat library -- http://stackoverflow.com/questions/10524667/android-asynctaskloader-doesnt-start-loadinbackground
		 */
		protected void onStartLoading()
		{
			Logger.i(TAG, "message finder onStartLoading");
			// The search manager returns the values greater than equal to -1
			// So if the loader has executed, the result is always greater than -2.
			// Else we need to start the loader.
			if (taskComplete)
			{
				deliverResult(position);
			}
			else
			{
				forceLoad();
			}
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		return showMessageContextMenu(mAdapter.getItem(position - mConversationsView.getHeaderViewsCount()), view);
	}

	protected boolean showMessageContextMenu(ConvMessage message, View v)
	{
		if (shouldProcessMessagesOnTap(message))
		{
			/**
			 * Inflate ActionMode, if some other action mode is open
			 */
			if (mActionMode.whichActionModeIsOn() != MULTI_SELECT_ACTION_MODE)
			{
				mActionMode.showActionMode(MULTI_SELECT_ACTION_MODE, activity.getString(R.string.selected_count, mAdapter.getSelectedCount()), true, R.menu.multi_select_chat_menu, HikeActionMode.DEFAULT_LAYOUT_RESID);
			}

			/**
			 * Update ActionMode
			 */
			else
			{
				mActionMode.updateTitle(activity.getString(R.string.selected_count, mAdapter.getSelectedCount()));
			}

			processMessageOnTap(message, mAdapter.isSelected(message));

			mAdapter.setActionMode(true);
			v.setVisibility(mAdapter.isSelected(message) ? View.VISIBLE : View.INVISIBLE);

			hideShowActionModeMenus();
			/**
			 * Hiding any open tip
			 */
			mTips.hideTip();

			return true;
		}

		else
		{
			return false;
		}
	}

	private boolean shouldProcessMessagesOnTap(ConvMessage message)
	{
		/**
		 * Exit Condition 1.
		 */
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
		/**
		 * If there are no selected items, then finish the actionMode Exit Condition 2
		 */
		if (!(mAdapter.getSelectedCount() > 0))
		{
			mActionMode.finish();
			/**
			 * Showing any hidden tip
			 */
			mTips.showHiddenTip();
			return false;
		}

		/**
		 * Do not inflate ActionMode if any actionMode is on other than MULTI_SELECT_MODE
		 */
		int whichActionMode = mActionMode.whichActionModeIsOn();

		/**
		 * Exit Condition 3
		 */
		if (whichActionMode != -1 && whichActionMode != MULTI_SELECT_ACTION_MODE)
		{
			return false;
		}

		return true;
	}

	private void processMessageOnTap(ConvMessage message, boolean isMsgSelected)
	{
		if (message.isFileTransferMessage())
		{
			selectedNonTextMsgs = ChatThreadUtils.incrementDecrementMsgsCount(selectedNonTextMsgs, isMsgSelected);

			HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
			
			if ((message.isSent() && TextUtils.isEmpty(hikeFile.getFileKey())) || (!message.isSent() && !hikeFile.wasFileDownloaded()))
			{
				/*
				 * This message has not been downloaded or uploaded yet. this can't be forwarded
				 */
				if (message.isSent())
				{
					selectedNonForwadableMsgs = ChatThreadUtils.incrementDecrementMsgsCount(selectedNonForwadableMsgs, isMsgSelected);
				}
			}
			else
			{
				HikeFileType ftype = hikeFile.getHikeFileType();
				// we do not support location and contact sharing
				if (ftype != HikeFileType.LOCATION && ftype != HikeFileType.CONTACT)
				{
					shareableMessagesCount = ChatThreadUtils.incrementDecrementMsgsCount(shareableMessagesCount, isMsgSelected);
				}
			}
		}

		else if (message.getMetadata() != null && message.getMetadata().isPokeMessage())
		{
			// Poke message can only be deleted
			selectedNonTextMsgs = ChatThreadUtils.incrementDecrementMsgsCount(selectedNonTextMsgs, isMsgSelected);
		}
		else if (message.isStickerMessage())
		{
			// Sticker message is a non text message.
			selectedNonTextMsgs = ChatThreadUtils.incrementDecrementMsgsCount(selectedNonTextMsgs, isMsgSelected);
		}
		
		else if (message.getMessageType() == MESSAGE_TYPE.CONTENT || message.getMessageType() == MESSAGE_TYPE.FORWARD_WEB_CONTENT || message.getMessageType() == MESSAGE_TYPE.WEB_CONTENT)
        {
            // Content card is a non text message.
            selectedNonTextMsgs = ChatThreadUtils.incrementDecrementMsgsCount(selectedNonTextMsgs, isMsgSelected);
        }

	}

	/**
	 * This method is used to hiding/showing multi_select_action_bar_menus
	 */
	private void hideShowActionModeMenus()
	{
		mActionMode.showHideMenuItem(R.id.copy_msgs, selectedNonTextMsgs == 0);

		mActionMode.showHideMenuItem(R.id.share_msgs, shareableMessagesCount == 1 && mAdapter.getSelectedCount() == 1);

		mActionMode.showHideMenuItem(R.id.forward_msgs, !(selectedNonForwadableMsgs > 0));
	}

	protected void destroyActionMode()
	{
		resetSelectedMessageCounters();
		mAdapter.removeSelection();
		mAdapter.setActionMode(false);
		mAdapter.notifyDataSetChanged();
		/**
		 * if we have hidden tips while initializing action mode we should unhide them
		 */ 
		mTips.showHiddenTip();
	}
	
	private void resetSelectedMessageCounters()
	{
		shareableMessagesCount = 0;
		selectedNonForwadableMsgs = 0;
		selectedNonForwadableMsgs = 0;
		selectedNonTextMsgs = 0;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		/**
		 * Something's not right with firstVisibleItem here. Need to verify it - Piyush
		 */
		if (!reachedEnd && !loadingMoreMessages && messages != null && !messages.isEmpty() && firstVisibleItem <= HikeConstants.MIN_INDEX_TO_LOAD_MORE_MESSAGES)
		{
			int startIndex = getMessagesStartIndex();

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

			loadingMoreMessages = true;
			Logger.d(TAG, "Calling load more messages : ");
			loadMessage(true);
		}
		else if (!loadingMoreMessages && messages != null && !messages.isEmpty())
		{
			int lastVisibleItem = firstVisibleItem + visibleItemCount - 1;
			if (messages.getRaw(Math.min(lastVisibleItem + HikeConstants.MIN_INDEX_TO_LOAD_MORE_MESSAGES, messages.size()-1)) == null)
			{
				loadingMoreMessages = true;
				initializeMessages(true, false);
			}
			else if (messages.getRaw(Math.max(firstVisibleItem - HikeConstants.MIN_INDEX_TO_LOAD_MORE_MESSAGES, 0)) == null)
			{
				loadingMoreMessages = true;
				initializeMessages(true, true);
			}
		}

		showOverflowIndicatorIfRequired(firstVisibleItem, visibleItemCount, totalItemCount);

		View unreadMessageIndicator = activity.findViewById(R.id.new_message_indicator);

		if (unreadMessageIndicator.getVisibility() == View.VISIBLE && mConversationsView.getLastVisiblePosition() > messages.size() - unreadMessageCount - 2)
		{
			hideUnreadCountIndicator();
		}

		if (view.getLastVisiblePosition() < messages.size() - HikeConstants.MAX_FAST_SCROLL_VISIBLE_POSITION)
		{
			if (currentFirstVisibleItem < firstVisibleItem)
			{
				if (unreadMessageIndicator.getVisibility() == View.GONE)
				{
					showView(R.id.scroll_bottom_indicator);
				}

				hideView(R.id.scroll_top_indicator);
			}

			else if (currentFirstVisibleItem > firstVisibleItem)
			{
				hideView(R.id.scroll_bottom_indicator);
				/*
				 * if user is viewing message less than certain position in chatthread we should not show topfast scroll.
				 */
				if (firstVisibleItem > HikeConstants.MAX_FAST_SCROLL_VISIBLE_POSITION)
				{
					showView(R.id.scroll_top_indicator);
				}
				else
				{
					hideView(R.id.scroll_top_indicator);
				}
			}
		}
		else
		{
			hideView(R.id.scroll_bottom_indicator);
			hideView(R.id.scroll_top_indicator);
		}
		currentFirstVisibleItem = firstVisibleItem;
	}
	
	private void showOverflowIndicatorIfRequired(int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		showOverflowSearchIndicatorIfRequired(firstVisibleItem, visibleItemCount, totalItemCount);
	}

	private void showOverflowSearchIndicatorIfRequired(int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		/* 
		 * SEARCH INDICATOR
		 * The search indicator over overflow menu item is shown if:
		 *  - It hasn't been shown before
		 *  - Theres nothing being displayed in its position right now, like gc pin unread count.
		 *  - User has scrolled up by atleast 1 message.
		 *  - Messages not are being loaded.
		 *  Note: This is to be shown only once
		 */
		if (!ctSearchIndicatorShown && !mActionBar.isOverflowMenuIndicatorInUse()
				&& (firstVisibleItem + visibleItemCount + 1) < totalItemCount && !loadingMoreMessages)
		{
			// If user has already discovered search option, theres on need to show the search icon on overflow menu icon.
			// Just mark it already shown and move on.
			if (sharedPreference.getData(HikeMessengerApp.CT_SEARCH_CLICKED, false))
			{
				ctSearchIndicatorShown = true;
				sharedPreference.saveData(HikeMessengerApp.CT_SEARCH_INDICATOR_SHOWN, true);
			}
			// If the indicator is successfully displayed the setting is saved, so that its not shown again.
			else if (mActionBar.updateOverflowMenuIndicatorImage(R.drawable.ic_top_bar_indicator_search))
			{
				ctSearchIndicatorShown = true;
				sharedPreference.saveData(HikeMessengerApp.CT_SEARCH_INDICATOR_SHOWN, true);
			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		switch (v.getId())
		{
		case R.id.messageedittext:
			return mShareablePopupLayout.onEditTextTouch(v, event);

		case R.id.msg_compose:

			if(stickerTagWatcher != null)
			{
				stickerTagWatcher.onTouch(v, event);
			}
			
			/**
			 * Fix for android bug, where the focus is removed from the edittext when you have a layout with tabs (Emoticon layout) for hard keyboard devices
			 * http://code.google.com/p/android/issues/detail?id=2516
			 */
			if (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS)
			{
				mComposeView.requestFocusFromTouch();
			}
			return mShareablePopupLayout.onEditTextTouch(v, event);

		default:
			return mGestureDetector.onTouchEvent(event);
		}

	}

	@Override
	public void onScrollStateChanged(AbsListView myListView, int scrollState)
	{
		Logger.i(TAG, "Scroll State  in chatThread : " + scrollState);

		View bottomFastScrollIndicator = activity.findViewById(R.id.scroll_bottom_indicator);

		View upFastScrollIndicator = activity.findViewById(R.id.scroll_top_indicator);

		if (bottomFastScrollIndicator.getVisibility() == View.VISIBLE)
		{
			if (myListView.getLastVisiblePosition() >= messages.size() - HikeConstants.MAX_FAST_SCROLL_VISIBLE_POSITION)
			{
				hideView(R.id.scroll_bottom_indicator);
			}

			else if (isScrollStateIdle(scrollState))
			{
				uiHandler.sendEmptyMessageDelayed(HIDE_DOWN_FAST_SCROLL_INDICATOR, 2000);
			}
		}

		if (upFastScrollIndicator.getVisibility() == View.VISIBLE)
		{

			if (myListView.getLastVisiblePosition() <= HikeConstants.MAX_FAST_SCROLL_VISIBLE_POSITION)
			{
				hideView(R.id.scroll_top_indicator);
			}

			else if (isScrollStateIdle(scrollState))
			{
				uiHandler.sendEmptyMessageDelayed(HIDE_UP_FAST_SCROLL_INDICATOR, 2000);
			}
		}

		mAdapter.setIsListFlinging(!(isScrollStateIdle(scrollState)));

	}

	private boolean isScrollStateIdle(int scrollState)
	{
		return scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
	}

	/**
	 * This method is called when a one to one or group chat thread is instantiated
	 */
	public void loadData()
	{
		fetchConversation(true);
	}

	private void addtoMessageMap(int from, int to)
	{
		for (int i = to - 1; i >= from; i--)
		{
			ConvMessage message = messages.get(i);
			ConvMessage msg = ChatThreadUtils.checkNUpdateFTMsg(activity.getApplicationContext(), message);
			if (msg != null)
			{
				message = msg;
				messages.set(i, message);
			}
			addtoMessageMap(message);
		}
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
			if (mMessageMap == null)
			{
				mMessageMap = new HashMap<Long, ConvMessage>();
			}

			if (msg.isFileTransferMessage())
			{
				if (TextUtils.isEmpty(msg.getMetadata().getHikeFiles().get(0).getFileKey()))
				{
					mMessageMap.put(msg.getMsgID(), msg);
					return;
				}
			}
			if (msg.isSMS())
			{
				if (msgState == State.SENT_UNCONFIRMED || msgState == State.SENT_FAILED)
				{
					mMessageMap.put(msg.getMsgID(), msg);
				}
			}
			else
			{
				if (msgState != State.SENT_DELIVERED_READ)
				{
					mMessageMap.put(msg.getMsgID(), msg);
				}
			}
		}
	}

	protected void removeFromMessageMap(ConvMessage msg)
	{
		if (mMessageMap == null)
			return;

		if (msg.isFileTransferMessage())
		{
			if (!TextUtils.isEmpty(msg.getMetadata().getHikeFiles().get(0).getFileKey()))
			{
				mMessageMap.remove(msg.getMsgID());
			}
		}
		else
		{
			mMessageMap.remove(msg.getMsgID());
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		Logger.d(TAG, "Inside onEventReceived of pubSub : " + type);

		/**
		 * Using switch on String, as it is present in JDK 7 onwards. Switch makes for a cleaner and easier to read code as well.
		 * http://stackoverflow.com/questions/338206/why-cant-i-switch-on-a-string
		 */
		switch (type)
		{
		case HikePubSub.MESSAGE_RECEIVED:
			onMessageReceived(object);
			break;
		case HikePubSub.END_TYPING_CONVERSATION:
			onEndTypingNotificationReceived(object);
			break;
		case HikePubSub.TYPING_CONVERSATION:
			onTypingConversationNotificationReceived(object);
			break;
		case HikePubSub.MESSAGE_DELIVERED:
			onMessageDelivered(object);
			break;
		case HikePubSub.SERVER_RECEIVED_MSG:
			long msgId = ((Long) object).longValue();
			setStateAndUpdateView(msgId, true);
			break;
		case HikePubSub.SERVER_RECEIVED_MULTI_MSG:
			onServerReceivedMultiMessage(object);
			break;
		case HikePubSub.ICON_CHANGED:
			onIconChanged(object);
			break;
		case HikePubSub.UPLOAD_FINISHED:
			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			break;
		case HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED:
			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			break;
		case HikePubSub.FILE_MESSAGE_CREATED:
			onFileMessageCreated(object);
			break;
		case HikePubSub.DELETE_MESSAGE:
			onDeleteMessage(object);
			break;
		case HikePubSub.STICKER_DOWNLOADED:
			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			break;
		case HikePubSub.MESSAGE_FAILED:
			onMessageFailed(object);
			break;
		case HikePubSub.CHAT_BACKGROUND_CHANGED:
			onChatBackgroundChanged(object);
			break;
		case HikePubSub.CLOSE_CURRENT_STEALTH_CHAT:
			/**
			 * Closing only if the current chat thread is stealth
			 */
			if (mConversation != null && mConversation.isStealth())
			{
				uiHandler.sendEmptyMessage(CLOSE_CURRENT_STEALTH_CHAT);
			}
			break;
		case HikePubSub.ClOSE_PHOTO_VIEWER_FRAGMENT:
			uiHandler.sendEmptyMessage(CLOSE_PHOTO_VIEWER_FRAGMENT);
			break;
		case HikePubSub.BLOCK_USER:
			blockUser(object, true);
			break;
		case HikePubSub.UNBLOCK_USER:
			blockUser(object, false);
			break;
		case HikePubSub.UPDATE_NETWORK_STATE:
			uiHandler.sendEmptyMessage(UPDATE_NETWORK_STATE);
			break;
		case HikePubSub.BULK_MESSAGE_DELIVERED_READ:
			onBulkMessageDeliveredRead(object);
			break;
		case HikePubSub.STICKER_CATEGORY_MAP_UPDATED:
			uiHandler.sendEmptyMessage(STICKER_CATEGORY_MAP_UPDATED);
			break;
		case HikePubSub.STICKER_FTUE_TIP:
			uiHandler.sendEmptyMessage(STICKER_FTUE_TIP);
			break;
        case HikePubSub.MULTI_MESSAGE_DB_INSERTED:
            onMultiMessageDbInserted(object);
            break;
        case HikePubSub.SHARED_WHATSAPP:		
          	 uiHandler.sendEmptyMessage(SHARING_FUNCTIONALITY);		
        	 break;	   
        case HikePubSub.MUTE_CONVERSATION_TOGGLED:
			onMuteConversationToggled(object);
			break;
        case HikePubSub.STICKER_RECOMMEND_PREFERENCE_CHANGED:
        	onStickerRecommendPreferenceChanged();
        case HikePubSub.STEALTH_CONVERSATION_MARKED:
        case HikePubSub.STEALTH_CONVERSATION_UNMARKED:
        	onConversationStealthToggle(object,HikePubSub.STEALTH_CONVERSATION_MARKED.equals(type));
			break;
        case HikePubSub.ENTER_TO_SEND_SETTINGS_CHANGED:
        	onEnterToSendSettingsChanged();
        	break;
        case HikePubSub.NUDGE_SETTINGS_CHANGED:
        	onNudgeSettingsChnaged();
        	break;
		default:
			Logger.e(TAG, "PubSub Registered But Not used : " + type);
			break;
		}
	}
	
	private void onNudgeSettingsChnaged()
	{
		_doubleTapPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean(HikeConstants.DOUBLE_TAP_PREF, true);
	}
	
	private void onEnterToSendSettingsChanged()
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				defineEnterAction();
			}
		});
	}
	
	private void onStickerRecommendPreferenceChanged()
	{
		activity.runOnUiThread(new Runnable()
		{
			
			@Override
			public void run()
			{
				if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.STICKER_RECOMMEND_PREF, true))
				{
					setupStickerSearch();
				}
				else
				{
					dismissStickerRecommendationPopup();
					releaseStickerSearchResources();
				}
			}
		});
	}
	
	private void onConversationStealthToggle(Object object, boolean markStealth)
	{
		if(!mConversation.getMsisdn().equals((String)object))
		{
			return;
		}
		mConversation.setIsStealth(markStealth);
		uiHandler.sendEmptyMessage(UPDATE_STEALTH_BADGE);
	}
	
	private void onMuteConversationToggled(Object object)
	{
		Pair<String, Boolean> mutePair = (Pair<String, Boolean>) object;

		/**
		 * Proceeding only if we caught an event for this groupchat/botchat thread
		 */

		if (mutePair.first.equals(msisdn))
		{
			mConversation.setIsMute(mutePair.second);
		}

		sendUIMessage(MUTE_CONVERSATION_TOGGLED, mutePair.second);
	}

	private void onMultiMessageDbInserted(Object object)
	{
		List<Pair<ContactInfo, ConvMessage>> pairList = (List<Pair<ContactInfo, ConvMessage>>) object;
		for (final Pair<ContactInfo, ConvMessage> pair : pairList)
		{
			ContactInfo conInfo = pair.first;
			String newMsisdn = conInfo.getMsisdn();

			if (msisdn.equals(newMsisdn))
			{

				if (isActivityVisible && SoundUtils.isTickSoundEnabled(activity.getApplicationContext()))
				{
					SoundUtils.playSoundFromRaw(activity.getApplicationContext(), R.raw.message_sent, AudioManager.STREAM_RING);
				}

				sendUIMessage(MULTI_MSG_DB_INSERTED, pair.second);

				break;
			}
		}
	}

    /**
	 * Handles message received events in chatThread
	 * 
	 * @param object
	 */
	protected void onMessageReceived(Object object)
	{
		ConvMessage message = (ConvMessage) object;
		String senderMsisdn = message.getMsisdn();
		if (senderMsisdn == null)
		{
			Logger.wtf(TAG, "Message with missing msisdn:" + message.toString());
			return;
		}
		
		if (msisdn.equals(senderMsisdn))
		{
			if (isActivityVisible)
			{
				ChatThreadUtils.publishReadByForMessage(message, mConversationDb, msisdn);
				
				if(message.getPrivateData() != null && message.getPrivateData().getTrackID() != null)
				{
					//Logs for Msg Reliability
					MsgRelLogManager.logMsgRelEvent(message, MsgRelEventType.RECEIVER_OPENS_CONV_SCREEN);
				}
			}

			if (message.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
			{
				/**
				 * ParticipantInfoState == NO_INFO indicates a normal message.
				 */
				handleSystemMessages();
			}

			if (isActivityVisible && SoundUtils.isTickSoundEnabled(activity.getApplicationContext()))
			{
				SoundUtils.playSoundFromRaw(activity.getApplicationContext(), R.raw.received_message, AudioManager.STREAM_RING);
			}
			
			sendUIMessage(MESSAGE_RECEIVED, message);

		}
	}

	protected boolean onMessageDelivered(Object object)
	{

		Pair<String, Long> pair = (Pair<String, Long>) object;
		// If the msisdn don't match we simply return
		if (!mConversation.getMsisdn().equals(pair.first))
		{
			return false;
		}
		long msgID = pair.second;
		// TODO we could keep a map of msgId -> conversation objects
		// somewhere to make this faster
		ConvMessage msg = findMessageById(msgID);
		if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED.ordinal()))
		{
			msg.setState(ConvMessage.State.SENT_DELIVERED);

			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			return true;
		}
		return false;
	}

	protected ConvMessage findMessageById(long msgID)
	{
		if (mMessageMap == null)
			return null;

		return mMessageMap.get(msgID);
	}

	protected void handleSystemMessages()
	{
		// TODO DO NOTHING. Only classes which need to handle such type of messages need to override this method
		return;
	}

	protected void sendUIMessage(int what, Object data)
	{
		Message message = Message.obtain();
		message.what = what;
		message.obj = data;
		uiHandler.sendMessage(message);
	}

	protected void sendUIMessage(int what, long delayTime, Object data)
	{
		Message message = Message.obtain();
		message.what = what;
		message.obj = data;
		uiHandler.sendMessageDelayed(message, delayTime);
	}

	/**
	 * Utility method for adding listeners for pubSub
	 * 
	 * @param listeners
	 */
	protected void addToPubSub()
	{
		mPubSubListeners = getPubSubEvents();
		Logger.d(TAG, "adding pubsub, length = " + Integer.toString(mPubSubListeners.length));
		HikeMessengerApp.getPubSub().addListeners(this, mPubSubListeners);
	}

	/**
	 * Returns pubSubListeners for ChatThread
	 * 
	 */

	private String[] getPubSubEvents()
	{
		String[] retVal;
		/**
		 * Array of pubSub listeners common to both {@link OneToOneChatThread} and {@link GroupChatThread}
		 */
		String[] commonEvents = new String[] { HikePubSub.MESSAGE_RECEIVED, HikePubSub.END_TYPING_CONVERSATION, HikePubSub.TYPING_CONVERSATION, HikePubSub.MESSAGE_DELIVERED,
				HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.SERVER_RECEIVED_MSG, HikePubSub.SERVER_RECEIVED_MULTI_MSG, HikePubSub.ICON_CHANGED, HikePubSub.UPLOAD_FINISHED,
				HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, HikePubSub.FILE_MESSAGE_CREATED, HikePubSub.DELETE_MESSAGE, HikePubSub.STICKER_DOWNLOADED, HikePubSub.MESSAGE_FAILED,
				HikePubSub.CHAT_BACKGROUND_CHANGED, HikePubSub.CLOSE_CURRENT_STEALTH_CHAT, HikePubSub.ClOSE_PHOTO_VIEWER_FRAGMENT, HikePubSub.STICKER_CATEGORY_MAP_UPDATED,
				HikePubSub.UPDATE_NETWORK_STATE, HikePubSub.BULK_MESSAGE_RECEIVED, HikePubSub.MULTI_MESSAGE_DB_INSERTED, HikePubSub.BLOCK_USER, HikePubSub.UNBLOCK_USER, HikePubSub.MUTE_CONVERSATION_TOGGLED, HikePubSub.SHARED_WHATSAPP, 
				HikePubSub.STEALTH_CONVERSATION_MARKED, HikePubSub.STEALTH_CONVERSATION_UNMARKED, HikePubSub.BULK_MESSAGE_DELIVERED_READ, HikePubSub.STICKER_RECOMMEND_PREFERENCE_CHANGED, HikePubSub.ENTER_TO_SEND_SETTINGS_CHANGED, HikePubSub.NUDGE_SETTINGS_CHANGED};

		/**
		 * Array of pubSub listeners we get from {@link OneToOneChatThread} or {@link GroupChatThread}
		 * 
		 */
		String[] moreEvents = getPubSubListeners();

		if (moreEvents == null)
		{
			retVal = new String[commonEvents.length];

			System.arraycopy(commonEvents, 0, retVal, 0, commonEvents.length);
		}

		else
		{
			retVal = new String[commonEvents.length + moreEvents.length];

			System.arraycopy(commonEvents, 0, retVal, 0, commonEvents.length);

			System.arraycopy(moreEvents, 0, retVal, commonEvents.length, moreEvents.length);

		}

		return retVal;
	}

	protected abstract String[] getPubSubListeners();

	/**
	 * Mimics the onDestroy method of an Activity. It is used to release resources held by the ChatThread instance.
	 */

	public void onDestroy()
	{
		setStickerRecommendFtueTipSeen();
		
		hideActionMode();

		removePubSubListeners();

		removeBroadcastReceiver();

		releaseComposeViewWatcher();

		releaseMessageAdapterResources();

		StickerManager.getInstance().saveCustomCategories();
		
		releaseMessageMap();
		
		((CustomLinearLayout) activity.findViewById(R.id.chat_layout)).setOnSoftKeyboardListener(null);
		
		releaseActionBarResources();
		
		releaseShareablePopUpResources();
		
		releaseStickerResources();
		
		releaseEmoticonResources();
		
		releaseStickerSearchResources();
	}
	
	private void releaseShareablePopUpResources()
	{
		if(mShareablePopupLayout != null)
		{
			mShareablePopupLayout.releaseResources();
		}
		mShareablePopupLayout = null;
	}
	
	private void releaseStickerResources()
	{
		if(mStickerPicker != null)
		{
			mStickerPicker.releaseResources();
		}
		mStickerPicker = null;
	}
	
	private void releaseEmoticonResources()
	{
		if(mEmoticonPicker != null)
		{
			mEmoticonPicker.releaseReources();
		}
		mEmoticonPicker = null;
	}
	
	private void releaseStickerSearchResources()
	{
		if(stickerTagWatcher != null)
		{
			stickerTagWatcher.releaseResources();
			mComposeView.removeTextChangedListener(stickerTagWatcher);
			stickerTagWatcher = null;
		}
	}
	
	private void releaseActionBarResources()
	{
		if (mActionBar != null)
		{
			mActionBar.releseResources();
			mActionBar = null;
		}
	}

	private void dismissShareablePopup()
	{
		/**
		 * It is important that along with releasing resources for Stickers/Emoticons, we also close their windows to prevent any BadWindow Exceptions later on.
		 */
		if (mShareablePopupLayout.isShowing())
		{
			mShareablePopupLayout.dismiss();
		}
	}

	/**
	 * Mimics the onPause method of an Activity.
	 */

	public void onPause()
	{
		Utils.hideSoftKeyboard(activity, mComposeView);
		
		isActivityVisible = false;
		
		resumeImageLoaders(true);

		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
	}

	/**
	 * Mimics the onResume method of an Activity.
	 */

	public void onResume()
	{
		isActivityVisible = true;

		if (shouldShowKeyboard())
		{
			Utils.showSoftKeyboard(activity, mComposeView);
		}
		
		/**
		 * Mark any messages unread as read
		 */
		setMessagesRead();

		/**
		 * Pause any onGoing loaders for MessagesAdapter
		 */

		resumeImageLoaders(false);

		/**
		 * Clear any pending notifications
		 */

		if (mConversation != null)
		{
			NotificationManager mgr = (NotificationManager) (activity.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE));
			mgr.cancel((int) mConversation.getMsisdn().hashCode());
		}

		/**
		 * Publish new Activity pubSub.
		 */

		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, activity);

		/**
		 * re - init the ComposeView watcher
		 */

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.init();
			mComposeViewWatcher.setBtnEnabled();
			mComposeView.requestFocus();
		}
	}

	public void onRestart()
	{
		isActivityVisible = true;

		Logger.d(TAG, "ChatThread : onRestart called");
	}

	/**
	 * We save drafts here
	 */
	protected void onStop()
	{
		dismissShareablePopup();
		dismissStickerRecommendationPopup();
		saveDraft();
	}
	
	/**
	 * This method is to be called before onNewIntent to cater to the following : 
	 * 1. Save drafts for the current chat thread if any. 
	 * 2. Dismiss stickers and emoticon pallete 
	 * 3. If actionMode is on, dismiss it
	 * 4. If photoViewer fragment was attached, remove it
	 * 5. If overflow menu is open then close it
	 * 6. Hide dialog if showing
	 * 7. Hide Walkie Talkie if showing
	 */
	protected void onPreNewIntent()
	{
		Logger.d(TAG, "Calling ChatThread's onPreNewIntent()");
		
		hideShareablePopups();
		
		hideActionMode();
		
		hideFragment(HikeConstants.IMAGE_FRAGMENT_TAG);
		
		hideThemePicker();
		
		hideOverflowMenu();
		
		hideDialog();
		
		hideWalkieTalkie();
		
		deactivateMessageSearch();
		
		saveDraft();
	}

	private void deactivateMessageSearch()
	{
		if (messageSearchManager != null)
		{
			messageSearchManager.deactivate();
		}
		if (searchDialog != null)
		{
			searchDialog.dismiss();
		}
	}
	
	private void hideWalkieTalkie()
	{
		if (audioRecordView != null)
		{
			audioRecordView.dismissAudioRecordView();
		}
	}


	private void hideDialog()
	{
		if (dialog != null)
		{
			dialog.dismiss();
			dialog = null;
		}
	}
	
	private void hideOverflowMenu()
	{
		if (mActionBar != null && mActionBar.isOverflowMenuShowing())
		{
			mActionBar.dismissOverflowMenu();
			mActionBar.resetView();
		}
	}
	
	private void hideThemePicker()
	{
		if (themePicker != null && themePicker.isShowing())
		{
			themePicker.dismiss();
		}
		
		if (attachmentPicker != null && attachmentPicker.isShowing())
		{
			attachmentPicker.dismiss();
		}
	}
	
	private void hideFragment(String tag)
	{
		if (activity.isFragmentAdded(tag))
		{
			activity.removeFragment(tag);
		}
	}
	
	private void hideActionMode()
	{
		if(mActionMode!= null && mActionMode.isActionModeOn())
		{
			mActionMode.finish();
		}
	}
	
	private void hideShareablePopups()
	{
		if (mShareablePopupLayout.isShowing())
		{
			mShareablePopupLayout.dismiss();
		}
	}

	protected void onStart()
	{
		/**
		 * We call this method to either re-init the stickerpicker and emoticon picker or update their listeners
		 */
		initShareablePopup();
		StickerManager.getInstance().showStickerRecommendTurnOnToast();
	}

	protected void hideView(int viewId)
	{
		activity.findViewById(viewId).setVisibility(View.GONE);
	}

	protected void showView(int viewId)
	{
		activity.findViewById(viewId).setVisibility(View.VISIBLE);
	}

	private void unreadCounterClicked()
	{
		mConversationsView.setSelection(messages.size() - unreadMessageCount - 1);
		hideUnreadCountIndicator();
	}

	private void hideUnreadCountIndicator()
	{
		unreadMessageCount = 0;
		hideView(R.id.new_message_indicator);
	}

	private void bottomScrollIndicatorClicked()
	{
		mConversationsView.setSelection(messages.size() - 1);
		hideView(R.id.scroll_bottom_indicator);
	}

	private void incrementUnreadMessageCount(int count)
	{
		unreadMessageCount += count;
	}

	/**
	 * Used to show the unreadCount indicator
	 */
	private void showUnreadCountIndicator()
	{
		incrementUnreadMessageCount(1);
		handleUnreadUI();
	}

	private void showUnreadCountIndicator(int unreadCount)
	{
		incrementUnreadMessageCount(unreadCount);
		handleUnreadUI();
	}

	private void handleUnreadUI()
	{
		/**
		 * fast scroll indicator and unread message should not show simultaneously
		 */
		hideView(R.id.scroll_bottom_indicator);
		showView(R.id.new_message_indicator);

		TextView indicatorText = (TextView) activity.findViewById(R.id.indicator_text);
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

	private void onEndTypingNotificationReceived(Object object)
	{
		TypingNotification typingNotification = (TypingNotification) object;
		if (typingNotification == null)
		{
			return;
		}

		if (msisdn.equals(typingNotification.getId()))
		{
			sendUIMessage(END_TYPING_CONVERSATION, typingNotification);
		}
	}

	/**
	 * This is used to add Typing Conversation on the UI
	 * 
	 * @param object
	 */
	protected void onTypingConversationNotificationReceived(Object object)
	{
		TypingNotification typingNotification = (TypingNotification) object;
		if (typingNotification == null)
		{
			return;
		}

		if (msisdn.equals(typingNotification.getId()))
		{
			sendUIMessage(TYPING_CONVERSATION, typingNotification);
		}
	}

	/**
	 * Adds typing notification on the UI
	 * 
	 * @param direction
	 * @param typingNotification
	 */
	protected void setTypingText(boolean direction, TypingNotification typingNotification)
	{
		if (messages.isEmpty() || messages.get(messages.size() - 1).getTypingNotification() == null)
		{
			addMessage(new ConvMessage(typingNotification));
		}
		else if (messages.get(messages.size() - 1).getTypingNotification() != null)
		{
			ConvMessage convMessage = messages.get(messages.size() - 1);
			convMessage.setTypingNotification(typingNotification);
			mAdapter.notifyDataSetChanged();
		}
	}

	protected boolean setStateAndUpdateView(long msgId, boolean updateView)
	{
		/*
		 * This would happen in the case if the events calling this method are called before the conversation is setup.
		 */
		if (mConversation == null || mAdapter == null)
		{
			return false;
		}
		ConvMessage msg = findMessageById(msgId);

		/*
		 * This is a hackish check. For some cases we were getting convMsg in another user's messageMap. which should not happen ideally. that was leading to showing hikeOfflineTip
		 * in wrong ChatThread.
		 */
		if (msg == null || TextUtils.isEmpty(msg.getMsisdn()) || !msg.getMsisdn().equals(msisdn))
		{
			Logger.i(TAG, "We are getting a wrong msisdn convMessage object in " + msisdn + " ChatThread");
			return false;
		}

		if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_CONFIRMED.ordinal()))
		{
			if (isActivityVisible && (!msg.isTickSoundPlayed()) && SoundUtils.isTickSoundEnabled(activity.getApplicationContext()))
			{
				SoundUtils.playSoundFromRaw(activity.getApplicationContext(), R.raw.message_sent, AudioManager.STREAM_RING);
			}
			msg.setTickSoundPlayed(true);
			msg.setState(ConvMessage.State.SENT_CONFIRMED);

			if (updateView)
			{
				MsgRelLogManager.logMsgRelEvent(msg, MsgRelEventType.SINGLE_TICK_ON_SENDER);
				
				uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			}
		}
		return true;
	}

	/**
	 * This indicates the clock to tick for a multi forward message
	 * 
	 * @param object
	 */
	private void onServerReceivedMultiMessage(Object object)
	{
		Pair<Long, Integer> p = (Pair<Long, Integer>) object;
		long baseId = p.first;
		int count = p.second;

		for (long msgId = baseId; msgId < (baseId + count); msgId++)
		{
//			View has to be updated only in case of Broadcast Conversation and not for corresponding 1-1 chats. This check is for optimization.
//			baseId = msgId corresponding to BroadcastConversation
//			rest of msgIds are for corresponding 1-1 Conversations
			if (msgId == baseId)
			{
				setStateAndUpdateView(msgId, true);
			}
			else
			{
				setStateAndUpdateView(msgId, false);
			}
		}

		uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
	}

	/**
	 * This is called when a group icon changes or a contact's dp is changed
	 * 
	 * @param object
	 */
	private void onIconChanged(Object object)
	{
		String mContactNumber = (String) object;
		if (mContactNumber.equals(msisdn))
		{
			uiHandler.sendEmptyMessage(UPDATE_AVATAR);
		}
	}

	/**
	 * This method is used to setAvatar for a contact.
	 */
	protected void setAvatar()
	{
		ImageView avatar = (ImageView) mActionBarView.findViewById(R.id.avatar);
		if (avatar == null)
		{
			return;
		}

		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn);
		if (drawable == null)
		{
			drawable = HikeMessengerApp.getLruCache().getDefaultAvatar(msisdn, false);
		}

		setAvatarStealthBadge();
		avatar.setScaleType(ScaleType.FIT_CENTER);
		avatar.setImageDrawable(drawable);
	}
	
	protected void setAvatarStealthBadge()
	{
		ImageView hiddenBadge = (ImageView) mActionBarView.findViewById(R.id.stealth_badge);
		if(hiddenBadge == null)
		{
			return;
		}
		if(mConversation.isStealth())
		{
			hiddenBadge.setVisibility(View.VISIBLE);
		}
		else
		{
			hiddenBadge.setVisibility(View.GONE);
		}
	}

	/**
	 * Called from PubSub thread, when a file upload is initiated, to add the convMessage to ChatThread
	 * 
	 * @param object
	 */
	private void onFileMessageCreated(Object object)
	{
		ConvMessage convMessage = (ConvMessage) object;

		/**
		 * Ensuring that the convMessage object belongs to the conversation
		 */

		if (!(convMessage.getMsisdn().equals(msisdn)))
		{
			return;
		}

		sendUIMessage(FILE_MESSAGE_CREATED, convMessage);
	}

	/**
	 * Called form PubSub thread, when a message is deleted.
	 * 
	 * @param object
	 */
	private void onDeleteMessage(Object object)
	{
		Pair<ArrayList<Long>, Bundle> deleteMessage = (Pair<ArrayList<Long>, Bundle>) object;
		ArrayList<Long> msgIds = deleteMessage.first;
		Bundle bundle = deleteMessage.second;
		String msgMsisdn = bundle.getString(HikeConstants.Extras.MSISDN);

		/**
		 * Received a delete message pubsub for a different thread, we received a false event with no msgIds
		 */
		if (!(msgMsisdn.equals(msisdn)) || msgIds.isEmpty())
		{
			return;
		}

		boolean deleteMediaFromPhone = bundle.getBoolean(HikeConstants.Extras.DELETE_MEDIA_FROM_PHONE);

		sendUIMessage(DELETE_MESSAGE, new Pair<Boolean, ArrayList<Long>>(deleteMediaFromPhone, msgIds));
	}

	/**
	 * Deletes the messages based on the message Ids present in the {@link ArrayList<Long>} in {@link Pair.second}
	 * 
	 * Called from the UI thread
	 * 
	 * @param pair
	 */
	private void deleteMessages(Pair<Boolean, ArrayList<Long>> pair)
	{
		/*
		 * This is done to avoid the case where unable to delete a message when unread count is displayed along with the message. 
		 * Because the same msgId is used for both unread count and the message.
		 * So deleting both.
		 */
		ArrayList<ConvMessage> deleteMsgs = new ArrayList<>();
		for (long msgId : pair.second)
		{
			for (ConvMessage convMessage : messages)
			{
				if (convMessage != null)
				{
					if (convMessage.getMsgID() == msgId)
					{
						deleteMsgs.add(convMessage);
					}
					else if (convMessage.getMsgID() > msgId)
					{
						break;
					}
				}
			}
		}
		for (Iterator<ConvMessage> iterator = deleteMsgs.iterator(); iterator.hasNext();) {
			ConvMessage convMessage = (ConvMessage) iterator.next();
			deleteMessage(convMessage, pair.first);
		}
		mAdapter.notifyDataSetChanged();
	}

	/**
	 * This function accomplishes the following : 1. Removes message from ChatThread and Db. 2. Removes message from {@code MessagesAdapter.undeliveredMessages} set of
	 * {@link MessagesAdapter} 3. If there is an ongoing FileTransfer, we cancel it.
	 * 
	 * @param convMessage
	 * @param deleteMediaFromPhone
	 */
	protected void deleteMessage(ConvMessage convMessage, boolean deleteMediaFromPhone)
	{
		mAdapter.removeMessage(convMessage);
		if (!convMessage.isSMS() && convMessage.getState() == State.SENT_CONFIRMED)
		{
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
			if (deleteMediaFromPhone && hikeFile != null)
			{
				hikeFile.delete(activity.getApplicationContext());
			}
            HikeConversationsDatabase.getInstance().reduceRefCount(key);
			FileTransferManager.getInstance(activity.getApplicationContext()).cancelTask(convMessage.getMsgID(), file, convMessage.isSent(), hikeFile.getFileSize());
			mAdapter.notifyDataSetChanged();

		}

		if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.CONTENT)
		{
			int numberOfMediaComponents = convMessage.platformMessageMetadata.mediaComponents.size();
			for (int i = 0; i < numberOfMediaComponents; i++)
			{
				CardComponent.MediaComponent mediaComponent = convMessage.platformMessageMetadata.mediaComponents.get(i);
				HikeConversationsDatabase.getInstance().reduceRefCount(mediaComponent.getKey());
			}
		}

		if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT || convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT)
		{
			String origin = Utils.conversationType(msisdn);
			JSONObject json = new JSONObject();
			try
			{
				json.put(HikePlatformConstants.CARD_TYPE, convMessage.webMetadata.getAppName());
				json.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.CARD_DELETE);
				json.put(AnalyticsConstants.ORIGIN, origin);
				json.put(AnalyticsConstants.CHAT_MSISDN, msisdn);
				HikeAnalyticsEvent.analyticsForPlatform(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, json);
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

	private void onMessageFailed(Object object)
	{
		long msgId = ((Long) object).longValue();
		ConvMessage convMessage = findMessageById(msgId);
		if (convMessage != null)
		{
			convMessage.setState(ConvMessage.State.SENT_FAILED);
			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
		}
	}

	/**
	 * Used to change the chat theme
	 * 
	 * @param object
	 */
	private void onChatBackgroundChanged(Object object)
	{
		Pair<String, ChatTheme> pair = (Pair<String, ChatTheme>) object;

		/**
		 * Proceeding only if the chat theme is changed for the current msisdn
		 */
		if (mConversation.getMsisdn().equals(pair.first))
		{
			sendUIMessage(CHAT_THEME, pair.second);
		}
	}

	/**
	 * Used to close the stealth chat
	 */
	private void closeStealthChat()
	{
		saveDraft();
		activity.finish();
	}

	/**
	 * If the user had typed something, we save it as a draft and will show it in the edit text box when he/she comes back to this conversation.
	 */
	private void saveDraft()
	{
		if (mComposeView != null && mComposeView.getVisibility() == View.VISIBLE && mComposeView.getId() == R.id.msg_compose)
		{
			Editor editor = activity.getSharedPreferences(HikeConstants.DRAFT_SETTING, android.content.Context.MODE_PRIVATE).edit();
			if (mComposeView.length() != 0)
			{
				editor.putString(msisdn, mComposeView.getText().toString());
			}
			else
			{
				editor.remove(msisdn);
			}
			editor.commit();
		}
	}

	private boolean removeFragment(String tag, boolean updateActionBar)
	{
		boolean isRemoved = activity.removeFragment(tag);
		if (isRemoved && updateActionBar)
		{
			setupActionBar(false);
			activity.updateActionBarColor(currentTheme.headerBgResId());
			activity.getSupportActionBar().show();
		}
		return isRemoved;
	}

	protected void setupActionBar(boolean firstInflation)
	{
		if (mCurrentActionMode ==  SEARCH_ACTION_MODE)
		{
			setupSearchMode(searchText);
		}
		else
		{
			setupDefaultActionBar(firstInflation);
		}
		mCurrentActionMode = -1;
	}

	/**
	 * Utility method used for setting up the ActionBar in the ChatThread.
	 * 
	 */
	protected void setupDefaultActionBar(boolean firstInflation)
	{
		mActionBarView = mActionBar.setCustomActionBarView(R.layout.chat_thread_action_bar);

		View backContainer = mActionBarView.findViewById(R.id.back);

		View contactInfoContainer = mActionBarView.findViewById(R.id.contact_info);

		/**
		 * Adding click listeners
		 */

		contactInfoContainer.setOnClickListener(this);
		backContainer.setOnClickListener(this);
		
		setAvatar();
	}

	/**
	 * Sets the label for the action bar
	 */
	protected void setLabel(String label)
	{
		if (label != null)
		{
			TextView mLabelTextView = (TextView) mActionBarView.findViewById(R.id.contact_name);

			mLabelTextView.setText(label);
		}
	}

	/**
	 * This method is used to construct {@link ConvMessage} with a given sticker and send it.
	 * 
	 * @param sticker
	 * @param categoryIdIfUnkown
	 * @param source
	 */
	protected void sendSticker(Sticker sticker, String source)
	{
		ConvMessage convMessage = Utils.makeConvMessage(msisdn, StickerManager.STICKER_MESSAGE_TAG, mConversation.isOnHike());
		ChatThreadUtils.setStickerMetadata(convMessage, sticker.getCategoryId(), sticker.getStickerId(), source);
		sendMessage(convMessage);
		
	}

	protected void sendChatThemeMessage()
	{
		long timestamp = System.currentTimeMillis() / 1000;
		mConversationDb.setChatBackground(msisdn, currentTheme.bgId(), timestamp);
		ConvMessage convMessage = ChatThreadUtils.getChatThemeConvMessage(activity.getApplicationContext(), timestamp, currentTheme.bgId(), mConversation);
		sendMessage(convMessage);
	}
	
	
	/**
	 * Called from the UI Handler to change the chat theme
	 * 
	 * @param chatTheme
	 */
	private void changeChatTheme(ChatTheme chatTheme)
	{
		updateUIAsPerTheme(chatTheme);

		currentTheme = chatTheme;
	}

	/**
	 * open Profile from action bar. Calling the child class' respective functions here
	 */
	protected void openProfileScreen()
	{
		return;
	}

	protected ChatTheme getCurrentlTheme()
	{
		return mAdapter.getChatTheme();
	}

	/**
	 * Used to show clear conversation confirmation dialog
	 */
	protected void showClearConversationDialog()
	{
		this.dialog = HikeDialogFactory.showDialog(activity, HikeDialogFactory.CLEAR_CONVERSATION_DIALOG, this);
	}

	/**
	 * Used to clear a user's conversation
	 */
	protected void clearConversation()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_CONVERSATION, msisdn);
		messages.clear();

		if (mMessageMap != null)
		{
			mMessageMap.clear();
		}

		mAdapter.notifyDataSetChanged();
		Logger.d(TAG, "Clearing conversation");
	}

	/**
	 * Used to email chat
	 */
	protected void emailChat()
	{
		if (messages != null && messages.size() > 0)
		{
			EmailConversationsAsyncTask emailTask = new EmailConversationsAsyncTask(activity, null);
			Utils.executeConvAsyncTask(emailTask, mConversation.getConvInfo());
		}
		
		else
		{
			Toast.makeText(activity, R.string.empty_email, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Called on the UI thread, it is used to update the network error view
	 * 
	 * @param isNetworkError
	 */
	private void showNetworkError(boolean isNetworkError)
	{
		activity.findViewById(R.id.network_error_chat).setVisibility(isNetworkError ? View.VISIBLE : View.GONE);
	}

	/**
	 * This is called from the UI thread
	 */
	protected void updateNetworkState()
	{
		showNetworkError(ChatThreadUtils.checkNetworkError());
	}

	/**
	 * Mark unread messages. This is called from {@link #onResume()}
	 */
	private void setMessagesRead()
	{
		/**
		 * Proceeding only if we have an unread message to be marked
		 */
		if (isLastMessageReceivedAndUnread())
		{
			if (PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
			{
					setSMSReadInNative();
			}

			HikeMessengerApp.getPubSub().publish(HikePubSub.MSG_READ, msisdn);
			ChatThreadUtils.sendMR(msisdn);
		}

	}

	/**
	 * Returns true if and only if the last message was received but unread
	 * 
	 * @return
	 */
	private boolean isLastMessageReceivedAndUnread()
	{
		if (mAdapter == null || mConversation == null)
		{
			return false;
		}
		
		return ChatThreadUtils.isLastMessageReceivedAndUnread(messages);
	}

	/**
	 * Since SMS are primarily a use case for {@link OneToOneChatThread}, this method is implemented there only.
	 */
	protected void setSMSReadInNative()
	{
		return;
	}

	/**
	 * Releases composeView watcher coupled to the EditText
	 */
	private void releaseComposeViewWatcher()
	{
		if (mComposeViewWatcher != null)
		{
			/**
			 * If we didn't send an end typing notification earlier, well, now is the best time to do it.
			 */

			if (!mComposeViewWatcher.wasEndTypingSent())
			{
				mComposeViewWatcher.sendEndTyping();
			}

			mComposeViewWatcher.releaseResources();
			mComposeViewWatcher = null;
		}
	}

	private void releaseMessageAdapterResources()
	{
		if (mAdapter != null)
		{
			mAdapter.onDestroy();
			mAdapter.resetPlayerIfRunning();
		}
	}

	private void releaseMessageMap()
	{

		if (mMessageMap != null)
		{
			mMessageMap.clear();
			mMessageMap = null;
		}
	}
	
	private void resumeImageLoaders(boolean flag)
	{
		if (mAdapter != null)
		{
			// mAdapter.getStickerLoader().setExitTasksEarly(false);
			mAdapter.getIconImageLoader().setExitTasksEarly(flag);
			mAdapter.getHighQualityThumbLoader().setExitTasksEarly(flag);
			if (!flag)
			{
				mAdapter.notifyDataSetChanged();
			}
		}
		
		if (mStickerPicker != null)
		{
			mStickerPicker.setExitTasksEarly(flag);
		}
	}

	/**
	 * Used for removing the typing notification
	 * 
	 * @return
	 */
	protected TypingNotification removeTypingNotification()
	{
		TypingNotification typingNotification = null;

		if (!messages.isEmpty() && messages.get(messages.size() - 1).getTypingNotification() != null)
		{
			typingNotification = messages.get(messages.size() - 1).getTypingNotification();
			messages.remove(messages.size() - 1);
		}

		return typingNotification;
	}

	/**
	 * This function tries to scroll to the bottom for new messages.
	 * 
	 * Don't scroll to bottom if the user is at older messages. It's possible user might be reading them.
	 * 
	 */
	protected void tryScrollingToBottom(ConvMessage convMessage, int unreadCount)
	{
		if (((convMessage != null && !convMessage.isSent()) || convMessage == null) && mConversationsView.getLastVisiblePosition() < messages.size() - 4)
		{

			if (convMessage.getTypingNotification() == null
					&& (convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO || convMessage.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE))
			{
				if (unreadCount == 0)
				{
					showUnreadCountIndicator();
				}

				else
				{
					showUnreadCountIndicator(unreadCount);
				}
			}

		}

		else
		{
			/**
			 * Scrolling to bottom.
			 */
			mConversationsView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
			
			/*
			 * Resetting the transcript mode once the list has scrolled to the bottom.
			 */
			uiHandler.sendEmptyMessage(DISABLE_TRANSCRIPT_MODE);
		}

	}

	protected void onBulkMessageDeliveredRead(Object object)
	{
		/**
		 * Defensive check
		 */
		if (messages == null || messages.isEmpty())
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

			updateReadByInLoop(mrMsgId, pair.getFirst().getSecond());

			for (int i = messages.size() - 1; i >= 0; i--)
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
							
							//updating hike off-line messages set
							if(mConversation.isOnHike())
							{
								removeFromUndeliverdMessages(msg);
							}
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
							
							//updating hike off-line messages set
							if(mConversation.isOnHike())
							{
								removeFromUndeliverdMessages(msg);
							}
						}
					}
				}
			}

			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
		}
	}

	protected void removeFromUndeliverdMessages(ConvMessage msg)
	{
		return;
	}


	/**
	 * This method will be overriden by respective classes
	 * 
	 * @param mrMsgId
	 * @param second
	 */

	protected void updateReadByInLoop(long mrMsgId, Set<String> second)
	{
		return;
	}

	public String getContactNumber()
	{
		return msisdn;
	}

	// ------------------------ ACTIONMODE CALLBACKS -------------------------------
	/**
	 * These methods is also overriden by {@link OneToNChatThread} for pins
	 */
	@Override
	public void actionModeDestroyed(int id)
	{
		switch (id)
		{
		case MULTI_SELECT_ACTION_MODE:
			destroyActionMode();
			break;
		case SEARCH_ACTION_MODE:
			destroySearchMode();
			break;
		default:
			break;
		}
	}

	@Override
	public void doneClicked(int id)
	{
	}

	@Override
	public void initActionbarActionModeView(int id, View view)
	{
	}

	@Override
	public boolean onActionItemClicked(int actionModeId, MenuItem menuItem)
	{
		switch (actionModeId)
		{
		case MULTI_SELECT_ACTION_MODE:
			return onActionModeMenuItemClicked(menuItem);
		default:
			break;
		}
		return false;
	}

	// ------------------------ ACTIONMODE CALLBACKs ENDS -------------------------------

	private boolean onActionModeMenuItemClicked(MenuItem menuItem)
	{
		HashMap<Long, ConvMessage> selectedMessagesMap = mAdapter.getSelectedMessagesMap();
		ArrayList<Long> selectedMsgIds;
		switch (menuItem.getItemId())
		{
		case R.id.delete_msgs:
			ArrayList<Long> selectedMsgIdsToDelete = new ArrayList<Long>(mAdapter.getSelectedMessageIds());
			this.dialog = HikeDialogFactory.showDialog(activity, HikeDialogFactory.DELETE_MESSAGES_DIALOG, this, mAdapter.getSelectedCount(),
					mAdapter.containsMediaMessage(selectedMsgIdsToDelete));
			return true;

		case R.id.forward_msgs:
			selectedMsgIds = new ArrayList<Long>(mAdapter.getSelectedMessageIds());
			Collections.sort(selectedMsgIds);
			recordForwardEvent();
			
			Intent intent = IntentFactory.getComposeChatActivityIntent(activity);
			String msg;
			intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
			JSONArray multipleMsgArray = new JSONArray();
			try
			{
				for (int i = 0; i < selectedMsgIds.size(); i++)
				{
					ConvMessage message = selectedMessagesMap.get(selectedMsgIds.get(i));
					JSONObject multiMsgFwdObject = new JSONObject();
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
					}
					else if (message.getMetadata() != null && message.getMetadata().isPokeMessage())
					{
						multiMsgFwdObject.put(HikeConstants.Extras.POKE, true);
					}
					
					else if (message.getMessageType() == HikeConstants.MESSAGE_TYPE.CONTENT)
					{
						multiMsgFwdObject.put(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE, HikeConstants.MESSAGE_TYPE.CONTENT);
						if (message.platformMessageMetadata != null)
						{
							multiMsgFwdObject.put(HikeConstants.METADATA, message.platformMessageMetadata.JSONtoString());
							if (message.contentLove != null)
							{
								multiMsgFwdObject.put(HikeConstants.ConvMessagePacketKeys.LOVE_ID, message.contentLove.loveId);
							}
						}
					}
					
					else if (message.getMessageType() == HikeConstants.MESSAGE_TYPE.WEB_CONTENT || message.getMessageType() == HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT)
					{
						multiMsgFwdObject.put(HikeConstants.MESSAGE_TYPE.MESSAGE_TYPE, HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT);
						multiMsgFwdObject.put(HikeConstants.HIKE_MESSAGE, message.getMessage());
						if (message.webMetadata != null)
						{
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
				return true; // No need to further process since there is a JSON Exception.
			}
			intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());
			intent.putExtra(HikeConstants.Extras.PREV_MSISDN, msisdn);
			intent = IntentFactory.shareFunctionality(intent, selectedMessagesMap.get(selectedMsgIds.get(0)), mAdapter, shareableMessagesCount, activity.getApplicationContext());
			activity.startActivity(intent);
			return true;

		case R.id.copy_msgs:
			selectedMsgIds = new ArrayList<Long>(mAdapter.getSelectedMessageIds());
			Collections.sort(selectedMsgIds);
			StringBuilder msgStr = new StringBuilder();
			int size = selectedMsgIds.size();

			if (!selectedMsgIds.isEmpty())
			{
				msgStr.append(selectedMessagesMap.get(selectedMsgIds.get(0)).getMessage());
				for (int i = 1; i < size; i++)
				{
					msgStr.append("\n");
					msgStr.append(selectedMessagesMap.get(selectedMsgIds.get(i)).getMessage());
				}
			}
			Utils.setClipboardText(msgStr.toString(), activity.getApplicationContext());
			Toast.makeText(activity.getApplicationContext(), R.string.copied, Toast.LENGTH_SHORT).show();
			mActionMode.finish();
			return true;

		case R.id.action_mode_overflow_menu:

			/**
			 * for (ConvMessage convMessage : selectedMessagesMap.values()) { //showActionModeOverflow(convMessage); }
			 */
			// TO DO
			return true;

		case R.id.share_msgs:
			selectedMsgIds = new ArrayList<Long>(mAdapter.getSelectedMessageIds());
			if (selectedMsgIds.size() == 1)
			{
				ConvMessage message = selectedMessagesMap.get(selectedMsgIds.get(0));
				HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
				hikeFile.shareFile(activity);
				mActionMode.finish();
			}
			else
			{
				Toast.makeText(activity, R.string.some_error, Toast.LENGTH_SHORT).show();
			}
			return true;

		default:
			mActionMode.finish();
			return false;
		}
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}
	

	/**
	 * Used to handle clicks on the Overlay mode when ActionMode is enabled
	 * 
	 * @param convMessage
	 */
	public void onBlueOverLayClick(ConvMessage convMessage, View view)
	{
		if (mActionMode.whichActionModeIsOn() == MULTI_SELECT_ACTION_MODE)
		{
			showMessageContextMenu(convMessage, view);
		}
	}

	@Override
	public void onDismiss()
	{
		if (mActionMode == null || !mActionMode.isActionModeOn())
		{
			mTips.showHiddenTip();
		}
	}

	protected void onConfigurationChanged(Configuration newConfig)
	{
		Logger.d(TAG, "newConfig : " + newConfig.toString());
		
		if (mShareablePopupLayout != null && mShareablePopupLayout.isShowing())
		{
			mShareablePopupLayout.dismiss();
		}
		
		if (stickerTagWatcher != null)
		{
			stickerTagWatcher.dismissStickerSearchPopup();
		}
		
		/**
		 * Handle theme background image change.
		 */
		if (getCurrentlTheme() != null && getCurrentlTheme() != ChatTheme.DEFAULT)
		{
			setBackground(getCurrentlTheme());
		}
		
		/**
		 * Handling MULTI_SELECT_ACTION_MODE orientation change
		 */
		if (mActionMode != null)
		{
			uiHandler.sendEmptyMessage(ACTION_MODE_CONFIG_CHANGE);
		}
		
		if (themePicker != null && themePicker.isShowing())
		{
			themePicker.onOrientationChange(newConfig.orientation);
		}
		
		if (mActionBar != null && mActionBar.isOverflowMenuShowing())
		{
			if (mShareablePopupLayout.isKeyboardOpen())
			{
				mActionBar.dismissOverflowMenu();
			}
		}
		
		if (attachmentPicker != null && attachmentPicker.isShowing())
		{
			if (mShareablePopupLayout.isKeyboardOpen())
			{
				attachmentPicker.dismiss();
			}
		}
	}
	
	/**
	 * This method is used to handle orientation changes for ActionMode.
	 * If a lower class has a special actionMode not known to super class, it should override this method
	 * 
	 * @param whichActionMode
	 */
	protected void handleActionModeOrientationChange(int whichActionMode)
	{
		switch (whichActionMode)
		{
		case MULTI_SELECT_ACTION_MODE:
			mActionMode.reInflateActionMode();
			hideShowActionModeMenus();
			mActionMode.updateTitle(activity.getString(R.string.selected_count, mAdapter.getSelectedCount()));
			break;
			
		case SEARCH_ACTION_MODE:
			mActionMode.reInflateActionMode();
			String oldText = mComposeView.getText().toString();
			int start = mComposeView.getSelectionStart();
			int end = mComposeView.getSelectionEnd();
			setUpSearchViews();
			mComposeView.setText(oldText);
			if (start != end)
			{
				mComposeView.setSelection(start, end);
			}
			else
			{
				mComposeView.setSelection(start);
			}
			break;
			
		default:
			break;
		}
	}
	
	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent keyEvent)
	{
		if (mConversation == null)
		{
			return false;
		}

		if ((view == mComposeView))
		{
	     // On some phones (like: micromax A120) "actionId" always comes 0, so added one more optional check (view.getId() ==R.id.msg_compose) & (view.getId() ==R.id.search_text)
			if ((actionId == EditorInfo.IME_ACTION_SEND)
					|| ((view.getId() == R.id.msg_compose) && PreferenceManager
							.getDefaultSharedPreferences(
									activity.getApplicationContext())
							.getBoolean(HikeConstants.SEND_ENTER_PREF, false)))	{
				if (!TextUtils.isEmpty(mComposeView.getText())) {
					sendButtonClicked();
				}
				return true;
			}
			else if (actionId == EditorInfo.IME_ACTION_SEARCH||(view.getId() ==R.id.search_text))
			{
				Utils.hideSoftKeyboard(activity.getApplicationContext(), mComposeView);
				searchMessage(false,true);
				return true;
			}
			return false;
		}
		return false;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event)
	{
		if ((event.getAction() == KeyEvent.ACTION_UP) && (keyCode == KeyEvent.KEYCODE_ENTER) && event.isAltPressed())
		{
			mComposeView.append(NEW_LINE_DELIMETER);
			/**
			 * Micromax phones appear to fire this event twice. Doing this seems to fix the problem.
			 */
			KeyEvent.changeAction(event, KeyEvent.ACTION_DOWN);
			return true;
		}
		return false;
	}

	/**
	 * Called from {@link #onActionModeMenuItemClicked(MenuItem)}
	 */
	private void recordForwardEvent()
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.FORWARD_MSG);
			metadata.put(HikeConstants.MSISDN, msisdn);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}
	
	@Override
	public void onPopupDismiss()
	{
		Logger.i(TAG, "onPopup Dismiss");
		if(activity.findViewById(R.id.sticker_btn).isSelected())
		{
			setStickerButtonSelected(false);
		}
		
		if(activity.findViewById(R.id.emoticon_btn).isSelected())
		{
			setEmoticonButtonSelected(false);
		}
	}
	
	@Override
	public void onBackKeyPressedET(CustomFontEditText editText)
	{
		// on back press - if keyboard was open , now keyboard gone , try to hide emoticons
		// if keyboard ws not open , onbackpress of activity will get call back, dismiss popup there
		// if we dismiss here in second case as well, then onbackpress of acitivty will be called and it will finish activity
		
		if (mShareablePopupLayout.isKeyboardOpen() && mShareablePopupLayout.isShowing())
		{
			mShareablePopupLayout.dismiss();
		}
		mShareablePopupLayout.onBackPressed();
	}

	/**
	 * Close the keyboard. Since we are going to a different fragment Reset the application Flags
	 */
	public void onAttachFragment(Fragment fragment)
	{
		if(fragment.getTag() == HikeConstants.STICKER_RECOMMENDATION_FRAGMENT_TAG || fragment.getTag() == HikeConstants.STICKER_RECOMMENDATION_FRAGMENT_FTUE_TAG)
		{
			return;
		}
		
		Utils.hideSoftKeyboard(activity.getApplicationContext(), mComposeView);

		saveCurrentActionMode();
		if (mShareablePopupLayout != null)
		{
			mShareablePopupLayout.onCloseKeyBoard();

		}
		
		if (mActionMode != null && mActionMode.isActionModeOn())
		{
			mActionMode.finish();
		}
	}

	public void onMenuKeyPressed()
	{
		if (mActionBar != null)
		{
			showOverflowMenu();
		}
	}
	
	protected boolean wasTipSetSeen(int whichTip)
	{
		if (mTips.isGivenTipShowing(whichTip))
		{
			mTips.setTipSeen(whichTip);
			return true;
		}
		
		return false;
	}
	
	public boolean isKeyboardOpen()
	{
		if(mShareablePopupLayout == null || !mShareablePopupLayout.isKeyboardOpen())
		{
			return false;
		}
		return true;
	}
	
	@Override
	public void onShown()
	{
		/**
		 * If the last message was visible before opening the keyboard it can be hidden hence we need to scroll to bottom.
		 */
		if (mConversationsView != null && (mConversationsView.getLastVisiblePosition() == mConversationsView.getCount() - 1))
		{
			if (shouldKeyboardPopupShow)
			{
				scrollToPosition(mConversationsView.getLastVisiblePosition(), 0);
			}
			else
			{
				uiHandler.sendEmptyMessage(SCROLL_TO_END);
			}
		}
	}
	
	@Override
	public void onHidden()
	{
	}

	public void dismissResidualAcitonMode()
	{
		if (mActionMode != null && mActionMode.isActionModeOn())
		{
			mActionMode.finish();
		}
	}
	
	/**
	 * blockOverLay flag indicates whether this is used to block a user or not. This function can also be called from in zero SMS Credits case.
	 * 
	 * @param label
	 * @param formatString
	 * @param overlayBtnText
	 * @param str
	 * @param drawableResId
	 */

	protected void showOverlay(String label, String formatString, String overlayBtnText, SpannableString str, int drawableResId, int viewTag)
	{
		Utils.hideSoftKeyboard(activity.getApplicationContext(), mComposeView);

		View mOverlayLayout = activity.findViewById(R.id.overlay_layout);
		mOverlayLayout.setTag(viewTag);

		if (mOverlayLayout.getVisibility() != View.VISIBLE && activity.hasWindowFocus())
		{
			Animation fadeIn = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in);
			mOverlayLayout.setAnimation(fadeIn);
		}

		mComposeView.setEnabled(false);

		mOverlayLayout.setVisibility(View.VISIBLE);
		mOverlayLayout.setOnClickListener(this);

		TextView message = (TextView) mOverlayLayout.findViewById(R.id.overlay_message);
		Button overlayBtn = (Button) mOverlayLayout.findViewById(R.id.overlay_button);
		ImageView overlayImg = (ImageView) mOverlayLayout.findViewById(R.id.overlay_image);

		overlayBtn.setOnClickListener(this);
		overlayBtn.setTag(viewTag);

		mComposeView.setEnabled(false);

		overlayImg.setImageResource(R.drawable.ic_no);
		overlayBtn.setText(overlayBtnText);

		message.setText(str);
	}


	/**
	 * Used to call {@link #showOverlay(boolean, String, String, String)} from {@link OneToOneChatThread} or {@link OneToNChatThread}
	 * 
	 * @param label
	 */
	protected void showBlockOverlay(String label)
	{
		/**
		 * Making the blocked user's name as bold
		 */
		String formatString = activity.getString(R.string.block_overlay_message);
		String formatted = String.format(formatString, label);
		SpannableString str = new SpannableString(formatted);
		int start = formatString.indexOf("%1$s");
		str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, start + label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		showOverlay(label, formatString, activity.getString(R.string.unblock_title), str, R.drawable.ic_no, R.string.unblock_title);
	}

	private void onOverlayLayoutClicked(int tag)
	{
		switch (tag)
		{

		/**
		 * Block Case :
		 */
		case R.string.unblock_title:
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, getMsisdnMainUser());
			break;

		/**
		 * Zero SMS Credits :
		 */
		case R.string.invite_now:
			Utils.logEvent(activity.getApplicationContext(), HikeConstants.LogEvent.INVITE_OVERLAY_BUTTON);
			inviteUser();
			hideOverlay();
			break;
		}
	}

	/**
	 * Invite user
	 */
	private void inviteUser()
	{
		if (mConversation.isOnHike())
		{
			Toast.makeText(activity, R.string.already_hike_user, Toast.LENGTH_LONG).show();
		}

		else
		{
			Utils.sendInviteUtil(new ContactInfo(msisdn, msisdn, mConversation.getConversationName(), msisdn), activity.getApplicationContext(),
					HikeConstants.SINGLE_INVITE_SMS_ALERT_CHECKED, getString(R.string.native_header), getString(R.string.native_info));

		}
	}
	
	private void blockUser(Object object, boolean isBlocked)
	{
		String mMsisdn = (String) object;

		/**
		 * Proceeding only if the blocked user's msisdn is that of the current chat thread
		 */
		if (mMsisdn.equals(getMsisdnMainUser()))
		{
			sendUIMessage(BLOCK_UNBLOCK_USER, isBlocked);
		}
	}
	
	protected void hideOverlay()
	{
		View mOverlayLayout = activity.findViewById(R.id.overlay_layout);

		if (mOverlayLayout.getVisibility() == View.VISIBLE && activity.hasWindowFocus())
		{
			Animation fadeOut = AnimationUtils.loadAnimation(activity.getApplicationContext(), android.R.anim.fade_out);
			mOverlayLayout.setAnimation(fadeOut);
		}

		mOverlayLayout.setVisibility(View.INVISIBLE);
	}
	
	/**
	 * This method is overriden by {@link OneToOneChatThread} and {@link OneToNChatThread}
	 * 
	 * @return
	 */
	protected String getBlockedUserLabel()
	{
		return null;
	}

	/**
	 * This runs only on the UI Thread
	 * 
	 * @param isBlocked
	 */
	protected void blockUnBlockUser(boolean isBlocked)
	{
		mConversation.setBlocked(isBlocked);

		if (isBlocked)
		{
			Utils.logEvent(activity.getApplicationContext(), HikeConstants.LogEvent.MENU_BLOCK);
			showBlockOverlay(getBlockedUserLabel());
		}

		else
		{
			mComposeView.setEnabled(true);
			hideOverlay();
		}
	}
	
	/**
	 * Used for giving block and unblock user pubSubs
	 */
	protected void onBlockUserclicked()
	{
		if (mConversation.isBlocked())
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, msisdn);
		}

		else
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, msisdn);
		}
	}

	public void scrollToEnd()
	{
		uiHandler.sendEmptyMessage(SCROLL_TO_END);
	}
	
	private void scrollToPosition(int position, int offSet)
	{
		int LAST_FEW_MESSAGES = 4;
		// SetSelection doesnt work for last few items.
		// We need to enable TRANSCRIPT_MODE_ALWAYS_SCROLL in such cases.
		if (position >= (messages.size() - 1) - LAST_FEW_MESSAGES)
		{
			/**
			 * Scrolling to bottom.
			 */
			mConversationsView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
			mConversationsView.setSelectionFromTop(position, offSet);

			/*
			 * Resetting the transcript mode once the list has scrolled to the bottom.
			 */
			uiHandler.sendEmptyMessage(DISABLE_TRANSCRIPT_MODE);
		}
		else
		{
			mConversationsView.setSelectionFromTop(position,offSet);
		}
	}
	
	protected void startHomeActivity()
	{
		Intent intent = IntentFactory.getHomeActivityIntent(activity);
		activity.startActivity(intent);
	}
	

	protected void showThemePicker(int footerTextId)
	{
		/**
		 * Hiding soft keyboard
		 */
		Utils.hideSoftKeyboard(activity, mComposeView);
		setUpThemePicker();
		themePicker.showThemePicker(activity.findViewById(R.id.cb_anchor), currentTheme,footerTextId, activity.getResources().getConfiguration().orientation);
	}
	
	public void saveCurrentActionMode()
	{
		if (mActionMode != null && mActionMode.isActionModeOn())
		{
			mCurrentActionMode = mActionMode.whichActionModeIsOn();
		}
	}

	protected void onSaveInstanceState(Bundle outState)
	{	
		shouldKeyboardPopupShow=HikeMessengerApp.keyboardApproach(activity);
		outState.putBoolean(HikeConstants.CONSUMED_FORWARDED_DATA, consumedForwardedData);
	}
	
	protected void onRestoreInstanceState(Bundle savedInstanceState) 
	{
		consumedForwardedData = savedInstanceState.getBoolean(HikeConstants.CONSUMED_FORWARDED_DATA, false);
	}
	
	public void clearComposeText()
	{
		if(mComposeView != null)
		{
			mComposeView.setText("");
		}
	}
	
	public void selectAllComposeText()
	{
			
	}
}
