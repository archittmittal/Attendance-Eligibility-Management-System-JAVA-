package com.attendance;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Detailed Summary Export — generates a beautiful HTML report with
 * date-range filtering, pie charts, bar charts, monthly trend charts,
 * and day-wise attendance heatmaps. All charts rendered via Graphics2D
 * and embedded as Base64 PNG.
 */
public class DetailedSummaryExport {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    // Color palette for charts
    private static final Color[] CHART_COLORS = {
            new Color(59, 130, 246),   // Blue
            new Color(16, 185, 129),   // Green
            new Color(245, 158, 11),   // Amber
            new Color(239, 68, 68),    // Red
            new Color(139, 92, 246),   // Purple
            new Color(236, 72, 153),   // Pink
            new Color(20, 184, 166),   // Teal
            new Color(249, 115, 22),   // Orange
    };

    /**
     * Instantly generate a full summary report (all-time data) and open it in the browser.
     * One click — no dialogs, no file chooser.
     */
    public static void generateInstantReport(Component parent, Student student) {
        LocalDate from = student.getSemesterStartDate() != null
                ? student.getSemesterStartDate() : LocalDate.now().minusMonths(6);
        LocalDate to = LocalDate.now();

        try {
            File tempFile = File.createTempFile("Attendance_Summary_", ".html");
            tempFile.deleteOnExit();
            generateDetailedHtml(tempFile, student, from, to);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(tempFile.toURI());
            } else {
                JOptionPane.showMessageDialog(parent,
                        "✅ Report saved!\n" + tempFile.getAbsolutePath(),
                        "Summary & Charts", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Error generating report: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Show the date range picker dialog and export the summary.
     */
    public static void showExportDialog(Component parent, Student student) {
        Color BG = ThemeManager.getBgColor();
        Color CARD = ThemeManager.getCardColor();
        Color TEXT = ThemeManager.getTextColor();
        Color SUBTEXT = ThemeManager.getSubtextColor();
        Color ACCENT = ThemeManager.getAccentColor();
        Color SURFACE = ThemeManager.getSurfaceColor();

        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(parent), "📊 Detailed Summary Export", true);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG);

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(ThemeManager.getHeaderColor());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel title = new JLabel("📊 Detailed Summary Report");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(ACCENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Select a date range for your detailed attendance summary");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(SUBTEXT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(title);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        headerPanel.add(subtitle);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(CARD);
        formPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel fromLabel = new JLabel("From Date (yyyy-mm-dd):");
        fromLabel.setForeground(TEXT);
        fromLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        formPanel.add(fromLabel, gbc);

        JTextField fromField = new UIUtils.RoundedTextField(12, 10);
        fromField.setBackground(SURFACE);
        fromField.setForeground(TEXT);
        fromField.setCaretColor(TEXT);
        // Default: semester start or 3 months ago
        LocalDate defaultFrom = student.getSemesterStartDate() != null
                ? student.getSemesterStartDate() : LocalDate.now().minusMonths(3);
        fromField.setText(defaultFrom.toString());
        gbc.gridx = 1;
        formPanel.add(fromField, gbc);

        JLabel toLabel = new JLabel("To Date (yyyy-mm-dd):");
        toLabel.setForeground(TEXT);
        toLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(toLabel, gbc);

        JTextField toField = new UIUtils.RoundedTextField(12, 10);
        toField.setBackground(SURFACE);
        toField.setForeground(TEXT);
        toField.setCaretColor(TEXT);
        toField.setText(LocalDate.now().toString());
        gbc.gridx = 1;
        formPanel.add(toField, gbc);

        // Quick buttons
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        quickPanel.setBackground(CARD);
        JButton lastWeek = new UIUtils.RoundedButton("Last Week", SURFACE, TEXT, 8);
        lastWeek.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lastWeek.addActionListener(e -> { fromField.setText(LocalDate.now().minusWeeks(1).toString()); toField.setText(LocalDate.now().toString()); });
        JButton lastMonth = new UIUtils.RoundedButton("Last Month", SURFACE, TEXT, 8);
        lastMonth.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lastMonth.addActionListener(e -> { fromField.setText(LocalDate.now().minusMonths(1).toString()); toField.setText(LocalDate.now().toString()); });
        JButton allTime = new UIUtils.RoundedButton("All Time", SURFACE, TEXT, 8);
        allTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        allTime.addActionListener(e -> { fromField.setText(defaultFrom.toString()); toField.setText(LocalDate.now().toString()); });
        quickPanel.add(lastWeek);
        quickPanel.add(lastMonth);
        quickPanel.add(allTime);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(quickPanel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        btnPanel.setBackground(BG);
        JButton exportBtn = new UIUtils.RoundedButton("📊 Generate Report", ACCENT, ThemeManager.getHeaderColor(), 10);
        exportBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        exportBtn.addActionListener(e -> {
            try {
                LocalDate from = LocalDate.parse(fromField.getText().trim());
                LocalDate to = LocalDate.parse(toField.getText().trim());
                if (from.isAfter(to)) {
                    JOptionPane.showMessageDialog(dialog, "From date must be before To date.");
                    return;
                }
                dialog.dispose();
                exportDetailedSummary(parent, student, from, to);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format. Use yyyy-mm-dd.");
            }
        });
        JButton cancelBtn = new UIUtils.RoundedButton("Cancel", SURFACE, TEXT, 10);
        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(exportBtn);
        btnPanel.add(cancelBtn);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * Export the detailed summary as a beautiful HTML file.
     */
    private static void exportDetailedSummary(Component parent, Student student, LocalDate from, LocalDate to) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Detailed Summary Report");
        chooser.setFileFilter(new FileNameExtensionFilter("HTML Document (*.html)", "html"));
        chooser.setSelectedFile(new File(student.getName().replaceAll("\\s+", "_") + "_Detailed_Summary.html"));

        int result = chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".html")) {
            file = new File(file.getAbsolutePath() + ".html");
        }

        try {
            generateDetailedHtml(file, student, from, to);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(file.toURI());
            } else {
                JOptionPane.showMessageDialog(parent,
                        "✅ Report saved!\n" + file.getAbsolutePath(),
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Error generating report: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Generate the full HTML report with embedded charts.
     */
    private static void generateDetailedHtml(File file, Student student, LocalDate from, LocalDate to) throws IOException {
        // ── GATHER DATA ──
        int totalAttended = 0, totalConducted = 0;
        // Per-subject data filtered by date range
        Map<String, int[]> subjectData = new LinkedHashMap<>(); // name -> [attended, conducted]
        Map<String, Map<String, int[]>> monthlyData = new LinkedHashMap<>(); // month -> subject -> [att, cond]
        Map<String, int[]> dayOfWeekData = new LinkedHashMap<>(); // day -> [attended, absent]

        String[] daysOfWeek = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        for (String day : daysOfWeek) {
            dayOfWeekData.put(day, new int[]{0, 0});
        }

        for (Subject s : student.getSubjects()) {
            int att = 0, cond = 0;
            for (AttendanceRecord r : s.getAttendanceHistory()) {
                if (r.getDate() == null) continue;
                if (r.getDate().isBefore(from) || r.getDate().isAfter(to)) continue;

                cond++;
                if (r.isPresent()) att++;

                // Monthly breakdown
                String monthKey = r.getDate().format(MONTH_FMT);
                monthlyData.computeIfAbsent(monthKey, k -> new LinkedHashMap<>());
                monthlyData.get(monthKey).computeIfAbsent(s.getName(), k -> new int[]{0, 0});
                int[] mv = monthlyData.get(monthKey).get(s.getName());
                mv[1]++;
                if (r.isPresent()) mv[0]++;

                // Day-of-week breakdown
                String dayKey = r.getDate().getDayOfWeek().toString();
                int[] dv = dayOfWeekData.get(dayKey);
                if (dv != null) {
                    if (r.isPresent()) dv[0]++;
                    else dv[1]++;
                }
            }
            subjectData.put(s.getName(), new int[]{att, cond});
            totalAttended += att;
            totalConducted += cond;
        }

        double overallPct = (totalConducted == 0) ? 100.0 : (double) totalAttended / totalConducted * 100.0;

        // ── GENERATE CHARTS ──
        String pieChartBase64 = generatePieChart(totalAttended, totalConducted - totalAttended);
        String barChartBase64 = generateBarChart(subjectData);
        String trendChartBase64 = generateTrendChart(monthlyData);
        String heatmapBase64 = generateDayHeatmap(dayOfWeekData);

        // ── BUILD HTML ──
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset='UTF-8'>\n<title>Detailed Attendance Summary</title>\n");
        html.append("<style>\n");
        html.append("* { box-sizing: border-box; margin: 0; padding: 0; }\n");
        html.append("body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%); color: #e2e8f0; padding: 40px 20px; min-height: 100vh; }\n");
        html.append(".container { max-width: 1000px; margin: 0 auto; }\n");
        html.append(".header { text-align: center; padding: 40px 30px; background: linear-gradient(135deg, #1e3a8a, #3b82f6, #6366f1); border-radius: 20px; margin-bottom: 30px; box-shadow: 0 20px 60px rgba(59,130,246,0.3); }\n");
        html.append(".header h1 { font-size: 36px; font-weight: 800; letter-spacing: 1px; margin-bottom: 8px; color: #fff; }\n");
        html.append(".header p { color: #bfdbfe; font-size: 15px; }\n");
        html.append(".header .date-range { background: rgba(255,255,255,0.15); display: inline-block; padding: 8px 20px; border-radius: 30px; margin-top: 12px; font-size: 14px; color: #e0e7ff; backdrop-filter: blur(5px); }\n");

        // Stats grid
        html.append(".stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 30px; }\n");
        html.append(".stat-card { background: #1e293b; border-radius: 16px; padding: 25px 20px; text-align: center; border: 1px solid #334155; transition: transform 0.2s; }\n");
        html.append(".stat-card:hover { transform: translateY(-3px); }\n");
        html.append(".stat-value { font-size: 32px; font-weight: 800; margin-bottom: 4px; }\n");
        html.append(".stat-label { font-size: 12px; text-transform: uppercase; letter-spacing: 1px; color: #94a3b8; }\n");

        // Cards
        html.append(".card { background: #1e293b; border-radius: 16px; padding: 30px; margin-bottom: 24px; border: 1px solid #334155; }\n");
        html.append(".card h3 { font-size: 18px; margin-bottom: 20px; color: #60a5fa; text-transform: uppercase; letter-spacing: 1px; padding-bottom: 10px; border-bottom: 2px solid #334155; }\n");
        html.append(".chart-img { text-align: center; padding: 10px; }\n");
        html.append(".chart-img img { max-width: 100%; height: auto; border-radius: 12px; }\n");

        // Table
        html.append("table { width: 100%; border-collapse: collapse; }\n");
        html.append("th { padding: 14px 12px; text-align: left; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; color: #94a3b8; border-bottom: 2px solid #334155; }\n");
        html.append("td { padding: 14px 12px; border-bottom: 1px solid #1e293b; }\n");
        html.append("tr:hover td { background: #0f172a; }\n");
        html.append(".badge-safe { background: #065f46; color: #6ee7b7; padding: 4px 12px; border-radius: 20px; font-size: 11px; font-weight: 700; }\n");
        html.append(".badge-risk { background: #7f1d1d; color: #fca5a5; padding: 4px 12px; border-radius: 20px; font-size: 11px; font-weight: 700; }\n");
        html.append(".pct-bar { height: 8px; border-radius: 4px; background: #334155; overflow: hidden; margin-top: 4px; }\n");
        html.append(".pct-fill { height: 100%; border-radius: 4px; transition: width 0.5s; }\n");

        html.append(".footer { text-align: center; padding: 30px; color: #475569; font-size: 12px; margin-top: 20px; }\n");
        html.append("@media (max-width: 768px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }\n");
        html.append("@media print { body { background: #fff; color: #000; } .card { border: 1px solid #ccc; } }\n");
        html.append("</style>\n</head>\n<body>\n<div class='container'>\n");

        // ── HEADER ──
        html.append("<div class='header'>\n");
        html.append("<h1>📊 Detailed Attendance Summary</h1>\n");
        html.append("<p>").append(escapeHTML(student.getName())).append(" — ").append(escapeHTML(student.getUsername())).append("</p>\n");
        html.append("<div class='date-range'>📅 ").append(from.format(DATE_FMT)).append(" → ").append(to.format(DATE_FMT));
        html.append("  •  ").append(ChronoUnit.DAYS.between(from, to)).append(" days</div>\n");
        html.append("</div>\n");

        // ── STATS GRID ──
        boolean eligible = overallPct >= 75.0;
        int eligibleCount = 0, atRiskCount = 0;
        for (int[] v : subjectData.values()) {
            double pct = v[1] == 0 ? 100.0 : (double) v[0] / v[1] * 100.0;
            if (pct >= 75.0) eligibleCount++; else atRiskCount++;
        }

        html.append("<div class='stats-grid'>\n");
        html.append(statCard(String.format("%.1f%%", overallPct), "Overall", eligible ? "#10b981" : "#ef4444"));
        html.append(statCard(totalAttended + "/" + totalConducted, "Classes", "#60a5fa"));
        html.append(statCard(String.valueOf(eligibleCount), "Eligible", "#10b981"));
        html.append(statCard(String.valueOf(atRiskCount), "At Risk", atRiskCount > 0 ? "#ef4444" : "#94a3b8"));
        html.append("</div>\n");

        // ── PIE CHART ──
        if (pieChartBase64 != null) {
            html.append("<div class='card'>\n<h3>📈 Overall Distribution</h3>\n");
            html.append("<div class='chart-img'><img src='data:image/png;base64,").append(pieChartBase64).append("' alt='Pie Chart'></div>\n</div>\n");
        }

        // ── SUBJECT TABLE ──
        html.append("<div class='card'>\n<h3>📋 Subject-wise Breakdown</h3>\n");
        html.append("<table>\n<tr><th>Subject</th><th>Attended</th><th>Conducted</th><th>Percentage</th><th>Status</th><th>Action Required</th></tr>\n");
        for (Map.Entry<String, int[]> entry : subjectData.entrySet()) {
            int[] v = entry.getValue();
            double pct = v[1] == 0 ? 100.0 : (double) v[0] / v[1] * 100.0;
            boolean safe = pct >= 75.0;
            String color = safe ? "#10b981" : "#ef4444";
            String badge = safe ? "<span class='badge-safe'>Safe</span>" : "<span class='badge-risk'>At Risk</span>";

            // Find subject for calculator
            String action = "-";
            for (Subject s : student.getSubjects()) {
                if (s.getName().equals(entry.getKey())) {
                    if (safe) {
                        int bunks = AttendanceCalculator.calculateSafeBunks(s);
                        action = "Can miss " + bunks + " more";
                    } else {
                        int recovery = AttendanceCalculator.calculateRecoveryClasses(s);
                        action = "Attend next " + recovery;
                    }
                    break;
                }
            }

            html.append("<tr>");
            html.append("<td><strong>").append(escapeHTML(entry.getKey())).append("</strong></td>");
            html.append("<td>").append(v[0]).append("</td>");
            html.append("<td>").append(v[1]).append("</td>");
            html.append("<td><strong style='color:").append(color).append(";'>").append(String.format("%.1f%%", pct)).append("</strong>");
            html.append("<div class='pct-bar'><div class='pct-fill' style='width:").append(String.format("%.0f", Math.min(pct, 100))).append("%; background:").append(color).append(";'></div></div></td>");
            html.append("<td>").append(badge).append("</td>");
            html.append("<td style='color:#94a3b8; font-size:13px;'>").append(action).append("</td>");
            html.append("</tr>\n");
        }
        html.append("</table>\n</div>\n");

        // ── BAR CHART ──
        if (barChartBase64 != null) {
            html.append("<div class='card'>\n<h3>📊 Subject Comparison</h3>\n");
            html.append("<div class='chart-img'><img src='data:image/png;base64,").append(barChartBase64).append("' alt='Bar Chart'></div>\n</div>\n");
        }

        // ── TREND CHART ──
        if (trendChartBase64 != null) {
            html.append("<div class='card'>\n<h3>📈 Monthly Attendance Trend</h3>\n");
            html.append("<div class='chart-img'><img src='data:image/png;base64,").append(trendChartBase64).append("' alt='Trend Chart'></div>\n</div>\n");
        }

        // ── HEATMAP ──
        if (heatmapBase64 != null) {
            html.append("<div class='card'>\n<h3>🗓️ Day-wise Attendance Heatmap</h3>\n");
            html.append("<div class='chart-img'><img src='data:image/png;base64,").append(heatmapBase64).append("' alt='Heatmap'></div>\n</div>\n");
        }

        // ── FOOTER ──
        html.append("<div class='footer'>");
        html.append("<p>Generated by <strong>Attendance & Eligibility Management System</strong></p>");
        html.append("<p>© ").append(LocalDate.now().getYear()).append(" — Report generated on ").append(LocalDate.now().format(DATE_FMT)).append("</p>");
        html.append("</div>\n");

        html.append("</div>\n</body>\n</html>");

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            writer.write(html.toString());
        }
    }

    private static String statCard(String value, String label, String color) {
        return "<div class='stat-card'><div class='stat-value' style='color:" + color + ";'>" + value + "</div><div class='stat-label'>" + label + "</div></div>\n";
    }

    // ═══════════════════════════════════════════
    // CHART: PIE
    // ═══════════════════════════════════════════

    private static String generatePieChart(int attended, int absent) {
        if (attended == 0 && absent == 0) return null;

        int w = 700, h = 400;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupGraphics(g);

        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(0, 0, w, h, 20, 20);

        int total = attended + absent;
        int presAngle = (int) Math.round((attended * 360.0) / total);

        int cx = 200, cy = 200, r = 150;

        // Shadow
        g.setColor(new Color(0, 0, 0, 40));
        g.fillOval(cx - r + 4, cy - r + 4, r * 2, r * 2);

        // Present
        g.setColor(new Color(16, 185, 129));
        g.fillArc(cx - r, cy - r, r * 2, r * 2, 90, -presAngle);

        // Absent
        g.setColor(new Color(239, 68, 68));
        g.fillArc(cx - r, cy - r, r * 2, r * 2, 90 - presAngle, -(360 - presAngle));

        // Center circle (donut)
        g.setColor(new Color(30, 41, 59));
        g.fillOval(cx - 60, cy - 60, 120, 120);

        // Center text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 28));
        String pctText = String.format("%.0f%%", (double) attended / total * 100);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(pctText, cx - fm.stringWidth(pctText) / 2, cy + 10);

        // Legend
        int lx = 420, ly = 140;
        g.setFont(new Font("Segoe UI", Font.BOLD, 18));

        g.setColor(new Color(16, 185, 129));
        g.fillRoundRect(lx, ly, 22, 22, 6, 6);
        g.setColor(new Color(226, 232, 240));
        g.drawString("Present (" + attended + ")", lx + 34, ly + 18);

        ly += 45;
        g.setColor(new Color(239, 68, 68));
        g.fillRoundRect(lx, ly, 22, 22, 6, 6);
        g.setColor(new Color(226, 232, 240));
        g.drawString("Absent (" + absent + ")", lx + 34, ly + 18);

        g.dispose();
        return toBase64(img);
    }

    // ═══════════════════════════════════════════
    // CHART: BAR
    // ═══════════════════════════════════════════

    private static String generateBarChart(Map<String, int[]> data) {
        if (data.isEmpty()) return null;

        int n = data.size();
        int w = Math.max(600, n * 100 + 100), h = 400;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupGraphics(g);

        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(0, 0, w, h, 20, 20);

        int chartLeft = 60, chartRight = w - 30, chartTop = 30, chartBottom = h - 60;
        int chartWidth = chartRight - chartLeft;
        int chartHeight = chartBottom - chartTop;

        // Y-axis gridlines
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.setColor(new Color(71, 85, 105));
        for (int pct = 0; pct <= 100; pct += 25) {
            int y = chartBottom - (int) (pct / 100.0 * chartHeight);
            g.setColor(new Color(51, 65, 85));
            g.drawLine(chartLeft, y, chartRight, y);
            g.setColor(new Color(148, 163, 184));
            g.drawString(pct + "%", 20, y + 5);
        }

        // 75% threshold line
        int threshY = chartBottom - (int) (0.75 * chartHeight);
        g.setColor(new Color(239, 68, 68, 120));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{8, 4}, 0));
        g.drawLine(chartLeft, threshY, chartRight, threshY);
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(239, 68, 68));
        g.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g.drawString("75%", chartRight - 28, threshY - 5);

        // Bars
        int barSpacing = chartWidth / n;
        int barWidth = Math.min(barSpacing - 20, 60);
        int i = 0;

        for (Map.Entry<String, int[]> entry : data.entrySet()) {
            int[] v = entry.getValue();
            double pct = v[1] == 0 ? 100.0 : (double) v[0] / v[1] * 100.0;
            int barH = (int) (pct / 100.0 * chartHeight);
            int x = chartLeft + i * barSpacing + (barSpacing - barWidth) / 2;
            int y = chartBottom - barH;

            Color barColor = CHART_COLORS[i % CHART_COLORS.length];
            g.setColor(barColor);
            g.fillRoundRect(x, y, barWidth, barH, 8, 8);

            // Value on top
            g.setColor(new Color(226, 232, 240));
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            String valStr = String.format("%.0f%%", pct);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(valStr, x + (barWidth - fm.stringWidth(valStr)) / 2, y - 6);

            // Label below
            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            fm = g.getFontMetrics();
            String label = entry.getKey().length() > 10 ? entry.getKey().substring(0, 10) + ".." : entry.getKey();
            g.drawString(label, x + (barWidth - fm.stringWidth(label)) / 2, chartBottom + 18);

            i++;
        }

        g.dispose();
        return toBase64(img);
    }

