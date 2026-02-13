package com.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Subject {
    private String name;
    private int classesPerWeek; // Used for future predictions

    // Legacy counters are now derived, but we keep them for code compatibility if
    // needed,
    // though it's better to calculate them on the fly.
    // However, to avoid breaking too much existing code that might rely on
    // setAttendance(int, int),
    // we will maintain the list as the source of truth for NEW operations.

    private List<AttendanceRecord> attendanceHistory;

    public Subject(String name, int classesPerWeek) {
        this.name = name;
        this.classesPerWeek = classesPerWeek;
        this.attendanceHistory = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

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

    // Overloaded method for backward compatibility / simple increment
    public void addClass(boolean attended) {
        addClass(LocalDate.now(), attended);
    }

    public void addClass(LocalDate date, boolean attended) {
        // Validation: duplicate check
        // For now, we will allow multiple records for same date (e.g. multiple
        // lectures)
        // or we could replace. Let's append for now.
        attendanceHistory.add(new AttendanceRecord(date, attended));
    }

    // This method is used for setting initial/legacy data where we don't have
    // dates.
    // We will simulate it by adding undated records or just records with null
    // date/past date?
    // Actually, create dummy records to match the count.
    public void setAttendance(int conducted, int attended) {
        if (attended > conducted) {
            throw new IllegalArgumentException("Attended classes cannot be more than conducted classes.");
        }
        attendanceHistory.clear();

        // Add attended records
        for (int i = 0; i < attended; i++) {
            attendanceHistory.add(new AttendanceRecord(LocalDate.now().minusDays(conducted - i), true));
        }
        // Add missed records
        for (int i = 0; i < (conducted - attended); i++) {
            attendanceHistory.add(new AttendanceRecord(LocalDate.now().minusDays(i + 1), false));
        }
    }

    public double getAttendancePercentage() {
        int conducted = getClassesConducted();
        if (conducted == 0)
            return 100.0; // Default to 100% if no classes
        return (double) getClassesAttended() / conducted * 100.0;
    }

    public List<AttendanceRecord> getAttendanceHistory() {
        return new ArrayList<>(attendanceHistory);
    }

    @Override
    public String toString() {
        return String.format("%s: %d/%d (%.2f%%)", name, getClassesAttended(), getClassesConducted(),
                getAttendancePercentage());
    }
}
