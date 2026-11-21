#include "AudioPipeline.h"

#include <android/log.h>
#include <jni.h>
#include <memory>
#include <string>

#define LOG_TAG "BitPerfectJni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace bitperfect;

namespace {

JavaVM* gJavaVm = nullptr;

class JniEnvScope {
public:
    JniEnvScope() {
        if (!gJavaVm) return;
        const jint res = gJavaVm->GetEnv(reinterpret_cast<void**>(&env_), JNI_VERSION_1_6);
        if (res == JNI_EDETACHED) {
            if (gJavaVm->AttachCurrentThread(&env_, nullptr) == JNI_OK) {
                attached_ = true;
            } else {
                env_ = nullptr;
            }
        } else if (res != JNI_OK) {
            env_ = nullptr;
        }
    }

    ~JniEnvScope() {
        if (attached_ && gJavaVm) {
            gJavaVm->DetachCurrentThread();
        }
    }

    JNIEnv* get() const noexcept { return env_; }

private:
    JNIEnv* env_{nullptr};
    bool attached_{false};
};

struct NativeEngineHolder {
    std::unique_ptr<AudioPipeline> pipeline;
    jobject kotlinEngineRef = nullptr;
    jmethodID onPositionMethod = nullptr;
    jmethodID onFormatMethod = nullptr;
    jmethodID onDacConnectedMethod = nullptr;
    jmethodID onStatusMethod = nullptr;
    jmethodID onErrorMethod = nullptr;
    jmethodID onCompletedMethod = nullptr;
};

} // namespace

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    gJavaVm = vm;
    return JNI_VERSION_1_6;
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_bitperfect_usb_BitPerfectEngine_nativeCreate(JNIEnv* env, jobject thiz) {
    auto* holder = new NativeEngineHolder();
    holder->pipeline = std::make_unique<AudioPipeline>();
    holder->kotlinEngineRef = env->NewGlobalRef(thiz);

    jclass cls = env->GetObjectClass(thiz);
    holder->onPositionMethod = env->GetMethodID(cls, "onNativePositionChanged", "(JJ)V");
    holder->onFormatMethod = env->GetMethodID(cls, "onNativeFormatChanged", "(IIIILjava/lang/String;)V");
    holder->onDacConnectedMethod = env->GetMethodID(cls, "onNativeDacConnected", "(Ljava/lang/String;I)V");
    holder->onStatusMethod = env->GetMethodID(cls, "onNativeStatusMessage", "(Ljava/lang/String;)V");
    holder->onErrorMethod = env->GetMethodID(cls, "onNativeError", "(Ljava/lang/String;)V");
    holder->onCompletedMethod = env->GetMethodID(cls, "onNativePlaybackCompleted", "()V");
    env->DeleteLocalRef(cls);

    PipelineCallbacks cbs{};

    cbs.onPositionChanged = [holder](int64_t posMs, int64_t durMs) {
        JniEnvScope scope;
        if (JNIEnv* e = scope.get(); e && holder->onPositionMethod) {
            e->CallVoidMethod(holder->kotlinEngineRef, holder->onPositionMethod,
                              static_cast<jlong>(posMs), static_cast<jlong>(durMs));
        }
    };

    cbs.onFormatChanged = [holder](uint32_t sr, uint32_t srcBits, uint32_t wireBits,
                                   uint32_t ch, const std::string& codec) {
        JniEnvScope scope;
        if (JNIEnv* e = scope.get(); e && holder->onFormatMethod) {
            jstring jCodec = e->NewStringUTF(codec.c_str());
            e->CallVoidMethod(holder->kotlinEngineRef, holder->onFormatMethod,
                              static_cast<jint>(sr), static_cast<jint>(srcBits),
                              static_cast<jint>(wireBits), static_cast<jint>(ch), jCodec);
            e->DeleteLocalRef(jCodec);
        }
    };

    cbs.onDacConnected = [holder](const std::string& dacName, uint16_t uacVersion) {
        JniEnvScope scope;
        if (JNIEnv* e = scope.get(); e && holder->onDacConnectedMethod) {
            jstring jName = e->NewStringUTF(dacName.c_str());
            e->CallVoidMethod(holder->kotlinEngineRef, holder->onDacConnectedMethod,
                              jName, static_cast<jint>(uacVersion));
            e->DeleteLocalRef(jName);
        }
    };

    cbs.onStatusMessage = [holder](const std::string& msg) {
        JniEnvScope scope;
        if (JNIEnv* e = scope.get(); e && holder->onStatusMethod) {
            jstring jMsg = e->NewStringUTF(msg.c_str());
            e->CallVoidMethod(holder->kotlinEngineRef, holder->onStatusMethod, jMsg);
            e->DeleteLocalRef(jMsg);
        }
    };

    cbs.onError = [holder](const std::string& err) {
        JniEnvScope scope;
        if (JNIEnv* e = scope.get(); e && holder->onErrorMethod) {
            jstring jErr = e->NewStringUTF(err.c_str());
            e->CallVoidMethod(holder->kotlinEngineRef, holder->onErrorMethod, jErr);
            e->DeleteLocalRef(jErr);
        }
    };

    cbs.onPlaybackCompleted = [holder]() {
        JniEnvScope scope;
        if (JNIEnv* e = scope.get(); e && holder->onCompletedMethod) {
            e->CallVoidMethod(holder->kotlinEngineRef, holder->onCompletedMethod);
        }
    };

    holder->pipeline->setCallbacks(std::move(cbs));
    return reinterpret_cast<jlong>(holder);
}

