package org.example.websocket;

import org.example.components.ChatPanel;
import org.example.components.SidebarPanel;
import org.example.data.GlobalData;

import javax.swing.SwingUtilities;
import javax.websocket.*;
import java.net.URI;
import java.time.LocalDateTime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonDeserializationContext;
import java.lang.reflect.Type;

@ClientEndpoint
public class ChatWebSocketClient {

    private Session session;
    private ChatPanel chatPanel; // Thêm thuộc tính này
    private final Gson gson;
    private SidebarPanel sidebarPanel;
    
    // Thêm phương thức setChatPanel
    public void setChatPanel(ChatPanel chatPanel) {
        this.chatPanel = chatPanel;
    }
    // Thêm phương thức setSidebarPanel
    public void setSidebarPanel(SidebarPanel sidebarPanel) {
        this.sidebarPanel = sidebarPanel;
    }
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("✅ Connected to server");
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message) {
        
        if (chatPanel == null) {
            System.out.println("⚠️ Chat panel not set, ignoring message");
            return;
        }
        System.out.println("================= Received message: " + message);
        if (message.contains("\"type\":\"USER_STATUS\"")) {
            handleUserStatusMessage(message);
            return;
        }

        if (message.contains("\"type\":\"MESSAGE_DELETED\"")) {
            handleWebSocketMessage(message);
            return;
        }

        try {
            // Parse JSON message
            MessagePayload payload = gson.fromJson(message, MessagePayload.class);
            
            // Xác định chatRoomId từ senderId và receiverId
            String chatRoomId = payload.chatRoomId;
            
            // Chỉ xử lý tin nhắn thuộc chatRoom hiện tại
            String currentChatRoomId = chatPanel.getCurrentChatRoomId();
            if (currentChatRoomId != null && currentChatRoomId.equals(chatRoomId)) {
                // Chỉ hiển thị tin nhắn mới nếu không phải do chính người dùng gửi
                // (vì tin nhắn của người dùng đã được thêm vào UI khi gửi đi)
                if (!payload.senderId.equals(GlobalData.userId)) {
                    chatPanel.addMessage(payload.senderId, payload.senderName, payload.content);
                }
            }
            // Cập nhật sidebar nếu có
            if (sidebarPanel != null) {
                System.out.println("Received message for chat room: " + chatRoomId);
                sidebarPanel.refreshChatList();
            }
        } catch (Exception e) {
            System.out.println("❌ Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("❌ Connection closed: " + reason);
    }

    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        } else {
            System.out.println("❗ WebSocket session not open.");
        }
    }
    
    /**
     * Chuẩn hóa chatRoomId từ hai userId (giống server)
     */
    static public String normalizeChatRoomId(String userA, String userB) {
        return userA.compareTo(userB) > 0 ? userA + "_" + userB : userB + "_" + userA;
    }
    // Add this method to the ChatWebSocketClient class
        public ChatWebSocketClient() {
            // Khởi tạo Gson với xử lý đặc biệt cho LocalDateTime
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
                    return LocalDateTime.parse(json.getAsString());
                }
            });
            gson = gsonBuilder.create();
            
            try {
                // Check if user is logged in
                if (GlobalData.userId == null || GlobalData.userId.isEmpty()) {
                    System.out.println("⚠️ User ID not available yet. WebSocket connection will be established later.");
                    return;
                }
                
                // Create connection URL with user ID as query parameter
                String wsUrl = "ws://localhost:8081/ws/chat?userId=" + GlobalData.userId;
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.connectToServer(this, new URI(wsUrl));
                
                System.out.println("🔄 Connecting to WebSocket server with user ID: " + GlobalData.userId);
            } catch (Exception e) {
                System.out.println("❌ Failed to connect to WebSocket server: " + e.getMessage());
                e.printStackTrace();
            }
        }
        // Call this from your WebSocket message handler
        // Không cần import org.json
        // Thêm class phụ để parse MESSAGE_DELETED
        private static class MessageDeletedPayload {
            String type;
            String chatRoomId;
            String messageId;
            String deletedBy;
            long timestamp;
        }

        public void handleWebSocketMessage(String json) {
            try {
                MessageDeletedPayload data = gson.fromJson(json, MessageDeletedPayload.class);
                if ("MESSAGE_DELETED".equals(data.type)) {
                    if (chatPanel != null && data.chatRoomId.equals(chatPanel.getCurrentChatRoomId())) {
                        SwingUtilities.invokeLater(() -> chatPanel.removeMessageById(data.messageId));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        // Add a method to connect after login
        public void connectAfterLogin() {
            if (session != null && session.isOpen()) {
                System.out.println("WebSocket already connected");
                return;
            }
            
            try {
                // Create connection URL with user ID
                String wsUrl = "ws://localhost:8081/ws/chat?userId=" + GlobalData.userId;
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.connectToServer(this, new URI(wsUrl));
                
                System.out.println("🔄 Connected to WebSocket server with user ID: " + GlobalData.userId);
            } catch (Exception e) {
                System.out.println("❌ Failed to connect to WebSocket server: " + e.getMessage());
                e.printStackTrace();
            }
        }
        public void closeConnection() {
            if (session != null && session.isOpen()) {
                try {
                    session.close();
                    System.out.println("WebSocket connection closed by user logout");
                } catch (Exception e) {
                    System.out.println("Error closing WebSocket connection: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // Add this method to handle USER_STATUS
private static class UserStatusPayload {
    String type;
    String userId;
    boolean online;
}

public void handleUserStatusMessage(String json) {
    try {
        UserStatusPayload data = gson.fromJson(json, UserStatusPayload.class);
        if ("USER_STATUS".equals(data.type)) {
            // Update your UI here: for example, update the user list or avatar
            SwingUtilities.invokeLater(() -> {
                if (sidebarPanel != null) {
                    sidebarPanel.updateUserOnlineStatus(data.userId, data.online);
                }
            });
            System.out.println("User " + data.userId + " is now " + (data.online ? "online" : "offline"));
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
}
    /**
     * Class đại diện cho cấu trúc tin nhắn từ server
     */
    private static class MessagePayload {
        private String senderId;
        private String receiverId;
        private String content;
        private String timestamp;
        private String chatRoomId; 
         private String senderName;// Thêm trường này để xác định chat room
    }
}