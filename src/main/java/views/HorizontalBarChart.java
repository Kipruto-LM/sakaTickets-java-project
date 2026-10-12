// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import java.awt.Color;                       // An RGB colour
import java.awt.Graphics2D;                  // The drawing "pen"

/**
 * Horizontal bars, one row per item (for example one row per event or ticket tier).
 * If "maximums" are given, each bar sits on a faint track showing how full it could be,
 * like a progress bar (e.g. seats sold out of capacity).
 *
 * OOP idea: the second {@link ChartPanel} subclass. It overrides paintChart() differently from
 * ColumnChart, but both can be used anywhere a ChartPanel is expected (polymorphism).
 */
class HorizontalBarChart extends ChartPanel {
    private static final int ROW_HEIGHT = 40;     // Height of each row
    private static final int THICKNESS = 14;      // Thickness of each bar
    private static final int LABEL_WIDTH = 170;   // Space on the left for the row names
    private static final int VALUE_WIDTH = 110;   // Space on the right for the numbers

    private final String[] labels;      // Row names
    private final double[] values;      // Bar lengths
    private final double[] maximums;    // How long each bar could be (or null: compare to the biggest value)
    private final String prefix;        // Text before each number, e.g. "KES "
    private final String suffix;        // Text after each number, e.g. " sold"
    private final Color oneColor;       // One colour for every bar, or null for a different colour per row

    // Constructor: the chart's height is one row per label.
    HorizontalBarChart(String[] labels, double[] values, double[] maximums, String prefix, String suffix,
            Color oneColor) {
        super(labels.length * ROW_HEIGHT + 8);
        this.labels = labels;
        this.values = values;
        this.maximums = maximums;
        this.prefix = prefix;
        this.suffix = suffix;
        this.oneColor = oneColor;
    }

    // Draws each row: name, track (if any), bar, number.
    @Override
    void paintChart(Graphics2D g, int width, int height) {
        // The biggest value, used as the scale when there are no maximums.
        double biggest = 0;
        for (double value : values) {
            biggest = Math.max(biggest, value);
        }
        int barLeft = LABEL_WIDTH;                    // Where bars start
        int fullLength = width - LABEL_WIDTH - VALUE_WIDTH;   // Length of a 100% bar

        for (int i = 0; i < labels.length; i++) {
            int y = 4 + i * ROW_HEIGHT + (ROW_HEIGHT - THICKNESS) / 2;   // Top of this row's bar
            // Pick the colour: the single colour, or the next one from the SERIES list.
            Color color = oneColor;
            if (color == null) {
                color = SERIES[i % SERIES.length];    // % wraps back to the start after 4 rows
            }

            // Row name on the left.
            g.setColor(TEXT);
            g.setFont(ViewStyles.BODY_FONT.deriveFont(12f));
            g.drawString(labels[i], 0, y + 11);

            // How full this bar is, from 0.0 to 1.0.
            double max = biggest;
            if (maximums != null) {
                max = maximums[i];
                // Faint track showing the full length (the colour with low opacity: 45 out of 255).
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
                g.fillRect(barLeft, y, fullLength, THICKNESS);
            }
            double fraction = 0;
            if (max > 0) {
                fraction = Math.min(1, values[i] / max);
            }

            // The bar itself.
            g.setColor(color);
            g.fillRect(barLeft, y, (int) (fullLength * fraction), THICKNESS);

            // The number after the bar, e.g. "KES 146.7K" or "24 sold".
            g.setColor(TEXT);
            g.setFont(VALUE_FONT);
            g.drawString(prefix + compact(values[i]) + suffix, barLeft + fullLength + 10, y + 11);
        }
    }
}
