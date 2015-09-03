#include <stdlib.h>
#include <stdio.h>
#include <android/log.h>
#include "solicalljni.h"
#include "include/solicall_api.h"

#define SAMPLE_RATE 48000
#define FRAME_MULTIPLIER 5
#define BYTES_PER_SAMPLE 2
#define CHANNEL_ID 0
#define FRAME_SIZE 1920

#define  LOG_TAG    "VoIP NDK (Solicall)"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

inline jbyte* get_byte_array(JNIEnv* env, jbyteArray pcm) {
	jboolean isCopy;
	return env->GetByteArrayElements(pcm, &isCopy);
}

inline void release_byte_array(JNIEnv* env, jbyteArray pcm, jbyte* data, jint mode) {
	env->ReleaseByteArrayElements(pcm, data, mode);
}

extern "C"
JNIEXPORT jint JNICALL Java_com_bsb_hike_voip_SolicallWrapper_packageInit
  (JNIEnv *env, jobject obj) {

    sSoliCallPackageInit mySoliCallPackageInit;

	mySoliCallPackageInit.sVersion = 5;
    mySoliCallPackageInit.pcSoliCallBin = "/storage/emulated/0/Android/data/com.bsb.hike/files/";

    int iRes = SoliCallPackageInit(&mySoliCallPackageInit);

    return (jint) iRes;

}

extern "C"
JNIEXPORT jint JNICALL Java_com_bsb_hike_voip_SolicallWrapper_AECInit
  (JNIEnv *env, jobject obj, jint CpuNR, jint CpuAEC, jshort AECMinOutputPercentageDuringEcho,
			jshort AECTypeParam, jshort ComfortNoisePercent, jint AECTailType) {

    sSoliCallInit mySoliCallInit;

    // Advanced AEC
    mySoliCallInit.iCPUPower = CpuAEC;
    mySoliCallInit.sBitsPerSample = (BYTES_PER_SAMPLE==1)?8:16;
    mySoliCallInit.iFrequency = SAMPLE_RATE;
    mySoliCallInit.sFrameSize = FRAME_MULTIPLIER;
    mySoliCallInit.sLookAheadSize = 0;
    mySoliCallInit.bDoNotChangeTheOutput = false;
    mySoliCallInit.sAECTypeParam = AECTypeParam; // 8;
    mySoliCallInit.sDelaySize = 2;
    mySoliCallInit.sMaxAsyncSpeakerDelayAECParam = 50;
    mySoliCallInit.sMaxAsyncMicDelayAECParam = 50;
    mySoliCallInit.sSensitivityLevelAECParam = 6;
    mySoliCallInit.sAggressiveLevelAECParam = 10;
    mySoliCallInit.sAECHowlingLevelTreatment = 10;
    mySoliCallInit.sMaxCoefInAECParam = 100;
    mySoliCallInit.sMinCoefInAECParam = 1;
    mySoliCallInit.sAECTailType = AECTailType; // -10;
    mySoliCallInit.sAECMinTailType = -1;
    mySoliCallInit.iNumberOfSamplesInAECBurst = 2000;
    mySoliCallInit.iNumberOfSamplesInHighConfidenceAECBurst = 2000;
    mySoliCallInit.sAECMinOutputPercentageDuringEcho = AECMinOutputPercentageDuringEcho; // 100; // 25;

    mySoliCallInit.sAecStartupAggressiveLevel = 10;
    mySoliCallInit.sComfortNoisePercent = ComfortNoisePercent; // 100;

    // Initialize AEC
	int iRes = SoliCallAECInit(CHANNEL_ID,&mySoliCallInit);

    mySoliCallInit.iCPUPower = CpuNR;
    mySoliCallInit.sBitsPerSample = (BYTES_PER_SAMPLE==1)?8:16;
	mySoliCallInit.iFrequency = SAMPLE_RATE;
	mySoliCallInit.sFrameSize = FRAME_MULTIPLIER;
	mySoliCallInit.sLookAheadSize = 0;
    mySoliCallInit.bNeedToCheckDTMF = false;
    mySoliCallInit.bRemoveNonSelfFrequencies = true;
	mySoliCallInit.sCNGInitialValue = 15;
	mySoliCallInit.sCNGDecrease = 5;
	mySoliCallInit.sCNGEndValue = 100;
    mySoliCallInit.sBurstEndDecrease = 40;
    mySoliCallInit.sBurstEndNumDecreaseSteps = 1;
    mySoliCallInit.sBurstEndLowerValue = 100;
    mySoliCallInit.sOutputAMPIncrease = 20;
    mySoliCallInit.sDelaySize = 6;
    mySoliCallInit.sDetectAggressiveLevel = 2;
    mySoliCallInit.sCleanAggressiveLevel = 9;
    mySoliCallInit.bCancelAcousticShock = false;
    mySoliCallInit.bDoNotChangeTheOutput = false;
    mySoliCallInit.bBypassVAD = false;

    mySoliCallInit.bActivateAGC = true; // or true to turn ON AGC
    mySoliCallInit.iDesiredAGCAmp = 48000;
    mySoliCallInit.iMinAGCCoef = 50;
    mySoliCallInit.iMaxAGCCoef = 500;

	// Initialize AGC and NR
	iRes = SoliCallInit(CHANNEL_ID,&mySoliCallInit);

    return (jint) iRes;
}

