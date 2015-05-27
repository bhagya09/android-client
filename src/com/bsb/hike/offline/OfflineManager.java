package com.bsb.hike.offline;

public class OfflineManager
{
	public static final OfflineManager _instance = new OfflineManager();

	private boolean isHotSpotCreated = false;

	private OfflineManager()
	{
		init();
	}

	public static OfflineManager getInstance()
	{
		return _instance;
	}
	
	/**
	 * Initialize all your functions here
	 */
	private void init()
	{

	}

	public void setIsHotSpotCreated(boolean isHotSpotCreated)
	{
		this.isHotSpotCreated = isHotSpotCreated;
	}

	public boolean isHotspotCreated()
	{
		return isHotSpotCreated;
	}
	
	public void addToTextQueue()
	{
		
	}
	
	public void addToFileQueue()
	{
		
	}
	
}
