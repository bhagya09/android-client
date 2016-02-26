package com.bsb.hike.media;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import java.io.File;
import java.io.IOException;

public class HikeAudioRecordView {
    private static final String TAG = "HikeAudioRecordView";
    // RECORDING STATES
    private static final byte IDLE = 1;

    private static final byte RECORDING = 2;

    private static final byte RECORDED = 3;

    private static final byte PLAYING = 4;

    private Activity mActivity;

    private byte recorderState;

    private long recordStartTime, recordedTime;

    private MediaRecorder recorder;

    private File selectedFile;

    private Handler recordingHandler = new Handler();

    private UpdateRecordingDuration updateRecordingDuration;

    private HikeAudioRecordListener listener;

    private static final long MIN_DURATION = 1000;

    private Context mContext;

    private ImageView recorderImg, recordingState;
    private TextView slideToCancel, recordInfo;

    private PopupWindow popup_l;

    private int screenWidth;

    public HikeAudioRecordView(Activity activity, HikeAudioRecordListener listener) {
        this.mActivity = activity;
        this.listener = listener;
        this.mContext = activity;

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        LOWER_TRIGGER_DELTA = (int) (screenWidth * 0.80);//DELTA at which we change the recording img to delete
        HIGHER_TRIGGER_DELTA = (int) (screenWidth * 0.50); //DELTA at which delete is triggered
    }

    boolean shouldApplyNavBarOffset() {
        return Utils.isLollipopOrHigher();
    }

    public void initialize(View parent, boolean shareablePopupSharing) {
        this.recorderState = this.IDLE;


        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View inflatedLayoutView = layoutInflater.inflate(R.layout.walkie_talkie_view, null);

        popup_l = new PopupWindow(inflatedLayoutView);
        popup_l.setWidth(parent.getWidth());
        popup_l.setHeight(parent.getHeight());
        int[] loc = new int[2];
        parent.getLocationOnScreen(loc);
        int bottomNavBArThreshold = shouldApplyNavBarOffset() ? HikeMessengerApp.bottomNavBarHeightPortrait : 0;
        popup_l.showAtLocation(parent, Gravity.NO_GRAVITY, 0, loc[1]);
        popup_l.setOutsideTouchable(false);

//        if(shareablePopupSharing)
//            inflatedLayoutView.setPadding(0,0,0,bottomNavBArThreshold);

        recorderImg = (ImageView) inflatedLayoutView.findViewById(R.id.walkie_recorder);
        recordInfo = (TextView) inflatedLayoutView.findViewById(R.id.record_info_duration);
        recordingState = (ImageView) inflatedLayoutView.findViewById(R.id.recording);
        slideToCancel = (TextView) inflatedLayoutView.findViewById(R.id.slidetocancel);

    }

    float historicX = Float.NaN;
    private static int LOWER_TRIGGER_DELTA, HIGHER_TRIGGER_DELTA; // Number of pixels to travel till trigger
    float dX;

    public boolean update(MotionEvent event) {
        if (recorderState == RECORDED || recorderState == PLAYING) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (recorderState == RECORDING) {
                    return false;
                }
                historicX = event.getRawX();
                dX = event.getX() - event.getRawX();
                boolean recordingStarted = startRecordingAudio();
                Log.d(TAG, " action down event recordingStarted: " + recordingStarted);
                return true;

            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, " action move occurred event.getX():: " + event.getX() + " :raw x:" + event.getRawX() + ":historicX:" + historicX + ":screenWidth:" + screenWidth);
                float newX = event.getRawX() - recorderImg.getWidth() / 2;
                recorderImg.animate()
                        .x(newX)
                        .setDuration(0)/*.scaleX(SCALE_FACTOR).scaleY(SCALE_FACTOR)*/
                        .start();
                float newrawX = event.getRawX();
                int diff = (int) (historicX - newrawX);
                slideToCancel.animate().x(slideToCancel.getLeft() - diff).setDuration(0).start();
//                slideToCancel.setFadingEdgeLength((int) newX - slideToCancel.getLeft());
                if (newrawX <= LOWER_TRIGGER_DELTA) {
                    recordingState.setImageResource(R.drawable.ic_white_cross);
                    recordingState.setBackgroundResource(R.drawable.combine_cross_circle);
                } else {
                    recordingState.setImageResource(R.drawable.reddot);
                    recordingState.setBackgroundColor(Color.TRANSPARENT);
                }
                break;
            case MotionEvent.ACTION_UP:
                float rawX = event.getRawX();
                if (rawX <= HIGHER_TRIGGER_DELTA) {
                    Log.d(TAG, "  You slided in left direction: will call cancel now");
                    cancelAndDeleteAudio();
                    return true;
                }

                if (stopRecordingAudio()) {
                    if (selectedFile == null) {
                        recordingError(true);
                        return true;
                    }
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
        stopRecorder();
        recordingError(false);
        listener.audioRecordCancelled();
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
        if(popup_l.isShowing())
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

    ;
}
