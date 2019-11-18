import javax.mail.MessagingException;
import javax.swing.*;

/**
 * Programmet kräver att man har ett Gmail-konto och att man tillåtit "Less Secure Apps".
 * Läs mer på https://myaccount.google.com/lesssecureapps
 */
public class Main {
    public static void main(String[] args) {
        GmailClient client = null; // Klient-objekt
        try {
            client = new GmailClient();
        } catch (MessagingException e) {
            e.printStackTrace();
            System.exit(1);
        }

        EmailClientGUI gui = new EmailClientGUI(client); // GUI-objekt
        SwingUtilities.invokeLater(gui::show); // Visa GUI så fort som möjligt men utan att blockera EDT, client.init() senare

        // Leta efter lagrade användaruppgifter, annars visa inloggningsruta
        if (!client.loadLoginInformation()) {
            new ConnectForm(client);
        }

        client.init(); // Initiera mailklienten
    }
}

