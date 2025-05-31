package org.example.components;

import org.example.data.GlobalData;
import org.example.DTO.ChatSidebarItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;

public class SidebarPanel extends JPanel {
    private JButton newChatButton;
    private DefaultListModel<ChatSidebarItem> chatListModel;
    private JList<ChatSidebarItem> chatList;
    private JFrame mainFrame;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Gson gson;
    // Add a new field to hold the callback function
    private Consumer<String> onChatSelected;

    // Add this method to set the chat selection handler
    public void setOnChatSelected(Consumer<String> onChatSelected) {
        this.onChatSelected = onChatSelected;
}
    public SidebarPanel(ActionListener onNewChatClicked) {
        // Initialize Gson with custom date time adapter
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_DATE_TIME);
            }
        });
        gson = gsonBuilder.create();
        
        // Setup UI
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 0));
        setBackground(Color.WHITE);
        
        // Create new chat button
        JButton newChatButton = new ButtonCustom("+ New Chat");
        newChatButton.addActionListener(onNewChatClicked);

        JButton newChatGroup = new ButtonCustom("+ New Group");
        newChatGroup.addActionListener(e -> showNewGroupDialog());

        JButton onlineList = new ButtonCustom("Online");
        newChatButton.addActionListener(onNewChatClicked);
        
        // Add the button to a panel at the top
       // Sử dụng FlowLayout với khoảng cách 15 pixels giữa các components
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2)); // hgap=15, vgap=5
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        topPanel.setBackground(Color.WHITE);

        // Thêm các nút vào panel
        topPanel.add(newChatButton);
        topPanel.add(newChatGroup);
        topPanel.add(onlineList);
        add(topPanel, BorderLayout.NORTH);
        
        // Create chat list
        chatListModel = new DefaultListModel<>();
        chatList = new JList<>(chatListModel);
        chatList.setCellRenderer(new ChatListCellRenderer());
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        chatList.addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting()) {
            ChatSidebarItem selectedItem = chatList.getSelectedValue();
            if (selectedItem != null && onChatSelected != null) {
                onChatSelected.accept(selectedItem.getChatRoomId());
            }
        }
        });
        JScrollPane scrollPane = new JScrollPane(chatList);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
        
        // Load data
        loadChatData();
    }
    
    private void loadChatData() {
        executorService.submit(() -> {
            try {
                String userId = GlobalData.userId;
                String apiUrl = "http://localhost:8081/api/list-message-side-bar/" + userId;
                System.out.println("Fetching chat data from: " + apiUrl);
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

                    Type listType = new TypeToken<ArrayList<ChatSidebarItem>>(){}.getType();
                    List<ChatSidebarItem> chatItems = gson.fromJson(response.toString(), listType);
                    
                    SwingUtilities.invokeLater(() -> {
                        chatListModel.clear();
                        for (ChatSidebarItem item : chatItems) {
                            chatListModel.addElement(item);
                        }
                    });
                } else {
                    System.err.println("Failed to fetch chat data: " + responseCode);
                    // If API fails, add some sample data
                    addSampleData();
                }
            } catch (Exception e) {
                e.printStackTrace();
                // If error occurs, add some sample data
                addSampleData();
            }
        });
    }
    private void showNewGroupDialog() {
    // Tìm JFrame cha
    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
    
    // Tạo và hiển thị dialog
    NewGroupDialog dialog = new NewGroupDialog(parent, data -> {
        System.out.println("Creating group: " + data.groupName);
        System.out.println("Members: " + data.memberIds);
        
        // Xử lý tạo group:
        // 1. Gửi request tạo group lên server
        try {
            // Có thể gọi API tạo group ở đây
            // createGroupOnServer(data.groupName, data.memberIds);
            
            // Sau khi tạo group thành công:
            JOptionPane.showMessageDialog(
                parent,
                "Group '" + data.groupName + "' has been created successfully!",
                "Group Created",
                JOptionPane.INFORMATION_MESSAGE
            );
            
            // Refresh danh sách chat để hiển thị group mới
            refreshChatList();
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                parent,
                "Failed to create group: " + ex.getMessage(),
                                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    });
    
    dialog.setVisible(true);
}
    private void addSampleData() {
        SwingUtilities.invokeLater(() -> {
            chatListModel.clear();
            
            ChatSidebarItem item1 = new ChatSidebarItem();
            item1.setChatRoomId("53acda57-67e2-491a-8b31-d1dd202566b9");
            item1.setLatestMessage("minh la 25");
            item1.setSenderName("user3");
            item1.setSenderAvatar("avatar3.jpg");
            item1.setSentAt(LocalDateTime.now().minusMinutes(5));
            item1.setUnreadCount(0);
            
            ChatSidebarItem item2 = new ChatSidebarItem();
            item2.setChatRoomId("dfc7a283-eab3-4d23-bd7d-8a8c08b08e33");
            item2.setLatestMessage("hello second");
            item2.setSenderName("user1");
            item2.setSenderAvatar("avatar1.jpg");
            item2.setSentAt(LocalDateTime.now().minusHours(1));
            item2.setUnreadCount(2);
            
            chatListModel.addElement(item1);
            chatListModel.addElement(item2);
        });
    }
    
    // Add this method to refresh the chat list
    public void refreshChatList() {
        loadChatData();
    }
}