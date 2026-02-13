package com.attendance;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MainWindow extends JFrame {
    private Student student;
    private JPanel subjectsPanel;
    private WeeklySchedule schedule; // For predictions

    public MainWindow() {
        // Initialize Dummy Data for Testing
        initData();

        // Window Setup
        setTitle("Attendance Eligibility Manager");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Header ---
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(60, 63, 65));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        headerPanel.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Attendance Dashboard - " + student.getName());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton addSubjectBtn = new JButton("+ Add Subject");
        addSubjectBtn.setFocusPainted(false);
        addSubjectBtn.addActionListener(this::showAddSubjectDialog);
        headerPanel.add(addSubjectBtn, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // --- Main Content (Scrollable List of Subjects) ---
        subjectsPanel = new JPanel();
        subjectsPanel.setLayout(new BoxLayout(subjectsPanel, BoxLayout.Y_AXIS));
        subjectsPanel.setBackground(new Color(245, 245, 245));

        JScrollPane scrollPane = new JScrollPane(subjectsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // --- Footer / Actions ---
        JPanel footerPanel = new JPanel();
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton planLeaveBtn = new JButton("Plan Leave / Predict");
        planLeaveBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        planLeaveBtn.setBackground(new Color(0, 120, 215));
        planLeaveBtn.setForeground(Color.WHITE);
        planLeaveBtn.setOpaque(true);
        planLeaveBtn.setBorderPainted(false);
        planLeaveBtn.addActionListener(this::showPredictionDialog);

        JButton holidaysBtn = new JButton("Manage Holidays");
        holidaysBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        holidaysBtn.addActionListener(e -> new ManageHolidaysDialog(this, student).setVisible(true));

        JButton semesterBtn = new JButton("Semester Settings");
        semesterBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        semesterBtn.addActionListener(e -> new SemesterSettingsDialog(this, student).setVisible(true));

        footerPanel.add(holidaysBtn);
        footerPanel.add(semesterBtn);
        footerPanel.add(planLeaveBtn);
        add(footerPanel, BorderLayout.SOUTH);

        // Initial Render
        refreshDashboard();
    }

    private void initData() {
        student = new Student("Student User");
        schedule = new WeeklySchedule();

        // Sample data
        Subject ds = new Subject("Data Structures", 4);
        ds.setAttendance(20, 18); // 90%
        student.addSubject(ds);
        // Mon, Wed
        schedule.addClass(java.time.DayOfWeek.MONDAY, ds);
        schedule.addClass(java.time.DayOfWeek.WEDNESDAY, ds);

        Subject os = new Subject("Operating Systems", 3);
        os.setAttendance(20, 14); // 70%
        student.addSubject(os);
        // Tue, Thu
        schedule.addClass(java.time.DayOfWeek.TUESDAY, os);
        schedule.addClass(java.time.DayOfWeek.THURSDAY, os);

        Subject coreJava = new Subject("Core Java", 5);
        coreJava.setAttendance(15, 12); // 80%
        student.addSubject(coreJava);
        // Fri
        schedule.addClass(java.time.DayOfWeek.FRIDAY, coreJava);
    }

    private void refreshDashboard() {
        subjectsPanel.removeAll();

        for (Subject s : student.getSubjects()) {
            subjectsPanel.add(createSubjectCard(s));
            subjectsPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
        }

        subjectsPanel.revalidate();
        subjectsPanel.repaint();
    }

    private JPanel createSubjectCard(Subject subject) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        card.setMaximumSize(new Dimension(850, 100));
        card.setPreferredSize(new Dimension(850, 100));

        // Left Container: Name and Stats
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(subject.getName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        double pct = subject.getAttendancePercentage();
        JLabel statsLabel = new JLabel(String.format("Attended: %d / %d  (%.1f%%)",
                subject.getClassesAttended(), subject.getClassesConducted(), pct));

        infoPanel.add(nameLabel);
        infoPanel.add(statsLabel);
        card.add(infoPanel, BorderLayout.WEST);

        // Center: Progress Bar and Status
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue((int) pct);
        progressBar.setStringPainted(true);

        // Calculate detailed stats if semester end date is set
        String statusText;
        Color statusColor;

        if (student.getSemesterEndDate() != null && student.getSemesterEndDate().isAfter(java.time.LocalDate.now())) {
            int remaining = AttendanceCalculator.calculateRemainingClasses(subject, schedule,
                    student.getSemesterEndDate(), student.getHolidays());
            double maxPossible = AttendanceCalculator.calculateMaxPossibleAttendance(subject, remaining);

            if (pct >= 75) {
                int safeBunks = AttendanceCalculator.calculateSafeBunks(subject);
                // Cap safe bunks by remaining classes
                int realSafeBunks = Math.min(safeBunks, remaining);
                statusText = String.format("Safe! Can miss %d/%d remaining.", realSafeBunks, remaining);
                statusColor = new Color(39, 174, 96);
                progressBar.setForeground(new Color(46, 204, 113));
            } else {
                if (maxPossible < 75.0) {
                    statusText = String.format("CRITICAL: Max possible is only %.1f%%!", maxPossible);
                    statusColor = Color.RED;
                    progressBar.setForeground(Color.RED);
                } else {
                    int recovery = AttendanceCalculator.calculateRecoveryClasses(subject);
                    statusText = String.format("Warning! Attend next %d classes. (Rem: %d)", recovery, remaining);
                    statusColor = new Color(192, 57, 43);
                    progressBar.setForeground(new Color(231, 76, 60));
                }
            }
        } else {
            // Standard Logic (Infinite Horizon)
            if (pct >= 75) {
                progressBar.setForeground(new Color(46, 204, 113)); // Green
                int safeBunks = AttendanceCalculator.calculateSafeBunks(subject);
                statusText = "Safe! You can bunk " + safeBunks + " classes.";
                statusColor = new Color(39, 174, 96);
            } else {
                progressBar.setForeground(new Color(231, 76, 60)); // Red
                int recovery = AttendanceCalculator.calculateRecoveryClasses(subject);
                statusText = "Warning! Attend next " + recovery + " classes!";
                statusColor = new Color(192, 57, 43);
            }
        }

        JLabel adviceLabel = new JLabel(statusText);
        adviceLabel.setForeground(statusColor);
        statusPanel.add(adviceLabel, BorderLayout.SOUTH);

        statusPanel.add(progressBar, BorderLayout.CENTER);
        card.add(statusPanel, BorderLayout.CENTER);

        // Right: Quick Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionsPanel.setOpaque(false);

        // Date Picker
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setToolTipText("Select Date for Attendance");
        actionsPanel.add(dateSpinner);

        JButton attendedBtn = new JButton("Attended");
        attendedBtn.setBackground(new Color(230, 255, 230));
        attendedBtn.addActionListener(e -> {
            java.util.Date selectedDate = (java.util.Date) dateSpinner.getValue();
            java.time.LocalDate localDate = selectedDate.toInstant().atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
            subject.addClass(localDate, true);
            refreshDashboard();
        });

        JButton missedBtn = new JButton("Missed");
        missedBtn.setBackground(new Color(255, 230, 230));
        missedBtn.addActionListener(e -> {
            java.util.Date selectedDate = (java.util.Date) dateSpinner.getValue();
            java.time.LocalDate localDate = selectedDate.toInstant().atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
            subject.addClass(localDate, false);
            refreshDashboard();
        });

        actionsPanel.add(attendedBtn);
        actionsPanel.add(missedBtn);
        card.add(actionsPanel, BorderLayout.EAST);

        return card;
    }

    private void showAddSubjectDialog(ActionEvent e) {
        JDialog dialog = new JDialog(this, "Add New Subject", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        inputPanel.add(new JLabel("Subject Name:"));
        JTextField nameField = new JTextField();
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Select Class Days:"));
        JPanel daysPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        JCheckBox mon = new JCheckBox("Mon");
        JCheckBox tue = new JCheckBox("Tue");
        JCheckBox wed = new JCheckBox("Wed");
        JCheckBox thu = new JCheckBox("Thu");
        JCheckBox fri = new JCheckBox("Fri");
        JCheckBox sat = new JCheckBox("Sat");

        daysPanel.add(mon);
        daysPanel.add(tue);
        daysPanel.add(wed);
        daysPanel.add(thu);
        daysPanel.add(fri);
        daysPanel.add(sat);
        inputPanel.add(daysPanel);

        dialog.add(inputPanel, BorderLayout.CENTER);

        JButton saveBtn = new JButton("Save Subject");
        saveBtn.addActionListener(event -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a subject name.");
                return;
            }

            int count = 0;
            if (mon.isSelected())
                count++;
            if (tue.isSelected())
                count++;
            if (wed.isSelected())
                count++;
            if (thu.isSelected())
                count++;
            if (fri.isSelected())
                count++;
            if (sat.isSelected())
                count++;

            if (count == 0) {
                JOptionPane.showMessageDialog(dialog, "Please select at least one day.");
                return;
            }

            Subject newSubject = new Subject(name, count);
            student.addSubject(newSubject);

            // Update Weekly Schedule
            if (mon.isSelected())
                schedule.addClass(java.time.DayOfWeek.MONDAY, newSubject);
            if (tue.isSelected())
                schedule.addClass(java.time.DayOfWeek.TUESDAY, newSubject);
            if (wed.isSelected())
                schedule.addClass(java.time.DayOfWeek.WEDNESDAY, newSubject);
            if (thu.isSelected())
                schedule.addClass(java.time.DayOfWeek.THURSDAY, newSubject);
            if (fri.isSelected())
                schedule.addClass(java.time.DayOfWeek.FRIDAY, newSubject);
            if (sat.isSelected())
                schedule.addClass(java.time.DayOfWeek.SATURDAY, newSubject);

            refreshDashboard();
            dialog.dispose();
        });

        JPanel btnPanel = new JPanel();
        btnPanel.add(saveBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showPredictionDialog(ActionEvent e) {
        new PredictionDialog(this, student, schedule).setVisible(true);
    }
}
