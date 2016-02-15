package com.bsb.hike.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.chatHead.ChatHeadUtils;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ashishagarwal on 07/12/15.
 */
public class BlockCallerListAdapter extends RecyclerViewCursorAdapter<BlockCallerListAdapter.ViewHolder>
{

	private Context mContext;

	private LayoutInflater mInflater;

	private IconLoader iconLoader;

	private int mIconImageSize;

	private View.OnClickListener onClickListener;

	private Map nameNumberMap = new HashMap<String, String>();

	public void swapCursor(Cursor result, Map<String, String> nameNumberMap)
	{
		this.nameNumberMap = nameNumberMap;
		swapCursor(result);
	}


	class ViewHolder extends RecyclerView.ViewHolder
	{
		CheckBox checkBox;

		TextView name;

		TextView msisdn;

		ImageView contactImage;

		View item;

		public ViewHolder(View view)
		{
			super(view);
			checkBox = (CheckBox) view.findViewById(R.id.checkbox);
			name = (TextView) view.findViewById(R.id.name);
			msisdn = (TextView) view.findViewById(R.id.number);
			contactImage = (ImageView) view.findViewById(R.id.contact_image);
			item = view.findViewById(R.id.hike_list_item);
		}
	}

	public BlockCallerListAdapter(Context context, Cursor c, Map<String, String> nameNumberMap, View.OnClickListener onClickListener, int flags)
	{
		super(context, c, flags);
		mContext = context;
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.nameNumberMap = nameNumberMap;
		mIconImageSize = mContext.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
		iconLoader = new IconLoader(mContext, mIconImageSize);
		iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		this.onClickListener = onClickListener;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor)
	{
		if (cursor != null)
		{
			String msisdn = cursor.getString(cursor.getColumnIndex(DBConstants.MSISDN));
			if (nameNumberMap != null && nameNumberMap.containsKey(msisdn) && nameNumberMap.get(msisdn) != null)
			{
				viewHolder.name.setText(nameNumberMap.get(msisdn).toString());
			}
			else
			{
				viewHolder.name.setText(cursor.getString(cursor.getColumnIndex(DBConstants.NAME)));
			}
			viewHolder.msisdn.setText(msisdn);
			viewHolder.checkBox.setButtonDrawable(R.drawable.block_button);
			if (cursor.getInt(cursor.getColumnIndex(DBConstants.HIKE_USER.IS_BLOCK)) == ChatHeadUtils.VALUE_TRUE)
			{
				viewHolder.checkBox.setChecked(true);
			}
			else
			{
				viewHolder.checkBox.setChecked(false);
			}
			iconLoader.loadImage(msisdn, viewHolder.contactImage, false, true, true);
			viewHolder.item.setOnClickListener(onClickListener);
		}
	}

	@Override
	protected void onContentChanged()
	{

	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View convertView;
		convertView = mInflater.inflate(R.layout.hike_list_item_caller, parent, false);
		return new ViewHolder(convertView);
	}

}
