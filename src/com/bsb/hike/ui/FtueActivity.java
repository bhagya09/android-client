package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author sidharth
 * 
 */
public class FtueActivity extends HikeAppStateBaseFragmentActivity
{
	public static final int NUM_OF_STICKERS = 4;

	public static final String TAG = "FtueActivity";

	private SharedPreferences accountPrefs;

	private List<Sticker> stickers = new ArrayList<Sticker>(NUM_OF_STICKERS);

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (Utils.requireAuth(this))
		{
			return;
		}

		HikeMessengerApp app = (HikeMessengerApp) getApplication();
		app.connectToService();

		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		setContentView(R.layout.ftue6);
		String name = accountPrefs.getString(HikeMessengerApp.NAME_SETTING, null);
		setupActionBar(name);
		String nuxStickerDetails = accountPrefs.getString(HikeConstants.NUX_STICKER_DETAILS, null);
		if (null != nuxStickerDetails)
		{
			try
			{
				JSONObject nuxStickerDetailsObj = new JSONObject(nuxStickerDetails);
				JSONArray arr = nuxStickerDetailsObj.getJSONArray("data");
				for (int i = 0; i < arr.length(); ++i)
				{
					JSONObject j = (JSONObject) arr.get(i);
					String category = j.getString("category");
					String stickerId = j.getString("stickerid");
					Sticker s = new Sticker(category, stickerId);
					Logger.d(TAG, " stickers category " + category + "  id : " + stickerId);
					stickers.add(s);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		GridView gridview = (GridView) findViewById(R.id.gridview);
		gridview.setAdapter(new FtueAdapter(this));
	}

	private void setupActionBar(String name)
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View ftueActionBar = LayoutInflater.from(this).inflate(R.layout.ftue6_action_bar, null);

		if (actionBar.getCustomView() == ftueActionBar)
		{
			return;
		}

		TextView tv = (TextView) ftueActionBar.findViewById(R.id.ftue_title);
		tv.setText(getString(R.string.ftue_sticker_screen_title, name));
		actionBar.setCustomView(ftueActionBar);
	}

	public class FtueAdapter extends BaseAdapter
	{
		private Context mContext;

		private Integer[] stickerResIds = { R.drawable.sticker_069_hi, R.drawable.sticker_11_teasing, R.drawable.sticker_10_love2, R.drawable.sticker_112_watchadoing };

		public FtueAdapter(Context c)
		{
			mContext = c;
		}

		@Override
		public int getCount()
		{
			return stickerResIds.length;
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			ImageView imageView;
			if (convertView == null)
			{
				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(170, 170));
				imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			}
			else
			{
				imageView = (ImageView) convertView;
			}
			imageView.setImageResource(stickerResIds[position]);
			imageView.setTag(stickers.get(position));
			return imageView;
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
	}
}
