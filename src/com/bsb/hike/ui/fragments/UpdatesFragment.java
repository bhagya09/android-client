package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.CentralTimelineAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class UpdatesFragment extends SherlockListFragment implements OnScrollListener, Listener
{

	private StatusMessage noStatusMessage;

	private CentralTimelineAdapter centralTimelineAdapter;

	private String userMsisdn;

	private SharedPreferences prefs;

	private List<StatusMessage> statusMessages;

	private boolean reachedEnd;

	private boolean loadingMoreMessages;

	private String[] pubSubListeners = { HikePubSub.TIMELINE_UPDATE_RECIEVED, HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED,
			HikePubSub.PROTIP_ADDED, HikePubSub.ICON_CHANGED };

	private String[] friendMsisdns;

	private int previousFirstVisibleItem;

	private long previousEventTime;

	private int velocity;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.updates, null);

		ListView updatesList = (ListView) parent.findViewById(android.R.id.list);
		updatesList.setEmptyView(parent.findViewById(android.R.id.empty));

		return parent;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (centralTimelineAdapter != null)
		{
			centralTimelineAdapter.getTimelineImageLoader().setExitTasksEarly(false);
			centralTimelineAdapter.getIconImageLoader().setExitTasksEarly(false);
			centralTimelineAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if (centralTimelineAdapter != null)
		{
			centralTimelineAdapter.getTimelineImageLoader().setExitTasksEarly(true);
			centralTimelineAdapter.getIconImageLoader().setExitTasksEarly(true);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		prefs = getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		userMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, "");

		statusMessages = new ArrayList<StatusMessage>();

		centralTimelineAdapter = new CentralTimelineAdapter(getActivity(), statusMessages, userMsisdn);
		setListAdapter(centralTimelineAdapter);
		getListView().setOnScrollListener(this);

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
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		StatusMessage statusMessage = centralTimelineAdapter.getItem(position - getListView().getHeaderViewsCount());
		if (statusMessage.getId() == CentralTimelineAdapter.FTUE_ITEM_ID || (statusMessage.getStatusMessageType() == StatusMessageType.NO_STATUS)
				|| (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST) || (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP))
		{
			return;
		}
		else if (userMsisdn.equals(statusMessage.getMsisdn()))
		{
			Intent intent = new Intent(getActivity(), ProfileActivity.class);
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			startActivity(intent);
			return;
		}

		if (HikeMessengerApp.isStealthMsisdn(statusMessage.getMsisdn()))
		{
			int stealthMode = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
			if (stealthMode != HikeConstants.STEALTH_ON)
			{
				return;
			}
		}
		Intent intent = Utils.createIntentFromContactInfo(new ContactInfo(null, statusMessage.getMsisdn(), statusMessage.getNotNullName(), statusMessage.getMsisdn()), true);
		intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
		intent.setClass(getActivity(), ChatThread.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		getActivity().finish();
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

		if (!reachedEnd && !loadingMoreMessages && !statusMessages.isEmpty()
				&& (firstVisibleItem + visibleItemCount) >= (statusMessages.size() - HikeConstants.MIN_INDEX_TO_LOAD_MORE_MESSAGES))
		{

			Logger.d(getClass().getSimpleName(), "Loading more items");
			loadingMoreMessages = true;

			AsyncTask<Void, Void, List<StatusMessage>> asyncTask = new AsyncTask<Void, Void, List<StatusMessage>>()
			{

				@Override
				protected List<StatusMessage> doInBackground(Void... params)
				{
					List<StatusMessage> olderMessages = HikeConversationsDatabase.getInstance().getStatusMessages(true, HikeConstants.MAX_OLDER_STATUSES_TO_LOAD_EACH_TIME,
							(int) statusMessages.get(statusMessages.size() - 1).getId(), friendMsisdns);
					return olderMessages;
				}

				@Override
				protected void onPostExecute(List<StatusMessage> olderMessages)
				{
					if (!isAdded())
					{
						return;
					}

					if (!olderMessages.isEmpty())
					{
						int scrollOffset = getListView().getChildAt(0).getTop();

						statusMessages.addAll(statusMessages.size(), olderMessages);
						centralTimelineAdapter.notifyDataSetChanged();
						getListView().setSelectionFromTop(firstVisibleItem, scrollOffset);
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
		Logger.d(getClass().getSimpleName(), "CentralTimeline Adapter Scrolled State: " + scrollState);
		centralTimelineAdapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_TIMELINE_IMAGES && scrollState == OnScrollListener.SCROLL_STATE_FLING);
		/*
		 * // Pause fetcher to ensure smoother scrolling when flinging if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) { // Before Honeycomb pause image loading
		 * on scroll to help with performance if (!Utils.hasHoneycomb()) { if(centralTimelineAdapter != null) { centralTimelineAdapter.getTimelineImageLoader().setPauseWork(true);
		 * centralTimelineAdapter.getIconImageLoader().setPauseWork(true); } } } else { if(centralTimelineAdapter != null) {
		 * centralTimelineAdapter.getTimelineImageLoader().setPauseWork(false); centralTimelineAdapter.getIconImageLoader().setPauseWork(false); } }
		 */
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

			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					statusMessages.add(startIndex, statusMessage);

					if (noStatusMessage != null && (statusMessages.size() >= HikeConstants.MIN_STATUS_COUNT || statusMessage.getMsisdn().equals(userMsisdn)))
					{
						statusMessages.remove(noStatusMessage);
						noStatusMessage = null;
					}
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
			HikeMessengerApp.getPubSub().publish(HikePubSub.RESET_NOTIFICATION_COUNTER, null);
		}
		else if (HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (!shouldAddFTUEItem())
					{
						removeFTUEItemIfExists();
					}
					else
					{
						addFTUEItem(statusMessages);
					}
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.PROTIP_ADDED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					addProtip((Protip) object);
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.ICON_CHANGED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
		}
	}

	private int getStartIndex()
	{
		int startIndex = 0;
		if (noStatusMessage != null)
		{
			startIndex++;
		}
		return startIndex;
	}

	private boolean shouldAddFTUEItem()
	{
		if (HomeActivity.ftueContactsData.isEmpty() || statusMessages.size() > HikeConstants.MIN_STATUS_COUNT || prefs.getBoolean(HikeMessengerApp.HIDE_FTUE_SUGGESTIONS, false))
		{
			return false;
		}

		/*
		 * To add an ftue item, we need to make sure the user does not have 5 friends.
		 */
		int friendCounter = 0;
		for (ContactInfo contactInfo : HomeActivity.ftueContactsData.getCompleteList())
		{
			FavoriteType favoriteType = contactInfo.getFavoriteType();
			if (favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_RECEIVED || favoriteType == FavoriteType.REQUEST_SENT
					|| favoriteType == FavoriteType.REQUEST_SENT_REJECTED)
			{
				friendCounter++;
			}
		}
		return friendCounter < HikeConstants.FTUE_LIMIT && friendCounter < HomeActivity.ftueContactsData.getCompleteList().size();
	}

	private void addFTUEItem(List<StatusMessage> statusMessages)
	{
		removeFTUEItemIfExists();
		statusMessages.add(new StatusMessage(CentralTimelineAdapter.FTUE_ITEM_ID, null, null, null, null, null, 0));
	}

	private void removeFTUEItemIfExists()
	{
		if (!statusMessages.isEmpty())
		{
			if (statusMessages.get(statusMessages.size() - 1).getId() == CentralTimelineAdapter.FTUE_ITEM_ID)
			{
				statusMessages.remove(statusMessages.size() - 1);
			}
		}
	}

	private class FetchUpdates extends AsyncTask<Void, Void, List<StatusMessage>>
	{

		@Override
		protected List<StatusMessage> doInBackground(Void... params)
		{
			List<ContactInfo> friendsList = HikeMessengerApp.getContactManager().getContactsOfFavoriteType(FavoriteType.FRIEND, HikeConstants.BOTH_VALUE, userMsisdn);

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
			if (!isAdded())
			{
				Logger.d(getClass().getSimpleName(), "Not added");
				return;
			}

			String name = Utils.getFirstName(prefs.getString(HikeMessengerApp.NAME_SETTING, null));
			String lastStatus = prefs.getString(HikeMessengerApp.LAST_STATUS, "");

			/*
			 * If we already have a few status messages in the timeline, no need to prompt the user to post his/her own message.
			 */
			if (result.size() < HikeConstants.MIN_STATUS_COUNT)
			{
				if (TextUtils.isEmpty(lastStatus))
				{
					noStatusMessage = new StatusMessage(CentralTimelineAdapter.EMPTY_STATUS_NO_STATUS_ID, null, "12345", getString(R.string.mood_update), getString(
							R.string.hey_name, name), StatusMessageType.NO_STATUS, System.currentTimeMillis() / 1000);
					statusMessages.add(0, noStatusMessage);
				}
				else if (result.isEmpty())
				{
					noStatusMessage = new StatusMessage(CentralTimelineAdapter.EMPTY_STATUS_NO_STATUS_RECENTLY_ID, null, "12345", getString(R.string.mood_update), getString(
							R.string.hey_name, name), StatusMessageType.NO_STATUS, System.currentTimeMillis() / 1000);
					statusMessages.add(0, noStatusMessage);
				}
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
				centralTimelineAdapter.setProtipIndex(startIndex);
			}

			statusMessages.addAll(result);
			Logger.d(getClass().getSimpleName(), "Updating...");
			/*
			 * added this to delay updating the adapter while the viewpager is swiping since it break that animation.
			 */
			new Handler().postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					if (shouldAddFTUEItem())
					{
						addFTUEItem(statusMessages);
					}
					else
					{
						removeFTUEItemIfExists();
					}

					centralTimelineAdapter.notifyDataSetChanged();
					HikeMessengerApp.getPubSub().addListeners(UpdatesFragment.this, pubSubListeners);

				}
			}, 300);
		}

	}

	private void addProtip(Protip protip)
	{
		if (protip != null)
		{
			final int startIndex = getStartIndex();
			statusMessages.add(getStartIndex(), new StatusMessage(protip));
			centralTimelineAdapter.setProtipIndex(startIndex);
		}
	}

}
