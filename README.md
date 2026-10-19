# ReGyro

Xposed / LSPosed module khắc phục lỗi cảm biến con quay hồi chuyển (gyroscope) cho tựa game Counter Flame (`com.ScarletWork.CounterFlame`).

## Nguyên nhân lỗi trong game

Mã nguồn của Counter Flame (nền tảng Unity) cố định mặt phẳng chuẩn ở góc dựng đứng 90° so với mặt đất. Khi người chơi hạ máy về tư thế cầm tự nhiên ~45° quanh trục ngang màn hình (trục Y của cảm biến):
- Toàn bộ dữ liệu từ `TYPE_GYROSCOPE`, `TYPE_ACCELEROMETER`, `TYPE_GRAVITY` và Rotation Vector (`TYPE_ROTATION_VECTOR`, `TYPE_GAME_ROTATION_VECTOR`) bị lệch 45° giữa trục X và trục Z.
- Hàm tính góc Euler trong game bị võng thành hình chữ V khi lia tâm ngang.

## Cơ chế hoạt động của ReGyro

Unity đọc cảm biến qua **NDK**, không đi qua `SystemSensorManager` của Java, nên hook Java thông thường không có tác dụng. ReGyro dùng **LSPosed Native Hook**:

1. **Native hook (chính)**: `native_init` hook `ASensorEventQueue_getEvents` trong `libandroid.so` — đúng đường dữ liệu mà `libunity.so` kéo sự kiện cảm biến về. Ngay khi dữ liệu về, module sửa tại chỗ:
   - Gyroscope, Gyroscope Uncalibrated, Accelerometer, Gravity, Linear Acceleration: xoay 2D giữa trục X và Z bù 45° quanh trục Y.
   - Triệt tiêu thành phần trục Z trên con quay hồi chuyển (`data[2] = 0`) để đường tâm không bị lắc trục Roll.
   - Rotation Vector / Game Rotation Vector (Unity `Input.gyro.attitude`): nhân quaternion bù 45° quanh trục Y.
2. **Java hook (dự phòng)**: hook `SystemSensorManager$SensorEventQueue.dispatchSensorEvent` phòng trường hợp game dùng `SensorManager` Java.

## Cài đặt & Sử dụng

1. Tải file APK từ mục **Actions** (Artifact `ReGyro-debug`) trên GitHub.
2. Cài đặt APK vào thiết bị đã cài đặt **LSPosed** (Zygisk).
3. Mở **LSPosed Manager** -> Kích hoạt module **ReGyro**.
4. Kiểm tra phạm vi (Scope): Đảm bảo đã tích chọn game **Counter Flame** (`com.ScarletWork.CounterFlame`).
5. Buộc dừng game và mở lại để trải nghiệm.

## Kiểm tra module đã chạy chưa

Trong **LSPosed Manager -> Log**, lọc tag `ReGyro`:
- `ReGyro: loaded into com.ScarletWork.CounterFlame` — module đã được nạp vào game.
- `ReGyro: native lib loaded` — thư viện native đã nạp.
- `native_init: hook ASensorEventQueue_getEvents -> 0` — hook native đã cài thành công (mã 0 = OK).

Nếu thiếu dòng nào, kiểm tra lại scope của module và khởi động lại game.
