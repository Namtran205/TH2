package com.example.employeemanager.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.employeemanager.R;
import com.example.employeemanager.dal.DBHelper;
import com.example.employeemanager.models.Employee;
import com.example.employeemanager.models.Salary;
import com.example.employeemanager.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bảng lương.
 * - Admin (không truyền EXTRA_EMPLOYEE_ID): xem tất cả, tính lương theo tháng, đánh dấu đã trả, tính lại, xoá.
 * - Nhân viên (có EXTRA_EMPLOYEE_ID): chỉ xem bảng lương của mình.
 */
public class SalaryActivity extends AppCompatActivity {
    public static final String EXTRA_EMPLOYEE_ID = "employeeId";

    private DBHelper db;
    private EditText edtMonth, edtYear;
    private TextView tvSummary;
    private ArrayAdapter<Salary> adapter;
    private final List<Salary> salaries = new ArrayList<>();
    private int employeeId = -1;        // -1 = chế độ admin
    private int filterMonth, filterYear; // filterMonth = 0 -> tất cả

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary);

        db = DBHelper.getInstance(this);
        employeeId = getIntent().getIntExtra(EXTRA_EMPLOYEE_ID, -1);
        setTitle(isAdmin() ? "Salary Management" : "My Salary");

        edtMonth = findViewById(R.id.edtMonth);
        edtYear = findViewById(R.id.edtYear);
        tvSummary = findViewById(R.id.tvSummary);
        ListView lv = findViewById(R.id.lvSalaries);

        filterMonth = TimeUtils.currentMonth();
        filterYear = TimeUtils.currentYear();
        edtMonth.setText(String.valueOf(filterMonth));
        edtYear.setText(String.valueOf(filterYear));

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, salaries);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((p, v, pos, id) -> showSalaryDetail(salaries.get(pos)));

        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            if (!readFilter()) return;
            // Nhân viên: tự động tính lương cho tháng được chọn (nếu chưa PAID)
            if (!isAdmin()) db.calculateSalary(employeeId, filterMonth, filterYear);
            loadData();
        });
        findViewById(R.id.btnAll).setOnClickListener(v -> {
            filterMonth = 0;
            loadData();
        });

        View btnCalc = findViewById(R.id.btnCalculate);
        if (isAdmin()) {
            btnCalc.setOnClickListener(v -> calculateAll());
        } else {
            btnCalc.setVisibility(View.GONE);
            // Nhân viên: tự động cập nhật lương tháng hiện tại khi mở màn hình
            db.markIncompleteLogs();
            db.calculateSalary(employeeId, filterMonth, filterYear);
        }
        loadData();
    }

    private boolean isAdmin() {
        return employeeId == -1;
    }

    private boolean readFilter() {
        try {
            int m = Integer.parseInt(edtMonth.getText().toString().trim());
            int y = Integer.parseInt(edtYear.getText().toString().trim());
            if (m < 1 || m > 12) { edtMonth.setError("1 - 12"); return false; }
            if (y < 2000 || y > 2100) { edtYear.setError("Invalid year"); return false; }
            filterMonth = m;
            filterYear = y;
            return true;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid month and year", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void calculateAll() {
        if (!readFilter()) return;
        int n = db.calculateAllSalaries(filterMonth, filterYear);
        Toast.makeText(this, n == 0
                        ? "No completed attendance in this month"
                        : "Calculated " + n + " salary record(s). PAID records are kept unchanged.",
                Toast.LENGTH_LONG).show();
        loadData();
    }

    private void loadData() {
        salaries.clear();
        if (isAdmin()) {
            salaries.addAll(db.getSalaries(filterMonth, filterYear));
        } else {
            for (Salary s : db.getSalariesByEmployee(employeeId)) {
                if (filterMonth == 0 || (s.getMonth() == filterMonth && s.getYear() == filterYear)) {
                    salaries.add(s);
                }
            }
        }
        adapter.notifyDataSetChanged();

        double hours = 0, ot = 0, total = 0, paid = 0;
        for (Salary s : salaries) {
            hours += s.getTotalHours();
            ot += s.getOvertimeHours();
            total += s.getTotalSalary();
            if (Salary.STATUS_PAID.equals(s.getStatus())) paid += s.getTotalSalary();
        }
        String period = filterMonth == 0 ? "All months"
                : String.format(Locale.US, "Month %02d/%d", filterMonth, filterYear);
        tvSummary.setText(period + " — " + salaries.size() + " record(s)"
                + "\nTotal hours: " + TimeUtils.hours(TimeUtils.round2(hours))
                + "   Overtime: " + TimeUtils.hours(TimeUtils.round2(ot))
                + "\nTotal salary: " + TimeUtils.money(total)
                + (isAdmin() ? "\nPaid: " + TimeUtils.money(paid) + "   Unpaid: " + TimeUtils.money(total - paid) : "")
                + String.format(Locale.US, "\n(Overtime = hours over %.0fh/day, paid x%.1f)",
                TimeUtils.STANDARD_HOURS_PER_DAY, TimeUtils.OVERTIME_RATE));
        if (salaries.isEmpty()) {
            tvSummary.append(isAdmin() ? "\nNo records. Press \"Calculate\" to generate." : "\nNo salary records yet.");
        }
    }

    private void showSalaryDetail(Salary s) {
        Employee e = db.getEmployeeById(s.getEmployeeId());
        if (e == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Salary details")
                .setMessage(DBHelper.describeSalary(s, e));

        if (!isAdmin()) {
            b.setPositiveButton("Close", null).show();
            return;
        }

        boolean paid = Salary.STATUS_PAID.equals(s.getStatus());
        b.setPositiveButton(paid ? "Mark UNPAID" : "Mark PAID", (d, w) -> {
            db.updateSalaryStatus(s.getId(), paid ? Salary.STATUS_UNPAID : Salary.STATUS_PAID);
            loadData();
        });
        if (!paid) {
            b.setNeutralButton("Recalculate", (d, w) -> {
                db.calculateSalary(s.getEmployeeId(), s.getMonth(), s.getYear());
                Toast.makeText(this, "Recalculated", Toast.LENGTH_SHORT).show();
                loadData();
            });
        }
        b.setNegativeButton("Delete", (d, w) -> new AlertDialog.Builder(this)
                .setMessage("Delete this salary record?")
                .setPositiveButton("Delete", (d2, w2) -> {
                    db.deleteSalary(s.getId());
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .show());
        b.show();
    }
}
