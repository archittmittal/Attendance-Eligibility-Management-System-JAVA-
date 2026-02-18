package com.attendance;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Leave prediction dialog — comprehensive impact analysis.
 * Shows:
 * 1. Attendance AFTER the leave period ends
 * 2. Full semester-end projection (if you attend everything AFTER leave)
 * 3. Recovery classes needed per subject
 * 4. Safe bunks remaining for the rest of the semester
 */
public class PredictionDialog extends JDialog {
    private Student student;
    private WeeklySchedule schedule;

    // Colors
    private static final Color BG_COLOR = new Color(30, 30, 46);
    private static final Color CARD_COLOR = new Color(49, 50, 68);
    private static final Color ACCENT_COLOR = new Color(137, 180, 250);
    private static final Color TEXT_COLOR = new Color(205, 214, 244);
    private static final Color FIELD_BG = new Color(69, 71, 90);

    public PredictionDialog(Frame owner, Student student, WeeklySchedule schedule) {
        super(owner, "🏖️ Leave Planner — Full Impact Analysis", true);
        this.student = student;
        this.schedule = schedule;

        setSize(640, 560);
        setLocationRelativeTo(owner);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        // ── Input Panel ──
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(CARD_COLOR);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 5, 6, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("📅 Plan Your Leave — See the FULL Impact");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(ACCENT_COLOR);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        inputPanel.add(titleLabel, gbc);

        JLabel startLabel = new JLabel("Leave Start Date:");
        startLabel.setForeground(TEXT_COLOR);
        startLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JTextField startDateField = createDateField(LocalDate.now().toString());

        JLabel endLabel = new JLabel("Leave End Date:");
        endLabel.setForeground(TEXT_COLOR);
        endLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JTextField endDateField = createDateField(LocalDate.now().plusDays(3).toString());

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        inputPanel.add(startLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(createDatePickerPanel(startDateField), gbc);
        gbc.gridy = 2;
        gbc.gridx = 0;
        inputPanel.add(endLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(createDatePickerPanel(endDateField), gbc);

        JButton predictBtn = new JButton("Analyze Leave Impact");
        predictBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        predictBtn.setBackground(ACCENT_COLOR);
        predictBtn.setForeground(BG_COLOR);
        predictBtn.setFocusPainted(false);
        predictBtn.setOpaque(true);
        predictBtn.setBorderPainted(false);
        predictBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        predictBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 5, 5, 5);
        inputPanel.add(predictBtn, gbc);

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // ── Result Area ──
        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Cascadia Mono", Font.PLAIN, 12));
        resultArea.setBackground(CARD_COLOR);
        resultArea.setForeground(TEXT_COLOR);
        resultArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        scrollPane.getViewport().setBackground(CARD_COLOR);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        predictBtn.addActionListener(e -> runAnalysis(startDateField, endDateField, resultArea));

        setContentPane(mainPanel);
    }

    private JTextField createDateField(String defaultValue) {
        JTextField field = new JTextField(12);
        field.setText(defaultValue);
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT_COLOR);
        field.setEditable(false);
        field.setCursor(new Cursor(Cursor.HAND_CURSOR));
        field.setCaretColor(TEXT_COLOR);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(88, 91, 112)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        return field;
    }

