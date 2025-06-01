package org.example.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.ChatUI;
import org.example.DTO.ChatMessage;
import org.example.data.GlobalData;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

public class AttachmentUtil {

    /**
     * Hiển thị file chooser để người dùng chọn file cần gửi
     */
    public static void handleFileAttachment(JPanel parent, String chatRoomId, ExecutorService executorService, 
                                           JPanel messagesPanel, JScrollPane scrollPane, 
                                           JComponent sidebar, Component mainFrame) {
        if (chatRoomId == null || chatRoomId.isEmpty()) {
            JOptionPane.showMessageDialog(
                parent,
                "Please select a chat first",
                "No chat selected",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        
        // Create a file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a file to send");
        
        // Show the dialog and check if a file was selected
        int result = fileChooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            uploadFile(parent, selectedFile, chatRoomId, executorService, messagesPanel, scrollPane, sidebar, mainFrame);
        }
    }

    /**
     * Tải file lên server
     */
    public static void uploadFile(JPanel parent, File file, String chatRoomId, ExecutorService executorService,
                                 JPanel messagesPanel, JScrollPane scrollPane, 
                                 JComponent sidebar, Component mainFrame) {
        // Disable buttons during upload
        Component[] components = ((Container) parent.getParent()).getComponents();
        for (Component c : components) {
            if (c instanceof JButton) {
                c.setEnabled(false);
            }
        }
        
        // Show upload progress
        JOptionPane uploadingDialog = new JOptionPane(
            "Uploading file: " + file.getName() + "...",
            JOptionPane.INFORMATION_MESSAGE,
            JOptionPane.DEFAULT_OPTION,
            null,
            new Object[]{},
            null
        );
        
        JDialog dialog = uploadingDialog.createDialog(parent, "Uploading");
        dialog.setModal(false);
        
        executorService.submit(() -> {
            try {
                // Create connection
                String apiUrl = "http://localhost:8081/api/upload-file";
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                // Set up the connection for multipart upload
                String boundary = "*****" + System.currentTimeMillis() + "*****";
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                connection.setRequestProperty("User-ID", GlobalData.userId);
                connection.setRequestProperty("Chat-Room-ID", chatRoomId);
                connection.setDoOutput(true);
                
                // Start dialog on EDT
                SwingUtilities.invokeLater(() -> dialog.setVisible(true));
                
                try (OutputStream outputStream = connection.getOutputStream();
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {
                    
                    // Add chat room ID
                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"chatRoomId\"").append("\r\n");
                    writer.append("\r\n");
                    writer.append(chatRoomId).append("\r\n");
                    
                    // Add sender ID
                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"senderId\"").append("\r\n");
                    writer.append("\r\n");
                    writer.append(GlobalData.userId).append("\r\n");
                    
                    // Add file
                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                          .append(file.getName()).append("\"").append("\r\n");
                    writer.append("Content-Type: ")
                          .append(URLConnection.guessContentTypeFromName(file.getName()))
                          .append("\r\n");
                    writer.append("\r\n");
                    writer.flush();
                    
                    // File data
                    try (FileInputStream inputStream = new FileInputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        outputStream.flush();
                    }
                    
                    // End of multipart/form-data
                    writer.append("\r\n");
                    writer.append("--").append(boundary).append("--").append("\r\n");
                    writer.flush();
                }
                
                // Check the response
                int responseCode = connection.getResponseCode();
                
                // Close dialog
                SwingUtilities.invokeLater(() -> dialog.dispose());
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // Parse JSON response to get file URL and ID
                    JsonObject jsonResponse = JsonParser.parseString(response.toString())
                                                      .getAsJsonObject();
                    
                    final String fileUrl = jsonResponse.get("fileUrl").getAsString();
                    final String fileId = jsonResponse.get("fileId").getAsString();
                    final String fileName = file.getName();
                    
                    // Create file message
                    ChatMessage fileMessage = new ChatMessage();
                    fileMessage.setChatRoomId(chatRoomId);
                    fileMessage.setSenderId(GlobalData.userId);
                    fileMessage.setContent("FILE:" + fileUrl + ":" + fileName);
                    fileMessage.setSentAt(LocalDateTime.now());
                    fileMessage.setSenderName("");
                    
                    // Thêm vào UI thông qua callback
                    if (parent instanceof org.example.components.ChatPanel) {
                        org.example.components.ChatPanel chatPanel = (org.example.components.ChatPanel) parent;
                        SwingUtilities.invokeLater(() -> {
                            chatPanel.addMessageBubble(fileMessage);
                            messagesPanel.revalidate();
                            messagesPanel.repaint();
                            
                            // Refresh sidebar
                            if (sidebar instanceof org.example.components.SidebarPanel) {
                                ((org.example.components.SidebarPanel) sidebar).refreshChatList();
                            }
                            
                            // Scroll to bottom
                            SwingUtilities.invokeLater(() -> {
                                JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                                verticalBar.setValue(verticalBar.getMaximum());
                            });
                        });
                    }
                    
                    // Send WebSocket notification about the file
                    String jsonMessage = String.format(
                        "{\"senderId\":\"%s\",\"receiverId\":\"%s\",\"content\":\"%s\",\"timestamp\":\"%s\",\"chatRoomId\":\"%s\",\"isFile\":true,\"fileUrl\":\"%s\",\"fileName\":\"%s\"}",
                        GlobalData.userId,
                        extractReceiverId(chatRoomId, GlobalData.userId),
                        "Sent a file: " + fileName,
                        System.currentTimeMillis(),
                        chatRoomId,
                        fileUrl,
                        fileName
                    );
                    
                    // Get reference to the WebSocket client from the main UI
                    if (mainFrame instanceof ChatUI) {
                        ChatUI chatUI = (ChatUI) mainFrame;
                        chatUI.getSocketClient().sendMessage(jsonMessage);
                    }
                    
                } else {
                    // Show error message
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                            parent,
                            "Failed to upload file. Error code: " + responseCode,
                            "Upload Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    });
                }
                
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    dialog.dispose();
                    JOptionPane.showMessageDialog(
                        parent,
                        "Error uploading file: " + ex.getMessage(),
                        "Upload Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                });
            } finally {
                // Re-enable buttons
                SwingUtilities.invokeLater(() -> {
                    for (Component c : components) {
                        if (c instanceof JButton) {
                            c.setEnabled(true);
                        }
                    }
                });
            }
        });
    }

