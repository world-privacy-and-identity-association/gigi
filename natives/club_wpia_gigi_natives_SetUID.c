#include <jni.h>
#include <sys/types.h>

#include <unistd.h>

#ifndef _Included_club_wpia_natives_SetUID
#define _Included_club_wpia_natives_SetUID
#ifdef __cplusplus
extern "C" {
#endif

static jobject getStatus(JNIEnv *env, int successCode, const char * message) {
    jstring message_str = (*env)->NewStringUTF(env, message);
    jboolean success = successCode;
    jclass cls = (*env)->FindClass(env, "Lclub/wpia/gigi/natives/SetUID$Status;");
    jmethodID constructor = (*env)->GetMethodID(env, cls, "<init>", "(ZLjava/lang/String;)V");
    return (*env)->NewObject(env, cls, constructor, success, message_str);
}

JNIEXPORT jobject JNICALL Java_club_wpia_gigi_natives_SetUID_setUid
        (JNIEnv *env, jobject obj, jint uid_, jint gid_) {

    /* We don't need the reference for the object/class we are working on */
    (void)obj;
    /* Fix uid and gid types */
    uid_t uid = (uid_t)uid_;
    if ((jint)uid != uid_) {
      return getStatus(env, 0, "UID does not fit in uid_t type.");
    }
    gid_t gid = (gid_t)gid_;
    if ((jint)gid != gid_) {
      return getStatus(env, 0, "GID does not fit in gid_t type.");
    }

    unsigned char work = 0;

    if(getgid() != gid || getegid() != gid) {
        if(setgid(gid)) {
            return getStatus(env, 0, "Error while setting GID.");
        }
        work |= 1;
    }

    if(getuid() != uid || geteuid() != uid) {
        if(setuid(uid)) {
            return getStatus(env, 0, "Error while setting UID.");
        }
        work |= 2;
    }

    char *status;
    switch (work) {
    case 0: status = "UID and GID already set."; break;
    case 1: status = "Successfully set GID (UID already set)."; break;
    case 2: status = "Successfully set UID (GID already set)."; break;
    case 3: status = "Successfully set UID and GID."; break;
    default: return getStatus(env, 0, "Unexpected internal state.");
    }
    return getStatus(env, 1, status);
}

#ifdef __cplusplus
}
#endif
#endif
