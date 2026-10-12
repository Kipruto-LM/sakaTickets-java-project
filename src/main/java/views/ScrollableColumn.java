// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import java.awt.Dimension;       // A width and height
import java.awt.Rectangle;       // A position plus a width and height
import javax.swing.JPanel;       // A container for other components
import javax.swing.Scrollable;   // The interface JScrollPane uses to ask a panel how to scroll

/**
 * A panel that always matches the scroll area's width, so the cards inside stretch to fill
 * the page and the page only scrolls up and down.
 *
 * Used by the dashboard overview, the event insights page and the event details page.
 *
 * OOP idea: implementing an interface. By implementing Scrollable, this class promises to
 * provide the five methods JScrollPane asks for, and JScrollPane can then treat it specially.
 * "extends JPanel implements Scrollable" = it IS a panel AND it can do what Scrollable promises.
 */
class ScrollableColumn extends JPanel implements Scrollable {
    // How big the visible area would like to be: just the panel's own preferred size.
    @Override
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

    // How far one click on the scroll arrow (or one mouse-wheel step) moves: 18 pixels.
    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 18; }

    // How far one Page Up / Page Down moves: almost a full screen, keeping 40px of overlap.
    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return visible.height - 40;
    }

    // true = always be exactly as wide as the visible area, so there's no sideways scrolling.
    @Override
    public boolean getScrollableTracksViewportWidth() { return true; }

    // false = can be taller than the visible area, so up/down scrolling works.
    @Override
    public boolean getScrollableTracksViewportHeight() { return false; }
}
