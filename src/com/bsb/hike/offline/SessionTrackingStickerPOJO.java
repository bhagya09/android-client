package com.bsb.hike.offline;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;

public class SessionTrackingStickerPOJO extends SessionTracFilePOJO
{

	private String stkId, catId;

	public SessionTrackingStickerPOJO(long size, String catId, String stkId)
	{
		super("stk", size);
		this.catId = catId;
		this.stkId = stkId;
	}

	public SessionTrackingStickerPOJO(long size, String catId, String stkId, int type)
	{
		super("stk", size);
		this.catId = catId;
		this.stkId = stkId;
		this.type = type;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException
	{
		JSONObject json = super.toJSONObject();
		json.put(HikeConstants.STICKER_ID, stkId);
		json.put(HikeConstants.CATEGORY_ID, catId);
		return json;
	}
}
