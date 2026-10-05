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
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import models.Event;

final class EventVisuals {
    private static final BufferedImage HERO = loadResource("/images/featured-concert.jpg");

    private EventVisuals() {
    }

    static JPanel hero(int height, int cornerRadius) {
        return imagePanel(HERO, height, cornerRadius, 0.16f);
    }

    static JPanel buyerChoice(int height, int cornerRadius) {
        return imagePanel(HERO, height, cornerRadius, 0.08f);
    }

    static JPanel hostChoice(int height, int cornerRadius) {
        return imagePanel(loadResource("/images/event-tech.jpg"), height, cornerRadius, 0.08f);
    }

    static JPanel event(Event event, int height, int cornerRadius) {
        return imagePanel(loadEventImage(event), height, cornerRadius, 0.08f);
    }

    private static JPanel imagePanel(BufferedImage image, int height, int cornerRadius, float shade) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                RoundRectangle2D mask = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(),
                        cornerRadius, cornerRadius);
                g.clip(mask);
                if (image == null) {
                    g.setPaint(new GradientPaint(0, 0, new Color(0x08, 0x67, 0x8C),
                            getWidth(), getHeight(), new Color(0x16, 0x1A, 0x28)));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    drawCover(g, image, getWidth(), getHeight());
                }
                g.setComposite(AlphaComposite.SrcOver.derive(shade));
                g.setPaint(new GradientPaint(0, 0, new Color(0x0F, 0x11, 0x17, 220),
                        getWidth(), 0, new Color(0x0F, 0x11, 0x17, 25)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(480, height));
        panel.setMinimumSize(new Dimension(180, height));
        return panel;
    }

    private static void drawCover(Graphics2D g, BufferedImage image, int width, int height) {
        if (width <= 0 || height <= 0) return;
        double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
        int drawWidth = (int) Math.ceil(image.getWidth() * scale);
        int drawHeight = (int) Math.ceil(image.getHeight() * scale);
        int x = (width - drawWidth) / 2;
        int y = (height - drawHeight) / 2;
        g.drawImage(image, x, y, drawWidth, drawHeight, null);
    }

    private static BufferedImage loadEventImage(Event event) {
        String customPath = event.getBannerImagePath();
        if (customPath != null && !customPath.isBlank()) {
            try {
                BufferedImage custom = ImageIO.read(new File(customPath));
                if (custom != null) return custom;
            } catch (IOException ignored) {
            }
        }
        String category = event.getCategory() == null ? "" : event.getCategory().toLowerCase(Locale.ROOT);
        String resource = category.contains("tech") ? "/images/event-tech.jpg"
                : category.contains("food") ? "/images/event-food.jpg"
                : category.contains("music") || category.contains("acoustic") ? "/images/event-music.jpg"
                : category.contains("night") || category.contains("dance") ? "/images/event-nightlife.jpg"
                : "/images/event-music.jpg";
        return loadResource(resource);
    }

    private static BufferedImage loadResource(String path) {
        try (var input = EventVisuals.class.getResourceAsStream(path)) {
            return input == null ? null : ImageIO.read(input);
        } catch (IOException exception) {
            return null;
        }
    }
}