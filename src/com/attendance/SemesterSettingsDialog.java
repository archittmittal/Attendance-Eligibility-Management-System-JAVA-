package com.attendance;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class SemesterSettingsDialog extends JDialog {
    private Student student;

    public SemesterSettingsDialog(Frame owner, Student student) {
        super(owner, "Semester Settings", true);
        this.student = student;

        setSize(400, 250);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Start Date
        inputPanel.add(new JLabel("Semester Start Date:"));
        JTextField startDateField = new JTextField();
        if (student.getSemesterStartDate() != null) {
            startDateField.setText(student.getSemesterStartDate().toString());
        } else {
            startDateField.setToolTipText("YYYY-MM-DD");
        }
        inputPanel.add(startDateField);

        // End Date
        inputPanel.add(new JLabel("Semester End Date:"));
        JTextField endDateField = new JTextField();
        if (student.getSemesterEndDate() != null) {
            endDateField.setText(student.getSemesterEndDate().toString());
        } else {
            endDateField.setToolTipText("YYYY-MM-DD");
        }
        inputPanel.add(endDateField);

        inputPanel.add(new JLabel("(Format: YYYY-MM-DD)"));
        inputPanel.add(new JLabel("")); // Spacer

        add(inputPanel, BorderLayout.CENTER);

        JButton saveBtn = new JButton("Save Configuration");
        saveBtn.addActionListener(e -> {
            try {
                String startStr = startDateField.getText().trim();
                String endStr = endDateField.getText().trim();

                if (!startStr.isEmpty() && !endStr.isEmpty()) {
                    LocalDate startDate = LocalDate.parse(startStr);
                    LocalDate endDate = LocalDate.parse(endStr);

                    if (endDate.isBefore(startDate)) {
                        JOptionPane.showMessageDialog(this, "End date cannot be before start date.");
                        return;
                    }

                    student.setSemesterStartDate(startDate);
                    student.setSemesterEndDate(endDate);
                    JOptionPane.showMessageDialog(this, "Semester settings saved!");
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Please enter valid dates.");
                }
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
            }
        });

        JPanel btnPanel = new JPanel();
        btnPanel.add(saveBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }
}
