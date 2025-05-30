package org.example.components;

import org.example.data.GlobalData;
import org.example.DTO.ChatMessage;

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
    }
    
    // Set chat room ID and load messages
    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
        loadMessages();
    }
    
    // Load messages from API
    private void loadMessages() {
        if (chatRoomId == null || chatRoomId.isEmpty()) {
            return;
        }
        
        executorService.submit(() -> {
            try {
                String apiUrl = "http://localhost:8081/demo/list-message/" + chatRoomId;
                
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
                    
                    SwingUtilities.invokeLater(() -> {
                        messagesPanel.removeAll();
                        for (ChatMessage message : messages) {
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
        JPanel bubble = new JPanel(new BorderLayout(5, 5));
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        // Set alignment and style based on sender
        if (isSelf) {
            bubble.setBackground(new Color(0, 132, 255));
            messagePanel.add(bubble, BorderLayout.EAST);
            bubble.setMaximumSize(new Dimension(300, 1000));
            bubble.setPreferredSize(new Dimension(Math.min(300, message.getContent().length() * 8), 
                                               Math.max(30, (message.getContent().length() / 40) * 20 + 20)));
        } else {
            bubble.setBackground(new Color(240, 240, 240));
            messagePanel.add(bubble, BorderLayout.WEST);
            bubble.setMaximumSize(new Dimension(300, 1000));
            bubble.setPreferredSize(new Dimension(Math.min(300, message.getContent().length() * 8),
                                               Math.max(30, (message.getContent().length() / 40) * 20 + 20)));
        }
        
        // Message text
        JLabel contentLabel = new JLabel("<html><p style='width: 280px;'>" + message.getContent() + "</p></html>");
        contentLabel.setForeground(isSelf ? Color.WHITE : Color.BLACK);
        bubble.add(contentLabel, BorderLayout.CENTER);
        
        // Time label
        JLabel timeLabel = new JLabel(message.getSentAt().format(TIME_FORMATTER));
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        timeLabel.setForeground(isSelf ? new Color(220, 220, 220) : Color.GRAY);
        bubble.add(timeLabel, BorderLayout.SOUTH);
        
        messagesPanel.add(messagePanel);
        messagesPanel.add(Box.createVerticalStrut(5));
    }
    
    // Add a message programmatically (for new messages)
    public void addMessage(String senderId, String content) {
        ChatMessage message = new ChatMessage();
        message.setChatRoomId(chatRoomId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        
        addMessageBubble(message);
        messagesPanel.revalidate();
        messagesPanel.repaint();
        
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