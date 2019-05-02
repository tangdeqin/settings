LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    src/com/android/providers/settings/EventLogTags.logtags

LOCAL_JAVA_LIBRARIES := telephony-common \
                        ims-common \
                        mediatek-framework \
                        mediatek-common
LOCAL_STATIC_JAVA_LIBRARIES := junit

LOCAL_STATIC_JAVA_LIBRARIES := com.mediatek.providers.settings.ext

LOCAL_PACKAGE_NAME := MtkSettingsProvider
LOCAL_OVERRIDES_PACKAGES := SettingsProvider
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)

########################
include $(call all-makefiles-under,$(LOCAL_PATH))