import com.sun.mail.imap.IMAPBodyPart;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

/**
 * Klass för Mail-objekt. Innehåller Mail-data.
 */
public class Mail implements Serializable {
    static final String defaultBody = "Laddar innehåll...";
    private static final long serialVersionUID = 1L;
    private String parentFolder;
    private String subject;
    private String from;
    private int messageNumber;
    private String body = Mail.defaultBody;
    private String to;
    private String cc;
    private Date date;
    private ArrayList<MailAttachment> attachments = new ArrayList<>();
    private transient Message message; // Är inte serializable

    Mail(Message message) throws MessagingException {
        this.message = message;
        this.parentFolder = message.getFolder().getName();
        this.subject = message.getSubject();
        this.from = ((InternetAddress) message.getFrom()[0]).getAddress();
        this.messageNumber = message.getMessageNumber();

        // Mottagareadresser
        ArrayList<String> toArr = new ArrayList<>();
        for (Address recepient : message.getRecipients(Message.RecipientType.TO)) {
            toArr.add(((InternetAddress) recepient).getAddress());
        }
        this.to = String.join("; ", toArr);

        // CC-addresser
        if (message.getRecipients(Message.RecipientType.CC) != null) {
            ArrayList<String> ccArr = new ArrayList<>();
            for (Address recepient : message.getRecipients(Message.RecipientType.CC)) {
                ccArr.add(((InternetAddress) recepient).getAddress());
            }
            this.cc = String.join("; ", ccArr);
        }

        // Datum
        this.date = message.getReceivedDate();
    }

    public String getParentFolder() {
        return parentFolder;
    }

    boolean hasAttachment() {
        return attachments != null && !attachments.isEmpty();
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        System.out.println(getSubject() + ": Setting message");
        this.message = message;
    }

    void addAttachment(IMAPBodyPart part) {
        attachments.add(new MailAttachment(part));
    }

    void fetchAttachments() {
        attachments = new ArrayList<>();
        try {
            if (message.getContentType().toLowerCase().contains("multipart")) {
                Multipart multipart = (Multipart) message.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    Part part = multipart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                        addAttachment((IMAPBodyPart) part);
                    }
                }
            }
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<MailAttachment> getAttachments() {
        return attachments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mail mail = (Mail) o;
        return this.hashCode() == mail.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, cc, date, subject);
    }

    @Override
    public String toString() {
        return (subject.length() > 16 ? subject.substring(0, 13) + "..." : subject) + "\n" + from;
    }

    public Date getDate() {
        return date;
    }

    String getBody() {
        return body;
    }

    void setBody(String body) {
        this.body = body;
    }

    String getTo() {
        return to;
    }

    String getSubject() {
        return subject;
    }

    String getFrom() {
        return from;
    }

    int getMessageNumber() {
        return messageNumber;
    }

    public String getCc() {
        return cc;
    }
}
