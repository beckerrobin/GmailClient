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
        client.connect();
        EmailClientGUI gui = new EmailClientGUI(client);
        SwingUtilities.invokeLater(gui::show);

        if (client.isConnected())
            client.init();
    }
}

