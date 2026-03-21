package com.attendance;

import java.awt.*;
import java.awt.geom.Path2D;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Attendance Trends Dialog — Displays weekly attendance trend line chart with premium UI.
 * Features:
 * - Interactive legend for subject highlighting
 * - Rounded bars with vertical gradients
 * - Subject initials for easy identification
 * - Glow effect on 75% threshold and overall trend lines
 * - Premium dark/light theme support
 */
public class AttendanceTrendsDialog extends JDialog {

    // Colors (resolved via ThemeManager for dark/light support)
    private static final Color BG_COLOR = ThemeManager.getBgColor();
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

    private Subject highlightedSubject = null;
    private final ChartPanel chartPanel;

    public AttendanceTrendsDialog(Frame owner, Student student) {
        super(owner, "📊 Attendance Trends", true);
        setSize(950, 650);
        setLocationRelativeTo(owner);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        // ── Header ──
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 15, 30));

        JLabel titleLabel = new JLabel("Attendance Analytics");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_COLOR);

        JLabel subTitleLabel = new JLabel("Weekly breakdown and trend analysis of your subjects");
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subTitleLabel.setForeground(SUBTEXT_COLOR);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 5));
        titleGroup.setBackground(BG_COLOR);
        titleGroup.add(titleLabel);
        titleGroup.add(subTitleLabel);
        headerPanel.add(titleGroup, BorderLayout.WEST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ── Data Computation ──
        List<Subject> subjects = student.getSubjects();
        Map<Subject, List<double[]>> weeklyData = computeWeeklyData(subjects);

        // Calculate max week
        int maxWeekNum = 1;
        for (List<double[]> points : weeklyData.values()) {
            for (double[] p : points) {
                maxWeekNum = Math.max(maxWeekNum, (int) p[0]);
            }
        }
        maxWeekNum = Math.max(maxWeekNum, 1);

        // ── Chart Panel ──
        chartPanel = new ChartPanel(subjects, weeklyData);
        int minWidth = 850;
        int calculatedWidth = (maxWeekNum + 1) * subjects.size() * 35 + 200;
        chartPanel.setPreferredSize(new Dimension(Math.max(minWidth, calculatedWidth), 450));

        JScrollPane scrollPane = new JScrollPane(chartPanel,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        scrollPane.getViewport().setBackground(BG_COLOR);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // ── Legend ──
        JPanel legendWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        legendWrapper.setBackground(BG_COLOR);
        legendWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, SURFACE),
                BorderFactory.createEmptyBorder(10, 20, 15, 20)));

        legendWrapper.add(createLegendItem("Overall Trend", TEXT_COLOR, null, false));

        for (int i = 0; i < subjects.size(); i++) {
            Subject s = subjects.get(i);
            Color color = LINE_COLORS[i % LINE_COLORS.length];
            legendWrapper.add(createLegendItem(s.getName(), color, s, true));
        }

        legendWrapper.add(createLegendItem("75% Goal", RED, null, false));

        mainPanel.add(legendWrapper, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createLegendItem(String name, Color color, Subject subject, boolean interactive) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        item.setBackground(BG_COLOR);
        if (interactive) item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(interactive ? "●" : "━");
        icon.setFont(new Font("Segoe UI", Font.BOLD, interactive ? 20 : 18));
        icon.setForeground(color);

        JLabel label = new JLabel(name);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_COLOR);

        item.add(icon);
        item.add(label);

        if (interactive && subject != null) {
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    highlightedSubject = subject;
                    label.setForeground(ACCENT_COLOR);
                    chartPanel.repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    highlightedSubject = null;
                    label.setForeground(TEXT_COLOR);
                    chartPanel.repaint();
                }
            });
        }

        return item;
    }

    private static Map<Subject, List<double[]>> computeWeeklyData(List<Subject> subjects) {
        Map<Subject, List<double[]>> result = new LinkedHashMap<>();
        for (Subject subject : subjects) {
            List<AttendanceRecord> records = subject.getAttendanceHistory();
            if (records.isEmpty()) {
                result.put(subject, new ArrayList<>());
                continue;
            }
            List<AttendanceRecord> sorted = new ArrayList<>(records);
            sorted.sort(Comparator.comparing(r -> r.getDate() != null ? r.getDate() : LocalDate.MIN));
            sorted.removeIf(r -> r.getDate() == null);
            if (sorted.isEmpty()) {
                result.put(subject, new ArrayList<>());
                continue;
            }

            LocalDate firstDate = sorted.get(0).getDate();
            List<double[]> weekPoints = new ArrayList<>();
            int cumAttended = 0, cumConducted = 0, currentWeek = 0;

            for (AttendanceRecord record : sorted) {
                int weekNum = (int) ChronoUnit.WEEKS.between(firstDate, record.getDate());
                cumConducted++;
                if (record.isPresent()) cumAttended++;

                if (weekNum > currentWeek || record == sorted.get(sorted.size() - 1)) {
                    double pct = (double) cumAttended / cumConducted * 100.0;
                    weekPoints.add(new double[] { weekNum, pct });
                    currentWeek = weekNum;
                }
            }
            result.put(subject, weekPoints);
        }
        return result;
    }

    private class ChartPanel extends JPanel {
        private final List<Subject> subjects;
        private final Map<Subject, List<double[]>> weeklyData;
        private int chartLeft, chartTop, chartRight, chartBottom, maxWeek;
        private String tooltipText = null;
        private int tooltipX = -1, tooltipY = -1;

        ChartPanel(List<Subject> subjects, Map<Subject, List<double[]>> weeklyData) {
            this.subjects = subjects;
            this.weeklyData = weeklyData;
            setBackground(BG_COLOR);
            addMouseMotionListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) { updateTooltip(e.getX(), e.getY()); }
            });
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) { tooltipText = null; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            chartLeft = 70; chartTop = 40;
            chartRight = getWidth() - 40;
            chartBottom = getHeight() - 60;
            int chartWidth = chartRight - chartLeft;
            int chartHeight = chartBottom - chartTop;

            maxWeek = 1;
            for (List<double[]> pts : weeklyData.values()) for (double[] p : pts) maxWeek = Math.max(maxWeek, (int) p[0]);
            maxWeek = Math.max(maxWeek, 1);

            // ── Grid ──
            g2.setStroke(new BasicStroke(1f));
            for (int pct = 0; pct <= 100; pct += 25) {
                int y = chartBottom - (int) (pct / 100.0 * chartHeight);
                g2.setColor(new Color(SURFACE.getRed(), SURFACE.getGreen(), SURFACE.getBlue(), 50));
                g2.drawLine(chartLeft, y, chartRight, y);
                g2.setColor(SUBTEXT_COLOR);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString(pct + "%", chartLeft - 45, y + 5);
            }

            // ── Threshold ──
            int threshY = chartBottom - (int) (0.75 * chartHeight);
            g2.setColor(new Color(RED.getRed(), RED.getGreen(), RED.getBlue(), 40));
            g2.setStroke(new BasicStroke(5f));
            g2.drawLine(chartLeft, threshY, chartRight, threshY);
            g2.setColor(RED);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{10f, 6f}, 0f));
            g2.drawLine(chartLeft, threshY, chartRight, threshY);

            // ── Bars ──
            int wGroupW = chartWidth / (maxWeek + 1);
            int barWidth = Math.max(6, (wGroupW - 20) / subjects.size());
            int colorIdx = 0;
            for (Subject s : subjects) {
                List<double[]> points = weeklyData.get(s);
                if (points == null || points.isEmpty()) { colorIdx++; continue; }

                boolean isDimmed = highlightedSubject != null && highlightedSubject != s;
                float alpha = isDimmed ? 0.15f : 1.0f;
                Color baseColor = LINE_COLORS[colorIdx % LINE_COLORS.length];
                Color barColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int)(alpha * 255));

                for (double[] p : points) {
                    int x = chartLeft + (int)p[0] * wGroupW + (wGroupW - subjects.size() * barWidth) / 2 + colorIdx * barWidth;
                    int y = chartBottom - (int) (p[1] / 100.0 * chartHeight);
                    int h = chartBottom - y;

                    GradientPaint gp = new GradientPaint(x, y, barColor, x, chartBottom, new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), (int)(alpha * 30)));
                    g2.setPaint(gp);
                    g2.fillRoundRect(x + 1, y, barWidth - 2, h, 8, 8);
                    g2.setColor(barColor);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x + 1, y, barWidth - 2, h, 8, 8);

                    if (barWidth > 20 && !isDimmed) {
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                        String initial = s.getName().substring(0, Math.min(2, s.getName().length())).toUpperCase();
                        g2.setColor(new Color(255, 255, 255, 160));
                        g2.drawString(initial, x + (barWidth - g2.getFontMetrics().stringWidth(initial)) / 2, y - 8);
                    }
                }
                colorIdx++;
            }

            // ── Overall Line ──
            List<double[]> overall = computeOverallTrend();
            if (overall.size() >= 2 && highlightedSubject == null) {
                g2.setColor(new Color(TEXT_COLOR.getRed(), TEXT_COLOR.getGreen(), TEXT_COLOR.getBlue(), 100));
                g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D.Double path = new Path2D.Double();
                boolean first = true;
                for (double[] p : overall) {
                    int x = chartLeft + (int)p[0] * wGroupW + wGroupW / 2;
                    int y = chartBottom - (int) (p[1] / 100.0 * chartHeight);
                    if (first) { path.moveTo(x, y); first = false; } else path.lineTo(x, y);
                }
                g2.draw(path);
                g2.setColor(TEXT_COLOR);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(path);
            }

            // ── X-Axis Labels ──
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(SUBTEXT_COLOR);
            for (int w = 0; w <= maxWeek; w++) {
                int x = chartLeft + w * wGroupW + wGroupW / 2;
                g2.drawString("Week " + (w + 1), x - 25, chartBottom + 30);
            }

            if (tooltipText != null) renderTooltip(g2);
            g2.dispose();
        }

        private void renderTooltip(Graphics2D g2) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            String[] lines = tooltipText.split("\n");
            int tw = 0; for (String l : lines) tw = Math.max(tw, fm.stringWidth(l));
            tw += 24; int th = (fm.getHeight() + 6) * lines.length + 15;
            int tx = Math.min(tooltipX + 15, getWidth() - tw - 15);
            int ty = Math.max(tooltipY - th - 15, 15);

            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRoundRect(tx + 4, ty + 4, tw, th, 15, 15);
            g2.setColor(HEADER_COLOR);
            g2.fillRoundRect(tx, ty, tw, th, 15, 15);
            g2.setColor(ACCENT_COLOR);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(tx, ty, tw, th, 15, 15);

            g2.setColor(TEXT_COLOR);
            int currY = ty + fm.getAscent() + 10;
            for (String l : lines) { g2.drawString(l, tx + 12, currY); currY += fm.getHeight() + 6; }
        }

        private void updateTooltip(int mx, int my) {
            int chartWidth = chartRight - chartLeft;
            int chartHeight = chartBottom - chartTop;
            String best = null; double minDist = 40;
            int wGroupW = chartWidth / (maxWeek + 1);
            int barWidth = Math.max(6, (wGroupW - 20) / subjects.size());

            int colorIdx = 0;
            for (Subject s : subjects) {
                List<double[]> points = weeklyData.get(s);
                if (points == null) { colorIdx++; continue; }
                for (double[] p : points) {
                    int x = chartLeft + (int)p[0] * wGroupW + (wGroupW - subjects.size() * barWidth) / 2 + colorIdx * barWidth + barWidth / 2;
                    int y = chartBottom - (int) (p[1] / 100.0 * chartHeight);
                    double d = Math.sqrt(Math.pow(mx - x, 2) + Math.pow(my - y, 2));
                    if (d < minDist) {
                        minDist = d;
                        String status = p[1] >= 75 ? "ELIGIBLE ✓" : "LOW ATTENDANCE ⚠";
                        best = String.format("%s\nWeek %d\n%.1f%% - %s", s.getName(), (int)p[0] + 1, p[1], status);
                    }
                }
                colorIdx++;
            }
            tooltipText = best; tooltipX = mx; tooltipY = my; repaint();
        }

        private List<double[]> computeOverallTrend() {
            LocalDate refDate = null;
            for (Subject s : subjects) {
                for (AttendanceRecord r : s.getAttendanceHistory()) {
                    if (r.getDate() != null && (refDate == null || r.getDate().isBefore(refDate))) refDate = r.getDate();
                }
            }
            if (refDate == null) return new ArrayList<>();
            TreeMap<Integer, int[]> weekMap = new TreeMap<>();
            for (Subject s : subjects) {
                for (AttendanceRecord r : s.getAttendanceHistory()) {
                    if (r.getDate() == null) continue;
                    int week = (int) ChronoUnit.WEEKS.between(refDate, r.getDate());
                    weekMap.computeIfAbsent(week, k -> new int[]{0, 0});
                    weekMap.get(week)[1]++;
                    if (r.isPresent()) weekMap.get(week)[0]++;
                }
            }
            List<double[]> res = new ArrayList<>();
            int cA = 0, cC = 0;
            for (Map.Entry<Integer, int[]> e : weekMap.entrySet()) {
                cA += e.getValue()[0]; cC += e.getValue()[1];
                res.add(new double[]{e.getKey(), (double) cA / cC * 100.0});
            }
            return res;
        }
    }
}
