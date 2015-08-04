package com.bsb.hike.timeline.view;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

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

public class UpdatesFragment extends Fragment implements Listener, OnClickListener
{

	private TimelineCardsAdapter timelineCardsAdapter;

	private String userMsisdn;

	private SharedPreferences prefs;

	private List<StatusMessage> statusMessages;

	private String[] pubSubListeners = { HikePubSub.TIMELINE_UPDATE_RECIEVED, HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, HikePubSub.PROTIP_ADDED, HikePubSub.ICON_CHANGED,
			HikePubSub.ACTIVITY_UPDATE, HikePubSub.TIMELINE_WIPE, HikePubSub.TIMELINE_FTUE_LIST_UPDATE };

	private String[] friendMsisdns;

	private RecyclerView mUpdatesList;

	private LinearLayoutManager mLayoutManager;

	private View actionsView;

	private TimelineActions actionsData = new TimelineActions();

	private Gson gson;

	private List<ContactInfo> mFtueFriendList;

	/**
	 * When packet is not received, in this case , this tell how many referred contacts have to be shown which we received from postAddressBook
	 */
	private final int MAX_CONTCATS_ALLOWED_TO_SHOW_INITIALLY = 4;

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

		mFtueFriendList = new ArrayList<ContactInfo>();

		timelineCardsAdapter = new TimelineCardsAdapter(getActivity(), statusMessages, userMsisdn, mFtueFriendList, getLoaderManager(), getActivity().getSupportFragmentManager());
		timelineCardsAdapter.setActionsData(actionsData);

		mUpdatesList.setAdapter(timelineCardsAdapter);

