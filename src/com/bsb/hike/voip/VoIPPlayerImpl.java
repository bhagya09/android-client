package com.bsb.hike.voip;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.bsb.hike.utils.Logger;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by anuj on 15/04/16.
 */

public class VoIPPlayerImpl implements VoIPPlayer {

    private Thread playbackThread;
    private AudioTrack audioTrack;
    private boolean keepRunning;
    private int playbackSampleRate;
    private int minBufSizePlayback;

    private final LinkedBlockingQueue<VoIPDataPacket> playbackBuffersQueue      = new LinkedBlockingQueue<>();
    private final String tag = VoIPConstants.TAG + getClass().getSimpleName();

    @Override
    public void start(final PlayerCallback callback) {
        keepRunning = true;

        playbackThread = new Thread(new Runnable() {

            @Override
            public void run() {

                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                int index;
                int size;

                playbackSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_VOICE_CALL);
                minBufSizePlayback = AudioTrack.getMinBufferSize(playbackSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                Logger.d(tag, "AUDIOTRACK - minBufSizePlayback: " + minBufSizePlayback + ", playbackSampleRate: " + playbackSampleRate);

                try {
                    audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, playbackSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizePlayback, AudioTrack.MODE_STREAM);
                } catch (IllegalArgumentException e) {
                    Logger.w(tag, "Unable to initialize AudioTrack: " + e.toString());
                    callback.onInitFailure();
                    return;
                }

                try {
                    audioTrack.play();
                } catch (IllegalStateException e) {
                    Logger.e(tag, "Audiotrack error: " + e.toString());
                    callback.onInitFailure();
                }

                while (keepRunning) {
                    VoIPDataPacket dp;
                    try {
                        dp = playbackBuffersQueue.take();
                        if (dp != null) {

                            callback.aboutToPlay(dp);

                            // Resample
                            byte[] output = dp.getData();
                            if (output == null)
                                continue;

                            if (playbackSampleRate != VoIPConstants.AUDIO_SAMPLE_RATE) {
                                output = callback.resample(dp.getData(), 16, VoIPConstants.AUDIO_SAMPLE_RATE, playbackSampleRate);
                            }

                            // For streaming mode, we must write data in chunks <= buffer size
                            index = 0;
                            while (index < output.length) {
                                size = Math.min(minBufSizePlayback, output.length - index);
                                audioTrack.write(output, index, size);
                                index += size;
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                if (audioTrack != null) {
                    try {
                        audioTrack.pause();
                        audioTrack.flush();
                        audioTrack.release();
                        audioTrack = null;
                    } catch (IllegalStateException e) {
                        Logger.w(tag, "Audiotrack IllegalStateException: " + e.toString());
                    }
                }

            }
        }, "PLAY_BACK_THREAD");

        playbackThread.start();

    }

    @Override
    public void addToQueue(VoIPDataPacket dp) throws InterruptedException {
        if (playbackBuffersQueue.size() < VoIPConstants.MAX_SAMPLES_BUFFER)
            playbackBuffersQueue.put(dp);
        else
            Logger.w(tag, "Playback buffers queue full.");

    }

    @Override
    public void stop() {
        playbackThread.interrupt();
        keepRunning = false;
        playbackBuffersQueue.clear();
    }

}
