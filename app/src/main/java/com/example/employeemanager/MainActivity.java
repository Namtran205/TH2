package com.example.employeemanager;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.employeemanager.activities.AdminActivity;
import com.example.employeemanager.activities.EmployeeActivity;
import com.example.employeemanager.activities.LoginActivity;
import com.example.employeemanager.models.Account;
import com.example.employeemanager.utils.SessionManager;

/**
 * Màn hình khởi động: đọc SharedPreferences, nếu đã đăng nhập thì vào thẳng màn hình theo role.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);

        Class<?> target = LoginActivity.class;
        if (session.isLoggedIn()) {
            target = Account.ROLE_ADMIN.equals(session.getRole())
                    ? AdminActivity.class : EmployeeActivity.class;
        }
        startActivity(new Intent(this, target));
        finish();
    }
}
