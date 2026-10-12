// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.AccountManager;          // Lists and deletes host accounts
import controllers.EventManager;            // Deletes a host's events
import controllers.OrderManager;            // Counts and reloads bookings
import java.awt.BorderLayout;               // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.Dimension;                  // A width and height
import java.awt.GridLayout;                 // Layout with equal-sized cells
import javax.swing.Box;                     // Invisible spacers
import javax.swing.BoxLayout;               // Stacks items in one column
import javax.swing.JButton;                 // A clickable button
import javax.swing.JComponent;              // The parent class of all Swing components
import javax.swing.JLabel;                  // A piece of text
import javax.swing.JOptionPane;             // Pop-up message and question boxes
import javax.swing.JPanel;                  // A container
import javax.swing.JPasswordField;          // A text box that hides what you type
import javax.swing.JScrollPane;             // Adds scroll bars
import models.Event;                        // An event
import models.User;                         // A host account

/**
 * The dashboard's "Settings" page.
 *
 *   - Every host sees their account details and a "Delete my account" button.
 *   - The main admin (Talel) also sees a list of all host accounts, each with a Delete button.
 *
 * Deleting a host also deletes their events and those events' bookings, so nothing is left
 * pointing at an account that no longer exists.
 *
 * OOP ideas shown here:
 * - Single responsibility: this class only handles account settings.
 * - Objects working together: deleting an account uses three controllers - EventManager removes the
 *   events, OrderManager reloads the bookings, and AccountManager removes the account.
 * - Callbacks: logoutAction and changedAction let the dashboard react without this page knowing how.
 */
class SettingsPage extends JPanel {
    private final AccountManager accountManager;
    private final EventManager eventManager;
    private final OrderManager orderManager;
    private final User host;                    // The host who is logged in
    private final Runnable logoutAction;        // Callback: log out (after deleting your own account)
    private final Runnable changedAction;       // Callback: events changed, refresh the dashboard
    private final JPanel hostList = new JPanel();   // The admin's list of host accounts (rebuilt by refresh())

