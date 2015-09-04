package com.bsb.hike.adapters;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.InputType;
import android.text.format.Formatter;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.ui.ApkSelectionActivity;
import com.bsb.hike.ui.ApkSelectionActivity.ApplicationSelectionStatus;
public class ApkExplorerListAdapter extends BaseAdapter{
    
	private Context mContext;
    private ArrayList<ApplicationSelectionStatus> appInfo;
    private LayoutInflater mInflater ;
	
	public ApkExplorerListAdapter(Context mContext, ArrayList<ApplicationSelectionStatus> apkInfo)
	{
		this.mContext = mContext;
		this.appInfo = apkInfo;
		this.mInflater = LayoutInflater.from(mContext);
	}

    
	@Override
	public int getCount()
	{
		if(appInfo!=null)
		{
			return appInfo.size();
		}
		return 0;
	}

	@Override
	public Object getItem(int position)
	{
		if (appInfo != null)
		{
			return appInfo.get(position);
		}
		return null;
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}
    
    class ViewHolder
	{
    	TextView apkName;

		TextView apkSize;

		ImageView apkImage;

		CheckBox  apkSelection;
		
		public ViewHolder(TextView apkName, TextView apkSize, ImageView apkImage, CheckBox apkSelection)
		{
			super();
			this.apkName = apkName;
			this.apkSize = apkSize;
			this.apkImage = apkImage;
			this.apkSelection = apkSelection;
		}

		public TextView getApkName()
		{
			return this.apkName;
		}

		public TextView getApkSize()
		{
			return this.apkSize;
		}

		public ImageView getApkImage()
		{
			return this.apkImage;
    	}

		public CheckBox getApkSelection()
		{
			return this.apkSelection;
		}
		
	}
    
	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		ViewHolder listHolder;
		TextView apkName;
		TextView apkSize;
		ImageView apkImage;
		CheckBox apkSelection;
		if (convertView == null)
		{
			convertView = mInflater.inflate(R.layout.apk_list_single, parent, false);
			apkName = (TextView) convertView.findViewById(R.id.apk_name);
			apkSelection = (CheckBox) convertView.findViewById(R.id.apk_selection_box);
			apkSize = (TextView) convertView.findViewById(R.id.apk_size);
			apkImage = (ImageView) convertView.findViewById(R.id.apk_image);
			listHolder = new ViewHolder(apkName, apkSize, apkImage, apkSelection);
			convertView.setTag(listHolder);
		}
		else
		{
			listHolder = (ViewHolder) convertView.getTag();
			apkName = listHolder.getApkName();
			apkImage = listHolder.getApkImage();
			apkSelection = listHolder.getApkSelection();
			apkSize = listHolder.getApkSize();
		}
		apkSelection.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{

				((ApkSelectionActivity) mContext).toggleSelection(position);
			}
		});

		apkImage.setImageDrawable(appInfo.get(position).getApplicationInfo().loadIcon(mContext.getPackageManager()));
		apkName.setText(mContext.getPackageManager().getApplicationLabel(appInfo.get(position).getApplicationInfo()));
		File apkFile = new File(appInfo.get(position).getApplicationInfo().sourceDir);
		if (apkFile != null)
			apkSize.setText(Formatter.formatFileSize(mContext, apkFile.length()));

		if (appInfo.get(position).getApplicationSelectionStatus())
		{
			apkSelection.setChecked(true);
		}
		else
		{
			apkSelection.setChecked(false);
		}

		return convertView;
	}
}