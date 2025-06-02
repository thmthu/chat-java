package org.example.components;

import org.example.DTO.ChatSidebarItem;

import java.net.URL;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class ChatListCellRenderer extends JPanel implements ListCellRenderer<ChatSidebarItem> {
    private JLabel avatarLabel = new JLabel();
    private JLabel nameLabel = new JLabel();
    private JLabel messageLabel = new JLabel();
    private JLabel timeLabel = new JLabel();
    private JLabel unreadLabel = new JLabel();
    private JLabel onlineDotLabel = new JLabel();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private SidebarPanel sidebarPanel;

    public ChatListCellRenderer(SidebarPanel sidebarPanel) {
        this.sidebarPanel = sidebarPanel;
        setLayout(new BorderLayout(10, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Avatar panel on the left
        JPanel avatarPanel = new JPanel(new BorderLayout());
        avatarPanel.setPreferredSize(new Dimension(50, 50));
        avatarPanel.add(avatarLabel, BorderLayout.CENTER);

        // Online dot at bottom right of avatar
        JPanel avatarWrapper = new JPanel(null);
        avatarWrapper.setPreferredSize(new Dimension(50, 50));
        avatarLabel.setBounds(0, 0, 40, 40);
        onlineDotLabel.setBounds(32, 32, 14, 14);
        avatarWrapper.add(avatarLabel);
        avatarWrapper.add(onlineDotLabel);
        avatarPanel.add(avatarWrapper, BorderLayout.CENTER);

        // Center panel with name and message
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(5, 8, 0, 0));

        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        messageLabel.setForeground(Color.GRAY);

        centerPanel.add(nameLabel);
        centerPanel.add(Box.createVerticalStrut(3));
        centerPanel.add(messageLabel);

        add(avatarPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ChatSidebarItem> list, 
                                                 ChatSidebarItem value, 
                                                 int index, 
                                                 boolean isSelected, 
                                                 boolean cellHasFocus) {

        // Avatar image or fallback
        try {
            URL url = new URL(value.getSenderAvatar());
            ImageIcon originalIcon = new ImageIcon(url);
            Image scaledImage = originalIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            avatarLabel.setIcon(scaledIcon);
            avatarLabel.setText("");
        } catch (Exception e) {
            avatarLabel.setIcon(null);
            avatarLabel.setText(value.getSenderName().substring(0, 1).toUpperCase());
        }
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setBackground(new Color(230, 230, 250));
        avatarLabel.setOpaque(true);

        // Online status dot
        boolean isOnline = sidebarPanel != null && sidebarPanel.getOnlineUserIds().contains(value.getSenderName());
        if (isOnline) {
            onlineDotLabel.setIcon(createDotIcon(new Color(0, 200, 0), 12));
            onlineDotLabel.setVisible(true);
        } else {
            onlineDotLabel.setIcon(createDotIcon(Color.LIGHT_GRAY, 12));
            onlineDotLabel.setVisible(true);
        }

        // Set name and message
        nameLabel.setText(value.getSenderName());
        messageLabel.setText(value.getLatestMessage().length() > 30 ? 
                            value.getLatestMessage().substring(0, 27) + "..." : 
                            value.getLatestMessage());

        // Set selection background
        setBackground(isSelected ? new Color(240, 240, 240) : Color.WHITE);

        return this;
    }

    // Helper to create a colored dot icon
    private Icon createDotIcon(Color color, int diameter) {
        return new Icon() {
            public int getIconWidth() { return diameter; }
            public int getIconHeight() { return diameter; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(color);
                g.fillOval(x, y, diameter, diameter);
            }
        };
    }
}