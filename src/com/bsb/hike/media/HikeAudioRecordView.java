package com.bsb.hike.media;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.modules.animationModule.HikeAnimationFactory;
import com.bsb.hike.utils.HikeAnalyticsEvent;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
    private byte recorderState = IDLE;
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
    private PopupWindow popup_l;
    private int DELETE_TRIGGER_DELTA; //Delta for which the delete/cancel option appears
    private int DELETE_REVERT_TRIGGER_DELTA; //Delta for which the delete/cancel option disappears back
    private float recBgrndXPos;

    public HikeAudioRecordView(Activity activity, HikeAudioRecordListener listener) {
        this.mActivity = activity;
        this.listener = listener;
        this.mContext = activity;
        updateTriggerLevels();
    }

    public void onConfigChanged() {
        updateTriggerLevels();
    }

    float micPositionMaxSlide;
    private void updateTriggerLevels(){
        micPositionMaxSlide = DrawUtils.dp(24);
        int screenWidth;
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            screenWidth = DrawUtils.displayMetrics.heightPixels;
            if(HikeMessengerApp.bottomNavBarWidthLandscape != 0){
                micPositionMaxSlide = HikeMessengerApp.bottomNavBarWidthLandscape/2 - DrawUtils.dp(1);
            }
        } else {
            screenWidth = DrawUtils.displayMetrics.widthPixels;
        }
        micPositionMaxSlide = micPositionMaxSlide - screenWidth;

        DELETE_TRIGGER_DELTA = (int) (screenWidth * 0.72);//we change the recording img to delete
        DELETE_REVERT_TRIGGER_DELTA = (int) (screenWidth * 0.72); //CE-434
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
        recBgrndXPos  = rectBgrnd.getX() + DrawUtils.dp(9);
    }


    private void setupRecorderPulsating(ViewStub pulsatingDot) {
        if (pulsatingDot != null) {
            pulsatingDot.setOnInflateListener(new ViewStub.OnInflateListener() {

                @Override
                public void onInflate(ViewStub stub, View inflated) {
                    recorderImg = inflated;
                    recorderImg.setDrawingCacheEnabled(true);
                }
            });

            pulsatingDot.inflate();
        }
    }

    private Runnable ringAnimRunnable1, ringAnimRunnable2;
    /**
     * Used to start mic wave animation
     *
     * @param view
     */
    private void startPulsatingDotAnimation(View view) {
        mHandler.postDelayed(getPulsatingRunnable(view, R.id.mic_image), 0);
        mHandler.postDelayed(ringAnimRunnable1 = getPulsatingRunnable(view, R.id.ring2), 1000);
        mHandler.postDelayed(ringAnimRunnable2 = getPulsatingRunnable(view, R.id.ring2), 1750);
    }

    private Runnable getPulsatingRunnable(final View view, final int viewId) {
        return new Runnable() {
            @Override
            public void run() {
                if(viewId == R.id.ring2) {
                    ImageView ringView = (ImageView) view.findViewById(viewId);
                    if(ringView.getVisibility() != View.VISIBLE) ringView.setVisibility(View.VISIBLE);
                    ringView.startAnimation(HikeAnimationFactory.getScaleFadeRingAnimation(0));
                } else {
                    View micImage = recorderImg.findViewById(R.id.mic_image);
                    micImage.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.scale_to_mid_bounce));
                }
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
                float x = event.getX() + view.getX();
                if (startedDraggingX != -1) {
                    float dist = (x - startedDraggingX);
                    float alpha = 1.0f + dist / distCanMove;
                    if (alpha > 1) {
                        alpha = 1;
                    } else if (alpha < 0) {
                        alpha = 0;
                    }
                    slideToCancel.setAlpha(alpha);
                    //CE-435: Preventing user from sliding beyond the X
                    float maxLeftSlidePossible = micPositionMaxSlide - rectBgrnd.getX();
                    if (dist <= 0.0f && dist >= maxLeftSlidePossible) {
                        recorderImg.setTranslationX(dist);
                    }
                } else {
                    if (event.getX() <= DELETE_TRIGGER_DELTA) startedDraggingX = x;
                    distCanMove = (recorderImg.getMeasuredWidth() - slideToCancel.getMeasuredWidth() - DrawUtils.dp(48)) / 2.0f;
                    if (distCanMove <= 0) {
                        distCanMove = DrawUtils.dp(80);
                    } else if (distCanMove > DrawUtils.dp(80)) {
                        distCanMove = DrawUtils.dp(80);
                    }
                }
                float rawX = event.getRawX();
                if (rawX > DELETE_REVERT_TRIGGER_DELTA)
                {
                    if(rectBgrnd.getVisibility() == View.VISIBLE)
                    rectBgrnd.setVisibility(View.INVISIBLE);
                }
                else if (rawX <= DELETE_TRIGGER_DELTA)
                {
                    if(rectBgrnd.getVisibility() != View.VISIBLE) {
                        rectBgrnd.setVisibility(View.VISIBLE);
                        rectBgrnd.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.scale_to_mid));
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (recorderState == IDLE) return false;
                startedDraggingX = -1;
                if (rectBgrnd .getVisibility() == View.VISIBLE)
                {
                    slideLeftComplete();
                    Log.d(TAG, "   slided in left direction done");
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
        recorderImg.animate().x(recBgrndXPos).setDuration(200).setListener(getAnimationListener()).start();
        recorderState = CANCELLED;
        stopUpdateTimeAndRecorder();
        recordInfo.animate().alpha(0.0f).setDuration(0).start();
        doVibration(50);
    }

    static final int CANCEL_RECORDING = 1;
    static final int DO_CANCEL_ANIMATION = 2;
    static final int DO_SCATTER_ANIMATION = 3;
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
                rectBgrnd.startAnimation(getCrossDissapearScaleInAnimation());
                break;
            case DO_SCATTER_ANIMATION:
                rectBgrnd.setVisibility(View.INVISIBLE);
                ImageView innerView = (ImageView)inflatedLayoutView.findViewById(R.id.delete_inner);
                innerView.setVisibility(View.VISIBLE);
                ImageView outerView = (ImageView) inflatedLayoutView.findViewById(R.id.delete_outer);
                outerView.setVisibility(View.VISIBLE);
                playDeleteAnim(innerView, outerView);
                break;
            default:
                break;
        }
    }

    public static Animation getCrossDissapearScaleInAnimation()
    {
        AnimationSet animSet = new AnimationSet(true);
        float a = 1f;
        float pivotX = 0.5f;
        float pivotY = 0.75f;

        Animation anim0 = new ScaleAnimation(a, 0.0f, a,0.0f, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
        anim0.setDuration(200);
        animSet.addAnimation(anim0);

        return anim0;
    }

    private void postCancelTask() {
        Message message = Message.obtain();
        message.what = DO_CANCEL_ANIMATION;
        mHandler.sendMessage(message);

        Message message1 = Message.obtain();
        message1.what = DO_SCATTER_ANIMATION;
        mHandler.sendMessageDelayed(message1,200);
    }

    private void postCancelMessage(){

        Message cancelMsg = Message.obtain();
        cancelMsg.what = CANCEL_RECORDING;
        mHandler.sendMessageDelayed(cancelMsg,200);
    }

    static int anim_repeat_count;
    private static final int SCATTER_ANIM_DURATION = 100;
    public void playDeleteAnim(View inner, View outer){

        anim_repeat_count = 1;
        AnimatorSet deletingAnimation = new AnimatorSet();

        ArrayList<Animator> viewAnimList = new ArrayList<Animator>();

        ObjectAnimator innerAlpha = ObjectAnimator.ofFloat(inner, "alpha", 0.0f, 1.0f).setDuration(SCATTER_ANIM_DURATION);
        innerAlpha.setRepeatCount(1);
        innerAlpha.setRepeatMode(ValueAnimator.REVERSE);
        viewAnimList.add(innerAlpha);

        ObjectAnimator anim1 = ObjectAnimator.ofFloat(inner, "scaleX", 0.5f, 1.0f).setDuration(SCATTER_ANIM_DURATION);
        anim1.setRepeatCount(1);
        anim1.setRepeatMode(ValueAnimator.REVERSE);
        viewAnimList.add(anim1);

        ObjectAnimator anim1y = ObjectAnimator.ofFloat(inner, "scaleY", 0.5f, 1.0f).setDuration(SCATTER_ANIM_DURATION);
        anim1y.setRepeatCount(1);
        anim1y.setRepeatMode(ValueAnimator.REVERSE);
        viewAnimList.add(anim1y);

        ObjectAnimator outerAlpha = ObjectAnimator.ofFloat(outer, "alpha", 0.0f, 1.0f).setDuration(SCATTER_ANIM_DURATION);
        outerAlpha.setStartDelay(50);
        outerAlpha.setRepeatCount(1);
        outerAlpha.setRepeatMode(ValueAnimator.REVERSE);
        viewAnimList.add(outerAlpha);

        ObjectAnimator anim2 = ObjectAnimator.ofFloat(outer, "scaleX", 0.5f, 1.0f).setDuration(SCATTER_ANIM_DURATION);
        anim2.setRepeatCount(1);
        anim2.setStartDelay(100);
        anim2.setRepeatMode(ValueAnimator.REVERSE);
        viewAnimList.add(anim2);

        ObjectAnimator anim2y = ObjectAnimator.ofFloat(outer, "scaleY", 0.5f, 1.0f).setDuration(SCATTER_ANIM_DURATION);
        anim2y.setRepeatCount(1);
        anim2y.setStartDelay(100);
        anim2y.setRepeatMode(ValueAnimator.REVERSE);
        viewAnimList.add(anim2y);

        deletingAnimation.playTogether(viewAnimList);

        deletingAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                anim_repeat_count--;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (anim_repeat_count > 0) {
                    animator.start();
                } else {
                    postCancelMessage();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        deletingAnimation.start();
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

        } else if(selectedFile.length() == 0){
            /* CE-159: Preventing the empty file from being sent.
                This happen on some devices when sound recording permission is forbidden/denied */
            listener.audioRecordCancelled(HikeAudioRecordListener.AUDIO_CANCELLED_DEFAULT);
            recordingError(true);
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
        /* CE-154: When system dialog waiting for user to permit sound recording,
           the orientation change needs to be blocked, as we unblock it in stopRecorder anyways */
        Utils.blockOrientationChange(mActivity);
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

    public void stopRecorderAndShowError() {
        stopRecorder();
        recordingError(true);
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
                    stopRecorder();
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
            recordInfo.setAlpha(1);
            rectBgrnd.clearAnimation();
            recorderImg.setVisibility(View.VISIBLE);
            rectBgrnd.setVisibility(View.INVISIBLE);
            recordingState.setVisibility(View.VISIBLE);
            clearRingAnim();
        }
    }

    private void clearRingAnim(){
        ImageView ringView = (ImageView) recorderImg.findViewById(R.id.ring2);
        ringView.clearAnimation();
        ringView.setVisibility(View.GONE);
        mHandler.removeCallbacks(ringAnimRunnable1);
        ringAnimRunnable1 = null;
        mHandler.removeCallbacks(ringAnimRunnable2);
        ringAnimRunnable2 = null;
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
