package views;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.function.DoubleFunction;
import java.util.function.IntFunction;

/**
 * Horizontal bars, one row per category (for example one row per ticket tier).
 * When row maximums are given, each bar sits on a track showing the full capacity,
 * so it doubles as a progress meter.
 *
 * OOP idea: a third {@link ChartPanel} subclass. The screens can use ColumnChart, LineChart
 * or HorizontalBarChart interchangeably as "a chart" (polymorphism).
 */
class HorizontalBarChart extends ChartPanel {
    private static final int VALUE_WIDTH = 104;
    private static final int ROW_HEIGHT = 40;
    private static final int THICKNESS = 14;

    private final String[] labels;
    private final double[] values;
    // Optional: the most each row could reach (capacity). null means bars share one scale.
    private final double[] maximums;
    private final DoubleFunction<String> valueText;
    private final IntFunction<String> tooltip;
    // When set, every bar uses this one colour (rows are the same kind of thing, e.g. events).
    // When null, each row gets the next categorical colour (rows are different kinds, e.g. tiers).
    private final Color singleColor;

    HorizontalBarChart(String[] labels, double[] values, double[] maximums, DoubleFunction<String> valueText,
            IntFunction<String> tooltip) {
        this(labels, values, maximums, valueText, tooltip, null);
    }

    // Overloaded constructor that also takes a single bar colour.
    HorizontalBarChart(String[] labels, double[] values, double[] maximums, DoubleFunction<String> valueText,
            IntFunction<String> tooltip, Color singleColor) {
        super(Math.max(1, labels.length) * ROW_HEIGHT + 8);
        this.singleColor = singleColor;
        this.labels = labels.clone();
        this.values = values.clone();
        this.maximums = maximums == null ? null : maximums.clone();
        this.valueText = valueText;
        this.tooltip = tooltip;
    }

    @Override
    protected void paintChart(Graphics2D g, int width, int height) {
        double sharedMax = 0;
        for (double value : values) sharedMax = Math.max(sharedMax, value);
        // The label column grows to fit the longest label, up to 40% of the chart width.
        g.setFont(ViewStyles.BODY_FONT.deriveFont(12f));
        int keyWidth = singleColor == null ? 14 : 0;
        int widest = 0;
        for (String label : labels) widest = Math.max(widest, g.getFontMetrics().stringWidth(label));
        int barLeft = Math.max(70, Math.min((int) (width * 0.4), widest + keyWidth + 14));
        int barRight = width - VALUE_WIDTH;
        for (int i = 0; i < labels.length; i++) {
            int rowTop = 4 + i * ROW_HEIGHT;
            int barY = rowTop + (ROW_HEIGHT - THICKNESS) / 2;
            Color color = singleColor != null ? singleColor : SERIES[i % SERIES.length];
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
            g.setColor(TEXT);
            g.setFont(ViewStyles.BODY_FONT.deriveFont(12f));
            g.drawString(fitText(g, labels[i], barLeft - textLeft - 10), textLeft, barY + 11);

            double max = maximums == null ? sharedMax : maximums[i];
            double fraction = max <= 0 ? 0 : Math.min(1, values[i] / max);
            if (maximums != null) {
                // Track: the unfilled part of the meter, a dark step of the same hue.
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
                g.fillRoundRect(barLeft, barY, barRight - barLeft, THICKNESS, 8, 8);
            }
            g.setColor(color);
            fillHorizontalBar(g, barLeft, barY, barLeft + (barRight - barLeft) * fraction, THICKNESS);

            // Value at the bar's tip; meters keep it in a fixed column after the full track.
            g.setColor(TEXT);
            g.setFont(VALUE_FONT);
            int valueX = maximums == null ? (int) (barLeft + (barRight - barLeft) * fraction) + 8 : barRight + 10;
            g.drawString(valueText.apply(values[i]), valueX, barY + 11);
            if (maximums != null) {
                g.setColor(MUTED);
                g.setFont(AXIS_FONT);
                drawRight(g, Math.round(fraction * 100) + "%", width, barY + 11);
            }
        }
    }

    // Shortens a label with "..." so it never runs into the bar.
    private static String fitText(Graphics2D g, String text, int maxWidth) {
        if (g.getFontMetrics().stringWidth(text) <= maxWidth) return text;
        String shortened = text;
        while (shortened.length() > 1 && g.getFontMetrics().stringWidth(shortened + "...") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "...";
    }

    // The whole row is the hover target, not just the thin bar.
    @Override
    protected int indexAt(Point point) {
        int index = (point.y - 4) / ROW_HEIGHT;
        return point.y < 4 || index >= labels.length ? -1 : index;
    }

    @Override
    protected String tooltipFor(int index) {
        return tooltip.apply(index);
    }
}
