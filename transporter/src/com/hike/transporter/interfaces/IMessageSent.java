package com.hike.transporter.interfaces;

import com.hike.transporter.models.SenderConsignment;
import com.hike.transporter.utils.TConstants.ERRORCODES;

/**
 * 
 * @author himanshu
 *	This informs user about the state of the message bening sent
 */
public interface IMessageSent
{
	public void onTransitBegin(SenderConsignment senderConsignment);

	public void onTransitEnd(SenderConsignment senderConsignment);

	public void onChunkSend(SenderConsignment senderConsignment);

	public void onMessageDelivered(SenderConsignment senderConsignment);
	
	public void onError(SenderConsignment senderConsignment, ERRORCODES errorCode);

}
