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
import android.widget.EditText;
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
import com.bsb.hike.messageinfo.MessageInfoItem;
import com.bsb.hike.messageinfo.OnetoOneDataModel;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class MessageInfoActivity extends HikeAppStateBaseFragmentActivity implements HikePubSub.Listener
{
	private MessageInfoAdapter messageInfoAdapter;

	private String msisdn;

	private ChatTheme chatTheme;

	private long messageID;

	private ListView parentListView;

	public HashSet<MessageInfo> messageInfoMap;

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
	public void handleUIMessage(Message msg){
		if(msg.what==1){
			messageInfoAdapter.notifyDataSetChanged();
		}
	}

	private List<MessageInfoItem> messageInfoItemList = new ArrayList<MessageInfoItem>();

	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED, HikePubSub.BULK_MESSAGE_RECEIVED };

	private boolean isGroupChat, isOnetoOne, isBroadCast;

	private MessageInfoController controller;

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
		// initialisePinHistory();
		setupActionBar();
		addMessageHeaderView(dataModel.getConvMessage(), 1);
		Toast.makeText(this, "Message Info "+populateMessageInfo(), Toast.LENGTH_LONG).show();
		;
	}

	private void setDataModelsandControllers()
	{
		int type = getIntent().getExtras().getInt(HikeConstants.MESSAGE_INFO.MESSAGE_INFO_TYPE, HikeConstants.MESSAGE_INFO.ONE_TO_ONE);
		msisdn = getIntent().getExtras().getString(HikeConstants.MSISDN);
		messageID = getIntent().getExtras().getLong(HikeConstants.MESSAGE_ID);
		if (type == HikeConstants.MESSAGE_INFO.GROUP)
		{
			isGroupChat = true;
			dataModel = new GroupChatDataModel(msisdn, messageID);
			controller = new MessageInfoControllerGroup(dataModel);

		}
		else if (type == HikeConstants.MESSAGE_INFO.BROADCAST)
		{
			isBroadCast = true;
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

		String[] listeners = new String[] { HikePubSub.MESSAGE_DELIVERED, HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.MSG_READ };

		MessageInfoController(MessageInfoDataModel dataModel)
		{
			this.dataModel = dataModel;
		}
		ConvMessage convMessage;

		void init()
		{
			dataModel.fetchAllParticipantsInfo();
			dataModel.fetchMessageInfo();
			convMessage=dataModel.getConvMessage();
			addItems();
			messageInfoAdapter = new MessageInfoAdapter(MessageInfoActivity.this, messageInfoItemList);
			parentListView.setAdapter(messageInfoAdapter);
			HikeMessengerApp.getPubSub().addListeners(this, listeners);

		}

		void addItems()
		{

		}

		void onDestroy()
		{
			if(controller!=null)
			controller.onDestroy();
		}

		@Override
		public void onEventReceived(String type, Object object)
		{
			refreshUI(type);
		}

		// This can be different across controller way to refresh so keeping like this as of now
		public void refreshUI(String eventType)
		{
			Logger.d("MessageInfo","Refreshing UI");
			dataModel.fetchMessageInfo();
			messageInfoItemList.clear();
			addItems();
			uiHandler.sendEmptyMessage(1);
			//messageInfoAdapter.notifyDataSetChanged();
		}

	}

	private class MessageInfoControllerOnetoOne extends MessageInfoController
	{


		MessageInfoControllerOnetoOne(MessageInfoDataModel dataModel)
		{
			super(dataModel);
		}

		@Override
		public void addItems()
		{
			Logger.d("MessageInfo","Adding Items One to One");
			messageInfoItemList.add(new MessageInfoItem.MessageStatusHeader(MessageInfoItem.HEADER_ID, "Read List"));
			participantTreeMap = dataModel.participantTreeMap;
			Iterator iterator = participantTreeMap.values().iterator();
			MessageInfoDataModel.MessageInfoParticipantData participantData = (MessageInfoDataModel.MessageInfoParticipantData) iterator.next();

			messageInfoItemList.add(new MessageInfoItem.MesageInfoParticipantItem(participantData, MessageInfoItem.MesageInfoParticipantItem.READ_CONTACT,
				MessageInfoItem.MesageInfoParticipantItem.READ_CONTACT));
			messageInfoItemList.add(new MessageInfoItem.MessageStatusHeader(MessageInfoItem.HEADER_ID, "Delivered List"));
			messageInfoItemList.add(new MessageInfoItem.MesageInfoParticipantItem(participantData, MessageInfoItem.MesageInfoParticipantItem.DELIVERED_CONTACT,
				MessageInfoItem.MesageInfoParticipantItem.DELIVERED_CONTACT));

		}



	}

	private class MessageInfoControllerGroup extends MessageInfoController
	{

		MessageInfoControllerGroup(MessageInfoDataModel dataModel)
		{
			super(dataModel);
		}


		@Override
		public void addItems()
		{
			Logger.d("MessageInfo", "Adding Items Group");

			participantTreeMap = dataModel.participantTreeMap;
			Iterator iterator = participantTreeMap.values().iterator();

			ArrayList<MessageInfoItem> readList=new ArrayList<MessageInfoItem>();
			ArrayList<MessageInfoItem> deliveredList=new ArrayList<MessageInfoItem>();
			ArrayList<MessageInfoItem> playedList=new ArrayList<MessageInfoItem>();
			while(iterator.hasNext()){
				MessageInfoDataModel.MessageInfoParticipantData participantData = (MessageInfoDataModel.MessageInfoParticipantData) iterator.next();
				readList.add(new MessageInfoItem.MesageInfoParticipantItem(participantData, MessageInfoItem.MesageInfoParticipantItem.READ_CONTACT,
					MessageInfoItem.MesageInfoParticipantItem.READ_CONTACT));
				deliveredList.add((new MessageInfoItem.MesageInfoParticipantItem(participantData, MessageInfoItem.MesageInfoParticipantItem.DELIVERED_CONTACT,
						MessageInfoItem.MesageInfoParticipantItem.DELIVERED_CONTACT)));
				playedList.add((new MessageInfoItem.MesageInfoParticipantItem(participantData, MessageInfoItem.MesageInfoParticipantItem.PLAYED_CONTACT,
					MessageInfoItem.MesageInfoParticipantItem.PLAYED_CONTACT)));
			}


			messageInfoItemList.add(new MessageInfoItem.MessageStatusHeader(MessageInfoItem.HEADER_ID, "Read List"));
			for(MessageInfoItem messageInfoItem:readList){
				messageInfoItemList.add(messageInfoItem);
			}

			messageInfoItemList.add(new MessageInfoItem.MessageStatusHeader(MessageInfoItem.HEADER_ID, "Delivered List"));
			for(MessageInfoItem messageInfoItem:deliveredList){
				messageInfoItemList.add(messageInfoItem);
			}


		}

	}


	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();

		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		chatTheme = db.getChatThemeForMsisdn(msisdn);

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

	 HashSet<MessageInfo>messageInfoMapw = HikeConversationsDatabase.getInstance().getMessageInfo(messageID);
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

	private void addMessageHeaderView(ConvMessage convMessage, int messageType)
	{
		// TODO :Layout will be different based on the type of message sent
		switch (messageType)
		{
		// Text Sent
		case 1:
		{
			headerView = getLayoutInflater().inflate(R.layout.messageinfo_sent_text, null);

			TextView tv = (TextView) headerView.findViewById(R.id.text);
			// tv.setText(message);
			ViewStub viewStub = (ViewStub) findViewById(R.id.textMessage);
			View v = viewStub.inflate();
			TextView t = (TextView) v.findViewById(R.id.text);
			t.setText(convMessage.getMessage());

			break;
		}
		// Image Sent
		case 2:
		{
			break;
		}
		// Audio Sent
		case 3:
		{
			break;
		}
		// Video sent
		case 4:
		{
			break;
		}
		// Location Sent
		case 5:
		{
			break;
		}
		// File Sent
		case 6:
		{

			break;
		}
		// Nudge Sent
		case 7:
		{
			break;
		}
		// Default case
		default:

		}

		// parentListView.addHeaderView(headerView);

	}

}
