import com.sun.mail.pop3.POP3Store;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailClient {
    private final String pop3 = "pop.gmail.com";
    private final int pop3Port = 995;
    private final String smtp = "smtp.gmail.com";
    private final int smtpPort = 465;
    private POP3Store emailStore;
    private String username;
    private String password; // lnhrdmevuyvoybzg

    public static void main(String[] args) {
        EmailClient client = new EmailClient();
        new ConnectForm(client);

        if (client.username == null || client.username.isBlank())
            System.exit(0);

        Properties properties = new Properties();
        properties.put("mail.pop3.host", client.pop3);
        properties.put("mail.pop3.port", client.pop3Port);
        properties.put("mail.pop3.ssl.enable", true);
        Session emailSession = Session.getDefaultInstance(properties);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            client.emailStore = (POP3Store) emailSession.getStore("pop3");
            client.emailStore.connect(client.username, client.password);
            EmailClientGUI gui = new EmailClientGUI();
            gui.populateMailList(client.readInbox());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void setParams(String username, char[] password) {
        this.username = username;
        this.password = String.valueOf(password);
    }

    Folder readInbox() throws MessagingException {
        return emailStore.getFolder("INBOX");
    }
}
