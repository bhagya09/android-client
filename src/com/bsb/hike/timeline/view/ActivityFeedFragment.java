package com.bsb.hike.timeline.view;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.timeline.adapter.ActivityFeedCursorAdapter;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ActivityFeedFragment extends Fragment implements Listener
{

	private ActivityFeedCursorAdapter activityFeedCardAdapter;

	private String[] pubSubListeners = { HikePubSub.ICON_CHANGED, HikePubSub.ACTIVITY_UPDATE, HikePubSub.CLOSE_CURRENT_STEALTH_CHAT };

	private RecyclerView mActivityFeedRecyclerView;

	private LinearLayoutManager mLayoutManager;

	//Default Constructor as per android guidelines
	public ActivityFeedFragment()
	{
		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.activity_feed, null);
		mActivityFeedRecyclerView = (RecyclerView) parent.findViewById(R.id.activityFeedRecycleView);
		mLayoutManager = new LinearLayoutManager(getActivity());
		mActivityFeedRecyclerView.setLayoutManager(mLayoutManager);
		setupActionBar();
		return parent;
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		HikeMessengerApp.getPubSub().publish(HikePubSub.UNSEEN_STATUS_COUNT_CHANGED, null);
		HikeMessengerApp.getPubSub().publish(HikePubSub.BADGE_COUNT_ACTIVITY_UPDATE_CHANGED, new Integer(0));
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		executeActivityFeedFetchTask();
	}

	private void executeActivityFeedFetchTask()
	{
		FetchActivityFeeds fetchUpdates = new FetchActivityFeeds();
		fetchUpdates.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

		if (HikePubSub.ICON_CHANGED.equals(type))
		{
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					activityFeedCardAdapter.notifyDataSetChanged();
				}
			});
		}
		else if (HikePubSub.ACTIVITY_UPDATE.equals(type))
		{
			Logger.d(HikeConstants.TIMELINE_LOGS, "inside AFF, revc pubsub ACTIVITY_UPDATE");
			executeActivityFeedFetchTask();
		}
		else if (HikePubSub.CLOSE_CURRENT_STEALTH_CHAT.equals(type))
		{
			executeActivityFeedFetchTask();
		}
	}

	private class FetchActivityFeeds extends AsyncTask<Void, Void, Cursor>
	{

		@Override
		protected Cursor doInBackground(Void... params)
		{
			String[] msisdnList = HikeConversationsDatabase.getInstance().getTimelineFriendsMsisdn(HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING,""));
			return HikeConversationsDatabase.getInstance().getActivityFeedsCursor(msisdnList);
		}

		@Override
		protected void onPostExecute(final Cursor result)
		{
			if (!isAdded())
			{
				Logger.d(getClass().getSimpleName(), "Not added");
				return;
			}

			Logger.d(HikeConstants.TIMELINE_LOGS, "onPost Execute, The no of feeds are " + result.getCount());
			
			if(result != null)
			{
				if (activityFeedCardAdapter == null)
				{
					activityFeedCardAdapter = new ActivityFeedCursorAdapter(getActivity(), result, 0);
					mActivityFeedRecyclerView.setAdapter(activityFeedCardAdapter);
					HikeMessengerApp.getPubSub().addListeners(ActivityFeedFragment.this, pubSubListeners);
				}
				else
				{
					activityFeedCardAdapter.swapCursor(result);
				}

				UpdateActivityFeedsTask updateActivityFeedTask = new UpdateActivityFeedsTask();
				updateActivityFeedTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			else
			{
				Logger.d(HikeConstants.TIMELINE_LOGS, "DB call for Feed return 0 result " + result.getCount());
			}
		}

	}

	private class UpdateActivityFeedsTask extends AsyncTask<Void, Void, Void>
	{

		@Override
		protected Void doInBackground(Void... params)
		{
			HikeConversationsDatabase.getInstance().updateActivityFeedReadStatus();
			HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.USER_TIMELINE_ACTIVITY_COUNT, 0);
			return null;
		}

	}

	public void setupActionBar()
	{
		ActionBar actionBar = ((TimelineActivity) getActivity()).getSupportActionBar();

		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = getActivity().getLayoutInflater().inflate(R.layout.compose_action_bar, null);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(getResources().getString(R.string.activity_feed_actionbar_title));

		TextView subText = (TextView) actionBarView.findViewById(R.id.subtext);
		subText.setVisibility(View.GONE);

		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		actionBar.setCustomView(actionBarView);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		menu.clear();
	}
	
}
