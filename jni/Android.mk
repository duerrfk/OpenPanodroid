 LOCAL_PATH := $(call my-dir)
 
 include $(CLEAR_VARS)
 
 LOCAL_MODULE := cubicpano-jni
 LOCAL_SRC_FILES := cubicpano-jni.cpp
 LOCAL_LDLIBS    := -lm -llog -ljnigraphics
 
 include $(BUILD_SHARED_LIBRARY)
 
 