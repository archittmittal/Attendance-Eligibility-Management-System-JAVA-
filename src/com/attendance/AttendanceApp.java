package com.attendance;

import javax.swing.*;

/**
 * Application entry point.
 * Flow: Login/Register → Semester Setup (if first time) → Dashboard
 * All data persisted in MySQL via JDBC.
 */
public class AttendanceApp {
    public static void main(String[] args) {
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            // Set global dark theme defaults
            UIManager.put("OptionPane.background", new java.awt.Color(30, 30, 46));
            UIManager.put("Panel.background", new java.awt.Color(30, 30, 46));
            UIManager.put("OptionPane.messageForeground", new java.awt.Color(205, 214, 244));
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            // Test database connection first
            if (!DatabaseManager.getInstance().testConnection()) {
                JOptionPane.showMessageDialog(null,
                        "Cannot connect to MySQL database.\n"
                                + "Please ensure MySQL is running and credentials are correct.\n\n"
                                + "URL: " + DatabaseConfig.DB_URL + "\n"
                                + "User: " + DatabaseConfig.DB_USER,
                        "Database Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            // Show Login Dialog
            LoginDialog loginDialog = new LoginDialog(null);
            loginDialog.setVisible(true);

            if (loginDialog.isLoginSuccessful()) {
                Student student = loginDialog.getAuthenticatedStudent();
                MainWindow mainWindow = new MainWindow(student);
                mainWindow.setVisible(true);
            } else {
                System.exit(0); // User closed login without logging in
            }
        });
    }
}
