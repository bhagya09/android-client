package com.bsb.hike.chatHead;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.AppConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.ui.HikeAuthActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class StickerShareSettings extends Activity
{

	class ListViewItem
	{
		String appName;

		Boolean appChoice;

		Drawable appIcon;

		String pkgName;
	}
	
	boolean selectAll = true;
	
	boolean deSelectAll = true;   
	

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.settings_sticker_share);

		final ArrayList<ListViewItem> mListViewItems = new ArrayList<ListViewItem>();

		JSONArray jsonObj;
		int i;

		try
		{
			jsonObj = new JSONArray(HikeSharedPreferenceUtil.getInstance().getData(HikeConstants.ChatHead.PACKAGE_LIST, HikeConstants.Extras.WHATSAPP_PACKAGE));

			for (i = 0; i < jsonObj.length(); i++)
			{
				JSONObject obj = jsonObj.getJSONObject(i);

				if (Utils.isPackageInstalled(getApplicationContext(), obj.getString(HikeConstants.ChatHead.PACKAGE_NAME)))
				{
					ListViewItem listItem = new ListViewItem();
					listItem.appName = obj.getString(HikeConstants.ChatHead.APP_NAME);
					listItem.appChoice = obj.getBoolean(HikeConstants.ChatHead.APP_ENABLE);
					listItem.pkgName = obj.getString(HikeConstants.ChatHead.PACKAGE_NAME);
					try
					{
						listItem.appIcon = getPackageManager().getApplicationIcon(obj.getString(HikeConstants.ChatHead.PACKAGE_NAME));
					}
					catch (NameNotFoundException e)
					{
						e.printStackTrace();
					}
					mListViewItems.add(listItem);
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
       
		ArrayAdapter<ListViewItem> listAdapter = new ArrayAdapter<ListViewItem>(this, R.layout.settings_sticker_share_item, mListViewItems)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				ListViewItem listItem = new ListViewItem();
				listItem = mListViewItems.get(position);
				if (convertView == null)
				{
					LayoutInflater inflater = getLayoutInflater();
					convertView = inflater.inflate(R.layout.settings_sticker_share_item, null);
				}
				ImageView imgView = (ImageView) convertView.findViewById(R.id.app_icon);
				TextView txtView = (TextView) convertView.findViewById(R.id.app_name);
				CheckBox ckBox = (CheckBox) convertView.findViewById(R.id.checkbox_item);
				imgView.setBackground(listItem.appIcon);
				txtView.setText(listItem.appName);
				if (listItem.appChoice)
				{
				ckBox.setChecked(true);
				deSelectAll = false;
				}
				else
				{
				selectAll = false;	
				}
				ckBox.setOnClickListener(new View.OnClickListener()
				{
					
					@Override
					public void onClick(View v)
					{
					}
				});
				return convertView;
			}

		};
        if(selectAll)
        {
        	TextView txtView = (TextView)findViewById(R.id.main_text_select_all);
            txtView.setText(getString(R.string.deselect_all));

        	TextView tv = (TextView)findViewById(R.id.side_text_select_all);
            tv.setText(getString(R.string.sticker_widget_hide));
        }
        else
        {
         	TextView txtView = (TextView)findViewById(R.id.main_text_select_all);
            txtView.setText(getString(R.string.select_all));
        
            TextView tv = (TextView)findViewById(R.id.side_text_select_all);
            tv.setText(getString(R.string.sticker_widget_show));
        
        }

        CheckBox checkBox = (CheckBox)findViewById(R.id.select_all_checkbox);
        checkBox.setChecked(false);
		ListView listView = (ListView)findViewById(R.id.list_items);
		listView.setAdapter(listAdapter);
		
	}

/*
	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		Logger.d("ashish",""+which);
	}
*/
}
