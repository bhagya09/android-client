package com.hike.transporter.models;

import java.util.concurrent.BlockingQueue;

import com.hike.transporter.Consignee;
import com.hike.transporter.Consigner;

/**
 * 
 * @author himanshu/GauravK
 *
 *	This class contains all the information regarding a Topic.i.e-Consignee,Consigner and the message Queue.
 */
public class TopicResources
{
	private Topic topic;

	private Consigner senderRunnable;

	private Consignee receiverRunnable;

	private BlockingQueue<SenderConsignment> messagesQueue;

	/**
	 * @param topic
	 * @param senderRunnable
	 * @param receiverRunnable
	 * @param messagesQueue
	 */
	public TopicResources(Topic topic, Consigner senderRunnable, Consignee receiverRunnable, BlockingQueue<SenderConsignment> messagesQueue)
	{
		this.topic = topic;
		this.senderRunnable = senderRunnable;
		this.receiverRunnable = receiverRunnable;
		this.messagesQueue = messagesQueue;
	}

	/**
	 * @param topic
	 *            the topic to set
	 */
	public void setTopic(Topic topic)
	{
		this.topic = topic;
	}

	/**
	 * @param senderRunnable
	 *            the senderRunnable to set
	 */
	public void setSenderRunnable(Consigner senderRunnable)
	{
		this.senderRunnable = senderRunnable;
	}

	/**
	 * @param receiverRunnable
	 *            the receiverRunnable to set
	 */
	public void setReceiverRunnable(Consignee receiverRunnable)
	{
		this.receiverRunnable = receiverRunnable;
	}

	/**
	 * @param messagesQueue
	 *            the messagesQueue to set
	 */
	public void setMessagesQueue(BlockingQueue<SenderConsignment> messagesQueue)
	{
		this.messagesQueue = messagesQueue;
	}

	/**
	 * @return the topic
	 */
	public Topic getTopic()
	{
		return topic;
	}

	/**
	 * @return the senderRunnable
	 */
	public Runnable getSenderRunnable()
	{
		return senderRunnable;
	}

	/**
	 * @return the receiverRunnable
	 */
	public Runnable getReceiverRunnable()
	{
		return receiverRunnable;
	}

	/**
	 * @return the messagesQueue
	 */
	public BlockingQueue<SenderConsignment> getMessagesQueue()
	{
		return messagesQueue;
	}

	public void putInQueue(SenderConsignment senderConsignment)
	{
		try
		{
			messagesQueue.put(senderConsignment);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	public void releaseResources()
	{
		senderRunnable.stop();
		receiverRunnable.stop();
		messagesQueue.clear();
	}
}
