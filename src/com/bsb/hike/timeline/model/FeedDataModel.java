package com.bsb.hike.timeline.model;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.timeline.model.ActionsDataModel.ActionTypes;
import com.bsb.hike.timeline.model.ActionsDataModel.ActivityObjectTypes;

public class FeedDataModel
{
	private int mAction;

	private String mObjID;

	private long mTimestamp;

	private String mActor;

	private ActionTypes mActionType;
	
	private ActivityObjectTypes mObjType;

	public FeedDataModel(JSONObject jsonObj) throws JSONException
	{
		if (jsonObj.has(HikeConstants.DATA))
		{
			final JSONObject jsonData = jsonObj.getJSONObject(HikeConstants.DATA);

			setAction(jsonData.getInt(HikeConstants.SUB_TYPE));

			String statusId = jsonData.getString(HikeConstants.SU_ID);
			
			if(TextUtils.isEmpty(statusId))
			{
				setObjType(ActivityObjectTypes.STATUS_UPDATE);
				
				setObjID(statusId);
			}

			setTimestamp(jsonData.getLong(HikeConstants.TIMESTAMP));

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
}
