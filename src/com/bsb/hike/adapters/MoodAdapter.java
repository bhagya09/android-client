package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.timeline.view.StatusUpdate;
import com.bsb.hike.utils.EmoticonConstants;

public class MoodAdapter extends BaseAdapter implements OnItemClickListener
{

	private int moodCount;

	private String[] moodHeadings;

	private LayoutInflater inflater;

	private Context context;

	private final int moodHeight;

	private final int moodWidth;

	private List<Integer> moods;

	public MoodAdapter(Context context, int columns)
	{
		this.inflater = LayoutInflater.from(context);
		this.context = context;
		this.moodHeadings = context.getResources().getStringArray(R.array.mood_headings);
		this.inflater = LayoutInflater.from(context);

		int width = context.getResources().getDisplayMetrics().widthPixels;
		moodWidth = (int) (width / columns);
		moodHeight = moodWidth;

		this.moods = new ArrayList<Integer>();
		this.moods.addAll(EmoticonConstants.moodMapping.keySet());
		Collections.sort(this.moods);
		this.moodCount = moods.size();
	}

	@Override
	public int getCount()
	{
		return moodCount;
	}

	@Override
	public Integer getItem(int position)
	{
		return moods.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (convertView == null)
		{
			convertView = inflater.inflate(R.layout.mood_item, null);
		}

		LayoutParams lp = new LayoutParams(moodWidth, moodHeight);
		convertView.setLayoutParams(lp);

		Pair<Integer, Integer> tag = new Pair<Integer, Integer>(getItem(position), position);
		convertView.setTag(tag);

		ImageView moodImage = (ImageView) convertView.findViewById(R.id.mood);
		TextView moodText = (TextView) convertView.findViewById(R.id.mood_text);

		moodImage.setImageResource(EmoticonConstants.moodMapping.get(getItem(position)));
		moodText.setText(moodHeadings[position]);

		return convertView;
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		@SuppressWarnings("unchecked")
		Pair<Integer, Integer> tag = (Pair<Integer, Integer>) view.getTag();
		((StatusUpdate) context).setMood(tag.first, tag.second);
	}
}
