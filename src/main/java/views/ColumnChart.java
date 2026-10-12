package views;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.function.IntFunction;

/**
 * Vertical columns, one per category or day (for example "tickets sold per day").
 *
 * OOP idea: inheritance and overriding. ColumnChart extends {@link ChartPanel} and fills in
 * the abstract methods with column-specific drawing and hit-testing.
 */
class ColumnChart extends ChartPanel {
    private static final int LEFT = 38;
    private static final int RIGHT = 8;
    private static final int TOP = 20;
    private static final int BOTTOM = 24;

    private final double[] values;
    private final String[] labels;
    private final IntFunction<String> tooltip;
    private final Color color;
    private final int labelEvery;
    private final boolean labelEveryColumn;

    // labelEvery = show every Nth x-axis label (useful for 30 days of data).
    // labelEveryColumn = print the value on every column cap (only for a handful of columns);
    // otherwise only the highest column gets a value label.
    ColumnChart(double[] values, String[] labels, IntFunction<String> tooltip, Color color,
            int labelEvery, boolean labelEveryColumn, int height) {
        super(height);
        this.values = values.clone();
        this.labels = labels.clone();
        this.tooltip = tooltip;
        this.color = color;
        this.labelEvery = Math.max(1, labelEvery);
        this.labelEveryColumn = labelEveryColumn;
    }

    @Override
    protected void paintChart(Graphics2D g, int width, int height) {
        double max = 0;
        int peak = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
                peak = i;
            }
        }
        double axisMax = niceMax(max);
        int plotBottom = height - BOTTOM;
        int plotHeight = plotBottom - TOP;

        // Recessive gridlines and y-axis ticks.
        g.setFont(AXIS_FONT);
        for (int tick = 0; tick <= 4; tick++) {
            double value = axisMax * tick / 4;
            int y = (int) Math.round(plotBottom - plotHeight * value / axisMax);
            g.setColor(GRID);
            g.drawLine(LEFT, y, width - RIGHT, y);
            g.setColor(MUTED);
            drawRight(g, compact(value), LEFT - 6, y + 4);
        }

        double slot = slotWidth(width);
        double barWidth = Math.max(2, Math.min(24, slot - 2));
        for (int i = 0; i < values.length; i++) {
            double center = LEFT + slot * i + slot / 2;
            double top = plotBottom - plotHeight * values[i] / axisMax;
            if (i == hoverIndex) {
                g.setColor(new Color(0x2A, 0x30, 0x44, 120));
                g.fillRect((int) (center - slot / 2), TOP, (int) Math.ceil(slot), plotHeight);
            }
            g.setColor(hoverIndex < 0 || i == hoverIndex ? color : color.darker());
            fillVerticalBar(g, center - barWidth / 2, top, barWidth, plotBottom);

            g.setColor(MUTED);
            g.setFont(AXIS_FONT);
            if (showAxisLabel(i, values.length, labelEvery)) {
                drawAxisLabel(g, labels[i], (float) center, height - 6, width);
            }
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

    private double slotWidth(int width) {
        return values.length == 0 ? 0 : (double) (width - LEFT - RIGHT) / values.length;
    }

    // The whole column slot (not just the thin bar) counts as the hover target.
    @Override
    protected int indexAt(Point point) {
        double slot = slotWidth(getWidth());
        if (slot <= 0 || point.x < LEFT || point.x > getWidth() - RIGHT
                || point.y < TOP || point.y > getHeight() - BOTTOM) {
            return -1;
        }
        return Math.min(values.length - 1, (int) ((point.x - LEFT) / slot));
    }

    @Override
    protected String tooltipFor(int index) {
        return tooltip.apply(index);
    }
}
