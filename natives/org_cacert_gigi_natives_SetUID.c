#include <jni.h>
#include <sys/types.h>
#include <unistd.h>

#ifndef _Included_org_cacert_natives_SetUID
#define _Included_org_cacert_natives_SetUID
#ifdef __cplusplus
extern "C" {
#endif

static jobject getStatus(JNIEnv *env, int successCode, const char * message) {
    jstring message_str = (*env)->NewStringUTF(env, message);
    jboolean success = successCode;
    jclass cls = (*env)->FindClass(env, "Lorg/cacert/gigi/natives/SetUID$Status;");
    jmethodID constructor = (*env)->GetMethodID(env, cls, "<init>", "(ZLjava/lang/String;)V");
    return (*env)->NewObject(env, cls, constructor, success, message_str);
}

JNIEXPORT jobject JNICALL Java_org_cacert_gigi_natives_SetUID_setUid
        (JNIEnv *env, jobject obj, jint uid, jint gid) {

    /* We don't need the reference for the object/class we are working on */
    (void)obj;

    if(setgid((int)gid)) {
        return (jobject)getStatus(env, 0, "Error while setting GID.");
    }

    if(setuid((int)uid)) {
        return (jobject)getStatus(env, 0, "Error while setting UID.");
    }

    return (jobject)getStatus(env, 1, "Successfully set uid/gid.");
}

#ifdef __cplusplus
}
#endif
#endif
