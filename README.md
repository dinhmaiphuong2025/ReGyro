# ReGyro

Xposed / LSPosed module that fixes the gyroscope aiming bug in Counter Flame (`com.ScarletWork.CounterFlame`).

**[English](#english) | [中文](#中文) | [Русский](#русский) | [Tiếng Việt](#tiếng-việt)**

---

<a id="english"></a>
## English

### Root cause

Counter Flame is a Unity game whose code assumes the reference plane of the device is at a 90° upright angle to the ground. When you hold the phone at a natural ~45° tilt around the screen's horizontal axis (the sensor's Y axis):

- All data from `TYPE_GYROSCOPE`, `TYPE_ACCELEROMETER`, `TYPE_GRAVITY` and rotation vectors (`TYPE_ROTATION_VECTOR`, `TYPE_GAME_ROTATION_VECTOR`) is offset by 45° between the X and Z axes.
- The game's Euler-angle calculation sags into a "V" shape when aiming horizontally.

### How ReGyro works

Unity reads sensors through the **NDK**, not through Java's `SystemSensorManager`, so a plain Java hook has no effect. ReGyro uses the **LSPosed Native Hook**:

1. **Native hook (primary)**: `native_init` hooks `ASensorEventQueue_getEvents` in `libandroid.so` — the exact path `libunity.so` uses to pull sensor events. As soon as data arrives, the module rewrites it in place:
   - Gyroscope, Gyroscope Uncalibrated, Accelerometer, Gravity, Linear Acceleration: 2D rotation between the X and Z axes to compensate 45° around the Y axis.
   - Zeroes the Z component on gyroscopes (`data[2] = 0`) so the crosshair is not shaken by the roll axis.
   - Rotation Vector / Game Rotation Vector (Unity `Input.gyro.attitude`): multiplies the quaternion by a 45° compensation around the Y axis.
2. **Java hook (fallback)**: hooks `SystemSensorManager$SensorEventQueue.dispatchSensorEvent` in case the game uses the Java `SensorManager`.

### Installation

1. Download the APK from the **Actions** tab on GitHub (artifact `ReGyro-debug`).
2. Install it on a device with **LSPosed** (Zygisk).
3. Open **LSPosed Manager** and enable the **ReGyro** module.
4. Check the scope: make sure **Counter Flame** (`com.ScarletWork.CounterFlame`) is selected.
5. Force-stop the game and relaunch it.

### Verifying the module is running

In **LSPosed Manager -> Log**, filter by tag `ReGyro`:
- `ReGyro: loaded into com.ScarletWork.CounterFlame` — the module was injected into the game.
- `ReGyro: native lib loaded` — the native library was loaded.
- `native_init: hook ASensorEventQueue_getEvents -> 0` — the native hook was installed (code 0 = OK).

If any line is missing, check the module scope and restart the game.

---

<a id="中文"></a>
## 中文

### 问题根源

Counter Flame 是一款 Unity 游戏，其代码假定设备的参考平面与地面呈 90° 竖直夹角。当你以自然的约 45° 倾角握持手机（绕屏幕水平轴，即传感器的 Y 轴）时：

- `TYPE_GYROSCOPE`、`TYPE_ACCELEROMETER`、`TYPE_GRAVITY` 以及旋转矢量（`TYPE_ROTATION_VECTOR`、`TYPE_GAME_ROTATION_VECTOR`）的所有数据在 X 轴与 Z 轴之间产生 45° 偏移。
- 游戏中的欧拉角计算在水平瞄准时呈现"V"形下垂。

### 工作原理

Unity 通过 **NDK** 读取传感器，而不是通过 Java 的 `SystemSensorManager`，因此普通的 Java Hook 无效。ReGyro 使用 **LSPosed Native Hook**：

1. **Native Hook（主要）**：`native_init` 挂钩 `libandroid.so` 中的 `ASensorEventQueue_getEvents` —— 这正是 `libunity.so` 获取传感器事件的路径。数据一到，模块就地进行改写：
   - 陀螺仪、未校准陀螺仪、加速度计、重力、线性加速度：在 X 轴与 Z 轴之间进行 2D 旋转，绕 Y 轴补偿 45°。
   - 将陀螺仪的 Z 分量置零（`data[2] = 0`），使准星不受横滚轴影响。
   - 旋转矢量 / 游戏旋转矢量（Unity `Input.gyro.attitude`）：将四元数乘以绕 Y 轴的 45° 补偿四元数。
2. **Java Hook（备用）**：挂钩 `SystemSensorManager$SensorEventQueue.dispatchSensorEvent`，以防游戏使用 Java 的 `SensorManager`。

### 安装

1. 从 GitHub 的 **Actions** 页面下载 APK（工件 `ReGyro-debug`）。
2. 在已安装 **LSPosed**（Zygisk）的设备上安装。
3. 打开 **LSPosed Manager**，启用 **ReGyro** 模块。
4. 检查作用域：确保已勾选 **Counter Flame**（`com.ScarletWork.CounterFlame`）。
5. 强制停止游戏并重新启动。

### 验证模块是否生效

在 **LSPosed Manager -> 日志** 中按标签 `ReGyro` 过滤：
- `ReGyro: loaded into com.ScarletWork.CounterFlame` — 模块已注入游戏。
- `ReGyro: native lib loaded` — 原生库已加载。
- `native_init: hook ASensorEventQueue_getEvents -> 0` — 原生 Hook 已安装（代码 0 = 成功）。

如果缺少任何一行，请检查模块作用域并重启游戏。

---

<a id="русский"></a>
## Русский

### Причина проблемы

