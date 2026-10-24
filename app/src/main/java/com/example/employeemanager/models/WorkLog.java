package com.example.employeemanager.models;

import java.util.Locale;

public class WorkLog {
    public static final String STATUS_CHECKED_IN = "CHECKED_IN";   // đã check-in, chưa check-out
    public static final String STATUS_COMPLETED = "COMPLETED";     // đủ check-in + check-out
    public static final String STATUS_INCOMPLETE = "INCOMPLETE";   // qua ngày mà không check-out

    private int id;
    private int employeeId;
    private String workDate;      // yyyy-MM-dd
    private String checkInTime;   // HH:mm:ss
    private String checkOutTime;  // HH:mm:ss (null nếu chưa check-out)
    private double totalHours;
    private String status;

    public WorkLog() {
    }

    public WorkLog(int id, int employeeId, String workDate, String checkInTime,
                   String checkOutTime, double totalHours, String status) {
        this.id = id;
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.totalHours = totalHours;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getWorkDate() { return workDate; }
    public void setWorkDate(String workDate) { this.workDate = workDate; }

    public String getCheckInTime() { return checkInTime; }
    public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }

    public String getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(String checkOutTime) { this.checkOutTime = checkOutTime; }

    public double getTotalHours() { return totalHours; }
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return workDate + "  [" + status + "]"
                + "\nIn: " + checkInTime
                + "   Out: " + (checkOutTime == null ? "--:--:--" : checkOutTime)
                + "   Total: " + String.format(Locale.US, "%.2f", totalHours) + " h";
    }
}
