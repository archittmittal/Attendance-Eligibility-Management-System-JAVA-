package com.attendance;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized theme management for the application.
 * Supports Dark (Catppuccin Mocha) and Light (Catppuccin Latte) themes.
 * Theme preference is persisted in the database.
 */
public class ThemeManager {

    public interface ThemeChangeListener {
        void onThemeChanged(boolean isDarkMode);
    }

    private static boolean darkMode = true;
    private static final List<ThemeChangeListener> listeners = new ArrayList<>();

    // ═══ Dark Theme (Catppuccin Mocha) ═══
    private static final Color DARK_BG = new Color(30, 30, 46);
    private static final Color DARK_CARD = new Color(49, 50, 68);
    private static final Color DARK_HEADER = new Color(24, 24, 37);
    private static final Color DARK_ACCENT = new Color(137, 180, 250);
    private static final Color DARK_TEXT = new Color(205, 214, 244);
    private static final Color DARK_SUBTEXT = new Color(147, 153, 178);
    private static final Color DARK_SURFACE = new Color(69, 71, 90);
    private static final Color DARK_FIELD_BG = new Color(69, 71, 90);
    private static final Color DARK_GREEN = new Color(166, 227, 161);
    private static final Color DARK_RED = new Color(243, 139, 168);
    private static final Color DARK_YELLOW = new Color(249, 226, 175);
    private static final Color DARK_BORDER = new Color(88, 91, 112);

    // ═══ Light Theme (Catppuccin Latte) ═══
    private static final Color LIGHT_BG = new Color(239, 241, 245);
    private static final Color LIGHT_CARD = new Color(255, 255, 255);
    private static final Color LIGHT_HEADER = new Color(220, 224, 232);
    private static final Color LIGHT_ACCENT = new Color(30, 102, 245);
    private static final Color LIGHT_TEXT = new Color(76, 79, 105);
    private static final Color LIGHT_SUBTEXT = new Color(108, 111, 133);
    private static final Color LIGHT_SURFACE = new Color(204, 208, 218);
    private static final Color LIGHT_FIELD_BG = new Color(230, 233, 239);
    private static final Color LIGHT_GREEN = new Color(64, 160, 43);
    private static final Color LIGHT_RED = new Color(210, 15, 57);
    private static final Color LIGHT_YELLOW = new Color(223, 142, 29);
    private static final Color LIGHT_BORDER = new Color(172, 176, 190);

    // ═══ Getters ═══
    public static Color getBgColor() {
        return darkMode ? DARK_BG : LIGHT_BG;
    }

    public static Color getCardColor() {
        return darkMode ? DARK_CARD : LIGHT_CARD;
    }

    public static Color getHeaderColor() {
        return darkMode ? DARK_HEADER : LIGHT_HEADER;
    }

    public static Color getAccentColor() {
        return darkMode ? DARK_ACCENT : LIGHT_ACCENT;
    }

    public static Color getTextColor() {
        return darkMode ? DARK_TEXT : LIGHT_TEXT;
    }

    public static Color getSubtextColor() {
        return darkMode ? DARK_SUBTEXT : LIGHT_SUBTEXT;
    }

    public static Color getSurfaceColor() {
        return darkMode ? DARK_SURFACE : LIGHT_SURFACE;
    }

    public static Color getFieldBgColor() {
        return darkMode ? DARK_FIELD_BG : LIGHT_FIELD_BG;
    }

    public static Color getGreenColor() {
        return darkMode ? DARK_GREEN : LIGHT_GREEN;
    }

    public static Color getRedColor() {
        return darkMode ? DARK_RED : LIGHT_RED;
    }

    public static Color getYellowColor() {
        return darkMode ? DARK_YELLOW : LIGHT_YELLOW;
    }

    public static Color getBorderColor() {
        return darkMode ? DARK_BORDER : LIGHT_BORDER;
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    /**
     * Toggle between dark and light mode.
     */
    public static void toggleTheme() {
        darkMode = !darkMode;
        notifyListeners();
    }

    /**
     * Set theme explicitly.
     */
    public static void setDarkMode(boolean dark) {
        if (darkMode != dark) {
            darkMode = dark;
            notifyListeners();
        }
    }

    /**
     * Load theme preference from database for a student.
     */
    public static void loadTheme(int studentId) {
        String theme = DatabaseManager.getInstance().loadTheme(studentId);
        darkMode = !"light".equalsIgnoreCase(theme);
    }

    /**
     * Save theme preference to database.
     */
    public static void saveTheme(int studentId) {
        DatabaseManager.getInstance().saveTheme(studentId, darkMode ? "dark" : "light");
    }

    // ═══ Listener Management ═══
    public static void addThemeChangeListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }

    public static void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (ThemeChangeListener listener : new ArrayList<>(listeners)) {
            listener.onThemeChanged(darkMode);
        }
    }

    /**
     * Get the theme toggle button label.
     */
    public static String getToggleLabel() {
        return darkMode ? "☀️ Light Mode" : "🌙 Dark Mode";
    }
}
