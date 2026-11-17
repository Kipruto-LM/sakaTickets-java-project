package views;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import models.Event;

/**
 * Shared image helper for the views. It loads artwork and creates Swing panels
 * that paint cropped, rounded images when Swing asks them to redraw.
 *
 * Core concepts:
 * - Abstraction: callers request artwork without handling image loading or painting.
 * - Encapsulation: image selection and rendering helpers are hidden from the screens.
 * - Inheritance/polymorphism: CoverImagePanel extends JPanel and overrides paintComponent().
 * - Static utility methods: the class provides shared behavior without being instantiated.
 */
final class EventVisuals {
    // Caches the primary hero image to prevent continuous disk reads
    private static final BufferedImage HERO = loadResource("/images/featured-concert.jpg");

    /** Prevents instances because this class only provides static artwork helpers. */
    private EventVisuals() {
    }

    /** Generates the main hero banner used on the ticket summary screen. */
    static JPanel hero(int height, int cornerRadius) {
        return imagePanel(HERO, height, cornerRadius, 0.16f);
    }

    /** Generates the artwork for the "Attend" role selection card. */
    static JPanel buyerChoice(int height, int cornerRadius) {
        return imagePanel(HERO, height, cornerRadius, 0.08f);
    }

    /** Generates the artwork for the "Create" role selection card. */
    static JPanel hostChoice(int height, int cornerRadius) {
        return imagePanel(loadResource("/images/event-tech.jpg"), height, cornerRadius, 0.08f);
    }

    /** 
     * Generates the artwork for a specific event based on its category or custom upload path.
     * 
     * @param event        The event data model containing category/image path information.
     * @param height       The requested panel height.
     * @param cornerRadius The arc diameter for the rounded corners.
     */
    static JPanel event(Event event, int height, int cornerRadius) {
        return imagePanel(loadEventImage(event), height, cornerRadius, 0.08f);
    }

    /** Creates the foundational CoverImagePanel with sizing constraints. */
    private static JPanel imagePanel(BufferedImage image, int height, int cornerRadius, float shade) {
        CoverImagePanel panel = new CoverImagePanel(image, cornerRadius, shade);
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(480, height));
        panel.setMinimumSize(new Dimension(180, height));
        return panel;
    }

    /**
     * A specialized JPanel. Overriding paintComponent customizes how this object
     * draws itself while keeping Swing's normal painting lifecycle.
     */
    private static final class CoverImagePanel extends JPanel {
        private final BufferedImage image;
        private final int cornerRadius;
        private final float shade;

        /** Stores rendering settings used later by Swing's paint lifecycle. */
        private CoverImagePanel(BufferedImage image, int cornerRadius, float shade) {
            this.image = image;
            this.cornerRadius = cornerRadius;
            this.shade = shade;
        }

        /** Paints clipped, scaled artwork and a tint whenever Swing redraws the panel. */
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            
            // Enable high-quality anti-aliasing for smooth rounded corners
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Create a rounded rectangle clipping mask
            RoundRectangle2D mask = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(),
                    cornerRadius, cornerRadius);
            g.clip(mask);
            
            if (image == null) {
                // Fallback gradient if the image fails to load
                g.setPaint(new GradientPaint(0, 0, new Color(0x08, 0x67, 0x8C),
                        getWidth(), getHeight(), new Color(0x16, 0x1A, 0x28)));
                g.fillRect(0, 0, getWidth(), getHeight());
            } else {
                drawCover(g, image, getWidth(), getHeight());
            }
            
            // Apply a subtle dark gradient wash to ensure white text placed over the image remains legible
            g.setComposite(AlphaComposite.SrcOver.derive(shade));
            Color wash = ViewStyles.BACKGROUND;
            g.setPaint(new GradientPaint(0, 0, new Color(wash.getRed(), wash.getGreen(), wash.getBlue(), 180),
                    getWidth(), 0, new Color(wash.getRed(), wash.getGreen(), wash.getBlue(), 18)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    /** 
     * Scales an image to completely fill its panel bounds, calculating the aspect ratio
     * to prevent aggressive vertical cropping on ultrawide or 1080p monitors.
     */
    private static void drawCover(Graphics2D g, BufferedImage image, int width, int height) {
        if (width <= 0 || height <= 0) return;
        
        // Calculate aspect ratios for smart scaling
        double panelRatio = (double) width / height;
        double imageRatio = (double) image.getWidth() / image.getHeight();
        
        int drawWidth, drawHeight;
        
        // Scale based on the dominant dimension to ensure the panel is filled without stretching
        if (panelRatio > imageRatio) {
            drawWidth = width;
            drawHeight = (int) (width / imageRatio);
        } else {
            drawHeight = height;
            drawWidth = (int) (height * imageRatio);
        }
        
        // Center the scaled image within the panel bounds
        int x = (width - drawWidth) / 2;
        int y = (height - drawHeight) / 2;
        
        // Force high-quality bicubic rendering for fullscreen upscaling
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        g.drawImage(image, x, y, drawWidth, drawHeight, null);
    }

    /** Chooses a bundled image based on the event's category. */
    /** Selects a bundled image according to the event's normalized category. */
    private static BufferedImage loadEventImage(Event event) {
        String category = event.getCategory() == null
                ? ""
                : event.getCategory().toLowerCase(Locale.ROOT);

        if (category.contains("tech")) return loadResource("/images/event-tech.jpg");
        if (category.contains("food")) return loadResource("/images/event-food.jpg");
        if (category.contains("night") || category.contains("dance")) {
            return loadResource("/images/event-nightlife.jpg");
        }
        return loadResource("/images/event-music.jpg");
    }

    /** Helper to load packaged images from the classpath JAR. */
    /** Reads one image resource from the application's classpath. */
    private static BufferedImage loadResource(String path) {
        try (var input = EventVisuals.class.getResourceAsStream(path)) {
            return input == null ? null : ImageIO.read(input);
        } catch (IOException exception) {
            return null;
        }
    }
}