package com.example.employeemanager.dal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.employeemanager.models.Account;
import com.example.employeemanager.models.Employee;
import com.example.employeemanager.models.Salary;
import com.example.employeemanager.models.WorkLog;
import com.example.employeemanager.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "employee_manager.db";
    private static final int DB_VERSION = 1;

    private static DBHelper instance;

    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) instance = new DBHelper(context.getApplicationContext());
        return instance;
    }

    private DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Account (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL)");

        db.execSQL("CREATE TABLE Employee (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "code TEXT NOT NULL UNIQUE, " +
                "fullname TEXT NOT NULL, " +
                "email TEXT, " +
                "phone TEXT, " +
                "hourlyRate REAL NOT NULL, " +
                "accountId INTEGER, " +
                "FOREIGN KEY(accountId) REFERENCES Account(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE WorkLog (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "employeeId INTEGER NOT NULL, " +
                "workDate TEXT NOT NULL, " +
                "checkInTime TEXT NOT NULL, " +
                "checkOutTime TEXT, " +
                "totalHours REAL DEFAULT 0, " +
                "status TEXT NOT NULL, " +
                "UNIQUE(employeeId, workDate), " + // chống check-in trùng ở mức DB
                "FOREIGN KEY(employeeId) REFERENCES Employee(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE Salary (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "employeeId INTEGER NOT NULL, " +
                "month INTEGER NOT NULL, " +
                "year INTEGER NOT NULL, " +
                "totalHours REAL DEFAULT 0, " +
                "overtimeHours REAL DEFAULT 0, " +
                "totalSalary REAL DEFAULT 0, " +
                "status TEXT NOT NULL, " +
                "UNIQUE(employeeId, month, year), " +
                "FOREIGN KEY(employeeId) REFERENCES Employee(id) ON DELETE CASCADE)");

        seedData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Salary");
        db.execSQL("DROP TABLE IF EXISTS WorkLog");
        db.execSQL("DROP TABLE IF EXISTS Employee");
        db.execSQL("DROP TABLE IF EXISTS Account");
        onCreate(db);
    }

    /** Dữ liệu mẫu: 1 admin, 2 nhân viên, vài ngày công tháng trước để demo tính lương. */
    private void seedData(SQLiteDatabase db) {
        insertAccount(db, "admin", "admin123", Account.ROLE_ADMIN);

        long acc1 = insertAccount(db, "nv001", "123456", Account.ROLE_EMPLOYEE);
        long emp1 = insertEmployee(db, "NV001", "Nguyen Van An", "an.nv@company.com",
                "0901234567", 50000, acc1);

        long acc2 = insertAccount(db, "nv002", "123456", Account.ROLE_EMPLOYEE);
        insertEmployee(db, "NV002", "Tran Thi Binh", "binh.tt@company.com",
                "0912345678", 45000, acc2);

        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        String prefix = TimeUtils.monthPrefix(c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
        insertSeedLog(db, emp1, prefix + "01", "08:00:00", "17:00:00");  // 9h  -> 1h OT
        insertSeedLog(db, emp1, prefix + "02", "08:00:00", "16:00:00");  // 8h
        insertSeedLog(db, emp1, prefix + "03", "08:00:00", "19:30:00");  // 11.5h -> 3.5h OT
        insertSeedLog(db, emp1, prefix + "04", "08:00:00", null);        // quên check-out
    }

    private long insertAccount(SQLiteDatabase db, String u, String p, String role) {
        ContentValues v = new ContentValues();
        v.put("username", u);
        v.put("password", p);
        v.put("role", role);
        return db.insert("Account", null, v);
    }

    private long insertEmployee(SQLiteDatabase db, String code, String name, String email,
                                String phone, double rate, long accountId) {
        ContentValues v = new ContentValues();
        v.put("code", code);
        v.put("fullname", name);
        v.put("email", email);
        v.put("phone", phone);
        v.put("hourlyRate", rate);
        v.put("accountId", accountId);
        return db.insert("Employee", null, v);
    }

    private void insertSeedLog(SQLiteDatabase db, long empId, String date, String in, String out) {
        ContentValues v = new ContentValues();
        v.put("employeeId", empId);
        v.put("workDate", date);
        v.put("checkInTime", in);
        if (out != null) {
            v.put("checkOutTime", out);
            v.put("totalHours", TimeUtils.hoursBetween(in, out));
            v.put("status", WorkLog.STATUS_COMPLETED);
        } else {
            v.put("totalHours", 0);
            v.put("status", WorkLog.STATUS_INCOMPLETE);
        }
        db.insert("WorkLog", null, v);
    }

    // =====================================================================
    // ACCOUNT
    // =====================================================================

    public Account login(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, username, password, role FROM Account WHERE username = ? AND password = ?",
                new String[]{username, password})) {
            if (c.moveToFirst()) {
                return new Account(c.getInt(0), c.getString(1), c.getString(2), c.getString(3));
            }
        }
        return null;
    }

    public boolean isUsernameExists(String username, int excludeAccountId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM Account WHERE username = ? AND id <> ?",
                new String[]{username, String.valueOf(excludeAccountId)})) {
            return c.moveToFirst();
        }
    }

    // =====================================================================
    // EMPLOYEE
    // =====================================================================

    private static final String EMP_SELECT =
            "SELECT e.id, e.code, e.fullname, e.email, e.phone, e.hourlyRate, e.accountId, a.username " +
            "FROM Employee e LEFT JOIN Account a ON e.accountId = a.id ";

    private Employee readEmployee(Cursor c) {
        Employee e = new Employee(c.getInt(0), c.getString(1), c.getString(2), c.getString(3),
                c.getString(4), c.getDouble(5), c.getInt(6));
        e.setUsername(c.getString(7));
        return e;
    }

    public List<Employee> getAllEmployees() {
        return searchEmployees("");
    }

    /** Tìm theo mã, họ tên, email hoặc số điện thoại. */
    public List<Employee> searchEmployees(String keyword) {
        List<Employee> list = new ArrayList<>();
        String k = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Cursor c = getReadableDatabase().rawQuery(EMP_SELECT +
                        "WHERE e.code LIKE ? OR e.fullname LIKE ? OR e.email LIKE ? OR e.phone LIKE ? " +
                        "ORDER BY e.code",
                new String[]{k, k, k, k})) {
            while (c.moveToNext()) list.add(readEmployee(c));
        }
        return list;
    }

    public Employee getEmployeeById(int id) {
        try (Cursor c = getReadableDatabase().rawQuery(EMP_SELECT + "WHERE e.id = ?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? readEmployee(c) : null;
        }
    }

    public Employee getEmployeeByAccountId(int accountId) {
        try (Cursor c = getReadableDatabase().rawQuery(EMP_SELECT + "WHERE e.accountId = ?",
                new String[]{String.valueOf(accountId)})) {
            return c.moveToFirst() ? readEmployee(c) : null;
        }
    }

    public boolean isCodeExists(String code, int excludeEmployeeId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM Employee WHERE code = ? AND id <> ?",
                new String[]{code, String.valueOf(excludeEmployeeId)})) {
            return c.moveToFirst();
        }
    }

    /** Thêm nhân viên kèm tài khoản đăng nhập (role EMPLOYEE). */
    public boolean addEmployee(Employee e, String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long accId = insertAccount(db, username, password, Account.ROLE_EMPLOYEE);
            if (accId == -1) return false;
            long empId = insertEmployee(db, e.getCode(), e.getFullname(), e.getEmail(),
                    e.getPhone(), e.getHourlyRate(), accId);
            if (empId == -1) return false;
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /** Cập nhật nhân viên; password rỗng = giữ nguyên mật khẩu cũ. */
    public boolean updateEmployee(Employee e, String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("code", e.getCode());
            v.put("fullname", e.getFullname());
            v.put("email", e.getEmail());
            v.put("phone", e.getPhone());
            v.put("hourlyRate", e.getHourlyRate());
            db.update("Employee", v, "id = ?", new String[]{String.valueOf(e.getId())});

            ContentValues a = new ContentValues();
            a.put("username", username);
            if (password != null && !password.isEmpty()) a.put("password", password);
            db.update("Account", a, "id = ?", new String[]{String.valueOf(e.getAccountId())});

            db.setTransactionSuccessful();
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /** Xoá nhân viên + tài khoản + chấm công + lương. */
    public boolean deleteEmployee(Employee e) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String[] id = {String.valueOf(e.getId())};
            db.delete("WorkLog", "employeeId = ?", id);
            db.delete("Salary", "employeeId = ?", id);
            db.delete("Employee", "id = ?", id);
            db.delete("Account", "id = ?", new String[]{String.valueOf(e.getAccountId())});
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // =====================================================================
    // WORKLOG (chấm công)
    // =====================================================================

    private WorkLog readWorkLog(Cursor c) {
        return new WorkLog(c.getInt(0), c.getInt(1), c.getString(2), c.getString(3),
                c.getString(4), c.getDouble(5), c.getString(6));
    }

    private static final String LOG_SELECT =
            "SELECT id, employeeId, workDate, checkInTime, checkOutTime, totalHours, status FROM WorkLog ";

    public WorkLog getWorkLog(int employeeId, String date) {
        try (Cursor c = getReadableDatabase().rawQuery(LOG_SELECT +
                        "WHERE employeeId = ? AND workDate = ?",
                new String[]{String.valueOf(employeeId), date})) {
            return c.moveToFirst() ? readWorkLog(c) : null;
        }
    }

    /** Lịch sử chấm công. month = 0 -> lấy tất cả. */
    public List<WorkLog> getWorkLogs(int employeeId, int month, int year) {
        List<WorkLog> list = new ArrayList<>();
        String datePattern = month == 0 ? "%" : TimeUtils.monthPrefix(month, year) + "%";
        try (Cursor c = getReadableDatabase().rawQuery(LOG_SELECT +
                        "WHERE employeeId = ? AND workDate LIKE ? ORDER BY workDate DESC",
                new String[]{String.valueOf(employeeId), datePattern})) {
            while (c.moveToNext()) list.add(readWorkLog(c));
        }
        return list;
    }

    /** Ngày cũ đã check-in mà không check-out -> đánh dấu INCOMPLETE (không tính lương). */
    public void markIncompleteLogs() {
        ContentValues v = new ContentValues();
        v.put("status", WorkLog.STATUS_INCOMPLETE);
        v.put("totalHours", 0);
        getWritableDatabase().update("WorkLog", v, "status = ? AND workDate < ?",
                new String[]{WorkLog.STATUS_CHECKED_IN, TimeUtils.today()});
    }

    /** @return null nếu thành công, ngược lại là thông báo lỗi. */
    public String checkIn(int employeeId) {
        markIncompleteLogs();
        String today = TimeUtils.today();
        WorkLog log = getWorkLog(employeeId, today);
        if (log != null) {
            if (WorkLog.STATUS_CHECKED_IN.equals(log.getStatus())) {
                return "You already checked in today at " + log.getCheckInTime() + ".";
            }
            return "You have already completed today's attendance.";
        }
        ContentValues v = new ContentValues();
        v.put("employeeId", employeeId);
        v.put("workDate", today);
        v.put("checkInTime", TimeUtils.nowTime());
        v.put("totalHours", 0);
        v.put("status", WorkLog.STATUS_CHECKED_IN);
        long id = getWritableDatabase().insert("WorkLog", null, v);
        return id == -1 ? "Check-in failed (duplicate record)." : null;
    }

    /** @return null nếu thành công, ngược lại là thông báo lỗi. */
    public String checkOut(int employeeId) {
        markIncompleteLogs();
        String today = TimeUtils.today();
        WorkLog log = getWorkLog(employeeId, today);
        if (log == null) return "You have not checked in today.";
        if (WorkLog.STATUS_COMPLETED.equals(log.getStatus())) {
            return "You already checked out today at " + log.getCheckOutTime() + ".";
        }
        if (!WorkLog.STATUS_CHECKED_IN.equals(log.getStatus())) {
            return "Today's attendance record is invalid.";
        }
        String now = TimeUtils.nowTime();
        double hours = TimeUtils.hoursBetween(log.getCheckInTime(), now);
        if (hours <= 0) {
            return "Invalid check-out time: it must be after check-in (" + log.getCheckInTime() + ").";
        }
        ContentValues v = new ContentValues();
        v.put("checkOutTime", now);
        v.put("totalHours", hours);
        v.put("status", WorkLog.STATUS_COMPLETED);
        getWritableDatabase().update("WorkLog", v, "id = ?", new String[]{String.valueOf(log.getId())});

        // Tự động cập nhật bảng lương tháng hiện tại
        calculateSalary(employeeId, TimeUtils.currentMonth(), TimeUtils.currentYear());
        return null;
    }

    // =====================================================================
    // SALARY
    // =====================================================================

    private Salary readSalary(Cursor c) {
        Salary s = new Salary(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3),
                c.getDouble(4), c.getDouble(5), c.getDouble(6), c.getString(7));
        if (c.getColumnCount() > 8) {
            s.setEmployeeCode(c.getString(8));
            s.setEmployeeName(c.getString(9));
        }
        return s;
    }

    private static final String SAL_SELECT =
            "SELECT s.id, s.employeeId, s.month, s.year, s.totalHours, s.overtimeHours, " +
            "s.totalSalary, s.status, e.code, e.fullname " +
            "FROM Salary s JOIN Employee e ON s.employeeId = e.id ";

    public Salary getSalary(int employeeId, int month, int year) {
        try (Cursor c = getReadableDatabase().rawQuery(SAL_SELECT +
                        "WHERE s.employeeId = ? AND s.month = ? AND s.year = ?",
                new String[]{String.valueOf(employeeId), String.valueOf(month), String.valueOf(year)})) {
            return c.moveToFirst() ? readSalary(c) : null;
        }
    }

    /**
     * Tính lương tháng cho 1 nhân viên từ các ngày công COMPLETED.
     * - Giờ > 8h/ngày là tăng ca, tính hệ số 1.5.
     * - Bảng lương đã PAID thì không tính lại.
     * @return bản ghi lương, hoặc null nếu tháng đó không có giờ làm.
     */
    public Salary calculateSalary(int employeeId, int month, int year) {
        Employee emp = getEmployeeById(employeeId);
        if (emp == null) return null;

        Salary existing = getSalary(employeeId, month, year);
        if (existing != null && Salary.STATUS_PAID.equals(existing.getStatus())) return existing;

        double total = 0, overtime = 0;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT totalHours FROM WorkLog WHERE employeeId = ? AND status = ? AND workDate LIKE ?",
                new String[]{String.valueOf(employeeId), WorkLog.STATUS_COMPLETED,
                        TimeUtils.monthPrefix(month, year) + "%"})) {
            while (c.moveToNext()) {
                double h = c.getDouble(0);
                total += h;
                overtime += TimeUtils.overtimeOfDay(h);
            }
        }
        total = TimeUtils.round2(total);
        overtime = TimeUtils.round2(overtime);
        if (total == 0 && existing == null) return null;

        double money = TimeUtils.calculateSalary(total, overtime, emp.getHourlyRate());
        ContentValues v = new ContentValues();
        v.put("employeeId", employeeId);
        v.put("month", month);
        v.put("year", year);
        v.put("totalHours", total);
        v.put("overtimeHours", overtime);
        v.put("totalSalary", money);
        v.put("status", Salary.STATUS_UNPAID);

        SQLiteDatabase db = getWritableDatabase();
        if (existing == null) {
            db.insert("Salary", null, v);
        } else {
            db.update("Salary", v, "id = ?", new String[]{String.valueOf(existing.getId())});
        }
        return getSalary(employeeId, month, year);
    }

    /** Tính lương tháng cho tất cả nhân viên. @return số bản ghi được tạo/cập nhật. */
    public int calculateAllSalaries(int month, int year) {
        markIncompleteLogs();
        int count = 0;
        for (Employee e : getAllEmployees()) {
            if (calculateSalary(e.getId(), month, year) != null) count++;
        }
        return count;
    }

    /** Danh sách bảng lương (admin). month = 0 -> tất cả. */
    public List<Salary> getSalaries(int month, int year) {
        List<Salary> list = new ArrayList<>();
        String sql = SAL_SELECT + (month == 0 ? "" : "WHERE s.month = ? AND s.year = ? ") +
                "ORDER BY s.year DESC, s.month DESC, e.code";
        String[] args = month == 0 ? null
                : new String[]{String.valueOf(month), String.valueOf(year)};
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) list.add(readSalary(c));
        }
        return list;
    }

    public List<Salary> getSalariesByEmployee(int employeeId) {
        List<Salary> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(SAL_SELECT +
                        "WHERE s.employeeId = ? ORDER BY s.year DESC, s.month DESC",
                new String[]{String.valueOf(employeeId)})) {
            while (c.moveToNext()) list.add(readSalary(c));
        }
        return list;
    }

    public void updateSalaryStatus(int salaryId, String status) {
        ContentValues v = new ContentValues();
        v.put("status", status);
        getWritableDatabase().update("Salary", v, "id = ?", new String[]{String.valueOf(salaryId)});
    }

    public void deleteSalary(int salaryId) {
        getWritableDatabase().delete("Salary", "id = ?", new String[]{String.valueOf(salaryId)});
    }

    public static String describeSalary(Salary s, Employee e) {
        return String.format(Locale.US,
                "Employee: %s - %s\nPeriod: %02d/%d\nHourly rate: %s\n\n" +
                        "Total hours: %s\nRegular hours: %s\nOvertime hours: %s (x%.1f)\n\n" +
                        "Total salary: %s\nStatus: %s",
                e.getCode(), e.getFullname(), s.getMonth(), s.getYear(),
                TimeUtils.money(e.getHourlyRate()) + "/h",
                TimeUtils.hours(s.getTotalHours()),
                TimeUtils.hours(TimeUtils.round2(s.getRegularHours())),
                TimeUtils.hours(s.getOvertimeHours()), TimeUtils.OVERTIME_RATE,
                TimeUtils.money(s.getTotalSalary()), s.getStatus());
    }
}
