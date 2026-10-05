package views;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

/** Stores appearance preferences and applies the matching FlatLaf and SakaTickets palette. */
public final class ThemeManager {
    // Java Preferences keeps appearance settings local to this computer/account.
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(ThemeManager.class);
    private static boolean darkMode = true;
    private static Color accentColor = new Color(0x00, 0xB4, 0xFF);

    private ThemeManager() {
    }

    /** Loads saved settings and applies the matching FlatLaf before UI construction. */
    public static void initialize() {
        darkMode = PREFERENCES.getBoolean("darkMode", true);
        try {
            accentColor = Color.decode(PREFERENCES.get("accentColor", "#00B4FF"));
        } catch (NumberFormatException exception) {
            accentColor = new Color(0x00, 0xB4, 0xFF);
        }
        ViewStyles.setThemeColors(darkMode, accentColor);
        applyLookAndFeel();
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static Color getAccentColor() {
        return accentColor;
    }

    /** Persists the selected mode and changes FlatLaf for newly constructed components. */
    public static void setDarkMode(boolean dark) {
        darkMode = dark;
        PREFERENCES.putBoolean("darkMode", dark);
        ViewStyles.setThemeColors(darkMode, accentColor);
        applyLookAndFeel();
    }

    /** Persists the global accent used by primary actions and active navigation. */
    public static void setAccentColor(Color color) {
        if (color == null) return;
        accentColor = color;
        PREFERENCES.put("accentColor", String.format("#%06X", color.getRGB() & 0xFFFFFF));
        ViewStyles.setThemeColors(darkMode, accentColor);
    }

    /** Recursively maps custom component colors and borders between dark and light palettes. */
    public static void applyPalette(Component component) {
        if (component instanceof JComponent swingComponent) {
            Color background = swingComponent.getBackground();
            Color foreground = swingComponent.getForeground();
            if (background != null) swingComponent.setBackground(mapBackground(background));
            if (foreground != null) swingComponent.setForeground(mapForeground(foreground));
            Border border = swingComponent.getBorder();
            if (border != null) swingComponent.setBorder(mapBorder(border));
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) applyPalette(child);
        }
    }

    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(darkMode ? new FlatDarkLaf() : new FlatLightLaf());
            applyGlobalFonts();
        } catch (Exception exception) {
            System.err.println("Could not apply selected theme: " + exception.getMessage());
        }
    }

    private static Color mapBackground(Color color) {
        int rgb = color.getRGB() & 0xFFFFFF;
        int mapped = darkMode ? backgroundToDark(rgb) : backgroundToLight(rgb);
        return mapped == rgb ? color : new Color(mapped | (color.getAlpha() << 24), true);
    }

    private static Color mapForeground(Color color) {
        int rgb = color.getRGB() & 0xFFFFFF;
        int mapped = darkMode ? foregroundToDark(rgb) : foregroundToLight(rgb);
        return mapped == rgb ? color : new Color(mapped | (color.getAlpha() << 24), true);
    }

    // Separate role maps prevent a white surface from being mistaken for white text.
    private static int backgroundToLight(int rgb) {
        return switch (rgb) {
            case 0x0F1117, 0x121212 -> 0xF4F7FB;
            case 0x171B24, 0x1E1E1E -> 0xFFFFFF;
            case 0x101520, 0x2A2A2A -> 0xF8FAFD;
            case 0x2A3044, 0x383838 -> 0xD2DAE6;
            default -> rgb;
        };
    }

    private static int backgroundToDark(int rgb) {
        return switch (rgb) {
            case 0xF4F7FB, 0xE8EDF4 -> 0x0F1117;
            case 0xFFFFFF -> 0x171B24;
            case 0xF8FAFD, 0xD2DAE6 -> 0x2A3044;
            default -> rgb;
        };
    }

    private static int foregroundToLight(int rgb) {
        return switch (rgb) {
            case 0xFFFFFF, 0xF0F4FF -> 0x172033;
            case 0x8896B3, 0xA0A0A0, 0x6F7E99 -> 0x56657C;
            default -> rgb;
        };
    }

    private static int foregroundToDark(int rgb) {
        return switch (rgb) {
            case 0x172033 -> 0xF0F4FF;
            case 0x56657C -> 0x8896B3;
            default -> rgb;
        };
    }

    private static Border mapBorder(Border border) {
        if (border instanceof CompoundBorder compound) {
            return BorderFactory.createCompoundBorder(mapBorder(compound.getOutsideBorder()),
                    mapBorder(compound.getInsideBorder()));
        }
        if (border instanceof LineBorder line) {
            return new LineBorder(mapBorderColor(line.getLineColor()), line.getThickness(), line.getRoundedCorners());
        }
        if (border instanceof MatteBorder matte && matte.getMatteColor() != null) {
            return new MatteBorder(matte.getBorderInsets(), mapBorderColor(matte.getMatteColor()));
        }
        return border;
    }

    private static Color mapBorderColor(Color color) {
        int rgb = color.getRGB() & 0xFFFFFF;
        int mapped = darkMode ? backgroundToDark(rgb) : backgroundToLight(rgb);
        return mapped == rgb ? color : new Color(mapped | (color.getAlpha() << 24), true);
    }

    /** Restores the application font role after each FlatLaf change. */
    private static void applyGlobalFonts() {
        Font bodyFont = new Font("Ubuntu Sans", Font.PLAIN, 14);
        UIManager.put("defaultFont", bodyFont);
        for (String key : new String[] {"Label.font", "Button.font", "TextField.font", "PasswordField.font",
                "ComboBox.font", "List.font", "Table.font", "MenuItem.font", "OptionPane.messageFont"}) {
            UIManager.put(key, bodyFont);
        }
    }
}
