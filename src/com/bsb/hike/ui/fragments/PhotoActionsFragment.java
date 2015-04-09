package com.bsb.hike.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bsb.hike.R;

public class PhotoActionsFragment extends SherlockFragment
{
	private View mFragmentView;

	private String[] mTitles;

	private String[] mDescription;

	private int itemIcons[] = { R.drawable.set_icon, R.drawable.send_icon };

	private ActionListener mListener;

	public static final int ACTION_SET_DP = 1;

	public static final int ACTION_SEND = 2;

	public static interface ActionListener
	{
		void onAction(int actionCode);
	}

	public PhotoActionsFragment(ActionListener argListener)
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

		View divider = new View(getActivity());

		final View view2 = mAdapter.getView(1, null, null);

		view1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

		divider.setBackgroundColor(getResources().getColor(R.color.file_transfer_pop_up_button));

		divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));

		view2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

		view1.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				mListener.onAction(ACTION_SET_DP);
				view1.setEnabled(false);
				view2.setEnabled(false);
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
			}
		});

		LinearLayout itemsLayout = (LinearLayout) mFragmentView.findViewById(R.id.itemsLayout);

		itemsLayout.addView(view1);

		itemsLayout.addView(divider);

		itemsLayout.addView(view2);

		return mFragmentView;
	}

	private void loadData()
	{
		mTitles = getActivity().getResources().getStringArray(R.array.photos_actions_titles);

		mDescription = getActivity().getResources().getStringArray(R.array.photos_actions_description);
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
