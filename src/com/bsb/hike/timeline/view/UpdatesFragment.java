package com.bsb.hike.timeline.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HomeAnalyticsConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.ImageParser;
import com.bsb.hike.media.ImageParser.ImageParserListener;
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
import com.bsb.hike.timeline.EndlessRecyclerScrollListener;
import com.bsb.hike.timeline.TimelineActionsManager;
import com.bsb.hike.timeline.adapter.TimelineCardsAdapter;
import com.bsb.hike.timeline.model.ActionsDataModel;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.timeline.model.StatusMessage.StatusMessageType;
import com.bsb.hike.timeline.model.TimelineActions;
import com.bsb.hike.ui.GalleryActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StealthModeManager;
import com.bsb.hike.utils.Utils;
import com.etiennelawlor.quickreturn.library.enums.QuickReturnViewType;
import com.etiennelawlor.quickreturn.library.listeners.QuickReturnRecyclerViewOnScrollListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.bsb.hike.utils.HikeAnalyticsEvent;

public class UpdatesFragment extends Fragment implements Listener, OnClickListener
{

	static final int TIMELINE_POST_IMAGE_REQ = 0;

	public static final String SHOW_PROFILE_HEADER = "showProfileHeader";

	private TimelineCardsAdapter timelineCardsAdapter;

	private String userMsisdn;

	private SharedPreferences prefs;

	private List<StatusMessage> statusMessages;

	private String[] pubSubListeners = { HikePubSub.TIMELINE_UPDATE_RECIEVED, HikePubSub.LARGER_UPDATE_IMAGE_DOWNLOADED, HikePubSub.PROTIP_ADDED, HikePubSub.ICON_CHANGED,
			HikePubSub.ACTIVITY_UPDATE, HikePubSub.TIMELINE_WIPE, HikePubSub.TIMELINE_FTUE_LIST_UPDATE,HikePubSub.HIKE_JOIN_TIME_OBTAINED, HikePubSub.USER_JOIN_TIME_OBTAINED, HikePubSub.CLOSE_CURRENT_STEALTH_CHAT, HikePubSub.PROFILE_UPDATE_FINISH };
	
	private String[] friendMsisdns = new String[]{};

	private RecyclerView mUpdatesList;

	private LinearLayoutManager mLayoutManager;

	private View actionsView;

	private Gson gson;

	private List<ContactInfo> mFtueFriendList;

	/**
	 * When packet is not received, in this case , this tell how many referred contacts have to be shown which we received from postAddressBook
	 */
	private final int MAX_CONTCATS_ALLOWED_TO_SHOW_INITIALLY = 4;

	private boolean mShowProfileHeader;

	private ArrayList<String> mMsisdnArray = new ArrayList<String>();

	private View emptyLayout;

	private FetchUpdates fetchUpdates;

	public static final int START_FTUE_WITH_INIT_CARD = 0;

	public static final int START_FTUE_WITH_FAV_CARD = 1;

	public static final int EMPTY_STATE = -10;

	public static final int FILL_STATE = -11;

	public static final int MSG_DELETE = -12;
	
