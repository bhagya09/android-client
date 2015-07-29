package com.bsb.hike.timeline.view;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.Protip;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.timeline.ActionsDeserializer;
import com.bsb.hike.timeline.adapter.TimelineCardsAdapter;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.timeline.model.FeedDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.TimelineActions;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.etiennelawlor.quickreturn.library.enums.QuickReturnViewType;
import com.etiennelawlor.quickreturn.library.listeners.QuickReturnRecyclerViewOnScrollListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class UpdatesFragment extends SherlockFragment implements Listener, OnClickListener
{

	private StatusMessage noStatusMessage;
	
	private StatusMessage ftueStatusMessage;

	private TimelineCardsAdapter timelineCardsAdapter;

	private String userMsisdn;

	private SharedPreferences prefs;

	private List<StatusMessage> statusMessages;

	private String[] pubSubListeners = { HikePubSub.TIMELINE_UPDATE_RECIEVED, HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED,
			HikePubSub.PROTIP_ADDED, HikePubSub.ICON_CHANGED, HikePubSub.ACTIVITY_UPDATE, HikePubSub.TIMELINE_WIPE };

	private String[] friendMsisdns;

	private RecyclerView mUpdatesList;

	private LinearLayoutManager mLayoutManager;

	private View actionsView;
	
	private TimelineActions actionsData = new TimelineActions();

	private Gson gson;
	
	private List<String> mFtueFriendList;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.updates, null);
		mUpdatesList = (RecyclerView) parent.findViewById(R.id.updatesRecycleView);
		actionsView = parent.findViewById(R.id.new_update_tab); 
		mLayoutManager = new LinearLayoutManager(getActivity());
		mUpdatesList.setLayoutManager(mLayoutManager);
		
		// TODO
		// mUpdatesList.setEmptyView(parent.findViewById(android.R.id.empty));
		return parent;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		prefs = getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		
		GsonBuilder gsonBuilder = new GsonBuilder();
	    gsonBuilder.registerTypeAdapter(TimelineActions.class, new ActionsDeserializer());
	    gson = gsonBuilder.create();

		userMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, "");

		statusMessages = new ArrayList<StatusMessage>();
		
		mFtueFriendList = new ArrayList<String>();

		timelineCardsAdapter = new TimelineCardsAdapter(getActivity(), statusMessages, userMsisdn, mFtueFriendList, getLoaderManager(), getSherlockActivity().getSupportFragmentManager());
		timelineCardsAdapter.setActionsData(actionsData);

		mUpdatesList.setAdapter(timelineCardsAdapter);

		QuickReturnRecyclerViewOnScrollListener scrollListener = new QuickReturnRecyclerViewOnScrollListener.Builder(QuickReturnViewType.HEADER).header(actionsView)
				.minHeaderTranslation(-1 * HikePhotosUtils.dpToPx(45)).isSnappable(false).build();

		mUpdatesList.setOnScrollListener(scrollListener);
		
		actionsView.findViewById(R.id.new_photo_tab).setOnClickListener(this);
		
		actionsView.findViewById(R.id.new_status_tab).setOnClickListener(this);

		FetchUpdates fetchUpdates = new FetchUpdates();

		if (Utils.isHoneycombOrHigher())
		{
			fetchUpdates.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		else
		{
			fetchUpdates.execute();
		}
	}

	@Override
	public void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onEventReceived(String type, final Object object)
	{

		if (!isAdded())
		{
			return;
		}

		if (HikePubSub.TIMELINE_UPDATE_RECIEVED.equals(type))
		{
			final StatusMessage statusMessage = (StatusMessage) object;
			final int startIndex = getStartIndex();

			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					statusMessages.add(startIndex, statusMessage);
					timelineCardsAdapter.notifyDataSetChanged();
				}
			});
			HikeMessengerApp.getPubSub().publish(HikePubSub.RESET_NOTIFICATION_COUNTER, null);
		}
		else if (HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED.equals(type))
		{
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					timelineCardsAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.PROTIP_ADDED.equals(type))
		{
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					addProtip((Protip) object);
					timelineCardsAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.ICON_CHANGED.equals(type))
		{
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					timelineCardsAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.ACTIVITY_UPDATE.equals(type))
		{
			if (object != null && object instanceof FeedDataModel)
			{
				FeedDataModel feedData = (FeedDataModel) object;
				actionsData.updateByActivityFeed(feedData);
				notifyVisibleItems();
			}
		}
		else if (HikePubSub.TIMELINE_WIPE.equals(type))
		{
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					statusMessages.clear();
					timelineCardsAdapter.notifyDataSetChanged();
				}
			});
		}
	}
	
	private int getStartIndex()
	{
		int startIndex = 0;
		if (ftueStatusMessage != null)
		{
			startIndex++;
		}
		return startIndex;
	}

	private boolean shouldAddFTUEItem()
	{
		return false;
		/*if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ENABLE_TIMELINE_FTUE, true))
		{
			return true;
		}
		else
		{
			ftueStatusMessage = null;
			return false;
		}*/
	}

	private void addFTUEItem()
	{
		int counter = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TIMELINE_FTUE_CARD_SHOWN_COUNTER, 0);
		int cardCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TIMELINE_FTUE_TOTAL_CARD_COUNTER, 4);
		ContactInfo contact = null;
		if(counter == 0)
		{
			//To SHOW BASIC CARD
			ftueStatusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_INIT, null, null, null, null, null, 0);
			statusMessages.add(0, ftueStatusMessage);
			return;
		}
		else if(counter <= cardCount)
		{
			//To SHOW Fav Card
			contact = ContactManager.getInstance().getContact(mFtueFriendList.get(counter - 1));
			ftueStatusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_FAV, null, contact.getMsisdn(), contact.getName(), null, null, 0);
			statusMessages.add(0, ftueStatusMessage);
			return;
		}
		else if(counter == cardCount + 1)
		{
			//To SHOW Exit Card
			ftueStatusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_EXIT, null, null, null, null, null, 0);
			statusMessages.add(0, ftueStatusMessage);
			return;
		}
	}

	private class FetchUpdates extends AsyncTask<Void, Void, List<StatusMessage>>
	{

		@Override
		protected List<StatusMessage> doInBackground(Void... params)
		{
		
			List<ContactInfo> friendsList = ContactManager.getInstance().getContactsOfFavoriteType(FavoriteType.FRIEND, HikeConstants.BOTH_VALUE, userMsisdn);

			ArrayList<String> msisdnList = new ArrayList<String>();

			for (ContactInfo contactInfo : friendsList)
			{
				if (TextUtils.isEmpty(contactInfo.getMsisdn()))
				{
					continue;
				}
				msisdnList.add(contactInfo.getMsisdn());
			}
			msisdnList.add(userMsisdn);

			friendMsisdns = new String[msisdnList.size()];
			msisdnList.toArray(friendMsisdns);
			List<StatusMessage> statusMessages = HikeConversationsDatabase.getInstance().getStatusMessages(true, HikeConstants.MAX_STATUSES_TO_LOAD_INITIALLY, -1, friendMsisdns);

			return statusMessages;
		}

		@Override
		protected void onPostExecute(List<StatusMessage> result)
		{
			if (!isAdded() || result == null)
			{
				Logger.d(getClass().getSimpleName(), "Not added or result null");
				return;
			}

			final ArrayList<String> suIDList = new ArrayList<String>();
			
			for(StatusMessage suMessage: result)
			{
				if (!TextUtils.isEmpty(suMessage.getMappedId()))
				{
					suIDList.add(suMessage.getMappedId());
				}
			}
			
			if(!suIDList.isEmpty())
			{
				// Get actions for SU from HTTP
				JSONArray suIDArray = new JSONArray(suIDList);
				JSONObject suUpdateJSON = new JSONObject();
				try
				{
					suUpdateJSON.put(HikeConstants.SU_ID_LIST, suIDArray);
					RequestToken requestToken = HttpRequests.getActionUpdates(suUpdateJSON, actionUpdatesReqListener);
					requestToken.execute();
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				// Get actions for SU from DB
				HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
				{
					@Override
					public void run()
					{
						HikeConversationsDatabase.getInstance().getActionsData(ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(),suIDList, actionsData);
						timelineCardsAdapter.setActionsData(actionsData);
						notifyVisibleItems();
					}
				}, 0);
			}
			
			/*
			 * If we already have a few status messages in the timeline, no need to prompt the user to post his/her own message.
			 */
			/*if (result.size() < HikeConstants.MIN_STATUS_COUNT)
			{
				if (TextUtils.isEmpty(lastStatus))
				{
					noStatusMessage = new StatusMessage(TimelineCardsAdapter.EMPTY_STATUS_NO_STATUS_ID, null, "12345", getString(R.string.mood_update), getString(
							R.string.hey_name, name), StatusMessageType.NO_STATUS, System.currentTimeMillis() / 1000);
					statusMessages.add(0, noStatusMessage);
				}
				else if (result.isEmpty())
				{
					noStatusMessage = new StatusMessage(TimelineCardsAdapter.EMPTY_STATUS_NO_STATUS_RECENTLY_ID, null, "12345", getString(R.string.mood_update), getString(
							R.string.hey_name, name), StatusMessageType.NO_STATUS, System.currentTimeMillis() / 1000);
					statusMessages.add(0, noStatusMessage);
				}
			}*/

			long currentProtipId = prefs.getLong(HikeMessengerApp.CURRENT_PROTIP, -1);

			Protip protip = null;
			boolean showProtip = false;
			if (currentProtipId != -1)
			{
				showProtip = true;
				protip = HikeConversationsDatabase.getInstance().getProtipForId(currentProtipId);
			}

			if (showProtip && protip != null)
			{
				final int startIndex = getStartIndex();
				statusMessages.add(startIndex, new StatusMessage(protip));
				timelineCardsAdapter.setProtipIndex(startIndex);
			}

			statusMessages.addAll(result);
			Logger.d(getClass().getSimpleName(), "Updating...");
			
			// Get Fav users list from SharedPref for FTUE
			if(shouldAddFTUEItem())
			{
				HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
				{
					@Override
					public void run()
					{

						HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
						String mymsisdn = settings.getData(HikeMessengerApp.MSISDN_SETTING, "");
						mFtueFriendList = new ArrayList<String>(Utils.getServerRecommendedContactsSelection(settings.getData(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, null), mymsisdn));
						timelineCardsAdapter.setFTUEFriendList(mFtueFriendList);
						addFTUEItem();
						notifyVisibleItems();
					}
				}, 0);
			}
			
			HikeMessengerApp.getPubSub().addListeners(UpdatesFragment.this, pubSubListeners);
		}

	}
	
	private IRequestListener actionUpdatesReqListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			final JSONObject response = (JSONObject) result.getBody().getContent();
			
			if (Utils.isResponseValid(response))
			{
				actionsData = gson.fromJson(response.toString(), TimelineActions.class);

				timelineCardsAdapter.setActionsData(actionsData);
				
				notifyVisibleItems();
				
				HikeConversationsDatabase.getInstance().updateActionsData(actionsData,ActivityObjectTypes.STATUS_UPDATE);
			}
		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
			// Do nothing
		}

		@Override
		public void onRequestFailure(HttpException httpException)
		{
			// Do nothing
		}
	};

	private void addProtip(Protip protip)
	{
		if (protip != null)
		{
			final int startIndex = getStartIndex();
			statusMessages.add(getStartIndex(), new StatusMessage(protip));
			timelineCardsAdapter.setProtipIndex(startIndex);
		}
	}

	@Override
	public void onClick(View arg0)
	{
		switch (arg0.getId())
		{
		case R.id.new_photo_tab:
			int galleryFlags = GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS | GalleryActivity.GALLERY_EDIT_SELECTED_IMAGE | GalleryActivity.GALLERY_COMPRESS_EDITED_IMAGE
					| GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM;
			Intent galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(getActivity(), galleryFlags, null);
			startActivity(galleryPickerIntent);
			break;

		case R.id.new_status_tab:
			startActivity(IntentFactory.getPostStatusUpdateIntent(getActivity(), null));
			break;

		default:
			break;
		}
	}
	
	public void notifyVisibleItems()
	{
		if (UpdatesFragment.this.isAdded())
		{
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					timelineCardsAdapter.notifyDataSetChanged();
				}
			});
		}
	}
	

}
