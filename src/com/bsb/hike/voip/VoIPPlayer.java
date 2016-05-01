package com.bsb.hike.voip;

import com.bsb.hike.voip.VoIPDataPacket;

/**
 * Created by anuj on 15/04/16.
 */
public interface VoIPPlayer {
    void start(PlayerCallback callback);
    void addToQueue(VoIPDataPacket dp) throws InterruptedException;
    void stop();

    interface PlayerCallback {
        void onInitFailure();
        void aboutToPlay(VoIPDataPacket dp);
        byte[] resample(byte[] sourceData, int bitsPerSample, int sourceRate, int targetRate);

    }
}
