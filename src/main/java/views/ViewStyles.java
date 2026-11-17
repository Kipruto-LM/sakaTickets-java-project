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

    private ViewStyles() {
        // Private constructor prevents instantiation of this utility class.
    }

    /**
     * Updates the shared semantic colors based on the active theme preference.
     * 
     * @param dark   True if the application is in Dark Mode, false for Light Mode.
     * @param accent The user-selected accent color for primary actions.
     */
    static void setThemeColors(boolean dark, Color accent) {
        // High-contrast Light Mode applies pure white surfaces on an off-white background
        BACKGROUND = dark ? new Color(0x0F, 0x11, 0x17) : new Color(0xF3, 0xF4, 0xF6);
        SURFACE = dark ? new Color(0x17, 0x1B, 0x24) : new Color(0xFF, 0xFF, 0xFF);
        INPUT = dark ? new Color(0x2A, 0x2A, 0x2A) : new Color(0xF9, 0xFA, 0xFB);
        FOREGROUND = dark ? new Color(0xF0, 0xF4, 0xFF) : new Color(0x11, 0x18, 0x27);
        MUTED = dark ? new Color(0x88, 0x96, 0xB3) : new Color(0x6B, 0x72, 0x80);
        OUTLINE = dark ? new Color(0x2A, 0x30, 0x44) : new Color(0xE5, 0xE7, 0xEB);
        ACCENT = accent == null ? new Color(0x00, 0xB4, 0xFF) : accent;
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