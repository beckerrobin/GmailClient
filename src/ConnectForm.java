import javax.swing.*;
import java.awt.event.*;

public class ConnectForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField userField;
    private JPasswordField passwordField;
    private GmailClient gmailClient;

    public ConnectForm(GmailClient eclient) {
        this.gmailClient = eclient;
        this.userField.setText("becker.b.robin@gmail.com");
        this.passwordField.setText("lnhrdmevuyvoybzg");

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
        this.gmailClient.setParams(this.userField.getText(), this.passwordField.getPassword());
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}
