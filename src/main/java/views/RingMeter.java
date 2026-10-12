// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import java.awt.BasicStroke;     // Line thickness and end style for drawing
import java.awt.Color;           // An RGB colour
import java.awt.Font;            // Font style constants
import java.awt.Graphics2D;      // The drawing "pen"
import java.awt.Point;           // An (x, y) position, e.g. of the mouse
import java.awt.geom.Arc2D;      // Part (or all) of a circle's outline

/**
 * A ring that fills up to show one percentage, such as "72% of seats sold".
 *
 * OOP idea: the smallest {@link ChartPanel} subclass. It has only one value, so its
 * indexAt() just answers "is the mouse over the ring?".
 */
class RingMeter extends ChartPanel {
    private static final Color TRACK = new Color(0x10, 0x42, 0x81);   // Dark blue: the empty part of the ring
    private static final Color FILL = SERIES[0];                       // Bright blue: the filled part (from ChartPanel)

    private final double fraction;       // How full the ring is, from 0.0 (empty) to 1.0 (full)
    private final String caption;        // Small text under the percentage, e.g. "of seats sold"
    private final String tooltipText;    // Text shown when hovering over the ring

    // Constructor. size = the ring's height in pixels.
    RingMeter(double fraction, String caption, String tooltipText, int size) {
        super(size);                                          // Run ChartPanel's constructor first
        this.fraction = Math.max(0, Math.min(1, fraction));   // Keep the value between 0 and 1
        this.caption = caption;
        this.tooltipText = tooltipText;
    }

    // Draws the ring. ChartPanel calls this every time the chart needs painting.
    @Override
    protected void paintChart(Graphics2D g, int width, int height) {
        // Make the circle fit the smaller side, leaving 12px of room all round.
        int diameter = Math.min(width, height) - 24;
        // Top-left corner of the square that holds the circle, so the circle is centred.
        double x = (width - diameter) / 2.0;
        double y = (height - diameter) / 2.0;
        // 1. The full dark track: a 12px-thick circle outline (0 to 360 degrees).
        g.setStroke(new BasicStroke(12f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(TRACK);
        g.draw(new Arc2D.Double(x, y, diameter, diameter, 0, 360, Arc2D.OPEN));
        // 2. The bright part on top, with rounded ends (CAP_ROUND).
        g.setColor(FILL);
        g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (fraction > 0) {
            // Starts at 12 o'clock and runs clockwise (negative angles in Java2D).
            g.draw(new Arc2D.Double(x, y, diameter, diameter, 90, -360 * fraction, Arc2D.OPEN));
        }
        // 3. The big percentage in the middle. Its font grows with the ring (never below 18pt).
        g.setColor(TEXT);
        g.setFont(ViewStyles.BODY_FONT.deriveFont(Font.BOLD, Math.max(18f, diameter / 4.2f)));
        drawCentered(g, Math.round(fraction * 100) + "%", width / 2f, height / 2f + 6);
        // 4. The small caption under it.
        g.setColor(MUTED);
        g.setFont(AXIS_FONT);
        drawCentered(g, caption, width / 2f, height / 2f + 24);
    }

    // Returns 0 when the mouse is over the ring, or -1 when it isn't.
    // Uses Pythagoras: a point is inside a circle if dx² + dy² <= radius².
    @Override
    protected int indexAt(Point point) {
        double dx = point.x - getWidth() / 2.0;      // Horizontal distance from the centre
        double dy = point.y - getHeight() / 2.0;     // Vertical distance from the centre
        double radius = (Math.min(getWidth(), getHeight()) - 24) / 2.0 + 8;   // Ring radius plus a little extra
        return dx * dx + dy * dy <= radius * radius ? 0 : -1;
    }

    // The tooltip is always the same text, whatever the index.
    @Override
    protected String tooltipFor(int index) {
        return tooltipText;
    }
}
