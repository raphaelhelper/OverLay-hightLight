# Word Popup

App Android: quét chữ đang hiển thị trên màn hình (mọi app khác), highlight các từ có
trong danh sách từ vựng của bạn, tap vào từ để xem nghĩa + ví dụ + dịch.

## Nguồn dữ liệu

Đọc mọi file `.txt` nằm trực tiếp trong 1 thư mục bạn chọn (không đọc thư mục con).
Mỗi mục trong file đúng định dạng:

```
1.

two-story
hai tầng
Example: They live in a nice two-story house.
Dịch: Họ sống trong một ngôi nhà hai tầng đẹp.
```

## Cách dùng

1. Mở app → **"1. Chọn thư mục chứa file từ vựng"** → chọn thư mục có ~200 file txt.
   App tự đọc và giữ trong bộ nhớ (không cần đọc lại mỗi lần mở app).
2. Bấm **"2. Bật quyền Accessibility"** → bật app "Word Popup" trong danh sách dịch vụ
   trợ năng (Accessibility). Đây là quyền bắt buộc để app đọc được chữ trên màn hình.
3. Xong — mở bất kỳ app nào khác, từ nào có trong danh sách sẽ được highlight màu xanh
   nhạt. Tap vào để hiện popup nghĩa + ví dụ + dịch; tap dấu ✕ để đóng, tap lại từ đó
   để đóng nhanh. Popup không tự tắt.

Không cần cấp quyền "hiển thị trên ứng dụng khác" — overlay dùng loại cửa sổ dành
riêng cho Accessibility Service nên không cần quyền đó.

## Build APK bằng GitHub Actions (làm trên điện thoại)

1. Tạo 1 repo GitHub mới, upload toàn bộ nội dung thư mục này lên (giữ nguyên cấu trúc
   thư mục, kể cả `.github/workflows/build-apk.yml`).
2. Vào tab **Actions** của repo → workflow "Build APK" sẽ tự chạy sau khi push (hoặc
   bấm "Run workflow" để chạy tay).
3. Đợi build xong (vài phút) → vào lần chạy đó → mục **Artifacts** → tải
   `word-popup-debug-apk` (file zip chứa `app-debug.apk`).
4. Giải nén, chuyển file `.apk` vào điện thoại, bật "Cài từ nguồn không xác định" rồi
   cài như bình thường.

## Giới hạn cần biết

- Vị trí highlight dựa vào API `EXTRA_DATA_TEXT_CHARACTER_LOCATION` (chính xác theo
  từng chữ) khi app đang quét hỗ trợ; nếu không lấy được, app fallback highlight cả
  khối chữ (node) chứa từ đó thay vì đúng 1 từ.
- Một số app (ví dụ app ngân hàng, ô nhập mật khẩu) chặn Accessibility đọc nội dung —
  những màn hình đó sẽ không highlight được, đây là giới hạn hệ thống, không phải lỗi.
- App chỉ đọc file `.txt` nằm trực tiếp trong thư mục đã chọn, không quét thư mục con.
- minSdk 26 (Android 8.0 trở lên).
