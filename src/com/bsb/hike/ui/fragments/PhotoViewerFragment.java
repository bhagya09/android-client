package com.bsb.hike.ui.fragments;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.LruCache;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.SharedMediaAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.dialog.CustomAlertDialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.dialog.HikeDialogListener;
import com.bsb.hike.media.SharedMediaCursorIterator;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.HikeSharedFile;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.GroupConversation;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.HikeSharedFilesActivity;
import com.bsb.hike.ui.utils.DepthPageTransformer;
import com.bsb.hike.utils.HikeUiHandler;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class PhotoViewerFragment extends SherlockFragment implements OnPageChangeListener
{
	private View mParent;

	private SharedMediaAdapter smAdapter;

	private ViewPager selectedPager;

	private ArrayList<HikeSharedFile> sharedMediaItems;

	private int numColumns;

	private int actualSize;

	private int sizeOfImage;

	private final static int UNSPECIFIED_INIT_POS = -1;

	private int initialPosition = UNSPECIFIED_INIT_POS;

	private String msisdn;

	private Map<String, String> msisdnToNameMap;

	private TextView senderName;

	private TextView itemTimeStamp;

	private boolean isGroup = false;

	private String conversationName;

	String[] msisdnArray;

	String[] nameArray;

	private ImageView galleryButton;

	private boolean isEditEnabled;

	private Menu menu;

	private boolean latestFirst;

	private Cursor smCursor;

	private int mPageSelected;

	private SharedMediaCursorIterator smIterator;

	private static final String LATEST_FIRST = "LATEST_FIRST";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mParent = inflater.inflate(R.layout.shared_media_viewer, null);

		initializeViews(mParent);

		readArguments();

		if (savedInstanceState != null)
		{
			initialPosition = savedInstanceState.getInt(HikeConstants.Extras.CURRENT_POSITION, UNSPECIFIED_INIT_POS);
		}

		isEditEnabled = Utils.isPhotosEditEnabled();

		return mParent;
	}

	private void initializeViews(View parent)
	{
		selectedPager = (ViewPager) parent.findViewById(R.id.selection_pager);
		senderName = (TextView) parent.findViewById(R.id.sender_name);
		itemTimeStamp = (TextView) parent.findViewById(R.id.item_time_stamp);
		galleryButton = (ImageView) parent.findViewById(R.id.gallary_button);
	}

	private void readArguments()
	{
		sharedMediaItems = getArguments().getParcelableArrayList(HikeConstants.Extras.SHARED_FILE_ITEMS);
		initialPosition = getArguments().getInt(HikeConstants.MEDIA_POSITION, UNSPECIFIED_INIT_POS);
		msisdn = getArguments().getString(HikeConstants.Extras.MSISDN);
		isGroup = getArguments().getBoolean(HikeConstants.Extras.IS_GROUP_CONVERSATION, false);
		conversationName = getArguments().getString(HikeConstants.Extras.CONVERSATION_NAME);
		latestFirst = getArguments().getBoolean(LATEST_FIRST);

		if (isGroup)
		{
			msisdnArray = getArguments().getStringArray(HikeConstants.Extras.PARTICIPANT_MSISDN_ARRAY);
			nameArray = getArguments().getStringArray(HikeConstants.Extras.PARTICIPANT_NAME_ARRAY);
			msisdnToNameMap = new HashMap<String, String>(msisdnArray.length);
			for (int i = 0; i < msisdnArray.length; i++)
			{
				msisdnToNameMap.put(msisdnArray[i], nameArray[i]);
			}
		}
	}

	private void intialiazeViewPager()
	{
		int screenWidth = getResources().getDisplayMetrics().widthPixels;
		int screenHeight = getResources().getDisplayMetrics().heightPixels;
		sizeOfImage = screenWidth < screenHeight ? screenWidth : screenHeight;
		numColumns = Utils.getNumColumnsForGallery(getResources(), sizeOfImage);
		actualSize = Utils.getActualSizeForGallery(getResources(), sizeOfImage, numColumns);

		smCursor = HikeConversationsDatabase.getInstance().getSharedMedia(msisdn, true, latestFirst);
		smIterator = new SharedMediaCursorIterator(smCursor, msisdn);
		smAdapter = new SharedMediaAdapter(getActivity(), actualSize, smCursor, msisdn, selectedPager, this,smIterator);

		selectedPager.setAdapter(smAdapter);
		selectedPager.setOnPageChangeListener(this);

		selectedPager.setPageTransformer(false, new ViewPager.PageTransformer()
		{
			// Adding some sleek animations on transforming pages.
			@Override
			public void transformPage(View page, float position)
			{
				final float normalizedposition = Math.abs(Math.abs(position) - 1);
				page.setAlpha(normalizedposition);
				page.setScaleX(normalizedposition / 2 + 0.5f);
				page.setScaleY(normalizedposition / 2 + 0.5f);
			}
		});

		// The method setPageTransformer works only on API 11+. For lower devices, we can add margin to the view pager to show gap between adjacent
		// views.
		if (Utils.isHoneycombOrHigher())
		{
			selectedPager.setPageTransformer(true, new DepthPageTransformer());
		}
		else
		{
			selectedPager.setPageMargin((int) getResources().getDimension(R.dimen.horizontal_page_margin));
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		/*
		 * We post execute setupActionBar in ChatThread; So to handle action bar of media viewer on rotation we need to do its action bar setup after activity is created and post
		 * UI this runnable
		 */
		(new Handler()).post(new Runnable()
		{

			@Override
			public void run()
			{
				setupActionBar();
			}
		});
		
		intialiazeViewPager();
		
		setAdapterSelectedPos();


		galleryButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				startActivity(HikeSharedFilesActivity.getHikeSharedFilesActivityIntent(getSherlockActivity(), isGroup, conversationName, msisdnArray, nameArray, msisdn));
			}
		});

		setHasOptionsMenu(true);
		
		super.onActivityCreated(savedInstanceState);
	}

	private void setAdapterSelectedPos()
	{
		if (initialPosition != UNSPECIFIED_INIT_POS)
		{
			if (latestFirst)
			{
				setSelection(smAdapter.getCount() - initialPosition - 1);
			}
			else
			{
				setSelection(initialPosition);
			}
		}
		else
		{
			if (!sharedMediaItems.isEmpty())
			{
				long msgId = sharedMediaItems.get(0).getMsgId();
				if (msgId != 0)
				{
					setSelection(getRowFromMsgID(msgId));
				}
			}
		}
	}

	private int getRowFromMsgID(long msgId)
	{
		int start = 0;
		
		int end = smCursor.getCount();
		
		int msgIdIndex = smCursor.getColumnIndex(BaseColumns._ID);
		
		//Binary since msgId is sorted
		while (end > start)
		{
			int mid = end - start == 0 ? end : start + ((end - start) / 2);

			if (smCursor.moveToPosition(mid))
			{
				long result = smCursor.getLong(msgIdIndex);
				if (result == msgId)
				{
					return mid;
				}
				else
				{
					if (result < msgId)
					{
						if (latestFirst)
						{
							end = mid;
						}
						else
						{
							start = mid;
						}
					}
					else
					{
						if (latestFirst)
						{
							start = mid;
						}
						else
						{
							end = mid;
						}
					}
				}
			}
			else
			{
				break;
			}
		}
		return 0;
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if (smAdapter != null)
		{
			smAdapter.getSharedFileImageLoader().setExitTasksEarly(true);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(HikeConstants.Extras.CURRENT_POSITION, selectedPager.getCurrentItem());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onPageScrollStateChanged(int arg0)
	{
		// Do nothing
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2)
	{
		// Do nothing
	}

	@Override
	public void onPageSelected(int position)
	{
		 mPageSelected = position;
		 
		 setSenderDetails();
		 
		if (menu != null && getCurrentSelectedItem() != null)
		{
			if (isEditEnabled && getCurrentSelectedItem().getHikeFileType().compareTo(HikeFileType.IMAGE) == 0)
			{
				menu.findItem(R.id.edit_pic).setVisible(true);
			}
			else
			{
				menu.findItem(R.id.edit_pic).setVisible(false);
			}
		}
	}

	private void setSenderDetails()
	{
		if (getCurrentSelectedItem() != null)
		{
			senderName.setText(getSenderName());
			long timeStamp = getCurrentSelectedItem().getTimeStamp();
			String date = Utils.getFormattedDate(getSherlockActivity(), timeStamp);
			String time = Utils.getFormattedTime(false, getSherlockActivity(), timeStamp);
			itemTimeStamp.setText(date + ", " + time);
		}
	}

	private String getSenderName()
	{
		HikeSharedFile hsf = getCurrentSelectedItem();
		if (hsf.isSent())
		{
			return getString(R.string.you);
		}
		else if (isGroup)
		{
			return msisdnToNameMap.get(hsf.getGroupParticipantMsisdn());
		}
		else
		{
			return conversationName;
		}
	}

	private void setSelection(int position)
	{
		selectedPager.setCurrentItem(position, false);
	}

	public void setupActionBar()
	{
		if (getSherlockActivity() == null)
		{
			return;
		}
		/*
		 * else part
		 */
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = getSherlockActivity().getLayoutInflater().inflate(R.layout.compose_action_bar, null);
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_header_photo_viewer));

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(isGroup ? conversationName : Utils.getFirstName(conversationName));

		TextView subText = (TextView) actionBarView.findViewById(R.id.subtext);
		subText.setVisibility(View.GONE);

		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	private void finish()
	{
		getSherlockActivity().onBackPressed();
	}

	public static void openPhoto(int resId, Context context, ArrayList<HikeSharedFile> hikeSharedFiles, boolean fromChatThread, Conversation conversation, boolean latestFirst)
	{
		Pair<String[], String[]> msisdnAndNameArrays = Utils.getMsisdnToNameArray(conversation);
		openPhoto(resId, context, hikeSharedFiles, fromChatThread, hikeSharedFiles.size() - 1, conversation.getMsisdn(), conversation.getLabel(),
				conversation instanceof GroupConversation, msisdnAndNameArrays.first, msisdnAndNameArrays.second, latestFirst);
	}

	public static void openPhoto(int resId, Context context, ArrayList<HikeSharedFile> hikeSharedFiles, boolean fromChatThread, int mediaPosition, String fromMsisdn,
			String convName, boolean isGroup, String[] msisdnArray, String[] nameArray, boolean latestFirst)
	{
		PhotoViewerFragment photoViewerFragment = new PhotoViewerFragment();
		Bundle arguments = new Bundle();
		arguments.putInt(HikeConstants.MEDIA_POSITION, hikeSharedFiles.size() == 1 ? UNSPECIFIED_INIT_POS : mediaPosition);
		arguments.putBoolean(HikeConstants.FROM_CHAT_THREAD, fromChatThread);
		arguments.putString(HikeConstants.Extras.MSISDN, fromMsisdn);
		arguments.putString(HikeConstants.Extras.CONVERSATION_NAME, convName);
		arguments.putParcelableArrayList(HikeConstants.Extras.SHARED_FILE_ITEMS, hikeSharedFiles);
		arguments.putBoolean(HikeConstants.Extras.IS_GROUP_CONVERSATION, isGroup);
		arguments.putBoolean(LATEST_FIRST, latestFirst);
		if (isGroup)
		{
			arguments.putStringArray(HikeConstants.Extras.PARTICIPANT_MSISDN_ARRAY, msisdnArray);
			arguments.putStringArray(HikeConstants.Extras.PARTICIPANT_NAME_ARRAY, nameArray);
		}

		photoViewerFragment.setArguments(arguments);

		FragmentTransaction fragmentTransaction = ((FragmentActivity) context).getSupportFragmentManager().beginTransaction();
		fragmentTransaction.add(resId, photoViewerFragment, HikeConstants.IMAGE_FRAGMENT_TAG);
		fragmentTransaction.commitAllowingStateLoss();

	}

	/**
	 * used to open photo/video from a 1:1 conversation
	 */
	public static void openPhoto(int resId, Context context, ArrayList<HikeSharedFile> hikeSharedFiles, boolean fromChatThread, int mediaPosition, String fromMsisdn,
			String convName, boolean latestFirst)
	{
		openPhoto(resId, context, hikeSharedFiles, fromChatThread, mediaPosition, fromMsisdn, convName, false, null, null, latestFirst);
	}

	LruCache<Integer, HikeSharedFile> hsfLru = new LruCache<Integer, HikeSharedFile>(3)
	{
		protected int sizeOf(Integer key, HikeSharedFile value)
		{
			return 1;
		};
	};

	private HikeSharedFile getCurrentSelectedItem()
	{
		HikeSharedFile currentFile = hsfLru.get(mPageSelected);
		if (currentFile == null)
		{
			currentFile = smIterator.getFromCursor(smCursor, mPageSelected);
			if (currentFile == null)
			{
				return null;
			}
			else
			{
				hsfLru.put(mPageSelected, currentFile);
			}
		}
		return currentFile;
	}
	
	@Override
	public void onDetach()
	{
		super.onDetach();
		hsfLru.evictAll();
		if (!smCursor.isClosed())
		{
			smCursor.close();
		}
	}

	public void removeCurrentSelectedItem()
	{
		hsfLru.evictAll();
		HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
		{
			@Override
			public void run()
			{
				smCursor = HikeConversationsDatabase.getInstance().getSharedMedia(msisdn, true, latestFirst);

				getActivity().runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						if (isAdded())
						{
							if (smCursor.getCount() > 0)
							{
								smAdapter.changeCursor(smCursor);
							}
							else
							{
								finish();
							}
						}
					}
				});
			}
		}, 0);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		if (getCurrentSelectedItem() == null)
		{
			return false;
		}

		switch (item.getItemId())
		{
		// deletes current selected item from viewpager
		case R.id.delete_msgs:
			HikeDialogFactory.showDialog(getSherlockActivity(), HikeDialogFactory.DELETE_FILES_DIALOG, new HikeDialogListener()
			{

				@Override
				public void positiveClicked(HikeDialog hikeDialog)
				{
					HikeSharedFile itemToDelete = getCurrentSelectedItem();
					ArrayList<Long> msgIds = new ArrayList<Long>(1);
					msgIds.add(itemToDelete.getMsgId());

					Bundle bundle = new Bundle();
					bundle.putString(HikeConstants.Extras.MSISDN, msisdn);
					bundle.putInt(HikeConstants.Extras.DELETED_MESSAGE_TYPE, HikeConstants.SHARED_MEDIA_TYPE);
					HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_MESSAGE, new Pair<ArrayList<Long>, Bundle>(msgIds, bundle));

					// if delete media from phone is checked
					if (((CustomAlertDialog) hikeDialog).isChecked())
					{
						itemToDelete.delete(getActivity().getApplicationContext());
					}

					HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_SHARED_FILE_DELETED, itemToDelete);

					hikeDialog.dismiss();
					removeCurrentSelectedItem();
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
			}, 1); // 1 since we are deleting a single file

			return true;
		case R.id.forward_msgs:
			File selFile = getCurrentSelectedItem().getFile();
			if (selFile == null || !selFile.exists())
			{
				Toast.makeText(HikeMessengerApp.getInstance().getApplicationContext(), R.string.file_expire, Toast.LENGTH_SHORT).show();
				return false;
			}
			Intent intent = new Intent(getSherlockActivity(), ComposeChatActivity.class);
			intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
			JSONArray multipleMsgArray = new JSONArray();
			try
			{
				JSONObject multiMsgFwdObject = new JSONObject();
				Utils.handleFileForwardObject(multiMsgFwdObject, getCurrentSelectedItem());
				multipleMsgArray.put(multiMsgFwdObject);
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

			getCurrentSelectedItem().shareFile(getSherlockActivity());
			return true;
		case R.id.edit_pic:
			Intent editIntent = IntentFactory.getPictureEditorActivityIntent(getActivity(), getCurrentSelectedItem().getExactFilePath(), true, null, false);
			getActivity().startActivity(editIntent);
			return true;
		}
		return false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		menu.clear();
		inflater.inflate(R.menu.photo_viewer_wedit_option_menu, menu);
		this.menu = menu;
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onResume()
	{
		if (!getSherlockActivity().getSupportActionBar().isShowing())
		{
			toggleViewsVisibility();
		}
		if (smAdapter != null)
		{
			smAdapter.getSharedFileImageLoader().setExitTasksEarly(false);
			smAdapter.notifyDataSetChanged();
		}
		super.onResume();
	}

	public void toggleViewsVisibility()
	{
		if (getSherlockActivity() != null)
		{
			ActionBar actionbar = getSherlockActivity().getSupportActionBar();
			Animation animation;
			if (!actionbar.isShowing())
			{
				actionbar.show();
				animation = AnimationUtils.loadAnimation(getSherlockActivity(), R.anim.fade_in_animation);
			}
			else
			{
				actionbar.hide();
				animation = AnimationUtils.loadAnimation(getSherlockActivity(), R.anim.fade_out_animation);
			}
			animation.setDuration(300);
			animation.setFillAfter(true);
			mParent.findViewById(R.id.info_group).startAnimation(animation);
			mParent.findViewById(R.id.gradient).startAnimation(animation);
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		if (getCurrentSelectedItem() != null)
		{
			// Display edit button only if,
			// 1.Photos is enabled
			// 2.Media is of type image/*
			if (isEditEnabled && getCurrentSelectedItem().getHikeFileType().compareTo(HikeFileType.IMAGE) == 0)
			{
				menu.findItem(R.id.edit_pic).setVisible(true);
			}
			else
			{
				menu.findItem(R.id.edit_pic).setVisible(false);
			}
		}
	}
	
	@Override
	public void onDestroy()
	{
		// TODO Auto-generated method stub
		
		//To remove any callbacks, if present inside handler in adaptor
		if(smAdapter != null)
		{
			smAdapter.onDestroy();
		}
		
		super.onDestroy();
	}
}
