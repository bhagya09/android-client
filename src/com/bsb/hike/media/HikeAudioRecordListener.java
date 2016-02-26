package com.bsb.hike.media;

/**
 * Created by nidhi on 26/02/16.
 */
public interface HikeAudioRecordListener {
    public void audioRecordSuccess(String filePath, long duration);

    public void audioRecordCancelled();
}