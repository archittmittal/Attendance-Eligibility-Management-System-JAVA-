package com.attendance;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Main application dashboard.
 * Shows attendance cards for each subject, overall stats, and action buttons.
 * All data is loaded from and saved to MySQL via DatabaseManager.
 */
public class MainWindow extends JFrame implements ThemeManager.ThemeChangeListener {
    private Student student;
    private JPanel subjectsPanel;
    private JPanel summaryPanel;
    private WeeklySchedule schedule;
    private JPanel undoPanel; // Toast bar for undo
    private javax.swing.Timer undoTimer; // Auto-dismiss timer

    // Colors — now resolved dynamically via ThemeManager
    private Color BG_COLOR;
    private Color CARD_COLOR;
    private Color HEADER_COLOR;
    private Color ACCENT_COLOR;
    private Color TEXT_COLOR;
    private Color SUBTEXT_COLOR;
    private Color GREEN;
    private Color RED;
    private Color YELLOW;
    private Color SURFACE;

    private void refreshColors() {
        BG_COLOR = ThemeManager.getBgColor();
        CARD_COLOR = ThemeManager.getCardColor();
        HEADER_COLOR = ThemeManager.getHeaderColor();
        ACCENT_COLOR = ThemeManager.getAccentColor();
        TEXT_COLOR = ThemeManager.getTextColor();
        SUBTEXT_COLOR = ThemeManager.getSubtextColor();
        GREEN = ThemeManager.getGreenColor();
        RED = ThemeManager.getRedColor();
        YELLOW = ThemeManager.getYellowColor();
        SURFACE = ThemeManager.getSurfaceColor();
    }

