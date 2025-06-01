package org.example.components;

import org.example.data.GlobalData;
import org.example.DTO.ChatMessage;
import org.example.utils.AttachmentUtil;
import org.example.ChatUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import java.io.File;

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
    private JLabel chatTitleLabel;
    private JButton deleteButton;
    
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
        messagesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagesPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        // Add some spacing at the bottom to ensure messages don't get hidden behind input panel
        messagesPanel.add(Box.createVerticalStrut(20));
        
        // Wrap messages panel in scroll pane
        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Create chat header panel with title and delete button
        JPanel headerPanel = createHeaderPanel();
        
        // Input panel at the bottom
        JPanel inputPanel = createInputPanel();
        
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refreshLayout();
            }
        });
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Chat title label on the left
        chatTitleLabel = new JLabel("Select a chat");
        chatTitleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        chatTitleLabel.setForeground(Color.decode("#666666"));
        headerPanel.add(chatTitleLabel, BorderLayout.WEST);
        
        // Delete button on the right
        deleteButton = new JButton("Delete");
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setBackground(Color.decode("#FF6B6B"));
        deleteButton.setBorderPainted(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setEnabled(false); // Initially disabled
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        // Add rounded corners and styling
        deleteButton.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Paint rounded background
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 20, 20);
                
                // Paint text
                super.paint(g, c);
                g2.dispose();
            }
        });
        
        // Add hover effect
        deleteButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (deleteButton.isEnabled()) {
                    deleteButton.setBackground(Color.decode("#FF4040"));
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (deleteButton.isEnabled()) {
                    deleteButton.setBackground(Color.decode("#FF6B6B"));
                }
            }
        });
        
        // Add delete action
        deleteButton.addActionListener(e -> {
            if (chatRoomId != null && !chatRoomId.isEmpty()) {
                // Confirm deletion
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete this chat?",
                    "Delete Chat",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (confirm == JOptionPane.YES_OPTION) {
                    deleteChatRoom(chatRoomId, chatTitleLabel, deleteButton);
                }
            }
        });
        
        headerPanel.add(deleteButton, BorderLayout.EAST);
        return headerPanel;
    }
    
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.setBackground(Color.WHITE);
        
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        JButton attachButton = new JButton("📎");
        attachButton.setBorderPainted(false);
        attachButton.setContentAreaFilled(false);
        attachButton.setFocusPainted(false);
        attachButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        attachButton.setToolTipText("Attach file");
        Font currentFont = attachButton.getFont();
        Font boldLargerFont = currentFont.deriveFont(Font.BOLD, 25f);
        attachButton.setFont(boldLargerFont);        
        attachButton.setForeground(Color.decode("#99CCFF"));
        // Use attachment utility for file handling
        attachButton.addActionListener(e -> {
            Component mainFrame = SwingUtilities.getWindowAncestor(this);
            AttachmentUtil.handleFileAttachment(this, chatRoomId, executorService, 
                                             messagesPanel, scrollPane, 
                                             sidebarPanel, mainFrame);
        });
        
        sendButton = new ButtonCustom("Send");
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(attachButton, BorderLayout.WEST);
        
        return inputPanel;
    }
    
    private void refreshLayout() {
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }
    
    // Set chat room ID and load messages
    public void setChatRoomId(String chatRoomId) {
        System.out.println("Setting chat room ID: " + chatRoomId);
        this.chatRoomId = chatRoomId;
        
        // Update header UI
        if (chatRoomId != null && !chatRoomId.isEmpty()) {
            // Enable delete button
            if (deleteButton != null) {
                deleteButton.setEnabled(true);
            }
            
            // Update chat title - use a simplified version if we don't have the actual name
            if (chatTitleLabel != null) {
                String displayName;
                if (chatRoomId.startsWith("group_")) {
                    displayName = "Group Chat";
                } else {
                    String[] parts = chatRoomId.split("_");
                    String otherUserId = parts[0].equals(GlobalData.userId) ? parts[1] : parts[0];
                    displayName = otherUserId;
                }
                chatTitleLabel.setText(displayName);
            }
        } else {
            // Disable delete button and reset title
            if (deleteButton != null) {
                deleteButton.setEnabled(false);
            }
            if (chatTitleLabel != null) {
                chatTitleLabel.setText("Select a chat");
            }
        }
        
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
    public void removeMessageById(String messageId) {
    if (messageId == null) return;
    Component[] components = messagesPanel.getComponents();
    for (Component comp : components) {
        if (comp instanceof JPanel && messageId.equals(
                ((JPanel) comp).getName() != null ? ((JPanel) comp).getName().replace("message-", "") : null)) {
            messagesPanel.remove(comp);
            messagesPanel.revalidate();
            messagesPanel.repaint();
            break;
        }
    }
}
    public void addMessageBubble(ChatMessage message) {
        boolean isSelf = message.getSenderId().equals(GlobalData.userId);
        
        // Message panel
        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        // messagePanel.setOpaque(false);
        messagePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        messagePanel.setLayout(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        messagePanel.setOpaque(false);
         if (message.getMessageId() != null) {
        messagePanel.setName("message-" + message.getMessageId());
    }
        // Message bubble
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        // Set alignment and style based on sender
        if (isSelf) {
            bubble.setBackground(Color.decode("#578FCA"));
            messagePanel.add(bubble, BorderLayout.EAST);
        } else {
            bubble.setBackground(new Color(240, 240, 240));
            messagePanel.add(bubble, BorderLayout.WEST);
        }
        
        // Add sender name label - only for messages from others
        if (!isSelf && message.getSenderName() != null && !message.getSenderName().isEmpty()) {
        JPanel senderNamePanel = new JPanel(new BorderLayout());
        senderNamePanel.setOpaque(false);

        JLabel senderNameLabel = new JLabel("  "+ message.getSenderName());
        senderNameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        senderNameLabel.setForeground(Color.decode("#99CCFF"));

        senderNamePanel.add(senderNameLabel, BorderLayout.WEST);

        // Add senderNamePanel directly to messagesPanel before messagePanel
        messagesPanel.add(senderNamePanel);
    
    }
        if (isSelf && message.getMessageId() != null) {
    JPopupMenu popupMenu = new JPopupMenu();
    JMenuItem deleteItem = new JMenuItem("Delete");
    popupMenu.add(deleteItem);

    deleteItem.addActionListener(e -> {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete this message?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            deleteMessageById(message.getMessageId(), messagePanel);
            System.out.println("Message deleted: " + message.getMessageId());
        }
    });

    bubble.setComponentPopupMenu(popupMenu);
    messagePanel.setComponentPopupMenu(popupMenu);
}
        // Check if this is a file message
        String content = message.getContent();
        if (content.startsWith("FILE")) {
            // Extract file URL and name
            String[] parts = content.split("=", 3);
            if (parts.length >= 3) {
                String fileName = parts[1];
                String fileUrl = parts[2];

                
                // Create file attachment panel
                JPanel filePanel = new JPanel();
                filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
                filePanel.setBackground(bubble.getBackground());
                filePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                
                // File icon
                JLabel fileIcon = new JLabel("File");
                fileIcon.setFont(new Font("Arial", Font.BOLD, 14));
                fileIcon.setForeground(isSelf ? Color.WHITE : Color.decode("#99CCFF"));                
                // File name as a clickable link
                JButton fileLink = new JButton(fileName);
                fileLink.setBorderPainted(false);
                fileLink.setContentAreaFilled(false);
                fileLink.setForeground(isSelf ? Color.WHITE : Color.BLUE);
                fileLink.setFont(new Font("Arial", Font.PLAIN, 14));
                fileLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
                fileLink.setToolTipText("Click to download");
                
                // Add download action using AttachmentUtil
                fileLink.addActionListener(e -> {
                    AttachmentUtil.downloadFile(this, fileUrl, fileName, executorService);
                });
                
                filePanel.add(fileIcon);
                filePanel.add(Box.createHorizontalStrut(5));
                filePanel.add(fileLink);
                
                bubble.add(filePanel);
            } else {
                // Fallback to regular text display if format is wrong
                addRegularTextContent(bubble, content, isSelf);
            }
        } else {
            // Regular text message
            addRegularTextContent(bubble, content, isSelf);
        }
        
        // Time label
        JLabel timeLabel = new JLabel(message.getSentAt().format(TIME_FORMATTER));
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        timeLabel.setForeground(isSelf ? new Color(220, 220, 220) : Color.GRAY);
        timeLabel.setAlignmentX(isSelf ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        
        bubble.add(Box.createVerticalStrut(3)); // Spacing
        bubble.add(timeLabel);
        
        messagesPanel.add(messagePanel);
        messagesPanel.add(Box.createVerticalStrut(5));
    }
    
    // Helper method to add regular text content to a bubble
    private void addRegularTextContent(JPanel bubble, String content, boolean isSelf) {
        JTextArea contentLabel = new JTextArea(content);
        contentLabel.setEditable(false);
        contentLabel.setLineWrap(true);
        contentLabel.setWrapStyleWord(true);
        contentLabel.setBackground(bubble.getBackground());
        contentLabel.setForeground(isSelf ? Color.WHITE : Color.BLACK);
        contentLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        contentLabel.setBorder(null);
        
        // Size adjustment based on content length
        int contentLength = content.length();
        int preferredWidth;
        
        if (contentLength <= 5) {
            preferredWidth = contentLength * 7 + 5;
        } else if (contentLength <= 20) {
            preferredWidth = contentLength * 7 + 5;
        } else {
            preferredWidth = Math.min(300, contentLength * 5 + 5);
        }
        
        contentLabel.setPreferredSize(new Dimension(preferredWidth, contentLabel.getPreferredSize().height));
        bubble.add(contentLabel);
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
    
    // Delete a chat room
    private void deleteChatRoom(String chatRoomId, JLabel chatTitleLabel, JButton deleteButton) {
        executorService.submit(() -> {
            try {
                // Create the API URL with the chat room ID
                URL url = new URL("http://localhost:8081/api/delete-chat/" + chatRoomId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("Content-Type", "application/json");
                
                // Add user ID as a request parameter or header for authentication
                connection.setRequestProperty("User-ID", GlobalData.userId);
                
                int responseCode = connection.getResponseCode();
                
                SwingUtilities.invokeLater(() -> {
                    if (responseCode == HttpURLConnection.HTTP_OK || 
                        responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                        // Success - update UI
                        clearChat();
                        chatTitleLabel.setText("Select a chat");
                        deleteButton.setEnabled(false);
                        
                        // Refresh sidebar to remove the deleted chat
                        if (sidebarPanel != null) {
                            sidebarPanel.refreshChatList();
                        }
                        
                        JOptionPane.showMessageDialog(
                            this,
                            "Chat deleted successfully.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        // Error handling
                        JOptionPane.showMessageDialog(
                            this,
                            "Failed to delete chat. Error code: " + responseCode,
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                });
                
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to delete chat: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                });
            }
        });
    }

    private void deleteMessageById(String messageId, JPanel messagePanel) {
    executorService.submit(() -> {
        try {
            String apiUrl = "http://localhost:8081/api/messages/" + messageId;
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("User-ID", GlobalData.userId);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                SwingUtilities.invokeLater(() -> {
                    messagesPanel.remove(messagePanel);
                    messagesPanel.revalidate();
                    messagesPanel.repaint();
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to delete message. Error code: " + responseCode,
                        "Delete Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                });
            }
             } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                    this,
                    "Error deleting message: " + ex.getMessage(),
                    "Delete Error",
                    JOptionPane.ERROR_MESSAGE
                );
            });
        }
    });
}
    // Clear the chat panel
    public void clearChat() {
        this.chatRoomId = null;
        
        SwingUtilities.invokeLater(() -> {
            messagesPanel.removeAll();
            messagesPanel.revalidate();
            messagesPanel.repaint();
        });
    }
}