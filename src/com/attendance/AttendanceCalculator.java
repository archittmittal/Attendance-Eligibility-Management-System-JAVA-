package com.attendance;

public class AttendanceCalculator {
    private static final double REQUIRED_PERCENTAGE = 75.0;

    /**
     * Calculates if the student is currently eligible.
     */
    public static boolean isEligible(Subject subject) {
        return subject.getAttendancePercentage() >= REQUIRED_PERCENTAGE;
    }

    /**
     * Calculates how many more classes a student can miss (bunk) and still maintain
     * 75%.
     * Formula derived:
     * (Attended) / (Conducted + X) >= 0.75
     * Attended >= 0.75 * Content + 0.75 * X
     * Attended - 0.75 * Conducted >= 0.75 * X
     * X <= (Attended - 0.75 * Conducted) / 0.75
     * X <= (Attended / 0.75) - Conducted
     */
    public static int calculateSafeBunks(Subject subject) {
        double currentPercentage = subject.getAttendancePercentage();
        if (currentPercentage < REQUIRED_PERCENTAGE) {
            return 0; // Already below threshold, cannot bunk any
        }

        int attended = subject.getClassesAttended();
        int conducted = subject.getClassesConducted();

        // Calculate max total classes possible with current attended to keep > 75%
        // 0.75 = attended / (conducted + bunks)
        // conducted + bunks = attended / 0.75
        // bunks = (attended / 0.75) - conducted

        return (int) Math.floor((attended / (REQUIRED_PERCENTAGE / 100.0)) - conducted);
    }

    /**
     * Calculates how many consecutive classes a student MUST attend to reach 75%.
     * Formula derived:
     * (Attended + X) / (Conducted + X) >= 0.75
     * Attended + X >= 0.75 * Conducted + 0.75 * X
     * 0.25 * X >= 0.75 * Conducted - Attended
     * X >= (0.75 * Conducted - Attended) / 0.25
     */
    public static int calculateRecoveryClasses(Subject subject) {
        double currentPercentage = subject.getAttendancePercentage();
        if (currentPercentage >= REQUIRED_PERCENTAGE) {
            return 0; // Already eligible
        }

        int attended = subject.getClassesAttended();
        int conducted = subject.getClassesConducted();

        // return (int) Math.ceil(( (REQUIRED_PERCENTAGE/100.0) * conducted - attended)
        // / (1.0 - (REQUIRED_PERCENTAGE/100.0)));
        // Simplified formula for 75%: 3 * conducted - 4 * attended
        // derived from: (attended + x) / (conducted + x) >= 0.75 => 4(attended+x) >=
        // 3(conducted+x) => 4att + 4x >= 3cond + 3x => x >= 3cond - 4att

        int recovery = (int) Math.ceil((3.0 * conducted - 4.0 * attended));
        return recovery > 0 ? recovery : 0;
    }

    /**
     * Predicts the attendance percentage for each subject if the student takes
     * leave from startDate to endDate.
     */
    public static java.util.Map<Subject, Double> predictAttendanceAfterLeave(
            Student student,
            WeeklySchedule schedule,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate) {
        java.util.Map<Subject, Double> projections = new java.util.HashMap<>();

        // Initialize with current stats
        java.util.Map<Subject, Integer> projectedConducted = new java.util.HashMap<>();
        java.util.Map<Subject, Integer> projectedAttended = new java.util.HashMap<>();

        for (Subject s : student.getSubjects()) {
            projectedConducted.put(s, s.getClassesConducted());
            projectedAttended.put(s, s.getClassesAttended());
        }

        // Simulate days
        java.time.LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            // Check if it's a holiday (global or personal holiday that isn't the one we are
            // planning?
            // In this context, 'student.getHolidays()' are official holidays where NO class
            // happens.
            if (!student.getHolidays().contains(current)) {
                java.time.DayOfWeek day = current.getDayOfWeek();
                java.util.List<Subject> subjectsToday = schedule.getSubjectsOn(day);

                if (subjectsToday != null) {
                    for (Subject s : subjectsToday) {
                        // Class happens, but student is ABSENT (taking leave)
                        projectedConducted.put(s, projectedConducted.get(s) + 1);
                        // Attended count stays same
                    }
                }
            }
            current = current.plusDays(1);
        }

        // Calculate percentages
        for (Subject s : student.getSubjects()) {
            int cond = projectedConducted.get(s);
            int att = projectedAttended.get(s);
            double pct = (cond == 0) ? 100.0 : (double) att / cond * 100.0;
            projections.put(s, pct);
        }

        return projections;
    }

    public static int calculateRemainingClasses(
            Subject subject,
            WeeklySchedule schedule,
            java.time.LocalDate endDate,
            java.util.List<java.time.LocalDate> holidays) {

        if (endDate == null || endDate.isBefore(java.time.LocalDate.now())) {
            return 0;
        }

        int count = 0;
        java.time.LocalDate current = java.time.LocalDate.now().plusDays(1); // Start from tomorrow

        while (!current.isAfter(endDate)) {
            if (!holidays.contains(current)) {
                java.time.DayOfWeek day = current.getDayOfWeek();
                java.util.List<Subject> subjectsToday = schedule.getSubjectsOn(day);
                if (subjectsToday != null && subjectsToday.contains(subject)) {
                    count++;
                }
            }
            current = current.plusDays(1);
        }
        return count;
    }

    public static double calculateMaxPossibleAttendance(Subject subject, int remainingClasses) {
        int attended = subject.getClassesAttended();
        int conducted = subject.getClassesConducted();

        int totalConducted = conducted + remainingClasses;
        int totalAttended = attended + remainingClasses; // Assuming attending all remaining

        return (totalConducted == 0) ? 100.0 : (double) totalAttended / totalConducted * 100.0;
    }
}
