package com.bsb.hike.timeline.view;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.timeline.adapter.ActivityFeedCardCursorAdapter;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.etiennelawlor.quickreturn.library.enums.QuickReturnViewType;
import com.etiennelawlor.quickreturn.library.listeners.QuickReturnRecyclerViewOnScrollListener;

public class ActivityFeedFragment extends SherlockFragment implements Listener
{

	private ActivityFeedCardCursorAdapter activityFeedCardAdapter;

	private String[] pubSubListeners = { HikePubSub.ICON_CHANGED, HikePubSub.ACTIVITY_UPDATE };

	private RecyclerView mActivityFeedRecyclerView;

	private LinearLayoutManager mLayoutManager;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setupActionBar();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.activity_feed, null);
		mActivityFeedRecyclerView = (RecyclerView) parent.findViewById(R.id.activityFeedRecycleView);
		mLayoutManager = new LinearLayoutManager(getActivity());
		mActivityFeedRecyclerView.setLayoutManager(mLayoutManager);

		// TODO
		// mUpdatesList.setEmptyView(parent.findViewById(android.R.id.empty));
		return parent;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		QuickReturnRecyclerViewOnScrollListener scrollListener = new QuickReturnRecyclerViewOnScrollListener.Builder(QuickReturnViewType.HEADER).isSnappable(false).build();

		mActivityFeedRecyclerView.setOnScrollListener(scrollListener);

		executeActivityFeedFetchTask(true);
	}

	private void executeActivityFeedFetchTask(boolean isFirstTime)
	{
		FetchActivityFeeds fetchUpdates = new FetchActivityFeeds(isFirstTime);

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

		super.onActivityCreated(savedInstanceState);
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
				}
			});
		}
		else if (HikePubSub.ACTIVITY_UPDATE.equals(type))
		{
			executeActivityFeedFetchTask(false);
		}
	}

	private class FetchActivityFeeds extends AsyncTask<Void, Void, Cursor>
	{
		private boolean isFirstTimeLoading;
		
		public FetchActivityFeeds(boolean isFirstTimeLoading)
		{
			this.isFirstTimeLoading = isFirstTimeLoading;
		}

		@Override
		protected Cursor doInBackground(Void... params)
		{
			return HikeConversationsDatabase.getInstance().getActivityFeedsCursor();
		}

		@Override
		protected void onPostExecute(final Cursor result)
		{
			if (!isAdded())
			{
				Logger.d(getClass().getSimpleName(), "Not added");
				return;
			}

			new Handler().post(new Runnable()
			{

				@Override
				public void run()
				{
					if (isFirstTimeLoading)
					{
						activityFeedCardAdapter = new ActivityFeedCardCursorAdapter(getActivity(), result, 0);
						mActivityFeedRecyclerView.setAdapter(activityFeedCardAdapter);
						HikeMessengerApp.getPubSub().addListeners(ActivityFeedFragment.this, pubSubListeners);
					}
					else
					{
						activityFeedCardAdapter.swapCursor(result);
					}
					
					if(isAdded() && isVisible())
					{
						UpdateActivityFeedsTask updateActivityFeedTask = new UpdateActivityFeedsTask();

						if (Utils.isHoneycombOrHigher())
						{
							updateActivityFeedTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						}
						else
						{
							updateActivityFeedTask.execute();
						}
					}
				}
			});
		}

	}

	private class UpdateActivityFeedsTask extends AsyncTask<Void, Void, Void>
	{

		@Override
		protected Void doInBackground(Void... params)
		{
			HikeConversationsDatabase.getInstance().updateActivityFeedReadStatus();
			return null;
		}

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

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(getResources().getString(R.string.activity_feed_actionbar_title));

		TextView subText = (TextView) actionBarView.findViewById(R.id.subtext);
		subText.setVisibility(View.GONE);

		
		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if (isAdded() && isVisible())
				{
					int count = getFragmentManager().getBackStackEntryCount();
					if (count == 0)
					{
						getActivity().onBackPressed();
					}
					else
					{
						getFragmentManager().popBackStack();
					}
				}
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		menu.clear();
	}
	
}
