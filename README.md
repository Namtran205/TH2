# Employee Working Hours and Salary Management

Android app (Java + SQLite) for managing employees, daily attendance and monthly salaries.

## Demo accounts (seeded on first run)

| Role     | Username | Password |
|----------|----------|----------|
| Admin    | admin    | admin123 |
| Employee | nv001    | 123456   |
| Employee | nv002    | 123456   |

`nv001` already has 4 attendance days in the **previous month** (one with no check-out) so the
salary calculation can be demonstrated right away: Admin → Salaries → enter last month → Calculate.

## Database (SQLite – `dal/DBHelper.java`)

- `Account(id, username, password, role)`
- `Employee(id, code, fullname, email, phone, hourlyRate, accountId)`
- `WorkLog(id, employeeId, workDate, checkInTime, checkOutTime, totalHours, status)`
- `Salary(id, employeeId, month, year, totalHours, overtimeHours, totalSalary, status)`

## Project structure

```
com.example.employeemanager
├── activities
│   ├── LoginActivity.java       login, 2 roles, "Remember me"
│   ├── AdminActivity.java       employee CRUD + search (Admin)
│   ├── SalaryActivity.java      salary management (Admin) / my salary (Employee)
│   ├── EmployeeActivity.java    check-in / check-out, today + month summary (Employee)
│   └── AttendanceActivity.java  daily history + monthly working-hour summary
├── dal
│   └── DBHelper.java            SQLite: tables, seed data, all queries
├── models
│   ├── Account.java
│   ├── Employee.java
│   ├── WorkLog.java
│   └── Salary.java
├── utils
│   ├── SessionManager.java      SharedPreferences login storage
│   └── TimeUtils.java           hours / overtime / salary formulas
└── MainActivity.java            reads SharedPreferences and routes by role
```

## Phân công công việc

Nhóm phân chia theo file để các thành viên có thể làm việc độc lập và hạn chế xung đột khi đẩy mã nguồn lên GitHub.

| Thành viên | Phần phụ trách | File phụ trách |
|------------|----------------|------------------|
| **Trịnh Thu Hồng** (Người 1) | Base: cấu hình dự án, SQLite Database, Login, SharedPreferences và các tiện ích dùng chung | Toàn bộ file Gradle, `.gitignore`, `README.md`, `AndroidManifest.xml`, `res/values/*`, `models/*`, `dal/DBHelper.java`, `utils/*`, `MainActivity.java`, `LoginActivity.java`, `activity_login.xml` |
| **Nguyễn Chí Ngọc** (Người 2) | Chức năng Admin: thêm, sửa, xóa, tìm kiếm nhân viên và quản lý bảng lương | `AdminActivity.java`, `activity_admin.xml`, `dialog_employee.xml`, `SalaryActivity.java`, `activity_salary.xml` |
| **Trần Hoàng Nam** (Người 3) | Chức năng Employee: check-in, check-out, xem lịch sử chấm công và tổng hợp giờ làm theo tháng | `EmployeeActivity.java`, `activity_employee.xml`, `AttendanceActivity.java`, `activity_attendance.xml` |

Mã Java của các màn hình nằm trong `app/src/main/java/com/example/employeemanager/activities/`; các layout tương ứng nằm trong `app/src/main/res/layout/`.

## Business rules

- **Working hours** = checkOutTime − checkInTime (hours, 2 decimals).
- **Overtime**: hours above **8h per day**, paid at **x1.5** (`TimeUtils.STANDARD_HOURS_PER_DAY`, `OVERTIME_RATE`).
- **Salary** = regularHours × hourlyRate + overtimeHours × hourlyRate × 1.5.
- Salary is recalculated automatically after every check-out (current month). Admin can calculate any month for all employees, mark PAID/UNPAID, recalculate or delete. PAID records are never overwritten.

## Data validation

- Only one check-in per employee per day (checked in code + `UNIQUE(employeeId, workDate)` in DB).
- Cannot check out without checking in, check out twice, or check out at a time ≤ check-in time.
- A day checked in but not checked out is marked `INCOMPLETE` the next day and is **not paid**.
- Employee form: required fields, unique code and username, valid email, phone (0 + 9–10 digits), hourly rate > 0.

## Run

Open the folder in Android Studio (Koala or newer) → let Gradle sync → Run on an emulator/device (API 24+).

## GitHub collaboration

```bash
git init
git add .
git commit -m "Initial commit: employee hours & salary management"
git branch -M main
git remote add origin https://github.com/<your-account>/EmployeeManager.git
git push -u origin main
```

Team members: `git clone` the repo, create a branch per feature
(`git checkout -b feature/attendance`), commit, push and open a Pull Request into `main`.
