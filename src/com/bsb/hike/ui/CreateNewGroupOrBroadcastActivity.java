package com.bsb.hike.ui;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.BroadcastConversation;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.ChangeProfileImageBaseActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

public class CreateNewGroupOrBroadcastActivity extends ChangeProfileImageBaseActivity
{

	private SharedPreferences preferences;

	private String groupOrBroadcastId;

	private ImageView groupOrBroadcastImage;

	private EditText groupOrBroadcastName;

	private TextView broadcastNote;
	
	private View doneBtn;

	private ImageView arrow;

	private TextView postText;

	private Bitmap groupBitmap;
	
	private boolean isBroadcast;

	private ArrayList<String> broadcastRecipients;
	
	private String myMsisdn;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		isBroadcast = getIntent().getBooleanExtra(HikeConstants.IS_BROADCAST, false);
		broadcastRecipients = getIntent().getStringArrayListExtra(HikeConstants.Extras.BROADCAST_RECIPIENTS);
		super.onCreate(savedInstanceState);
		createView();
		setupActionBar();

		preferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

		if (savedInstanceState != null)
		{
			groupOrBroadcastId = savedInstanceState.getString(HikeConstants.Extras.GROUP_BROADCAST_ID);
		}

		if (TextUtils.isEmpty(groupOrBroadcastId))
		{
			String uid = preferences.getString(HikeMessengerApp.UID_SETTING, "");
			if (isBroadcast)
			{
				groupOrBroadcastId = "b:" + uid + ":" + System.currentTimeMillis();
			}
			else
			{
				groupOrBroadcastId = uid + ":" + System.currentTimeMillis();
			}
			Logger.d("BroadcastActivity1111", "broadcastId is :" + groupOrBroadcastId);
		}

