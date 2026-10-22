#include <jni.h>
#include <android/log.h>
#include <android/sensor.h>
#include <dlfcn.h>
#include <cstring>
#include "zygisk.hpp"
#include "And64InlineHook.hpp"

#define LOG_TAG "ReGyro"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static ssize_t (*orig_getEvents)(ASensorEventQueue *queue, ASensorEvent *events,
                                 size_t count) = nullptr;

/* 45-degree pitch compensation (rotate around the Y axis) */
static const float COS_A = 0.70710678f;    /* cos(45 deg)  */
static const float SIN_A = 0.70710678f;    /* sin(45 deg)  */
static const float COS_HALF = 0.92387953f; /* cos(22.5 deg) */
static const float SIN_HALF = 0.38268343f; /* sin(22.5 deg) */

static inline bool isGyroType(int32_t type) {
    return type == ASENSOR_TYPE_GYROSCOPE || type == ASENSOR_TYPE_GYROSCOPE_UNCALIBRATED;
}

static void transformEvent(ASensorEvent *e) {
    switch (e->type) {
        case ASENSOR_TYPE_GYROSCOPE:
        case ASENSOR_TYPE_GYROSCOPE_UNCALIBRATED:
        case ASENSOR_TYPE_ACCELEROMETER:
        case ASENSOR_TYPE_GRAVITY:
        case ASENSOR_TYPE_LINEAR_ACCELERATION: {
            /* 2D rotation between X and Z (pitch compensation around Y axis) */
            float x = e->data[0];
            float z = e->data[2];
            e->data[0] = x * COS_A + z * SIN_A;
            e->data[2] = -x * SIN_A + z * COS_A;

            if (isGyroType(e->type)) {
                /* Kill roll axis completely so the crosshair stays level */
                e->data[2] = 0.0f;
                if (e->type == ASENSOR_TYPE_GYROSCOPE_UNCALIBRATED) {
                    float dx = e->data[3];
                    float dz = e->data[5];
                    e->data[3] = dx * COS_A + dz * SIN_A;
                    e->data[5] = -dx * SIN_A + dz * COS_A;
                }
            }
            break;
        }
        case ASENSOR_TYPE_GAME_ROTATION_VECTOR:
        case ASENSOR_TYPE_ROTATION_VECTOR: {
            /* Multiply quaternion by rotation of +45 deg around Y */
            float qx = e->data[0];
            float qy = e->data[1];
            float qz = e->data[2];
            float qw = e->data[3];
            e->data[0] = qx * COS_HALF - qz * SIN_HALF;
            e->data[1] = qw * SIN_HALF + qy * COS_HALF;
            e->data[2] = qx * SIN_HALF + qz * COS_HALF;
            e->data[3] = qw * COS_HALF - qy * SIN_HALF;
            break;
        }
        default:
            break;
    }
}

static ssize_t fake_getEvents(ASensorEventQueue *queue, ASensorEvent *events, size_t count) {
    ssize_t n = orig_getEvents(queue, events, count);
    if (n > 0) {
        for (ssize_t i = 0; i < n; i++) {
            transformEvent(&events[i]);
        }
    }
    return n;
}

static void install_hook() {
    if (orig_getEvents != nullptr) {
        LOGI("install_hook: hook already installed");
        return;
    }

    void *handle = dlopen("libandroid.so", RTLD_NOW | RTLD_LOCAL);
    if (handle == nullptr) {
        handle = dlopen("libsensor.so", RTLD_NOW | RTLD_LOCAL);
    }
    if (handle == nullptr) {
        LOGI("install_hook: cannot dlopen libandroid.so / libsensor.so");
        return;
    }

    void *sym = dlsym(handle, "ASensorEventQueue_getEvents");
    if (sym == nullptr) {
        LOGI("install_hook: ASensorEventQueue_getEvents not found");
        return;
    }

    A64HookFunction(sym, (void *)fake_getEvents, (void **)&orig_getEvents);
    LOGI("install_hook: A64HookFunction ASensorEventQueue_getEvents -> %s",
         orig_getEvents ? "success" : "failed");
}

class ReGyroModule : public zygisk::ModuleBase {
public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override {
        this->api = api;
        this->env = env;
    }

    void preAppSpecialize(zygisk::AppSpecializeArgs *args) override {
        target = isTargetProcess(args);

        if (!target) {
            // Not target process, tell Zygisk to dlclose module library immediately
            api->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);
        }
    }

    void postAppSpecialize(const zygisk::AppSpecializeArgs *) override {
        if (!target) return;
        LOGI("ReGyro active in target process (Zygisk)");
        install_hook();
    }

private:
    zygisk::Api *api = nullptr;
    JNIEnv *env = nullptr;
    bool target = false;

    bool isTargetProcess(zygisk::AppSpecializeArgs *args) {
        if (!args) return false;
        if (args->nice_name) {
            const char *process = env->GetStringUTFChars(args->nice_name, nullptr);
            if (process != nullptr) {
                bool match = (strstr(process, "com.ScarletWork.CounterFlame") != nullptr);
                if (match) {
                    LOGI("preAppSpecialize: target matched nice_name=[%s]", process);
                }
                env->ReleaseStringUTFChars(args->nice_name, process);
                if (match) return true;
            }
        }
        if (args->app_data_dir) {
            const char *dir = env->GetStringUTFChars(args->app_data_dir, nullptr);
            if (dir != nullptr) {
                bool match = (strstr(dir, "com.ScarletWork.CounterFlame") != nullptr);
                if (match) {
                    LOGI("preAppSpecialize: target matched app_data_dir=[%s]", dir);
                }
                env->ReleaseStringUTFChars(args->app_data_dir, dir);
                if (match) return true;
            }
        }
        return false;
    }
};

REGISTER_ZYGISK_MODULE(ReGyroModule)
