package com.bsb.hike.timeline.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.Protip;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class StatusMessage
{

	public static enum StatusMessageType
	{
		TEXT(0), IMAGE(1), TEXT_IMAGE(2), PROFILE_PIC(3), FRIEND_REQUEST(4), FRIEND_REQUEST_ACCEPTED(5), NO_STATUS(6), USER_ACCEPTED_FRIEND_REQUEST(7), PROTIP(8), JOINED_HIKE(9); 
	
		int mKey;

		StatusMessageType(int argKey)
		{
			mKey = argKey;
		}

		public int getKey()
		{
			return mKey;
		}

		public static StatusMessageType getType(int type)
		{
			switch (type)
			{
			case 0:
				return StatusMessageType.TEXT;
			case 1:
				return StatusMessageType.IMAGE;
			case 2:
				return StatusMessageType.TEXT_IMAGE;
			case 3:
				return StatusMessageType.PROFILE_PIC;
			case 4:
				return StatusMessageType.FRIEND_REQUEST;
			case 5:
				return StatusMessageType.FRIEND_REQUEST_ACCEPTED;
			case 6:
				return StatusMessageType.NO_STATUS;
			case 7:
				return StatusMessageType.USER_ACCEPTED_FRIEND_REQUEST;
			case 8:
				return StatusMessageType.PROTIP;
			case 9:
				return StatusMessageType.JOINED_HIKE;
			default:
				throw new IllegalArgumentException("Invalid ActionType key");
			}
		}
	}

	private boolean isRead;

	private long id;

	private String mappedId;

	private String msisdn;

	private String name;

	private String text;

	private StatusMessageType statusMessageType;

	private long timeStamp;

	private int moodId;

	private int timeOfDay;

	private Protip protip;

	private String fileKey;
	
	private boolean isHistoricalUpdate;
	
	private ActionsDataModel actionsData;
	
	public StatusMessage(JSONObject statusMessageJson) throws JSONException
	{
		this.msisdn = statusMessageJson.getString(HikeConstants.FROM);

		this.timeStamp = statusMessageJson.getLong(HikeConstants.TIMESTAMP);
		/* prevent us from receiving a message from the future */
		long now = System.currentTimeMillis() / 1000;
		this.timeStamp = (this.timeStamp > now) ? now : this.timeStamp;

		JSONObject data = statusMessageJson.getJSONObject(HikeConstants.DATA);

		this.mappedId = data.getString(HikeConstants.STATUS_ID);

		if (data.optBoolean(HikeConstants.PROFILE))
		{
			this.statusMessageType = StatusMessageType.PROFILE_PIC;
			this.text = "";
		}
		else if (data.has(HikeConstants.STATUS_MESSAGE) && !data.getString(HikeConstants.STATUS_MESSAGE).equals("null"))
		{
			if (data.has(HikeConstants.SU_IMAGE_KEY))
			{
				this.fileKey = data.optString(HikeConstants.SU_IMAGE_KEY);
				this.statusMessageType = StatusMessageType.TEXT_IMAGE;
			}
			else
			{
				this.statusMessageType = StatusMessageType.TEXT;
			}
			this.text = data.optString(HikeConstants.STATUS_MESSAGE);
		}
		else if (data.has(HikeConstants.SU_IMAGE_KEY))
		{
			this.fileKey = data.optString(HikeConstants.SU_IMAGE_KEY);
			this.statusMessageType = StatusMessageType.IMAGE;
		}
		this.moodId = data.optInt(HikeConstants.MOOD) - 1;
		this.timeOfDay = data.optInt(HikeConstants.TIME_OF_DAY);
		
		if(data.has(HikeConstants.HISTORICAL_UPDATE))
		{
			this.isHistoricalUpdate = data.optBoolean(HikeConstants.HISTORICAL_UPDATE);
		}
	}

	public StatusMessage(long id, String mappedId, String msisdn, String name, String text, StatusMessageType statusMessageType, long timeStamp)
	{
		this(id, mappedId, msisdn, name, text, statusMessageType, timeStamp, -1, 0);
	}

	public StatusMessage(long id, String mappedId, String msisdn, String name, String text, StatusMessageType statusMessageType, long timeStamp, int moodId, int timeOfDay)
	{
		this(id, mappedId, msisdn, name, text, statusMessageType, timeStamp, moodId, timeOfDay, null);
	}
	
	public StatusMessage(long id, String mappedId, String msisdn, String name, String text, StatusMessageType statusMessageType, long timeStamp, int moodId, int timeOfDay, String fileKey)
	{
		this(id, mappedId, msisdn, name, text, statusMessageType, timeStamp, moodId, timeOfDay, fileKey, false);
	}

	public StatusMessage(long id, String mappedId, String msisdn, String name, String text, StatusMessageType statusMessageType, long timeStamp, int moodId, int timeOfDay, String fileKey, boolean isRead)
	{
		this.id = id;
		this.mappedId = mappedId;
		this.msisdn = msisdn;
		this.name = name;
		this.text = text;
		this.statusMessageType = statusMessageType;
		this.timeStamp = timeStamp;
		this.moodId = moodId;
		this.timeOfDay = timeOfDay;
		this.fileKey = fileKey;
		this.isRead = isRead;
	}

	public StatusMessage(Protip protip)
	{
		this.protip = protip;
		this.mappedId = protip.getMappedId();
		this.name = HikeConstants.PROTIP_STATUS_NAME;
		this.text = protip.getHeader();
		this.timeStamp = protip.getTimeStamp();
		this.statusMessageType = StatusMessageType.PROTIP;
	}

	public StatusMessage(Cursor cursor)
	{
		if(cursor == null)
		{
			throw new IllegalArgumentException("Cursor passed to StatusMessage was null");
		}
		this.msisdn = cursor.getString(cursor.getColumnIndex(DBConstants.MSISDN));
		this.statusMessageType = StatusMessageType.getType(cursor.getInt(cursor.getColumnIndex(DBConstants.STATUS_TYPE)));
		this.text = cursor.getString(cursor.getColumnIndex(DBConstants.STATUS_TEXT));
		this.moodId = cursor.getInt(cursor.getColumnIndex(DBConstants.MOOD_ID));
		this.fileKey = cursor.getString(cursor.getColumnIndex(DBConstants.FILE_KEY));
		this.mappedId = cursor.getString(cursor.getColumnIndex(DBConstants.STATUS_MAPPED_ID));
	}
	
	
	public void setId(long id)
	{
		this.id = id;
	}

	public long getId()
	{
		return id;
	}

	public String getMappedId()
	{
		return mappedId;
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public String getNotNullName()
	{
		return TextUtils.isEmpty(name) ? msisdn : name;
	}

	public String getText()
	{
		return text;
	}

	public StatusMessageType getStatusMessageType()
	{
		return statusMessageType;
	}

	public void setTimeStamp(long timeStamp)
	{
		this.timeStamp = timeStamp;
	}

	public long getTimeStamp()
	{
		return timeStamp;
	}

	public boolean hasMood()
	{
		return (EmoticonConstants.moodMapping.containsKey(moodId));
	}

	public int getMoodId()
	{
		return moodId;
	}

	public int getTimeOfDay()
	{
		return timeOfDay;
	}

	public Protip getProtip()
	{
		return protip;
	}

	public boolean isRead() {
		return isRead;
	}

	public void setRead(boolean read) {
		isRead = read;
	}

	public String getTimestampFormatted(boolean pretty, Context context)
	{
		
		if(getTimeStamp() == 0)
		{
			return "";
		}
		
		if (pretty)
		{
			return Utils.getFormattedTime(pretty, context, timeStamp);
		}
		else
		{
			Date date = new Date(timeStamp * 1000);
			String format;
			if (android.text.format.DateFormat.is24HourFormat(context))
			{
				format = "d MMM ''yy 'AT' HH:mm";
			}
			else
			{
				format = "d MMM ''yy 'AT' h:mm aaa";
			}
			DateFormat df = new SimpleDateFormat(format);
			return df.format(date);
		}
	}

	public String getFileKey()
	{
		return fileKey;
	}

	public void setFileKey(String fileKey)
	{
		this.fileKey = fileKey;
	}
	
	public boolean isMyStatusUpdate()
	{
		return Utils.isSelfMsisdn(msisdn);
	}

	public boolean isHistoricalUpdate()
	{
		return isHistoricalUpdate;
	}

	public ActionsDataModel getActionsData()
	{
		return actionsData;
	}

	public void setActionsData(ActionsDataModel actionsData)
	{
		this.actionsData = actionsData;
	}
	
	public static StatusMessage getJoinedHikeStatus(ContactInfo contactInfo)
	{
		if(contactInfo == null)
		{
			return null;
		}
		
		return new StatusMessage(HikeConstants.JOINED_HIKE_STATUS_ID, null, contactInfo.getMsisdn(), contactInfo.getName(), HikeMessengerApp.getInstance().getString(
				R.string.joined_hike_update), StatusMessageType.JOINED_HIKE, contactInfo.getHikeJoinTime());
	}

	@Override
	public String toString()
	{
		return "StatusMessage [mappedId=" + mappedId + ", msisdn=" + msisdn + ", name=" + name + ", text=" + text + ", statusMessageType=" + statusMessageType
				+ ", isHistoricalUpdate=" + isHistoricalUpdate + ", actionsData=" + actionsData + "]";
	}
	
	
}
