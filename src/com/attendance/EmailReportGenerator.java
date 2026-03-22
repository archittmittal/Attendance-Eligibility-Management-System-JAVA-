package com.attendance;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Generates styled HTML attendance reports and sends them via email.
 * Uses Jakarta Mail API with Gmail SMTP.
 */
public class EmailReportGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Send an attendance report email to the specified recipient.
     *
     * @param recipientEmail the email to send to
     * @param subject        the email subject line
     * @param student        the student whose data to report
     * @return true if sent successfully
     */
    public static boolean sendReport(String recipientEmail, String subject, Student student) {
        String htmlBody = generateHtmlReport(student);
        return sendEmail(recipientEmail, subject, htmlBody);
    }

    /**
     * Send a raw HTML email via SMTP.
     */
    public static boolean sendEmail(String recipientEmail, String subject, String htmlBody) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", EmailConfig.SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(EmailConfig.SMTP_PORT));
        props.put("mail.smtp.ssl.trust", EmailConfig.SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EmailConfig.SENDER_EMAIL, EmailConfig.SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EmailConfig.SENDER_EMAIL, "Attendance Manager"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email sent successfully to " + recipientEmail);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generate a beautiful HTML attendance report for email.
     * Uses inline CSS since email clients don't support external stylesheets.
     */
    public static String generateHtmlReport(Student student) {
        int totalAttended = 0, totalConducted = 0;
        for (Subject s : student.getSubjects()) {
            totalAttended += s.getClassesAttended();
            totalConducted += s.getClassesConducted();
        }
        double overallPct = (totalConducted == 0) ? 100.0 : (double) totalAttended / totalConducted * 100.0;
        boolean eligible = overallPct >= 75.0;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='margin:0; padding:0; background-color:#f0f2f5; font-family:Segoe UI,Arial,sans-serif;'>");

        // Container
        html.append("<div style='max-width:650px; margin:20px auto; background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,0.08);'>");

        // Header Banner
        html.append("<div style='background:linear-gradient(135deg,#1e3a8a,#3b82f6); padding:35px 30px; text-align:center;'>");
        html.append("<h1 style='color:#ffffff; margin:0 0 8px 0; font-size:26px; letter-spacing:1px;'>📊 Attendance Report</h1>");
        html.append("<p style='color:#bfdbfe; margin:0; font-size:14px;'>Generated on ").append(LocalDate.now().format(DATE_FMT)).append("</p>");
        html.append("</div>");

        // Student Info
        html.append("<div style='padding:25px 30px; border-bottom:1px solid #e5e7eb;'>");
        html.append("<table style='width:100%;'><tr>");
        html.append("<td style='padding:5px 0;'><span style='color:#6b7280; font-size:13px;'>Student</span><br><strong style='font-size:16px; color:#111827;'>").append(escapeHTML(student.getName())).append("</strong></td>");
        html.append("<td style='padding:5px 0; text-align:right;'><span style='color:#6b7280; font-size:13px;'>Username</span><br><strong style='font-size:16px; color:#111827;'>").append(escapeHTML(student.getUsername())).append("</strong></td>");
        html.append("</tr></table>");
        html.append("</div>");

        // Overall Stats Card
        String overallColor = eligible ? "#059669" : "#dc2626";
        String overallBg = eligible ? "#ecfdf5" : "#fef2f2";
        String overallLabel = eligible ? "✅ ELIGIBLE" : "⚠️ AT RISK";

        html.append("<div style='margin:25px 30px; padding:25px; background:").append(overallBg).append("; border-radius:12px; text-align:center; border:1px solid ").append(eligible ? "#a7f3d0" : "#fecaca").append(";'>");
        html.append("<div style='font-size:42px; font-weight:800; color:").append(overallColor).append(";'>").append(String.format("%.1f%%", overallPct)).append("</div>");
        html.append("<div style='font-size:14px; color:#6b7280; margin:5px 0;'>").append(totalAttended).append(" / ").append(totalConducted).append(" classes attended</div>");
        html.append("<div style='display:inline-block; padding:5px 16px; border-radius:20px; font-size:12px; font-weight:700; color:").append(overallColor).append("; background:").append(eligible ? "#d1fae5" : "#fee2e2").append("; letter-spacing:0.5px;'>").append(overallLabel).append("</div>");
        html.append("</div>");

        // Subject-wise Table
        html.append("<div style='padding:0 30px 30px 30px;'>");
        html.append("<h3 style='color:#1e3a8a; font-size:16px; margin-bottom:15px; text-transform:uppercase; letter-spacing:1px; border-bottom:2px solid #e5e7eb; padding-bottom:10px;'>Subject-wise Breakdown</h3>");
        html.append("<table style='width:100%; border-collapse:collapse;'>");
        html.append("<tr style='background:#f9fafb;'>");
        html.append("<th style='padding:12px 10px; text-align:left; font-size:12px; color:#6b7280; text-transform:uppercase; letter-spacing:0.5px; border-bottom:2px solid #e5e7eb;'>Subject</th>");
        html.append("<th style='padding:12px 10px; text-align:center; font-size:12px; color:#6b7280; text-transform:uppercase; letter-spacing:0.5px; border-bottom:2px solid #e5e7eb;'>Attended</th>");
        html.append("<th style='padding:12px 10px; text-align:center; font-size:12px; color:#6b7280; text-transform:uppercase; letter-spacing:0.5px; border-bottom:2px solid #e5e7eb;'>Percentage</th>");
        html.append("<th style='padding:12px 10px; text-align:center; font-size:12px; color:#6b7280; text-transform:uppercase; letter-spacing:0.5px; border-bottom:2px solid #e5e7eb;'>Status</th>");
        html.append("<th style='padding:12px 10px; text-align:right; font-size:12px; color:#6b7280; text-transform:uppercase; letter-spacing:0.5px; border-bottom:2px solid #e5e7eb;'>Action</th>");
        html.append("</tr>");

        for (Subject s : student.getSubjects()) {
            double pct = s.getAttendancePercentage();
            boolean subEligible = pct >= 75.0;
            String rowColor = subEligible ? "#059669" : "#dc2626";
            String statusBadge = subEligible
                    ? "<span style='background:#d1fae5; color:#065f46; padding:3px 10px; border-radius:12px; font-size:11px; font-weight:600;'>Safe</span>"
                    : "<span style='background:#fee2e2; color:#991b1b; padding:3px 10px; border-radius:12px; font-size:11px; font-weight:600;'>At Risk</span>";

            String actionText;
            if (subEligible) {
                int safeBunks = AttendanceCalculator.calculateSafeBunks(s);
                actionText = "Can miss " + safeBunks;
            } else {
                int recovery = AttendanceCalculator.calculateRecoveryClasses(s);
                actionText = "Attend next " + recovery;
            }

            html.append("<tr style='border-bottom:1px solid #f3f4f6;'>");
            html.append("<td style='padding:14px 10px; font-weight:600; color:#111827;'>").append(escapeHTML(s.getName())).append("</td>");
            html.append("<td style='padding:14px 10px; text-align:center; color:#374151;'>").append(s.getClassesAttended()).append(" / ").append(s.getClassesConducted()).append("</td>");
            html.append("<td style='padding:14px 10px; text-align:center; font-weight:700; color:").append(rowColor).append(";'>").append(String.format("%.1f%%", pct)).append("</td>");
            html.append("<td style='padding:14px 10px; text-align:center;'>").append(statusBadge).append("</td>");
            html.append("<td style='padding:14px 10px; text-align:right; color:#6b7280; font-size:13px;'>").append(actionText).append("</td>");
            html.append("</tr>");
        }

        html.append("</table>");
        html.append("</div>");

        // Footer
        html.append("<div style='background:#f9fafb; padding:20px 30px; text-align:center; border-top:1px solid #e5e7eb;'>");
        html.append("<p style='margin:0; color:#9ca3af; font-size:12px;'>This is an automated report from <strong>Attendance & Eligibility Management System</strong></p>");
        html.append("<p style='margin:5px 0 0 0; color:#d1d5db; font-size:11px;'>© ").append(LocalDate.now().getYear()).append(" — Do not reply to this email</p>");
        html.append("</div>");

        html.append("</div></body></html>");
        return html.toString();
    }

    private static String escapeHTML(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#").append((int) c).append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
