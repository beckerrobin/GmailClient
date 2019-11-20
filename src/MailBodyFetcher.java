import com.sun.mail.imap.IMAPBodyPart;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import java.io.File;
import java.io.IOException;

/**
 * Klass för att hämta mail-body
 */
class MailBodyFetcher implements Runnable {
    private Mail mail;
    private MailFolder mailFolder;

    MailBodyFetcher(Mail mail, MailFolder mailFolder) {
        this.mail = mail;
        this.mailFolder = mailFolder;
    }

    private String getMailContent(Object content) throws MessagingException, IOException {
        StringBuilder returnValue = new StringBuilder();
        if (content instanceof Part && ((Part) content).isMimeType("text/*")) {
            Part part = (Part) content;
            if (part.isMimeType("text/html")) {
                returnValue.append(part.getContent());
            }
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
//            System.out.println(multipart.getContentType() + ": " + multipart.getCount());
            if (multipart.getContentType().toLowerCase().contains("multipart/alternative")) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    Part subPart = multipart.getBodyPart(i);
                    if (subPart.isMimeType("text/html")) {
                        returnValue.append(subPart.getContent());
                    } else {
                        if (multipart.getCount() == 1) {
                            returnValue.append(subPart.getContent());
                        }
                    }
                }
            } else if (multipart.getContentType().toLowerCase().contains("multipart/")) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    returnValue.append(getMailContent(multipart.getBodyPart(i)));
                }
            } else {
                System.out.println("Doesn't recognise mail: " + multipart.getContentType().toLowerCase());
            }
        } else if (content instanceof IMAPBodyPart) {
            IMAPBodyPart part = (IMAPBodyPart) content;
//            System.out.println(part.getContentType() + ": " + part.getContent());

            if (part.getContentType().contains("multipart/")) {
                returnValue.append(getMailContent(part.getContent()));
            } else if (part.getContentType().contains("IMAGE/")) {
                if (part.getContentType().contains("GIF") || part.getContentType().endsWith("JPEG")) {
                    String fileName = part.getContentID().replaceAll("[<>]", "");
                    File imageFile = new File(mailFolder.getCacheSubFolder(), fileName);
                    if (!imageFile.exists()) { // Bilden kan finnas cachad
                        part.saveFile(imageFile);
                    }
                } else {
                    mail.addAttachment(part);
                }
            } else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                mail.addAttachment(part);
            } else {
                System.out.println("disisp: " + part.getDisposition());
                System.out.println("unkown type: " + part.getContentType());
                System.out.println("file name: " + part.getFileName());
                returnValue.append("\nunkown imap mimetype: ").append(part.getContentType());
                returnValue.append("\ndata type: ").append(part.getContent().getClass().getName());
                returnValue.append("\ndata: ").append(part.getContent());
            }
        } else {
            returnValue.append(content);
        }
        return returnValue.toString();
    }

    private String getMailBody() throws MessagingException, IOException {
        return getMailContent(mail.getMessage().getContent());
    }

    @Override
    public void run() {
        EmailClientGUI.startLoad();
        try {
            mail.setBody(this.getMailBody());
            mailFolder.cacheMail(mail);
        } catch (MessagingException | IOException e) {
            System.out.println("Error vid hämtning för body för mailid " + this.mail.getMessageNumber() + ": " + e.getMessage());
        } finally {
            EmailClientGUI.stopLoad();
        }
    }
}
