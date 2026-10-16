package vn.notiguard;

/** Evidence from separate read-only samples, never a claim that a bank or push works. */
final class OfflineCheck {
    enum Stage { MONITORING, UNKNOWN, GMS_MISSING, DEBUG_ENABLED, FIRST_SAMPLE, WAITING, TWO_SAMPLES, AFTER_REBOOT }

    static Stage evaluate(boolean monitoring, Boolean gms, Boolean developer, Boolean usb, Boolean wireless,
                          int firstBoot, int boot, long firstElapsed, long elapsed) {
        if (monitoring) return Stage.MONITORING;
        if (gms == null || developer == null || usb == null || wireless == null) return Stage.UNKNOWN;
        if (!gms) return Stage.GMS_MISSING;
        if (developer || usb || wireless) return Stage.DEBUG_ENABLED;
        if (boot < 0 || elapsed < 0) return Stage.UNKNOWN;
        if (firstBoot < 0 || firstElapsed < 0) return Stage.FIRST_SAMPLE;
        if (boot > firstBoot) return Stage.AFTER_REBOOT;
        // A restored/replaced boot counter or monotonic-clock reversal invalidates the old sample.
        if (boot < firstBoot || elapsed < firstElapsed) return Stage.FIRST_SAMPLE;
        return elapsed - firstElapsed >= 120_000 ? Stage.TWO_SAMPLES : Stage.WAITING;
    }

    static String explanation(Stage stage) {
        return switch (stage) {
            case MONITORING -> "Chưa thể kiểm chứng độc lập: phiên Shizuku chưa được hoàn tác. Hoàn tác trước để tránh việc NotiGuard tự thêm GMS làm sai kết quả.";
            case UNKNOWN -> "Chưa đủ dữ liệu để kết luận. Không coi giá trị không đọc được là false.";
            case GMS_MISSING -> "GMS đang thiếu trong MILLET. Chế độ chỉ kiểm tra không tự sửa được cấu hình này. Thiết lập hệ thống chưa đạt điều kiện đang kiểm tra.";
            case DEBUG_ENABLED -> "GMS có trong MILLET nhưng vẫn còn cờ nhà phát triển/gỡ lỗi bật. Chưa kiểm chứng điều kiện tắt hoàn toàn.";
            case FIRST_SAMPLE -> "Đã ghi mẫu đầu: GMS hiện diện, các cờ gỡ lỗi đã tắt. Đợi ít nhất 2 phút rồi kiểm tra lại; sau đó thử khởi động lại và kiểm tra thêm.";
            case WAITING -> "Chưa đủ 2 phút kể từ mẫu đầu. Mẫu hiện tại vẫn có GMS và các cờ gỡ lỗi tắt.";
            case TWO_SAMPLES -> "Hai lần đọc cách nhau ít nhất 2 phút đều có GMS, các cờ gỡ lỗi tắt. Chưa quan sát liên tục hoặc kiểm tra qua khởi động lại.";
            case AFTER_REBOOT -> "Quan sát thấy GMS sau một lần khởi động lại, các cờ gỡ lỗi đang tắt. Đây là bằng chứng cấu hình tại hai thời điểm; chưa chứng minh push, pin hoặc ngân hàng hoạt động.";
        };
    }
}