    /**
     * Tải file từ server về máy
     */
   /**
     * Tải file từ server về máy
     */
    public static void downloadFile(JPanel parent, String fileUrl, String fileName, ExecutorService executorService) {
        // Create a file chooser for saving

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save File");
        fileChooser.setSelectedFile(new File(fileName));
        
        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            
            // Show download progress
            JOptionPane downloadingDialog = new JOptionPane(
                "Downloading file: " + fileName + "...",
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                new Object[]{},
                null
            );
            
            JDialog dialog = downloadingDialog.createDialog(parent, "Downloading");
            dialog.setModal(false);
            
            executorService.submit(() -> {
                try {
                    // Start dialog on EDT
                    SwingUtilities.invokeLater(() -> dialog.setVisible(true));
                        
               
                    URL url = new URL(fileUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-ID", GlobalData.userId);
                    
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Get file from server
                        try (InputStream inputStream = connection.getInputStream();
                            FileOutputStream outputStream = new FileOutputStream(fileToSave)) {
                            
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                        
                        // Close dialog and show success message
                        SwingUtilities.invokeLater(() -> {
                            dialog.dispose();
                            JOptionPane.showMessageDialog(
                                parent,
                                "File downloaded successfully.",
                                "Download Complete",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                        });
                    } else {
                        // Show error message
                        SwingUtilities.invokeLater(() -> {
                            dialog.dispose();
                            JOptionPane.showMessageDialog(
                                parent,
                                "Failed to download file. Error code: " + responseCode,
                                "Download Error",
                                JOptionPane.ERROR_MESSAGE
                            );
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        JOptionPane.showMessageDialog(
                            parent,
                            "Error downloading file: " + ex.getMessage(),
                            "Download Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    });
                }
            });
        }
    }

    /**
     * Trích xuất ID người nhận từ chat room ID
     */
    public static String extractReceiverId(String chatRoomId, String currentUserId) {
        if (chatRoomId == null) return "";
        
        if (chatRoomId.startsWith("group_")) {
            return "group";
        }
        
        String[] ids = chatRoomId.split("_");
        if (ids.length == 2) {
            return ids[0].equals(currentUserId) ? ids[1] : ids[0];
        }
        return "";
    }
}