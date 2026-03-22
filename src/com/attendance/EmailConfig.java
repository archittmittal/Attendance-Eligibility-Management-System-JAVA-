package com.attendance;

/**
 * Email (SMTP) configuration constants.
 * Uses Gmail SMTP by default. To send emails, you need a Gmail App Password:
 * 1. Go to https://myaccount.google.com/apppasswords
 * 2. Generate an App Password for "Mail"
 * 3. Paste it as SENDER_PASSWORD below
 */
public class EmailConfig {
    // Gmail SMTP Settings
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final int SMTP_PORT = 587;

    // ── FILL THESE IN ──
    public static final String SENDER_EMAIL = "your.email@gmail.com";   // Your Gmail address
    public static final String SENDER_PASSWORD = "xxxx xxxx xxxx xxxx"; // Gmail App Password

    // Email Defaults
    public static final String EMAIL_SUBJECT_WEEKLY = "📊 Weekly Attendance Report";
    public static final String EMAIL_SUBJECT_MONTHLY = "📊 Monthly Attendance Report";

    private EmailConfig() {
        // Prevent instantiation — utility class
    }
}
