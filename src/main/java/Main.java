// Main.java has no "package" line, so it lives in the default (top-level) package.
import views.MainFrame;                     // The app's main window
import com.formdev.flatlaf.FlatDarkLaf;     // The FlatLaf dark theme (an outside library listed in pom.xml)
import java.awt.Font;                       // A font (family, style, size)
import javax.swing.JFrame;                  // A desktop window
import javax.swing.JOptionPane;             // Pop-up message boxes
import javax.swing.UIManager;               // Global Swing settings such as the default font

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
        FlatDarkLaf.setup();                                              // Use the dark theme
        UIManager.put("defaultFont", new Font("Ubuntu Sans", Font.PLAIN, 14));   // One font for the whole app

        try {
            // Object creation: "new MainFrame()" builds the window (and connects to the database).
            MainFrame app = new MainFrame();
            app.setExtendedState(JFrame.MAXIMIZED_BOTH);                  // Fill the screen
            app.setVisible(true);                                         // Show the window
        } catch (RuntimeException exception) {
            // Can't reach the database (missing db.properties, wrong password, server off...):
            // explain what went wrong in a pop-up, then close the program.
            JOptionPane.showMessageDialog(null, exception.getMessage(),
                    "SakaTickets - database problem", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
