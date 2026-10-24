package com.example.employeemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.employeemanager.R;
import com.example.employeemanager.dal.DBHelper;
import com.example.employeemanager.models.Employee;
import com.example.employeemanager.utils.SessionManager;
import com.example.employeemanager.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin: quản lý nhân viên (thêm, sửa, xoá, tìm kiếm, xem) + vào quản lý lương.
 */
public class AdminActivity extends AppCompatActivity {
    private DBHelper db;
    private SessionManager session;
    private EditText edtSearch;
    private TextView tvCount;
    private ArrayAdapter<Employee> adapter;
    private final List<Employee> employees = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        setTitle("Employee Management");

        db = DBHelper.getInstance(this);
        session = new SessionManager(this);

        ((TextView) findViewById(R.id.tvAdminInfo)).setText("Logged in as: " + session.getUsername() + " (Admin)");
        edtSearch = findViewById(R.id.edtSearch);
        tvCount = findViewById(R.id.tvCount);
        ListView lv = findViewById(R.id.lvEmployees);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, employees);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((p, v, pos, id) -> showEmployeeOptions(employees.get(pos)));

        findViewById(R.id.btnSearch).setOnClickListener(v -> loadEmployees());
        edtSearch.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadEmployees();
                return true;
            }
            return false;
        });
        findViewById(R.id.btnAdd).setOnClickListener(v -> showEmployeeDialog(null));
        findViewById(R.id.btnSalary).setOnClickListener(v ->
                startActivity(new Intent(this, SalaryActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEmployees();
    }

    private void loadEmployees() {
        employees.clear();
        employees.addAll(db.searchEmployees(edtSearch.getText().toString()));
        adapter.notifyDataSetChanged();
        tvCount.setText(employees.size() + " employee(s) — tap an item to view / edit / delete");
    }

    private void showEmployeeOptions(Employee e) {
        String info = e.getCode() + " - " + e.getFullname()
                + "\nEmail: " + e.getEmail()
                + "\nPhone: " + e.getPhone()
                + "\nHourly rate: " + TimeUtils.money(e.getHourlyRate()) + "/h"
                + "\nUsername: " + e.getUsername();

        new AlertDialog.Builder(this)
                .setTitle("Employee details")
                .setMessage(info)
                .setPositiveButton("Edit", (d, w) -> showEmployeeDialog(e))
                .setNegativeButton("Delete", (d, w) -> confirmDelete(e))
                .setNeutralButton("Attendance", (d, w) -> {
                    Intent i = new Intent(this, AttendanceActivity.class);
                    i.putExtra(AttendanceActivity.EXTRA_EMPLOYEE_ID, e.getId());
                    startActivity(i);
                })
                .show();
    }

    private void confirmDelete(Employee e) {
        new AlertDialog.Builder(this)
                .setTitle("Delete employee")
                .setMessage("Delete " + e.getCode() + " - " + e.getFullname()
                        + "?\nAccount, attendance and salary records will also be deleted.")
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteEmployee(e);
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadEmployees();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** emp == null -> thêm mới, ngược lại -> sửa. */
    private void showEmployeeDialog(Employee emp) {
        View view = getLayoutInflater().inflate(R.layout.dialog_employee, null);
        EditText edtCode = view.findViewById(R.id.edtCode);
        EditText edtName = view.findViewById(R.id.edtFullname);
        EditText edtEmail = view.findViewById(R.id.edtEmail);
        EditText edtPhone = view.findViewById(R.id.edtPhone);
        EditText edtRate = view.findViewById(R.id.edtHourlyRate);
        EditText edtUser = view.findViewById(R.id.edtAccUsername);
        EditText edtPass = view.findViewById(R.id.edtAccPassword);

        boolean isEdit = emp != null;
        if (isEdit) {
            edtCode.setText(emp.getCode());
            edtName.setText(emp.getFullname());
            edtEmail.setText(emp.getEmail());
            edtPhone.setText(emp.getPhone());
            edtRate.setText(String.valueOf((long) emp.getHourlyRate()));
            edtUser.setText(emp.getUsername());
            edtPass.setHint("New password (leave blank to keep)");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Update employee" : "Add employee")
                .setView(view)
                .setPositiveButton(isEdit ? "Update" : "Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        // Ghi đè nút để dialog không tự đóng khi dữ liệu không hợp lệ
        dialog.setOnShowListener(d -> {
            Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btn.setOnClickListener(v -> {
                String code = edtCode.getText().toString().trim().toUpperCase();
                String name = edtName.getText().toString().trim();
                String email = edtEmail.getText().toString().trim();
                String phone = edtPhone.getText().toString().trim();
                String rateStr = edtRate.getText().toString().trim();
                String user = edtUser.getText().toString().trim();
                String pass = edtPass.getText().toString();
                int empId = isEdit ? emp.getId() : -1;
                int accId = isEdit ? emp.getAccountId() : -1;

                if (TextUtils.isEmpty(code)) { edtCode.setError("Required"); return; }
                if (db.isCodeExists(code, empId)) { edtCode.setError("Code already exists"); return; }
                if (TextUtils.isEmpty(name)) { edtName.setError("Required"); return; }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    edtEmail.setError("Invalid email"); return;
                }
                if (!phone.matches("^0\\d{9,10}$")) {
                    edtPhone.setError("Phone must start with 0 and have 10-11 digits"); return;
                }
                double rate;
                try {
                    rate = Double.parseDouble(rateStr);
                } catch (NumberFormatException ex) {
                    edtRate.setError("Invalid number"); return;
                }
                if (rate <= 0) { edtRate.setError("Hourly rate must be > 0"); return; }
                if (TextUtils.isEmpty(user)) { edtUser.setError("Required"); return; }
                if (db.isUsernameExists(user, accId)) { edtUser.setError("Username already exists"); return; }
                if (!isEdit && pass.length() < 4) { edtPass.setError("At least 4 characters"); return; }
                if (isEdit && !pass.isEmpty() && pass.length() < 4) {
                    edtPass.setError("At least 4 characters"); return;
                }

                Employee e = isEdit ? emp : new Employee();
                e.setCode(code);
                e.setFullname(name);
                e.setEmail(email);
                e.setPhone(phone);
                e.setHourlyRate(rate);

                boolean ok = isEdit ? db.updateEmployee(e, user, pass) : db.addEmployee(e, user, pass);
                Toast.makeText(this, ok ? (isEdit ? "Updated" : "Added") : "Save failed",
                        Toast.LENGTH_SHORT).show();
                if (ok) {
                    dialog.dismiss();
                    loadEmployees();
                }
            });
        });
        dialog.show();
    }

    private void logout() {
        session.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
