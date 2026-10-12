// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import java.awt.Color;                      // An RGB colour
import java.awt.Graphics2D;                 // The drawing "pen"

/**
 * Vertical columns, one per day or category (for example "tickets sold per day").
 *
 * OOP idea: inheritance and overriding. ColumnChart extends {@link ChartPanel} and fills in
 * paintChart() with its own drawing code.
 */
class ColumnChart extends ChartPanel {
    // Empty space (in pixels) around the bars, for the labels.
    private static final int LEFT = 38;     // Room for the numbers on the left
    private static final int RIGHT = 8;
    private static final int TOP = 20;      // Room for the value printed above the tallest bar
    private static final int BOTTOM = 24;   // Room for the labels under the bars

    private final double[] values;            // Height of each column
    private final String[] labels;            // Label under each column, e.g. "3 Oct"
    private final Color color;                // Column colour
    private final int labelEvery;             // Show every Nth label (so 30 dates don't overlap)
    private final boolean valueOnEveryColumn; // Print a number above every column, or only the tallest

    // Constructor: stores what to draw. height = how tall the chart is in pixels.
    ColumnChart(double[] values, String[] labels, Color color, int labelEvery, boolean valueOnEveryColumn, int height) {
        super(height);                        // ChartPanel's constructor sets the size
        this.values = values;
        this.labels = labels;
        this.color = color;
        this.labelEvery = labelEvery;
        this.valueOnEveryColumn = valueOnEveryColumn;
    }

    // Draws the gridlines with their numbers, then each column with its label.
    @Override
    void paintChart(Graphics2D g, int width, int height) {
        // Find the biggest value and which column holds it.
        double max = 0;
        int tallest = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
                tallest = i;
            }
        }
        double axisMax = niceMax(max);              // Top of the scale, a "round" number
        int bottom = height - BOTTOM;               // y-position of the baseline
        int plotHeight = bottom - TOP;              // Space available for the bars

        // Five gridlines: at 0, 1/4, 2/4, 3/4 and the top of the scale.
        g.setFont(AXIS_FONT);
        for (int step = 0; step <= 4; step++) {
            double value = axisMax * step / 4;
            // Screen y grows DOWNWARDS, so bigger values are drawn higher (smaller y).
            int y = bottom - (int) (plotHeight * value / axisMax);
            g.setColor(GRID);
            g.drawLine(LEFT, y, width - RIGHT, y);
            g.setColor(MUTED);
            drawRight(g, compact(value), LEFT - 6, y + 4);
        }

        // The columns. Each one gets an equal "slot" of the width.
        int slot = (width - LEFT - RIGHT) / values.length;
        int barWidth = Math.min(24, slot - 2);                  // At most 24px wide, with a gap
        for (int i = 0; i < values.length; i++) {
            int centre = LEFT + slot * i + slot / 2;            // Middle of this column's slot
            int barHeight = (int) (plotHeight * values[i] / axisMax);
            g.setColor(color);
            g.fillRect(centre - barWidth / 2, bottom - barHeight, barWidth, barHeight);

            // Label under the column (only every Nth one, to avoid crowding).
            if (i % labelEvery == 0) {
                g.setColor(MUTED);
                g.setFont(AXIS_FONT);
                drawCentered(g, labels[i], centre, height - 6);
            }
            // Number above the column (every column, or just the tallest).
            if (values[i] > 0 && (valueOnEveryColumn || i == tallest)) {
                g.setColor(TEXT);
                g.setFont(VALUE_FONT);
                drawCentered(g, compact(values[i]), centre, bottom - barHeight - 5);
            }
        }
    }
}
