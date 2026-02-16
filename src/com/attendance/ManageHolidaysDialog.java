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
        JTextField fromField = createTextField(10);
        fromField.setText(LocalDate.now().toString());
        fromField.setToolTipText("YYYY-MM-DD");

        JLabel toLabel = createLabel("To Date:");
        JTextField toField = createTextField(10);
        toField.setText("");
        toField.setToolTipText("YYYY-MM-DD (leave empty for single day)");

        dateRow.add(fromLabel);
        dateRow.add(fromField);
        dateRow.add(Box.createHorizontalStrut(10));
        dateRow.add(toLabel);
        dateRow.add(toField);
        inputPanel.add(dateRow);

        // ── Row 3: Action Buttons ──
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        btnRow.setBackground(BG_COLOR);

        JButton addSingleBtn = createButton("+ Add Holiday", SUCCESS_COLOR);
        JButton addGroupBtn = createButton("+ Add Group Holiday", ACCENT_COLOR);
        JButton removeBtn = createButton("Remove Selected", ERROR_COLOR);
        JButton removeGroupBtn = createButton("Remove Group", WARN_COLOR);

        addSingleBtn.setToolTipText("Add a single-day holiday with the given description");
        addGroupBtn.setToolTipText("Add holidays for every date from 'From' to 'To' with the same description");
        removeBtn.setToolTipText("Remove just the selected holiday row");
        removeGroupBtn.setToolTipText("Remove ALL holidays with the same description as the selected row");

        btnRow.add(addSingleBtn);
        btnRow.add(addGroupBtn);
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
                    loadHolidays();
                    fromField.setText("");
                    toField.setText("");
                    descField.setText("Official Holiday");
                    JOptionPane.showMessageDialog(this,
                            days + " holidays added for \"" + description + "\"!",
                            "Group Holiday Added", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
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
