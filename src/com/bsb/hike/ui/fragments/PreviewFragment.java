package com.bsb.hike.ui.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.photos.HikePhotosUtils;
import com.bsb.hike.photos.HikePhotosUtils.FilterTools.FilterList;
import com.bsb.hike.photos.HikePhotosUtils.MenuType;
import com.bsb.hike.photos.views.DoodleEffectItemLinearLayout;
import com.bsb.hike.photos.views.FilterEffectItemLinearLayout;
import com.bsb.hike.ui.PictureEditer;
import com.bsb.hike.ui.PictureEditer.EditorClickListener;
import com.jess.ui.TwoWayGridView;

public final class PreviewFragment extends Fragment
{

	private EditorClickListener handler;

	private Bitmap mOriginalBitmap;

	private ImageAdapter mAdapter;

	private int menuType;
	
	private int height;

	private static final String MENU_TYPE_KEY = "MENU_TYPE_KEY";

	private static final String BITMAP_KEY = "BITMAP_KEY";
	
	private static final String MENU_HEIGHT_KEY = "MENU_HEIGHT_KEY";

	//Default Constructor as per android guidelines
	public PreviewFragment()
	{
		
	}
	
	public static PreviewFragment newInstance(int type, Bitmap bitmap)
	{
		PreviewFragment newFrag = new PreviewFragment();
		Bundle newFragBundle = new Bundle();
		newFragBundle.putInt(MENU_TYPE_KEY, type);
		newFragBundle.putParcelable(BITMAP_KEY, bitmap);
		newFrag.setArguments(newFragBundle);
		return newFrag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Bundle newFragBundle = getArguments();

		menuType = newFragBundle.getInt(MENU_TYPE_KEY);

		mOriginalBitmap = newFragBundle.getParcelable(BITMAP_KEY);
		
		if(savedInstanceState != null)
		{
			height = savedInstanceState.getInt(MENU_HEIGHT_KEY);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		//adding null check since click handler becomes null if PictureEditor is restarted by the OS
		if (handler == null && getActivity() instanceof PictureEditer)
		{
			handler = ((PictureEditer) getActivity()).getClickHandler();
		}

		if(height==0)
		{
			height = container.getMeasuredHeight();
		}
		
		LinearLayout layout = (LinearLayout) LayoutInflater.from(getActivity()).inflate(R.layout.photos_pager_layout, container, false);

		TwoWayGridView gridView = (TwoWayGridView) layout.findViewById(R.id.HorizontalGridView);
		// gridView.setLayoutParams(new TwoWayAbsListView.LayoutParams(TwoWayAbsListView.LayoutParams.MATCH_PARENT, HikePhotosUtils.dpToPx(getActivity().getApplicationContext(),
		// height)));
		gridView.setColumnWidth(GridView.AUTO_FIT);
		gridView.setRowHeight(height);
		gridView.setOnItemClickListener(handler);
		mAdapter = new ImageAdapter(handler);
		gridView.setAdapter(mAdapter);
		ViewStub adjuster = (ViewStub) layout.findViewById(R.id.sizeBarStub);
		switch (menuType)
		{
		case MenuType.DOODLE_TYPE:
			layout.setWeightSum(HikeConstants.HikePhotos.PHOTOS_PAGER_DOODLE_WEIGHT_SUM);
			RelativeLayout sizeBar = (RelativeLayout) adjuster.inflate();
			sizeBar.findViewById(R.id.plusWidth).setOnClickListener(handler);
			sizeBar.findViewById(R.id.minusWidth).setOnClickListener(handler);
			ViewStub stub = (ViewStub) sizeBar.findViewById(R.id.viewStubPreview);
			DoodleEffectItemLinearLayout inflated = (DoodleEffectItemLinearLayout) stub.inflate();
			inflated.setBrushColor(HikePhotosUtils.DoodleColors[0]);
			inflated.setBrushWidth(HikePhotosUtils.dpToPx(HikeConstants.HikePhotos.DEFAULT_BRUSH_WIDTH));
			inflated.setPadding(0, 0, 0, 0);
			inflated.setRingColor(HikeConstants.HikePhotos.DEFAULT_RING_COLOR);
			inflated.refresh();
			inflated.invalidate();
			handler.setDoodlePreview(inflated);
			break;
		case MenuType.EFFECTS_TYPE:
			layout.setWeightSum(HikeConstants.HikePhotos.PHOTOS_PAGER_FILTER_WEIGHT_SUM);
			adjuster.setVisibility(View.GONE);
			break;
		case MenuType.BORDER_TYPE:
			break;
		case MenuType.QUALITY_TYPE:
			break;
		case MenuType.TEXT_TYPE:
			break;
		default:
			break;
		}
		layout.invalidate();
		HikePhotosUtils.FilterTools.setSelectedColor(HikePhotosUtils.DoodleColors[0]);
		HikePhotosUtils.FilterTools.setSelectedFilter(FilterList.getHikeEffects().filters.get(0));
		return layout;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(MENU_HEIGHT_KEY, height);
		super.onSaveInstanceState(outState);
	}

	class ImageAdapter extends BaseAdapter
	{

		private Context mContext;

		public ImageAdapter(EditorClickListener adapter)
		{
			mContext = HikeMessengerApp.getInstance().getApplicationContext();
		}

		@Override
		public int getCount()
		{
			int count = 0;
			switch (menuType)
			{
			case MenuType.EFFECTS_TYPE:
				count = FilterList.getHikeEffects().filters.size();
				break;
			case MenuType.DOODLE_TYPE:
				count = HikePhotosUtils.DoodleColors.length;
				break;
			}
			return count;
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
		public int getItemViewType(int position)
		{
			switch (menuType)
			{
			case MenuType.EFFECTS_TYPE:
				return 0;
			case MenuType.DOODLE_TYPE:
				return 1;
			}
			return 0;
		}

		// Convert DP to PX
		// Source: http://stackoverflow.com/a/8490361

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			switch (menuType)
			{
			case MenuType.EFFECTS_TYPE:
				if (convertView == null)
				{
					convertView = LayoutInflater.from(mContext).inflate(R.layout.filter_preview_item, parent, false);
				}
				FilterList myFilters = FilterList.getHikeEffects();
				String filterName = myFilters.names.get(position);
				Object tagFilterName = convertView.getTag();
				if (tagFilterName != null && ((String) tagFilterName).equals(filterName))
				{
					return convertView;
				}
				else
				{
					if (HikePhotosUtils.FilterTools.getCurrentFilterItem() != null)
					{
						String existingTag = ((String) HikePhotosUtils.FilterTools.getCurrentFilterItem().getTag());

						if (existingTag != null && existingTag.equals(filterName))
						{
							FilterEffectItemLinearLayout filterPreviewView = (FilterEffectItemLinearLayout) convertView;
							filterPreviewView.init(mOriginalBitmap, myFilters.names.get(position));
							filterPreviewView.setFilter(mContext, myFilters.filters.get(position), false);
							convertView.setTag(filterName);
							return convertView;
						}
					}
					FilterEffectItemLinearLayout filterPreviewView = (FilterEffectItemLinearLayout) convertView;
					filterPreviewView.init(mOriginalBitmap, myFilters.names.get(position));
					filterPreviewView.setFilter(mContext, myFilters.filters.get(position), true);
					convertView.setTag(filterName);
				}

				return convertView;
			case MenuType.BORDER_TYPE:
				break;
			case MenuType.TEXT_TYPE:
				break;
			case MenuType.DOODLE_TYPE:
				if (convertView == null)
				{
					convertView = LayoutInflater.from(mContext).inflate(R.layout.doodle_preview_item, parent, false);
				}
				int currentPosColor = HikePhotosUtils.DoodleColors[position];
				Object tagColor = convertView.getTag();
				if (tagColor != null && ((Integer) tagColor) == currentPosColor)
				{
					return convertView;
				}
				else
				{
					if (HikePhotosUtils.FilterTools.getCurrentDoodleItem() != null)
					{
						Integer existingColor = ((Integer) HikePhotosUtils.FilterTools.getCurrentDoodleItem().getTag());

						if (existingColor != null && existingColor == currentPosColor)
						{
							DoodleEffectItemLinearLayout doodleItem = (DoodleEffectItemLinearLayout) convertView;
							doodleItem.setBrushColor(currentPosColor, false);
							doodleItem.refresh();
							convertView.setTag(currentPosColor);
							return convertView;
						}
					}

					DoodleEffectItemLinearLayout doodleItem = (DoodleEffectItemLinearLayout) convertView;
					doodleItem.setBrushColor(currentPosColor, true);
					doodleItem.refresh();
					convertView.setTag(currentPosColor);
				}
				break;
			case MenuType.QUALITY_TYPE:
				break;
			}

			return convertView;

		}
	}
}
