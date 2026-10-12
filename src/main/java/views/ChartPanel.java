// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import java.awt.Color;                     // An RGB colour
import java.awt.Dimension;                 // A width and height
import java.awt.Font;                      // Font style constants
import java.awt.Graphics;                  // The basic drawing "pen" Swing hands us
import java.awt.Graphics2D;                // A more powerful drawing pen
import java.awt.RenderingHints;            // Drawing quality settings
import javax.swing.JPanel;                 // A container we can draw on

/**
 * Base class for the app's two charts.
 *
 *   ChartPanel (abstract)
 *     |- ColumnChart        vertical bars, e.g. tickets sold per day
 *     |- HorizontalBarChart sideways bars, e.g. revenue per event
 *
 * OOP ideas shown here:
 * - Abstract class: you can't create a plain ChartPanel. It leaves paintChart() empty ("abstract")
 *   for each kind of chart to fill in.
 * - Inheritance: both charts get the shared colours, size setup and helper methods from here.
 * - Polymorphism: Swing calls paintComponent() on any chart, and Java runs the right
 *   subclass's paintChart() to draw it.
 */
abstract class ChartPanel extends JPanel {
    // Shared chart colours, tuned for the dark dashboard.
    static final Color GRID = new Color(0x2A, 0x30, 0x44);      // Faint gridlines
    static final Color TEXT = new Color(0xF0, 0xF4, 0xFF);      // Main text (near white)
    static final Color MUTED = new Color(0x88, 0x96, 0xB3);     // Axis labels (grey-blue)
    // Colours for different rows (e.g. ticket tiers): blue, orange, green, yellow.
    static final Color[] SERIES = {
        new Color(0x39, 0x87, 0xE5), new Color(0xD9, 0x59, 0x26),
        new Color(0x19, 0x9E, 0x70), new Color(0xC9, 0x85, 0x00)
    };
    static final Font AXIS_FONT = ViewStyles.DATA_FONT.deriveFont(Font.PLAIN, 10f);   // Small axis text
    static final Font VALUE_FONT = ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 11f);   // Value labels

    // Constructor shared by both charts: sets the size.
    ChartPanel(int height) {
        setOpaque(false);                                  // Let the card colour show through
        setPreferredSize(new Dimension(320, height));
    }

    // Swing calls this whenever the chart needs drawing. It switches on smooth edges,
    // then lets the subclass draw (that's the abstract method below).
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintChart(g, getWidth(), getHeight());
    }

    // Each subclass must write this method to draw its own kind of chart.
    abstract void paintChart(Graphics2D g, int width, int height);

    // ---- Helpers shared by the subclasses ----

    // Short numbers: 950, 1.2K, 3.4M.
    static String compact(double value) {
        if (value >= 1000000) {
            return String.format("%.1fM", value / 1000000);   // Millions, one decimal
        }
        if (value >= 1000) {
            return String.format("%.1fK", value / 1000);      // Thousands, one decimal
        }
        return String.format("%.0f", value);                  // Small numbers, no decimals
    }

    // Rounds the top of an axis up to a number that splits into 4 whole steps (e.g. 13 -> 16).
    static double niceMax(double max) {
        if (max <= 0) {
            return 4;
        }
        return Math.ceil(max / 4) * 4;
    }

    // Draws text so that it ENDS at x (right-aligned).
    static void drawRight(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text), y);
    }

    // Draws text centred on x.
    static void drawCentered(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
    }
}
