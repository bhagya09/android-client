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
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;
import com.bsb.hike.utils.Utils;

public class FeedDataModel
{
	private int mAction;

	private String mObjID;

	private long mTimestamp;

	private String mActor;

	private ActionTypes mActionType;
	
	private ActivityObjectTypes mObjType;
	
	private int readStatus;

	public FeedDataModel(JSONObject jsonObj) throws JSONException
	{
		if (jsonObj.has(HikeConstants.DATA))
		{
			setTimestamp(jsonObj.getLong(HikeConstants.TIMESTAMP));
			
			final JSONObject jsonData = jsonObj.getJSONObject(HikeConstants.DATA);

			setAction(jsonData.getInt(HikeConstants.SUB_TYPE));

			String statusId = jsonData.getString(HikeConstants.SU_ID);
			
			if(!TextUtils.isEmpty(statusId))
			{
				setObjType(ActivityObjectTypes.STATUS_UPDATE);
				
				setObjID(statusId);
			}

			//setTimestamp(jsonData.getLong(HikeConstants.TIMESTAMP));

			setActor(jsonData.getString(HikeConstants.FROM));

			if (mAction == ActionsDataModel.ActionTypes.LIKE.getKey())
			{
				setActionType(ActionsDataModel.ActionTypes.LIKE);
			}
			else if (mAction == ActionsDataModel.ActionTypes.UNLIKE.getKey())
			{
				setActionType(ActionsDataModel.ActionTypes.UNLIKE);
			}
		}
	}
	
	public FeedDataModel(Cursor cursor)
	{
		if(cursor != null)
		{
			setTimestamp(cursor.getInt(cursor.getColumnIndex(DBConstants.FEED_TS)));
			setReadStatus(cursor.getInt(cursor.getColumnIndex(DBConstants.READ)));
			setActor(cursor.getString(cursor.getColumnIndex(DBConstants.FEED_ACTOR)));
			setActionType(cursor.getInt(cursor.getColumnIndex(DBConstants.FEED_ACTION_ID)));
		}
		
	}
	
	private void setActionType(int actionType)
	{
		this.mActionType = ActionTypes.getType(actionType);
	}

	public int getAction()
	{
		return mAction;
	}

	public void setAction(int mAction)
	{
		this.mAction = mAction;
	}

	public String getObjID()
	{
		return mObjID;
	}

	public void setObjID(String mStatusID)
	{
		this.mObjID = mStatusID;
	}

	public long getTimestamp()
	{
		return mTimestamp;
	}

	public void setTimestamp(long mTimestamp)
	{
		this.mTimestamp = mTimestamp;
	}

	public String getActor()
	{
		return mActor;
	}

	public void setActor(String mActor)
	{
		this.mActor = mActor;
	}

	public ActionTypes getActionType()
	{
		return mActionType;
	}

	public void setActionType(ActionTypes mActionType)
	{
		this.mActionType = mActionType;
	}

	public ActivityObjectTypes getObjType()
	{
		return mObjType;
	}

	public void setObjType(ActivityObjectTypes mObjType)
	{
		this.mObjType = mObjType;
	}
	
	public int getReadStatus()
	{
		return readStatus;
	}
	
	public void setReadStatus(int readStatus)
	{
		this.readStatus = readStatus;
	}
	
	public String getTimestampFormatted(boolean pretty, Context context)
	{
		if (pretty)
		{
			return Utils.getFormattedTime(pretty, context, mTimestamp);
		}
		else
		{
			Date date = new Date(mTimestamp * 1000);
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
}
