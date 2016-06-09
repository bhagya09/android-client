package com.bsb.hike.voip;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AutomaticGainControl;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class VoIPRecorderImpl implements VoIPRecorder {

    private Thread recordingThread;
    private int recordingSampleRate;
    private int minBufSizeRecording;
    private int preferredFrameSize;
    private boolean isRunning;
    private boolean mute;

    private final String tag = VoIPConstants.TAG + getClass().getSimpleName();

    /**
     *
     * @param preferredFrameSize Pass a negative value if there is no preferred size.
     */
    public VoIPRecorderImpl(int preferredFrameSize) {
        this.preferredFrameSize = preferredFrameSize;
    }

    @Override
    public void startRecording(final RecorderCallback callback) {

        isRunning = true;
        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {

                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

                AudioRecord recorder = null;

                int audioSource = MediaRecorder.AudioSource.MIC;

                // Start recording audio from the mic
                // Try different sample rates
                for (int rate : new int[]{VoIPConstants.AUDIO_SAMPLE_RATE, 44100, 24000, 22050}) {
                    try {
                        recordingSampleRate = rate;

                        minBufSizeRecording = AudioRecord.getMinBufferSize(recordingSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                        if (minBufSizeRecording < 0) {
                            Logger.w(tag, "Sample rate " + recordingSampleRate + " is not valid.");
                            continue;
                        }

                        if (preferredFrameSize > 0) {
                            Logger.d(tag, "Old minBufSizeRecording: " + minBufSizeRecording + " at sample rate: " + recordingSampleRate);

                            if (minBufSizeRecording < preferredFrameSize * 2)
                                minBufSizeRecording = preferredFrameSize * 2;
                            else
                                minBufSizeRecording = ((minBufSizeRecording + (preferredFrameSize * 2) - 1) / (preferredFrameSize * 2)) * preferredFrameSize * 2;
                            Logger.d(tag, "New minBufSizeRecording: " + minBufSizeRecording);
                        }

                        recorder = new AudioRecord(audioSource, recordingSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizeRecording);
                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                            recorder.startRecording();
                            break;
                        } else {
                            recorder.release();
                        }
                    } catch (IllegalArgumentException e) {
                        Logger.e(tag, "AudioRecord init failed (" + recordingSampleRate + "): " + e.toString());
                    } catch (IllegalStateException e) {
                        Logger.e(tag, "Recorder exception (" + recordingSampleRate + "): " + e.toString());
                    }

                }

                if (recorder == null || recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    Logger.e(tag, "AudioRecord initialization failed. Mic may not work.");
                    callback.onInitFailure();
                    return;
                }

                // Attach AGC
                try {
                    if (Utils.isJellybeanOrHigher()) {
                        if (AutomaticGainControl.isAvailable()) {
                            AutomaticGainControl agc = AutomaticGainControl.create(recorder.getAudioSessionId());
                            if (agc != null) {
                                Logger.w(VoIPConstants.TAG, "Initial AGC status: " + agc.getEnabled());
                                agc.setEnabled(true);
                            }
                        } else
                            Logger.w(tag, "AGC not available.");
                    }
                } catch (NullPointerException e) {
                    // java.lang.NullPointerException at android.media.audiofx.AudioEffect.isEffectTypeAvailable(AudioEffect.java:482) at
                    // android.media.audiofx.AutomaticGainControl.isAvailable(AutomaticGainControl.java:51)
                    Logger.w(tag, "AutomaticGainControl NPE: " + e.toString());
                }

                // Start processing recorded data
                byte[] recordedData = new byte[minBufSizeRecording];
                int retVal;
                while (isRunning) {
                    retVal = recorder.read(recordedData, 0, recordedData.length);
                    if (retVal != recordedData.length) {
                        Logger.w(tag, "Unexpected recorded data length. Expected: " + recordedData.length + ", Recorded: " + retVal);
                        continue;
                    }

                    if (mute)
                        continue;

                    // Resample
                    byte[] output;
                    if (recordingSampleRate != VoIPConstants.AUDIO_SAMPLE_RATE) {
                        // We need to resample the mic signal
                        output = callback.resample(recordedData, 16, recordingSampleRate, VoIPConstants.AUDIO_SAMPLE_RATE);
                    } else
                        output = recordedData;

                    // Break input audio into smaller chunks for Solicall AEC
                    int index = 0;
                    int newSize;
                    while (index < retVal) {
                        if (retVal - index < SolicallWrapper.SOLICALL_FRAME_SIZE * 2)
                            newSize = retVal - index;
                        else
                            newSize = SolicallWrapper.SOLICALL_FRAME_SIZE * 2;

                        byte[] data = new byte[newSize];
                        System.arraycopy(output, index, data, 0, newSize);
                        index += newSize;

                        // Add it to the samples to encode queue
                        callback.onDataAvailable(data);
                    }

                    if (Thread.interrupted()) {
                        break;
                    }
                }

                // Stop recording
                if (recorder != null)
                    if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                        recorder.stop();
                    }

                recorder.release();
            }
        }, "RECORDING_THREAD");

        recordingThread.start();

    }

    @Override
    public void stop() {
        recordingThread.interrupt();
        isRunning = false;
    }

    @Override
    public void setMute(boolean mute) {
        this.mute = mute;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

}
