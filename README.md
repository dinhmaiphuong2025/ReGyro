# ReGyro

Xposed / LSPosed module khắc phục lỗi cảm biến con quay hồi chuyển (gyroscope) cho tựa game Counter Flame (`com.ScarletWork.CounterFlame`).

## Nguyên nhân lỗi trong game

Mã nguồn của Counter Flame (nền tảng Unity) cố định mặt phẳng chuẩn ở góc dựng đứng 90° so với mặt đất. Khi người chơi hạ máy về tư thế cầm tự nhiên ~45° quanh trục ngang màn hình (trục Y của cảm biến):
- Toàn bộ dữ liệu từ `TYPE_GYROSCOPE`, `TYPE_ACCELEROMETER`, `TYPE_GRAVITY` và Rotation Vector (`TYPE_ROTATION_VECTOR`, `TYPE_GAME_ROTATION_VECTOR`) bị lệch 45° giữa trục X và trục Z.
- Hàm tính góc Euler trong game bị võng thành hình chữ V khi lia tâm ngang.

## Cơ chế hoạt động của ReGyro

Module hook vào `android.hardware.SystemSensorManager$SensorEventQueue.dispatchSensorEvent`:
1. **Theo dõi handle cảm biến**: Tự động ánh xạ `handle` sang `Sensor.getType()` thông qua hook `SystemSensorManager.mHandleToSensor` và `registerListenerImpl`.
2. **Xoay bù tọa độ 45°**:
   - Đối với Gyroscope, Accelerometer, Gravity: Xoay ma trận 2D giữa trục X và Z bù lại 45° quanh trục Y.
   - Triệt tiêu thành phần trục Z (`v[2] = 0f`) trên con quay hồi chuyển nhằm loại bỏ hiện tượng lắc tâm/vô lăng không mong muốn.
   - Đối với Rotation Vector (Unity `Input.gyro.attitude`): Nhân quaternion bù 45° quanh trục Y.

## Cài đặt & Sử dụng

1. Tải file APK từ mục **Actions** (Artifact `ReGyro-debug`) trên GitHub.
2. Cài đặt APK vào thiết bị đã cài đặt **LSPosed** (Zygisk / Riru).
3. Mở **LSPosed Manager** -> Kích hoạt module **ReGyro**.
4. Kiểm tra phạm vi (Scope): Đảm bảo đã tích chọn game **Counter Flame** (`com.ScarletWork.CounterFlame`).
5. Buộc dừng game và mở lại để trải nghiệm.
