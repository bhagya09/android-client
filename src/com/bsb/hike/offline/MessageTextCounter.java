package com.bsb.hike.offline;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MessageTextCounter
{

	private int noTextMessagesSent = 0;

	private int noNudgesSent = 0;

	private int noTextMessagesReceived = 0;

	private int noNudgesReceived = 0;
	
	private int noContactSent = 0;

	private int noContactRec = 0;
	
	private int messRead=0;
	
	private int messDel=0;

	public MessageTextCounter()
	{

	}

	public void incrementSendTextMsg()
	{
		noTextMessagesSent++;
	}

	public void incremenrSendNudges()
	{
		noNudgesSent++;
	}

	public void incrementReceivedTextMsg()
	{
		noTextMessagesReceived++;
	}

	public void incremenrReceivedNudges()
	{
		noNudgesReceived++;
	}
	
	public void incrementContactSend()
	{
		noContactSent++;
	}

	public void incrementContactReceived()
	{
		noContactRec++;
	}

	public void incrementMessDel()
	{
		messDel++;
	}

	public void incrementMessRec(int ctr)
	{
		messRead+=ctr;
	}

	public void releaseResources()
	{
		noTextMessagesSent = 0;

		noNudgesSent = 0;

		noTextMessagesReceived = 0;

		noNudgesReceived = 0;

		noContactSent = 0;

		noContactRec = 0;
		
		messDel=0;
		
		messRead=0;

	}
	
	public JSONArray toJSONObject() throws JSONException
	{

		JSONArray array = new JSONArray();
		if (noTextMessagesSent != 0 || noTextMessagesReceived != 0)
		{
			JSONObject ob = new JSONObject();
			ob.put("ft", "txts");
			ob.put("st", noTextMessagesSent);
			ob.put("et", noTextMessagesReceived);
			array.put(ob);
		}

		if (noNudgesSent != 0 || noNudgesReceived != 0)
		{
			JSONObject ob = new JSONObject();
			ob.put("ft", "nud");
			ob.put("st", noNudgesSent);
			ob.put("et", noNudgesReceived);
			array.put(ob);
		}
		
		if (noContactSent != 0 || noContactRec != 0)
		{
			JSONObject ob = new JSONObject();
			ob.put("ft", "cons");
			ob.put("st", noContactSent);
			ob.put("et", noContactRec);
			array.put(ob);
		}
		
		if (messDel != 0 || messRead != 0)
		{
			JSONObject ob = new JSONObject();
			ob.put("ft", "mdmr");
			ob.put("st", messDel);
			ob.put("et", messRead);
			array.put(ob);
		}
		return array;
	}

}
