import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebView;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

/**
 * Klass för att hantera GUI och allmänt gränssnitt mot användaren. Tar ett GmailClient-objekt vid initialiseringen
 * Det finns 2 lägen ("modes"). 'Read' och 'Compose' som får GUI:t att ändra sitt utseende.
 */
public class EmailClientGUI {
    static String selectedFolder = "INBOX";
    private static int loadProgress = 0;
    private int lastSelectedIndex = -1;
    private GmailClient gmailClient;
    private JFrame mainFrame;
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel panel;
    private JLabel accountLabel;
    private JButton loginButton = new JButton("Logga in");
    private JButton newButton;
    private JTextField fromField;
    private JTextField ccField;
    private JFXPanel jfxPanel;
    private JList<Mail> mailList;
    private JButton cancelButton;
    private JButton sendButton;
    private JLabel fromLabel;
    private JTextField toField;
    private JTextField subjectField;
    private JPanel buttonPanel;
    private JPanel rightPanel;
    private JProgressBar loadProgressBar;
    private JProgressBar sendProgressBar;
    private JPanel bodyPanel;
    private JScrollPane mailListScrollPane;
    private JPanel connectPanel;
    private JPanel attachmentPanel;


    EmailClientGUI(GmailClient gmailClient) {
        this.gmailClient = gmailClient;
        readMode(); // Starta i läs-mode

        // Lyssnar efter om någon klickar på ett mail i vänstra listan
        mailList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && mailList.getSelectedIndex() >= 0) {
                lastSelectedIndex = mailList.getSelectedIndex();
                selectMail(mailList.getSelectedValue());
            }
        });
        mailList.setCellRenderer(new MailCellRenderer());

        ImageIcon googleIcon = new ImageIcon("resources/32px_google.png");
        loginButton.setIcon(new ImageIcon(googleIcon.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH)));
        loginButton.addActionListener(a -> new ConnectForm(gmailClient));

        newButton.setIcon(new ImageIcon("resources/create_32dp.png"));
        newButton.addActionListener(event -> newMail());

        FocusAdapter mailAddressFieldChecker = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                JTextField field = (JTextField) e.getComponent();
                if (field.isEditable()) { // Adresser i formatet "adress1; adress2;"
                    if (field.getText().isEmpty()) {
                        resetTextFieldLayout(field);
                        return;
                    }
                    String content = field.getText().trim();

                    // Sära på mailadresser med "; "
                    Pattern pattern = Pattern.compile("((?<!;) )|(;(?!\\s))");
                    Matcher m = pattern.matcher(content);
                    content = m.replaceAll("; ");

                    // Kontrollera alla mailadresser
                    String[] adresses = content.split(";\\s");
                    StringBuilder newAddressString = new StringBuilder();
                    for (String adress : adresses) {
                        adress = adress.trim();
                        if (adress.endsWith(";")) {
                            adress = adress.substring(0, adress.length() - 1);
                        }
                        if (!isMailAddress(adress)) {
                            showWrongInput(field);
                            return;
                        }
                        newAddressString.append(adress.concat("; "));
                    }

                    resetTextFieldLayout(field);
                    field.setText(newAddressString.toString().trim());
                }
            }
        };

        // Kontrollera TO adresser
        toField.addFocusListener(mailAddressFieldChecker);

        // Kontrollera CC adresser
        ccField.addFocusListener(mailAddressFieldChecker);
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
                        "\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])");
        return p.matcher(email).matches();
    }

    static void startLoad() {
        loadProgress++;
    }

    /**
     * Minska ladd-värde. Processbar döljs vid loadProgress <= 0
     */
    static void stopLoad() {
        loadProgress--;
        if (loadProgress < 0)
            loadProgress = 0;
    }

    private void deleteMail(Mail mail) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                MailFolder mailFolder = gmailClient.getMailFolder(mail.getParentFolder());
                if (!mailFolder.getMailSet().contains(mail))
                    return;

                synchronized (gmailClient.getMailFolder(mail.getParentFolder())) {
                    try {
                        mailFolder.folder.setFlags(new int[]{mail.getMessageNumber()}, new Flags(Flags.Flag.DELETED), true);
                        mailFolder.folder.expunge();
                        mailFolder.removeMailObject(mail);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        if (loadProgress == 0)
            populateMailList(selectedFolder);
        readMode();
    }

    private void replyMail(Mail mail) {
        composeMode();
        toField.setText(mail.getFrom());
        subjectField.setText(mail.getSubject().toUpperCase().startsWith("SV:") ? mail.getSubject() : "SV: " + mail.getSubject());
        Platform.runLater(() -> {
            TextArea textArea = new TextArea();
            textArea.setWrapText(true);
            textArea.setStyle("-fx-border-color: black");

            jfxPanel.setScene(new Scene(textArea));
            textArea.setText("\n\n\n" + mail.getBody());
            textArea.positionCaret(0);
            jfxPanel.grabFocus();
            textArea.requestFocus();
        });
    }

    /**
     * Välj ett mail, förändra GUI:et
     */
    private void selectMail(Mail mail) {
        readMode();
        fromField.setText(mail.getFrom());
        toField.setText(mail.getTo());
        ccField.setText(mail.getCc());
        subjectField.setText(mail.getSubject());
        subjectField.setCaretPosition(0);

        for (ActionListener actionListener : cancelButton.getActionListeners()) {
            cancelButton.removeActionListener(actionListener);
        }
        cancelButton.addActionListener(event -> {
            deleteMail(mail);
        });
        cancelButton.setVisible(true);
        cancelButton.setEnabled(true);

        for (ActionListener actionListener : sendButton.getActionListeners()) {
            sendButton.removeActionListener(actionListener);
        }
        sendButton.addActionListener(e -> replyMail(mail));
        sendButton.setText("Svara");
        sendButton.setVisible(true);

        int saved = mailList.getSelectedIndex();
        if (mail.getBody().equals(Mail.defaultBody)) {
            Thread t = new Thread(new SwingWorker<Void, Void>() {
                @Override
                protected void process(List<Void> chunks) {
                    sendProgressBar.setVisible(true);
                }

                @Override
                protected Void doInBackground() throws Exception {
                    publish(); // Visa progressbar
                    Executors.newSingleThreadExecutor().submit(EmailClientGUI.this.gmailClient.reloadMail(mail)).get();
                    return null;
                }

                @Override
                protected void done() {
                    if (saved == lastSelectedIndex)
                        selectMail(mail);
                    sendProgressBar.setVisible(false);
                }
            });
            t.setPriority(8);
            sendButton.setEnabled(false);
            t.start();
        } else {
            sendButton.setEnabled(true);
        }
        if (mail.hasAttachment()) {
            attachmentPanel.removeAll();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            if (mail.getMessage() == null) {
                try {
                    mail.setMessage(gmailClient.getMailFolder(mail.getParentFolder()).fetchMessage(mail.getMessageNumber()));
                    mail.fetchAttachments();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
            for (MailAttachment attachment : mail.getAttachments()) {
                MailAttachmentButton button = new MailAttachmentButton(attachment);
                attachmentPanel.add(button, gbc);
            }
            attachmentPanel.setVisible(true);
        }
        System.out.println(Integer.toUnsignedString(mail.hashCode()));

        Platform.runLater(() -> {
            WebView webView = new WebView();
            jfxPanel.setScene(new Scene(webView));
            String parsedBody = mailBodyParser(mail);
            webView.getEngine().loadContent(parsedBody);
        });
    }

    private String mailBodyParser(Mail mail) {
        String body = mail.getBody();
        if (body.contains("cid:")) {
            body = body.replaceAll("cid:", gmailClient.getMailFolder(mail.getParentFolder()).getCacheSubFolder().toURI().toString());
        }
        return body;
    }

    /**
     * Återställ ett JTextField till orginalutseende, se showWrongInput()
     *
     * @param field Fältet som ska förändras
     */
    private void resetTextFieldLayout(JTextField field) {
        field.setBackground(Color.white);
    }

    /**
     * Visa för användaren att JTextField innehåller ett felaktigt värde
     *
     * @param field Fältet som ska förändras
     */
    private void showWrongInput(JTextField field) {
        field.setBackground(new Color(0xffcabd));
    }

    /**
     * Metod för att skicka ett mail.
     * Kontrollera först att mottagarens adress är en korrekt mailadress.
     * Om ämnesraden är tom så dubbelkolla med användaren om den verkligen ska vara det.
     */
    private void sendMail() {
        // Check to
        String[] toAddresses = this.toField.getText().trim().split(";\\s*");
        for (String toAddress : toAddresses) {
            if (!isMailAddress(toAddress)) {
                showWrongInput(this.toField);
                return;
            }
        }

        // Check CC
        if (!this.ccField.getText().isBlank()) {
            String[] ccAdresses = this.ccField.getText().trim().split(";\\s*");
            for (String ccAdress : ccAdresses) {
                if (!isMailAddress(ccAdress)) {
                    showWrongInput(this.ccField);
                    return;
                }
            }
        }

        // Check Subject
        if (subjectField.getText().isBlank()) {
            int res = JOptionPane.showConfirmDialog(panel, "Ämnesraden är tom, vill du skicka mailet ändå?", "Tom ämnesrad", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res != 0)
                return;
        }

        // Check body
        String body = ((TextArea) jfxPanel.getScene().getRoot()).getText();
        if (body.isEmpty()) {
            int res = JOptionPane.showConfirmDialog(panel, "Mailet är tomt, vill du skicka mailet ändå?", "Tom mailkropp", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res != 0)
                return;
        }

        // Skicka via SMTP
        Timer timer = new Timer(2000, e -> sendProgressBar.setVisible(true));
        timer.setRepeats(false);
        timer.start();
        sendButton.setEnabled(false);
        cancelButton.setEnabled(false);
        ((TextArea) jfxPanel.getScene().getRoot()).setEditable(false);

        SwingWorker<Boolean, Void> sw = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return gmailClient.sendMail(toField.getText(), ccField.getText(), subjectField.getText(), ((TextArea) jfxPanel.getScene()
                        .getRoot()).getText());
            }

            @Override
            protected void done() {
                timer.stop();
                sendProgressBar.setVisible(false);
            }
        };
        sw.execute();
        readMode();
    }

    /**
     * Visa GUI:t
     */
    void show() {
        mainFrame = new JFrame("GmailKlient");
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                super.windowOpened(e);
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        while (true) {
                            if (loadProgress > 0) {
                                loadProgressBar.setVisible(true);
                            } else {
                                loadProgressBar.setVisible(false);
                            }
                            sleep(500);
                        }
                    }
                }.execute();
                // Medans GUI:t visas:

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        while (true) {
                            if (gmailClient.getGmailAddress() != null) {
                                if (loginButton.isVisible()) {
                                    connectPanel.removeAll();
                                    connectPanel.add(accountLabel);
                                    connectPanel.validate();
                                    connectPanel.repaint();
                                }

                                // Om ansluten till Google
                                if (gmailClient.isConnected()) {
                                    accountLabel.setText(gmailClient.getGmailAddress());
                                    newButton.setEnabled(true);
                                    populateMailList(selectedFolder); // Populate the left column with mails from the selected folder
                                } else {
                                    startLoad();
                                    accountLabel.setText("Ansluter till gmail...");
                                    newButton.setEnabled(false);
                                    if (gmailClient.connect()) {
                                        // Om anslutning upprättats, hoppa över sleep nedan
                                        stopLoad();
                                        continue;
                                    }
                                }
                            } else {
                                // Visa loginknapp
                                stopLoad();
                                connectPanel.removeAll();
                                connectPanel.add(loginButton);
                                connectPanel.validate();
                                connectPanel.repaint();
                            }
                            try {
                                sleep(1500);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }.execute();
            }
        });
        mainFrame.setPreferredSize(new Dimension(800, 600));
        mainFrame.setContentPane(this.panel);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    mainFrame.setVisible(false); // Stäng GUI först = responsivt GUI
                    gmailClient.close();
                } catch (MessagingException ex) {
                    System.out.println(ex.getMessage());
                    System.exit(2);
                }
            }
        });
        mainFrame.setVisible(true);
    }

    /**
     * Compose-läget. Används vid författande av nya mail, även vid svar/vidarebefodrande
     */
    private void composeMode() {
        toField.setEditable(true);
        toField.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        this.ccField.setEditable(true);
        this.ccField.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        subjectField.setEditable(true);
        subjectField.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        fromField.setText(this.gmailClient.getGmailAddress());

        sendButton.setText("Skicka");
        for (ActionListener actionListener : sendButton.getActionListeners()) {
            sendButton.removeActionListener(actionListener);
        }
        sendButton.addActionListener(event -> sendMail());
        sendButton.setVisible(true);
        sendButton.setEnabled(true);

        for (ActionListener actionListener : cancelButton.getActionListeners()) {
            cancelButton.removeActionListener(actionListener);
        }
        cancelButton.addActionListener(event -> readMode());
        cancelButton.setVisible(true);
        cancelButton.setEnabled(true);

        attachmentPanel.setVisible(false);

        Platform.runLater(() -> {
            TextArea textArea = new TextArea();
            textArea.setWrapText(true);
            textArea.setStyle("-fx-border-color: black");
            jfxPanel.setScene(new Scene(textArea));
            jfxPanel.setEnabled(true);
        });
    }

    /**
     * Läs-läget. Standardläge som används utanför Compose-läget.
     */
    private void readMode() {
        toField.setEditable(false);
        toField.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
        this.ccField.setEditable(false);
        this.ccField.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
        subjectField.setEditable(false);
        subjectField.setBorder(null);
        sendButton.setVisible(false);
        cancelButton.setVisible(false);
        fromField.setText("");
        toField.setText("");
        this.ccField.setText("");
        subjectField.setText("");
        attachmentPanel.setVisible(false);

        Platform.runLater(() -> {
            jfxPanel.setScene(new Scene(new Label()));
            jfxPanel.setBackground(Color.gray);
        });
    }

    /**
     * När användaren initierar processen att författa ett nytt mail.
     */
    private void newMail() {
        composeMode();
        toField.setText("");
        this.ccField.setText("");
        subjectField.setText("");
        mailList.setSelectedIndex(-1);
        toField.grabFocus();
    }

    /**
     * Fyll det grafiska tabellen med mail från den specifika listan
     */
    private void populateMailList(String selectedFolder) {
        List<Mail> mails = Arrays.asList(gmailClient.getMailArray(selectedFolder));
        mails.sort(Comparator.comparing(Mail::getDate).reversed());
        mailList.setListData(mails.toArray(Mail[]::new));
    }

    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
