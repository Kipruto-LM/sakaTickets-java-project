import views.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Throwable ignored) {
            // Fall back to the platform Swing theme if FlatLaf is unavailable.
        }
        SwingUtilities.invokeLater(() -> {
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