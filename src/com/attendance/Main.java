package com.attendance;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Attendance Eligibility System Logic Check ---");

        // Scenario 1: Safe Bunks
        // Student has attended 30/35 classes (85.7%)
        Subject ds = new Subject("Data Structures", 4);
        ds.setAttendance(35, 30);
        System.out.println("\nSubject: " + ds.getName());
        System.out.println(ds); // Should show 85.71%

        if (AttendanceCalculator.isEligible(ds)) {
            System.out.println("Status: Eligible");
            int safeBunks = AttendanceCalculator.calculateSafeBunks(ds);
            System.out.println("You can miss " + safeBunks + " more classes.");
            // Verification: If we miss 'safeBunks' classes, are we still >= 75%?
            // (30) / (35 + safeBunks) >= 0.75 ?
            double projected = (30.0 / (35.0 + safeBunks)) * 100.0;
            System.out.println("Verification: 30 / " + (35 + safeBunks) + " = " + String.format("%.2f%%", projected));
        } else {
            System.out.println("Status: Not Eligible");
        }

        // Scenario 2: Recovery
        // Student has attended 20/35 classes (57.1%)
        Subject os = new Subject("Operating Systems", 3);
        os.setAttendance(35, 20);
        System.out.println("\nSubject: " + os.getName());
        System.out.println(os); // Should show 57.14%

        if (!AttendanceCalculator.isEligible(os)) {
            System.out.println("Status: Not Eligible");
            int recovery = AttendanceCalculator.calculateRecoveryClasses(os);
            System.out.println("You must attend " + recovery + " consecutive classes.");
            // Verification: If we attend 'recovery' classes, are we >= 75%?
            // (20 + recovery) / (35 + recovery) >= 0.75 ?
            double projected = ((20.0 + recovery) / (35.0 + recovery)) * 100.0;
            System.out.println("Verification: " + (20 + recovery) + " / " + (35 + recovery) + " = "
                    + String.format("%.2f%%", projected));
        } else {
            System.out.println("Status: Eligible");
        }

        // Scenario 3: Planned Leave Simulation
        System.out.println("\n--- Planned Leave Simulation ---");
        Student student = new Student("Archit");
        student.addSubject(ds);
        student.addSubject(os);

        WeeklySchedule schedule = new WeeklySchedule();
        // Mon: DS, Tue: OS, Wed: DS, Thu: OS, Fri: DS
        schedule.addClass(java.time.DayOfWeek.MONDAY, ds);
        schedule.addClass(java.time.DayOfWeek.TUESDAY, os);
        schedule.addClass(java.time.DayOfWeek.WEDNESDAY, ds);
        schedule.addClass(java.time.DayOfWeek.THURSDAY, os);
        schedule.addClass(java.time.DayOfWeek.FRIDAY, ds);

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate nextWeek = today.plusWeeks(1);

        System.out.println("Taking leave from " + today + " to " + nextWeek);

        // Add a holiday in between (e.g. Wednesday)
        java.time.LocalDate wednesday = today
                .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.WEDNESDAY));
        student.addHoliday(wednesday);
        System.out.println("Note: " + wednesday + " is an official holiday (no classes).");

        java.util.Map<Subject, Double> predictions = AttendanceCalculator.predictAttendanceAfterLeave(student, schedule,
                today, nextWeek);

        for (java.util.Map.Entry<Subject, Double> entry : predictions.entrySet()) {
            System.out.printf("Subject: %s -> Projected: %.2f%%\n", entry.getKey().getName(), entry.getValue());
        }
    }
}
