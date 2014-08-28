package com.bsb.hike.ui;

import java.util.ArrayList;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.FileListAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.FileListItem;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

public class SharedOtherFilesActivity extends HikeAppStateBaseFragmentActivity implements OnScrollListener, HikePubSub.Listener
{

	private ListView listView;

	private FileListAdapter listAdapter;

	private TextView emptyView;

	private ArrayList<FileListItem> items;

	private int previousFirstVisibleItem;

	private int velocity;

	private long previousEventTime;

	private TextView title;

	private TextView subText;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.file_select_layout);

		String msisdn = getIntent().getStringExtra(HikeConstants.Extras.MSISDN);
		items = (ArrayList<FileListItem>) HikeConversationsDatabase.getInstance().getSharedMedia(msisdn, HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY, -1, false);

		listAdapter = new FileListAdapter(this, items);
		emptyView = (TextView) findViewById(R.id.search_empty_view);
		listView = (ListView) findViewById(R.id.file_list);
		listView.setEmptyView(emptyView);
		listView.setAdapter(listAdapter);

		setupActionBar();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		title = (TextView) actionBarView.findViewById(R.id.title);
		subText = (TextView) actionBarView.findViewById(R.id.subtext);

		setTitle(getString(R.string.shared_files));

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	private void setTitle(String titleString)
	{
		title.setText(titleString);
		subText.setVisibility(View.GONE);
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (previousFirstVisibleItem != firstVisibleItem)
		{
			long currTime = System.currentTimeMillis();
			long timeToScrollOneElement = currTime - previousEventTime;
			velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

			previousFirstVisibleItem = firstVisibleItem;
			previousEventTime = currTime;
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		listAdapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES && scrollState == OnScrollListener.SCROLL_STATE_FLING);
	}
}
