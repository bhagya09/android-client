package com.bsb.hike.photos;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.commonsware.cwac.camera.CameraHost;
import com.commonsware.cwac.camera.CameraUtils;
import com.commonsware.cwac.camera.DeviceProfile;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;
import com.commonsware.cwac.camera.CameraHost.FailureReason;
import com.commonsware.cwac.camera.CameraHost.RecordingHint;
import com.commonsware.cwac.camera.SimpleCameraHost.Builder;

public class HikeCameraHost implements CameraHost
{
	private static final String[] SCAN_TYPES = { "image/jpeg", "image/png" };

	private static HikeCameraHost cameraHost;

	private Context ctxt = null;

	private int cameraId = -1;

	private DeviceProfile profile = null;

	private File photoDirectory = null;

	private File videoDirectory = null;

	private RecordingHint recordingHint = null;

	private boolean mirrorFFC = false;

	private boolean useFrontFacingCamera = true;

	private boolean scanSavedImage = false;

	private boolean useFullBleedPreview = true;

	private boolean useSingleShotMode = true;

	private File mLastPhotoFile;

	private HikePhotosListener mListener;

	public Size previewSize;

	public static HikeCameraHost getInstance(boolean useFFC)
	{
		cameraHost = new HikeCameraHost(HikeMessengerApp.getInstance().getApplicationContext());
		cameraHost.useFrontFacingCamera = useFFC;
		return cameraHost;
	}

	private HikeCameraHost(Context argContent)
	{
		ctxt = argContent;
	}

	public void toggleFFC()
	{
		useFrontFacingCamera = useFrontFacingCamera ? false : true;
	}

	@Override
	public Camera.Parameters adjustPictureParameters(PictureTransaction xact, Camera.Parameters parameters)
	{
		return (parameters);
	}

	@Override
	public Camera.Parameters adjustPreviewParameters(Camera.Parameters parameters)
	{
		return (parameters);
	}

	@Override
	public void configureRecorderAudio(int cameraId, MediaRecorder recorder)
	{
		recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	}

