// This file belongs to the "views" package: the screens (the V in MVC).
package views;

// Imports bring in the Java classes this file uses, so we can write "Color" instead of "java.awt.Color".
import java.awt.BorderLayout;    // Layout with NORTH, SOUTH, EAST, WEST and CENTER areas
import java.awt.Color;           // An RGB colour
import java.awt.Dimension;       // A width and height pair, used for sizes
import java.awt.Font;            // Font style constants such as Font.BOLD
import java.awt.GridLayout;      // Layout that puts components in equal-sized rows/columns
import javax.swing.BorderFactory; // Helper for making borders (lines, padding)
import javax.swing.Box;          // Helper for making invisible spacers
import javax.swing.BoxLayout;    // Layout that stacks components in one column or row
import javax.swing.JButton;      // A clickable button
import javax.swing.JLabel;       // A piece of text on screen
import javax.swing.JPanel;       // A container that holds other components
import javax.swing.border.EmptyBorder; // Invisible padding around a component

/**
 * The first screen: guests choose "Browse events" and hosts choose "Start hosting".
 *
 * Layout from top to bottom:
 *   1. Header bar   - small SakaTickets logo on the left, location on the right.
 *   2. Brand block  - the big centred "SakaTickets" name and the tagline.
 *   3. Two choices  - an "Attend" card and a "Create" card, each with a picture and a button.
 *   4. Footer       - two small lines of text.
 *
 * OOP ideas shown here:
 * - Inheritance: WelcomePanel extends JPanel, so it IS a panel that can be shown in the window.
 *   It gets methods like setLayout(), setBackground() and add() from JPanel for free.
 * - Callbacks with Runnable: the constructor receives two actions from MainFrame. The panel just
 *   runs them when a button is clicked. It doesn't need to know which screen comes next.
 * - Private helper methods split building the screen into small, readable parts
 *   (buildHeader, buildBrand, buildChoice, buildFooter). "private" means only this class can call them.
 */
public class WelcomePanel extends JPanel {

