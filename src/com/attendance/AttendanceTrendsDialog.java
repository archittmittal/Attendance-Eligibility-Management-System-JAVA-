package com.attendance;

import java.awt.*;
import java.awt.geom.Path2D;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Attendance Trends Dialog — Displays weekly attendance trend line chart.
 * Custom-painted using Graphics2D (no external library needed).
 *
 * Features:
 * - One line per subject (different Catppuccin colors)
 * - Dashed 75% threshold line
 * - X-axis: weeks, Y-axis: attendance % (0–100)
 * - Mouse hover shows exact values via tooltip
 * - Overall combined trend line
 */
public class AttendanceTrendsDialog extends JDialog {

    // Colors (resolved via ThemeManager for dark/light support)
    private static final Color BG_COLOR = ThemeManager.getBgColor();
    private static final Color CARD_COLOR = ThemeManager.getCardColor();
    private static final Color HEADER_COLOR = ThemeManager.getHeaderColor();
    private static final Color ACCENT_COLOR = ThemeManager.getAccentColor();
    private static final Color TEXT_COLOR = ThemeManager.getTextColor();
    private static final Color SUBTEXT_COLOR = ThemeManager.getSubtextColor();
    private static final Color SURFACE = ThemeManager.getSurfaceColor();
    private static final Color RED = ThemeManager.getRedColor();
    private static final Color GREEN = ThemeManager.getGreenColor();
    private static final Color YELLOW = ThemeManager.getYellowColor();

    // Subject line colors
    private static final Color[] LINE_COLORS = {
            new Color(137, 180, 250), // Blue
            new Color(166, 227, 161), // Green
            new Color(249, 226, 175), // Yellow
            new Color(243, 139, 168), // Pink
            new Color(203, 166, 247), // Mauve
            new Color(148, 226, 213), // Teal
            new Color(250, 179, 135), // Peach
            new Color(180, 190, 254), // Lavender
    };

