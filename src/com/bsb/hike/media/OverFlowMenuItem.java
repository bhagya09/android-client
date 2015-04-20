package com.bsb.hike.media;

public class OverFlowMenuItem
{
	public String text;

	public int unreadCount;

	public int drawableId;

	public int id;

	public boolean enabled = true;

	public OverFlowMenuItem(String text, int unreadCount, int drawableId, int id)
	{
		this(text, unreadCount, drawableId, id, true);
	}

	public OverFlowMenuItem(String text, int unreadCount, int drawableId, int id, boolean enabled)
	{
		this.text = text;
		this.unreadCount = unreadCount;
		this.drawableId = drawableId;
		this.id = id;
		this.enabled = enabled;
	}

}
