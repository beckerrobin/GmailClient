import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailClientGUI {
    private final int emailCount = 2;
    private List<Mail> mailArrayList = Collections.synchronizedList(new ArrayList<>());
    private GmailClient gmailClient;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel panel;
    private JLabel accountLabel;
    private JButton newButton;
    private JTextField fromField;
    private JTextField ccField;
    private JScrollPane bodyScroll;
    private JTextArea bodyArea;
    private JList mailList;
    private JButton cancelButton;
    private JButton sendButton;
    private JLabel fromLabel;
    private JTextField toField;
    private JTextField subjectField;
    private JPanel buttonPanel;
    private JPanel rightPanel;

    EmailClientGUI(GmailClient gmailClient) {
        this.gmailClient = gmailClient;
        this.accountLabel.setText(gmailClient.getUsername());
        readMode();

        mailList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                readMode();
                Mail mail = (Mail) mailList.getSelectedValue();
                this.fromField.setText(mail.getFrom());
                this.toField.setText(mail.getTo());
                this.subjectField.setText(mail.getSubject());
                this.subjectField.setCaretPosition(0);
                this.bodyArea.setText(mail.getBody());
                this.bodyArea.setCaretPosition(0);
            }
        });
        newButton.addActionListener(e -> newMail());
        cancelButton.addActionListener(e -> readMode());
        sendButton.addActionListener(e -> sendMail());
    }

    private void sendMail() {
        // Check to
        // Check cc
        // Check body
        // Send via SMTP

    }

    void show() {
        if (gmailClient.isConnected()) {
            this.newButton.setEnabled(true);
        }
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
                    System.exit(2);
                }
            }
        });
    }

    private void composeMode() {
        this.toField.setEditable(true);
        this.toField.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        this.ccField.setEditable(true);
        this.ccField.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));

        this.subjectField.setEditable(true);
        this.subjectField.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
//        this.subjectField.setBackground(Color.WHITE);

        this.fromField.setText(this.gmailClient.getUsername());
        this.sendButton.setVisible(true);
        this.cancelButton.setVisible(true);

    }

    private void readMode() {
        this.toField.setEditable(false);
        this.toField.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
        this.ccField.setEditable(false);
        this.ccField.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
//        this.subjectField.setText("Subject");
        this.subjectField.setEditable(false);
        this.subjectField.setBorder(null);

        this.sendButton.setVisible(false);
        this.cancelButton.setVisible(false);
        this.fromField.setText("");
        this.toField.setText("");
        this.ccField.setText("");
        this.subjectField.setText("");
        this.bodyArea.setText("");
    }

    private void newMail() {
        composeMode();
        this.toField.setText("");
        this.ccField.setText("");
        this.subjectField.setText("");
        this.bodyArea.setText("");
        this.mailList.setSelectedIndex(-1);
        this.toField.grabFocus();
    }

    void populateMailList() throws MessagingException {
        Folder emailFolder = gmailClient.getOpenFolder("INBOX");
        int mailCount = emailFolder.getMessageCount();

        Message[] messages;
        if (mailCount >= emailCount)
            messages = emailFolder.getMessages(mailCount - emailCount + 1, mailCount);
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
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
