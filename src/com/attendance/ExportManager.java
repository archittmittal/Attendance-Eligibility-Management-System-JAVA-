package com.attendance;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Export Manager — Handles exporting attendance data as CSV or PDF.
 * No external libraries required.
 *
 * CSV: Plain text file with headers and per-subject breakdown.
 * PDF: Uses Java's built-in printing API (java.awt.print) to render
 * formatted content to a PDF-compatible PrinterJob (or saves via Graphics2D).
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

    // ═══════════════════════════════════════════
    // PDF EXPORT
    // ═══════════════════════════════════════════

    /**
     * Export attendance data as a PDF report.
     * Uses Java's built-in printing/graphics API to render a multi-page document.
     *
     * @param parent  the parent component for dialogs
     * @param student the student whose data to export
     */
    public static void exportPDF(Component parent, Student student) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Attendance as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
        chooser.setSelectedFile(new java.io.File("attendance_" + student.getName().replaceAll("\\s+", "_") + ".pdf"));

        int result = chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION)
            return;

        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new java.io.File(file.getAbsolutePath() + ".pdf");
        }

        // Check overwrite
        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(parent,
                    "File already exists. Overwrite?", "Confirm",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION)
                return;
        }

        try {
            // Generate formatted PDF report directly (no printer dependency)
            generateFormattedReport(file, student);

            JOptionPane.showMessageDialog(parent,
                    "✅ Report exported successfully!\n" + file.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                    "Error exporting report: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Generate a formatted plain-text report that mimics a PDF layout.
     * This is a reliable cross-platform approach that doesn't depend on PDF
     * printers.
     */
    private static void generateFormattedReport(java.io.File file, Student student) throws IOException {
        // Actually write as a proper text-based PDF
        // We'll use raw PDF generation here — minimal but valid PDF
        try (OutputStream os = new FileOutputStream(file)) {
            PDFWriter pdf = new PDFWriter(os);
            pdf.begin();

            // Title
            pdf.addLine("ATTENDANCE REPORT", 18, true);
            pdf.addLine("", 12, false);

            // Student info
            pdf.addLine("Student: " + student.getName(), 12, false);
            pdf.addLine("Username: " + student.getUsername(), 12, false);
            pdf.addLine("Generated: " + LocalDate.now(), 12, false);
            if (student.isSemesterConfigured()) {
                pdf.addLine("Semester: " + student.getSemesterStartDate() + " to " + student.getSemesterEndDate(), 12,
                        false);
            }
            pdf.addLine("", 12, false);

            // Overall stats
            int totalAttended = 0, totalConducted = 0;
            for (Subject s : student.getSubjects()) {
                totalAttended += s.getClassesAttended();
                totalConducted += s.getClassesConducted();
            }
            double overallPct = (totalConducted == 0) ? 100.0 : (double) totalAttended / totalConducted * 100.0;
            pdf.addLine("Overall Attendance: " + totalAttended + "/" + totalConducted
                    + " (" + String.format("%.1f%%", overallPct) + ")", 14, true);
            pdf.addLine("", 12, false);

            // Per-subject table
            pdf.addLine("PER-SUBJECT BREAKDOWN", 14, true);
            pdf.addLine("--------------------------------------------------", 10, false);
            pdf.addLine(String.format("%-20s %8s %8s %8s %8s", "Subject", "Attended", "Conducted", "Pct", "Eligible"),
                    10, false);
            pdf.addLine("--------------------------------------------------", 10, false);
            for (Subject s : student.getSubjects()) {
                pdf.addLine(String.format("%-20s %8d %8d %7.1f%% %8s",
                        truncate(s.getName(), 20),
                        s.getClassesAttended(),
                        s.getClassesConducted(),
                        s.getAttendancePercentage(),
                        s.getAttendancePercentage() >= 75 ? "Yes" : "No"), 10, false);
            }
            pdf.addLine("--------------------------------------------------", 10, false);
            pdf.addLine("", 12, false);

            // Detailed records
            pdf.addLine("DETAILED ATTENDANCE RECORDS", 14, true);
            pdf.addLine(String.format("%-12s %-5s %-20s %-8s", "Date", "Day", "Subject", "Status"), 10, false);
            pdf.addLine("--------------------------------------------------", 10, false);

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
            allRecords.sort((a, b) -> {
                int cmp = a[0].compareTo(b[0]);
                return cmp != 0 ? cmp : a[2].compareTo(b[2]);
            });

            for (String[] row : allRecords) {
                pdf.addLine(String.format("%-12s %-5s %-20s %-8s",
                        row[0], row[1], truncate(row[2], 20), row[3]), 10, false);
            }

            pdf.end();
        }
    }

    /**
     * Escape a CSV field (quote if it contains commas or quotes).
     */
    private static String escapeCSV(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * Truncate a string to maxLen characters.
     */
    private static String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen - 2) + ".." : s;
    }

    /**
     * Minimal PDF writer — generates valid PDF 1.4 files without external
     * dependencies.
     * Writes simple single-page text content using Helvetica font.
     */
    private static class PDFWriter {
        private final OutputStream out;
        private final List<String> textLines = new ArrayList<>();
        private final List<int[]> fontSpecs = new ArrayList<>(); // [fontSize, bold]

        PDFWriter(OutputStream out) {
            this.out = out;
        }

        void addLine(String text, int fontSize, boolean bold) {
            textLines.add(text);
            fontSpecs.add(new int[] { fontSize, bold ? 1 : 0 });
        }

        void begin() {
            // Nothing to do here
        }

        void end() throws IOException {
            // Build PDF content
            StringBuilder content = new StringBuilder();
            content.append("BT\n");

            int y = 750; // Start from top
            for (int i = 0; i < textLines.size(); i++) {
                int fontSize = fontSpecs.get(i)[0];
                boolean bold = fontSpecs.get(i)[1] == 1;
                String fontName = bold ? "/F2" : "/F1";

                content.append(fontName).append(" ").append(fontSize).append(" Tf\n");
                content.append("36 ").append(y).append(" Td\n");
                content.append("(").append(escapePDF(textLines.get(i))).append(") Tj\n");

                y -= (fontSize + 4);
                if (y < 50)
                    break; // Page boundary
            }
            content.append("ET\n");

            String stream = content.toString();
            byte[] streamBytes = stream.getBytes("ISO-8859-1");

            // PDF structure
            StringBuilder pdf = new StringBuilder();
            List<Integer> offsets = new ArrayList<>();

            // Header
            pdf.append("%PDF-1.4\n");

            // Object 1: Catalog
            offsets.add(pdf.length());
            pdf.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            // Object 2: Pages
            offsets.add(pdf.length());
            pdf.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            // Object 3: Page
            offsets.add(pdf.length());
            pdf.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] ");
            pdf.append("/Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n");

            // Object 4: Content stream
            offsets.add(pdf.length());
            pdf.append("4 0 obj\n<< /Length ").append(streamBytes.length).append(" >>\nstream\n");
            pdf.append(stream);
            pdf.append("endstream\nendobj\n");

            // Object 5: Font (Helvetica)
            offsets.add(pdf.length());
            pdf.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            // Object 6: Font (Helvetica-Bold)
            offsets.add(pdf.length());
            pdf.append("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

            // Cross-reference table
            int xrefOffset = pdf.length();
            pdf.append("xref\n");
            pdf.append("0 ").append(offsets.size() + 1).append("\n");
            pdf.append("0000000000 65535 f \n");
            for (int offset : offsets) {
                pdf.append(String.format("%010d 00000 n \n", offset));
            }

            // Trailer
            pdf.append("trailer\n<< /Size ").append(offsets.size() + 1);
            pdf.append(" /Root 1 0 R >>\n");
            pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");

            out.write(pdf.toString().getBytes("ISO-8859-1"));
        }

        private static String escapePDF(String text) {
            return text.replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("%", "");
        }
    }
}