JNIEXPORT void JNICALL
Java_com_bitperfect_usb_BitPerfectEngine_nativeDestroy(JNIEnv* env, jobject, jlong handle) {
    auto* holder = reinterpret_cast<NativeEngineHolder*>(handle);
    if (!holder) return;

    holder->pipeline->releaseDac();
    holder->pipeline.reset();

    if (holder->kotlinEngineRef) {
        env->DeleteGlobalRef(holder->kotlinEngineRef);
        holder->kotlinEngineRef = nullptr;
    }
    delete holder;
}

JNIEXPORT jboolean JNICALL
Java_com_bitperfect_usb_BitPerfectEngine_nativeInitializeDac(JNIEnv*, jobject, jlong handle, jint usbFd) {
    auto* holder = reinterpret_cast<NativeEngineHolder*>(handle);
    if (!holder) return JNI_FALSE;
    return holder->pipeline->initializeDac(static_cast<int>(usbFd)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_bitperfect_usb_BitPerfectEngine_nativeReleaseDac(JNIEnv*, jobject, jlong handle) {
    auto* holder = reinterpret_cast<NativeEngineHolder*>(handle);
    if (holder) holder->pipeline->releaseDac();
}

JNIEXPORT jboolean JNICALL
Java_com_bitperfect_usb_BitPerfectEngine_nativeLoadFile(JNIEnv* env, jobject, jlong handle, jstring path) {
    auto* holder = reinterpret_cast<NativeEngineHolder*>(handle);
    if (!holder || !path) return JNI_FALSE;

    const char* cPath = env->GetStringUTFChars(path, nullptr);
    std::string cppPath(cPath ? cPath : "");
    env->ReleaseStringUTFChars(path, cPath);

    return holder->pipeline->loadFile(cppPath) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_bitperfect_usb_BitPerfectEngine_nativePlay(JNIEnv*, jobject, jlong handle) {
    auto* holder = reinterpret_cast<NativeEngineHolder*>(handle);
    return (holder && holder->pipeline->play()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_bitperfect_usb_BitPerfectEngine_nativePause(JNIEnv*, jobject, jlong handle) {
    auto* holder = reinterpret_cast<NativeEngineHolder*>(handle);
    if (holder) holder->pipeline->pause();
}

JNIEXPORT void JNICALL
Java_com_bitperfect_usb_BitPerfectEngine_nativeStop(JNIEnv*, jobject, jlong handle) {
    auto* holder = reinterpret_cast<NativeEngineHolder*>(handle);
    if (holder) holder->pipeline->stop();
}

JNIEXPORT jboolean JNICALL
Java_com_bitperfect_usb_BitPerfectEngine_nativeSeekTo(JNIEnv*, jobject, jlong handle, jlong positionMs) {
    auto* holder = reinterpret_cast<NativeEngineHolder*>(handle);
    return (holder && holder->pipeline->seekToMs(static_cast<int64_t>(positionMs))) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_bitperfect_usb_BitPerfectEngine_nativeSetHardwareVolume(JNIEnv*, jobject, jlong handle, jfloat percent) {
    auto* holder = reinterpret_cast<NativeEngineHolder*>(handle);
    return (holder && holder->pipeline->setHardwareVolume(static_cast<float>(percent))) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
