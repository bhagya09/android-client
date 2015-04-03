package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeSharedFileAdapter;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.CustomAlertDialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.ui.fragments.PhotoViewerFragment;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class HikeSharedFilesActivity extends HikeAppStateBaseFragmentActivity implements OnScrollListener, OnItemClickListener, OnItemLongClickListener
{
	static String MULTISELECT_MODE = "multi_mode";
	static String MULTISELECT_KEYS = "multi_keys";
	private List<HikeSharedFile> sharedFilesList;

	private HikeSharedFileAdapter adapter;

	private String msisdn;

	private boolean multiSelectMode;

	private HashSet<Long> selectedSharedFileItems;

	private TextView multiSelectTitle;

	private int previousFirstVisibleItem;

	private long previousEventTime;

	private int velocity;
	
	private boolean reachedEnd = false;
	
	private boolean loadingMoreItems = false;
	
	private boolean isGroup = false;
	
	private String conversationName;
	
	private String[] msisdnArray = null;
	
	private String[] nameArray = null;
	
	private String TAG = "HikeSharedFilesActivity";
	
	public boolean isMultiSelectMode()
	{
		return multiSelectMode;
	}

	public void setMultiSelectMode(boolean multiSelectMode)
	{
		this.multiSelectMode = multiSelectMode;
	}

	private String[] pubSubListeners = { HikePubSub.HIKE_SHARED_FILE_DELETED };

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_ACTION_BAR_OVERLAY);
		setContentView(R.layout.gallery);

		selectedSharedFileItems = new HashSet<Long>();
		sharedFilesList = new ArrayList<HikeSharedFile>();

		Bundle data;
		if (savedInstanceState != null)
		{
			data = savedInstanceState;
		}
		else
		{
			data = getIntent().getExtras();
		}

		msisdn = data.getString(HikeConstants.Extras.MSISDN);
		isGroup = data.getBoolean(HikeConstants.Extras.IS_GROUP_CONVERSATION, false);
		conversationName = data.getString(HikeConstants.Extras.CONVERSATION_NAME);
		if(isGroup)
		{
			msisdnArray = data.getStringArray(HikeConstants.Extras.PARTICIPANT_MSISDN_ARRAY);
			nameArray = data.getStringArray(HikeConstants.Extras.PARTICIPANT_NAME_ARRAY);
		}

		GridView gridView = (GridView) findViewById(R.id.gallery);

		int sizeOfImage = getResources().getDimensionPixelSize(R.dimen.gallery_album_item_size);

		int numColumns = Utils.getNumColumnsForGallery(getResources(), sizeOfImage);
		int actualSize = Utils.getActualSizeForGallery(getResources(), sizeOfImage, numColumns);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
		
		sharedFilesList = (List<HikeSharedFile>) HikeConversationsDatabase.getInstance().getSharedMedia(msisdn, HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY, -1, true);
		adapter = new HikeSharedFileAdapter(this, sharedFilesList, actualSize, selectedSharedFileItems, false);

		gridView.setNumColumns(numColumns);
		gridView.setAdapter(adapter);
		gridView.setOnScrollListener(this);
		gridView.setOnItemClickListener(this);
		gridView.setOnItemLongClickListener(this);
		
		setupActionBar();
		if(data.getBoolean(MULTISELECT_MODE)){
			long[] selecKeys = data.getLongArray(MULTISELECT_KEYS);
			if(selecKeys!=null){
				int size = selecKeys.length;
				for(int i=0;i<size;i++){
				selectedSharedFileItems.add(selecKeys[i]);
				}
				multiSelectMode=true;
				setupMultiSelectActionBar();
			}
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.ClOSE_PHOTO_VIEWER_FRAGMENT, null);
	}

	@Override
	public void onBackPressed()
	{
		if(removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG))
		{
			return;
		}
		if (multiSelectMode)
		{
			destroyActionMode();
		}
		else
		{
			super.onBackPressed();
		}
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(conversationName);

		TextView subText = (TextView) actionBarView.findViewById(R.id.subtext);
		subText.setVisibility(View.GONE);

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_header));
		
		actionBar.setCustomView(actionBarView);
	}

	private void setupMultiSelectActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.action_mode_action_bar, null);

		View closeBtn = actionBarView.findViewById(R.id.close_action_mode);
		multiSelectTitle = (TextView) actionBarView.findViewById(R.id.title);
		ViewGroup closeContainer = (ViewGroup) actionBarView.findViewById(R.id.close_container);

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});
		actionBar.setCustomView(actionBarView);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);

		initializeActionMode();
		setMultiSelectTitle();
	}

	public boolean initializeActionMode()
	{
		setMultiSelectMode(true);
		if (selectedSharedFileItems.size() > 0)
		{
			setActionModeTitle(selectedSharedFileItems.size());
		}
		invalidateOptionsMenu();
		return true;
	}

	public void destroyActionMode()
	{
		selectedSharedFileItems.clear();
		adapter.notifyDataSetChanged();
		setMultiSelectMode(false);
		setupActionBar();
		invalidateOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (multiSelectMode)
		{
			getSupportMenuInflater().inflate(R.menu.multi_select_chat_menu, menu);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if(multiSelectMode)
		{
			if (selectedSharedFileItems.size() > FileTransferManager.getInstance(this).remainingTransfers())
			{
				menu.findItem(R.id.forward_msgs).setVisible(false);
			}
			else
			{
				menu.findItem(R.id.forward_msgs).setVisible(true);
			}
			
			menu.findItem(R.id.share_msgs).setVisible(selectedSharedFileItems.size() == 1);
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	private void setActionModeTitle(int count)
	{
		if (multiSelectTitle != null)
		{
			multiSelectTitle.setText(getString(R.string.selected_count, count));
		}
	}

	
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
	
		if (!reachedEnd && !loadingMoreItems && sharedFilesList != null && !sharedFilesList.isEmpty() && (firstVisibleItem + visibleItemCount)  <= totalItemCount - 9)
		{
			loadingMoreItems = true;

			final long lastItemId = sharedFilesList.get(sharedFilesList.size()-1).getMsgId();

			AsyncTask<Void, Void, List<HikeSharedFile>> asyncTask = new AsyncTask<Void, Void, List<HikeSharedFile>>()
			{

				@Override
				protected List<HikeSharedFile> doInBackground(Void... params)
				{
					return  (List<HikeSharedFile>) HikeConversationsDatabase.getInstance().getSharedMedia(msisdn, HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY, lastItemId, true);
				}

				@Override
				protected void onPostExecute(List<HikeSharedFile> result)
				{
					if (!result.isEmpty())
					{
						sharedFilesList.addAll(result);
						adapter.notifyDataSetChanged();
					}
					else
					{
						Logger.d(TAG,"No more items found in Db");
						reachedEnd = true;
					}
					loadingMoreItems = false;
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
		adapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES && scrollState == OnScrollListener.SCROLL_STATE_FLING);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		handleItemClick(position, id);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		if (!multiSelectMode)
		{
			setupMultiSelectActionBar();
		}

		handleItemClick(position, id);

		return true;
	}

	private void handleItemClick(int position, long id)
	{
		HikeSharedFile sharedFileItem = sharedFilesList.get(position);

		if (multiSelectMode)
		{
			if (selectedSharedFileItems.contains(sharedFileItem.getMsgId()))
			{
				selectedSharedFileItems.remove(sharedFileItem.getMsgId());
				if (selectedSharedFileItems.isEmpty())
				{
					destroyActionMode();
				}
				else
				{
					setMultiSelectTitle();
				}
			}
			else
			{
				selectedSharedFileItems.add(sharedFileItem.getMsgId());
				setMultiSelectTitle();
			}

			invalidateOptionsMenu();
			adapter.notifyDataSetChanged();
		}
		else
		{
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.OPEN_THUMBNAIL_VIA_GALLERY);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			ArrayList<HikeSharedFile> sharedMediaItems = new ArrayList<HikeSharedFile>(sharedFilesList.size());
			sharedMediaItems.addAll(sharedFilesList);
			Collections.reverse(sharedMediaItems);
			PhotoViewerFragment.openPhoto(R.id.parent_layout, HikeSharedFilesActivity.this, sharedMediaItems, 
					false, sharedMediaItems.size()-position-1, msisdn, conversationName, isGroup, msisdnArray, nameArray);
		}
	}

	private void setMultiSelectTitle()
	{
		if (multiSelectTitle == null)
		{
			return;
		}
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, selectedSharedFileItems.size()));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (multiSelectMode)
		{
			onActionModeItemClicked(item);
		}
		return super.onOptionsItemSelected(item);
	}

	public boolean onActionModeItemClicked(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.delete_msgs:
			HikeDialogFactory.showDialog(HikeSharedFilesActivity.this, HikeDialogFactory.DELETE_FILES_DIALOG, new HikeDialogListener()
			{
				
				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					ArrayList<Long> msgIds = new ArrayList<Long>(selectedSharedFileItems);
					Bundle bundle = new Bundle();
					bundle.putString(HikeConstants.Extras.MSISDN, msisdn);
					bundle.putInt(HikeConstants.Extras.DELETED_MESSAGE_TYPE, HikeConstants.SHARED_MEDIA_TYPE);
					HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_MESSAGE, new Pair<ArrayList<Long>, Bundle>(msgIds, bundle));
					Iterator<HikeSharedFile> iterator= sharedFilesList.iterator();
					while (iterator.hasNext())
					{
						HikeSharedFile hsf = iterator.next();
						if(selectedSharedFileItems.contains(hsf.getMsgId()))
						{
							// if delete media from phone is checked
							if(((CustomAlertDialog) hikeDialog).isChecked() && hsf.exactFilePathFileExists())
							{
								hsf.delete(getApplicationContext());
							}
							iterator.remove();
						}
					}
					
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
					
				}
			}, selectedSharedFileItems.size());
			
			return true;
		case R.id.forward_msgs:
			ArrayList<Long> selectedMsgIds = new ArrayList<Long>(selectedSharedFileItems);
			Collections.sort(selectedMsgIds);
			Intent intent = new Intent(HikeSharedFilesActivity.this, ComposeChatActivity.class);
			intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
			JSONArray multipleMsgArray = new JSONArray();
			try
			{
				Iterator<HikeSharedFile> iterator= sharedFilesList.iterator();
				while (iterator.hasNext())
				{
					HikeSharedFile hikeFile = iterator.next();
					if(selectedSharedFileItems.contains(hikeFile.getMsgId()))
					{
						JSONObject multiMsgFwdObject = new JSONObject();
						multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_KEY, hikeFile.getFileKey());

						multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_PATH, hikeFile.getFilePath());
						multiMsgFwdObject.putOpt(HikeConstants.Extras.FILE_TYPE, hikeFile.getFileTypeString());
						if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
						{
							multiMsgFwdObject.putOpt(HikeConstants.Extras.RECORDING_TIME, hikeFile.getRecordingDuration());
						}

						multipleMsgArray.put(multiMsgFwdObject);
					}
				}
			}
			catch (JSONException e)
			{
				Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
			}
			intent.putExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT, multipleMsgArray.toString());
			intent.putExtra(HikeConstants.Extras.PREV_MSISDN, msisdn);
			startActivity(intent);
			return true;
		case R.id.share_msgs:
			if (selectedSharedFileItems.size() == 1)
			{
				Iterator<HikeSharedFile> iterator= sharedFilesList.iterator();
				long msgId = selectedSharedFileItems.iterator().next();
				while (iterator.hasNext())
				{
					HikeSharedFile hikeSharedFile = iterator.next();
					if(msgId == hikeSharedFile.getMsgId())
					{
						hikeSharedFile.shareFile(HikeSharedFilesActivity.this);
					}
				}
				destroyActionMode();
			}
			else
			{
				Toast.makeText(HikeSharedFilesActivity.this, "Some error occured!", Toast.LENGTH_SHORT).show();
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void onEventReceived(String type, Object object)
	{
		 if (HikePubSub.HIKE_SHARED_FILE_DELETED.equals(type))
			{
				if(!(object instanceof HikeSharedFile))
				{
					return;
				}
				final HikeSharedFile hikeSharedFile = (HikeSharedFile) object;
				runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						sharedFilesList.remove(hikeSharedFile);
						adapter.notifyDataSetChanged();
					}
				});
			}

		super.onEventReceived(type, object);
	}
	
	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if(adapter != null)
		{
			adapter.getSharedFileImageLoader().setExitTasksEarly(true);
		}
	}
	
	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		if(adapter != null)
		{
			adapter.getSharedFileImageLoader().setExitTasksEarly(false);
			adapter.notifyDataSetChanged();
		}
	}
	
	@Override
	protected void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}
	
	public static Intent getHikeSharedFilesActivityIntent(Context context, Conversation conversation)
	{
		Pair<String[], String[]> msisdnAndNameArrays = Utils.getMsisdnToNameArray(conversation);
		return getHikeSharedFilesActivityIntent(context, conversation instanceof OneToNConversation, conversation.getLabel(), 
				msisdnAndNameArrays.first, msisdnAndNameArrays.second, conversation.getMsisdn());
	}
	/**
	 * used to open gallery from a 1:1 conversation
	 */
	
	public static Intent getHikeSharedFilesActivityIntent(Context context, String conversationName, String msisdn)
	{
		return getHikeSharedFilesActivityIntent(context, false, conversationName, null, null, msisdn);
	}
	public static Intent getHikeSharedFilesActivityIntent(Context context, boolean isGroup, String conversationName, String[] msisdnArray, String[] nameArray, String msisdn)
	{
		Intent intent = new Intent(context, HikeSharedFilesActivity.class);
		intent.putExtra(HikeConstants.Extras.IS_GROUP_CONVERSATION, isGroup);
		intent.putExtra(HikeConstants.Extras.CONVERSATION_NAME, conversationName);
		if (isGroup)
		{
			intent.putExtra(HikeConstants.Extras.PARTICIPANT_MSISDN_ARRAY, msisdnArray);
			intent.putExtra(HikeConstants.Extras.PARTICIPANT_NAME_ARRAY, nameArray);
		}
		intent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putAll(getIntent().getExtras());
		if(multiSelectMode){
			long[] selectedValues = new long[selectedSharedFileItems.size()];
			int i=0;
			for(Long id : selectedSharedFileItems){
				selectedValues[i++] = id;
			}
			outState.putLongArray(MULTISELECT_KEYS, selectedValues);
			outState.putBoolean(MULTISELECT_MODE, true);
		}
		super.onSaveInstanceState(outState);
	}
	public boolean removeFragment(String tag)
	{
		boolean isRemoved = super.removeFragment(tag);
		if (isRemoved)
		{
			getSupportActionBar().show();
			setupActionBar();
		}
		return isRemoved;
	}
}
