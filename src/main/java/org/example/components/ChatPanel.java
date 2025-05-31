package org.example.components;

import org.example.data.GlobalData;
import org.example.DTO.ChatMessage;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;

public class ChatPanel extends JPanel {
    private JPanel messagesPanel;
    private JScrollPane scrollPane;
    private JTextField messageField;
    private JButton sendButton;
    private String chatRoomId;
    private final Gson gson;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private SidebarPanel sidebarPanel;
    public ChatPanel() {
        // Initialize Gson with custom date time adapter
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                return LocalDateTime.parse(json.getAsString());
            }
        });
        gson = gsonBuilder.create();
        
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        
        // Messages panel with BoxLayout for vertical stacking
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(Color.WHITE);
        messagesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // THÊM DÒNG NÀY: Đảm bảo messagesPanel sử dụng đủ không gian ngang
        messagesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagesPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        // Add some spacing at the bottom to ensure messages don't get hidden behind input panel
        messagesPanel.add(Box.createVerticalStrut(20));
        
        // Wrap messages panel in scroll pane
        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        
        // Input panel at the bottom
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.setBackground(Color.WHITE);
        
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        sendButton = new ButtonCustom("Send");
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        add(inputPanel, BorderLayout.SOUTH);
        addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    refreshLayout();
                }
            });
        }
    private void refreshLayout() {
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }
    // Set chat room ID and load messages
    public void setChatRoomId(String chatRoomId) {
        System.out.println("Setting chat room ID: " + chatRoomId);
        this.chatRoomId = chatRoomId;
        loadMessages();
    }
    public void setSidebarPanel(SidebarPanel sidebarPanel) {
        this.sidebarPanel = sidebarPanel;
    }   
    // Get the current chat room ID
    public String getCurrentChatRoomId() {
        return chatRoomId;
    }
    
    // Load messages from API
    private void loadMessages() {
        if (chatRoomId == null || chatRoomId.isEmpty()) {
            return;
        }
        
        executorService.submit(() -> {
            try {
                String apiUrl = "http://localhost:8081/api/list-message/" + chatRoomId;
                
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    Type listType = new TypeToken<ArrayList<ChatMessage>>(){}.getType();
                    List<ChatMessage> messages = gson.fromJson(response.toString(), listType);
                    System.out.println("Loaded " + messages.size() + " messages for chat room: " + chatRoomId);
                    SwingUtilities.invokeLater(() -> {
                        messagesPanel.removeAll();
                        for (ChatMessage message : messages) {
                            System.out.println("Adding message: " + message.getContent() + " from " + message.getSenderName());
                            addMessageBubble(message);
                        }
                        
                        // Add spacing at the end
                        messagesPanel.add(Box.createVerticalStrut(20));
                        messagesPanel.revalidate();
                        messagesPanel.repaint();
                        
                        // Scroll to bottom
                        SwingUtilities.invokeLater(() -> {
                            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                            verticalBar.setValue(verticalBar.getMaximum());
                        });
                    });
                } else {
                    System.err.println("Failed to fetch messages: " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
        // Add a single message bubble to the chat
    public void addMessageBubble(ChatMessage message) {
        boolean isSelf = message.getSenderId().equals(GlobalData.userId);
        
        // Message panel
        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messagePanel.setOpaque(false);
        messagePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Message bubble
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        // Set alignment and style based on sender
        if (isSelf) {
            bubble.setBackground(new Color(0, 132, 255));
            messagePanel.add(bubble, BorderLayout.EAST);
        } else {
            bubble.setBackground(new Color(240, 240, 240));
            messagePanel.add(bubble, BorderLayout.WEST);
        }
          // Thêm sender name label vào messagePanel thay vì bubble
    // Chỉ hiển thị nếu không phải tin nhắn của mình
        if (!isSelf && message.getSenderName() != null && !message.getSenderName().isEmpty()) {
            // Tạo panel riêng cho sender name để căn chỉnh đúng
            JPanel senderNamePanel = new JPanel(new BorderLayout());
            senderNamePanel.setOpaque(false);
            
            JLabel senderNameLabel = new JLabel(message.getSenderName());
            senderNameLabel.setFont(new Font("Arial", Font.BOLD, 12));
            senderNameLabel.setForeground(Color.decode("#99CCFF"));
            
            // Đặt sender name ở bên phải (EAST)
            senderNamePanel.add(senderNameLabel, BorderLayout.WEST);
            
            // Thêm senderNamePanel vào phía trên của messagePanel
            messagePanel.add(senderNamePanel, BorderLayout.NORTH);
        }
        // Message text - sử dụng JTextArea thay vì JLabel
        JTextArea contentLabel = new JTextArea(message.getContent());
        System.out.println("Adding message: " + message.getContent());
        contentLabel.setEditable(false);
        contentLabel.setLineWrap(true);
        contentLabel.setWrapStyleWord(true);
        contentLabel.setBackground(bubble.getBackground());
        contentLabel.setForeground(isSelf ? Color.WHITE : Color.BLACK);
        contentLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        contentLabel.setBorder(null);
        
        // Thiết lập kích thước cố định cho vùng chứa tin nhắn
        int contentLength = message.getContent().length();
        int preferredWidth;

        if (contentLength <= 5) {
            // Tin nhắn rất ngắn (như "mi", "ok", "hello")
            preferredWidth = contentLength * 5 + 5; // Thêm padding
        } else if (contentLength <= 20) {
            // Tin nhắn độ dài trung bình
            preferredWidth = contentLength * 5 + 5;
        } else {
            // Tin nhắn dài
            preferredWidth = Math.min(300, contentLength * 5 + 5);
        }
                contentLabel.setPreferredSize(new Dimension(preferredWidth, contentLabel.getPreferredSize().height));        
        // Time label
        JLabel timeLabel = new JLabel(message.getSentAt().format(TIME_FORMATTER));
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        timeLabel.setForeground(isSelf ? new Color(220, 220, 220) : Color.GRAY);
        timeLabel.setAlignmentX(isSelf ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        
        bubble.add(contentLabel);
        bubble.add(Box.createVerticalStrut(3)); // Spacing
        bubble.add(timeLabel);
        
        
        // Đảm bảo messagePanel có chiều rộng đầy đủ
        //messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, messagePanel.getPreferredSize().height));
        
        messagesPanel.add(messagePanel);
        messagesPanel.add(Box.createVerticalStrut(5));
        //sidebarPanel.refreshChatList();
    }
     
    // Add a message programmatically (for new messages)
    public void addMessage(String senderId, String senderName, String content) {
        ChatMessage message = new ChatMessage();
        message.setChatRoomId(chatRoomId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        message.setSenderName(senderName);
        addMessageBubble(message);
        messagesPanel.revalidate();
        messagesPanel.repaint();
        sidebarPanel.refreshChatList();
        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }
    
    // Set action listener for send button
    public void setSendMessageActionListener(ActionListener listener) {
        sendButton.addActionListener(listener);
    }
    
    // Get message from text field and clear it
    public String getMessageText() {
        String message = messageField.getText().trim();
        messageField.setText("");
        return message;
    }
    
    // Get message field for key listeners
    public JTextField getMessageField() {
        return messageField;
    }
    
    // Refresh messages from server
    public void refreshMessages() {
        loadMessages();
    }
}