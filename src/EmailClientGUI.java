import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class EmailClientGUI implements Runnable {
    private JList mailList;
    private JButton newButton;
    private JTextArea bodyArea;
    private JTextField fromField;
    private JTextField toField;
    private JTextField ccField;
    private JLabel accountLabel;
    private JPanel panel;
    private ArrayList<Mail> mailArrayList = new ArrayList<>();

    public EmailClientGUI() {
        mailList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mailList.getSelectedIndex();
            }
        });
    }

    public void show() {
        JFrame frame = new JFrame("Email Client");
        frame.setPreferredSize(new Dimension(800, 600));
        frame.setContentPane(this.panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void populateMailList(Folder emailFolder) throws MessagingException {
        emailFolder.open(Folder.READ_ONLY);
        int mailCount = emailFolder.getMessageCount();
        Message[] messages = emailFolder.getMessages(mailCount - 10, mailCount);
        for (Message message : messages) {
            try {
                this.mailArrayList.add(new Mail(message));
                System.out.println(message.getSubject());
                ArrayList<Mail> messageArrayCopy = new ArrayList<>(this.mailArrayList);
//                Collections.copy(messageArrayCopy, messageArray);
                Collections.reverse(messageArrayCopy);
                this.mailList.setListData(messageArrayCopy.toArray());
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
        Collections.reverse(this.mailArrayList);
        this.mailList.setListData(this.mailArrayList.toArray());
        emailFolder.close();
    }

    @Override
    public void run() {
        show();
    }
}

class Mail implements Runnable{
    private String subject;
    private String from;
    private int id;
    private String body;

    public Mail(Message message) throws MessagingException {
        this.subject = message.getSubject();
        this.from = ((InternetAddress) message.getFrom()[0]).getAddress();
        this.id = message.getMessageNumber();
    }

    @Override
    public String toString() {
        return subject + " " + from;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getId() {
        return id;
    }

    @Override
    public void run() {

    }
}
