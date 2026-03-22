package com.attendance;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Background email scheduler.
 * Checks once per hour if a weekly or monthly report is due,
 * and sends automatically if configured.
 * Runs on a daemon thread so it won't prevent app shutdown.
 */
public class EmailScheduler {

    private Timer timer;
    private final Student student;

    private static final long CHECK_INTERVAL_MS = 60 * 60 * 1000; // 1 hour
    private static final int WEEKLY_DAYS = 7;
    private static final int MONTHLY_DAYS = 30;

    public EmailScheduler(Student student) {
        this.student = student;
    }

    /**
     * Start the background scheduler.
     */
    public void start() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer("EmailScheduler", true); // daemon thread
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAndSend();
            }
        }, 5000, CHECK_INTERVAL_MS); // First check after 5 seconds, then hourly

        System.out.println("📧 Email scheduler started for " + student.getName());
    }

    /**
     * Stop the scheduler.
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            System.out.println("📧 Email scheduler stopped.");
        }
    }

    /**
     * Check if a report needs to be sent and send it.
     */
    private void checkAndSend() {
        String email = student.getEmail();
        String frequency = student.getEmailFrequency();

        // Skip if not configured
        if (email == null || email.trim().isEmpty() || "off".equalsIgnoreCase(frequency)) {
            return;
        }

        LocalDate lastSent = student.getLastEmailSent();
        LocalDate today = LocalDate.now();

        int requiredDays = "weekly".equalsIgnoreCase(frequency) ? WEEKLY_DAYS : MONTHLY_DAYS;

        // Check if enough time has passed
        if (lastSent != null && ChronoUnit.DAYS.between(lastSent, today) < requiredDays) {
            return; // Not due yet
        }

        // Determine subject line
        String subject = "weekly".equalsIgnoreCase(frequency)
                ? EmailConfig.EMAIL_SUBJECT_WEEKLY
                : EmailConfig.EMAIL_SUBJECT_MONTHLY;

        System.out.println("📧 Sending scheduled " + frequency + " report to " + email + "...");

        boolean success = EmailReportGenerator.sendReport(email, subject, student);

        if (success) {
            student.setLastEmailSent(today);
            DatabaseManager.getInstance().updateLastEmailSent(student.getId(), today);
            System.out.println("✅ Scheduled " + frequency + " report sent successfully!");
        } else {
            System.err.println("❌ Failed to send scheduled report.");
        }
    }
}
