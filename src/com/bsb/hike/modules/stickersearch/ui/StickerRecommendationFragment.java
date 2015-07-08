package com.bsb.hike.modules.stickersearch.ui;

import static com.bsb.hike.modules.stickersearch.StickerSearchConstants.SCROLL_SPEED_PER_DIP;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.modules.stickersearch.listeners.IStickerRecommendFragmentListener;
import com.bsb.hike.ui.fragments.StickerShopFragment;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class StickerRecommendationFragment extends SherlockFragment implements Listener
{
	
	private String[] pubSubListeners = {HikePubSub.STICKER_DOWNLOADED};
	
	private StickerRecomendationAdapter mAdapter;

	private static IStickerRecommendFragmentListener listener;

	private List<Sticker> stickerList;

	private RecyclerView recyclerView;
	
	private RecyclerView.LayoutManager mLayoutManager;
	
	private int MIN_STICKER_LIST_SIZE_FOR_SCROLL, SCROLL_TO_POSITION;
	
	public static StickerRecommendationFragment newInstance(IStickerRecommendFragmentListener lIStickerRecommendFragmentListener, ArrayList<Sticker> stickerList)
	{
		StickerRecommendationFragment fragment = new StickerRecommendationFragment();
		listener = lIStickerRecommendFragmentListener;
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
		MIN_STICKER_LIST_SIZE_FOR_SCROLL = StickerManager.getInstance().getNumColumnsForStickerGrid(getActivity()) + 1;
		SCROLL_TO_POSITION = MIN_STICKER_LIST_SIZE_FOR_SCROLL - 1;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		View parent = inflater.inflate(R.layout.sticker_recommend, container, false);
		
		recyclerView = (RecyclerView) parent.findViewById(R.id.recyclerView);
		mLayoutManager = new  CustomLinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false, SCROLL_SPEED_PER_DIP);
	    recyclerView.setLayoutManager(mLayoutManager);
	    
	    mAdapter = new StickerRecomendationAdapter(stickerList, this);
		recyclerView.setAdapter(mAdapter);
		
		setClickListeners(parent);

		return parent;
	}

	private void setClickListeners(View parent)
	{
		View close = parent.findViewById(R.id.sticker_recommend_popup_close);
		View settings = parent.findViewById(R.id.sticker_recommend_popup_settings);
		close.setOnClickListener(closeListener);
		settings.setOnClickListener(settingsListener);
		
	}
	@Override
	public void onStop()
	{
		Logger.d(StickerTagWatcher.TAG, "recommend fragment on stop called");
		super.onStop();
	}

	@Override
	public void onDestroy()
	{
		Logger.d(StickerTagWatcher.TAG, "recommend fragment on destroy called");
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	private OnClickListener closeListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			if (listener != null)
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
			if (listener != null)
			{
				listener.onSettingsClicked();
			}
		}
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		HikeMessengerApp.getPubSub().addListeners(StickerRecommendationFragment.this, pubSubListeners);
	}

	public void onClick(View view) 
	{
		
		
	}
	
	public void setAndNotify(List<Sticker> stickerList)
	{
		this.stickerList = stickerList;

		if (mAdapter != null && recyclerView != null)
		{
			recyclerView.removeAllViews();
			mAdapter.setStickerList(stickerList);
			mAdapter.notifyDataSetChanged();
		}
	}

	public boolean showFtueAnimation()
	{
		if (recyclerView != null && stickerList != null && stickerList.size() >= MIN_STICKER_LIST_SIZE_FOR_SCROLL)
		{
			recyclerView.smoothScrollToPosition(SCROLL_TO_POSITION);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public void click(View view)
	{
		int position = recyclerView.getChildPosition(view);
		if (listener == null || stickerList == null || stickerList.size() <= position)
		{
			Logger.wtf(StickerTagWatcher.TAG, "sometghing wrong, sticker can't be selected");
			return;
		}
		Sticker sticker = stickerList.get(position);
		listener.stickerSelected(sticker);
	}
	
	private void refreshStickerList()
	{
		if(!isAdded())
		{
			return;
		}
		getSherlockActivity().runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				if(mAdapter == null)
				{
					return;
				}
				mAdapter.notifyDataSetChanged();
			}
		});
	}
	
	@Override
	public void onEventReceived(String type, Object object)
	{
		switch (type)
		{
		case HikePubSub.STICKER_DOWNLOADED:
			refreshStickerList();
			break;
		default:
			break;
		}
	}
}
