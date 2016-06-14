package com.bsb.hike.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.support.v4.app.Fragment;
import com.bsb.hike.R;

public class PhotoActionsFragment extends Fragment
{
	private View mFragmentView;

	private String[] mTitles;

	private int itemIcons[] = { R.drawable.ic_profile_picture, R.drawable.ic_send_friend, R.drawable.ic_camera };

	private ActionListener mListener;

	public static final int ACTION_SET_DP = 1;

	public static final int ACTION_SEND = 2;

	public static final int ACTION_POST = 3;

	public interface ActionListener
	{
		void onAction(int actionCode);
	}

	//Default Constructor as per android guidelines
	public PhotoActionsFragment()
	{
		
	}
	
	public void setActionListener(ActionListener argListener)
	{
		mListener = argListener;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		mFragmentView = inflater.inflate(R.layout.photos_action_fragment, null);

		loadData();

		PhotoActionsListAdapter mAdapter = new PhotoActionsListAdapter();

		final View view1 = mAdapter.getView(0, null, null);
		view1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

		final View view2 = mAdapter.getView(1, null, null);
		view2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

		final View view3 = mAdapter.getView(2, null, null);
		view3.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

		view1.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mListener.onAction(ACTION_SET_DP);
				view1.setEnabled(false);
				view2.setEnabled(false);
				view3.setEnabled(false);
			}
		});
		
		view2.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				mListener.onAction(ACTION_SEND);
				view1.setEnabled(false);
				view2.setEnabled(false);
				view3.setEnabled(false);
			}
		});
		
		view3.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				mListener.onAction(ACTION_POST);
				view1.setEnabled(false);
				view2.setEnabled(false);
				view3.setEnabled(false);
			}
		});

		LinearLayout itemsLayout = (LinearLayout) mFragmentView.findViewById(R.id.itemsLayout);

		itemsLayout.addView(view3);

		itemsLayout.addView(view1);

		itemsLayout.addView(view2);

		return mFragmentView;
	}

	private void loadData()
	{
		mTitles = getActivity().getResources().getStringArray(R.array.photos_actions_titles);
	}

	class PhotoActionsListAdapter extends BaseAdapter
	{

		private LayoutInflater inflater;

		public PhotoActionsListAdapter()
		{
			inflater = LayoutInflater.from(getActivity().getApplicationContext());
		}

		@Override
		public int getCount()
		{
			return mTitles.length;
		}

		@Override
		public Object getItem(int position)
		{
			return null;
		}

		@Override
		public long getItemId(int position)
		{
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
			{
				convertView = inflater.inflate(R.layout.photos_action_list_item, null);

				PhotosOptionsViewHolder holder = new PhotosOptionsViewHolder();

				holder.titleTv = (TextView) convertView.findViewById(R.id.title);

				holder.iconIv = (ImageView) convertView.findViewById(R.id.icon);

				convertView.setTag(holder);
			}

			PhotosOptionsViewHolder holder = (PhotosOptionsViewHolder) convertView.getTag();

			holder.titleTv.setText(mTitles[position]);

			holder.iconIv.setImageDrawable(getResources().getDrawable(itemIcons[position]));

			return convertView;
		}

		class PhotosOptionsViewHolder
		{
			TextView titleTv;

			ImageView iconIv;
		}
	}

}
