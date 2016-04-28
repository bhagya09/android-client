package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessageInfoAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.messageinfo.GroupChatDataModel;
import com.bsb.hike.messageinfo.MessageInfo;
import com.bsb.hike.messageinfo.MessageInfoDataModel;
import com.bsb.hike.messageinfo.MessageInfoDeliveredList;
import com.bsb.hike.messageinfo.MessageInfoItem;
import com.bsb.hike.messageinfo.MessageInfoList;
import com.bsb.hike.messageinfo.MessageInfoPlayedList;
import com.bsb.hike.messageinfo.MessageInfoReadList;
import com.bsb.hike.messageinfo.OnetoOneDataModel;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class MessageInfoActivity extends HikeAppStateBaseFragmentActivity implements HikePubSub.Listener
{
	private MessageInfoAdapter messageInfoAdapter;

	private String msisdn;

	private long messageID;

	private ListView parentListView;

	private Context mContext;

	View headerView;


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
		if (msg.what == 1)
		{
			messageInfoAdapter.notifyDataSetChanged();
		}
	}

	private List<MessageInfoItem> messageInfoItemList = new ArrayList<MessageInfoItem>();


	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED, HikePubSub.BULK_MESSAGE_RECEIVED };

	private boolean isGroupChat, isOnetoOne, isBroadCast;

	private MessageInfoController controller;

	private ChatTheme chatTheme;

	private MessageInfoDataModel dataModel;

	protected void onCreate(Bundle savedInstanceState)
	{
		/*
		 * Making the action bar transparent for custom theming.
		 */
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		super.onCreate(savedInstanceState);

		initializeListViewandAdapters();
		setDataModelsandControllers();

		chatTheme = HikeConversationsDatabase.getInstance().getChatThemeForMsisdn(msisdn);
		setupActionBar();
		addMessageHeaderView(dataModel.getConvMessage());
		Toast.makeText(this, "Message Info " + populateMessageInfo(), Toast.LENGTH_LONG).show();

	}

	private void setDataModelsandControllers()
	{
		int type = getIntent().getExtras().getInt(HikeConstants.MESSAGE_INFO.MESSAGE_INFO_TYPE, HikeConstants.MESSAGE_INFO.ONE_TO_ONE);
		msisdn = getIntent().getExtras().getString(HikeConstants.MSISDN);
		messageID = getIntent().getExtras().getLong(HikeConstants.MESSAGE_ID);
		if (type == HikeConstants.MESSAGE_INFO.GROUP || type == HikeConstants.MESSAGE_INFO.BROADCAST)
		{
			isGroupChat = true;
			dataModel = new GroupChatDataModel(msisdn, messageID);
			controller = new MessageInfoControllerGroup(dataModel);

		}
		else
		{
			isOnetoOne = true;
			dataModel = new OnetoOneDataModel(msisdn, messageID);
			controller = new MessageInfoControllerOnetoOne(dataModel);
		}
		controller.init();

	}

	public void initializeListViewandAdapters()
	{
		setContentView(R.layout.message_info_try);
		parentListView = (ListView) findViewById(R.id.profile_content);
		headerView = null;
		mContext = HikeMessengerApp.getInstance().getApplicationContext();

	}

	private abstract class MessageInfoController implements HikePubSub.Listener
	{
		MessageInfoDataModel dataModel;

		public TreeMap<String, MessageInfoDataModel.MessageInfoParticipantData> participantTreeMap;

		String[] listeners = new String[] { HikePubSub.MESSAGE_DELIVERED, HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.MSG_READ, HikePubSub.ONETON_MESSAGE_DELIVERED_READ };

		MessageInfoController(MessageInfoDataModel dataModel)
		{
			this.dataModel = dataModel;
		}

		ConvMessage convMessage;

		public abstract void refreshData();

		void init()
		{
			dataModel.fetchAllParticipantsInfo();
			convMessage = dataModel.getConvMessage();
			addItems();

			messageInfoAdapter = new MessageInfoAdapter(MessageInfoActivity.this, messageInfoItemList, isGroupChat);

			parentListView.setAdapter(messageInfoAdapter);
			HikeMessengerApp.getPubSub().addListeners(this, listeners);

		}

		void onDestroy()
		{
			if (controller != null)
				controller.onDestroy();
		}

		@Override
		public void onEventReceived(String type, Object object)
		{
			//Holding it for now
			//refreshData();
		}

		protected boolean shouldAddPlayedList()
		{
			return convMessage.isFileTransferMessage();
		}

		protected abstract void notifyAdapter();

		public abstract void addItems();
	}

	private class MessageInfoControllerOnetoOne extends MessageInfoController
	{

		MessageInfoControllerOnetoOne(MessageInfoDataModel dataModel)
		{
			super(dataModel);
		}

		@Override
		public void refreshData()
		{

		}

		@Override
		protected void notifyAdapter()
		{

		}

		@Override
		public void addItems()
		{
			Logger.d("MessageInfo", "Adding Items One to One");

			messageInfoItemList.add(new MessageInfoItem.MessageStatusHeader("Read List", R.drawable.ic_double_tick_white));
			participantTreeMap = dataModel.participantTreeMap;
			Iterator iterator = participantTreeMap.values().iterator();
			MessageInfoDataModel.MessageInfoParticipantData participantData = (MessageInfoDataModel.MessageInfoParticipantData) iterator.next();

			messageInfoItemList.add(new MessageInfoItem.MesageInfoParticipantItem(participantData, MessageInfoItem.MesageInfoParticipantItem.READ_CONTACT,
					MessageInfoItem.MesageInfoParticipantItem.READ_CONTACT));
			messageInfoItemList.add(new MessageInfoItem.MessageStatusHeader("Delivered List", R.drawable.ic_double_tick_white));
			messageInfoItemList.add(new MessageInfoItem.MesageInfoParticipantItem(participantData, MessageInfoItem.MesageInfoParticipantItem.DELIVERED_CONTACT,
					MessageInfoItem.MesageInfoParticipantItem.DELIVERED_CONTACT));
			notifyAdapter();
		}

	}

	private class MessageInfoControllerGroup extends MessageInfoController
	{

		MessageInfoControllerGroup(MessageInfoDataModel dataModel)
		{
			super(dataModel);
		}

		@Override
		public void refreshData()
		{
			dataModel.fetchAllParticipantsInfo();
			addItems();
			Iterator<MessageInfoItem> ite=messageInfoItemList.iterator();
			while(ite.hasNext()){
				Logger.d("refresh"," messageInfoItemList "+ite.next().toString());
			}
			notifyAdapter();
		}

		@Override
		protected void notifyAdapter()
		{

			uiHandler.sendEmptyMessageDelayed(1,1000);

		}

		public void updateItemsinList()
		{
			Iterator<MessageInfoList> iterator = listsToBedisplayed.iterator();
			while (iterator.hasNext())
			{
				MessageInfoList messageInfoList = iterator.next();
				messageInfoItemList.add(messageInfoList.messageStatusHeader);
				List<MessageInfoItem.MesageInfoParticipantItem> allDisplayedContactItems = messageInfoList.allDisplayedContactItems;
				for (MessageInfoItem.MesageInfoParticipantItem item : allDisplayedContactItems)
				{
					messageInfoItemList.add(item);
				}
				if (messageInfoList.getRemainingItemCount() > 0)
					messageInfoItemList.add(messageInfoList.remainingItem);
			}
			for (MessageInfoItem item : messageInfoItemList)
			{
				Logger.d("MList", " " + item);
			}
		}

		public List<MessageInfoList> listsToBedisplayed = new ArrayList<MessageInfoList>();

		MessageInfoList readList, deliveredList, playedList;

		@Override
		public synchronized void addItems()
		{
			Logger.d("MessageInfo", "Adding Items Group");
			messageInfoItemList.clear();
			listsToBedisplayed.clear();
			participantTreeMap = dataModel.participantTreeMap;
			Iterator iterator = participantTreeMap.values().iterator();
			readList = new MessageInfoReadList(participantTreeMap.size(),getResources().getString(R.string.read_list),R.string.emptyreadlist);
			deliveredList = new MessageInfoDeliveredList(participantTreeMap.size(),R.string.emptydeliveredlist);
			playedList = new MessageInfoPlayedList(participantTreeMap.size(),R.string.emptyplayedlist);
			while (iterator.hasNext())
			{

				MessageInfoDataModel.MessageInfoParticipantData participantData = (MessageInfoDataModel.MessageInfoParticipantData) iterator.next();

				readList.addParticipant(participantData);
				deliveredList.addParticipant(participantData);
				if (shouldAddPlayedList())
				{
					playedList.addParticipant(participantData);
				}
			}
			if (shouldAddPlayedList())
			{
				listsToBedisplayed.add(playedList);

			}
			listsToBedisplayed.add(readList);
			listsToBedisplayed.add(deliveredList);

			updateItemsinList();

		}

	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();

		if (chatTheme != ChatTheme.DEFAULT)
			actionBar.setBackgroundDrawable(getResources().getDrawable(chatTheme.headerBgResId()));
		else
			actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.blue_hike));
		actionBar.setDisplayShowTitleEnabled(true);

		actionBar.setIcon(R.drawable.hike_logo_top_bar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText("Message Info");

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

		}
		return true;
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
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

		headerView = getLayoutInflater().inflate(R.layout.messageinfo_sent_text, null);

		TextView tv = (TextView) headerView.findViewById(R.id.text);
		// tv.setText(message);
		ViewStub viewStub = (ViewStub) findViewById(R.id.textMessage);
		View v = viewStub.inflate();
		TextView t = (TextView) v.findViewById(R.id.text);
		t.setText(convMessage.getMessage());
		View messageView = findViewById(R.id.relativeLayout);
		messageView.setBackgroundResource(chatTheme.bgResId());
		StatusBarColorChanger.setStatusBarColor(this, chatTheme.statusBarColor());
		if (chatTheme != ChatTheme.DEFAULT)
		{

			ImageView backgroundImage = (ImageView) findViewById(R.id.messageinfo_background);
			backgroundImage.setScaleType(chatTheme.isTiled() ? ImageView.ScaleType.FIT_XY : ImageView.ScaleType.CENTER_CROP);

			backgroundImage.setImageDrawable(Utils.getChatTheme(chatTheme, this));
		}
		else
		{
			messageView.setBackgroundResource(R.color.chat_thread_default_bg);

		}

	}
}
