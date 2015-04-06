package com.bsb.hike.analytics;

import java.util.Random;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants.MsgRelEventType;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikePacket;
import com.bsb.hike.models.MessagePrivateData;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.Utils;

public class MsgRelLogManager
{
	private static final String TAG = "MsgRelLogManager";
	
	public static Random randomGenerator = new Random();
	
	/**
	 * This starts Msg Tracking
	 * If 'isMessageToBeTracked(msgType)' says true, then updates 'pd' field on convMessage
	 * @param convMessage
	 * @param msgType
	 */
	public static void startMessageRelLogging(ConvMessage convMessage, String msgType)
	{
		if (convMessage != null)
		{
			if (isMessageToBeTracked(msgType))
			{
				Logger.d(AnalyticsConstants.MSG_REL_TAG, "===========================================");
				Logger.d(AnalyticsConstants.MSG_REL_TAG, "Starting message sending");
				if (convMessage.getPrivateData() == null)
				{
					convMessage.setPrivateData(new MessagePrivateData(UUID.randomUUID().toString(), msgType));
				}
				else
				{
					Logger.e(MsgRelLogManager.class.getSimpleName(), "Found Conv Message With NUll PD, should not be case ");
				}
				recordMsgRel(convMessage.getPrivateData().getTrackID(), MsgRelEventType.SEND_BUTTON_CLICKED, msgType);
			}
		}
	}

