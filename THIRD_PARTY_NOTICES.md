# Nguồn và giấy phép

NotiGuard được viết cho dự án này, phát hành theo GPL-3.0-or-later (xem LICENSE).

Hai thuật toán trong `XiaomiRules.java` được chuyển từ Kotlin sang Java và thay marker sở hữu:

- [dingwen07/hyperos-fcm-fix](https://github.com/dingwen07/hyperos-fcm-fix), tác giả dingwen07 và những người đóng góp; GPL-3.0.
- Commit cố định: `fe594239b01ba4b67d60da764683cadd13e5d287`.
- Tệp gốc: `AurogonConfig.kt`, `MilletNoRestrictList.kt`.
- Không đưa APK, script thực thi, tài nguyên hoặc các dịch vụ nền của dự án đó vào NotiGuard.

Thư viện sử dụng:

- [Shizuku-API 13.1.5](https://github.com/RikkaApps/Shizuku-API), RikkaApps, MIT. API và provider; không đóng gói ứng dụng Shizuku.
- AndroidX annotation là phụ thuộc gián tiếp của Shizuku, Apache-2.0.
- JUnit 4.13.2 (EPL-1.0) và org.json 20240303 (public domain) chỉ dùng trong kiểm thử, không nằm trong APK.
- Gradle wrapper 8.13 từ [gradle/gradle](https://github.com/gradle/gradle/tree/v8.13.0), Apache-2.0; chỉ là công cụ build.

MIT License — Shizuku API

Copyright (c) 2021 Rikka

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
