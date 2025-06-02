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
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonDeserializationContext;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;

public class SidebarPanel extends JPanel {
    private JButton newChatButton;
    private DefaultListModel<ChatSidebarItem> chatListModel;
    private JList<ChatSidebarItem> chatList;
    private JFrame mainFrame;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Gson gson;
    private Consumer<String> onChatSelected;

    // Store online user IDs
    private Set<String> onlineUserIds = new HashSet<>();

    // Getter for renderer
    public Set<String> getOnlineUserIds() {
        return onlineUserIds;
    }

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

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 0));
        setBackground(Color.WHITE);

        JButton newChatButton = new ButtonCustom("        + New Chat      ");
        newChatButton.addActionListener(onNewChatClicked);

        JButton newChatGroup = new ButtonCustom("   + New Group  ");
        newChatGroup.addActionListener(e -> showNewGroupDialog());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        topPanel.setBackground(Color.WHITE);

        topPanel.add(newChatButton);
        topPanel.add(newChatGroup);
        add(topPanel, BorderLayout.NORTH);

        chatListModel = new DefaultListModel<>();
        chatList = new JList<>(chatListModel);
        chatList.setCellRenderer(new ChatListCellRenderer(this)); // Pass SidebarPanel for online status
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
                        // After loading chat data, fetch online users
                        loadOnlineUsers();
                    });
                } else {
                    System.err.println("Failed to fetch chat data: " + responseCode);
                    addSampleData();
                    loadOnlineUsers();
                }
            } catch (Exception e) {
                e.printStackTrace();
                addSampleData();
                loadOnlineUsers();
            }
        });
    }
public void updateUserOnlineStatus(String userId, boolean online) {
        synchronized (onlineUserIds) {
            if (online) {
                onlineUserIds.add(userId);
            } else {
                onlineUserIds.remove(userId);
            }
        }
        // Repaint the chat list to reflect the new status
        SwingUtilities.invokeLater(() -> {
            if (chatList != null) {
                chatList.repaint();
            }
        });
    }
    // Fetch online users from backend
    private void loadOnlineUsers() {
        executorService.submit(() -> {
            System.out.println("Fetching online users...");
            try {
                String apiUrl = "http://localhost:8081/api/online-users";
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                System.out.println("Online users API response code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {

                        response.append(line);
                    }
                    reader.close();
                    System.out.println("==========Online users response: " + response.toString());
                    // Assume response is a JSON array of user IDs: ["user1", "user2", ...]
                    Type listType = new TypeToken<List<String>>(){}.getType();
                    List<String> ids = gson.fromJson(response.toString(), listType);
                    synchronized (onlineUserIds) {
                        onlineUserIds.clear();
                        onlineUserIds.addAll(ids);
                    }
                    SwingUtilities.invokeLater(() -> chatList.repaint());
                }
            } catch (Exception e) {
                System.out.println("Error fetching online users: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void showNewGroupDialog() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);

        NewGroupDialog dialog = new NewGroupDialog(parent, data -> {
            System.out.println("Creating group: " + data.groupName);
            System.out.println("Members: " + data.memberIds);

            try {
                JOptionPane.showMessageDialog(
                    parent,
                    "Group '" + data.groupName + "' has been created successfully!",
                    "Group Created",
                    JOptionPane.INFORMATION_MESSAGE
                );
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

    public void refreshChatList() {
        loadChatData();
    }
}