package com.attendance;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Login and Registration dialog.
 * First screen the user sees. Allows login or new account creation.
 * Password validation with real-time feedback during registration.
 */
public class LoginDialog extends JDialog {

    private Student authenticatedStudent;
    private boolean loginSuccessful = false;

    // UI Colors mapped to ThemeManager (default is dark before login)
    private static final Color BG_COLOR = ThemeManager.getBgColor();
    private static final Color CARD_COLOR = ThemeManager.getCardColor();
    private static final Color ACCENT_COLOR = ThemeManager.getAccentColor();
    private static final Color TEXT_COLOR = ThemeManager.getTextColor();
    private static final Color ERROR_COLOR = ThemeManager.getRedColor();
    private static final Color SUCCESS_COLOR = ThemeManager.getGreenColor();
    private static final Color FIELD_BG = ThemeManager.getFieldBgColor();

    public LoginDialog(Frame owner) {
        super(owner, "Attendance Manager — Login", true);
        setSize(500, 680);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        // ── Header ──
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("🎓 Attendance Manager");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(ACCENT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Track. Plan. Stay Eligible.");
        subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        subtitleLabel.setForeground(TEXT_COLOR);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ── Tabbed Pane (Login / Register) ──
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(CARD_COLOR);
        tabbedPane.setForeground(TEXT_COLOR);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));

        tabbedPane.addTab("  Login  ", createLoginPanel());

