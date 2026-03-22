package com.attendance;

import javax.swing.*;
import java.awt.*;

/**
 * Email settings dialog — configure email address, frequency (off/weekly/monthly),
 * and send test emails. Follows the app's theme patterns.
 */
public class EmailSettingsDialog extends JDialog {

    private JTextField emailField;
    private JComboBox<String> frequencyCombo;
    private final Student student;

    private final Color BG_COLOR = ThemeManager.getBgColor();
    private final Color CARD_COLOR = ThemeManager.getCardColor();
    private final Color HEADER_COLOR = ThemeManager.getHeaderColor();
    private final Color ACCENT_COLOR = ThemeManager.getAccentColor();
    private final Color TEXT_COLOR = ThemeManager.getTextColor();
    private final Color SUBTEXT_COLOR = ThemeManager.getSubtextColor();
    private final Color SURFACE = ThemeManager.getSurfaceColor();
    private final Color GREEN = ThemeManager.getGreenColor();
    private final Color RED = ThemeManager.getRedColor();

    public EmailSettingsDialog(JFrame parent, Student student) {
        super(parent, "📧 Email Report Settings", true);
        this.student = student;

        setSize(480, 420);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        // ── Header ──
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(HEADER_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("📧 Email Report Settings");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(ACCENT_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Receive automatic attendance reports in your inbox");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(SUBTEXT_COLOR);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        headerPanel.add(subtitleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ── Form ──
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(CARD_COLOR);
        formPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        // Email label
        JLabel emailLabel = new JLabel("Email Address:");
        emailLabel.setForeground(TEXT_COLOR);
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(emailLabel, gbc);

        // Email field
        emailField = new UIUtils.RoundedTextField(20, 10);
        emailField.setBackground(SURFACE);
        emailField.setForeground(TEXT_COLOR);
        emailField.setCaretColor(TEXT_COLOR);
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emailField.setText(student.getEmail() != null ? student.getEmail() : "");
        gbc.gridy = 1;
        formPanel.add(emailField, gbc);

        // Hint
        JLabel hintLabel = new JLabel("Reports will be sent to this email address");
        hintLabel.setForeground(SUBTEXT_COLOR);
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        gbc.gridy = 2;
        formPanel.add(hintLabel, gbc);

        // Frequency label
        JLabel freqLabel = new JLabel("Report Frequency:");
        freqLabel.setForeground(TEXT_COLOR);
        freqLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridy = 3;
        gbc.insets = new Insets(15, 5, 8, 5);
        formPanel.add(freqLabel, gbc);

        // Frequency dropdown
        frequencyCombo = new JComboBox<>(new String[]{"Off", "Weekly", "Monthly"});
        frequencyCombo.setBackground(SURFACE);
        frequencyCombo.setForeground(TEXT_COLOR);
        frequencyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        // Set current value
        String freq = student.getEmailFrequency();
        if ("weekly".equalsIgnoreCase(freq)) frequencyCombo.setSelectedIndex(1);
        else if ("monthly".equalsIgnoreCase(freq)) frequencyCombo.setSelectedIndex(2);
        else frequencyCombo.setSelectedIndex(0);
        gbc.gridy = 4;
        gbc.insets = new Insets(8, 5, 8, 5);
        formPanel.add(frequencyCombo, gbc);

        // Last sent info
        String lastSentText = student.getLastEmailSent() != null
                ? "Last report sent: " + student.getLastEmailSent()
                : "No reports sent yet";
        JLabel lastSentLabel = new JLabel(lastSentText);
        lastSentLabel.setForeground(SUBTEXT_COLOR);
        lastSentLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        gbc.gridy = 5;
        formPanel.add(lastSentLabel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // ── Buttons ──
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        buttonPanel.setBackground(BG_COLOR);

        JButton testBtn = new UIUtils.RoundedButton("📤 Send Test Email", ACCENT_COLOR, HEADER_COLOR, 10);
        testBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        testBtn.addActionListener(e -> sendTestEmail());

        JButton saveBtn = new UIUtils.RoundedButton("💾 Save", GREEN, HEADER_COLOR, 10);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.addActionListener(e -> saveSettings());

        JButton cancelBtn = new UIUtils.RoundedButton("Cancel", SURFACE, TEXT_COLOR, 10);
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(testBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void sendTestEmail() {
        String email = emailField.getText().trim();
        if (email.isEmpty() || !email.contains("@")) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid email address.",
                    "Invalid Email", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show progress
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Run in background thread
        new Thread(() -> {
            boolean success = EmailReportGenerator.sendReport(email,
                    "📊 Test Attendance Report", student);

            SwingUtilities.invokeLater(() -> {
                setCursor(Cursor.getDefaultCursor());
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "✅ Test email sent successfully!\nCheck your inbox at: " + email,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "❌ Failed to send email.\n\nPlease verify:\n"
                                    + "1. EmailConfig.java has correct Gmail credentials\n"
                                    + "2. You're using a Gmail App Password (not regular password)\n"
                                    + "3. Your internet connection is active",
                            "Email Failed", JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    private void saveSettings() {
        String email = emailField.getText().trim();
        String frequency = frequencyCombo.getSelectedItem().toString().toLowerCase();

        if (!"off".equals(frequency) && (email.isEmpty() || !email.contains("@"))) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid email address to enable reports.",
                    "Invalid Email", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Update model
        student.setEmail(email);
        student.setEmailFrequency(frequency);

        // Save to DB
        DatabaseManager.getInstance().saveEmailSettings(student);

        JOptionPane.showMessageDialog(this,
                "✅ Email settings saved!",
                "Saved", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