    public AttendanceTrendsDialog(Frame owner, Student student) {
        super(owner, "📊 Attendance Trends", true);
        setSize(850, 550);
        setLocationRelativeTo(owner);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        // ── Header ──
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        JLabel titleLabel = new JLabel("📊 Attendance Trends — Weekly Breakdown");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(ACCENT_COLOR);
        headerPanel.add(titleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ── Chart Panel ──
        List<Subject> subjects = student.getSubjects();
        Map<Subject, List<double[]>> weeklyData = computeWeeklyData(subjects);

        // Calculate max week for sizing
        int maxWeek = 1;
        for (List<double[]> points : weeklyData.values()) {
            for (double[] p : points) {
                maxWeek = Math.max(maxWeek, (int) p[0]);
            }
        }
        maxWeek = Math.max(maxWeek, 1);

        // Calculate preferred width: enough space for all bars
        int subjectCount = subjects.size();
        int barWidth = Math.max(16, 32 - subjectCount); // wider bars for fewer subjects
        int minBarSpacing = 6;
        int chartWidth = (maxWeek + 1) * subjectCount * (barWidth + minBarSpacing) + 120;
        int chartHeight = 400;

        ChartPanel chartPanel = new ChartPanel(subjects, weeklyData);
        chartPanel.setPreferredSize(new Dimension(chartWidth, chartHeight));

        JScrollPane scrollPane = new JScrollPane(chartPanel,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(CARD_COLOR);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // ── Legend ──
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        legendPanel.setBackground(BG_COLOR);
        legendPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 12, 10));

        // Overall line
        JLabel overallDot = new JLabel("━ Overall");
        overallDot.setForeground(TEXT_COLOR);
        overallDot.setFont(new Font("Segoe UI", Font.BOLD, 12));
        legendPanel.add(overallDot);

        for (int i = 0; i < subjects.size(); i++) {
            JLabel dot = new JLabel("━ " + subjects.get(i).getName());
            dot.setForeground(LINE_COLORS[i % LINE_COLORS.length]);
            dot.setFont(new Font("Segoe UI", Font.BOLD, 12));
            legendPanel.add(dot);
        }

        // Threshold legend
        JLabel threshDot = new JLabel("┈ 75% Threshold");
        threshDot.setForeground(RED);
        threshDot.setFont(new Font("Segoe UI", Font.BOLD, 12));
        legendPanel.add(threshDot);

        mainPanel.add(legendPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    /**
     * Compute weekly attendance percentages for each subject.
     * Returns Map: Subject -> List of [weekNumber, cumulativePercentage].
     */
    private static Map<Subject, List<double[]>> computeWeeklyData(List<Subject> subjects) {
        Map<Subject, List<double[]>> result = new LinkedHashMap<>();

        for (Subject subject : subjects) {
            List<AttendanceRecord> records = subject.getAttendanceHistory();
            if (records.isEmpty()) {
                result.put(subject, new ArrayList<>());
                continue;
            }

            // Sort by date
            List<AttendanceRecord> sorted = new ArrayList<>(records);
            sorted.sort(Comparator.comparing(r -> r.getDate() != null ? r.getDate() : LocalDate.MIN));

            // Remove records with null dates
            sorted.removeIf(r -> r.getDate() == null);
            if (sorted.isEmpty()) {
                result.put(subject, new ArrayList<>());
                continue;
            }

            LocalDate firstDate = sorted.get(0).getDate();
            List<double[]> weekPoints = new ArrayList<>();

            int cumAttended = 0;
            int cumConducted = 0;
            int currentWeek = 0;

            for (AttendanceRecord record : sorted) {
                int weekNum = (int) ChronoUnit.WEEKS.between(firstDate, record.getDate());
                cumConducted++;
                if (record.isPresent())
                    cumAttended++;

                // If we've moved to a new week or this is the last record, log the point
                if (weekNum > currentWeek || record == sorted.get(sorted.size() - 1)) {
                    double pct = (cumConducted == 0) ? 100.0 : (double) cumAttended / cumConducted * 100.0;
                    weekPoints.add(new double[] { weekNum, pct });
                    currentWeek = weekNum;
                }
            }

            // Ensure we have at least final cumulative point
            if (weekPoints.isEmpty()) {
                double pct = (cumConducted == 0) ? 100.0 : (double) cumAttended / cumConducted * 100.0;
                weekPoints.add(new double[] { 0, pct });
            }

            result.put(subject, weekPoints);
        }

        return result;
    }

    /**
     * Custom chart panel that draws the line chart.
     */
    private static class ChartPanel extends JPanel {
        private final List<Subject> subjects;
        private final Map<Subject, List<double[]>> weeklyData;

        // Chart area bounds (computed in paint)
        private int chartLeft, chartTop, chartRight, chartBottom;
        private int maxWeek;

        // Tooltip
        private String tooltipText = null;
        private int tooltipX = -1, tooltipY = -1;

        ChartPanel(List<Subject> subjects, Map<Subject, List<double[]>> weeklyData) {
            this.subjects = subjects;
            this.weeklyData = weeklyData;
            setBackground(CARD_COLOR);
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            addMouseMotionListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    updateTooltip(e.getX(), e.getY());
                }
            });
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    tooltipText = null;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            // Chart area
            chartLeft = 55;
            chartTop = 15;
            chartRight = getWidth() - 25;
            chartBottom = getHeight() - 35;
            int chartWidth = chartRight - chartLeft;
            int chartHeight = chartBottom - chartTop;

            // Find max week across all subjects
            maxWeek = 1;
            for (List<double[]> points : weeklyData.values()) {
                for (double[] p : points) {
                    maxWeek = Math.max(maxWeek, (int) p[0]);
                }
            }
            maxWeek = Math.max(maxWeek, 1);

            // ── Grid Lines ──
            g2.setColor(SURFACE);
            g2.setStroke(new BasicStroke(1f));
            // Horizontal lines at 0%, 25%, 50%, 75%, 100%
            for (int pct = 0; pct <= 100; pct += 25) {
                int y = chartBottom - (int) (pct / 100.0 * chartHeight);
                g2.setColor(SURFACE);
                g2.drawLine(chartLeft, y, chartRight, y);
                // Label
                g2.setColor(SUBTEXT_COLOR);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.drawString(pct + "%", chartLeft - 35, y + 4);
            }

            // X-axis labels (week numbers)
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(ACCENT_COLOR);
            int step = Math.max(1, maxWeek / 10);
            for (int w = 0; w <= maxWeek; w += step) {
                int x = chartLeft + (int) ((double) w / maxWeek * chartWidth);
                g2.drawString("Week " + (w + 1), x - 16, chartBottom + 22);
            }

            // ── 75% Threshold Line ──
            int threshY = chartBottom - (int) (75.0 / 100.0 * chartHeight);
            g2.setColor(RED);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[] { 6.0f, 4.0f }, 0.0f));
            g2.drawLine(chartLeft, threshY, chartRight, threshY);

            // ── Subject Bars ──
            int subjectCount = subjects.size();
            int barWidth = Math.max(8, chartWidth / (maxWeek * subjectCount + subjectCount));
            int colorIdx = 0;
            for (Subject subject : subjects) {
                List<double[]> points = weeklyData.get(subject);
                if (points == null || points.isEmpty()) {
                    colorIdx++;
                    continue;
                }
                Color barColor = LINE_COLORS[colorIdx % LINE_COLORS.length];
                for (double[] p : points) {
                    int week = (int) p[0];
                    double pct = p[1];
                    int x = chartLeft + week * (barWidth * subjectCount + 4) + colorIdx * barWidth;
                    int y = chartBottom - (int) (pct / 100.0 * chartHeight);
                    int height = chartBottom - y;
                    // Color code: green if >=75, yellow if 60-75, red if <60
                    Color statusColor = pct >= 75 ? GREEN : (pct >= 60 ? YELLOW : RED);
                    g2.setColor(barColor);
                    g2.fillRect(x, y, barWidth, height);
                    g2.setColor(statusColor);
                    g2.drawRect(x, y, barWidth, height);
                    // Value label
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    g2.setColor(barColor);
                    g2.drawString(String.format("%.1f%%", pct), x + 2, y - 6);
                }
                colorIdx++;
            }

            // ── Overall Combined Line ──
            List<double[]> overallPoints = computeOverallTrend();
            if (overallPoints.size() >= 2) {
                g2.setColor(TEXT_COLOR);
                g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D.Double overallPath = new Path2D.Double();
                boolean first = true;
                for (double[] p : overallPoints) {
                    int x = chartLeft + (int) (p[0] / maxWeek * chartWidth);
                    int y = chartBottom - (int) (p[1] / 100.0 * chartHeight);
                    if (first) {
                        overallPath.moveTo(x, y);
                        first = false;
                    } else {
                        overallPath.lineTo(x, y);
                    }
                }
                g2.draw(overallPath);
            }

            // ── Axes ──
            g2.setColor(ACCENT_COLOR);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(chartLeft, chartTop, chartLeft, chartBottom);
            g2.drawLine(chartLeft, chartBottom, chartRight, chartBottom);
            // Axis labels
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(HEADER_COLOR);
            g2.drawString("Weeks", (chartLeft + chartRight) / 2 - 30, chartBottom + 40);
            g2.rotate(-Math.PI / 2);
            g2.drawString("Attendance %", -((chartTop + chartBottom) / 2 + 40), chartLeft - 50);
            g2.rotate(Math.PI / 2);

            // ── Tooltip ──
            if (tooltipText != null) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(tooltipText) + 12;
                int th = fm.getHeight() + 8;
                int tx = Math.min(tooltipX + 15, getWidth() - tw - 5);
                int ty = Math.max(tooltipY - th - 5, 5);

                g2.setColor(HEADER_COLOR);
                g2.fillRoundRect(tx, ty, tw, th, 6, 6);
                g2.setColor(ACCENT_COLOR);
                g2.drawRoundRect(tx, ty, tw, th, 6, 6);
                g2.setColor(TEXT_COLOR);
                g2.drawString(tooltipText, tx + 6, ty + th - 6);
            }

            g2.dispose();
        }

        /**
         * Compute an overall combined attendance trend.
         */
        private List<double[]> computeOverallTrend() {
            // Find the global earliest date across all subjects first
            LocalDate refDate = null;
            for (Subject subject : subjects) {
                for (AttendanceRecord record : subject.getAttendanceHistory()) {
                    if (record.getDate() != null) {
                        if (refDate == null || record.getDate().isBefore(refDate)) {
                            refDate = record.getDate();
                        }
                    }
                }
            }

            if (refDate == null) {
                return new ArrayList<>();
            }

            // Merge all records, group by week relative to global refDate
            TreeMap<Integer, int[]> weekMap = new TreeMap<>(); // week -> [attended, conducted]

            for (Subject subject : subjects) {
                List<AttendanceRecord> records = subject.getAttendanceHistory();
                for (AttendanceRecord record : records) {
                    if (record.getDate() == null)
                        continue;
                    int week = (int) ChronoUnit.WEEKS.between(refDate, record.getDate());
                    weekMap.computeIfAbsent(week, k -> new int[] { 0, 0 });
                    weekMap.get(week)[1]++;
                    if (record.isPresent())
                        weekMap.get(week)[0]++;
                }
            }

            // Convert to cumulative percentages
            List<double[]> result = new ArrayList<>();
            int cumAtt = 0, cumCond = 0;
            for (Map.Entry<Integer, int[]> entry : weekMap.entrySet()) {
                cumAtt += entry.getValue()[0];
                cumCond += entry.getValue()[1];
                double pct = (cumCond == 0) ? 100.0 : (double) cumAtt / cumCond * 100.0;
                result.add(new double[] { entry.getKey(), pct });
            }
            return result;
        }

        private void updateTooltip(int mx, int my) {
            int chartWidth = chartRight - chartLeft;
            int chartHeight = chartBottom - chartTop;
            if (chartWidth <= 0 || chartHeight <= 0)
                return;

            // Find closest data point
            double minDist = 20; // pixel threshold
            String best = null;

            int colorIdx = 0;
            for (Subject subject : subjects) {
                List<double[]> points = weeklyData.get(subject);
                if (points == null) {
                    colorIdx++;
                    continue;
                }
                for (double[] p : points) {
                    int x = chartLeft + (int) (p[0] / maxWeek * chartWidth);
                    int y = chartBottom - (int) (p[1] / 100.0 * chartHeight);
                    double dist = Math.sqrt((mx - x) * (mx - x) + (my - y) * (my - y));
                    if (dist < minDist) {
                        minDist = dist;
                        best = String.format("%s — W%d: %.1f%%", subject.getName(), (int) p[0] + 1, p[1]);
                    }
                }
                colorIdx++;
            }

            tooltipText = best;
            tooltipX = mx;
            tooltipY = my;
            repaint();
        }
    }
}
