package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessageInfoAdapter;
import com.bsb.hike.chatthemes.ChatThemeManager;
import com.bsb.hike.chatthemes.CustomBGRecyclingImageView;
import com.bsb.hike.chatthemes.HikeChatThemeConstants;
import com.bsb.hike.chatthread.ChatThreadUtils;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.messageinfo.GroupChatDataModel;
import com.bsb.hike.messageinfo.MessageInfo;
import com.bsb.hike.messageinfo.MessageInfoDataModel;
import com.bsb.hike.messageinfo.MessageInfoDeliveredList;
import com.bsb.hike.messageinfo.MessageInfoItem;
import com.bsb.hike.messageinfo.MessageInfoList;
import com.bsb.hike.messageinfo.MessageInfoLoader;
import com.bsb.hike.messageinfo.MessageInfoLoaderData;
import com.bsb.hike.messageinfo.MessageInfoReadList;
import com.bsb.hike.messageinfo.MessageInfoView;
import com.bsb.hike.messageinfo.OnetoOneDataModel;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.HikeChatTheme;
import com.bsb.hike.offline.OfflineUtils;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.models.ConvMessage.State;

public class MessageInfoActivity extends HikeAppStateBaseFragmentActivity implements HikePubSub.Listener
{
	private MessageInfoAdapter messageInfoAdapter;

	private String msisdn;

	private long messageID;

	public ListView parentListView;

	private Context mContext;

	View headerView;

	Conversation conversation;

	private static final int NOTIFY_ADAPTER = 1;

	private ConvMessage convMessage;

	public List<MessageInfoList> listsToBedisplayed = new ArrayList<MessageInfoList>();
	public int messageviewheight,screenheightM;

