# ReGyro (Zygisk Edition)

Zygisk module that fixes the gyroscope aiming bug in Counter Flame (`com.ScarletWork.CounterFlame`) for Magisk, KernelSU, and APatch without Xposed/LSPosed.

**[English](#english) | [中文](#中文) | [Русский](#русский) | [Tiếng Việt](#tiếng-việt)**

---

<a id="english"></a>
## English

### Root cause

Counter Flame is a Unity game whose code assumes the reference plane of the device is at a 90° upright angle to the ground. When you hold the phone at a natural ~45° tilt around the screen's horizontal axis (the sensor's Y axis):

- All data from `TYPE_GYROSCOPE`, `TYPE_ACCELEROMETER`, `TYPE_GRAVITY` and rotation vectors (`TYPE_ROTATION_VECTOR`, `TYPE_GAME_ROTATION_VECTOR`) is offset by 45° between the X and Z axes.
- The game's Euler-angle calculation sags into a "V" shape when aiming horizontally.

### How ReGyro Zygisk works

Unlike the legacy LSPosed approach which loaded Java reflection hooks into the game, ReGyro Zygisk runs entirely at the native C++ level:

1. **Zygote specialization gating**: When `com.ScarletWork.CounterFlame` is specialized, the module stays active; for any other process, it immediately unloads itself (`DLCLOSE_MODULE_LIBRARY`).
2. **Native hook**: Uses Dobby to hook `ASensorEventQueue_getEvents` in `libandroid.so` (the exact path Unity NDK uses to pull sensor events). As data arrives, coordinates are transformed in place:
   - Gyroscope, Gyroscope Uncalibrated, Accelerometer, Gravity, Linear Acceleration: 2D rotation matrix between the X and Z axes compensating 45° around the Y axis.
   - Preserves natural sensor micro-noise on all axes (preventing anti-cheat telemetry bans from flat-zero anomaly checks).
   - Rotation Vector / Game Rotation Vector: multiplies the quaternion by a 45° compensation around the Y axis.
3. **Zero Xposed footprint**: Completely invisible to anti-cheat checks looking for `XposedBridge`, `de.robv`, or LSPosed memory maps.

### Installation

1. Download the ZIP from the **Actions** tab on GitHub (artifact `ReGyro-Zygisk`).
2. Flash the ZIP file in **KernelSU**, **APatch**, or **Magisk** Manager.
3. Reboot your device.
4. Launch Counter Flame.

### Verification

Check logcat for the `ReGyro` tag:
```bash
adb logcat -s ReGyro
```
Expected output:
- `ReGyro loaded into com.ScarletWork.CounterFlame (Zygisk)`
- `install_hook: DobbyHook ASensorEventQueue_getEvents -> 0`

---

<a id="中文"></a>
## 中文

### 问题根源

Counter Flame 是一款 Unity 游戏，其代码假定设备的参考平面与地面呈 90° 竖直夹角。当你以自然的约 45° 倾角握持手机（绕屏幕水平轴，即传感器的 Y 轴）时：

- `TYPE_GYROSCOPE`、`TYPE_ACCELEROMETER`、`TYPE_GRAVITY` 以及旋转矢量的所有数据在 X 轴与 Z 轴之间产生 45° 偏移。
- 游戏中的欧拉角计算在水平瞄准时呈现"V"形下垂。

### 工作原理

与依赖 Java Hook 的传统 LSPosed 模块不同，ReGyro Zygisk 完全在原生 C++ 层运行：

1. **进程隔离**：仅在目标进程 `com.ScarletWork.CounterFlame` 中驻留，在其他进程中自动调用 `DLCLOSE_MODULE_LIBRARY` 卸载。
2. **Native Hook**：利用 Dobby 挂钩 `libandroid.so` 中的 `ASensorEventQueue_getEvents`（Unity NDK 读取传感器事件的实际路径）：
   - 陀螺仪、未校准陀螺仪、加速度计、重力、线性加速度：绕 Y 轴进行 45° 坐标变换矩阵旋转。
   - 保留各轴的自然传感器微小噪声（避免被反作弊 Telemetry 判定为异常静态数据而封号）。
   - 旋转矢量：将四元数乘以绕 Y 轴补偿 45° 的旋转四元数。
