import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MailCellRenderer implements ListCellRenderer<Mail> {
    private JPanel cellPanel;
    private JLabel mailDate;
    private JLabel mailSubject;
    private JLabel mailSender;
    private JLabel attachmentLabel;
    private final int topBottomPadding = 8;

    MailCellRenderer() {
        cellPanel = new JPanel();
        cellPanel.setLayout(new BoxLayout(cellPanel, BoxLayout.Y_AXIS));
        cellPanel.setBackground(Color.white);
        cellPanel.setBorder(new EmptyBorder(topBottomPadding, 3, topBottomPadding, 3));

        mailSender = new JLabel();
        mailSender.setFont(new Font("Calibri", Font.PLAIN, 14)); // Windows vista+-only font
        mailSubject = new JLabel();
        mailSubject.setFont(new Font("Calibri", Font.PLAIN, 14)); // Windows vista+-only font
        mailSubject.setBackground(Color.white);
        mailDate = new JLabel();
        mailDate.setFont(new Font("Calibri", Font.PLAIN, 10)); // Windows vista+-only font
        mailDate.setBackground(Color.white);
        ImageIcon attachmentIcon = new ImageIcon("resources/attachment_18dp.png");
        ImageIcon scaledIcon = new ImageIcon(attachmentIcon.getImage().getScaledInstance(12, 12, Image.SCALE_SMOOTH));
        attachmentLabel = new JLabel(scaledIcon);

        cellPanel.add(mailSubject);
        cellPanel.add(mailSender);
        cellPanel.add(mailDate);
        cellPanel.add(attachmentLabel);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Mail> list, Mail value, int index, boolean isSelected, boolean cellHasFocus) {
        mailSender.setText(value.getFrom());
        mailSubject.setText(value.getSubject().length() <= 34 ? value.getSubject() : value.getSubject().substring(0, 34).trim() + "...");
        LocalDateTime today = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        LocalDateTime mailReceiveDateTime = LocalDateTime.ofInstant(value.getDate().toInstant(), ZoneId.systemDefault());

        // Formatera text
        if (mailReceiveDateTime.isAfter(today)) { // Idag
            mailDate.setText(mailReceiveDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        } else if (mailReceiveDateTime.isAfter(today.minusDays(6))) { // I veckan
            mailDate.setText(mailReceiveDateTime.format(DateTimeFormatter.ofPattern("EEE HH:mm")));
        } else {
            mailDate.setText(mailReceiveDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }

        // Hide attachment icon
        if (!value.hasAttachment()) {
            attachmentLabel.setVisible(false);
        } else {
            attachmentLabel.setVisible(true);
        }

        if (cellHasFocus) {
            cellPanel.setBackground(new Color(0xE0EED8));
        } else {
            cellPanel.setBackground(Color.white);
        }

        list.setFixedCellHeight(-1);
        return cellPanel;
    }
}
