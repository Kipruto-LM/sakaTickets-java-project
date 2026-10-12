// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import java.awt.Color;               // An RGB colour
import java.awt.Font;                // A font (family, style, size)
import javax.swing.BorderFactory;    // Helper for making borders
import javax.swing.JButton;          // A clickable button
import javax.swing.JLabel;           // A piece of text
import javax.swing.JTextField;       // A one-line text input box

/**
 * Shared colours, fonts and styling helpers used by every screen.
 *
 * OOP ideas shown here:
 * - Utility class: everything is static, so you call ViewStyles.stylePrimaryButton(button)
 *   without creating an object.
 * - "final" class and private constructor: nobody can extend it or create a ViewStyles object.
 * - Reuse (DRY - Don't Repeat Yourself): the styling lives in one place, so changing a colour
 *   here updates the whole app.
 * - No access modifier ("package-private"): only classes in the views package can use it.
 */
final class ViewStyles {
    // Colours. new Color(red, green, blue) - each from 0x00 (none) to 0xFF (full).
    static final Color BACKGROUND = new Color(0x12, 0x12, 0x12);   // Page background (near black)
    static final Color SURFACE = new Color(0x1E, 0x1E, 0x1E);      // Cards and secondary buttons
    static final Color INPUT = new Color(0x2A, 0x2A, 0x2A);        // Text boxes
    static final Color ACCENT = new Color(0x0D, 0x6E, 0xFD);       // Brand blue
    static final Color FOREGROUND = Color.WHITE;                   // Main text
    static final Color MUTED = new Color(0xA0, 0xA0, 0xA0);        // Less important text (grey)
    static final Color OUTLINE = new Color(0x38, 0x38, 0x38);      // Thin borders
    // Fonts: family name, style, size in points.
    static final Font DISPLAY_FONT = new Font("Ubuntu", Font.PLAIN, 16);       // Headings
    static final Font BODY_FONT = new Font("Ubuntu Sans", Font.PLAIN, 14);     // Normal text
    static final Font DATA_FONT = new Font("Ubuntu Mono", Font.PLAIN, 12);     // Numbers and small labels (monospace)

    // Private constructor: stops anyone from writing "new ViewStyles()".
    private ViewStyles() {
    }

    // Makes a label look like a heading: white, bold, at the given size.
    static void styleHeading(JLabel label, float size) {
        label.setForeground(FOREGROUND);
        label.setFont(DISPLAY_FONT.deriveFont(Font.BOLD, size));   // deriveFont = same font, new style/size
    }

    // Makes a label look like secondary text: grey, normal weight, at the given size.
    static void styleSecondaryText(JLabel label, float size) {
        label.setForeground(MUTED);
        label.setFont(BODY_FONT.deriveFont(Font.PLAIN, size));
    }

    // Styles the main call-to-action button: white text on brand blue.
    static void stylePrimaryButton(JButton button) {
        button.setForeground(FOREGROUND);                                  // Text colour
        button.setBackground(ACCENT);                                      // Fill colour
        button.setFont(BODY_FONT.deriveFont(Font.BOLD, 14f));
        button.setOpaque(true);                                            // Paint the fill colour
        button.setFocusPainted(false);                                     // No focus outline after clicking
        button.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));   // Inner padding: top, left, bottom, right
        button.putClientProperty("JButton.buttonType", "roundRect");       // FlatLaf: rounded corners
        button.setRolloverEnabled(true);                                   // Change look when hovered
        // Show the pointing-hand cursor over the button.
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    // Styles a less important button: white text on a dark fill with a thin outline.
    static void styleSecondaryButton(JButton button) {
        button.setForeground(FOREGROUND);
        button.setBackground(SURFACE);
        button.setFont(BODY_FONT.deriveFont(Font.BOLD, 14f));
        button.setOpaque(true);
        button.setFocusPainted(false);
        // A compound border = an outline around the outside plus padding inside it.
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setRolloverEnabled(true);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    // Styles a text box: white text on a dark grey fill, with an outline and padding.
    static void styleInput(JTextField field) {
        field.setForeground(FOREGROUND);
        field.setBackground(INPUT);
        field.setCaretColor(FOREGROUND);    // The blinking typing cursor is white too
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }
}
