package com.bsb.hike.timeline.view;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.AttachmentPicker;
import com.bsb.hike.media.ImageParser;
import com.bsb.hike.media.ImageParser.ImageParserListener;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.Protip;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.timeline.ActionsDeserializer;
import com.bsb.hike.timeline.TimelineConstants;
import com.bsb.hike.timeline.adapter.TimelineCardsAdapter;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.TimelineActions;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.ui.HomeActivity;
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

	private TimelineCardsAdapter timelineCardsAdapter;

	private String userMsisdn;

	private SharedPreferences prefs;

	private List<StatusMessage> statusMessages;

	private String[] pubSubListeners = { HikePubSub.TIMELINE_UPDATE_RECIEVED, HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED,
			HikePubSub.PROTIP_ADDED, HikePubSub.ICON_CHANGED };

	private String[] friendMsisdns;

	private RecyclerView mUpdatesList;

	private RecyclerView.LayoutManager mLayoutManager;

	private View actionsView;
	
	private TimelineActions actionsData = new TimelineActions();

	private Gson gson;

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
	    gsonBuilder.registerTypeAdapter(ActionsDataModel.class, new ActionsDeserializer());
	    gson = gsonBuilder.create();

		userMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, "");

		statusMessages = new ArrayList<StatusMessage>();

		timelineCardsAdapter = new TimelineCardsAdapter(getActivity(), statusMessages, userMsisdn);

		mUpdatesList.setAdapter(timelineCardsAdapter);

		QuickReturnRecyclerViewOnScrollListener scrollListener = new QuickReturnRecyclerViewOnScrollListener.Builder(QuickReturnViewType.HEADER).header(actionsView)
				.minHeaderTranslation(-1 * HikePhotosUtils.dpToPx(60)).isSnappable(false).build();

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

					if (noStatusMessage != null && (statusMessages.size() >= HikeConstants.MIN_STATUS_COUNT || statusMessage.getMsisdn().equals(userMsisdn)))
					{
						statusMessages.remove(noStatusMessage);
						noStatusMessage = null;
					}
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
		else if (HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED.equals(type))
		{
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
		statusMessages.add(new StatusMessage(TimelineCardsAdapter.FTUE_ITEM_ID, null, null, null, null, null, 0));
	}

	private void removeFTUEItemIfExists()
	{
		if (!statusMessages.isEmpty())
		{
			if (statusMessages.get(statusMessages.size() - 1).getId() == TimelineCardsAdapter.FTUE_ITEM_ID)
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
				suIDList.add(suMessage.getMappedId());
			}
			
			if(!suIDList.isEmpty())
			{
				// Get actions for SU from HTTP
				JSONArray suIDArray = new JSONArray(suIDList);
				JSONObject suUpdateJSON = new JSONObject();
				try
				{
					suUpdateJSON.put(HikeConstants.SU_ID_LIST, suIDArray);
					HttpRequests.getActionUpdates(suUpdateJSON, actionUpdatesReqListener);
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
						timelineCardsAdapter.notifyVisibleItems();
					}
				}, 0);
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

					timelineCardsAdapter.notifyDataSetChanged();
					HikeMessengerApp.getPubSub().addListeners(UpdatesFragment.this, pubSubListeners);

				}
			}, 300);
		}

	}
	
	private IRequestListener actionUpdatesReqListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			JSONObject response = (JSONObject) result.getBody().getContent();
			if (response.optString("stat").equals("ok"))
			{
				actionsData = gson.fromJson(response.toString(), TimelineActions.class);
				timelineCardsAdapter.notifyVisibleItems();
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
			Toast.makeText(getActivity().getApplicationContext(), "Clicked new photo", Toast.LENGTH_SHORT).show();
			boolean editPic = Utils.isPhotosEditEnabled();
			int galleryFlags = GalleryActivity.GALLERY_ALLOW_MULTISELECT | GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS;
			if (editPic)
			{
				galleryFlags = galleryFlags | GalleryActivity.GALLERY_EDIT_SELECTED_IMAGE;
			}
			Intent imageIntent = IntentFactory.getHikeGalleryPickerIntent(getActivity().getApplicationContext(), galleryFlags, null);
			imageIntent.putExtra(GalleryActivity.START_FOR_RESULT, true);
			startActivityForResult(imageIntent, TimelineConstants.TIMELINE_NEW_PHOTO_REQUEST);
			break;

		case R.id.new_status_tab:
			Toast.makeText(getActivity().getApplicationContext(), "Clicked new status", Toast.LENGTH_SHORT).show();
			break;

		default:
			break;
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		Toast.makeText(getActivity().getApplicationContext(), "Got result in fragment", Toast.LENGTH_SHORT).show();
		if (resultCode == Activity.RESULT_CANCELED)
		{
			return;
		}
		
		switch (requestCode)
		{
		case AttachmentPicker.EDITOR:
			if(resultCode == Activity.RESULT_OK)
			{
				ImageParserListener listener = new ImageParser.ImageParserListener()
				{
					
					@Override
					public void imageParsed(String imagePath)
					{
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void imageParsed(Uri uri)
					{
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void imageParseFailed()
					{
						// TODO Auto-generated method stub
						
					}
				};
				ImageParser.parseResult(getActivity().getApplicationContext(), resultCode, data, listener, false);
			}
			else if (resultCode == GalleryActivity.GALLERY_ACTIVITY_RESULT_CODE)
			{
				// This would be executed if photos is not enabled on the device
			}
			break;

		default:
			break;
		}
	}

}
