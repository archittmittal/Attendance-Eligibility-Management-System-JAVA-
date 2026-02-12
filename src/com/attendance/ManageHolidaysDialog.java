package com.attendance;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

public class ManageHolidaysDialog extends JDialog {
    private Student student;
    private DefaultTableModel tableModel;

    public ManageHolidaysDialog(Frame owner, Student student) {
        super(owner, "Manage Academic Calendar", true);
        this.student = student;

        setSize(500, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // --- Header ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Add official holidays (No class days):"));
        add(headerPanel, BorderLayout.NORTH);

        // --- Table ---
        String[] columnNames = { "Date", "Description" }; // Description is not stored in Student yet, just UI
                                                          // placehoder
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        loadHolidays();

        JTable table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Footer (Add Action) ---
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JTextField dateField = new JTextField(10);
        dateField.setText(LocalDate.now().toString());
        JButton addBtn = new JButton("Add Holiday");

        addBtn.addActionListener(e -> {
            try {
                LocalDate date = LocalDate.parse(dateField.getText().trim());
                if (student.getHolidays().contains(date)) {
                    JOptionPane.showMessageDialog(this, "Holiday already exists.");
                } else {
                    student.addHoliday(date);
                    // Resort if needed or just add
                    loadHolidays();
                    dateField.setText("");
                }
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format (YYYY-MM-DD).");
            }
        });

        footerPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        footerPanel.add(dateField);
        footerPanel.add(addBtn);

        add(footerPanel, BorderLayout.SOUTH);
    }

    private void loadHolidays() {
        tableModel.setRowCount(0);
        List<LocalDate> holidays = student.getHolidays();
        Collections.sort(holidays);

        for (LocalDate date : holidays) {
            tableModel.addRow(new Object[] { date.toString(), "Official Holiday" });
        }
    }
}
