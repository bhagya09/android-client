package com.bsb.hike.voip;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

public class SolicallWrapper {
	
	private native int packageInit();
	private native int AECInit(int CpuNR, int CpuAEC, short AECMinOutputPercentageDuringEcho, 
			short AECTypeParam, short ComfortNoisePercent, int AECTailType);
	private native int processSpeakerFrame(byte[] stream);
	private native int processMicFrame(byte[] input, byte[] output);
	private native int terminate();
	
	public static final int SOLICALL_FRAME_SIZE = 960; 

	static {
		System.loadLibrary("solicall");
	}
	
	public void init() {
		int init = packageInit();
		
		// Get AEC parameters
		HikeSharedPreferenceUtil sharedPref = HikeSharedPreferenceUtil.getInstance();
		int CpuNoiseReduction = sharedPref.getData(HikeConstants.VOIP_AEC_CPU_NR, 0);
		int CpuAEC = sharedPref.getData(HikeConstants.VOIP_AEC_CPU, 2);
		short AecMinOutput = (short) sharedPref.getData(HikeConstants.VOIP_AEC_MO, 0);
		short AecTypeParam = (short) sharedPref.getData(HikeConstants.VOIP_AEC_TYPE, 4);
		short comfortNoise = (short) sharedPref.getData(HikeConstants.VOIP_AEC_CNP, 100);
		int AecTailType = sharedPref.getData(HikeConstants.VOIP_AEC_TAIL_TYPE, -18);
		
		Logger.d(VoIPConstants.TAG, "AEC parameters: " + CpuNoiseReduction + ", " 
				+ CpuAEC + ", " 
				+ AecMinOutput + ", " 
				+ AecTypeParam + ", " 
				+ comfortNoise + ", " 
				+ AecTailType);
		
		// Initialize AEC
		init = AECInit(CpuNoiseReduction, CpuAEC, AecMinOutput, AecTypeParam, comfortNoise, AecTailType);
		Logger.d(VoIPConstants.TAG, "AEC init: " + init);
	}
	
	public void destroy() {
		terminate();
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
