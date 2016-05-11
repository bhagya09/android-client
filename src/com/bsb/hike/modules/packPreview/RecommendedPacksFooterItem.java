package com.bsb.hike.modules.packPreview;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.bsb.hike.R;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants;
import com.bsb.hike.modules.stickersearch.StickerSearchUtils;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

import java.util.List;

/**
 * Created by anubhavgupta on 10/02/16.
 */
public class RecommendedPacksFooterItem extends BasePackPreviewAdapterItem implements View.OnClickListener
{
	private View recommendedPacksFooterView;

	private RecyclerView rvRecommendedPacksGrid;

	private RecommendedPacksAdapter mRecommendedPacksAdapter;

	private Context mContext;

	private StickerCategory stickerCategory;

	private List<StickerCategory> recommendedPacks;

	private Activity activity;

	public RecommendedPacksFooterItem(Context context, Activity activity, StickerCategory stickerCategory)
	{
		this.mContext = context;
		this.activity = activity;
		this.stickerCategory = stickerCategory;
		this.recommendedPacks = stickerCategory.getSimilarPacks();
		initView();
		registerListener();
	}

	private void initView()
	{
		recommendedPacksFooterView = LayoutInflater.from(mContext).inflate(R.layout.recommended_packs_footer, null, false);
		rvRecommendedPacksGrid = (RecyclerView) recommendedPacksFooterView.findViewById(R.id.rvRecommendedPacksGrid);
		setDetails();
	}

	private void setDetails()
	{
		setRvHeight();
		LinearLayoutManager recommendedPacksLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
		mRecommendedPacksAdapter = new RecommendedPacksAdapter(mContext, this);
		rvRecommendedPacksGrid.setLayoutManager(recommendedPacksLayoutManager);
		rvRecommendedPacksGrid.setAdapter(mRecommendedPacksAdapter);
		mRecommendedPacksAdapter.setRecommendedPacks(recommendedPacks);
		mRecommendedPacksAdapter.notifyDataSetChanged();
	}

	private void setRvHeight()
	{
		int height = StickerSearchUtils.getStickerSize() + Utils.dpToPx(10) + Utils.dpToPx(6) + Utils.spToPx(12);
		rvRecommendedPacksGrid.getLayoutParams().height = height;
	}

	@Override
	public View getView()
	{
		return recommendedPacksFooterView;
	}

	@Override
	public RecyclerView.ViewHolder getViewHolder()
	{
		return new RecommendedPacksFooterItemViewHolder(getView());
	}

	@Override
	public void onClick(View v)
	{
		int position = rvRecommendedPacksGrid.getChildAdapterPosition(v);
		if (position < 0 || position >= recommendedPacks.size())
		{
			return;
		}

		StickerCategory stickerCategory = recommendedPacks.get(position);
		IntentFactory.openPackPreviewIntent(mContext, stickerCategory.getCategoryId(), position, StickerConstants.PackPreviewClickSource.RECOMMENDATION, null);
		activity.finish();
	}

	private void registerListener()
	{
		IntentFilter filter = new IntentFilter(StickerManager.STICKER_PREVIEW_DOWNLOADED);
		LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, filter);
	}

	private void deRegisterListener()
	{
		LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			switch (intent.getAction())
			{
			case StickerManager.STICKER_PREVIEW_DOWNLOADED:
				mRecommendedPacksAdapter.notifyDataSetChanged();
				break;
			}
		}
	};


	@Override
	public void releaseResources()
	{
		deRegisterListener();
	}

	private class RecommendedPacksFooterItemViewHolder extends RecyclerView.ViewHolder
	{

		public RecommendedPacksFooterItemViewHolder(View row)
		{
			super(row);
		}
	}
}
