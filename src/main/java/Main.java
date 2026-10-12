// Main.java has no "package" line, so it lives in the default (top-level) package.
import views.MainFrame;                     // The app's main window
import com.formdev.flatlaf.FlatDarkLaf;     // The FlatLaf dark theme (an outside library listed in pom.xml)
import java.awt.Font;                       // A font (family, style, size)
import javax.swing.JFrame;                  // A desktop window
import javax.swing.SwingUtilities;          // Helpers for running code on Swing's UI thread
import javax.swing.UIManager;               // Global Swing settings such as the theme and default fonts

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
    // args holds any words typed after the program name on the command line (not used here).
    public static void main(String[] args) {
        // Polymorphism: setLookAndFeel accepts any LookAndFeel. FlatDarkLaf is one
        // subclass of LookAndFeel, so we can pass it in.
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Throwable ignored) {
            // Fall back to the platform Swing theme if FlatLaf is unavailable.
        }
        // One font for the whole app: Ubuntu Sans, normal weight, size 14.
        Font bodyFont = new Font("Ubuntu Sans", Font.PLAIN, 14);
        UIManager.put("defaultFont", bodyFont);
        // Also set it on each kind of component, in case the theme has its own default for them.
        for (String key : new String[] {"Label.font", "Button.font", "TextField.font", "PasswordField.font",
                "ComboBox.font", "List.font", "Table.font", "MenuItem.font", "OptionPane.messageFont"}) {
            UIManager.put(key, bodyFont);
        }
        // Swing screens must be built on the UI thread, so we hand the work to invokeLater.
        // "() -> { ... }" is a lambda: a block of code passed in to run later.
        SwingUtilities.invokeLater(() -> {
            // Object creation: "new MainFrame()" builds one window object from the MainFrame class.
            MainFrame app;
            try {
                app = new MainFrame();
            } catch (database.DatabaseException exception) {
                // Can't reach the database (missing db.properties, wrong password, server off...):
                // explain what went wrong in a pop-up, then close the program.
                javax.swing.JOptionPane.showMessageDialog(null, exception.getMessage(),
                        "SakaTickets - database problem", javax.swing.JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }
            app.setVisible(true);   // Show the window
            app.validate();         // Work out the layout of everything inside it
            app.repaint();          // Draw it
            // Once the window is showing, maximise it to fill the screen, then lay out and draw again.
            SwingUtilities.invokeLater(() -> {
                app.setExtendedState(JFrame.MAXIMIZED_BOTH);
                app.validate();
                app.repaint();
            });
        });
    }
}
