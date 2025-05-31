package org.example.components;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.example.data.GlobalData;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.OutputStream;
import com.google.gson.Gson;
public class NewGroupDialog extends JDialog {

    private JTextField groupNameField;
    private JButton createButton;
    private JPanel selectedMembersPanel;
    private List<String> selectedUserIds = new ArrayList<>();
    private List<UserCheckbox> userCheckboxes = new ArrayList<>();

    public NewGroupDialog(JFrame parent, Consumer<GroupData> onCreate) {
        super(parent, "New Group", true);
        setLayout(new BorderLayout(10, 10));
        setSize(500, 400);
        setLocationRelativeTo(parent);

        // Set background toàn bộ dialog thành trắng
        getContentPane().setBackground(Color.WHITE);

        // Input panel for group name
        JPanel inputPanel = new JPanel();
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        // Group name field
        groupNameField = new JTextField();
        Color borderColor = Color.decode("#99CCFF");
        groupNameField.setBorder(new CompoundBorder(
                new LineBorder(borderColor, 2, true),
                new EmptyBorder(5, 10, 5, 10)
        ));
        groupNameField.setPreferredSize(new Dimension(0, 30));

        JPanel groupNamePanel = labeledField("Group Name:", groupNameField);
        groupNamePanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        groupNamePanel.setBackground(Color.WHITE);
        inputPanel.add(groupNamePanel);

        // Member selection panel
        JPanel memberSelectionPanel = new JPanel(new BorderLayout(10, 10));
        memberSelectionPanel.setBackground(Color.WHITE);
        memberSelectionPanel.setBorder(new EmptyBorder(5, 10, 10, 10));

        // Available members list - USE DIRECT CHECKBOXES INSTEAD OF JLIST
        JPanel availableMembersPanel = new JPanel(new BorderLayout(5, 5));
        availableMembersPanel.setBackground(Color.WHITE);
        
        JLabel availableMembersLabel = new JLabel("Available Members:");
        availableMembersLabel.setForeground(Color.decode("#99CCFF"));
        availableMembersPanel.add(availableMembersLabel, BorderLayout.NORTH);

        // Create panel with checkboxes
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setBackground(Color.WHITE);

        // Add checkboxes for sample users
        addSampleCheckboxes(checkboxPanel);

        JScrollPane memberScrollPane = new JScrollPane(checkboxPanel);
        memberScrollPane.setPreferredSize(new Dimension(200, 200));
        availableMembersPanel.add(memberScrollPane, BorderLayout.CENTER);

        // Selected members panel
        selectedMembersPanel = new JPanel();
        selectedMembersPanel.setLayout(new BoxLayout(selectedMembersPanel, BoxLayout.Y_AXIS));
        selectedMembersPanel.setBackground(Color.WHITE);
        selectedMembersPanel.setBorder(new CompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(5, 5, 5, 5)
        ));

        JPanel selectedPanel = new JPanel(new BorderLayout(5, 5));
        selectedPanel.setBackground(Color.WHITE);
        
        JLabel selectedLabel = new JLabel("Selected Members:");
        selectedLabel.setForeground(Color.decode("#99CCFF"));
        selectedPanel.add(selectedLabel, BorderLayout.NORTH);
        selectedPanel.add(new JScrollPane(selectedMembersPanel), BorderLayout.CENTER);

        // Add both panels to member selection panel
        memberSelectionPanel.add(availableMembersPanel, BorderLayout.WEST);
        memberSelectionPanel.add(selectedPanel, BorderLayout.CENTER);

        inputPanel.add(memberSelectionPanel);

