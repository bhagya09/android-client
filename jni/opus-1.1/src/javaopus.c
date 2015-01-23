// #include "javaopus.h"
#include <jni.h>
#include "opus.h"
#include <android/log.h>
#include <stdlib.h>
#include <stdbool.h>

#define  LOG_TAG    "NDK Message"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define INPUT_BUFFER_SIZE	1000000
jbyte* inputBuffer = NULL;
jbyte *writeMarker, *readMarker, *finalMarker, *loopMemory;
static jint availableData = 0;
static int inputBufferSize;

void allocateInputBuffer() {
	inputBufferSize = INPUT_BUFFER_SIZE;
	inputBuffer = NULL;
	writeMarker = NULL;
	readMarker = NULL;
	finalMarker = NULL;
	loopMemory = NULL;

	while (inputBuffer == NULL && inputBufferSize > 10000) {
		inputBuffer = malloc(inputBufferSize);
		if (inputBuffer == NULL) {
			inputBufferSize -= 5000;
			LOGE("Memory allocation failed. Retrying for: %d", inputBufferSize);
		}
	}

	if (inputBuffer == NULL)
		LOGE("Memory allocation failed. Giving up!");
	else
		writeMarker = inputBuffer;
}

void shutdown() {
	free(inputBuffer);
	inputBuffer = NULL;
	LOGD("Native memory released.");
	writeMarker = NULL;
	readMarker = NULL;
	finalMarker = NULL;
	loopMemory = NULL;

}

void appendToBuffer(jbyte* data, jint size) {

	// LOGD("appendtobuffer called with size: %d ", size);
	if (readMarker == NULL)
		readMarker = inputBuffer;

	if (writeMarker + size > inputBuffer + inputBufferSize) {
		// Data will not fit
		// LOGD("Rotating buffer..");
		jint bufferLeft = inputBuffer + inputBufferSize - writeMarker;
		memcpy(writeMarker, data, bufferLeft);
		memcpy(inputBuffer, data + bufferLeft, size - bufferLeft);
		writeMarker = inputBuffer + (size - bufferLeft);
		finalMarker = inputBuffer + inputBufferSize;
	} else {
		memcpy(writeMarker, data, size);
		writeMarker = writeMarker + size;
	}

	if (writeMarker > finalMarker || finalMarker == NULL)
		finalMarker = writeMarker;

	// LOGD("WRITING, readMarker: %d, writeMarker: %d, finalMarker: %d ", readMarker - inputBuffer, writeMarker - inputBuffer, finalMarker - inputBuffer);

}

jbyte* readFromBuffer(jint size) {

	// LOGD("readFromBuffer called with size: %d", size);
	// LOGD("writeMarker: %d, readMarker: %d, finalMarker: %d", writeMarker, readMarker, finalMarker);
	jbyte* temp = NULL;

	if (writeMarker < readMarker && finalMarker < readMarker) {
		LOGE("The impossible has occurred.");
		return NULL;
	}

	if (writeMarker > readMarker)
		availableData = writeMarker - readMarker;

	if (writeMarker < readMarker) {
		availableData = finalMarker - readMarker;
		availableData += writeMarker - inputBuffer;
	}

	if (writeMarker == readMarker)
		availableData = 0;

	if (availableData < size) {
		// LOGD("Not enough data to encode: %d", availableData);
		return NULL;
	}

	if (writeMarker < readMarker && finalMarker - readMarker < size) {

		// We gotta loop
		// LOGD("inputBuffer: %d, writeMarker: %d, readMarker: %d, finalMarker: %d, availableData: %d", inputBuffer, writeMarker, readMarker, finalMarker, availableData);
		if (loopMemory != NULL) {
			LOGE("Cannot assign loopMemory since it is not null");
			return NULL;
		}

		// LOGD("writeMarker: %d, readMarker: %d, finalMarker: %d", writeMarker, readMarker, finalMarker);
		loopMemory = malloc(size);
		availableData = finalMarker - readMarker;
		memcpy(loopMemory, readMarker, availableData);
		memcpy(loopMemory + availableData, inputBuffer, size - availableData);
		readMarker = inputBuffer + size - availableData;
		// LOGD("Returning loop memory");
		temp = loopMemory;
		// LOGD("READING, readMarker: %d, writeMarker: %d, finalMarker: %d, temp: LOOP ", readMarker - inputBuffer, writeMarker - inputBuffer, finalMarker - inputBuffer);
	} else {
		temp = readMarker;
		readMarker += size;
		// LOGD("READING, readMarker: %d, writeMarker: %d, finalMarker: %d, temp: %d ", readMarker - inputBuffer, writeMarker - inputBuffer, finalMarker - inputBuffer, temp - inputBuffer);
	}


	// LOGD("!writeMarker: %d, readMarker: %d, finalMarker: %d", writeMarker, readMarker, finalMarker);
	// LOGD("Returning read from: %d ", temp);
	return temp;

}

inline void freeLoopMemory() {
	if (loopMemory != NULL) {
		// LOGD("Clearing loop memory");
		free(loopMemory);
		loopMemory = NULL;
	}
}

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
	  // opus_encoder_ctl((OpusEncoder *)enc, OPUS_SET_BITRATE(32000));
	  opus_encoder_ctl((OpusEncoder *)(intptr_t)enc, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));
	  // opus_encoder_ctl((OpusEncoder *)(intptr_t)enc, OPUS_SET_COMPLEXITY(1));
	  // opus_encoder_ctl((OpusEncoder *)enc, OPUS_SET_PACKET_LOSS_PERC(5));
	  // opus_encoder_ctl((OpusEncoder *)enc, OPUS_SET_INBAND_FEC(1));
	  allocateInputBuffer();
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
	  shutdown();
}

JNIEXPORT jint JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1queue
  (JNIEnv * je, jobject jo, jbyteArray stream) {

	// This function only adds the data to be encoded in the encoder buffer
	// Actually encoding is done by `opus_get_encoded_data`

	jbyte *in;

	in = get_byte_array(je, stream);
	appendToBuffer(in, (*je)->GetArrayLength(je, stream));
	release_byte_array(je, stream, in, JNI_ABORT);

	return 0;
}

JNIEXPORT jint JNICALL Java_com_bsb_hike_voip_OpusWrapper_opus_1get_1encoded_1data
  (JNIEnv * je, jobject jo, jlong encoder, jint frameSize, jbyteArray output, jint maxDataBytes) {

	opus_int32 retVal;
	jbyte *out, *copy, *temp;
	jint totalSize = 0;

	out = get_byte_array(je, output);
	copy = out;

	temp = readFromBuffer(frameSize * 2);
	if (temp != NULL) {
		retVal = opus_encode((OpusEncoder *)(intptr_t)encoder, (opus_int16 *)temp, (int)frameSize, out, (opus_int32) maxDataBytes);
		totalSize += retVal;
	}

	freeLoopMemory();
	release_byte_array(je, output, copy, 0);
	return totalSize;
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
