import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

public class MailAttachmentButton extends JTextArea implements MouseListener {

    private String fileName;
    private String fileExtension;
    private MailAttachment mailAttachment;

    public MailAttachmentButton(MailAttachment mailAttachment) {
        super();
        this.mailAttachment = mailAttachment;

        final int rows = 3;
        final int horizPadding = 4;
        final int vertPadding = 4;
        this.setBorder(new CompoundBorder(new LineBorder(new Color(0xA8D1A8)), new EmptyBorder(horizPadding, vertPadding, horizPadding, vertPadding)));
        final int height = this.getRowHeight() * rows + vertPadding * 2 + 2; // raders höjd + padding top & bottom + LineBorder 1px * 2
        final int width = this.getColumnWidth() * 15;
        final Dimension BUTTON_DIMENSION = new Dimension(width, height);

        this.setRows(rows);
        this.setMinimumSize(BUTTON_DIMENSION);
        this.setPreferredSize(BUTTON_DIMENSION);
        this.setLineWrap(false);
        this.setEditable(false);
        this.addMouseListener(this);
        this.setBackground(Color.white);

        String fileName = mailAttachment.getFileName();
        this.fileName = fileName.substring(0, fileName.lastIndexOf("."));
        this.fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

        String fileNameText = this.fileName;
        // Om texten kommer bli längre än rutan:
        while (this.getFontMetrics(this.getFont()).charsWidth(fileNameText.toCharArray(), 0, fileNameText.length()) >= width - horizPadding * 2) {
            fileNameText = fileNameText.substring(0, fileNameText.length() - 1).trim();
        }

        String stringBuilder = fileNameText + "\n" + fileExtension.toUpperCase() +
                "\n" + sizeString(mailAttachment.getSize());
        this.setText(stringBuilder);
    }

    String sizeString(int size) {
        String end = "B";
        String[] sizes = {"B", "kB", "MB", "GB", "TB"};
        for (int i = 0; i < size; i++) {
            if (size >= 1024) {
                size = size / 1024;
                end = sizes[i];
            } else {
                break;
            }
        }
        return size + " " + end;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName + "." + fileExtension)); // Föreslå filnamn
        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            // Spara fil
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    mailAttachment.saveFile(fileChooser.getSelectedFile().getAbsolutePath());
                    return null;
                }
            }.execute();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        this.setBackground(new Color(0xE0EED8));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        this.setBackground(Color.white);
    }
}