extern "C"
JNIEXPORT jint JNICALL Java_com_bsb_hike_voip_SolicallWrapper_terminate
(JNIEnv *env, jobject obj) {

	if (SoliCallAECTerminate(CHANNEL_ID) != SOLICALL_RC_SUCCESS)
	{
		LOGE("error in terminate\n");
	}

	if (SoliCallTerminate(CHANNEL_ID) != SOLICALL_RC_SUCCESS)
	{
		LOGE("error in terminate\n");
	}
}

extern "C"
JNIEXPORT jint JNICALL Java_com_bsb_hike_voip_SolicallWrapper_processSpeakerFrame
(JNIEnv *env, jobject obj, jbyteArray stream) {

	jbyte *in;
	in = get_byte_array(env, stream);

	if (SoliCallAECProcessSpkFrame(CHANNEL_ID, (BYTE *)in, env->GetArrayLength(stream)) != SOLICALL_RC_SUCCESS)
	{
		LOGE("Error in process frame. Did you pass the call length limit?\n");
	}

	release_byte_array(env, stream, in, JNI_ABORT);
	return 0;

}

extern "C"
JNIEXPORT jint JNICALL Java_com_bsb_hike_voip_SolicallWrapper_processMicFrame
  (JNIEnv *env, jobject obj, jbyteArray input, jbyteArray output) {

	jbyte *in, *out;
	int bytesOut;
	int iVAD, iConfidentVAD, iDTMF, iLastNoiseAmplitude, iEstimatedVoiceAmplitude;
	int iCalculatedAGCCoef, iTmpCurrEchoAmplitude;

	in = get_byte_array(env, input);
//	out = get_byte_array(env, output);

	/*
	// AEC
	if (SoliCallAECProcessMicFrame(CHANNEL_ID, (BYTE *)in, FRAME_SIZE, (BYTE *)in, &bytesOut, &iTmpCurrEchoAmplitude) != SOLICALL_RC_SUCCESS)
	{
		LOGE("Error in process frame. Did you pass the call length limit?\n");
	}
	*/

	/*
	// AGC and NR
	if (SoliCallProcessFrame(CHANNEL_ID, (BYTE *)in, FRAME_SIZE, (BYTE *)in, &bytesOut, &iVAD, &iConfidentVAD, &iDTMF, &iLastNoiseAmplitude, &iEstimatedVoiceAmplitude, &iCalculatedAGCCoef) != SOLICALL_RC_SUCCESS)
	{
		LOGE("Error in process frame. Did you pass the call length limit?\n");
	}
	*/

	// Combo AEC, AGC and NR
	if (SoliCallComboAECNRProcessFrame(CHANNEL_ID, (BYTE *)in, FRAME_SIZE, (BYTE *)in, &bytesOut,
			&iVAD, &iConfidentVAD, &iDTMF, &iLastNoiseAmplitude, &iEstimatedVoiceAmplitude, &iCalculatedAGCCoef, &iTmpCurrEchoAmplitude) != SOLICALL_RC_SUCCESS)
	{
		LOGE("Error in process frame. Did you pass the call length limit?\n");
	}
//	LOGD("Voice: %d", iConfidentVAD);

	release_byte_array(env, input, in, 0);

	return iConfidentVAD;
}
