package views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The first screen: guests choose "Browse events" and hosts choose "Start hosting".
 *
 * OOP ideas shown here:
 * - Inheritance: WelcomePanel extends JPanel, so it IS a panel that can be shown in the window.
 * - Callbacks with Runnable: the constructor receives two actions from MainFrame. The panel just
 *   runs them when a button is clicked. It doesn't need to know which screen comes next.
 * - Private helper methods split building the screen into small, readable parts.
 */
public class WelcomePanel extends JPanel {
    public WelcomePanel(Runnable browseEvents, Runnable hostEventAction) {
        setLayout(new BorderLayout(0, 24));
        setBackground(new Color(0x0F, 0x11, 0x17));
        setBorder(new EmptyBorder(25, 38, 28, 38));
        // The slim header bar from the original design stays at the very top.
        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        // The big, centred SakaTickets brand sits at the top of the page.
        JPanel brand = buildBrand();
        brand.setAlignmentX(LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, brand.getPreferredSize().height));
        content.add(brand);
        content.add(Box.createVerticalStrut(26));
        JPanel choices = new JPanel(new GridLayout(1, 2, 18, 0));
        choices.setOpaque(false);
        choices.add(buildChoice("01   /   ATTEND", "Find your people.\nFind your next thing.",
            "Live music, food, culture and ideas worth leaving home for.", "Browse events",
            EventVisuals.buyerChoice(216, 12), ViewStyles.ACCENT, Color.WHITE, browseEvents));
        choices.add(buildChoice("02   /   CREATE", "Put your event\non the map.",
            "Build your event, shape ticket tiers, and follow every sale.", "Start hosting",
            EventVisuals.hostChoice(216, 12), new Color(0x00, 0xE5, 0xC4), new Color(0x0F, 0x11, 0x17),
                hostEventAction));
        choices.setPreferredSize(new Dimension(1000, 438));
            choices.setMaximumSize(new Dimension(Integer.MAX_VALUE, 438));
            choices.setAlignmentX(LEFT_ALIGNMENT);
            content.add(choices);
            content.add(Box.createVerticalGlue());
            JPanel footer = buildFooter();
            footer.setAlignmentX(LEFT_ALIGNMENT);
            content.add(footer);
        add(content, BorderLayout.CENTER);
    }

    // Builds the slim header bar: a small "SakaTickets" with "NAIROBI · LIVE EVENTS" under it
    // on the left, and the city's location on the right.
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        // A dividing line under the header separates it from the big brand below.
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(0, 0, 14, 0)));
        JPanel identity = new JPanel(new GridLayout(2, 1, 0, 2));
        identity.setOpaque(false);
        JLabel name = new JLabel("SakaTickets");
        name.setFont(ViewStyles.DISPLAY_FONT.deriveFont(Font.BOLD, 18f));
        name.setForeground(Color.WHITE);
        JLabel descriptor = new JLabel("NAIROBI  ·  LIVE EVENTS");
        descriptor.setForeground(new Color(0x00, 0xB4, 0xFF));
        descriptor.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 9f));
        identity.add(name);
        identity.add(descriptor);
        header.add(identity, BorderLayout.WEST);
        JLabel location = new JLabel("KENYA   /   01°17'S  36°49'E");
        location.setForeground(new Color(0x88, 0x96, 0xB3));
        location.setFont(ViewStyles.DATA_FONT.deriveFont(10f));
        header.add(location, BorderLayout.EAST);
        return header;
    }

    // Builds the centred brand block: a small line on top, the large "SakaTickets" name,
    // a short accent bar under it, and the tagline below.
    private JPanel buildBrand() {
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("GOOD THINGS HAPPEN HERE");
        eyebrow.setForeground(new Color(0x00, 0xE5, 0xC4));
        eyebrow.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 12f));

        // Swing labels understand simple HTML, so we can colour "Saka" and "Tickets" differently.
        JLabel name = new JLabel("<html><span style='color:#FFFFFF'>Saka</span>"
                + "<span style='color:#0D6EFD'>Tickets</span></html>");
        name.setFont(ViewStyles.DISPLAY_FONT.deriveFont(Font.BOLD, 72f));
        // HTML labels try to stretch across the whole width; keeping them at their own size lets them centre.
        name.setMaximumSize(name.getPreferredSize());

        // A short blue bar under the name, like an underline.
        JPanel accentBar = new JPanel();
        accentBar.setBackground(ViewStyles.ACCENT);
        accentBar.setPreferredSize(new Dimension(96, 5));
        accentBar.setMaximumSize(new Dimension(96, 5));

        JLabel tagline = new JLabel("Good things happen when you show up.");
        ViewStyles.styleHeading(tagline, 24f);
        JLabel description = new JLabel("Discover Nairobi events, book tickets, or bring your own idea to life.");
        ViewStyles.styleSecondaryText(description, 15f);

        // CENTER_ALIGNMENT lines every piece up down the middle of the page.
        eyebrow.setAlignmentX(CENTER_ALIGNMENT);
        name.setAlignmentX(CENTER_ALIGNMENT);
        accentBar.setAlignmentX(CENTER_ALIGNMENT);
        tagline.setAlignmentX(CENTER_ALIGNMENT);
        description.setAlignmentX(CENTER_ALIGNMENT);

        brand.add(eyebrow);
        brand.add(Box.createVerticalStrut(6));
        brand.add(name);
        brand.add(Box.createVerticalStrut(12));
        brand.add(accentBar);
        brand.add(Box.createVerticalStrut(18));
        brand.add(tagline);
        brand.add(Box.createVerticalStrut(6));
        brand.add(description);
        return brand;
    }

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
        words.add(javax.swing.Box.createVerticalStrut(7));
        words.add(heading);
        words.add(javax.swing.Box.createVerticalStrut(6));
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
        choice.setPreferredSize(new Dimension(520, 438));
        choice.setMinimumSize(new Dimension(300, 360));
        return choice;
    }

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