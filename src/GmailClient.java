import com.sun.mail.imap.IMAPStore;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

// TODO: Logg to file, remove all SOUT

/**
 * Mailklient-klass. Använder IMAP efter att POP3 inte fungerade tillräckligt bra vid tester.
 */
class GmailClient {
    final static String LOGIN_FILE_PATH = "login";
    final static String CACHE_DIRECTORY = "cache/"; // Root-mapp för cache-objekt
    public static boolean canCache = false;
    static LocalTime programStart;
    static String username;
    //    private final String POP3_HOST = "pop.gmail.com";
    //    private final int POP3_PORT = 995;
    private final String IMAP_HOST = "imap.gmail.com";
    private final int IMAP_PORT = 993; // Google imap-port
    private final String SMTP_HOST = "smtp.gmail.com";
    private final int SMTP_PORT = 587; // Google smtp-port
    private Map<String, MailFolder> mailFolderMap = new LinkedHashMap<>();
    private IMAPStore imapStore;
    private Properties smtpProperties;
    private String gmailAddress;
    private String password;

    GmailClient() throws MessagingException {
//        Properties pop3Properties = new Properties();
//        pop3Properties.put("mail.pop3.host", this.pop3);
//        pop3Properties.put("mail.pop3.port", this.pop3Port);
//        pop3Properties.put("mail.pop3.ssl.enable", true);
//        pop3Properties.put("mail.pop3.finalizecleanclose", true);
//        pop3Properties.put("mail.pop3.rsetbeforequit", true);
        this.smtpProperties = new Properties();
        smtpProperties.put("mail.store.protocol", "smtp");
        smtpProperties.put("mail.smtp.host", this.SMTP_HOST);
        smtpProperties.put("mail.smtp.port", this.SMTP_PORT);
        smtpProperties.put("mail.smtp.auth", "true");
        smtpProperties.put("mail.smtp.starttls.enable", "true"); //TLS
        smtpProperties.put("mail.smtp.connectiontimeout", 180000);
        smtpProperties.put("mail.smtp.timeout", 10000);
        Properties imapProperties = new Properties();
        imapProperties.put("mail.store.protocol", "imaps");
        imapProperties.put("mail.imap.host", this.IMAP_HOST);
        imapProperties.put("mail.imap.port", this.IMAP_PORT);
        imapProperties.put("mail.imap.ssl.enable", true);
        imapProperties.put("mail.imap.connectiontimeout", 180000);
        imapProperties.put("mail.imap.timeout", 10000);

        Session imapSession = Session.getInstance(imapProperties);
//        imapSession.setDebug(true);
        this.imapStore = (IMAPStore) imapSession.getStore("imap");
    }

    /**
     * Returnerar MailFolder-objekt med angivet namn
     *
     * @param folderName Namnet på mailmappen
     */
    MailFolder getMailFolder(String folderName) {
        return mailFolderMap.get(folderName);
    }

