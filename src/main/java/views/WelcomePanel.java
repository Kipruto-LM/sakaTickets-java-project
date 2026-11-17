package views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The initial landing page. It displays the two user choices and calls the
 * navigation callbacks supplied by MainFrame when either button is pressed.
 *
 * Core concepts:
 * - Inheritance: JPanel allows the welcome screen to be placed in MainFrame.
 * - Composition: the page is assembled from reusable Swing panels and controls.
 * - Encapsulation: artwork panels are private state used for responsive layout.
 * - Callbacks: Runnable parameters let MainFrame own navigation decisions.
 * - Polymorphism: ComponentListener handles window resize events through an override.
 */
public class WelcomePanel extends JPanel {
    private final JPanel buyerArtwork;
    private final JPanel hostArtwork;

    /**
     * Constructs the welcome screen and wires up the primary navigation callbacks.
     *
     * @param browseEvents callback executed when the user clicks to attend events
     * @param hostEventAction callback executed when the user clicks to host events
     */
    public WelcomePanel(Runnable browseEvents, Runnable hostEventAction) {
        setLayout(new BorderLayout(0, 24));
        setBackground(new Color(0x0F, 0x11, 0x17));
        setBorder(new EmptyBorder(25, 38, 28, 38));
        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel introduction = buildIntroduction();
        introduction.setAlignmentX(LEFT_ALIGNMENT);
        introduction.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                introduction.getPreferredSize().height));
        content.add(introduction);
        content.add(Box.createVerticalStrut(22));

        JPanel choices = new JPanel(new GridLayout(1, 2, 18, 0));
        choices.setOpaque(false);
        buyerArtwork = EventVisuals.buyerChoice(216, 12);
        hostArtwork = EventVisuals.hostChoice(216, 12);
        choices.add(buildChoice("01   /   ATTEND", "Find your people.\nFind your next thing.",
                "Live music, food, culture and ideas worth leaving home for.", "Browse events",
                buyerArtwork, ViewStyles.ACCENT, Color.WHITE, browseEvents));
        choices.add(buildChoice("02   /   CREATE", "Put your event\non the map.",
                "Build your event, shape ticket tiers, and follow every sale.", "Start hosting",
                hostArtwork, new Color(0x00, 0xE5, 0xC4), new Color(0x0F, 0x11, 0x17),
                hostEventAction));
        choices.setAlignmentX(LEFT_ALIGNMENT);
        choices.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        content.add(choices);
        content.add(Box.createVerticalGlue());

        JPanel footer = buildFooter();
        footer.setAlignmentX(LEFT_ALIGNMENT);
        content.add(footer);
        add(content, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            /** Resizes the large role images when the welcome screen changes size. */
            @Override
            public void componentResized(ComponentEvent event) {
                resizeRoleArtwork();
            }
        });
    }

    /**
     * Calculates proportional dimensions for the two role images.
     * This keeps their aspect ratio readable on wide desktop windows.
     */
    private void resizeRoleArtwork() {
        if (getWidth() <= 0) return;
        int cardWidth = Math.max(360, (getWidth() - 76 - 18) / 2);
        int imageHeight = Math.max(216, Math.min(500, (int) (cardWidth * 0.54)));
        buyerArtwork.setPreferredSize(new Dimension(480, imageHeight));
        hostArtwork.setPreferredSize(new Dimension(480, imageHeight));
        revalidate();
    }

    /** Constructs the top branding and location strip. */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel identity = new JPanel(new GridLayout(2, 1, 0, 2));
        identity.setOpaque(false);

        JLabel name = new JLabel("SakaTickets");
        name.setFont(ViewStyles.DISPLAY_FONT.deriveFont(java.awt.Font.BOLD, 22f));
        name.setForeground(Color.WHITE);
        JLabel descriptor = new JLabel("NAIROBI  ·  LIVE EVENTS");
        descriptor.setForeground(new Color(0x00, 0xB4, 0xFF));
        descriptor.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 9f));
        identity.add(name);
        identity.add(descriptor);
        header.add(identity, BorderLayout.WEST);

        JLabel location = new JLabel("KENYA   /   01°17′S  36°49′E");
        location.setForeground(new Color(0x88, 0x96, 0xB3));
        location.setFont(ViewStyles.DATA_FONT.deriveFont(10f));
        header.add(location, BorderLayout.EAST);
        return header;
    }

    /** Constructs the introductory mission statement text block. */
    private JPanel buildIntroduction() {
        JPanel introduction = new JPanel(new BorderLayout(0, 8));
        introduction.setOpaque(false);
        JLabel eyebrow = new JLabel("GOOD THINGS HAPPEN HERE");
        eyebrow.setForeground(new Color(0x00, 0xE5, 0xC4));
        eyebrow.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 10f));
        JLabel heading = new JLabel("Good things happen when you show up.");
        ViewStyles.styleHeading(heading, 33f);
        JLabel description = new JLabel(
                "Discover Nairobi events, book tickets, or bring your own idea to life.");
        ViewStyles.styleSecondaryText(description, 15f);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(eyebrow);
        copy.add(Box.createVerticalStrut(8));
        copy.add(heading);
        copy.add(Box.createVerticalStrut(4));
        copy.add(description);
        introduction.add(copy, BorderLayout.WEST);
        return introduction;
    }

    /**
     * Constructs a complete, styled role selection card.
     *
     * @param index visual numbering step
     * @param title primary card heading
     * @param description explanatory text
     * @param buttonText action button text
     * @param image image panel used by the card
     * @param buttonColor button background color
     * @param buttonTextColor button text color
     * @param action callback to run when the button is pressed
     */
    private JPanel buildChoice(String index, String title, String description, String buttonText,
            JPanel image, Color buttonColor, Color buttonTextColor, Runnable action) {
        JPanel choice = new JPanel(new BorderLayout(0, 0));
        choice.setBackground(new Color(0x17, 0x1B, 0x24));
        choice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(9, 9, 12, 9)));
        choice.putClientProperty("FlatLaf.style", "arc: 12");
        choice.add(image, BorderLayout.NORTH);

        JPanel copy = new JPanel(new BorderLayout(0, 10));
        copy.setOpaque(false);
        copy.setBorder(new EmptyBorder(15, 10, 2, 8));
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel(index);
        eyebrow.setForeground(new Color(0x00, 0xB4, 0xFF));
        eyebrow.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 10f));
        JLabel heading = new JLabel("<html>" + title.replace("\n", "<br>") + "</html>");
        ViewStyles.styleHeading(heading, 21f);
        JLabel explanation = new JLabel("<html>" + description + "</html>");
        ViewStyles.styleSecondaryText(explanation, 13f);
        words.add(eyebrow);
        words.add(Box.createVerticalStrut(7));
        words.add(heading);
        words.add(Box.createVerticalStrut(6));
        words.add(explanation);

        JButton choose = new JButton(buttonText);
        choose.setFont(ViewStyles.BODY_FONT.deriveFont(java.awt.Font.BOLD, 13f));
        choose.setForeground(buttonTextColor);
        choose.setBackground(buttonColor);
        choose.setOpaque(true);
        choose.setFocusPainted(false);
        choose.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
        choose.setPreferredSize(new Dimension(184, 44));
        choose.setMinimumSize(new Dimension(150, 44));
        choose.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        choose.addActionListener(event -> action.run());

        JPanel actionRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        actionRow.setOpaque(false);
        actionRow.add(choose);
        copy.add(words, BorderLayout.CENTER);
        copy.add(actionRow, BorderLayout.SOUTH);
        choice.add(copy, BorderLayout.CENTER);
        choice.setMinimumSize(new Dimension(300, 360));
        return choice;
    }

    /** Constructs the footer text block. */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 1, 0, 1));
        JLabel left = new JLabel("MADE FOR NAIROBI NIGHTS, DAYS AND EVERYTHING BETWEEN");
        left.setForeground(new Color(0x6F, 0x7E, 0x99));
        left.setFont(ViewStyles.DATA_FONT.deriveFont(9f));
        JLabel right = new JLabel("DISCOVER  ·  GATHER  ·  CREATE");
        right.setForeground(new Color(0x6F, 0x7E, 0x99));
        right.setFont(ViewStyles.DATA_FONT.deriveFont(9f));
        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }
}
