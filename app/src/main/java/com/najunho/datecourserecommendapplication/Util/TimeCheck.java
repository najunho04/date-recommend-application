package com.najunho.datecourserecommendapplication.Util;

import com.google.firebase.Timestamp;

import java.util.Calendar;

public class TimeCheck {
    public static boolean isValidTimeFormat(String input) {
        if (input == null) return false;

        // 00:00 형식 정규식
        if (!input.matches("^\\d{2}:\\d{2}$")) {
            return false;
        }

        // 실제 숫자 범위 검사
        String[] parts = input.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        return (hour >= 0 && hour <= 23) && (minute >= 0 && minute <= 59);
    }

    public static Timestamp convertToTimestamp(String timeString) {
        if (!isValidTimeFormat(timeString)) return null;

        String[] parts = timeString.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return new Timestamp(calendar.getTime());
    }
}
