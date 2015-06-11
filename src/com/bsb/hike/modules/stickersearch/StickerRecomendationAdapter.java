package com.bsb.hike.modules.stickersearch;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.smartImageLoader.StickerLoader;
import com.bsb.hike.ui.utils.RecyclingImageView;
import com.jess.ui.TwoWayAbsListView;

public class StickerRecomendationAdapter extends BaseAdapter
{
	private List<Sticker> stickerList;
	
	private StickerLoader stickerLoader;
	
	private Context mContext;
	
	private int sizeEachImage;
	
	public StickerRecomendationAdapter(List<Sticker> stickerList)
	{
		this.stickerList = stickerList;
		this.stickerLoader = new StickerLoader(HikeMessengerApp.getInstance());
		this.mContext = HikeMessengerApp.getInstance();
		
		this.sizeEachImage = StickerSearchUtils.getStickerSize();
		
	}
	
	@Override
	public int getCount()
	{
		return stickerList.size();
	}

	@Override
	public Sticker getItem(int position)
	{
		return stickerList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		Sticker sticker = getItem(position);
		
		ViewHolder viewHolder;
		TwoWayAbsListView.LayoutParams ll = new TwoWayAbsListView.LayoutParams(sizeEachImage, sizeEachImage);
		
		if(convertView == null)
		{
			viewHolder = new ViewHolder();
			convertView = new RecyclingImageView(mContext);
			int padding = mContext.getResources().getDimensionPixelSize(R.dimen.sticker_recommend_sticker_image_padding);
			convertView.setLayoutParams(ll);
			((ImageView) convertView).setScaleType(ScaleType.CENTER_INSIDE);
			((ImageView) convertView).setPadding(padding, padding, padding, padding);
			viewHolder.stickerImage = (RecyclingImageView) convertView;
			convertView.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		stickerLoader.loadImage(sticker.getSmallStickerPath(), viewHolder.stickerImage, false);
		return convertView;
	}
	
	public void setStickerList(List<Sticker> stickerList)
	{
		this.stickerList = stickerList;
	}
	
	private class ViewHolder
	{
		ImageView stickerImage;
	}

}
