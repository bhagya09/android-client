package com.bsb.hike.filetransfer;

import com.bsb.hike.filetransfer.FileTransferBase.FTState;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class FileSavedState implements Serializable
{
	public static final String CURRENT_STATE = "currentState";

	public static final String TOTAL_SIZE = "totalSize";

	public static final String TRANSFERRED_SIZE = "transferredSize";

	public static final String SESSION_ID = "sessionId";

	public static final String FILE_KEY = "fileKey";

	public static final String ANIMATED_PROGRESS = "animatedProgress";

	public static final String RESPONSE_JSON = "responseJSON";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private FTState _currentState;

	private long _totalSize; // (in bytes)

	private long _transferredSize;

	private String _sessionId;

	private String _responseJson;

	private String _fileKey;

	private int _animatedProgress;

	public FileSavedState(FTState state, long totalSize, long transferredSize, int animatedProgress)
	{
		_currentState = state;
		_totalSize = totalSize;
		_transferredSize = transferredSize;
		_sessionId = null;
		_responseJson = null;
		_animatedProgress = animatedProgress;
	}

	public FileSavedState(FTState state, long totalSize, long transferredSize, String sId, JSONObject response, int animatedProgress)
	{
		_currentState = state;
		_totalSize = totalSize;
		_transferredSize = transferredSize;
		_sessionId = sId;
		if (response != null)
		{
			_responseJson = response.toString();
		}
		else
		{
			_responseJson = null;
		}
		_animatedProgress = animatedProgress;
	}

	public FileSavedState()
	{
		_currentState = FTState.NOT_STARTED;
	}

	public FileSavedState(FTState state, String mFileKey, int animatedProgress)
	{
		_currentState = state;
		_fileKey = mFileKey;
		_animatedProgress = animatedProgress;
	}

	public FileSavedState(FileSavedState fss)
	{
		_currentState = fss._currentState;
		_totalSize = fss._totalSize;
		_transferredSize = fss._transferredSize;
		_sessionId = fss._sessionId;
		_responseJson = fss._responseJson;
		_fileKey = fss._fileKey;
		_animatedProgress = fss._animatedProgress;
	}

	public long getTotalSize()
	{
		return _totalSize;
	}

	public void setTotalSize(long s)
	{
		_totalSize = s;
	}

	public void setAnimatedProgress(int s)
	{
		_animatedProgress = s;
	}

	public long getTransferredSize()
	{
		return _transferredSize;
	}

	public void setTransferredSize(long s)
	{
		_transferredSize = s;
	}

	public int getAnimatedProgress()
	{
		return _animatedProgress;
	}

	public FTState getFTState()
	{
		return _currentState;
	}

	public void setFTState(FTState ftstate)
	{
		_currentState = ftstate;
	}

	public String getSessionId()
	{
		return _sessionId;
	}

	public void setSessionId(String sessionId)
	{
		this._sessionId = sessionId;
	}

	public String getFileKey()
	{
		return _fileKey;
	}

	public void setFileKey(String fileKey)
	{
		this._fileKey = fileKey;
	}

	public JSONObject getResponseJson()
	{
		try
		{
			if (_responseJson != null)
			{
				return (new JSONObject(_responseJson));
			}
			return null;
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public void setResponseJson(JSONObject json)
	{
		this._responseJson = json.toString();
	}

	@Override
	public String toString()
	{
		// TODO Auto-generated method stub
		String s = super.toString() + " " + _currentState + " " + _transferredSize + "/" + _totalSize;
		return s;
	}

	public static FileSavedState getFileSavedStateFromJSON(JSONObject json)
	{
		FileSavedState state = new FileSavedState();
		state._currentState = FTState.values()[json.optInt(CURRENT_STATE)];
		state._totalSize = json.optLong(TOTAL_SIZE);
		state._transferredSize = json.optLong(TRANSFERRED_SIZE);
		state._sessionId = json.optString(SESSION_ID);
		state._responseJson = json.optString(RESPONSE_JSON);
		state._fileKey = json.optString(FILE_KEY);
		state._animatedProgress = json.optInt(ANIMATED_PROGRESS);
		return state;
	}

	public JSONObject toJSON()
	{
		JSONObject json = new JSONObject();
		try
		{
			json.put(CURRENT_STATE, _currentState.ordinal());
			json.put(TOTAL_SIZE, _totalSize);
			json.put(TRANSFERRED_SIZE, _transferredSize);
			json.put(SESSION_ID, _sessionId);
			json.put(RESPONSE_JSON, _responseJson);
			json.put(FILE_KEY, _fileKey);
			json.put(ANIMATED_PROGRESS, _animatedProgress);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return json;
	}
}