	private boolean reachedEnd;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.updates, null);
		mUpdatesList = (RecyclerView) parent.findViewById(R.id.updatesRecycleView);
		actionsView = parent.findViewById(R.id.new_update_tab);
		emptyLayout = parent.findViewById(R.id.timeline_no_item);
		mLayoutManager = new LinearLayoutManager(getActivity());
		mUpdatesList.setLayoutManager(mLayoutManager);
		// TODO
		// mUpdatesList.setEmptyView(parent.findViewById(android.R.id.empty));
		return parent;
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		checkIfTimelineEmpty();
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

		if (getArguments() != null)
		{
			mShowProfileHeader = getArguments().getBoolean(SHOW_PROFILE_HEADER, false);

			String[] msisdnArray = getArguments().getStringArray(HikeConstants.MSISDNS);

			for (String msisdn : msisdnArray)
			{
				mMsisdnArray.add(msisdn);
			}

			if (msisdnArray!= null && msisdnArray.length == 1)
			{
				//Send Analytics
				HikeAnalyticsEvent.analyticsForUserProfileOpen(msisdnArray[0], "ProfileTap");
			}

		}
		
		timelineCardsAdapter = new TimelineCardsAdapter(getActivity(), statusMessages, userMsisdn, mFtueFriendList, getLoaderManager(), mShowProfileHeader, mMsisdnArray)
		{
			@Override
			public void handleUIMessage(Message msg)
			{
				super.handleUIMessage(msg);
				if (msg.arg1 == UpdatesFragment.EMPTY_STATE || msg.arg1 == UpdatesFragment.FILL_STATE)
				{
					checkIfTimelineEmpty();
					msg.recycle();
				}
				else if (msg.arg1 == UpdatesFragment.MSG_DELETE)
				{
					if (actionsView != null && getActivity() != null)
					{
						getActivity().runOnUiThread(new Runnable()
						{
							public void run()
							{
								actionsView.setX(0);
								actionsView.setY(0);
							}
						});
					}
				}
			}
		};

		mUpdatesList.setAdapter(timelineCardsAdapter);
		
		mUpdatesList.addOnScrollListener(new EndlessRecyclerScrollListener(mLayoutManager)
		{
			@Override
			public void onLoadMore(int current_page)
			{
				if (!reachedEnd && !statusMessages.isEmpty() && statusMessages.size() > HikeConstants.MAX_OLDER_STATUSES_TO_LOAD_EACH_TIME)
				{
					AsyncTask<String, Void, List<StatusMessage>> asyncTask = new AsyncTask<String, Void, List<StatusMessage>>()
					{

						@Override
						protected List<StatusMessage> doInBackground(String... params)
						{
							if (params != null && params.length > 0)
							{
								for (String msisdn : params)
								{
									// TODO Improve for multiple msisdns
									if (userMsisdn.equals(msisdn) || Utils.showContactsUpdates(ContactManager.getInstance().getContact(msisdn,true,true)))
									{
										friendMsisdns = params;
										break;
									}
								}
							}

							List<StatusMessage> olderMessages = null;

							int startId = -1;

							if (isLastMsgJoinTime())
							{
								startId = (int) statusMessages.get(statusMessages.size() - 2).getId();
							}
							else
							{
								startId = (int) statusMessages.get(statusMessages.size() - 1).getId();
							}

							if (friendMsisdns.length > 0 && startId > 1)
							{
								olderMessages = HikeConversationsDatabase.getInstance().getStatusMessages(mShowProfileHeader ? false : true,
										HikeConstants.MAX_OLDER_STATUSES_TO_LOAD_EACH_TIME, startId, friendMsisdns);
							}
							else
							{
								olderMessages = new ArrayList<>();
							}
							return olderMessages;
						}

						@Override
						protected void onPostExecute(List<StatusMessage> olderMessages)
						{
							if (!isAdded())
							{
								return;
							}

							final ArrayList<String> suIDList = new ArrayList<String>();

							if (!olderMessages.isEmpty())
							{
								for (StatusMessage suMessage : olderMessages)
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
										Logger.d(HikeConstants.TIMELINE_LOGS, "list of suIDArray, fetching HTTP calls " + suIDArray);
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
											HikeConversationsDatabase.getInstance().getActionsData(ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), suIDList,
													TimelineActionsManager.getInstance().getActionsData());
											notifyVisibleItems();
										}
									}, 0);
								}
								
								statusMessages.addAll(statusMessages.size(), olderMessages);
								timelineCardsAdapter.notifyDataSetChanged();
							}
							else
							{
								/*
								 * This signifies that we've reached the end. No need to query the db anymore unless we add a new message.
								 */
								reachedEnd = true;
								addJoinTimeMessage();
							}

						}
					};
					asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mMsisdnArray.toArray(new String[mMsisdnArray.size()]));

				}
				else
				{
					//User joined status message
					addJoinTimeMessage();
				}
			}
		});

		if (!mShowProfileHeader)
		{
			QuickReturnRecyclerViewOnScrollListener quickReturnscrollListener = new QuickReturnRecyclerViewOnScrollListener.Builder(QuickReturnViewType.HEADER).header(actionsView)
					.minHeaderTranslation(-1 * HikePhotosUtils.dpToPx(52)).isSnappable(false).build();

			mUpdatesList.addOnScrollListener(quickReturnscrollListener);
			
			actionsView.findViewById(R.id.new_photo_tab).setOnClickListener(this);

			actionsView.findViewById(R.id.new_status_tab).setOnClickListener(this);
		}
		else
		{
			actionsView.setVisibility(View.GONE);
		}

		fetchUpdates = new FetchUpdates();
		fetchUpdates.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mMsisdnArray.toArray(new String[mMsisdnArray.size()]));
	}

	private boolean isLastMsgJoinTime()
	{
		// Check if last message is joined hike message (self inserted)
		if (statusMessages != null && !statusMessages.isEmpty())
		{
			StatusMessage lastMessage = statusMessages.get(statusMessages.size() - 1);
			if (lastMessage.getStatusMessageType() == StatusMessageType.JOINED_HIKE)
			{
				return true;
			}
		}
		return false;
	}

	private void addJoinTimeMessage()
	{
		if(isLastMsgJoinTime())
		{
			return;
		}
		
		if(mShowProfileHeader)
		{
			ContactInfo joinConInfo = ContactManager.getInstance().getContact(mMsisdnArray.get(0), true, true);
			
			if(!joinConInfo.isOnhike())
			{
				return;
			}
			
			StatusMessage cJoinedSM = StatusMessage.getJoinedHikeStatus(joinConInfo);
			
			if (cJoinedSM != null)
			{
				statusMessages.add(cJoinedSM);
				notifyVisibleItems();
				if(cJoinedSM.getTimeStamp() == 0)
				{
					joinConInfo.httpGetHikeJoinTime();
				}
			}
			
			Logger.d(HikeConstants.TIMELINE_LOGS, "User Profile screen, so adding SU " + cJoinedSM);
		}
	}
	
	@Override
	public void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		timelineCardsAdapter.onDestroy();
		super.onDestroy();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if (timelineCardsAdapter != null && !timelineCardsAdapter.getSUViewedSet().isEmpty())
		{
			JSONArray viewedJsonArray = new JSONArray();
			HashSet<String> viewedSUs = timelineCardsAdapter.getSUViewedSet();
			for (String suID : viewedSUs)
			{
				viewedJsonArray.put(suID);
			}

			JSONObject viewsPayload = new JSONObject();
			try
			{
				viewsPayload.put(HikeConstants.SU_ID, viewedJsonArray);
				Logger.d("SendViewsAPI", "Payload"+viewsPayload.toString());
				timelineCardsAdapter.getSUViewedSet().clear();
				RequestToken sendViewsToken = HttpRequests.sendViewsLink(viewsPayload, new IRequestListener()
				{
					@Override
					public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
					{
						Logger.d("SendViewsAPI", "Failed");
					}

					@Override
					public void onRequestSuccess(Response result)
					{
						if (Utils.isResponseValid((JSONObject) result.getBody().getContent()))
						{
							Logger.d("SendViewsAPI", "Success");
						}
						else
						{
							Logger.d("SendViewsAPI", "Stat failed");
						}

					}

					@Override
					public void onRequestProgressUpdate(float progress)
					{
						// Do nothing
					}
				});
				
				if(sendViewsToken!=null)
				{
					sendViewsToken.execute();
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
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
			HikeMessengerApp.getPubSub().publish(HikePubSub.RESET_NOTIFICATION_COUNTER, null);
			
			final StatusMessage statusMessage = (StatusMessage) object;

			if (StealthModeManager.getInstance().isStealthMsisdn(statusMessage.getMsisdn()) && !StealthModeManager.getInstance().isActive())
			{
				return;
			}

			// If not showing profile, lets add new message
			if (mShowProfileHeader)
			{
				// If showing profile, check if msisdn is same or not
				if (mMsisdnArray != null && statusMessage.getMsisdn().equals(mMsisdnArray.get(0)))
				{
					//Do nothing, add message
				}
				else
				{
					return;
				}
			}

			final int startIndex = getStartIndex();

			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					Logger.d(HikeConstants.TIMELINE_LOGS, "on pubsub TIMELINE_UPDATE_RECIEVED adding SU " + statusMessage + "at index " + startIndex);
					statusMessages.add(startIndex, statusMessage);
					timelineCardsAdapter.notifyDataSetChanged();
				}
			});
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
		else if (HikePubSub.ICON_CHANGED.equals(type) || HikePubSub.PROFILE_UPDATE_FINISH.equals(type))
		{
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					timelineCardsAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.ACTIVITY_UPDATE.equals(type))
		{
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					notifyVisibleItems();
				}
			});

		}
		else if (HikePubSub.TIMELINE_WIPE.equals(type))
		{
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					statusMessages.clear();
					resetSharedPrefOnRemovingFTUE();
					timelineCardsAdapter.notifyDataSetChanged();
					HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);
					HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.USER_TIMELINE_ACTIVITY_COUNT, 0);
				}
			});
		}
		else if (HikePubSub.TIMELINE_FTUE_LIST_UPDATE.equals(type))
		{
			if (shouldAddFTUEItem())
			{
				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if (!isAdded())
						{
							return;
						}

						if (object != null)
						{
							mFtueFriendList = new ArrayList<ContactInfo>(); 
							Pair<Set<String>, Integer> pair = (Pair<Set<String>, Integer>)object;
							HashSet<String> msisdnSet = (HashSet<String>) pair.first;
							int counter = pair.second;
							Logger.d("tl_ftue", "inside pubub " + msisdnSet+", and final count is "+ counter);
							Iterator<String> iterator = msisdnSet.iterator();
							int i=0;
							while(iterator.hasNext() && i < counter)
							{
								ContactInfo info = ContactManager.getInstance().getContact(iterator.next(), true, true);
								if (info.getFavoriteType().equals(FavoriteType.NOT_FRIEND) 
										&& !Utils.getUserContactInfo(false).getMsisdn().equals(info.getMsisdn()))
								{
									mFtueFriendList.add(info);
									i++;
									Logger.d("tl_ftue", " adding "+ info.getMsisdn()+" as htis is NON-FRIEND");
								}
								else
								{
									Logger.d("tl_ftue", " not adding "+ info.getMsisdn()+" as status is "+ info.getMsisdnType());
								}
							}
							Logger.d("tl_ftue", "inside pubub, final list after check is " + mFtueFriendList);
							//SU List is Empty or NO FTUE card was present
							if(statusMessages.isEmpty() 
									|| !(statusMessages.get(0).getId() == TimelineCardsAdapter.FTUE_CARD_EXIT
									|| statusMessages.get(0).getId() == TimelineCardsAdapter.FTUE_CARD_INIT
									|| statusMessages.get(0).getId() == TimelineCardsAdapter.FTUE_CARD_FAV))
							{
								updateFTUEMsisdnsList(mFtueFriendList, false);
								addFTUEItem();
								notifyVisibleItems();
							}
							else //SU List is not empty and FTUE card is present
							{
								//We have to rearrange only When FTUE of Type FTUE_CARD_FAV is present
								boolean rearrange = statusMessages.get(0).getId() == TimelineCardsAdapter.FTUE_CARD_FAV ? true : false;
								updateFTUEMsisdnsList(mFtueFriendList, rearrange);
							}
						}
					}

				});
			}
		}
		else if (HikePubSub.HIKE_JOIN_TIME_OBTAINED.equals(type) || HikePubSub.USER_JOIN_TIME_OBTAINED.equals(type))
		{
			if (!mShowProfileHeader || mMsisdnArray == null || mMsisdnArray.isEmpty())
			{
				return;
			}

			Pair<String, Long> msisdnHikeJoinTimePair = (Pair<String, Long>) object;

			String msisdn = msisdnHikeJoinTimePair.first;
			long hikeJoinTime = msisdnHikeJoinTimePair.second;

			if (!msisdn.equals(mMsisdnArray.get(0)))
			{
				return;
			}

			ContactManager.getInstance().getContact(mMsisdnArray.get(0), true, true, false).setHikeJoinTime(hikeJoinTime);

			notifyVisibleItems();
		}
		else if (HikePubSub.CLOSE_CURRENT_STEALTH_CHAT.equals(type))
		{
			if (isAdded() && getActivity() != null && !Utils.isEmpty(statusMessages))
			{
				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						boolean shouldRefresh = false;
						for(int i = statusMessages.size() - 1; i >= 0 ; i--)
						{
							StatusMessage statusMessage = statusMessages.get(i);
							if(StealthModeManager.getInstance().isStealthMsisdn(statusMessage.getMsisdn()))
							{
								statusMessages.remove(i);
								shouldRefresh = true;
							}
						}

						if(shouldRefresh)
						{
							timelineCardsAdapter.notifyDataSetChanged();
						}
					}
				});
			}
		}
	}

	private int getStartIndex()
	{
		int startIndex = 0;
		if (!statusMessages.isEmpty() && HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ENABLE_TIMELINE_FTUE, true))
		{
			startIndex++;
		}
		return startIndex;
	}

	// mShowProfileHeader:- tells screen is Profile screen or TL screen
	private boolean shouldAddFTUEItem()
	{
		return HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ENABLE_TIMELINE_FTUE, true) && !mShowProfileHeader;
	}

	private void addFTUEItem()
	{
		// If no msisdn to show, then no need to show FTUE
		if (mFtueFriendList.isEmpty() && !HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.EXIT_CARD_ON_TOP, false))
		{
			resetSharedPrefOnRemovingFTUE();
			timelineCardsAdapter.removeAllFTUEItems();
			return;
		}
		StatusMessage ftueStatusMessage = null;
		ContactInfo contact = null;
		if (!HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.INIT_CARD_SHOWN, false)
				|| HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.INIT_CARD_ON_TOP, false))
		{
			// To SHOW BASIC CARD
			ftueStatusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_INIT, null, null, null, null, null, 0);
			statusMessages.add(0, ftueStatusMessage);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.INIT_CARD_SHOWN, true);
			HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.INIT_CARD_ON_TOP, true);
			return;
		}
		else if (HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.EXIT_CARD_SHOWN, false)
				&& HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.EXIT_CARD_ON_TOP, false))
		{
			// To SHOW Exit Card
			ftueStatusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_EXIT, null, null, null, null, null, 0);
			statusMessages.add(0, ftueStatusMessage);
		}
		else 
		{
			// To SHOW Fav Card
			contact = mFtueFriendList.get(0);
			ftueStatusMessage = new StatusMessage(TimelineCardsAdapter.FTUE_CARD_FAV, null, contact.getMsisdn(), contact.getName(), null, null, 0);
			statusMessages.add(0, ftueStatusMessage);
			return;
		}
	}

	/**
	 * This sets two values in SP
	 * 1) Set FLAG :-  ENABLE_TIMELINE_FTUE to false...as not to show Timeline till next packet comes
	 */
	private void resetSharedPrefOnRemovingFTUE()
	{
		HikeSharedPreferenceUtil.getInstance().saveData(HikeConstants.ENABLE_TIMELINE_FTUE, false);
		
		if(mFtueFriendList != null)
		{
			mFtueFriendList.clear();
		}
	}

	private class FetchUpdates extends AsyncTask<String, Void, List<StatusMessage>>
	{

		@Override
		protected List<StatusMessage> doInBackground(String... params)
		{

			if (params != null && params.length > 0)
			{
				for (String msisdn : params)
				{
					// TODO Improve for multiple msisdns
					if (userMsisdn.equals(msisdn) || Utils.showContactsUpdates(ContactManager.getInstance().getContact(msisdn, true, true)))
					{
						friendMsisdns = params;
						break;
					}
				}
			}
			else
			{
				friendMsisdns = HikeConversationsDatabase.getTimelineFriendsMsisdn(userMsisdn);
			}

			List<StatusMessage> statusMessages = null;

			if (friendMsisdns.length > 0)
			{
				statusMessages = HikeConversationsDatabase.getInstance().getStatusMessages(mShowProfileHeader?false:true, HikeConstants.MAX_STATUSES_TO_LOAD_INITIALLY, -1, friendMsisdns);
			}
			else
			{
				statusMessages = new ArrayList<>();
			}
			
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

			Logger.d(HikeConstants.TIMELINE_LOGS, "list of SUs to show on Timeline " + result);
			
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
					Logger.d(HikeConstants.TIMELINE_LOGS, "list of suIDArray, fetching HTTP calls " + suIDArray);
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
						HikeConversationsDatabase.getInstance().getActionsData(ActionsDataModel.ActivityObjectTypes.STATUS_UPDATE.getTypeString(), suIDList, TimelineActionsManager.getInstance().getActionsData());
						notifyVisibleItems();
					}
				}, 0);
			}

			long currentProtipId = -1l;

			//Defensive check. TODO Remove protip code from application.
			try
			{
				currentProtipId = prefs.getLong(HikeMessengerApp.CURRENT_PROTIP, -1l);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}

			Protip protip = null;
			boolean showProtip = false;
			if (currentProtipId != -1l)
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
			
			Logger.d(HikeConstants.TIMELINE_LOGS, "list of SUs after protip on Timeline " + statusMessages);
			
			Logger.d(getClass().getSimpleName(), "Updating...");

			if (shouldAddFTUEItem())
			{
				mFtueFriendList = getFtueFriendList();
				if (mFtueFriendList != null)
				{
					timelineCardsAdapter.setFTUEFriendList(mFtueFriendList);
					addFTUEItem();
					getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							timelineCardsAdapter.notifyItemInserted(0);
						}
					});
				}
			}
			
			if(statusMessages.size() < HikeConstants.MAX_STATUSES_TO_LOAD_INITIALLY)
			{
				addJoinTimeMessage();
			}
			
			timelineCardsAdapter.notifyDataSetChanged();
			
			HikeMessengerApp.getPubSub().addListeners(UpdatesFragment.this, pubSubListeners);
		}

	}
	
	private void checkIfTimelineEmpty()
	{
		if (statusMessages.isEmpty() && fetchUpdates.getStatus() == AsyncTask.Status.FINISHED && !mShowProfileHeader)
		{
			emptyLayout.setVisibility(View.VISIBLE);
		}
		else
		{
			emptyLayout.setVisibility(View.GONE);
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
			
			HikeSharedPreferenceUtil.getInstance().saveStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, msisdnSet);
			Logger.d("tl_ftue", "====== List from Server Reco:- " + msisdnSet);
		}
		else
		{
			isFromPacketList = true;
			Logger.d("tl_ftue", "====== Going to check fron list received from server packet" + msisdnSet);
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
				
				if(c == null || c.getFavoriteType() == null || c.getMsisdn() == null)
				{
					Logger.d("tl_ftue", "NPE: favourite null");
					continue;
				}
				
				if (c.getFavoriteType().equals(FavoriteType.NOT_FRIEND) 
						&& !c.getMsisdn().equals(Utils.getUserContactInfo(false).getMsisdn()))
				{
					Logger.d("tl_ftue", id + " is not a frnd so adding for ftue list :- " + c.getName() +", "+ c.getFavoriteType());
					finalContactLsit.add(c);
					counter++;
				}
				else
				{
					Logger.d("tl_ftue", id + " a frnd so NOT ADDING.... for ftue list :- " + c.getName() +", "+ c.getFavoriteType());
				}
				
				if (isFromSignUpList && counter == MAX_CONTCATS_ALLOWED_TO_SHOW_INITIALLY)
				{
					break;
				}
				else if (isFromPacketList && counter == finalCount)
				{
					Logger.d("tl_ftue", "Max count reached got from server packet " + counter + " so leaving next contacts");
					break;
				}
			}
		}

		if (finalContactLsit == null || finalContactLsit.isEmpty())
		{
			if(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.EXIT_CARD_ON_TOP, false))
			{
				Logger.d("tl_ftue", "NO contacts to show, but previous EXIT CARD TO SHOW, so showing it");
				return finalContactLsit;
			}
			else
			{
				resetSharedPrefOnRemovingFTUE();
				Logger.d("tl_ftue", "NO contacts, and no exit card to show ===> so showing no FTUE");
			}
		}
		else
		{
			updateFTUEMsisdnsList(finalContactLsit, false);
			Logger.d("tl_ftue", "final list after check "+ settings.getStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, null));
			Logger.d("tl_ftue", "their names are "+ finalContactLsit);
		}
		return finalContactLsit;
	}

	private void updateFTUEMsisdnsList(List<ContactInfo> finalContactLsit, boolean rearrangeList)
	{
		Set<String> list = new HashSet<String>();
		for(int i = 0; i< finalContactLsit.size(); i++)
		{
			list.add(finalContactLsit.get(i).getMsisdn());
		}
		HikeSharedPreferenceUtil settings = HikeSharedPreferenceUtil.getInstance();
		settings.saveStringSet(HikeConstants.TIMELINE_FTUE_MSISDN_LIST, list);
		
		//set current FTUE is as first element of list
		if(rearrangeList)
		{
			String currentMsisdn = statusMessages.get(0).getMsisdn();
			ContactInfo currentFTUE = ContactManager.getInstance().getContact(currentMsisdn, true, true);
			finalContactLsit.remove(currentFTUE);
			finalContactLsit.add(0, currentFTUE);
		}
		timelineCardsAdapter.setFTUEFriendList(finalContactLsit);
	}

	private IRequestListener actionUpdatesReqListener = new IRequestListener()
	{
		@Override
		public void onRequestSuccess(Response result)
		{
			final JSONObject response = (JSONObject) result.getBody().getContent();

			Logger.d(HikeConstants.TIMELINE_LOGS, "responce from http call "+ response);
			
			if (Utils.isResponseValid(response))
			{
				TimelineActions actionsData = gson.fromJson(response.toString(), TimelineActions.class);
				
				if(actionsData == null)
				{
					return;
				}

				notifyVisibleItems();

				HikeConversationsDatabase.getInstance().updateActionsData(actionsData, ActivityObjectTypes.STATUS_UPDATE);
				
				TimelineActionsManager.getInstance().setActionsData(actionsData);
			}
		}

		@Override
		public void onRequestProgressUpdate(float progress)
		{
			// Do nothing
		}

		@Override
		public void onRequestFailure(@Nullable Response errorResponse, HttpException httpException)
		{
			// Do nothing
			Logger.d(HikeConstants.TIMELINE_LOGS, "responce from http call failed "+ httpException.toString());
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
			recordNewPhotoClick();
			int galleryFlags = GalleryActivity.GALLERY_CATEGORIZE_BY_FOLDERS | GalleryActivity.GALLERY_CROP_IMAGE | GalleryActivity.GALLERY_COMPRESS_EDITED_IMAGE
					| GalleryActivity.GALLERY_DISPLAY_CAMERA_ITEM;

			Intent galleryPickerIntent = IntentFactory.getHikeGalleryPickerIntent(getActivity(), galleryFlags, Utils.getNewImagePostFilePath());
			startActivityForResult(galleryPickerIntent, TIMELINE_POST_IMAGE_REQ);
			break;

		case R.id.new_status_tab:
			recordNewStatusClick();
			Intent newSUIntent = IntentFactory.getPostStatusUpdateIntent(getActivity(), null, null, false);
			Utils.setSpecies(HomeAnalyticsConstants.SU_SPECIES_TIMELINE_TEXT_BUTTON, newSUIntent);
			startActivity(newSUIntent);
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, final Intent data)
	{
		if (resultCode == Activity.RESULT_CANCELED)
		{
			return;
		}

		final String genus = data.getStringExtra(HikeConstants.Extras.GENUS);

		switch (requestCode)
		{
		case TIMELINE_POST_IMAGE_REQ:
			ImageParser.parseResult(getActivity(), resultCode, data, new ImageParserListener()
			{
				@Override
				public void imageParsed(String imagePath)
				{
					Intent newSUIntent = IntentFactory.getPostStatusUpdateIntent(getActivity(), null, imagePath, false);
					Utils.setSpecies(HomeAnalyticsConstants.SU_SPECIES_TIMELINE_PHOTO_BUTTON, newSUIntent);
					if(!TextUtils.isEmpty(genus))
					{
						Utils.setGenus(genus, newSUIntent);
					}
					startActivity(newSUIntent);
				}

				@Override
				public void imageParsed(Uri uri)
				{
					// Do nothing
				}

				@Override
				public void imageParseFailed()
				{
					// Do nothing
				}
			}, false);
			break;

		default:
			break;
		}
	}

	public boolean isEmpty()
	{
		if(isAdded() && isVisible() && timelineCardsAdapter != null)
		{
			return timelineCardsAdapter.getItemCount() == 0;
		}
		return false;
	}

	public void scrollToTop()
	{
		if(!isEmpty())
		{
			if(mUpdatesList!=null)
			{
				mUpdatesList.scrollToPosition(0);
			}
		}
	}

	private void recordNewPhotoClick()
	{
		recordTimelineButtonClick("tl_photo");
	}

	private void recordNewStatusClick()
	{
		recordTimelineButtonClick("tl_status");
	}

	private void recordTimelineButtonClick(String whichItem)
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put(AnalyticsConstants.V2.UNIQUE_KEY, HomeAnalyticsConstants.TIMELINE_UK);
			json.put(AnalyticsConstants.V2.KINGDOM, HomeAnalyticsConstants.HOMESCREEN_KINGDOM);
			json.put(AnalyticsConstants.V2.PHYLUM, AnalyticsConstants.UI_EVENT);
			json.put(AnalyticsConstants.V2.CLASS, AnalyticsConstants.CLICK_EVENT);
			json.put(AnalyticsConstants.V2.ORDER, whichItem);

			HAManager.getInstance().recordV2(json);
		}

		catch (JSONException e)
		{
			e.toString();
		}
	}


}