    public MainWindow(Student student) {
        this.student = student;
        this.schedule = new WeeklySchedule();

        // Load theme preference from DB
        ThemeManager.loadTheme(student.getId());
        refreshColors();

        // Load schedule from DB
        DatabaseManager.getInstance().loadSchedule(schedule, student.getSubjects());

        // Window Setup
        setTitle("Attendance Eligibility Manager — " + student.getName());
        setSize(960, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        // Register theme listener
        ThemeManager.addThemeChangeListener(this);

        // ── Header ──
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(HEADER_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("🎓 Attendance Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(ACCENT_COLOR);

        JLabel userLabel = new JLabel("Welcome, " + student.getName() + "  ");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userLabel.setForeground(SUBTEXT_COLOR);

        JButton addSubjectBtn = new UIUtils.RoundedButton("+ Add Subject", ACCENT_COLOR, HEADER_COLOR, 12);
        addSubjectBtn.addActionListener(this::showAddSubjectDialog);

        // Theme toggle button
        JButton themeToggleBtn = new UIUtils.RoundedButton(ThemeManager.getToggleLabel(), SURFACE, TEXT_COLOR, 12);
        themeToggleBtn.addActionListener(e -> {
            ThemeManager.toggleTheme();
            ThemeManager.saveTheme(student.getId());
        });

        JButton logoutBtn = new UIUtils.RoundedButton("🚪 Logout", RED, HEADER_COLOR, 12);
        logoutBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to logout?",
                    "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                ThemeManager.removeThemeChangeListener(this);
                dispose();
                SwingUtilities.invokeLater(() -> {
                    LoginDialog loginDialog = new LoginDialog(null);
                    loginDialog.setVisible(true);
                    if (loginDialog.isLoginSuccessful()) {
                        Student newStudent = loginDialog.getAuthenticatedStudent();
                        new MainWindow(newStudent).setVisible(true);
                    } else {
                        System.exit(0);
                    }
                });
            }
        });

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);
        headerRight.add(userLabel);
        headerRight.add(addSubjectBtn);
        headerRight.add(themeToggleBtn);
        headerRight.add(logoutBtn);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(headerRight, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // ── Summary Panel ──
        summaryPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        summaryPanel.setBackground(BG_COLOR);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));

        // ── Main Content (Scrollable List of Subjects) ──
        subjectsPanel = new JPanel();
        subjectsPanel.setLayout(new BoxLayout(subjectsPanel, BoxLayout.Y_AXIS));
        subjectsPanel.setBackground(BG_COLOR);
        subjectsPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        // Initialize Undo Panel
        initializeUndoPanel();

        // Wrapper for Summary + Undo to sit at the top
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBackground(BG_COLOR);
        topContainer.add(summaryPanel, BorderLayout.CENTER);
        topContainer.add(undoPanel, BorderLayout.SOUTH); // Undo bar appears below summary

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(BG_COLOR);
        centerPanel.add(topContainer, BorderLayout.NORTH);
        centerPanel.add(subjectsPanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(BG_COLOR);
        add(scrollPane, BorderLayout.CENTER);

        // ── Footer ──
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        footerPanel.setBackground(HEADER_COLOR);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        footerPanel.add(createFooterButton("📋 Timetable",
                e -> new TimetableDialog(this, schedule, student.getSubjects()).setVisible(true)));
        footerPanel.add(createFooterButton("📊 Trends",
                e -> new AttendanceTrendsDialog(this, student).setVisible(true)));
        footerPanel.add(createFooterButton("📅 Manage Holidays",
                e -> new ManageHolidaysDialog(this, student).setVisible(true)));
        footerPanel.add(createFooterButton("⚙️ Semester Settings",
                e -> {
                    new SemesterSettingsDialog(this, student).setVisible(true);
                    refreshDashboard();
                }));
        footerPanel.add(createFooterButton("🏖️ Plan Leave / Predict",
                this::showPredictionDialog));

        JButton exportBtn = createFooterButton("📤 Export", null);
        JPopupMenu exportMenu = new JPopupMenu();
        exportMenu.setBackground(new Color(49, 50, 68));
        JMenuItem csvItem = new JMenuItem("📄 Export as CSV");
        csvItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        csvItem.setBackground(new Color(49, 50, 68));
        csvItem.setForeground(TEXT_COLOR);
        csvItem.addActionListener(ev -> ExportManager.exportCSV(this, student));
        JMenuItem pdfItem = new JMenuItem("📑 Export Formal Report");
        pdfItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pdfItem.setBackground(new Color(49, 50, 68));
        pdfItem.setForeground(TEXT_COLOR);
        pdfItem.addActionListener(ev -> ExportManager.exportFormalReport(this, student));
        exportMenu.add(csvItem);
        exportMenu.add(pdfItem);
        exportBtn.addActionListener(ev -> exportMenu.show(exportBtn, 0, -exportMenu.getPreferredSize().height));
        footerPanel.add(exportBtn);

        footerPanel.add(createFooterButton("🔑 Change Password",
                e -> showChangePasswordDialog()));

        add(footerPanel, BorderLayout.SOUTH);

        // Check if semester is configured — if not, show setup wizard
        if (!student.isSemesterConfigured()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        "Welcome! Please configure your semester dates first.",
                        "First-Time Setup", JOptionPane.INFORMATION_MESSAGE);
                new SemesterSettingsDialog(this, student).setVisible(true);
            });
        }

        // Initial Render
        refreshDashboard();
    }

    private JButton createFooterButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new UIUtils.RoundedButton(text, SURFACE, TEXT_COLOR, 12);
        if (action != null) btn.addActionListener(action);
        return btn;
    }

    private void refreshDashboard() {
        // ── Summary ──
        summaryPanel.removeAll();
        int totalSubjects = student.getSubjects().size();
        int eligibleCount = 0;
        int atRiskCount = 0;
        double totalAttended = 0;
        double totalConducted = 0;

        for (Subject s : student.getSubjects()) {
            if (AttendanceCalculator.isEligible(s))
                eligibleCount++;
            else
                atRiskCount++;
            totalAttended += s.getClassesAttended();
            totalConducted += s.getClassesConducted();
        }

        double overallPct = (totalConducted == 0) ? 100.0 : (totalAttended / totalConducted * 100.0);

        summaryPanel.add(createStatCard("Overall", String.format("%.1f%%", overallPct),
                overallPct >= 75 ? GREEN : RED));
        summaryPanel.add(createStatCard("Subjects", String.valueOf(totalSubjects), ACCENT_COLOR));
        summaryPanel.add(createStatCard("Eligible", String.valueOf(eligibleCount), GREEN));
        summaryPanel.add(createStatCard("At Risk", String.valueOf(atRiskCount),
                atRiskCount > 0 ? RED : SUBTEXT_COLOR));

        summaryPanel.revalidate();
        summaryPanel.repaint();

        // ── Subject Cards ──
        subjectsPanel.removeAll();

        if (student.getSubjects().isEmpty()) {
            JLabel emptyLabel = new JLabel("No subjects added yet. Click '+ Add Subject' to get started!");
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 15));
            emptyLabel.setForeground(SUBTEXT_COLOR);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
            subjectsPanel.add(emptyLabel);
        } else {
            for (Subject s : student.getSubjects()) {
                subjectsPanel.add(createSubjectCard(s));
                subjectsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        subjectsPanel.revalidate();
        subjectsPanel.repaint();
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE, 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(SUBTEXT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(valueLabel);
        card.add(titleLabel);
        return card;
    }

    private JPanel createSubjectCard(Subject subject) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE, 1),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        card.setMaximumSize(new Dimension(900, 110));
        card.setPreferredSize(new Dimension(900, 110));

        double pct = subject.getAttendancePercentage();

        // ── Left: Name and Stats ──
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(subject.getName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        nameLabel.setForeground(TEXT_COLOR);

        JLabel statsLabel = new JLabel(String.format("Attended: %d / %d  (%.1f%%)",
                subject.getClassesAttended(), subject.getClassesConducted(), pct));
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statsLabel.setForeground(SUBTEXT_COLOR);

        infoPanel.add(nameLabel);
        infoPanel.add(statsLabel);
        card.add(infoPanel, BorderLayout.WEST);

        // ── Center: Progress Bar + Status ──
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue((int) pct);
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progressBar.setBackground(SURFACE);
        progressBar.setBorderPainted(false);

        String statusText;
        Color statusColor;

        if (student.isSemesterConfigured() && student.getSemesterEndDate().isAfter(LocalDate.now())) {
            int remaining = AttendanceCalculator.calculateRemainingClasses(
                    subject, schedule, student.getSemesterEndDate(), student.getHolidayDates(), student);
            double maxPossible = AttendanceCalculator.calculateMaxPossibleAttendance(subject, remaining);

            if (pct >= 75) {
                int safeBunks = Math.min(AttendanceCalculator.calculateSafeBunks(subject), remaining);
                statusText = String.format("✅ Safe! Can miss %d / %d remaining", safeBunks, remaining);
                statusColor = GREEN;
                progressBar.setForeground(GREEN);
            } else if (maxPossible < 75.0) {
                statusText = String.format("🔴 CRITICAL: Max possible = %.1f%%", maxPossible);
                statusColor = RED;
                progressBar.setForeground(RED);
            } else {
                int recovery = AttendanceCalculator.calculateRecoveryClasses(subject);
                statusText = String.format("⚠️ Attend next %d classes! (%d remaining)", recovery, remaining);
                statusColor = YELLOW;
                progressBar.setForeground(RED);
            }
        } else {
            if (pct >= 75) {
                int safeBunks = AttendanceCalculator.calculateSafeBunks(subject);
                statusText = "✅ Safe! Can miss " + safeBunks + " classes";
                statusColor = GREEN;
                progressBar.setForeground(GREEN);
            } else {
                int recovery = AttendanceCalculator.calculateRecoveryClasses(subject);
                statusText = "⚠️ Attend next " + recovery + " classes!";
                statusColor = RED;
                progressBar.setForeground(RED);
            }
        }

        JLabel adviceLabel = new JLabel(statusText);
        adviceLabel.setForeground(statusColor);
        adviceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        statusPanel.add(progressBar, BorderLayout.CENTER);
        statusPanel.add(adviceLabel, BorderLayout.SOUTH);
        card.add(statusPanel, BorderLayout.CENTER);

        // ── Right: Actions ──
        JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
        actionsPanel.setOpaque(false);

        // Top row: Attended / Missed
        JPanel markPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        markPanel.setOpaque(false);

        JButton attendedBtn = new UIUtils.RoundedButton("✅ Present", GREEN, HEADER_COLOR, 8);
        attendedBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));

        JButton missedBtn = new UIUtils.RoundedButton("❌ Absent", RED, HEADER_COLOR, 8);
        missedBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));

        attendedBtn.addActionListener(e -> markAttendance(subject, true));
        missedBtn.addActionListener(e -> markAttendance(subject, false));

        markPanel.add(attendedBtn);
        markPanel.add(missedBtn);

        // Bottom row: Edit / Delete
        JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 3));
        editPanel.setOpaque(false);

        JButton historyBtn = new UIUtils.RoundedButton("📅 History / Past", SURFACE, TEXT_COLOR, 8);
        historyBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        historyBtn.setToolTipText("View attendance history, add past records, or rename subject");
        historyBtn.addActionListener(e -> showEditSubjectDialog(subject));

        JButton deleteBtn = new UIUtils.RoundedButton("🗑️ Delete", SURFACE, RED, 8);
        deleteBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        deleteBtn.addActionListener(e -> deleteSubject(subject));

        editPanel.add(historyBtn);
        editPanel.add(deleteBtn);

        actionsPanel.add(markPanel);
        actionsPanel.add(editPanel);

        card.add(actionsPanel, BorderLayout.EAST);
        return card;
    }

    private void initializeUndoPanel() {
        undoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        undoPanel.setBackground(new Color(40, 40, 50)); // Darker background
        undoPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT_COLOR));
        undoPanel.setVisible(false);

        JLabel msgLabel = new JLabel("Attendance marked.");
        msgLabel.setForeground(TEXT_COLOR);
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton undoBtn = new UIUtils.RoundedButton("↩ Undo", ACCENT_COLOR, HEADER_COLOR, 8);
        undoBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));

        undoBtn.addActionListener(e -> handleUndo());

        undoPanel.add(msgLabel);
        undoPanel.add(undoBtn);

        // Add to the top of the center panel (below header)
        // We need to find the centerPanel which holds the summary and subjects
        // In constructor: centerPanel.add(summaryPanel, BorderLayout.NORTH);
        // We will inject a wrapper panel in the constructor instead to hold this.
    }

    // Temporary storage for undo
    private Subject lastSubject;
    private LocalDate lastDate;

    private void handleUndo() {
        if (lastSubject != null && lastDate != null) {
            // Remove from DB
            DatabaseManager.getInstance().deleteAttendanceRecord(lastSubject.getId(), lastDate);
            // Remove from Model
            lastSubject.removeRecordForDate(lastDate);

            // Hide toast
            undoPanel.setVisible(false);
            undoTimer.stop();

            // Refresh UI
            refreshDashboard();

            JOptionPane.showMessageDialog(this, "Attendance record removed.", "Undo Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void markAttendance(Subject subject, boolean present) {
        LocalDate today = LocalDate.now();

        // Validate: date must be within semester
        if (student.isSemesterConfigured()) {
            LocalDate semStart = student.getSemesterStartDate();
            LocalDate semEnd = student.getSemesterEndDate();
            if (semStart != null && today.isBefore(semStart)) {
                JOptionPane.showMessageDialog(this,
                        "Cannot mark attendance before semester starts (" + semStart + ").",
                        "Invalid Date", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (semEnd != null && today.isAfter(semEnd)) {
                JOptionPane.showMessageDialog(this,
                        "Cannot mark attendance after semester ends (" + semEnd + ").",
                        "Invalid Date", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Block attendance on holidays
        if (student.getHolidayDates().contains(today)) {
            JOptionPane.showMessageDialog(this,
                    "📅 Today (" + today + ") is a holiday!\nAttendance cannot be marked on holidays.",
                    "Holiday", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Block attendance during mid-sem exams
        if (student.isDuringMidsemExams(today)) {
            JOptionPane.showMessageDialog(this,
                    "📝 Today falls within the mid-sem exam period.\nNo classes are scheduled during exams.",
                    "Exam Period", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check if this day is in the subject's weekly schedule
        java.util.List<Subject> scheduledSubjects = schedule.getSubjectsOn(today.getDayOfWeek());
        if (scheduledSubjects == null || !scheduledSubjects.contains(subject)) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "📌 " + today.getDayOfWeek() + " is NOT a scheduled day for " + subject.getName() + ".\n"
                            + "Add this as an EXTRA CLASS?",
                    "Extra Class", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice != JOptionPane.YES_OPTION)
                return;
        }

        // Check for duplicate
        if (subject.hasRecordForDate(today)) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Attendance already marked for today. Update it?",
                    "Duplicate Entry", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION)
                return;
        }

        subject.addClass(today, present);
        DatabaseManager.getInstance().saveAttendanceRecord(subject.getId(), today, present);

        // Show Undo Toast
        lastSubject = subject;
        lastDate = today;

        showUndoToast();

        refreshDashboard();
    }

    private void showUndoToast() {
        if (undoPanel == null)
            return;

        undoPanel.setVisible(true);
        if (undoTimer != null && undoTimer.isRunning()) {
            undoTimer.stop();
        }

        undoTimer = new javax.swing.Timer(5000, e -> undoPanel.setVisible(false));
        undoTimer.setRepeats(false);
        undoTimer.start();
    }

    private void deleteSubject(Subject subject) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete \"" + subject.getName() + "\"? This will remove all attendance records too.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            student.removeSubject(subject);
            DatabaseManager.getInstance().deleteSubject(subject.getId());
            // Rebuild schedule
            rebuildSchedule();
            refreshDashboard();
        }
    }

    private void rebuildSchedule() {
        schedule = new WeeklySchedule();
        DatabaseManager.getInstance().loadSchedule(schedule, student.getSubjects());
    }

    private void showEditSubjectDialog(Subject subject) {
        SubjectDetailDialog dialog = new SubjectDetailDialog(this, subject, student);
        dialog.setVisible(true);
        if (dialog.isChanged()) {
            refreshDashboard();
        }
    }

    private void showAddSubjectDialog(ActionEvent e) {
        JDialog dialog = new JDialog(this, "Add New Subject", true);
        dialog.setSize(450, 420);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(CARD_COLOR);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        JLabel nameLabel = new JLabel("Subject Name:");
        nameLabel.setForeground(TEXT_COLOR);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JTextField nameField = new UIUtils.RoundedTextField(15, 10);
        nameField.setBackground(SURFACE);
        nameField.setForeground(TEXT_COLOR);
        nameField.setCaretColor(TEXT_COLOR);

        JLabel daysLabel = new JLabel("Class Days:");
        daysLabel.setForeground(TEXT_COLOR);
        daysLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel daysPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        daysPanel.setBackground(CARD_COLOR);
        JCheckBox mon = createDayCheckBox("Mon");
        JCheckBox tue = createDayCheckBox("Tue");
        JCheckBox wed = createDayCheckBox("Wed");
        JCheckBox thu = createDayCheckBox("Thu");
        JCheckBox fri = createDayCheckBox("Fri");
        JCheckBox sat = createDayCheckBox("Sat");
        daysPanel.add(mon);
        daysPanel.add(tue);
        daysPanel.add(wed);
        daysPanel.add(thu);
        daysPanel.add(fri);
        daysPanel.add(sat);

        // Optional: Initial attendance
        JLabel initLabel = new JLabel("Initial Attendance (optional, for joining mid-semester):");
        initLabel.setForeground(SUBTEXT_COLOR);
        initLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        JLabel condLabel = new JLabel("Classes Conducted:");
        condLabel.setForeground(TEXT_COLOR);
        JTextField condField = new JTextField("0");
        condField.setBackground(SURFACE);
        condField.setForeground(TEXT_COLOR);
        condField.setCaretColor(TEXT_COLOR);

        JLabel attLabel = new JLabel("Classes Attended:");
        attLabel.setForeground(TEXT_COLOR);
        JTextField attField = new JTextField("0");
        attField.setBackground(SURFACE);
        attField.setForeground(TEXT_COLOR);
        attField.setCaretColor(TEXT_COLOR);

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row++;
        inputPanel.add(nameLabel, gbc);
        gbc.gridy = row++;
        inputPanel.add(nameField, gbc);
        gbc.gridy = row++;
        inputPanel.add(daysLabel, gbc);
        gbc.gridy = row++;
        inputPanel.add(daysPanel, gbc);
        gbc.gridy = row++;
        inputPanel.add(initLabel, gbc);
        gbc.gridwidth = 1;
        gbc.gridy = row;
        gbc.gridx = 0;
        inputPanel.add(condLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(condField, gbc);
        gbc.gridy = ++row;
        gbc.gridx = 0;
        inputPanel.add(attLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(attField, gbc);

        mainPanel.add(inputPanel, BorderLayout.CENTER);

        JButton saveBtn = new UIUtils.RoundedButton("Save Subject", ACCENT_COLOR, HEADER_COLOR, 12);

        saveBtn.addActionListener(event -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a subject name.");
                return;
            }

            // Collect selected days
            List<DayOfWeek> selectedDays = new ArrayList<>();
            if (mon.isSelected())
                selectedDays.add(DayOfWeek.MONDAY);
            if (tue.isSelected())
                selectedDays.add(DayOfWeek.TUESDAY);
            if (wed.isSelected())
                selectedDays.add(DayOfWeek.WEDNESDAY);
            if (thu.isSelected())
                selectedDays.add(DayOfWeek.THURSDAY);
            if (fri.isSelected())
                selectedDays.add(DayOfWeek.FRIDAY);
            if (sat.isSelected())
                selectedDays.add(DayOfWeek.SATURDAY);

            if (selectedDays.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select at least one day.");
                return;
            }

            // Parse initial attendance
            int conducted = 0, attended = 0;
            try {
                conducted = Integer.parseInt(condField.getText().trim());
                attended = Integer.parseInt(attField.getText().trim());
                if (attended > conducted) {
                    JOptionPane.showMessageDialog(dialog, "Attended cannot exceed conducted.");
                    return;
                }
                if (conducted < 0 || attended < 0) {
                    JOptionPane.showMessageDialog(dialog, "Values cannot be negative.");
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Enter valid numbers for attendance.");
                return;
            }

            // Create subject
            Subject newSubject = new Subject(name, selectedDays.size());

            // Save to DB
            int subjectId = DatabaseManager.getInstance().addSubject(student.getId(), name, selectedDays.size());
            if (subjectId < 0) {
                JOptionPane.showMessageDialog(dialog, "Error saving subject.");
                return;
            }
            newSubject.setId(subjectId);

            // Save schedule
            DatabaseManager.getInstance().saveSchedule(subjectId, selectedDays);

            // Set initial attendance
            if (conducted > 0) {
                newSubject.setAttendance(conducted, attended,
                        selectedDays, student.getSemesterStartDate(),
                        student.getHolidayDates(), student::isDuringMidsemExams);
                DatabaseManager.getInstance().saveInitialAttendance(
                        subjectId, conducted, attended, selectedDays, student);
            }

            // Add to student
            student.addSubject(newSubject);

            // Update schedule
            for (DayOfWeek day : selectedDays) {
                schedule.addClass(day, newSubject);
            }

            refreshDashboard();
            dialog.dispose();
        });

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(BG_COLOR);
        btnPanel.add(saveBtn);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    private JCheckBox createDayCheckBox(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setBackground(CARD_COLOR);
        cb.setForeground(TEXT_COLOR);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setFocusPainted(false);
        return cb;
    }

    private void showPredictionDialog(ActionEvent e) {
        new PredictionDialog(this, student, schedule).setVisible(true);
    }

    /**
     * Show a dialog to change the current user's password.
     * Requires old password verification and new password validation.
     */
    private void showChangePasswordDialog() {
        JDialog dialog = new JDialog(this, "Change Password", true);
        dialog.setSize(420, 400);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(CARD_COLOR);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 5, 6, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        // Fields
        JLabel oldLabel = new JLabel("Current Password:");
        oldLabel.setForeground(TEXT_COLOR);
        oldLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JPasswordField oldField = new JPasswordField(20);
        stylePasswordField(oldField);

        JLabel newLabel = new JLabel("New Password:");
        newLabel.setForeground(TEXT_COLOR);
        newLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JPasswordField newField = new JPasswordField(20);
        stylePasswordField(newField);

        JLabel confirmLabel = new JLabel("Confirm New Password:");
        confirmLabel.setForeground(TEXT_COLOR);
        confirmLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JPasswordField confirmField = new JPasswordField(20);
        stylePasswordField(confirmField);

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(RED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Layout
        int row = 0;
        gbc.gridy = row++;
        formPanel.add(oldLabel, gbc);
        gbc.gridy = row++;
        formPanel.add(oldField, gbc);
        gbc.gridy = row++;
        formPanel.add(newLabel, gbc);
        gbc.gridy = row++;
        formPanel.add(newField, gbc);
        gbc.gridy = row++;
        formPanel.add(confirmLabel, gbc);
        gbc.gridy = row++;
        formPanel.add(confirmField, gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(12, 5, 6, 5);
        formPanel.add(statusLabel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Save button
        JButton saveBtn = new JButton("Update Password");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveBtn.setBackground(ACCENT_COLOR);
        saveBtn.setForeground(HEADER_COLOR);
        saveBtn.setFocusPainted(false);
        saveBtn.setOpaque(true);
        saveBtn.setBorderPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        saveBtn.addActionListener(e -> {
            String oldPw = new String(oldField.getPassword());
            String newPw = new String(newField.getPassword());
            String confirmPw = new String(confirmField.getPassword());

            // Verify current password
            String storedHash = DatabaseManager.getInstance().getPasswordHash(student.getId());
            String oldHash = PasswordValidator.hashPassword(oldPw);
            if (!oldHash.equals(storedHash)) {
                statusLabel.setText("Current password is incorrect.");
                return;
            }

            // Validate new password
            String validationError = PasswordValidator.validate(newPw);
            if (validationError != null) {
                statusLabel.setText(validationError);
                return;
            }

            // Check match
            if (!newPw.equals(confirmPw)) {
                statusLabel.setText("New passwords do not match.");
                return;
            }

            // Check not same as old
            if (oldPw.equals(newPw)) {
                statusLabel.setText("New password must be different from current.");
                return;
            }

            // Save
            String newHash = PasswordValidator.hashPassword(newPw);
            DatabaseManager.getInstance().updatePasswordHash(student.getId(), newHash);

            JOptionPane.showMessageDialog(dialog,
                    "Password changed successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(BG_COLOR);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        btnPanel.add(saveBtn);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    private void stylePasswordField(JPasswordField f) {
        f.setBackground(SURFACE);
        f.setForeground(TEXT_COLOR);
        f.setCaretColor(TEXT_COLOR);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(88, 91, 112), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    @Override
    public void onThemeChanged(boolean isDarkMode) {
        // Rebuild the entire window with new theme colors
        ThemeManager.removeThemeChangeListener(this);
        dispose();
        SwingUtilities.invokeLater(() -> {
            MainWindow newWindow = new MainWindow(student);
            newWindow.setVisible(true);
        });
    }
}
