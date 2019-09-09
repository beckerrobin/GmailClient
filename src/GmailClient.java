import com.sun.mail.pop3.POP3Store;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import java.util.Properties;

public class GmailClient {
    private final String pop3 = "pop.gmail.com";
    private final int pop3Port = 995;
    private final String smtp = "smtp.gmail.com";
    private final int smtpPort = 465;
    private POP3Store emailStore;
    private String username;
    private String password; // lnhrdmevuyvoybzg

    public GmailClient() throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.pop3.host", this.pop3);
        properties.put("mail.pop3.port", this.pop3Port);
        properties.put("mail.pop3.ssl.enable", true);
        Session emailSession = Session.getDefaultInstance(properties);
        this.emailStore = (POP3Store) emailSession.getStore("pop3");
    }
    public void connect() throws MessagingException {
        this.emailStore.connect(this.username, this.password);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setParams(String username, char[] password) {
        this.username = username;
        this.password = String.valueOf(password);
    }

    Folder readInbox() throws MessagingException {
        return emailStore.getFolder("INBOX");
    }
}
