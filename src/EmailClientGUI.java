import javax.mail.MessagingException;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

public class EmailClientGUI {
    private GmailClient gmailClient;
    private Thread guiUpdate;
    private String selectedFolder = "INBOX";

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
            if (!e.getValueIsAdjusting() && mailList.getSelectedIndex() >= 0) {
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
        toField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (isMailAddress(toField.getText()))
                    resetTextFieldLayout(toField);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (isMailAddress(toField.getText()))
                    resetTextFieldLayout(toField);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (isMailAddress(toField.getText()))
                    resetTextFieldLayout(toField);
            }
        });
    }

    /**
     * Kontrollerar om addressen är en giltig mailadress enligt RFC 5322
     * Från https://emailregex.com/ hämtad 2019-09-11
     *
     * @param email String
     * @return Ger true om hela email är en giltig emailadress, annars false
     */
    private static boolean isMailAddress(String email) {
        Pattern p = Pattern.compile(
                "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c" +
                        "\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@" +
                        "(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[" +
                        "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?" +
                        "|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[" +
                        "\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");
        return p.matcher(email).matches();
    }

    private void resetTextFieldLayout(JTextField field) {
        field.setBackground(Color.white);

    }

    private void showWrongInput(JTextField field) {
//            field.setForeground(Color.red);
//            field.setFont(field.getFont().deriveFont(Font.BOLD));
        field.setBackground(new Color(0xffcabd));
    }

    private void sendMail() {
        // Check to
        if (!isMailAddress(this.toField.getText())) {
            showWrongInput(this.toField);
            return;
        }

        // Check Subject
        if (this.subjectField.getText().isBlank()) {
            int res = JOptionPane.showConfirmDialog(this.panel, "Ämnesraden är tom, vill du skicka mailet ändå?", "Tom ämnesrad", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res != 0)
                return;
        }
        // Check body?

        // Send via SMTP
        boolean res = gmailClient.sendMail(this.toField.getText(), this.subjectField.getText(), this.bodyArea.getText());
        if (res)
            readMode();
    }

    void show() {
        if (gmailClient.isConnected()) {
            this.newButton.setEnabled(true);
        }
        JFrame frame = new JFrame("GmailKlient");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                super.windowOpened(e);

                // Constantly update GUI if client is connected
                guiUpdate = new Thread(() -> {
                    while (true) {
                        if (gmailClient.isConnected())
                            try {
                                populateMailList(); // Populate the left column with mails from the selected folder
                            } catch (MessagingException ex) {
                                ex.printStackTrace();
                            }
                        try {
                            sleep(5000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                guiUpdate.setPriority(3);
                guiUpdate.start();
            }
        });
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
//        this.ccField.setEditable(true);
//        this.ccField.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        this.subjectField.setEditable(true);
        this.subjectField.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
        this.fromField.setText(this.gmailClient.getUsername());
        this.sendButton.setVisible(true);
        this.cancelButton.setVisible(true);
    }

    private void readMode() {
        this.toField.setEditable(false);
        this.toField.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
//        this.ccField.setEditable(false);
//        this.ccField.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
        this.subjectField.setEditable(false);
        this.subjectField.setBorder(null);
        this.sendButton.setVisible(false);
        this.cancelButton.setVisible(false);
        this.fromField.setText("");
        this.toField.setText("");
//        this.ccField.setText("");
        this.subjectField.setText("");
        this.bodyArea.setText("");
    }

    private void newMail() {
        composeMode();
        this.toField.setText("");
//        this.ccField.setText("");
        this.subjectField.setText("");
        this.bodyArea.setText("");
        this.mailList.setSelectedIndex(-1);
        this.toField.grabFocus();
    }

    private void populateMailList() throws MessagingException {
        List<Mail> mails = Arrays.asList(gmailClient.getLocalMail(gmailClient.getFolder(selectedFolder)));
        Collections.reverse(mails);
        this.mailList.setListData(mails.toArray(Mail[]::new));

    }
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
