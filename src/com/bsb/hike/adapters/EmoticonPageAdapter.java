package com.bsb.hike.adapters;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.EmoticonPickerListener;
import com.bsb.hike.utils.Logger;

public class EmoticonPageAdapter extends BaseAdapter implements OnClickListener
{
	
	LayoutInflater inflater;

	int currentPage;

	int offset;

	private List<Integer> recentEmoticons  = new ArrayList<Integer>();

	private int[] emoticonSubCategories;

	private int[] emoticonResIds;

	private int idOffset;

	Context context;
	EmoticonPickerListener listener;
	

	public EmoticonPageAdapter(Context context, int[] emoticonSubCategories, int[] emoticonResIds, int currentPage, int idOffset, EmoticonPickerListener listener)
	{
		this.listener = listener;
		this.context = context;
		this.currentPage = currentPage;
		this.inflater = LayoutInflater.from(context);
		this.emoticonSubCategories = emoticonSubCategories;
		this.emoticonResIds = emoticonResIds;
		this.idOffset = idOffset;

		/*
		 * There will be a positive offset for subcategories having a greater than 1 index.
		 */
		if (currentPage > 1)
		{
			for (int i = 0; i <= currentPage - 2; i++)
			{
				this.offset += emoticonSubCategories[i];
			}
		}
		else if (currentPage == 0)
		{
			int startOffset = idOffset;
			int endOffset = startOffset + emoticonResIds.length;

			int [] arr = HikeConversationsDatabase.getInstance().fetchEmoticonsOfType(startOffset, endOffset, -1);
			for(int i:arr)
			{
				recentEmoticons.add(i);
			}
		}
	}

	@Override
	public int getCount()
	{
		if (currentPage == 0)
		{
			return recentEmoticons.size();
		}
		else
		{
			return emoticonSubCategories[currentPage - 1];
		}
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
			convertView = inflater.inflate(R.layout.emoticon_item, parent, false);
		}

		if (currentPage == 0)
		{
			convertView.setTag(Integer.valueOf(recentEmoticons.get(position)));
			((ImageView) convertView).setImageResource(emoticonResIds[recentEmoticons.get(position) - idOffset]);
		}
		else
		{
			convertView.setTag(Integer.valueOf(idOffset + offset + position));
			((ImageView) convertView).setImageResource(emoticonResIds[offset + position]);
		}
		convertView.setOnClickListener(this);
		return convertView;
	}

	@Override
	public void onClick(View v)
	{
		Logger.i("emoticon", "item clicked");
		int emoticonIndex = (Integer) v.getTag();
		updateRecentsPalette(emoticonIndex);
		listener.emoticonSelected(emoticonIndex);

	}

	/**
	 * This is to update the recent emoticons palette on sending any emoticons.
	 * 
	 * @param emoticonIndex
	 */
	private void updateRecentsPalette(int emoticonIndex)
	{
		if (!recentEmoticons.contains(emoticonIndex))
		{
			recentEmoticons.add(0, emoticonIndex);
		}
		else
		{
			recentEmoticons.remove((Object)emoticonIndex);
			recentEmoticons.add(0, emoticonIndex);
		}		
	}
}
