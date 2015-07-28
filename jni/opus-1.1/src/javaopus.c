// #include "javaopus.h"
#include <jni.h>
#include "opus.h"
#include <android/log.h>
#include <stdlib.h>
#include <stdbool.h>

#define  LOG_TAG    "VoIP NDK (Opus)"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

inline jbyte* get_byte_array(JNIEnv* env, jbyteArray pcm) {
	jboolean isCopy;
	return (*env)->GetByteArrayElements(env, pcm, &isCopy);
}

inline void release_byte_array(JNIEnv* env, jbyteArray pcm, jbyte* data, jint mode) {
	(*env)->ReleaseByteArrayElements(env, pcm, data, mode);
}

JNIEXPORT jlong JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1encoder_1create
  (JNIEnv * je, jobject jo, jint samplingRate, jint channels, jint errors) {

	  jlong enc = (intptr_t)opus_encoder_create((opus_int32)samplingRate, (int)channels, (int)OPUS_APPLICATION_VOIP, (int *)&errors);
	  opus_encoder_ctl((OpusEncoder *)(intptr_t)enc, OPUS_SET_APPLICATION(OPUS_APPLICATION_VOIP));
	  opus_encoder_ctl((OpusEncoder *)(intptr_t)enc, OPUS_SET_FORCE_CHANNELS(1));
	  opus_encoder_ctl((OpusEncoder *)(intptr_t)enc, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));
	  opus_encoder_ctl((OpusEncoder *)enc, OPUS_SET_PACKET_LOSS_PERC(10));
	  opus_encoder_ctl((OpusEncoder *)enc, OPUS_SET_INBAND_FEC(1));
	  return enc;

  }

JNIEXPORT void JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1set_1bitrate
  (JNIEnv *je, jobject jo, jlong enc, jint bitrate) {
	  opus_encoder_ctl((OpusEncoder *)(intptr_t)enc, OPUS_SET_BITRATE(bitrate));
	  // LOGD("Encoder bitrate set to: %d ", bitrate);

}

JNIEXPORT void JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1set_1complexity
  (JNIEnv *je, jobject jo, jlong enc, jint complexity) {
	  opus_encoder_ctl((OpusEncoder *)(intptr_t)enc, OPUS_SET_COMPLEXITY(complexity));

}


JNIEXPORT void JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1encoder_1destroy
  (JNIEnv * je, jobject jo, jlong encoder) {

	  opus_encoder_destroy((OpusEncoder *)(intptr_t)encoder);
}

JNIEXPORT jint JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1encode
  (JNIEnv * je, jobject jo, jlong encoder, jbyteArray input, jint frameSize, jbyteArray output, jint maxDataBytes) {

	  opus_int32 retVal;
	  jbyte *in, *out;

	  in = get_byte_array(je, input);
	  out = get_byte_array(je, output);
	  retVal = opus_encode((OpusEncoder *)(intptr_t)encoder, (opus_int16 *)in, (int)frameSize, out, (opus_int32) maxDataBytes);
	  release_byte_array(je, input, in, JNI_ABORT);
	  release_byte_array(je, output, out, 0);

	  return retVal;
}

JNIEXPORT jlong JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1decoder_1create
  (JNIEnv *je, jobject jo, jint samplingRate, jint channels, jint errors) {


	jlong dec = (intptr_t)opus_decoder_create((opus_int32)samplingRate, (int)channels, (int*)&errors);
	return dec;
}

JNIEXPORT void JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1set_1gain
(JNIEnv *je, jobject jo, jlong dec, jint gain) {
	opus_decoder_ctl((OpusDecoder *)(intptr_t)dec, OPUS_SET_GAIN(gain));
	// LOGD("Decoder gain set to: %d ", gain);

}

JNIEXPORT void JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1decoder_1destroy
  (JNIEnv * je, jobject jo, jlong decoder) {

	  opus_decoder_destroy((OpusDecoder *)(intptr_t)decoder);
}

JNIEXPORT jint JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1decode
  (JNIEnv * je, jobject jo, jlong decoder, jbyteArray input, jint inputLength, jbyteArray output, jint frameSize, jint decode_fec) {

	  opus_int32 retVal;
	  jbyte *in, *out;

	  in = get_byte_array(je, input);
	  out = get_byte_array(je, output);
	  retVal = opus_decode((OpusDecoder *)(intptr_t)decoder, in, inputLength,(opus_int16 *)out, frameSize, decode_fec);
	  release_byte_array(je, input, in, JNI_ABORT);
	  release_byte_array(je, output, out, 0);

	  return retVal;
}

JNIEXPORT jint JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1plc
  (JNIEnv * je, jobject jo, jlong decoder, jbyteArray output, jint frameSize) {

	  opus_int32 retVal;
	  jbyte *out;

	  out = get_byte_array(je, output);
	  retVal = opus_decode((OpusDecoder *)(intptr_t)decoder, NULL, 0, (opus_int16 *)out, frameSize, 0);
	  release_byte_array(je, output, out, 0);

	  return retVal;
}



jstring
Java_com_bsb_hike_voip_OpusWrapper_stringFromJNI( JNIEnv* env,
                                                  jobject thiz )
{
#if defined(__arm__)
  #if defined(__ARM_ARCH_7A__)
    #if defined(__ARM_NEON__)
      #if defined(__ARM_PCS_VFP)
        #define ABI "armeabi-v7a/NEON (hard-float)"
      #else
        #define ABI "armeabi-v7a/NEON"
      #endif
    #else
      #if defined(__ARM_PCS_VFP)
        #define ABI "armeabi-v7a (hard-float)"
      #else
        #define ABI "armeabi-v7a"
      #endif
    #endif
  #else
   #define ABI "armeabi"
  #endif
#elif defined(__i386__)
   #define ABI "x86"
#elif defined(__x86_64__)
   #define ABI "x86_64"
#elif defined(__mips64)  /* mips64el-* toolchain defines __mips__ too */
   #define ABI "mips64"
#elif defined(__mips__)
   #define ABI "mips"
#elif defined(__aarch64__)
   #define ABI "arm64-v8a"
#else
   #define ABI "unknown"
#endif

    return (*env)->NewStringUTF(env, "Hello from JNI !  Compiled with ABI " ABI ".");
}
