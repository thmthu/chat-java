package org.example.components;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.function.Consumer;

public class NewChatDialog extends JDialog {

    private JTextField recipientField;
    private JTextArea messageArea;
    private JButton sendButton;


    public NewChatDialog(JFrame parent, Consumer<ChatData> onSend) {
        super(parent, "New Chat", true);
        setLayout(new BorderLayout(10, 10));
        setSize(350, 250);
        setLocationRelativeTo(parent);

        // Set background toàn bộ dialog thành trắng
        getContentPane().setBackground(Color.WHITE);

        // Input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setBackground(Color.WHITE); // background trắng cho panel chứa input
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS)); // Xếp theo chiều dọc
        inputPanel.setBackground(Color.WHITE);

        recipientField = new JTextField();
        Color borderColor = Color.decode("#99CCFF");
        recipientField.setBorder(new CompoundBorder(
                new LineBorder(borderColor, 2, true), // viền màu xanh, dày 2 px, bo góc
                new EmptyBorder(5, 10, 5, 10)       // padding bên trong 10 px
        ));
        // Thu nhỏ chiều cao ô recipientField bằng cách setPreferredSize chiều cao thấp hơn
        recipientField.setPreferredSize(new Dimension(0, 10)); // chiều cao 25px thay vì mặc định cao hơn

        messageArea = new JTextArea(5, 20);
        messageArea.setBorder(new CompoundBorder(
                new LineBorder(borderColor, 2, true), // viền màu xanh, dày 2 px, bo góc
                new EmptyBorder(10, 10, 10, 10)       // padding bên trong 10 px
        ));

        JPanel recipientPanel = labeledField("Recipient:", recipientField);
        recipientPanel.setBorder(new EmptyBorder(10, 10, 5, 10)); // top, left, bottom, right margin

        JPanel messagePanel = labeledField("Message:",messageArea);
        messagePanel.setBorder(new EmptyBorder(5, 10, 10, 10));

        recipientPanel.setBackground(Color.WHITE);
        messagePanel.setBackground(Color.WHITE);
        inputPanel.add(recipientPanel);
        inputPanel.add(messagePanel);

        // Send button
        sendButton = new ButtonCustom("Send");
        sendButton.addActionListener(e -> {
            String recipient = recipientField.getText().trim();
            String content = messageArea.getText().trim();
            if (!recipient.isEmpty() && !content.isEmpty()) {
                onSend.accept(new ChatData(recipient, content));
                dispose(); // close popup
            } else {
                JOptionPane.showMessageDialog(this, "Please fill in both fields.");
            }
        });
        JPanel sendButtonWrapper = new JPanel(new BorderLayout());
        sendButtonWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // top, left, bottom, right
        sendButtonWrapper.setBackground(Color.WHITE);

        sendButtonWrapper.add(sendButton, BorderLayout.CENTER);

        add(inputPanel, BorderLayout.CENTER);
        add(sendButtonWrapper, BorderLayout.SOUTH);
    }

    private JPanel labeledField(String labelText, Component field) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.decode("#99CCFF")); // set màu label ở đây
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    // Dữ liệu chat gửi đi
    public static class ChatData {
        public final String recipient;
        public final String message;
        public ChatData(String recipient, String message) {
            this.recipient = recipient;
            this.message = message;
        }
    }
}
