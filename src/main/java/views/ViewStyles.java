package views;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

final class ViewStyles {
    static final Color BACKGROUND = new Color(0x12, 0x12, 0x12);
    static final Color SURFACE = new Color(0x1E, 0x1E, 0x1E);
    static final Color INPUT = new Color(0x2A, 0x2A, 0x2A);
    static final Color ACCENT = new Color(0x0D, 0x6E, 0xFD);
    static final Color FOREGROUND = Color.WHITE;
    static final Color MUTED = new Color(0xA0, 0xA0, 0xA0);
    static final Color OUTLINE = new Color(0x38, 0x38, 0x38);

    private ViewStyles() {
    }

    static void styleHeading(JLabel label, float size) {
        label.setForeground(FOREGROUND);
        label.setFont(label.getFont().deriveFont(Font.BOLD, size));
    }

    static void styleSecondaryText(JLabel label, float size) {
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, size));
    }

    static void stylePrimaryButton(JButton button) {
        button.setForeground(FOREGROUND);
        button.setBackground(ACCENT);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    static void styleSecondaryButton(JButton button) {
        button.setForeground(FOREGROUND);
        button.setBackground(SURFACE);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    static void styleInput(JTextField field) {
        field.setForeground(FOREGROUND);
        field.setBackground(INPUT);
        field.setCaretColor(FOREGROUND);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }
}
