import javax.mail.*;
import java.io.IOException;

class FetchMailBody implements Runnable {
    private Mail mail;
    private Folder folder;

    FetchMailBody(Mail mail, Folder folder) {
        this.mail = mail;
        this.folder = folder;
    }

    @Override
    public void run() {
        try {
            mail.setBody(this.getMailBody());
            System.out.println("Hämtat body för " + this.mail.getId());
        } catch (MessagingException | IOException e) {
            System.out.println("Error för " + this.mail.getId() + ": " + e.getMessage());
        }
    }

    private String getMailBody() throws MessagingException, IOException {
        Message msg = folder.getMessage(this.mail.getId());

        Object content = msg.getContent();
        if (content instanceof Multipart) {
            StringBuilder messageContent = new StringBuilder();
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                Part part = multipart.getBodyPart(i);
                System.out.println(this.mail.getId() + ": (" + i + "/" + multipart.getCount() + ") " + part.getContentType());
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
