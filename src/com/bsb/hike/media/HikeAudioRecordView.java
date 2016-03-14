package com.bsb.hike.media;

import android.app.Activity;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.io.File;
import java.io.IOException;

/**
 * Created by nidhi on 01/02/16.
 */
public class HikeAudioRecordView {
    private static final String TAG = "HikeAudioRecordView";
    // RECORDING STATES
    private static final byte IDLE = 1;
    private static final byte RECORDING = 2;
    private static final byte RECORDED = 3;
    private static final byte PLAYING = 4;

    private static final long MIN_DURATION = 1000;
    private Activity mActivity;
    private byte recorderState;
    private long recordStartTime, recordedTime;
    private MediaRecorder recorder;
    private File selectedFile;
    private Handler recordingHandler = new Handler();
    private UpdateRecordingDuration updateRecordingDuration;
    private HikeAudioRecordListener listener;

    private Context mContext;
    private View recorderImg;
    private RedDot recordingState;
    private TextView slideToCancel, recordInfo;
    private ImageView rectBgrnd;
    private ViewStub waverMic;
    //    View inflatedLayoutView;
    private float walkieSize;
    private PopupWindow popup_l;
    private final int LOWER_TRIGGER_DELTA; //Min Delta of the delete/cancel range - ui to change
    private final int HIGHER_TRIGGER_DELTA; //Delta at which delete/cancel is triggered

    public HikeAudioRecordView(Activity activity, HikeAudioRecordListener listener) {
        this.mActivity = activity;
        this.listener = listener;
        this.mContext = activity;

        int screenWidth = DrawUtils.displayMetrics.widthPixels;
        LOWER_TRIGGER_DELTA = (int) (screenWidth * 0.80);//we change the recording img to delete
        walkieSize = mContext.getResources().getDimensionPixelSize(R.dimen.walkie_mic_size);
        HIGHER_TRIGGER_DELTA = (int) (screenWidth * 0.50 + walkieSize / 2);
    }

    public void initialize(View parent, boolean shareablePopupSharing) {
        this.recorderState = this.IDLE;
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inflatedLayoutView = layoutInflater.inflate(R.layout.walkie_talkie_view, null);

        HikeMessengerApp.bottomNavBarHeightPortrait = Utils.getBottomNavBarHeight(mContext);

        popup_l = new PopupWindow(inflatedLayoutView);
        popup_l.setWidth(parent.getWidth());
        popup_l.setHeight(parent.getHeight() * 2);
        int[] loc = new int[2];
        parent.getLocationOnScreen(loc);
        if (shareablePopupSharing) {
            popup_l.showAtLocation(parent, Gravity.NO_GRAVITY, 0, loc[1] - parent.getHeight());
        } else {
            if (HikeMessengerApp.bottomNavBarHeightPortrait != 0) {
                popup_l.showAtLocation(parent, Gravity.NO_GRAVITY, 0, loc[1] - parent.getHeight());
            } else {
                popup_l.showAtLocation(parent, Gravity.NO_GRAVITY, 0, loc[1]);
            }
        }

        recordInfo = (TextView) inflatedLayoutView.findViewById(R.id.record_info_duration);
        recordingState = (RedDot) inflatedLayoutView.findViewById(R.id.recording);
        slideToCancel = (TextView) inflatedLayoutView.findViewById(R.id.slidetocancel);
        rectBgrnd = (ImageView) inflatedLayoutView.findViewById(R.id.recording_cancel);
        waverMic = (ViewStub) inflatedLayoutView.findViewById(R.id.walkie_recorder);
        setupRecorderPulsating(waverMic);
    }


    private void setupRecorderPulsating(ViewStub pulsatingDot) {
        if (pulsatingDot != null) {
            pulsatingDot.setOnInflateListener(new ViewStub.OnInflateListener() {

                @Override
                public void onInflate(ViewStub stub, View inflated) {
                    startPulsatingDotAnimation(inflated);
                    recorderImg = inflated;
                }
            });

            pulsatingDot.inflate();
        }
    }

