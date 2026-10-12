// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import java.awt.Color;                       // An RGB colour
import java.awt.Graphics2D;                  // The drawing "pen"
import java.awt.Point;                       // An (x, y) position
import java.util.function.DoubleFunction;    // An object holding a function: double in, something out
import java.util.function.IntFunction;       // An object holding a function: int in, something out

/**
 * Horizontal bars, one row per category (for example one row per ticket tier).
 * When row maximums are given, each bar sits on a track showing the full capacity,
 * so it doubles as a progress meter.
 *
 * OOP idea: a third {@link ChartPanel} subclass. The screens can use ColumnChart, LineChart
 * or HorizontalBarChart interchangeably as "a chart" (polymorphism).
 */
class HorizontalBarChart extends ChartPanel {
    private static final int VALUE_WIDTH = 104;   // Space kept on the right for value text
    private static final int ROW_HEIGHT = 40;     // Height of each row
    private static final int THICKNESS = 14;      // Thickness of each bar

    private final String[] labels;                // Row names, e.g. tier names
    private final double[] values;                // Bar lengths
    // Optional: the most each row could reach (capacity). null means bars share one scale.
    private final double[] maximums;
    private final DoubleFunction<String> valueText;   // Turns a value into text, e.g. 25200 -> "KES 25.2K"
    private final IntFunction<String> tooltip;        // Builds the hover text for row i
    // When set, every bar uses this one colour (rows are the same kind of thing, e.g. events).
    // When null, each row gets the next categorical colour (rows are different kinds, e.g. tiers).
    private final Color singleColor;

    // Constructor without a single colour: passes null to the full constructor below.
    HorizontalBarChart(String[] labels, double[] values, double[] maximums, DoubleFunction<String> valueText,
            IntFunction<String> tooltip) {
        this(labels, values, maximums, valueText, tooltip, null);
    }

    // Overloaded constructor that also takes a single bar colour.
    HorizontalBarChart(String[] labels, double[] values, double[] maximums, DoubleFunction<String> valueText,
            IntFunction<String> tooltip, Color singleColor) {
        // Chart height = one row per label (at least one row) plus a little padding.
        super(Math.max(1, labels.length) * ROW_HEIGHT + 8);
        this.singleColor = singleColor;
        this.labels = labels.clone();
        this.values = values.clone();
        this.maximums = maximums == null ? null : maximums.clone();
        this.valueText = valueText;
        this.tooltip = tooltip;
    }

    // Draws each row: colour dot + label, optional track, the bar, then the value text.
    @Override
    protected void paintChart(Graphics2D g, int width, int height) {
        // The biggest value, used as the shared scale when there are no maximums.
        double sharedMax = 0;
        for (double value : values) sharedMax = Math.max(sharedMax, value);
        // The label column grows to fit the longest label, up to 40% of the chart width.
        g.setFont(ViewStyles.BODY_FONT.deriveFont(12f));
        int keyWidth = singleColor == null ? 14 : 0;          // Room for the colour dot, if there is one
        int widest = 0;
        for (String label : labels) widest = Math.max(widest, g.getFontMetrics().stringWidth(label));
        int barLeft = Math.max(70, Math.min((int) (width * 0.4), widest + keyWidth + 14));   // Where bars start
        int barRight = width - VALUE_WIDTH;                   // Where a full bar ends
        for (int i = 0; i < labels.length; i++) {
            int rowTop = 4 + i * ROW_HEIGHT;                  // Top of this row
            int barY = rowTop + (ROW_HEIGHT - THICKNESS) / 2; // Bar is centred in its row
            // Either the one shared colour, or the next colour from SERIES (% wraps around after 4).
            Color color = singleColor != null ? singleColor : SERIES[i % SERIES.length];
            // Hovered row: faint highlight behind the whole row.
            if (i == hoverIndex) {
                g.setColor(new Color(0x2A, 0x30, 0x44, 120));
                g.fillRect(0, rowTop, width, ROW_HEIGHT);
            }
            // Colour key dot beside the label, so the text itself stays in text colours.
            int textLeft = 0;
            if (singleColor == null) {
                g.setColor(color);
                g.fillOval(0, barY + 3, 8, 8);
                textLeft = 14;
            }
            // The row label, shortened with "..." if it's too long.
            g.setColor(TEXT);
            g.setFont(ViewStyles.BODY_FONT.deriveFont(12f));
            g.drawString(fitText(g, labels[i], barLeft - textLeft - 10), textLeft, barY + 11);

            // How full this bar is, from 0.0 to 1.0.
            double max = maximums == null ? sharedMax : maximums[i];
            double fraction = max <= 0 ? 0 : Math.min(1, values[i] / max);
            if (maximums != null) {
                // Track: the unfilled part of the meter, a dark step of the same hue.
                // Alpha 45 out of 255 makes it mostly see-through.
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
                g.fillRoundRect(barLeft, barY, barRight - barLeft, THICKNESS, 8, 8);
            }
            // The bar itself.
            g.setColor(color);
            fillHorizontalBar(g, barLeft, barY, barLeft + (barRight - barLeft) * fraction, THICKNESS);

            // Value at the bar's tip; meters keep it in a fixed column after the full track.
            g.setColor(TEXT);
            g.setFont(VALUE_FONT);
            int valueX = maximums == null ? (int) (barLeft + (barRight - barLeft) * fraction) + 8 : barRight + 10;
            g.drawString(valueText.apply(values[i]), valueX, barY + 11);
            // Meters also show the percentage at the far right.
            if (maximums != null) {
                g.setColor(MUTED);
                g.setFont(AXIS_FONT);
                drawRight(g, Math.round(fraction * 100) + "%", width, barY + 11);
            }
        }
    }

    // Shortens a label with "..." so it never runs into the bar.
    private static String fitText(Graphics2D g, String text, int maxWidth) {
        if (g.getFontMetrics().stringWidth(text) <= maxWidth) return text;   // Already fits
        String shortened = text;
        // Remove one character at a time until "text..." fits.
        while (shortened.length() > 1 && g.getFontMetrics().stringWidth(shortened + "...") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "...";
    }

    // The whole row is the hover target, not just the thin bar.
    @Override
    protected int indexAt(Point point) {
        int index = (point.y - 4) / ROW_HEIGHT;                               // Which row the mouse is in
        return point.y < 4 || index >= labels.length ? -1 : index;           // -1 if above or below all rows
    }

    // Asks the tooltip function for row `index`'s text.
    @Override
    protected String tooltipFor(int index) {
        return tooltip.apply(index);
    }
}