3. **零 Xposed 痕迹**：彻底免除 `XposedBridge`、`de.robv` 或 LSPosed 内存映射被反作弊检测的风险。

### 安装方法

1. 从 GitHub 的 **Actions** 页面下载 ZIP（工件 `ReGyro-Zygisk`）。
2. 在 **KernelSU**、**APatch** 或 **Magisk** 管理器中刷入 ZIP 模块。
3. 重启设备。
4. 启动 Counter Flame 即可。

### 验证生效

通过 logcat 查看日志：
```bash
adb logcat -s ReGyro
```

---

<a id="русский"></a>
## Русский

### Причина проблемы

Counter Flame — игра на Unity, код которой предполагает, что опорная плоскость устройства находится под углом 90° к земле. При естественном наклоне ~45° данные осей X и Z смещаются, вызывая V-образное провисание прицела.

### Как работает ReGyro Zygisk

Модуль работает исключительно на уровне нативного C++ без использования Xposed/Java:

1. **Изоляция процессов**: Активируется только в `com.ScarletWork.CounterFlame`, для всех остальных процессов немедленно выгружается.
2. **Native-хук**: Использует Dobby для перехвата `ASensorEventQueue_getEvents` в `libandroid.so`.
   - Вращение 2D-матрицы между X и Z на 45° вокруг оси Y.
   - Сохранение естественного микрошума датчиков (защита от банов античитом за аномально плоские данные).
   - Поворот кватернионов векторов вращения на 45° вокруг оси Y.
3. **Отсутствие следов Xposed**: Полная невидимость для проверок памяти на `XposedBridge` и `liblspd.so`.

### Установка

1. Скачайте ZIP из вкладки **Actions** на GitHub (артефакт `ReGyro-Zygisk`).
2. Прошейте ZIP через **KernelSU**, **APatch** или **Magisk**.
3. Перезагрузите устройство.
4. Запустите Counter Flame.

---

<a id="tiếng-việt"></a>
## Tiếng Việt

### Nguyên nhân lỗi trong game

Mã nguồn của Counter Flame (nền tảng Unity) cố định mặt phẳng chuẩn ở góc dựng đứng 90° so với mặt đất. Khi người chơi cầm máy ở góc nghiêng tự nhiên ~45°, các trục cảm biến bị lệch 45° giữa X và Z khiến tâm ngắm bị võng hình chữ V khi lia ngang.

### Cơ chế hoạt động của ReGyro Zygisk

Khác với bản LSPosed cũ phải kéo theo framework Java, bản Zygisk chạy 100% ở tầng C++ Native:

1. **Lọc tiến trình tại Zygote**: Module chỉ kích hoạt khi tiến trình là `com.ScarletWork.CounterFlame`, với mọi app khác module tự giải phóng bộ nhớ (`DLCLOSE_MODULE_LIBRARY`).
2. **Native Hook sạch sẽ**: Dùng Dobby hook trực tiếp `ASensorEventQueue_getEvents` trong `libandroid.so` (đúng đường Unity NDK đọc dữ liệu):
   - Xoay ma trận 2D giữa trục X và Z bù 45° quanh trục Y.
   - **Giữ nguyên dao động tự nhiên (noise) của trục Z**: Không còn ép `0.0f` để tránh bị server game quét anomaly telemetry gắn cờ bot/cheat.
   - Bù 45° cho Rotation Vector (Quaternion).
3. **Không để lại dấu vết Xposed**: Loại bỏ hoàn toàn `XposedBridge`, `de.robv`, `liblspd.so` khỏi bộ nhớ `/proc/self/maps`.

### Cài đặt & Sử dụng

1. Tải file ZIP từ mục **Actions** (Artifact `ReGyro-Zygisk`) trên GitHub.
2. Mở trình quản lý **KernelSU**, **APatch** hoặc **Magisk** -> Chọn cài đặt module từ tệp ZIP vừa tải.
3. Khởi động lại thiết bị.
4. Mở Counter Flame và trải nghiệm.

### Kiểm tra module hoạt động

Xem logcat qua lệnh:
```bash
adb logcat -s ReGyro
```
Log hợp lệ:
- `ReGyro loaded into com.ScarletWork.CounterFlame (Zygisk)`
- `install_hook: DobbyHook ASensorEventQueue_getEvents -> 0`
