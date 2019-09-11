import com.sun.mail.imap.IMAPStore;

import javax.mail.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class GmailClient {
    //    private final String pop3 = "pop.gmail.com";
//    private final int pop3Port = 995;
    private final String imapHost = "imap.gmail.com";
    private final int imapPort = 993;
    private final String smtpHost = "smtp.gmail.com";
    private final int smtpPort = 465;

    private IMAPStore emailStore;
    private String username;
    private String password;
    private Set<Folder> openFolders = new HashSet<>();

    GmailClient() throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imap.host", this.imapHost);
        properties.put("mail.imap.port", this.imapPort);
//        properties.put("mail.pop3.host", this.pop3);
//        properties.put("mail.pop3.port", this.pop3Port);
//        properties.put("mail.pop3.ssl.enable", true);
//        properties.put("mail.pop3.finalizecleanclose", true);
//        properties.put("mail.pop3.rsetbeforequit", true);
        properties.put("mail.imap.ssl.enable", true);
        properties.put("mail.imap.connectiontimeout", 180000);
        properties.put("mail.imap.timeout", 10000);

        Session emailSession = Session.getInstance(properties);

        //        emailSession.setDebug(true);

        this.emailStore = (IMAPStore) emailSession.getStore("imap");
    }

    void connect() throws MessagingException {
        for (int i = 0; i < 3; i++) {
            try {
                this.emailStore.connect(this.username, this.password);
                break;
            } catch (MessagingException e) {
                System.out.println("Could not connect: " + e.getMessage());
                System.out.println("Retrying...");
            }
        }
        for (Folder folder : emailStore.getDefaultFolder().list("*")) {
            System.out.println(folder);
        }
    }

    String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    boolean isConnected() {
        return this.emailStore.isConnected();
    }

    void setParams(String username, char[] password) {
        this.username = username;
        this.password = String.valueOf(password);
    }

    Folder getOpenFolder(String folderName) throws MessagingException {
        Folder folder = this.emailStore.getFolder(folderName.toLowerCase());
        folder.open(Folder.READ_ONLY);
        this.openFolders.add(folder);
        return folder;
    }

    void close() throws MessagingException {
        for (Folder openFolder : this.openFolders) {
            System.out.println("Closing " + openFolder.getFullName());
            openFolder.close();
        }
        this.openFolders.clear();
        this.emailStore.close();
    }

    String getMailBody(int msgid, Folder folder) throws MessagingException, IOException {
        Message msg = folder.getMessage(msgid);

        Object content = msg.getContent();
        if (content instanceof Multipart) {
            StringBuilder messageContent = new StringBuilder();
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                Part part = multipart.getBodyPart(i);
                System.out.println(msgid + ": (" + i + "/" + multipart.getCount() + ") " + part.getContentType());
                if ((part.isMimeType("text/plain")) || (multipart.getCount() == 1 && part.isMimeType("text/html"))) {
                    messageContent.append(part.getContent().toString());
                }
            }
            return messageContent.toString();
        } else {
            return content.toString();
        }
    }
}

class SetMailBody implements Runnable {
    private Mail mail;
    private Folder folder;
    private GmailClient gmailClient;

    SetMailBody(Mail mail, Folder folder, GmailClient gmailClient) {
        this.mail = mail;
        this.folder = folder;
        this.gmailClient = gmailClient;
    }

    @Override
    public void run() {
        String msg;
        try {
            msg = this.gmailClient.getMailBody(this.mail.getId(), this.folder);
            mail.setBody(msg);
            System.out.println("Hämtat body för " + this.mail.getId());
        } catch (MessagingException | IOException e) {
            System.out.println("Error för " + this.mail.getId());
            e.printStackTrace();
        }
    }
}

