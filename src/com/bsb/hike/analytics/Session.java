package com.bsb.hike.analytics;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Session
	{
		private long sessionId;

		private String appOpenSource;

		private String msgType;

		private String srcContext;

		private int convType;

		private long sessionStartingBytes;

		private int appId;

		private long sessionStartingTimeStamp;
		
		private Map<String, ChatSession> msisdnChatSessionMap;

		public Session()
		{
			appId = HikeMessengerApp.getInstance().getApplicationInfo().uid;

			sessionStartingTimeStamp = System.currentTimeMillis();

			sessionStartingBytes = Utils.getTotalDataConsumed(appId);

			// 1)sid :- store currentTime in a var (changed via every startS), send that value
			sessionId = System.currentTimeMillis();
			
			appOpenSource = AnalyticsConstants.AppOpenSource.REGULAR_APP_OPEN;
			
			srcContext = "-1";
			
			msisdnChatSessionMap = new HashMap<String, ChatSession>();
		}

		public long getSessionId()
		{
			return sessionId;
		}

		public void setSessionId(long sessionId)
		{
			this.sessionId = sessionId;
		}

		public String getAppOpenSource()
		{
			return appOpenSource;
		}

		public void setAppOpenSource(String appOpenSource)
		{
			this.appOpenSource = appOpenSource;
		}

		public String getMsgType()
		{
			return msgType;
		}

		public void setMsgType(String msgType)
		{
			this.msgType = msgType;
		}

		public String getSrcContext()
		{
			return srcContext;
		}

		public void setSrcContext(String srcContext)
		{
			this.srcContext = srcContext;
		}

		public int getConvType()
		{
			return convType;
		}

		public void setConvType(int convType)
		{
			this.convType = convType;
		}

		public long getSessionStartingBytes()
		{
			return sessionStartingBytes;
		}

		public void setSessionStartingBytes(long sessionStartingBytes)
		{
			this.sessionStartingBytes = sessionStartingBytes;
		}

		public int getAppId()
		{
			return appId;
		}

		public void setAppId(int appId)
		{
			this.appId = appId;
		}

		public long getSessionStartingTimeStamp()
		{
			return sessionStartingTimeStamp;
		}

		public void setSessionStartingTimeStamp(long sessionStartingTimeStamp)
		{
			this.sessionStartingTimeStamp = sessionStartingTimeStamp;
		}

		public void startSession()
		{
			this.sessionId = System.currentTimeMillis();
			
			this.sessionStartingTimeStamp = System.currentTimeMillis();
			
			this.sessionStartingBytes = Utils.getTotalDataConsumed(appId);
		}
		
		public long getSessionTime()
		{
			return System.currentTimeMillis() - this.sessionStartingTimeStamp;
		}
		
		public long getDataConsumedInSession()
		{
			return Utils.getTotalDataConsumed(appId) - this.sessionStartingBytes;
		}
		
		public void reset()
		{
			// reset --> srcctx to "-1"
			srcContext = "-1";

			// reset --> contype to normal
			convType = AnalyticsConstants.ConversationType.NORMAL;

			// reset --> msgtype to ""
			msgType = "";

			appOpenSource = AnalyticsConstants.AppOpenSource.REGULAR_APP_OPEN;
		}
		
		public void endChatSessions()
		{
			ArrayList<ChatSession> chatSessions = getChatSesions();
			if(chatSessions != null && !chatSessions.isEmpty())
			{
				for (ChatSession chatSession : chatSessions)
				{
					chatSession.endChatSession();
				}
			}
		}
		
		public void startChatSession(String msisdn)
		{
			ChatSession chatSession = msisdnChatSessionMap.get(msisdn);
			if (chatSession == null)
			{
				chatSession = new ChatSession(msisdn);

				msisdnChatSessionMap.put(msisdn, chatSession);
			}

			chatSession.startChatSession();

		}

		public void endChatSesion(String msisdn)
		{
			ChatSession chatSession = msisdnChatSessionMap.get(msisdn);
			if (chatSession != null)
			{
				chatSession.endChatSession();
			}
		}

		public ArrayList<ChatSession> getChatSesions()
		{
			return new ArrayList<ChatSession>(msisdnChatSessionMap.values());
		}

        public ChatSession getIndividualChatSesions(String msisdn)
        {
            return msisdnChatSessionMap.get(msisdn);
        }

        public void removeChatSessionFromMap(String msisdn)
        {
            msisdnChatSessionMap.remove(msisdn);
        }
	}