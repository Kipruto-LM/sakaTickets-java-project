import views.MainFrame;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import views.ThemeManager;

/** Starts SakaTickets after loading saved theme preferences and configuring shared fonts. */
public class Main {
    /** Initializes appearance defaults, constructs the frame, and maximizes after first paint. */
    public static void main(String[] args) {
        // Install the saved FlatLaf theme before Swing constructs any components.
        ThemeManager.initialize();
        // Create and show Swing windows on the event-dispatch thread.
        SwingUtilities.invokeLater(() -> {
            MainFrame app = new MainFrame();
            app.setVisible(true);
            app.validate();
            app.repaint();
            SwingUtilities.invokeLater(() -> {
                // Maximize after the first visible paint so window managers calculate bounds correctly.
                app.setExtendedState(JFrame.MAXIMIZED_BOTH);
                app.validate();
                app.repaint();
            });
        });
    }
}