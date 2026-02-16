package com.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Student model — represents a registered user of the system.
 * Contains personal info, semester dates, subjects, and holidays.
 */
public class Student {
    private int id; // Database primary key
    private String name;
    private String username;
    private List<Subject> subjects;
    private List<Holiday> holidays;

    // Semester date fields (4 dates for accurate calculation)
    private LocalDate semesterStartDate;
    private LocalDate midsemExamStartDate; // Classes pause
    private LocalDate midsemExamEndDate; // Classes resume
    private LocalDate semesterEndDate; // Last teaching day (before end-sem exams)

    public Student(String name) {
        this.name = name;
        this.subjects = new ArrayList<>();
        this.holidays = new ArrayList<>();
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

    // ── Username ──
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // ── Subjects ──
    public void addSubject(Subject subject) {
        subjects.add(subject);
    }

    public void removeSubject(Subject subject) {
        subjects.remove(subject);
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    // ── Holidays ──
    public void addHoliday(Holiday holiday) {
        if (!holidays.contains(holiday)) {
            holidays.add(holiday);
        }
    }

    public void removeHoliday(Holiday holiday) {
        holidays.remove(holiday);
    }

    public void removeHolidayByDate(LocalDate date) {
        holidays.removeIf(h -> h.getDate().equals(date));
    }

    public void removeHolidaysByDescription(String description) {
        holidays.removeIf(h -> h.getDescription().equals(description));
    }

    public void updateHoliday(LocalDate oldDate, LocalDate newDate, String newDescription) {
        for (Holiday h : holidays) {
            if (h.getDate().equals(oldDate)) {
                h.setDate(newDate);
                h.setDescription(newDescription);
                break;
            }
        }
    }

    public List<Holiday> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<Holiday> holidays) {
        this.holidays = holidays;
    }

    /**
     * Convenience method: returns just the dates for backward compatibility
     * with AttendanceCalculator, PredictionDialog, etc.
     */
    public List<LocalDate> getHolidayDates() {
        List<LocalDate> dates = new ArrayList<>();
        for (Holiday h : holidays) {
            dates.add(h.getDate());
        }
        return dates;
    }

    // ── Semester Dates ──
    public void setSemesterStartDate(LocalDate date) {
        this.semesterStartDate = date;
    }

    public LocalDate getSemesterStartDate() {
        return semesterStartDate;
    }

    public void setSemesterEndDate(LocalDate date) {
        this.semesterEndDate = date;
    }

    public LocalDate getSemesterEndDate() {
        return semesterEndDate;
    }

    public void setMidsemExamStartDate(LocalDate date) {
        this.midsemExamStartDate = date;
    }

    public LocalDate getMidsemExamStartDate() {
        return midsemExamStartDate;
    }

    public void setMidsemExamEndDate(LocalDate date) {
        this.midsemExamEndDate = date;
    }

    public LocalDate getMidsemExamEndDate() {
        return midsemExamEndDate;
    }

    /**
     * Check if semester dates have been configured.
     */
    public boolean isSemesterConfigured() {
        return semesterStartDate != null && semesterEndDate != null;
    }

    /**
     * Check if a given date falls within the mid-sem exam period (no classes).
     */
    public boolean isDuringMidsemExams(LocalDate date) {
        if (midsemExamStartDate == null || midsemExamEndDate == null) {
            return false;
        }
        return !date.isBefore(midsemExamStartDate) && !date.isAfter(midsemExamEndDate);
    }
}
