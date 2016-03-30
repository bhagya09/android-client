package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.timeline.model.StatusMessage;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

public class MessageMetadata
{

	private boolean isSync;

	public static enum NudgeAnimationType
	{
		NONE, SINGLE
	}

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

	private boolean isPokeMessage;

	private StatusMessage statusMessage;

	private Sticker sticker;
	
	private boolean oldUser;
	
	private boolean isGhostMessage;
	
	private int duration;
	
	private boolean voipInitiator;

	public boolean isVoipInitiator() {
		return voipInitiator;
	}

	public int getDuration() {
		return duration;
	}
	
	// This is used to put Title on the Nuj RUj notification
	public String getKey()
	{
		try 
		{
			return 	json.getJSONObject(HikeConstants.DATA).optString(HikeConstants.UserJoinMsg.NOTIF_TITLE,"");
		}
		catch (JSONException e) {
			Logger.d("JSON Exception", "Returning null Title");
			return null;
		} 
	}
	public boolean isGhostMessage()
	{
		return isGhostMessage;
	}

	private NudgeAnimationType nudgeAnimationType = NudgeAnimationType.NONE;

	private int pinMessage =0 ;

	private boolean newBroadcast;

	private String groupAdder;

	private String caption;

	public int getPinMessage()
	{
		return pinMessage;
	}
	
	public boolean isSilent()
	{
		switch (getPushSetting()) 
		{
			case HikeConstants.PushType.loud:
				return false;
			case HikeConstants.PushType.silent:
			case HikeConstants.PushType.none:
			default:
				return true;
		}
	}
	
	private int getPushSetting()
	{
		try 
		{
			return json.getJSONObject(HikeConstants.DATA).optInt(HikeConstants.UserJoinMsg.PUSH_SETTING, HikeConstants.PushType.silent);
		}
		catch (JSONException e) {
			Logger.d("JSON Exception", "Returning loud notification");
			return HikeConstants.PushType.loud;
		} 
	}
	
	public boolean shouldShowPush()
	{
		switch(getPushSetting())
		{
			case HikeConstants.PushType.none:
				return false;
			case HikeConstants.PushType.silent:
			case HikeConstants.PushType.loud:
			default:
				return true;
		}
	}

	public void setPinMessage(int pinMessage)
	{
		this.pinMessage = pinMessage;
	}

