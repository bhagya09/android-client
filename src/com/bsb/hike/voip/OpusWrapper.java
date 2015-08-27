package com.bsb.hike.voip;

import java.io.File;
import java.io.IOException;

import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.UnzipUtil;


public class OpusWrapper {
	
	private long encoder = 0;
	private long decoder = 0;
	private int currentBitrate = 0;
	
	/**
	 * The frame size of samples expected by the Opus codec. 
	 * Permitted values are 120, 240, 480, 960, 1920, and 2880.
	 * Changing this will break backward compatibility.
	 */
	public static final int OPUS_FRAME_SIZE = 2880;
	
	public static final int OPUS_LOWEST_SUPPORTED_BITRATE = 4000; 
	
	private native long opus_encoder_create(int samplingRate, int channels, int errors);
	private native int opus_encode(long encoder, byte[] input, int frameSize, byte[] output, int outputSize);
	private native void opus_encoder_destroy(long encoder);
	private native void opus_set_bitrate(long encoder, int bitrate);
	private native void opus_set_gain(long decoder, int gain);
	private native void opus_set_complexity(long encoder, int complexity);

	private native long opus_decoder_create(int samplingRate, int channels, int errors);
	private native void opus_decoder_destroy(long decoder);
	private native int opus_decode(long decoder, byte[] stream, int length, byte[] output, int frameSize, int decodeFEC);
	private native int opus_plc(long decoder, byte[] output, int frameSize);
	
	private Object encoderLock = new Object();
	private Object decoderLock = new Object();

	private static String TAG = "OpusWrapper";
	
	public OpusWrapper() throws IOException
	{
		try
		{
			System.loadLibrary("opuscodec");
		}
		catch(UnsatisfiedLinkError ex)
		{
			/*
			 * HACK - to avoid library duplication!
			 * System can't load the library from the libs/armeabi dir because: https://code.google.com/p/android/issues/detail?id=9089
			 * Hence, we are extracting the library from the apk source to internal storage and loading it from there.
			 */
			Logger.d(TAG, "Unsatisfied link error, loading library from internal storage");
			Context mContext = HikeMessengerApp.getInstance();
			String destPath = mContext.getFilesDir().toString();
			String libName = "libopuscodec.so";
			String lib = destPath + File.separator + libName;
			File extractedFile = new File(lib);
			if(!extractedFile.isFile())
			{
				Logger.d(TAG, "File does not exists, extracting");
				UnzipUtil.extractFile(mContext.getApplicationInfo().sourceDir, VoIPUtils.ndkLibPath + libName, destPath);
			}
			Logger.d(TAG,"Loading library from internal storage.");
			System.load(lib);
		}
	}
	
	public int getEncoder(int samplingRate, int channels, int bitrate) {
		int errors = 0;
		encoder = opus_encoder_create(samplingRate, channels, errors);
		setEncoderBitrate(bitrate);
		setEncoderComplexity(0);
		return errors;
	}
	
	public void setEncoderBitrate(int bitrate) {
		if (encoder == 0)
			return;
		
		if (bitrate == currentBitrate)
			return;
		
		Logger.d(VoIPConstants.TAG, "Encoder bitrate: " + bitrate);
		opus_set_bitrate(encoder, bitrate);
		currentBitrate = bitrate;
	}
	
	public void setEncoderComplexity(int complexity) {
		if (encoder == 0)
			return;
		
		opus_set_complexity(encoder, complexity);
//		Log.d(VoIPConstants.TAG, "Setting complexity to: " + complexity);
	}
	
	/**
	 * Encode <b>one frame</b> of PCM data. <br/>
	 * Frame size is {@link #OPUS_FRAME_SIZE}. Input buffer should twice as many bytes of data
	 * since each sample is 16-bit.
	 * @param input
	 * @param output
	 * @return
	 * @throws Exception
	 */
	public int encode(byte[] input, byte[] output) throws Exception {
		synchronized (encoderLock) {
			if (encoder == 0)
				throw new Exception("No encoder created.");
			
			if (input == null || output == null)
				return 0;

			return opus_encode(encoder, input, input.length / 2, output, output.length);
		}
	}

	public int getDecoder(int samplingRate, int channels) {
		int errors = 0;
		decoder = opus_decoder_create(samplingRate, channels, errors);
		return errors;
	}
	
	public void setDecoderGain(int gain) {
		if (decoder == 0)
			return;
		
		opus_set_gain(decoder, gain);
		Logger.d(VoIPConstants.TAG, "Setting gain to: " + gain);
	}
	
	public int decode(byte[] input, byte[] output) throws Exception {
		synchronized (decoderLock) {
			if (decoder == 0)
				throw new Exception("No decoder created.");
			
			if (input == null || output == null)
				return 0;

			return opus_decode(decoder, input, input.length, output, output.length / 2, 0);
		}
	}
	
	public int fec(byte[] input, byte[] output) throws Exception {
		synchronized (decoderLock) {
			if (decoder == 0)
				throw new Exception("No decoder created.");
			
			if (input == null || output == null)
				return 0;

			return opus_decode(decoder, input, input.length, output, output.length / 2, 1);
		}
	}
	
	public int plc(byte[] output) throws Exception {
		synchronized (decoderLock) {
			if (decoder == 0)
				throw new Exception("No decoder created.");
			
			if (output == null)
				return 0;

			return opus_plc(decoder, output, output.length / 2);
		}
	}
	
	public void destroy() {
		synchronized (encoderLock) {
			synchronized (decoderLock) {
				if (encoder != 0) {
					opus_encoder_destroy(encoder);
					encoder = 0;
				}
				if (decoder != 0) {
					opus_decoder_destroy(decoder);
					decoder = 0;
				}
			}
		}
	}
}

