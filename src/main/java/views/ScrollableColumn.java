package views;

import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.Scrollable;

/**
 * A panel that always matches the scroll area's width, so the cards inside stretch to fill
 * the page and the page only scrolls up and down.
 *
 * OOP idea: implementing an interface. By implementing Scrollable, this class promises to
 * provide the five methods JScrollPane asks for, and JScrollPane can then treat it specially.
 */
class ScrollableColumn extends JPanel implements Scrollable {
    @Override
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 18; }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return visible.height - 40;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() { return true; }

    @Override
    public boolean getScrollableTracksViewportHeight() { return false; }
}
