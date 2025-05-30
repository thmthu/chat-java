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
            if (!message.isEmpty()) {
                try {
                    // Create JSON message
                    String userId = GlobalData.userId;
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    
                    String jsonMessage = String.format(
                        "{\"senderId\":\"%s\",\"receiverId\":\"%s\",\"content\":\"%s\",\"timestamp\":\"%s\"}",
                        userId,
                        "1", // Set the appropriate recipient ID
                        message,
                        timestamp
                    );
                    
                    // Send through WebSocket
                    socketClient.sendMessage(jsonMessage);
                    
                    // Add message to UI (optimistic update)
                    chatPanel.addMessage(userId, message);
                    
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to send message: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        // When chat room is selected (e.g., from sidebar)
        chatPanel.setChatRoomId("2_1"); // Set the actual chat room ID
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatUI::new);
    }
}
