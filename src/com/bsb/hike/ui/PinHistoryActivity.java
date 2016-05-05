package com.bsb.hike.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.PinHistoryAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.ui.utils.StatusBarColorChanger;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class PinHistoryActivity extends HikeAppStateBaseFragmentActivity implements OnScrollListener, HikePubSub.Listener, OnItemLongClickListener 
{	
	private PinHistoryAdapter pinAdapter;
	
	private List<ConvMessage> textPins;
	
	private String msisdn;
	
	private ChatTheme chatTheme;
	
	private ImageView backgroundImage;
		
	private HikeConversationsDatabase mDb;

	private OneToNConversation mConversation;
	
	private long convId;
	
	private ListView mPinListView;
	
	private boolean mLoadingMorePins;
	
	private boolean mReachedEnd;
	
	private boolean isActionModeOn;
	
	private TextView mActionModeTitle;
		
	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED, HikePubSub.BULK_MESSAGE_RECEIVED};

	protected void onCreate(Bundle savedInstanceState)
	{
		/*
		* Making the action bar transparent for custom theming.
		*/
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		
		super.onCreate(savedInstanceState);
		
		initialisePinHistory();
	}	
	
	private void initialisePinHistory()
	{
		setContentView(R.layout.sticky_pins);
		
		msisdn = getIntent().getExtras().getString(HikeConstants.TEXT_PINS);
		
		convId = getIntent().getExtras().getLong(HikeConstants.EXTRA_CONV_ID);

		mPinListView = (ListView) findViewById(android.R.id.list);
		
		backgroundImage = (ImageView) findViewById(R.id.pin_history_background);

		mDb = HikeConversationsDatabase.getInstance();
		
		this.mConversation = (OneToNConversation) mDb.getConversation(msisdn, 0, true);
		
		this.textPins = mDb.getAllPinMessage(0, HikeConstants.MAX_PINS_TO_LOAD_INITIALLY, msisdn, mConversation);
		
		chatTheme = mDb.getChatThemeForMsisdn(msisdn);
		
		mPinListView.setEmptyView(findViewById(android.R.id.empty));
		
		pinAdapter = new PinHistoryAdapter(this, textPins, msisdn, convId, mConversation, true,chatTheme, this);

		mPinListView.setOnScrollListener(this);
		
		mPinListView.setOnItemLongClickListener(this);
		
		mPinListView.setAdapter(pinAdapter);
		StatusBarColorChanger.setStatusBarColor(this, chatTheme.statusBarColor());
		
		if (chatTheme != ChatTheme.DEFAULT)
		{
			backgroundImage.setScaleType(chatTheme.isTiled() ? ScaleType.FIT_XY : ScaleType.CENTER_CROP);
			
			backgroundImage.setImageDrawable(Utils.getChatTheme(chatTheme, this));
		}
		else
		{
			View stickylayout=findViewById(R.id.sticky_parent_list);
			stickylayout.setBackgroundResource(R.color.chat_thread_default_bg);
			
		}
		
		Utils.resetPinUnreadCount(mConversation);
		
		View pinEmptyState = getPinEmptyState(chatTheme);
		
		if (pinEmptyState != null)
		{
			ViewGroup empty = (ViewGroup)findViewById(android.R.id.empty);
			
			empty.removeAllViews();
			
			empty.addView(pinEmptyState);
			
			mPinListView.setEmptyView(empty);
		}

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

		setupActionBar();
	}
	
	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		
		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		chatTheme = db.getChatThemeForMsisdn(msisdn);
		if(chatTheme!=ChatTheme.DEFAULT)
		actionBar.setBackgroundDrawable(getResources().getDrawable(chatTheme.headerBgResId()));
		else
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.blue_hike));
		actionBar.setDisplayShowTitleEnabled(true);

		actionBar.setIcon(R.drawable.hike_logo_top_bar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.pin_history);

		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if(isActionModeOn)
		{
			getMenuInflater().inflate(R.menu.multi_select_chat_menu, menu);
			
			menu.findItem(R.id.forward_msgs).setVisible(false);
			
			menu.findItem(R.id.copy_msgs).setVisible(true);
		}else
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
		if(isActionModeOn)
		{
			return onActionModeItemClicked(item);
		}
		if(item.getItemId()==android.R.id.home)
		{
			onBackPressed();
			
		}
		return true;
	}
	
	@Override
	public void onBackPressed()
	{
		if(isActionModeOn)
		{
			HikeAnalyticsEvent.recordAnalyticsForGCPins(AnalyticsConstants.GCEvents.GC_PIN_ACTION, AnalyticsConstants.GCEvents.GC_PIN_ACTION_CANCEL, null, AnalyticsConstants.GCEvents.CANCEL_SRC_OTHERS);
			destroyActionMode();
			return;
		}
		super.onBackPressed();
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
	public void onScrollStateChanged(AbsListView view, int scrollState) 
	{
		
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) 
	{
		if (!mReachedEnd && !mLoadingMorePins && textPins != null && !textPins.isEmpty() && (firstVisibleItem + visibleItemCount)  <= totalItemCount - 5)
		{
			mLoadingMorePins = true;
			
			AsyncTask<Void, Void, List<ConvMessage>> asyncTask = new AsyncTask<Void, Void, List<ConvMessage>>()
			{
				@Override
				protected List<ConvMessage> doInBackground(Void... params)
				{
					return mDb.getAllPinMessage(pinAdapter.getCurrentPinsCount(), HikeConstants.MAX_OLDER_PINS_TO_LOAD_EACH_TIME, msisdn, mConversation);
				}

				@Override
				protected void onPostExecute(List<ConvMessage> result)
				{
					if (!result.isEmpty())
					{
						pinAdapter.appendPinstoView(result);
					}
					else
					{
						mReachedEnd = true;
					}
					mLoadingMorePins = false;
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
	public void onEventReceived(String type, Object object) 
	{
		if(mConversation == null)
		{
			return;
		}
		
		if(HikePubSub.MESSAGE_RECEIVED.equals(type))
		{
			final ConvMessage convMsg = (ConvMessage)object;			
			handleIncomingPin(convMsg);			
		}
		else if(HikePubSub.BULK_MESSAGE_RECEIVED.equals(type))
		{
			HashMap<String, LinkedList<ConvMessage>> messageListMap = (HashMap<String, LinkedList<ConvMessage>>) object;
			final LinkedList<ConvMessage> messageList = messageListMap.get(msisdn);
			
			if(messageList != null)
			{
				List<ConvMessage> pinsOnly = new ArrayList<ConvMessage>();

				for (final ConvMessage message : messageList)
				{
					if(message.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
					{
						String msisdn = message.getMsisdn();
						
						if(msisdn != null && msisdn.equals(this.msisdn))
						{
							pinsOnly.add(message);
						}
					}
				}
				handleBulkPins(pinsOnly);
			}
		}
	}

	private TextView getPinEmptyState(ChatTheme chatTheme)
	{
		try
		{
			TextView tv = (TextView) LayoutInflater.from(this).inflate(chatTheme.systemMessageTextViewLayoutId(), null, false);
			tv.setText(R.string.pinHistoryTutorialText);
			if (chatTheme == ChatTheme.DEFAULT)
			{
				tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pin_empty_state_default, 0, 0, 0);
			}
			else
			{
				tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pin_empty_state, 0, 0, 0);
			}
			tv.setCompoundDrawablePadding(10);
			android.widget.ScrollView.LayoutParams lp = new ScrollView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp.gravity = Gravity.CENTER;
			tv.setLayoutParams(lp);
			return tv;
		}
		catch (Exception e)
		{
			// if chat theme starts returning layout id which is not text-view, playSafe
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
	}

	private void setupActionModeActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setDisplayHomeAsUpEnabled(false);
		View actionBarView = LayoutInflater.from(this).inflate(R.layout.action_mode_action_bar, null);

		View closeBtn = actionBarView.findViewById(R.id.close_action_mode);
		mActionModeTitle = (TextView) actionBarView.findViewById(R.id.title);
		ViewGroup closeContainer = (ViewGroup) actionBarView.findViewById(R.id.close_container);

		closeContainer.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				HikeAnalyticsEvent.recordAnalyticsForGCPins(AnalyticsConstants.GCEvents.GC_PIN_ACTION, AnalyticsConstants.GCEvents.GC_PIN_ACTION_CANCEL, null, AnalyticsConstants.GCEvents.CANCEL_SRC_CROSS);
				destroyActionMode();
			}
		});
		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);
		
		initializeActionMode();
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) 
	{
		HikeAnalyticsEvent.recordAnalyticsForGCPins(AnalyticsConstants.GCEvents.GC_PIN_LONG_TAP, null, null, null);
		return showPinContextMenu((ConvMessage)pinAdapter.getItem(position));
	}
	
	public boolean showPinContextMenu(ConvMessage pinMsg)
	{
		if(pinMsg == null)
		{
			return false;
		}
		pinAdapter.toggleSelection(pinMsg);
		
		boolean hasCheckedItems = pinAdapter.getSelectedPinsCount() > 0;
		
		if(hasCheckedItems && !isActionModeOn)
		{
			setupActionModeActionBar();
		}
		else if(!hasCheckedItems && isActionModeOn)
		{
			HikeAnalyticsEvent.recordAnalyticsForGCPins(AnalyticsConstants.GCEvents.GC_PIN_ACTION, AnalyticsConstants.GCEvents.GC_PIN_ACTION_CANCEL, null, AnalyticsConstants.GCEvents.CANCEL_SRC_OTHERS);
			destroyActionMode();
		}
		
		if(isActionModeOn)
		{
			setActionModeTitle(pinAdapter.getSelectedPinsCount());
		}
		invalidateOptionsMenu();
		
		return true;
	}
	
	private void destroyActionMode()
	{
		pinAdapter.removeSelection();		
		setActionModeOn(false);
		setupActionBar();
		
		invalidateOptionsMenu();
	}
	
	private void setActionModeTitle(int count)
	{
		if (mActionModeTitle != null)
		{
			mActionModeTitle.setText(getString(R.string.selected_count, count));
		}
	}
	
	public boolean initializeActionMode()
	{
		setActionModeOn(true);
		
		if (pinAdapter.getSelectedPinsCount() > 0)
		{
			setActionModeTitle(pinAdapter.getSelectedPinsCount());
		}
		return true;
	}
	
	private void setActionModeOn(boolean isOn)
	{
		isActionModeOn = isOn;
		pinAdapter.setActionMode(isOn);
		pinAdapter.notifyDataSetChanged();
	}
	
	public boolean onActionModeItemClicked(MenuItem item)
	{
		final ArrayList<Long> selectedPinIds = new ArrayList<Long>(pinAdapter.getSelectedPinsIds());;
		
		switch (item.getItemId())
		{
		case R.id.delete_msgs:
			HikeDialogFactory.showDialog(PinHistoryActivity.this, HikeDialogFactory.DELETE_PINS_DIALOG, new HikeDialogListener()
			{
				
				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					HikeAnalyticsEvent.recordAnalyticsForGCPins(AnalyticsConstants.GCEvents.GC_PIN_ACTION, AnalyticsConstants.GCEvents.GC_PIN_ACTION_DELETE, null, null);
					removeMessage(selectedPinIds);					
					pinAdapter.notifyDataSetChanged();
					destroyActionMode();
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
				
			}, pinAdapter.getSelectedPinsCount());
			
			return true;
			
		case R.id.copy_msgs:
			Collections.sort(selectedPinIds);
			StringBuilder pinStr = new StringBuilder();
			int size = selectedPinIds.size();
			
			for (int i = 0; i < size; i++)
			{
				pinStr.append(pinAdapter.getSelectedPinsMap().get(selectedPinIds.get(i)).getMessage());
				pinStr.append("\n");				
			}
			Utils.setClipboardText(pinStr.toString(), getApplicationContext());
			Toast.makeText(PinHistoryActivity.this, R.string.copied, Toast.LENGTH_SHORT).show();
			HikeAnalyticsEvent.recordAnalyticsForGCPins(AnalyticsConstants.GCEvents.GC_PIN_ACTION, AnalyticsConstants.GCEvents.GC_PIN_ACTION_COPY, null, null);
			destroyActionMode();
			return true;
		default:
			destroyActionMode();
		return false;
		}
	}
	
	/**
	 * Removes the selected pins from db and updates pin adapter 
	 * 
	 * @param selectedPinIds message ids of pins to be deleted
	 */
	private void removeMessage(ArrayList<Long> selectedPinIds)
	{
		Bundle bundle = new Bundle();
		bundle.putString(HikeConstants.Extras.MSISDN, msisdn);
		bundle.putInt(HikeConstants.Extras.DELETED_MESSAGE_TYPE, HikeConstants.SHARED_PIN_TYPE);
		HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_MESSAGE, new Pair<ArrayList<Long>, Bundle>(selectedPinIds, bundle));
		
		
		final HashMap<Long, ConvMessage> selectedMessagesMap = pinAdapter.getSelectedPinsMap();

		if(selectedMessagesMap.containsKey(textPins.get(0).getMsgID()))
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.LATEST_PIN_DELETED, textPins.get(0).getMsgID());			
		}

		int size = selectedMessagesMap.size();
		
		for(int i=0; i<size; i++)
		{
			pinAdapter.removeMessage(selectedMessagesMap.get(selectedPinIds.get(i)));
		}
	}
	
	private void handleIncomingPin(final ConvMessage convMsg)
	{
		if(convMsg.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			String msisdn = convMsg.getMsisdn();
			
			if(msisdn != null && msisdn.equals(this.msisdn))
			{
				
				if(pinAdapter != null)
				{
					runOnUiThread(new Runnable() 
					{						
						@Override
						public void run() 
						{
							pinAdapter.addPinMessage(convMsg);
							pinAdapter.notifyDataSetChanged();							
						}
					});
				}
			}
			Utils.resetPinUnreadCount(mConversation);
		}
	}
	
	private void handleBulkPins(final List<ConvMessage> pinList)
	{
		if(pinAdapter != null)
		{
			runOnUiThread(new Runnable() 
			{						
				@Override
				public void run() 
				{
					pinAdapter.addPins(pinList);
				}
			});
			Utils.resetPinUnreadCount(mConversation);
		}
	}
	@Override
	protected void setStatusBarColor(Window window, String color)
	{
		// TODO Auto-generated method stub
		return;
	}
}