        // Wrap register panel in scroll pane so all fields + button are accessible
        JPanel registerPanel = createRegisterPanel();
        JScrollPane registerScroll = new JScrollPane(registerPanel);
        registerScroll.setBorder(null);
        registerScroll.getVerticalScrollBar().setUnitIncrement(12);
        registerScroll.getViewport().setBackground(CARD_COLOR);
        tabbedPane.addTab("  Register  ", registerScroll);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(BG_COLOR);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(5, 30, 15, 30));
        centerWrapper.add(tabbedPane, BorderLayout.CENTER);

        mainPanel.add(centerWrapper, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_COLOR);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = createLabel("Username:");
        JTextField userField = createTextField();
        JLabel passLabel = createLabel("Password:");
        JPasswordField passField = createPasswordField();
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(ERROR_COLOR);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton loginBtn = createButton("Login", ACCENT_COLOR);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(userLabel, gbc);
        gbc.gridy = 1;
        panel.add(userField, gbc);
        gbc.gridy = 2;
        panel.add(passLabel, gbc);
        gbc.gridy = 3;
        panel.add(passField, gbc);
        gbc.gridy = 4;
        gbc.insets = new Insets(20, 5, 8, 5);
        panel.add(loginBtn, gbc);
        gbc.gridy = 5;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(statusLabel, gbc);

        loginBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please enter both username and password.");
                return;
            }

            // Pass plaintext password — DatabaseManager verifies against stored salted hash
            Student student = DatabaseManager.getInstance().authenticateStudent(username, password);

            if (student != null) {
                authenticatedStudent = student;
                loginSuccessful = true;
                // Load subjects, holidays, schedule
                loadStudentData(student);
                dispose();
            } else {
                statusLabel.setText("Invalid username or password.");
            }
        });

        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_COLOR);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        JLabel nameLabel = createLabel("Full Name:");
        JTextField nameField = createTextField();
        JLabel userLabel = createLabel("Username:");
        JTextField userField = createTextField();
        JLabel passLabel = createLabel("Password:");
        JPasswordField passField = createPasswordField();
        JLabel confirmLabel = createLabel("Confirm Password:");
        JPasswordField confirmField = createPasswordField();

        // Password strength indicators
        JPanel strengthPanel = new JPanel(new GridLayout(5, 1, 0, 1));
        strengthPanel.setBackground(CARD_COLOR);
        JLabel len = createStrengthLabel("✗ 8+ characters");
        JLabel upper = createStrengthLabel("✗ Uppercase letter");
        JLabel lower = createStrengthLabel("✗ Lowercase letter");
        JLabel digit = createStrengthLabel("✗ Number");
        JLabel special = createStrengthLabel("✗ Special character");
        strengthPanel.add(len);
        strengthPanel.add(upper);
        strengthPanel.add(lower);
        strengthPanel.add(digit);
        strengthPanel.add(special);

        // Real-time validation
        passField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String pw = new String(passField.getPassword());
                updateIndicator(len, pw.length() >= 8, "8+ characters");
                updateIndicator(upper, PasswordValidator.hasUppercase(pw), "Uppercase letter");
                updateIndicator(lower, PasswordValidator.hasLowercase(pw), "Lowercase letter");
                updateIndicator(digit, PasswordValidator.hasDigit(pw), "Number");
                updateIndicator(special, PasswordValidator.hasSpecialChar(pw), "Special character");
            }

            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(ERROR_COLOR);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton registerBtn = createButton("Create Account", SUCCESS_COLOR);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(nameLabel, gbc);
        gbc.gridy = 1;
        panel.add(nameField, gbc);
        gbc.gridy = 2;
        panel.add(userLabel, gbc);
        gbc.gridy = 3;
        panel.add(userField, gbc);
        gbc.gridy = 4;
        panel.add(passLabel, gbc);
        gbc.gridy = 5;
        panel.add(passField, gbc);
        gbc.gridy = 6;
        panel.add(strengthPanel, gbc);
        gbc.gridy = 7;
        panel.add(confirmLabel, gbc);
        gbc.gridy = 8;
        panel.add(confirmField, gbc);
        gbc.gridy = 9;
        gbc.insets = new Insets(15, 5, 5, 5);
        panel.add(registerBtn, gbc);
        gbc.gridy = 10;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(statusLabel, gbc);

        registerBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());
            String confirm = new String(confirmField.getPassword());

            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("All fields are required.");
                return;
            }

            if (username.length() < 4) {
                statusLabel.setText("Username must be at least 4 characters.");
                return;
            }

            String validationError = PasswordValidator.validate(password);
            if (validationError != null) {
                statusLabel.setText(validationError);
                return;
            }

            if (!password.equals(confirm)) {
                statusLabel.setText("Passwords do not match.");
                return;
            }

            if (DatabaseManager.getInstance().usernameExists(username)) {
                statusLabel.setText("Username already taken.");
                return;
            }

            String hash = PasswordValidator.hashPassword(password);
            int id = DatabaseManager.getInstance().registerStudent(name, username, hash);

            if (id > 0) {
                authenticatedStudent = new Student(name);
                authenticatedStudent.setId(id);
                authenticatedStudent.setUsername(username);
                loginSuccessful = true;
                JOptionPane.showMessageDialog(this,
                        "Account created successfully! Welcome, " + name + "!",
                        "Registration Successful",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                statusLabel.setText("Registration failed. Please try again.");
            }
        });

        return panel;
    }

    // ── Helper Methods ──

    private void loadStudentData(Student student) {
        // Load subjects with attendance records
        java.util.List<Subject> subjects = DatabaseManager.getInstance().loadSubjects(student.getId());
        for (Subject s : subjects) {
            student.addSubject(s);
        }
        // Load holidays
        student.setHolidays(DatabaseManager.getInstance().loadHolidays(student.getId()));
    }

    private void updateIndicator(JLabel label, boolean met, String text) {
        if (met) {
            label.setText("✓ " + text);
            label.setForeground(SUCCESS_COLOR);
        } else {
            label.setText("✗ " + text);
            label.setForeground(ERROR_COLOR);
        }
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_COLOR);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }

    private JLabel createStrengthLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(ERROR_COLOR);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return l;
    }

    private JTextField createTextField() {
        UIUtils.RoundedTextField f = new UIUtils.RoundedTextField(20, 12);
        f.setBackground(FIELD_BG);
        f.setForeground(TEXT_COLOR);
        f.setCaretColor(TEXT_COLOR);
        return f;
    }

    private JPasswordField createPasswordField() {
        UIUtils.RoundedPasswordField f = new UIUtils.RoundedPasswordField(20, 12);
        f.setBackground(FIELD_BG);
        f.setForeground(TEXT_COLOR);
        f.setCaretColor(TEXT_COLOR);
        return f;
    }

    private JButton createButton(String text, Color bgColor) {
        return new UIUtils.RoundedButton(text, bgColor, BG_COLOR, 12);
    }

    // ── Public Accessors ──

    public Student getAuthenticatedStudent() {
        return authenticatedStudent;
    }

    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }
}
