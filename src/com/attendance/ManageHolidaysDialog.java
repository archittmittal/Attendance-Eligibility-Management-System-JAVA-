package com.attendance;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Manage academic holidays.
 * Supports:
 * - Single holiday with custom description
 * - Group holidays (date range) like Holi Break, Diwali Break etc.
 * - Remove individual holidays or entire groups by description
 */
public class ManageHolidaysDialog extends JDialog {
    private Student student;
    private DefaultTableModel tableModel;

    // Colors (dark theme)
    private static final Color BG_COLOR = new Color(30, 30, 46);
    private static final Color CARD_COLOR = new Color(49, 50, 68);
    private static final Color ACCENT_COLOR = new Color(137, 180, 250);
    private static final Color TEXT_COLOR = new Color(205, 214, 244);
    private static final Color FIELD_BG = new Color(69, 71, 90);
    private static final Color ERROR_COLOR = new Color(243, 139, 168);
    private static final Color SUCCESS_COLOR = new Color(166, 227, 161);
    private static final Color SURFACE = new Color(69, 71, 90);
    private static final Color WARN_COLOR = new Color(249, 226, 175);

    public ManageHolidaysDialog(Frame owner, Student student) {
        super(owner, "Manage Academic Calendar", true);
        this.student = student;

        setSize(680, 580);
        setLocationRelativeTo(owner);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        // ── Table ──
        String[] columnNames = { "Date", "Day", "Description" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        loadHolidays();

        JTable table = new JTable(tableModel);
        table.setBackground(CARD_COLOR);
        table.setForeground(TEXT_COLOR);
        table.setSelectionBackground(ACCENT_COLOR);
        table.setSelectionForeground(BG_COLOR);
        table.setGridColor(SURFACE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setBackground(SURFACE);
        table.getTableHeader().setForeground(TEXT_COLOR);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setRowHeight(28);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(CARD_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // ── Input Panel (Bottom) ──
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBackground(BG_COLOR);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));

        // ── Row 1: Description ──
        JPanel descRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        descRow.setBackground(BG_COLOR);

        JLabel descLabel = createLabel("Description:");
        JTextField descField = createTextField(22);
        descField.setToolTipText("e.g. Holi Break, Republic Day, College Fest");
        descField.setText("Official Holiday");

        descRow.add(descLabel);
        descRow.add(descField);
        inputPanel.add(descRow);

        // ── Row 2: Date inputs ──
        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        dateRow.setBackground(BG_COLOR);

        JLabel fromLabel = createLabel("From Date:");
        JTextField fromField = createDateField(LocalDate.now().toString());

        JLabel toLabel = createLabel("To Date:");
        JTextField toField = createDateField("");
        toField.setToolTipText("Leave empty for single day");

        dateRow.add(fromLabel);
        dateRow.add(createDatePickerPanel(fromField));
        dateRow.add(Box.createHorizontalStrut(10));
        dateRow.add(toLabel);
        dateRow.add(createDatePickerPanel(toField));
        inputPanel.add(dateRow);

        // ── Row 3: Action Buttons ──
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        btnRow.setBackground(BG_COLOR);

        JButton addSingleBtn = createButton("+ Add Holiday", SUCCESS_COLOR);
        JButton addGroupBtn = createButton("+ Add Group Holiday", ACCENT_COLOR);
        JButton editBtn = createButton("✏ Edit Selected", new Color(180, 190, 254));
        JButton removeBtn = createButton("Remove Selected", ERROR_COLOR);
        JButton removeGroupBtn = createButton("Remove Group", WARN_COLOR);

        addSingleBtn.setToolTipText("Add a single-day holiday with the given description");
        addGroupBtn.setToolTipText("Add holidays for every date from 'From' to 'To' with the same description");
        editBtn.setToolTipText("Edit the date or description of the selected holiday");
        removeBtn.setToolTipText("Remove just the selected holiday row");
        removeGroupBtn.setToolTipText("Remove ALL holidays with the same description as the selected row");

        btnRow.add(addSingleBtn);
        btnRow.add(addGroupBtn);
        btnRow.add(editBtn);
        btnRow.add(removeBtn);
        btnRow.add(removeGroupBtn);
        inputPanel.add(btnRow);

        // ── Hint label ──
        JLabel hintLabel = new JLabel(
                "  💡 For group holidays (e.g. Holi Break), set both From & To dates and click 'Add Group Holiday'");
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLabel.setForeground(new Color(147, 153, 178));
        inputPanel.add(hintLabel);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        // ── Actions ──

        // Add Single Holiday
        addSingleBtn.addActionListener(e -> {
            try {
                LocalDate date = LocalDate.parse(fromField.getText().trim());
                String description = descField.getText().trim();
                if (description.isEmpty())
                    description = "Official Holiday";

                if (student.getHolidayDates().contains(date)) {
                    JOptionPane.showMessageDialog(this, "Holiday already exists for " + date + ".");
                } else {
                    Holiday holiday = new Holiday(date, description);
                    student.addHoliday(holiday);
                    DatabaseManager.getInstance().addHoliday(student.getId(), date, description);

                    // Auto-clean conflicting attendance records
                    int cleaned = DatabaseManager.getInstance().deleteAttendanceOnDate(student.getId(), date);
                    // Reload subject data to reflect cleaned records
                    if (cleaned > 0) {
                        reloadStudentSubjects();
                        JOptionPane.showMessageDialog(this,
                                "Holiday added! " + cleaned + " conflicting attendance record(s) were removed.",
                                "Holiday Added", JOptionPane.INFORMATION_MESSAGE);
                    }

                    loadHolidays();
                    fromField.setText("");
                    descField.setText("Official Holiday");
                }
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
            }
        });

        // Add Group Holiday (date range)
        addGroupBtn.addActionListener(e -> {
            try {
                LocalDate fromDate = LocalDate.parse(fromField.getText().trim());
                String toText = toField.getText().trim();

                if (toText.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter a 'To Date' for group holiday.\nFor single-day holiday, use 'Add Holiday' instead.");
                    return;
                }

                LocalDate toDate = LocalDate.parse(toText);
                String description = descField.getText().trim();
                if (description.isEmpty())
                    description = "Official Holiday";

                if (toDate.isBefore(fromDate)) {
                    JOptionPane.showMessageDialog(this, "'To Date' cannot be before 'From Date'.");
                    return;
                }

                // Count how many days
                long days = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;

                int confirm = JOptionPane.showConfirmDialog(this,
                        "Add " + days + " holiday(s) from " + fromDate + " to " + toDate
                                + "\nDescription: \"" + description + "\"\n\nProceed?",
                        "Confirm Group Holiday", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    // Add to student model
                    LocalDate current = fromDate;
                    while (!current.isAfter(toDate)) {
                        student.addHoliday(new Holiday(current, description));
                        current = current.plusDays(1);
                    }
                    // Batch save to DB
                    DatabaseManager.getInstance().addHolidayRange(student.getId(), fromDate, toDate, description);

                    // Auto-clean conflicting attendance records in the range
                    int cleaned = DatabaseManager.getInstance().deleteAttendanceInRange(student.getId(), fromDate,
                            toDate);
                    if (cleaned > 0) {
                        reloadStudentSubjects();
                    }

                    loadHolidays();
                    fromField.setText("");
                    toField.setText("");
                    descField.setText("Official Holiday");

                    String msg = days + " holidays added for \"" + description + "\"!";
                    if (cleaned > 0) {
                        msg += "\n" + cleaned + " conflicting attendance record(s) were removed.";
                    }
                    JOptionPane.showMessageDialog(this, msg,
                            "Group Holiday Added", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
            }
        });

        // Edit Selected Holiday
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a holiday to edit.");
                return;
            }

            String oldDateStr = (String) tableModel.getValueAt(row, 0);
            String oldDesc = (String) tableModel.getValueAt(row, 2);
            LocalDate oldDate = LocalDate.parse(oldDateStr);

            // Build edit panel
            JPanel editPanel = new JPanel(new GridLayout(2, 2, 8, 8));
            editPanel.setBackground(CARD_COLOR);

            JLabel dateLbl = new JLabel("Date:");
            dateLbl.setForeground(TEXT_COLOR);
            JTextField dateInput = createDateField(oldDateStr);

            JLabel descLbl = new JLabel("Description:");
            descLbl.setForeground(TEXT_COLOR);
            JTextField descInput = createTextField(20);
            descInput.setText(oldDesc);

            editPanel.add(dateLbl);
            editPanel.add(createDatePickerPanel(dateInput));
            editPanel.add(descLbl);
            editPanel.add(descInput);

            int result = JOptionPane.showConfirmDialog(this, editPanel,
                    "Edit Holiday", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                try {
                    LocalDate newDate = LocalDate.parse(dateInput.getText().trim());
                    String newDesc = descInput.getText().trim();
                    if (newDesc.isEmpty())
                        newDesc = "Official Holiday";

                    // Check for duplicate date (only if date changed)
                    if (!newDate.equals(oldDate) && student.getHolidayDates().contains(newDate)) {
                        JOptionPane.showMessageDialog(this,
                                "A holiday already exists on " + newDate + ".");
                        return;
                    }

                    student.updateHoliday(oldDate, newDate, newDesc);
                    DatabaseManager.getInstance().updateHoliday(student.getId(), oldDate, newDate, newDesc);
                    loadHolidays();
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
                }
            }
        });

        // Remove Selected (single row)
        removeBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String dateStr = (String) tableModel.getValueAt(row, 0);
                LocalDate date = LocalDate.parse(dateStr);
                student.removeHolidayByDate(date);
                DatabaseManager.getInstance().removeHoliday(student.getId(), date);
                loadHolidays();
            } else {
                JOptionPane.showMessageDialog(this, "Select a holiday to remove.");
            }
        });

        // Remove Group (all holidays with same description)
        removeGroupBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String description = (String) tableModel.getValueAt(row, 2);
                // Count how many rows share this description
                int count = 0;
                for (Holiday h : student.getHolidays()) {
                    if (h.getDescription().equals(description))
                        count++;
                }

                int confirm = JOptionPane.showConfirmDialog(this,
                        "Remove ALL " + count + " holidays with description:\n\""
                                + description + "\"?",
                        "Confirm Group Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (confirm == JOptionPane.YES_OPTION) {
                    student.removeHolidaysByDescription(description);
                    DatabaseManager.getInstance().removeHolidaysByDescription(student.getId(), description);
                    loadHolidays();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Select a holiday row to identify the group.");
            }
        });

        setContentPane(mainPanel);
    }

    /**
     * Reload all subjects (and their attendance records) from DB.
     * Called after conflicting attendance records are cleaned up.
     */
    private void reloadStudentSubjects() {
        student.getSubjects().clear();
        List<Subject> freshSubjects = DatabaseManager.getInstance().loadSubjects(student.getId());
        for (Subject s : freshSubjects) {
            student.addSubject(s);
        }
    }

    private void loadHolidays() {
        tableModel.setRowCount(0);
        List<Holiday> holidays = student.getHolidays();
        // Sort by date
        holidays.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        for (Holiday h : holidays) {
            String dayName = h.getDate().getDayOfWeek().toString().charAt(0)
                    + h.getDate().getDayOfWeek().toString().substring(1).toLowerCase();
            tableModel.addRow(new Object[] { h.getDate().toString(), dayName, h.getDescription() });
        }
    }

    // ── UI Helper Methods ──

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_COLOR);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }

    private JTextField createTextField(int columns) {
        JTextField f = new JTextField(columns);
        f.setBackground(FIELD_BG);
        f.setForeground(TEXT_COLOR);
        f.setCaretColor(TEXT_COLOR);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(88, 91, 112)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return f;
    }

    private JTextField createDateField(String text) {
        JTextField f = new JTextField(10);
        f.setBackground(FIELD_BG);
        f.setForeground(TEXT_COLOR);
        f.setEditable(false);
        f.setCursor(new Cursor(Cursor.HAND_CURSOR));
        f.setCaretColor(TEXT_COLOR);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(88, 91, 112)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        f.setText(text);
        return f;
    }

    private JPanel createDatePickerPanel(JTextField field) {
        JPanel p = new JPanel(new BorderLayout(5, 0));
        p.setBackground(BG_COLOR); // Use BG_COLOR as it's added to inputPanel which is BG_COLOR

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

    private JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bgColor);
        btn.setForeground(BG_COLOR);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        return btn;
    }
}
