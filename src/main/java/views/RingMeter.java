package views;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Arc2D;

/**
 * A ring that fills up to show one percentage, such as "72% of seats sold".
 *
 * OOP idea: the smallest {@link ChartPanel} subclass. It has only one value, so its
 * indexAt() just answers "is the mouse over the ring?".
 */
class RingMeter extends ChartPanel {
    private static final Color TRACK = new Color(0x10, 0x42, 0x81);
    private static final Color FILL = SERIES[0];

    private final double fraction;
    private final String caption;
    private final String tooltipText;

    RingMeter(double fraction, String caption, String tooltipText, int size) {
        super(size);
        this.fraction = Math.max(0, Math.min(1, fraction));
        this.caption = caption;
        this.tooltipText = tooltipText;
    }

    @Override
    protected void paintChart(Graphics2D g, int width, int height) {
        int diameter = Math.min(width, height) - 24;
        double x = (width - diameter) / 2.0;
        double y = (height - diameter) / 2.0;
        g.setStroke(new BasicStroke(12f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(TRACK);
        g.draw(new Arc2D.Double(x, y, diameter, diameter, 0, 360, Arc2D.OPEN));
        g.setColor(FILL);
        g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (fraction > 0) {
            // Starts at 12 o'clock and runs clockwise (negative angles in Java2D).
            g.draw(new Arc2D.Double(x, y, diameter, diameter, 90, -360 * fraction, Arc2D.OPEN));
        }
        g.setColor(TEXT);
        g.setFont(ViewStyles.BODY_FONT.deriveFont(Font.BOLD, Math.max(18f, diameter / 4.2f)));
        drawCentered(g, Math.round(fraction * 100) + "%", width / 2f, height / 2f + 6);
        g.setColor(MUTED);
        g.setFont(AXIS_FONT);
        drawCentered(g, caption, width / 2f, height / 2f + 24);
    }

    @Override
    protected int indexAt(Point point) {
        double dx = point.x - getWidth() / 2.0;
        double dy = point.y - getHeight() / 2.0;
        double radius = (Math.min(getWidth(), getHeight()) - 24) / 2.0 + 8;
        return dx * dx + dy * dy <= radius * radius ? 0 : -1;
    }

    @Override
    protected String tooltipFor(int index) {
        return tooltipText;
    }
}