    /**
     * Ladda sparade inloggningsuppgifter från disk.
     *
     * @return true om inloggningsuppgifter kunde laddas, annars false.
     * Garanterar inte att inloggningsuppgifterna är giltiga.
     */
    boolean loadLoginInformation() {
        File loginFile = new File(GmailClient.LOGIN_FILE_PATH);
        if (loginFile.exists() && loginFile.canRead()) {
            try (BufferedReader br = new BufferedReader(new FileReader(loginFile, StandardCharsets.UTF_16))) {
                String data = br.readLine();
                String[] stringArr = data.split(";");
                setParams(stringArr[0], stringArr[1]);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Anslut till Googles server
     *
     * @return true om anslutningen lyckades
     */
    boolean connect() {
        EmailClientGUI.startLoad();
        try {
            this.imapStore.connect(this.gmailAddress, this.password);
        } catch (MessagingException e) {
            System.out.println("Anslutningen misslyckades: " + e.getMessage());
            if (e.getMessage().contains("no password specified")) {
                new ConnectForm(this); // Om inget lösenord angavs, visa inloggningsruta
            }
            EmailClientGUI.stopLoad();
            return false;
        }
        return true;
    }

    MailBodyFetcher reloadMail(Mail mail) {
        return this.mailFolderMap.get(EmailClientGUI.selectedFolder).reloadMail(mail);
    }

    /**
     * Körs vid uppstart men efter GUI
     */
    void init() {
        if (!isConnected() && !connect())
            return;

        System.out.println("Starting time ");
        programStart = LocalTime.now();

        // Spara senaste inloggninguppgifterna som fungerade. Sparas i klartext.
        File loginFile = new File(LOGIN_FILE_PATH);
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(loginFile, StandardCharsets.UTF_16))) {
            printWriter.println(GmailClient.this.gmailAddress + ";" + GmailClient.this.password);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Skapa användar-cachemapp om denna inte finns
        File cacheFolder = new File(CACHE_DIRECTORY + GmailClient.username);
        canCache = (cacheFolder.mkdir() || cacheFolder.exists());

        // Ladda alla mappar från IMAP-store och ladda cache för varje mapp från disk om det finns
        try {
            for (Folder folder : imapStore.getDefaultFolder().list("*")) {
                System.out.println(folder);
                MailFolder newMailFolder = new MailFolder(folder);
                mailFolderMap.put(folder.getName(), newMailFolder);
            }
        } catch (MessagingException e) {
            System.out.println("Could not get list of folders: " + e.getMessage());
        }

        if (isConnected()) {
            initTest();
        }
        EmailClientGUI.stopLoad();
    }

    /**
     * Öppnar inboxen automatiskt
     */
    private void initTest() {
        // Öppna inboxen
        MailFolder inbox = this.mailFolderMap.get("INBOX");
        try {
            inbox.initFolder();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns an array of the Mail-objects that has already been fetched, from the specified folder,
     *
     * @param folderName Folder to return Mail-objects from
     * @return A Mail-array
     */
    Mail[] getMailArray(String folderName) {
        if (mailFolderMap.containsKey(folderName))
            return this.mailFolderMap.get(folderName).getMailSet().toArray(Mail[]::new);
        return new Mail[0];
    }

    String getGmailAddress() {
        return gmailAddress;
    }

    /**
     * Check if IMAP is connected
     *
     * @return true if imap-store is connected, else false
     */
    boolean isConnected() {
        return this.imapStore.isConnected();
    }

    /**
     * Send a new mail via SMTP
     *
     * @param to      Mail recepient
     * @param subject Mail subject
     * @param body    Mail body
     * @return return true if successful, else false
     */
    boolean sendMail(String to, String cc, String subject, String body) {
        Session smtpSession = Session.getInstance(this.smtpProperties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(gmailAddress, password);
                    }
                });
        smtpSession.setDebug(true);

        MimeMessage message = new MimeMessage(smtpSession);
        try {
            message.setFrom(gmailAddress);
            String[] toArr = to.split(";\\s*");
            if (toArr.length > 0)
                message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", toArr)));
            String[] ccArr = cc.split(";\\s*");
            if (ccArr.length > 0)
                message.addRecipients(Message.RecipientType.CC, InternetAddress.parse(String.join(",", ccArr)));
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

    /**
     * Sätter inloggningsparametrar, callback för ConnectForm.
     */
    void setParams(String username, String password) {
        this.gmailAddress = username;
        GmailClient.username = gmailAddress.substring(0, gmailAddress.indexOf("@"));
        this.password = String.valueOf(password);
    }

    /**
     * Körs vid nedstängning av programmet. Avbryter threads och stänger anslutningar
     */
    void close() throws MessagingException {
        System.out.println("Closing gmail client");
        System.out.println("Closing folders");

        for (MailFolder mailFolder : this.mailFolderMap.values()) {
            if (mailFolder.folder.isOpen()) {
                System.out.println("Closing " + mailFolder);
                mailFolder.close();
            }
        }

        if (this.imapStore.isConnected()) {
            System.out.println("Closing IMAP store");
            this.imapStore.close();
        }
    }
}