    /**
     * Used to start pulsating dot animation for stickers
     *
     * @param view
     */
    private void startPulsatingDotAnimation(View view) {
        new Handler().postDelayed(getPulsatingRunnable(view, R.id.ring1), 0);
        new Handler().postDelayed(getPulsatingRunnable(view, R.id.ring2), 100);
    }

    private Runnable getPulsatingRunnable(final View view, final int viewId) {
        return new Runnable() {
            @Override
            public void run() {
                ImageView ringView = (ImageView) view.findViewById(viewId);
                if (viewId == R.id.ring1)
                    ringView.startAnimation(HikeAnimationFactory.getScaleRingAnimation(0));
                else
                    ringView.startAnimation(HikeAnimationFactory.getScaleFadeRingAnimation(0));
            }
        };
    }

    private float startedDraggingX = -1;

    public boolean update(View view, MotionEvent event) {
        if (recorderState == RECORDED || recorderState == PLAYING) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (recorderState == RECORDING) {
                    return false;
                }
                boolean recordingStarted = startRecordingAudio();
                Log.d(TAG, " action down event recordingStarted: " + recordingStarted);
                startedDraggingX = -1;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (recorderState == IDLE) return false;
                Log.d(TAG, " action move occurred event.getX():: " + event.getX() + " :view get x:" + view.getX());
                float x = event.getX();
                x = x + view.getX();
                if (startedDraggingX != -1) {
                    float dist = (x - startedDraggingX);
                    recorderImg.setTranslationX(dist);
                    slideToCancel.setTranslationX(dist);
                } else {
                    if (event.getX() <= LOWER_TRIGGER_DELTA) startedDraggingX = x;
                }
                float rawX = event.getRawX();
                if (rawX <= LOWER_TRIGGER_DELTA) {
                    if (rawX <= HIGHER_TRIGGER_DELTA) {
                        Log.d(TAG, " slided in left direction: will call cancel now");
                        cancelAndDeleteAudio();
                        return true;
                    } else {
                        rectBgrnd.setVisibility(View.VISIBLE);
                        recordingState.setVisibility(View.INVISIBLE);
                    }
                } else {
                    rectBgrnd.setVisibility(View.INVISIBLE);
                    recordingState.setVisibility(View.VISIBLE);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (recorderState == IDLE) return false;
                startedDraggingX = -1;
                if (event.getRawX() <= HIGHER_TRIGGER_DELTA) {
                    Log.d(TAG, "   slided in left direction: will call cancel now");
                    cancelAndDeleteAudio();
                    return true;
                }

                if (stopRecordingAudio()) {
                    if (selectedFile == null) {
                        recordingError(true);
                        return true;
                    }
                    Log.d(TAG, "  sending the file now");
                    doVibration(50);
                    listener.audioRecordSuccess(selectedFile.getPath(), recordedTime);
                }

                return true;
        }
        return true;
    }

