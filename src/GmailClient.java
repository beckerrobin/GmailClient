import com.sun.mail.imap.IMAPStore;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static java.lang.Thread.sleep;

public class GmailClient {
    //    private final String pop3 = "pop.gmail.com";
//    private final int pop3Port = 995;
    private final String imapHost = "imap.gmail.com";
    private final int imapPort = 993;
    private final String smtpHost = "smtp.gmail.com";
    private final int smtpPort = 587;
    private Map<Folder, TreeSet<Mail>> folderMailMap = Collections.synchronizedMap(new HashMap<>());
    private IMAPStore imapStore;
    private Properties smtpProperties;
    private String username;
    private String password;

    private ExecutorService mailBodyES = Executors.newCachedThreadPool(); // Gets mail bodys
    private ExecutorService newMailES = Executors.newCachedThreadPool(); // Looks for new mail
    private ArrayList<FutureTask<Void>> newMailTasks = new ArrayList<>(); // Tasks to look for new mail, one task per folder
    private Stack<FetchMailBody> mailsToGet = new Stack<>();

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

    boolean connect() {
        System.out.println("Connecting...");
        try {
            this.imapStore.connect(this.username, this.password);
        } catch (MessagingException e) {
            System.out.println("Unable to connect: " + e.getMessage());
            return false;
        }

        // LIST ALL FOLDERS
        try {
            for (Folder folder : imapStore.getDefaultFolder().list("*")) {
                System.out.println(folder);
            }
        } catch (MessagingException e) {
            System.out.println("Could not get list of folders: " + e.getMessage());
            return false;
        }
        return true;
    }

    void init() {
        // OPEN INBOX
        Folder inboxFolder = getFolder("INBOX");

        // INITIALLY ADD LATEST 5 MAILS TO MAP
        fetchMails(inboxFolder, 2);

        // START LOOKING FOR NEW MAIL IN INBOX
        for (FutureTask<Void> newMailFuture : newMailTasks) {
            newMailES.submit(newMailFuture);
        }
    }

    void selectFolder(String folder) {

    }

    /**
     * Thread-safe to add new mails to Folder-Mail-Map
     *
     * @param folder: Key
     * @param mails:  Value
     */
    private void addMailsToMap(Folder folder, List<Mail> mails) {
        synchronized (this.folderMailMap.get(folder)) {
            this.folderMailMap.get(folder).addAll(mails);
        }
        for (Mail mail : mails) {
            mailsToGet.push(new FetchMailBody(mail, folder));
        }
        mailBodyES.submit(() -> {
            while (!mailsToGet.empty()) {
                mailBodyES.submit(mailsToGet.pop());
            }
        });
    }

    private void addMailToMap(Folder folder, Mail mail) {
        synchronized (this.folderMailMap.get(folder)) {
            this.folderMailMap.get(folder).add(mail);
        }
        mailBodyES.submit(new FetchMailBody(mail, folder));
    }

    private synchronized void addFolderMailMap(Folder folder, TreeSet<Mail> set) {
        this.folderMailMap.put(folder, set);
    }

    /**
     * Returns an array of the Mail-objects from specified folder that has already been fetched
     *
     * @param folder Folder to return Mail-objects from
     * @return A Mail-array
     */
    Mail[] getLocalMail(Folder folder) {
        return this.folderMailMap.get(folder).toArray(Mail[]::new);
    }

    String getUsername() {
        return username;
    }

    /**
     * Fetches all available mail from specified folder
     *
     * @param folder: The folder to check for mails
     * @throws MessagingException
     */
    private void fetchMails(Folder folder) throws MessagingException {
        fetchMails(folder, folder.getMessageCount());
    }

    /**
     * Fetches the latest amount of mail from specified folder. I.e. amount=5 will get the newest 5 messages from the specified folder
     *
     * @param folder From what folder to get the messages
     * @param amount How many messages to get
     * @throws MessagingException
     */
    private void fetchMails(Folder folder, int amount) {
        try {
            int messageCount = folder.getMessageCount();
            Message[] messages = folder.getMessages(messageCount - amount + 1, messageCount);
            for (Message message : messages) {
                addMailToMap(folder, new Mail(message));
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if IMAP connected
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
    boolean sendMail(String to, String subject, String body) {
        Session smtpSession = Session.getInstance(this.smtpProperties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
        smtpSession.setDebug(true);

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

    /**
     * Checks for new mail
     *
     * @param folder: Folder to check for new mail in
     * @return array of Message of the returned emails
     */
    private Message[] checkNewMail(Folder folder) throws MessagingException {
        if (!folder.isOpen())
            folder.open(Folder.READ_ONLY);
        int lastId = this.folderMailMap.get(folder).last().getId();
        return folder.getMessages(lastId + 1, folder.getMessageCount());
    }

    void setParams(String username, char[] password) {
        this.username = username;
        this.password = String.valueOf(password);
    }

    /**
     * Öppnar och returnerar en öppen emailmapp
     *
     * @param folderName: String
     * @return Returnerar en öppen Folder
     */
    synchronized Folder getFolder(String folderName) {
        Folder folder;
        try {
            folder = this.imapStore.getFolder(folderName.toLowerCase());
        } catch (MessagingException e) {
            System.out.println("Could not open folder: " + e.getMessage());
            return null;
        }

        Optional<Folder> folderExists = this.folderMailMap.keySet().stream().findFirst();

        if (folderExists.isPresent()) {
            // Folder opened before
            return this.folderMailMap.keySet().stream().findFirst().get();
        } else {
            // First time folder is opened
            System.out.println("Opening folder: " + folderName);
            try {
                folder.open(Folder.READ_ONLY);
            } catch (MessagingException e) {
                System.out.println("Could not open folder as Read-only: " + e.getMessage());
                return null;
            }
            addFolderMailMap(folder, new TreeSet<>(Comparator.comparingInt(Mail::getId)));

            // TODO: Gör detta generiskt för alla mappar
            newMailTasks.add(new FutureTask<>(() -> {
                while (true) {
                    try {
                        Message[] messages = checkNewMail(folder);
                        List<Mail> mails = new ArrayList<>();
                        for (Message message : messages) {
                            mails.add(new Mail(message));
                        }
                        addMailsToMap(folder, mails);

                    } catch (MessagingException ignored) {
                    }
                    sleep(5000); // How long to wait before checking for mail again, allowed to be interrupted
                }
            }));

            return folder;
        }
    }

    void close() throws MessagingException {
        System.out.println("Closing gmail client");

        if (!newMailTasks.isEmpty()) {
            System.out.println("Canceling check-for-new-mail tasks");
            newMailTasks.forEach(e -> {
                if (!e.isCancelled()) {
                    e.cancel(true);
                }
            });
        }

        System.out.println("Shutting down new mail exec-service");
        newMailES.shutdown();

        System.out.println("Shutting down mail body exec-service");
        mailBodyES.shutdown();

        System.out.println("Closing folders");
        for (Folder openFolder : this.folderMailMap.keySet()) {
            if (openFolder.isOpen()) {
                System.out.println("Closing " + openFolder.getFullName());
                openFolder.close();
            }
        }

        if (this.imapStore.isConnected()) {
            System.out.println("Closing IMAP store");
            this.imapStore.close();
        }
    }
}
