// ChatUI.java
package org.example;

import org.example.components.ChatPanel;
import org.example.components.SidebarPanel;
import org.example.data.GlobalData;
import org.example.websocket.ChatWebSocketClient;
import org.example.components.NewChatDialog;

import javax.swing.*;
import java.awt.*;

public class ChatUI extends JFrame {
    private final ChatWebSocketClient socketClient = new ChatWebSocketClient();
    public ChatUI() {
        setTitle("Chat Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        getContentPane().add(mainPanel);


        SidebarPanel sidebarPanel = new SidebarPanel(e -> {
            NewChatDialog dialog = new NewChatDialog(this, data -> {
                System.out.println("Send to: " + data.recipient);
                System.out.println("Message: " + data.message);
                
                try {
                    // Create JSON message in the format expected by the server
                    String userId = GlobalData.userId; // You should replace this with actual logged-in user ID
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    
                    String jsonMessage = String.format(
                        "{\"senderId\":\"%s\",\"receiverId\":\"%s\",\"content\":\"%s\",\"timestamp\":\"%s\"}",
                        userId,
                        data.recipient,
                        data.message,
                        timestamp
                    );
                    
                    // Send through WebSocket connection
                    socketClient.sendMessage(jsonMessage);
                    
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
                    String receiverId = extractReceiverId(currentChatRoomId, userId);
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    
                    String jsonMessage = String.format(
                        "{\"senderId\":\"%s\",\"receiverId\":\"%s\",\"content\":\"%s\",\"timestamp\":\"%s\"}",
                        userId,
                        receiverId, // Sử dụng receiverId được trích xuất từ chatRoomId
                        message,
                        timestamp
                    );
                    
                    socketClient.sendMessage(jsonMessage);
                    chatPanel.addMessage(userId, message);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatUI::new);
    }
}