    private void recordingError(boolean showError) {
        recorderState = IDLE;

        if (showError) {

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity, R.string.error_recording, Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (selectedFile == null) {
            return;
        }
        if (selectedFile.exists()) {
            selectedFile.delete();
            selectedFile = null;
        }
    }

    private boolean stopRecordingAudio() {
        if (recorderState != RECORDING) {
            return false;
        }

        recorderState = IDLE;
        stopRecorder();
        long mDuration = (System.currentTimeMillis() - recordStartTime);
        if (mDuration < MIN_DURATION) {
            recordingError(true);
            return false;

        } else {
            recordedTime = mDuration / 1000;
            setUpPreviewRecordingLayout(recordInfo, recordedTime);
        }
        return true;
    }

    private boolean startRecordingAudio() {
        if (recorder == null) {
            initialiseRecorder(recordInfo);
            if (selectedFile == null) {
                selectedFile = Utils.getOutputMediaFile(HikeFile.HikeFileType.AUDIO_RECORDING, null, true);
                recorder.setOutputFile(selectedFile.getPath());
            }
        }
        if ((recorder != null) && (selectedFile != null)) {
            try {
                recorder.setOutputFile(selectedFile.getPath());
                recorder.prepare();
                recorder.start();
                recordStartTime = System.currentTimeMillis();
                setupRecordingView(this.recordInfo, null, recordStartTime);
                recorderState = RECORDING;
            } catch (IOException e) {
                stopRecorder();
                recordingError(true);
                Logger.e(getClass().getSimpleName(), "Failed to start recording", e);
                return false;
            } catch (IllegalStateException e) {
                stopRecorder();
                recordingError(true);
                Logger.e(getClass().getSimpleName(), "Failed to start recording", e);
                return false;
            } catch (RuntimeException e) {
                stopRecorder();
                recordingError(true);
                Logger.e(getClass().getSimpleName(), "Failed to start recording", e);
                return false;
            }

            Utils.blockOrientationChange(mActivity);
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            stopRecorder();
            recordingError(true);
            Logger.e(getClass().getSimpleName(), "Failed to start recording empty selectedFile");
            return false;
        }
        return false;
    }

    private void setupRecordingView(TextView recordInfo, ImageView recordImage, long startTime) {
        recorderState = RECORDING;
        updateRecordingDuration = new UpdateRecordingDuration(recordInfo, startTime);
        recordingHandler.post(updateRecordingDuration);
    }

    private void cancelAndDeleteAudio() {
        doVibration(50);
        stopRecorder();
        recordingError(false);
        listener.audioRecordCancelled();
    }

    private void doVibration(int timeInMilliSecs) {
        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(timeInMilliSecs);
    }

    private void stopRecorder() {
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Utils.unblockOrientationChange(mActivity);
        if (updateRecordingDuration != null) {
            recordingHandler.removeCallbacks(updateRecordingDuration);
            updateRecordingDuration.stopUpdating();
            updateRecordingDuration = null;
        }
        if (recorder != null) {
            /*
             * Catching RuntimeException here to prevent the app from crashing when the the media recorder is immediately stopped after starting.
			 */
            try {
                recorder.stop();
            } catch (RuntimeException e) {
            }
            recorder.reset();
            recorder.release();
            recorder = null;
        }
        dismissAudioRecordView();
        recorderState = IDLE;
    }

    private void initialiseRecorder(final TextView recordInfo) {
        if (recorder == null) {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setMaxDuration(HikeConstants.MAX_DURATION_RECORDING_SEC * 1000);
            recorder.setMaxFileSize(HikeConstants.MAX_FILE_SIZE);
        }

        recorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                stopRecorder();
                recordingError(true);
            }
        });
        recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                stopRecorder();
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {

                    recordedTime = (System.currentTimeMillis() - recordStartTime) / 1000;
                    setUpPreviewRecordingLayout(recordInfo, recordedTime);
                } else {
                    recordingError(true);
                }
            }
        });
    }

    private void setUpPreviewRecordingLayout(TextView recordText, long duration) {
        recorderState = RECORDED;
        Utils.setupFormattedTime(recordText, duration);
    }

    public void dismissAudioRecordView() {
        if (popup_l.isShowing())
            popup_l.dismiss();
    }

    private class UpdateRecordingDuration implements Runnable {
        private long startTime;

        private TextView durationText;

        private boolean keepUpdating = true;

        public UpdateRecordingDuration(TextView durationText, long startTime) {
            this.durationText = durationText;
            this.startTime = startTime;
        }

        public void stopUpdating() {
            keepUpdating = false;
        }

        public long getStartTime() {
            return startTime;
        }

        @Override
        public void run() {
            long timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
            Utils.setupFormattedTime(durationText, timeElapsed);
            if (keepUpdating) {
                recordingHandler.postDelayed(updateRecordingDuration, 500);
            }
        }
    }
}