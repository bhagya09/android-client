package com.bsb.hike.modules.packPreview;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

public class StickerPreviewActivity extends HikeAppStateBaseFragmentActivity
{
	public StickerPreviewActivity()
	{
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sticker_preview_parent);
		setupPreviewFragment(savedInstanceState);
		setupActionBar();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}
	
	private void setupPreviewFragment(Bundle savedInstance)
	{
		if(savedInstance != null)
		{
			return;
		}
		
		Intent intent = getIntent();
		String catId = intent.getStringExtra(HikeConstants.STICKER_CATEGORY_ID);
		StickerPreviewFragment stickerPreviewFragment = StickerPreviewFragment.newInstance(catId);
		getSupportFragmentManager().beginTransaction().replace(R.id.sticker_preview_parent, stickerPreviewFragment).commit();
	}
	
	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = getLayoutInflater().inflate(R.layout.sticker_shop_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);
		View stickerSettingsBtn = actionBarView.findViewById(R.id.sticker_settings_btn);
		stickerSettingsBtn.setVisibility(View.GONE);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.pack_preview_activity_actionbar_text);

		backContainer.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		actionBar.setCustomView(actionBarView);

	}
}
