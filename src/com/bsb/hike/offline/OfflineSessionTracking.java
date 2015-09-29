package com.bsb.hike.offline;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.utils.Logger;

public class OfflineSessionTracking
{
	public static volatile OfflineSessionTracking _instance = null;

	private String toUser = null;

	private String fUser = null;

	private Map<Long, SessionTracFilePOJO> listOfFiles = null;

	MessageTextCounter counter = new MessageTextCounter();
	
	public static final String TAG = "OfflineAnalytics";
	
	private long sessionStartTime, sessionEndTime;
	
	private long connectionId = 0l;

	public static OfflineSessionTracking getInstance()
	{
		if (_instance == null)
		{
			synchronized (OfflineSessionTracking.class)
			{
				if (_instance == null)
				{
					_instance = new OfflineSessionTracking();
				}
			}
		}
		return _instance;
	}

	public void startTracking()
	{
		listOfFiles = new ConcurrentHashMap<Long, SessionTracFilePOJO>();
		toUser = null;
		fUser = null;
		sessionStartTime=System.currentTimeMillis();
	}

	public void addToListOfFiles(long msgId, SessionTracFilePOJO filepojo)
	{
		listOfFiles.put(msgId, filepojo);
	}

	public void removeFromList(long msgId)
	{
		if (listOfFiles.containsKey(msgId))
		{
			listOfFiles.remove(msgId);
		}
	}

	public void setConnectionId(long connectionId)
	{
		this.connectionId = connectionId;
	}
	
	public void clearFilesList()
	{
		listOfFiles.clear();
	}

	public SessionTracFilePOJO getFileSession(long msgId)
	{

		if (listOfFiles.containsKey(msgId))
		{
			return listOfFiles.get(msgId);
		}

		return null;
	}

	public String getToUser()
	{
		return toUser;
	}

	public void setToUser(String toUser)
	{
		this.toUser = toUser;
	}

	public String getfUser()
	{
		return fUser;
	}

	public void setfUser(String fUser)
	{
		this.fUser = fUser;
	}

	public void incrementMsgSend(boolean pokeMessage)
	{
		if (pokeMessage)
		{
			counter.incremenrSendNudges();
		}
		else
		{
			counter.incrementSendTextMsg();
		}
	}

	public void incrementContact(boolean isSent)
	{
		if (isSent)
		{
			counter.incrementContactSend();
		}
		else
		{
			counter.incrementContactReceived();
		}
	}
	
	public void incrementMsgRec(boolean pokeMessage)
	{
		if (pokeMessage)
		{
			counter.incremenrReceivedNudges();
		}
		else
		{
			counter.incrementReceivedTextMsg();
		}
	}

	public void incremenrMsgReceived(boolean pokeMessage)
	{
		if (pokeMessage)
		{
			counter.incremenrReceivedNudges();
		}
		else
		{
			counter.incrementReceivedTextMsg();
		}
	}

	public void stopTracking()
	{
		sessionEndTime=System.currentTimeMillis();	
		if (!TextUtils.isEmpty(fUser))
		{
			// send Analytics
			try
			{
				JSONObject object = computeAnalyticsJSON();
				object.put("sest", sessionEndTime - sessionStartTime);
				Logger.d(TAG, "OfflineAnaly >>>" + object.toString(2));
				OfflineAnalytics.recordSessionAnalytics(object);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		releaseResources();
	}

	private JSONObject computeAnalyticsJSON() throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put("tu", toUser);
		json.put("fu", fUser);
		json.put("connId", connectionId);
		JSONArray array = getFilesJSON();
		JSONArray coun=counter.toJSONObject();
		
		for(int i=0;i<coun.length();i++)
		{
			array.put(coun.getJSONObject(i));
		}
		
		if (array.length() > 0)
			json.put("hd", array);

		return json;
	}

	private JSONArray getFilesJSON() throws JSONException
	{
		JSONArray array = new JSONArray();
		for (Entry<Long, SessionTracFilePOJO> entry : listOfFiles.entrySet())
		{
			array.put(entry.getValue().toJSONObject());
		}
		return array;
	}

	private void releaseResources()
	{
		if (listOfFiles != null)
			listOfFiles.clear();
		toUser = null;
		fUser = null;
		if (counter != null)
		{
			counter.releaseResources();
		}
		sessionStartTime = sessionEndTime = 0l;
	}
	
	public void incrementDel()
	{
		counter.incrementMessDel();
	}
	
	public void incrementRead(int ctr)
	{
		counter.incrementMessRec(ctr);
	}

	// here we require a unique conn Id between the two devices for analytics.So we are sending connectID between the two devices both will use the larger one
	public void updateConnectionId(long connectionIdNew)
	{
		if (connectionIdNew > connectionId)
		{
			connectionId = connectionIdNew;
		}
	}
	
	public long getConnectionId()
	{
		return connectionId;
	}
}
