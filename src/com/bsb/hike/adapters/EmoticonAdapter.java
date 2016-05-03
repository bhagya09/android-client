package com.bsb.hike.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.bsb.hike.R;
import com.bsb.hike.media.EmoticonPickerListener;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.SmileyParser;
import com.viewpagerindicator.IconPagerAdapter;

public class EmoticonAdapter extends PagerAdapter implements IconPagerAdapter
{

	public final int MAX_EMOTICONS_PER_ROW;

	public static final int MAX_EMOTICONS_PER_ROW_PORTRAIT = 7;

	public static final int MAX_EMOTICONS_PER_ROW_LANDSCAPE = 10;

	public static final int RECENTS_SUBCATEGORY_INDEX = -1;

	private LayoutInflater inflater;

	private Context context;

	private EmoticonPickerListener listener;

	private int[] emoticonResIds;

	private int[] emoticonSubCategories;

	private int[] categoryResIds;

	private int idOffset;

	public EmoticonAdapter(Context context, EmoticonPickerListener listener, boolean isPortrait, int[] categoryResIds)
	{
		this(context, listener, isPortrait, categoryResIds, false);
	}

	public EmoticonAdapter(Context context, EmoticonPickerListener listener, boolean isPortrait, int[] categoryResIds, boolean emojiOnly)
	{
		MAX_EMOTICONS_PER_ROW = isPortrait ? MAX_EMOTICONS_PER_ROW_PORTRAIT : MAX_EMOTICONS_PER_ROW_LANDSCAPE;

		this.inflater = LayoutInflater.from(context);
		this.context = context;
		this.listener = listener;
		this.categoryResIds = categoryResIds;

		emoticonResIds = emojiOnly ? EmoticonConstants.EMOJI_RES_IDS : EmoticonConstants.DEFAULT_SMILEY_RES_IDS;
		emoticonSubCategories = emojiOnly ? SmileyParser.EMOJI_SUBCATEGORIES : SmileyParser.EMOTICONS_SUBCATEGORIES;

		if (emojiOnly)
		{
			for (int i : SmileyParser.HIKE_SUBCATEGORIES)
			{
				idOffset += i;
			}
		}
	}

	@Override
	public int getCount()
	{
		return emoticonSubCategories.length + 1;
	}

	@Override
	public boolean isViewFromObject(View view, Object object)
	{
		return view == object;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object)
	{
		((ViewPager) container).removeView((View) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		View emoticonPage = inflater.inflate(R.layout.emoticon_page, null);

		GridView emoticonGrid = (GridView) emoticonPage.findViewById(R.id.emoticon_grid);
		emoticonGrid.setNumColumns(MAX_EMOTICONS_PER_ROW);
		emoticonGrid.setVerticalScrollBarEnabled(false);
		emoticonGrid.setHorizontalScrollBarEnabled(false);
		emoticonGrid.setAdapter(new EmoticonPageAdapter(context, emoticonSubCategories, emoticonResIds, position, idOffset, listener));
		emoticonPage.setTag(position);

		((ViewPager) container).addView(emoticonPage);
		return emoticonPage;
	}

	@Override
	public int getIconResId(int index)
	{
		return categoryResIds[index];
	}

	/**
	 * This is to update the recent emoticons palette, because of caching.
	 * 
	 * @param view
	 * @param position
	 */
	public void refreshView(View view, int position)
	{
		GridView grid = (GridView) view.findViewById(R.id.emoticon_grid);
		grid.setAdapter(new EmoticonPageAdapter(context, emoticonSubCategories, emoticonResIds, position, idOffset, listener));
	}

	@Override
	public String getIconContentDescription(int index)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
