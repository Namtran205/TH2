package com.example.employeemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.employeemanager.R;
import com.example.employeemanager.dal.DBHelper;
import com.example.employeemanager.models.Employee;
import com.example.employeemanager.models.WorkLog;
import com.example.employeemanager.utils.SessionManager;
import com.example.employeemanager.utils.TimeUtils;

import java.util.List;

/**
 * Nhân viên: check-in / check-out hằng ngày, xem lịch sử chấm công và bảng lương.
 */
public class EmployeeActivity extends AppCompatActivity {
    private DBHelper db;
    private SessionManager session;
    private Employee employee;
    private TextView tvToday, tvMonth;
    private Button btnCheckIn, btnCheckOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee);
        setTitle("Attendance");

        db = DBHelper.getInstance(this);
        session = new SessionManager(this);
        employee = db.getEmployeeByAccountId(session.getAccountId());
        if (employee == null) { // tài khoản đã bị xoá
            logout();
            return;
        }

        ((TextView) findViewById(R.id.tvWelcome)).setText("Hello, " + employee.getFullname());
        ((TextView) findViewById(R.id.tvInfo)).setText("Code: " + employee.getCode()
                + "  |  Hourly rate: " + TimeUtils.money(employee.getHourlyRate()) + "/h");
        tvToday = findViewById(R.id.tvToday);
        tvMonth = findViewById(R.id.tvMonth);
        btnCheckIn = findViewById(R.id.btnCheckIn);
        btnCheckOut = findViewById(R.id.btnCheckOut);

        btnCheckIn.setOnClickListener(v -> {
            String err = db.checkIn(employee.getId());
            showResult(err, "Checked in at " + TimeUtils.nowTime());
        });
        btnCheckOut.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Check out")
                .setMessage("Check out now? You cannot check in again today.")
                .setPositiveButton("Check out", (d, w) -> {
                    String err = db.checkOut(employee.getId());
                    WorkLog log = db.getWorkLog(employee.getId(), TimeUtils.today());
                    showResult(err, log == null ? "Checked out"
                            : "Checked out. Worked " + TimeUtils.hours(log.getTotalHours()) + " today.");
                })
                .setNegativeButton("Cancel", null)
                .show());

        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            Intent i = new Intent(this, AttendanceActivity.class);
            i.putExtra(AttendanceActivity.EXTRA_EMPLOYEE_ID, employee.getId());
            startActivity(i);
        });
        findViewById(R.id.btnMySalary).setOnClickListener(v -> {
            Intent i = new Intent(this, SalaryActivity.class);
            i.putExtra(SalaryActivity.EXTRA_EMPLOYEE_ID, employee.getId());
            startActivity(i);
        });
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (employee != null) refresh();
    }

    private void showResult(String error, String successMsg) {
        if (error != null) {
            new AlertDialog.Builder(this).setTitle("Not allowed").setMessage(error)
                    .setPositiveButton("OK", null).show();
        } else {
            Toast.makeText(this, successMsg, Toast.LENGTH_LONG).show();
        }
        refresh();
    }

    private void refresh() {
        db.markIncompleteLogs();
        String today = TimeUtils.today();
        WorkLog log = db.getWorkLog(employee.getId(), today);

        String status;
        if (log == null) {
            status = "Not checked in yet";
        } else if (WorkLog.STATUS_CHECKED_IN.equals(log.getStatus())) {
            status = "Working — checked in at " + log.getCheckInTime();
        } else {
            status = "Done — " + log.getCheckInTime() + " → " + log.getCheckOutTime()
                    + " (" + TimeUtils.hours(log.getTotalHours()) + ")";
        }
        tvToday.setText("Today: " + today + "\nStatus: " + status);

        // Nút được bật/tắt theo trạng thái để tránh thao tác sai
        btnCheckIn.setEnabled(log == null);
        btnCheckOut.setEnabled(log != null && WorkLog.STATUS_CHECKED_IN.equals(log.getStatus()));

        // Tóm tắt tháng hiện tại
        int m = TimeUtils.currentMonth(), y = TimeUtils.currentYear();
        List<WorkLog> logs = db.getWorkLogs(employee.getId(), m, y);
        double total = 0, ot = 0;
        int days = 0;
        for (WorkLog l : logs) {
            if (WorkLog.STATUS_COMPLETED.equals(l.getStatus())) {
                days++;
                total += l.getTotalHours();
                ot += TimeUtils.overtimeOfDay(l.getTotalHours());
            }
        }
        total = TimeUtils.round2(total);
        ot = TimeUtils.round2(ot);
        tvMonth.setText("This month (" + m + "/" + y + ")"
                + "\nWorking days: " + days
                + "\nTotal hours: " + TimeUtils.hours(total) + "   Overtime: " + TimeUtils.hours(ot)
                + "\nEstimated salary: " + TimeUtils.money(
                TimeUtils.calculateSalary(total, ot, employee.getHourlyRate())));
    }

    private void logout() {
        session.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
