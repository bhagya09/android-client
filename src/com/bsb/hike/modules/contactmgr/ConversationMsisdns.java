package com.bsb.hike.modules.contactmgr;

import java.util.List;
import java.util.Map;

import android.util.Pair;

public class ConversationMsisdns
{
	private List<String> oneToOneMsisdns;

	private Map<String, Pair<List<String>, Long>> groupLastMsisdns; // This contains group last msisdns and timestamp of last message in a group

	public ConversationMsisdns(List<String> msisdns, Map<String, Pair<List<String>, Long>> grpLastMsisdnsTimestamp)
	{
		this.oneToOneMsisdns = msisdns;
		this.groupLastMsisdns = grpLastMsisdnsTimestamp;
	}

	public List<String> getOneToOneMsisdns()
	{
		return oneToOneMsisdns;
	}

	public Map<String, Pair<List<String>, Long>> getGroupLastMsisdnsWithTimestamp()
	{
		return groupLastMsisdns;
	}

	public void setOneToOneMsisdns(List<String> msisdns)
	{
		oneToOneMsisdns = msisdns;
	}

	public void setGroupLastMsisdnsWithTimestamp(Map<String, Pair<List<String>, Long>> grpLastMsisdnsTimestamp)
	{
		groupLastMsisdns = grpLastMsisdnsTimestamp;
	}
}
