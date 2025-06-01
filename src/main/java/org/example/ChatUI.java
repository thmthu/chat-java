// ChatUI.java
package org.example;

import org.example.components.ChatPanel;
import org.example.components.SidebarPanel;
import org.example.data.GlobalData;
import org.example.websocket.ChatWebSocketClient;
import org.example.components.NewChatDialog;
import org.example.components.ButtonCustom;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ChatUI extends JFrame {
    private final ChatWebSocketClient socketClient = new ChatWebSocketClient();
    // Add this method to the ChatUI class

    public ChatWebSocketClient getSocketClient() {
        return socketClient;
    }
    public ChatUI() {
        setTitle("CHAT APP HELLO USER " + GlobalData.userId + "!");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        getContentPane().add(mainPanel);

        // Create the header panel with logout button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Create app title for the left side of header
        JLabel appTitle = new JLabel("CHAT APP HELLO USER " + GlobalData.userId + "!");
        appTitle.setFont(new Font("Arial", Font.BOLD, 18));
        appTitle.setForeground(Color.decode("#99CCFF"));
        headerPanel.add(appTitle, BorderLayout.WEST);
        
        // Create logout button for the right side of header
        
        JButton logoutButton = new ButtonCustom("Logout");
        // Add action listener to logout button
        logoutButton.addActionListener(e -> {
            // Clear user session data
            GlobalData.userId = null;
            
            // Close WebSocket connection
            socketClient.closeConnection();
            
            // Close current window and open login screen
            dispose();
            new LoginUI().setVisible(true);
        });
        
        headerPanel.add(logoutButton, BorderLayout.EAST);
        
        // Add header panel to the top of the main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        SidebarPanel sidebarPanel = new SidebarPanel(e -> {
            NewChatDialog dialog = new NewChatDialog(this, data -> {
            
                try {
                   String userId = GlobalData.userId;
                    String receiverId = data.recipient;
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    
                    // Tạo chatRoomId cho direct chat
                    String chatRoomId = ChatWebSocketClient.normalizeChatRoomId(userId, receiverId);
                    
                    String jsonMessage = String.format(
                        "{\"senderId\":\"%s\",\"receiverId\":\"%s\",\"content\":\"%s\",\"timestamp\":\"%s\",\"chatRoomId\":\"%s\"}",
                        userId,
                        receiverId,
                        data.message,
                        timestamp,
                        chatRoomId
                    );
                    
                    // Send through WebSocket connection
                    socketClient.sendMessage(jsonMessage);
                    
                    // Refresh chat list
                    
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to send message: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    ex.printStackTrace();
                }
            });
            dialog.setVisible(true);
        });

        ChatPanel chatPanel = new ChatPanel();
        

        mainPanel.add(sidebarPanel, BorderLayout.WEST);
        mainPanel.add(chatPanel, BorderLayout.CENTER);
        // In ChatUI.java constructor


        // Set up send button action
        chatPanel.setSendMessageActionListener(e -> {
            String message = chatPanel.getMessageText();
            String currentChatRoomId = chatPanel.getCurrentChatRoomId();
            
            if (!message.isEmpty() && currentChatRoomId != null) {
                try {
                    String userId = GlobalData.userId;
                    String receiverId;
                    
                    // Kiểm tra nếu là group chat (bắt đầu bằng "group_")
                    boolean isGroupChat = currentChatRoomId.startsWith("group_");
                    
                    if (isGroupChat) {
                        // Nếu là group chat, receiverId = "group"
                        receiverId = "group";
                    } else {
                        // Nếu là direct chat, extract receiverId từ chatRoomId
                        receiverId = extractReceiverId(currentChatRoomId, userId);
                    }
                    
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    
                    // Thêm chatRoomId vào JSON
                    String jsonMessage = String.format(
                        "{\"senderId\":\"%s\",\"receiverId\":\"%s\",\"content\":\"%s\",\"timestamp\":\"%s\",\"chatRoomId\":\"%s\"}",
                        userId,
                        receiverId,
                        message,
                        timestamp,
                        currentChatRoomId
                    );
                    
                    socketClient.sendMessage(jsonMessage);
                    chatPanel.addMessage(userId,"", message);
            
                  
                } catch (Exception ex) {
                    // Error handling...
                }
            } else if (currentChatRoomId == null) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please select a chat first",
                    "No chat selected",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
                sidebarPanel.setOnChatSelected(chatRoomId -> {
                    chatPanel.setChatRoomId(chatRoomId);
                });

                setVisible(true);
                socketClient.setChatPanel(chatPanel);
                socketClient.setSidebarPanel(sidebarPanel);
                chatPanel.setSidebarPanel(sidebarPanel);
                socketClient.connectAfterLogin();

            }
    // Thêm method này vào ChatUI.java
    private String extractReceiverId(String chatRoomId, String currentUserId) {
        if (chatRoomId == null) return "";
        
        String[] ids = chatRoomId.split("_");
        if (ids.length == 2) {
            // Nếu chatRoomId có format "user1_user2", trả về ID không phải của người dùng hiện tại
            return ids[0].equals(currentUserId) ? ids[1] : ids[0];
        }
        return "";
    }

   // Just modify the main method:

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Start with login UI instead of directly opening ChatUI
            new LoginUI().setVisible(true);
        });
    }
}