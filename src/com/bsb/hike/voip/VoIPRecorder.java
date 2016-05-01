package com.bsb.hike.voip;

import com.bsb.hike.voip.VoIPDataPacket;

/**
 * Created by anuj on 15/04/16.
 */

public interface VoIPRecorder {

    void startRecording(RecorderCallback callback);
    void stop();
    void setMute(boolean mute);
    boolean isRunning();
    VoIPDataPacket take() throws InterruptedException;


    interface RecorderCallback {
        void onInitFailure();
        byte[] resample(byte[] sourceData, int bitsPerSample, int sourceRate, int targetRate);
    }
}
