package com.attendance;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

/**
 * Manage academic holidays.
 * Holidays are dates when NO classes happen (national holidays, institutional
 * off-days).
 * These are excluded from attendance calculations.
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

    public ManageHolidaysDialog(Frame owner, Student student) {
        super(owner, "Manage Academic Calendar", true);
        this.student = student;

        setSize(520, 450);
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
        table.setGridColor(new Color(69, 71, 90));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setBackground(new Color(69, 71, 90));
        table.getTableHeader().setForeground(TEXT_COLOR);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setRowHeight(28);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(CARD_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // ── Footer (Add/Remove Actions) ──
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        footerPanel.setBackground(BG_COLOR);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JTextField dateField = new JTextField(10);
        dateField.setBackground(FIELD_BG);
        dateField.setForeground(TEXT_COLOR);
        dateField.setCaretColor(TEXT_COLOR);
        dateField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(88, 91, 112)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        dateField.setText(LocalDate.now().toString());

        JButton addBtn = new JButton("+ Add Holiday");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setBackground(SUCCESS_COLOR);
        addBtn.setForeground(BG_COLOR);
        addBtn.setFocusPainted(false);
        addBtn.setOpaque(true);
        addBtn.setBorderPainted(false);

        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        removeBtn.setBackground(ERROR_COLOR);
        removeBtn.setForeground(BG_COLOR);
        removeBtn.setFocusPainted(false);
        removeBtn.setOpaque(true);
        removeBtn.setBorderPainted(false);

        addBtn.addActionListener(e -> {
            try {
                LocalDate date = LocalDate.parse(dateField.getText().trim());
                if (student.getHolidays().contains(date)) {
                    JOptionPane.showMessageDialog(this, "Holiday already exists.");
                } else {
                    student.addHoliday(date);
                    DatabaseManager.getInstance().addHoliday(student.getId(), date, "Official Holiday");
                    loadHolidays();
                    dateField.setText("");
                }
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format (YYYY-MM-DD).");
            }
        });

        removeBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String dateStr = (String) tableModel.getValueAt(row, 0);
                LocalDate date = LocalDate.parse(dateStr);
                student.removeHoliday(date);
                DatabaseManager.getInstance().removeHoliday(student.getId(), date);
                loadHolidays();
            } else {
                JOptionPane.showMessageDialog(this, "Select a holiday to remove.");
            }
        });

        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setForeground(TEXT_COLOR);
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        footerPanel.add(dateLabel);
        footerPanel.add(dateField);
        footerPanel.add(addBtn);
        footerPanel.add(removeBtn);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private void loadHolidays() {
        tableModel.setRowCount(0);
        List<LocalDate> holidays = student.getHolidays();
        Collections.sort(holidays);

        for (LocalDate date : holidays) {
            String dayName = date.getDayOfWeek().toString().charAt(0)
                    + date.getDayOfWeek().toString().substring(1).toLowerCase();
            tableModel.addRow(new Object[] { date.toString(), dayName, "Official Holiday" });
        }
    }
}
