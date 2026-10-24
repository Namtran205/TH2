package com.example.employeemanager.models;

import java.util.Locale;

public class Salary {
    public static final String STATUS_UNPAID = "UNPAID";
    public static final String STATUS_PAID = "PAID";

    private int id;
    private int employeeId;
    private int month;
    private int year;
    private double totalHours;
    private double overtimeHours;
    private double totalSalary;
    private String status;

    // Chỉ dùng để hiển thị
    private String employeeCode;
    private String employeeName;

    public Salary() {
    }

    public Salary(int id, int employeeId, int month, int year, double totalHours,
                  double overtimeHours, double totalSalary, String status) {
        this.id = id;
        this.employeeId = employeeId;
        this.month = month;
        this.year = year;
        this.totalHours = totalHours;
        this.overtimeHours = overtimeHours;
        this.totalSalary = totalSalary;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public double getTotalHours() { return totalHours; }
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }

    public double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(double overtimeHours) { this.overtimeHours = overtimeHours; }

    public double getTotalSalary() { return totalSalary; }
    public void setTotalSalary(double totalSalary) { this.totalSalary = totalSalary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public double getRegularHours() {
        return totalHours - overtimeHours;
    }

    @Override
    public String toString() {
        String who = employeeCode != null ? employeeCode + " - " + employeeName + "\n" : "";
        return who + String.format(Locale.US, "%02d/%d  [%s]", month, year, status)
                + String.format(Locale.US, "\nHours: %.2f h (OT: %.2f h)", totalHours, overtimeHours)
                + String.format(Locale.US, "\nSalary: %,.0f VND", totalSalary);
    }
}
