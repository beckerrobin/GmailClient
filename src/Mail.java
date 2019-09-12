import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.util.Objects;

public class Mail {
    private String subject;
    private String from;
    private int id;
    private String body = "Laddar...";
    private String to;

    Mail(Message message) throws MessagingException {
        this.subject = message.getSubject();
        this.from = ((InternetAddress)message.getFrom()[0]).getAddress();
        this.id = message.getMessageNumber();
        this.to = ((InternetAddress)message.getAllRecipients()[0]).getAddress();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mail mail = (Mail) o;
        return id == mail.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (subject.length() > 16 ? subject.substring(0, 13) + "..." : subject) + " " + from;
    }

    String getBody() {
        return body;
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

    void setBody(String body) {
        this.body = body;
    }

    int getId() {
        return id;
    }
}
