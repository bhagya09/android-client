package com.bsb.hike.analytics;

import com.bsb.hike.utils.Logger;


public class ChatSession {
    private String msisdn;

    private long chatSessionTime;

    private long sessionStartingTimeStamp;

    private boolean sessionEnded = false;

    public ChatSession(String msisdn) {
        this.msisdn = msisdn;

        startChatSession();

        chatSessionTime = -1;
    }

    public void startChatSession() {
        sessionStartingTimeStamp = System.currentTimeMillis();
        sessionEnded = false;
    }

    public long getChatSessionTime() {
        return chatSessionTime;
    }

    private void updateChatSessionTotalTime() {
        long timeSpent = (System.currentTimeMillis() - sessionStartingTimeStamp);

        this.chatSessionTime += timeSpent;

        Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Chat Session Time Spent -- " + timeSpent);
    }

    public void endChatSession() {
        if (sessionEnded) {
            return;
        } else {
            updateChatSessionTotalTime();
            sessionEnded = true;
        }
    }

    public String getMsisdn() {
        return msisdn;
    }
}
