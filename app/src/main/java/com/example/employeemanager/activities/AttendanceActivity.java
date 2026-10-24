package com.example.employeemanager.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.employeemanager.R;
import com.example.employeemanager.dal.DBHelper;
import com.example.employeemanager.models.Employee;
import com.example.employeemanager.models.WorkLog;
import com.example.employeemanager.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lịch sử chấm công theo ngày + tổng hợp giờ làm theo tháng.
 * Dùng cho cả nhân viên (xem của mình) và admin (xem của 1 nhân viên).
 */
public class AttendanceActivity extends AppCompatActivity {
    public static final String EXTRA_EMPLOYEE_ID = "employeeId";

    private DBHelper db;
    private Employee employee;
    private EditText edtMonth, edtYear;
    private TextView tvSummary;
    private ArrayAdapter<WorkLog> adapter;
    private final List<WorkLog> logs = new ArrayList<>();
    private int month, year;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        setTitle("Attendance History");

        db = DBHelper.getInstance(this);
        employee = db.getEmployeeById(getIntent().getIntExtra(EXTRA_EMPLOYEE_ID, -1));
        if (employee == null) {
            finish();
            return;
        }
        db.markIncompleteLogs();

        ((TextView) findViewById(R.id.tvEmployee)).setText(employee.getCode() + " - " + employee.getFullname());
        edtMonth = findViewById(R.id.edtMonth);
        edtYear = findViewById(R.id.edtYear);
        tvSummary = findViewById(R.id.tvSummary);
        ListView lv = findViewById(R.id.lvLogs);

        month = TimeUtils.currentMonth();
        year = TimeUtils.currentYear();
        edtMonth.setText(String.valueOf(month));
        edtYear.setText(String.valueOf(year));

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logs);
        lv.setAdapter(adapter);

        findViewById(R.id.btnView).setOnClickListener(v -> {
            try {
                int m = Integer.parseInt(edtMonth.getText().toString().trim());
                int y = Integer.parseInt(edtYear.getText().toString().trim());
                if (m < 1 || m > 12) { edtMonth.setError("1 - 12"); return; }
                if (y < 2000 || y > 2100) { edtYear.setError("Invalid year"); return; }
                month = m;
                year = y;
                load();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid month and year", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btnAll).setOnClickListener(v -> {
            month = 0;
            load();
        });
        load();
    }

    private void load() {
        logs.clear();
        logs.addAll(db.getWorkLogs(employee.getId(), month, year));
        adapter.notifyDataSetChanged();

        int completed = 0, incomplete = 0, working = 0;
        double total = 0, ot = 0;
        for (WorkLog l : logs) {
            switch (l.getStatus()) {
                case WorkLog.STATUS_COMPLETED:
                    completed++;
                    total += l.getTotalHours();
                    ot += TimeUtils.overtimeOfDay(l.getTotalHours());
                    break;
                case WorkLog.STATUS_INCOMPLETE:
                    incomplete++;
                    break;
                default:
                    working++;
            }
        }
        total = TimeUtils.round2(total);
        ot = TimeUtils.round2(ot);

        String period = month == 0 ? "All time"
                : String.format(Locale.US, "Monthly summary %02d/%d", month, year);
        tvSummary.setText(period
                + "\nCompleted days: " + completed
                + "   Incomplete: " + incomplete
                + (working > 0 ? "   In progress: " + working : "")
                + "\nTotal hours: " + TimeUtils.hours(total)
                + "   Regular: " + TimeUtils.hours(TimeUtils.round2(total - ot))
                + "\nOvertime: " + TimeUtils.hours(ot)
                + "\nEstimated salary: " + TimeUtils.money(
                TimeUtils.calculateSalary(total, ot, employee.getHourlyRate()))
                + (incomplete > 0 ? "\n* Incomplete days (no check-out) are not paid." : ""));
    }
}
