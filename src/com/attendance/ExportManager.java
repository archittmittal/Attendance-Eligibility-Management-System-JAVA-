package com.attendance;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Export Manager — Handles exporting attendance data as CSV or HTML Formal Report.
 * Uses Graphics2D to embed pie charts into the HTML report for visual appeal.
 */
public class ExportManager {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ═══════════════════════════════════════════
    // CSV EXPORT
    // ═══════════════════════════════════════════

    /**
     * Export attendance data as CSV for the given student.
     * Opens a JFileChooser for the user to pick a save location.
     *
     * @param parent  the parent component for dialogs
     * @param student the student whose data to export
     */
    public static void exportCSV(Component parent, Student student) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Attendance as CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        chooser.setSelectedFile(new java.io.File("attendance_" + student.getName().replaceAll("\\s+", "_") + ".csv"));

        int result = chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION)
            return;

        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new java.io.File(file.getAbsolutePath() + ".csv");
        }

        // Check overwrite
        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(parent,
                    "File already exists. Overwrite?", "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION)
                return;
        }

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            // Header section
            writer.println("# Attendance Report");
            writer.println("# Student: " + student.getName());
            writer.println("# Username: " + student.getUsername());
            writer.println("# Generated: " + LocalDate.now());
            if (student.isSemesterConfigured()) {
                writer.println("# Semester: " + student.getSemesterStartDate() + " to " + student.getSemesterEndDate());
            }
            writer.println();

            // Overall stats
            int totalAttended = 0, totalConducted = 0;
            for (Subject s : student.getSubjects()) {
                totalAttended += s.getClassesAttended();
                totalConducted += s.getClassesConducted();
            }
            double overallPct = (totalConducted == 0) ? 100.0 : (double) totalAttended / totalConducted * 100.0;
            writer.println("# Overall Attendance: " + totalAttended + "/" + totalConducted
                    + " (" + String.format("%.1f%%", overallPct) + ")");
            writer.println();

            // Per-subject summary
            writer.println("## Per-Subject Summary");
            writer.println("Subject,Attended,Conducted,Percentage,Eligible");
            for (Subject s : student.getSubjects()) {
                writer.printf("%s,%d,%d,%.1f%%,%s%n",
                        escapeCSV(s.getName()),
                        s.getClassesAttended(),
                        s.getClassesConducted(),
                        s.getAttendancePercentage(),
                        s.getAttendancePercentage() >= 75 ? "Yes" : "No");
            }
            writer.println();

            // Detailed records
            writer.println("## Detailed Records");
            writer.println("Date,Day,Subject,Status");

            // Collect all records and sort by date
            List<String[]> allRecords = new ArrayList<>();
            for (Subject s : student.getSubjects()) {
                for (AttendanceRecord record : s.getAttendanceHistory()) {
                    if (record.getDate() != null) {
                        allRecords.add(new String[] {
                                record.getDate().format(DATE_FMT),
                                record.getDate().getDayOfWeek().toString().substring(0, 3),
                                s.getName(),
                                record.isPresent() ? "Present" : "Absent"
                        });
                    }
                }
            }

            // Sort by date, then subject
            allRecords.sort((a, b) -> {
                int cmp = a[0].compareTo(b[0]);
                return cmp != 0 ? cmp : a[2].compareTo(b[2]);
            });

            for (String[] row : allRecords) {
                writer.printf("%s,%s,%s,%s%n", row[0], row[1], escapeCSV(row[2]), row[3]);
            }

            JOptionPane.showMessageDialog(parent,
                    "✅ CSV exported successfully!\n" + file.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                    "Error exporting CSV: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String escapeCSV(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    // ═══════════════════════════════════════════
    // FORMAL HTML REPORT EXPORT
    // ═══════════════════════════════════════════

    public static void exportFormalReport(Component parent, Student student) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Formal Report");
        chooser.setFileFilter(new FileNameExtensionFilter("HTML Document (*.html)", "html"));
        chooser.setSelectedFile(new java.io.File(student.getName().replaceAll("\\s+", "_") + "_Attendance_Report.html"));

        int result = chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION)
            return;

        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".html")) {
            file = new java.io.File(file.getAbsolutePath() + ".html");
        }

        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(parent,
                    "File already exists. Overwrite?", "Confirm",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION)
                return;
        }

        try {
            generateHtmlReport(file, student);
            
            // Try to open it
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(file.toURI());
            } else {
                JOptionPane.showMessageDialog(parent,
                        "✅ Report exported successfully!\n" + file.getAbsolutePath() + "\n\n(Open it in any modern web browser to view your beautifully styled report)",
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Error generating report: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void generateHtmlReport(java.io.File file, Student student) throws IOException {
        int totalAttended = 0, totalConducted = 0;
        for (Subject s : student.getSubjects()) {
            totalAttended += s.getClassesAttended();
            totalConducted += s.getClassesConducted();
        }
        double overallPct = (totalConducted == 0) ? 100.0 : (double) totalAttended / totalConducted * 100.0;

        // Generate Base64 Pie Chart
        String base64Chart = generateOverallPieChartBase64(totalAttended, totalConducted - totalAttended);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n<title>Official Academic Report</title>\n");
        // CSS Styling...
        html.append("<style>\n");
        html.append("body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; background-color: #f1f3f5; color: #1e293b; margin: 0; padding: 40px; }\n");
        html.append(".container { max-width: 900px; margin: 0 auto; background: #ffffff; padding: 50px; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.08); }\n");
        html.append(".header { text-align: center; border-bottom: 3px solid #1e3a8a; padding-bottom: 25px; margin-bottom: 35px; }\n");
        html.append(".header h1 { color: #1e3a8a; margin: 0 0 8px 0; font-size: 32px; text-transform: uppercase; letter-spacing: 2px; font-weight: 800; }\n");
        html.append(".header p { color: #64748b; margin: 4px 0; font-size: 15px; font-weight: 500; }\n");
        html.append(".info-section { display: flex; justify-content: space-between; gap: 20px; margin-bottom: 40px; }\n");
        html.append(".info-box { background: #f8fafc; padding: 25px; border-radius: 10px; flex: 1; border: 1px solid #e2e8f0; }\n");
        html.append(".info-box h3 { margin: 0 0 15px 0; color: #0f172a; font-size: 17px; border-bottom: 2px solid #cbd5e1; padding-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px; }\n");
        html.append(".info-box p { margin: 8px 0; font-size: 15px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px dashed #e2e8f0; padding-bottom: 5px; }\n");
        html.append(".info-box p:last-child { border-bottom: none; padding-bottom: 0; margin-bottom: 0; }\n");
        html.append(".chart-container { text-align: center; margin: 40px 0; background: #f8fafc; padding: 30px; border-radius: 16px; border: 1px dashed #cbd5e1; }\n");
        html.append(".chart-container h3 { color: #0f172a; margin-top: 0; margin-bottom: 20px; font-size: 18px; text-transform: uppercase; letter-spacing: 1px; }\n");
        html.append(".chart-container img { max-width: 100%; height: auto; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }\n");
        html.append(".table-title { color: #1e3a8a; border-bottom: 3px solid #e2e8f0; padding-bottom: 10px; font-size: 20px; margin-top: 50px; text-transform: uppercase; font-weight: 700; }\n");
        html.append("table { width: 100%; border-collapse: separate; border-spacing: 0; margin-top: 20px; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }\n");
        html.append("th, td { padding: 15px 18px; text-align: left; border-bottom: 1px solid #e2e8f0; }\n");
        html.append("th { background-color: #1e3a8a; color: white; font-weight: 600; text-transform: uppercase; font-size: 13px; letter-spacing: 1.2px; }\n");
        html.append("tr:last-child td { border-bottom: none; }\n");
        html.append("tr:hover { background-color: #f1f5f9; }\n");
        html.append(".status-safe { color: #065f46; font-weight: 700; background: #a7f3d0; padding: 6px 12px; border-radius: 20px; font-size: 12px; text-transform: uppercase; display: inline-block; }\n");
        html.append(".status-danger { color: #991b1b; font-weight: 700; background: #fecaca; padding: 6px 12px; border-radius: 20px; font-size: 12px; text-transform: uppercase; display: inline-block; }\n");
        html.append(".footer { text-align: center; margin-top: 60px; color: #94a3b8; font-size: 13px; border-top: 1px solid #e2e8f0; padding-top: 30px; }\n");
        html.append("@media print { \n");
        html.append("  body { background-color: #ffffff; padding: 0; font-size: 12pt; }\n");
        html.append("  .container { box-shadow: none; padding: 0; max-width: 100%; }\n");
        html.append("  .chart-container { background: none; border: none; padding: 10px; }\n");
        html.append("  .info-box { background: none; border: 1px solid #ccc; }\n");
        html.append("}\n");
        html.append("</style>\n</head>\n<body>\n");

        html.append("<div class=\"container\">\n");
        
        // Header
        html.append("<div class=\"header\">\n");
        html.append("<h1>Official Academic Report</h1>\n");
        html.append("<p>Attendance & Eligibility Management System</p>\n");
        html.append("<p>Generated on " + LocalDate.now() + "</p>\n");
        html.append("</div>\n");

        // Info Section
        html.append("<div class=\"info-section\">\n");
        html.append("<div class=\"info-box\">\n");
        html.append("<h3>Student Information</h3>\n");
        html.append("<p><span>Full Name</span> <strong>" + escapeHTML(student.getName()) + "</strong></p>\n");
        html.append("<p><span>Registration / Username</span> <strong>" + escapeHTML(student.getUsername()) + "</strong></p>\n");
        if (student.isSemesterConfigured()) {
            html.append("<p><span>Semester Window</span> <strong>" + student.getSemesterStartDate() + " to " + student.getSemesterEndDate() + "</strong></p>\n");
        }
        html.append("</div>\n");

        html.append("<div class=\"info-box\">\n");
        html.append("<h3>Overall Attendance Summary</h3>\n");
        html.append("<p><span>Total Attended</span> <strong>" + totalAttended + " classes</strong></p>\n");
        html.append("<p><span>Total Conducted</span> <strong>" + totalConducted + " classes</strong></p>\n");
        html.append("<p><span>Cumulative Percentage</span> <strong style=\"color: #1e3a8a; font-size: 18px;\">" + String.format("%.2f%%", overallPct) + "</strong></p>\n");
        html.append("</div>\n");
        html.append("</div>\n");

        // Chart
        if (base64Chart != null) {
            html.append("<div class=\"chart-container\">\n");
            html.append("<h3>Attendance Distribution Overlay</h3>\n");
            html.append("<img src=\"data:image/png;base64," + base64Chart + "\" alt=\"Overall Attendance Chart\">\n");
            html.append("</div>\n");
        }

        // Table
        html.append("<h3 class=\"table-title\">Subject-wise Integrity Breakdown</h3>\n");
        html.append("<table>\n");
        html.append("<tr><th>Subject</th><th>Att.</th><th>Cond.</th><th>Percentage</th><th>Eligibility Status</th><th>Actionable Notes</th></tr>\n");

        for (Subject s : student.getSubjects()) {
            double pct = s.getAttendancePercentage();
            String statusClass = pct >= 75.0 ? "status-safe" : "status-danger";
            String statusText = pct >= 75.0 ? "Eligible" : "Critical (< 75%)";
            
            String notes = "-";
            if (pct >= 75.0) {
                int safeBunks = s.calculateSafeBunks(75.0);
                if (safeBunks > 0) notes = "<strong>" + safeBunks + "</strong> Safe Bunk" + (safeBunks > 1 ? "s" : "");
            } else {
                int needed = s.calculateClassesNeeded(75.0);
                notes = "Attend next <strong>" + needed + "</strong> class" + (needed > 1 ? "es" : "");
            }

            html.append("<tr>");
            html.append("<td><strong>").append(escapeHTML(s.getName())).append("</strong></td>");
            html.append("<td>").append(s.getClassesAttended()).append("</td>");
            html.append("<td>").append(s.getClassesConducted()).append("</td>");
            html.append("<td><strong>").append(String.format("%.1f%%", pct)).append("</strong></td>");
            html.append("<td><span class=\"").append(statusClass).append("\">").append(statusText).append("</span></td>");
            html.append("<td><span style=\"font-size: 13.5px; color: #475569;\">").append(notes).append("</span></td>");
            html.append("</tr>\n");
        }
        
        html.append("</table>\n");

        // Footer
        html.append("<div class=\"footer\">\n");
        html.append("<p>This is a system-generated document and requires no physical signature. Designed natively with the Premium Academic Theme.</p>\n");
        html.append("<p><strong>Attendance & Eligibility Management System</strong> &copy; " + LocalDate.now().getYear() + "</p>\n");
        html.append("</div>\n");

        html.append("</div>\n</body>\n</html>");

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            writer.write(html.toString());
        }
    }

    private static String generateOverallPieChartBase64(int attended, int absent) {
        if (attended == 0 && absent == 0) return null;
        
        int width = 800;
        int height = 500;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        // Clear background with transparent or white
        g2d.setColor(new Color(248, 250, 252)); // match chart-container background
        g2d.fillRect(0, 0, width, height);
        
        int total = attended + absent;
        int presentAngle = (int) Math.round((attended * 360.0) / total);
        int absentAngle = 360 - presentAngle;
        
        int cx = 250;
        int cy = 250;
        int radius = 180;
        
        // Present Slice (Emerald Green)
        g2d.setColor(new Color(5, 150, 105));
        g2d.fillArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -presentAngle);
        
        // Absent Slice (Crimson Red)
        g2d.setColor(new Color(220, 38, 38));
        g2d.fillArc(cx - radius, cy - radius, radius * 2, radius * 2, 90 - presentAngle, -absentAngle);
        
        // Draw Legend
        int legendX = cx + radius + 80;
        int legendY = cy - 40;
        
        g2d.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 22));
        
        // Present Legend
        g2d.setColor(new Color(5, 150, 105));
        g2d.fillRoundRect(legendX, legendY, 25, 25, 8, 8);
        g2d.setColor(new Color(15, 23, 42));
        g2d.drawString("Present (" + attended + ")", legendX + 40, legendY + 20);
        
        // Absent Legend
        legendY += 50;
        g2d.setColor(new Color(220, 38, 38));
        g2d.fillRoundRect(legendX, legendY, 25, 25, 8, 8);
        g2d.setColor(new Color(15, 23, 42));
        g2d.drawString("Absent (" + absent + ")", legendX + 40, legendY + 20);
        
        g2d.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            return null;
        }
    }

    private static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