    private JPanel createDatePickerPanel(JTextField field) {
        JPanel p = new JPanel(new BorderLayout(5, 0));
        p.setBackground(CARD_COLOR);

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

    private void runAnalysis(JTextField startField, JTextField endField, JTextArea resultArea) {
        try {
            LocalDate start = LocalDate.parse(startField.getText().trim());
            LocalDate end = LocalDate.parse(endField.getText().trim());

            if (end.isBefore(start)) {
                JOptionPane.showMessageDialog(this, "End date cannot be before start date.");
                return;
            }

            long leaveDays = ChronoUnit.DAYS.between(start, end) + 1;
            List<LocalDate> holidays = student.getHolidayDates();
            LocalDate semEnd = student.getSemesterEndDate();
            boolean hasSemDates = student.isSemesterConfigured() && semEnd != null && semEnd.isAfter(LocalDate.now());

            // ═══ SECTION 1: Attendance AFTER the leave ═══
            Map<Subject, Double> afterLeave = AttendanceCalculator.predictAttendanceAfterLeave(
                    student, schedule, start, end);

            StringBuilder sb = new StringBuilder();
            sb.append("╔══════════════════════════════════════════════╗\n");
            sb.append("║     LEAVE IMPACT ANALYSIS REPORT             ║\n");
            sb.append("╚══════════════════════════════════════════════╝\n\n");
            sb.append("Leave Period : ").append(start).append(" → ").append(end);
            sb.append("  (").append(leaveDays).append(" days)\n\n");

            // ── Part A: Immediate Impact (after leave ends) ──
            sb.append("━━━ PART A: IMPACT AFTER YOUR LEAVE ENDS ━━━\n\n");
            sb.append(String.format("  %-18s %8s → %-8s %s\n", "Subject", "Now", "After", "Status"));
            sb.append("  ─────────────────────────────────────────────\n");

            boolean anyDanger = false;
            for (Map.Entry<Subject, Double> entry : afterLeave.entrySet()) {
                Subject s = entry.getKey();
                double currentPct = s.getAttendancePercentage();
                double afterPct = entry.getValue();
                String status;
                if (afterPct < 75.0) {
                    status = "🔴 DANGER";
                    anyDanger = true;
                } else if (afterPct < 80.0) {
                    status = "⚠️  RISKY";
                } else {
                    status = "✅ SAFE";
                }
                sb.append(String.format("  %-18s %6.1f%% → %5.1f%%  %s\n",
                        s.getName(), currentPct, afterPct, status));
            }

            // ── Part B: Semester-End Projection ──
            if (hasSemDates) {
                sb.append("\n\n━━━ PART B: FULL SEMESTER PROJECTION ━━━\n");
                sb.append("  (If you attend EVERY class after your leave until " + semEnd + ")\n\n");
                sb.append(String.format("  %-18s %8s %10s %10s %s\n",
                        "Subject", "AfterLv", "SemEnd%", "CanMiss", "MustAttend"));
                sb.append("  ─────────────────────────────────────────────────────────\n");

                for (Map.Entry<Subject, Double> entry : afterLeave.entrySet()) {
                    Subject s = entry.getKey();
                    double afterPct = entry.getValue();

                    // Calculate classes missed during leave
                    int classesLost = countClassesDuringPeriod(s, start, end, holidays);

                    // Simulated state after leave
                    int conductedAfterLeave = s.getClassesConducted() + classesLost;
                    int attendedAfterLeave = s.getClassesAttended(); // didn't attend during leave

                    // Remaining classes from day after leave to semester end
                    int remainingAfterLeave = 0;
                    LocalDate cursor = end.plusDays(1);
                    while (!cursor.isAfter(semEnd)) {
                        if (!holidays.contains(cursor) && !student.isDuringMidsemExams(cursor)) {
                            List<Subject> daySubjects = schedule.getSubjectsOn(cursor.getDayOfWeek());
                            if (daySubjects != null && daySubjects.contains(s)) {
                                remainingAfterLeave++;
                            }
                        }
                        cursor = cursor.plusDays(1);
                    }

                    // Best case: attend ALL remaining
                    int bestConducted = conductedAfterLeave + remainingAfterLeave;
                    int bestAttended = attendedAfterLeave + remainingAfterLeave;
                    double bestPct = (bestConducted == 0) ? 100.0 : (double) bestAttended / bestConducted * 100.0;

                    // How many can still miss (safe bunks after leave)
                    // To keep >= 75%: attended / (conducted + remaining) >= 0.75
                    // safeBunks = floor(attended/0.75 - conducted) but from post-leave state
                    // attending all
                    int canStillMiss = 0;
                    if (bestPct >= 75.0) {
                        canStillMiss = (int) Math.floor(
                                (double) bestAttended / 0.75 - bestConducted);
                        if (canStillMiss < 0)
                            canStillMiss = 0;
                    }

                    // How many MUST attend (recovery from post-leave state)
                    // Need: (attendedAfterLeave + x) / (conductedAfterLeave + x) >= 0.75
                    // x >= (3*conducted - 4*attended) [for 75%]
                    int mustAttend = 0;
                    if (afterPct < 75.0) {
                        mustAttend = (int) Math.ceil(3.0 * conductedAfterLeave - 4.0 * attendedAfterLeave);
                        if (mustAttend < 0)
                            mustAttend = 0;
                    }

                    String status;
                    if (bestPct < 75.0) {
                        status = "🔴 UNRECOVERABLE!";
                    } else if (mustAttend > 0) {
                        status = "⚠️  Must recover";
                    } else {
                        status = "✅";
                    }

                    sb.append(String.format("  %-18s %6.1f%% %8.1f%%   %4d     %4d   %s\n",
                            s.getName(), afterPct, bestPct, canStillMiss, mustAttend, status));
                }

                // ── Part C: Summary Verdict ──
                sb.append("\n\n━━━ PART C: VERDICT & ADVICE ━━━\n\n");

                if (!anyDanger) {
                    sb.append("  ✅ SAFE TO TAKE THIS LEAVE!\n");
                    sb.append("  Your attendance stays above 75% in all subjects.\n");
                    sb.append("  Enjoy your break! 🎉\n\n");
                    sb.append("  💡 TIP: Check the 'CanMiss' column above to see\n");
                    sb.append("     how many MORE classes you can skip this semester.\n");
                } else {
                    sb.append("  🔴 WARNING: This leave WILL affect your eligibility!\n\n");

                    // Show recovery plan for each danger subject
                    for (Map.Entry<Subject, Double> entry : afterLeave.entrySet()) {
                        Subject s = entry.getKey();
                        double afterPct = entry.getValue();
                        if (afterPct < 75.0) {
                            int classesLost = countClassesDuringPeriod(s, start, end, holidays);
                            int conductedAfterLeave = s.getClassesConducted() + classesLost;
                            int attendedAfterLeave = s.getClassesAttended();
                            int mustAttend = (int) Math.ceil(3.0 * conductedAfterLeave - 4.0 * attendedAfterLeave);
                            if (mustAttend < 0)
                                mustAttend = 0;

                            sb.append(String.format("  📌 %s: Attend next %d classes non-stop!\n",
                                    s.getName(), mustAttend));

                            // Check if recovery is even possible
                            int remaining = 0;
                            LocalDate c = end.plusDays(1);
                            while (!c.isAfter(semEnd)) {
                                if (!holidays.contains(c) && !student.isDuringMidsemExams(c)) {
                                    List<Subject> ds = schedule.getSubjectsOn(c.getDayOfWeek());
                                    if (ds != null && ds.contains(s))
                                        remaining++;
                                }
                                c = c.plusDays(1);
                            }
                            if (mustAttend > remaining) {
                                sb.append(String.format("     ⛔ Only %d classes left — CANNOT recover!\n", remaining));
                            } else {
                                sb.append(String.format("     (%d classes left — recovery possible ✓)\n", remaining));
                            }
                        }
                    }

                    sb.append("\n  💡 ADVICE: Consider shortening your leave, or\n");
                    sb.append("     skipping only low-impact days.\n");
                }
            } else {
                // No semester dates — basic output
                sb.append("\n\n━━━ VERDICT ━━━\n\n");
                if (!anyDanger) {
                    sb.append("  ✅ You can safely take this leave!\n");
                } else {
                    sb.append("  🔴 This leave will affect eligibility!\n");
                    sb.append("  Consider shorter leave or attend recovery classes.\n");
                }
                sb.append("\n  ℹ️ Set your semester dates in Settings to see\n");
                sb.append("     full semester projection & recovery plan.\n");
            }

            resultArea.setText(sb.toString());
            resultArea.setCaretPosition(0);

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.");
        }
    }

    /**
     * Count how many classes of a subject fall during a period, excluding holidays
     * and midsems.
     */
    private int countClassesDuringPeriod(Subject subject, LocalDate start, LocalDate end, List<LocalDate> holidays) {
        int count = 0;
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            if (!holidays.contains(cursor) && !student.isDuringMidsemExams(cursor)) {
                List<Subject> daySubjects = schedule.getSubjectsOn(cursor.getDayOfWeek());
                if (daySubjects != null && daySubjects.contains(subject)) {
                    count++;
                }
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }
}
