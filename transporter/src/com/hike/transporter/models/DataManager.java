package com.hike.transporter.models;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import android.os.Message;

import com.hike.transporter.HandlerUtil;
import com.hike.transporter.Transporter;
import com.hike.transporter.utils.Logger;
import com.hike.transporter.utils.TConstants;
import com.hike.transporter.utils.TConstants.THandlerConstants;

/**
 * 
 * @author himanshu/GauravK
 * 
 * This class manages all the data,publishes the consignment on the topic etc.
 *
 */
public class DataManager
{

	// Mapping of AWB Number vs SenderConsignment
	Map<Long, SenderConsignment> messageMap = null;

	// Mapping of Topic vs Resources
	Map<String, TopicResources> topicMap = null;

	public static volatile DataManager _instance = null;

	public static DataManager getInstance()
	{
		if (_instance == null)
		{
			synchronized (DataManager.class)
			{
				if (_instance == null)
				{
					_instance = new DataManager();
				}
			}
		}
		return _instance;
	}

	private DataManager()
	{
		messageMap = new ConcurrentHashMap<Long, SenderConsignment>();

		topicMap = new ConcurrentHashMap<String, TopicResources>();
	}

	public void putInMessageMap(SenderConsignment senderConsignment)
	{
		// set Awb Number on Sender consignment
		long awb = senderConsignment.getAwb();
		if (awb == -1)
		{
			senderConsignment.setAwb(senderConsignment.hashCode());
			messageMap.put((long) senderConsignment.hashCode(), senderConsignment);
		}
		else
		{
			messageMap.put(senderConsignment.getAwb(), senderConsignment);
		}
	}

	public void putInTopicMap(String topic, TopicResources topicResources)
	{
		topicMap.put(topic, topicResources);
	}

	public boolean containsInMessageMap(long awbNumber)
	{
		return messageMap.containsKey(awbNumber);
	}

	public boolean containsInTopicMap(String topic)
	{
		return topicMap.containsKey(topic);
	}

	public void remove(long awb)
	{
		if (messageMap.containsKey(awb))
		{
			messageMap.remove(awb);
		}
		removeFromPersistanceDb(awb);
	}

	private void removeFromPersistanceDb(long awb)
	{
		Message msg = Message.obtain();
		msg.what = THandlerConstants.DEL_MSG_DB;
		msg.obj = awb;
		HandlerUtil.getInstance().sendMessage(msg);

	}

	public void removeTopicFromMap(String topic)
	{
		if (topicMap.containsKey(topic))
		{
			topicMap.remove(topic);
		}
	}

	public SenderConsignment getSenderConsigment(long awbNumber)
	{
		if (containsInMessageMap(awbNumber))
		{
			return messageMap.get(awbNumber);
		}
		return null;
	}

	public TopicResources getTopicResources(String topic)
	{
		if (containsInTopicMap(topic))
		{
			return topicMap.get(topic);
		}
		return null;
	}

	public void releaseResources()
	{
		Logger.d("DataManager", "Goining to releaseResources");
		
		for (Entry<String, TopicResources> e : topicMap.entrySet())
		{
			TopicResources resources = e.getValue();
			resources.releaseResources();
		}
		messageMap.clear();
		topicMap.clear();
	}

	public void publishMsgToQueue(SenderConsignment senderConsignment)
	{
		if (senderConsignment.ackRequired)
		{
			putInMessageMap(senderConsignment);
		}
		saveMsg(senderConsignment);
		String topic = senderConsignment.getTopic();
		TopicResources topicRes = getTopicResources(topic);
		if (topicRes == null)
		{
			// fallback here for compatibility
			Logger.e("DataManager", "No Topic Resource was found- " + topic + " trying fallback");
			String ackTopic = Transporter.getInstance().getConfig().getAckTopic();
			topicRes = getTopicResources(ackTopic);
		}

		Logger.d("DataManager", "Topic respurces is " + topicRes + "and Sender Consigmnemtn is " + senderConsignment);

		// This is null as the topic res has been cleared and then this code is executing ...A slight possible case
		if (topicRes == null)
		{
			Logger.d("DataManager", "topic Res is null so returning");
			return;
		}

		topicRes.putInQueue(senderConsignment);
	}
	
	public void saveMsg(SenderConsignment senderConsignment)
	{
		if (senderConsignment.persistance)
		{
			Message msg = Message.obtain();
			msg.what = TConstants.THandlerConstants.SAVE_MSG_DB;
			msg.obj = senderConsignment;
			HandlerUtil.getInstance().sendMessage(msg);
		}
	}
}
