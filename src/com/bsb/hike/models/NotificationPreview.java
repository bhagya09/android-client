package com.bsb.hike.models;

/**
 * 
 * @author umangjeet
 *
 * This class has been added to allow single notifications to be able to display
 * their title, further it can be used to house other single Notification related properties
 */

public class NotificationPreview 
{
	
	String message;
	
	String title;

	public NotificationPreview(String message, String title) 
	{
		this.message = message;
		this.title = title;
	}

	public String getMessage() 
	{
		return message;
	}

	public String getTitle() 
	{
		return title;
	}

}
