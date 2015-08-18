package com.bsb.hike.timeline;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import static android.support.v7.widget.RecyclerView.OnScrollListener;

public abstract class EndlessRecyclerScrollListener extends OnScrollListener
{

	private int mPreviousTotal = 0;

	private boolean mLoading = true;

	private int visibleThreshold = 10;

	int firstVisibleItem, visibleItemCount, totalItemCount;

	private int mCurrentPage = 0;

	private LinearLayoutManager mGridLayoutManager;

	public EndlessRecyclerScrollListener(LinearLayoutManager layoutManager)
	{
		mGridLayoutManager = layoutManager;
	}

	@Override
	public void onScrolled(RecyclerView recyclerView, int dx, int dy)
	{
		super.onScrolled(recyclerView, dx, dy);

		visibleItemCount = recyclerView.getChildCount();
		totalItemCount = mGridLayoutManager.getItemCount();
		firstVisibleItem = mGridLayoutManager.findFirstVisibleItemPosition();

		if (mLoading)
		{
			if (totalItemCount > mPreviousTotal)
			{
				mLoading = false;
				mPreviousTotal = totalItemCount;
			}
		}
		if (!mLoading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold))
		{
			mCurrentPage++;

			onLoadMore(mCurrentPage);

			mLoading = true;
		}
	}

	public void reset()
	{
		mCurrentPage = 0;
		mPreviousTotal = 0;
		mLoading = true;
	}

	public abstract void onLoadMore(int current_page);
}
