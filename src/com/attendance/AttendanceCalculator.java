package com.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Attendance calculator engine.
 * Handles eligibility checks, safe bunk calculation, recovery classes,
 * leave prediction, and remaining class calculation (accounting for mid-sem
 * exams).
 */
public class AttendanceCalculator {
    private static final double REQUIRED_PERCENTAGE = 75.0;

    /**
     * Check if student is currently eligible (>= 75%).
     */
    public static boolean isEligible(Subject subject) {
        return subject.getAttendancePercentage() >= REQUIRED_PERCENTAGE;
    }

    /**
     * Calculate how many more classes can be missed while maintaining 75%.
     * Formula: floor((Attended / 0.75) - Conducted)
     */
    public static int calculateSafeBunks(Subject subject) {
        if (subject.getAttendancePercentage() < REQUIRED_PERCENTAGE) {
            return 0;
        }
        int attended = subject.getClassesAttended();
        int conducted = subject.getClassesConducted();
        return (int) Math.floor((attended / (REQUIRED_PERCENTAGE / 100.0)) - conducted);
    }

    /**
     * Calculate how many consecutive classes must be attended to reach 75%.
     * Formula: ceil(3 * conducted - 4 * attended) [simplified for 75%]
     */
    public static int calculateRecoveryClasses(Subject subject) {
        if (subject.getAttendancePercentage() >= REQUIRED_PERCENTAGE) {
            return 0;
        }
        int attended = subject.getClassesAttended();
        int conducted = subject.getClassesConducted();
        int recovery = (int) Math.ceil((3.0 * conducted - 4.0 * attended));
        return recovery > 0 ? recovery : 0;
    }

    /**
     * Predict attendance after taking leave from startDate to endDate.
     * Accounts for holidays and mid-sem exam periods.
     */
    public static java.util.Map<Subject, Double> predictAttendanceAfterLeave(
            Student student,
            WeeklySchedule schedule,
            LocalDate startDate,
            LocalDate endDate) {

        java.util.Map<Subject, Double> projections = new java.util.HashMap<>();
        java.util.Map<Subject, Integer> projectedConducted = new java.util.HashMap<>();
        java.util.Map<Subject, Integer> projectedAttended = new java.util.HashMap<>();

        for (Subject s : student.getSubjects()) {
            projectedConducted.put(s, s.getClassesConducted());
            projectedAttended.put(s, s.getClassesAttended());
        }

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            // Skip holidays and mid-sem exam periods
            if (!student.getHolidayDates().contains(current) && !student.isDuringMidsemExams(current)) {
                DayOfWeek day = current.getDayOfWeek();
                List<Subject> subjectsToday = schedule.getSubjectsOn(day);
                if (subjectsToday != null) {
                    for (Subject s : subjectsToday) {
                        projectedConducted.put(s, projectedConducted.get(s) + 1);
                        // Student is absent — attended stays same
                    }
                }
            }
            current = current.plusDays(1);
        }

        for (Subject s : student.getSubjects()) {
            int cond = projectedConducted.get(s);
            int att = projectedAttended.get(s);
            double pct = (cond == 0) ? 100.0 : (double) att / cond * 100.0;
            projections.put(s, pct);
        }

        return projections;
    }

    /**
     * Calculate remaining classes until end date.
     * Excludes holidays AND mid-sem exam period.
     */
    public static int calculateRemainingClasses(
            Subject subject,
            WeeklySchedule schedule,
            LocalDate endDate,
            List<LocalDate> holidays,
            Student student) {

        if (endDate == null || endDate.isBefore(LocalDate.now())) {
            return 0;
        }

        int count = 0;
        LocalDate current = LocalDate.now().plusDays(1);

        while (!current.isAfter(endDate)) {
            // Skip holidays
            if (!holidays.contains(current)) {
                // Skip mid-sem exam period
                if (student == null || !student.isDuringMidsemExams(current)) {
                    DayOfWeek day = current.getDayOfWeek();
                    List<Subject> subjectsToday = schedule.getSubjectsOn(day);
                    if (subjectsToday != null && subjectsToday.contains(subject)) {
                        count++;
                    }
                }
            }
            current = current.plusDays(1);
        }
        return count;
    }

    /**
     * Backward-compatible overload (without student for mid-sem check).
     */
    public static int calculateRemainingClasses(
            Subject subject,
            WeeklySchedule schedule,
            LocalDate endDate,
            List<LocalDate> holidays) {
        return calculateRemainingClasses(subject, schedule, endDate, holidays, null);
    }

    /**
     * Calculate max possible attendance if student attends ALL remaining classes.
     */
    public static double calculateMaxPossibleAttendance(Subject subject, int remainingClasses) {
        int attended = subject.getClassesAttended();
        int conducted = subject.getClassesConducted();
        int totalConducted = conducted + remainingClasses;
        int totalAttended = attended + remainingClasses;
        return (totalConducted == 0) ? 100.0 : (double) totalAttended / totalConducted * 100.0;
    }
}
