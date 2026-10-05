package views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class WelcomePanel extends JPanel {
    public WelcomePanel(Runnable browseEvents, Runnable hostEventAction) {
        setLayout(new BorderLayout(0, 24));
        setBackground(new Color(0x0F, 0x11, 0x17));
        setBorder(new EmptyBorder(25, 38, 28, 38));
        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 22));
        content.setOpaque(false);
        content.add(buildIntroduction(), BorderLayout.NORTH);
        JPanel choices = new JPanel(new GridLayout(1, 2, 18, 0));
        choices.setOpaque(false);
        choices.add(buildChoice("01   /   ATTEND", "Find your people.\nFind your next thing.",
                "Live music, food, culture and ideas worth leaving home for.", "Browse events  →",
                EventVisuals.buyerChoice(204, 12), ViewStyles.ACCENT, Color.WHITE, browseEvents));
        choices.add(buildChoice("02   /   CREATE", "Put your event\non the map.",
                "Build your event, shape ticket tiers, and follow every sale.", "Start hosting  →",
                EventVisuals.hostChoice(204, 12), new Color(0x00, 0xE5, 0xC4), new Color(0x0F, 0x11, 0x17),
                hostEventAction));
        content.add(choices, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
    }

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

    private JPanel buildIntroduction() {
        JPanel introduction = new JPanel(new BorderLayout(0, 8));
        introduction.setOpaque(false);
        JLabel eyebrow = new JLabel("GOOD THINGS HAPPEN HERE");
        eyebrow.setForeground(new Color(0x00, 0xE5, 0xC4));
        eyebrow.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 10f));
        JLabel heading = new JLabel("How do you want to show up?");
        ViewStyles.styleHeading(heading, 33f);
        JLabel description = new JLabel("Find your next unforgettable night, or make one happen.");
        ViewStyles.styleSecondaryText(description, 15f);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(eyebrow);
        copy.add(javax.swing.Box.createVerticalStrut(8));
        copy.add(heading);
        copy.add(javax.swing.Box.createVerticalStrut(4));
        copy.add(description);
        introduction.add(copy, BorderLayout.WEST);
        return introduction;
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

        JPanel copy = new JPanel(new BorderLayout(14, 8));
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
        choose.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        choose.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        choose.addActionListener(event -> action.run());
        copy.add(words, BorderLayout.CENTER);
        copy.add(choose, BorderLayout.EAST);
        choice.add(copy, BorderLayout.CENTER);
        choice.setMinimumSize(new Dimension(360, 320));
        return choice;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 1, 0, 1));
        JLabel left = new JLabel("MADE FOR NAIROBI NIGHTS, DAYS & EVERYTHING BETWEEN");
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