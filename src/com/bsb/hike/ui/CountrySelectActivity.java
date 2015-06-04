
package com.bsb.hike.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.PinnedHeaderListView;
import com.bsb.hike.adapters.SectionedBaseAdapter;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

public class CountrySelectActivity extends HikeAppStateBaseFragmentActivity implements TextWatcher
{
	public static final String RESULT_COUNTRY_NAME = "resCName";

	public static final String RESULT_COUNTRY_CODE = "resCode";

	private SectionedBaseAdapter listViewAdapter;

	private PinnedHeaderListView listView;

	private boolean searching;

	private CountryFilter filter;

	private BaseAdapter searchListViewAdapter;

	private HashMap<String, ArrayList<Country>> countries = new HashMap<String, ArrayList<Country>>();

	private List<String> sortedCountries = new ArrayList<String>();

	public ArrayList<Country> searchResult;

	public static class Country
	{
		public String name;

		public String code;

		public String shortname;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		searching = false;

		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().getAssets().open("countries.txt")));
			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] args = line.split(";");
				Country c = new Country();
				c.name = args[1];
				c.code = args[2];
				c.shortname = args[0];
				String n = c.name.substring(0, 1).toUpperCase();
				ArrayList<Country> arr = countries.get(n);
				if (arr == null)
				{
					arr = new ArrayList<Country>();
					countries.put(n, arr);
					sortedCountries.add(n);
				}
				arr.add(c);
			}
		}
		catch (Exception e)
		{

		}

		Collections.sort(sortedCountries, new Comparator<String>()
		{
			@Override
			public int compare(String lhs, String rhs)
			{
				return lhs.compareTo(rhs);
			}
		});

		for (ArrayList<Country> arr : countries.values())
		{
			Collections.sort(arr, new Comparator<Country>()
			{
				@Override
				public int compare(Country country, Country country2)
				{
					return country.name.compareTo(country2.name);
				}
			});
		}

		setContentView(R.layout.country_select_layout);

		searchListViewAdapter = new SearchAdapter(this);

		listView = (PinnedHeaderListView) findViewById(R.id.listView);
		listView.setVerticalScrollBarEnabled(false);
		listView.setAdapter(listViewAdapter = new ListAdapter(this));
		((EditText) findViewById(R.id.search_text)).addTextChangedListener(this);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				if (searching)
				{
					if (i < searchResult.size())
					{
						Country c = searchResult.get(i);
						setResult(c);
					}
				}
				else
				{
					int section = listViewAdapter.getSectionForPosition(i);
					int row = listViewAdapter.getPositionInSectionForPosition(i);
					if (section < sortedCountries.size())
					{
						String n = sortedCountries.get(section);
						ArrayList<Country> arr = countries.get(n);
						if (row < arr.size())
						{
							Country c = arr.get(row);
							setResult(c);
						}
					}
				}
			}
		});

		filter = new CountryFilter();
		setupActionBar();
	}

	private void setResult(Country c)
	{
		Intent intent = new Intent();
		intent.putExtra(HikeConstants.Extras.SELECTED_COUNTRY, c.name);
		intent.putExtra(RESULT_COUNTRY_NAME, c.name);
		intent.putExtra(RESULT_COUNTRY_CODE, c.code);
		setResult(RESULT_OK, intent);
		finish();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		ImageView backIcon = (ImageView) actionBarView.findViewById(R.id.up);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		actionBar.setCustomView(actionBarView);
		Toolbar parent=(Toolbar)actionBarView.getParent();
		parent.setContentInsetsAbsolute(0,0);

		backIcon.setImageResource(R.drawable.ic_back);
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_header));
		title.setText(R.string.select_country);
	}

	private class SearchAdapter extends BaseAdapter
	{
		private Context mContext;

		public SearchAdapter(Context context)
		{
			mContext = context;
		}

		@Override
		public boolean areAllItemsEnabled()
		{
			return true;
		}

		@Override
		public boolean isEnabled(int i)
		{
			return true;
		}

		@Override
		public int getCount()
		{
			if (searchResult == null)
			{
				return 0;
			}
			return searchResult.size();
		}

		@Override
		public Object getItem(int i)
		{
			return null;
		}

		@Override
		public long getItemId(int i)
		{
			return i;
		}

		@Override
		public boolean hasStableIds()
		{
			return false;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup)
		{
			if (view == null)
			{
				LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = li.inflate(R.layout.country_row_layout, viewGroup, false);
			}
			TextView textView = (TextView) view.findViewById(R.id.settings_row_text);
			TextView detailTextView = (TextView) view.findViewById(R.id.settings_row_text_detail);
			View divider = view.findViewById(R.id.settings_row_divider);

			Country c = searchResult.get(i);
			textView.setText(c.name);
			detailTextView.setText("(+" + c.code + ")");
			if (i == searchResult.size() - 1)
			{
				divider.setVisibility(View.GONE);
			}
			else
			{
				divider.setVisibility(View.VISIBLE);
			}

			return view;
		}

		@Override
		public int getItemViewType(int i)
		{
			return 0;
		}

		@Override
		public int getViewTypeCount()
		{
			return 1;
		}

		@Override
		public boolean isEmpty()
		{
			return searchResult == null || searchResult.size() == 0;
		}
	}

	private class ListAdapter extends SectionedBaseAdapter
	{
		private Context mContext;

		public ListAdapter(Context context)
		{
			mContext = context;
		}

		@Override
		public Object getItem(int section, int position)
		{
			return null;
		}

		@Override
		public long getItemId(int section, int position)
		{
			return 0;
		}

		@Override
		public int getSectionCount()
		{
			return sortedCountries.size();
		}

		@Override
		public int getCountForSection(int section)
		{
			String n = sortedCountries.get(section);
			ArrayList<Country> arr = countries.get(n);
			return arr.size();
		}

		@Override
		public View getItemView(int section, int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
			{
				LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = li.inflate(R.layout.country_row_layout, parent, false);
			}
			TextView textView = (TextView) convertView.findViewById(R.id.settings_row_text);
			TextView detailTextView = (TextView) convertView.findViewById(R.id.settings_row_text_detail);
			View divider = convertView.findViewById(R.id.settings_row_divider);

			String n = sortedCountries.get(section);
			ArrayList<Country> arr = countries.get(n);
			Country c = arr.get(position);
			textView.setText(c.name);
			detailTextView.setText("(+" + c.code + ")");
			if (position == arr.size() - 1)
			{
				divider.setVisibility(View.GONE);
			}
			else
			{
				divider.setVisibility(View.VISIBLE);
			}

			return convertView;
		}

		@Override
		public int getItemViewType(int section, int position)
		{
			return 0;
		}

		@Override
		public int getItemViewTypeCount()
		{
			return 1;
		}

		@Override
		public int getSectionHeaderViewType(int section)
		{
			return 0;
		}

		@Override
		public int getSectionHeaderViewTypeCount()
		{
			return 1;
		}

		@Override
		public View getSectionHeaderView(int section, View convertView, ViewGroup parent)
		{
			if (convertView == null)
			{
				LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = li.inflate(R.layout.friends_group_view, parent, false);
				convertView.setBackgroundColor(getResources().getColor(R.color.white));
			}
			TextView textView = (TextView) convertView.findViewById(R.id.name);
			textView.setText(sortedCountries.get(section).toUpperCase());
			TextView countView = (TextView) convertView.findViewById(R.id.count);
			countView.setText(getCountForSection(section)+"");
			return convertView;
		}
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		String query = s.toString().trim().toLowerCase();

		if (!TextUtils.isEmpty(query))
		{
			if (!(listView.getAdapter() instanceof SearchAdapter))
			{
				listView.setAdapter(searchListViewAdapter);
				if (android.os.Build.VERSION.SDK_INT >= 11)
				{
					listView.setFastScrollAlwaysVisible(false);
				}
				listView.setFastScrollEnabled(false);
				listView.setVerticalScrollBarEnabled(true);
			}
			searching = true;
			filter.filter(query);
		}
		else
		{
			if ((listView.getAdapter() instanceof SearchAdapter))
			{
				searching = false;
				listView.setAdapter(listViewAdapter);
				if (android.os.Build.VERSION.SDK_INT >= 11)
				{
					listView.setFastScrollAlwaysVisible(true);
				}
				listView.setFastScrollEnabled(true);
				listView.setVerticalScrollBarEnabled(false);
			}
		}

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	}

	private class CountryFilter extends Filter
	{
		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			FilterResults results = new FilterResults();
			String textToBeFiltered = constraint.toString();
			ArrayList<Country> resultArray = new ArrayList<Country>();

			String n = textToBeFiltered.substring(0, 1);
			ArrayList<Country> arr = countries.get(n.toUpperCase());
			if (arr != null)
			{
				for (Country c : arr)
				{
					if (c.name.toLowerCase().startsWith(textToBeFiltered))
					{
						resultArray.add(c);
					}
				}
			}

			results.count = 1;
			results.values = resultArray;

			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results)
		{
			searchResult = (ArrayList<Country>) results.values;
			searchListViewAdapter.notifyDataSetChanged();
		}
	}
}
