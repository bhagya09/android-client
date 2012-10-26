package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.ui.CreditsActivity;
import com.bsb.hike.utils.Utils;

public class MessageMetadata
{
	private String dndMissedCallNumber;
	private boolean newUser;
	private JSONObject json;
	private JSONArray dndNumbers;
	private ParticipantInfoState participantInfoState = ParticipantInfoState.NO_INFO;
	private List<HikeFile> hikeFileList;
	private JSONArray gcjParticipantInfo;
	private boolean newGroup;
	private String msisdn;
	private boolean showBIS;
	private int credits;

	public MessageMetadata(JSONObject metadata) throws JSONException
	{
		this.participantInfoState = metadata.has(HikeConstants.DND_USERS) || metadata.has(HikeConstants.DND_NUMBERS) ? ParticipantInfoState.DND_USER : ParticipantInfoState.fromJSON(metadata);
		switch (this.participantInfoState) 
		{
		case CHANGED_GROUP_NAME:
			this.msisdn = metadata.getString(HikeConstants.FROM);
			break;

		case DND_USER:
			this.dndNumbers = metadata.has(HikeConstants.DND_USERS) ? 
					metadata.getJSONArray(HikeConstants.DND_USERS) : metadata.getJSONArray(HikeConstants.DND_NUMBERS);
			break;

		case PARTICIPANT_JOINED:
			this.gcjParticipantInfo = metadata.getJSONArray(HikeConstants.DATA);
			this.newGroup = metadata.optBoolean(HikeConstants.NEW_GROUP);
			break;

		case PARTICIPANT_LEFT:
			this.msisdn = metadata.getString(HikeConstants.DATA);
			this.showBIS = HikeConstants.MqttMessageTypes.BLOCK_INTERNATIONAL_SMS.equals(metadata.optString(HikeConstants.SUB_TYPE));
			break;

		case USER_JOIN:
		case USER_OPT_IN:
			this.msisdn = metadata.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN);
			this.credits = metadata.getJSONObject(HikeConstants.DATA).optInt(HikeConstants.CREDITS, -1);
			break;
		}
		this.newUser = metadata.optString(HikeConstants.NEW_USER).equals("true");
		this.dndMissedCallNumber = metadata.optString(HikeConstants.METADATA_DND);
		this.hikeFileList = getHikeFileListFromJSONArray(metadata.optJSONArray(HikeConstants.FILES));
		this.json = metadata;
	}

	private List<HikeFile> getHikeFileListFromJSONArray(JSONArray fileList)
	{
		if(fileList == null)
		{
			return null;
		}
		List<HikeFile> hikeFileList = new ArrayList<HikeFile>();
		for(int i = 0; i<fileList.length(); i++)
		{
			hikeFileList.add(new HikeFile(fileList.optJSONObject(i)));
		}
		return hikeFileList;
	}

	public JSONArray getGcjParticipantInfo() {
		return gcjParticipantInfo;
	}

	public boolean isNewGroup() {
		return newGroup;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public boolean isShowBIS() {
		return showBIS;
	}

	public int getCredits() {
		return credits;
	}

	public JSONArray getDndNumbers()
	{
		return dndNumbers;
	}

	public List<HikeFile> getHikeFiles()
	{
		return hikeFileList;
	}

	public String getDNDMissedCallNumber()
	{
		return dndMissedCallNumber;
	}

	public ParticipantInfoState getParticipantInfoState()
	{
		return participantInfoState;
	}

	public String serialize()
	{
		return this.json.toString();
	}

	public boolean getNewUser()
	{
		return newUser;
	}

	public JSONObject getJSON()
	{
		return json;
	}

	public Spannable getMessage(final Context context, final ConvMessage convMessage, boolean shouldSetClickListener)
	{
		String content = "tap here";
		StringBuilder formatArg = new StringBuilder();
		final StringBuilder dndMsisdn = new StringBuilder();
		if(dndNumbers != null)
		{
			try
			{
				Map<String, GroupParticipant> participantList = ((GroupConversation)convMessage.getConversation()).getGroupParticipantList();
				for(int i = 0; i<dndNumbers.length(); i++)
				{
					GroupParticipant dndParticipant = participantList.get(dndNumbers.getString(i));
					if(dndParticipant != null)
					{
						String separator = (i == (dndNumbers.length() -2)) ? " and " : ((i < dndNumbers.length() -2) ? ", " : "");
						formatArg.append(dndParticipant.getContactInfo().getFirstName() + separator);
					}
					String msisdnSeparator = (i < (dndNumbers.length() - 1)) ? Build.MANUFACTURER.equalsIgnoreCase("Samsung") ? "," : ";" : "";
					dndMsisdn.append(dndNumbers.getString(i) + msisdnSeparator);
				}
			}
			catch (JSONException e) 
			{
				Log.e(getClass().getSimpleName(), "Invalid JSON", e);
			}
		}
		else
		{
			formatArg.append(Utils.getFirstName(convMessage.getConversation().getLabel()));
			dndMsisdn.append(convMessage.getMsisdn());
		}
		String message = context.getString(
				!TextUtils.isEmpty(dndMissedCallNumber) ? 
						R.string.dnd_message : !newUser ? 
								R.string.friend_joined_hike_no_creds : R.string.friend_joined_hike_with_creds, 
								formatArg, 
								dndMissedCallNumber);
		Spannable spannable = Spannable.Factory.getInstance().newSpannable(message);
		int index = message.indexOf(content);
		if (index != -1 && shouldSetClickListener) 
		{
			spannable.setSpan(new ClickableSpan() 
			{
				@Override
				public void onClick(View blah) 
				{
					Intent intent = !TextUtils.isEmpty(dndMissedCallNumber) ? new Intent(Intent.ACTION_VIEW) : new Intent(context, CreditsActivity.class);
					if (!TextUtils.isEmpty(dndMissedCallNumber)) 
					{
						Utils.logEvent(context, HikeConstants.LogEvent.OPT_IN_TAP_HERE);
						intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse("sms:" + dndMsisdn));
						intent.putExtra("sms_body", context.getString(R.string.dnd_invite_message, dndMissedCallNumber));
					}
					context.startActivity(intent);
				}
			}, index, index + content.length(),
			Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		return spannable;
	}
}
