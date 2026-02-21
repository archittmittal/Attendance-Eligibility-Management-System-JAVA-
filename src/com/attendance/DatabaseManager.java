package com.attendance;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton Database Manager for all JDBC operations.
 * Handles CRUD for students, subjects, attendance records, holidays, and
 * schedule.
 * Uses PreparedStatements to prevent SQL injection.
 * Uses try-with-resources for automatic resource management.
 */
public class DatabaseManager {

    // ───── Singleton Pattern ─────
    private static DatabaseManager instance;

    private DatabaseManager() {
        initializeTables();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // ───── Connection Management ─────

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.DB_URL,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD);
    }

    /**
     * Test database connectivity.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Auto-create tables if they don't exist (first run safety).
     */
    private void initializeTables() {
        String[] createStatements = {
                "CREATE TABLE IF NOT EXISTS students ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "name VARCHAR(100) NOT NULL, "
                        + "username VARCHAR(50) NOT NULL UNIQUE, "
                        + "password_hash VARCHAR(255) NOT NULL, "
                        + "semester_start_date DATE, "
                        + "midsem_exam_start_date DATE, "
                        + "midsem_exam_end_date DATE, "
                        + "last_teaching_day DATE, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ")",
                "CREATE TABLE IF NOT EXISTS subjects ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "student_id INT NOT NULL, "
                        + "name VARCHAR(100) NOT NULL, "
                        + "classes_per_week INT DEFAULT 0, "
                        + "FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE"
                        + ")",
                "CREATE TABLE IF NOT EXISTS attendance_records ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "subject_id INT NOT NULL, "
                        + "record_date DATE NOT NULL, "
                        + "is_present BOOLEAN NOT NULL, "
                        + "FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE, "
                        + "UNIQUE KEY unique_record (subject_id, record_date)"
                        + ")",
                "CREATE TABLE IF NOT EXISTS holidays ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "student_id INT NOT NULL, "
                        + "holiday_date DATE NOT NULL, "
                        + "description VARCHAR(200) DEFAULT 'Official Holiday', "
                        + "FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE, "
                        + "UNIQUE KEY unique_holiday (student_id, holiday_date)"
                        + ")",
                "CREATE TABLE IF NOT EXISTS weekly_schedule ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "subject_id INT NOT NULL, "
                        + "day_of_week VARCHAR(10) NOT NULL, "
                        + "FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE"
                        + ")"
        };

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            for (String sql : createStatements) {
                stmt.executeUpdate(sql);
            }
            // Add theme column if it doesn't exist (migration for existing DBs)
            try {
                stmt.executeUpdate(
                        "ALTER TABLE students ADD COLUMN theme VARCHAR(10) DEFAULT 'dark'");
            } catch (SQLException ignored) {
                // Column already exists — safe to ignore
            }
        } catch (SQLException e) {
            System.err.println("Error initializing tables: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════
    // STUDENT CRUD
    // ══════════════════════════════════════════════

    /**
     * Register a new student. Returns the generated student ID, or -1 on
     * failure.
     */
    public int registerStudent(String name, String username, String passwordHash) {
        String sql = "INSERT INTO students (name, username, password_hash) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, username);
            pstmt.setString(3, passwordHash);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error registering student: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Authenticate a student by username and plaintext password.
     * Fetches the stored hash, then verifies in Java (supports salted hashes).
     * Automatically upgrades legacy unsalted hashes on successful login.
     *
     * @param username the username
     * @param password the plaintext password (NOT a hash)
     * @return a fully loaded Student object, or null if login fails
     */
    public Student authenticateStudent(String username, String password) {
        String sql = "SELECT * FROM students WHERE username = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    // Verify password against stored hash (salted or legacy)
                    if (!PasswordValidator.checkPassword(password, storedHash)) {
                        return null; // Wrong password
                    }

                    Student student = new Student(rs.getString("name"));
                    student.setId(rs.getInt("id"));
                    student.setUsername(rs.getString("username"));

                    // Load semester dates
                    Date d = rs.getDate("semester_start_date");
                    if (d != null)
                        student.setSemesterStartDate(d.toLocalDate());
                    d = rs.getDate("midsem_exam_start_date");
                    if (d != null)
                        student.setMidsemExamStartDate(d.toLocalDate());
                    d = rs.getDate("midsem_exam_end_date");
                    if (d != null)
                        student.setMidsemExamEndDate(d.toLocalDate());
                    d = rs.getDate("last_teaching_day");
                    if (d != null)
                        student.setSemesterEndDate(d.toLocalDate());

                    // Auto-upgrade legacy unsalted hash to salted
                    if (PasswordValidator.isLegacyHash(storedHash)) {
                        String upgradedHash = PasswordValidator.hashPassword(password);
                        updatePasswordHash(student.getId(), upgradedHash);
                    }

                    return student;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating: " + e.getMessage());
        }
        return null;
    }

    /**
     * Update a student's password hash (used for hash upgrades and password
     * changes).
     */
    public void updatePasswordHash(int studentId, String newHash) {
        String sql = "UPDATE students SET password_hash = ? WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newHash);
            pstmt.setInt(2, studentId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating password hash: " + e.getMessage());
        }
    }

    /**
     * Check if a username already exists.
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM students WHERE username = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking username: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get the stored password hash for a student.
     */
    public String getPasswordHash(int studentId) {
        String sql = "SELECT password_hash FROM students WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting password hash: " + e.getMessage());
        }
        return null;
    }

    /**
     * Save semester settings for a student.
     */
    public void saveSemesterSettings(Student student) {
        String sql = "UPDATE students SET semester_start_date = ?, midsem_exam_start_date = ?, "
                + "midsem_exam_end_date = ?, last_teaching_day = ? WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, student.getSemesterStartDate() != null
                    ? Date.valueOf(student.getSemesterStartDate())
                    : null);
            pstmt.setDate(2, student.getMidsemExamStartDate() != null
                    ? Date.valueOf(student.getMidsemExamStartDate())
                    : null);
            pstmt.setDate(3, student.getMidsemExamEndDate() != null
                    ? Date.valueOf(student.getMidsemExamEndDate())
                    : null);
            pstmt.setDate(4, student.getSemesterEndDate() != null
                    ? Date.valueOf(student.getSemesterEndDate())
                    : null);
            pstmt.setInt(5, student.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving semester settings: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════
    // SUBJECT CRUD
    // ══════════════════════════════════════════════

    /**
     * Add a new subject for a student. Returns the generated subject ID.
     */
    public int addSubject(int studentId, String name, int classesPerWeek) {
        String sql = "INSERT INTO subjects (student_id, name, classes_per_week) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, studentId);
            pstmt.setString(2, name);
            pstmt.setInt(3, classesPerWeek);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding subject: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Update a subject's name and classes per week.
     */
    public void updateSubject(int subjectId, String newName, int classesPerWeek) {
        String sql = "UPDATE subjects SET name = ?, classes_per_week = ? WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setInt(2, classesPerWeek);
            pstmt.setInt(3, subjectId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating subject: " + e.getMessage());
        }
    }

    /**
     * Delete a subject (cascades to attendance records and schedule).
     */
    public void deleteSubject(int subjectId) {
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, subjectId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting subject: " + e.getMessage());
        }
    }

    /**
     * Load all subjects for a student (with their attendance records).
     */
    public List<Subject> loadSubjects(int studentId) {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT * FROM subjects WHERE student_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Subject subject = new Subject(rs.getString("name"), rs.getInt("classes_per_week"));
                    subject.setId(rs.getInt("id"));

                    // Load attendance records for this subject
                    loadAttendanceRecords(conn, subject);

                    subjects.add(subject);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading subjects: " + e.getMessage());
        }
        return subjects;
    }

    // ══════════════════════════════════════════════
    // ATTENDANCE RECORDS
    // ══════════════════════════════════════════════

    /**
     * Save a single attendance record.
     * Uses INSERT ... ON DUPLICATE KEY UPDATE to handle re-marking.
     */
    public void saveAttendanceRecord(int subjectId, LocalDate date, boolean present) {
        String sql = "INSERT INTO attendance_records (subject_id, record_date, is_present) "
                + "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE is_present = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, subjectId);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.setBoolean(3, present);
            pstmt.setBoolean(4, present); // for the UPDATE part
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving attendance: " + e.getMessage());
        }
    }

    /**
     * Load attendance records for a subject (called internally).
     */
    private void loadAttendanceRecords(Connection conn, Subject subject) throws SQLException {
        String sql = "SELECT * FROM attendance_records WHERE subject_id = ? ORDER BY record_date";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, subject.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("record_date").toLocalDate();
                    boolean present = rs.getBoolean("is_present");
                    subject.addClass(date, present);
                }
            }
        }
    }

    /**
     * Delete attendance record for a specific date.
     */
    public void deleteAttendanceRecord(int subjectId, LocalDate date) {
        String sql = "DELETE FROM attendance_records WHERE subject_id = ? AND record_date = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, subjectId);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting attendance record: " + e.getMessage());
        }
    }

    /**
     * Delete ALL attendance records for a student on a specific date.
     * Used when a holiday is added retroactively — clears conflicting records.
     * Returns the number of records deleted.
     */
    public int deleteAttendanceOnDate(int studentId, LocalDate date) {
        String sql = "DELETE ar FROM attendance_records ar "
                + "JOIN subjects s ON ar.subject_id = s.id "
                + "WHERE s.student_id = ? AND ar.record_date = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setDate(2, Date.valueOf(date));
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting attendance on holiday: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Delete ALL attendance records for a student within a date range.
     * Used when a group holiday is added retroactively.
     * Returns the number of records deleted.
     */
    public int deleteAttendanceInRange(int studentId, LocalDate fromDate, LocalDate toDate) {
        String sql = "DELETE ar FROM attendance_records ar "
                + "JOIN subjects s ON ar.subject_id = s.id "
                + "WHERE s.student_id = ? AND ar.record_date BETWEEN ? AND ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setDate(2, Date.valueOf(fromDate));
            pstmt.setDate(3, Date.valueOf(toDate));
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting attendance in range: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Bulk save attendance records (for setting initial data).
     * Places records ONLY on scheduled weekdays, starting from semester start
     * date,
     * skipping holidays and mid-sem exam periods.
     */
    public void saveInitialAttendance(int subjectId, int conducted, int attended,
            List<java.time.DayOfWeek> scheduledDays, Student student) {
        // Clear existing records first
        String deleteSql = "DELETE FROM attendance_records WHERE subject_id = ?";
        String insertSql = "INSERT INTO attendance_records (subject_id, record_date, is_present) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE is_present = VALUES(is_present)";

        // Determine start date: semester start or fallback to today minus a safe window
        LocalDate startDate;
        if (student != null && student.getSemesterStartDate() != null) {
            startDate = student.getSemesterStartDate();
        } else {
            // Fallback: go back enough days to fit all conducted classes
            startDate = LocalDate.now().minusDays((long) conducted * 7 / Math.max(scheduledDays.size(), 1) + 14);
        }

        // Collect valid class dates from startDate to today on scheduled weekdays only
        List<LocalDate> validDates = new ArrayList<>();
        LocalDate cursor = startDate;
        LocalDate today = LocalDate.now();
        List<LocalDate> holidayDates = (student != null) ? student.getHolidayDates() : new ArrayList<>();

        while (!cursor.isAfter(today) && validDates.size() < conducted) {
            // Only on scheduled weekdays
            if (scheduledDays.contains(cursor.getDayOfWeek())) {
                // Skip holidays
                if (!holidayDates.contains(cursor)) {
                    // Skip mid-sem exam period
                    if (student == null || !student.isDuringMidsemExams(cursor)) {
                        validDates.add(cursor);
                    }
                }
            }
            cursor = cursor.plusDays(1);
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Transaction

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, subjectId);
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                // First 'attended' dates are present, rest are absent
                for (int i = 0; i < validDates.size(); i++) {
                    insertStmt.setInt(1, subjectId);
                    insertStmt.setDate(2, Date.valueOf(validDates.get(i)));
                    insertStmt.setBoolean(3, i < attended); // first N are present
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error saving initial attendance: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════
    // HOLIDAYS
    // ══════════════════════════════════════════════

    /**
     * Add a holiday for a student.
     */
    public void addHoliday(int studentId, LocalDate date, String description) {
        String sql = "INSERT INTO holidays (student_id, holiday_date, description) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE description = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.setString(3, description);
            pstmt.setString(4, description);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding holiday: " + e.getMessage());
        }
    }

    /**
     * Add a group holiday (date range) for a student.
     * Inserts one row per date from 'fromDate' to 'toDate' (inclusive) with the
     * same description.
     */
    public void addHolidayRange(int studentId, LocalDate fromDate, LocalDate toDate, String description) {
        String sql = "INSERT INTO holidays (student_id, holiday_date, description) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE description = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            LocalDate current = fromDate;
            while (!current.isAfter(toDate)) {
                pstmt.setInt(1, studentId);
                pstmt.setDate(2, Date.valueOf(current));
                pstmt.setString(3, description);
                pstmt.setString(4, description);
                pstmt.addBatch();
                current = current.plusDays(1);
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error adding holiday range: " + e.getMessage());
        }
    }

    /**
     * Remove a holiday.
     */
    public void removeHoliday(int studentId, LocalDate date) {
        String sql = "DELETE FROM holidays WHERE student_id = ? AND holiday_date = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing holiday: " + e.getMessage());
        }
    }

    /**
     * Remove all holidays with a given description (group removal).
     */
    public void removeHolidaysByDescription(int studentId, String description) {
        String sql = "DELETE FROM holidays WHERE student_id = ? AND description = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setString(2, description);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing holidays by description: " + e.getMessage());
        }
    }

    /**
     * Update a holiday's date and/or description.
     */
    public void updateHoliday(int studentId, LocalDate oldDate, LocalDate newDate, String newDescription) {
        String sql = "UPDATE holidays SET holiday_date = ?, description = ? WHERE student_id = ? AND holiday_date = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(newDate));
            pstmt.setString(2, newDescription);
            pstmt.setInt(3, studentId);
            pstmt.setDate(4, Date.valueOf(oldDate));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating holiday: " + e.getMessage());
        }
    }

    /**
     * Load all holidays for a student (with descriptions).
     */
    public List<Holiday> loadHolidays(int studentId) {
        List<Holiday> holidays = new ArrayList<>();
        String sql = "SELECT holiday_date, description FROM holidays WHERE student_id = ? ORDER BY holiday_date";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("holiday_date").toLocalDate();
                    String desc = rs.getString("description");
                    holidays.add(new Holiday(date, desc));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading holidays: " + e.getMessage());
        }
        return holidays;
    }

    // ══════════════════════════════════════════════
    // WEEKLY SCHEDULE
    // ══════════════════════════════════════════════

    /**
     * Save the weekly schedule for a subject (replaces existing).
     */
    public void saveSchedule(int subjectId, List<DayOfWeek> days) {
        String deleteSql = "DELETE FROM weekly_schedule WHERE subject_id = ?";
        String insertSql = "INSERT INTO weekly_schedule (subject_id, day_of_week) VALUES (?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, subjectId);
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (DayOfWeek day : days) {
                    insertStmt.setInt(1, subjectId);
                    insertStmt.setString(2, day.name());
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error saving schedule: " + e.getMessage());
        }
    }

    /**
     * Load weekly schedule for a student's subjects.
     */
    public void loadSchedule(WeeklySchedule schedule, List<Subject> subjects) {
        String sql = "SELECT day_of_week FROM weekly_schedule WHERE subject_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Subject subject : subjects) {
                pstmt.setInt(1, subject.getId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        DayOfWeek day = DayOfWeek.valueOf(rs.getString("day_of_week"));
                        schedule.addClass(day, subject);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading schedule: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════
    // THEME PERSISTENCE
    // ══════════════════════════════════════════════

    /**
     * Load the theme preference for a student.
     * 
     * @return "dark" or "light" (defaults to "dark")
     */
    public String loadTheme(int studentId) {
        String sql = "SELECT theme FROM students WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String theme = rs.getString("theme");
                    return theme != null ? theme : "dark";
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading theme: " + e.getMessage());
        }
        return "dark";
    }

    /**
     * Save the theme preference for a student.
     */
    public void saveTheme(int studentId, String theme) {
        String sql = "UPDATE students SET theme = ? WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, theme);
            pstmt.setInt(2, studentId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving theme: " + e.getMessage());
        }
    }
}
