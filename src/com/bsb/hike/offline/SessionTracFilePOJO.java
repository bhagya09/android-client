package com.bsb.hike.offline;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.offline.OfflineConstants.MessageType;
import com.coremedia.iso.boxes.FileTypeBox;

public class SessionTracFilePOJO
{
	private long startTime, endTime;

	private String fileType;

	private long size = 0l;

	protected int type = MessageType.SENT.ordinal();

	public SessionTracFilePOJO(String fileType, long size)
	{
		this.fileType = fileType;
		this.size = size;
		startTime = System.currentTimeMillis();
	}

	public SessionTracFilePOJO(String fileType, long size, int type)
	{
		this(fileType, size);
		this.type = type;
	}

	public void recordEndTime()
	{
		endTime = System.currentTimeMillis();
	}

	public JSONObject toJSONObject() throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put("st", startTime);
		json.put("et", endTime);

		json.put("ft", computeShortFileType(fileType));
		json.put("sz", size);
		json.put("ty", type);
		return json;
	}

	protected String computeShortFileType(String fileType)
	{
		if (TextUtils.isEmpty(fileType))
		{
			return "";
		}

		if (fileType.length() > 2)
		{
			return fileType.substring(0, 2);
		}
		return fileType;

	}
}
