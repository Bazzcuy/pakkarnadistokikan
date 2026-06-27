package com.bagas.stokikan.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateUtil {
    private DateUtil() {}

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    public static String batchNo(int next) {
        return "BG-" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()) + "-" + String.format(Locale.US, "%03d", next);
    }

    public static String trxNo(int next) {
        return "TRX-" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()) + "-" + String.format(Locale.US, "%03d", next);
    }
}