    // Constructor: runs once when MainFrame writes "new WelcomePanel(...)".
    // browseEvents    = what to do when "Browse events" is clicked (MainFrame shows the catalogue).
    // hostEventAction = what to do when "Start hosting" is clicked (MainFrame shows the host login).
    public WelcomePanel(Runnable browseEvents, Runnable hostEventAction) {
        setLayout(new BorderLayout(0, 24));              // BorderLayout with a 24px gap between areas
        setBackground(new Color(0x0F, 0x11, 0x17));      // Very dark blue-black page background
        setBorder(new EmptyBorder(25, 38, 28, 38));      // Padding: top, left, bottom, right
        // The slim header bar from the original design stays at the very top.
        add(buildHeader(), BorderLayout.NORTH);

        // "content" holds everything under the header, stacked top to bottom.
        JPanel content = new JPanel();
        content.setOpaque(false);                         // Transparent, so the page background shows
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS)); // Y_AXIS = stack vertically

        // The big, centred SakaTickets brand sits at the top of the page.
        JPanel brand = buildBrand();
        brand.setAlignmentX(LEFT_ALIGNMENT);              // Line it up with the other full-width blocks
        // Let it stretch to any width, but never taller than it needs to be.
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, brand.getPreferredSize().height));
        content.add(brand);
        content.add(Box.createVerticalStrut(26));         // 26px of empty space below the brand

        // Two equal cards side by side: 1 row, 2 columns, 18px gap between them.
        JPanel choices = new JPanel(new GridLayout(1, 2, 18, 0));
        choices.setOpaque(false);
        // Card 1, for guests. Clicking its button runs browseEvents.
        choices.add(buildChoice("01   /   ATTEND", "Find your people.\nFind your next thing.",
            "Live music, food, culture and ideas worth leaving home for.", "Browse events",
            EventVisuals.buyerChoice(216), ViewStyles.ACCENT, Color.WHITE, browseEvents));
        // Card 2, for hosts. A teal button with dark text. Clicking it runs hostEventAction.
        choices.add(buildChoice("02   /   CREATE", "Put your event\non the map.",
            "Build your event, shape ticket tiers, and follow every sale.", "Start hosting",
            EventVisuals.hostChoice(216), new Color(0x00, 0xE5, 0xC4), new Color(0x0F, 0x11, 0x17),
            hostEventAction));
        choices.setPreferredSize(new Dimension(1000, 438));                  // Ideal size of the card row
        choices.setMaximumSize(new Dimension(Integer.MAX_VALUE, 438));       // Any width, max 438px tall
        choices.setAlignmentX(LEFT_ALIGNMENT);
        content.add(choices);

        content.add(Box.createVerticalGlue());            // "Glue" soaks up spare space, pushing the footer down
        JPanel footer = buildFooter();
        footer.setAlignmentX(LEFT_ALIGNMENT);
        content.add(footer);

        add(content, BorderLayout.CENTER);                // CENTER takes all the space left under the header
    }

    // Builds the slim header bar: a small "SakaTickets" with "NAIROBI · LIVE EVENTS" under it
    // on the left, and the city's location on the right.
    // Returns a new JPanel; the constructor places it in the NORTH (top) area.
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());   // WEST = logo, EAST = location
        header.setOpaque(false);                           // Transparent background
        // A compound border joins two borders: a 1px line along the bottom (the divider)
        // plus 14px of padding above that line.
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2A, 0x30, 0x44)), // top, left, bottom, right
                new EmptyBorder(0, 0, 14, 0)));

        // "identity" stacks the small logo text and the line under it: 2 rows, 1 column.
        JPanel identity = new JPanel(new GridLayout(2, 1, 0, 2));
        identity.setOpaque(false);
        JLabel name = new JLabel("SakaTickets");                           // Small logo text
        name.setFont(ViewStyles.DISPLAY_FONT.deriveFont(Font.BOLD, 18f));  // deriveFont = same font, new style/size
        name.setForeground(Color.WHITE);                                   // Text colour
        JLabel descriptor = new JLabel("NAIROBI  ·  LIVE EVENTS");         // Line under the logo
        descriptor.setForeground(new Color(0x00, 0xB4, 0xFF));             // Light blue
        descriptor.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 9f));
        identity.add(name);                                                // Row 1
        identity.add(descriptor);                                          // Row 2
        header.add(identity, BorderLayout.WEST);                           // Left side of the header

        JLabel location = new JLabel("KENYA   /   01°17'S  36°49'E");     // Nairobi's map coordinates
        location.setForeground(new Color(0x88, 0x96, 0xB3));               // Muted grey-blue
        location.setFont(ViewStyles.DATA_FONT.deriveFont(10f));
        header.add(location, BorderLayout.EAST);                           // Right side of the header
        return header;                                                     // Hand the finished panel back
    }

    // Builds the centred brand block: a small line on top, the large "SakaTickets" name,
    // a short accent bar under it, and the tagline below.
    private JPanel buildBrand() {
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));           // Stack the pieces vertically

        // Small teal line above the name (designers call this an "eyebrow").
        JLabel eyebrow = new JLabel("GOOD THINGS HAPPEN HERE");
        eyebrow.setForeground(new Color(0x00, 0xE5, 0xC4));                // Teal
        eyebrow.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 12f));

        // Swing labels understand simple HTML, so we can colour "Saka" and "Tickets" differently.
        JLabel name = new JLabel("<html><span style='color:#FFFFFF'>Saka</span>"   // "Saka" in white
                + "<span style='color:#0D6EFD'>Tickets</span></html>");            // "Tickets" in brand blue
        name.setFont(ViewStyles.DISPLAY_FONT.deriveFont(Font.BOLD, 72f));          // Very large and bold
        // HTML labels try to stretch across the whole width; keeping them at their own size lets them centre.
        name.setMaximumSize(name.getPreferredSize());

        // A short blue bar under the name, like an underline.
        JPanel accentBar = new JPanel();                    // An empty panel is just a coloured rectangle
        accentBar.setBackground(ViewStyles.ACCENT);         // Brand blue
        accentBar.setPreferredSize(new Dimension(96, 5));   // 96px wide, 5px tall
        accentBar.setMaximumSize(new Dimension(96, 5));     // Stops BoxLayout stretching it wider

        JLabel tagline = new JLabel("Good things happen when you show up.");
        ViewStyles.styleHeading(tagline, 24f);              // Shared heading style from ViewStyles
        JLabel description = new JLabel("Discover Nairobi events, book tickets, or bring your own idea to life.");
        ViewStyles.styleSecondaryText(description, 15f);    // Shared grey text style

        // CENTER_ALIGNMENT lines every piece up down the middle of the page.
        eyebrow.setAlignmentX(CENTER_ALIGNMENT);
        name.setAlignmentX(CENTER_ALIGNMENT);
        accentBar.setAlignmentX(CENTER_ALIGNMENT);
        tagline.setAlignmentX(CENTER_ALIGNMENT);
        description.setAlignmentX(CENTER_ALIGNMENT);

        // Add everything top to bottom. Each "strut" is a fixed gap in pixels.
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

    // Builds one choice card (picture on top, words and a button underneath).
    // It is called twice, once for "Attend" and once for "Create", so the card code is written
    // only once and reused with different values - this is code reuse through method parameters.
    //   index           - small label such as "01 / ATTEND"
    //   title           - big heading; "\n" marks where the line should break
    //   description     - one sentence under the heading
    //   buttonText      - the words on the button
    //   image           - the picture panel made by EventVisuals
    //   buttonColor     - button background colour
    //   buttonTextColor - button text colour
    //   action          - the callback to run when the button is clicked
    private JPanel buildChoice(String index, String title, String description, String buttonText,
            JPanel image, Color buttonColor, Color buttonTextColor, Runnable action) {
        JPanel choice = new JPanel(new BorderLayout(0, 0));       // The card itself
        choice.setBackground(new Color(0x17, 0x1B, 0x24));        // Slightly lighter than the page
        // Thin outline around the card, plus padding inside it.
        choice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(9, 9, 12, 9)));
        choice.putClientProperty("FlatLaf.style", "arc: 12");     // Asks the FlatLaf theme for rounded corners
        choice.add(image, BorderLayout.NORTH);                    // Picture across the top of the card

        // "copy" is the text area under the picture: words in the middle, button at the bottom.
        JPanel copy = new JPanel(new BorderLayout(0, 10));
        copy.setOpaque(false);
        copy.setBorder(new EmptyBorder(15, 10, 2, 8));

        // "words" stacks the three pieces of text vertically.
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel(index);                                   // e.g. "01 / ATTEND"
        eyebrow.setForeground(new Color(0x00, 0xB4, 0xFF));                   // Light blue
        eyebrow.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 10f));
        // JLabel can't show "\n" line breaks, but it understands HTML, so "\n" becomes "<br>".
        JLabel heading = new JLabel("<html>" + title.replace("\n", "<br>") + "</html>");
        ViewStyles.styleHeading(heading, 21f);
        // Wrapping text in <html> also lets a long sentence wrap onto a second line.
        JLabel explanation = new JLabel("<html>" + description + "</html>");
        ViewStyles.styleSecondaryText(explanation, 13f);
        words.add(eyebrow);
        words.add(Box.createVerticalStrut(7));                                // 7px gap
        words.add(heading);
        words.add(Box.createVerticalStrut(6));                                // 6px gap
        words.add(explanation);

        // The button. Its colours come from the parameters, so each card can look different.
        JButton choose = new JButton(buttonText);
        choose.setFont(ViewStyles.BODY_FONT.deriveFont(Font.BOLD, 13f));
        choose.setForeground(buttonTextColor);                    // Text colour
        choose.setBackground(buttonColor);                        // Fill colour
        choose.setOpaque(true);                                   // Actually paint the fill colour
        choose.setFocusPainted(false);                            // No dotted focus box after clicking
        choose.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16)); // Padding inside the button
        choose.setPreferredSize(new Dimension(184, 44));          // Ideal size
        choose.setMinimumSize(new Dimension(150, 44));            // Never squeeze smaller than this
        // Show the pointing-hand cursor when the mouse is over the button.
        choose.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        // Callback: when clicked, run the action MainFrame gave us. "event ->" is a lambda,
        // a short way to write a tiny method that Swing calls on each click.
        choose.addActionListener(event -> action.run());

        // A FlowLayout row keeps the button at its own size, on the left, instead of stretching.
        JPanel actionRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        actionRow.setOpaque(false);
        actionRow.add(choose);
        copy.add(words, BorderLayout.CENTER);                     // Text fills the middle
        copy.add(actionRow, BorderLayout.SOUTH);                  // Button sits at the bottom
        choice.add(copy, BorderLayout.CENTER);                    // Text area under the picture
        choice.setPreferredSize(new Dimension(520, 438));         // Ideal card size
        choice.setMinimumSize(new Dimension(300, 360));           // Smallest allowed card size
        return choice;
    }

    // Builds the footer: one short line on the left, one on the right, in small muted text.
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 1, 0, 1));                       // 12px space above the text
        JLabel left = new JLabel("MADE FOR NAIROBI NIGHTS, DAYS AND EVERYTHING BETWEEN");
        left.setForeground(new Color(0x6F, 0x7E, 0x99));                      // Muted grey-blue
        left.setFont(ViewStyles.DATA_FONT.deriveFont(9f));                    // Small monospace text
        JLabel right = new JLabel("DISCOVER  ·  GATHER  ·  CREATE");
        right.setForeground(new Color(0x6F, 0x7E, 0x99));
        right.setFont(ViewStyles.DATA_FONT.deriveFont(9f));
        footer.add(left, BorderLayout.WEST);                                  // Left side
        footer.add(right, BorderLayout.EAST);                                 // Right side
        return footer;
    }
}