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
    private final Color BG_COLOR = ThemeManager.getBgColor();
    private final Color CARD_COLOR = ThemeManager.getCardColor();
    private final Color ACCENT_COLOR = ThemeManager.getAccentColor();
    private final Color TEXT_COLOR = ThemeManager.getTextColor();
    private final Color FIELD_BG = ThemeManager.getFieldBgColor();
    private final Color SUCCESS_COLOR = ThemeManager.getGreenColor();

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
        addDateRow(inputPanel, gbc, row++, "Semester Start Date:",
                createDatePickerPanel(startField), "When regular classes begin");
        addDateRow(inputPanel, gbc, row++, "Mid-Sem Exam Start:",
                createDatePickerPanel(midStartField), "When classes pause for mid-sem exams");
        addDateRow(inputPanel, gbc, row++, "Mid-Sem Exam End:",
                createDatePickerPanel(midEndField), "When classes resume after mid-sems");
        addDateRow(inputPanel, gbc, row++, "Last Teaching Day:",
                createDatePickerPanel(lastDayField), "Last day of regular classes (before end-sem)");

        // Info label
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        JLabel infoLabel = new JLabel("ℹ️ Click the 📅 or the field to pick a date.");
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

        JButton saveBtn = new UIUtils.RoundedButton("💾 Save Configuration", ACCENT_COLOR, BG_COLOR, 12);

        saveBtn.addActionListener(e -> saveSemesterSettings(
                startField, midStartField, midEndField, lastDayField));

        JButton resetBtn = new UIUtils.RoundedButton("🔄 Reset All", ThemeManager.getRedColor(), BG_COLOR, 12);

        resetBtn.addActionListener(e -> {
            String warningMessage = "<html><body style='width:320px;'>"
                    + "<b>⚠️ Are you sure you want to reset ALL semester dates?</b><br><br>"
                    + "This will clear all 4 fields:<br>"
                    + "&nbsp;&nbsp;• Semester Start Date<br>"
                    + "&nbsp;&nbsp;• Mid-Sem Exam Start Date<br>"
                    + "&nbsp;&nbsp;• Mid-Sem Exam End Date<br>"
                    + "&nbsp;&nbsp;• Last Teaching Day<br><br>"
                    + "<b>Side effects:</b><br>"
                    + "&nbsp;&nbsp;• Safe-bunk calculations will stop working<br>"
                    + "&nbsp;&nbsp;• Remaining-class counts will be unavailable<br>"
                    + "&nbsp;&nbsp;• Critical attendance warnings (\"max possible < 75%\") will be disabled<br>"
                    + "&nbsp;&nbsp;• Leave predictions will lose accuracy<br><br>"
                    + "<i>You can reconfigure these dates at any time.</i>"
                    + "</body></html>";
            int choice = JOptionPane.showConfirmDialog(this,
                    warningMessage,
                    "⚠️ Confirm Reset — Side Effects", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
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
            String labelText, JPanel fieldPanel, String tooltip) {
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
        fieldPanel.setToolTipText(tooltip);
        panel.add(fieldPanel, gbc);
    }

    private JTextField createDateField(LocalDate date) {
        JTextField field = new UIUtils.RoundedTextField(12, 10);
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setEditable(false);
        field.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (date != null) {
            field.setText(date.toString());
        }
        return field;
    }

    private JPanel createDatePickerPanel(JTextField field) {
        JPanel p = new JPanel(new BorderLayout(5, 0));
        p.setBackground(CARD_COLOR);

        JButton btn = new UIUtils.RoundedButton("📅", ThemeManager.getHeaderColor(), ACCENT_COLOR, 10);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));

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
