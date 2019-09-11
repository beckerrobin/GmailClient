import javax.mail.MessagingException;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        GmailClient client = null;
        try {
            client = new GmailClient();
        } catch (MessagingException e) {
            e.printStackTrace();
            System.exit(1);
        }

        new ConnectForm(client);
        if (!(client.getUsername() == null || client.getUsername().isBlank() || client.getUsername().equals("Username"))) {
            try {
                client.connect();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
        EmailClientGUI gui = new EmailClientGUI(client);
        SwingUtilities.invokeLater(gui::show);
        if (client.isConnected())
            try {
                gui.populateMailList();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
    }
}
