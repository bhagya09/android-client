package com.bsb.hike.ui;

import android.content.Intent;
import android.os.Bundle;

import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

public class LinkDispatcherActivity extends HikeAppStateBaseFragmentActivity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Intent in = getIntent();
		if (in != null)
		{
			in.setClass(LinkDispatcherActivity.this, HomeActivity.class);
			in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(in);
		}
	}
}
