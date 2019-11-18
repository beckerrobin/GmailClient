import javax.swing.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ConnectForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField userField;
    private JPasswordField passwordField;
    private GmailClient gmailClient;

    public ConnectForm(GmailClient gmailClient) {
        this.gmailClient = gmailClient;

        // Autofyll senast använda inloggningsuppgifterna om dessa finns
        File loginFile = new File(GmailClient.LOGIN_FILE_PATH);
        if (loginFile.exists() && loginFile.canRead()) {
            try (BufferedReader br = new BufferedReader(new FileReader(loginFile, StandardCharsets.UTF_16))) {
                String data = br.readLine();
                String[] stringArr = data.split(";");
                this.userField.setText(stringArr[0]);
                this.passwordField.setText(stringArr[1]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        setContentPane(contentPane);
        setModal(true);
        setTitle("Anslut...");
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());
        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void onOK() {
        this.gmailClient.setParams(this.userField.getText(), String.valueOf(this.passwordField.getPassword()));
        dispose();
        this.gmailClient.init();
    }

    private void onCancel() {
        dispose();
    }
}
