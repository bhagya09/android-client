package com.bsb.hike.ui;

import com.bsb.hike.R;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

public class HikeDirectHelpPageActivity extends HikeAppStateBaseFragmentActivity 
{
	private View helpPageActionBar = null;
	
	private TextView title;
	
	 @Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hike_direct_help_page);
		setActionBar();
	}
	 
	private void setActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		if (helpPageActionBar == null)
		{
			helpPageActionBar = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);
		}

		if (actionBar.getCustomView() == helpPageActionBar)
		{
			return;
		}

		View backContainer = helpPageActionBar.findViewById(R.id.back);

		title = (TextView) helpPageActionBar.findViewById(R.id.title);
		helpPageActionBar.findViewById(R.id.seprator).setVisibility(View.GONE);

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		title.setText(R.string.hike_direct_help);
		actionBar.setCustomView(helpPageActionBar);
	}

	 @Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
