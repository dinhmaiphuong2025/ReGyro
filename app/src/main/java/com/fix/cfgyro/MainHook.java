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

    // Lưu ánh xạ giữa handle của cảm biến và loại cảm biến (Sensor Type)
    private static final SparseIntArray handleToType = new SparseIntArray();

    // Góc bù nghiêng máy: 45 độ (Đổi thành -45.0 nếu bạn xoay ngang máy theo chiều ngược lại)
    private static final double PITCH_OFFSET_DEG = 45.0;
    private static final float COS_A = (float) Math.cos(Math.toRadians(PITCH_OFFSET_DEG));
    private static final float SIN_A = (float) Math.sin(Math.toRadians(PITCH_OFFSET_DEG));
    private static final float SIN_HALF = (float) Math.sin(Math.toRadians(PITCH_OFFSET_DEG / 2.0));
    private static final float COS_HALF = (float) Math.cos(Math.toRadians(PITCH_OFFSET_DEG / 2.0));

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName == null || "android".equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": Initializing in " + lpparam.packageName);

        // 1. Ghi nhận handle của từng loại cảm biến khi SystemSensorManager khởi tạo hoặc khi game đăng ký
        Class<?> smClass = XposedHelpers.findClass("android.hardware.SystemSensorManager", lpparam.classLoader);

        // Hook constructor để lấy mHandleToSensor có sẵn
        XposedBridge.hookAllConstructors(smClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    Map<?, ?> handleToSensor = (Map<?, ?>) XposedHelpers.getObjectField(param.thisObject, "mHandleToSensor");
                    if (handleToSensor != null) {
                        synchronized (handleToType) {
                            for (Map.Entry<?, ?> entry : handleToSensor.entrySet()) {
                                if (entry.getKey() instanceof Integer && entry.getValue() instanceof Sensor) {
                                    Sensor s = (Sensor) entry.getValue();
                                    handleToType.put((Integer) entry.getKey(), s.getType());
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        });

        // Hook registerListenerImpl để chắc chắn bắt được mọi sensor được đăng ký
        XposedBridge.hookAllMethods(smClass, "registerListenerImpl", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args.length >= 2 && param.args[1] instanceof Sensor) {
                    Sensor sensor = (Sensor) param.args[1];
                    int handle = -1;
                    try {
                        handle = XposedHelpers.getIntField(sensor, "mHandle");
                    } catch (Throwable ignored) {
                        try {
                            handle = (int) XposedHelpers.callMethod(sensor, "getHandle");
                        } catch (Throwable ignored2) {
                        }
                    }
                    if (handle != -1) {
                        synchronized (handleToType) {
                            handleToType.put(handle, sensor.getType());
                        }
                    }
                }
            }
        });

        // 2. Xoay trục tọa độ về góc 90 độ ngay khi cảm biến trả dữ liệu
        Class<?> queueClass = XposedHelpers.findClass(
                "android.hardware.SystemSensorManager$SensorEventQueue", lpparam.classLoader);

        XposedBridge.hookAllMethods(queueClass, "dispatchSensorEvent", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                int handle = (int) param.args[0];
                float[] v = (float[]) param.args[1];
                if (v == null || v.length < 3) return;

                int type;
                synchronized (handleToType) {
                    type = handleToType.get(handle, -1);
                }

                // Nếu là Gyroscope, Gia tốc hoặc Trọng lực: Xoay 2D giữa trục X và Z
                if (type == Sensor.TYPE_GYROSCOPE || type == Sensor.TYPE_GYROSCOPE_UNCALIBRATED
                        || type == Sensor.TYPE_ACCELEROMETER || type == Sensor.TYPE_GRAVITY) {
                    float x = v[0];
                    float z = v[2];
                    v[0] = x * COS_A + z * SIN_A;
                    v[2] = -x * SIN_A + z * COS_A;

                    // Triệt tiêu hoàn toàn trục Roll (vô lăng) trên Gyro để đường tâm đi ngang tuyệt đối
                    if (type == Sensor.TYPE_GYROSCOPE || type == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
                        v[2] = 0f;
                    }

                    // Nếu là Gyroscope Uncalibrated và có drift bias
                    if (type == Sensor.TYPE_GYROSCOPE_UNCALIBRATED && v.length >= 6) {
                        float dx = v[3];
                        float dz = v[5];
                        v[3] = dx * COS_A + dz * SIN_A;
                        v[5] = -dx * SIN_A + dz * COS_A;
                    }
                }
                // Nếu game dùng Rotation Vector (Input.gyro.attitude của Unity): Nhân Quaternion bù 45 độ quanh trục Y
                else if ((type == Sensor.TYPE_GAME_ROTATION_VECTOR || type == Sensor.TYPE_ROTATION_VECTOR) && v.length >= 4) {
                    float qx = v[0], qy = v[1], qz = v[2], qw = v[3];
                    v[0] = qx * COS_HALF - qz * SIN_HALF;
                    v[1] = qw * SIN_HALF + qy * COS_HALF;
                    v[2] = qx * SIN_HALF + qz * COS_HALF;
                    v[3] = qw * COS_HALF - qy * SIN_HALF;
                }
            }
        });
    }
}
