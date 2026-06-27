package com.bagas.stokikan.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtil {
    private DateUtil() {}

    public static String today() {
        return LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    }

    public static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String batchNumber(int nextId) {
        return "BG-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + String.format("%03d", nextId);
    }

    public static String transactionNumber(int nextId) {
        return "TRX-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + String.format("%03d", nextId);
    }
}
