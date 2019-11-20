import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import java.io.*;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.Thread.sleep;

/**
 * Klass som representerar en mail-mapp. Innehåller ett Set med alla Mail-objekt.
 * Hämtar Mail-body för alla ingående mail.
 */
public class MailFolder {
    private final LinkedHashSet<Mail> mailSet = new LinkedHashSet<>();
    Folder folder;
    private Runnable getNewMailTask;
    private ExecutorService mailBodyES = Executors.newCachedThreadPool(); // Gets mail bodys
    private ExecutorService newMailES = Executors.newSingleThreadExecutor(); // Bakgrundstråd som kollar efter nya mail
    private ExecutorService mailObjectCreator = Executors.newFixedThreadPool(4);
    private File cacheSubFolder;
    private boolean canCacheThisFolder = false;

    MailFolder(Folder folder) {
        this.folder = folder;
        this.folder.addMessageCountListener(new MessageCountListener() {
            @Override
            public void messagesAdded(MessageCountEvent messageCountEvent) {

            }

            @Override
            public void messagesRemoved(MessageCountEvent messageCountEvent) {
                try {
                    folder.expunge();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        });

        // Om programmet kan spara cachefilder, skapa cache-mapå för denna mail-mapp
        if (GmailClient.canCache) {
            this.cacheSubFolder = new File(GmailClient.CACHE_DIRECTORY + GmailClient.username +
                    File.separator + folder.toString() + File.separator);
            if (this.cacheSubFolder.exists() || this.cacheSubFolder.mkdir()) {
                canCacheThisFolder = true;
                if (this.cacheSubFolder.exists()) {
                    // Ladda cache
                    loadCache();
                }
            } else {
                System.out.println("Kan inte skapa lokal cache för mapp " + folder.getName());
            }
        }

        // Bakgrundstråd som letar efter nya mail
        this.getNewMailTask = new FutureTask<>(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        ArrayList<Mail> mailArrayList = getSortedMailArray();
                        int lastId = mailArrayList.get(mailArrayList.size() - 1).getMessageNumber();

                        // Hämta alla mail som finns online och som har högre id än senaste
                        Message[] messages = folder.getMessages(lastId + 1, folder.getMessageCount());
                        for (Message message : messages) {
                            addMailObject(new Mail(message));
                        }
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                    try {
                        sleep(5000); // How long to wait before checking for mail again, allowed to be interrupted
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }, null);
    }

    private ArrayList<Mail> getSortedMailArray() {
        ArrayList<Mail> mailArrayList;
        synchronized (this.mailSet) {
            mailArrayList = new ArrayList<>(this.mailSet);
        }
        mailArrayList.sort(Comparator.comparing(Mail::getDate));
        return mailArrayList;
    }

    public File getCacheSubFolder() {
        return cacheSubFolder;
    }

    Message fetchMessage(int id) throws MessagingException {
        return folder.getMessage(id);
    }

    /**
     * Läs in alla lokalt cachade mailfiler till mailSet
     */
    private void loadCache() {
        EmailClientGUI.startLoad();
        File[] cachedMailFiles = cacheSubFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".moj"));
        if (cachedMailFiles != null) {
            for (File cachedMailFile : cachedMailFiles) {
                try (FileInputStream fileInputStream = new FileInputStream(cachedMailFile);
                     ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                    Mail cachedMail;
                    try {
                        Object object = objectInputStream.readObject();
                        if (object instanceof Mail) {
                            cachedMail = (Mail) object;
                        } else {
                            System.out.println("Error: Inte ett mail-objekt: " + cachedMailFile.getAbsolutePath());
                            continue;
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        continue;
                    }
                    synchronized (mailSet) {
                        mailSet.add(cachedMail);
                    }
                    System.out.println("Laddar mail från cache: " + cachedMail.getSubject());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        EmailClientGUI.stopLoad();
    }

    synchronized Set<Mail> getMailSet() {
        return mailSet;
    }

    /**
     * Initiera MailFolder-objektet
     */
    void initFolder() throws MessagingException {
        System.out.println("Opening folder: " + folder.getName());
        // Öppna folder
        folder.open(Folder.READ_WRITE); // Gmail stödjer flera writers samtidigt

        // Börja gå igenom mailen och uppdatera
        int messageCount = getMessageCount();
        if (messageCount > 0 && !mailSet.isEmpty()) {
            // Börja tråd som letar nya mail
            newMailES.submit(getNewMailTask);
        } else if (messageCount == 0) {
            // Foldern finns men är tom
            newMailES.submit(new Runnable() {
                @Override
                public void run() {
                    int messageCount = getMessageCount();
                    while (messageCount <= 0) {
                        try {
                            sleep(10000);
                        } catch (InterruptedException ignored) {
                        }
                        messageCount = getMessageCount();
                    }

                    try {
                        for (Message message : folder.getMessages()) {
                            addMailObject(new Mail(message));
                        }
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                    newMailES.submit(getNewMailTask);
                }
            });
        }

        // Hämta alla mail (även redan hämtade), filtrera bort de mail ur cachen som inte längre är aktuella
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                EmailClientGUI.startLoad();
                Message[] messagesArray;
                try {
                    // Hämtar array med Message, i ordningen äldst först.
                    messagesArray = MailFolder.this.folder.getMessages();
                } catch (MessagingException e) {
                    e.printStackTrace();
                    return;
                }
                EmailClientGUI.stopLoad();
                if (messagesArray.length == 0)
                    return;
                System.out.println(messagesArray.length + " mail hämtade sammanlagt.");

                // Om fler än 5 nya mail, visa laddningsbar
                boolean loadsOfMailToLoad = Math.abs(messagesArray.length - MailFolder.this.mailSet.size()) > 5;
                if (loadsOfMailToLoad) {
                    EmailClientGUI.startLoad();
                }

                ArrayList<Future<Mail>> futureTasks = new ArrayList<>();
                ArrayList<Message> messsageArrayList = new ArrayList<>(Arrays.asList(messagesArray));
                Collections.reverse(messsageArrayList); // Vänd ordning på listan, börja skapa mail-objekt av de senaste mailen först

                /*
                 Skapandet av ett Mail objekt kan ta någon sekund per mail.
                 Därför skapas alla objekten i separata trådar.
                */
                for (Message message : messsageArrayList) {
                    futureTasks.add(mailObjectCreator.submit(() -> {
                        Mail mail;
                        try {
                            mail = new Mail(message);
                            return mail;
                        } catch (MessagingException e) {
                            System.out.println("e:" + e.getMessage());
                        }
                        return null;
                    }));
                }

                Set<Mail> onlineMails = new HashSet<>(); // Används för att rensa ut gamla mail nedan
                Set<Mail> localMailsToBeRemoved = new HashSet<>(mailSet); // Används för att rensa ut gamla mail nedan

                // Lägg till mail (som inte redan laddats från cachen) till MailFoldern genom addMail()
                for (Future<Mail> futureTask : futureTasks) {
                    try {
                        Mail mail = futureTask.get();
                        if (mail != null) {
                            /* Eftersom mailSet är ett Set kommer endast mail läggas till om dessa inte redan finns i
                             settet baserat på Mail.equals() */
                            MailFolder.this.addMailObject(futureTask.get());
                            onlineMails.add(mail);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                if (loadsOfMailToLoad) {
                    EmailClientGUI.stopLoad();
                }

                // Rensa gamla mail (som inte längre finns online) från cache
                localMailsToBeRemoved.removeAll(onlineMails); // Sortera ut cachade mail som inte finns online
                localMailsToBeRemoved.forEach(MailFolder.this::removeMailObject);
                System.out.println("Inbox load time: " + ((LocalTime.now().toNanoOfDay() - GmailClient.programStart.toNanoOfDay()) / 1000000) + "ms");
            }
        });
    }

    /**
     * Hämta antalet mail i mailmappen
     */
    private int getMessageCount() {
        try {
            return folder.getMessageCount();
        } catch (MessagingException e) {
            System.out.println("Failed to get message count: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Tar bort (endast lokalt) ett mail, även den cachade filen raderas.
     */
    void removeMailObject(Mail mail) {
        System.out.println("Tar bort mail " + mail.getSubject());
        synchronized (mailSet) {
            mailSet.remove(mail);
        }
        File cacheFile = new File(cacheSubFolder, Integer.toUnsignedString(mail.hashCode()) + ".moj");
        cacheFile.delete();
    }

    /**
     * Lägga till mail till MailFolder, om mailet inte redan existerar i mailSet. Startar tråden som hämtar Body.
     */
    private void addMailObject(Mail mail) {
        boolean isNew;
        synchronized (mailSet) {
            isNew = mailSet.add(mail);
        }

        if (isNew) {
            // Skapa tråd som hämtar body och sedan cachar mailet
            mailBodyES.submit(new MailBodyFetcher(mail, this));
        }
    }

    /**
     * Laddar om mail-body
     */
    MailBodyFetcher reloadMail(Mail mail) {
        System.out.println("Laddar om " + mail.getSubject());
        return new MailBodyFetcher(mail, this);
    }

    /**
     * Callback fär FetchMailBody
     */
    void cacheMail(Mail mail) {
        if (canCacheThisFolder) {
            try {
                File cacheFile = new File(cacheSubFolder, Integer.toUnsignedString(mail.hashCode()) + ".moj");
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(cacheFile));
                objectOutputStream.writeObject(mail);
                objectOutputStream.flush();
                objectOutputStream.close();
                if (cacheFile.length() == 0L)
                    System.out.println("Error: Kunde inte spara cachefil");
            } catch (IOException e) {
                System.out.println("error: ");
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return folder.getName();
    }

    /**
     * Stänger mailfolder-objektet. Dödar trådar. Skickar "close" till IMAP-servern.
     */
    void close() {
        newMailES.shutdownNow();
        mailBodyES.shutdownNow();
        mailObjectCreator.shutdownNow();

        try {
            folder.close();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
