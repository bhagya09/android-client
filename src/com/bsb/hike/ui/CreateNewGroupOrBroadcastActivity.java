package com.bsb.hike.ui;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.utils.ChangeProfileImageBaseActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class CreateNewGroupOrBroadcastActivity extends ChangeProfileImageBaseActivity
{

	private SharedPreferences preferences;

	private String convId;

	private ImageView convImage;

	private EditText convName;

	private TextView broadcastNote;
	
	private View doneBtn;

	private ImageView arrow;

	private TextView postText;

	private Bitmap groupBitmap;
	
	/**
	 * @author anubansal
	 *
	 * Declaring the oneToNConversationType
	 */
	private static enum ConvType
	{
		GROUP,
		BROADCAST
	};
	
	private ConvType convType;
	
	private String myMsisdn;

	private TextView groupNote;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setConvType();
		createView();
		setupActionBar();

		preferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

		if (savedInstanceState != null)
		{
			setConversationId(savedInstanceState.getString(HikeConstants.Extras.CONVERSATION_ID));
		}

		if (TextUtils.isEmpty(convId))
		{
			String conversationId = null;
			
			String uid = preferences.getString(HikeMessengerApp.UID_SETTING, "");
			switch (convType)
			{
				case BROADCAST:
					conversationId = HikeConstants.BROADCAST_ID_PREFIX + uid + ":" + System.currentTimeMillis();
					break;
					
				case GROUP:
					conversationId = uid + ":" + System.currentTimeMillis();
					break;
			}
			setConversationId(conversationId);
		}

		Object object = getLastCustomNonConfigurationInstance();
		if (object != null && (object instanceof Bitmap))
		{
			groupBitmap = (Bitmap) object;
			convImage.setImageBitmap(groupBitmap);
		}
		else
		{
			if (convType == ConvType.BROADCAST)
			{
				findViewById(R.id.broadcast_bg).setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(convId, true));
			}
			else if (convType == ConvType.GROUP)
			{
				convImage.setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(convId, true));
			}
		}
		
		if(convType == ConvType.GROUP)
		{
			showProductPopup(ProductPopupsConstants.PopupTriggerPoints.NEWGRP.ordinal());
		}
	}

	/**
	 * This method sets the OneToNConversation type to be handled
	 */
	private void setConvType()
	{
		convType = getIntent().hasExtra(HikeConstants.Extras.CREATE_BROADCAST) ? ConvType.BROADCAST : ConvType.GROUP;
	}

	private void createView() {
		
		if (convType == ConvType.BROADCAST)
		{
			setContentView(R.layout.create_new_broadcast);

			convImage = (ImageView) findViewById(R.id.broadcast_profile_image);
			convName = (EditText) findViewById(R.id.broadcast_name);
			myMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, "");
			broadcastNote = (TextView) findViewById(R.id.broadcast_info);
			broadcastNote.setText(Html.fromHtml(getString(R.string.broadcast_participant_info, myMsisdn)));
			convName.addTextChangedListener(new TextWatcher()
			{

				@Override
				public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
				{

				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
				{

				}

				@Override
				public void afterTextChanged(Editable editable)
				{
					Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, true);
				}
			});
		}
		
		else if (convType == ConvType.GROUP)
		{
			setContentView(R.layout.create_new_group);

			convImage = (ImageView) findViewById(R.id.group_profile_image);
			convName = (EditText) findViewById(R.id.group_name);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			groupNote = (TextView) findViewById(R.id.group_info);
			groupNote.setText(Html.fromHtml(getString(R.string.group_participant_info)));
			convName.addTextChangedListener(new TextWatcher()
			{

				@Override
				public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
				{

				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
				{

				}

				@Override
				public void afterTextChanged(Editable editable)
				{
					Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, !TextUtils.isEmpty(editable.toString().trim()));
				}
			});
			
			if(convImage != null)
			{
				convImage.setOnClickListener(new OnClickListener()
				{
					
					@Override
					public void onClick(View v)
					{												
						showProfileImageEditDialog(CreateNewGroupOrBroadcastActivity.this, CreateNewGroupOrBroadcastActivity.this, convId, null);
					}
				});
			}
		}
	}

	@Override
	public void onBackPressed()
	{
		/**
		 * Deleting the temporary file, if it exists.
		 */
		File file = new File(Utils.getTempProfileImageFileName(convId));
		file.delete();

		super.onBackPressed();
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		if (groupBitmap != null)
		{
			return groupBitmap;
		}
		return super.onRetainCustomNonConfigurationInstance();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		if (!TextUtils.isEmpty(convId))
		{
			outState.putString(HikeConstants.Extras.CONVERSATION_ID, convId);
		}
		super.onSaveInstanceState(outState);
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		doneBtn = actionBarView.findViewById(R.id.done_container);
		arrow = (ImageView) actionBarView.findViewById(R.id.arrow);
		postText = (TextView) actionBarView.findViewById(R.id.post_btn);

		doneBtn.setVisibility(View.VISIBLE);

		switch(convType)
		{
			case BROADCAST:
				Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, true);
				title.setText(R.string.new_broadcast);
				postText.setText(R.string.done);
				break;
				
			case GROUP:
				Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, false);
				title.setText(R.string.new_group);
				postText.setText(R.string.next_signup);
				break;
		}

		doneBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if (convType == ConvType.BROADCAST)
				{
					sendBroadCastAnalytics();
				}
				onNextPressed();
			}
		});
		
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

	private void onNextPressed()
	{
		switch (convType)
		{
			case BROADCAST:
				Intent intentBroadcast = IntentFactory.openComposeChatIntentForBroadcast(this, convId, convName.getText().toString().trim());
				setResult(RESULT_OK, intentBroadcast);
				finish();
				break;
				
			case GROUP:
				Intent intentGroup = IntentFactory.openComposeChatIntentForGroup(this, convId, convName.getText().toString().trim());
				startActivity(intentGroup);
				break;
		}
	}
	
	@Override
	public String profileImageCropped()
	{
		String imgPath = super.profileImageCropped();
		
		setGroupPreivewBitmap(imgPath);
		
		return imgPath;
	}

	/**
	 * Used to set preview bitmap for the new group
	 * @param path of the bitmap
	 */
	private void setGroupPreivewBitmap(String path)
	{
		Bitmap tempBitmap = HikeBitmapFactory.scaleDownBitmap(path, HikeConstants.SIGNUP_PROFILE_IMAGE_DIMENSIONS, HikeConstants.SIGNUP_PROFILE_IMAGE_DIMENSIONS,
				Bitmap.Config.RGB_565, true, false);

		groupBitmap = HikeBitmapFactory.getCircularBitmap(tempBitmap);
		convImage.setImageBitmap(HikeBitmapFactory.getCircularBitmap(tempBitmap));

		/*
		 * Saving the icon in the DB.
		 */
		byte[] bytes = BitmapUtils.bitmapToBytes(tempBitmap, CompressFormat.JPEG, 100);

		tempBitmap.recycle();
		ContactManager.getInstance().setIcon(convId, bytes, false);
	}

	private void sendBroadCastAnalytics()
	{
		try
		{
			JSONObject metadata = new JSONObject();
			metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.BROADCAST_DONE);
			HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
	}
	
	/**
	 * Sets the local msisdn of the profile 
	 */
	protected void setConversationId(String convId)
	{
		this.convId = convId;
		super.setLocalMsisdn(this.convId);
	}
}
