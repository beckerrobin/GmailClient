import javax.swing.*;

public class LoadingPopup extends JDialog {
    private JPanel contentPane;
    private JLabel textLabel;

    public LoadingPopup(String text) {
        this.textLabel.setText(text);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setContentPane(contentPane);
        setResizable(false);
        setModalityType(ModalityType.APPLICATION_MODAL);
        pack();
    }

    public void setText(String newText) {
        textLabel.setText(newText);
    }
}
