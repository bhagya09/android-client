package com.bsb.hike.timeline.view;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.timeline.adapter.ActivityFeedCursorAdapter;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.etiennelawlor.quickreturn.library.enums.QuickReturnViewType;
import com.etiennelawlor.quickreturn.library.listeners.QuickReturnRecyclerViewOnScrollListener;

public class ActivityFeedFragment extends Fragment implements Listener
{

	private ActivityFeedCursorAdapter activityFeedCardAdapter;

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
		return parent;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		QuickReturnRecyclerViewOnScrollListener scrollListener = new QuickReturnRecyclerViewOnScrollListener.Builder(QuickReturnViewType.HEADER).isSnappable(false).build();

		mActivityFeedRecyclerView.setOnScrollListener(scrollListener);

		executeActivityFeedFetchTask();
	}

	private void executeActivityFeedFetchTask()
	{
		FetchActivityFeeds fetchUpdates = new FetchActivityFeeds();

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
			executeActivityFeedFetchTask();
		}
	}

	private class FetchActivityFeeds extends AsyncTask<Void, Void, Cursor>
	{

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

					/**
					 * Added this check as to ensure that this call for updating read status only when screen is shown to user i.e in post execute, fragment is Added and visible
					 */
					if (isAdded() && isVisible())
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
		ActionBar actionBar = ((HikeAppStateBaseFragmentActivity) getActivity()).getSupportActionBar();

		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = getActivity().getLayoutInflater().inflate(R.layout.compose_action_bar, null);

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
					getActivity().onBackPressed();
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
