ROOT := $(call my-dir)
LOCAL_PATH := $(call my-dir)

###########################################################  Build opus
# https://code.google.com/p/csipsimple/source/browse/trunk/CSipSimple/jni/opus/android_toolchain/Android.mk
include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog

# MY_MODULE_DIR       := opus
USE_FIXED_POINT := 0
ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI),armeabi armeabi-v7a))
USE_FIXED_POINT := 1
endif

LOCAL_PATH			:= $(ROOT)/opus-1.1
OPUS_PATH := $(LOCAL_PATH)
LOCAL_MODULE		:= opuscodec
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include $(OPUS_PATH)/celt $(OPUS_PATH)/silk

# we need to rebuild silk cause we don't know what are diff required for opus and may change in the future
include $(OPUS_PATH)/silk_sources.mk 
LOCAL_SRC_FILES += $(SILK_SOURCES)

ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI),armeabi armeabi-v7a))
LOCAL_C_INCLUDES += $(OPUS_PATH)/silk/fixed
LOCAL_SRC_FILES += $(SILK_SOURCES_FIXED)
LOCAL_SRC_FILES += $(OPUS_SOURCES_FIXED)
else
LOCAL_C_INCLUDES += $(OPUS_PATH)/silk/float
LOCAL_SRC_FILES += $(SILK_SOURCES_FLOAT)
LOCAL_SRC_FILES += $(OPUS_SOURCES_FLOAT)
endif


include $(OPUS_PATH)/celt_sources.mk
LOCAL_SRC_FILES += $(CELT_SOURCES)
include $(OPUS_PATH)/opus_sources.mk
LOCAL_SRC_FILES += $(OPUS_SOURCES)

LOCAL_SRC_FILES += src/javaopus.c
 

LOCAL_CFLAGS		:= -I$(LOCAL_PATH)/include -I$(LOCAL_PATH)/celt -I$(LOCAL_PATH)/silk -I$(LOCAL_PATH)/silk/fixed -D__EMX__ -DOPUS_BUILD -DUSE_ALLOCA -DHAVE_LRINT -DHAVE_LRINTF -O3 -fno-math-errno
# Hack to mute restrict not supported by ndk 
LOCAL_CFLAGS += -Drestrict=__restrict
LOCAL_CFLAGS += -Drestrict=__restrict
ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI),armeabi armeabi-v7a))
LOCAL_CFLAGS += -DFIXED_POINT=1 -DDISABLE_FLOAT_API
endif


include $(BUILD_SHARED_LIBRARY)

###########################################################  
# Declare solicall library

LOCAL_PATH := $(ROOT)/solicall

include $(CLEAR_VARS)

LOCAL_MODULE    := solicallsdk

ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI),armeabi armeabi-v7a))
LOCAL_SRC_FILES := libSoliCallSDK.a
else
LOCAL_SRC_FILES := libSoliCallSDK-x86.a
endif

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include

include $(PREBUILT_STATIC_LIBRARY)

###########################################################  
# Declare solicall library #2

LOCAL_PATH := $(ROOT)/solicall

include $(CLEAR_VARS)

LOCAL_LDLIBS := -llog -ldl
LOCAL_MODULE    := solicall
LOCAL_SRC_FILES := solicalljni.cpp
LOCAL_STATIC_LIBRARIES := solicallsdk

LOCAL_CFLAGS += -D_LINUX

include $(BUILD_SHARED_LIBRARY)







