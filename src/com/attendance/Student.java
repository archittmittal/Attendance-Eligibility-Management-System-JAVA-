package com.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Student {
    private String name;
    private List<Subject> subjects;
    private List<LocalDate> holidays;

    public Student(String name) {
        this.name = name;
        this.subjects = new ArrayList<>();
        this.holidays = new ArrayList<>();
    }

    public void addSubject(Subject subject) {
        subjects.add(subject);
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void addHoliday(LocalDate date) {
        if (!holidays.contains(date)) {
            holidays.add(date);
        }
    }

    public List<LocalDate> getHolidays() {
        return holidays;
    }

    private LocalDate semesterEndDate;
    private LocalDate semesterStartDate;

    public void setSemesterEndDate(LocalDate date) {
        this.semesterEndDate = date;
    }

    public LocalDate getSemesterEndDate() {
        return semesterEndDate;
    }

    public void setSemesterStartDate(LocalDate date) {
        this.semesterStartDate = date;
    }

    public LocalDate getSemesterStartDate() {
        return semesterStartDate;
    }

    public String getName() {
        return name;
    }
}
