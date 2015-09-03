package com.bsb.hike.media;

public class OverFlowMenuItem
{
	public String text;

	public int unreadCount;

	public int drawableId;

	public int id;

	public boolean enabled = true;
	
	public boolean secondary = false;
	
	public OverFlowMenuItem(String text, int unreadCount, int drawableId, int id)
	{
		this(text, unreadCount, drawableId, id, true);
	}

	public OverFlowMenuItem(String text, int unreadCount, int drawableId, int id, boolean enabled)
	{
		this(text, unreadCount, drawableId, false, id, enabled);
	}
	
	public OverFlowMenuItem(String text, int unreadCount, int drawableId, boolean secondary, int id)
	{
		this(text, unreadCount, drawableId, secondary, id, true);
	}
	
	public OverFlowMenuItem(String text, int unreadCount, int drawableId, boolean secondary, int id, boolean enabled)
	{
		this.text = text;
		this.unreadCount = unreadCount;
		this.drawableId = drawableId;
		this.id = id;
		this.enabled = enabled;
		this.secondary = secondary;
	}

}
