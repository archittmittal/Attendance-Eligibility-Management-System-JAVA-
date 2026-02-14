package com.attendance;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Semester Settings Dialog — Configure semester dates.
 * Captures 4 key dates:
 * 1. Semester Start Date
 * 2. Mid-Sem Exam Start Date (classes pause)
 * 3. Mid-Sem Exam End Date (classes resume)
 * 4. Last Teaching Day (before end-sem exams)
 */
public class SemesterSettingsDialog extends JDialog {

    private Student student;
    private boolean saved = false;

    // Colors
    private static final Color BG_COLOR = new Color(30, 30, 46);
    private static final Color CARD_COLOR = new Color(49, 50, 68);
    private static final Color ACCENT_COLOR = new Color(137, 180, 250);
    private static final Color TEXT_COLOR = new Color(205, 214, 244);
    private static final Color FIELD_BG = new Color(69, 71, 90);
    private static final Color SUCCESS_COLOR = new Color(166, 227, 161);

    public SemesterSettingsDialog(Frame owner, Student student) {
        super(owner, "Semester Configuration", true);
        this.student = student;

        setSize(500, 420);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        // ── Header ──
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        JLabel titleLabel = new JLabel("📅 Semester Configuration");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(ACCENT_COLOR);
        headerPanel.add(titleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ── Input Panel ──
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(CARD_COLOR);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 30, 10, 30),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;

        // Date fields
        JTextField startField = createDateField(student.getSemesterStartDate());
        JTextField midStartField = createDateField(student.getMidsemExamStartDate());
        JTextField midEndField = createDateField(student.getMidsemExamEndDate());
        JTextField lastDayField = createDateField(student.getSemesterEndDate());

        int row = 0;
        addDateRow(inputPanel, gbc, row++, "Semester Start Date:", startField,
                "When regular classes begin");
        addDateRow(inputPanel, gbc, row++, "Mid-Sem Exam Start:", midStartField,
                "When classes pause for mid-sem exams");
        addDateRow(inputPanel, gbc, row++, "Mid-Sem Exam End:", midEndField,
                "When classes resume after mid-sems");
        addDateRow(inputPanel, gbc, row++, "Last Teaching Day:", lastDayField,
                "Last day of regular classes (before end-sem)");

        // Info label
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        JLabel infoLabel = new JLabel("ℹ️ Format: YYYY-MM-DD  (e.g. 2026-01-06)");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        infoLabel.setForeground(new Color(147, 153, 178));
        inputPanel.add(infoLabel, gbc);

        JPanel cardWrapper = new JPanel(new BorderLayout());
        cardWrapper.setBackground(BG_COLOR);
        cardWrapper.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));
        cardWrapper.add(inputPanel, BorderLayout.CENTER);

        mainPanel.add(cardWrapper, BorderLayout.CENTER);

        // ── Buttons (Save + Reset) ──
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(BG_COLOR);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 15, 0));

        JButton saveBtn = new JButton("💾 Save Configuration");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveBtn.setBackground(ACCENT_COLOR);
        saveBtn.setForeground(BG_COLOR);
        saveBtn.setFocusPainted(false);
        saveBtn.setOpaque(true);
        saveBtn.setBorderPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        saveBtn.addActionListener(e -> saveSemesterSettings(
                startField, midStartField, midEndField, lastDayField));

        JButton resetBtn = new JButton("🔄 Reset All");
        resetBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        resetBtn.setBackground(new Color(243, 139, 168)); // Red
        resetBtn.setForeground(BG_COLOR);
        resetBtn.setFocusPainted(false);
        resetBtn.setOpaque(true);
        resetBtn.setBorderPainted(false);
        resetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resetBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        resetBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Reset all semester dates? This will clear all 4 fields.",
                    "Confirm Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                startField.setText("");
                midStartField.setText("");
                midEndField.setText("");
                lastDayField.setText("");

                // Clear from student object
                student.setSemesterStartDate(null);
                student.setMidsemExamStartDate(null);
                student.setMidsemExamEndDate(null);
                student.setSemesterEndDate(null);

                // Clear from database
                DatabaseManager.getInstance().saveSemesterSettings(student);

                JOptionPane.showMessageDialog(this,
                        "Semester settings have been reset.",
                        "Reset", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnPanel.add(saveBtn);
        btnPanel.add(resetBtn);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void addDateRow(JPanel panel, GridBagConstraints gbc, int row,
            String labelText, JTextField field, String tooltip) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.4;
        JLabel label = new JLabel(labelText);
        label.setForeground(TEXT_COLOR);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        field.setToolTipText(tooltip);
        panel.add(field, gbc);
    }

    private JTextField createDateField(LocalDate date) {
        JTextField field = new JTextField(12);
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(88, 91, 112), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        if (date != null) {
            field.setText(date.toString());
        }
        return field;
    }

    private void saveSemesterSettings(JTextField startField, JTextField midStartField,
            JTextField midEndField, JTextField lastDayField) {
        try {
            String startStr = startField.getText().trim();
            String midStartStr = midStartField.getText().trim();
            String midEndStr = midEndField.getText().trim();
            String lastDayStr = lastDayField.getText().trim();

            // Validate: start and last day are required
            if (startStr.isEmpty() || lastDayStr.isEmpty()) {
                showError("Semester Start and Last Teaching Day are required.");
                return;
            }

            LocalDate start = LocalDate.parse(startStr);
            LocalDate lastDay = LocalDate.parse(lastDayStr);

            if (lastDay.isBefore(start)) {
                showError("Last Teaching Day cannot be before Semester Start.");
                return;
            }

            // Mid-sem dates are optional (some semesters may not have them)
            LocalDate midStart = null;
            LocalDate midEnd = null;
            if (!midStartStr.isEmpty() && !midEndStr.isEmpty()) {
                midStart = LocalDate.parse(midStartStr);
                midEnd = LocalDate.parse(midEndStr);

                if (midEnd.isBefore(midStart)) {
                    showError("Mid-Sem Exam End cannot be before Mid-Sem Exam Start.");
                    return;
                }
                if (midStart.isBefore(start) || midEnd.isAfter(lastDay)) {
                    showError("Mid-Sem dates must fall within the semester period.");
                    return;
                }
            }

            // Save to student object
            student.setSemesterStartDate(start);
            student.setMidsemExamStartDate(midStart);
            student.setMidsemExamEndDate(midEnd);
            student.setSemesterEndDate(lastDay);

            // Save to database
            DatabaseManager.getInstance().saveSemesterSettings(student);

            saved = true;
            JOptionPane.showMessageDialog(this,
                    "Semester settings saved successfully!",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (DateTimeParseException ex) {
            showError("Invalid date format. Please use YYYY-MM-DD.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public boolean isSaved() {
        return saved;
    }
}
