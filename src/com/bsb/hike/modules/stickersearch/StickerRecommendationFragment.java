package com.bsb.hike.modules.stickersearch;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.Logger;
import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayAdapterView.OnItemClickListener;
import com.jess.ui.TwoWayGridView;

public class StickerRecommendationFragment extends SherlockFragment
{
	private StickerRecomendationAdapter mAdapter;

	private IStickerRecommendFragmentListener listener;

	private List<Sticker> stickerList;

	private StickerRecommendationFragment(IStickerRecommendFragmentListener listner)
	{
		this.listener = listner;
	}
	
	public static StickerRecommendationFragment newInstance(IStickerRecommendFragmentListener listener, ArrayList<Sticker> stickerList)
	{
		StickerRecommendationFragment fragment = new StickerRecommendationFragment(listener);
		Bundle args = new Bundle();
		args.putParcelableArrayList(HikeConstants.LIST, stickerList);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		stickerList = args.getParcelableArrayList(HikeConstants.LIST);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		
		View parent = inflater.inflate(R.layout.sticker_recommend, container, false);
		
		mAdapter = new StickerRecomendationAdapter(stickerList);
		
		TwoWayGridView gridView = (TwoWayGridView) parent.findViewById(R.id.twoWayView);
		gridView.setColumnWidth(GridView.AUTO_FIT);
		gridView.setAdapter(mAdapter);
		gridView.setOnItemClickListener(getOnItemClickListener());
		gridView.setSelector(R.drawable.sticker_recommend_item_selector);
		
		ImageView close = (ImageView) parent.findViewById(R.id.sticker_recommend_popup_close);
		ImageView settings = (ImageView) parent.findViewById(R.id.sticker_recommend_popup_settings);
		close.setOnClickListener(closeListener);
		settings.setOnClickListener(settingsListener);
		return parent;
	}
	
	@Override
	public void onStop()
	{
		Logger.d("anubhav", "recommend fragment on stop called");
		super.onStop();
	}
	
	@Override
	public void onDestroy()
	{
		Logger.d("anubhav", "recommend fragment on destroy called");
		super.onDestroy();
	}
	private OnClickListener closeListener = new OnClickListener()
	{
		
		@Override
		public void onClick(View v)
		{
			if(listener != null)
			{
				listener.onCloseClicked();
			}
		}
	};
	
	private OnClickListener settingsListener = new OnClickListener()
	{
		
		@Override
		public void onClick(View v)
		{
			if(listener != null)
			{
				listener.onSettingsClicked();
			}
		}
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}

	private OnItemClickListener getOnItemClickListener()
	{
		return new OnItemClickListener()
		{

			@Override
			public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id)
			{
				if(listener == null || stickerList == null || stickerList.size() <= position)
				{
					Logger.wtf(StickerTagWatcher.TAG, "sometghing wrong, sticker can't be selected");
					return ;
				}
				Sticker sticker = stickerList.get(position);
				listener.stickerSelected(sticker);
			}
		};
	}

	public void setAndNotify(List<Sticker> stickerList)
	{
		this.stickerList = stickerList;
		
		if (mAdapter != null)
		{
			mAdapter.setStickerList(stickerList);
			mAdapter.notifyDataSetChanged();
		}
	}
}
