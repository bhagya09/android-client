package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessageInfoAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.messageinfo.MessageInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.models.Conversation.OneToOneConversation;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class MessageInfoActivity extends HikeAppStateBaseFragmentActivity implements HikePubSub.Listener
{
	private MessageInfoAdapter messageInfoAdapter;

	private List<ConvMessage> textPins;

	private String msisdn;

	private ChatTheme chatTheme;

	private ImageView backgroundImage;

	private HikeConversationsDatabase mDb;

	private Conversation mConversation;

	private long convId;

	private ListView mPinListView;

	private long messageID;

	public HashSet<MessageInfo> messageInfoMap;

	private TextView messageInfoTextView;

	private Context mContext;


	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED, HikePubSub.BULK_MESSAGE_RECEIVED };

	protected void onCreate(Bundle savedInstanceState)
	{
		/*
		 * Making the action bar transparent for custom theming.
		 */
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		super.onCreate(savedInstanceState);

		initialisePinHistory();
		Toast.makeText(this,"Message Info",Toast.LENGTH_SHORT).show();
	}

	private void initialisePinHistory()
	{
		setContentView(R.layout.message_info);
		mContext=HikeMessengerApp.getInstance().getApplicationContext();
		messageInfoTextView=(TextView)findViewById(R.id.textView);
		msisdn = getIntent().getExtras().getString(HikeConstants.MSISDN);

		messageID=getIntent().getExtras().getLong(HikeConstants.MESSAGE_ID);
		//convId = getIntent().getExtras().getLong(HikeConstants.EXTRA_CONV_ID);

		//backgroundImage = (ImageView) findViewById(R.id.pin_history_background);

		mDb = HikeConversationsDatabase.getInstance();


		this.mConversation =  mDb.getConversation(msisdn, 0, true);
		OneToOneConversation one;



		//this.textPins = mDb.getAllPinMessage(0, HikeConstants.MAX_PINS_TO_LOAD_INITIALLY, msisdn, mConversation);

		chatTheme = mDb.getChatThemeForMsisdn(msisdn);

		populateMessageInfo();


		if (chatTheme != ChatTheme.DEFAULT)
		{
			//backgroundImage.setScaleType(chatTheme.isTiled() ? ScaleType.FIT_XY : ScaleType.CENTER_CROP);

			//backgroundImage.setImageDrawable(Utils.getChatTheme(chatTheme, this));
		}
		else
		{

			//stickylayout.setBackgroundResource(R.color.chat_thread_default_bg);

		}

		//Utils.resetPinUnreadCount(mConversation);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

		setupActionBar();
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
	public void onEventReceived(String type, Object object)
	{
		if (mConversation == null)
		{
			return;
		}

		if (HikePubSub.MESSAGE_RECEIVED.equals(type))
		{

		}
		else if (HikePubSub.BULK_MESSAGE_RECEIVED.equals(type)) {
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
	}


	@Override
	protected void setStatusBarColor(Window window, String color)
	{
		// TODO Auto-generated method stub
		return;
	}
	private void populateMessageInfo(){

		messageInfoMap=mDb.getMessageInfo(messageID);

		Iterator<MessageInfo>messageInfoIterator=messageInfoMap.iterator();
		String text=" ";
		while(messageInfoIterator.hasNext()){
			MessageInfo info=messageInfoIterator.next();
			String readS,deliverS;
			readS=info.getReadTimestamp()==0? " ":"Read at "+Utils.getFormattedTime(false,mContext,info.getReadTimestamp());
			deliverS=info.getDeliveredTimestamp()==0? " ":"Delivered at "+Utils.getFormattedTime(false,mContext,info.getDeliveredTimestamp());
			text=text +info.getReceiverMsisdn()+" "+readS+" "+deliverS+" \n";

		}
		messageInfoTextView.setText(text);
	}
}
