package com.example.employeemanager.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Các hàm tính giờ làm và lương (thuần Java, không phụ thuộc Android).
 */
public class TimeUtils {

    /** Số giờ làm chuẩn trong 1 ngày. Vượt quá -> tính tăng ca (overtime). */
    public static final double STANDARD_HOURS_PER_DAY = 8.0;
    /** Hệ số lương tăng ca: 150% lương giờ. */
    public static final double OVERTIME_RATE = 1.5;

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";

    public static String today() {
        return new SimpleDateFormat(DATE_FORMAT, Locale.US).format(new Date());
    }

    public static String nowTime() {
        return new SimpleDateFormat(TIME_FORMAT, Locale.US).format(new Date());
    }

    public static int currentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    public static int currentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /** Prefix dùng để lọc workDate theo tháng, ví dụ "2026-10-". */
    public static String monthPrefix(int month, int year) {
        return String.format(Locale.US, "%04d-%02d-", year, month);
    }

    /**
     * Số giờ giữa checkIn và checkOut (cùng ngày), làm tròn 2 chữ số.
     * Trả về -1 nếu giờ check-out không hợp lệ (<= check-in hoặc sai định dạng).
     */
    public static double hoursBetween(String checkIn, String checkOut) {
        try {
            SimpleDateFormat f = new SimpleDateFormat(TIME_FORMAT, Locale.US);
            f.setLenient(false);
            long in = f.parse(checkIn).getTime();
            long out = f.parse(checkOut).getTime();
            if (out <= in) return -1;
            return round2((out - in) / 3600000.0);
        } catch (ParseException | NullPointerException e) {
            return -1;
        }
    }

    /** Giờ tăng ca của 1 ngày. */
    public static double overtimeOfDay(double totalHours) {
        return round2(Math.max(0, totalHours - STANDARD_HOURS_PER_DAY));
    }

    /** Lương = giờ thường * lương giờ + giờ tăng ca * lương giờ * hệ số tăng ca. */
    public static double calculateSalary(double totalHours, double overtimeHours, double hourlyRate) {
        double regular = totalHours - overtimeHours;
        return Math.round(regular * hourlyRate + overtimeHours * hourlyRate * OVERTIME_RATE);
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public static String money(double v) {
        return String.format(Locale.US, "%,.0f VND", v);
    }

    public static String hours(double v) {
        return String.format(Locale.US, "%.2f h", v);
    }
}
