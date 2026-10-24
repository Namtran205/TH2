package com.example.employeemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.employeemanager.R;
import com.example.employeemanager.dal.DBHelper;
import com.example.employeemanager.models.Account;
import com.example.employeemanager.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private EditText edtUsername, edtPassword;
    private CheckBox chkRemember;
    private DBHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle("Login");

        db = DBHelper.getInstance(this);
        session = new SessionManager(this);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        chkRemember = findViewById(R.id.chkRemember);
        Button btnLogin = findViewById(R.id.btnLogin);

        // Tự điền lại thông tin đã lưu trong SharedPreferences
        if (session.isRemember()) {
            edtUsername.setText(session.getUsername());
            edtPassword.setText(session.getPassword());
            chkRemember.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString();

        if (username.isEmpty()) {
            edtUsername.setError("Username is required");
            return;
        }
        if (password.isEmpty()) {
            edtPassword.setError("Password is required");
            return;
        }

        Account acc = db.login(username, password);
        if (acc == null) {
            Toast.makeText(this, "Wrong username or password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!acc.isAdmin() && db.getEmployeeByAccountId(acc.getId()) == null) {
            Toast.makeText(this, "This account is not linked to any employee", Toast.LENGTH_SHORT).show();
            return;
        }

        session.saveLogin(acc, chkRemember.isChecked());
        Toast.makeText(this, "Welcome " + acc.getUsername(), Toast.LENGTH_SHORT).show();

        Intent i = new Intent(this, acc.isAdmin() ? AdminActivity.class : EmployeeActivity.class);
        startActivity(i);
        finish();
    }
}
