package com.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Subject model — represents a single course/subject.
 * Tracks attendance history through AttendanceRecord objects.
 */
public class Subject {
    private int id; // Database primary key
    private String name;
    private int classesPerWeek;
    private List<AttendanceRecord> attendanceHistory;

    public Subject(String name, int classesPerWeek) {
        this.name = name;
        this.classesPerWeek = classesPerWeek;
        this.attendanceHistory = new ArrayList<>();
    }

    // ── ID (Database) ──
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // ── Name ──
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // ── Attendance Stats ──
    public int getClassesConducted() {
        return attendanceHistory.size();
    }

    public int getClassesAttended() {
        int count = 0;
        for (AttendanceRecord record : attendanceHistory) {
            if (record.isPresent()) {
                count++;
            }
        }
        return count;
    }

    public int getClassesPerWeek() {
        return classesPerWeek;
    }

    public void setClassesPerWeek(int classesPerWeek) {
        this.classesPerWeek = classesPerWeek;
    }

    // ── Add Attendance ──

    /**
     * Add attendance for today.
     */
    public void addClass(boolean attended) {
        addClass(LocalDate.now(), attended);
    }

    /**
     * Add attendance for a specific date.
     * If a record for this date already exists, it gets updated instead.
     */
    public void addClass(LocalDate date, boolean attended) {
        for (AttendanceRecord record : attendanceHistory) {
            if (record.getDate() != null && record.getDate().equals(date)) {
                record.setPresent(attended); // Update existing
                return;
            }
        }
        attendanceHistory.add(new AttendanceRecord(date, attended));
    }

    /**
     * Remove attendance record for a specific date.
     */
    public void removeRecordForDate(LocalDate date) {
        attendanceHistory.removeIf(r -> r.getDate() != null && r.getDate().equals(date));
    }

    /**
     * Set initial attendance data (bulk, for starting mid-semester).
     */
    public void setAttendance(int conducted, int attended) {
        if (attended > conducted) {
            throw new IllegalArgumentException("Attended classes cannot be more than conducted classes.");
        }
        attendanceHistory.clear();

        for (int i = 0; i < attended; i++) {
            attendanceHistory.add(new AttendanceRecord(LocalDate.now().minusDays(conducted - i), true));
        }
        for (int i = 0; i < (conducted - attended); i++) {
            attendanceHistory.add(new AttendanceRecord(LocalDate.now().minusDays(i + 1), false));
        }
    }

    // ── Percentage ──
    public double getAttendancePercentage() {
        int conducted = getClassesConducted();
        if (conducted == 0)
            return 100.0;
        return (double) getClassesAttended() / conducted * 100.0;
    }

    // ── History ──
    public List<AttendanceRecord> getAttendanceHistory() {
        return new ArrayList<>(attendanceHistory);
    }

    /**
     * Check if attendance has already been marked for a specific date.
     */
    public boolean hasRecordForDate(LocalDate date) {
        for (AttendanceRecord record : attendanceHistory) {
            if (record.getDate() != null && record.getDate().equals(date)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s: %d/%d (%.2f%%)", name, getClassesAttended(), getClassesConducted(),
                getAttendancePercentage());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Subject other = (Subject) obj;
        if (id != 0 && other.id != 0)
            return id == other.id;
        return name != null && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return id != 0 ? Integer.hashCode(id) : (name != null ? name.hashCode() : 0);
    }
}
