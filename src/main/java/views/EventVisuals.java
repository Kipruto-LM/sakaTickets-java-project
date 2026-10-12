// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import java.awt.Color;                     // An RGB colour (optionally with transparency)
import java.awt.Dimension;                 // A width and height
import java.awt.Graphics;                  // The basic "pen" Swing gives us for drawing
import java.awt.image.BufferedImage;       // A picture held in memory
import java.io.File;                       // A file on disk
import javax.imageio.ImageIO;              // Reads JPG/PNG files into BufferedImages
import javax.swing.JPanel;                 // A container we can draw on
import models.Event;                       // The event model

/**
 * Builds the image panels (event pictures and banners) shown across the app.
 *
 * OOP ideas shown here:
 * - Utility class: screens only use its static methods (no EventVisuals objects are made).
 * - Nested class: ImagePanel (at the bottom) is a JPanel subclass that overrides
 *   paintComponent() to draw a picture.
 * - Abstraction: screens just ask for EventVisuals.event(...) and get a finished panel back,
 *   without knowing how the image is loaded or scaled.
 */
final class EventVisuals {
    // The big concert photo, loaded once when the class is first used and then reused.
    private static final BufferedImage HERO = loadResource("/images/featured-concert.jpg");

    // Concert photo (used on the receipt screen).
    static JPanel hero(int height) {
        return new ImagePanel(HERO, height);
    }

    // Concert photo for the welcome page's "Attend" card.
    static JPanel buyerChoice(int height) {
        return new ImagePanel(HERO, height);
    }

    // Meetup photo for the welcome page's "Create" card.
    static JPanel hostChoice(int height) {
        return new ImagePanel(loadResource("/images/event-tech.jpg"), height);
    }

    // The picture for a particular event.
    static JPanel event(Event event, int height) {
        return new ImagePanel(loadEventImage(event), height);
    }

    // Finds the right picture for an event:
    //   1. the host's chosen banner (a built-in image or a file on the computer), or
    //   2. a built-in picture that matches the event's category.
    private static BufferedImage loadEventImage(Event event) {
        String path = event.getBannerImagePath();
        if (path != null && path.startsWith("/images/")) {         // A picture bundled with the app
            BufferedImage bundled = loadResource(path);
            if (bundled != null) {
                return bundled;
            }
        }
        if (path != null && !path.isBlank()) {                       // A picture file the host picked
            try {
                return ImageIO.read(new File(path));
            } catch (Exception exception) {
                // Couldn't read the file: use a category picture below instead.
            }
        }
        // Choose a picture from words in the category (lower-cased so "Tech" and "tech" both match).
        String category = event.getCategory().toLowerCase();
        if (category.contains("tech")) {
            return loadResource("/images/event-tech.jpg");
        } else if (category.contains("food")) {
            return loadResource("/images/event-food.jpg");
        } else if (category.contains("night") || category.contains("dance")) {
            return loadResource("/images/event-nightlife.jpg");
        } else {
            return loadResource("/images/event-music.jpg");
        }
    }

    // Loads a picture bundled inside the app (from src/main/resources). Returns null if it's missing.
    private static BufferedImage loadResource(String path) {
        try {
            return ImageIO.read(EventVisuals.class.getResource(path));
        } catch (Exception exception) {
            return null;                    // Missing or unreadable picture: the panel shows a colour instead
        }
    }

    /**
     * A panel that shows one picture, scaled to fill the whole panel.
     *
     * OOP idea: inheritance + overriding. ImagePanel IS a JPanel, and it overrides
     * paintComponent() - the method Swing calls whenever the panel needs drawing - to draw the picture.
     */
    private static class ImagePanel extends JPanel {
        private final BufferedImage image;      // The picture (null if it couldn't be loaded)

        ImagePanel(BufferedImage image, int height) {
            this.image = image;
            setPreferredSize(new Dimension(480, height));
            setMinimumSize(new Dimension(180, height));
            setBackground(new Color(0x16, 0x1A, 0x28));    // Shown when there's no picture
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);                       // Paint the background colour first
            if (image == null) {
                return;                                    // No picture: just the background colour
            }
            // Scale the picture so it COVERS the panel without being stretched (like a phone wallpaper):
            // use whichever scale is bigger, then centre it. Any extra is cut off at the edges.
            double scale = Math.max((double) getWidth() / image.getWidth(), (double) getHeight() / image.getHeight());
            int drawWidth = (int) (image.getWidth() * scale);
            int drawHeight = (int) (image.getHeight() * scale);
            int x = (getWidth() - drawWidth) / 2;
            int y = (getHeight() - drawHeight) / 2;
            g.drawImage(image, x, y, drawWidth, drawHeight, null);
        }
    }
}
