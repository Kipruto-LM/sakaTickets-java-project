// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import java.awt.AlphaComposite;            // Controls transparency when drawing
import java.awt.Color;                     // An RGB colour (optionally with transparency)
import java.awt.Dimension;                 // A width and height
import java.awt.GradientPaint;             // A smooth fade from one colour to another
import java.awt.Graphics;                  // The basic "pen" Swing gives us for drawing
import java.awt.Graphics2D;                // A more powerful pen with shapes, smoothing, etc.
import java.awt.RenderingHints;            // Quality settings for drawing
import java.awt.geom.RoundRectangle2D;     // A rectangle with rounded corners
import java.awt.image.BufferedImage;       // A picture held in memory
import java.io.File;                       // A file on disk
import java.io.IOException;                // Error thrown when reading a file fails
import java.util.Locale;                   // Language rules for lower-casing
import javax.imageio.ImageIO;              // Reads JPG/PNG files into BufferedImages
import javax.swing.JPanel;                 // A container we can draw on
import models.Event;                       // The event model

/**
 * Builds the image panels (event pictures and banners) shown across the app.
 *
 * OOP ideas shown here:
 * - Utility class: a final class with a private constructor and only static methods.
 * - Anonymous inner class: imagePanel() creates a JPanel subclass on the spot and
 *   overrides paintComponent() to draw a picture with rounded corners.
 * - Abstraction: screens just ask for EventVisuals.event(...) and get a finished panel back,
 *   without knowing how the image is loaded or scaled.
 */
final class EventVisuals {
    // The big concert photo, loaded once when the class is first used and then reused.
    private static final BufferedImage HERO = loadResource("/images/featured-concert.jpg");

    // Private constructor: nobody needs an EventVisuals object, only its static methods.
    private EventVisuals() {
    }

    // The public helpers below all reuse the same private imagePanel() method.
    // height = panel height in pixels; cornerRadius = how rounded the corners are.

    // Concert photo with a slightly stronger dark shade (used on the receipt screen).
    static JPanel hero(int height, int cornerRadius) {
        return imagePanel(HERO, height, cornerRadius, 0.16f);
    }

    // Concert photo for the welcome page's "Attend" card.
    static JPanel buyerChoice(int height, int cornerRadius) {
        return imagePanel(HERO, height, cornerRadius, 0.08f);
    }

    // Meetup photo for the welcome page's "Create" card.
    static JPanel hostChoice(int height, int cornerRadius) {
        return imagePanel(loadResource("/images/event-tech.jpg"), height, cornerRadius, 0.08f);
    }

    // The picture for a particular event.
    static JPanel event(Event event, int height, int cornerRadius) {
        return imagePanel(loadEventImage(event), height, cornerRadius, 0.08f);
    }

    // Builds a panel that draws `image` filling its whole area, with rounded corners and a
    // dark fade from the left. `shade` is how strong that fade is (0 = none, 1 = full).
    private static JPanel imagePanel(BufferedImage image, int height, int cornerRadius, float shade) {
        // Anonymous subclass of JPanel. @Override replaces the parent's paintComponent()
        // with our own drawing code (polymorphism: Swing calls OUR version when it repaints).
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);                   // Let JPanel do its normal painting first
                Graphics2D g = (Graphics2D) graphics.create();    // A copy of the pen we can change freely
                // Smooth edges and smooth picture scaling.
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                // "Clip" to a rounded rectangle: anything drawn outside it is cut off,
                // which gives the picture its rounded corners.
                RoundRectangle2D mask = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(),
                        cornerRadius, cornerRadius);
                g.clip(mask);
                if (image == null) {
                    // No picture found: fill with a teal-to-navy colour fade instead.
                    g.setPaint(new GradientPaint(0, 0, new Color(0x08, 0x67, 0x8C),
                            getWidth(), getHeight(), new Color(0x16, 0x1A, 0x28)));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    drawCover(g, image, getWidth(), getHeight());   // Draw the picture to fill the panel
                }
                // Draw a see-through dark fade over the picture (darker on the left).
                // The 4th number in each Color is its opacity (0 = invisible, 255 = solid).
                g.setComposite(AlphaComposite.SrcOver.derive(shade));
                g.setPaint(new GradientPaint(0, 0, new Color(0x0F, 0x11, 0x17, 220),
                        getWidth(), 0, new Color(0x0F, 0x11, 0x17, 25)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();                                      // Free the copied pen
            }
        };
        panel.setOpaque(false);                                   // Don't paint a plain background behind the picture
        panel.setPreferredSize(new Dimension(480, height));       // Ideal size
        panel.setMinimumSize(new Dimension(180, height));         // Smallest size
        return panel;
    }

    // Draws the image scaled to COVER the whole area (like a phone wallpaper): it keeps its
    // shape, fills every pixel, and any overflow is cropped equally from both sides.
    private static void drawCover(Graphics2D g, BufferedImage image, int width, int height) {
        if (width <= 0 || height <= 0) return;      // Nothing to draw into yet
        // Pick the larger of the two scale factors so the image covers both width and height.
        double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
        int drawWidth = (int) Math.ceil(image.getWidth() * scale);
        int drawHeight = (int) Math.ceil(image.getHeight() * scale);
        // Centre the scaled image (x or y may be negative, which crops the edges).
        int x = (width - drawWidth) / 2;
        int y = (height - drawHeight) / 2;
        g.drawImage(image, x, y, drawWidth, drawHeight, null);
    }

    // Finds the right picture for an event:
    //   1. the host's chosen banner (a built-in image or a file on the computer), or
    //   2. a built-in picture that matches the event's category.
    private static BufferedImage loadEventImage(Event event) {
        String customPath = event.getBannerImagePath();
        if (customPath != null && !customPath.isBlank()) {
            if (customPath.startsWith("/images/")) {             // A picture bundled with the app
                BufferedImage bundled = loadResource(customPath);
                if (bundled != null) return bundled;
            }
            try {
                BufferedImage custom = ImageIO.read(new File(customPath));   // A file the host picked
                if (custom != null) return custom;
            } catch (IOException ignored) {
                // Couldn't read the file: fall through to the category picture below.
            }
        }
        // Choose a picture from keywords in the category (lower-cased so "Tech" and "tech" both match).
        String category = event.getCategory() == null ? "" : event.getCategory().toLowerCase(Locale.ROOT);
        // A chain of ternaries works like if / else if / else.
        String resource = category.contains("tech") ? "/images/event-tech.jpg"
                : category.contains("food") ? "/images/event-food.jpg"
                : category.contains("music") || category.contains("acoustic") ? "/images/event-music.jpg"
                : category.contains("night") || category.contains("dance") ? "/images/event-nightlife.jpg"
                : "/images/event-music.jpg";
        return loadResource(resource);
    }

    // Loads a picture bundled inside the app (from src/main/resources). Returns null if it's missing.
    private static BufferedImage loadResource(String path) {
        try {
            return ImageIO.read(EventVisuals.class.getResource(path));
        } catch (Exception exception) {
            return null;                    // Missing or unreadable picture: the panel shows a colour instead
        }
    }
}
