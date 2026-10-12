package views;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.function.IntFunction;

/**
 * A line with a soft area wash, used for running totals such as "tickets sold so far".
 * Hovering shows a crosshair, a marker and a tooltip for the nearest day.
 *
 * OOP idea: another subclass of {@link ChartPanel}. It shares the base class's hover tracking
 * and helpers, but overrides paintChart() and indexAt() to draw and hit-test a line.
 */
class LineChart extends ChartPanel {
    private static final int LEFT = 42;
    private static final int RIGHT = 14;
    private static final int TOP = 18;
    private static final int BOTTOM = 24;

    private final double[] values;
    private final String[] labels;
    private final IntFunction<String> tooltip;
    private final Color color;
    private final int labelEvery;
    // Optional horizontal reference line (for example total capacity). NaN means "no line".
    private final double reference;
    private final String referenceLabel;

    LineChart(double[] values, String[] labels, IntFunction<String> tooltip, Color color, int labelEvery,
            double reference, String referenceLabel, int height) {
        super(height);
        this.values = values.clone();
        this.labels = labels.clone();
        this.tooltip = tooltip;
        this.color = color;
        this.labelEvery = Math.max(1, labelEvery);
        this.reference = reference;
        this.referenceLabel = referenceLabel;
    }

    @Override
    protected void paintChart(Graphics2D g, int width, int height) {
        if (values.length == 0) return;
        double max = 0;
        for (double value : values) max = Math.max(max, value);
        if (!Double.isNaN(reference)) max = Math.max(max, reference);
        double axisMax = niceMax(max);
        int plotBottom = height - BOTTOM;
        int plotHeight = plotBottom - TOP;

        g.setFont(AXIS_FONT);
        for (int tick = 0; tick <= 4; tick++) {
            double value = axisMax * tick / 4;
            int y = (int) Math.round(plotBottom - plotHeight * value / axisMax);
            g.setColor(GRID);
            g.drawLine(LEFT, y, width - RIGHT, y);
            g.setColor(MUTED);
            drawRight(g, compact(value), LEFT - 6, y + 4);
        }
        for (int i = 0; i < values.length; i++) {
            if (showAxisLabel(i, values.length, labelEvery)) {
                drawAxisLabel(g, labels[i], (float) xAt(i, width), height - 6, width);
            }
        }

        // Reference line: a solid hairline with its label, never dashed.
        if (!Double.isNaN(reference)) {
            int y = (int) Math.round(yAt(reference, axisMax, plotBottom, plotHeight));
            g.setColor(MUTED);
            g.drawLine(LEFT, y, width - RIGHT, y);
            g.setFont(AXIS_FONT);
            g.drawString(referenceLabel, LEFT + 6, y - 5);
        }

        Path2D line = new Path2D.Double();
        Path2D area = new Path2D.Double();
        area.moveTo(xAt(0, width), plotBottom);
        for (int i = 0; i < values.length; i++) {
            double x = xAt(i, width);
            double y = yAt(values[i], axisMax, plotBottom, plotHeight);
            if (i == 0) line.moveTo(x, y); else line.lineTo(x, y);
            area.lineTo(x, y);
        }
        area.lineTo(xAt(values.length - 1, width), plotBottom);
        area.closePath();

        // Area wash at ~10% opacity, then the 2px line on top.
        g.setComposite(AlphaComposite.SrcOver.derive(0.12f));
        g.setColor(color);
        g.fill(area);
        g.setComposite(AlphaComposite.SrcOver);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(line);

        // End marker with its value: the one number this chart labels directly.
        int last = values.length - 1;
        drawMarker(g, xAt(last, width), yAt(values[last], axisMax, plotBottom, plotHeight));
        g.setColor(TEXT);
        g.setFont(VALUE_FONT);
        drawRight(g, compact(values[last]), (float) xAt(last, width) - 8,
                (float) yAt(values[last], axisMax, plotBottom, plotHeight) - 9);

        // Hover crosshair and marker.
        if (hoverIndex >= 0) {
            double x = xAt(hoverIndex, width);
            g.setColor(MUTED);
            g.setStroke(new BasicStroke(1f));
            g.drawLine((int) x, TOP, (int) x, plotBottom);
            drawMarker(g, x, yAt(values[hoverIndex], axisMax, plotBottom, plotHeight));
        }
    }

    // A filled dot with a 2px ring in the surface colour so it stays readable on top of the line.
    private void drawMarker(Graphics2D g, double x, double y) {
        g.setColor(SURFACE);
        g.fill(new Ellipse2D.Double(x - 6, y - 6, 12, 12));
        g.setColor(color);
        g.fill(new Ellipse2D.Double(x - 4, y - 4, 8, 8));
    }

    private double xAt(int index, int width) {
        return values.length == 1 ? LEFT : LEFT + (double) (width - LEFT - RIGHT) * index / (values.length - 1);
    }

    private static double yAt(double value, double axisMax, int plotBottom, int plotHeight) {
        return plotBottom - plotHeight * value / axisMax;
    }

    // Snaps to the nearest day, so the whole plot area works as the hover target.
    @Override
    protected int indexAt(Point point) {
        if (values.length == 0 || point.x < LEFT - 8 || point.x > getWidth() - RIGHT + 8) return -1;
        double step = values.length == 1 ? 1 : (double) (getWidth() - LEFT - RIGHT) / (values.length - 1);
        int index = (int) Math.round((point.x - LEFT) / step);
        return Math.max(0, Math.min(values.length - 1, index));
    }

    @Override
    protected String tooltipFor(int index) {
        return tooltip.apply(index);
    }
}
