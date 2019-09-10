import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailClientGUI {
    private JList mailList;
    private JButton newButton;
    private JTextArea bodyArea;
    private JTextField fromField;
    private JTextField toField;
    private JTextField ccField;
    private JLabel accountLabel;
    private JPanel panel;
    private JScrollPane bodyScroll;
    private JLabel subjectLabel;
    private List<Mail> mailArrayList = Collections.synchronizedList(new ArrayList<>());
    private GmailClient gmailClient;
    private ExecutorService executorService = Executors.newCachedThreadPool();

    private final int emailCount = 3;

    EmailClientGUI(GmailClient gmailClient) {
        this.gmailClient = gmailClient;
        this.accountLabel.setText(gmailClient.getUsername());

        mailList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Mail mail = (Mail) mailList.getSelectedValue();
                this.fromField.setText(mail.getFrom());
                this.toField.setText(mail.getTo());
                this.subjectLabel.setText(mail.getSubject());
                this.bodyArea.setText(mail.getBody());
                this.bodyArea.setCaretPosition(0);
            }
        });
        newButton.addActionListener(e -> newMail());
    }

    void show() {
        JFrame frame = new JFrame("Email Client");
        frame.setPreferredSize(new Dimension(800, 600));
        frame.setContentPane(this.panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    executorService.shutdown();
                    gmailClient.close();
                } catch (MessagingException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void newMail() {
        this.toField.setText("");
        this.bodyArea.setText("");
    }
    void populateMailList() throws MessagingException {
        Folder emailFolder = gmailClient.getOpenFolder("INBOX");
        int mailCount = emailFolder.getMessageCount();

        Message[] messages;
        if (mailCount >= emailCount)
             messages= emailFolder.getMessages(mailCount - emailCount, mailCount);
        else
            messages = emailFolder.getMessages();
        for (Message message : messages) {
            Mail mailObj;
            try {
                mailObj = new Mail(message);

                executorService.submit(new SetMailBody(mailObj, emailFolder, gmailClient));
            } catch (MessagingException | ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                continue;
            }
            this.mailArrayList.add(mailObj);

            ArrayList<Mail> messageArrayCopy = new ArrayList<>(this.mailArrayList);
            Collections.reverse(messageArrayCopy);
            this.mailList.setListData(messageArrayCopy.toArray());
        }
        Collections.reverse(this.mailArrayList);
        this.mailList.setListData(this.mailArrayList.toArray());
    }
}
