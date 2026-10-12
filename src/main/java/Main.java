import views.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point of the SakaTickets application.
 *
 * OOP idea: this class has no fields and creates no Main objects. Its only job is to
 * set up the look of the app and create the first real object, a {@link MainFrame}.
 * From that point on, everything happens through objects talking to each other.
 */
public class Main {
    // "static" means this method belongs to the class itself, not to an object,
    // so Java can call it before any objects exist.
    public static void main(String[] args) {
        // Polymorphism: setLookAndFeel accepts any LookAndFeel. FlatDarkLaf is one
        // subclass of LookAndFeel, so we can pass it in.
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Throwable ignored) {
            // Fall back to the platform Swing theme if FlatLaf is unavailable.
        }
        Font bodyFont = new Font("Ubuntu Sans", Font.PLAIN, 14);
        UIManager.put("defaultFont", bodyFont);
        for (String key : new String[] {"Label.font", "Button.font", "TextField.font", "PasswordField.font",
                "ComboBox.font", "List.font", "Table.font", "MenuItem.font", "OptionPane.messageFont"}) {
            UIManager.put(key, bodyFont);
        }
        // Swing screens must be built on the UI thread, so we hand the work to invokeLater.
        SwingUtilities.invokeLater(() -> {
            // Object creation: "new MainFrame()" builds one window object from the MainFrame class.
            MainFrame app = new MainFrame();
            app.setVisible(true);
            app.validate();
            app.repaint();
            SwingUtilities.invokeLater(() -> {
                app.setExtendedState(JFrame.MAXIMIZED_BOTH);
                app.validate();
                app.repaint();
            });
        });
    }
}