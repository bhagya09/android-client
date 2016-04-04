package com.bsb.hike.media;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
public class HikeAudioRecordView implements PopupWindow.OnDismissListener {
    private static final String TAG = "HikeAudioRecordView";
    // RECORDING STATES
    private static final byte IDLE = 1;
    private static final byte RECORDING = 2;
    private static final byte RECORDED = 3;
    private static final byte PLAYING = 4;
    private static final byte CANCELLED = 5;

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
    private TextView recordInfo;
    private LinearLayout slideToCancel;
    private ImageView rectBgrnd;
    private ViewStub waverMic;
    private float walkieSize;
    private PopupWindow popup_l;
    private int LOWER_TRIGGER_DELTA; //Min Delta of the delete/cancel range - ui to change
    private int HIGHER_TRIGGER_DELTA; //Delta at which delete/cancel is triggered

    public HikeAudioRecordView(Activity activity, HikeAudioRecordListener listener) {
        this.mActivity = activity;
        this.listener = listener;
        this.mContext = activity;
        updateTriggerLevels();
    }

    public void onConfigChanged() {
        updateTriggerLevels();
    }

    private void updateTriggerLevels(){
        int screenWidth;
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            screenWidth = DrawUtils.displayMetrics.heightPixels;
        } else {
            screenWidth = DrawUtils.displayMetrics.widthPixels;
        }
        LOWER_TRIGGER_DELTA = (int) (screenWidth * 0.80);//we change the recording img to delete
        walkieSize = mContext.getResources().getDimensionPixelSize(R.dimen.walkie_mic_size);
        HIGHER_TRIGGER_DELTA = (int) (screenWidth * 0.50 + walkieSize / 2);
    }

    View inflatedLayoutView ;
    private void initViews() {
        this.recorderState = this.IDLE;
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflatedLayoutView = layoutInflater.inflate(R.layout.walkie_talkie_view, null);

        HikeMessengerApp.bottomNavBarHeightPortrait = Utils.getBottomNavBarHeight(mContext);

        recordInfo = (TextView) inflatedLayoutView.findViewById(R.id.record_info_duration);
        recordingState = (RedDot) inflatedLayoutView.findViewById(R.id.recording);
        slideToCancel = (LinearLayout) inflatedLayoutView.findViewById(R.id.slidelayout);
        rectBgrnd = (ImageView) inflatedLayoutView.findViewById(R.id.recording_cancel);
        waverMic = (ViewStub) inflatedLayoutView.findViewById(R.id.walkie_recorder);
        setupRecorderPulsating(waverMic);
    }

    public void initialize(View parent, boolean shareablePopupSharing) {
	    if(inflatedLayoutView == null) initViews();
        if(popup_l != null && popup_l.isShowing()) return;
        selectedFile = Utils.getOutputMediaFile(HikeFile.HikeFileType.AUDIO_RECORDING, null, true);
        popup_l = new PopupWindow(inflatedLayoutView);
        popup_l.setWidth(parent.getWidth());
        popup_l.setHeight(parent.getHeight() * 2);
        popup_l.setOnDismissListener(this);
        int[] loc = new int[2];
        parent.getLocationOnScreen(loc);
        popup_l.setAnimationStyle(R.style.WalkietalkieAnim);
        //CE-93:recording continues even if user has released the finger from the WT icon
        popup_l.setFocusable(false);
        popup_l.setOutsideTouchable(false);

        popup_l.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                popup_l.dismiss();
                return true;
            }
        });

        if (shareablePopupSharing) {
            popup_l.showAtLocation(parent, Gravity.NO_GRAVITY, 0, loc[1] - parent.getHeight());
        } else {
            if (HikeMessengerApp.bottomNavBarHeightPortrait != 0) {
                popup_l.showAtLocation(parent, Gravity.NO_GRAVITY, 0, loc[1] - parent.getHeight());
            } else {
                popup_l.showAtLocation(parent, Gravity.NO_GRAVITY, 0, loc[1]);
            }
        }
        if(recorderImg != null)
            startPulsatingDotAnimation(recorderImg);
    }


    private void setupRecorderPulsating(ViewStub pulsatingDot) {
        if (pulsatingDot != null) {
            pulsatingDot.setOnInflateListener(new ViewStub.OnInflateListener() {

                @Override
                public void onInflate(ViewStub stub, View inflated) {
                    recorderImg = inflated;
                }
            });

            pulsatingDot.inflate();
        }
    }

    /**
     * Used to start mic wave animation
     *
     * @param view
     */
    private void startPulsatingDotAnimation(View view) {
        View micImage = recorderImg.findViewById(R.id.mic_image);
        micImage.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.scale_to_mid_bounce));
        new Handler().postDelayed(getPulsatingRunnable(view, R.id.ring2), 600);
        new Handler().postDelayed(getPulsatingRunnable(view, R.id.ring2), 1350);
    }

    private Runnable getPulsatingRunnable(final View view, final int viewId) {
        return new Runnable() {
            @Override
            public void run() {
                ImageView ringView = (ImageView) view.findViewById(viewId);
                ringView.startAnimation(HikeAnimationFactory.getScaleFadeRingAnimation(0));
            }
        };
    }

    private float startedDraggingX = -1;
    private float distCanMove = DrawUtils.dp(80);

    public boolean update(View view, MotionEvent event) {
        if (recorderState == PLAYING || recorderState == CANCELLED) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (recorderState == RECORDING) {
                    return false;
                }
                boolean recordingStarted = startRecordingAudio();
                Log.d(TAG, " action down event recordingStarted: " + recordingStarted);
                if(!recordingStarted) {
                    return false;
                }
                startedDraggingX = -1;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (recorderState == IDLE) return false;
                Log.d(TAG, " action move occurred event.getX():: " + event.getX() + " :view get x:" + view.getX());
                float x = event.getX();
                x = x + view.getX();
                if (startedDraggingX != -1) {
                    float dist = (x - startedDraggingX);
                    float alpha = 1.0f + dist / distCanMove;
                    if (alpha > 1) {
                        alpha = 1;
                    } else if (alpha < 0) {
                        alpha = 0;
                    }
                    slideToCancel.setAlpha(alpha);
                    if(dist <= 0.0f) recorderImg.setTranslationX(dist);
                } else {
                    if (event.getX() <= LOWER_TRIGGER_DELTA) startedDraggingX = x;
                    distCanMove = (recorderImg.getMeasuredWidth() - slideToCancel.getMeasuredWidth() - DrawUtils.dp(48)) / 2.0f;
                    if (distCanMove <= 0) {
                        distCanMove = DrawUtils.dp(80);
                    } else if (distCanMove > DrawUtils.dp(80)) {
                        distCanMove = DrawUtils.dp(80);
                    }
                }
                float rawX = event.getRawX();
                if (rawX <= LOWER_TRIGGER_DELTA) {
                    if (rawX <= HIGHER_TRIGGER_DELTA) {
                        Log.d(TAG, "  move slided in left direction: will call cancel now");
                        slideLeftComplete();
                        return true;
                    } else {

                        if(rectBgrnd.getVisibility() != View.VISIBLE) {
                            rectBgrnd.setVisibility(View.VISIBLE);
                            rectBgrnd.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.scale_to_mid));
                            recordingState.setVisibility(View.INVISIBLE);
                        }
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
                    Log.d(TAG, "   slided in left direction: will call cancel now" );
                    slideLeftComplete();
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

    private Animator.AnimatorListener mAnimationListener = null;

    private Animator.AnimatorListener getAnimationListener() {
        if (mAnimationListener != null) return mAnimationListener;
        mAnimationListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                postCancelTask();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        };
        return mAnimationListener;
    }

    private void slideLeftComplete() {
        recorderState = CANCELLED;
        doVibration(50);
        stopUpdateTimeAndRecorder();
        recorderImg.animate().x(rectBgrnd.getX() + DrawUtils.dp(10)).setDuration(500).setListener(getAnimationListener()).start();
    }

    static final int CANCEL_RECORDING = 1;
    static final int DO_CANCEL_ANIMATION = 2;
    protected Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg == null) {
                Logger.e(TAG, "Getting a null message in HikeAudioRecordView");
                return;
            }
            handleUIMessage(msg);
        }
    };

    void handleUIMessage(android.os.Message msg) {
        switch (msg.what) {
            case CANCEL_RECORDING:
                cancelAndDeleteAudio(HikeAudioRecordListener.AUDIO_CANCELLED_BY_USER);
                break;
            case DO_CANCEL_ANIMATION:
                recorderImg.setVisibility(View.INVISIBLE);
                rectBgrnd.startAnimation(HikeAnimationFactory.getScaleInAnimation(0));
                break;
            default:
                break;
        }
    }

    private void postCancelTask() {
        Message message = Message.obtain();
        message.what = DO_CANCEL_ANIMATION;
        mHandler.sendMessageDelayed(message, 500);

        Message message1 = Message.obtain();
        message1.what = CANCEL_RECORDING;
        mHandler.sendMessageDelayed(message1,1000);
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

    private HikeTipVisibilityAnimator tipVisibilityAnimator;
    private void showRecordingInfoTip(final int stringResId)
    {
        if (tipVisibilityAnimator == null) {
            tipVisibilityAnimator = new HikeTipVisibilityAnimator(stringResId, inflatedLayoutView, mActivity, R.id.overflow_tip_info, HikeTipVisibilityAnimator.TIP_ANIMATION_LENGTH_SHORT);
        }
        tipVisibilityAnimator.startInfoTipAnim();
    }

    private void dismissTipIfShowing(){
        if(tipVisibilityAnimator != null) tipVisibilityAnimator.dismissInfoTipIfShowing();
    }

    private boolean stopRecordingAudio() {
        if (recorderState == IDLE) {
            return false;
        }

        recorderState = IDLE;
        stopRecorder();
        long mDuration = (System.currentTimeMillis() - recordStartTime);
        if (mDuration < MIN_DURATION) {
            listener.audioRecordCancelled(HikeAudioRecordListener.AUDIO_CANCELLED_MINDURATION);
            recordingError(false);
            return false;

        } else {
            if(mDuration <= HikeConstants.MAX_DURATION_RECORDING_SEC * 1000) {
                recordedTime = mDuration / 1000;
                setUpPreviewRecordingLayout(recordInfo, recordedTime);
            }
        }
        return true;
    }

    private void showUnmountError(){
        recorderState = IDLE;
        stopRecorder();
        Toast.makeText(mActivity, R.string.card_unmount, Toast.LENGTH_SHORT).show();
        listener.audioRecordCancelled(HikeAudioRecordListener.AUDIO_CANCELLED_NOSPACE);
    }

    private boolean startRecordingAudio() {
        if (recorder == null) {
            boolean recorderSuccessful = initialiseRecorder(recordInfo);

            if (recorder != null && selectedFile == null) {
                selectedFile = Utils.getOutputMediaFile(HikeFile.HikeFileType.AUDIO_RECORDING, null, true);
                if(selectedFile == null){
                    showUnmountError();
                    return false;
                }
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
        return true;
    }

    private void setupRecordingView(TextView recordInfo, ImageView recordImage, long startTime) {
        recorderState = RECORDING;
        updateRecordingDuration = new UpdateRecordingDuration(recordInfo, startTime);
        recordingHandler.post(updateRecordingDuration);
    }

    private void cancelAndDeleteAudio(int cause) {
        if(recorderState != CANCELLED) doVibration(50);
        stopRecorder();
        recordingError(false);
        listener.audioRecordCancelled(cause);
    }

    private void doVibration(int timeInMilliSecs) {
        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(timeInMilliSecs);
    }

    private void stopRecorder() {
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Utils.unblockOrientationChange(mActivity);
        stopUpdateTimeAndRecorder();
        dismissAudioRecordView();
        recorderState = IDLE;
    }


    private void stopUpdateTimeAndRecorder(){
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
    }

    private boolean initialiseRecorder(final TextView recordInfo) {
        if (recorder == null) {
            try {
                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                recorder.setMaxDuration(HikeConstants.MAX_DURATION_RECORDING_SEC * 1000);
                recorder.setMaxFileSize(HikeConstants.MAX_FILE_SIZE);
            } catch (Exception e) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mActivity, R.string.error_recording, Toast.LENGTH_SHORT).show();
                    }
                });
                return false;
            }
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
                stopUpdateTimeAndRecorder();
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                    showRecordingInfoTip(R.string.max_duration_recorded);
                    recordedTime = (System.currentTimeMillis() - recordStartTime) / 1000;
                    setUpPreviewRecordingLayout(recordInfo, recordedTime);
                } else {
                    recordingError(true);
                }
            }
        });
        return true;
    }

    private void setUpPreviewRecordingLayout(TextView recordText, long duration) {
        recorderState = RECORDED;
        Utils.setupFormattedTime(recordText, duration);
    }

    public void dismissAudioRecordView() {
        if(popup_l != null && popup_l.isShowing()) {
            recorderImg.clearAnimation();
            dismissTipIfShowing();
            popup_l.dismiss();
        }
    }

    public void cancelAndDismissAudio() {
        if(recorderState != IDLE && recorderState != CANCELLED){
            cancelAndDeleteAudio(HikeAudioRecordListener.AUDIO_CANCELLED_DEFAULT);
        }

        if (popup_l != null && popup_l.isShowing())
            popup_l.dismiss();
    }

    public boolean isShowing(){
        return (popup_l!= null) ? popup_l.isShowing(): false;
    }

    @Override
    public void onDismiss() {
        if (recorderState != IDLE) cancelAndDismissAudio();
        resetAndClearAnim();
        recorderState = IDLE;
    }

    // Fixing issues: (1)Translated x position gets retained (2)Visibility of X & dot doesn't reset
    private void resetAndClearAnim() {
        if (popup_l != null) {
            recorderImg.clearAnimation();
            float amtOfXTranslated = (recorderImg.getX() - recorderImg.getTranslationX());
            recorderImg.setX(amtOfXTranslated);
            slideToCancel.setAlpha(1);
            rectBgrnd.clearAnimation();
            recorderImg.setVisibility(View.VISIBLE);
            rectBgrnd.setVisibility(View.INVISIBLE);
            recordingState.setVisibility(View.VISIBLE);
        }
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
