package com.hike.transporter.interfaces;

import com.hike.transporter.models.ReceiverConsignment;

/**
 * 
 * @author himanshu
 *	This informs user about the state of the message
 */
public interface IMessageReceived
{
	public void onMessageReceived(ReceiverConsignment receiver);

	public void onChunkRead(ReceiverConsignment receiver);
	
	public void onFileCompleted(ReceiverConsignment receiver);
}
