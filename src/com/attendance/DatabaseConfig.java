package com.attendance;

/**
 * Database configuration constants.
 * Centralized database connection parameters.
 */
public class DatabaseConfig {
    // JDBC Connection Parameters
    public static final String DB_URL = "jdbc:mysql://localhost:3306/attendance_system";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "Archit@123";

    // Connection Pool Settings
    public static final int MAX_RETRIES = 3;
    public static final int RETRY_DELAY_MS = 1000;

    private DatabaseConfig() {
        // Prevent instantiation — utility class
    }
}