		QuickReturnRecyclerViewOnScrollListener scrollListener = new QuickReturnRecyclerViewOnScrollListener.Builder(QuickReturnViewType.HEADER).header(actionsView)
				.minHeaderTranslation(-1 * HikePhotosUtils.dpToPx(50)).isSnappable(false).build();

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
		else if (HikePubSub.TIMELINE_FTUE_LIST_UPDATE.equals(type))
		{
			if (shouldAddFTUEItem())
			{
				HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
				{
					@Override
					public void run()
					{
						if (!isAdded())
						{
							return;
						}

						int shownCounter = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TIMELINE_FTUE_CARD_SHOWN_COUNTER, 0);
						if (shownCounter != 0)
						{
							ContactInfo currentCardContact = mFtueFriendList.get(shownCounter - 1);
							mFtueFriendList = new ArrayList<ContactInfo>();
							mFtueFriendList.addAll(getFtueFriendList());
							mFtueFriendList.add(0, currentCardContact);
						}
						else
						{
							mFtueFriendList = getFtueFriendList();
						}
						timelineCardsAdapter.setFTUEFriendList(mFtueFriendList);
						addFTUEItem();
						notifyVisibleItems();
					}

				}, 0);
			}
		}
	}

	private int getStartIndex()
	{
		int startIndex = 0;
		if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ENABLE_TIMELINE_FTUE, true))
		{
			startIndex++;
		}
		return startIndex;
	}

	private boolean shouldAddFTUEItem()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ENABLE_TIMELINE_FTUE, true);
	}

	private void addFTUEItem()
	{
		int counter = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TIMELINE_FTUE_CARD_SHOWN_COUNTER, 0);
		int cardCount = mFtueFriendList.size();

		// If no msisdn to show, then no need to show FTUE
		if (mFtueFriendList.isEmpty())
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_TIMELINE_FTUE, false);

			// counter = 0, means not shown init card so no need to change
			// counter != 0, means init card is shown so do it 1
			if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TIMELINE_FTUE_CARD_SHOWN_COUNTER, 0) != 0)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.TIMELINE_FTUE_CARD_SHOWN_COUNTER, 1);
			}
			timelineCardsAdapter.removeAllFTUEItems();
			return;
		}
		StatusMessage ftueStatusMessage = null;
		ContactInfo contact = null;
		if (counter == 0)
		{
			// To SHOW BASIC CARD
			ftueStatusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_INIT, null, null, null, null, null, 0);
			statusMessages.add(0, ftueStatusMessage);
			return;
		}
		else if (counter <= cardCount)
		{
			// To SHOW Fav Card
			contact = mFtueFriendList.get(counter - 1);
			ftueStatusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_FAV, null, contact.getMsisdn(), contact.getName(), null, null, 0);
			statusMessages.add(0, ftueStatusMessage);
			return;
		}
		else if (counter == cardCount + 1)
		{
			// To SHOW Exit Card
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

			for (StatusMessage suMessage : result)
			{
				if (!TextUtils.isEmpty(suMessage.getMappedId()))
				{
					suIDList.add(suMessage.getMappedId());
				}
			}

			if (!suIDList.isEmpty())
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
						HikeConversationsDatabase.getInstance().getActionsData(ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), suIDList, actionsData);
						timelineCardsAdapter.setActionsData(actionsData);
						notifyVisibleItems();
					}
				}, 0);
			}

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
			if (shouldAddFTUEItem())
			{
				HikeHandlerUtil.getInstance().postRunnableWithDelay(new Runnable()
				{
					@Override
					public void run()
					{
						mFtueFriendList = getFtueFriendList();
						if (mFtueFriendList != null)
						{
							timelineCardsAdapter.setFTUEFriendList(mFtueFriendList);
							addFTUEItem();
						}
						notifyVisibleItems();
					}

				}, 0);
			}

			HikeMessengerApp.getPubSub().addListeners(UpdatesFragment.this, pubSubListeners);
		}

	}

	/**
	 * returns FTUE list to be shown in Timeline FTUE
	 * 
	 * case A) Got List from msisdn's received from FTUE packet, get 'N' non frnd Contacts 'N' received in packet case B) List from msisdn's received from FTUE packet is not there
	 * now go for fetching max 4 msisdn from List reveived on sign up
	 * 
	 * @return
	 */
	private List<ContactInfo> getFtueFriendList()
	{
		HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
		Set<String> msisdnSet = settings.getStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, null);
		List<ContactInfo> finalContactLsit = null;
		int finalCount = HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.TIMELINE_FTUE_CARD_TO_SHOW_COUNTER, 4);
		boolean isFromSignUpList = false;
		boolean isFromPacketList = false;
		if (msisdnSet == null)
		{
			String mymsisdn = settings.getData(HikeMessengerApp.MSISDN_SETTING, "");
			String list = settings.getData(HikeMessengerApp.SERVER_RECOMMENDED_CONTACTS, null);
			msisdnSet = Utils.getServerRecommendedContactsSelection(list, mymsisdn);
			isFromSignUpList = true;
		}
		else
		{
			isFromPacketList = true;
		}
		if (msisdnSet != null)
		{
			int counter = 0;
			Iterator<String> iterator = msisdnSet.iterator();
			finalContactLsit = new ArrayList<ContactInfo>();
			while (iterator.hasNext())
			{
				String id = iterator.next();
				ContactInfo c = ContactManager.getInstance().getContact(id, true, true);
				if (c.getFavoriteType().equals(FavoriteType.NOT_FRIEND))
				{
					finalContactLsit.add(c);
					counter++;
				}
				if (isFromSignUpList && counter == MAX_CONTCATS_ALLOWED_TO_SHOW_INITIALLY)
				{
					break;
				}
				else if (isFromPacketList && counter == finalCount)
				{
					break;
				}
			}
		}
		else
		{
			Logger.d(UpdatesFragment.class.getName(), "Both list are empty, so no FTUE");
		}

		if (finalContactLsit == null)
		{
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_TIMELINE_FTUE, false);
		}
		return finalContactLsit;
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

				HikeConversationsDatabase.getInstance().updateActionsData(actionsData, ActivityObjectTypes.STATUS_UPDATE);
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
