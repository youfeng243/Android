LOCAL_PATH := $(call my-dir)
         
include $(CLEAR_VARS)
         
LOCAL_MODULE    := mouse
LOCAL_LDLIBS    := -llog 
LOCAL_SRC_FILES := com_example_server_Mouse.cpp
include $(BUILD_SHARED_LIBRARY)