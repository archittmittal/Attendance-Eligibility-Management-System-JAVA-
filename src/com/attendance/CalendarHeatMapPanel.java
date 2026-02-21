package com.attendance;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calendar Heat-Map Panel — Displays a monthly calendar with color-coded
 * attendance status per day.
 * 
 * Colors:
 * - Green (#a6e3a1) = Present
 * - Red (#f38ba8) = Absent
 * - Gray (#585b70) = No class / Holiday
 * - Current day gets a highlighted border
 * 
 * Navigable by month with prev/next buttons.
 */
public class CalendarHeatMapPanel extends JPanel {

    // Colors (Catppuccin dark theme)
    private static final Color BG_COLOR = new Color(30, 30, 46);
    private static final Color HEADER_COLOR = new Color(24, 24, 37);
    private static final Color ACCENT_COLOR = new Color(137, 180, 250);
    private static final Color TEXT_COLOR = new Color(205, 214, 244);
    private static final Color SUBTEXT_COLOR = new Color(147, 153, 178);
    private static final Color SURFACE = new Color(69, 71, 90);
    private static final Color GREEN = new Color(166, 227, 161);
    private static final Color RED = new Color(243, 139, 168);
    private static final Color YELLOW = new Color(249, 226, 175);

    private static final int CELL_SIZE = 40;
    private static final int CELL_GAP = 4;
    private static final int HEADER_HEIGHT = 25;
    private static final int NAV_HEIGHT = 40;

    private YearMonth currentMonth;
    private final Map<LocalDate, Boolean> attendanceMap; // true=present, false=absent
    private final List<LocalDate> holidayDates;
    private String tooltipText = null;
    private int tooltipX = -1;
    private int tooltipY = -1;

    public CalendarHeatMapPanel(Subject subject, List<LocalDate> holidayDates) {
        this.currentMonth = YearMonth.now();
        this.holidayDates = holidayDates;
        this.attendanceMap = new HashMap<>();

        // Build attendance map from subject history
        for (AttendanceRecord record : subject.getAttendanceHistory()) {
            if (record.getDate() != null) {
                attendanceMap.put(record.getDate(), record.isPresent());
            }
        }

        setBackground(BG_COLOR);
        setPreferredSize(new Dimension(
                7 * (CELL_SIZE + CELL_GAP) + CELL_GAP + 20,
                NAV_HEIGHT + HEADER_HEIGHT + 7 * (CELL_SIZE + CELL_GAP) + 30));

        // Mouse hover for tooltips
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateTooltip(e.getX(), e.getY());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                tooltipText = null;
                repaint();
            }
        });
    }

    /**
     * Navigate to the previous month.
     */
    public void previousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        repaint();
    }

    /**
     * Navigate to the next month.
     */
    public void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        repaint();
    }

    /**
     * Get the current month label for display.
     */
    public String getMonthLabel() {
        return currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + currentMonth.getYear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int startX = 10;
        int startY = 10;

        // ── Day Headers (Mon, Tue, ..., Sun) ──
        String[] dayHeaders = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(SUBTEXT_COLOR);
        for (int i = 0; i < 7; i++) {
            int x = startX + i * (CELL_SIZE + CELL_GAP);
            g2.drawString(dayHeaders[i], x + CELL_SIZE / 2 - g2.getFontMetrics().stringWidth(dayHeaders[i]) / 2,
                    startY + HEADER_HEIGHT - 5);
        }

        // ── Calendar Grid ──
        int daysInMonth = currentMonth.lengthOfMonth();
        LocalDate firstDay = currentMonth.atDay(1);
        // DayOfWeek: MONDAY=1 ... SUNDAY=7
        int startCol = firstDay.getDayOfWeek().getValue() - 1; // 0-indexed (Mon=0)

        LocalDate today = LocalDate.now();
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            int pos = startCol + day - 1;
            int col = pos % 7;
            int row = pos / 7;

            int x = startX + col * (CELL_SIZE + CELL_GAP);
            int y = startY + HEADER_HEIGHT + row * (CELL_SIZE + CELL_GAP);

            // Determine cell color
            Color cellColor;
            Color textColor;

            if (attendanceMap.containsKey(date)) {
                if (attendanceMap.get(date)) {
                    cellColor = darken(GREEN, 0.4f);
                    textColor = GREEN;
                } else {
                    cellColor = darken(RED, 0.4f);
                    textColor = RED;
                }
            } else if (holidayDates.contains(date)) {
                cellColor = darken(YELLOW, 0.5f);
                textColor = YELLOW;
            } else {
                cellColor = SURFACE;
                textColor = SUBTEXT_COLOR;
            }

            // Draw cell background
            g2.setColor(cellColor);
            g2.fillRoundRect(x, y, CELL_SIZE, CELL_SIZE, 8, 8);

            // Highlight today with an accent border
            if (date.equals(today)) {
                g2.setColor(ACCENT_COLOR);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(x, y, CELL_SIZE, CELL_SIZE, 8, 8);
                g2.setStroke(new BasicStroke(1f));
            }

            // Draw day number
            g2.setColor(textColor);
            String dayStr = String.valueOf(day);
            int textWidth = g2.getFontMetrics().stringWidth(dayStr);
            g2.drawString(dayStr, x + (CELL_SIZE - textWidth) / 2, y + CELL_SIZE / 2 + 5);
        }

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
     * Update tooltip based on mouse position.
     */
    private void updateTooltip(int mx, int my) {
        int startX = 10;
        int startY = 10 + HEADER_HEIGHT;

        int col = (mx - startX) / (CELL_SIZE + CELL_GAP);
        int row = (my - startY) / (CELL_SIZE + CELL_GAP);

        if (col < 0 || col >= 7 || row < 0) {
            tooltipText = null;
            repaint();
            return;
        }

        LocalDate firstDay = currentMonth.atDay(1);
        int startCol = firstDay.getDayOfWeek().getValue() - 1;
        int dayNum = row * 7 + col - startCol + 1;

        if (dayNum < 1 || dayNum > currentMonth.lengthOfMonth()) {
            tooltipText = null;
            repaint();
            return;
        }

        LocalDate date = currentMonth.atDay(dayNum);
        String status;
        if (attendanceMap.containsKey(date)) {
            status = attendanceMap.get(date) ? "✅ Present" : "❌ Absent";
        } else if (holidayDates.contains(date)) {
            status = "🏖️ Holiday";
        } else {
            status = "— No class";
        }

        tooltipText = date + " : " + status;
        tooltipX = mx;
        tooltipY = my;
        repaint();
    }

    /**
     * Darken a color.
     */
    private static Color darken(Color c, float factor) {
        return new Color(
                Math.max(0, (int) (c.getRed() * (1 - factor))),
                Math.max(0, (int) (c.getGreen() * (1 - factor))),
                Math.max(0, (int) (c.getBlue() * (1 - factor))));
    }
}
