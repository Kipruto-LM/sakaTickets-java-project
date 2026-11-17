import views.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;
import java.sql.SQLException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Application entry point. Set up the look and feel first, then create Swing
 * components on the Event Dispatch Thread.
 *
 * Core concepts:
 * - Static method: main() is the JVM entry point and does not need a Main object.
 * - Event-driven programming: SwingUtilities schedules UI work on Swing's UI thread.
 * - Exception handling: a database startup error is shown instead of being hidden.
 */
public class Main {

    /** Configures Swing and starts the window while reporting startup database errors. */
    public static void main(String[] args) {
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame app = new MainFrame();
                app.setVisible(true);

                // Let Swing paint the initial layout before the window manager resizes it.
                app.validate();
                app.repaint();
                SwingUtilities.invokeLater(() -> {
                    app.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    app.validate();
                    app.repaint();
                });
            } catch (SQLException exception) {
                JOptionPane.showMessageDialog(null, "Could not open the SQLite database: "
                        + exception.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}