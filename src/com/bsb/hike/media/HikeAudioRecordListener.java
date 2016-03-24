package com.bsb.hike.media;

/**
 * Created by nidhi on 26/02/16.
 */
public interface HikeAudioRecordListener {
    public static final int AUDIO_CANCELLED_DEFAULT = 0;
    public static final int AUDIO_CANCELLED_BY_USER = 1;
    public static final int AUDIO_CANCELLED_MINDURATION = 2;
    public static final int AUDIO_CANCELLED_NOSPACE = 3;


    public void audioRecordSuccess(String filePath, long duration);

    public void audioRecordCancelled(int cause);
}