	public MessageMetadata(String jsonString,boolean isSent) throws JSONException{
		this(new JSONObject(jsonString),isSent);
	}
	public MessageMetadata(JSONObject metadata, boolean isSent) throws JSONException
	{
		this.participantInfoState = metadata.has(HikeConstants.DND_USERS) || metadata.has(HikeConstants.DND_NUMBERS) ? ParticipantInfoState.DND_USER : ParticipantInfoState
				.fromJSON(metadata);
		switch (this.participantInfoState)
		{
		case CHANGED_GROUP_NAME:
			this.msisdn = metadata.getString(HikeConstants.FROM);
			break;

		case DND_USER:
			this.dndNumbers = metadata.has(HikeConstants.DND_USERS) ? metadata.getJSONArray(HikeConstants.DND_USERS) : metadata.getJSONArray(HikeConstants.DND_NUMBERS);
			break;
			
		case CHANGE_ADMIN:
			this.msisdn = metadata.getJSONObject(HikeConstants.DATA).getString(HikeConstants.ADMIN_MSISDN);
			break;

		case PARTICIPANT_JOINED:
			this.gcjParticipantInfo = metadata.getJSONArray(HikeConstants.DATA);
			if(metadata.has(HikeConstants.METADATA))
			{
				JSONObject mdata = metadata.getJSONObject(HikeConstants.METADATA);
				if (mdata.has(HikeConstants.FROM))
				{
					this.groupAdder = mdata.getString(HikeConstants.FROM);
				}
				this.isSync = mdata.optInt(HikeConstants.MqttMessageTypes.SYNC, 0) == 1 ? true : false;
			}
			this.newGroup = metadata.optBoolean(HikeConstants.NEW_GROUP);
			this.newBroadcast = metadata.optBoolean(HikeConstants.NEW_BROADCAST);
			break;

		case PARTICIPANT_LEFT:
			this.msisdn = metadata.getString(HikeConstants.DATA);
			this.showBIS = HikeConstants.MqttMessageTypes.BLOCK_INTERNATIONAL_SMS.equals(metadata.optString(HikeConstants.SUB_TYPE));
			break;

		case USER_JOIN:
		case USER_OPT_IN:
			this.msisdn = metadata.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN);
			this.credits = metadata.getJSONObject(HikeConstants.DATA).optInt(HikeConstants.CREDITS, -1);
			this.oldUser = HikeConstants.RETURNING_USER.equals(metadata.optString(HikeConstants.SUB_TYPE));
			break;

		case CHANGED_GROUP_IMAGE:
			this.msisdn = metadata.getString(HikeConstants.FROM);
			break;
		case STATUS_MESSAGE:
			this.statusMessage = new StatusMessage(metadata);
			break;
		case CHAT_BACKGROUND:
			this.msisdn = metadata.optString(HikeConstants.FROM);
			break;
		case VOIP_CALL_SUMMARY:
			this.duration = metadata.getJSONObject(HikeConstants.DATA).getInt(HikeConstants.VOIP_CALL_DURATION);
			this.voipInitiator = metadata.getJSONObject(HikeConstants.DATA).getBoolean(HikeConstants.VOIP_CALL_INITIATOR);
			break;
		}
		this.newUser = metadata.optString(HikeConstants.NEW_USER).equals("true");
		this.dndMissedCallNumber = metadata.optString(HikeConstants.METADATA_DND);
		this.hikeFileList = getHikeFileListFromJSONArray(metadata.optJSONArray(HikeConstants.FILES), isSent, metadata.optString(HikeConstants.CAPTION));
		if (HikeConstants.LOCATION_CONTENT_TYPE.equals(metadata.optString(HikeConstants.CONTENT_TYPE)))
		{
			this.hikeFileList = new ArrayList<HikeFile>();
			this.hikeFileList.add(new HikeFile(metadata, isSent));
		}
		this.isPokeMessage = metadata.optBoolean(HikeConstants.POKE);
		this.pinMessage  = metadata.optInt(HikeConstants.PIN_MESSAGE);
		this.setCaption(metadata.optString(HikeConstants.CAPTION));
		this.json = metadata;
		if (metadata.has(StickerManager.STICKER_ID))
		{
			String cat = metadata.optString(StickerManager.CATEGORY_ID);
			this.sticker = new Sticker(cat, metadata.optString(StickerManager.STICKER_ID));
		}
	}

	/**
	 * Returns the sticker category. Used only for cases where the category is an unknown one.
	 * 
	 * @return
	 */
	public String getUnknownStickerCategory()
	{
		return json.optString(StickerManager.CATEGORY_ID);
	}

	private List<HikeFile> getHikeFileListFromJSONArray(JSONArray fileList, boolean isSent, String caption)
	{
		if (fileList == null)
		{
			return null;
		}
		List<HikeFile> hikeFileList = new ArrayList<HikeFile>();
		for (int i = 0; i < fileList.length(); i++)
		{
			HikeFile newHikeFile = new HikeFile(fileList.optJSONObject(i), isSent);
			if(!TextUtils.isEmpty(caption))
			{
				newHikeFile.setCaption(caption);
			}
			hikeFileList.add(newHikeFile);
		}
		return hikeFileList;
	}

	public JSONArray getGcjParticipantInfo()
	{
		return gcjParticipantInfo;
	}

	public boolean isNewGroup()
	{
		return newGroup;
	}
	
	public boolean isNewBroadcast()
	{
		return newBroadcast;
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	public boolean isShowBIS()
	{
		return showBIS;
	}

	public int getCredits()
	{
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

	public boolean isPokeMessage()
	{
		return isPokeMessage;
	}

	public JSONObject getJSON()
	{
		return json;
	}

	public StatusMessage getStatusMessage()
	{
		return statusMessage;
	}

	public Sticker getSticker()
	{
		return sticker;
	}
	
	public boolean isOldUser()
	{
		return oldUser;
	}

	public NudgeAnimationType getNudgeAnimationType()
	{
		return nudgeAnimationType;
	}

	public void setNudgeAnimationType(NudgeAnimationType type)
	{
		this.nudgeAnimationType = type;
	}

	public String getGroupAdder() {
		return groupAdder;
	}

	public void setGroupAdder(String groupAdder) {
		this.groupAdder = groupAdder;
	}

	
	/**
	 * Used to update the sticker object as well as the JSON Object in the message metadata
	 * 
	 * @param newCategoryId
	 * @throws JSONException
	 */
	public void updateSticker(String newCategoryId) throws JSONException
	{
		if (this.json.has(StickerManager.CATEGORY_ID))
		{
			this.json.put(StickerManager.CATEGORY_ID, newCategoryId);
		}
		this.sticker.setCategoryId(newCategoryId);
	}

	public String getCaption()
	{
		return caption;
	}

	public void setCaption(String caption)
	{
		this.caption = caption;
	}

	public boolean isSync()
	{
		return isSync;
	}
}
