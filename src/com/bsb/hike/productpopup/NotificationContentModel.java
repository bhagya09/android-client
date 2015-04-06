package com.bsb.hike.productpopup;

public class NotificationContentModel
{
	private String title;

	private String user;

	private boolean shouldPlaySound;

	private int triggerpoint;
	
	/**
	 * @param title
	 * @param user
	 * @param shouldPlaySound
	 * @param triggerpoint
	 */
	public NotificationContentModel(String title, String user, boolean shouldPlaySound, int triggerpoint)
	{
		this.title = title;
		this.user = user;
		this.shouldPlaySound = shouldPlaySound;
		this.triggerpoint = triggerpoint;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(String user)
	{
		this.user = user;
	}

	/**
	 * @param shouldPlaySound the shouldPlaySound to set
	 */
	public void setShouldPlaySound(boolean shouldPlaySound)
	{
		this.shouldPlaySound = shouldPlaySound;
	}

	/**
	 * @param triggerpoint the triggerpoint to set
	 */
	public void setTriggerpoint(int triggerpoint)
	{
		this.triggerpoint = triggerpoint;
	}

	/**
	 * @return the title
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * @return the user
	 */
	public String getUser()
	{
		return user;
	}

	/**
	 * @return the shouldPlaySound
	 */
	public boolean isShouldPlaySound()
	{
		return shouldPlaySound;
	}

	/**
	 * @return the triggerpoint
	 */
	public int getTriggerpoint()
	{
		return triggerpoint;
	}
}
