// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import java.awt.Color;                     // An RGB colour
import java.awt.Cursor;                    // The mouse pointer shape (arrow, hand, ...)
import java.awt.Dimension;                 // A width and height
import java.awt.Font;                      // Font style constants
import java.awt.Graphics;                  // The basic drawing "pen" Swing hands us
import java.awt.Graphics2D;                // A more powerful drawing pen
import java.awt.Point;                     // An (x, y) position
import java.awt.RenderingHints;            // Drawing quality settings
import java.awt.event.MouseAdapter;        // Lets us react to only the mouse events we care about
import java.awt.event.MouseEvent;          // Details of a mouse event (where, which button)
import java.awt.geom.RoundRectangle2D;     // A rectangle with rounded corners
import java.util.function.IntConsumer;     // An object holding an action that takes one int
import javax.swing.JPanel;                 // A container we can draw on
import javax.swing.ToolTipManager;         // Shows the little hover pop-up text

/**
 * Base class for every chart in the app (columns, lines, bars, rings).
 *
 * The family of chart classes:
 *   ChartPanel (abstract)
 *     |- ColumnChart        vertical bars, e.g. tickets sold per day
 *     |- HorizontalBarChart sideways bars, e.g. revenue per tier
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
    static final Color SURFACE = new Color(0x17, 0x1B, 0x24);   // Card background behind the charts
    static final Color GRID = new Color(0x2A, 0x30, 0x44);      // Faint gridlines
    static final Color TEXT = new Color(0xF0, 0xF4, 0xFF);      // Main text (near white)
    static final Color MUTED = new Color(0x88, 0x96, 0xB3);     // Axis labels (grey-blue)
    // Categorical colours in a fixed order (blue, orange, aqua, yellow), checked for colour-blind safety.
    // SERIES[0] is blue, SERIES[1] orange, and so on. Arrays start counting at 0.
    static final Color[] SERIES = {
        new Color(0x39, 0x87, 0xE5), new Color(0xD9, 0x59, 0x26),
        new Color(0x19, 0x9E, 0x70), new Color(0xC9, 0x85, 0x00)
    };
    static final Font AXIS_FONT = ViewStyles.DATA_FONT.deriveFont(Font.PLAIN, 10f);   // Small axis text
    static final Font VALUE_FONT = ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 11f);   // Value labels on bars/lines

    // Which data point the mouse is over (-1 means none). Protected so subclasses can read it.
    protected int hoverIndex = -1;
    // Optional action to run when a data point is clicked (null = not clickable).
    private IntConsumer clickAction;

    // Constructor shared by all charts: sets the size and starts tracking the mouse for hover effects.
    // "protected" = only subclasses (and classes in this package) can call it.
    protected ChartPanel(int preferredHeight) {
        setOpaque(false);                                         // Let the card colour show through
        setPreferredSize(new Dimension(320, preferredHeight));    // Ideal size
        setMinimumSize(new Dimension(160, preferredHeight));      // Smallest size
        ToolTipManager.sharedInstance().registerComponent(this);  // Turn on tooltips for this chart
        // An anonymous MouseAdapter subclass that overrides just the three mouse events we need.
        MouseAdapter hover = new MouseAdapter() {
            // Runs every time the mouse moves over the chart.
            @Override
            public void mouseMoved(MouseEvent event) {
                int index = indexAt(event.getPoint());   // Which data point is under the mouse?
                if (index != hoverIndex) {               // Only act when it changes
                    hoverIndex = index;
                    // Hand cursor over a clickable point, normal arrow otherwise.
                    setCursor(Cursor.getPredefinedCursor(index >= 0 && clickAction != null
                            ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();                           // Redraw so the hover highlight moves
                }
            }

            // Runs when the chart is clicked.
            @Override
            public void mouseClicked(MouseEvent event) {
                int index = indexAt(event.getPoint());
                if (index >= 0 && clickAction != null) {
                    clickAction.accept(index);           // Run the click action with that point's index
                }
            }

            // Runs when the mouse leaves the chart: clear the highlight.
            @Override
            public void mouseExited(MouseEvent event) {
                hoverIndex = -1;
                repaint();
            }
        };
        addMouseListener(hover);          // Clicks, enter and exit
        addMouseMotionListener(hover);    // Movement
    }

    // Makes each data point clickable; the action receives the clicked index.
    void setClickAction(IntConsumer clickAction) {
        this.clickAction = clickAction;
    }

    // Template method: "final" so subclasses can't skip the shared setup. They override paintChart() instead.
    @Override
    protected final void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();    // A copy of the pen we can change freely
        // Turn on smooth edges for shapes and text, and precise line drawing.
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        paintChart(g, getWidth(), getHeight());           // The subclass draws its own chart here
        g.dispose();                                      // Free the copied pen
    }

    // Swing calls this to find the tooltip text for the point under the mouse.
    // Returning null means "no tooltip here".
    @Override
    public String getToolTipText(MouseEvent event) {
        int index = indexAt(event.getPoint());
        return index < 0 ? null : tooltipFor(index);
    }

    // The three "holes" each subclass must fill in.
    // "abstract" = no body here; every subclass MUST write its own version.
    protected abstract void paintChart(Graphics2D g, int width, int height);   // Draw the chart
    protected abstract int indexAt(Point point);                               // Which point is at (x, y)? -1 = none
    protected abstract String tooltipFor(int index);                           // Hover text for that point

    // ---- Helpers shared by the subclasses ----

    // Short numbers for axes: 950, 1.2K, 3.4M.
    static String compact(double value) {
        if (Math.abs(value) >= 1_000_000) return trim(value / 1_000_000) + "M";   // Millions
        if (Math.abs(value) >= 1_000) return trim(value / 1_000) + "K";           // Thousands
        return trim(value);                                                        // Small numbers as they are
    }

    // Whole numbers without decimals ("12"), others with one decimal ("1.5").
    // Math.rint rounds to the nearest whole number, so value == rint(value) means "already whole".
    private static String trim(double value) {
        return value == Math.rint(value) ? String.format("%.0f", value) : String.format("%.1f", value);
    }

    // Rounds an axis maximum up to a clean number that splits into 4 even ticks
    // (8 -> 2, 4, 6, 8; 120 -> 30, 60, 90, 120), so the ticks read well.
    static double niceMax(double max) {
        if (max <= 0) return 4;                                       // Empty chart: show 0-4
        if (max <= 20) return Math.max(4, Math.ceil(max / 4) * 4);    // Small counts: next multiple of 4
        // magnitude = the power of ten just below max (e.g. 100 for 347).
        double magnitude = Math.pow(10, Math.floor(Math.log10(max)));
        // Try 100, 120, 160, 200, 240, 400, ... and return the first one that's big enough.
        for (double step : new double[] {1, 1.2, 1.6, 2, 2.4, 4, 5, 6, 8, 10}) {
            if (step * magnitude >= max) return step * magnitude;
        }
        return 10 * magnitude;
    }

    // A bar with a 4px rounded end at the data and a square end at the baseline.
    // It's drawn as a rounded rectangle, then a plain rectangle over the bottom part to square it off.
    static void fillVerticalBar(Graphics2D g, double x, double top, double width, double bottom) {
        double height = bottom - top;
        if (height <= 0) return;                         // Zero value: nothing to draw
        double radius = Math.min(4, height);             // Corner size (smaller for very short bars)
        g.fill(new RoundRectangle2D.Double(x, top, width, height, radius * 2, radius * 2));
        g.fill(new java.awt.geom.Rectangle2D.Double(x, top + radius, width, height - radius));
    }

    // The sideways version: rounded on the right (data) end, square on the left (baseline).
    static void fillHorizontalBar(Graphics2D g, double left, double y, double right, double thickness) {
        double width = right - left;
        if (width <= 0) return;
        double radius = Math.min(4, width);
        g.fill(new RoundRectangle2D.Double(left, y, width, thickness, radius * 2, radius * 2));
        g.fill(new java.awt.geom.Rectangle2D.Double(left, y, width - radius, thickness));
    }

    // Draws text right-aligned so it ends at x.
    static void drawRight(Graphics2D g, String text, float x, float y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text), y);   // Start one text-width to the left
    }

    // Whether the x-axis label at this index should be drawn: every Nth one plus the last,
    // skipping any that would sit too close to the last label and overlap it.
    static boolean showAxisLabel(int index, int count, int every) {
        if (index == count - 1 || every == 1) return true;    // Always show the last label (and all if every == 1)
        // % is "remainder": index % every == 0 picks every Nth label.
        return index % every == 0 && count - 1 - index >= Math.max(2, every * 2 / 3);
    }

    // Draws an axis label centred under x, but nudged inwards so it never runs off the chart's edge.
    static void drawAxisLabel(Graphics2D g, String text, float x, float y, int chartWidth) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        // Where centring would put the left edge, kept between 0 and (chart width - text width).
        float left = Math.max(0, Math.min(chartWidth - textWidth - 1, x - textWidth / 2f));
        g.drawString(text, left, y);
    }

    // Draws text centred on x.
    static void drawCentered(Graphics2D g, String text, float x, float y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2f, y);
    }
}
