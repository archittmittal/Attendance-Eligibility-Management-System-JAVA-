package com.attendance;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * A custom, lightweight Date Picker dialog for Swing.
 * Designed to match the application's modern dark theme.
 */
public class DatePicker extends JDialog {

    private LocalDate selectedDate;
    private YearMonth currentMonth;

    private final JPanel calendarPanel = new JPanel(new GridLayout(0, 7));
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);

    // Theme Colors (Matches MainWindow)
    private static final Color BG_COLOR = new Color(30, 30, 46);
    private static final Color HEADER_COLOR = new Color(24, 24, 37);
    private static final Color ACCENT_COLOR = new Color(137, 180, 250);
    private static final Color TEXT_COLOR = new Color(205, 214, 244);
    private static final Color HOVER_COLOR = new Color(69, 71, 90);

    public DatePicker(Frame parent, LocalDate initialDate) {
        super(parent, "Select Date", true);
        this.selectedDate = (initialDate != null) ? initialDate : LocalDate.now();
        this.currentMonth = YearMonth.from(selectedDate);

        setLayout(new BorderLayout());
        setResizable(false);
        getContentPane().setBackground(BG_COLOR);

        initializeUI();
        updateCalendar();

        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        // ── Header (Month/Year Navigation) ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_COLOR);
        header.setPreferredSize(new Dimension(300, 45));

        JButton prevBtn = createNavButton(" < ");
        JButton nextBtn = createNavButton(" > ");

        monthLabel.setForeground(TEXT_COLOR);
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));

        header.add(prevBtn, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextBtn, BorderLayout.EAST);

        prevBtn.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });

        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });

        add(header, BorderLayout.NORTH);

        // ── Calendar Board ──
        calendarPanel.setBackground(BG_COLOR);
        calendarPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(calendarPanel, BorderLayout.CENTER);
    }

    private void updateCalendar() {
        calendarPanel.removeAll();

        // Update Month Label
        String monthName = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        monthLabel.setText(monthName + " " + currentMonth.getYear());

        // Day Headers (Mon, Tue, ...)
        String[] days = { "Mo", "Tu", "We", "Th", "Fr", "Sa", "Su" };
        for (String day : days) {
            JLabel lbl = new JLabel(day, SwingConstants.CENTER);
            lbl.setForeground(ACCENT_COLOR);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            calendarPanel.add(lbl);
        }

        // Calculation for grid
        LocalDate firstOfMonth = currentMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
        int daysInMonth = currentMonth.lengthOfMonth();

        // Padding for first week
        for (int i = 1; i < dayOfWeek; i++) {
            calendarPanel.add(new JLabel(""));
        }

        // Actual Days
        for (int day = 1; day <= daysInMonth; day++) {
            final int d = day;
            LocalDate date = currentMonth.atDay(day);

            JLabel dayLabel = new JLabel(String.valueOf(day), SwingConstants.CENTER);
            dayLabel.setPreferredSize(new Dimension(35, 35));
            dayLabel.setOpaque(true);
            dayLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            if (date.equals(selectedDate)) {
                dayLabel.setBackground(ACCENT_COLOR);
                dayLabel.setForeground(BG_COLOR);
            } else if (date.equals(LocalDate.now())) {
                dayLabel.setBackground(HEADER_COLOR);
                dayLabel.setForeground(ACCENT_COLOR);
                dayLabel.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR, 1));
            } else {
                dayLabel.setBackground(BG_COLOR);
                dayLabel.setForeground(TEXT_COLOR);
            }

            dayLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedDate = currentMonth.atDay(d);
                    dispose();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!date.equals(selectedDate)) {
                        dayLabel.setBackground(HOVER_COLOR);
                    }
                    dayLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!date.equals(selectedDate)) {
                        if (date.equals(LocalDate.now())) {
                            dayLabel.setBackground(HEADER_COLOR);
                        } else {
                            dayLabel.setBackground(BG_COLOR);
                        }
                    }
                }
            });

            calendarPanel.add(dayLabel);
        }

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Consolas", Font.BOLD, 16));
        btn.setForeground(ACCENT_COLOR);
        btn.setBackground(HEADER_COLOR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static LocalDate show(Component parent, LocalDate initial) {
        Frame owner = (parent instanceof Frame) ? (Frame) parent : (Frame) SwingUtilities.getWindowAncestor(parent);
        DatePicker picker = new DatePicker(owner, initial);
        picker.setVisible(true);
        return picker.selectedDate;
    }
}
