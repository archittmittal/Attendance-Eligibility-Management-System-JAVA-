package com.attendance;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.DayOfWeek;
import java.util.List;

/**
 * Timetable Dialog — Displays a weekly schedule grid.
 * Shows which subjects are scheduled on each day of the week (Mon–Sat).
 * Color-coded cells with the existing dark theme.
 */
public class TimetableDialog extends JDialog {

    // Colors (match existing Catppuccin dark theme)
    private static final Color BG_COLOR = new Color(30, 30, 46);
    private static final Color CARD_COLOR = new Color(49, 50, 68);
    private static final Color HEADER_COLOR = new Color(24, 24, 37);
    private static final Color ACCENT_COLOR = new Color(137, 180, 250);
    private static final Color TEXT_COLOR = new Color(205, 214, 244);
    private static final Color SUBTEXT_COLOR = new Color(147, 153, 178);
    private static final Color SURFACE = new Color(69, 71, 90);

    // Subject colors for visual distinction
    private static final Color[] SUBJECT_COLORS = {
            new Color(137, 180, 250), // Blue
            new Color(166, 227, 161), // Green
            new Color(249, 226, 175), // Yellow
            new Color(243, 139, 168), // Red/Pink
            new Color(203, 166, 247), // Mauve
            new Color(148, 226, 213), // Teal
            new Color(250, 179, 135), // Peach
            new Color(180, 190, 254), // Lavender
    };

    public TimetableDialog(Frame owner, WeeklySchedule schedule, List<Subject> subjects) {
        super(owner, "📋 Weekly Timetable", true);
        setSize(700, 450);
        setLocationRelativeTo(owner);
        setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        // ── Header ──
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));
        JLabel titleLabel = new JLabel("📋 Weekly Timetable");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(ACCENT_COLOR);
        headerPanel.add(titleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ── Build Table Model ──
        // Days as rows (Mon–Sat), one column per subject slot
        DayOfWeek[] days = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        };
        String[] dayLabels = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

        // Find the max number of subjects on any single day
        int maxSubjects = 1;
        for (DayOfWeek day : days) {
            List<Subject> daySubjects = schedule.getSubjectsOn(day);
            if (daySubjects != null && daySubjects.size() > maxSubjects) {
                maxSubjects = daySubjects.size();
            }
        }

        // Build column headers
        String[] columns = new String[maxSubjects + 1];
        columns[0] = "Day";
        for (int i = 0; i < maxSubjects; i++) {
            columns[i + 1] = "Subject " + (i + 1);
        }

        // Build data
        Object[][] data = new Object[days.length][maxSubjects + 1];
        for (int r = 0; r < days.length; r++) {
            data[r][0] = dayLabels[r];
            List<Subject> daySubjects = schedule.getSubjectsOn(days[r]);
            if (daySubjects != null) {
                for (int c = 0; c < daySubjects.size(); c++) {
                    data[r][c + 1] = daySubjects.get(c).getName();
                }
            }
            // Fill remaining with empty
            for (int c = (daySubjects != null ? daySubjects.size() : 0); c < maxSubjects; c++) {
                data[r][c + 1] = "";
            }
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setBackground(CARD_COLOR);
        table.setForeground(TEXT_COLOR);
        table.setGridColor(SURFACE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(50);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.getTableHeader().setBackground(HEADER_COLOR);
        table.getTableHeader().setForeground(ACCENT_COLOR);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setReorderingAllowed(false);

        // Custom cell renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);

                if (col == 0) {
                    // Day column
                    c.setBackground(HEADER_COLOR);
                    c.setForeground(ACCENT_COLOR);
                    setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else {
                    String text = (value != null) ? value.toString() : "";
                    if (!text.isEmpty()) {
                        // Subject cell — color based on subject index
                        int subjectIndex = getSubjectIndex(subjects, text);
                        Color subjectColor = SUBJECT_COLORS[subjectIndex % SUBJECT_COLORS.length];
                        c.setBackground(darken(subjectColor, 0.3f));
                        c.setForeground(subjectColor);
                        setFont(new Font("Segoe UI", Font.BOLD, 13));
                    } else {
                        // Empty cell
                        c.setBackground(CARD_COLOR);
                        c.setForeground(SUBTEXT_COLOR);
                        setFont(new Font("Segoe UI", Font.ITALIC, 12));
                        setText("—");
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        scrollPane.getViewport().setBackground(BG_COLOR);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // ── Legend Panel ──
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        legendPanel.setBackground(BG_COLOR);
        legendPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 12, 10));

        for (int i = 0; i < subjects.size(); i++) {
            JLabel dot = new JLabel("● " + subjects.get(i).getName());
            dot.setForeground(SUBJECT_COLORS[i % SUBJECT_COLORS.length]);
            dot.setFont(new Font("Segoe UI", Font.BOLD, 12));
            legendPanel.add(dot);
        }

        if (subjects.isEmpty()) {
            JLabel emptyLabel = new JLabel("No subjects added yet.");
            emptyLabel.setForeground(SUBTEXT_COLOR);
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            legendPanel.add(emptyLabel);
        }

        mainPanel.add(legendPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    /**
     * Get the index of a subject by name in the subjects list (for consistent
     * coloring).
     */
    private static int getSubjectIndex(List<Subject> subjects, String name) {
        for (int i = 0; i < subjects.size(); i++) {
            if (subjects.get(i).getName().equals(name)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Darken a color by a factor (0.0 = no change, 1.0 = black).
     */
    private static Color darken(Color color, float factor) {
        int r = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int g = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int b = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(r, g, b);
    }
}
