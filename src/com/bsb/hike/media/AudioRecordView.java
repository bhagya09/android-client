package com.bsb.hike.media;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.filetransfer.FTAnalyticEvents;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class AudioRecordView
{
	private static final int AUDIO_RECORD_VIEW_ID = 121;

	// DIALOG STATES
	private static final byte IDLE = 1;

	private static final byte RECORDING = 2;

	private static final byte RECORDED = 3;

	private static final byte PLAYING = 4;

	// DIALOG STATES ENDS
	private Dialog dialog;

	private Activity activity;

	private byte recorderState;

	private long recordStartTime, recordedTime;

	private MediaRecorder recorder;

	private File selectedFile;

	private Handler recordingHandler = new Handler();

	private UpdateRecordingDuration updateRecordingDuration;

	private HikeAudioRecordListener listener;

	private static final long MIN_DURATION = 1000;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public AudioRecordView(Activity activity, HikeAudioRecordListener listener)
	{
		this.activity = activity;
		this.listener = listener;
	}

	public void show()
	{
		initView();
		recorderState = IDLE;
		selectedFile = Utils.getOutputMediaFile(HikeFileType.AUDIO_RECORDING, null, true);
		if (selectedFile != null)
		{
			recorder.setOutputFile(selectedFile.getPath());
			dialog.show();
		}
		else
		{
			Toast.makeText(activity, R.string.card_unmount, Toast.LENGTH_SHORT).show();
			listener.audioRecordCancelled(HikeAudioRecordListener.AUDIO_CANCELLED_NOSPACE);
		}
	}

	private void initView()
	{

		dialog = new HikeDialog(activity, AUDIO_RECORD_VIEW_ID);

		dialog.setContentView(R.layout.record_audio_dialog);

		final TextView recordInfo = (TextView) dialog.findViewById(R.id.record_info);
		final ImageView recordImage = (ImageView) dialog.findViewById(R.id.record_img);
		final Button cancelBtn = (Button) dialog.findViewById(R.id.cancel_btn);
		final Button sendBtn = (Button) dialog.findViewById(R.id.send_btn);
		final ImageButton recordBtn = (ImageButton) dialog.findViewById(R.id.btn_record);

		recordBtn.setEnabled(true);

		recordBtn.setImageResource(R.drawable.ic_record_selector);
		sendBtn.setEnabled(false);

		recordingHandler = new Handler();

		initialiseRecorder(recordBtn, recordInfo, recordImage, cancelBtn, sendBtn);

		recordBtn.setOnTouchListener(new OnTouchListener()
		{

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				int action = event.getAction();
				if (recorderState == RECORDED || recorderState == PLAYING)
				{
					return false;
				}
				switch (action)
				{
				case MotionEvent.ACTION_DOWN:
					if (recorderState == RECORDING)
					{
						return false;
					}
					recordBtn.setPressed(true);
					// New recording
					if (recorder == null)
					{
						initialiseRecorder(recordBtn, recordInfo, recordImage, cancelBtn, sendBtn);
						if(selectedFile == null)
						{
							selectedFile = Utils.getOutputMediaFile(HikeFileType.AUDIO_RECORDING, null, true);
						}
					}
					
					if((recorder != null) && (selectedFile != null))
					{
						try
						{
							recorder.setOutputFile(selectedFile.getPath());
							recorder.prepare();
							recorder.start();
							recordStartTime = System.currentTimeMillis();
							setupRecordingView(recordInfo, recordImage, recordStartTime);
							recorderState = RECORDING;
						}
						catch (IOException e)
						{
							stopRecorder();
							recordingError(true);
							dialog.dismiss();
							Logger.e(getClass().getSimpleName(), "Failed to start recording", e);
							return false;
						}
						catch (IllegalStateException e)
						{
							stopRecorder();
							recordingError(true);
							dialog.dismiss();
							Logger.e(getClass().getSimpleName(), "Failed to start recording", e);
							return false;
						}
						catch (RuntimeException e)
						{
							stopRecorder();
							recordingError(true);
							dialog.dismiss();
							Logger.e(getClass().getSimpleName(), "Failed to start recording", e);
							return false;
						}
	
						Utils.blockOrientationChange(activity);
						activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					}
					else
					{
						stopRecorder();
						recordingError(true);
						dialog.dismiss();
						Logger.e(getClass().getSimpleName(), "Failed to start recording empty selectedFile");
						return false;
					}
					return true;
				case MotionEvent.ACTION_UP:
					if (recorderState != RECORDING)
					{
						return false;
					}
					recordBtn.setPressed(false);
					recorderState = IDLE;
					stopRecorder();
					long mDuration = (System.currentTimeMillis() - recordStartTime);
					if(mDuration < MIN_DURATION)
					{
						recordingError(true);
						recordInfo.setText(R.string.hold_talk);
					}
					else
					{
						recordedTime = mDuration / 1000;
						setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordedTime);
					}
					activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					Utils.unblockOrientationChange(activity);
					return true;
				}
				return false;
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				dialog.cancel();
				if (selectedFile != null)
				{
					selectedFile.delete();
				}
				listener.audioRecordCancelled(HikeAudioRecordListener.AUDIO_CANCELLED_BY_USER);
			}
		});

		sendBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				dialog.dismiss();
				if (selectedFile == null)
				{
					recordingError(true);
					FTAnalyticEvents.logDevError(FTAnalyticEvents.UPLOAD_INIT_4_1, 0, FTAnalyticEvents.UPLOAD_FILE_TASK, "init", "Audio recording error");
					return;
				}
				listener.audioRecordSuccess(selectedFile.getPath(), recordedTime);
			}
		});

		dialog.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				stopRecorder();
				recordingError(false);
			}
		});

		dialog.show();
	}

	private void setUpPreviewRecordingLayout(ImageButton recordBtn, TextView recordText, ImageView recordImage, Button sendBtn, long duration)
	{
		recorderState = RECORDED;

		recordBtn.setEnabled(false);
		recordBtn.setImageResource(R.drawable.ic_big_tick);
		recordImage.setImageResource(R.drawable.ic_recorded);
		Utils.setupFormattedTime(recordText, duration);

		sendBtn.setEnabled(true);
	}

	private void setUpPlayingRecordingLayout(ImageButton recordBtn, TextView recordInfo, ImageView recordImage, Button sendBtn, long startTime)
	{
		recorderState = PLAYING;

		sendBtn.setEnabled(true);

		updateRecordingDuration = new UpdateRecordingDuration(recordInfo, recordImage, startTime, 0);
		recordingHandler.post(updateRecordingDuration);
	}

	private void recordingError(boolean showError)
	{
		recorderState = IDLE;

		if (showError)
		{
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity, R.string.error_recording, Toast.LENGTH_SHORT).show();
				}
			});
		}
		if (selectedFile == null)
		{
			return;
		}
		if (selectedFile.exists())
		{
			selectedFile.delete();
			selectedFile = null;
		}
	}

	private void stopRecorder()
	{
		if (updateRecordingDuration != null)
		{
			recordingHandler.removeCallbacks(updateRecordingDuration);
			updateRecordingDuration.stopUpdating();
			updateRecordingDuration = null;
		}
		if (recorder != null)
		{
			/*
			 * Catching RuntimeException here to prevent the app from crashing when the the media recorder is immediately stopped after starting.
			 */
			try
			{
				recorder.stop();
			}
			catch (RuntimeException e)
			{
			}
			recorder.reset();
			recorder.release();
			recorder = null;
		}
		recorderState = IDLE;
	}

	private void setupRecordingView(TextView recordInfo, ImageView recordImage, long startTime)
	{
		recorderState = RECORDING;

		updateRecordingDuration = new UpdateRecordingDuration(recordInfo, recordImage, startTime, R.drawable.ic_recording);
		recordingHandler.post(updateRecordingDuration);
	}

	private void initialiseRecorder(final ImageButton recordBtn, final TextView recordInfo, final ImageView recordImage, final Button cancelBtn, final Button sendBtn)
	{
		if (recorder == null)
		{
			/* CE-62: Handling MediaRecorder.setAudioSource crash, observed when the sound
			   or audio recording permission is disabled/forbidden from settings */
			try {
				recorder = new MediaRecorder();
				recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
				recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
				recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
				recorder.setMaxDuration(HikeConstants.MAX_DURATION_RECORDING_SEC * 1000);
				recorder.setMaxFileSize(HikeConstants.MAX_FILE_SIZE);
			} catch (Exception e) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(activity, R.string.error_recording, Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
		
		recorder.setOnErrorListener(new OnErrorListener()
		{
			@Override
			public void onError(MediaRecorder mr, int what, int extra)
			{
				stopRecorder();
				recordingError(true);
			}
		});
		recorder.setOnInfoListener(new OnInfoListener()
		{
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra)
			{
				stopRecorder();
				if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
				{
					recordedTime = (System.currentTimeMillis() - recordStartTime) / 1000;
					setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordedTime);
				}
				else
				{
					recordingError(true);
				}
			}
		});
	}

	private class UpdateRecordingDuration implements Runnable
	{
		private long startTime;

		private TextView durationText;

		private ImageView recordImage;

		private boolean keepUpdating = true;

		private boolean imageSet = false;

		private int imageRes;

		public UpdateRecordingDuration(TextView durationText, ImageView iv, long startTime, int imageRes)
		{
			this.durationText = durationText;
			this.recordImage = iv;
			this.startTime = startTime;
			this.imageRes = imageRes;
		}

		public void stopUpdating()
		{
			keepUpdating = false;
		}

		public long getStartTime()
		{
			return startTime;
		}

		@Override
		public void run()
		{
			long timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
			Utils.setupFormattedTime(durationText, timeElapsed);
			if (!imageSet)
			{
				recordImage.setImageResource(imageRes);
				imageSet = true;
			}
			if (keepUpdating)
			{
				recordingHandler.postDelayed(updateRecordingDuration, 500);
			}
		}
	};
	
	public void dismissAudioRecordView()
	{
		if (dialog != null && dialog.isShowing())
		{
			dialog.dismiss();
		}
	}
}
