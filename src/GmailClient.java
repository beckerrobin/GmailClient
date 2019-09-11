import com.sun.mail.imap.IMAPStore;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
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
    private final int smtpPort = 587;

    private IMAPStore imapStore;
    private Properties smtpProperties;

    private String username;
    private String password;
    private Set<Folder> openFolders = new HashSet<>();

    GmailClient() throws MessagingException {
//        Properties pop3Properties = new Properties();
//        pop3Properties.put("mail.pop3.host", this.pop3);
//        pop3Properties.put("mail.pop3.port", this.pop3Port);
//        pop3Properties.put("mail.pop3.ssl.enable", true);
//        pop3Properties.put("mail.pop3.finalizecleanclose", true);
//        pop3Properties.put("mail.pop3.rsetbeforequit", true);
        this.smtpProperties = new Properties();
        smtpProperties.put("mail.store.protocol", "smtp");
        smtpProperties.put("mail.smtp.host", this.smtpHost);
        smtpProperties.put("mail.smtp.port", this.smtpPort);
        smtpProperties.put("mail.smtp.auth", "true");
        smtpProperties.put("mail.smtp.starttls.enable", "true"); //TLS
        smtpProperties.put("mail.smtp.connectiontimeout", 180000);
        smtpProperties.put("mail.smtp.timeout", 10000);
        Properties imapProperties = new Properties();
        imapProperties.put("mail.store.protocol", "imaps");
        imapProperties.put("mail.imap.host", this.imapHost);
        imapProperties.put("mail.imap.port", this.imapPort);
        imapProperties.put("mail.imap.ssl.enable", true);
        imapProperties.put("mail.imap.connectiontimeout", 180000);
        imapProperties.put("mail.imap.timeout", 10000);

        Session imapSession = Session.getInstance(imapProperties);
//        imapSession.setDebug(true);
        this.imapStore = (IMAPStore) imapSession.getStore("imap");
    }

    void connect() throws MessagingException {
        for (int i = 0; i < 3; i++) {
            try {
                this.imapStore.connect(this.username, this.password);
                break;
            } catch (MessagingException e) {
                System.out.println("Could not connect: " + e.getMessage());
                System.out.println("Retrying...");
            }
        }
        for (Folder folder : imapStore.getDefaultFolder().list("*")) {
            System.out.println(folder);
        }
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    boolean isConnected() {
        return this.imapStore.isConnected();
    }

    boolean sendMail(String to, String subject, String body) {
        Session smtpSession = Session.getInstance(this.smtpProperties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
//        smtpSession.setDebug(true);
        MimeMessage message = new MimeMessage(smtpSession);
        try {
            message.setFrom(username);
            message.setRecipient(Message.RecipientType.TO, (new InternetAddress(to)));
//            message.addRecipients(Message.RecipientType.CC, InternetAddress.parse(String.join(",", cc)));
            message.setSubject(subject);
            message.setText(body);
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
        try {
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    void setParams(String username, char[] password) {
        this.username = username;
        this.password = String.valueOf(password);
    }

    Folder getOpenFolder(String folderName) throws MessagingException {
        Folder folder = this.imapStore.getFolder(folderName.toLowerCase());
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
        this.imapStore.close();
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

