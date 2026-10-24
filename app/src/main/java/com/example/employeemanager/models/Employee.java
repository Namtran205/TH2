package com.example.employeemanager.models;

import java.util.Locale;

public class Employee {
    private int id;
    private String code;
    private String fullname;
    private String email;
    private String phone;
    private double hourlyRate;
    private int accountId;

    // Không thuộc bảng Employee, chỉ dùng để hiển thị/sửa tài khoản đăng nhập
    private String username;

    public Employee() {
    }

    public Employee(int id, String code, String fullname, String email, String phone,
                    double hourlyRate, int accountId) {
        this.id = id;
        this.code = code;
        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
        this.hourlyRate = hourlyRate;
        this.accountId = accountId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    @Override
    public String toString() {
        return code + " - " + fullname
                + "\nEmail: " + email + " | Phone: " + phone
                + "\nHourly rate: " + String.format(Locale.US, "%,.0f", hourlyRate) + " VND/h"
                + (username != null ? " | User: " + username : "");
    }
}