	protected Handler uiHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			/**
			 * Defensive check
			 */
			if (msg == null)
			{
				Logger.e("MessageInfo", "Getting a null message in chat thread");
				return;
			}
			handleUIMessage(msg);
		}

	};

	public void handleUIMessage(Message msg)
	{
		if (msg.what == NOTIFY_ADAPTER)
		{

			messageInfoAdapter.addAll(messageMap);
			messageInfoAdapter.notifyDataSetChanged();

		}
	}

	private List<MessageInfoItem> messageInfoItemList = Collections.synchronizedList(new ArrayList<MessageInfoItem>());

	private LinkedHashSet<MessageInfoItem> messageMap = new LinkedHashSet<MessageInfoItem>();

	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED, HikePubSub.BULK_MESSAGE_RECEIVED };

	private boolean isGroupChat, isOnetoOne, isBroadCast,isSMSMessage,wasSMSMessage;

	private MessageInfoController controller;

	private HikeChatTheme chatTheme;

	private MessageInfoDataModel dataModel;

	protected void onCreate(Bundle savedInstanceState)
	{
		/*
		 * Making the action bar transparent for custom theming.
		 */
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		super.onCreate(savedInstanceState);
		msisdn = getIntent().getExtras().getString(HikeConstants.MSISDN);
		messageID = getIntent().getExtras().getLong(HikeConstants.MESSAGE_ID);
		convMessage = HikeConversationsDatabase.getInstance().getMessageFromID(messageID, msisdn);
		isSMSMessage=getIntent().getBooleanExtra(HikeConstants.SMS_MESSAGE, false);
		convMessage.setSMS(isSMSMessage);
		wasSMSMessage=isSMSMessage;

		initializeListViewandAdapters();
		setDataModelsandControllers();

		String chatThemeID = HikeConversationsDatabase.getInstance().getChatThemeIdForMsisdn(msisdn);
		chatTheme=ChatThemeManager.getInstance().getTheme(chatThemeID);
		ColorDrawable statusBarColor = (ColorDrawable) ChatThemeManager.getInstance().
		getDrawableForTheme(chatTheme.getThemeId(), HikeChatThemeConstants.ASSET_INDEX_STATUS_BAR_BG);
		statusBarColorValue=statusBarColor.getColor();
		setupActionBar();
		setChatTheme();
		Utils.blockOrientationChange(MessageInfoActivity.this);


	}

	private void setDataModelsandControllers()
	{
		int type = getIntent().getExtras().getInt(HikeConstants.MESSAGE_INFO.MESSAGE_INFO_TYPE, HikeConstants.MESSAGE_INFO.ONE_TO_ONE);

		if (type == HikeConstants.MESSAGE_INFO.GROUP || type == HikeConstants.MESSAGE_INFO.BROADCAST)
		{
			isGroupChat = true;
			if (dataModel == null)
				dataModel = new GroupChatDataModel(msisdn, messageID);
			if (controller == null)
				controller = new MessageInfoControllerGroup(dataModel);

		}
		else
		{
			isOnetoOne = true;
			if (dataModel == null)
				dataModel = new OnetoOneDataModel(msisdn, messageID);
			if (controller == null)
				controller = new MessageInfoControllerOnetoOne(dataModel);
		}
		controller.init();

	}

	public void initializeListViewandAdapters()
	{
		setContentView(R.layout.message_info_try);
		parentListView = (ListView) findViewById(R.id.profile_content);
		messageInfoAdapter = new MessageInfoAdapter(MessageInfoActivity.this, messageInfoItemList, convMessage);
		parentListView.setAdapter(messageInfoAdapter);
		headerView = null;
		mContext = HikeMessengerApp.getInstance().getApplicationContext();

	}

	private abstract class MessageInfoController implements HikePubSub.Listener, LoaderManager.LoaderCallbacks<MessageInfoLoaderData>,MessageInfoAdapter.MessageInfoViewListener
	{
		MessageInfoDataModel dataModel;

		public TreeMap<String, MessageInfoDataModel.MessageInfoParticipantData> participantTreeMap;

		String[] listeners = new String[] { HikePubSub.MESSAGE_DELIVERED, HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.MSG_READ, HikePubSub.ONETON_MESSAGE_DELIVERED_READ };

		MessageInfoController(MessageInfoDataModel dataModel)
		{
			this.dataModel = dataModel;
		}

		MessageInfoList readList, deliveredList, playedList;

		public abstract void refreshData();

		Conversation mConversation;

		HikeChatTheme chatTheme;
		MessageInfoView messageInfoView;

		View mView;

		protected int readListString;

		protected int controllerType;

		public boolean areAnyReceiptsReceived;

		void init()
		{
			String chatThemeID = HikeConversationsDatabase.getInstance().getChatThemeIdForMsisdn(msisdn);
			chatTheme= ChatThemeManager.getInstance().getTheme(chatThemeID);
			mConversation = HikeConversationsDatabase.getInstance().getConversation(msisdn, 1, true);
			messageInfoAdapter.setConversation(mConversation);
			messageInfoAdapter.setMsisdn(msisdn);
			// getSupportLoaderManager().initLoader(1,null,this).forceLoad();
			// dataModel.fetchAllParticipantsInfo();
			getSupportLoaderManager().initLoader(1, null, this).forceLoad();
			HikeMessengerApp.getPubSub().addListeners(this, listeners);
			messageInfoAdapter.setMessageInfoViewListener(this);

		}

		@Override
		public Loader<MessageInfoLoaderData> onCreateLoader(int id, Bundle args)
		{
			return new MessageInfoLoader(MessageInfoActivity.this, dataModel);
		}

		@Override
		public void onLoadFinished(Loader<MessageInfoLoaderData> loader, MessageInfoLoaderData data)
		{
			participantTreeMap = data.participantTreeMap;
			areAnyReceiptsReceived=data.areAnyReceiptsReceived;
			messageInfoView = new MessageInfoView(convMessage, chatTheme, MessageInfoActivity.this, mConversation, messageInfoAdapter);
			readListString = messageInfoView.getReadListHeaderString(controllerType);
			messageInfoAdapter.setMessageInfoView(messageInfoView);
			addItems();
			notifyAdapter();
		}

		@Override
		public void onLoaderReset(Loader<MessageInfoLoaderData> loader)
		{

		}

		void onDestroy()
		{
			HikeMessengerApp.getPubSub().removeListeners(this, listeners);
			messageInfoView.resetPlayerIfRunning();

		}
		void onBackPress(){
			messageInfoView.resetPlayerIfRunning();
		}
		public ConvMessage getConvMessage()
		{
			return convMessage;
		}

		@Override
		public void onEventReceived(String type, Object object)
		{
			// Holding it for now
			refreshData();
		}

		protected boolean shouldAddPlayedList()
		{
			return convMessage.isFileTransferMessage();
		}

		protected abstract void notifyAdapter();

		public abstract void addItems();

		public boolean isNotApplicable()
		{

			if (convMessage.getState().ordinal() < State.SENT_DELIVERED.ordinal())
				return false;
			return convMessage.getState().ordinal() <= State.SENT_DELIVERED_READ.ordinal() && !areAnyReceiptsReceived;

		}

	}

	private class MessageInfoControllerOnetoOne extends MessageInfoController
	{

		MessageInfoControllerOnetoOne(MessageInfoDataModel dataModel)
		{
			super(dataModel);
			controllerType=MessageInfoView.ONE_TO_ONE;
		}

		@Override
		public void refreshData() {
			dataModel.fetchAllParticipantsInfo();
			addItems();
			notifyAdapter();


		}

		@Override
		protected void notifyAdapter()
		{
			uiHandler.sendEmptyMessage(NOTIFY_ADAPTER);
		}

		@Override
		public void addItems()
		{

			Logger.d("MessageInfo", "Adding Items One to One");
			messageMap.clear();
			participantTreeMap = dataModel.participantTreeMap;
			if(wasSMSMessage)
			isSMSMessage=!dataModel.areAnyReceiptsReceived();
			convMessage.setSMS(isSMSMessage);
			messageMap.add(new MessageInfoItem.MessageInfoViewItem(convMessage));
			if(isNotApplicable()){
				messageMap.add(new MessageInfoItem.MessageInfoNotApplicableItem());
			}else if(isSMSMessage){
				messageMap.add(new MessageInfoItem.MessageInfoSMSItem());
			}
			else
			{
				Iterator<MessageInfoDataModel.MessageInfoParticipantData> iterator = participantTreeMap.values().iterator();
				MessageInfoDataModel.MessageInfoParticipantData participantData = null;
				if (iterator.hasNext())
					participantData = iterator.next();
				// Creating readList
				MessageInfoItem.MessageInfoItemOnetoOne readList = new MessageInfoItem.MessageInfoItemOnetoOne(0, getString(readListString), R.drawable.ic_double_tick_r_blue);
				if (!participantTreeMap.isEmpty())
				{
					if (participantData != null)
						readList.setTimeStamp(participantData.getReadTimeStamp());
				}
				// Creating deliveredList
				MessageInfoItem.MessageInfoItemOnetoOne deliveredList = new MessageInfoItem.MessageInfoItemOnetoOne(0, getString(R.string.delivered_list_one),
						R.drawable.ic_double_tick_blue);
				if (!participantTreeMap.isEmpty())
				{
					if (participantData != null)
						deliveredList.setTimeStamp(participantData.getDeliveredTimeStamp());
				}

				messageMap.add(readList);
				messageMap.add(deliveredList);

			}
		}


		public void updateItemsinMap()
		{

		}

		@Override
		public void onMessageInfoViewDrawn(int height) {
			WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			int screenHeight = display.getHeight();

			if(height>screenHeight-200){
				int offset=height%screenHeight;
				if(offset<screenHeight/3)
					offset=screenHeight/3;
				parentListView.setSelection(1);
			}
		}
	}

	private class MessageInfoControllerGroup extends MessageInfoController
	{

		MessageInfoControllerGroup(MessageInfoDataModel dataModel)
		{
			super(dataModel);
			controllerType=MessageInfoView.GROUP_CHAT;
		}

		@Override
		public void refreshData()
		{
			dataModel.fetchAllParticipantsInfo();
			addItems();
			Iterator<MessageInfoItem> ite = messageInfoItemList.iterator();
			while (ite.hasNext())
			{
				Logger.d("refresh", " messageInfoItemList " + ite.next().toString());
			}
			notifyAdapter();

		}

		@Override
		protected void notifyAdapter()
		{

			uiHandler.sendEmptyMessage(NOTIFY_ADAPTER);

		}

		public void updateItemsinList()
		{
			Iterator<MessageInfoList> iterator = listsToBedisplayed.iterator();

			messageMap.add(new MessageInfoItem.MessageInfoViewItem(convMessage));
			if(isNotApplicable()){
				messageMap.add(new MessageInfoItem.MessageInfoNotApplicableItem());
			}else{
			while (iterator.hasNext())
			{
				MessageInfoList messageInfoList = iterator.next();
				messageInfoList.sortList();
				messageInfoList.setDivider();
				messageMap.add(messageInfoList.messageStatusHeader);
				List<MessageInfoItem.MesageInfoParticipantItem> allDisplayedContactItems = messageInfoList.allDisplayedContactItems;
				for (MessageInfoItem.MesageInfoParticipantItem item : allDisplayedContactItems)
				{
					messageMap.add(item);
				}
				if (messageInfoList.getRemainingItemCount() > 0)
					messageMap.add(messageInfoList.remainingItem);

			}}
		}

		@Override
		public synchronized void addItems()
		{
			Logger.d("MessageInfo", "Adding Items Group");
			messageMap.clear();
			listsToBedisplayed.clear();
			participantTreeMap = dataModel.participantTreeMap;
			Iterator iterator = participantTreeMap.values().iterator();
			readList = new MessageInfoReadList(participantTreeMap.size(), getResources().getString(readListString), R.string.emptyreadlist);
			deliveredList = new MessageInfoDeliveredList(participantTreeMap.size(), R.string.emptydeliveredlist);
			// Disabling played list as of now
			// playedList = new MessageInfoPlayedList(participantTreeMap.size(), R.string.emptyplayedlist);
			while (iterator.hasNext())
			{
				MessageInfoDataModel.MessageInfoParticipantData participantData = (MessageInfoDataModel.MessageInfoParticipantData) iterator.next();
				readList.addParticipant(participantData);
				deliveredList.addParticipant(participantData);

			}

			listsToBedisplayed.add(readList);
			if (deliveredList.shouldAddList())
				listsToBedisplayed.add(deliveredList);

			updateItemsinList();

		}

		@Override
		public void onMessageInfoViewDrawn(int height) {
			WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			int screenHeight = display.getHeight();
			messageviewheight=height;
			screenheightM=screenHeight;
			if(height>screenHeight){
				int offset=height%screenHeight;
				if(offset<screenHeight/3)
					offset=screenHeight/3;


				parentListView.setSelectionFromTop(1, 100);
			}
		}
	}

	private void setChatTheme()
	{

		ColorDrawable statusBarColor = (ColorDrawable) ChatThemeManager.getInstance().
			getDrawableForTheme(chatTheme.getThemeId(), HikeChatThemeConstants.ASSET_INDEX_STATUS_BAR_BG);
		StatusBarColorChanger.setStatusBarColorValue(MessageInfoActivity.this, statusBarColor.getColor());
		setBackground(chatTheme.getThemeId());

	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();

		if (!chatTheme.getThemeId().equals(ChatThemeManager.getInstance().defaultChatThemeId))
			actionBar.setBackgroundDrawable(ChatThemeManager.getInstance().getDrawableForTheme(chatTheme.getThemeId(), HikeChatThemeConstants.ASSET_INDEX_ACTION_BAR_BG));
		else
			actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.blue_hike));
		actionBar.setDisplayShowTitleEnabled(true);

		actionBar.setIcon(R.drawable.hike_logo_top_bar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(getString(R.string.message_info));

		actionBar.setCustomView(actionBarView);
		Toolbar parent = (Toolbar) actionBarView.getParent();
		parent.setContentInsetsAbsolute(0, 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		if (item.getItemId() == android.R.id.home)
		{
			onBackPressed();
			return true;

		}
		return false;
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (controller != null)
		{
			controller.onDestroy();
			controller = null;
		}
	}

	@Override
	protected void setStatusBarColor(Window window, String color)
	{
		// TODO Auto-generated method stub
		return;
	}

	private String populateMessageInfo()
	{

		HashSet<MessageInfo> messageInfoMapw = HikeConversationsDatabase.getInstance().getMessageInfo(messageID);
		Iterator<MessageInfo> messageInfoIterator = messageInfoMapw.iterator();
		String text = " ";
		while (messageInfoIterator.hasNext())
		{
			MessageInfo info = messageInfoIterator.next();
			String readS, deliverS;
			readS = info.getReadTimestamp() == 0 ? " " : "Read at " + Utils.getFormattedTime(false, mContext, info.getReadTimestamp());
			deliverS = info.getDeliveredTimestamp() == 0 ? " " : "Delivered at " + Utils.getFormattedTime(false, mContext, info.getDeliveredTimestamp());
			text = text + info.getReceiverMsisdn() + " " + readS + " " + deliverS + " \n";
			Logger.d("MessageInfo", " setting text as " + text);
		}

		return text;
	}


	private void addMessageHeaderView(ConvMessage convMessage)
	{

		/*
		 * LinearLayout relativeLayout = (LinearLayout) findViewById(R.id.message_container_layout); MessageInfoView messageInfoView = new MessageInfoView(convMessage, chatTheme,
		 * MessageInfoActivity.this, conversation); relativeLayout.addView(messageInfoView.getView(convMessage)); View messageView = findViewById(R.id.relativeLayout);
		 * messageView.setBackgroundResource(chatTheme.bgResId()); final ScrollView sv=(ScrollView)findViewById(R.id.scrollviewmessagecontainer); sv.postDelayed(new Runnable() {
		 * 
		 * @Override public void run() { sv.fullScroll(ScrollView.FOCUS_DOWN); } },0); StatusBarColorChanger.setStatusBarColor(this, chatTheme.statusBarColor()); if (chatTheme !=
		 * ChatTheme.DEFAULT) {
		 * 
		 * ImageView backgroundImage = (ImageView) findViewById(R.id.messageinfo_background); backgroundImage.setScaleType(chatTheme.isTiled() ? ImageView.ScaleType.FIT_XY :
		 * ImageView.ScaleType.CENTER_CROP);
		 * 
		 * backgroundImage.setImageDrawable(Utils.getChatTheme(chatTheme, this)); } else { messageView.setBackgroundResource(R.color.chat_thread_default_bg);
		 * 
		 * }
		 */

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		controller.onBackPress();
		if(removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG)){
			setupActionBar();
			return;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		super.onConfigurationChanged(newConfig);
		setBackground(chatTheme.getThemeId());
		messageInfoAdapter.isScrollPositionSet=false;
		messageInfoAdapter.notifyDataSetChanged();

		Logger.d("orienmsg", "newConfig " + newConfig);

	}
	private void setThemeBackground(RecyclingImageView backgroundImage, Drawable drawable, boolean isTiled, boolean isCustom) {
		if((drawable == null) || (backgroundImage == null)){
			return;
		}
		if(isTiled){
			backgroundImage.setScaleType(ImageView.ScaleType.FIT_XY);
		} else {
			int orientation = mContext.getResources().getConfiguration().orientation;
			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				backgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
			} else {
				backgroundImage.setScaleType(ImageView.ScaleType.MATRIX);
			}
			ChatThreadUtils.applyMatrixTransformationToImageView(drawable, backgroundImage);
		}


		backgroundImage.setImageDrawable(drawable);
	}
	protected void setBackground(String themeId)
	{
		RecyclingImageView backgroundImage = (RecyclingImageView) findViewById(R.id.background);

		//backgroundImage.setOverLay(false);
		if (themeId.equals(ChatThemeManager.getInstance().defaultChatThemeId))
		{
			backgroundImage.setImageDrawable(ChatThemeManager.getInstance().getDrawableForTheme(themeId, HikeChatThemeConstants.ASSET_INDEX_BG_PORTRAIT));
			backgroundImage.setBackgroundResource(R.color.white);

		}
		else
		{

			Drawable drawable = Utils.getChatTheme(themeId, mContext);
			setThemeBackground(backgroundImage, drawable, ChatThemeManager.getInstance().getTheme(themeId).isTiled(), ChatThemeManager.getInstance().getTheme(themeId).isCustomTheme());
		}
	}

}
