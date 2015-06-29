package com.bsb.hike.models;

import com.bsb.hike.HikeConstants.NotificationType;

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
	
	//The notification type describe that the notification is from bot,chat,group,dpupdate,statusupdate etc ..
	
	int notificationType=NotificationType.OTHER;
	
	public NotificationPreview(String message, String title,int notificationType) 
	{
		this.message = message;
		this.title = title;
		this.notificationType=notificationType;
	}

	public String getMessage() 
	{
		return message;
	}

	public String getTitle()
	{
		return title;
	}

	public int getNotificationType()
	{
		return notificationType;
	}

}
