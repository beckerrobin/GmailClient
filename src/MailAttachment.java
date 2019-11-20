import com.sun.mail.imap.IMAPBodyPart;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.Serializable;

public class MailAttachment implements Serializable {
    private String fileName = "none";
    private int size;
    private transient IMAPBodyPart bodyPart;

    public MailAttachment(IMAPBodyPart part) {
        try {
            this.fileName = part.getFileName();
            this.size = part.getSize();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        this.bodyPart = part;
    }

    int getSize() {
        return size;
    }

    void saveFile(String filePath) throws IOException, MessagingException {
        if (bodyPart != null) {
            bodyPart.saveFile(filePath);
        }
    }

    public String getFileName() {
        return fileName;
    }
}