        // Create button
        createButton = new ButtonCustom("Create Group");
                // Thay thế đoạn code xử lý nút Create
        createButton.addActionListener(e -> {
            String groupName = groupNameField.getText().trim();
            if (groupName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a group name.");
                return;
            }
            
            if (selectedUserIds.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one member.");
                return;
            }
            
            // Tạo executor để gửi request trong background
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    // Gửi request tạo group
                    selectedUserIds.add(GlobalData.userId); // Thêm người dùng hiện tại vào danh sách
                    createGroupOnServer(groupName, selectedUserIds);
                    
                    // Nếu thành công, thông báo và đóng dialog
                    SwingUtilities.invokeLater(() -> {
                        // Thông báo thành công
                        JOptionPane.showMessageDialog(
                            this,
                            "Group created successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        
                        // Gọi callback
                        onCreate.accept(new GroupData(groupName, selectedUserIds));
                        
                        // Đóng dialog
                        dispose();
                    });
                } catch (Exception ex) {
                    // Nếu có lỗi, hiển thị thông báo
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                            this,
                            "Failed to create group: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    });
                } finally {
                    executor.shutdown();
                }
            });
        });
        
        JPanel createButtonWrapper = new JPanel(new BorderLayout());
        createButtonWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        createButtonWrapper.setBackground(Color.WHITE);
        createButtonWrapper.add(createButton, BorderLayout.CENTER);

        add(inputPanel, BorderLayout.CENTER);
        add(createButtonWrapper, BorderLayout.SOUTH);
    }

        // Thay thế phương thức addSampleCheckboxes() và thêm phương thức loadUsersFromAPI()
    
    private void addSampleCheckboxes(JPanel container) {
        // Gọi API để lấy danh sách users thực tế
        loadUsersFromAPI(container);
    }
    
    private void loadUsersFromAPI(JPanel container) {
        // Tạo một executor service để gọi API trong background
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        
        executorService.submit(() -> {
            try {
                // Gọi API
                URL url = new URL("http://localhost:8081/api/all");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Đọc response
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // Parse JSON
                    String jsonResponse = response.toString();
                    JsonArray jsonArray = JsonParser.parseString(jsonResponse).getAsJsonArray();
                    
                    // Cập nhật UI trong EDT
                    SwingUtilities.invokeLater(() -> {
                        try {
                            for (JsonElement element : jsonArray) {
                                JsonObject userObj = element.getAsJsonObject();
                                String userId = userObj.get("id").getAsString();
                                String username = userObj.get("username").getAsString();
                                
                                // Tạo checkbox cho mỗi user
                                JCheckBox checkbox = new JCheckBox(username);
                                checkbox.setBackground(Color.WHITE);
                                
                                checkbox.addActionListener(e -> {
                                    if (checkbox.isSelected()) {
                                        selectedUserIds.add(userId);
                                    } else {
                                        selectedUserIds.remove(userId);
                                    }
                                    updateSelectedMembers();
                                });
                                
                                userCheckboxes.add(new UserCheckbox(checkbox, userId, username));
                                container.add(checkbox);
                                container.add(Box.createVerticalStrut(5)); // Space between checkboxes
                            }
                            
                            // Refresh UI
                            container.revalidate();
                            container.repaint();
                        } catch (Exception e) {
                            e.printStackTrace();
                            showErrorAndLoadSampleData(container);
                        }
                    });
                } else {
                    // Nếu API call thất bại, hiển thị sample data
                    SwingUtilities.invokeLater(() -> showErrorAndLoadSampleData(container));
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> showErrorAndLoadSampleData(container));
            }
            
            executorService.shutdown();
        });
    }
        // Thêm phương thức này vào class NewGroupDialog
    private void createGroupOnServer(String groupName, List<String> userIds) throws Exception {
        // Tạo JSON payload
        String jsonPayload = String.format(
            "{\"groupName\":\"%s\",\"userIds\":%s}",
            groupName,
            new Gson().toJson(userIds)
        );
        
        // Tạo connection
        URL url = new URL("http://localhost:8081/api/create-group");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        
        // Gửi payload
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // Đọc response
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
            throw new Exception("Failed to create group. Response code: " + responseCode);
        }
    }
    private void showErrorAndLoadSampleData(JPanel container) {
        JOptionPane.showMessageDialog(
            this,
            "Could not load users from server. Using sample data instead.",
            "Connection Error",
            JOptionPane.WARNING_MESSAGE
        );
        
        // Thêm sample data
        String[][] users = {
            {"1", "John Doe"},
            {"2", "Jane Smith"},
            {"3", "Michael Johnson"},
            {"4", "Emily Brown"},
            {"5", "David Wilson"}
        };
        
        for (String[] user : users) {
            JCheckBox checkbox = new JCheckBox(user[1]);
            checkbox.setBackground(Color.WHITE);
            
            final String userId = user[0];
            final String username = user[1];
            
            checkbox.addActionListener(e -> {
                if (checkbox.isSelected()) {
                    selectedUserIds.add(userId);
                } else {
                    selectedUserIds.remove(userId);
                }
                updateSelectedMembers();
            });
            
            userCheckboxes.add(new UserCheckbox(checkbox, userId, username));
            container.add(checkbox);
            container.add(Box.createVerticalStrut(5));
        }
        
        container.revalidate();
        container.repaint();
    }

    private void updateSelectedMembers() {
        selectedMembersPanel.removeAll();
        
        for (UserCheckbox userCheckbox : userCheckboxes) {
            if (userCheckbox.checkbox.isSelected()) {
                // Add selected user to the panel
                JPanel userItem = new JPanel(new BorderLayout(5, 0));
                userItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                userItem.setBackground(Color.decode("#F0F8FF"));
                userItem.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
                
                JLabel nameLabel = new JLabel(userCheckbox.username);
                userItem.add(nameLabel, BorderLayout.WEST);
                
                selectedMembersPanel.add(userItem);
                selectedMembersPanel.add(Box.createVerticalStrut(2));
            }
        }
        
        selectedMembersPanel.revalidate();
        selectedMembersPanel.repaint();
    }

    private JPanel labeledField(String labelText, Component field) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.decode("#99CCFF"));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    // Helper class for checkbox-user link
    private static class UserCheckbox {
        JCheckBox checkbox;
        String userId;
        String username;
        
        public UserCheckbox(JCheckBox checkbox, String userId, String username) {
            this.checkbox = checkbox;
            this.userId = userId;
            this.username = username;
        }
    }

    // Group data class to return when creating a group
    public static class GroupData {
        public final String groupName;
        public final List<String> memberIds;

        public GroupData(String groupName, List<String> memberIds) {
            this.groupName = groupName;
            this.memberIds = memberIds;
        }
    }
}