    // ═══════════════════════════════════════════
    // CHART: TREND LINE
    // ═══════════════════════════════════════════

    private static String generateTrendChart(Map<String, Map<String, int[]>> monthlyData) {
        if (monthlyData.isEmpty()) return null;

        int w = 700, h = 400;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupGraphics(g);

        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(0, 0, w, h, 20, 20);

        int chartLeft = 55, chartRight = w - 30, chartTop = 30, chartBottom = h - 50;
        int chartWidth = chartRight - chartLeft;
        int chartHeight = chartBottom - chartTop;

        // Y-axis gridlines
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        for (int pct = 0; pct <= 100; pct += 25) {
            int y = chartBottom - (int) (pct / 100.0 * chartHeight);
            g.setColor(new Color(51, 65, 85));
            g.drawLine(chartLeft, y, chartRight, y);
            g.setColor(new Color(148, 163, 184));
            g.drawString(pct + "%", 15, y + 5);
        }

        // 75% line
        int threshY = chartBottom - (int) (0.75 * chartHeight);
        g.setColor(new Color(239, 68, 68, 100));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{6, 4}, 0));
        g.drawLine(chartLeft, threshY, chartRight, threshY);
        g.setStroke(new BasicStroke(1));

        // Compute overall monthly percentage
        List<String> months = new ArrayList<>(monthlyData.keySet());
        int n = months.size();
        if (n < 2) {
            g.setColor(new Color(148, 163, 184));
            g.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            g.drawString("Need at least 2 months of data for trend", w / 2 - 120, h / 2);
            g.dispose();
            return toBase64(img);
        }

        // Plot overall trend
        int[] xPoints = new int[n];
        int[] yPoints = new int[n];
        double[] pcts = new double[n];

        for (int i = 0; i < n; i++) {
            Map<String, int[]> subjectMap = monthlyData.get(months.get(i));
            int mAtt = 0, mCond = 0;
            for (int[] v : subjectMap.values()) { mAtt += v[0]; mCond += v[1]; }
            pcts[i] = mCond == 0 ? 100.0 : (double) mAtt / mCond * 100.0;
            xPoints[i] = chartLeft + (int) ((double) i / (n - 1) * chartWidth);
            yPoints[i] = chartBottom - (int) (pcts[i] / 100.0 * chartHeight);
        }

        // Line
        g.setColor(new Color(59, 130, 246));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < n - 1; i++) {
            g.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
        }

        // Points + labels
        g.setStroke(new BasicStroke(1));
        for (int i = 0; i < n; i++) {
            // Point
            g.setColor(new Color(59, 130, 246));
            g.fillOval(xPoints[i] - 6, yPoints[i] - 6, 12, 12);
            g.setColor(new Color(30, 41, 59));
            g.fillOval(xPoints[i] - 3, yPoints[i] - 3, 6, 6);

            // Value
            g.setColor(new Color(226, 232, 240));
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g.drawString(String.format("%.0f%%", pcts[i]), xPoints[i] - 12, yPoints[i] - 12);

            // Month label
            g.setColor(new Color(148, 163, 184));
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g.drawString(months.get(i), xPoints[i] - 15, chartBottom + 16);
        }

        g.dispose();
        return toBase64(img);
    }

    // ═══════════════════════════════════════════
    // CHART: DAY HEATMAP
    // ═══════════════════════════════════════════

    private static String generateDayHeatmap(Map<String, int[]> dayData) {
        int w = 700, h = 200;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupGraphics(g);

        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(0, 0, w, h, 20, 20);

        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        String[] labels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        int cellW = 90, cellH = 70;
        int startX = (w - days.length * (cellW + 10)) / 2;
        int startY = 50;

        g.setColor(new Color(148, 163, 184));
        g.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g.drawString("Attendance rate by day of week", w / 2 - 110, 30);

        for (int i = 0; i < days.length; i++) {
            int[] v = dayData.getOrDefault(days[i], new int[]{0, 0});
            int total = v[0] + v[1];
            double pct = total == 0 ? 100.0 : (double) v[0] / total * 100.0;

            // Gradient from red to green
            Color cellColor;
            if (pct >= 85) cellColor = new Color(6, 78, 59);       // dark green
            else if (pct >= 75) cellColor = new Color(21, 128, 61);  // green
            else if (pct >= 60) cellColor = new Color(161, 98, 7);   // amber
            else cellColor = new Color(127, 29, 29);                  // red

            int x = startX + i * (cellW + 10);
            g.setColor(cellColor);
            g.fillRoundRect(x, startY, cellW, cellH, 12, 12);

            // Percentage
            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 20));
            FontMetrics fm = g.getFontMetrics();
            String pctStr = String.format("%.0f%%", pct);
            g.drawString(pctStr, x + (cellW - fm.stringWidth(pctStr)) / 2, startY + 35);

            // Count
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            fm = g.getFontMetrics();
            String countStr = v[0] + "/" + total;
            g.drawString(countStr, x + (cellW - fm.stringWidth(countStr)) / 2, startY + 52);

            // Day label
            g.setColor(new Color(148, 163, 184));
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            fm = g.getFontMetrics();
            g.drawString(labels[i], x + (cellW - fm.stringWidth(labels[i])) / 2, startY + cellH + 18);
        }

        g.dispose();
        return toBase64(img);
    }

    // ═══════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════

    private static void setupGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static String toBase64(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return null;
        }
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
