package com.bsb.hike.gallery;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;

public class GalleryViewHolder extends RecyclerView.ViewHolder {

	public ImageView galleryThumb;
	public TextView galleryName;
	public View selected;
	public TextView galleryCount;
	public ViewGroup contentLayout;
	public ViewGroup albumLayout;

	public GalleryViewHolder(View view) {
		super(view);
		galleryName = (TextView) view.findViewById(R.id.album_title);
		galleryCount = (TextView) view.findViewById(R.id.album_count);
		galleryThumb = (ImageView) view.findViewById(R.id.album_image);
		contentLayout = (ViewGroup) view.findViewById(R.id.contentLayout);
		albumLayout = (ViewGroup) view.findViewById(R.id.album_layout);
		selected = view.findViewById(R.id.selected);
	}
}
