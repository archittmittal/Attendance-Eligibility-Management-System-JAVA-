package com.attendance;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/**
 * Subject detail/edit dialog.
 * Shows: name editing, date-wise attendance table, add/toggle/delete records.
 */
public class SubjectDetailDialog extends JDialog {
    private Subject subject;
    private Student student;
    private DefaultTableModel tableModel;
    private boolean changed = false;

    // Colors
    private static final Color BG_COLOR = new Color(30, 30, 46);
    private static final Color CARD_COLOR = new Color(49, 50, 68);
    private static final Color ACCENT_COLOR = new Color(137, 180, 250);
    private static final Color TEXT_COLOR = new Color(205, 214, 244);
    private static final Color SUBTEXT_COLOR = new Color(147, 153, 178);
    private static final Color FIELD_BG = new Color(69, 71, 90);
    private static final Color GREEN = new Color(166, 227, 161);
    private static final Color RED = new Color(243, 139, 168);
    private static final Color SURFACE = new Color(69, 71, 90);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd (EEE)");

    public SubjectDetailDialog(Frame owner, Subject subject, Student student) {
        super(owner, "📋 " + subject.getName() + " — Manage Attendance & History", true);
        this.subject = subject;
        this.student = student;

        setSize(620, 560);
        setLocationRelativeTo(owner);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 5));
        mainPanel.setBackground(BG_COLOR);

        // ═══ Top: Subject Name Edit ═══
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBackground(CARD_COLOR);
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel nameLabel = new JLabel("Subject:");
        nameLabel.setForeground(SUBTEXT_COLOR);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JTextField nameField = new JTextField(subject.getName());
        nameField.setBackground(FIELD_BG);
        nameField.setForeground(TEXT_COLOR);
        nameField.setCaretColor(TEXT_COLOR);
        nameField.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(88, 91, 112)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JButton renameBtn = createSmallButton("Rename", ACCENT_COLOR, BG_COLOR);
        renameBtn.addActionListener(e -> {
            String newName = nameField.getText().trim();
            if (!newName.isEmpty() && !newName.equals(subject.getName())) {
                subject.setName(newName);
                DatabaseManager.getInstance().updateSubject(
                        subject.getId(), newName, subject.getClassesPerWeek());
                setTitle("📋 " + newName + " — Attendance Details");
                changed = true;
            }
        });

        // Summary stats
        JLabel summaryLabel = new JLabel(getSummaryText());
        summaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        summaryLabel.setForeground(subject.getAttendancePercentage() >= 75 ? GREEN : RED);

        JPanel nameRow = new JPanel(new BorderLayout(8, 0));
        nameRow.setOpaque(false);
        nameRow.add(nameLabel, BorderLayout.WEST);
        nameRow.add(nameField, BorderLayout.CENTER);
        nameRow.add(renameBtn, BorderLayout.EAST);

        topPanel.add(nameRow, BorderLayout.NORTH);
        topPanel.add(summaryLabel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // ═══ Center: Date-wise Attendance Table ═══
        String[] columns = { "Date", "Day", "Status", "Action" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3; // Only action column
            }
        };
        populateTable();

        JTable table = new JTable(tableModel);
        table.setBackground(CARD_COLOR);
        table.setForeground(TEXT_COLOR);
        table.setGridColor(SURFACE);
        table.setSelectionBackground(SURFACE);
        table.setSelectionForeground(ACCENT_COLOR);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.getTableHeader().setBackground(new Color(24, 24, 37));
        table.getTableHeader().setForeground(TEXT_COLOR);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Color-code the Status column
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if ("✅ Present".equals(value)) {
                    c.setForeground(GREEN);
                } else {
                    c.setForeground(RED);
                }
                c.setBackground(isSelected ? SURFACE : CARD_COLOR);
                return c;
            }
        });

        // Action column — toggle button
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                c.setForeground(ACCENT_COLOR);
                c.setBackground(isSelected ? SURFACE : CARD_COLOR);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        // Click on action column to toggle
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                int col = table.columnAtPoint(evt.getPoint());
                if (col == 3 && row >= 0) {
                    String dateStr = (String) tableModel.getValueAt(row, 0);
                    LocalDate date = LocalDate.parse(dateStr);
                    String currentStatus = (String) tableModel.getValueAt(row, 2);
                    boolean wasPresent = "✅ Present".equals(currentStatus);

                    // Toggle
                    subject.addClass(date, !wasPresent);
                    DatabaseManager.getInstance().saveAttendanceRecord(
                            subject.getId(), date, !wasPresent);

                    // Refresh table
                    tableModel.setValueAt(!wasPresent ? "✅ Present" : "❌ Absent", row, 2);
                    tableModel.setValueAt(!wasPresent ? "→ Mark Absent" : "→ Mark Present", row, 3);
                    summaryLabel.setText(getSummaryText());
                    summaryLabel.setForeground(subject.getAttendancePercentage() >= 75 ? GREEN : RED);
                    changed = true;
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        tableScroll.getViewport().setBackground(CARD_COLOR);

        // ═══ Calendar Heat-Map View ═══
        CalendarHeatMapPanel calendarPanel = new CalendarHeatMapPanel(subject, student.getHolidayDates());

        JPanel calendarWrapper = new JPanel(new BorderLayout());
        calendarWrapper.setBackground(BG_COLOR);

        // Navigation bar (prev/next month)
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        navPanel.setBackground(BG_COLOR);

        JButton prevBtn = createSmallButton("◀ Prev", ACCENT_COLOR, BG_COLOR);
        JLabel monthLabel = new JLabel(calendarPanel.getMonthLabel());
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        monthLabel.setForeground(TEXT_COLOR);
        JButton nextBtn = createSmallButton("Next ▶", ACCENT_COLOR, BG_COLOR);

        prevBtn.addActionListener(e -> {
            calendarPanel.previousMonth();
            monthLabel.setText(calendarPanel.getMonthLabel());
        });
        nextBtn.addActionListener(e -> {
            calendarPanel.nextMonth();
            monthLabel.setText(calendarPanel.getMonthLabel());
        });

        navPanel.add(prevBtn);
        navPanel.add(monthLabel);
        navPanel.add(nextBtn);

        // Legend
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        legendPanel.setBackground(BG_COLOR);
        legendPanel.add(createLegendDot("Present", GREEN));
        legendPanel.add(createLegendDot("Absent", RED));
        legendPanel.add(createLegendDot("Holiday", new Color(249, 226, 175)));
        legendPanel.add(createLegendDot("No Class", SUBTEXT_COLOR));

        calendarWrapper.add(navPanel, BorderLayout.NORTH);
        calendarWrapper.add(calendarPanel, BorderLayout.CENTER);
        calendarWrapper.add(legendPanel, BorderLayout.SOUTH);

        // ═══ Tabbed Pane ═══
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(CARD_COLOR);
        tabbedPane.setForeground(TEXT_COLOR);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.addTab("📋 Table View", tableScroll);
        tabbedPane.addTab("📅 Calendar View", calendarWrapper);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // ═══ Bottom: Add Date + Delete Record + Close ═══
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(BG_COLOR);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));

        // Add date row
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        addRow.setOpaque(false);

        JTextField dateField = new JTextField(LocalDate.now().toString(), 10);
        dateField.setBackground(FIELD_BG);
        dateField.setForeground(TEXT_COLOR);
        dateField.setEditable(false);
        dateField.setCursor(new Cursor(Cursor.HAND_CURSOR));
        dateField.setCaretColor(TEXT_COLOR);
        dateField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(88, 91, 112)),
                BorderFactory.createEmptyBorder(5, 6, 5, 6)));

        JButton addPresentBtn = createSmallButton("+ Present", GREEN, BG_COLOR);
        JButton addAbsentBtn = createSmallButton("+ Absent", RED, BG_COLOR);
        JButton deleteBtn = createSmallButton("🗑 Delete Selected", new Color(243, 139, 168), BG_COLOR);

        addPresentBtn.addActionListener(e -> addRecord(dateField, true, table, summaryLabel));
        addAbsentBtn.addActionListener(e -> addRecord(dateField, false, table, summaryLabel));
        deleteBtn.addActionListener(e -> deleteSelectedRecord(table, summaryLabel));

        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setForeground(TEXT_COLOR);
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        addRow.add(dateLabel);
        addRow.add(createDatePickerPanel(dateField));
        addRow.add(addPresentBtn);
        addRow.add(addAbsentBtn);
        addRow.add(deleteBtn);

        // Close button
        JPanel closeRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        closeRow.setOpaque(false);

        JButton closeBtn = createSmallButton("Done", ACCENT_COLOR, BG_COLOR);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 30, 8, 30));
        closeBtn.addActionListener(e -> dispose());
        closeRow.add(closeBtn);

        bottomPanel.add(addRow, BorderLayout.NORTH);
        bottomPanel.add(closeRow, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void populateTable() {
        tableModel.setRowCount(0);
        List<AttendanceRecord> records = subject.getAttendanceHistory();

        // Sort by date descending (most recent first)
        records.sort(Comparator.comparing(
                AttendanceRecord::getDate, Comparator.nullsLast(Comparator.reverseOrder())));

        for (AttendanceRecord record : records) {
            LocalDate date = record.getDate();
            String dateStr = date != null ? date.toString() : "N/A";
            String dayStr = date != null ? date.getDayOfWeek().toString().substring(0, 3) : "?";
            String status = record.isPresent() ? "✅ Present" : "❌ Absent";
            String action = record.isPresent() ? "→ Mark Absent" : "→ Mark Present";
            tableModel.addRow(new Object[] { dateStr, dayStr, status, action });
        }
    }

    private void addRecord(JTextField dateField, boolean present, JTable table, JLabel summaryLabel) {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());

            // Validate: date must be within semester
            if (student.isSemesterConfigured()) {
                LocalDate semStart = student.getSemesterStartDate();
                LocalDate semEnd = student.getSemesterEndDate();
                if (semStart != null && date.isBefore(semStart)) {
                    JOptionPane.showMessageDialog(this,
                            "Invalid! Date is before semester start (" + semStart + ").",
                            "Out of Range", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (semEnd != null && date.isAfter(semEnd)) {
                    JOptionPane.showMessageDialog(this,
                            "Invalid! Date is after semester ends (" + semEnd + ").",
                            "Out of Range", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            // Check if this day matches the subject's weekly schedule
            // We need the schedule from MainWindow — use DatabaseManager to check
            java.util.List<String> scheduledDays = getScheduledDaysForSubject(subject.getId());
            String dayName = date.getDayOfWeek().name();
            if (!scheduledDays.contains(dayName)) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "📌 " + date.getDayOfWeek() + " (" + date + ") is NOT a scheduled day for "
                                + subject.getName() + ".\nAdd this as an EXTRA CLASS?",
                        "Extra Class", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (choice != JOptionPane.YES_OPTION)
                    return;
            }

            subject.addClass(date, present);
            DatabaseManager.getInstance().saveAttendanceRecord(subject.getId(), date, present);

            populateTable();
            summaryLabel.setText(getSummaryText());
            summaryLabel.setForeground(subject.getAttendancePercentage() >= 75 ? GREEN : RED);
            changed = true;
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid date format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Get scheduled day names for a subject from DB.
     */
    private java.util.List<String> getScheduledDaysForSubject(int subjectId) {
        java.util.List<String> days = new java.util.ArrayList<>();
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                DatabaseConfig.DB_URL, DatabaseConfig.DB_USER, DatabaseConfig.DB_PASSWORD);
                java.sql.PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT day_of_week FROM weekly_schedule WHERE subject_id = ?")) {
            pstmt.setInt(1, subjectId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    days.add(rs.getString("day_of_week"));
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error loading schedule days: " + e.getMessage());
        }
        return days;
    }

    private void deleteSelectedRecord(JTable table, JLabel summaryLabel) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a row to delete.");
            return;
        }

        String dateStr = (String) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete attendance record for " + dateStr + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            LocalDate date = LocalDate.parse(dateStr);
            subject.removeRecordForDate(date);
            DatabaseManager.getInstance().deleteAttendanceRecord(subject.getId(), date);

            populateTable();
            summaryLabel.setText(getSummaryText());
            summaryLabel.setForeground(subject.getAttendancePercentage() >= 75 ? GREEN : RED);
            changed = true;
        }
    }

    private String getSummaryText() {
        return String.format("Attended: %d / %d   |   %.1f%%   |   %s",
                subject.getClassesAttended(), subject.getClassesConducted(),
                subject.getAttendancePercentage(),
                subject.getAttendancePercentage() >= 75 ? "✅ Eligible" : "⚠️ At Risk");
    }

    private JButton createSmallButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return btn;
    }

    private JLabel createLegendDot(String text, Color color) {
        JLabel label = new JLabel("● " + text);
        label.setForeground(color);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        return label;
    }

    public boolean isChanged() {
        return changed;
    }

    private JPanel createDatePickerPanel(JTextField field) {
        JPanel p = new JPanel(new BorderLayout(5, 0));
        p.setBackground(BG_COLOR);

        JButton btn = new JButton("📅");
        btn.setBackground(new Color(24, 24, 37)); // Match HEADER_COLOR
        btn.setForeground(ACCENT_COLOR);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        btn.addActionListener(e -> {
            LocalDate current = null;
            try {
                if (!field.getText().isEmpty())
                    current = LocalDate.parse(field.getText());
            } catch (Exception ex) {
            }
            LocalDate picked = DatePicker.show(this, current);
            if (picked != null) {
                field.setText(picked.toString());
            }
        });

        // Also open on field click
        field.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                btn.doClick();
            }
        });

        p.add(field, BorderLayout.CENTER);
        p.add(btn, BorderLayout.EAST);
        return p;
    }
}
