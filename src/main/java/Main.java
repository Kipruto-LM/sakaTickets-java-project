import views.MainFrame;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import views.ThemeManager;

/**
 * Application entry point. It applies the saved theme, then asks Swing to build
 * the interface on its Event Dispatch Thread (the thread Swing uses for UI work).
 */
public class Main {
    
    /** 
     * Bootstraps the application.
     * 
     * @param args Command line arguments (unused).
     */
    public static void main(String[] args) {
        
        // 1. Initialize the UI styling before any Swing components are instantiated
        // This ensures the Dark/Light mode preferences are applied globally.
        ThemeManager.initialize();
        
        // 2. Schedule the GUI creation on the Event-Dispatch Thread (EDT).
        // This is a strict requirement for Java Swing thread safety.
        SwingUtilities.invokeLater(() -> {
            
            // Construct the main container frame and its CardLayout child panels
            MainFrame app = new MainFrame();
            
            // Force the window to paint its initial layout bounds
            app.setVisible(true);
            app.validate();
            app.repaint();
            
            // 3. Queue a secondary task on the EDT to maximize the window.
            // Doing this *after* the first paint prevents clipping/rendering bugs 
            // commonly caused by OS window managers (especially in WSL/Linux environments).
            SwingUtilities.invokeLater(() -> {
                app.setExtendedState(JFrame.MAXIMIZED_BOTH);
                app.validate();
                app.repaint();
            });
        });
    }
}