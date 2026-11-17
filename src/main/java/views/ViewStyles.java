package views;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Shared style helpers for the views. Keeping colors, fonts, and common button
 * styles here avoids repeating the same presentation code in every screen.
 *
 * Core concepts:
 * - Encapsulation: palette and typography values are centralized in one class.
 * - Abstraction: screens request a style by role instead of repeating Swing settings.
 * - Static utility methods: the class is not instantiated and shares common behavior.
 */
final class ViewStyles {
    // Global palette tokens
    static Color BACKGROUND = new Color(0x12, 0x12, 0x12);
    static Color SURFACE = new Color(0x1E, 0x1E, 0x1E);
    static Color INPUT = new Color(0x2A, 0x2A, 0x2A);
    static Color ACCENT = new Color(0x0D, 0x6E, 0xFD);
    static Color FOREGROUND = Color.WHITE;
    static Color MUTED = new Color(0xA0, 0xA0, 0xA0);
    static Color OUTLINE = new Color(0x38, 0x38, 0x38);
    
    // Global typography tokens
    static final Font DISPLAY_FONT = new Font("Ubuntu", Font.PLAIN, 16);
    static final Font BODY_FONT = new Font("Ubuntu Sans", Font.PLAIN, 14);
    static final Font DATA_FONT = new Font("Ubuntu Mono", Font.PLAIN, 12);

    /** Prevents instances because all style values and methods are shared statically. */
    private ViewStyles() {
        // Private constructor prevents instantiation of this utility class.
    }

    /**
     * Styles a label for primary headings (e.g., page titles, event names).
     * 
     * @param label The JLabel to style.
     * @param size  The font size to apply.
     */
    static void styleHeading(JLabel label, float size) {
        label.setForeground(FOREGROUND);
        label.setFont(DISPLAY_FONT.deriveFont(Font.BOLD, size));
    }

    /**
     * Styles a label for secondary text (e.g., descriptions, dates, hints).
     * 
     * @param label The JLabel to style.
     * @param size  The font size to apply.
     */
    static void styleSecondaryText(JLabel label, float size) {
        label.setForeground(MUTED);
        label.setFont(BODY_FONT.deriveFont(Font.PLAIN, size));
    }

    /**
     * Styles a button as a primary call-to-action, injecting FlatLaf properties
     * for rounded corners and dynamic hover/pressed color state changes.
     * 
     * @param button The JButton to style.
     */
    static void stylePrimaryButton(JButton button) {
        button.setForeground(FOREGROUND);
        button.setBackground(ACCENT);
        button.setFont(BODY_FONT.deriveFont(Font.BOLD, 14f));
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        button.putClientProperty("JButton.buttonType", "roundRect");
        
        // Adds web-like CSS hover and pressed effects via FlatLaf
        button.putClientProperty("FlatLaf.style", 
            "hoverBackground: lighten($Button.background, 10%); " +
            "pressedBackground: darken($Button.background, 10%)");
            
        button.setRolloverEnabled(true);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    /**
     * Styles a button as a secondary action (outlined, matching the surface color).
     * 
     * @param button The JButton to style.
     */
    static void styleSecondaryButton(JButton button) {
        button.setForeground(FOREGROUND);
        button.setBackground(SURFACE);
        button.setFont(BODY_FONT.deriveFont(Font.BOLD, 14f));
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        button.putClientProperty("JButton.buttonType", "roundRect");
        
        // Subtle hover effect for secondary buttons
        button.putClientProperty("FlatLaf.style", 
            "hoverBackground: darken($Button.background, 5%); " +
            "pressedBackground: darken($Button.background, 10%)");
            
        button.setRolloverEnabled(true);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    /**
     * Applies the shared form-field styling including surface color, text color, 
     * caret visibility, and inset padding.
     * 
     * @param field The JTextField to style.
     */
    static void styleInput(JTextField field) {
        field.setForeground(FOREGROUND);
        field.setBackground(INPUT);
        field.setCaretColor(FOREGROUND);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }
}