	/**
	 * 
	 * @param jsonObj
	 */
	public static void logMsgRelDR(JSONObject jsonObj, String eventType)
	{
		try
		{
			if (jsonObj.has(HikeConstants.PRIVATE_DATA))
			{
				String id = jsonObj.optString(HikeConstants.DATA);
				long msgID;
				try
				{
					msgID = Long.parseLong(id);
				}
				catch (NumberFormatException e)
				{
					Logger.e(MsgRelLogManager.class.getSimpleName(), "Exception occured while parsing msgId. Exception : " + e);
					msgID = -1;
				}
				JSONObject pdObject = jsonObj.optJSONObject(HikeConstants.PRIVATE_DATA);
				String trackId = pdObject.optString(HikeConstants.MSG_REL_UID);
				String msgType = pdObject.optString(HikeConstants.MSG_REL_MSG_TYPE);
				if (trackId != null && msgID != -1)
				{
					recordMsgRel(trackId, eventType, msgType);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(TAG, "Exception in MsgRelLogging : " + e.getMessage());
		}
	}

	/**
	 * @param jsonObj
	 * @param eventType
	 * @throws JSONException
	 */
	public static void logMsgRelEvent(JSONObject jsonObj, String eventType) throws JSONException
	{
		try
		{
			if(jsonObj.has(HikeConstants.PRIVATE_DATA))
			{
				JSONObject pd = jsonObj.getJSONObject(HikeConstants.PRIVATE_DATA);
				String trackId = pd.getString(HikeConstants.MSG_REL_UID);
				if (trackId != null)
				{
					long msgId = jsonObj.getLong(HikeConstants.MESSAGE_ID);
					recordMsgRel(trackId, eventType);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.e(TAG, "Exception in MsgRelLogging : " + e.getMessage());
		}
	}

	/**
	 * 
	 * @param convMessage
	 * @param msgType
	 */
	public static void logMsgRelEvent(ConvMessage convMessage, String eventType)
	{
		MessagePrivateData messagePrivateData = convMessage.getPrivateData();
		if (messagePrivateData != null && messagePrivateData.getTrackID() != null && !OneToNConversationUtils.isOneToNConversation(convMessage.getMsisdn()))
		{
			recordMsgRel(messagePrivateData.getTrackID(), eventType, messagePrivateData.getMsgType());
		}
	}

	/**
	 * 
	 * @param packet
	 * @param eventType
	 */
	public static void logPacketForMsgReliability(HikePacket packet, String eventType)
	{
		if (packet.getTrackId() != null)
		{
			recordMsgRel(packet.getTrackId(), eventType);
		}
	}

	/**
	 * Records Event for Msg Reliability With High Priority and NON_UI_Event
	 * @param eventType
	 * @param uid
	 * @param uId
	 * @param msgType
	 */
	
	public static void recordMsgRel(String trackID, String eventType)
	{
		recordMsgRel(trackID, eventType, "-1");
	}
	
	public static void recordMsgRel(String trackID, String eventType, String msgType)
	{
		JSONObject metadata = null;
		try
		{
			metadata = new JSONObject();
			
			// track_id:-
			metadata.put(AnalyticsConstants.TRACK_ID, trackID);
			
			// msg_id:-
			//metadata.put(AnalyticsConstants.MSG_ID, msgId);
			
			//Constant Field need to be added for all the messages as required by Analytics Team
			metadata.put(AnalyticsConstants.MSG_REL_CONST_STR, "msg");
			
			// msg type:- Text/STICKER/Multimedia
			metadata.put(AnalyticsConstants.MESSAGE_TYPE, msgType);
			
			// event type:- 0 to 19
			metadata.put(AnalyticsConstants.MSG_REL_EVENT_TYPE, eventType);
			
			// con:- 2g/3g/4g/wifi/off
			metadata.put(AnalyticsConstants.CONNECTION_TYPE, Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext()));
			
			HAManager.getInstance().record(AnalyticsConstants.MSG_REL, AnalyticsConstants.NON_UI_EVENT, EventPriority.HIGH, metadata, AnalyticsConstants.MSG_REL);
			
			Logger.d(AnalyticsConstants.MSG_REL_TAG, " --track: " + trackID + " --m_type: " + msgType + " --event_num: " + eventType + " --con_type: "
					+ Utils.getNetworkType(HikeMessengerApp.getInstance().getApplicationContext()));
		}
		catch (JSONException e)
		{
			Logger.d(AnalyticsConstants.MSG_REL_TAG, "invalid json");
		}
	}
	
	private static boolean isMessageToBeTracked(String msgType)
	{
		if(!TextUtils.isEmpty(msgType))
		{
			if(AnalyticsConstants.MessageType.TEXT.equals(msgType))
			{
				int randomInt = randomGenerator.nextInt(AnalyticsConstants.MAX_RANGE_TEXT_MSG);
				int probSample = HikeSharedPreferenceUtil.getInstance().
						getData(HikeMessengerApp.PROB_NUM_TEXT_MSG, AnalyticsConstants.text_msg_track_decider);
				Logger.d(AnalyticsConstants.MSG_REL_TAG, " --random number : " + randomInt + ", prob sampling: "+ probSample);
				if(randomInt <= probSample)
				{
					return true;
				}
			}
			else if(AnalyticsConstants.MessageType.STICKER.equals(msgType))
			{
				int randomInt = randomGenerator.nextInt(AnalyticsConstants.MAX_RANGE_STK_MSG);
				int probSample = HikeSharedPreferenceUtil.getInstance().
						getData(HikeMessengerApp.PROB_NUM_STICKER_MSG, AnalyticsConstants.stk_msg_track_decider);
				Logger.d(AnalyticsConstants.MSG_REL_TAG, " --random number : " + randomInt + ", prob sampling: "+ probSample);
				if(randomInt <= probSample)
				{
					return true;
				}
			}
			else if(AnalyticsConstants.MessageType.MULTIMEDIA.equals(msgType))
			{
				int randomInt = randomGenerator.nextInt(AnalyticsConstants.MAX_RANGE_MULTIMEDIA_MSG);
				int probSample = HikeSharedPreferenceUtil.getInstance().
						getData(HikeMessengerApp.PROB_NUM_MULTIMEDIA_MSG, AnalyticsConstants.multimedia_msg_track_decider);
				Logger.d(AnalyticsConstants.MSG_REL_TAG, " --random number : " + randomInt + ", prob sampling: "+ probSample);
				if(randomInt <= probSample)
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static void recordAckMsgRelEvent(HikePacket packet)
	{
		Logger.d(AnalyticsConstants.MSG_REL_TAG, "===========================================");
		Logger.d(AnalyticsConstants.MSG_REL_TAG, "Ack Arrives, track_id:- " + packet.getTrackId());
		if (HikeConstants.MqttMessageTypes.NEW_MESSAGE_READ.equals(packet.getMsgType()))
		{
			MsgRelLogManager.logPacketForMsgReliability(packet, MsgRelEventType.RECEIVER_MQTT_RECV_MSG_ACK);
		}
		else
		{
			MsgRelLogManager.logPacketForMsgReliability(packet, MsgRelEventType.SENDER_RECV_ACK);
		}
	}
	
	public static void recordPacketArrivedAtMqtt(HikePacket packet)
	{
		Logger.d(AnalyticsConstants.MSG_REL_TAG, "===========================================");
		Logger.d(AnalyticsConstants.MSG_REL_TAG, "Packet Arrives at MQTT , track_id:- " + packet.getTrackId());
		if (HikeConstants.MqttMessageTypes.NEW_MESSAGE_READ.equals(packet.getMsgType()))
		{
			MsgRelLogManager.logPacketForMsgReliability(packet, MsgRelEventType.RECEIVER_MQTT_RECV_MR_FROM_RECEIVER);
		}
		else
		{
			MsgRelLogManager.logPacketForMsgReliability(packet, MsgRelEventType.SENDER_MQTT_RECV_SENDING_MSG);
		}
	}
}