Counter Flame — игра на Unity, код которой предполагает, что опорная плоскость устройства находится под углом 90° к земле. Когда вы держите телефон под естественным наклоном ~45° вокруг горизонтальной оси экрана (оси Y датчика):

- Все данные с `TYPE_GYROSCOPE`, `TYPE_ACCELEROMETER`, `TYPE_GRAVITY` и векторов вращения (`TYPE_ROTATION_VECTOR`, `TYPE_GAME_ROTATION_VECTOR`) смещаются на 45° между осями X и Z.
- Расчёт углов Эйлера в игре «провисает» в форме буквы «V» при горизонтальном прицеливании.

### Как работает ReGyro

Unity читает датчики через **NDK**, а не через Java `SystemSensorManager`, поэтому обычный Java-хук не действует. ReGyro использует **LSPosed Native Hook**:

1. **Native-хук (основной)**: `native_init` перехватывает `ASensorEventQueue_getEvents` в `libandroid.so` — тот самый путь, по которому `libunity.so` получает события датчиков. Как только данные поступают, модуль переписывает их на месте:
   - Гироскоп, некалиброванный гироскоп, акселерометр, гравитация, линейное ускорение: 2D-поворот между осями X и Z для компенсации 45° вокруг оси Y.
   - Обнуляет компонент Z у гироскопа (`data[2] = 0`), чтобы прицел не трясло по оси крена.
   - Вектор вращения / игровой вектор вращения (Unity `Input.gyro.attitude`): умножает кватернион на компенсационный кватернион 45° вокруг оси Y.
2. **Java-хук (резервный)**: перехватывает `SystemSensorManager$SensorEventQueue.dispatchSensorEvent` на случай, если игра использует Java `SensorManager`.

### Установка

1. Скачайте APK со вкладки **Actions** на GitHub (артефакт `ReGyro-debug`).
2. Установите его на устройство с **LSPosed** (Zygisk).
3. Откройте **LSPosed Manager** и включите модуль **ReGyro**.
4. Проверьте область действия: убедитесь, что выбрана **Counter Flame** (`com.ScarletWork.CounterFlame`).
5. Принудительно остановите игру и запустите её заново.

### Проверка работы модуля

В **LSPosed Manager -> Журнал** отфильтруйте по тегу `ReGyro`:
- `ReGyro: loaded into com.ScarletWork.CounterFlame` — модуль внедрён в игру.
- `ReGyro: native lib loaded` — нативная библиотека загружена.
- `native_init: hook ASensorEventQueue_getEvents -> 0` — нативный хук установлен (код 0 = ОК).

Если какой-то строки нет, проверьте область действия модуля и перезапустите игру.

---

<a id="tiếng-việt"></a>
## Tiếng Việt

### Nguyên nhân lỗi trong game

Mã nguồn của Counter Flame (nền tảng Unity) cố định mặt phẳng chuẩn ở góc dựng đứng 90° so với mặt đất. Khi người chơi hạ máy về tư thế cầm tự nhiên ~45° quanh trục ngang màn hình (trục Y của cảm biến):

- Toàn bộ dữ liệu từ `TYPE_GYROSCOPE`, `TYPE_ACCELEROMETER`, `TYPE_GRAVITY` và Rotation Vector (`TYPE_ROTATION_VECTOR`, `TYPE_GAME_ROTATION_VECTOR`) bị lệch 45° giữa trục X và trục Z.
- Hàm tính góc Euler trong game bị võng thành hình chữ V khi lia tâm ngang.

### Cơ chế hoạt động của ReGyro

Unity đọc cảm biến qua **NDK**, không đi qua `SystemSensorManager` của Java, nên hook Java thông thường không có tác dụng. ReGyro dùng **LSPosed Native Hook**:

1. **Native hook (chính)**: `native_init` hook `ASensorEventQueue_getEvents` trong `libandroid.so` — đúng đường dữ liệu mà `libunity.so` kéo sự kiện cảm biến về. Ngay khi dữ liệu về, module sửa tại chỗ:
   - Gyroscope, Gyroscope Uncalibrated, Accelerometer, Gravity, Linear Acceleration: xoay 2D giữa trục X và Z bù 45° quanh trục Y.
   - Triệt tiêu thành phần trục Z trên con quay hồi chuyển (`data[2] = 0`) để đường tâm không bị lắc trục Roll.
   - Rotation Vector / Game Rotation Vector (Unity `Input.gyro.attitude`): nhân quaternion bù 45° quanh trục Y.
2. **Java hook (dự phòng)**: hook `SystemSensorManager$SensorEventQueue.dispatchSensorEvent` phòng trường hợp game dùng `SensorManager` Java.

### Cài đặt & Sử dụng

1. Tải file APK từ mục **Actions** (Artifact `ReGyro-debug`) trên GitHub.
2. Cài đặt APK vào thiết bị đã cài đặt **LSPosed** (Zygisk).
3. Mở **LSPosed Manager** -> Kích hoạt module **ReGyro**.
4. Kiểm tra phạm vi (Scope): Đảm bảo đã tích chọn game **Counter Flame** (`com.ScarletWork.CounterFlame`).
5. Buộc dừng game và mở lại để trải nghiệm.

### Kiểm tra module đã chạy chưa

Trong **LSPosed Manager -> Log**, lọc tag `ReGyro`:
- `ReGyro: loaded into com.ScarletWork.CounterFlame` — module đã được nạp vào game.
- `ReGyro: native lib loaded` — thư viện native đã nạp.
- `native_init: hook ASensorEventQueue_getEvents -> 0` — hook native đã cài thành công (mã 0 = OK).

Nếu thiếu dòng nào, kiểm tra lại scope của module và khởi động lại game.
