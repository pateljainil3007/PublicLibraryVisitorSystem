#include <jni.h>
#include <string.h>
#include <stdio.h>

JNIEXPORT jdouble JNICALL
Java_com_library_visitorsystem_NativeBridge_nativeCalculateOverdueFine(
        JNIEnv *env,
        jobject thiz,
        jlong daysOverdue,
        jdouble dailyRate) {
    if (daysOverdue <= 0) {
        return 0.0;
    }
    return (jdouble) (daysOverdue * dailyRate);
}

JNIEXPORT jstring JNICALL
Java_com_library_visitorsystem_NativeBridge_nativeGetLibraryEngineStatus(
        JNIEnv *env,
        jobject thiz) {
    char message[128];
    snprintf(message, sizeof(message), "C Native Engine (NDK) - Operational");
    return (*env)->NewStringUTF(env, message);
}