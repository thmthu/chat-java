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
