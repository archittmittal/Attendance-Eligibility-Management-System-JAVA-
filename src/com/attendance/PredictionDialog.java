package com.attendance;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

import java.time.format.DateTimeParseException;
import java.util.Map;

public class PredictionDialog extends JDialog {
    private Student student;
    private WeeklySchedule schedule; // Passed from Main Window
    private JTextField startDateField;
    private JTextField endDateField;
    private JTextArea resultArea;

    public PredictionDialog(Frame owner, Student student, WeeklySchedule schedule) {
        super(owner, "Plan Leave & Predict", true);
        this.student = student;
        this.schedule = schedule;

        setSize(500, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        inputPanel.add(new JLabel("Start Date (YYYY-MM-DD):"));
        startDateField = new JTextField(LocalDate.now().toString());
        inputPanel.add(startDateField);

        inputPanel.add(new JLabel("End Date (YYYY-MM-DD):"));
        endDateField = new JTextField(LocalDate.now().plusDays(1).toString());
        inputPanel.add(endDateField);

        JButton predictBtn = new JButton("Predict Impact");
        predictBtn.addActionListener(e -> calculatePrediction());
        inputPanel.add(predictBtn);

        add(inputPanel, BorderLayout.NORTH);

        // Result Area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JScrollPane(resultArea), BorderLayout.CENTER);
    }

    private void calculatePrediction() {
        try {
            LocalDate start = LocalDate.parse(startDateField.getText().trim());
            LocalDate end = LocalDate.parse(endDateField.getText().trim());

            if (end.isBefore(start)) {
                JOptionPane.showMessageDialog(this, "End date cannot be before start date.");
                return;
            }

            Map<Subject, Double> predictions = AttendanceCalculator.predictAttendanceAfterLeave(student, schedule,
                    start, end);

            StringBuilder sb = new StringBuilder();
            sb.append("--- Prediction Results ---\n");
            sb.append("Leave Period: ").append(start).append(" to ").append(end).append("\n\n");

            boolean safeToLeave = true;

            for (Map.Entry<Subject, Double> entry : predictions.entrySet()) {
                Subject s = entry.getKey();
                double predictedPct = entry.getValue();
                double currentPct = s.getAttendancePercentage();

                sb.append(String.format("%-20s: %.1f%%  ->  %.1f%%\n", s.getName(), currentPct, predictedPct));

                if (predictedPct < 75.0) {
                    sb.append("   [WARNING] Attendance will drop below 75%!\n");
                    safeToLeave = false;
                } else {
                    sb.append("   [SAFE] You will stay eligible.\n");
                }
                sb.append("\n");
            }

            if (safeToLeave) {
                sb.append("\nSummary: You can safely take this leave! Enjoy! :)");
            } else {
                sb.append("\nSummary: CAUTION! This leave will affect your eligibility in specific subjects.");
            }

            resultArea.setText(sb.toString());

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.");
        }
    }
}
