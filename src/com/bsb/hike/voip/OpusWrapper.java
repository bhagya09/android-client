package com.bsb.hike.voip;

import com.bsb.hike.utils.Logger;


public class OpusWrapper {
	
	private static long encoder = 0;
	private static long decoder = 0;
	public static final int OPUS_FRAME_SIZE = 2880; // permitted values are 120, 240, 480, 960, 1920, and 2880
	public static final int OPUS_LOWEST_SUPPORTED_BITRATE = 3000; 
	
	private native long opus_encoder_create(int samplingRate, int channels, int errors);
	private native void opus_encoder_destroy(long encoder);
	private native int opus_queue(byte[] stream);	
	private native int opus_get_encoded_data(long encoder, int frameSize, byte[] output, int maxDataBytes);
	private native void opus_set_bitrate(long encoder, int bitrate);
	private native void opus_set_gain(long decoder, int gain);
	private native void opus_set_complexity(long encoder, int complexity);

	private native long opus_decoder_create(int samplingRate, int channels, int errors);
	private native void opus_decoder_destroy(long decoder);
	private native int opus_decode(long decoder, byte[] stream, int length, byte[] output, int frameSize, int decodeFEC);
	
	private static Object encoderLock = new Object();
	private static Object decoderLock = new Object();
	
	static {
		System.loadLibrary("opuscodec");
	}
	
	public int getEncoder(int samplingRate, int channels, int bitrate) {
		int errors = 0;
		encoder = opus_encoder_create(samplingRate, channels, errors);
		setEncoderBitrate(bitrate);
		return errors;
	}
	
	public void setEncoderBitrate(int bitrate) {
		if (encoder == 0)
			return;
		opus_set_bitrate(encoder, bitrate);
	}
	
	public void setDecoderGain(int gain) {
		if (decoder == 0)
			return;
		
		opus_set_gain(decoder, gain);
		Logger.d(VoIPConstants.TAG, "Setting gain to: " + gain);
	}
	
	public void setEncoderComplexity(int complexity) {
		if (encoder == 0)
			return;
		
		opus_set_complexity(encoder, complexity);
//		Log.d(VoIPConstants.TAG, "Setting complexity to: " + complexity);
	}
	
	public int getDecoder(int samplingRate, int channels) {
		int errors = 0;
		decoder = opus_decoder_create(samplingRate, channels, errors);
		return errors;
	}
	
	public int queue(byte[] stream) throws Exception {
		synchronized (encoderLock) {
			if (encoder == 0)
				throw new Exception("No encoder created.");
			
			return opus_queue(stream);
		}
	}
	
	public int getEncodedData(int frameSize, byte[] output, int maxDataBytes) throws Exception {
		synchronized (encoderLock) {
			if (encoder == 0)
				throw new Exception("No encoder created.");
			
			return opus_get_encoded_data(encoder, frameSize, output, maxDataBytes);
		}
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
	
	public int plc(byte[] input, byte[] output) throws Exception {
		synchronized (decoderLock) {
			if (decoder == 0)
				throw new Exception("No decoder created.");
			
			if (output == null)
				return 0;

			return opus_decode(decoder, null, 0, output, output.length / 2, 1);
		}
	}
	
	public void destroy() {
		synchronized (encoderLock) {
			synchronized (decoderLock) {
				if (encoder != 0)
					opus_encoder_destroy(encoder);
				if (decoder != 0)
					opus_decoder_destroy(decoder);
				
				encoder = 0;
				decoder = 0;
			}
		}
	}
}