    // Constructor: builds the page. The host list is only added for the main admin.
    SettingsPage(AccountManager accountManager, EventManager eventManager, OrderManager orderManager,
            User host, Runnable logoutAction, Runnable changedAction) {
        this.accountManager = accountManager;
        this.eventManager = eventManager;
        this.orderManager = orderManager;
        this.host = host;
        this.logoutAction = logoutAction;
        this.changedAction = changedAction;
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        // Card 1: who is signed in.
        JPanel details = new JPanel(new GridLayout(4, 1, 0, 6));
        details.setOpaque(false);
        details.add(grey("Name: " + host.getName()));
        details.add(grey("Username: " + host.getUsername()));
        details.add(grey("Email: " + host.getEmail()));
        details.add(grey("Account ID: " + host.getUserId()));
        addBlock(column, EventAnalyticsPanel.card("Host account", "Your account details", details));

        if (isMainAdmin()) {
            // Card 2 (admin only): every host account, each with a Delete button.
            hostList.setOpaque(false);
            hostList.setLayout(new BoxLayout(hostList, BoxLayout.Y_AXIS));
            addBlock(column, EventAnalyticsPanel.card("Host accounts",
                    "Deleting a host also deletes their events and bookings", hostList));
        } else {
            // Card 2 (other hosts): delete your own account.
            JButton deleteMine = new JButton("Delete my account");
            ViewStyles.styleSecondaryButton(deleteMine);
            deleteMine.addActionListener(action -> deleteOwnAccount());
            JPanel holder = new JPanel(new BorderLayout());
            holder.setOpaque(false);
            holder.add(deleteMine, BorderLayout.WEST);
            addBlock(column, EventAnalyticsPanel.card("Delete account",
                    "Removes your account, your events and their bookings. This cannot be undone.", holder));
        }

        // Putting the column in the NORTH of a BorderLayout keeps the cards at their natural
        // height at the top of the page, instead of stretching them to fill it.
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(column, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(top);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
        refresh();
    }

    // Rebuilds the admin's host list from the database (does nothing for other hosts).
    void refresh() {
        if (!isMainAdmin()) {
            return;
        }
        hostList.removeAll();
        for (User account : accountManager.getAllHosts()) {
            hostList.add(buildHostRow(account));
            hostList.add(Box.createVerticalStrut(8));
        }
        hostList.revalidate();
        hostList.repaint();
    }

    // True if the logged-in host is the built-in admin account.
    private boolean isMainAdmin() {
        return User.DEFAULT_ADMIN_ID.equals(host.getUserId());
    }

    // One row of the host list: name, username and email, how many events, and a Delete button.
    private JPanel buildHostRow(User account) {
        int eventCount = eventManager.getEventsByHostId(account.getUserId()).size();
        JLabel text = grey(account.getName() + "  (" + account.getUsername() + ")  ·  " + account.getEmail()
                + "  ·  " + eventCount + (eventCount == 1 ? " event" : " events"));
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(text, BorderLayout.CENTER);
        if (User.DEFAULT_ADMIN_ID.equals(account.getUserId())) {
            row.add(grey("Main admin"), BorderLayout.EAST);            // No Delete button for the admin
        } else {
            JButton delete = ViewStyles.smallButton("Delete");
            delete.addActionListener(action -> deleteOtherHost(account));
            row.add(delete, BorderLayout.EAST);
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    // ---- Deleting ----

    // The admin deletes another host, after confirming.
    private void deleteOtherHost(User account) {
        String question = "Delete the host account \"" + account.getUsername() + "\"?\n" + describeLosses(account)
                + "\nThis cannot be undone.";
        int answer = JOptionPane.showConfirmDialog(this, question, "Delete host",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            deleteHostAndEvents(account);
            changedAction.run();                // Refresh the dashboard's numbers
            refresh();                          // Rebuild this list
        }
    }

    // A host deletes their own account. They must type their password first, as a safety check.
    private void deleteOwnAccount() {
        JPasswordField passwordBox = new JPasswordField(16);
        // A message box can show any component; here it shows a label and a password box.
        Object[] message = {"This deletes your account.\n" + describeLosses(host)
                + "\nThis cannot be undone. Enter your password to confirm:", passwordBox};
        int answer = JOptionPane.showConfirmDialog(this, message, "Delete my account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;                                                     // Cancelled
        }
        String password = new String(passwordBox.getPassword());
        if (accountManager.authenticate(host.getUsername(), password).isEmpty()) {
            JOptionPane.showMessageDialog(this, "That password is incorrect. Your account was not deleted.",
                    "Delete my account", JOptionPane.ERROR_MESSAGE);
            return;
        }
        deleteHostAndEvents(host);
        JOptionPane.showMessageDialog(this, "Your account has been deleted.", "Account deleted",
                JOptionPane.INFORMATION_MESSAGE);
        logoutAction.run();                                             // Back to the welcome page
    }

    // The actual deleting, in the right order: events (and their bookings) first, then the account.
    private void deleteHostAndEvents(User account) {
        eventManager.deleteEventsByHost(account.getUserId());
        orderManager.reload();                  // Our list of bookings is now out of date
        accountManager.deleteHost(account.getUserId());
    }

    // Builds a line like "Their 3 events and 41 bookings will be deleted too."
    private String describeLosses(User account) {
        int events = 0;
        int bookings = 0;
        for (Event event : eventManager.getEventsByHostId(account.getUserId())) {
            events++;
            bookings += orderManager.getBookingsByEvent(event.getEventId()).size();
        }
        return events + (events == 1 ? " event" : " events") + " and " + bookings
                + (bookings == 1 ? " booking" : " bookings") + " will be deleted too.";
    }

    // ---- Small helpers ----

    // Adds a full-width block to the column with a gap after it.
    private void addBlock(JPanel column, JComponent block) {
        block.setAlignmentX(LEFT_ALIGNMENT);
        column.add(block);
        column.add(Box.createVerticalStrut(16));
    }

    // A grey label.
    private JLabel grey(String text) {
        JLabel label = new JLabel(text);
        ViewStyles.styleSecondaryText(label, 13f);
        return label;
    }
}
