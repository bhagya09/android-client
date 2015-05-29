package com.bsb.hike.voip;

import java.io.File;
import java.io.IOException;

import android.content.Context;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.UnzipUtil;

public class SolicallWrapper 
{
	
	private native int packageInit();
	private native int AECInit(int CpuNR, int CpuAEC, short AECMinOutputPercentageDuringEcho, 
			short AECTypeParam, short ComfortNoisePercent, int AECTailType);
	private native int processSpeakerFrame(byte[] stream);
	private native int processMicFrame(byte[] input, byte[] output);
	private native int terminate();
	
	public static final int SOLICALL_FRAME_SIZE = 960;

	private String TAG = "SolicallWrapper";

	public SolicallWrapper() throws IOException
	{
		try
		{
			System.loadLibrary("solicall");	
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
			String libName = "libsolicall.so";
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
	
	public void init() {
		synchronized (this) {
			int init = packageInit();
			
			// Get AEC parameters
			HikeSharedPreferenceUtil sharedPref = HikeSharedPreferenceUtil.getInstance();
			int CpuNoiseReduction = sharedPref.getData(HikeConstants.VOIP_AEC_CPU_NR, 0);
			int CpuAEC = sharedPref.getData(HikeConstants.VOIP_AEC_CPU, 2);
			short AecMinOutput = (short) sharedPref.getData(HikeConstants.VOIP_AEC_MO, 0);
			short AecTypeParam = (short) sharedPref.getData(HikeConstants.VOIP_AEC_TYPE, 1); // Before 24 May, value was 4
			short comfortNoise = (short) sharedPref.getData(HikeConstants.VOIP_AEC_CNP, 100);
			int AecTailType = sharedPref.getData(HikeConstants.VOIP_AEC_TAIL_TYPE, -18);
			
//			Logger.d(VoIPConstants.TAG, "AEC parameters: " + CpuNoiseReduction + ", " 
//					+ CpuAEC + ", " 
//					+ AecMinOutput + ", " 
//					+ AecTypeParam + ", " 
//					+ comfortNoise + ", " 
//					+ AecTailType);
			
			// Initialize AEC
			init = AECInit(CpuNoiseReduction, CpuAEC, AecMinOutput, AecTypeParam, comfortNoise, AecTailType);
			// Logger.d(VoIPConstants.TAG, "AEC init: " + init);
		}
	}
	
	public void destroy() {
		synchronized (this) {
			terminate();
		}
	}
	
	public void processSpeaker(byte[] frame) {
		synchronized (this) {
			processSpeakerFrame(frame);
		}
	}
	
	public int processMic(byte[] frame) {
		int ret = 0;
		synchronized (this) {
			ret = processMicFrame(frame, null);
		}
		return ret;
	}
}
