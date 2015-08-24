/**
 * 
 */
package com.bsb.hike.modules.kpt;

import java.util.ArrayList;

import com.bsb.hike.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * @author anubansal
 *
 */
public class ShorthandAdapter extends BaseAdapter
{
	private Context mContext;
	private ArrayList<String> shortcutList;
	private ArrayList<String> expansionList;	
	private static LayoutInflater inflater;
	private ListView mListView;
	private ATRDeleteListener mListener;
	
	/**
	 * Default Constructor
	 */
	public ShorthandAdapter(Context context, ArrayList<String> shortcutList, ArrayList<String> expansionList, ListView listView, ATRDeleteListener listener)
	{
		this.mContext = context;
		this.shortcutList = shortcutList;
		this.expansionList = expansionList;
		this.mListView = listView;
		this.mListener = listener;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);	
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		final int index = position;
		View view = convertView;
		if (convertView == null)
		{
			view = inflater.inflate(R.layout.shorthand_list_item, null);
		}
		TextView shortcutView = (TextView) view.findViewById(R.id.shortcut_view);
		TextView expansionView = (TextView) view.findViewById(R.id.expansion_view);
		shortcutView.setText(shortcutList.get(position));
		expansionView.setText(expansionList.get(position));
		Button del = (Button) view.findViewById(R.id.remove_atr);
		
		del.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mListener.atrDeleted(shortcutList.get(index));
				shortcutList.remove(index);
				expansionList.remove(index);
				notifyDataSetChanged();
			}
		});
		return view;
	}

	@Override
	public int getCount()
	{
		// TODO Auto-generated method stub
		return shortcutList.size();
	}

	@Override
	public Object getItem(int position)
	{
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public long getItemId(int position)
	{
		// TODO Auto-generated method stub
		return position;
	}

}
