package com.bsb.hike.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

public class FtueBroadcast extends HikeAppStateBaseFragmentActivity
{
	private View doneBtn;

	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.broadcast_ftue);
		doneBtn = (View) findViewById(R.id.btn_broadcast_ftue);
		
		doneBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				HikeSharedPreferenceUtil.getInstance().saveData(HikeMessengerApp.SHOW_BROADCAST_FTUE_SCREEN, false);
				Intent intent = new Intent(FtueBroadcast.this, ComposeChatActivity.class);
				intent.putExtra(HikeConstants.Extras.COMPOSE_MODE, HikeConstants.Extras.CREATE_BROADCAST_MODE);
				intent.putExtra(HikeConstants.Extras.CREATE_BROADCAST, true);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
			}
		});	
		
		setupActionBar();
		
	}

	private void setupActionBar() 
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);
		View backContainer = actionBarView.findViewById(R.id.back);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.broadcast);

		backContainer.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});
		
		actionBar.setCustomView(actionBarView);
	}
}