		Object object = getLastCustomNonConfigurationInstance();
		if (object != null && (object instanceof Bitmap))
		{
			groupBitmap = (Bitmap) object;
			groupOrBroadcastImage.setImageBitmap(groupBitmap);
		}
		else
		{
			if (isBroadcast)
			{
				findViewById(R.id.broadcast_bg).setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(groupOrBroadcastId, true));
			}
			else
			{
				groupOrBroadcastImage.setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(groupOrBroadcastId, true));
			}
		}
		
		if(!isBroadcast)
		{
			showProductPopup(ProductPopupsConstants.PopupTriggerPoints.NEWGRP.ordinal());
		}
	}

	private void createView() {
		
		if (isBroadcast)
		{
			setContentView(R.layout.create_new_broadcast);

			groupOrBroadcastImage = (ImageView) findViewById(R.id.broadcast_profile_image);
			groupOrBroadcastName = (EditText) findViewById(R.id.broadcast_name);
//			groupOrBroadcastName.setHint(BroadcastConversation.defaultBroadcastName(broadcastRecipients));
			myMsisdn = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MSISDN_SETTING, "");
			broadcastNote = (TextView) findViewById(R.id.broadcast_info);
			broadcastNote.setText(Html.fromHtml(getString(R.string.broadcast_participant_info, myMsisdn)));
//			broadcastNote.setText(getString(R.string.broadcast_participant_info, myMsisdn));
			groupOrBroadcastName.addTextChangedListener(new TextWatcher()
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
		
		else
		{
			setContentView(R.layout.create_new_group);

			groupOrBroadcastImage = (ImageView) findViewById(R.id.group_profile_image);
			groupOrBroadcastName = (EditText) findViewById(R.id.group_name);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			groupOrBroadcastName.addTextChangedListener(new TextWatcher()
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
		}
	}

	@Override
	public void onBackPressed()
	{
		/**
		 * Deleting the temporary file, if it exists.
		 */
		File file = new File(Utils.getTempProfileImageFileName(groupOrBroadcastId));
		file.delete();

		onBack();
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
		if (!TextUtils.isEmpty(groupOrBroadcastId))
		{
			outState.putString(HikeConstants.Extras.GROUP_BROADCAST_ID, groupOrBroadcastId);
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

		if (isBroadcast)
		{
			Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, true);
			title.setText(R.string.new_broadcast);
			postText.setText(R.string.done);

			doneBtn.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					sendBroadCastAnalytics();
					createBroadcast(broadcastRecipients);
				}
			});
			
			backContainer.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					onBack();
				}
			});
		}
		else
		{
			Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, false);
			title.setText(R.string.new_group);
			postText.setText(R.string.next_signup);

			doneBtn.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					Intent intent = new Intent(CreateNewGroupOrBroadcastActivity.this, ComposeChatActivity.class);
					intent.putExtra(HikeConstants.Extras.GROUP_NAME, groupOrBroadcastName.getText().toString().trim());
					intent.putExtra(HikeConstants.Extras.GROUP_BROADCAST_ID, groupOrBroadcastId);
					intent.putExtra(HikeConstants.Extras.CREATE_GROUP, true);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
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
		}

		actionBar.setCustomView(actionBarView);
	}

	private void onBack()
	{
		if (isBroadcast)
		{
			IntentManager.onBackPressedCreateNewBroadcast(CreateNewGroupOrBroadcastActivity.this, broadcastRecipients);
			finish();
		}
	}
	
	private void createBroadcast(ArrayList<String> selectedContactsMsisdns)
	{
		String broadcastName = groupOrBroadcastName.getText().toString().trim();
		
		boolean newBroadcast = true;

//		Construct ContactInfo for all msisdns in 'selectedContactsMsisdns'
		ArrayList<ContactInfo> selectedContactList = new ArrayList<ContactInfo>(selectedContactsMsisdns.size());
		for (String msisdn : selectedContactsMsisdns)
		{
			ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, true, false);
			selectedContactList.add(contactInfo);
		}
		
		Map<String, PairModified<GroupParticipant, String>> participantList = new HashMap<String, PairModified<GroupParticipant, String>>();
		
		for (ContactInfo particpant : selectedContactList)
		{
			GroupParticipant broadcastParticipant = new GroupParticipant(particpant);
			participantList.put(particpant.getMsisdn(), new PairModified<GroupParticipant, String>(broadcastParticipant, broadcastParticipant.getContactInfo().getNameOrMsisdn()));
		}
		ContactInfo userContactInfo = Utils.getUserContactInfo(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE));

		BroadcastConversation broadcastConversation;
		broadcastConversation = new BroadcastConversation(groupOrBroadcastId, null, userContactInfo.getMsisdn(), true);
		broadcastConversation.setGroupParticipantList(participantList);

		Logger.d(getClass().getSimpleName(), "Creating group: " + groupOrBroadcastId);
		HikeConversationsDatabase mConversationDb = HikeConversationsDatabase.getInstance();
		mConversationDb.addRemoveGroupParticipants(groupOrBroadcastId, broadcastConversation.getGroupParticipantList(), false);
		if (newBroadcast)
		{
			mConversationDb.addConversation(broadcastConversation.getMsisdn(), false, broadcastName, broadcastConversation.getGroupOwner());
			ContactManager.getInstance().insertGroup(broadcastConversation.getMsisdn(),broadcastName);
		}

		try
		{
			// Adding this boolean value to show a different system message
			// if its a new group
			JSONObject gcjPacket = broadcastConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
			gcjPacket.put(HikeConstants.NEW_BROADCAST, newBroadcast);
			ConvMessage msg = new ConvMessage(gcjPacket, broadcastConversation, this, true);
			ContactManager.getInstance().updateGroupRecency(groupOrBroadcastId, msg.getTimestamp());
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, msg);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		JSONObject gcjJson = broadcastConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
		/*
		 * Adding the group name to the packet
		 */
		if (newBroadcast)
		{
			JSONObject metadata = new JSONObject();
			try
			{
				metadata.put(HikeConstants.NAME, broadcastName);

				String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
				String fileName = Utils.getTempProfileImageFileName(groupOrBroadcastId);
				File groupImageFile = new File(directory, fileName);

				if (groupImageFile.exists())
				{
					metadata.put(HikeConstants.REQUEST_DP, true);
				}

				gcjJson.put(HikeConstants.METADATA, metadata);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		HikeMqttManagerNew.getInstance().sendMessage(gcjJson, HikeMqttManagerNew.MQTT_QOS_ONE);

		ContactInfo conversationContactInfo = new ContactInfo(groupOrBroadcastId, groupOrBroadcastId, groupOrBroadcastId, groupOrBroadcastId);
		Intent intent = Utils.createIntentFromContactInfo(conversationContactInfo, true);
		intent.setClass(this, ChatThread.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();

	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		String path = null;
		if (resultCode != RESULT_OK)
		{
			return;
		}

		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		/*
		 * Making sure the directory exists before setting a profile image
		 */
		File dir = new File(directory);
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		String fileName = Utils.getTempProfileImageFileName(groupOrBroadcastId);
		final String destFilePath = directory + "/" + fileName;

		File selectedFileIcon = null;

		switch (requestCode)
		{
		case HikeConstants.CAMERA_RESULT:
			/* fall-through on purpose */
		case HikeConstants.GALLERY_RESULT:
			Logger.d("ProfileActivity", "The activity is " + this);
			if (requestCode == HikeConstants.CAMERA_RESULT)
			{
				String filePath = preferences.getString(HikeMessengerApp.FILE_PATH, "");
				selectedFileIcon = new File(filePath);

				/*
				 * Removing this key. We no longer need this.
				 */
				Editor editor = preferences.edit();
				editor.remove(HikeMessengerApp.FILE_PATH);
				editor.commit();
			}
			if (requestCode == HikeConstants.CAMERA_RESULT && !selectedFileIcon.exists())
			{
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			boolean isPicasaImage = false;
			Uri selectedFileUri = null;
			if (requestCode == HikeConstants.CAMERA_RESULT)
			{
				path = selectedFileIcon.getAbsolutePath();
			}
			else
			{
				if (data == null)
				{
					Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
					return;
				}
				selectedFileUri = data.getData();
				if (Utils.isPicasaUri(selectedFileUri.toString()))
				{
					isPicasaImage = true;
					path = Utils.getOutputMediaFile(HikeFileType.PROFILE, null, false).getAbsolutePath();
				}
				else
				{
					String fileUriStart = "file://";
					String fileUriString = selectedFileUri.toString();
					if (fileUriString.startsWith(fileUriStart))
					{
						selectedFileIcon = new File(URI.create(Utils.replaceUrlSpaces(fileUriString)));
						/*
						 * Done to fix the issue in a few Sony devices.
						 */
						path = selectedFileIcon.getAbsolutePath();
					}
					else
					{
						path = Utils.getRealPathFromUri(selectedFileUri, this);
					}
				}
			}
			if (TextUtils.isEmpty(path))
			{
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			if (!isPicasaImage)
			{
				Utils.startCropActivity(this, path, destFilePath);
			}
			else
			{
				/*
				 * TODO handle picasa case.
				 */
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			break;
		case HikeConstants.CROP_RESULT:
			String finalDestFilePath = data.getStringExtra(MediaStore.EXTRA_OUTPUT);
			if (finalDestFilePath == null)
			{
				Toast.makeText(getApplicationContext(), R.string.error_setting_profile, Toast.LENGTH_SHORT).show();
				return;
			}

			Bitmap tempBitmap = HikeBitmapFactory.scaleDownBitmap(finalDestFilePath, HikeConstants.SIGNUP_PROFILE_IMAGE_DIMENSIONS, HikeConstants.SIGNUP_PROFILE_IMAGE_DIMENSIONS,
					Bitmap.Config.RGB_565, true, false);

			groupBitmap = HikeBitmapFactory.getCircularBitmap(tempBitmap);
			groupOrBroadcastImage.setImageBitmap(HikeBitmapFactory.getCircularBitmap(tempBitmap));

			/*
			 * Saving the icon in the DB.
			 */
			byte[] bytes = BitmapUtils.bitmapToBytes(tempBitmap, CompressFormat.JPEG, 100);

			tempBitmap.recycle();

			ContactManager.getInstance().setIcon(groupOrBroadcastId, bytes, false);

			break;
		}
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
}
