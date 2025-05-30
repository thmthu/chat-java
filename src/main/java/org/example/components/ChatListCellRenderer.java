package org.example.components;


import org.example.DTO.ChatSidebarItem;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    public ChatListCellRenderer() {
        setLayout(new BorderLayout(10, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Avatar panel on the left
        JPanel avatarPanel = new JPanel(new BorderLayout());
        avatarPanel.setPreferredSize(new Dimension(50, 50));
        avatarPanel.add(avatarLabel, BorderLayout.CENTER);
        
        // Center panel with name and message
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        messageLabel.setForeground(Color.GRAY);
        
        centerPanel.add(nameLabel);
        centerPanel.add(Box.createVerticalStrut(3));
        centerPanel.add(messageLabel);
        
        // Right panel with time and unread count
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        timeLabel.setForeground(Color.GRAY);
        
        unreadLabel.setFont(new Font("Arial", Font.BOLD, 12));
        unreadLabel.setForeground(Color.WHITE);
        unreadLabel.setBackground(new Color(0, 122, 255));
        unreadLabel.setOpaque(true);
        unreadLabel.setHorizontalAlignment(SwingConstants.CENTER);
        unreadLabel.setBorder(new EmptyBorder(2, 6, 2, 6));
        
        rightPanel.add(timeLabel);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(unreadLabel);
        
        add(avatarPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }
    
    @Override
    public Component getListCellRendererComponent(JList<? extends ChatSidebarItem> list, 
                                                 ChatSidebarItem value, 
                                                 int index, 
                                                 boolean isSelected, 
                                                 boolean cellHasFocus) {

         try {
        URL url = new URL(value.getSenderAvatar());
        ImageIcon originalIcon = new ImageIcon(url);
        
        // Scale the image to fit the avatar panel (50x50)
        Image scaledImage = originalIcon.getImage().getScaledInstance(
            40, 40, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        
        avatarLabel.setIcon(scaledIcon);
        //avatarLabel.setText(""); // Clear any text
    } catch (Exception e) {
        // Fallback to text avatar if image loading fails
        System.err.println("Failed to load avatar image: " + e.getMessage());
        avatarLabel.setIcon(null);
        avatarLabel.setText(value.getSenderName().substring(0, 1).toUpperCase());
    }
    
        // Set avatar (for demo, we'll just show first letter of name)
        avatarLabel.setText(value.getSenderName().substring(0, 1).toUpperCase());
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setBackground(new Color(230, 230, 250));
        avatarLabel.setOpaque(true);
        
        // Set name and message
        nameLabel.setText(value.getSenderName());
        messageLabel.setText(value.getLatestMessage().length() > 30 ? 
                            value.getLatestMessage().substring(0, 27) + "..." : 
                            value.getLatestMessage());
        
        // Set time
        timeLabel.setText(value.getSentAt().format(TIME_FORMATTER));
        
        // Set unread count
        if (value.getUnreadCount() > 0) {
            unreadLabel.setText(String.valueOf(value.getUnreadCount()));
            unreadLabel.setVisible(true);
        } else {
            unreadLabel.setVisible(false);
        }
        
        // Set selection background
        if (isSelected) {
            setBackground(new Color(240, 240, 240));
        } else {
            setBackground(Color.WHITE);
        }
        
        return this;
    }
}