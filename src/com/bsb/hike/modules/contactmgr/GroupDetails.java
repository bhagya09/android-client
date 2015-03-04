package com.bsb.hike.modules.contactmgr;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.bsb.hike.utils.PairModified;

public class GroupDetails
{
	private String groupId;

	private String groupName;

	private boolean isGroupAlive;

	private ConcurrentLinkedQueue<PairModified<String, String>> lastMsisdns;

	private long timestamp;
	
	private boolean isGroupMute;

	GroupDetails(String groupId, String grpName, boolean alive, ConcurrentLinkedQueue<PairModified<String, String>> lastMsisdns)
	{
		this(groupId, grpName, alive, lastMsisdns, 0);
	}

	GroupDetails(String groupId, String grpName, boolean alive, ConcurrentLinkedQueue<PairModified<String, String>> lastMsisdns, long timestamp)
	{
		this.groupId = groupId;
		this.groupName = grpName;
		this.isGroupAlive = alive;
		this.lastMsisdns = (null == lastMsisdns) ? new ConcurrentLinkedQueue<PairModified<String, String>>() : lastMsisdns;
		this.timestamp = timestamp;
	}

	public GroupDetails(String groupId, String grpName, boolean alive, boolean mute)
	{
		this(groupId, grpName, alive, null, 0);
		this.isGroupMute = mute;
	}
	
	public String getGroupId()
	{
		return groupId;
	}

	void setGroupId(String groupId)
	{
		this.groupId = groupId;
	}

	public String getGroupName()
	{
		return groupName;
	}

	void setGroupName(String grpName)
	{
		groupName = grpName;
	}

	public boolean isGroupAlive()
	{
		return isGroupAlive;
	}

	void setIsGroupAlive(boolean alive)
	{
		isGroupAlive = alive;
	}

	public ConcurrentLinkedQueue<PairModified<String, String>> getLastMsisdns()
	{
		return lastMsisdns;
	}

	void setLastMsisdns(ConcurrentLinkedQueue<PairModified<String, String>> lastMsisdns)
	{
		this.lastMsisdns = lastMsisdns;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	void setTimestamp(long timestamp)
	{
		this.timestamp = timestamp;
	}

	void setName(String msisdn, String name)
	{
		if (null != lastMsisdns)
		{
			for (PairModified<String, String> msPair : lastMsisdns)
			{
				if (msisdn.equals(msPair.getFirst()))
				{
					msPair.setSecond(name);
				}
			}
		}
	}

	public String getName(String msisdn)
	{
		if (null != lastMsisdns)
		{
			for (PairModified<String, String> msPair : lastMsisdns)
			{
				if (msisdn.equals(msPair.getFirst()))
				{
					return msPair.getSecond();
				}
			}
		}

		return null;
	}
	
	public boolean isGroupMute()
	{
		return isGroupMute;
	}
	
	void setGroupMute(boolean mute)
	{
		isGroupMute = mute;
	}

}
