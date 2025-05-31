package org.example.websocket;

import org.example.components.ChatPanel;
import org.example.components.SidebarPanel;
import org.example.data.GlobalData;

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
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI("ws://localhost:8081/ws/chat"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                System.out.println("✅ Received message for chat room: " + chatRoomId);
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