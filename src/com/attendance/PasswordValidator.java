package com.attendance;

/**
 * Password validation utility.
 * Enforces strong password requirements:
 * - Minimum 8 characters
 * - At least 1 uppercase letter (A-Z)
 * - At least 1 lowercase letter (a-z)
 * - At least 1 digit (0-9)
 * - At least 1 special character (@#$%^&*!_-)
 */
public class PasswordValidator {

    public static final int MIN_LENGTH = 8;

    private PasswordValidator() {
        // Utility class
    }

    /**
     * Validate password against all rules.
     * Returns null if valid, or an error message describing the first failure.
     */
    public static String validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return "Password must be at least " + MIN_LENGTH + " characters.";
        }
        if (!hasUppercase(password)) {
            return "Password must contain at least 1 uppercase letter (A-Z).";
        }
        if (!hasLowercase(password)) {
            return "Password must contain at least 1 lowercase letter (a-z).";
        }
        if (!hasDigit(password)) {
            return "Password must contain at least 1 digit (0-9).";
        }
        if (!hasSpecialChar(password)) {
            return "Password must contain at least 1 special character (@#$%^&*!_-).";
        }
        return null; // Valid
    }

    public static boolean hasUppercase(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isUpperCase(c))
                return true;
        }
        return false;
    }

    public static boolean hasLowercase(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isLowerCase(c))
                return true;
        }
        return false;
    }

    public static boolean hasDigit(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c))
                return true;
        }
        return false;
    }

    public static boolean hasSpecialChar(String s) {
        String specials = "@#$%^&*!_-";
        for (char c : s.toCharArray()) {
            if (specials.indexOf(c) >= 0)
                return true;
        }
        return false;
    }

    /**
     * Simple hash function for password storage.
     * Uses SHA-256 for basic security.
     */
    public static String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
