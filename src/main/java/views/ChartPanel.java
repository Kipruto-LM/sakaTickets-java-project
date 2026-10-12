package views;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntConsumer;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

/**
 * Base class for every chart in the app (columns, lines, bars, rings).
 *
 * OOP ideas shown here:
 * - Abstract class: you can't create a plain ChartPanel. It leaves three methods abstract
 *   (paintChart, indexAt, tooltipFor) for each kind of chart to fill in.
 * - Template method pattern: paintComponent() does the shared setup (smooth edges, clean-up)
 *   and then calls paintChart(), which each subclass writes its own way.
 * - Polymorphism: the screens treat every chart as a ChartPanel, and Java runs the right
 *   subclass's drawing code at runtime.
 * - Inheritance for reuse: shared colours, number formatting and hover tracking live here once.
 */
abstract class ChartPanel extends JPanel {
    // Shared chart colours, tuned for the dark dashboard surface.
    static final Color SURFACE = new Color(0x17, 0x1B, 0x24);
    static final Color GRID = new Color(0x2A, 0x30, 0x44);
    static final Color TEXT = new Color(0xF0, 0xF4, 0xFF);
    static final Color MUTED = new Color(0x88, 0x96, 0xB3);
    // Categorical colours in a fixed order (blue, orange, aqua, yellow), checked for colour-blind safety.
    static final Color[] SERIES = {
        new Color(0x39, 0x87, 0xE5), new Color(0xD9, 0x59, 0x26),
        new Color(0x19, 0x9E, 0x70), new Color(0xC9, 0x85, 0x00)
    };
    static final Font AXIS_FONT = ViewStyles.DATA_FONT.deriveFont(Font.PLAIN, 10f);
    static final Font VALUE_FONT = ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 11f);

    // Which data point the mouse is over (-1 means none). Protected so subclasses can read it.
    protected int hoverIndex = -1;
    // Optional action to run when a data point is clicked (null = not clickable).
    private IntConsumer clickAction;

    // Constructor shared by all charts: sets the size and starts tracking the mouse for hover effects.
    protected ChartPanel(int preferredHeight) {
        setOpaque(false);
        setPreferredSize(new Dimension(320, preferredHeight));
        setMinimumSize(new Dimension(160, preferredHeight));
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int index = indexAt(event.getPoint());
                if (index != hoverIndex) {
                    hoverIndex = index;
                    setCursor(Cursor.getPredefinedCursor(index >= 0 && clickAction != null
                            ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                int index = indexAt(event.getPoint());
                if (index >= 0 && clickAction != null) {
                    clickAction.accept(index);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hoverIndex = -1;
                repaint();
            }
        };
        addMouseListener(hover);
        addMouseMotionListener(hover);
    }

    // Makes each data point clickable; the action receives the clicked index.
    void setClickAction(IntConsumer clickAction) {
        this.clickAction = clickAction;
    }

    // Template method: "final" so subclasses can't skip the shared setup. They override paintChart() instead.
    @Override
    protected final void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        paintChart(g, getWidth(), getHeight());
        g.dispose();
    }

    // Swing calls this to find the tooltip text for the point under the mouse.
    @Override
    public String getToolTipText(MouseEvent event) {
        int index = indexAt(event.getPoint());
        return index < 0 ? null : tooltipFor(index);
    }

    // The three "holes" each subclass must fill in.
    protected abstract void paintChart(Graphics2D g, int width, int height);
    protected abstract int indexAt(Point point);
    protected abstract String tooltipFor(int index);

    // ---- Helpers shared by the subclasses ----

    // Short numbers for axes: 950, 1.2K, 3.4M.
    static String compact(double value) {
        if (Math.abs(value) >= 1_000_000) return trim(value / 1_000_000) + "M";
        if (Math.abs(value) >= 1_000) return trim(value / 1_000) + "K";
        return trim(value);
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? String.format("%.0f", value) : String.format("%.1f", value);
    }

    // Rounds an axis maximum up to a clean number that splits into 4 even ticks
    // (8 -> 2, 4, 6, 8; 120 -> 30, 60, 90, 120), so the ticks read well.
    static double niceMax(double max) {
        if (max <= 0) return 4;
        if (max <= 20) return Math.max(4, Math.ceil(max / 4) * 4);
        double magnitude = Math.pow(10, Math.floor(Math.log10(max)));
        for (double step : new double[] {1, 1.2, 1.6, 2, 2.4, 4, 5, 6, 8, 10}) {
            if (step * magnitude >= max) return step * magnitude;
        }
        return 10 * magnitude;
    }

    // A bar with a 4px rounded end at the data and a square end at the baseline.
    static void fillVerticalBar(Graphics2D g, double x, double top, double width, double bottom) {
        double height = bottom - top;
        if (height <= 0) return;
        double radius = Math.min(4, height);
        g.fill(new RoundRectangle2D.Double(x, top, width, height, radius * 2, radius * 2));
        g.fill(new java.awt.geom.Rectangle2D.Double(x, top + radius, width, height - radius));
    }

    static void fillHorizontalBar(Graphics2D g, double left, double y, double right, double thickness) {
        double width = right - left;
        if (width <= 0) return;
        double radius = Math.min(4, width);
        g.fill(new RoundRectangle2D.Double(left, y, width, thickness, radius * 2, radius * 2));
        g.fill(new java.awt.geom.Rectangle2D.Double(left, y, width - radius, thickness));
    }

    // Draws text right-aligned so it ends at x.
    static void drawRight(Graphics2D g, String text, float x, float y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text), y);
    }

    // Whether the x-axis label at this index should be drawn: every Nth one plus the last,
    // skipping any that would sit too close to the last label and overlap it.
    static boolean showAxisLabel(int index, int count, int every) {
        if (index == count - 1 || every == 1) return true;
        return index % every == 0 && count - 1 - index >= Math.max(2, every * 2 / 3);
    }

    // Draws an axis label centred under x, but nudged inwards so it never runs off the chart's edge.
    static void drawAxisLabel(Graphics2D g, String text, float x, float y, int chartWidth) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        float left = Math.max(0, Math.min(chartWidth - textWidth - 1, x - textWidth / 2f));
        g.drawString(text, left, y);
    }

    static void drawCentered(Graphics2D g, String text, float x, float y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2f, y);
    }
}
