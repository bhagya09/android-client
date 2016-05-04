package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.FileListAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.FileListItem;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

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
	
	private boolean mLoadingMoreFiles;
	
	private boolean mReachedEnd;
	
	String msisdn;
		
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.file_select_layout);

		msisdn = getIntent().getStringExtra(HikeConstants.Extras.MSISDN);
		items = (ArrayList<FileListItem>) HikeConversationsDatabase.getInstance().getSharedMedia(msisdn, HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY, -1, false);

		listAdapter = new FileListAdapter(this, items);
		emptyView = (TextView) findViewById(R.id.search_empty_view);
		listView = (ListView) findViewById(R.id.file_list);
		listView.setEmptyView(emptyView);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				FileListItem item = items.get(i);
				HikeFile.openFile(item.getFile(), item.getHikeSharedFile().getFileTypeString(), SharedOtherFilesActivity.this);
			}
		});

		listView.setOnScrollListener(this);
		setupActionBar();
	}

	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if(listAdapter != null)
		{
			listAdapter.getFileImageLoader().setExitTasksEarly(true);
		}
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);


		title = (TextView) actionBarView.findViewById(R.id.title);
		subText = (TextView) actionBarView.findViewById(R.id.subtext);

		setTitle(getString(R.string.shared_files));

		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);
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
		if(listAdapter != null)
		{
			listAdapter.getFileImageLoader().setExitTasksEarly(false);
			listAdapter.notifyDataSetChanged();
		}
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
		
		if (!mReachedEnd && !mLoadingMoreFiles && items != null && !items.isEmpty() && (firstVisibleItem + visibleItemCount) <= totalItemCount - 5)
		{
			mLoadingMoreFiles = true;

			AsyncTask<Void, Void, List<FileListItem>> asyncTask = new AsyncTask<Void, Void, List<FileListItem>>()
			{
				@Override
				protected List<FileListItem> doInBackground(Void... params)
				{
					long maxMsgId = items.get(items.size() - 1).getHikeSharedFile().getMsgId();
					return (List<FileListItem>) HikeConversationsDatabase.getInstance().getSharedMedia(msisdn, HikeConstants.MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY, maxMsgId, false);
				}

				@Override
				protected void onPostExecute(List<FileListItem> result)
				{
					if (!result.isEmpty())
					{
						items.addAll(result);
						listAdapter.notifyDataSetChanged();
					}
					else
					{
						mReachedEnd = true;
					}
					mLoadingMoreFiles = false;
				}
			};
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		listAdapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES && scrollState == OnScrollListener.SCROLL_STATE_FLING);
	}
}
