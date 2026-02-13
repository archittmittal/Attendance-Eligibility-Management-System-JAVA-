package com.attendance;

import java.time.LocalDate;

public class AttendanceRecord {
    private LocalDate date;
    private boolean present;

    public AttendanceRecord(LocalDate date, boolean present) {
        this.date = date;
        this.present = present;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isPresent() {
        return present;
    }

    @Override
    public String toString() {
        return date + ": " + (present ? "Present" : "Absent");
    }
}
