package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
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
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Utils;

public class UpdatesFragment extends SherlockListFragment implements
		OnScrollListener, Listener {

	private StatusMessage noStatusMessage;
	private CentralTimelineAdapter centralTimelineAdapter;
	private String userMsisdn;
	private SharedPreferences prefs;
	private List<StatusMessage> statusMessages;
	private boolean reachedEnd;
	private boolean loadingMoreMessages;

	private String[] pubSubListeners = { HikePubSub.TIMELINE_UPDATE_RECIEVED,
			HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED,
			HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED,
			HikePubSub.PROTIP_ADDED};
	private String[] friendMsisdns;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View parent = inflater.inflate(R.layout.updates, null);

		ListView updatesList = (ListView) parent
				.findViewById(android.R.id.list);
		updatesList.setEmptyView(parent.findViewById(android.R.id.empty));

		return parent;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (centralTimelineAdapter != null) {
			centralTimelineAdapter.restartImageLoaderThread();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (centralTimelineAdapter != null) {
			centralTimelineAdapter.stopImageLoaderThread();
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		prefs = getActivity().getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		userMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, "");

		statusMessages = new ArrayList<StatusMessage>();

		centralTimelineAdapter = new CentralTimelineAdapter(getActivity(),
				statusMessages, userMsisdn);
		setListAdapter(centralTimelineAdapter);
		getListView().setOnScrollListener(this);

		FetchUpdates fetchUpdates = new FetchUpdates();
		if (Utils.isHoneycombOrHigher()) {
			fetchUpdates.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			fetchUpdates.execute();
		}
	}

	@Override
	public void onDestroy() {
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		StatusMessage statusMessage = centralTimelineAdapter.getItem(position);
		if (statusMessage.getId() == CentralTimelineAdapter.FTUE_ITEM_ID
				|| (statusMessage.getStatusMessageType() == StatusMessageType.NO_STATUS)
				|| (statusMessage.getStatusMessageType() == StatusMessageType.FRIEND_REQUEST)
				|| (statusMessage.getStatusMessageType() == StatusMessageType.PROTIP)) {
			return;
		} else if (userMsisdn.equals(statusMessage.getMsisdn())) {
			Intent intent = new Intent(getActivity(), ProfileActivity.class);
			intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
			startActivity(intent);
			return;
		}

		Intent intent = Utils.createIntentFromContactInfo(new ContactInfo(null,
				statusMessage.getMsisdn(), statusMessage.getNotNullName(),
				statusMessage.getMsisdn()), true);
		intent.putExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, true);
		intent.setClass(getActivity(), ChatThread.class);
		startActivity(intent);
	}

	@Override
	public void onScroll(AbsListView view, final int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (!reachedEnd
				&& !loadingMoreMessages
				&& !statusMessages.isEmpty()
				&& (firstVisibleItem + visibleItemCount) >= (statusMessages
						.size() - HikeConstants.MIN_INDEX_TO_LOAD_MORE_MESSAGES)) {

			Log.d(getClass().getSimpleName(), "Loading more items");
			loadingMoreMessages = true;

			AsyncTask<Void, Void, List<StatusMessage>> asyncTask = new AsyncTask<Void, Void, List<StatusMessage>>() {

				@Override
				protected List<StatusMessage> doInBackground(Void... params) {
					List<StatusMessage> olderMessages = HikeConversationsDatabase
							.getInstance()
							.getStatusMessages(
									true,
									HikeConstants.MAX_OLDER_STATUSES_TO_LOAD_EACH_TIME,
									(int) statusMessages.get(
											statusMessages.size() - 1).getId(),
									friendMsisdns);
					return olderMessages;
				}

				@Override
				protected void onPostExecute(List<StatusMessage> olderMessages) {
					if (!isAdded()) {
						return;
					}

					if (!olderMessages.isEmpty()) {
						statusMessages.addAll(statusMessages.size(),
								olderMessages);
						centralTimelineAdapter.notifyDataSetChanged();
						getListView().setSelection(firstVisibleItem);
					} else {
						/*
						 * This signifies that we've reached the end. No need to
						 * query the db anymore unless we add a new message.
						 */
						reachedEnd = true;
					}

					loadingMoreMessages = false;
				}

			};
			if (Utils.isHoneycombOrHigher()) {
				asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				asyncTask.execute();
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	public void onEventReceived(String type, Object object) {

		if (!isAdded()) {
			return;
		}

		if (HikePubSub.TIMELINE_UPDATE_RECIEVED.equals(type)) {
			final StatusMessage statusMessage = (StatusMessage) object;
			final int startIndex = getStartIndex();
			Utils.resetUnseenStatusCount(prefs);

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					statusMessages.add(startIndex, statusMessage);
					if (noStatusMessage != null
							&& (statusMessages.size() >= HikeConstants.MIN_STATUS_COUNT || statusMessage
									.getMsisdn().equals(userMsisdn))) {
						statusMessages.remove(noStatusMessage);
						noStatusMessage = null;
					}
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.RESET_NOTIFICATION_COUNTER, null);
		} else if (HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED.equals(type)) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
		} else if (HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED.equals(type)) {
			if (!shouldAddFTUEItem()) {
				return;
			}
			addFTUEItem(statusMessages);
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
		}else if (HikePubSub.PROTIP_ADDED.equals(type)){
			addProtip((Protip)object);
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					centralTimelineAdapter.notifyDataSetChanged();
				}
			});
		}
	}

	private int getStartIndex() {
		int startIndex = 0;
		if (noStatusMessage != null) {
			startIndex++;
		}
		return startIndex;
	}

	private boolean shouldAddFTUEItem() {
		if (HomeActivity.ftueList.isEmpty()
				|| statusMessages.size() > HikeConstants.MIN_STATUS_COUNT) {
			return false;
		}

		/*
		 * To add an ftue item, we need to make sure the user does not have 5
		 * friends.
		 */
		int friendCounter = 0;
		for (ContactInfo contactInfo : HomeActivity.ftueList) {
			FavoriteType favoriteType = contactInfo.getFavoriteType();
			if (favoriteType == FavoriteType.FRIEND
					|| favoriteType == FavoriteType.REQUEST_RECEIVED
					|| favoriteType == FavoriteType.REQUEST_SENT
					|| favoriteType == FavoriteType.REQUEST_SENT_REJECTED) {
				friendCounter++;
			}
		}
		return friendCounter < HikeConstants.FTUE_LIMIT;
	}

	private void addFTUEItem(List<StatusMessage> statusMessages) {
		if (!statusMessages.isEmpty()) {
			if (statusMessages.get(statusMessages.size() - 1).getId() == CentralTimelineAdapter.FTUE_ITEM_ID) {
				statusMessages.remove(statusMessages.size() - 1);
			}
		}
		statusMessages.add(new StatusMessage(
				CentralTimelineAdapter.FTUE_ITEM_ID, null, null, null, null,
				null, 0));
	}

	private class FetchUpdates extends
			AsyncTask<Void, Void, List<StatusMessage>> {

		@Override
		protected List<StatusMessage> doInBackground(Void... params) {
			List<ContactInfo> friendsList = HikeUserDatabase.getInstance()
					.getContactsOfFavoriteType(FavoriteType.FRIEND,
							HikeConstants.BOTH_VALUE, userMsisdn);

			ArrayList<String> msisdnList = new ArrayList<String>();

			for (ContactInfo contactInfo : friendsList) {
				if (TextUtils.isEmpty(contactInfo.getMsisdn())) {
					continue;
				}
				msisdnList.add(contactInfo.getMsisdn());
			}
			msisdnList.add(userMsisdn);

			friendMsisdns = new String[msisdnList.size()];
			msisdnList.toArray(friendMsisdns);
			List<StatusMessage> statusMessages = HikeConversationsDatabase
					.getInstance().getStatusMessages(true,
							HikeConstants.MAX_STATUSES_TO_LOAD_INITIALLY, -1,
							friendMsisdns);

			if (shouldAddFTUEItem()) {
				addFTUEItem(statusMessages);
			}

			return statusMessages;
		}

		@Override
		protected void onPostExecute(List<StatusMessage> result) {
			if (!isAdded()) {
				Log.d(getClass().getSimpleName(), "Not added");
				return;
			}

			String name = Utils.getFirstName(prefs.getString(
					HikeMessengerApp.NAME_SETTING, null));
			String lastStatus = prefs.getString(HikeMessengerApp.LAST_STATUS,
					"");

			/*
			 * If we already have a few status messages in the timeline, no need
			 * to prompt the user to post his/her own message.
			 */
			if (result.size() < HikeConstants.MIN_STATUS_COUNT) {
				if (TextUtils.isEmpty(lastStatus)) {
					noStatusMessage = new StatusMessage(
							CentralTimelineAdapter.EMPTY_STATUS_NO_STATUS_ID,
							null, "12345", getString(R.string.team_hike),
							getString(R.string.hey_name, name),
							StatusMessageType.NO_STATUS,
							System.currentTimeMillis() / 1000);
					statusMessages.add(0, noStatusMessage);
				} else if (result.isEmpty()) {
					noStatusMessage = new StatusMessage(
							CentralTimelineAdapter.EMPTY_STATUS_NO_STATUS_RECENTLY_ID,
							null, "12345", getString(R.string.team_hike),
							getString(R.string.hey_name, name),
							StatusMessageType.NO_STATUS, System
									.currentTimeMillis() / 1000);
					statusMessages.add(0, noStatusMessage);
				}
			}

			long currentProtipId = prefs.getLong(
					HikeMessengerApp.CURRENT_PROTIP, -1);

			Protip protip = null;
			boolean showProtip = false;
			if (currentProtipId !=-1) {
				showProtip = true;
				protip = HikeConversationsDatabase.getInstance()
						.getProtipForId(currentProtipId);
			} 
			
			if (showProtip && protip != null) {
				statusMessages.add(0, new StatusMessage(protip));
			}

			statusMessages.addAll(result);
			Log.d(getClass().getSimpleName(), "Updating...");
			/*
			 * added this to delay updating the adapter while the viewpager is
			 * swiping since it break that animation.
			 */
			new Handler().postDelayed(new Runnable() {

				@Override
				public void run() {
					centralTimelineAdapter.notifyDataSetChanged();
					HikeMessengerApp.getPubSub().addListeners(
							UpdatesFragment.this, pubSubListeners);
				}
			}, 300);
		}

	}
	
	private void addProtip(Protip protip){	
		if(protip!=null)
			statusMessages.add(getStartIndex(), new StatusMessage(protip));
	}
	
}
