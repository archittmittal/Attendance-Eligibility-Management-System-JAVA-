package com.attendance;

import java.time.LocalDate;

/**
 * Represents a single holiday date with a description.
 * Used to pair a date with the reason for the holiday (e.g. "Holi Break").
 */
public class Holiday {
    private LocalDate date;
    private String description;

    public Holiday(LocalDate date, String description) {
        this.date = date;
        this.description = (description != null && !description.isEmpty()) ? description : "Official Holiday";
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setDescription(String description) {
        this.description = (description != null && !description.isEmpty()) ? description : "Official Holiday";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Holiday holiday = (Holiday) o;
        return date.equals(holiday.date);
    }

    @Override
    public int hashCode() {
        return date.hashCode();
    }

    @Override
    public String toString() {
        return date + " (" + description + ")";
    }
}
