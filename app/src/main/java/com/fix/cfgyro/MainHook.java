package com.fix.cfgyro;

import android.hardware.Sensor;
import android.util.SparseIntArray;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Map;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "ReGyro";
    private static final String TARGET_PACKAGE = "com.ScarletWork.CounterFlame";

    // Ánh xạ handle -> loại cảm biến (dùng cho đường hook Java dự phòng)
    private static final SparseIntArray handleToType = new SparseIntArray();
    private static volatile Object sSystemSensorManager;

    // Góc bù nghiêng máy: 45 độ (đổi dấu nếu cầm ngược chiều)
    private static final double PITCH_OFFSET_DEG = 45.0;
    private static final float COS_A = (float) Math.cos(Math.toRadians(PITCH_OFFSET_DEG));
    private static final float SIN_A = (float) Math.sin(Math.toRadians(PITCH_OFFSET_DEG));
    private static final float SIN_HALF = (float) Math.sin(Math.toRadians(PITCH_OFFSET_DEG / 2.0));
    private static final float COS_HALF = (float) Math.cos(Math.toRadians(PITCH_OFFSET_DEG / 2.0));

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": loaded into " + lpparam.packageName);

        // 1. Native hook: Unity đọc cảm biến qua NDK (ASensorEventQueue_getEvents)
        try {
            System.loadLibrary("regyro");
            XposedBridge.log(TAG + ": native lib loaded");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": native lib load FAILED: " + t);
        }

        // 2. Java hook dự phòng cho đường SensorManager thông thường
        try {
            Class<?> smClass = XposedHelpers.findClass(
                    "android.hardware.SystemSensorManager", lpparam.classLoader);

            XposedBridge.hookAllConstructors(smClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    sSystemSensorManager = param.thisObject;
                    collectHandles(param.thisObject);
                }
            });

            XposedBridge.hookAllMethods(smClass, "registerListenerImpl", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length >= 2 && param.args[1] instanceof Sensor) {
                        Sensor sensor = (Sensor) param.args[1];
                        int handle = sensorHandle(sensor);
                        if (handle >= 0) {
                            synchronized (handleToType) {
                                handleToType.put(handle, sensor.getType());
                            }
                        }
                        XposedBridge.log(TAG + ": registerListener type=" + sensor.getType()
                                + " handle=" + handle);
                    }
                }
            });

            Class<?> queueClass = XposedHelpers.findClass(
                    "android.hardware.SystemSensorManager$SensorEventQueue", lpparam.classLoader);

            XposedBridge.hookAllMethods(queueClass, "dispatchSensorEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length < 2) return;
                    int handle;
                    try {
                        handle = ((Number) param.args[0]).intValue();
                    } catch (Throwable t) {
                        return;
                    }
                    float[] v = (float[]) param.args[1];
                    if (v == null || v.length < 3) return;

                    int type = resolveType(handle);

                    if (type == Sensor.TYPE_GYROSCOPE || type == Sensor.TYPE_GYROSCOPE_UNCALIBRATED
                            || type == Sensor.TYPE_ACCELEROMETER || type == Sensor.TYPE_GRAVITY
                            || type == Sensor.TYPE_LINEAR_ACCELERATION) {
                        float x = v[0];
                        float z = v[2];
                        v[0] = x * COS_A + z * SIN_A;
                        v[2] = -x * SIN_A + z * COS_A;

                        if (type == Sensor.TYPE_GYROSCOPE || type == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
                            v[2] = 0f;
                            if (type == Sensor.TYPE_GYROSCOPE_UNCALIBRATED && v.length >= 6) {
                                float dx = v[3];
                                float dz = v[5];
                                v[3] = dx * COS_A + dz * SIN_A;
                                v[5] = -dx * SIN_A + dz * COS_A;
                            }
                        }
                    } else if ((type == Sensor.TYPE_GAME_ROTATION_VECTOR
                            || type == Sensor.TYPE_ROTATION_VECTOR) && v.length >= 4) {
                        float qx = v[0], qy = v[1], qz = v[2], qw = v[3];
                        v[0] = qx * COS_HALF - qz * SIN_HALF;
                        v[1] = qw * SIN_HALF + qy * COS_HALF;
                        v[2] = qx * SIN_HALF + qz * COS_HALF;
                        v[3] = qw * COS_HALF - qy * SIN_HALF;
                    }
                }
            });

            XposedBridge.log(TAG + ": Java sensor hooks installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Java hook setup FAILED: " + t);
        }
    }

    private static int sensorHandle(Sensor sensor) {
        try {
            return XposedHelpers.getIntField(sensor, "mHandle");
        } catch (Throwable ignored) {
        }
        try {
            return (int) XposedHelpers.callMethod(sensor, "getHandle");
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static void collectHandles(Object sm) {
        if (sm == null) return;
        try {
            Map<?, ?> m = (Map<?, ?>) XposedHelpers.getObjectField(sm, "mHandleToSensor");
            if (m != null) {
                synchronized (handleToType) {
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        if (e.getKey() instanceof Integer && e.getValue() instanceof Sensor) {
                            handleToType.put((Integer) e.getKey(),
                                    ((Sensor) e.getValue()).getType());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static int resolveType(int handle) {
        synchronized (handleToType) {
            int t = handleToType.get(handle, -1);
            if (t != -1) return t;
        }
        collectHandles(sSystemSensorManager);
        synchronized (handleToType) {
            return handleToType.get(handle, -1);
        }
    }
}