	@Override
	public void configureRecorderOutput(int cameraId, MediaRecorder recorder)
	{
		recorder.setOutputFile(getVideoPath().getAbsolutePath());
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void configureRecorderProfile(int cameraId, MediaRecorder recorder)
	{
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_HIGH))
		{
			recorder.setProfile(CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH));
		}
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_LOW))
		{
			recorder.setProfile(CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW));
		}
		else
		{
			throw new IllegalStateException("cannot find valid CamcorderProfile");
		}
	}

	@Override
	public int getCameraId()
	{
		if (cameraId == -1)
		{
			initCameraId();
		}

		return (cameraId);
	}

	private void initCameraId()
	{
		int count = Camera.getNumberOfCameras();
		int result = -1;

		if (count > 0)
		{
			result = 0; // if we have a camera, default to this one

			Camera.CameraInfo info = new Camera.CameraInfo();

			for (int i = 0; i < count; i++)
			{
				Camera.getCameraInfo(i, info);

				if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK && !useFrontFacingCamera())
				{
					result = i;
					break;
				}
				else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && useFrontFacingCamera())
				{
					result = i;
					break;
				}
			}
		}

		cameraId = result;
	}

	@Override
	public DeviceProfile getDeviceProfile()
	{
		if (profile == null)
		{
			initDeviceProfile(ctxt);
		}

		return (profile);
	}

	private void initDeviceProfile(Context ctxt)
	{
		profile = DeviceProfile.getInstance(ctxt);
	}

	@SuppressWarnings({ "unused", "deprecation" })
	@Override
	public Camera.Size getPictureSize(PictureTransaction xact, Camera.Parameters parameters)
	{
		List<Size> sizes = parameters.getSupportedPictureSizes();
		for (Size size : sizes)
		{
			Log.d("CameraHost", "getPictureSize h: " + size.height + " w: " + size.width);
		}
		return (CameraUtils.getLargestPictureSize(this, parameters));
	}

	@Override
	public Camera.Size getPreviewSize(int displayOrientation, int width, int height, Camera.Parameters parameters)
	{
		List<Size> sizes = parameters.getSupportedPictureSizes();
		for (Size size : sizes)
		{
			Log.d("CameraHost", "getPreviewSize h: " + size.height + " w: " + size.width);
		}
		return (CameraUtils.getBestAspectPreviewSize(displayOrientation, width, height, parameters));
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public Camera.Size getPreferredPreviewSizeForVideo(int displayOrientation, int width, int height, Camera.Parameters parameters, Camera.Size deviceHint)
	{
		Log.d("CameraHost", "getPreferredPreviewSizeForVideo");

		if (deviceHint != null)
		{
			previewSize = deviceHint;
			return (deviceHint);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			previewSize = parameters.getPreferredPreviewSizeForVideo();
			return previewSize;
		}

		return (null);
	}

	@Override
	public Camera.ShutterCallback getShutterCallback()
	{
		return (null);
	}

	@Override
	public void handleException(Exception e)
	{
		Log.e(getClass().getSimpleName(), "Exception in setPreviewDisplay()", e);
	}

	@Override
	public boolean mirrorFFC()
	{
		return (mirrorFFC);
	}

	@Override
	public void saveImage(PictureTransaction xact, Bitmap bitmap)
	{
		if (bitmap != null)
		{
			mListener.onComplete(bitmap);
		}
	}

	@Override
	public void saveImage(PictureTransaction xact, final byte[] image)
	{

		Runnable saveImageRunnable = new Runnable()
		{

			@Override
			public void run()
			{
				File defaultPicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

				if (!defaultPicDir.exists())
				{
					defaultPicDir.mkdirs();
				}

				File photo = new File(defaultPicDir, getPhotoFilename());

				if (photo.exists())
				{
					photo.delete();
				}

				FileOutputStream fos = null;
				try
				{
					fos = new FileOutputStream(photo.getPath());
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					bos.write(image);
					bos.flush();
					fos.getFD().sync();
					bos.close();

					//Always scan saved image
					MediaScannerConnection.scanFile(ctxt, new String[] { photo.getPath() }, SCAN_TYPES, null);
				}
				catch (java.io.IOException e)
				{
					handleException(e);
				}
				finally
				{
					if (fos != null)
					{
						try
						{
							fos.flush();
							fos.close();
							mLastPhotoFile = photo;
							// mListener.onComplete(mLastPhotoFile);
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		};

		HikeHandlerUtil.getInstance().postRunnableWithDelay(saveImageRunnable, 0);
	}

	public void setOnImageSavedListener(HikePhotosListener argListener)
	{
		mListener = argListener;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onAutoFocus(boolean success, Camera camera)
	{
		if (success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{
			new MediaActionSound().play(MediaActionSound.FOCUS_COMPLETE);
		}
	}

	@Override
	public boolean useSingleShotMode()
	{
		return (useSingleShotMode);
	}

	@Override
	public void autoFocusAvailable()
	{
		// no-op
	}

	@Override
	public void autoFocusUnavailable()
	{
		// no-op
	}

	@Override
	public RecordingHint getRecordingHint()
	{
		if (recordingHint == null)
		{
			initRecordingHint();
		}

		return (recordingHint);
	}

	private void initRecordingHint()
	{
		recordingHint = profile.getDefaultRecordingHint();

		if (recordingHint == RecordingHint.NONE)
		{
			recordingHint = RecordingHint.ANY;
		}
	}

	@Override
	public void onCameraFail(FailureReason reason)
	{
		Log.e("CWAC-Camera", String.format("Camera access failed"));
	}

	@Override
	public boolean useFullBleedPreview()
	{
		return (useFullBleedPreview);
	}

	@Override
	public float maxPictureCleanupHeapUsage()
	{
		return (1.0f);
	}

	public File getPhotoPath()
	{
		File dir = getPhotoDirectory();

		dir.mkdirs();

		return (new File(dir, getPhotoFilename()));
	}

	protected File getPhotoDirectory()
	{
		if (photoDirectory == null)
		{
			initPhotoDirectory();
		}

		return (photoDirectory);
	}

	private void initPhotoDirectory()
	{
		photoDirectory = new File(Utils.getFileParent(HikeFileType.IMAGE, false));
	}

	protected String getPhotoFilename()
	{
		return Utils.getOriginalFile(HikeFileType.IMAGE, null);
	}

	protected File getVideoPath()
	{
		File dir = getVideoDirectory();

		dir.mkdirs();

		return (new File(dir, getVideoFilename()));
	}

	protected File getVideoDirectory()
	{
		if (videoDirectory == null)
		{
			initVideoDirectory();
		}

		return (videoDirectory);
	}

	private void initVideoDirectory()
	{
		videoDirectory = new File(Utils.getFileParent(HikeFileType.VIDEO, false));
	}

	protected String getVideoFilename()
	{
		return Utils.getOriginalFile(HikeFileType.VIDEO, null);
	}

	protected boolean useFrontFacingCamera()
	{
		return (useFrontFacingCamera);
	}

	protected boolean scanSavedImage()
	{
		return (scanSavedImage);
	}

	public File getLastSavedFile()
	{
		return mLastPhotoFile;
	}
}
