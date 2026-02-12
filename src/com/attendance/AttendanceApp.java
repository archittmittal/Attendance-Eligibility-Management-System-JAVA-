package com.attendance;

import javax.swing.*;

public class AttendanceApp {
    public static void main(String[] args) {
        // Set Look and Feel to System default for better integration
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new MainWindow().setVisible(true);
        });
    }
}
