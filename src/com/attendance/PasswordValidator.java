package com.attendance;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Password validation and hashing utility.
 * Enforces strong password requirements:
 * - Minimum 8 characters
 * - At least 1 uppercase letter (A-Z)
 * - At least 1 lowercase letter (a-z)
 * - At least 1 digit (0-9)
 * - At least 1 special character (@#$%^&*!_-)
 *
 * Uses salted SHA-256 hashing. Format: "salt:hash" (Base64 salt + hex hash).
 * Falls back to legacy unsalted SHA-256 for existing accounts, then
 * upgrades them on next successful login.
 */
public class PasswordValidator {

    public static final int MIN_LENGTH = 8;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordValidator() {
        // Utility class
    }

    // ══════════════════════════════════════════════
    // VALIDATION RULES
    // ══════════════════════════════════════════════

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

    // ══════════════════════════════════════════════
    // SALTED HASHING (NEW — Secure)
    // ══════════════════════════════════════════════

    /**
     * Hash a password with a random 16-byte salt.
     * Returns "base64Salt:hexHash".
     */
    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hexHash = sha256(salt, password);
        return saltB64 + ":" + hexHash;
    }

    /**
     * Verify a plaintext password against a stored hash.
     * Supports both:
     * - New format: "base64Salt:hexHash" (salted)
     * - Legacy format: plain 64-char hex SHA-256 (unsalted)
     *
     * @return true if the password matches
     */
    public static boolean checkPassword(String password, String storedHash) {
        if (storedHash == null || password == null) {
            return false;
        }

        if (storedHash.contains(":")) {
            // New salted format — "base64Salt:hexHash"
            String[] parts = storedHash.split(":", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            String expectedHash = parts[1];
            return expectedHash.equals(sha256(salt, password));
        } else {
            // Legacy unsalted SHA-256 — compare directly
            return storedHash.equals(sha256Unsalted(password));
        }
    }

    /**
     * Check if a stored hash is in the old unsalted format.
     * Used to trigger automatic upgrade on next login.
     */
    public static boolean isLegacyHash(String storedHash) {
        // Legacy hashes are exactly 64 hex chars with no colon
        return storedHash != null
                && !storedHash.contains(":")
                && storedHash.length() == 64
                && storedHash.matches("[0-9a-f]+");
    }

    // ══════════════════════════════════════════════
    // INTERNAL HELPERS
    // ══════════════════════════════════════════════

    /** SHA-256(salt + password) → hex string. */
    private static String sha256(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Legacy unsalted SHA-256(password) → hex string. */
    private static String sha256Unsalted(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
