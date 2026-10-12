// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import java.awt.BasicStroke;                // Line thickness for drawing
import java.awt.Color;                      // An RGB colour
import java.awt.Graphics2D;                 // The drawing "pen"
import java.awt.Point;                      // An (x, y) position
import java.util.function.IntFunction;      // An object holding a function: int in, something out

/**
 * Vertical columns, one per category or day (for example "tickets sold per day").
 *
 * OOP idea: inheritance and overriding. ColumnChart extends {@link ChartPanel} and fills in
 * the abstract methods with column-specific drawing and hit-testing.
 */
class ColumnChart extends ChartPanel {
    // Empty space (in pixels) around the plot area, for the axis labels.
    private static final int LEFT = 38;     // Room for the y-axis numbers
    private static final int RIGHT = 8;
    private static final int TOP = 20;      // Room for the value label above the tallest column
    private static final int BOTTOM = 24;   // Room for the x-axis labels

    private final double[] values;                // Height of each column
    private final String[] labels;                // Label under each column, e.g. "3 Oct"
    private final IntFunction<String> tooltip;    // Builds the hover text for column i
    private final Color color;                    // Column colour
    private final int labelEvery;                 // Show every Nth x-axis label
    private final boolean labelEveryColumn;       // Print a value on every column, or just the highest

    // labelEvery = show every Nth x-axis label (useful for 30 days of data).
    // labelEveryColumn = print the value on every column cap (only for a handful of columns);
    // otherwise only the highest column gets a value label.
    ColumnChart(double[] values, String[] labels, IntFunction<String> tooltip, Color color,
            int labelEvery, boolean labelEveryColumn, int height) {
        super(height);                           // ChartPanel sets the size and hover tracking
        this.values = values.clone();            // clone() keeps our own copy, safe from outside changes
        this.labels = labels.clone();
        this.tooltip = tooltip;
        this.color = color;
        this.labelEvery = Math.max(1, labelEvery);   // At least 1, to avoid dividing by zero later
        this.labelEveryColumn = labelEveryColumn;
    }

    // Draws the chart: gridlines and y-axis numbers, then each column, then the baseline.
    @Override
    protected void paintChart(Graphics2D g, int width, int height) {
        // Find the biggest value and which column holds it (the "peak").
        double max = 0;
        int peak = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
                peak = i;
            }
        }
        double axisMax = niceMax(max);           // Round the top of the axis up to a clean number
        int plotBottom = height - BOTTOM;        // y-position of the baseline
        int plotHeight = plotBottom - TOP;       // Height available for columns

        // Recessive gridlines and y-axis ticks.
        // 5 lines: at 0%, 25%, 50%, 75% and 100% of axisMax.
        g.setFont(AXIS_FONT);
        for (int tick = 0; tick <= 4; tick++) {
            double value = axisMax * tick / 4;
            // Screen y grows DOWNWARDS, so bigger values are drawn higher (smaller y).
            int y = (int) Math.round(plotBottom - plotHeight * value / axisMax);
            g.setColor(GRID);
            g.drawLine(LEFT, y, width - RIGHT, y);               // Faint horizontal line
            g.setColor(MUTED);
            drawRight(g, compact(value), LEFT - 6, y + 4);       // Its number, to the left
        }

        double slot = slotWidth(width);                          // Width given to each column
        double barWidth = Math.max(2, Math.min(24, slot - 2));   // Bar is 2-24px wide, leaving a gap
        for (int i = 0; i < values.length; i++) {
            double center = LEFT + slot * i + slot / 2;          // Middle of this column's slot
            double top = plotBottom - plotHeight * values[i] / axisMax;   // Top of this bar
            // Hovered column: draw a faint highlight behind the whole slot.
            if (i == hoverIndex) {
                g.setColor(new Color(0x2A, 0x30, 0x44, 120));
                g.fillRect((int) (center - slot / 2), TOP, (int) Math.ceil(slot), plotHeight);
            }
            // While hovering, the other columns are drawn darker so the hovered one stands out.
            g.setColor(hoverIndex < 0 || i == hoverIndex ? color : color.darker());
            fillVerticalBar(g, center - barWidth / 2, top, barWidth, plotBottom);

            // x-axis label under the column (only some of them, to avoid crowding).
            g.setColor(MUTED);
            g.setFont(AXIS_FONT);
            if (showAxisLabel(i, values.length, labelEvery)) {
                drawAxisLabel(g, labels[i], (float) center, height - 6, width);
            }
            // Value label above the column (all columns, or only the peak).
            if (values[i] > 0 && (labelEveryColumn || i == peak)) {
                g.setColor(TEXT);
                g.setFont(VALUE_FONT);
                drawCentered(g, compact(values[i]), (float) center, (float) top - 5);
            }
        }
        // Baseline drawn last so columns sit on it.
        g.setColor(MUTED);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(LEFT, plotBottom, width - RIGHT, plotBottom);
    }

    // Splits the plot width equally between the columns.
    private double slotWidth(int width) {
        return values.length == 0 ? 0 : (double) (width - LEFT - RIGHT) / values.length;
    }

    // The whole column slot (not just the thin bar) counts as the hover target.
    @Override
    protected int indexAt(Point point) {
        double slot = slotWidth(getWidth());
        // Outside the plot area: no column.
        if (slot <= 0 || point.x < LEFT || point.x > getWidth() - RIGHT
                || point.y < TOP || point.y > getHeight() - BOTTOM) {
            return -1;
        }
        // Which slot is the mouse in? (Math.min guards against the very right edge.)
        return Math.min(values.length - 1, (int) ((point.x - LEFT) / slot));
    }

    // Asks the tooltip function for column `index`'s text.
    @Override
    protected String tooltipFor(int index) {
        return tooltip.apply(index);
    }
}
