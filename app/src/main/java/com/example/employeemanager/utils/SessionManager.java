package com.example.employeemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.employeemanager.models.Account;

/**
 * Lưu thông tin tài khoản đăng nhập bằng SharedPreferences.
 */
public class SessionManager {
    private static final String PREF_NAME = "login_prefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_ACCOUNT_ID = "accountId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ROLE = "role";
    private static final String KEY_REMEMBER = "remember";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(Account account, boolean remember) {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putInt(KEY_ACCOUNT_ID, account.getId())
                .putString(KEY_USERNAME, account.getUsername())
                .putString(KEY_PASSWORD, remember ? account.getPassword() : "")
                .putString(KEY_ROLE, account.getRole())
                .putBoolean(KEY_REMEMBER, remember)
                .apply();
    }

    /** Đăng xuất: giữ lại username/password nếu người dùng chọn "Remember me". */
    public void logout() {
        SharedPreferences.Editor e = prefs.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_ACCOUNT_ID)
                .remove(KEY_ROLE);
        if (!isRemember()) {
            e.remove(KEY_USERNAME).remove(KEY_PASSWORD);
        }
        e.apply();
    }

    public boolean isLoggedIn() { return prefs.getBoolean(KEY_LOGGED_IN, false); }
    public int getAccountId() { return prefs.getInt(KEY_ACCOUNT_ID, -1); }
    public String getUsername() { return prefs.getString(KEY_USERNAME, ""); }
    public String getPassword() { return prefs.getString(KEY_PASSWORD, ""); }
    public String getRole() { return prefs.getString(KEY_ROLE, ""); }
    public boolean isRemember() { return prefs.getBoolean(KEY_REMEMBER, false); }
}
